<p align="center">
  <img src="https://img.shields.io/badge/Java-27-ED8B00?logo=openjdk&logoColor=white" alt="Java 27">
  <img src="https://img.shields.io/badge/Maven-standalone_bricks-C71A36?logo=apachemaven&logoColor=white" alt="Maven, standalone bricks">
  <img src="https://img.shields.io/badge/bricks-4-2E8B57" alt="4 bricks">
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
| [data-modeling](bricks/data-modeling/) | Records + sealed interfaces + exhaustive switch are ONE modeling primitive, not three features | Data modeling | [Read](https://lukasgrigis.dev/blog/java-records-sealed-pattern-matching/) |
| [jdk27-defaults](bricks/jdk27-defaults/) | JDK 27 shrinks object headers and switches small containers to G1 — same code, two JVMs, two measured defaults | JVM / GC | — |
| [structured-concurrency](bricks/structured-concurrency/) *(preview, JDK 27)* | `StructuredTaskScope` turns sibling cancellation, thread cleanup and deadlines into a guarantee instead of a discipline every caller maintains by hand | Concurrency | — |
| [virtual-threads-pinning](bricks/virtual-threads-pinning/) | JEP 491 changed the cost of `synchronized`, not its meaning — one binary, two JVMs, 2 threads inside a critical section vs. 64 | Concurrency | — |

Each brick's README carries the thesis, a file-by-file tour, and the experiment to run yourself.

## How to run

```
mise run data-modeling           # build + run the data-modeling brick
mise run jdk27-defaults          # build + run the jdk27-defaults brick, on JDK 26 and JDK 27
mise run structured-concurrency  # build + run the structured-concurrency brick (JDK 27, preview)
mise run virtual-threads-pinning # build + run the virtual-threads-pinning brick, on JDK 21 and JDK 26
mise run demo                    # run every brick in turn
mise run build                   # compile every brick, run nothing
```

Or without mise: `cd bricks/<brick> && mvn -q compile && java -cp target/classes <MainClass>` —
each brick's README names its main class(es). Tool versions are pinned in [mise.toml](mise.toml)
(Java 27, Maven 3). Three bricks name their JDKs in their own mise tasks, independent of the root
toolchain: `jdk27-defaults` runs the same classes on JDK 26.0.1 and 27.0.0,
`virtual-threads-pinning` on JDK 21.0.2 and 26.0.1, and `structured-concurrency` is pinned to
JDK 27.0.0 with `--enable-preview`.

## Project structure

```
java-foundations/
├── mise.toml                    # pinned toolchain + one task per brick
└── bricks/
    ├── data-modeling/           # standalone Maven project — own pom, own README
    ├── jdk27-defaults/          # release 26, runs unchanged on JDK 26 and JDK 27
    ├── structured-concurrency/  # preview brick — JDK 27 with --enable-preview until JEP 543 lands
    └── virtual-threads-pinning/ # release 21, runs unchanged on JDK 21 and JDK 26
```

No parent pom, no aggregator: a brick you can't copy out of the repo and run isn't standalone.
Packages follow `dev.lukasgrigis.foundations.<brick>` — one subpackage per brick.

## License

MIT — see [LICENSE](LICENSE).
