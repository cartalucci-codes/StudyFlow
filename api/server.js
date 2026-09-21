require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { sequelize } = require('./models');

const authRoutes = require('./routes/auth');
const taskRoutes = require('./routes/tasks');
const studySessionRoutes = require('./routes/studysessions');
const userRoutes = require('./routes/users');
const notificationRoutes = require('./routes/notifications');
const startReminderJob = require('./jobs/reminderJob');

const app = express();
app.use(cors());
app.use(express.json());

app.get('/', (req, res) => res.json({ status: 'StudyFlow API running' }));

app.use('/api/auth', authRoutes);
app.use('/api/tasks', taskRoutes);
app.use('/api/studysessions', studySessionRoutes);
app.use('/api/users', userRoutes);
app.use('/api/notifications', notificationRoutes);

const PORT = process.env.PORT || 3000;

// Only auto-start the server (and DB sync) when run directly, not when imported by tests.
if (require.main === module) {
  sequelize.sync().then(() => {
    app.listen(PORT, () => console.log(`StudyFlow API listening on port ${PORT}`));
    startReminderJob();
  });
}

module.exports = app;
