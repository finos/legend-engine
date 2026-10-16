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

//! `SectionParser` and `ElementProtocol` for `###Elasticsearch`: the
//! engine's `ElasticsearchParserGrammar` (the reader), then its
//! `ElasticsearchStoreParseTreeWalker` (the walk), each store in turn, and
//! the stores' JSON. The section has imports.

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

use super::read::store;
use super::{ElasticsearchStore, SECTION_KIND, walk};

/// Parser for `###Elasticsearch` sections.
pub struct ElasticsearchSectionParser;

impl SectionParser for ElasticsearchSectionParser {
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
            match store(ctx) {
                Ok(store) => read.push(store),
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for store in read {
            let source_info = store.body.source_info.clone();
            match walk::store(store.body) {
                Ok(record) => elements.push(Box::new(ElasticsearchStore {
                    package: store.package,
                    name: SpannedString {
                        value: store.name,
                        source_info: store.name_source_info,
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

#[linkme::distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static ELASTICSEARCH_SECTION_PARSER: &(dyn SectionParser + Send + Sync) =
    &ElasticsearchSectionParser;

/// Protocol converter for Elasticsearch stores: their records' JSON, with
/// their package and name.
pub struct ElasticsearchProtocol;

impl ElementProtocol for ElasticsearchProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Box<RawValue>> {
        let store = element
            .as_any()
            .downcast_ref::<ElasticsearchStore>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "ElasticsearchProtocol: a '{}' element is not an ElasticsearchStore",
                        element.kind()
                    ),
                    None,
                )
            })?;
        let json = store
            .record
            .converted()?
            .with(
                "package",
                store
                    .package
                    .as_ref()
                    .map(ToString::to_string)
                    .unwrap_or_default(),
            )
            .with("name", store.name.value.as_str());
        Ok(to_raw_value(&json)?)
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static ELASTICSEARCH_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &ElasticsearchProtocol;
