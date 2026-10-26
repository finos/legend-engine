# Impact review: <PR title or branch>

**Verdict:** <ship / ship with follow-ups / needs changes / blocked>. <One sentence on why.>

**Change in one line:** <what the author says it does>. **Observed:** <what the evidence says it does>.

| | |
|---|---|
| base / head | `<base sha>` / `<head sha>` |
| lenses | sql · protocol · auth (only those that applied) |
| corpus | <N> plans, <M> models, suites <list>; head rebuilt <modules> |
| blind spots | <from `engine-review scope`, e.g. H2-only, SQL transpiler not covered> |

## Findings

Most severe first. Each finding has:

- **severity**: blocker, major, minor or note.
- **evidence**: a report cluster or item, a SQL or JSON diff excerpt, or `file:line`.
- **why it matters**: the user-visible consequence, not the mechanism.
- **what to do**: a fix, a test to add, or a question for the author.

### 1. [severity] <title>
<evidence>
<why it matters>
<what to do>

## Transitive impact

Which features or query shapes the change reaches beyond the one it targets, with the corpus items
or callers that show it. State it plainly when nothing beyond the target changed.

## Coverage gaps

Code the diff touches that no corpus item exercised. For each gap, name the shape and say whether you
added an EMIT model or probe, or propose one.

## Evidence

- report: `<run>/report.md`
- SQL clusters reviewed: <n> of <n>
- protocol: backward <ok / failures>, forward <ok / failures>, `_type` changes <n>
- auth checklist sections applied: <A–H>
