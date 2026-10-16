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

//! # Legend Pure — Runtimes
//!
//! The engine core's `###Runtime` section: packageable runtimes, the
//! mappings they run and the connections to their stores.
//!
//! ```pure
//! ###Runtime
//! Runtime my::Runtime
//! {
//!   mappings: [ my::Mapping ];
//!   connections:
//!   [
//!     my::Db: [ connection: my::DbConnection ],
//!     ModelStore: [ json: #{ JsonModelConnection { class: my::Person; url: 'data:application/json,[]'; } }# ]
//!   ];
//! }
//! ```
//!
//! Mirrors the engine's `RuntimeParserGrammar` / `RuntimeParseTreeWalker`.
//! A runtime's own connections are read by `dsl-connection`'s grammars. An
//! element's own runtime, `#{ mappings: [ … ]; connections: [ … ]; }#` (a
//! service's), is read by [`section::parse_embedded_runtime`].
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::PackageableRuntime`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement).
//! - [`section`] — [`section::RuntimeSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `Runtime`.
//! - [`protocol`] — [`protocol::RuntimeProtocol`] registers as
//!   [`legend_pure_parser_protocol::ElementProtocol`] for runtimes.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
pub mod protocol;
pub mod section;
