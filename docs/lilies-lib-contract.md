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

## Library provider: sync and "Refresh from Lilies Lib"

Target keeps books it picked up to date (only those: it never imports other Lilies Lib books). Runs when Reading or
the Library opens if Settings → Extensions → Lilies Lib → "Sync automatically" is on, and on demand with the
refresh icon on a linked book's page. Read-only: Target never writes to Lilies Lib through it.

Lilies Lib needs:

```xml
<permission
    android:name="com.anonymous.eighteeneleven.permission.READ_LIBRARY"
    android:protectionLevel="dangerous"
    android:label="read your Lilies Lib library" />

<provider
    android:name=".TargetLibraryProvider"
    android:authorities="com.anonymous.eighteeneleven.target"
    android:exported="true"
    android:readPermission="com.anonymous.eighteeneleven.permission.READ_LIBRARY" />
```

`dangerous` means Android shows the user an "Allow" dialog the first time Target turns sync on. This works across
different signing keys (unlike `signature`). Target declares `<uses-permission>` for it.

`query(content://com.anonymous.eighteeneleven.target/books)` returns **one row per book**, all books (Target picks
the ids it has linked). Columns use the picker's names: `contract_version` (1), `book_id`, `title`, `author`,
`page_count`, `publisher`, `published_year`. Same types and leniency as the picker; a missing column = empty.
Insert/update/delete: not supported (return null/0).

Merge rule in Target: Lilies Lib wins for title, author, pages, publisher, year, but an empty Lilies field never
erases what Target has. Target-only things (status, rating, notes, goal, cover, sessions) are never touched. A
linked book that no longer exists in Lilies Lib is left alone ("This book is no longer in Lilies Lib").

## Send a Target book to Lilies Lib (manual)

Book page in Target → "Send to Lilies Lib" icon (only books not linked yet). Never automatic.

| | |
|---|---|
| action | `com.ridvan.target.action.ADD_BOOK` (explicit package, `startActivityForResult`) |
| extras | `contract_version`, `title` (always), `author`, `page_count`, `publisher`, `published_year` (when set) |

Lilies Lib needs an exported activity with that action + `DEFAULT` category. It shows the book and asks the user to
confirm (it may detect that it already has the book). Answer:
- `RESULT_OK` with extras `contract_version` = 1 and `book_id` = the new (or the matching existing) Lilies Lib book's id.
  Target then links its book to that id, so sync works for it from then on. If another Target book is already linked
  to that id, Target says so and doesn't link.
- `RESULT_CANCELED`: nothing happens.

## Planned, not built: progress back to Lilies Lib

- Link key: `book_id` ↔ Target's `books.externalId` (with `externalSource = "lilies_lib"`).
- Target → Lilies Lib current page / finished, e.g. an explicit intent `com.ridvan.target.action.READING_PROGRESS`
  with `contract_version`, `book_id`, `current_page`, `finished`, `updated_at` (epoch ms), confirmed in Lilies Lib.
