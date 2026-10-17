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

//! # Legend Pure — Persistence
//!
//! The engine's `###Persistence` section (its persistence extension):
//! persistences, which run a service and persist its result, and their
//! contexts.
//!
//! ```pure
//! ###Persistence
//! Persistence my::PeoplePersistence
//! {
//!   doc: 'Persists the people';
//!   trigger: Manual;
//!   service: my::PeopleService;
//!   serviceOutputTargets:
//!   [
//!     TDS { keys: [ id ] datasetType: Delta { actionIndicator: None; } }
//!     ->
//!     Relational #{ table: people; database: my::Db; }#
//!   ];
//! }
//! ```
//!
//! Mirrors the engine's `PersistenceParserGrammar`,
//! `PersistenceParseTreeWalker` and `PersistenceContextParseTreeWalker`.
//! A trigger, a context's platform and a service output's target are each
//! read by the grammar their type names: see [`grammar`]. The engine core's
//! `Manual` trigger and `Default` platform are built in; a type no linked
//! grammar reads is unsupported, so that the engine parses the section.
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::PersistenceElement`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement).
//! - [`section`] — [`section::PersistenceSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `Persistence`.
//! - [`protocol`] — [`protocol::PersistenceProtocol`] registers as
//!   [`legend_pure_parser_protocol::ElementProtocol`] for the elements.
//! - [`grammar`] — the plug-in grammars of triggers, platforms and targets.
//! - [`read`] — the section's grammar, as blocks of fields.
//!
//! The walker builds its objects as protocol records
//! ([`legend_pure_parser_protocol::v1::record`]).

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
pub mod grammar;
pub mod protocol;
pub mod read;
pub mod section;
mod walk;
