const mongoose = require('mongoose');

const userSchema = new mongoose.Schema(
  {
    firebaseUid: { type: String, required: true, unique: true, index: true },
    displayName: { type: String, default: '' },
    email: { type: String, default: '' },
    dietaryPreferences: { type: [String], default: [] },
    avoidedIngredients: { type: [String], default: [] },
  },
  { timestamps: true }
);

module.exports = mongoose.model('User', userSchema);
