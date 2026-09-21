const admin = require('firebase-admin');
const fs = require('fs');
require('dotenv').config();

let messaging = null;

const keyPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH || './firebase-service-account.json';

if (fs.existsSync(keyPath)) {
  const serviceAccount = require(require('path').resolve(keyPath));
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
  messaging = admin.messaging();
  console.log('Firebase Admin initialised - push notifications enabled.');
} else {
  console.log('No Firebase service account found at', keyPath, '- push notifications disabled (everything else still works).');
}

module.exports = { messaging };
