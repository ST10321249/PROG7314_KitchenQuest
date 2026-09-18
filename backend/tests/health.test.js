jest.mock('../src/config/firebaseAdmin', () => ({}));
jest.mock('firebase-admin/auth', () => ({ getAuth: jest.fn() }));

const request = require('supertest');
const app = require('../src/app');

describe('GET /health', () => {
  it('returns 200 and an ok status', async () => {
    const res = await request(app).get('/health');

    expect(res.status).toBe(200);
    expect(res.body).toEqual({ status: 'ok' });
  });

  it('returns a JSON 404 for unknown routes', async () => {
    const res = await request(app).get('/does-not-exist');

    expect(res.status).toBe(404);
    expect(res.body.error).toContain('Route not found');
  });
});
