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
| Column binding | `[db]TBL.COL` path | Column name (or lambda over `$src`) |
| SQL-gen input | Table alias tree | Evaluated sub-select |
| Type-checked source? | No — schema only | Yes — full Pure typing |

Two properties of the design are worth internalising up front:

1. **The source is compiled Pure.** Whatever produces the relation — an
   existing named function, or an inline `#>{db.TABLE}#->select(...)`
   expression — goes through the normal Pure compiler and carries a typed
   `RelationType` on its last expression. Everything downstream (row-type
   extraction, PK resolution, `$src`-lambda typing) reads off that
   `RelationType`.

2. **Property RHS is either a column name or a lambda over `$src`.** The bare
   `firstName: FIRSTNAME` form is *sugar* — the compiler lowers it to
   `{$src.FIRSTNAME}` so downstream code deals with a single shape (a
   `LambdaFunction` typed at the row type). Anything more expressive — string
   concatenation, arithmetic, semi-structured navigation — is just a longer
   lambda body.

Read those two ideas back into every stage below and the pipeline is mostly
obvious.

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

- `propName: COLUMN` — bare column (sugar, lowered to `{$src.COLUMN}`).
- `propName: $src.COLUMN + '-' + $src.OTHER` — arbitrary Pure expression over `$src`.
- `propName: EnumerationMapping <id> : COLUMN` — enum-typed property.
- `propName: Binding <path> : COLUMN` — semi-structured (JSON / binary) property.
- `propName ( ... )` — normal embedded (child columns from the same relation).
- `propName () Inline [<setId>]` — inline embedded (delegates to another set).
- `+localProp: Type[mult] : <rhs>` — local (mapping-scoped) property.

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
firstName:   $src.'FIRST NAME',
ageInMonths: $src.AGE * 12,
greeting:    'Hello ' + $src.'FIRST NAME'
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

### 2.10 Union mapping

```
*Person: Operation { meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(rfSet1, rfSet2) }

*Person[rfSet1]: Relation { ~func myPkg::personSet1Func():Relation<Any>[1]  ... }
*Person[rfSet2]: Relation { ~func myPkg::personSet2Func():Relation<Any>[1]  ... }
```

Mixed Relation + Relational leaves are supported. **All leaves must resolve to
the same store** — the router rejects cross-store unions.

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
| Routed graph | Same shape, but `_relationFunction.expressionSequence` is now a `ClusteredValueSpecification` carrying the target store. |
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
| Second | Resolves the source (`~func` by descriptor, `~src` by inline compilation), attaches it as `_relationFunction`, extracts the row `GenericType` from the function's last expression, and builds each property's `_valueFn` lambda typed with `$src` bound to that row type. |
| Third | Resolves `~primaryKey` names against the row type's columns (hard error with an "Available columns: [...]" message on miss). If `~primaryKey` was omitted, leaves it empty for runtime inference. |

### 5.1 Bare-column → `$src.<col>` lowering

In the Second pass, a property mapping authored as `firstName: FIRSTNAME` gets
its `_valueFn` synthesised as if the user had written `firstName: $src.FIRSTNAME`.
Every downstream consumer (validator, SQL generator, composer, protocol
transfer) sees a single shape — a `LambdaFunction` body — regardless of which
surface syntax was used.

The trade-off: **bare-column authoring is round-trip-lossy**. The composer
will re-render it as the explicit `$src.<col>` form. Semantics are identical.

### 5.2 The `asColumnRef` fast-path helper

Consumers that need to recover the original column name (SQL push-down fast
paths, IDE displays, debug output) use `RelationFunctionPropertyMappingTools.asColumnRef`,
which pattern-matches a `_valueFn` body of exactly one `$src.<col>` accessor
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
- **Lazily at SQL-generation** — yes. `processRelationFunctionClassMapping`
  and a handful of property-navigation entry points call
  `ensureRelationFunctionPrimaryKeyResolved` right before they need PK data.
  That's what makes PK inference "just work" without a compile-time pass.

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
`$src` parameter (whatever name the user chose) is bound to the synthetic
cursor's alias via `updateFunctionParamScope`. Processing the body produces a
relational operation tree whose leaves are placeholder `TableAliasColumn`s.
`expressionTouchesVariant` decides whether the body reaches into semi-structured
territory.

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
body). Bare-column authoring re-emits as the explicit `$src.<col>` form
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
| Protocol transfer | `core/pure/protocol/vX_X_X/models/dsl/mapping.pure`, `core/pure/protocol/vX_X_X/transfers/mapping.pure` |

---

## 13. Decision Cheat-sheet

| Question | Answer |
|----------|--------|
| Difference between `~func` and `~src`? | `~func` references an existing Pure function; `~src` inlines a zero-arg expression. The compiler treats both uniformly after wrapping `~src` in a synthetic lambda. |
| What property RHS forms are supported? | Bare column identifier (lowered to `{$src.<col>}`) or a full Pure expression over `$src`. |
| When should I omit `~primaryKey`? | When the function body's leaves are recognised by a registered `RelationElementAccessorExtension` and the operator chain preserves PK ([§8.3](#83-platform-relation-operators)). Otherwise declare it explicitly. |
| Can I map multiple PK columns? | Yes: `~primaryKey: [COL1, COL2]`. |
| Property types supported? | Primitives, `Enumeration` (with `EnumerationMapping`), `Variant`, and complex `Class` types (with `Binding` for binding-style, or a variant-touching valueFn for lift-style). `[*]` multiplicities are honoured when the valueFn body's multiplicity is subsumed. |
| Property types rejected? | Structural containers `Map`, `List`, `Pair` — they are `Class` but unsupported by the RFPM lift. |
| Where does multiplicity / type validation happen? | In `MappingValidator`, after the compiler has typed every `_valueFn`. Skipped when a transformer is present (the transformer owns the conversion). |
| How does `$x.address.city` resolve on a normal-embedded set? | Direct child lookup in the embedded set's `propertyMappings`. |
| How does `$x.address.city` resolve on an inline-embedded set? | Via `_classMappingByIdRecursive` on the mapping (`inlineEmbeddedRelationFunctionMapping`). |
| Can inline embedded use a different relation function? | Yes — the inline target set is fully independent. |
| Can a Relation mapping participate in a union? | Yes. All leaves must resolve to the same store. |
| Can I mix Relation and Relational leaves in a union? | Yes, as long as they share the same store. |
| What happens to milestoning columns in a union with an RF leaf? | The RF leaf contributes empty milestoning columns; temporal filtering does not apply to that branch. |
| Cross-store union? | Not supported — enforced during store-contract resolution. |
| How does semi-structured / variant lift work? | The RFPM transformer evaluates the property's `valueFn` against a *synthetic RF cursor*, detects variant-ness, and picks a downstream shape ([§9.3](#93-property-navigation-and-the-rfpm-lift-path)). |
| How do local properties differ from class properties? | `+name: Type[mult]` declares a property that exists only within the mapping scope; the canonical Pure class is unchanged. |
| Will bare-column authoring round-trip verbatim? | Semantics: yes. Syntax: no — the composer re-emits it as `$src.<col>`. |

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

