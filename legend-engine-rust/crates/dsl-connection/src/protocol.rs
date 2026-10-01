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

//! `ElementProtocol` for connections: the engine's `PackageableConnection`.
//!
//! ```json
//! {
//!     "_type": "connection",
//!     "package": "my",
//!     "name": "PeopleConnection",
//!     "connectionValue": { "_type": "JsonModelConnection", … },
//!     "sourceInformation": { … }
//! }
//! ```
//!
//! A value is converted by the grammar that read it:
//! [`convert_connection_value`], which runtimes use for their own
//! connections too.

use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;
use serde_json::json;

use crate::ast::{ConnectionValue, KIND, PackageableConnection};
use crate::grammar::connection_value_grammar;

/// Protocol converter for connections.
pub struct ConnectionProtocol;

impl ElementProtocol for ConnectionProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<serde_json::Value> {
        let connection = element
            .as_any()
            .downcast_ref::<PackageableConnection>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "ConnectionProtocol: a '{}' element is not a PackageableConnection",
                        element.kind()
                    ),
                    None,
                )
            })?;
        Ok(json!({
            "_type": "connection",
            "package": connection
                .package
                .as_ref()
                .map(ToString::to_string)
                .unwrap_or_default(),
            "name": connection.name.value.as_str(),
            "connectionValue": convert_connection_value(&connection.value)?,
            "sourceInformation": SourceInformation::from(&connection.source_info),
        }))
    }
}

/// The engine's JSON of a connection value, by the grammar that read it.
///
/// # Errors
///
/// No linked grammar reads the value's type (unsupported), or the grammar's
/// conversion fails.
pub fn convert_connection_value(value: &ConnectionValue) -> Result<serde_json::Value> {
    let type_name = &value.type_name.text;
    connection_value_grammar(type_name)
        .ok_or_else(|| {
            ConvertError::unsupported(
                format!("no connection grammar is linked for '{type_name}'"),
                &value.source_info,
            )
        })?
        .convert(value)
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static CONNECTION_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &ConnectionProtocol;
