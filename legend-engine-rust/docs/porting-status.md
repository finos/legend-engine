# Grammar porting status

Which engine grammars the Rust parser reads. A section type is taken over by
the engine as soon as the linked library parses it (see
`engine-integration.md`, "Engine Java side"); a section that holds grammar not
ported yet is `unsupported`, so the engine parses it with Java. Each grammar is
checked by live parity (`engine-integration.md`, "Live parity"): the engine's
suites that use it run in `shadow` mode in Build CI.

Every section type the engine has is ported. They were ported on
legend-engine's `rust-parser` branch (finos/legend-engine#5362) and joined this
workspace one change at a time, each with the modules whose suites use its
grammars added to the live parity entry.

## The grammars

| Grammar | Crate | Engine owner |
|---|---|---|
| Section, expression and model override hooks; standalone expressions (`parseLambda`, `parseValueSpecification`, `parseGraphFetch`) | `api`, `parser` | core |
| `###Pure`: classes, enumerations, functions and their tests, profiles, associations, measures; `native function` is parsed and `unsupported` | `parser`, `protocol` | core |
| Islands: graph fetch `#{…}#`, relation accessor `#>{…}#`, `#SQL{…}#` and `#TDS{…}#`, the last two keeping their text | `dsl-graph`, `dsl-store`, `dsl-sql`, `dsl-tds` | core, sql, tds |
| Core embedded data (`ExternalFormat`, `ModelStore`, `Reference`, `Relation`) and test assertions (`EqualTo`, `EqualToJson`, `Relation`); `###Data` | `parser`, `protocol`, `dsl-data` | core |
| `###Mapping`: the section (includes, element headers, `testSuites`, legacy `MappingTests` with `Object` input), `include dataspace`, and the core's mapping elements: `EnumerationMapping`, `Pure`, `Relation`, `XStore`, `ModelJoin`, `Operation`, `AggregationAware` | `dsl-mapping` | core |
| `###Connection` and `###Runtime`: the sections (`Runtime`, `SingleConnectionRuntime`, embedded connections, connection stores) and the core's model connections (`JsonModelConnection`, `XmlModelConnection`, `ModelChainConnection`) | `dsl-connection`, `dsl-runtime` | core |
| `###Relational`, `Relational` mapping elements and test input, `Relational #{…}#` test data, `RelationalDatabaseConnection` with its specifications, `###QueryPostProcessor`; Snowflake's datasource specification and authentication strategy | `dsl-relational`, `dsl-relational-snowflake` | relationalStore |
| `###Service` | `dsl-service` | service |
| `###ServiceStore` (with its mapping elements, connection and test data), `###MongoDB` (with its mappings and connection), `###Elasticsearch` (with its connection), and connections' authentication specifications | `dsl-service-store`, `dsl-mongodb`, `dsl-elasticsearch`, `dsl-authentication` | serviceStore, mongodb, elasticsearch, authentication |
| Function activators: `###Snowflake`, `###MemSql`, `###BigQuery`, `###FunctionJar`, `###HostedService`; `###Deephaven` (with its connection) | `dsl-function-activator`, `dsl-snowflake`, `dsl-memsql-function`, `dsl-bigquery-function`, `dsl-function-jar`, `dsl-hosted-service`, `dsl-deephaven` | snowflake, memsqlFunction, bigqueryFunction, functionJar, hostedService, deephaven |
| `###Persistence`, with its relational target and cloud platform | `dsl-persistence`, `dsl-persistence-relational`, `dsl-persistence-cloud` | persistence |
| `###DataSpace`, `###DataQualityValidation`, `###Diagram`, `###ExternalFormat`, `###FileGeneration` and `###GenerationSpecification`, `###Text` | `dsl-data-space`, `dsl-dataquality`, `dsl-diagram`, `dsl-external-format`, `dsl-generation`, `dsl-text` | data-space, dataquality, diagram, external format, generation, text |
| Whole models (`parseModel`): the engine's section split and section index, parsed in Rust in `on` mode, with no ANTLR parsing in Java | `api` | core |

A mapping element, legacy test input, connection, embedded data, island or
test assertion type that only an engine extension without a Rust port reads
(`#GQL{…}#`, for instance) makes its section `unsupported`, so the engine parses
it with Java.

## Whole models

`PureGrammarParser.parseModel` parses a whole model: it splits the code into
sections with its ANTLR `CodeParserGrammar`, then parses each with its
section type's parser. In `on` mode, the Rust parser parses the whole model
instead, with no ANTLR parsing in Java; when it can't, the fallback parses it
the engine's way, whose sections the Rust parser still takes over one by one
(see `engine-integration.md`, "Engine Java side").

`api::parse_model` splits the code as the engine does, by text alone
(checked case by case against the engine: a header in a string, indented,
nameless or followed by code, CRLF, an empty section, offsets), parses each
section with `parse_section`, and builds the section index, its spans
measured as the engine measures them. Code written on a section's header line
is parsed in its section's type, and a `###` inside a body is a token no
grammar reads, an error at its first `#`, as in the engine.

The whole-model path's own logic is covered by
`crates/api/tests/parse_model.rs`, whose expectations are the engine's
results, and by live parity, which compares whole models as well as sections.
