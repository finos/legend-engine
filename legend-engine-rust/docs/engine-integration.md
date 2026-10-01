# Engine Integration & Extensibility

How this parser plugs into Legend Engine, and how Engine adds its own Rust
grammars later. Status: design agreed September 2026. Milestone 1 (`###Pure`
sections through JNI, verified by a shadow-mode parity harness) is in progress.

**Location.** This workspace lives in legend-engine, as `legend-engine-rust/`,
for now: it moved on 2026-10-01 from legend-pure's `rust-parser` branch
(`legend-pure-rust/` at 172d559fedd), together with the Java binding, so the
engine needs no legend-pure snapshot and one repository holds a change from
grammar to engine. Crates, the binding's package and the library keep their
legend-pure names. What belongs to legend-pure (see "Where grammars live")
goes back there once its interpreter work restarts.

## Engine hook

Engine core (legend-engine branch `rust-parser`,
`legend-engine-language-pure-grammar`) has two hooks a ServiceLoader
`PureGrammarParserExtension` uses to replace its parsers:
`SectionParserOverride` replaces a section type's parser, whoever defines the
type, and `ExpressionParserOverride` replaces the parser of standalone
expressions. Each override receives the parser it replaces.

The section parser receives `SectionSourceCode`:

- `code` is the section **body**, without its `###Pure` header. The Rust
  parser treats headerless input as a `Pure` section with imports.
- `walkerSourceInformation` carries `sourceId`, `lineOffset` and
  `columnOffset`. `lineOffset` is signed and is often `-1`: headerless code gets
  `\n###Pure\n` prepended, the header sits on line 1, and
  `lineOffset = headerLine - 2`.
- Positions are 1-based with an inclusive end column (ANTLR semantics).
  `columnOffset` applies to the first line only.
  `returnSourceInfo = false` means no `sourceInformation` anywhere.

The section parser must return the section class Java's parser returns (an
`ImportAwareCodeSection` for a grammar with imports, always for `###Pure`;
a `DefaultCodeSection` for one without, such as `###Data`), feed each
element to the consumer, and signal errors as
`EngineException(message, SourceInformation, PARSER)`.

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
| `PureProtocolExtension` element subtypes | `protocol::ElementProtocol` (DSL element → JSON object), carried as the untagged `PackageableElement::Extension(Value)` |
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
  text (in the engine, both are xts extensions), the relational store's
  `###Relational` section and `Relational #{…}#` test data
  (`dsl-relational`), the engine core's embedded data types and
  `###Data` section (`dsl-data`), and the `###Mapping` section
  (`dsl-mapping`, with the engine core's mapping element grammars: step 4
  of `porting-status.md`).
- **legend-engine:** Connection, Runtime, Service, DataSpace, the xts
  stores' mapping elements, connections and test data, and the xts islands
  (`#GQL{}#`, …): a `dsl-*` crate per engine module (`dsl-connection`,
  `dsl-runtime`, `dsl-service`, `dsl-persistence` and its extensions',
  `dsl-data-space`, `dsl-dataquality`, `dsl-diagram`, `dsl-external-format`, `dsl-relational-snowflake`, `dsl-service-store`,
  `dsl-mongodb`, `dsl-deephaven`, `dsl-elasticsearch`,
  `dsl-authentication`; the data space include is read in `dsl-mapping`),
  depending on the legend-pure crates.

### Native library assembly

- The `jni` crate's `cdylib`, `liblegend_pure_rust_parser`, links every
  grammar crate of the workspace; the engine loads it.
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
unlinked grammars, `native function`, and offsets below `-1`. With
`returnSourceInfo = false`, every `sourceInformation` / `…SourceInformation`
field is removed. `parse_section_json` / `capabilities_json` are the JSON
forms the JNI layer returns.

### Generic JNI surface

The Java class `org.finos.legend.pure.rust.parser.NativeGrammarParser` lives in
the engine module `legend-engine-language-pure-grammar-rust` (below), in the
package it had in legend-pure, which the JNI symbols' names follow:

- `capabilities()` returns `{"sections":[…],"islands":[…]}`.
- `parseSection(sectionType, code, sourceId, lineOffset, columnOffset, returnSourceInfo)`
  returns JSON: `{"elements":[…],"imports":[…]}` (`imports` absent when
  the section's grammar has none) or
  `{"error":{"message","sourceInformation","kind"}}`.
  `kind: "unsupported"` lets Java fall back to its own parser.
- `parseExpression(kind, code, sourceId, lineOffset, columnOffset, returnSourceInfo)`
  parses a standalone `lambda`, `valueSpecification` or `graphFetch` as the
  engine's `PureGrammarParser` does, into `{"value":…}` or the same error
  JSON. A lambda's text is a code block: one lambda is the result; other
  statements become the body of a lambda without parameters.

### Engine Java side

- `RustGrammarParserExtension` takes over the section types the loaded
  library parses, as its `capabilities()` report them (read when the
  extensions load; without a library, `Pure` alone, so that `on` and
  `shadow` fail rather than quietly parse with Java), with a
  `SectionParserOverride`, engine core's hook for
  replacing a section type's parser, whether the engine's own or another
  extension's (`###Data`, the xts sections). Each `RustSectionParser` keeps
  the Java parser it replaces, for `off`, the fallback, shadow comparisons
  and the corpus dump. A type the Rust library has no grammar for isn't
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
- A Rust result the engine's classpath can't read is `unsupported`: a class
  instance or embedded data type no protocol extension defines (`#TDS{…}#`
  without xts-tds, `Relational #{…}#` without the relational store). Its
  grammar is an extension this engine lacks, which the Java parser reports
  as it always has.
- It also takes over standalone expressions with an
  `ExpressionParserOverride`: the same modes, fallback and classification
  apply to `parseLambda`, `parseValueSpecification` and `parseGraphFetch`.
- The module reads the Rust JSON, and the Java results it compares with it,
  with the engine's protocol mapper, except that floats are read as exact
  `BigDecimal`s. The engine's own mapper reads a decimal through a double
  (`30000.00` becomes `30000.0`, extra digits are rounded), where the Java
  parser keeps the literal's digits and scale.
- A Rust result's section is an `ImportAwareCodeSection` when the result
  has `imports`, and a `DefaultCodeSection` when it doesn't: a section
  grammar says whether it has imports (`SectionParser::imports`; `###Data`
  has none), as Java's does. The parity harness and the golden corpus
  compare the section class too.

### Grammar that only exists in Java

A section may contain grammar only the engine's Java extensions define, such
as `#GQL{}#` or an extension's embedded test data (`ServiceStore #{…}#`). The
Rust parser reports such a section `unsupported`, naming what it does link,
and `on` mode parses it with Java. Mixed mode, where Rust would leave a
placeholder for Java to fill in, was considered and dropped (2026-09-27):
falling back per section, even if it parses twice, keeps one parser
responsible for each result.

## Parity harness (legend-engine)

Module `legend-engine-core/legend-engine-core-base/legend-engine-core-language-pure/legend-engine-language-pure-grammar-rust`
(branch `rust-parser` of legend-engine), part of the default build: without
the native library, its tests that need it are skipped. It holds the binding
(`NativeGrammarParser`, with its smoke test, `TestNativeGrammarParser`),
`RustGrammarParserExtension` / `RustSectionParser` (the modes above) and
`TestRustParityCoreGrammar`, which runs the core grammar module's own 22
parser/roundtrip suites in `shadow` mode:

```bash
# from the legend-engine checkout's root (JDK 17; a per-branch revision)
(cd legend-engine-rust && cargo build -p legend-pure-parser-jni)
mvn -pl legend-engine-core/legend-engine-core-base/legend-engine-core-language-pure/legend-engine-language-pure-grammar-rust \
    test -Drevision=rust-parser-SNAPSHOT \
    -Dlegend.pure.rust.parser.lib=$PWD/legend-engine-rust/target/debug/liblegend_pure_rust_parser.dylib \
    -Dlegend.grammar.rust.reportDir=/tmp/parity
```

- `legend.grammar.rust.reportDir`: one file per mismatching (`mismatch/`) or
  unsupported (`unsupported/`) section, with the first differing JSON path,
  the section text and both results.
- `legend.grammar.rust.failOnMismatch=true`: strict mode (the module default
  is `false` while known gaps are being closed).
- `legend.grammar.rust.corpusDir` / `legend.grammar.rust.expressionCorpusDir`:
  dump every section / standalone expression with the Java result
  (positions on) as a golden corpus (see below); needs no native library.
- A `[rust-parser parity] {MATCH=…, ERROR_MATCH=…, MISMATCH=…, …}` summary is
  printed when the test JVM exits (it counts the as-called comparison and,
  for suites that parse without positions, the positions comparison).

## Golden corpus (Rust CI)

`tests/engine-corpus/` holds what the engine's test suites parse, with the
Java parser's result:

- `sections/`: every section body that the engine's core grammar suites,
  its compiler's grammar-based suites, the tests of the grammar and compiler
  HTTP APIs and the tests of 43 xts modules parse, of the types the Rust
  parser has a grammar for and of those step 7 of `porting-status.md` ports
  (since 2026-10-01, every section type the engine has);
- `expressions/`: every standalone expression that the core grammar suites
  and the tests of the compiler, the grammar and compiler HTTP APIs and 6 xts
  modules parse. Their offsets are all zero; the `api` crate's tests cover
  offsets.

Each entry is `<hash>.pure` (the text), `<hash>.meta.json` (section type or
expression kind, source id, line and column offsets) and `<hash>.java.json`
(`{elements, imports}` or `{value}` with positions, or
`{error: {message, sourceInformation}}`). The test
`crates/api/tests/engine_corpus.rs` parses each entry with `parse_section` or
`parse_expression` (positions on) and compares, with no JVM:

- **Results:** the JSON must be equal once nulls and empty arrays, which the
  engine reads back as absent, are dropped on both sides. Nothing else is
  normalized. A section's class is compared first: `imports` is recorded
  only for an `ImportAwareCodeSection`.
- **Errors:** Rust must report a `parser` error at the engine's start
  position.
- **Known failures:** `tests/engine-corpus/known-failures.txt` lists the
  entries that don't match yet, each with a category and reason. An unlisted
  mismatch fails the test, and so does a listed entry that now matches
  (remove its line), so the list only shrinks, apart from `accepted`
  entries: Java-only quirks the Rust parser, meant to replace it, doesn't
  reproduce.
- **Section types:** a type joins the corpus when its Rust grammar does:
  the dump records the section types the library it is given parses. To
  record a type's Java results before its Rust grammar is written, name it
  in `-Dlegend.grammar.rust.corpusSections` (comma-separated); the library
  reports it unsupported, so Java parses it. Inside the types it parses, a
  section is still unsupported, and listed as such, when it holds grammar
  without a Rust port: a mapping element, test input or connection of a
  type no linked grammar reads (while a section type's grammars are being
  ported, each such section's line names the subphases it waits for, as
  `scripts/known-failures.py` writes them), or a milestoning type other
  than `business` and `processing`.
- **Diffing:** the canonical Java and Rust JSON of each failure are written
  to `target/tmp/engine-corpus/`.

The parity harness and the corpus test each see what the other doesn't.
The corpus test compares with positions on only, and reads `[]` as absent.
The harness also catches differences that appear only with positions off:
the engine keeps positions on a function test's values even then. It also
catches the engine's list fields that default to null rather than empty, such
as `Enumeration.values` and `Measure.nonCanonicalUnits`. But both of the
harness's sides go through the engine's deserializers, so a field one of them
drops or alters compares equal there, while `on` mode loses it. The corpus
test compares Rust's JSON as the parser wrote it, with the Java result as the
engine reads it back, and so catches such a field whenever Rust writes it.
It found `ColSpec`'s deserializer dropping the column multiplicity (fixed in
the engine's protocol) and decimals read through doubles (the Rust grammar
module's mapper reads them exactly).

Regenerate after engine grammar changes, from the legend-engine checkout's
root, with JDK 17. The suites run in `off` mode; the native library only says which
section types to record. The first
command runs the core grammar suites and packages the Rust grammar module;
the others run other modules' own tests with that module, which holds the
binding, added to their classpath. The same technique runs the parity harness over
any module's tests, in `shadow` mode with `-Dlegend.pure.rust.parser.lib`.

```bash
C=$PWD/legend-engine-rust/tests/engine-corpus/sections
rm -rf "$C"   # the dump only adds files
M=legend-engine-core/legend-engine-core-base/legend-engine-core-language-pure
V="-Drevision=rust-parser-SNAPSHOT"
# the library, whose section types are recorded
L=$PWD/legend-engine-rust/target/debug/liblegend_pure_rust_parser.dylib
# while step 7 ports them, the section types the library doesn't parse yet
T=Persistence,DataSpace,DataQualityValidation,ServiceStore,ExternalFormat,Diagram,Snowflake,Elasticsearch,MongoDB,FileGeneration,GenerationSpecification,QueryPostProcessor,Deephaven,MemSql,HostedService,BigQuery,Text,FunctionJar
DUMP="$V -Dlegend.grammar.rust.mode=off -Dlegend.pure.rust.parser.lib=$L -Dlegend.grammar.rust.corpusSections=$T -Dlegend.grammar.rust.corpusDir=$C"
CP=$PWD/$M/legend-engine-language-pure-grammar-rust/target/legend-engine-language-pure-grammar-rust-rust-parser-SNAPSHOT.jar
# 1. the core grammar suites
# (only them: the module's own tests take over types without a Rust grammar)
mvn -pl $M/legend-engine-language-pure-grammar-rust package $DUMP \
    -Dtest=TestRustParityCoreGrammar -Dsurefire.failIfNoSpecifiedTests=false
# 2. the compiler's grammar-based suites
mvn -pl $M/legend-engine-language-pure-compiler test $DUMP -Dmaven.test.additionalClasspath=$CP \
    -Dtest='Test*CompilationFromGrammar,TestMilestoned*,TestRelation*' -Dsurefire.failIfNoSpecifiedTests=false
# 3. the grammar and compiler HTTP APIs' tests
for h in grammar compiler; do
  mvn -pl $M/legend-engine-language-pure-$h-http-api test $DUMP -Dmaven.test.additionalClasspath=$CP \
      -Dtest='Test*,*Test,*Tests' -Dsurefire.failIfNoSpecifiedTests=false
done
# 4. the xts modules' tests
X=(
  legend-engine-xts-analytics/legend-engine-xts-analytics-search/legend-engine-xt-analytics-search-generation
  legend-engine-xts-changetoken/legend-engine-xt-changetoken-compiler
  legend-engine-xts-data-space/legend-engine-xt-data-space-compiler
  legend-engine-xts-data-space/legend-engine-xt-data-space-generation
  legend-engine-xts-data-space/legend-engine-xt-data-space-grammar
  legend-engine-xts-dataquality/legend-engine-xt-dataquality-api
  legend-engine-xts-dataquality/legend-engine-xt-dataquality-compiler
  legend-engine-xts-deephaven/legend-engine-xt-deephaven-generator
  legend-engine-xts-deephaven/legend-engine-xt-deephaven-grammar
  legend-engine-xts-diagram/legend-engine-xt-diagram-compiler
  legend-engine-xts-diagram/legend-engine-xt-diagram-grammar
  legend-engine-xts-elasticsearch/legend-engine-xt-elasticsearch-V7-grammar
  legend-engine-xts-flatdata/legend-engine-xt-flatdata-driver-bloomberg
  legend-engine-xts-flatdata/legend-engine-xt-flatdata-model
  legend-engine-xts-functionJar/legend-engine-xt-functionJar-compiler
  legend-engine-xts-hostedService/legend-engine-xt-hostedService-compiler
  legend-engine-xts-json/legend-engine-xt-json-model
  legend-engine-xts-json/legend-engine-xt-json-runtime
  legend-engine-xts-mongodb/legend-engine-xt-nonrelationalStore-mongodb-grammar-integration
  legend-engine-xts-openapi/legend-engine-xt-openapi-generation
  legend-engine-xts-persistence/legend-engine-xt-persistence-emit
  legend-engine-xts-persistence/legend-engine-xt-persistence-grammar
  legend-engine-xts-persistence/legend-engine-xt-persistence-test-runner
  legend-engine-xts-powerbi/legend-engine-xt-powerbi-generation
  legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-snowflake/legend-engine-xt-relationalStore-snowflake-grammar
  legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit
  legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-execution/legend-engine-xt-relationalStore-executionPlan
  legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-grammar
  legend-engine-xts-service/legend-engine-language-pure-dsl-service
  legend-engine-xts-serviceStore/legend-engine-xt-serviceStore-executionPlan
  legend-engine-xts-serviceStore/legend-engine-xt-serviceStore-grammar
  legend-engine-xts-sql/legend-engine-xt-sql-expression/legend-engine-xt-sql-expression-compiler
  legend-engine-xts-tds/legend-engine-xt-tds-compiler
  # the step 7 section types' own grammar modules
  legend-engine-xts-generation/legend-engine-language-pure-dsl-generation
  legend-engine-xts-text/legend-engine-xt-text-grammar
  legend-engine-xts-dataquality/legend-engine-xt-dataquality-grammar
  legend-engine-xts-hostedService/legend-engine-xt-hostedService-grammar
  legend-engine-xts-functionJar/legend-engine-xt-functionJar-grammar
  legend-engine-xts-snowflake/legend-engine-xt-snowflake-grammar
  legend-engine-xts-bigqueryFunction/legend-engine-xt-bigqueryFunction-grammar
  legend-engine-xts-memsqlFunction/legend-engine-xt-memsqlFunction-grammar
  legend-engine-xts-persistence/legend-engine-xt-persistence-target-relational-grammar
  legend-engine-xts-persistence/legend-engine-xt-persistence-cloud-grammar
)
# install them at this revision first; their Pure (PAR) modules need `clean`
MAVEN_OPTS=-Xmx32g mvn clean install -T 3 -DskipTests $V -pl "$(IFS=,; echo "${X[*]}")" -am
for x in "${X[@]}"; do
  mvn -pl "$x" test $DUMP -Dmaven.test.additionalClasspath=$CP -Dmaven.test.failure.ignore=true \
      -Dtest='Test*,*Test,*Tests' -Dsurefire.failIfNoSpecifiedTests=false
done
```

The expressions come from the core grammar suites and from the modules whose
code or tests call the expression parser:

```bash
E=$PWD/legend-engine-rust/tests/engine-corpus/expressions
rm -rf "$E"
EDUMP="$V -Dlegend.grammar.rust.mode=off -Dlegend.grammar.rust.expressionCorpusDir=$E"
mvn -pl $M/legend-engine-language-pure-grammar-rust package $EDUMP \
    -Dtest=TestRustParityCoreGrammar -Dsurefire.failIfNoSpecifiedTests=false
Y=(
  $M/legend-engine-language-pure-compiler
  $M/legend-engine-language-pure-grammar-http-api
  $M/legend-engine-language-pure-compiler-http-api
  legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-grammar
  legend-engine-xts-dataquality/legend-engine-xt-dataquality-api
  legend-engine-xts-dataquality/legend-engine-xt-dataquality-generation
  legend-engine-xts-sql/legend-engine-xt-sql-expression/legend-engine-xt-sql-expression-compiler
  legend-engine-xts-tds/legend-engine-xt-tds-compiler
  legend-engine-xts-flatdata/legend-engine-xt-flatdata-runtime
)
# install them at this revision first, as above
for y in "${Y[@]}"; do
  mvn -pl "$y" test $EDUMP -Dmaven.test.additionalClasspath=$CP -Dmaven.test.failure.ignore=true \
      -Dtest='Test*,*Test,*Tests' -Dsurefire.failIfNoSpecifiedTests=false
done
```

Then run `cargo test -p legend-pure-parser-api --test engine_corpus` in
`legend-engine-rust/` and update `known-failures.txt` from its report.

## Parity target

- **Target:** the protocol objects `on` mode gives the engine (the Rust
  JSON, deserialized) equal the Java parser's, source positions included.
  The harness compares both after deserialization, which hides a field a
  deserializer drops; the golden corpus covers that (see above).
- **Not a target:** ANTLR's error message wording, or byte-identical JSON.
  Rust omits empty lists and `None` fields, and Jackson sorts keys.

## Known divergences

The golden corpus's `known-failures.txt` tracks what the engine's suites
exercise. Found while building the conversion, and not covered by the corpus
yet; each needs a fix at its source (parser or AST), not in the protocol
output:

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
