const DeviceToken = require('../models/DeviceToken');
const { tokenField } = require('../validators/deviceSchemas');
const ApiError = require('../utils/ApiError');

// Saves the FCM token of the phone the user is signed in on. Safe to call again at any time:
// the same token is only stored once, and if the phone was last used by a different account
// the token moves to the account that is signed in now.
async function registerDevice(req, res, next) {
  try {
    const device = await DeviceToken.findOneAndUpdate(
      { token: req.body.token },
      {
        $set: {
          userId: req.user.uid,
          platform: req.body.platform || 'android',
          lastSeenAt: new Date(),
        },
      },
      { upsert: true, new: true, setDefaultsOnInsert: true }
    );

    res.status(200).json({
      id: device._id.toString(),
      platform: device.platform,
      lastSeenAt: device.lastSeenAt ? device.lastSeenAt.toISOString() : null,
    });
  } catch (err) {
    next(err);
  }
}

// Forgets a phone's token, for example when the user signs out. It only ever removes the
// signed-in user's own tokens, and succeeds even if the token was already gone.
async function unregisterDevice(req, res, next) {
  try {
    const parsed = tokenField.safeParse(req.params.token);
    if (!parsed.success) {
      throw new ApiError(400, 'Invalid token');
    }

    await DeviceToken.deleteOne({ userId: req.user.uid, token: parsed.data });
    res.status(204).send();
  } catch (err) {
    next(err);
  }
}

module.exports = { registerDevice, unregisterDevice };
