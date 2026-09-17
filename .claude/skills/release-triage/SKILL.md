---
name: release-triage
description: Diagnose a failed Legend release or CI run — pull the failed jobs, separate real code failures from known environmental/flaky ones, and recommend an action. Use for "why did the release fail", "what broke", "triage this run", or any failed release/build run.
---

# Release triage

Separates *the branch is broken* from *the shared infrastructure is broken*. The
distinction matters: one needs a code fix and a re-release, the other needs an
escalation and nothing else.

A single failed job routinely contains both. In legend-engine run 34857019489 the
databricks job held one real PCT manifest failure and thirteen environmental
Databricks failures; acting on the job name alone would have got it wrong.

## 1. Find the run

Given a run ID, use it. Otherwise:
```bash
gh run list --workflow=release.yml -L 10
```
Pick the failed run on the branch in question and confirm with the user if ambiguous.

## 2. Map the failure

```bash
gh run view <run-id>
```
This gives the job list with pass/fail. Note which jobs failed and, importantly,
which jobs **never started** — a publish job showing `0s` means nothing reached
Central, which decides whether the version number is still usable.

## 3. Get the detail

```bash
gh run view <run-id> --log-failed --job <job-id>
```
Logs are enormous. Do not read them whole. Extract the per-class tally and the
individual errored tests:
```bash
gh run view <run-id> --log-failed --job <job-id> 2>&1 \
  | sed 's/.*[0-9]Z //' | grep -E 'Tests run:.*(Failures: [1-9]|Errors: [1-9])|<<< (ERROR|FAILURE)' | head -40
```
Count distinct test names, not surefire's tally — PCT tests are frequently
counted twice, so "26 errors" can mean 13 tests.

## 4. Classify

Check every distinct failure against
`.claude/skills/release-triage/references/known-failures.md`. For each, give a
verdict:

- **Environmental** — shared infrastructure, credentials, stale cloud-store
  state. No code fix exists. A rerun will fail identically until someone fixes
  the environment.
- **Real** — the branch genuinely broke. Needs a fix, a backport, and a re-release.
- **Unknown** — not in the reference. Say so plainly rather than forcing it into
  a category, and investigate from the stack trace.

Two rules of thumb. Failures in default-profile stores (H2, DuckDB, Java
binding, core, server) are almost always **real** — they run on every PR.
Failures in cloud stores (Snowflake, BigQuery, Databricks, Spanner, MemSQL)
under `pct-cloud-test` are often **environmental**, but a PCT expected-error
mismatch there is real and needs a manifest update.

## 5. Report and recommend

State, per failure: what failed, the verdict, and the recommended action. Then
the overall call — is this branch releasable after a rerun, or does it need a fix?

**Report; do not act.** Do not rerun jobs, edit manifests, or modify test code
without the user asking. An auto-rerun of an environmental failure just burns
another hour to fail the same way.

## 6. Record what you learned

If the failure was not already in `known-failures.md`, add it — signature,
verdict, action, and the run ID where it was first seen. This file is the whole
value of the skill; a triage that does not feed it teaches the next one nothing.
