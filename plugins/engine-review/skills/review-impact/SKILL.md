---
name: review-impact
description: "Targeted impact review of a legend-engine change, or of a change to any repository carrying an EMIT corpus (PR number, branch, commit range, or uncommitted work) for reviewers and authors: classifies the diff into hot-area lenses (SQL generation, protocol JSON, auth/JDBC connectivity), runs the EMIT corpus on base and head to show how generated SQL and protocol JSON actually change, applies the auth checklist statically, and writes a review that separates intended change from transitive impact and names coverage gaps. Use when asked to review a legend-engine PR or change, to assess its impact, blast radius or regression risk, or whether a small change to pureToSQLQuery/router/protocol/auth code is safe."
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
- **Branch:** the base is the merge-base of HEAD with the default branch (`origin/HEAD`, else
  `master` or `main`), which is the default when `--base`
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

Requirements: JDK 17 (set `ENGINE_REVIEW_JAVA_HOME`; on macOS `/usr/libexec/java_home -v 17` is
used when it is unset) and Maven 3.9 or later, which the chained local repositories need.

The run copies the snapshot hook (`EMITSnapshot` and the JUnit builder) and the corpus **from this
checkout** onto both revisions, so new corpus models measure old commits too. The rest of the EMIT
framework stays each revision's own, because it calls product APIs that change between revisions.
`run.json` records what was copied and rebuilt, plus the caveats.

A change to a `pom.xml` rebuilds every corpus-closure module that inherits from or depends on it,
even under `--rebuild changed`, so a dependency bump never leaves head mixing two dependency sets.

**Resuming.** Builds are cached by commit. Each snapshot pass writes a marker recording its inputs
and progress, and the hook writes `complete.json` last in every model directory.

- Rerun with the same `--out` and a completed pass is skipped.
- An interrupted pass reruns only the runner classes whose models did not all complete, selected with
  `surefire.includesFile`.
- A runner class whose fork died (OOM, "forked VM terminated") is retried once. If it still does not
  finish, the run stops rather than report missing models as engine differences. Raise the fork heap
  in `snapshot_args`, then rerun with the same `--out`.

Before any build, the run warns when `--out` or the cache is on `/tmp`, `/var/tmp` or tmpfs, which
are wiped on restart. It stops when the disk cannot hold *models × passes × `snapshot_mb_per_model`*
(default 1.5 MB). It also warns when the forks plus the Maven JVM would exceed the memory available,
counting about 1.3 GB of overhead per fork over its heap, or about 2.4 GB without
`-Djacoco.skip=true`. A lock file in `--out` keeps two runs out of the same directory.

On both revisions it also reads the root's effective pom (`help:effective-pom -N`, which takes
seconds and builds nothing). It then stops when a managed external pin used directly by a corpus-closure
module is in no local repository and every repository in `~/.m2/settings.xml` answers 404 or cannot be
reached. That typically means an internal artifact published only where this machine cannot reach.
Transitive dependencies are not checked.

### Other repositories with an EMIT corpus

Pass `--repo <checkout>` to review any Maven repository whose corpus modules depend on
`legend-engine-emit-junit`. Suites are discovered: every module with that dependency and
`*EMITTests` classes, run with the `*EMITTests` pattern. `scope` lists them.

Lenses come from `<repo>/.engine-review/lenses.json` (or `--config`), with the same `ignore`,
`rules`, `blind_spots` and optional `suites` keys as this plugin's `config/lenses.json`. A `suites`
entry names a discovered module and may set `lenses`, `tests` or `note`. Without a config file,
suites are still discovered but no lens fires, so `scope` selects nothing. Extra keys:

| key | default | meaning |
|---|---|---|
| `engine_version_property` | `legend.engine.version` | root-pom property pinning legend-engine |
| `java` | `17` | JDK for the repo's builds, from `ENGINE_REVIEW_JAVA_HOME_<java>` |
| `snapshot_args` | none | extra Maven arguments for the snapshot passes, e.g. `-DforkCount=4`, `-DargLine=-Xmx6g`, `-Djacoco.skip=true -Djacoco.argLine=` |
| `snapshot_mb_per_model` | `1.5` | disk estimate per model per pass, for the preflight |
| `preflight_dependencies` | `true` | check the closure's direct external pins resolve before building |

Such a repo uses the framework from the legend-engine release it pins. If that release's jar has
no snapshot hook, each revision gets the release's own framework source plus the hook from this
plugin's checkout (or `--engine-repo`). That build runs at the release's version and goes into the
revision's private local repository, where it shadows the released jar. The release commit is read
from `legendExecutionVersion.json` in its `legend-engine-shared-core` jar and fetched if missing.
When base and head pin different engine releases, `run.json` says so. That range needs its own
review in legend-engine, because the downstream corpus sees the engine only through its own models.

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
