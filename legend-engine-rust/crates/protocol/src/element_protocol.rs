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

//! Plug-in trait for converting section-grammar (DSL) elements into
//! protocol JSON.
//!
//! Parser-side analog: `legend_pure_parser_parser::SectionParser`, which
//! produces [`DSLElement`]s for a `###Kind` section. This module is the
//! protocol-side half: the crate that owns a section grammar implements
//! [`ElementProtocol`] for each element kind it produces and
//! self-registers into [`ELEMENT_PROTOCOLS`]:
//!
//! ```ignore
//! use legend_pure_parser_protocol::element_protocol::{ElementProtocol, ELEMENT_PROTOCOLS};
//! use linkme::distributed_slice;
//!
//! #[distributed_slice(ELEMENT_PROTOCOLS)]
//! static DIAGRAM: &(dyn ElementProtocol + Send + Sync) = &DiagramProtocol;
//! ```
//!
//! The converted JSON is carried as
//! [`PackageableElement::Extension`](crate::v1::element::PackageableElement::Extension),
//! so it must be a complete protocol object including its `_type`. A DSL
//! element whose kind has no registered converter is a
//! [`ConvertError::Unsupported`].

use std::collections::HashSet;
use std::sync::OnceLock;

use legend_pure_parser_ast::dsl::DSLElement;
use linkme::distributed_slice;
use serde_json::value::RawValue;

pub use crate::error::{ConvertError, Result};

/// Converts one kind of [`DSLElement`] into its Protocol v1 JSON object.
pub trait ElementProtocol: Send + Sync {
    /// The [`DSLElement::kind`] this converter handles.
    fn kind(&self) -> &str;

    /// Converts `element` into its protocol JSON object, carrying its `_type`,
    /// written once as text (`docs/protocol-json.md`).
    ///
    /// # Errors
    ///
    /// [`ConvertError::Plugin`] when `element` is not the expected concrete
    /// type, or any nested conversion failure.
    fn convert(&self, element: &dyn DSLElement) -> Result<Box<RawValue>>;

    /// Writes [`ElementProtocol::convert`]'s JSON at the end of `out`, the
    /// model's JSON, rather than into a buffer of its own that is then
    /// copied there. The default copies `convert`'s; a converter of large
    /// elements writes them in place.
    ///
    /// # Errors
    ///
    /// As for [`ElementProtocol::convert`].
    fn convert_into(&self, element: &dyn DSLElement, out: &mut Vec<u8>) -> Result<()> {
        out.extend_from_slice(self.convert(element)?.get().as_bytes());
        Ok(())
    }
}

/// Distributed slice into which each section grammar crate registers its
/// [`ElementProtocol`]s. See the module docs for the registration pattern.
#[distributed_slice]
pub static ELEMENT_PROTOCOLS: [&'static (dyn ElementProtocol + Send + Sync)] = [..];

/// The registered [`ELEMENT_PROTOCOLS`], validated once per process.
///
/// # Panics
///
/// Panics when two registered converters share a `kind()`, which is a
/// link-time misconfiguration.
#[must_use]
pub fn discovered_element_protocols() -> &'static [&'static (dyn ElementProtocol + Send + Sync)] {
    static DISCOVERED: OnceLock<Vec<&'static (dyn ElementProtocol + Send + Sync)>> =
        OnceLock::new();
    DISCOVERED.get_or_init(|| validate_element_protocols(ELEMENT_PROTOCOLS.iter().copied()))
}

fn validate_element_protocols<I>(protocols: I) -> Vec<&'static (dyn ElementProtocol + Send + Sync)>
where
    I: IntoIterator<Item = &'static (dyn ElementProtocol + Send + Sync)>,
{
    let mut seen: HashSet<&'static str> = HashSet::new();
    let mut out = Vec::new();
    for p in protocols {
        let kind = p.kind();
        assert!(
            seen.insert(kind),
            "discovered_element_protocols: two element protocol converters registered for kind `{kind}`",
        );
        out.push(p);
    }
    out
}

/// Converts `element` with the registered converter for its kind.
///
/// # Errors
///
/// [`ConvertError::Unsupported`] when no linked crate registered a converter
/// for the element's kind; otherwise the converter's own error.
pub fn convert_dsl_element(element: &dyn DSLElement) -> Result<Box<RawValue>> {
    protocol_of(element)?.convert(element)
}

/// [`convert_dsl_element`], written at the end of `out`.
///
/// # Errors
///
/// As for [`convert_dsl_element`].
pub fn convert_dsl_element_into(element: &dyn DSLElement, out: &mut Vec<u8>) -> Result<()> {
    protocol_of(element)?.convert_into(element, out)
}

/// The registered converter for `element`'s kind.
fn protocol_of(element: &dyn DSLElement) -> Result<&'static (dyn ElementProtocol + Send + Sync)> {
    let kind = element.kind();
    discovered_element_protocols()
        .iter()
        .copied()
        .find(|p| p.kind() == kind)
        .ok_or_else(|| {
            ConvertError::unsupported(
                format!("no protocol converter is registered for element kind '{kind}'"),
                element.source_info(),
            )
        })
}

#[cfg(test)]
mod tests {
    use super::*;

    struct KindA;
    struct KindADup;

    impl ElementProtocol for KindA {
        fn kind(&self) -> &'static str {
            "kind-a"
        }
        fn convert(&self, _: &dyn DSLElement) -> Result<Box<RawValue>> {
            Ok(serde_json::value::to_raw_value(&serde_json::Value::Null)?)
        }
    }

    impl ElementProtocol for KindADup {
        fn kind(&self) -> &'static str {
            "kind-a"
        }
        fn convert(&self, _: &dyn DSLElement) -> Result<Box<RawValue>> {
            Ok(serde_json::value::to_raw_value(&serde_json::Value::Null)?)
        }
    }

    static KIND_A: KindA = KindA;
    static KIND_A_DUP: KindADup = KindADup;

    #[test]
    fn distinct_kinds_validate() {
        let out = validate_element_protocols([&KIND_A as &(dyn ElementProtocol + Send + Sync)]);
        assert_eq!(out.len(), 1);
    }

    #[test]
    #[should_panic(expected = "two element protocol converters registered for kind `kind-a`")]
    fn duplicate_kinds_panic() {
        let _ = validate_element_protocols([
            &KIND_A as &(dyn ElementProtocol + Send + Sync),
            &KIND_A_DUP as &(dyn ElementProtocol + Send + Sync),
        ]);
    }
}
