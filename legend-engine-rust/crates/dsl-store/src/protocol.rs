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

//! `IslandProtocol` for the relation-store accessor.
//!
//! Tag: `">"`. Converts [`RelationStoreAccessorRef`] AST into the v1
//! protocol's `ClassInstance { _type: ">", … }` value specification,
//! matching the Java engine's JSON shape:
//!
//! ```json
//! {
//!     "_type": ">",
//!     "path": ["my::mainDb", "PersonTable"],
//!     "sourceInformation": "…"
//! }
//! ```
//!
//! Plug-in callers register via [`default_island_protocols`] when
//! calling [`legend_pure_parser_protocol::dispatch_island_convert`].

use legend_pure_parser_ast::island::IslandContent;
use legend_pure_parser_protocol::IslandProtocol;
use legend_pure_parser_protocol::island_protocol::{ConvertError, ISLAND_PROTOCOLS, Result};
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::value_spec::{ClassInstance, ValueSpecification};
use linkme::distributed_slice;
use serde::Serialize;

use crate::ast::{RelationStoreAccessorRef, TAG};

/// Protocol converter for relation-store accessors. Emits the
/// canonical `_type: ">"` JSON shape.
pub struct RelationStoreAccessorProtocol;

impl IslandProtocol for RelationStoreAccessorProtocol {
    fn tag(&self) -> &str {
        TAG
    }

    fn convert(&self, content: &dyn IslandContent) -> Result<ValueSpecification> {
        let accessor = content
            .as_any()
            .downcast_ref::<RelationStoreAccessorRef>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "RelationStoreAccessorProtocol: content with tag '{}' is not a \
                         RelationStoreAccessorRef",
                        content.tag()
                    ),
                    None,
                )
            })?;
        convert_relation_store_accessor(accessor)
    }
}

#[distributed_slice(ISLAND_PROTOCOLS)]
static RELATION_STORE_ACCESSOR_PROTOCOL: &(dyn IslandProtocol + Send + Sync) =
    &RelationStoreAccessorProtocol;

/// Convenience helper —
/// `vec![Box::new(RelationStoreAccessorProtocol)]` for callers that
/// want the store DSL protocol converter registered.
#[must_use]
pub fn default_island_protocols() -> Vec<Box<dyn IslandProtocol>> {
    vec![Box::new(RelationStoreAccessorProtocol)]
}

/// The engine's `RelationStoreAccessor` (`{path, sourceInformation}`),
/// positioned, like its class instance, on the whole island.
fn convert_relation_store_accessor(
    accessor: &RelationStoreAccessorRef,
) -> Result<ValueSpecification> {
    let position = SourceInformation::from(&accessor.source_info);
    Ok(ValueSpecification::ClassInstance(ClassInstance {
        type_name: TAG.to_string(),
        value: serde_json::to_value(RelationStoreAccessorJson {
            path: accessor.path.iter().map(ToString::to_string).collect(),
            source_information: position.clone(),
        })?,
        source_information: Some(position),
    }))
}

/// The engine's `RelationStoreAccessor` value.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct RelationStoreAccessorJson {
    path: Vec<String>,
    source_information: SourceInformation,
}
