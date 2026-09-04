#!/bin/bash
# One-time setup on the Mac: installs the auto-updater + a launchd job that
# runs it every 30 minutes. Requires: brew, and a one-time `gh auth login`
# (the script tells you if either is missing).
set -euo pipefail
command -v brew >/dev/null || { echo "Install Homebrew first: https://brew.sh"; exit 1; }
brew list gh >/dev/null 2>&1 || brew install gh
brew list android-platform-tools >/dev/null 2>&1 || brew install android-platform-tools
# tmux gives the bridge a PTY. `claude --remote-control` is an interactive
# session and launchd hands a job no terminal, so without this the bridge
# can only live in a Terminal window somebody remembered to leave open.
brew list tmux >/dev/null 2>&1 || brew install tmux
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

# ---- The Claude bridge. Separate job on purpose: the phone updater and the
# bridge fail independently, and one job doing both would mean a tmux problem
# stopping APK installs, or an unplugged phone looking like a dead bridge.
BDEST="$HOME/.mcft-bridge"
mkdir -p "$BDEST"
curl -fsSL "https://raw.githubusercontent.com/malletcraft/mallet_estimator/main/tools/mac/mcft-bridge.sh" \
  -o "$BDEST/mcft-bridge.sh"
chmod +x "$BDEST/mcft-bridge.sh"

BPLIST="$HOME/Library/LaunchAgents/com.malletcrafts.claudebridge.plist"
cat > "$BPLIST" <<PL
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>Label</key><string>com.malletcrafts.claudebridge</string>
  <key>ProgramArguments</key><array>
    <string>/bin/bash</string>
    <string>$BDEST/mcft-bridge.sh</string>
  </array>
  <!-- Same 60 s tick as the updater, and NOT KeepAlive: tmux daemonises, so
       launchd would see the starter exit and restart it in a loop for ever.
       A tick that checks and returns restarts the bridge within a minute of
       any death, including the first minute after the Mac wakes. -->
  <key>StartInterval</key><integer>60</integer>
  <key>RunAtLoad</key><true/>
  <key>EnvironmentVariables</key><dict>
    <key>PATH</key><string>/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin</string>
  </dict>
</dict></plist>
PL
launchctl unload "$BPLIST" 2>/dev/null || true
launchctl load "$BPLIST"

echo "Installed two jobs:"
echo "  phone updater — the phone updates itself whenever it's on the cable"
echo "                  log: $HOME/.mcft-auto-update/log.txt"
echo "  claude bridge — restarts within 60s of any death, survives logout"
echo "                  log: $HOME/.mcft-bridge/log.txt"
echo "                  watch it live: tmux attach -t mcft-bridge  (detach: ctrl-b then d)"
echo
echo "A SLEEPING MAC STILL RUNS NOTHING. caffeinate holds off idle sleep while"
echo "the bridge runs, but a closed lid on battery sleeps anyway. If the bridge"
echo "has to answer at night, the Mac has to be awake."
