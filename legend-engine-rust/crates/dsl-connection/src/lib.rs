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

//! # Legend Pure — Connections
//!
//! The engine core's `###Connection` section: packageable connections.
//!
//! ```pure
//! ###Connection
//! JsonModelConnection my::PeopleConnection
//! {
//!   class: my::Person;
//!   url: 'data:application/json,[]';
//! }
//! ```
//!
//! Mirrors the engine's `ConnectionParserGrammar` /
//! `ConnectionParseTreeWalker`. A connection's value is read by the grammar
//! its type names (the engine's `ConnectionValueParser`s: the core's model
//! connections, a store's own): see [`grammar`]. A value of a type no linked
//! grammar reads is unsupported, so that the engine parses the section. A
//! runtime's own connections, `#{ Type { … } }#`, are read by the same
//! grammars: [`section::parse_embedded_connection`].
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::PackageableConnection`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement), and
//!   [`ast::ConnectionValue`].
//! - [`section`] — [`section::ConnectionSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `Connection`.
//! - [`protocol`] — [`protocol::ConnectionProtocol`] registers as
//!   [`legend_pure_parser_protocol::ElementProtocol`] for connections.
//! - [`grammar`] — the plug-in grammars for connection values.
//! - [`model`] — the engine core's model connections.
//! - [`uri`] — what `java.net.URI` makes of a URL, for connections' walkers.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
pub mod grammar;
pub mod model;
pub mod protocol;
pub mod section;
pub mod uri;
