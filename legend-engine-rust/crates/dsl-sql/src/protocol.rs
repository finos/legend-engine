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

//! `IslandProtocol` for SQL expressions.
//!
//! Tag: `"SQL"`. Converts [`SqlExpression`] into the engine's JSON shape, a
//! class instance of type `SQL` holding a `SQLExpressionProtocol`:
//!
//! ```json
//! {
//!     "_type": "classInstance",
//!     "type": "SQL",
//!     "value": { "sql": "select a from csv('a,b\n1,2')" },
//!     "sourceInformation": "…"
//! }
//! ```

use legend_pure_parser_ast::island::IslandContent;
use legend_pure_parser_protocol::IslandProtocol;
use legend_pure_parser_protocol::island_protocol::{ConvertError, ISLAND_PROTOCOLS, Result};
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::value_spec::{ClassInstance, ValueSpecification};
use linkme::distributed_slice;
use serde::Serialize;

use crate::ast::{SqlExpression, TAG};

/// Protocol converter for SQL expressions.
pub struct SqlIslandProtocol;

impl IslandProtocol for SqlIslandProtocol {
    fn tag(&self) -> &str {
        TAG
    }

    fn convert(&self, content: &dyn IslandContent) -> Result<ValueSpecification> {
        let expression = content
            .as_any()
            .downcast_ref::<SqlExpression>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "SqlIslandProtocol: content with tag '{}' is not a SqlExpression",
                        content.tag()
                    ),
                    None,
                )
            })?;
        // Positioned, like the engine's, on the whole island.
        Ok(ValueSpecification::ClassInstance(ClassInstance {
            type_name: TAG.to_string(),
            value: serde_json::to_value(SqlJson {
                sql: expression.text.as_str(),
            })?,
            source_information: Some(SourceInformation::from(&expression.source_info)),
        }))
    }
}

/// The engine's SQL class instance value: the SQL as written.
#[derive(Serialize)]
struct SqlJson<'a> {
    sql: &'a str,
}

#[distributed_slice(ISLAND_PROTOCOLS)]
static SQL_ISLAND_PROTOCOL: &(dyn IslandProtocol + Send + Sync) = &SqlIslandProtocol;

/// Convenience helper — `vec![Box::new(SqlIslandProtocol)]` for callers
/// that want the SQL protocol converter registered.
#[must_use]
pub fn default_island_protocols() -> Vec<Box<dyn IslandProtocol>> {
    vec![Box::new(SqlIslandProtocol)]
}
