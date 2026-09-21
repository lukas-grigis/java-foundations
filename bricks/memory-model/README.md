# memory-model

The Java Memory Model answers one question — who sees what, and when. `synchronized`,
`volatile`, and `final` are its three answers, and the myth this brick kills is
"`volatile` makes an operation atomic".

## The questions

| Question | Run it | Number on this machine | What the spec says |
|---|---|---|---|
| Does `counter++` from two threads end up at 2N? | `mise run memory-model:lost-updates` | plain 20,008,778–20,374,610 / 40,000,000 (5 runs)<br>volatile 20,443,138–21,845,850 / 40,000,000 (5 runs)<br>synchronized 40,000,000 (5/5) | "the value 1 is added to the value of the variable and the sum is stored back into the variable" — [§15.14.2](https://docs.oracle.com/javase/specs/jls/se26/html/jls-15.html#jls-15.14.2), JLS SE26.<br>"An unlock on a monitor happens-before every subsequent lock on that monitor." — [§17.4.5](https://docs.oracle.com/javase/specs/jls/se26/html/jls-17.html#jls-17.4.5) |
| Does a plain-`boolean` spin loop ever notice a flag another thread set? | `mise run memory-model:stale-read` | plain 3000 ms — capped, never noticed (5/5)<br>volatile 0.0367–0.0395 ms (5 runs) | "The compiler is free to read the field `this.done` just once, and reuse the cached value in each execution of the loop." — [§17.3](https://docs.oracle.com/javase/specs/jls/se26/html/jls-17.html#jls-17.3).<br>"A write to a volatile variable v synchronizes-with all subsequent reads of v by any thread" — [§17.4.4](https://docs.oracle.com/javase/specs/jls/se26/html/jls-17.html#jls-17.4.4) |
| Publishing through a data race, no lock — how often does the reader see a field still at its default 0? | `mise run memory-model:unsafe-publication` | plain 40–206 zero-reads / ~17.5M–20.9M reads (5 runs)<br>final 0 zero-reads / ~18.5M–23.9M reads (5/5) | "A thread that can only see a reference to an object after that object has been completely initialized is guaranteed to see the correctly initialized values for that object's `final` fields." — [§17.5](https://docs.oracle.com/javase/specs/jls/se26/html/jls-17.html#jls-17.5) |

## The code you would actually write

- **`example/HitCounter.java`** — one `int` field, one `hits++` line. `VolatileHitCounter` is
  the same class with that field made volatile — the fix most engineers reach for first,
  because "volatile means thread-safe" is what sticks from a five-minute explanation. It is
  not: `++` is still a read, an add and a store, and volatile only guarantees each of those
  three sees the latest value, not that the three happen as one. `SynchronizedHitCounter`
  guards the same line with a lock — the actual fix.
- **`example/Poller.java`** — a plain `stopped` flag checked at the top of a loop.
  `VolatilePoller` is the same class with that one field made volatile; nothing else changes.
- **`example/Settings.java`** — a plain `timeoutMs` field set once in the constructor.
  `ImmutableSettings` makes it `final` — the only thing that protects a lock-free reader.

## Recorded run

```
Apple M2 Pro (12 cores: 8 performance + 4 efficiency), macOS 26.5.1
OpenJDK 64-Bit Server VM (build 27+35-2325, mixed mode, sharing)
2026-09-21

$ mise run memory-model
================= LostUpdates =================
plain int     20009822 hits
volatile int  20443138 hits
synchronized  40000000 hits

================= StaleRead =================
plain boolean     3000 ms (cap — still running)
volatile boolean  0.036667 ms

================= UnsafePublication =================
plain field   41 zero-reads / 20861125 reads
final field   0 zero-reads / 21088000 reads
```

Each proof ran 5 times; the table above gives the range, not the best run. One number held
flat across every run and is reported as such: `synchronized` at exactly 40,000,000. `final`
field zero-reads were also 0 in all 5 runs — reported as 0, not "never happens", because 5 runs
is not a proof of never.

## When this demo would lie to you

- **`LostUpdates`, plain row**: this is not two threads interleaving on `hits++`. Isolated in
  its own JVM and timed, the plain variant finishes 40,000,000 increments in single-digit
  milliseconds — around two orders of magnitude faster than the volatile row — because C2
  keeps the non-volatile `hits` field in a register for the whole loop and writes it back
  once (measured on JDK 26.0.1, 20.09.2026); one thread's entire share of increments gets overwritten by the other's final store.
  That is JLS §17.4's "myriad of code transformations", not §15.14.2's "read, add, store"
  non-atomicity — the §15.14.2 quote in the table explains the **volatile** row (real
  interleaving loss), not the plain one. The three variants also run one after another in a
  single JVM, so by the third call the call site is megamorphic; isolated per variant the
  volatile row's range narrowed by a few million (JDK 26.0.1, 20.09.2026).
- **`StaleRead`, plain row**: `3000 ms (cap — still running)` is not a measured duration, it's
  the timeout — the loop never noticed the flag. The volatile row is timed from a nanoTime
  stamp the worker writes itself, not around `Thread.join()`, but it still includes real OS
  scheduling latency between `shutdown()` and the worker's next loop check; true `volatile`
  visibility is on the order of tens of nanoseconds, well below what's printed here.
- **`StaleRead`'s §17.3 bullet, stated correctly**: adding a `Thread.sleep(1)` inside the plain
  loop often makes it terminate in practice, because the JIT is less likely to keep the field
  hoisted across an interpreted or lightly-compiled loop — but JLS §17.3 gives exactly that
  sleeping loop as its own broken example and says the loop "would never terminate, even if
  another thread changed the value of `this.done`." Terminating is luck, not a rule.
- **`UnsafePublication`**: the two variants run at different speeds, so the reader sees a
  different number of total reads in each — that is why both are printed alongside the
  zero-read count. The same zero-valued slot can also be counted again on a later pass before
  the writer overwrites it, so the count can over-state how many distinct publications were
  missed; read it as "at least one race was caught", not a precise tally.
- Where the interesting outcome is a low-single-digit-percent tail — the worst-case lost-update
  magnitude, a double-checked-locking race — a plain loop like these will not reliably surface
  it; that is what jcstress's forked-process, billions-of-samples methodology is for. Where the
  question is "what did the JIT actually do", JFR or `-XX:+PrintCompilation` answers it, not a
  counter.
- `final` only pins the reference; it does not freeze what it points at. A `record` component
  copied with `List.copyOf` in its compact constructor is what protects a mutable list from a
  caller's later edit — a different mechanism, with no race and no number, so it is not one of
  the three proofs above.
