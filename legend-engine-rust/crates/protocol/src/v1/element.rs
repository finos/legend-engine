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

//! Protocol `PackageableElement` — the top-level element discriminated union.
//!
//! Maps to Java `org.finos.legend.engine.protocol.pure.m3.PackageableElement`
//! and its concrete subclasses.

use serde::{Deserialize, Serialize};

use super::annotation::{StereotypePtr, TaggedValue};
use super::generic_type::GenericType;
use super::multiplicity::Multiplicity;
use super::property::{Constraint, Property, QualifiedProperty};
use super::source_info::SourceInformation;
use super::value_spec::{ValueSpecification, Variable, serialize_variables};

/// Top-level packageable element — discriminated by `_type` in JSON.
///
/// Each variant wraps a named struct (struct-per-variant pattern).
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(tag = "_type")]
pub enum PackageableElement {
    /// A class definition. `_type = "class"`.
    #[serde(rename = "class")]
    Class(ProtocolClass),
    /// An enumeration definition. `_type = "Enumeration"`.
    #[serde(rename = "Enumeration")]
    Enumeration(ProtocolEnumeration),
    /// A function definition. `_type = "function"`.
    #[serde(rename = "function")]
    Function(ProtocolFunction),
    /// A profile definition. `_type = "profile"`.
    #[serde(rename = "profile")]
    Profile(ProtocolProfile),
    /// An association definition. `_type = "association"`.
    #[serde(rename = "association")]
    Association(ProtocolAssociation),
    /// A measure definition. `_type = "measure"`.
    #[serde(rename = "measure")]
    Measure(ProtocolMeasure),
    /// A section index (parser metadata). `_type = "sectionIndex"`.
    #[serde(rename = "sectionIndex")]
    SectionIndex(ProtocolSectionIndex),
    /// An element owned by a section-grammar plug-in (e.g. a mapping or a
    /// diagram), already converted to its complete protocol JSON object by
    /// a registered [`ElementProtocol`](crate::ElementProtocol). The object
    /// carries its own `_type`.
    ///
    /// Untagged, so it serializes as the plug-in's object verbatim and,
    /// when deserializing, catches any `_type` the core model doesn't know.
    #[serde(untagged)]
    Extension(serde_json::Value),
}

impl PackageableElement {
    /// The element's path as the engine computes it (`getPath()`):
    /// `package::name`, or just `name` without a package. For a function the
    /// name includes its signature (`f_String_1__Boolean_1_`).
    #[must_use]
    pub fn path(&self) -> String {
        let (package, name) = match self {
            Self::Class(e) => (e.package_path.as_str(), e.name.as_str()),
            Self::Enumeration(e) => (e.package_path.as_str(), e.name.as_str()),
            Self::Function(e) => (e.package_path.as_str(), e.name.as_str()),
            Self::Profile(e) => (e.package_path.as_str(), e.name.as_str()),
            Self::Association(e) => (e.package_path.as_str(), e.name.as_str()),
            Self::Measure(e) => (e.package_path.as_str(), e.name.as_str()),
            Self::SectionIndex(e) => (e.package_path.as_str(), e.name.as_str()),
            Self::Extension(v) => (
                v.get("package")
                    .and_then(serde_json::Value::as_str)
                    .unwrap_or(""),
                v.get("name")
                    .and_then(serde_json::Value::as_str)
                    .unwrap_or(""),
            ),
        };
        if package.is_empty() {
            name.to_string()
        } else {
            format!("{package}::{name}")
        }
    }
}

/// A reference to a packageable element: the engine's
/// `PackageableElementPointer`, e.g. a class's super type
/// `{"type": "CLASS", "path": "model::Legal", "sourceInformation": …}`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PackageableElementPointer {
    /// The kind of element (`CLASS`, …): the engine's `PackageableElementType`,
    /// when known.
    #[serde(rename = "type", default, skip_serializing_if = "Option::is_none")]
    pub element_type: Option<String>,
    /// The element's path.
    pub path: String,
    /// Source location of the reference.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

impl PackageableElementPointer {
    /// A pointer to the class at `path`.
    #[must_use]
    pub fn class(path: impl Into<String>, source_information: Option<SourceInformation>) -> Self {
        Self {
            element_type: Some("CLASS".to_string()),
            path: path.into(),
            source_information,
        }
    }
}

impl From<&str> for PackageableElementPointer {
    fn from(path: &str) -> Self {
        Self::class(path, None)
    }
}

impl From<String> for PackageableElementPointer {
    fn from(path: String) -> Self {
        Self::class(path, None)
    }
}

/// A stereotype or tag a profile declares: the engine's `ProfileStereotype`
/// / `ProfileTag`, `{"value": …, "sourceInformation": …}`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProfileEntry {
    /// The stereotype or tag name.
    pub value: String,
    /// Source location of the declaration.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

impl From<&str> for ProfileEntry {
    fn from(value: &str) -> Self {
        Self {
            value: value.to_string(),
            source_information: None,
        }
    }
}

impl From<String> for ProfileEntry {
    fn from(value: String) -> Self {
        Self {
            value,
            source_information: None,
        }
    }
}

/// Protocol representation of a class element.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolClass {
    /// Package path.
    #[serde(rename = "package")]
    pub package_path: String,
    /// Element name.
    pub name: String,
    /// Super types (extends).
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub super_types: Vec<PackageableElementPointer>,
    /// Regular properties.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub properties: Vec<Property>,
    /// Qualified (derived) properties.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub qualified_properties: Vec<QualifiedProperty>,
    /// Constraints.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub constraints: Vec<Constraint>,
    /// Original milestoned properties (business temporal).
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub original_milestoned_properties: Vec<Property>,
    /// Stereotypes.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub tagged_values: Vec<TaggedValue>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// Protocol representation of an enumeration element.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolEnumeration {
    /// Package path.
    #[serde(rename = "package")]
    pub package_path: String,
    /// Element name.
    pub name: String,
    /// Enum values. Always written, even empty: the engine's field defaults
    /// to null, not to an empty list.
    #[serde(default)]
    pub values: Vec<ProtocolEnumMember>,
    /// Stereotypes.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub tagged_values: Vec<TaggedValue>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// An enum member (a value within an enumeration).
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolEnumMember {
    /// The enum value name.
    pub value: String,
    /// Stereotypes on this member.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values on this member.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub tagged_values: Vec<TaggedValue>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// Protocol representation of a function element.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolFunction {
    /// Package path.
    #[serde(rename = "package")]
    pub package_path: String,
    /// Function name.
    pub name: String,
    /// Parameters.
    #[serde(
        default,
        skip_serializing_if = "Vec::is_empty",
        serialize_with = "serialize_variables"
    )]
    pub parameters: Vec<Variable>,
    /// Return generic type.
    pub return_generic_type: GenericType,
    /// Return multiplicity.
    pub return_multiplicity: Multiplicity,
    /// Body expressions.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub body: Vec<ValueSpecification>,
    /// Stereotypes.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub tagged_values: Vec<TaggedValue>,
    /// Function test suites.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub tests: Vec<super::function_test::FunctionTestSuite>,
    /// Pre-constraints.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub pre_constraints: Vec<serde_json::Value>,
    /// Post-constraints.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub post_constraints: Vec<serde_json::Value>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// Protocol representation of a profile element.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolProfile {
    /// Package path.
    #[serde(rename = "package")]
    pub package_path: String,
    /// Profile name.
    pub name: String,
    /// Stereotypes declared by this profile.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub stereotypes: Vec<ProfileEntry>,
    /// Tags declared by this profile.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub tags: Vec<ProfileEntry>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// Protocol representation of an association element.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolAssociation {
    /// Package path.
    #[serde(rename = "package")]
    pub package_path: String,
    /// Association name.
    pub name: String,
    /// Properties (typically exactly two).
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub properties: Vec<Property>,
    /// Qualified properties.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub qualified_properties: Vec<QualifiedProperty>,
    /// Original milestoned properties.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub original_milestoned_properties: Vec<Property>,
    /// Stereotypes.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub tagged_values: Vec<TaggedValue>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// A unit definition within a measure. Maps to Java
/// `org.finos.legend.engine.protocol.pure.m3.type.Unit`.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolUnit {
    /// The unit's own name (`Pes`), without its measure.
    pub name: String,
    /// The path of the unit's measure.
    pub measure: String,
    /// The conversion to the canonical unit, a lambda `{x | …}`; absent for
    /// non-convertible units.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub conversion_function: Option<ValueSpecification>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// Protocol representation of a measure element.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolMeasure {
    /// Package path.
    #[serde(rename = "package")]
    pub package_path: String,
    /// Measure name.
    pub name: String,
    /// The canonical unit (marked with `*`).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub canonical_unit: Option<ProtocolUnit>,
    /// Non-canonical units. The engine leaves them null (not empty) for a
    /// non-convertible measure with one unit.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub non_canonical_units: Option<Vec<ProtocolUnit>>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// A section within a section index — discriminated by `_type`.
///
/// Maps to Java `Section` hierarchy:
/// - `DefaultCodeSection` → `_type = "default"`
/// - `ImportAwareCodeSection` → `_type = "importAware"` (adds `imports`)
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(tag = "_type")]
pub enum ProtocolSection {
    /// A default code section with no imports.
    #[serde(rename = "default")]
    Default(DefaultCodeSection),
    /// An import-aware code section.
    #[serde(rename = "importAware")]
    ImportAware(ImportAwareCodeSection),
}

/// A default code section.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DefaultCodeSection {
    /// The parser name (e.g., `"Pure"`, `"Mapping"`).
    pub parser_name: String,
    /// Element paths in this section.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub elements: Vec<String>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// An import-aware code section (adds `imports` to the base section).
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ImportAwareCodeSection {
    /// The parser name (e.g., `"Pure"`).
    pub parser_name: String,
    /// Element paths in this section.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub elements: Vec<String>,
    /// Import statements.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub imports: Vec<String>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// Protocol representation of a section index (parser metadata).
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolSectionIndex {
    /// Package path.
    #[serde(rename = "package")]
    pub package_path: String,
    /// Section index name (typically the source file name).
    pub name: String,
    /// Sections in this index.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub sections: Vec<ProtocolSection>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
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
    fn test_class_type_discriminator() {
        let e = PackageableElement::Class(ProtocolClass {
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
        });
        let json = serde_json::to_value(&e).unwrap();
        assert_eq!(json["_type"], "class");
        assert_eq!(json["package"], "model");
        assert_eq!(json["name"], "Person");
    }

    #[test]
    fn test_enumeration_type_is_capitalized() {
        let e = PackageableElement::Enumeration(ProtocolEnumeration {
            package_path: "model".into(),
            name: "Color".into(),
            values: vec![ProtocolEnumMember {
                value: "RED".into(),
                stereotypes: vec![],
                tagged_values: vec![],
                source_information: None,
            }],
            stereotypes: vec![],
            tagged_values: vec![],
            source_information: None,
        });
        let json = serde_json::to_value(&e).unwrap();
        assert_eq!(json["_type"], "Enumeration"); // Capital E!
        assert_eq!(json["values"][0]["value"], "RED");
    }

    #[test]
    fn test_profile_type_discriminator() {
        let e = PackageableElement::Profile(ProtocolProfile {
            package_path: "meta::pure::profiles".into(),
            name: "doc".into(),
            stereotypes: vec!["deprecated".into()],
            tags: vec!["description".into()],
            source_information: None,
        });
        let json = serde_json::to_value(&e).unwrap();
        assert_eq!(json["_type"], "profile");
        assert_eq!(json["stereotypes"][0]["value"], "deprecated");
        assert_eq!(json["tags"][0]["value"], "description");
    }

    #[test]
    fn test_function_type_discriminator() {
        let e = PackageableElement::Function(ProtocolFunction {
            package_path: "model".into(),
            name: "hello__String_1_".into(),
            parameters: vec![],
            return_generic_type: simple_type("String"),
            return_multiplicity: Multiplicity::PURE_ONE,
            body: vec![],
            stereotypes: vec![],
            tagged_values: vec![],
            tests: vec![],
            pre_constraints: vec![],
            post_constraints: vec![],
            source_information: None,
        });
        let json = serde_json::to_value(&e).unwrap();
        assert_eq!(json["_type"], "function");
        assert_eq!(json["name"], "hello__String_1_");
    }

    #[test]
    fn test_class_empty_vecs_omitted() {
        let e = PackageableElement::Class(ProtocolClass {
            package_path: "model".into(),
            name: "Empty".into(),
            super_types: vec![],
            properties: vec![],
            qualified_properties: vec![],
            constraints: vec![],
            original_milestoned_properties: vec![],
            stereotypes: vec![],
            tagged_values: vec![],
            source_information: None,
        });
        let json = serde_json::to_value(&e).unwrap();
        assert!(json.get("properties").is_none());
        assert!(json.get("superTypes").is_none());
        assert!(json.get("stereotypes").is_none());
        assert!(json.get("constraints").is_none());
        assert!(json.get("sourceInformation").is_none());
    }

    #[test]
    fn test_section_index_serialization() {
        let e = PackageableElement::SectionIndex(ProtocolSectionIndex {
            package_path: "__internal__".into(),
            name: "test.pure".into(),
            sections: vec![
                ProtocolSection::ImportAware(ImportAwareCodeSection {
                    parser_name: "Pure".into(),
                    elements: vec!["model::Person".into(), "model::Firm".into()],
                    imports: vec!["model".into()],
                    source_information: None,
                }),
                ProtocolSection::Default(DefaultCodeSection {
                    parser_name: "Mapping".into(),
                    elements: vec![],
                    source_information: None,
                }),
            ],
            source_information: None,
        });
        let json = serde_json::to_value(&e).unwrap();
        assert_eq!(json["_type"], "sectionIndex");
        // ImportAware section
        assert_eq!(json["sections"][0]["_type"], "importAware");
        assert_eq!(json["sections"][0]["parserName"], "Pure");
        assert_eq!(json["sections"][0]["elements"].as_array().unwrap().len(), 2);
        assert_eq!(json["sections"][0]["imports"][0], "model");
        // Default section
        assert_eq!(json["sections"][1]["_type"], "default");
        assert_eq!(json["sections"][1]["parserName"], "Mapping");
    }

    #[test]
    fn test_roundtrip_class() {
        let e = PackageableElement::Class(ProtocolClass {
            package_path: "model".into(),
            name: "Person".into(),
            super_types: vec!["model::LegalEntity".into()],
            properties: vec![],
            qualified_properties: vec![],
            constraints: vec![],
            original_milestoned_properties: vec![],
            stereotypes: vec![],
            tagged_values: vec![],
            source_information: Some(SourceInformation {
                source_id: "test.pure".into(),
                start_line: 1,
                start_column: 1,
                end_line: 5,
                end_column: 1,
            }),
        });
        let json_str = serde_json::to_string(&e).unwrap();
        let back: PackageableElement = serde_json::from_str(&json_str).unwrap();
        assert_eq!(back, e);
    }
}
