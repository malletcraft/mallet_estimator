#!/bin/bash
# MCFT Site Photos — silent phone updater (runs on Amit's Mac via launchd).
# Every run: find the newest green camera build on GitHub Actions; if it is
# newer than the last one installed AND the phone is on the cable, download
# and `adb install -r` it — which Android performs WITHOUT any on-phone tap
# because the Mac is an authorized debugging host. State lives beside the
# script so a missed run (phone unplugged) simply retries next time.
set -euo pipefail
REPO="malletcraft/mallet_estimator"
ARTIFACT="mcft-site-photos-camera-apk"
STATE="$HOME/.mcft-auto-update-run"
WORK="$HOME/.mcft-auto-update"
LOG="$WORK/log.txt"
mkdir -p "$WORK"
exec >>"$LOG" 2>&1
echo "--- $(date '+%Y-%m-%d %H:%M:%S') tick"

command -v gh >/dev/null || { echo "gh missing"; exit 0; }
command -v adb >/dev/null || { echo "adb missing"; exit 0; }

# KEEP THIS SCRIPT CURRENT FROM main, every tick. 2026-10-08: the Mac was
# still running a copy from before the "timed-out download is not a missing
# artifact" fix, so 0.3.174 stalled on a bug already fixed in the repo --
# setup.sh copies the script once, and nothing ever copied it again. Now the
# copy on main is the only version: fetched, syntax-checked, swapped in and
# re-run. A fetch that fails leaves this copy running and says so.
if [ -z "${MCFT_UPDATER_FRESH:-}" ]; then
  new="$WORK/mcft-auto-update.sh.new"
  if gh api "repos/$REPO/contents/tools/mac/mcft-auto-update.sh?ref=main" \
       -H "Accept: application/vnd.github.raw" > "$new" 2>/dev/null \
     && [ -s "$new" ] && bash -n "$new"; then
    if ! cmp -s "$new" "$0"; then
      cp "$new" "$0" && chmod +x "$0"
      echo "updater refreshed from main — re-running it"
      rm -f "$new"
      MCFT_UPDATER_FRESH=1 exec /bin/bash "$0"
    fi
  else
    echo "updater self-refresh FAILED (fetch or syntax) — running this copy"
  fi
  rm -f "$new"
fi

# Newest green run of the android workflow on main.
run=$(gh run list -R "$REPO" -w "android-app.yml" -b main -s success -L 1 \
      --json databaseId -q '.[0].databaseId' || true)
[ -n "$run" ] || { echo "no green run found"; exit 0; }
last=$(cat "$STATE" 2>/dev/null || echo "")
[ "$run" != "$last" ] || { echo "already installed run $run"; exit 0; }

# Phone actually here, and say WHICH failure it is.
#
# 2026-09-21: the phone was on the cable and this printed "phone not
# connected", which is the one thing it was not. `adb devices` reports an
# unaccepted USB-debugging prompt as `unauthorized` and a wedged daemon as
# `offline`, and the old test only matched `device` -- so three different
# faults with three different fixes all read as the cable being out. That
# sends somebody to check a cable that is fine.
STATES=$(adb devices | awk 'NR>1 && NF>=2 {print $2}' | sort -u | tr '\n' ',' | sed 's/,$//')
if ! printf '%s' "$STATES" | tr ',' '\n' | grep -qx device; then
  case "$STATES" in
    *unauthorized*)
      echo "run $run pending — phone IS on the cable but USB debugging is not" \
           "authorized. Unlock the phone and tap 'Always allow from this computer'." ;;
    *offline*)
      echo "run $run pending — adb says 'offline' (wedged daemon or asleep)." \
           "Fix: adb kill-server; adb devices" ;;
    "")
      echo "run $run pending — no device on any port. Cable that carries data," \
           "and File Transfer / PTP mode rather than charge-only." ;;
    *)
      echo "run $run pending — adb reports state(s) [$STATES], none usable." ;;
  esac
  exit 0
fi

# KEEP THE APK UNTIL IT IS ON THE PHONE. 2026-10-08: the 0.3.176 download
# took 35 minutes, the phone slipped off the cable in its last minute, the
# install failed -- and the APK was deleted with it, so the retry had to fetch
# all 129 MB again. A downloaded build now waits here, one file per run, until
# adb has installed it; a newer run replaces it. unzip -t rejects a file cut
# short, so a half-written APK is downloaded again rather than installed.
KEEP="$WORK/apk-run-$run.apk"
find "$WORK" -maxdepth 1 -name 'apk-run-*.apk' ! -name "apk-run-$run.apk" -delete
if [ -s "$KEEP" ] && unzip -tq "$KEEP" >/dev/null 2>&1; then
  echo "run $run — reusing the APK already downloaded, no new download"
else
rm -f "$KEEP"
rm -rf "$WORK/dl"; mkdir -p "$WORK/dl"
# A failed download is NOT "this run has no camera build". 2026-10-07: the
# 0.3.167 download timed out on a sleepy Mac, this branch logged "no camera
# artifact" and marked the run seen -- so the dock never tried that build
# again, and the phone sat on 0.3.165 with nothing in the log saying why.
# Ask GitHub whether the artifact EXISTS before giving up on the run: only a
# run that genuinely has none is skipped; anything else retries next tick.
if ! gh run download "$run" -R "$REPO" -n "$ARTIFACT" -D "$WORK/dl"; then
  has=$(gh api "repos/$REPO/actions/runs/$run/artifacts" \
        -q "[.artifacts[] | select(.name==\"$ARTIFACT\" and .expired==false)] | length" \
        2>/dev/null || echo "?")
  if [ "$has" = "0" ]; then
    echo "run $run has no camera artifact (skipped build?) — marking seen"
    echo "$run" > "$STATE"
  else
    echo "run $run download FAILED (artifact present=$has) — state NOT advanced, will retry next tick"
  fi
  rm -rf "$WORK/dl"; exit 0
fi
apk=$(find "$WORK/dl" -name "*.apk" | head -1)
[ -n "$apk" ] || { echo "artifact empty"; rm -rf "$WORK/dl"; exit 0; }
mv "$apk" "$KEEP"; rm -rf "$WORK/dl"
fi
echo "installing $(basename "$KEEP") from run $run"
if adb install -r "$KEEP"; then
  echo "$run" > "$STATE"
  rm -f "$KEEP"
  # Read the version back OFF THE PHONE. "INSTALLED" was this script's word
  # for "adb did not error", and the whole reason today's twelve-day gap went
  # unnoticed is that nothing ever compared what shipped against what the
  # handset actually runs. Now the log says which version is on it.
  got=$(adb shell dumpsys package com.malletcrafts.sitephotos 2>/dev/null \
        | awk -F= '/versionName/{print $2; exit}' | tr -d '\r')
  echo "INSTALLED run $run — phone now reports versionName=${got:-unknown}"
else
  echo "run $run FAILED to install — state NOT advanced, APK kept, will retry next tick"
fi
