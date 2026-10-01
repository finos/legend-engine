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

//! # Legend Pure — Data quality
//!
//! The engine's `###DataQualityValidation` section (its data quality
//! extension): validations of a class's instances, of a relation's rows,
//! and comparisons of two relations.
//!
//! ```pure
//! ###DataQualityValidation
//! DataQualityValidation my::PersonValidation
//! {
//!   context: fromDataSpace(my::People, 'Default');
//!   validationTree: $[ my::Person<mustBeAdult>{ name } ]$;
//!   filter: p: my::Person[1]|$p.age > 0;
//! }
//!
//! DataQualityRelationValidation my::RowsValidation
//! {
//!   query: |#>{my::Store.people}#->select(~[name]);
//!   validations:
//!   [
//!     { name: 'notEmpty'; assertion: rel|$rel->assertRelationNotEmpty(); }
//!   ];
//! }
//!
//! DataQualityRelationComparison my::Reconciliation
//! {
//!   source: |#>{my::Store.people}#->select(~[name]);
//!   target: |#>{my::Store.staff}#->select(~[name]);
//!   keys: [name];
//!   strategy: MD5Hash;
//! }
//! ```
//!
//! Mirrors the engine's `DataQualityParserGrammar` and
//! `DataQualityTreeWalker`. The section is read whole first, as blocks of
//! fields ([`legend_pure_parser_parser::blocks`]), then each element walked
//! into a protocol record ([`legend_pure_parser_protocol::v1::record`]). A
//! persistence strategy, `# Type { … }#`, is unsupported: the engine reads
//! one with an extension, and none is linked.
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::DataQualityElement`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement).
//! - [`section`] — [`section::DataQualitySectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for
//!   `DataQualityValidation`.
//! - [`protocol`] — [`protocol::DataQualityProtocol`] registers as
//!   [`legend_pure_parser_protocol::ElementProtocol`] for its elements.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
pub mod protocol;
mod read;
pub mod section;
mod walk;
