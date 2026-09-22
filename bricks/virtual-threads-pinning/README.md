# virtual-threads-pinning

JEP 491 changed the cost of `synchronized`, not its meaning: on JDK 26 (JEP 491 shipped in JDK 24)
a virtual thread that blocks inside a synchronized method lets go of its carrier thread, so
synchronized code scales the way `ReentrantLock` code always did — mutual exclusion never moved.

A restaurant has 2 waiters and 64 guests. Every guest eats alone in a private room, and a meal
takes 100 ms. On JDK 21 the waiter has to stand in the room for the whole meal. With 2 waiters,
2 guests eat at a time, so 64 guests need 32 rounds, about 3.2 seconds. On JDK 26 the waiter
leaves once the guest is seated and serves the next one. All 64 guests eat at the same time, and
the last one is done after about 100 ms. The rooms and the meals are the same, and a room still
holds one guest at a time. Only what the waiter does during the meal changed. In Java the waiters
are carrier threads, the guests are virtual threads, the private room is a lock, and the meal is
the blocking call held inside it. "2 waiters" is `-Djdk.virtualThreadScheduler.parallelism=2` —
a flag, not a law of the JVM (see below).

## The questions

Both JDKs must be installed (`mise install java@openjdk-21.0.2 java@26.0.1`); every task compiles
once with `--release 21` and runs the SAME class files on both. THREADS = **64**, carrier count
fixed by `-Djdk.virtualThreadScheduler.parallelism=2 -Djdk.virtualThreadScheduler.maxPoolSize=2`
(`tasks.toml`) — JDK 21's `maxInside` predicts from that flag, not core count, and saturates at
64, written "64 of 64" at the ceiling.

| Question                                                                                    | Run it                                                            | Number on this machine                                                      | What the spec says                                                                                                                                                                                                                                                                                                                                                                                                |
|---------------------------------------------------------------------------------------------|-------------------------------------------------------------------|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 64 threads, each blocking inside its OWN synchronized lock — how many run at once?          | `mise run virtual-threads-pinning:blocking-inside-synchronized`   | JDK 21: maxInside **2** of 64 · JDK 26: maxInside **64** of 64              | "We will change the JVM's implementation of the synchronized keyword so that virtual threads can acquire, hold, and release monitors, independently of their carriers." — [JEP 491, Description](https://openjdk.org/jeps/491)                                                                                                                                                                                    |
| Same shape with a `ReentrantLock` instead — does the JDK matter?                            | `mise run virtual-threads-pinning:blocking-inside-reentrant-lock` | JDK 21: maxInside **64** of 64 · JDK 26: maxInside **64** of 64             | "There are two scenarios in which a virtual thread cannot be unmounted during blocking operations because it is pinned to its carrier: When it executes code inside a synchronized block or method, or When it executes a native method or a foreign function." — [JEP 444, Executing virtual threads](https://openjdk.org/jeps/444) — `ReentrantLock` triggers neither scenario, so it never pins, on either JDK |
| 64 threads, ONE shared monitor — does JEP 491 change contention?                            | `mise run virtual-threads-pinning:shared-lock`                    | JDK 21: maxInside **1** · JDK 26: maxInside **1** (definitional, see below) | "Only one thread at a time may hold a lock on a monitor." — [JLS §17.1, Synchronization](https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html) — unchanged; JEP 491 is about unmounting from a carrier, not about who may hold the lock                                                                                                                                                                 |
| Does waiting for ANOTHER thread's class init still pin, as JEP 491's own text says it does? | `mise run virtual-threads-pinning:still-pins`                     | JDK 21: maxInside **2** of 64 · JDK 26: maxInside **64** of 64              | "When waiting for a class to be initialized by another thread (JVMS §5.5). This is a special case where the virtual thread blocks in the JVM, thus pinning the carrier." — [JEP 491, Future Work](https://openjdk.org/jeps/491) — confirmed on JDK 21; **no longer true on JDK 26** — see JDK-8369238 below                                                                                                       |

Without mise (proof 1; swap the JDK path and the class name for the other three):

```
$(mise where java@openjdk-21.0.2)/bin/java -Djdk.virtualThreadScheduler.parallelism=2 -Djdk.virtualThreadScheduler.maxPoolSize=2 -cp target/classes dev.lukasgrigis.foundations.virtualthreadspinning.proof.BlockingInsideSynchronized
$(mise where java@26.0.1)/bin/java -Djdk.virtualThreadScheduler.parallelism=2 -Djdk.virtualThreadScheduler.maxPoolSize=2 -cp target/classes dev.lukasgrigis.foundations.virtualthreadspinning.proof.BlockingInsideSynchronized
```

## The code you would actually write

[`SynchronizedSessionClient`](src/main/java/dev/lukasgrigis/foundations/virtualthreadspinning/example/SynchronizedSessionClient.java)
is the pre-JDK-24 shape: `renew()` sends an IdP heartbeat over the network, and the whole method
is `synchronized` so two concurrent renewals for the same session never race. That is a blocking
network call sitting inside a synchronized method — exactly the pattern JEP 444 named as pinning,
and the pattern teams were told to rewrite.
[`LockingSessionClient`](src/main/java/dev/lukasgrigis/foundations/virtualthreadspinning/example/LockingSessionClient.java)
is that rewrite: same heartbeat, same mutual exclusion, a `ReentrantLock` around it instead.

Which one to write today, targeting JDK 24 or later: `synchronized`. JEP 491 itself says so —
"If you are writing new code, we agree with the recommendation in *Java Concurrency in Practice*
§13.4: Use `synchronized` where practical, since it is more convenient and less error prone, and
use `ReentrantLock`... when more flexibility is required." A library that still has to run on
JDK 21 or earlier is better off with `LockingSessionClient` — the synchronized version really pins
there, exactly what proof 1 measures.

## Recorded run

`mise run virtual-threads-pinning` on 2026-09-21, Apple M2 Pro, macOS 26.5.1, arm64. JDK
21.0.2+13-58, JDK 26.0.1+8-34. Every proof: THREADS = 64,
`-Djdk.virtualThreadScheduler.parallelism=2 -Djdk.virtualThreadScheduler.maxPoolSize=2`. Each
line below ran 5 times in this session; `maxInside` never varied, so it is reported as one
number; `elapsed` is the min–max across all five runs.

```
BlockingInsideSynchronized
  JDK 21   synchronized     maxInside = 2 of 64     elapsed = 3334–3472 ms
  JDK 26   synchronized     maxInside = 64 of 64    elapsed = 111–120 ms

BlockingInsideReentrantLock
  JDK 21   ReentrantLock    maxInside = 64 of 64    elapsed = 111–120 ms
  JDK 26   ReentrantLock    maxInside = 64 of 64    elapsed = 110–121 ms

SharedLock
  JDK 21   shared lock      maxInside = 1           elapsed = 6678–6924 ms
  JDK 26   shared lock      maxInside = 1           elapsed = 6667–6965 ms

StillPins
  JDK 21   waiting-on-init  maxInside = 2 of 64     elapsed = 70–80 ms
  JDK 26   waiting-on-init  maxInside = 64 of 64    elapsed = 76–85 ms
```

**SharedLock, read `elapsed` first.** `maxInside = 1` on both JDKs is true by construction — one
shared, reentrant monitor, true on any JVM — and proves nothing about JEP 491. `elapsed` carries
this proof: 6.7–7.0 s on **both** JDKs, 64 renewals of ~105 ms each in strict sequence. JEP 491 bought this row
nothing — the point: mutual exclusion never moved, only pinning did.

**The flag, swept** (proof 1): `maxInside` on JDK 21 tracks `parallelism` exactly; JDK 26 ignores it.

```
parallelism        1     2     4     8     (no flags = 12 cores on this laptop)
JDK 21 maxInside    1     2     4     8     12
JDK 26 maxInside   64    64    64    64     64
```

**JFR cross-check** (`-XX:StartFlightRecording` + `jfr print --events jdk.VirtualThreadPinned`).
Proof 1: **64** `jdk.VirtualThreadPinned` events on JDK 21 — 64 of the 64 threads pinned — **0**
on JDK 26. Proof 4 (StillPins): **0 events on BOTH JDKs** — that event fires only for pinning
caused by a native stack frame, and "waiting for another thread's class init" pins a different
way (parked in the VM, no native frame), so JFR stays silent either way; `maxInside` is the only
signal for this one.

**Why JDK 26 no longer pins here:** [JDK-8369238](https://bugs.openjdk.org/browse/JDK-8369238),
"Allow virtual thread preemption on some common class initialization paths" (fixVersion 26): "we
can reuse the same mechanism to preempt all virtual threads blocked waiting for a class to be
initialized along some of the most common initialization paths, in particular, when executing
`invokestatic`, `new`, `getstatic` and `putstatic` from the interpreter." `SlowInit.touch()` here
is an `invokestatic`. The fix targets only the *waiter*: a thread running `<clinit>` itself still
gets a `jdk.VirtualThreadPinned` event on JDK 26 here (`pinnedReason = "VM call to
...SlowInit.<clinit> on stack"`) — checked, not shipped as a fifth proof; one file answers one
question, and this one already has.

## When this demo would lie to you

- `elapsed` depends on JIT warm-up and this machine's scheduler and shifts on a cold JVM, a busier
  laptop or a different core count, though `maxInside` should not, as long as the `-D` flags are
  set; it is also not a real production number — 64 requests never arrive in one burst.
- StillPins's fix is interpreter-path only and flips under one flag: on JDK 26, `-Xint` leaves
  `maxInside` at 64 (measured); `-Xcomp` drops it to 2 (measured) — same class file, same JDK,
  opposite conclusion, because JDK-8369238 targets specific interpreted bytecodes only.
- Native-code pinning — "if a virtual thread calls native code, either through a native method or
  the Foreign Function & Memory API, and that native code calls back to Java code that performs a
  blocking operation or blocks on a monitor, then the virtual thread will be pinned" ([JEP 491,
  Diagnosing remaining cases of pinning](https://openjdk.org/jeps/491)) — real and current, but
  unmeasured here: it would need a native library to build.
- For actual throughput under realistic, staggered load, this demo is the wrong tool; reach for
  `jcstress` (correctness under concurrency) or a longer JFR recording under real traffic.
