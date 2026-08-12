# Snake

A dependency-free Swing implementation of Snake on four board topologies:

- **Plane** — ordinary solid walls
- **Torus** — opposite edges join without reflection
- **Klein bottle** — left/right edges join with reflection; top/bottom edges join normally
- **Real projective plane** — both pairs of opposite edges join with reflection

## Requirements

JDK 25 or newer. Older Java releases are intentionally unsupported.

## Build and run

```sh
rm -rf out && mkdir out
find src -name '*.java' -print0 \
  | xargs -0 javac --release 25 -encoding UTF-8 -Xlint:all -Werror -d out
java -cp out snake.gui.SnakeFrame
```

Use the arrow keys to steer and Space to pause or resume. Speed and topology are selected before the game starts.

## Tests

```sh
rm -rf out && mkdir out
find src test -name '*.java' -print0 \
  | xargs -0 javac --release 25 -encoding UTF-8 -Xlint:all -Werror -d out
java -ea -Djava.awt.headless=true -cp out snake.gui.SnakeTests
java -ea -Djava.awt.headless=true -cp out snake.gui.SnakeSmokeTests
```

The dependency-free headless suites cover exhaustive topology properties, game-model invariants, deterministic timing and apple selection, property notifications, and pixel-level rendering. CI also launches the real Swing window under Xvfb to verify controls, key bindings, terminal-state handling, and restart behaviour.
