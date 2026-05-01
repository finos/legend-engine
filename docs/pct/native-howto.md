# Contributing Java (aka Native) Platform Functions
This guide assumes you have already:
- [ ] completed the [Dev Setup](overview.md#development-setup),
- [ ] identified the function you want to add,
- [ ] identified where in the [taxonomy](taxonomy.md) it belongs,

In this guide, we will use the addition of the native "**cosh**" function, and subsequent wiring to be able to cross-compile
to target databases, as the example for our walkthrough.

## Determine and Define the Pure Function Signature
All platform functions have a function signature defined in Pure. A Java platform function (also referred to as a "*native*" function),
will differ in two ways:
1. The Java function's Pure Function Signature will have the keyword "native" in it.
2. This function signature will not have a function body written in Pure. The *.pure file will only contain the function signature but no implementation.

Follow steps 1-3 in [the Pure Function How-To Guide](purefunction-howto.md) to create the pure file and function signature for your new function.

##### Example
For "**cosh**", we created a *.pure file here:

![cosh-file-taxonomy](assets/howto-cosh.pure.PNG)

The function signature in the file looks like this:
```Java
native function
    <<PCT.function>>
        {
            doc.doc='cosh returns the hyperbolic cosine of a number'
        }
meta::pure::functions::math::trigonometry::cosh(number:Number[1]):Float[1];
```

## How to Run your Native Function
Before we dig into the implementation of the function, it is important to know how can use Pure to call your native function
while you are developing the code in *Java*.

### TestHelper files
There are two *TestFunction_TestHelper_* files in the ```legend-engine-pure-code-functions-standard``` package to help you run Pure code without re-compiling the PureIDE. 
Re-compiling the PureIDE to pick up new Java changes can be slow, it is faster to run your code via:
- ```TestFunction_TestHelper_Interpreted``` - run Pure code in the *Interpreted* execution mode.
- ```TestFunction_TestHelper_Compiled``` - run Pure code in the *Compiled* execution mode.

###### IDE: IntelliJ

If you are unfamiliar, see the [Platform Concepts](concepts-glossary.md#pure-runtime-code-paths) page for an explainer on the Legend code execution modes.

#### Let's take a look at the example Pure code in TestHelper
There is a ```resources/testHelperScratch.pure``` plaintext file for each of the TestHelpers (*compiled* and *interpreted*). 
You can edit, play with, and **execute Pure code directly from IntelliJ by editing this file**!

##### Example
Going back to our **cosh** example, we can call it from the testHelper via editing ```resources/testHelperScratch.pure``` 
to contain pure code that calls the function signature we've defined:
```Java
import meta::pure::functions::math::*;
function test():Any[*]
{
    meta::pure::functions::math::cosh(.5);
}
```

Building and running this unit test will result in the following error:

```Execution error at (resource:testHelper.pure line:4 column:30), "The function 'cosh_Number_1__Float_1_' is not supported by this execution platform"```

This is good. If we can fix these errors and enable our simple snippet to succeed, we will have completed the native Java implementation of our new platform function!

*Note: TestHelpers will not have access to the full universe of Pure functions (e.g. dependencies which core_functions_standard was not built with).
However, it will enable you to complete your native function for the 2 code paths.*

## Adding the Java (native) implementation
Recall that there are two execution modes (interpreted, and compiled) for Legend engine.

### Adding the *"Compiled"* code path
Within the package: ```legend-engine-pure-runtime-java-extension-compiled-functions-standard```, you will need to make changes in 3 places:
1. in the ```natives``` package, add a *.java file, while following the same directory hierarchy as for the *.pure code for your function.
2. ```StandardFunctionsHelper.java``` - Contains the java implementation of the function
3. ```StandardFunctionsExtensionCompiled.java``` - you must import and register your Java implementation here.

##### Example
See **cosh** PR [here](https://github.com/finos/legend-engine/pull/3604/files#diff-e8aa7c61ce30513ab14d1bc07ba10d0a5d57cf7fe62baecaf2272a6255a31dc0)
for modifications to the 3 files mentioned above.

![howto-native-compiled](assets/howto-native-compiled.PNG)

###### Let's take a look at natives/math/trigonometry/CosH.java
Within this Java file, you will add boilerplate code to enable the Legend platform to pick up this new implementation. The key line in the file is:

```Java
super("StandardFunctionGen.cosh", new Class[]{Number.class}, "cosh_Number_1__Float_1_");
```
You will see that there are three params:
1. ```StandardFunctionGen.cosh``` - refers to the function defined in ```StandardFunctionsHelper.java```
2. the second param indicates the Type(s) of the params accepted by the function
3. the last param is the function signature of the new function (*Hint* - we can get this from the TestHelper error output we saw in the earlier step)

To refine our implementation and ensure proper registration, use ```TestFunction_TesterHelper_Compiled``` to execute the pure code (defined in the respective ```testHelperScratch.pure```) that calls
your native function.

#### Functions with Multiple Signatures
If your function has multiple overloaded signatures (e.g., different parameter types or counts), **you will typically need a separate Java file per signature in *compiled* mode**, each registering a different canonical function name in `*ExtensionCompiled.java`.

A good real example is `extend`, which has **eight** overloaded signatures (single vs. array column-spec, function vs. aggregate, with vs. without window). Each signature gets its own `Native` subclass under [`…/relation/compiled/natives/`](https://github.com/finos/legend-engine/tree/master/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-runtime-java-extension-compiled-functions-relation/src/main/java/org/finos/legend/pure/runtime/java/extension/external/relation/compiled/natives):

| Java file | Canonical Pure name |
|-----------|--------------------|
| `Extend.java` | `extend_Relation_1__FuncColSpec_1__Relation_1_` |
| `ExtendArray.java` | `extend_Relation_1__FuncColSpecArray_1__Relation_1_` |
| `ExtendAgg.java` | `extend_Relation_1__AggColSpec_1__Relation_1_` |
| `ExtendAggArray.java` | `extend_Relation_1__AggColSpecArray_1__Relation_1_` |
| `ExtendWindowAgg.java` | `extend_Relation_1___Window_1__AggColSpec_1__Relation_1_` |
| `ExtendWindowAggArray.java` | `extend_Relation_1___Window_1__AggColSpecArray_1__Relation_1_` |
| `ExtendWindowFunc.java` | `extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_` |
| `ExtendWindowFuncArray.java` | `extend_Relation_1___Window_1__FuncColSpecArray_1__Relation_1_` |

All eight are registered in [`RelationExtensionCompiled.getExtraNatives()`](https://github.com/finos/legend-engine/blob/master/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-runtime-java-extension-compiled-functions-relation/src/main/java/org/finos/legend/pure/runtime/java/extension/external/relation/compiled/RelationExtensionCompiled.java). `groupBy` follows the same pattern with four files (`GroupByColSpecAgg`, `GroupByColSpecAggArray`, `GroupByColSpecArrayAgg`, `GroupByColSpecArrayAggArray`).

> **Note on Interpreted mode:** in `RelationExtensionInterpreted` all eight `extend_Relation_…` canonical names map to the *same* `Extend::new` class (see the `Tuples.pair(...)` block at the top of the file). The "one Java file per signature" rule above is a **compiled-mode** concern — interpreted mode frequently reuses a single class across multiple canonical names. Always check both extension classes when wiring a new function.

The rule of thumb: **the registration list in `*ExtensionCompiled.java` is the source of truth** for which signatures are wired up — when adding overloads, mirror that pattern.

#### Two compiled-mode Native patterns
When you open the example natives, you'll notice two distinct shapes. Both are valid; pick based on complexity:

1. **Simple delegation** — extend `AbstractNativeFunctionGeneric` and just map the canonical name to a static helper method. This is the `cosh` / `Limit` / `Slice` pattern:
   ```Java
   // Limit.java
   super("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.limit",
         new Class[]{Relation.class, Long.class, ExecutionSupport.class},
         false, true, false,
         "limit_Relation_1__Integer_1__Relation_1_");
   ```

2. **Custom code generation** — extend `AbstractNative` and override `build(...)` / `buildBody(...)` to emit Java source into the generated bytecode. Use this when the arguments need pre-processing (e.g., column-spec lambdas, function compilation, building intermediate transfer objects). This is the `Extend` pattern: the constructor takes only the canonical name and the real work happens in the overridden methods.

If your function can be expressed as "call this static helper with these args", use pattern 1. If it needs to manipulate Pure-graph nodes or compose generated code, use pattern 2 (use `Extend.java` / `GroupByColSpecAgg.java` as templates).

### Adding the *"Interpreted"* code path
Within the package: ```legend-engine-pure-runtime-java-extension-interpreted-functions-standard```, you will need to make changes in 2 places:
1. in the ```natives``` package, add a *.java file, while following the same directory hierarchy as for the *.pure code for your function.
2. ```StandardFunctionsExtensionInterpreted.java``` - you must import and register your Java implementation here.

See **cosh** PR [here](https://github.com/finos/legend-engine/pull/3604/files#diff-60ddd057c70f86c5eeb758b771022f16c7ad82e8640a26b6d424d34f2834189b) for the modifications to the 2 files mentioned above.

> **_Note:_** This example has a very simple (one-line) implementation in Java. For more complex logic, the
```legend-engine-pure-runtime-java-extension-shared-functions-standard``` package can be leveraged to ensure that the common
parts of the implementation are not duplicated across execution modes. See [TimeBucketShared.java in the timeBucket PR](https://github.com/finos/legend-pure/pull/943/files#diff-aab5b1a4b8acf90017761a5798f01ea3542a2afd62bac761e69074bf6073e678)
> for an example that leverages the "shared-functions-standard" utility package.

## Adding PCT Tests
Within the same *.pure* file where you defined the pure function signature, you can use the *PureIDE* to write PCT Tests. 
Look at existing functions tagged with stereotype ```<<PCT.test>>``` to get inspiration. Also, check [PCT Test Conventions](conventions.md#pct-tests) for useful tips and a refresher on best practices.
- *If no java changes needed* - In PureIDE, use welcome.pure to execute your tests using the InMemory Adapter and refine your tests.
- *If iterating on java code* - In IntelliJ, use the ```TestHelper``` files (for interpreted and compiled) to run your pure code without recompiling PureIDE

##### Example
```Java
function go():Any[*]
{
  // the below calls the PCT Tests, passing the inmemoryadapter as the param
  let inmemoryadapter = meta::pure::test::pct::testAdapterForInMemoryExecution_Function_1__X_o_;
  meta::pure::functions::math::tests::trigonometry::cosh::testCosH_Identities($inmemoryadapter);
  meta::pure::functions::math::tests::trigonometry::cosh::testCosH_Integers($inmemoryadapter);
  meta::pure::functions::math::tests::trigonometry::cosh::testCosH_Floats($inmemoryadapter);
  meta::pure::functions::math::tests::trigonometry::cosh::testCosH_Eval($inmemoryadapter);
  meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig($inmemoryadapter);
}
```

### Example PR
The resulting code for [*cosh.pure* can be seen in this PR.](https://github.com/finos/legend-engine/pull/3604/files#diff-e3bc3198a3951d9ac9fd31d38d4de7be98a92d5b31d648ad806d7c35b72d2ac0)

---

## Relation Functions (Different from Standard Functions)

The examples above use **Standard functions** (like `cosh`). If you're adding a **Relation function** (functions that operate on `Relation<T>` types, like `extend`, `groupBy`, `cumulativeDistribution`, `rank`), the process is similar but uses **different packages and helper classes**.

### Key Differences for Relation Functions

| Aspect | Standard Functions | Relation Functions |
|--------|-------------------|-------------------|
| Pure definition package | `legend-engine-pure-code-functions-standard` | `legend-engine-pure-code-functions-relation` |
| Compiled natives package | `legend-engine-pure-runtime-java-extension-compiled-functions-standard` | `legend-engine-pure-runtime-java-extension-compiled-functions-relation` |
| Interpreted natives package | `legend-engine-pure-runtime-java-extension-interpreted-functions-standard` | `legend-engine-pure-runtime-java-extension-interpreted-functions-relation` |
| Shared / cross-mode package | `legend-engine-pure-runtime-java-extension-shared-functions-standard` (e.g. `TimeBucketShared.java`) | `legend-engine-pure-runtime-java-extension-shared-functions-relation` (e.g. `TestTDS.java`) |
| Helper class invoked from generated code | `StandardFunctionsHelper.java` | `RelationNativeImplementation.java` *(lives in the compiled module)* |
| Extension class (compiled) | `StandardFunctionsExtensionCompiled.java` | `RelationExtensionCompiled.java` |
| Extension class (interpreted) | `StandardFunctionsExtensionInterpreted.java` | `RelationExtensionInterpreted.java` |

> **Note:** For Relation functions, `RelationNativeImplementation.java` is the entry point that compiled-mode natives delegate to (look at the `super("…RelationNativeImplementation.<method>", …)` calls in classes like `Limit.java` or `Slice.java`, or the inlined `RelationNativeImplementation.projectExtend(...)` call inside `Extend.buildBody()`). The shared `TestTDS.java` is the underlying in-memory tabular data structure that both compiled and interpreted modes manipulate.

### Relation Function Examples
- [`Extend.java`](https://github.com/finos/legend-engine/blob/master/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-runtime-java-extension-compiled-functions-relation/src/main/java/org/finos/legend/pure/runtime/java/extension/external/relation/compiled/natives/Extend.java) — a `Relation<T> → Relation<T+R>` function with custom type inference (uses the *custom code generation* pattern)
- [`Limit.java`](https://github.com/finos/legend-engine/blob/master/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-runtime-java-extension-compiled-functions-relation/src/main/java/org/finos/legend/pure/runtime/java/extension/external/relation/compiled/natives/Limit.java) — minimal *simple delegation* example for a single-signature relation native
- [`CumulativeDistribution.java`](https://github.com/finos/legend-engine/blob/master/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-runtime-java-extension-compiled-functions-relation/src/main/java/org/finos/legend/pure/runtime/java/extension/external/relation/compiled/natives/CumulativeDistribution.java) — a single-signature window scalar returning `Float[1]`
- [`GroupByColSpecAgg.java`](https://github.com/finos/legend-engine/blob/master/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-runtime-java-extension-compiled-functions-relation/src/main/java/org/finos/legend/pure/runtime/java/extension/external/relation/compiled/natives/GroupByColSpecAgg.java) — example of one of multiple `groupBy` overloads
- [`RelationNativeImplementation.java`](https://github.com/finos/legend-engine/blob/master/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-runtime-java-extension-compiled-functions-relation/src/main/java/org/finos/legend/pure/runtime/java/extension/external/relation/compiled/RelationNativeImplementation.java) — the helper class your natives delegate to

### Type Inference for Relation Functions
Relation functions often have complex return types like `Relation<T+R>` (input columns plus new columns). These require **custom type inference** registered in `Handlers.java`.

The canonical example is `extend`, which takes a `Relation<T>` plus a column-spec describing one or more new columns, and returns a `Relation<T+R>`:

```
Input:  Relation<(name:String, value:Integer)>, ~doubled: x | $x.value * 2 : Integer
Output: Relation<(name:String, value:Integer, doubled:Integer)>
```

The inference logic lives in [`Handlers.ExtendReturnInference(...)`](https://github.com/finos/legend-engine/blob/master/legend-engine-core/legend-engine-core-base/legend-engine-core-language-pure/legend-engine-language-pure-compiler/src/main/java/org/finos/legend/engine/language/pure/compiler/toPureGraph/handlers/Handlers.java) (around line 443) and is wired into all eight `extend_Relation_…` handler registrations in the same file (search for `extend_Relation`). When adding a new relation function whose return type depends on its column-spec arguments, model your inference function after `ExtendReturnInference` and register it the same way.

---

## Troubleshooting Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `"The function 'xxx' is not supported by this execution platform"` | Function not registered in extension class | Register in **both** `*ExtensionCompiled.java` AND `*ExtensionInterpreted.java` |
| `"Cannot determine return type"` | Missing or incorrect type inference | Add/fix inference function in `Handlers.java` |
| Works in Compiled mode but fails in Interpreted (or vice versa) | Only registered in one extension | Register in both compiled and interpreted extensions |
| `"Native function not found"` | Canonical name mismatch | Verify the function signature string matches exactly (e.g., `extend_Relation_1__FuncColSpec_1__Relation_1_`) |
| SQL works on Postgres but fails on SQL Server | Database-specific syntax differences | Add override in the database-specific extension file (e.g., `sqlServerExtension.pure`) |
| Handler registered but never called | Canonical name in `Handlers.java` doesn't match Pure signature | Double-check the naming pattern: `functionName_ParamType1_Mult1__ParamType2_Mult2__ReturnType_Mult_` |
| `"Couldn't find DynaFunction to Postgres model translation for xxx()"` | Function missing from SQL dialect translation | Add entry to `getDynaFunctionConverterMap()` in `toPostgresModel.pure` (see [Wiring How-To](wiring-howto.md#sql-dialect-translation-topostgresmodelpure)) |

---

## Next Steps
The next step is to wire your function to run on Relational Database targets. We must instruct the platform on how to "wire" (aka cross-compile) the function
to the target database runtime. See the [Wiring How-To](wiring-howto.md) guide for how to determine what changes are necessary, and the steps to take.
