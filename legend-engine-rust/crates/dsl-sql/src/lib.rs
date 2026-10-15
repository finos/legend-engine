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

//! # Legend Pure — SQL expression island
//!
//! Self-contained crate hosting the SQL expression island grammar. Tag:
//! `"SQL"`.
//!
//! ```pure
//! #SQL{select a from csv('a,b
//! 1,2')}#->filter(x|$x.a == 1)
//! ```
//!
//! The body is SQL, not Pure, so the grammar reads it as written
//! ([`legend_pure_parser_parser::IslandParser::raw_body`]). Like the
//! engine's SQL island (`SQLExpressionGrammarParserExtension`, whose
//! `SQLExpressionProtocol` keeps the text as `sql`), it doesn't parse the
//! SQL: the compiler does.
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::SqlExpression`] implementing
//!   [`legend_pure_parser_ast::island::IslandContent`].
//! - [`parser`] — [`parser::SqlIslandParser`] registers as
//!   [`legend_pure_parser_parser::IslandParser`] with tag `"SQL"`.
//! - [`protocol`] — [`protocol::SqlIslandProtocol`] registers as
//!   [`legend_pure_parser_protocol::IslandProtocol`] with tag `"SQL"`.
//!
//! Core crates carry no SQL knowledge — this crate is the sole owner.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
pub mod parser;
pub mod protocol;
