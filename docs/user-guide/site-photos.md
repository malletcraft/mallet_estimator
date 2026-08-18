# MCFT Site Photos — capture on site, annotate in ImageMeter, keep in ERPNext

*The Android app, for phones and Chromebooks. Built for sites with no
signal: everything works offline and syncs itself when the network
returns.*

## Install and sign in

- **Managed devices**: install from the company's private Google Play app
  (auto-updates from then on).
- **Direct link** (testing/fallback):
  `https://github.com/malletcraft/mallet_estimator/releases/download/android-latest/mcft-site-photos.apk`
  — the same link always serves the newest build and installs over the old.
- First launch → **Settings**: site URL (pre-filled), plus your **API key
  and secret** — generated once on your ERPNext user page (Settings → API
  Access; the secret is shown a single time). The app never uses a login
  session, which is why days offline don't sign you out.
- Your account needs the **Mallet Site Photographer** role (or admin).

## Capturing a room

1. Shoot the 360 with the Insta360 X3 and export it to the phone's gallery
   (the direct camera connection arrives with the Insta360 SDK).
2. In the app: pick **Project → Room → Stage** — the lists are the real
   ERPNext masters, cached on the phone.
3. **New site with no project yet?** Project picker → **＋ New client /
   project…** — type both names, works fully offline; on sync they become
   real masters, or match existing ones no matter how the spelling differs.
4. **Pick 360 photo** → the phone splits it into six captioned faces and
   saves them to
   `Pictures/MCFT Site Photos/<Client>/<Project>/<Project> — <Room>/`.

Each capture card shows its life honestly: *on this phone* → *uploading* →
*Synced as MEST-PH-…*, with the reason shown if a sync attempt failed
(it retries itself; **Sync now** forces one).

## Annotating in ImageMeter

Open ImageMeter → your project/room folder → **add photos** → pick from
Google Photos — the album named `<Project> — <Room>` is the one the app just
wrote. Annotate as usual. ImageMeter's own Drive sync carries the annotated
copies back; the server matches them to the right capture and face by
filename (`MCAP-…`/`MEST-PH-…`) every hour. Anything it cannot prove goes to
a review queue in ERPNext rather than being guessed.

## Seeing it all in ERPNext

**Site Photos** in the estimator menu — client → project → room → captures,
faces and annotation badges. The room's timeline across dates and stages is
the site-progress record.

## Worth knowing

- Turn **off** Google Photos backup for the `MCFT Site Photos` folder unless
  you want every face in your personal cloud.
- A Chromebook joined to the X3's Wi-Fi has no internet — captures queue,
  exactly as on site, and sync when you're back on a normal network.
