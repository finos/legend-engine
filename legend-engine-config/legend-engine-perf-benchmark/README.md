# Pipeline performance benchmark

Times each stage of the query pipeline separately, so a slow query can be attributed to a phase
rather than to "the platform". Background, measurements and the hotspots this harness found are in
[finos/legend-engine#5140](https://github.com/finos/legend-engine/issues/5140).

## Phases

| Phase | What it measures |
|---|---|
| `parse` | `PureGrammarParser.parseModel` - grammar to `PureModelContextData` |
| `compile` | `Compiler.compile` - the `PureModel` constructor (multi-pass graph build and validation) |
| `lambda` | `HelperValueSpecificationBuilder.buildLambda` - building the query lambda |
| `extensions` | Building the Pure extension list |
| `planPure` | `PlanGenerator.generateExecutionPlanAsPure` - preeval, routing, clustering, pure-to-SQL, SQL-to-string |
| `javaBind` | `PlanPlatform.JAVA.bindPlan` - Java platform binding |
| `serialize` | `PlanGenerator.serializeToJSON` - protocol transform plus Pure `toJSON` |
| `deserialize` | `PlanGenerator.stringToPlan` - Jackson |
| `execute` | `PlanExecutor.execute` against in-memory H2 (only with `--execute`) |

Iteration 0 of a run is cold: it pays class loading and JIT for the generated Pure code base.
Steady-state cost is the median after `--warmup` iterations.

## Running

```bash
mvn install -DskipTests -pl legend-engine-config/legend-engine-perf-benchmark -am
mvn -q -pl legend-engine-config/legend-engine-perf-benchmark exec:java \
    -Dexec.mainClass=org.finos.legend.engine.perf.PipelineBench \
    -Dexec.args="--scale 1000 --query simple --iters 10 --execute"
```

Or build a classpath once and run the class directly, which is faster to iterate on and gives full
control of JVM flags for profiling:

```bash
mvn -q -pl legend-engine-config/legend-engine-perf-benchmark dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
java -Xmx8g -Xss8m -cp "legend-engine-config/legend-engine-perf-benchmark/target/classes:$(cat /tmp/cp.txt)" \
    org.finos.legend.engine.perf.PipelineBench --scale 1000 --query simple --iters 10
```

## Workload

`--scale N` sets the size of the generated model: N classes, N tables, N-1 joins, and a mapping
covering every class. The query stays fixed, so a scale sweep isolates how each phase reacts to
model size. Query shapes vary one dimension each:

| Shape | Varies |
|---|---|
| `simple` | filter plus three projected columns |
| `joinK` | navigation depth: projects across a K-deep property chain |
| `projW` / `longprojW` | projection width; `longproj` uses 390-character aliases to exercise dialect alias limits |
| `aggK` | number of aggregates in a `groupBy` |
| `graphK` | graph-fetch tree depth (the shape services use) |
| `semiK` / `semiwK` / `semijoinK` | semi-structured path depth, access width, and width combined with join navigation |
| `variantrel` | Variant functions through the relation path |
| `m2mview` | a model-to-model view over the relational mapping |

Mapping shape flags: `--nextmult 1|0..1` (chain multiplicity), `--union K` (Operation union over K
set implementations), `--includes K` (K-deep mapping include chain), `--milestoning`
(business-temporal), `--relfunc` (Relation `~func` mappings plus ModelJoin associations), `--m2m`
(model-to-model view via `ModelChainConnection`), `--semi D` (SEMISTRUCTURED column with a JSON
binding over a D-level model).

Other flags: `--dbtype H2|DuckDB|Snowflake` (plan generation works for any dialect without a
connection), `--iters`, `--warmup`, `--execute`, `--csv <path>`, `--dumpplan <path>`, `--pause`
(waits for a profiler to attach before measuring).

To run a model that already exists rather than a generated one:

```bash
--filelist files.txt --queryfile query.txt --mapping demo::MyMapping --db demo::MyDatabase
```

where `files.txt` lists `.pure` files one per line and `query.txt` holds the query body.

## Profiling

Plan generation runs as Java generated from Pure, so profiler frames are mangled generated class
names. `scripts/demangle_pure_frames.py` rewrites them back to Pure function names and reports self
and inclusive time per Pure function:

```bash
java ... org.finos.legend.engine.perf.PipelineBench --scale 1000 --query simple --iters 3000 &
asprof collect -d 30 -e wall -i 5ms -t -o collapsed -f profile.collapsed <pid>
grep '^\[main' profile.collapsed | sed 's/^\[main[^]]*\];//' > main.collapsed
scripts/demangle_pure_frames.py main.collapsed demangled.collapsed report.txt
```

Wall-clock mode matters: a CPU sampler under-reports class loading, lock and IO waits, which
dominate the cold path. On CI runners where `perf_events` is restricted, use `ctimer`.

## Suite runs, baselines and regression checks

A suite run executes a fixed set of workloads in one JVM and writes a results file describing both
the numbers and the machine they were measured on:

```bash
# create or update the stored baseline
java ... org.finos.legend.engine.perf.PipelineBench --suite default --rebase

# compare a run against the baseline; exits non-zero if it deviates beyond the margin
java ... org.finos.legend.engine.perf.PipelineBench --suite default

# keep the run's own results as well as comparing
java ... org.finos.legend.engine.perf.PipelineBench --suite default --out perf-results.json
```

Flags: `--baseline <path>` (default `perf-baseline.json`), `--out <path>`, `--rebase`,
`--margin 0.5` (allowed deviation for absolute phase medians), `--canary-margin 0.3` (allowed
deviation for ratios), `--min-delta-ms 25` (absolute deltas smaller than this are treated as noise),
`--allow-improvement` (only enforce the slower side), `--iters`, `--warmup`.

The results file records the environment, every workload's cold and warm phase timings, and the
computed canary ratios:

```json
{
  "schemaVersion": 1,
  "suite": "default",
  "generatedAt": "2026-09-10T00:27:16Z",
  "environment": {
    "os": "Mac OS X", "arch": "aarch64", "cpu": "Apple M4 Max",
    "availableProcessors": 16, "maxHeapMb": 8192,
    "javaVersion": "17.0.19", "jvmName": "OpenJDK 64-Bit Server VM",
    "jvmArgs": "[-Xmx8g, -Xss8m]", "gitCommit": "a314b6f...", "ci": false
  },
  "workloads": [
    {
      "workload": "simple@scale1000/H2",
      "config": { "scale": 1000, "query": "simple", "dbType": "H2", "...": "..." },
      "iterations": 5, "warmup": 2,
      "coldMs":       { "parse": 159, "compile": 114, "planPure": 12, "...": 0 },
      "warmMedianMs": { "parse": 152, "compile": 90,  "planPure": 13, "...": 0 },
      "planJsonChars": 2011
    }
  ],
  "canaries": [
    {
      "name": "compileCliffJoinDepth", "phase": "compile",
      "numerator": "join16@scale30/H2", "denominator": "join8@scale30/H2",
      "guards": "exponential lambda compilation on optional navigation chains",
      "value": 31.0
    }
  ]
}
```

## What the comparison enforces

Absolute durations move several-fold between machines, so the two kinds of check are treated
differently:

- **Deviation is judged in both directions.** A slower result is a regression. A faster one fails
  too, and says so separately: until the gain is recorded, the baseline still permits the old cost,
  so a later change could give the improvement back and the check would call it fine. Failing forces
  the new level into the baseline, where it protects the fix. Rerun with `--rebase` and commit the
  baseline in the same pull request as the change that earned it. `--allow-improvement` turns this
  side off.
- **Canary ratios are always enforced.** A ratio between two workloads measured in the same JVM
  cancels machine speed, so it is comparable between a laptop and a CI runner. These catch the
  regressions that matter - a phase silently changing complexity class.
- **Absolute phase medians are enforced only when the environment fingerprint matches** the
  baseline (same OS, architecture, CPU, processor count, heap and Java major version). On a
  different machine they are reported as advisory notes instead of failures. Deltas below
  `--min-delta-ms` are ignored either way, because millisecond phases are dominated by JIT noise.
  This is also why the improvement side does not fire on a faster machine: everything looks faster
  there, which says nothing about the code.

Baselines belong in a checked-in file updated by pull request, so an intentional performance change
is reviewed alongside the code that causes it - in either direction. `--rebase` is how that file is
regenerated.

The baseline committed here was measured on a developer machine, so on a CI runner the environment
fingerprint will not match and absolute timings stay advisory while canary ratios are enforced. Run
`--suite default --rebase` once on the runner class and commit that file to enforce both.

## In CI

The **Pipeline Benchmark** job in `.github/workflows/build.yml` runs this suite on every pull
request and compares it against the checked-in baseline. It reuses that workflow's build output
rather than building again, so it costs about the time the suite itself takes.

A deviation does not fail the build. The job reports it - in the job summary, and as a comment on
the pull request - and leaves the judgement to the reviewer, who can see whether the change
explains the movement. Blocking on it would be wrong for a measurement this environment-sensitive:
runner hardware differs from whatever machine recorded the baseline, and even ratios drift somewhat
between very different processors.

To accept the new numbers, run the **Performance Baseline** workflow
(`.github/workflows/performance.yml`) with `rebase` enabled and `ref` set to the pull request's
branch. The run records the results as the baseline and pushes them
as a commit on that branch, so the change in cost is reviewed in the same pull request as the change
that caused it. The manual trigger also takes an `args` field for extra flags, for example
`--margin 0.3 --iters 8`.

Two limitations worth knowing. A pull request from a fork gets a read-only token, so the comment is
best effort - the job summary always carries the same report - and a rebase must be run from the
fork or the baseline committed by hand.

And a baseline recorded on a developer machine will not match a runner's fingerprint, so absolute
timings are reported as notes. Ratios survive the move far better but not perfectly: measured
across an Apple M4 Max and a four-core cloud runner, five of six canaries landed inside a 30% band
while the union ratio came in 31% low, purely from the hardware. Recording the baseline on the
runner class the workflow uses removes that drift and makes absolute timings comparable again.

## Canary ratios

Absolute times vary several-fold between machines, so thresholds on them are either noisy or
useless. Ratios between two workloads measured in the same JVM cancel machine speed and catch the
regressions that matter - a phase silently becoming quadratic or exponential in some dimension.
Suggested canaries, with the shape each one guards:

| Canary | Guards |
|---|---|
| `compile(join16) / compile(join8)` | exponential lambda compilation on `[0..1]` navigation chains |
| `compile(scale 1000) / compile(scale 100)` | quadratic terms in mapping validation and table resolution |
| `planPure(union40) / planPure(union10)` | quadratic pure-to-SQL in union set implementations |
| `planPure(join10) / planPure(join2)` | pure-to-SQL superlinearity in navigation depth |
| `compile(relfunc 200) / compile(relfunc 50)` | per-column lambda compilation in Relation mappings |
| `javaBind(graph4) / javaBind(graph1)` | platform binder growth in graph-fetch tree size |

Baselines belong in a checked-in file updated by pull request, so an intentional performance change
is reviewed alongside the code that causes it.
