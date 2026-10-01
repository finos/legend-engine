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

//! `ElementProtocol` for data quality elements: their records' JSON, with
//! their package and name, and the annotations of those that have them
//! (stereotypes positioned on their values, as the section walkers but the
//! Pure one position them).

use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use legend_pure_parser_protocol::v1::annotation::TaggedValue;
use legend_pure_parser_protocol::v1::convert::section_stereotype;
use linkme::distributed_slice;
use serde_json::{Value, json};

use crate::ast::{DataQualityElement, SECTION_KIND};

/// Protocol converter for data quality elements.
pub struct DataQualityProtocol;

impl ElementProtocol for DataQualityProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Value> {
        let element = element
            .as_any()
            .downcast_ref::<DataQualityElement>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "DataQualityProtocol: a '{}' element is not a DataQualityElement",
                        element.kind()
                    ),
                    None,
                )
            })?;
        let mut json = element.record.to_json()?;
        json["package"] = json!(
            element
                .package
                .as_ref()
                .map(ToString::to_string)
                .unwrap_or_default()
        );
        json["name"] = json!(element.name.value.as_str());
        if element.kind.annotated() {
            json["stereotypes"] = serde_json::to_value(
                element
                    .stereotypes
                    .iter()
                    .map(section_stereotype)
                    .collect::<Vec<_>>(),
            )?;
            json["taggedValues"] = serde_json::to_value(
                element
                    .tagged_values
                    .iter()
                    .map(TaggedValue::from)
                    .collect::<Vec<_>>(),
            )?;
        }
        Ok(json)
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static DATA_QUALITY_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &DataQualityProtocol;
