const { getMessaging } = require('firebase-admin/messaging');
require('../config/firebaseAdmin');
const DeviceToken = require('../models/DeviceToken');

// Firebase accepts at most 500 tokens in one multicast request.
const BATCH_SIZE = 500;

// Errors that mean the token will never work again, so it should be forgotten.
const DEAD_TOKEN_ERRORS = new Set([
  'messaging/registration-token-not-registered',
  'messaging/invalid-registration-token',
]);

// FCM only accepts strings in the data payload.
function toStringMap(data = {}) {
  return Object.fromEntries(
    Object.entries(data)
      .filter(([, value]) => value !== undefined && value !== null)
      .map(([key, value]) => [key, String(value)])
  );
}

function chunk(items, size) {
  const chunks = [];
  for (let i = 0; i < items.length; i += size) {
    chunks.push(items.slice(i, i + size));
  }
  return chunks;
}

// Sends a notification to the given FCM tokens and forgets any that Firebase says are dead.
// Returns how many were delivered to Firebase, how many failed and how many were removed.
//
// "data" is delivered to the app along with the notification. Use a "type" key so the app
// knows what the notification is about, and a "route" key to open a screen when it is tapped.
async function sendToTokens(tokens, { title, body, data, channelId }) {
  let sent = 0;
  let failed = 0;
  const dead = [];

  for (const batch of chunk(tokens, BATCH_SIZE)) {
    const response = await getMessaging().sendEachForMulticast({
      tokens: batch,
      notification: { title, body },
      data: toStringMap(data),
      android: {
        priority: 'high',
        ...(channelId ? { notification: { channelId } } : {}),
      },
    });

    sent += response.successCount;
    failed += response.failureCount;

    response.responses.forEach((result, index) => {
      if (!result.success && DEAD_TOKEN_ERRORS.has(result.error && result.error.code)) {
        dead.push(batch[index]);
      }
    });
  }

  if (dead.length > 0) {
    await DeviceToken.deleteMany({ token: { $in: dead } });
  }

  return { sent, failed, removed: dead.length };
}

// Sends a notification to every device registered for a user.
// Returns null when the user has no registered devices.
async function sendToUser(userId, message) {
  const devices = await DeviceToken.find({ userId });
  if (devices.length === 0) {
    return null;
  }

  return sendToTokens(
    devices.map((device) => device.token),
    message
  );
}

module.exports = { sendToTokens, sendToUser };
