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

//! `###FileGeneration`: the engine's `FileGenerationParserGrammar`, then
//! its `FileGenerationParseTreeWalker`.
//!
//! A file generation is named by its generator's type, any identifier:
//! `Avro my::Config { … }`. Its fields are its scope elements, its output
//! path and configuration properties of any name. Where the grammar reads a
//! field both as its own and as a property (`scopeElements: ['a'];`,
//! `generationOutputPath: 'p';`), it takes its own; a property of either
//! name is the walker's error.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::optional;
use legend_pure_parser_parser::forms::{list, string_text};
use legend_pure_parser_parser::words::{
    Name, identifier, is_word, is_word_kind, qualified_name, unexpected,
};
use legend_pure_parser_protocol::v1::record::{Record, Value};
use serde_json::{Map, Value as Json};
use smol_str::SmolStr;

type R<T> = Result<T, ParseError>;

/// The names only the grammar's own fields may have.
const RESERVED: [&str; 2] = ["generationOutputPath", "scopeElements"];

/// `Type my::G { field … }`.
pub(crate) struct ReadFileGeneration {
    pub type_name: Name,
    pub package: Option<Package>,
    pub name: SmolStr,
    pub name_source_info: SourceInfo,
    scope_elements: Vec<Vec<String>>,
    output_paths: Vec<String>,
    properties: Vec<Property>,
    /// From the type to `}`.
    pub source_info: SourceInfo,
}

/// `name: value;`, its name as written (`getText()`).
struct Property {
    name: String,
    value: Json,
    source_info: SourceInfo,
}

/// A file generation.
pub(crate) fn read(ctx: &mut ParserContext<'_>) -> R<ReadFileGeneration> {
    let start = ctx.cursor().current_source_info();
    let type_name = identifier(ctx)?;
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    let mut read = ReadFileGeneration {
        type_name,
        package,
        name,
        name_source_info,
        scope_elements: Vec::new(),
        output_paths: Vec::new(),
        properties: Vec::new(),
        source_info: start.clone(),
    };
    ctx.cursor().expect(TokenKind::LBrace)?;
    while !ctx.cursor().check(TokenKind::RBrace) {
        field(ctx, &mut read)?;
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    read.source_info = ctx.cursor().span_since(&start);
    Ok(read)
}

/// `name(.name)*`, as written.
fn property_name(ctx: &mut ParserContext<'_>) -> R<String> {
    let mut name = identifier(ctx)?.text.to_string();
    while ctx.cursor().eat(TokenKind::Dot) {
        name.push('.');
        name.push_str(&identifier(ctx)?.text);
    }
    Ok(name)
}

/// Whether `[ qualified name, … ]`, possibly empty, is next: the grammar's
/// `elementsArray`.
fn paths_ahead(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    let mut at = 1;
    loop {
        let token = cursor.peek_at(at);
        match token.kind {
            TokenKind::RBracket => return true,
            TokenKind::PathSep | TokenKind::Comma | TokenKind::StringLiteral => {}
            kind if is_word_kind(kind) => {}
            _ => return false,
        }
        at += 1;
    }
}

fn field(ctx: &mut ParserContext<'_>, read: &mut ReadFileGeneration) -> R<()> {
    let start = ctx.cursor().current_source_info();
    let first = ctx.cursor().peek().clone();
    let name = property_name(ctx)?;
    let own = |field: &str| is_word(&first, field) && name == field;
    ctx.cursor().expect(TokenKind::Colon)?;
    if own("scopeElements") && ctx.cursor().check(TokenKind::LBracket) && paths_ahead(ctx) {
        let paths = list(ctx, |ctx| Ok(qualified_name(ctx)?.path))?;
        ctx.cursor().expect(TokenKind::Semicolon)?;
        read.scope_elements.push(paths);
        return Ok(());
    }
    if own("generationOutputPath")
        && ctx.cursor().check(TokenKind::StringLiteral)
        && ctx.cursor().peek_kind_at(1) == TokenKind::Semicolon
    {
        let path = ctx.cursor().advance().clone();
        ctx.cursor().expect(TokenKind::Semicolon)?;
        read.output_paths.push(string_text(&path)?);
        return Ok(());
    }
    let value = value(ctx)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    read.properties.push(Property {
        name,
        value,
        source_info: ctx.cursor().span_since(&start),
    });
    Ok(())
}

/// An `INTEGER`, a Java `long`: one too large makes the engine fail.
fn integer(ctx: &mut ParserContext<'_>) -> R<Json> {
    let token = ctx.cursor().expect(TokenKind::IntegerLiteral)?;
    token
        .text
        .parse::<i64>()
        .map(Json::from)
        .map_err(|_| ParseError::Unsupported {
            message: format!("the engine fails on {}, too large for a long", token.text),
            source_info: token.source_info,
        })
}

/// A property's value: an integer, a string, a boolean, a list of strings
/// or of integers (an empty one, of strings), or a map of properties.
fn value(ctx: &mut ParserContext<'_>) -> R<Json> {
    let token = ctx.cursor().peek().clone();
    match token.kind {
        TokenKind::IntegerLiteral => integer(ctx),
        TokenKind::StringLiteral => {
            ctx.cursor().advance();
            Ok(Json::String(string_text(&token)?))
        }
        TokenKind::True | TokenKind::False => {
            ctx.cursor().advance();
            Ok(Json::Bool(token.kind == TokenKind::True))
        }
        TokenKind::LBracket => {
            ctx.cursor().advance();
            let mut values = Vec::new();
            if !ctx.cursor().check(TokenKind::RBracket) {
                let strings = ctx.cursor().check(TokenKind::StringLiteral);
                loop {
                    if strings {
                        let string = ctx.cursor().expect(TokenKind::StringLiteral)?;
                        values.push(Json::String(string_text(&string)?));
                    } else {
                        values.push(integer(ctx)?);
                    }
                    if !ctx.cursor().eat(TokenKind::Comma) {
                        break;
                    }
                }
            }
            ctx.cursor().expect(TokenKind::RBracket)?;
            Ok(Json::Array(values))
        }
        TokenKind::LBrace => {
            ctx.cursor().advance();
            let mut map = Map::new();
            while !ctx.cursor().check(TokenKind::RBrace) {
                let name = property_name(ctx)?;
                ctx.cursor().expect(TokenKind::Colon)?;
                let value = value(ctx)?;
                ctx.cursor().expect(TokenKind::Semicolon)?;
                map.insert(name, value);
            }
            ctx.cursor().expect(TokenKind::RBrace)?;
            Ok(Json::Object(map))
        }
        _ => Err(unexpected(&token)),
    }
}

/// The walker's `visitFileGeneration`: the generation's own record,
/// without its package and name.
pub(crate) fn walk(read: ReadFileGeneration) -> R<Record> {
    let at = read.source_info;
    // Java's `substring(0, 1).toLowerCase()`, which an empty type fails.
    let mut characters = read.type_name.value.chars();
    let Some(first) = characters.next() else {
        return Err(ParseError::Unsupported {
            message: "the engine fails on an empty generation type".to_string(),
            source_info: read.type_name.source_info,
        });
    };
    let type_name: String = first.to_lowercase().chain(characters).collect();
    let scope_elements = optional(read.scope_elements, "scopeElements", &at)?.unwrap_or_default();
    let output_path = optional(read.output_paths, "generationOutputPath", &at)?;
    let properties = read
        .properties
        .into_iter()
        .map(|property| {
            if RESERVED.contains(&property.name.as_str()) {
                return Err(ParseError::Unexpected {
                    message: format!(
                        "Can't have config property with reserved name '{}'",
                        property.name
                    ),
                    source_info: at.clone(),
                });
            }
            Ok(Record::untyped()
                .at(&property.source_info)
                .with("name", property.name)
                .with("value", Value::Json(property.value)))
        })
        .collect::<R<Vec<_>>>()?;
    Ok(Record::new("fileGeneration")
        .at(&at)
        .with("type", type_name)
        .with(
            "typeSourceInformation",
            Value::SourceInfo(read.type_name.source_info),
        )
        .with("scopeElements", scope_elements)
        .with_some("generationOutputPath", output_path)
        .with("configurationProperties", properties))
}
