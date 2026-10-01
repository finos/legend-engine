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

//! Plug-in trait for converting embedded data content into protocol JSON.
//!
//! Parser-side analog: `legend_pure_parser_parser::EmbeddedDataParser`. A
//! crate that owns an embedded data grammar registers both halves under the
//! same data type:
//!
//! ```ignore
//! use legend_pure_parser_protocol::embedded_data_protocol::{
//!     EmbeddedDataProtocol, EMBEDDED_DATA_PROTOCOLS,
//! };
//! use linkme::distributed_slice;
//!
//! #[distributed_slice(EMBEDDED_DATA_PROTOCOLS)]
//! static MY_DATA: &(dyn EmbeddedDataProtocol + Send + Sync) = &MyDataProtocol;
//! ```

use std::collections::HashSet;
use std::sync::OnceLock;

use legend_pure_parser_ast::data::EmbeddedDataContent;
use linkme::distributed_slice;

pub use crate::error::{ConvertError, Result};

/// Converts one embedded data grammar's content into the engine's JSON for
/// it: an `EmbeddedData` subtype, with its `_type`.
pub trait EmbeddedDataProtocol: Send + Sync {
    /// The data type this converter handles: the same string the matching
    /// `EmbeddedDataParser::type_name()` returns, e.g. `Relational`.
    fn type_name(&self) -> &str;

    /// Converts `content`, which implementations downcast to their own type.
    ///
    /// # Errors
    /// Returns [`ConvertError::Plugin`] on a downcast mismatch.
    fn convert(&self, content: &dyn EmbeddedDataContent) -> Result<serde_json::Value>;
}

/// Distributed slice into which each embedded data grammar crate registers
/// its [`EmbeddedDataProtocol`]. See the module docs.
#[distributed_slice]
pub static EMBEDDED_DATA_PROTOCOLS: [&'static (dyn EmbeddedDataProtocol + Send + Sync)] = [..];

/// The registered [`EMBEDDED_DATA_PROTOCOLS`], validated once per process.
///
/// # Panics
///
/// Panics when two registered converters share a data type: a link-time
/// misconfiguration.
#[must_use]
pub fn discovered_embedded_data_protocols()
-> &'static [&'static (dyn EmbeddedDataProtocol + Send + Sync)] {
    static DISCOVERED: OnceLock<Vec<&'static (dyn EmbeddedDataProtocol + Send + Sync)>> =
        OnceLock::new();
    DISCOVERED.get_or_init(|| {
        let mut seen: HashSet<&'static str> = HashSet::new();
        let mut protocols = Vec::new();
        for protocol in EMBEDDED_DATA_PROTOCOLS.iter().copied() {
            let type_name = protocol.type_name();
            assert!(
                seen.insert(type_name),
                "two embedded data converters are registered for type `{type_name}`"
            );
            protocols.push(protocol);
        }
        protocols
    })
}
