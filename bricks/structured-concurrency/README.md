# structured-concurrency

**PREVIEW API.** `StructuredTaskScope` is a preview feature. This brick was written against
**JEP 533 — Structured Concurrency (Seventh Preview), JDK 27** — compiled and run with
`--release 27 --enable-preview`. Finalisation is proposed for JDK 28 (JEP 543, currently
**Candidate** status, not yet delivered). Until then the API can change, and so can this brick.

A `StructuredTaskScope` turns "remember to cancel the siblings, remember to stop the loser
threads, remember to enforce the deadline" from a discipline every caller has to maintain into
something the scope guarantees. These four numbers are what that guarantee actually buys, against
the `ExecutorService` code that does the same job by hand.

## The questions

| Question | Run it | Number on this machine | What the spec says |
|---|---|---|---|
| The dashboard loader's order service fails at 100 ms, its user service would take 5 s — how long until the caller has the exception? | `mise run structured-concurrency:sibling-cancellation` | ExecutorService 5007–5012 ms → StructuredTaskScope 107–110 ms. *`DashboardLoaderExecutor` reads the user future first; a loader that happened to read the failing future first would get ~110 ms too — it depends entirely on join order. The scope's number never does.* | "If one of the `findUser()` or `fetchOrder()` subtasks fails, by throwing an exception, then the other is cancelled, i.e., interrupted, if it has not yet completed." — JEP 533, Description — [openjdk.org/jeps/533](https://openjdk.org/jeps/533) |
| The caller gives up waiting — how many subtasks are still running 1 s later? | `mise run structured-concurrency:leaked-threads` | ExecutorService 3 subtasks still running → StructuredTaskScope 0 subtasks still running | "The `close` method always waits for threads executing subtasks to finish, even if the scope is cancelled." — JEP 533, Description → Cancellation — [openjdk.org/jeps/533](https://openjdk.org/jeps/533) |
| A 300 ms deadline on a 5 s task — elapsed and is the subtask still running 500 ms later, hand-rolled `future.get(timeout)` vs. `withTimeout`? | `mise run structured-concurrency:timeout` | ExecutorService 307–313 ms, 1 subtask still running → StructuredTaskScope 306–309 ms, 0 subtask still running | "if the timeout expires before or while waiting in `join()` then the scope is cancelled, which cancels all incomplete subtasks, and `join()` throws an `ExecutionException` with a `CancelledByTimeoutException` as the cause." — JEP 533, Description → Configuration — [openjdk.org/jeps/533](https://openjdk.org/jeps/533) |
| Three mirrors race, one is fast — elapsed, and how many losers are still running 150 ms later? | `mise run structured-concurrency:first-wins` | ExecutorService 112–114 ms, 2 losers still running → StructuredTaskScope 105–112 ms, 0 losers still running | "As soon as one subtask succeeds, the scope is cancelled, cancelling the unfinished subtasks, and `join()` returns the result of the successful subtask." — JEP 533, Description → Joiners — [openjdk.org/jeps/533](https://openjdk.org/jeps/533) |

Without mise: `cd bricks/structured-concurrency && JAVA_HOME="$(mise where java@27.0.0)" mvn -q compile`,
then run any of `dev.lukasgrigis.foundations.structuredconcurrency.proof.{SiblingCancellation,LeakedThreads,Timeout,FirstWins}`
with `java --enable-preview -cp target/classes <FQCN>` on that same JDK 27.

## The code you would actually write

[`example/DashboardLoaderExecutor.java`](src/main/java/dev/lukasgrigis/foundations/structuredconcurrency/example/DashboardLoaderExecutor.java)
is the dashboard loader most codebases have today: an `ExecutorService`, three `submit()` calls,
three `Future.get()` calls in a row. It reads fine, and it's the bug most reviewers miss — when
one `get()` throws, the futures not read yet are never told; nothing in the method ever calls
`Future.cancel()` on them, so their work keeps running on borrowed threads for a request that has
already failed.
[`example/DashboardLoaderScope.java`](src/main/java/dev/lukasgrigis/foundations/structuredconcurrency/example/DashboardLoaderScope.java)
is the fix: one scope, three forks, one `join()`. The default policy is "fail if any subtask
fails", so the moment any collaborator fails, the other two are cancelled before `join()`
returns — there is no `Future.cancel()` to forget because there is no `Future` to forget it on.
[`example/Collaborators.java`](src/main/java/dev/lukasgrigis/foundations/structuredconcurrency/example/Collaborators.java)
holds the plain interfaces and records both loaders share, including the `Page` they return — no
HTTP, no Spring. `proof.SiblingCancellation` runs both loaders exactly as they are, against stub
services: the user service takes 5 s, the order service fails after 100 ms.

## Recorded run

Apple M2 Pro, macOS 26.5.1. `openjdk 27 2026-09-15` (build 27+35-2325), installed via mise, run
with `--enable-preview`. 21.09.2026. The block below is one real, unedited `mise run
structured-concurrency`; the table under it is the range over five runs — where the number moved,
the range is reported, not the best run, and a constant count is reported as constant, not as a
range.

```
================= SiblingCancellation =================
ExecutorService  5012 ms
StructuredTaskScope  107 ms

================= LeakedThreads =================
ExecutorService  3 subtasks still running
StructuredTaskScope  0 subtasks still running

================= Timeout =================
ExecutorService  307 ms, 1 subtask still running
StructuredTaskScope  306 ms, 0 subtask still running

================= FirstWins =================
ExecutorService  113 ms, 2 losers still running
StructuredTaskScope  105 ms, 0 losers still running
```

| Proof | ExecutorService (5 runs) | StructuredTaskScope (5 runs) |
|---|---|---|
| SiblingCancellation | 5007–5012 ms | 107–110 ms |
| LeakedThreads | 3 subtasks (constant) | 0 subtasks (constant) |
| Timeout | 307–313 ms, 1 subtask (constant) | 306–309 ms, 0 subtask (constant) |
| FirstWins | 112–114 ms, 2 losers (constant) | 105–112 ms, 0 losers (constant) |

## When this demo would lie to you

- **These are elapsed-time and live-subtask-count numbers, not a benchmark.** No warm-up loop,
  no steady state, one JVM invocation per run. For throughput or GC/scheduler behaviour under
  real load, JFR or a proper harness is the right tool, not `System.nanoTime()` around four lines.
- **The "still running" counts are an `AtomicInteger` incremented and decremented inside the
  task itself**, not a thread count — a fixed-size pool's core threads sit alive forever whether
  they are doing anything or not, so counting threads would have measured the pool, not the leak.
  This also means the two variants in each proof use separate counters: a slow loser from the
  first variant can still be mid-sleep when the second variant starts, and a shared counter would
  let it decrement into the wrong measurement.
- **The `ExecutorService` numbers describe common, naive code, not the only way to write it.** A
  team that always remembers `Future.cancel(true)` on siblings, or reaches for
  `invokeAll(tasks, timeout, unit)` for a deadline (which behaves close to the scope — same
  elapsed, same "0 still running" — because it cancels its tasks on timeout, unlike a bare
  `future.get(timeout)`), gets closer to the scope's behaviour by hand. `StructuredTaskScope` is
  what makes that the default instead of a discipline every caller has to maintain — that
  discipline gap is the actual subject of this brick, not raw speed.
- **This is JDK 27's seventh preview, not the finished API.** The third `R_X` type parameter,
  `ExecutionException`-wrapping on every built-in joiner, and `Joiner.onTimeout()` replaced by
  `Joiner.timeout()` all changed between JDK 26 (JEP 525) and JDK 27 (JEP 533), and can change
  again before JEP 543 finalises the API, currently targeted at JDK 28.
- **Single machine, single architecture.** All four proofs ran on Apple silicon; absolute
  millisecond values will differ elsewhere, and the 150/300/500/1000 ms windows chosen here may
  need widening on a slower or more loaded box for the counts to still land cleanly.
- **SiblingCancellation's gap is real but order-dependent** (see the row above) — it is the one
  number in this brick that a different natural coding of the same ExecutorService logic can
  make disappear.

Root [README.md](../../README.md) lists every brick.
