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

//! The data element AST.

use std::any::Any;

use legend_pure_parser_ast::annotation::{
    PackageableElementPtr, SpannedString, StereotypePtr, TaggedValue,
};
use legend_pure_parser_ast::data::EmbeddedDataValue;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};

/// The section kind, `###Data`, which is also the elements' kind.
pub const KIND: &str = "Data";

/// A data element: `Data my::MyData { ExternalFormat #{ … }# }`.
#[derive(Debug, Clone, PartialEq)]
pub struct DataElement {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values, a documentation literal's first.
    pub tagged_values: Vec<TaggedValue>,
    /// The element's data, if it has some.
    pub data: Option<EmbeddedDataValue>,
    /// Data for other elements, in source order.
    pub resolvers: Vec<DataResolver>,
    /// From the documentation, or `Data`, to `}`.
    pub source_info: SourceInfo,
}

/// Data a data element gives another element.
#[derive(Debug, Clone, PartialEq)]
pub enum DataResolver {
    /// `my::Store: Relation #{ … }#;`: the element's data, the engine's
    /// `BaseDataResolver`.
    Base {
        /// The element.
        element: PackageableElementPtr,
        /// Its data.
        data: EmbeddedDataValue,
        /// From the element to the end of its data.
        source_info: SourceInfo,
    },
    /// `my::OtherData;`: another data element's, the engine's
    /// `ReferenceDataResolver`, positioned on the path.
    Reference(PackageableElementPtr),
}

impl Spanned for DataElement {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for DataElement {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &self.stereotypes
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &self.tagged_values
    }
}

impl PackageableElement for DataElement {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for DataElement {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        KIND
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
