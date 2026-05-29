# H2 Case-Sensitive Identifiers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Pure-generated DDL and DML against H2 distinguish identifiers
that differ only in case (e.g. `prime` vs `Prime`, `Schema1` vs `schema1`),
so that schemas and tables whose names differ only in case coexist as
distinct objects. Restore the original "emit DDL for every reachable schema
and table" behaviour by completing the revert of commit 0b17d9d2's
workaround.

**Architecture:** Apply Approach B from the spec
(`docs/superpowers/specs/2026-05-29-h2-case-sensitive-identifiers-design.md`):
introduce an always-quote identifier processor in the H2 dialect, wire it
into both H2 v1.4.200 and v2.1.214 dialect files, and fix three DDL/DML
emitters that today hand-concatenate raw schema and table names instead of
routing through `dbConfig.identifierProcessor`. Complete the partial revert
of commit 0b17d9d2 (already partly reverted by commit 076798f) so that
`testRelationalServiceWithCaseSensitiveSchemaNames` becomes the load-bearing
regression check for the fix.

**Tech Stack:** Pure language (`.pure` files), Java 11, JUnit 4 (existing
`TestServiceTestSuite`), Maven 3.6.2+.

---

## State of play at start

These facts about the repo as of branch `h2case` from `master @ 9fab7575859`
are load-bearing for this plan; verify them in Task 1 before proceeding.

- **Workaround commit** `0b17d9d29681c8567352b4b30ceacf6480a55ae2` (#4723)
  added DDL filtering by test data CSV.
- **Partial revert** `076798f922ddcae851b6adb8b39e666df1de6970` (#4764)
  reverted the workaround's effect on two of three `setUpDataSQLs`
  overloads and restored `testDDL.pure` expected DDL strings to the full
  pre-workaround list. It left:
  - `setUpDataSQLsV2` in `toDDL.pure` still calling
    `extractSchemaTablePairsFromCsv` and passing computed pairs.
  - The helper functions `extractSchemaTablePairsFromCsv` /
    `extractSchemaTablePairsFromRecords` in place.
  - `schemaAndTableSetup` in 3-arg form, with the empty-pairs
    short-circuit.
- The new regression test
  `testRelationalServiceWithCaseSensitiveSchemaNames` was updated to TDSv2
  in the same partial-revert commit and uses connection-block test data,
  which routes through `setUpDataSQLsV2` — so the test currently passes
  via the still-active workaround in V2.
- `testCrossStoreGraphFetch.pure` already asserts the pre-workaround
  `testDataSetupSqls->size() == 7`.

## File structure

Files modified by this plan (with one-line responsibility statement):

- `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/DDL/toDDL.pure` — complete the workaround revert; restore `schemaAndTableSetup` to 2-arg form; remove dead helpers.
- `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension.pure` — add the shared always-quote identifier processor for H2.
- `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension2_1_214.pure` — wire always-quote in v2.1.214; fix `translateCreateTableStatementH2` and `loadValuesToDbTableH2` bypasses.
- `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension1_4_200.pure` — wire always-quote in v1.4.200.
- `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/extensionDefaults.pure` — fix `loadValuesToDbTableDefault` bypass.
- `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/DDL/testDDL.pure` — update expected DDL strings to reflect quoted identifiers.

## Path-shortening conventions used below

To keep step listings readable, this plan uses these abbreviations. They
are not real shell variables; substitute when running commands.

- `$CORE_PURE` = `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure`
- `$SVC_TEST_RUNNER` = `legend-engine-xts-service/legend-engine-test-runner-service`

---

## Task 1: Verify baseline state

**Goal:** Confirm the state-of-play assumptions above before making changes.
If any check disagrees, stop and reconcile the plan before continuing.

**Files:**
- Read only — no changes.

- [ ] **Step 1: Confirm we are on the right branch**

Run:
```bash
git status
git log --oneline -3
```
Expected: branch `h2case`; latest commit is the design doc commit
("Design: H2 case-sensitive schema and table identifiers"); recent
history includes `9fab7575859` ("Fix databricks test expectations").

- [ ] **Step 2: Confirm both reference commits exist**

Run:
```bash
git log --oneline 0b17d9d2 076798f922d -- 'legend-engine-xts-relationalStore/**/DDL/toDDL.pure' | head -5
```
Expected: both commit SHAs resolve and touch `toDDL.pure`.

- [ ] **Step 3: Confirm `setUpDataSQLsV2` still uses the workaround**

Run:
```bash
grep -n "extractSchemaTablePairsFromCsv\|setUpDataSQLsV2" legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/DDL/toDDL.pure
```
Expected: `setUpDataSQLsV2` body still contains `extractSchemaTablePairsFromCsv`
and passes `$pairs`. The other two `setUpDataSQLs` overloads pass `[]`.

- [ ] **Step 4: Confirm the regression test exists and is enabled**

Run:
```bash
grep -B1 -A4 "testRelationalServiceWithCaseSensitiveSchemaNames" legend-engine-xts-service/legend-engine-test-runner-service/src/test/java/org/finos/legend/engine/testable/service/TestServiceTestSuite.java | head -10
```
Expected: `@Test` annotation (no `@Ignore`), method present.

- [ ] **Step 5: Confirm the two H2 DDL bypasses are still there**

Run:
```bash
grep -n "schema.name + '.'" legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension2_1_214.pure legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/extensionDefaults.pure
```
Expected: hits in both `h2Extension2_1_214.pure` (two: lines around 88 and
108) and `extensionDefaults.pure` (one: around line 635).

No commit at the end of this task — it is read-only verification.

---

## Task 2: Complete the workaround revert in `toDDL.pure`

**Goal:** Make `setUpDataSQLsV2` also pass `[]`, restore
`schemaAndTableSetup` to its original 2-arg signature, and delete the dead
helper functions. After this task the workaround code is gone and DDL is
emitted for every reachable schema/table on every code path.

**Files:**
- Modify: `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/DDL/toDDL.pure`

- [ ] **Step 1: Replace the three-arg `schemaAndTableSetup` with its
      original two-arg form**

In `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/DDL/toDDL.pure`,
replace the whole block currently spanning roughly lines 155–184 (the
function definition for `schemaAndTableSetup` that takes
`dataSchemaTablePairs`) with the original two-arg version:

```pure
function meta::alloy::service::execution::schemaAndTableSetup(db:Database[1], dbConfig:DbConfig[1]):String[*]
{
   let allSchemas = $db->allSchemas();

   let schemaSetup = $allSchemas->map({schema |
      meta::relational::functions::toDDL::dropSchemaStatement($schema.name, $dbConfig)->concatenate(
          meta::relational::functions::toDDL::createSchemaStatement($schema.name, $dbConfig))
   });

   let tableSetup = $allSchemas->map({schema |
      $schema.tables->map({t |
        dropTableStatement($db, $schema.name, $t.name, $dbConfig)->concatenate(
            createTableStatement($db, $schema.name, $t.name, $dbConfig))
      })
   });

   $schemaSetup->concatenate($tableSetup);
}
```

- [ ] **Step 2: Delete the two helper functions that compute the
      workaround pairs**

In the same file, delete these two function definitions (currently around
lines 134–153):

- `meta::alloy::service::execution::extractSchemaTablePairsFromCsv`
- `meta::alloy::service::execution::extractSchemaTablePairsFromRecords`

After deletion, the section immediately above `schemaAndTableSetup` should
end with `loadValuesToDbTable2` and pick up at `schemaAndTableSetup`
directly.

- [ ] **Step 3: Update `setUpDataSQLs` (string overload) to the 2-arg call**

In the same file, replace the body of
`meta::alloy::service::execution::setUpDataSQLs(data:String[1], db:Database[*], dbConfig:DbConfig[1])`
(currently around lines 186–195) so the `schemaAndTableSetup` call is
2-arg:

```pure
function meta::alloy::service::execution::setUpDataSQLs(data:String[1], db:Database[*], dbConfig:DbConfig[1]) : String[*]
{
   let schemaAndTableSetup = $db->map(d|$d->meta::alloy::service::execution::schemaAndTableSetup($dbConfig));

   let formattedData = $data->split('\n')
                            ->map(l|list($l->meta::alloy::service::execution::splitWithEmptyValue()))
                            ->concatenate(list(''));

   $schemaAndTableSetup->concatenate(loadCsvDataToDbTable($formattedData, $db, $dbConfig, t:Table[1]|$t));
}
```

- [ ] **Step 4: Update `setUpDataSQLsV2` to drop the workaround call**

In the same file, replace the body of
`meta::alloy::service::execution::setUpDataSQLsV2`
(currently around lines 197–207) with:

```pure
// Properly handle/respect quoted values
function meta::alloy::service::execution::setUpDataSQLsV2(data:String[1], db:Database[*], dbConfig:DbConfig[1]) : String[*]
{
   let schemaAndTableSetup = $db->map(d|$d->meta::alloy::service::execution::schemaAndTableSetup($dbConfig));

   let formattedData = $data->meta::pure::functions::string::parseCSV()
                            ->concatenate(list(''));

   $schemaAndTableSetup->concatenate(loadCsvDataToDbTable($formattedData, $db, $dbConfig, t:Table[1]|$t));
}
```

This is the load-bearing change: the call now passes through
`schemaAndTableSetup` with no filter, so V2 also emits DDL for every
reachable schema/table.

- [ ] **Step 5: Update the third `setUpDataSQLs` (records overload) to
      the 2-arg call**

In the same file, replace the body of
`meta::alloy::service::execution::setUpDataSQLs(records:List<String>[*], db:Database[*], dbConfig:DbConfig[1])`
(currently around lines 209–213) with:

```pure
function meta::alloy::service::execution::setUpDataSQLs(records:List<String>[*], db:Database[*], dbConfig:DbConfig[1]) : String[*]
{
   let schemaAndTableSetup = $db->map(d|$d->meta::alloy::service::execution::schemaAndTableSetup($dbConfig));
   $schemaAndTableSetup->concatenate(loadCsvDataToDbTable($records, $db, $dbConfig, t:Table[1]|$t));
}
```

- [ ] **Step 6: Confirm the file compiles and no stale references remain**

Run:
```bash
grep -n "dataSchemaTablePairs\|extractSchemaTablePairsFromCsv\|extractSchemaTablePairsFromRecords" legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/DDL/toDDL.pure
```
Expected: no output.

- [ ] **Step 7: Verify no other code references the deleted helpers**

Run:
```bash
grep -rn "extractSchemaTablePairsFromCsv\|extractSchemaTablePairsFromRecords" legend-engine-xts-relationalStore --include="*.pure" --include="*.java"
```
Expected: no output (no remaining callers).

- [ ] **Step 8: Commit**

```bash
git add legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/DDL/toDDL.pure
git commit -m "$(cat <<'EOF'
Complete revert of DDL-filtering workaround from commit 0b17d9d2

Restores schemaAndTableSetup to its original two-arg form and removes
the now-unused extractSchemaTablePairsFromCsv / ...FromRecords helpers.
setUpDataSQLsV2 was the last call site still passing computed pairs
after the partial revert in 076798f; it now passes nothing.

After this commit, DDL is emitted for every reachable schema and table
across all setUpDataSQLs overloads, restoring the pre-#4723 behaviour.
testRelationalServiceWithCaseSensitiveSchemaNames is expected to fail
red against H2 until the dialect fixes in subsequent tasks land.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Confirm the regression test now fails red

**Goal:** Verify that with the workaround fully reverted and no H2 fix yet
in place, `testRelationalServiceWithCaseSensitiveSchemaNames` fails. This
is the red half of the TDD loop.

**Files:**
- Test: `$SVC_TEST_RUNNER/src/test/java/org/finos/legend/engine/testable/service/TestServiceTestSuite.java`

- [ ] **Step 1: Build the dependency chain so the modified Pure can run**

The test runs against compiled Pure. Build the relational core-pure
module first (skip tests for speed):

```bash
mvn clean install -DskipTests -pl legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure -am -T 4
```
Expected: BUILD SUCCESS. Takes a few minutes on a clean repo.

- [ ] **Step 2: Run the case-sensitive regression test**

```bash
mvn test -pl legend-engine-xts-service/legend-engine-test-runner-service -Dtest='TestServiceTestSuite#testRelationalServiceWithCaseSensitiveSchemaNames'
```
Expected: **FAIL.** The failure should reflect a schema-collision symptom
— either an `EqualToJsonAssertFail` where the actual differs from the
expected `[{"Name":"Widget","Type":"TypeA"},…]` (because `schema1.product_table`'s
columns overwrote `Schema1.product_table`'s, so the query returns wrong
or empty rows), or a `TestExecutionStatus.FAIL` with a column-not-found
error.

If the test passes here, stop and investigate — the assumption that the
workaround was the only thing making the test pass is wrong, and the
plan needs revisiting.

No commit at the end of this task — it is a diagnostic checkpoint.

---

## Task 4: Add the always-quote identifier processor in `h2Extension.pure`

**Goal:** Introduce a new identifier processor function that
unconditionally double-quotes every identifier the H2 dialect emits, so
H2 preserves identifier case for both storage and lookup.

**Files:**
- Modify: `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension.pure`

- [ ] **Step 1: Add the `default::*` import**

In `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension.pure`,
add this import line after the existing imports (after line 5):

```pure
import meta::relational::functions::sqlQueryToString::default::*;
```

This is required so the new function can refer to `isFreeMarkerIdentifier`
without a fully-qualified name.

- [ ] **Step 2: Add the always-quote function**

In the same file, after the `dbExtensionLoaderForH2` function (after
line 11), insert this new function definition:

```pure
function meta::relational::functions::sqlQueryToString::h2::processIdentifierWithDoubleQuotesAlways(identifier:String[1], dbConfig: DbConfig[1]):String[1]
{
   if(isFreeMarkerIdentifier($identifier),
      | $identifier,
      | '"%s"'->format($identifier->replace('"', ''))
   );
}
```

Notes for reviewer:
- The freemarker check mirrors the behaviour of the default
  `processIdentifierWithQuoteChar` (in `extensionDefaults.pure:556`),
  which does not quote freemarker placeholders. Without this, template
  substitution like `${table(id, "tbl.col")}` would be wrapped in quotes
  and break.
- `->replace('"', '')` strips any embedded double quotes from the
  identifier before wrapping, matching the existing helper's behaviour.
- The `dbConfig` parameter is unused but required to match the
  `identifierProcessor` SAM signature
  (`{String[1], DbConfig[1] -> String[1]}`) declared at
  `dbExtension.pure:291`.

- [ ] **Step 3: Confirm the function parses**

Run:
```bash
grep -n "processIdentifierWithDoubleQuotesAlways" legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension.pure
```
Expected: one hit at the function definition. Compilation is verified by
the next task's wiring change, which is when Pure first tries to resolve
the function symbol.

No commit at the end of this task — wiring in the next two tasks belongs
in the same commit.

---

## Task 5: Wire the always-quote processor into the H2 v2.1.214 dialect

**Goal:** Make the H2 v2.1.214 dialect use the new processor as its
`identifierProcessor`.

**Files:**
- Modify: `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension2_1_214.pure`

- [ ] **Step 1: Replace the `identifierProcessor` wiring**

In `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension2_1_214.pure`,
change line 39 from:

```pure
      identifierProcessor = processIdentifierWithDoubleQuotes_String_1__DbConfig_1__String_1_,
```

to:

```pure
      identifierProcessor = meta::relational::functions::sqlQueryToString::h2::processIdentifierWithDoubleQuotesAlways_String_1__DbConfig_1__String_1_,
```

The fully-qualified name is used to avoid an ambiguity risk between
`default::*` and `h2::*` imports.

- [ ] **Step 2: Confirm the wiring is in place**

Run:
```bash
grep -n "identifierProcessor =" legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension2_1_214.pure
```
Expected: one hit, referencing `processIdentifierWithDoubleQuotesAlways`.

No commit at the end of this task.

---

## Task 6: Wire the always-quote processor into the H2 v1.4.200 dialect

**Goal:** Same as Task 5 but for the v1.4.200 dialect file.

**Files:**
- Modify: `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension1_4_200.pure`

- [ ] **Step 1: Replace the `identifierProcessor` wiring**

In `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension1_4_200.pure`,
change line 35 from:

```pure
      identifierProcessor = processIdentifierWithDoubleQuotes_String_1__DbConfig_1__String_1_,
```

to:

```pure
      identifierProcessor = meta::relational::functions::sqlQueryToString::h2::processIdentifierWithDoubleQuotesAlways_String_1__DbConfig_1__String_1_,
```

- [ ] **Step 2: Confirm the wiring is in place**

Run:
```bash
grep -n "identifierProcessor =" legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension1_4_200.pure
```
Expected: one hit, referencing `processIdentifierWithDoubleQuotesAlways`.

- [ ] **Step 3: Verify the new processor compiles in both dialects**

Run:
```bash
mvn clean install -DskipTests -pl legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure -am -T 4
```
Expected: BUILD SUCCESS. If Pure fails to resolve the function symbol,
recheck the fully-qualified name and the import added in Task 4 Step 1.

- [ ] **Step 4: Commit Tasks 4–6 together**

```bash
git add legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension.pure legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension2_1_214.pure legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension1_4_200.pure
git commit -m "$(cat <<'EOF'
H2 dialect: always quote emitted identifiers

Adds processIdentifierWithDoubleQuotesAlways in h2Extension.pure and
wires it as the identifierProcessor for both H2 v1.4.200 and v2.1.214
dialects. Unlike the default processIdentifierWithDoubleQuotes, this
processor quotes regardless of dbConfig.quoteIdentifiers, so H2 emits
quoted identifiers consistently and preserves case for both storage
and lookup. Freemarker placeholders are left unquoted to keep template
substitution working.

This change does not on its own fix the case-collision symptom on
CREATE TABLE / INSERT VALUES; those paths bypass the identifier
processor today and are fixed in the next commit.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Fix the `translateCreateTableStatementH2` bypass

**Goal:** Route the H2 v2.1.214 non-temp `CREATE TABLE` emission through
`tableToString`, so schema and table names go through the (now
always-quote) identifier processor instead of being hand-concatenated.

**Files:**
- Modify: `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension2_1_214.pure`

- [ ] **Step 1: Replace the hand-concat in `translateCreateTableStatementH2`**

In `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension2_1_214.pure`,
replace the function definition currently spanning lines 84–95 with:

```pure
function <<access.private>> meta::relational::functions::sqlQueryToString::h2::v2_1_214::translateCreateTableStatementH2(createTableSQL:CreateTableSQL[1], dbConfig:DbConfig[1]): String[1]
{
  let t = $createTableSQL.table;
  let applyConstraints = $createTableSQL.applyConstraints;
  'Create Table ' + $t->tableToString($dbConfig)
  + '('
  + $t.columns->cast(@meta::relational::metamodel::Column)
      ->map(c | $c.name->processColumnName($dbConfig) + ' ' + getColumnTypeSqlTextH2($c.type) + if($c.nullable->isEmpty() || $applyConstraints == false, | '', | if($c.nullable == true , | ' NULL', | ' NOT NULL')))
      ->joinStrings(',')
  + if ($t.primaryKey->isEmpty() || $applyConstraints == false, | '', | ', PRIMARY KEY(' + $t.primaryKey->map(c | $c.name)->joinStrings(',') + ')')
  +');';
}
```

What changed:
- `if($t.schema.name == 'default',|'',|$t.schema.name+'.')+$t.name`
  becomes `$t->tableToString($dbConfig)`. `tableToString`
  (defined at `dbExtension.pure:565-574`) already handles the
  `default`-schema short-circuit, so the conditional is no longer needed
  and the schema and table names route through `identifierProcessor`.
- Note the stray `+` before `'('` on the original line 89 is preserved as
  a leading concatenation operator on a new line — Pure tolerates this,
  but if a parse error appears, drop one of the two `+` characters
  (single `+` suffices).

- [ ] **Step 2: Confirm the bypass is gone**

Run:
```bash
grep -n "schema.name + '.'" legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension2_1_214.pure
```
Expected: one hit (still in `loadValuesToDbTableH2` at line ~108 — that
is the next task).

No commit at the end of this task.

---

## Task 8: Fix the `loadValuesToDbTableH2` bypass

**Goal:** Route the H2 v2.1.214 in-line `INSERT … VALUES` emission through
`tableToString`.

**Files:**
- Modify: `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension2_1_214.pure`

- [ ] **Step 1: Replace the hand-concat in `loadValuesToDbTableH2`**

In the same file, replace the function definition currently spanning
lines 105–116 with:

```pure
function <<access.private>> meta::relational::functions::sqlQueryToString::h2::v2_1_214::loadValuesToDbTableH2(loadTableSQL: LoadTableSQL[1], dbConfig: DbConfig[1]): String[*]
{
  $loadTableSQL.parsedData.values->map(row| let sql =
    'insert into ' + $loadTableSQL.table->tableToString($dbConfig)
    + ' ('
    + $loadTableSQL.columnsToLoad.name->map(colName | $colName->processColumnName($dbConfig))->joinStrings(',')
    +') '
    + 'values ('
    + $row.values->meta::relational::functions::sqlQueryToString::h2::v2_1_214::convertValuesToCsvH2($loadTableSQL.columnsToLoad.type)
    + ');';
  );
}
```

What changed:
- `if($loadTableSQL.table.schema.name == 'default', | '' , | $loadTableSQL.table.schema.name + '.') + $loadTableSQL.table.name`
  becomes `$loadTableSQL.table->tableToString($dbConfig)`. Same rationale
  as Task 7.

- [ ] **Step 2: Confirm both H2 v2.1.214 bypasses are now gone**

Run:
```bash
grep -n "schema.name + '.'" legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension2_1_214.pure
```
Expected: no output.

No commit at the end of this task.

---

## Task 9: Fix the `loadValuesToDbTableDefault` bypass

**Goal:** Route the shared default `loadValuesToDbTableDefault` through
`tableToString`. This is the path that the H2 v1.4.200 dialect delegates
to via `loadValuesToDbTableForH2`'s non-CSV branch
(`h2Extension1_4_200.pure:74-78`) and is also used by any other dialect
that does not override it.

**Files:**
- Modify: `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/extensionDefaults.pure`

- [ ] **Step 1: Replace the hand-concat in `loadValuesToDbTableDefault`**

In `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/extensionDefaults.pure`,
replace the function definition currently spanning lines 630–643 with:

```pure
function meta::relational::functions::sqlQueryToString::default::loadValuesToDbTableDefault(loadTableSQL:LoadTableSQL[1] , dbConfig: DbConfig[1]) : String[*]
{
   if ($loadTableSQL.parsedData.values->isEmpty(),
    | [],
    |
      let sql = 'insert into ' + $loadTableSQL.table->tableToString($dbConfig) + ' ('
            + $loadTableSQL.columnsToLoad.name->map(colName | $colName->processColumnName($dbConfig))->joinStrings(',')
            +') '
            + 'values';

      $loadTableSQL.parsedData.values->map(row |  '(' + $row.values->meta::relational::functions::database::testDataSQLgeneration::convertValuesToCsv($loadTableSQL.columnsToLoad.type) + ')')
        ->joinStrings($sql, ', ', ';');
   );
}
```

What changed:
- `if($loadTableSQL.table.schema.name=='default', |'' ,|$loadTableSQL.table.schema.name + '.') + $loadTableSQL.table.name`
  becomes `$loadTableSQL.table->tableToString($dbConfig)`.

Cross-dialect note: this is a behaviour change for any dialect that
inherits `loadValuesToDbTableDefault` and emits `INSERT … VALUES`
statements. For dialects whose `identifierProcessor` only quotes when
`quoteIdentifiers=true` or for reserved words, the visible output is
unchanged for typical identifiers. For dialects that override
`schemaNameToIdentifier` / `tableNameToIdentifier`, the output now
correctly reflects those overrides — this fixes a latent inconsistency.

- [ ] **Step 2: Confirm the bypass is gone**

Run:
```bash
grep -n "table.schema.name + '.'" legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/extensionDefaults.pure
```
Expected: no output.

- [ ] **Step 3: Verify no other DDL bypasses remain in H2**

Run:
```bash
grep -rn "schema.name + '.'\|schema.name+'.'" legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString --include="*.pure"
```
Expected: no output. (If hits remain in non-H2 dialects, they are out of
scope; only flag if anything in `dbSpecific/h2/` or in `default::` shared
helpers shows up.)

- [ ] **Step 4: Build to surface any compilation issues**

```bash
mvn clean install -DskipTests -pl legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure -am -T 4
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit Tasks 7–9 together**

```bash
git add legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension2_1_214.pure legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/extensionDefaults.pure
git commit -m "$(cat <<'EOF'
Route H2 CREATE TABLE and INSERT VALUES through identifier processor

Three emitters previously hand-concatenated schema and table names as
raw strings rather than routing through dbConfig.identifierProcessor:

  - translateCreateTableStatementH2 (h2Extension2_1_214.pure)
  - loadValuesToDbTableH2 (h2Extension2_1_214.pure)
  - loadValuesToDbTableDefault (extensionDefaults.pure), reached by the
    H2 v1.4.200 dialect via delegation

All three now use the tableToString helper, which dispatches schema and
table names through identifierProcessor and handles the default-schema
short-circuit. Combined with the always-quote H2 identifier processor,
this restores case-sensitive identifier handling for CREATE TABLE and
INSERT VALUES against H2 — the paths that the new regression test
exercises.

For non-H2 dialects whose identifier processor only quotes conditionally,
the visible INSERT VALUES output is unchanged for typical identifiers.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 10: Confirm the regression test now passes green

**Goal:** Verify the H2 dialect changes make
`testRelationalServiceWithCaseSensitiveSchemaNames` pass. This is the
green half of the TDD loop and the load-bearing assertion for the fix.

**Files:**
- Test: `$SVC_TEST_RUNNER/src/test/java/org/finos/legend/engine/testable/service/TestServiceTestSuite.java`

- [ ] **Step 1: Run the case-sensitive regression test**

```bash
mvn test -pl legend-engine-xts-service/legend-engine-test-runner-service -Dtest='TestServiceTestSuite#testRelationalServiceWithCaseSensitiveSchemaNames'
```
Expected: **PASS.**

The test now exercises the full revert path: both `Schema Schema1` and
`Schema schema1` get their DDL emitted; with the always-quote H2
identifier processor and the routed CREATE TABLE / INSERT VALUES paths,
each schema is created as a distinct object; the query for `Name, Type`
returns the expected `Widget/TypeA` and `Gadget/TypeB` rows.

If this fails, diagnose:
- Re-run with `-X` for verbose output.
- Check that the case-sensitive schema model and service `.pure`
  resources are the ones from commit 0b17d9d2 / 076798f (TDSv2 form).
- Confirm Pure was rebuilt before running the test (Maven incremental
  builds sometimes miss `.pure` changes — `mvn clean install -DskipTests`
  on the core-pure module is the safe reset).
- Check that the H2 dialect actually loads
  `processIdentifierWithDoubleQuotesAlways` by adding a temporary
  printout, or by re-running with Pure IDE.

No commit at the end of this task — diagnostic checkpoint.

---

## Task 11: Update `testDDL.pure` expected DDL strings for quoted output

**Goal:** Update the three test functions in `testDDL.pure` so their
expected SQL lists include the now-emitted double-quote characters around
schema, table and column identifiers.

**Files:**
- Modify: `$CORE_PURE/src/main/resources/core_relational/relational/sqlQueryToString/DDL/testDDL.pure`

The same expected-string update applies to all three test functions in
the file:

- `testSetupDataSqlGeneration` (around lines 28–71)
- `testSetupDataSqlGenerationWithDataAsString` (around lines 76–116)
- `testSetupDataSqlGenerationWithColumnValueHasDelimiterAndQuotes`
  (around lines 118–159)

The DDL statement shape changes as follows. Schemas become
`"<name>"`; tables become `"<name>"`; columns and PRIMARY KEY references
in `Create Table` become `"<NAME>"` (column names are upper-cased by
`columnNameToIdentifier`'s reserved-word path only for `kerberos`, `date`,
`first` — but in `processColumnName`, every column goes through both
`columnNameToIdentifier` and `identifierProcessor`. The visible effect is
that **every** column name now gets quoted by the always-quote processor,
preserving its original case from the model.)

⚠ Reviewer note: `columnNameToIdentifierDefault` in
`extensionDefaults.pure:19-22` uppercases and quotes `date`, `kerberos`,
`first`. After the always-quote processor strips inner quotes and
re-wraps, the visible result for `date` is `"DATE"` (not `"date"`). For
other columns it is `"<original-case>"`. Apply this rule when rewriting
each expected line.

- [ ] **Step 1: Rewrite the expected list in
      `testSetupDataSqlGeneration`**

Replace the contents of the `expectedSqls` list in
`meta::relational::tests::ddl::testSetupDataSqlGeneration` with:

```pure
  let expectedSqls= [
   'Drop schema if exists "productSchema" cascade;',
   'Create Schema if not exists "productSchema";',
   'Drop schema if exists "default" cascade;',
   'Create Schema if not exists "default";',
   'Drop table if exists "productSchema"."productTable";',
   'Create Table "productSchema"."productTable"("ID" INT NOT NULL,"NAME" VARCHAR(200) NULL, PRIMARY KEY(ID));',
   'Drop table if exists "personTable";',
   'Create Table "personTable"("ID" INT NOT NULL,"FIRSTNAME" VARCHAR(200) NULL,"LASTNAME" VARCHAR(200) NULL,"AGE" INT NULL,"ADDRESSID" INT NULL,"FIRMID" INT NULL,"MANAGERID" INT NULL, PRIMARY KEY(ID));',
   'Drop table if exists "PersonTableExtension";',
   'Create Table "PersonTableExtension"("ID" INT NOT NULL,"FIRSTNAME" VARCHAR(200) NULL,"LASTNAME" VARCHAR(200) NULL,"AGE" INT NULL,"ADDRESSID" INT NULL,"FIRMID" INT NULL,"MANAGERID" INT NULL,"birthDate" DATE NULL, PRIMARY KEY(ID));',
   'Drop table if exists "differentPersonTable";',
   'Create Table "differentPersonTable"("ID" INT NOT NULL,"FIRSTNAME" VARCHAR(200) NULL,"LASTNAME" VARCHAR(200) NULL,"AGE" INT NULL,"ADDRESSID" INT NULL,"FIRMID" INT NULL,"MANAGERID" INT NULL, PRIMARY KEY(ID));',
   'Drop table if exists "firmTable";',
   'Create Table "firmTable"("ID" INT NOT NULL,"LEGALNAME" VARCHAR(200) NULL,"ADDRESSID" INT NULL,"CEOID" INT NULL, PRIMARY KEY(ID));',
   'Drop table if exists "firmExtensionTable";',
   'Create Table "firmExtensionTable"("firmId" INT NOT NULL,"legalName" VARCHAR(200) NULL,"establishedDate" DATE NULL, PRIMARY KEY(firmId));',
   'Drop table if exists "otherFirmTable";',
   'Create Table "otherFirmTable"("ID" INT NOT NULL,"LEGALNAME" VARCHAR(200) NULL,"ADDRESSID" INT NULL, PRIMARY KEY(ID));',
   'Drop table if exists "addressTable";',
   'Create Table "addressTable"("ID" INT NOT NULL,"TYPE" INT NULL,"NAME" VARCHAR(200) NULL,"STREET" VARCHAR(100) NULL,"COMMENTS" VARCHAR(100) NULL, PRIMARY KEY(ID));',
   'Drop table if exists "locationTable";',
   'Create Table "locationTable"("ID" INT NOT NULL,"PERSONID" INT NULL,"PLACE" VARCHAR(200) NULL,"DATE" DATE NULL, PRIMARY KEY(ID));',
   'Drop table if exists "placeOfInterestTable";',
   'Create Table "placeOfInterestTable"("ID" INT NOT NULL,"locationID" INT NOT NULL,"NAME" VARCHAR(200) NULL, PRIMARY KEY(ID,locationID));',
   'Drop table if exists "validPersonTable";',
   'Create Table "validPersonTable"("ID" INT NOT NULL,"FIRSTNAME" VARCHAR(200) NULL,"LASTNAME" VARCHAR(200) NULL,"AGE" INT NULL,"ADDRESSID" INT NULL,"FIRMID" INT NULL,"MANAGERID" INT NULL, PRIMARY KEY(ID));',
   'insert into "personTable" ("ID","FIRSTNAME","LASTNAME","AGE","ADDRESSID","FIRMID","MANAGERID") values (1,\'Peter\',\'Smith\',23,1,1,2);'
   ];
```

Notes:
- `locationTable`'s `date` column is now `"DATE"` (upper) because
  `columnNameToIdentifierDefault` upper-cases reserved-word columns
  before the always-quote processor wraps them.
- `PRIMARY KEY(ID)` and `PRIMARY KEY(firmId)` etc. are intentionally
  left **unquoted** in the original — the existing emitter at
  `extensionDefaults.pure:618` does not route primary key column names
  through `processColumnName`. Whether to quote them too is out of
  scope; preserve the existing unquoted form to keep the test as a
  faithful record of current behaviour.

- [ ] **Step 2: Apply the same rewrite to
      `testSetupDataSqlGenerationWithDataAsString`**

Replace the `expectedSqls` list in the second test function with an
identical list to Step 1 (the test data is the same).

- [ ] **Step 3: Apply the same rewrite to
      `testSetupDataSqlGenerationWithColumnValueHasDelimiterAndQuotes`**

Replace the `expectedSqls` list in the third test function with an
identical list to Step 1, **except** the two trailing `insert` lines,
which differ for this test. Quote the table name and the column list in
those two lines as well:

```pure
   'insert into "personTable" ("ID","FIRSTNAME","LASTNAME","AGE","ADDRESSID","FIRMID","MANAGERID") values (1,\' James\',\'I\'\'m Johnson, Jr\',23,1,1,2);',
   'insert into "personTable" ("ID","FIRSTNAME","LASTNAME","AGE","ADDRESSID","FIRMID","MANAGERID") values (1,\' Peter\',\'  "I\'\'m Smith, Jr"\',23,1,1,2);'
```

- [ ] **Step 4: Run the testDDL tests**

```bash
mvn test -pl legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure -Dtest='Test_Pure_Relational' -Dpure.test.filter='meta::relational::tests::ddl' 2>&1 | tail -40
```
Expected: PASS for the three `testSetupDataSqlGeneration*` tests.

If a test fails on a single line, the actual SQL emitted is in the
failure message; reconcile any per-line difference (most commonly a
column that needs upper-casing because it is in the reserved-word list,
or a column whose original case differs from what is asserted).

- [ ] **Step 5: Commit**

```bash
git add legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/DDL/testDDL.pure
git commit -m "$(cat <<'EOF'
Update testDDL expectations for H2 quoted identifiers

H2 dialect now emits double-quoted schema, table and column identifiers
unconditionally. Update the expected SQL lists in the three
testSetupDataSqlGeneration variants to match. Reserved-word columns
(date) appear as "DATE" because columnNameToIdentifierDefault uppercases
them before the identifier processor wraps them.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 12: Survey other golden SQL fixtures

**Goal:** Identify any other Pure or Java tests that assert exact H2-dialect
SQL output and would break under the new quoted form. Flag (but do not
fix) findings so the user can triage them against CI results.

**Files:**
- Read only — this task produces a list, not changes.

- [ ] **Step 1: Search for Pure tests that pin H2 SQL output**

Run:
```bash
grep -rln "DatabaseType.H2" legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational --include="*.pure" | xargs grep -ln "assertEquals\|assertSameElements" | head -20
```
Expected: a list of `.pure` files that combine `DatabaseType.H2` with
string assertions. Review each candidate and note whether the asserted
strings contain `select … from <schema>.<table>` / `insert into … `
patterns that will change shape.

- [ ] **Step 2: Search Java tests for hard-coded H2 SQL**

Run:
```bash
grep -rln 'jdbc:h2\|DatabaseType.H2' legend-engine-xts-relationalStore --include="*.java" | xargs grep -ln 'select .* from\|insert into' 2>/dev/null | head -20
```
Expected: a list of Java test files. Review candidates for asserted SQL
strings.

- [ ] **Step 3: Search PCT golden files for the H2 store**

Run:
```bash
find legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-h2 -name '*.pure' -o -name '*.json' | head -20
```
Expected: a list of PCT resources. PCT comparisons typically assert
SQL output for the relational store; any that pin H2 strings will need
updating.

- [ ] **Step 4: Record the survey**

Append a short list of files identified to the bottom of this plan
document as a checklist for CI triage. Do not modify any of the
identified files in this task — they will be triaged once CI runs.

```bash
git add docs/superpowers/plans/2026-05-29-h2-case-sensitive-identifiers.md
git commit -m "$(cat <<'EOF'
Record golden-SQL survey for CI triage

Lists Pure and Java test files that pin H2 SQL output and may need
expectation updates once the H2 always-quote dialect change lands.
These will be triaged against CI failures rather than guessed at
locally.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 13: Push to CI for full-suite triage

**Goal:** Get CI to run the suites we are not running locally and surface
the actual list of golden-fixture updates needed beyond `testDDL.pure`.

**Files:**
- No file changes.

- [ ] **Step 1: Confirm the local commits are linear and clean**

```bash
git log --oneline master..HEAD
```
Expected: a sequence ending with the design-doc commit (already present),
then the workaround revert, the always-quote dialect, the DDL bypass
fix, the testDDL expectations, the survey.

- [ ] **Step 2: Push the branch and open a draft PR for CI**

```bash
git push -u origin h2case
gh pr create --draft --title 'H2: case-sensitive schema and table identifiers' --body "$(cat <<'EOF'
## Summary
- Always-quote in the H2 dialect plus DDL-translator bypass fixes
- Completes the revert of #4723's DDL-filtering workaround
- See design doc: docs/superpowers/specs/2026-05-29-h2-case-sensitive-identifiers-design.md
- See plan: docs/superpowers/plans/2026-05-29-h2-case-sensitive-identifiers.md

## Test plan
- [x] testRelationalServiceWithCaseSensitiveSchemaNames passes locally
- [x] testDDL.pure testSetupDataSqlGeneration variants pass locally
- [ ] CI: full Pure relational test suite
- [ ] CI: H2 PCT suite
- [ ] CI: any other golden-SQL fixtures flagged in Task 12 survey

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

⚠ Before running the `gh pr create` command, confirm with the user
that they want the PR opened. The plan opens a draft PR, but pushing and
opening a PR is a shared-state action — pause for confirmation.

- [ ] **Step 3: Triage CI failures iteratively**

For each new failure that is genuinely an expectation update (rather than
a real bug), update the relevant fixture string by adding quotes in the
same shape used in Task 11. Commit each fixture update as its own
commit so the bisection story stays clear:

```bash
git add <fixture file>
git commit -m "Update <fixture> expectations for H2 quoted identifiers"
```

For each failure that looks like a real bug rather than a quoting
diff, stop and reconcile with the design before patching.

No final commit at the end of this task — the work concludes when CI is
green.

---

## Self-review against the spec

**Spec coverage check:**
- "Introduce an always-quote identifier processor" → Task 4.
- "Wire it into both H2 dialect versions" → Tasks 5–6.
- "Fix `translateCreateTableStatementH2`" → Task 7.
- "Fix `loadValuesToDbTableDefault`" → Task 9.
- "Fix `loadValuesToDbTableH2`" → Task 8.
- "Audit the H2 dialect for further bypasses" → Task 9 Step 3.
- "Revert commit 0b17d9d2" → Task 2 (modulo the partial pre-revert).
- "Use testRelationalServiceWithCaseSensitiveSchemaNames as verification"
  → Tasks 3 (red) and 10 (green).
- "Update golden expectations" → Task 11 (testDDL), Task 12 (survey for
  others), Task 13 (iterative CI triage).
- "Approach A documented as an option" → reflected in spec only; not
  implemented.
- "Option 3 documented as an alternative" → reflected in spec only; not
  implemented.

**Placeholder scan:** No TBDs, TODOs, or "implement later" markers.

**Type/name consistency:** `processIdentifierWithDoubleQuotesAlways` is
the function defined in Task 4 and referenced in Tasks 5–6. `tableToString`
is the function referenced in Tasks 7–9 and lives at
`dbExtension.pure:565`. Both names match across tasks.

---

## CI triage checklist (populated in Task 12)

Use this section to record files identified by the Task 12 survey that
may need expectation updates after CI surfaces failures. Append entries
as `- [ ] <file>:<short description>`.
