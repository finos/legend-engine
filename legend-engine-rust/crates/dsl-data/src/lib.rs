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

//! # Legend Pure — Data elements
//!
//! The engine core's `###Data` section: data elements, each holding embedded
//! data and data for other elements.
//!
//! ```pure
//! ###Data
//! Data my::PersonData
//! {
//!   ExternalFormat
//!   #{
//!     contentType: 'application/json';
//!     data: '{"name": "John"}';
//!   }#
//! }
//! ```
//!
//! Mirrors the engine's `DataParserGrammar` / `DataParseTreeWalker` and its
//! `DataElement` protocol. The embedded data itself is read by the parser's
//! data grammars (`ParserContext::parse_embedded_data`) and converted by
//! the protocol's `convert_embedded_data`, as anywhere else.
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::DataElement`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement).
//! - [`parser`] — [`parser::DataSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `Data`, a section
//!   without imports.
//! - [`protocol`] — [`protocol::DataElementProtocol`] registers as
//!   [`legend_pure_parser_protocol::ElementProtocol`] for data elements.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
pub mod parser;
pub mod protocol;
