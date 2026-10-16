# Engine Integration & Extensibility

How this parser plugs into Legend Engine, and how Engine adds its own Rust
grammars. Status: the engine's core grammars are linked, and checked against
the Java parser by live parity; the others join one change at a time (see
`porting-status.md`).

**Location.** This workspace lives in legend-engine, as `legend-engine-rust/`,
for now: it moved on 2026-10-01 from legend-pure's `rust-parser` branch
(`legend-pure-rust/` at 172d559fedd), together with the Java binding, so the
engine needs no legend-pure snapshot and one repository holds a change from
grammar to engine. Crates, the binding's package and the library keep their
legend-pure names. What belongs to legend-pure (see "Where grammars live")
goes back there once its interpreter work restarts.

## Engine hook

Engine core (`legend-engine-language-pure-grammar`) has three hooks a
ServiceLoader `PureGrammarParserExtension` uses to replace its parsers:
`SectionParserOverride` replaces a section type's parser, whoever defines the
type, `ExpressionParserOverride` replaces the parser of standalone
expressions, and `ModelParserOverride` replaces the parser of whole models
(`PureGrammarParser.parseModel`). Each override receives the parser it
replaces.

The section parser receives `SectionSourceCode`:

- `code` is the section **body**, without its `###Pure` header. The Rust
  parser treats headerless input as a `Pure` section with imports.
- `walkerSourceInformation` carries `sourceId`, `lineOffset` and
  `columnOffset`. `lineOffset` is signed and is often `-1`: headerless code gets
  `\n###Pure\n` prepended, the header sits on line 1, and
  `lineOffset = headerLine - 2`.
- Positions are 1-based with an inclusive end column (ANTLR semantics).
  `columnOffset` applies to the first line only. In the JSON a position is
  one string, `sourceId:line:column-column` when it starts and ends on one
  line, `sourceId:line:column-line:column` otherwise (see "Positions" below).
  `returnSourceInfo = false` means no `sourceInformation` anywhere: the Rust
  parser always emits positions, and the engine's binding drops every
  `SourceInformation` when it reads the JSON for a caller that asked for none.

The section parser must return the section class Java's parser returns (an
`ImportAwareCodeSection` for a grammar with imports, always for `###Pure`;
a `DefaultCodeSection` for one without, such as `###Data`), feed each
element to the consumer, and signal errors as
`EngineException(message, SourceInformation, PARSER)`.

**Whole models:** the engine's `parseModel` puts `\n###Pure\n` in front of
the code and splits it into sections by text alone (`CodeLexerGrammar`): a
section starts at a line break followed by `###` and a name
(`[A-Za-z0-9_][A-Za-z0-9_$]*`), wherever that is, inside a string too; any
other `###` stays in its section's text, where no grammar reads it. Each
section is then parsed by its type's section parser as above (`lineOffset`
the header's line, counted with the default header, less two; no column
offset, so code on a header's line is positioned from the end of the
header), and the model gets a section index, `__internal__::SectionIndex`:
each section's type, class, element paths and imports, both listed once,
and span. The span is the engine walker's over the prefixed code: from the
line break that starts the header (on the line before the header's, the
model's column offset applying to line 1 only) to the section's last
character (the header itself for an empty section), which ends on the line
it starts on, its end column its start column plus its UTF-16 length. The
Rust parser reproduces all of it (`api::parse_model`), so a model can be
parsed with no ANTLR parsing in Java (see "Engine Java side").

**Standalone expressions:** `PureGrammarParser.parseLambda`,
`parseValueSpecification` and `parseGraphFetch` (Studio's query text goes
through the first) don't parse sections; they go through
`parseExpression` instead. The expressions embedded in Mapping, Service and
other sections are parsed with those sections.

## Extensibility model

The design mirrors Java's `PureGrammarParserExtension` and ServiceLoader:

- **Grammar crates register themselves.**
- **Whoever links the native library chooses the grammar set.**
- **The JNI surface never changes when a grammar is added.**

### Rust extension points

| Engine Java hook | Rust counterpart |
|---|---|
| `getExtraSectionParsers` | `parser::SectionParser`. Its `ParserContext` exposes the host parser, so embedded lambdas reuse the Pure expression grammar, and embedded data (`parse_embedded_data`) the data grammars; `protocol::convert_embedded_data` converts that data. |
| `PureProtocolExtension` element subtypes | `protocol::ElementProtocol` (DSL element → its JSON object, written once as text: `Box<RawValue>`). The api's `*_json` functions, which JNI calls, write it as it is; `protocol::convert_element` reads it back into the untagged `PackageableElement::Extension(Value)` for callers that want values |
| `getExtraEmbeddedPureParsers` (islands) | `parser::IslandParser` + `protocol::IslandProtocol`, keyed by the same tag |
| `getExtraEmbeddedDataParsers` (test data) | `parser::EmbeddedDataParser` + `protocol::EmbeddedDataProtocol`, keyed by the same data type; the engine core's types (`ExternalFormat`, `ModelStore`, `Reference`, `Relation`) are built in. Like the engine's, a grammar may read a core kind of data, which needs no protocol of its own: the data space's `DataspaceTestData` reads a `Reference` |
| `getExtraMappingElementParsers` (mapping element bodies) | `dsl-mapping`'s `MappingElementGrammar`, keyed by the element's type (`Pure`, `EnumerationMapping`, …); it converts what it parses too. The engine core's are built in |
| `getExtraMappingIncludeParsers` (mapping includes) | Read by `dsl-mapping` itself: `include mapping` and the data space extension's `include dataspace` (`ast::IncludeKind`); another type is unsupported |
| `getExtraMappingTestInputDataParsers` (legacy mapping test inputs) | `dsl-mapping`'s `TestInputGrammar`, keyed by the input's type; the core `Object` is built in |
| `getExtraTestAssertionParsers` (test assertions) | Core grammar in `parser` (`ParserContext::parse_test_assertion`): `EqualTo`, `EqualToJson`, and `Relation` as mapping and service tests read it; another type is unsupported |
| `IRelationalGrammarParserExtension` (a relational connection's specifications) | `dsl-relational`'s `SpecificationGrammar` (`SPECIFICATION_GRAMMARS`), keyed by what it specifies (datasource specification, authentication strategy, post-processor, query generation config) and its type, and `LocalModeGrammar` (`LOCAL_MODE_GRAMMARS`) by database type; the relational store's own are built in |
| `IAuthenticationGrammarParserExtension` (a connection's authentication, `# Type { … }#`) | `dsl-authentication`'s `AuthenticationGrammar` (`AUTHENTICATION_GRAMMARS`), keyed by the island's type; the authentication module's own are built in, their secrets a fixed grammar |
| `getExtraConnectionParsers` (connection values) | `dsl-connection`'s `ConnectionValueGrammar`, keyed by the connection's type; the core's model connections are built in. A runtime's own connections, `#{ Type { … } }#`, are read by the same grammars (`dsl-connection`'s `parse_embedded_connection`) |

### Registration

Each extension point has a `linkme` distributed slice; linking a crate
activates its registrations, which is the Rust analog of ServiceLoader:

| Slice | Crate | Registers |
|---|---|---|
| `ISLAND_PARSERS` | `parser` | `IslandParser`, keyed by tag |
| `EMBEDDED_DATA_PARSERS` | `parser` | `EmbeddedDataParser`, keyed by data type |
| `SECTION_PARSERS` | `parser` | `SectionParser`, keyed by section kind |
| `ISLAND_PROTOCOLS` | `protocol` | `IslandProtocol`, keyed by tag |
| `EMBEDDED_DATA_PROTOCOLS` | `protocol` | `EmbeddedDataProtocol`, keyed by data type |
| `ELEMENT_PROTOCOLS` | `protocol` | `ElementProtocol`, keyed by element kind |
| `MAPPING_ELEMENT_GRAMMARS` | `dsl-mapping` | `MappingElementGrammar`, keyed by mapping element type |
| `TEST_INPUT_GRAMMARS` | `dsl-mapping` | `TestInputGrammar`, keyed by legacy test input type |

`parse()` and `convert_source_file()` use whatever is registered; each slice
rejects duplicate keys at first use. A grammar crate depends only on `parser`
and `protocol` (and the host crate whose slice it registers into, e.g. a
store's mapping grammar on `dsl-mapping`), never on `api`. A crate's own
built-in grammars are listed in the crate rather than registered into its own
slice: linkme's registration in the declaring crate needs `unsafe`, which
every crate forbids. Anything parsed but not convertible (no
registered protocol, or no Protocol v1 form, like `native function`) is a
`ConvertError::Unsupported` at the construct's position, never a placeholder.
Tests that need a fixed plug-in set call `parse_with_sections` directly.

Gotcha: rustc drops crates that nothing references, so an aggregator must add
`use grammar_crate as _;`. See `crates/dsl-graph/tests/registration.rs`.

### Where grammars live

All of them live in this workspace for now (see "Location" above). The
implementation follows the declaration, which says where each goes once
legend-pure takes its own back:

- **legend-pure:** the Pure core (`ast`, `lexer`, `parser`, `protocol`, and
  the `api` and `jni` crates), `#{}#` graph fetch and `#>{}#` relation
  accessor, and the `#TDS{}#` and `#SQL{}#` islands, which only keep their
  text (in the engine, both are xts extensions), the engine core's embedded
  data types and `###Data` section (`dsl-data`), and the `###Mapping` section
  (`dsl-mapping`, with the engine core's mapping element grammars; see
  `porting-status.md`), and the relational store's `###Relational` section
  and its mapping, connection and test data grammars (`dsl-relational`).
- **legend-engine:** Connection and Runtime (`dsl-connection`,
  `dsl-runtime`), then a `dsl-*` crate per engine module for its section
  types, its stores' mapping elements, connections and test data, and its
  islands (`#GQL{}#`, …), depending on the legend-pure crates.

### Native library assembly

- The `jni` crate's `cdylib`, `liblegend_pure_rust_parser`, links every
  grammar crate of the workspace; the engine loads it.
- The engine module `legend-engine-language-pure-grammar-rust` ships it: its
  Maven build runs `cargo build --release --locked -p legend-pure-parser-jni`
  and puts the library in the module's jar, under
  `META-INF/native/<platform>/` (`linux-x86_64`, `osx-aarch64`, …).
  `NativeGrammarParser` loads it from there, through a temporary copy, unless
  `-Dlegend.pure.rust.parser.lib` names another; `java.library.path` comes
  last. The build needs `cargo` on the `PATH`, unless `-P skip-rust`: the
  jar then has no library, and the engine parses with its Java parsers
  (`on` mode refuses to parse without it). A jar carries the library of the
  platform it was built on: Linux x86_64 for Build CI and releases.
- The crate is also an `rlib` holding the generic `Java_*` exports, so an
  **aggregator `cdylib`** can link `jni` with another grammar set: how
  legend-pure's grammars and the engine's come together again once they live
  apart. The exported symbols are the same, so Java just loads a different
  library. Force-linking the `jni` rlib (`use legend_pure_rust_parser as _;`)
  is enough for the aggregator to re-export the `Java_*` symbols (verified on
  macOS arm64); no forwarders are needed.

### Rust contract (`api` crate)

`legend_pure_parser_api::parse_section(&SectionRequest)` implements the
section contract above: `lineOffset` / `columnOffset` are applied in the
lexer (`tokenize_with_offset`), so AST, error and protocol positions are all
born in document coordinates; the first error wins; `Unsupported` covers
unlinked grammars, `native function`, and offsets below `-1`. Positions are
always emitted. The body is parsed in its section type
(`parser::parse_section_body`), with no header in front of it; a `###` header
inside it is a token no grammar reads, an error at its first `#`, as in the
engine. `parse_model(&ModelRequest)` parses a whole model as described above:
the engine's split, each section by `parse_section` (the first section's
error wins; a section type without a linked grammar, or offsets below zero,
make the model unsupported), then the section index.
`parse_section_json` / `parse_expression_json` / `parse_model_json_bytes` (a
model written in one buffer) / `capabilities_json` are the JSON forms the JNI
layer returns.

### Generic JNI surface

The Java class `org.finos.legend.pure.rust.parser.NativeGrammarParser` lives in
the engine module `legend-engine-language-pure-grammar-rust` (below), in the
package it had in legend-pure, which the JNI symbols' names follow. Each
method returns its JSON as UTF-8 bytes (`byte[]`): one copy into a Java
array, which Jackson reads as it is, where a Java string would be converted
twice on its way out of the library (to modified UTF-8, then to Latin-1 or
UTF-16) and read through a `Reader`.

**Positions** are strings in this JSON, not the engine's objects: one token
for Jackson rather than twelve, and the whole JSON about a third smaller. The engine's
`SourceInformation` reads both forms (a `@JsonCreator` taking the string,
`fromString`) and still writes objects, so nothing the engine sends out
changes. A source id may hold `:` and `-`; both sides read the string from
its end. The position format needs no lockstep between library and binding:
an engine that reads strings reads a library that writes objects, and a
library that writes strings, given an engine that can't read them, fails as
a protocol divergence (`INTERNAL_ERROR`, then the fallback), not as
undefined behaviour. The native methods' signatures still must match their
library.

- `capabilities()` returns `{"sections":[…],"islands":[…]}`.
- `parseSection(sectionType, code, sourceId, lineOffset, columnOffset)`
  returns JSON: `{"elements":[…],"imports":[…]}` (`imports` absent when
  the section's grammar has none) or
  `{"error":{"message","sourceInformation","kind"}}`.
  `kind: "unsupported"` lets Java fall back to its own parser.
- `parseExpression(kind, code, sourceId, lineOffset, columnOffset)`
  parses a standalone `lambda`, `valueSpecification` or `graphFetch` as the
  engine's `PureGrammarParser` does, into `{"value":…}` or the same error
  JSON. A lambda's text is a code block: one lambda is the result; other
  statements become the body of a lambda without parameters.
- `parseModel(code, sourceId, lineOffset, columnOffset)` parses a whole
  model as the engine's `PureGrammarParser.parseModel` does, into
  `{"elements":[…]}`, the section index last, or the same error JSON.

### Engine Java side

- `RustGrammarParserExtension` takes over the section types the loaded
  library parses, as its `capabilities()` report them (read when the
  extensions load; without a library, none: `on` mode then fails on the
  first model, and `shadow` counts the comparisons it can't make, rather
  than quietly parse with Java), with a
  `SectionParserOverride`, engine core's hook for
  replacing a section type's parser, whether the engine's own or another
  extension's (`###Data`, the xts sections). Each `RustSectionParser` keeps
  the Java parser it replaces, for `off`, the fallback and shadow
  comparisons. A type the Rust library has no grammar for isn't
  taken over, so it parses exactly as with Java alone.
- The mode, `-Dlegend.grammar.rust.mode`, is read per parse:

  | Mode | Behavior |
  |---|---|
  | `off` (default) | Delegates to the Java parser. |
  | `shadow` | Runs both parsers, compares the protocol objects after deserialization, and returns the Java result. |
  | `on` | Returns the Rust result; when the Rust parser fails, see below. |

- **Fallback.** With `-Dlegend.grammar.rust.fallback=true` (the default),
  `on` parses a section again with the Java parser whenever the Rust parser
  fails, whatever the failure: input it doesn't support, a syntax error (the
  engine then reports Java's error), a result the engine can't read, or an
  exception. That parses such sections twice. With `false`, the Rust
  parser's error is reported, e.g. "no island grammar is linked for
  '#GQL{…}#'; the linked ones are: …".
- **Fallback logging.** Every fallback is logged at `WARN`, by one logger,
  `RustGrammarFallback`: the Rust parser's error (its kind, message and
  position) or its exception, with what was parsed (the model, a section of
  some type, or a standalone expression of some kind) and its source id;
  and a model left to the engine's parser because it holds a section of a
  type not taken over, with that type and its header's position.
- A Rust result the engine's classpath can't read is `unsupported`: a class
  instance or embedded data type no protocol extension defines (`#TDS{…}#`
  without xts-tds, `Relational #{…}#` without the relational store). Its
  grammar is an extension this engine lacks, which the Java parser reports
  as it always has.
- It also takes over standalone expressions with an
  `ExpressionParserOverride`: the same modes, fallback and classification
  apply to `parseLambda`, `parseValueSpecification` and `parseGraphFetch`.
- And whole models, with a `ModelParserOverride` (`RustModelParser`). In
  `on` mode, the Rust parser parses the whole model, with no ANTLR parsing in
  Java; when it fails, the fallback decides, as for sections: the engine's
  parser (ANTLR splits the model, and each section is taken over as above),
  or the Rust error. `shadow` compares the whole model, its elements and
  section index, besides each section; `off` parses with the engine's
  parser. A model holding a section of a type the extension doesn't take
  over is the engine's parser's in every mode, as such a section is: Java
  reads the model's section headers with a text scan, as the engine's lexer
  reads them.
- The module reads the Rust JSON as a stream, each element straight into its
  protocol object, with no JSON tree in between. With the bytes, that halves
  the Java side of a Rust parse: the perf benchmark's `parser` suite measured
  the EMIT models at 55 then 47 ms, and a 568 KB relational model at 218 then
  193 ms.
  The engine's reader fails on a class instance of a type no extension maps
  with an `UnknownClassInstanceTypeException`, which the module reports as
  `unsupported` (above).
- The module reads the Rust JSON, and the Java results it compares with it,
  with the engine's protocol mapper, except that floats are read as exact
  `BigDecimal`s. The engine's own mapper reads a decimal through a double
  (`30000.00` becomes `30000.0`, extra digits are rounded), where the Java
  parser keeps the literal's digits and scale.
- A Rust result's section is an `ImportAwareCodeSection` when the result
  has `imports`, and a `DefaultCodeSection` when it doesn't: a section
  grammar says whether it has imports (`SectionParser::imports`; `###Data`
  has none), as Java's does. The parity harness compares the section class
  too.

### Grammar that only exists in Java

A section may contain grammar only the engine's Java extensions define, such
as `#GQL{}#` or an extension's embedded test data (`ServiceStore #{…}#`). The
Rust parser reports such a section `unsupported`, naming what it does link,
and `on` mode parses it with Java. Mixed mode, where Rust would leave a
placeholder for Java to fill in, was considered and dropped (2026-09-27):
falling back per section, even if it parses twice, keeps one parser
responsible for each result.

## Parity harness (legend-engine)

Module `legend-engine-core/legend-engine-core-base/legend-engine-core-language-pure/legend-engine-language-pure-grammar-rust`,
part of the default build, which makes the native library and puts it in the
module's jar; its tests require it. It holds the binding
(`NativeGrammarParser`, with its contract test, `TestNativeGrammarParser`,
which also checks the library loads from the jar),
`RustGrammarParserExtension` / `RustSectionParser` (the modes above),
`RustGrammarParity` (the comparisons) and `TestRustParityCoreGrammar`, which
runs the core grammar module's own parser and roundtrip suites in `shadow`
mode:

```bash
# from the legend-engine checkout's root (JDK 17; a per-branch revision)
mvn -pl legend-engine-core/legend-engine-core-base/legend-engine-core-language-pure/legend-engine-language-pure-grammar-rust \
    test -Drevision=$(git rev-parse --abbrev-ref HEAD)-SNAPSHOT
```

Every section, standalone expression and whole model parsed in `shadow` mode
is parsed by both parsers and compared:

- **Results:** the protocol objects, the Rust JSON deserialized by the
  engine's reader, must equal the Java parser's, section class included.
  When they do and positions were asked for, the Rust JSON as the parser
  wrote it must equal the Java result's JSON too, once positions are written
  in the compact form, nulls and empty arrays dropped and numbers compared
  by value: deserialization can't hide a field the engine's reader drops or
  alters there (`RAW_MISMATCH`).
- **Errors:** when Java rejects the input, Rust must report a parser error
  at the same start position (not ANTLR's wording).
- **Accepted list:**
  `src/main/resources/legend-grammar-rust/parity-accepted.txt` names what
  may differ, one key per line with a category and reason: an input's hash
  (`model-<hash>` for a whole model) for an `accepted` Java-only quirk, and
  `unlinked:<kind>:<type>` for a grammar this library doesn't link yet
  (`pending`, naming the change that ports it) or won't (`java-only`). A
  Rust result whose type no protocol extension on the classpath defines,
  when Java rejected the input too, needs no entry, nor does a whole model
  Rust reports unsupported: one of its sections is, and that section's own
  comparison decides.
- **Unexpected:** any other comparison. `TestRustParityCoreGrammar` fails on
  one; `-Dlegend.grammar.rust.failOnMismatch=true` fails the test that
  parsed a mismatch instead.
- `legend.grammar.rust.reportDir` (the module default:
  `target/rust-parity`): one file per unexpected mismatch (`mismatch/`) or
  unsupported input (`unsupported/`), with the first differing JSON path,
  the input and both results, and, when the JVM exits, a summary
  (`summary/<uuid>.json`): the counts, the unexpected comparisons, the
  accepted list's keys used and the unlinked grammars met.
- A `[rust-parser parity] {MATCH=…, ERROR_MATCH=…, MISMATCH=…, …}` summary is
  printed when the test JVM exits (it counts the as-called comparison and,
  for suites that parse without positions, the positions comparison); whole
  models' comparisons are counted apart, in
  `[rust-parser parity: models] {…}`.

## Live parity (Build CI)

The engine's own suites check the Rust parser as they run: Build CI's
`rustParserParity` matrix entry (`.github/workflows/resources/modulesToTest.json`)
runs the tests of the modules it lists with the Rust grammar module's classes
and native library on their classpath and the Rust parser in `shadow` mode,
and `.github/scripts/rust-parser-parity-gate.sh` merges the summaries of
their test JVMs. It fails on an unexpected comparison, on a comparison the
library was unavailable for, when nothing was compared, and on an accepted
list entry no comparison used (it matches now, or its input is gone: remove
it). The modules are the binding itself, the compiler, the grammar and
compiler HTTP APIs, the core EMIT models (grammar and model-to-model), and
the SQL and TDS expression modules; a change that ports a grammar adds the
modules whose suites use it, their EMIT models included, and removes its
`unlinked:` entries.

Locally, from the checkout's root, with the binding built:

```bash
R=legend-engine-core/legend-engine-core-base/legend-engine-core-language-pure/legend-engine-language-pure-grammar-rust
mvn test -pl <the modules> -Drevision=$(git rev-parse --abbrev-ref HEAD)-SNAPSHOT \
    -Dmaven.test.additionalClasspath=$PWD/$R/target/classes \
    -Dlegend.grammar.rust.mode=shadow -Dlegend.grammar.rust.reportDir=/tmp/parity \
    -Dlegend.pure.rust.parser.required=true -Dmaven.test.failure.ignore=true
.github/scripts/rust-parser-parity-gate.sh /tmp/parity $R/src/main/resources/legend-grammar-rust/parity-accepted.txt
```

## Parity target

- **Target:** the protocol objects `on` mode gives the engine (the Rust
  JSON, deserialized) equal the Java parser's, source positions included.
  The harness compares both after deserialization, which hides a field a
  deserializer drops; its raw JSON comparison covers that (see above).
- **Not a target:** ANTLR's error message wording, or byte-identical JSON.
  Rust omits empty lists and `None` fields, and Jackson sorts keys.

## Known divergences

The accepted list tracks what the engine's suites exercise. Found while
building the conversion, and not exercised by those suites yet; each needs a
fix at its source (parser or AST), not in the protocol output:

- **Chained comparisons.** Rust's `parse_comparison` takes one operator, so
  `a < b < c` and `a == b == c` are parse errors; the engine accepts them
  (`lessThan(lessThan(a, b), c)`, `equal(a, equal(b, c))`).
- **Unary plus.** The Rust parser drops `+x`; the engine emits `plus(x)`.
- **Grammar superset.** Rust accepts some syntax the engine rejects. Bitwise
  operators and `allVersionsInRange()` are already `Unsupported`, and
  `parse_section`'s engine profile rejects `<T|m>` parameters. Still
  accepted: bare `(cols)`, `@[m]`, arithmetic inside collection literals,
  function types (the engine's "The type {…} is not supported yet"), and
  Legend Pure's type-argument algebra (`Z=T - X + Y ⊆ S`), which the protocol
  drops.
- **Engine-extension islands.** `#GQL{…}#`, `ServiceStore #{…}#` test data
  and the other islands and embedded data whose grammar is an engine
  extension with no Rust port are `unsupported`, so the engine parses such
  sections with Java (see "Grammar that only exists in Java" above). The
  engine core's embedded data types are all parsed.
