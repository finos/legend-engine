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

//! `ElementProtocol` for runtimes: the engine's `PackageableRuntime`.
//!
//! ```json
//! {
//!     "_type": "runtime",
//!     "package": "my",
//!     "name": "Runtime",
//!     "runtimeValue": {
//!         "_type": "engineRuntime",
//!         "mappings": [ { "type": "MAPPING", "path": "my::Mapping", … } ],
//!         "connections": [ { "store": { "type": "STORE", … }, "storeConnections": [ … ], … } ],
//!         "connectionStores": [ { "connectionPointer": { … }, "storePointers": [ … ], … } ],
//!         "sourceInformation": { … }
//!     },
//!     "sourceInformation": { … }
//! }
//! ```
//!
//! A single connection runtime's value is a `localEngineRuntime`. A store's
//! connections are there only when it has some.

use legend_pure_dsl_connection::protocol::convert_connection_value;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use legend_pure_parser_protocol::v1::element::PackageableElementPointer;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;
use serde_json::{Value, json};

use crate::ast::{
    ConnectionStores, ElementPath, EngineRuntime, KIND, PackageableRuntime, RuntimeConnection,
    StoreConnections,
};

/// Protocol converter for runtimes.
pub struct RuntimeProtocol;

impl ElementProtocol for RuntimeProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Value> {
        let runtime = element
            .as_any()
            .downcast_ref::<PackageableRuntime>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "RuntimeProtocol: a '{}' element is not a PackageableRuntime",
                        element.kind()
                    ),
                    None,
                )
            })?;
        let mut value = convert_engine_runtime(&runtime.value)?;
        if runtime.single_connection {
            value["_type"] = json!("localEngineRuntime");
        }
        Ok(json!({
            "_type": "runtime",
            "package": runtime
                .package
                .as_ref()
                .map(ToString::to_string)
                .unwrap_or_default(),
            "name": runtime.name.value.as_str(),
            "runtimeValue": value,
            "sourceInformation": SourceInformation::from(&runtime.source_info),
        }))
    }
}

/// The engine's `EngineRuntime`.
///
/// # Errors
///
/// A runtime's own connection fails to convert.
pub fn convert_engine_runtime(runtime: &EngineRuntime) -> Result<Value> {
    Ok(json!({
        "_type": "engineRuntime",
        "mappings": runtime
            .mappings
            .iter()
            .map(|mapping| pointer("MAPPING", mapping))
            .collect::<Vec<_>>(),
        "connections": runtime
            .connections
            .iter()
            .filter(|store| !store.connections.is_empty())
            .map(store_connections)
            .collect::<Result<Vec<_>>>()?,
        "connectionStores": runtime
            .connection_stores
            .iter()
            .map(connection_stores)
            .collect::<Vec<_>>(),
        "sourceInformation": SourceInformation::from(&runtime.source_info),
    }))
}

fn store_connections(store: &StoreConnections) -> Result<Value> {
    Ok(json!({
        "store": pointer("STORE", &store.store),
        "storeConnections": store
            .connections
            .iter()
            .map(|identified| {
                let connection = match &identified.connection {
                    RuntimeConnection::Pointer(path) => connection_pointer(path),
                    RuntimeConnection::Embedded(value) => convert_connection_value(value)?,
                };
                Ok(json!({
                    "id": identified.id.as_str(),
                    "connection": connection,
                    "sourceInformation": SourceInformation::from(&identified.source_info),
                }))
            })
            .collect::<Result<Vec<_>>>()?,
        "sourceInformation": SourceInformation::from(&store.source_info),
    }))
}

fn connection_stores(stores: &ConnectionStores) -> Value {
    json!({
        "connectionPointer": connection_pointer(&stores.connection),
        "storePointers": stores
            .stores
            .iter()
            .map(|store| PackageableElementPointer {
                element_type: store.element_type.as_ref().map(ToString::to_string),
                path: store.path.clone(),
                source_information: Some(SourceInformation::from(&store.source_info)),
            })
            .collect::<Vec<_>>(),
        "sourceInformation": SourceInformation::from(&stores.source_info),
    })
}

/// The engine's `ConnectionPointer`, positioned on the path.
fn connection_pointer(path: &ElementPath) -> Value {
    json!({
        "_type": "connectionPointer",
        "connection": path.path,
        "sourceInformation": SourceInformation::from(&path.source_info),
    })
}

/// A pointer of `element_type`, positioned on the path.
fn pointer(element_type: &str, path: &ElementPath) -> PackageableElementPointer {
    PackageableElementPointer {
        element_type: Some(element_type.to_string()),
        path: path.path.clone(),
        source_information: Some(SourceInformation::from(&path.source_info)),
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static RUNTIME_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &RuntimeProtocol;
