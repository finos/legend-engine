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

//! `SectionParser` for `###FunctionJar`: the engine's
//! `FunctionJarParserGrammar` (the reader), then its `FunctionJarTreeWalker`
//! (the walk), each function jar in turn; and its elements'
//! `ActivatorProtocol`. The section has imports.

use legend_pure_dsl_function_activator::element::ActivatorProtocol;
use legend_pure_dsl_function_activator::read::{
    ReadActivator, activator, deployment, documentation, function,
};
use legend_pure_dsl_function_activator::walk::{self, deployment_owner};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::blocks::{
    ReadBlock, ReadField, ReadValue, path_value, simple, string_value,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{is_word, unexpected};
use legend_pure_parser_parser::{ParserContext, SectionParser};
use legend_pure_parser_protocol::element_protocol::{ELEMENT_PROTOCOLS, ElementProtocol};
use legend_pure_parser_protocol::v1::record::Record;
use linkme::distributed_slice;

use crate::SECTION_KIND;

type R<T> = Result<T, ParseError>;

/// Parser for `###FunctionJar` sections.
pub struct FunctionJarSectionParser;

impl SectionParser for FunctionJarSectionParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        match parse_body(ctx) {
            Ok(elements) => elements,
            // The engine reports the first error.
            Err(error) => {
                errors.push(error);
                Vec::new()
            }
        }
    }
}

/// The section read whole, then walked.
fn parse_body(ctx: &mut ParserContext<'_>) -> R<Vec<Box<dyn DSLElement>>> {
    let mut read = Vec::new();
    while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
        read.push(function_jar(ctx)?);
    }
    read.into_iter()
        .map(|jar| {
            Ok(
                Box::new(walk::activator(jar, SECTION_KIND, walk_function_jar)?)
                    as Box<dyn DSLElement>,
            )
        })
        .collect()
}

/// `'''doc''' FunctionJar <<s>> {t} my::J { field … }`.
fn function_jar(ctx: &mut ParserContext<'_>) -> R<ReadActivator> {
    let start = ctx.cursor().current_source_info();
    let documentation = documentation(ctx);
    let keyword = ctx.cursor().peek().clone();
    if !is_word(&keyword, "FunctionJar") {
        return Err(unexpected(&keyword));
    }
    activator(ctx, &start, documentation, field)
}

fn field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "function" => simple(ctx, "function", function),
        "ownership" => simple(ctx, "ownership", deployment),
        "documentation" => simple(ctx, "documentation", string_value),
        // Read and left out, as the walker leaves it.
        "activationConfiguration" => simple(ctx, "activationConfiguration", path_value),
        _ => Ok(None),
    }
}

/// The walker's `visitFunctionJar`: the function jar's own record, without
/// its package, name and annotations.
fn walk_function_jar(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let function = walk::function(read.take("function", &at)?)?;
    let ownership = deployment_owner(read.take("ownership", &at)?)?;
    let documentation = read
        .take_optional("documentation", &at)?
        .map(ReadField::string)
        .transpose()?;
    Ok(Record::new("functionJar")
        .at(&at)
        .with("function", function)
        .with("ownership", ownership)
        .with_some("documentation", documentation))
}

#[distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static FUNCTION_JAR_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &FunctionJarSectionParser;

#[distributed_slice(ELEMENT_PROTOCOLS)]
static FUNCTION_JAR_PROTOCOL: &(dyn ElementProtocol + Send + Sync) =
    &ActivatorProtocol(SECTION_KIND);
