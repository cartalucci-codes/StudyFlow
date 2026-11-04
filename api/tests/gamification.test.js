process.env.DB_STORAGE = ':memory:';
const { currentStreak, longestStreak } = require('../services/gamification');
const { sequelize } = require('../models');

afterAll(async () => {
  await sequelize.close();
});

describe('streak maths', () => {
  const today = new Date('2026-10-30T12:00:00Z');

  test('counts consecutive days ending today', () => {
    expect(currentStreak(['2026-10-30', '2026-10-29', '2026-10-28'], today)).toBe(3);
  });

  test('a streak survives until a whole day is missed (ending yesterday still counts)', () => {
    expect(currentStreak(['2026-10-29', '2026-10-28'], today)).toBe(2);
  });

  test('a gap resets the streak', () => {
    expect(currentStreak(['2026-10-30', '2026-10-27'], today)).toBe(1);
    expect(currentStreak(['2026-10-27', '2026-10-26'], today)).toBe(0);
  });

  test('longest streak finds the best run, ignoring duplicates', () => {
    const days = ['2026-10-01', '2026-10-02', '2026-10-02', '2026-10-03', '2026-10-10', '2026-10-11'];
    expect(longestStreak(days)).toBe(3);
    expect(longestStreak([])).toBe(0);
  });
});
