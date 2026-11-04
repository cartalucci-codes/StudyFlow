const { User, StudySession, Badge } = require('../models');

const XP_PER_TASK = 10; // XP for completing a task (study sessions award 1 XP per focused minute)

// 'YYYY-MM-DD' (UTC) for a date-like value
function dayKey(date) {
  return new Date(date).toISOString().slice(0, 10);
}

// Length of the streak of consecutive study days that ends today (or yesterday, so a streak
// isn't "lost" until a whole day has been missed).
function currentStreak(dayKeys, today = new Date()) {
  const set = new Set(dayKeys);
  const cursor = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()));
  if (!set.has(dayKey(cursor))) cursor.setUTCDate(cursor.getUTCDate() - 1);
  let streak = 0;
  while (set.has(dayKey(cursor))) {
    streak += 1;
    cursor.setUTCDate(cursor.getUTCDate() - 1);
  }
  return streak;
}

// Longest run of consecutive study days ever recorded (badges are kept once earned).
function longestStreak(dayKeys) {
  const days = [...new Set(dayKeys)].sort();
  let best = 0;
  let run = 0;
  let prev = null;
  for (const d of days) {
    const t = Date.parse(`${d}T00:00:00Z`);
    run = prev !== null && t - prev === 86400000 ? run + 1 : 1;
    prev = t;
    best = Math.max(best, run);
  }
  return best;
}

const BADGES = [
  { type: 'First session', earned: (s) => s.sessions >= 1 },
  { type: '3-day streak', earned: (s) => s.longest >= 3 },
  { type: '7-day streak', earned: (s) => s.longest >= 7 },
  { type: '30-day streak', earned: (s) => s.longest >= 30 },
];

async function awardXp(userId, amount) {
  if (amount > 0) await User.increment('xpPoints', { by: amount, where: { userId } });
}

// Awards any badges the user has newly qualified for. Returns the newly created badges.
async function evaluateBadges(userId) {
  const sessions = await StudySession.findAll({ where: { userId, completed: true } });
  const stats = {
    sessions: sessions.length,
    longest: longestStreak(sessions.map((s) => dayKey(s.startTime))),
  };
  const have = (await Badge.findAll({ where: { userId } })).map((b) => b.badgeType);
  const created = [];
  for (const b of BADGES) {
    if (b.earned(stats) && !have.includes(b.type)) {
      created.push(await Badge.create({ userId, badgeType: b.type }));
    }
  }
  return created;
}

module.exports = { XP_PER_TASK, dayKey, currentStreak, longestStreak, awardXp, evaluateBadges };
