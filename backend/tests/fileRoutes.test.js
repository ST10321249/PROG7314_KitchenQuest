jest.mock('../src/config/firebaseAdmin', () => ({}));
jest.mock('firebase-admin/auth', () => ({ getAuth: jest.fn() }));
jest.mock('../src/models/CookingHistory', () => ({ updateMany: jest.fn() }));
jest.mock('../src/services/fileStorage', () => ({
  save: jest.fn(),
  findOwned: jest.fn(),
  openDownloadStream: jest.fn(),
  remove: jest.fn(),
  countForUser: jest.fn(),
}));

const { Readable } = require('stream');
const request = require('supertest');
const { getAuth } = require('firebase-admin/auth');
const CookingHistory = require('../src/models/CookingHistory');
const fileStorage = require('../src/services/fileStorage');
const app = require('../src/app');
const { maxFileBytes, maxFilesPerUser } = require('../src/config/files');

const AUTH = { Authorization: 'Bearer valid-token' };
const FILE_ID = '507f1f77bcf86cd799439011';

const JPEG = Buffer.concat([Buffer.from([0xff, 0xd8, 0xff, 0xe0]), Buffer.alloc(40, 1)]);
const PNG = Buffer.concat([
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
  Buffer.alloc(40, 2),
]);
const WEBP = Buffer.concat([
  Buffer.from('RIFF'),
  Buffer.from([0x24, 0, 0, 0]),
  Buffer.from('WEBPVP8 '),
  Buffer.alloc(40, 3),
]);

const stored = {
  id: FILE_ID,
  contentType: 'image/jpeg',
  size: JPEG.length,
  createdAt: '2026-10-24T08:00:00.000Z',
};

beforeEach(() => {
  jest.resetAllMocks();
  getAuth.mockReturnValue({ verifyIdToken: jest.fn().mockResolvedValue({ uid: 'uid-1' }) });
  fileStorage.countForUser.mockResolvedValue(0);
});

describe('POST /api/files', () => {
  it('requires a signed-in user and reads nothing before checking', async () => {
    const res = await request(app).post('/api/files').set('Content-Type', 'image/jpeg').send(JPEG);

    expect(res.status).toBe(401);
    expect(fileStorage.save).not.toHaveBeenCalled();
  });

  it.each([
    ['image/jpeg', JPEG],
    ['image/png', PNG],
    ['image/webp', WEBP],
  ])('stores a valid %s photo for the signed-in user', async (type, bytes) => {
    fileStorage.save.mockResolvedValue({ ...stored, contentType: type });

    const res = await request(app).post('/api/files').set(AUTH).set('Content-Type', type).send(bytes);

    expect(res.status).toBe(201);
    expect(res.body.id).toBe(FILE_ID);
    expect(fileStorage.save).toHaveBeenCalledWith({
      ownerUid: 'uid-1',
      buffer: expect.any(Buffer),
      contentType: type,
    });
    expect(fileStorage.save.mock.calls[0][0].buffer.equals(bytes)).toBe(true);
  });

  it('rejects a file that only claims to be an image', async () => {
    const res = await request(app)
      .post('/api/files')
      .set(AUTH)
      .set('Content-Type', 'image/jpeg')
      .send(Buffer.from('<script>alert(1)</script> not an image at all'));

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/not a valid/);
    expect(fileStorage.save).not.toHaveBeenCalled();
  });

  it('rejects an image whose content type does not match its bytes', async () => {
    const res = await request(app)
      .post('/api/files')
      .set(AUTH)
      .set('Content-Type', 'image/jpeg')
      .send(PNG);

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/does not match/);
  });

  it('rejects content types that are not photos', async () => {
    const pdf = await request(app)
      .post('/api/files')
      .set(AUTH)
      .set('Content-Type', 'application/pdf')
      .send(Buffer.from('%PDF-1.4 something'));
    const json = await request(app).post('/api/files').set(AUTH).send({ photo: 'abc' });

    expect(pdf.status).toBe(415);
    expect(json.status).toBe(415);
    expect(fileStorage.save).not.toHaveBeenCalled();
  });

  it('rejects an empty upload', async () => {
    const res = await request(app).post('/api/files').set(AUTH).set('Content-Type', 'image/jpeg');

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('The photo is empty');
  });

  it('rejects a photo that is over the size limit', async () => {
    const tooBig = Buffer.concat([JPEG, Buffer.alloc(maxFileBytes)]);

    const res = await request(app)
      .post('/api/files')
      .set(AUTH)
      .set('Content-Type', 'image/jpeg')
      .send(tooBig);

    expect(res.status).toBe(413);
    expect(fileStorage.save).not.toHaveBeenCalled();
  });

  it('stops a user storing more than the allowed number of photos', async () => {
    fileStorage.countForUser.mockResolvedValue(maxFilesPerUser);

    const res = await request(app)
      .post('/api/files')
      .set(AUTH)
      .set('Content-Type', 'image/jpeg')
      .send(JPEG);

    expect(res.status).toBe(409);
    expect(fileStorage.countForUser).toHaveBeenCalledWith('uid-1');
    expect(fileStorage.save).not.toHaveBeenCalled();
  });
});

describe('GET /api/files/:id', () => {
  it('requires a signed-in user', async () => {
    const res = await request(app).get(`/api/files/${FILE_ID}`);

    expect(res.status).toBe(401);
  });

  it('sends the photo to its owner with safe headers', async () => {
    fileStorage.findOwned.mockResolvedValue(stored);
    fileStorage.openDownloadStream.mockReturnValue(Readable.from([JPEG]));

    const res = await request(app).get(`/api/files/${FILE_ID}`).set(AUTH).buffer(true).parse(
      (response, done) => {
        const chunks = [];
        response.on('data', (chunk) => chunks.push(chunk));
        response.on('end', () => done(null, Buffer.concat(chunks)));
      }
    );

    expect(res.status).toBe(200);
    expect(res.headers['content-type']).toBe('image/jpeg');
    expect(res.headers['x-content-type-options']).toBe('nosniff');
    expect(res.headers['cache-control']).toBe('private, max-age=86400');
    expect(res.body.equals(JPEG)).toBe(true);
    expect(fileStorage.findOwned).toHaveBeenCalledWith(FILE_ID, 'uid-1');
  });

  it('answers 304 when the app already has the photo', async () => {
    fileStorage.findOwned.mockResolvedValue(stored);

    const res = await request(app)
      .get(`/api/files/${FILE_ID}`)
      .set(AUTH)
      .set('If-None-Match', `"${FILE_ID}"`);

    expect(res.status).toBe(304);
    expect(fileStorage.openDownloadStream).not.toHaveBeenCalled();
  });

  it('answers 404 for a photo that is not the user\'s own, without revealing it exists', async () => {
    fileStorage.findOwned.mockResolvedValue(null);

    const res = await request(app).get(`/api/files/${FILE_ID}`).set(AUTH);

    expect(res.status).toBe(404);
    expect(res.body.error).toBe('Photo not found');
    expect(fileStorage.openDownloadStream).not.toHaveBeenCalled();
  });

  it('answers 404 for an id that cannot exist', async () => {
    fileStorage.findOwned.mockResolvedValue(null);

    const res = await request(app).get('/api/files/not-an-id').set(AUTH);

    expect(res.status).toBe(404);
  });
});

describe('DELETE /api/files/:id', () => {
  it('removes the photo and clears it from any history entry', async () => {
    fileStorage.findOwned.mockResolvedValue(stored);
    fileStorage.remove.mockResolvedValue();
    CookingHistory.updateMany.mockResolvedValue({});

    const res = await request(app).delete(`/api/files/${FILE_ID}`).set(AUTH);

    expect(res.status).toBe(204);
    expect(fileStorage.remove).toHaveBeenCalledWith(FILE_ID);
    expect(CookingHistory.updateMany).toHaveBeenCalledWith(
      { userId: 'uid-1', photoFileId: FILE_ID },
      { $set: { photoFileId: null } }
    );
  });

  it('will not delete a photo that belongs to someone else', async () => {
    fileStorage.findOwned.mockResolvedValue(null);

    const res = await request(app).delete(`/api/files/${FILE_ID}`).set(AUTH);

    expect(res.status).toBe(404);
    expect(fileStorage.remove).not.toHaveBeenCalled();
    expect(CookingHistory.updateMany).not.toHaveBeenCalled();
  });
});
