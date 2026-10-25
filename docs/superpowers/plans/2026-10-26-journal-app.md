# Personal Photo Journal (Android) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first version of a private, capture-first Android journal with a month bento view, tags, lock, reminder and encrypted backup to Cloudflare R2.

**Architecture:** Single Gradle module, Kotlin + Jetpack Compose. Screens own a ViewModel exposing `StateFlow` UI state; all database and photo-file writes go through `JournalRepository`. Backup is a WorkManager job that reads "needs backup" rows from the repository, encrypts with AES-256-GCM and uploads to R2 through a small S3 SigV4 client.

**Tech Stack:** Kotlin 2.x, Compose (Material 3), Navigation Compose, Room (KSP), CameraX, Coil, androidx.biometric, WorkManager, OkHttp, androidx.exifinterface; tests with JUnit4, Robolectric, kotlinx-coroutines-test, Room in-memory DB, OkHttp MockWebServer.

**Spec:** `docs/superpowers/specs/2026-10-26-journal-app-design.md`

## Global Constraints

- Package / applicationId: `com.personal.journal`. minSdk 29, targetSdk and compileSdk = latest stable.
- No server, no accounts, no analytics, no crash SDK. `android:allowBackup="false"`.
- Dark theme only. Colors: background `#121212`, surface `#1C1C1C`, surface raised `#262626`, text `#EDEDED`/`#BDBDBD`/`#8C8C8C`, primary action fill `#EDEDED` with text `#121212`, destructive outline `#3A2522` icon `#E58A80`. No accent color.
- Typeface Geist 400/500/600 bundled in `res/font`; all text sizes in sp mapped to Material type roles.
- Radii: tiles 20dp, cards 24dp, buttons 16dp, chips pill, photo frames 28dp. Touch targets at least 48dp.
- Photos: long edge max 2560 px, JPEG quality 85, EXIF orientation applied, GPS stripped; thumbnails long edge 480 px.
- Tag names: lowercase, trimmed, max 24 chars, unique.
- Backup objects: `entries/<id>.json.enc`, `photos/<id>.jpg.enc`, `meta.json.enc`; format `version(1) | nonce(12) | AES-256-GCM ciphertext+tag`; key = HKDF-SHA256(128-bit phrase entropy, info `"journal-backup-v1"`), 256-bit.
- Backup job: a few minutes after any save + every 6 h; unmetered network unless "Use mobile data"; battery not low.
- Copy strings (exact): "Today, N entries", "Add a caption", "Each photo becomes its own entry, on the day it was taken.", "No date in this photo, so it's set to today.", "Tags for all N", "Add N entries", "N days, M entries", "<tag> on N days this month", "Nothing logged yet", "Your first entry goes here. Take a photo, or write a line about today.", "Each day you log becomes a tile. Busier days get bigger ones.", "Camera access is off", "Open settings", "Discard this entry?", "Delete this entry? The photo and caption will be removed from this phone and from your backup.", "Anything from today?", "Read all".

## Review Focus

- A phone photo with EXIF orientation 6 (rotated) must be saved and shown upright, not sideways. Test in Task 3.
- An entry saved at 23:59 in IST and then viewed after the phone's zone changes to UTC must stay on the same calendar day. Test in Task 2.
- Backup interrupted after the photo upload but before the entry JSON upload must retry the whole entry next run and never mark it backed up. Test in Task 12.
- Restore with a wrong recovery phrase must fail before downloading any entries and show "Recovery phrase doesn't match this backup." Test in Task 13.
- Tag input "  Gym ", "GYM", a 30-char name and an empty string: the first two collapse to `gym`, the long one is rejected, the empty one is ignored. Test in Task 2.

---

## File Structure

```
app/src/main/java/com/personal/journal/
  JournalApp.kt                 Application; builds AppContainer
  AppContainer.kt               manual DI: db, repository, photo store, backup, settings
  MainActivity.kt               Compose host, lock gate, nav graph, launcher shortcuts
  ui/theme/Theme.kt, Type.kt    tokens from Global Constraints
  ui/components/TagRow.kt       tag row + tag search (shared by Review, Write, Import)
  ui/components/Chips.kt        filter chip, tag chip, date label
  ui/capture/CaptureScreen.kt, CaptureViewModel.kt
  ui/review/ReviewScreen.kt, ReviewViewModel.kt
  ui/write/WriteScreen.kt, WriteViewModel.kt
  ui/importer/ImportScreen.kt, ImportViewModel.kt
  ui/month/MonthScreen.kt, MonthViewModel.kt, BentoGrid.kt
  ui/day/DayScreen.kt, DayViewModel.kt
  ui/entry/EntryScreen.kt, EntryViewModel.kt
  ui/settings/SettingsScreen.kt, SettingsViewModel.kt, BackupSetupScreen.kt, RestoreScreen.kt
  ui/lock/LockScreen.kt, LockGate.kt
  data/db/JournalDatabase.kt, Entities.kt, EntryDao.kt, TagDao.kt
  data/JournalRepository.kt     only writer of db + photo files
  data/PhotoStore.kt            resize, thumbnail, atomic write, cleanup
  data/Dates.kt                 local_date derivation, EXIF date parsing
  data/Tags.kt                  tag normalisation
  data/Settings.kt              DataStore-backed prefs (lock, reminder, backup flags)
  bento/BentoPacker.kt          pure Kotlin tile sizing + packing
  backup/Crypto.kt              phrase, HKDF, AES-GCM envelope, Keystore wrapping
  backup/SigV4.kt               AWS SigV4 signer
  backup/R2Client.kt            put/get/delete/list over OkHttp
  backup/BackupCodec.kt         entry <-> JSON
  backup/BackupRunner.kt        one backup pass (pure logic, testable)
  backup/BackupWorker.kt        WorkManager wrapper + scheduling
  backup/RestoreRunner.kt       restore logic
  backup/CredentialStore.kt     Keystore-encrypted R2 creds + wrapped master key
  reminder/ReminderWorker.kt    notification + scheduling
app/src/main/res/font/geist_*.ttf, raw/bip39_english.txt
app/src/test/...                unit + Robolectric tests mirroring the packages
```

---

### Task 1: Project scaffold, theme and app shell

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `JournalApp.kt`, `AppContainer.kt`, `MainActivity.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`, `res/font/geist_regular.ttf|geist_medium.ttf|geist_semibold.ttf`, `.gitignore`
- Test: `app/src/test/java/com/personal/journal/ui/theme/ThemeTest.kt`

**Interfaces:**
- Produces: `JournalTheme(content: @Composable () -> Unit)`; `object JournalColors { val Background, Surface, SurfaceRaised, TextPrimary, TextSecondary, TextMuted, PrimaryFill, OnPrimary, DestructiveOutline, DestructiveIcon: Color }`; `object JournalShapes { val tile = RoundedCornerShape(20.dp); card = 24.dp; button = 16.dp; photo = 28.dp }`; `class AppContainer(context: Context)` (fields added by later tasks); `MainActivity` hosting a `NavHost` with route `"capture"` showing a placeholder.

- [ ] **Step 1:** `git init`, add `.gitignore` (Android template), Gradle wrapper, version catalog with all Tech Stack libraries, manifest with `allowBackup="false"`, `CAMERA` and `POST_NOTIFICATIONS` permissions.
- [ ] **Step 2: Write the failing test** `ThemeTest.colorsMatchSpec()` asserting `JournalColors.Background == Color(0xFF121212)`, `Surface == Color(0xFF1C1C1C)`, `PrimaryFill == Color(0xFFEDEDED)`, `OnPrimary == Color(0xFF121212)`.
- [ ] **Step 3:** Run `./gradlew testDebugUnitTest --tests '*ThemeTest*'`. Expected: FAIL (unresolved reference).
- [ ] **Step 4:** Implement Theme.kt/Type.kt (dark `ColorScheme` built from JournalColors, Geist `FontFamily`), JournalApp, AppContainer, MainActivity with placeholder.
- [ ] **Step 5:** Run `./gradlew testDebugUnitTest assembleDebug`. Expected: PASS, APK builds.
- [ ] **Step 6:** Commit `chore: scaffold app, theme tokens and shell`.

### Task 2: Database, dates and tag rules

**Files:**
- Create: `data/db/Entities.kt`, `EntryDao.kt`, `TagDao.kt`, `JournalDatabase.kt`, `data/Dates.kt`, `data/Tags.kt`
- Test: `data/db/EntryDaoTest.kt`, `data/DatesTest.kt`, `data/TagsTest.kt` (Robolectric for DAO)

**Interfaces:**
- Produces:
  - `enum class EntryType { PHOTO, TEXT }`, `enum class DateSource { CAMERA, EXIF, FALLBACK_TODAY, MANUAL }`
  - `@Entity("entries") data class EntryEntity(id: String, type: EntryType, text: String?, photoFile: String?, thumbFile: String?, takenAt: Long, localDate: String, dateSource: DateSource, createdAt: Long, updatedAt: Long, deletedAt: Long?, backedUpAt: Long?)` with indexes on `localDate`, `takenAt`, `(deletedAt, backedUpAt)`
  - `@Entity("tags") data class TagEntity(id: String, name: String /*unique*/, useCount: Int, lastUsedAt: Long)`; `@Entity("entry_tags") data class EntryTagCrossRef(entryId: String, tagId: String)` with cascade FKs
  - `data class EntryWithTags(entry: EntryEntity, tags: List<TagEntity>)`
  - `EntryDao`: `upsert(e)`, `getById(id): EntryWithTags?`, `observeMonth(prefix: String /*"YYYY-MM"*/): Flow<List<EntryWithTags>>` (non-deleted), `observeDay(localDate: String): Flow<List<EntryWithTags>>` (ordered by takenAt), `needsBackup(): List<EntryEntity>` (`backedUpAt IS NULL OR backedUpAt < updatedAt`, includes deleted), `markBackedUp(id, at)`, `purge(id)`, `countToday(localDate): Int`
  - `TagDao`: `findByName(name)`, `insert(tag)`, `setTagsFor(entryId, tagIds)`, `orderedForPicker(selected: Set<String>): List<TagEntity>` (selected first, then useCount desc, then lastUsedAt desc), `search(prefix): List<TagEntity>`, `tagsUsedInMonth(prefix): List<TagEntity>`
  - `fun localDateOf(epochMillis: Long, zone: ZoneId): String` (`"YYYY-MM-DD"`); `fun exifDateToMillis(exifValue: String?, zone: ZoneId): Long?` (format `yyyy:MM:dd HH:mm:ss`)
  - `fun normalizeTag(raw: String): String?` (trim, lowercase; null when empty or longer than 24)

- [ ] **Step 1: Write failing tests**
  - `DatesTest.lateNightStaysOnDay()`: `localDateOf(<2026-10-25T23:59 IST millis>, ZoneId.of("Asia/Kolkata")) == "2026-10-25"`; the stored string is unchanged when later read with `ZoneOffset.UTC` (assert repository never recomputes; covered in Task 4).
  - `DatesTest.exifParsing()`: `"2026:10:23 19:05:00"` in IST -> millis of 2026-10-23T19:05+05:30; `null`, `""`, `"0000:00:00 00:00:00"` -> `null`.
  - `TagsTest.normalisation()`: `"  Gym "`->`"gym"`, `"GYM"`->`"gym"`, `"a".repeat(30)`->`null`, `""`->`null`, `"badminton"`->`"badminton"`.
  - `EntryDaoTest.monthExcludesDeleted()`, `dayOrderedByTime()`, `needsBackupIncludesTombstones()`, `pickerOrdering()` (selected `cricket` first, then `gym` useCount 9, then `office` useCount 4).
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*data*'`. Expected: FAIL.
- [ ] **Step 3:** Implement entities, DAOs, database (version 1, `exportSchema = true` to `app/schemas`), Dates.kt, Tags.kt.
- [ ] **Step 4:** Re-run. Expected: PASS.
- [ ] **Step 5:** Commit `feat: room schema, date and tag rules`.

### Task 3: Photo store

**Files:**
- Create: `data/PhotoStore.kt`
- Test: `data/PhotoStoreTest.kt` (Robolectric), fixtures `app/src/test/resources/rotated_6.jpg` (EXIF orientation 6, GPS tags), `wide.jpg` (4000x3000)

**Interfaces:**
- Produces: `class PhotoStore(filesDir: File)` with
  - `suspend fun save(id: String, source: InputStream): StoredPhoto` -> `data class StoredPhoto(photoFile: String, thumbFile: String, exifTakenAt: String?)` (relative paths `photos/<id>.jpg`, `thumbs/<id>.jpg`; EXIF date read before stripping)
  - `fun delete(id: String)`, `fun file(relative: String): File`
  - `fun cleanup(knownIds: Set<String>): Int` (removes `*.tmp` and files whose id is unknown; returns count)
  - `fun freeBytes(): Long`

- [ ] **Step 1: Write failing tests:** `savesUpright()` (rotated_6 saved width/height swapped vs raw, orientation tag normal); `resizesToMax2560()` (wide.jpg -> 2560x1920; thumb long edge 480); `stripsGps()` (no `TAG_GPS_LATITUDE` in output); `returnsExifDate()`; `cleanupRemovesOrphansAndTmp()`.
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*PhotoStoreTest*'`. Expected: FAIL.
- [ ] **Step 3:** Implement: decode with `inSampleSize`, rotate by EXIF, scale, compress JPEG 85 to `<name>.tmp`, `FileOutputStream.fd.sync()`, rename. Thumbnail from the scaled bitmap.
- [ ] **Step 4:** Re-run. Expected: PASS.
- [ ] **Step 5:** Commit `feat: photo store with resize, thumbnails, atomic writes`.

### Task 4: Journal repository

**Files:**
- Create: `data/JournalRepository.kt`; Modify: `AppContainer.kt` (add `db`, `photoStore`, `repository`)
- Test: `data/JournalRepositoryTest.kt`

**Interfaces:**
- Consumes: Task 2 DAOs and helpers, Task 3 `PhotoStore`.
- Produces: `class JournalRepository(db, photoStore, clock: Clock, zone: () -> ZoneId, onChanged: () -> Unit)`
  - `suspend fun savePhoto(source: InputStream, caption: String?, tags: List<String>, takenAt: Long?, dateSource: DateSource): String` (when `takenAt` null and source EXIF missing -> now + `FALLBACK_TODAY`; EXIF present and dateSource EXIF -> EXIF time)
  - `suspend fun saveText(text: String, tags: List<String>, takenAt: Long): String`
  - `suspend fun update(id: String, text: String?, tags: List<String>?, takenAt: Long?)` (date change -> `localDate` recomputed, `dateSource = MANUAL`)
  - `suspend fun delete(id: String)` (soft delete; files removed; never-backed-up rows purged immediately)
  - `fun observeMonth(yearMonth: YearMonth): Flow<List<EntryWithTags>>`, `fun observeDay(localDate: String): Flow<List<EntryWithTags>>`, `suspend fun get(id: String): EntryWithTags?`
  - `suspend fun pickerTags(selected: Set<String>): List<TagEntity>`, `suspend fun searchTags(query: String): List<TagEntity>`
  - `suspend fun startupCleanup(): Int`
  - `onChanged` is called after every write (backup scheduling hooks in later)
- Tag save rules: names pass through `normalizeTag`; unknown names are created; `useCount += 1`, `lastUsedAt = now`.

- [ ] **Step 1: Write failing tests:** `photoWithoutExifFallsBackToToday()`; `textSavesWithTags()` (`["Gym","gym "]` -> one tag `gym`, useCount 1); `zoneChangeKeepsDay()` (save at 23:59 IST, switch `zone` lambda to UTC, `observeDay("2026-10-25")` still returns it); `manualDateRecomputesDay()`; `deleteRemovesFilesKeepsTombstone()` (backed-up entry) and `deleteNeverBackedUpPurges()`; `onChangedFiresOnWrites()`; `lowStorageRejects()` (fake PhotoStore with `freeBytes() < 50 MB` throws `InsufficientStorageException`).
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*JournalRepositoryTest*'`. Expected: FAIL.
- [ ] **Step 3:** Implement; writes run in `db.withTransaction` after the photo file is in place.
- [ ] **Step 4:** Re-run. Expected: PASS.
- [ ] **Step 5:** Commit `feat: journal repository`.

### Task 5: Bento packer

**Files:**
- Create: `bento/BentoPacker.kt`
- Test: `bento/BentoPackerTest.kt`

**Interfaces:**
- Produces:
  - `data class DaySummary(localDate: String, photoCount: Int, textCount: Int, firstTextLength: Int, isToday: Boolean)`
  - `enum class TileSize(val cols: Int, val rows: Int) { SMALL(1,1), WIDE(2,1), LARGE(2,2) }`
  - `fun sizeFor(day: DaySummary): TileSize` — today LARGE; photoCount 1 SMALL, 2-3 WIDE, 4+ LARGE; text-only SMALL, WIDE when `firstTextLength > 60`
  - `data class PlacedTile(localDate: String, size: TileSize, col: Int, row: Int)`
  - `fun pack(days: List<DaySummary>, columns: Int = 3): List<PlacedTile>` — input sorted by date descending; greedy first-fit scanning row-major for the first free slot that fits; when a later SMALL tile fits a gap left earlier, it fills it (order may shift for SMALL tiles only)

- [ ] **Step 1: Write failing tests:** size rules table; `noOverlap()` and `withinColumns()` over fixtures of 1, 7, 14, 31 mixed days; `todayAtTopLeft()`; `gapFilledBySmall()` (sequence LARGE, WIDE, WIDE, SMALL -> no empty cell in rows 0-1); `emptyInputReturnsEmpty()`.
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*BentoPackerTest*'`. Expected: FAIL.
- [ ] **Step 3:** Implement with an occupancy `MutableList<BooleanArray>` grown per row.
- [ ] **Step 4:** Re-run. Expected: PASS.
- [ ] **Step 5:** Commit `feat: bento sizing and packing`.

### Task 6: Shared components: chips and tag row with search

**Files:**
- Create: `ui/components/Chips.kt`, `ui/components/TagRow.kt`
- Test: `ui/components/TagRowTest.kt` (Compose UI test under Robolectric)

**Interfaces:**
- Consumes: `JournalRepository.pickerTags`, `searchTags`.
- Produces:
  - `@Composable fun TagChip(label: String, selected: Boolean, onClick: () -> Unit, trailingClear: Boolean = false)`
  - `@Composable fun DateLabel(text: String, emphasised: Boolean)`
  - `@Composable fun TagRow(state: TagRowState, onToggle: (String) -> Unit, onQuery: (String) -> Unit, onCreate: (String) -> Unit, onOpenSearch: () -> Unit, onCloseSearch: () -> Unit)`
  - `data class TagRowState(tags: List<String>, selected: Set<String>, searching: Boolean, query: String, results: List<String>, canCreate: Boolean)`
- Behaviour: search icon first; `+ Create "<query>"` shown when `canCreate` (normalised query not in results); "Selected" row visible while searching.

- [ ] **Step 1: Write failing tests:** `selectedRendersFilled()`; `createOfferedOnlyWithoutExactMatch()` (query "ba" with result "badminton" -> create shown; query "badminton" -> not shown); `tapTogglesTag()`.
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*TagRowTest*'`. Expected: FAIL.
- [ ] **Step 3:** Implement per the "Final design" canvas (chip 32dp tall visual, 48dp touch target via `minimumInteractiveComponentSize`).
- [ ] **Step 4:** Re-run. Expected: PASS.
- [ ] **Step 5:** Commit `feat: chips and tag row with search`.

### Task 7: Capture, After the shot, Write

**Files:**
- Create: `ui/capture/*`, `ui/review/*`, `ui/write/*`; Modify: `MainActivity.kt` (routes `capture`, `review/{tempPath}`, `write`; launcher shortcuts `res/xml/shortcuts.xml` for "Take photo" and "Write")
- Test: `ui/review/ReviewViewModelTest.kt`, `ui/write/WriteViewModelTest.kt`

**Interfaces:**
- Consumes: Task 4 repository, Task 6 `TagRow`.
- Produces: `CaptureViewModel.todayCount: StateFlow<Int>`; `ReviewViewModel.save(caption: String, tags: Set<String>)`; `WriteViewModel.save(text: String, tags: Set<String>, takenAt: Long)`, `WriteViewModel.hasUnsavedText: Boolean`.
- CameraX: `PreviewView` in a 28dp rounded frame; capture to `cacheDir/capture-<uuid>.jpg`, then navigate to review; review Save calls `repository.savePhoto(..., dateSource = CAMERA, takenAt = now)` and deletes the temp file.
- Permission denied state shows "Camera access is off" + "Open settings" (`ACTION_APPLICATION_DETAILS_SETTINGS`); Gallery and Write remain enabled.
- Write: back with non-blank text shows dialog "Discard this entry?".

- [ ] **Step 1: Write failing tests:** `reviewSaveUsesCameraSourceAndCleansTemp()`; `writeBlankTextCannotSave()`; `writeUnsavedFlag()`.
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*Review*' --tests '*Write*'`. Expected: FAIL.
- [ ] **Step 3:** Implement screens and ViewModels.
- [ ] **Step 4:** Re-run tests. Expected: PASS.
- [ ] **Step 5:** On the emulator: install (`./gradlew installDebug`), take a photo, save with tag, write a text entry; capture screenshots with `adb exec-out screencap -p > shots/capture.png` (also review, write, permission-denied) and compare against the canvas.
- [ ] **Step 6:** Commit `feat: capture, review and write flows`.

### Task 8: Month, Day and Entry

**Files:**
- Create: `ui/month/MonthScreen.kt`, `MonthViewModel.kt`, `BentoGrid.kt`, `ui/day/*`, `ui/entry/*`
- Test: `ui/month/MonthViewModelTest.kt`, `ui/entry/EntryViewModelTest.kt`

**Interfaces:**
- Consumes: Task 4 repository, Task 5 `sizeFor`/`pack`, Task 6 chips.
- Produces:
  - `data class MonthUiState(yearMonth: YearMonth, daysLogged: Int, entryCount: Int, filterTags: List<String>, activeTag: String?, tiles: List<TileUi>, isEmpty: Boolean)`; `data class TileUi(placed: PlacedTile, thumbs: List<String>, firstText: String?, dimmed: Boolean, isToday: Boolean)`
  - `MonthViewModel.setTag(tag: String?)`, `MonthViewModel.showMonth(yearMonth: YearMonth)`
  - `EntryViewModel.updateText(String)`, `updateTags(Set<String>)`, `updateDate(Long)`, `delete()`
- Subtitle strings: "N days, M entries"; with tag "<tag> on N days this month"; empty "Nothing logged yet".
- Dimmed tiles alpha 0.2; positions computed from the unfiltered month.
- Navigation: tile -> `day/{localDate}`, day card -> `entry/{id}`; Capture button on Month and Day; month change by horizontal swipe.
- Delete dialog copy from Global Constraints.

- [ ] **Step 1: Write failing tests:** `filterDimsButKeepsLayout()` (gym on 22,18,16: those `dimmed=false`, others true, `placed` equal to unfiltered); `subtitleCounts()`; `emptyMonthState()`; `todayFlagged()`; `entryDateChangeMovesDay()`; `entryDeleteNavigatesBack()`.
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*Month*' --tests '*Entry*'`. Expected: FAIL.
- [ ] **Step 3:** Implement; `BentoGrid` is a custom `Layout` placing children at `col * cell + gaps`, cell height 110dp, gap 8dp, 12dp side gutter.
- [ ] **Step 4:** Re-run. Expected: PASS.
- [ ] **Step 5:** On the emulator with a debug-only seeding action (14 days of sample entries), screenshot Month, filtered Month, Day, Entry and the empty Month; compare against the canvas.
- [ ] **Step 6:** Commit `feat: month bento, day and entry screens`.

### Task 9: Gallery import

**Files:**
- Create: `ui/importer/ImportScreen.kt`, `ImportViewModel.kt`
- Test: `ui/importer/ImportViewModelTest.kt`

**Interfaces:**
- Consumes: Task 4 `savePhoto`, Task 6 `TagRow`.
- Produces: `data class ImportItem(uri: Uri, takenAt: Long, hasExifDate: Boolean, caption: String, error: String?)`; `ImportViewModel.load(uris: List<Uri>)`, `setDate(index, Long)`, `setCaption(index, String)`, `setSharedTags(Set<String>)`, `saveAll(): ImportResult(saved: Int, failed: List<Int>)`.
- Picker: `PickMultipleVisualMedia(maxItems = 20)`, images only. Unreadable item -> `error = "This photo couldn't be read"`, excluded from save.

- [ ] **Step 1: Write failing tests:** `exifDateUsed()`; `missingExifFallsBackToToday()` (`hasExifDate=false`); `sharedTagsAppliedToAll()`; `partialFailureKeepsOthers()`.
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*ImportViewModelTest*'`. Expected: FAIL.
- [ ] **Step 3:** Implement; titles "Add N photos", button "Add N entries", label "Tags for all N".
- [ ] **Step 4:** Re-run. Expected: PASS.
- [ ] **Step 5:** Emulator: import 3 photos (push fixtures with `adb push` + media scan), screenshot the import screen.
- [ ] **Step 6:** Commit `feat: gallery import`.

### Task 10: Lock, settings and reminder

**Files:**
- Create: `data/Settings.kt`, `ui/lock/LockScreen.kt`, `ui/lock/LockGate.kt`, `ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`, `reminder/ReminderWorker.kt`
- Test: `ui/lock/LockGateTest.kt`, `reminder/ReminderWorkerTest.kt`

**Interfaces:**
- Produces:
  - `class AppSettings(dataStore)`: `lockEnabled: Flow<Boolean>` (default true), `reminderEnabled` (default true), `reminderTime: LocalTime` (default 21:00), `backupUseMobileData` (default false)
  - `class LockGate(clock: Clock, timeoutMs: Long = 60_000)`: `fun onBackground()`, `fun shouldLock(): Boolean`, `fun unlocked()`
  - `ReminderScheduler.schedule(time: LocalTime)`; `ReminderWorker` posts "Anything from today?" opening Capture, skipped when `EntryDao.countToday(today) > 0`
- Lock: `BiometricPrompt` with `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`; gate applies on cold start and after 60 s in background, including launcher shortcuts.
- Settings screen rows per spec 4.9 (backup rows wired in Task 12); storage line "N entries, X MB on this phone".

- [ ] **Step 1: Write failing tests:** `locksAfterTimeout()` (59 s no, 61 s yes); `coldStartLocks()`; `reminderSkippedWhenLoggedToday()`; `reminderPostsWhenNothingToday()`.
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*Lock*' --tests '*Reminder*'`. Expected: FAIL.
- [ ] **Step 3:** Implement; request `POST_NOTIFICATIONS` when the reminder is first enabled.
- [ ] **Step 4:** Re-run. Expected: PASS.
- [ ] **Step 5:** Emulator: enrol a fingerprint (`adb -e emu finger touch 1`), verify lock after backgrounding; screenshot Lock and Settings.
- [ ] **Step 6:** Commit `feat: lock, settings and daily reminder`.

### Task 11: Backup crypto and credential storage

**Files:**
- Create: `backup/Crypto.kt`, `backup/CredentialStore.kt`, `res/raw/bip39_english.txt`
- Test: `backup/CryptoTest.kt`

**Interfaces:**
- Produces:
  - `object RecoveryPhrase { fun generate(random: SecureRandom): List<String> /*12 words*/; fun toEntropy(words: List<String>): ByteArray /*16 bytes, checksum verified, throws InvalidPhraseException*/ }`
  - `fun deriveMasterKey(entropy: ByteArray): SecretKey` (HKDF-SHA256, empty salt, info `"journal-backup-v1"`, 32 bytes)
  - `object Envelope { const val VERSION: Byte = 1; fun seal(key: SecretKey, plain: ByteArray): ByteArray; fun open(key: SecretKey, sealed: ByteArray): ByteArray /*throws on tamper or wrong key*/ }`
  - `fun keyCheck(key: SecretKey): ByteArray` (= `seal` of `"journal-key-check"`)
  - `class CredentialStore(context)`: `saveR2(R2Credentials)`, `loadR2(): R2Credentials?`, `saveMasterKey(SecretKey)`, `loadMasterKey(): SecretKey?`, `savePhrase(List<String>)`, `loadPhrase(): List<String>?`, `clear()`; all values encrypted with an Android Keystore AES key (alias `journal_local`)
  - `data class R2Credentials(accountId: String, accessKeyId: String, secretAccessKey: String, bucket: String)`

- [ ] **Step 1: Write failing tests:** `phraseRoundTrip()` (generate -> toEntropy -> same bytes); `badChecksumRejected()`; `hkdfKnownVector()` (RFC 5869 test case 1 adapted to SHA-256 output length 32); `sealOpenRoundTrip()`; `wrongKeyFails()`; `tamperFails()` (flip one ciphertext byte); `noncesDiffer()` (two seals of same data differ).
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*CryptoTest*'`. Expected: FAIL.
- [ ] **Step 3:** Implement with `javax.crypto` (`AES/GCM/NoPadding`, 128-bit tag) and `Mac("HmacSHA256")` for HKDF. CredentialStore uses DataStore + Keystore cipher (Robolectric-skipped; covered on device in Task 13).
- [ ] **Step 4:** Re-run. Expected: PASS.
- [ ] **Step 5:** Commit `feat: backup crypto and credential store`.

### Task 12: R2 client and backup runner

**Files:**
- Create: `backup/SigV4.kt`, `backup/R2Client.kt`, `backup/BackupCodec.kt`, `backup/BackupRunner.kt`, `backup/BackupWorker.kt`, `ui/settings/BackupSetupScreen.kt`; Modify: `AppContainer.kt` (repository `onChanged` -> `BackupWorker.scheduleSoon()`), `SettingsScreen.kt` (backup rows)
- Test: `backup/SigV4Test.kt`, `backup/R2ClientTest.kt` (MockWebServer), `backup/BackupRunnerTest.kt`

**Interfaces:**
- Consumes: Task 2 `needsBackup`/`markBackedUp`/`purge`, Task 3 `PhotoStore.file`, Task 11 `Envelope`, `CredentialStore`.
- Produces:
  - `object SigV4 { fun sign(request: Request, creds: R2Credentials, now: Instant, region: String = "auto", service: String = "s3"): Request }`
  - `class R2Client(http: OkHttpClient, creds: R2Credentials, endpoint: HttpUrl = "https://<accountId>.r2.cloudflarestorage.com")`: `put(key, bytes)`, `get(key): ByteArray?`, `delete(key)`, `list(prefix): List<String>` (handles continuation tokens); throws `BackupException(kind: BackupErrorKind)` with kinds `AUTH, BUCKET_NOT_FOUND, NETWORK, STORAGE_FULL, UNKNOWN`
  - `object BackupCodec { fun encode(e: EntryWithTags): ByteArray; fun decode(bytes: ByteArray): BackupEntry }`; `data class BackupEntry(id, type, text, takenAt, localDate, dateSource, tags: List<String>, updatedAt, deletedAt)`
  - `class BackupRunner(dao, photoStore, client, key, clock)`: `suspend fun runOnce(): BackupReport(uploaded: Int, deleted: Int, failed: Int, lastError: BackupErrorKind?)`; per entry: photo put (if live PHOTO) -> entry JSON put -> `markBackedUp`; tombstone: entry JSON put -> photo delete -> `purge`; ensures `meta.json.enc` exists with `keyCheck`
  - `BackupWorker.scheduleSoon()` (unique, 5-minute delay), `schedulePeriodic()` (6 h), constraints from Global Constraints
  - Setup flow: credentials form -> "Test connection" (put/get/delete `probe`) -> generate phrase -> show 12 words -> confirm by re-typing -> save.
  - Settings backup rows: status line, "Back up now" (`BackupWorker.runNow()`), "Use mobile data" toggle, "Show recovery phrase" (behind a BiometricPrompt), "Restore" (Task 13), "Disconnect" (`CredentialStore.clear()` + cancel work, after confirmation). Startup cleanup's count of entries with missing photo files is shown here when non-zero.

- [ ] **Step 1: Write failing tests:**
  - `SigV4Test.matchesAwsExample()` using the AWS SigV4 "get-vanilla" test-suite vector (fixed date `20150830T123600Z`, keys from the suite) -> expected `Authorization` header.
  - `R2ClientTest`: `putSendsSignedRequest()`, `listFollowsContinuation()`, `403MapsToAuth()`, `404BucketMapsToBucketNotFound()`.
  - `BackupRunnerTest`: `uploadsPhotoThenJsonThenMarks()`; `interruptedAfterPhotoRetriesWholeEntry()` (fake client fails the JSON put: entry not marked, next run uploads both and marks); `tombstoneDeletesPhotoAndPurges()`; `oneFailureDoesNotBlockOthers()`; `objectsAreEncrypted()` (uploaded bytes start with VERSION and don't contain caption plaintext); `objectNamesHaveNoContent()` (keys match `entries/<uuid>.json.enc` / `photos/<uuid>.jpg.enc`).
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*backup*'`. Expected: FAIL.
- [ ] **Step 3:** Implement. Status persisted in `AppSettings`: last success time, entry count, last error; Settings shows "Last backup <relative time>, N entries" or the error text; Month shows a one-line banner when the last success is older than 3 days and an error is set.
- [ ] **Step 4:** Re-run. Expected: PASS.
- [ ] **Step 5:** Commit `feat: encrypted R2 backup`.

### Task 13: Restore and end-to-end backup check

**Files:**
- Create: `backup/RestoreRunner.kt`, `ui/settings/RestoreScreen.kt`; Modify: first-launch empty Month to offer "Restore from backup"
- Test: `backup/RestoreRunnerTest.kt`

**Interfaces:**
- Consumes: Task 12 client/codec, Task 11 phrase + key check, Task 4 repository (insert path that bypasses `onChanged` and sets `backedUpAt = updatedAt`), Task 3 PhotoStore (thumbnail rebuild).
- Produces: `class RestoreRunner(...)`: `suspend fun verify(phrase: List<String>): SecretKey` (throws `WrongPhraseException` -> UI copy "Recovery phrase doesn't match this backup."); `fun restore(key: SecretKey): Flow<RestoreProgress(done: Int, total: Int)>`; merge rule: by id, newer `updatedAt` wins; tombstones skipped; after success the phrase and key are stored via `CredentialStore`.

- [ ] **Step 1: Write failing tests:** `wrongPhraseFailsBeforeDownloads()` (fake client records zero `entries/` gets); `restoresEntriesTagsAndPhotos()`; `skipsTombstones()`; `mergeNewerWins()`; `restoredEntriesNotReuploaded()` (`needsBackup()` empty after restore).
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*RestoreRunnerTest*'`. Expected: FAIL.
- [ ] **Step 3:** Implement RestoreRunner and RestoreScreen (progress bar, error states).
- [ ] **Step 4:** Re-run. Expected: PASS.
- [ ] **Step 5:** Real round trip: on emulator A set up backup against a test R2 bucket, create 5 photo + 3 text entries with tags, delete one, run "Back up now"; wipe app data (`adb shell pm clear com.personal.journal`), restore with the phrase; verify 7 entries, tags and photos match. Record the result in the commit message.
- [ ] **Step 6:** Commit `feat: restore from encrypted backup`.

### Task 14: Polish and screenshot review

**Files:**
- Modify: any screen files with issues found.
- Create: `docs/screenshots/` (captured images).

- [ ] **Step 1:** Run the full suite `./gradlew testDebugUnitTest lintDebug`. Expected: PASS, no lint errors.
- [ ] **Step 2:** On the emulator capture every screen (Capture, permission-off, After the shot, Tag search, Write, Import, Month, filtered Month, empty Month, Day, Entry, Delete dialog, Lock, Settings, Backup setup, Restore) at font scale 1.0 and 1.3 (`adb shell settings put system font_scale 1.3`, restore to 1.0 after).
- [ ] **Step 3:** Compare against the "Final design" canvas and Global Constraints (colors, radii, copy, 48dp targets); fix clipped text and mismatches; re-capture changed screens once.
- [ ] **Step 4:** Install on the owner's phone (`adb install -r app/build/outputs/apk/debug/app-debug.apk`) and walk through capture -> month -> backup.
- [ ] **Step 5:** Commit `chore: polish pass and screenshots`.
