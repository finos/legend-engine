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

//! DSL extensibility primitive — section-level analog of [`island::IslandContent`](crate::island::IslandContent).
//!
//! Each M2 DSL (Mapping, Diagram, standalone Relational, …) lives in
//! its own crate. The DSL crate owns its AST, parser, composer, and
//! compiler-extension. Core knows nothing of any specific DSL — it
//! only carries [`Box<dyn DSLElement>`](DSLElement) inside the
//! [`Element::DSLElement`](crate::element::Element::DSLElement)
//! variant, the same way `IslandExpression` carries
//! `Box<dyn IslandContent>`.
//!
//! # Implementing a new DSL element
//!
//! ```rust,ignore
//! #[derive(Debug, Clone, PartialEq, legend_pure_parser_ast::PackageableElement)]
//! pub struct DiagramDef {
//!     pub package: Option<Package>,
//!     pub name: SpannedString,
//!     pub stereotypes: Vec<StereotypePtr>,
//!     pub tagged_values: Vec<TaggedValue>,
//!     pub source_info: SourceInfo,
//!     // … DSL-specific fields …
//! }
//!
//! impl DSLElement for DiagramDef {
//!     fn kind(&self) -> &str { "Diagram" }
//!     fn as_any(&self) -> &dyn std::any::Any { self }
//!     fn clone_box(&self) -> Box<dyn DSLElement> { Box::new(self.clone()) }
//!     fn eq_content(&self, other: &dyn DSLElement) -> bool {
//!         other.as_any().downcast_ref::<Self>().is_some_and(|o| self == o)
//!     }
//! }
//! ```
//!
//! Then wrap with `Element::DSLElement(Box::new(my_diagram))` so it
//! flows through the rest of the AST machinery.

use std::any::Any;

use crate::element::{Annotated, PackageableElement};
use crate::source_info::Spanned;

/// Type-erased AST element contributed by a DSL crate.
///
/// Carries the bookkeeping a DSL element needs to live alongside M3
/// elements in the AST without core knowing the concrete type.
/// Equivalent role to [`island::IslandContent`](crate::island::IslandContent),
/// scaled up to top-level packageable elements.
pub trait DSLElement: PackageableElement + std::fmt::Debug + Send + Sync {
    /// Section kind that owns this element, e.g. `"Diagram"` for the
    /// Diagram DSL. Matches the section header `###Diagram` and lets
    /// `Element` consumers route polymorphically without downcasting.
    fn kind(&self) -> &str;

    /// Downcast to the concrete type for DSL-specific handling.
    ///
    /// Mirrors the same pattern as
    /// [`island::IslandContent::as_any`](crate::island::IslandContent::as_any).
    fn as_any(&self) -> &dyn Any;

    /// Clone into a new boxed trait object.
    ///
    /// Required because `Element` implements [`Clone`] and the trait
    /// object can't auto-derive it.
    fn clone_box(&self) -> Box<dyn DSLElement>;

    /// Equality comparison with another DSL element.
    ///
    /// Implementations should downcast `other` to `Self` and compare;
    /// returning `false` when the other element is a different
    /// concrete type is the correct fallback.
    fn eq_content(&self, other: &dyn DSLElement) -> bool;
}

// ---------------------------------------------------------------------------
// Trait-object glue — Spanned / Annotated / PackageableElement on the trait
// itself so callers can use `&dyn DSLElement` polymorphically.
// ---------------------------------------------------------------------------
//
// Implementing those traits for `Box<dyn DSLElement>` turns it into a
// drop-in PackageableElement that the rest of the AST can dispatch
// through, identically to a concrete struct like `ClassDef`.

impl Spanned for Box<dyn DSLElement> {
    fn source_info(&self) -> &crate::source_info::SourceInfo {
        (**self).source_info()
    }
}

impl Annotated for Box<dyn DSLElement> {
    fn stereotypes(&self) -> &[crate::annotation::StereotypePtr] {
        (**self).stereotypes()
    }
    fn tagged_values(&self) -> &[crate::annotation::TaggedValue] {
        (**self).tagged_values()
    }
}

impl PackageableElement for Box<dyn DSLElement> {
    fn package(&self) -> Option<&crate::type_ref::Package> {
        (**self).package()
    }
    fn name(&self) -> &crate::type_ref::Identifier {
        (**self).name()
    }
}
