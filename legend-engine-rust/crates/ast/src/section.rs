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

//! Source file, section, and import types.
//!
//! A Pure source file consists of one or more [`Section`]s, each with its own
//! import scope. The parser produces a [`SourceFile`] as its top-level output.
//!
//! ```text
//! ###Pure                              ← Section (kind="Pure")
//! import meta::pure::profiles::*;      ← ImportStatement
//! import model::domain::*;             ← ImportStatement
//!
//! Class model::domain::Person { ... }  ← Element
//!
//! ###Mapping                           ← Section (kind="Mapping")
//! import model::domain::*;
//! ...
//! ```

use crate::Spanned;
use crate::element::Element;
use crate::source_info::SourceInfo;
use crate::type_ref::{Identifier, Package};

/// The top-level parse result — a complete Pure source file.
///
/// Contains one or more sections, each with its own import scope and elements.
/// The parser produces a `SourceFile`; the protocol crate consumes it.
///
/// # Convenience
///
/// Use [`all_elements()`](Self::all_elements) to iterate all elements across
/// sections when import context is not needed.
#[derive(Debug, Clone, PartialEq, Spanned)]
pub struct SourceFile {
    /// The sections in this source file, in parse order.
    pub sections: Vec<Section>,
    /// Source location covering the entire file.
    pub source_info: SourceInfo,
}

impl SourceFile {
    /// Iterates all elements across all sections (flat).
    ///
    /// Use this when you don't need to know which section an element belongs to.
    pub fn all_elements(&self) -> impl Iterator<Item = &Element> {
        self.sections.iter().flat_map(|s| &s.elements)
    }

    /// Returns the total number of elements across all sections.
    #[must_use]
    pub fn element_count(&self) -> usize {
        self.sections.iter().map(|s| s.elements.len()).sum()
    }
}

/// A section within a Pure source file.
///
/// Each section has its own kind (Pure, Mapping, etc.), its own imports,
/// and its own elements. Imports within a section only apply to that section's
/// elements — they do not leak into other sections.
#[derive(Debug, Clone, PartialEq, Spanned)]
pub struct Section {
    /// The section grammar name as it appears after `###`.
    /// Common values: `"Pure"`, `"Mapping"`, `"Connection"`, `"Runtime"`.
    pub kind: Identifier,
    /// Import statements at the top of this section.
    pub imports: Vec<ImportStatement>,
    /// The elements defined in this section.
    pub elements: Vec<Element>,
    /// Source location covering the entire section.
    pub source_info: SourceInfo,
}

/// An import statement: `import meta::pure::profiles::*;`.
///
/// The `path` represents the package being imported. In Pure, all imports
/// are wildcard imports (`::*`).
#[derive(Debug, Clone, PartialEq, Spanned)]
pub struct ImportStatement {
    /// The package being imported (e.g., `meta::pure::profiles`).
    pub path: Package,
    /// Source location of the full import statement.
    pub source_info: SourceInfo,
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::annotation::SpannedString;
    use crate::element::{PackageableElement, ProfileDef};
    use crate::source_info::Spanned;
    use smol_str::SmolStr;

    use crate::test_utils::src;

    #[test]
    fn test_source_file_with_sections() {
        let file = SourceFile {
            sections: vec![
                Section {
                    kind: SmolStr::new("Pure"),
                    imports: vec![ImportStatement {
                        path: Package::root(SmolStr::new("meta"), src())
                            .child(SmolStr::new("pure"), src())
                            .child(SmolStr::new("profiles"), src()),
                        source_info: src(),
                    }],
                    elements: vec![Element::Profile(ProfileDef {
                        package: Some(Package::root(SmolStr::new("meta"), src())),
                        name: SpannedString {
                            value: SmolStr::new("doc"),
                            source_info: src(),
                        },
                        stereotype_names: vec![],
                        tag_names: vec![SpannedString {
                            value: SmolStr::new("description"),
                            source_info: src(),
                        }],
                        stereotypes: vec![],
                        tagged_values: vec![],
                        source_info: src(),
                    })],
                    source_info: src(),
                },
                Section {
                    kind: SmolStr::new("Mapping"),
                    imports: vec![],
                    elements: vec![],
                    source_info: src(),
                },
            ],
            source_info: src(),
        };

        assert_eq!(file.sections.len(), 2);
        assert_eq!(file.element_count(), 1);
        assert_eq!(file.sections[0].kind, "Pure");
        assert_eq!(file.sections[1].kind, "Mapping");

        // Convenience iterator
        let all: Vec<_> = file.all_elements().collect();
        assert_eq!(all.len(), 1);
        assert_eq!(all[0].name(), "doc");
    }

    #[test]
    fn test_section_kind_is_identifier() {
        let kind: Identifier = SmolStr::new("Mapping");
        assert_eq!(kind, "Mapping");
    }

    #[test]
    fn test_import_statement() {
        let import = ImportStatement {
            path: Package::root(SmolStr::new("model"), src()).child(SmolStr::new("domain"), src()),
            source_info: SourceInfo::new("test.pure", 2, 1, 2, 25),
        };
        assert_eq!(import.path.to_string(), "model::domain");
        assert_eq!(import.source_info().start_line, 2);
    }

    #[test]
    fn test_import_scope_per_section() {
        let file = SourceFile {
            sections: vec![
                Section {
                    kind: SmolStr::new("Pure"),
                    imports: vec![
                        ImportStatement {
                            path: Package::root(SmolStr::new("meta"), src())
                                .child(SmolStr::new("pure"), src()),
                            source_info: src(),
                        },
                        ImportStatement {
                            path: Package::root(SmolStr::new("model"), src()),
                            source_info: src(),
                        },
                    ],
                    elements: vec![],
                    source_info: src(),
                },
                Section {
                    kind: SmolStr::new("Pure"),
                    imports: vec![ImportStatement {
                        path: Package::root(SmolStr::new("other"), src()),
                        source_info: src(),
                    }],
                    elements: vec![],
                    source_info: src(),
                },
            ],
            source_info: src(),
        };

        // Each section has its own import scope
        assert_eq!(file.sections[0].imports.len(), 2);
        assert_eq!(file.sections[1].imports.len(), 1);
    }
}
