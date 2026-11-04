const express = require('express');
const { Badge } = require('../models');
const requireAuth = require('../middleware/auth');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();
router.use(requireAuth);

// GET /api/badges -> badges this user has earned
router.get(
  '/',
  asyncHandler(async (req, res) => {
    res.json(await Badge.findAll({ where: { userId: req.userId }, order: [['dateEarned', 'ASC']] }));
  })
);

module.exports = router;
