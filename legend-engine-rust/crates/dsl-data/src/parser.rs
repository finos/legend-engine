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

//! `SectionParser` for `###Data`, the engine's `DataParserGrammar`:
//!
//! ```text
//! definition:   dataElement* EOF
//! dataElement:  documentation? Data stereotypes? taggedValues? qualifiedName
//!               { embeddedData? (dataResolver ;)* }
//! dataResolver: qualifiedName (: embeddedData)?
//! ```
//!
//! The section has no imports.

use legend_pure_parser_ast::annotation::{PackageableElementPtr, SpannedString};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{ParserContext, SectionParser};

use crate::ast::{DataElement, DataResolver, KIND};

/// Parser for `###Data` sections.
pub struct DataSectionParser;

impl SectionParser for DataSectionParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        KIND
    }

    fn imports(&self) -> bool {
        false
    }

    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
            match data_element(ctx) {
                Ok(element) => elements.push(Box::new(element)),
                // The engine reports the first error; the rest of the section
                // is not read.
                Err(error) => {
                    errors.push(error);
                    break;
                }
            }
        }
        elements
    }
}

/// `'''doc''' Data <<s>> {t} my::MyData { data  resolvers }`.
fn data_element(ctx: &mut ParserContext<'_>) -> Result<DataElement, ParseError> {
    let start = ctx.cursor().current_source_info();
    let documentation = ctx
        .cursor()
        .check(TokenKind::StringLiteral)
        .then(|| ctx.cursor().advance().clone());
    let keyword = ctx.cursor().peek().clone();
    if keyword.kind != TokenKind::Identifier || keyword.text != KIND {
        return Err(ParseError::Unexpected {
            message: format!("Unexpected token '{}': expected 'Data'", keyword.text),
            source_info: keyword.source_info,
        });
    }
    ctx.cursor().advance();
    let stereotypes = if ctx.cursor().check(TokenKind::LessLess) {
        ctx.parse_stereotypes()?
    } else {
        Vec::new()
    };
    let tagged_values = if ctx.cursor().check(TokenKind::LBrace) {
        ctx.parse_tagged_values()?
    } else {
        Vec::new()
    };
    let tagged_values = ParserContext::with_documentation(documentation, tagged_values)?;
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    ctx.cursor().expect(TokenKind::LBrace)?;
    let data = if ctx.at_embedded_data() {
        Some(ctx.parse_embedded_data()?)
    } else {
        None
    };
    let mut resolvers = Vec::new();
    while !ctx.cursor().check(TokenKind::RBrace) {
        resolvers.push(resolver(ctx)?);
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(DataElement {
        package,
        name: SpannedString {
            value: name,
            source_info: name_source_info,
        },
        stereotypes,
        tagged_values,
        data,
        resolvers,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `my::Store: Type #{ … }#;` or `my::OtherData;`.
fn resolver(ctx: &mut ParserContext<'_>) -> Result<DataResolver, ParseError> {
    let start = ctx.cursor().current_source_info();
    let (package, name, _) = ctx.parse_qualified_name()?;
    let element = PackageableElementPtr {
        package,
        name,
        source_info: ctx.cursor().span_since(&start),
    };
    let resolver = if ctx.cursor().eat(TokenKind::Colon) {
        let data = ctx.parse_embedded_data()?;
        DataResolver::Base {
            element,
            data,
            source_info: ctx.cursor().span_since(&start),
        }
    } else {
        DataResolver::Reference(element)
    };
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok(resolver)
}

#[linkme::distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static DATA_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &DataSectionParser;
