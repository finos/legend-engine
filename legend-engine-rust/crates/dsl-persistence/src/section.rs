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

//! `SectionParser` for `###Persistence`: the engine's
//! `PersistenceParserGrammar` ([`read`](crate::read)), then its
//! `PersistenceParseTreeWalker` and `PersistenceContextParseTreeWalker`
//! (the crate's walk), each element in turn.

use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{ParserContext, SectionParser};
use smol_str::SmolStr;

use crate::ast::{PersistenceElement, SECTION_KIND};
use crate::read::{ReadElement, element};
use crate::walk::{context, persistence};

/// Parser for `###Persistence` sections.
pub struct PersistenceSectionParser;

impl SectionParser for PersistenceSectionParser {
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

/// The walker's `visitElement`.
fn walk_element(
    ctx: &mut ParserContext<'_>,
    read: ReadElement,
) -> Result<PersistenceElement, ParseError> {
    let ReadElement { body, path } = read;
    let source_info = body.source_info.clone();
    let record = if body.keyword.text == "PersistenceContext" {
        context(ctx, body)?
    } else {
        persistence(ctx, body)?
    };
    let mut parts: Vec<&str> = path.path.split("::").collect();
    let name = SmolStr::new(parts.pop().unwrap_or_default());
    let package = parts.split_first().map(|(root, rest)| {
        rest.iter().fold(
            Package::root(SmolStr::new(root), path.source_info.clone()),
            |package, part| package.child(SmolStr::new(part), path.source_info.clone()),
        )
    });
    Ok(PersistenceElement {
        package,
        name: SpannedString {
            value: name,
            source_info: path.source_info,
        },
        record,
        source_info,
    })
}

#[linkme::distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static PERSISTENCE_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &PersistenceSectionParser;
