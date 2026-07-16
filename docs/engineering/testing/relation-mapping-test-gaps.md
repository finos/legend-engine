# Relation Mapping Test Gap Analysis

Audit of test coverage for **relation (function) class mappings** — the mapping form
`ClassName: Relation { ~func f():Relation<Any>[1] | ~src <expr>; col: COL }` — across
the three unit-test layers plus the EMIT integration layer.

This document reflects the codebase **after PR #4941 ("Relation Function Class Mapping
improvements")**, which materially changed the feature and its test surface (see
[Section 2](#section-2--what-4941-changed)). It supersedes the earlier gap analysis,
which predated #4941.

Development status for each gap is one of:

| Status | Meaning |
|:---|:---|
| `DONE` | Covered today (noted so the matrix stays honest and to prevent re-duplication) |
| `TEST_ONLY` | Engine feature works; only test authoring is needed |
| `NEW_DEV` | Engine change required before a test can pass |
| `UNCERTAIN` | Needs a probe test to classify as `TEST_ONLY` or `NEW_DEV` |

---

## Section 1 — Current Test Surface

Relation mapping is now the second-most-heavily-tested mapping form in the relational
store (after classic `Relational`). All Pure tests run against **H2** via the compiled
`Test_Pure_Relational` harness and are stereotyped `<<test.Test, test.AlloyOnly>>`.

### Java (grammar + compiler)

| File | Relation tests | Notes |
|:---|:---:|:---|
| `TestRelationFunctionMappingCompilation` | 36 | Added by #4941. Dedicated compiler suite: `~src`, expression-RHS, multiplicity/type validation, quoted columns, embedded, negative paths |
| `TestMappingCompilationFromGrammar` | 18 | Core grammar-level compile checks (valid/invalid function pointer, PK, embedded, union, include) |
| `TestMappingGrammarParser` | 7 | Parse-only: valid/faulty mapping, embedded, inline embedded, PK parsing, source-information, enumeration |
| `TestMappingGrammarRoundtrip` | 8 | Parse→compose fidelity: plain, embedded, PK, testSuite, union, include, extends |
| `TestRelationalCompilationFromGrammar` | ~15 | Relational-store specifics: PK auto-infer (bare/through-join/explicit), Binding, EnumerationMapping, testSuite, tabular func |

### Pure (`<<test.Test, test.AlloyOnly>>`, executed on H2)

| File | Tests | ToFix | Feature focus |
|:---|:---:|:---:|:---|
| `mapping/relation/tests.pure` | 56 | 0 | Simple, association, pre-filter, embedded, inline-embedded, enumeration, groupBy, window column, milestoning (all temporal types + `allVersionsInRange`), mixed Relation+Relational, `~src`, expression-RHS |
| `mapping/relation/pkInferenceTests.pure` | 43 | 0 | PK inference (bare accessor, through-join, explicit) and PK-of-func analysis |
| `mapping/relation/aggregation/testRelationFunctionAggregation.pure` | 9 | 2 | Sub-aggregation via collection navigation |
| `mapping/relation/coverage/relationCoverageDistinct.pure` | 14 | 0 | `->distinct()` in function body (NEW) |
| `mapping/relation/coverage/relationCoverageFilter.pure` | 3 | 0 | `->filter()` in function body, incl. join-in-filter (NEW) |
| `mapping/relation/coverage/relationCoverageGroupBy.pure` | 12 | 0 | `->groupBy()` in function body, multi-aggregate (NEW) |
| `mapping/relation/coverage/relationCoverageDates.pure` | 10 | 0 | `StrictDate` / `DateTime` typed properties (NEW) |
| `mapping/union/relation/testRelationUnion.pure` | 15 | 1 | Union of Relation sets, mixed Relation+Relational union |
| `mapping/union/relation/testRelationUnionAdvanced.pure` | 12 | 12 | Advanced union model-join traversal (all blocked) |
| `mapping/modelJoin/testModelJoinSimple.pure` | 10 | 0 | ModelJoin (relation-set association) — simple |
| `mapping/modelJoin/testModelJoinAdvanced.pure` | 23 | 1 | ModelJoin — nested, sub-filter, sub-aggregation, qualified props |
| `mapping/modelJoin/testModelJoinMilestoning.pure` | 9 | 1 | ModelJoin across milestoned relation sets |
| `mapping/modelJoin/testModelJoinUnion.pure` | 6 | 2 | ModelJoin over union of relation sets |

**Totals:** ~84 Java + ~222 Pure relation-mapping test functions. **19 Pure tests are
`<<test.ToFix>>`** (engine work required — see [Section 5](#section-5--known-tofix-tests-new_dev)).

### EMIT (integration)

**Zero** relation-mapping EMIT models exist. Six classic `Relational` models plus two
shared-fixture models exist as templates. See [Section 6](#section-6--emit-integration-tests).

---

## Section 2 — What #4941 Changed

#4941 is the reference point for this audit. Its test-relevant changes:

1. **Grammar — two new source forms.**
   - `~src <expression>` as an alternative to `~func <descriptor>`: an inline zero-arg
     Pure expression evaluating to a `Relation`, wrapped by the walker in a synthetic
     lambda.
   - Property RHS may now be a **full Pure expression over `$src`**, not only a bare
     column (`amount: $src.QTY * $src.PRICE`). Protocol gained
     `RelationFunctionClassMapping.sourceLambda` and `RelationFunctionPropertyMapping.valueFn`;
     the composer round-trips both forms.

2. **Multiplicity-subsumption validator (most impactful for test authoring).**
   A relation property mapping now fails to compile with
   `"Multiplicity Error: The property 'X' has a multiplicity range of [1] when the given
   expression has a multiplicity range of [0..1]"` when the property multiplicity does
   not subsume the mapped column/expression multiplicity. Because a **nullable** column
   infers as `[0..1]`, **a `[1]` property must be backed by a `NOT NULL` column** (or a
   `->toOne()` expression). This drove the design of the `coverage` store, whose columns
   are declared `NOT NULL` wherever a `[1]` property is mapped. Type subsumption is also
   validated (primitive subtype allowed, supertype rejected; `Boolean`≠`String`).

3. **New dedicated compiler suite** `TestRelationFunctionMappingCompilation` (36 tests).

4. **Previously-`ToFix` tests promoted to passing** in `tests.pure`, including
   `testMixedTemporalMappingWithAllVersionsInRange1` and `testMixedMappingWithFilterInProject`
   (mixed-temporal self-join isolation), and `testMixedCaseMappingThroughAssociationChain`.

5. **Union / ModelJoin routing improvements** (`resolveOperationPreservingRouting`,
   `findMainRelation`, widened union builder signatures) — unblocked most basic union
   and simple/advanced ModelJoin tests. Advanced union traversal remains blocked
   ([Section 5](#section-5--known-tofix-tests-new_dev)).

---

## Section 3 — Feature Coverage Matrix

Reference base: classic `Relational` tests under `.../relational/tests/mapping/`.

| # | Feature | Relational ref | Relation `<<test.Test>>` | Java | Gap? | Status |
|:--|:---|:---:|:---:|:---:|:---:|:---:|
| 1 | Simple property mapping | Y | Y | Y | — | DONE |
| 2 | Association / XStore | Y | Y | Y | — | DONE |
| 3 | ModelJoin association | (rel-only) | Y (48) | Y | — | DONE |
| 4 | Embedded mapping | Y | Y | Y | — | DONE |
| 5 | Inline embedded (`() Inline`) | Y | Y | Y | — | DONE |
| 6 | Class-level filter (in `~func` body) | Y | Y (coverage) | — | — | DONE |
| 7 | GroupBy (in `~func` body) | Y | Y (coverage) | — | — | DONE |
| 8 | Distinct (in `~func` body) | Y | Y (coverage) | — | — | DONE |
| 9 | Enumeration mapping | Y | Y (8+) | Y | — | DONE |
| 10 | Binding transformer | Y | (compile only) | Y | Partial | TEST_ONLY |
| 11 | Local properties (`+prop`) | Y | Y (implicit) | Y | Partial | TEST_ONLY |
| 12 | `~src` inline source | n/a | Y (2) | Y | Partial | TEST_ONLY |
| 13 | Expression RHS (`$src.x + …`) | n/a | Y (3) | Y | Partial | TEST_ONLY |
| 14 | Union (Relation + Relation) | Y | Y (14) + advanced ToFix | Y | Partial | MIXED |
| 15 | Mixed union (Relation + Relational) | Y | Y (basic) + ToFix | Y | Partial | MIXED |
| 16 | Mapping include | Y | (compile/roundtrip) | Y | Partial | UNCERTAIN |
| 17 | Milestoning — processing/business/bi-temporal | Y | Y | — | — | DONE |
| 18 | Milestoning — `allVersionsInRange` | Y | Y | — | — | DONE |
| 19 | Milestoning — `allVersions()` (unbounded) | Y | N | — | **YES** | UNCERTAIN |
| 20 | Window functions | N | Y (1) | — | — | DONE |
| 21 | Primary key inference / explicit PK | (implicit) | Y (43) | Y | — | DONE |
| 22 | Sub-aggregation (collection agg) | N | Y (7) + 2 ToFix | — | Partial | MIXED |
| 23 | Date / DateTime typed properties | Y | Y (coverage) | Y | — | DONE |
| 24 | Boolean typed properties | Y | N | — | **YES** | TEST_ONLY |
| 25 | Computed column (SQL func / concat) | Y | Y (`testRfpmLiftMixedConcat`, arithmetic) | Y | Partial | TEST_ONLY |
| 26 | Subtype / inheritance | Y | N | — | **YES** | NEW_DEV |
| 27 | Mapping extension (`extends [setId]`) | Y | (roundtrip only) | Y | **YES** | NEW_DEV |
| 28 | Union `importDataFlow` / PK discriminator | Y | 1 ToFix | — | **YES** | NEW_DEV |
| 29 | Graph fetch (`->graphFetch()/serialize()`) | Y | N | — | **YES** | UNCERTAIN |
| 30 | Model chain / store substitution | Y | N | — | **YES** | UNCERTAIN |
| 31 | Service + plan generation | Y (EMIT) | N | — | **YES** | TEST_ONLY (EMIT) |
| 32 | testSuites / Testables ("Run Tests") | Y | N (SPI stub) | (compile only) | **YES** | NEW_DEV |

---

## Section 4 — Remaining Unit-Test Gaps

Gaps that are `TEST_ONLY` (author now) or `UNCERTAIN` (probe first). `NEW_DEV`
blockers are listed separately in [Section 5](#section-5--known-tofix-tests-new_dev).

### G-1 — Boolean typed properties — `TEST_ONLY`
No relation test maps a `Boolean` property. `testDB.personTable.IS_MALE` already exists
(`NOT NULL`). Add `testBooleanProperty` / `testBooleanPropertyInFilter` on a
`Boolean[1]` property.

### G-2 — Local property (`+prop`) dedicated coverage — `TEST_ONLY`
`+prop` is exercised pervasively but has no dedicated test file. Add tests for a `+prop`
used as a filter predicate and a `+prop` whose value is a computed expression.

### G-3 — `~src` inline source — broaden — `TEST_ONLY`
Only `testExplicitSrcMappingQuery` / `testExplicitSrcMappingWithAssociation` exist. Add
`~src` variants for embedded, groupBy-in-src, filter-in-src, and a union leg backed by
`~src`, mirroring the `~func` coverage.

### G-4 — Expression RHS — broaden — `TEST_ONLY`
`testExpressionMappingArithmetic(WithFilter)` and `testRfpmLiftMixedConcat` exist. Add
expression-RHS combined with enumeration transform, in a groupBy mapping, and an
explicit `->toOne()` coercion of a nullable column to a `[1]` property (the sanctioned
alternative to a `NOT NULL` column under the multiplicity validator).

### G-5 — Binding transformer execution — `TEST_ONLY`
Binding is covered at compile/roundtrip only. Add an execution test that decodes a
semi-structured column through a `Binding` on a relation-mapped property.

### G-6 — `allVersions()` (unbounded milestoning) — `UNCERTAIN`
`allVersionsInRange` is covered; unbounded `allVersions()` is not. Probe with
`testMappingWithProcessingTemporalMilestoningAllVersions` to classify.

### G-7 — Graph fetch — `UNCERTAIN`
All relation tests use TDS `->project()`. Probe `Person.all()->graphFetch(#{…}#)->serialize(…)`
on a simple relation mapping. (For groupBy/distinct, classic MFT marks graph fetch as an
error — those are expected-unsupported.)

### G-8 — Model chain / mapping include (execution) — `UNCERTAIN`
Include compiles and round-trips; no test executes a query through an include chain that
resolves to a relation set. Probe `testRelationMappingInclude`.

---

## Section 5 — Known `ToFix` Tests (`NEW_DEV`)

19 written-but-blocked Pure tests. Each is engine work; the test is the acceptance
criterion.

| Area | Count | Root cause |
|:---|:---:|:---|
| `testRelationUnionAdvanced.pure` | 12 | Advanced union model-join traversal: `RelationFunction cannot be cast to NamedRelation` (filter-from-child), empty-collection cast (reverse traversal), `TdsSelectSqlQuery cannot be cast to NamedRelation` (mixed rfsi+Relational), H2 GROUP BY column quoting for aggregated rfsi legs |
| `testRelationUnion.pure` | 1 | `testUnionTwoRelationMappings_PksWithImportDataFlow` — `U_TYPE` discriminator column not surfaced by rfsi subqueries for `importDataFlow=true` PK propagation |
| `testRelationFunctionAggregation.pure` | 2 | Sub-aggregation SQL generation edge cases |
| `testModelJoinAdvanced.pure` | 1 | Remaining advanced ModelJoin traversal case |
| `testModelJoinMilestoning.pure` | 1 | Milestoned ModelJoin self-join isolation |
| `testModelJoinUnion.pure` | 2 | ModelJoin over union legs |

Additional `NEW_DEV` items with **no** test yet (blocked before authoring):

- **Subtype / inheritance** (matrix #26): `meta::relational::mapping::findMappingForType`
  polymorphic dispatch does not handle `RelationFunctionInstanceSetImplementation`.
- **Mapping extension `extends`** (matrix #27): `RelationFunctionClassMappingSecondPassCompiler`
  extend-chain processing. Grammar round-trips today (`testRelationFunctionMappingWithExtends`)
  but execution/second-pass is unimplemented.
- **Testables SPI** (matrix #32): `RelationAccessorTestConnectionFactory` is a stub;
  wiring it is engine development, not test authoring.

---

## Section 6 — EMIT Integration Tests

EMIT (Engine Model Integration Test) drives the **full six-phase pipeline** —
INITIALIZATION → PARSE → COMPILE → MODEL_GENERATION → FILE_GENERATION → TEST_EXECUTION →
PLAN_GENERATION — from an assembled `.pure` model plus a `*.emit.yaml` descriptor. It is
the only layer that exercises grammar parsing, compilation, testSuite execution, and plan
generation together, from scratch, the way a Studio/HTTP client would. Unit tests prove
correctness breadth; **EMIT proves the pipeline works end-to-end for representative
scenarios** and is intentionally a *subset* of the unit-test matrix.

- **Runner:** `legend-engine-xt-relationalStore-emit/src/test/java/.../RelationalEMITTests.java`
- **Models:** `.../emit-models/` (today: 6 classic `relational-*` models + `relational-shared-domain`,
  `relational-shared-firm-db` fixtures; **zero relation models**)
- **Data mechanism:** `testSuites:` blocks referencing a `Data` element via
  `Reference #{ path }#`, or inline `Relation #{ … }#` / `ExternalFormat` embedded data.

Each relation model **must** respect the #4941 multiplicity validator: every `[1]`
property must be backed by a `NOT NULL` column (or a `->toOne()` expression). Reuse the
`relational-shared-*` fixtures where the shape matches.

### Planned EMIT models

| Model | Mirrors | Exercises | Status |
|:---|:---|:---|:---:|
| `relation-simple` | `relational-simple` | `~func` returning `Relation<(…)>`, plain property mapping, `EqualToJson` testSuite. Foundational — the first end-to-end relation pipeline check. | TEST_ONLY |
| `relation-src` | (new) | `~src <expr>` inline source form through the full pipeline (grammar composer + compile + execute). Guards the #4941 inline-source path end-to-end. | TEST_ONLY |
| `relation-expression-rhs` | (new) | Expression-RHS property (`amount: $src.QTY * $src.PRICE`) + a `->toOne()` coercion, asserting computed values. Guards the composer + validator interplay. | TEST_ONLY |
| `relation-join` | `relational-joins` | Relation function with an in-body join **and** an XStore/ModelJoin association to a second relation-mapped class. | TEST_ONLY |
| `relation-enumeration` | `relational-enumeration` | `EnumerationMapping` transformer on a relation property; testSuite asserts decoded enum values. | TEST_ONLY |
| `relation-filter` | `relational-filter` | `->filter()` in the function body; testSuite asserts excluded rows never surface. | TEST_ONLY |
| `relation-groupBy` | (new) | `->groupBy()` in the function body with aggregates; testSuite over the aggregated class. | TEST_ONLY |
| `relation-service` | `relational-service` | A `Service` (`PureSingleExecution`) over a relation mapping — exercises both TEST_EXECUTION **and** PLAN_GENERATION phases. | TEST_ONLY |
| `relation-union` | (new) | `Operation { union(…) }` over two relation sets; testSuite confirms rows from both legs. | TEST_ONLY |
| `relation-relational-union` | (new) | Mixed union: one `Relation` set + one classic `Relational` set under one union operation. | TEST_ONLY |
| `relation-embedded` | (new) | Embedded (`address( … )`) and inline-embedded relation property mapping. | TEST_ONLY |
| `relation-milestoning` | (new) | A `<<temporal.*>>` class backed by a milestoned relation function; testSuite with a business/processing-date query. | TEST_ONLY |

### Explicitly out of EMIT scope (for now)

- **Advanced union / sub-aggregation-in-union** — blocked at the unit layer (Section 5);
  no EMIT until the engine work lands.
- **Subtype/inheritance and `extends`** — `NEW_DEV`; no passing pipeline yet.
- **Graph fetch, `allVersions()`, model chain** — `UNCERTAIN`; add EMIT only after the
  probe tests (G-6..G-8) classify them as `TEST_ONLY`.
- **Lineage assertions** — EMIT does not assert lineage; that remains an MFT concern and
  MFT is not adopted for relation mappings.

---

## Section 7 — Recommended Order

1. **Author-now unit gaps (`TEST_ONLY`):** G-1 (boolean) → G-4 (`->toOne()` coercion) →
   G-3 (`~src` breadth) → G-2 (local props) → G-5 (Binding execution).
2. **Probe (`UNCERTAIN`):** G-6 (`allVersions()`), G-7 (graph fetch), G-8 (model chain) —
   minimal `<<test.Test>>` functions to reclassify each as `TEST_ONLY` or `NEW_DEV`.
3. **EMIT models (`TEST_ONLY`):** `relation-simple` → `relation-service` → `relation-join`
   → `relation-enumeration` → `relation-filter` → `relation-groupBy` → `relation-union`
   → `relation-relational-union` → `relation-embedded` → `relation-src` /
   `relation-expression-rhs` → `relation-milestoning`.
4. **Engine work (`NEW_DEV`), unblocking the 19 `ToFix` + subtype + `extends`:** advanced
   union traversal → union `importDataFlow`/`U_TYPE` → sub-aggregation SQL → ModelJoin
   remainders → subtype dispatch → `extends` second-pass.

---

## Key File Reference

| File | Role |
|:---|:---|
| `.../mapping/relation/tests.pure` | Main relation `<<test.Test>>` suite (56) |
| `.../mapping/relation/pkInferenceTests.pure` | PK inference (43) |
| `.../mapping/relation/aggregation/testRelationFunctionAggregation.pure` | Sub-aggregation (9, 2 ToFix) |
| `.../mapping/relation/coverage/relationCoverageStore.pure` | Combined `NOT NULL` store + `Trade`/`PositionStats` classes + data loader |
| `.../mapping/relation/coverage/relationCoverage{Distinct,Filter,GroupBy,Dates}.pure` | distinct/filter/groupBy/date coverage (39) |
| `.../mapping/union/relation/testRelationUnion{,Advanced}.pure` | Union (15, 1 ToFix) + advanced (12 ToFix) |
| `.../mapping/modelJoin/testModelJoin{Simple,Advanced,Milestoning,Union}.pure` | ModelJoin associations (48, 4 ToFix) |
| `TestRelationFunctionMappingCompilation.java` | #4941 compiler suite (36) |
| `TestMappingCompilationFromGrammar.java` / `TestRelationalCompilationFromGrammar.java` | Compiler checks |
| `TestMappingGrammarParser.java` / `TestMappingGrammarRoundtrip.java` | Parse / round-trip |
| `legend-engine-xt-relationalStore-emit/src/test/resources/emit-models/` | EMIT models (6 relational, 0 relation) |
