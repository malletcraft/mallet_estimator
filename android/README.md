# MCFT Site Photos — Android

Two modules:

- **`pano/`** — pure JVM. The 360→faces projection and the ImageMeter naming
  convention, held to the goldens the server publishes
  (`mallet_estimator/tests/golden/`). Runs anywhere a JDK exists; CI's
  "Projection contract" job is this.
- **`app/`** — the phone app. Included only with `-PwithApp` because it needs
  the Android SDK.

## What the app does (reduced scope, pending the Insta360 SDK)

Pick a 360 from the gallery (exported by the Insta360 app) → choose
client/project/room/stage from the cached masters → the phone splits it into
six captioned faces and writes them to
`Pictures/MCFT Site Photos/<Client>/<Project>/<Project> — <Room>/`, where
ImageMeter's importer (via Google Photos) picks them up → the untouched pano
queues in app-private storage and syncs to ERPNext whenever there is signal
(create → upload → bind, idempotent on the device-minted `MCAP-…` id).

Credentials: site URL + API key/secret (Settings screen). Generate the pair on
the user's page in ERPNext; the app never holds a session, so sync survives
any number of offline days.

## Building

APK: CI (`android-app.yml`) on every push touching `android/`, artifact
`mcft-site-photos-apk`. Locally: `gradle -PwithApp :app:assembleDebug`
(needs the Android SDK).

Contract tests only: `gradle :pano:test` (no SDK needed).

## The committed keystore

`debug.keystore` is committed ON PURPOSE. It is a sideload-testing signature,
not a secret (storepass `android`, the platform convention): committing it
means every CI build carries the SAME signature, so a new APK installs over
the old one instead of demanding an uninstall that would wipe the offline
queue. It must never sign anything for Play — the Play upload key will be a
real secret in CI, once the Play Console exists.
