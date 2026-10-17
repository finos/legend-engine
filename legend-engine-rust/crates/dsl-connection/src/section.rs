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

//! `SectionParser` for `###Connection`: the engine's `ConnectionParserGrammar`
//! and `ConnectionParseTreeWalker`.
//!
//! ```text
//! definition:  connection*
//! connection:  Type my::Connection { value }
//! embedded:    Type { value }          (a runtime's own, in `#{ … }#`)
//! ```
//!
//! As in the engine, the section's grammar is read whole first, each value's
//! body read but not parsed; then each value is read by its type's grammar.
//! The section's syntax errors are therefore reported before any error in a
//! value.

use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{Name, identifier};
use legend_pure_parser_parser::{Block, Island, ParserContext, SectionParser};

use crate::ast::{ConnectionValue, KIND, PackageableConnection};
use crate::grammar::{ValueSpan, connection_value_grammar, linked_connection_value_types};

type R<T> = Result<T, ParseError>;

/// Parser for `###Connection` sections.
pub struct ConnectionSectionParser;

impl SectionParser for ConnectionSectionParser {
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
            match read_connection(ctx) {
                Ok(connection) => read.push(connection),
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for connection in read {
            match walk_connection(ctx, connection) {
                Ok(connection) => elements.push(Box::new(connection)),
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

/// A connection as the section's grammar reads it.
struct ReadConnection {
    type_name: Name,
    package: Option<Package>,
    name: SpannedString,
    body: Block,
    source_info: SourceInfo,
}

/// `Type my::Connection { … }`.
fn read_connection(ctx: &mut ParserContext<'_>) -> R<ReadConnection> {
    let start = ctx.cursor().current_source_info();
    let type_name = identifier(ctx)?;
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    let body = ctx.read_block()?;
    Ok(ReadConnection {
        type_name,
        package,
        name: SpannedString {
            value: name,
            source_info: name_source_info,
        },
        body,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// A connection's value, read by its type's grammar; the whole connection
/// is the value's span.
fn walk_connection(ctx: &mut ParserContext<'_>, read: ReadConnection) -> R<PackageableConnection> {
    let span = ValueSpan {
        value: read.source_info.clone(),
        body: read.body.source_info.clone(),
        embedded: false,
    };
    let value = connection_value(ctx, read.type_name, &read.body, span)?;
    Ok(PackageableConnection {
        package: read.package,
        name: read.name,
        value,
        source_info: read.source_info,
    })
}

/// A value's body, read by its type's grammar.
fn connection_value(
    ctx: &mut ParserContext<'_>,
    type_name: Name,
    body: &Block,
    span: ValueSpan,
) -> R<ConnectionValue> {
    let Some(grammar) = connection_value_grammar(&type_name.text) else {
        return Err(ParseError::Unsupported {
            message: format!(
                "no connection grammar is linked for '{}'; the linked ones are: {}",
                type_name.text,
                linked_connection_value_types()
            ),
            source_info: span.value,
        });
    };
    let parsed = ctx.parse_block(body, |body| grammar.parse(&span, body))?;
    Ok(ConnectionValue {
        type_name,
        body: parsed,
        embedded: span.embedded,
        source_info: span.value,
    })
}

/// A runtime's own connection, `#{ Type { … } }#` (the engine's
/// `ConnectionParser.parseEmbeddedRuntimeConnections`), its value read by its
/// type's grammar. What follows the value's `}` is not read, as the engine's
/// rule ends there.
///
/// # Errors
///
/// An empty island, a syntax error in `Type { … }`, the value's grammar's
/// error, or a type no linked grammar reads (unsupported).
pub fn parse_embedded_connection(
    ctx: &mut ParserContext<'_>,
    island: &Island,
) -> R<ConnectionValue> {
    if island.text().is_empty() {
        return Err(ParseError::Unexpected {
            message: "Embedded connection must not be empty".to_string(),
            source_info: island.source_info.clone(),
        });
    }
    ctx.parse_island(island, |ctx| {
        let start = ctx.cursor().current_source_info();
        let type_name = identifier(ctx)?;
        let body = ctx.read_block()?;
        let source_info = ctx.cursor().span_since(&start);
        while !ctx.cursor().check(TokenKind::Eof) {
            ctx.cursor().advance();
        }
        let span = ValueSpan {
            value: source_info,
            body: body.source_info.clone(),
            embedded: true,
        };
        connection_value(ctx, type_name, &body, span)
    })
}

#[linkme::distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static CONNECTION_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &ConnectionSectionParser;
