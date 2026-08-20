# MCFT Site Photos — capture on site, annotate in the app, keep in ERPNext

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

Tap any capture card → its six faces → tap a face to annotate.

**Get close first.** Pinch to zoom (up to 10×) and drag the photo around.
Marking a wall edge accurately at full-room zoom is guesswork; zoom in and
it takes one try. Nothing you place moves when you zoom — the marks live on
the photo, not on the screen.

**Everything works the same way: drop it, then drag it into place.**

- **+ Measure** drops a line across the middle of the view. Drag each end
  onto what you're measuring — a **loupe** appears in the top corner
  showing the exact pixel under your finger, so your thumb never hides the
  point. Ends snap to ends, so a corner shared by two measurements stays
  one corner. Then **Value** to key the number in mm/cm/m/in/ft, or shoot
  it with the laser (below). The `m · ft` chip shows metric and imperial
  together.
- **+ Opening** drops a rectangle — choose **Window, Door, Column, Beam**
  or **Opening** — and you drag its **four** corners onto the real ones.
  Four, not two, because a window seen from an angle is not a square on the
  photo, and keeping its true shape is what lets the model place it
  correctly. **Tag** changes what it is or adds a note.
- **+ Note** drops a text pin ("damp patch", "switchboard").
- **Tap** anything to select it; handles appear only on the selection, so
  nothing gets nudged by accident. **Delete** removes the selection,
  **Undo** takes back the last change.

### The laser (Leica DISTO D2)

Tap **Laser** once — it finds the meter and connects. Then, for each
measurement: **select the measure first, then press the button on the
meter.** The number lands on the selected line; there is nothing to
confirm and nothing to type. If the meter is set to feet and inches, the
app switches to showing both units.

Two things to know, both from Leica's own documentation:

- **Do not pair the D2 in Android's Bluetooth settings.** This meter is
  designed to connect without pairing, and pairing it stops it working. If
  it is already paired, unpair it.
- Only one app can hold the meter at a time. If Leica's own DISTO app is
  running in the background, close it.

The meter switches itself off after a few minutes to save battery — that's
normal. Tap **Laser** again to reconnect.

Annotations are data, never burned into the photo — they sync to ERPNext
with the capture and follow it to every device. Correct a value any time;
the photo never needs re-shooting.

Annotate every wall face: the measured faces are what the SketchUp room
model gets built from.

### The room's baseline

The first properly-marked capture of a room is its **geometry baseline** —
the one the SketchUp model gets built from. Mark it in ERPNext
(**Geometry baseline** on the capture), and once the six faces are right,
**freeze** it. After freezing, its annotations stop changing, so re-running
the model always produces the same room. Later captures of that room are
progress photos: annotate them freely, they never affect the model. If
something real changes on site — a wall comes down — capture the room
again and make the new capture the baseline.

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
