const express = require('express');
const { Op } = require('sequelize');
const { Task } = require('../models');
const requireAuth = require('../middleware/auth');
const asyncHandler = require('../middleware/asyncHandler');
const { awardXp, XP_PER_TASK } = require('../services/gamification');

const router = express.Router();
router.use(requireAuth);

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const PRIORITIES = ['Low', 'Med', 'High'];
const STATUSES = ['Open', 'Done'];

function validDate(value) {
  return value !== undefined && value !== null && !Number.isNaN(Date.parse(value));
}

// GET /api/tasks?since=<ISO timestamp> -> this user's tasks (optionally only those changed since)
router.get(
  '/',
  asyncHandler(async (req, res) => {
    const where = { userId: req.userId };
    if (req.query.since && validDate(req.query.since)) {
      where.updatedAt = { [Op.gt]: new Date(req.query.since) };
    }
    res.json(await Task.findAll({ where, order: [['dueDate', 'ASC']] }));
  })
);

// POST /api/tasks -> create OR update (upsert) a task.
// The Android app generates the task id locally (offline-first), so the same task can be pushed
// more than once by the sync job without ever creating duplicates.
router.post(
  '/',
  asyncHandler(async (req, res) => {
    const { taskId, title, description, subject, dueDate, priority, status, reminderTime } = req.body;

    if (!title || typeof title !== 'string' || title.trim().length === 0 || title.length > 150) {
      return res.status(400).json({ error: 'title is required (max 150 characters)' });
    }
    if (!validDate(dueDate)) return res.status(400).json({ error: 'a valid dueDate is required' });
    if (priority && !PRIORITIES.includes(priority)) return res.status(400).json({ error: 'invalid priority' });
    if (status && !STATUSES.includes(status)) return res.status(400).json({ error: 'invalid status' });
    if (reminderTime && !validDate(reminderTime)) return res.status(400).json({ error: 'invalid reminderTime' });
    if (taskId && !UUID_RE.test(taskId)) return res.status(400).json({ error: 'taskId must be a UUID' });

    const fields = {
      title: title.trim(),
      description: description || null,
      subject: subject || null,
      dueDate,
      priority: priority || 'Med',
      status: status || 'Open',
      reminderTime: reminderTime || null,
    };

    if (taskId) {
      const existing = await Task.findByPk(taskId);
      if (existing && existing.userId !== req.userId) {
        return res.status(409).json({ error: 'taskId already in use' });
      }
      if (existing) {
        const completedNow = existing.status !== 'Done' && fields.status === 'Done';
        await existing.update(fields);
        if (completedNow) await awardXp(req.userId, XP_PER_TASK);
        return res.json(existing);
      }
    }

    const task = await Task.create({ ...(taskId ? { taskId } : {}), userId: req.userId, ...fields });
    if (task.status === 'Done') await awardXp(req.userId, XP_PER_TASK);
    return res.status(201).json(task);
  })
);

// PUT /api/tasks/:id -> partial update
router.put(
  '/:id',
  asyncHandler(async (req, res) => {
    const task = await Task.findOne({ where: { taskId: req.params.id, userId: req.userId } });
    if (!task) return res.status(404).json({ error: 'Task not found' });

    const { title, description, subject, dueDate, priority, status, reminderTime } = req.body;
    if (title !== undefined && (typeof title !== 'string' || !title.trim() || title.length > 150)) {
      return res.status(400).json({ error: 'invalid title' });
    }
    if (dueDate !== undefined && !validDate(dueDate)) return res.status(400).json({ error: 'invalid dueDate' });
    if (priority !== undefined && !PRIORITIES.includes(priority)) return res.status(400).json({ error: 'invalid priority' });
    if (status !== undefined && !STATUSES.includes(status)) return res.status(400).json({ error: 'invalid status' });

    const completedNow = status === 'Done' && task.status !== 'Done';
    await task.update({ title, description, subject, dueDate, priority, status, reminderTime });
    if (completedNow) await awardXp(req.userId, XP_PER_TASK);
    res.json(task);
  })
);

// DELETE /api/tasks/:id
router.delete(
  '/:id',
  asyncHandler(async (req, res) => {
    const task = await Task.findOne({ where: { taskId: req.params.id, userId: req.userId } });
    if (!task) return res.status(404).json({ error: 'Task not found' });
    await task.destroy();
    res.json({ deleted: true });
  })
);

module.exports = router;
