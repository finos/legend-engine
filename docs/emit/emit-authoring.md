# Writing EMIT Tests — Authoring Guide

A companion to [`emit.md`](emit.md), which describes the design and architecture
of the EMIT framework. This document is for developers (human or AI) who want
to add EMIT tests — either to cover a new feature they have built, or to close
a gap that has been discovered in an existing area.

It assumes you already understand the Legend feature you are exercising. If
you need the conceptual model of what EMIT is, what it runs, and why,
read `emit.md` first.

---

## 1. When to Use EMIT

EMIT is the right harness when you want to verify that a model — written in
the same Legend grammar a Studio user would write — survives the full engine
pipeline: parse → compile → model generation → file/artifact generation →
test execution → plan generation. Use it when:

- You are adding a feature whose value is **end-to-end behavior**: a new
  mapping flavor, a new generation extension, a new store type, a new
  external-format binding, etc.
- You are adding a feature that **interacts** with other features and you
  want a regression that exercises the combination — for instance "service
  with multi-execution backed by a relational mapping and an external-format
  binding". Unit tests in each module won't catch the interaction; an EMIT
  model will.
- You have discovered a **testing gap** — a Studio model that exposed a bug,
  a feature combination that has no example, or an interaction whose failure
  mode you would like to lock in.

EMIT is **not** the right harness for:

- Per-function correctness across stores — that is PCT (`docs/pct/`).
- Mapping-feature parity against store adaptors — that is MFT.
- Pure-level unit tests on a single grammar production — write a parser or
  compiler unit test in the owning module.

If your test needs are split — for example, a feature has a per-function
component and an end-to-end component — write both: a PCT/unit test for the
function, an EMIT test for the model-level surface.

---

## 2. Descriptor and Source Root

Every EMIT test is two things on disk:

1. A `*.emit.yaml` **descriptor** that names the test and points at its sources.
2. A **sibling source-root directory** containing one or more `.pure` files
   that make up the model.

```
<classpath-root>/                      ← e.g. emit-models/, or relational-emit-models/ (see §3)
  relational-simple.emit.yaml          ← descriptor
  relational-simple/                   ← source root (same stem)
    store/db.pure
    mapping/personMapping.pure
    data/testData.pure
```

The descriptor's `modelSources.model.root` is interpreted **relative to the
descriptor's own directory**, and `files` are interpreted relative to that
root. By convention the root has the same stem as the YAML file, which keeps
adjacent tests visually grouped in directory listings.

Discovery (`EMITModelDiscovery`) scans the classpath for `*.emit.yaml`, so
the descriptor's filename is what makes the test exist. A `.pure` file with
no descriptor pointing at it is dead weight.

---

## 3. Where to Place Your Test

EMIT tests live in `src/test/resources/emit-models/` inside a module whose
**test classpath already contains every dependency the test needs**. This is
the single most important decision when adding a test. The right module makes
your test "just work"; the wrong module gives you a `ClassNotFoundException`
or a missing-`StoreContract` failure at run time.

Apply the rule from `emit.md` §3.2:

> Pick the most specific per-feature `-emit` module whose classpath covers
> **all** features in the model. If no per-feature module covers the
> combination, fall back to the cross-feature module.

### 3.1 Per-feature modules (single feature area)

| Your model uses… | Place it in… |
|---|---|
| A fixture whose purpose is to exercise the EMIT runner itself (e.g. a deliberate compile failure, a dependency-clash case) | `legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/` (bootstrap examples for the framework, several using fake test-only SPIs — not catalog content) |
| Core Pure language constructs — constraint, measure, profile, qualified property, inheritance, function, association traversal | `legend-engine-core/legend-engine-core-emit-tests`, under `src/test/resources/grammar-emit-models/`, run by `GrammarEMITTests` |
| M2M mapping (no service, no store) | `legend-engine-core/legend-engine-core-emit-tests`, under `src/test/resources/m2m-emit-models/`, run by `M2MEMITTests` |
| Relational store / mapping / connection (including embedded service tests) | `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit`, under `src/test/resources/relational-emit-models/`, run by `RelationalEMITTests` |
| Relation (`~func`) function-based class mapping — `ClassName: Relation { ~func f():Relation<Any>[1]; ... }` | `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit`, under `src/test/resources/relation-emit-models/`, run by `RelationEMITTests` |
| Service shapes — multi-execution, test data shared between services, and the deprecated `test: Single` block on a Service (Phase 5's legacy service test runner) | `legend-engine-xts-service/legend-engine-xt-service-emit`, under `src/test/resources/emit-models/`, run by `ServiceEMITTests` |
| A Service over an M2M mapping | `legend-engine-xts-service/legend-engine-xt-service-emit`, as above. `legend-engine-core-emit-tests` has no service test runner, and giving it one would make a core module depend on `legend-engine-xts-service` |
| File / model generation | `legend-engine-xts-generation/legend-engine-xt-generation-emit` |
| External format / binding | The format's `-emit` module under `legend-engine-core-external-format` or its `xts-*` peer (e.g. `legend-engine-xts-json/legend-engine-external-format-jsonSchema-emit`) |
| Flat-data store | `legend-engine-xts-flatdata/legend-engine-xt-flatdata-emit` |
| Persistence (service-output target, ingestion tests) | `legend-engine-xts-persistence/legend-engine-xt-persistence-emit` |

If the module you want doesn't exist yet, create it — see §9.

#### Legacy `MappingTests` place by mapping kind, not by runner

A deprecated `MappingTests [ ... ]` block on a Mapping (Phase 5's legacy
`MappingTestRunner`) involves **no Service**, so it does not belong with the
service fixtures even though it is the sibling of the legacy *service* test
runner. Place it with the mappings it exercises, exactly as you would a modern
`testSuites` block:

| The mapping under test is… | Place it in… |
|---|---|
| Relational | `relational-emit-models/` — `relational-legacy-mapping-test` |
| M2M | `m2m-emit-models/` in `legend-engine-core-emit-tests` — `m2m-legacy-mapping-test` |
| Spanning several feature areas | `legend-engine-emit-tests` (cross-feature, §3.2) |

The runner being deprecated is a property of the *test style*, not a feature
area; grouping by it would scatter mappings away from their own suites.

The split also matters for coverage, not just tidiness: `MappingTestRunner`
resolves `<Object, JSON, …>` input data into a `JsonModelConnection` over a
base64 `data:` URL, while store-backed input data goes through
`ConnectionFactoryExtension` and, for relational, becomes H2 setup SQL. One
fixture proves nothing about the other branch, so a new store flavour wanting
legacy-mapping-test coverage needs its own fixture in its own suite.

### 3.2 Cross-feature module (multi-area combinations)

When a model legitimately spans more than one feature area — a service
backed by a relational mapping with a JSON Schema binding, a multi-execution
service driving a function activator, a generation-spec producing a relational
mapping — drop the test in:

```
legend-engine-config/legend-engine-emit-tests
```

This module carries a test-scoped dependency on
`legend-engine-extensions-collection-generation`, so it pulls in the full
protocol/grammar/compiler/generation surface and can run any combination.
Reserve it for tests that genuinely need the cross-cutting classpath; do not
use it as a default landing pad, otherwise per-feature modules grow stale.

### 3.3 Quick decision

1. List the feature tags your model exercises (use the taxonomy in `emit.md` §6.2).
2. Find the most specific `-emit` module whose `pom.xml` already declares
   every needed feature as a test-scoped dependency.
3. If one exists, place the test there. If not, place it in
   `legend-engine-emit-tests`.

When in doubt, run `mvn -pl <candidate-module> test -Dtest=<their suite>`
after dropping your descriptor in — if it loads and runs your test as a
DynamicTest, the placement is correct.

---

## 4. Authoring Workflow

A practical step-by-step for a new test:

### Step 1 — Choose a name and a placement module

Pick a short, hyphenated name that reads as a noun phrase describing what
the test demonstrates: `relational-joins`, `service-with-binding`,
`m2m-derived-property`. The name becomes the YAML filename stem, the source
root directory name, and the descriptor's `name` field.

### Step 2 — Lay out the source root

Under `src/test/resources/emit-models/`, create the source root directory.
EMIT imposes no directory structure — the descriptor's `files` list names
each `.pure` file by its path under the root, so you can organize the source
root however reads best for the test. One common layout groups files by kind:

```
emit-models/
  my-feature-name/
    model/
    mapping/
    store/         ← only if the test has a store
    service/       ← only if the test has a service
    data/          ← test data elements
    generation/    ← generation specs
```

But a flat root, or any other grouping, is equally valid. Use whatever keeps
the fixture easy to read.

Every `.pure` file must start with the Apache 2.0 copyright header
(Checkstyle blocks the build otherwise — see `CLAUDE.md`).

### Step 3 — Write the `.pure` files

Use the Legend grammar exactly as Studio would produce it. Anchor packages
under a short, generic prefix (`demo::`, `test::`) rather than the
organization-style namespaces production projects carry.

Each grammar section starts with its header: `###Pure`, `###Mapping`,
`###Relational`, `###Service`, `###Connection`, `###Runtime`, `###Data`,
`###FileGeneration`, `###GenerationSpecification`, etc. A single file can
contain as many elements and sections as you like — combining several into one
`.pure` file is common and often clearest, especially when the elements are
small or closely related. Split across multiple files only when it genuinely
aids readability.

If you are testing behavior that requires test data, embed it as a `Data`
element (`###Data`) or inline `ExternalFormat` blocks in your test
asserts. Look at an existing example in the target module for the exact
syntax — see for instance `m2m-passing/mapping/personMapping.pure` for a
mapping with an embedded test suite.

### Step 4 — Write the `*.emit.yaml` descriptor

Place the descriptor at the same level as the source root directory, with
the same stem. Minimum viable shape:

```yaml
name: my-feature-name
title: "Short human title"
description: |
  One or two sentences explaining what the test demonstrates and what,
  specifically, you expect EMIT to verify.

modelSources:
  model:
    root: my-feature-name
    files:
      - model/types.pure
      - mapping/mapping.pure

features:
  - mapping:mapping
  - scaffolding:class
complexity: basic
tags:
  - <your-tag>
```

The fields are documented in `emit.md` §6.1. Notes from experience:

- **`title` and `description`** are catalog metadata, not test commentary —
  write them for someone browsing the catalog looking for an example, not
  for someone reading the test fixture. The description should describe the
  *model* and, above all, what the test is *intended to verify* — the
  feature, combination, or behavior it exists to exercise. Most models run
  through most of the pipeline, so listing the phases adds little; say what
  the model proves, not which phases happen to fire.
- **`features`** must use `domain:capability` pairs from the controlled
  taxonomy in `emit.md` §6.2 (e.g., `grammar:association`,
  `mapping:relational-embedded`, `store:relational-filter`). Sort
  alphabetically. If you need a new capability, add it to the taxonomy
  in the same PR.
- **`stores`** is the empty list `[]` for store-less models. Omitting it is
  not the same — be explicit.
- **`complexity`** is determined by domain-crossing depth: count the
  distinct non-scaffolding domains in the feature list. 1–2 domains → `basic`,
  3–4 domains → `intermediate`, 5+ domains → `advanced`.
- **`tags`** is free-form. Use it for the things you would type into a
  hypothetical search box — `h2`, `multi-execution`, `derived-property`,
  `failure-mode`. For connection-dialect tags (`h2`, `duckdb`, `snowflake`, ...), tag
  every dialect the model's declared connections actually drive Plan Generation with,
  plus every engine (`h2`/`duckdb`) its test suites actually execute on — these can
  differ for the same fixture; see §11.4.

### Step 5 — Run it

EMIT tests show up as JUnit dynamic tests under the module's
`*EMITTests` class. Run them with:

```bash
mvn test -pl <your-module-path> -Dtest=<ModuleName>EMITTests
```

With the default `testContainers(...)` wiring (see §8), the IDE shows a tree
per model: a `my-feature-name` container holding a `Load Model Descriptor`
task, an `Initialization` group (`Parsing`, `Compilation`, `Model
Generation`), and — as applicable — `Test` (one entry per atomic test, e.g.
`demo::MyMapping / suite_1 / test_1`, with each suite's entries framed by
`… / Open Test Suite` and `… / Close Test Suite` tasks covering the suite's setup
and teardown), `File/Artifact Generation`, and `Service Plan Generation`
groups. Each leaf is a real JUnit test that performs its phase's work when it
runs, so passes, failures, and durations are reported per phase. If something
fails, the failure message includes the per-phase summary from
`EMITResult.getSummary()`.

If the module does not yet have a `*EMITTests` class, see §9.

---

## 5. Reusing Models via `dependencies`

Most non-trivial models pull in shared types so that the test focuses on the
feature under exercise rather than re-defining `Person`, `Firm`, etc. EMIT
supports two dependency-specification styles. They are equivalent in
behavior; pick by ergonomics.

### 5.1 Reference another `.emit.yaml` (preferred for shared models)

```yaml
modelSources:
  model:
    root: relational-simple
    files:
      - store/db.pure
      - mapping/personMapping.pure
  dependencies:
    - source: relational-shared-domain.emit.yaml
```

The referenced descriptor's primary model becomes a dependency of your test.
This is the cleanest way to reuse a curated bundle — a single change to the
shared YAML automatically propagates.

Add `excludes` (supporting `*` / `**` globs) to drop files you don't want
pulled in:

```yaml
  dependencies:
    - source: core-api.emit.yaml
      excludes:
        - core-api/**/*_experimental.pure
```

### 5.2 Inline root + files (for one-off ad-hoc deps)

```yaml
  dependencies:
    - root: m2m-dep-source
      files:
        - model/depModel.pure
        - mapping/depMapping.pure
```

Use this when there is no reusable shared model — e.g. you specifically want
a one-off dependency for testing a corner case like out-of-scope test
ignoring (see `m2m-with-dep.emit.yaml`).

### 5.3 What dependencies do and don't do

- Dependency files are **parsed and compiled** alongside the primary model
  so that cross-references resolve.
- They are **not in scope** for generation, test execution, or plan
  generation. EMIT will refuse to run tests defined on a dependency
  element — see `m2m-with-dep.emit.yaml` for the regression that locks
  this in.
- If two reachable paths supply the same file (diamond dependency), the
  loader notices and loads it once. If two reachable paths supply
  **different** files at the same virtual path, INITIALIZATION fails with
  a clash. See the `diamond/` fixtures.

### 5.4 Shared models in other modules

A test's dependency can live in a different module — the descriptor is
located by classpath, so as long as the target module declares a test-scoped
dependency on the module owning the shared YAML, the reference resolves.
Be conservative with this: cross-module shared deps add classpath coupling.
Prefer to keep a shared model in the same module as its consumers; promote
it to a more central module only when several modules want it.

---

## 6. Writing Tests for Specific Phases

EMIT is a pipeline. Your `.pure` model shapes which phases run and which
get skipped. Use the table below to design a model that exercises the
phase(s) you care about.

| Phase | Triggered by | Skip condition |
|---|---|---|
| INITIALIZATION | Always runs | Never skipped |
| PARSE | Always runs | Never skipped |
| COMPILE | Always runs | Never skipped |
| MODEL_GENERATION | Presence of a `GenerationSpecification` | No `GenerationSpecification` |
| FILE_GENERATION (4a) | `GenerationSpecification.fileGenerations` non-empty | No spec or empty list |
| FILE_GENERATION (4b) | An `ArtifactGenerationExtension` accepts an element | No applicable extension |
| TEST_EXECUTION | Any `Testable` element or legacy mapping/service test in the primary model | No test elements |
| PLAN_GENERATION | Any `Service` element | No services |

Some practical patterns:

- **Cover a new mapping flavor**: write a class, a store (if applicable), a
  mapping, and a mapping `testSuites` block with at least one passing
  assertion. This exercises parse → compile → test execution.
- **Cover a new file-generation extension**: add a `FileGenerationSpecification`
  referenced from a `GenerationSpecification`. The bootstrap
  `file-generation.emit.yaml` uses a fake SPI under
  `src/test/resources/META-INF/services/` so the test can assert exact
  generated content; do the same for a real extension by relying on the
  registered SPI on the module classpath.
- **Cover a service shape**: write a `Service` element. Plan generation
  runs automatically for it. Add `testSuites` if you want test execution
  to run too.
- **Cover an artifact-generation extension**: add an element that your
  extension's `canGenerate` matches. No spec is needed — Phase 4b iterates
  every registered extension over every element.

---

## 7. Negative Tests (Failure Modes)

Negative tests in EMIT are uncommon. Most expected or desired failures (such
as expected parser or compilation errors) should be tested in unit tests for
the individual features. Generally, EMIT should only be used for negative
tests where the expected failure arises in the interaction between
multiple features.

The EMIT JUnit integration (`EMITTestSuiteBuilder`) does not currently support
negative tests. In the case where you find you need a negative EMIT test, you
can drive `EMITRunner` directly from a JUnit class. The framework's own
self-tests (`TestEMITRunner`) provide examples of how to do this.

When you author a negative test, set `tags: [failure-mode]` (or a more
specific tag) so the catalog distinguishes intentional-failure fixtures
from genuine model examples.

---

## 8. The `*EMITTests` JUnit Class

Each `-emit` module has one or more JUnit classes that wire the discovery
into JUnit 5. They are uniformly tiny:

```java
public class MyModuleEMITTests
{
    @TestFactory
    Stream<DynamicContainer> emit()
    {
        return EMITTestSuiteBuilder.testContainers("emit-models/");
    }
}
```

Be conscious of Surefire's default include patterns (`**/Test*.java`,
`**/*Test.java`, `**/*Tests.java`, and `**/*TestCase.java`) when naming the
class. Otherwise, you may find that the tests are not being run during builds.

The argument is the classpath root under which `*.emit.yaml` files are
discovered — `"emit-models/"` is the convention. Discovery will find
everything in the root and all its sub-directories, so adding a new
descriptor is purely a matter of dropping the YAML in.

If your target module hosts more than one subject area, don't share a single root — give each
subject its own disambiguated root name and its own `*EMITTests` class, so each area can be run
on its own and a failure is attributable to one subject rather than the whole module.
`legend-engine-xt-relationalStore-emit` does this: classic relational-mapping models live under
`relational-emit-models/` (`RelationalEMITTests`), and relation (`~func`) mapping models live
under `relation-emit-models/` (`RelationEMITTests`). `legend-engine-core-emit-tests` does the same
with `grammar-emit-models/` (`GrammarEMITTests`) and `m2m-emit-models/` (`M2MEMITTests`).

Split by **subject**, not by size: the test is whether someone debugging one area would want to
run it without the other. Note the HTML coverage-report caveat in
`emit.md` §5.4 — a disambiguated root needs an explicit `includedRelativeSubpaths` entry in the
consuming pom to show up in that report; plain `emit-models/` roots don't need this. Add the entry
in the same PR that introduces the root.

`testContainers(...)` returns one `DynamicContainer` per model, named after
the model, with its tasks grouped by phase: a `Load Model Descriptor` task,
an `Initialization` container (`Parsing`, `Compilation`, `Model Generation` —
each phase runs inside its task, so its reported duration is the phase's real
cost), then `File/Artifact Generation`, `Test` (suite entries framed by
`Open Test Suite` / `Close Test Suite` tasks), and `Service Plan Generation`
containers as applicable. IDEs and Surefire render this as a nested tree,
which reads far better than a flat list once a module has more than a handful
of models. **This is the recommended entry point.**

If you would rather have a flat stream of individual tests with no grouping,
use `EMITTestSuiteBuilder.tests("emit-models/")` (returns
`Stream<DynamicTest>`); the flattened tasks carry the model name as a
`[model-name] …` prefix. (`taskStream(...)` / `taskList(...)` are the former
names for the flat form and are now **deprecated** — prefer `tests(...)` or
`testContainers(...)`.)

To run only a subset of models — to split a large module across several suite
classes, or to control ordering — pass the model name(s). A model is named by
the location of its `*.emit.yaml` relative to the root, **without** the
`.emit.yaml` suffix (e.g. `"basic/class-simple"`):

```java
public class RelationalJoinsEMITTests
{
    @TestFactory
    Stream<DynamicContainer> emit()
    {
        // just these two models, in the order given
        return EMITTestSuiteBuilder.testContainers("emit-models/", "relational/simple", "relational/joins");
    }
}
```

Every selector exists on both entry points and accepts a single name,
varargs, an `Iterable`, or a `Stream`: `testContainers(root, names…)` for the
grouped form and `tests(root, names…)` for the flat form. `testContainer(root,
name)` (singular) returns a single `DynamicContainer`, or `null` when no such
model exists; the multi-name `tests(...)` / `testContainers(...)` selectors
throw `IllegalArgumentException` for an unknown name.

An EMIT test module can be as simple as a single suite file with all the YAML
in one directory tree, or it can have multiple suite files — each targeting a
different sub-tree (a distinct root) or an explicit set of named models.

---

## 9. Standing Up a New `-emit` Module

If your test needs a feature combination that no existing `-emit` module
can host on its classpath, you have two choices: place the test in
`legend-engine-emit-tests`, or stand up a new `-emit` module for the
feature area. Prefer a new module when the feature area has more than one
test in its future; prefer the cross-feature module for one-off
combinations.

To create a new `-emit` module:

1. Add the module as a sibling of its parent feature module (e.g.
   `legend-engine-xts-foo/legend-engine-xt-foo-emit`). Register it in the
   parent `pom.xml`.
2. The `pom.xml` should mirror the relational pattern in
   `legend-engine-xt-relationalStore-emit/pom.xml`:
    - A `test`-scoped dependency on `legend-engine-emit-junit`.
    - `junit-jupiter-api` and `junit-jupiter-engine` at `test` scope.
    - Test-scoped dependencies on the feature's grammar, protocol,
      compiler, Pure runtime, and (where relevant) execution / Java
      platform binding artifacts.
    - Any test runner SPI the model needs — e.g.
      `legend-engine-test-runner-mapping` for models with mapping test
      suites.
3. Add one or more `*EMITTests` JUnit classes as shown in §8.
4. Add `src/test/resources/emit-models/` and start dropping descriptors.

The new module's only job is to host EMIT fixtures with the right
classpath; do not put production code in it.

---

## 10. Closing Coverage Gaps

When you discover a gap — a bug found in the field, an interaction with no
example, a feature combination the catalog is missing — the gap-closing
workflow is the same as the feature-coverage workflow, with one extra step:
write the descriptor's `description` to explicitly call out what was
previously uncovered.

A description like:

> "A relational mapping whose source table has a quoted-identifier column
> name — regression for the issue where `ColumnMapping.column.name` was
> unquoted on the SQL emission path."

is far more useful to a future maintainer than:

> "A relational mapping test."

Tie the gap to the taxonomy: if no existing feature tag describes the gap,
extend the taxonomy in `emit.md` §6.2 in the same PR and tag the new
descriptor with it. This is how the catalog converges on full coverage over
time.

### 10.1 Data that tells the wrong answers apart

A passing assertion proves nothing if the data cannot tell the right answer
from the plausible wrong ones. For each query shape, write down how it could
go wrong (a filter branch dropped, two joins merged into one, a join
duplicated, the null side of an outer join lost), and add rows so that each of
those wrong answers returns different rows. `relational-exists-to-many` is an
example. Its first draft asserted `exists(A) && exists(B)` and passed on
master. The captured SQL then showed master drops the second `exists()`: every
firm in the data that matched A also matched B. One extra firm, matching A
but not B, made the bug visible.

### 10.2 Shapes that are wrong today: probe services

Do not assert a wrong result, and do not add a failing test. If master
returns wrong rows for a shape you want covered, add it as a **probe**: a
`Service` with no `testSuites`. Plan generation still runs on it (and
`EMITSnapshot` captures its SQL, see `emit.md` §5.5), but nothing asserts on
its rows. Say in the service's `documentation` why it is a probe, and name
the `ToFix` test or issue that tracks the bug. Move it into a test suite in
the PR that fixes the bug.

---

## 11. Classification and Deduplication Guidelines

When harvesting EMIT tests from Studio projects (or adding models to close
coverage gaps), follow these rules to avoid redundant models and to keep
the feature metadata accurate.

### 11.1 Classification

Every `features` entry must be a `domain:capability` pair from `emit.md`
§6.2. Classify by examining the `.pure` source for concrete engine
constructs — not by category labels the source project may carry.

**Complexity** is derived mechanically: count the distinct non-scaffolding
domains in the feature list (scaffolding = any `scaffolding:*` entry).

| Distinct domains | Complexity |
|---|---|
| 1–2 | `basic` |
| 3–4 | `intermediate` |
| 5+ | `advanced` |

### 11.2 Deduplication

Before adding a model, extract its sorted `features` set and compare it
against **all** existing `*.emit.yaml` files in the Engine repository.

**The only reason to skip a candidate is an exact feature-set match** —
when another model (existing or newly selected) has the identical sorted
set of `domain:capability` features. Subsets and supersets are **not**
duplicates:

- `{A}` isolates feature A through the pipeline.
- `{A, B}` tests the A+B interaction — may catch regressions that
  neither `{A}` nor `{B}` alone would catch.
- `{A, B, C, D}` tests the full multi-feature composition.

All three provide distinct regression coverage and should all be kept.

### 11.3 Don't Tag a Capability the Fixture Cannot Execute

Some capabilities are only meaningful at run time — a mapping transform, a local
mapping property, a merge operation. If you cannot make the model *execute* the
capability, **do not add a fixture that merely parses and compiles it** and tag the
capability in `features`. Every feature tag is a claim of coverage: it lands in the
HTML coverage dashboard (`emit.md` §5.4) and in any gap analysis computed from
descriptor metadata. A tag backed only by successful compilation reports a capability
as exercised while proving nothing about its behaviour, and a false green is worse
than an honest gap — it stops anyone from looking again.

Leave the capability untagged and uncovered instead, and say why: in the descriptor of
whatever related fixture you *are* adding, or in the commit that decides not to add
one. An honest gap is discoverable; a wrong tag is not.

A worked example. Model-to-model mapping test suites can supply exactly **one** source
connection: `ModelStoreTestConnectionFactory.buildModelStoreConnectionsForStore`
returns inside its loop on the first entry, and
`buildCloseableConnectionFromExternalFormat` installs a single `StreamProviderHolder`
thread-local stream. So a `ModelStore: ModelStore #{ ClassA: …, ClassB: … }#` block
silently uses only `ClassA`, and any query whose plan crosses two set implementations
yields an `InMemoryCrossStoreGraphFetchExecutionNode` whose second store read fails
with `RuntimeException: Input stream was not provided`. That behaviour is long-standing
and intentionally left alone — other code depends on its particulars. It means
`mapping:m2m-local-property` and the two `mapping:operation-mapping-merge*`
capabilities cannot be proven by any M2M fixture, since each needs two populated
sources. All three have real, verified grammar; authoring them is easy and would be
dishonest. They wait for a cross-store fixture where a second store supplies the other
side.

This is not an argument against parse-and-compile fixtures generally. A fixture whose
subject *is* the language construct — everything under `grammar-emit-models/` — proves
exactly what it claims, because there is nothing to execute. The rule is about matching
the claim to what the fixture actually demonstrates.

### 11.4 Declare Connections to Match What Each EMIT Phase Actually Uses

A relational connection does two unrelated jobs in an EMIT model, and a fixture's
`type:`/`tags:` must account for both separately.

**Plan Generation (Phase 6) uses the connection exactly as declared.**
`EMITTasks` calls `ServicePlanGenerator.generateServiceExecutionPlan` directly on the
compiled `Service`, with no connection substitution — whatever `type:` the model
declares (`H2`, `DuckDB`, `Snowflake`, `Postgres`, `Databricks`, `BigQuery`, ...) is the
dialect the generated `ExecutionPlan`'s SQL is actually rendered in. This is how you
prove a feature generates correct dialect-specific SQL for a given store: declare that
store's real type and assert on the plan's SQL text.

**Test Execution (Phase 5) ignores the declared type and picks purely off the query's
return type.** `ServiceTestRunner`/`MappingTestRunner` check
`TestReturnTypeHelper.isRelationReturnType` on the compiled query function. If it's a
subtype of `meta::pure::metamodel::relation::Relation` — covering both
`->project(~[...])` over a mapped class and a raw relation/table accessor chained with
`->from(runtime)` — `RelationalConnectionFactory.buildRelationalTestConnection` rebuilds
the connection from scratch as DuckDB; otherwise it rebuilds it as H2. This happens for
*any* declared type, including cloud dialects — there is no "build a real Snowflake test
connection" path, because EMIT cannot execute against a live warehouse. If the model has
no test suite/test data for that store at all, this swap never runs, and Plan Generation
is the only phase that touches the connection. See the comment in
`TestExecutionContextHelper` ("Relation-returning tests execute against DuckDB rather
than H2").

**So there are two different fixture shapes, and each needs its own tagging rule:**

- **A fixture that only exercises Plan Generation** (no test data for that store) can
  freely declare any real dialect — `Snowflake`, `Postgres`, `Databricks`, `BigQuery`,
  etc. — to prove that dialect's SQL rendering. Tag it with that dialect alone; Phase 5
  never runs, so there is nothing else to tag. `legend-engine-xts-snowflake`'s
  `snowflakeAppTestModels.pure` shows the proven, credential-free grammar for a
  compile-only Snowflake connection:
  ```
  type: Snowflake;
  specification: Snowflake
  {
    name: 'test';
    account: 'account';
    warehouse: 'warehouse';
    region: 'region';
  };
  auth: DefaultH2;
  ```
- **A fixture whose only store-backed queries are H2-vs-DuckDB — no cloud dialect
  claim at stake — should declare the type that Phase 5 will actually force,** so the
  declaration isn't silently contradicted by the swap:
  ```
  ###Connection
  RelationalDatabaseConnection demo::relation::binary::connection::BinaryConnection
  {
    store: demo::relation::binary::store::BinaryDB;
    type: DuckDB;
    specification: DuckDB
    {
      path: '';
    };
    auth: Test;
  }
  ```
  A real example: `relational-semistructured-model-chain.emit.yaml`'s only service
  returns a `Relation` via `project(~[...])`, so its connection is declared `type:
  DuckDB` (it used to be declared `H2`, named `FirmH2Connection`, while already correctly
  tagged `duckdb` — the declaration was the thing that was wrong, not the tag; fixed
  alongside this guidance).
- **A fixture that wants both** — asserting a cloud dialect's Plan Generation SQL *and*
  running real Phase 5 assertions against the same query — should declare the cloud
  dialect (for Plan Generation) and tag **both** that dialect and whichever of
  `h2`/`duckdb` Phase 5 actually exercises for that query's return type. These are two
  independently true, independently verified claims about the same fixture, not a
  contradiction: the plan's SQL text is genuinely `Snowflake`-dialect, and the Phase 5
  result-value assertions genuinely only ever ran against DuckDB or H2 — never against
  real Snowflake. Say so in the fixture's `description` so a reviewer doesn't mistake a
  passing Phase 5 assertion for validated Snowflake behavior.
- **A single connection shared by multiple services with *different* return-type
  outcomes cannot be declared to match all of them** — the swap is computed per query,
  not per connection, so one connection backing both a TDS-returning service (forced to
  H2) and a `Relation`-returning one (forced to DuckDB) will legitimately run on both
  engines no matter what it declares. Tag every engine actually exercised.
  `relation-service-dates.emit.yaml` is this case: one `H2`-declared connection backs
  both kinds of service, so it's correctly tagged both `h2` and `duckdb` — there's no
  single declared type that would make that fixture need only one tag.

A declared-vs-actual mismatch also breaks any Phase 5 expected value whose
representation is dialect-specific. DuckDB's `typeConversion.pure` maps both `Binary`
and `Varbinary` straight to `BLOB` — a variable-length type with no fixed-length
padding — while H2's `BINARY(n)` is fixed-length and zero-pads short values. A fixture
backing a relation-returning query over a `BINARY(16)` column must derive its expected
hex from DuckDB's unpadded `BLOB`, not from H2's zero-padded form, because that is what
Phase 5 will actually execute against regardless of the declared type.

**A new dialect's `###Connection` grammar needs its own grammar module on the EMIT
module's test classpath**, separately from the execution/pure modules already needed
for Phase 5's swap. Parsing `specification: DuckDB { ... }` is handled by a pluggable
`PureGrammarParserExtension` (`DuckDBParserGrammar`, shipped in
`legend-engine-xt-relationalStore-duckdb-grammar`) — it is not part of the core
relational connection grammar. `legend-engine-xt-relationalStore-emit`'s `pom.xml`
needed this dependency added before any fixture here could declare `type: DuckDB`
literally; declaring `type: Snowflake` (or any other dialect) in a fixture under a given
`-emit` module needs that dialect's own `*-grammar` module added to that module's
`pom.xml` the same way, if it isn't already a transitive dependency.

Relevant code: `TestReturnTypeHelper.isRelationReturnType`
(`legend-engine-core-testable`), the `ServiceTestRunner.ServiceTestSuiteSession` /
`MappingTestRunner` hint computation (`legend-engine-test-runner-service` /
`legend-engine-test-runner-mapping`),
`RelationalConnectionFactory.buildRelationalTestConnection` /
`buildDuckDBTestConnection` / `buildH2TestConnection`
(`legend-engine-xt-relationalStore-executionPlan`), `EMITTasks`'s Plan Generation phase
calling `ServicePlanGenerator.generateServiceExecutionPlan` directly on the declared
connection (`legend-engine-core-emit`), and the comment in `TestExecutionContextHelper`.
The `###Connection` grammar for `type: DuckDB` is exercised in
`TestDuckDBConnectionGrammarRoundtrip`.
