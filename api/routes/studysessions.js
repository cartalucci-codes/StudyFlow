const express = require('express');
const { StudySession, User } = require('../models');
const requireAuth = require('../middleware/auth');

const router = express.Router();
router.use(requireAuth);

// GET /api/studysessions -> list this user's study sessions
router.get('/', async (req, res) => {
  const sessions = await StudySession.findAll({ where: { userId: req.userId } });
  res.json(sessions);
});

// POST /api/studysessions -> log a completed (or abandoned) study session, award XP if completed
router.post('/', async (req, res) => {
  const { taskId, startTime, durationMins, completed } = req.body;
  if (!startTime || !durationMins) {
    return res.status(400).json({ error: 'startTime and durationMins are required' });
  }
  const session = await StudySession.create({
    userId: req.userId, taskId, startTime, durationMins, completed: !!completed,
  });

  if (completed) {
    const user = await User.findByPk(req.userId);
    await user.update({ xpPoints: user.xpPoints + Math.round(durationMins) });
  }

  res.status(201).json(session);
});

module.exports = router;
