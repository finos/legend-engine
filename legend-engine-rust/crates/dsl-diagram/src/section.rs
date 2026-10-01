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

//! `SectionParser` and `ElementProtocol` for `###Diagram`: the engine's
//! `DiagramParserGrammar` (the reader), then its `DiagramParseTreeWalker`
//! (the walk), each diagram in turn; and the diagrams' JSON.

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

use crate::ast::{Diagram, SECTION_KIND};
use crate::read::diagram;
use crate::walk;

/// Parser for `###Diagram` sections.
pub struct DiagramSectionParser;

impl SectionParser for DiagramSectionParser {
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
            match diagram(ctx) {
                Ok(diagram) => read.push(diagram),
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for diagram in read {
            let source_info = diagram.body.source_info.clone();
            match walk::diagram(diagram.body) {
                Ok(record) => elements.push(Box::new(Diagram {
                    package: diagram.package,
                    name: SpannedString {
                        value: diagram.name,
                        source_info: diagram.name_source_info,
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
static DIAGRAM_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &DiagramSectionParser;

/// Protocol converter for diagrams: their records' JSON, with their
/// package and name.
pub struct DiagramProtocol;

impl ElementProtocol for DiagramProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Value> {
        let diagram = element.as_any().downcast_ref::<Diagram>().ok_or_else(|| {
            ConvertError::plugin(
                format!(
                    "DiagramProtocol: a '{}' element is not a Diagram",
                    element.kind()
                ),
                None,
            )
        })?;
        let mut json = diagram.record.to_json()?;
        json["package"] = json!(
            diagram
                .package
                .as_ref()
                .map(ToString::to_string)
                .unwrap_or_default()
        );
        json["name"] = json!(diagram.name.value.as_str());
        Ok(json)
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static DIAGRAM_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &DiagramProtocol;
