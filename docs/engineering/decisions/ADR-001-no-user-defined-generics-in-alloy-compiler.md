# ADR-001: No User-Defined Generic Types in the Alloy Compiler Protocol

**Status:** Accepted  
**Date:** _Historic_ 
**Deciders:** Legend Engine core team

---

## Context

The Alloy compiler (`legend-engine`) translates a `PureModelContextData` snapshot — a bag of
versioned JSON protocol POJOs — into a live `PureModel` graph at request time. It must complete
in quickly because it runs on every query or model-load request that is not already
cached.

The underlying `legend-pure` compiler supports the full Pure language, including
**parameterised (generic) class and function declarations**, e.g.:

```pure
// Valid in legend-pure; ships pre-compiled in the legend-pure JARs
Class meta::pure::functions::collection::Pair<U, V>
{
    first  : U[1];
    second : V[1];
}
```

Supporting the same feature in the Alloy compiler would require implementing the complete
parametric polymorphism unification algorithm that `legend-pure` uses — a non-trivial piece of
work with a significant per-request performance cost and a large surface area for edge cases
(contravariance, multiplicity parameters, mutual recursion).

At the same time, **users never need to declare generic types through Legend Studio or the
`###Pure` grammar**. User domain models consist of concrete classes (`Person`, `Trade`,
`Account`) whose properties may be *typed with* generic instantiations from the platform library
(e.g. `List<Person>`, `Pair<String, Integer>`) but which do not declare new type parameters
themselves. This is an intrinsic property of the domain-modelling use case: user classes model
real-world entities, not container abstractions.

---

## Decision

The Alloy compiler protocol **does not include a `typeParameters` field** on the `Class` or
`Function` protocol POJOs, and the compiler does not attempt to handle user-defined generic
class or function declarations.

Concretely:

- The `Class` protocol POJO has no `typeParameters` field; the compiler will reject any
  protocol payload that attempts to declare one.
- The `Function` protocol POJO likewise has no `typeParameters` field.
- *Using* generic types from the Pure standard library remains fully supported:
  `Property.genericType` carries `typeArguments` for concrete instantiations such as
  `List<Person>`, and all generic collection functions (`filter`, `map`, `fold`, `first`, …)
  are callable from user expressions via the normal function-resolution path.
- Parameterised container types (`Pair<U,V>`, `List<T>`, `Map<K,V>`, …) are part of the Pure
  standard library, compiled once by `legend-pure`, and available at runtime — they do not need
  to be re-declared by users.

---

## Consequences

### Positive

- The Alloy compiler stays simpler and fast: no full unification algorithm is needed at request time.
- The protocol is simpler and its JSON schema is stable — there are no generic parameters to
  version or validate.
- Type errors in user models remain easy to diagnose: all types are concrete, so there are no
  polymorphism edge cases to reason about.
- The implementation scope of `CompilerExtension` / `Processor<T>` contributors is reduced —
  they never need to handle `TypeVariable` nodes in user-submitted class definitions.

### Negative / Limitations

- Users cannot create reusable generic domain classes in Legend Studio. If a pattern is
  genuinely polymorphic (e.g. a generic `Result<T>` wrapper), it must either be added to the
  `legend-pure` standard library (compiled at build time) or modelled as a class with
  a `Map<String, Any>` property as an escape hatch.
- Recursive type definitions that would require mutual type-parameter unification cannot be
  expressed through the Alloy protocol. Complex polymorphic patterns must live in `legend-pure`.

### Related constraints (same decision class)

The same reasoning applies to two closely related restrictions in the Alloy protocol:

- **`native` functions** cannot be declared by users through the grammar — native function
  registration is a build-time concern handled exclusively by `legend-pure`'s `Handlers`
  registry.
- **`@` meta-programming annotations and compile-time evaluation** are not available in the
  Alloy grammar, for the same reason: they require full compiler infrastructure that is
  only available in `legend-pure`.

---

## References

- `docs/engineering/architecture/alloy-compiler.md §1.2` — Key Deliberate Constraints
- `docs/engineering/architecture/alloy-compiler.md §1.3` — Relationship to `legend-pure`
- `legend-engine-language-pure-compiler` module — `toPureGraph/PureModel.java`
- `legend-pure` [Compiler Pipeline](https://github.com/finos/legend-pure/blob/main/docs/architecture/compiler-pipeline.md) §4 — Type Resolution and Generics

