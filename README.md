# Snake

A dependency-free Swing implementation of Snake on six board topologies, every surface you can make by gluing opposite edges of a rectangle:

- **Plane** — solid walls on all sides
- **Cylinder** — left and right edges join; top and bottom are walls
- **Möbius band** — left and right edges join with a reflection; top and bottom are walls
- **Torus** — both pairs of edges join without reflection
- **Klein bottle** — left and right edges join with a reflection; top and bottom join normally
- **Real projective plane** — both pairs of edges join with reflection

The margin around the board shows a faint copy of each glued neighbour, mirrored wherever the gluing reflects, so you can see where the snake will re-emerge before it crosses. Walls are drawn as thick black bands, glued edges as dashed lines (orange where the gluing reflects), and the board's light diagonal texture makes a mirrored copy obvious at a glance. The window also explains the selected topology's edge rules.

## Requirements

JDK 25 or newer. Older Java releases are intentionally unsupported.

## Build and run

```sh
rm -rf out && mkdir out
find src -name '*.java' -print0 \
  | xargs -0 javac --release 25 -encoding UTF-8 -Xlint:all -Werror -d out
java -cp out snake.gui.SnakeFrame
```

## Controls and display

Use the arrow keys to steer while the board has focus. Space pauses or resumes, Esc pauses without resuming, F2 starts a ready game, and F3 resets it. Holding a game shortcut triggers it only once per press. Tab and Shift+Tab reach the buttons and settings; focused settings keep their normal arrow keys, and Space activates a focused button. Starting, pausing or restarting returns focus to the board. You can also click the board to return to steering.

Speed (1 to 9) and topology are chosen before the game starts. Restart keeps these settings, resets the score and clock, and reuses the window. Zoom (100%, 150% or 200%) is available at any time and also survives a restart. The renderer uses the display's effective resolution for sharp enlarged cells and mirrored neighbours; scrollbars keep the board reachable when the enlarged window would exceed the screen.

Ready and paused games show a banner above the board, without hiding the snake. Steering while paused updates the direction indicator without advancing the snake. Switching to another window or minimising the game pauses it; returning never resumes it automatically. The About dialog temporarily pauses a running game and restores it on a normal close, but preserves manual pauses and cancels automatic resume if you switched applications while the dialog was open.

## Package

```sh
rm -rf out && mkdir out
find src -name '*.java' -print0 \
  | xargs -0 javac --release 25 -encoding UTF-8 -Xlint:all -Werror -d out
jar --create --file Snake.jar --main-class snake.gui.SnakeFrame -C out .
java -jar Snake.jar
```

`jar` ships in the JDK's `bin` directory. `Snake.jar` is ignored by git.

## Tests

```sh
rm -rf out && mkdir out
find src test -name '*.java' -print0 \
  | xargs -0 javac --release 25 -encoding UTF-8 -Xlint:all -Werror -d out
java -ea -Djava.awt.headless=true -cp out snake.gui.SnakeTests
java -ea -Djava.awt.headless=true -cp out snake.gui.SnakeSmokeTests
java -ea -Djava.awt.headless=true -cp out snake.gui.SnakeInteractionTests
java -ea -cp out snake.gui.SnakeGuiTests
java -ea -cp out snake.gui.SnakeInputTests
```

The dependency-free headless suites cover exhaustive properties of all nine edge gluings, game-model invariants, deterministic timing and apple selection, property notifications, paused-turn repainting, overlays, and pixel-level rendering at every zoom level, including the mirrored neighbour copies.

The two GUI suites need a display. `SnakeGuiTests` checks component actions and in-place restart deterministically. `SnakeInputTests` runs outside the Swing event-dispatch thread, using real `java.awt.Robot` input and bounded EDT queries. It covers keyboard-only setup and play, held shortcuts, modifier changes on release, real timer movement, pause stability, focus loss, About-dialog behaviour, and stopping old/disposed timers. Do not type or click in its windows while it runs.

GitHub Actions performs the warning-clean JDK 25 build on pushes and pull requests to `master`, and on manual runs. GUI tests run under Xvfb; the real-input suite uses a 1280×1024 virtual display and needs no window manager or third-party Java libraries.
