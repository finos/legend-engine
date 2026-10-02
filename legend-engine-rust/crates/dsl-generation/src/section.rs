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

//! `SectionParser`s and `ElementProtocol`s for `###FileGeneration` and
//! `###GenerationSpecification`: each section read whole, then each element
//! walked; and the elements' JSON. Both sections have imports.

use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{ParserContext, SectionParser};
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use legend_pure_parser_protocol::v1::record::Record;
use linkme::distributed_slice;
use serde_json::value::{RawValue, to_raw_value};
use smol_str::SmolStr;

use crate::ast::{GenerationElement, GenerationKind};
use crate::{file_generation, generation_specification};

/// An element read, and its walk.
struct Read {
    package: Option<Package>,
    name: SmolStr,
    name_source_info: SourceInfo,
    source_info: SourceInfo,
    walk: Box<dyn FnOnce() -> std::result::Result<Record, ParseError>>,
}

/// Reads the section's elements whole, then walks each: the engine reports
/// a syntax error before its walker's.
fn parse(
    ctx: &mut ParserContext<'_>,
    errors: &mut Vec<ParseError>,
    kind: GenerationKind,
    read: fn(&mut ParserContext<'_>) -> std::result::Result<Read, ParseError>,
) -> Vec<Box<dyn DSLElement>> {
    let mut elements_read = Vec::new();
    while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
        match read(ctx) {
            Ok(element) => elements_read.push(element),
            Err(error) => {
                errors.push(error);
                return Vec::new();
            }
        }
    }
    let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
    for element in elements_read {
        match (element.walk)() {
            Ok(record) => elements.push(Box::new(GenerationElement {
                kind,
                package: element.package,
                name: SpannedString {
                    value: element.name,
                    source_info: element.name_source_info,
                },
                record,
                source_info: element.source_info,
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

fn read_file_generation(ctx: &mut ParserContext<'_>) -> std::result::Result<Read, ParseError> {
    let mut read = file_generation::read(ctx)?;
    Ok(Read {
        package: read.package.take(),
        name: read.name.clone(),
        name_source_info: read.name_source_info.clone(),
        source_info: read.source_info.clone(),
        walk: Box::new(move || file_generation::walk(read)),
    })
}

fn read_specification(ctx: &mut ParserContext<'_>) -> std::result::Result<Read, ParseError> {
    let read = generation_specification::read(ctx)?;
    let body = read.body;
    Ok(Read {
        package: read.package,
        name: read.name,
        name_source_info: read.name_source_info,
        source_info: body.source_info.clone(),
        walk: Box::new(move || generation_specification::walk(body)),
    })
}

/// Parser for `###FileGeneration` sections.
pub struct FileGenerationSectionParser;

impl SectionParser for FileGenerationSectionParser {
    fn kind(&self) -> &str {
        GenerationKind::FileGeneration.section()
    }

    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        parse(
            ctx,
            errors,
            GenerationKind::FileGeneration,
            read_file_generation,
        )
    }
}

/// Parser for `###GenerationSpecification` sections.
pub struct GenerationSpecificationSectionParser;

impl SectionParser for GenerationSpecificationSectionParser {
    fn kind(&self) -> &str {
        GenerationKind::GenerationSpecification.section()
    }

    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        parse(
            ctx,
            errors,
            GenerationKind::GenerationSpecification,
            read_specification,
        )
    }
}

#[distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static FILE_GENERATION_SECTION_PARSER: &(dyn SectionParser + Send + Sync) =
    &FileGenerationSectionParser;

#[distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static GENERATION_SPECIFICATION_SECTION_PARSER: &(dyn SectionParser + Send + Sync) =
    &GenerationSpecificationSectionParser;

/// Protocol converter for one section's elements: their records' JSON,
/// with their package and name.
pub struct GenerationProtocol(pub GenerationKind);

impl ElementProtocol for GenerationProtocol {
    fn kind(&self) -> &str {
        self.0.section()
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Box<RawValue>> {
        let element = element
            .as_any()
            .downcast_ref::<GenerationElement>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "GenerationProtocol: a '{}' element is not a GenerationElement",
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
static FILE_GENERATION_PROTOCOL: &(dyn ElementProtocol + Send + Sync) =
    &GenerationProtocol(GenerationKind::FileGeneration);

#[distributed_slice(ELEMENT_PROTOCOLS)]
static GENERATION_SPECIFICATION_PROTOCOL: &(dyn ElementProtocol + Send + Sync) =
    &GenerationProtocol(GenerationKind::GenerationSpecification);
