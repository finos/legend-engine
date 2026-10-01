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

//! Protocol property types — properties, qualified properties, constraints, defaults.
//!
//! Maps to Java:
//! - `org.finos.legend.engine.protocol.pure.m3.function.property.Property`
//! - `org.finos.legend.engine.protocol.pure.m3.function.property.QualifiedProperty`
//! - `org.finos.legend.engine.protocol.pure.m3.constraint.Constraint`
//! - `org.finos.legend.engine.protocol.pure.m3.function.property.DefaultValue`
//! - `org.finos.legend.engine.protocol.pure.m3.function.property.AggregationKind`

use serde::{Deserialize, Serialize};

use super::annotation::{StereotypePtr, TaggedValue};
use super::generic_type::GenericType;
use super::multiplicity::Multiplicity;
use super::source_info::SourceInformation;

/// Aggregation kind for properties.
///
/// Maps to Java `org.finos.legend.engine.protocol.pure.m3.function.property.AggregationKind`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum AggregationKind {
    /// No aggregation.
    NONE,
    /// Shared aggregation.
    SHARED,
    /// Composite aggregation.
    COMPOSITE,
}

/// A default value for a property.
///
/// Maps to Java `org.finos.legend.engine.protocol.pure.m3.function.property.DefaultValue`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DefaultValue {
    /// The value specification for the default.
    pub value: serde_json::Value,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// A class property.
///
/// Maps to Java `org.finos.legend.engine.protocol.pure.m3.function.property.Property`.
///
/// JSON example:
/// ```json
/// {
///   "name": "firstName",
///   "genericType": { "rawType": { "fullPath": "String" } },
///   "multiplicity": { "lowerBound": 1, "upperBound": 1 }
/// }
/// ```
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Property {
    /// The property name.
    pub name: String,
    /// The property's generic type.
    pub generic_type: GenericType,
    /// The property's multiplicity.
    pub multiplicity: Multiplicity,
    /// Optional default value.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub default_value: Option<DefaultValue>,
    /// Stereotypes applied to this property.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values applied to this property.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub tagged_values: Vec<TaggedValue>,
    /// Aggregation kind.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub aggregation: Option<AggregationKind>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// A qualified (derived) property.
///
/// Maps to Java `org.finos.legend.engine.protocol.pure.m3.function.property.QualifiedProperty`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QualifiedProperty {
    /// The qualified property name.
    pub name: String,
    /// Parameters (as value specifications).
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub parameters: Vec<serde_json::Value>,
    /// Return generic type.
    pub return_generic_type: GenericType,
    /// Return multiplicity.
    pub return_multiplicity: Multiplicity,
    /// Stereotypes.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub tagged_values: Vec<TaggedValue>,
    /// Body expressions.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub body: Vec<serde_json::Value>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// A class constraint.
///
/// Maps to Java `org.finos.legend.engine.protocol.pure.m3.constraint.Constraint`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Constraint {
    /// Constraint name.
    pub name: String,
    /// Owner (optional).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub owner: Option<String>,
    /// The constraint function definition (always a lambda).
    pub function_definition: serde_json::Value,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
    /// External identifier.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub external_id: Option<String>,
    /// Enforcement level (e.g., `"Warn"`, `"Error"`).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub enforcement_level: Option<String>,
    /// Message function (evaluated when constraint fails).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub message_function: Option<serde_json::Value>,
}

#[cfg(test)]
mod tests {
    use super::super::generic_type::{PackageableType, RawType};
    use super::*;

    fn simple_type(path: &str) -> GenericType {
        GenericType {
            raw_type: RawType::Packageable(PackageableType {
                full_path: path.into(),
                source_information: None,
            }),
            type_arguments: vec![],
            multiplicity_arguments: vec![],
            type_variable_values: vec![],
            source_information: None,
        }
    }

    #[test]
    fn test_property_serialization() {
        let p = Property {
            name: "firstName".into(),
            generic_type: simple_type("String"),
            multiplicity: Multiplicity::PURE_ONE,
            default_value: None,
            stereotypes: vec![],
            tagged_values: vec![],
            aggregation: None,
            source_information: None,
        };
        let json = serde_json::to_value(&p).unwrap();

        assert_eq!(json["name"], "firstName");
        assert_eq!(json["genericType"]["rawType"]["fullPath"], "String");
        assert_eq!(json["multiplicity"]["lowerBound"], 1);
        assert_eq!(json["multiplicity"]["upperBound"], 1);
        // Empty vecs and None should be omitted
        assert!(json.get("stereotypes").is_none());
        assert!(json.get("taggedValues").is_none());
        assert!(json.get("aggregation").is_none());
        assert!(json.get("defaultValue").is_none());
        assert!(json.get("sourceInformation").is_none());
    }

    #[test]
    fn test_property_with_aggregation() {
        let p = Property {
            name: "parts".into(),
            generic_type: simple_type("Part"),
            multiplicity: Multiplicity::ZERO_MANY,
            default_value: None,
            stereotypes: vec![],
            tagged_values: vec![],
            aggregation: Some(AggregationKind::COMPOSITE),
            source_information: None,
        };
        let json = serde_json::to_value(&p).unwrap();
        assert_eq!(json["aggregation"], "COMPOSITE");
    }

    #[test]
    fn test_qualified_property_serialization() {
        let qp = QualifiedProperty {
            name: "fullName".into(),
            parameters: vec![],
            return_generic_type: simple_type("String"),
            return_multiplicity: Multiplicity::PURE_ONE,
            stereotypes: vec![],
            tagged_values: vec![],
            body: vec![],
            source_information: None,
        };
        let json = serde_json::to_value(&qp).unwrap();

        assert_eq!(json["name"], "fullName");
        assert_eq!(json["returnGenericType"]["rawType"]["fullPath"], "String");
        assert_eq!(json["returnMultiplicity"]["lowerBound"], 1);
        // Empty vecs omitted
        assert!(json.get("parameters").is_none());
        assert!(json.get("body").is_none());
    }

    #[test]
    fn test_constraint_serialization() {
        let c = Constraint {
            name: "positiveAge".into(),
            owner: None,
            function_definition: serde_json::json!({"_type": "lambda", "body": []}),
            source_information: None,
            external_id: None,
            enforcement_level: Some("Error".into()),
            message_function: None,
        };
        let json = serde_json::to_value(&c).unwrap();

        assert_eq!(json["name"], "positiveAge");
        assert_eq!(json["enforcementLevel"], "Error");
        assert_eq!(json["functionDefinition"]["_type"], "lambda");
        assert!(json.get("owner").is_none());
        assert!(json.get("externalId").is_none());
        assert!(json.get("messageFunction").is_none());
    }

    #[test]
    fn test_property_roundtrip() {
        let p = Property {
            name: "age".into(),
            generic_type: simple_type("Integer"),
            multiplicity: Multiplicity::ZERO_ONE,
            default_value: None,
            stereotypes: vec![],
            tagged_values: vec![],
            aggregation: None,
            source_information: None,
        };
        let json_str = serde_json::to_string(&p).unwrap();
        let back: Property = serde_json::from_str(&json_str).unwrap();
        assert_eq!(back, p);
    }
}
