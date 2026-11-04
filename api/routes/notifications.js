const express = require('express');
const { DeviceToken } = require('../models');
const requireAuth = require('../middleware/auth');
const asyncHandler = require('../middleware/asyncHandler');
const { sendToUser } = require('../services/push');

const router = express.Router();
router.use(requireAuth);

// POST /api/notifications/register { fcmToken } -> remember this device for push notifications
router.post(
  '/register',
  asyncHandler(async (req, res) => {
    const { fcmToken } = req.body;
    if (!fcmToken || typeof fcmToken !== 'string' || fcmToken.length > 255) {
      return res.status(400).json({ error: 'fcmToken is required' });
    }
    // A device token belongs to one account at a time (e.g. someone logging in on a shared phone).
    await DeviceToken.destroy({ where: { fcmToken } });
    const token = await DeviceToken.create({ userId: req.userId, fcmToken });
    res.status(201).json({ registered: true, tokenId: token.tokenId });
  })
);

// POST /api/notifications/unregister { fcmToken } -> stop pushing to this device (on log out)
router.post(
  '/unregister',
  asyncHandler(async (req, res) => {
    const { fcmToken } = req.body;
    if (!fcmToken) return res.status(400).json({ error: 'fcmToken is required' });
    await DeviceToken.destroy({ where: { userId: req.userId, fcmToken } });
    res.json({ unregistered: true });
  })
);

// POST /api/notifications/send-test -> push a real notification to this user's devices right now.
// Used by the "Send test notification" button in Settings (and handy for the demo video).
router.post(
  '/send-test',
  asyncHandler(async (req, res) => {
    const result = await sendToUser(
      req.userId,
      (req.body && req.body.title) || 'StudyFlow reminder',
      (req.body && req.body.body) || 'This is a real push notification sent from the StudyFlow API.'
    );
    if (result.disabled) {
      return res.status(503).json({ error: 'Push notifications are not configured on this server' });
    }
    if (result.noTokens) return res.status(404).json({ error: 'No device registered for this user yet' });
    res.json(result);
  })
);

module.exports = router;
