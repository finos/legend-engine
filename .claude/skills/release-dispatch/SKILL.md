---
name: release-dispatch
description: Dispatch a Legend release workflow and babysit it to completion — picks the right workflow, runs preflight, confirms, triggers, then watches and triages any failure. Use for "release 4.143.1", "cut a release off this branch", "run the release action", "kick off the release".
---

# Release dispatch

Dispatches a release and stays with it. The point is that the user does not have
to watch a 1–3 hour pipeline or discover a failure an hour after it happened.

Read `.claude/skills/release-preflight/references/repo-matrix.md` before anything.

## 1. Establish the target

Repo from `git remote get-url origin`, branch and version from the user. If they
gave a version but no branch, use the current branch and say which one you are using.

**Pick the workflow from the matrix, not from the file listing.** `release.yml`
is correct for all five repos. Do not use `release-large-runner.yml` (dead since
February 2026) or `legend-stack-release.yml` (stale, `repository_dispatch`-driven).

Verify with actual history rather than assumption:
```bash
gh run list --workflow=release.yml -L 5
```

## 2. Preflight

Run the `release-preflight` skill's checks in full. If anything blocks, stop and
report — do not dispatch "to see what happens", because a failed release after
`release:prepare` leaves a pushed tag and version commits to clean up.

## 3. Confirm before dispatching

Dispatching publishes to Maven Central, which is **immutable**. Always confirm,
stating plainly:

- repo, branch, version, workflow
- whether it is a real publish or a dry run
- the expected duration from recent runs of that workflow

**The dry-run trap** (`legend-pure` and `legend-engine` only): `dryRun: true`
skips only the *publish* steps. `release:prepare` still runs, so it still pushes
the tag and the version-bump commits. A dry run followed by a real run therefore
needs a full `release-cleanup` in between. Say this whenever the user asks for a
dry run — most people expect it to be side-effect free, and it is not.

## 4. Dispatch

```bash
gh workflow run release.yml --ref <branch> -f releaseVersion=<version>
```
Add `-f dryRun=true` only where the input exists and the user asked for it.
`gh workflow run` prints nothing on success, so confirm it actually started:
```bash
gh run list --workflow=release.yml -L 3
```
Report the run ID and URL.

## 5. Babysit

Poll rather than blocking on `gh run watch`, which holds the terminal for hours:
```bash
gh run view <run-id>
```
Sensible cadence is every few minutes early on, then spaced out — the build job
alone is over an hour on legend-engine. For a long unattended watch, the `loop`
skill fits well.

Report meaningful transitions only: the build job finishing (this is the point
the tag has been pushed and the branch bumped), the test matrix starting, the
publish job starting. Do not narrate every poll.

## 6. On failure

Hand straight to `release-triage`. Do not guess at a cause from the job name —
a single failed job routinely mixes a real failure with an environmental one.
Report the verdict and the recommended action; do not rerun or "fix" anything
without asking.

## 7. On success

Hand to `post-release-verify`.
