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

//! `SectionParser` for `###Snowflake`: the engine's `SnowflakeParserGrammar`
//! (the reader), then its `SnowflakeTreeWalker` (the walk), its apps first,
//! then its UDFs; and its elements' `ActivatorProtocol`. The section has no
//! imports.

use legend_pure_dsl_function_activator::element::ActivatorProtocol;
use legend_pure_dsl_function_activator::walk::activator;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{ParserContext, SectionParser};
use legend_pure_parser_protocol::element_protocol::{ELEMENT_PROTOCOLS, ElementProtocol};
use linkme::distributed_slice;

use crate::SECTION_KIND;
use crate::read::{SnowflakeKind, element};
use crate::walk;

/// Parser for `###Snowflake` sections.
pub struct SnowflakeSectionParser;

impl SectionParser for SnowflakeSectionParser {
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
        let mut read = Vec::new();
        while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
            match element(ctx) {
                Ok(Some(element)) => read.push(element),
                Ok(None) => {}
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        // The walker walks the apps, then the UDFs.
        let (apps, udfs): (Vec<_>, Vec<_>) = read
            .into_iter()
            .partition(|element| element.kind == SnowflakeKind::App);
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for element in apps.into_iter().chain(udfs) {
            let kind = element.kind;
            match activator(element.activator, SECTION_KIND, |body| {
                walk::element(kind, body)
            }) {
                Ok(element) => elements.push(Box::new(element)),
                // The engine reports the first error.
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        elements
    }
}

#[distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static SNOWFLAKE_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &SnowflakeSectionParser;

#[distributed_slice(ELEMENT_PROTOCOLS)]
static SNOWFLAKE_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &ActivatorProtocol(SECTION_KIND);
