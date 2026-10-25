# Personal photo journal for Android: design spec

Date: 2026-10-26
Status: draft for review
Design canvas: https://claude.ai/artifact/DDzFKY7BWM8EZ9nrUhZSoP (page "Final design")

## 1. Summary

A private journal for one person on one Android phone. It opens to the camera so a moment can be captured in one action, also takes text entries and gallery imports, and organises everything by day: each logged day becomes a tile in a month "bento" grid, bigger when the day was fuller. Entries can carry optional tags (gym, office, cricket, badminton, ...) and the month can be filtered by tag. There is no server; an encrypted backup goes to the owner's own Cloudflare R2 bucket.

## 2. Goals and non-goals

Goals
- Capture in one action from opening the app (photo) and one gesture for writing.
- Scrolling back through days and months is pleasant and fast.
- Nothing is lost when the phone is lost: encrypted, automatic backup with a tested restore.
- Private by default: nothing appears in the gallery; nothing leaves the phone unencrypted.

Non-goals (v1)
- Mood, location, video, search over text, sharing, multiple devices, accounts, analytics, crash-reporting SDKs.
- "On this day" (planned for the week after v1).
- Tag rename/merge management.

## 3. Scope of the first build

| Area | Included |
|---|---|
| Capture | In-app camera, caption, tags, save |
| Write | Text entry with tags and editable time |
| Import | Multi-select from Android Photo Picker, one entry per photo, original date, shared tags |
| Look back | Month bento view, tag filter, day view, entry view |
| Edit | Caption/text, date/time, tags; delete with confirmation |
| Privacy | Fingerprint/PIN lock (on by default) |
| Habit | One daily reminder at a chosen time |
| Backup | Encrypted, automatic, incremental backup to R2; full restore |

## 4. Screens and behaviour

Visual reference: the "Final design" page of the canvas. Screens not yet drawn (lock, settings, backup setup, restore, camera permission off, delete confirmation) follow the same visual system (section 5) during the build and are reviewed as screenshots before being called done.

### 4.1 Capture (launch screen)
- Camera preview in a rounded card; flash and front/back switch over it.
- "Today, N entries" row under the preview opens the Month view.
- Bottom row: Gallery (Photo Picker), shutter, Write.
- Launcher shortcuts (long-press icon): "Take photo", "Write".
- Camera permission denied: preview area shows "Camera access is off" with an "Open settings" button; Gallery and Write still work.

### 4.2 After the shot
- Photo preview, Retake, caption field, tag row, time ("Today, 08:42"), Save.
- Save returns to Capture so the next shot is immediate.

### 4.3 Tag row and tag search (used by After the shot, Write, Import)
- Row: search icon, then tags ordered by selected first, then most-used, then most-recent. Tap toggles; selected = white fill.
- Search: the row becomes a field; matches filter live; no exact match offers `+ Create "<text>"`; currently selected tags stay visible.
- Tag names are stored lowercase and trimmed; max 24 characters; duplicates collapse.
- Any number of tags per entry; tags are always optional.

### 4.4 Write
- Keyboard opens immediately; time chip (tap to change); tag row above keyboard; Save.
- Leaving with unsaved text asks "Discard this entry?".

### 4.5 Import
- Android Photo Picker, multi-select (up to 20 per import).
- One card per photo: thumbnail, date, Change, caption.
- Date comes from EXIF `DateTimeOriginal`; if missing, today is used and the card says "No date in this photo, so it's set to today."
- "Tags for all N" row applies to every imported photo.
- "Add N entries" saves all; partial failure reports which photos failed and keeps the rest.

### 4.6 Month (bento)
- Header: month name, "N days, M entries"; settings button.
- Tag filter chips: All + tags used this month. Selecting a tag dims non-matching days (opacity ~0.2, positions unchanged) and the subtitle reads "<tag> on N days this month"; the chip shows ✕ to clear.
- Only days with entries get tiles. Today's tile carries a white "Today, 25" label.
- Swipe horizontally (or tap the month name) to change month.
- Camera button (bottom-right) returns to Capture.
- First open (no entries): one large "Today" card with "Take a photo" and "Write".

Bento sizing rules (computed, never stored)
- Today: 2x2.
- Photo days: 1 entry 1x1; 2-3 entries 2x1; 4+ entries 2x2.
- Text-only days: 1x1, or 2x1 when the first text exceeds ~60 characters.
- Mixed days follow the photo rule; text-only parts of a large tile show the first text.
- Packing: 3 columns, days in descending date order, greedy placement into the first free slot that fits, filling gaps with later 1x1 tiles when possible.
- Fallback if packing proves unstable: equal 1x1 tiles (the rest of the app is unaffected).

### 4.7 Day
- Back to month; day name, "N entries".
- Entries in time order as soft cards: photo with caption, tags and time; text cards clamp to 3 lines with "Read all".
- Camera button returns to Capture.

### 4.8 Entry
- Photo (or text), date/time, caption/text, tags.
- Actions: Edit (caption/text, tags), Date (date and time picker), Delete (isolated, outlined).
- Delete confirmation: "Delete this entry? The photo and caption will be removed from this phone and from your backup."

### 4.9 Settings
- Lock the journal (on/off).
- Daily reminder (on/off), time (default 21:00), text "Anything from today?".
- Backup: status line, Back up now, Use mobile data (off by default), Show recovery phrase (after lock prompt), Restore, Disconnect.
- Storage line: "N entries, X MB on this phone".

### 4.10 Lock
- When enabled, the app shows a locked screen on cold start and after 1 minute in the background; BiometricPrompt with device credential fallback.
- Launcher shortcuts still require unlock before showing content.

## 5. Visual system ("Soft surfaces", Mono)

| Token | Value |
|---|---|
| Background | #121212 |
| Surface | #1C1C1C |
| Surface raised | #262626 |
| Text primary | #EDEDED |
| Text secondary | #BDBDBD |
| Text muted | #8C8C8C |
| Primary action | #EDEDED fill, #121212 text |
| Destructive | outline #3A2522, icon #E58A80 |
| Typeface | Geist (400/500/600) via bundled font; sizes map to Material type roles in sp |
| Radius | tiles 20, cards 24, buttons 16, chips 16 (pill), photo frames 28 |
| Spacing | 4-pt scale; screen gutter 12-20 |

- Dark theme only in v1.
- No accent colour; colour comes from the photos.
- Touch targets at least 48 dp; respect system font scale and "Remove animations".
- Motion: Material shared-axis between Month, Day and Entry; fade-through elsewhere.

## 6. Architecture

- Kotlin, Jetpack Compose, single Gradle module, min SDK 29, target latest.
- Packages: `ui/<screen>` (Composable + ViewModel), `data` (repository, Room, file store), `backup`, `lock`, `reminder`.
- Manual dependency wiring through one `AppContainer` created in `Application`; no DI framework.
- Each screen has a ViewModel exposing a `StateFlow` of UI state; screens never touch Room or files directly.
- `JournalRepository` is the only owner of database and photo-file writes; backup observes it.

| Need | Library |
|---|---|
| Camera | CameraX (camera-core, camera-view) |
| Gallery | ActivityResultContracts.PickMultipleVisualMedia |
| EXIF | androidx.exifinterface |
| Database | Room (KSP) |
| Images | Coil (Compose) |
| Lock | androidx.biometric |
| Background work | WorkManager (backup, reminder) |
| Navigation | Navigation Compose |
| HTTP | OkHttp, with a small AWS SigV4 signer for R2's S3 API (PUT, GET, DELETE, LIST only) |
| Crypto | JCA AES-256-GCM, HKDF-SHA256; Android Keystore for at-rest key wrapping |

## 7. Data model (Room)

entries
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | UUID |
| type | TEXT | PHOTO or TEXT |
| text | TEXT? | caption, or body for TEXT |
| photo_file | TEXT? | relative path under `files/photos/` |
| thumb_file | TEXT? | relative path under `files/thumbs/` |
| taken_at | INTEGER | epoch millis, UTC |
| local_date | TEXT | `YYYY-MM-DD`, fixed at save in the device's zone at that time |
| date_source | TEXT | CAMERA, EXIF, FALLBACK_TODAY, MANUAL |
| created_at | INTEGER | |
| updated_at | INTEGER | bumped on every change |
| deleted_at | INTEGER? | soft-delete marker |
| backed_up_at | INTEGER? | null or < updated_at means "needs upload" |

Indexes: `local_date`, `taken_at`, `(deleted_at, backed_up_at)`.

tags: `id` TEXT PK, `name` TEXT UNIQUE (lowercase), `use_count` INTEGER, `last_used_at` INTEGER.

entry_tags: `entry_id`, `tag_id`, composite PK; foreign keys with cascade.

Rules
- Changing an entry's date manually recomputes `local_date` from the new date.
- Delete: set `deleted_at`, bump `updated_at`, delete local photo and thumbnail; row stays until its tombstone is backed up, then is purged.
- Tag `use_count` and `last_used_at` update on save.

## 8. Photo storage

- App-private `files/photos/<id>.jpg`: re-encoded JPEG, long edge max 2560 px, quality 85, EXIF orientation applied, EXIF location stripped.
- `files/thumbs/<id>.jpg`: long edge 480 px.
- Write order: write to `*.tmp`, fsync, rename, then insert the Room row in a transaction. On launch, a cleanup removes files without rows and rows (non-deleted PHOTO) whose files are missing are reported in Settings.
- `android:allowBackup="false"`: Android's own cloud backup is disabled; R2 backup is the only copy off-device.

## 9. Backup (Cloudflare R2, encrypted, first build)

### 9.1 Setup
- One-time screen in Settings: Account ID, Access Key ID, Secret Access Key, bucket name.
- "Test connection" writes, reads and deletes a probe object before saving.
- Credentials are stored encrypted with an Android Keystore key; never compiled into the APK.
- On first setup the app generates a 12-word recovery phrase (128-bit entropy, BIP-39 word list) and asks the owner to write it down; it must be re-typed once to confirm.

### 9.2 Encryption
- Master key = HKDF-SHA256(recovery phrase entropy, info "journal-backup-v1"), 256-bit.
- The master key is kept on the phone wrapped by an Android Keystore key, so daily backups need no phrase entry.
- Each uploaded object = `version(1 byte) | nonce(12 bytes) | AES-256-GCM ciphertext+tag`, with a fresh random nonce per object.
- `meta.json.enc` holds format version and a key-check value so restore can verify the phrase before downloading everything.
- Object names use entry UUIDs only; no dates, tags or text in names.

### 9.3 What is uploaded
- `entries/<id>.json.enc`: id, type, text, taken_at, local_date, date_source, tags (names), updated_at, deleted_at.
- `photos/<id>.jpg.enc`: full-size photo (thumbnails are rebuilt on restore).
- Tombstone: deleting uploads the entry JSON with `deleted_at` set and deletes `photos/<id>.jpg.enc`.

### 9.4 When
- WorkManager unique job: a few minutes after any save, plus every 6 hours.
- Constraints: unmetered network (unless "Use mobile data" is on), battery not low.
- Incremental: uploads entries where `backed_up_at` is null or older than `updated_at`; photo first, then entry JSON, then `backed_up_at` is set.
- Retries with exponential backoff; per-entry failures do not block other entries.

### 9.5 Restore
- Settings > Restore (also offered on first launch when no entries exist): enter credentials and recovery phrase, verify against `meta.json.enc`, then list and download all `entries/`, then photos, with a progress bar; rebuild thumbnails; skip tombstoned entries.
- Restore into a non-empty journal merges by id, newest `updated_at` wins.

### 9.6 Status and errors
- Settings shows "Last backup <time>, N entries" or the exact failure (wrong credentials, bucket not found, no network, storage full).
- A persistent failure for more than 3 days shows a one-line banner on the Month view.

### 9.7 Cost
- About 1 GB per year at 5 photos a day; inside R2's 10 GB free tier for years.

## 10. Reminder
- WorkManager periodic job aligned to the chosen time (a few minutes of drift accepted).
- Notification "Anything from today?" opens Capture; it is skipped if an entry was already saved today.
- Requests POST_NOTIFICATIONS permission when the reminder is first enabled.

## 11. Edge cases
- Time zones: `local_date` is fixed at save; travelling never moves past entries.
- Midnight: an entry saved at 23:59 belongs to that day; the Today tile switches on the next app foreground.
- Very long text: Day clamps to 3 lines; Entry shows full text, scrollable.
- Low storage: saving checks free space (needs at least 50 MB) and shows an error instead of failing silently.
- Photo Picker returns an unreadable item: that card shows "This photo couldn't be read" and is excluded.
- Lost recovery phrase: existing phone keeps backing up (key is wrapped locally); Settings lets the owner view the phrase after unlock so it can be re-saved.

## 12. Testing
- Unit: bento packing (fixed fixtures for 1-31 days, mixed sizes, no overlaps, no gaps where avoidable), date derivation (EXIF, fallback, zone changes), tag normalisation, crypto round-trip, SigV4 signing against known vectors.
- Room: DAO tests for month query, tag filter, needs-backup query, soft delete.
- Integration: backup + restore round-trip against a real R2 test bucket (manual, run before each release); restore on a fresh emulator.
- UI: screenshots of every screen on emulator at default and 1.3 font scale, reviewed before "done".

## 13. Milestones (first build, about 1.5 weeks)

| # | Milestone | Done when |
|---|---|---|
| M1 | Project, theme, data layer, photo store | Room + file store with tests pass |
| M2 | Capture, After the shot, Write, tags | Entries save with tags on device |
| M3 | Month bento, tag filter, Day, Entry, edit/delete | Navigating a seeded month works on device |
| M4 | Import, lock, reminder, settings | All flows work on device |
| M5 | Encrypted R2 backup and restore | Round-trip restore on a fresh emulator succeeds |
| M6 | Polish pass and screenshot review | Every screen reviewed at both font scales |

After v1: "On this day".

## 14. Open items
- App name (undecided).
- Lock-related and backup screens are specified here but not yet drawn; they follow section 5 and are reviewed as screenshots during M4-M5.
