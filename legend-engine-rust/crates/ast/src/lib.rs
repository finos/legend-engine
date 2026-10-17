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

//! # Legend Pure Parser — AST
//!
//! Core data model for the Pure grammar parser. This crate defines all AST types
//! used to represent parsed Pure source code. It has zero serialization dependencies
//! and is designed for direct consumption by both the protocol crate (for Protocol JSON output)
//! and a future Rust-based compiler.
//!
//! ## Design Principles
//!
//! - **Everything is `Spanned`** — every AST node carries a [`SourceInfo`] for precise error
//!   reporting and IDE integration. No node exists without a known source position.
//! - **No `serde`** — AST is a pure data model, serialization lives in the protocol crate
//! - **Immutable** — All types are constructed once during parsing, never mutated
//! - **Type-safe** — Rust enums with data, not stringly-typed maps
//! - **Compiler-ready** — AST ≠ Protocol JSON; designed for parser efficiency and future compiler use

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod annotation;
pub mod data;
pub mod dsl;
pub mod element;
pub mod expression;
pub mod island;
pub mod section;
pub mod source_info;
pub mod test_assertion;
pub mod type_ref;

#[cfg(test)]
pub(crate) mod test_utils;

pub use legend_pure_parser_ast_derive::{Annotated, PackageableElement, Spanned};
pub use section::{ImportStatement, Section, SourceFile};
pub use source_info::{SourceInfo, SourceOffset};
pub use type_ref::{
    HasMultiplicity, Identifier, Multiplicity, MultiplicityArgument, Package, TypeReference,
    TypeSpec, UnitReference,
};
