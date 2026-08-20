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

# Phone actually here? (authorized devices print as 'device')
adb devices | awk 'NR>1 && $2=="device"' | grep -q . || {
  echo "run $run pending — phone not connected"; exit 0; }

rm -rf "$WORK/dl"; mkdir -p "$WORK/dl"
gh run download "$run" -R "$REPO" -n "$ARTIFACT" -D "$WORK/dl" || {
  echo "run $run has no camera artifact (skipped build?) — marking seen"
  echo "$run" > "$STATE"; exit 0; }
apk=$(find "$WORK/dl" -name "*.apk" | head -1)
[ -n "$apk" ] || { echo "artifact empty"; exit 0; }
echo "installing $(basename "$apk") from run $run"
adb install -r "$apk" && echo "$run" > "$STATE" && echo "INSTALLED run $run"
rm -rf "$WORK/dl"
