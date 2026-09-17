const app = require('./app');
const env = require('./config/env');
const connectDB = require('./config/db');

connectDB();

app.listen(env.port, () => {
  console.log(`KitchenQuest API listening on port ${env.port}`);
});
