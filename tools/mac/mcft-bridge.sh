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

# FIND claude RATHER THAN TRUST launchd's PATH.
#
# 2026-09-21, from this log: the bridge started fine on 17 Sep and reported
# "claude missing" from 19 Sep on -- so the binary MOVED. launchd does not
# read a login shell's profile; the plist hardcodes
# /opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin, and Claude Code's native
# installer puts its launcher in ~/.local/bin and removes the npm/homebrew
# one. `claude` therefore stayed on Amit's PATH in Terminal and vanished from
# launchd's, which is why this looked like a machine problem and was a
# one-line environment problem.
#
# The old check also made a dead end of it: "claude missing" says a name was
# not found, never WHERE it looked, so nobody could tell a moved binary from
# an uninstalled one. It now names every path tried.
CLAUDE=""
for c in "$(command -v claude 2>/dev/null || true)" \
         "$HOME/.local/bin/claude" \
         "$HOME/.claude/local/claude" \
         "$HOME/bin/claude" \
         /opt/homebrew/bin/claude \
         /usr/local/bin/claude; do
  [ -n "$c" ] && [ -x "$c" ] && { CLAUDE="$c"; break; }
done
if [ -z "$CLAUDE" ]; then
  echo "$(date '+%F %T') claude not found. Tried: PATH ($PATH), ~/.local/bin," \
       "~/.claude/local, ~/bin, /opt/homebrew/bin, /usr/local/bin."
  echo "$(date '+%F %T')   Find it with: which claude   (in Terminal), then add" \
       "its directory to the plist PATH in ~/Library/LaunchAgents/com.malletcrafts.claudebridge.plist"
  exit 0
fi

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

# WHO IS THIS CLI SIGNED IN AS, recorded before every start.
#
# 2026-09-22: the CLI ran, showed its trust prompt, was answered, and STILL
# registered no session the cloud could see -- and nothing anywhere said why.
# Every other explanation was checked and killed: `--remote-control [name]`
# does take an optional name and parses fine, the binary is found, the pane
# is alive. What no log could answer was WHICH ACCOUNT it is signed into, and
# a CLI signed into a different account (or signed out) registers a session
# the cloud side of this project simply cannot see. That is indistinguishable
# from a dead bridge from the outside, which is the shape of fault this repo
# keeps meeting.
#
# Best-effort and never fatal: a CLI that cannot report its auth can still be
# worth starting, and the log now carries the answer either way.
echo "$(date '+%F %T') auth: $("$CLAUDE" auth status 2>&1 | tr '\n' ' ' | cut -c1-200)"
echo "$(date '+%F %T') version: $("$CLAUDE" --version 2>&1 | head -1)"
echo "$(date '+%F %T') starting bridge as '$RC_NAME' using $CLAUDE"
# remain-on-exit keeps a crashed pane readable instead of vanishing, so the
# next tick can SEE that it died and this log can say when.
# -c "$WORK": a FIXED working directory. Claude's folder trust is recorded
# per path, and launchd hands a job whatever cwd it likes -- so without this
# the prompt can come back on a directory nobody answered for, which is how a
# bridge that worked last week asks again this week.
tmux new-session -d -s "$SESSION" -c "$WORK" \
  "caffeinate -is '$CLAUDE' --remote-control $RC_NAME"
tmux set-option -t "$SESSION" remain-on-exit on 2>/dev/null || true
echo "$(date '+%F %T') started (attach with: tmux attach -t $SESSION)"
