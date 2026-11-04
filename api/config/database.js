const { Sequelize } = require('sequelize');
require('dotenv').config();

let sequelize;

if (process.env.DB_DIALECT === 'mssql') {
  // Azure SQL Database (production). Credentials come from App Service "Application settings".
  sequelize = new Sequelize(process.env.DB_NAME, process.env.DB_USER, process.env.DB_PASS, {
    host: process.env.DB_HOST,
    port: Number(process.env.DB_PORT || 1433),
    dialect: 'mssql',
    dialectOptions: {
      options: {
        encrypt: true, // Azure SQL requires encrypted connections
        trustServerCertificate: false,
        connectTimeout: 60000, // allow time for a paused serverless database to wake up
        requestTimeout: 30000,
      },
    },
    pool: { max: 5, min: 0, idle: 10000 },
    logging: false,
  });
} else {
  // SQLite (local dev / GitHub Actions CI) - zero setup, same data model.
  sequelize = new Sequelize({
    dialect: 'sqlite',
    storage: process.env.DB_STORAGE || './studyflow.sqlite',
    logging: false,
  });
}

module.exports = sequelize;
