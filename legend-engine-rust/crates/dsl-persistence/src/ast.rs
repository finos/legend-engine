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

//! The persistence AST: the `###Persistence` section's elements, each a
//! [`Record`] in the shape of the engine's protocol object.

use std::any::Any;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};

use legend_pure_parser_protocol::v1::record::Record;

/// The section kind, `###Persistence`, which is also its elements' kind.
pub const SECTION_KIND: &str = "Persistence";

/// `Persistence my::P { … }` or `PersistenceContext my::C { … }`.
#[derive(Debug, Clone, PartialEq)]
pub struct PersistenceElement {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned on the element's path.
    pub name: SpannedString,
    /// The element's object, without its package and name.
    pub record: Record,
    /// From `Persistence` or `PersistenceContext` to `}`.
    pub source_info: SourceInfo,
}

impl Spanned for PersistenceElement {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for PersistenceElement {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl PackageableElement for PersistenceElement {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for PersistenceElement {
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
