# Site Photos — Android (Trusted Web Activity)

The Android app is a thin shell around the web app at `/sitephoto`. That is
the whole point: **an ordinary change ships as a web deploy and reaches every
phone with no Play release and no review.** A Play release is only needed when
something in this folder changes — the package name, the icons, the target
host, or the Android runtime itself.

It also means the web app and the Android app cannot drift apart, because
they are the same app.

## What has to exist before the first build

1. A **Play Console organization account** (D-U-N-S required; start early —
   verification is the long pole, not the $25).
2. An **upload keystore**. Generate once, keep it out of this repo forever:

   ```
   keytool -genkeypair -v -keystore android.keystore -alias sitephotos \
       -keyalg RSA -keysize 2048 -validity 10000
   ```

   Store it as the `ANDROID_KEYSTORE_BASE64` / `ANDROID_KEYSTORE_PASSWORD` /
   `ANDROID_KEY_ALIAS` / `ANDROID_KEY_PASSWORD` GitHub secrets. A lost upload
   key is recoverable through Play support; a leaked one is not.

3. **Digital Asset Links**, or the app shows a browser URL bar and looks like
   a web page rather than an app. Put BOTH fingerprints into *Site Photo
   Settings → Android App (TWA)*:
   - Play Console → *App signing* → **App signing key certificate** SHA-256
   - Play Console → *App signing* → **Upload key certificate** SHA-256

   The site then publishes `/.well-known/assetlinks.json` itself — no deploy
   needed to change a fingerprint. Verify before releasing:

   ```
   curl -s https://mcft-stg.frappe.cloud/.well-known/assetlinks.json
   ```

   Chrome and Google cache this, so a wrong file is slower to undo than a
   missing one — which is why the site serves nothing at all until both the
   package name and a fingerprint are set.

## Building

```
npm i -g @bubblewrap/cli
bubblewrap init --manifest=https://mcft-stg.frappe.cloud/sitephoto/manifest.json
bubblewrap build          # produces app-release-bundle.aab
```

`twa-manifest.json` here is the checked-in answer to everything `init` asks,
so keep it as the source of truth and let `init` read it rather than
answering the prompts by hand.

## Which track

Start on **internal testing** (up to 100 testers by email): installs through
Play, upgrades automatically, no public listing and no full review. Move to
production only if this is ever offered to other studios.

## Pointing at a different site

`host`, `startUrl`, `fullScopeUrl` and `webManifestUrl` all name the site. A
production deployment is a different package id and a different keystore —
do not repoint this one, or a staging build will overwrite the real app on
everyone's phone.
