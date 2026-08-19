# :camera — the Insta360 X3 bridge

Approved SDK application: 2026-08-19, account amitrameshphadke@gmail.com.

**The SDK is proprietary and must NEVER be committed to this repository**
(same non-negotiable class as cost data — this repo is public and a push is
permanent). The module is therefore built around the AAR being *absent*:

- `libs/` is **gitignored**. Building with `-PwithCamera` and an empty
  `libs/` produces the stub (a warning says so); dropping the SDK AARs into
  `libs/` is what arms the real wiring.
- **Private parking**: the SDK zip (`Android-SDK-V1.10.1.zip`) lives in the
  owner's Google Drive under the ImageMeter handover folder, subfolder
  "Insta360 SDK (private parking — never into a repo)" — a tree the
  `mcft-erpnext-drive` service account already reads. CI fetches it by file
  id at build time using the `MCFT_GDRIVE_SA_JSON` secret. The camera APK it
  produces is uploaded ONLY as an Actions artifact (login-gated); the public
  `android-latest` release always stays SDK-free.
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
