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

//! # Legend Pure — Mappings
//!
//! The engine core's `###Mapping` section: mappings, their includes, their
//! elements and their tests.
//!
//! ```pure
//! ###Mapping
//! Mapping my::PersonMapping
//! (
//!   include my::BaseMapping
//!   *my::Person: Pure
//!   {
//!     ~src my::_Person
//!     name: $src.fullName
//!   }
//! )
//! ```
//!
//! Mirrors the engine's `MappingParserGrammar` / `MappingParseTreeWalker`
//! and its `Mapping` protocol. A mapping element's body is read by the
//! grammar its type names (`Pure`, `EnumerationMapping`, a store's
//! `Relational`, …), and a legacy test's input by the grammar its type
//! names (`Object`, a store's `Relational`): see [`grammar`]. An element or
//! input of a type no linked grammar reads is unsupported, so that the
//! engine parses the section. Test assertions and embedded data are read by
//! the parser's grammars for them, as anywhere else.
//!
//! An include names a mapping (`include my::M`, `include mapping my::M`)
//! or, as the engine's data space extension reads it, a data space
//! (`include dataspace my::DataSpace`; [`ast::IncludeKind`]); an include of
//! any other type is unsupported.
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::Mapping`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement).
//! - [`section`] — [`section::MappingSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `Mapping`.
//! - [`protocol`] — [`protocol::MappingProtocol`] registers as
//!   [`legend_pure_parser_protocol::ElementProtocol`] for mappings.
//! - [`grammar`] — the plug-in grammars for mapping element bodies and
//!   legacy test inputs.
//! - [`enumeration`] — the engine core's `EnumerationMapping` elements.
//! - [`pure_instance`] — the engine core's `Pure` (model-to-model) class
//!   mappings.
//! - [`relation_function`] — the engine core's `Relation` class mappings.
//! - [`xstore`] and [`model_join`] — the engine core's `XStore` and
//!   `ModelJoin` association mappings.
//! - [`operation`] — the engine core's `Operation` class mappings.
//! - [`aggregation_aware`] — the engine core's `AggregationAware` class
//!   mappings.
//! - [`object_input`] — the engine core's `Object` test input.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod aggregation_aware;
pub mod ast;
pub mod enumeration;
pub mod grammar;
pub mod model_join;
pub mod object_input;
pub mod operation;
pub mod protocol;
pub mod pure_instance;
pub mod relation_function;
pub mod section;
pub mod xstore;
