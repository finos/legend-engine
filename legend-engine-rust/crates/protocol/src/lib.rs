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

//! # Legend Pure Parser — Protocol
//!
//! Defines the Protocol v1 JSON model and provides bidirectional conversion
//! between the parser AST and the protocol model. The protocol model mirrors
//! the Java `org.finos.legend.engine.protocol.pure.m3` package; the JSON it
//! produces deserializes into the same Java objects the engine's ANTLR4
//! parser builds.
//!
//! Island grammars and section (DSL) grammars plug in their own converters
//! through [`ISLAND_PROTOCOLS`] and [`ELEMENT_PROTOCOLS`].
//!
//! Protocol JSON lives here and in grammar crates' own protocol modules; the
//! AST and parser carry no protocol knowledge.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod element_protocol;
pub mod embedded_data_protocol;
pub mod error;
pub mod island_protocol;
pub mod v1;

pub use element_protocol::{ELEMENT_PROTOCOLS, ElementProtocol};
pub use embedded_data_protocol::{EMBEDDED_DATA_PROTOCOLS, EmbeddedDataProtocol};
pub use error::ConvertError;
pub use island_protocol::{ISLAND_PROTOCOLS, IslandProtocol, dispatch_island_convert};
