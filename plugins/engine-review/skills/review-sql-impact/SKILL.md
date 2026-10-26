---
name: review-sql-impact
description: "SQL-generation lens of a legend-engine impact review: compares the execution plans and SQL that every EMIT corpus service and mapping test produces on base vs head (alias-normalized, clustered by structural delta - joins, tables, subqueries, distinct, group by, dropped predicates), traces each cluster to the code that caused it, and flags transitive impact and coverage gaps. Use for changes to pureToSQLQuery, the router, relational Pure, plan generation or relational execution, or when asked how a change affects generated SQL or whether it can cause a join explosion."
---

# SQL impact lens

Inputs: a finished `engine-review run` directory (see `review-impact`), or two snapshot directories
compared with `engine-review compare <base> <head> --out <dir>`.

Background reading, needed the first time: `references/sql-review-guide.md`. It covers what each
metric delta means and which ones change results.

## Procedure

1. **Check the run is whole.** In `report.md`, the header lists `head rebuilt` and the caveats. If
   the changed module is not in `head rebuilt` and no caveat explains why, the head ran base code
   and the comparison is void.
2. **Read the outcome table.** Any `error-new` is a regression until explained. If there are
   linkage errors (`NoSuchMethodError`, `ClassNotFoundException`) and the caveats mention
   un-rebuilt dependents, re-run with `--rebuild dependents` before reporting.
3. **Go through every cluster, most severe first.** For each one:
   - Read the representative SQL diff. It shows aliases as generated; the cluster was formed after
     alias normalization, so alias noise is already discounted.
   - Name the structural change in plain words, for example "the second exists() branch is dropped",
     "the person table is joined twice", or "the filter moved from ON to WHERE".
   - Find the hunk that causes it, and check that the cluster's members all reach that hunk.
   - Decide whether it is **intended** (it matches the PR's stated intent and is asserted by a test
     in the PR), **transitive** (a shape the PR did not mention, which is a finding), or **noise**
     (justify it).
   - For a transitive or suspicious cluster, say whether the *rows* change, not just the SQL. When
     in doubt, run the corpus test on head (`engine-review snapshot --rev <head> --suites relational
     --out <dir>`); its result is in `_surefire`.
4. **Alias-only changes.** Report a count. They matter only if aliases reach user-visible output, or
   if many queries changed when the PR claims a narrow fix, which is a sign of a shared path.
5. **EMIT test outcomes.** A pass → fail flip in `report.md` is a functional regression with a
   concrete reproducer. Quote the test and its message.
6. **Coverage gaps.** For each changed SQL-path function, ask whether any corpus plan changed
   because of it. If none did, grep the corpus for the construct. If the construct is absent, report
   the gap, and offer to add an EMIT model or probe service as `sql-review-guide.md` describes. A
   gap that is left open recurs on the next PR.
7. **Blind spots.** Copy the relevant `engine-review scope` blind spots into the review. EMIT
   executes on H2 only, so a dialect-specific change needs that dialect's PCT run.

## Worked example: #5266, reverted as #5418

The command was `engine-review run --base <master d3dc49c9757> --head <master + #5266 re-applied>`
with `--suites relational`. The two-line change gave 199 plans: 191 identical, 2 unchanged errors,
and 6 changed, falling into three high-severity clusters.

1. **`duplicate_joins +1`, ranked first:** `sameExistsInFilterAndProjection` and
   `sameExistsReachedTwiceFromToOneSide`. The same `exists()` used twice in one query is joined twice
   on head, where base had one join. This is the join explosion that got #5266 reverted. The mapping
   tests still pass, because the duplicate joins are on a distinct key. Only the SQL shows it.
2. **`join.left_outer +1; subqueries +1; ...`:** the multi-`exists` probes (`&&`, `||`, negated plus
   positive). Master's SQL keeps only the first `exists()`, giving
   `where x is not null and x is not null`. Head adds the missing branch. This is the intended fix.
3. **The same, plus `boolean_ops +1`:** chained `filter(exists A)->filter(exists B)`. Master drops the
   second filter entirely, and head restores it. This is the intended fix, though the PR never
   mentioned the chained form.

The verdict a reviewer could have written before merging: **"needs changes."** The fix is right for
different predicates on one property, but it must keep merging identical ones (cluster 1). Because
the rows stay correct, only the SQL comparison guards that shape. The corpus now carries it, so the
next attempt at this fix is checked automatically.
