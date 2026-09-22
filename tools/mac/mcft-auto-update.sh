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

rm -rf "$WORK/dl"; mkdir -p "$WORK/dl"
gh run download "$run" -R "$REPO" -n "$ARTIFACT" -D "$WORK/dl" || {
  echo "run $run has no camera artifact (skipped build?) — marking seen"
  echo "$run" > "$STATE"; exit 0; }
apk=$(find "$WORK/dl" -name "*.apk" | head -1)
[ -n "$apk" ] || { echo "artifact empty"; exit 0; }
echo "installing $(basename "$apk") from run $run"
if adb install -r "$apk"; then
  echo "$run" > "$STATE"
  # Read the version back OFF THE PHONE. "INSTALLED" was this script's word
  # for "adb did not error", and the whole reason today's twelve-day gap went
  # unnoticed is that nothing ever compared what shipped against what the
  # handset actually runs. Now the log says which version is on it.
  got=$(adb shell dumpsys package com.malletcrafts.sitephotos 2>/dev/null \
        | awk -F= '/versionName/{print $2; exit}' | tr -d '\r')
  echo "INSTALLED run $run — phone now reports versionName=${got:-unknown}"
else
  echo "run $run FAILED to install — state NOT advanced, will retry next tick"
fi
rm -rf "$WORK/dl"
