# Preeval P5 — results

Plan: `docs/superpowers/plans/2026-09-30-preeval-java-native-p5-benchmark-cutover.md`.

## Benchmark (Task 3, 2026-09-30, HEAD 06cc8376284)

AMD Ryzen 9 PRO 6950H, 16 CPUs, JDK 11.0.29, heap 8192 MB, Linux amd64. Command: `PipelineBench --preeval --iters 10 --warmup 3`, run twice back to back. The medians below are from run 2.

**Workloads.** Class-based workloads use `--direct-preval`, which pre-evaluates the query lambda directly (overload A, the SQL, lineage, OpenAPI and `SQLExecutor` path). This is needed because planning a class-based query with a mapping and runtime passed to the planner never calls `preval`. `relsort` is a Relation sort; its `ascending`/`descending` are prevalled on the router path.

| Workload | planPure ms PURE / JAVA | preval ms PURE / JAVA | speedup (PURE/JAVA) | spans |
|---|---|---|---|---|
| `simple@scale100+direct/H2` | 22 / 21 | 1.45 / 2.59 | 0.56 | 1 |
| `simple@scale1000+direct/H2` | 16 / 19 | 1.08 / 2.0 | 0.54 | 1 |
| `join8@scale30+direct/H2` | 42 / 40 | 4.63 / 3.12 | 1.48 | 1 |
| `agg4@scale30+direct/H2` | 12 / 13 | 2.65 / 2.47 | 1.07 | 1 |
| `groupBy@scale30+direct/H2` | 9 / 9 | 1.03 / 1.21 | 0.85 | 1 |
| `simple@scale50+relfunc+direct/H2` | 15 / 15 | 0.77 / 1.21 | 0.64 | 1 |
| `simple@scale30+milestoned+direct/H2` | 13 / 13 | 0.66 / 1.16 | 0.57 | 1 |
| `graph4@scale30+direct/H2` | 73 / 74 | 0.27 / 0.63 | 0.43 | 1 |
| `m2mview@scale30+m2m+direct/H2` | 17 / 16 | 0.24 / 0.6 | 0.4 | 1 |
| `relsort@scale30/H2` | 10 / 10 | 1.27 / 1.71 | 0.74 | 2 |

Totals: run 1 PURE 15.15 ms, JAVA 17.16 ms (×0.88); run 2 PURE 14.05 ms, JAVA 16.70 ms (×0.84). Both runs report no regression under the plan's rule. That is only because every preval here takes under 6 ms, so no delta can exceed the 10 ms noise floor. `planPure` differences are noise: except for `relsort`, plan generation does not call preval on these workloads.

## Hook cost (run 2, statistics pass under JAVA)

| Workload | JAVA preval ms | all hooks ms | `stopPreeval` calls / ms | rules fired |
|---|---|---|---|---|
| `simple@scale100` | 2.59 | 0.43 | 12 / 0.27 | none |
| `simple@scale1000` | 2.0 | 0.25 | 12 / 0.15 | none |
| `join8` | 3.12 | 0.97 | 96 / 0.86 | none |
| `agg4` | 2.47 | 1.08 | 28 / 0.32 | ReactivateRule 4 |
| `groupBy` | 1.21 | 0.33 | 10 / 0.12 | ReactivateRule 1 |
| `simple+relfunc` | 1.21 | 0.21 | 12 / 0.11 | none |
| `simple+milestoned` | 1.16 | 0.22 | 13 / 0.13 | none |
| `graph4` | 0.63 | 0.04 | 2 / 0.04 | none |
| `m2mview` | 0.6 | 0.04 | 2 / 0.04 | none |
| `relsort` | 1.71 | 0.41 | 16 / 0.21 | InlineRule 2, ReactivateRule 4 |

Hook callbacks are 7–43% of JAVA's preval time. So the §8 risk ("callbacks into Pure per node erase the gain") is **not** the main cost. The rest points to a fixed per-call overhead in the Java path of about 0.3–0.6 ms (e.g. `graph4`: 0.59 ms outside hooks, against 0.27 ms for the whole PURE preval). That overhead dominates small queries. Java wins only where the traversal is large (`join8`, ×1.48).

## Gate

**Stopped.** No regression under the rule, but the spec's P5 exit criterion also requires a **measured gain**, and there is none: JAVA is slower on 8 of 10 workloads and about 15% slower in total. Cutover (Task 6) is on hold pending the user's decision. The spec target gain is not set.
