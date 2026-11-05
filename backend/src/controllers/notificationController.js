const DeviceToken = require('../models/DeviceToken');
const { sendToTokens, sendToUser } = require('../services/notificationService');
const ApiError = require('../utils/ApiError');

const TEST_MESSAGE = {
  title: 'KitchenQuest',
  body: 'Push notifications are working.',
  data: { type: 'test' },
};

// Sends a test notification to the signed-in user's own devices, or to just one of them if a
// token is given. It can never reach anyone else's phone.
async function sendTestNotification(req, res, next) {
  try {
    const userId = req.user.uid;
    let result;

    if (req.body.token) {
      const device = await DeviceToken.findOne({ userId, token: req.body.token });
      result = device ? await sendToTokens([device.token], TEST_MESSAGE) : null;
    } else {
      result = await sendToUser(userId, TEST_MESSAGE);
    }

    if (!result) {
      throw new ApiError(404, 'No registered device found for this account');
    }

    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
}

module.exports = { sendTestNotification };
