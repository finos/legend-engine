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

//! The section's grammar, `ServiceStoreParserGrammar`'s `definition`: what
//! it reads, before the walk checks it, as blocks of fields (see
//! [`legend_pure_parser_parser::blocks`]), here in parentheses.
//!
//! A store's, or a group's, services and groups are fields named
//! `Service` and `ServiceGroup`, each a block whose keyword is theirs and
//! whose first field is its `id`. A parameter is a block whose fields are
//! its `name`, its `type` and its options; a type reference, a block
//! whose keyword is its first token (`[` for a list) and whose fields are
//! its `type` and, for a class, its `binding`.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{
    Fields, ReadBlock, ReadField, ReadValue, fields_until, identifier_value, simple, values,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::separated;
use legend_pure_parser_parser::words::{
    adjacent, identifier, is_word, is_word_kind, qualified_name, unexpected,
};
use smol_str::SmolStr;

use crate::mapping::parameter_name;

type R<T> = Result<T, ParseError>;

/// `ServiceStore my::S ( … )`.
pub(crate) struct ReadStore {
    pub package: Option<Package>,
    pub name: SmolStr,
    pub name_source_info: SourceInfo,
    /// Its services and groups; positioned from `ServiceStore` to `)`.
    pub body: ReadBlock,
}

/// A store: an optional `description`, which the walker leaves out, then
/// its services and groups.
pub(crate) fn store(ctx: &mut ParserContext<'_>) -> R<ReadStore> {
    let keyword = ctx.cursor().peek().clone();
    if !is_word(&keyword, "ServiceStore") {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    ctx.cursor().expect(TokenKind::LParen)?;
    if is_word(ctx.cursor().peek(), "description") {
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::Colon)?;
        identifier(ctx)?;
        ctx.cursor().expect(TokenKind::Semicolon)?;
    }
    let mut reader = element;
    let fields = fields_until(ctx, &mut reader)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(ReadStore {
        package,
        name,
        name_source_info,
        body: ReadBlock {
            source_info: ctx.cursor().span_since(&keyword.source_info),
            keyword,
            fields,
        },
    })
}

/// `( field … )`, the fields `fields` reads.
fn parens(ctx: &mut ParserContext<'_>, mut fields: impl Fields) -> R<Vec<ReadField>> {
    ctx.cursor().expect(TokenKind::LParen)?;
    let read = fields_until(ctx, &mut fields)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(read)
}

/// A service or a group.
fn element(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "Service" => block(ctx, "Service", service_field),
        "ServiceGroup" => block(ctx, "ServiceGroup", group_field),
        _ => Ok(None),
    }
}

/// `Keyword id ( field … )`, named `name`, its id its first field.
fn block(
    ctx: &mut ParserContext<'_>,
    name: &'static str,
    fields: impl Fields,
) -> R<Option<(&'static str, ReadValue)>> {
    let keyword = ctx.cursor().advance().clone();
    let id = identifier(ctx)?;
    let mut read = vec![ReadField {
        name: "id",
        source_info: id.source_info.clone(),
        value: ReadValue::Identifier(id),
    }];
    read.extend(parens(ctx, fields)?);
    Ok(Some((
        name,
        ReadValue::Block(ReadBlock {
            source_info: ctx.cursor().span_since(&keyword.source_info),
            keyword,
            fields: read,
        }),
    )))
}

fn group_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "path" => simple(ctx, "path", identifier_value),
        _ => element(ctx, name),
    }
}

fn service_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "path" => simple(ctx, "path", identifier_value),
        "requestBody" => simple(ctx, "requestBody", type_reference),
        "method" => simple(ctx, "method", identifier_value),
        "parameters" => simple(ctx, "parameters", |ctx| {
            ctx.cursor().expect(TokenKind::LParen)?;
            let parameters = if ctx.cursor().check(TokenKind::RParen) {
                Vec::new()
            } else {
                separated(ctx, parameter)?
            };
            ctx.cursor().expect(TokenKind::RParen)?;
            Ok(ReadValue::List(parameters))
        }),
        "response" => simple(ctx, "response", type_reference),
        "security" => simple(ctx, "security", |ctx| values(ctx, identifier_value)),
        _ => Ok(None),
    }
}

/// `type` or `[type]`: a primitive type's name, or `my::Class <-
/// my::Binding`, whose `<-` is one token of the engine's lexer.
fn type_reference(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let first = ctx.cursor().peek().clone();
    let list = ctx.cursor().eat(TokenKind::LBracket);
    let start = ctx.cursor().current_source_info();
    let class = qualified_name(ctx)?;
    let mut fields = Vec::new();
    let cursor = ctx.cursor();
    if cursor.check(TokenKind::Less)
        && cursor.peek_kind_at(1) == TokenKind::Minus
        && adjacent(cursor.peek(), cursor.peek_at(1))
    {
        cursor.advance();
        cursor.advance();
        let binding = qualified_name(ctx)?;
        fields.push(ReadField {
            name: "type",
            value: ReadValue::Path(class),
            source_info: ctx.cursor().span_since(&start),
        });
        fields.push(ReadField {
            name: "binding",
            source_info: binding.source_info.clone(),
            value: ReadValue::Path(binding),
        });
    } else if class.has_package {
        // A primitive type is one name: a class needs its binding.
        return Err(unexpected(ctx.cursor().peek()));
    } else {
        fields.push(ReadField {
            name: "type",
            source_info: class.source_info.clone(),
            value: ReadValue::Path(class),
        });
    }
    if list {
        ctx.cursor().expect(TokenKind::RBracket)?;
    }
    Ok(ReadValue::Block(ReadBlock {
        source_info: ctx.cursor().span_since(&first.source_info),
        keyword: first,
        fields,
    }))
}

/// `name: type ( option, … )`, at least one option if any parentheses.
fn parameter(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let first: Token = ctx.cursor().peek().clone();
    let name = parameter_name(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let type_start = ctx.cursor().current_source_info();
    let type_reference = type_reference(ctx)?;
    let mut fields = vec![
        ReadField {
            name: "name",
            value: ReadValue::Text(name, first.source_info.clone()),
            source_info: first.source_info.clone(),
        },
        ReadField {
            name: "type",
            value: type_reference,
            source_info: ctx.cursor().span_since(&type_start),
        },
    ];
    if ctx.cursor().eat(TokenKind::LParen) {
        fields.extend(separated(ctx, option)?);
        ctx.cursor().expect(TokenKind::RParen)?;
    }
    Ok(ReadValue::Block(ReadBlock {
        source_info: ctx.cursor().span_since(&first.source_info),
        keyword: first,
        fields,
    }))
}

/// `option = value`: a boolean, a name, or for `enum` a qualified name.
fn option(ctx: &mut ParserContext<'_>) -> R<ReadField> {
    let token = ctx.cursor().peek().clone();
    let name = match token.text.as_str() {
        _ if !is_word_kind(token.kind) => return Err(unexpected(&token)),
        "allowReserved" => "allowReserved",
        "required" => "required",
        "explode" => "explode",
        "location" => "location",
        "style" => "style",
        "enum" => "enum",
        _ => return Err(unexpected(&token)),
    };
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::Equals)?;
    let value = match name {
        "location" | "style" => identifier_value(ctx)?,
        "enum" => ReadValue::Path(qualified_name(ctx)?),
        _ => {
            let value = ctx.cursor().peek().clone();
            let value = match value.kind {
                TokenKind::True => true,
                TokenKind::False => false,
                _ => return Err(unexpected(&value)),
            };
            ctx.cursor().advance();
            ReadValue::Boolean(value)
        }
    };
    Ok(ReadField {
        name,
        value,
        source_info: ctx.cursor().span_since(&token.source_info),
    })
}
