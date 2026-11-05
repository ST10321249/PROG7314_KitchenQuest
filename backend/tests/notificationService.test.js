jest.mock('../src/config/firebaseAdmin', () => ({}));
jest.mock('firebase-admin/messaging', () => ({ getMessaging: jest.fn() }));
jest.mock('../src/models/DeviceToken', () => ({
  find: jest.fn(),
  deleteMany: jest.fn(),
}));

const { getMessaging } = require('firebase-admin/messaging');
const DeviceToken = require('../src/models/DeviceToken');
const { sendToTokens, sendToUser } = require('../src/services/notificationService');

let sendEachForMulticast;

function allSucceed(tokens) {
  return {
    successCount: tokens.length,
    failureCount: 0,
    responses: tokens.map(() => ({ success: true })),
  };
}

beforeEach(() => {
  jest.resetAllMocks();
  sendEachForMulticast = jest.fn(({ tokens }) => Promise.resolve(allSucceed(tokens)));
  getMessaging.mockReturnValue({ sendEachForMulticast });
});

describe('sendToTokens', () => {
  it('turns every data value into a string, as Firebase requires', async () => {
    await sendToTokens(['t1'], {
      title: 'Expiring soon',
      body: 'Milk expires tomorrow',
      data: { type: 'expiry', count: 3, urgent: true, skipped: undefined, nothing: null },
    });

    expect(sendEachForMulticast.mock.calls[0][0].data).toEqual({
      type: 'expiry',
      count: '3',
      urgent: 'true',
    });
  });

  it('sends at high priority and can name an Android channel', async () => {
    await sendToTokens(['t1'], { title: 'a', body: 'b', channelId: 'expiry_alerts' });

    expect(sendEachForMulticast.mock.calls[0][0].android).toEqual({
      priority: 'high',
      notification: { channelId: 'expiry_alerts' },
    });
  });

  it('leaves the channel out when none is given', async () => {
    await sendToTokens(['t1'], { title: 'a', body: 'b' });

    expect(sendEachForMulticast.mock.calls[0][0].android).toEqual({ priority: 'high' });
  });

  it('splits more than 500 tokens into separate requests', async () => {
    const tokens = Array.from({ length: 1001 }, (_, i) => `token-${i}`);

    const result = await sendToTokens(tokens, { title: 'a', body: 'b' });

    expect(sendEachForMulticast).toHaveBeenCalledTimes(3);
    expect(sendEachForMulticast.mock.calls.map(([call]) => call.tokens.length)).toEqual([
      500, 500, 1,
    ]);
    expect(result).toEqual({ sent: 1001, failed: 0, removed: 0 });
  });

  it('removes the right tokens when failures are spread across batches', async () => {
    const tokens = Array.from({ length: 501 }, (_, i) => `token-${i}`);
    sendEachForMulticast
      .mockResolvedValueOnce({
        successCount: 499,
        failureCount: 1,
        responses: tokens.slice(0, 500).map((_, i) =>
          i === 7
            ? { success: false, error: { code: 'messaging/invalid-registration-token' } }
            : { success: true }
        ),
      })
      .mockResolvedValueOnce({
        successCount: 0,
        failureCount: 1,
        responses: [
          { success: false, error: { code: 'messaging/registration-token-not-registered' } },
        ],
      });
    DeviceToken.deleteMany.mockResolvedValue({});

    const result = await sendToTokens(tokens, { title: 'a', body: 'b' });

    expect(result).toEqual({ sent: 499, failed: 2, removed: 2 });
    expect(DeviceToken.deleteMany).toHaveBeenCalledWith({
      token: { $in: ['token-7', 'token-500'] },
    });
  });

  it('does not touch the database when every send worked', async () => {
    await sendToTokens(['t1', 't2'], { title: 'a', body: 'b' });

    expect(DeviceToken.deleteMany).not.toHaveBeenCalled();
  });
});

describe('sendToUser', () => {
  it('returns null without calling Firebase when the user has no devices', async () => {
    DeviceToken.find.mockResolvedValue([]);

    const result = await sendToUser('uid-1', { title: 'a', body: 'b' });

    expect(result).toBeNull();
    expect(sendEachForMulticast).not.toHaveBeenCalled();
  });

  it('sends to all of the user\'s devices', async () => {
    DeviceToken.find.mockResolvedValue([{ token: 't1' }, { token: 't2' }]);

    const result = await sendToUser('uid-1', { title: 'a', body: 'b' });

    expect(DeviceToken.find).toHaveBeenCalledWith({ userId: 'uid-1' });
    expect(sendEachForMulticast.mock.calls[0][0].tokens).toEqual(['t1', 't2']);
    expect(result.sent).toBe(2);
  });
});
