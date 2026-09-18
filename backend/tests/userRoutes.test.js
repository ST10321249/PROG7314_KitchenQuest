jest.mock('../src/config/firebaseAdmin', () => ({}));
jest.mock('firebase-admin/auth', () => ({ getAuth: jest.fn() }));
jest.mock('../src/models/User', () => ({
  findOne: jest.fn(),
  findOneAndUpdate: jest.fn(),
}));

const request = require('supertest');
const { getAuth } = require('firebase-admin/auth');
const User = require('../src/models/User');
const app = require('../src/app');

const verifyIdToken = jest.fn();
const AUTH = { Authorization: 'Bearer valid-token' };

const profile = {
  firebaseUid: 'uid-1',
  displayName: 'Test User',
  email: 'test@example.com',
  dietaryPreferences: ['Vegetarian'],
  avoidedIngredients: ['Peanuts'],
};

beforeEach(() => {
  jest.resetAllMocks();
  getAuth.mockReturnValue({ verifyIdToken });
  verifyIdToken.mockResolvedValue({ uid: 'uid-1', email: 'test@example.com', name: 'Token Name' });
});

describe('authentication on protected routes', () => {
  it.each([
    ['post', '/api/users/sync'],
    ['get', '/api/users/me'],
    ['put', '/api/users/me'],
  ])('%s %s returns 401 without a token', async (method, path) => {
    const res = await request(app)[method](path);

    expect(res.status).toBe(401);
    expect(User.findOne).not.toHaveBeenCalled();
    expect(User.findOneAndUpdate).not.toHaveBeenCalled();
  });

  it('returns 401 when the token is invalid', async () => {
    verifyIdToken.mockRejectedValue(new Error('bad token'));

    const res = await request(app).get('/api/users/me').set(AUTH);

    expect(res.status).toBe(401);
  });
});

describe('POST /api/users/sync', () => {
  it('creates a profile for a new user and returns 201', async () => {
    User.findOneAndUpdate.mockResolvedValue({
      value: profile,
      lastErrorObject: { updatedExisting: false },
    });

    const res = await request(app)
      .post('/api/users/sync')
      .set(AUTH)
      .send({ dietaryPreferences: ['Vegetarian'] });

    expect(res.status).toBe(201);
    expect(res.body).toEqual(profile);

    const [filter, update] = User.findOneAndUpdate.mock.calls[0];
    expect(filter).toEqual({ firebaseUid: 'uid-1' });
    expect(update.$setOnInsert).toMatchObject({
      firebaseUid: 'uid-1',
      email: 'test@example.com',
      displayName: 'Token Name',
      dietaryPreferences: ['Vegetarian'],
    });
  });

  it('returns 200 with the existing profile for a returning user', async () => {
    User.findOneAndUpdate.mockResolvedValue({
      value: profile,
      lastErrorObject: { updatedExisting: true },
    });

    const res = await request(app).post('/api/users/sync').set(AUTH).send({});

    expect(res.status).toBe(200);
    expect(res.body).toEqual(profile);
  });

  it('returns 400 when dietaryPreferences is not an array', async () => {
    const res = await request(app)
      .post('/api/users/sync')
      .set(AUTH)
      .send({ dietaryPreferences: 'Vegetarian' });

    expect(res.status).toBe(400);
    expect(User.findOneAndUpdate).not.toHaveBeenCalled();
  });
});

describe('GET /api/users/me', () => {
  it("returns the authenticated user's profile, looked up by the token uid", async () => {
    User.findOne.mockResolvedValue(profile);

    const res = await request(app).get('/api/users/me').set(AUTH);

    expect(res.status).toBe(200);
    expect(res.body).toEqual(profile);
    expect(User.findOne).toHaveBeenCalledWith({ firebaseUid: 'uid-1' });
  });

  it('returns 404 when the user has not been synced yet', async () => {
    User.findOne.mockResolvedValue(null);

    const res = await request(app).get('/api/users/me').set(AUTH);

    expect(res.status).toBe(404);
  });
});

describe('PUT /api/users/me', () => {
  it('updates only the allowed fields and returns the new profile', async () => {
    User.findOneAndUpdate.mockResolvedValue({ ...profile, displayName: 'New Name' });

    const res = await request(app)
      .put('/api/users/me')
      .set(AUTH)
      .send({ displayName: 'New Name', firebaseUid: 'hacker', email: 'evil@example.com' });

    expect(res.status).toBe(200);
    expect(res.body.displayName).toBe('New Name');
    expect(User.findOneAndUpdate).toHaveBeenCalledWith(
      { firebaseUid: 'uid-1' },
      { $set: { displayName: 'New Name' } },
      expect.objectContaining({ new: true, runValidators: true })
    );
  });

  it('returns 400 for an empty body', async () => {
    const res = await request(app).put('/api/users/me').set(AUTH).send({});

    expect(res.status).toBe(400);
    expect(User.findOneAndUpdate).not.toHaveBeenCalled();
  });

  it('returns 400 when avoidedIngredients is not an array', async () => {
    const res = await request(app)
      .put('/api/users/me')
      .set(AUTH)
      .send({ avoidedIngredients: 'nuts' });

    expect(res.status).toBe(400);
  });

  it('returns 404 when the user has not been synced yet', async () => {
    User.findOneAndUpdate.mockResolvedValue(null);

    const res = await request(app).put('/api/users/me').set(AUTH).send({ displayName: 'X' });

    expect(res.status).toBe(404);
  });
});
