jest.mock('../src/config/firebaseAdmin', () => ({}));
jest.mock('firebase-admin/auth', () => ({ getAuth: jest.fn() }));
jest.mock('../src/models/CookingHistory', () => ({
  find: jest.fn(),
  create: jest.fn(),
  findOne: jest.fn(),
  exists: jest.fn(),
  updateMany: jest.fn(),
}));
jest.mock('../src/services/fileStorage', () => ({
  findOwned: jest.fn(),
  remove: jest.fn(),
}));

const request = require('supertest');
const { getAuth } = require('firebase-admin/auth');
const CookingHistory = require('../src/models/CookingHistory');
const fileStorage = require('../src/services/fileStorage');
const app = require('../src/app');

const AUTH = { Authorization: 'Bearer valid-token' };
const ENTRY_ID = '607f1f77bcf86cd799439022';
const PHOTO_A = '507f1f77bcf86cd799439011';
const PHOTO_B = '507f1f77bcf86cd799439012';

function entry(overrides = {}) {
  return {
    _id: ENTRY_ID,
    recipeSourceId: '716429',
    recipeTitle: 'Pasta',
    cookedAt: new Date('2026-10-20T18:00:00.000Z'),
    servings: 2,
    rating: 4,
    difficultyFeedback: 'Easy',
    photoFileId: null,
    save: jest.fn().mockResolvedValue(),
    ...overrides,
  };
}

function ownsPhotos(...ids) {
  fileStorage.findOwned.mockImplementation(async (id) => (ids.includes(id) ? { id } : null));
}

beforeEach(() => {
  jest.resetAllMocks();
  getAuth.mockReturnValue({ verifyIdToken: jest.fn().mockResolvedValue({ uid: 'uid-1' }) });
});

describe('GET /api/history', () => {
  it('includes the photo id, or null when there is none', async () => {
    CookingHistory.find.mockReturnValue({
      sort: jest.fn().mockResolvedValue([entry(), entry({ photoFileId: PHOTO_A })]),
    });

    const res = await request(app).get('/api/history').set(AUTH);

    expect(res.status).toBe(200);
    expect(res.body.map((item) => item.photoFileId)).toEqual([null, PHOTO_A]);
  });
});

describe('POST /api/history with a photo', () => {
  const body = { recipeSourceId: '716429', recipeTitle: 'Pasta', photoFileId: PHOTO_A };

  it('links a photo the user owns', async () => {
    ownsPhotos(PHOTO_A);
    CookingHistory.create.mockResolvedValue(entry({ photoFileId: PHOTO_A }));

    const res = await request(app).post('/api/history').set(AUTH).send(body);

    expect(res.status).toBe(201);
    expect(res.body.photoFileId).toBe(PHOTO_A);
    expect(CookingHistory.create).toHaveBeenCalledWith(
      expect.objectContaining({ userId: 'uid-1', photoFileId: PHOTO_A })
    );
  });

  it('refuses a photo that is not the user\'s own', async () => {
    ownsPhotos();

    const res = await request(app).post('/api/history').set(AUTH).send(body);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('Photo not found');
    expect(CookingHistory.create).not.toHaveBeenCalled();
  });

  it('still works without a photo', async () => {
    CookingHistory.create.mockResolvedValue(entry());

    const res = await request(app)
      .post('/api/history')
      .set(AUTH)
      .send({ recipeSourceId: '716429', recipeTitle: 'Pasta' });

    expect(res.status).toBe(201);
    expect(res.body.photoFileId).toBeNull();
    expect(fileStorage.findOwned).not.toHaveBeenCalled();
  });
});

describe('PATCH /api/history/:id', () => {
  it('attaches a photo the user owns', async () => {
    const found = entry();
    CookingHistory.findOne.mockResolvedValue(found);
    ownsPhotos(PHOTO_A);

    const res = await request(app)
      .patch(`/api/history/${ENTRY_ID}`)
      .set(AUTH)
      .send({ photoFileId: PHOTO_A });

    expect(res.status).toBe(200);
    expect(res.body.photoFileId).toBe(PHOTO_A);
    expect(found.save).toHaveBeenCalled();
    expect(CookingHistory.findOne).toHaveBeenCalledWith({ _id: ENTRY_ID, userId: 'uid-1' });
    expect(fileStorage.remove).not.toHaveBeenCalled();
  });

  it('refuses a photo that is not the user\'s own and changes nothing', async () => {
    const found = entry();
    CookingHistory.findOne.mockResolvedValue(found);
    ownsPhotos();

    const res = await request(app)
      .patch(`/api/history/${ENTRY_ID}`)
      .set(AUTH)
      .send({ photoFileId: PHOTO_A });

    expect(res.status).toBe(400);
    expect(found.save).not.toHaveBeenCalled();
  });

  it('deletes the old photo when it is replaced', async () => {
    CookingHistory.findOne.mockResolvedValue(entry({ photoFileId: PHOTO_A }));
    CookingHistory.exists.mockResolvedValue(null);
    ownsPhotos(PHOTO_A, PHOTO_B);

    const res = await request(app)
      .patch(`/api/history/${ENTRY_ID}`)
      .set(AUTH)
      .send({ photoFileId: PHOTO_B });

    expect(res.body.photoFileId).toBe(PHOTO_B);
    expect(fileStorage.remove).toHaveBeenCalledWith(PHOTO_A);
  });

  it('keeps the old photo when another entry still uses it', async () => {
    CookingHistory.findOne.mockResolvedValue(entry({ photoFileId: PHOTO_A }));
    CookingHistory.exists.mockResolvedValue({ _id: 'other' });
    ownsPhotos(PHOTO_A, PHOTO_B);

    await request(app)
      .patch(`/api/history/${ENTRY_ID}`)
      .set(AUTH)
      .send({ photoFileId: PHOTO_B });

    expect(fileStorage.remove).not.toHaveBeenCalled();
  });

  it('removes the photo from the entry and deletes it when given null', async () => {
    CookingHistory.findOne.mockResolvedValue(entry({ photoFileId: PHOTO_A }));
    CookingHistory.exists.mockResolvedValue(null);
    ownsPhotos(PHOTO_A);

    const res = await request(app)
      .patch(`/api/history/${ENTRY_ID}`)
      .set(AUTH)
      .send({ photoFileId: null });

    expect(res.status).toBe(200);
    expect(res.body.photoFileId).toBeNull();
    expect(fileStorage.remove).toHaveBeenCalledWith(PHOTO_A);
  });

  it('does nothing to the photo when the same one is sent again', async () => {
    CookingHistory.findOne.mockResolvedValue(entry({ photoFileId: PHOTO_A }));
    ownsPhotos(PHOTO_A);

    const res = await request(app)
      .patch(`/api/history/${ENTRY_ID}`)
      .set(AUTH)
      .send({ photoFileId: PHOTO_A });

    expect(res.status).toBe(200);
    expect(fileStorage.remove).not.toHaveBeenCalled();
  });

  it('answers 404 for an entry that belongs to someone else', async () => {
    CookingHistory.findOne.mockResolvedValue(null);

    const res = await request(app)
      .patch(`/api/history/${ENTRY_ID}`)
      .set(AUTH)
      .send({ photoFileId: PHOTO_A });

    expect(res.status).toBe(404);
  });

  it('answers 404 for an entry id that cannot exist', async () => {
    const res = await request(app)
      .patch('/api/history/not-an-id')
      .set(AUTH)
      .send({ photoFileId: PHOTO_A });

    expect(res.status).toBe(404);
    expect(CookingHistory.findOne).not.toHaveBeenCalled();
  });

  it('rejects a body that is missing or has a bad photo id', async () => {
    const empty = await request(app).patch(`/api/history/${ENTRY_ID}`).set(AUTH).send({});
    const bad = await request(app)
      .patch(`/api/history/${ENTRY_ID}`)
      .set(AUTH)
      .send({ photoFileId: 'abc' });

    expect(empty.status).toBe(400);
    expect(bad.status).toBe(400);
    expect(CookingHistory.findOne).not.toHaveBeenCalled();
  });

  it('requires a signed-in user', async () => {
    const res = await request(app)
      .patch(`/api/history/${ENTRY_ID}`)
      .send({ photoFileId: PHOTO_A });

    expect(res.status).toBe(401);
  });
});
