const FavouriteRecipe = require('../models/FavouriteRecipe');
const ApiError = require('../utils/ApiError');

function toDto(favourite) {
  return {
    id: favourite._id.toString(),
    recipeSourceId: favourite.recipeSourceId,
    recipeTitle: favourite.recipeTitle,
    imageUrl: favourite.imageUrl,
  };
}

async function getFavourites(req, res, next) {
  try {
    const favourites = await FavouriteRecipe.find({ userId: req.user.uid }).sort({ createdAt: -1 });
    res.status(200).json(favourites.map(toDto));
  } catch (err) {
    next(err);
  }
}

async function addFavourite(req, res, next) {
  try {
    const { recipeSourceId, recipeTitle, imageUrl } = req.body;

    // Favouriting an already-favourited recipe just returns the existing
    // record, rather than erroring — the heart-toggle button in the app
    // shouldn't have to know whether it's already saved.
    const favourite = await FavouriteRecipe.findOneAndUpdate(
      { userId: req.user.uid, recipeSourceId },
      { $setOnInsert: { userId: req.user.uid, recipeSourceId, recipeTitle, imageUrl: imageUrl || null } },
      { new: true, upsert: true }
    );

    res.status(201).json(toDto(favourite));
  } catch (err) {
    next(err);
  }
}

// :recipeSourceId here is the Spoonacular recipe id, not the Mongo _id —
// this keeps the Android client from needing to track two different ids
// for the same recipe.
async function removeFavourite(req, res, next) {
  try {
    const favourite = await FavouriteRecipe.findOneAndDelete({
      userId: req.user.uid,
      recipeSourceId: req.params.recipeSourceId,
    });

    if (!favourite) {
      throw new ApiError(404, 'Favourite not found');
    }

    res.status(204).send();
  } catch (err) {
    next(err);
  }
}

module.exports = { getFavourites, addFavourite, removeFavourite };