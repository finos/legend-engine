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

//! `SectionParser` for `###Runtime`: the engine's `RuntimeParserGrammar` and
//! `RuntimeParseTreeWalker`.
//!
//! ```text
//! runtime:     Runtime my::R { ( mappings: [ my::M, … ];
//!                              | connections: [ my::Store: [ id: connection, … ], … ];
//!                              | connectionStores: [ my::C: [ (type)? my::Store, … ], … ]; )* }
//! single:      SingleConnectionRuntime my::R { ( mappings: [ my::M, … ]; | connection: my::C; )* }
//! connection:  my::Connection | #{ Type { … } }#
//! ```
//!
//! As in the engine, the section's grammar is read whole first, a runtime's
//! own connections read but not parsed; then the runtimes are walked, all
//! `Runtime`s first, then all `SingleConnectionRuntime`s, and the elements
//! come out in that order. A runtime needs its mappings, a single connection
//! runtime its connection; a field may be written once; a store's
//! connections may be given once, and, as the engine keeps a store only
//! when it has connections, a store without any doesn't count.

use std::collections::HashSet;

use legend_pure_dsl_connection::section::parse_embedded_connection;
use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::{optional, required};
use legend_pure_parser_parser::forms::{field_start, list};
use legend_pure_parser_parser::words::{
    identifier, is_word, is_word_kind, qualified_name, unexpected,
};
use legend_pure_parser_parser::{Island, ParserContext, SectionParser};
use smol_str::SmolStr;

use crate::ast::{
    ConnectionStores, ElementPath, EngineRuntime, IdentifiedConnection, KIND, PackageableRuntime,
    RuntimeConnection, StoreConnections, StorePointer,
};

type R<T> = Result<T, ParseError>;

/// The engine's `PackageableElementType`s, which a store pointer's type
/// names in any case.
const ELEMENT_TYPES: [&str; 24] = [
    "PACKAGE",
    "PROFILE",
    "CLASS",
    "ASSOCIATION",
    "ENUMERATION",
    "FUNCTION",
    "STORE",
    "RUNTIME",
    "BINDING",
    "MAPPING",
    "SERVICE",
    "PERSISTENCE",
    "PERSISTENCE_CONTEXT",
    "FLATTEN",
    "DATASTORESPEC",
    "DATASPACE",
    "DIAGRAM",
    "FILE_GENERATION",
    "DATA",
    "QUERYPOSTPROCESSOR",
    "DATA_QUALITY_VALIDATION",
    "CONNECTION",
    "EXECUTION_ENVIRONMENT_INSTANCE",
    "DATAELEMENT",
];

/// The runtime grammar's keywords, which a store pointer's type can't be.
const KEYWORDS: [&str; 7] = [
    "Runtime",
    "SingleConnectionRuntime",
    "import",
    "mappings",
    "connections",
    "connection",
    "connectionStores",
];

/// Parser for `###Runtime` sections.
pub struct RuntimeSectionParser;

impl SectionParser for RuntimeSectionParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        KIND
    }

    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        let mut read = Vec::new();
        while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
            match read_runtime(ctx) {
                Ok(runtime) => read.push(runtime),
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        // The engine walks the runtimes, then the single connection ones.
        let (single, runtimes): (Vec<_>, Vec<_>) = read
            .into_iter()
            .partition(|runtime| runtime.single_connection);
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for runtime in runtimes.into_iter().chain(single) {
            match walk_runtime(ctx, runtime) {
                Ok(runtime) => elements.push(Box::new(runtime)),
                // The engine reports the first error.
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        elements
    }
}

// ── The grammar: the section read, its connections not parsed yet ───────

/// A runtime as its grammar reads it: each field as often as written.
struct ReadRuntime {
    single_connection: bool,
    package: Option<Package>,
    name: SpannedString,
    mappings: Vec<Vec<ElementPath>>,
    connections: Vec<Vec<ReadStoreConnections>>,
    connection_stores: Vec<Vec<ConnectionStores>>,
    connection: Vec<ElementPath>,
    source_info: SourceInfo,
}

struct ReadStoreConnections {
    store: ElementPath,
    connections: Vec<ReadIdentifiedConnection>,
    source_info: SourceInfo,
}

struct ReadIdentifiedConnection {
    id: SmolStr,
    connection: ReadConnection,
    source_info: SourceInfo,
}

enum ReadConnection {
    Pointer(ElementPath),
    Embedded(Island),
}

/// `Runtime my::R { … }` or `SingleConnectionRuntime my::R { … }`.
fn read_runtime(ctx: &mut ParserContext<'_>) -> R<ReadRuntime> {
    let start = ctx.cursor().current_source_info();
    let keyword = ctx.cursor().peek().clone();
    let single_connection = if is_word(&keyword, "Runtime") {
        false
    } else if is_word(&keyword, "SingleConnectionRuntime") {
        true
    } else {
        return Err(unexpected(&keyword));
    };
    ctx.cursor().advance();
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    ctx.cursor().expect(TokenKind::LBrace)?;
    let mut read = ReadRuntime {
        single_connection,
        package,
        name: SpannedString {
            value: name,
            source_info: name_source_info,
        },
        mappings: Vec::new(),
        connections: Vec::new(),
        connection_stores: Vec::new(),
        connection: Vec::new(),
        source_info: start.clone(),
    };
    loop {
        let field = ctx.cursor().peek().clone();
        if is_word(&field, "mappings") {
            field_start(ctx)?;
            read.mappings.push(list(ctx, element_path)?);
        } else if !single_connection && is_word(&field, "connections") {
            field_start(ctx)?;
            read.connections.push(list(ctx, store_connections)?);
        } else if !single_connection && is_word(&field, "connectionStores") {
            field_start(ctx)?;
            read.connection_stores.push(list(ctx, connection_stores)?);
        } else if single_connection && is_word(&field, "connection") {
            field_start(ctx)?;
            read.connection.push(element_path(ctx)?);
        } else {
            break;
        }
        ctx.cursor().expect(TokenKind::Semicolon)?;
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

/// `my::Store: [ id: connection, … ]`.
fn store_connections(ctx: &mut ParserContext<'_>) -> R<ReadStoreConnections> {
    let store = element_path(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let connections = list(ctx, identified_connection)?;
    let source_info = ctx.cursor().span_since(&store.source_info);
    Ok(ReadStoreConnections {
        store,
        connections,
        source_info,
    })
}

/// `id: my::Connection` or `id: #{ Type { … } }#`.
fn identified_connection(ctx: &mut ParserContext<'_>) -> R<ReadIdentifiedConnection> {
    let id = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let connection = if ctx.cursor().check(TokenKind::IslandOpen) {
        ReadConnection::Embedded(ctx.read_island()?)
    } else {
        ReadConnection::Pointer(element_path(ctx)?)
    };
    Ok(ReadIdentifiedConnection {
        source_info: ctx.cursor().span_since(&id.source_info),
        id: id.value,
        connection,
    })
}

/// `my::Connection: [ (type)? my::Store, … ]`.
fn connection_stores(ctx: &mut ParserContext<'_>) -> R<ConnectionStores> {
    let connection = element_path(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let stores = list(ctx, store_pointer)?;
    let source_info = ctx.cursor().span_since(&connection.source_info);
    Ok(ConnectionStores {
        connection,
        stores,
        source_info,
    })
}

/// `(type)? my::Store`: the type a word other than the grammar's keywords,
/// which the walker reads as a `PackageableElementType`; one it doesn't know
/// is the engine's to report.
fn store_pointer(ctx: &mut ParserContext<'_>) -> R<StorePointer> {
    let start = ctx.cursor().current_source_info();
    let element_type = if ctx.cursor().eat(TokenKind::LParen) {
        let word = ctx.cursor().peek().clone();
        if !is_word_kind(word.kind) || KEYWORDS.contains(&word.text.as_str()) {
            return Err(unexpected(&word));
        }
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::RParen)?;
        let element_type = word.text.to_uppercase();
        if !ELEMENT_TYPES.contains(&element_type.as_str()) {
            return Err(ParseError::Unsupported {
                message: format!(
                    "'{}' names no packageable element type, which the engine reports",
                    word.text
                ),
                source_info: word.source_info,
            });
        }
        Some(SmolStr::new(element_type))
    } else {
        None
    };
    let path = element_path(ctx)?;
    Ok(StorePointer {
        element_type,
        path: path.path,
        source_info: ctx.cursor().span_since(&start),
    })
}

// ── The walk ─────────────────────────────────────────────────────────────

/// The walker's `visitRuntime` and `visitSingleConnectionRuntime`.
fn walk_runtime(ctx: &mut ParserContext<'_>, read: ReadRuntime) -> R<PackageableRuntime> {
    let source_info = read.source_info;
    let mappings = required(read.mappings, "mappings", &source_info)?;
    let (connections, connection_stores) = if read.single_connection {
        let connection = required(read.connection, "connection", &source_info)?;
        let stores = ConnectionStores {
            source_info: connection.source_info.clone(),
            connection,
            stores: Vec::new(),
        };
        (Vec::new(), vec![stores])
    } else {
        let connections = optional(read.connections, "connections", &source_info)?;
        let connections = walk_connections(ctx, connections.unwrap_or_default())?;
        let connection_stores = optional(
            read.connection_stores,
            "connectionPointerStores",
            &source_info,
        )?;
        (connections, connection_stores.unwrap_or_default())
    };
    Ok(PackageableRuntime {
        package: read.package,
        name: read.name,
        single_connection: read.single_connection,
        value: EngineRuntime {
            mappings,
            connections,
            connection_stores,
            source_info: source_info.clone(),
        },
        source_info,
    })
}

/// A runtime of an element's own, `#{ mappings: [ … ]; connections: [ … ]; }#`
/// (the engine's `RuntimeParser.parseEmbeddedRuntime`): its mappings and its
/// connections by store, each optional, the runtime positioned from its
/// first field to its last. The engine's rule reads fields while they come
/// and stops there, so what follows is not read; with no field read, the
/// runtime is empty, an error the engine reports at `at`, where the island
/// is in the element's grammar.
///
/// # Errors
///
/// A syntax error in a field, an empty runtime, a field written twice, a
/// store's connections given twice, or a connection's error.
pub fn parse_embedded_runtime(
    ctx: &mut ParserContext<'_>,
    island: &Island,
    at: &SourceInfo,
) -> R<EngineRuntime> {
    let (mappings, connections, source_info) = ctx.parse_island(island, |ctx| {
        let start = ctx.cursor().current_source_info();
        let (mut mappings, mut connections) = (Vec::new(), Vec::new());
        let mut source_info = None;
        loop {
            let field = ctx.cursor().peek().clone();
            if is_word(&field, "mappings") {
                field_start(ctx)?;
                mappings.push(list(ctx, element_path)?);
            } else if is_word(&field, "connections") {
                field_start(ctx)?;
                connections.push(list(ctx, store_connections)?);
            } else {
                break;
            }
            ctx.cursor().expect(TokenKind::Semicolon)?;
            source_info = Some(ctx.cursor().span_since(&start));
        }
        while !ctx.cursor().check(TokenKind::Eof) {
            ctx.cursor().advance();
        }
        Ok((mappings, connections, source_info))
    })?;
    let Some(source_info) = source_info else {
        return Err(ParseError::Unexpected {
            message: "Embedded runtime must not be empty".to_string(),
            source_info: at.clone(),
        });
    };
    let mappings = optional(mappings, "mappings", &source_info)?.unwrap_or_default();
    let connections = optional(connections, "connections", &source_info)?.unwrap_or_default();
    Ok(EngineRuntime {
        mappings,
        connections: walk_connections(ctx, connections)?,
        connection_stores: Vec::new(),
        source_info,
    })
}

/// Each store's connections, a runtime's own parsed; a store with
/// connections may not be given again.
fn walk_connections(
    ctx: &mut ParserContext<'_>,
    read: Vec<ReadStoreConnections>,
) -> R<Vec<StoreConnections>> {
    let mut with_connections = HashSet::new();
    let mut walked = Vec::new();
    for store in read {
        if with_connections.contains(&store.store.path) {
            return Err(ParseError::Unexpected {
                message: format!(
                    "Connections for store '{}' is already specified",
                    store.store.path
                ),
                source_info: store.source_info,
            });
        }
        let mut connections = Vec::new();
        for identified in store.connections {
            let connection = match identified.connection {
                ReadConnection::Pointer(path) => RuntimeConnection::Pointer(path),
                ReadConnection::Embedded(island) => {
                    RuntimeConnection::Embedded(parse_embedded_connection(ctx, &island)?)
                }
            };
            connections.push(IdentifiedConnection {
                id: identified.id,
                connection,
                source_info: identified.source_info,
            });
        }
        if !connections.is_empty() {
            with_connections.insert(store.store.path.clone());
        }
        walked.push(StoreConnections {
            store: store.store,
            connections,
            source_info: store.source_info,
        });
    }
    Ok(walked)
}

#[linkme::distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static RUNTIME_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &RuntimeSectionParser;
