---
name: trim-comments
description: "Reviews every comment and Javadoc added by the current branch (vs. its target/default branch) and trims them to intent-only: delete anything that narrates what the code does or rationalises why it was written, keep only the genuine surprise a reader would otherwise 'fix'. Use when asked to 'trim comments', 'clean up comments before pushing', 'make comments intent-focused', or before raising an MR."
---

# Trim comments to intent-only

Scoped to one branch's diff — not a whole-file or whole-repo comment sweep.

## The bar

**Naming is the documentation.** A function name and its body should read better than any comment
you could put above them; if they don't, rename or restructure rather than explain. Do not narrate
what the code does, and do not rationalise why you wrote it — both restate the code, and both go
stale the moment it changes.

A comment earns its place only when the code is genuinely ambiguous, or when it looks like it
contradicts the status quo and a reader would otherwise "fix" it:

- a spec or dialect quirk being worked around,
- a deliberate deviation from the surrounding pattern,
- an ordering constraint that isn't visible locally,
- a link to an issue.

**Write the surprise, not the summary.**

**Tests are not an exception — they are the clearest case.** A test's name and its assertions
already state the intended behaviour; a comment above them almost always repeats it. Name the test
so the comment becomes unnecessary. The only test comment worth keeping is one explaining why an
assertion that looks wrong is correct.

**Match the surrounding file's comment density.** Most functions in this codebase have none.

## 1. Determine scope

Target branch, in order of preference:

- `args`, if the caller passed one explicitly.
- The branch named in the most recent `push-mr.sh ... --target <branch>` invocation this session, if
  visible in context.
- Otherwise auto-detect: `git symbolic-ref refs/remotes/origin/HEAD --short 2>/dev/null` (strip the
  `origin/` prefix), falling back to the first of `finos-master`, `master`, `main` that exists as
  `origin/<name>`.

Diff range: `origin/<target>...HEAD` — a merge-base diff, not a two-dot diff, since the branch may
be rebased ahead of a stale local tracking ref.

## 2. Review, don't rewrite from scratch

For every file in that diff, look only at **added** comment and Javadoc lines (lines starting `+`
for a `//`, `/* */`, `/** */`, `#`, or language equivalent). Pre-existing comments the branch didn't
touch are out of scope.

Apply the bar above to each one. Concretely:

- **Delete** comments that narrate what the following line or block does — the identifiers should
  already say that. If they don't, rename instead of commenting.
- **Delete** comments that explain history or the current change: "previously X, now Y", "this used
  to be hardcoded", "added for the Z flow", benchmark numbers quoted only to justify a default. Git
  already has the history, and the narrative is wrong as soon as the next change lands.
- **Delete** comments that rationalise the author's reasoning rather than warn the reader. "We do
  this because it's cleaner" tells the next person nothing they can act on.
- **Collapse** multi-paragraph Javadoc to 1–3 sentences: the contract, plus the one non-obvious
  constraint. Keep `@param`/`@return`/`@throws` that document a real public API contract, but
  tighten the text.
- **Keep**, tightened to as few clauses as possible, a comment that names a real constraint a reader
  could not derive locally — why a lock-free path is required, why a flag must be rechecked after
  registering, why a specific ordering prevents a race. If a reviewer would plausibly flag the code
  as wrong without the explanation, the explanation earns its place.
- **In tests**, prefer renaming the test over keeping the comment. Delete comments restating the
  arrangement, the action, or the assertion.

Do not touch:

- License headers.
- Comments outside the diff's added lines.
- Any code or logic — this pass is comments and Javadoc only.

## 3. Verify and commit

1. Diff this pass's changes against the pre-edit state and confirm every changed line is a
   comment/Javadoc/whitespace change — no logic diffs. Spot-check a sample.
2. Commit the trims as their own commit (don't amend an already-pushed commit), e.g.
   `git commit -m "Trim comments to intent-only"`. If nothing needed trimming, there is nothing to
   commit — say so and stop.
