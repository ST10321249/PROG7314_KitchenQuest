# KitchenQuest sync contract

This document describes how pantry and shopping data moves between the app (Room database and a queue of
pending changes) and the API (MongoDB). It is the agreement that the offline repositories, the sync worker
and the API all follow, so changes to any of them should be checked against it.

Pantry and shopping are covered first because they are the workflows that must work offline. Cooking
history, favourites and the user profile are not part of this contract yet (see the last section).

## 1. Identifiers

Every pantry and shopping item has two ids.

| Id | Created by | Purpose |
| --- | --- | --- |
| `clientId` | The app, when the item is first created | Stays the same for the whole life of the item, even before the server has seen it. It is how the server recognises a create that was sent twice. |
| `id` | MongoDB, when the item is first saved | The id used in API paths such as `PATCH /api/pantry/:id`. |

Rules:

- `clientId` is a UUID made by the app (`ClientIds.newId()`). The API accepts any 8 to 64 characters from
  `A-Z a-z 0-9 - _`.
- `clientId` can only be set when an item is created. Any `clientId` sent in an update is ignored.
- The server returns both `id` and `clientId` for every item.
- Items created before this contract have no `clientId` (it is `null`). When the app first loads such an item
  it uses the server `id` as the local key, so nothing needs to be migrated on the server.
- The app keeps one stable local key per item: the `clientId` when it has one, otherwise the server `id`.
  Screens only ever see this local key, so an item's id does not change when it finishes syncing. The
  repository swaps in the server `id` when it calls the API.
- A `clientId` is unique per user. Two users can never clash, and the API never returns another user's item.

## 2. User separation

- Every API call is made as the signed-in Firebase user. The API filters every query by the Firebase `uid`.
- Every Room row and every queued change stores the `uid` of the user it belongs to.
- The app only reads and sends rows for the user who is signed in now. Rows for a different account stay on
  the phone untouched and are not shown or sent until that account signs in again.

## 3. Timestamps

- `updatedAt` is set by the server whenever an item is created or changed, and is returned as an ISO 8601
  string. The app never sends it.
- The app records its own `createdAt` on each queued change. This only decides the order changes are sent in.
- Timestamps are for ordering and display. They are not used to decide conflicts (see section 6).

## 4. Endpoints

All endpoints need `Authorization: Bearer <Firebase ID token>`. Without it they answer `401`.

### Pantry (`/api/pantry`) and shopping (`/api/shopping-list`)

| Call | Success | Notes |
| --- | --- | --- |
| `GET /` | `200` list | Returns every item for the user, with `id`, `clientId` and `updatedAt`. |
| `POST /` | `201` created, or `200` if this `clientId` already exists | Safe to send again. The response is always the stored item. Without a `clientId` it behaves as before and always creates. |
| `PATCH /:id` | `200` item | Only the fields sent are changed. `404` if the item does not exist, belongs to someone else, or `:id` is not a valid server id. |
| `DELETE /:id` | `204` | `404` if the item is already gone. The app treats that as success. |
| `POST /api/shopping-list/transfer` | `201` created, or `200` if already done | See section 7. |

Failure codes the app should expect:

| Code | Meaning | What the app does |
| --- | --- | --- |
| `400` | The request is not valid (blank name, quantity not above zero, past expiry date, bad `clientId`) | Mark the change as failed. Retrying will not help. |
| `401` | Missing, expired or invalid token | Keep the change, try again once the user is signed in. |
| `404` | The item does not exist | Depends on the change, see section 5. |
| `5xx` or no connection or timeout | The server or network is unavailable | Keep the change and retry later. |

## 5. Queued changes

Each change the user makes is saved to Room in the same transaction as the data it changes, so it survives
the app being closed. A change has:

- an operation id (`ClientIds.newId()`), the `uid`, the type and the item it refers to
- a payload with the fields to send
- a status: `PENDING`, `IN_FLIGHT`, `SUCCEEDED` or `FAILED`
- an attempt count, the last error, `createdAt` and the time of the next attempt

### Order

- Changes are sent one at a time, oldest first, so a change always arrives after the changes it depends on
  (an edit after its create, a transfer after the create of the shopping item).
- Changes to different items do not depend on each other. They are only held back while an older change is
  waiting to be retried, which is nearly always because the network is down for everything. A change that has
  used up all its attempts becomes `FAILED` and no longer holds anything back.

### Combining changes

Before sending, changes that have not yet started are merged so less is sent and nothing can arrive out of
order:

- An edit to an item whose create is still waiting is folded into that create.
- Several edits to one item are merged into one, and the latest value of each field wins.
- Deleting an item whose create never left the phone cancels both, and nothing is sent.
- Deleting an item removes any waiting edits to it.
- Editing a change that had `FAILED` is treated as the user fixing it, so it starts again as `PENDING` with
  its attempts reset. Deleting an item whose create had failed simply removes it, because the server never
  had it.
- A change that is already `IN_FLIGHT` is never altered. Later changes are queued behind it.

### Result of sending a change

| Result | What happens |
| --- | --- |
| Success | The change is removed. The local row gets the server `id` and `updatedAt`, and any changes queued behind the create are given the server `id`. |
| Timeout, `5xx`, or any other unexpected failure | Back to `PENDING`, counted as an attempt, with a longer wait before the next one. |
| No connection | Back to `PENDING`. Not counted, because the change is not at fault. |
| `401` | Back to `PENDING`. Not counted. Nothing is lost. |
| `400` | `FAILED` with the server message, and the local row is marked `FAILED` so it can be shown to the user. |
| `404` on a delete | Success. The item is already gone. |
| `404` on an edit | The item was deleted elsewhere. The edit is dropped and the local row is removed. |
| `404` on a create or transfer | Treated as a failure that can be retried. It almost always means the server has not been updated yet. |
| Still failing after `SyncPolicy.MAX_ATTEMPTS` (8) attempts | `FAILED`, and it stops holding up newer changes. |

Retries start at `SyncPolicy.INITIAL_BACKOFF_SECONDS` (30 seconds) and double each time, up to an hour.

### When changes are sent

- A background job (`SyncWorker`, run by WorkManager) sends the queue. It only runs while the phone is
  connected, and WorkManager runs it again later, with a growing wait, if something could not be sent.
- A sync is requested every time the user changes something, every time a list is opened while changes are
  waiting, and when the app starts.
- Only one sync runs at a time. A request made while one is running starts another straight after it.
- Only the signed-in account's changes are sent. Another account's changes wait until that account signs in.
- If the app is closed while a change is being sent, it is sent again next time. This is safe because
  creates are recognised by their `clientId` and deleting something that is gone counts as success.

## 6. Conflicts

Two devices, or the app and the website, can change the same item. The rules are kept simple on purpose:

1. **Fields are merged.** An edit only sends the fields that changed, so edits to different fields of the
   same item both survive.
2. **The last change to reach the server wins** for a field edited on both sides.
3. **A delete beats an edit.** If the item was deleted on the server, a waiting edit is dropped.
4. **Waiting changes beat the server copy on the phone.** When the app refreshes from the server it does not
   overwrite an item that still has changes waiting. Once those are sent, the next refresh shows the result.

## 7. Moving a bought item to the pantry

`POST /api/shopping-list/transfer`

```json
{
  "shoppingItemId": "507f1f77bcf86cd799439011",
  "shoppingClientId": "optional, used when the item has not got a server id yet",
  "pantryItem": {
    "clientId": "required, a new UUID",
    "ingredientName": "Eggs",
    "quantity": 12,
    "unit": "pcs",
    "category": "Dairy",
    "expiryDate": "2026-11-01"
  }
}
```

Either `shoppingItemId` or `shoppingClientId` must be given. The response is the pantry item.

The server first creates the pantry item (once only, because of its `clientId`) and then removes the shopping
item (which does nothing if it is already gone). Each step is safe to repeat, so if the first request times
out the app sends the same request again and the end result is one pantry item and no shopping item. It is
not a single database transaction, but it can never produce a duplicate or lose the item.

On the phone the same change is made in one Room transaction (add the pantry row, remove the shopping row,
queue one transfer), so the user sees it happen immediately.

The queued request refers to the shopping item by its server `id` when it has one, and by its `clientId`
otherwise. If the shopping item itself never left the phone, there is nothing on the server to remove, so
no transfer is queued and the pantry item is simply created.

## 8. Refreshing from the server

When the phone is online and a user's queue is empty (or after it has been sent), the app loads
`GET /api/pantry` or `GET /api/shopping-list` and updates Room:

1. Each server item is matched to a local row by `clientId`, or by `id` for older items.
2. A matched row is updated, unless that item has changes waiting (rule 4 in section 6).
3. A server item with no local row is added.
4. A synced local row with no server item was deleted elsewhere, so it is removed.
5. Rows with changes waiting are never removed by a refresh.

## 9. Not covered yet

- Cooking history, saved recipes and the profile still use the network directly. They can be added with
  the same `clientId` approach later.
- There is no "changed since" request and no delete markers. Lists are small, so the app loads the whole
  list. Delete markers can be added if lists ever grow large.
- Translations are cached on the phone (`TranslationCache`) but are not part of syncing. They are display
  copies and never leave the phone.
- Photos and push notifications are described in `DATA_LAYER_HANDOFF.md`.
