# Motion pass — design

Date: 2026-10-30
Status: design approved in chat, awaiting spec review
Builds on: scrapbook theme (`scrapbook-theme`, PR #1). The motion PR targets `scrapbook-theme`, not `main`.

## Why

The owner's reference for "smooth and clear" is a fintech app whose smoothness comes from continuity: nothing appears from nowhere, the object you touch carries into the next screen, every press answers instantly, and colour only means something. The journal today cross-fades almost every navigation (`fadeIn()/fadeOut()` defaults in `JournalNavHost`), so screens swap rather than flow, and taps give no physical answer.

Success: opening a day, opening an entry, going back and saving a photo each read as one continuous motion; every tappable visibly reacts on press; nothing regresses capture speed.

## Decisions (with the owner)

| Decision | Choice |
|---|---|
| After "Save to today" | Stay camera-first. The saved photo shrinks into the camera's swipe-up handle. No navigation change |
| Month tile → Day | Container transform: the tile grows into the Day page; back shrinks it into the same tile |
| Day tile → Entry | Shared element: the photo becomes the Entry hero; a note card becomes the Entry text card |
| Other navigations | Horizontal push/pop |
| Capture ↔ Month | Unchanged vertical sheet |
| Press feedback | Scale to 0.96 on spring for every clickable; haptic only on shutter, Month tiles and Day tiles |
| Out of scope | Green full-screen success, swipe-to-confirm, prints scattering from tile into the Day grid, any after-save motion for Write |

## 1. Navigation transitions

`JournalNavHost` (`MainActivity.kt`) stops using a single global fade. A pure function picks the transition from the pair of routes:

```kotlin
enum class NavMotion { Shared, Push, Sheet }
fun navMotion(from: String?, to: String?): NavMotion
```

| From ↔ To | Motion | Enter (forward) | Exit (forward) | Pop reverses |
|---|---|---|---|---|
| Capture ↔ Month | `Sheet` | existing slide up | existing lift + fade | yes, unchanged |
| Month ↔ Day, Day ↔ Entry | `Shared` | fade in | fade out + scale to 0.96 | yes |
| anything else | `Push` | slide in from right (full width) | slide 1/4 left + fade | yes |

- Transitions use springs (`spring(stiffness = StiffnessMediumLow)`, no bounce) instead of tweens. The existing `sheet()` tween for Capture ↔ Month stays as-is so that pair doesn't change.
- Under `Shared`, background content only fades and scales; nothing slides sideways under a growing container.
- Predictive back is active (targetSdk 37, no opt-out). Navigation Compose seeks pop transitions from the back gesture, and shared bounds follow it. No extra code; it is on the device checklist.
- Route `ENTRY` → `ENTRY` doesn't occur (the pager stays inside one destination).

## 2. Shared elements

### Plumbing

- `SharedTransitionLayout` wraps the `NavHost`.
- A new file `ui/motion/SharedScopes.kt` provides `LocalSharedTransitionScope` and `LocalNavAnimatedScope` (`AnimatedVisibilityScope`) via `CompositionLocal`. Each `composable { }` block provides its own `AnimatedContentScope`.
- Helper modifiers in the same file, no-ops when either scope is absent (previews, tests):
  - `Modifier.sharedTile(key: String, shape: Shape)` → `sharedBounds` with `OverlayClip(shape)` and `resizeMode = RemeasureToBounds`
  - `Modifier.sharedPhoto(key: String, shape: Shape)` → `sharedElement` with `OverlayClip(shape)`
- Keys are built by pure functions, unit-tested: `dayKey(date) = "day-$date"`, `photoKey(entryId) = "photo-$entryId"`, `noteKey(entryId) = "note-$entryId"`.
- Bounds transform uses the same spring as §1.

### Month tile → Day (container transform)

- Source: the Month `Tile` outer `Box`, `sharedTile(dayKey(date), JournalShapes.tile)`, placed before `clip`. The `graphicsLayer` alpha for dimmed tiles stays outside it.
- Target: a wrapper `Box` around `DayScreen`, inside `composable(Routes.DAY)`. It exists before `DayViewModel` loads, so the target is present on the first frame. It is placed outside `statusBarsPadding`, so the full screen is the end bounds. Shape morphs from `JournalShapes.tile` to the full screen.
- The collage fades out and the masonry fades in (default `sharedBounds` enter/exit fades).
- Back: Month is activity-scoped (`monthVm`), so the tile exists when Day pops, and the page shrinks into it.
- Month is a vertically scrolling `Column`, so the tile is always composed (no lazy list) and the return target always exists. If it was scrolled off-screen, it animates from off-screen, which is acceptable.
- Month filtered by tag: dimmed tiles still carry keys, which is harmless.

### Day tile → Entry (shared element)

- Source:
  - `PhotoTile` image gets `sharedPhoto(photoKey(id), JournalShapes.card)`.
  - `NoteTile` column gets `sharedTile(noteKey(id), JournalShapes.card)`.
- Target: the Entry `PhotoPage` hero box gets `sharedPhoto(photoKey(id), JournalShapes.hero)`, and `TextPage` gets `sharedTile(noteKey(id), JournalShapes.hero)`.
- Only the settled pager page carries a key (`page == pager.settledPage`), so the 45dp side peeks don't fly.
- Swipe then back: the newly settled entry's key matches its Day tile. If that tile is scrolled out of the lazy staggered grid (not composed), there is no match and the plain `Shared` fade plays. This is accepted, not fixed this week.
- First-frame risk: `EntryScreen` returns an empty `Box` until `vm.entry` is non-null. The load is one Room read and usually beats the first frame. `publish()` sets `day` and `selected` before `entry`, so the pager opens on the tapped page.
  - If on device the hero misses the transition start, fall back to this: keep the shared element on a placeholder hero `Box` with the same key while loading. The route already carries the id, and Day passes nothing else.
- Coil: Day tiles and the Entry hero request different sizes, so their memory-cache keys differ. The hero's `ImageRequest` sets `placeholderMemoryCacheKey` to the Day tile's result key, so the already-decoded bitmap shows mid-flight instead of `Sunk` grey.
  - The Day tile records its key from `AsyncImage(onSuccess = …)` into a small in-memory map on `AppContainer`, keyed by entry id.
  - If the key isn't there (cold open), the hero shows `Sunk` and Coil's own crossfade.

## 3. Press feedback

- New `ui/motion/Press.kt`: `PressScaleIndication`, an `IndicationNodeFactory` that scales its node to 0.96 while pressed and springs back (`spring(dampingRatio = 0.6f, stiffness = StiffnessMedium)`).
- `JournalTheme` provides it through `LocalIndication`, so every existing `clickable`/`selectable` (dock slots, chips, buttons, circle icons, tiles, banner, links) gets it without call-site edits.
- Ripples go away. The scrapbook look has none by design.
- Haptics come from `LocalHapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)` at three call sites only: shutter, Month `Tile`, Day `PhotoTile`/`NoteTile`. Haptics happen on click, not on press-down.
- The system "reduce animations" setting: Compose scales animation durations with the animator duration scale. Springs collapse to instant at scale 0, which is the expected behaviour.

## 4. After save: photo shrinks into the handle

- `ReviewScreen.onDone` now receives the saved photo file. Before popping, `JournalNavHost` writes its path into the Capture entry's `savedStateHandle["justSaved"]`.
- `CaptureScreen` reads and clears `justSaved`. While set, a local overlay plays:
  - The photo, full-bleed over the viewfinder area, animates its bounds into the swipe-up handle's bounds (scale + translate + corner radius from 0 to the handle's pill), over a spring, while fading at the end.
  - The handle then does one bump (scale 1 → 1.12 → 1).
- The handle's bounds come from `onGloballyPositioned`. If they aren't known yet, the overlay is skipped.
- The overlay is not a nav shared element. Review is popped, and the camera re-binds behind the overlay, so the two don't interact.
- The overlay ignores touches (`pointerInput` not consumed), so the shutter is usable at once.
- Write saves have no equivalent and stay as they are.

## Error and edge states

| Case | Behaviour |
|---|---|
| Photo not yet decoded mid-flight | Placeholder from the Day tile's cache key; otherwise `Sunk` then crossfade |
| Entry swiped, Day tile off-screen on back | Plain shared fade, no flight |
| Entry deleted (pager pops when day empties) | Day tile gone, so no match; plain fade |
| Rotation / theme switch mid-transition | Transition is cancelled by recomposition; screens land in final state |
| Lock screen shown during transition | Journal stays composed under the lock; unaffected |
| Save fails | `onDone` isn't called, so no overlay; error shows in Review as today |
| Reduced motion (animator scale 0) | All transitions instant |

## Testing

Unit (`./gradlew :app:testDebugUnitTest`):
- `navMotion` covers every route pair in the table, including Capture ↔ Month and unknown routes falling back to `Push`.
- Key helpers: `dayKey`, `photoKey`, `noteKey` are distinct and stable.
- `justSaved` handoff: a small pure helper that consumes the value once (read then clear), tested for the single-shot behaviour.

Device (`emulator-5556`, never `emulator-5554`, 390dp width, light and dark):
1. Month tile → Day → back: grows and shrinks into the same tile, corners morph.
2. Day photo tile → Entry → back: photo carries both ways with no grey flash. Same for a note tile.
3. Entry: swipe to another page, back. It flies to that tile if visible, otherwise fades.
4. Predictive back: start a back swipe on Day and Entry, cancel halfway. The screen returns intact and status bar icons are correct.
5. Settings, Write, Import, Review: horizontal push and pop.
6. Capture ↔ Month: sheet unchanged.
7. Press feedback on dock, chips, buttons, tiles. Haptic on shutter and tiles.
8. Shoot → Save → the photo shrinks into the handle, the handle bumps, and the shutter is usable immediately.

Screenshots or short recordings of 1, 2 and 8 go in the PR.

## Shipping order

Each step is shippable on its own if a later one slips:
1. §1 transitions + §3 press feedback (small, low risk)
2. §2 shared elements (the bulk)
3. §4 after-save shrink

## Files touched

- `MainActivity.kt`: transition table, `SharedTransitionLayout`, scope providers, Day wrapper, `justSaved` handoff
- new `ui/motion/NavMotion.kt`, `ui/motion/SharedScopes.kt`, `ui/motion/Press.kt`
- `ui/theme/Theme.kt`: `LocalIndication`
- `ui/month/MonthScreen.kt`: tile key + haptic
- `ui/day/DayScreen.kt`: photo/note keys, cache key capture, haptic
- `ui/entry/EntryScreen.kt`: hero/text keys on the settled page, placeholder key
- `ui/review/ReviewScreen.kt`: `onDone(file)`
- `ui/capture/CaptureScreen.kt`: overlay, handle bounds, haptic on shutter
- `AppContainer.kt`: image cache key map

Overlap: the parallel session `assign-tag-colours-by-cr` may touch tile colour code in `MonthScreen.kt`/`DayScreen.kt`. The edits here are limited to modifiers on the tile containers and click handlers, not colour, to keep merges trivial.
