---
name: review-protocol-impact
description: "Protocol JSON lens of a legend-engine impact review: compares the PureModelContextData JSON the engine produces from the same EMIT corpus grammar on base vs head, cross-reads each side's JSON with the other engine (backward = stored SDLC JSON into the new engine, forward = new JSON into an older engine), diffs the _type subtype registry, and applies static checks for Jackson annotations, subtype registration, Pure vX_X_X protocol and grammar/composer symmetry. Use for changes to protocol POJOs, PureProtocolExtension, grammar parsers or composers, the compiler, Pure protocol models, or when asked whether a change breaks protocol or JSON compatibility."
---

# Protocol impact lens

Inputs: a finished `engine-review run` directory (it includes the forward pass unless `--no-forward`
was used), plus the diff. Background reading: `references/protocol-compat.md`, which covers the
mappers, strict and lenient classes, the `_type` sources, and where a change must land.

## Dynamic evidence (report.md → "Protocol JSON")

1. **Compatibility failures, which are blockers until explained.**
   - *Backward deserialize or compile failed.* JSON already stored in SDLC no longer loads in the
     new engine. This blocks the change, unless the element type never shipped.
   - *Forward deserialize failed.* An engine older than head rejects JSON that head-era Studio or
     SDLC will write. Usually this is a new field on a strict class, or a new `_type`. Not always a
     blocker, but it needs a rollout order (engines first) stated in the PR. Say which class is
     strict.
   - *Head parse or compile failed where base passed.* This is a grammar or compiler regression, with
     a corpus model as the reproducer.
2. **`_type` registry changes.**
   - `removed` / `retargeted`: stored JSON with that `_type` breaks. This is a blocker unless an
     alias is kept registered.
   - `added`: forward-incompatible for older engines.
   - `collision`: two classes now answer to one name, and which one wins is undefined. Fix it.
3. **JSON shape changes, where the same grammar produces different JSON.** Each pattern reads as
   `<added|removed|changed> <element _type>.<json path>`.
   - `added` on a strict class: forward risk (see 1).
   - `removed` or `changed`: did the author add a read path for the old shape, and is there a
     `TestCompatibilityAndMigration`-style test for it?
   - A pattern that touches element types the PR does not mention is transitive impact: say so.
4. **Backward drift.** Base JSON read by head differs from head's own parse of the same grammar.
   Stored models will silently mean something else from now on. Common causes are a new field
   whose default differs from what the parser sets, or a converter rewriting old shapes.

## Static checks (always run them, since EMIT only sees element types the corpus uses)

Run the greps in `references/protocol-compat.md` § "Static checks". For every protocol-class hunk,
answer four questions:

- Strict or lenient class?
- A read path for JSON already stored?
- Pure `vX_X_X` models, transfers and `_type` literals updated to match?
- Composer updated with the parser, plus a round-trip test?

Note that grammar round-trip tests compare text, so they pass a consistent rename.

For generated protocols (`*.protocol.json`), a Pure class rename **is** a `_type` rename.

## Coverage

The corpus covers the element types used by the EMIT models on the selected suites. If the diff
touches an element type that no snapshot contains (check `pmcd.json` files for its `_type`), report
it as a coverage gap. Either add an EMIT model that uses the element (see `docs/emit/emit-authoring.md`
for placement) or run `--suites crossfeature`, which needs nearly the whole reactor built.
