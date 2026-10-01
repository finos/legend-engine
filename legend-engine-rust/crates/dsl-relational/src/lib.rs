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

//! # Legend Pure — Relational store grammars
//!
//! The relational store's grammars, as the engine's relational store grammar
//! module reads them:
//!
//! - the `###Relational` section: databases, their schemas, tables, views,
//!   joins and filters (`RelationalParserGrammar`, `RelationalParseTreeWalker`
//!   and the `Database` protocol);
//! - `Relational #{ … }#` test data: one CSV text per table, written as Pure
//!   strings joined with `+` (`RelationalEmbeddedDataParser` and its
//!   `RelationalCSVData` protocol);
//! - `Relational` mapping elements, class and association mappings, and the
//!   `Relational` legacy mapping test input (the `mapping` rule of
//!   `RelationalParserGrammar`, and `RelationalGrammarParserExtension`'s
//!   mapping element and test input parsers).
//!
//! ```pure
//! ###Relational
//! Database store::TestDB
//! (
//!   Table PersonTable (id INTEGER PRIMARY KEY, firmId INTEGER, name VARCHAR(200))
//!   Table FirmTable (id INTEGER PRIMARY KEY, legalName VARCHAR(200))
//!   Join Firm_Person(FirmTable.id = PersonTable.firmId)
//! )
//! ```
//!
//! Crate layout:
//!
//! - [`connection`] —
//!   [`connection::RelationalConnectionGrammar`] registers as
//!   `dsl-connection`'s connection grammar for `RelationalDatabaseConnection`;
//!   its specifications' grammars are plug-ins of their own
//!   ([`connection::specification`]).
//! - [`ast`] — [`ast::Database`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement), and
//!   [`ast::RelationalData`], an
//!   [`EmbeddedDataContent`](legend_pure_parser_ast::data::EmbeddedDataContent).
//! - [`mapping`] — [`mapping::RelationalMappingGrammar`] and
//!   [`mapping::RelationalInputGrammar`] register as `dsl-mapping`'s mapping
//!   element and legacy test input grammars for `Relational`.
//! - [`section`] — [`section::RelationalSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `Relational`, a section
//!   without imports; [`operation`] and `milestoning` are its operation and
//!   milestoning grammars, [`tokens`] the relational lexer's tokens.
//! - [`database_protocol`] — [`database_protocol::DatabaseProtocol`]
//!   registers as [`legend_pure_parser_protocol::ElementProtocol`] for
//!   databases.
//! - [`parser`] — [`parser::RelationalDataParser`] registers as
//!   [`legend_pure_parser_parser::EmbeddedDataParser`] for `Relational`.
//! - [`protocol`] — [`protocol::RelationalDataProtocol`] registers as
//!   [`legend_pure_parser_protocol::EmbeddedDataProtocol`] for `Relational`.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
pub mod connection;
pub mod database_protocol;
pub mod mapping;
mod milestoning;
pub mod operation;
pub mod parser;
pub mod protocol;
pub mod section;
pub mod tokens;
