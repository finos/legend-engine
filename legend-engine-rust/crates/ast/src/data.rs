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

//! Embedded data, `Type #{ … }#`: test data written in a grammar of its own,
//! the engine's `EmbeddedData`.
//!
//! The engine core's types (`ExternalFormat`, `ModelStore`, `Reference` and
//! `Relation`) are modeled here; other types come from plug-in grammars,
//! which store their content as a `Box<dyn EmbeddedDataContent>`, as island
//! grammars do with [`IslandContent`](crate::island::IslandContent).

use std::any::Any;
use std::fmt;

use smol_str::SmolStr;

use crate::annotation::PackageableElementPtr;
use crate::expression::Literal;
use crate::source_info::SourceInfo;
use crate::type_ref::Identifier;

/// Embedded data: `Relation #{ … }#`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct EmbeddedDataValue {
    /// The data type, e.g. `Relation`.
    pub type_name: Identifier,
    /// The data, as its type's grammar reads it.
    pub data: EmbeddedData,
    /// From the type to `}#`.
    pub source_info: SourceInfo,
}

/// The data of an [`EmbeddedDataValue`].
#[derive(Debug, Clone, PartialEq)]
pub enum EmbeddedData {
    /// The body as written: data no grammar reads where it stands (a test's
    /// expected value of a type other than `Relation`).
    Text(SmolStr),
    /// `ExternalFormat #{ contentType: '…'; data: '…'; }#`.
    ExternalFormat(ExternalFormatData),
    /// `ModelStore #{ my::Person: [^my::Person(name = 'x')] }#`.
    ModelStore(ModelStoreData),
    /// `Reference #{ my::Data }#`: the data of a data element.
    Reference(DataElementReference),
    /// `Relation #{ … }#`.
    Relation(RelationData),
    /// Data a linked embedded data grammar reads: `Relational #{ … }#`.
    Extension(Box<dyn EmbeddedDataContent>),
}

/// The content of embedded data a plug-in grammar reads.
///
/// ```rust,ignore
/// impl EmbeddedDataContent for MyData {
///     fn type_name(&self) -> &str { "MyData" }
///     fn as_any(&self) -> &dyn Any { self }
///     fn clone_box(&self) -> Box<dyn EmbeddedDataContent> { Box::new(self.clone()) }
///     fn eq_content(&self, other: &dyn EmbeddedDataContent) -> bool {
///         other.as_any().downcast_ref::<Self>().is_some_and(|o| self == o)
///     }
/// }
/// ```
pub trait EmbeddedDataContent: fmt::Debug + Send + Sync {
    /// The data type, e.g. `Relational`.
    fn type_name(&self) -> &str;

    /// Downcast to the concrete type.
    fn as_any(&self) -> &dyn Any;

    /// Clone into a new boxed trait object.
    fn clone_box(&self) -> Box<dyn EmbeddedDataContent>;

    /// Equality with another content: implementations downcast `other` to
    /// `Self` and compare.
    fn eq_content(&self, other: &dyn EmbeddedDataContent) -> bool;
}

impl Clone for Box<dyn EmbeddedDataContent> {
    fn clone(&self) -> Self {
        self.clone_box()
    }
}

impl PartialEq for dyn EmbeddedDataContent {
    fn eq(&self, other: &Self) -> bool {
        self.eq_content(other)
    }
}

/// `Relation #{ … }#` data: tables of text cells, the engine's
/// `RelationElementsData`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct RelationData {
    /// The tables.
    pub elements: Vec<RelationElement>,
    /// The body's tables, from the first to the end of the body, as the
    /// engine positions them (see the parser).
    pub source_info: SourceInfo,
}

/// One table of relation data: `path.to.table: a, b ⏎ 1, 2;`, or a table
/// without a path.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct RelationElement {
    /// The table's path, e.g. `["default", "PersonTable"]`; empty for a table
    /// without one.
    pub paths: Vec<SmolStr>,
    /// The header's cells.
    pub columns: Vec<SmolStr>,
    /// Each row's cells.
    pub rows: Vec<Vec<SmolStr>>,
    /// From the path (or `:`) to the closing `;`.
    pub source_info: SourceInfo,
}

/// `ExternalFormat #{ … }#` data: text in a format the content type names,
/// the engine's `ExternalFormatData`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ExternalFormatData {
    /// The content type, e.g. `application/json`.
    pub content_type: String,
    /// The data.
    pub data: String,
}

/// `Reference #{ … }#` data: a data element's path, the engine's
/// `DataElementReference`. A plug-in grammar may read one too, referring
/// to an element of another type (the data space's `DataspaceTestData`).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DataElementReference {
    /// The referenced element's path, its quoted parts unquoted.
    pub data_element: String,
    /// The referenced element's type, the engine's `PackageableElementType`:
    /// `DATA` for `Reference`.
    pub element_type: SmolStr,
}

/// `ModelStore #{ … }#` data: per class, instances or other embedded data,
/// the engine's `ModelStoreData`.
#[derive(Debug, Clone, PartialEq)]
pub struct ModelStoreData {
    /// The entries, in source order.
    pub model_data: Vec<ModelTestData>,
}

/// One class's entry of [`ModelStoreData`]: `my::Person: …`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct ModelTestData {
    /// The class.
    pub model: PackageableElementPtr,
    /// Its data.
    pub data: ModelTestDataValue,
    /// From the class to the end of its data.
    pub source_info: SourceInfo,
}

/// The data of a [`ModelTestData`].
#[derive(Debug, Clone, PartialEq)]
pub enum ModelTestDataValue {
    /// `[^my::Person(…), …]`, the engine's `ModelInstanceTestData`.
    Instances(Vec<ModelInstance>),
    /// `ExternalFormat #{ … }#` or other embedded data, the engine's
    /// `ModelEmbeddedTestData`.
    Embedded(EmbeddedDataValue),
}

/// An instance in model store data: `^my::Person(name = 'x', age = 1)`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct ModelInstance {
    /// The instance's class.
    pub class: PackageableElementPtr,
    /// Its property values, in source order.
    pub properties: Vec<ModelPropertyValue>,
    /// From `^` to `)`.
    pub source_info: SourceInfo,
}

/// A property's value in a [`ModelInstance`]: `name = 'x'` or
/// `names = ['x', 'y']`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct ModelPropertyValue {
    /// The property.
    pub property: Identifier,
    /// The values: one, or those of a `[…]` list.
    pub values: Vec<ModelValue>,
    /// From the property to the end of its value.
    pub source_info: SourceInfo,
}

/// A value in model store data.
#[derive(Debug, Clone, PartialEq)]
pub enum ModelValue {
    /// A literal; a signed number is one literal, positioned from its sign.
    Literal(Literal),
    /// `my::Gender.MALE`.
    EnumValue {
        /// The enumeration.
        enumeration: PackageableElementPtr,
        /// The value.
        value: Identifier,
        /// From the enumeration to the value.
        source_info: SourceInfo,
    },
    /// A nested instance.
    Instance(ModelInstance),
}
