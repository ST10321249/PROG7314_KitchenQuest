jest.mock('../src/config/firebaseAdmin', () => ({}));
jest.mock('firebase-admin/auth', () => ({ getAuth: jest.fn() }));
jest.mock('firebase-admin/messaging', () => ({ getMessaging: jest.fn() }));

const fs = require('fs');
const path = require('path');
const request = require('supertest');
const { getAuth } = require('firebase-admin/auth');
const app = require('../src/app');

// Where each router is mounted in app.js. The health check is the only public router.
const MOUNTS = {
  userRoutes: '/api/users',
  pantryRoutes: '/api/pantry',
  shoppingRoutes: '/api/shopping-list',
  recipeRoutes: '/api/recipes',
  favouriteRoutes: '/api/favourites',
  historyRoutes: '/api/history',
  recipeNoteRoutes: '/api/recipe-notes',
  deviceRoutes: '/api/devices',
  notificationRoutes: '/api/notifications',
  fileRoutes: '/api/files',
};
const PUBLIC_ROUTERS = ['healthRoutes'];

const routesDir = path.join(__dirname, '../src/routes');

function endpointsOf(routerName) {
  const router = require(path.join(routesDir, routerName));
  const endpoints = [];

  router.stack.forEach((layer) => {
    if (!layer.route) return;
    Object.keys(layer.route.methods).forEach((method) => {
      endpoints.push({ method, path: layer.route.path });
    });
  });

  return endpoints;
}

// Fills in route parameters such as :id so the request reaches the route.
function concretePath(mount, routePath) {
  const full = routePath === '/' ? mount : `${mount}${routePath}`;
  return full.replace(/:[A-Za-z]+/g, 'abc123');
}

const everyEndpoint = Object.entries(MOUNTS).flatMap(([routerName, mount]) =>
  endpointsOf(routerName).map((endpoint) => ({
    label: `${endpoint.method.toUpperCase()} ${mount}${endpoint.path === '/' ? '' : endpoint.path}`,
    method: endpoint.method,
    url: concretePath(mount, endpoint.path),
  }))
);

beforeEach(() => {
  jest.resetAllMocks();
  getAuth.mockReturnValue({
    verifyIdToken: jest.fn().mockRejectedValue(new Error('invalid token')),
  });
});

describe('every API route needs a signed-in user', () => {
  it('knows about every router, so a new one cannot be forgotten', () => {
    const onDisk = fs
      .readdirSync(routesDir)
      .filter((file) => file.endsWith('.js'))
      .map((file) => file.replace('.js', ''));

    const known = [...Object.keys(MOUNTS), ...PUBLIC_ROUTERS].sort();

    expect(onDisk.sort()).toEqual(known);
  });

  it('finds a sensible number of routes to check', () => {
    expect(everyEndpoint.length).toBeGreaterThan(25);
  });

  it.each(everyEndpoint)('$label answers 401 without a token', async ({ method, url }) => {
    const res = await request(app)[method](url).send({});

    expect(res.status).toBe(401);
  });

  it.each(everyEndpoint)('$label answers 401 with an invalid token', async ({ method, url }) => {
    const res = await request(app)[method](url).set('Authorization', 'Bearer not-valid').send({});

    expect(res.status).toBe(401);
  });

  it('keeps the health check public', async () => {
    const res = await request(app).get('/health');

    expect(res.status).toBe(200);
  });
});
