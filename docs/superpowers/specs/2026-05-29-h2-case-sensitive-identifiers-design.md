# H2 case-sensitive schema and table identifiers

Status: Draft — awaiting review
Author: Aziem Chawdhary
Date: 2026-05-29

## Problem

When a Pure `Database` declares two schemas whose names differ only in case
(for example `prime` and `Prime`), or two tables in different schemas where
the qualified names collide only after case folding (`prime.synonym` vs
`Prime.synonym`), the legend-engine H2 store treats them as the same object.
DDL generated for the second schema or table overwrites the first, and
mappings that resolve to a column on one schema can return data shaped by
the other.

The root cause sits in two places:

1. H2's JDBC defaults. The shared connection string carries `MODE=LEGACY`,
   under which `DATABASE_TO_UPPER` is `TRUE`. H2 folds unquoted identifiers
   to upper case for both storage and lookup, so the two schemas collapse to
   one.
2. The H2 SQL dialect itself. Most of the dialect routes identifier emission
   through `dbConfig.identifierProcessor`, which respects per-connection
   quoting. Two functions do not: `translateCreateTableStatementH2` in
   `h2Extension2_1_214.pure` (non-temp `CREATE TABLE`) and the default
   `loadValuesToDbTableDefault` in `extensionDefaults.pure` that the H2
   dialect falls back to for in-line `INSERT … VALUES`. Both concatenate
   `$t.schema.name + '.' + $t.name` as raw strings. The H2 v1.4 dialect
   inherits the second bypass via its delegation to the default.

The asymmetry — `DROP TABLE` and SELECT path quote consistently, but
`CREATE TABLE` and `INSERT VALUES` emit raw identifiers — means even setting
`quoteIdentifiers=true` on a connection today does not fully solve the
problem.

## Background — commit 0b17d9d2 and downstream cost

The earlier commit `0b17d9d29681c8567352b4b30ceacf6480a55ae2`
("Execute DDL statements only for schemas and tables provided by test data
CSV", #4723) introduced a workaround for the same root cause.

Before that commit, `setUpDataSQLs` emitted DDL for every schema and table
reachable in the `Database` definition. After it, DDL emission is filtered
to the `(schemaName, tableName)` pairs actually present in the test data
CSV. The commit also added a regression test
`testRelationalServiceWithCaseSensitiveSchemaNames`
(`TestServiceTestSuite.java`) plus two resource files defining a database
with `Schema Schema1` and `Schema schema1`, each containing a `product_table`
with different columns.

The workaround sidesteps the H2 case collision: when test data references
only `Schema1.product_table`, no DDL is emitted for `schema1.product_table`,
so the second `CREATE TABLE` never runs and never overwrites the first.

The cost is real and is the reason for this work. Downstream tests that
legitimately query an *empty* table — a table that has no rows in the test
CSV but is reachable in the model — now never see the table created. Their
queries fail with "table not found" instead of returning an empty result.
Once H2 properly handles case-sensitive identifiers, the workaround
becomes unnecessary, and reverting it restores the intended
"create DDL for every reachable table" semantics without reintroducing
the case-collision bug.

## Goals

- Make Pure-generated DDL and DML against H2 distinguish identifiers that
  differ only in case (`prime` vs `Prime`, `synonym` vs `Synonym`).
- Make `testRelationalServiceWithCaseSensitiveSchemaNames` pass after
  reverting commit 0b17d9d2, with no change to the test's
  `model::CaseSensitiveConnection` (which does not set
  `quoteIdentifiers=true`).
- Restore "DDL for every reachable schema and table" by reverting
  commit 0b17d9d2 once the H2 fix is in.
- Preserve a defined escape hatch so a test or deployment can opt back into
  the old H2 case-folding behaviour if needed.

## Non-goals

- Changing how dialects other than H2 emit identifiers.
- Changing the per-connection `quoteIdentifiers` protocol field. Tests and
  models that explicitly set or rely on it keep working unchanged.
- Modifying the H2 v1 (1.4.200) branch beyond what is necessary to keep the
  shared DDL helper consistent.
- Touching the persistence-component H2 test harness
  (`legend-engine-xt-persistence-component-relational-h2`), which constructs
  its own JDBC URL with bespoke options.

## Approach B (primary) — always-quote in the H2 dialect, plus DDL bypass fix

### Mechanism

Quote every schema, table and column identifier the H2 SQL dialect emits,
regardless of the connection's `quoteIdentifiers` setting. With H2's
quoted-identifier semantics, quoted names are case-preserving and
case-sensitive irrespective of `DATABASE_TO_UPPER`, so
`"prime"."synonym"` and `"Prime"."synonym"` become distinct objects without
touching the JDBC URL.

For this to work end-to-end, every DDL and DML emitter in the H2 dialect
must route schema and table names through `dbConfig.identifierProcessor`
(or the existing `tableToString` helper, which dispatches through it).
Today two paths do not, and they happen to be the paths that the new
regression test exercises: `CREATE TABLE` and `INSERT … VALUES`.

### Changes

1. Introduce an always-quote identifier processor in the H2 dialect. The
   simplest form is a new function under the H2 dialect namespace, e.g.
   `processIdentifierWithDoubleQuotesAlways(identifier, dbConfig)`, that
   strips any embedded `"` characters from the identifier and wraps it in
   `"…"` unconditionally. It deliberately bypasses the
   `dbConfig.quoteIdentifiers || isReserved || hasSpace`
   conditional in
   `processIdentifierWithQuoteChar` (`extensionDefaults.pure:554`).
2. Wire the new processor into both H2 dialect versions:
   - `h2Extension2_1_214.pure:39` — `identifierProcessor = …WithDoubleQuotesAlways`.
   - `h2Extension1_4_200.pure:35` — same.
3. Fix the DDL/DML bypasses so they route through the identifier processor:
   - `translateCreateTableStatementH2` in `h2Extension2_1_214.pure:84-95`.
     Replace the literal `$t.schema.name + '.' + $t.name` construction with
     `$t->tableToString($dbConfig)`. `tableToString` already handles the
     `default`-schema short-circuit (`dbExtension.pure:565-574`).
   - The default `loadValuesToDbTableDefault` in
     `extensionDefaults.pure:630-643`. Replace
     `$loadTableSQL.table.schema.name + '.' + $loadTableSQL.table.name`
     with `$loadTableSQL.table->tableToString($dbConfig)`. This fixes the
     H2 v1.4 path, which delegates to this default, and any other dialect
     that did the same.
   - `loadValuesToDbTableH2` in `h2Extension2_1_214.pure:105-116`. Same
     replacement as the default.
4. Audit the H2 dialect for any further hand-written identifier
   concatenation (`$t.name`, `$schema.name`) that bypasses the processor.
   The expected outcome of the audit is: no further bypasses.

### Why an H2-only always-quote processor rather than forcing
`quoteIdentifiers=true` on the connection

The protocol-level `quoteIdentifiers` flag is user-facing and applies to
the whole connection. Silently overriding it to `true` for H2 would surprise
a user who deliberately set it to `false`. Defining the always-quote policy
at the dialect level keeps the per-connection flag faithful to its
documented meaning everywhere else, and confines the H2-specific behaviour
to the H2 dialect file. The semantic awkwardness — the H2 dialect ignores
the per-connection request to not quote — is documented but contained.

### Escape hatch

To opt out of always-quote for a specific connection, the user can switch
to Option 3 below (DDL bypass fix only) plus `quoteIdentifiers=true` on the
connection. Approach B itself does not introduce a connection-level
override flag, by design — the whole point of B is that the dialect makes
the choice uniformly.

### Test impact

- Golden-string DDL and SQL assertions for H2 will need updating, because
  emitted identifiers now contain `"…"`. Affected suites include
  `testDDL.pure`, the PCT golden-SQL fixtures for H2, and any
  `assertEquals(expected, generatedSql)` checks targeting the H2 dialect.
- Tests that build raw H2 schemas via JDBC (outside the Pure SQL path) are
  unaffected by Approach B alone. If such a test needs case-sensitive
  schemas, it must either quote in the raw DDL or combine with Approach A.

## Approach A (option) — flip H2 JDBC defaults

### Mechanism

Append `;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=FALSE` to the
default H2 JDBC connection string. `DATABASE_TO_UPPER=FALSE` preserves the
case of unquoted identifiers; `CASE_INSENSITIVE_IDENTIFIERS=FALSE` is
required because the default for that flag flips to `TRUE` once
`DATABASE_TO_UPPER` is `FALSE`. Both flags together yield case-preserving,
case-sensitive identifier handling at the JDBC layer, regardless of
whether emitted SQL is quoted.

### Changes

1. Add a public constant
   `CASE_SENSITIVE_IDENTIFIERS_PROPERTIES = ";DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=FALSE"`
   in `H2Defaults` (`legend-engine-xt-relationalStore-h2-execution-2.1.214`)
   so the value is defined once.
2. Append it inside `H2Defaults.getDefaultH2Properties()` (v2 branch only).
3. Reuse the same constant from `H2Manager.buildURL`
   (`legend-engine-xt-relationalStore-executionPlan-connection`) in both
   the embedded/file-mode branch (line 57) and the in-memory branch
   (line 76). Have `H2Manager.buildURL` delegate to
   `H2Defaults.getDefaultH2Properties()` instead of duplicating the
   `MODE=LEGACY;NON_KEYWORDS=…` literal — this removes existing drift
   between the two files.
4. Update hardcoded test URLs that bypass `H2Manager` to either consume
   `H2Defaults.getDefaultH2Properties()` or append the new suffix:
   - `TestArrowNodeExecutor` (`legend-engine-xts-arrow`).
   - `TestRelationalResultToArrowIPCSerializer`
     (`legend-engine-xt-relationalStore-executionPlan`).
   - `PostgresServerGenericTest` (`legend-engine-xt-sql-postgres-server`)
     — already inlines a copy of the LEGACY default; switch to
     `H2Defaults.getDefaultH2Properties()` so it stays in sync.
5. Out of scope: the persistence-component H2 tests, which already use
   `DATABASE_TO_UPPER=false;mode=mysql;…` and are independent.

### Escape hatch

The existing `legend.test.h2.properties` system property continues to
override the full default string in both `H2Manager` and `H2Defaults`.
A CI job or a local debug session that needs the old behaviour can set
that property to the prior LEGACY string. This is documented as the
opt-out mechanism.

### Test impact

- Tests whose hand-written SQL relies on H2's auto-UPPER folding (for
  example, fixtures that `CREATE TABLE foo` then `SELECT * FROM FOO`) will
  break. The blast radius is potentially wide and uncertain until run on
  CI.
- Pure-generated SQL is unaffected in shape — only its runtime
  interpretation by H2 changes.
- The new regression test passes without any change to its connection.

## Option 3 (alternative) — DDL bypass fix only, no always-quote

### Mechanism

Apply only the DDL bypass fixes from Approach B — replace
`$t.schema.name + '.' + $t.name` with `$t->tableToString($dbConfig)` in
the H2 dialect's `translateCreateTableStatementH2`, the H2 dialect's
`loadValuesToDbTableH2`, and the default `loadValuesToDbTableDefault`.
Leave the H2 identifier processor as today: quotes only when
`dbConfig.quoteIdentifiers` is `true`, or for reserved words, or for
identifiers containing spaces.

The H2 dialect then becomes internally consistent — every DDL and DML
emitter routes through the same identifier processor — but case-sensitive
identifier handling still requires the connection to set
`quoteIdentifiers=true`.

### Trade-off

This is the smallest possible change and has the lowest blast radius.
It fixes a real, latent inconsistency in the H2 dialect. However, the
new regression test
`testRelationalServiceWithCaseSensitiveSchemaNames`, as defined in commit
0b17d9d2, does not set `quoteIdentifiers=true` on
`model::CaseSensitiveConnection`. Under Option 3 alone the test would not
pass after revert, and every future case-sensitive use case would carry
the same per-connection burden. Listed here as an alternative that the
project could fall back to if Approach B proves too costly in golden-SQL
churn, but it is not the recommended path.

## Revert and verification plan

This is the load-bearing part of the work and is the same regardless of
approach.

1. Land the chosen H2 fix (Approach B by default).
2. Run `testRelationalServiceWithCaseSensitiveSchemaNames`. On the current
   branch, with commit 0b17d9d2 still in place, this test passes because
   the workaround filters DDL by test data. It should still pass after
   the fix lands — neither approach should regress it.
3. Revert commit `0b17d9d29681c8567352b4b30ceacf6480a55ae2`. The revert
   restores `toDDL.pure`'s
   "emit DDL for every reachable schema and table" behaviour and restores
   the previously-removed expected DDL strings in `testDDL.pure`.
4. Re-run `testRelationalServiceWithCaseSensitiveSchemaNames`. This is the
   critical signal. The test now exercises the path the workaround used
   to hide: both `Schema Schema1` and `Schema schema1` get their DDL
   emitted. If the H2 fix is real, both schemas exist as distinct objects
   and the query returns the expected `name, type` data. If the fix is
   incomplete, the test fails the same way it would have on master before
   the workaround.
5. Re-run the suite at large to confirm nothing else regresses. For
   Approach B, expect to update golden DDL/SQL string fixtures (notably
   `testDDL.pure`) to reflect the new quoted output. For Approach A,
   expect breakage from fixtures that rely on auto-UPPER folding; triage
   on CI.
6. Commit the revert and any expectation updates together so the bisection
   story remains coherent: "H2 case fix" → "revert workaround + adjust
   expectations".

The new test
(`testRelationalServiceWithCaseSensitiveSchemaNames` plus the two
resource files
`legend-testable-relational-case-sensitive-schema-model.pure` and
`legend-testable-relational-service-case-sensitive-schema.pure`)
is the authoritative regression check for this work. It is the only test
in the repository that explicitly asserts that two schemas differing only
in case can coexist with different table shapes.

## Risks and open questions

- **Golden-SQL churn under Approach B.** The volume of expected-SQL
  fixtures that change once H2 emits quoted identifiers everywhere is
  unknown until CI runs. Some PCT modules snapshot generated SQL; those
  expectations need updating in the same change set as the dialect
  change.
- **Embedded vs in-memory consistency.** Approach B is independent of
  storage mode. Approach A applies uniformly to both embedded/file-mode
  and in-memory by design.
- **Interaction with `MODE=LEGACY`.** H2's `MODE=LEGACY` is preserved by
  both approaches. LEGACY does not change quoted-identifier semantics, so
  Approach B is unaffected. For Approach A, the two added flags override
  LEGACY's `DATABASE_TO_UPPER` default.
- **Future SQL extensions for H2 dialect.** Any new emitter added under
  Approach B must route through `tableToString` or
  `dbConfig.identifierProcessor` or it silently breaks the quoting
  guarantee. Worth a brief mention in the H2 dialect comments.
- **Persistence-component H2 tests.** Out of scope. If they later need
  the same treatment, a separate piece of work.

## Implementation notes for the plan

- The `tableToString` helper at `dbExtension.pure:565` already short-circuits
  the `default` schema name. The DDL bypass fix should rely on that rather
  than reproducing the `if($t.schema.name == 'default', |'', | …)`
  conditional in the two DDL functions.
- `processIdentifierWithQuoteChar` strips embedded `"` characters when
  quoting. The new always-quote processor under Approach B should do the
  same to preserve current behaviour for identifiers containing literal
  double quotes.
- Reserved-word handling is delegated to
  `dbConfig.isDbReservedIdentifier` today. The always-quote processor
  obsoletes the reserved-word check for the wrap decision, but the H2
  reserved-words list at `h2Extension1_4_200.pure:80-87` and its v2
  counterpart should be left untouched.
