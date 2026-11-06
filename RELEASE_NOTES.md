# KitchenQuest – Development and release notes

This is the project's development history from **Part 1 (research and design)** through **Part 2 (working prototype)** to the **Final POE (version 2.0.0)**. It is based on the supplied Part 1 reports, the implementation in the current project, and the Git history. Part 1 was a design phase, **not a published application release**.

## Part 1 – Research and design (August–September 2026)

KitchenQuest began with an ingredient-first cooking concept: help people use the food they already have, understand what's missing from a recipe and reduce waste from ingredients reaching expiry.

### Research that informed the app

- **SuperCook** influenced the pantry-first recipe search, ingredient matching and connected shopping list.
- **Kitchen Stories** informed full recipe details, adjustable servings and a simpler step-by-step cooking view with timers.
- **KitchenPal** informed ingredient quantities, categories, expiry tracking, shopping integration and reminders.
- The research identified opportunities to add a more personal cooking record: saved recipes, ratings, notes and cooking history.

### Planning and design deliverables

- Produced the KitchenQuest concept, early logo, Figma screen designs and navigation wireflow.
- Defined 12 functional requirements (FR-01–FR-12), nonfunctional requirements and three custom features (CR-01–CR-03).
- Designed the Android/Kotlin/Jetpack Compose application around ViewModels, repositories and a hosted REST API.
- Planned Firebase sign-in, MongoDB data, recipe API integration, Room offline storage, notifications and two-language support.
- Drafted the REST API endpoints, entity models, UML/component diagrams and project timeline.

The research also discussed **voice input and barcode scanning** as possible future ingredient-entry approaches. They should not be counted as implemented features in this release.

## Part 2 – First working prototype (September–October 2026)

The initial implementation established the actual Android app, backend, account system and core cooking journey. The Git history includes the original implementation in September and UI/test refinements in October.

### Accounts and navigation

- Created the Android project and shared five-tab navigation (Home, Recipes, My Kitchen, Cook and Profile).
- Added registration, email/password login, password recovery and Google SSO with Firebase Authentication.
- Added session restoration, onboarding state and sign-out; introduced validation and useful error/loading states.
- Implemented dietary and avoided-ingredient onboarding choices and a profile Settings screen.
- Built a consistent KitchenQuest interface using reusable Compose components and the orange/cream design.

### Hosted backend and database

- Created the Node.js/Express REST API and hosted it on Render.
- Connected MongoDB Atlas using Mongoose; added user models, Firebase Admin token verification, request validation and error handling.
- Added profile synchronization, protected CRUD routes and Retrofit/OkHttp access from Android.
- Connected Spoonacular as the source for live recipe searches and details, keeping its API key on the backend.
- Added backend route tests and Android ViewModel/repository tests.

### Pantry, shopping and recipes

- Added My Kitchen with ingredient quantities, units, categories, expiry dates, editing, deletion, search and ingredient-name suggestions.
- Added the Shopping List and routes for manual items and purchased-state changes.
- Built recipe search, recipe details, ingredient/serving scaling and favourites.
- Added **What Can I Make?**, using pantry ingredients to calculate a match ratio and list missing ingredients.
- Added warnings and Home summaries for ingredients approaching expiry.

### Cooking, personal records and prototype polish

- Implemented guided cooking steps, timers and the initial Cooking History and recipe-note flows.
- Added recipe completion with personal feedback and links from profile/history back into recipes.
- Improved navigation, styling, form validation and tests after the first prototype handovers.
- Set up GitHub Actions to test Android and the backend; later temporarily disabled it during Part 3 development to limit campus CI usage.

The prototype made the main app usable, but **biometrics, robust offline syncing, live FCM delivery, full localized resources and blob photo storage** were built in the later POE phase.

## Version 2.0.0 – Final POE implementation (October–November 2026)

The Android project's configured version is **2.0.0 / version code 2**. The features below are present in source; final store-publication evidence is tracked separately rather than assumed.

### Authentication and account preferences – Raheel

- Added AndroidX Biometric support with device/enrolment checks and an opt-in setting per Firebase account.
- Added quick unlock for returning authenticated sessions, retry and normal-sign-in fallback.
- Preserved verified biometric state during language changes and protected the app when returning from the background.
- Added English and Afrikaans string resources across the main app and persisted the language choice.
- Corrected navigation so changing language stays on Settings and the normal launch splash is shown before cold-start biometric unlock.
- Added reusable Google ML Kit translation/model-download infrastructure; the service is present, but not every recipe or pantry field is automatically translated.
- Added backend language-preference synchronization and cleaned up temporary development-stage wording.

### Offline data and backend services – Akeev

- Added Room entities/DAOs for pantry items, shopping items, queued operations and later cached recipe/translation records.
- Added client-generated identifiers and idempotent server-side handling to reduce duplicate writes after retries.
- Added account-scoped local-first repository infrastructure and WorkManager jobs to replay queued changes when connected.
- Implemented Firebase Cloud Messaging device-token routes and a protected push-test endpoint.
- Added MongoDB GridFS services and protected image upload/download routes with ownership, size and file-type checks.
- Expanded tests for Room, synchronization, account isolation, FCM, photo validation and API behavior.

### Offline pantry and shopping, plus expiry reminders – Avikar

- Connected the pantry screen to Room so existing items remain visible without a network connection.
- Allowed offline pantry changes to persist and synchronize when the device reconnects.
- Connected the Shopping List to Room and queued offline changes, including safe purchased-item transfer into the pantry.
- Added on-screen feedback for pending/failed/synchronized operations.
- Added scheduled local expiry notifications, notification channels and a per-account Settings preference.
- Added restart, retry, account separation, conflict and offline-flow tests.

### Recipes, cooking, notifications and photos – Mohamed

- Cached opened recipes and the saved-recipes list for offline viewing and cooking; fresh recipe search still needs internet.
- Improved cooking-session persistence and restored current steps/timer deadlines after interruptions.
- Added Android timer alarms/local notifications for timers finishing while the app is in the background.
- Wired Android Firebase Cloud Messaging token registration and notification delivery to the backend endpoints.
- Added a dish-photo picker, client-side resizing, GridFS upload and display in Cooking History.
- Updated the adaptive launcher icon, production-version configuration and the restored Android/backend CI workflow.

### Final documentation and quality

- Updated the GitHub README to cover architecture, core flows, POE features, tests, deployment and required screenshot locations.
- Updated this historical release log to cover the initial research and design, prototype work and final POE implementation.
- Reinstated GitHub Actions to run Android tests/builds and backend Jest tests on pushes and pull requests.
- Included updated test coverage for offline data, migration, authentication, notifications, timers, file storage and cooking history.
- Updated Cooking Mode to keep the display awake while cooking, localized the timer-start control and safeguarded restored step indices.
- Localized the native biometric enrollment prompt to match the selected app language.

## Feature coverage against the Part 1 design

| Requirement | Status in the supplied source | Notes |
| --- | --- | --- |
| FR-01 – Account registration and Google SSO | Implemented | Firebase auth with validation and reset flow |
| FR-02 – Biometric quick unlock | Implemented | Uses an existing signed-in Firebase session; reinstall/backup edge cases require phone testing |
| FR-03 – Preferences | Implemented, with qualification | Settings and profile sync work; applying all dietary preferences as default recipe filters is not consistently evident |
| FR-04 – Search, filters and scaling | Partially implemented | Search, recipe detail and scaling exist; full difficulty, cuisine and meal-type filters are not offered |
| FR-05 – Pantry CRUD and validation | Implemented | Also available offline |
| FR-06 – Expiry prioritization | Mostly implemented | Highlighting/alerts exist; specifically ranking recipes by expiring ingredients is not fully established |
| FR-07 / CR-01 – Ingredient match ratio | Implemented | Match ratio and missing ingredients in recommendations |
| FR-08 – Shopping and transfers | Mostly implemented | Manual entry, missing items and transfers exist; a dedicated few-missing-items discovery view is not evident |
| FR-09 / CR-02 – Guided cooking and timers | Implemented with final polish | Saved progress, shared timers and screen-awake behavior in the final overlay |
| FR-10 – Notification channels and alerts | Implemented in code | FCM, expiry and timer alerts require permission/device tests |
| FR-11 – Cooking history | Implemented | Rating, difficulty, private note and optional photos |
| FR-12 – Room offline + synchronization | Implemented in code | Pantry/shopping queue and cached recipe reading; verify on a physical device |
| CR-03 – Expiry-aware details | Mostly implemented | Ingredient detail/expiry warnings exist; recipe recommendation prioritization may need refinement |

## Known limits and verification tasks

- **External recipe data:** Spoonacular search and fresh recommendations require an internet connection. User/API text may remain in its original language; ML Kit infrastructure does not automatically translate every displayed field.
- **Optional Part 1 ideas:** Voice/barcode ingredient capture and all advanced filters were not added to the final app.
- **Server latency:** The hosted Render service can have a cold-start delay; upstream Spoonacular errors remain possible.
- **Platform permissions:** Push/local notifications and exact alarms depend on Android permissions and device settings.
- **Publication proof still needed:** Capture real promotional screenshots, verify a signed release APK, attach it to the final GitHub release, insert the final demo link in the README and create/push the `Final-POE` tag after final testing.
- **Physical-device checks:** Demonstrate real FCM delivery, biometric login, offline edits/sync after app restart, timer completion, GridFS photo retrieval and account isolation.

See the [README](README.md) for how to build, test and demonstrate the final application.
