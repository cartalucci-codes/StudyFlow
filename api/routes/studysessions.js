const express = require('express');
const { StudySession } = require('../models');
const requireAuth = require('../middleware/auth');
const asyncHandler = require('../middleware/asyncHandler');
const { awardXp, evaluateBadges } = require('../services/gamification');

const router = express.Router();
router.use(requireAuth);

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

// GET /api/studysessions -> this user's study sessions
router.get(
  '/',
  asyncHandler(async (req, res) => {
    res.json(await StudySession.findAll({ where: { userId: req.userId }, order: [['startTime', 'DESC']] }));
  })
);

// POST /api/studysessions -> log a study session. Idempotent on the client-generated sessionId,
// so the offline sync job can safely retry. Completed sessions award 1 XP per focused minute
// and may unlock streak badges.
router.post(
  '/',
  asyncHandler(async (req, res) => {
    const { sessionId, taskId, startTime, durationMins, completed } = req.body;

    if (Number.isNaN(Date.parse(startTime))) return res.status(400).json({ error: 'a valid startTime is required' });
    if (!Number.isInteger(durationMins) || durationMins < 1 || durationMins > 600) {
      return res.status(400).json({ error: 'durationMins must be a whole number between 1 and 600' });
    }
    if (sessionId && !UUID_RE.test(sessionId)) return res.status(400).json({ error: 'sessionId must be a UUID' });
    if (taskId && !UUID_RE.test(taskId)) return res.status(400).json({ error: 'taskId must be a UUID' });

    if (sessionId) {
      const existing = await StudySession.findByPk(sessionId);
      if (existing) {
        if (existing.userId !== req.userId) return res.status(409).json({ error: 'sessionId already in use' });
        return res.json(existing); // already synced - nothing more to do
      }
    }

    const session = await StudySession.create({
      ...(sessionId ? { sessionId } : {}),
      userId: req.userId,
      taskId: taskId || null,
      startTime,
      durationMins,
      completed: !!completed,
    });

    let newBadges = [];
    if (session.completed) {
      await awardXp(req.userId, durationMins);
      newBadges = await evaluateBadges(req.userId);
    }
    res.status(201).json({ ...session.toJSON(), newBadges: newBadges.map((b) => b.badgeType) });
  })
);

module.exports = router;
