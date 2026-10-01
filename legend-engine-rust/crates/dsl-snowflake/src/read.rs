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

//! The section's grammar, `SnowflakeParserGrammar`: what it reads, before
//! the walk checks it, as blocks of fields (see
//! [`legend_pure_parser_parser::blocks`]), named as the walker names them
//! (`usageRole` is its `role`, `permissionScheme` its `scheme`,
//! `activationConfiguration` its `activation`).

use legend_pure_parser_ast::annotation::{StereotypePtr, TaggedValue};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{
    ReadBlock, ReadValue, braces, identifier_value, path_value, simple, string_value,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::function_identifier;
use legend_pure_parser_parser::words::{
    expect_word, is_string, is_word, qualified_name, unexpected,
};
use smol_str::SmolStr;

use crate::ast::SnowflakeKind;

type R<T> = Result<T, ParseError>;

/// `'''doc''' Keyword <<s>> {t} my::E { field … }`.
pub(crate) struct ReadElement {
    pub kind: SnowflakeKind,
    pub documentation: Option<Token>,
    pub stereotypes: Vec<StereotypePtr>,
    pub tagged_values: Vec<TaggedValue>,
    pub package: Option<Package>,
    pub name: SmolStr,
    pub name_source_info: SourceInfo,
    /// The fields; positioned from the documentation, or the keyword, to
    /// `}`.
    pub body: ReadBlock,
}

/// An app or a UDF; or a deployment configuration, read and left out
/// (`None`), as the walker leaves it.
pub(crate) fn element(ctx: &mut ParserContext<'_>) -> R<Option<ReadElement>> {
    let start = ctx.cursor().current_source_info();
    if is_word(ctx.cursor().peek(), "SnowflakeAppDeploymentConfiguration") {
        ctx.cursor().advance();
        qualified_name(ctx)?;
        ctx.cursor().expect(TokenKind::LBrace)?;
        expect_word(ctx, "activationConnection")?;
        ctx.cursor().expect(TokenKind::Colon)?;
        qualified_name(ctx)?;
        ctx.cursor().expect(TokenKind::Semicolon)?;
        ctx.cursor().expect(TokenKind::RBrace)?;
        return Ok(None);
    }
    let documentation = is_string(ctx.cursor().peek()).then(|| ctx.cursor().advance().clone());
    let keyword = ctx.cursor().peek().clone();
    let kind = if is_word(&keyword, "SnowflakeApp") {
        SnowflakeKind::App
    } else if is_word(&keyword, "SnowflakeM2MUdf") {
        SnowflakeKind::M2MUdf
    } else {
        return Err(unexpected(&keyword));
    };
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
    let (_, fields) = match kind {
        SnowflakeKind::App => braces(ctx, app_field)?,
        SnowflakeKind::M2MUdf => braces(ctx, udf_field)?,
    };
    Ok(Some(ReadElement {
        kind,
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
    }))
}

/// `my::f(Type[m], …):Type[m]`, as written without spaces.
fn function(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let start = ctx.cursor().current_source_info();
    let text = function_identifier(ctx)?;
    Ok(ReadValue::Text(text, ctx.cursor().span_since(&start)))
}

/// `Deployment { identifier: 'id' }`.
fn ownership(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    expect_word(ctx, "Deployment")?;
    ctx.cursor().expect(TokenKind::LBrace)?;
    expect_word(ctx, "identifier")?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let id = string_value(ctx)?;
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(id)
}

/// The fields both an app and a UDF have.
fn common_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "description" => simple(ctx, "description", string_value),
        "function" => simple(ctx, "function", function),
        "ownership" => simple(ctx, "ownership", ownership),
        "activationConfiguration" => simple(ctx, "activation", path_value),
        "deploymentSchema" => simple(ctx, "deploymentSchema", string_value),
        _ => Ok(None),
    }
}

fn app_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "applicationName" => simple(ctx, "applicationName", string_value),
        "usageRole" => simple(ctx, "role", string_value),
        "permissionScheme" => simple(ctx, "scheme", identifier_value),
        _ => common_field(ctx, name),
    }
}

fn udf_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "udfName" => simple(ctx, "udfName", string_value),
        "deploymentStage" => simple(ctx, "deploymentStage", string_value),
        _ => common_field(ctx, name),
    }
}
