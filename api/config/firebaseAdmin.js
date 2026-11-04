const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

let messaging = null;

// Locally: put the downloaded key at ./firebase-service-account.json (gitignored).
// On Azure: set FIREBASE_SERVICE_ACCOUNT_BASE64 to the base64 of that JSON file instead, so the
// secret never has to live in the repository.
function loadServiceAccount() {
  if (process.env.FIREBASE_SERVICE_ACCOUNT_BASE64) {
    return JSON.parse(Buffer.from(process.env.FIREBASE_SERVICE_ACCOUNT_BASE64, 'base64').toString('utf8'));
  }
  const keyPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH || './firebase-service-account.json';
  if (fs.existsSync(keyPath)) {
    return JSON.parse(fs.readFileSync(path.resolve(keyPath), 'utf8'));
  }
  return null;
}

try {
  const serviceAccount = loadServiceAccount();
  if (serviceAccount) {
    admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
    messaging = admin.messaging();
    console.log('Firebase Admin initialised - push notifications enabled.');
  } else {
    console.log('No Firebase service account found - push notifications disabled (everything else still works).');
  }
} catch (err) {
  console.error('Firebase Admin failed to initialise - push disabled:', err.message);
}

module.exports = { messaging };
