const cron = require('node-cron');
const { Op } = require('sequelize');
const { Task } = require('../models');
const { messaging } = require('../config/firebaseAdmin');
const { sendToUser } = require('../services/push');

const MINUTE = 60 * 1000;

// Runs every 15 minutes and pushes:
//   - "task due soon" for open tasks due 45-60 minutes from now, and
//   - "deadline missed" for open tasks that became overdue in the last 15 minutes.
// Each window is exactly one cron interval wide, so every task is notified about once without
// needing to store any extra "already reminded" state.
async function runReminderCheck(now = Date.now()) {
  const dueSoon = await Task.findAll({
    where: {
      status: 'Open',
      dueDate: { [Op.gte]: new Date(now + 45 * MINUTE), [Op.lt]: new Date(now + 60 * MINUTE) },
    },
  });
  for (const task of dueSoon) {
    await sendToUser(task.userId, 'Task due soon', `"${task.title}" is due within the hour.`);
  }

  const missed = await Task.findAll({
    where: {
      status: 'Open',
      dueDate: { [Op.gte]: new Date(now - 15 * MINUTE), [Op.lt]: new Date(now) },
    },
  });
  for (const task of missed) {
    await sendToUser(task.userId, 'Deadline missed', `"${task.title}" was due and is still open.`);
  }
  return { dueSoon: dueSoon.length, missed: missed.length };
}

function startReminderJob() {
  if (!messaging) {
    console.log('Reminder job not started - push notifications are not configured.');
    return;
  }
  cron.schedule('*/15 * * * *', () => {
    runReminderCheck().catch((err) => console.error('Reminder job failed:', err));
  });
  console.log('Reminder job scheduled (every 15 minutes).');
}

module.exports = startReminderJob;
module.exports.runReminderCheck = runReminderCheck;
