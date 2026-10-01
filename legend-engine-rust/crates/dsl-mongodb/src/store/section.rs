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

//! `SectionParser` and `ElementProtocol` for `###MongoDB`: the engine's
//! `MongoDBSchemaParserGrammar` (the reader), then its
//! `MongoDBSchemaParseTreeWalker` (the walk), each database in turn, and
//! the databases' JSON. The section has no imports.

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

use super::read::database;
use super::{MongoDatabase, SECTION_KIND, walk};

/// Parser for `###MongoDB` sections.
pub struct MongoDBSectionParser;

impl SectionParser for MongoDBSectionParser {
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
            match database(ctx) {
                Ok(database) => read.push(database),
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for database in read {
            match walk::database(&database) {
                Ok(record) => elements.push(Box::new(MongoDatabase {
                    package: database.package,
                    name: SpannedString {
                        value: database.name,
                        source_info: database.name_source_info,
                    },
                    record,
                    source_info: database.source_info,
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
static MONGODB_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &MongoDBSectionParser;

/// Protocol converter for MongoDB databases: their records' JSON, with their
/// package and name.
pub struct MongoDBProtocol;

impl ElementProtocol for MongoDBProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Value> {
        let store = element
            .as_any()
            .downcast_ref::<MongoDatabase>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "MongoDBProtocol: a '{}' element is not a MongoDatabase",
                        element.kind()
                    ),
                    None,
                )
            })?;
        let mut json = store.record.to_json()?;
        json["package"] = json!(
            store
                .package
                .as_ref()
                .map(ToString::to_string)
                .unwrap_or_default()
        );
        json["name"] = json!(store.name.value.as_str());
        Ok(json)
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static MONGODB_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &MongoDBProtocol;
