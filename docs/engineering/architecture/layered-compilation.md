# Layered Compilation

> **Related docs:**
> [Alloy Compiler](alloy-compiler.md) | [Key Java Areas](key-java-areas.md) |
> [Architecture Overview](overview.md)

---

## 1. What It Is

A `PureModel` compiled with another graph's `Metadata` is a **layer** on that graph. The other
graph is the **baseline**:

```java
PureModel layer = Compiler.compile(data, DeploymentMode.PROD, user, "", baselineMetadata);
```

The layer compiles only the elements in `data`, and sees the baseline as if it were part of the
same model. The baseline is never written to. Layers nest: a layer's own `Metadata`
(`layer.getExecutionSupport().getMetadata()`) is a valid baseline for the next one.

Every compile that is given a baseline is a layer; there is no opt-in. A compile without one
(`metaData == null`) is unaffected, except that an association to a platform class now stays in the
model (see the reverse-reference rule in §2) instead of being written into the platform class,
which every model in the JVM shares.

The point is to compile only what is new. Callers that compile generated Pure on top of a loaded
model - `meta::legend::compile` from Pure, the SQL `tb()` source, the PCT adapters - used to print
the parts of the model they depended on back to grammar and compile them again, on every call.
On the SQL flows that cost grew with the size of the database a query reads; see
[§7](#7-performance).

---

## 2. Rules

| Rule | Behaviour |
|---|---|
| The baseline is read-only | New elements go to the layer. Baseline packages are only reached through overlays (`LayeredPackage`), so nothing can add children to them, and removing a baseline child throws. |
| The layer sees everything | Baseline elements resolve by path and through package children; baseline mappings, stores, runtimes and connections resolve like the layer's own. |
| No redefinition | Defining an element whose path exists in the baseline is a compile error: `An element named 'X' already exists in the package 'p'`. Callers that print dependencies must skip what is already in the graph (§5). |
| Calls resolve against the baseline | A call to a baseline function compiles against its declared signature (`BaselineFunctionHandler`), the way calls to the model's own functions do. Packages that hold platform handlers stay under the handler table, so a platform function the compiler deliberately does not support still fails. |
| Reverse references stay in the layer | An association the layer defines on a class it does not own - a baseline or platform class - is kept in the layer's side index, not in the class; the compiler reads it through `PureModel.getPropertiesFromAssociations` and `getQualifiedPropertiesFromAssociations`, which union the class with every enclosing layer. A layer subclass of a type it does not own is not recorded in that type's `specializations`. |
| No package prefix | A package prefix cannot be combined with a baseline. |

---

## 3. Where It Lives

| Artefact | Path / class |
|---|---|
| Layer root and metadata | `PureModel` (constructor), `toPureGraph/LayeredPackage.java`, `compiler/LayeredMetadata.java` |
| Calls into the baseline | `toPureGraph/handlers/BaselineFunctionHandler.java`, `PureModel.getBaselineFunctionHandlers` |
| Reverse-reference writes | `PureModel.addPropertyFromAssociation`, `addQualifiedPropertyFromAssociation`, `addSpecialization` |
| Compiled natives | `legend-engine-pure-runtime-java-extension-compiled-functions-legendCompiler`: `LegendCompileWithFunction`, `LayerExecutionSupport` |
| Interpreted natives | `legend-engine-pure-runtime-java-extension-interpreted-functions-legendCompiler`: `LegendCompileWithFunction`, `LegendCompileMixedProcessorSupport` |
| Pure helpers | `core_external_compiler/compiler.pure` (natives and their tests), `core/legend/tools/devUtils.pure` (`meta::legend::isInGraph`) |
| Java tests | `TestLayeredPackage`, `TestLayeredPureModel`, `TestPureModelReverseReferences` (compiler module) |
| Pure tests | `meta::legend::tests` in `compiler.pure`, run by `TestLegendCompileFunctions` in the compiled and interpreted modules |

---

## 4. From Pure: `meta::legend::compile`

The compile natives use the calling graph as the baseline, so the code they compile is a layer on
it:

```pure
native function meta::legend::compile(s:String[1]):PackageableElement[*];
native function meta::legend::compile<T|m>(grammar:String[1], f:Function<{->T[m]}>[1]):T[m];
native function meta::legend::compileVS(s:String[1]):Any[1];
native function meta::legend::compilePMCD(pmcdJSON:String[1]):PackageableElement[*];
native function meta::legend::compilePMCD<T|m>(pmcdJSON:String[1], f:Function<{->T[m]}>[1]):T[m];
```

`compile` and `compilePMCD` without `f` return the elements they compiled. The forms taking `f` run `f` with the
layer visible and return what it returns: inside `f`, and in every function it calls, the layer's
elements resolve by path; once `f` returns, or fails, the calling graph is as it was.

```pure
assertEmpty('a::A'->lenientPathToElement());
assertEquals('A', meta::legend::compile('Class a::A{}', {| 'a::A'->pathToElement().name->toOne()}));
assertEmpty('a::A'->lenientPathToElement());
```

A compile inside `f` layers on the enclosing layer, so it can extend the elements `f` sees.

---

## 5. Compiling Only What Is New

Because the layer already sees the calling graph, generated code should reference the graph's
elements rather than redefine them - and must, since redefinition is an error.
`meta::legend::isInGraph(element)` tells whether an element is part of the calling graph. The SQL
`tb()` source, for example, prints only the database dependencies that are not:

```pure
let databaseStr = $store
                  ->scanDependencies(...)
                  ->removeDuplicates({a,b|$a->elementToPath() == $b->elementToPath()})
                  ->filter(x|!$x->meta::legend::isInGraph())
                  ->map(x|$x->printPackageableElements(...))
                  ->joinStrings('\n');
```

For a database already in the model this prints nothing, and the compile reduces to the accessor
function itself.

---

## 6. How the Runtimes Run `f`

**Compiled.** Generated code reads elements through the execution support it is given on every
call. The native compiles the layer, then evaluates `f` on `LayerExecutionSupport`: a copy of the
calling `CompiledExecutionSupport` - same class loader, caches, console and extensions - whose
processor support reads the layer's metadata. Its execution-end listeners belong to the calling
execution, so cleanup still happens when that execution ends.

**Interpreted (Pure IDE).** Interpreted natives look elements up through the runtime's processor
support, which the IDE sets to `LegendCompileMixedProcessorSupport`. The native pushes the layer onto
a per-thread stack on that processor support, runs `f`, and pops it in a `finally`. While a layer is
active, `package_getByUserPath` answers a path the calling graph does not hold from the layer. The
shared `Context` caches path lookups for the life of the runtime, so a lookup answered by the layer
is never put in it; otherwise the layer's elements would stay visible after `f` returns.

In both runtimes the compile natives called inside `f` use the active layer as their baseline.

---

## 7. Performance

`PipelineBench --suite sql` in `legend-engine-perf-benchmark` times the SQL flows that compile
through `meta::legend::compile`, over generated databases of 100 and 1000 tables. A `tb()` source
used to print the whole database it reads from and compile it again; as a layer it compiles only
its accessor, so the cost no longer follows the size of the database:

| Workload (1000 tables, 20 columns each) | Before | Layered |
|---|---|---|
| `#SQL` query lambda with one `tb()`, against a loaded model | 64 ms | 9 ms |
| `#SQL` query lambda joining two `tb()` sources | 130 ms | 19 ms |
| Compile a model with 10 `#SQL` functions | 651 ms | 87 ms |
| SQL schema endpoint | 363 ms | 300 ms |

The schema endpoint still compiles one accessor per table, so it stays linear in the number of
tables. The suite's canaries guard the other flows staying flat; see the module's README.

---

## 8. Known Limits

- **Compiled code cannot use a layer's elements as Java types.** There are no generated classes for
  them, so evaluating a layer function, instantiating a layer class or casting to one from compiled
  code fails with `ClassNotFoundException`. This predates layering.
- **Pure code does not see a layer's reverse references on baseline classes.** The side index is
  read by the compiler only: at run time a baseline class's `propertiesFromAssociations` and
  `specializations` do not include what a layer added, so code walking `specializations` from a
  baseline type, such as the router's leaf-type search, does not find layer subclasses.
- **Milestoning is not applied on the baseline side.** A layer association to a baseline temporal
  class is not milestoning-transformed on that class.
