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

//! The `###Deephaven` section: Deephaven stores, their tables and their
//! columns, and Deephaven apps, functions activated as Deephaven
//! applications (`DeephavenParserGrammar` and `DeephavenParseTreeWalker`).
//!
//! ```pure
//! ###Deephaven
//! Deephaven my::Trades
//! (
//!   Table trades ( id: INT, price: DECIMAL(10, 2) )
//! )
//!
//! DeephavenApp my::TradesApp
//! {
//!   applicationName: 'trades';
//!   function: my::trades():Any[*];
//!   ownership: Deployment { identifier: 'owner' };
//! }
//! ```
//!
//! The section is read whole first, then each element walked into a
//! protocol record ([`legend_pure_parser_protocol::v1::record`]): the
//! stores, then the apps, as the walker walks them.

use std::any::Any;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use legend_pure_parser_protocol::v1::record::Record;

mod read;
pub mod section;
mod walk;

/// The section kind, `###Deephaven`, which is also its elements' kind.
pub const SECTION_KIND: &str = "Deephaven";

/// A store, `Deephaven my::S ( … )`, or an app, `DeephavenApp my::A { … }`.
#[derive(Debug, Clone, PartialEq)]
pub struct DeephavenElement {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// The element's object, without its package and name.
    pub record: Record,
    /// From its keyword to its end.
    pub source_info: SourceInfo,
}

impl Spanned for DeephavenElement {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

/// An element has no annotations.
impl Annotated for DeephavenElement {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl PackageableElement for DeephavenElement {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for DeephavenElement {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn DSLElement> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn DSLElement) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}
