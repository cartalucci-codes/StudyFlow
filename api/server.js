require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { sequelize } = require('./models');

const authRoutes = require('./routes/auth');
const taskRoutes = require('./routes/tasks');
const studySessionRoutes = require('./routes/studysessions');
const userRoutes = require('./routes/users');
const notificationRoutes = require('./routes/notifications');
const badgeRoutes = require('./routes/badges');
const startReminderJob = require('./jobs/reminderJob');

const app = express();
app.use(cors());
app.use(express.json());

app.get('/', (req, res) => res.json({ status: 'StudyFlow API running', version: '2.0.0' }));

app.use('/api/auth', authRoutes);
app.use('/api/tasks', taskRoutes);
app.use('/api/studysessions', studySessionRoutes);
app.use('/api/users', userRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/badges', badgeRoutes);

app.use((req, res) => res.status(404).json({ error: 'Not found' }));

// Central error handler: bad input or a database problem returns a clean JSON error
// instead of crashing the server.
// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  if (err.name === 'SequelizeValidationError') return res.status(400).json({ error: 'Invalid data' });
  if (err.name === 'SequelizeUniqueConstraintError') return res.status(409).json({ error: 'Already exists' });
  if (err.status && err.status >= 400 && err.status < 500) {
    return res.status(err.status).json({ error: err.message || 'Bad request' });
  }
  console.error(err);
  res.status(500).json({ error: 'Internal server error' });
});

const PORT = process.env.PORT || 3000;

// Azure SQL's free/serverless tier pauses when idle and can take ~1 minute to wake up, so the first
// connection attempt after a pause may fail. Retry for a while instead of giving up immediately.
async function connectWithRetry(attempts = 12, delayMs = 10000) {
  for (let i = 1; i <= attempts; i += 1) {
    try {
      await sequelize.authenticate();
      return;
    } catch (err) {
      console.warn(`Database connection attempt ${i}/${attempts} failed: ${err.message}`);
      if (i === attempts) throw err;
      await new Promise((resolve) => setTimeout(resolve, delayMs));
    }
  }
}

// Only auto-start the server (and DB sync) when run directly, not when imported by tests.
if (require.main === module) {
  connectWithRetry()
    .then(() => sequelize.sync())
    .then(() => {
      app.listen(PORT, () => console.log(`StudyFlow API listening on port ${PORT}`));
      startReminderJob();
    })
    .catch((err) => {
      console.error('Could not connect to / sync the database:', err.message);
      process.exit(1);
    });
}

module.exports = app;
