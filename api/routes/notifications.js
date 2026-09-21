const express = require('express');
const { DeviceToken } = require('../models');
const requireAuth = require('../middleware/auth');
const { messaging } = require('../config/firebaseAdmin');

const router = express.Router();
router.use(requireAuth);

// POST /api/notifications/register -> save an FCM device token for push notifications
router.post('/register', async (req, res) => {
  const { fcmToken } = req.body;
  if (!fcmToken) return res.status(400).json({ error: 'fcmToken is required' });

  const [token] = await DeviceToken.findOrCreate({
    where: { userId: req.userId, fcmToken },
  });
  res.status(201).json({ registered: true, tokenId: token.tokenId });
});

// POST /api/notifications/send-test -> push a real notification to every device this
// user is registered on. Handy for demoing "real-time notifications" without waiting
// on the scheduled due-date job - fires immediately.
router.post('/send-test', async (req, res) => {
  if (!messaging) {
    return res.status(503).json({ error: 'Push notifications are not configured on this server (no Firebase service account).' });
  }
  const { title, body } = req.body;
  const tokens = await DeviceToken.findAll({ where: { userId: req.userId } });
  if (tokens.length === 0) {
    return res.status(404).json({ error: 'No device tokens registered for this user yet' });
  }

  const response = await messaging.sendEachForMulticast({
    tokens: tokens.map((t) => t.fcmToken),
    notification: {
      title: title || 'StudyFlow reminder',
      body: body || 'This is a test push notification from the StudyFlow API.',
    },
  });

  res.json({ sent: response.successCount, failed: response.failureCount });
});

module.exports = router;

