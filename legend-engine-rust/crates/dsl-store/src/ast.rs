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

//! Store-DSL AST.
//!
//! A single node type, [`RelationStoreAccessorRef`], implementing
//! [`IslandContent`]. It captures the syntactic shape only — the
//! `store` reference is resolved later by the dsl-store
//! `CompilerExtension`.

use std::any::Any;

use legend_pure_parser_ast::island::IslandContent;
use legend_pure_parser_ast::source_info::SourceInfo;
use smol_str::SmolStr;

/// The island tag this DSL claims.
pub const TAG: &str = ">";

/// `#>{ qualified::Store.tableOrColumn }#`.
///
/// `path` mirrors the Java parser's `_path(parts)`: the body string is
/// split on `.` so the first segment is the qualified store path
/// (`my::mainDb`) and the remainder is the table / column / nested-set
/// trail (`PersonTable`, then optionally `column`).
///
/// The compiler-extension layer resolves `path[0]` against
/// `meta::pure::store::Store` subclasses to set the `store` reference
/// on the generated metaclass instance.
///
/// # Example
///
/// `#>{ my::mainDb.PersonTable }#` produces:
///
/// ```text
/// path = ["my::mainDb", "PersonTable"]
/// ```
#[derive(Debug, Clone, PartialEq)]
pub struct RelationStoreAccessorRef {
    /// Dotted path. `path[0]` is the qualified store path
    /// (`pkg::sub::Store`); the remainder is the relation/column trail.
    pub path: Vec<SmolStr>,
    /// Source location of the entire `#>{…}#` island.
    pub source_info: SourceInfo,
}

impl IslandContent for RelationStoreAccessorRef {
    #[allow(clippy::unnecessary_literal_bound)]
    fn tag(&self) -> &str {
        TAG
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn IslandContent> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn IslandContent) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|o| self == o)
    }
}
