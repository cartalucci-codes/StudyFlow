const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');
const sequelize = require('../config/database');

const User = sequelize.define('User', {
  userId: { type: DataTypes.UUID, defaultValue: uuidv4, primaryKey: true },
  name: { type: DataTypes.STRING(100), allowNull: false },
  email: { type: DataTypes.STRING(150), allowNull: false, unique: true },
  passwordHash: { type: DataTypes.STRING(255), allowNull: true }, // null for Google SSO-only accounts
  language: { type: DataTypes.STRING(10), defaultValue: 'en' },
  themePref: { type: DataTypes.STRING(10), defaultValue: 'light' },
  xpPoints: { type: DataTypes.INTEGER, defaultValue: 0 },
});

const Task = sequelize.define('Task', {
  taskId: { type: DataTypes.UUID, defaultValue: uuidv4, primaryKey: true },
  userId: { type: DataTypes.UUID, allowNull: false },
  title: { type: DataTypes.STRING(150), allowNull: false },
  description: { type: DataTypes.TEXT, allowNull: true },
  subject: { type: DataTypes.STRING(60), allowNull: true },
  dueDate: { type: DataTypes.DATE, allowNull: false },
  priority: { type: DataTypes.ENUM('Low', 'Med', 'High'), defaultValue: 'Med' },
  status: { type: DataTypes.ENUM('Open', 'Done'), defaultValue: 'Open' },
  reminderTime: { type: DataTypes.DATE, allowNull: true },
});

const StudySession = sequelize.define('StudySession', {
  sessionId: { type: DataTypes.UUID, defaultValue: uuidv4, primaryKey: true },
  userId: { type: DataTypes.UUID, allowNull: false },
  taskId: { type: DataTypes.UUID, allowNull: true },
  startTime: { type: DataTypes.DATE, allowNull: false },
  durationMins: { type: DataTypes.INTEGER, allowNull: false },
  completed: { type: DataTypes.BOOLEAN, defaultValue: false },
});

const Badge = sequelize.define('Badge', {
  badgeId: { type: DataTypes.UUID, defaultValue: uuidv4, primaryKey: true },
  userId: { type: DataTypes.UUID, allowNull: false },
  badgeType: { type: DataTypes.STRING(40), allowNull: false },
  dateEarned: { type: DataTypes.DATE, defaultValue: DataTypes.NOW },
});

const DeviceToken = sequelize.define('DeviceToken', {
  tokenId: { type: DataTypes.UUID, defaultValue: uuidv4, primaryKey: true },
  userId: { type: DataTypes.UUID, allowNull: false },
  fcmToken: { type: DataTypes.STRING(255), allowNull: false },
});

User.hasMany(Task, { foreignKey: 'userId' });
User.hasMany(StudySession, { foreignKey: 'userId' });
User.hasMany(Badge, { foreignKey: 'userId' });
User.hasMany(DeviceToken, { foreignKey: 'userId' });
Task.hasMany(StudySession, { foreignKey: 'taskId' });

module.exports = { sequelize, User, Task, StudySession, Badge, DeviceToken };
