# Scrapbook Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Re-theme the journal as a light/dark "scrapbook": cream or espresso surfaces, Outfit type, tag-coloured notes, a dock on Month and Day, and a tinted entry pager.

**Architecture:** One `JournalPalette` per theme, provided through a `CompositionLocal`. `JournalColors` keeps its current property names as `@Composable` getters, so existing call sites compile and each screen task migrates its own file. Pure functions (`tagColour`, `photoTint`, `slipMaxLines`) carry the logic and are unit-tested. Screens only consume them.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2026.09.00), Navigation 2.10.2, Coil 3, Robolectric + compose-ui-test for JVM tests.

**Spec:** `docs/superpowers/specs/2026-10-29-scrapbook-theme-design.md`. Mocks: `docs/design/scrapbook-theme/index.html`.

Run all JVM tests with `./gradlew :app:testDebugUnitTest`, and a single class with `./gradlew :app:testDebugUnitTest --tests '<fqcn>'`.

## Global Constraints

- Light and dark follow `isSystemInDarkTheme()`. There is no in-app switch.
- Token values exactly as in the spec's colour table. Tag colours: teal `#2E8A78` (white text), orange `#F7954B`, lavender `#B8B3F6`, mustard `#F5B840`, sky `#8CC8F0`, sage `#A8D5A2`. Untagged: pink `#F9CAD5`. Danger/lock: coral `#F07F86`.
- Anything on a tag colour uses `onColour` `#1E1B19` text, `paper` circles and `#1E1B19` pills, in both themes.
- Outfit is the only typeface. `geist.ttf`, `caveat.ttf`, `Caveat` and `handwriting()` are deleted.
- No shadows and no gradients. Borders are 1.5dp `line`. Radii: tile/card 24dp, hero 40dp, print 7dp, pills fully round.
- No new dependencies apart from the Outfit font file.
- Copy is fixed by the spec: "Add a line about this…", "Save to today", "Retake", "A fresh month.", "Every day you capture becomes a tile here. Busier days get bigger.", "Take the first photo", "Your journal", "Touch the sensor to open it.", "Unlock", "Not backed up", "Your {n} entries live only on this phone.", "Set up backup".
- Motion is out of scope. Keep today's navigation transitions.

## Review Focus

1. A photo whose thumbnail is missing or undecodable. Expect: the entry still opens, with the background colour and a `sunk` placeholder (Task 6 test `nullWhenNoUsablePixels`, Task 7 test `missingThumbnailFallsBack`).
2. Deleting the only, or the last, entry of a day from the pager. Expect: a return to Day (or Month if the day is now empty), with no crash and no stale page (Task 7 test `deletingLastEntryEmptiesDay`).
3. A very long note on a one-row month tile. Expect: at least one readable line, never text cut at the top (Task 5 test `shortTileStillShowsOneLine`).
4. An entry with several tags, or a tag renamed by case or whitespace. Expect: the same colour every time, the first tag alphabetically, using the normalised name (Task 3 tests `sameNameSameColour`, `firstTagAlphabetically`).
5. Switching the system theme while the app is open. Expect: everything recolours at once, including tinted entry backgrounds (Task 1 test `darkThemeProvidesDarkPalette`, Task 6 test `darkAndLightTintsDiffer`).

---

### Task 1: Palettes and system theme switching

**Files:**
- Create: `app/src/main/java/com/personal/journal/ui/theme/Palette.kt`
- Modify: `app/src/main/java/com/personal/journal/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/personal/journal/MainActivity.kt` (the `enableEdgeToEdge()` call at line 91)
- Test: `app/src/test/java/com/personal/journal/ui/theme/ThemeTest.kt` (replace its contents)

**Interfaces:**
- Produces: `data class JournalPalette(background, card, sunk, line, text, textSecondary, textMuted, action, onAction, paper, onPaper, onColour, danger: Color, isDark: Boolean)`, `val LightPalette`, `val DarkPalette`, `val LocalJournalPalette`, and `fun JournalTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`.
- Produces: `object JournalColors` with `@Composable @ReadOnlyComposable` getters for every palette field (`Background`, `Card`, `Sunk`, `Line`, `TextPrimary`, `TextSecondary`, `TextMuted`, `Action`, `OnAction`, `Paper`, `OnPaper`, `OnColour`, `Danger`), plus **legacy aliases** that later tasks remove: `Surface`→card, `SurfaceRaised`→sunk, `PrimaryFill`→action, `OnPrimary`→onAction, `Hairline`→line, `DestructiveIcon`→danger, `DestructiveOutline`→line, `Scrim`→text at 0.55 alpha.
- `JournalShapes` gains `hero = RoundedCornerShape(40.dp)` and `print = RoundedCornerShape(7.dp)`, and `tile`/`card` become 24dp.

- [ ] **Step 1: Write the failing tests.** `paletteValuesMatchSpec` asserts `LightPalette.background == Color(0xFFFFF6E9)`, `DarkPalette.background == Color(0xFF191614)`, `LightPalette.action == Color(0xFF1E1B19)`, `DarkPalette.action == Color(0xFFF3ECE3)`, `LightPalette.sunk == Color(0xFFF4E9DA)`, `DarkPalette.paper == Color(0xFFEFE8DE)`, and `onColour == Color(0xFF1E1B19)` in both. `darkThemeProvidesDarkPalette` is a Robolectric `createComposeRule` test: inside `JournalTheme(darkTheme = true)` it reads `LocalJournalPalette.current` and asserts `isDark`. It does the same for `false`.
- [ ] **Step 2: Run.** `./gradlew :app:testDebugUnitTest --tests 'com.personal.journal.ui.theme.ThemeTest'`. Expected: FAIL (unresolved `LightPalette`).
- [ ] **Step 3: Implement** `Palette.kt` with the spec table values. `JournalTheme` provides the palette, and Material `colorScheme` maps from it (`lightColorScheme` or `darkColorScheme`). `JournalColors` getters read `LocalJournalPalette.current`.
- [ ] **Step 4: Fix compile errors.** Any use of `JournalColors.X` outside composition (a `drawBehind`, `remember` or non-composable function) must read the colour into a local inside the composable first. Run `./gradlew :app:compileDebugKotlin` until it is clean.
- [ ] **Step 5: System bars.** Change line 91 to `enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))`, using `android.graphics.Color`.
- [ ] **Step 6: Run the whole suite.** `./gradlew :app:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 7: Commit.** `git commit -m "feat(theme): light and dark scrapbook palettes following the system"`

### Task 2: Outfit typeface

**Files:**
- Add: `app/src/main/res/font/outfit.ttf` (the variable font, from `https://github.com/google/fonts/raw/main/ofl/outfit/Outfit%5Bwght%5D.ttf`) and `licenses/Outfit-OFL.txt` (the `OFL.txt` from the same folder)
- Delete: `app/src/main/res/font/geist.ttf`, `app/src/main/res/font/caveat.ttf`, `licenses/Caveat-OFL.txt`
- Modify: `app/src/main/java/com/personal/journal/ui/theme/Type.kt`, and the `handwriting(` call sites in `ui/month/MonthScreen.kt`, `ui/day/DayScreen.kt` and `ui/entry/EntryScreen.kt`
- Test: `app/src/test/java/com/personal/journal/ui/theme/TypeTest.kt`

**Interfaces:**
- Produces: `val Outfit: FontFamily` (Normal, Medium, SemiBold, Bold, with variation settings like today's `geist()`), and the `JournalTypography` slots: `displayLarge` = display 42sp Bold, letterSpacing −0.025em, line 1.0; `headlineLarge` = entryText 28sp Medium, line 1.35; `titleLarge` = caption 20sp Medium, line 1.4; `bodyLarge` = body 16sp Medium, line 1.38; `labelLarge` = label 15sp SemiBold; `bodySmall` = meta 14sp Normal. Keep the existing slot names that screens already use, mapped to the nearest of these.
- Produces: `fun slipStyle(size: TextUnit): TextStyle` (Outfit SemiBold, line 1.2), used by Task 5.

- [ ] **Step 1: Failing test.** `displayIsOutfitBold42`: `JournalTypography.displayLarge.fontSize == 42.sp`, `fontWeight == FontWeight.Bold`, `fontFamily == Outfit`. `slipStyleScales`: `slipStyle(13.sp).fontSize == 13.sp` and its line height is `13.sp * 1.2f`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.** Add the font files, rewrite `Type.kt`, delete `Caveat` and `handwriting`, and replace each call site with `MaterialTheme.typography.bodyLarge`. Task 5 restyles month notes properly, so a plain style is fine here.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(theme): Outfit replaces Geist and Caveat"`

### Task 3: Tag colours

**Files:**
- Create: `app/src/main/java/com/personal/journal/ui/theme/TagColours.kt`
- Test: `app/src/test/java/com/personal/journal/ui/theme/TagColoursTest.kt`

**Interfaces:**
- Produces: `data class TagColour(val fill: Color, val onFill: Color)`, `val TagPalette: List<TagColour>` (the 6 colours in spec order; teal's `onFill` is white, the rest are `#1E1B19`), `val Untagged: TagColour` (pink), `fun tagColour(name: String): TagColour`, and `fun entryColour(tagNames: List<String>): TagColour`.

- [ ] **Step 1: Failing tests.** `sameNameSameColour`: `tagColour("gym") == tagColour("gym")`, and `tagColour(" Gym ") == tagColour("gym")` (normalised through `normalizeTag`). `alwaysInPalette`: for 200 generated names, `tagColour(n) in TagPalette`. `firstTagAlphabetically`: `entryColour(listOf("office", "cricket")) == tagColour("cricket")`. `noTagsIsPink`: `entryColour(emptyList()) == Untagged`. `tealHasWhiteText`: the `TagPalette` entry whose fill is `Color(0xFF2E8A78)` has `onFill == Color.White`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.** Index is `normalizeTag(name) ?: name`, then `.hashCode().mod(TagPalette.size)`. `String.hashCode` is specified by the JVM, so it is stable across runs and devices.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(theme): stable colour per tag"`

### Task 4: Shared components and the dock

**Files:**
- Modify: `ui/components/Buttons.kt`, `ui/components/Chips.kt`, `ui/components/Fields.kt`, `ui/components/TagRow.kt`, `ui/components/DateTimePicker.kt` (tokens only in the last three)
- Create: `ui/components/Dock.kt`
- Test: `app/src/test/java/com/personal/journal/ui/components/DockTest.kt`

All paths are under `app/src/main/java/com/personal/journal/`.

**Interfaces:**
- `PrimaryButton(label, onClick, modifier, enabled, height = 56.dp, arrow: Boolean = false)`: an `action` pill. With `arrow`, label left plus a 36dp `onAction` circle holding an arrow on the right. `onColour: Boolean = false` forces `#1E1B19`/white (used on tag colours).
- `SoftButton`: a `card` pill with a 1.5dp `line` border. `CircleIconButton`: a 46dp `card` circle with a `line` border, plus an `onColour: Boolean = false` variant using `paper`.
- `TagChip(name, selected, …)`: unselected is `card` with a border and a 9dp `tagColour(name).fill` dot. Selected is filled with `tagColour(name).fill` and its `onFill` text. The "All" chip selected is `action`/`onAction`.
- `DateLabel(text, emphasised)`: a `paper`/`onPaper` sticker, and `action`/`onAction` when emphasised.
- Produces: `data class DockAction(val icon: ImageVector, val label: String, val onClick: () -> Unit, val primary: Boolean = false, val danger: Boolean = false)` and `@Composable fun Dock(actions: List<DockAction>, modifier: Modifier = Modifier)`: a 68dp-high `action` pill with 52dp action slots. The primary slot is a 56dp `onAction` circle with `action` icon colour. Danger icons use `danger`. Each slot sets `contentDescription = label`.
- Produces: `val DockClearance = 100.dp` (bottom padding for scrolling content under a dock).

- [ ] **Step 1: Failing test.** `dockActionsAreLabelledAndClickable` (Robolectric compose): render `Dock` with "Import photos", "Take a photo" (primary) and "Write"; `onNodeWithContentDescription("Write").performClick()` flips a flag; all three nodes exist.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement** the components above. Remove the uses of the legacy aliases inside these files.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest`. Expected: PASS, including the existing `TagRowTest`.
- [ ] **Step 5: Commit.** `git commit -m "feat(ui): scrapbook buttons, chips and dock"`

### Task 5: Month: slips, kraft tiles, empty state, dock

**Files:**
- Modify: `ui/month/MonthState.kt` (`TileItem.Note`, `interleave`), `ui/month/MonthScreen.kt`, `MainActivity.kt` (the Month route's lambdas)
- Create: `ui/month/Slip.kt` (`slipMaxLines`)
- Test: `app/src/test/java/com/personal/journal/ui/month/MonthStateTest.kt` (add cases) and `app/src/test/java/com/personal/journal/ui/month/SlipTest.kt`

**Interfaces:**
- Consumes: `entryColour` (Task 3), `slipStyle` (Task 2), `Dock`/`DockAction`/`DockClearance` (Task 4), and `JournalShapes.print`.
- Changes: `data class Note(val text: String, val tags: List<String>) : TileItem`. `interleave` passes `it.tags.map { t -> t.name }`.
- Produces: `fun slipMaxLines(availableHeightPx: Float, lineHeightPx: Float, verticalPaddingPx: Float, alone: Boolean): Int`, returning `min(if (alone) 4 else 3, max(1, floor((availableHeightPx - verticalPaddingPx) / lineHeightPx)))`.
- `MonthScreen` signature changes: replace `onCapture: () -> Unit` with `onCamera: () -> Unit`, and add `onImport: (List<Uri>) -> Unit`. In `MainActivity`, `onCamera = { nav.popBackStack(Routes.CAPTURE, inclusive = false) }` and `onImport = { container.pendingImport = it; nav.navigate(Routes.IMPORT) }` (the same as Capture's).

- [ ] **Step 1: Failing tests.** In `MonthStateTest`, `noteCarriesItsTags`: an entry with tags `office` and `cricket` produces `TileItem.Note(text, listOf("office", "cricket"))` (order as stored). In `SlipTest`: `aloneCapsAtFour` gives `slipMaxLines(1000f, 20f, 12f, alone = true) == 4`; `sharedCapsAtThree` gives `slipMaxLines(1000f, 20f, 12f, false) == 3`; `shortTileStillShowsOneLine` gives `slipMaxLines(25f, 20f, 12f, false) == 1`; `fitsWhatRoomAllows` gives `slipMaxLines(60f, 20f, 12f, false) == 2`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement the state change and `slipMaxLines`.**
- [ ] **Step 4: Restyle `Scrapbook()`.** The tile background is `JournalColors.Sunk`. Prints use `JournalShapes.print` with a `Paper` border (3dp sides, 8dp bottom). A note is a `Box` filled with `entryColour(item.tags).fill`, 7dp radius, padding 8dp × 6dp, text in `slipStyle(size)` and `onFill` colour, `maxLines = slipMaxLines(...)`, `TextOverflow.Ellipsis`, wrapping its content height and centred vertically on its spot, clamped inside the tile. Sizes: 15sp alone, 13sp on LARGE tiles, 12sp otherwise. Keep `widened(1.6f, max = 0.92f)` and the existing rotation.
- [ ] **Step 5: Restyle the header, chips (Task 4 `TagChip`), settings circle and empty state** (the spec's copy, three tilted 82×108dp tiles in teal, mustard with "1" and coral, a `PrimaryButton(arrow = true)` labelled "Take the first photo" calling `onCamera`, and "look back at {previous month}" calling `vm.previous()`). Replace the FAB with `Dock(listOf(DockAction(Import, "Import photos", launchPicker), DockAction(Camera, "Take a photo", onCamera, primary = true), DockAction(Write, "Write", onWrite)))`, hidden while the month is empty. The picker is `rememberLauncherForActivityResult(PickMultipleVisualMedia(20))` exactly as in `CaptureScreen.kt:141`. Add `DockClearance` bottom padding to the grid.
- [ ] **Step 6: Run** `./gradlew :app:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 7: Commit.** `git commit -m "feat(month): kraft tiles, tag-coloured slips and the dock"`

### Task 6: Photo tint

**Files:**
- Create: `ui/entry/PhotoTint.kt`
- Test: `app/src/test/java/com/personal/journal/ui/entry/PhotoTintTest.kt`

**Interfaces:**
- Produces: `fun photoTint(argbPixels: IntArray, dark: Boolean): Color?` (pure) and `class TintCache { suspend fun tint(entryId: String, thumb: File?, dark: Boolean): Color? }`. The cache decodes on `Dispatchers.IO` with `BitmapFactory.Options.inSampleSize` set so the long side is ≤ 64px, scales to 32×32, and memoises by `"$entryId-$dark"`. It returns null on a missing or undecodable file.
- Algorithm: convert each pixel to HSL and skip pixels with L < 0.08 or L > 0.95. If none remain, return null. Average the hue as a circular mean (average of the unit vectors) and the saturation as a plain mean. Output `Color.hsl(hue, min(s, if (dark) 0.30f else 0.45f), if (dark) 0.14f else 0.92f)`.

- [ ] **Step 1: Failing tests.** `lightnessIsFixed`: for a saturated red array, `photoTint(red, false)` has HSL lightness 0.92 ± 0.01 and `photoTint(red, true)` has 0.14 ± 0.01. `saturationClamped`: the red's saturation is ≤ 0.45 in light and ≤ 0.30 in dark. `hueFollowsPhoto`: a pure-blue array gives hue 240 ± 2. `circularHue`: an array that is half hue 350 and half hue 10 gives hue ≈ 0 (within 3), not 180. `nullWhenNoUsablePixels`: all black gives null, and all white gives null. `greyscaleIsNeutral`: mid-grey gives saturation 0. `darkAndLightTintsDiffer`: the same input gives different colours for `dark = true` and `false`. `missingThumbnailFallsBack` (Robolectric): `TintCache().tint("x", File("/nope.jpg"), false) == null`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement** `photoTint` and `TintCache`.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(entry): photo tint from the thumbnail"`

### Task 7: Entry pager

**Files:**
- Modify: `ui/entry/EntryViewModel.kt`, `ui/entry/EntryScreen.kt`, and the `MainActivity.kt` ENTRY route (provide one `TintCache` from `AppContainer`)
- Modify: `AppContainer.kt` (add `val tintCache = TintCache()`)
- Test: `app/src/test/java/com/personal/journal/ui/entry/EntryViewModelTest.kt` (add cases; existing ones must keep passing)

**Interfaces:**
- Consumes: `photoTint`/`TintCache` (Task 6), `entryColour` (Task 3), `Dock` (Task 4), `JournalShapes.hero`.
- `EntryViewModel(repository, id)` keeps its constructor. It adds `val day: StateFlow<List<EntryWithTags>>` (the opened entry's day from `repository.observeDay(localDate)`, in the same order as the Day screen), `val selected: StateFlow<Int>`, `val initialIndex: Int` (resolved in `load()`) and `fun select(index: Int)`. The existing `entry` flow becomes `day[selected]`. `startEdit`, `saveEdit`, `updateDate` and `delete` act on the selected entry. After `delete()`, `selected` is clamped to `day.size - 1`. Expose `val dayIsEmpty: StateFlow<Boolean>`.

- [ ] **Step 1: Failing tests.** `opensOnTappedEntry`: a day with entries A, B and C opened on B has `initialIndex == 1` and `entry.value.id == B`. `actionsFollowSelection`: after `select(2)`, `updateDate(m)` changes C, not B. `deletingLastEntryEmptiesDay`: a day with one entry, `delete()`, gives `dayIsEmpty.value == true` with no exception. `deleteKeepsIndexInRange`: delete C while it is selected and `selected == 1`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement the view model changes.**
- [ ] **Step 4: Screen.** `HorizontalPager(pageCount = day.size, initialPage = initialIndex)` with `contentPadding = PaddingValues(horizontal = 45.dp)` and `pageSpacing = 12.dp`, so neighbours peek about 20dp. It syncs `pagerState.settledPage` to `vm.select`. Header: back circle, "{n} of {total}", the day in display style, and the time in meta. A photo page shows a 300×430dp hero clipped to `JournalShapes.hero` (`Sunk` placeholder), with the caption in `titleLarge`, `maxLines = 3`, and a tap to expand to full. A text page fills with `entryColour(tags).fill`, shows a tag pill, the date and the text in `headlineLarge` (`onColour`), and scrolls. Background: `animateColorAsState` of the current page's tint (`tintCache.tint(id, thumb, palette.isDark) ?: Background`), or the text entry's tag colour. The dock holds Edit, Change date and Delete (danger), and keeps the existing edit and date flows. When `dayIsEmpty` becomes true, call `onBack()`.
- [ ] **Step 5: Run** `./gradlew :app:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 6: Commit.** `git commit -m "feat(entry): swipe through the day, tinted by each photo"`

### Task 8: Day screen

**Files:**
- Modify: `ui/day/DayScreen.kt` and the DAY route in `MainActivity.kt`

**Interfaces:**
- Consumes: `entryColour`, `Dock`, `DockClearance`, `DateLabel`.
- `DayScreen` replaces `onCapture` with `onCamera` and adds `onImport: (List<Uri>) -> Unit` and `onWrite: () -> Unit`, wired exactly as Month's in Task 5.

- [ ] **Step 1: Restyle.** Title in `displayLarge`, meta "{whenLabel} · {n} entries". Photo cards keep the masonry with a `paper` time sticker in the bottom-left. Note cards are filled with `entryColour(tags).fill`, with `bodyLarge` text in `onFill` and the footer "{time} · {first tag}" (time only if untagged). Replace the FAB with the same three-action `Dock`, and add `DockClearance` bottom padding.
- [ ] **Step 2: Build and run** `./gradlew :app:testDebugUnitTest`. Expected: PASS, including `PhotoAspectTest`.
- [ ] **Step 3: Commit.** `git commit -m "feat(day): tag-coloured notes and the dock"`

### Task 9: Capture, Review, Write

**Files:**
- Modify: `ui/capture/CaptureScreen.kt`, `ui/review/ReviewScreen.kt`, `ui/write/WriteScreen.kt`

- [ ] **Step 1: Capture.** Viewfinder clip is 36dp. The shutter is an 86dp `Action` disc with a 66dp `OnAction` core. The flash and flip buttons are `paper` circles over the camera. Gallery and write are `CircleIconButton`s. The handle keeps its behaviour with `TextMuted` colour.
- [ ] **Step 2: Review.** Photo clip is 36dp. The caption placeholder is "Add a line about this…" in `titleLarge` `TextMuted`. Tag chips come from Task 4. The bottom row is `SoftButton("Retake")` at weight 1 and `PrimaryButton("Save to today", arrow = true)` at weight 1.7.
- [ ] **Step 3: Write.** No field box: the text is `headlineLarge` on `Background` with an `Action`-coloured caret. The date chip sits in the top bar. Tag chips (a selected one fills with its colour) sit above a full-width `PrimaryButton("Save to today", arrow = true)`.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest`. Expected: PASS (`ReviewViewModelTest`, `WriteViewModelTest`, `SwipeUpToOpenTest`).
- [ ] **Step 5: Commit.** `git commit -m "feat(ui): scrapbook capture, review and write"`

### Task 10: Lock, Settings, Import, Backup, Restore, and removing the aliases

**Files:**
- Modify: `ui/lock/LockScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/BackupSetupScreen.kt`, `ui/settings/RestoreScreen.kt`, `ui/importer/ImportScreen.kt`, `ui/theme/Theme.kt` (delete the legacy aliases)

- [ ] **Step 1: Lock.** A 470dp coral (`Danger`) block with a 40dp radius and 28dp padding, holding "Your journal" (`displayLarge`, `onColour`), "Touch the sensor to open it." and an 84dp `#1E1B19` lock circle at the top right. Below it, `PrimaryButton("Unlock", arrow = true)`.
- [ ] **Step 2: Settings.** `Card` groups with 1.5dp `Line` borders and 24dp radius. Toggles are `Action` track and `OnAction` thumb when on, `Sunk` when off. When `!state.backupConnected`, the backup group is a mustard block with "Not backed up", "Your {n} entries live only on this phone." and `PrimaryButton("Set up backup", onColour = true)`. Otherwise it stays as the current status line. Backup Setup, Restore and Import change tokens only.
- [ ] **Step 3: Remove the legacy aliases** from `JournalColors`, then run `./gradlew :app:compileDebugKotlin` and migrate whatever still uses them.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest`. Expected: PASS (`BackupBannerTest`, `LockGateTest`).
- [ ] **Step 5: Commit.** `git commit -m "feat(ui): scrapbook lock and settings; drop legacy colour names"`

### Task 11: On-device visual review, both themes

**Files:**
- Create: `shots/scrapbook/` (screenshots)
- Modify: `DebugSeeder.kt` only if needed to seed the states below

- [ ] **Step 1: Install** with `./gradlew :app:installDebug` on `emulator-5554`, seeded by `DebugSeeder`.
- [ ] **Step 2: Capture each screen in light and dark** (`adb shell cmd uimode night yes|no`), at a 390dp-wide emulator: Capture, Review, Month, Month empty (a future month), Day, Entry photo, Entry text, Write, Lock, Settings (backup off), Import. Save each as `shots/scrapbook/<screen>-<light|dark>.png`.
- [ ] **Step 3: Capture the states.** A long caption (expanded and collapsed), a day with 6+ entries, a one-row tile with a long note, an entry with a deleted thumbnail file, switching the theme while the Entry screen is open, and the dark full-screen lavender text entry.
- [ ] **Step 4: Critique** every screenshot against the mocks and the spec. Fix the gaps, re-shoot, and list in the PR description anything deliberately left.
- [ ] **Step 5: Commit.** `git commit -m "chore: scrapbook theme screenshots"`
