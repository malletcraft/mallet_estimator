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
4. **Room size** — pick Small / Medium / Large (or Server default). Small
   rooms need wider split faces or the walls come back with corners cut
   off; the app computes the right field-of-view from the room geometry
   (a ~5×7 ft bathroom needs ≈130°, a large living room ≈100°). The
   choice is remembered, so a morning of bathrooms is one tap.
5. **Pick 360 photo** → the phone splits it into six captioned faces and
   saves them to
   `Pictures/MCFT Site Photos/<Client>/<Project>/<Project> — <Room>/`.

**Shooting technique — what makes the split faces true:**

- Camera at **half the ceiling height** (≈4 ft 9 in under a 9½ ft
  ceiling). Mid-height splits the view evenly between floor and ceiling
  — a low camera is the main reason ceiling corners go missing.
- Stick **vertical, camera level**; the X3's own levelling then exports a
  true pano, and a level pano in means square walls out.
- Stand near the **room centre** when possible — every wall gets the same
  treatment.

Each capture card shows its life honestly: *on this phone* → *uploading* →
*Synced as MEST-PH-…*, with the reason shown if a sync attempt failed
(it retries itself; **Sync now** forces one).

## Annotating in the app (new — replaces ImageMeter)

Tap any capture card → its six faces → tap a face to annotate:

- **Measure**: tap the two ends of what you're measuring, key the value in
  mm/cm/m/in/ft — the label shows metric (and imperial too, via the `m · ft`
  chip). Drag an endpoint to fine-tune; a **magnifier loupe** follows your
  finger so the point lands exactly on the wall edge.
- **Notes**: long-press drops a text pin ("damp patch", "switchboard").
- Annotations are data, never burned into the photo — they sync to ERPNext
  with the capture and follow it to every device. Tap a label to correct a
  value; the photo never needs re-shooting.

Annotate every wall face: the measured faces are what the SketchUp room
model gets built from.

## Annotating in ImageMeter (legacy, until audio notes land in-app)

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
