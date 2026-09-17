---
name: release-preflight
description: Verify a Legend repo is actually releasable before dispatching a release — tag free, version unpublished on Central, branch clean and pushed, pom version correct, no stray release-plugin commits. Use before any release dispatch, or when asked "can I release X", "is the branch ready", "check before I cut 4.143.1".
---

# Release preflight

A Legend release costs 1–3 hours of CI. Every check here is cheap and catches a
failure that would otherwise surface after that wait — or worse, after the tag
has been pushed.

Read `.claude/skills/release-preflight/references/repo-matrix.md` first. The five
repos do not share a release mechanism, and three of the five have **no
pre-flight gate in CI at all**, so this skill is the only thing standing between
them and a burned version number.

## Inputs

Version to release, and the branch to release from. If the user gave only a
version, use the current branch. Derive the repo from `git remote get-url origin`.

## Checks

Run these and report a go/no-go list. Read-only — nothing here mutates anything.

1. **Working tree clean** — `git status --porcelain`. Anything uncommitted will
   not be in the release.
2. **Branch pushed and in sync** — `git status -sb`. Ahead means the release
   builds something other than what you are looking at; behind means you are
   about to release without someone else's commits.
3. **Tag free, locally and on origin** — the tag is `<repo-name>-<version>`:
   ```bash
   git tag -l "<repo>-<version>"
   git ls-remote --tags origin "refs/tags/<repo>-<version>"
   ```
   Either one existing means this version was already attempted. Do not delete it
   here — that is `release-cleanup`'s job, and the tag may be a real past release.
4. **Not already on Central** — immutable, so a hit is terminal for that version:
   ```bash
   curl -s -o /dev/null -w '%{http_code}' \
     "https://repo1.maven.org/maven2/<group path>/<artifact>/<version>/<artifact>-<version>.pom"
   ```
   `404` = free. `200` = that version number is burned; the only move is to bump.
5. **Root pom version matches** — the branch should be at `<version>-SNAPSHOT`
   for the version being released. A mismatch usually means a previous release
   partially ran and bumped the branch (e.g. sitting at `4.143.2-SNAPSHOT` when
   you mean to cut `4.143.1`). For `legend-shared`, read `<revision>` instead.
6. **No stray release-plugin commits** — scan recent history for
   `[maven-release-plugin]`, including **inside squashed commit bodies**:
   ```bash
   git log --format='%H %s%n%b' -15 | grep -n 'maven-release-plugin'
   ```
   A squashed backport can hide them in its body while the version bump is real.
   That is what broke the 4.143.1 release.
7. **Version format** — `MAJOR.MINOR.PATCH`, no `-SNAPSHOT`. Every repo's
   workflow rejects anything else.
8. **Workflow exists on the target ref** — `workflow_dispatch` can only dispatch
   a workflow file present on that branch: `git ls-tree --name-only <ref> .github/workflows/`.
9. **Last release attempt on this branch** — the highest-value check:
   ```bash
   gh run list --workflow=release.yml -L 10
   ```
   If the most recent run on this branch failed, say why before the user spends
   the time again. Hand off to `release-triage` for the diagnosis.
10. **`legend-pure` only** — its workflow rejects a `releaseVersion` below the
    branch's current version. Check that ordering explicitly.

## Output

A short checklist, each line pass/fail with the observed value. Then a verdict:
clear to release, or the specific blocking items. If something blocks, name the
skill that fixes it (`release-cleanup` for a stale tag or version drift) rather
than fixing it here.

Never dispatch from this skill. Preflight reports; `release-dispatch` acts.
