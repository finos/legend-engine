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

//! `SectionParser` for `###BigQuery`: the engine's
//! `BigQueryFunctionParserGrammar` (the reader), then its
//! `BigQueryFunctionTreeWalker` (the walk), its functions first, then its
//! deployment configurations; and its elements' `ActivatorProtocol`. The
//! section has no imports.

use legend_pure_dsl_function_activator::element::ActivatorProtocol;
use legend_pure_dsl_function_activator::walk::activator;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{ParserContext, SectionParser};
use legend_pure_parser_protocol::element_protocol::{ELEMENT_PROTOCOLS, ElementProtocol};
use linkme::distributed_slice;

use crate::SECTION_KIND;
use crate::read::{ReadElement, element};
use crate::walk::function_activator;

/// Parser for `###BigQuery` sections.
pub struct BigQuerySectionParser;

impl SectionParser for BigQuerySectionParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn imports(&self) -> bool {
        false
    }

    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        let mut functions = Vec::new();
        let mut configurations = Vec::new();
        while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
            match element(ctx) {
                Ok(ReadElement::Function(function)) => functions.push(*function),
                Ok(ReadElement::Configuration(source_info)) => configurations.push(source_info),
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        // The walker walks the functions, then the configurations.
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for function in functions {
            match activator(function, SECTION_KIND, function_activator) {
                Ok(element) => elements.push(Box::new(element)),
                // The engine reports the first error.
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        if let Some(source_info) = configurations.into_iter().next() {
            errors.push(ParseError::Unsupported {
                message:
                    "the engine makes a BigQuery function deployment configuration an element \
                          without a name, which Rust doesn't reproduce"
                        .to_string(),
                source_info,
            });
            return Vec::new();
        }
        elements
    }
}

#[distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static BIGQUERY_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &BigQuerySectionParser;

#[distributed_slice(ELEMENT_PROTOCOLS)]
static BIGQUERY_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &ActivatorProtocol(SECTION_KIND);
