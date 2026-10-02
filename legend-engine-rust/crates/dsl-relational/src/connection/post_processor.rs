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

//! The relational store's post-processors (`PostProcessorParserGrammar` and
//! its `PostProcessorParseTreeWalker`):
//!
//! ```text
//! mapper { mappers: [ table { from: 'a'; to: 'A'; schemaFrom: 's'; schemaTo: 'S'; }, schema { from: 's'; to: 'S'; }, … ]; }
//! relationalMapper { my::Mapper, … }
//! ExtractSubQueriesAsCTEsPostProcessor
//! ```
//!
//! What the walker reads in a body, and where it reports a mapper's field
//! missing or written twice (the mapper), is positioned as the walker parses
//! the body again (see [`specification`](super::specification)). A mapper
//! and a relational mapper post-processor have no position of their own; an
//! `ExtractSubQueriesAsCTEsPostProcessor` is positioned as the specification.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::required;
use legend_pure_parser_parser::words::{is_word, qualified_name, unexpected};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::element::PackageableElementPointer;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde::Serialize;
use serde_json::Value;

use super::specification::{
    BodyUse, Specification, SpecificationBody, SpecificationGrammar, SpecificationKind,
    SpecificationSpan, downcast,
};
use super::values::{at_field, field, string};

type R<T> = Result<T, ParseError>;

/// The relational store's post-processor grammars.
pub(crate) static CORE_POST_PROCESSORS: [&(dyn SpecificationGrammar + Send + Sync); 3] = [
    &MapperGrammar,
    &RelationalMapperGrammar,
    &ExtractSubQueriesAsCtesGrammar,
];

/// `mapper { mappers: [ … ]; }`: names mapped.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Mappers(pub Vec<Mapper>);

/// A table's or a schema's name mapped.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Mapper {
    /// `table { from: 'a'; to: 'A'; schemaFrom: 's'; schemaTo: 'S'; }`.
    Table {
        /// The table's name.
        from: String,
        /// Its new name.
        to: String,
        /// The table's schema's name.
        schema_from: String,
        /// Its new name.
        schema_to: String,
    },
    /// `schema { from: 's'; to: 'S'; }`.
    Schema {
        /// The schema's name.
        from: String,
        /// Its new name.
        to: String,
    },
}

/// `relationalMapper { my::Mapper, … }`: the relational mappers, each
/// positioned as the walker parses the body again.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RelationalMappers(pub Vec<(String, SourceInfo)>);

/// `ExtractSubQueriesAsCTEsPostProcessor`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ExtractSubQueriesAsCtes;

body!(Mappers);
body!(RelationalMappers);
body!(ExtractSubQueriesAsCtes);

/// The `mapper` grammar.
pub struct MapperGrammar;

/// The `relationalMapper` grammar.
pub struct RelationalMapperGrammar;

/// The `ExtractSubQueriesAsCTEsPostProcessor` grammar.
pub struct ExtractSubQueriesAsCtesGrammar;

/// A mapper as the grammar reads it: its fields, as often as written.
struct ReadMapper {
    table: bool,
    froms: Vec<String>,
    tos: Vec<String>,
    schema_froms: Vec<String>,
    schema_tos: Vec<String>,
    source_info: SourceInfo,
}

impl SpecificationGrammar for MapperGrammar {
    fn kind(&self) -> SpecificationKind {
        SpecificationKind::PostProcessor
    }

    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "mapper"
    }

    fn body_use(&self) -> BodyUse {
        BodyUse::Required
    }

    fn parse(
        &self,
        _span: &SpecificationSpan,
        body: Option<&mut ParserContext<'_>>,
    ) -> R<Box<dyn SpecificationBody>> {
        let Some(ctx) = body else {
            return Ok(Box::new(Mappers(Vec::new())));
        };
        // Exactly `mappers: [ … ];`.
        let keyword = ctx.cursor().peek().clone();
        if !is_word(&keyword, "mappers") {
            return Err(unexpected(&keyword));
        }
        let (read, _) = field(ctx, |ctx| {
            ctx.cursor().expect(TokenKind::LBracket)?;
            let mut mappers = Vec::new();
            if !ctx.cursor().check(TokenKind::RBracket) {
                mappers.push(read_mapper(ctx)?);
                while ctx.cursor().eat(TokenKind::Comma) {
                    mappers.push(read_mapper(ctx)?);
                }
            }
            ctx.cursor().expect(TokenKind::RBracket)?;
            Ok(mappers)
        })?;
        // The walker, once the body is read.
        let mappers = read
            .into_iter()
            .map(|mapper| {
                let at = &mapper.source_info;
                if mapper.table {
                    Ok(Mapper::Table {
                        from: required(mapper.froms, "from", at)?,
                        to: required(mapper.tos, "to", at)?,
                        schema_from: required(mapper.schema_froms, "schemaFrom", at)?,
                        schema_to: required(mapper.schema_tos, "schemaTo", at)?,
                    })
                } else {
                    Ok(Mapper::Schema {
                        from: required(mapper.froms, "from", at)?,
                        to: required(mapper.tos, "to", at)?,
                    })
                }
            })
            .collect::<R<_>>()?;
        Ok(Box::new(Mappers(mappers)))
    }

    fn convert(&self, specification: &Specification) -> Result<Value, ConvertError> {
        let Mappers(mappers) = downcast::<Mappers>(specification)?;
        Ok(serde_json::to_value(MapperPostProcessorJson {
            mappers: mappers.iter().map(MapperJson::from).collect(),
        })?)
    }
}

/// `table { … }` or `schema { … }`.
fn read_mapper(ctx: &mut ParserContext<'_>) -> R<ReadMapper> {
    let start = ctx.cursor().current_source_info();
    let keyword = ctx.cursor().peek().clone();
    let table = if is_word(&keyword, "table") {
        true
    } else if is_word(&keyword, "schema") {
        false
    } else {
        return Err(unexpected(&keyword));
    };
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::LBrace)?;
    let mut read = ReadMapper {
        table,
        froms: Vec::new(),
        tos: Vec::new(),
        schema_froms: Vec::new(),
        schema_tos: Vec::new(),
        source_info: start.clone(),
    };
    loop {
        if at_field(ctx, "from") {
            read.froms.push(field(ctx, string)?.0);
        } else if at_field(ctx, "to") {
            read.tos.push(field(ctx, string)?.0);
        } else if table && at_field(ctx, "schemaFrom") {
            read.schema_froms.push(field(ctx, string)?.0);
        } else if table && at_field(ctx, "schemaTo") {
            read.schema_tos.push(field(ctx, string)?.0);
        } else {
            break;
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    read.source_info = ctx.cursor().span_since(&start);
    Ok(read)
}

impl SpecificationGrammar for RelationalMapperGrammar {
    fn kind(&self) -> SpecificationKind {
        SpecificationKind::PostProcessor
    }

    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "relationalMapper"
    }

    fn body_use(&self) -> BodyUse {
        BodyUse::Required
    }

    fn parse(
        &self,
        _span: &SpecificationSpan,
        body: Option<&mut ParserContext<'_>>,
    ) -> R<Box<dyn SpecificationBody>> {
        let mut mappers = Vec::new();
        let Some(ctx) = body else {
            return Ok(Box::new(RelationalMappers(mappers)));
        };
        if !ctx.cursor().check(TokenKind::Eof) {
            let name = qualified_name(ctx)?;
            mappers.push((name.path, name.source_info));
            while ctx.cursor().eat(TokenKind::Comma) {
                let name = qualified_name(ctx)?;
                mappers.push((name.path, name.source_info));
            }
        }
        Ok(Box::new(RelationalMappers(mappers)))
    }

    fn convert(&self, specification: &Specification) -> Result<Value, ConvertError> {
        let RelationalMappers(mappers) = downcast::<RelationalMappers>(specification)?;
        Ok(serde_json::to_value(RelationalMapperPostProcessorJson {
            relational_mappers: mappers
                .iter()
                .map(|(path, source_info)| PackageableElementPointer {
                    element_type: Some("QUERYPOSTPROCESSOR".to_string()),
                    path: path.clone(),
                    source_information: Some(SourceInformation::from(source_info)),
                })
                .collect(),
        })?)
    }
}

impl SpecificationGrammar for ExtractSubQueriesAsCtesGrammar {
    fn kind(&self) -> SpecificationKind {
        SpecificationKind::PostProcessor
    }

    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "ExtractSubQueriesAsCTEsPostProcessor"
    }

    fn body_use(&self) -> BodyUse {
        BodyUse::None
    }

    fn parse(
        &self,
        _span: &SpecificationSpan,
        _body: Option<&mut ParserContext<'_>>,
    ) -> R<Box<dyn SpecificationBody>> {
        Ok(Box::new(ExtractSubQueriesAsCtes))
    }

    fn convert(&self, specification: &Specification) -> Result<Value, ConvertError> {
        Ok(serde_json::to_value(ExtractSubQueriesAsCtesJson {
            source_information: SourceInformation::from(&specification.source_info),
        })?)
    }
}

/// The engine's `MapperPostProcessor`, without a position.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "mapper")]
struct MapperPostProcessorJson<'a> {
    mappers: Vec<MapperJson<'a>>,
}

/// The engine's `TableNameMapper` and `SchemaNameMapper`.
#[derive(Serialize)]
#[serde(tag = "_type")]
enum MapperJson<'a> {
    #[serde(rename = "table")]
    Table {
        from: &'a str,
        to: &'a str,
        schema: SchemaMapperJson<'a>,
    },
    #[serde(rename = "schema")]
    Schema { from: &'a str, to: &'a str },
}

/// A table mapper's schema mapper.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "schema")]
struct SchemaMapperJson<'a> {
    from: &'a str,
    to: &'a str,
}

impl<'a> From<&'a Mapper> for MapperJson<'a> {
    fn from(mapper: &'a Mapper) -> Self {
        match mapper {
            Mapper::Table {
                from,
                to,
                schema_from,
                schema_to,
            } => Self::Table {
                from,
                to,
                schema: SchemaMapperJson {
                    from: schema_from,
                    to: schema_to,
                },
            },
            Mapper::Schema { from, to } => Self::Schema { from, to },
        }
    }
}

/// The engine's `RelationalMapperPostProcessor`, without a position.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "relationalMapper", rename_all = "camelCase")]
struct RelationalMapperPostProcessorJson {
    relational_mappers: Vec<PackageableElementPointer>,
}

/// The engine's `ExtractSubQueriesAsCTEsPostProcessor`.
#[derive(Serialize)]
#[serde(
    tag = "_type",
    rename = "ExtractSubQueriesAsCTEsPostProcessor",
    rename_all = "camelCase"
)]
struct ExtractSubQueriesAsCtesJson {
    source_information: SourceInformation,
}
