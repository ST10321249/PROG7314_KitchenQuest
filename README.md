# KitchenQuest

KitchenQuest is an Android app built around one idea: match recipes to what's actually in your kitchen instead of making you browse recipes and then find out what you're missing. Add what you've got to your pantry, and the app shows what you can cook right now, flags what's missing on anything close, and pushes ingredients that are close to expiring to the front so they get used before they go off.

Built for PROG7314 (Part 2) by Group 13.

## What it does

Sign up or sign in with email/password or Google. First time in, onboarding asks about dietary preferences and anything to avoid, and that carries through to the rest of the app.

From there:
- My Kitchen is the pantry - add, edit, delete ingredients, with search and autocomplete once there's a few in there
- Shopping list works the same way, with items markable as bought
- Recipes come from the Spoonacular API, with full detail pages and ingredient scaling by servings
- What Can I Make? checks the pantry against recipes and shows what's missing
- Recipes can be saved to Favourites
- Cooking Mode walks through a recipe step by step, with timers running alongside - recipe timers and a standalone kitchen timer both land in one list
- Finished cooks get logged to Cooking History with a rating and notes
- Settings holds display name, dietary preferences and avoided ingredients, saved server-side so it survives a reinstall

Home and Profile are still placeholder screens for now - what they'd summarise already works from its own tab. A few other things are planned but not built yet, listed further down.

## How it fits together

```
Android app (Kotlin, Jetpack Compose) -> Retrofit -> REST API (Node/Express) -> MongoDB
```

Screens follow MVVM with a repository sitting between the ViewModel and the network layer. On the backend, every route except `/health` checks the caller's Firebase ID token before doing anything, and looks up data by the UID inside that token rather than trusting anything the client sends - so nobody can read or change someone else's profile just by changing an id in a request.

## Stack

**Android:** Kotlin, Jetpack Compose, MVVM + Repository, Retrofit + OkHttp, Firebase Auth
**Backend:** Node.js, Express, MongoDB via Mongoose, Firebase Admin SDK for token checks, Spoonacular for recipe data
**Hosting:** Render (API), MongoDB Atlas (database)
**Testing:** JUnit on Android, Jest + Supertest on the backend - both run through GitHub Actions on every push

## Running it

Needs Android Studio and a phone or emulator on Android 8.0 (API 26) or newer. Node.js 22+ only matters if the backend is being touched.

Clone it, open the `KitchenQuest` folder in Android Studio, let Gradle sync, hit run. The app points at the hosted API by default, so that's genuinely everything needed to get it going. The first request after the server's sat idle a while can take close to a minute - that's Render's free tier waking up, not a bug.

To run the backend locally instead (for backend changes, or a personal database):

```
cd backend -> npm install -> cp .env.example .env
```

then fill in:
- `MONGODB_URI` - a MongoDB connection string, database name included
- `FIREBASE_SERVICE_ACCOUNT_PATH` - path to a Firebase service account key (Firebase Console -> Project Settings -> Service Accounts)
- `SPOONACULAR_API_KEY` - a free key from spoonacular.com/food-api

None of that goes into the repo - `.env` and the service account key are both gitignored. Then `npm start`, and `MongoDB connected` followed by the server listening on port 5000 should show up.

To point the app at that instead of the hosted one, add `api.baseUrl=http://localhost:5000` to `KitchenQuest/local.properties` (also gitignored). On a real phone over USB, also run `adb reverse tcp:5000 tcp:5000` so it can actually reach the PC.

Full API reference lives in `backend/README.md`.

## Tests

```
cd KitchenQuest && ./gradlew testDebugUnitTest
cd backend && npm test
```

Both run in CI on every push.

## Still to come

- Offline support via RoomDB, syncing once back online
- Biometric unlock
- Push notifications for timers, expiry and shopping reminders
- Afrikaans

## Team

Group 13:
- Raheel Singh - ST10321249
- Akeev Sivai - ST10312856
- Avikar Maharaj - ST10325729
- Mohamed Shaheer Joosab - ST10108240