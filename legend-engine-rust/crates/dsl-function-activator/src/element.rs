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

//! A function activator, with a [`Record`] in the shape of the engine's
//! protocol object, and its JSON.

use std::any::Any;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use legend_pure_parser_protocol::element_protocol::{ConvertError, ElementProtocol, Result};
use legend_pure_parser_protocol::v1::annotation::TaggedValue as TaggedValueJson;
use legend_pure_parser_protocol::v1::convert::section_stereotype;
use legend_pure_parser_protocol::v1::record::Record;
use serde_json::{Value, json};

/// `'''doc''' Keyword <<s>> {t} my::A { … }`.
#[derive(Debug, Clone, PartialEq)]
pub struct ActivatorElement {
    /// Its section's kind, which is its kind.
    pub section: &'static str,
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

impl Spanned for ActivatorElement {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for ActivatorElement {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &self.stereotypes
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &self.tagged_values
    }
}

impl PackageableElement for ActivatorElement {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for ActivatorElement {
    fn kind(&self) -> &str {
        self.section
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

/// Protocol converter for the activators of the section it names: their
/// records' JSON, with their package, name and annotations (stereotypes
/// positioned on their values, as the section walkers but the Pure one
/// position them).
pub struct ActivatorProtocol(pub &'static str);

impl ElementProtocol for ActivatorProtocol {
    fn kind(&self) -> &str {
        self.0
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Value> {
        let element = element
            .as_any()
            .downcast_ref::<ActivatorElement>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "ActivatorProtocol: a '{}' element is not an ActivatorElement",
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
                .map(TaggedValueJson::from)
                .collect::<Vec<_>>(),
        )?;
        Ok(json)
    }
}
