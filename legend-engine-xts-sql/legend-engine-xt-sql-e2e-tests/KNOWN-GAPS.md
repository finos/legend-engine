# SQL E2E — known gaps and traps

**Read this before investigating any failure in this suite.** Most surprising results here are
*already known* and several are artifacts of the harness rather than bugs in Legend. Chasing one of
them costs hours. This page is the short version; the long-form writeup with evidence is
[`docs/engineering/guides/sql-e2e-interpreted-devloop.md`](../../docs/engineering/guides/sql-e2e-interpreted-devloop.md).

## How to run

```bash
# Compiled suite — the authoritative signal, and what CI runs
mvn test -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests -Dtest=TestPostgresParity

# One case or one family (substring match on the test id)
mvn test -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests \
    -Dtest=TestPostgresParity -Dtest.filter=to_char_token__HH

# Re-record baselines after a real behaviour change
mvn test -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests \
    -Dtest=TestPostgresParity -Dparity.updateStatus=true
```

If you edit corpus YAML or the test class and then invoke `surefire:test` directly (instead of
`mvn test`), refresh the copies under `target/` first — surefire skips both phases and will
silently use stale files:

```bash
mvn -o -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests process-test-resources   # YAML changed
mvn -o -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests test-compile             # test class changed
```

There is also an **interpreted** dev loop (`legend-engine-xt-sql-e2e-pure`) for iterating on
`.pure` sources in seconds instead of a 15–25 min rebuild. **It is not a correctness oracle** — see
the first trap below.

## Baseline statuses

Each case records `expected_tds_status` / `expected_rel_status`, re-recorded with
`-Dparity.updateStatus=true`. A run fails only when a case *diverges from its baseline*, so a
red run means "behaviour changed", not "something is broken".

| status | meaning |
|---|---|
| `PASS` | Legend matched the reference Postgres |
| `FAIL` | both ran; results differ |
| `ERROR` | Legend could not plan/execute it (usually an unsupported construct) |
| `BUG` | Postgres itself rejected the reference SQL |
| `SKIP` | explicitly skipped via `skip:` |

Current corpus: **2,307 cases**. TDS baselines: 1005 PASS / 1128 ERROR / 42 FAIL / 70 BUG / 62 SKIP.
Relation baselines: 1161 PASS / 968 ERROR / 46 FAIL / 70 BUG / 62 SKIP. **A large ERROR count is
expected** — the corpus deliberately enumerates the whole Postgres surface, most of which Legend
does not implement.

## Traps that have already cost real time

### 1. Interpreted-mode results are not trustworthy — always confirm against compiled

The interpreted dev loop diverges from compiled on constructs that are perfectly fine in
production. Every divergence investigated in depth so far has been an artifact, not a product bug.
Two confirmed Pure-interpreter defects are behind most of them:

- **`pair` type-variable capture.** `pair<U,V>` binds its own `U` over a polymorphic function
  reference that also names a parameter `U`, dropping the quantifier. Breaks every quantified /
  scalar subquery (`= ANY`, `> ALL`, `IN (subquery)`) with
  `Type Error: 'ConcreteFunctionDefinition<<U,Z> {...}>' not a subtype of 'SqlTransformContext'`.
  Compiled is immune (generated Java uses wildcards).
- **`reactivate` does not constant-fold interpreted.** `LIMIT n OFFSET m` legitimately emits
  `slice(rel, m, m + n)` and relies on pre-evaluation (`preeval.pure:859`) to fold it. The
  compiled `reactivate` folds; the interpreted one does not, so it surfaces as
  `"Not a number: ... instanceOf InstanceValue"` (TDS) or `"Invalid type for second parameter
  inside the slice function"` (Relation). `ORDER BY ... NULLS FIRST/LAST` fails in the same
  subsystem.

Do **not** infer a Relation-path bug from these: both entry points begin with `assertRelation(...)`,
so the TDS path bails *by design* and is baselined `ERROR`. "TDS passes" there means "TDS errors
as intended".

**A wrong-answer (`FAIL`) result is no more trustworthy than an `ERROR`.** The interpreter can
silently compute a different value — `NULL = NULL` evaluates to `true` interpreted and to `NULL`
compiled. `null_equals_null`, `null_not_equals`, `null_is_distinct_from` and
`case_simple_null_compare` all look like three-valued-logic bugs interpreted, and all four pass
both paths compiled.

### 2. A query with no table reference never reaches Legend

`StatementDispatcherVisitor.visitDefault` routes any statement whose `TableNameExtractor` finds no
qualified name to `ExecutionType.Metadata_Generic`, which `SQLManager` serves from a **real
Postgres container**, not Legend. So `SELECT to_char(TIMESTAMP '...', 'HH')` with no `FROM` is
answered by Postgres and compared against Postgres — it passes while testing nothing.

This is correct production behaviour (a BI client's `SELECT 1` should not invoke the planner), but
it means **a new test case must reference a real table or it is vacuous**. Append something like
`FROM dates WHERE id = 1`. Note a derived table does not count — only real table names and table
functions do.

6 cases remain deliberately table-free: `functions/sequence_functions` (3 — `NEXTVAL`/`SETVAL` are
side-effecting and both sides share one database, so they can never match; arguably should be
`skip:`), `smoke_tests` (2 — deliberate coverage of the constant-query dispatch path), and
`structural/values_clause` (1 — standalone `VALUES` has no `SELECT` to attach a `FROM` to).

### 3. Quote any `sql:` value containing `#`

YAML treats ` #` in an unquoted scalar as a comment. 18 cases silently executed as truncated
nonsense for months — `SELECT json_val #> '{a,b}' AS result FROM json_data WHERE id = 2` ran as
`SELECT json_val`, losing the `FROM` too. Always quote:

```yaml
sql: "SELECT id, int_val # small_val AS result FROM numbers WHERE id = 1"
```

## Known unsupported areas (baselined `ERROR` — expected, do not chase)

| area | detail |
|---|---|
| `to_char` tokens | Only `YYYY YYY YY Y MM MONTH/MON(+case) DD DDD D DAY/DY(+case) HH24 MI SS WW Q` are implemented (`toCharFormats()` in `fromPure.pure`). `HH`, `HH12`, `IYYY`, `IW`, `CC`, `TZ*`, `AM/PM`, `MS/US`, `FF1`–`FF6` etc. are explicitly `toCharUnsupported` and have been since 2023-11-13. |
| `EXTRACT` fields | Only `YEAR QUARTER MONTH WEEK DOW DOY DAY HOUR MINUTE SECOND EPOCH` (`extractFieldToValue()`). `CENTURY`, `DECADE`, `ISOYEAR`, `MILLENNIUM`, `TIMEZONE*`, `JULIAN`, `MICROSECONDS`, `MILLISECONDS` are not. |
| geometric operators | 157 cases, effectively none supported (`#`, `##`, `<->`, …). |
| range / network / bitstring / FTS / other-type operators | largely unsupported. |
| JSON | `json_data` is in the fixture and the compiled model, but JSON column types raise `"JSON not supported yet!"` in the Pure relational metamodel, so the interpreted model omits the table entirely. Some `#>`-family operators *do* work on the Relation path. |
| set-returning functions | `UNNEST`, `generate_series` etc. unsupported. |
| sequences | `NEXTVAL`/`SETVAL` unsupported (and untestable here, see above). |
| standalone `VALUES` | TDS: "values only supported on relation inputs"; Relation: "only single value values currently supported". |

## Gaps specific to the interpreted dev loop

These affect `legend-engine-xt-sql-e2e-pure` only; the compiled suite is unaffected.

1. **Constant-only queries** (`SELECT 1`) fail in-process — TDS hits a plan-JSON serialisation
   bug, Relation hits `wrapPrimitiveInTDS is not supported yet`. They pass on the wire path.
2. **`json_data` is absent from the interpreted Pure model** (JSON column types unsupported), so
   every JSON-column case is unavailable there.
3. **Parameterised accessors** (`tds_persons_by_dept`) are skipped; no corpus case uses them.
