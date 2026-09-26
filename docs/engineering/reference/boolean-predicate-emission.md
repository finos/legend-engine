# Boolean Predicate Emission

How a relational dialect that has no boolean literal distinguishes a SQL *predicate* from a boolean
*value*, and how the generator coerces between them.

---

## Summary

- Seven dialects set `isBooleanLiteralSupported = false`: **MemSQL, SparkSQL, Oracle, DB2,
  Sybase ASE, Sybase IQ, SqlServer**. All seven use the shared Boolean literal processor, so a
  boolean literal emits as the **quoted string** `'true'` / `'false'`.
- On those dialects the generator must know, for every element, whether it is a **predicate**
  (already `1`/`0`) or a **value** (a `'true'`/`'false'` string). It coerces values with `= true`.
  `isBooleanOperation` makes that call.
- Getting it wrong is silent, not loud. MySQL-family engines coerce `'true'` to `0`, so
  `1 = 'true'` is **false** and `0 = 'true'` is **true** — the predicate is inverted and the query
  returns the complement.
- The two directions fail differently: a **wrongly wrapped predicate** inverts; a **wrongly unwrapped
  value** matches nothing on MemSQL and is a syntax error on Oracle and SqlServer.
- A **stored boolean column** is a third representation: `0`/`1` in the table, while every boolean
  the generator emits on MemSQL is a string. A Boolean read straight off a `BIT`/`BOOLEAN` column is
  wrapped in the `booleanColumn` dyna function, which MemSQL renders as a conversion to the string
  form and every other dialect renders as the bare column.

---

## The mechanism

```
processDynaFunction / flattenCaseChain / processNot / dialect select renderer
        │
        ├─ maybeWrapAsBooleanOperation(e, sgc)   — no-op when isBooleanLiteralSupported
        │        │
        │        └─ wrapAsBooleanOperation(e, extensions)
        │                 │
        │                 └─ isBooleanOperation(e, extensions) ? e
        │                                                      : DynaFunction('equal', [e, Literal(true)])
        │
        └─ Literal(true) → Boolean literal processor, format '\'%s\'' → 'true'

pureToSQLQuery (dialect-agnostic)
        │
        └─ readStoredBooleanColumn(e) — a class property, a relation column access, or a table
                 accessor's projected column resolving to a TableAliasColumn of store type Bit/Boolean
                 → DynaFunction('booleanColumn', [column])
                        MemSQL:   case when col is null then null when col = 1 then 'true' else 'false' end
                        default:  col
```

`isBooleanOperation`, in `core_relational/relational/sqlQueryToString/dbExtension.pure`, answers
**true** for:

- a dyna function carrying the `predicate` stereotype (comparisons, `and`/`or`/`not`, `in`, `exists`,
  the null checks, `matches`, `regexpLike`, …);
- `group`, when it has exactly one operand and that operand is a predicate;
- `case` / `if`, when **every result operand** — the `then` values plus the trailing `else` — is a
  predicate;
- the `optionalVarPlaceHolderOpSelector` and `equalEnumOpSelector` FreeMarker holders.

Everything else is a value. Notably `sqlTrue` / `sqlFalse` are values — they emit the literal
strings — which is what makes the boolean-property shape
`case when <cond> then sqlTrue else sqlFalse end` correctly keep its `= true` wrap.

A case is therefore either wholly a predicate or wholly a value. When it is a value, `processCase`
converts its predicate arms so that every arm yields `'true'`/`'false'`. Mixed arms would otherwise
emit `1`/`0` from one and a string from another, and no single outer coercion reads both.

### Where the wrap is applied

| Call site | Applied by |
|---|---|
| operands of `and` / `or` | `processDynaFunction` |
| the condition of an `if` | `flattenCaseChain` |
| the operand of `not` | `processNot`, both branches |
| parameters of a non-boolean dyna function | `maybeWrapAsBooleanCaseOperation` (a `case`, not `= true`) |
| projected columns | `shouldWrapWithCase` in each dialect's `processSelectColumns*` |
| the WHERE / HAVING root | the dialect's own select renderer |
| a Boolean read off a `BIT`/`BOOLEAN` column | `readStoredBooleanColumn` in `pureToSQLQuery.pure`, at class-property resolution, at relation column access and at table-accessor projection; rendered through `booleanColumn` |

### Stored boolean columns

On MemSQL `1 = 'true'` is 0 and `0 = 'true'` is 1, so comparing a column against the string literal
inverts the predicate, and a bare column at the WHERE root fails as soon as the root is wrapped. The
column has to be read into the representation the rest of the query uses. `booleanColumn` does that
and nothing else:

- It is inserted only in `pureToSQLQuery`: when a class-mapping property resolves to a plain column,
  when a relation column access (`$x.flag`) resolves to a plain column, and when a table accessor
  (`#>{db.T}#`) projects its columns. The column's store type
  is the whole test. The Pure type is not consulted, because in the relation flow the router has
  already retyped the column with the store's precise primitive (`TinyInt` for a `Bit` column), so
  `Boolean` is not what reaches this point. A mapping expression such as `case(cond, 'true', 'false')`, a derived column of an
  earlier projection, and anything authored inside a Database join or filter are never wrapped,
  because they already carry the representation the dialect emits.
- Only a column whose store type is `Bit` or `Boolean` qualifies. Column types are rewritten to
  `Integer` on a subselect, which is what keeps derived columns out.
- The MemSQL rendering preserves NULL. `case when col = 1 then 'true' else 'false' end` would turn a
  missing value into `'false'`, so `isEmpty` would never match and `== false` would match NULL rows.
- `== false` renders `= 'false'`, which on the converted value behaves as `col = 0`, not `col != 1`.
  In MySQL any non-zero value is true, so "false" is exactly zero, and both forms drop NULL rows.
- The default rendering is the column itself, so SQL text on every other dialect is unchanged. The
  dialect-translation flow passes it through in `toPostgresModel` the same way.

---

## Emission by dialect

| Dialect | bool literal | WHERE root wrap | HAVING root wrap | Projection wrap | Projection form |
|---|---|---|---|---|---|
| MemSQL | `'%s'` | yes | yes | yes | `then 'true' else 'false'` |
| Oracle | `'%s'` | yes | yes | yes | `then 'true' else 'false'` |
| SqlServer | `'%s'` | yes | yes | yes | `then cast(1 as bit) else cast(0 as bit)` |
| DB2 | `'%s'` | yes | yes | yes | `then 'true' else 'false'` |
| SparkSQL | `'%s'` | yes | no | yes | `then 'true' else 'false'` |
| Sybase ASE | `'%s'` | yes | yes | no | — |
| Sybase IQ | `'%s'` | yes | yes | yes | `then 'true' else 'false'` |
| H2, DuckDB, Postgres, Snowflake, BigQuery, Databricks, Redshift, Spanner, Trino, ClickHouse, Presto, Hive | native | n/a | n/a | n/a | n/a |

---

## Classifying a dyna function

Whether a dyna function is a predicate is declared on its `DynaFunctionRegistry` entry with
`<<DynaFunctionReturn.predicate>>`, alongside the `collection` stereotype, and read back through
`extractEnumValue`. `DbConfig.dynaFuncDispatch` asserts that every dyna function reaching SQL
generation is on the registry, so the enum is the authoritative index and the lookup can rely on it.

A function that renders as a SQL condition rather than a value — a comparison, a membership test, a
regex match — **must** carry the stereotype. Without it, a dialect with no boolean literal will
compare the condition against `'true'` and invert it. A function that renders as a value must not
carry it: `booland`, `boolor`, `sqlTrue` and `sqlFalse` all read as boolean but emit
`'true'`/`'false'` strings, and need the coercion.

---

## Dialect constraints

Properties of the engines and of the current emission that bound what this layer can express.

**Oracle and SqlServer accept no boolean as a value.** `processCase` emits predicates as CASE
results (`case when c then (a = b) ... end`), which those two reject outright, so an
`if(cond, |pred, |pred)` is invalid SQL there however the result is coerced. Expressing it would mean
rewriting the CASE arms to values.

**`isNumeric` is classified as a predicate but does not render as one everywhere.** SparkSQL,
Sybase ASE and Sybase IQ emit `isnumeric(%s)`, which returns an integer rather than a condition. The
correction is a comparison (`isnumeric(%s) = 1`) in those registrations; dropping the stereotype
instead would wrap it as `isnumeric(x) = 'true'`, wrong in a different way. SparkSQL has no
`isnumeric` function at all, independently of this.

**SparkSQL does not coerce the HAVING root**, so a boolean value there is left bare.

**A `BIT` column reads as `Boolean`.** The relational compiler types a `Bit` column as `Boolean`, not
as the `TinyInt` precise primitive, so a relation column declared `flag:Boolean[1]` can be the whole
filter predicate (`filter(x | $x.flag)`). An untyped `#TDS` column is `[0..1]`, which `filter` does
not accept bare; declare the multiplicity in the header when the bare form is wanted.

**Sybase ASE never case-wraps a projected boolean** — it passes `conditionalExprAllowed = true`,
unlike the other six.

---

## Coverage

Behaviour is proved by PCT executing against real databases. Rendering is pinned at the element level:
a hand-built `RelationalOperationElement` is passed through `processOperation` for one dialect and only
that snippet is asserted, so a change elsewhere in the transpiler does not fail these tests. Build the
reactor and run a dialect's PCT suite to check behaviour.

| Test | Pins |
|---|---|
| `composition::testFilterWithBooleanIfPredicate` | a case with predicate arms stays a predicate |
| `composition::testFilterOnBooleanValuedExpression` | a boolean value at the WHERE root is coerced |
| `composition::testFilterWithMixedPredicateAndLiteralIf` | mixed arms agree on one representation |
| `regexpLike::testRegexpLike_Conjunction` | a predicate-returning function composed under `&&` |
| `composition::testFilterOnStoredBooleanColumn` | a `BIT` column compared, negated and null-checked, with a NULL row; fails on MemSQL without `booleanColumn` |
| `composition::testFilterWithStoredBooleanColumnAsIfCondition` | the reported `if` shape with a stored column as its condition |
| `composition::testFilterOnStoredBooleanColumnDirectly` | a `flag:Boolean[1]` column as the whole predicate, negated, and under `&&` |
| `composition::testFilterOnStoredBooleanProperty`, `testFilterWithStoredBooleanPropertyAsIfCondition` | the same shapes through a class mapping: instances stored in a table, the Boolean property mapped to its column |
| `sqlstring::testBooleanIfPredicateStaysAPredicate_MemSQL` | `and(pred, if(pred, pred, pred))` renders the `if` as a bare CASE on MemSQL |
| `sqlstring::testStoredBooleanColumnIsReadAsString_MemSQL`, `oracle::tests::testStoredBooleanColumnIsReadAsString_Oracle` | the `booleanColumn` rendering, alone and compared to `true`/`false` |
| `sqlQueryToString::testBooleanColumnRendersAsTheColumnByDefault` | the default rendering is the column itself |
| `pureToSqlQuery::testBooleanStoreColumnsAreReadThroughBooleanColumn` and sibling | only a `Bit`/`Boolean` column is wrapped |
| `isBooleanOperation` unit tests in `dbExtension.pure` | every verdict the above depend on, including a negative set holding `booland`/`boolor`/`sqlTrue`/`sqlFalse` as values |

The relation tests live in `core_functions_relation/relation/tests/composition.pure`, the regex one in
`core_functions_unclassified/string/regex/regexpLike.pure`, the dialect rendering ones in each dialect's
own module. Each asserts both directions, because the failure mode is inversion and a one-sided
assertion would still pass.

**SparkSQL, Sybase ASE and Sybase IQ have no PCT module**, so they are changed only where the change
is dialect-agnostic.

---

## Test harness

A boolean `#TDS` column reaches the relational PCT harness as a `Bit` column, and each dialect's DDL
translator has to know what to do with it: `BOOLEAN` on Postgres, ClickHouse, Databricks, Snowflake,
Trino and DB2, `BOOL` on Spanner, `NUMBER(1)` on Oracle, `BIT` on SqlServer and MemSQL. The CSV loader
writes a boolean as `true`/`false`; Oracle and SqlServer take `1`/`0` instead, through the `bit`
rendering the shared `convertValuesToCsv` now accepts. Until this was in place no relation test could
carry a boolean column, which is why the stored-column inversion above was invisible.

Oracle stores the column as a number and emits its boolean literal as a string, so it renders
`booleanColumn` exactly as MemSQL does; both use `booleanColumnAsString`. SqlServer converts `'true'`
to a bit on comparison and needs no rendering.

## See also

- [Router & Pure-to-SQL](../architecture/router-and-pure-to-sql.md) — the `sqlQueryToString` /
  `dbExtension` stack this lives in
- [Regular Expression Portability](regex-portability.md) — the same argument for leaving
  non-PCT dialects alone
- [`docs/pct/expected-failures-howto.md`](../../pct/expected-failures-howto.md) — manifest exclusions
