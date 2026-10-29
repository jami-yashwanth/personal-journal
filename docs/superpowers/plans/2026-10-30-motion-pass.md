# Motion Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make navigation continuous. Each route pair gets a spatial transition, Month tile → Day → Entry use shared elements, every press scales on a spring, and a saved photo shrinks into the camera's swipe-up handle.

**Architecture:** A pure `navMotion(from, to)` table drives `NavHost` transitions. A `SharedTransitionLayout` around the `NavHost` exposes its scopes through `CompositionLocal`s, so screens add `sharedTile`/`sharedPhoto` modifiers without new parameters. Press feedback is one `IndicationNodeFactory` provided via `LocalIndication`. The after-save shrink is a local overlay in `CaptureScreen`, fed through the Capture back-stack entry's `savedStateHandle`.

**Tech Stack:** Kotlin 2.4, Compose BOM 2026.09.00 (`SharedTransitionLayout`, `IndicationNodeFactory`), Navigation Compose 2.10.2, Coil 3.6.3, JUnit 4 + Robolectric 4.17.

**Spec:** `docs/superpowers/specs/2026-10-30-motion-pass-design.md`

## Global Constraints

- Base branch `scrapbook-theme`; the PR targets `scrapbook-theme`, not `main`.
- Springs, not tweens, for every new animation. Nav/bounds spring: `spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)`. Press spring: `spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)`.
- Pressed scale is exactly `0.96f`. Shared-route exit scale is `0.96f`. Push exit offset is `-width / 4`.
- Capture ↔ Month keeps the existing `sheet()` tween and slides unchanged.
- Haptics only on the shutter, Month `Tile`, and Day `PhotoTile`/`NoteTile`: `HapticFeedbackType.ContextClick`, fired in `onClick`.
- In `MonthScreen.kt`/`DayScreen.kt`, touch only tile container modifiers and click handlers, not colour code (a parallel session edits tag colours).
- Unit tests: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :app:testDebugUnitTest`.
- Device checks only on `emulator-5556`, never `emulator-5554`, at 390dp width, light and dark.
- Comments match the codebase: one-line KDoc or `//` stating why, no more.

## Review Focus

1. **Entry hero missing on the first frame.** `EntryScreen` returns a bare `Box` until `vm.entry` loads. If the hero isn't composed when the transition starts, the photo doesn't fly. Expected: the photo flies from the Day tile. Pinned by device check D2 in Task 7, with the fallback in Task 5 Step 6.
2. **Side peeks flying.** Pages other than the settled one must carry no shared key. Pinned by the `sharedKeyFor` test in Task 5.
3. **Press scale stuck after a cancelled press** (drag off a tile, scroll starts). Expected: it springs back to 1. Pinned by `pressReleasesOnCancel` in Task 2.
4. **`justSaved` replayed.** After rotation or returning to Capture from Month, the overlay must not play again. Pinned by `consumesOnce` in Task 6.
5. **Predictive back cancelled halfway** on Day/Entry. Expected: the screen is intact, the shared element is back in place, status bar icons are correct. Pinned by device check D4 in Task 7.

---

### Task 1: Transition table

**Files:**
- Create: `app/src/main/java/com/personal/journal/ui/motion/NavMotion.kt`
- Modify: `app/src/main/java/com/personal/journal/MainActivity.kt` (the `NavHost(...)` defaults and the `CAPTURE`/`MONTH` per-destination overrides)
- Test: `app/src/test/java/com/personal/journal/ui/motion/NavMotionTest.kt`

**Interfaces:**
- Produces: `enum class NavMotion { Shared, Push, Sheet }`, `fun navMotion(from: String?, to: String?): NavMotion`, and `fun <T> navSpring(): SpringSpec<T>` (the nav/bounds spring from Global Constraints), all in `com.personal.journal.ui.motion`.

- [ ] **Step 1: Write the failing test** `NavMotionTest` (plain JUnit, no Robolectric):
  - `captureAndMonthAreSheet`: `navMotion(Routes.CAPTURE, Routes.MONTH) == Sheet`, and the reverse too.
  - `monthDayAndDayEntryAreShared`: `(MONTH, DAY)`, `(DAY, MONTH)`, `(DAY, ENTRY)` and `(ENTRY, DAY)` are all `Shared`.
  - `everythingElseIsPush`: `(MONTH, SETTINGS)`, `(SETTINGS, BACKUP_SETUP)`, `(CAPTURE, REVIEW)`, `(CAPTURE, WRITE)`, `(DAY, WRITE)`, `(MONTH, ENTRY)` and `(CAPTURE, DAY)` are all `Push`.
  - `unknownRoutesArePush`: `navMotion(null, Routes.MONTH) == Push`, `navMotion("x", "y") == Push`.
- [ ] **Step 2: Run it.** Run `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :app:testDebugUnitTest --tests '*NavMotionTest'`. Expected: FAIL, unresolved `navMotion`.
- [ ] **Step 3: Implement `navMotion` and `navSpring`.** Match on the unordered route pair, using the `Routes` constants (route patterns such as `day/{date}`, as `destination.route` returns them).
- [ ] **Step 4: Wire it into `JournalNavHost`.**
  - Replace the global `enterTransition`, `exitTransition`, `popEnterTransition` and `popExitTransition` with lambdas that switch on `navMotion(initialState.destination.route, targetState.destination.route)`:
    - `Push`: forward enter `slideInHorizontally(navSpring()) { it }`; forward exit `slideOutHorizontally(navSpring()) { -it / 4 } + fadeOut(navSpring())`; pop mirrors these.
    - `Shared`: enter `fadeIn(navSpring())`; exit `fadeOut(navSpring()) + scaleOut(navSpring(), targetScale = 0.96f)`; pop enter `fadeIn + scaleIn(initialScale = 0.96f)`; pop exit `fadeOut`.
    - `Sheet`: `EnterTransition.None`/`ExitTransition.None` at the NavHost level, so the existing per-destination `CAPTURE`/`MONTH` overrides (kept verbatim) take over.
  - Swap the `else fadeIn()`/`else fadeOut()` branches in those overrides for the shared default lambdas, so leaving Capture for Review is a push.
- [ ] **Step 5: Run all unit tests.** Expected: PASS.
- [ ] **Step 6: Build and install, then check on device.**
  - Run `./gradlew :app:installDebug` and `adb -s emulator-5556 shell am start -n com.personal.journal/.MainActivity`.
  - Month → Settings slides in from the right. Back slides out. Capture ↔ Month is unchanged.
- [ ] **Step 7: Commit** `feat(motion): spatial push and pop between screens`.

### Task 2: Press feedback and haptics

**Files:**
- Create: `app/src/main/java/com/personal/journal/ui/motion/Press.kt`
- Modify: `ui/theme/Theme.kt` (`JournalTheme`), `ui/capture/CaptureScreen.kt` (shutter click), `ui/month/MonthScreen.kt` (`Tile` click), `ui/day/DayScreen.kt` (`PhotoTile`/`NoteTile` clicks)
- Test: `app/src/test/java/com/personal/journal/ui/motion/PressTest.kt`

**Interfaces:**
- Produces:
  - `object PressScaleIndication : IndicationNodeFactory`
  - `const val PressedScale = 0.96f`
  - `@Composable fun rememberTapHaptic(): () -> Unit`, which returns a lambda calling `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.ContextClick)`.

- [ ] **Step 1: Write failing Robolectric compose tests** in `PressTest`:
  - `themeProvidesPressScale`: inside `JournalTheme { }`, `LocalIndication.current` is `PressScaleIndication`.
  - `pressScalesDown`: a `Box(Modifier.testTag("b").size(100.dp).clickable {})` inside `JournalTheme`.
    - `performTouchInput { down(center) }`, then `mainClock.advanceTimeBy(500)`.
    - The node's `graphicsLayer` scale is read through a test-only `SemanticsPropertyKey` `PressScaleKey` that the node exposes. Assert `0.96f ± 0.01`.
  - `pressReleasesOnCancel`: `down(center)`, then `moveBy(Offset(0f, 400f))`, then `up()`, then advance 1000ms. Scale is `1f ± 0.01`. Use a parent `verticalScroll` so the drag cancels the press.
- [ ] **Step 2: Run it.** Run `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :app:testDebugUnitTest --tests '*PressTest'`. Expected: FAIL.
- [ ] **Step 3: Implement `PressScaleIndication`.**
  - It creates a `Modifier.Node` implementing `LayoutModifierNode` and `SemanticsModifierNode`. The node collects `interactionSource.interactions`, tracking `PressInteraction.Press` against `Release`/`Cancel`.
  - It animates an `Animatable(1f)` to `PressedScale`/`1f` with the press spring, places with `placeWithLayer { scaleX = v; scaleY = v }`, and invalidates placement on each frame.
  - It exposes `PressScaleKey` in semantics.
  - `equals`/`hashCode` are identity, since it's an object.
- [ ] **Step 4: Provide it in `JournalTheme`.** Inside `MaterialTheme(...)`'s content, wrap with `CompositionLocalProvider(LocalIndication provides PressScaleIndication)`. It must sit inside `MaterialTheme` because `MaterialTheme` sets the ripple.
- [ ] **Step 5: Add haptics.** At the shutter `onClick`, the Month `Tile` and the Day tiles, call `val haptic = rememberTapHaptic()`, then `onClick = { haptic(); open() }`.
- [ ] **Step 6: Run all unit tests.** Expected: PASS.
- [ ] **Step 7: Check on device.** Dock, chips, buttons and tiles all scale on press with no ripple.
- [ ] **Step 8: Commit** `feat(motion): press scale on every tap, haptics on shutter and tiles`.

### Task 3: Shared transition plumbing

**Files:**
- Create: `app/src/main/java/com/personal/journal/ui/motion/SharedScopes.kt`
- Modify: `MainActivity.kt` (wrap the `NavHost` and provide scopes in each `composable`)
- Test: `app/src/test/java/com/personal/journal/ui/motion/SharedKeysTest.kt`

**Interfaces:**
- Produces, in `com.personal.journal.ui.motion`:
  - `val LocalSharedScope: ProvidableCompositionLocal<SharedTransitionScope?>` (default `null`)
  - `val LocalNavScope: ProvidableCompositionLocal<AnimatedVisibilityScope?>` (default `null`)
  - `fun dayKey(date: String): String = "day-$date"`
  - `fun photoKey(entryId: String): String = "photo-$entryId"`
  - `fun noteKey(entryId: String): String = "note-$entryId"`
  - `@Composable fun Modifier.sharedTile(key: String, shape: Shape): Modifier`: `sharedBounds` with `resizeMode = RemeasureToBounds`, `clipInOverlayDuringTransition = OverlayClip(shape)` and `boundsTransform = { _, _ -> navSpring() }`. Returns `this` when either local is null.
  - `@Composable fun Modifier.sharedPhoto(key: String, shape: Shape): Modifier`: the same with `sharedElement`.
  - `@Composable fun NavScoped(scope: AnimatedVisibilityScope, content: @Composable () -> Unit)`, which provides `LocalNavScope`.

- [ ] **Step 1: Write the failing test** `SharedKeysTest`:
  - `keysAreDistinctPerKind`: `photoKey("a") != noteKey("a")`.
  - `dayKeyIsStable`: `dayKey("2026-10-30") == "day-2026-10-30"`.
  - `modifiersAreNoOpsWithoutScopes`: a Robolectric compose test. `Box(Modifier.testTag("t").size(10.dp).sharedTile("k", RectangleShape))` with no providers renders, and `onNodeWithTag("t").assertExists()`.
- [ ] **Step 2: Run it.** Expected: FAIL.
- [ ] **Step 3: Implement `SharedScopes.kt`.**
- [ ] **Step 4: Wrap the nav host.**
  - In `JournalNavHost`, wrap `NavHost` in `SharedTransitionLayout { CompositionLocalProvider(LocalSharedScope provides this) { NavHost(...) } }`.
  - Wrap each `composable` body's content in `NavScoped(this) { ... }`. The `this` inside `composable` is `AnimatedContentScope`.
- [ ] **Step 5: Run all unit tests and check on device.** Expected: PASS, and no visual change on device.
- [ ] **Step 6: Commit** `feat(motion): shared transition scopes around the nav host`.

### Task 4: Month tile grows into Day

**Files:**
- Modify: `ui/month/MonthScreen.kt` (`Tile`: modifier only), `MainActivity.kt` (`Routes.DAY` composable)

**Interfaces:**
- Consumes: `sharedTile`, `dayKey`, `JournalShapes.tile` (Task 3).

- [ ] **Step 1: Month source.** In `Tile`, insert `.sharedTile(dayKey(tile.placed.localDate), JournalShapes.tile)` after the dimming `graphicsLayer` and before `.clip(JournalShapes.tile)`.
- [ ] **Step 2: Day target.**
  - In the `Routes.DAY` composable, wrap `DayScreen(...)` in `Box(Modifier.fillMaxSize().sharedTile(dayKey(date), JournalShapes.tile).background(JournalColors.Background))`.
  - The wrapper exists before `DayViewModel` loads, and sits outside `DayScreen`'s `statusBarsPadding`.
- [ ] **Step 3: Build and check on device (light and dark).**
  - Tap a tile: the card grows to full screen and its corners square off. The collage fades out and the masonry fades in.
  - Back: the page shrinks into the same tile.
  - Filtered month: dimmed tiles still open.
  - Scroll the Month so a tile is half off-screen, open it and go back: it lands correctly.
- [ ] **Step 4: Run all unit tests.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(motion): month tile grows into its day`.

### Task 5: Day tile → Entry hero

**Files:**
- Create: `app/src/main/java/com/personal/journal/ui/motion/ImageKeys.kt`
- Modify: `AppContainer.kt` (one `val imageKeys = ImageKeys()`), `ui/day/DayScreen.kt` (`PhotoTile`/`NoteTile` modifiers, `onSuccess`), `ui/entry/EntryScreen.kt` (`PhotoPage` hero, `TextPage` card, pager), `MainActivity.kt` (pass `container.imageKeys` to Day and Entry)
- Test: `app/src/test/java/com/personal/journal/ui/motion/ImageKeysTest.kt`, `app/src/test/java/com/personal/journal/ui/motion/SharedKeysTest.kt`

**Interfaces:**
- Consumes: `sharedPhoto`, `sharedTile`, `photoKey`, `noteKey` (Task 3).
- Produces:
  - `class ImageKeys { fun put(entryId: String, key: MemoryCache.Key); fun get(entryId: String): MemoryCache.Key? }`, backed by a thread-safe map capped at 200 entries (LRU, via `LinkedHashMap(accessOrder = true)` under `synchronized`).
  - `fun sharedKeyFor(page: Int, settledPage: Int, key: String): String?`, which returns `key` only when `page == settledPage`.
  - New parameters: `DayScreen(…, imageKeys: ImageKeys)` and `EntryScreen(…, imageKeys: ImageKeys)`.

- [ ] **Step 1: Write the failing tests.**
  - `ImageKeysTest`:
    - `putThenGet` returns the key.
    - `missingIsNull`.
    - `evictsOldestPast200`: put 201, and the first is null.
  - `SharedKeysTest.peeksHaveNoKey`:
    - `sharedKeyFor(1, 1, "k") == "k"`
    - `sharedKeyFor(0, 1, "k") == null`
    - `sharedKeyFor(2, 1, "k") == null`
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement `ImageKeys` and `sharedKeyFor`** in `ImageKeys.kt` and `SharedScopes.kt`.
- [ ] **Step 4: Day sources.**
  - The `PhotoTile` `AsyncImage` gets `Modifier.sharedPhoto(photoKey(id), JournalShapes.card)` and `onSuccess = { imageKeys.put(id, it.result.memoryCacheKey ?: return@AsyncImage) }`. Thread `id` into `PhotoTile`.
  - The `NoteTile` `Column` gets `.sharedTile(noteKey(id), JournalShapes.card)` before `.clip`.
- [ ] **Step 5: Entry targets.**
  - Pass `pager.settledPage` and `page` into `PhotoPage`/`TextPage`.
  - The hero `Box` uses `sharedKeyFor(page, settled, photoKey(id))?.let { Modifier.sharedPhoto(it, JournalShapes.hero) } ?: Modifier`, before `.clip(JournalShapes.hero)`. `TextPage` does the same with `sharedTile(noteKey(id), JournalShapes.hero)`.
  - The hero `AsyncImage` takes an `ImageRequest.Builder(LocalContext.current).data(file).placeholderMemoryCacheKey(imageKeys.get(id)).build()`.
- [ ] **Step 6: Check on device (light and dark): D2.**
  - Day photo → Entry: the photo flies with no grey flash, and back reverses it. Repeat for a note.
  - If the photo doesn't fly on the first open (the first frame is the empty `Box`), apply the spec's fallback:
    - In `EntryScreen`, before `e` loads, render a hero-shaped `Box` carrying `sharedPhoto(photoKey(vm.id), JournalShapes.hero)`, at the same layout position.
    - Expose `val id` on `EntryViewModel`.
    - Then recheck.
- [ ] **Step 7: Check swipe then back.** Swipe to page 2, then go back. It flies to that tile if it's visible, otherwise fades. Delete the last entry of a day: plain fade, no crash.
- [ ] **Step 8: Run all unit tests.** Expected: PASS.
- [ ] **Step 9: Commit** `feat(motion): day photo becomes the entry hero`.

### Task 6: Saved photo shrinks into the handle

**Files:**
- Create: `app/src/main/java/com/personal/journal/ui/motion/OneShot.kt`
- Modify: `MainActivity.kt` (`REVIEW`/`CAPTURE` composables; `ReviewScreen` is unchanged because the `REVIEW` composable already has `path`), `ui/capture/CaptureScreen.kt` (overlay, handle bounds)
- Test: `app/src/test/java/com/personal/journal/ui/motion/OneShotTest.kt`

**Interfaces:**
- Produces:
  - `const val JustSaved = "justSaved"`
  - `fun SavedStateHandle.consumeOnce(key: String): String?`, which reads the value and removes it.
  - `CaptureScreen(…, justSaved: File?)`

- [ ] **Step 1: Write the failing test** `OneShotTest.consumesOnce`:
  - `SavedStateHandle(mapOf(JustSaved to "/p.jpg"))`.
  - The first `consumeOnce(JustSaved) == "/p.jpg"`, and the second is `null`.
- [ ] **Step 2: Run it.** Expected: FAIL.
- [ ] **Step 3: Implement `consumeOnce`.**
- [ ] **Step 4: Hand the path over.**
  - In `MainActivity` `REVIEW`, set `onDone = { nav.getBackStackEntry(Routes.CAPTURE).savedStateHandle[JustSaved] = path; nav.popBackStack(Routes.CAPTURE, false) }`. `ReviewScreen` is unchanged.
  - In `CAPTURE`, `val justSaved = remember(entry) { entry.savedStateHandle.consumeOnce(JustSaved)?.let(::File) }`. Use `rememberSaveable` so rotation doesn't replay it: consume into saveable state, then clear it after the animation ends.
- [ ] **Step 5: Build the overlay** in `CaptureScreen`.
  - Record the viewfinder `Box` bounds and the handle bounds with `onGloballyPositioned` (root coordinates).
  - When `justSaved != null` and both bounds are known, draw a top-level `AsyncImage(justSaved)` with `ContentScale.Crop`.
  - It animates one `Animatable(0f → 1f, navSpring())` progress. Offset and size `lerp` from the viewfinder rect to the handle rect. The corner radius `lerp`s from `ViewfinderShape`'s radius to 20.dp. Alpha is `1` until progress 0.8, then fades to 0.
  - On finish, the handle bumps (`Animatable` scale `1 → 1.12 → 1` on the press spring) and the state clears.
  - The overlay has no `pointerInput`, so touches pass through.
  - If the handle bounds are unknown, skip the overlay.
- [ ] **Step 6: Run all unit tests.** Expected: PASS.
- [ ] **Step 7: Check on device: D8.**
  - Shoot → Save: the photo shrinks into the handle, the handle bumps, and the shutter works immediately.
  - Rotate after saving: no replay.
  - Save, then swipe to Month and back: no replay.
- [ ] **Step 8: Commit** `feat(motion): saved photo shrinks into the journal handle`.

### Task 7: Device pass and PR

- [ ] **Step 1: Run all unit tests.** Run `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :app:testDebugUnitTest`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 2: Set up the emulator.** Run `adb -s emulator-5556 shell wm size 1080x2340` and `adb -s emulator-5556 shell wm density 440` (≈392dp wide). Install with the seed extra: `adb -s emulator-5556 shell am start -n com.personal.journal/.MainActivity --ez seed true`.
- [ ] **Step 3: Run the spec's device checks D1–D8 in light, then dark.** Switch with `adb -s emulator-5556 shell cmd uimode night yes|no`. For D4, start a back swipe from the left edge on Day and Entry and release halfway. Record D1, D2 and D8 with `adb -s emulator-5556 shell screenrecord`. Save the recordings and screenshots under `docs/design/motion/`.
- [ ] **Step 4: Reset the emulator.** Run `wm size reset` and `wm density reset`.
- [ ] **Step 5: Open the PR.** `gh pr create --base scrapbook-theme`. Write the description from the spec decisions, list known gaps (off-screen tile on back fades, no Write after-save motion), and link the recordings.
