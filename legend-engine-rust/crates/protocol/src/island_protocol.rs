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

//! Plug-in trait for converting island AST content into protocol JSON.
//!
//! Parser-side analog: `legend_pure_parser_parser::IslandParser`. This
//! module is the protocol-side half of the pair: an island grammar
//! registers both under the same tag.
//!
//! Each DSL crate that owns an island grammar (`dsl-graph`,
//! `dsl-store`, `dsl-tds`) implements [`IslandProtocol`] keyed on
//! the same tag its `IslandParser` uses, converts the typed
//! [`IslandContent`] into the protocol's [`ValueSpecification`], and
//! self-registers into [`ISLAND_PROTOCOLS`]:
//!
//! ```ignore
//! use legend_pure_parser_protocol::island_protocol::{IslandProtocol, ISLAND_PROTOCOLS};
//! use linkme::distributed_slice;
//!
//! #[distributed_slice(ISLAND_PROTOCOLS)]
//! static MY_ISLAND: &(dyn IslandProtocol + Send + Sync) = &MyIslandProtocol;
//! ```
//!
//! AST → protocol conversion (`v1::convert`) routes every island
//! through [`discovered_island_protocols`]; an island whose tag has no
//! registered converter is a [`ConvertError::Unsupported`].

use std::collections::HashSet;
use std::sync::OnceLock;

use legend_pure_parser_ast::island::{IslandContent, IslandExpression};
use linkme::distributed_slice;

pub use crate::error::{ConvertError, Result};
use crate::v1::value_spec::ValueSpecification;

/// Plug-in for converting one island grammar's [`IslandContent`]
/// into a protocol-side [`ValueSpecification`].
///
/// Implementations live in DSL crates next to that DSL's
/// `IslandParser` and `IslandComposer` impls. The `tag()` value
/// must match the tag the parser and composer use.
///
/// # Example
///
/// ```rust,ignore
/// pub struct GraphFetchProtocol;
/// impl IslandProtocol for GraphFetchProtocol {
///     fn tag(&self) -> &str { "" }
///     fn convert(&self, content: &dyn IslandContent) -> Result<ValueSpecification> {
///         let tree = content
///             .as_any()
///             .downcast_ref::<RootGraphFetchTree>()
///             .ok_or_else(|| /* error */)?;
///         /* convert tree to ValueSpecification */
///     }
/// }
/// ```
pub trait IslandProtocol: Send + Sync {
    /// The island tag this converter handles. Same string the
    /// matching `IslandParser::tag()` and `IslandComposer::tag()`
    /// return — `""` for graph fetch, `">"` for
    /// `RelationStoreAccessor`, `"TDS"` for TDS, etc.
    fn tag(&self) -> &str;

    /// Convert one [`IslandContent`] (already downcast-ready via
    /// [`IslandContent::as_any`]) into a protocol value
    /// specification. Implementations downcast to their concrete
    /// content type and emit the corresponding protocol JSON shape.
    ///
    /// # Errors
    /// Returns [`ConvertError::Plugin`] on a downcast mismatch, or
    /// propagates a nested expression conversion failure.
    fn convert(&self, content: &dyn IslandContent) -> Result<ValueSpecification>;
}

/// Look up an [`IslandProtocol`] for `island.tag()` in `protocols`
/// and convert.
///
/// Returns `Ok(Some(value))` if a matching converter was found and
/// produced a value, `Ok(None)` if no converter matches (caller
/// decides whether to fall back to a built-in path), or `Err` if a
/// matching converter ran but failed.
///
/// # Errors
/// Propagates the matching converter's error verbatim.
pub fn dispatch_island_convert(
    island: &IslandExpression,
    protocols: &[&dyn IslandProtocol],
) -> Result<Option<ValueSpecification>> {
    let tag = island.tag();
    for p in protocols {
        if p.tag() == tag {
            return p.convert(island.content.as_ref()).map(Some);
        }
    }
    Ok(None)
}

/// Distributed slice into which each island grammar crate registers its
/// [`IslandProtocol`]. See the module docs for the registration pattern.
#[distributed_slice]
pub static ISLAND_PROTOCOLS: [&'static (dyn IslandProtocol + Send + Sync)] = [..];

/// The registered [`ISLAND_PROTOCOLS`], validated once per process.
///
/// # Panics
///
/// Panics when two registered converters share a `tag()`: each island tag
/// must map to exactly one converter, so a collision is a link-time
/// misconfiguration.
#[must_use]
pub fn discovered_island_protocols() -> &'static [&'static (dyn IslandProtocol + Send + Sync)] {
    static DISCOVERED: OnceLock<Vec<&'static (dyn IslandProtocol + Send + Sync)>> = OnceLock::new();
    DISCOVERED.get_or_init(|| validate_island_protocols(ISLAND_PROTOCOLS.iter().copied()))
}

fn validate_island_protocols<I>(protocols: I) -> Vec<&'static (dyn IslandProtocol + Send + Sync)>
where
    I: IntoIterator<Item = &'static (dyn IslandProtocol + Send + Sync)>,
{
    let mut seen: HashSet<&'static str> = HashSet::new();
    let mut out = Vec::new();
    for p in protocols {
        let tag = p.tag();
        assert!(
            seen.insert(tag),
            "discovered_island_protocols: two island protocol converters registered for tag `{tag}`",
        );
        out.push(p);
    }
    out
}

/// Converts `island` with the registered converter for its tag.
///
/// # Errors
///
/// [`ConvertError::Unsupported`] when no linked crate registered a
/// converter for the island's tag; otherwise the converter's own error.
pub fn convert_island(island: &IslandExpression) -> Result<ValueSpecification> {
    let protocols: Vec<&dyn IslandProtocol> = discovered_island_protocols()
        .iter()
        .map(|p| *p as &dyn IslandProtocol)
        .collect();
    dispatch_island_convert(island, &protocols)?.ok_or_else(|| {
        ConvertError::unsupported(
            format!(
                "no protocol converter is registered for island tag '{}'",
                island.tag()
            ),
            &island.source_info,
        )
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    struct TagA;
    struct TagADup;

    impl IslandProtocol for TagA {
        fn tag(&self) -> &'static str {
            "tag-a"
        }
        fn convert(&self, _: &dyn IslandContent) -> Result<ValueSpecification> {
            Err(ConvertError::plugin("unused", None))
        }
    }

    impl IslandProtocol for TagADup {
        fn tag(&self) -> &'static str {
            "tag-a"
        }
        fn convert(&self, _: &dyn IslandContent) -> Result<ValueSpecification> {
            Err(ConvertError::plugin("unused", None))
        }
    }

    static TAG_A: TagA = TagA;
    static TAG_A_DUP: TagADup = TagADup;

    #[test]
    fn distinct_tags_validate() {
        let out = validate_island_protocols([&TAG_A as &(dyn IslandProtocol + Send + Sync)]);
        assert_eq!(out.len(), 1);
    }

    #[test]
    #[should_panic(expected = "two island protocol converters registered for tag `tag-a`")]
    fn duplicate_tags_panic() {
        let _ = validate_island_protocols([
            &TAG_A as &(dyn IslandProtocol + Send + Sync),
            &TAG_A_DUP as &(dyn IslandProtocol + Send + Sync),
        ]);
    }
}
