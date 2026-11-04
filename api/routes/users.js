const express = require('express');
const { User, Task, StudySession, Badge, DeviceToken, sequelize } = require('../models');
const requireAuth = require('../middleware/auth');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();
router.use(requireAuth);

const LANGUAGES = ['en', 'zu', 'af'];
const THEMES = ['light', 'dark'];

function publicUser(user) {
  const { passwordHash, ...safe } = user.toJSON();
  return safe;
}

// GET /api/users/me
router.get(
  '/me',
  asyncHandler(async (req, res) => {
    const user = await User.findByPk(req.userId);
    if (!user) return res.status(404).json({ error: 'User not found' });
    res.json(publicUser(user));
  })
);

// PUT /api/users/me -> update display name / language / theme
router.put(
  '/me',
  asyncHandler(async (req, res) => {
    const user = await User.findByPk(req.userId);
    if (!user) return res.status(404).json({ error: 'User not found' });

    const { name, language, themePref } = req.body;
    if (name !== undefined && (typeof name !== 'string' || !name.trim() || name.length > 100)) {
      return res.status(400).json({ error: 'invalid name' });
    }
    if (language !== undefined && language !== null && !LANGUAGES.includes(language)) {
      return res.status(400).json({ error: 'language must be one of en, zu, af' });
    }
    if (themePref !== undefined && themePref !== null && !THEMES.includes(themePref)) {
      return res.status(400).json({ error: 'themePref must be light or dark' });
    }

    await user.update({
      name: name ? name.trim() : user.name,
      language: language || user.language,
      themePref: themePref || user.themePref,
    });
    res.json(publicUser(user));
  })
);

// DELETE /api/users/me -> permanently delete the account and everything linked to it.
// (Google Play requires apps that create accounts to offer account deletion.)
router.delete(
  '/me',
  asyncHandler(async (req, res) => {
    await sequelize.transaction(async (transaction) => {
      const where = { where: { userId: req.userId }, transaction };
      await Task.destroy(where);
      await StudySession.destroy(where);
      await Badge.destroy(where);
      await DeviceToken.destroy(where);
      await User.destroy(where);
    });
    res.json({ deleted: true });
  })
);

module.exports = router;
