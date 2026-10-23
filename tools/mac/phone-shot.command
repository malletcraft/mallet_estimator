#!/bin/bash
# MCFT Phone Shot — double-click on the Mac: the phone's screen lands on the
# Desktop AND on the clipboard, ready to Cmd+V into the Claude chat.
#
# Why this exists (Amit, 23 Oct 2026): a webcam pointed at a handset is the
# worst copy of a screen that is already digital, and every typed command is
# one too many. The phone is on the cable for the auto-updater anyway, so the
# same adb pulls the real pixels. Installed and kept current on the Desktop by
# mcft-auto-update.sh, the same self-refreshing dock that installs the APK —
# Claude ships a change, the next tick carries it, nobody types.
set -euo pipefail
# Into a folder, not loose on the Desktop (Amit, 23 Oct 2026: "just dont store
# the screesnshot directly on desktop but inside a folder in desktop").
SHOTDIR="$HOME/Desktop/MCFT Phone Shots"; mkdir -p "$SHOTDIR"
OUT="$SHOTDIR/phone-$(date +%H%M%S).png"
if ! command -v adb >/dev/null; then
  echo "adb is missing — the dock's setup installs it; run tools/mac/setup.sh once."; exit 1
fi
if ! adb exec-out screencap -p > "$OUT" 2>/dev/null || [ ! -s "$OUT" ]; then
  rm -f "$OUT"
  echo "No screen came back. The usual three, in order: the cable is out, the"
  echo "phone is asleep (wake it), or USB debugging is not allowed for this Mac"
  echo "(unlock the phone and tap 'Always allow')."
  exit 1
fi
# On the clipboard too — the whole point is Cmd+V into the chat. Best effort:
# a clipboard refusal must not delete a screenshot that is already saved.
osascript -e "set the clipboard to (read (POSIX file \"$OUT\") as «class PNGf»)" 2>/dev/null || true
echo "Saved $OUT — and it is on the clipboard: Cmd+V into the chat."
