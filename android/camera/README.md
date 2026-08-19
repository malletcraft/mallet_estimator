# :camera — the Insta360 X3 bridge

Approved SDK application: 2026-08-19, account amitrameshphadke@gmail.com.

**The SDK is proprietary and must NEVER be committed to this repository**
(same non-negotiable class as cost data — this repo is public and a push is
permanent). The module is therefore built around the AAR being *absent*:

- `libs/` is **gitignored**. Building with `-PwithCamera` and an empty
  `libs/` produces the stub (a warning says so); dropping the SDK AARs into
  `libs/` is what arms the real wiring.
- **Private parking**: the SDK zip lives in the owner's Google Drive folder
  `MCFT Insta360 SDK` (not in any repo). CI fetches it from there at build
  time using the existing `MCFT_GDRIVE_SA_JSON` service account — share the
  folder with that service account, read-only.
- Local dev: download the zip from https://www.insta360.com/sdk/record
  (owner's login), unzip, copy the `.aar` files into `android/camera/libs/`.

## Design

`X3Bridge` is the only doorway. The app never imports it — it probes for
the class by name (`CameraCapability`), so `:app` compiles identically with
or without `:camera` included. Flow once wired: connect over the X3's Wi-Fi
→ list recent panoramas → download → hand the file to the exact pipeline
gallery-picked panos already use (FaceWriter split → MediaStore →
SyncWorker). No new sync path — the camera is just a faster way to get the
pano onto the phone.
