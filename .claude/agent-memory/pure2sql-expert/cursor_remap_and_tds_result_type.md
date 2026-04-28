---
name: Cursor remap & TDS result-type bugs (isolateTdsSelect, isResultColumnsDynamic)
description: Two-bug cluster in pure2sql — cursor remap crash in isolateTdsSelect and empty-tdsColumns NPE downstream — triggered by pivot-after-concatenate and other post-isolation reshapes
type: project
---

Session on branch `pct-rebasing`, dated 2026-04-22. Canonical reproducer: `testStaticPivot_AfterConcatenate` in `composition.pure`.

## Bug 1: "Found 0 nodes" in `isolateTdsSelect`  — FIXED this session

**File:** `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/pureToSQLQuery/pureToSQLQuery.pure`

**Location:** `isolateTdsSelect` 4-param overload, approx lines 3052-3068.

**Before (crashes):**
```pure
currentTreeNode = if($query.currentTreeNode->isEmpty(),| $query.currentTreeNode,| $query.currentTreeNode->toOne()->findOneNode($select.data->toOne(), $newNode))
```

**After (falls back to new outer root):**
```pure
currentTreeNode = if($query.currentTreeNode->isEmpty(),
                     | $query.currentTreeNode,
                     | let remapped = $query.currentTreeNode->toOne()->findNode($select.data->toOne(), $newNode);
                       if($remapped->size() == 1, | $remapped->toOne(), | $newNode);)
```

**Why the fix works:** `findOneNode` asserts `size == 1` (crashes with "Found 0 nodes") when the cursor can't be remapped into the newly-isolated tree. After isolation, everything below is buried inside a `subselect`, so the outer-query cursor position can only be the new root. Falling back to `$newNode` is semantically correct.

**Type gotcha:** `findNode` returns `RelationalTreeNode[*]`, NOT `[0..1]`, so `->orElse($newNode)` does not typecheck. Must use the explicit size-check pattern above. An IDE edit that used `->orElse` silently failed the Maven PAR build — the failure was hidden under a `2>&1 | tail -N` pipe (see build_discipline).

## Bug 2: Empty `tdsColumns` → NPE in `RelationalResult.buildTransformersAndBuilder` — NOT fixed this session

After bug 1 unblocks plan generation, the same test hits `java.lang.NullPointerException` in `RelationalResult.buildTransformersAndBuilder` at `ExecutionNodeTDSResultHelper.getTDSColumn` (around line 40). The `TDSResultType` has zero/null tdsColumns.

### Root-cause chain (all Pure-side, with file references)

1. `processStaticPivot` (in `pureToSQLQuery.pure` ~line 6386) ends with `->isolateNonTerminalGroupByQueryWithEmptyGroupingColumns(...)`, wrapping the pivot result in a fresh `RootJoinTreeNode` via `isolateTdsSelect`.
2. After isolation, the outer `SelectSQLQuery` no longer has `pivot` set — the pivot now lives inside the subselect's `TableAlias.relationalElement`.
3. `isResultColumnsDynamic(SelectSQLQuery)` at `pureToSQLQuery.pure` ~line 6328 is defined as `!$select.pivot->isEmpty() || $select.columns->isEmpty()`. Only inspects the TOP-LEVEL select, so after isolation returns **false**.
4. `generateSQLExecutionNode` in `relationalMappingExecution.pure` ~line 226 sets `isResultColumnsDynamic = []` (→ null in Java) based on that false.
5. For `$a->concatenate($b)->pivot(...)` without a `->cast(...)` pinning the types, the expression's Pure genericType is effectively `Relation<?>`. In `generateInstantiationExecutionNode` ~line 127 the path-matching condition fails; falls through to line 132 which maps over `.columns` → **empty tdsColumns**.
6. Java side: `RelationalResult.buildTransformersAndBuilder` ~line 248 only rebuilds tdsColumns from JDBC metadata when `sqlExecutionNode.isResultColumnsDynamic` is explicitly true. Null (step 4), so the empty tdsColumns stays → `getTDSColumn` NPEs.

### Two candidate fixes

**(a) Pure-side (recommended root-cause fix)** — update `isResultColumnsDynamic` at `pureToSQLQuery.pure:6326-6329` to detect pivot inside nested subselects:
```pure
!$select.pivot->isEmpty() ||
$select.columns->isEmpty() ||
($select.data->isNotEmpty() &&
 $select.data.alias.relationalElement->instanceOf(SelectSQLQuery) &&
 $select.data.alias.relationalElement->cast(@SelectSQLQuery)->isResultColumnsDynamic())
```
Restores the flag to true post-isolation so the existing JDBC-metadata rebuild path triggers.

**(b) Java-side (defensive)** — in `RelationalResult.java` change the guard at approx line 247 from:
```java
if (sqlExecutionNode.isResultColumnsDynamic != null && sqlExecutionNode.isResultColumnsDynamic)
```
to:
```java
if (tdsResultType.tdsColumns == null || BooleanUtils.isTrue(sqlExecutionNode.isResultColumnsDynamic))
```
Requires `import org.apache.commons.lang3.BooleanUtils;`.
**Caveat:** this only catches **null**. If Pure emits `[]` (empty list) instead of null, this still won't fire — verify the actual runtime value before relying on it.

## Bug 3: Latent NPE in SQLExecutionResult.java

**File:** `legend-engine-xts-relationalStore/.../SQLExecutionResult.java` ~line 96:
```java
if (this.SQLExecutionNode.isResultColumnsDynamic != null && this.SQLExecutionNode.isResultColumnsDynamic)
```
The **null guard is load-bearing**. Removing it regressed 327 DuckDB PCT tests this session with NPE on auto-unboxing. Pure emits `[]` when `isResultColumnsDynamic` is false, which maps to null in Java. Do NOT remove without first ensuring Pure always emits non-null here.

## `#TDS#` inline vs let-CTE matters for reproduction

- **Inline** `#TDS#->concatenate(#TDS#)->pivot(...)` fails at post-processor validation with `NODE VALIDATION ERROR: currentTreeNode root DOESN'T CONTAIN root` — a **different** code path from the isolateTdsSelect fix.
- **Let-CTE** `let a = #TDS#; let b = #TDS#; $a->concatenate($b)->pivot(...)` hits "Found 0 nodes" in `findOneNode` via the `isolateTdsSelect` path — this is the bug the fix addresses.

**Why the CTE form matters:** `processFunctionDefinition` turns CTEs into `CommonTableExpression`s in scope. Resolving `$a` / `$b` produces a `SelectWithCursor` with non-empty `select.data`. When this flows into `processConcatenate`, the cursor state ends up with a `root[]` node that is a different object instance than `$select.data`'s `root[]` — they print identically but are not `==`. `findNode`'s `oldTarget == oldNode` check uses **reference equality**, so it fails; falls to `findNodeByChild`, which returns `[]` because neither root has children.

## Future investigators: symptom → bug map

| Symptom | Likely bug |
|---------|-----------|
| `Found 0 nodes` (or `Found N nodes, expected 1`) inside `findOneNode` called from `isolateTdsSelect` | Bug 1 — cursor cannot be remapped after tree isolation, needs `$newNode` fallback |
| `NullPointerException` in `ExecutionNodeTDSResultHelper.getTDSColumn` at plan execution, pivot/unpivot/nested-aggregate in the query | Bug 2 — `isResultColumnsDynamic` false-negative after post-isolation |
| 300+ DuckDB PCTs regressing with NPE on `boolean isResultColumnsDynamic` auto-unbox | Bug 3 — someone removed the null guard in `SQLExecutionResult.java:96` |
| `NODE VALIDATION ERROR: currentTreeNode root DOESN'T CONTAIN root` | Different bug (inline `#TDS#` form); not bug 1 |
