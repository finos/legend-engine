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

//! Graph-fetch AST.
//!
//! Three node types implementing [`IslandContent`]:
//!
//! - [`RootGraphFetchTree`] — top-level `#{ class { … } }#` envelope.
//! - [`PropertyGraphFetchTree`] — one selected field within a tree
//!   (`firstName`, `'alias' : firstName`, `addr { city }`,
//!   `employeesByName(['Peter'])`, `employees->subType(@Manager)`).
//! - [`SubTypeGraphFetchTree`] — `->subType(@Type){…}` among the root's
//!   paths.
//!
//! Mirrors the graph grammar (`GraphFetchTreeParserGrammar`) and Java's
//! `RootGraphFetchTree` / `PropertyGraphFetchTree` /
//! `SubTypeGraphFetchTree` protocol shapes.

use std::any::Any;

use legend_pure_parser_ast::annotation::PackageableElementPtr;
use legend_pure_parser_ast::expression::Literal;
use legend_pure_parser_ast::island::IslandContent;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Identifier;
use smol_str::SmolStr;

/// The island tag this DSL claims (`""` — empty string).
pub const TAG: &str = "";

/// Root of a graph fetch tree: `#{ Type { field1, field2 } }#`.
///
/// # Example
///
/// ```text
/// #{
///     my::Person {
///         firstName,
///         lastName,
///         address {
///             city
///         }
///     }
/// }#
/// ```
#[derive(Debug, Clone, PartialEq)]
pub struct RootGraphFetchTree {
    /// The root class, positioned on its path.
    pub class: PackageableElementPtr,
    /// Property sub-trees: regular field selections.
    pub sub_trees: Vec<PropertyGraphFetchTree>,
    /// Subtype sub-trees: `->subType(@Type){...}` casts.
    pub sub_type_trees: Vec<SubTypeGraphFetchTree>,
    /// The whole tree, class to closing `}`.
    pub source_info: SourceInfo,
}

impl IslandContent for RootGraphFetchTree {
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

/// A property field within a graph fetch tree.
///
/// Represents: `property`, `property(args)`, `'alias':property`,
/// `property->subType(@Type)` or `property{subFields}`, and combinations
/// thereof.
#[derive(Debug, Clone, PartialEq)]
pub struct PropertyGraphFetchTree {
    /// The property name.
    pub property: Identifier,
    /// Where the property name is.
    pub property_source_info: SourceInfo,
    /// Qualified property parameters (e.g., `['Peter']`).
    pub parameters: Vec<GraphFetchParameter>,
    /// Optional alias: `'aliasName' : property`.
    pub alias: Option<SmolStr>,
    /// Optional subtype cast on the property: `->subType(@Type)`.
    pub sub_type: Option<PackageableElementPtr>,
    /// Property sub-trees (nested field selections).
    pub sub_trees: Vec<PropertyGraphFetchTree>,
    /// The whole path, alias to closing `}`.
    pub source_info: SourceInfo,
}

/// A subtype cast among the root's paths: `->subType(@Type){fields}`.
#[derive(Debug, Clone, PartialEq)]
pub struct SubTypeGraphFetchTree {
    /// The subtype class, positioned on its path.
    pub sub_type_class: PackageableElementPtr,
    /// Property sub-trees within the subtype.
    pub sub_trees: Vec<PropertyGraphFetchTree>,
    /// The whole path, `->` to closing `}`.
    pub source_info: SourceInfo,
}

/// A property parameter: the graph grammar's `parameter`.
#[derive(Debug, Clone, PartialEq)]
pub enum GraphFetchParameter {
    /// A single value.
    Scalar(ScalarParameter),
    /// `[value, …]`.
    Collection {
        /// The values.
        values: Vec<ScalarParameter>,
        /// From `[` to `]`.
        source_info: SourceInfo,
    },
}

/// The graph grammar's `scalarParameter`.
#[derive(Debug, Clone, PartialEq)]
pub enum ScalarParameter {
    /// A literal, `%latest` included.
    Literal(Literal),
    /// `$name`.
    Variable {
        /// The name, without `$`.
        name: Identifier,
        /// Where the name is (without the `$`, as the engine positions it).
        source_info: SourceInfo,
    },
    /// `my::Enum.VALUE`.
    EnumValue {
        /// The enumeration.
        enumeration: PackageableElementPtr,
        /// The value name.
        value: Identifier,
        /// The whole reference.
        source_info: SourceInfo,
    },
}
