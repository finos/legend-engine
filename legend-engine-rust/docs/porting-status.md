# Grammar porting status

Which engine grammars the Rust parser reads, in the order they are ported.
A section type is taken over by the engine as soon as the linked library
parses it (see `engine-integration.md`, "Engine Java side"); a section that
holds grammar not ported yet is `unsupported`, so the engine parses it with
Java. Each port is checked against the golden corpus and the shadow harness
over the corpus suites before it is marked done.

## Steps

| Step | Grammar | Rust crate | Status |
|---|---|---|---|
| 0 | Section and expression override hooks; corpus for any section type | `api` (+ engine grammar-rust) | Done |
| 1 | Standalone expressions (`parseLambda`, `parseValueSpecification`, `parseGraphFetch`) | `api`, `parser` | Done |
| 2 | Core embedded data (`ExternalFormat`, `ModelStore`, `Reference`) and `###Data` | `parser`, `protocol`, `dsl-data` | Done |
| 3 | `###Relational` | `dsl-relational` | Done |
| 4 | `###Mapping`, one subphase per mapping grammar (below) | `dsl-mapping` and the stores' crates | Done |
| 5 | `###Connection` and `###Runtime`, one subphase per connection grammar (below) | `dsl-connection`, `dsl-runtime`, `dsl-authentication` and the stores' crates | Done |
| 6 | `###Service` (below) | `dsl-service` | Done |
| 7 | The other section types, by volume, one subphase each (below) | a `dsl-*` crate per engine module | In progress |

## Step 4: `###Mapping`

The engine's `###Mapping` section (`MappingParserGrammar`) holds includes,
mapping elements, legacy `MappingTests` and `testSuites`. Each mapping
element's body is a grammar of its own, chosen by the element's type
(`Person: Pure { … }`) and registered by the engine module that owns it; a
body whose grammar is not ported makes its section `unsupported`.

"Sections" counts the golden corpus's `###Mapping` sections that wait for
the grammar (662 in all, 50 of them rejected by the engine; dumped
2026-09-30). `tests/engine-corpus/known-failures.txt` names, for each section
still unsupported, every subphase it waits for, so
`grep -c 'step 4.2 ' tests/engine-corpus/known-failures.txt` counts the
sections 4.2 unlocks; a subphase is done when no line names it
(`scripts/known-failures.py` writes the lines). A section matches
once all the grammars it uses are ported. The elements a subphase reads in
sections that still wait for others are checked too, by comparing those
sections with the other elements blanked out (spaces, so that nothing
moves) against the Java result without them
(`scripts/isolate-mapping-sections.py`).

| Subphase | Grammar | Engine owner | Sections | Status |
|---|---|---|---|---|
| 4.0 | The section: `Mapping` elements, imports, `include mapping` (with store substitutions), mapping element headers (`*`, ids, `extends`), `testSuites` (embedded data and assertions), legacy `MappingTests` with `Object` test input data | core | 662 | Done: 75 sections match |
| 4.1 | `EnumerationMapping` | core | 58 | Done: 13 more sections match |
| 4.2 | `Pure` class mappings (model to model) | core | 202 | Done: 110 more sections match |
| 4.3 | `Relational` class and association mappings, `Relational` test input data | relationalStore | 178 | Done: 166 more sections match |
| 4.4 | `Relation` class mappings (relation functions) | core | 117 | Done: 105 more sections match |
| 4.5 | `XStore` association mappings | core | 69 | Done, with 4.6: 85 more sections match |
| 4.6 | `ModelJoin` association mappings | core | 25 | Done, with 4.5 |
| 4.7 | `Operation` class mappings (including merge operations) | core | 25 | Done: 24 more sections match |
| 4.8 | `AggregationAware` class mappings and their `AggregateSpecification`s | core | 11 | Done: 11 more sections match |
| 4.9 | `ServiceStore` class mappings | serviceStore | 62 | Done: 61 more sections match (the other is an accepted divergence), and the 26 sections with `ServiceStore #{…}#` test data |
| 4.10 | `MongoDB` class mappings | mongodb | 5 | Done: 5 more sections match |
| 4.11 | `include dataspace` | data-space | 4 | Done: 4 more sections match |

The core grammars come first. 4.9 to 4.11 belong to xts modules with no
legend-pure counterpart: a `dsl-*` crate per engine module (`dsl-service-store`
for 4.9, `dsl-mongodb` for 4.10), except 4.11, an include type read in
`dsl-mapping`. Steps 5 to 7 follow the same rule. All the grammars, the
engine's and legend-pure's, live in this workspace, in legend-engine, since
2026-10-01 (see `engine-integration.md`, "Location").

With 4.11, every mapping grammar the corpus uses is ported. The only
`###Mapping` sections the Rust parser leaves to Java are the two the engine
rejects for an element or test input type no grammar reads: Rust reports
them `unsupported`, since an engine extension may read the type.

## Step 5: `###Connection` and `###Runtime`

The engine's `###Connection` section (`ConnectionParserGrammar`) holds
connections, `Type my::Connection { … }`, each value read by the grammar its
type names (the engine's `ConnectionValueParser`s), registered by the engine
module that owns the type. Its `###Runtime` section (`RuntimeParserGrammar`)
holds runtimes: their mappings, and their connections by store, pointers to
packageable connections or connections of their own, `#{ Type { … } }#`, read
by the same grammars. A connection whose type's grammar is not ported makes
its section `unsupported`.

"Sections" counts the golden corpus's `###Connection` and `###Runtime`
sections that wait for the grammar (310 in all: 198 `###Connection`, 41 of
them rejected by the engine, and 112 `###Runtime`, 5 rejected; dumped
2026-09-30). `tests/engine-corpus/known-failures.txt` names the subphases
each unsupported section waits for, as for step 4
(`scripts/known-failures.py` writes the lines).

| Subphase | Grammar | Engine owner | Sections | Status |
|---|---|---|---|---|
| 5.0 | The sections: connections, runtimes (`Runtime` and `SingleConnectionRuntime`: mappings, connections by store, pointers and embedded connections, connection stores), and the engine core's model connections (`JsonModelConnection`, `XmlModelConnection`, `ModelChainConnection`) | core | 310 | Done: 140 sections match |
| 5.1 | `RelationalDatabaseConnection`, with the relational store's datasource specifications, authentication strategies, post-processors and query generation configs | relationalStore | 132 | Done: 105 more sections match (one the engine rejects for a query generation config type no grammar reads is accepted) |
| 5.2 | Snowflake datasource specification and authentication strategy | relationalStore-snowflake | 20 | Done: 26 more sections match (those the engine rejects for Snowflake's fields among them) |
| 5.3 | `ServiceStoreConnection` | serviceStore | 16 | Done: 15 more sections match; one end-of-input error position is accepted |
| 5.4 | `MongoDBConnection`, with the authentication strategies of `dsl-authentication` | mongodb | 5 | Done: 5 more sections match |
| 5.5 | `DeephavenConnection` | deephaven | 6 | Done: 6 more sections match |
| 5.6 | `Elasticsearch7ClusterConnection` | elasticsearch | 11 | Done: 11 more sections match |

The relational store's connection is a grammar of grammars: its datasource
specification, authentication strategy and post-processors are each read by
the grammar their type names, which the store's database extensions register
(Snowflake's is the only one the corpus suites use). The database
extensions' grammars other than Snowflake's are not in the corpus.

The MongoDB, Deephaven and Elasticsearch connections name their
authentication as an island of its own, `# Type { … }#`, read by the
grammar its type names (the engine's `AuthenticationSpecification`
grammars, `dsl-authentication`).

With 5.6, every connection grammar the corpus uses is ported. The only
`###Connection` and `###Runtime` sections the Rust parser leaves to Java are
those the engine rejects for a type no grammar reads (a query generation
config): Rust reports them `unsupported`, since an engine extension may read
the type.

## Step 6: `###Service`

The engine's `###Service` section (its service extension's
`ServiceParserGrammar`) holds services and execution environments. A
service's queries, test parameters and post-validations are Pure
expressions and primitive values; its runtime is a packaged one's path or
one of its own, `#{ … }#`, read by the runtime grammar (`dsl-runtime`'s
`parse_embedded_runtime`); its test suites' data and assertions are read by
the parser's embedded data and test assertion grammars, so every grammar it
uses was ported by steps 1 to 5. The step is the section's own grammar
alone: one subphase.

The golden corpus's 237 `###Service` sections (dumped 2026-10-01; 45 of
them rejected by the engine) all match but the two the engine rejects for
an embedded data or test assertion type no grammar reads: Rust reports them
`unsupported`, since an engine extension may read the type. An execution
of another type than `Single` and `Multi`, which only an engine extension
reads, is unsupported too.

Two engine behaviors came with it: a primitive value (a test's parameter,
an `EqualTo` assertion's expected value) is read by the engine's
`primitiveValue` rule, `toBytes('…')` included
(`ParserContext::parse_primitive_value`); and a lambda parameter typed
`Result` without type arguments is `Result<Any|1..*>`, as the engine's
protocol mapper makes it when it reads the protocol.

## Step 7: the other section types

The engine's other section types, each its own grammar, registered by the
engine module that owns it, ported in order of volume: one subphase each
(both of the generation module's in one). Their Rust grammars are in the
crate of the engine module that owns them (an existing one for the stores
whose connections and mapping elements step 4 and 5 ported).

"Sections" counts the golden corpus's sections of the type (737 in all;
dumped 2026-10-01 from the corpus suites and each type's own grammar modules,
"Rejected" those the engine rejects). `tests/engine-corpus/known-failures.txt`
names the subphase each waits for (`grep -c 'step 7.1 '` counts 7.1's).

| Subphase | Section type | Engine owner | Sections | Rejected | Status |
|---|---|---|---|---|---|
| 7.1 | `###Persistence`: persistences and persistence contexts, with their triggers, platforms and targets (the relational target and the cloud platforms are extensions of their own) | persistence | 205 | 81 | Done: all match but one the engine rejects for a platform type no grammar reads (accepted) |
| 7.2 | `###DataSpace` | data-space | 142 | 29 | Done: all match but one the engine rejects without a position (accepted) |
| 7.3 | `###DataQualityValidation`: data quality validations, relation validations and relation comparisons | dataquality | 88 | 10 | Done: all match |
| 7.4 | `###ServiceStore`: service stores | serviceStore | 81 | 13 | Done: all match but one the engine rejects for a security scheme no grammar reads (accepted) |
| 7.5 | `###ExternalFormat`: schema sets and bindings | core (external format) | 77 | 0 | |
| 7.6 | `###Diagram` | diagram | 36 | 20 | |
| 7.7 | `###Snowflake`: Snowflake apps and M2M UDFs | snowflake | 20 | 11 | |
| 7.8 | `###Elasticsearch`: Elasticsearch 7 stores | elasticsearch | 12 | 0 | |
| 7.9 | `###MongoDB`: MongoDB stores | mongodb | 12 | 0 | |
| 7.10 | `###FileGeneration` and `###GenerationSpecification` | generation | 15 | 6 | |
| 7.11 | `###QueryPostProcessor`: relational mappers | relationalStore | 10 | 0 | |
| 7.12 | `###Deephaven`: Deephaven stores and apps | deephaven | 9 | 0 | |
| 7.13 | `###MemSql`: MemSQL functions | memsqlFunction | 7 | 4 | |
| 7.14 | `###HostedService` | hostedService | 6 | 1 | |
| 7.15 | `###BigQuery`: BigQuery functions | bigqueryFunction | 6 | 3 | |
| 7.16 | `###Text` | text | 6 | 3 | |
| 7.17 | `###FunctionJar` | functionJar | 5 | 1 | |

7.1's crates are `dsl-persistence` (the section; the `Manual` trigger and
`Default` platform), `dsl-persistence-relational` (the `Relational`
target) and `dsl-persistence-cloud` (the `AwsGlue` platform), each engine
module's. The walker builds its objects as records shaped like the engine's
protocol (the protocol crate's `record`): the protocol has some sixty classes,
each built by one walker method. Two of the walker's ways are reproduced: a
trigger's, platform's or target's island is parsed without its last two
characters (it takes `}#` off a text that doesn't have it), and a service
parameter's primitive value is parsed again from its tokens' texts joined,
so that positions after a space inside it move left.

7.2's crate is `dsl-data-space`, read and walked the same way. It also
registers the `DataspaceTestData` embedded data, a data space's data as an
execution context's test data: like the engine's parser, it reads a core
reference (to a `DATASPACE` element), which needs no protocol of its own.
The engine fails without a position on an executable that has neither a
query nor an executable; a Rust error always has one, so Rust reports it
unsupported and the engine parses the section (accepted).

7.3's crate is `dsl-dataquality`, read and walked the same way: class
validations with their validation trees (`$[ my::Class<constraint>{ … } ]$`,
whose property parameters and sub-type paths the walker leaves out),
relation validations and relation comparisons, with their test suites. A
persistence strategy, `# Type { … }#`, is unsupported: the engine reads it
with an extension of its data quality grammar, and no module of the engine
has one.

7.4's grammar joins the crate of the store's other grammars,
`dsl-service-store` (its `store` module), read and walked the same way,
its blocks in parentheses. A store's `description` is read and left out,
as the walker leaves it. A security scheme is unsupported, as the engine
reads one with an extension of its grammar and none of its modules has
one; so is an `explode` option of a parameter that isn't a list, on which
the engine fails without a message.

