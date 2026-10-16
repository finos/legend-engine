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

//! Shared annotation types: stereotypes, tagged values, and parameters.
//!
//! These types are used across multiple elements (classes, enums, functions,
//! associations, properties) and are factored out here to avoid duplication.

use crate::Spanned;
use crate::element::PackageableElement;
use crate::source_info::SourceInfo;
use crate::type_ref::{Identifier, Multiplicity, Package, TypeSpec};

/// A qualified reference to a packageable element (e.g., a Profile, Class, Enum).
///
/// This is distinct from [`Package`] which models the package hierarchy itself.
/// A `PackageableElementPtr` points *to* a packageable element via its fully
/// qualified path and element name.
///
/// For example, in `<<meta::pure::profiles::doc.deprecated>>`, the profile
/// reference `meta::pure::profiles::doc` is a `PackageableElementPtr` with:
/// - `package`: `meta::pure::profiles`
/// - `name`: `doc`
#[derive(Debug, Clone, PartialEq, Eq, Spanned)]
pub struct PackageableElementPtr {
    /// The package the element belongs to, or `None` for root-level elements.
    pub package: Option<Package>,
    /// The element name.
    pub name: Identifier,
    /// Source location.
    pub source_info: SourceInfo,
}

impl PackageableElement for PackageableElementPtr {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name
    }
}

impl crate::element::Annotated for PackageableElementPtr {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }
    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl std::fmt::Display for PackageableElementPtr {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        if let Some(pkg) = &self.package {
            write!(f, "{pkg}::{}", self.name)
        } else {
            write!(f, "{}", self.name)
        }
    }
}

/// A reference to a stereotype on a profile: `<<profile.stereotypeName>>`.
///
/// For example, `<<temporal.businesstemporal>>` has:
/// - `profile`: reference to the profile element (`temporal`)
/// - `value`: the stereotype name (`businesstemporal`)
#[derive(Debug, Clone, PartialEq, Spanned)]
pub struct StereotypePtr {
    /// Reference to the profile this stereotype belongs to.
    pub profile: PackageableElementPtr,
    /// The stereotype name within the profile.
    pub value: Identifier,
    /// Where the value alone is: the engine's section walkers other than
    /// the Pure one position a stereotype there.
    pub value_source_info: SourceInfo,
    /// Source location of the full stereotype reference.
    pub source_info: SourceInfo,
}

/// A tagged value on an element: `{profile.tagName = 'value'}`.
///
/// For example, `{doc.description = 'A person'}` has:
/// - `tag`: a [`TagPtr`] pointing to `doc.description`
/// - `value`: the string `"A person"`
#[derive(Debug, Clone, PartialEq, Spanned)]
pub struct TaggedValue {
    /// The tag reference (profile + tag name).
    pub tag: TagPtr,
    /// The string value assigned to the tag.
    pub value: String,
    /// Written as a text block (`'''…'''`): the protocol's `multiLine`.
    pub multi_line: bool,
    /// Source location of the full tagged value.
    pub source_info: SourceInfo,
}

/// A reference to a tag on a profile: `profile.tagName`.
#[derive(Debug, Clone, PartialEq, Spanned)]
pub struct TagPtr {
    /// Reference to the profile this tag belongs to.
    pub profile: PackageableElementPtr,
    /// The tag name within the profile.
    pub value: Identifier,
    /// Source location of this tag reference.
    pub source_info: SourceInfo,
}

/// A function or qualified property parameter.
///
/// For example, `name: String[1]` has:
/// - `name`: `"name"`
/// - `type_ref`: `String`
/// - `multiplicity`: `[1]`
#[derive(Debug, Clone, PartialEq, Spanned)]
pub struct Parameter {
    /// Parameter name (may be quoted, e.g., `'1,2,3'`).
    pub name: Identifier,
    /// Parameter type. `None` for untyped lambda parameters (e.g., `{x | ...}`).
    pub type_ref: Option<TypeSpec>,
    /// Parameter multiplicity. `None` for untyped lambda parameters.
    pub multiplicity: Option<Multiplicity>,
    /// Source location of this parameter.
    pub source_info: SourceInfo,
}

/// A simple string value with source information.
///
/// Used in profile definitions where stereotype names and tag names
/// are plain strings that need source tracking.
///
/// Carries `Serialize`/`Deserialize` so compiled-model nodes that
/// embed it (e.g. a compiled profile node)
/// can round-trip through `.purem` binary snapshots. The rule "no
/// serde on the AST" is about Protocol JSON responsibilities (which
/// still live exclusively in the `protocol` crate); binary
/// round-trip via postcard is a distinct concern, and `SourceInfo`
/// already has the same derives for the same reason.
#[derive(Debug, Clone, PartialEq, Eq, Spanned, serde::Serialize, serde::Deserialize)]
pub struct SpannedString {
    /// The string value.
    pub value: Identifier,
    /// Source location.
    pub source_info: SourceInfo,
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::source_info::Spanned;
    use crate::type_ref::TypeReference;
    use smol_str::SmolStr;

    use crate::test_utils::src;

    fn profile_ref(name: &str) -> PackageableElementPtr {
        PackageableElementPtr {
            package: None,
            name: SmolStr::new(name),
            source_info: src(),
        }
    }

    #[test]
    fn test_packageable_element_ptr() {
        let ptr = PackageableElementPtr {
            package: Some(
                Package::root(SmolStr::new("meta"), src())
                    .child(SmolStr::new("pure"), src())
                    .child(SmolStr::new("profiles"), src()),
            ),
            name: SmolStr::new("doc"),
            source_info: SourceInfo::new("test.pure", 1, 3, 1, 18),
        };
        assert_eq!(ptr.name(), "doc");
        assert_eq!(ptr.package().unwrap().to_string(), "meta::pure::profiles");
        assert_eq!(ptr.source_info().start_column, 3);
    }

    #[test]
    fn test_packageable_element_ptr_root() {
        let ptr = PackageableElementPtr {
            package: None,
            name: SmolStr::new("doc"),
            source_info: src(),
        };
        assert_eq!(ptr.name(), "doc");
        assert!(ptr.package().is_none());
    }

    #[test]
    fn test_stereotype_ptr_is_spanned() {
        let stereo = StereotypePtr {
            profile: profile_ref("temporal"),
            value: SmolStr::new("businesstemporal"),
            value_source_info: SourceInfo::new("test.pure", 1, 15, 1, 30),
            source_info: SourceInfo::new("test.pure", 1, 3, 1, 30),
        };
        assert_eq!(stereo.source_info().start_column, 3);
        assert_eq!(stereo.value, "businesstemporal");
    }

    #[test]
    fn test_tagged_value_is_spanned() {
        let tv = TaggedValue {
            tag: TagPtr {
                profile: profile_ref("doc"),
                value: SmolStr::new("description"),
                source_info: src(),
            },
            value: "A person".to_string(),
            multi_line: false,
            source_info: SourceInfo::new("test.pure", 2, 1, 2, 35),
        };
        assert_eq!(tv.source_info().start_line, 2);
        assert_eq!(tv.value, "A person");
        assert_eq!(tv.tag.value, "description");
    }

    #[test]
    fn test_parameter_is_spanned() {
        let param = Parameter {
            name: SmolStr::new("name"),
            type_ref: Some(TypeSpec::Type(TypeReference {
                package: None,
                name: SmolStr::new("String"),
                type_arguments: vec![],
                multiplicity_arguments: vec![],
                type_variable_values: vec![],
                source_info: src(),
            })),
            multiplicity: Some(Multiplicity::one()),
            source_info: SourceInfo::new("test.pure", 3, 5, 3, 20),
        };
        assert_eq!(param.source_info().start_line, 3);
        assert_eq!(param.name, "name");
    }

    #[test]
    fn test_spanned_string() {
        let ss = SpannedString {
            value: SmolStr::new("deprecated"),
            source_info: SourceInfo::new("test.pure", 4, 10, 4, 20),
        };
        assert_eq!(ss.source_info().start_column, 10);
        assert_eq!(ss.value, "deprecated");
    }
}
