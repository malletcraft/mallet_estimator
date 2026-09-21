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
  # A LIVE PANE IS NOT A LIVE BRIDGE, and this check used to stop here.
  #
  # 2026-09-21: the bridge was deaf for twelve days while this script
  # reported nothing at all. `claude` was RUNNING -- pane_dead 0, the happy
  # path above -- and sitting for ever on the "Yes, I trust this folder"
  # prompt, which registers no session with the cloud. So the supervisor saw
  # health, exited quietly 1440 times a day, and the APK that fixes the FOV
  # sat on Drive while the phone stayed twelve days behind. That is the same
  # "silence and health look identical" fault this file's own header warns
  # about, one level up: the check that was supposed to catch a dead CLI
  # cannot see a STUCK one.
  #
  # So say something. Nothing is auto-answered here -- a trust prompt is a
  # decision about what code may run on somebody's laptop, and a script that
  # clicks yes on his behalf is worse than one that waits. But it stops being
  # invisible, and `cat ~/.mcft-bridge/log.txt` now answers "why is the Mac
  # deaf" in one line.
  PANE=$(tmux capture-pane -p -t "$SESSION" 2>/dev/null | tail -25 || true)
  if printf '%s' "$PANE" | grep -qiE 'do you trust|trust the files|yes, i trust|❯ *1\. *yes'; then
    STAMP="$WORK/.waiting-on-trust"
    # Log once per stall, not once a minute, or the log is useless.
    if [ ! -f "$STAMP" ]; then
      echo "$(date '+%F %T') BRIDGE IS STUCK at the folder-trust prompt and is NOT connected."
      echo "$(date '+%F %T')   Fix at the machine: tmux attach -t $SESSION , answer it, then ctrl-b d"
      printf '%s\n' "$PANE" | sed 's/^/    | /'
      : > "$STAMP"
    fi
    exit 0
  fi
  rm -f "$WORK/.waiting-on-trust" 2>/dev/null || true
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
# -c "$WORK": a FIXED working directory. Claude's folder trust is recorded
# per path, and launchd hands a job whatever cwd it likes -- so without this
# the prompt can come back on a directory nobody answered for, which is how a
# bridge that worked last week asks again this week.
tmux new-session -d -s "$SESSION" -c "$WORK" \
  "caffeinate -is claude --remote-control $RC_NAME"
tmux set-option -t "$SESSION" remain-on-exit on 2>/dev/null || true
echo "$(date '+%F %T') started (attach with: tmux attach -t $SESSION)"
