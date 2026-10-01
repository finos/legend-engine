# Legend Grammar Parser (Rust)

A Rust implementation of the Legend grammar parser. It turns grammar text
into an AST and the AST into **Protocol v1 JSON** (`PureModelContextData`
elements) that deserializes into the same objects as Legend Engine's
Java/ANTLR parser produces.
The end goal is to serve as Legend Engine's grammar parser through JNI; see
[docs/engine-integration.md](docs/engine-integration.md), and
[docs/porting-status.md](docs/porting-status.md) for the grammars ported so
far and the order of the rest.

The workspace moved here from legend-pure on 2026-10-01 (its `rust-parser`
branch, `legend-pure-rust/` at `172d559fedd`); names keep their legend-pure
prefix, and the crates that belong to legend-pure go back there later (see
"Location" in [docs/engine-integration.md](docs/engine-integration.md)).

## Architecture

```
┌────────────────────────────────────────────────────────┐
│  API: parse_section / capabilities                      │  ← engine contract (JSON)
├────────────────────────────────────────────────────────┤
│  Grammars: dsl-{data,graph,relational,store,sql,tds}   │  ← plug-ins (parser + protocol halves)
├────────────────────────────────────────────────────────┤
│  Protocol                                               │  ← AST → Protocol v1 JSON
├────────────────────────────────────────────────────────┤
│  Parser                                                 │  ← Tokens → AST (recursive descent)
├────────────────────────────────────────────────────────┤
│  Lexer                                                  │  ← Source → Tokens
├────────────────────────────────────────────────────────┤
│  AST (+ ast-derive)                                     │  ← Shared data model
└────────────────────────────────────────────────────────┘
```

## Quick Start

```bash
cargo build --workspace
cargo test --workspace

cargo lint-lib        # strict: no unwrap/expect in library code
cargo lint            # pedantic clippy, curated allowlist in .cargo/config.toml
cargo fmt --check
./scripts/check-copyright.sh

# Coverage (requires cargo-llvm-cov + cargo-nextest), as CI runs it.
# 80% on the way back to 85%.
cargo llvm-cov nextest --workspace --exclude legend-pure-parser-jni --fail-under-lines 80
```

## Crate Map

| Crate | Purpose | Depends on |
|-------|---------|------------|
| `legend-pure-parser-ast` | AST data model (`Element`, `Expression`, `TypeReference`, `SourceInfo`) and plug-in content traits (`DSLElement`, `IslandContent`, `EmbeddedDataContent`) | `ast-derive`, `smol_str`, `serde` |
| `legend-pure-parser-ast-derive` | `Spanned` / `Annotated` / `PackageableElement` derives | `syn`, `quote` |
| `legend-pure-parser-lexer` | Tokenizer | ast |
| `legend-pure-parser-parser` | Recursive-descent parser; `SectionParser` / `IslandParser` / `EmbeddedDataParser` plug-in traits | ast, lexer |
| `legend-pure-parser-protocol` | Protocol v1 model and AST ↔ protocol conversion; `IslandProtocol` / `EmbeddedDataProtocol` / `ElementProtocol` plug-in traits | ast, `serde_json`, `linkme` |
| `legend-pure-parser-api` | Engine-facing contract: parse one section body (with engine line/column offsets) into protocol elements, or a `parser` / `unsupported` / `internal` error; `capabilities()` | parser, protocol |
| `legend-pure-parser-jni` | JNI bridge: `liblegend_pure_rust_parser` (`cdylib` + `rlib` for aggregators) exporting `NativeGrammarParser.capabilities` / `parseSection` | api, dsl-authentication, dsl-connection, dsl-data, dsl-data-space, dsl-deephaven, dsl-elasticsearch, dsl-graph, dsl-mapping, dsl-mongodb, dsl-persistence, dsl-persistence-cloud, dsl-persistence-relational, dsl-relational, dsl-relational-snowflake, dsl-runtime, dsl-service, dsl-service-store, dsl-store, dsl-sql, dsl-tds, `jni` |
| `legend-pure-dsl-connection` | The `###Connection` section: connections, each value read by the grammar its type names, the core's model connections built in (parser + protocol) | ast, lexer, parser, protocol |
| `legend-pure-dsl-data` | The `###Data` section: data elements and their data for other elements (parser + protocol) | ast, lexer, parser, protocol |
| `legend-pure-dsl-mapping` | The `###Mapping` section: mappings, their includes and tests, and the plug-in grammars of mapping element bodies and legacy test inputs (parser + protocol) | ast, lexer, parser, protocol |
| `legend-pure-dsl-graph` | Graph-fetch island `#{ Class { prop } }#` (parser + protocol) | ast, lexer, parser, protocol |
| `legend-pure-dsl-store` | Relation-store-accessor island `#>{ store::Db.table }#` (parser + protocol) | ast, lexer, parser, protocol |
| `legend-pure-dsl-relational` | The relational store: the `###Relational` section (databases), `Relational #{…}#` test data, `Relational` mapping elements and test input, and `RelationalDatabaseConnection` with its specifications (parser + protocol) | ast, lexer, parser, protocol, dsl-mapping, dsl-connection |
| `legend-pure-dsl-mongodb` | The engine's MongoDB extension: `MongoDB` mapping elements and `MongoDBConnection`s (parser + protocol) | ast, lexer, parser, protocol, dsl-mapping, dsl-connection, dsl-authentication |
| `legend-pure-dsl-deephaven` | The engine's Deephaven extension: `DeephavenConnection`s (parser + protocol) | ast, lexer, parser, protocol, dsl-connection, dsl-authentication |
| `legend-pure-dsl-elasticsearch` | The engine's Elasticsearch extension: `Elasticsearch7ClusterConnection`s (parser + protocol) | ast, lexer, parser, protocol, dsl-connection, dsl-authentication |
| `legend-pure-dsl-authentication` | The engine's authentication module: connections' authentication specifications, `# Type { … }#`, and their secrets (parser + protocol) | ast, lexer, parser, protocol |
| `legend-pure-dsl-data-space` | The engine's data space extension: the `###DataSpace` section's data spaces, and `DataspaceTestData` embedded data (parser + protocol) | ast, lexer, parser, protocol |
| `legend-pure-dsl-persistence` | The engine's persistence extension: the `###Persistence` section's persistences and contexts, and the plug-in grammars of triggers, platforms and targets (parser + protocol) | ast, lexer, parser, protocol, dsl-connection |
| `legend-pure-dsl-persistence-relational` | The persistence extension's relational target, `Relational #{ … }#` | dsl-persistence |
| `legend-pure-dsl-persistence-cloud` | The persistence extension's cloud platform, `AwsGlue #{ … }#` | dsl-persistence |
| `legend-pure-dsl-relational-snowflake` | The relational store's Snowflake extension: its datasource specification, authentication strategy and local mode in relational connections | dsl-relational |
| `legend-pure-dsl-runtime` | The `###Runtime` section: runtimes, their mappings and connections, and the runtimes services hold (parser + protocol) | ast, lexer, parser, protocol, dsl-connection |
| `legend-pure-dsl-service` | The engine's service extension: the `###Service` section's services (executions, test suites, legacy tests, post-validations) and execution environments (parser + protocol) | ast, lexer, parser, protocol, dsl-runtime |
| `legend-pure-dsl-service-store` | The engine's service store extension: `ServiceStore` mapping elements, `ServiceStoreConnection`s and `ServiceStore #{…}#` test data (parser + protocol) | ast, lexer, parser, protocol, dsl-mapping, dsl-connection |
| `legend-pure-dsl-sql` | SQL expression island `#SQL{…}#`, its text kept as written (parser + protocol) | ast, parser, protocol |
| `legend-pure-dsl-tds` | TDS literal island `#TDS{…}#`, its CSV body kept as written (parser + protocol) | ast, parser, protocol |

The Java side of the JNI bridge, `NativeGrammarParser`, is in the engine
module
[`legend-engine-language-pure-grammar-rust`](../legend-engine-core/legend-engine-core-base/legend-engine-core-language-pure/legend-engine-language-pure-grammar-rust),
with the extension that hands the engine's sections to the library:

```bash
cargo build -p legend-pure-parser-jni
# from the repository root, with JDK 17:
mvn -pl legend-engine-core/legend-engine-core-base/legend-engine-core-language-pure/legend-engine-language-pure-grammar-rust \
    test -Drevision=rust-parser-SNAPSHOT \
    -Dlegend.pure.rust.parser.lib=$PWD/legend-engine-rust/target/debug/liblegend_pure_rust_parser.dylib
```

## Adding a Grammar Feature

1. Write failing tests first, derived from Legend Engine's grammar tests
   (`TestDomainGrammarRoundtrip`, `TestDomainGrammarParser`, …).
2. `crates/ast/` — add types or variants for the new syntax.
3. `crates/lexer/` — new tokens, if needed.
4. `crates/parser/` — parse into the AST.
5. `crates/protocol/` — AST → Protocol JSON, matching the engine's Java protocol classes.
6. `cargo insta review` to approve changed snapshots.

## Tracing

```bash
RUST_LOG=legend_pure_parser=debug cargo test
```
