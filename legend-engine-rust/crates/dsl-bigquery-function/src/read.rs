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

//! The section's grammar, `BigQueryFunctionParserGrammar`: what it reads,
//! before the walk checks it. A function's fields are read as a block of
//! fields (see [`legend_pure_parser_parser::blocks`]), named as the walker
//! names them (`ownership` is its `owner`).

use legend_pure_dsl_function_activator::read::{
    ReadActivator, activator, deployment, documentation, function,
};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{ReadValue, path_value, simple, string_value};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{expect_word, is_word, qualified_name, unexpected};

type R<T> = Result<T, ParseError>;

/// A function, or a deployment configuration.
pub(crate) enum ReadElement {
    /// `BigQueryFunction my::F { … }`.
    Function(Box<ReadActivator>),
    /// `BigQueryFunctionDeploymentConfiguration my::C { … }`, from its
    /// keyword to `}`.
    Configuration(SourceInfo),
}

/// A function or a deployment configuration.
pub(crate) fn element(ctx: &mut ParserContext<'_>) -> R<ReadElement> {
    let start = ctx.cursor().current_source_info();
    if is_word(
        ctx.cursor().peek(),
        "BigQueryFunctionDeploymentConfiguration",
    ) {
        ctx.cursor().advance();
        qualified_name(ctx)?;
        ctx.cursor().expect(TokenKind::LBrace)?;
        expect_word(ctx, "activationConnection")?;
        ctx.cursor().expect(TokenKind::Colon)?;
        qualified_name(ctx)?;
        ctx.cursor().expect(TokenKind::Semicolon)?;
        ctx.cursor().expect(TokenKind::RBrace)?;
        return Ok(ReadElement::Configuration(ctx.cursor().span_since(&start)));
    }
    let documentation = documentation(ctx);
    let keyword = ctx.cursor().peek().clone();
    if !is_word(&keyword, "BigQueryFunction") {
        return Err(unexpected(&keyword));
    }
    Ok(ReadElement::Function(Box::new(activator(
        ctx,
        &start,
        documentation,
        field,
    )?)))
}

fn field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "functionName" => simple(ctx, "functionName", string_value),
        "description" => simple(ctx, "description", string_value),
        "function" => simple(ctx, "function", function),
        // The walker names it `owner`.
        "ownership" => simple(ctx, "owner", deployment),
        "activationConfiguration" => simple(ctx, "activationConfiguration", path_value),
        _ => Ok(None),
    }
}
