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

//! The `###QueryPostProcessor` section: relational mappers, which rename a
//! database's schemas and tables when its queries run
//! (`RelationalMapperParserGrammar` and `RelationalMapperParseTreeWalker`).
//!
//! ```pure
//! ###QueryPostProcessor
//! RelationalMapper my::Renames
//! (
//!   DatabaseMappers: [ [my::Db.sales] -> 'SalesDb' ];
//!   SchemaMappers: [ my::Db.sales -> 'Sales' ];
//!   TableMappers: [ my::Db.sales.ORDERS -> 'Orders' ];
//! )
//! ```
//!
//! The walker checks nothing the grammar doesn't, so each mapper is read
//! into its record directly. Not reproduced: the walker keeps a section's
//! imports only when it has a mapper.

use std::any::Any;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::{list, string_text};
use legend_pure_parser_parser::words::{self, Name, at_word, is_word, unexpected};
use legend_pure_parser_parser::{ParserContext, SectionParser};
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use legend_pure_parser_protocol::v1::record::Record;
use linkme::distributed_slice;
use serde_json::{Value, json};

type R<T> = std::result::Result<T, ParseError>;

/// The section kind, `###QueryPostProcessor`, which is also its mappers'
/// kind.
pub const SECTION_KIND: &str = "QueryPostProcessor";

/// The grammar's keywords that its `identifier` excludes.
const KEYWORDS: [&str; 4] = [
    "RelationalMapper",
    "DatabaseMappers",
    "SchemaMappers",
    "TableMappers",
];

/// `RelationalMapper my::M ( … )`.
#[derive(Debug, Clone, PartialEq)]
pub struct RelationalMapper {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// The mapper's object, without its package and name.
    pub record: Record,
    /// From `RelationalMapper` to `)`.
    pub source_info: SourceInfo,
}

impl Spanned for RelationalMapper {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

/// A mapper has no annotations.
impl Annotated for RelationalMapper {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl PackageableElement for RelationalMapper {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for RelationalMapper {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn DSLElement> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn DSLElement) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}

/// The grammar's `identifier`: a word but its keywords, or a quoted name.
fn identifier(ctx: &mut ParserContext<'_>) -> R<Name> {
    let token = ctx.cursor().peek().clone();
    if KEYWORDS.iter().any(|keyword| is_word(&token, keyword)) {
        return Err(unexpected(&token));
    }
    words::identifier(ctx)
}

/// A database's qualified name, as written (`getText()`).
fn database(ctx: &mut ParserContext<'_>) -> R<String> {
    let mut text = identifier(ctx)?.text.to_string();
    while ctx.cursor().eat(TokenKind::PathSep) {
        text.push_str("::");
        text.push_str(&identifier(ctx)?.text);
    }
    Ok(text)
}

/// `my::Db.schema`: a `Schema` pointer, positioned on itself.
fn schema(ctx: &mut ParserContext<'_>) -> R<Record> {
    let start = ctx.cursor().current_source_info();
    let database = database(ctx)?;
    ctx.cursor().expect(TokenKind::Dot)?;
    let schema = identifier(ctx)?;
    Ok(Record::new("Schema")
        .with("database", database)
        .with("schema", schema.value.to_string())
        .at(&ctx.cursor().span_since(&start)))
}

/// `my::Db.schema.TABLE`: a `Table` pointer, positioned on itself.
fn table(ctx: &mut ParserContext<'_>) -> R<Record> {
    let start = ctx.cursor().current_source_info();
    let database = database(ctx)?;
    ctx.cursor().expect(TokenKind::Dot)?;
    let schema = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Dot)?;
    let table = identifier(ctx)?;
    Ok(Record::new("Table")
        .with("database", database)
        .with("schema", schema.value.to_string())
        .with("table", table.value.to_string())
        .at(&ctx.cursor().span_since(&start)))
}

/// `-> 'name'`.
fn target(ctx: &mut ParserContext<'_>) -> R<String> {
    ctx.cursor().expect(TokenKind::Arrow)?;
    let name = ctx.cursor().expect(TokenKind::StringLiteral)?;
    string_text(&name)
}

/// `Keyword: [ mapper, … ];`, if its keyword is next.
fn mappers(
    ctx: &mut ParserContext<'_>,
    keyword: &str,
    mapper: fn(&mut ParserContext<'_>) -> R<Record>,
) -> R<Option<Vec<Record>>> {
    if !at_word(ctx, keyword) {
        return Ok(None);
    }
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::Colon)?;
    let mappers = list(ctx, mapper)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok(Some(mappers))
}

/// `[ my::Db.schema, … ] -> 'name'`.
fn database_mapper(ctx: &mut ParserContext<'_>) -> R<Record> {
    let schemas = list(ctx, schema)?;
    let name = target(ctx)?;
    Ok(Record::untyped()
        .with("databaseName", name)
        .with("schemas", schemas))
}

/// `my::Db.schema -> 'name'`.
fn schema_mapper(ctx: &mut ParserContext<'_>) -> R<Record> {
    let from = schema(ctx)?;
    let to = target(ctx)?;
    Ok(Record::untyped().with("from", from).with("to", to))
}

/// `my::Db.schema.TABLE -> 'name'`.
fn table_mapper(ctx: &mut ParserContext<'_>) -> R<Record> {
    let from = table(ctx)?;
    let to = target(ctx)?;
    Ok(Record::untyped().with("from", from).with("to", to))
}

/// A relational mapper: its database, schema and table mappers, in that
/// order, each optional.
fn mapper(ctx: &mut ParserContext<'_>) -> R<RelationalMapper> {
    let keyword = ctx.cursor().peek().clone();
    if !is_word(&keyword, "RelationalMapper") {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    ctx.cursor().expect(TokenKind::LParen)?;
    let database_mappers = mappers(ctx, "DatabaseMappers", database_mapper)?;
    let schema_mappers = mappers(ctx, "SchemaMappers", schema_mapper)?;
    let table_mappers = mappers(ctx, "TableMappers", table_mapper)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    let source_info = ctx.cursor().span_since(&keyword.source_info);
    Ok(RelationalMapper {
        package,
        name: SpannedString {
            value: name,
            source_info: name_source_info,
        },
        record: Record::new("relationalMapper")
            .at(&source_info)
            .with_some("databaseMappers", database_mappers)
            .with_some("schemaMappers", schema_mappers)
            .with_some("tableMappers", table_mappers),
        source_info,
    })
}

/// Parser for `###QueryPostProcessor` sections.
pub struct QueryPostProcessorSectionParser;

impl SectionParser for QueryPostProcessorSectionParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
            match mapper(ctx) {
                Ok(mapper) => elements.push(Box::new(mapper)),
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        elements
    }
}

#[distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static QUERY_POST_PROCESSOR_SECTION_PARSER: &(dyn SectionParser + Send + Sync) =
    &QueryPostProcessorSectionParser;

/// Protocol converter for relational mappers: their records' JSON, with
/// their package and name.
pub struct RelationalMapperProtocol;

impl ElementProtocol for RelationalMapperProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Value> {
        let mapper = element
            .as_any()
            .downcast_ref::<RelationalMapper>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "RelationalMapperProtocol: a '{}' element is not a RelationalMapper",
                        element.kind()
                    ),
                    None,
                )
            })?;
        let mut json = mapper.record.to_json()?;
        json["package"] = json!(
            mapper
                .package
                .as_ref()
                .map(ToString::to_string)
                .unwrap_or_default()
        );
        json["name"] = json!(mapper.name.value.as_str());
        Ok(json)
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static RELATIONAL_MAPPER_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &RelationalMapperProtocol;
