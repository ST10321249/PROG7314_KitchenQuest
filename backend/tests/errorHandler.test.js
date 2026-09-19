const express = require('express');
const request = require('supertest');
const ApiError = require('../src/utils/ApiError');
const errorHandler = require('../src/middleware/errorHandler');
const notFoundHandler = require('../src/middleware/notFoundHandler');

function buildApp() {
  const app = express();
  app.use(express.json());
  app.get('/api-error', () => {
    throw new ApiError(400, 'Bad input');
  });
  app.get('/crash', () => {
    throw new Error('E11000 duplicate key error collection: kitchenquest.users');
  });
  app.post('/echo', (req, res) => res.json(req.body));
  app.use(notFoundHandler);
  app.use(errorHandler);
  return app;
}

describe('error handling', () => {
  let consoleError;

  beforeEach(() => {
    consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleError.mockRestore();
  });

  it('returns the message of a deliberate ApiError', async () => {
    const res = await request(buildApp()).get('/api-error');

    expect(res.status).toBe(400);
    expect(res.body).toEqual({ error: 'Bad input' });
  });

  it('hides the details of an unexpected error from the client', async () => {
    const res = await request(buildApp()).get('/crash');

    expect(res.status).toBe(500);
    expect(res.body).toEqual({ error: 'Internal server error' });
    expect(JSON.stringify(res.body)).not.toContain('E11000');
  });

  it('still logs the real error on the server', async () => {
    await request(buildApp()).get('/crash');

    expect(consoleError).toHaveBeenCalledTimes(1);
    expect(consoleError.mock.calls[0][0].message).toContain('E11000');
  });

  it('returns a 400 for malformed JSON instead of a server error', async () => {
    const res = await request(buildApp())
      .post('/echo')
      .set('Content-Type', 'application/json')
      .send('{"broken":');

    expect(res.status).toBe(400);
    expect(consoleError).not.toHaveBeenCalled();
  });

  it('returns a JSON 404 for unknown routes', async () => {
    const res = await request(buildApp()).get('/nope');

    expect(res.status).toBe(404);
    expect(res.body.error).toContain('Route not found');
  });
});
