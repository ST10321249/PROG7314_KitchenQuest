# KitchenQuest

An Android cooking and recipe-management app that helps users manage the ingredients they have, reduce food waste, discover recipes they can actually make right now, and follow those recipes while cooking.

KitchenQuest takes an ingredient-first approach: instead of browsing recipes and then checking what you're missing, it matches recipes to what's already in your kitchen and prioritises ingredients that are close to expiring.

Built for **PROG7314 (Part 2 prototype)** by Group 13.

---

## Table of contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
- [Running tests](#running-tests)
- [What's planned next](#whats-planned-next)
- [Architecture](#architecture)
- [Team](#team)

---

## Features

### Implemented

**Account & onboarding**
- Register and sign in with email/password, or Google sign-in
- Password reset
- Session restored automatically on app restart
- Onboarding captures dietary preference(s) and avoided ingredients

**Settings & profile**
- View and edit display name, dietary preferences and avoided ingredients
- Changes are saved to the backend and to the phone, so they load correctly if the app is closed and reopened, or reinstalled

**My Kitchen (pantry)**
- Add, edit and delete pantry ingredients (name, quantity, unit, category, optional expiry date)
- Search and autocomplete from ingredients already in the pantry
- Ingredient detail screen

**Shopping list**
- Add, edit, delete and mark items purchased

**Recipes**
- Search recipes (via the Spoonacular API), with recipe details and ingredient scaling
- Save recipes as favourites
- "What Can I Make?" — matches recipes against what's in My Kitchen

**Cooking**
- Guided, step-by-step Cooking Mode
- Recipe timers and a standalone Kitchen Timer, both shown in one Active Timers list
- Cooking History, with the completed recipe, rating and notes saved at the end of a cook

**Backend**
- A REST API (Node.js/Express) backing all of the above, with MongoDB for storage
- Every protected route verifies the signed-in user's Firebase ID token before running
- Deployed and publicly reachable — the app talks to it with no local setup required

### Not yet implemented

These are intentionally left for later, per the project plan:
- The Home and Profile tabs are still placeholder screens (the features they'd summarise, like the pantry and recipes, already work from their own tabs)
- Offline mode (RoomDB) and background sync
- Biometric (fingerprint) unlock
- Push notifications (Firebase Cloud Messaging) for timers, expiry and shopping
- Multiple languages (English only for now)
- Final production imagery/icons

---

## Tech stack

| Layer | Technology |
|---|---|
| Android app | Kotlin, Jetpack Compose |
| Architecture | MVVM + Repository pattern |
| Networking | Retrofit + OkHttp |
| Authentication | Firebase Authentication (email/password + Google) |
| Backend | Node.js + Express |
| Database | MongoDB (Mongoose) |
| Backend auth | Firebase Admin SDK (verifies ID tokens) |
| Recipe data | Spoonacular API |
| Hosting | Render |
| Testing | JUnit (Android), Jest + Supertest (backend) |
| CI | GitHub Actions |

---

## Project structure

```
PROG7314_KitchenQuest/
├── KitchenQuest/          Android app (Kotlin, Jetpack Compose)
│   └── app/src/main/java/com/example/kitchenquest/
│       ├── data/          Repositories, API interfaces and DTOs, one folder per feature
│       ├── feature/       Screens and ViewModels, one folder per feature
│       ├── navigation/    App-wide navigation graph
│       └── ui/            Shared theme and reusable components
├── backend/               REST API (Node.js + Express + MongoDB)
│   └── src/
│       ├── routes/        Route definitions
│       ├── controllers/   Request handlers
│       ├── models/        MongoDB/Mongoose schemas
│       ├── middleware/    Auth, validation, error handling
│       └── config/        Environment, database and Firebase setup
└── .github/workflows/     CI: builds the app and runs its unit tests on every push
```

---

## Getting started

### Prerequisites
- **Android Studio** (recent version) with an Android SDK installed
- A phone or emulator running **Android 8.0 (API 26)** or newer
- For backend development only: **Node.js 22+**

### 1. Clone the repository
```bash
git clone https://github.com/ST10321249/PROG7314_KitchenQuest.git
cd PROG7314_KitchenQuest
```

### 2. Run the Android app
The app is already configured to use the **hosted backend** by default, so no local setup is needed for this step.

1. Open the `KitchenQuest` folder in Android Studio.
2. Let Gradle sync.
3. Run the app on an emulator or a phone connected over USB.

The first API request after a period of inactivity can take up to about a minute, since the free hosting tier sleeps when idle — this is expected, not a bug.

### 3. (Optional) Run the backend locally
Only needed if you're changing backend code, or want the app to talk to a database on your own machine instead of the hosted one.

```bash
cd backend
npm install
cp .env.example .env
```

Fill in `.env` with your own values:

| Variable | Description |
|---|---|
| `MONGODB_URI` | A MongoDB connection string (e.g. from MongoDB Atlas), including a database name |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Path to a Firebase service account key JSON file (from Firebase Console → Project Settings → Service Accounts) |
| `SPOONACULAR_API_KEY` | A free API key from [spoonacular.com/food-api](https://spoonacular.com/food-api) |

None of these values are committed to the repo — `.env` and the service account key are both gitignored.

```bash
npm start
```

You should see `MongoDB connected` and `KitchenQuest API listening on port 5000`.

To point the Android app at this local server instead of the hosted one, add this line to `KitchenQuest/local.properties` (also gitignored):
```
api.baseUrl=http://localhost:5000
```
On a physical phone connected by USB, also run `adb reverse tcp:5000 tcp:5000` so the phone can reach your PC.

See [`backend/README.md`](backend/README.md) for the full API reference and deployment details.

---

## Running tests

**Android:**
```bash
cd KitchenQuest
./gradlew testDebugUnitTest
```

**Backend:**
```bash
cd backend
npm test
```

Both run automatically on GitHub Actions on every push.

---

## What's planned next

- Finish the Home and Profile screens
- RoomDB for offline pantry, shopping list and favourites, with sync when connectivity returns
- Biometric unlock after first sign-in
- Push notifications for timers, expiring ingredients and shopping reminders
- Afrikaans language support

---

## Architecture

```
Android (Kotlin, Jetpack Compose)
        │  MVVM + Repository
        ▼
   Retrofit + OkHttp  ──(Firebase ID token)──┐
        │                                     │
        ▼                                     ▼
  REST API (Node.js + Express)  ───────►  Firebase (Auth)
        │
        ▼
  MongoDB (via Mongoose)
```

Every protected API request carries the signed-in user's Firebase ID token. The backend verifies that token with the Firebase Admin SDK before touching the database, so a user's data can only ever be read or changed by that same user.

---

## Team

Group 13:
- Raheel Singh — ST10321249
- Akeev Sivai — ST10312856
- Avikar Maharaj — ST10325729
- Mohamed Shaheer Joosab — ST10108240
