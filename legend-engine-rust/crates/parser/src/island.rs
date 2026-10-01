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

//! Island grammar parser plugin infrastructure.
//!
//! Each island grammar (graph fetch, path, SQL, etc.) provides an
//! [`IslandParser`] implementation that can parse the content between
//! `#tag{` and `}#` tokens. The main parser dispatches to the
//! appropriate plugin based on the tag.
//!
//! # Adding a New Island Grammar
//!
//! 1. Define your AST types implementing [`IslandContent`]
//! 2. Implement [`IslandParser`] for your grammar
//! 3. Register it via [`default_island_parsers`] or pass to `parse_with_islands()`
//!
//! # Thread-Safety Requirements
//!
//! **All island parser implementations must be `Send + Sync`.**
//!
//! The CLI parallelizes parsing across multiple files using Rayon
//! (`files.par_iter().map(|path| parse(...))`), which means the same
//! `IslandParser` instance may be invoked concurrently from different
//! threads. The `Send + Sync` supertraits on [`IslandParser`] enforce
//! this at compile time.
//!
//! In practice, this means your implementation:
//! - **Must not** use `Rc`, `Cell`, `RefCell`, or other non-thread-safe types
//! - **Must not** hold mutable state between `parse()` calls
//! - **Should** be stateless (zero-sized structs are ideal, like `GraphFetchIslandParser`)
//! - If state is required, use `Arc<Mutex<...>>` or atomics
//!
//! The same requirement applies to [`IslandContent`] (the AST output), which
//! also requires `Send + Sync`.
//!
//! [`IslandContent`]: legend_pure_parser_ast::island::IslandContent

use std::collections::HashSet;

use legend_pure_parser_ast::island::IslandContent;
use linkme::distributed_slice;

use crate::error::ParseError;
use crate::parser::ParserContext;

/// Trait for island grammar parsers.
///
/// Each island grammar provides a parser that reads the body between
/// `#tag{` and `}#` and produces a boxed [`IslandContent`].
///
/// The host lexes the body again at its place in the document and hands the
/// parser a cursor over those tokens alone, which it must consume whole. A
/// grammar that isn't made of Pure tokens reads the body as written instead
/// (see [`IslandParser::raw_body`]).
///
/// # Thread Safety
///
/// This trait requires `Send + Sync` because the CLI parallelizes file
/// parsing across threads. Implementations must be stateless or use
/// thread-safe interior mutability. The compiler will reject any
/// implementation that does not satisfy these bounds.
///
/// # Example
///
/// ```rust,ignore
/// use legend_pure_parser_parser::island::IslandParser;
///
/// // Good: zero-sized, stateless struct — trivially Send + Sync
/// struct MyIslandParser;
///
/// impl IslandParser for MyIslandParser {
///     fn tag(&self) -> &str { "myTag" }
///     fn parse(&self, ctx: &mut ParserContext<'_>) -> Result<Box<dyn IslandContent>, ParseError> {
///         // parse tokens from ctx...
///         # todo!()
///     }
/// }
/// ```
pub trait IslandParser: Send + Sync {
    /// The tag this parser handles.
    ///
    /// - `""` for `#{…}#` (graph fetch)
    /// - `">"` for `#>{…}#` (relation store accessor)
    /// - `"TDS"` for `#TDS{…}#` (TDS literal)
    fn tag(&self) -> &str;

    /// Whether this grammar reads its island's body as written
    /// ([`ParserContext::island_text`]) rather than as Pure tokens. The host
    /// then doesn't lex the body, so text that isn't Pure (CSV, SQL) is
    /// fine, and the parser's cursor is empty. Like the engine's
    /// `EmbeddedPureParser`, which receives the island's text.
    fn raw_body(&self) -> bool {
        false
    }

    /// Parse island content from the parser context.
    ///
    /// The `ParserContext` provides the island's span and text, the token
    /// cursor over its body, and expression parsing utilities.
    ///
    /// # Errors
    ///
    /// Returns `ParseError` if the island content is malformed.
    fn parse(&self, ctx: &mut ParserContext<'_>) -> Result<Box<dyn IslandContent>, ParseError>;
}

/// Returns the default set of built-in island parsers.
///
/// Empty by default — DSL crates that own island grammars (`dsl-graph`,
/// `dsl-store`, `dsl-tds`) export their own helpers (e.g.
/// `legend_pure_dsl_graph::parser::default_island_parsers`) which
/// callers concatenate into the slice they pass to
/// [`crate::parse_with_islands`]. Core carries no DSL-specific
/// island parsers.
#[must_use]
pub fn default_island_parsers() -> Vec<Box<dyn IslandParser>> {
    Vec::new()
}

/// Distributed slice into which each [`IslandParser`]-providing crate
/// registers its parser instance.
///
/// ```ignore
/// use legend_pure_parser_parser::island::{IslandParser, ISLAND_PARSERS};
/// use linkme::distributed_slice;
///
/// #[distributed_slice(ISLAND_PARSERS)]
/// static MY_ISLAND: &(dyn IslandParser + Send + Sync) = &MyIslandParser;
/// ```
///
/// The slice is consumed by [`discovered_island_parsers`] which
/// returns a `Vec<&'static dyn IslandParser>` ready to thread into
/// [`crate::parse`]. Discovery validates that no two registered
/// parsers share a `tag()`.
#[distributed_slice]
pub static ISLAND_PARSERS: [&'static (dyn IslandParser + Send + Sync)] = [..];

/// Discover and validate the [`ISLAND_PARSERS`] slice.
///
/// # Panics
///
/// Panics when two registered island parsers share a `tag()`. Each
/// tag must be globally unique because the main parser dispatches by
/// tag string match.
#[must_use]
pub fn discovered_island_parsers() -> Vec<&'static (dyn IslandParser + Send + Sync)> {
    validate_island_parsers(ISLAND_PARSERS.iter().copied())
}

/// Strict-validating helper factored out for unit-testing without
/// touching the global [`ISLAND_PARSERS`] slice.
#[must_use]
fn validate_island_parsers<I>(parsers: I) -> Vec<&'static (dyn IslandParser + Send + Sync)>
where
    I: IntoIterator<Item = &'static (dyn IslandParser + Send + Sync)>,
{
    let mut seen: HashSet<&'static str> = HashSet::new();
    let mut out: Vec<&'static (dyn IslandParser + Send + Sync)> = Vec::new();
    for p in parsers {
        let tag = p.tag();
        assert!(
            seen.insert(tag),
            "discovered_island_parsers: two island parsers registered for tag `{tag}`. \
             Each `#{tag}{{ … }}#` form must have exactly one parser; collision is a misconfiguration.",
        );
        out.push(p);
    }
    out
}

/// Owning wrapper that exposes a `&'static dyn IslandParser` through
/// the `Vec<Box<dyn IslandParser>>` plumbing the parser internals
/// require today. Internal bridge used by [`crate::parse`] to fold
/// the discovered slice into the existing
/// [`crate::Parser::with_plugins`] entry point without churning the
/// parser's storage representation.
pub(crate) struct StaticIslandParser(pub &'static (dyn IslandParser + Send + Sync));

impl IslandParser for StaticIslandParser {
    fn tag(&self) -> &str {
        self.0.tag()
    }
    fn raw_body(&self) -> bool {
        self.0.raw_body()
    }
    fn parse(&self, ctx: &mut ParserContext<'_>) -> Result<Box<dyn IslandContent>, ParseError> {
        self.0.parse(ctx)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::parser::ParserContext;

    struct TagA;
    struct TagB;
    struct TagADup;

    impl IslandParser for TagA {
        fn tag(&self) -> &'static str {
            "tag-a"
        }
        fn parse(
            &self,
            _ctx: &mut ParserContext<'_>,
        ) -> Result<Box<dyn IslandContent>, ParseError> {
            unimplemented!()
        }
    }
    impl IslandParser for TagB {
        fn tag(&self) -> &'static str {
            "tag-b"
        }
        fn parse(
            &self,
            _ctx: &mut ParserContext<'_>,
        ) -> Result<Box<dyn IslandContent>, ParseError> {
            unimplemented!()
        }
    }
    impl IslandParser for TagADup {
        fn tag(&self) -> &'static str {
            "tag-a"
        }
        fn parse(
            &self,
            _ctx: &mut ParserContext<'_>,
        ) -> Result<Box<dyn IslandContent>, ParseError> {
            unimplemented!()
        }
    }

    #[test]
    fn discovered_empty_slice_returns_empty_vec() {
        assert!(discovered_island_parsers().is_empty());
    }

    #[test]
    fn validate_accepts_unique_tags() {
        let parsers: [&'static (dyn IslandParser + Send + Sync); 2] = [&TagA, &TagB];
        let out = validate_island_parsers(parsers.iter().copied());
        assert_eq!(out.len(), 2);
    }

    #[test]
    #[should_panic(expected = "two island parsers registered for tag `tag-a`")]
    fn validate_panics_on_tag_collision() {
        let parsers: [&'static (dyn IslandParser + Send + Sync); 2] = [&TagA, &TagADup];
        let _ = validate_island_parsers(parsers.iter().copied());
    }
}
