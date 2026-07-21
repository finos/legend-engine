# Relation Mapping Test Gap Analysis

Audit of test coverage for **relation (function) class mappings** — the mapping form
`ClassName: Relation { ~func f():Relation<Any>[1] | ~src <expr>; col: COL }` — across
the three unit-test layers plus the EMIT integration layer.

This document reflects the codebase **after PR #4941 ("Relation Function Class Mapping
improvements")** and the **`relation/coverage` test cycle** (boolean / local-property /
`~src` / expression / distinct / filter / groupBy / dates / allVersions / include /
subtype). It supersedes the earlier gap analysis, which predated #4941.

Development status for each gap is one of:

| Status | Meaning |
|:---|:---|
| `DONE` | Covered and passing today |
| `TEST_ONLY` | Engine feature works; only test authoring is needed |
| `NEW_DEV` | Engine change required before a test can pass |
| `UNCERTAIN` | Needs a probe test to classify as `TEST_ONLY` or `NEW_DEV` |

### Run status (latest cycle)

- **Fixed:** `testStrictDateProject` — was a test-side `#TDS` date-literal format bug, not
  an engine issue (see [gotchas](#test-authoring-gotchas)).
- **Marked `ToFix`:** `testSubtypeSuperclassQuery` — relation-mapping polymorphic dispatch
  is unimplemented (matrix #26).
- **Deleted (feature not available):** the Binding-transformer and `extends` coverage
  tests — those mapping features are not yet supported (matrix #10, #27). Reclassified
  from `TEST_ONLY` to `NEW_DEV`.
- **Regressions surfaced in the shared suite** (not part of this cycle's authoring):
  `testEmbeddedRelationMappingWithAssociation`, `testEnumerationMappingProjection` — see
  [Section 5](#section-5--known-tofix-tests-new_dev).

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
| `mapping/relation/tests.pure` | 56 | 0¹ | Simple, association, pre-filter, embedded, inline-embedded, enumeration, groupBy, window column, milestoning (all temporal types + `allVersionsInRange`), mixed Relation+Relational, `~src`, expression-RHS |
| `mapping/relation/pkInferenceTests.pure` | 43 | 0 | PK inference (bare accessor, through-join, explicit) |
| `mapping/relation/aggregation/testRelationFunctionAggregation.pure` | 9 | 2 | Sub-aggregation via collection navigation |
| `mapping/relation/relationCoverageDistinct.pure` | 14 | 0 | `->distinct()` in function body |
| `mapping/relation/relationCoverageFilter.pure` | 3 | 0 | `->filter()` in function body, incl. join-in-filter |
| `mapping/relation/relationCoverageGroupBy.pure` | 12 | 0 | `->groupBy()` in function body, multi-aggregate |
| `mapping/relation/relationCoverageDates.pure` | 10 | 0 | `StrictDate` / `DateTime` typed properties |
| `mapping/relation/relationCoverageBoolean.pure` | 3 | 0 | `Boolean[1]` property from a `BIT NOT NULL` column |
| `mapping/relation/relationCoverageLocalProperty.pure` | 2 | 0 | `+prop` (plain + computed) query & filter |
| `mapping/relation/relationCoverageSrc.pure` | 3 | 0 | `~src` inline source (plain / filter / subset) |
| `mapping/relation/relationCoverageExpression.pure` | 3 | 0 | Expression RHS: arithmetic, concat, `->toOne()` coercion |
| `mapping/relation/relationCoverageAllVersions.pure` | 2 | 0 | Unbounded `allVersions()` + processing-date `all(d)` |
| `mapping/relation/relationCoverageInclude.pure` | 2 | 0 | Mapping `include` executed end-to-end |
| `mapping/relation/relationCoverageSubtype.pure` | 1 | 1 | Subtype: direct-subclass query (pass) + superclass dispatch (ToFix) |
| `mapping/union/relation/testRelationUnion.pure` | 15 | 1 | Union of Relation sets, mixed Relation+Relational union |
| `mapping/union/relation/testRelationUnionAdvanced.pure` | 12 | 12 | Advanced union model-join traversal (all blocked) |
| `mapping/modelJoin/testModelJoinSimple.pure` | 10 | 0 | ModelJoin (relation-set association) — simple |
| `mapping/modelJoin/testModelJoinAdvanced.pure` | 23 | 1 | ModelJoin — nested, sub-filter, sub-aggregation, qualified props |
| `mapping/modelJoin/testModelJoinMilestoning.pure` | 9 | 1 | ModelJoin across milestoned relation sets |
| `mapping/modelJoin/testModelJoinUnion.pure` | 6 | 2 | ModelJoin over union of relation sets |

¹ `tests.pure` has 0 `ToFix` stereotypes but **2 tests are currently failing as shared-suite
regressions** (Section 5).

**Totals:** ~84 Java + ~238 Pure relation-mapping test functions. **20 Pure tests are
`<<test.ToFix>>`** (engine work required — see [Section 5](#section-5--known-tofix-tests-new_dev)),
plus the 2 regressions above.

The `relation/relationCoverage*.pure` files live in the same package as the main tests
and share a single `<<test.BeforePackage>>` setUp. Their backing store (`coverageDB`) is
defined in `relationMappingSetup.pure` alongside `testDB`. Every `[1]` property is backed
by a `NOT NULL` column (required by the #4941 multiplicity validator). The Binding-transformer
and `extends` coverage files were **removed** this cycle — those features are not yet available.

### EMIT (integration)

**Zero** relation-mapping EMIT models exist. Six classic `Relational` models plus two
shared-fixture models exist as templates. See [Section 6](#section-6--emit-integration-tests).

---

## Section 2 — What #4941 Changed

#4941 is the reference point for this audit. Its test-relevant changes:

1. **Grammar — two new source forms.**
   - `~src <expression>` as an alternative to `~func <descriptor>`: an inline zero-arg
     Pure expression evaluating to a `Relation`. (Note: the runtime parser stack-overflows
     if a raw `#>{store.table}#` literal appears directly under `~src`; wrap the expression
     in a zero-arg helper function invoked from `~src`.)
   - Property RHS may now be a **full Pure expression over `$src`**, not only a bare
     column (`amount: $src.QTY * $src.PRICE`). Protocol gained
     `RelationFunctionClassMapping.sourceLambda` and `RelationFunctionPropertyMapping.valueFn`.

2. **Multiplicity-subsumption validator (most impactful for test authoring).**
   A relation property mapping fails to compile with
   `"Multiplicity Error: The property 'X' has a multiplicity range of [1] when the given
   expression has a multiplicity range of [0..1]"` when the property multiplicity does not
   subsume the mapped column/expression multiplicity. A **nullable** column infers as
   `[0..1]`, so **a `[1]` property must be backed by a `NOT NULL` column** (or a
   `->toOne()` expression). Type subsumption is also validated (primitive subtype allowed,
   supertype rejected; `Boolean`≠`String`).

3. **New dedicated compiler suite** `TestRelationFunctionMappingCompilation` (36 tests).

4. **Previously-`ToFix` tests promoted to passing** in `tests.pure`, incl.
   `testMixedTemporalMappingWithAllVersionsInRange1`, `testMixedMappingWithFilterInProject`,
   `testMixedCaseMappingThroughAssociationChain`.

5. **Union / ModelJoin routing improvements** unblocked most basic union and simple/advanced
   ModelJoin tests. Advanced union traversal remains blocked ([Section 5](#section-5--known-tofix-tests-new_dev)).

### Test-authoring gotchas

- **Multiplicity:** `[1]` property ⇐ `NOT NULL` column, or `->toOne()` in an expression RHS.
- **Date columns via `TestTDS` (both `#TDS` literals and projected `StrictDate` results):**
  the `TestTDS` parser (`Parsers.MINIMAL`) only recognises full datetimes
  (`YYYY-MM-DDT00:00:00.000+0000`); a **date-only** `YYYY-MM-DD` value is inferred as `STRING`
  and, against a `Date`/`StrictDate` column, fails with `"Not supported data type :'STRING'
  for Pure type: 'StrictDate'"`. This bites twice:
  (a) in a typed `#TDS` literal — use the full-datetime value format; and
  (b) when **projecting a `StrictDate` (DATE) property** from a relation mapping — the
  materialised value is date-only and hits the same round-trip. Project `->toString()`
  (String column) to assert date-only values. `DateTime`/`TIMESTAMP` columns carry a time
  component and project directly.
- **`~src`:** wrap the source relation expression in a helper function (parser limitation
  above).
- **Boolean columns:** a relation accessor types a `BIT`/`BOOLEAN` SQL column as `TinyInt`,
  not `Boolean` (`databaseHelperFunctions.pure` maps both to the `Bit` datatype, which the
  relation type system surfaces as `TinyInt`). A bare `active: ACTIVE` mapping to a
  `Boolean[1]` property therefore fails with `"Mismatching property and relation expression
  types … Boolean … TinyInt"`. Populate the property via an expression RHS coercion
  (`active: $src.ACTIVE == 1`).

---

## Section 3 — Feature Coverage Matrix

Reference base: classic `Relational` tests under `.../relational/tests/mapping/`.

| # | Feature | Relational ref | Relation `<<test.Test>>` | Java | Status |
|:--|:---|:---:|:---:|:---:|:---:|
| 1 | Simple property mapping | Y | Y | Y | DONE |
| 2 | Association / XStore | Y | Y | Y | DONE |
| 3 | ModelJoin association | (rel-only) | Y (48) | Y | DONE |
| 4 | Embedded mapping | Y | Y | Y | DONE |
| 5 | Inline embedded (`() Inline`) | Y | Y | Y | DONE |
| 6 | Class-level filter (in `~func` body) | Y | Y (coverage) | — | DONE |
| 7 | GroupBy (in `~func` body) | Y | Y (coverage) | — | DONE |
| 8 | Distinct (in `~func` body) | Y | Y (coverage) | — | DONE |
| 9 | Enumeration mapping | Y | Y (8+) | Y | DONE |
| 10 | Binding transformer | Y | N (deleted) | Y (compile) | **NEW_DEV** |
| 11 | Local properties (`+prop`) | Y | Y (coverage) | Y | DONE |
| 12 | `~src` inline source | n/a | Y (5) | Y | DONE |
| 13 | Expression RHS (`$src.x + …`) | n/a | Y (6) | Y | DONE |
| 14 | Union (Relation + Relation) | Y | Y (14) + advanced ToFix | Y | MIXED |
| 15 | Mixed union (Relation + Relational) | Y | Y (basic) + ToFix | Y | MIXED |
| 16 | Mapping include (execution) | Y | Y (coverage) | Y | DONE |
| 17 | Milestoning — processing/business/bi-temporal | Y | Y | — | DONE |
| 18 | Milestoning — `allVersionsInRange` | Y | Y | — | DONE |
| 19 | Milestoning — `allVersions()` (unbounded) | Y | Y (coverage) | — | DONE |
| 20 | Window functions | N | Y (1) | — | DONE |
| 21 | Primary key inference / explicit PK | (implicit) | Y (43) | Y | DONE |
| 22 | Sub-aggregation (collection agg) | N | Y (7) + 2 ToFix | — | MIXED |
| 23 | Date / DateTime typed properties | Y | Y (coverage) | Y | DONE |
| 24 | Boolean typed properties | Y | Y (coverage, via `$src.col == 1` coercion) | — | DONE |
| 25 | Computed column (SQL func / concat) | Y | Y (coverage + `testRfpmLiftMixedConcat`) | Y | DONE |
| 26 | Subtype / inheritance | Y | Direct: Y · Superclass: ToFix | — | MIXED (NEW_DEV) |
| 27 | Mapping extension (`extends [setId]`) | Y | N (deleted) | Y (roundtrip) | **NEW_DEV** |
| 28 | Union `importDataFlow` / PK discriminator | Y | 1 ToFix | — | **NEW_DEV** |
| 29 | Graph fetch (`->graphFetch()/serialize()`) | Y | N (only classic-embedded mapping tests use `graphFetch`; zero occurrences under `mapping/relation/`) | — | **UNCERTAIN** |
| 30 | Model chain / store substitution | Y | Partial (include only) | — | UNCERTAIN |
| 31 | Service + plan generation | Y (EMIT) | N | — | TEST_ONLY (EMIT) |
| 32 | testSuites / Testables ("Run Tests") | Y | N (SPI stub) | (compile) | **NEW_DEV** |
| 33 | Variant / semi-structured RFPM-lift extraction (`get()->to()/->toMany()`) | Y (classic `extractFromSemiStructured`) | N (removed) | — | **NEW_DEV** |

---

## Section 4 — Remaining Unit-Test Gaps

Most author-now gaps from the previous cycle are now closed (matrix #11, #12, #13, #16,
#19, #23, #24, #25, #29). What remains:

### G-A — Store substitution / model chain — `UNCERTAIN` (matrix #30)
Mapping `include` now executes (`relationCoverageInclude`), but a store-substitution model
chain (child mapping substituting the store of an included relation set) is untested. Probe
`testRelationMappingStoreSubstitution`.

### G-B — Service + plan generation — `TEST_ONLY` (EMIT) (matrix #31)
Best covered at the EMIT layer (`relation-service`, Section 6) rather than as a Pure unit
test.

### G-C — `~src` / expression RHS — optional breadth — `TEST_ONLY`
Current coverage is representative. Optional additions: `~src` backing a union leg;
expression RHS combined with an enumeration transform or inside a groupBy mapping.

Author-now gaps are otherwise exhausted; the substantive remaining work is engine-side
([Section 5](#section-5--known-tofix-tests-new_dev)).

---

## Section 5 — Known `ToFix` Tests and Regressions (`NEW_DEV`)

### Written-but-blocked tests (`<<test.ToFix>>`) — 20 total

| Area | Count | Root cause |
|:---|:---:|:---|
| `testRelationUnionAdvanced.pure` | 12 | Advanced union model-join traversal: `RelationFunction cannot be cast to NamedRelation` (filter-from-child), empty-collection cast (reverse traversal), `TdsSelectSqlQuery cannot be cast to NamedRelation` (mixed rfsi+Relational), H2 GROUP BY column quoting for aggregated rfsi legs |
| `testRelationUnion.pure` | 1 | `testUnionTwoRelationMappings_PksWithImportDataFlow` — `U_TYPE` discriminator column not surfaced by rfsi subqueries for `importDataFlow=true` PK propagation |
| `testRelationFunctionAggregation.pure` | 2 | Sub-aggregation SQL generation edge cases |
| `testModelJoinAdvanced.pure` | 1 | Remaining advanced ModelJoin traversal case |
| `testModelJoinMilestoning.pure` | 1 | Milestoned ModelJoin self-join isolation |
| `testModelJoinUnion.pure` | 2 | ModelJoin over union legs |
| `relationCoverageSubtype.pure` | 1 | `testSubtypeSuperclassQuery` — superclass query fails `"Error mapping not found for class Person"` (`router_routing.pure:584`); polymorphic dispatch over `RelationFunctionInstanceSetImplementation` is unimplemented |

### Features not yet available (tests removed / not authored)

- **Binding transformer** (matrix #10): the `col: Binding <binding> : COL` execution path is
  not available for relation mappings; the coverage test was removed. Grammar/compile
  round-trips exist (`testValidRelationFunctionMappingWithBinding`).
- **Mapping extension `extends`** (matrix #27): `RelationFunctionClassMappingSecondPassCompiler`
  extend-chain execution is unimplemented; the coverage test was removed. Grammar round-trips
  (`testRelationFunctionMappingWithExtends`).
- **Subtype / inheritance** (matrix #26): direct-subclass query works; superclass
  polymorphic dispatch does not (`findMappingForType` over `RelationFunctionInstanceSetImplementation`).
- **Testables SPI** (matrix #32): `RelationAccessorTestConnectionFactory` is a stub.
- **Variant / semi-structured RFPM-lift extraction** (matrix #33): property bodies like
  `$src.PAYLOAD->get(..)->to(@T)` / `->toMany(@T)` lower to the `variantTo` DynaFunction
  (`pureToSQLQuery_variant.pure`), which has **no converter** in the `sqlDialectTranslation`
  model path (`toPostgresModel.pure::getDynaFunctionConverterMap`) that these
  `<<test.AlloyOnly>>` relation-mapping tests execute through — so execution fails with
  *"Couldn't find DynaFunction to Postgres model translation for variantTo()."* The
  `testRfpmLiftPrimitive`, `testRfpmLiftClass`, and `testRfpmLiftClassToMany` tests were
  removed. `testRfpmLiftExplicitMap` (`->toMany(@PayloadFirm)->map(f|$f.tags)`) additionally
  failed earlier at **routing** — the router looks for a class mapping for the unmapped
  `PayloadFirm` (`helperFunctions.pure`) — and was removed too, along with the now-orphaned
  `PayloadFirm` / `PersonLifted*` classes and `Lifted*Mapping` mappings. Still present: the
  non-variant lift path (`testRfpmLiftMixedConcat`, matrix #25) and the type-rejection case
  (`testRfpmLiftUnsupportedStructuralContainer`, `<<test.ToFix>>`). Note the *classic* way to
  read a `SEMISTRUCTURED` column — `extractFromSemiStructured(..)`, wired into both the SQL
  string and model paths — **is** supported and covered by `meta::relational::tests::semistructured`.
  Re-add these tests once `variantTo` (and map-over-extracted-class routing) are supported in
  the model path.

### Shared-suite regressions surfaced (not authored this cycle)

Both live in `tests.pure` (added by #4814), are `<<test.Test>>`, and now fail. They are
**not** caused by the `coverage` work (which touches only `coverage/`); `tests.pure` was
last modified by #4941, and #4932 rewrote same-connection XStore associations as ModelJoins.

| Test | Symptom | Likely cause |
|:---|:---|:---|
| `testEmbeddedRelationMappingWithAssociation` | `firm` resolves to `TDSNull` for cross-firm rows (David→Firm B, Fabrice→Firm C) | Association/join-resolution regression, consistent with the #4932 XStore→ModelJoin rewrite |
| `testEnumerationMappingProjection` | `Column "persontable_1.FIRST NAME" not found` | SQL-gen aliasing bug for a quoted column-with-space in the nested subquery of an enumeration+filter relation mapping |

These need engine fixes (owned by the causing change), not `ToFix` masking — flagged here
for tracking.

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
| `relation-src` | (new) | `~src <expr>` inline source form through the full pipeline (grammar composer + compile + execute). | TEST_ONLY |
| `relation-expression-rhs` | (new) | Expression-RHS property (`amount: $src.QTY * $src.PRICE`) + a `->toOne()` coercion, asserting computed values. | TEST_ONLY |
| `relation-join` | `relational-joins` | Relation function with an in-body join **and** an XStore/ModelJoin association to a second relation-mapped class. | TEST_ONLY |
| `relation-enumeration` | `relational-enumeration` | `EnumerationMapping` transformer on a relation property; testSuite asserts decoded enum values. | TEST_ONLY |
| `relation-filter` | `relational-filter` | `->filter()` in the function body; testSuite asserts excluded rows never surface. | TEST_ONLY |
| `relation-groupBy` | (new) | `->groupBy()` in the function body with aggregates; testSuite over the aggregated class. | TEST_ONLY |
| `relation-service` | `relational-service` | A `Service` (`PureSingleExecution`) over a relation mapping — exercises TEST_EXECUTION **and** PLAN_GENERATION. | TEST_ONLY |
| `relation-union` | (new) | `Operation { union(…) }` over two relation sets; testSuite confirms rows from both legs. | TEST_ONLY |
| `relation-relational-union` | (new) | Mixed union: one `Relation` set + one classic `Relational` set under one union operation. | TEST_ONLY |
| `relation-embedded` | (new) | Embedded (`address( … )`) and inline-embedded relation property mapping. | TEST_ONLY |
| `relation-milestoning` | (new) | A `<<temporal.*>>` class backed by a milestoned relation function; testSuite with a business/processing-date query. | TEST_ONLY |

### Explicitly out of EMIT scope (for now)

- **Advanced union / sub-aggregation-in-union** — blocked at the unit layer (Section 5).
- **Binding, `extends`, subtype-superclass dispatch** — `NEW_DEV`; no passing pipeline yet.
- **Store substitution / model chain** — `UNCERTAIN`; add EMIT only after the probe test
  (G-A) classifies it as `TEST_ONLY`.
- **Lineage assertions** — EMIT does not assert lineage; that remains an MFT concern and
  MFT is not adopted for relation mappings.

---

## Section 7 — Recommended Order

1. **EMIT models (`TEST_ONLY`, highest remaining value):** `relation-simple` →
   `relation-service` → `relation-join` → `relation-enumeration` → `relation-filter` →
   `relation-groupBy` → `relation-union` → `relation-relational-union` → `relation-embedded`
   → `relation-src` / `relation-expression-rhs` → `relation-milestoning`.
2. **Probe (`UNCERTAIN`):** G-A (store substitution / model chain).
3. **Engine work (`NEW_DEV`), unblocking the 20 `ToFix` + subtype/binding/extends and the 2
   regressions:** advanced union traversal → union `importDataFlow`/`U_TYPE` →
   sub-aggregation SQL → ModelJoin remainders → subtype-superclass dispatch → `extends`
   second-pass → Binding execution → (regressions) #4932 XStore→ModelJoin association
   resolution and the quoted-column SQL-gen aliasing bug.

---

## Key File Reference

| File | Role |
|:---|:---|
| `.../mapping/relation/tests.pure` | Main relation `<<test.Test>>` suite (56; 2 regressions) |
| `.../mapping/relation/pkInferenceTests.pure` | PK inference (43) |
| `.../mapping/relation/aggregation/testRelationFunctionAggregation.pure` | Sub-aggregation (9, 2 ToFix) |
| `.../mapping/relation/relationMappingSetup.pure` | `testDB` + `coverageDB` schemas, all class/enum definitions, merged `setUp()` |
| `.../mapping/relation/relationCoverage{Distinct,Filter,GroupBy,Dates,Boolean,LocalProperty,Src,Expression,AllVersions,Include,Subtype}.pure` | Coverage suites (55 test + 1 ToFix), same package as main tests |
| `.../mapping/union/relation/testRelationUnion{,Advanced}.pure` | Union (15, 1 ToFix) + advanced (12 ToFix) |
| `.../mapping/modelJoin/testModelJoin{Simple,Advanced,Milestoning,Union}.pure` | ModelJoin associations (48, 4 ToFix) |
| `TestRelationFunctionMappingCompilation.java` | #4941 compiler suite (36) |
| `TestMappingCompilationFromGrammar.java` / `TestRelationalCompilationFromGrammar.java` | Compiler checks |
| `TestMappingGrammarParser.java` / `TestMappingGrammarRoundtrip.java` | Parse / round-trip |
| `legend-engine-xt-relationalStore-emit/src/test/resources/emit-models/` | EMIT models (6 relational, 0 relation) |
