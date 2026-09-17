const app = require('./app');
const env = require('./config/env');
const connectDB = require('./config/db');
require('./config/firebaseAdmin');

async function start() {
  try {
    await connectDB();
  } catch (err) {
    console.error('Failed to connect to MongoDB, exiting:', err.message);
    process.exit(1);
  }

  app.listen(env.port, () => {
    console.log(`KitchenQuest API listening on port ${env.port}`);
  });
}

start();
