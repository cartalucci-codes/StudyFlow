const request = require('supertest');
const { v4: uuidv4 } = require('uuid');
process.env.DB_STORAGE = ':memory:';
const app = require('../server');
const { sequelize } = require('../models');

let token;
let otherToken;
const auth = (t) => ({ Authorization: `Bearer ${t}` });

beforeAll(async () => {
  await sequelize.sync({ force: true });
  const a = await request(app).post('/api/auth/register').send({
    name: 'Task Tester', email: 'tasks@studyflow.dev', password: 'Password123!',
  });
  token = a.body.token;
  const b = await request(app).post('/api/auth/register').send({
    name: 'Other Student', email: 'other@studyflow.dev', password: 'Password123!',
  });
  otherToken = b.body.token;
});

afterAll(async () => {
  await sequelize.close();
});

describe('Task endpoints', () => {
  test('rejects unauthenticated requests', async () => {
    const res = await request(app).get('/api/tasks');
    expect(res.statusCode).toBe(401);
  });

  test('creates and lists a task for the logged-in user', async () => {
    const createRes = await request(app)
      .post('/api/tasks')
      .set(auth(token))
      .send({ title: 'Finish Part 2', subject: 'OPSC6312', dueDate: '2026-10-12T18:00:00Z', priority: 'High' });
    expect(createRes.statusCode).toBe(201);

    const listRes = await request(app).get('/api/tasks').set(auth(token));
    expect(listRes.statusCode).toBe(200);
    expect(listRes.body.length).toBe(1);
    expect(listRes.body[0].title).toBe('Finish Part 2');
  });

  test('offline sync is idempotent: pushing the same client-generated id twice does not duplicate', async () => {
    const taskId = uuidv4();
    const body = { taskId, title: 'Offline task', dueDate: '2026-11-01T10:00:00Z' };
    const first = await request(app).post('/api/tasks').set(auth(token)).send(body);
    const second = await request(app).post('/api/tasks').set(auth(token)).send({ ...body, title: 'Offline task (edited)' });
    expect(first.statusCode).toBe(201);
    expect(second.statusCode).toBe(200);
    expect(second.body.taskId).toBe(taskId);

    const list = await request(app).get('/api/tasks').set(auth(token));
    const matches = list.body.filter((t) => t.taskId === taskId);
    expect(matches.length).toBe(1);
    expect(matches[0].title).toBe('Offline task (edited)');
  });

  test('completing a task awards XP exactly once', async () => {
    const taskId = uuidv4();
    const base = { taskId, title: 'XP task', dueDate: '2026-11-02T10:00:00Z' };
    await request(app).post('/api/tasks').set(auth(token)).send(base);
    const before = (await request(app).get('/api/users/me').set(auth(token))).body.xpPoints;

    await request(app).post('/api/tasks').set(auth(token)).send({ ...base, status: 'Done' });
    await request(app).post('/api/tasks').set(auth(token)).send({ ...base, status: 'Done' }); // re-sync
    const after = (await request(app).get('/api/users/me').set(auth(token))).body.xpPoints;
    expect(after - before).toBe(10);
  });

  test('invalid input returns 400 and does not crash the API', async () => {
    const bad = [
      { dueDate: '2026-11-01T10:00:00Z' }, // no title
      { title: 'x', dueDate: 'not-a-date' },
      { title: 'x', dueDate: '2026-11-01T10:00:00Z', priority: 'Urgent' },
      { title: 'x', dueDate: '2026-11-01T10:00:00Z', taskId: 'not-a-uuid' },
    ];
    for (const body of bad) {
      const res = await request(app).post('/api/tasks').set(auth(token)).send(body);
      expect(res.statusCode).toBe(400);
    }
    const stillUp = await request(app).get('/');
    expect(stillUp.statusCode).toBe(200);
  });

  test("users cannot see or change each other's tasks", async () => {
    const list = await request(app).get('/api/tasks').set(auth(otherToken));
    expect(list.body.length).toBe(0);

    const mine = (await request(app).get('/api/tasks').set(auth(token))).body[0];
    const hijack = await request(app).put(`/api/tasks/${mine.taskId}`).set(auth(otherToken)).send({ title: 'hacked' });
    expect(hijack.statusCode).toBe(404);
    const steal = await request(app)
      .post('/api/tasks')
      .set(auth(otherToken))
      .send({ taskId: mine.taskId, title: 'steal', dueDate: '2026-11-01T10:00:00Z' });
    expect(steal.statusCode).toBe(409);
  });

  test('deletes a task', async () => {
    const mine = (await request(app).get('/api/tasks').set(auth(token))).body[0];
    const del = await request(app).delete(`/api/tasks/${mine.taskId}`).set(auth(token));
    expect(del.statusCode).toBe(200);
    const again = await request(app).delete(`/api/tasks/${mine.taskId}`).set(auth(token));
    expect(again.statusCode).toBe(404);
  });
});
