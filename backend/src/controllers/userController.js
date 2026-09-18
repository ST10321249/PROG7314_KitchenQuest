const User = require('../models/User');
const ApiError = require('../utils/ApiError');

async function syncUser(req, res, next) {
  try {
    const { uid, email, name } = req.user;
    const { displayName, dietaryPreferences, avoidedIngredients } = req.body;

    const result = await User.findOneAndUpdate(
      { firebaseUid: uid },
      {
        $setOnInsert: {
          firebaseUid: uid,
          email: email || '',
          displayName: displayName || name || '',
          dietaryPreferences: dietaryPreferences || [],
          avoidedIngredients: avoidedIngredients || [],
        },
      },
      { upsert: true, new: true, includeResultMetadata: true }
    );

    const created = !result.lastErrorObject.updatedExisting;
    res.status(created ? 201 : 200).json(result.value);
  } catch (err) {
    next(err);
  }
}

async function getMe(req, res, next) {
  try {
    const user = await User.findOne({ firebaseUid: req.user.uid });

    if (!user) {
      throw new ApiError(404, 'User profile not found. Sync the user first.');
    }

    res.status(200).json(user);
  } catch (err) {
    next(err);
  }
}

module.exports = { syncUser, getMe };
