# Scrapbook theme screenshots (emulator-5556, 1170x2532 at 480dpi = 390dp wide, Oct 30 2026)

Data: DebugSeeder (launched once via `--ez seed true`; later launches without the extra), plus entries added on the device. Navigation was adb taps/swipes. Theme via `cmd uimode night yes|no` with the screen open.

- capture-light/dark.png: Capture screen, live camera, dock handle and gallery/shutter/write buttons. App start.
- review-light/dark.png: Review after tapping the shutter (photo, "Add a line about this...", tag row, Retake / Save to today).
- write-light.png / write-dark.png: Write screen, empty, keyboard open (tap pen button on Day dock).
- write-with-text-tag-light.png: Write with text "Called mum tonight." and the lavender tag "family" selected.
- month-light/dark.png: Month (October), 15 entries, tag filter chips, dock. Open journal handle from Capture.
- month-empty-light/dark.png: September ("Nothing logged yet / No entries this month."). Swipe right on Month. Future months cannot be reached (MonthViewModel.showMonth blocks them), so a past empty month stands in.
- month-long-note-light.png: Month, one-row tiles with long text notes truncated with an ellipsis (29 Oct "Slow Saturday...", 26 Oct "Interview...").
- month-missing-thumb-light.png: Month after deleting the newest thumb file (`run-as ... rm files/thumbs/ab7da829...jpg`) and force-stopping the app; today's tile shows the beige blank polaroid.
- day-light/dark.png: Day (today, 4 entries at that moment) from tapping the Today tile.
- day-many-top-light.png / day-many-bottom-light.png: Day with 7 entries, top and scrolled to the bottom. day-dark-7.png: the same 7-entry day in dark.
- day-missing-thumb-light.png: Day after the thumb was deleted; Day shows the full photo, so it still renders.
- entry-photo-light/dark.png: Entry pager, 2 of 4 (photo "First coffee before standup"), tapped from Day.
- entry-caption-collapsed-light.png: Entry 1 of 4 with a 6-line caption collapsed (3 lines + ellipsis, dock overlaps the last line).
- entry-caption-expanded-light.png: after tapping the caption; text runs under the dock. entry-caption-expanded-scrolled-light.png: scrolled to the end of the caption (all 7 lines visible).
- entry-text-light/dark.png: Entry 3 of 4, text-only "Lunch with Ravi..." (untagged, pink).
- entry-text-dark-lavender.png / entry-text-lavender-light.png: Entry 2 of 7, text "Called mum tonight." tagged family (lavender; "family".hashCode mod 6 = 2).
- entry-missing-thumb-light.png: Entry 1 of 7 opened after the thumb was deleted; Entry uses the full photo so it renders normally.
- entry-theme-switch-before-light.png / entry-theme-switch-after-dark.png: Entry 2 of 4 open in light, switched to dark with the screen open, screenshot after ~2s.
- settings-light/dark.png: Settings with backup not set up ("Not backed up", Set up backup, Restore). Lock toggle is disabled ("Set a screen lock in Android settings to use this").
- import-light/dark.png: Import ("Add 2 photos") reached via the dock gallery button and the system photo picker (two jpgs pushed to /sdcard/Pictures, removed afterwards; nothing imported).

## Could not capture
- lock-light/dark: the journal lock only appears when Biometrics.available() is true; this AVD has no device credential/biometric, so the lock screen never shows. Setting a device PIN would change the emulator's security settings, so I did not.
- Future month empty: not reachable (blocked in the app); September substituted.
- Fresh (current) empty month: needs an empty DB.
- entry-missing-thumb: Entry screen does not use the thumbnail, so the state is only visible on Month.
