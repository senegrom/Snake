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

Speed (1 to 9) and topology are chosen before the game starts. Restart keeps these settings, resets the score and clock, and reuses the window. The default **Fit** zoom scales the whole board, walls and neighbouring margins to the available viewport, so the snake and apples remain visible even on a small display. Setup controls and help collapse when play starts to give the board more room; **Settings** shows or hides them at any time, and Restart shows them again. Zoom remains accessible while settings are hidden.

Fixed zoom (100%, 150% or 200%) is also available at any time and survives a restart. It may require scrolling on smaller screens. Start, Restart, zoom changes and window resizing reveal the head rather than leaving the new snake outside an old scroll position. Choose Fit to see the entire board without scrolling. The renderer uses the display's effective resolution for sharp scaled cells and mirrored neighbours. View changes never change game coordinates, speed, scoring or topology.

Ready and paused games show a banner above the board, without hiding the snake. Steering while paused updates the direction indicator without advancing the snake. Switching to another window or minimising the game pauses it; returning never resumes it automatically. The About dialog temporarily pauses a running game and restores it on a normal close, but preserves manual pauses and cancels automatic resume if you switched applications while the dialog was open.

A shortcut held while switching windows stays suppressed until its key is released. If the operating system did not deliver a release made in another application, tap that shortcut once to re-arm it, then press it again to act; its button remains available immediately. This deliberately avoids mistaking auto-repeat for a new press after focus returns.

Settings occupy separate labelled rows so a narrow window cannot hide a wrapped Zoom selector. The score and clock have their own row to keep button captions readable. Window size and position are calculated together within the available work area, then submitted as one native bounds request.

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
java -ea -Djava.awt.headless=true -cp out snake.gui.SnakeViewportTests render
java -ea -cp out snake.gui.SnakeGuiTests
java -ea -cp out snake.gui.SnakeInputTests
java -ea -cp out snake.gui.SnakeFocusLayoutTests
java -ea -cp out snake.gui.SnakeViewportTests
```

The dependency-free headless suites cover exhaustive properties of all nine edge gluings, game-model invariants, deterministic timing and apple selection, property notifications, paused-turn repainting, overlays, and pixel-level rendering at every fixed zoom level and fractional Fit scales, including the mirrored neighbour copies.

The GUI suites need a display. `SnakeGuiTests` checks component actions and in-place restart deterministically. `SnakeInputTests` runs outside the Swing event-dispatch thread, using real `java.awt.Robot` input and bounded EDT queries. It covers keyboard-only setup and play, held shortcuts, modifier changes on release, real timer movement, pause stability, focus loss, About-dialog behaviour, and stopping old/disposed timers. `SnakeFocusLayoutTests` covers held Space/F2/F3 across focus changes, keys originally handled by focused controls, observed and missed outside-window releases, and control visibility across all topologies and fixed zoom levels. `SnakeViewportTests` verifies whole-board Fit visibility through Start/Pause/Restart/settings changes, head visibility after scrolled restarts and manual zoom changes, and native window sizing after zoom-driven repositioning. Geometry checks wait for native events to settle with a bounded timeout. Do not type or click in the test windows while a suite runs.

GitHub Actions runs two jobs in parallel on pushes and pull requests to `master`, and on manual runs. The build job performs the warning-clean JDK 25 compile, the headless suites and the deterministic GUI smoke test. The desktop job runs the Robot-driven suites under bare Xvfb and under an Openbox-managed virtual desktop, so a timing-dependent failure is isolated from the build and can be re-run on its own; Openbox is only a test dependency, not a game dependency.

The narrow-layout checks use a 1024×768 display at 200% scaling and assert that the logical work area really is 512×384. Install Xvfb, Openbox and x11-utils to run the managed-desktop checks locally:

```sh
xvfb-run -a -s '-screen 0 1280x1024x24' bash test/run-managed-tests.sh normal
xvfb-run -a -s '-screen 0 1024x768x24' bash test/run-managed-tests.sh small
```
