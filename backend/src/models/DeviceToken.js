const mongoose = require('mongoose');

const SIXTY_DAYS_IN_SECONDS = 60 * 24 * 60 * 60;

const deviceTokenSchema = new mongoose.Schema(
  {
    userId: { type: String, required: true, index: true },
    // The Firebase Cloud Messaging token of one app installation.
    token: { type: String, required: true, unique: true },
    platform: { type: String, enum: ['android'], default: 'android' },
    // The app registers again each time it starts, which keeps this fresh. A token that
    // has not been seen for 60 days belongs to an app that is no longer used, so MongoDB
    // removes it automatically.
    lastSeenAt: { type: Date, default: Date.now, expires: SIXTY_DAYS_IN_SECONDS },
  },
  { timestamps: true }
);

module.exports = mongoose.model('DeviceToken', deviceTokenSchema);
