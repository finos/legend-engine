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

**Sybase ASE never case-wraps a projected boolean** — it passes `conditionalExprAllowed = true`,
unlike the other six.

---

## Coverage

Behaviour is proved by PCT executing against real databases, not by asserting on generated SQL text.
A SQL-text assertion pins incidental rendering and answers the wrong question; what matters is
whether the database returns the right rows. Build the reactor and run a dialect's PCT suite to check
it.

| Test | Pins |
|---|---|
| `composition::testFilterWithBooleanIfPredicate` | a case with predicate arms stays a predicate |
| `composition::testFilterOnBooleanValuedExpression` | a boolean value at the WHERE root is coerced |
| `composition::testFilterWithMixedPredicateAndLiteralIf` | mixed arms agree on one representation |
| `regexpLike::testRegexpLike_Conjunction` | a predicate-returning function composed under `&&` |
| `isBooleanOperation` unit tests in `dbExtension.pure` | every verdict the above depend on, including a negative set holding `booland`/`boolor`/`sqlTrue`/`sqlFalse` as values |

The relation tests live in `core_functions_relation/relation/tests/composition.pure`, the regex one in
`core_functions_unclassified/string/regex/regexpLike.pure`. Each asserts both directions, because the
failure mode is inversion and a one-sided assertion would still pass.

**SparkSQL, Sybase ASE and Sybase IQ have no PCT module**, so nothing here is demonstrable on them;
they are changed only where the change is dialect-agnostic. Oracle and DB2 have PCT modules whose
containers are harder to bring up than the rest — `OracleTestConnectionIntegration` and
`CustomDB2Container` hardcode their image coordinates instead of honouring
`legend.engine.testcontainer.registry`, so mirrored environments cannot pull them.

---

## See also

- [Router & Pure-to-SQL](../architecture/router-and-pure-to-sql.md) — the `sqlQueryToString` /
  `dbExtension` stack this lives in
- [Regular Expression Portability](regex-portability.md) — the same argument for leaving
  non-PCT dialects alone
- [`docs/pct/expected-failures-howto.md`](../../pct/expected-failures-howto.md) — manifest exclusions
