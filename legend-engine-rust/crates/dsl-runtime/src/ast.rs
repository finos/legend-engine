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

//! The runtime AST.

use std::any::Any;

use legend_pure_dsl_connection::ast::ConnectionValue;
use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use smol_str::SmolStr;

/// The section kind, `###Runtime`, which is also the elements' kind.
pub const KIND: &str = "Runtime";

/// `Runtime my::R { … }` or `SingleConnectionRuntime my::R { … }`: a
/// packageable runtime, the engine's `PackageableRuntime`.
#[derive(Debug, Clone, PartialEq)]
pub struct PackageableRuntime {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// Whether a `SingleConnectionRuntime`: one connection, its value's
    /// only connection store.
    pub single_connection: bool,
    /// The runtime's mappings and connections.
    pub value: EngineRuntime,
    /// From `Runtime` to `}`.
    pub source_info: SourceInfo,
}

/// A runtime's mappings and connections, the engine's `EngineRuntime`.
#[derive(Debug, Clone, PartialEq)]
pub struct EngineRuntime {
    /// `mappings: [ my::M, … ];`.
    pub mappings: Vec<ElementPath>,
    /// `connections: [ my::Store: [ id: connection, … ], … ];`, as written:
    /// the engine keeps a store's connections only when it has some.
    pub connections: Vec<StoreConnections>,
    /// `connectionStores: [ my::Connection: [ my::Store, … ], … ];`, or a
    /// single connection runtime's connection.
    pub connection_stores: Vec<ConnectionStores>,
    /// The runtime's span.
    pub source_info: SourceInfo,
}

/// A path to an element: a mapping, a store or a connection.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ElementPath {
    /// The path, unquoted.
    pub path: String,
    /// The path.
    pub source_info: SourceInfo,
}

/// `my::Store: [ id: connection, … ]`: a store's connections.
#[derive(Debug, Clone, PartialEq)]
pub struct StoreConnections {
    /// The store.
    pub store: ElementPath,
    /// Its connections.
    pub connections: Vec<IdentifiedConnection>,
    /// From the store to `]`.
    pub source_info: SourceInfo,
}

/// `id: my::Connection` or `id: #{ Type { … } }#`.
#[derive(Debug, Clone, PartialEq)]
pub struct IdentifiedConnection {
    /// The id, unquoted.
    pub id: SmolStr,
    /// The connection.
    pub connection: RuntimeConnection,
    /// From the id to the connection's end.
    pub source_info: SourceInfo,
}

/// A runtime's connection: a packageable connection's path, or its own.
#[derive(Debug, Clone, PartialEq)]
pub enum RuntimeConnection {
    /// `my::Connection`.
    Pointer(ElementPath),
    /// `#{ Type { … } }#`.
    Embedded(ConnectionValue),
}

/// `my::Connection: [ my::Store, (dataspace) my::DataSpace, … ]`: the stores
/// a connection is for.
#[derive(Debug, Clone, PartialEq)]
pub struct ConnectionStores {
    /// The connection.
    pub connection: ElementPath,
    /// The stores.
    pub stores: Vec<StorePointer>,
    /// From the connection to `]`; a single connection runtime's
    /// connection's path.
    pub source_info: SourceInfo,
}

/// `(type) my::Store`: an element providing stores.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct StorePointer {
    /// The engine's `PackageableElementType` the type names, upper case.
    pub element_type: Option<SmolStr>,
    /// The path, unquoted.
    pub path: String,
    /// From `(`, or the path, to the path's end.
    pub source_info: SourceInfo,
}

impl Spanned for PackageableRuntime {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for PackageableRuntime {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl PackageableElement for PackageableRuntime {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for PackageableRuntime {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        KIND
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
