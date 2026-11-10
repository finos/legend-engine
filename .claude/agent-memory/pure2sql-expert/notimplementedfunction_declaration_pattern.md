---
name: notimplementedfunction-declaration-pattern
description: How to declare a Pure function that is only lowered to SQL / only valid in a context (mapping, graphFetch) and never executed in-memory — use Pure-bodied <<functionType.NotImplementedFunction>>, NOT native
metadata:
  type: reference
---

**Q: how do you declare a Pure function that types/compiles but is only ever lowered by a store's plan generator (SQL pushdown / graphFetch), never executed in-memory?**

**A: a Pure-bodied `function` carrying `<<functionType.NotImplementedFunction>>`. NOT a `native function`.**

**Precedents:**
- `meta::pure::graphFetch::execution::graphFetch` / `graphFetchChecked` / `graphFetchUnexpanded` / `graphFetchCheckedUnexpanded` — `core/pure/graphFetch/graphFetch.pure`. Body = `fail('Not implemented!'); $collection;` (or a `Checked` wrapper).
- `meta::pure::mapping::from` / `withMapping` / `withChainedMappings` — `core/pure/mapping/mappingExtension.pure`. Body = identity (`$t`).
- Also `meta::pure::mutation::save` (mutation.pure), `meta::pure::dataQuality::checked` (dataQuality.pure), the binding functions in `core/pure/binding/functions/functions.pure`, and the calendar functions in `core/pure/corefunctions/dateExtension.pure`.

**Why this form (vs `native function`):**
- The `meta::pure::profiles::functionType` profile (stereotype `NotImplementedFunction`) comes from legend-pure `platform`, which every core module depends on.
- Pre-evaluation skips any call whose function has `NotImplementedFunction`: `router/preeval/preeval.pure` computes `canPrevalFunc` as false for `SideEffectFunction` / `NotImplementedFunction`. So the trivial body never runs during preeval; the store lowering intercepts the call.
- A `native function` without a registered Java native implementation risks a compiled-runtime failure (join/select/extend have implementations under `legend-engine-pure-runtime-java-extension-compiled-functions-relation/.../compiled/natives/`). No `native function <<functionType.NotImplementedFunction>>` exists in the repo — the two are alternatives, not combined.
- The `fail(...)` in the body is a loud, actionable error if any code path does run it in memory.

**Contrast — declare `native function <<PCT.function>>` (with a Java implementation) when** the function executes in memory and needs cross-store parity (join/select/extend relation operators). `<<PCT.function>>` signals "testable in memory across stores"; its absence signals SQL-only.

**Relation mapping joins:** `meta::pure::functions::relation::navigationJoin` / `filterJoin` (and the `JoinKind` enum) live in legend-pure, `legend-pure-m3-core` `platform/pure/grammar/functions/meta/type/relation/relationJoin.pure`, and follow this pattern: a `'''` docstring, `function <<functionType.NotImplementedFunction>>`, body `fail('...'); $target` (navigationJoin, returns `Relation<R>`) / `$rel` (filterJoin, returns `Relation<L>`). `modelJoin` is declared there too but without the stereotype: it is recognised structurally by the mapping compilers and never matched.
