// Apply the user's saved food restrictions to recommendations. Ingredient matching
// must not be treated as proof that a recipe meets a dietary requirement.
const AVOIDED_TERMS = {
  nuts: ['nut', 'nuts', 'almond', 'cashew', 'walnut', 'pecan', 'pistachio', 'hazelnut', 'macadamia', 'peanut', 'pine nut', 'brazil nut', 'mixed nuts', 'nut butter'],
  shellfish: ['shrimp', 'prawn', 'crab', 'lobster', 'crayfish', 'scallop', 'clam', 'mussel', 'oyster', 'shellfish'],
  eggs: ['egg', 'mayonnaise', 'meringue'],
  milk: ['milk', 'cream', 'cheese', 'butter', 'yogurt', 'yoghurt', 'whey', 'casein', 'ghee', 'buttermilk'],
  soy: ['soy', 'soya', 'soybean', 'soybeans', 'tofu', 'tempeh', 'edamame', 'miso'],
};
const PORK_AND_ALCOHOL = ['pork', 'bacon', 'ham', 'prosciutto', 'lard', 'gelatin', 'gelatine', 'wine', 'beer', 'rum', 'vodka', 'whiskey', 'whisky', 'brandy', 'liqueur'];
const MEAT_AND_FISH = ['beef', 'chicken', 'turkey', 'lamb', 'veal', 'duck', 'fish', 'tuna', 'salmon', 'anchovy', 'sardine', ...PORK_AND_ALCOHOL.slice(0, 5), ...AVOIDED_TERMS.shellfish];

function normalize(value) {
  return String(value || '').trim().toLowerCase();
}
function containsTerm(value, term) {
  const escaped = term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return new RegExp(`(^|[^a-z])${escaped}(s)?([^a-z]|$)`, 'i').test(value);
}
function avoidedTerms(choices) {
  return [...new Set((choices || []).flatMap((choice) => {
    const key = normalize(choice);
    return AVOIDED_TERMS[key] || [key];
  }).filter(Boolean))];
}
function hasForbiddenIngredient(name, choices) {
  const lower = normalize(name);
  return avoidedTerms(choices).some((term) => containsTerm(lower, term));
}
function isUnexpired(item, today = new Date().toISOString().slice(0, 10)) {
  if (!item.expiryDate) return true;
  const date = item.expiryDate instanceof Date ? item.expiryDate.toISOString().slice(0, 10) : String(item.expiryDate).slice(0, 10);
  return date >= today;
}
function pantryIngredientIsEligible(item, preferences, avoided, today) {
  if (!isUnexpired(item, today) || hasForbiddenIngredient(item.ingredientName, avoided)) return false;
  const diets = (preferences || []).map(normalize);
  const category = normalize(item.category);
  const name = normalize(item.ingredientName);
  const vegetarian = diets.includes('vegetarian') || diets.includes('vegan') || diets.includes('halal');
  if (vegetarian && (['meat', 'seafood', 'fish', 'poultry'].includes(category) || MEAT_AND_FISH.some((term) => containsTerm(name, term)))) return false;
  if (diets.includes('vegan') && (['dairy', 'eggs'].includes(category) || [...AVOIDED_TERMS.milk, ...AVOIDED_TERMS.eggs].some((term) => containsTerm(name, term)))) return false;
  return true;
}
function recipeIsEligible(recipe, preferences, avoided) {
  const diets = (preferences || []).map(normalize).filter((diet) => diet && diet !== 'no restrictions');
  const ingredients = recipe.extendedIngredients;
  // Do not assume an incomplete recipe is safe for somebody's food restrictions.
  if ((diets.length || (avoided || []).length) && !Array.isArray(ingredients)) return false;
  const names = (ingredients || []).map((ingredient) => `${ingredient.name || ''} ${ingredient.original || ''} ${ingredient.originalName || ''}`);
  if (names.some((name) => hasForbiddenIngredient(name, avoided))) return false;
  for (const diet of diets) {
    if (diet === 'vegetarian' && recipe.vegetarian !== true) return false;
    if (diet === 'vegan' && recipe.vegan !== true) return false;
    if (diet === 'gluten-free' && recipe.glutenFree !== true) return false;
    if (diet === 'dairy-free' && recipe.dairyFree !== true) return false;
    // Spoonacular does not certify recipes as halal. Be conservative: display
    // vegetarian dishes only, without obvious alcohol or pork derivatives.
    if (diet === 'halal' && (recipe.vegetarian !== true || names.some((name) => PORK_AND_ALCOHOL.some((term) => containsTerm(normalize(name), term))))) return false;
    // Unknown restrictions cannot safely be asserted as satisfied.
    if (!['vegetarian', 'vegan', 'gluten-free', 'dairy-free', 'halal'].includes(diet)) return false;
  }
  return true;
}
module.exports = { hasForbiddenIngredient, isUnexpired, pantryIngredientIsEligible, recipeIsEligible };
