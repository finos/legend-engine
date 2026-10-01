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

//! `SectionParser` and `ElementProtocol` for `###Snowflake`: the engine's
//! `SnowflakeParserGrammar` (the reader), then its `SnowflakeTreeWalker`
//! (the walk), its apps first, then its UDFs; and the elements' JSON. The
//! section has no imports.

use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{ParserContext, SectionParser};
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use legend_pure_parser_protocol::v1::annotation::TaggedValue;
use legend_pure_parser_protocol::v1::convert::section_stereotype;
use linkme::distributed_slice;
use serde_json::{Value, json};

use crate::ast::{SECTION_KIND, SnowflakeElement, SnowflakeKind};
use crate::read::{ReadElement, element};
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
            match walk_element(element) {
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

/// The walker's `visit…` of an element: its annotations first.
fn walk_element(read: ReadElement) -> std::result::Result<SnowflakeElement, ParseError> {
    let source_info = read.body.source_info.clone();
    let tagged_values = ParserContext::with_documentation(read.documentation, read.tagged_values)?;
    let record = walk::element(read.kind, read.body)?;
    Ok(SnowflakeElement {
        kind: read.kind,
        package: read.package,
        name: SpannedString {
            value: read.name,
            source_info: read.name_source_info,
        },
        stereotypes: read.stereotypes,
        tagged_values,
        record,
        source_info,
    })
}

#[distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static SNOWFLAKE_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &SnowflakeSectionParser;

/// Protocol converter for Snowflake apps and UDFs: their records' JSON,
/// with their package, name and annotations (stereotypes positioned on
/// their values, as the section walkers but the Pure one position them).
pub struct SnowflakeProtocol;

impl ElementProtocol for SnowflakeProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Value> {
        let element = element
            .as_any()
            .downcast_ref::<SnowflakeElement>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "SnowflakeProtocol: a '{}' element is not a SnowflakeElement",
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
        json["stereotypes"] = serde_json::to_value(
            element
                .stereotypes
                .iter()
                .map(section_stereotype)
                .collect::<Vec<_>>(),
        )?;
        json["taggedValues"] = serde_json::to_value(
            element
                .tagged_values
                .iter()
                .map(TaggedValue::from)
                .collect::<Vec<_>>(),
        )?;
        Ok(json)
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static SNOWFLAKE_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &SnowflakeProtocol;
