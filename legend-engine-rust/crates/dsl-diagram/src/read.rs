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

//! The section's grammar, `DiagramParserGrammar`: what it reads, before the
//! walk checks it, as blocks of fields (see
//! [`legend_pure_parser_parser::blocks`]). The core parser reads the
//! section's imports first.
//!
//! A view's id is a field `id`; a number pair, a list of its two numbers'
//! texts; a property, a pair of its class and its name.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{
    ReadBlock, ReadField, ReadValue, braces, keyword_block, simple, values,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{is_word, unexpected};
use smol_str::SmolStr;

use crate::tokens::{identifier, number, qualified_name, view_id};

type R<T> = Result<T, ParseError>;

/// `Diagram my::D { view … }`.
pub(crate) struct ReadDiagram {
    pub package: Option<Package>,
    pub name: SmolStr,
    pub name_source_info: SourceInfo,
    /// Its views; positioned from `Diagram` to `}`.
    pub body: ReadBlock,
}

/// A diagram.
pub(crate) fn diagram(ctx: &mut ParserContext<'_>) -> R<ReadDiagram> {
    let keyword = ctx.cursor().peek().clone();
    if !is_word(&keyword, "Diagram") {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    let (_, fields) = braces(ctx, view)?;
    Ok(ReadDiagram {
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

/// `classView id { … }`, `propertyView { … }` or `generalizationView { … }`.
fn view(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "classView" => {
            let keyword = ctx.cursor().advance().clone();
            let (id, id_at) = view_id(ctx)?;
            let (_, mut fields) = braces(ctx, class_view_field)?;
            fields.insert(
                0,
                ReadField {
                    name: "id",
                    value: ReadValue::Text(id, id_at.clone()),
                    source_info: id_at,
                },
            );
            Ok(Some((
                "classView",
                ReadValue::Block(ReadBlock {
                    source_info: ctx.cursor().span_since(&keyword.source_info),
                    keyword,
                    fields,
                }),
            )))
        }
        "propertyView" => Ok(Some((
            "propertyView",
            ReadValue::Block(keyword_block(ctx, property_view_field)?),
        ))),
        "generalizationView" => Ok(Some((
            "generalizationView",
            ReadValue::Block(keyword_block(ctx, relationship_view_field)?),
        ))),
        _ => Ok(None),
    }
}

fn class_view_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "class" => simple(ctx, "class", |ctx| {
            Ok(ReadValue::Path(qualified_name(ctx)?))
        }),
        "position" => simple(ctx, "position", number_pair),
        "rectangle" => simple(ctx, "rectangle", number_pair),
        "hideProperties" => simple(ctx, "hideProperties", boolean),
        "hideTaggedValue" => simple(ctx, "hideTaggedValue", boolean),
        "hideStereotype" => simple(ctx, "hideStereotype", boolean),
        _ => Ok(None),
    }
}

fn property_view_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        // `my::Class.property`.
        "property" => simple(ctx, "property", |ctx| {
            let class = qualified_name(ctx)?;
            let start = class.source_info.clone();
            ctx.cursor().expect(TokenKind::Dot)?;
            let property = identifier(ctx)?;
            Ok(ReadValue::Pair(
                Box::new(ReadValue::Path(class)),
                Box::new(ReadValue::Identifier(property)),
                ctx.cursor().span_since(&start),
            ))
        }),
        _ => relationship_view_field(ctx, name),
    }
}

fn relationship_view_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    let id = |ctx: &mut ParserContext<'_>| {
        let (id, id_at) = view_id(ctx)?;
        Ok(ReadValue::Text(id, id_at))
    };
    match name {
        "source" => simple(ctx, "source", id),
        "target" => simple(ctx, "target", id),
        "points" => simple(ctx, "points", |ctx| values(ctx, number_pair)),
        _ => Ok(None),
    }
}

/// `(number, number)`.
fn number_pair(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    ctx.cursor().expect(TokenKind::LParen)?;
    let (first, first_at) = number(ctx)?;
    ctx.cursor().expect(TokenKind::Comma)?;
    let (second, second_at) = number(ctx)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(ReadValue::List(vec![
        ReadValue::Text(first, first_at),
        ReadValue::Text(second, second_at),
    ]))
}

fn boolean(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let token = ctx.cursor().peek().clone();
    let value = match token.kind {
        TokenKind::True => true,
        TokenKind::False => false,
        _ => return Err(unexpected(&token)),
    };
    ctx.cursor().advance();
    Ok(ReadValue::Boolean(value))
}
