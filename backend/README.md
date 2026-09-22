# KitchenQuest API

Backend for the KitchenQuest app - a REST API written in Node and Express. It stores everything in MongoDB and checks every request against Firebase to make sure whoever's calling it actually is who they say they are.

```
Android app -> Retrofit -> Express API -> Firebase Admin checks the token -> Mongoose -> MongoDB
```

## Built with

Node.js + Express for the server, MongoDB + Mongoose for storage, the Firebase Admin SDK to verify tokens, Spoonacular's API for recipe data, Zod for validating what comes in, Morgan for request logs, Jest + Supertest for the tests.

## Layout

```
backend/
  src/
    config/       env loading, MongoDB connection, Firebase Admin setup
    controllers/  the request handlers
    middleware/   auth check, validation, 404s, error handling
    models/       Mongoose schemas
    routes/       route definitions
    utils/        shared helpers (ApiError)
    validators/   Zod schemas
    app.js        the Express app itself, no listener attached (so it's testable)
    server.js     connects to Mongo, then starts listening
  tests/          Jest tests
  .env.example    template for the env file
  package.json
```

## Getting it running

Needs Node 22+ - Firebase Admin won't run on anything older.

```
cd backend -> npm install -> cp .env.example .env
```

Fill in `.env`, add a Firebase service account key (see below), then:
```
npm run dev     # restarts on save
npm start       # plain start
```

The server connects to MongoDB before it starts listening, so if that connection fails it just exits rather than quietly accepting requests it can't do anything with.

### Env vars

| Variable | What it's for | Default |
|---|---|---|
| `NODE_ENV` | development or production | development |
| `PORT` | port the server runs on | 5000 |
| `MONGODB_URI` | MongoDB connection string, database name included | required |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | path to the Firebase service account JSON | required |
| `SPOONACULAR_API_KEY` | key used for recipe requests | required |

`.env` and the service account key are both in `.gitignore` - don't commit them. Same for the Spoonacular key: keep it in `.env` locally and in whatever's hosting the API, never in the code itself.

### MongoDB Atlas

Spin up a free cluster, add a database user, drop the connection string into `MONGODB_URI` (the database name goes in before the `?`, e.g. `/kitchenquest`). Under Network Access, allow the IP running the API, or `0.0.0.0/0` while just developing. If the server hangs and then fails to connect, an IP that's not on that list is almost always why.

### Firebase service account key

Firebase console -> Project settings -> Service accounts -> Generate new private key. Save it as `backend/serviceAccountKey.json`, or point `FIREBASE_SERVICE_ACCOUNT_PATH` somewhere else if preferred. It has to come from the same Firebase project the app signs into.

## Auth

Every route except `/health` needs:
```
Authorization: Bearer <Firebase ID token>
```

The Admin SDK verifies it, and the user gets identified from the token's `uid` - never from anything sent in the body or URL. No token, or a bad one, gets a `401`.

## Endpoints

**`GET /health`** - public, just confirms the API is up.
```json
{ "status": "ok" }
```

**`POST /api/users/sync`** - creates the signed-in user's profile if there isn't one yet, otherwise returns what's already there (never overwrites it). Call this right after sign-in.
```json
{
  "displayName": "Example User",
  "dietaryPreferences": ["Vegetarian"],
  "avoidedIngredients": ["Peanuts", "Shellfish"]
}
```
`201` for a new profile, `200` if one already existed. No `displayName` sent -> falls back to whatever Firebase has. `email` always comes from the token, not the body.

**`GET /api/users/me`** - the signed-in user's profile, or `404` if they haven't synced yet.

**`PUT /api/users/me`** - updates whichever of `displayName`, `dietaryPreferences`, `avoidedIngredients` gets sent. Only sent fields change; anything else (including trying to sneak in a `firebaseUid` or `email`) is ignored. Needs at least one field. `200` with the updated profile, `400` for bad data, `404` if there's no profile yet.

## Errors

Every error comes back the same shape:
```json
{ "error": "message" }
```
| Status | Meaning |
|---|---|
| `400` | bad request data |
| `401` | missing or invalid token |
| `404` | route or profile not found |
| `500` | something broke server-side (the real error is logged server-side, the client just gets a generic message) |

## Where it's hosted

Live on Render: **https://prog7314-kitchenquest.onrender.com** - `/health` confirms it's up.

Set up as Node, root directory `backend`, `npm install` to build, `npm start` to run, health check on `/health`, deploying off the `api-data-layer` branch for now (switch to `main` once everything's merged in).

Env vars are set directly in Render - same names as above, plus `NODE_VERSION=22` - and `FIREBASE_SERVICE_ACCOUNT_PATH` points at `/etc/secrets/serviceAccountKey.json`. The actual key file goes in under Secret Files there, not as an env var value.

It's on the free tier, so it sleeps after a while idle, and the first request after that can take close to a minute to wake it back up - worth remembering before a demo.

To point at a local backend instead, add `api.baseUrl=http://localhost:5000` to `KitchenQuest/local.properties`. On a phone over USB, also run `adb reverse tcp:5000 tcp:5000`.

## Tests

```
npm test
```

Runs with Jest and Supertest. Firebase and the database are both mocked, so none of it needs a `.env`, a service account key, or an internet connection.
