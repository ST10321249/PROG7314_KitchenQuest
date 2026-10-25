# KitchenQuest data layer handoff

This is for Avikar (pantry and shopping, V1 to V7) and Mohamed (recipes and cooking, M1 to M7). It explains
what the data layer already does, how to use it from the screens, and what is still left for you.

Read `SYNC_CONTRACT.md` first for the rules behind how changes are saved and sent. This document is about
using them.

## 1. What is already built

| Piece | Where | What it does |
| --- | --- | --- |
| Room database | `data/local/` | Stores the pantry, the shopping list, the queue of unsent changes and translations on the phone. |
| Offline repositories | `data/pantry/OfflinePantryRepository.kt`, `data/shopping/OfflineShoppingRepository.kt` | The pantry and shopping repositories the screens already use. They read from the phone and save changes to it straight away. |
| Change queue | `data/sync/OperationQueue.kt` | Keeps the list of unsent changes small and in order. |
| Sync | `data/sync/SyncProcessor.kt`, `SyncWorker.kt`, `WorkManagerSyncScheduler.kt` | Sends the queue to the server when the phone is online, and retries when something fails. |
| Wiring | `data/AppRepositories.kt`, `KitchenQuestApplication.kt` | Gives the ViewModels the offline repositories by default. |
| Translation cache | `data/local/TranslationCache.kt` | Remembers translations on the phone so they can be shown offline. |
| Push notifications (server) | `backend/src/routes/deviceRoutes.js`, `notificationRoutes.js` | Stores phone tokens and sends push notifications. |
| Photos (server) | `backend/src/routes/fileRoutes.js` | Stores and serves cooking photos in MongoDB GridFS. |

The ViewModels that use the pantry or shopping list only had their default repository changed, to
`AppRepositories.pantry` and `AppRepositories.shopping`. Their logic is untouched, and tests that pass in
their own fake repositories keep working.

## 2. For Avikar (pantry and shopping)

### Showing whether an item has reached the server (V6)

Every `PantryItemDto` and `ShoppingItemDto` has a `syncState`. It exists only on the phone and is never sent
to the server.

| `syncState` | Meaning | Suggested display |
| --- | --- | --- |
| `SYNCED` | The server has the latest version. | Nothing, or a small tick. |
| `PENDING` | A change is saved on the phone and waiting to be sent. | "Waiting to sync" |
| `FAILED` | The server refused the change, or it kept failing. | An error mark with the reason. |

To show a count of changes still waiting, or the reason a change failed:

```kotlin
val dao = KitchenQuestDatabase.getInstance(context).pendingOperationDao()
val userId = AppRepositories.currentUserId()

dao.observeWaitingCount(userId)          // Flow<Int> of changes still to be sent
dao.getOutstanding(userId)               // the queue, including FAILED ones with lastError
```

A failed change is not retried automatically. The user fixes it by editing the item, which gives it a fresh
start, or by deleting it, which removes it. To ask for a sync right away, call
`AppRepositories.syncScheduler.requestSync()`.

### Reading the pantry reactively (V1)

The repositories return a list each time they are asked. If a screen should update by itself, use
`KitchenQuestDatabase.getInstance(context).pantryDao().observeAll(userId)` (and `shoppingDao()`), which is a
`Flow` of rows. Convert rows with `toDto()` from `data/local/Mappers.kt`.

### Moving a bought shopping item to the pantry (V4)

`ShoppingViewModel.moveToPantry` currently adds a pantry item and then deletes the shopping item as two
steps. Both work offline, but `ShoppingRepository.transferToPantry(...)` does it as one safe step and is the
better choice:

```kotlin
shoppingRepository.transferToPantry(
    id = item.id,
    ingredientName = item.ingredientName,
    quantity = item.quantity ?: 1.0,
    unit = item.unit.ifBlank { "unit" },
    category = "Uncategorised",
    expiryDate = null
)
```

### Expiry alerts (V5)

Read the pantry from Room (`pantryDao().getAll(userId)`), so alerts work without a connection. `expiryDate` is
a `yyyy-MM-dd` string. The notification channels and scheduling are yours. If a server-sent alert is ever
wanted, `sendToUser` in `backend/src/services/notificationService.js` is ready to use.

### Account switching (V6)

Every row and every queued change belongs to a Firebase uid, and the repositories only read and send the
signed-in user's rows. Signing out and in as someone else shows that person's data, and the first user's
unsent changes wait until they sign in again. Nothing needs clearing on sign-out.

### Test checklist for V7

1. Turn on airplane mode. Add, edit and delete pantry and shopping items. The list updates straight away.
2. Close the app completely and reopen it. The changes are still there and still marked as waiting.
3. Turn the network back on. The changes are sent without opening anything. Check MongoDB: each item must
   appear exactly once.
4. Add an item, turn the network off the moment you tap save, and turn it back on. The item must not be
   duplicated.
5. Sign in as a second account. It must not see the first account's items.
6. Edit the same item on two phones. See the conflict rules in the contract (section 6).

### Rules to keep

- Do not call `PantryApi` or `ShoppingApi` from a ViewModel. Go through the repositories so changes are
  queued.
- Item ids on screen are local keys. Never build a URL from one yourself.
- The server must be running the latest backend, or creates can duplicate and transfers fail.

## 3. For Mohamed (recipes and cooking)

### Translating recipe text (M1)

`TranslationCache` keeps translations on the phone so recipe text translated once can be shown offline.

```kotlin
val cache = TranslationCache(KitchenQuestDatabase.getInstance(context).translationCacheDao())

suspend fun displayText(text: String, source: String, target: String): String {
    cache.get(source, target, text)?.let { return it }
    val translated = translationService.translateOrOriginal(text, source, target)
    if (translated != text) cache.put(source, target, text, translated)
    return translated
}
```

Keep the original text in your own data and translate only for display. Never use a translation as an id or to
match ingredients. The cache holds up to 2,000 translations and removes the oldest.

Saved recipes are not stored in Room yet. For an offline read-only copy, follow the same pattern as pantry
(see section 6 for how to add a table).

### Timers (M2, M3)

Timers are not part of the data layer. If you want to store them in Room, add a table by following section 6.

### Push notifications (M4)

All endpoints need the signed-in user's Firebase token, which the existing network client already adds.

| Call | Use it | Notes |
| --- | --- | --- |
| `POST /api/devices` with `{ "token": "<FCM token>" }` | When the app starts after sign-in, and whenever Firebase gives a new token (`onNewToken`) | Safe to repeat. If the phone is used by another account, the token moves to that account. |
| `DELETE /api/devices/{token}` | When the user signs out | Always succeeds. Only removes the signed-in user's own token. |
| `POST /api/notifications/test` with `{}` or `{ "token": "..." }` | To check pushes work | Returns `{ "sent", "failed", "removed" }`. `404` means no device is registered. |

Registering again every time the app starts matters: tokens not refreshed for 60 days are deleted
automatically, and tokens Firebase reports as invalid are deleted when a push is sent.

Example Retrofit interface (not in the project yet):

```kotlin
interface DeviceApi {
    @POST("api/devices")
    suspend fun register(@Body request: RegisterDeviceRequest)

    @DELETE("api/devices/{token}")
    suspend fun unregister(@Path("token") token: String)
}
```

`@Path` encodes the characters in an FCM token for you.

Notifications sent by the server carry a `data` map. Suggested keys: `type` (what it is about, for example
`test` or `expiry`) and `route` (the screen to open when it is tapped). All values arrive as strings. The
server can name an Android notification channel when it sends, so create the channel on the phone first. Without
a channel id, Android uses the app's default channel.

The server sends with the same Firebase service account as sign-in. Check the Cloud Messaging API is enabled
for the project in the Firebase console.

### Photos (M5)

| Call | Use it | Notes |
| --- | --- | --- |
| `POST /api/files` | Upload a photo | The request body is the raw image. Set the content type to `image/jpeg`, `image/png` or `image/webp`. The response is `{ id, contentType, size, createdAt }`. |
| `GET /api/files/{id}` | Show a photo | Needs the same `Authorization` header, so load it with the app's authenticated OkHttp client. Supports `If-None-Match`. |
| `DELETE /api/files/{id}` | Remove a photo | Also clears it from any history entry. |
| `PATCH /api/history/{id}` with `{ "photoFileId": "<id>" }` | Attach a photo to a cooking history entry | Send `null` to remove it. A replaced or removed photo is deleted from storage. |
| `POST /api/history` with `photoFileId` | Create an entry that already has a photo | Optional. |

Limits: 3 MB per photo and 50 photos per user. The phone must shrink or compress the photo before uploading.
The server checks the real file contents, so a renamed file or a mismatched content type is rejected (`400`).
Other errors are `413` too big, `415` wrong type, `409` too many photos, and `404` for a photo that does not
exist or is not yours.

Flow: let the user pick a photo, compress it, upload it, then `PATCH` the history entry with the returned id.

Example Retrofit interface (not in the project yet):

```kotlin
interface FileApi {
    @POST("api/files")
    suspend fun upload(@Body photo: RequestBody): UploadedFileDto
}

val body = bytes.toRequestBody("image/jpeg".toMediaType())
```

One thing to watch: the app's JSON setup leaves out properties that equal their default value. To send an
explicit `null` (to remove a photo), declare the property with no default:

```kotlin
@Serializable
data class UpdateHistoryRequest(val photoFileId: String?)   // no "= null"
```

`CookingHistoryDto` does not have `photoFileId` yet. Add `val photoFileId: String? = null` to it when you
show photos.

### Release (M6, M7)

- The CI workflow is saved as `.github/workflows/android-ci.yml.disabled`. Renaming it back to
  `android-ci.yml` restores it. It already has a backend job (`npm ci` and `npm test`).
- The first Android test run downloads Robolectric's Android files, so CI needs internet access. That is
  normal.
- Do not commit `backend/.env`, `serviceAccountKey.json` or `local.properties`.

## 4. New server endpoints at a glance

| Endpoint | Added in | Purpose |
| --- | --- | --- |
| `POST /api/shopping-list/transfer` | Sync contracts | Move a bought item to the pantry safely. |
| `clientId` on pantry and shopping creates | Sync contracts | Makes a repeated create safe. |
| `POST /api/devices`, `DELETE /api/devices/{token}` | Push notifications | Register and remove a phone for push. |
| `POST /api/notifications/test` | Push notifications | Send a test push to your own devices. |
| `POST /api/files`, `GET /api/files/{id}`, `DELETE /api/files/{id}` | Photos | Photo storage. |
| `PATCH /api/history/{id}` | Photos | Attach or remove a history photo. |

MongoDB collections created: `devicetokens`, `photos.files` and `photos.chunks`. Pantry and shopping items
gain a `clientId` field.

## 5. Running the tests

```
cd KitchenQuest
./gradlew testDebugUnitTest assembleDebug      (Windows: .\gradlew.bat ...)

cd backend
npm ci
npm test
```

What they cover:

- **Room and sync (Android):** the database tables and queries, user separation, the queue's combining rules,
  the sync worker against a fake server (lost replies, rejected items, giving up, cancellation), two phones
  on one account, database migration, the translation cache, and that the app starts with the right
  repositories.
- **API (backend):** every route needs a sign-in (checked automatically for every route), each user only sees
  their own data, repeated creates and transfers, device registration and push cleanup, and photo upload
  checks and owner checks.

The server tests use fakes for Firebase and the database, so a real push or a real phone is still needed
for a final check.

## 6. Changing the database

1. Add or change the entity and the DAO.
2. Raise `version` in `KitchenQuestDatabase`.
3. Add a `Migration` and put it in `MIGRATIONS`. `MIGRATION_1_2` is an example.
4. Build once, so Room writes the new `schemas/.../N.json`, and commit that file.
5. Add a test to `MigrationTest`. The test `everyVersionStepHasAMigration` fails if a version has no
   migration, and the migration tests check the result against the saved schema.

Never use `fallbackToDestructiveMigration`. It would delete changes that have not been sent yet.

## 7. Known limits

- **Not run on a phone yet.** The logic, sync and database are tested on the JVM with a real SQLite
  database and a fake server. Airplane-mode behaviour, WorkManager timing and real push delivery still
  need checking on a device.
- **The server must be redeployed** for the new endpoints and `clientId` handling to exist on Render.
- **Only the pantry and shopping list work offline.** History, saved recipes and the profile still need a
  connection.
- **No "changed since" sync.** The app loads whole lists, which is fine for the expected sizes.
- **Photo storage is small.** The free MongoDB tier has 512 MB, and the photo limits are chosen to fit a
  few users.
- **Biometric and backup rules** are from the authentication work and are not part of this layer.
