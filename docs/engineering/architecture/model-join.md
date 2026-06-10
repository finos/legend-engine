# ModelJoin: Parser → Compiler → Router → Pure-to-SQL Developer Guide

> **Audience.** Engine developers touching the Pure grammar, mapping compiler, router,
> or `pureToSQLQuery.pure`. Assumes familiarity with `PureModelContextData`, the
> mapping compiler's `HelperMappingBuilder`, the router's `routeFunction` /
> `PermutationSet` machinery, and the relational SQL generator (SWC, `JoinTreeNode`,
> `Operation`, `processValueSpecification`).
>
> **Authoritative sources** (referenced throughout):
>
> | Stage | File |
> |------|------|
> | Parser | `legend-engine-language-pure-grammar/.../mapping/ModelJoinAssociationMappingParseTreeWalker.java` + `ModelJoinAssociationMapping{Lexer,Parser}Grammar.g4` |
> | Compiler | `legend-engine-language-pure-compiler/.../HelperMappingBuilder.java` (`processModelJoinAssociationMapping`, `rewriteModelJoinExpression`, `resolveLambdaParamNames`) |
> | Router | `legend-engine-pure-code-compiled-core/.../core/pure/router/store/builder.pure` (`potentiallyRoutePropertyMapping`, `ModelJoinPropertyMapping` arm) |
> | Pure→SQL | `legend-engine-xt-relationalStore-core-pure/.../pureToSQLQuery.pure` (`processModelJoinPropertyMapping`, `compileModelJoinForBranch`, `buildModelJoinPlaceholder`, `buildBranchSourceSWC`, `materializeTargetAsSubselect`) |
>

---

## 0. What is a ModelJoin?

**ModelJoin is a store-agnostic way to declare a relationship between two model
classes — at the model layer, with no reference to physical-store details.**

A model in Legend describes domain classes. To answer queries that traverse
associations between those classes, the engine needs to know how the two sides
relate *physically* (which columns, which join keys, which predicate). Historically
this was expressed by association mappings whose join condition is written in the
target store's vocabulary:

- For Relational, the user writes a `Join` in the database DSL — referencing real
  tables and columns — and binds the association to that database join.
- For other stores, the user reaches for `XStore`, originally designed for truly
  cross-store relationships.

### The problem this creates

The condition leaks physical details into the model, and worse, into any consumer of
that model:

- **Coupling at the physical layer.** A consumer who wants to use the association
  must import the producer's database definition and mapping in order to compile
  their query. The model's logical contract drags its physical realisation with it.
- **No re-targetability.** The same association cannot be re-pointed at a different
  underlying store without rewriting the join.
- **Composition of independently-modelled domains is awkward.** Two teams that have
  modelled their domains separately cannot describe a relationship between their
  classes without one of them adopting the other's store.

### Why XStore wasn't the answer

`XStore` was originally introduced for cross-store associations (e.g. one side in
Snowflake, the other in Mongo). In practice teams started using it for the
*localised* case too — both sides in the same store, but expressed as model-level
join conditions to avoid the physical coupling above (the "localize XStore" hack).
That use was always a square peg in a round hole:

- Only a hand-picked whitelist of Pure functions worked (`equal`, `and`, `or`,
  `not`, the comparison operators).
- Only **single-level property access** — `$this.prop` was fine, `$this.prop1.prop2`
  was not.
- No `toLower`, `toString`, `if`, `isNotEmpty`, or any other ordinary Pure function.
- No support for unions on either side.
- The expression walker was hand-rolled (`transformExpressionSequenceIntoJoin`
  + `convertToRelationalElement`), duplicating logic the regular Pure-to-SQL
  expression compiler already did correctly.

### What ModelJoin does differently

ModelJoin is a first-class association-mapping kind whose join condition is a Pure
lambda over the two associated classes:

```
Association test::FirmEmployees { employer: test::Firm[1]; employees: test::Person[*]; }

Mapping test::M
(
   ...
   test::FirmEmployees: ModelJoin
   {
      employees: { firm:Firm[1], person:Person[1] | $firm.region.code == $person.addr.code }
   }
)
```

Two consequences follow:

1. **The condition is store-agnostic at definition time.** It references model
   classes and properties; nothing in the lambda body names a table, column, or
   join. The same `ModelJoinAssociationMapping` can apply to *any* store kind
   provided that store knows how to translate it.
2. **The condition flows through the standard Pure pipeline.** Routing treats the
   lambda as ordinary `routeFunction` input (§4), so the Pure router resolves each
   property access against the right store/mapping. For Relational, translation
   reuses the regular `processValueSpecification` SQL generator (§5), so anything
   that compiles in a normal Pure query — `if`, `toLower`, nested property
   navigation, function composition — works inside a ModelJoin condition for free.

### Scope and current state

- **Routing is store-agnostic.** §5 has no Relational-specific knowledge; it just
  routes the lambda body and narrows `PermutationSet`s to the MJPM target.
- **Translation is currently implemented for Relational only** — the most common
  store backing model joins. Other stores can plug in by extending
  `processPropertyMapping`'s `m:ModelJoinPropertyMapping[*]` dispatch with their own
  lowering.
- **Cross-store ModelJoin** (different stores on each side) is *not* in scope. The
  router's "exactly one cluster" assertion (§5 step 4) makes that explicit:
  cross-store joins remain XStore's territory. ModelJoin replaces the *localised*
  XStore use, not its original purpose.
- **Capability parity with regular Pure.** Anything `processValueSpecification`
  supports works inside a ModelJoin condition. Nested property access on either
  side, unions on either side, milestoning, filters/groupBy/distinct on the target
  class mapping, and self-associations are all supported.

The rest of this document covers, stage by stage, how a ModelJoin lambda is parsed,
compiled, routed, and finally translated to SQL by the Relational store.

---

## 1. Pipeline at a glance

```
   Mapping grammar text
            │
            ▼  (1) Parser
   ModelJoinAssociationMapping  [protocol POJO]
       { association, joinCondition: LambdaFunction (user param names) }
            │
            ▼  (2) Compiler
   ModelJoinAssociationImplementation  [Pure graph]
       └── ModelJoinPropertyMapping × (M × N × 2 directions)
              joinCondition uses sentinels  _mj_src / _mj_tgt
            │
            ▼  (3) Router
   Same MJPMs, but joinCondition is now ROUTED
       (each sub-expression annotated with its target store/mapping)
            │
            ▼  (4) Pure → SQL  (relational store)
   Synthetic RelationalPropertyMapping per (src, tgt) pair
            │
            ▼
   Existing union/single-target SQL generation
```

Each stage's job is small and well-bounded. The two non-obvious bits are:

- **Sentinel rewrite** happens at the boundary between Parser and Compiler. The user
  writes `{firm, person | …}`; everything downstream sees `{_mj_src, _mj_tgt | …}`.
- **The MJPM cross-product** happens in the Compiler. By the time the router and SQL
  generator see the mapping, "one user lambda" has already become "M×N MJPMs sharing
  the same lambda body".

---

## 2. The running example

We use this throughout. It exercises all four stages and is rich enough to show
nested-property navigation on both sides; §6 also adds a *contrasting* union variant.

```
###Pure
Class test::Person  { firstName: String[1]; firmId: String[1]; addr: test::Addr[1]; }
Class test::Firm    { id: String[1]; region: test::Region[1]; }
Class test::Region  { code: String[1]; }
Class test::Addr    { code: String[1]; }
Association test::FirmEmployees { employer: test::Firm[1]; employees: test::Person[*]; }

###Mapping
Mapping test::M
(
  test::Firm   [firm_set]:    Relational { ~mainTable [db]firmTbl    id     = [db]firmTbl.ID,    region = [@firm_region] }
  test::Person [person_set]:  Relational { ~mainTable [db]personTbl  firstName = [db]personTbl.FNAME,
                                                                      firmId    = [db]personTbl.FIRMID,
                                                                      addr      = [@person_addr] }
  test::Region [region_set]:  Relational { ~mainTable [db]regionTbl  code   = [db]regionTbl.CODE }
  test::Addr   [addr_set]:    Relational { ~mainTable [db]addrTbl    code   = [db]addrTbl.CODE }

  test::FirmEmployees: ModelJoin
  {
     employees:  { firm:Firm[1], person:Person[1] | $firm.region.code == $person.addr.code }
  }
)
```

A query such as
`Firm.all()->filter(f | $f.employees->exists(p | $p.firstName == 'A'))` will be the
trigger that walks all four stages.

> **Why typed parameters here?** With untyped params (`{firm, person | …}`) and the
> association `(employer:Firm, employees:Person)`, the compiler's name-based pairing
> would fail (`firm` ≠ `employer`, `person` ≠ `employees`) — see §5 step 4. Typing
> the params makes the example pair-resolve via the type-based path while keeping
> the readable names `firm` / `person`.

---

## 3. Parser

### Grammar registration

`MappingParserGrammar.g4` recognises `ModelJoin` as a mapping-element kind. The
mapping parser dispatches the inner body to a dedicated sub-grammar so the lambda
can be parsed by the standard domain parser:

```
// ModelJoinAssociationMappingParserGrammar.g4
parser grammar ModelJoinAssociationMappingParserGrammar;
import M3ParserGrammar;
modelJoinAssociationMapping:  combinedExpression EOF ;
```

The grammar deliberately delegates to `combinedExpression` from `M3ParserGrammar` —
this gives ModelJoin first-class access to the same expression syntax used by `meta::`
function bodies. There are no ModelJoin-specific tokens beyond what `M3` already has.

### Walker → protocol POJO

`CorePureGrammarParser.parseModelJoinAssociationMapping` constructs a
`ModelJoinAssociationMapping` POJO and drives `ModelJoinAssociationMappingParseTreeWalker`.
The walker has exactly one job: take the `combinedExpression`, parse it via
`DomainParser.parseCombinedExpression`, and assert it is a `LambdaFunction`:

```java
// ModelJoinAssociationMappingParseTreeWalker.java
public void visitModelJoinAssociationMapping(... ctx, ModelJoinAssociationMapping mj)
{
    mj.joinCondition = visitLambda(ctx.combinedExpression());
}

private LambdaFunction visitLambda(... ctx)
{
    String lambdaString = this.input.getText(...);
    ValueSpecification vs = new DomainParser().parseCombinedExpression(lambdaString, ...);
    if (!(vs instanceof LambdaFunction))
    {
        throw new EngineException(
            "ModelJoin association mapping requires a lambda join condition of the form "
          + "'{src: SrcClass[1], tgt: TgtClass[1] | <boolean expression>}'",
            this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
    }
    return (LambdaFunction) vs;
}
```

Things the parser **does not** do:

- It does **not** check arity (2 params), parameter types, or self-association rules
  — those are compiler concerns.
- It does **not** rewrite parameter names. The lambda stored on
  `ModelJoinAssociationMapping.joinCondition` carries the user's original param names
  (`firm`, `person` for our example).

### Protocol shape after parsing

```
ModelJoinAssociationMapping
├── association  → PackageableElementPointer("test::FirmEmployees")
├── id           → null  (defaults to derived id later)
└── joinCondition: LambdaFunction
    ├── parameters: [Variable(name="firm",   genericType=Firm),
    │                Variable(name="person", genericType=Person)]
    └── body: [ AppliedFunction(equal, [
                  AppliedProperty(code, [AppliedProperty(region, [Var(firm)])]),
                  AppliedProperty(code, [AppliedProperty(addr,   [Var(person)])])
              ])]
```

Round-tripping (`composer` → grammar) is symmetric and tested in
`TestModelJoinAssociationMappingRoundTrip`.

### Common parser-stage failures

| Input | Error |
|------|-------|
| Bare expression — `firm.id == person.firmId` (no `{… \| …}` braces) | `PARSER error … ModelJoin association mapping requires a lambda join condition of the form '{src: SrcClass[1], tgt: TgtClass[1] \| <boolean expression>}'` |
| Syntax error inside the lambda body | Whatever `DomainParser` raises, with source info pointing into the body. |

---

## 4. Compiler

Entry point: `HelperMappingBuilder.processModelJoinAssociationMapping`
(routed from `processAssociationMapping` via the `ModelJoinAssociationMapping` arm).
Output: a fully-typed `ModelJoinAssociationImplementation` with
M × N × 2 `ModelJoinPropertyMapping`s on it.

### Step 1 — Validate and resolve the association

```java
Association assoc = context.resolveAssociation(mj.association.path, ...);
if (assoc._properties().size() != 2)
{
    throw new EngineException("ModelJoin requires an association with exactly 2 properties", ...);
}
Property prop1 = ...;   // employer  → Firm
Property prop2 = ...;   // employees → Person
```

ModelJoin is binary by definition; non-binary associations are rejected here.

### Step 2 — Pick the rewrite-direction property names

For each direction, the lambda body is rewritten so that the *owning* side maps to
`_mj_src` and the *navigated* side maps to `_mj_tgt`. We need the two association
property names in their pre-milestoning form so the rewrite key matches the user's
parameter names (or, after pairing in step 4, the user's parameters):

```java
String name1 = resolveOriginalPropertyName(prop1, assoc);   // "employer"
String name2 = resolveOriginalPropertyName(prop2, assoc);   // "employees"
```

`resolveOriginalPropertyName` consults `_originalMilestonedProperties` so a
generated milestoning suffix (e.g. `employerAllVersions`) is collapsed back to
the original `employer`.

### Step 3 — Emit two protocol-level MJPMs

One per direction. **Both share the same input lambda** at this point; the body
rewrite happens inside `rewriteModelJoinExpression`:

```java
ModelJoinPropertyMapping pm1 = createModelJoinPropertyMapping(mj, "employees",
        rewriteModelJoinExpression(joinCondition, "employer", "employees", prop1, prop2, ctx));

ModelJoinPropertyMapping pm2 = createModelJoinPropertyMapping(mj, "employer",
        rewriteModelJoinExpression(joinCondition, "employees", "employer", prop2, prop1, ctx));
```

For each direction we tell the rewriter "src side is the owning side of this
direction; tgt side is the navigated side". The lambda body is walked twice (once
per direction), producing two independent rewritten lambdas.

### Step 4 — Pair the user's lambda parameters with the association properties

This is the trickiest piece, in `resolveLambdaParamNames`. Given:

- The user's two lambda parameters (with optional types).
- The `(thisProp, thatProp)` pair the caller wants to substitute as `_mj_src` / `_mj_tgt`.

We need a deterministic mapping `[paramName_for_this, paramName_for_that]`.

Resolution order:

1. **Type-based** — if both params are typed *and* the association is not
   self-referential, pair by subtype check. Self-associations are skipped here
   because both orderings would match (both property types equal → ambiguous).
2. **Name-based fallback** — pair by `param.name == associationProperty.name`.
   Required for self-associations and untyped lambdas.

If both fail we throw, with one of three messages chosen for clarity:

| Situation | Message (paraphrased) |
|----------|------------------------|
| Both typed, not self-assoc, types don't pair | "ModelJoin lambda parameter types do not match association property types: expected one parameter of type 'Firm' and one of type 'Person', got '…'" |
| Self-association, names don't match | "Self-association ModelJoin requires lambda parameter names to match the association property names ('parent' and 'child'), got '…' and '…'" |
| Untyped, names don't match | "Cannot pair ModelJoin lambda parameters 'a' and 'b' with association properties 'employer' and 'employees'. Either type the parameters explicitly (e.g. {a:Firm[1], b:Person[1] \| …}) or rename the parameters to match the association property names ('employer' and 'employees')." |

For our running example `{firm:Firm[1], person:Person[1] | …}`:

- Both typed → enter type-based path.
- `Firm` is a subtype of `firm`'s declared type `Firm`, and `Person` of `person`'s
  declared `Person` → unambiguous pair.
- Returns `["firm", "person"]` (param0 stands in for `_mj_src`, param1 for `_mj_tgt`)
  for the `employees`-direction call; symmetric flip for the `employer` direction.

### Step 5 — Rewrite the lambda body, twice

`rewriteModelJoinExpression` walks the body and replaces references to the user's
two parameters with `_mj_src` / `_mj_tgt` sentinels. The walker handles
`Variable`, `AppliedProperty`, `AppliedFunction`, `Collection` and recurses
through nested structures. Variable replacement is keyed by *name*, so it is safe
on both typed and untyped lambdas.

Sentinel constants live on `HelperMappingBuilder`:

```java
public static final String MODEL_JOIN_SOURCE_VAR = "_mj_src";
public static final String MODEL_JOIN_TARGET_VAR = "_mj_tgt";
```

For our example, after step 4 + step 5, the two MJPMs carry:

```
pm1.property = "employees"
pm1.joinCondition.body = equal($_mj_src.region.code, $_mj_tgt.addr.code)
                         ^^^^^^^^                    ^^^^^^^^
                         was $firm                  was $person

pm2.property = "employer"
pm2.joinCondition.body = equal($_mj_tgt.region.code, $_mj_src.addr.code)
                         ^^^^^^^^                    ^^^^^^^^
                         was $firm                  was $person
```

(The two directions have *the same body* with `_mj_src` / `_mj_tgt` swapped — exactly
the right semantic for the two navigation directions.)

### Step 6 — Cross-product expansion across class mappings

Each association property's class can be mapped by multiple `SetImplementation`s
(e.g. when `Person` is unioned across `personFT` and `personCT`, or when subtypes
participate). For each direction we materialise one *compiled* property mapping per
(sourceSet × targetSet) pair:

```java
MutableList<InstanceSetImplementation> firmSets   = findAllSetsForClassOrSubtypes(Firm,  …);
MutableList<InstanceSetImplementation> personSets = findAllSetsForClassOrSubtypes(Person, …);

PropertyMappingBuilder builder = new PropertyMappingBuilder(context, parentMapping, assocImpl, allClassMappings);

// pm1 navigates Person ← Firm  (src=Firm, tgt=Person)  → use (firmSets, personSets)
compileForAllPairs(firmSets, personSets,
        (src, tgt) -> builder.visitModelJoinPropertyMapping(pm1, src, tgt), out);

// pm2 navigates Firm ← Person  (src=Person, tgt=Firm)  → use (personSets, firmSets)
compileForAllPairs(personSets, firmSets,
        (src, tgt) -> builder.visitModelJoinPropertyMapping(pm2, src, tgt), out);

assocImpl._propertyMappings(out);
```

For our running example with single-set classes, this produces 2 compiled MJPMs
total (one per direction). With a unioned Person across two sets, it produces 4
(2 directions × 2 person sets). With both classes unioned 2-ways, 8.

> **Why expand here, not later?** The router and the SQL generator both iterate
> property mappings. By materialising one MJPM per concrete pair up front, we avoid
> teaching downstream stages about the cross-product. Each MJPM carries its own
> `sourceSetImplementationId` / `targetSetImplementationId` and is interchangeable
> with a hand-written single-pair `RelationalPropertyMapping`.

### Compiler output for the running example

```
ModelJoinAssociationImplementation
├── id          = "test_FirmEmployees"
├── association → test::FirmEmployees
└── propertyMappings:
    ├── ModelJoinPropertyMapping { property=employees, src=firm_set, tgt=person_set,
    │                              joinCondition = {| equal($_mj_src.region.code, $_mj_tgt.addr.code) } }
    └── ModelJoinPropertyMapping { property=employer,  src=person_set, tgt=firm_set,
                                   joinCondition = {| equal($_mj_tgt.region.code, $_mj_src.addr.code) } }
```

Note `parameters = []` on the rewritten lambdas — sentinels are referenced by name
only, no formal parameters needed (the router/SQL stages bind them via scope, not
function application).

---

## 5. Router

The router's job is to attach **store/mapping context** to every sub-expression of
the user's query so the plan generator knows which store handles each piece.
ModelJoin lambdas need special handling because their bodies reference
`_mj_src` / `_mj_tgt` — variables that have no value in the surrounding query
scope; they are bound only during property-mapping resolution.

Entry point: `core/pure/router/store/builder.pure → potentiallyRoutePropertyMapping`,
the `m:ModelJoinPropertyMapping[1]` match arm.

### Step 1 — Re-route the join-condition lambda independently

When the router descends into a property whose property-mapping is a
`ModelJoinPropertyMapping`, it *cannot* just route the surrounding expression and
stop — the lambda body itself contains property accesses (`$_mj_src.region.code`)
that need to be resolved against the source set's store, and similarly for the
target side.

So the arm runs a sub-routing pass:

```legend
m:ModelJoinPropertyMapping[1] |
   let srcVar = ^VariableExpression(name='_mj_src', genericType=$sourceRoutedVS.value.genericType,    multiplicity=PureOne);
   let tgtVar = ^VariableExpression(name='_mj_tgt', genericType=$propertyRoutedVS.value.genericType, multiplicity=PureOne);
   ...
   let routed = $m.joinCondition->routeFunction(true, $initialState, ^ExecutionContext(), $initialVars, [], $extensions, $debug);
   ...
   ^$m(joinCondition = $routedJoinCondition->cast(@LambdaFunction<{Nil[1],Nil[1]->Boolean[1]}>));
```

`$initialVars` binds `_mj_src` to a `VariableExpression` whose generic type is the
*source side's class* (taken from `sourceRoutedVS`), and `_mj_tgt` to one typed by
the *target side's class*. From this point the existing `routeFunction` machinery
resolves `$_mj_src.region` exactly as it would resolve `$f.region` in any other
lambda — by looking up the property mapping for `region` on the source set.

### Step 2 — Narrow the routed VS's `sets` to the specific MJPM target

A subtle but important step. `sourceRoutedVS` and `propertyRoutedVS` carry a
`PermutationSet` listing *all* mappings that could satisfy the source/target classes
(possibly an entire union). But this MJPM is for **one specific pair**. We must
narrow the routed VS so subsequent property resolution inside the join condition
binds to the exact sub-impl this MJPM targets:

```legend
let srcSetImpl = $srcPermSet.sets->map(s|$s->resolveOperation(...))->filter(s|$s.id == $m.sourceSetImplementationId)->toOne();
let tgtSetImpl = $tgtPermSet.sets->map(s|$s->resolveOperation(...))->filter(s|$s.id == $m.targetSetImplementationId)->toOne();
let resolvedSrcPermSet = ^$srcPermSet(sets = $srcSetImpl);
let resolvedTgtPermSet = ^$tgtPermSet(sets = $tgtSetImpl);
let srcVarVS = ^$sourceRoutedVS(sets = $srcSetImpl);
let tgtVarVS = ^$propertyRoutedVS(sets = $tgtSetImpl);
```

This is what makes the union case (§6 example B) work correctly: each MJPM gets its
own routed lambda whose property accesses bind to *its* leaf set, not the union as a
whole. `buildSQLQueryOutManySetImplementations` later collapses the per-leaf
results back into a single union join.

### Step 3 — Pre-populate the routing strategy

Bare `routeFunction` would otherwise allocate fresh `PermutationSet` ids and may
collide with the outer query's strategy. The arm pre-seeds the strategy with the
two narrowed `PermutationSet`s and bumps the counter past their ids:

```legend
let initialSets = [$resolvedSrcPermSet, $resolvedTgtPermSet];
let initialCounter = max($resolvedSrcPermSet.id, $resolvedTgtPermSet.id);
let newRoutingStrategy = ^$routingStrategy(sets = $initialSets, setsByDepth = ^Map<String, PermutationSet>());
let initialState = ^RoutingState(... counter = $initialCounter, routingStrategy = $newRoutingStrategy, ...);
```

### Step 4 — Extract the single cluster

After `routeFunction`, the join-condition expression sequence is wrapped in
`ClusteredValueSpecification`s. ModelJoin lambdas are guaranteed to live in **one
store** (cross-store ModelJoin is rejected upstream), so we assert exactly one
cluster and unwrap it:

```legend
let clusters = $routed.expressionSequence->evaluateAndDeactivate();
assert($clusters->size() == 1, 'Expected exactly one cluster for routed ModelJoin joinCondition, got ' + $clusters->size()->toString());
let cluster = $clusters->toOne()->cast(@ClusteredValueSpecification);
let routedJoinCondition = ^$routed(expressionSequence = $cluster.val);
^$m(joinCondition = $routedJoinCondition->cast(@LambdaFunction<{Nil[1],Nil[1]->Boolean[1]}>));
```

### Router output for the running example

For the `employees`-direction MJPM `(firm_set → person_set)`:

```
ModelJoinPropertyMapping
├── property = "employees"
├── source / target SetImpl ids = firm_set / person_set
└── joinCondition: LambdaFunction (ROUTED)
       body = equal(
                $_mj_src.region.code,    ← each AppliedProperty annotated with the route
                $_mj_tgt.addr.code         (region resolved to firm_set's region property mapping;
              )                            addr resolved to person_set's addr property mapping)
```

The mirror MJPM for the `employer` direction is routed independently, with
`_mj_src` typed as `Person` and `_mj_tgt` as `Firm` — even though it shares the
same source body, the property bindings flip.

### Why the router doesn't generate SQL or join trees

The router stays purely at the value-spec level. Its only ModelJoin output is a
*routed lambda* whose property accesses are tagged with the right store/mapping.
The actual translation to a SQL `JOIN` happens in step 6, which can now trust that
every `AppliedProperty` it encounters has well-defined store/mapping info.

---

## 6. Pure → SQL

The relational store consumes the routed MJPMs and produces SQL. Code lives in
`pureToSQLQuery.pure`, dispatched from `processPropertyMapping`'s
`m:ModelJoinPropertyMapping[*]` arm.

### Core idea

> **Lower the lambda once, per (src, tgt) pair, into a synthetic
> `RelationalPropertyMapping`, then hand it to the existing
> `processRelationalPropertyMapping` machinery.**

Two facts make this possible:

1. **Reuse `processValueSpecification`.** The full Pure expression compiler already
   handles `==`, `and`/`or`, `if`, function calls, nested property access, etc. We
   just give it `_mj_src`/`_mj_tgt` bound to two SWCs and let it compile the body.
2. **Synthetic RPM looks identical to a hand-written `@Join` RPM.** Once we have a
   compiled `Operation` plus the two endpoints' aliases, we wrap it in a
   `RelationalOperationElementWithJoin{joinTreeNode = …}`. From there the
   single-target / union / materialise-target paths in
   `processRelationalPropertyMapping` work unchanged.

Everything else is plumbing for two awkward facts:

- **Shared root.** `mergeSQLQueryData` (used inside `processDynaFunction`,
  `processEquals`, …) requires both operands' query trees to share a root. So we
  physically attach a target placeholder under the source root **before** compiling.
- **Multiplicity.** A ModelJoin against unions can produce many MJPMs sharing one
  lambda. We compile per pair, then feed all the synthetic RPMs to the union flow.

### Top-level dispatch

```
processPropertyMapping
  └── m:ModelJoinPropertyMapping[*]
       └── processModelJoinPropertyMapping (mjpms, property, srcSWC, …)
              │
              │ 1. group MJPMs by (srcSetId, tgtSetId)        ─── distinct pairs
              │ 2. for each pair → compileModelJoinForBranch  ─── synthetic RPM
              │ 3. processRelationalPropertyMapping(synthRPMs,…) ── existing flow
              │
              ▼
          single-pair  →  doJoinToClass
          multi-pair   →  buildSQLQueryOutManySetImplementations  (UNION ALL etc.)
```

Step 3 is the whole point. We do not re-implement union, milestoning,
materialisation or filter-push-down — those are already correct in
`processRelationalPropertyMapping`.

### `compileModelJoinForBranch` — the heart

For one (srcSet, tgtSet) pair, given the original `srcSWC` (the caller's query tree
rooted at the source class):

| Step | What happens | Output |
|------|-------------|--------|
| **A. Build branch source SWC** (`buildBranchSourceSWC`) | A *fresh* SWC rooted at the raw source table (or RelationFunction subselect). Aliases use suffix `_mj_src`. **Independent** of `srcSWC`. | `branchSrcSWC` |
| **B. Attach target placeholder** (`buildModelJoinPlaceholder`) | Build a fresh target-side SWC (raw table or RF subselect, suffix `_mj_tgt`), join it under `branchSrcSWC` via a dummy `1=1` join named `<src>_<tgt>_mj_ph`. Both `_mj_src` and `_mj_tgt` now live in one tree. | `withPlaceholder` |
| **C. Compile lambda body** | Bind `_mj_src → branchSrcSWC`, `_mj_tgt → withPlaceholder` in a `FunctionParamScope`, then `processValueSpecification(lambda.body[0], …)`. | `resultSWC` containing compiled `Operation` |
| **D. Extract** | Pull the compiled condition (`select.columns` or `filteringOperation`); locate the placeholder JTN in the result tree and the source-side intermediates that compilation may have added (e.g. for `$_mj_src.region.code`). | `compiledCondition`, `placeholderJTN`, `sourceIntermediates` |
| **E. Build the real join** | `^Join(operation = compiledCondition, target = compiledTgtAlias, aliases = [(parentOfTarget, tgt), (tgt, parentOfTarget)])`. `parentOfTarget` is the deepest source intermediate (or the branch source alias if no chain). | `realJoin` |
| **F. Materialise target if needed** (`materializeTargetAsSubselect`) | If the target side accumulated children (nested target navigation), or the target class mapping has filter / groupBy / distinct, wrap target as a `SelectSQLQuery` exposing only the columns referenced by the ON clause; rewrite `realJoin.operation` aliases to point at the subselect. | `targetJTN` |
| **G. Stitch chain** | If the source side accumulated intermediates, append `targetJTN` at their deepest leaf. | `fullChainJTN` |
| **H. Wrap as RPM** | `^RelationalPropertyMapping{relationalOperationElement = RelationalOperationElementWithJoin{joinTreeNode = fullChainJTN}, sourceSetImplementationId, targetSetImplementationId}`. | synthetic RPM |

Two points worth internalising:

- The branch source SWC in step **A** is deliberately **not** the caller's `srcSWC`.
  We need TableAliasColumns in the compiled `Operation` to refer to the *raw* source
  table (so the union infra's `modifyColumnNameInOperation` / `findFkListForEachSet`
  can rewrite them). The rooting of the result onto the caller's `srcSWC` is the
  union flow's job, not ours.
- `_mj_src` / `_mj_tgt` are only **inside** `compileModelJoinForBranch`. The synthetic
  RPM that leaves the function references real aliases, exactly like a hand-written
  `@Join` mapping.

### Worked example A — running example, nested on both sides

Recall: `{firm:Firm[1], person:Person[1] | $firm.region.code == $person.addr.code}`.
After parser + compiler + router, the MJPM the SQL stage receives carries:

```
{ | equal($_mj_src.region.code, $_mj_tgt.addr.code) }       (parameters: [])
sourceSetImplementationId = firm_set
targetSetImplementationId = person_set
```

**A. Branch source SWC** — alias `firm_src` over `firmTbl`.

```
RootJoinTreeNode(alias = firm_src @ firmTbl)
```

**B. Placeholder attached** — fresh `person_tgt` over `personTbl`, joined `1=1`.

```
RootJoinTreeNode(alias = firm_src @ firmTbl)
      └── JoinTreeNode(alias = person_tgt @ personTbl,  join = firm_set_person_set_mj_ph (1=1))
```

**C. Compile body** — `processValueSpecification` runs on
`equal($_mj_src.region.code, $_mj_tgt.addr.code)`.

It resolves `_mj_src` → `branchSrcSWC` (cursor at `firm_src`) and walks `region.code`:
that property has a `@firm_region` join, so `processRelationalPropertyMapping` adds a
real child JTN under `firm_src`. Same for the target side — `addr.code` brings in
`@person_addr` under `person_tgt`. After compilation:

```
RootJoinTreeNode(firm_src @ firmTbl)
   ├── JoinTreeNode(region_a @ regionTbl,   join = firm_region)             ← src intermediate
   └── JoinTreeNode(person_tgt @ personTbl, join = ..._mj_ph)                ← placeholder
         └── JoinTreeNode(addr_a @ addrTbl, join = person_addr)              ← tgt child (will move)

compiledCondition := (region_a.CODE = addr_a.CODE)
```

**D / E. Build the real join.**

`sourceIntermediates = [region_a-subtree]`, `parentOfTarget = region_a` (deepest leaf
of the source intermediate chain).

```
realJoin = Join {
  name      = firm_set_person_set_ModelJoin
  operation = (region_a.CODE = addr_a.CODE)
  target    = person_tgt
  aliases   = [(region_a, person_tgt), (person_tgt, region_a)]
}
```

**F. Materialise target.** Target side has the `addr_a` child, so we wrap it:

```
person_tgt_sub :=
  SELECT personTbl.*, addr_a.CODE AS CODE_1
  FROM   personTbl  AS person_tgt
  JOIN   addrTbl    AS addr_a  ON person_addr(person_tgt, addr_a)

realJoin.operation rewritten to: (region_a.CODE = person_tgt_sub.CODE_1)
realJoin.target    = person_tgt_sub
```

**G. Stitch chain.** Append the materialised target at the deepest leaf of the
source intermediate chain:

```
firm_src
   └── region_a    (join = firm_region)
         └── person_tgt_sub   (join = realJoin)
```

**H. Wrap as RPM** with `sourceSetImplementationId = firm_set`,
`targetSetImplementationId = person_set`.

#### Hand-off and final SQL shape

`processRelationalPropertyMapping` sees one synthetic RPM → `doJoinToClass`. The
chain is grafted under whatever the caller's `srcSWC` already had. Conceptually:

```sql
SELECT ...
FROM   firmTbl  AS firm_root
JOIN   regionTbl AS region_a
       ON  firm_region(firm_root, region_a)
JOIN  (SELECT personTbl.*, addr_a.CODE AS CODE_1
       FROM   personTbl AS person_tgt
       JOIN   addrTbl   AS addr_a
              ON  person_addr(person_tgt, addr_a)
      ) AS person_tgt_sub
       ON  region_a.CODE = person_tgt_sub.CODE_1
WHERE  ...
```

The source-side `region` access happens **between** the source root and the target.
That is how nested property access on the `_mj_src` side is naturally expressed —
the model-join condition's left-hand side is an ordinary multi-hop property
navigation that just happens to terminate in a comparison instead of a projection.

### Worked example B — union target, non-nested condition (contrast)

To contrast §6A and exercise the multi-set path, vary the running example so
`Person` is unioned across two leaf sets, and simplify the condition to a single-hop
equality (so we can focus on the union mechanics):

```
###Mapping
test::Person[person_ft]: Relational { ~mainTable [db]personFT  firstName=[db]personFT.FNAME, firmId=[db]personFT.FIRMID }
test::Person[person_ct]: Relational { ~mainTable [db]personCT  firstName=[db]personCT.FNAME, firmId=[db]personCT.FIRMID }
*test::Person:    Operation { meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(person_ft, person_ct) }

test::FirmEmployees: ModelJoin
{
   employees:  { firm:Firm[1], person:Person[1] | $firm.id == $person.firmId }
}
```

#### MJPMs after Pure compilation

The compiler explodes the single lambda into one MJPM per (firm leaf × person leaf),
all sharing the same rewritten lambda body:

```
mjpm1: firm_set → person_ft     joinCondition = { | equal($_mj_src.id, $_mj_tgt.firmId) }
mjpm2: firm_set → person_ct     joinCondition = { | equal($_mj_src.id, $_mj_tgt.firmId) }
```

(After §4 the bodies are also routed: `id` bound to `firm_set`'s mapping; `firmId`
bound — independently per MJPM — to `person_ft` for mjpm1 and to `person_ct` for
mjpm2.)

#### Per-pair compilation

Both pairs run §6's seven steps. Because the condition is primitive==primitive (no
nested properties) and neither target has filter / groupBy / distinct, every pair
takes the **fast path**:

- **A.** `firm_src @ firmTbl`
- **B.** placeholder under it, e.g. `person_ft_tgt @ personFT, join = firm_set_person_ft_mj_ph (1=1)`
- **C.** Compile `equal($_mj_src.id, $_mj_tgt.firmId)`. Both properties resolve to direct
  `TableAliasColumn`s; **no** intermediates added on either side.
- **D.** `compiledCondition = (firm_src.ID = person_ft_tgt.FIRMID)`,
  `sourceIntermediates = []`, `parentOfTarget = firm_src`.
- **E.** `realJoin1 = Join{op = (firm_src.ID = person_ft_tgt.FIRMID), target = person_ft_tgt,
  aliases = [(firm_src, person_ft_tgt), (person_ft_tgt, firm_src)]}`
- **F.** No target children, no filter/groupBy/distinct → **no materialisation**. `targetJTN1` is
  a bare `JoinTreeNode(person_ft_tgt, realJoin1)`.
- **G.** No source intermediates → `fullChainJTN1 = targetJTN1`.
- **H.** `synthRPM1` with `targetSetImplementationId = person_ft`.

The second pair (`firm_set → person_ct`) follows the same shape with fresh aliases.

#### Hand-off

We now have `synthRPMs = [synthRPM1, synthRPM2]`. `processRelationalPropertyMapping`
sees `size > 1` and hits the multi-set branch:

```
buildSQLQueryOutManySetImplementations(srcSWC, [synthRPM1, synthRPM2], …)
   ├── buildUnion         ── builds a UNION ALL subselect over person_ft / person_ct
   ├── findFkListForEachSet ── extracts the join columns referenced per branch
   └── buildUnionJoin     ── ORs the per-branch ON clauses against the unioned alias
```

#### Final SQL shape

```sql
SELECT ...
FROM   firmTbl AS firm_root
JOIN   ( SELECT FIRMID, FNAME, '__person_ft__' AS _kind FROM personFT
          UNION ALL
         SELECT FIRMID, FNAME, '__person_ct__' AS _kind FROM personCT
       ) AS person_union
   ON   firm_root.ID = person_union.FIRMID
WHERE  ...
```

The two synthetic RPMs collapsed into a single `JOIN` to a `UNION ALL` subselect.
We emitted **one** `ON` clause because both pair conditions referenced equivalent
logical columns; if the lambda body were asymmetric across leaves (e.g. different
right-hand expressions per leaf), `buildUnionJoin` would produce an `OR` of the
per-leaf conditions.

### Decision-points cheat-sheet

| Question | Answer | Where |
|---------|--------|-------|
| Source has nested property access? | Adds child JTNs under `branchSrcSWC` during step C; absorbed into the `fullChainJTN` chain in step G. | step C, G |
| Target has nested property access? | Adds child JTNs under the placeholder; triggers `materializeTargetAsSubselect` (step F) so we expose only the needed columns and the ON clause references the subselect. | step F |
| Target class mapping has `~filter` / `~groupBy` / `~distinct`? | Same: forces materialisation. The materialised inner select calls `processRelationalMappingSpecification` which bakes in those clauses. | step F (`needsMaterialization`) |
| Target is a Union? | Multiple MJPMs (one per leaf). Synthetic RPMs flow into `buildSQLQueryOutManySetImplementations` → `buildUnion` + `buildUnionJoin`. | step 3 of dispatch |
| Source is a Union? | Multiple MJPMs (one per leaf). Same multi-set path; `addMissingColumnIfUnion` exposes FK columns on each source branch. | step 3 of dispatch |
| Source is a `RelationFunction`? | `buildBranchSourceSWC` calls `processRelationFunctionClassMapping`; aliases reference the wrapped `RelationFunctionSelectSQLQuery`. Otherwise identical. | step A |
| Target is a `RelationFunction`? | Same on the target side inside `buildModelJoinPlaceholder`. The wrapper must be preserved (historical bug — see below). | step B |
| Self-association? | Compiler binds both lambda params to the same class via name-based pairing; SQL side is unchanged — branch source and placeholder are two independent aliases of the same table. | n/a |

### Things to watch out for (historical bugs, in code form)

These all live in `compileModelJoinForBranch` / `buildModelJoinPlaceholder` /
`materializeTargetAsSubselect`. Touching any of them invites regression here.

1. **Alias drift across `merge()`.** Inside `processValueSpecification`, `mergeSQLQueryData`
   may rename the placeholder JTN's alias for uniqueness. Step **D** must read the
   *post-compilation* alias from `resultSWC.select.data`, not the *pre-compilation*
   one. Using the stale alias produces SQL with broken column references.

2. **`reprocess=true` on the placeholder application.** When attaching the placeholder
   via `applyJoinInTree`, `reprocess` must be `true` so the placeholder's `aliases`
   stay in sync with the actually applied JTN alias (this matters for RelationFunction
   targets where `createJoinTableAlias` mutates the alias name).

3. **RelationFunction wrapper preservation.** `buildBranchSourceSWC` /
   `buildModelJoinPlaceholder` must keep the `RelationFunctionSelectSQLQuery` wrapper
   visible at the alias. If you accidentally peel it off, downstream property
   resolution falls through to the `SelectSQLQuery` arm and fails with
   "Found 0 relations".

4. **Materialisation column projection.** `materializeTargetAsSubselect` exposes
   exactly the TACs that the ON clause references on the target side, plus rename
   collisions handled by `buildColumnRenameMap`. Do **not** project the whole inner
   tree — the outer ON clause TACs are then rewritten via `reprocessAliases` to point
   at the subselect alias.

5. **Sentinel uniqueness.** The `_mj_src` / `_mj_tgt` strings are protocol-level
   constants set by the Pure compiler (`MODEL_JOIN_SOURCE_VAR` /
   `MODEL_JOIN_TARGET_VAR` in `HelperMappingBuilder`) and consumed by both the
   router (`builder.pure`) and the SQL generator's `varToSelect` map.
   Don't rename one side without the other two.

---

## 7. Where to add new behaviour

| Goal | Touch |
|------|-------|
| New supported expression in the lambda body | Nothing — if `processValueSpecification` already handles it for ordinary queries, ModelJoin gets it for free. |
| New target store kind | Extend the `match` arms in `buildBranchSourceSWC` / `buildModelJoinPlaceholder` so the resulting alias preserves whatever wrapper the SQL generator expects. |
| New target post-processing requiring materialisation | Extend the `targetHasComplexMapping` predicate in step **F**. |
| Non-binary association support | Currently rejected at compiler step 1; adding support requires a new `ModelJoinAssociationImplementation` shape and rethinking the cross-product expansion. |
| Cross-store ModelJoin | Out of scope for the relational generator. The router's "exactly one cluster" assertion (§5 step 4) is the natural place to relax — but downstream the SQL stage assumes one store, so a parallel multi-store plan-generation path would be needed. |
| Custom parameter-pairing rule | `resolveLambdaParamNames` in `HelperMappingBuilder`. |

---

## 8. Quick reference: function call graph

```
PARSE
  CorePureGrammarParser.parseModelJoinAssociationMapping
     └── ModelJoinAssociationMappingParseTreeWalker.visitModelJoinAssociationMapping
            └── DomainParser.parseCombinedExpression  (must yield a LambdaFunction)

COMPILE
  HelperMappingBuilder.processModelJoinAssociationMapping
     ├── resolveOriginalPropertyName            (handles milestoning)
     ├── rewriteModelJoinExpression  × 2        (one per direction)
     │      ├── resolveLambdaParamNames         (type-based, then name-based)
     │      └── rewriteValueSpecification       (substitutes _mj_src / _mj_tgt)
     ├── findAllSetsForClassOrSubtypes  × 2
     └── compileForAllPairs  × 2
            └── PropertyMappingBuilder.visitModelJoinPropertyMapping (per pair)

ROUTE  (core/pure/router/store/builder.pure)
  potentiallyRoutePropertyMapping
     └── m:ModelJoinPropertyMapping[1] arm
            ├── narrow PermutationSets to (sourceSetImplementationId, targetSetImplementationId)
            ├── seed RoutingState with those PermutationSets
            ├── routeFunction(joinCondition, …)
            └── unwrap single ClusteredValueSpecification

SQL  (pureToSQLQuery.pure)
  processPropertyMapping
   └── processModelJoinPropertyMapping            (groups MJPMs by pair, dispatches)
         └── compileModelJoinForBranch            (per pair → synthetic RPM)
               ├── buildBranchSourceSWC           (raw-table SWC for _mj_src)
               ├── buildModelJoinPlaceholder      (raw-table SWC for _mj_tgt + 1=1 join)
               ├── processValueSpecification      (compile lambda body)
               ├── (internal) getDeepestLeafAlias / appendAtDeepestLeaf
               └── materializeTargetAsSubselect   (when target needs wrapping)
         └── processRelationalPropertyMapping     (drives single-pair / union path)
               ├── doJoinToClass                  (single pair)
               └── buildSQLQueryOutManySetImplementations + buildUnionJoin   (many pairs)
```


