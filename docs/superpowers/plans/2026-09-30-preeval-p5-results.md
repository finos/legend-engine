# Preeval P5 — results

Plan: `docs/superpowers/plans/2026-09-30-preeval-java-native-p5-benchmark-cutover.md`.

## Initial benchmark (Task 3, 2026-09-30, HEAD 06cc8376284)

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

## Initial gate

Stopped: no regression under the rule, but no measured gain either (JAVA about 15% slower in total). The user chose to optimise, then re-run.

## Optimisation (2026-09-30)

**Method.** Two probes, not committed, compared PURE and JAVA on one workload at a time:
- a tight loop of direct `preval` calls on one model;
- the first `preval` call on each of many fresh models.

JFR execution sampling (1 ms) attributed the JAVA time.

**Root cause.** The benchmark compiles a new `PureModel` every iteration, so every measured call is the first call on a fresh model. On a reused model, JAVA was already 1.0–2.8× faster. The first call on a fresh model was slower because of per-model work the Pure implementation never does:

1. **Function types through processor support** (46% of JAVA samples on fresh models). `CompiledPrevalRuntime.parameterNames` used `processorSupport.function_getFunctionType`, which computes the type and caches it in a per-model `Context` map that starts empty. Pure's `functionType()` reads `classifierGenericType.typeArguments[0].rawType` directly.
   - **Fix** (`5a115316b0a`): read it the same way for function definitions and native functions, falling back to processor support otherwise.
   - First call on a fresh model: `simple` JAVA 1405 → 764 µs (PURE 1127); `join8` JAVA 2192 → 1362 µs (PURE 3098).
2. **Element lookups per call.** The compiled runtime is built per `preval` call, so its path → element cache was discarded after every call, and each call repeated its `package_getByUserPath` walks.
   - **Fix** (`fe3c2cc96b7`): cache per model (weakly keyed on the execution support).
   - This cannot help a model's first call, but on a reused model JAVA steady state dropped further: `graph4` 38 → 17 µs, `simple` 114 → 88, `agg4` 461 → 404, `join8` 657 → 615.
   - A cache shared across models was rejected. Test harnesses build separate Pure runtimes in one class loader, so it could return another runtime's element.

**Steady state on a reused model (µs per call, after both fixes):**

| Workload | PURE | JAVA | speedup |
|---|---|---|---|
| `graph4@scale30` | 39 | 17 | ×2.3 |
| `simple@scale30` | 189 | 88 | ×2.1 |
| `agg4@scale30` | 823 | 404 | ×2.0 |
| `join8@scale30` | 1840 | 615 | ×3.0 |

## Benchmark after optimisation (2026-09-30, HEAD fe3c2cc96b7)

Same machine and command, two runs. Medians from run 2:

| Workload | planPure ms PURE / JAVA | preval ms PURE / JAVA | speedup (PURE/JAVA) |
|---|---|---|---|
| `simple@scale100+direct/H2` | 23 / 21 | 1.64 / 1.38 | 1.19 |
| `simple@scale1000+direct/H2` | 18 / 16 | 1.2 / 1.07 | 1.12 |
| `join8@scale30+direct/H2` | 43 / 42 | 4.77 / 2.16 | 2.21 |
| `agg4@scale30+direct/H2` | 12 / 13 | 2.71 / 2.12 | 1.28 |
| `groupBy@scale30+direct/H2` | 9 / 9 | 1.03 / 0.99 | 1.04 |
| `simple@scale50+relfunc+direct/H2` | 14 / 15 | 0.7 / 0.64 | 1.09 |
| `simple@scale30+milestoned+direct/H2` | 13 / 13 | 0.72 / 0.72 | 1.0 |
| `graph4@scale30+direct/H2` | 70 / 71 | 0.26 / 0.42 | 0.62 |
| `m2mview@scale30+m2m+direct/H2` | 16 / 16 | 0.24 / 0.4 | 0.6 |
| `relsort@scale30/H2` | 9 / 10 | 1.25 / 1.26 | 0.99 |

Totals: run 1 PURE 14.4 ms, JAVA 11.58 ms (×1.24); run 2 PURE 14.52 ms, JAVA 11.16 ms (×1.30). No regressions under the rule.

**Remaining gap.** `graph4` and `m2mview` are the two smallest queries: 2 hook calls each, about 0.25 ms under PURE. There, JAVA is still about 0.15 ms slower on a fresh model's first call. The remaining per-model cost is spread thinly:
- the Pure-side glue for the Java path (`prevalJava`, `toPrevalHooks`);
- the native entry;
- the first `package_getByUserPath` walks on each model.

Removing the walks needs `instanceof` or metadata-index lookups instead of paths, which is a larger refactor. On a reused model these queries are 2.3× faster under JAVA.

## Gate (after optimisation)

**Passed.**
- **Measured gain:** ×1.24–1.30 in total on the fresh-model benchmark, which is pessimistic for JAVA, and ×2.0–3.0 at steady state on a reused model.
- **No regression** under the plan's rule. The two tiny fresh-model cases are recorded above.
- **Target gain** (spec §4.5): total preval ×1.24–1.30 across the ten comparison workloads on fresh models, and ×2–3 at steady state.
