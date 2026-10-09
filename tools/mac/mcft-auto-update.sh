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

# ONE TICK AT A TIME. Amit, 2026-10-09: "make sure fone not connected is not
# the escuse to redownload the apk. it waste a lot of time." A camera build
# takes ~20 min to fetch and launchd fires every 60 s; a second tick starting
# mid-download would wipe the .partial folder below and begin again from zero.
# The lock holds this tick's pid, so one left by a crash is noticed and taken.
LOCK="$WORK/tick.lock"
if ! mkdir "$LOCK" 2>/dev/null; then
  held=$(cat "$LOCK/pid" 2>/dev/null || echo "")
  # The self-refresh below re-execs under the SAME pid, so its own lock is not a rival.
  if [ -n "$held" ] && [ "$held" != "$$" ] && kill -0 "$held" 2>/dev/null; then
    echo "previous tick (pid $held) still running — leaving its download alone"; exit 0
  fi
  echo "stale lock from pid ${held:-?} — taking it"
fi
echo $$ > "$LOCK/pid"
trap 'rm -rf "$LOCK"' EXIT

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
      chmod +x "$new" && mv "$new" "$0"
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

# DOWNLOAD FIRST, PHONE SECOND. Amit, 2026-10-08: "apk should get donwloaded
# to mac and then get installed. if required i will pluin fone again but my
# wait of downloading apk should not get wasted just because fone wass not
# connected". The camera build is ~130 MB and takes ~20 min on this line, so
# it is fetched whether or not the phone is on the cable, kept in a folder
# per run, and installed on whichever tick next finds the phone. Only a
# successful install deletes it.
CACHE="$WORK/cache"
mkdir -p "$CACHE"
# Older runs' downloads are superseded by this one.
find "$CACHE" -mindepth 1 -maxdepth 1 ! -name "$run" ! -name "$run.partial" -exec rm -rf {} +
apk=$(find "$CACHE/$run" -name "*.apk" 2>/dev/null | head -1 || true)
# A kept file that is not a whole zip (cut short, disk full) is fetched again
# rather than installed -- unzip -t, from the Mac session's version of this
# change (malletcraft/mallet_estimator#3).
if [ -n "$apk" ] && ! unzip -tq "$apk" >/dev/null 2>&1; then
  echo "run $run — kept APK is damaged, downloading it again"
  rm -rf "$CACHE/$run"; apk=""
fi
if [ -z "$apk" ]; then
  # Into .partial, renamed only when complete, so a download cut off half
  # way is never taken for a finished one.
  rm -rf "$CACHE/$run.partial"; mkdir -p "$CACHE/$run.partial"
  echo "downloading run $run (phone not needed for this step)"
  # A failed download is NOT "this run has no camera build". 2026-10-07: the
  # 0.3.167 download timed out on a sleepy Mac, this branch logged "no camera
  # artifact" and marked the run seen -- so the dock never tried that build
  # again, and the phone sat on 0.3.165 with nothing in the log saying why.
  # Ask GitHub whether the artifact EXISTS before giving up on the run: only a
  # run that genuinely has none is skipped; anything else retries next tick.
  if ! gh run download "$run" -R "$REPO" -n "$ARTIFACT" -D "$CACHE/$run.partial"; then
    has=$(gh api "repos/$REPO/actions/runs/$run/artifacts" \
          -q "[.artifacts[] | select(.name==\"$ARTIFACT\" and .expired==false)] | length" \
          2>/dev/null || echo "?")
    if [ "$has" = "0" ]; then
      echo "run $run has no camera artifact (skipped build?) — marking seen"
      echo "$run" > "$STATE"
    else
      echo "run $run download FAILED (artifact present=$has) — state NOT advanced, will retry next tick"
    fi
    rm -rf "$CACHE/$run.partial"; exit 0
  fi
  got_apk=$(find "$CACHE/$run.partial" -name "*.apk" | head -1)
  [ -n "$got_apk" ] && unzip -tq "$got_apk" >/dev/null 2>&1 \
    || { echo "run $run artifact has no whole .apk"; rm -rf "$CACHE/$run.partial"; exit 0; }
  mv "$CACHE/$run.partial" "$CACHE/$run"
  apk=$(find "$CACHE/$run" -name "*.apk" | head -1)
  echo "downloaded run $run — kept at $apk until it is installed"
fi

# Phone actually here, and say WHICH failure it is.
#
# 2026-09-21: the phone was on the cable and this printed "phone not
# connected", which is the one thing it was not. `adb devices` reports an
# unaccepted USB-debugging prompt as `unauthorized` and a wedged daemon as
# `offline`, and the old test only matched `device` -- so three different
# faults with three different fixes all read as the cable being out. That
# sends somebody to check a cable that is fine.
STATES=$( (adb devices || true) | awk 'NR>1 && NF>=2 {print $2}' | sort -u | tr '\n' ',' | sed 's/,$//')
if ! printf '%s' "$STATES" | tr ',' '\n' | grep -qx device; then
  case "$STATES" in
    *unauthorized*)
      echo "run $run downloaded, pending — phone IS on the cable but USB debugging is not" \
           "authorized. Unlock the phone and tap 'Always allow from this computer'." ;;
    *offline*)
      echo "run $run downloaded, pending — adb says 'offline' (wedged daemon or asleep)." \
           "Fix: adb kill-server; adb devices" ;;
    "")
      echo "run $run downloaded, pending — no device on any port. Cable that carries data," \
           "and File Transfer / PTP mode rather than charge-only." ;;
    *)
      echo "run $run downloaded, pending — adb reports state(s) [$STATES], none usable." ;;
  esac
  exit 0
fi

echo "installing $(basename "$apk") from run $run"
if adb install -r "$apk"; then
  echo "$run" > "$STATE"
  rm -rf "$CACHE/$run"
  # Read the version back OFF THE PHONE. "INSTALLED" was this script's word
  # for "adb did not error", and the whole reason today's twelve-day gap went
  # unnoticed is that nothing ever compared what shipped against what the
  # handset actually runs. Now the log says which version is on it.
  got=$(adb shell dumpsys package com.malletcrafts.sitephotos 2>/dev/null \
        | awk -F= '/versionName/{print $2; exit}' | tr -d '\r')
  echo "INSTALLED run $run — phone now reports versionName=${got:-unknown}"
else
  echo "run $run FAILED to install — download kept, will retry next tick"
fi
