# ADR-004: Test-Execution Natives for the SQL E2E Interpreted Dev Loop

**Status:** Phases 1–3, 7, 8 and 9 implemented and verified live against the interpreted LSP; Phases
4–6 still proposed.
**Date:** 2026-09-29 (Phases 1–3 implemented 2026-09-30; Phase 7 implemented 2026-09-30; Phase 8
implemented 2026-09-30; Phase 9 implemented 2026-09-30)
**Deciders:** _Pending review for remaining phases_

---

## Context

The SQL e2e parity corpus (`TestPostgresParity`, 2,307 cases) can be run through the interpreted
Pure LSP so that edits to `fromPure.pure` and the rest of the SQL→Pure translation are testable in
seconds rather than a 15–25 min rebuild. See
[the dev-loop guide](../guides/sql-e2e-interpreted-devloop.md).

That harness currently depends on a native called `tryEval`:

```pure
native function meta::external::query::sql::e2e::tryEval(f:Function<{->String[1]}>[1]):String[1];
```

It exists because **Pure has no `try`/`catch`**. Without per-case trapping, one failing case aborts
an entire category run — unacceptable for a dev loop. It is used by `runCaseSafe`, which packs
status and message into one string with a `__ERROR__` marker prefix and a `~#~` field delimiter,
then unpacks them on the other side.

Four problems motivate this ADR.

**1. `tryEval` is not a Pure function and must not be reachable from production.** It is a
Java-level escape hatch. The codebase already has a precedent for exactly this kind of
capability — `meta::legend::execute` — and that one is deliberately fenced off. `tryEval` is not.

**2. The API is unfit even for its current single use.** It is `String`-only, and it distinguishes
failure from success with a magic prefix that collides silently if a test legitimately returns a
string starting with `__ERROR__`. The `fieldDelim`/`split` packing that sits on top of it exists
only to work around the `String[1]` return type — and it was itself the source of a real bug
(`split()` with a multi-character token, see the guide).

**3. Other frameworks have the same gap.** A survey of the Pure test frameworks found:

| framework | error-tolerant? | evidence |
|---|---|---|
| PCT | **yes** — `executePCTTest` returns `TestResult`, catches and classifies | `CompiledSupport.java:2471,2502` |
| vanilla `<<test.Test>>` | yes, but only because JUnit gives one `TestCase` per function | `PureTestBuilder.java:54-72` |
| surveyor `runTests` | yes — but only because `executeTest` traps; the Pure driver has no protection | `surveyor.pure:66-111` |
| **SDT in-Pure driver** | **no** — `runSdtTestsInIDE` is a bare nested `map` ending in `assertEquals`; the first failure aborts the rest | `sdtFramework.pure:225-245` |

So the trapping already exists — it is just not reusable. `executeTest` and `executePCTTest` are
`<<access.private>>` to the surveyor package and shaped around test functions, which is precisely
why this harness had to invent its own.

**4. The harness reimplements result comparison instead of sharing the compiled suite's.**
`framework.pure` carries ~130 lines of string canonicalisation that `ResultComparator` has no
counterpart for, because the interpreted harness compares *strings* where the compiled suite
compares *typed values*. The two suites therefore disagree about what counts as a divergence — the
opposite of what a dev loop for that suite should do. Detailed in §4.

Two defects were found in the existing implementations while surveying, noted here because any new
native should avoid inheriting them:

- **Interpreted `executePCTTest` catches `Exception`, not `Throwable`** (`ExecutePCTTest.java:146`),
  while compiled catches `Throwable` (`CompiledSupport.java:2502`). A bare `AssertionError` in an
  interpreted PCT test escapes and aborts the surrounding `runPCTTests`.
- **`CompiledSupport.java:2633` divides an already-millisecond value by 1e6 again**, so
  `TestResult.elapsed` — and hence `TestReport.totalElapsed` — is always `0` in compiled mode.
  Interpreted is correct (`ExecutePCTTest.java:173`).

---

## Decision

### 1. Follow surveyor's architecture: one native, Pure drivers

Surveyor keeps exactly one thing in Java — the per-test trap — and writes the drivers in Pure
(`executeTest` native + `runTests`/`runTestsFromPath` in Pure). Mirror that:

```pure
// The only new *execution* native: trap + time a single (case, path) pair.
native function meta::external::query::sql::e2e::executeSQLE2ETest(
    caseId:String[1], path:SqlE2EPath[1]) : SqlE2ETestResult[1];

// Plain Pure: an explicit list of case refs (see resolveCaseRefs).
function meta::external::query::sql::e2e::executeSQLTests(
    refs:SqlE2ECaseRef[*]) : SqlE2ETestReport[1];

// Plain Pure: everything matching a filter (exact category, `prefix*`, or '' for all).
function meta::external::query::sql::e2e::executeSQLTestCategory(
    category:String[1]) : SqlE2ETestReport[1];
```

(`SqlE2EPath`, `SqlE2ECaseRef`, `SqlE2ETestResult`/`SqlE2ETestReport` are this module's own types —
see the revisions to §2 and §3 below; the signatures above are what actually shipped, not the
surveyor-typed, composite-string-id versions originally proposed here.)

**"Category" is the framework's term**, not "suite" — it is used in `SqlE2ERunner.Entry.category`
(derived from the YAML path, e.g. `structural/null_semantics`), on `SqlE2ECase.category`, and in
`TestPostgresParity`.

Aggregation in `executeSQLTests` is the same plain `filter`/`size` that surveyor's `runTests` uses;
no Java is required for it.

Two further natives fall out of §4 and §5 — `compareToReference` and `resolveCaseRefs` — but neither
executes anything: one compares two already-computed results, the other reads the corpus index.
`executeSQLE2ETest` remains the single point where Pure work is invoked from Java.

### 2. Own the result model — do not reuse surveyor's

**Superseded during implementation.** The original decision here was to reuse
`meta::pure::test::surveyor::TestResult`/`TestStatus`/`TestReport` — both public classes, so a
native in this repo could return one directly — and to keep `SqlE2EOutcome` as a wrapper carrying
`expected` (the baseline) and a `bug:Boolean` alongside a `TestResult`, since `TestStatus` has no
`BUG` value.

That wrapper turned out to be the wrong fix for the wrong problem: `TestStatus` not having `BUG`
isn't a gap in surveyor, it's a sign a corpus case isn't a Pure test function and shouldn't be typed
as one. Owning the model instead removes the wrapper entirely and gives `BUG` first-class status:

```pure
Enum meta::external::query::sql::e2e::SqlE2ETestStatus { PASS, FAIL, ERROR, SKIP, BUG }

Class meta::external::query::sql::e2e::SqlE2ETestResult
{
  id: String[1];
  status: SqlE2ETestStatus[1];
  elapsed: Integer[1];
  message: String[0..1];
}

Class meta::external::query::sql::e2e::SqlE2ETestReport
{
  results: SqlE2ETestResult[*];
  totalElapsed: Integer[1];
  passCount: Integer[1];
  failCount: Integer[1];
  errorCount: Integer[1];
  skipCount: Integer[1];
  bugCount: Integer[1];
}
```

`SqlE2EOutcome` survives, but only to carry `expected` (the corpus baseline) next to a
`SqlE2ETestResult` — a report-layer concern a result type still has no business knowing about,
regardless of whose result type it is:

```pure
Class meta::external::query::sql::e2e::SqlE2EOutcome
{
  id: String[1];
  expected: String[0..1];
  result: SqlE2ETestResult[1];
}
```

`runOneCase`'s BUG path returns a `__BUG__`-prefixed string (mirroring the existing `__SKIP__`
prefix), classified by `SqlE2ENativeHelper.classifyResult` before the generic FAIL case — not, as
first implemented, a `fail('BUG: ...')` throw classified as `ERROR` with a message-prefix
convention layered on top. The throw-and-string-match version worked, but re-created exactly the
kind of out-of-band signalling problem 4 (this document, top) called out `tryEval`'s marker
prefixes for — a second status wearing the first one's clothes. A dedicated prefix + dedicated
enum value says what it means.

The cost is what §7 already costs for `runOneCase`'s Java-side wiring: `newCoreInstance`/
`setValueForProperty` calls hand-written per field, once per runtime, instead of getting the type
for free from `platform`. That cost was already being paid to build `TestResult`; owning the type
adds no new mechanism, just a second class to build the same way.

### 3. The native takes a typed `(caseId, path)` pair; it resolves the work function itself

The native must **not** perform the SQL work: `executeLegend` (parse → `sqlToPure` → routing →
plan-gen → dialect SQL) has to stay in Pure or the dev loop loses its reason to exist. So the
native invokes a well-known Pure function:

```pure
// Plain Pure. Returns [] on match, or the diff description on mismatch.
function meta::external::query::sql::e2e::runOneCase(corpusId:String[1], path:SqlE2EPath[1]):String[0..1]
```

with the classification contract:

| `runOneCase` outcome | `SqlE2ETestStatus` |
|---|---|
| returns `[]` | `PASS` |
| returns a `__SKIP__`-prefixed string | `SKIP`, message = the rest of the string |
| returns a `__BUG__`-prefixed string | `BUG`, message = the rest of the string |
| returns any other non-empty string | `FAIL`, message = the diff |
| throws | `ERROR`, message = `PCTTools.getMessageFromError(unwrapExecutionError(t))` |

This fits because `compareToReference` already returns `String[0..1]` with `[]` on match — the
return value *is* the classification signal for the PASS/FAIL/ERROR core, extended with two
sentinel prefixes (mirroring `tryEval`'s own `__ERROR__` prefix, which this whole native replaces)
for the two states that aren't "did the comparison match."

Both runtimes can resolve a Pure function by path at execution time:

- **Interpreted:** `processorSupport.package_getByUserPath(RUN_ONE_CASE)`
- **Compiled:** `processorSupport.package_getByUserPath(RUN_ONE_CASE)` too — `CompiledProcessorSupport`
  implements the same method, resolving through `Metadata.getElementByPath` when the metadata
  supports it. `CoreGen.getSharedPureFunction((Function<?>) resolved, es)` then adapts it to a
  callable `SharedPureFunction`. (The originally-planned `Pure.pathToElement` walks the root package
  by plain segment names and was never needed — `package_getByUserPath` on the mangled path works
  identically in both runtimes.)

The mangled path lives in **one shared constant** so the two implementations cannot drift:

```java
// SqlE2ENativeHelper
public static final String RUN_ONE_CASE =
        "meta::external::query::sql::e2e::runOneCase_String_1__SqlE2EPath_1__String_$0_1$_";
```

#### Alternatives considered

- **Pass a lambda** (`f:Function<{->String[0..1]}>[1]`). Works, and is what `tryEval` does today,
  but every call site must thread `$conn`/`$ext`/`$c` into a closure.
- **Pass a function reference** (`testFn:Function<...>[1]`), as `executePCTTest` does — type-checked,
  and `PackageableElement.getUserPathForPackageableElement` yields the `fqn` for free.
- **A single composite string id** (`"<corpusId>|TDS"`, split back apart inside `runOneCase`). This
  is what was originally proposed and initially built. It was replaced with the typed pair above
  once it became clear the composite string was a needless echo of the exact bug class problem 2
  (this document, top) already named: `split()`/substring parsing of a delimited convention, in the
  same module that had already been bitten by that once. `resolveCaseRefs` returning
  `SqlE2ECaseRef[*]` (a `{corpusId, path}` class) instead of `String[*]` is the same reasoning
  applied to the enumeration side.
- **Chosen: typed `(corpusId:String[1], path:SqlE2EPath[1])`.** The hardcoded mangled path in Java
  is still the cost, unchanged from the composite-id version — it is accepted for the same reason:
  `runOneCase` and both extensions ship in the *same artifact* and version together, unlike
  `meta::legend::execute`, whose string spans a repo boundary. Generated per-case wrappers are a
  two-argument call instead of one, which costs nothing since wrappers are generated Pure source,
  never typed by hand at a CLI (`pure-lsp execute` only calls zero-arg functions in the first
  place, so "easy to type as one string" was never a real advantage of the composite id).

Two guards are **required** to make this acceptable:
- a null result from path resolution must raise a clear *"cannot resolve `<path>` — did
  `runOneCase` change signature?"*, not an NPE;
- a Pure test that calls `runOneCase` directly, so a signature change breaks a **compile** rather
  than only the native at runtime.

### 4. Push the compiled suite's result normalisation down, and delete the Pure canonicalisers

`framework.pure` carries ~130 lines of string canonicalisation — `normalizeValue`, `looksNumeric`,
`looksLikeDatetime`, `normalizeNumeric`, `roundToSignificantDigits`, `rowKey`, `compareResults`,
`nullToken` — that the compiled suite has no equivalent of. `ResultComparator` is 120 lines and
does essentially nothing but a float epsilon. That asymmetry is not an accident of effort; it is a
consequence of *where each suite draws the Java/Pure boundary*.

**Why compiled needs almost none.** Both of its sides are `rs.getObject(i)` over JDBC —
`DirectPostgresRunner:65` for the reference, `TestPostgresParity:621` for Legend. The Legend side's
rendering happens once, structurally, at the pg-wire boundary: `LegendResultSet`'s type processors
turn the engine's JSON strings into typed Java values (dates → epoch millis, `Number` → the right
width, `Boolean`/`String` passthrough), the wire encoder writes them under the correct Postgres type
OID, and the JDBC driver reconstructs typed objects on the far side. So `ResultComparator` is handed
`Integer`↔`Integer` and `Timestamp`↔`Timestamp` and only ever has to decide *tolerance*, never
*format*.

**Why interpreted needed all of it.** Its Legend side is the raw `executePlanAsJSON` TDS JSON,
deserialised in Pure and `toString()`'d; its reference side is `String.valueOf(v)` over the same
JDBC objects (`SqlE2ERunner.matrixNode:405`). Two unrelated renderings, both erased to `String`
before anything can compare them — so the lost type had to be *guessed back from the text*. That is
what `looksNumeric`/`looksLikeDatetime` are: heuristics recovering a type the boundary threw away.
It is also why they were a bug farm — the dash-position rule exists solely to stop a `DATE` literal
being mistaken for a float, and got it wrong twice (first on dates, then on negative exponents).

**Decision: move the boundary, do not port the logic.** Add a second native:

```pure
native function meta::external::query::sql::e2e::compareToReference(
    caseId:String[1], resultJson:String[1]) : String[0..1];
```

Pure keeps exactly the part under test — parse → `sqlToPure` → routing → plan-gen →
`executePlanAsJSON`. Java turns that JSON into a typed `ResultMatrix` and calls **the same
`ResultComparator` the compiled suite calls**. `runOneCase` collapses to `executeLegend` followed by
`compareToReference`, and its `String[0..1]` contract from §3 is satisfied directly by
`ComparisonResult` (`[]` on match, the joined diffs otherwise).

**The reuse is not hypothetical — the parser already exists and already accepts this exact JSON.**
`LegendTdsResultParser` (in `legend-engine-xt-sql-postgres-server`) streams a TDS result into
`List<LegendColumn>` + `List<Object>` rows, and `LegendResultSet` is the type-processor layer over
it. Three things line up:

- the protocol's `TDSColumn.type` values are *literally* the `LegendDataType` constants the
  processor map is keyed on — `Integer`, `Float`, `StrictDate`, `DateTime`, `String`, `Boolean`,
  and the `meta::pure::precisePrimitives::*` FQNs;
- `legend-engine-xt-sql-postgres-server` is already a **compile-scope** dependency of the e2e-tests
  module, so no new dependency edge;
- `LegendTdsResultParser:114` passes empty `linearizedInheritances`, exactly as the production
  `LegendExecutionService:64` does — so an unmapped precise primitive falls through to identity in
  the harness precisely as it does in production. (Only `GenericLegendExecution` populates them.)

**One real gap must be closed first: dates.** `LegendResultSet` emits epoch-millis `Long`, while the
JDBC reference side emits `java.sql.Date`/`Timestamp`. Compiled only gets away with this because the
wire encoder and JDBC driver turn those millis back into a `Timestamp` before the comparator sees
them. Short-circuiting the wire means `ResultComparator` would see `Long` vs `Timestamp`, fall
through to its `toString()` default, and report a false divergence on **every date column**.

Fix it in `ResultComparator`, not in the harness: canonicalise temporal values (`java.util.Date`,
`java.time.*`) to epoch millis before the `Number` branch. This is a **correctness improvement for
the compiled suite too** — today a date cell is compared by `toString()` and only agrees because
both sides happen to come from the same driver; `'2023-01-16'` vs `'2023-01-16 00:00:00.0'` would be
a spurious failure the moment that stopped being true. Do *not* instead reuse the pg-wire
`TypeConversion` encoders: that drags the wire codec in to reproduce a round-trip whose entire
purpose here is to be skipped.

**Two behaviour changes follow, both intended:**

- **Float tolerance loosens to CI's.** `roundToSignificantDigits` quantises to 12 significant digits
  — deliberately ~5 orders stricter than `ResultComparator`'s `1e-6` relative epsilon, chosen when
  the only available primitive was per-value string equality. After push-down, interpreted adopts
  CI's exact tolerance. This is the point: for a dev loop whose governing rule is *confirm every
  finding against compiled*, agreeing with CI beats being stricter than it.
- **NULL becomes a real Java `null` on both sides.** The `NULL_TOKEN` sentinel in `matrixNode` and
  `nullToken()` in Pure both exist only because Pure's deserialiser cannot hold a null in a
  `String[*]`. With comparison in Java, `cellEquals`'s null branch handles it and both go away —
  along with the `nullReplacementInArray` deserialisation config that caused the column-shift bug.

Two further fidelity gains come for free: `ResultMatrix.sorted()` sorts column-wise and
numeric-aware, where the Pure path sorted lexicographically on a delimiter-joined row key; and
`ResultComparator` compares column *names* case-insensitively and reports mismatches, where
`compareResults` only ever compared counts.

### 5. Expose the id list as a native so the driver stops re-reading YAML

```pure
native function meta::external::query::sql::e2e::resolveCaseRefs(filter:String[1]):SqlE2ECaseRef[*];
```

A thin wrapper over the existing `SqlE2ERunner.listIds`, which already supports the full filter
grammar: exact id, exact category, `prefix*`, or empty for everything. No JSON round-trip. Returns
`SqlE2ECaseRef[*]` rather than `String[*]` per §3's revision — each corpus id crossed with both
`SqlE2EPath` values, as a typed `{corpusId, path}` pair instead of a delimited string.

**Verified live** (2026-09-30, warm interpreted LSP): `resolveCaseRefs('smoke_tests')` returned 20 refs
(10 corpus cases × 2 paths); `executeSQLTestCategory('smoke_tests')` returned
`pass=16 fail=0 error=4 skip=0`, matching the legacy `divergenceReport` driver's count on the same
filter exactly (the 4 errors are the pre-existing table-free-query gap documented in
`KNOWN-GAPS.md`, not a regression). All five `SqlE2ETestStatus` values were separately confirmed via
`executeSQLE2ETest` on hand-picked cases: `PASS` (`smoke_select_column`), `FAIL`
(`null_equals_null`, message sourced directly from `ResultComparator`: `"Row 0, column 'result':
expected 'null' but got 'true'"`), `ERROR` (table-free bare-literal query), `SKIP`
(`abbrev__cidr__unsupported`, message = the corpus `skip:` reason), `BUG` (`cast_numeric_to_int`,
message = `"postgres rejected reference SQL: ERROR: integer out of range"`). Float and
date/timestamp normalisation (§4) were confirmed separately: `AVG(numeric_val)` and `MAX(ts)` both
PASS with no Pure-side rounding, and `d + INTERVAL '1 day'` (a `StrictDate` result) PASSes too —
covering the `java.sql.Date` and `java.sql.Timestamp` cases in `ResultComparator`'s temporal
canonicalisation.

This matters for two reasons:

- `executeSQLTestCategory` is then trivially `executeSQLTests(resolveCaseRefs($category))`.
- **The parallel driver can enumerate through the native instead of parsing YAML in Python.**
  Today the driver reads the corpus independently of `SqlE2ERunner`, so Java and Python hold
  separate notions of it. That duplication goes away.

### 6. Parallelism stays client-side; do not thread inside the native

The existing per-case parallelism comes entirely from the **LSP daemon's request pool**
(`requestPoolSize: 12`), fanning out across separate `execute` calls — never from Pure.

**A suite-level native is therefore inherently serial**, and must stay that way.
`LegendPureSession.java:1561-1564` constructs a *fresh* `StackPreservingFunctionExecutionInterpreted`
per request precisely to avoid *"cross-talk between concurrent executions"*. Inside a native there
is a single `functionExecution` plus per-execution mutable state (`variableContext`,
`instantiationContext`, `functionExpressionCallStack`); threading over that shares exactly what
that design isolates. Compiled has the same concern with a shared `ExecutionSupport`.

Two tiers of granularity result:

| granularity | mechanism | when |
|---|---|---|
| category | the 74 existing `run::<category>` nullary wrappers, now one-liners over `executeSQLTestCategory` | most categories; no generation at all |
| per-case | generated nullary wrappers over `executeSQLE2ETest(caseId)` | categories exceeding the 600s bridge ceiling, and single-case diagnosis |

Category granularity is **not** sufficient everywhere: measured, `functions/aggregate_functions`
took 1338s, `functions/string_functions` 1437s and `functions/math_functions` 900s even *with*
per-case chunked parallelism. A single serial call on any of those would exceed the ceiling and
return zero partial results. For those, either an explicit id slice or a prefix filter
(`executeSQLTestCategory('avg_*')`) splits the work without generating wrappers.

### 7. Make corpus edits live

Editing `parity-tests/*.yaml` is currently **invisible** to a running daemon, silently — the old
SQL keeps running. Two independent layers cause it:

1. `TestCaseLoader.load` reads via `getClassLoader().getResourceAsStream(...)`, and the LSP's
   classpath entry is the built jar, so source edits never reach the JVM.
2. `SqlE2ERunner` is a JVM-lifetime singleton whose `corpus` map is populated once in the
   constructor.

This is a confusing asymmetry: `framework.pure`, `model.pure` and `fromPure.pure` are all live
because the LSP reads them from `--repo-root`; only the corpus is not, because it is data in a jar.

**Resolution — read the corpus from a directory when configured:**

```java
String corpusDir = System.getProperty("sql.e2e.corpus.dir");   // falls back to the classpath
```

following the precedent already in `SqlE2ERunner`, which honours `-Dsql.e2e.postgres.host` to use
an external Postgres.

**Constraint on the LSP option mechanism:** `POST /set-option` sets
`System.setProperty("pure.options.X", "true")` and is read back through `Boolean.getBoolean` — it is
**boolean-only** and cannot carry a path. Therefore:

- the **path** is supplied at daemon launch via `--jvm-arg=-Dsql.e2e.corpus.dir=…` (the launch
  already passes several `-D` args);
- a **boolean option** `pure.options.SqlE2ECorpusLive` makes the runner re-read the corpus from
  that directory, and is flippable live via `pure-lsp-option set SqlE2ECorpusLive` with no restart.

A `reloadSqlE2ECorpus()` native gives an explicit refresh. It must **not** re-run the constructor:
container startup, schema seeding and Vault registration happen there, and restarting the
Testcontainers Postgres would be slow and destructive. It re-reads YAML into `corpus`/`knownTables`
and clears `referenceCache`/`referenceErrorCache` only. Those fields are `final`, so clear and
repopulate rather than reassign. (The reference caches are keyed by SQL string, so an edited SQL
would naturally miss anyway; it is the `corpus` map that goes stale.)

### 8. Scope the natives away from production

Two composable levers exist, both verified:

1. **Compile-time — repo visibility.** `Visibility.isVisibleInRepository` enforces the
   `.definition.json` dependency graph. Production Pure cannot compile a reference to a symbol in a
   repo it does not declare.
2. **Runtime — Maven scope.** Ship the declaration and the native implementation only on the test
   classpath.

The cleanest precedent is **SDT**: `core_external_store_relational_sdt` is a normal compile-scope
`*-pure` module carrying `.definition.json` + natives + `META-INF/services`, which every consumer
pulls in at `<scope>test</scope>` (the Pure IDE takes `runtime` so the repo stays browsable). This
is stronger than PCT's `core_external_test_connection`, which ships a test-only native
(`getTestConnection`) at *compile* scope to ~16 consumers — isolation by naming convention only.

`core_external_query_sql_e2e` currently achieves isolation only by having no consumers. That is
sufficient today but not a stated contract; adopting the SDT consumption model makes it one.

---

## Consequences

### Positive

- Deletes `tryEval`, `errorPrefix()`, `fieldDelim()`, the `__ERROR__` prefix, and the whole
  `runCaseSafe` pack/unpack dance — all of which exist only to smuggle a status and message through
  a `String[1]` return.
- Deletes a further ~130 lines of Pure — the whole `normalizeValue` / `looksNumeric` /
  `looksLikeDatetime` / `normalizeNumeric` / `roundToSignificantDigits` / `rowKey` /
  `compareResults` / `nullToken` chain, plus `toResultSet`, `SqlE2EResultSet`, `SqlE2EResultRow`
  and the `nullReplacementInArray` config. **Every normalisation bug found in this cycle lived
  there**, and each was a symptom of recovering a type the boundary had already erased.
- The two suites compare results through **one** implementation, so a divergence reported
  interpreted means the same thing CI means by it — which is the premise the whole
  confirm-against-compiled discipline rests on.
- `ResultComparator` becomes type-aware about dates instead of relying on the wire round-trip to
  normalise them, closing a latent false-failure in the compiled suite as a side effect.
- Own results (`SqlE2ETestResult`/`SqlE2ETestStatus`/`SqlE2ETestReport`) fold `BUG` in as a first-
  class status, so counts and elapsed times come free of any surveyor-shaped wrapper, and `bugCount`
  is a real field on the report instead of a per-outcome boolean computed by string-matching a
  message prefix.
- Typed `(corpusId, path:SqlE2EPath)` and `SqlE2ECaseRef` eliminate every remaining
  string-convention parse in this module — the exact bug class (`split()`/substring on a delimited
  id) that motivated problem 2 at the top of this document.
- The parallel driver stops re-reading YAML; `resolveCaseRefs` becomes the single source of truth.
- Corpus edits become live, removing a silent-staleness trap.
- Category runs need no code generation at all.

### Negative / Limitations

- A mangled Pure path is hardcoded in Java, in a shared constant. Mitigated by the two guards in
  §3 — though only the null-resolution guard has actually been added; the direct-call Pure test is
  still outstanding, see the Phase 1 status note below.
- The native is SQL-e2e-specific by name, so **SDT cannot reuse it**. If unblocking
  `runSdtTestsInIDE` is also a goal, the same implementation generalises to
  `executeLambdaAsTest(fqn, f):TestResult` in a shared test-support repo — a rename plus a repo
  move. That is a larger change (it touches legend-pure and the cross-repo rebuild chain) and is
  deliberately deferred.
- Owning the result model (§2) means `newCoreInstance`/`setValueForProperty` wiring is now needed
  for **two** classes (`SqlE2ETestResult` and `SqlE2ECaseRef`) in each runtime, not reused from
  `platform`. Both are small, and the pattern is identical, so the marginal cost is low.
- **Interpreted float comparison gets *looser*, not tighter** (12 significant digits → CI's `1e-6`
  relative epsilon). A handful of currently-reported float divergences will stop being reported.
  Deliberate — see §4 — but it does mean the dev loop can no longer be used to hunt small float
  drift that CI tolerates. If that is ever wanted, tighten `ResultComparator`'s epsilon for both
  suites rather than reintroducing a second comparator.
- Comparison moves out of Pure, so a change to comparison semantics stops being live-editable and
  needs a module rebuild + daemon restart. Acceptable: comparison is the part of the loop that is
  *not* under test, which is the whole argument for pushing it down.

---

## Implementation plan

Phased so that each step is independently useful and reviewable.

### Phase 0 — upstream bug fixes (independent, low risk)

1. `ExecutePCTTest.java:146` — catch `Throwable`, not `Exception`, to match compiled.
2. `CompiledSupport.java:2633` — remove the second `/ 1000000`; `elapsedMs` is already
   milliseconds.

Both are in legend-pure and valuable regardless of the rest. They require the cross-repo rebuild
chain (legend-pure → legend-engine).

### Phase 1 — the per-case native — **implemented 2026-09-30**

3. Add `runOneCase(corpusId:String[1], path:SqlE2EPath[1]):String[0..1]` to `framework.pure` —
   corpus lookup plus the existing `executeLegend` body (superseded to the typed pair by §3's
   revision; originally landed as a single composite-string id, replaced same day).
4. Add `RUN_ONE_CASE` constant and the trap/classify helpers to `SqlE2ENativeHelper`, reusing the
   `PCTTools.unwrapExecutionError` / `getMessageFromError` calls already there.
5. Declare `executeSQLE2ETest(caseId, path):SqlE2ETestResult` in `framework.pure` (own type per §2's
   revision, not surveyor's `TestResult`).
6. Implement + register in `SqlE2ECompiledExtension` (`CoreGen.getSharedPureFunction` +
   `newCoreInstance`/`setValueForProperty`, mirroring `CompiledSupport.buildCompiledTestResult`) and
   `SqlE2EInterpretedExtension` (`NativeFunction`, `repository.newEphemeralAnonymousCoreInstance`,
   mirroring `ExecutePCTTest.buildTestResult`).
   - catches `Throwable`; writes `elapsed` in milliseconds directly
   - **the `[0..1]` null check is load-bearing** — absent means `PASS`. Confirmed correct in both
     runtimes: interpreted returns Java `null` (not `""`), compiled returns Java `null` (not the
     `"null"` string `String.valueOf` would have produced).
7. Added the null-resolution guard (a clear `IllegalStateException` if `RUN_ONE_CASE` doesn't
   resolve). **Not yet added:** a Pure test that calls `runOneCase` directly so a signature drift
   breaks a compile rather than only the native at runtime — still outstanding.

**Status note:** landed once with a composite string id (`"<corpusId>|TDS"`), then reworked the same
day to the typed `(corpusId, path:SqlE2EPath)` pair described in §3 — the composite-id version never
shipped past this local branch.

### Phase 2 — push result normalisation down — **implemented 2026-09-30**

Do this **before** Phase 3, so the driver rewrite lands on final comparison semantics rather than
having to be re-baselined twice.

8. Teach `ResultComparator.cellEquals` to canonicalise temporal values (`java.util.Date`,
   `java.sql.Date`/`Timestamp`, `java.time.*`) to epoch millis ahead of the `Number` branch. Land
   this on its own and confirm the compiled suite is unchanged — it should be a no-op there today.
9. Add a small adapter exposing a TDS JSON string as a `LegendExecutionResult` over
   `LegendTdsResultParser`, drive it through `LegendResultSet`'s processors, and collect a typed
   `ResultMatrix`. Empty `linearizedInheritances`, matching `LegendExecutionService`.
10. `compareToReference(caseId, resultJson):String[0..1]` native: build the actual matrix from the
    JSON, fetch the cached reference matrix for the case, apply `sorted()` to both when the case
    has no `ORDER BY`, and return `[]` or the joined diffs.
11. Rewrite `runOneCase` as `executeLegend` → `compareToReference`. Delete `toResultSet`,
    `normalizeValue`, `looksNumeric`, `looksLikeDatetime`, `normalizeNumeric`,
    `roundToSignificantDigits`, `rowKey`, `compareResults`, `nullToken`, `SqlE2EResultSet`,
    `SqlE2EResultRow`.
12. Drop the `NULL_TOKEN` sentinel from `matrixNode` — with comparison in Java the reference matrix
    no longer needs to survive a Pure `String[*]`, so nulls stay null. `matrixNode` is then only
    needed for `diagCase`-style display, not comparison.
13. Re-baseline: confirmed live against the LSP rather than a full corpus sweep (a full re-baseline
    of all 2,307 cases was not run this session) — spot-checked `smoke_tests` (16 pass / 4 error,
    matching the legacy driver exactly) plus targeted float (`AVG(numeric_val)`) and date/timestamp
    (`d + INTERVAL '1 day'`, `MAX(ts)`) cases, all PASS. **Outstanding:** a full-corpus interpreted
    sweep to confirm no other category shifted, and updating `KNOWN-GAPS.md`'s recorded baseline
    counts if it did.

### Phase 3 — id listing and the Pure drivers — **implemented 2026-09-30**

14. `resolveCaseRefs(filter):SqlE2ECaseRef[*]` native over `SqlE2ERunner.listIds`, crossed with both
    `SqlE2EPath` values (own type per §3's revision, not a delimited `String[*]`).
15. `executeSQLTests(refs:SqlE2ECaseRef[*]):SqlE2ETestReport` and
    `executeSQLTestCategory(category):SqlE2ETestReport` in Pure.
16. **Not done:** the 74 `run::<category>` wrappers still call the legacy `run(filter)` driver
    (kept working, rewritten to build `SqlE2EOutcome` from `executeSQLE2ETest` instead of the old
    `tryEval`/`runCaseSafe` pack-unpack scheme) rather than being rewritten as one-liners over
    `executeSQLTestCategory`. Both driver layers work and were verified independently; consolidating
    onto one is left for a follow-up pass.
17. Deleted `tryEval`, `runCaseSafe`, `errorPrefix`, `fieldDelim`. `SqlE2EOutcome` wraps a
    `SqlE2ETestResult` and keeps `expected`; `BUG` is now a `SqlE2ETestStatus` value directly (§2's
    revision), so the boolean `bug` field this step originally planned was never needed.

### Phase 4 — live corpus

18. `-Dsql.e2e.corpus.dir` support in `SqlE2ERunner`, falling back to the classpath.
19. `pure.options.SqlE2ECorpusLive` gate + `reloadSqlE2ECorpus()` native that rebuilds `corpus`,
    `knownTables` and clears the reference caches **without** touching the container.
20. Document both in `KNOWN-GAPS.md`, replacing the current "rebuild jar + restart daemon" note.

### Phase 5 — driver simplification

21. Point the parallel driver at `resolveCaseRefs` instead of parsing YAML.
22. Use category-level parallelism (the 74 wrappers) by default; keep per-case generation only for
    the categories that exceed the ceiling.
23. Chunk at **12**, not 16, to match `requestPoolSize` — the current 16 over-subscribes by 4 and
    makes each chunk roughly two waves rather than one.

### Phase 6 — scope enforcement (optional, do last)

24. Adopt the SDT consumption model: keep the module compile-scope internally, and require any
    consumer to declare it `<scope>test</scope>` (`runtime` for the Pure IDE). Today there are no
    consumers, so this is about making the contract explicit before one appears.

### Phase 7 — ad hoc SQL execution native — **implemented and verified live 2026-09-30**

Everything above operates on corpus cases: a case id resolves to SQL text via `SqlE2ECase`, which
only exists for rows recorded in `parity-tests/*.yaml`. There is no path from an arbitrary SQL
string a developer is iterating on to a PASS/FAIL/ERROR verdict without first adding it to the
corpus — a real friction point when exploring whether a *new* construct is supported before deciding
it's worth a permanent corpus entry.

25. Add a native that takes raw SQL text (not a corpus id) and a `SqlE2EPath` — using `Both` here is
    meaningful, unlike in `runOneCase`, since there is no pair of pre-rewritten `tds_`/`rel_`
    accessor SQL to choose between; the same input SQL is routed through both source-provider
    prefixes if `Both` is requested:

    ```pure
    Class meta::external::query::sql::e2e::SqlE2EAdhocResult
    {
      path: SqlE2EPath[1];        // TDS or Relation - never Both, one per path actually run
      legendResultJson: String[0..1];   // raw TDS JSON, present unless legendError is
      legendError: String[0..1];
      referenceRows: String[0..1];      // reference Postgres result, rendered for display
      referenceError: String[0..1];
      matched: Boolean[0..1];           // present only when both sides executed without error
    }

    native function meta::external::query::sql::e2e::executeAdhocSQL(
        sql:String[1], path:SqlE2EPath[1]) : SqlE2EAdhocResult[*];
    ```

    Returns `[*]` (one or two results) rather than `[1]` so a `Both` request yields both paths'
    results in one call instead of forcing two round trips.
26. Java side: extend `SqlE2ERunner` (or a thin sibling) with an adhoc-SQL entry point that runs the
    reference side via the existing `DirectPostgresRunner` — no corpus lookup, no reference cache,
    since there is no stable id to cache against; every call re-executes against Postgres. The
    Legend side reuses `executeLegend` unchanged (it never depended on the corpus beyond the SQL
    string and prefix).
27. Unlike `runOneCase`, this native does **not** collapse to PASS/FAIL/ERROR — there is no baseline
    to compare against, only "did each side execute, and if both did, did they agree." Hence the
    richer `SqlE2EAdhocResult` shape instead of reusing `SqlE2ETestResult`.
28. A thin `pure-lsp`-friendly wrapper (`diagAdhoc(sql, path):String[1]`, formatting
    `SqlE2EAdhocResult` for `println`) gives the same `diagCase`-style convenience this ADR already
    relies on for manual verification, without needing a corpus entry first.

**Implementation notes (deviations from the plan above, all minor):**

- `executeLegend` needs a `SqlE2EConnection` and an `Extension[*]`, both of which `runOneCase` gets
  from the corpus. Ad hoc SQL has neither, so a second tiny native,
  `sqlE2EConnectionJson():String[1]`, and its Pure wrapper `connection():SqlE2EConnection[1]`, were
  added — same deserialisation pattern as `corpus()`, standalone rather than nested in a case.
  `Extension[*]` needed no native at all: `meta::relational::extension::relationalExtensions()` is
  already a plain Pure function.
- Step 26 undersold the split: `SqlE2ERunner.prepareAdhoc(sql, path)` does the FROM-rewrite and
  reference execution in Java and returns both (plus any errors) to the caller, but the Legend side
  is **not** "executeLegend unchanged" called directly from Java - it goes through a new Pure
  function, `runOneAdhocLegend(rewrittenSql, prefix):String[1]`, resolved and invoked by
  `executeAdhocSQL` the same way `executeSQLE2ETest` resolves `RUN_ONE_CASE` (§3). This preserves
  the ADR's core invariant that everything under test stays in Pure, live-editable - `executeLegend`
  itself was never going to be called directly from a native without an intermediate Pure function,
  the same reasoning §3 gives for `runOneCase`.
- `matched` is deliberately **best-effort**: `SqlE2ENativeHelper.matchAgainstReference` returns
  `null` (rather than throwing) if parsing Legend's own JSON fails after a successful execution -
  an edge case, but one where discarding an already-obtained `legendResultJson` to report a
  secondary parse failure would be the wrong trade-off for a diagnostic tool.

**Verified live** (2026-09-30, warm interpreted LSP, no corpus entry for any of these three SQL
strings):
- `SELECT id, name FROM persons WHERE age > 30 ORDER BY id` with path `Both` → two results (TDS,
  Relation), both `matched=true`, both showing full Legend JSON and rendered reference rows.
- `SELECT (NULL = NULL) AS result FROM persons WHERE id = 1` with path `TDS` → `matched=false`,
  reproducing the known `NULL = NULL` divergence (§ "Verified live" note on §5) with no corpus case
  required.
- `SELECT * FROM this_table_does_not_exist` with path `TDS` → both `legendError` ("no cte named ...
  found") and `referenceError` ("relation ... does not exist") populated independently, with
  `matched` correctly absent — confirming the two failure channels don't get conflated into one.

### Phase 8 — reduce the native surface: fold corpus fetch, comparison, and connection into three
natives — **implemented and verified live 2026-09-30**

After Phases 1–3 and 7, the native surface had grown to six declarations
(`sqlE2ECasesJson`/`corpus()`, `sqlE2EConnectionJson`/`connection()`, `compareToReference`,
`resolveCaseRefs`, `executeSQLE2ETest`, `executeAdhocSQL`) — more than the "one native, Pure drivers"
architecture in §1 needs. Three of the six exist only to smuggle plain data across the Java/Pure
boundary as JSON, a pattern this document already argues against in §4 for the comparison path.
This phase applies the same reasoning to the rest of the surface.

**29. `compareToReference` is no longer Pure-visible.** `executeSQLE2ETest` already resolves and
invokes `runOneCase` from Java; it can call `SqlE2ERunner.compareToReference` directly afterward,
Java-to-Java, with no Pure hop in between. `runOneCase` changes from returning a diff (`String[0..1]`,
via `compareToReference`) to returning the raw Legend JSON directly (`String[1]`) on the success
path; `SqlE2ENativeHelper.classifySuccess(corpusId, hasOrderBy, raw)` does the `__SKIP__`/`__BUG__`
prefix check and, for anything else, the comparison and PASS/FAIL classification, all in one method
shared by both runtimes.

**30. `resolveCaseRefs` absorbs `sqlE2ECasesJson`/`corpus()` entirely**, deleting the native, and the
`SqlE2ECorpus`/`SqlE2ECase` classes it existed to deserialise into. The insight: `SqlE2ECaseRef` is
already scoped to one `(corpusId, path)` pair — exactly the granularity where `legendSqlTds` vs
`legendSqlRelation` and `expectedTdsStatus` vs `expectedRelStatus` get resolved down to a single
value anyway. Expanding the ref to carry that resolution moves the "look up this case's data" work
that `corpus()` did from being a **separate, repeatable** Pure-visible fetch to a **one-time**
Java-side resolution (`SqlE2ERunner.resolveCaseRef`) reused by both the bulk (`resolveCaseRefs`) and
single-case (`executeSQLE2ETest`'s internal rebuild, see decision 32) paths:

```pure
Class meta::external::query::sql::e2e::SqlE2ECaseRef
{
  corpusId: String[1];
  path: SqlE2EPath[1];
  sql: String[0..1];            // rewritten for this path; empty if skip/bugReason/rewriteError is set
  hasOrderBy: Boolean[1];
  skip: String[0..1];
  bugReason: String[0..1];
  rewriteError: String[0..1];
  expectedStatus: String[0..1]; // the recorded baseline for this path
}
```

`runOneCase`'s signature changes from `(corpusId:String[1], path:SqlE2EPath[1])` to
`(ref:SqlE2ECaseRef[1], conn:SqlE2EConnection[1])` — a straight-line read of fields the caller
already resolved, with no lookup of its own:

```pure
function meta::external::query::sql::e2e::runOneCase(ref:SqlE2ECaseRef[1], conn:SqlE2EConnection[1]):String[1]
{
  assert($ref.path != SqlE2EPath.Both, | 'runOneCase requires a single path (TDS or Relation), got Both');
  if($ref.skip->isNotEmpty(), | '__SKIP__' + $ref.skip->toOne(),
    | if($ref.bugReason->isNotEmpty(), | '__BUG__' + 'postgres rejected reference SQL: ' + $ref.bugReason->toOne(),
        | if($ref.rewriteError->isNotEmpty(), | fail('rewrite error: ' + $ref.rewriteError->toOne());'';,
            | executeLegend($ref.sql->toOne(), if($ref.path == SqlE2EPath.TDS, |'tds_', |'rel_'),
                            $conn, meta::relational::extension::relationalExtensions())
          )
      )
  );
}
```

**31. `sqlE2EConnectionJson`/`connection()` are also deleted**, not kept as a shared accessor as
originally planned when this phase was first scoped (see the discussion trail below) — both
`executeSQLE2ETest` and `executeAdhocSQL` now build a `SqlE2EConnection` CoreInstance directly in
Java (`buildConnectionInstance`, the same `newCoreInstance`/`setValueForProperty` pattern already
used for every other class this module builds) and pass it as a parameter to `runOneCase` /
`runOneAdhocLegend`, rather than having the invoked Pure function fetch it itself through a native.
The connection is five static config fields (host/port/database/user/password) - building it twice,
once per native, costs less than the JSON round trip it replaces.

**32. `executeSQLE2ETest` keeps its `(caseId, path)` signature**, deliberately not switched to
accept a `SqlE2ECaseRef[1]` directly even though `executeSQLTests` already has one in hand from
`resolveCaseRefs` when it calls this native per-ref. The alternative was considered and rejected: taking
a ref directly is marginally cheaper for that one caller, but breaks the "run this one case by id
string" convenience every generated per-category wrapper and manual diagnosis call relies on, and
those calls never have a ref sitting in hand to pass. `executeSQLE2ETest`'s Java implementation
rebuilds the ref internally via `SqlE2ERunner.resolveCaseRef` - a cheap in-memory map lookup for an
already-loaded corpus entry, not the JSON-generating, Postgres-querying round trip the old `corpus()`
call was. The redundant rebuild when a caller already has a ref (the `executeSQLTests` case) is the
accepted cost of keeping one signature instead of two.

**33. `runQuiet` (the legacy `run`/`divergenceReport` driver) is rewritten onto `resolveCaseRefs`**,
closing out the Phase 3 status note that flagged this driver as still using the old bulk-fetch
pattern:

```pure
function meta::external::query::sql::e2e::runQuiet(filter:String[1]):SqlE2EOutcome[*]
{
  meta::external::query::sql::e2e::resolveCaseRefs($filter)->map(ref | meta::external::query::sql::e2e::runOutcomeFor($ref))
}
```

`SqlE2EOutcome.expected` now reads directly from `ref.expectedStatus` (already resolved by
`resolveCaseRefs`) instead of being threaded through as a separate parameter computed inline in
`runQuiet` from a raw `SqlE2ECase`.

**Net: the native surface drops from six declarations to three** — `resolveCaseRefs`,
`executeSQLE2ETest`, `executeAdhocSQL`. All Pure-side SQL-under-test logic (parse → route → plan-gen
→ dialect SQL, in `sqlSources`/`executeLegend`/`runOneCase`/`runOneAdhocLegend`) is unchanged; what
moved is exactly the parts §1 says should never have been separate natives in the first place -
data marshalling and comparison that Java could always do directly once it was already the one
resolving and invoking the Pure work function.

**Verified live** (2026-09-30, warm interpreted LSP, fresh daemon after the native-surface change):
all five `SqlE2ETestStatus` values re-confirmed on the same cases as decision 30's "Phase 3"
verification (`smoke_select_column` → PASS, `null_equals_null` → FAIL with the identical
`ResultComparator` message, `abbrev__cidr__unsupported` → SKIP, `cast_numeric_to_int` → BUG,
`avg__num__from_table`/`max__ts__from_table`/`interval_date_plus_days` → PASS, confirming float and
date/timestamp normalisation are unaffected); `executeAdhocSQL` with path `Both` re-confirmed
matching both paths; `resolveCaseRefs` on both a normal case (`smoke_select_column`, both refs carrying
correctly-resolved `sql`/`hasOrderBy`/`expectedStatus`, empty `skip`/`bugReason`/`rewriteError`) and
a skipped case (`abbrev__cidr__unsupported`, both refs carrying the skip reason with `sql` empty and
no rewrite/reference attempted) confirm the corpus-fetch consolidation is correct for both the
normal and short-circuit paths. **Not verified this pass:** a full-category sweep (`smoke_tests`,
20 cases) - two attempts hit the LSP bridge's request-handling limits after several other
already-completed diagnostic calls had been left running server-side on the same daemon (an
operational mistake in this session, not a defect in the native surface itself: `pure-lsp
cancel-tests --all` should have been called before moving on, per the known daemon-hygiene trap
already documented in the dev-loop guide). Per-case verification stands in for it here; a
full-category sweep against a clean daemon is recommended before treating this phase as fully closed.

### Phase 9 — drop the sentinel scheme from `runOneCase`; rename `resolveCaseRefs` — **implemented and verified live 2026-09-30**

Two follow-on cleanups once decision 30 put `skip`/`bugReason`/`rewriteError` on `SqlE2ECaseRef`,
fully resolved before `executeSQLE2ETest` ever calls `runOneCase`:

**34. The `__SKIP__`/`__BUG__` sentinel-prefix scheme moves out of `runOneCase` and into Java.**
`executeSQLE2ETest`'s Java implementation already has the resolved `ref` in hand *before* deciding
whether to invoke `runOneCase` at all (see decision 32's rebuild-via-`resolveCaseRef`) — so encoding
"this is a skip" as a magic string prefix on `runOneCase`'s return, only to parse it back out in
`SqlE2ENativeHelper.classifySuccess` a few lines later in the same language, was a Pure hop that
bought nothing. `SqlE2ENativeHelper.classifyShortCircuit(ref)` now checks `skip`/`bugReason`/
`rewriteError` directly in Java and returns a `Classification` (or `null` if there is real SQL to
run) *before* `runOneCase` is resolved or invoked at all — skip/bug/rewrite-error cases now pay for
neither a `package_getByUserPath` lookup nor a `CoreInstance` build. `runOneCase` drops from a
three-level nested `if`/`fail()` to a straight-line body:

```pure
function meta::external::query::sql::e2e::runOneCase(ref:SqlE2ECaseRef[1], conn:SqlE2EConnection[1]):String[1]
{
  assert($ref.path != SqlE2EPath.Both, | 'runOneCase requires a single path (TDS or Relation), got Both');
  let prefix = if($ref.path == SqlE2EPath.TDS, | 'tds_', | 'rel_');
  let ext = meta::relational::extension::relationalExtensions();
  meta::external::query::sql::e2e::executeLegend($ref.sql->toOne(), $prefix, $conn, $ext);
}
```

It is only ever invoked when `ref.sql` is non-empty, which `classifyShortCircuit` returning `null`
already guarantees. `SqlE2ENativeHelper.SKIP_PREFIX`/`BUG_PREFIX` are deleted along with the
prefix-matching in `classifySuccess`, which now does only the reference comparison.

**35. `resolveCaseRefs` replaces `listCaseIds`** (mechanical rename across the Pure native, both
runtime extension classes, `SqlE2ENativeHelper`, `SqlE2ERunner`, and their tests/docs). The old name
was accurate when the native returned bare ids; since decision 30 it returns fully-resolved
`SqlE2ECaseRef`s, and `listCaseIds` alongside `SqlE2ERunner.listIds` (which genuinely does return
bare ids) was a standing source of confusion. `resolveCaseRefs` pairs it with the existing singular
`resolveCaseRef(corpusId, path)` already used internally by `executeSQLE2ETest`.

**Verified live** (2026-09-30, warm interpreted LSP, fresh daemon after this change — the daemon
launch itself surfaced that the cached engine-scale classpath does not include
`legend-engine-xt-sql-e2e-pure`/`-tests`; a merged classpath combining the base engine classpath with
`mvn dependency:build-classpath` output for `legend-engine-xt-sql-e2e-tests` plus both modules'
`target/classes` was required): `abbrev__cidr__unsupported` → `SKIP (unsupported type category)`,
`op_div__by_zero__error` → `BUG (postgres rejected reference SQL: ERROR: division by zero)` — both
confirmed short-circuited without a `runOneCase` invocation — and `smoke_select_column` → `PASS`,
confirming the still-invoked comparison path is unaffected. `mvn clean install` + `TestSqlE2ERunner`
(7/7) green beforehand.

---

## Testing

- **Phase 0:** existing PCT suites must stay green; add a PCT test that throws an `AssertionError`
  interpreted and assert the run continues.
- **Phase 1 — done:** all five classification paths (`[]` → `PASS`, `__SKIP__`-prefixed → `SKIP`,
  `__BUG__`-prefixed → `BUG`, other non-empty → `FAIL`, throw → `ERROR`) confirmed live in the
  interpreted runtime on real corpus cases (§5 records the specific cases and outputs). **Not yet
  done:** the equivalent confirmation against the **compiled** runtime — everything verified this
  session went through the interpreted LSP only, since that is the harness's own execution path; the
  compiled native (`SqlE2ECompiledExtension.executeSQLE2ETest`) built and linked successfully
  (mangled ids resolved, no "native not implemented" errors) but was never actually invoked. A JUnit
  test exercising it directly (or via a compiled `TestPostgresParity`-style harness) is outstanding,
  along with unit tests for `SqlE2ENativeHelper.classifySuccess`/`classifyThrow`, which currently
  have no direct test coverage in either runtime.
- **Phase 2 — done, narrower than originally planned:** step 8 (`ResultComparator` temporal
  canonicalisation) was unit-tested directly (`TdsJsonResultMatrixParsesTypedValuesIncludingNulls`,
  `compareToReferenceDetectsRowCountMismatch` in `TestSqlE2ERunner`) and confirmed live for both
  `StrictDate` and `DateTime` columns (§5). The **directional-only** check this section originally
  specified (interpreted may only move `FAIL → PASS` on float cases, never the reverse) was not run
  as a full-corpus diff — only spot checks were done, so a change in an unrelated category would not
  yet have been caught.
- **Phase 3 — partially done:** `smoke_tests` interpreted counts reproduced the legacy driver's
  count exactly (§5). A full-corpus sweep comparing pre- and post-rewrite counts across all
  categories was not run.
- **Phase 4:** edit a corpus SQL, call `reloadSqlE2ECorpus()`, confirm the new SQL executes with no
  restart; confirm the Postgres container is not restarted.
- **Phase 7 — done:** `executeAdhocSQL` has no baseline to check against by construction, so
  verification was structural rather than a pass/fail comparison. Confirmed live: `matched=true` on
  a real query never recorded as a corpus case, with `Both` correctly returning independent TDS and
  Relation results in one call; `matched=false` reproducing a known divergence (`NULL = NULL`) with
  no corpus entry; and `legendError`/`referenceError` populating **independently** (both set, for
  different reasons, on the same input) with `matched` correctly absent when either side fails.
  **Not done:** no automated test (JUnit or Pure) exists for this yet — only manual LSP verification,
  same gap as Phases 1–3's compiled-runtime coverage.
- **Phase 8 — done, with one gap:** per-case re-verification of all five statuses plus ad hoc `Both`
  and `resolveCaseRefs`'s expanded-ref shape (both the normal and skip short-circuit paths) done live
  against a freshly-restarted daemon (§ Phase 8's own "Verified live" note has the specifics).
  **Not done:** a full-category sweep re-confirming the exact `smoke_tests` counts from Phase 3's
  verification - two attempts hit the LSP bridge's limits due to an accumulation of abandoned
  server-side executions from earlier in the same session, not a defect surfaced in the native
  surface itself. Recommended before closing this phase out: `pure-lsp cancel-tests --all` (or a
  fresh daemon) followed by `executeSQLTestCategory('smoke_tests')`, expecting the unchanged
  `pass=16 fail=0 error=4 skip=0`.
- **Full compiled-suite regression check (2026-09-30):** ran `TestPostgresParity` in full —
  4614 dynamic tests, 0 failures after triage. The run surfaced 6 `FIX DETECTED` assertions (3
  case ids × 2 paths: `op_add__date_plus_int_days`, `date_literal__fractional_6_digits`,
  `date_literal__fractional_9_digits`) — all previously-`FAIL` fixtures now passing, consistent
  with the §4 temporal-canonicalisation work landing in `ResultComparator.cellEquals` earlier this
  session. Re-ran each id individually to confirm the pass is a genuine value match and not a
  fixture/harness artifact, then updated `expected_tds_status`/`expected_rel_status` to `PASS` in
  `parity-tests/operators/math_operators.yaml` and `parity-tests/functions/date_literals.yaml` per
  `assertNoRegression`'s own instructions. This is independent confirmation that the natives
  refactor (Phase 8) introduced no regression in the compiled runtime, which shares
  `ResultComparator`/`ResultMatrix`/`TestCaseLoader`/`AstFromRewriter` with the interpreted dev loop
  but not `SqlE2ERunner`/the natives themselves.
- Throughout: **confirm any apparent new divergence against the compiled suite**
  (`-Dtest.filter=<id>`) before treating it as real — every divergence investigated so far has
  been an interpreted-mode or harness artifact.

---

## References

- [SQL e2e interpreted dev loop](../guides/sql-e2e-interpreted-devloop.md)
- [Known gaps](../../../legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests/KNOWN-GAPS.md)
- `platform/pure/essential/tests/surveyor.pure` — `TestResult`/`TestStatus`/`TestReport`,
  `executeTest`, `executePCTTest`, `runTests`
- `CompiledSupport.java:2411-2547` / `ExecutePCTTest.java` — the trap-and-classify precedent
- `PCTTools.java` — `unwrapExecutionError`, `getMessageFromError`
- `sdtFramework.pure:225-245` — `runSdtTestsInIDE`, the untrapped driver
- `LegendPureSession.java:1561-1564` — fresh executor per request
- `ResultComparator.java` / `ResultMatrix.java` — the compiled suite's comparison and sort
- `LegendResultSet.java:157-214` — the type-processor map keyed on `LegendDataType`
- `LegendTdsResultParser.java:100-117` — TDS JSON → `LegendColumn`s + rows, the reuse point
- `SqlE2ERunner.java:388-410` — `matrixNode`, the reference side's `String.valueOf` rendering
