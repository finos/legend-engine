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

//! The section's grammar, `DataSpaceParserGrammar`: what it reads, before
//! the walk checks it, as blocks of fields (see
//! [`legend_pure_parser_parser::blocks`]).

use legend_pure_parser_ast::annotation::{StereotypePtr, TaggedValue};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{
    Fields, ReadBlock, ReadField, ReadValue, bare_block, braces, choice, expression_until,
    identifier_value, keyword_block, path_value, simple, string_value, typed_island, values,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{is_string, is_word, qualified_name, unexpected};
use smol_str::SmolStr;

type R<T> = Result<T, ParseError>;

/// `'''doc''' DataSpace <<s>> {t} my::D { field … }`.
pub(crate) struct ReadDataSpace {
    pub documentation: Option<Token>,
    pub stereotypes: Vec<StereotypePtr>,
    pub tagged_values: Vec<TaggedValue>,
    pub package: Option<Package>,
    pub name: SmolStr,
    pub name_source_info: SourceInfo,
    /// The fields; positioned from the documentation, or `DataSpace`, to `}`.
    pub body: ReadBlock,
}

/// A data space.
pub(crate) fn data_space(ctx: &mut ParserContext<'_>) -> R<ReadDataSpace> {
    let start = ctx.cursor().current_source_info();
    let documentation = is_string(ctx.cursor().peek()).then(|| ctx.cursor().advance().clone());
    let keyword = ctx.cursor().peek().clone();
    if !is_word(&keyword, "DataSpace") {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
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
    let (_, fields) = braces(ctx, data_space_field)?;
    Ok(ReadDataSpace {
        documentation,
        stereotypes,
        tagged_values,
        package,
        name,
        name_source_info,
        body: ReadBlock {
            source_info: ctx.cursor().span_since(&start),
            keyword,
            fields,
        },
    })
}

/// A block of `fields` without a keyword, as a value.
fn bare(ctx: &mut ParserContext<'_>, fields: impl Fields) -> R<ReadValue> {
    Ok(ReadValue::Block(bare_block(ctx, fields)?))
}

fn data_space_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "executionContexts" => simple(ctx, "executionContexts", |ctx| {
            values(ctx, |ctx| bare(ctx, execution_context_field))
        }),
        "defaultExecutionContext" => simple(ctx, "defaultExecutionContext", string_value),
        "title" => simple(ctx, "title", string_value),
        "description" => simple(ctx, "description", string_value),
        "diagrams" => simple(ctx, "diagrams", |ctx| {
            values(ctx, |ctx| bare(ctx, diagram_field))
        }),
        "elements" => simple(ctx, "elements", |ctx| values(ctx, element_scope_path)),
        "executables" => simple(ctx, "executables", |ctx| {
            values(ctx, |ctx| bare(ctx, executable_field))
        }),
        "supportInfo" => simple(ctx, "supportInfo", support_info),
        "operationalMetadata" => simple(ctx, "operationalMetadata", |ctx| {
            bare(ctx, operational_metadata_field)
        }),
        // Deprecated: read, not walked.
        "groupId" => simple(ctx, "groupId", string_value),
        "artifactId" => simple(ctx, "artifactId", string_value),
        "versionId" => simple(ctx, "versionId", string_value),
        "featuredDiagrams" => simple(ctx, "featuredDiagrams", |ctx| values(ctx, path_value)),
        _ => Ok(None),
    }
}

fn execution_context_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "name" => simple(ctx, "name", string_value),
        "title" => simple(ctx, "title", string_value),
        "description" => simple(ctx, "description", string_value),
        "mapping" => simple(ctx, "mapping", path_value),
        // `my::Provider (. key, …)?`.
        "mappingProvider" => simple(ctx, "mappingProvider", |ctx| {
            let start = ctx.cursor().current_source_info();
            let element = path_value(ctx)?;
            let mut keys = Vec::new();
            if ctx.cursor().eat(TokenKind::Dot) {
                keys.push(identifier_value(ctx)?);
                while ctx.cursor().eat(TokenKind::Comma) {
                    keys.push(identifier_value(ctx)?);
                }
            }
            Ok(ReadValue::Pair(
                Box::new(element),
                Box::new(ReadValue::List(keys)),
                ctx.cursor().span_since(&start),
            ))
        }),
        "defaultRuntime" => simple(ctx, "defaultRuntime", path_value),
        "testData" => simple(ctx, "testData", typed_island),
        _ => Ok(None),
    }
}

fn diagram_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "title" => simple(ctx, "title", string_value),
        "description" => simple(ctx, "description", string_value),
        "diagram" => simple(ctx, "diagram", path_value),
        _ => Ok(None),
    }
}

/// `(-)? my::Element`: a block whose keyword is its first token, holding
/// the path as `path`.
fn element_scope_path(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let first = ctx.cursor().peek().clone();
    ctx.cursor().eat(TokenKind::Minus);
    let path_start = ctx.cursor().current_source_info();
    let path = qualified_name(ctx)?;
    let path_at = ctx.cursor().span_since(&path_start);
    Ok(ReadValue::Block(ReadBlock {
        source_info: ctx.cursor().span_since(&first.source_info),
        keyword: first,
        fields: vec![ReadField {
            name: "path",
            value: ReadValue::Path(path),
            source_info: path_at,
        }],
    }))
}

fn executable_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "title" => simple(ctx, "title", string_value),
        "description" => simple(ctx, "description", string_value),
        // A path, or a function's identifier, `my::f(String[1]):String[1]`,
        // which the walker reads as written, without spaces (`getText()`).
        "executable" => simple(ctx, "executable", |ctx| {
            let block = ctx.read_until(TokenKind::Semicolon)?;
            if block.text().contains('(') {
                return Ok(ReadValue::Text(block.text(), block.source_info));
            }
            ctx.parse_block(&block, path_value)
        }),
        "query" => simple(ctx, "query", |ctx| {
            let query = expression_until(ctx, TokenKind::Semicolon)?;
            Ok(ReadValue::Expression(Box::new(query)))
        }),
        // `VALID_STRING | INTEGER | DECIMAL`, as written.
        "id" => simple(ctx, "id", |ctx| {
            let token = ctx.cursor().peek().clone();
            let word = legend_pure_parser_parser::words::is_word_kind(token.kind);
            if !word
                && !matches!(
                    token.kind,
                    TokenKind::IntegerLiteral | TokenKind::DecimalLiteral
                )
            {
                return Err(unexpected(&token));
            }
            ctx.cursor().advance();
            Ok(ReadValue::String(token))
        }),
        "executionContextKey" => simple(ctx, "executionContextKey", string_value),
        "sampleValues" => simple(ctx, "sampleValues", typed_island),
        _ => Ok(None),
    }
}

/// `Email { … }`, `Combined { … }` or `{ … }`.
fn support_info(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    if ctx.cursor().check(TokenKind::LBrace) {
        return bare(ctx, full_info_field);
    }
    choice(
        ctx,
        &mut [
            ("Email", &mut |ctx| {
                keyword_block(ctx, |ctx, name| match name {
                    "documentationUrl" => simple(ctx, "documentationUrl", string_value),
                    "address" => simple(ctx, "address", string_value),
                    _ => Ok(None),
                })
            }),
            ("Combined", &mut |ctx| {
                keyword_block(ctx, |ctx, name| match name {
                    "documentationUrl" => simple(ctx, "documentationUrl", string_value),
                    "emails" => simple(ctx, "emails", |ctx| values(ctx, string_value)),
                    "website" => simple(ctx, "website", string_value),
                    "faqUrl" => simple(ctx, "faqUrl", string_value),
                    "supportUrl" => simple(ctx, "supportUrl", string_value),
                    _ => Ok(None),
                })
            }),
        ],
    )
}

fn full_info_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "documentation" => simple(ctx, "documentation", |ctx| bare(ctx, link_field)),
        "website" => simple(ctx, "website", |ctx| bare(ctx, link_field)),
        "faqUrl" => simple(ctx, "faqUrl", |ctx| bare(ctx, link_field)),
        "supportUrl" => simple(ctx, "supportUrl", |ctx| bare(ctx, link_field)),
        "emails" => simple(ctx, "emails", |ctx| {
            values(ctx, |ctx| {
                bare(ctx, |ctx, name| match name {
                    "title" => simple(ctx, "title", string_value),
                    "address" => simple(ctx, "address", string_value),
                    _ => Ok(None),
                })
            })
        }),
        "expertise" => simple(ctx, "expertise", |ctx| {
            values(ctx, |ctx| {
                bare(ctx, |ctx, name| match name {
                    "description" => simple(ctx, "description", string_value),
                    "expertIds" => simple(ctx, "expertIds", |ctx| values(ctx, string_value)),
                    _ => Ok(None),
                })
            })
        }),
        _ => Ok(None),
    }
}

fn link_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "label" => simple(ctx, "label", string_value),
        "url" => simple(ctx, "url", string_value),
        _ => Ok(None),
    }
}

fn operational_metadata_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "coverageRegions" => simple(ctx, "coverageRegions", |ctx| values(ctx, identifier_value)),
        "updateFrequency" => simple(ctx, "updateFrequency", identifier_value),
        _ => Ok(None),
    }
}
