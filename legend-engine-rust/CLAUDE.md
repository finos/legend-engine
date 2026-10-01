# CLAUDE.md — `legend-engine-rust/`

Guidance for working in this Cargo workspace: a Rust implementation of the
Legend grammar parser whose output is **Protocol v1 JSON** for Legend Engine,
which calls it through JNI. The scope is deliberately small: parser, protocol,
the section and island grammars, the `api` registry and the `jni` bridge.
There is no compiler, runtime or CLI here; those lived on legend-pure's
archived `legend-pure-rust` branch (source snapshot `beb0c7f1886`).

The workspace lives in legend-engine for now: it moved on 2026-10-01 from
legend-pure's `rust-parser` branch (`legend-pure-rust/` at `172d559fedd`, the
archive), so one repository holds a change from grammar to engine. Names keep
their legend-pure prefix (crates, the Java binding's package, the library).
The crates that belong to legend-pure (the core, its islands, `dsl-data`,
`dsl-relational`, `dsl-mapping`: `docs/engine-integration.md`, "Where
grammars live") go back there once its interpreter work restarts.

Design and roadmap: [docs/engine-integration.md](docs/engine-integration.md).
Which grammars are ported, and the order: [docs/porting-status.md](docs/porting-status.md).

## Crates

| Crate | Layer |
|---|---|
| `ast` (+ `ast-derive`) | Data model; plug-in content traits `DSLElement`, `IslandContent`, `EmbeddedDataContent` |
| `lexer` | Source → tokens |
| `parser` | Tokens → AST; plug-in traits `SectionParser`, `IslandParser`, `EmbeddedDataParser` |
| `protocol` | AST → Protocol v1 JSON; plug-in traits `IslandProtocol`, `EmbeddedDataProtocol`, `ElementProtocol` |
| `api` | Engine contract: `parse_section` (section body + offsets → elements/imports or error), `capabilities` |
| `jni` | `liblegend_pure_rust_parser`: JNI exports for Java `org.finos.legend.pure.rust.parser.NativeGrammarParser` (in the engine module `legend-engine-language-pure-grammar-rust`); the only crate with `unsafe` (`#[unsafe(no_mangle)]`) |
| `dsl-graph`, `dsl-store`, `dsl-sql`, `dsl-tds` | Island grammars `#{…}#`, `#>{…}#`, `#SQL{…}#` and `#TDS{…}#` (parser + protocol halves only) |
| `dsl-relational` | The `###Relational` section (databases; a section without imports, read from Pure tokens with the relational lexer's multi-token keywords rebuilt in `tokens.rs`), `Relational #{…}#` test data, the `Relational` mapping elements and legacy test input (`mapping.rs`, registered into `dsl-mapping`'s grammars), and `RelationalDatabaseConnection` (`connection/`, registered into `dsl-connection`'s grammars; its datasource specifications, authentication strategies, post-processors and query generation configs are plug-ins of their own, `connection/specification.rs`, which also reproduces where the engine's walker positions what it parses again in them); and the `###QueryPostProcessor` section's relational mappers (`mapper.rs`) |
| `dsl-data` | The `###Data` section (a section without imports); its embedded data is read by the parser's core data grammars |
| `dsl-mapping` | The `###Mapping` section: read whole first (its grammar), then walked, as the engine does, so section syntax errors come before errors inside element bodies. Element bodies and legacy test inputs are plug-in grammars (`grammar.rs`), keyed by type; an unlinked type is unsupported |
| `dsl-connection` | The `###Connection` section, read whole first, then each value by the grammar its type names (`grammar.rs`'s `ConnectionValueGrammar`; the core's model connections in `model.rs`, a store's registered by its crate); `parse_embedded_connection` reads a runtime's own `#{ Type { … } }#` |
| `dsl-runtime` | The `###Runtime` section (`Runtime`, `SingleConnectionRuntime`), walked as the engine does: runtimes before single connection ones, a store kept only with connections, its own connections read by `dsl-connection`; `parse_embedded_runtime` reads a service's own `#{ … }#` |
| `dsl-persistence` | The engine's persistence extension: the `###Persistence` section's persistences and contexts, read whole first as blocks of fields (the parser's `blocks`), then walked into records shaped as the engine's protocol (the protocol's `record`); triggers, platforms and targets are plug-in grammars (`grammar.rs`), whose islands the walker parses two characters short |
| `dsl-persistence-relational`, `dsl-persistence-cloud` | The persistence extension's `Relational` target and `AwsGlue` platform, registered into `dsl-persistence`'s grammars |
| `dsl-data-space` | The engine's data space extension: the `###DataSpace` section (without imports), read and walked as `dsl-persistence`'s; `DataspaceTestData`, embedded data a data space's reference |
| `dsl-dataquality` | The engine's data quality extension: the `###DataQualityValidation` section (without imports), read and walked as `dsl-persistence`'s: class validations and their trees, relation validations and comparisons; a persistence strategy is unsupported |
| `dsl-diagram` | The engine's diagram extension: the `###Diagram` section (with imports), read and walked as `dsl-persistence`'s; view ids and signed numbers rebuilt from Pure tokens as the engine's diagram lexer reads them (`tokens.rs`) |
| `dsl-generation` | The engine's generation extension: the `###FileGeneration` and `###GenerationSpecification` sections (with imports), read whole, then walked; a file generation's own fields preferred over properties of their names, as its grammar does |
| `dsl-external-format` | The engine's external format core: the `###ExternalFormat` section's schema sets and bindings (with imports), read and walked as `dsl-persistence`'s, schema sets first; a format no engine module defines is unsupported |
| `dsl-service` | The engine's service extension: the `###Service` section's services and execution environments, read whole first (queries and test values parsed as its grammar does), then walked in the engine walker's order; runtimes of its own read by `dsl-runtime`, test data and assertions by the parser's grammars |
| `dsl-function-activator` | What the engine's function activator grammars share (the engine's `functionActivator` module declares their protocol): an activator's header, `'''doc''' Keyword <<s>> {t} my::A { … }` (`read.rs`), its `function` and `ownership: Deployment { … }` fields, read and walked (`walk.rs`), and `ActivatorElement` with its `ActivatorProtocol` (`element.rs`), which each activator's crate registers for its section; no section of its own |
| `dsl-hosted-service` | The engine's hosted services: the `###HostedService` section's hosted services, read and walked as `dsl-snowflake`'s, with `dsl-function-activator`, then its execution environments, `dsl-service`'s `ExecutionEnvironment`s (`read.rs`, `walk.rs`); test suites, post validations, post-deployment actions and deployment configurations, which no test writes, are unsupported |
| `dsl-memsql-function` | The engine's MemSQL function activators: the `###MemSql` section's functions, read and walked as `dsl-snowflake`'s, with `dsl-function-activator`; a deployment configuration, which the walker makes an element without a name, is unsupported |
| `dsl-snowflake` | The engine's Snowflake function activators: the `###Snowflake` section's apps and model-to-model UDFs, read and walked as `dsl-persistence`'s, apps first, with `dsl-function-activator`; deployment configurations read and left out |
| `dsl-relational-snowflake` | The relational store's Snowflake extension: the `Snowflake` datasource specification, the `SnowflakePublic` authentication strategy (`dsl-relational`'s `FieldGrammar`s) and its local mode |
| `dsl-mongodb` | The engine's MongoDB extension: the `###MongoDB` section (`store/`, its schemas read as the engine's `MongoDBSchemaDeserializer` reads them), `MongoDB` mapping elements (`mapping.rs`) and `MongoDBConnection`s (`connection.rs`; a host is one word of the grammar's lexer, joined from adjacent tokens) |
| `dsl-deephaven` | The engine's Deephaven extension: the `###Deephaven` section (`store/`, read and walked as `dsl-persistence`'s, stores first; an app's function read by the grammar's own `appFunctionIdentifier`, whose multiplicities take no `..`) and `DeephavenConnection`s (`connection.rs`; the walker positions the connection on its body, and Rust leaves URLs it can't judge as `java.net.URI` does to the engine) |
| `dsl-elasticsearch` | The engine's Elasticsearch extension: the `###Elasticsearch` section (`store/`, read and walked as `dsl-persistence`'s, fields named by the lexer's literal names) and `Elasticsearch7ClusterConnection`s (`connection.rs`; the cluster's `# URL { … }#`, checked with `dsl-connection`'s `uri`) |
| `dsl-authentication` | The engine's authentication module: the `# Type { … }#` authentication specifications connections hold (`grammar.rs`, by type; `specifications.rs`) and their secrets and AWS credentials (`secret.rs`), read into the engine's protocol objects (`object.rs`) |
| `dsl-service-store` | The engine's service store extension: the `###ServiceStore` section (`store/`, read and walked as `dsl-persistence`'s, its blocks in parentheses; security schemes unsupported), `ServiceStore` mapping elements (`mapping.rs`), `ServiceStoreConnection`s (`connection.rs`) and `ServiceStore #{…}#` test data (`data.rs`) |

## Build & Test

```bash
cargo test --workspace                 # all tests
cargo test -p legend-pure-parser-parser <name>   # one test
cargo insta review                     # approve snapshot changes
cargo lint-lib && cargo lint           # zero-warning gates
cargo fmt --check
./scripts/check-copyright.sh
cargo doc --workspace --no-deps        # must produce 0 warnings (CI gate)
cargo llvm-cov nextest --workspace --exclude legend-pure-parser-jni --fail-under-lines 80   # CI gate (80; target 85)

# JNI contract and the engine's parity suites (from the engine checkout's root, JDK 17;
# docs/engine-integration.md, "Parity harness"):
(cd legend-engine-rust && cargo build -p legend-pure-parser-jni)
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -pl legend-engine-core/legend-engine-core-base/legend-engine-core-language-pure/legend-engine-language-pure-grammar-rust \
    test -Drevision=rust-parser-SNAPSHOT \
    -Dlegend.pure.rust.parser.lib=$PWD/legend-engine-rust/target/debug/liblegend_pure_rust_parser.dylib
```

## Code Conventions (enforced by CI)

- Edition **2024**. `#![forbid(unsafe_code)]` in every crate except `jni`.
- No `unwrap()` / `expect()` in library code; `cargo lint-lib` is a hard gate.
- `cargo lint` (pedantic) stays at zero warnings. Every `-A` in the alias
  carries a justification; adding one is a policy change, not a paper-over.
- Public items need `///` docs (`#![deny(missing_docs)]`).
- `SmolStr` for identifiers; `thiserror` for error types; `tracing` (never
  `info+` in hot paths).
- Copyright header on `.rs`, `.toml`, `.sh` (Markdown/JSON exempt).
- Snapshot tests use `insta`.
- `ast-derive` derives are hierarchical: use exactly one of `Spanned`,
  `Annotated` or `PackageableElement` per struct.

## Engine contract (what "correct" means here)

- The reference is **Legend Engine's grammar**
  (`legend-engine-language-pure-grammar`: `DomainParserGrammar.g4`,
  `DomainParseTreeWalker`), not legend-pure's M3 grammar. Protocol JSON must
  deserialize into the same Java objects the engine's parser produces.
- Operator chains are **bug-compatible** with the engine
  (`protocol/src/v1/engine_operators.rs`): the AST keeps conventional
  precedence, and conversion flattens each chain to source order and replays
  `DomainParseTreeWalker`'s algorithm (`plus([a,b,c])`, nested `divide`,
  `==` binding to its left operand, `$x > 0 && $x < 30` →
  `lessThan(and(…), 30)`). Don't "fix" these shapes on the Rust side; an
  engine precedence fix would land upstream first.
- Constraints are `{$this: [1] | body}` lambdas; unnamed ones are named by
  their index. `###Pure` is always `ImportAwareCodeSection`; a section
  grammar without imports (`SectionParser::imports` false, e.g. `###Data`)
  is a `DefaultCodeSection`.
- Positions: 1-based, inclusive end column, offsets applied like the
  engine's `ParseTreeWalkerSourceInformation`.
- Never emit a placeholder for something that can't be converted: return
  `ConvertError::Unsupported` at its position so the engine falls back to
  its own parser.
- Grammar plug-ins self-register through linkme slices (`ISLAND_PARSERS`,
  `SECTION_PARSERS`, `ISLAND_PROTOCOLS`, `ELEMENT_PROTOCOLS`); see
  `docs/engine-integration.md`.
- Not targets: ANTLR error-message wording, and byte-identical JSON.

## Correctness policy: no tactical test-pass hacks

- **Fix at the source of truth.** If the engine emits `X`, produce `X` where
  the data is born (lexer, parser, conversion), not by post-processing output.
- **No pattern-matching on test-specific shapes**, error-message strings or
  classifier names.
- **Don't transform intermediate values to satisfy an assertion.** Ask why
  the pipeline produces `Y` instead of `X`.
- **If you must defer, defer cleanly:** `#[ignore = "needs <structural fix>"]`
  with a one-line reason. First prove the full fix really is out of reach.

## Adding a grammar feature

1. Write failing tests first, derived from the engine's
   `TestDomainGrammarRoundtrip` / `TestDomainGrammarParser`.
2. `ast` types, then `lexer` tokens if needed, then `parser`, then `protocol`
   conversion.
3. `cargo insta review`.
