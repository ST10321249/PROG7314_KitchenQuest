const mongoose = require('mongoose');
const env = require('./env');

mongoose.connection.on('error', (err) => {
  console.error('MongoDB connection error:', err.message);
});

mongoose.connection.on('disconnected', () => {
  console.warn('MongoDB disconnected');
});

async function connectDB() {
  await mongoose.connect(env.mongodbUri);
  console.log('MongoDB connected');
}

module.exports = connectDB;
