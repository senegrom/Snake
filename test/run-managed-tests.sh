#!/usr/bin/env bash
# Called inside xvfb-run; do not silently test without a window manager.
set -euo pipefail
mode="${1:-normal}"
case "$mode" in normal|small) ;; *) echo "Expected normal or small" >&2; exit 2 ;; esac
openbox >/tmp/snake-openbox.log 2>&1 &
wm=$!
trap 'kill "$wm" 2>/dev/null || true; wait "$wm" 2>/dev/null || true' EXIT
ready=false
for ((attempt=0; attempt<50; attempt++)); do
  if xprop -root _NET_SUPPORTING_WM_CHECK 2>/dev/null | grep -q 'window id'; then
    ready=true
    break
  fi
  sleep 0.1
done
if [[ "$ready" != true ]]; then
  cat /tmp/snake-openbox.log >&2
  echo "Openbox did not become ready" >&2
  exit 1
fi
if [[ "$mode" == small ]]; then
  java -ea -Djava.awt.headless=false -Dsun.java2d.uiScale=2 -cp out snake.gui.SnakeFocusLayoutTests small-layout
  java -ea -Djava.awt.headless=false -Dsun.java2d.uiScale=2 -cp out snake.gui.SnakeViewportTests small
else
  java -ea -Djava.awt.headless=false -cp out snake.gui.ShortcutTrackerTests
  java -ea -Djava.awt.headless=false -cp out snake.gui.SnakeFocusLayoutTests
  java -ea -Djava.awt.headless=false -cp out snake.gui.SnakeViewportTests
fi
