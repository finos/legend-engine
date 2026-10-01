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

//! The data quality AST: the `###DataQualityValidation` section's elements,
//! each with a [`Record`] in the shape of the engine's protocol object.

use std::any::Any;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use legend_pure_parser_protocol::v1::record::Record;

/// The section kind, `###DataQualityValidation`, which is also its
/// elements' kind.
pub const SECTION_KIND: &str = "DataQualityValidation";

/// What a data quality element validates.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DataQualityKind {
    /// `DataQualityValidation`: a class's instances, by its constraints.
    Validation,
    /// `DataQualityRelationValidation`: a relation's rows, by assertions.
    RelationValidation,
    /// `DataQualityRelationComparison`: two relations, row by row.
    RelationComparison,
}

impl DataQualityKind {
    /// Whether the element has annotations: a comparison has neither
    /// documentation nor stereotypes nor tagged values, in its grammar or
    /// in its protocol class.
    #[must_use]
    pub fn annotated(self) -> bool {
        self != Self::RelationComparison
    }
}

/// A data quality element, `DataQualityValidation my::V { … }` and the
/// like.
#[derive(Debug, Clone, PartialEq)]
pub struct DataQualityElement {
    /// What it validates.
    pub kind: DataQualityKind,
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// `<<profile.stereotype>>`.
    pub stereotypes: Vec<StereotypePtr>,
    /// `{profile.tag = 'value'}`, a documentation literal's first.
    pub tagged_values: Vec<TaggedValue>,
    /// The element's object, without its package, name and annotations.
    pub record: Record,
    /// From the documentation, or the keyword, to `}`.
    pub source_info: SourceInfo,
}

impl Spanned for DataQualityElement {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for DataQualityElement {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &self.stereotypes
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &self.tagged_values
    }
}

impl PackageableElement for DataQualityElement {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for DataQualityElement {
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
