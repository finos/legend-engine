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

use legend_pure_dsl_function_activator::read::{
    ReadActivator, activator, deployment, documentation, function,
};
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{
    ReadValue, identifier_value, path_value, simple, string_value,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{expect_word, is_word, qualified_name, unexpected};

type R<T> = Result<T, ParseError>;

/// How a function is activated.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum SnowflakeKind {
    /// `SnowflakeApp`: as a Snowflake app.
    App,
    /// `SnowflakeM2MUdf`: as a model-to-model UDF.
    M2MUdf,
}

/// An app or a UDF, as read.
pub(crate) struct ReadElement {
    pub kind: SnowflakeKind,
    pub activator: ReadActivator,
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
    let documentation = documentation(ctx);
    let keyword = ctx.cursor().peek().clone();
    let (kind, activator) = if is_word(&keyword, "SnowflakeApp") {
        (
            SnowflakeKind::App,
            activator(ctx, &start, documentation, app_field)?,
        )
    } else if is_word(&keyword, "SnowflakeM2MUdf") {
        (
            SnowflakeKind::M2MUdf,
            activator(ctx, &start, documentation, udf_field)?,
        )
    } else {
        return Err(unexpected(&keyword));
    };
    Ok(Some(ReadElement { kind, activator }))
}

/// The fields both an app and a UDF have.
fn common_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "description" => simple(ctx, "description", string_value),
        "function" => simple(ctx, "function", function),
        "ownership" => simple(ctx, "ownership", deployment),
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
