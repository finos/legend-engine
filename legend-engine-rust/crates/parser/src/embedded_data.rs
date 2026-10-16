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

//! Embedded data grammar plug-ins: `Type #{ … }#` in a store's test data,
//! the engine's `EmbeddedDataParser` extensions.
//!
//! The engine core's types ([`CORE_TYPES`]) are built in. A crate that owns
//! another type's grammar (`dsl-relational`'s `Relational`) implements
//! [`EmbeddedDataParser`] and registers it into [`EMBEDDED_DATA_PARSERS`];
//! its protocol half registers alongside, in the protocol crate's
//! `EMBEDDED_DATA_PROTOCOLS`:
//!
//! ```ignore
//! use legend_pure_parser_parser::embedded_data::{EmbeddedDataParser, EMBEDDED_DATA_PARSERS};
//! use linkme::distributed_slice;
//!
//! #[distributed_slice(EMBEDDED_DATA_PARSERS)]
//! static MY_DATA: &(dyn EmbeddedDataParser + Send + Sync) = &MyDataParser;
//! ```
//!
//! Data of a type no linked grammar reads is unsupported: the engine may
//! read it, and parses such a section itself.

use std::collections::HashSet;
use std::sync::OnceLock;

use legend_pure_parser_ast::SourceInfo;
use legend_pure_parser_ast::data::EmbeddedData;
use linkme::distributed_slice;

use crate::error::ParseError;
use crate::parser::ParserContext;
use crate::parser::strings::string_value;

/// The engine core's external format data type: text in a content type.
pub const EXTERNAL_FORMAT: &str = "ExternalFormat";

/// The engine core's model store data type: instances per class.
pub const MODEL_STORE: &str = "ModelStore";

/// The engine core's reference data type: a data element's data.
pub const REFERENCE: &str = "Reference";

/// The engine core's relation data type: tables of text cells.
pub const RELATION: &str = "Relation";

/// The engine core's data types, which the parser reads itself.
pub const CORE_TYPES: [&str; 4] = [EXTERNAL_FORMAT, MODEL_STORE, REFERENCE, RELATION];

/// A grammar for embedded data of one type.
///
/// Stateless, like [`IslandParser`](crate::IslandParser): the same instance
/// may parse on several threads at once.
pub trait EmbeddedDataParser: Send + Sync {
    /// The data type this grammar reads, e.g. `Relational`.
    fn type_name(&self) -> &str;

    /// Whether the grammar reads the body as written
    /// ([`ParserContext::island_text`]) rather than as Pure tokens; see
    /// [`IslandParser::raw_body`](crate::IslandParser::raw_body).
    fn raw_body(&self) -> bool {
        false
    }

    /// Parses the body. The context's
    /// [`island_span`](ParserContext::island_span) is the whole embedded
    /// data, from its type to `}#`, and its cursor holds the body's tokens,
    /// lexed at their place in the document, which the grammar must consume
    /// whole.
    ///
    /// The data is usually the grammar's own
    /// ([`EmbeddedData::Extension`]), but may be of a core kind, as the
    /// engine's parsers may return a core class: the data space's
    /// `DataspaceTestData` is a [`EmbeddedData::Reference`].
    ///
    /// # Errors
    ///
    /// Returns `ParseError` if the body is malformed.
    fn parse(&self, ctx: &mut ParserContext<'_>) -> Result<EmbeddedData, ParseError>;
}

/// Distributed slice into which each embedded data grammar crate registers
/// its [`EmbeddedDataParser`]. See the module docs.
#[distributed_slice]
pub static EMBEDDED_DATA_PARSERS: [&'static (dyn EmbeddedDataParser + Send + Sync)] = [..];

/// The registered [`EMBEDDED_DATA_PARSERS`], validated once per process.
///
/// # Panics
///
/// Panics when two registered grammars share a type, or one claims a
/// built-in type ([`CORE_TYPES`]): each type must have exactly one grammar,
/// so a collision is a link-time misconfiguration.
#[must_use]
pub fn discovered_embedded_data_parsers()
-> &'static [&'static (dyn EmbeddedDataParser + Send + Sync)] {
    static DISCOVERED: OnceLock<Vec<&'static (dyn EmbeddedDataParser + Send + Sync)>> =
        OnceLock::new();
    DISCOVERED.get_or_init(|| {
        let mut seen: HashSet<&'static str> = HashSet::from(CORE_TYPES);
        let mut parsers = Vec::new();
        for parser in EMBEDDED_DATA_PARSERS.iter().copied() {
            let type_name = parser.type_name();
            assert!(
                seen.insert(type_name),
                "two embedded data grammars are registered for type `{type_name}`"
            );
            parsers.push(parser);
        }
        parsers
    })
}

/// The data types this build reads: the built-in ones and the linked ones.
#[must_use]
pub fn linked_types() -> Vec<&'static str> {
    let mut types: Vec<&'static str> = CORE_TYPES
        .into_iter()
        .chain(
            discovered_embedded_data_parsers()
                .iter()
                .map(|p| p.type_name()),
        )
        .collect();
    types.sort_unstable();
    types
}

/// A reference's element path, as the engine's reference grammars read it
/// (the core's `ReferenceEmbeddedDataParser`, and plug-ins like the data
/// space's `DataspaceDataElementReferenceParser`): the body's text trimmed,
/// then `PureGrammarParserUtility.validatePath`, each part an identifier or
/// a quoted name. `reference` names the reference in the error a blank body
/// gives. Errors are reported at `span`, the whole embedded data.
///
/// # Errors
///
/// Returns `ParseError` if the body is blank or a part is not a name.
pub fn element_path(text: &str, reference: &str, span: &SourceInfo) -> Result<String, ParseError> {
    // Java's `String.trim()`.
    let path = text.trim_matches(|c: char| c <= ' ');
    if path.is_empty() {
        return Err(ParseError::unexpected(
            format!("Path should be provided for {reference}"),
            span.clone(),
        ));
    }
    let invalid =
        |part: &str| ParseError::unexpected(format!("Invalid identifier: {part}"), span.clone());
    let mut parts = Vec::new();
    for part in path.split("::") {
        if part.starts_with('\'') {
            if !part.ends_with('\'') {
                return Err(invalid(part));
            }
            let (value, _) = string_value(part).map_err(|_| invalid(part))?;
            parts.push(value);
        } else if is_valid_identifier(part) {
            parts.push(part.to_string());
        } else {
            return Err(invalid(part));
        }
    }
    Ok(parts.join("::"))
}

/// The engine's `VALID_STRING_PATTERN`: `[A-Za-z0-9_][A-Za-z0-9_$]*`.
fn is_valid_identifier(text: &str) -> bool {
    let mut chars = text.chars();
    chars
        .next()
        .is_some_and(|c| c.is_ascii_alphanumeric() || c == '_')
        && chars.all(|c| c.is_ascii_alphanumeric() || c == '_' || c == '$')
}
