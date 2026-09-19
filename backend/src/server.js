const env = require('./config/env');
const { missingEnvVars } = require('./config/validateEnv');

const missing = missingEnvVars(env);

if (missing.length > 0) {
  console.error(
    `Missing required environment variables: ${missing.join(', ')}. ` +
      'Copy .env.example to .env and fill them in.'
  );
  process.exit(1);
}

const app = require('./app');
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
