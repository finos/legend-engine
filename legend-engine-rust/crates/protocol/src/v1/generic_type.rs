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

//! Protocol generic type and packageable type.
//!
//! Maps to Java:
//! - `org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType`
//! - `org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType`
//!
//! - `org.finos.legend.engine.protocol.pure.m3.type.Type` (polymorphic on `_type`)
//! - `org.finos.legend.engine.protocol.pure.m3.relation.{RelationType, Column}`
//!
//! `GenericType` wraps a `rawType` — a named type (`packageableType`) or a
//! relation type (`relationType`) — plus optional type arguments,
//! multiplicity arguments, and type variable values.
//!
//! JSON example:
//! ```json
//! {
//!   "rawType": { "_type": "packageableType", "fullPath": "String" }
//! }
//! ```

use serde::{Deserialize, Serialize};

use super::multiplicity::Multiplicity;
use super::source_info::SourceInformation;

/// A reference to a named type by its fully qualified path.
///
/// Maps to Java `PackageableType extends PackageableElementPtr`.
/// In JSON: `{"fullPath": "String"}` or `{"fullPath": "meta::pure::metamodel::type::Integer"}`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PackageableType {
    /// The fully qualified path of the type (e.g., `"String"`, `"model::Person"`).
    pub full_path: String,
    /// Source location of the type reference.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// A `GenericType`'s base type: the engine's polymorphic `Type`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(tag = "_type")]
pub enum RawType {
    /// A named type. `_type = "packageableType"`.
    #[serde(rename = "packageableType")]
    Packageable(PackageableType),
    /// A relation type `(a: Integer, b: String[1])`. `_type = "relationType"`.
    #[serde(rename = "relationType")]
    Relation(RelationType),
}

impl RawType {
    /// A named type with the given path.
    #[must_use]
    pub fn packageable(
        full_path: impl Into<String>,
        source_information: Option<SourceInformation>,
    ) -> Self {
        Self::Packageable(PackageableType {
            full_path: full_path.into(),
            source_information,
        })
    }

    /// The path of a named type; `None` for a relation type.
    #[must_use]
    pub fn full_path(&self) -> Option<&str> {
        match self {
            Self::Packageable(p) => Some(&p.full_path),
            Self::Relation(_) => None,
        }
    }
}

/// A relation type: its columns. Maps to Java
/// `org.finos.legend.engine.protocol.pure.m3.relation.RelationType`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RelationType {
    /// The columns, in declaration order.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub columns: Vec<Column>,
    /// Source location (the engine's parser leaves it unset).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// One relation column. Maps to Java
/// `org.finos.legend.engine.protocol.pure.m3.relation.Column`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Column {
    /// The column name.
    pub name: String,
    /// The column type.
    pub generic_type: GenericType,
    /// The column multiplicity (`[0..1]` when not written).
    pub multiplicity: Multiplicity,
    /// Source location of the column declaration.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// A generic type with optional type arguments and multiplicity arguments.
///
/// Maps to Java `org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType`.
///
/// The `rawType` field holds the base type. `typeArguments` holds generic
/// parameters (e.g., `T` in `List<T>`), `multiplicityArguments` holds multiplicity
/// parameters, and `typeVariableValues` holds type variable value specifications.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GenericType {
    /// The base type (e.g., `{"_type": "packageableType", "fullPath": "String"}`).
    pub raw_type: RawType,
    /// Generic type arguments (e.g., `T` in `List<T>`).
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub type_arguments: Vec<GenericType>,
    /// Multiplicity arguments.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub multiplicity_arguments: Vec<Multiplicity>,
    /// Type variable values: the literals of `VARCHAR(200)`.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub type_variable_values: Vec<super::value_spec::ValueSpecification>,
    /// Source location of the generic type reference.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_packageable_type_serialization() {
        let pt = PackageableType {
            full_path: "String".into(),
            source_information: None,
        };
        let json = serde_json::to_value(&pt).unwrap();
        assert_eq!(json["fullPath"], "String");
        assert!(json.get("sourceInformation").is_none());
        assert_eq!(json.as_object().unwrap().len(), 1);
    }

    #[test]
    fn test_simple_generic_type() {
        let gt = GenericType {
            raw_type: RawType::Packageable(PackageableType {
                full_path: "String".into(),
                source_information: None,
            }),
            type_arguments: vec![],
            multiplicity_arguments: vec![],
            type_variable_values: vec![],
            source_information: None,
        };
        let json = serde_json::to_value(&gt).unwrap();

        // Only rawType should be present (all others empty/none → omitted)
        assert_eq!(json["rawType"]["fullPath"], "String");
        assert!(json.get("typeArguments").is_none());
        assert!(json.get("multiplicityArguments").is_none());
        assert!(json.get("typeVariableValues").is_none());
        assert!(json.get("sourceInformation").is_none());
    }

    #[test]
    fn test_generic_type_with_type_arguments() {
        let gt = GenericType {
            raw_type: RawType::Packageable(PackageableType {
                full_path: "meta::pure::metamodel::type::List".into(),
                source_information: None,
            }),
            type_arguments: vec![GenericType {
                raw_type: RawType::Packageable(PackageableType {
                    full_path: "String".into(),
                    source_information: None,
                }),
                type_arguments: vec![],
                multiplicity_arguments: vec![],
                type_variable_values: vec![],
                source_information: None,
            }],
            multiplicity_arguments: vec![],
            type_variable_values: vec![],
            source_information: None,
        };
        let json = serde_json::to_value(&gt).unwrap();

        assert_eq!(
            json["rawType"]["fullPath"],
            "meta::pure::metamodel::type::List"
        );
        let args = json["typeArguments"].as_array().unwrap();
        assert_eq!(args.len(), 1);
        assert_eq!(args[0]["rawType"]["fullPath"], "String");
    }

    #[test]
    fn test_generic_type_roundtrip() {
        let gt = GenericType {
            raw_type: RawType::Packageable(PackageableType {
                full_path: "Integer".into(),
                source_information: Some(SourceInformation {
                    source_id: "test.pure".into(),
                    start_line: 1,
                    start_column: 5,
                    end_line: 1,
                    end_column: 12,
                }),
            }),
            type_arguments: vec![],
            multiplicity_arguments: vec![Multiplicity::PURE_ONE],
            type_variable_values: vec![],
            source_information: None,
        };
        let json_str = serde_json::to_string(&gt).unwrap();
        let back: GenericType = serde_json::from_str(&json_str).unwrap();
        assert_eq!(back, gt);
    }

    #[test]
    fn test_deserialize_from_java_json() {
        let java_json = r#"{
            "rawType": { "_type": "packageableType", "fullPath": "String", "sourceInformation": {"sourceId": "a.pure", "startLine": 1, "startColumn": 1, "endLine": 1, "endColumn": 6} },
            "typeArguments": [],
            "multiplicityArguments": []
        }"#;
        let gt: GenericType = serde_json::from_str(java_json).unwrap();
        assert_eq!(gt.raw_type.full_path(), Some("String"));
        assert!(gt.type_arguments.is_empty());
        assert!(matches!(
            &gt.raw_type,
            RawType::Packageable(p) if p.source_information.is_some()
        ));
    }
}
