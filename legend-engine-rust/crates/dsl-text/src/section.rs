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

//! `SectionParser` and `ElementProtocol` for `###Text`: the engine's
//! `TextParserGrammar` (the reader), then its `TextParseTreeWalker` (the
//! walk), each text in turn; and the texts' JSON. The section has no
//! imports.

use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::blocks::{
    ReadBlock, ReadField, braces, identifier_value, simple, string_value,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{is_word, unexpected};
use legend_pure_parser_parser::{ParserContext, SectionParser};
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use legend_pure_parser_protocol::v1::record::Record;
use linkme::distributed_slice;
use serde_json::value::{RawValue, to_raw_value};

use crate::{SECTION_KIND, TextElement};

type R<T> = std::result::Result<T, ParseError>;

/// Parser for `###Text` sections.
pub struct TextSectionParser;

impl SectionParser for TextSectionParser {
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

/// `Text my::T { field … }`, read.
struct ReadText {
    package: Option<Package>,
    name: SpannedString,
    /// Its fields; positioned from `Text` to `}`.
    body: ReadBlock,
}

/// The section read whole, then walked.
fn parse_body(ctx: &mut ParserContext<'_>) -> R<Vec<Box<dyn DSLElement>>> {
    let mut read = Vec::new();
    while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
        read.push(text(ctx)?);
    }
    read.into_iter()
        .map(|text| {
            let source_info: SourceInfo = text.body.source_info.clone();
            Ok(Box::new(TextElement {
                package: text.package,
                name: text.name,
                record: walk(text.body)?,
                source_info,
            }) as Box<dyn DSLElement>)
        })
        .collect()
}

/// `Text my::T { (type: name; | content: 'text';)* }`.
fn text(ctx: &mut ParserContext<'_>) -> R<ReadText> {
    let keyword = ctx.cursor().peek().clone();
    if !is_word(&keyword, "Text") {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    let (_, fields) = braces(ctx, |ctx: &mut ParserContext<'_>, name: &str| match name {
        "type" => simple(ctx, "type", identifier_value),
        "content" => simple(ctx, "content", string_value),
        _ => Ok(None),
    })?;
    Ok(ReadText {
        package,
        name: SpannedString {
            value: name,
            source_info: name_source_info,
        },
        body: ReadBlock {
            source_info: ctx.cursor().span_since(&keyword.source_info),
            keyword,
            fields,
        },
    })
}

/// The walker's `visitText`: its type, optional, then its content.
fn walk(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let text_type = read
        .take_optional("type", &at)?
        .map(ReadField::identifier)
        .transpose()?;
    let content = read.take("content", &at)?.string()?;
    Ok(Record::new("text")
        .at(&at)
        .with_some("type", text_type)
        .with("content", content))
}

#[distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static TEXT_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &TextSectionParser;

/// Protocol converter for texts: their records' JSON, with their package
/// and name.
pub struct TextProtocol;

impl ElementProtocol for TextProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Box<RawValue>> {
        let text = element
            .as_any()
            .downcast_ref::<TextElement>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "TextProtocol: a '{}' element is not a TextElement",
                        element.kind()
                    ),
                    None,
                )
            })?;
        let json = text
            .record
            .converted()?
            .with(
                "package",
                text.package
                    .as_ref()
                    .map(ToString::to_string)
                    .unwrap_or_default(),
            )
            .with("name", text.name.value.as_str());
        Ok(to_raw_value(&json)?)
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static TEXT_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &TextProtocol;
