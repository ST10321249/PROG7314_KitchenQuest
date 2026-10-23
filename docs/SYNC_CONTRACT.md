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

- Changes for the same item are sent in the order they were made.
- Changes for different items do not wait for each other.
- A transfer waits for the create of the shopping item it refers to, if that create has not been sent yet.
- Only one change is sent at a time, oldest first.

### Combining changes

Before sending, changes that have not yet started are merged so less is sent and nothing can arrive out of
order:

- An edit to an item whose create is still waiting is folded into that create.
- Several edits to one item are merged into one, and the latest value of each field wins.
- Deleting an item whose create never left the phone cancels both, and nothing is sent.
- Deleting an item removes any waiting edits to it.
- A change that is already `IN_FLIGHT` is never altered. Later changes are queued behind it.

### Result of sending a change

| Result | What happens |
| --- | --- |
| Success | The change is `SUCCEEDED` and removed. The local row gets the server `id` and `updatedAt`. |
| Network error, timeout, `5xx` | Back to `PENDING` with a longer wait before the next attempt. |
| `401` | Back to `PENDING`. Nothing is lost. |
| `400` | `FAILED` with the server message. The local row is marked as failed so it can be shown to the user. |
| `404` on a delete | Success. The item is already gone. |
| `404` on an edit | The item was deleted elsewhere. The edit is dropped and the local row is removed. |
| Still failing after `SyncPolicy.MAX_ATTEMPTS` attempts | `FAILED` |

Retries start at `SyncPolicy.INITIAL_BACKOFF_SECONDS` and grow each time.

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
- Photos and push notifications are described in the data layer handoff.
