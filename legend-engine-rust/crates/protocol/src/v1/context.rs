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

//! Protocol `PureModelContextData` — the top-level container.
//!
//! Maps to Java `org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData`.
//! This is the outermost JSON structure that wraps all protocol elements.
//!
//! JSON example:
//! ```json
//! {
//!   "serializer": { "name": "pure", "version": "vX_X_X" },
//!   "elements": [ { "_type": "class", ... }, { "_type": "function", ... } ]
//! }
//! ```

use serde::{Deserialize, Serialize};

use super::element::PackageableElement;

/// Protocol metadata identifying the serializer.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Protocol {
    /// Serializer name (always `"pure"` for grammar-produced data).
    pub name: String,
    /// Serializer version (e.g., `"vX_X_X"`).
    pub version: String,
}

impl Default for Protocol {
    fn default() -> Self {
        Self {
            name: "pure".into(),
            version: "vX_X_X".into(),
        }
    }
}

/// The top-level protocol container — `PureModelContextData`.
///
/// This is the outermost JSON structure containing a serializer identifier
/// and a list of protocol elements. It's what the Java protocol expects to
/// receive and what our emitter produces.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PureModelContextData {
    /// The serializer identifier.
    #[serde(default)]
    pub serializer: Option<Protocol>,
    /// All packageable elements.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub elements: Vec<PackageableElement>,
}

impl PureModelContextData {
    /// Creates a new context data with the default protocol serializer.
    #[must_use]
    pub fn new(elements: Vec<PackageableElement>) -> Self {
        Self {
            serializer: Some(Protocol::default()),
            elements,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::super::element::{ProtocolClass, ProtocolProfile};
    use super::super::source_info::SourceInformation;
    use super::*;

    #[test]
    fn test_empty_context_serialization() {
        let ctx = PureModelContextData {
            serializer: Some(Protocol::default()),
            elements: vec![],
        };
        let json = serde_json::to_value(&ctx).unwrap();
        assert_eq!(json["serializer"]["name"], "pure");
        assert_eq!(json["serializer"]["version"], "vX_X_X");
        assert!(
            json.get("elements").is_none(),
            "empty elements should be omitted"
        );
    }

    #[test]
    fn test_context_with_elements() {
        let ctx = PureModelContextData::new(vec![
            PackageableElement::Profile(ProtocolProfile {
                package_path: "meta::pure::profiles".into(),
                name: "doc".into(),
                stereotypes: vec![],
                tags: vec!["description".into()],
                source_information: None,
            }),
            PackageableElement::Class(ProtocolClass {
                package_path: "model".into(),
                name: "Person".into(),
                super_types: vec![],
                properties: vec![],
                qualified_properties: vec![],
                constraints: vec![],
                original_milestoned_properties: vec![],
                stereotypes: vec![],
                tagged_values: vec![],
                source_information: None,
            }),
        ]);
        let json = serde_json::to_value(&ctx).unwrap();

        assert_eq!(json["elements"].as_array().unwrap().len(), 2);
        assert_eq!(json["elements"][0]["_type"], "profile");
        assert_eq!(json["elements"][1]["_type"], "class");
    }

    #[test]
    fn test_roundtrip_context() {
        let ctx = PureModelContextData::new(vec![PackageableElement::Class(ProtocolClass {
            package_path: "model".into(),
            name: "Foo".into(),
            super_types: vec![],
            properties: vec![],
            qualified_properties: vec![],
            constraints: vec![],
            original_milestoned_properties: vec![],
            stereotypes: vec![],
            tagged_values: vec![],
            source_information: Some(SourceInformation {
                source_id: "a.pure".into(),
                start_line: 1,
                start_column: 1,
                end_line: 3,
                end_column: 1,
            }),
        })]);
        let json_str = serde_json::to_string(&ctx).unwrap();
        let back: PureModelContextData = serde_json::from_str(&json_str).unwrap();
        assert_eq!(back, ctx);
    }

    #[test]
    fn test_deserialize_from_java_shape() {
        let java_json = r#"{
            "serializer": { "name": "pure", "version": "vX_X_X" },
            "elements": [
                {
                    "_type": "class",
                    "package": "model",
                    "name": "Person"
                }
            ]
        }"#;
        let ctx: PureModelContextData = serde_json::from_str(java_json).unwrap();
        assert_eq!(ctx.elements.len(), 1);
        if let PackageableElement::Class(c) = &ctx.elements[0] {
            assert_eq!(c.package_path, "model");
            assert_eq!(c.name, "Person");
        } else {
            panic!("expected Class");
        }
    }
}
