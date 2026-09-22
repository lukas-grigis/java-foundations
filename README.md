<p align="center">
  <img src="https://img.shields.io/badge/Java-27-ED8B00?logo=openjdk&logoColor=white" alt="Java 27">
  <img src="https://img.shields.io/badge/Maven-standalone_bricks-C71A36?logo=apachemaven&logoColor=white" alt="Maven, standalone bricks">
  <img src="https://img.shields.io/badge/bricks-5-2E8B57" alt="5 bricks">
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
| [memory-model](bricks/memory-model/) | `synchronized`, `volatile` and `final` are three different promises, not three flavors of thread safety: one against lost updates, one against stale reads, one against half-built objects | Concurrency | — |

Each brick's README carries the thesis, a file-by-file tour, and the experiment to run yourself.

## Run it

```
mise install                         # once: Java 27 and Maven 3
mise run demo                        # every brick, one after the other
mise run memory-model                # one brick: build it, then every proof
mise run memory-model:stale-read     # one proof
mise tasks                           # every brick and proof, with what it shows
```

A brick that needs another JDK says so on its first run, with the exact line to install it
(`missing JDK: mise install java@26.0.1`): `jdk27-defaults` runs the same classes on JDK 26.0.1
and 27.0.0, `virtual-threads-pinning` on JDK 21.0.2 and 26.0.1, and `structured-concurrency` is
pinned to JDK 27.0.0 with `--enable-preview`.

Without mise: `cd bricks/<brick> && mvn -q compile && java -cp target/classes <MainClass>` — each
brick's README names its main classes and the JDK they need.

## Project structure

```
java-foundations/
├── mise.toml          # the toolchain, build and demo; picks up every bricks/*/tasks.toml
├── bin/brick          # the one runner behind every brick task
└── bricks/<brick>/    # one standalone Maven project per brick
    ├── pom.xml        # the Java release it compiles for
    ├── tasks.toml     # its tasks: which JDK builds it, which JDKs and flags run which proof
    ├── README.md      # the thesis, the code you would actually write, the recorded run
    └── src/main/java/dev/lukasgrigis/foundations/<brick>/
        ├── example/   # code as it would look in a service
        └── proof/     # one question each, one number each
```

No parent pom, no aggregator: a brick you can't copy out of the repo and run isn't standalone.
Packages follow `dev.lukasgrigis.foundations.<brick>` with the dashes removed — one subpackage per
brick. `data-modeling`, the first brick, keeps its classes in one package, without the
`example/` and `proof/` split. A new brick is a folder with a `pom.xml` and a `tasks.toml`, plus
its row in the table above and the count in the bricks badge; nothing else in the root changes.

## License

MIT — see [LICENSE](LICENSE).
