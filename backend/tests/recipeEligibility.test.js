const { hasForbiddenIngredient, isUnexpired, pantryIngredientIsEligible, recipeIsEligible } = require('../src/services/recipeEligibility');

describe('recipe and pantry eligibility', () => {
  it('keeps ingredients expiring today but not yesterday', () => {
    expect(isUnexpired({ expiryDate: '2026-11-06' }, '2026-11-06')).toBe(true);
    expect(isUnexpired({ expiryDate: '2026-11-05' }, '2026-11-06')).toBe(false);
  });
  it('does not match eggplant as an avoided egg', () => {
    expect(hasForbiddenIngredient('Eggplant', ['Eggs'])).toBe(false);
    expect(hasForbiddenIngredient('Scrambled eggs', ['Eggs'])).toBe(true);
  });
  it('excludes meat from a vegetarian pantry search', () => {
    expect(pantryIngredientIsEligible({ ingredientName: 'Chicken', category: 'Meat' }, ['Vegetarian'], [], '2026-11-06')).toBe(false);
  });
  it('requires reliable metadata for dietary restrictions', () => {
    expect(recipeIsEligible({ vegetarian: true }, ['Vegetarian'], [])).toBe(false);
    expect(recipeIsEligible({ vegetarian: true, extendedIngredients: [{ name: 'Peanuts' }] }, ['Vegetarian'], ['Nuts'])).toBe(false);
    expect(recipeIsEligible({ vegetarian: true, extendedIngredients: [{ name: 'Rice' }] }, ['Vegetarian'], ['Nuts'])).toBe(true);
  });
  it('uses conservative filtering for halal food', () => {
    expect(recipeIsEligible({ vegetarian: false, extendedIngredients: [{ name: 'Chicken' }] }, ['Halal'], [])).toBe(false);
    expect(recipeIsEligible({ vegetarian: true, extendedIngredients: [{ name: 'White wine' }] }, ['Halal'], [])).toBe(false);
  });
});
