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

//! `SectionParser` for `###DataSpace`: the engine's `DataSpaceParserGrammar`
//! (the crate's reader), then its `DataSpaceParseTreeWalker` (the crate's
//! walk), each data space in turn. The section has no imports.

use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{ParserContext, SectionParser};

use crate::ast::{DataSpace, SECTION_KIND};
use crate::read::{ReadDataSpace, data_space};
use crate::walk;

/// Parser for `###DataSpace` sections.
pub struct DataSpaceSectionParser;

impl SectionParser for DataSpaceSectionParser {
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
            match data_space(ctx) {
                Ok(data_space) => read.push(data_space),
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for data_space in read {
            match walk_data_space(ctx, data_space) {
                Ok(data_space) => elements.push(Box::new(data_space)),
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

/// The walker's `visitDataSpace`: its annotations first.
fn walk_data_space(
    ctx: &mut ParserContext<'_>,
    read: ReadDataSpace,
) -> Result<DataSpace, ParseError> {
    let source_info = read.body.source_info.clone();
    let tagged_values = ParserContext::with_documentation(read.documentation, read.tagged_values)?;
    let record = walk::data_space(ctx, read.body)?;
    Ok(DataSpace {
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
static DATA_SPACE_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &DataSpaceSectionParser;
