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

//! `SectionParser` and `ElementProtocol` for `###Deephaven`: the engine's
//! `DeephavenParserGrammar` (the reader), then its
//! `DeephavenParseTreeWalker` (the walk), its stores first, then its apps;
//! and the elements' JSON. The section has imports.

use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{ParserContext, SectionParser};
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use linkme::distributed_slice;
use serde_json::value::{RawValue, to_raw_value};

use super::read::{ReadBody, ReadElement, element};
use super::{DeephavenElement, SECTION_KIND, walk};

/// Parser for `###Deephaven` sections.
pub struct DeephavenSectionParser;

impl SectionParser for DeephavenSectionParser {
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
        // The walker walks the stores, then the apps.
        let (stores, apps): (Vec<_>, Vec<_>) = read
            .into_iter()
            .partition(|element| matches!(element.body, ReadBody::Store(..)));
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for element in stores.into_iter().chain(apps) {
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

/// The walker's `visitDeephavenStore` or `visitDeephavenApp`.
fn walk_element(read: ReadElement) -> std::result::Result<DeephavenElement, ParseError> {
    let (record, source_info) = match read.body {
        ReadBody::Store(tables, source_info) => (walk::store(tables, &source_info)?, source_info),
        ReadBody::App(body) => {
            let source_info = body.source_info.clone();
            (walk::app(body)?, source_info)
        }
    };
    Ok(DeephavenElement {
        package: read.package,
        name: SpannedString {
            value: read.name,
            source_info: read.name_source_info,
        },
        record,
        source_info,
    })
}

#[distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static DEEPHAVEN_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &DeephavenSectionParser;

/// Protocol converter for Deephaven stores and apps: their records' JSON,
/// with their package and name.
pub struct DeephavenProtocol;

impl ElementProtocol for DeephavenProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Box<RawValue>> {
        let element = element
            .as_any()
            .downcast_ref::<DeephavenElement>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "DeephavenProtocol: a '{}' element is not a DeephavenElement",
                        element.kind()
                    ),
                    None,
                )
            })?;
        let json = element
            .record
            .converted()?
            .with(
                "package",
                element
                    .package
                    .as_ref()
                    .map(ToString::to_string)
                    .unwrap_or_default(),
            )
            .with("name", element.name.value.as_str());
        Ok(to_raw_value(&json)?)
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static DEEPHAVEN_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &DeephavenProtocol;
