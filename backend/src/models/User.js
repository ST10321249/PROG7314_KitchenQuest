const mongoose = require('mongoose');

const userSchema = new mongoose.Schema(
  {
    firebaseUid: { type: String, required: true, unique: true, index: true },
    displayName: { type: String, default: '' },
    email: { type: String, default: '' },
    dietaryPreferences: { type: [String], default: [] },
    avoidedIngredients: { type: [String], default: [] },
    // Optional for existing accounts; their language is not changed during migration.
    language: { type: String, enum: ['en', 'af'] },
  },
  { timestamps: true }
);

module.exports = mongoose.model('User', userSchema);
