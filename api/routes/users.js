const express = require('express');
const { User } = require('../models');
const requireAuth = require('../middleware/auth');

const router = express.Router();
router.use(requireAuth);

// GET /api/users/me
router.get('/me', async (req, res) => {
  const user = await User.findByPk(req.userId);
  if (!user) return res.status(404).json({ error: 'User not found' });
  const { passwordHash, ...safe } = user.toJSON();
  res.json(safe);
});

// PUT /api/users/me -> update language / theme / notification prefs
router.put('/me', async (req, res) => {
  const user = await User.findByPk(req.userId);
  if (!user) return res.status(404).json({ error: 'User not found' });
  const { name, language, themePref } = req.body;
  await user.update({
    name: name ?? user.name,
    language: language ?? user.language,
    themePref: themePref ?? user.themePref,
  });
  const { passwordHash, ...safe } = user.toJSON();
  res.json(safe);
});

module.exports = router;
