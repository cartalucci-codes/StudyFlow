const request = require('supertest');
process.env.DB_STORAGE = ':memory:'; // isolated in-memory DB for tests
const app = require('../server');
const { sequelize } = require('../models');

beforeAll(async () => {
  await sequelize.sync({ force: true });
});

afterAll(async () => {
  await sequelize.close();
});

describe('Auth endpoints', () => {
  const testUser = { name: 'Test Student', email: 'test@studyflow.dev', password: 'Password123!' };

  test('registers a new user and returns a JWT', async () => {
    const res = await request(app).post('/api/auth/register').send(testUser);
    expect(res.statusCode).toBe(201);
    expect(res.body.token).toBeDefined();
    expect(res.body.user.email).toBe(testUser.email);
    expect(res.body.user.passwordHash).toBeUndefined(); // never leaks the hash
  });

  test('stores a bcrypt hash, never the plain text password', async () => {
    const { User } = require('../models');
    const user = await User.findOne({ where: { email: testUser.email } });
    expect(user.passwordHash).not.toBe(testUser.password);
    expect(user.passwordHash.startsWith('$2')).toBe(true); // bcrypt prefix
  });

  test('rejects duplicate email registration (case-insensitive)', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .send({ ...testUser, email: 'TEST@studyflow.dev' });
    expect(res.statusCode).toBe(409);
  });

  test('rejects invalid email, short password and missing name', async () => {
    const bad = [
      { name: 'A', email: 'not-an-email', password: 'Password123!' },
      { name: 'A', email: 'a@b.co', password: 'short' },
      { name: '', email: 'c@d.co', password: 'Password123!' },
    ];
    for (const body of bad) {
      const res = await request(app).post('/api/auth/register').send(body);
      expect(res.statusCode).toBe(400);
    }
  });

  test('logs in with correct credentials', async () => {
    const res = await request(app).post('/api/auth/login').send({
      email: testUser.email,
      password: testUser.password,
    });
    expect(res.statusCode).toBe(200);
    expect(res.body.token).toBeDefined();
  });

  test('rejects login with wrong password', async () => {
    const res = await request(app).post('/api/auth/login').send({
      email: testUser.email,
      password: 'wrong-password',
    });
    expect(res.statusCode).toBe(401);
  });

  test('malformed JSON returns 400 instead of crashing', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .set('Content-Type', 'application/json')
      .send('{"email": ');
    expect(res.statusCode).toBe(400);
  });
});
