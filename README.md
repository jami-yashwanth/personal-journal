# Journal

A private, capture-first photo journal for Android. It opens to the camera, organises everything by day in a month "bento" grid, supports text entries and tags, and backs up end-to-end encrypted to your own Cloudflare R2 bucket. No server, no account, no analytics.

## Features

- **Capture:** camera opens first; optional caption and tags; launcher shortcuts for "Take photo" and "Write".
- **Write:** text entries with an editable date and time.
- **Import:** pick up to 20 photos from the gallery; each becomes its own entry on the day it was taken (EXIF date, or today with a note when a photo has none).
- **Look back:** month bento grid (busier days get bigger tiles), tag filter that dims non-matching days, day view, entry view with edit, date change and delete.
- **Tags:** one-tap recent tags, search, create; lowercase and de-duplicated.
- **Lock:** fingerprint or device PIN, on by default; re-locks after 1 minute in the background.
- **Reminder:** one daily notification ("Anything from today?"), skipped on days you've already logged.
- **Backup:** incremental, automatic, AES-256-GCM encrypted on the phone before upload; restore on a new phone with your 12-word recovery phrase.

## Build and install

Requirements: JDK 17, Android SDK with platform 37.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Tests:

```bash
./gradlew testDebugUnitTest            # 107 JVM/Robolectric tests
./gradlew connectedDebugAndroidTest    # device tests: Keystore + backup/restore over HTTP
```

## Setting up backup (about 10 minutes)

1. In the Cloudflare dashboard, create an R2 bucket (for example `journal`).
2. Create an R2 API token with **Object Read & Write** permission limited to that bucket. Note the Account ID, Access Key ID and Secret Access Key.
3. In the app: Settings > Set up backup. Paste the four values and tap **Test connection**.
4. Write down the 12-word recovery phrase and type it back to confirm.

The keys and phrase stay on the phone, encrypted with an Android Keystore key. Everything uploaded is encrypted; object names contain only random IDs. **Without the recovery phrase the backup cannot be opened.** You can view it again in Settings after unlocking.

Backups run a couple of minutes after you save and every 6 hours, on Wi-Fi unless you allow mobile data. Restore: Settings > Restore from backup (also offered on an empty journal).

## How it's built

Kotlin, Jetpack Compose (Material 3), Room, CameraX, Coil, WorkManager, androidx.biometric, OkHttp with a small SigV4 signer for R2's S3 API.

```
app/src/main/java/com/personal/journal/
  data/        Room schema, repository (sole writer), photo store, settings
  bento/       tile sizing and packing for the month grid
  backup/      crypto, recovery phrase, SigV4, R2 client, backup and restore runners
  reminder/    daily reminder worker
  ui/          one package per screen (Compose + ViewModel), shared components, theme
```

Design spec and implementation plan live in `docs/superpowers/`; design mockups in `docs/design/` (final look: `final-cool-*.png`).
