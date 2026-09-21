const express = require('express');
const { Task } = require('../models');
const requireAuth = require('../middleware/auth');

const router = express.Router();
router.use(requireAuth);

// GET /api/tasks?since=<ISO timestamp>  -> list tasks for the logged-in user (optionally changed since last sync)
router.get('/', async (req, res) => {
  const { since } = req.query;
  const where = { userId: req.userId };
  if (since) {
    const { Op } = require('sequelize');
    where.updatedAt = { [Op.gt]: new Date(since) };
  }
  const tasks = await Task.findAll({ where });
  res.json(tasks);
});

// POST /api/tasks -> create a task
router.post('/', async (req, res) => {
  const { title, description, subject, dueDate, priority, reminderTime } = req.body;
  if (!title || !dueDate) return res.status(400).json({ error: 'title and dueDate are required' });
  const task = await Task.create({
    userId: req.userId, title, description, subject, dueDate, priority, reminderTime,
  });
  res.status(201).json(task);
});

// PUT /api/tasks/:id -> update a task
router.put('/:id', async (req, res) => {
  const task = await Task.findOne({ where: { taskId: req.params.id, userId: req.userId } });
  if (!task) return res.status(404).json({ error: 'Task not found' });
  await task.update(req.body);
  res.json(task);
});

// DELETE /api/tasks/:id
router.delete('/:id', async (req, res) => {
  const task = await Task.findOne({ where: { taskId: req.params.id, userId: req.userId } });
  if (!task) return res.status(404).json({ error: 'Task not found' });
  await task.destroy();
  res.json({ deleted: true });
});

module.exports = router;
