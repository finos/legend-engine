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

//! `IslandProtocol` for TDS literals.
//!
//! Tag: `"TDS"`. Converts [`TdsLiteral`] into the engine's JSON shape, a
//! class instance of type `TDS` holding a `TDSContainer`:
//!
//! ```json
//! {
//!     "_type": "classInstance",
//!     "type": "TDS",
//!     "value": { "tdsString": "\n  id, name\n  1, Alice\n" },
//!     "sourceInformation": { … }
//! }
//! ```

use legend_pure_parser_ast::island::IslandContent;
use legend_pure_parser_protocol::IslandProtocol;
use legend_pure_parser_protocol::island_protocol::{ConvertError, ISLAND_PROTOCOLS, Result};
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::value_spec::{ClassInstance, ValueSpecification};
use linkme::distributed_slice;

use crate::ast::{TAG, TdsLiteral};

/// Protocol converter for TDS literals.
pub struct TdsIslandProtocol;

impl IslandProtocol for TdsIslandProtocol {
    fn tag(&self) -> &str {
        TAG
    }

    fn convert(&self, content: &dyn IslandContent) -> Result<ValueSpecification> {
        let literal = content
            .as_any()
            .downcast_ref::<TdsLiteral>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "TdsIslandProtocol: content with tag '{}' is not a TdsLiteral",
                        content.tag()
                    ),
                    None,
                )
            })?;
        // Positioned, like the engine's, on the whole island.
        Ok(ValueSpecification::ClassInstance(ClassInstance {
            type_name: TAG.to_string(),
            value: serde_json::json!({ "tdsString": literal.text.as_str() }),
            source_information: Some(SourceInformation::from(&literal.source_info)),
        }))
    }
}

#[distributed_slice(ISLAND_PROTOCOLS)]
static TDS_ISLAND_PROTOCOL: &(dyn IslandProtocol + Send + Sync) = &TdsIslandProtocol;

/// Convenience helper — `vec![Box::new(TdsIslandProtocol)]` for callers
/// that want the TDS protocol converter registered.
#[must_use]
pub fn default_island_protocols() -> Vec<Box<dyn IslandProtocol>> {
    vec![Box::new(TdsIslandProtocol)]
}
