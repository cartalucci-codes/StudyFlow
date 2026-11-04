const { DeviceToken } = require('../models');
const { messaging } = require('../config/firebaseAdmin');

const DEAD_TOKEN_CODES = [
  'messaging/registration-token-not-registered',
  'messaging/invalid-registration-token',
];

// Sends a push to every device the user has registered. We send a *data* message (not a
// "notification" message) so the app's StudyFlowMessagingService always handles it - this lets
// the app respect the user's notification settings and build the notification itself.
async function sendToUser(userId, title, body) {
  if (!messaging) return { sent: 0, failed: 0, disabled: true };

  const tokens = await DeviceToken.findAll({ where: { userId } });
  if (tokens.length === 0) return { sent: 0, failed: 0, noTokens: true };

  const response = await messaging.sendEachForMulticast({
    tokens: tokens.map((t) => t.fcmToken),
    data: { title, body },
    android: { priority: 'high' },
  });

  // Clean up tokens for uninstalled apps / stale registrations.
  await Promise.all(
    response.responses.map((r, i) =>
      !r.success && r.error && DEAD_TOKEN_CODES.includes(r.error.code) ? tokens[i].destroy() : null
    )
  );

  return { sent: response.successCount, failed: response.failureCount };
}

module.exports = { sendToUser };
