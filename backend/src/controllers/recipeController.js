const PantryItem = require('../models/PantryItem');
const User = require('../models/User');
const { pantryIngredientIsEligible, recipeIsEligible } = require('../services/recipeEligibility');
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
    const user = await User.findOne({ firebaseUid: req.user.uid });
    const diets = user?.dietaryPreferences || [];
    const avoided = user?.avoidedIngredients || [];
    const today = new Date().toISOString().slice(0, 10);
    const pantry = await PantryItem.find({ userId: req.user.uid });
    const byName = new Map(pantry.map((item) => [item.ingredientName.trim().toLowerCase(), item]));

    // What Can I Make can pass a temporary ingredient list. Known expired pantry
    // ingredients must not be reintroduced through that list, but an ingredient
    // the user adds manually is allowed unless it is restricted by their diet.
    const requested = typeof req.query.ingredients === 'string'
      ? req.query.ingredients.split(',').map((name) => name.trim()).filter(Boolean)
      : null;
    const selected = requested?.length ? requested.map((name) => byName.get(name.toLowerCase()) || {
      ingredientName: name,
      category: '',
      expiryDate: null,
    }) : pantry;
    const ingredientNames = [...new Map(selected
      .filter((item) => pantryIngredientIsEligible(item, diets, avoided, today))
      .map((item) => [item.ingredientName.toLowerCase(), item.ingredientName])).values()];
    if (ingredientNames.length === 0) return res.status(200).json({ recipes: [] });

    const number = req.query.number ? Number(req.query.number) : 10;
    const matches = await recipeApiClient.findByIngredients(ingredientNames, number);
    if (matches.length === 0) return res.status(200).json({ recipes: [] });
    let safeMatches = matches;
    if (diets.some((d) => d.toLowerCase() !== 'no restrictions') || avoided.length) {
      const ids = matches.map((recipe) => recipe.id);
      const details = await recipeApiClient.getRecipesInformationBulk(ids);
      const safeIds = new Set(details
        .filter((recipe) => recipeIsEligible(recipe, diets, avoided))
        .map((recipe) => String(recipe.id)));
      safeMatches = matches.filter((recipe) => safeIds.has(String(recipe.id)));
    }
    res.status(200).json({ recipes: safeMatches.map(toRecommendationDto) });
  } catch (err) {
    next(err);
  }
}

module.exports = { searchRecipes, getRecipeById, getRecommendations };