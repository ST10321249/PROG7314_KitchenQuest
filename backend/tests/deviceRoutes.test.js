jest.mock('../src/config/firebaseAdmin', () => ({}));
jest.mock('firebase-admin/auth', () => ({ getAuth: jest.fn() }));
jest.mock('firebase-admin/messaging', () => ({ getMessaging: jest.fn() }));
jest.mock('../src/models/DeviceToken', () => ({
  findOneAndUpdate: jest.fn(),
  findOne: jest.fn(),
  find: jest.fn(),
  deleteOne: jest.fn(),
  deleteMany: jest.fn(),
}));

const request = require('supertest');
const { getAuth } = require('firebase-admin/auth');
const { getMessaging } = require('firebase-admin/messaging');
const DeviceToken = require('../src/models/DeviceToken');
const app = require('../src/app');

const AUTH = { Authorization: 'Bearer valid-token' };
const TOKEN = 'fcm-token-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa:APA91bExample';
const OTHER_TOKEN = 'fcm-token-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb:APA91bExample';

let sendEachForMulticast;

function multicastResult(results) {
  return {
    successCount: results.filter((r) => r.success).length,
    failureCount: results.filter((r) => !r.success).length,
    responses: results,
  };
}

beforeEach(() => {
  jest.resetAllMocks();
  getAuth.mockReturnValue({ verifyIdToken: jest.fn().mockResolvedValue({ uid: 'uid-1' }) });
  sendEachForMulticast = jest.fn();
  getMessaging.mockReturnValue({ sendEachForMulticast });
});

describe('POST /api/devices', () => {
  it('requires a signed-in user', async () => {
    const res = await request(app).post('/api/devices').send({ token: TOKEN });

    expect(res.status).toBe(401);
    expect(DeviceToken.findOneAndUpdate).not.toHaveBeenCalled();
  });

  it('saves the token for the signed-in user', async () => {
    DeviceToken.findOneAndUpdate.mockResolvedValue({
      _id: 'dev-1',
      platform: 'android',
      lastSeenAt: new Date('2026-10-24T08:00:00.000Z'),
    });

    const res = await request(app).post('/api/devices').set(AUTH).send({ token: TOKEN });

    expect(res.status).toBe(200);
    expect(res.body).toEqual({
      id: 'dev-1',
      platform: 'android',
      lastSeenAt: '2026-10-24T08:00:00.000Z',
    });
    expect(DeviceToken.findOneAndUpdate).toHaveBeenCalledWith(
      { token: TOKEN },
      { $set: expect.objectContaining({ userId: 'uid-1', platform: 'android' }) },
      expect.objectContaining({ upsert: true })
    );
  });

  it('never sends the token back in the response', async () => {
    DeviceToken.findOneAndUpdate.mockResolvedValue({
      _id: 'dev-1',
      token: TOKEN,
      platform: 'android',
      lastSeenAt: new Date(),
    });

    const res = await request(app).post('/api/devices').set(AUTH).send({ token: TOKEN });

    expect(JSON.stringify(res.body)).not.toContain(TOKEN);
  });

  it('moves a token to the account signed in now', async () => {
    DeviceToken.findOneAndUpdate.mockResolvedValue({ _id: 'dev-1', platform: 'android' });
    getAuth.mockReturnValue({ verifyIdToken: jest.fn().mockResolvedValue({ uid: 'uid-2' }) });

    await request(app).post('/api/devices').set(AUTH).send({ token: TOKEN });

    expect(DeviceToken.findOneAndUpdate).toHaveBeenCalledWith(
      { token: TOKEN },
      { $set: expect.objectContaining({ userId: 'uid-2' }) },
      expect.anything()
    );
  });

  it('rejects a missing, short or spaced token', async () => {
    const missing = await request(app).post('/api/devices').set(AUTH).send({});
    const short = await request(app).post('/api/devices').set(AUTH).send({ token: 'abc' });
    const spaced = await request(app)
      .post('/api/devices')
      .set(AUTH)
      .send({ token: 'a token with spaces in it that is long enough' });

    expect(missing.status).toBe(400);
    expect(short.status).toBe(400);
    expect(spaced.status).toBe(400);
    expect(DeviceToken.findOneAndUpdate).not.toHaveBeenCalled();
  });

  it('rejects a platform that is not supported', async () => {
    const res = await request(app)
      .post('/api/devices')
      .set(AUTH)
      .send({ token: TOKEN, platform: 'toaster' });

    expect(res.status).toBe(400);
  });
});

describe('DELETE /api/devices/:token', () => {
  it('only removes the signed-in user\'s own token', async () => {
    DeviceToken.deleteOne.mockResolvedValue({ deletedCount: 1 });

    const res = await request(app)
      .delete(`/api/devices/${encodeURIComponent(TOKEN)}`)
      .set(AUTH);

    expect(res.status).toBe(204);
    expect(DeviceToken.deleteOne).toHaveBeenCalledWith({ userId: 'uid-1', token: TOKEN });
  });

  it('succeeds when the token is already gone', async () => {
    DeviceToken.deleteOne.mockResolvedValue({ deletedCount: 0 });

    const res = await request(app)
      .delete(`/api/devices/${encodeURIComponent(TOKEN)}`)
      .set(AUTH);

    expect(res.status).toBe(204);
  });

  it('rejects a token that cannot be valid', async () => {
    const res = await request(app).delete('/api/devices/abc').set(AUTH);

    expect(res.status).toBe(400);
    expect(DeviceToken.deleteOne).not.toHaveBeenCalled();
  });

  it('requires a signed-in user', async () => {
    const res = await request(app).delete(`/api/devices/${encodeURIComponent(TOKEN)}`);

    expect(res.status).toBe(401);
  });
});

describe('POST /api/notifications/test', () => {
  it('requires a signed-in user', async () => {
    const res = await request(app).post('/api/notifications/test').send({});

    expect(res.status).toBe(401);
    expect(sendEachForMulticast).not.toHaveBeenCalled();
  });

  it('sends to every device registered for the signed-in user only', async () => {
    DeviceToken.find.mockResolvedValue([{ token: TOKEN }, { token: OTHER_TOKEN }]);
    sendEachForMulticast.mockResolvedValue(
      multicastResult([{ success: true }, { success: true }])
    );

    const res = await request(app).post('/api/notifications/test').set(AUTH).send({});

    expect(res.status).toBe(200);
    expect(res.body).toEqual({ sent: 2, failed: 0, removed: 0 });
    expect(DeviceToken.find).toHaveBeenCalledWith({ userId: 'uid-1' });
    expect(sendEachForMulticast).toHaveBeenCalledWith(
      expect.objectContaining({
        tokens: [TOKEN, OTHER_TOKEN],
        notification: { title: 'KitchenQuest', body: 'Push notifications are working.' },
        data: { type: 'test' },
      })
    );
  });

  it('can target a single one of the user\'s devices', async () => {
    DeviceToken.findOne.mockResolvedValue({ token: OTHER_TOKEN });
    sendEachForMulticast.mockResolvedValue(multicastResult([{ success: true }]));

    const res = await request(app)
      .post('/api/notifications/test')
      .set(AUTH)
      .send({ token: OTHER_TOKEN });

    expect(res.status).toBe(200);
    expect(DeviceToken.findOne).toHaveBeenCalledWith({ userId: 'uid-1', token: OTHER_TOKEN });
    expect(sendEachForMulticast).toHaveBeenCalledWith(
      expect.objectContaining({ tokens: [OTHER_TOKEN] })
    );
  });

  it('will not send to a token that belongs to someone else', async () => {
    DeviceToken.findOne.mockResolvedValue(null);

    const res = await request(app)
      .post('/api/notifications/test')
      .set(AUTH)
      .send({ token: OTHER_TOKEN });

    expect(res.status).toBe(404);
    expect(sendEachForMulticast).not.toHaveBeenCalled();
  });

  it('answers 404 when the user has no registered device', async () => {
    DeviceToken.find.mockResolvedValue([]);

    const res = await request(app).post('/api/notifications/test').set(AUTH).send({});

    expect(res.status).toBe(404);
    expect(res.body.error).toMatch(/No registered device/);
    expect(sendEachForMulticast).not.toHaveBeenCalled();
  });

  it('removes tokens that Firebase says are no longer valid', async () => {
    DeviceToken.find.mockResolvedValue([{ token: TOKEN }, { token: OTHER_TOKEN }]);
    DeviceToken.deleteMany.mockResolvedValue({ deletedCount: 1 });
    sendEachForMulticast.mockResolvedValue(
      multicastResult([
        { success: true },
        { success: false, error: { code: 'messaging/registration-token-not-registered' } },
      ])
    );

    const res = await request(app).post('/api/notifications/test').set(AUTH).send({});

    expect(res.body).toEqual({ sent: 1, failed: 1, removed: 1 });
    expect(DeviceToken.deleteMany).toHaveBeenCalledWith({ token: { $in: [OTHER_TOKEN] } });
  });

  it('keeps tokens when the failure may be temporary', async () => {
    DeviceToken.find.mockResolvedValue([{ token: TOKEN }]);
    sendEachForMulticast.mockResolvedValue(
      multicastResult([{ success: false, error: { code: 'messaging/server-unavailable' } }])
    );

    const res = await request(app).post('/api/notifications/test').set(AUTH).send({});

    expect(res.body).toEqual({ sent: 0, failed: 1, removed: 0 });
    expect(DeviceToken.deleteMany).not.toHaveBeenCalled();
  });

  it('hides Firebase errors behind a generic server error', async () => {
    DeviceToken.find.mockResolvedValue([{ token: TOKEN }]);
    sendEachForMulticast.mockRejectedValue(new Error('credential details here'));
    const spy = jest.spyOn(console, 'error').mockImplementation(() => {});

    const res = await request(app).post('/api/notifications/test').set(AUTH).send({});

    expect(res.status).toBe(500);
    expect(res.body.error).toBe('Internal server error');
    spy.mockRestore();
  });
});
