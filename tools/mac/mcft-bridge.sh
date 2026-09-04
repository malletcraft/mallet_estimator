#!/bin/bash
# MCFT — keep the Claude Code bridge alive on Amit's Mac (launchd, every 60s).
#
# WHY THIS EXISTS. `claude --remote-control` starts an INTERACTIVE session:
# it wants a terminal, and it is one process. So the bridge lived in whatever
# Terminal window happened to be open, and it died with the window, with a
# logout, or with the machine. Twice in one session a dispatch from the cloud
# simply vanished — and the failure is worse than a plain outage, because
# get_session keeps reporting session_status IDLE and connection_status
# connected for a session whose process is gone. The status field cannot see
# a dead CLI. Silence and health look identical, which is the shape of fault
# this repo has met before and now refuses to leave in place.
#
# THE PTY IS THE WHOLE PROBLEM, and why this is not simply a KeepAlive job:
# launchd gives a job no terminal, so `claude --remote-control` under launchd
# directly has nothing to attach to. tmux supplies the PTY, and it also
# leaves Amit a way in — `tmux attach -t mcft-bridge` shows the real session
# exactly as if he had started it himself.
#
# tmux DAEMONISES, so a KeepAlive job would see the starter exit and restart
# it forever. Hence the same pattern the APK updater already uses here: a
# cheap StartInterval tick that checks and returns. Restart lands within 60
# seconds of any death, including after a wake from sleep.
#
# A FIXED SESSION NAME is the second half of the fix. Every `claude` restart
# registers a NEW session id, so a trigger bound to the old id is bound to a
# corpse and cloud-side code had to hunt for the newest bridge row each time.
# --remote-control takes a name; with a stable one the Mac is addressable by
# name instead of by a guess about recency.
#
# WHAT THIS CANNOT DO, said plainly rather than discovered later: a sleeping
# Mac runs nothing. caffeinate holds off IDLE sleep while the bridge runs,
# but a closed lid on battery still sleeps and no launchd tick fires until
# the machine wakes. If the bridge must be reachable at night, the Mac needs
# to be awake — that is a settings decision, not something a script can win.
set -euo pipefail
SESSION="mcft-bridge"
RC_NAME="mcft-mac"
WORK="$HOME/.mcft-bridge"
LOG="$WORK/log.txt"
mkdir -p "$WORK"
exec >>"$LOG" 2>&1

command -v tmux >/dev/null || { echo "$(date '+%F %T') tmux missing"; exit 0; }
command -v claude >/dev/null || { echo "$(date '+%F %T') claude missing"; exit 0; }

# Alive? tmux knowing the session is not enough — the pane can hold a dead
# shell after the CLI crashes, which looks identical from the outside and is
# precisely the state that produced a deaf bridge reporting itself connected.
if tmux has-session -t "$SESSION" 2>/dev/null \
   && tmux list-panes -t "$SESSION" -F '#{pane_dead}' 2>/dev/null | grep -qx 0; then
  # Quiet on the happy path: this runs 1440 times a day.
  exit 0
fi

if tmux has-session -t "$SESSION" 2>/dev/null; then
  echo "$(date '+%F %T') session present but pane dead — recreating"
  tmux kill-session -t "$SESSION" 2>/dev/null || true
fi

echo "$(date '+%F %T') starting bridge as '$RC_NAME'"
# remain-on-exit keeps a crashed pane readable instead of vanishing, so the
# next tick can SEE that it died and this log can say when.
tmux new-session -d -s "$SESSION" \
  "caffeinate -is claude --remote-control $RC_NAME"
tmux set-option -t "$SESSION" remain-on-exit on 2>/dev/null || true
echo "$(date '+%F %T') started (attach with: tmux attach -t $SESSION)"
