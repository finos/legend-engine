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

//! `RelationalDatabaseConnection` (the relational store's
//! `RelationalDatabaseConnectionParserGrammar` and its
//! `RelationalDatabaseConnectionParseTreeWalker`), registered as
//! `dsl-connection`'s connection grammar for the type:
//!
//! ```text
//! value:  ( store: my::Db; | type: H2; | queryTimeOutInSeconds: 30; | mode: local;
//!         | timezone: 'US/Arizona'; | timezone: -0500; | quoteIdentifiers: true;
//!         | specification: Type { … }; | auth: Type { … };
//!         | postProcessors: [ Type { … }, … ]; | queryGenerationConfigs: [ Type { … }, … ]; )*
//! ```
//!
//! The walker checks the fields once the value is read, in this order: the
//! store (optional), the database type (required, one of the engine's), the
//! query timeout, the time zone, whether to quote identifiers, the
//! post-processors and the query generation configs (each read then), the
//! mode, then, unless the mode is `local`, the datasource specification and
//! the authentication strategy, each required and read then. A `local`
//! connection's are its database type's local mode's. Each field may be
//! written once. The specifications are read by the grammar their type
//! names: see [`specification`].

use legend_pure_dsl_connection::ast::{ConnectionValue, ConnectionValueBody};
use legend_pure_dsl_connection::grammar::{
    CONNECTION_VALUE_GRAMMARS, ConnectionValueGrammar, ValueSpan,
};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::{optional, required};
use legend_pure_parser_parser::words::{
    Name, adjacent, identifier, is_word_kind, qualified_name, unexpected,
};
use legend_pure_parser_parser::{ParserContext, string_value};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;
use serde::Serialize;
use serde_json::Value;
use smol_str::SmolStr;

/// Implements [`specification::SpecificationBody`] for a body type.
macro_rules! body {
    ($type:ty) => {
        impl $crate::connection::specification::SpecificationBody for $type {
            fn as_any(&self) -> &dyn std::any::Any {
                self
            }

            fn clone_box(&self) -> Box<dyn $crate::connection::specification::SpecificationBody> {
                Box::new(self.clone())
            }

            fn eq_content(
                &self,
                other: &dyn $crate::connection::specification::SpecificationBody,
            ) -> bool {
                other
                    .as_any()
                    .downcast_ref::<Self>()
                    .is_some_and(|other| self == other)
            }
        }
    };
}

pub mod authentication;
pub mod datasource;
pub mod field_grammar;
pub mod generation_config;
pub mod post_processor;
pub mod specification;
mod values;

use specification::{
    LocalSpecification, ReadSpecification, Specification, SpecificationKind, local_mode_grammar,
    parse_specification,
};
use values::{at_field, boolean, field, integer};

type R<T> = Result<T, ParseError>;

/// The connection type this grammar reads.
pub const TYPE_NAME: &str = "RelationalDatabaseConnection";

/// The engine's `DatabaseType`s.
const DATABASE_TYPES: [&str; 21] = [
    "DB2",
    "H2",
    "MemSQL",
    "Sybase",
    "SybaseIQ",
    "Composite",
    "Postgres",
    "SqlServer",
    "Hive",
    "Snowflake",
    "Presto",
    "Trino",
    "BigQuery",
    "Redshift",
    "Databricks",
    "Spanner",
    "Athena",
    "DuckDB",
    "Oracle",
    "ClickHouse",
    "Aurora",
];

/// The connection grammar's keywords, which a specification's type can't be.
const KEYWORDS: [&str; 10] = [
    "store",
    "type",
    "mode",
    "specification",
    "auth",
    "postProcessors",
    "queryTimeOutInSeconds",
    "timezone",
    "quoteIdentifiers",
    "queryGenerationConfigs",
];

/// A relational connection, as its walker keeps it.
#[derive(Debug, Clone, PartialEq)]
pub struct RelationalConnection {
    /// `store: my::Db;`: the store's path, unquoted, and the path's span.
    pub store: Option<(String, SourceInfo)>,
    /// The engine's `DatabaseType`.
    pub database_type: SmolStr,
    /// `queryTimeOutInSeconds: 30;`.
    pub query_timeout: Option<i32>,
    /// `timezone: …;`: a zone, unescaped, or an offset as written.
    pub time_zone: Option<String>,
    /// `quoteIdentifiers: true;`.
    pub quote_identifiers: Option<bool>,
    /// `postProcessors: [ … ];`.
    pub post_processors: Option<Vec<Specification>>,
    /// `queryGenerationConfigs: [ … ];`.
    pub query_generation_configs: Option<Vec<Specification>>,
    /// The datasource specification and authentication strategy.
    pub specifications: Specifications,
}

/// A relational connection's datasource specification and authentication
/// strategy.
#[derive(Debug, Clone, PartialEq)]
pub enum Specifications {
    /// `specification: …; auth: …;`.
    Written {
        /// The datasource specification.
        datasource: Box<Specification>,
        /// The authentication strategy.
        authentication: Box<Specification>,
    },
    /// `mode: local;`: its database type's local mode's, their JSON.
    Local {
        /// The datasource specification.
        datasource: LocalSpecification,
        /// The authentication strategy.
        authentication: LocalSpecification,
    },
}

impl ConnectionValueBody for RelationalConnection {
    fn as_any(&self) -> &dyn std::any::Any {
        self
    }

    fn clone_box(&self) -> Box<dyn ConnectionValueBody> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn ConnectionValueBody) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}

/// The relational connection grammar.
pub struct RelationalConnectionGrammar;

/// A connection's fields as its grammar reads them, each as often as
/// written.
#[derive(Default)]
struct ReadFields {
    stores: Vec<(String, SourceInfo)>,
    types: Vec<(Name, SourceInfo)>,
    timeouts: Vec<i32>,
    modes: Vec<Name>,
    time_zones: Vec<String>,
    quote_identifiers: Vec<bool>,
    auths: Vec<ReadSpecification>,
    datasources: Vec<ReadSpecification>,
    post_processors: Vec<Vec<ReadSpecification>>,
    generation_configs: Vec<Vec<ReadSpecification>>,
}

impl ConnectionValueGrammar for RelationalConnectionGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(
        &self,
        span: &ValueSpan,
        ctx: &mut ParserContext<'_>,
    ) -> R<Box<dyn ConnectionValueBody>> {
        let read = read_fields(ctx)?;
        Ok(Box::new(walk(ctx, read, &span.value)?))
    }

    fn convert(&self, value: &ConnectionValue) -> Result<Value, ConvertError> {
        let connection = value
            .body
            .as_any()
            .downcast_ref::<RelationalConnection>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "the body of a relational connection is not the relational connection grammar's",
                    Some(&value.source_info),
                )
            })?;
        convert(connection, value)
    }
}

// ── The grammar ──────────────────────────────────────────────────────────

fn read_fields(ctx: &mut ParserContext<'_>) -> R<ReadFields> {
    let mut read = ReadFields::default();
    loop {
        if at_field(ctx, "store") {
            read.stores.push(
                field(ctx, |ctx| {
                    let name = qualified_name(ctx)?;
                    Ok((name.path, name.source_info))
                })?
                .0,
            );
        } else if at_field(ctx, "type") {
            read.types.push(field(ctx, identifier)?);
        } else if at_field(ctx, "queryTimeOutInSeconds") {
            read.timeouts.push(field(ctx, integer)?.0);
        } else if at_field(ctx, "mode") {
            read.modes.push(field(ctx, identifier)?.0);
        } else if at_field(ctx, "timezone") {
            read.time_zones.push(field(ctx, time_zone)?.0);
        } else if at_field(ctx, "quoteIdentifiers") {
            read.quote_identifiers.push(field(ctx, boolean)?.0);
        } else if at_field(ctx, "auth") || at_field(ctx, "specification") {
            let keyword = ctx.cursor().peek().clone();
            let (specification, source_info) = field(ctx, |ctx| {
                read_specification(ctx, Some(keyword.source_info.clone()))
            })?;
            let specification = ReadSpecification {
                source_info,
                ..specification
            };
            if keyword.text == "auth" {
                read.auths.push(specification);
            } else {
                read.datasources.push(specification);
            }
        } else if at_field(ctx, "postProcessors") {
            read.post_processors.push(field(ctx, specification_list)?.0);
        } else if at_field(ctx, "queryGenerationConfigs") {
            read.generation_configs
                .push(field(ctx, specification_list)?.0);
        } else {
            return Ok(read);
        }
    }
}

/// A time zone: `'US/Arizona'` (its quotes are the grammar's), or an offset
/// from UTC, `-0500`, one token in the engine's lexer.
fn time_zone(ctx: &mut ParserContext<'_>) -> R<String> {
    let sign = ctx.cursor().peek().clone();
    if sign.kind == TokenKind::StringLiteral {
        ctx.cursor().advance();
        return string_value(&sign.text)
            .map(|(value, _)| value)
            .map_err(|message| ParseError::Unexpected {
                message,
                source_info: sign.source_info,
            });
    }
    let digits = ctx.cursor().peek_at(1).clone();
    let offset = matches!(sign.kind, TokenKind::Plus | TokenKind::Minus)
        && digits.kind == TokenKind::IntegerLiteral
        && digits.text.len() == 4
        && adjacent(&sign, &digits);
    if !offset {
        return Err(unexpected(&sign));
    }
    ctx.cursor().advance();
    ctx.cursor().advance();
    Ok(format!("{}{}", sign.text, digits.text))
}

/// `[ Type { … }, … ]`, each positioned on itself, and parsed again from
/// its type.
fn specification_list(ctx: &mut ParserContext<'_>) -> R<Vec<ReadSpecification>> {
    ctx.cursor().expect(TokenKind::LBracket)?;
    let mut specifications = Vec::new();
    if !ctx.cursor().check(TokenKind::RBracket) {
        specifications.push(read_specification(ctx, None)?);
        while ctx.cursor().eat(TokenKind::Comma) {
            specifications.push(read_specification(ctx, None)?);
        }
    }
    ctx.cursor().expect(TokenKind::RBracket)?;
    Ok(specifications)
}

/// `Type { … }` or `Type`, the type a word other than the grammar's
/// keywords, parsed again from `anchor`, or from its type.
fn read_specification(
    ctx: &mut ParserContext<'_>,
    anchor: Option<SourceInfo>,
) -> R<ReadSpecification> {
    let token = ctx.cursor().peek().clone();
    if !is_word_kind(token.kind) || KEYWORDS.contains(&token.text.as_str()) {
        return Err(unexpected(&token));
    }
    ctx.cursor().advance();
    let type_name = Name {
        text: token.text.clone(),
        value: token.text.clone(),
        source_info: token.source_info.clone(),
    };
    let body = if ctx.cursor().check(TokenKind::LBrace) {
        Some(ctx.read_block()?)
    } else {
        None
    };
    Ok(ReadSpecification {
        type_name,
        body,
        source_info: ctx.cursor().span_since(&token.source_info),
        anchor: anchor.unwrap_or(token.source_info),
    })
}

// ── The walk ─────────────────────────────────────────────────────────────

/// The walker's `visitRelationalDatabaseConnectionValue`.
fn walk(ctx: &mut ParserContext<'_>, read: ReadFields, at: &SourceInfo) -> R<RelationalConnection> {
    let store = optional(read.stores, "store", at)?;
    let (database_type, type_source_info) = required(read.types, "type", at)?;
    if !DATABASE_TYPES.contains(&database_type.value.as_str()) {
        return Err(ParseError::Unexpected {
            message: format!("Unknown database type '{}'", database_type.value),
            source_info: type_source_info,
        });
    }
    let query_timeout = optional(read.timeouts, "queryTimeOutInSeconds", at)?;
    let time_zone = optional(read.time_zones, "timezone", at)?;
    let quote_identifiers = optional(read.quote_identifiers, "quoteIdentifiers", at)?;
    let post_processors = optional(read.post_processors, "postProcessors", at)?
        .map(|list| specifications(ctx, SpecificationKind::PostProcessor, &list))
        .transpose()?;
    let query_generation_configs = optional(read.generation_configs, "queryGenerationConfigs", at)?
        .map(|list| specifications(ctx, SpecificationKind::QueryGenerationConfig, &list))
        .transpose()?;
    let mode = optional(read.modes, "mode", at)?;
    let specifications = if mode.is_some_and(|mode| mode.value == "local") {
        let store_path = store.as_ref().map(|(path, _)| path.as_str());
        let Some((datasource, authentication)) = local_mode_grammar(&database_type.value)
            .and_then(|local| local.specifications(store_path))
        else {
            return Err(ParseError::Unsupported {
                message: format!(
                    "no local mode is linked for database type '{}', or it fails, which the engine \
                     reports",
                    database_type.value
                ),
                source_info: at.clone(),
            });
        };
        Specifications::Local {
            datasource,
            authentication,
        }
    } else {
        let datasource = required(read.datasources, "specification", at)?;
        let datasource =
            parse_specification(ctx, SpecificationKind::DatasourceSpecification, &datasource)?;
        let authentication = required(read.auths, "auth", at)?;
        let authentication = parse_specification(
            ctx,
            SpecificationKind::AuthenticationStrategy,
            &authentication,
        )?;
        Specifications::Written {
            datasource: Box::new(datasource),
            authentication: Box::new(authentication),
        }
    };
    Ok(RelationalConnection {
        store,
        database_type: database_type.value,
        query_timeout,
        time_zone,
        quote_identifiers,
        post_processors,
        query_generation_configs,
        specifications,
    })
}

fn specifications(
    ctx: &mut ParserContext<'_>,
    kind: SpecificationKind,
    list: &[ReadSpecification],
) -> R<Vec<Specification>> {
    list.iter()
        .map(|read| parse_specification(ctx, kind, read))
        .collect()
}

// ── The protocol ─────────────────────────────────────────────────────────

/// The engine's `RelationalDatabaseConnection`: its lists are always there,
/// but the query generation configs, which are there when written.
fn convert(
    connection: &RelationalConnection,
    value: &ConnectionValue,
) -> Result<Value, ConvertError> {
    let convert_all = |list: &[Specification]| -> Result<Vec<Value>, ConvertError> {
        list.iter().map(convert_specification).collect()
    };
    let (datasource, authentication) = match &connection.specifications {
        Specifications::Written {
            datasource,
            authentication,
        } => (
            SpecificationJson::Converted(convert_specification(datasource)?),
            SpecificationJson::Converted(convert_specification(authentication)?),
        ),
        Specifications::Local {
            datasource,
            authentication,
        } => (
            SpecificationJson::Local(datasource),
            SpecificationJson::Local(authentication),
        ),
    };
    let post_processors = convert_all(connection.post_processors.as_deref().unwrap_or_default())?;
    let query_generation_configs = connection
        .query_generation_configs
        .as_deref()
        .map(convert_all)
        .transpose()?;
    Ok(serde_json::to_value(RelationalConnectionJson {
        connection_type: TYPE_NAME,
        database_type_name: connection.database_type.as_str(),
        database_type: connection.database_type.as_str(),
        datasource_specification: datasource,
        authentication_strategy: authentication,
        post_processors,
        post_processor_with_parameter: [],
        source_information: SourceInformation::from(&value.source_info),
        element: connection.store.as_ref().map(|(path, _)| path.as_str()),
        element_source_information: connection
            .store
            .as_ref()
            .map(|(_, source_info)| SourceInformation::from(source_info)),
        query_time_out_in_seconds: connection.query_timeout,
        time_zone: connection.time_zone.as_deref(),
        quote_identifiers: connection.quote_identifiers,
        query_generation_configs,
        local_mode: matches!(connection.specifications, Specifications::Local { .. }),
    })?)
}

/// The engine's `RelationalDatabaseConnection`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct RelationalConnectionJson<'a> {
    #[serde(rename = "_type")]
    connection_type: &'static str,
    #[serde(rename = "type")]
    database_type_name: &'a str,
    database_type: &'a str,
    datasource_specification: SpecificationJson<'a>,
    authentication_strategy: SpecificationJson<'a>,
    post_processors: Vec<Value>,
    post_processor_with_parameter: [(); 0],
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    element: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    element_source_information: Option<SourceInformation>,
    #[serde(skip_serializing_if = "Option::is_none")]
    query_time_out_in_seconds: Option<i32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    time_zone: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    quote_identifiers: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    query_generation_configs: Option<Vec<Value>>,
    /// Written only for a connection in local mode.
    #[serde(skip_serializing_if = "std::ops::Not::not")]
    local_mode: bool,
}

/// A datasource specification or an authentication strategy: its grammar's
/// JSON, or a local mode's.
#[derive(Serialize)]
#[serde(untagged)]
enum SpecificationJson<'a> {
    Converted(Value),
    Local(&'a LocalSpecification),
}

/// A specification's JSON, by the grammar that read it.
fn convert_specification(specification: &Specification) -> Result<Value, ConvertError> {
    specification::specification_grammar(specification.kind, &specification.type_name.text)
        .ok_or_else(|| {
            ConvertError::unsupported(
                format!(
                    "no relational {} grammar is linked for '{}'",
                    specification.kind.what(),
                    specification.type_name.text
                ),
                &specification.source_info,
            )
        })?
        .convert(specification)
}

#[distributed_slice(CONNECTION_VALUE_GRAMMARS)]
static RELATIONAL_CONNECTION_GRAMMAR: &(dyn ConnectionValueGrammar + Send + Sync) =
    &RelationalConnectionGrammar;
