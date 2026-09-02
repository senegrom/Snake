# Snake

A dependency-free Swing implementation of Snake on six board topologies, every surface you can make by gluing opposite edges of a rectangle:

- **Plane** — solid walls on all sides
- **Cylinder** — left and right edges join; top and bottom are walls
- **Möbius band** — left and right edges join with a reflection; top and bottom are walls
- **Torus** — both pairs of edges join without reflection
- **Klein bottle** — left and right edges join with a reflection; top and bottom join normally
- **Real projective plane** — both pairs of edges join with reflection

The margin around the board shows a faint copy of each glued neighbour, mirrored wherever the gluing reflects, so you can see where the snake will re-emerge before it crosses. Walls are drawn as thick black bands, glued edges as dashed lines (orange where the gluing reflects), and the board's light diagonal texture makes a mirrored copy obvious at a glance.

## Requirements

JDK 25 or newer. Older Java releases are intentionally unsupported.

## Build and run

```sh
rm -rf out && mkdir out
find src -name '*.java' -print0 \
  | xargs -0 javac --release 25 -encoding UTF-8 -Xlint:all -Werror -d out
java -cp out snake.gui.SnakeFrame
```

Use the arrow keys to steer and Space to pause or resume. Speed (1 to 9) and topology are chosen before the game starts; Restart keeps them and reuses the window.

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
java -ea -cp out snake.gui.SnakeGuiTests
```

The dependency-free headless suites cover exhaustive properties of all nine edge gluings, game-model invariants, deterministic timing and apple selection, property notifications, and pixel-level rendering including the mirrored neighbour copies. The GUI suite needs a display; it drives the real Swing window to verify controls, key bindings, terminal-state handling, and in-place restart. GitHub Actions performs the same warning-clean JDK 25 build on every push to `master` and on manual runs, running the GUI suite under Xvfb.
