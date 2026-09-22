# jdk27-defaults

JDK 27 changed two JVM defaults with zero code change — object headers shrink and G1 replaces
Serial in small containers — and the same `.class` files prove both, just by choosing which `java`
binary runs them.

## The questions

| Question                                                                                                                                                                                                                   | Run it                                      | Number on this machine                                                                                                                       | What the spec says                                                                                                                                                                                                                                                                                                                                     |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| How many fewer bytes does one stored session cost on the heap when the object header shrinks from 96 to 64 bits (JEP 534), and is the `UseCompactObjectHeaders` flag — not something else about JDK 27 — the actual cause? | `mise run jdk27-defaults:bytes-per-session` | 104.8–104.9 bytes/session (headers off) → 80.7–80.9 bytes/session (headers on), matching within 0.2 bytes on both JDKs once the flag matches | JEP 534, Summary: "Make compact object headers the default object header layout in the HotSpot JVM. Compact object headers reduce object headers from 96 bits down to 64 bits on 64-bit architectures, thereby reducing heap size, improving deployment density, and increasing data locality." — [openjdk.org/jeps/534](https://openjdk.org/jeps/534) |
| Which collector does the JVM choose when nobody tells it (JEP 523), on JDK 26 vs JDK 27?                                                                                                                                   | `mise run jdk27-defaults:which-collector`   | `0` (Serial) on 26.0.1 → `1` (G1) on 27, both under a single-CPU constraint; `1` on both with no constraint                                  | JEP 523, Description: "If you do not specify a garbage collector on the command line then the JVM will always select G1, regardless of the number of processors and the available physical memory." — [openjdk.org/jeps/523](https://openjdk.org/jeps/523)                                                                                             |
| What does the Serial→G1 switch cost an idle JVM in resident memory, on JDK 27?                                                                                                                                             | `mise run jdk27-defaults:idle-footprint`    | 42.7–43.2 MB (Serial) → 43.9–44.0 MB (G1), about 0.8–1.3 MB more                                                                             | JEP 523, Goals: "the performance metrics of throughput, latency, memory footprint, and startup time should not degrade significantly" for environments that used to get Serial — [openjdk.org/jeps/523](https://openjdk.org/jeps/523)                                                                                                                  |

Without mise: `cd bricks/jdk27-defaults && JAVA_HOME="$(mise where java@26.0.1)" mvn -q compile`,
then one line per JDK, e.g. for `BytesPerSession`:

```
$(mise where java@26.0.1)/bin/java -Xms512m -Xmx512m -XX:+UseG1GC -cp target/classes dev.lukasgrigis.foundations.jdk27defaults.proof.BytesPerSession
$(mise where java@27.0.0)/bin/java -Xms512m -Xmx512m -XX:+UseG1GC -cp target/classes dev.lukasgrigis.foundations.jdk27defaults.proof.BytesPerSession
```

The other flag combinations (`-XX:+UseCompactObjectHeaders`, `-XX:ActiveProcessorCount=1`, the
Serial/G1 pair for `IdleFootprint`) are in `tasks.toml`.

## The code you would actually write

[`example/SessionRecord.java`](src/main/java/dev/lukasgrigis/foundations/jdk27defaults/example/SessionRecord.java)
is a record with three `long` fields — a session id, a user id, an expiry — held in
[`example/SessionStore.java`](src/main/java/dev/lukasgrigis/foundations/jdk27defaults/example/SessionStore.java),
a `Map`-backed in-memory session store an auth service keeps between requests. There is no broken
variant here: nothing about this code is wrong on JDK 26. `proof.BytesPerSession` allocates two
million of them into the store and reads the heap delta — that is honestly a *session*, not a
single object: a `HashMap<Long, SessionRecord>` entry is three heap objects (the record, the
boxed `Long` key, and the map's internal node), and compact headers shrink all three. Measured on
their own — a plain array of `SessionRecord`, no map — one record alone saves 8 of those 24 bytes;
the other 16 come from the key and the node. For a store holding on the order of a million
sessions, that is 24 bytes × 1,000,000 ≈ 24 MB back from a container's memory limit, for free, on
JDK 27. The one flag worth setting deliberately: pin the collector you actually want
(`-XX:+UseG1GC` or `-XX:+UseSerialGC`) rather than inherit whatever a future default happens to
pick — and if a JNI library or agent ever chokes on the new header layout,
`-XX:-UseCompactObjectHeaders` is the escape hatch back to the JDK 26 shape.

## Recorded run

Apple M2 Pro, macOS 26.5.1. `openjdk 26.0.1 2026-04-21` (build 26.0.1+8-34) and `openjdk 27
2026-09-15` (build 27+35-2325), installed via mise. 21.09.2026, five runs each; where the number
moved, the range is reported, not the best run — a zero would be reported as a zero, none came up.
Block below is one full run, verbatim, via `mise run jdk27-defaults`:

```
================= BytesPerSession (JEP 534) =================
java-26.0.1 compact=false  104.8 bytes-per-session
java-26.0.1 compact=true  80.8 bytes-per-session
java-27 compact=true  80.9 bytes-per-session
java-27 compact=false  104.9 bytes-per-session

================= WhichCollector (JEP 523) =================
java-26.0.1 [Copy, MarkSweepCompact]  0 g1-selected
java-27 [G1 Young Generation, G1 Concurrent GC, G1 Old Generation]  1 g1-selected
java-26.0.1 [G1 Young Generation, G1 Concurrent GC, G1 Old Generation]  1 g1-selected
java-27 [G1 Young Generation, G1 Concurrent GC, G1 Old Generation]  1 g1-selected

================= IdleFootprint (Serial vs G1 on 27) =================
Serial  42.8 MB
G1  43.9 MB
```

Across five runs: `BytesPerSession` — headers off 104.8–104.9, headers on 80.7–80.9, both JDKs
within 0.2 bytes of each other once the flag matches. `WhichCollector` — deterministic, `0`/`1`
identically on all five runs. `IdleFootprint` — Serial 42.7–43.2 MB, G1 43.9–44.0 MB, delta
0.8–1.3 MB.

## When this demo would lie to you

- **`BytesPerSession` is a heap accounting number, not a benchmark.** It reads
  `Runtime.totalMemory() − freeMemory()` after one forced `System.gc()` — no warm-up loop, no
  steady state. It answers "how many bytes", not "how fast"; for throughput or pause time, JFR or
  a proper JMH harness is the right tool, not this proof.
- **The 24-byte saving is shape-dependent, and mostly not about the record.** Two-thirds of it is
  the `HashMap`'s own bookkeeping (the boxed key, the node), not the `SessionRecord` itself. A
  session store backed by a plain array or an `IntObjectHashMap`-style structure would see less;
  a `Map` with a bigger key type could see more. The one number that is about the record alone —
  8 bytes — only holds because its three `long` fields already land on an 8-byte boundary; the
  header always shrinks by 4 bytes, but whether that turns into a visible saving on the object's
  own size depends on what else is in it.
- **`-XX:MaxRAM` is not what flips the collector.** It was tried in this proof's `WhichCollector`
  and dropped: it sizes the heap (confirmed with `-XX:+PrintFlagsFinal`) but the GC-selection
  ergonomics never read it — `-XX:MaxRAM=1g` alone leaves 26.0.1 on G1. `-XX:ActiveProcessorCount=1`
  alone is what selects Serial on 26.0.1; JDK 27 ignores `-XX:MaxRAM` outright ("support was
  removed in 27.0"). The exact "< 1792 MB" half of the old rule — JEP 523, Motivation: "testing
  showed that Serial had significant advantages in throughput and footprint in constrained
  environments with a single CPU or less than 1792 MB of physical memory"
  ([openjdk.org/jeps/523](https://openjdk.org/jeps/523)) — is not independently verified here; it
  would need a real memory-constrained environment (a cgroup or a VM), not a flag that the
  selector does not consult.
- **`IdleFootprint` is OS-reported RSS, a proxy, not an isolate — and the ~1 MB delta does not
  come from GC threads.** It includes the JIT code cache, metaspace and native thread stacks
  alongside GC bookkeeping (roughly 41 of the 43 MB), and it swings by a few hundred KB between
  runs. Forcing G1 down to one GC thread (`-XX:ActiveProcessorCount=1`) instead of the ten it
  picks by default does not move the number — the delta is not thread-count overhead. Treat the
  range as "roughly a percent more resident memory with G1 on this machine", not a precise cost
  of the collector's own data structures.
- **`IdleFootprint` shells out to `ps`, so it only runs on macOS or Linux.** There is no
  equivalent one-liner on Windows; the proof would need a different RSS source there.
- **Both proofs ran on Apple silicon with 32 GB of memory.** Header layout, alignment, and
  ergonomic sizing can differ on other architectures, heap sizes, or container memory limits;
  rerun before trusting the exact numbers on a different machine.

Root [README.md](../../README.md) lists every brick.
