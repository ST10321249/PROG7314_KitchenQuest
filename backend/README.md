# KitchenQuest API

REST API for the KitchenQuest Android app, built with Node.js and Express. It stores user profile data in MongoDB (via Mongoose) and protects its routes by verifying Firebase ID tokens sent from the app.

```
Android app -> Retrofit -> Express API -> Firebase Admin (verify token) -> Mongoose -> MongoDB
```

## Tech stack

| Technology | Purpose |
|---|---|
| Node.js + Express | HTTP server and routing |
| MongoDB + Mongoose | Database and data models |
| Firebase Admin SDK | Verifies Firebase ID tokens |
| Zod | Request body validation |
| Morgan | Request logging |
| Jest + Supertest | Automated tests |

## Project structure

```
backend/
├── src/
│   ├── config/        env loading, MongoDB connection, Firebase Admin setup
│   ├── controllers/   request handlers
│   ├── middleware/    auth, validation, 404 and error handling
│   ├── models/        Mongoose schemas
│   ├── routes/        route definitions
│   ├── utils/         shared helpers (ApiError)
│   ├── validators/    Zod schemas
│   ├── app.js         Express app (no listener, so it can be tested)
│   └── server.js      connects to MongoDB, then starts listening
├── tests/             Jest tests
├── .env.example       template for environment variables
└── package.json
```

## Setup

Requires Node.js 22 or newer (a requirement of the Firebase Admin SDK).

1. Install dependencies:
   ```bash
   cd backend
   npm install
   ```
2. Create your environment file from the template and fill in the values:
   ```bash
   cp .env.example .env
   ```
3. Add a Firebase service account key (see below).
4. Start the server:
   ```bash
   npm run dev     # auto-restarts on file changes
   npm start       # plain start
   ```

On startup the server connects to MongoDB first, then begins listening. If the database connection fails it exits instead of accepting requests.

### Environment variables

| Variable | Description | Default |
|---|---|---|
| `NODE_ENV` | `development` or `production` | `development` |
| `PORT` | Port the server listens on | `5000` |
| `MONGODB_URI` | MongoDB connection string, including the database name | none (required) |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Path to the Firebase service account JSON | none (required) |

`.env` and the service account key are listed in `.gitignore`. Never commit them.

### MongoDB Atlas

Create a free cluster, add a database user, and copy the connection string into `MONGODB_URI` (add the database name, e.g. `/kitchenquest`, before the `?`). Under **Network Access**, allow the IP address of the machine running the API, or `0.0.0.0/0` for development. If the server hangs on start and then fails to connect, an IP that is not on this list is the usual cause.

### Firebase service account key

In the Firebase console open **Project settings -> Service accounts -> Generate new private key**. Save the downloaded file as `backend/serviceAccountKey.json` (or point `FIREBASE_SERVICE_ACCOUNT_PATH` at wherever you keep it). It must belong to the same Firebase project the Android app signs in with.

## Authentication

Every route except `/health` requires a Firebase ID token:

```
Authorization: Bearer <Firebase ID token>
```

The token is verified with the Firebase Admin SDK, and the user is identified from the verified token's `uid`. The API never trusts a user ID sent in the request body or URL. A missing or invalid token returns `401`.

## Endpoints

### `GET /health`

Public. Confirms the API is running.

```json
{ "status": "ok" }
```

### `POST /api/users/sync`

Creates the signed-in user's profile if it does not exist, otherwise returns the existing one. Call it after sign-in. All body fields are optional and only used when the profile is first created. An existing profile is never overwritten by this call.

Request body:

```json
{
  "displayName": "Example User",
  "dietaryPreferences": ["Vegetarian"],
  "avoidedIngredients": ["Peanuts", "Shellfish"]
}
```

Returns `201` with the new profile, or `200` with the existing one. If `displayName` is not sent, the name from the Firebase token is used. `email` always comes from the token.

### `GET /api/users/me`

Returns the signed-in user's profile, or `404` if they have not been synced yet.

```json
{
  "_id": "6aadb1c1ed585ca3c83ebd40",
  "firebaseUid": "firebase-user-id",
  "displayName": "Example User",
  "email": "user@example.com",
  "dietaryPreferences": ["Vegetarian"],
  "avoidedIngredients": ["Peanuts", "Shellfish"],
  "createdAt": "2026-09-18T21:48:49.623Z",
  "updatedAt": "2026-09-18T21:48:49.811Z"
}
```

### `PUT /api/users/me`

Updates the signed-in user's profile. Send any of `displayName`, `dietaryPreferences` and `avoidedIngredients`. Only the fields you send are changed. Other fields (including `firebaseUid` and `email`) are ignored. At least one field is required.

```json
{
  "dietaryPreferences": ["Vegetarian", "Gluten free"],
  "avoidedIngredients": ["Peanuts"]
}
```

Returns `200` with the updated profile, `400` for invalid data, or `404` if the user has not been synced yet.

## Errors

All errors are JSON in the same shape:

```json
{ "error": "message" }
```

| Status | Meaning |
|---|---|
| `400` | Invalid request data |
| `401` | Missing or invalid Firebase token |
| `404` | Route or user profile not found |
| `500` | Unexpected server error (details are logged on the server, not returned) |

## Hosted API (Render)

The API is deployed on Render at **https://prog7314-kitchenquest.onrender.com**. The Android app uses this address by default, so it works on a phone with no local setup. Check it with `https://prog7314-kitchenquest.onrender.com/health`.

Render settings:

| Setting | Value |
|---|---|
| Language | Node |
| Branch | `api-data-layer` (switch to `main` once it is merged) |
| Root Directory | `backend` |
| Build Command | `npm install` |
| Start Command | `npm start` |
| Health Check Path | `/health` |

Environment variables (set in Render, never committed):

| Variable | Value |
|---|---|
| `MONGODB_URI` | The MongoDB Atlas connection string |
| `NODE_ENV` | `production` |
| `NODE_VERSION` | `22` |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | `/etc/secrets/serviceAccountKey.json` |

The Firebase key is added under **Secret Files** with the filename `serviceAccountKey.json`.

Notes:
- Render redeploys automatically when the deployed branch is pushed to.
- On the free plan the server sleeps when idle, so the first request after a quiet period can take up to about a minute. The app allows for this.
- To run the app against an API on your own PC instead, add `api.baseUrl=http://localhost:5000` to `KitchenQuest/local.properties`. For a phone, also run `adb reverse tcp:5000 tcp:5000` so the phone's `localhost` reaches your PC.

## Testing

```bash
npm test
```

Tests use Jest and Supertest. Firebase and the database are mocked, so they need no `.env`, service account key, network or MongoDB connection.
