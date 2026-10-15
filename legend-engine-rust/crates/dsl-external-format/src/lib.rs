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

//! # Legend Pure — External formats
//!
//! The engine's `###ExternalFormat` section (its external format core):
//! schema sets, schemas in a format, and bindings of a model to them.
//!
//! ```pure
//! ###ExternalFormat
//! import my::*;
//!
//! SchemaSet my::PeopleSchemas
//! {
//!   format: FlatData;
//!   schemas: [ { id: people; content: 'section A: DelimitedWithHeadings { … }'; } ];
//! }
//!
//! Binding my::PeopleBinding
//! {
//!   schemaSet: my::PeopleSchemas;
//!   contentType: 'application/x.flatdata';
//!   modelIncludes: [ my::Person ];
//! }
//! ```
//!
//! Mirrors the engine's `ExternalFormatParserGrammar` and
//! `ExternalFormatParseTreeWalker`. The section is read whole first, as
//! blocks of fields ([`legend_pure_parser_parser::blocks`]), then walked
//! into protocol records ([`legend_pure_parser_protocol::v1::record`]): its
//! schema sets first, then its bindings, as the walker walks them. A schema
//! set's format must be one the engine's modules define; another is
//! unsupported, for the engine to check against the formats on its
//! classpath.
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::ExternalFormatElement`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement).
//! - [`section`] — [`section::ExternalFormatSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `ExternalFormat`, and
//!   [`section::ExternalFormatProtocol`] as
//!   [`legend_pure_parser_protocol::ElementProtocol`] for its elements.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
mod read;
pub mod section;
mod walk;
