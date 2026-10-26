# Protocol JSON compatibility: mechanics and checks

Protocol JSON is a contract. SDLC stores every entity as JSON, and Studio and older or newer engines
exchange it. The Java POJOs are **not versioned**: a single package tree
(`org.finos.legend.engine.protocol.pure.{v1,m3,dsl}`) has to read JSON written by every client
version. Versioning exists only on the Pure side (`core/pure/protocol/vX_X_X` plus frozen
`v1_*_0` copies, with `PureClientVersions.production`).

## The three mappers, and why strictness differs by caller

| mapper | where | unknown fields | converters |
|---|---|---|---|
| `ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports()` | library code, tests, `SDLCLoader`, `PlanGenerator`, EMIT snapshots | **fail**, except classes annotated `@JsonIgnoreProperties(ignoreUnknown = true)` | yes |
| HTTP server mapper (`Server.java`, Dropwizard base) | REST API | ignored | **no** `ProtocolConverter`s |
| `PureProtocolObjectMapperFactory.getNewObjectMapper()` | bare; `TestCompatibilityAndMigration` adds `NON_EMPTY` | fail | no |

The standard mapper sorts properties alphabetically, sorts map keys, and drops nulls but keeps
empty lists. `_type` is always written first.

Lenient classes include `PureModelContextData`, the whole `ValueSpecification` hierarchy, `DataSpace`
and a few execution-plan nodes. **Class, Mapping, Function and most other elements are strict**, so a
new field on them breaks every reader older than the change that loads with the standard mapper.

## How `_type` is decided

1. Static `@JsonSubTypes` on abstract bases (`ValueSpecification`, `PackageableElement`,
   `PureModelContext`, `ExecutionNode`, relational `DataType`, `PropertyMapping`, ...).
2. `PureProtocolExtension.getExtraProtocolSubTypeInfoCollectors()` → `ProtocolSubTypeInfo.newBuilder(...).withSubtype(X.class, "name")`.
   The extensions are discovered through `META-INF/services/...PureProtocolExtension`.
3. `ClassInstance.type` uses a second discriminator, resolved through
   `getClassInstanceTypeMappings` / `getExtraClassInstanceTypeMappings`.
4. Defaults let JSON with no `_type` deserialize: `defaultImpl`, and `withDefaultSubType` (for
   example `Runtime` → `LegacyRuntime`).
5. **Generated protocols** (`*.protocol.json` → `GenerateMetaClasses`). `_type` defaults to the Pure
   class name with a lower-case first letter, unless a `<<JSON.name>>` tag is set. **Renaming the
   Pure class silently renames `_type`.** The Java for these protocols is generated, so it never
   appears in the diff.

Registration throws only when the same *class* is registered twice. Two classes with the same
`_type` name under one base are **not** detected. The snapshot's `_subtypes.json` lists them as
collisions.

## What a compatible change looks like

- **Adding a field:** fine for new readers. Older strict readers fail on JSON that carries it, which
  is the forward direction. Check whether the class is lenient. If it is strict, the change needs a
  rollout note: engines must be upgraded before Studio and SDLC start emitting the field.
- **Renaming or removing a field:** old JSON must still load. The tools for that are a custom
  `JsonDeserializer` (`Function`, `Property`, `Variable`, `CString`, ...), an `@JsonCreator` with
  `@Deprecated` parameters (`PureModelContextData`, `Unit`), `@JsonAlias`, `Access.WRITE_ONLY`
  legacy setters (`MappingIncludeMapping`), or a `ProtocolConverter`. A `ProtocolConverter` is not
  applied by the HTTP server mapper.
- **Changing what a field means** (units, defaults, nullability): the JSON shape is identical but
  stored models change behaviour. The cross-read drift report is the only automated signal.
- **Renaming or removing a `_type`:** blocker unless the old name stays registered.

## Where a protocol change has to land (all of them, in one PR)

1. The Java POJO, plus a compatibility path for old JSON.
2. Subtype registration and `getExtraProtocolToClassifierPathMap`. `TestExtensions` in
   `legend-engine-extensions-collection-generation` enforces the classifier map.
3. The grammar parser **and** composer, with a round-trip test.
4. The compiler (`toPureGraph`, `CompilerExtension`).
5. The Pure protocol models and `transfers` in `vX_X_X`, including the hard-coded `_type = '...'`
   literals. There are hundreds of them; grep both the old and new name.
6. For generated protocols, the Pure source named in `*.protocol.json`.
7. Outside this repo: Studio's TypeScript protocol.

## What the existing tests do and do not catch

- `TestGrammarRoundtrip.test(...)` (64 suites) goes text → JSON → text and **compares text only**.
  A field rename applied consistently to POJO, parser and composer passes all of them.
- `TestCompatibilityAndMigration` pins old JSON → new JSON with JsonUnit, but it runs on the bare
  mapper with `NON_EMPTY`, so do not build golden files from it.
- No test compares the Java POJOs or subtype registry with the Pure `vX_X_X` classes. Drift between
  them shows up only where Java `readValue`s Pure output: plan generation and
  `TestRoundTripWithPureTransformation`.

## Static checks for a diff (EMIT does not do these for you)

```bash
git diff BASE HEAD -- '**/protocol/**' '**/*-protocol*/**' | grep -nE '^[-+].*@Json(SubTypes|TypeInfo|Property|Ignore|IgnoreProperties|Creator|Alias|Include|Deserialize|Serialize)'
git diff BASE HEAD | grep -nE '^[-+].*withSubtype\(|^[-+].*_type\s*=\s*'"'"
git diff BASE HEAD --stat -- '**/*.protocol.json' '**/protocols/pure/**' '**/core/pure/protocol/**'
git diff BASE HEAD | grep -nE '^-\s+public [A-Za-z<>\[\], ]+ [a-zA-Z_]+;'   # removed public POJO fields
```

For each hit, ask:

- Is the class strict or lenient?
- Is there a read path for JSON already stored?
- Did the Pure `vX_X_X` side change too?
- Did the composer change with the parser?
