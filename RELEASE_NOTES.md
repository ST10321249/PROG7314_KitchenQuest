# KitchenQuest release notes

## Version 2.0.0: Final POE (`Final-POE`)

Everything below was added or changed since the Part 2 prototype (version 1.0).

### New features

**Security and accounts**
- Biometric unlock with fingerprint or face, turned on per account in Settings.
- Returning users are asked for biometrics before the app opens. Cancelling never skips the check: it falls back to the normal login.
- The chosen language is saved to the user profile.

**Languages**
- Full **Afrikaans** translation alongside English: every screen, button, error message, empty state and notification.
- The language can be switched in Settings and is kept after the app restarts.
- Recipe text from Spoonacular can be translated on the phone with Google ML Kit (language models are downloaded from Settings).

**Offline mode with automatic sync**
- My Kitchen and the Shopping List are stored in a Room database, so they open instantly and work without internet.
- Adding, editing, deleting and buying items offline is queued on the phone and sent automatically by WorkManager once the phone is back online.
- Client ids and idempotent server writes make sure a retried change is never saved twice.
- Items show whether they are waiting to sync or failed, with the reason and options to fix them.
- Saved recipes, and any recipe opened before, can be read and cooked without internet. *(innovative feature)*

**Notifications**
- Real-time **push notifications** through Firebase Cloud Messaging. The phone registers for push after login and stops receiving them after logout.
- A **Send test notification** button on the Notifications screen.
- Daily **expiry reminders** for pantry items that are about to expire, with a per-account toggle.
- **Cooking timer alarms** that ring when a timer finishes, even if the app is closed or the phone is locked. *(innovative feature)*

**Cooking**
- The cooking session (recipe, current step and timers) is saved, so it continues after the app is closed or restarted. *(innovative feature)*
- Timers keep the correct time while the app is closed, and finish properly if they ran out in the meantime.
- **Dish photos:** after cooking, pick a photo of the meal. It is resized on the phone, stored in MongoDB GridFS, and shown in Cooking History. *(innovative feature)*

**Production assets**
- New custom adaptive app icon (steaming pot in KitchenQuest orange and cream).
- Signed release APK (version 2.0.0, version code 2).
- Promotional screenshots for the Play Store listing.

### Backend (API) changes
- Room-compatible sync contract with client ids and idempotent creates for pantry and shopping items.
- `POST /api/devices` and `DELETE /api/devices/:token` to register phones for push, plus `POST /api/notifications/test`.
- `POST /api/files`, `GET /api/files/:id` and `DELETE /api/files/:id` for photos in GridFS, with owner checks, type detection and size limits.
- `PATCH /api/history/:id` to attach a photo to a cooking history entry.

### Quality
- Many new unit tests: Room migrations, the offline queue, sync retries, account isolation, restart persistence, the offline recipe cache, cooking-session restore, timer alarms, push registration, notifications and photo upload.
- GitHub Actions CI restored. It runs the Android unit tests, builds the app and runs the backend tests on every push.
- Android `Log` statements added to the offline recipe, cooking session, timer alarm, push and photo code to follow the app's lifecycle and state changes.

### Known limitations
- Recipe search and What Can I Make? need internet, because they ask Spoonacular for fresh results.
- The hosted API is on Render's free plan, so the first request after a quiet period can take up to a minute.
- On Android 13 and newer, exact timer alarms need **Alarms & reminders** to be allowed. Without it, the alarm can arrive a little late.