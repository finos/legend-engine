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

//! # Legend Pure — Diagrams
//!
//! The engine's `###Diagram` section (its diagram extension): diagrams of
//! classes, their properties and generalizations, laid out.
//!
//! ```pure
//! ###Diagram
//! Diagram my::People
//! {
//!   classView 4cec85f9-9b66-450a-bdcb-c855aa0314e1
//!   {
//!     class: my::Person;
//!     position: (100.0, 50.0);
//!     rectangle: (120.0, 40.0);
//!   }
//!   generalizationView
//!   {
//!     source: 4cec85f9-9b66-450a-bdcb-c855aa0314e1;
//!     target: legalEntity;
//!     points: [(160.0, 70.0), (160.0, 10.0)];
//!   }
//! }
//! ```
//!
//! Mirrors the engine's `DiagramParserGrammar` and
//! `DiagramParseTreeWalker`. The section is read whole first, as blocks of
//! fields ([`legend_pure_parser_parser::blocks`]), then walked into protocol
//! records ([`legend_pure_parser_protocol::v1::record`]). The engine's
//! lexer reads a view's id, `[A-Za-z0-9_-]+`, as one token, and a number's
//! sign with it: the reader rebuilds them from the Pure tokens written
//! together (its `tokens` module).
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::Diagram`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement).
//! - [`section`] — [`section::DiagramSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `Diagram`, and
//!   [`section::DiagramProtocol`] as
//!   [`legend_pure_parser_protocol::ElementProtocol`] for diagrams.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
mod read;
pub mod section;
mod tokens;
mod walk;
