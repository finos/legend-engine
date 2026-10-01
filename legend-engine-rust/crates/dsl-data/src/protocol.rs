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

//! `ElementProtocol` for data elements: the engine's `DataElement`.
//!
//! ```json
//! {
//!     "_type": "dataElement",
//!     "package": "my",
//!     "name": "MyData",
//!     "stereotypes": [],
//!     "taggedValues": [],
//!     "data": { "_type": "externalFormat", … },
//!     "dataResolvers": [ { "_type": "referenceDataResolver", … } ],
//!     "sourceInformation": { … }
//! }
//! ```
//!
//! `dataResolvers` is absent when there are none, as the engine leaves it
//! null.

use legend_pure_parser_ast::annotation::PackageableElementPtr;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use legend_pure_parser_protocol::v1::annotation::{StereotypePtr, TaggedValue};
use legend_pure_parser_protocol::v1::convert::{convert_embedded_data, section_stereotype};
use legend_pure_parser_protocol::v1::data::EmbeddedData;
use legend_pure_parser_protocol::v1::element::PackageableElementPointer;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;
use serde::Serialize;

use crate::ast::{DataElement, DataResolver, KIND};

/// Protocol converter for data elements.
pub struct DataElementProtocol;

impl ElementProtocol for DataElementProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<serde_json::Value> {
        let element = element
            .as_any()
            .downcast_ref::<DataElement>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "DataElementProtocol: a '{}' element is not a DataElement",
                        element.kind()
                    ),
                    None,
                )
            })?;
        let json = DataElementJson {
            package: element
                .package
                .as_ref()
                .map(ToString::to_string)
                .unwrap_or_default(),
            name: element.name.value.to_string(),
            stereotypes: element.stereotypes.iter().map(section_stereotype).collect(),
            tagged_values: element.tagged_values.iter().map(Into::into).collect(),
            data: element
                .data
                .as_ref()
                .map(convert_embedded_data)
                .transpose()?,
            data_resolvers: if element.resolvers.is_empty() {
                None
            } else {
                Some(
                    element
                        .resolvers
                        .iter()
                        .map(resolver)
                        .collect::<Result<_>>()?,
                )
            },
            source_information: SourceInformation::from(&element.source_info),
        };
        Ok(serde_json::to_value(json)?)
    }
}

/// The engine's `DataElement`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "dataElement", rename_all = "camelCase")]
struct DataElementJson {
    package: String,
    name: String,
    stereotypes: Vec<StereotypePtr>,
    tagged_values: Vec<TaggedValue>,
    #[serde(skip_serializing_if = "Option::is_none")]
    data: Option<EmbeddedData>,
    #[serde(skip_serializing_if = "Option::is_none")]
    data_resolvers: Option<Vec<DataResolverJson>>,
    source_information: SourceInformation,
}

/// The engine's `DataResolver`s.
#[derive(Serialize)]
#[serde(tag = "_type")]
enum DataResolverJson {
    #[serde(rename = "baseDataResolver", rename_all = "camelCase")]
    Base {
        element_pointer: PackageableElementPointer,
        data: EmbeddedData,
        source_information: SourceInformation,
    },
    #[serde(rename = "referenceDataResolver", rename_all = "camelCase")]
    Reference {
        element_pointer: PackageableElementPointer,
        source_information: SourceInformation,
    },
}

fn resolver(resolver: &DataResolver) -> Result<DataResolverJson> {
    Ok(match resolver {
        DataResolver::Base {
            element,
            data,
            source_info,
        } => DataResolverJson::Base {
            element_pointer: pointer(element),
            data: convert_embedded_data(data)?,
            source_information: SourceInformation::from(source_info),
        },
        // Positioned on the path, as its pointer is.
        DataResolver::Reference(element) => DataResolverJson::Reference {
            element_pointer: pointer(element),
            source_information: SourceInformation::from(&element.source_info),
        },
    })
}

/// A pointer without a type, positioned on the path.
fn pointer(element: &PackageableElementPtr) -> PackageableElementPointer {
    PackageableElementPointer {
        element_type: None,
        path: element.to_string(),
        source_information: Some(SourceInformation::from(&element.source_info)),
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static DATA_ELEMENT_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &DataElementProtocol;
