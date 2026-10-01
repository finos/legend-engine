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

//! The walk (the engine's `SnowflakeTreeWalker`): each element's fields
//! checked and built in the walker's order, into records shaped as the
//! engine's protocol.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::blocks::{ReadBlock, ReadField, ReadValue};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::record::Record;

use crate::ast::SnowflakeKind;

type R<T> = Result<T, ParseError>;

/// The engine's permission schemes.
const PERMISSION_SCHEMES: [&str; 2] = ["DEFAULT", "SEQUESTERED"];

/// A value the grammar can't have read where it stands: a bug.
fn misread(what: &str, at: &SourceInfo) -> ParseError {
    ParseError::Unexpected {
        message: format!("the Snowflake grammar misread {what}"),
        source_info: at.clone(),
    }
}

/// The walker's `visitSnowflakeApp` or `visitSnowflakeM2MUdf`: the
/// element's own record, without its package, name and annotations.
pub(crate) fn element(kind: SnowflakeKind, read: ReadBlock) -> R<Record> {
    match kind {
        SnowflakeKind::App => app(read),
        SnowflakeKind::M2MUdf => udf(read),
    }
}

fn optional_string(read: &mut ReadBlock, name: &'static str, at: &SourceInfo) -> R<Option<String>> {
    read.take_optional(name, at)?
        .map(ReadField::string)
        .transpose()
}

/// The activated function, positioned on its identifier.
fn function(field: ReadField) -> R<Record> {
    let ReadValue::Text(path, at) = field.value else {
        return Err(misread("a function", &field.source_info));
    };
    Ok(Record::untyped()
        .with("type", "FUNCTION".to_string())
        .with("path", path)
        .at(&at))
}

/// The owner, a deployment's.
fn ownership(field: ReadField) -> R<Record> {
    Ok(Record::new("DeploymentOwner").with("id", field.string()?))
}

/// The deployment configuration of `kind`, its connection positioned.
fn activation(read: &mut ReadBlock, kind: &'static str, at: &SourceInfo) -> R<Option<Record>> {
    read.take_optional("activation", at)?
        .map(|field| {
            let ReadValue::Path(connection) = field.value else {
                return Err(misread("a connection", &field.source_info));
            };
            Ok(Record::new(kind).with(
                "activationConnection",
                Record::new("connectionPointer")
                    .with("connection", connection.path)
                    .at(&connection.source_info),
            ))
        })
        .transpose()
}

/// The walker's `visitSnowflakeApp`.
fn app(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let application_name = read.take("applicationName", &at)?.string()?;
    let function = function(read.take("function", &at)?)?;
    let ownership = ownership(read.take("ownership", &at)?)?;
    let description = optional_string(&mut read, "description", &at)?;
    let usage_role = optional_string(&mut read, "role", &at)?;
    let permission_scheme = match read.take_optional("scheme", &at)? {
        Some(field) => {
            let field_at = field.source_info.clone();
            let scheme = field.identifier()?;
            if !PERMISSION_SCHEMES.contains(&scheme.as_str()) {
                return Err(ParseError::Unexpected {
                    message: format!("Unknown permission scheme '{scheme}'"),
                    source_info: field_at,
                });
            }
            Some(scheme)
        }
        None => None,
    };
    let deployment_schema = optional_string(&mut read, "deploymentSchema", &at)?;
    let activation = activation(&mut read, "snowflakeDeploymentConfiguration", &at)?;
    Ok(Record::new("snowflakeApp")
        .at(&at)
        .with("applicationName", application_name)
        .with("function", function)
        .with("ownership", ownership)
        .with_some("description", description)
        .with_some("usageRole", usage_role)
        .with_some("permissionScheme", permission_scheme)
        .with_some("deploymentSchema", deployment_schema)
        .with_some("activationConfiguration", activation))
}

/// The walker's `visitSnowflakeM2MUdf`.
fn udf(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let udf_name = read.take("udfName", &at)?.string()?;
    let function = function(read.take("function", &at)?)?;
    let ownership = ownership(read.take("ownership", &at)?)?;
    let description = optional_string(&mut read, "description", &at)?;
    let deployment_schema = read.take("deploymentSchema", &at)?.string()?;
    let deployment_stage = read.take("deploymentStage", &at)?.string()?;
    let activation = activation(&mut read, "snowflakeM2MUdfDeploymentConfiguration", &at)?;
    Ok(Record::new("snowflakeM2MUdf")
        .at(&at)
        .with("udfName", udf_name)
        .with("function", function)
        .with("ownership", ownership)
        .with_some("description", description)
        .with("deploymentSchema", deployment_schema)
        .with("deploymentStage", deployment_stage)
        .with_some("activationConfiguration", activation))
}
