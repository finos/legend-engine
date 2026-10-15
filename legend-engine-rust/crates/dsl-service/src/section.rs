// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! `SectionParser` for `###Service`: the engine's `ServiceParserGrammar` and
//! `ServiceParseTreeWalker`.
//!
//! ```text
//! definition:  (service | environment)*
//! service:     '''doc'''? Service <<stereotypes>>? {taggedValues}? my::S
//!              { ( pattern: '…'; | title: '…'; | documentation: '…';
//!                | owners: [ '…', … ]; | autoActivateUpdates: true; | mcpServer: name;
//!                | ownership: DID { identifier: '…' }; | ownership: UserList { users: [ '…', … ] };
//!                | execution: Single { query: …; mapping: my::M; runtime: runtime; }
//!                | execution: Multi { query: …; key: '…'; executions['k']: { mapping: …; runtime: … } }
//!                | testSuites: [ suite, … ] | test: legacy | postValidations: [ { … }, … ] )* }
//! runtime:     my::Runtime; | #{ mappings: [ … ]; connections: [ … ]; }#;
//! suite:       id: { data: [ connections: [ my::Connection: Type #{ … }#, … ] ]
//!                    tests: [ id: { parameters: [ p = value, … ] serializationFormat: f;
//!                                   keys: [ '…', … ]; asserts: [ id: Type #{ … }#, … ] }, … ] }
//!            | id '''doc'''? ( my::Store: Type #{ … }#; my::DataElement;
//!                              id '''doc'''? (p = value, …)? ['key', …]? (: f)? => Type #{ … }#; )
//! legacy:      Single { data: '…'; asserts: [ { [ value, list([ value, … ]), … ], assertion }, … ]; }
//!            | Multi { tests['key']: { data: '…'; asserts: [ … ]; } … }
//! environment: ExecutionEnvironment my::E { executions: [ key: single | key: [ key: single, … ], … ]; }
//! single:      { mapping: my::M; ( runtime: runtime
//!                                | runtimeComponents: { runtime: my::R; class: my::C; binding: my::B; } )+ }
//! ```
//!
//! As in the engine, the section's grammar is read whole first: each query,
//! assertion lambda and parameter value parsed, as the engine's grammar
//! parses them; embedded runtimes, data and test assertions read but not
//! parsed. Then the services are walked, then the execution environments,
//! each in the engine walker's order: its errors come after the grammar's.
//!
//! An execution of another type, `execution: Type { … }`, is read by the
//! grammar an engine extension registers for the type; Rust has none, so
//! such a service is unsupported, and the engine parses the section. The
//! engine also fails, without a position, on an execution environment that
//! mixes single and grouped executions, or whose runtime components lack
//! one: those are unsupported too.

use legend_pure_dsl_runtime::ast::ElementPath;
use legend_pure_dsl_runtime::section::parse_embedded_runtime;
use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::test_assertion::TestAssertion;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::{missing, optional, required};
use legend_pure_parser_parser::forms::{
    field_start, list, separated, string, string_field, string_text,
};
use legend_pure_parser_parser::words::{
    Name, at_word, expect_word, identifier, is_string, is_word, is_word_kind, qualified_name,
    unexpected,
};
use legend_pure_parser_parser::{Block, Island, ParserContext, SectionParser, TypedIsland};
use smol_str::SmolStr;

use crate::ast::{
    ConnectionTestData, DataResolver, Execution, ExecutionEnvironment, ExecutionParameters,
    KeyedExecutionParameter, KeyedSingleExecutionTest, LegacyTest, MultiExecutionParameters,
    Ownership, ParameterValue, PostValidation, PostValidationAssertion, Runtime, RuntimeComponents,
    SECTION_KIND, Service, ServiceTest, ServiceTestSuite, SingleExecutionParameters, TestContainer,
    TestData, TestParameter,
};

type R<T> = Result<T, ParseError>;

/// The service grammar's keywords and the engine core's (`all`, `let`, …):
/// words that are not a `VALID_STRING`, which an execution's type is.
const KEYWORDS: [&str; 46] = [
    "stereotypes",
    "tags",
    "Service",
    "import",
    "Single",
    "Multi",
    "pattern",
    "owners",
    "documentation",
    "autoActivateUpdates",
    "ownership",
    "DID",
    "identifier",
    "UserList",
    "users",
    "execution",
    "executions",
    "query",
    "key",
    "mapping",
    "runtime",
    "binding",
    "class",
    "runtimeComponents",
    "testSuites",
    "data",
    "connections",
    "tests",
    "asserts",
    "serializationFormat",
    "parameters",
    "keys",
    "list",
    "title",
    "postValidations",
    "description",
    "params",
    "assertions",
    "mcpServer",
    "test",
    "ExecutionEnvironment",
    "all",
    "let",
    "allVersions",
    "allVersionsInRange",
    "toBytes",
];

/// The type the engine's `EqualToRelation` assertions read as a relation.
const RELATION: &str = "Relation";

/// The type of the data an atomic test's expected result may be, besides a
/// relation.
const EXTERNAL_FORMAT: &str = "ExternalFormat";

/// Parser for `###Service` sections.
pub struct ServiceSectionParser;

impl SectionParser for ServiceSectionParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        let (mut services, mut environments) = (Vec::new(), Vec::new());
        while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
            let read = if at_word(ctx, "ExecutionEnvironment") {
                read_environment(ctx).map(|environment| environments.push(environment))
            } else {
                read_service(ctx).map(|service| services.push(service))
            };
            if let Err(error) = read {
                errors.push(error);
                return Vec::new();
            }
        }
        // The engine walks the services, then the execution environments.
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for service in services {
            match walk_service(ctx, service) {
                Ok(service) => elements.push(Box::new(service)),
                // The engine reports the first error.
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        for environment in environments {
            match walk_environment(ctx, environment) {
                Ok(environment) => elements.push(Box::new(environment)),
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        elements
    }
}

// ── The grammar: the section read, its islands not parsed yet ───────────

/// A service as its grammar reads it: each field as often as written.
struct ReadService {
    documentation: Option<Token>,
    stereotypes: Vec<StereotypePtr>,
    tagged_values: Vec<TaggedValue>,
    package: Option<Package>,
    name: SpannedString,
    patterns: Vec<Token>,
    titles: Vec<Token>,
    owners: Vec<Vec<Token>>,
    ownerships: Vec<ReadOwnership>,
    documentations: Vec<Token>,
    auto_activate_updates: Vec<Token>,
    executions: Vec<ReadExecution>,
    test_suites: Vec<Vec<ReadSuite>>,
    tests: Vec<ReadLegacyTest>,
    post_validations: Vec<Vec<ReadPostValidation>>,
    mcp_servers: Vec<(Name, SourceInfo)>,
    source_info: SourceInfo,
}

enum ReadOwnership {
    Deployment(Token),
    Users(Vec<Token>),
}

enum ReadExecution {
    Single {
        queries: Vec<Expression>,
        mappings: Vec<ElementPath>,
        runtimes: Vec<ReadRuntime>,
        source_info: SourceInfo,
    },
    Multi {
        queries: Vec<Expression>,
        keys: Vec<Token>,
        parameters: Vec<ReadKeyedParameter>,
        source_info: SourceInfo,
    },
    /// `Type { … }`, another type's.
    Extension {
        type_name: Token,
        source_info: SourceInfo,
    },
}

struct ReadKeyedParameter {
    key: Token,
    mappings: Vec<ElementPath>,
    runtimes: Vec<ReadRuntime>,
    source_info: SourceInfo,
}

enum ReadRuntime {
    Pointer(ElementPath),
    /// The island, and its span to the `;` after it.
    Embedded(Island, SourceInfo),
}

enum ReadSuite {
    /// `id: { data: [ … ] tests: [ … ] }`.
    Fields {
        id: Name,
        data: Vec<ReadSuiteData>,
        tests: Vec<Vec<ReadTest>>,
        source_info: SourceInfo,
    },
    /// `id 'doc'? ( statements )`.
    Flat {
        id: Name,
        doc: Option<Token>,
        resolvers: Vec<ReadResolver>,
        tests: Vec<ReadAtomicTest>,
        /// From the documentation, or `(`, to `)`.
        body: SourceInfo,
        source_info: SourceInfo,
    },
}

/// `data: [ connections: [ … ] … ]`.
struct ReadSuiteData {
    connections: Vec<Vec<ReadConnectionData>>,
    source_info: SourceInfo,
}

struct ReadConnectionData {
    connection: String,
    data: TypedIsland,
    source_info: SourceInfo,
}

enum ReadResolver {
    Base {
        element: ElementPath,
        data: Box<TypedIsland>,
        source_info: SourceInfo,
    },
    Reference(ElementPath),
}

/// `id: { parameters: … serializationFormat: …; keys: …; asserts: … }`.
struct ReadTest {
    id: Name,
    parameters: Vec<Vec<ParameterValue>>,
    serialization_formats: Vec<Name>,
    keys: Vec<Vec<Token>>,
    asserts: Vec<Vec<(Name, TypedIsland)>>,
    source_info: SourceInfo,
}

/// `id 'doc'? (params)? [keys]? (: format)? => Type #{ … }#;`.
struct ReadAtomicTest {
    id: Name,
    doc: Option<Token>,
    parameters: Vec<ParameterValue>,
    keys: Vec<Token>,
    serialization_format: Option<Name>,
    expected: TypedIsland,
    source_info: SourceInfo,
}

enum ReadLegacyTest {
    Single(ReadLegacyFields),
    Multi {
        tests: Vec<(Token, ReadLegacyFields)>,
        source_info: SourceInfo,
    },
}

/// `{ data: '…'; asserts: [ … ]; }`, and from what it starts with (`Single`
/// or `tests`) to `}`.
struct ReadLegacyFields {
    data: Vec<Token>,
    asserts: Vec<Vec<TestContainer>>,
    source_info: SourceInfo,
}

struct ReadPostValidation {
    descriptions: Vec<Token>,
    parameters: Vec<Vec<Expression>>,
    assertions: Vec<Vec<PostValidationAssertion>>,
    source_info: SourceInfo,
}

/// An execution environment as its grammar reads it.
struct ReadEnvironment {
    package: Option<Package>,
    name: SpannedString,
    executions: Vec<ReadExecutionParameters>,
    source_info: SourceInfo,
}

enum ReadExecutionParameters {
    Single(ReadSingleParameters),
    Multi(Name, Vec<ReadSingleParameters>),
}

/// `key: { mapping: …; runtime: …; runtimeComponents: { … } }`.
struct ReadSingleParameters {
    key: Name,
    mapping: ElementPath,
    runtimes: Vec<ReadRuntime>,
    runtime_components: Vec<ReadRuntimeComponents>,
    source_info: SourceInfo,
}

/// `{ runtime: …; class: …; binding: …; }`, each as often as written.
struct ReadRuntimeComponents {
    runtimes: Vec<ElementPath>,
    classes: Vec<ElementPath>,
    bindings: Vec<ElementPath>,
}

/// `'''doc''' Service <<s>> {t} my::S { … }`.
fn read_service(ctx: &mut ParserContext<'_>) -> R<ReadService> {
    let start = ctx.cursor().current_source_info();
    let documentation = is_string(ctx.cursor().peek()).then(|| ctx.cursor().advance().clone());
    expect_word(ctx, "Service")?;
    let stereotypes = if ctx.cursor().check(TokenKind::LessLess) {
        ctx.parse_stereotypes()?
    } else {
        Vec::new()
    };
    let tagged_values = if ctx.cursor().check(TokenKind::LBrace) {
        ctx.parse_tagged_values()?
    } else {
        Vec::new()
    };
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    ctx.cursor().expect(TokenKind::LBrace)?;
    let mut read = ReadService {
        documentation,
        stereotypes,
        tagged_values,
        package,
        name: SpannedString {
            value: name,
            source_info: name_source_info,
        },
        patterns: Vec::new(),
        titles: Vec::new(),
        owners: Vec::new(),
        ownerships: Vec::new(),
        documentations: Vec::new(),
        auto_activate_updates: Vec::new(),
        executions: Vec::new(),
        test_suites: Vec::new(),
        tests: Vec::new(),
        post_validations: Vec::new(),
        mcp_servers: Vec::new(),
        source_info: start.clone(),
    };
    while !ctx.cursor().check(TokenKind::RBrace) {
        let field = ctx.cursor().peek().clone();
        let name = if is_word_kind(field.kind) {
            field.text.as_str()
        } else {
            ""
        };
        match name {
            "pattern" => read.patterns.push(string_field(ctx)?),
            "title" => read.titles.push(string_field(ctx)?),
            "documentation" => read.documentations.push(string_field(ctx)?),
            "owners" => {
                field_start(ctx)?;
                read.owners.push(list(ctx, string)?);
                ctx.cursor().expect(TokenKind::Semicolon)?;
            }
            "ownership" => {
                field_start(ctx)?;
                read.ownerships.push(ownership(ctx)?);
                ctx.cursor().expect(TokenKind::Semicolon)?;
            }
            "autoActivateUpdates" => {
                field_start(ctx)?;
                let value = ctx.cursor().peek().clone();
                if !matches!(value.kind, TokenKind::True | TokenKind::False) {
                    return Err(unexpected(&value));
                }
                ctx.cursor().advance();
                read.auto_activate_updates.push(value);
                ctx.cursor().expect(TokenKind::Semicolon)?;
            }
            "mcpServer" => {
                field_start(ctx)?;
                let server = identifier(ctx)?;
                ctx.cursor().expect(TokenKind::Semicolon)?;
                read.mcp_servers
                    .push((server, ctx.cursor().span_since(&field.source_info)));
            }
            "execution" => {
                field_start(ctx)?;
                read.executions.push(execution(ctx)?);
            }
            "testSuites" => {
                field_start(ctx)?;
                read.test_suites.push(list(ctx, suite)?);
            }
            "test" => {
                field_start(ctx)?;
                read.tests.push(legacy_test(ctx)?);
            }
            "postValidations" => {
                field_start(ctx)?;
                read.post_validations.push(list(ctx, post_validation)?);
            }
            _ => return Err(unexpected(&field)),
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    read.source_info = ctx.cursor().span_since(&start);
    Ok(read)
}

/// A path, unquoted.
fn element_path(ctx: &mut ParserContext<'_>) -> R<ElementPath> {
    let name = qualified_name(ctx)?;
    Ok(ElementPath {
        path: name.path,
        source_info: name.source_info,
    })
}

/// `field: my::Path;`'s path.
fn path_field(ctx: &mut ParserContext<'_>) -> R<ElementPath> {
    field_start(ctx)?;
    let path = element_path(ctx)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok(path)
}

/// `DID { identifier: '…' }` or `UserList { users: [ '…', … ] }`.
fn ownership(ctx: &mut ParserContext<'_>) -> R<ReadOwnership> {
    let kind = ctx.cursor().peek().clone();
    let ownership = if is_word(&kind, "DID") {
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::LBrace)?;
        expect_word(ctx, "identifier")?;
        ctx.cursor().expect(TokenKind::Colon)?;
        ReadOwnership::Deployment(string(ctx)?)
    } else if is_word(&kind, "UserList") {
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::LBrace)?;
        expect_word(ctx, "users")?;
        ctx.cursor().expect(TokenKind::Colon)?;
        ReadOwnership::Users(list(ctx, string)?)
    } else {
        return Err(unexpected(&kind));
    };
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ownership)
}

/// A field's value, `combinedExpression`: an expression before the field's
/// `;`, which a lambda's body doesn't take.
fn expression_until(ctx: &mut ParserContext<'_>, terminator: TokenKind) -> R<Expression> {
    let value = ctx.read_until(terminator)?;
    expression_block(ctx, &value)
}

/// An expression read as a block, which it must be whole.
// `ParserContext::parse_expression` itself is not general enough over the
// block's context lifetime, so the closure stays.
#[allow(clippy::redundant_closure_for_method_calls)]
fn expression_block(ctx: &mut ParserContext<'_>, block: &Block) -> R<Expression> {
    ctx.parse_block(block, |block| block.parse_expression())
}

/// `query: …;`'s expression.
fn query(ctx: &mut ParserContext<'_>) -> R<Expression> {
    field_start(ctx)?;
    let query = expression_until(ctx, TokenKind::Semicolon)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok(query)
}

/// `Single { … }`, `Multi { … }`, or `Type { … }`, another type's.
fn execution(ctx: &mut ParserContext<'_>) -> R<ReadExecution> {
    let kind = ctx.cursor().peek().clone();
    if is_word(&kind, "Single") {
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::LBrace)?;
        let (mut queries, mut mappings, mut runtimes) = (Vec::new(), Vec::new(), Vec::new());
        loop {
            let field = ctx.cursor().peek().clone();
            if is_word(&field, "query") {
                queries.push(query(ctx)?);
            } else if is_word(&field, "mapping") {
                mappings.push(path_field(ctx)?);
            } else if is_word(&field, "runtime") {
                field_start(ctx)?;
                runtimes.push(runtime(ctx)?);
            } else {
                break;
            }
        }
        ctx.cursor().expect(TokenKind::RBrace)?;
        return Ok(ReadExecution::Single {
            queries,
            mappings,
            runtimes,
            source_info: ctx.cursor().span_since(&kind.source_info),
        });
    }
    if is_word(&kind, "Multi") {
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::LBrace)?;
        let (mut queries, mut keys, mut parameters) = (Vec::new(), Vec::new(), Vec::new());
        loop {
            let field = ctx.cursor().peek().clone();
            if is_word(&field, "query") {
                queries.push(query(ctx)?);
            } else if is_word(&field, "key") {
                keys.push(string_field(ctx)?);
            } else if is_word(&field, "executions") {
                parameters.push(keyed_parameter(ctx)?);
            } else {
                break;
            }
        }
        ctx.cursor().expect(TokenKind::RBrace)?;
        return Ok(ReadExecution::Multi {
            queries,
            keys,
            parameters,
            source_info: ctx.cursor().span_since(&kind.source_info),
        });
    }
    if !is_word_kind(kind.kind) || KEYWORDS.contains(&kind.text.as_str()) {
        return Err(unexpected(&kind));
    }
    ctx.cursor().advance();
    ctx.read_block()?;
    Ok(ReadExecution::Extension {
        source_info: ctx.cursor().span_since(&kind.source_info),
        type_name: kind,
    })
}

/// `executions['key']: { mapping: …; runtime: …; }`.
fn keyed_parameter(ctx: &mut ParserContext<'_>) -> R<ReadKeyedParameter> {
    let start = ctx.cursor().advance().source_info.clone();
    ctx.cursor().expect(TokenKind::LBracket)?;
    let key = string(ctx)?;
    ctx.cursor().expect(TokenKind::RBracket)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    ctx.cursor().expect(TokenKind::LBrace)?;
    let (mut mappings, mut runtimes) = (Vec::new(), Vec::new());
    loop {
        let field = ctx.cursor().peek().clone();
        if is_word(&field, "mapping") {
            mappings.push(path_field(ctx)?);
        } else if is_word(&field, "runtime") {
            field_start(ctx)?;
            runtimes.push(runtime(ctx)?);
        } else {
            break;
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ReadKeyedParameter {
        key,
        mappings,
        runtimes,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `my::Runtime;` or `#{ … }#;`.
fn runtime(ctx: &mut ParserContext<'_>) -> R<ReadRuntime> {
    if ctx.cursor().check(TokenKind::IslandOpen) {
        let island = ctx.read_island()?;
        ctx.cursor().expect(TokenKind::Semicolon)?;
        let source_info = ctx.cursor().span_since(&island.source_info);
        return Ok(ReadRuntime::Embedded(island, source_info));
    }
    let path = element_path(ctx)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok(ReadRuntime::Pointer(path))
}

/// `id: { … }` or `id 'doc'? ( … )`.
fn suite(ctx: &mut ParserContext<'_>) -> R<ReadSuite> {
    let id = identifier(ctx)?;
    if ctx.cursor().eat(TokenKind::Colon) {
        ctx.cursor().expect(TokenKind::LBrace)?;
        let (mut data, mut tests) = (Vec::new(), Vec::new());
        loop {
            let field = ctx.cursor().peek().clone();
            if is_word(&field, "data") {
                field_start(ctx)?;
                ctx.cursor().expect(TokenKind::LBracket)?;
                let mut connections = Vec::new();
                while at_word(ctx, "connections") {
                    field_start(ctx)?;
                    connections.push(list(ctx, connection_data)?);
                }
                ctx.cursor().expect(TokenKind::RBracket)?;
                data.push(ReadSuiteData {
                    connections,
                    source_info: ctx.cursor().span_since(&field.source_info),
                });
            } else if is_word(&field, "tests") {
                field_start(ctx)?;
                tests.push(list(ctx, test)?);
            } else {
                break;
            }
        }
        ctx.cursor().expect(TokenKind::RBrace)?;
        return Ok(ReadSuite::Fields {
            source_info: ctx.cursor().span_since(&id.source_info),
            id,
            data,
            tests,
        });
    }
    let body = ctx.cursor().current_source_info();
    let doc = is_string(ctx.cursor().peek()).then(|| ctx.cursor().advance().clone());
    ctx.cursor().expect(TokenKind::LParen)?;
    let (mut resolvers, mut tests) = (Vec::new(), Vec::new());
    while !ctx.cursor().check(TokenKind::RParen) {
        if at_data_statement(ctx) {
            resolvers.push(resolver(ctx)?);
        } else {
            tests.push(atomic_test(ctx)?);
        }
    }
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(ReadSuite::Flat {
        body: ctx.cursor().span_since(&body),
        source_info: ctx.cursor().span_since(&id.source_info),
        id,
        doc,
        resolvers,
        tests,
    })
}

/// `my::Connection: Type #{ … }#`.
fn connection_data(ctx: &mut ParserContext<'_>) -> R<ReadConnectionData> {
    let connection = qualified_name(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let data = ctx.read_typed_island()?;
    Ok(ReadConnectionData {
        source_info: ctx.cursor().span_since(&connection.source_info),
        connection: connection.path,
        data,
    })
}

/// `id: { parameters: [ … ] serializationFormat: …; keys: [ … ]; asserts: [ … ] }`.
fn test(ctx: &mut ParserContext<'_>) -> R<ReadTest> {
    let id = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    ctx.cursor().expect(TokenKind::LBrace)?;
    let mut read = ReadTest {
        id,
        parameters: Vec::new(),
        serialization_formats: Vec::new(),
        keys: Vec::new(),
        asserts: Vec::new(),
        source_info: SourceInfo::new("", 0, 0, 0, 0),
    };
    loop {
        let field = ctx.cursor().peek().clone();
        if is_word(&field, "parameters") {
            field_start(ctx)?;
            read.parameters.push(list(ctx, parameter)?);
        } else if is_word(&field, "serializationFormat") {
            field_start(ctx)?;
            read.serialization_formats.push(identifier(ctx)?);
            ctx.cursor().expect(TokenKind::Semicolon)?;
        } else if is_word(&field, "keys") {
            field_start(ctx)?;
            ctx.cursor().expect(TokenKind::LBracket)?;
            read.keys.push(separated(ctx, string)?);
            ctx.cursor().expect(TokenKind::RBracket)?;
            ctx.cursor().expect(TokenKind::Semicolon)?;
        } else if is_word(&field, "asserts") {
            field_start(ctx)?;
            read.asserts.push(list(ctx, |ctx| {
                let id = identifier(ctx)?;
                ctx.cursor().expect(TokenKind::Colon)?;
                Ok((id, ctx.read_typed_island()?))
            })?);
        } else {
            break;
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    read.source_info = ctx.cursor().span_since(&read.id.source_info);
    Ok(read)
}

/// `name = value`.
fn parameter(ctx: &mut ParserContext<'_>) -> R<ParameterValue> {
    let name = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Equals)?;
    Ok(ParameterValue {
        name: name.value,
        value: primitive_value(ctx)?,
    })
}

/// A literal, an enum value, bytes, or a collection of them.
fn primitive_value(ctx: &mut ParserContext<'_>) -> R<Expression> {
    ctx.parse_primitive_value()
}

/// Whether a data statement is next: `my::Store: Type #{ … }#;` or
/// `my::DataElement;`, rather than a test, `id … => …;`, whose id is a
/// single name.
fn at_data_statement(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    let mut next = 1;
    while cursor.peek_kind_at(next) == TokenKind::PathSep {
        next += 2;
    }
    if next > 1 {
        return true;
    }
    match cursor.peek_kind_at(next) {
        TokenKind::Semicolon => true,
        TokenKind::Colon => cursor.peek_kind_at(next + 2) == TokenKind::IslandOpen,
        _ => false,
    }
}

/// `my::Store: Type #{ … }#;` or `my::DataElement;`.
fn resolver(ctx: &mut ParserContext<'_>) -> R<ReadResolver> {
    let element = element_path(ctx)?;
    let resolver = if ctx.cursor().eat(TokenKind::Colon) {
        let data = Box::new(ctx.read_typed_island()?);
        ReadResolver::Base {
            source_info: ctx.cursor().span_since(&element.source_info),
            element,
            data,
        }
    } else {
        ReadResolver::Reference(element)
    };
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok(resolver)
}

/// `id 'doc'? (p = value, …)? ['key', …]? (: format)? => Type #{ … }#;`.
fn atomic_test(ctx: &mut ParserContext<'_>) -> R<ReadAtomicTest> {
    let id = identifier(ctx)?;
    let doc = is_string(ctx.cursor().peek()).then(|| ctx.cursor().advance().clone());
    let mut parameters = Vec::new();
    if ctx.cursor().eat(TokenKind::LParen) {
        parameters = separated(ctx, parameter)?;
        ctx.cursor().expect(TokenKind::RParen)?;
    }
    let mut keys = Vec::new();
    if ctx.cursor().eat(TokenKind::LBracket) {
        keys = separated(ctx, string)?;
        ctx.cursor().expect(TokenKind::RBracket)?;
    }
    let serialization_format = if ctx.cursor().eat(TokenKind::Colon) {
        Some(identifier(ctx)?)
    } else {
        None
    };
    // `=` then `>`, which a lexer of Pure reads as one token when adjacent.
    if !ctx.cursor().eat(TokenKind::FatArrow) {
        ctx.cursor().expect(TokenKind::Equals)?;
        ctx.cursor().expect(TokenKind::Greater)?;
    }
    let expected = ctx.read_typed_island()?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok(ReadAtomicTest {
        source_info: ctx.cursor().span_since(&id.source_info),
        id,
        doc,
        parameters,
        keys,
        serialization_format,
        expected,
    })
}

/// `Single { … }` or `Multi { tests['key']: { … } … }`.
fn legacy_test(ctx: &mut ParserContext<'_>) -> R<ReadLegacyTest> {
    let kind = ctx.cursor().peek().clone();
    if is_word(&kind, "Single") {
        ctx.cursor().advance();
        return Ok(ReadLegacyTest::Single(legacy_fields(ctx, &kind)?));
    }
    if !is_word(&kind, "Multi") {
        return Err(unexpected(&kind));
    }
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::LBrace)?;
    let mut tests = Vec::new();
    while at_word(ctx, "tests") {
        let start = ctx.cursor().advance().clone();
        ctx.cursor().expect(TokenKind::LBracket)?;
        let key = string(ctx)?;
        ctx.cursor().expect(TokenKind::RBracket)?;
        ctx.cursor().expect(TokenKind::Colon)?;
        tests.push((key, legacy_fields(ctx, &start)?));
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ReadLegacyTest::Multi {
        tests,
        source_info: ctx.cursor().span_since(&kind.source_info),
    })
}

/// `{ data: '…'; asserts: [ … ]; }`, `start` what it follows.
fn legacy_fields(ctx: &mut ParserContext<'_>, start: &Token) -> R<ReadLegacyFields> {
    ctx.cursor().expect(TokenKind::LBrace)?;
    let (mut data, mut asserts) = (Vec::new(), Vec::new());
    loop {
        let field = ctx.cursor().peek().clone();
        if is_word(&field, "data") {
            data.push(string_field(ctx)?);
        } else if is_word(&field, "asserts") {
            field_start(ctx)?;
            asserts.push(list(ctx, test_container)?);
            ctx.cursor().expect(TokenKind::Semicolon)?;
        } else {
            break;
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ReadLegacyFields {
        data,
        asserts,
        source_info: ctx.cursor().span_since(&start.source_info),
    })
}

/// `{ [ value, list([ value, … ]), … ], assertion }`.
fn test_container(ctx: &mut ParserContext<'_>) -> R<TestContainer> {
    let start = ctx.cursor().expect(TokenKind::LBrace)?;
    let parameters = list(ctx, |ctx| {
        if at_word(ctx, "list") && ctx.cursor().peek_kind_at(1) == TokenKind::LParen {
            let list_start = ctx.cursor().advance().source_info.clone();
            ctx.cursor().advance();
            let values = list(ctx, primitive_value)?;
            ctx.cursor().expect(TokenKind::RParen)?;
            return Ok(TestParameter::List {
                values,
                source_info: ctx.cursor().span_since(&list_start),
            });
        }
        Ok(TestParameter::Value(Box::new(primitive_value(ctx)?)))
    })?;
    ctx.cursor().expect(TokenKind::Comma)?;
    let assertion = expression_until(ctx, TokenKind::RBrace)?;
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(TestContainer {
        parameters,
        assertion,
        source_info: ctx.cursor().span_since(&start.source_info),
    })
}

/// `{ description: '…'; params: [ … ]; assertions: [ id: …, … ]; }`.
fn post_validation(ctx: &mut ParserContext<'_>) -> R<ReadPostValidation> {
    let start = ctx.cursor().expect(TokenKind::LBrace)?;
    let mut read = ReadPostValidation {
        descriptions: Vec::new(),
        parameters: Vec::new(),
        assertions: Vec::new(),
        source_info: start.source_info.clone(),
    };
    loop {
        let field = ctx.cursor().peek().clone();
        if is_word(&field, "description") {
            read.descriptions.push(string_field(ctx)?);
        } else if is_word(&field, "params") {
            field_start(ctx)?;
            read.parameters
                .push(list(ctx, |ctx| expression_until(ctx, TokenKind::Comma))?);
            ctx.cursor().expect(TokenKind::Semicolon)?;
        } else if is_word(&field, "assertions") {
            field_start(ctx)?;
            read.assertions.push(list(ctx, |ctx| {
                let id = identifier(ctx)?;
                ctx.cursor().expect(TokenKind::Colon)?;
                let assertion = expression_until(ctx, TokenKind::Comma)?;
                Ok(PostValidationAssertion {
                    source_info: ctx.cursor().span_since(&id.source_info),
                    id: id.value,
                    assertion,
                })
            })?);
            ctx.cursor().expect(TokenKind::Semicolon)?;
        } else {
            break;
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    read.source_info = ctx.cursor().span_since(&start.source_info);
    Ok(read)
}

/// `ExecutionEnvironment my::E { executions: [ … ]; }`.
fn read_environment(ctx: &mut ParserContext<'_>) -> R<ReadEnvironment> {
    let start = ctx.cursor().advance().source_info.clone();
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    ctx.cursor().expect(TokenKind::LBrace)?;
    expect_word(ctx, "executions")?;
    ctx.cursor().expect(TokenKind::Colon)?;
    ctx.cursor().expect(TokenKind::LBracket)?;
    let executions = separated(ctx, |ctx| {
        let key = identifier(ctx)?;
        ctx.cursor().expect(TokenKind::Colon)?;
        if ctx.cursor().eat(TokenKind::LBracket) {
            let executions = separated(ctx, |ctx| {
                let key = identifier(ctx)?;
                ctx.cursor().expect(TokenKind::Colon)?;
                single_parameters(ctx, key)
            })?;
            ctx.cursor().expect(TokenKind::RBracket)?;
            return Ok(ReadExecutionParameters::Multi(key, executions));
        }
        Ok(ReadExecutionParameters::Single(single_parameters(
            ctx, key,
        )?))
    })?;
    ctx.cursor().expect(TokenKind::RBracket)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ReadEnvironment {
        package,
        name: SpannedString {
            value: name,
            source_info: name_source_info,
        },
        executions,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `{ mapping: …; (runtime: … | runtimeComponents: { … })+ }`, after
/// `key:`.
fn single_parameters(ctx: &mut ParserContext<'_>, key: Name) -> R<ReadSingleParameters> {
    ctx.cursor().expect(TokenKind::LBrace)?;
    if !at_word(ctx, "mapping") {
        return Err(unexpected(ctx.cursor().peek()));
    }
    let mapping = path_field(ctx)?;
    let (mut runtimes, mut runtime_components) = (Vec::new(), Vec::new());
    loop {
        let field = ctx.cursor().peek().clone();
        if is_word(&field, "runtime") {
            field_start(ctx)?;
            runtimes.push(runtime(ctx)?);
        } else if is_word(&field, "runtimeComponents") {
            field_start(ctx)?;
            runtime_components.push(components(ctx)?);
        } else if runtimes.is_empty() && runtime_components.is_empty() {
            return Err(unexpected(&field));
        } else {
            break;
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ReadSingleParameters {
        source_info: ctx.cursor().span_since(&key.source_info),
        key,
        mapping,
        runtimes,
        runtime_components,
    })
}

/// `{ runtime: my::R; class: my::C; binding: my::B; }`, each positioned
/// from its field to its `;`.
fn components(ctx: &mut ParserContext<'_>) -> R<ReadRuntimeComponents> {
    ctx.cursor().expect(TokenKind::LBrace)?;
    let mut read = ReadRuntimeComponents {
        runtimes: Vec::new(),
        classes: Vec::new(),
        bindings: Vec::new(),
    };
    loop {
        let field = ctx.cursor().peek().clone();
        let components = if is_word(&field, "runtime") {
            &mut read.runtimes
        } else if is_word(&field, "class") {
            &mut read.classes
        } else if is_word(&field, "binding") {
            &mut read.bindings
        } else {
            break;
        };
        let path = path_field(ctx)?;
        components.push(ElementPath {
            path: path.path,
            source_info: ctx.cursor().span_since(&field.source_info),
        });
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(read)
}

// ── The walk: each service's parts, in the engine walker's order ────────

/// The walker's `visitService`.
fn walk_service(ctx: &mut ParserContext<'_>, read: ReadService) -> R<Service> {
    let source_info = read.source_info;
    let tagged_values = ParserContext::with_documentation(read.documentation, read.tagged_values)?;
    let pattern = string_text(&required(read.patterns, "pattern", &source_info)?)?;
    let documentation = string_text(&required(
        read.documentations,
        "documentation",
        &source_info,
    )?)?;
    let title = optional(read.titles, "title", &source_info)?
        .map(|title| string_text(&title))
        .transpose()?;
    let auto_activate_updates = optional(
        read.auto_activate_updates,
        "autoActivateUpdates",
        &source_info,
    )?
    .is_some_and(|value| value.kind == TokenKind::True);
    let owners = optional(read.owners, "owners", &source_info)?
        .unwrap_or_default()
        .iter()
        .map(string_text)
        .collect::<R<_>>()?;
    let ownership = optional(read.ownerships, "ownership", &source_info)?
        .map(|ownership| match ownership {
            ReadOwnership::Deployment(id) => {
                Ok::<_, ParseError>(Ownership::Deployment(string_text(&id)?))
            }
            ReadOwnership::Users(users) => Ok(Ownership::Users(
                users.iter().map(string_text).collect::<R<_>>()?,
            )),
        })
        .transpose()?;
    let execution = walk_execution(ctx, required(read.executions, "execution", &source_info)?)?;
    let test_suites = optional(read.test_suites, "testSuites", &source_info)?
        .map(|suites| {
            suites
                .into_iter()
                .map(|suite| walk_suite(ctx, suite))
                .collect::<R<Vec<_>>>()
        })
        .transpose()?;
    let test = optional(read.tests, "test", &source_info)?
        .map(walk_legacy_test)
        .transpose()?;
    let post_validations = optional(read.post_validations, "postValidation", &source_info)?
        .unwrap_or_default()
        .into_iter()
        .map(walk_post_validation)
        .collect::<R<_>>()?;
    let mcp_server = optional(read.mcp_servers, "mcpServer", &source_info)?
        .map(|(server, at)| {
            let mut chars = server.value.chars();
            let valid = chars
                .next()
                .is_some_and(|c| c.is_ascii_alphabetic() || c == '_')
                && chars.all(|c| c.is_ascii_alphanumeric() || c == '_');
            if valid {
                Ok(server.value)
            } else {
                Err(ParseError::Unexpected {
                    message:
                        "Service mcpServer should be a valid identifier ([a-zA-Z_][a-zA-Z0-9_]*)"
                            .to_string(),
                    source_info: at,
                })
            }
        })
        .transpose()?;
    Ok(Service {
        package: read.package,
        name: read.name,
        stereotypes: read.stereotypes,
        tagged_values,
        pattern,
        title,
        owners,
        ownership,
        documentation,
        auto_activate_updates,
        execution,
        test_suites,
        test,
        post_validations,
        mcp_server,
        source_info,
    })
}

/// The walker's `visitExecution`: the query first, then the mapping and
/// the runtime, which come together.
fn walk_execution(ctx: &mut ParserContext<'_>, read: ReadExecution) -> R<Execution> {
    match read {
        ReadExecution::Single {
            queries,
            mappings,
            runtimes,
            source_info,
        } => {
            let query = required(queries, "query", &source_info)?;
            let mapping = optional(mappings, "mapping", &source_info)?;
            let runtime = optional(runtimes, "runtime", &source_info)?;
            let mapping_runtime = match (mapping, runtime) {
                (Some(mapping), Some(runtime)) => Some((mapping, walk_runtime(ctx, runtime)?)),
                (None, None) => None,
                (None, Some(_)) => return Err(missing("mapping", &source_info)),
                (Some(_), None) => return Err(missing("runtime", &source_info)),
            };
            Ok(Execution::Single {
                query,
                mapping_runtime,
                source_info,
            })
        }
        ReadExecution::Multi {
            queries,
            keys,
            parameters,
            source_info,
        } => {
            let query = required(queries, "query", &source_info)?;
            let key = optional(keys, "key", &source_info)?;
            if !parameters.is_empty() && key.is_none() {
                return Err(missing("key", &source_info));
            }
            let parameters = parameters
                .into_iter()
                .map(|parameter| {
                    let source_info = parameter.source_info;
                    let mapping = required(parameter.mappings, "mapping", &source_info)?;
                    let runtime = required(parameter.runtimes, "runtime", &source_info)?;
                    Ok(KeyedExecutionParameter {
                        key: string_text(&parameter.key)?,
                        mapping,
                        runtime: walk_runtime(ctx, runtime)?,
                        source_info,
                    })
                })
                .collect::<R<_>>()?;
            Ok(Execution::Multi {
                query,
                key: key.map(|key| string_text(&key)).transpose()?,
                parameters,
                source_info,
            })
        }
        ReadExecution::Extension {
            type_name,
            source_info,
        } => Err(ParseError::Unsupported {
            message: format!(
                "no service execution grammar is linked for '{}'; the engine's extensions may \
                 read it",
                type_name.text
            ),
            source_info,
        }),
    }
}

/// The walker's `visitRuntime`: a pointer, or a runtime of its own.
fn walk_runtime(ctx: &mut ParserContext<'_>, read: ReadRuntime) -> R<Runtime> {
    Ok(match read {
        ReadRuntime::Pointer(path) => Runtime::Pointer(path),
        ReadRuntime::Embedded(island, at) => {
            Runtime::Embedded(parse_embedded_runtime(ctx, &island, &at)?)
        }
    })
}

/// The walker's `visitServiceTestSuite`.
fn walk_suite(ctx: &mut ParserContext<'_>, read: ReadSuite) -> R<ServiceTestSuite> {
    match read {
        ReadSuite::Fields {
            id,
            data,
            tests,
            source_info,
        } => {
            let test_data = optional(data, "data", &source_info)?
                .map(|data| {
                    let connections = optional(data.connections, "connections", &data.source_info)?
                        .map(|connections| {
                            connections
                                .into_iter()
                                .map(|connection| {
                                    Ok(ConnectionTestData {
                                        connection: connection.connection,
                                        data: ctx.parse_embedded_data_island(&connection.data)?,
                                        source_info: connection.source_info,
                                    })
                                })
                                .collect::<R<Vec<_>>>()
                        })
                        .transpose()?;
                    Ok::<_, ParseError>(TestData::Connections {
                        connections,
                        source_info: data.source_info,
                    })
                })
                .transpose()?;
            let tests = required(tests, "tests", &source_info)?
                .into_iter()
                .map(|test| walk_test(ctx, test))
                .collect::<R<_>>()?;
            Ok(ServiceTestSuite {
                id: id.value,
                doc: None,
                test_data,
                tests,
                source_info,
            })
        }
        ReadSuite::Flat {
            id,
            doc,
            resolvers,
            tests,
            body,
            source_info,
        } => {
            let doc = doc.map(|doc| string_text(&doc)).transpose()?;
            let resolvers = resolvers
                .into_iter()
                .map(|resolver| {
                    Ok(match resolver {
                        ReadResolver::Base {
                            element,
                            data,
                            source_info,
                        } => DataResolver::Base {
                            element,
                            data: ctx.parse_embedded_data_island(&data)?,
                            source_info,
                        },
                        ReadResolver::Reference(element) => DataResolver::Reference(element),
                    })
                })
                .collect::<R<Vec<_>>>()?;
            let test_data = (!resolvers.is_empty()).then_some(TestData::Resolvers {
                resolvers,
                source_info: body,
            });
            let tests = tests
                .into_iter()
                .map(|test| walk_atomic_test(ctx, test))
                .collect::<R<_>>()?;
            Ok(ServiceTestSuite {
                id: id.value,
                doc,
                test_data,
                tests,
                source_info,
            })
        }
    }
}

/// The walker's `visitServiceTest`.
fn walk_test(ctx: &mut ParserContext<'_>, read: ReadTest) -> R<ServiceTest> {
    let source_info = read.source_info;
    let serialization_format = optional(
        read.serialization_formats,
        "serializationFormat",
        &source_info,
    )?;
    let parameters = optional(read.parameters, "parameters", &source_info)?;
    let keys = optional(read.keys, "keys", &source_info)?
        .unwrap_or_default()
        .iter()
        .map(string_text)
        .collect::<R<_>>()?;
    let assertions = required(read.asserts, "asserts", &source_info)?
        .into_iter()
        .map(|(id, island)| Ok((id.value, ctx.parse_test_assertion(&island)?)))
        .collect::<R<_>>()?;
    Ok(ServiceTest {
        id: read.id.value,
        doc: None,
        parameters,
        keys,
        serialization_format: serialization_format.map(|format| format.value),
        assertions,
        source_info,
    })
}

/// The walker's `visitServiceAtomicTest`: the expected result is a relation,
/// or external format data, which the test asserts equal to its result.
fn walk_atomic_test(ctx: &mut ParserContext<'_>, read: ReadAtomicTest) -> R<ServiceTest> {
    let doc = read.doc.map(|doc| string_text(&doc)).transpose()?;
    let keys = read.keys.iter().map(string_text).collect::<R<_>>()?;
    let expected = &read.expected;
    let assertion = if expected.type_name == RELATION {
        ctx.parse_test_assertion(expected)?
    } else {
        let data = ctx.parse_embedded_data_island(expected)?;
        if data.type_name != EXTERNAL_FORMAT {
            return Err(ParseError::Unexpected {
                message: format!(
                    "Service atomic test '=> ...' expects Relation or ExternalFormat embedded \
                     data as the expected result; got: {} data",
                    data.type_name
                ),
                source_info: expected.source_info.clone(),
            });
        }
        TestAssertion::EqualToJson {
            expected: data,
            source_info: expected.source_info.clone(),
        }
    };
    Ok(ServiceTest {
        id: read.id.value,
        doc,
        parameters: (!read.parameters.is_empty()).then_some(read.parameters),
        keys,
        serialization_format: read.serialization_format.map(|format| format.value),
        assertions: vec![(SmolStr::new_static("default"), assertion)],
        source_info: read.source_info,
    })
}

/// The walker's `visitTest`.
fn walk_legacy_test(read: ReadLegacyTest) -> R<LegacyTest> {
    Ok(match read {
        ReadLegacyTest::Single(fields) => {
            let (data, asserts) =
                walk_legacy_fields(fields.data, fields.asserts, &fields.source_info)?;
            LegacyTest::Single {
                data,
                asserts,
                source_info: fields.source_info,
            }
        }
        ReadLegacyTest::Multi { tests, source_info } => LegacyTest::Multi {
            tests: tests
                .into_iter()
                .map(|(key, fields)| {
                    let (data, asserts) =
                        walk_legacy_fields(fields.data, fields.asserts, &fields.source_info)?;
                    Ok(KeyedSingleExecutionTest {
                        key: string_text(&key)?,
                        data,
                        asserts,
                        source_info: fields.source_info,
                    })
                })
                .collect::<R<_>>()?,
            source_info,
        },
    })
}

/// A legacy test's data, required, and assertions, optional.
fn walk_legacy_fields(
    data: Vec<Token>,
    asserts: Vec<Vec<TestContainer>>,
    source_info: &SourceInfo,
) -> R<(String, Vec<TestContainer>)> {
    let data = string_text(&required(data, "data", source_info)?)?;
    let asserts = optional(asserts, "asserts", source_info)?.unwrap_or_default();
    Ok((data, asserts))
}

/// The walker's `visitPostValidation`.
fn walk_post_validation(read: ReadPostValidation) -> R<PostValidation> {
    let source_info = read.source_info;
    let description = string_text(&required(read.descriptions, "description", &source_info)?)?;
    let parameters = required(read.parameters, "params", &source_info)?;
    let assertions = required(read.assertions, "assertions", &source_info)?;
    Ok(PostValidation {
        description,
        parameters,
        assertions,
        source_info,
    })
}

/// The walker's `visitExecutionEnvironment`: its executions all single ones,
/// or all groups of them.
fn walk_environment(ctx: &mut ParserContext<'_>, read: ReadEnvironment) -> R<ExecutionEnvironment> {
    let source_info = read.source_info;
    let mixed = || ParseError::Unsupported {
        message: "the engine fails on an execution environment mixing single and grouped \
                  executions"
            .to_string(),
        source_info: source_info.clone(),
    };
    let executions = if read
        .executions
        .iter()
        .any(|execution| matches!(execution, ReadExecutionParameters::Single(_)))
    {
        ExecutionParameters::Single(
            read.executions
                .into_iter()
                .map(|execution| match execution {
                    ReadExecutionParameters::Single(single) => walk_single_parameters(ctx, single),
                    ReadExecutionParameters::Multi(..) => Err(mixed()),
                })
                .collect::<R<_>>()?,
        )
    } else {
        ExecutionParameters::Multi(
            read.executions
                .into_iter()
                .map(|execution| match execution {
                    ReadExecutionParameters::Multi(master_key, singles) => {
                        Ok(MultiExecutionParameters {
                            master_key: master_key.value,
                            executions: singles
                                .into_iter()
                                .map(|single| walk_single_parameters(ctx, single))
                                .collect::<R<_>>()?,
                        })
                    }
                    ReadExecutionParameters::Single(_) => Err(mixed()),
                })
                .collect::<R<_>>()?,
        )
    };
    Ok(ExecutionEnvironment {
        package: read.package,
        name: read.name,
        executions,
        source_info,
    })
}

/// The walker's `visitSingleExecutionParameters`.
fn walk_single_parameters(
    ctx: &mut ParserContext<'_>,
    read: ReadSingleParameters,
) -> R<SingleExecutionParameters> {
    let source_info = read.source_info;
    if read.key.value.contains(' ') {
        return Err(ParseError::Unexpected {
            message: format!(
                "Execution param key cannot contain spaces. Invalid Key: {}",
                read.key.value
            ),
            source_info,
        });
    }
    let runtime = optional(read.runtimes, "runtime", &source_info)?
        .map(|runtime| walk_runtime(ctx, runtime))
        .transpose()?;
    let runtime_components = optional(read.runtime_components, "runtimeComponents", &source_info)?
        .map(|components| {
            // The engine takes each component's first, and fails without one.
            let first = |paths: Vec<ElementPath>| {
                paths
                    .into_iter()
                    .next()
                    .ok_or_else(|| ParseError::Unsupported {
                        message: "the engine fails on runtime components without a runtime, a \
                              class or a binding"
                            .to_string(),
                        source_info: source_info.clone(),
                    })
            };
            Ok::<_, ParseError>(RuntimeComponents {
                runtime: first(components.runtimes)?,
                class: first(components.classes)?,
                binding: first(components.bindings)?,
            })
        })
        .transpose()?;
    Ok(SingleExecutionParameters {
        key: read.key.value,
        mapping: read.mapping,
        runtime,
        runtime_components,
        source_info,
    })
}

#[linkme::distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static SERVICE_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &ServiceSectionParser;
