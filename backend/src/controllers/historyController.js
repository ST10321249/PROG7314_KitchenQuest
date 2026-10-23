const CookingHistory = require('../models/CookingHistory');

function toDto(entry) {
  return {
    id: entry._id.toString(),
    recipeSourceId: entry.recipeSourceId,
    recipeTitle: entry.recipeTitle,
    cookedAt: entry.cookedAt.toISOString(),
    servings: entry.servings,
    rating: entry.rating,
    difficultyFeedback: entry.difficultyFeedback,
  };
}

async function getHistory(req, res, next) {
  try {
    const entries = await CookingHistory.find({ userId: req.user.uid }).sort({ cookedAt: -1 });
    res.status(200).json(entries.map(toDto));
  } catch (err) {
    next(err);
  }
}

async function addHistoryEntry(req, res, next) {
  try {
    const { recipeSourceId, recipeTitle, servings, rating, difficultyFeedback } = req.body;

    const entry = await CookingHistory.create({
      userId: req.user.uid,
      recipeSourceId,
      recipeTitle,
      servings: servings ?? null,
      rating: rating ?? null,
      difficultyFeedback: difficultyFeedback ?? null,
    });

    res.status(201).json(toDto(entry));
  } catch (err) {
    next(err);
  }
}

module.exports = { getHistory, addHistoryEntry };