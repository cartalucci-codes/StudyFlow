const request = require('supertest');
const { v4: uuidv4 } = require('uuid');
process.env.DB_STORAGE = ':memory:';
const app = require('../server');
const { sequelize } = require('../models');

let token;
const auth = () => ({ Authorization: `Bearer ${token}` });

beforeAll(async () => {
  await sequelize.sync({ force: true });
  const res = await request(app).post('/api/auth/register').send({
    name: 'Focus Student', email: 'focus@studyflow.dev', password: 'Password123!',
  });
  token = res.body.token;
});

afterAll(async () => {
  await sequelize.close();
});

describe('Study sessions, XP and badges', () => {
  test('a completed session awards 1 XP per minute and unlocks the first-session badge', async () => {
    const sessionId = uuidv4();
    const res = await request(app)
      .post('/api/studysessions')
      .set(auth())
      .send({ sessionId, startTime: new Date().toISOString(), durationMins: 25, completed: true });
    expect(res.statusCode).toBe(201);
    expect(res.body.newBadges).toContain('First session');

    const me = await request(app).get('/api/users/me').set(auth());
    expect(me.body.xpPoints).toBe(25);

    const badges = await request(app).get('/api/badges').set(auth());
    expect(badges.body.map((b) => b.badgeType)).toContain('First session');
  });

  test('re-syncing the same session is idempotent (no double XP)', async () => {
    const sessionId = uuidv4();
    const body = { sessionId, startTime: new Date().toISOString(), durationMins: 10, completed: true };
    await request(app).post('/api/studysessions').set(auth()).send(body);
    const again = await request(app).post('/api/studysessions').set(auth()).send(body);
    expect(again.statusCode).toBe(200);

    const me = await request(app).get('/api/users/me').set(auth());
    expect(me.body.xpPoints).toBe(35); // 25 + 10, not 25 + 10 + 10
  });

  test('abandoned sessions are stored but award no XP', async () => {
    await request(app)
      .post('/api/studysessions')
      .set(auth())
      .send({ sessionId: uuidv4(), startTime: new Date().toISOString(), durationMins: 5, completed: false });
    const me = await request(app).get('/api/users/me').set(auth());
    expect(me.body.xpPoints).toBe(35);
  });

  test('rejects invalid durations', async () => {
    const res = await request(app)
      .post('/api/studysessions')
      .set(auth())
      .send({ startTime: new Date().toISOString(), durationMins: 0, completed: true });
    expect(res.statusCode).toBe(400);
  });
});

describe('Account management', () => {
  test('settings update validates language and theme', async () => {
    const bad = await request(app).put('/api/users/me').set(auth()).send({ language: 'fr' });
    expect(bad.statusCode).toBe(400);
    const ok = await request(app).put('/api/users/me').set(auth()).send({ language: 'zu', themePref: 'dark' });
    expect(ok.statusCode).toBe(200);
    expect(ok.body.language).toBe('zu');
  });

  test('push test returns 503 when Firebase is not configured', async () => {
    const res = await request(app).post('/api/notifications/send-test').set(auth()).send({});
    expect(res.statusCode).toBe(503);
  });

  test('deleting the account removes the user and their data', async () => {
    const del = await request(app).delete('/api/users/me').set(auth());
    expect(del.statusCode).toBe(200);
    const { User, StudySession } = require('../models');
    expect(await User.count()).toBe(0);
    expect(await StudySession.count()).toBe(0);
  });
});
