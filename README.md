# Snake

A Java/Swing Snake game played on different topological surfaces.

## Topologies

- **Plane** — ordinary walls; crossing an edge ends the game.
- **Torus** — opposite edges wrap directly.
- **Klein bottle** — one pair of opposite edges wraps with a reflection.
- **Projective plane** — both pairs of opposite edges wrap with a reflection.

## Run

The project has no external dependencies. Compile the Java sources and run `snakeGUI.Main`.

```sh
mkdir -p build/classes
find src -name '*.java' -print0 | xargs -0 javac -d build/classes
java -cp build/classes snakeGUI.Main
```

An Eclipse `.project` and `.classpath` are also retained for IDE users.

## Tests

The regression suite deliberately uses only the JDK, keeping this small project free of build-system and test-framework dependencies.

```sh
mkdir -p build/test-classes
find src test -name '*.java' -print0 | xargs -0 javac -Xlint:all -d build/test-classes
java -ea -Djava.awt.headless=true -cp build/test-classes AllTests
```

GitHub Actions runs the same checks on JDK 8, 17 and 21 for pushes and pull requests.

## Licence

GNU Affero General Public License v3.0. See `LICENSE`.
