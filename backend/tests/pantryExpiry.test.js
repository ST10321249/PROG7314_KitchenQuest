jest.mock('../src/config/firebaseAdmin', () => ({}));
jest.mock('firebase-admin/auth', () => ({ getAuth: jest.fn() }));
jest.mock('../src/models/PantryItem', () => ({
  create: jest.fn(),
  findOne: jest.fn(),
  findOneAndUpdate: jest.fn(),
}));
jest.mock('../src/models/ShoppingListItem', () => ({ deleteOne: jest.fn() }));

const request = require('supertest');
const { getAuth } = require('firebase-admin/auth');
const PantryItem = require('../src/models/PantryItem');
const ShoppingListItem = require('../src/models/ShoppingListItem');
const app = require('../src/app');

const AUTH = { Authorization: 'Bearer valid-token' };
const OBJECT_ID = '507f1f77bcf86cd799439011';
const CLIENT_ID = '3f2b8c1e-9d4a-4c6e-8b1a-2f7d5e9c0a11';

const daysFromNow = (days) =>
  new Date(Date.now() + days * 86400000).toISOString().slice(0, 10);

const item = {
  ingredientName: 'Milk',
  quantity: 1,
  unit: 'L',
  category: 'Dairy',
};

function stored(expiryDate) {
  return { _id: OBJECT_ID, clientId: CLIENT_ID, ...item, expiryDate: new Date(expiryDate) };
}

beforeEach(() => {
  jest.resetAllMocks();
  getAuth.mockReturnValue({ verifyIdToken: jest.fn().mockResolvedValue({ uid: 'uid-1' }) });
  PantryItem.findOne.mockResolvedValue(null);
});

// A change made while the phone was offline can arrive after its expiry date has passed.
// That must not make the server refuse it, or the item would never be saved.
describe('expiry dates that have passed', () => {
  it('accepts a create from the app even though the date has since passed', async () => {
    PantryItem.create.mockResolvedValue(stored(daysFromNow(-2)));

    const res = await request(app)
      .post('/api/pantry')
      .set(AUTH)
      .send({ ...item, clientId: CLIENT_ID, expiryDate: daysFromNow(-2) });

    expect(res.status).toBe(201);
    expect(PantryItem.create).toHaveBeenCalled();
  });

  it('still refuses a direct create without a clientId that has a past date', async () => {
    const res = await request(app)
      .post('/api/pantry')
      .set(AUTH)
      .send({ ...item, expiryDate: daysFromNow(-1) });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/cannot be in the past/);
    expect(PantryItem.create).not.toHaveBeenCalled();
  });

  it('accepts a direct create with today or a future date', async () => {
    PantryItem.create.mockResolvedValue(stored(daysFromNow(0)));

    const today = await request(app)
      .post('/api/pantry')
      .set(AUTH)
      .send({ ...item, expiryDate: daysFromNow(0) });
    const future = await request(app)
      .post('/api/pantry')
      .set(AUTH)
      .send({ ...item, expiryDate: daysFromNow(5) });

    expect(today.status).toBe(201);
    expect(future.status).toBe(201);
  });

  it('accepts an edit that arrives after the date has passed', async () => {
    PantryItem.findOneAndUpdate.mockResolvedValue(stored(daysFromNow(-1)));

    const res = await request(app)
      .patch(`/api/pantry/${OBJECT_ID}`)
      .set(AUTH)
      .send({ expiryDate: daysFromNow(-1) });

    expect(res.status).toBe(200);
  });

  it('accepts moving a bought item to the pantry after the date has passed', async () => {
    PantryItem.create.mockResolvedValue(stored(daysFromNow(-3)));
    ShoppingListItem.deleteOne.mockResolvedValue({});

    const res = await request(app)
      .post('/api/shopping-list/transfer')
      .set(AUTH)
      .send({
        shoppingItemId: OBJECT_ID,
        pantryItem: { ...item, clientId: CLIENT_ID, expiryDate: daysFromNow(-3) },
      });

    expect(res.status).toBe(201);
  });

  it('still refuses text that is not a date', async () => {
    const create = await request(app)
      .post('/api/pantry')
      .set(AUTH)
      .send({ ...item, clientId: CLIENT_ID, expiryDate: 'next week' });
    const edit = await request(app)
      .patch(`/api/pantry/${OBJECT_ID}`)
      .set(AUTH)
      .send({ expiryDate: 'next week' });

    expect(create.status).toBe(400);
    expect(edit.status).toBe(400);
    expect(create.body.error).toMatch(/Invalid date/);
  });
});
