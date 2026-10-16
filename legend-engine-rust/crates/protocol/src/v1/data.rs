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

//! Protocol embedded data: the engine's `EmbeddedData` and its core types
//! (`org.finos.legend.engine.protocol.pure.v1.model.data`), in function
//! tests, data elements and plug-in grammars' data.

use serde::{Deserialize, Serialize};

use super::element::PackageableElementPointer;
use super::source_info::SourceInformation;
use super::value_spec::ValueSpecification;

/// Embedded data: the engine's `EmbeddedData`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(tag = "_type")]
pub enum EmbeddedData {
    /// Inline data in an external format.
    #[serde(rename = "externalFormat")]
    ExternalFormat(ExternalFormatData),
    /// Instances, or other embedded data, per class.
    #[serde(rename = "modelStore")]
    ModelStore(ModelStoreData),
    /// A data element.
    #[serde(rename = "reference", rename_all = "camelCase")]
    Reference {
        /// The data element.
        data_element: PackageableElementPointer,
        /// Source location.
        #[serde(default, skip_serializing_if = "Option::is_none")]
        source_information: Option<SourceInformation>,
    },
    /// Relation data: tables of text cells.
    #[serde(rename = "relationAccessor")]
    Relation(RelationElementsData),
    /// Data a plug-in grammar converts: the engine's JSON for it, its
    /// `_type` included. Untagged, so it serializes as that JSON verbatim.
    #[serde(untagged)]
    Extension(serde_json::Value),
}

/// Relation data, the engine's `RelationElementsData`. Its lists, like
/// those of [`RelationElement`], are always written, even empty: the
/// engine's walker sets them, and its classes default them to null.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RelationElementsData {
    /// The tables.
    #[serde(default)]
    pub relation_elements: Vec<RelationElement>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// One table of relation data: its path, header and rows.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RelationElement {
    /// The table's path; empty for a table without one.
    #[serde(default)]
    pub paths: Vec<String>,
    /// The header's cells.
    #[serde(default)]
    pub columns: Vec<String>,
    /// The rows.
    #[serde(default)]
    pub rows: Vec<RelationRowTestData>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// One row of a relation data table.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RelationRowTestData {
    /// The row's cells.
    #[serde(default)]
    pub values: Vec<String>,
}

/// Data in an external format, e.g. JSON.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalFormatData {
    /// The content type, e.g. `application/json`.
    pub content_type: String,
    /// The data.
    pub data: String,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// Model store data, the engine's `ModelStoreData`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelStoreData {
    /// One entry per class.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub model_data: Vec<ModelTestData>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// One class's model store data, the engine's `ModelTestData`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(tag = "_type")]
pub enum ModelTestData {
    /// Instances, the engine's `ModelInstanceTestData`.
    #[serde(rename = "modelInstanceData", rename_all = "camelCase")]
    Instances {
        /// The class.
        model: String,
        /// The instances: a collection of `new` calls.
        instances: ValueSpecification,
        /// Source location.
        #[serde(default, skip_serializing_if = "Option::is_none")]
        source_information: Option<SourceInformation>,
    },
    /// Embedded data, the engine's `ModelEmbeddedTestData`.
    #[serde(rename = "modelEmbeddedData", rename_all = "camelCase")]
    Embedded {
        /// The class.
        model: String,
        /// The data.
        data: EmbeddedData,
        /// Source location.
        #[serde(default, skip_serializing_if = "Option::is_none")]
        source_information: Option<SourceInformation>,
    },
}
