#!/bin/bash
# One-time setup on the Mac: installs the auto-updater + a launchd job that
# runs it every 30 minutes. Requires: brew, and a one-time `gh auth login`
# (the script tells you if either is missing).
set -euo pipefail
command -v brew >/dev/null || { echo "Install Homebrew first: https://brew.sh"; exit 1; }
brew list gh >/dev/null 2>&1 || brew install gh
brew list android-platform-tools >/dev/null 2>&1 || brew install android-platform-tools
gh auth status >/dev/null 2>&1 || { echo "Run: gh auth login --web   (one time), then re-run this setup"; exit 1; }

DEST="$HOME/.mcft-auto-update"
mkdir -p "$DEST"
curl -fsSL "https://raw.githubusercontent.com/malletcraft/mallet_estimator/main/tools/mac/mcft-auto-update.sh" \
  -o "$DEST/mcft-auto-update.sh"
chmod +x "$DEST/mcft-auto-update.sh"

PLIST="$HOME/Library/LaunchAgents/com.malletcrafts.autoupdate.plist"
cat > "$PLIST" <<PL
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>Label</key><string>com.malletcrafts.autoupdate</string>
  <key>ProgramArguments</key><array>
    <string>/bin/bash</string>
    <string>$DEST/mcft-auto-update.sh</string>
  </array>
  <!-- 60 s, not 30 min. Amit, 2026-08-21: 'always push latest apk moment
       its available. no need to wait for 30 minutes window.' A poll is one
       cheap `gh run list` call and it exits immediately when the newest
       green run is the one already installed, so the cost of asking often
       is close to nothing — while half an hour of waiting to find out a
       build is broken is not. -->
  <key>StartInterval</key><integer>60</integer>
  <key>RunAtLoad</key><true/>
  <key>EnvironmentVariables</key><dict>
    <key>PATH</key><string>/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin</string>
  </dict>
</dict></plist>
PL
launchctl unload "$PLIST" 2>/dev/null || true
launchctl load "$PLIST"
echo "Installed. The phone now updates itself whenever it's on the cable."
echo "Log: $HOME/.mcft-auto-update/log.txt"
