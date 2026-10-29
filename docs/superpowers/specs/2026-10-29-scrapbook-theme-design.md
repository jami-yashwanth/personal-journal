# Scrapbook theme — design

Date: 2026-10-29
Status: approved direction, awaiting spec review
Mocks: `docs/design/scrapbook-theme/index.html` (light and dark, all screens). The plum exploration in `docs/design/plum-theme/` was rejected and is kept only for reference.

## Why

The app works but looks generic: neutral greys, cross-fades everywhere, nothing that feels like a personal scrapbook. The goal is a visual identity that makes looking back enjoyable (the product's week-2 retention goal) without slowing capture.

Reference: a colourful education-app dashboard. We took its warmth, chunky rounded shapes, black pill actions and dark dock. We did **not** take its stat widgets, illustrations or greetings, because the journal's content is photos and colour must not compete with them.

## Decisions (with the owner)

| Decision | Choice |
|---|---|
| Direction | Colour-blocked scrapbook: colour only where there is no photo |
| Month notes | Coloured slips (option A), not transparent text |
| Themes | Light and dark, following the system setting |
| Typeface | Outfit only. Geist and Caveat are removed |
| Action colour | Ink pill in light, cream pill in dark. No accent colour |
| Motion | Out of scope here; a separate motion pass follows this work |

## Visual system

### Colour tokens

| Token | Light | Dark | Use |
|---|---|---|---|
| `background` | `#FFF6E9` cream | `#191614` espresso | screen |
| `card` | `#FFFFFF` | `#24201C` | groups, circle buttons, chips |
| `sunk` | `#F4E9DA` | `#2B2520` | month tiles (kraft), empty slots |
| `line` | `#EBDFCD` | `#38312B` | 1.5dp borders, dividers |
| `text` | `#1E1B19` | `#F3ECE3` | primary text |
| `textSecondary` | `#5C554E` | `#C9BEB2` | body on cream |
| `textMuted` | `#8E857B` | `#968A7E` | meta lines |
| `action` | `#1E1B19` | `#F3ECE3` | buttons, dock, toggles, selected chip, today badge |
| `onAction` | `#FFFFFF` | `#191614` | content on `action` |
| `paper` | `#FFFFFF` | `#EFE8DE` | print borders, date and time stickers |
| `onPaper` | `#1E1B19` | `#1E1B19` | text on `paper` |
| `onColour` | `#1E1B19` | `#1E1B19` | text on any tag colour, both themes |
| `danger` | `#F07F86` | `#F07F86` | delete icon, lock block |

Tag colours are the **same in both themes**: teal `#2E8A78` (white text), orange `#F7954B`, lavender `#B8B3F6`, mustard `#F5B840`, sky `#8CC8F0`, sage `#A8D5A2`. Untagged notes use pink `#F9CAD5`. Coral is reserved for delete and the lock screen and is never a tag colour.

Rule: anything placed on a tag colour (text, circle buttons, pill buttons) uses the light-theme treatment in both themes (`onColour` text, `paper` circles, ink `#1E1B19` pills), so contrast never depends on the theme.

### Tag colour assignment

`tagColour(name)` is a pure function: a stable hash of the normalised tag name, taken modulo the 6 tag colours. No schema change, and the same tag is always the same colour on every screen and device. Two tags can share a colour; that is accepted. An entry with several tags takes the colour of its first tag in alphabetical order. No tag means pink.

Letting the owner pick a tag's colour is out of scope. The mocks' specific pairings (cricket teal, gym orange) are illustrative; the hash decides the real ones.

### Type

Outfit (variable font, OFL), bundled in `res/font`. Delete `geist.ttf` and `caveat.ttf` and add `licenses/Outfit-OFL.txt`. `handwriting()` and the `Caveat` family are removed, and their call sites use the body styles.

| Style | Size / weight | Use |
|---|---|---|
| display | 42sp / Bold, tracking −0.025em | month name, day title, Settings |
| entryText | 28–30sp / Medium | full-screen text entry, Write |
| caption | 20sp / Medium | photo caption in the viewer |
| body | 16sp / Medium | day notes |
| label | 15sp / SemiBold | buttons |
| meta | 14sp / Normal | counts, times |
| slip | 12–15sp / SemiBold | month note slips (size depends on the tile) |

### Shape and depth

Radii: tile and card 24dp, hero photo 40dp, print 7dp, pill fully round. Borders are 1.5dp `line` on cards, chips and circle buttons. No shadows and no gradients (in line with the owner's UI bar).

## Screens

Each item lists only what changes; everything not listed keeps today's behaviour.

- **Theme plumbing.** `JournalTheme` picks the light or dark palette from `isSystemInDarkTheme()` and provides it through a `CompositionLocal`. `JournalColors.X` becomes a `@Composable` getter, so the roughly 260 existing call sites across 18 files compile unchanged. Any use outside a composable (for example in `drawBehind` or `remember` lambdas) has to read the colour first; the compiler finds these. Status and nav bar icons follow the theme (`enableEdgeToEdge` with `SystemBarStyle.auto`).
- **Capture.** Viewfinder radius 36dp on `background`. Shutter: an `action` disc with an `onAction` core. Today pill: `card` with up to 3 thumbnails, where a text entry shows as a square in its tag colour.
- **Review.** Caption placeholder "Add a line about this…" in the caption style. Tag chips show a colour dot. Buttons: "Retake" (outline) and "Save to today" (action pill with an arrow circle).
- **Month.** Tile layout is unchanged: `BentoPacker` sizes, the `scatter()` positions, and the "+N" badge. The tile background becomes `sunk`, prints get a `paper` border, and notes become **slips**: a tag-coloured rounded rectangle (7dp radius) with `onColour` text, drawn above the prints and tilted by the spot's rotation. A slip shows as many lines as its tile has room for, at most 4 when it is the tile's only item and 3 otherwise. The date sticker is `paper`; today's is `action`. The settings button is a circle. Add about 100dp of bottom padding so the dock never covers the last row.
- **Month, empty.** Three tilted blank tiles (teal, mustard with "1", coral), "A fresh month.", "Every day you capture becomes a tile here. Busier days get bigger.", a "Take the first photo" action pill, and a "look back at {previous month}" link. No dock on this screen.
- **Dock (new, on Month and Day).** A floating `action` pill with gallery import, camera (larger, `onAction` circle) and write. Camera returns to Capture. Gallery opens the same `PickMultipleVisualMedia(20)` picker as Capture and continues into the existing Import flow. Write opens the Write screen. This replaces today's camera FAB.
- **Day.** The masonry is unchanged. Photo cards get a `paper` time sticker. Note cards fill with their tag colour and use `onColour` text, with the footer "13:15 · office".
- **Entry (photo).** It becomes a **horizontal pager over the day's entries**, opening on the tapped entry. The hero photo is 300×430dp with a 40dp radius, and the previous and next entries peek about 20dp at the screen edges. The meta line shows "{n} of {total}". The background is the photo tint (below), cross-faded as the page changes. Edit, date and delete move into the dock and act on the current page.
- **Entry (text).** The page fills with the entry's tag colour. A tag pill and the date sit above the text in the entryText style. The dock is the same as for a photo entry.
- **Write.** Text in the entryText style directly on the background, with no field box. The selected tag chip fills with its colour. Save is a full-width action pill, "Save to today".
- **Lock.** A coral block (40dp radius) containing "Your journal", "Touch the sensor to open it." and an `action` lock circle. Below it, an "Unlock" action pill.
- **Settings.** `card` groups with a 1.5dp border and `action` toggles. When backup is not set up, the backup group becomes a mustard warning block ("Not backed up", "Your {n} entries live only on this phone.") with an ink "Set up backup" pill. Backup Setup, Restore and Import screens are restyled with tokens only, with no layout changes.

## Photo tint

The tint is computed from the entry's existing thumbnail, with no new dependency:

1. Decode the thumbnail downsampled to 32×32 and average the pixels in HSL. Ignore pixels with lightness below 0.08 or above 0.95, so black borders and blown highlights don't win.
2. Light theme: hue as averaged, saturation clamped to at most 0.45, lightness fixed at 0.92. Dark theme: saturation at most 0.30, lightness fixed at 0.14.
3. The result is cached in memory per entry id. While it is computing, or if decoding fails, the screen shows `background`.

Because lightness is fixed, text contrast is guaranteed whatever the photo.

## States

- **Empty month:** as specified above.
- **Long text:** month slips clamp to the room available. Day notes show in full. The viewer caption clamps to 3 lines, and tapping it expands it over the photo. The full-screen text entry scrolls.
- **Slow or failed thumbnail:** prints and the hero photo show `sunk`, and the tint falls back to `background`.
- **Many tags:** the chip rows scroll horizontally, as they do today.
- **Dark mode with a bright text entry:** the full-screen lavender entry is kept as is, and checked on the device at night before release.

## Testing

- Unit tests: `tagColour` is stable for the same name and stays within the palette. The tint function returns fixed lightness for black, white, saturated and greyscale inputs. The existing `ScatterTest` and `MonthStateTest` still pass.
- Compose UI test: the dock's three actions navigate to the right routes, and the entry pager opens on the tapped entry and keeps edit and delete pointed at the visible page.
- Visual review on the emulator, required by the owner's UI bar: every screen in light and dark at 390dp width, plus the empty month, a long caption, a day with 6+ entries, and an entry whose thumbnail is missing. Screenshots go to `shots/scrapbook/` and are critiqued before the work is called done.

## Out of scope

The motion pass (shared elements, spatial transitions, press feedback) comes next as its own spec. Also out of scope: user-picked tag colours, stat widgets, illustrations, and an in-app theme switch.
