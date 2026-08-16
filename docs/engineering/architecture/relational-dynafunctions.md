# DynaFunctions in the Relational Database DSL

> **Scope.** This document covers dyna functions **as written by a modeller** in the
> `###Relational` Database DSL — inside `Filter`, `MultiGrainFilter`, `Join`, `View` column
> mappings, and relational property mappings. It documents what is supported today, how the
> support is implemented, where it breaks, and a proposal for improving it.
>
> Dyna functions **synthesised by the router** while translating a Pure query to SQL are a
> different producer feeding the same registry. Their dispatch, dialect registration, and
> null-safe-equality handling are already documented in
> [Router & Pure-to-SQL §5.3–5.7](router-and-pure-to-sql.md#53-dbconfig--dbextension). This
> document cross-references that material rather than restating it, and returns to the
> producer distinction in §10.1, where it constrains the design.

---

## 0. The short version

The working belief is *"we support a small subset of dyna functions (filter, joins)"*. That
is half right, and the half that is wrong is the half that matters:

| Layer | What it constrains |
|---|---|
| Grammar | **Nothing.** `functionOperation: identifier PAREN_OPEN (args)? PAREN_CLOSE` — any name, any arity, including zero. |
| Parse-tree walker | Nothing. The identifier is copied verbatim into `DynaFunc.funcName`. |
| Compiler | Nothing. No name check, no arity check, no type check. |
| Composer | Arity only, for 12 names — and on mismatch it emits a **comment string into the regenerated grammar** rather than failing (§9.1). |
| SQL generation | Everything. A raw Pure `assert`, at plan-generation time, with no `SourceInformation`. |

So the surface is not small because the engine restricts it. It is small because only a
handful of dyna functions are ergonomic, tested, or known-good — and nothing tells a
modeller which ones. `Filter Foo(bogusFunc(t.a, t.b, t.c))` parses, compiles, and passes
validation; it fails only when someone runs a query that touches it.

---

## 1. Where you can write a dyna function

All entry points are in
`legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-grammar/src/main/antlr4/org/finos/legend/engine/language/pure/grammar/from/antlr4/RelationalParserGrammar.g4`:

| Construct | Rule |
|---|---|
| `Filter <name>(<operation>)` | `filter` — `RelationalParserGrammar.g4:158` |
| `MultiGrainFilter <name>(<operation>)` | `multiGrainFilter` — `:160` |
| `Join <name>(<operation>)` | `join` — `:162` |
| `View` column mapping | `viewColumnMapping` — `:145` |
| Relational property mapping (`###Mapping`) | `relationalPropertyMapping` — `:296` |

The operation sub-grammar is `:165-229`:

```antlr
operation:                  booleanOperation | joinOperation
booleanOperation:           atomicOperation booleanOperationRight?
booleanOperationRight:      booleanOperator operation
booleanOperator:            AND | OR
atomicOperation:            (groupOperation
                             | (databasePointer? functionOperation)
                             | columnOperation
                             | joinOperation
                             | constant) atomicOperationRight?
atomicOperationRight:       (atomicOperator atomicOperation) | atomicSelfOperator
atomicOperator:             EQUAL | TEST_NOT_EQUAL | NOT_EQUAL | GREATER_THAN
                            | LESS_THAN | GREATER_OR_EQUAL | LESS_OR_EQUAL
atomicSelfOperator:         IS_NULL | IS_NOT_NULL
groupOperation:             PAREN_OPEN operation PAREN_CLOSE
functionOperation:          identifier PAREN_OPEN (functionOperationArgument
                              (COMMA functionOperationArgument)*)? PAREN_CLOSE
functionOperationArgument:  operation | functionOperationArgumentArray
functionOperationArgumentArray:
                            BRACKET_OPEN (functionOperationArgument
                              (COMMA functionOperationArgument)*)? BRACKET_CLOSE
```

Line 208 — `functionOperation` — is the whole story for names: it is `identifier`-based, so
**there is no `dynaFunc` rule and no keyword list**. `functionOperationArgumentArray` is what
lets you write `in(firmTable.ID, [2, 3, 4])`.

Column references are `tableAliasColumnOperation` (`:216-220`). There is no `this`/`that`
keyword pair in this grammar — `{target}` is the only reserved alias
(`RelationalLexerGrammar.g4:26`), used for directed self-joins.

---

## 2. Two notations, and the 12 names that have sugar

A `DynaFunc` reaches the protocol by one of two routes, both in
`.../grammar/from/RelationalParseTreeWalker.java`.

### 2.1 Operator sugar — a closed set of 12

| Walker method | Grammar | `DynaFunc.funcName` |
|---|---|---|
| `visitAtomicOperator` `:812-843` | `=` `>` `<` `>=` `<=` `!=` `<>` | `equal`, `greaterThan`, `lessThan`, `greaterThanEqual`, `lessThanEqual`, `notEqual`, `notEqualAnsi` |
| `visitAtomicSelfOperator` `:799-810` | `is null`, `is not null` | `isNull`, `isNotNull` |
| `visitBooleanOperation` `:745-777` | `and`, `or` | `and`, `or` |
| `visitGroupOperation` `:734-743` | `( … )` | `group` |

`group` is synthetic — a modeller never writes it. The walker creates it for parentheses, and
again at `:765-770` to force precedence when `and` and `or` mix. `visitBooleanOperation` also
flattens same-operator chains (`:757-760`), so `a and b and c` becomes a single three-argument
`and` rather than nested pairs.

### 2.2 Functional notation — unconstrained

```java
// RelationalParseTreeWalker.java:845-852
private RelationalOperationElement visitFunctionOperation(...)
{
    DynaFunc operation = new DynaFunc();
    operation.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
    operation.funcName = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
    operation.parameters = ListIterate.collect(ctx.functionOperationArgument(), ...);
    return operation;
}
```

The name is whatever was typed. Nothing looks it up.

### 2.3 The protocol object

`.../legend-engine-xt-relationalStore-protocol/src/main/java/.../store/relational/model/operation/DynaFunc.java`
is the entire class:

```java
public class DynaFunc extends RelationalOperationElement
{
    public String funcName;
    public List<RelationalOperationElement> parameters = Collections.emptyList();
}
```

Dispatch across `RelationalOperationElement` subtypes is Jackson-only (`@JsonSubTypes` in
`RelationalOperationElement.java:21-28`, discriminator `"dynaFunc"`). There is no
`RelationalOperationElementVisitor` anywhere in the repo — both directions use `instanceof`
chains, in `HelperRelationalBuilder.processRelationalOperationElement` and
`HelperRelationalGrammarComposer.renderRelationalOperationElement`.

### 2.4 Round-trip: Database bodies get sugar, mapping bodies do not

`HelperRelationalGrammarComposer.renderDynaFunc` (`:82-200`) mirrors exactly the same 12
names back to operator form — but only when `useDynaFunctionName` is `false`:

```java
// HelperRelationalGrammarComposer.java:85
if (context.getUseDynaFunctionName() && (!dynaFunc.funcName.equals("group")))
{
    return defaultDynaFunctionRender(dynaFunc, context);   // funcName(a, b)
}
```

`withNoDynaFunctionNames()` is called from **exactly one site** in the repo —
`RelationalGrammarComposerExtension:281`, inside `renderDatabase` — and the default is `true`
(`RelationalGrammarComposerContext:71`). Hence the asymmetry, which is deliberate (see the
comment at `HelperRelationalGrammarComposer.java:85`):

- inside a `Database`, `equal(a, b)` round-trips to `a = b`;
- inside a `Mapping`, `a = b` round-trips to `equal(a, b)`.

`TestRelationalGrammarRoundtrip.testRelationalAtomicOperationInFunctionalForm()` (`:618`)
asserts precisely this normalisation. Both notations are accepted on input:

```
// input — functional form
Join Abe(and(greaterThan([test::TEST_DB2]a.col1, 2),
             group(or(greaterThan([test::TEST_DB2]b.col1, 3),
                      greaterThan([test::TEST_DB2]b.col1, 4)))))

// output — always operator form inside a Database
Join Abe([test::TEST_DB2]a.col1 > 2 and ([test::TEST_DB2]b.col1 > 3
                                         or [test::TEST_DB2]b.col1 > 4))
```

A second, lower-fidelity composer exists on the Pure side —
`core_relational/relational/extensions/grammarSerializerExtension.pure:228-266` — covering the
same 12 names (`dynaFunc2` `:230-236`, `dynaFunc1` `:237-238`, `group` `:239`, `and`/`or`
`:240`, fallback `:242`).

---

## 3. Pipeline walk-through

Take `Filter activeOnly(in(firmTable.LEGALNAME, ['Firm C', 'Firm A']))`.

**Parse** — `visitFilter` (`RelationalParseTreeWalker:637-645`) → `visitOperation` →
`visitFunctionOperation` (`:845`) produces
`DynaFunc{funcName: "in", parameters: [TableAliasColumn, LiteralList]}` with source
information attached.

**Compile** — `RelationalCompilerExtension` runs `processDatabaseFilterFirstPass` (`:215`,
name + database shell only) then `processDatabaseFilterSecondPass` (`:236`, the operation).
The conversion is a plain `instanceof` chain:

```java
// HelperRelationalBuilder.processRelationalOperationElement:851-858
else if (operationElement instanceof DynaFunc)
{
    DynaFunc dynaFunc = (DynaFunc) operationElement;
    MutableList<RelationalOperationElement> ps = ListIterate.collect(dynaFunc.parameters,
            e -> processRelationalOperationElement(e, context, aliasMap, selfJoinTargets));
    return new Root_meta_relational_metamodel_DynaFunction_Impl("", m3SourceInformation,
                   context.pureModel.getClass("meta::relational::metamodel::DynaFunction"))
            ._name(dynaFunc.funcName)
            ._parameters(ps);
}
```

`._name(dynaFunc.funcName)` is the whole validation story. The string goes straight into the
M3 graph.

**Generate SQL** — the chain lives in
`.../legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbExtension.pure`:

```
processOperation                     (:526)   d:DynaFunction[1] | processDynaFunction($d, $sgc)
  └─ processDynaFunction             (:862-892)
       ├─ isEqualsFromFilter         (:864)   → processEqualFromFilter  (see §5)
       ├─ 'case'                     (:867)   → processCase             (:916)
       ├─ 'not'                      (:870)   → processNot              (:949)
       ├─ 'extractFromSemiStructured'(:873)   → processExtractFromSemiStructured (:894)
       └─ DbConfig.dynaFuncDispatch  (:170-174)  ← registry membership assert
            └─ getDynaFunctionDispatcher (:1029-1037) ← per-dialect lookup
                 └─ DynaFunctionToSql.toSql (:1045-1056) ← format-string substitution
```

Those four intercepted names never reach the registry lookup at all.

The registry-membership check:

```pure
// dbExtension.pure:170-174
dynaFuncDispatch(dynaFn: DynaFunction[1], sgc: SqlGenerationContext[1])
{
   assert(DynaFunctionRegistry->enumValues().name->contains($dynaFn.name),
          'dyna function [' + $dynaFn.name +
          '] is not registered in meta::relational::functions::sqlQueryToString::DynaFunctionRegistry');
   $this.dbExtension.dynaFuncDispatch->eval($dynaFn, $sgc);
}
```

and the per-dialect lookup:

```pure
// dbExtension.pure:1029-1037
let result = $dispatch->get($func.name)->cast(@List<DynaFunctionToSql>).values
              ->filter(d | $d.stateMatch->isEmpty() || $sgc.generationState->in($d.stateMatch));
assertSize($result, 1, | '[unsupported-api] The function \'' + $func.name +
                         '\' (state: [...]) is not supported yet');
```

For the mechanics of `DynaFunctionToSql`, `ToSql`, `GenerationState` and how a dialect
registers overrides, see
[Router & Pure-to-SQL §5.4–5.5](router-and-pure-to-sql.md#54-dynafunction-dispatch).

---

## 4. Compile-time guards that *do* exist

None of them concern the dyna function itself, but they are the only compile-time safety in
this area and are worth knowing:

| Guard | Location | Behaviour |
|---|---|---|
| Duplicate `Join` / `Filter` names | `RelationalCompilerExtension:212-213` | `EngineException` |
| Join refers to no table | `HelperRelationalBuilder:659-673` | `"A join must refer to at least one table"` |
| Join refers to > 2 tables | same | `"A join can only contain 2 tables. Please use Join chains (using '>')…"` |
| Self-join without `{target}` | `HelperRelationalBuilder:667-699` | `"The system can only find one table in the join. Please use the '{target}' notation…"` |
| `{target}` column not found | `:695` | `"The column 'X' can't be found in the table 'Y'"` |
| `sqlNull()` in a join condition | `RelationalValidator:237-265` | **Warning only**, and only reachable from *mapping* validation (`:270`) — not from `Database` compilation |

Note how join sides are identified: positionally, by alias count
(`processDatabaseJoinSecondPass`, `HelperRelationalBuilder:648-714`), never by dyna function
name. No dyna function name is special-cased in the compiler at all.

The `sqlNull` warning is the single piece of dyna-function-aware semantic checking in Java:

```java
// RelationalValidator.java:248-254
if ("sqlNull".equals(funcName))
{
    pureModel.addWarnings(Lists.mutable.with(new Warning(
        SourceInformationHelper.fromM3SourceInformation(dynaFunction.getSourceInformation()),
        "Invalid use of sqlNull() in join condition. Use 'column is NULL' instead of "
        + "'column = sqlNull()' in join condition.")));
}
```

It is a useful precedent for §10: it shows the mechanism (a `Warning` carrying
`SourceInformation`, produced during a graph walk) already exists and works.

---

## 5. `Filter` and `Join` are not the same

This surprises people, so it deserves its own section.

`Config.callingFromFilter` (`dbExtension.pure:28`) is set to `true` in exactly one situation
per dialect: while rendering `SelectSQLQuery.filteringOperation`, i.e. the `WHERE` clause
(e.g. `extensionDefaults.pure:357`, `postgresExtension.pure:337`, `snowflakeExtension.pure:638`).
**Join conditions never set it.**

```pure
// dbExtension.pure:927-931
isEqualsFromFilter(...)  // name == 'equal'
                         // && callingFromFilter == true
                         // && all params are TableAliasColumn with column.nullable != false

// dbExtension.pure:943-947
processEqualFromFilter(...)  // rewrites to nullSafeEqual and reprocesses
```

Consequence:

- `Filter F(t.a = t.b)` over nullable columns emits **null-safe** SQL.
- `Join J(t.a = t.b)` over the same columns emits a plain `=`.

`:888` resets `callingFromFilter` when descending into non-`and`/`or` children, so the rewrite
applies only at the top boolean level of the filter.

Two further behaviours worth knowing:

- **Boolean coercion.** `isBooleanOperation` (`dbExtension.pure:801-806`) lists the 25 dyna
  functions treated as predicates. Anything else appearing in a boolean position is wrapped by
  `maybeWrapAsBooleanOperation` (`:778-785`) as `equal(expr, true)`. This is why
  `Filter kerbFilter(startsWith(traderTable.kerberos, 'gs_') = 'true')`
  (`graphFetch/tests/testCrossStoreGraphFetch.pure:108`) looks odd but works. The list is
  extensible per store via `RelationalExtension.sqlQueryToString_isBooleanOperation`
  (`relational/extensions/extension.pure:56`).
- **Parameter wrapping.** `processDynaFunction:877-885` wraps `and`/`or` parameters as boolean
  operations, wraps only the head of `if`, and wraps parameters of non-boolean functions as
  boolean *case* operations.

---

## 6. What is actually supported — three different numbers

These are three answers to three different questions. Conflating them is the most common
mistake in discussions about dyna function support.

| Question | Answer | Source |
|---|---|---|
| Which names are **registered**? | **231** | `DynaFunctionRegistry` enum, `dbExtension.pure:1077-1309` |
| Which names **render** on a given dialect? | default **113**, ∪ that dialect's overrides — e.g. **179 / 231** on H2, **224 / 231** on Snowflake | `extensionDefaults.pure:186-305` + `<db>Extension.pure` |
| Which names are **exercised in this repository's fixtures**? | **15** in call form, of which 10 are not operator sugar | scan of all `Filter` / `MultiGrainFilter` / `Join` bodies |

### 6.1 Registered ≠ renderable

`getDynaFunctionToSqlDefault` (`extensionDefaults.pure:186-305`) supplies 113 entries, all
registered for every generation state. The other 118 registered names have **no default
rendering** — each dialect must supply its own. That is not the same as "unsupported": it
means support is per-dialect, and a filter that works on Snowflake may fail on Redshift.

Per-dialect override counts (distinct `dynaFnToSql` names in each `<db>Extension.pure`):

| Dialect | Overrides | Dialect | Overrides |
|---|---|---|---|
| DuckDB | 128 | SparkSQL / Postgres | 68 |
| Snowflake | 122 | Oracle | 67 |
| Databricks | 108 | SQL Server | 66 |
| H2 (all versions) | 75 | Trino | 63 |
| Sybase ASE / Spanner | 73 | Presto | 57 |
| Sybase IQ | 72 | BigQuery | 50 |
| ClickHouse | 71 | Redshift | 48 |
| MemSQL | 70 | DB2 | 45 |
| | | Composite | 7 |
| | | **Hive** | **0** (default table only) |

**`putAll` is replace, not merge.** The dispatcher map value is `List<DynaFunctionToSql>`, and
a dialect entry replaces the whole list for that key. A dialect that overrides a name must
therefore re-supply *every* generation state it needs; a partial override leaves the other
states hitting `[unsupported-api]`. ClickHouse `:84-87` splits `nullSafeEqual` /
`nullSafeNotEqual` across `selectOutsideWhenGenerationState()` and
`notSelectOutsideWhenGenerationStates()` for exactly this reason, as do SQL Server `:98-99`,
Sybase ASE `:88-89`, SparkSQL `:78-79`, Oracle `:188-189` and Sybase IQ `:79-80`.

Five registered names have **no** `dynaFnToSql` entry in any dialect: `between`, `case`,
`explodeSemiStructured`, `not`, `pair` — and for four of them that is by design, not a gap.
`case` and `not` are intercepted upstream (`processDynaFunction:867`, `:870`); `pair` is
router-internal (`pureToSQLQuery.pure:4007` builds `newDynaFunction('pair', $roes)` and consumes
it structurally, never rendering it); `explodeSemiStructured` is lowered by the router into a
lateral flatten join before SQL generation and so never reaches dispatch — see §11.7.

### 6.2 Renderable ≠ used

A balanced-paren scan of every `Filter` / `MultiGrainFilter` / `Join` body in the repository —
**671 definitions** across production and test resources — finds only 15 registry dyna
functions written in call form:

| Name | Occurrences | Name | Occurrences |
|---|---|---|---|
| `and` | 5 | `extractFromSemiStructured` | 1 |
| `in` | 3 | `toString` | 1 |
| `length` | 3 | `case` | 1 |
| `or` | 2 | `isNull` | 1 |
| `greaterThan` | 2 | `now` | 1 |
| `datePart` | 1 | `convertVarchar128` | 1 |
| `explodeSemiStructured` | 1 | `startsWith` | 1 |
| | | `lessThan` | 1 |

Strip the operator-sugar names and **10 of the 231 registered dyna functions are exercised
anywhere in Database-DSL text in this repository**.

> **Read that number carefully.** It measures this repository's fixtures, not the world.
> Production Legend models plausibly use more. It is evidence that our *coverage* is thin,
> not proof that anything else is broken.

Only four Pure fixtures put a dyna function inside a Database `Filter` or `Join`:

| Fixture | What it exercises |
|---|---|
| `tests/mapping/inClause/testInClauseForJoinsAndFilters.pure:78` | `in(…)` in both a `Filter` and a `Join`, with `assertSameSQL` expectations at `:29`, `:37`, `:45`, `:54` — **the copyable pattern** |
| `tests/mapping/union/testUnion.pure:839-840` | `greaterThan(length(A.name_s1), 0)` in a `Join` |
| `validation/tests/testComplexValidations.pure:218` | `lessThan(length(locationTable.STREET), 10)` in a `Filter` |
| `graphFetch/tests/testCrossStoreGraphFetch.pure:108` | `startsWith(...)` in a `Filter` |

On the Java side the coverage is `TestRelationalGrammarRoundtrip.testRelationalOperations()`
(`:573`) and `testRelationalAtomicOperationInFunctionalForm()` (`:618`) — both grammar-level,
both limited to the 12 operator-sugar names plus `case`, `substring` and `sqlNull`.

---

## 7. Four registries that should agree, and do not

| Registry | Location | Size | Purpose |
|---|---|---|---|
| `DynaFunctionRegistry` enum | `dbExtension.pure:1077-1309` | **231** | Canonical name list; membership asserted at `:172` |
| Type-inference overload table | `relationalExtension.pure:189-~1400` | **158** unique names (163 `pair`s) | Return-type inference for View columns, test-data generation, result columns |
| Default `dynaFnToSql` table | `extensionDefaults.pure:186-305` | **113** | Base SQL rendering, per generation state |
| Grammar operator sugar | walker `:799-843` + composer `:82-200` | **12** | Infix notation and round-trip |
| Boolean-valued classification | `dbExtension.pure:801-806` | 25 | **Not an allow-list.** Decides which functions are predicates, for `maybeWrapAsBooleanOperation` |

The type-inference map has five duplicate keys — `corr`, `covarPopulation`, `covarSample`,
`mod`, `mostRecentDayOfWeek` — which is why 163 `pair` entries yield 158 unique names. `PureMap`
is backed by an eclipse-collections `MutableMap`, which strongly suggests `newMap()` is
last-write-wins and silent; if so, one entire `list([...])` of inference rules for each of those
five names is unreachable dead code. **Confirm `newMap`'s duplicate-key semantics before acting
on this** — it is a check to run, not yet a finding. If confirmed it is a small, clean,
fully independent fix.

A further, independent name-keyed dispatch exists on the newer SQL-dialect-translation path:
`sqlDialectTranslation/toPostgresModel.pure`, `convertDynaFunction` (`:251-266`) and
`getDynaFunctionConverterMap` (`:268-465`). It is the only one that self-validates — at
`:454-461` it asserts every key it holds is present in `DynaFunctionRegistry`.

---

## 8. Error taxonomy — six ways this fails, none at compile time

| # | Trigger | Where | Message |
|---|---|---|---|
| 1 | Name not in the registry | `dbExtension.pure:172` | `dyna function [X] is not registered in meta::relational::functions::sqlQueryToString::DynaFunctionRegistry` |
| 2 | Registered, but no handler for this dialect + generation state (or two handlers match) | `dbExtension.pure:1033` | `[unsupported-api] The function 'X' (state: [Select, false]) is not supported yet` |
| 3 | Wrong arity | *nowhere* — falls through to `format(...)` at `:1054` | a generic `%s`-count error from `meta::pure::functions::string::format` |
| 4 | Wrong arity for one of the 12 sugared names, on compose | `HelperRelationalGrammarComposer:82-200` | a **comment string emitted into the grammar** — see §9.1 |
| 5 | Unknown name on the SQL-dialect-translation path | `toPostgresModel.pure:264` | `Couldn't find DynaFunction to Postgres model translation for X().` |
| 6 | Type inference has no matching overload | `relationalExtension.pure:116` | `Type inference not supported yet! Dyna function: X` |

None of these is an `EngineException`. None carries `SourceInformation`. Rows 1, 2, 3, 5 and 6
surface at **plan generation**, long after the model compiled cleanly — which contradicts the
error convention in [Coding Standards](../standards/coding-standards.md). Row 4 is worse: it
does not surface at all (§9.1).

The only per-function arity assertions in the entire codebase are ad hoc:

- `processExtractFromSemiStructured` — `dbExtension.pure:897`:
  `assertSize($params, 3, $func.name + ' takes 3 arguments, was called with N instead')`,
  plus a path-regex assert `:903` and a type-name whitelist `:906`.
- `isDistinct` — `extensionDefaults.pure:228`.
- `date` and `dayOfWeekNumber` on the Postgres-model path — `toPostgresModel.pure:590`, `:560`.
- The `ToSql` class constraint `dbExtension.pure:1060` and the `parametersWithinWhenClause`
  count check `:1034` — structural, not per-function.

---

## 9. Known defects

### 9.1 Silent model corruption on round-trip

This is the most serious defect in this area.

`HelperRelationalGrammarComposer.renderDynaFunc` (`:82-200`) is the only place in the whole
pipeline that checks dyna function arity — and when the check fails it *returns a comment
string as the rendered operation*:

```java
// HelperRelationalGrammarComposer.java:138-145
case "equal":
{
    if (dynaFunc.parameters.size() != 2)
    {
        return "/* Unable to transform operation: exactly 2 parameters are expected for '=' operation */";
    }
    return renderRelationalOperationElement(dynaFunc.parameters.get(0), context)
           + " = " + renderRelationalOperationElement(dynaFunc.parameters.get(1), context);
}
```

The same pattern appears for `group` (`:95`), `isNull` (`:124`), `isNotNull` (`:132`),
`greaterThan` (`:148`), `lessThan` (`:156`), `greaterThanEqual` (`:164`), `lessThanEqual`
(`:172`), `notEqual` (`:180`) and `notEqualAnsi` (`:188`).

Repro shape: a `PureModelContextData` containing
`DynaFunc{funcName: "equal", parameters: [<one element>]}` — reachable via the protocol API,
or via any producer that builds `DynaFunc` objects directly — composed back to grammar. The
resulting Database body contains a comment where a predicate used to be:

```
Join Firm_Person(/* Unable to transform operation: exactly 2 parameters are expected for '=' operation */)
```

That text then re-parses as an empty join condition. No error is raised at any point.

The Pure-side composer has the same design plus a copy-paste bug:
`grammarSerializerExtension.pure:263`, where `dynaFunc1`'s assert message says *"exactly 2
parameters"* while the check is `size() == 1`.

### 9.2 Exceptions without source information

- `HelperRelationalBuilder:869` — the `instanceof` chain's fallthrough throws a bare
  `UnsupportedOperationException`.
- `HelperRelationalBuilder:653` and `:728` cast the compiled operation to `(Operation)`.
  `DynaFunction` extends `Operation`, so dyna functions are fine — but a `Filter` or `Join`
  whose top-level operation is a `Literal`, `TableAliasColumn` or `ElementWithJoins` throws a
  bare `ClassCastException` with no source information.

### 9.3 Duplicated grammar implementations

A second, independent `###Relational` parser and composer live upstream in the
`legend-pure-m2-store-relational-grammar` jar, and a second composer lives in-repo at
`grammarSerializerExtension.pure`. Any grammar-layer change has to be made in more than one
place or the implementations diverge.

---

## 10. Proposal — parameter validation

### 10.1 The seam

The constraint that makes a naive "validate the name at compile time" proposal wrong is that
the dyna function registry has **three producers**, not one:

1. **Engine protocol path** — a modeller authors `###Relational` text, the engine parses it to
   protocol `DynaFunc` objects, and `HelperRelationalBuilder` compiles them. This document's
   main subject.
2. **Router synthesis** — the router constructs `^DynaFunction(...)` directly in Pure against
   the M3 metamodel while translating Pure to SQL (`pureToSQLQuery.pure`, `calendarFunctions.pure`,
   `milestoning.pure`, graphFetch, postprocessors). These legitimately use names, shapes and
   arities no human writes. `pair` (`pureToSQLQuery.pure:4007`) is the clean example: a
   registered name that exists only to carry a tuple between router stages.
3. **Upstream Pure grammar** — `###Relational` written in a `.pure` source file is parsed by an
   independent ANTLR implementation in the `legend-pure-m2-store-relational-grammar` jar, which
   builds the M3 `Database` graph **without ever entering engine Java**. This is not a corner
   case: `core_relational` alone ships **91** `.pure` files containing `###Relational` Database
   definitions, and it declares that jar as both a Pure-repo and a Maven dependency
   (`legend-engine-xt-relationalStore-core-pure/pom.xml:46`, `:170`).

Validation must cover producers 1 and 3 without constraining producer 2.

**The seam between 1 and 2 is structural, and it is a language boundary.**
Every call site of `HelperRelationalBuilder.processRelationalOperationElement` — joins `:653`,
filters `:728`, View column mappings `:503`/`:506`, property mappings `:1069`/`:1805`, primary
keys `:1159`, class-mapping groupBy (`RelationalCompilerExtension:314`) — consumes the
**protocol** `DynaFunc` type. The router never produces protocol `DynaFunc`; it builds M3
`DynaFunction` instances in Pure. Nothing crosses. A check installed on the protocol → graph
path is therefore *structurally incapable* of seeing a router-generated dyna function. That
means it:

- applies to exactly the human-authored operations, and nothing else;
- needs **no** feature flag and no `callingFromFilter`-style context threading;
- needs **no** protocol change, so no new `v1_XX` sub-package and no transfer function;
- can raise a proper `EngineException` with the `SourceInformation` that
  `DynaFunc.sourceInformation` already carries (set at `RelationalParseTreeWalker:848`) and that
  survives into M3 (`HelperRelationalBuilder:855` passes `m3SourceInformation` into the
  `DynaFunction` constructor).

That last point is the practical win: the same mistake that today produces
`[unsupported-api] The function 'X' … is not supported yet` at plan generation would produce
a compile error pointing at the exact character offset in the `Filter`.

**Producer 3 is why the check should not live only in Java.** A Java-side validator is blind
to `.pure`-source databases. Put the checking *algorithm* in a Pure function over the M3 graph
— walking `db._joins()._operation()`, `db._filters()._operation()` and view column mappings —
and make the Java validator a thin adapter that calls it and converts issues to `Warning` /
`EngineException` via `SourceInformationHelper.fromM3SourceInformation`, exactly as the
existing `sqlNull` warning does (`RelationalValidator:250-254`). A `core_relational` Pure test
can then sweep every `Database` in the graph through the identical rules, closing producer 3
without any change to the upstream jar.

### 10.2 Why the existing overload table is not enough

`getDynaFunctionTypeInferenceMap` (`relationalExtension.pure:189+`) keys a list of
(guard predicate, return-type function) pairs by function name, which *looks* like a
signature table:

```pure
Map<String, List<Pair<Function<{RelationalOperationElement[*] -> Boolean[1]}>,
                      Function<{RelationalOperationElement[*] -> DataType[0..1]}>>>>
```

It is not one. Of the 364 lambdas in it, **150 guards are literally `true`**, and only 13
mention `->size()` at all. `abs` is `{params | true}`; `add` indexes `$params->at(0)` and
`->at(1)`, so a wrong-arity call dies with a Pure index error rather than a diagnostic. And
the map is consumed only for View column typing, test-data generation and result-column
typing (`relationalMappingExecution.pure:207`, `testDataGeneration.pure:440`, `:912`, `:1156`)
— never for filters, never for joins, never for validation.

So the parameter contract has to be **added**, not merely surfaced. That is the main cost of
this proposal and should be planned for as such.

### 10.3 Recommended shape — mirror `SqlFunction`, do not reuse it

The newer SQL-dialect-translation subsystem already has a well-designed function descriptor, at
`.../legend-engine-xt-relationalStore-sqlDialectTranslation-pure/.../functionRegistry/functionRegistry.pure`:

```pure
Class <<typemodifiers.abstract>> …::functionRegistry::SqlFunction
{
  name          : String[*];
  variations    : SqlFunctionVariation[1..*];
  tests         : SqlFunctionTest[1..*];
  documentation : String[1];              // mandatory
}

Class …::functionRegistry::SqlFunctionVariation
{
  parameterTypes : Class<SqlType>[*];
  returnType     : Class<SqlType>[1];
  documentation  : String[0..1];
  identifier()   { … }: String[1];
}

Class …::functionRegistry::VariadicSqlFunctionVariation extends SqlFunctionVariation
[ hasAtLeastOneArgument: … ]
```

Copy this **shape** — mandatory `documentation`, one-or-more typed variations, a variadic
subclass, tests attached to the declaration. Do **not** reuse the types themselves:

- `SqlFunctionVariation.parameterTypes` is `Class<SqlType>[*]`, typed in **postgres-model
  SqlTypes**. The dyna path's types are `meta::relational::metamodel::datatype::*`. Wrong type
  lattice.
- `sqlFunctionRegistry()` is keyed by `Class<SqlFunction>`; dyna functions are keyed by
  `String` name, and ~118 dyna names have no `SqlFunction` to hang off.
- A fixed `parameterTypes` list cannot express what actually matters when validating authored
  grammar: "arity 2 or 3", "argument 1 must be a column reference", "argument 2 must be a
  string literal from a fixed domain" (`mostRecentDayOfWeek`).
- `VariadicSqlFunctionVariation`'s only constraint, `hasAtLeastOneArgument`, `println`s rather
  than failing — not a foundation for compile-time errors.

So: a dyna-specific descriptor table, living beside `dbExtension.pure` in `core_relational` so
that both the Java compiler and the Pure sweep can reach it. It should carry name, arity, and
argument *kind* (column reference / literal / literal list / nested operation / join chain);
keep `SqlFunction`'s **mandatory `documentation`**, which is what makes §11 self-documenting
rather than a bare pass/fail grid; and add one field `SqlFunction` has no need for:

- **`producers`** — `Grammar`, `Router`, or both. This expresses the §10.1 seam *in data* as a
  second line of defence: `pair` declares `Router` only, so grammar-authored use is reported,
  while router use is unaffected because it never reaches the validator.

Derive everything else from that table: registry membership, the documentation catalogue, and
the test table of §11.

Start with the names actually reachable from the DSL, not all 231. The 12 sugared names plus
the 10 empirically used ones (§6.2) is a tractable first tranche and covers the overwhelming
majority of real filters and joins.

**Be honest about the ceiling.** Name existence, arity, argument kind, literal domains and
producer are all achievable. **Relational datatype checking is not** — verifying that
`add(col, 'x')` is type-consistent needs the return type of nested dyna functions, which means
`getDynaFunctionTypeInferenceMap`, which covers only 158 of 231 names. Datatype validation is
gated on closing that gap; do not promise it in an early phase.

**Keep the lookup cheap.** Materialising a 231-entry descriptor map through the generated
`core_relational_*` statics on every `Database` compile is on the hot compile path. Memoize it
— once per `PureModel`, keyed by name into a map — and measure rather than assume.

### 10.4 Rollout must be warning-first

Models in the wild may already contain names or arities that a new check would reject, and a
compile error on an existing model is an outage. Stage it:

1. **Warn.** Emit `pureModel.addWarnings(...)` with `SourceInformation`, using
   `RelationalValidator:250-254` as the working precedent. Ship, then watch.
2. **Error.** Promote to `EngineException` with `EngineErrorType.COMPILATION` once the warning
   is quiet across CI and known model corpora.
3. **Keep one escape hatch**, keyed off the same declaration table — a name may be declared
   "known, unvalidated" so that adding a function is never blocked on writing its signature.

Names with no declaration should warn, never hard-fail, until step 2 — otherwise every
router-adjacent or externally-produced model becomes uncompilable on upgrade.

Promote in this order: **unknown name first**, then arity, then argument kind. Unknown name is
already a hard failure today (`dbExtension.pure:172`) — just a late, source-info-less one — so
promoting it moves an existing failure earlier and cannot break a model that works now. Arity
and kind are genuinely new judgements and will be wrong on some of the long tail; they should
sit as warnings for at least one release while the descriptor table is corrected against real
models.

**One backwards-compatibility trap.** A deployment may load a custom `DbExtension` that
registers dyna names outside the core enum, and those models work today. So "unknown name" must
mean *absent from the enum **and** from every loaded `DbExtension`'s dispatch table* — not
merely absent from the enum. Pair this with a severity flag
(`off` / `warn` / `strict`) so a deployment blocked by a bad descriptor can downgrade without a
rollback.

**Sync is one test, not a process.** A Pure test asserting
`DynaFunctionRegistry->enumValues().name->sort() == descriptors().name->sort()` makes
divergence impossible rather than merely discouraged: adding an enum value without a descriptor
fails the build, and so does the reverse. Java should read the canonical name set from the enum
at runtime (`PureModel.getEnumeration(...)`) rather than keeping a duplicate list.

---


## 11. Proposal — test coverage that documents itself

### 11.1 Scope: parse, compile, valid — deliberately not per-dialect

The goal is narrow and worth stating precisely, because it determines the whole design:

> For every dyna function, one checked-in row asserting that a canonical `###Relational`
> snippet using it **parses**, **compiles**, and produces **no unexpected errors or warnings**.

Not covered by *this* layer: whether a function renders on any particular dialect, which would
need a live server per dialect and a per-dialect exclusion manifest. A second, complementary
layer covering **execution on DuckDB** now exists — see §11.6. §11.5 states the residual risk
that remains after both.

PCT is also the wrong vehicle, for a structural reason rather than a scoping one. PCT starts
from a `<<PCT.test>>` **Pure function** and asserts every adapter produces the same answer. A
dyna function written in a Database `Filter` has no Pure function in front of it — it is a
store-level operation, unreachable from any `meta::pure::functions::*` entry point. PCT cannot
enumerate this surface at all.

### 11.2 Both harnesses already exist

Nothing new needs building. Two existing classes cover the two levels.

**Level 1 — the operation in isolation.** `TestRelationalOperationElementGrammarRoundtrip`
(`:37-60`) parses a bare operation, round-trips it through protocol JSON, composes it back, and
asserts the text is unchanged:

```java
protected static void test(String val, String expectedErrorMsg)
{
    RelationalOperationElement op = RelationalGrammarParserExtension
            .parseRelationalOperationElement(val, "", 0, 0, true);
    String json = objectMapper.writeValueAsString(op);
    operation = objectMapper.readValue(json, RelationalOperationElement.class);
    …
    Assert.assertEquals(null, val, renderedOperation);
}
```

This is the cheapest possible check and it catches the §9.1 corruption directly — a wrong-arity
operation must produce an error, not a comment string. It has **one** test case today (`:63`).

> **Fix its error path first.** The compose call and the final `assertEquals` run
> *unconditionally* after a caught parse error, when `operation` is still `null`.
> `renderRelationalOperationElement(null, …)` falls through every `instanceof` to
> `PureGrammarComposerUtility.unsupported(op.getClass(), …)` and throws an NPE; even surviving
> that, `assertEquals(val, null)` fails. So today only the round-trips-cleanly case works and
> the `expectedErrorMsg` parameter is effectively dead — which is why the class has exactly one
> test. Guarding the compose on `operation != null` is a two-line change.

**Level 2 — the operation inside a real `Database`, compiled.**
`TestRelationalCompilationFromGrammar` already extends
`TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite`, whose `test` helper is
exactly the "parses, compiles, and is valid" assertion:

```java
// TestCompilationFromGrammar.java:84
public static Pair<PureModelContextData, PureModel> test(
        String str, String expectedErrorMsg, List<String> expectedWarnings)
{
    PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(str);
    // full JSON re-serialisation round-trip when no error is expected
    …
    PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, …);
    modelData.getElements().parallelStream().forEach(pureModel::getPackageableElement);
    if (expectedErrorMsg != null) { Assert.fail("Expected compilation error …"); }
    if (expectedWarnings == null)
    {
        Assert.assertTrue("expected no warnings but found …", pureModel.getDefects().isEmpty());
    }
    …
}
```

It parses, re-serialises the protocol through JSON, compiles to a `PureModel`, forces every
element to be built, and asserts **no defects** unless the row declares them. That is the
definition of "valid" this proposal needs, already written and already used by 4,800 lines of
relational compilation tests.

The `expectedWarnings` parameter matters more than it looks — see §11.4.

### 11.3 The table

One row per dyna function, in `TestRelationalCompilationFromGrammar` (or a focused sibling
class), each row a small self-contained `Database`:

```java
test("###Relational\n" +
     "Database test::db\n" +
     "(\n" +
     "  Table firmTable (ID INT PRIMARY KEY, LEGALNAME VARCHAR(200))\n" +
     "  Filter f_in(in(firmTable.LEGALNAME, ['Firm A', 'Firm C']))\n" +
     ")\n");
```

That snippet is modelled on the working fixture at
`tests/mapping/inClause/testInClauseForJoinsAndFilters.pure:78`, so the shape is known to
parse and compile. Add the Level-1 round-trip row alongside it where the function has operator
sugar. Properties that follow:

- **The table is the documentation of the authoring surface.** It is the only place that says,
  in copy-pasteable form, how each dyna function is actually written — including the
  `functionOperationArgumentArray` (`[…]`) form, the `{target}` self-join notation, and which
  functions need the `= 'true'` boolean-coercion idiom of §5.
- **Coverage is enforced, not aspirational.** A name in the declaration table of §10.3 with no
  row fails the build, so the catalogue and its examples cannot drift apart.
- **Failure rows are documentation too.** A function that is *not* meant to be authorable —
  `pair`, and anything else declaring `producers = [Router]` — gets a row with an
  `expectedErrorMsg`, which records the intent in an executable form.

Cost is close to zero: no new module, no new framework, no test server, two existing classes.

### 11.4 The catch: this only documents *support* once §10 lands

This must be stated plainly or the suite will be over-trusted.

Today the compiler validates nothing (§3, §4). So
`Filter f(bogusFunc(t.a, t.b, t.c))` **passes** a parse-and-compile test. Right now the table
proves that a snippet is syntactically well-formed and survives protocol round-tripping — real
value, and it locks in the §9.1 fix — but it says nothing about whether the function works.

The two become the same thing only after §10's validation exists. Then:

- an unknown or wrongly-shaped dyna function fails to compile, so a passing row genuinely means
  "this is a supported way to write this function";
- `expectedWarnings` carries the warning-first rollout of §10.4 directly: during the warn stage
  a row records the exact warning text, and promoting to an error is a mechanical edit from
  `expectedWarnings` to `expectedErrorMsg` — the diff *is* the changelog of what got stricter.

Practical consequence for sequencing: build the table **with** the validation work, not before
it. Ahead of validation it is a syntax catalogue; after, it is the support contract.

### 11.5 What this leaves uncovered

Skipping per-dialect testing is a reasonable trade, but it leaves one real gap, and the
document should not pretend otherwise: **compiling successfully does not mean the query will
run.** Failure modes 2 and 6 of §8 — `[unsupported-api] The function 'X' … is not supported
yet`, and missing type inference — are inherently per-dialect and per-generation-state, and no
amount of parse-and-compile testing detects them. A `Filter` using a function that H2 renders
and Redshift does not will pass every row in this suite and fail at plan generation against
Redshift.

If that risk ever needs closing, the cheap version does not require test servers: the
per-dialect dispatcher tables are ordinary Pure maps, so a build-time test can reflect over
`DynaFunctionRegistry->enumValues()` against each dialect's dispatcher and assert renderability
statically. The idiom is already proven at `debugPrintExtension.pure:261-268`. That is a
strictly additive follow-on, not a prerequisite, and it is out of scope here.

**Update:** the risk is now closed *for DuckDB* by the execution layer in §11.6. Every other
dialect remains uncovered.

### 11.6 Execution layer — EMIT on DuckDB

`legend-engine-xt-relationalStore-emit/src/test/resources/relational-emit-models/relational-semistructured`
executes dyna functions against a real, in-process DuckDB. The model is named and tagged for the
capability it covers — modelled semi-structured data — not for the engine that happens to run it
or the IR node that implements it.

**How DuckDB is selected.** EMIT names no database anywhere — not in the `.emit.yaml`, not in
the runner. `RelationalConnectionFactory:187-189` picks DuckDB when `hints.isRelation()`, and
that hint comes from the *return type of the test-suite query*
(`MappingTestRunner:534-538` → `TestReturnTypeHelper.isRelationReturnType`). A query written
`->project([...], ['names'])` returns a TDS and gets H2; one written `->project(~[...])`
returns a `Relation` and gets DuckDB. No framework change is needed — only the query form.

That silent-fallback risk is real, so the model pins it down: it maps a property with `toJson`,
which has **no H2 rendering at all**. If the suite ever reverted to H2 the test would fail with
`[unsupported-api]` rather than passing quietly.

What it covers that nothing else did:

| | Notes |
|---|---|
| First emit model on DuckDB | the other 55 are all H2 |
| First `SEMISTRUCTURED` column in emit | becomes DuckDB `JSON` (`typeConversion.pure:78`) |
| Array indexing `orders[0].price` | the path regex always allowed `[n]`; nothing used it |
| Nested indexing `matrix[0][1]`, `items[0].tags[1]` | consecutive indices, and index→property→index |
| Dyna function inside a `Filter` predicate | no `Filter` in the emit corpus does this |

**Seed data.** JSON goes into a `Relation #{ … }#` block as CSV with doubled double-quotes
(`1,"{""legalName"":""Firm X""}"`). Expected JSON in an `EqualToJson` assert is a *Pure string
literal*, so a quote inside it needs `\\"` to survive both layers — the double-escaping is
easy to get wrong and the model demonstrates the working form.

### 11.7 Arrays and lateral flatten — what works, and the narrow gap

Array flattening from the Database DSL **already works**. The gap is narrower than it looks, and
sits in *where* you may write the call rather than in missing machinery.

**The lowering exists.** `applyJoinWithExplodeInCondition`
(`pureToSQLQuery.pure:9229-9337`, triggered at `:9159` by `explodeInCurrentJoinOperation`)
rewrites a grammar-authored `explodeSemiStructured` into a `SemiStructuredArrayFlatten` relation,
a `JoinTreeNode(lateral = true)`, and a `SemiStructuredArrayFlattenOutput` column named `VALUE`,
then substitutes the dyna node out of the predicate (`:9288`). After lowering the outer predicate
sees an ordinary subquery column, not a dyna function.

**So array-backed `[*]` properties are authorable today.**
`core_relational/relational/tests/semistructured/model/explodeSemiStructuredMapping.legend`
maps `trades: trade[*]`, `orders: order[*]` and a chained-join `products: String[*]`
(`:51-53`, `:215-217`) through joins whose conditions explode a JSON array (`:119-122`):

```
Join Block_Trade(extractFromSemiStructured(explodeSemiStructured(Semistructured.Blocks.BLOCKDATA,
    'relatedEntities', 'SEMISTRUCTURED'), 'tag', 'VARCHAR') = 'trade' and …)
```

The property mapping itself is unchanged from the ordinary one-to-many form —
`prop[setId]: [store]@JoinName`.

**What is actually still missing:**

| Gap | Where |
|---|---|
| Flatten fires **only** inside a `Join` operation | In a `View` column or `Filter`, `explodeSemiStructured` reaches `processDynaFunction` (`dbExtension.pure:869-899`, where it is deliberately absent), finds no `dynaFnToSql`, and fails |
| One explode per join | `assert($allExplodeCalls->size() == 1, …)` `:9236` — in-source comment: *"needs to be relaxed to support many-to-many"* |
| No nested explosion | `assert(…instanceOf(TableAliasColumn), …)` `:9239` — the operand must be a plain column |
| No array-valued scalar | `extractFromSemiStructured`'s whitelist (`dbExtension.pure:912`) admits no array/semi-structured return, so `array_*` stay unreachable outside the explode path |

**Lateral is fully supported in the SQL layer, but not equally.** `JoinTreeNode.lateral`
(`relational.pure:155` in legend-pure 5.94.0) is set only by the planner — three sites, all in
`pureToSQLQuery*` — and read only by emitters. Five dialects register a
`DbExtension.lateralJoinProcessor`:

| Dialect | Emits |
|---|---|
| DuckDB (`duckdbExtension.pure:39`, impl `:724-730`) | `, lateral <alias>` — comma-join form |
| Snowflake / Databricks | `inner join lateral <alias>`, no `ON` |
| **H2** (`h2Extension2_1_214.pure:32`, impl `:319-335`) | **no LATERAL at all** — a faked `left outer join … on ("a"."__INPUT__" = <navigation>)` |

H2's emitter asserts the join operation is exactly `1 = 1` *and* the right side is a
`SemiStructuredArrayFlatten`, so **H2 can only execute the flatten shape** — all seven
`tests::lateral::*` / `flatten::testFlatten_LateralJoin*` PCT tests are expected failures there
(`Cast exception: TableSubquery cannot be cast to SemiStructuredArrayFlatten`). DuckDB passes
every one: zero `lateral` entries across its PCT manifests.

Note there are **two** SQL emitters — `sqlQueryToString`'s `DbExtension` hook and
`sqlDialectTranslation`'s `NodeProcessor<LateralJoin>` — and only H2 implements the second.
Any authoring feature has to be rendered in both.

**Authoring a lateral join** would be grammar + compiler work only: no `lateral` token exists in
`RelationalParserGrammar.g4`, `Join` carries no lateral flag, and there is no protocol field —
but the metamodel already has one, so nothing upstream needs to change. `TabularFunction`
(`c3ba1be1e92`) is the precedent for shipping such syntax engine-first: its token is absent from
the upstream `legend-pure-m2-store-relational-grammar` parser.

**Higher-order array functions already push down.** `map` / `filter` / `fold` over a Variant
array are not dyna functions — they become `MapRelationalLambda` / `FilterRelationalLambda` /
`FoldRelationalLambda` (`pureToSQLQuery_variant.pure:669`, `:820`, `:636`) and render on DuckDB as
`apply(arr, lambda x : …)`, `filter(arr, lambda x : …)`, `reduce(arr, lambda acc, x : …, init)`
(`duckdbExtension.pure:685-722`). They are reachable from the **Pure/relation** path only; the
operation grammar has no lambda (`functionOperationArgument: operation | functionOperationArgumentArray`),
so they cannot be written in a `Filter` or `View`. They are also unreachable from a
*class-mapping* query — routing a Variant navigation through a mapped class fails with
`Error mapping not found for class Map`; the working vehicle is a relation-store accessor
(`#>{DB.TABLE}#`), where the array work happens inside the relation source.

### 11.8 Semi-structured limitations found by building the EMIT models

Six EMIT models under `relational-emit-models/relational-semistructured*` cover modelled
semi-structured data end to end on DuckDB. Authoring them turned up limitations that no existing
test records, listed here with the exact symptom. Each was reproduced against a live DuckDB, not
inferred from source. **None is fixed** — they are recorded so the next person does not rediscover
them, and the models deliberately do not assert the broken behaviour.

**Authoring surface** (traced to source, all enforced at SQL-generation time — neither
`extractFromSemiStructured` nor `explodeSemiStructured` is a grammar keyword, so the compiler
passes name, arity, path and return type through unchecked, `HelperRelationalBuilder.java:853-856`):

| Constraint | Where |
|---|---|
| Path must match `(ident\|[N]\|["quoted"])(.ident\|[N]\|["quoted"])*` — no `[*]`, no `[-1]`, no `..`, no `a."k"`, no `$.a` | `dbExtension.pure:918-921`, asserted `:910` |
| Return type is one of exactly 10 scalars; **no `ARRAY`/`SEMISTRUCTURED`/`VARIANT`/`JSON`** | `dbExtension.pure:912-913` |
| Neither check runs on the dialect-translation path | `toPostgresModel.pure:1055-1066` |
| `explodeSemiStructured` is legal **only inside a Join**; elsewhere it compiles then dies with `[unsupported-api] … is not supported yet` | `pureToSQLQuery.pure:9158`; `dbExtension.pure:1040` |
| At most one *unique* explode per Join; operand must be a plain `TableAliasColumn` of a table or view; source needs ≥1 primary key | `pureToSQLQuery.pure:9236`, `:9239`, `:9245-9248` |
| Array operations are unavailable across a **join-based** binding (`prop: Binding … : @Join \| …`) | `pureToSQLQuery_variant.pure:179-200`, in-source comment |

**Behavioural gaps, reproduced on DuckDB.** All four appear only in the *relation* query form
(`->project(~[...])`), which EMIT requires in order to route to a semi-structured-capable target.
The equivalent TDS forms work, which is why the parameterised suite passes 151/151 and still misses
every one of these.

1. **`fold` cannot be written in a relation colSpec.** `->fold({a, acc | $acc + $a.name}, '')`
   fails to parse — `no viable alternative at input '->project(~[…->fold({a, acc | …'`. The
   two-parameter *block* lambda is the problem, not `fold`; parenthesising does not help.
   *Workaround, used by the model:* declare the fold as a derived property in `###Pure`, where the
   ordinary domain grammar applies, and project the derived property.

2. **`sort` and `distinct` on a semi-structured array fail at the database.**
   `Binder Error: No function matches the given name and argument types 'array_sort(INTEGER)'` —
   the element is passed as a scalar where DuckDB needs a LIST. Same defect class as the
   JSON-vs-LIST cast bugs in §11.7. Neither operation is covered anywhere in the repo.

3. **`first()` and `exists()` do not collapse the flatten** when applied directly to a bound
   to-many property. For a firm with three addresses, `addresses->first().name` returns **three**
   rows (`A1`, `B2`, `A3`) instead of one, and `addresses->exists(a | $a.rank > 8)` returns three
   booleans instead of one. Both were isolated in single-column suites, so this is not an artefact
   of a co-projected column. Note `addresses->filter(…)->first()` behaves correctly — the defect is
   specific to the un-filtered form. `at(0)` and `filter(…)->size()` are correct.

4. **`->size()` over a join-backed to-many fails in relation form.**
   `column "ID" must appear in the GROUP BY clause or must be part of an aggregate function`.
   Counting elements produced by `explodeSemiStructured` works through a TDS `groupBy` but not
   through a relation projection.

5. **A fan-out column and an aggregate over the same collection cannot share one relation
   projection.** `~[code: r | $r.tags.code, n: r | $r.tags->size()]` fails with
   `column "REFERENCE" must appear in the GROUP BY clause`. Either alone is fine — this is
   narrower than (4), which fails even when the aggregate is projected on its own.

**What a binding can be attached to.** The operation on the right of a `Binding` is not limited to
a bare column, and the bound property is not limited to `[1]`. All of the following are supported
and now covered:

| Form | Example |
|---|---|
| bare column | `firm: Binding B : [db]T.PROFILE` |
| navigation to a sub-document | `firm: Binding B : extractFromSemiStructured([db]T.PROFILE, 'employment.firm', 'VARCHAR')` |
| `parseJson` over a plain VARCHAR column | `firm: Binding B : parseJson([db]T.PROFILE_JSON)` |
| through a join | `managerFirm: Binding B : [db]@Manager \| [db]T.PROFILE` |
| **to-many — an array of objects** | `tags: Binding TagB : parseJson(extractFromSemiStructured([db]T.CONTENT, 'payload.tags', 'VARCHAR'))` with `tags: Tag[*]` |
| sibling arrays off one column | as above, plus `participants: Binding ParticipantB : … 'payload.participants' …` |

The navigation form is what lets one class absorb documents of different shapes: a union whose
legs bind at different depths — one navigating to a sub-document, the other binding the root —
resolves onto a single class, and the query cannot tell the legs apart.

**Follow-up — the Snowflake `GET` failure is most likely this same cast.**
`Test_Relational_Snowflake_Semistructured` skips exactly two tests,
`union::testSemiStructuredUnionMappingWithBinding` and `…WithBindingAndFilter`, both with:

```
Invalid argument types for function 'GET': (VARCHAR(134217728), VARCHAR(8))
```

Databricks skips the same pair. That is precisely the **binding-fed-by-a-navigation** form: the
navigation is declared `'VARCHAR'`, so the renderer emits a primitive cast, and the property
access the binding then synthesises calls `GET(<varchar>, 'firmName')` — Snowflake's `GET` needs a
VARIANT/OBJECT, not a string. DuckDB passes because its arrow operator tolerates JSON text.

If that reading holds, the fix is the shape already used for `processVariantInstanceOf`: stamp
`avoidCastIfPrimitive = true` on a navigation whose result feeds a binding, so no
`::varchar` is appended and the value stays a document. Worth confirming against a live Snowflake
before changing anything — the whole model is asserted on DuckDB here, which cannot reproduce it.

**What does work**, and is now asserted: all 10 whitelist target types; bracket-quoted keys
containing a space and a dot; index-leading paths against an array-rooted document; four-level
typed navigation through a binding, including an absent optional branch; primitive, object, and
nested-inside-nested collections via implicit lateral flatten; `size`, `isEmpty`, `isNotEmpty`,
`at`, `filter`, `map`, `fold` (via a derived property), and — new ground — **`max` and `min`**;
explosion inside a join with a view supplying the exploded column; a join keyed on a scalar read
out of a document; and every binding source form in the table above.

---

## 12. Phased roadmap

**Phase 0 — Publish and instrument.** This document. Add the cross-check tests that turn
implicit drift into reviewable data: default `dynaFnToSql` ⊆ enum, each per-dialect union ⊆
enum, type-inference keys ⊆ enum with the 73-name delta emitted as a named gap list. Confirm
the `newMap` duplicate-key question (§7) and fix the five duplicates if it holds. Fix the bare
`UnsupportedOperationException` (`HelperRelationalBuilder:869`) and the unguarded `(Operation)`
casts (`:653`, `:728`) to typed `EngineException`s with `SourceInformation`. Zero behaviour
change beyond error quality, zero dependencies, and it establishes the true size of the gap
before anyone commits to filling it.

**Phase 1 — Make failures diagnosable.** Convert the plan-generation asserts (§8, rows 1, 2, 5
and 6) to carry `SourceInformation` and surface as `EngineException`. Fix §9.1 so a composer arity
mismatch fails loudly instead of writing a comment into the model — in **both** composers, the
Java one and `grammarSerializerExtension.pure:228-266`, or they drift further apart. No new
contracts required; this is the highest value-per-risk phase and should not wait behind the
contract work.

**Phase 2 — Declare contracts, with the table.** Add the descriptor table (§10.3) for the
Database-DSL-reachable names, with the enum ↔ descriptor sync test. Put the checking algorithm
in Pure, wire the Java adapter from the Database processor pass that populates `_operation()`,
and emit **warnings only** (§10.4). Build the §11.3 parse-and-compile table in the *same*
change: per §11.4 the table only documents support once validation exists, so the two belong
together — each declaration lands with its documentation, its example, and its test row.

**Phase 3 — Enforce.** Promote warnings to errors in the §10.4 order, which for the test suite
is a mechanical `expectedWarnings` → `expectedErrorMsg` edit per row. Then collapse the §7
registries onto the declaration table only if Phase 2 has shown the descriptor shape covers the
real cases — see §13 for why unification may not be worth doing at all.

Each phase is independently shippable and independently valuable: Phase 0 fixes bugs and makes
the gap visible, Phase 1 puts errors in the right place, Phase 2 gives authors feedback in
Studio plus an executable catalogue of how each function is written, and Phase 3 makes the
catalogue binding.

---

## 13. Risks and open questions

**Should the four registries actually be unified?** The tempting answer is yes; the honest
answer is probably no. They are not four copies of one thing — they answer four different
questions ("does this name exist", "what does it return", "how does it render in ANSI-ish SQL",
"how does it render *here*"). Collapsing them would force every dialect to declare an opinion
about all 231 names, which is both false and a large mechanical change with no user-visible
benefit: the per-dialect variation *is* the product. Prefer declaring `DynaFunctionRegistry`
canonical, adding the subset-check tests of Phase 0, and publishing the deltas as data.

**Two ANTLR implementations of one language, with no conformance test.** The engine grammar and
the upstream `legend-pure-m2-store-relational-grammar` grammar are independent implementations
of `###Relational`. §10.1's Pure-side check closes the validation gap, but only if someone runs
the sweep. The standing risk is larger than dyna functions: a future engine-side grammar change
can add syntax the upstream parser rejects, and nothing would catch it.

**Descriptor accuracy for the long tail.** Roughly 73 of the 231 names have no type-inference
entry and thin real-world usage. Descriptors written by reading `dynaFnToSql` format strings
will be wrong for some of them. Warning-only rollout is the mitigation; the risk is that nobody
reads the warnings and a later phase promotes a wrong rule to an error.

**Open questions**

- **Should unregistered names ever be permitted?** A store extension may want to introduce a
  dyna function without touching the core enum. Today the enum is a hard gate at
  `dbExtension.pure:172`, and §10.4 proposes consulting loaded `DbExtension`s as well. If
  first-class extensibility matters, the descriptor table needs its own extension point.
- **Is `producers` per-descriptor or per-signature?** Some names are legitimately both
  grammar-authored and router-synthesised with *different* arities — `case` is one. Per-signature
  is more correct and more expensive.
- **How much of the 118-name dialect-only gap is intentional?** Some entries are genuinely
  dialect-specific; others are almost certainly unfinished. Nothing in the proposed test scope
  distinguishes the two — that is the deliberate omission of §11.5, and closing it needs the
  static renderability sweep described there.
- **Who writes the documentation strings?** If `documentation` is mandatory on the descriptor,
  that is ~22 prose entries for the first tranche and eventually 231. It is the largest chunk of
  the contract work and the highest-value output; it probably warrants its own sub-phase with
  several contributors.
- **Is the `withNoDynaFunctionNames()` asymmetry (§2.4) intentional or calcified?** The comment
  at `HelperRelationalGrammarComposer.java:85` says intentional. Worth confirming before anyone
  "fixes" it, because it affects whether composer unification should also unify rendering.

---

## 14. See also

| Document | Relevance |
|---|---|
| [Router & Pure-to-SQL](router-and-pure-to-sql.md) §5.3–5.7 | `DbExtension`, `DynaFunctionToSql` dispatch, dialect registration, null-safe equality — the router-side producer |
| [ModelJoin](model-join.md) | `mergeSQLQueryData` and `processDynaFunction` interaction |
| [`docs/pct/wiring-howto.md`](../../pct/wiring-howto.md) `:47-60`, `:185-202` | Pure function → DynaFunction → SQL wiring, and the Postgres-model translation error |
| [Coding Standards](../standards/coding-standards.md) | `EngineException` / `SourceInformation` conventions referenced in §8 |
