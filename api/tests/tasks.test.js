const request = require('supertest');
process.env.DB_STORAGE = ':memory:';
const app = require('../server');
const { sequelize } = require('../models');

let token;

beforeAll(async () => {
  await sequelize.sync({ force: true });
  const res = await request(app).post('/api/auth/register').send({
    name: 'Task Tester', email: 'tasks@studyflow.dev', password: 'Password123!',
  });
  token = res.body.token;
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
      .set('Authorization', `Bearer ${token}`)
      .send({ title: 'Finish Part 2', subject: 'OPSC6312', dueDate: '2026-10-12T18:00:00Z', priority: 'High' });
    expect(createRes.statusCode).toBe(201);

    const listRes = await request(app).get('/api/tasks').set('Authorization', `Bearer ${token}`);
    expect(listRes.statusCode).toBe(200);
    expect(listRes.body.length).toBe(1);
    expect(listRes.body[0].title).toBe('Finish Part 2');
  });
});
