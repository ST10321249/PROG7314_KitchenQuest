const User = require('../models/User');

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

module.exports = { syncUser };
