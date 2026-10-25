const mongoose = require('mongoose');

const cookingHistorySchema = new mongoose.Schema(
  {
    userId: { type: String, required: true, index: true },
    recipeSourceId: { type: String, required: true },
    recipeTitle: { type: String, required: true },
    cookedAt: { type: Date, default: Date.now },
    servings: { type: Number, default: null },
    rating: { type: Number, min: 1, max: 5, default: null },
    // The id of a photo of the finished dish, stored with the file endpoints.
    photoFileId: { type: String, default: null },
    difficultyFeedback: { type: String, enum: ['Easy', 'Medium', 'Hard', null], default: null },
  },
  { timestamps: true }
);

module.exports = mongoose.model('CookingHistory', cookingHistorySchema);