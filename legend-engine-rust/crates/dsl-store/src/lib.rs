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

//! # Legend Pure — Store DSL
//!
//! Self-contained crate hosting the relation-store-accessor inline-island
//! grammar (`#>{ qualified::Store.tableOrColumn }#`). Tag: `">"`.
//!
//! Mirrors the Java
//! `org.finos.legend.pure.m2.inlinedsl.store.RelationStoreAccessor` DSL
//! and its `meta::pure::store::RelationStoreAccessor<T>` metaclass:
//!
//! ```pure
//! Class meta::pure::store::RelationStoreAccessor<T>
//!     extends meta::pure::metamodel::relation::RelationElementAccessor<T>
//! {
//!    store : Store[1];
//!    path  : String[*];
//! }
//! ```
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::RelationStoreAccessorRef`] implementing
//!   [`legend_pure_parser_ast::island::IslandContent`].
//! - [`parser`] — [`parser::RelationStoreAccessorParser`] registers as
//!   [`legend_pure_parser_parser::IslandParser`] with tag `">"`.
//! - [`protocol`] — [`protocol::RelationStoreAccessorProtocol`]
//!   registers as
//!   [`legend_pure_parser_protocol::IslandProtocol`] with tag `">"`.
//!
//! Core crates carry no store knowledge — this crate is the sole owner.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
pub mod parser;
pub mod protocol;
