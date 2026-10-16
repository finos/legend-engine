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

//! Island grammar expressions — `#tag{ content }#`.
//!
//! Island grammars are embedded mini-languages within Pure expressions.
//! Each island is identified by a tag between `#` and `{`:
//!
//! | Syntax      | Tag   | Content Type |
//! |-------------|-------|-------------|
//! | `#{ }#`     | `""`  | Graph fetch tree |
//! | `#>{ }#`    | `">"` | Path expression (future) |
//! | `#sql{ }#`  | `"sql"`| Embedded SQL (future) |
//!
//! ## Plugin Architecture
//!
//! The island system is trait-based to support extensibility:
//!
//! - **[`IslandContent`]** (this module): trait for AST storage. Each island
//!   grammar implements this to provide its content type. The AST stores
//!   `Box<dyn IslandContent>` so new islands can be added from external crates.
//! - **`IslandParser`** (parser crate): trait for parsing `#tag{ content }#`.
//! - **`IslandComposer`** (compose crate): trait for rendering content back to text.
//!
//! Graph fetch is the first built-in island grammar. Future islands (path,
//! SQL, etc.) can implement the same traits from separate crates.

use std::any::Any;
use std::fmt;

use crate::source_info::SourceInfo;

/// Trait for island grammar content stored in the AST.
///
/// Every island grammar (graph fetch, path, SQL, etc.) implements this
/// trait to provide its content type. The AST stores `Box<dyn IslandContent>`
/// so new islands can be added from external crates without modifying the
/// AST `Expression` enum.
///
/// # Implementing a New Island Grammar
///
/// ```rust,ignore
/// #[derive(Debug, Clone, PartialEq)]
/// pub struct MyIslandContent { /* ... */ }
///
/// impl IslandContent for MyIslandContent {
///     fn tag(&self) -> &str { "myTag" }
///     fn as_any(&self) -> &dyn Any { self }
///     fn clone_box(&self) -> Box<dyn IslandContent> { Box::new(self.clone()) }
///     fn eq_content(&self, other: &dyn IslandContent) -> bool {
///         other.as_any().downcast_ref::<Self>().map_or(false, |o| self == o)
///     }
/// }
/// ```
pub trait IslandContent: fmt::Debug + Send + Sync {
    /// The island tag: `""` for `#{}#`, `">"` for `#>{}#`, etc.
    fn tag(&self) -> &str;

    /// Downcast to the concrete type for type-specific handling.
    fn as_any(&self) -> &dyn Any;

    /// Clone into a new boxed trait object.
    fn clone_box(&self) -> Box<dyn IslandContent>;

    /// Equality comparison with another island content.
    ///
    /// Implementations should downcast `other` to `Self` and compare.
    fn eq_content(&self, other: &dyn IslandContent) -> bool;
}

/// An island grammar expression: `#tag{ content }#`.
///
/// The `content` field holds a type-erased `Box<dyn IslandContent>`.
/// Consumers access the concrete type via:
///
/// ```rust,ignore
/// if let Some(tree) = island.content.as_any().downcast_ref::<RootGraphFetchTree>() {
///     // handle graph fetch
/// }
/// ```
pub struct IslandExpression {
    /// The parsed content of the island grammar (type-erased).
    pub content: Box<dyn IslandContent>,
    /// Source location of the entire `#tag{ ... }#` expression.
    pub source_info: SourceInfo,
}

impl IslandExpression {
    /// Returns the island tag (delegated to the content trait).
    #[must_use]
    pub fn tag(&self) -> &str {
        self.content.tag()
    }
}

impl Clone for IslandExpression {
    fn clone(&self) -> Self {
        Self {
            content: self.content.clone_box(),
            source_info: self.source_info.clone(),
        }
    }
}

impl PartialEq for IslandExpression {
    fn eq(&self, other: &Self) -> bool {
        self.content.eq_content(other.content.as_ref())
    }
}

impl fmt::Debug for IslandExpression {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("IslandExpression")
            .field("tag", &self.content.tag())
            .field("content", &self.content)
            .field("source_info", &self.source_info)
            .finish()
    }
}

// Graph-fetch concrete types (`RootGraphFetchTree`,
// `PropertyGraphFetchTree`, `SubTypeGraphFetchTree`) live in the
// `legend-pure-dsl-graph` crate's `ast` module. Core ast keeps only
// the `IslandContent` trait surface so DSL crates can plug in.
