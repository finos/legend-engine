# How to Wire a Function to Run on Target Relational Databases
## Alloy Compiler (Handlers.java)
*Only needed if you are adding a **new** Platform function or editing the function signature of an existing function.*

Currently, we need to manually register new functions in `Handlers.java`. This is needed by the Alloy compiler to recognize the function and perform type checking.

### What to update in Handlers.java
Handlers.java needs to be updated in two places:
1. **Handler registration** - Register your function using the `h()` helper method
2. **Return type inference** - If your function has a complex return type, add an inference function

### Understanding the `h()` method parameters
```java
h("meta::pure::functions::math::cosh_Number_1__Float_1_",  // Canonical function name
  "cosh",                                                   // Short name
  false,                                                    // Requires type inference?
  ps -> res("Float", "one", pureModel),                    // Return type inference
  ps -> true)                                               // Validation predicate
```

| Parameter | Description |
|-----------|-------------|
| Canonical name | Full function path with signature pattern: `functionName_ParamType1_Mult1__ParamType2_Mult2__ReturnType_Mult_` |
| Short name | The function name as called in Pure |
| Requires inference | Set to `true` if the return type depends on input types (e.g., Relation functions) |
| Return type | A function that computes the return type; use `res("TypeName", "multiplicity", pureModel)` for simple types |
| Validation | A predicate to validate the function call; usually `ps -> true` |

### Tips for Handlers.java
1. Look at how similar functions were registered
2. Make sure all variations of your function signature are registered
3. Check that params and return output types and multiplicities match what you expect

###### Example PRs showing Handlers.java updates
- [Handlers.java update for between](https://github.com/finos/legend-engine/pull/3560/files#diff-e99981e388e52fb746dc3e0959192f6db18bc1ec8ed1b43b8a769f28c5233564)
Notice how there is one line per function signature found in *between.pure*
- [Handlers.java update for timeBucket](https://github.com/finos/legend-engine/pull/3491/files#diff-e99981e388e52fb746dc3e0959192f6db18bc1ec8ed1b43b8a769f28c5233564)
There is only one function signature registered as *timeBucket.pure* only contains one function signature (at the time of the PR)

## Registering the New Function to Enable Pure to SQL Translation
The function signature for your new function must be registered in, at a minimum, three places:
1. ```pureToSqlQuery.pure```
2. ```relationalExtension.pure```, and
3. ```dbExtension.pure```

### Understanding the SQL Generation Pipeline
When Legend translates Pure to SQL, it goes through an intermediate representation called **DynaFunction**:

```
Pure Function  →  DynaFunction (IR)  →  SQL String
```

**DynaFunction** is a database-agnostic representation of the function call. The `dynaFnToSql` mappings then convert this to actual SQL for each database.

### What each file does

| File | Purpose |
|------|---------|
| `dbExtension.pure` | Registers the DynaFunction name in the `DynaFunction` enum |
| `pureToSqlQuery.pure` | Maps the Pure function to a processor that creates the DynaFunction |
| `relationalExtension.pure` | Registers the function with the relational extension |
| `extensionDefaults.pure` | Defines the default SQL template (used if no DB-specific override) |
| `{dbname}Extension.pure` | Database-specific SQL overrides (e.g., `snowflakeExtension.pure`) |

See [the "*between*" PR for a simplistic example](https://github.com/finos/legend-engine/pull/3560/files#diff-bf7def219fdef8a303208f8d40450b2b9a99539417b4fe6316cccf439452d1ac)

## Wiring to Target Relational Database Runtimes
We may need to add further SQL translation instructions to help the platform cross-compile to the target database runtimes.
This section describes some common scenarios with examples.

### ANSI SQL Functions
For ANSI SQL functions, we add them to ```extensionDefaults.pure```. This file contains the default SQL translation to
enable cross-compilation to relational target runtimes.

###### Example
*cosh* was added to extensionDefaults.pure instead of in the database-specific extensions as it falls into the same category of functions
as *cos*, *sin*, etc. which are ANSI and already defined in that file.
```Java
dynaFnToSql('cosh',                   $allStates,            ^ToSql(format='cosh(%s)'),
```

### Non-ANSI SQL Functions
We will need to add database-specific SQL to the respective pure file that contains the custom wiring for that database.

#### Where are database extension files located?
Database-specific extensions are in:
```
legend-engine-xts-relationalStore/
  legend-engine-xt-relationalStore-dbExtension/
    legend-engine-xt-relationalStore-{dbname}/
      .../sqlQueryToString/{dbname}Extension.pure
```

For example:
- DuckDB: `legend-engine-xt-relationalStore-duckdb/.../duckdbExtension.pure`
- Snowflake: `legend-engine-xt-relationalStore-snowflake/.../snowflakeExtension.pure`
- SQL Server: `legend-engine-xt-relationalStore-sqlserver/.../sqlServerExtension.pure`

#### When do you need a database-specific override?
Add an override when:
- The function name differs (e.g., `stddev_pop` vs `stdevp`)
- The parameter order differs
- The syntax differs (e.g., different date/interval formatting)
- The function doesn't exist and needs to be emulated

##### Example
*timeBucket* has different syntax in two target database runtimes: duckDb and Snowflake. These two queries will not yield equivalent results. Further, you can see that the parameters they accept are different.
```Java
// DuckDb
Select time_bucket(Interval '2 Day', timestamp '2024-01-31 00:32:34');
// results in 
2024-01-31 00:00:00

// Snowflake
SELECT TIME_SLICE(TIMESTAMP_FROM_PARTS(2024, 01, 31, 00, 32, 34), 2, 'DAY', 'START')
// results in
2024-01-30 00:00:00.000
```

We have to adjust for this in the target-specific wiring. To ensure functional correctness, this is how we wired those functions
in the database-specific pure extensions:
###### duckdbExtension.pure
```Java
dynaFnToSql('timeBucket',             $allStates,            ^ToSql(format='cast(time_bucket(%s) as timestamp_s)', transform={p:String[3] | constructIntervalFunction($p->at(2), $p->at(1)) + ', ' + $p->at(0) + ', ' + constructTimeBucketOffset($p->at(2))})),

// DuckDb uses a different origin for calculation of timebuckets; this offset helps to standardize toward unix epoch as origin and
// the offset for intervals < WEEK are set to align with Snowflake's methodology, as opposed to that which is outlined in DuckDb
// ref: https://github.com/duckdb/duckdb/blob/68bd4a5277430245e3d9edf1abbb9813520a3dff/extension/core_functions/scalar/date/time_bucket.cpp#L18
function meta::relational::functions::sqlQueryToString::duckDB::constructTimeBucketOffset(unit:String[1]):String[1]
{
  let unitWithoutQuotes = $unit->removeQuotesIfExist();
  let ISOMondayEpochOffset = 'timestamp \'1969-12-29 00:00:00\'';
  let EpochOffset = 'timestamp \'1970-01-01 00:00:00\'';

  let offset = [
      pair(DurationUnit.YEARS->toString(), $EpochOffset),
      pair(DurationUnit.MONTHS->toString(), $EpochOffset),
      pair(DurationUnit.WEEKS->toString(), $ISOMondayEpochOffset),
      pair(DurationUnit.DAYS->toString(), $EpochOffset),
      pair(DurationUnit.HOURS->toString(), $EpochOffset),
      pair(DurationUnit.MINUTES->toString(), $EpochOffset),
      pair(DurationUnit.SECONDS->toString(), $EpochOffset)
   ]->filter(p | $p.first == $unitWithoutQuotes).second->toOne('Unit not found: ' + $unitWithoutQuotes);
}
```
###### snowflakeExtension.pure
```Java
dynaFnToSql('timeBucket',             $allStates,            ^ToSql(format='TIME_SLICE(%s)', transform={p:String[3]|$p->at(0) + ', ' + constructInterval($p->at(2), $p->at(1))})),

function meta::relational::functions::sqlQueryToString::snowflake::constructInterval(unit:String[1], i:String[1]):String[1]
{
   let unitWithoutQuotes = $unit->removeQuotesIfExist();

   let interval= [
      pair(DurationUnit.YEARS->toString(), '\'YEAR\''),
      pair(DurationUnit.MONTHS->toString(), '\'MONTH\''),
      pair(DurationUnit.WEEKS->toString(), '\'WEEK\''),
      pair(DurationUnit.DAYS->toString(), '\'DAY\''),
      pair(DurationUnit.HOURS->toString(), '\'HOUR\''),
      pair(DurationUnit.MINUTES->toString(), '\'MINUTE\''),
      pair(DurationUnit.SECONDS->toString(), '\'SECOND\'')
   ]->filter(p | $p.first == $unitWithoutQuotes).second->toOne('Unit not supported: ' + $unitWithoutQuotes);

   $i + ', ' + $interval;
}
```

### To Route or not to Route?
- Native Platform Functions - are never routed, as there is no implementation in Pure. **Skip** this section for *native functions*.
- Pure Platform Functions - This is a choice by the developer. PCT does not care about implementation details, it cares about functional correctness.
Routing is controlled in ```router_routing.pure``` via the config in ```function meta::pure::router::routing::shouldStopFunctions```

---

## SQL Dialect Translation (toPostgresModel.pure)

> **Important:** This step is often missed! If your function uses the SQL dialect translation pathway, you need to register it here too.

In addition to the `extensionDefaults.pure` and database-specific extension files, there's another SQL generation system used for certain execution paths: **SQL Dialect Translation**.

### When do you need to update toPostgresModel.pure?

If you see an error like:
```
"Couldn't find DynaFunction to Postgres model translation for yourFunction()."
```

You need to add your function to the `getDynaFunctionConverterMap()` function in `toPostgresModel.pure`.

### File location
```
legend-engine-xts-relationalStore/
  legend-engine-xt-relationalStore-generation/
    legend-engine-xt-relationalStore-pure/
      legend-engine-xt-relationalStore-core-pure/
        src/main/resources/core_relational/relational/sqlDialectTranslation/
          toPostgresModel.pure
```

### How to add your function

Add a new entry to the `getDynaFunctionConverterMap()` function:

```pure
pair('yourFunctionName',  functionCall('postgres_equivalent_function')),
```

For example, existing entries look like:
```pure
pair('abs',       functionCall('abs')),
pair('ceiling',   {p:Expression[*]| castExpression(functionCall('ceiling', $p), 'bigint')}),
pair('cos',       functionCall('cos')),
pair('denseRank', functionCall('dense_rank')),
```

For more complex translations, you can provide a lambda that transforms the parameters:
```pure
pair('yourFunction', {p:Expression[*]| /* custom translation logic */ }),
```

---

##### Routing Example
In the case of the *between* platform function, we saw that the platform implementation of inequality
operators for null (Nil) return a boolean result. E.g. ```null >= 1``` evaluates to ```false``` on the platform. 
However, null comparisons on DuckDb and Snowflake target runtimes evaluate to a result of ```null```. 

In the interest of functional correctness, we decided **not** to delegate the calculation of "*between*" to the target databases, but to instead fully route the function such that the generated SQL leverages
the platform's implementation of the calculation. [Note the lack of SQL wiring in the "*between*" PR](https://github.com/finos/legend-engine/pull/3560/files#diff-bf7def219fdef8a303208f8d40450b2b9a99539417b4fe6316cccf439452d1ac). Because this function is fully-routed, the platform uses the underlying SQL translation for inequalities to cross-compile to target relational databases.

> **_Optional:_**
See [Platform Concepts](concepts-glossary.md) for details about the platform/PCT's preference
for functional correctness, and details about *routing*.

## Testing your Wiring to Target Runtimes with Adapters
You can use *Adapters* to test your wiring. Ctrl+Shift+F for "testAdapterFor" to find all available adapters. You can use these adapters
to execute your PCT Tests in the respective target runtime. 

##### Example
```Java
let duckdbadapter = meta::relational::tests::pct::testAdapterForRelationalWithDuckDBExecution_Function_1__X_o_;
  
// runs the PCT "testTimeBucketSeconds" against DuckDb (eval on DuckDb)
meta::pure::functions::date::tests::testTimeBucketSeconds($duckdbadapter);
```

## Next Step
Learn how to run PCT Test Suites, and register expectedFailures, for targets where we have chosen not to implement yet, via the [Expected Failures How-To](expected-failures-howto.md).