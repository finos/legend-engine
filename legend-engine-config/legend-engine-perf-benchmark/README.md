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
| `joinK` | navigation depth: projects across a K-deep property chain (compile and lambda build grow about 1.9x per level, so keep K small; use `hop` for deep join trees) |
| `ujoinK` | `joinK` then `restrict` to the first column, so every join is unread |
| `projW` / `longprojW` | projection width; `longproj` uses 390-character aliases to exercise dialect alias limits |
| `aggK` | number of aggregates in a `groupBy` |
| `graphK` | graph-fetch tree depth (the shape services use) |
| `semiK` / `semiwK` / `semijoinK` | semi-structured path depth, access width, and width combined with join navigation |
| `variantrel` | Variant functions through the relation path |
| `m2mview` | a model-to-model view over the relational mapping |
| `conjK` | number of root filter conjuncts, each copied through a join into every aggregating view subquery (needs `--aggviews`) |
| `starK` / `ustarK` | K sibling left joins from the root, all read / all unread after a `restrict` (needs `--star`) |
| `dupK` | K pairs of left joins that repeat each other: same table, same condition, two join names (needs `--star`) |
| `hop` / `uhop` | one property mapped through a `--hops`-long join chain, read / unread after a `restrict` |
| `towerN` / `towerstarN` | N wrapper selects stacked by `limit` + `filter` layers over `--width` columns (default 20), alone / over a `--star` of left joins; the optimizer has nothing to shrink, so this measures its cost on a deep tree |
| `winK` | K nested `olapGroupBy` window layers |
| `caseK` | K select-list columns, each a conditional over three OR-ed comparisons |
| `andK` / `orandK` / `inK` | one filter of K conjuncts / K OR-ed pairs of conjuncts / membership of a list of K values, over `--width` columns |
| `trimW` | a limited subselect of W columns of which the outer select keeps one: the optimizer drops the wrapper and W-1 columns |

Mapping shape flags: `--nextmult 1|0..1` (chain multiplicity), `--union K` (Operation union over K
set implementations), `--includes K` (K-deep mapping include chain), `--milestoning`
(business-temporal), `--relfunc` (Relation `~func` mappings plus ModelJoin associations), `--m2m`
(model-to-model view via `ModelChainConnection`), `--semi D` (SEMISTRUCTURED column with a JSON
binding over a D-level model), `--aggviews V` (V `~groupBy` views joined to the root table, each
reachable from `C0` through a to-one property), `--star K` (K pairs of to-one properties on `C0`, each
pair mapped through two differently named joins with the same condition `T0.fk = Ti.id`), `--hops K`
(a property `hop` on `C0` mapped through the K-join chain `@J0 > ... > @J(K-1)`).

Other flags: `--dbtype H2|DuckDB|Snowflake` (plan generation works for any dialect without a
connection), `--sqlopt all|nojoinrules|none` (query-level SQL optimizer flags: every rule, every rule but
the two left-join rules, or no optimization), `--iters`, `--warmup`, `--execute`, `--csv <path>`, `--dumpplan <path>`, `--pause`
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
`--record <results.json>` (write a results file measured elsewhere into the baseline without
re-running the suite),
`--margin 0.5` (allowed deviation for absolute phase medians), `--canary-margin 0.3` (allowed
deviation for ratios), `--min-delta-ms 25` (absolute deltas smaller than this are treated as noise),
`--allow-improvement` (only enforce the slower side), `--iters`, `--warmup`, `--rounds 3` (run the
whole suite three times in one JVM and take each phase's median over the rounds; use it when recording a
baseline and in CI, since a single round on a shared machine moves some canaries by 30-60%), `--only <text>` (run only
the workloads whose id contains the text, to try one shape; the checks then see the rest as missing).

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

The baseline holds **one entry per environment**, keyed by a fingerprint of the operating system,
architecture, CPU, processor count, heap size and Java major version. A run is compared against the
entry for the machine it is running on:

- **An entry for this machine exists.** Both ratios and absolute phase medians are compared against
  it, in both directions, since they describe the same hardware. Deltas below `--min-delta-ms` are
  ignored, because millisecond phases are dominated by JIT noise.
- **No entry for this machine.** Nothing is checked. The run prints its own canary ratios and says
  which environments the baseline does know about, so the next step is obvious: record one here with
  `--rebase`.

Nothing is compared across machines, because nothing survives the trip intact. Absolute timings
plainly do not. Ratios travel better but not far enough to trust: the same commit measured on an
Apple M4 Max and in a Linux container on the same host put the compile-cliff canary at 30.0 and
36.7, a 22% gap from environment alone. A single shared baseline would have spent that budget on
hardware instead of on code.

SQL sizes are enforced the same way. Every workload records the size of the SQL in its plan (statements,
characters, selects, joins, left joins and aliased columns, summed over all statements). Unlike a
timing it is deterministic, so a plan more than 2% larger fails, and so does one more than 2% smaller
until the new size is recorded. That is what notices a rule that has stopped shrinking a query it used
to shrink, which no timing shows: a guard that is too tight, or a pass that skips a rewrite, makes
plan generation faster and the SQL larger. Compare a workload with its `+sqlopt-none` twin to see what
the optimizer removes.

A machine with no entry of its own still gets the size check, against the first entry that holds sizes, since
sizes do not depend on the machine; its timings are only reported.

Recording is additive. `--rebase` writes the entry for the machine it runs on and leaves every other
entry untouched, so a laptop baseline and a CI baseline live in the same committed file without
overwriting each other.

Deviation is judged in both directions. A slower result is a regression. A faster one is reported
separately, because until the gain is recorded the baseline still permits the old cost, and a later
change could give it back unnoticed. Rerun with `--rebase` and commit the baseline in the same pull
request as the change that earned it. `--allow-improvement` turns that side off.

## Measuring a second environment with Docker

`scripts/run-in-container.sh` runs the suite inside a Linux container, so another environment can be
added without another machine. It mounts the repository and the Maven repository at the paths they
have on the host, so the classpath resolved outside works unchanged inside.

```bash
scripts/run-in-container.sh --rebase            # record this container as an environment
scripts/run-in-container.sh                     # compare against that entry later
```

`PERF_IMAGE` and `PERF_HEAP` override the image and heap. A chained local Maven repository is
supported through `MAVEN_REPO_LOCAL` and `MAVEN_REPO_LOCAL_TAIL`. Note that the heap is part of the
fingerprint, so changing `PERF_HEAP` describes a different environment and needs its own entry.

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
best effort - the job summary always carries the same report - and the Performance Baseline workflow
cannot push a rebase to the fork's branch.

And a runner will not match a baseline recorded on a developer machine, so until someone records an
entry from the runner itself, the job reports its numbers and checks nothing. There are two ways to
add that entry. Run the Performance Baseline workflow once against the default branch, or download
the `perf-results` artifact from any Pipeline Benchmark run and feed it in:

```bash
gh run download <run-id> -n perf-results -D /tmp/perf
java ... org.finos.legend.engine.perf.PipelineBench --record /tmp/perf/perf-results.json
```

The second path is the one that works for a fork, and it is how the hosted-runner entry in the
committed baseline was recorded. Either way, every pull request after it is compared against numbers
measured on the same hardware.

Hosted runners are not a single machine. `ubuntu-latest` is backed by more than one processor model,
and the fingerprint includes the CPU, so a run landing on a different SKU misses the entry and falls
back to checking nothing. That is the intended failure mode - a silent no-op is better than a false
regression - but it means the baseline needs an entry per SKU that shows up, added the same way.

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
| `planPure(conj10) / planPure(conj4)` with `--aggviews 10` | SQL optimization superlinearity in the number of filter conjuncts pushed into subqueries |
| `planPure(dup40) / planPure(dup10)` with `--star 40` | SQL optimization superlinearity in the number of repeated left joins merged |
| `planPure(star40) / planPure(star10)` with `--star 40` | SQL optimization superlinearity in the number of read left joins checked for elimination |
| `planPure(uhop)` with `--hops 40` / with `--hops 10` | SQL optimization superlinearity in the depth of an unread left-join chain |
| `planPure(tower80) / planPure(tower20)` | SQL optimization superlinearity in the number of nested wrapper selects |
| `planPure(towerstar25) / planPure(towerstar8)` with `--star 40` | SQL optimization superlinearity in the depth of a wrapper tower over a star of left joins |
| `planPure(win20) / planPure(win5)` | SQL optimization superlinearity in the number of nested window layers |
| `planPure(case200) / planPure(case50)` | SQL optimization superlinearity in the number of conditional select-list columns |
| `planPure(and300) / planPure(and100)` | SQL optimization superlinearity in the number of conjuncts of one filter |
| `planPure(orand200) / planPure(orand50)` | SQL optimization superlinearity in the size of one flat OR-of-AND predicate |
| `planPure(trim200) / planPure(trim20)` | SQL optimization superlinearity in the number of columns trimmed from a subselect |
| `planPure(towerN) / planPure(towerN, --sqlopt none)`, likewise `towerstar25`, `win20`, `case200`, `and300`, `orand200`, `trim200` | what the optimizer adds to plan generation for a shape, measured against the same query with the optimizer off in the same JVM |

The last group guards the cost of the optimizer itself, so that a new rule, or a change to an existing
one, cannot make these shapes much more expensive to plan. The shapes are modelled on the services where
the optimizer added the most plan-generation time in a run over every service at HEAD. Over 91 of them,
the optimizer's relative cost (plan time with it on divided by plan time with it off) rose steadily with
the nesting depth of the SQL, from a median of 1.26 for depth up to 3 to 1.66 for depth 15 and over (the
deepest real statements nest 28 selects), and was highest for statements with thousands of CASE
expressions (1.73). It did not depend on the number of joins: statements with fewer than 50 joins and
with more than 1000 had the same median, about 1.3. The towers, window layers and conditional columns
therefore carry most of the guarding; the join shapes stay because the join rules once scaled badly.

Baselines belong in a checked-in file updated by pull request, so an intentional performance change
is reviewed alongside the code that causes it.
