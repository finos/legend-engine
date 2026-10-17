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

//! The section's grammar, `MongoDBSchemaParserGrammar`: what it reads,
//! before the walk builds it. Its order is the grammar's: a database's
//! includes, then its collections, views and joins; a collection's
//! validation level, its validation action, then its schema.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{at_word, is_word, is_word_kind, unexpected};
use smol_str::SmolStr;

use super::json::{Json, value};

type R<T> = Result<T, ParseError>;

/// `Database my::D ( … )`.
pub(crate) struct ReadDatabase {
    pub package: Option<Package>,
    pub name: SmolStr,
    pub name_source_info: SourceInfo,
    /// Its collections with a schema, which the walker keeps.
    pub collections: Vec<ReadCollection>,
    /// From `Database` to `)`.
    pub source_info: SourceInfo,
}

/// `Collection name ( … jsonSchema: { … }; )`.
pub(crate) struct ReadCollection {
    /// As the walker reads it: a `"quoted"` name without its quotes, not
    /// unescaped.
    pub name: String,
    pub moderate: bool,
    pub warn: bool,
    pub schema: Json,
    /// The collection.
    pub source_info: SourceInfo,
}

/// The grammar's `identifier`: a word (`true`, `false` and `null`
/// included), or a `"quoted"` name.
fn identifier(ctx: &mut ParserContext<'_>) -> R<Token> {
    let token = ctx.cursor().peek().clone();
    let word = is_word_kind(token.kind) || matches!(token.kind, TokenKind::True | TokenKind::False);
    let quoted = token.kind == TokenKind::StringLiteral && token.text.starts_with('"');
    if !word && !quoted {
        return Err(unexpected(&token));
    }
    ctx.cursor().advance();
    Ok(token)
}

/// The grammar's `qualifiedName`, of its identifiers.
fn qualified_name(ctx: &mut ParserContext<'_>) -> R<()> {
    identifier(ctx)?;
    while ctx.cursor().eat(TokenKind::PathSep) {
        identifier(ctx)?;
    }
    Ok(())
}

/// One of `values`, as a word.
fn one_of(ctx: &mut ParserContext<'_>, values: [&str; 2]) -> R<String> {
    let token = ctx.cursor().peek().clone();
    if !values.iter().any(|value| is_word(&token, value)) {
        return Err(unexpected(&token));
    }
    ctx.cursor().advance();
    Ok(token.text.to_string())
}

/// A database.
pub(crate) fn database(ctx: &mut ParserContext<'_>) -> R<ReadDatabase> {
    let keyword = ctx.cursor().peek().clone();
    if !is_word(&keyword, "Database") {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    ctx.cursor().expect(TokenKind::LParen)?;
    while at_word(ctx, "include") {
        ctx.cursor().advance();
        qualified_name(ctx)?;
    }
    let mut collections = Vec::new();
    loop {
        if at_word(ctx, "Collection") {
            if let Some(collection) = collection(ctx)? {
                collections.push(collection);
            }
        } else if at_word(ctx, "Join") {
            join(ctx)?;
        } else {
            break;
        }
    }
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(ReadDatabase {
        package,
        name,
        name_source_info,
        collections,
        source_info: ctx.cursor().span_since(&keyword.source_info),
    })
}

/// A collection, or, without a schema, a view (`None`), which the walker
/// leaves out.
fn collection(ctx: &mut ParserContext<'_>) -> R<Option<ReadCollection>> {
    let keyword = ctx.cursor().advance().clone();
    let name = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::LParen)?;
    let mut moderate = false;
    if at_word(ctx, "validationLevel") {
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::Colon)?;
        moderate = one_of(ctx, ["strict", "moderate"])? == "moderate";
        ctx.cursor().expect(TokenKind::Semicolon)?;
    }
    let mut warn = false;
    if at_word(ctx, "validationAction") {
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::Colon)?;
        warn = one_of(ctx, ["error", "warn"])? == "warn";
        ctx.cursor().expect(TokenKind::Semicolon)?;
    }
    let schema = if at_word(ctx, "jsonSchema") {
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::Colon)?;
        let schema = value(ctx)?;
        ctx.cursor().expect(TokenKind::Semicolon)?;
        Some(schema)
    } else {
        None
    };
    ctx.cursor().expect(TokenKind::RParen)?;
    let text = name.text.as_str();
    let name = if name.kind == TokenKind::StringLiteral {
        text.get(1..text.len() - 1).unwrap_or_default().to_string()
    } else {
        text.to_string()
    };
    Ok(schema.map(|schema| ReadCollection {
        name,
        moderate,
        warn,
        schema,
        source_info: ctx.cursor().span_since(&keyword.source_info),
    }))
}

/// `[my::Database]`.
fn database_pointer(ctx: &mut ParserContext<'_>) -> R<()> {
    if ctx.cursor().eat(TokenKind::LBracket) {
        qualified_name(ctx)?;
        ctx.cursor().expect(TokenKind::RBracket)?;
    }
    Ok(())
}

/// `[my::Database]? [my::Database]? collection.field`.
fn field_path(ctx: &mut ParserContext<'_>) -> R<()> {
    database_pointer(ctx)?;
    database_pointer(ctx)?;
    identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Dot)?;
    identifier(ctx)?;
    Ok(())
}

/// `Join name ( path = path )`, which the walker leaves out.
fn join(ctx: &mut ParserContext<'_>) -> R<()> {
    ctx.cursor().advance();
    identifier(ctx)?;
    ctx.cursor().expect(TokenKind::LParen)?;
    field_path(ctx)?;
    ctx.cursor().expect(TokenKind::Equals)?;
    field_path(ctx)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(())
}
