# Target ↔ Lilies Lib contract (v1)

How Target (Android, `com.ridvan.target`) picks a book from Lilies Lib (`com.anonymous.eighteeneleven`).
Both apps are offline and signed with different keys, so they talk only through Android intents on the
phone — no network, no signature permissions. Target's code for this: `data/extensions/LiliesLibContract.kt`
(constants + parser, unit-tested) and `LiliesLib.kt`.

## v1: pick a book

### Request (Target → Lilies Lib)

Target calls `startActivityForResult` (Activity Result API) with:

| | |
|---|---|
| action | `com.ridvan.target.action.PICK_BOOK` |
| package | `com.anonymous.eighteeneleven` (explicit) |
| extra `contract_version` | `Int`, the highest version Target understands (now `1`) |

Lilies Lib needs an **exported** activity with this intent filter:

```xml
<activity android:name=".PickBookActivity" android:exported="true">
  <intent-filter>
    <action android:name="com.ridvan.target.action.PICK_BOOK" />
    <category android:name="android.intent.category.DEFAULT" />
  </intent-filter>
</activity>
```

In Expo this means a small native module / config plugin: the activity shows Lilies Lib's book list
(or a picker screen), then calls `setResult(...)` and `finish()`.

### Answer (Lilies Lib → Target)

- **Cancel / back:** `RESULT_CANCELED` (no data). Target does nothing.
- **Picked:** `RESULT_OK` with a result `Intent` whose **extras** carry the book. No content:// URI is needed for
  v1 (it's one small record).

| extra | type | required | meaning |
|---|---|---|---|
| `contract_version` | Int | yes (missing is read as 1) | version this answer follows; `1` |
| `book_id` | String | **yes** | Lilies Lib's own stable id for the book (its SQLite row id or uuid, as text). Must not change for that book. |
| `title` | String | **yes** | non-blank |
| `author` | String | no | |
| `page_count` | Int | no | > 0, otherwise ignored |
| `publisher` | String | no | |
| `published_year` | Int | no | 1..9999 |
| `cover_uri` | String (content:// URI) | no | if sent, add `FLAG_GRANT_READ_URI_PERMISSION` to the result intent. **Target v1 ignores it** (it draws covers). |

Lenient on purpose (React Native bridges differ): numbers may come as Int, Long, Double (`17.0`) or text (`"160"`);
an id sent as a number is turned into text. A missing `book_id` or blank `title` = nothing happens in Target.

What Target does with it: creates a new Target book (status Reading, random drawn cover) with
`externalSource = "lilies_lib"` and `externalId = book_id`. If the account already has a book with that
`externalId`, it shows "already in your library" and adds nothing. Existing Target books are never matched
or merged; nothing is imported automatically.

### When Target shows the button

Settings → Extensions → Lilies Lib → "Use in Target" is on (per Target account) **and** Lilies Lib is installed.
If it's installed but doesn't answer `PICK_BOOK` (older version), Target says to update it.
Target finds the package via a `<queries><package android:name="com.anonymous.eighteeneleven"/></queries>` entry.

## Versioning rules

- Inside a version, changes are **additive only**: new optional extras. Old Target ignores extras it doesn't know.
- A breaking change (renamed/removed/re-typed field, different meaning) gets a new number. Target sends the highest
  version it knows in the request; Lilies Lib should answer with a version `<=` that number. Target ignores answers
  with a version it doesn't know (`> 1` today).
- The action name stays the same across versions.

## Planned, not built: v2 (progress back to Lilies Lib)

Design only, so v1 doesn't paint us into a corner:

- The link key is `book_id` ↔ Target's `books.externalId` (with `externalSource = "lilies_lib"`).
- Direction Target → Lilies Lib, e.g. current page / finished. Likely an exported `ContentProvider` in Lilies Lib
  (authority e.g. `com.anonymous.eighteeneleven.target`) that Target writes to with `ContentResolver.update`, or an
  explicit broadcast/activity intent `com.ridvan.target.action.READING_PROGRESS` carrying `contract_version`,
  `book_id`, `current_page`, `finished` (Boolean), `updated_at` (epoch ms). Since the keys differ, protect it with a
  user-visible confirmation in Lilies Lib or a normal-level custom permission rather than a signature permission.
- Whichever is chosen will be `contract_version = 2`; v1 picking keeps working unchanged.
