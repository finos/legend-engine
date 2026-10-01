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

//! `SectionParser` and `ElementProtocol` for `###ExternalFormat`: the
//! engine's `ExternalFormatParserGrammar` (the reader), then its
//! `ExternalFormatParseTreeWalker` (the walk), its schema sets first, then
//! its bindings; and the elements' JSON.

use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{ParserContext, SectionParser};
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use linkme::distributed_slice;
use serde_json::{Value, json};

use crate::ast::{ExternalFormatElement, ExternalFormatKind, SECTION_KIND};
use crate::read::element;
use crate::walk;

/// Parser for `###ExternalFormat` sections.
pub struct ExternalFormatSectionParser;

impl SectionParser for ExternalFormatSectionParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        let mut read = Vec::new();
        while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
            match element(ctx) {
                Ok(element) => read.push(element),
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        // The walker walks the schema sets, then the bindings.
        let (schema_sets, bindings): (Vec<_>, Vec<_>) = read
            .into_iter()
            .partition(|element| element.kind == ExternalFormatKind::SchemaSet);
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for element in schema_sets.into_iter().chain(bindings) {
            let source_info = element.body.source_info.clone();
            match walk::element(element.kind, element.body) {
                Ok(record) => elements.push(Box::new(ExternalFormatElement {
                    kind: element.kind,
                    package: element.package,
                    name: SpannedString {
                        value: element.name,
                        source_info: element.name_source_info,
                    },
                    record,
                    source_info,
                })),
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
static EXTERNAL_FORMAT_SECTION_PARSER: &(dyn SectionParser + Send + Sync) =
    &ExternalFormatSectionParser;

/// Protocol converter for schema sets and bindings: their records' JSON,
/// with their package and name.
pub struct ExternalFormatProtocol;

impl ElementProtocol for ExternalFormatProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Value> {
        let element = element
            .as_any()
            .downcast_ref::<ExternalFormatElement>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "ExternalFormatProtocol: a '{}' element is not an ExternalFormatElement",
                        element.kind()
                    ),
                    None,
                )
            })?;
        let mut json = element.record.to_json()?;
        json["package"] = json!(
            element
                .package
                .as_ref()
                .map(ToString::to_string)
                .unwrap_or_default()
        );
        json["name"] = json!(element.name.value.as_str());
        Ok(json)
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static EXTERNAL_FORMAT_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &ExternalFormatProtocol;
