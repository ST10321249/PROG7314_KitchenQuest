const path = require('path');
const { initializeApp, cert, getApps } = require('firebase-admin/app');
const env = require('./env');

if (!getApps().length) {
  const serviceAccount = require(path.resolve(env.firebaseServiceAccountPath));

  initializeApp({
    credential: cert(serviceAccount),
  });
}

module.exports = { getApps };
