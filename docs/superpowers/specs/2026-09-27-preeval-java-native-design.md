# Preeval as a Java-backed Pure native — Design

**Date:** 2026-09-27
**Status:** Approved design, pending implementation plan
**Scope:** Reimplement `meta::pure::router::preeval` (plan-time partial evaluation) in Java behind a Pure
native, on Java 11; plan the move to Java 17+/21 as a separate track.

## 1. Goals

This is a pilot for moving platform features from Pure to Java-backed natives. Success is judged on
three equal axes:

1. **Performance** — measurable reduction in preeval time inside plan generation, no regression on any
   benchmarked query.
2. **Maintainability** — the rewrite rules live in typed, debuggable, unit-testable Java.
3. **Platform template** — the module layout, runtime port and parity strategy are reusable for the
   next Pure feature to move.

Non-goals: changing preeval's behaviour, changing any public Pure signature, or changing the
`RouterExtension` extension points.

## 2. Current state

- Implementation: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/preeval.pure`
  (~1,265 lines). Architecture reference: `docs/engineering/architecture/preeval.md`.
- Public overloads (names used below):
  - **A** `preval<T>(fd, ext)` / **B** `preval<T>(fd, ext, debug)` — return a `FunctionDefinition`.
  - **C/D** `preval(fd, vars, inScopeVars, ext[, debug])` — return `PrevalWrapper`.
  - **E** `preval(fd, State, ext)` — wrapped in `traceSpan('preval')`.
  - **F** `preval(FunctionExpression, inScopeVars, ext, debug)` — wrapped in `traceSpan('preval')`.
- Callers:
  - Router hot path: `router_routing.pure:241` runs **F** on every `NormalizeRequiredFunction`
    expression; `store/routing.pure` runs **B/F** at ~8 sites resolving `from`/`with`/mutation arguments.
  - Service extension (`extension.pure:28`, **F**), SQL `fromPure.pure:251,319` (**A**), lineage
    `fullAnalytics.pure:167,286` (**A** plus a local `shouldStopPreeval`), OpenAPI `pureToOpenApi.pure:172`
    (**A**), `SQLExecutor.java:385` (**A** via generated Java).
  - No production caller constructs a custom `State`; `getPreevalStateWithAdditionalStopInlineFunc`
    is test-only.
- Extension points:
  - `RouterExtension.shouldStopPreeval` — relational (`storeContract.pure:72`), Elasticsearch 7
    (`store_contract.pure:79`), lineage (local).
  - `RouterExtension.shouldStopRouting` via `routing::shouldStop` — feeds `defaultFunctionInlineStrategy`.
- Tests: `preeval/tests.pure` (101 tests, 4 `ToFix`, 1 excluded on Java compiled) and relational
  `router/tests/testPreeval.pure` (1 test, 2 `ToFix`), via `assertRoundTrip` (vX_X_X protocol-JSON
  comparison plus evaluation-equivalence for zero-arg functions). Run compiled-only through
  `Test_Pure_Core` and `Test_Pure_Relational`. No interpreted coverage.

## 3. Architecture

### 3.1 Layering

```
compiled-core (repo "core")               ── preval overloads A–F, State, PrevalWrapper, strategies, hooks builder
        │ depends on
core_functions_preeval (new repo)         ── native prevalNative, Class PrevalHooks
        │ depends on
legend-pure (m3 / m4 / runtime engines)
```

The Java core depends only on legend-pure APIs. It never imports a compiled-core `Root_*` class. That
avoids a cycle: compiled-core's generated code calls the native, so the native cannot depend on
compiled-core's generated classes.

### 3.2 Modules

New family `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/`,
following the `functions-unclassified` pattern:

| Module | Contents |
|---|---|
| `legend-engine-pure-functions-preeval-pure` | Repo `core_functions_preeval`: `native function meta::pure::functions::preeval::prevalNative(...)`, `Class PrevalHooks`, PCT/extension wiring as required by the repo definition. |
| `legend-engine-pure-runtime-java-extension-shared-functions-preeval` | The mode-agnostic Java core, package `org.finos.legend.engine.pure.preeval`. |
| `legend-engine-pure-runtime-java-extension-compiled-functions-preeval` | Native shell (`PrevalNative`) via `AbstractNativeFunctionGeneric` pointing at the static entry method `CompiledPreeval.preval`; `CompiledExtension` registration. `CompiledPrevalRuntime` is added in P1. |
| `legend-engine-pure-runtime-java-extension-interpreted-functions-preeval` | Native shell (`PrevalNative`), a `NativeFunction`; `InterpretedExtension` registration. `InterpretedPrevalRuntime` is added in P1. |

`core.definition.json` gains a `core_functions_preeval` dependency. Compiled-core's pom gains the
`-pure` and `-compiled` modules, mirroring `functions-unclassified`. The PureIDE light server gains the
`-interpreted` module.

### 3.3 Pure side

- **`prevalNative`** takes the function definition or function expression, the in-scope variables,
  the rolling in-scope variables, a `PrevalHooks` and a `DebugContext`. It returns a
  `meta::pure::functions::preeval::PrevalResult` with `value`, `canPreval`, `openVars` and `modified`.
  This result class lives in `core_functions_preeval` because the native cannot reference
  compiled-core's `PrevalWrapper`. The overloads map it onto `PrevalWrapper`.
- **`PrevalHooks`** carries Pure function-valued properties:
  - `stopPreeval: Function<{Any[*]->Boolean[1]}>` — the existing `defaultPreevalStopStrategy`,
    including the hard-coded engine type list and extension `shouldStopPreeval` contributions.
    The list stays in Pure because it names engine types.
  - `shouldInline: Function<{Function<Any>[1]->Boolean[1]}>` — the existing `State.shouldInlineFxn`.
  - `isGeneratedMilestoningProperty: Function<{Function<Any>[1]->Boolean[1]}>`.
  - `resolveTdsSchema: Function<{ValueSpecification[1], Map<String,List<Any>>[1]->Any[*]}>` — wraps
    `meta::pure::tds::schema::resolveSchema` with the extensions closed over.
  - `isGetAllFunction: Function<{Function<Any>[1]->Boolean[1]}>` — wraps `routing::isGetAllFunction`.
- **Unchanged:** the public overloads A–F keep their exact signatures. Overload E builds the hooks
  from its `State`; the others build a default `State` as today.
- **Switch:** `meta::pure::functions::preeval::preevalImplementation()` returns `PURE`, `JAVA` or
  `SHADOW`. The existing Pure implementation stays as `prevalInternal`; overloads E and F dispatch
  through `prevalWithImplementation(item, state, extensions, implementation)`. The default is `PURE`
  until cutover (§6).
  `preevalImplementation()` is a small native in `core_functions_preeval` that reads the system
  property `legend.engine.preeval.implementation`, which defaults to `PURE`. Tests and CI jobs select
  an implementation with `-Dlegend.engine.preeval.implementation=JAVA|SHADOW`.
- `SHADOW` runs both implementations and compares results through the normalisation used by
  `assertRoundTrip`: vX_X_X protocol JSON of the value, plus the `canPreval`, `openVars` and
  `modified` flags. Any difference fails with a `preeval SHADOW mismatch` message showing both
  normalised results.

### 3.4 Java core (`org.finos.legend.engine.pure.preeval`)

| Unit | Responsibility |
|---|---|
| `Preevaluator` | Entry points `prevalFunctionDefinition(...)` and `prevalExpression(...)`; sets up state; traces. |
| `PrevalState` | Immutable: `inScopeVars`, `rollingInScopeVars`, `inScopeTypeParams`, `path` (inlining stack for cycle prevention), `depth`, `debug`. Has `with…` copy methods. |
| `PrevalResult<T>` | Immutable: `value`, `canPreval`, `openVars`, `modified`. `markModified()`. |
| `NodeDispatcher` | The single type-dispatch site: LambdaFunction → FunctionDefinition → FunctionExpression → VariableExpression → InstanceValue → KeyExpression → lambda holder → leaf types → fallback (`stopPreeval` hook assert). |
| `FunctionDefinitionSequencer` | Expression-sequence fold: drops non-`let` non-final statements, `let` inlining decision, rolling scope, rewriting open variables of a lambda. |
| `rules/FunctionExpressionRule` | Interface `matches(fe, params, state)` / `apply(...)`. |
| `rules/Rules` | The one ordered list of rules (see below). A test pins the order. |
| `LambdaHolders` | Table of engine types, identified by path, that contain lambdas, with the property names to rewrite: `meta::pure::tds::BasicColumnSpecification.func`, `meta::pure::tds::AggregateValue.{mapFn,aggregateFn}`, `meta::pure::functions::collection::AggregateValue.{mapFn,aggregateFn}`, `meta::pure::metamodel::relation::AggColSpec.{map,reduce}`, `AggColSpecArray.aggSpecs`. |
| `Leaves` | Types returned unchanged (`SchemaState`, `RootGraphFetchTree`, `Binding`, `Store`, `FuncColSpec`, `TestParameters`) and those marked non-prevalable (`RelationStoreAccessor`, `RelationElementAccessor`), by path. |
| `GenericTypes` | `resolveGenericType` / `resolveFunctionType` against `inScopeTypeParams`. |
| `Scope` | `addToScope` (with self-reference dropping), `resolveVariable` (circularity assert), `areAllInScope`, `openVars`, `isInstanceValue`. |
| `PrevalHooks` | Java view of the Pure hooks: `stopPreeval(Object)`, `shouldInline(Function)`, `isGeneratedMilestoningProperty(Function)`, `resolveTdsSchema(ValueSpecification, Map)`, `isGetAllFunction(Function)`. Implemented by each adapter over the Pure functions. |
| `PrevalRuntime` | Port for everything mode-specific: `reactivate(vs, vars)`, `evaluate(fn, args…)`, `newInstanceValue(genericType, multiplicity, values)`, `newGenericType`/`newMultiplicity`, `copy(node)`, `rebuildLambda(original, newExpressionSequence, openVariables)`, `openVariableValues(lambda)`, `instanceOf(obj, path)`, `getProperty`/`setProperty` for lambda holders. Introduced in P1 with its first callers; P0 needs only `ProcessorSupport`. |
| `DebugTrace` | Depth-indented trace through SLF4J, gated by `DebugContext.debug`, with the same messages as today. |

**Rule order** (matches today's first-match semantics):

1. **Dedicated handlers**, applied before generic processing:
   - `IfRule`
   - `AndOrRule`
2. **Generic path**, after parameters are prevalled:
   - A stop check. The function is not prevalled if it has the `SideEffectFunction` or
     `NotImplementedFunction` stereotype, or if `stopPreeval` says so.
   - `InlineFunctionRule`
   - `EvalExpansionRule`
3. **Only when not reactivatable**, first match wins:
   - `TdsColumnsSchemaRule`
   - `EvalOnColumnRule`
   - `MapUnrollRule`
   - `FoldUnrollRule`
   - `ConcatenateRule`
   - `EmptyCastRule`
   - `FilterFalseRule`
   - `FilterTrueRule`
   - `ToOneManyRule`
   - `ToOneRule`
   - `GenericTypeRule`
4. **Otherwise:**
   - `ReactivateRule`, including re-prevalling any lambda holders the reactivation returns.

**Behaviour contract:** a rule-for-rule port, quirks included. This covers:
- `cast` of an empty collection;
- the `concatenate` multiplicity guard;
- `filter`-false skipping `getAll`;
- the `RelationType` return-type fix for `eval` on `Column`;
- the `Cast(@X)` multiplicity quirk in `InstanceValue` rebuilding;
- unwrapping nested `InstanceValue`s.

Anything that looks like a bug is reproduced, marked `// parity:` with a follow-up issue link, and
fixed only after cutover.

**Java 17 readiness in Java 11 code:**
- `PrevalState` and `PrevalResult` are final immutable classes, so they can become `record`s.
- The rules are a closed, explicitly listed set, so they can become `sealed`.
- `NodeDispatcher` is the only `instanceof` chain, so it can become a pattern-matching `switch`.

### 3.5 Adapters

**`CompiledPrevalRuntime`:**
- `reactivate` → `Pure.reactivate`, falling back to `dynamicallyEvaluateValueSpecification` as the
  existing native does.
- `evaluate` → `Pure.evaluate` / `PureCompiledLambda`.
- New nodes → `CompiledProcessorSupport.newCoreInstance`.
- `rebuildLambda` → `bridge.buildLambda(...)`, following `Reactivator`, so rewritten lambdas stay
  executable.
- `openVariableValues` → `Pure.getOpenVariables`.

**`InterpretedPrevalRuntime`:**
- M3 nodes are wrapped with `*CoreInstanceWrapper.toX(...)`.
- `reactivate` / `evaluate` → `FunctionExecutionInterpreted.executeValueSpecification` /
  `executeLambda`.
- New nodes → `ProcessorSupport.newAnonymousCoreInstance`.
- Rewritten lambdas become `LambdaWithContext` with the correct `VariableContext`.
- Results are returned through `ValueSpecificationBootstrap.wrapValueSpecification`.

### 3.6 Errors

Pure `assert`s ("Unsupported type: …", "Circular variable reference: …") become
`PureExecutionException`s carrying the native call site's source information and identical message
text. Exceptions thrown by hooks or reactivation propagate unchanged. Nothing is swallowed.

## 4. Testing and parity

1. **Existing suites per implementation.**
   - `tests.pure` and relational `testPreeval.pure` run under `PURE`, `JAVA` and `SHADOW` in compiled
     mode (`Test_Pure_Core`, `Test_Pure_Relational`).
   - `ToFix` and platform-excluded tests must keep their current status.
2. **New interpreted runner.** `Test_Interpreted_Preeval` (compiled-core `src/test/java/.../interpreted/`)
   runs `meta::pure::router::preeval::tests` in interpreted mode under `JAVA`.
3. **Shadow estate.** Downstream suites run under `SHADOW`: relational core, H2 and DuckDB PCT, SQL
   `testTranspile`, lineage and service. Every difference is either fixed in Java or recorded as a
   Pure quirk that Java reproduces.
4. **Java unit tests** (JUnit 5, shared module):
   - each rule's `matches` and `apply`, including negative cases taken from neighbouring rules;
   - the rule-order pin;
   - `Scope` and `GenericTypes`;
   - fixtures compiled from Pure snippets.
5. **Performance.**
   - A benchmark in `legend-engine-config/legend-engine-perf-benchmark`, parameterised `PURE` vs
     `JAVA`, over relational TDS/Relation, milestoning, SQL-over-Legend and graph fetch queries.
   - It reports the `traceSpan('preval')` time and end-to-end `planPure` time.
   - Per-rule fire counts and timings come from `DebugTrace`/metrics.
   - The target gain is set after the first baseline run.

## 5. Phases — Track 1 (Java 11 port)

Each phase is one PR. P0–P4 do not change production behaviour, because the switch default stays
`PURE`.

| Phase | Deliverable | Exit criteria |
|---|---|---|
| P0 Scaffold | Module family; `prevalNative` and `preevalImplementation` natives with compiled/interpreted shells; `PrevalHooks`; `PrevalResult`; the `PURE`/`JAVA`/`SHADOW` switch; identity native | Build green; native callable from Pure in compiled and interpreted modes |
| P1 Core traversal | `PrevalRuntime` and both adapters; state, result, dispatcher, sequencer, `Scope`, `GenericTypes`, variables, `InstanceValue`/`KeyExpression`/lambda holders/leaves, `ReactivateRule` | Constant-folding and variable subset of `tests.pure` green under `JAVA` — done |
| P2 Rules | All remaining rules | All of `tests.pure` and relational `testPreeval.pure` green under `JAVA` and `SHADOW` — **done** (relational: `testPrerouting42`, the file's only `<<test.Test>>`; its two `<<test.ToFix>>` tests, `testPrerouting41` and `testPrerouting_Store`, are excluded) |
| P3 Interpreted | `Test_Interpreted_Preeval`; adapter fixes | Green in interpreted mode — **done** (336/336 under `PURE`, `JAVA` and `SHADOW`) |
| P4 Shadow estate | Downstream suites under `SHADOW` | Zero unexplained differences |
| P5 Benchmark and cutover | Benchmark, baseline, default → `JAVA` | No regression; measured gain |
| P6 Cleanup (after one release) | Delete `prevalInternal` and the switch (`prevalWithImplementation`); rewrite `docs/engineering/architecture/preeval.md`; publish a "Pure feature → Java native" template guide | — |

### P1 status (2026-09-28)

`Test_Pure_Preeval_Java` is green under both `JAVA` and `SHADOW` for all 26 P1 target tests (52/52,
the two implementations times 26 tests). `DEFERRED_TO_P2` is empty: no P1 target test needed a P2
rule. The shared harness (`AbstractTestPrevalNative`) is green in both compiled and interpreted mode,
8/8, covering folding, substitution, multi-value re-activation, open variables, the
`AggColSpec`/`AggColSpecArray` lambda-holder paths, call-site errors and identity/non-mutation.
`Test_Pure_Core` (1192/1192) and `Test_Pure_Preeval` (111/111) pass unchanged under the default
`PURE`. Full exit-verification run: `Test_Pure_Core`/`Test_Pure_Preeval`/`Test_Pure_Preeval_Java`
together, 1355/1355, 0 failures/errors. Checkstyle: 0 violations across the four preeval modules and
compiled-core.

#### P1 structure vs §3.4/§3.5

P2 Task 1 extracted the rule structure out of the inline P1 traversal: `rules/ExpressionRule` (the
generic-path `matches`/`apply` contract) and `PreParameterRule` (the `if`/`and`/`or` pre-parameter
hook point), collected into `Rules.PRE_PARAMETER`, `Rules.EXPANSION`, `Rules.NOT_PREVALLED` and
`Rules.REACTIVATE`, with `TestRules` pinning the order. `ReactivateRule` now owns re-activation,
including re-prevalling lambda holders. The interpreted adapter's `LambdaWithContext`-to-plain-lambda
copy and the compiled adapter's reflective result/hook construction (both noted in P1) are unchanged.

Known gaps carried into P2/P3, recorded honestly rather than closed:
- Interpreted mode has harness coverage only; the full `tests.pure` interpreted runner
  (`Test_Interpreted_Preeval`) is P3 scope.
- The interpreted `BasicColumnSpecification`/`AggregateValue` lambda-holder paths are untested — the
  platform-only shared harness cannot construct those engine types.
- The `Cast(@X)` multiplicity-identity quirk path is not exercised in P1; it belongs to the P2 cast
  rules.
- Interpreted `InstanceValue._values()` unwraps primitives (`AnyHelper.UNWRAP_PRIMITIVES`: Float
  `BigDecimal` → `double`, re-wrapped via `BigDecimal.valueOf`, losing precision/scale) and does not
  resolve stubs (enum stubs could hit the stop assertion) — needs a `PrevalRuntime.values(InstanceValue)`
  port method; must be fixed before P3.
- The `Cast(@X)` multiplicity quirk compares with `runtime.pureOne()` by identity; interpreted
  `pureOne()` returns a fresh wrapper per call, so identity is unverified — needs
  `PrevalRuntime.isPureOne(Multiplicity)`; covered by P2's cast rules.
- Error text differs from Pure for primitives/non-packageable classifiers (`typeDescription`),
  contrary to §3.6 "identical message text".
- Unported P2 constructs fall through to generic processing in JAVA mode (semantically valid but not
  Pure-identical); only SHADOW detects it; no baseline of divergence across the full `tests.pure` yet.

### P1 requirements carried from P0 review

- `SHADOW` comparison must also include `genericType` and multiplicity (at least top-level; ideally
  every node) so type-level rules (the `eval`-on-`Column` `RelationType` fix, the `Cast` multiplicity
  quirk, `GenericTypeRule`) are gated by parity, not just the value. — **Done.** `SHADOW` compares type
  and multiplicity at the top level.
- Java must never mutate its input nodes: `SHADOW` returns Pure's result after running Java on the
  same inputs, so a Java-side mutation would be invisible to the comparison unless this is enforced
  separately. — **Done.** Every change goes through a `PrevalRuntime.with…` copy; the harness asserts
  non-mutation directly.
- `prevalJava` must either assert `inScopeTypeParams`, `path` and `depth` are empty, or the native
  signature must carry them, before release. — **Done.** `prevalJava` asserts all three are empty.
- The compiled native should pass call-site source information (§3.6). — **Done.** The compiled
  native carries the `prevalNative` call site's source information into every error.
- Result/instance construction behind `PrevalRuntime` should use anonymous/ephemeral instances
  (interpreted) and direct generated-class construction (compiled); exercise non-empty `openVars`. —
  **Done, with a noted fallback.** Interpreted uses ephemeral instances as designed. Compiled uses
  `processorSupport.newCoreInstance` plus reflective typed setters, not direct generated-class
  construction: the preeval module's own generated `Root_*` classes are not available at compile time
  (the native compiles before its own Pure sources are code-generated), so the adapter falls back to
  the generic construction path. Non-empty `openVars` are exercised by the harness.
- Validate `legend.engine.preeval.implementation` once and cache it, surfacing a clear error for an
  unrecognised value. — **Done.** The switch is parsed once per property value.
- TDD RED for new modules: run with `-Dmdep.analyze.skip=true` so the test (not the dependency
  analyzer) demonstrates the failure; direct Surefire invocations need `-DargLine=`. — **Done.** Every
  P1 task's RED ran with `-Dmdep.analyze.skip=true`; direct Surefire runs used `-DargLine=`.

### P2 status (2026-09-28)

`Test_Pure_Preeval_Java` is green under both `JAVA` and `SHADOW` for all 111 collected
`meta::pure::router::preeval::tests` (222/222). `KNOWN_DIVERGENT` is empty: the 49-name baseline
(`docs/superpowers/plans/2026-09-28-preeval-p2-shadow-baseline.md`) was closed across Tasks 4–8, with
no unexpected failures at any step. Relational `testPrerouting42`
(`legend-engine-xt-relationalStore-core-pure`'s `Test_Pure_Relational_Preeval_Java`, selected by name
out of the same `meta::pure::router::preeval::tests` package — the core jar's ~111 tests are on that
module's classpath too — with an exactly-one-match guard) is green under both `JAVA` and `SHADOW`
(2/2). `TestRules` pins the full Pure handler order (`Rules.PRE_PARAMETER`, `Rules.EXPANSION`,
`Rules.NOT_PREVALLED`, `Rules.REACTIVATE`). Full exit-verification run: `Test_Pure_Core` (1192/1192),
`Test_Pure_Preeval` (111/111) and `Test_Pure_Preeval_Java` (222/222) together, 1525/1525, 0
failures/errors, under the default `PURE`. Checkstyle: 0 violations across the four preeval modules,
compiled-core and relational core-pure.

**RelationType eval-on-Column probe.** A temporary `System.err` marker was added to
`EvalOnColumnRule.apply`'s `isInstanceOf(rawType, RELATION_TYPE)` branch (the compiled return-type
fix-up) and the full 222-execution `Test_Pure_Preeval_Java` run was repeated with it in place (output
capture verified working via the SLF4J warnings that appear in the same log). The marker fired zero
times: neither `tesColumnEvalOnRelation` nor `tesColumnEvalOnRelationWithCast` reaches that branch —
both tests' `eval`-on-`Column` targets resolve to a scalar `String` return type, not a `RelationType`,
so the fix-up condition never holds. The branch exists and is reachable in principle but is untested
by the current suite (see known gaps below). The probe was removed and the four preeval modules plus
compiled-core were rebuilt clean before every run reported above.

**Known gaps (carried forward, not closed by P2):**
- Interpreted-mode coverage of the TDS/`Relation` preeval paths — including `EvalOnColumnRule`'s
  `RelationType` fix-up and the `BasicColumnSpecification`/`AggregateValue` lambda-holder paths noted
  in P1 — is harness-only; the full `tests.pure` interpreted runner (`Test_Interpreted_Preeval`) is P3
  scope.
- The `EvalOnColumnRule` `RelationType` return-type fix-up (§3.4) has no test in either `tests.pure` or
  the relational suite that drives a `Column`-typed `eval` target whose own return type is a
  `RelationType`; confirmed unreached by the probe above.
- `// parity:` markers (13: 10 in the shared preeval module, 2 in the compiled adapter, 1 in the
  interpreted adapter) have no filed follow-up issue links yet (§3.4).
- **Compiled copies keep the original source information (deliberate deviation).** Compiled
  `FunctionExpression`/`InstanceValue`/`KeyExpression`/`GenericType` rewrites use the single-argument
  `CompiledSupport.copy` (`CompiledPrevalRuntime`), so the copy keeps the original node's source
  information. Pure's generated `^$x(...)` stamps `preeval.pure`'s source information on the copy
  instead. Lambdas are aligned with Pure (Task 5 fix, needed so the compiled body lookup behaves the
  same). The deviation is kept because errors then point at user code rather than at `preeval.pure`,
  and `SHADOW` does not compare source information. Decide before P4 whether to align it.
- **`SHADOW` does not gate nested types.** `describePrevalResult` compares the protocol JSON of the
  value plus the top-level `genericType`/multiplicity only. The protocol JSON does not carry the
  `genericType` or multiplicity of nested `FunctionExpression`s, so a divergence there passes
  `SHADOW`. P4 should add a structural per-node comparator (value, `genericType`, multiplicity at
  every node) before the shadow estate is treated as a parity gate.

### P2 requirements carried from P1 review

- P2 Task 1 refactor (extract `ExpressionRule`/`PreParameterRule`, one ordered `Rules` list with an
  order-pinning test, `ReactivateRule`, route `InstanceValue`/`Multiplicity` reads through the
  runtime) — **Done.** `Rules.java` holds `PRE_PARAMETER`, `EXPANSION`, `NOT_PREVALLED`, `REACTIVATE`;
  `TestRules` pins the order; `PrevalRuntime.values(InstanceValue)` and
  `PrevalRuntime.isPureOne(Multiplicity)` exist and are used at all read sites.
- Widen the ports (`PrevalHooks` gains `shouldInline`, `isGeneratedMilestoningProperty`,
  `isGetAllFunction`, `resolveTdsSchema`; `PrevalRuntime` gains the type/rewrite/multiplicity helpers
  the rules need) — **Done.**
- Add `Scope.addToScope` (with self-reference dropping) and exercise `inScopeTypeParams` and the
  inlining `path` cycle check — **Done.**
- Run all `tests.pure` tests under `SHADOW` before implementing rules and commit the failing list as a
  baseline manifest — **Done.** Task 3's baseline (49 names) is recorded in
  `docs/superpowers/plans/2026-09-28-preeval-p2-shadow-baseline.md`; every rule task closed its
  expected subset with no unexpected failures.
- Error-text parity (`typeDescription`) — **Done, in Task 2**, ahead of the rule tasks, for
  `PackageableElement` types. For a non-`PackageableElement` type the interpreted adapter prints
  `type.getName()` where Pure prints `$type->makeString()`, so that text may still differ.
- `DebugTrace` messages — **Not at parity.** Trace text is diagnostic only and differs from Pure's:
  - Output goes through SLF4J (`DebugTrace`, `[depth] message`), not Pure's `printDebugWithDepth`
    prefix and indentation.
  - The per-rule messages (`InlineRule`, `EmptyCastRule`, `FilterFalseRule`, `FilterTrueRule`,
    `MapUnrollRule`, `FoldUnrollRule`, `ConcatenateRule`) name the function with
    `typeDescription(func)`, which prints its classifier's path; Pure prints
    `$newSfe.func->elementToPath()`. Java's "Expanding eval" carries no path; Pure's is
    "Expanding eval: <path>". Only the fixed strings
    ("Handling toOne", "Handling toOneMany", "Handling genericType") match Pure's wording.
  - The stop-check and final-decision messages do have Java call sites (`Preevaluator`), with
    different text: "Unable to perform preval: <classifier path>", "Not prevalling (<reason>)" and
    "Performing preval", against Pure's "Unable to perform preval: <path> (<is native>)", "Not
    prevalling: <path> (<reason>)" and "Performing preval: <path> (can recativate dynamically: …)".
  - Not ported at all: the sequencer messages ("Dropping non-variable assignement…", "adding variable
    … to rolling scope…", "Processing parameter: …"/"Completed processing parameter: …", "Function
    expression has been modified …") and the `addToScope` messages ("Adding variables to scope: …" /
    "Adding type params to scope: …").
  - Messages are built lazily (`PrevalServices.trace` takes a `Supplier<String>`), so no text is
    built when debug is off.
- JUnit 5 tests for `GenericTypes` (incl. the `FunctionType` always-modified quirk) — **Not added.**
  No `TestGenericTypes` exists; only `TestScope` and `TestRules` were added alongside the rule work.
  `GenericTypes` is exercised indirectly through `Test_Pure_Preeval_Java` and the harness, not by a
  dedicated unit test.
- `// parity:` markers get follow-up issue links (§3.4) once issues are filed — **Still pending.** 13
  markers exist (10 shared, 2 compiled, 1 interpreted), none linked to a filed issue.

### P3 status (2026-09-28)

`Test_Interpreted_Preeval` is green: 336/336 (112 tests collected from `meta::pure::router::preeval::tests`
in interpreted mode, run once each under `PURE`, `JAVA` and `SHADOW`). `KNOWN_INTERPRETED_FAILURES` is
empty. Full-module verification run (`mvn clean install` with tests on, `-DargLine=-Xmx6g`, one Surefire
fork covering every compiled-core test class, matching CI's lifecycle): `TestCoreCompiledStateIntegrity`
34/34 (1 skipped), `TestIdBuilderCore` 3/3, `Test_Pure_Core` 1192/1192, `Test_Pure_Preeval` 111/111,
`Test_Pure_Preeval_Java` 222/222, `Test_Interpreted_Preeval` 336/336 — 1898 tests total,
0 failures/errors, 1 skipped, under the default
`PURE`. No OOM or heap-related warnings. Checkstyle: 0 violations on compiled-core and on
`legend-engine-pure-runtime-java-extension-interpreted-functions-preeval`. compiled-core's test run now
takes about 2 minutes longer for the interpreted suite (measured: `Test_Interpreted_Preeval` alone,
141.5 s Surefire-reported).

`Test_Interpreted_Preeval` collects 112 tests, one more than the 111 the compiled suites collect from
the same `tests.pure` source, because `testPrerouting20a` is tagged
`{test.excludePlatform = 'Java compiled'}` and so is excluded from compiled mode but runs in
interpreted mode. Interpreted mode therefore adds coverage rather than dropping it.

**`prevalJava` typed-wrapper bug, found by P3.** A throwaway spike at the start of P3 ran the
interpreted suite under each implementation before any rule work began. It found `prevalJava` always
built `^PrevalWrapper<Any>` instead of the typed wrapper PURE builds for function definitions
(`^PrevalWrapper<FunctionDefinition<Any>>`). Compiled mode never saw it — type erasure means the
compiled JVM does not check the type argument — and `SHADOW` never saw it either, because SHADOW
returns the PURE-side wrapper. Only interpreted `cast`, which does check type arguments, surfaced it
(99 of 101 interpreted `JAVA` failures in the spike). This argues for keeping interpreted runs in CI
going forward, not just as a one-off P3 gate.

**Interpreted variable-map reads now resolve stubs (consistency fix).**
`InterpretedPrevalRuntime`'s `toVars`/lambda-context variable-map reads used
`getValueForMetaPropertyToMany` (raw, unresolved) instead of
`Instance.getValueForMetaPropertyToManyResolved`. No platform-only scenario made the unresolved read
observable: the interpreter resolves stubs before values reach `toVars`/`openVariableValues` in the
paths the suite exercises, so nothing failed before the fix. It is fixed for consistency with the rest
of the adapter, which resolves stubs everywhere else.

**P2 known gaps closed by P3:**
- Interpreted-mode coverage of the TDS/`Relation` preeval paths (including the
  `BasicColumnSpecification`/`AggregateValue` lambda-holder paths) is no longer harness-only —
  `Test_Interpreted_Preeval` now exercises the full `tests.pure` suite in interpreted mode.
- P3 finished the stub-resolution sweep in the two remaining variable-map reads, `toVars` and
  `openVariableValues` (see above). The P1 known gap about `InstanceValue._values()` not resolving
  stubs was a separate item, already closed before P3 by `PrevalRuntime.values(InstanceValue)`
  (`InterpretedPrevalRuntime.values(InstanceValue)`).

**Known gaps (carried forward, not closed by P3):**
- Interpreted coverage of the relational `testPrerouting42` is still pending: it needs relational
  interpreted natives on `legend-engine-xt-relationalStore-core-pure`'s interpreted classpath, which
  `Test_Interpreted_Preeval` (compiled-core only) does not have. P4 item.
- Two sites in `preeval.pure` build a hand-built `InstanceValue` that is let-bound and never
  `evaluateAndDeactivate`d: the trailing-let handling in `prevalFunctionDefinition`, and the
  collection-expansion rule in `prevalGenericFunctionExpression` (the `map`/`fold` unrolling pairs). In
  interpreted mode such a value can read back with an empty `genericType`, as diagnosed while building
  the SHADOW rejection tests (`testShadowRejectsDifferentTypes`/`testShadowRejectsDifferentMultiplicities`).
  If either ever became a top-level preeval result, SHADOW's `describePrevalResult` would NPE in
  `printGenericType`. Latent today — no test drives it into that position — P4 should either route them
  through `evaluateAndDeactivate` or make `describe` robust to a missing `genericType`.
- `Test_Interpreted_Preeval` reuses one `TestSuite` under `PURE`, `JAVA` and `SHADOW`, so the Surefire
  XML lists each test three times with identical names. Pass/fail counts are accurate, but CI triage
  needs the log, not just the XML, to tell which implementation failed. P4 item.
- JAVA-mode wrapper typing matches PURE only for function definitions. For a non-function-definition
  top-level result, PURE builds a narrower wrapper (`PrevalWrapper<FunctionExpression>`,
  `<InstanceValue>` or `<ValueSpecification>`, depending on the site), while JAVA always returns
  `PrevalWrapper<Any>`. `describePrevalResult` never compares the wrapper's type argument, and compiled
  mode erases it, so this drift is invisible to SHADOW and to compiled callers today. It would only
  fail a future caller that casts the wrapper to its narrower type under interpreted JAVA. P4 item: the
  SHADOW comparator should compare wrapper type arguments (or equivalent) so this drift is caught in
  compiled mode as well.

## 6. Cutover criteria

- zero `SHADOW` differences across default-profile CI suites;
- the interpreted runner is green;
- the benchmark shows no regression.

After cutover the default becomes `JAVA`. `PURE` remains selectable for one release as a rollback
path.

## 7. Track 2 — Java 17+/21

Recommended target: **Java 21 LTS**. Pattern-matching `switch`, the feature that most pays off in
this design, is final only in 21.

| Phase | Work |
|---|---|
| J0 Feasibility spike | Inventory blockers and produce a go/no-go memo:<br>• the root pom enforcer range `[11.0.10,12)`;<br>• the source/target level legend-pure uses to compile generated Java at run time (compiled mode);<br>• legend-pure's own build JDK;<br>• third-party support (Dropwizard, Jersey, Testcontainers, Maven plugins);<br>• downstream consumers embedding engine jars on JDK 11, a FINOS-level decision. |
| J1 Build on 21, target 11 | CI builds and tests on JDK 21 with `--release 11`, flushing out runtime incompatibilities without affecting consumers. |
| J2 Raise target | Enforcer and `maven.compiler.release` → 21, coordinated with legend-pure and announced to consumers. |
| J3 Modernise preeval | `PrevalState`/`PrevalResult` → `record`s; `FunctionExpressionRule` → `sealed`; `NodeDispatcher` → pattern-matching `switch`; `var` and text blocks in adapters and tests. No behaviour change, guarded by the parity suites. |
| J4 Guidance | Add a Java 21 feature-usage section to `docs/engineering/standards/coding-standards.md`. |

## 8. Risks

| Risk | Mitigation |
|---|---|
| Compiled-mode lambda rebuilding loses closures, so rewritten lambdas fail at execution | Follow `Reactivator`'s `bridge.buildLambda` pattern; `assertRoundTrip` evaluation-equivalence plus shadow estate catch it early (P1/P2). |
| Callbacks into Pure per node (`stopPreeval`) erase the performance gain | Measure per-hook call counts in P5. If they dominate, move the hard-coded stop list into Java by path as a follow-up (was an explicit design choice to keep it in Pure). |
| Interpreted-mode differences (`LambdaWithContext`, wrappers) | P3 dedicated runner; adapter isolated behind `PrevalRuntime`. |
| Hidden reliance on quirks by downstream code | Rule-for-rule parity plus the `SHADOW` run over the full test estate before cutover. |
| Repo-pattern overlap (`core` claims `meta::pure::*`) | Same situation as `core_functions_unclassified` today; verify in P0. |
