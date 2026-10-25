jest.mock('../src/config/firebaseAdmin', () => ({}));
jest.mock('firebase-admin/auth', () => ({ getAuth: jest.fn() }));
jest.mock('firebase-admin/messaging', () => ({ getMessaging: jest.fn() }));
jest.mock('../src/models/PantryItem', () => ({
  find: jest.fn(),
  create: jest.fn(),
  findOne: jest.fn(),
  findOneAndUpdate: jest.fn(),
  findOneAndDelete: jest.fn(),
}));
jest.mock('../src/models/ShoppingListItem', () => ({
  find: jest.fn(),
  create: jest.fn(),
  findOne: jest.fn(),
  findOneAndUpdate: jest.fn(),
  findOneAndDelete: jest.fn(),
  deleteOne: jest.fn(),
}));
jest.mock('../src/models/CookingHistory', () => ({
  find: jest.fn(),
  create: jest.fn(),
  findOne: jest.fn(),
}));

const request = require('supertest');
const { getAuth } = require('firebase-admin/auth');
const PantryItem = require('../src/models/PantryItem');
const ShoppingListItem = require('../src/models/ShoppingListItem');
const CookingHistory = require('../src/models/CookingHistory');
const app = require('../src/app');

const AUTH = { Authorization: 'Bearer valid-token' };
const ID = '507f1f77bcf86cd799439011';

function signInAs(uid) {
  getAuth.mockReturnValue({ verifyIdToken: jest.fn().mockResolvedValue({ uid }) });
}

function sortable(result) {
  return { sort: jest.fn().mockResolvedValue(result) };
}

beforeEach(() => {
  jest.resetAllMocks();
});

// The API decides whose data to touch from the verified token, never from anything the
// app sends. These check the signed-in user's uid is part of every database query, and
// that an id belonging to someone else behaves exactly like an id that does not exist.
describe.each(['uid-1', 'uid-2'])('signed in as %s', (uid) => {
  beforeEach(() => signInAs(uid));

  it('lists only the pantry of the signed-in user', async () => {
    PantryItem.find.mockReturnValue(sortable([]));

    await request(app).get('/api/pantry').set(AUTH);

    expect(PantryItem.find).toHaveBeenCalledWith({ userId: uid });
  });

  it('edits and deletes only pantry items the signed-in user owns', async () => {
    PantryItem.findOneAndUpdate.mockResolvedValue(null);
    PantryItem.findOneAndDelete.mockResolvedValue(null);

    const patch = await request(app).patch(`/api/pantry/${ID}`).set(AUTH).send({ quantity: 2 });
    const del = await request(app).delete(`/api/pantry/${ID}`).set(AUTH);

    expect(patch.status).toBe(404);
    expect(del.status).toBe(404);
    expect(PantryItem.findOneAndUpdate.mock.calls[0][0]).toEqual({ _id: ID, userId: uid });
    expect(PantryItem.findOneAndDelete.mock.calls[0][0]).toEqual({ _id: ID, userId: uid });
  });

  it('lists only the shopping list of the signed-in user', async () => {
    ShoppingListItem.find.mockReturnValue(sortable([]));

    await request(app).get('/api/shopping-list').set(AUTH);

    expect(ShoppingListItem.find).toHaveBeenCalledWith({ userId: uid });
  });

  it('edits and deletes only shopping items the signed-in user owns', async () => {
    ShoppingListItem.findOneAndUpdate.mockResolvedValue(null);
    ShoppingListItem.findOneAndDelete.mockResolvedValue(null);

    const patch = await request(app)
      .patch(`/api/shopping-list/${ID}`)
      .set(AUTH)
      .send({ isPurchased: true });
    const del = await request(app).delete(`/api/shopping-list/${ID}`).set(AUTH);

    expect(patch.status).toBe(404);
    expect(del.status).toBe(404);
    expect(ShoppingListItem.findOneAndUpdate.mock.calls[0][0]).toEqual({ _id: ID, userId: uid });
    expect(ShoppingListItem.findOneAndDelete.mock.calls[0][0]).toEqual({ _id: ID, userId: uid });
  });

  it('only matches the signed-in user\'s own items when moving a shopping item to the pantry', async () => {
    PantryItem.findOne.mockResolvedValue(null);
    PantryItem.create.mockResolvedValue({ _id: ID, ingredientName: 'Eggs' });
    ShoppingListItem.deleteOne.mockResolvedValue({});

    await request(app)
      .post('/api/shopping-list/transfer')
      .set(AUTH)
      .send({
        shoppingItemId: ID,
        pantryItem: {
          clientId: 'client-abcdef-1',
          ingredientName: 'Eggs',
          quantity: 1,
          unit: 'pcs',
          category: 'Dairy',
        },
      });

    expect(PantryItem.findOne).toHaveBeenCalledWith({ userId: uid, clientId: 'client-abcdef-1' });
    expect(ShoppingListItem.deleteOne).toHaveBeenCalledWith({ _id: ID, userId: uid });
  });

  it('lists only the cooking history of the signed-in user', async () => {
    CookingHistory.find.mockReturnValue(sortable([]));

    await request(app).get('/api/history').set(AUTH);

    expect(CookingHistory.find).toHaveBeenCalledWith({ userId: uid });
  });

  it('only updates history entries the signed-in user owns', async () => {
    CookingHistory.findOne.mockResolvedValue(null);

    const res = await request(app)
      .patch(`/api/history/${ID}`)
      .set(AUTH)
      .send({ photoFileId: null });

    expect(res.status).toBe(404);
    expect(CookingHistory.findOne).toHaveBeenCalledWith({ _id: ID, userId: uid });
  });

  it('ignores a userId sent in the body and uses the one from the token', async () => {
    PantryItem.findOne.mockResolvedValue(null);
    PantryItem.create.mockResolvedValue({ _id: ID, ingredientName: 'Milk' });

    await request(app).post('/api/pantry').set(AUTH).send({
      userId: 'someone-else',
      clientId: 'client-abcdef-2',
      ingredientName: 'Milk',
      quantity: 1,
      unit: 'L',
      category: 'Dairy',
    });

    expect(PantryItem.create).toHaveBeenCalledWith(expect.objectContaining({ userId: uid }));
  });
});
