# Reading an SQL impact report

`engine-review run` compares the execution plan of every service and every debuggable test (mapping
test suites) in the EMIT corpus, base vs head. Each plan lands in one bucket:

| bucket | meaning | default reading |
|---|---|---|
| identical | same SQL text, same plan | no impact |
| alias-only | identical after renumbering table aliases | cosmetic, unless aliases are quoted into user-visible output |
| changed | SQL shape or non-SQL plan content differs | **read every cluster** |
| error-new | base planned it, head throws | regression until proven otherwise |
| error-fixed | base threw, head plans it | behaviour change; check it was intended and asserted |
| error-changed | both throw, different message | usually noise; check for a swallowed root cause |
| added / removed | the plan exists on one side only | corpus or discovery difference, not product impact |

Changed plans are **clustered by their metric delta**, e.g. `join.left_outer +1; subqueries +1;
table.persontable +1`. One cluster is one code-level cause. Read the representative diff, then
check that every member plausibly shares the cause.

## Structural deltas and why they matter

- **`duplicate_joins` up.** A join now repeats an earlier one exactly: same target, same ON
  condition, only the alias differs. This is the **join explosion** pattern, and it is what got
  #5266 reverted. That change made the `exists()` join alias per node (`_ecq` plus the node id), so
  the *same* `exists()` used twice in one query (in the filter and again in a projection, or reached
  twice through an association) became two identical joins instead of one. N uses gave N joins.
  Rows stayed correct only because each join is on a `select distinct` key, so tests kept passing.
  Treat any increase as high severity.
- **`joins` / `join.*` up, same `table.X` up, `duplicate_joins` unchanged.** A new join to a table
  already in the query, for a *different* predicate or path. It can be the intended fix, as in
  #5266's handling of `exists(A) && exists(B)`. Check whether the new join is on a unique key. If it
  is not, rows multiply: wrong counts and sums, and duplicates in a projection.
- **`join.inner` ↔ `join.left_outer`.** Inner drops rows with no match, outer keeps them with nulls.
  This always changes semantics for optional or to-many navigation.
- **A predicate moves between `ON` and `WHERE` of an outer join.** On an outer join these are
  different queries: `WHERE` turns the outer join back into an inner one. The metric counts do not
  change, so look for it in the SQL diff of "same shape, different SQL text" clusters.
- **`distinct` added or removed.** Something is compensating for, or no longer compensating for,
  row multiplication. Find out which.
- **`exists` / `in_subquery` ↔ join.** The relational router turns `exists()` into a join against
  a `select distinct` subquery. A change in that rewrite shows up as `subqueries ±1` together with
  join changes.
- **`group_by`, `having`, `window` changed.** Aggregation semantics changed. Check every aggregate
  column and the grouping keys.
- **Boolean predicate count down.** A filter branch was **dropped**. Master does exactly this today
  for several `exists()` on one to-many property: the second branch disappears and the SQL reads
  `x is not null and x is not null`. A test can still pass if its data never distinguishes the
  branches. The SQL is the evidence; a passing test is not.
- **`resultType` changes (non-SQL plan changes).** Columns or types of the result changed, which is
  visible to every caller. Severity is high.

## Telling intended from unintended

1. List what the PR says it changes: which Pure functions, which query shapes.
2. For each cluster, find the hunk that produces it. Grep the changed function's callers in
   `core_relational` (for example `buildExistsAsJoinWithNullCheck` ← `processExists` ← filter and
   project paths). A cluster whose models do not use the PR's shape is **transitive impact**. That
   is what this review exists to find.
3. A cluster with members across unrelated features (milestoning, graphFetch, union) means the
   change sits on a shared path. Treat it as high risk even if each diff looks small.

## When the report shows no change but the diff touched SQL generation

Absence of change is **not** safety. It usually means the corpus does not exercise the changed
branch. Identify the construct the changed code handles, then grep the corpus for it:

```bash
grep -rln '<construct>' legend-engine-*/**/src/test/resources/*emit-models*/
```

If nothing matches, say so in the review as a coverage gap and propose (or write) an EMIT model:

- Put it in `relational-emit-models/` of `legend-engine-xt-relationalStore-emit`. Reuse
  `relational-shared-domain` and `relational-shared-firm-db` through `dependencies`, the way
  `relational-exists-to-many` does.
- Make it a **mapping test suite** with `EqualToJson` asserts. Sort the result (TDS `sort([asc(...)])`
  or `sort(~col->ascending())`) so row order is deterministic.
- **Design the data so wrong answers differ from the right one.** For each shape, write down the
  plausible bugs (branch dropped, join merged, join duplicated, null side lost) and add rows that
  make each bug change the result.
- If master gets the shape wrong today, do **not** assert the wrong answer, and do not add a failing
  test. Add a plan-only **probe service**: a `Service` with no `testSuites`. EMIT plans it, the
  snapshot captures its SQL, and the documentation says why it is a probe. Move it into a test suite
  once fixed.
- Tag it with `query:*` features (`docs/emit/emit.md` §6.2).
- Check it on one revision with `engine-review snapshot --rev <base> --suites relational --out <dir>`.
  That takes about a minute once the base is built.

## Blind spots

- **H2 only.** EMIT executes on H2. Dialect rendering (`dbExtension/*` other than H2,
  `sqlDialectTranslation`) is not compared. Run that dialect's PCT module, or capture SQL from a PCT
  run (JDBC logging), and compare by hand.
- **Not in the corpus.** The SQL-to-Pure transpiler (`legend-engine-xts-sql`) has its own corpus
  (`TestPostgresParity`, skill `pure-lsp-sql-e2e`).
- **Plans come from each test runner's debug path.** Every runner implements it once #5423 is in a
  revision. On older revisions, only services and mapping tests contribute plans, and the other
  tests' plans show up as "only in head" or "only in base".
- **Head built with `--rebuild changed`.** Modules downstream of the change were not recompiled. A
  signature change shows up as `error-new` with linkage errors such as `NoSuchMethodError` or
  `ClassNotFoundException`. Re-run with `--rebuild dependents` before believing such an error.
