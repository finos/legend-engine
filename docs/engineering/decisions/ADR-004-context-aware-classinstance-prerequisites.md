# ADR-004: Context-Aware Prerequisites for ClassInstance Islands

**Status:** Accepted
**Date:** 2026-11-17
**Deciders:** legend-core team

---

## Context

### How the Alloy compiler orders work

`PureModel` compiles a `PureModelContextData` in passes (`PureModel.java`, `runPasses`):

1. **First and second passes** run per element *class*, ordered by the class-level DAG that each `Processor` declares
   through `getPrerequisiteClasses()`. After the first pass every element exists, with its name, function signatures
   and handlers registered, and simple attributes such as a service's `pattern` set.
2. The **prerequisite-elements pass** runs once all first and second passes have finished. Each processor returns the
   `PackageableElementPointer`s its element depends on. Lambdas are walked with
   `ValueSpecificationPrerequisiteElementsPassBuilder`.
3. `DependencyManagement.topologicallySortElements` sorts the elements into **dependency levels**, and fails with
   `Detected a circular dependency in element prerequisites graph!` on a cycle.
4. The **milestoning and third passes** run one level at a time. Function bodies, and every `ClassInstance` island
   inside them (for example `#SQL{...}#`), are built in the third pass. So are `Service._execution()` and similar
   cross-element wiring. **Within a level the order isn't stable:** it is `UnifiedMap` iteration order of the paths
   when compiling sequentially, and concurrent when a `ForkJoinPool` is configured.

Anything that reads another element's third-pass output is therefore only safe if that element is at a lower
dependency level, which in practice means it is declared as a prerequisite.

### The gap

Islands contribute prerequisites through `CompilerExtension.getExtraClassInstancePrerequisiteElementsPassProcessors()`,
a `Procedure2<Object, Set<PackageableElementPointer>>` that receives **only the protocol object**. The compiler's
other prerequisite hooks receive a `CompileContext`: class mappings, embedded data, tests, test assertions, service
executions and service tests. Core itself resolves function names through first-pass handlers during this pass
(`CompileContext.collectPrerequisiteElementsFromFunctionName`). An island that references an element by anything
other than its path can't declare it.

The driving case is `#SQL{ select ... from service('/my/pattern') }#`. It references a service by **pattern**, and it
needs `Service._execution()`, which is third-pass state. A service depends on its mapping, runtime and connections,
so it always sits at a deeper level than a function whose only link to it is the island. Without a declared edge the
function is compiled first and finds no execution.

### Function bodies

`FunctionCompilerExtension.functionPrerequisiteElementsPass` removes every `FUNCTION` pointer, so that recursion
compiles. That also removes edges where an island reads another function's **compiled body**. `#SQL{ ... func('f') }#`
derives its columns from the target's body, so it depends on compile order. `PackageableElementPointer` equality is
by **path only**, so a function that both calls `B` and reads `B` through an island holds a single pointer for `B`,
whose type depends on which visitor added it first. Type-based stripping can't tell the two apart.

### Evidence

A throwaway harness compiled two functions, a reader using `#SQL{select b from func('src')}#` and a source, across
12 name pairs, each sequentially and repeatedly with an 8-thread pool:

| Shape | Passed |
|---|---|
| Same level, explicit column | 6 / 48 (sequential only, for name pairs whose hash order happened to put the source first) |
| Source one level deeper | 0 / 48 |
| Reader one level deeper | 48 / 48 |
| `select *` (no column check) | 48 / 48 (empty columns pass through unnoticed) |

The dependency level decides the outcome, and the service shape (source deeper) fails every time.

## Decision

1. **Context-aware island prerequisites.** Add to `CompilerExtension`:

   ```java
   default Map<String, Procedure3<Object, CompileContext, Set<PackageableElementPointer>>> getExtraClassInstancePrerequisiteElementsPassProcessorsWithContext()
   ```

   `CompilerExtensions` aggregates it, and `ValueSpecificationPrerequisiteElementsPassBuilder.visit(ClassInstance)`
   applies it together with the existing context-free map: an island type can have both, and their prerequisites are
   combined.

   **Contract:**
   - it runs in the prerequisite-elements pass;
   - it may read first- and second-pass state only, never third-pass output;
   - it must be thread-safe and read-only;
   - it must not throw. The pass runs outside the per-element error handling, so an exception aborts the whole
     compilation, partial compilation included. It returns nothing for unresolvable references, and the island's third
     pass reports them against the element.

2. **Function bodies keep their rule; `func(...)` is typed by its target's signature.** Function bodies still drop
   every `FUNCTION` pointer. `#SQL func(...)` over a target that declares its columns (`Relation<(...)>`) takes them
   from that signature, as any Pure call does, so it needs no compile order. A target declared `Relation<Any>` still
   reads the target's compiled body and stays order-dependent, exactly as before this change. The inline `~src` lambda
   of a relation class mapping is now walked, keeping only the pointers its islands declare.

3. **First consumer: `#SQL`.** `SQLCompilerExtension` implements the context-aware hook, parses the SQL, and passes
   each table function to its own plug-in SPI through a new opt-in default,
   `TableFunctionCompilerExtension.extractPrerequisiteElements(TableFunction, CompileContext)`. `service(...)` resolves
   its pattern among the model's services (`PureModel.getAllPackageableElementsOfType`); two services with the same
   pattern are a compile error. `tb(...)` declares its store; `func(...)` declares nothing.
   `TableFunctionSimpleElementExtractor` itself stays opt-in, so existing subclasses outside this repository add no
   edges. A context-aware and a context-free hook registered for the same island type are combined. The layering is: core hook → island owner → that island's
   plug-ins.

## Alternatives Considered

| Alternative | Why it was rejected |
|---|---|
| Reference services by path, using the existing context-free hook | Loses parity with `service('/pattern')` in the Legend SQL (Postgres) server |
| Compile the referenced service lazily from inside the island | Needs the protocol element, compiles it twice, races with its own third pass under a `ForkJoinPool`, and bypasses cycle detection |
| Class-level prerequisite (functions depend on services) | Only orders the first and second passes, not the third pass, and inverts every function/service model |
| Change the existing hook's signature to take `CompileContext` | Breaks every extension that implements it |
| Emit a null-typed pointer so it isn't stripped | Depends on visitor order, because pointer equality is by path only |
| Keep island-declared `FUNCTION` pointers in function bodies (strip by provenance) | Needed only for `func(...)` over `Relation<Any>` targets, where only identity projections work anyway. The edges closed cycles through existing coarse edges (function → runtime → mapping → `~func` or mapping tests) in models that compile today |
| Reject `func(...)` targets declared `Relation<Any>` | Breaks `select *` readers of such targets that compile today |

## Consequences

**Positive**
- Deterministic, correct ordering for `#SQL` service sources, in sequential and parallel compilation.
- The island hook is now consistent with the compiler's other prerequisite hooks.
- Reusable by any island that references elements by something other than a path.
- `#SQL func(...)` over a target that declares its columns compiles the same way regardless of names and element
  order.

**Negative**
- One more extension method, and a contract (no third-pass reads, no throwing) that implementers must respect.
- `#SQL` text is parsed once more per island, during the prerequisite pass.

**Behaviour changes**
- `#SQL func(...)` over a target with declared columns is typed by the signature instead of the body: a body column
  such as `Varchar(200)` declared `String` is seen as `String`, as the final reader already was.
- Islands in a relation class mapping's `~src` now add the mapping's prerequisites.
- A context-free island hook keeps working when a context-aware one is registered for the same island type.

**Limitations**
- `func(...)` over a target declared `Relation<Any>` is still order-dependent, as before.
- `service(...)` in the `~src` of a mapping that is the service's own mapping, or is listed in its runtime, is a cycle:
  the service already depends on that mapping.
- Islands in class qualified properties, constraints and association properties get no prerequisites; those elements
  have no prerequisite elements pass.

## Verification

Each ordering suite was also run with the fix reverted, to confirm it fails without it.

| Suite | Module | Covers | Result | With the fix reverted |
|---|---|---|---|---|
| `TestClassInstancePrerequisites` | `legend-engine-language-pure-compiler` | context-aware and context-free hooks combined; an unresolvable island reference is reported by the island, not thrown | 2/2; module suite 533/533 | the combined-hooks test fails with either/or dispatch |
| `TestSQLExpressionCompileOrdering` | `legend-engine-xt-sql-expression-compiler` | `func(...)` sources over targets with declared columns, at the same and at a deeper dependency level, 12 name pairs chosen so both hash orders occur, sequential and `ForkJoinPool` compilation | 48/48 | 42 fail (columns read from the body, as on master) |
| `TestSQLExpressionServiceSource` | `legend-engine-xt-sql-expression-compiler` | `service(...)` sources: Relation, TabularDataSet, parameters, multi-execution, error cases; chains through `func(...)`, two functions sharing a service, a service whose query calls a function reading another service, 16 readers compiled in parallel over 5 rounds, PMCD element order shuffled with 8 seeds, partial compilation, cycles; typing by the signature of a called function; no false cycle through a typed `func(...)` target; opt-in base extractor; nested, `ORDER BY`, simple-`CASE`, `ARRAY` and `~src` sources; `~src` reading its own runtime; multi-execution key binding; argument typing; project arguments | 49/49 | 23 of the first 25 fail (service prerequisites disabled); each later regression test fails with its fix reverted |
| `Test_SQL_Pure` | `legend-engine-xt-sql-pure` | the Pure `service` source, including error messages | 269/269 | n/a |
| `Test_SQLExpression_Pure` | `legend-engine-xt-sql-expression-tests-pure` | plans and generated SQL for `#SQL` service sources, and existing `#SQL` output unchanged with `removeFrom` off | 12/12 | n/a |
| `TestSQLExpressionServiceSourceParity` | `legend-engine-xt-sql-http-api` | the same SQL run through the Postgres SQL endpoint and inside `#SQL{}` against H2 returns the same columns and rows: Relation and TDS services, parameters, a multi-execution key, an enum column, nulls, row-limited services | 15/15 | the row-limited `WHERE` cases fail without the relational router fix |

Not covered: the Testcontainers Postgres e2e suite (`legend-engine-xt-sql-e2e-tests`), which needs Docker.

## References

- `docs/engineering/architecture/alloy-compiler.md` §4 (compilation pipeline) and §9.3 (class-instance syntax)
- `docs/compiler/compiler-extension-processor.md` (processor contract)
