<p align="center">
  <img src="https://img.shields.io/badge/Java-26-ED8B00?logo=openjdk&logoColor=white" alt="Java 26">
  <img src="https://img.shields.io/badge/Maven-standalone_bricks-C71A36?logo=apachemaven&logoColor=white" alt="Maven, standalone bricks">
  <img src="https://img.shields.io/badge/bricks-1-2E8B57" alt="1 brick">
  <img src="https://img.shields.io/badge/license-MIT-blue" alt="MIT License">
</p>

<h1 align="center">Java Foundations</h1>

<p align="center">
  A brick series on modern Java: each brick takes ONE design primitive of the language and makes
  it land with a small, honest, runnable example — no framework, no toy syntax tour.<br>
  Every brick is a standalone Maven project you can <code>cd</code> into and run on its own;
  the series is just the shelf they sit on. Each brick has a companion deep-dive on
  <a href="https://lukasgrigis.dev/blog/">lukasgrigis.dev</a>.
</p>

---

## The bricks

Folders are slugs, nothing is numbered.

| Brick | Thesis | Category | Article |
|-------|--------|----------|---------|
| [data-modeling](bricks/data-modeling/) | Records + sealed interfaces + exhaustive switch are ONE modeling primitive, not three features | Data modeling | — |

Each brick's README carries the thesis, a file-by-file tour, and the experiment to run yourself.

## How to run

```
mise run data-modeling   # build + run the data-modeling brick
mise run demo            # run every brick in turn
mise run build           # compile every brick, run nothing
```

Or without mise: `cd bricks/<brick> && mvn -q compile && java -cp target/classes <MainClass>` —
each brick's README names its main class. Tool versions are pinned in [mise.toml](mise.toml)
(Java 26, Maven 3).

## Project structure

```
java-foundations/
├── mise.toml            # pinned toolchain + one task per brick
└── bricks/
    └── data-modeling/   # standalone Maven project — own pom, own README
```

No parent pom, no aggregator: a brick you can't copy out of the repo and run isn't standalone.
Packages follow `dev.lukasgrigis.foundations.<brick>` — one subpackage per brick.

## License

MIT — see [LICENSE](LICENSE).
