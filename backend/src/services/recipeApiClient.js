const env = require('../config/env');

const SPOONACULAR_BASE_URL = 'https://api.spoonacular.com';

function getApiKey() {
  if (!env.spoonacularApiKey) {
    throw new Error('SPOONACULAR_API_KEY is not set');
  }

  return env.spoonacularApiKey;
}

// Searches recipes by free-text query, with optional filters.
async function searchRecipes({ query, diet, cuisine, maxReadyTime, number = 10 }) {
  const params = new URLSearchParams({
    apiKey: getApiKey(),
    number: String(number),
    addRecipeInformation: 'true',
  });

  if (query) params.set('query', query);
  if (diet) params.set('diet', diet);
  if (cuisine) params.set('cuisine', cuisine);
  if (maxReadyTime) params.set('maxReadyTime', String(maxReadyTime));

  const response = await fetch(`${SPOONACULAR_BASE_URL}/recipes/complexSearch?${params}`);

  if (!response.ok) {
    throw new Error(`Spoonacular search failed with status ${response.status}`);
  }

  const data = await response.json();
  return data.results || [];
}

// Retrieves full detail for a single recipe by its Spoonacular id.
async function getRecipeById(id) {
  const params = new URLSearchParams({ apiKey: getApiKey() });

  const response = await fetch(`${SPOONACULAR_BASE_URL}/recipes/${id}/information?${params}`);

  if (response.status === 404) {
    return null;
  }

  if (!response.ok) {
    throw new Error(`Spoonacular recipe lookup failed with status ${response.status}`);
  }

  return response.json();
}

// Ranks recipes by how many of the given ingredients they use, and lists what's missing.
// This is the core of CR-01: each result already carries usedIngredientCount / missedIngredients.
async function findByIngredients(ingredients, number = 10) {
  const params = new URLSearchParams({
    apiKey: getApiKey(),
    ingredients: ingredients.join(','),
    number: String(number),
    ranking: '2', // 2 = minimize missing ingredients
    ignorePantry: 'true',
  });

  const response = await fetch(`${SPOONACULAR_BASE_URL}/recipes/findByIngredients?${params}`);

  if (!response.ok) {
    throw new Error(`Spoonacular ingredient search failed with status ${response.status}`);
  }

  return response.json();
}

module.exports = { searchRecipes, getRecipeById, findByIngredients };