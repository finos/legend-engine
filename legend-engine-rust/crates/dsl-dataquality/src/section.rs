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

//! `SectionParser` for `###DataQualityValidation`: the engine's
//! `DataQualityParserGrammar` (the crate's reader), then its
//! `DataQualityTreeWalker` (the crate's walk), each element in turn. The
//! section has no imports.

use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{ParserContext, SectionParser};

use crate::ast::{DataQualityElement, SECTION_KIND};
use crate::read::{ReadElement, element};
use crate::walk;

/// Parser for `###DataQualityValidation` sections.
pub struct DataQualitySectionParser;

impl SectionParser for DataQualitySectionParser {
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
                Ok(element) => read.push(element),
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for element in read {
            match walk_element(ctx, element) {
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
fn walk_element(
    ctx: &mut ParserContext<'_>,
    read: ReadElement,
) -> Result<DataQualityElement, ParseError> {
    let source_info = read.body.source_info.clone();
    let tagged_values = ParserContext::with_documentation(read.documentation, read.tagged_values)?;
    let record = walk::element(ctx, read.kind, read.body)?;
    Ok(DataQualityElement {
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

#[linkme::distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static DATA_QUALITY_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &DataQualitySectionParser;
