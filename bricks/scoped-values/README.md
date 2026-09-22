# scoped-values

`ThreadLocal` has done two jobs for years: a per-thread cache for objects that are expensive to build, and a
carrier for request context down the call stack. Virtual threads break the first job, because a thread no longer
outlives its task. Scoped values (JEP 506, final since JDK 25) replace the second, because a binding can neither
outlive its scope nor be changed from below.

## The questions

| Question | Run it | Number on this machine | What the spec says |
|---|---|---|---|
| 10,000 tasks each format a timestamp through a `ThreadLocal`-cached `SimpleDateFormat` — how many formatters get built on a pool of 8 platform threads, on virtual threads, and with one shared `DateTimeFormatter`? | `mise run scoped-values:formatter-instances` | pool of 8: **8** · virtual threads: **10,000** · shared `DateTimeFormatter`: **1** (5/5 runs) | "If you migrate code from using a thread pool to using a virtual thread per task, be wary of usages of this idiom since creating an expensive resource for every virtual thread may degrade performance significantly." — [JEP 444, Do not pool virtual threads](https://openjdk.org/jeps/444) |
| Every other request sets its tenant and forgets to clear it — how many of the 500 anonymous requests in between run as the tenant before them? | `mise run scoped-values:context-leak` | `ThreadLocal` on a pool of 1: **500 of 500** · `ThreadLocal` on virtual threads: **0** · `ScopedValue` on a pool of 1: **0** (5/5 runs) | "In particular, if a thread pool is used, the value of a thread-local variable set in one task could, if not properly cleared, accidentally leak into an unrelated task, potentially leading to dangerous security vulnerabilities." — [JEP 506, Problems with thread-local variables](https://openjdk.org/jeps/506) |
| A helper deep in the call stack switches the tenant for one lookup — which tenant does its caller see once the helper has returned? | `mise run scoped-values:write-back` | `ThreadLocal`: **tenant-b** · `ScopedValue`: **tenant-a** (5/5 runs) | "Any code that can call the get method of a thread-local variable can call the set method of that variable at any time." — [JEP 506, Problems with thread-local variables](https://openjdk.org/jeps/506). "There is no set method that lets faraway code change the scoped value at any time." — [JEP 506, Description](https://openjdk.org/jeps/506) |

Without mise: `cd bricks/scoped-values && mvn -q compile`, then run any of
`dev.lukasgrigis.foundations.scopedvalues.proof.{FormatterInstances,ContextLeak,WriteBack}` with
`java -cp target/classes <FQCN>` on JDK 27.

## The code you would actually write

[`example/ThreadLocalDateFormat.java`](src/main/java/dev/lukasgrigis/foundations/scopedvalues/example/ThreadLocalDateFormat.java)
is the cache idiom from before virtual threads: one `SimpleDateFormat` per thread, built on first use. It is not
thread-safe and not cheap to build, so on a pool the trick paid off — 8 threads, 8 formatters, reused all day. On
virtual threads every task gets a fresh thread, so the cache builds one formatter per task and never hits.
[`example/SharedDateFormat.java`](src/main/java/dev/lukasgrigis/foundations/scopedvalues/example/SharedDateFormat.java)
is the fix for that job: `DateTimeFormatter` is immutable and thread-safe, so one instance serves every thread.
The expensive object never belonged to a thread.

[`example/ThreadLocalTenant.java`](src/main/java/dev/lukasgrigis/foundations/scopedvalues/example/ThreadLocalTenant.java)
carries the tenant of a request down the call stack. Its `runAs` is careful and puts the previous tenant back
in a `finally`, but `set` is public too, as in MDC or SecurityContextHolder, because one place sets the tenant
and another clears it — so any code that can read it can change it.
[`example/ScopedTenant.java`](src/main/java/dev/lukasgrigis/foundations/scopedvalues/example/ScopedTenant.java) is
the same holder on a `ScopedValue`: a tenant is bound for one `runAs` call and gone when it returns, and there is
no setter. Code further down can only run its own callees with another tenant, in a nested scope.

`ThreadLocal` is not deprecated, and libraries rely on it. The sort is for your own code: context down the call
stack becomes a scoped value, an expensive object becomes one shared, immutable instance.

## Recorded run

Apple M2 Pro, macOS 26.5.1. `openjdk 27 2026-09-15` (build 27+35-2325), installed via mise, classes compiled
with `--release 27`. 2026-09-22, five runs. All five were identical, so the block below is every run:

```
================= FormatterInstances =================
platform pool of 8  ThreadLocal<SimpleDateFormat>  8 formatters for 10000 tasks
virtual threads     ThreadLocal<SimpleDateFormat>  10000 formatters for 10000 tasks
virtual threads     shared DateTimeFormatter       1 formatter for 10000 tasks

================= ContextLeak =================
platform pool of 1  ThreadLocal  500 of 500 anonymous requests ran as a stale tenant
virtual threads     ThreadLocal  0 of 500 anonymous requests ran as a stale tenant
platform pool of 1  ScopedValue  0 of 500 anonymous requests ran as a stale tenant

================= WriteBack =================
ThreadLocal  caller sees tenant-b after the call
ScopedValue  caller sees tenant-a after the call
```

## Finding yours

JEP 444, Thread-local variables: "The system property jdk.traceVirtualThreadLocals can be used to trigger a stack
trace when a virtual thread sets the value of any thread-local variable." It also fires on the first `get()` of a
`withInitial`, because that is where the initial value is set. One virtual thread calling
`ThreadLocalDateFormat.formatter()` with `-Djdk.traceVirtualThreadLocals=true`:

```
VirtualThread[#25]/runnable@ForkJoinPool-1-worker-1
    java.base/java.lang.ThreadLocal.setInitialValue(ThreadLocal.java:214)
    java.base/java.lang.ThreadLocal.get(ThreadLocal.java:193)
    java.base/java.lang.ThreadLocal.get(ThreadLocal.java:171)
    dev.lukasgrigis.foundations.scopedvalues.example.ThreadLocalDateFormat.formatter(ThreadLocalDateFormat.java:22)
    java.base/java.lang.VirtualThread.run(VirtualThread.java:470)
```

Run `FormatterInstances` with the flag and it prints 10,000 of these, one per virtual thread in its second row.
The platform-pool row prints none — the flag only watches virtual threads — and the shared-formatter row prints
none because it sets no thread-local at all. Every hit is either context, which becomes a scoped value, or an
expensive object, which becomes one shared instance.

## When this demo would lie to you

- **`FormatterInstances` counts formatters, not time.** What one formatter costs depends on the pattern, the
  JIT and the machine; the count is the part that scales with the number of tasks. For how much slower the
  10,000-formatter row runs, a JMH harness is the right tool, not this proof.
- **The pool row is 8 because the pool keeps its 8 threads.** A cached pool whose idle threads expire, or a
  pool that replaces a thread after an exception, builds more — the cache is only as good as the lifetime of
  its threads.
- **`ContextLeak` runs on a pool of one thread on purpose**, so every anonymous request inherits exactly the
  tenant of the request before it. A bigger pool leaks about as often once each thread has served one tenant (a
  pool of 8 left 489–496 of 500 stale here). What changes is which tenant a request inherits, and that is what
  makes the real bug hard to find.
- **The `ThreadLocal` rows need a bug** — a missing `remove()` in `ContextLeak`, a missing restore in
  `WriteBack`. Careful code (`ThreadLocalTenant.runAs`) makes neither mistake, but with `ThreadLocal` "careful"
  means saving and restoring the previous value by hand at every entry point — miss one call site and the leak
  is back. `ScopedValue` gets that save-and-restore for free from its nested-scope structure: there is nothing
  to forget, no `remove`, no `set`.
- **"virtual threads: 0 stale" is not a fix, it is a side effect.** Each task gets a fresh thread, so there is
  nothing to leak into. The same `ThreadLocal` code leaks again the moment it runs on a platform-thread pool
  somewhere else in the application.
- **Scoped values reach child threads only through `StructuredTaskScope`**, which is still a preview API in
  JDK 27 (JEP 533). Inheritance is therefore not measured here; the
  [structured-concurrency](../structured-concurrency/) brick covers the scope itself.

Root [README.md](../../README.md) lists every brick.
