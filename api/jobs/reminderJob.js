const cron = require('node-cron');
const { Op } = require('sequelize');
const { Task, DeviceToken } = require('../models');
const { messaging } = require('../config/firebaseAdmin');

/**
 * Runs every 15 minutes: finds tasks due within the next hour that haven't been
 * reminded about yet, and pushes a notification to that user's registered devices.
 * Matches Planning & Design doc, Section 5: "a scheduled server-side job ... triggers
 * FCM pushes for upcoming deadlines and daily study reminders."
 */
function startReminderJob() {
  if (!messaging) {
    console.log('Reminder job not started - push notifications are not configured.');
    return;
  }

  cron.schedule('*/15 * * * *', async () => {
    const now = new Date();
    const inOneHour = new Date(now.getTime() + 60 * 60 * 1000);

    const dueSoonTasks = await Task.findAll({
      where: { dueDate: { [Op.between]: [now, inOneHour] }, status: 'Open' },
    });

    for (const task of dueSoonTasks) {
      const tokens = await DeviceToken.findAll({ where: { userId: task.userId } });
      if (tokens.length === 0) continue;

      await messaging.sendEachForMulticast({
        tokens: tokens.map((t) => t.fcmToken),
        notification: {
          title: 'Task due soon',
          body: `"${task.title}" is due within the hour.`,
        },
      });
    }
  });

  console.log('Reminder job scheduled (every 15 minutes).');
}

module.exports = startReminderJob;
