# legend-pure-parser-jni

JNI bridge for the Rust grammar parser. Builds `liblegend_pure_rust_parser`
(`cdylib`, plus an `rlib` for downstream aggregators), the native half of
`org.finos.legend.pure.rust.parser.NativeGrammarParser` in the engine module
`legend-engine-language-pure-grammar-rust`.

## Native entry points

Each returns JSON as UTF-8 bytes, which Jackson reads as they are: one copy
into a Java array, where a Java string would be converted twice.

| Java (`static native`) | Returns |
|---|---|
| `byte[] capabilities()` | `{"sections":[…],"islands":[…]}` — the grammars linked into this library |
| `byte[] parseSection(String sectionType, String code, String sourceId, int lineOffset, int columnOffset)` | `{"elements":[…],"imports":[…]}` (`imports` absent for a grammar without them) or `{"error":{"kind","message","sourceInformation"?}}` |
| `byte[] parseExpression(String kind, String code, String sourceId, int lineOffset, int columnOffset)` | `{"value":…}` (`kind`: `lambda`, `valueSpecification` or `graphFetch`) or the same error JSON |
| `byte[] parseModel(String code, String sourceId, int lineOffset, int columnOffset)` | `{"elements":[…]}` (a whole model, as `PureGrammarParser.parseModel` parses it: every section's elements, then the section index) or the same error JSON |

`kind` is `parser` (syntax error), `unsupported` (valid input this build
cannot convert — fall back to the Java parser) or `internal`. The contract
itself is `legend_pure_parser_api`; see `docs/engine-integration.md`.
Positions in this JSON are strings: `sourceId:line:column-column` on one
line, `sourceId:line:column-line:column` otherwise.

Every failure — argument errors and panics included — comes back as error
JSON; nothing unwinds across the FFI boundary. `null` is returned only if
the JVM cannot allocate the result array.

## Grammars and aggregators

The library links the island grammars `dsl-graph` (`#{…}#`), `dsl-store`
(`#>{…}#`), `dsl-sql` (`#SQL{…}#`) and `dsl-tds` (`#TDS{…}#`); the section
grammars `dsl-connection` (`###Connection`), `dsl-data` (`###Data`),
`dsl-elasticsearch` (`###Elasticsearch`), `dsl-mapping` (`###Mapping`),
`dsl-mongodb` (`###MongoDB`), `dsl-relational` (`###Relational` and
`###QueryPostProcessor`), `dsl-runtime` (`###Runtime`), `dsl-service`
(`###Service`) and `dsl-service-store` (`###ServiceStore`); the stores'
mapping, connection and test data grammars in `dsl-relational`,
`dsl-relational-snowflake`, `dsl-service-store`, `dsl-mongodb` and
`dsl-elasticsearch`; and the connections' authentication grammar
`dsl-authentication`. They self-register. To add grammars (e.g. Legend
Engine's own), build an aggregator `cdylib` that depends on this crate and the
grammar crates, and force-links them:
>>>>>>> 508c681b75b (feat(rust-parser): the ###Service grammar)
=======
`dsl-elasticsearch` (`###Elasticsearch`), `dsl-mapping` (`###Mapping`),
`dsl-mongodb` (`###MongoDB`), `dsl-relational` (`###Relational` and
`###QueryPostProcessor`), `dsl-runtime` (`###Runtime`), `dsl-service`
(`###Service`) and `dsl-service-store` (`###ServiceStore`); the stores'
mapping, connection and test data grammars in `dsl-relational`,
`dsl-relational-snowflake`, `dsl-service-store`, `dsl-mongodb` and
`dsl-elasticsearch`; and the connections' authentication grammar
`dsl-authentication`. They self-register. To add grammars (e.g. Legend
Engine's own), build an aggregator `cdylib` that depends on this crate and the
grammar crates, and force-links them:
>>>>>>> Stashed changes

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

The engine's `legend-engine-language-pure-grammar-rust` build runs the release
build and puts the library in its jar, under `META-INF/native/<platform>/`, where
`NativeGrammarParser` loads it from (through a temporary copy).
`-Dlegend.pure.rust.parser.lib=<absolute path>` loads another instead, and
`java.library.path` (`System.loadLibrary("legend_pure_rust_parser")`) is the last
resort. This crate is not published to crates.io: the jar is how it ships.
