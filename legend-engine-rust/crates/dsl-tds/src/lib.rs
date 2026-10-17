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

//! # Legend Pure — TDS DSL
//!
//! Self-contained crate hosting the TDS literal island grammar. Tag:
//! `"TDS"`.
//!
//! ```pure
//! #TDS{
//!   id, name:String[1]
//!   1, Alice
//!   2, Bob
//! }#
//! ```
//!
//! The body is CSV, not Pure, so the grammar reads it as written
//! ([`legend_pure_parser_parser::IslandParser::raw_body`]). Like the
//! engine's TDS island (`TDSRelationAccessorGrammarExtension`, whose
//! `TDSContainer` keeps the text as `tdsString`), it doesn't parse the
//! columns and rows: the compiler reads them.
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::TdsLiteral`] implementing
//!   [`legend_pure_parser_ast::island::IslandContent`].
//! - [`parser`] — [`parser::TdsIslandParser`] registers as
//!   [`legend_pure_parser_parser::IslandParser`] with tag `"TDS"`.
//! - [`protocol`] — [`protocol::TdsIslandProtocol`] registers as
//!   [`legend_pure_parser_protocol::IslandProtocol`] with tag `"TDS"`.
//!
//! Core crates carry no TDS knowledge — this crate is the sole owner.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
pub mod parser;
pub mod protocol;
