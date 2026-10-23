const RecipeNote = require('../models/RecipeNote');

function toDto(note) {
  return {
    recipeSourceId: note.recipeSourceId,
    note: note.note,
    updatedAt: note.updatedAt.toISOString(),
  };
}

async function getNote(req, res, next) {
  try {
    const note = await RecipeNote.findOne({
      userId: req.user.uid,
      recipeSourceId: req.params.recipeId,
    });


    if (!note) {
      return res.status(200).json(null);
    }

    res.status(200).json(toDto(note));
  } catch (err) {
    next(err);
  }
}

async function saveNote(req, res, next) {
  try {
    const { recipeSourceId, note } = req.body;

    const saved = await RecipeNote.findOneAndUpdate(
      { userId: req.user.uid, recipeSourceId },
      { $set: { note } },
      { new: true, upsert: true }
    );

    res.status(200).json(toDto(saved));
  } catch (err) {
    next(err);
  }
}

module.exports = { getNote, saveNote };