# legend-pure-parser-jni

JNI bridge for the Rust grammar parser. Builds `liblegend_pure_rust_parser`
(`cdylib`, plus an `rlib` for downstream aggregators), the native half of
`org.finos.legend.pure.rust.parser.NativeGrammarParser` in the engine module
`legend-engine-language-pure-grammar-rust`.

## Native entry points

| Java (`static native`) | Returns |
|---|---|
| `String capabilities()` | `{"sections":[…],"islands":[…]}` — the grammars linked into this library |
| `String parseSection(String sectionType, String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo)` | `{"elements":[…],"imports":[…]}` (`imports` absent for a grammar without them) or `{"error":{"kind","message","sourceInformation"?}}` |
| `String parseExpression(String kind, String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo)` | `{"value":…}` (`kind`: `lambda`, `valueSpecification` or `graphFetch`) or the same error JSON |

`kind` is `parser` (syntax error), `unsupported` (valid input this build
cannot convert — fall back to the Java parser) or `internal`. The contract
itself is `legend_pure_parser_api`; see `docs/engine-integration.md`.

Every failure — argument errors and panics included — comes back as error
JSON; nothing unwinds across the FFI boundary. `null` is returned only if
the JVM cannot allocate the result string.

## Grammars and aggregators

The library links the island grammars `dsl-graph` (`#{…}#`), `dsl-store`
(`#>{…}#`), `dsl-sql` (`#SQL{…}#`) and `dsl-tds` (`#TDS{…}#`); the section
grammars `dsl-connection` (`###Connection`), `dsl-data` (`###Data`),
`dsl-data-space` (`###DataSpace`), `dsl-dataquality`
(`###DataQualityValidation`), `dsl-diagram` (`###Diagram`),
`dsl-external-format` (`###ExternalFormat`),
`dsl-mapping` (`###Mapping`),
`dsl-persistence` (`###Persistence`, with its extensions'
`dsl-persistence-relational` and `dsl-persistence-cloud`), `dsl-relational`
(`###Relational`), `dsl-runtime` (`###Runtime`), `dsl-service`
(`###Service`), `dsl-service-store` (`###ServiceStore`) and `dsl-snowflake`
(`###Snowflake`); the stores'
mapping, connection and test data grammars in `dsl-relational`,
`dsl-relational-snowflake`, `dsl-service-store`, `dsl-mongodb`,
`dsl-deephaven` and `dsl-elasticsearch`; and the connections' authentication
grammar `dsl-authentication`. They self-register. To add grammars (e.g.
Legend Engine's own), build an aggregator `cdylib` that depends on this
crate and the grammar crates, and force-links them:

```rust
use legend_pure_rust_parser as _;
use my_engine_grammar as _;
```

The `Java_*` symbols of this rlib are re-exported by the aggregator without
forwarders (verified on macOS arm64); the Java class does not change.

## Building

```bash
cargo build -p legend-pure-parser-jni             # target/debug/liblegend_pure_rust_parser.{dylib,so}
cargo build -p legend-pure-parser-jni --release
```

Java loads it with `-Dlegend.pure.rust.parser.lib=<absolute path>` or from
`java.library.path` (`System.loadLibrary("legend_pure_rust_parser")`).
