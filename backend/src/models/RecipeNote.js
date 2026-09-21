const mongoose = require('mongoose');

const recipeNoteSchema = new mongoose.Schema(
  {
    userId: { type: String, required: true, index: true },
    recipeSourceId: { type: String, required: true },
    note: { type: String, required: true },
  },
  { timestamps: true }
);

// One note per user per recipe — saving again overwrites the previous note.
recipeNoteSchema.index({ userId: 1, recipeSourceId: 1 }, { unique: true });

module.exports = mongoose.model('RecipeNote', recipeNoteSchema);