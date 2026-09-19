const path = require('path');
const { initializeApp, cert, getApps } = require('firebase-admin/app');
const env = require('./env');

function loadServiceAccount() {
  try {
    return require(path.resolve(env.firebaseServiceAccountPath));
  } catch (err) {
    throw new Error(
      `Could not load the Firebase service account key at "${env.firebaseServiceAccountPath}": ${err.message}`
    );
  }
}

if (!getApps().length) {
  initializeApp({
    credential: cert(loadServiceAccount()),
  });
}

module.exports = { getApps };
