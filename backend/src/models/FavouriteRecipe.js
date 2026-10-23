const mongoose = require('mongoose');

const favouriteRecipeSchema = new mongoose.Schema(
  {
    userId: { type: String, required: true, index: true },
    recipeSourceId: { type: String, required: true },
    recipeTitle: { type: String, required: true },
    imageUrl: { type: String, default: null },
  },
  { timestamps: true }
);

// A user can only favourite the same recipe once.
favouriteRecipeSchema.index({ userId: 1, recipeSourceId: 1 }, { unique: true });

module.exports = mongoose.model('FavouriteRecipe', favouriteRecipeSchema);