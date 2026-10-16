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

//! The relational store's datasource specifications
//! (`DataSourceSpecificationParserGrammar` and its
//! `DataSourceSpecificationParseTreeWalker`):
//!
//! ```text
//! LocalH2 { ( testDataSetupCSV: 'csv'; | testDataSetupSqls: [ 'sql', … ]; )* }
//! Static { ( host: 'host'; | port: 1234; | name: 'database'; )* }
//! EmbeddedH2 { ( name: 'database'; | directory: 'directory'; | autoServerMode: true; )* }
//! ```
//!
//! Each positioned on its field, where its walker reports a field missing or
//! written twice; `LocalH2` keeps the first `testDataSetupSqls` without
//! checking for another.

use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::{optional, required};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde::Serialize;
use serde_json::Value;

use super::specification::{
    BodyUse, Specification, SpecificationBody, SpecificationGrammar, SpecificationKind,
    SpecificationSpan, downcast,
};
use super::values::{at_field, boolean, field, integer, string, strings};

type R<T> = Result<T, ParseError>;

/// The relational store's datasource specification grammars.
pub(crate) static CORE_DATASOURCE_SPECIFICATIONS: [&(dyn SpecificationGrammar + Send + Sync); 3] =
    [&LocalH2Grammar, &StaticGrammar, &EmbeddedH2Grammar];

/// `LocalH2`: an in-memory H2 database, set up by the test data.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LocalH2 {
    /// `testDataSetupCSV`, unescaped.
    pub test_data_setup_csv: Option<String>,
    /// The first `testDataSetupSqls`, unescaped.
    pub test_data_setup_sqls: Option<Vec<String>>,
}

/// `Static`: a database at a host and port.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Static {
    /// The host, unescaped.
    pub host: String,
    /// The port.
    pub port: i32,
    /// The database, unescaped.
    pub database_name: String,
}

/// `EmbeddedH2`: an H2 database in a directory.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct EmbeddedH2 {
    /// The database, unescaped.
    pub database_name: String,
    /// The directory, unescaped.
    pub directory: String,
    /// Whether in auto server mode.
    pub auto_server_mode: bool,
}

body!(LocalH2);
body!(Static);
body!(EmbeddedH2);

/// The `LocalH2` grammar.
pub struct LocalH2Grammar;

/// The `Static` grammar.
pub struct StaticGrammar;

/// The `EmbeddedH2` grammar.
pub struct EmbeddedH2Grammar;

impl SpecificationGrammar for LocalH2Grammar {
    fn kind(&self) -> SpecificationKind {
        SpecificationKind::DatasourceSpecification
    }

    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "LocalH2"
    }

    fn body_use(&self) -> BodyUse {
        BodyUse::Required
    }

    fn parse(
        &self,
        span: &SpecificationSpan,
        body: Option<&mut ParserContext<'_>>,
    ) -> R<Box<dyn SpecificationBody>> {
        let (mut csvs, mut sqls) = (Vec::new(), Vec::new());
        if let Some(ctx) = body {
            loop {
                if at_field(ctx, "testDataSetupCSV") {
                    csvs.push(field(ctx, string)?.0);
                } else if at_field(ctx, "testDataSetupSqls") {
                    sqls.push(field(ctx, strings)?.0);
                } else {
                    break;
                }
            }
        }
        let test_data_setup_csv = optional(csvs, "testDataSetupCsv", &span.source_info)?;
        Ok(Box::new(LocalH2 {
            test_data_setup_csv,
            test_data_setup_sqls: sqls.into_iter().next(),
        }))
    }

    fn convert(&self, specification: &Specification) -> Result<Value, ConvertError> {
        let local = downcast::<LocalH2>(specification)?;
        Ok(serde_json::to_value(LocalH2Json {
            source_information: SourceInformation::from(&specification.source_info),
            test_data_setup_csv: local.test_data_setup_csv.as_deref(),
            test_data_setup_sqls: local.test_data_setup_sqls.as_deref(),
        })?)
    }
}

impl SpecificationGrammar for StaticGrammar {
    fn kind(&self) -> SpecificationKind {
        SpecificationKind::DatasourceSpecification
    }

    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "Static"
    }

    fn body_use(&self) -> BodyUse {
        BodyUse::Required
    }

    fn parse(
        &self,
        span: &SpecificationSpan,
        body: Option<&mut ParserContext<'_>>,
    ) -> R<Box<dyn SpecificationBody>> {
        let (mut names, mut hosts, mut ports) = (Vec::new(), Vec::new(), Vec::new());
        if let Some(ctx) = body {
            loop {
                if at_field(ctx, "name") {
                    names.push(field(ctx, string)?.0);
                } else if at_field(ctx, "host") {
                    hosts.push(field(ctx, string)?.0);
                } else if at_field(ctx, "port") {
                    ports.push(field(ctx, integer)?.0);
                } else {
                    break;
                }
            }
        }
        let at = &span.source_info;
        let host = required(hosts, "host", at)?;
        let port = required(ports, "port", at)?;
        let database_name = required(names, "name", at)?;
        Ok(Box::new(Static {
            host,
            port,
            database_name,
        }))
    }

    fn convert(&self, specification: &Specification) -> Result<Value, ConvertError> {
        let fixed = downcast::<Static>(specification)?;
        Ok(serde_json::to_value(StaticJson {
            host: &fixed.host,
            port: fixed.port,
            database_name: &fixed.database_name,
            source_information: SourceInformation::from(&specification.source_info),
        })?)
    }
}

impl SpecificationGrammar for EmbeddedH2Grammar {
    fn kind(&self) -> SpecificationKind {
        SpecificationKind::DatasourceSpecification
    }

    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "EmbeddedH2"
    }

    fn body_use(&self) -> BodyUse {
        BodyUse::Required
    }

    fn parse(
        &self,
        span: &SpecificationSpan,
        body: Option<&mut ParserContext<'_>>,
    ) -> R<Box<dyn SpecificationBody>> {
        let (mut names, mut directories, mut modes) = (Vec::new(), Vec::new(), Vec::new());
        if let Some(ctx) = body {
            loop {
                if at_field(ctx, "name") {
                    names.push(field(ctx, string)?.0);
                } else if at_field(ctx, "directory") {
                    directories.push(field(ctx, string)?.0);
                } else if at_field(ctx, "autoServerMode") {
                    modes.push(field(ctx, boolean)?.0);
                } else {
                    break;
                }
            }
        }
        let at = &span.source_info;
        let database_name = required(names, "name", at)?;
        let directory = required(directories, "directory", at)?;
        let auto_server_mode = required(modes, "autoServerMode", at)?;
        Ok(Box::new(EmbeddedH2 {
            database_name,
            directory,
            auto_server_mode,
        }))
    }

    fn convert(&self, specification: &Specification) -> Result<Value, ConvertError> {
        let embedded = downcast::<EmbeddedH2>(specification)?;
        Ok(serde_json::to_value(EmbeddedH2Json {
            database_name: &embedded.database_name,
            directory: &embedded.directory,
            auto_server_mode: embedded.auto_server_mode,
            source_information: SourceInformation::from(&specification.source_info),
        })?)
    }
}

/// The engine's `LocalH2DatasourceSpecification`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "h2Local", rename_all = "camelCase")]
struct LocalH2Json<'a> {
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    test_data_setup_csv: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    test_data_setup_sqls: Option<&'a [String]>,
}

/// The engine's `StaticDatasourceSpecification`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "static", rename_all = "camelCase")]
struct StaticJson<'a> {
    host: &'a str,
    port: i32,
    database_name: &'a str,
    source_information: SourceInformation,
}

/// The engine's `EmbeddedH2DatasourceSpecification`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "h2Embedded", rename_all = "camelCase")]
struct EmbeddedH2Json<'a> {
    database_name: &'a str,
    directory: &'a str,
    auto_server_mode: bool,
    source_information: SourceInformation,
}
