const mongoose = require('mongoose');

const cookingHistorySchema = new mongoose.Schema(
  {
    userId: { type: String, required: true, index: true },
    clientId: { type: String, trim: true },
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

// Replaying a cooking-completion request must not create duplicate records.
cookingHistorySchema.index(
  { userId: 1, clientId: 1 },
  { unique: true, partialFilterExpression: { clientId: { $type: 'string' } } }
);

module.exports = mongoose.model('CookingHistory', cookingHistorySchema);