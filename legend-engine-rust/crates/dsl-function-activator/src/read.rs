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

//! An activator as its grammar reads it, before the walk checks it: its
//! header, then its fields, which its section's grammar reads as a block of
//! fields (see [`legend_pure_parser_parser::blocks`]); and the readers of
//! the fields most activators have.

use legend_pure_parser_ast::annotation::{StereotypePtr, TaggedValue};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{Fields, ReadBlock, ReadValue, braces, string_value};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::function_identifier;
use legend_pure_parser_parser::words::{expect_word, is_string};
use smol_str::SmolStr;

type R<T> = Result<T, ParseError>;

/// `'''doc''' Keyword <<s>> {t} my::A { field … }`.
pub struct ReadActivator {
    /// The documentation before the keyword, a string.
    pub documentation: Option<Token>,
    /// `<<profile.stereotype>>`.
    pub stereotypes: Vec<StereotypePtr>,
    /// `{profile.tag = 'value'}`.
    pub tagged_values: Vec<TaggedValue>,
    /// The package.
    pub package: Option<Package>,
    /// The name.
    pub name: SmolStr,
    /// The name's position.
    pub name_source_info: SourceInfo,
    /// The keyword and the fields; positioned from the documentation, or
    /// the keyword, to `}`.
    pub body: ReadBlock,
}

/// The documentation before an element's keyword, if any: a string, which
/// the walk requires to be a text block.
pub fn documentation(ctx: &mut ParserContext<'_>) -> Option<Token> {
    is_string(ctx.cursor().peek()).then(|| ctx.cursor().advance().clone())
}

/// An activator from `start`, its documentation read and its keyword next:
/// its annotations, its name, and its fields, which `fields` reads.
///
/// # Errors
///
/// A syntax error, or `fields`'s.
pub fn activator(
    ctx: &mut ParserContext<'_>,
    start: &SourceInfo,
    documentation: Option<Token>,
    fields: impl Fields,
) -> R<ReadActivator> {
    let keyword = ctx.cursor().advance().clone();
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
    let (_, fields) = braces(ctx, fields)?;
    Ok(ReadActivator {
        documentation,
        stereotypes,
        tagged_values,
        package,
        name,
        name_source_info,
        body: ReadBlock {
            source_info: ctx.cursor().span_since(start),
            keyword,
            fields,
        },
    })
}

/// `my::f(Type[m], …):Type[m]`, as written without spaces, positioned (the
/// grammars' `functionIdentifier`).
///
/// # Errors
///
/// A malformed identifier.
pub fn function(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let start = ctx.cursor().current_source_info();
    let text = function_identifier(ctx)?;
    Ok(ReadValue::Text(text, ctx.cursor().span_since(&start)))
}

/// `Deployment { identifier: 'id' }`: the identifier.
///
/// # Errors
///
/// Anything else next.
pub fn deployment(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    expect_word(ctx, "Deployment")?;
    ctx.cursor().expect(TokenKind::LBrace)?;
    expect_word(ctx, "identifier")?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let id = string_value(ctx)?;
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(id)
}
