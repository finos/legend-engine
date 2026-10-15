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

//! `SectionParser` for `###HostedService`: the engine's
//! `HostedServiceParserGrammar` (the reader), then its
//! `HostedServiceTreeWalker` (the walk), its hosted services first, then
//! its execution environments, then its deployment configurations; and
//! its hosted services' `ActivatorProtocol` (`dsl-service` converts its
//! execution environments). The section has imports.

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
use crate::walk::{environment, hosted_service};

/// Parser for `###HostedService` sections.
pub struct HostedServiceSectionParser;

impl SectionParser for HostedServiceSectionParser {
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
fn parse_body(ctx: &mut ParserContext<'_>) -> Result<Vec<Box<dyn DSLElement>>, ParseError> {
    let (mut services, mut environments, mut configurations) = (Vec::new(), Vec::new(), Vec::new());
    while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
        match element(ctx)? {
            ReadElement::Service(service) => services.push(*service),
            ReadElement::Environment(environment) => environments.push(environment),
            ReadElement::Configuration(source_info) => configurations.push(source_info),
        }
    }
    // The walker walks the hosted services, then the execution
    // environments, then the deployment configurations.
    let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
    for service in services {
        elements.push(Box::new(activator(service, SECTION_KIND, hosted_service)?));
    }
    for read in environments {
        elements.push(Box::new(environment(ctx, read)?));
    }
    if let Some(source_info) = configurations.into_iter().next() {
        return Err(ParseError::Unsupported {
            message: "the engine makes a hosted service deployment configuration an element \
                      without a name, which Rust doesn't reproduce"
                .to_string(),
            source_info,
        });
    }
    Ok(elements)
}

#[distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static HOSTED_SERVICE_SECTION_PARSER: &(dyn SectionParser + Send + Sync) =
    &HostedServiceSectionParser;

#[distributed_slice(ELEMENT_PROTOCOLS)]
static HOSTED_SERVICE_PROTOCOL: &(dyn ElementProtocol + Send + Sync) =
    &ActivatorProtocol(SECTION_KIND);
