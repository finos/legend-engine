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

//! # Legend Pure — Data spaces
//!
//! The engine's `###DataSpace` section (its data space extension): data
//! spaces, which name the models, executions and support of a domain.
//!
//! ```pure
//! ###DataSpace
//! DataSpace my::People
//! {
//!   executionContexts:
//!   [
//!     { name: 'Default'; mapping: my::Mapping; defaultRuntime: my::Runtime; }
//!   ];
//!   defaultExecutionContext: 'Default';
//!   title: 'People';
//! }
//! ```
//!
//! Mirrors the engine's `DataSpaceParserGrammar` and
//! `DataSpaceParseTreeWalker`. The section is read whole first, as blocks
//! of fields ([`legend_pure_parser_parser::blocks`]), then each data space
//! walked into a protocol record
//! ([`legend_pure_parser_protocol::v1::record`]). An executable with
//! neither a query nor an executable, which the engine fails on without a
//! position, is unsupported.
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::DataSpace`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement).
//! - [`section`] — [`section::DataSpaceSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `DataSpace`.
//! - [`data`] — [`data::DataSpaceTestDataParser`] registers as
//!   [`legend_pure_parser_parser::EmbeddedDataParser`] for
//!   `DataspaceTestData`, a reference to a data space.
//! - [`protocol`] — [`protocol::DataSpaceProtocol`] registers as
//!   [`legend_pure_parser_protocol::ElementProtocol`] for data spaces.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
pub mod data;
pub mod protocol;
mod read;
pub mod section;
mod walk;
