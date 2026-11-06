jest.mock('../src/config/firebaseAdmin', () => ({}));
jest.mock('firebase-admin/auth', () => ({ getAuth: jest.fn() }));
jest.mock('../src/models/PantryItem', () => ({ find: jest.fn() }));
jest.mock('../src/models/User', () => ({ findOne: jest.fn() }));
jest.mock('../src/services/recipeApiClient', () => ({
  searchRecipes: jest.fn(),
  getRecipeById: jest.fn(),
  findByIngredients: jest.fn(),
  getRecipesInformationBulk: jest.fn(),
}));

const request = require('supertest');
const { getAuth } = require('firebase-admin/auth');
const PantryItem = require('../src/models/PantryItem');
const User = require('../src/models/User');
const recipeApiClient = require('../src/services/recipeApiClient');
const app = require('../src/app');

const AUTH = { Authorization: 'Bearer valid-token' };
const verifyIdToken = jest.fn();

beforeEach(() => {
  jest.resetAllMocks();
  getAuth.mockReturnValue({ verifyIdToken });
  verifyIdToken.mockResolvedValue({ uid: 'uid-1', email: 'test@example.com' });
  User.findOne.mockResolvedValue({ dietaryPreferences: [], avoidedIngredients: [] });
  PantryItem.find.mockResolvedValue([]);
});

describe('recipe routes', () => {
  it('requires authentication', async () => {
    const res = await request(app).get('/api/recipes/search?query=pasta');

    expect(res.status).toBe(401);
    expect(recipeApiClient.searchRecipes).not.toHaveBeenCalled();
  });

  it('maps recipe search results into the Android DTO shape', async () => {
    recipeApiClient.searchRecipes.mockResolvedValue([
      {
        id: 123,
        title: 'Tomato Pasta',
        image: 'https://example.com/pasta.jpg',
        readyInMinutes: 25,
        servings: 2,
      },
    ]);

    const res = await request(app)
      .get('/api/recipes/search?query=pasta')
      .set(AUTH);

    expect(res.status).toBe(200);
    expect(res.body).toEqual([
      {
        recipeSourceId: '123',
        title: 'Tomato Pasta',
        imageUrl: 'https://example.com/pasta.jpg',
        readyInMinutes: 25,
        servings: 2,
      },
    ]);
    expect(recipeApiClient.searchRecipes).toHaveBeenCalledWith(
      expect.objectContaining({ query: 'pasta' })
    );
  });

  it('uses temporary ingredients supplied by What Can I Make', async () => {
    recipeApiClient.findByIngredients.mockResolvedValue([
      {
        id: 456,
        title: 'Chicken Rice',
        usedIngredientCount: 2,
        missedIngredientCount: 1,
        missedIngredients: [{ name: 'Onion' }],
      },
    ]);

    const res = await request(app)
      .get('/api/recipes/recommendations?ingredients=Chicken,Rice')
      .set(AUTH);

    expect(res.status).toBe(200);
    expect(PantryItem.find).toHaveBeenCalledWith({ userId: 'uid-1' });
    expect(recipeApiClient.findByIngredients).toHaveBeenCalledWith(
      ['Chicken', 'Rice'],
      10
    );
    expect(res.body.recipes[0]).toMatchObject({
      recipeSourceId: '456',
      matchRatio: 2 / 3,
      missingIngredients: ['Onion'],
    });
  });

  it('falls back to the saved pantry when no temporary ingredient list is supplied', async () => {
    PantryItem.find.mockResolvedValue([
      { ingredientName: 'Eggs' },
      { ingredientName: 'Milk' },
    ]);
    recipeApiClient.findByIngredients.mockResolvedValue([]);

    const res = await request(app)
      .get('/api/recipes/recommendations')
      .set(AUTH);

    expect(res.status).toBe(200);
    expect(PantryItem.find).toHaveBeenCalledWith({ userId: 'uid-1' });
    expect(recipeApiClient.findByIngredients).toHaveBeenCalledWith(
      ['Eggs', 'Milk'],
      10
    );
  });

  it('excludes expired pantry items, including when named in temporary ingredients', async () => {
    PantryItem.find.mockResolvedValue([
      { ingredientName: 'Chicken', category: 'Meat', expiryDate: new Date('2020-01-01') },
      { ingredientName: 'Rice', category: 'Grains', expiryDate: null },
    ]);
    recipeApiClient.findByIngredients.mockResolvedValue([]);
    const res = await request(app).get('/api/recipes/recommendations?ingredients=Chicken,Rice').set(AUTH);
    expect(res.status).toBe(200);
    expect(recipeApiClient.findByIngredients).toHaveBeenCalledWith(['Rice'], 10);
  });

  it('filters recipes by saved dietary choices and avoided ingredients', async () => {
    User.findOne.mockResolvedValue({ dietaryPreferences: ['Vegetarian'], avoidedIngredients: ['Nuts'] });
    PantryItem.find.mockResolvedValue([{ ingredientName: 'Tomato', category: 'Vegetable' }]);
    recipeApiClient.findByIngredients.mockResolvedValue([
      { id: 1, title: 'Safe pasta', usedIngredientCount: 1, missedIngredientCount: 0 },
      { id: 2, title: 'Nut pasta', usedIngredientCount: 1, missedIngredientCount: 0 },
      { id: 3, title: 'Meat pasta', usedIngredientCount: 1, missedIngredientCount: 0 },
    ]);
    recipeApiClient.getRecipesInformationBulk.mockResolvedValue([
      { id: 1, vegetarian: true, extendedIngredients: [{ name: 'Tomato' }] },
      { id: 2, vegetarian: true, extendedIngredients: [{ name: 'Cashews' }] },
      { id: 3, vegetarian: false, extendedIngredients: [{ name: 'Chicken' }] },
    ]);
    const res = await request(app).get('/api/recipes/recommendations').set(AUTH);
    expect(res.status).toBe(200);
    expect(res.body.recipes.map((r) => r.recipeSourceId)).toEqual(['1']);
  });

  it('excludes avoided foods from ingredient matching itself', async () => {
    User.findOne.mockResolvedValue({ dietaryPreferences: [], avoidedIngredients: ['Milk'] });
    PantryItem.find.mockResolvedValue([
      { ingredientName: 'Milk', category: 'Dairy' }, { ingredientName: 'Rice', category: 'Grains' },
    ]);
    recipeApiClient.findByIngredients.mockResolvedValue([]);
    const res = await request(app).get('/api/recipes/recommendations').set(AUTH);
    expect(res.status).toBe(200);
    expect(recipeApiClient.findByIngredients).toHaveBeenCalledWith(['Rice'], 10);
    expect(recipeApiClient.getRecipesInformationBulk).not.toHaveBeenCalled();
    expect(res.body).toEqual({ recipes: [] });
  });

  it('returns an empty recommendation list when the pantry is empty', async () => {
    PantryItem.find.mockResolvedValue([]);

    const res = await request(app)
      .get('/api/recipes/recommendations')
      .set(AUTH);

    expect(res.status).toBe(200);
    expect(res.body).toEqual({ recipes: [] });
    expect(recipeApiClient.findByIngredients).not.toHaveBeenCalled();
  });
});
