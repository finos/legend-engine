# Routing `EmbeddedRelationFunctionSetImplementation` in router unions

## Context

Relation function class mappings (`RelationFunctionInstanceSetImplementation`, "rfsi")
let a `~func` of return type `Relation<Any>` back a class mapping. When such a
mapping has embedded property mappings (e.g. `firm (legalName: firmName)`), the
compiler produces an `EmbeddedRelationFunctionSetImplementation` ("erf") that
inherits both from `EmbeddedSetImplementation` and from
`RelationFunctionInstanceSetImplementation`. The erf inherits the parent rfsi's
`relationFunction` so it can be clustered against the same upstream relation as
its owner.

When the same class is mapped twice and joined with a router `*Person : Operation
{ special_union(rfSet1, rfSet2) }`, accessing an embedded property (`p.firm`) on
the union must route through *both* erfs.

## The failing test

```
meta::relational::tests::mapping::union::relation::testUnionTwoRelationMappings_EmbeddedFirmFilter
```

It runs

```pure
|Person.all()->filter(p|$p.firm.legalName == 'Firm X')->project(~[name:p|$p.lastName])
```

against `unionOfTwoRelationMappingsWithEmbeddedFirm`, where each member of the
union is a relation mapping with an embedded `firm` block.

Before the fix the test failed at plan generation with:

> Relation function for set '...' in mapping '...' may not have been routed
> correctly. Found a SimpleFunctionExpression instead of a
> StoreClusteredValueSpecification.

The assertion lives at `cluster.pure:530`, inside
`storeContractForSetImplementation`'s `t: RelationFunctionInstanceSetImplementation`
arm.

## Temporary workaround (now removed)

Earlier work patched the symptom inside `cluster.pure`'s
`storeContractForSetImplementation`:

```pure
t: RelationFunctionInstanceSetImplementation[1]|
   let routedT = $t->meta::pure::router::store::routing::potentiallyRouteRelationFunctionSet($mapping, $extensions)
                   ->cast(@RelationFunctionInstanceSetImplementation);
   let vs = $routedT.relationFunction.expressionSequence->evaluateAndDeactivate();
   ...
```

This made the assertion hold by re-routing inside the clustering pass. It is the
wrong layer: routing decisions should be settled at routing time, and the
assertion should be a true invariant of `storeContractForSetImplementation`.

## Root cause

End-to-end for the failing query:

1. `processClass(Person)` routes the `*Person` `special_union`. Each parameter's
   `setImplementation` is replaced (via `classMappingById($i.id)`) with the
   freshly routed `rfSet1`/`rfSet2`. Both leaves are
   `RelationFunctionInstanceSetImplementation`s — their `relationFunction`s are
   routed via the `t:` arm of `potentiallyRouteRelationFunctionSet`
   (`routing.pure:117`).
2. `filter(p|$p.firm.legalName == ...)` enters
   `routeFunctionExpressionProperty` for `firm`. With the source being a union,
   `findMappingsFromProperty` (`mappingExtension.pure:198`) builds a synthetic
   `embedded_operation` `OperationSetImplementation` whose
   `parameters[i].setImplementation` is the erf from each member of the union.
3. `routeFunctionExpressionProperty` calls
   `potentiallyRouteRelationFunctionSets` on the synthetic operation. The new
   `erf:` arm in the singular `potentiallyRouteRelationFunctionSet` rebuilds
   each erf with a routed `relationFunction`, and the `o:` arm has an explicit
   `embedded_operation` branch that uses `$i.setImplementation` (the erf
   container already carries) rather than looking it up via `classMappingById`
   (which cannot resolve embedded ids like `rfSet1_firm`).
4. Clustering then dispatches on the resulting
   `StoreMappingRoutedValueSpecification`. The inner cluster of `$p.firm` is a
   `SimpleFunctionExpression`, so the `Any[1]` branch of `cluster.pure:403`
   falls through to `getClusterVSFromSets($r.sets, ...)`.
5. `getClusterVSFromSets` calls `storeContractForSetImplementation` on the
   `embedded_operation` `OperationSetImplementation`. The `o:` arm calls
   `resolveInstanceSetImplementations` (`mappingExtension.pure:175`):

   ```pure
   $sets->map(s | $s->match([
       o:OperationSetImplementation[1] | $o->resolveInstanceSetImplementations(),
       e:EmbeddedSetImplementation[1]  | $e->resolveRootFromEmbedded(),  // <-- here
       i:InstanceSetImplementation[1]  | $i
   ]));
   ```

   The erf matches the `e:` arm first (it predates the `i:` arm in this match
   block), so `resolveRootFromEmbedded` walks `$o.owner` up to the original
   *un-routed* `rfSet1`/`rfSet2` from the source mapping. The routed erf the
   routing pass produced — with its routed `relationFunction` — is discarded.
6. `storeContractForSetImplementation` then hits the `t:` arm on the un-routed
   owner and trips the assertion.

The bug is not in clustering: routing already produced everything needed. It is
that `resolveInstanceSetImplementations` "resolves" an erf by walking to its
owner, even though an erf is itself a fully-formed
`RelationFunctionInstanceSetImplementation` with its own routed
`relationFunction`.

## Fix

Three coordinated changes, none in the clustering pass:

### 1. `mappingExtension.pure` — keep erfs as their own roots

In `resolveInstanceSetImplementations` add an arm for
`EmbeddedRelationFunctionSetImplementation` *before* the existing
`EmbeddedSetImplementation` arm so the erf is returned as-is:

```pure
$sets->map(s | $s->match([
    o:OperationSetImplementation[1] | $o->resolveInstanceSetImplementations(),
    erf:meta::pure::mapping::relation::EmbeddedRelationFunctionSetImplementation[1] | $erf,
    e:EmbeddedSetImplementation[1]  | $e->resolveRootFromEmbedded(),
    i:InstanceSetImplementation[1]  | $i
]));
```

Why this is safe:

- erf extends `RelationFunctionInstanceSetImplementation`, which extends
  `InstanceSetImplementation`. The return type `InstanceSetImplementation[*]`
  is honoured.
- Of the other two callers,
  `meta::pure::extension::_storeContractForSetImplementation`
  (`functions.pure:54`) only needs a representative root to look up a
  `StoreContract` — erf and its owner share the same store, so the lookup is
  identical.
- The graphFetch caller (`graphExtension.pure:472`) asserts every operation
  parameter is a `PureInstanceSetImplementation`, so erfs cannot reach it.

### 2. `routing.pure` — route erfs (and the synthetic union) during routing

The singular `potentiallyRouteRelationFunctionSet` already has:

- An `erf:EmbeddedRelationFunctionSetImplementation` arm placed before the
  plain `e:EmbeddedSetImplementation` arm, so the erf's own `relationFunction`
  is routed (not the owner's).
- An explicit `embedded_operation` branch in the `o:OperationSetImplementation`
  arm: the synthetic union's parameter ids are scoped under their owner
  (e.g. `rfSet1_firm`) and cannot be resolved via `classMappingById`. The
  `setImplementation` the container already carries is routed instead.

These changes are what make step 5 above hand `storeContractForSetImplementation`
an erf whose own `relationFunction` is already a
`StoreClusteredValueSpecification`. The plural
`potentiallyRouteRelationFunctionSets` carries a matching `erf:` arm under
`nonRelFuncMappings` so the same is true when the erf arrives as a leaf rather
than as a parameter of `embedded_operation`.

### 3. `cluster.pure` — restore the assertion as an invariant

Drop the defensive `routedT` re-route. The `t:` arm reverts to:

```pure
t: RelationFunctionInstanceSetImplementation[1]|
   let vs = $t.relationFunction.expressionSequence->evaluateAndDeactivate();
   assert($vs->size() == 1, ...);
   assert($vs->at(0)->instanceOf(StoreClusteredValueSpecification), ...);
   ...
```

After changes 1 and 2 the routing pass guarantees that every erf that reaches
clustering has its `relationFunction` routed, and that
`resolveInstanceSetImplementations` returns that erf rather than its
un-routed owner — so the assertion becomes a real invariant rather than a
trap.

## Files touched

- `legend-engine-core/.../mapping/mappingExtension.pure`
  — `resolveInstanceSetImplementations`: add `erf:` arm.
- `legend-engine-core/.../router/store/routing.pure`
  — `potentiallyRouteRelationFunctionSet` (singular): `erf:` arm before
    `e:`/`t:`; `embedded_operation` branch in the `o:` arm.
  — `potentiallyRouteRelationFunctionSets` (plural): `erf:` arm under
    `nonRelFuncMappings`.
- `legend-engine-core/.../router/store/cluster.pure`
  — `storeContractForSetImplementation`: defensive re-route removed; assertion
    stands.

## Why not push the workaround into clustering?

The `routedT` re-route inside `storeContractForSetImplementation` would have
worked, but it ran the routing pipeline a second time from clustering, on a
single set at a time, with no caching and no guarantee of consistency with the
strategy that produced the rest of the routed graph. Routing decisions belong
in the routing pass; clustering should treat them as fixed.

## Validation

The following Pure tests cover this path (all `<<test.Test, test.AlloyOnly>>`
in `meta::relational::tests::mapping::union::relation`):

- `testUnionTwoRelationMappings_EmbeddedFirmProject`
- `testUnionTwoRelationMappings_EmbeddedFirmFilter`  ← the originally failing case
- `testUnionTwoRelationMappings_EnumFilter`
- the rest of `testRelationUnion.pure`

Run the relational PCT module (H2-backed) and the Pure test suite that
contains the above to validate.
