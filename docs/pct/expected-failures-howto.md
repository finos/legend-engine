# Final Steps
As the final step of your development, you should run the `Test_*_PCT.java` tests, which will pick up and run the PCT tests you defined in earlier steps. The steps below describe one method of running these tests while building.

## Important
> **_Obligatory Warning:_**
The next section should be the **Final Step** and should only happen once.
>
***Maven builds are expensive and should be avoided until absolutely necessary.***

## To Proceed
Run `mvn clean install` with tests to identify any tests that fail due to your new changes:

```
mvn clean install
```

Whenever you encounter a failing test:

1. **Determine whether the failure is expected.** If the adapter legitimately does not support the function (e.g. the SQL translation is not yet wired), it is an expected failure. Record it in the manifest so the test suite treats it as a known exclusion.

2. **If unexpected, fix the failure.**

3. Once fixed or excluded, use the **Resume Build From Specified Module** button to continue the build from where it
   left off (see image below).

![ResumeBuild](assets/resumeBuildbutton.PNG)

---

## Recording Expected Failures in the Manifest

Expected failures are recorded in **JSON manifest files** — one per function category per adapter

### Finding the Right Manifest

Each PCT module contains manifests under:

```
src/main/resources/pct-manifests/<adapter-key>/<Category>Functions_manifest.json
```

For example, for H2:

```
legend-engine-xt-relationalStore-h2-PCT/src/main/resources/pct-manifests/relational-h2/
    EssentialFunctions_manifest.json
    GrammarFunctions_manifest.json
    RelationFunctions_manifest.json
    StandardFunctions_manifest.json
    UnclassifiedFunctions_manifest.json
    VariantFunctions_manifest.json
    ScenarioQuantFunctions_manifest.json
```

The `MANIFEST_PATH` constants in the adapter's test class (e.g. `Test_Relational_H2_PCT.java`) list the exact paths if you are unsure which file to edit.

### The Manifest Format

A manifest file looks like this:

```json
{
  "adapter": "meta::relational::tests::pct::h2::testAdapterForRelationalWithH2Execution_Function_1__X_o_",
  "exclusions": [
    {
      "test": "meta::pure::functions::collection::tests::at::testAt_Function_1__Boolean_1_",
      "expectedError": "\"->at(...) function is supported only after direct access of 1->MANY properties\""
    },
    {
      "test": "meta::pure::functions::collection::tests::at::testAtWithVariable_Function_1__Boolean_1_",
      "expectedError": "\"->at(...) function is supported only after direct access of 1->MANY properties\""
    }
  ]
}
```

| Field | Description |
|-------|-------------|
| `adapter` | The Pure adapter function path (do not change this). |
| `exclusions` | Array of individual test exclusions. |
| `exclusions[].test` | The fully-qualified Pure test function path. |
| `exclusions[].expectedError` | The error message (or substring) the test is expected to produce. |

> **Each test must be listed individually.** There is no package-level shorthand — even if many sibling tests fail for the same reason, each needs its own entry.

### Getting the Copy-Paste Snippet from a Failing Test

When a PCT test fails, the test runner prints the JSON snippet for you. Paste it directly into the `exclusions` array of the appropriate manifest file:

```
Test failure in meta::pure::functions::collection::tests::at::testAt_Function_1__Boolean_1_

Add the following to your manifest exclusions:
{
  "test": "meta::pure::functions::collection::tests::at::testAt_Function_1__Boolean_1_",
  "expectedError": "\"->at(...) function is supported only after direct access of 1->MANY properties\""
}
```

##### Example: Adding Expected Failures

Open the correct manifest and add the entry to the `exclusions` array:

```json
{
  "adapter": "meta::relational::tests::pct::postgres::testAdapterForRelationalWithPostgresExecution_Function_1__X_o_",
  "exclusions": [
    {
      "test": "meta::pure::functions::relation::tests::zscore::testZScore_Nulls_Function_1__Boolean_1_",
      "expectedError": "Postgres throws type error on non-numeric values"
    },
    {
      "test": "meta::pure::functions::relation::tests::pivot::testPivot_Function_1__Boolean_1_",
      "expectedError": "Pivot not yet supported"
    }
  ]
}
```

> **Note on CTE-related errors in PCT-relational:** stores that reject common table
> expressions only produce `"Common table expression not supported on DB <X>"` errors
> for PCT tests whose Pure expression contains top-level `let` statements. The PCT
> relational adapter wraps multi-statement bodies in `eval()`, which triggers
> `processFunctionDefinition` to lift each top-level `let` into a `CommonTableExpression`.
> Structurally similar tests without `let`s skip this path and surface a different error
> (typically `"pivot is not supported"`, `"function not supported yet: <fn>"`, or a
> column-resolution error from the SQL engine). Do not copy expected-failure messages
> between sibling tests without confirming the expression's multi-statement shape. See
> [../engineering/architecture/router-and-pure-to-sql.md](../engineering/architecture/router-and-pure-to-sql.md#441-multi-statement-bodies-and-cte-generation)
> for the code path.

---

Once all failures are fixed or recorded in the manifests and the build succeeds, you are ready to submit your PR. If your change is relied upon by downstream repos you will need to import and test the SNAPSHOT build there as well.