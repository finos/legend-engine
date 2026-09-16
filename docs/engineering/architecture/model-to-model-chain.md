# Model-to-Model Chains

> **Related docs:**
> [Architecture Overview](overview.md) | [Domain Concepts](domain-concepts.md) |
> [Router & Pure-to-SQL](router-and-pure-to-sql.md) | [Execution Plans](execution-plans.md) |
> [ModelJoin](model-join.md) | [Semi-structured modelling guide](../../guides/semi-structured-modelling.md)

> **Scope.** A *model chain* is a model-to-model (M2M) mapping whose source class is itself mapped
> to a real store, joined to that second mapping by a `ModelChainConnection` in the runtime. This
> document covers how a chain is declared, how a query against the target model is rewritten and
> routed down to the backing store, which query shapes are supported, and where it still breaks.
>
> The store at the bottom of the chain is usually relational, and every worked example here is, but
> nothing in `chain.pure` is relational-specific — the final routing hands off to whatever
> `StoreContract` owns the last mapping.

---

## 0. What a model chain is

An M2M mapping transforms one model into another: `Firm` is populated from `S_FirmRow` by a
`: Pure` class mapping with a `~src` clause. Normally the `~src` instances arrive from outside —
a JSON payload through a `JsonModelConnection`, say.

A chain says instead: *that source class is not a payload, it is itself mapped to a store, so go
and fetch it.* The query is written against the target model (`Firm`), and execution reaches the
store that backs the source model (`S_FirmRow` → a relational table).

```
   query over Firm
        │
        ▼
   M2M mapping          Firm : Pure { ~src S_FirmRow ... }
        │
        ▼                                        ← the chain, declared by ModelChainConnection
   relational mapping   S_FirmRow : Relational { ... }
        │
        ▼
   SQL
```

This is what makes a target model usable as a published, store-agnostic interface: consumers query
`Firm` and never see `S_FirmRow` or the table under it.

---

## 1. Declaring one

Two pieces. A `ModelChainConnection` naming the mapping(s) below this one:

```
###Connection
ModelChainConnection chain::connection::FirmModelChainConnection
{
  mappings: [ chain::mapping::Relational ];
}
```

and a runtime that binds it to `ModelStore`, alongside the connection for the real store:

```
###Runtime
Runtime chain::runtime::FirmRuntime
{
  mappings: [ chain::mapping::M2M ];
  connections:
  [
    ModelStore:        [ chain: chain::connection::FirmModelChainConnection ],
    chain::store::DB:  [ h2:    chain::connection::FirmH2Connection ]
  ];
}
```

The runtime's own `mappings` is the top of the chain (the M2M mapping); the connection's `mappings`
are the rest, in order. The metamodel is one field — `meta::external::store::model::ModelChainConnection`
(`core/pure/mapping/modelToModel.pure:82`):

```pure
Class meta::external::store::model::ModelChainConnection extends Connection
{
   mappings : Mapping[*];
}
```

> **A chain cannot be expressed in a Mapping test suite.** `MappingTestRunner` builds its runtime
> purely from the test's store-test-data bindings, so a `testSuites` block has nowhere to name a
> `ModelChainConnection`. Use a Service, which carries an explicit Runtime, or build the runtime by
> hand in Pure. A mapping test over a chained model fails with
> `Found unexpected connection type for TabularDataSet Query: JsonModelConnection`, which is the
> runner's inability to express the chain rather than a defect in the chain.

---

## 2. Pipeline at a glance

Everything lives in `core/store/m2m/chain.pure`, reached from the M2M store contract.

| Path | Entry | Called from |
|---|---|---|
| Plan generation | `planExecutionChain` | `storeContract.pure:91` → `modelToModel.pure:167-169` |
| Interpreted execution | `executeChain` | `storeContract.pure:52` |
| Graph fetch | `planModelChainConnectionGraphFetchExecution` | `modelToModel.pure:471` — a **separate** route |

Graph fetch never reaches `planExecutionChain`; it is dispatched earlier and builds its plan from a
graph-fetch tree instead of a rewritten expression. That split matters when reading §6.

`planExecutionChain` does four things:

1. **Check the return type.** Only `TabularDataSet` and `Relation` are supported (§4).
2. **Reprocess the query** — `allReprocess` folds `reprocess` over `$mapping->concatenate($mcc.mappings->init())`,
   rewriting the expression so it reads the *source* model rather than the target.
3. **Route the rewritten query** against `$mcc.mappings->last()`, the mapping that actually reaches
   a store.
4. **Plan each resulting cluster**, which delegates to the target store's own `StoreContract.planExecution`.

So with `mappings: [A, B, C]` in the connection and `M` in the runtime, the query is reprocessed
through `M, A, B` and finally routed against `C`. A single-link chain reprocesses through the M2M
mapping and routes against the relational one.

---

## 3. Query reprocessing

`reprocess` (`chain.pure`) is a recursive walk over the query AST. Two things happen to every node.

### 3.1 Property hops become `eval`

A property on the target model has no meaning downstream, so each hop is replaced by the M2M
property mapping's transform lambda, applied to the rewritten source expression via
`createEvalFunctionExpression`. `$x.name` becomes:

```
eval({s | $s.firm.legalName->toUpper()}, $src)
```

and a two-hop path nests:

```
$x.holdings.pieceId
  → eval({c | $c.collateralId.identifier},
         eval({s | $s.firm.terms->subType(@S_RepoTerms).collateral}, $src))
```

This nesting is the single most important fact about chains. Every downstream consumer — including
the relational SQL generator — sees `eval`, not a property path, and anything that walks it must
carry context from the argument into the lambda body. §7 is a defect that came from not doing so.

### 3.2 Types are re-sourced

Generic types referring to the target class are rewritten to the chain's source class, so a
`VariableExpression` typed `Firm` becomes typed `S_FirmRow`, and a `FunctionExpression`'s
`genericType` and `resolvedTypeParameters` follow.

The rewrite is deliberately *not* applied to a `Relation`'s type argument. A
`Relation<RelationType(...)>` describes the columns the relation produces, not a mapped class;
re-pointing it at the source class produces a plan-generation failure casting `Class<S_FirmRow>` to
`RelationType`.

### 3.3 Column specifications

`project`, `groupBy` and friends carry their lambdas inside column-specification objects rather
than bare, and those are plain classes — not `ValueSpecification`s — so `reprocess` needs an
explicit arm per family:

| Family | Types | Field re-sourced |
|---|---|---|
| TDSv1 | `BasicColumnSpecification` | `func` |
| TDSv1 | `WindowColumnSpecification` and other `ColumnSpecification`s | none — their lambdas are over `TDSRow`, downstream of the chain boundary |
| Relation | `ColSpec`, `ColSpecArray` | none — names only |
| Relation | `FuncColSpec`, `FuncColSpecArray` | `function` |
| Relation | `AggColSpec`, `AggColSpecArray` | `map` only; `reduce` runs over values the map side already produced |

Two mechanical traps, both of which produce misleading errors:

- **Rebuild with `dynamicNew`, not `^$x(...)`.** Re-sourcing changes the spec's first type
  argument, and a copy preserves the stale one, failing to type-check. Read the replacement type
  argument off the rebuilt *lambda*, not off the rebuilt spec: `dynamicNew` does not carry type
  arguments through in compiled mode, so `genericType().typeArguments` is populated under the
  interpreter and empty when compiled.
- **`BasicColumnSpecification` must avoid `buildLambdaType`.** It copies the lambda's parameter
  nodes, and a `col()` lambda arrives still *active*; copying an active `VariableExpression` forces
  evaluation of the bound variable, surfacing as
  `Variable 'x' is not defined in the current variable context`. Build the type from
  `functionType()` instead, as the `AggregateValue` arm does.

---

## 4. Supported query shapes

| Shape | Supported | Notes |
|---|---|---|
| `project([lambdas], [names])` — TDSv1 | yes | |
| `project([col(...)])` — TDSv1 | yes | |
| `project(~[...])` — Relation / TDSv2 | yes | |
| `graphFetch(...)` | yes, via a separate route (§2) | subject to §6 |
| Anything else returning a `Class` | no | `planExecutionChain` asserts the return type is `TabularDataSet` or `Relation` |

The assert is deliberately a whitelist rather than absent. The body of `planExecutionChain` is
generic over `ResultType`, which is an argument that `Relation` works — not that a bare
`Firm.all()` returning objects does. Without the guard that shape fails with a cast error deep
inside cluster planning instead of one clear sentence.

---

## 5. Semi-structured data through a chain

The interesting case, and the one that motivated most of this document: the relational mapping at
the bottom reads a `SEMISTRUCTURED` column through a `Binding`.

```
chain::source::S_FirmRow: Relational
{
  id:   [chain::store::DB]FIRM_SCHEMA.FIRM_TABLE.ID,
  firm: Binding chain::store::FirmBinding : [chain::store::DB]FIRM_SCHEMA.FIRM_TABLE.FIRM_DETAILS
}
```

The binding's classes (`S_Firm`, `S_Division`, `S_Collateral`, …) are **not** class mappings in the
relational mapping. Nothing registers them; a
`SemiStructuredEmbeddedRelationalInstanceSetImplementation` is synthesised on demand by
`buildSemiStructuredPropertyMapping` while the SQL generator walks into the document, and discarded
afterwards.

That has a direct consequence for chains. An M2M mapping onto an embedded array needs a second
`: Pure` class mapping for the element type, and its `~src` class exists only inside the binding:

```
*chain::model::Holding: Pure
{
  ~src chain::source::S_Collateral      // reachable only via firm.terms->subType(...).collateral
  pieceId: $src.collateralId.identifier
}
```

Resolving that class by lookup is impossible — there is no class mapping to find, and a binding
model can reach the same class by two different JSON paths, so a class-keyed resolver would be
ambiguous by construction. It only works because the traversal that synthesised the set
implementation carries it forward. See §7.

> **Not chain-specific:** a nested flatten materialises a row for an intermediate collection even
> when nothing is projected from it, so a term carrying no `collateral` key yields a null row rather
> than nothing. The same projection written directly against the relational mapping produces the
> same row. Recorded in the
> [semi-structured modelling guide](../../guides/semi-structured-modelling.md#what-does-not-work-yet).

---

## 6. Known limitation — graph fetch behind a `Binding`

**Graph fetch cannot read any property mapped through a `Binding`.** Not array-specific: a tree
asking only for a to-one `String` behind the binding fails the same way.

Graph-fetch routing resolves a property's set implementations through
`mapping->classMappingById` (`graphFetch_routing.pure:58`). A binding-derived set implementation is
never in `mapping.classMappings` — §5 — so `sets` comes back empty and
`relationalGraphFetch.pure` has nothing to plan from. That site now asserts and names the
limitation rather than failing on a bare `toOne()` cast.

**Resolving the sets would not be enough.** Array elements need an identity, and `resolvePrimaryKey`
unwraps an embedded set implementation to its `setMappingOwner`, so every element of an embedded
array resolves to the *root table's* primary key. Nothing in the relational metamodel carries an
array ordinal — `SemiStructuredArrayFlatten` holds only `navigation` — and no dialect emits one.
`assertConditionsOnSetImpl` would pass, because the root key is non-empty, so a plan would be
generated returning rows the object builder cannot tell apart. Lifting the crash on its own would
trade a clear failure for silently wrong data.

Supporting it therefore means adding a per-element key across the metamodel and every dialect
emitter, plus recursive synthesis of nested binding-derived sets during routing — a signature change
in store-generic core routing. The four parked tests in §8 are the acceptance criteria.

A projection over the same model is **not** a workaround: it returns flat rows, not a nested object
graph. It is a different query.

---

## 7. Defects fixed, and why they were subtle

Recorded because each took a wrong turn that is easy to repeat.

**Relation queries were rejected outright**, by an assert admitting only `TabularDataSet`. Widening
it is necessary but not sufficient: `project(~[...])` survives routing as an `InstanceValue`
wrapping a `FuncColSpecArray`, so the query then failed on a missing match arm (§3.3) — the same
defect as `col()`, in a second type family. A third piece was the `RelationType` re-sourcing in
§3.2. One symptom, three causes.

**A class reachable only through a binding could not be routed** —
`The system can't find a mapping (0) for the class 'chain::source::S_Collateral'`. The cause was not
the mapping lookup that raised it. Because a chain rewrites property hops into nested `eval`s
(§3.1), `processEvaluate` in `pureToSQLQuery.pure` was resolving the argument, **discarding the
property mapping it resolved to**, and processing the lambda body with the *caller's* mapping. The
inner `eval` synthesised the binding-derived set implementation and threw it away, so the outer body
reached `findPropertyMapping` with no context and fell through to `rootClassMappingByClass`, which
only returns statically declared root class mappings. Queries whose properties all sat on the root
class passed, which is why it only showed up one level down. `processMap` was already carrying the
mapping through correctly; `findPropertyMapping` needed no change.

**`project([col(...)])` was unsupported**, for the missing-arm reason in §3.3. Every pre-existing
test used the two-list form, which is why it went unnoticed.

One consequence worth knowing when reading plans: carrying the property mapping through `eval` lets
a directly-mapped column resolve its real relational type instead of the `VARCHAR(8192)` placeholder,
so chained TDS plans now report e.g. `VARCHAR(200)` in the `type = TDS[...]` row — matching what the
same plan's `resultColumns` line always reported.

---

## 8. Tests

| Where | What |
|---|---|
| `meta::relational::tests::semistructured::modelChain` | 12 parameterised tests — TDSv1, `col()`, Relation and graph-fetch shapes over one fixture. Runs on every dialect the parameterised suite covers. Model: `core_relational/relational/tests/semistructured/model/semiStructuredModelChain.legend` |
| `relational-emit-models/relational-semistructured-model-chain` | EMIT model, one Service |
| `core/store/m2m/tests/legend/chain/` | Non-semi-structured chain tests: simple, filter, parameters, data quality |

The four graph-fetch tests are parked with `<<paramTest.Ignore>>`, keeping their expected output as
the acceptance criteria for §6. `collectParameterizedTests` only collects `paramTest.Test`, so they
leave every dialect collection cleanly.

The fixture is deliberately awkward: one row has a division with an empty team array and a term with
no collateral key, and another has neither key at all, so outer-flatten behaviour is asserted rather
than assumed.

Two harness notes:

- **The Pure IDE delta loop runs interpreted**, and disagrees with compiled mode here — see the
  `dynamicNew` note in §3.3. A green result in the IDE is not a substitute for the real suites.
- **The EMIT model carries only the relation Service.** A TabularDataSet service reaches H2, because
  `RelationalConnectionFactory` picks DuckDB only when the query returns a `Relation`, and then fails
  inside `legend_h2_extension_flatten_array`, which cannot read the document the model's `###Data`
  block loads. The identical query passes on both H2 and DuckDB under the PCT harness, whose CSV
  loading differs — an EMIT data-loading gap, not an engine defect. `relational-semistructured`'s
  mapping already states the convention that these models return a `Relation` so they do not
  "silently run somewhere without a `SEMISTRUCTURED` type".

---

## 9. See also

| Document | Relevance |
|---|---|
| [Router & Pure-to-SQL](router-and-pure-to-sql.md) §4 | `pureToSqlQuery`, including `processEvaluate` — where §7's defect lived |
| [Semi-structured modelling guide](../../guides/semi-structured-modelling.md) | User-facing capabilities and current limits of `SEMISTRUCTURED` columns |
| [Execution Plans](execution-plans.md) | `ExecutionNode` shapes the chain's clusters produce |
| [ModelJoin](model-join.md) | The other store-agnostic model-level feature, and a close structural analogue |
