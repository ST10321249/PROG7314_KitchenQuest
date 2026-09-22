const PantryItem = require('../models/PantryItem');
const recipeApiClient = require('../services/recipeApiClient');
const ApiError = require('../utils/ApiError');

function toSearchResultDto(recipe) {
  return {
    recipeSourceId: String(recipe.id),
    title: recipe.title,
    imageUrl: recipe.image || null,
    readyInMinutes: recipe.readyInMinutes ?? null,
    servings: recipe.servings ?? null,
  };
}

function toDetailDto(recipe) {
  return {
    recipeSourceId: String(recipe.id),
    title: recipe.title,
    imageUrl: recipe.image || null,
    readyInMinutes: recipe.readyInMinutes ?? null,
    servings: recipe.servings ?? null,
    ingredients: (recipe.extendedIngredients || []).map((ingredient) => ({
      name: ingredient.name,
      amount: ingredient.amount,
      unit: ingredient.unit,
    })),
    steps: (recipe.analyzedInstructions?.[0]?.steps || []).map((step) => ({
      number: step.number,
      instruction: step.step,
    })),
  };
}

// This is the DTO CR-01 depends on: matchRatio and missingIngredients
// travel with every result, so the Android app never has to open a
// recipe just to find out whether it's feasible.
function toRecommendationDto(recipe) {
  const usedCount = recipe.usedIngredientCount ?? 0;
  const missedCount = recipe.missedIngredientCount ?? 0;
  const totalCount = usedCount + missedCount;

  return {
    recipeSourceId: String(recipe.id),
    title: recipe.title,
    imageUrl: recipe.image || null,
    matchRatio: totalCount > 0 ? usedCount / totalCount : 0,
    missingIngredients: (recipe.missedIngredients || []).map((ingredient) => ingredient.name),
  };
}

async function searchRecipes(req, res, next) {
  try {
    const { query, diet, cuisine, maxReadyTime, number } = req.query;

    const results = await recipeApiClient.searchRecipes({
      query,
      diet,
      cuisine,
      maxReadyTime: maxReadyTime ? Number(maxReadyTime) : undefined,
      number: number ? Number(number) : undefined,
    });

    res.status(200).json(results.map(toSearchResultDto));
  } catch (err) {
    next(err);
  }
}

async function getRecipeById(req, res, next) {
  try {
    const recipe = await recipeApiClient.getRecipeById(req.params.id);

    if (!recipe) {
      throw new ApiError(404, 'Recipe not found');
    }

    res.status(200).json(toDetailDto(recipe));
  } catch (err) {
    next(err);
  }
}

async function getRecommendations(req, res, next) {
  try {
    // The normal flow uses every ingredient stored in the user's pantry.
    // Part 2's "What Can I Make?" screen can also pass a temporary
    // comma-separated ingredient list when the user removes a chip. This
    // changes the recommendation search without deleting pantry data.
    const requestedIngredients = typeof req.query.ingredients === 'string'
      ? req.query.ingredients
          .split(',')
          .map((name) => name.trim())
          .filter(Boolean)
      : [];

    let ingredientNames = requestedIngredients;

    if (ingredientNames.length === 0) {
      const pantryItems = await PantryItem.find({ userId: req.user.uid });
      ingredientNames = pantryItems.map((item) => item.ingredientName);
    }

    if (ingredientNames.length === 0) {
      return res.status(200).json({ recipes: [] });
    }

    const number = req.query.number ? Number(req.query.number) : 10;
    const results = await recipeApiClient.findByIngredients(ingredientNames, number);

    res.status(200).json({ recipes: results.map(toRecommendationDto) });
  } catch (err) {
    next(err);
  }
}

module.exports = { searchRecipes, getRecipeById, getRecommendations };