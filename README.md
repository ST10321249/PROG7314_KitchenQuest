# KitchenQuest

KitchenQuest is an Android app built around one idea: match recipes to what's actually in your kitchen instead of making you browse recipes and then find out what you're missing. Add what you've got to your pantry, and the app shows what you can cook right now, flags what's missing on anything close, and pushes ingredients that are close to expiring to the front so they get used before they go off.

Built for PROG7314 (Final POE) by Group 13.

- Demo video: *link to be added*
- Signed APK: attached to the `Final-POE` release on GitHub
- Everything that changed since Part 2: [RELEASE_NOTES.md](RELEASE_NOTES.md)
- Full API reference: [backend/README.md](backend/README.md)

## What it does

Sign up or sign in with email/password or Google. First time in, onboarding asks about dietary preferences and anything to avoid, and that carries through to the rest of the app. Coming back later, the app can be unlocked with a fingerprint or face instead of typing the password again.

From there:
- Home greets you by name and gives a quick-glance summary: how many ingredients are in your kitchen, how many are expiring soon, how many are on your shopping list, plus one-tap shortcuts into the Kitchen Timer, What Can I Make?, Shopping List and recipe search
- My Kitchen is the pantry - add, edit, delete ingredients, with search and autocomplete once there's a few in there. It works offline, and anything changed offline syncs by itself once the phone is back online
- Shopping list works the same way, with items markable as bought, and also works offline
- Recipes come from the Spoonacular API, with full detail pages and ingredient scaling by servings
- What Can I Make? checks the pantry against recipes and shows what's missing
- Recipes can be saved to Favourites, and saved recipes (plus anything opened before) can still be read and cooked with no internet
- Cooking Mode walks through a recipe step by step, with timers running alongside - recipe timers and a standalone kitchen timer both land in one list. The cook is saved on the phone, so closing the app doesn't lose your step or your timers, and a timer still rings when it finishes even if the app is closed
- Finished cooks get logged to Cooking History with a rating, notes and an optional photo of the dish
- Profile shows your cooking stats at a glance - meals cooked, cuisines tried, a difficulty breakdown, and how many ingredients you've used up before they expired - with quick links into Saved Recipes and Cooking History
- Settings holds display name, dietary preferences, avoided ingredients, language (English or Afrikaans), biometric unlock and expiry reminders, with the profile details saved server-side so they survive a reinstall
- Notifications - real push notifications from our server through Firebase Cloud Messaging, a daily reminder for ingredients about to expire, and timer alarms

Every screen from the original Figma is built and wired to a real ViewModel and, where it needs to be, the API. Everything Part 2 listed under "Still to come" is now in: biometric unlock, offline mode with sync, push notifications, Afrikaans and photos through blob storage. Recipe imagery on the recipe screens is still icon-based - the photos the app stores are the user's own dish photos in Cooking History.

## What's new for the POE

**Biometric unlock.** A returning user can unlock the app with a fingerprint or face through AndroidX BiometricPrompt. It only protects a session that's already signed in with Firebase - it never replaces the account login. It's switched on per account from Settings, and cancelling the prompt sends you to the normal login screen rather than letting you in. No biometric data ever leaves the phone.

**Offline mode with sync.** My Kitchen and the Shopping List live in a Room database on the phone, so they open instantly and keep working with no connection. Every change made offline goes into a queue on the phone that survives the app being closed, and WorkManager sends the queue to the API by itself once the phone is back online. Each item gets its id on the phone before it's sent, and the server uses that id, so a change that gets retried is never saved twice. Anything still waiting to sync, or that failed, is marked on screen with the reason. Saved recipes and any recipe that's been opened before are kept on the phone too, so they can be read and cooked offline. Recipe search still needs internet, because it asks Spoonacular for fresh results.

**Push notifications.** Firebase Cloud Messaging. After sign-in, the phone sends its messaging token to the API, and it's removed again on sign-out. The API uses the Firebase Admin SDK to push messages only to the signed-in user's own phones. The Notifications screen has a Send test notification button that asks the server to push a real message to the phone.

**Local notifications.** A daily reminder for pantry items about to expire, which can be switched on or off per account in Settings. Cooking timers are also scheduled with Android's AlarmManager, so a finished timer rings even if the app has been swiped away or the phone is locked.

**Afrikaans.** The whole app is available in English and Afrikaans - every screen, button, error, empty state and notification. The language is switched in Settings, sticks after a restart, and is saved to the profile. Recipe text from Spoonacular can also be translated on the phone with Google ML Kit.

**Photos (blob storage).** After a cook, you can pick a photo of the dish. It's resized on the phone, uploaded through the API into MongoDB GridFS, and shown in Cooking History. Only the person who uploaded a photo can download it.

**Ready for the Play Store.** Custom app icon (a steaming pot in KitchenQuest orange), a signed release APK, and promotional screenshots.

## Screens

Splash -> Onboarding (first run) or straight to Login (returning user) -> Login / Register / Forgot Password -> Biometric unlock (if it's turned on) -> Home.

Once signed in, five tabs sit across the bottom: Home, Recipes, My Kitchen, Cook, Profile.

- **Home** - a personalised landing screen: kitchen stats (in kitchen / expiring soon / to buy), an "expiring soon" strip pulled live from the pantry, a short list of recommended recipes, and one-tap shortcuts into the Kitchen Timer, What Can I Make?, Shopping List and recipe search
- **Recipes** - search Spoonacular, open a recipe's full detail with scaling, save it to Favourites
- **What Can I Make?** - reached from Recipes, matches the pantry against recipes and ranks them by match ratio
- **Recipe Details** - full recipe, ingredient amounts scale live off a servings stepper. Shows an "offline copy" note when there's no internet and the saved copy is being shown
- **Saved Recipes** - the Favourites list, also available offline
- **My Kitchen** - the pantry: add/edit/delete ingredients, search, ingredients expiring within three days are grouped separately from everything else, and each item shows whether it's synced, waiting to sync or failed
- **Ingredient Detail / Ingredient Editor** - one ingredient's record, and the add/edit form
- **Shopping List** - add items, tick them off, ticking one moves it straight into the pantry
- **Cook** - a small hub: jump back into an active cook, open the kitchen timer, see active timers, open cooking history
- **Cooking Mode** - one step at a time, Next/Previous
- **Active Timers / Kitchen Timer** - recipe timers and a standalone timer, both shown in one list
- **Recipe Complete** - rate it, tag a difficulty, leave a private note, add a photo of the dish
- **Cooking History** - past cooks with their rating, notes and photo
- **Notifications** - expiring items and shopping reminders, plus the Send test notification button for push
- **Profile** - cooking stats (meals cooked, cuisines, difficulty spread, ingredients saved from waste), with links into Saved Recipes and Cooking History, and a settings shortcut
- **Settings** - name, dietary preferences, avoided ingredients, language, biometric unlock, expiry reminders and translation models

Every screen on that list is real and working, tied to a ViewModel and, where it needs to be, the API.

## How it fits together

```
Android app (Kotlin, Jetpack Compose) -> Room on the phone + Retrofit -> REST API (Node/Express on Render) -> MongoDB Atlas + GridFS
```

Screens follow MVVM with a repository sitting between the ViewModel and the data. The repository decides whether data comes from Room on the phone or from the API, which is what lets the pantry, shopping list and saved recipes keep working offline. WorkManager sends queued offline changes in the background, Firebase Authentication handles sign-in, and Firebase Cloud Messaging delivers push notifications from the API to the phone.

On the backend, every route except `/health` checks the caller's Firebase ID token before doing anything, and looks up data by the UID inside that token rather than trusting anything the client sends - so nobody can read or change someone else's data just by changing an id in a request. The phone keeps accounts apart as well: every row in Room, every saved recipe copy, every saved cook and every setting is stored against the signed-in user's UID, so a second account on the same phone never sees the first one's data.

Cooking timers store the time they'll finish instead of counting seconds down, which is why the time is still right after the app has been closed.

## Stack

**Android:** Kotlin, Jetpack Compose, MVVM + Repository, Room, WorkManager, Retrofit + OkHttp, kotlinx.serialization, Firebase Auth, Firebase Cloud Messaging, AndroidX Biometric, Google ML Kit Translate
**Backend:** Node.js, Express, MongoDB via Mongoose, GridFS for photos, Firebase Admin SDK for token checks and push, Zod for request validation, Spoonacular for recipe data
**Hosting:** Render (API), MongoDB Atlas (database)
**Testing:** JUnit and Robolectric on Android, Jest + Supertest on the backend - both run through GitHub Actions on every push

## Running it

Needs Android Studio and a phone on Android 8.0 (API 26) or newer. Node.js 22+ only matters if the backend is being touched.

**Just using the app:** download `app-release.apk` from the `Final-POE` release on GitHub onto the phone, open it, allow installing from that source when the phone asks, and tap Install.

**From the source:** clone it, open the `KitchenQuest` folder in Android Studio, let Gradle sync, hit run. The app points at the hosted API by default, so that's genuinely everything needed to get it going. The first request after the server's sat idle a while can take close to a minute - that's Render's free tier waking up, not a bug.

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

**Building the signed APK:** create a signing key once with Android Studio's `keytool` and keep it outside the project. Then add a `KitchenQuest/keystore.properties` file with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`, and run `./gradlew assembleRelease`. The signed APK ends up in `KitchenQuest/app/build/outputs/apk/release/app-release.apk`. The key and `keystore.properties` are both gitignored and never go into the repo.

## Tests

```
cd KitchenQuest && ./gradlew testDebugUnitTest
cd backend && npm test
```

The Android tests cover the ViewModels, validation, the Room database and its migrations, the offline queue and sync, keeping accounts apart, surviving a restart, the offline recipe copies, restoring a cook, timer alarms, push registration, notifications and dish photo uploads. The backend tests cover sign-in checks, user isolation, every route, repeated sync requests, device tokens and photo validation.

## GitHub and CI

Each of us worked on our own branch - `authentication`, `api-data-layer`, `pantry-shopping` and `recipes-cooking` - updated it from `main` before starting, and merged it back into `main` when done, one person after the other. Work went in as small, tested commits with clear messages.

GitHub Actions (`.github/workflows/android-ci.yml`) runs on every push and pull request. One job runs the Android unit tests and builds the app on Java 21, and the other installs the backend and runs the Jest tests on Node 22 - so the project is proven to build and pass on a clean machine, not just on our laptops. The final tested commit on `main` is tagged `Final-POE`, and the signed APK is attached to that release.

## Part 1 requirements -> what we built

The Part 1 planning document set out twelve functional requirements. Here's where each one landed by the final POE:

- **FR-01 (accounts)** - done. Email/password and Google sign-in, email format checked, password needs 8+ characters with a capital, a number and a symbol, confirmation has to match, and Firebase itself rejects a duplicate email.
- **FR-02 (biometric unlock)** - done. Fingerprint or face unlock for a returning, already signed-in user, switched on per account in Settings, with the normal login as the fallback.
- **FR-03 (preferences on onboarding)** - done. Dietary preference and avoided ingredients are captured on first sign-in, editable later from Settings, and saved to the profile.
- **FR-04 (recipe search + scaling)** - done. Search hits Spoonacular, and the recipe detail screen has a servings stepper that scales every ingredient amount live. Quick filters (Vegetarian, Under 30 min) sit on the Recipes screen, but full filtering by difficulty, meal type and cuisine isn't in the UI.
- **FR-05 (pantry CRUD + validation)** - done. A blank name, a quantity that isn't a positive number, or an expiry date in the past are all rejected before the request goes anywhere - and it all works offline now too.
- **FR-06 (expiry flagging)** - done. Anything expiring within three days gets pulled into its own group at the top of My Kitchen, surfaced again on Home, and there's now a daily reminder notification for it.
- **FR-07 (pantry-to-recipe matching)** - done. The backend ranks recipes by the proportion of ingredients the pantry already has and lists what's missing, the same shape the planning doc specified.
- **FR-08 (shopping list)** - mostly done. Add items manually, tick them off, ticking one moves it straight into the pantry, and it all works offline. Surfacing "recipes you're only missing a couple of items for" isn't built.
- **FR-09 (guided cooking)** - mostly done. Step-by-step with Next/Previous, timers that stack alongside it, the cook survives the app closing, and finished timers ring as a notification. Keeping the screen awake during a cook isn't wired up.
- **FR-10 (notifications)** - done. Real push notifications through Firebase Cloud Messaging, daily expiry reminders, and timer alarms.
- **FR-11 (cooking history)** - done. A rating, a difficulty tag, a private note and an optional dish photo get captured right after a cook and show up in a filterable history.
- **FR-12 (offline via RoomDB)** - done. The pantry and shopping list are stored in Room and sync automatically when the connection comes back, and saved recipes can be read offline.

## Known limits

- Recipe search and What Can I Make? need internet, since they ask Spoonacular for fresh results
- The API is on Render's free tier, so the first request after a quiet spell can take close to a minute
- On Android 13 and newer, exact timer alarms need "Alarms & reminders" allowed for the app - without it the alarm can land a little late
- Recipe imagery is still icon-based rather than photos from Spoonacular

## Team

Group 13:
- Raheel Singh - ST10321249 - ST10321249@vcconnect.edu.za
- Kaeden Samsunder - ST10313111 - ST10313111@vcconnect.edu.za
- Akeev Sivai - ST10312856 - ST10312856@vcconnect.edu.za
- Avikar Maharaj - ST10325729 - ST10325729@vcconnect.edu.za
- Mohamed Shaheer Joosab - ST10108240 - ST10108240@vcconnect.edu.za

## References

- Android Developers. 2026. *Show a biometric authentication dialog.* Available at: https://developer.android.com/identity/sign-in/biometric-auth
- Android Developers. 2026. *Save data in a local database using Room.* Available at: https://developer.android.com/training/data-storage/room
- Android Developers. 2026. *Schedule tasks with WorkManager.* Available at: https://developer.android.com/develop/background-work/background-tasks/persistent
- Android Developers. 2026. *Schedule alarms.* Available at: https://developer.android.com/develop/background-work/services/alarms/schedule
- Firebase. 2026. *Firebase Cloud Messaging.* Available at: https://firebase.google.com/docs/cloud-messaging
- MongoDB. 2026. *GridFS.* Available at: https://www.mongodb.com/docs/manual/core/gridfs/
- Spoonacular. 2026. *Spoonacular Food API.* Available at: https://spoonacular.com/food-api