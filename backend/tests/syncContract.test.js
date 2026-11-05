jest.mock('../src/config/firebaseAdmin', () => ({}));
jest.mock('firebase-admin/auth', () => ({ getAuth: jest.fn() }));
jest.mock('../src/models/PantryItem', () => ({
  create: jest.fn(),
  findOne: jest.fn(),
  findOneAndUpdate: jest.fn(),
  findOneAndDelete: jest.fn(),
}));
jest.mock('../src/models/ShoppingListItem', () => ({
  create: jest.fn(),
  findOne: jest.fn(),
  findOneAndUpdate: jest.fn(),
  findOneAndDelete: jest.fn(),
  deleteOne: jest.fn(),
}));

const request = require('supertest');
const { getAuth } = require('firebase-admin/auth');
const PantryItem = require('../src/models/PantryItem');
const ShoppingListItem = require('../src/models/ShoppingListItem');
const app = require('../src/app');

const AUTH = { Authorization: 'Bearer valid-token' };
const CLIENT_ID = '3f2b8c1e-9d4a-4c6e-8b1a-2f7d5e9c0a11';
const OBJECT_ID = '507f1f77bcf86cd799439011';

function pantryDoc(overrides = {}) {
  return {
    _id: OBJECT_ID,
    clientId: CLIENT_ID,
    ingredientName: 'Milk',
    quantity: 1,
    unit: 'L',
    category: 'Dairy',
    expiryDate: null,
    updatedAt: new Date('2026-10-20T08:00:00.000Z'),
    ...overrides,
  };
}

function shoppingDoc(overrides = {}) {
  return {
    _id: OBJECT_ID,
    clientId: CLIENT_ID,
    ingredientName: 'Eggs',
    quantity: 12,
    unit: 'pcs',
    isPurchased: false,
    updatedAt: new Date('2026-10-20T08:00:00.000Z'),
    ...overrides,
  };
}

const pantryBody = {
  clientId: CLIENT_ID,
  ingredientName: 'Milk',
  quantity: 1,
  unit: 'L',
  category: 'Dairy',
};

beforeEach(() => {
  jest.resetAllMocks();
  getAuth.mockReturnValue({ verifyIdToken: jest.fn().mockResolvedValue({ uid: 'uid-1' }) });
});

describe('pantry create with a clientId', () => {
  it('creates the item and returns its clientId and updatedAt', async () => {
    PantryItem.findOne.mockResolvedValue(null);
    PantryItem.create.mockResolvedValue(pantryDoc());

    const res = await request(app).post('/api/pantry').set(AUTH).send(pantryBody);

    expect(res.status).toBe(201);
    expect(res.body.clientId).toBe(CLIENT_ID);
    expect(res.body.updatedAt).toBe('2026-10-20T08:00:00.000Z');
    expect(PantryItem.create).toHaveBeenCalledWith(
      expect.objectContaining({ clientId: CLIENT_ID, userId: 'uid-1' })
    );
  });

  it('returns the existing item instead of creating a duplicate when resent', async () => {
    PantryItem.findOne.mockResolvedValue(pantryDoc());

    const res = await request(app).post('/api/pantry').set(AUTH).send(pantryBody);

    expect(res.status).toBe(200);
    expect(res.body.id).toBe(OBJECT_ID);
    expect(PantryItem.create).not.toHaveBeenCalled();
    expect(PantryItem.findOne).toHaveBeenCalledWith({ userId: 'uid-1', clientId: CLIENT_ID });
  });

  it('returns the winning item when two identical requests race', async () => {
    PantryItem.findOne.mockResolvedValueOnce(null).mockResolvedValueOnce(pantryDoc());
    PantryItem.create.mockRejectedValue({ code: 11000 });

    const res = await request(app).post('/api/pantry').set(AUTH).send(pantryBody);

    expect(res.status).toBe(200);
    expect(res.body.id).toBe(OBJECT_ID);
  });

  it('still creates items that have no clientId', async () => {
    PantryItem.create.mockResolvedValue(pantryDoc({ clientId: undefined }));
    const { clientId, ...withoutClientId } = pantryBody;

    const res = await request(app).post('/api/pantry').set(AUTH).send(withoutClientId);

    expect(res.status).toBe(201);
    expect(res.body.clientId).toBeNull();
    expect(PantryItem.findOne).not.toHaveBeenCalled();
  });

  it('rejects a clientId with unsafe characters', async () => {
    const res = await request(app)
      .post('/api/pantry')
      .set(AUTH)
      .send({ ...pantryBody, clientId: 'bad id!' });

    expect(res.status).toBe(400);
    expect(PantryItem.create).not.toHaveBeenCalled();
  });

  it('hides other database errors behind a generic server error', async () => {
    PantryItem.findOne.mockResolvedValue(null);
    PantryItem.create.mockRejectedValue(new Error('connection lost'));
    const spy = jest.spyOn(console, 'error').mockImplementation(() => {});

    const res = await request(app).post('/api/pantry').set(AUTH).send(pantryBody);

    expect(res.status).toBe(500);
    expect(res.body.error).toBe('Internal server error');
    spy.mockRestore();
  });
});

describe('pantry update and delete', () => {
  it('does not let a clientId be changed after creation', async () => {
    PantryItem.findOneAndUpdate.mockResolvedValue(pantryDoc({ quantity: 2 }));

    const res = await request(app)
      .patch(`/api/pantry/${OBJECT_ID}`)
      .set(AUTH)
      .send({ quantity: 2, clientId: 'some-other-client-id' });

    expect(res.status).toBe(200);
    expect(PantryItem.findOneAndUpdate).toHaveBeenCalledWith(
      { _id: OBJECT_ID, userId: 'uid-1' },
      { $set: { quantity: 2 } },
      expect.anything()
    );
  });

  it('answers 404 rather than 500 when the id is not a valid server id', async () => {
    const patch = await request(app)
      .patch(`/api/pantry/${CLIENT_ID}`)
      .set(AUTH)
      .send({ quantity: 2 });
    const del = await request(app).delete(`/api/pantry/${CLIENT_ID}`).set(AUTH);

    expect(patch.status).toBe(404);
    expect(del.status).toBe(404);
    expect(PantryItem.findOneAndUpdate).not.toHaveBeenCalled();
    expect(PantryItem.findOneAndDelete).not.toHaveBeenCalled();
  });

  it('answers 404 when deleting an item that is already gone', async () => {
    PantryItem.findOneAndDelete.mockResolvedValue(null);

    const res = await request(app).delete(`/api/pantry/${OBJECT_ID}`).set(AUTH);

    expect(res.status).toBe(404);
  });
});

describe('shopping create with a clientId', () => {
  const body = { clientId: CLIENT_ID, ingredientName: 'Eggs', quantity: 12, unit: 'pcs' };

  it('creates the item and returns its clientId', async () => {
    ShoppingListItem.findOne.mockResolvedValue(null);
    ShoppingListItem.create.mockResolvedValue(shoppingDoc());

    const res = await request(app).post('/api/shopping-list').set(AUTH).send(body);

    expect(res.status).toBe(201);
    expect(res.body.clientId).toBe(CLIENT_ID);
  });

  it('returns the existing item when the same create is resent', async () => {
    ShoppingListItem.findOne.mockResolvedValue(shoppingDoc());

    const res = await request(app).post('/api/shopping-list').set(AUTH).send(body);

    expect(res.status).toBe(200);
    expect(ShoppingListItem.create).not.toHaveBeenCalled();
  });

  it('answers 404 for an id that is not a valid server id', async () => {
    const res = await request(app)
      .patch('/api/shopping-list/not-an-id')
      .set(AUTH)
      .send({ isPurchased: true });

    expect(res.status).toBe(404);
  });
});

describe('POST /api/shopping-list/transfer', () => {
  const transferBody = { shoppingItemId: OBJECT_ID, pantryItem: pantryBody };

  it('adds the pantry item and removes the shopping item', async () => {
    PantryItem.findOne.mockResolvedValue(null);
    PantryItem.create.mockResolvedValue(pantryDoc());
    ShoppingListItem.deleteOne.mockResolvedValue({ deletedCount: 1 });

    const res = await request(app)
      .post('/api/shopping-list/transfer')
      .set(AUTH)
      .send(transferBody);

    expect(res.status).toBe(201);
    expect(res.body.ingredientName).toBe('Milk');
    expect(ShoppingListItem.deleteOne).toHaveBeenCalledWith({
      _id: OBJECT_ID,
      userId: 'uid-1',
    });
  });

  it('finishes the job when a transfer is resent after it already succeeded', async () => {
    PantryItem.findOne.mockResolvedValue(pantryDoc());
    ShoppingListItem.deleteOne.mockResolvedValue({ deletedCount: 0 });

    const res = await request(app)
      .post('/api/shopping-list/transfer')
      .set(AUTH)
      .send(transferBody);

    expect(res.status).toBe(200);
    expect(PantryItem.create).not.toHaveBeenCalled();
  });

  it('can identify the shopping item by its clientId', async () => {
    PantryItem.findOne.mockResolvedValue(null);
    PantryItem.create.mockResolvedValue(pantryDoc());
    ShoppingListItem.deleteOne.mockResolvedValue({ deletedCount: 1 });

    const res = await request(app)
      .post('/api/shopping-list/transfer')
      .set(AUTH)
      .send({ shoppingClientId: 'shopping-client-1', pantryItem: pantryBody });

    expect(res.status).toBe(201);
    expect(ShoppingListItem.deleteOne).toHaveBeenCalledWith({
      clientId: 'shopping-client-1',
      userId: 'uid-1',
    });
  });

  it('requires a clientId on the pantry item', async () => {
    const { clientId, ...pantryWithoutId } = pantryBody;

    const res = await request(app)
      .post('/api/shopping-list/transfer')
      .set(AUTH)
      .send({ shoppingItemId: OBJECT_ID, pantryItem: pantryWithoutId });

    expect(res.status).toBe(400);
    expect(PantryItem.create).not.toHaveBeenCalled();
  });

  it('requires a way to identify the shopping item', async () => {
    const res = await request(app)
      .post('/api/shopping-list/transfer')
      .set(AUTH)
      .send({ pantryItem: pantryBody });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/shoppingItemId or shoppingClientId/);
  });

  it('requires a signed-in user', async () => {
    const res = await request(app).post('/api/shopping-list/transfer').send(transferBody);

    expect(res.status).toBe(401);
  });
});
