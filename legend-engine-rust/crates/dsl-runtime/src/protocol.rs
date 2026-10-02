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
//!         "sourceInformation": "…"
//!     },
//!     "sourceInformation": "…"
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
use serde::Serialize;
use serde_json::Value;
use serde_json::value::{RawValue, to_raw_value};

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

    fn convert(&self, element: &dyn DSLElement) -> Result<Box<RawValue>> {
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
        let mut runtime_value = EngineRuntimeJson::new(&runtime.value)?;
        if runtime.single_connection {
            runtime_value.runtime_type = "localEngineRuntime";
        }
        Ok(to_raw_value(&PackageableRuntimeJson {
            package: runtime
                .package
                .as_ref()
                .map(ToString::to_string)
                .unwrap_or_default(),
            name: runtime.name.value.as_str(),
            runtime_value,
            source_information: SourceInformation::from(&runtime.source_info),
        })?)
    }
}

/// The engine's `PackageableRuntime`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "runtime", rename_all = "camelCase")]
struct PackageableRuntimeJson<'a> {
    package: String,
    name: &'a str,
    runtime_value: EngineRuntimeJson<'a>,
    source_information: SourceInformation,
}

/// The engine's `EngineRuntime`.
///
/// # Errors
///
/// A runtime's own connection fails to convert.
pub fn convert_engine_runtime(runtime: &EngineRuntime) -> Result<Value> {
    Ok(serde_json::to_value(EngineRuntimeJson::new(runtime)?)?)
}

/// The engine's `EngineRuntime`, ready to serialize: [`convert_engine_runtime`]
/// without building a JSON value first, for a protocol object that holds
/// one.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct EngineRuntimeJson<'a> {
    #[serde(rename = "_type")]
    runtime_type: &'static str,
    mappings: Vec<PackageableElementPointer>,
    connections: Vec<StoreConnectionsJson<'a>>,
    connection_stores: Vec<ConnectionStoresJson<'a>>,
    source_information: SourceInformation,
}

impl<'a> EngineRuntimeJson<'a> {
    /// `runtime` as the engine's `EngineRuntime`. A store's connections are
    /// there only when it has some.
    ///
    /// # Errors
    ///
    /// A runtime's own connection fails to convert.
    pub fn new(runtime: &'a EngineRuntime) -> Result<Self> {
        Ok(Self {
            runtime_type: "engineRuntime",
            mappings: runtime
                .mappings
                .iter()
                .map(|mapping| pointer("MAPPING", mapping))
                .collect(),
            connections: runtime
                .connections
                .iter()
                .filter(|store| !store.connections.is_empty())
                .map(store_connections)
                .collect::<Result<Vec<_>>>()?,
            connection_stores: runtime
                .connection_stores
                .iter()
                .map(ConnectionStoresJson::from)
                .collect(),
            source_information: SourceInformation::from(&runtime.source_info),
        })
    }
}

/// The engine's `StoreConnections`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct StoreConnectionsJson<'a> {
    store: PackageableElementPointer,
    store_connections: Vec<IdentifiedConnectionJson<'a>>,
    source_information: SourceInformation,
}

/// The engine's `IdentifiedConnection`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct IdentifiedConnectionJson<'a> {
    id: &'a str,
    connection: ConnectionJson<'a>,
    source_information: SourceInformation,
}

/// A connection: a pointer, or one of the runtime's own, its grammar's JSON.
#[derive(Serialize)]
#[serde(untagged)]
enum ConnectionJson<'a> {
    Pointer(ConnectionPointerJson<'a>),
    Embedded(Value),
}

fn store_connections(store: &StoreConnections) -> Result<StoreConnectionsJson<'_>> {
    Ok(StoreConnectionsJson {
        store: pointer("STORE", &store.store),
        store_connections: store
            .connections
            .iter()
            .map(|identified| {
                let connection = match &identified.connection {
                    RuntimeConnection::Pointer(path) => {
                        ConnectionJson::Pointer(ConnectionPointerJson::from(path))
                    }
                    RuntimeConnection::Embedded(value) => {
                        ConnectionJson::Embedded(convert_connection_value(value)?)
                    }
                };
                Ok(IdentifiedConnectionJson {
                    id: identified.id.as_str(),
                    connection,
                    source_information: SourceInformation::from(&identified.source_info),
                })
            })
            .collect::<Result<Vec<_>>>()?,
        source_information: SourceInformation::from(&store.source_info),
    })
}

/// The engine's `ConnectionStores`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ConnectionStoresJson<'a> {
    connection_pointer: ConnectionPointerJson<'a>,
    store_pointers: Vec<PackageableElementPointer>,
    source_information: SourceInformation,
}

impl<'a> From<&'a ConnectionStores> for ConnectionStoresJson<'a> {
    fn from(stores: &'a ConnectionStores) -> Self {
        Self {
            connection_pointer: ConnectionPointerJson::from(&stores.connection),
            store_pointers: stores
                .stores
                .iter()
                .map(|store| PackageableElementPointer {
                    element_type: store.element_type.as_ref().map(ToString::to_string),
                    path: store.path.clone(),
                    source_information: Some(SourceInformation::from(&store.source_info)),
                })
                .collect(),
            source_information: SourceInformation::from(&stores.source_info),
        }
    }
}

/// The engine's `ConnectionPointer`, positioned on the path.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "connectionPointer", rename_all = "camelCase")]
struct ConnectionPointerJson<'a> {
    connection: &'a str,
    source_information: SourceInformation,
}

impl<'a> From<&'a ElementPath> for ConnectionPointerJson<'a> {
    fn from(path: &'a ElementPath) -> Self {
        Self {
            connection: &path.path,
            source_information: SourceInformation::from(&path.source_info),
        }
    }
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
