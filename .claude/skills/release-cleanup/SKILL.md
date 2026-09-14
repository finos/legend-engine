---
name: release-cleanup
description: Undo a failed or partial Legend release — inspect what the release actually did, then surgically remove the tag and version-bump commits so the version can be cut again. Use for "the release failed, clean it up", "undo the release commits", "delete the tag so I can re-release", "the version bump was unintentional".
---

# Release cleanup

A failed release usually leaves a pushed tag and version-bump commits behind.
Both must go before that version can be cut again — but *what* exactly was left
depends on the repo and on how the release failed, so *inspect before acting*.

Read `.claude/skills/release-preflight/references/repo-matrix.md` first.

## Do not reach for `clean-after-failed-release.yml`

Every repo has it, and it is blunt: it deletes `git describe`'s **globally
newest tag** and runs `git reset --hard HEAD~2 && git push --force`. That is
correct only when the last two commits really are the release-plugin pair and
the newest tag really belongs to this release. It is wrong when a backport
squashed release commits together with a fix (resetting 2 then deletes real
work), when the repo is `legend-shared` (one commit, not two), or when any newer
tag exists. Do it properly instead.

## 1. Establish what actually happened

- **Did anything reach Central?** The decisive question, because Central is
  immutable. Check the coordinate URL (see the matrix). `200` means that version
  is burned permanently — cleanup will not free it, and the user must bump. Also
  check whether the run's publish job ever started (`0s` = never ran).
- **What commits did the release add?** Look for `[maven-release-plugin]` in
  both subjects and bodies — a squash hides them in the body:
  ```bash
  git log --format='%H %s%n%b' -15 | grep -n 'maven-release-plugin'
  ```
- **What does the version diff look like?** Confirm the bump is all that changed:
  ```bash
  git diff HEAD~1 HEAD --stat | tail -3
  git diff HEAD~1 HEAD -- '*pom.xml' | grep -E '^[+-].*<version>' \
    | grep -oE '[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT' | sort | uniq -c
  ```
- **Does the tag exist, locally and on origin?**
  ```bash
  git tag -l '<repo>-<version>'; git ls-remote --tags origin 'refs/tags/<repo>-<version>'
  ```

## 2. Choose the undo shape

**If the release commits are clean and unpushed** — `git reset --hard` to before
them is fine.

**If they are pushed, or share a commit with real work** — do a forward revert.
Restore only the release-plugin artifacts and leave everything else:
```bash
git diff HEAD~1 HEAD --name-only -- '*pom.xml' | xargs git checkout HEAD~1 --
git commit -m "Revert maven-release-plugin version changes from #<pr>"
```
Then confirm the tree differs from the pre-release commit by only the real change:
```bash
git diff HEAD~1 --stat
```
This is the right shape whenever a backport squashed the release commits in with
a fix — it keeps the fix and drops only the version churn.

Remember `legend-shared` has no release-plugin commits at all: its bump is a
`<revision>` edit in one commit.

## 3. Delete the tag — confirm first

```bash
git tag -d <repo>-<version>
git push origin :refs/tags/<repo>-<version>
```
Deleting a published tag is outward-facing: **always confirm**, and check first
whether artifacts for it reached Central. If they did, the tag documents a real
published release and deleting it is probably wrong.

Record the SHAs before deleting, so the tag can be recreated:
```bash
git rev-parse <repo>-<version>          # annotated tag object
git rev-list -n1 <repo>-<version>       # the commit it points at
```
After a squash the tagged commit often exists on no branch at all, so this is
the only record of it.

## 4. Verify

- Root pom back at the intended `<version>-SNAPSHOT`, and no stray new version
  anywhere: `grep -rl '<wrong-version>' --include=pom.xml . | wc -l` should be 0.
- Tag gone locally and on origin.
- Central still `404` for the version.
- Branch pushed, so the next release builds the cleaned state.

Then hand to `release-preflight` to confirm the branch is releasable again.
