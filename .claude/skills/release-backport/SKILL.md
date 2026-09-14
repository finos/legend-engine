---
name: release-backport
description: Backport a fix to a Legend release branch safely, without dragging maven-release-plugin commits along with it. Use for "backport this to 4.100", "cherry-pick that fix onto the release branch", "get this fix into the release".
---

# Release backport

Getting a fix onto a release branch. The hazard this skill exists for: a
backport that also carries the release-plugin commits, which silently bumps the
whole branch's version and breaks the next release.

That is exactly what happened to legend-engine 4.143.1 — PR #5210 squashed
`prepare release 4.143.1`, `prepare for next development iteration`, and the
actual fix into one commit, moving 602 poms to `4.143.2-SNAPSHOT` as a side
effect. The fix was two lines of JSON.

Read `.claude/skills/release-preflight/references/repo-matrix.md` for branch and
label conventions.

## Preferred route: the label

`create-release-branch.yml` creates a label `backport release-legend-release-<stack-version>`
for each release branch. Applying it to a **merged master PR** opens the backport
PR automatically via `backport.yml`:
```bash
gh pr edit <pr> --add-label "backport release-legend-release-<stack-version>"
```
Use this when the fix is a merged master PR. Note the known limitation in
`backport.yml`: adding a second backport label later re-evaluates *all* labels
and posts a spurious failure comment for the already-completed one. Expected, not a bug.

## Manual route: cherry-pick

When the label route does not apply (fix not on master, or the automation failed):

1. **Identify exactly the commits to carry.** Usually one. Inspect before picking:
   ```bash
   git log --format='%H %s%n%b' -5 <source-ref>
   ```
2. **Refuse to carry release-plugin commits.** Before cherry-picking, check both
   subject and body for `[maven-release-plugin]`. If a candidate commit is a
   squash containing them, do **not** pick it whole. Pick the fix alone — either
   from the original unsquashed commit, or by applying just the non-pom paths:
   ```bash
   git show <sha> --stat | grep -v 'pom.xml$'
   ```
3. **Cherry-pick and verify the diff is only the fix:**
   ```bash
   git cherry-pick -x <sha>
   git diff HEAD~1 HEAD --stat | tail -3
   ```
   If any `pom.xml` version lines moved, stop — the backport has picked up a
   version bump and must be redone.
4. **Confirm the branch version is untouched.** The release branch must still sit
   at the `-SNAPSHOT` it had before. For `legend-shared`, check `<revision>`.

## After backporting

The release branch has changed, so anything already built from it is stale. Run
`release-preflight` before dispatching a release from the branch.

If a backport has already landed with release-plugin commits in it, that is
`release-cleanup`'s job — it can strip the version churn and keep the fix.
