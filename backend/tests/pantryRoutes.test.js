jest.mock('../src/config/firebaseAdmin', () => ({}));
jest.mock('firebase-admin/auth', () => ({ getAuth: jest.fn() }));
jest.mock('../src/models/PantryItem', () => ({ create: jest.fn() }));

const request = require('supertest');
const { getAuth } = require('firebase-admin/auth');
const PantryItem = require('../src/models/PantryItem');
const app = require('../src/app');

const AUTH = { Authorization: 'Bearer valid-token' };

beforeEach(() => {
  jest.resetAllMocks();
  getAuth.mockReturnValue({ verifyIdToken: jest.fn().mockResolvedValue({ uid: 'uid-1' }) });
});

describe('POST /api/pantry', () => {
  it('rejects a blank ingredient name', async () => {
    const res = await request(app).post('/api/pantry').set(AUTH).send({
      ingredientName: '   ',
      quantity: 1,
      unit: 'g',
      category: 'Meat',
    });

    expect(res.status).toBe(400);
    expect(PantryItem.create).not.toHaveBeenCalled();
  });

  it('rejects a non-positive quantity', async () => {
    const res = await request(app).post('/api/pantry').set(AUTH).send({
      ingredientName: 'Milk',
      quantity: 0,
      unit: 'L',
      category: 'Dairy',
    });

    expect(res.status).toBe(400);
  });

  it('creates an item with valid input', async () => {
    PantryItem.create.mockResolvedValue({
      _id: 'item-1',
      ingredientName: 'Milk',
      quantity: 1,
      unit: 'L',
      category: 'Dairy',
      expiryDate: null,
    });

    const res = await request(app).post('/api/pantry').set(AUTH).send({
      ingredientName: 'Milk',
      quantity: 1,
      unit: 'L',
      category: 'Dairy',
    });

    expect(res.status).toBe(201);
    expect(res.body.ingredientName).toBe('Milk');
  });
});