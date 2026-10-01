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

//! The section's grammar, `ElasticsearchParserGrammar`: what it reads,
//! before the walk checks it, as blocks of fields (see
//! [`legend_pure_parser_parser::blocks`]). The core parser reads the
//! section's imports first.
//!
//! An index, and a named property, is a block named by its name; a
//! property's type, a block whose keyword is the type and whose one field,
//! if any, is its `content`, `{ … }`.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{
    ReadBlock, ReadField, ReadValue, bare_block, braces, simple, some_values,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{Name, identifier, is_word, is_word_kind, unexpected};
use smol_str::SmolStr;

type R<T> = Result<T, ParseError>;

/// The grammar's keywords, which a `VALID_STRING` isn't.
const KEYWORDS: [&str; 18] = [
    "import",
    "Elasticsearch7Cluster",
    "indices",
    "properties",
    "fields",
    "Keyword",
    "Text",
    "Date",
    "Short",
    "Byte",
    "Integer",
    "Long",
    "Float",
    "HalfFloat",
    "Double",
    "Boolean",
    "Object",
    "Nested",
];

/// The scalar property types.
pub(crate) const SCALAR_TYPES: [&str; 11] = [
    "Keyword",
    "Text",
    "Date",
    "Short",
    "Byte",
    "Integer",
    "Long",
    "Float",
    "HalfFloat",
    "Double",
    "Boolean",
];

/// The complex property types.
pub(crate) const COMPLEX_TYPES: [&str; 2] = ["Object", "Nested"];

/// `Elasticsearch7Cluster my::S { … }`.
pub(crate) struct ReadStore {
    pub package: Option<Package>,
    pub name: SmolStr,
    pub name_source_info: SourceInfo,
    /// Its indices; positioned from `Elasticsearch7Cluster` to `}`.
    pub body: ReadBlock,
}

/// A store.
pub(crate) fn store(ctx: &mut ParserContext<'_>) -> R<ReadStore> {
    let keyword = ctx.cursor().peek().clone();
    if !is_word(&keyword, "Elasticsearch7Cluster") {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    let (_, fields) = braces(ctx, |ctx: &mut ParserContext<'_>, name: &str| match name {
        "indices" => simple(ctx, "'indices'", |ctx| {
            some_values(ctx, |ctx| {
                named(ctx, |ctx| {
                    Ok(ReadValue::Block(bare_block(ctx, properties_field)?))
                })
            })
        }),
        _ => Ok(None),
    })?;
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

/// `STRING | VALID_STRING`: a quoted name, or a word but one of the
/// grammar's keywords.
fn name(ctx: &mut ParserContext<'_>) -> R<Name> {
    let token = ctx.cursor().peek().clone();
    if is_word_kind(token.kind) && KEYWORDS.contains(&token.text.as_str()) {
        return Err(unexpected(&token));
    }
    identifier(ctx)
}

/// `name: value`, from the name to the value's end.
fn named(
    ctx: &mut ParserContext<'_>,
    value: impl FnOnce(&mut ParserContext<'_>) -> R<ReadValue>,
) -> R<ReadValue> {
    let name = name(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let value = value(ctx)?;
    let source_info = ctx.cursor().span_since(&name.source_info);
    Ok(ReadValue::Named(name, Box::new(value), source_info))
}

/// `properties: [ … ];`.
fn properties_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "properties" => simple(ctx, "'properties'", properties),
        _ => Ok(None),
    }
}

/// `[ name: Type, … ]`: at least one.
fn properties(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    some_values(ctx, |ctx| named(ctx, property_type))
}

/// A scalar type, its `{ fields: [ … ]; }` optional, or a complex one, its
/// `{ properties: [ … ]; }` required.
fn property_type(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let keyword = ctx.cursor().peek().clone();
    let text = keyword.text.as_str();
    let scalar = SCALAR_TYPES.contains(&text);
    if !is_word_kind(keyword.kind) || !(scalar || COMPLEX_TYPES.contains(&text)) {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
    let mut fields = Vec::new();
    if !scalar || ctx.cursor().check(TokenKind::LBrace) {
        let content = if scalar {
            bare_block(ctx, |ctx: &mut ParserContext<'_>, name: &str| match name {
                "fields" => simple(ctx, "'fields'", properties),
                _ => Ok(None),
            })?
        } else {
            bare_block(ctx, properties_field)?
        };
        fields.push(ReadField {
            name: "content",
            source_info: content.source_info.clone(),
            value: ReadValue::Block(content),
        });
    }
    Ok(ReadValue::Block(ReadBlock {
        source_info: ctx.cursor().span_since(&keyword.source_info),
        keyword,
        fields,
    }))
}
