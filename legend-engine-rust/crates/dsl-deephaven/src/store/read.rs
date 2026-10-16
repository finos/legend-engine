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

//! The section's grammar, `DeephavenParserGrammar`: what it reads, before
//! the walk checks it. The core parser reads the section's imports first.
//!
//! A store's tables, and a table's columns, are read as the grammar reads
//! them: in lists (`a: INT, b: INT`), of which the walker requires one. An
//! app's fields are read as blocks of fields (see
//! [`legend_pure_parser_parser::blocks`]).

use legend_pure_dsl_function_activator::read::deployment;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{ReadBlock, ReadValue, braces, simple, string_value};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::separated;
use legend_pure_parser_parser::words::{
    adjacent, identifier, is_word, is_word_kind, qualified_name, unexpected,
};
use smol_str::SmolStr;

type R<T> = Result<T, ParseError>;

/// The grammar's keywords, which a table's or a column's name
/// (`VALID_STRING | STRING`) isn't.
const KEYWORDS: [&str; 21] = [
    "import",
    "Deephaven",
    "Table",
    "tables",
    "columns",
    "columnDefinition",
    "BOOLEAN",
    "INT",
    "FLOAT",
    "DOUBLE",
    "DECIMAL",
    "STRING",
    "TIMESTAMP",
    "DATETIME",
    "DeephavenApp",
    "applicationName",
    "function",
    "ownership",
    "Deployment",
    "identifier",
    "description",
];

/// A column type's keyword, and its protocol type.
const COLUMN_TYPES: [(&str, &str); 8] = [
    ("BOOLEAN", "booleanType"),
    ("INT", "intType"),
    ("FLOAT", "floatType"),
    ("DOUBLE", "doubleType"),
    ("STRING", "stringType"),
    ("TIMESTAMP", "timestampType"),
    ("DATETIME", "dateTimeType"),
    ("DECIMAL", "decimalType"),
];

/// `Deephaven my::S ( … )` or `DeephavenApp my::A { … }`.
pub(crate) struct ReadElement {
    pub package: Option<Package>,
    pub name: SmolStr,
    pub name_source_info: SourceInfo,
    pub body: ReadBody,
}

/// What the grammar read of a store or an app.
pub(crate) enum ReadBody {
    /// A store's tables; positioned from `Deephaven` to `)`.
    Store(Vec<ReadTable>, SourceInfo),
    /// An app's fields; positioned from `DeephavenApp` to `}`.
    App(ReadBlock),
}

/// `Table name ( … )`: its lists of columns.
pub(crate) struct ReadTable {
    pub name: String,
    pub columns: Vec<Vec<ReadColumn>>,
    /// From `Table` to `)`.
    pub source_info: SourceInfo,
}

/// `name: TYPE`: its protocol type and, for `DECIMAL(p, s)`, its
/// precision and scale, as written.
pub(crate) struct ReadColumn {
    pub name: String,
    pub type_name: &'static str,
    pub decimal: Option<(Token, Token)>,
}

/// A store or an app.
pub(crate) fn element(ctx: &mut ParserContext<'_>) -> R<ReadElement> {
    let keyword = ctx.cursor().peek().clone();
    let app = is_word(&keyword, "DeephavenApp");
    if !app && !is_word(&keyword, "Deephaven") {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    let body = if app {
        let (_, fields) = braces(ctx, app_field)?;
        ReadBody::App(ReadBlock {
            source_info: ctx.cursor().span_since(&keyword.source_info),
            keyword,
            fields,
        })
    } else {
        ctx.cursor().expect(TokenKind::LParen)?;
        let mut tables = Vec::new();
        while is_word(ctx.cursor().peek(), "Table") {
            tables.push(table(ctx)?);
        }
        ctx.cursor().expect(TokenKind::RParen)?;
        ReadBody::Store(tables, ctx.cursor().span_since(&keyword.source_info))
    };
    Ok(ReadElement {
        package,
        name,
        name_source_info,
        body,
    })
}

/// `VALID_STRING | STRING`: a quoted name, or a word but one of the
/// grammar's keywords; unquoted.
fn name(ctx: &mut ParserContext<'_>) -> R<String> {
    let token = ctx.cursor().peek().clone();
    if is_word_kind(token.kind) && KEYWORDS.contains(&token.text.as_str()) {
        return Err(unexpected(&token));
    }
    Ok(identifier(ctx)?.value.to_string())
}

/// `Table name ( column, … … )`: any number of lists of columns.
fn table(ctx: &mut ParserContext<'_>) -> R<ReadTable> {
    let keyword = ctx.cursor().advance().clone();
    let name = name(ctx)?;
    ctx.cursor().expect(TokenKind::LParen)?;
    let mut columns = Vec::new();
    while !ctx.cursor().check(TokenKind::RParen) {
        columns.push(separated(ctx, column)?);
    }
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(ReadTable {
        name,
        columns,
        source_info: ctx.cursor().span_since(&keyword.source_info),
    })
}

/// `name: TYPE`, or `name: DECIMAL(precision, scale)`.
fn column(ctx: &mut ParserContext<'_>) -> R<ReadColumn> {
    let name = name(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let token = ctx.cursor().peek().clone();
    let type_name = COLUMN_TYPES
        .iter()
        .find(|(keyword, _)| is_word(&token, keyword))
        .map(|(_, type_name)| *type_name)
        .ok_or_else(|| unexpected(&token))?;
    ctx.cursor().advance();
    let decimal = if type_name == "decimalType" {
        ctx.cursor().expect(TokenKind::LParen)?;
        let precision = ctx.cursor().expect(TokenKind::IntegerLiteral)?;
        ctx.cursor().expect(TokenKind::Comma)?;
        let scale = ctx.cursor().expect(TokenKind::IntegerLiteral)?;
        ctx.cursor().expect(TokenKind::RParen)?;
        Some((precision, scale))
    } else {
        None
    };
    Ok(ReadColumn {
        name,
        type_name,
        decimal,
    })
}

fn app_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "applicationName" => simple(ctx, "applicationName", string_value),
        "function" => simple(ctx, "function", |ctx| {
            let start = ctx.cursor().current_source_info();
            let text = function_identifier(ctx)?;
            Ok(ReadValue::Text(text, ctx.cursor().span_since(&start)))
        }),
        "ownership" => simple(ctx, "ownership", deployment),
        "description" => simple(ctx, "description", string_value),
        _ => Ok(None),
    }
}

/// `my::f(Type[m], …):Type[m]`, as written without spaces: the grammar's
/// own `appFunctionIdentifier`, whose types are qualified names and whose
/// multiplicities are `[*]`, `[n]`, or `[n . . m]` and `[n . . *]`.
fn function_identifier(ctx: &mut ParserContext<'_>) -> R<String> {
    let mut text = qualified_name(ctx)?.text;
    text.push_str(&ctx.cursor().expect(TokenKind::LParen)?.text);
    if !ctx.cursor().check(TokenKind::RParen) {
        typed(ctx, &mut text)?;
        while ctx.cursor().check(TokenKind::Comma) {
            text.push_str(&ctx.cursor().advance().text);
            typed(ctx, &mut text)?;
        }
    }
    text.push_str(&ctx.cursor().expect(TokenKind::RParen)?.text);
    text.push_str(&ctx.cursor().expect(TokenKind::Colon)?.text);
    typed(ctx, &mut text)?;
    Ok(text)
}

/// `Type[m]`, appended to `text`.
fn typed(ctx: &mut ParserContext<'_>, text: &mut String) -> R<()> {
    text.push_str(&qualified_name(ctx)?.text);
    text.push_str(&ctx.cursor().expect(TokenKind::LBracket)?.text);
    if ctx.cursor().check(TokenKind::Star) {
        text.push_str(&ctx.cursor().advance().text);
    } else {
        text.push_str(&ctx.cursor().expect(TokenKind::IntegerLiteral)?.text);
        if ctx.cursor().check(TokenKind::Dot) {
            dot(ctx, text)?;
            dot(ctx, text)?;
            let bound = ctx.cursor().peek().clone();
            if !matches!(bound.kind, TokenKind::IntegerLiteral | TokenKind::Star) {
                return Err(unexpected(&bound));
            }
            text.push_str(&ctx.cursor().advance().text);
        }
    }
    text.push_str(&ctx.cursor().expect(TokenKind::RBracket)?.text);
    Ok(())
}

/// A `.` the engine's lexer reads alone: not the start of `..`, one token
/// the grammar doesn't take, nor of a number, `.1`.
fn dot(ctx: &mut ParserContext<'_>, text: &mut String) -> R<()> {
    let cursor = ctx.cursor();
    let dot = cursor.peek().clone();
    if dot.kind != TokenKind::Dot {
        return Err(unexpected(&dot));
    }
    let next = cursor.peek_at(1).clone();
    if adjacent(&dot, &next)
        && (next.kind == TokenKind::Dot || next.text.starts_with(|c: char| c.is_ascii_digit()))
    {
        cursor.advance();
        cursor.advance();
        return Err(ParseError::Unexpected {
            message: format!("Unexpected token '{}{}'", dot.text, next.text),
            source_info: ctx.cursor().span_since(&dot.source_info),
        });
    }
    text.push_str(&cursor.advance().text);
    Ok(())
}
