---
name: review-impact
description: "Targeted impact review of a legend-engine change (PR number, branch, commit range, or uncommitted work) for reviewers and authors: classifies the diff into hot-area lenses (SQL generation, protocol JSON, auth/JDBC connectivity), runs the EMIT corpus on base and head to show how generated SQL and protocol JSON actually change, applies the auth checklist statically, and writes a review that separates intended change from transitive impact and names coverage gaps. Use when asked to review a legend-engine PR or change, to assess its impact, blast radius or regression risk, or whether a small change to pureToSQLQuery/router/protocol/auth code is safe."
---

# Impact review

This review exists because small diffs in a few hot paths have caused large regressions. The
reverted #5266 changed two alias names in `pureToSQLQuery.pure`, and joins exploded across unrelated
queries. Its unit tests passed. The review's job is to show what the change *does* to the engine's
output, not only what the diff says.

Three lenses, each with its own skill:

| lens | method | skill |
|---|---|---|
| sql | EMIT corpus plans, base vs head, alias-normalized and clustered | `review-sql-impact` |
| protocol | EMIT corpus PMCD JSON, base vs head, cross-read both directions, `_type` registry | `review-protocol-impact` |
| auth | static checklist over changed identity, credential and connection code | `review-auth` |

The tools are `engine-review` (in this plugin's `bin/`; on PATH after `.claude-plugin/install-engine-review`).

## 1. Pin the revisions

- **PR:**
  ```bash
  gh pr view <n> --json title,body,baseRefName,headRefOid
  git fetch origin pull/<n>/head
  base=$(git merge-base origin/<baseRefName> <headRefOid>)
  ```
  Review `<headRefOid>`, not a local branch that may have drifted.
- **Branch:** the base is `git merge-base origin/master HEAD`, which is the default when `--base`
  is omitted.
- **Uncommitted work:** use `--head WORKTREE`.

## 2. Scope it

```bash
engine-review scope --base <base> --head <head>
```

This prints the lenses with the files that triggered each one, the **blind spots** (for example
"EMIT executes on H2 only"), and the EMIT suites to run. Then read the diff and the PR description,
and write down the author's claimed intent in one sentence. Every later finding is judged against
that sentence.

## 3. Start the dynamic run early (sql or protocol lens)

It is the slow part, so launch it in the background before the static work:

```bash
engine-review run --base <base> --head <head>          # add --suites relational ... to narrow
```

Tell the user the cost before starting:

- The **first** run on a given base commit builds the corpus closure, about 15 minutes. It is
  cached by commit under `~/.cache/legend-engine-review/m2/<sha>`.
- The head rebuilds only the changed modules.
- Each snapshot pass takes about one minute per suite, and there are three passes.
- Nothing is written to `~/.m2`.

Requirements: JDK 17 (found with `/usr/libexec/java_home -v 17`, or set `ENGINE_REVIEW_JAVA_HOME`)
and Maven 3.9 or later, which the chained local repositories need.

The run copies the snapshot hook (`EMITSnapshot` and the JUnit builder) and the corpus **from this
checkout** onto both revisions, so new corpus models measure old commits too. The rest of the EMIT
framework stays each revision's own, because it calls product APIs that change between revisions.
`run.json` records what was copied and rebuilt, plus the caveats.

If a Maven step fails, read the named log under `<run>/logs/` before anything else. A failed
product build means the comparison never happened. Do not review from a partial report.

## 4. Static work while it runs

- auth lens → follow `review-auth`.
- protocol lens → the static checks in `review-protocol-impact`.
- **Transitive reach**, for every lens. For each function or method the diff changes, list its
  callers (`grep -rn '<name>(' --include=*.pure --include=*.java`) and the features they serve. That
  list is your prediction of where the report will show change. A cluster you did not predict is the
  most valuable finding in the review.

## 5. Read the report

Read `<run>/report.md` (the full data is in `report.json`), then follow `review-sql-impact` and
`review-protocol-impact`. Account for every cluster: as intended, as transitive impact (a finding),
or as noise (say why).

## 6. Write the review

Use `references/review-template.md` and save the result as `<run>/review.md`.

- Lead with the verdict and the most severe findings.
- Every finding cites evidence: a report cluster, a diff excerpt, or `file:line`.
- Keep "observed by EMIT" apart from "static judgement".
- Name the coverage gaps. When the corpus missed the changed shape, offer to add a model or probe
  (see `references/sql-review-guide.md`); that keeps the corpus maintained.
- Posting to the PR (`gh pr comment`) is outward-facing, so ask before doing it.
