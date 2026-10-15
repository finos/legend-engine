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

//! `ElementProtocol` for persistences and persistence contexts: their
//! records' JSON, with their package and name.

use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use linkme::distributed_slice;
use serde_json::value::{RawValue, to_raw_value};

use crate::ast::{PersistenceElement, SECTION_KIND};

/// Protocol converter for the section's elements.
pub struct PersistenceProtocol;

impl ElementProtocol for PersistenceProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Box<RawValue>> {
        let element = element
            .as_any()
            .downcast_ref::<PersistenceElement>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "PersistenceProtocol: a '{}' element is not a persistence element",
                        element.kind()
                    ),
                    None,
                )
            })?;
        let json = element
            .record
            .converted()?
            .with(
                "package",
                element
                    .package
                    .as_ref()
                    .map(ToString::to_string)
                    .unwrap_or_default(),
            )
            .with("name", element.name.value.as_str());
        Ok(to_raw_value(&json)?)
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static PERSISTENCE_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &PersistenceProtocol;
