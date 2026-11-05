const mongoose = require('mongoose');
const CookingHistory = require('../models/CookingHistory');
const fileStorage = require('../services/fileStorage');
const ApiError = require('../utils/ApiError');

function toDto(entry) {
  return {
    id: entry._id.toString(),
    recipeSourceId: entry.recipeSourceId,
    recipeTitle: entry.recipeTitle,
    cookedAt: entry.cookedAt.toISOString(),
    servings: entry.servings,
    rating: entry.rating,
    difficultyFeedback: entry.difficultyFeedback,
    photoFileId: entry.photoFileId ?? null,
  };
}

// A photo can only be attached if it exists and belongs to the same user.
async function requireOwnPhoto(fileId, userId) {
  const file = await fileStorage.findOwned(fileId, userId);
  if (!file) {
    throw new ApiError(400, 'Photo not found');
  }
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
    const { recipeSourceId, recipeTitle, servings, rating, difficultyFeedback, photoFileId } =
      req.body;

    if (photoFileId) {
      await requireOwnPhoto(photoFileId, req.user.uid);
    }

    const entry = await CookingHistory.create({
      userId: req.user.uid,
      recipeSourceId,
      recipeTitle,
      servings: servings ?? null,
      rating: rating ?? null,
      difficultyFeedback: difficultyFeedback ?? null,
      photoFileId: photoFileId ?? null,
    });

    res.status(201).json(toDto(entry));
  } catch (err) {
    next(err);
  }
}

// Attaches a photo to a history entry, or removes it when photoFileId is null. A photo that
// is replaced or removed is deleted too, unless another entry still uses it.
async function updateHistoryEntry(req, res, next) {
  try {
    const userId = req.user.uid;

    if (!mongoose.isValidObjectId(req.params.id)) {
      throw new ApiError(404, 'History entry not found');
    }

    const entry = await CookingHistory.findOne({ _id: req.params.id, userId });
    if (!entry) {
      throw new ApiError(404, 'History entry not found');
    }

    const newPhotoId = req.body.photoFileId;
    if (newPhotoId) {
      await requireOwnPhoto(newPhotoId, userId);
    }

    const previousPhotoId = entry.photoFileId;
    entry.photoFileId = newPhotoId;
    await entry.save();

    if (previousPhotoId && previousPhotoId !== newPhotoId) {
      const stillUsed = await CookingHistory.exists({
        userId,
        photoFileId: previousPhotoId,
        _id: { $ne: entry._id },
      });
      const previous = await fileStorage.findOwned(previousPhotoId, userId);
      if (!stillUsed && previous) {
        await fileStorage.remove(previous.id);
      }
    }

    res.status(200).json(toDto(entry));
  } catch (err) {
    next(err);
  }
}

module.exports = { getHistory, addHistoryEntry, updateHistoryEntry };
