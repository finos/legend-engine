# Relation Mappings (`~func` / `~src`)

> **Audience.** Engine developers new to class-to-relation mappings
> (`RelationFunctionInstanceSetImplementation`). Covers the surface DSL and the
> concepts each stage of the pipeline is responsible for: grammar → compiler →
> validator → router → SQL generator, plus the variant / semi-structured lift
> path. Concrete class and file names are collected in the [file map](#12-authoritative-file-map)
> at the end — the body of the doc focuses on *what* and *why* rather than
> *how the code currently looks*, so it degrades gracefully as the code
> evolves.

---

## 0. What is a Relation mapping?

A **Relation mapping** (`Relation` keyword in the mapping grammar) binds a Pure
class to the output of a Pure expression that produces a `Relation<Any>`. The
source is a first-class Pure function (or inline expression), *not* a physical
table path.

Contrast with the classic `Relational` mapping:

| | `Relational` | `Relation` |
|---|---|---|
| Source | Physical table + joins (`~mainTable [db]TBL`) | Pure `Relation<Any>` expression |
| Column binding | `[db]TBL.COL` path | Column name (or lambda over `$row`) |
| SQL-gen input | Table alias tree | Evaluated sub-select |
| Type-checked source? | No — schema only | Yes — full Pure typing |

Two properties of the design are worth internalising up front:

1. **The source is compiled Pure.** Whatever produces the relation — an
   existing named function, or an inline `#>{db.TABLE}#->select(...)`
   expression — goes through the normal Pure compiler and carries a typed
   `RelationType` on its last expression. Everything downstream (row-type
   extraction, PK resolution, `$row`-lambda typing) reads off that
   `RelationType`.

2. **Property RHS is either a column name or a lambda over `$row`.** The bare
   `firstName: FIRSTNAME` form is *sugar* — the compiler lowers it to
   `{$row.FIRSTNAME}` so downstream code deals with a single shape (a
   `LambdaFunction` typed at the row type). Anything more expressive — string
   concatenation, arithmetic, semi-structured navigation — is just a longer
   lambda body.

Read those two ideas back into every stage below and the pipeline is mostly
obvious.

A property lambda actually sees **two** injected variables: `$row` (the
current row) and `$src` (this class's own **relation** — the whole source,
not a row). `$src` only matters for the join constructs in
[§2.11](#211-relationship-join-property-mappings) — everywhere else, `$row`
is the only variable you need.

---

## 1. DSL at a Glance

The block-type keyword `Relation` inside `###Mapping` opens a relation class
mapping. Skeleton:

```
Relation
{
  <source>            // ~func <descriptor>  OR  ~src <expression>
  ~primaryKey: <cols>?  // optional; auto-inferred if omitted
  <propertyMappings>?
}
```

Where a **source** is one of:

- `~func myPkg::personFunc():Relation<Any>[1]` — reference an existing Pure
  function.
- `~src #>{db.PERSON}#->filter(p | $p.AGE > 30)` — an inline zero-arg
  expression that evaluates to a `Relation<Any>`. The parser wraps it in a
  synthetic lambda so the compiler can treat both forms uniformly.

A **property mapping** is one of:

- `propName: COLUMN` — bare column (sugar, lowered to `{$row.COLUMN}`).
- `propName: $row.COLUMN + '-' + $row.OTHER` — arbitrary Pure expression over `$row`.
- `propName: EnumerationMapping <id> : COLUMN` — enum-typed property.
- `propName: Binding <path> : COLUMN` — semi-structured (JSON / binary) property.
- `propName ( ... )` — normal embedded (child columns from the same relation).
- `propName () Inline [<setId>]` — inline embedded (delegates to another set).
- `+localProp: Type[mult] : <rhs>` — local (mapping-scoped) property.
- `propName: modelJoin({l,r| <cond>})` — class navigation through an inline,
  model-level join (the condition is over the two classes' properties, like an
  association `ModelJoin`'s), target class mapping resolved from the property's
  declared type (see [§2.11](#211-relationship-join-property-mappings)).
- `propName: $src->navigationJoin(<targetRelationFn>, {l,r| <cond>}).COL` /
  `propName: $src->filterJoin(<targetRelationFn>, {l,r| <cond>}).COL` — a
  column reached through an inline join (see
  [§2.11](#211-relationship-join-property-mappings)).

The class mapping header may extend another Relation class mapping,
`*Class[id] extends [parentId]: Relation { ... }`, inheriting the property mappings
it does not map itself (see [§2.12](#212-extending-a-class-mapping-extends)).

Concrete ANTLR is in `RelationFunctionMappingParserGrammar.g4`; the walker in
`RelationFunctionMappingParseTreeWalker.java` produces the protocol POJOs.

---

## 2. Examples by Feature

Each example is a minimal illustration. They compose freely.

### 2.1 Primitive columns with `~func`

```
###Pure
Class myPkg::Person       { firstName: String[1]; age: Integer[1]; }
function myPkg::personFunc(): Relation<(FIRSTNAME:String, AGE:Integer)>[1]
{ #>{myDb.PERSON}#->select(~[FIRSTNAME, AGE]) }

###Mapping
Mapping myPkg::PersonMapping
(
  *Person: Relation
  {
    ~func myPkg::personFunc():Relation<Any>[1]
    firstName: FIRSTNAME,
    age:       AGE
  }
)
```

`Person.all()->filter(x | $x.age > 30)` evaluates the function to a
`SelectSQLQuery`, wraps it in a sub-select, and applies the filter on top.

### 2.2 Inline expression source (`~src`)

```
*Person: Relation
{
  ~src #>{myDb.PERSON}#->select(~[FIRSTNAME, AGE])
  firstName: FIRSTNAME,
  age:       AGE
}
```

Handled identically to `~func` after parse (the parser wraps the expression in
a synthetic zero-arg lambda).

### 2.3 Explicit primary key

```
~primaryKey: ID              // single column
~primaryKey: [FIRST, LAST]   // composite
```

Omit `~primaryKey` to let the runtime infer it from the function body — see
[§8](#8-primary-key-inference).

### 2.4 Property RHS as a Pure expression

```
firstName:   $row.'FIRST NAME',
ageInMonths: $row.AGE * 12,
greeting:    'Hello ' + $row.'FIRST NAME'
```

Compiled the same way as bare columns; only the body of the synthesised lambda
differs.

### 2.5 Local (derived) property

```
+displayAge: String[1] : AGE_DISPLAY
```

Extends the class in the mapping scope only; the canonical Pure class is
unmodified.

### 2.6 Semi-structured column via `Binding`

```
address: Binding myPkg::AddressBinding : ADDRESS_JSON
```

The binding's model unit must include the property's return type. At
SQL-generation time this becomes a `SemiStructuredEmbeddedRelationalInstanceSetImplementation`
(see [§9.3](#93-property-navigation-and-the-rfpm-lift-path)).

### 2.7 Enumeration mapping

```
###Mapping
Mapping myPkg::EmployeeMapping
(
  *Employee: Relation
  {
    ~func myPkg::employeeFunc():Relation<Any>[1]
    name:         NAME,
    employeeType: EnumerationMapping empTypeMap : EMP_TYPE
  }

  EmployeeType: EnumerationMapping empTypeMap
  {
    CONTRACT:  'CONTRACT',
    FULL_TIME: ['SALARY', 'FULL_TIME']
  }
)
```

graphFetch decodes an enum property exactly as it does a `###Relational` one: the SQL selects the store
value, to-one and to-many alike, and the generated Java maps it through the `EnumerationMapping`, failing
on a value the mapping does not list. As on `###Relational`, an enum property with no `EnumerationMapping`
cannot be fetched or compared to an enum value. A TDS projection instead decodes in SQL, so an unlisted
value comes back null.

### 2.8 Normal embedded mapping

```
*PersonWithAddress: Relation
{
  ~func myPkg::personFunc():Relation<Any>[1]
  firstName: FIRSTNAME,
  address
  (
    street: STREET,
    city:   CITY
  )
}
```

Child columns come from the same relation as the parent.

### 2.9 Inline embedded mapping

```
*PersonWithAddress[personSet]: Relation
{
  ~func myPkg::personFunc():Relation<Any>[1]
  firstName: FIRSTNAME,
  address () Inline [addressSet]
}

*Address[addressSet]: Relation
{
  ~func myPkg::personFunc():Relation<Any>[1]  // may even be a *different* function
  street: STREET,
  city:   CITY
}
```

The sub-object is fully independent — different function, different columns.

An inline target may itself declare an `Inline` property, and a `modelJoin` declared in the innermost target
resolves, under a single or a union owner, as it does one level up.

### 2.10 Union mapping

```
*Person: Operation { meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(rfSet1, rfSet2) }

*Person[rfSet1]: Relation { ~func myPkg::personSet1Func():Relation<Any>[1]  ... }
*Person[rfSet2]: Relation { ~func myPkg::personSet2Func():Relation<Any>[1]  ... }
```

Mixed Relation + Relational leaves are supported. **All leaves must resolve to
the same store** — the router rejects cross-store unions.

### 2.11 Relationship (join) property mappings

A Relation mapping's analogue of classic `###Relational` `@join`: a
**one-directional, per-property** join, declared inline on the property (no
`Association` required). `$src` here is the class's own **relation** — the
left operand of the join — distinct from `$row` (the current row).

Three constructs, split by what the property yields:

| Construct | Yields | Keeps | Target reference |
|---|---|---|---|
| `modelJoin` | another **mapped class** | — | resolved from the property's declared type (model level, no set-id reference) |
| `navigationJoin` | a column from the **joined (right)** side | right columns (chainable) | relation function |
| `filterJoin` | a column from **this (left)** side, gated by the join | left columns (chainable) | relation function |

**Join kind.** `navigationJoin`/`filterJoin` take an optional `JoinKind` argument between the
target and the condition — the same enum and position as the pre-existing TDS `Relation::join(rel2,
JoinKind.LEFT, cond)`: `$src->navigationJoin(demo::firmFn(), JoinKind.INNER, {p,f | ...})`. The
default varies by construct, matching each one's defining semantic: `navigationJoin` (and a single
`modelJoin`, which has no surface join-kind syntax at all) defaults `LEFT` — the join decides the
property's *value*, not whether the instance exists, so a source row with no match keeps its row and
the property is simply empty for it (projected as `null`; a filter on the property excludes it), the
same as a `###Relational` join property. `filterJoin`'s defining semantic — keep only source rows
that match a row of target — is itself an inner filter, so it defaults `INNER`; an explicit kind
always overrides either default, on any hop of a chain. An `INNER` hop after the first one behaves like
`###Relational`'s `@J1 > (INNER) @J2 | col`: the source row is kept, and the property is empty for it when
that hop finds no match. So `firstName : ... .FIRSTNAME` through a `filterJoin` is not merely
"the source column" — it is empty for rows with no matching target row (a plain source-column mapping
would not be), while a row failing the filterJoin's own gate is excluded from that gate's candidate
set the way a `###Relational` filter would be. `JoinKind.RIGHT`/`FULL` are not supported.

```
###Mapping
Mapping demo::PersonMapping
(
  *Person[person]: Relation
  {
    ~func demo::personFn():Relation<Any>[1]
    firstName : $row.FIRST_NAME,
    +firmId   : Integer[1] : $row.FIRM_ID,
    // class navigation — a model-level condition over Person's and Firm's properties (like an
    // association ModelJoin's); the target class mapping is resolved from the property's declared
    // type (the single root class mapping, of any store, whose class matches it exactly):
    firm      : modelJoin({p,f | $p.firmId == $f.id}),
    // column from the joined (right) side — target is a relation function:
    firmName  : $src->navigationJoin(demo::firmFn(), {p,f | $p.FIRM_ID == $f.ID}).LEGAL_NAME
  }

  *Firm[firm]: Relation
  {
    ~func demo::firmFn():Relation<Any>[1]
    id        : $row.ID,
    legalName : $row.LEGAL_NAME
  }
)
```

**Chaining.** `navigationJoin`/`filterJoin` each return one side's columns
only, so calls chain, with a terminal `.col` extracting the value:

```
firmCity : $src->navigationJoin(demo::firmFn(),    {p,f | $p.FIRM_ID    == $f.ID})
               ->navigationJoin(demo::addressFn(), {f,a | $f.ADDRESS_ID == $a.ID})
               .CITY
```

Because each hop keeps only one side's columns, there is no source/target
column-name collision to resolve — self-joins (e.g. `person -> manager`,
both sides backed by the same relation function) fall out for free; the SQL
layer aliases the two occurrences distinctly.

**Operands.** The source of the first `navigationJoin`/`filterJoin` of a chain is `$src`, optionally
transformed by `extend`, `filter`, `select`, `rename` and `sort` (see **Source steps** below); the source of
each later hop is the previous join. A `navigationJoin`/`filterJoin` target can be any expression that evaluates to a
`Relation` — a bare function call (`demo::firmFn()`), one with arguments, an
operator chain, or a more complex expression; SQL generation processes it as a
plain value specification (the same way TDS v2's own `Relation::join()`
processes its right-hand side), so no particular expression shape is
required.

**Source steps.** `$src->extend(~K: x | $x.FIRM_ID + 1)->filter(x | $x.ACTIVE == 'Y')->navigationJoin(demo::firmFn(), {p, f | $p.K == $f.ID}).LEGAL_NAME`
joins from the class mapping's own relation after the steps, in the order written. The steps are the
row-preserving ones — each source row survives exactly once, so the join result can still be matched back to
the mapped row — and only on `$src`, ahead of the first join:

- The join condition's first parameter ranges over the transformed relation: an `extend` column (a window
  function included) is visible to it and to every later step, `rename` moves the renamed column to the end,
  and `select` narrows what it sees.
- `filter` gates the join, never the mapped instance: a source row the filter removes keeps its instance and
  gets no joined row, so the joined columns are empty for it. It is not a `where` on the class mapping's rows.
  A window `extend` ahead of a `filter` runs first, so `filter(x | $x.RN == 1)` keeps the first row per
  partition.
- The join result is matched to the owning row by the mapping's `~primaryKey` (declared or inferred), so the
  transformed relation must still carry every primary key column. A `select` or `rename` that drops one fails
  at plan generation, naming the missing column and the columns that are left.
- legend-pure decides which steps may precede the first join (`extend`, `filter`, `select`, `rename` and `sort`) and
  rejects any other function there (`groupBy`, `distinct`, `join`, `project`, `concatenate`, `limit`, `drop`, ...), steps
  between two joins or after a join, and steps that do not lead into a join (`$src->extend(...)->size()`: use `$row`
  for the mapped row's columns) in a `.pure` source. The engine lowers whatever sits between `$src` and the first join
  and does not repeat those checks for a model compiled from protocol: `limit` would cut the source copy to its first
  rows and leave every other row with an empty joined column.
  A step's lambdas range over the source rows and use neither `$row`, `$src` nor a join chain.

**Placement.** A `navigationJoin`/`filterJoin` chain is either the property's entire value, or
followed by a `.col` — and a chain followed by `.col` may appear anywhere in the value expression,
as a `###Relational` `@J | col` may: inside `if` branches, comparisons, `isEmpty`/`isNotEmpty`,
`coalesce`, arithmetic, string `+` and `joinStrings`, alongside the mapping's own `$row` columns,
and several chains in one expression. The expression is evaluated on the source row, so a source row with no joined row still
gets the expression's value (`if(<chain>.CODE == 'Y', | true, | false)` is `false`, not empty, when
nothing matches), and each chain's join is added only when a query reads the property:

```
isLiquid : if($src->navigationJoin(demo::classificationFn(), {p, c | $p.ID == $c.PRODUCT_ID}).CODE == 'Y', | true, | false),
issueDate: if($row.SUB_TYPE == 'Future', | $src->navigationJoin(demo::futureFn(), {p, f | $p.ID == $f.ID}).ISSUE_DATE,
              | if($row.SUB_TYPE == 'Option', | $src->navigationJoin(demo::optionFn(), {p, o | $p.ID == $o.ID}).ISSUE_DATE, | []))
```

A chain not followed by `.col` must be the whole value, and a chain may not appear in another
join's arguments (its target or condition). `$src`, with or without steps, may appear only as a chain's root source, and
`$row` may not appear in any join argument (the join is over the whole source relation, not the
mapped row); a nested lambda's own `{row | ...}` is unaffected. An explicit `JoinKind` must be a
literal (`JoinKind.INNER`), not a computed expression.

These rules, and the `modelJoin` rules in [§5.5](#55-join-property-mappings), are defined and
enforced by legend-pure's Relation-mapping compiler (`RelationFunctionInstanceSetImplementationProcessor`,
documented in legend-pure's `mapping-grammar-reference.md` §15.10), which rejects a `.pure` source at compile
time. A model compiled from protocol never passes through that compiler, and the engine does not repeat its checks:
as with association `ModelJoin`, the engine's Java compiler only fails where it cannot build the mapping. A protocol
model that violates these rules gets whatever SQL generation produces for it, except for two cases that would
silently produce wrong SQL or lose the owning row's key, which are asserted at plan generation when a query reaches
the property:

- **`$src` is only a chain's root.** legend-pure rejects any other use of `$src` in a `.pure` source. For a model compiled
  from protocol, an unbound `$src` reaching SQL generation from a property value (`$src->size()`,
  `$src->filter(...)->size()`) is rejected in `processVariableExpression`; otherwise it would generate invalid or wrong
  SQL (`$src->size()` becomes a `count(*)` without a `GROUP BY`, the filter is dropped). The error carries no grammar
  source position. A `$src` inside a join's own condition or target is not caught here.
- **A transformed `$src` keeps every primary key column.** The join result is matched to the owning row by
  `~primaryKey`, so a transformation that drops one fails in `applyRelationMappingJoinSourceSteps`, naming the column.

`sort` is accepted as a step and has no effect: the join is matched back by primary key, so row order cannot change
the result (see §9.5).

**Possible future enhancement: aggregates over a join.** A join chain is row-level: `.COL` reads one value from the
joined row, so `$src->navigationJoin(...).COL` followed by `->size()`, `->sum()` or a similar aggregate is not
supported (`chain.COL->size()` compiles but is always 1). `Relational` mappings have no equivalent either: they
aggregate over a to-many property in the query, and a Relation mapping does the same through a `modelJoin` to a class
with a to-many association. Supporting an aggregate over the chain inside a property expression (`size`, `isEmpty`,
`exists`, `sum`/`min`/`max`/`avg` over a column) would need a compile rule that allows a chain as the first argument
of those functions and an aggregated keyed sub-select (`GROUP BY` the primary key, `COALESCE` to 0 for rows without a
match) in SQL generation.

**Qualified `modelJoin`.** `modelJoin(...)` and `meta::pure::functions::relation::modelJoin(...)`
are equivalent. Exporting a compiled inline model join always emits the qualified name; the
compiled metamodel does not store the original call spelling.

**Milestoning.** Every milestoned table a `navigationJoin`/`filterJoin` reaches is read at the
query's milestoning dates, as a `###Relational` join's target is: each hop of a chain, the source
copy a chain or `filterJoin` is keyed on, and accessors nested inside a target expression (a
`->join(...)`'s right side, a `->filter(...)` over an accessor). Processing-, business- and
bi-temporal tables are filtered at `%latest` and at a point in time alike.

**Typed targets.** The condition lambda's parameters are typed from the two
operands' row types, so the condition can only name a target's columns when
the target declares them — e.g. `function demo::firmFn(): Relation<(ID:Integer[1],
LEGAL_NAME:String[1])>[1]`. Against a target typed `Relation<Any>`, a column
reference fails as an unknown property of `Any`. (The `~func ...:Relation<Any>[1]`
in a class mapping header is only a descriptor and is unaffected.)

**`.col` is the value terminal.** `->select(~col)` on a `Relation` returns a
single-column `Relation`, never a scalar, so it can't satisfy a scalar
property on its own. The row-style `.col` accessor performs the
relation-to-value reduction, mirroring `$row.col`, and only resolves in a
relation-mapping property value. `navigationJoin`/`filterJoin` compile
anywhere, as in legend-pure, but are only lowered to SQL from a mapping and
fail if executed in memory.

**Multiplicity.** The declared property multiplicity governs, via the same
`Multiplicity.subsumes()` check every relation-mapping property uses, but — as
with classic `###Relational`'s `@Join | Col` — the compiler does not try to
prove a `[1]`/`[0..1]` property's join is actually to-one: equating the
target's primary key in the condition doesn't guarantee a single match any
more than any other condition shape does, so there is no PK-keying gate here.
A non-unique match on a to-one property is a mapping error the mapper is
responsible for avoiding, same as `###Relational`. A to-one property over a
join with no match is simply empty for that row, exactly as with
`###Relational`.

**Source primary key.** A `filterJoin`, or any chain of
`navigationJoin`/`filterJoin` calls, needs the primary key of the class
mapping it is declared on (explicit or inferred); plan generation fails with a
"Declare ~primaryKey" error when it can't be determined. A single
`navigationJoin` has no such requirement. See [§9.5](#95-join-sql-generation)
for why.

**Limitations.**
- Both compilers: the join constructs are compiled by the engine's Java
  compiler and by legend-pure's own compiler (Pure IDE, PAR-built `core-pure`
  mapping fixtures such as `relation/inlineJoin.pure`), with matching rules and
  error messages where the engine has to fail. legend-pure defines and checks
  what is supported; the engine adds no checks of its own.
- A `modelJoin` target must be the single root class mapping of the
  property's exact class, in the mapping or its includes (of any store).
  Operation mappings count when deciding the root, as for `*`: a starred
  Operation is the root and is rejected as unsupported, rather than an
  unstarred instance class mapping beside it being picked instead.
- No cross-store joins. graphFetch traverses `modelJoin`, `navigationJoin` and `filterJoin` properties like any
  other property (EMIT models `relation-inline-join-graphfetch`, `relation-graphfetch-enum` and
  `relation-class-mapping-extends`).
- A `modelJoin` target may be a `###Relational` class mapping with a `~filter`, non-temporal or processing-temporal:
  project, `filter`, `isEmpty` and graphFetch of a to-one, nested or to-many target return the rows the same
  properties return from a `###Relational` source (EMIT model `relation-inline-join-filtered-target`).

**Comparison with `###Relational`:**

| Concept | `###Relational` | Relation mapping |
|---|---|---|
| Where the join is declared | Named `Join` inside the `Database` | Inline condition lambda on the property |
| Class navigation | `firm : @Person_Firm` | `firm : modelJoin({p,f\|cond})` (condition over the classes' properties) |
| Column from the joined side | `firmName : @Person_Firm \| LEGALNAME` | `firmName : $src->navigationJoin(firmFn(), {p,f\|cond}).LEGALNAME` |
| Join chains | `@j1 > @j2` | repeated `navigationJoin`/`filterJoin` |
| Join kind | `(INNER)`/`(OUTER)` on the pointer | optional `JoinKind` argument on `navigationJoin`/`filterJoin` (default `LEFT`/`INNER` respectively — see above); `modelJoin` has no surface syntax, always effectively `LEFT` |

The structural difference: `@join` *references* a join pre-declared against
the `Database` schema; the relation-mapping form *carries the condition
inline*, because a Relation mapping's source is an arbitrary Pure function,
not a schema-backed store with a place to register named joins.

`modelJoin` reuses the existing `ModelJoinPropertyMapping` (the same class
XStore associations lower to — see [`model-join.md`](model-join.md));
`navigationJoin`/`filterJoin` are relation functions (declared in legend-pure) that only lower to SQL from a mapping.
Neither requires a metamodel change. See [§5.5](#55-join-property-mappings),
[§6](#6-validate), and [§9.5](#95-join-sql-generation) for how each stage
handles them.

### 2.12 Extending a class mapping (`extends`)

```
*Product[product]: Relation
{
  ~func productRel():Relation<Any>[1]
  id: ID,
  issuerName: $src->navigationJoin(issuerRel(), {p, i | $p.ISSUER_ID == $i.ID}).NAME,
  +productKey: Integer[1]: ID,
  sharesOutstanding: modelJoin({p, s | $p.productKey == $s.productId})
}
*Stock: Operation { meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(commonStock, preferredStock) }
Stock[stock] extends [product]: Relation { ~func stockRel():Relation<Any>[1]  stockClass: STOCK_CLASS }
*CommonStock[commonStock] extends [stock]: Relation { ~func commonStockRel():Relation<Any>[1] }
*PreferredStock[preferredStock] extends [stock]: Relation { ~func preferredStockRel():Relation<Any>[1]  name: 'Preferred ' + $row.NAME->toOne() }
```

A Relation class mapping can extend another Relation class mapping of the same
class or of the class's single direct superclass. legend-pure's
`MappingValidator` enforces this (`CommonStock extends [product]` is rejected
because `Product` is not `CommonStock`'s direct superclass); the engine compiler
does not check it, for any mapping type. The extending mapping still declares its
own `~func`/`~src`, and inherits every property mapping of the extended sets that it
does not map itself: columns, `$row` expressions, local properties, embedded and
`Inline` properties, `navigationJoin`/`filterJoin` and `modelJoin`. A property it
maps itself overrides the inherited one. Local properties are not inherited
*as properties*: a set's own property mappings can only use the locals it
declares, so a `modelJoin` declared on `CommonStock` cannot read `productKey`.

An inherited property mapping keeps the extended set as its owner and source
set id (`allPropertyMappings`, the same as for classic mappings). Its columns are
read, by name, from the relation of the set being queried, so that relation must
expose the columns the inherited mappings read. A `modelJoin` condition is routed
against the set that declares it ([§7](#7-route--store-contract)). A
`filterJoin` or chained join builds its keyed sub-select from the declaring
set's relation and joins it back on that set's primary key
([§9.5](#95-join-sql-generation)), so it finds a value only for rows that the
declaring set's relation contains as well (the usual case: the extending
relation narrows its parent's).

Queries behave as with a `###Relational` mapping of the same `extends`
hierarchy, including that `subType` does not narrow the rows:
`Product.all()->project(x | $x->subType(@Stock).sharesOutstanding.amount)` reads
the inherited `modelJoin` for every product, because the union's leaves resolve
to the one inherited property mapping. If the leaves each map the property
themselves instead, the union does not collapse and that query fails in both
mapping types.

---

## 3. Pipeline Overview

```
   Mapping grammar text
         │
         ▼  1. Parse         → protocol POJOs (RelationFunctionClassMapping, ...)
         │
         ▼  2. Compile       → Pure graph (RelationFunctionInstanceSetImplementation, ...)
         │                     (4 sequential passes, each adding a layer)
         │
         ▼  3. Validate      → structural + type/multiplicity errors surface here
         │
         ▼  4. Route         → attaches store contract, rewrites function body
         │                     into a ClusteredValueSpecification
         │
         ▼  5. Generate SQL  → sub-select for `getAll`, RFPM → downstream PM
                               synthesis at property-navigation time
```

Each stage takes the previous stage's output as-is; there's no branching or
back-flow. Data shapes:

| Stage output | Concept |
|---|---|
| Protocol | Serialisable JSON model. `~func` vs `~src`, `column` vs `valueFn` are mutually-exclusive fields at this layer. |
| Pure graph | `_relationFunction` typed `FunctionDefinition` (either resolved or freshly compiled), each property with `_valueFn` (a `LambdaFunction` typed at the row type). |
| Routed graph | Same shape, but `_relationFunction.expressionSequence` is a `ClusteredValueSpecification` carrying the target store. |
| SQL cursor | `SelectWithCursor` wrapping a `SelectSQLQuery` — this is the value threaded through the rest of query compilation. |

---

## 4. Parse

The grammar walker produces protocol POJOs and does two normalisations to make
downstream stages uniform:

1. **`~src` is wrapped in a zero-arg synthetic `LambdaFunction`** stored in
   `sourceLambda`. The `~func` form stores a `PackageableElementPointer` in
   `relationFunction`. Only one of the two is ever set.
2. **Bare-column property RHS is preserved as a plain string** (`column`);
   anything else parses as a full expression into a `LambdaFunction`
   (`valueFn`). The bare form gets lowered later in the compiler — parse-time
   just captures it verbatim.

Embedded property mappings have two shapes in the same POJO:

- **Normal embedded** — nested `propertyMappings` populated, `id`/`setImplementationId` empty.
- **Inline embedded** — empty `propertyMappings`, `id`/`setImplementationId` naming a peer class mapping.

Sub-properties inside a normal-embedded block are parsed with the **outer**
class stamped on their `property._class` (the parser has no type context).
Correcting that stamp is a compiler responsibility — see [§5.3](#53-embedded-property-mappings-and-the-_class-rewrite).

---

## 5. Compile

Four sequential passes run per class mapping. Each pass adds one layer of
information; splitting them is what allows the function body to be typed
*before* PK columns are resolved, and typed valueFn lambdas to exist *before*
validation runs.

| Pass | Adds |
|------|------|
| Prerequisite | Declares the mapped `Class` and (for `~func`) the referenced function as compilation prerequisites, so the function's typed `RelationType` is available before later passes need it. |
| First | Creates the `RelationFunctionInstanceSetImplementation` node, walks property mappings to build **skeleton** M3 property-mapping objects (no lambdas yet), and folds local properties into a per-mapping synthetic class. |
| Second | Resolves the source (`~func` by descriptor, `~src` by inline compilation), attaches it as `_relationFunction`, extracts the row `GenericType` from the function's last expression, and builds each property's `_valueFn` lambda with `$row` bound to that row type and `$src` bound to the relation itself (for join constructs, [§5.5](#55-join-property-mappings)). |
| Third | Resolves `~primaryKey` names against the row type's columns (hard error with an "Available columns: [...]" message on miss). If `~primaryKey` was omitted, leaves it empty for runtime inference. |

### 5.1 Bare-column → `$row.<col>` lowering

In the Second pass, a property mapping authored as `firstName: FIRSTNAME` gets
its `_valueFn` synthesised as if the user had written `firstName: $row.FIRSTNAME`.
Every downstream consumer (validator, SQL generator, composer, protocol
transfer) sees a single shape — a `LambdaFunction` body — regardless of which
surface syntax was used.

The trade-off: **bare-column authoring is round-trip-lossy**. The composer
will re-render it as the explicit `$row.<col>` form. Semantics are identical.

`$row` and `$src` are both injected the same way (a `VariableExpression`
registered via `ctx.addInferredVariables(...)` and flushed at the end of the
lambda): `$row` typed at the row `GenericType`, `$src` typed at the relation's
own `GenericType`. `$src` is a `Relation`, so `$src.COL` does not compile —
column access is always through `$row`.

### 5.2 The `asColumnRef` fast-path helper

Consumers that need to recover the original column name (SQL push-down fast
paths, IDE displays, debug output) use `RelationFunctionPropertyMappingTools.asColumnRef`,
which pattern-matches a `_valueFn` body of exactly one `$row.<col>` accessor
and returns the column name. Deliberately conservative — a complex expression
that happens to evaluate to a single column at runtime is not matched.

### 5.3 Embedded property mappings and the `_class` rewrite

When compiling a normal embedded mapping, the embedded builder rewrites each
child sub-property's `property._class` pointer from the outer class to the
embedded target class. Without this, `address ( city: CITY )` would look up
`city` on `Person` instead of `Address` and fail.

Inline embedded is different: the sub-object has its own separately-declared
class mapping. The embedded builder just records the target set ID; the actual
property mappings are resolved at routing / navigation time via
`inlineEmbeddedRelationFunctionMapping`.

### 5.4 IDs for embedded sets

Normal embedded: both `selfId` and `targetId` are `<parentId>_<propertyName>`.

Inline embedded: `selfId` still `<parentId>_<propertyName>`, but `targetId`
points to the separately-declared class mapping.

As with a classic `###Relational` inline, resolving an inline set keeps its own
`selfId` and copies the target's property mappings with `owner` and
`sourceSetImplementationId` set to the owner, so each owner — each branch of a
union owner included — resolves to its own set. The router resolves the inline
set for a single owner and for each branch of the `embedded_operation` it builds
for a union owner, and SQL generation resolves it again when it navigates from
the unresolved set (`findPropertyMapping`). Since `targetId` names the inline
target rather than the set itself, the router finds the owner's property mapping
for the set by the set's own id, and a `modelJoin` copied from the target finds
its source set by the owner id.

### 5.5 Join property mappings

- **`modelJoin`** parses through the generic `valueFn`/`combinedExpression`
  path as a `RelationFunctionPropertyMapping` containing the original `AppliedFunction`.
  `RelationModelJoinBuilder` recognises a property whose entire value is
  `modelJoin(<condition lambda>)` and the compiler lowers it to a
  `ModelJoinPropertyMapping`.
  It is a one-sided model join: the condition is **model-level**, over the
  owning class and the property's class (positional parameters, optionally
  typed as in an association `ModelJoin`), not over relation columns. The target class mapping is
  resolved the same way classic Association `ModelJoin` resolves its targets:
  `ClassMappingThirdPassBuilder.compileRelationModelJoin` finds the single
  root class mapping — in this mapping or its includes, of any store — whose
  class is exactly the property's declared type; there is no explicit target
  reference and no fallback for a subtype-only target. The condition is
  compiled (`PropertyMappingBuilder.compileRelationModelJoinCondition`) with
  its parameters typed to each set's `MappingClass` (so local `+` properties
  are visible) or class, exactly as association `ModelJoin` types them. The
  parameters are renamed to the same `_mj_src`/`_mj_tgt`
  sentinels classic Association `ModelJoin` compiles to
  (`ModelJoinLambdaRewriteVisitor`, shared with
  `HelperMappingBuilder.processModelJoinAssociationMapping`) — this is a
  lossy-but-stable rename, the same precedent association `ModelJoin` already
  sets by showing the user a different name (the two property names) than
  what they typed; inline `modelJoin` has no second property name available,
  so it shows the literal sentinels back on protocol round-trip instead. This
  makes the two constructs' compiled condition indistinguishable by shape,
  so the router and SQL-gen share one code path for both (see
  [§9.5](#95-join-sql-generation)) with no per-construct accommodation
  needed.
- **`navigationJoin`/`filterJoin`** parse as ordinary `valueFn` lambdas ending
  in `.col` — no grammar change. Both are declared as Pure-bodied `function
  <<functionType.NotImplementedFunction>>` (`fail(...)` + return the
  relation), the same idiom as `meta::pure::graphFetch::execution::graphFetch`
  — typed and compiled normally, never executed in-memory; a store's plan
  generator lowers the call before pre-evaluation would otherwise run it.
  Unlike `modelJoin`, there is **no dedicated grammar-side rewrite** — the two
  calls remain ordinary embedded `AppliedFunction`s, so their condition
  lambda's parameter types cannot be inferred by Pure's generic
  function-argument-inference machinery (an anonymous `RelationType` generic
  isn't something the framework can bind a lambda parameter to on its own).
  Instead, `Handlers.NavigationJoinFilterJoinInference`
  (`handlers/Handlers.java`) — a `ParametersInference` registered for both
  operators alongside `join`'s own `JoinInference`, sharing its lambda typing
  (`typeRelationJoinCondition`) — compiles `rel`/`target` first and types a
  two-parameter condition lambda's parameters from their row types, under the
  user's own names. Inside a relation-mapping property value, the
  terminal `.col` is typed against the operator's result relation via the
  Relation-unwrap path (`HelperValueSpecificationBuilder.processProperty`).
- **No column-rename step is needed for either.** Each construct returns (or
  is keyed to) one side's columns, so source/target column names never need
  to be disambiguated.

---

## 6. Validate

`MappingValidator.validateRelationFunctionClassMapping` runs *after* the
compiler has typed every `_valueFn` body. Two arms:

**Protocol-side.** Each inline `RelationFunctionEmbeddedPropertyMapping` (i.e.
one with `id` set and empty `propertyMappings`) must name a
`RelationFunctionClassMapping` that exists in the same protocol Mapping.

**Pure-graph side.** For each `RelationFunctionInstanceSetImplementation`:

- the relation function takes no parameters;
- its return type is `Relation<...>`;
- for every `RelationFunctionPropertyMapping._valueFn` (recursively into
  embedded sets):
  - the body's inferred multiplicity is subsumed by the property multiplicity;
  - the body's inferred raw type is a subtype of the property raw type — this
    check is skipped when a `BindingTransformer` or `EnumerationMapping`
    transformer is present, since the transformer is responsible for the
    conversion.

Errors mirror legend-pure's own validator wording so behaviour is consistent
between interpreted and compiled modes.

**Join-specific rules** ([§2.11](#211-relationship-join-property-mappings)) are legend-pure's
and are not checked here:

- **No PK-keyed uniqueness gate.** Covered in [§2.11](#211-relationship-join-property-mappings).
  A to-one `modelJoin`/`navigationJoin`/`filterJoin` property's join condition
  is not required to equate the target's primary key — doing so wouldn't
  actually prove the join is to-one, so the compiler doesn't try, matching
  classic `###Relational`'s `@Join | Col`.

---

## 7. Route & Store Contract

Routing is where each set implementation acquires a concrete store binding.

**Store contract per set type.**

| Set type | How the store is resolved |
|----------|--------------------------|
| `RelationFunctionInstanceSetImplementation` | Read from the routed function's `StoreClusteredValueSpecification` (asserts the function has already been routed). |
| `InstanceSetImplementation` | Standard `resolveStoreFromSetImplementation`. |
| `OperationSetImplementation` (union) | Recurse into each leaf; deduplicated store must be unique — this is the enforcement point for the "single store per union" rule. |
| `EmbeddedSetImplementation` | Delegate to the owning set. |

**Routing the function itself.** The single-set routing helper rewrites the
function's `expressionSequence` so its first expression becomes a
`ClusteredValueSpecification` — that's how downstream stages tell "routed"
apart from "unrouted". Class-level routing does the same across every set for
a class and caches the routed sets in `classMappingsByClass` so subsequent
property navigations don't re-route.

**Join property mappings.** `modelJoin` rides the existing
`ModelJoinPropertyMapping` router arm — no new arm needed.
`navigationJoin`/`filterJoin` are ordinary `valueFn`s: the property routes
like any other RFPM `valueFn`, and the join's target relation is reached via
the same `potentiallyRouteRelationFunctionSet` path used for `~func`/`~src`
sources.

**Accessor-only shortcut.** Most relation functions are native relation
operators over store accessors (`#>{db.T}#->filter(...)->limit(...)`). For
those the router contributes nothing but the accessor's store and the cluster
wrapper, so routing builds that cluster directly instead of running the full
router. A body qualifies only when the router would leave it untouched: no
Class, property, `getAll` or mapping-dependent reference, only native or
`shouldStop` functions (so nothing the router would inline or pre-evaluate),
no unmaterialized col-spec constructors, and exactly one store across the body,
lambdas and col specs included. Col-spec constructors take the full routing
path so SQL generation receives materialized specs. Anything else that needs
routing also takes the full routing path.

**Reusing routed sets in SQL generation.** Every `StoreMappingRoutedValueSpecification`
carries the sets the router already routed for that expression. pureToSql
keeps the routed relation function sets of the current expression, its
source expression and any attached union members in `setsFromRouter`, and
`processRelationFunctionClassMapping` takes the routed function body from there
by set id instead of routing the raw mapping body again. Set lookups by id
(`getClassMappingById`) still return the mapping's own instances, so
`RelationFunction` owners compare the same way across scopes. Inline-embedded
sets are excluded: they carry the target's id but the owner's function.

**Inherited property mappings** ([§2.12](#212-extending-a-class-mapping-extends)).
Looking a property up on the routed leaves of a union returns the same inherited
property mapping for each leaf, and the router keeps one copy, so the property
reads from a single set as in classic mappings. Its `modelJoin` condition names
the declaring set as its source, which is not among the routed sets: the
router (`modelJoinSourceSet` in `router/store/builder.pure`) routes the
condition against the extended set that the routed sets inherit it from. For a
`modelJoin` inside an inherited `Inline` target, each leaf resolves its own copy
of the `Inline` set, all under the declaring owner's id; the router routes the
condition against one of them.

---

## 8. Primary-Key Inference

When `~primaryKey` is omitted, the runtime derives PK columns from the
function's body. The algorithm lives in
`legend-engine-pure-code-compiled-core/.../core/pure/mapping/relationFunctionMapping.pure`.

### 8.1 Extension SPI

Store-specific knowledge is registered via `RelationElementAccessorExtension`
(a `ModuleExtension`). The relational store, for instance, registers a
resolver that reads `Table.primaryKey` / `View.primaryKey` off a
`RelationStoreAccessor` instance value.

This SPI keeps the PK inferencer store-agnostic: any store that produces its
own `InstanceValue` leaf types (rather than reusing `RelationStoreAccessor`)
just registers a resolver.

### 8.2 Recursive body walk

`inferPrimaryKeyColumnNames` dispatches on the shape of the value spec:

- `InstanceValue` → asks every registered `RelationElementAccessorExtension`.
- `ClusteredValueSpecification` → recurse into `.val`.
- `SimpleFunctionExpression` → per-operator handling (below).
- Anything else → no PK inferable.

### 8.3 Platform relation operators

User-defined helpers are inlined. Platform relation operators have hard-coded
PK propagation rules:

| Operator | PK result |
|----------|-----------|
| `filter`, `limit`, `drop`, `slice`, `sort`, `extend(*)`, `select` (no arg), `distinct` (no arg) | leftPK |
| `select(colSpec)` / `select(colSpecArray)` | leftPK ∩ projected columns |
| `rename(oldSpec, newSpec)` | leftPK with the old name substituted |
| `distinct(colSpecArray)` | the distinct-by columns |
| `groupBy(cols, aggs...)` | the group columns |
| `aggregate(aggs...)` | `[]` |
| `join(l, r, INNER \| LEFT, cond)` | leftPK ∪ rightPK |
| `join(l, r, RIGHT \| FULL, cond)` | `[]` |
| `asOfJoin(l, r, ...)` | leftPK ∪ rightPK |
| anything else | `[]` |

The table above is the authoritative behavioural contract — if you add a new
platform relation operator, decide which row it belongs to and add it to the
Pure implementation.

### 8.4 When inference runs

- **At compile time** — no. Third-pass PK resolution only validates
  *explicit* `~primaryKey` names.
- **Lazily, when needed** — yes. The router infers it on the unrouted body
  when it routes a set, and SQL-generation entry points
  (`ensureRelationFunctionPrimaryKeyResolved`, `resolvePrimaryKey`) infer it
  right before they need PK data. That's what makes PK inference "just work"
  without a compile-time pass.

Prefer implicit inference for straightforward function bodies; add an explicit
`~primaryKey` when the body is opaque to the operator table (custom store
accessors, unusual operator chains).

---

## 9. SQL Generation

The two entry points to know:

- `processRelationFunctionClassMapping` — how `getAll` on a Relation-backed
  class becomes SQL.
- `transformRelationFunctionPropertyMappingToRelational` — how a
  `RelationFunctionPropertyMapping` (RFPM) becomes a downstream property
  mapping during property navigation.

### 9.1 `getAll`: evaluate + sub-select

Given a routed `RelationFunctionInstanceSetImplementation`, `getAll` produces:

1. **Ensure PK is resolved** (§8) — auto-infer if empty.
2. **Route the function** if not already routed.
3. **Evaluate the routed expression sequence** against a fresh empty
   `SelectWithCursor` and default state — this returns a cursor carrying the
   materialised `SelectSQLQuery` for the relation body.
4. **Wrap in a sub-select** via `moveSelectQueryToSubSelect` so that filters,
   projections and sorts downstream operate on the *output* of the relation
   body, not on its internals.

The sub-select wrapping is what makes Relation mappings composable with
arbitrary Pure query pipelines — from the outside, the relation looks like a
single named source.

### 9.2 Dispatch in `processGetAll`

`processGetAll` matches on the set implementation and delegates:

- `RootRelationalInstanceSetImplementation` → classic relational path.
- `RelationFunctionInstanceSetImplementation` → the flow above.
- `OperationSetImplementation` (union) → single-leaf shortcut, or `buildUnion`
  for ≥2 leaves (see [§10](#10-union-sql-generation)).

### 9.3 Property navigation and the RFPM lift path

A property mapping stays as a `RelationFunctionPropertyMapping` in the Pure
graph. When navigation reaches one, it is transformed on-the-fly into a
concrete downstream property mapping shape. The transformation runs in three
conceptual steps:

**1. Build a synthetic RF cursor.** The transformer materialises a
`SelectWithCursor` whose relation is a `RelationFunction` populated with
**placeholder** `RelationFunctionColumn` instances — one per column in the
row type. The `owner` field of each placeholder column is intentionally left
empty; that's the marker that identifies it later.

**2. Evaluate the `valueFn` against the synthetic cursor.** The lambda's
`$row` reference is bound to the synthetic cursor's alias via
`updateFunctionParamScope`. Processing the body produces a
relational operation tree whose leaves are placeholder `TableAliasColumn`s.
`expressionTouchesVariant` decides whether the body reaches into semi-structured
territory. Only a sub-expression typed `Variant`, or read from a semi-structured
column, counts; a literal or `@Type` argument (`substring(0, 2)`, `cast(@StrictDate)`,
an `if` over string literals) does not, so such a body on a to-many primitive property
stays a plain `RelationalPropertyMapping` instead of being fanned out by an array flatten.

**3. Choose a downstream shape.** Based on the transformer and the variant
signal:

| Situation | Downstream shape |
|-----------|------------------|
| `BindingTransformer` present | `SemiStructuredEmbeddedRelationalInstanceSetImplementation` |
| Non-variant `valueFn` | `RelationalPropertyMapping` |
| Variant `valueFn`, `Class` target | `SemiStructuredEmbeddedRelationalInstanceSetImplementation` |
| Variant `valueFn`, primitive / Enum / `Variant` target | `SemiStructuredRelationalPropertyMapping` |
| Structural container target (`Map`, `List`, `Pair`) | Rejected — hard error |

The semi-structured embedded variant is backed by a synthetic
`RootRelationalInstanceSetImplementation` in its `setMappingOwner` slot
because downstream code (owner lookups, PK resolution) expects that shape.

### 9.4 Placeholder resolution at column-nav time

The placeholder TACs from step 2 don't reference the outer `SelectSQLQuery`
yet — they're anchored on the synthetic `RelationFunction` alias. When column
navigation eventually resolves them (`resolveTableAliasColumn`), it detects
the empty `owner` and either reuses a matching projected column in the outer
select or appends the column to it. This deferred resolution is what allows
the transformer to be locally simple — it doesn't need to know what outer
select it will land in.

A composite element — a `DynaFunction`, or the `JoinStrings` that string `+` and `joinStrings`
produce — resolves each operand against the outer select separately and merges the results with
`mergeSQLQueryData` (`resolveDynaFunction`, `resolveJoinStrings`). An operand that is a chain's
`.col` applies the chain's join while it resolves, so the merge is what carries that join into the
query, and it is also what shares one join between operands reading the same chain.

**Cursor position vs. tree root.** `processColumnFunctionExpression` (the
general `.col` terminal resolver, not specific to relation mappings) resolves
a column's table alias from the *receiver's own cursor position*
(`leftCursor.alias`, i.e. `leftCursor.currentTreeNode.alias`), not from the
tree's structural root (`leftSelect.data.alias`). These coincide in the
common case, but diverge for a `modelJoin`/`navigationJoin`/`filterJoin`
condition's target-side variable: resolving that variable through
`functionReferenceScope` yields a cursor whose `currentTreeNode` is the
join-placeholder node, while the tree's structural root is still the source
alias (attaching the placeholder join adds a *child*, it doesn't replace the
root). Using the root's alias there silently produces a `TableAliasColumn`
against the wrong (source) table. The same divergence affects two other
things `processColumnFunctionExpression` does: it must rebuild its result's
`currentTreeNode`/`positionBeforeLastApplyJoinTreeNode` from the receiver's
cursor (not the generically-threaded `$operation` parameter, which is stale
in the diverging case), and it must skip two column-list "shortcut" lookups
(`leftSelect`'s own columns; `$operation.parent`'s columns) that return a
matched `Alias`/`TableAliasColumn` verbatim — safe only when receiver and
operation coincide, since in the diverging case the match may carry a stale
alias reference one scope level removed from where the condition needs it.

### 9.5 Join SQL generation

`modelJoin` (both classic Association and inline), `navigationJoin`, and
`filterJoin` all reuse the same ModelJoin per-branch machinery
([`model-join.md`](model-join.md)): `compileModelJoinForBranch` in
`relationalModelJoins.pure` takes an explicit `targetId`/`targetSWC` (already
built — see below) plus an optional `mappedTargetSet:InstanceSetImplementation[0..1]`
(used only to detect filter/groupBy/distinct needing materialization), a
source cursor, and the join condition. It's called from
`processPropertyMappingReturnPropertyMapping`'s `ModelJoinPropertyMapping` arm
in `pureToSQLQuery.pure` for classic/inline `modelJoin`, and from
`processRelationMappingJoin`/`buildKeyedJoinChain` for `navigationJoin`/`filterJoin`.

The two kinds of caller differ only in how `targetSWC` gets built.
Classic/inline `modelJoin`'s target is a real, already-mapped class mapping,
so its `targetSWC` comes from `buildMappedModelJoinTargetSWC` dispatching on
the target's actual `InstanceSetImplementation` type (`RootRelationalInstanceSetImplementation`
or `RelationFunctionInstanceSetImplementation`), and the real target set is
passed through as `mappedTargetSet`. `navigationJoin`/`filterJoin` are
unmodeled — their target is just a `Relation`-valued expression (any
expression, not only a named function call — see [§2.11](#211-relationship-join-property-mappings)'s
Operands note), not a class mapping — so `relationMappingJoinTarget` in
`pureToSQLQuery.pure` wraps it as a synthetic `RelationFunctionInstanceSetImplementation`
around a zero-arg `LambdaFunction(expressionSequence = targetExpr)`
(`LambdaFunction` IS-A `FunctionDefinition`, so this works for any target
expression, not only a bare named function call) and feeds *that* synthetic
set through the exact same `buildMappedModelJoinTargetSWC` call classic/inline
`modelJoin` uses — not a bespoke, lighter-weight path. (An earlier attempt at
a genuinely bespoke path — processing the target via plain
`processValueSpecification`, the way TDS v2's own `Relation::join()` processes
its right-hand side — compiled fine but failed at runtime: a `validateNode`
assertion on a nested zero-arg call to a function whose body is a bare store
accessor, the common `navigationJoin(my::firmRel(), ...)` shape. Reusing
`buildMappedModelJoinTargetSWC`'s `RelationFunctionInstanceSetImplementation`
branch, which already goes through `processRelationFunctionClassMapping`,
avoids that.) A **single `navigationJoin`** exposes the target's columns,
each as an `Alias` over a `RelationalOperationElementWithJoin` carrying the
one-hop join tree; the terminal `.col` slots into one of them.

A **`filterJoin` or any chain** is lowered to one join from the source to a
single sub-select keyed on the source's primary key
(`processRelationMappingJoinAsKeyedSubselect` in `pureToSQLQuery.pure`):

```
source  LEFT OUTER JOIN  ( SELECT <copy>.PK AS relJoinKey_PK, <kept columns>
                           FROM <copy of source> JOIN hop1 JOIN hop2 ... )
        ON source.PK IS NOT DISTINCT FROM relJoinKey_PK
```

The source copy and each `navigationJoin`/`filterJoin` target are the `~func`'s own sub-select
(`withoutIdentityProjection`): the outer projection `processRelationFunctionClassMapping` adds over
it is dropped when it only re-exposes the same columns, and kept when it renames them.

Each hop is compiled against the alias the previous hop kept — the source
copy for the first hop and after a `filterJoin`, the previous target after a
`navigationJoin` — and attached under it, so the kept side of a `filterJoin`
is the source *copy* inside the sub-select. Two properties follow: a source
row with no match has no row in the sub-select, so the property is empty
rather than the source value; and every hop stays inside the sub-select, so
the outer query only ever sees a single-level join (the projection isolation
logic in `buildCorrelatedSubQuery` does not handle multi-level join trees of
sub-select-backed nodes).

**Transformed `$src`.** A join whose first source is `$src` with `extend`/`filter`/`select`/`rename`/`sort` steps
([§2.11](#211-relationship-join-property-mappings)) always takes the keyed sub-select path, even a single `navigationJoin`:
the join is not anchored on the source's own table alias, whose rows are the whole class mapping. The steps become
part of the relation the sub-select is keyed on. `applyRelationMappingJoinSourceSteps` substitutes the class mapping's
own relation function body for `$src` in the step chain, wraps the result in a synthetic
`RelationFunctionInstanceSetImplementation` (as `relationMappingJoinTarget` does for a target) and lowers it through
`processRelationFunctionClassMapping`, so the steps are routed, pre-evaluated and milestoned like the body of a
`~func` that ended in them. The first hop then joins from that relation, reading its columns by the names the steps
expose them under (`keptColumnsOnAlias`), and the sub-select stays `LEFT OUTER` joined to the real source by primary key:

```
source  LEFT OUTER JOIN  ( SELECT <copy>.PK AS relJoinKey_PK, <kept columns>
                           FROM ( <relation function body> + extend / filter / select / rename ) AS copy
                                LEFT OUTER JOIN <hop1> ON <condition over the transformed columns> )
        ON source.PK IS NOT DISTINCT FROM relJoinKey_PK
```

A `filter` therefore removes rows from the copy only: the source row it removes has no row in the sub-select, so the
joined columns are empty for it and the instance is kept. The steps' lambdas see only the relation's columns, so a
window `extend` runs inside the copy before a later `filter`, in the order written. The copy must still expose every
primary key column of the class mapping, which `applyRelationMappingJoinSourceSteps` asserts before lowering, naming the
missing column and the columns left. A `sort` is not lowered (`dropSortSteps`): it keeps each row once and the join
matches by key, so it cannot change the result, and `rename` followed by `sort` otherwise orders by a name that only exists
a level out. Because a sort is dropped rather than lowered, a sorted and an unsorted source share one join and a source of
`sort` alone is the plain source.

The join names carry the steps. The first hop's source id is `<class mapping id>_<fingerprint of the step chain>`
(`relationExpressionFingerprint`, the one a join target gets), and so is the keyed sub-select's alias, so two properties
with the same steps and join merge into one join in `mergeSQLQueryData`, and the same join over different steps, or over
no steps, stays two.

**Join kind per hop.** Each hop's own `JoinType` (`relationMappingJoinType`) defaults per its
construct — `LEFT_OUTER` for a `navigationJoin` hop, `INNER` for a `filterJoin` hop (its gating
semantic) — and an explicit `JoinKind` on that hop's own call overrides either default. The
`source  LEFT OUTER JOIN  ( ... )` connector shown above is always `LEFT_OUTER` regardless of any
hop's kind, so a source row with no row in the sub-select keeps its row with the property empty. (An
`INNER` connector would be merged into the query's `FROM` as an inner join, and projection isolation
would then wrap it in a `LEFT OUTER JOIN (SELECT <second copy of source> INNER JOIN ...) ON PK = PK`
self-join to keep those rows.) A single-hop `navigationJoin` (not going through the keyed sub-select)
likewise defaults `LEFT_OUTER`; an explicit `INNER` on it is that self-join case. A graphFetch child
query whose target is a Relation class mapping (`generateNonPrimitivePropertyQuery`) therefore leaves
isolation until its key and property queries have selected their columns: those are found by name on
the current node, and the self-join's sub-select exposes only the source's columns. Every hop's
condition is compiled against a `LEFT_OUTER` placeholder join (`compileModelJoinForBranch`'s
`placeholderJoinType`) and the hop's own kind is stamped on the compiled tree afterwards
(`forceJoinTypeTree`): compiling a condition of several terms under an
`INNER` placeholder makes `processDynaFunction`'s per-argument isolation pull the target into a
correlated sub-query, which `buildCorrelatedSubQuery` cannot build for a sub-select-backed target.
An `INNER` hop inside the keyed sub-select removes the sub-select's row for that source row, so the
`LEFT OUTER` connector keeps the source row with the property empty — the result of
`###Relational`'s nested `@J1 > (INNER) @J2`.

**Milestoning.** The property's value expression is evaluated against a synthetic cursor
(`evaluateRfpmValueFn`) that carries the query's `milestoningContext`, so every target built from it
(`buildMappedModelJoinTargetSWC` → `processRelationFunctionClassMapping`) and the keyed source copy
are processed under it, and `processRelationStoreAccessor` filters each milestoned accessor they
contain. When both sides of a `->join(...)` carry the same milestoning date column, the right side's
is dropped (`withoutMilestoningDateColumnsOf`), including a right-side user column that happens to carry that name.

**Join naming for `mergeSQLQueryData` dedup.** `mergeSQLQueryData` merges join-tree children by
`Join.name` alone, so the name every construct here generates (`modelJoinName`) must be equal for
two occurrences of the same join — the way a classic `###Relational` `@join` is shared by every
property mapping that references it — and different for any two different joins, or the second
join's condition is silently dropped. The name is a role-based fingerprint:

```
<sourceId>_<targetId>_<joinKind>_<joinType>_<condition by role>_<milestoning context>_ModelJoin
```

- **Condition by role.** The compiled condition is printed with each alias replaced by its role,
  never by its call-site-derived name or the relation behind it: `{S}.` for the source side,
  `{T}.` for the target side, `{S^}.`/`{T^}.` for a side's sub-select root alias (a condition
  that resolves a column through a sub-select-backed side reads it), and the role followed
  by the join names leading to it (`{S/<joinName>}.`) for an alias reached by navigating further
  from a side. Literals and operators are printed as they are. An alias name both sides use keeps
  the old expansion, since its role would be ambiguous. Printing the relations behind the aliases
  instead made the name exponential in the nesting depth of the source and target relations
  (each column's alias expanded its own sub-select again).
- **Source and target.** The roles only mean something because the ids identify the relations: a
  class mapping's `.id` for classic and inline `modelJoin`, and for a `navigationJoin`/`filterJoin`
  target, `relationMappingJoinTarget`'s content key — a structural fingerprint of the target
  expression (functions by path, lambdas with their bodies, literals, column specs, store
  accessors by database, schema and table), so `firmRel()->filter(f | $f.ID > 1)` and
  `firmRel()->filter(f | $f.ID > 2)` are different targets. A chained hop's source is the previous
  hop's kept target.
- **Milestoning context.** Both sides are milestoned by the query's context, so the same property
  read at two dates is two joins.

The keyed sub-select of a chained or `filterJoin` property is named from its hops' join names (its
root is always the class mapping's own relation). Names longer than 256 characters are cut and
suffixed with an MD5 of the rest, and carry no double quote because they can end up inside a quoted
SQL alias.

A hop's condition reads the kept columns off the alias the hop joins from,
under the names that alias exposes them by (`keptColumnsOnAlias`). Resolved
through the alias's sub-select instead, a computed column would be inlined as
its definition, which reads columns the sub-select need not expose: a key
computed by an `extend` and kept by a `select` would be recomputed from columns
the `select` dropped. The single-hop path gets the same from its source wrapper
alias, whose relational element is the source's outer projection and which
carries the source sub-select's own root alias name, so the join is re-anchored
onto the real source by name when it is applied.

A hop's target may itself compute the column its condition reads
(`firmRel()->extend(~KEY: f | $f.ID + 1)` joined on `$p.FIRMID == $f.KEY`), whatever the expression's type
(arithmetic, `if` with a branch that is empty, string concatenation) and through any mix of `extend`, `select`
and `filter` on the target. The target is its own sub-select, so the column is read as that sub-select exposes it
under its name (`exposedColumnOnAlias`) rather than as its defining expression, which would read the target's
columns from outside it. The column's SQL type is the one the Pure compiler inferred for `$f.KEY`. A computed
column read in a later hop's condition, a computed column of that later hop's target and a `filterJoin` target all
go the same way. A `modelJoin` onto a mapped property that is computed by an expression needs none of this: the
expression is inlined into the join condition. The condition itself cannot do arithmetic on `$p` or `$f`
(`{p, f | $p.FIRMID + 1 == $f.ID}` fails to compile with `Can't find a match for function 'plus(Any[2])'`, because the
lambda parameters are untyped); put the computation in an `extend` on the source (`$src->extend(...)`) or on the target.

A join chain may head the collection a Variant `filter`/`fold`/`map` lambda runs over, or sit inside the lambda body:
`resolveRelationalLambda` merges the select of every lambda parameter and of the body, so each join survives into the
query. (legend-pure's compiler rejects a `map` over a joined Variant column with a type-inference error; `filter`
and `fold` compile.)

The columns the keyed sub-select and the outer query read off a kept alias are referenced as that alias's
sub-select exposes them (`relationColumnOnAlias`): a computed column is exposed quoted (`as "firmKey"`) and a
table column unquoted, and a reference that differs in quoting fails on a database that folds unquoted
identifiers (H2). An all-uppercase name is referenced unquoted whichever way it is exposed. DuckDB treats the
two spellings as one identifier, so a Relation-returning EMIT query does not catch a mismatch; the exact SQL in
`inlineJoin.pure` (`Test_Pure_RelationMappingJoins`) is what pins the quoting.

**All three constructs' conditions compile to the same `_mj_src`/`_mj_tgt` sentinels** — just at
different stages. Classic Association `ModelJoin` and inline `modelJoin` rewrite the user's two
parameter names to the literal sentinels at **compile time**
(`ModelJoinLambdaRewriteVisitor`, shared by `HelperMappingBuilder.processModelJoinAssociationMapping`
and `PropertyMappingBuilder.compileRelationModelJoinCondition`) — the actual Pure-graph `LambdaFunction`
parameters are named `_mj_src`/`_mj_tgt`, so the router's existing (position-independent) check for
those two literal names already works for both without any extra accommodation.
`navigationJoin`/`filterJoin` instead keep the user's own parameter names through compilation and
rename them at **SQL-generation time**, positionally — `withJoinSideSentinels` in
`pureToSQLQuery.pure` maps the condition lambda's first/second declared parameter (whatever they're
named) to `_mj_src`/`_mj_tgt` on a throwaway copy right before `compileModelJoinForBranch` uses them
as `conditionVariableNames` keys in its `varToSelect` scope map. Because the condition's own
parameters are scoped to the condition either way, `compileModelJoinForBranch` compiles the ON clause
with only the two sides in scope (no enclosing function scope, relation-lambda bindings, qualifier
`$this`, or query variables of the same names) — `ModelJoinLambdaRewriteVisitor` additionally handles
an inner lambda that re-binds one of the two names by not rewriting inside it (shadowing).

Node ids and aliases carry the join kind they come from — `_mj` (modelJoin),
`_nj` (navigationJoin), `_fj` (filterJoin) — plus the node id, so a keyed
sub-select used at two navigation depths, or a self-join
(e.g. `person -> manager`, both sides the same relation function) aliases the
two occurrences of the underlying table distinctly. The ON condition is
compiled with `shouldIsolate = false`: an enclosing filter context (e.g.
`isNotEmpty`) must not trigger isolation of the condition's own trees.

---

## 10. Union SQL Generation

`buildUnion` accepts leaves of any `InstanceSetImplementation` mix and
per-leaf-dispatches to the relational or relation-function pipeline. The
interesting bits are the reconciliation points:

- **Milestoning columns.** Relation-function leaves contribute an empty
  column list (no physical table to inspect for temporal columns). This means
  temporal filtering doesn't apply to that branch — take this into account
  when authoring milestoned unions with RF leaves.
- **Column enumeration** for non-merge-compatible joins is derived from the
  leaf's already-materialised `SelectSQLQuery.columns` (there's no schema to
  read).
- **FK discovery** walks each leaf's property mappings; for RF leaves,
  `TableAliasColumn`s are filtered by matching `RelationFunction.owner` to
  the leaf set. Embedded-RF descends via `owner`.
- **Same-relation equality.** Two `RelationFunction`s are considered the same
  when their `owner` sets are the same.
- **Unique alias naming.** RF leaves render as `rf(<setId>)`.
- **Single-store constraint.** Enforced by the store-contract layer
  ([§6](#6-validate) / [§7](#7-route--store-contract)) — cross-store unions
  fail to route, not at union assembly.

---

## 11. Composer & Protocol Transfer

**Composer** (`DEPRECATED_PureGrammarComposerCore`) round-trips both source
forms (`~func` / `~src`) and both property-RHS forms (bare column / lambda
body). Bare-column authoring re-emits as the explicit `$row.<col>` form
because the compiler has already lowered it — this is intentional to keep
the round-trip semantics-preserving and avoid brittle pattern-matching to
recover the sugar.

**Protocol transfer** (`vX_X_X/transfers/mapping.pure`) splits the compiled
Pure `_relationFunction` back into the mutually-exclusive protocol fields
based on runtime type:

- `ConcreteFunctionDefinition` → `relationFunction: PackageableElementPointer`.
- `LambdaFunction` (from `~src`) → `sourceLambda: LambdaFunction`.

Property mappings always emit `valueFn` in the transferred protocol (never
`column`), matching the composer's non-lossy strategy.

---

## 12. Authoritative File Map

Use this to jump from a concept in the doc to its implementation. Paths are
relative to their module roots; every file lives under the modules described
in [`CLAUDE.md`](../../CLAUDE.md).

| Concern | Key files |
|---------|-----------|
| Lexer / parser grammars | `RelationFunctionMappingLexerGrammar.g4`, `RelationFunctionMappingParserGrammar.g4` |
| Parse-tree walker | `RelationFunctionMappingParseTreeWalker.java` |
| Grammar composer | `DEPRECATED_PureGrammarComposerCore.java` |
| Protocol POJOs | `RelationFunctionClassMapping.java`, `RelationFunctionPropertyMapping.java`, `RelationFunctionEmbeddedPropertyMapping.java` |
| Compiler passes | `ClassMappingPrerequisiteElementsPassBuilder.java`, `ClassMappingFirstPassBuilder.java`, `ClassMappingSecondPassBuilder.java`, `ClassMappingThirdPassBuilder.java` |
| Property-mapping builder | `PropertyMappingBuilder.java` |
| Bare-column matcher | `RelationFunctionPropertyMappingTools.java` |
| Validator | `MappingValidator.java` |
| Primary-key inference (Pure) | `core/pure/mapping/relationFunctionMapping.pure` |
| Runtime helpers / PK synthesis | `core_relational/relational/helperFunctions/helperFunctions.pure` |
| SQL metamodel additions (`RelationFunction`, `RelationFunctionColumn`) | `core_relational/relational/pureToSQLQuery/metamodel.pure` |
| Main SQL generation | `core_relational/relational/pureToSQLQuery/pureToSQLQuery.pure` |
| Variant / semi-structured SQL generation | `core_relational/relational/pureToSQLQuery/pureToSQLQuery_variant.pure` |
| Union SQL generation | `core_relational/relational/pureToSQLQuery/pureToSQLQuery_union.pure` |
| Routing / store contract | `core/pure/router/store/cluster.pure`, `core/pure/router/store/routing.pure` |
| Inline-embedded resolution | `core/pure/mapping/mappingExtension.pure` |
| Protocol transfer | `core/pure/protocol/vX_X_X/models/dsl/mapping.pure`, `core/pure/protocol/vX_X_X/transfers/mapping.pure`, and the `v1_33_0` equivalents (`models/metamodel.pure`, `transfers/mapping.pure`) — both carry inline `modelJoin` as `InlineModelJoinPropertyMapping` (in the `relation` package, not `modelJoin`) |
| `navigationJoin`/`filterJoin`/`modelJoin` functions, `JoinKind` | legend-pure `legend-pure-m3-core`: `platform/pure/grammar/functions/meta/type/relation/relationJoin.pure` |
| `navigationJoin`/`filterJoin` condition-lambda typing, `.col` unwrap | `handlers/Handlers.java` (`NavigationJoinFilterJoinInference`, `NavigationJoinReturnInference`, `FilterJoinReturnInference`), `HelperValueSpecificationBuilder.processProperty`, `ProcessingContext.isRelationMappingPropertyValue` |
| Join-condition sentinel renaming | compile time (classic/inline `modelJoin`): `ModelJoinLambdaRewriteVisitor.java`, `HelperMappingBuilder.MODEL_JOIN_SOURCE_VAR`/`TARGET_VAR`, `PropertyMappingBuilder.compileRelationModelJoinCondition`. SQL-gen time (`navigationJoin`/`filterJoin`): `pureToSqlQuery.pure` (`withJoinSideSentinels`). Router's literal-name check: `router/routing/router_routing.pure` (`routeValueSpecification`) |
| `modelJoin` resolution and lowering | `RelationModelJoinBuilder.java` |
| `modelJoin` protocol POJO (reused) | `.../mapping/modelJoin/ModelJoinPropertyMapping.java` |
| Join SQL generation | `relationalModelJoins.pure` (`compileModelJoinForBranch`), `pureToSQLQuery.pure` (`processRelationMappingJoin`; `processRelationMappingJoinAsKeyedSubselect` / `buildKeyedJoinChain` for `filterJoin` and chains) |
| Join validation | legend-pure at compile time for `.pure` sources: `RelationFunctionInstanceSetImplementationProcessor`. Engine: `ClassMappingThirdPassBuilder.inferRelationModelJoinTarget` (target must resolve) at compile time; at plan generation, reported only when a query reaches the property and without a source position, the unbound-`$src` assertion (`processVariableExpression`) and the source-PK assertions for chained/`filterJoin` lowering in `pureToSQLQuery.pure` (`processRelationMappingJoinAsKeyedSubselect`) and, for a transformed `$src`, that its steps keep every primary key column (`applyRelationMappingJoinSourceSteps`) |
| Join tests | `TestRelationFunctionMappingJoinCompilation.java` (compiler), `TestRelationMappingJoinPlanGeneration.java` (relational executionPlan: protocol compile then plan generation, the unbound-`$src` assertion), `TestNavigationJoinSqlGeneration.java`/`TestFilterJoinSqlGeneration.java`/`TestInlineModelJoinSqlGeneration.java` (exact-SQL-text, relational executionPlan), `inlineJoin.pure`, `inlineJoinSteppedSource.pure`, `inlineJoinComputedTarget.pure`, `inlineJoinFilteredTarget.pure` and `inlineReAlias.pure` (exact SQL and rows, `Test_Pure_RelationMappingJoins`), EMIT models `relation-inline-join`, `relation-inline-self-join`, `relation-inline-join-advanced`, `relation-inline-join-milestoning`, `relation-inline-join-graphfetch`, `relation-inline-join-filtered-target`, `relation-inline-embedded-model-join`, `relation-inline-nested-inline`, `relation-inline-join-computed-key`, `relation-inline-join-variant`, `relation-inline-join-stepped-source` (DuckDB only), `relation-expression-rhs-to-many`, `relation-graphfetch-enum` |
| `extends` tests | `inlineJoin.pure` (`ExtendsMapping`: exact SQL for an inherited `modelJoin` under `subType`, under a filter, on a union of leaves, inside an inherited `Inline` target on a union of leaves, and with an inherited `navigationJoin` two levels down, `Test_Pure_RelationMappingJoins`), EMIT model `relation-class-mapping-extends` (project, filter and graphFetch through every inherited property form, checked against a `###Relational` mapping with the same hierarchy) |

---

## 13. Decision Cheat-sheet

| Question | Answer |
|----------|--------|
| Difference between `~func` and `~src`? | `~func` references an existing Pure function; `~src` inlines a zero-arg expression. The compiler treats both uniformly after wrapping `~src` in a synthetic lambda. |
| What property RHS forms are supported? | Bare column identifier (lowered to `{$row.<col>}`), a full Pure expression over `$row`, a relation-level join construct (`navigationJoin`/`filterJoin`) over `$src` (optionally after `extend`/`filter`/`select`/`rename`/`sort`), or a model-level `modelJoin` ([§2.11](#211-relationship-join-property-mappings)). |
| What's the difference between `$row` and `$src`? | `$row` is the current row (own-column access). `$src` is the class's own **relation** — only meaningful as the left operand of a join construct. `$src.COL` does not compile. |
| How do I express a relationship to another mapped class? | `modelJoin` — `firm : modelJoin({p,f\|$p.firmId == $f.id})`, a condition over the two classes' properties like an association `ModelJoin`'s. Target class mapping is always resolved from the property's declared type — no explicit target reference. Lowers to the same `ModelJoinPropertyMapping` an XStore association would produce. |
| How do I pull a column through a join without full class navigation? | `navigationJoin` (joined/right column) or `filterJoin` (this/left column, gated by the join), terminated with `.col`. Target is a relation function, not a set-id. |
| Can join constructs chain? | Yes — `navigationJoin`/`filterJoin` return one side's relation, so calls compose; `.col` is terminal. `modelJoin` is not chainable (use ordinary property navigation into the target's own mapping instead). |
| Are joins ever OUTER? | `navigationJoin` defaults `LEFT` (and `modelJoin`, which has no surface join-kind syntax, is always effectively LEFT); `filterJoin` defaults `INNER` (its gating semantic). Either can override via an explicit `JoinKind` argument on `navigationJoin`/`filterJoin`. A row with no match keeps its row with an empty property value regardless of kind — kind governs whether a given hop's *gate* requires a match, not whether the source row survives. |
| When should I omit `~primaryKey`? | When the function body's leaves are recognised by a registered `RelationElementAccessorExtension` and the operator chain preserves PK ([§8.3](#83-platform-relation-operators)). Otherwise declare it explicitly. |
| Can I map multiple PK columns? | Yes: `~primaryKey: [COL1, COL2]`. |
| Property types supported? | Primitives, `Enumeration` (with `EnumerationMapping`), `Variant`, and complex `Class` types (with `Binding` for binding-style, or a variant-touching valueFn for lift-style). `[*]` multiplicities are honoured when the valueFn body's multiplicity is subsumed. |
| Property types rejected? | Structural containers `Map`, `List`, `Pair` — they are `Class` but unsupported by the RFPM lift. |
| Where does multiplicity / type validation happen? | In `MappingValidator`, after the compiler has typed every `_valueFn`. Skipped when a transformer is present (the transformer owns the conversion). |
| How does `$x.address.city` resolve on a normal-embedded set? | Direct child lookup in the embedded set's `propertyMappings`. |
| How does `$x.address.city` resolve on an inline-embedded set? | Via `_classMappingByIdRecursive` on the mapping (`inlineEmbeddedRelationFunctionMapping`). |
| Can inline embedded use a different relation function? | Yes — the inline target set is fully independent. |
| Can a Relation mapping participate in a union? | Yes. All leaves must resolve to the same store. |
| Can a Relation mapping `extends` another? | Yes, a Relation mapping of the same class or of its direct superclass. It keeps its own `~func`/`~src` and inherits the property mappings it does not map itself, joins included, with classic `extends` semantics ([§2.12](#212-extending-a-class-mapping-extends)). |
| Can I mix Relation and Relational leaves in a union? | Yes, as long as they share the same store. |
| What happens to milestoning columns in a union with an RF leaf? | The RF leaf contributes empty milestoning columns; temporal filtering does not apply to that branch. |
| Cross-store union? | Not supported — enforced during store-contract resolution. |
| How does semi-structured / variant lift work? | The RFPM transformer evaluates the property's `valueFn` against a *synthetic RF cursor*, detects variant-ness, and picks a downstream shape ([§9.3](#93-property-navigation-and-the-rfpm-lift-path)). |
| How do local properties differ from class properties? | `+name: Type[mult]` declares a property that exists only within the mapping scope; the canonical Pure class is unchanged. |
| Will bare-column authoring round-trip verbatim? | Semantics: yes. Syntax: no — the composer re-emits it as `$row.<col>`. |

---

## 14. Where to Look Next

- To trace a specific query end-to-end, start at
  `processRelationFunctionClassMapping` in `pureToSQLQuery.pure` and follow
  the call sites of `evaluateRfpmValueFn` for property navigation.
- To add a new store that participates in PK inference, register a
  `RelationElementAccessorExtension` — see the relational store's
  `syntheticRelationalAccessorExtension` as the reference implementation.
- To extend the PK-inference operator table, edit
  `inferPrimaryKeyColumnNamesFromFunctionExpression` in
  `relationFunctionMapping.pure`; the operator table in [§8.3](#83-platform-relation-operators)
  is the contract to keep it aligned with.
- To add a new property RHS form, extend the parser rule
  `relationFunctionPropertyMapping`, decide how the Second pass should lower
  it into a `_valueFn`, and update the composer for round-trip.
- To debug a failing Relation-mapping test, the usual suspects are: (a)
  incorrect `~primaryKey` inference — check that your operator chain is
  covered by [§8.3](#83-platform-relation-operators); (b) placeholder-TAC
  resolution — inspect the outer `SelectSQLQuery.columns` at the point of
  failure; (c) union store mismatch — check `storeContractForSetImplementation`
  output for each leaf.
- To debug a failing join property mapping, check: (a) `$row` vs `$src`
  confusion — column access must be `$row.<col>`, not `$src.<col>`
  ([§5.1](#51-bare-column--rowcol-lowering)); (b) for SQL-shape issues,
  inspect `compileModelJoinForBranch`'s `varToSelect` resolution and the
  one-sided projection restriction ([§9.5](#95-join-sql-generation)).

