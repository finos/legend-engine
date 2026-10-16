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

//! # Legend Pure — BigQuery function activators
//!
//! The engine's `###BigQuery` section (its `bigqueryFunction` extension):
//! functions activated as BigQuery functions.
//!
//! ```pure
//! ###BigQuery
//! BigQueryFunction my::PeopleFunction
//! {
//!   functionName: 'people';
//!   function: my::people():TabularDataSet[1];
//!   ownership: Deployment { identifier: 'owner' };
//!   activationConfiguration: my::BigQueryConnection;
//! }
//! ```
//!
//! Mirrors the engine's `BigQueryFunctionParserGrammar` and
//! `BigQueryFunctionTreeWalker`. The section is read whole first, then
//! walked into protocol records
//! ([`legend_pure_parser_protocol::v1::record`]), its functions read,
//! walked and converted as `dsl-function-activator` reads, walks and
//! converts function activators. The walker makes a deployment
//! configuration an element without a name, which Rust reports
//! unsupported, after the functions' errors.
//!
//! Crate layout:
//!
//! - [`section`] — [`section::BigQuerySectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `BigQuery`, and an
//!   `ActivatorProtocol` as [`legend_pure_parser_protocol::ElementProtocol`]
//!   for its elements.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

mod read;
pub mod section;
mod walk;

/// The section kind, `###BigQuery`, which is also its elements' kind.
pub const SECTION_KIND: &str = "BigQuery";
