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

//! Section grammar parser plug-in infrastructure.
//!
//! The Pure file format separates declarations into sections via
//! `###Identifier` headers (`###Pure`, `###Diagram`, `###Mapping`,
//! …). The default `Pure` section parses M3 elements (Class,
//! Function, Profile, …). Other section kinds belong to M2 DSLs and
//! aren't part of M3's grammar — each DSL crate registers a
//! [`SectionParser`] that the core parser dispatches to when it sees
//! the matching section header.
//!
//! This trait is the section-level analog of
//! [`IslandParser`](crate::island::IslandParser):
//!
//! | Scope | Trait | Body shape |
//! |---|---|---|
//! | Inline (`#tag{ … }#`) | `IslandParser` | `Box<dyn IslandContent>` |
//! | Section (`###Tag\n …`) | `SectionParser` | `Vec<Box<dyn DSLElement>>` |
//!
//! # Adding a new DSL section
//!
//! 1. Define your AST types implementing
//!    [`DSLElement`].
//! 2. Implement [`SectionParser`] for your DSL.
//! 3. Pass it via [`parse_with_sections`](crate::parse_with_sections).
//!
//! # Thread-safety
//!
//! Implementations must be `Send + Sync` (matching `IslandParser`)
//! because the CLI parallelises file parsing via Rayon.

use std::collections::HashSet;

use legend_pure_parser_ast::dsl::DSLElement;
use linkme::distributed_slice;

use crate::ParserContext;
use crate::error::ParseError;

/// Plug-in for parsing the body of a non-`Pure` section (e.g. `###Diagram`).
///
/// The core parser handles section detection, header parsing, and
/// import statements (for a grammar that has them, see
/// [`imports`](Self::imports)). Once the body begins, dispatch delegates to
/// a matching `SectionParser` if one is registered for the section
/// kind. The plug-in consumes tokens through the supplied
/// [`ParserContext`] up to the next section boundary or EOF and
/// returns the parsed [`DSLElement`]s.
///
/// `ParserContext` is the same handle that island parsers receive —
/// it gives both raw cursor access (`ctx.cursor()`) and high-level
/// helpers (`ctx.parse_expression()`, `ctx.parse_qualified_name()`,
/// …). DSLs whose body grammar embeds Pure expressions (Mapping's
/// filter and transform lambdas, future Function-DSL bodies) need
/// `parse_expression`; lower-level DSLs (Diagram) can ignore the
/// helpers and just use the cursor.
pub trait SectionParser: Send + Sync {
    /// Section header that this parser handles, without the leading
    /// `###`. For example, `"Diagram"` matches `###Diagram`.
    fn kind(&self) -> &str;

    /// Whether the section starts with `import` statements, which the core
    /// parser reads before handing the body over. The engine's section
    /// grammars mostly have them; one without them (`###Data`) reads
    /// `import` as its own text, which is then an error where the engine
    /// reports one, and the engine gives the section a `DefaultCodeSection`
    /// rather than an `ImportAwareCodeSection`.
    fn imports(&self) -> bool {
        true
    }

    /// Parse the body of a section of [`kind`](Self::kind). Consumes
    /// from `ctx.cursor()` up to (but not past) the next section
    /// header or EOF.
    ///
    /// Element-level errors are pushed onto `errors`; the parser is
    /// expected to recover and continue rather than abort the whole
    /// section.
    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>>;
}

/// Distributed slice into which each [`SectionParser`]-providing crate
/// registers its parser instance.
///
/// ```ignore
/// use legend_pure_parser_parser::section_parser::{SectionParser, SECTION_PARSERS};
/// use linkme::distributed_slice;
///
/// #[distributed_slice(SECTION_PARSERS)]
/// static MY_SECTION: &(dyn SectionParser + Send + Sync) = &MyDslSectionParser;
/// ```
///
/// The slice is consumed by [`discovered_section_parsers`] which
/// validates that no two registered parsers share a `kind()`.
#[distributed_slice]
pub static SECTION_PARSERS: [&'static (dyn SectionParser + Send + Sync)] = [..];

/// Discover and validate the [`SECTION_PARSERS`] slice.
///
/// # Panics
///
/// Panics when two registered section parsers share a `kind()`. Each
/// section header (e.g. `###Mapping`) must dispatch to exactly one
/// parser; collision is a misconfiguration.
#[must_use]
pub fn discovered_section_parsers() -> Vec<&'static (dyn SectionParser + Send + Sync)> {
    validate_section_parsers(SECTION_PARSERS.iter().copied())
}

/// Strict-validating helper factored out for unit-testing without
/// touching the global [`SECTION_PARSERS`] slice.
#[must_use]
fn validate_section_parsers<I>(parsers: I) -> Vec<&'static (dyn SectionParser + Send + Sync)>
where
    I: IntoIterator<Item = &'static (dyn SectionParser + Send + Sync)>,
{
    let mut seen: HashSet<&'static str> = HashSet::new();
    let mut out: Vec<&'static (dyn SectionParser + Send + Sync)> = Vec::new();
    for p in parsers {
        let kind = p.kind();
        assert!(
            seen.insert(kind),
            "discovered_section_parsers: two section parsers registered for kind `{kind}`. \
             Each `###{kind}` block must dispatch to exactly one parser; collision is a misconfiguration.",
        );
        out.push(p);
    }
    out
}

/// Owning wrapper that exposes a `&'static dyn SectionParser` through
/// the `Vec<Box<dyn SectionParser>>` plumbing the parser internals
/// require today. Internal bridge used by [`crate::parse`] to fold
/// the discovered slice into the existing
/// [`crate::Parser::with_plugins`] entry point without churning the
/// parser's storage representation.
pub(crate) struct StaticSectionParser(pub &'static (dyn SectionParser + Send + Sync));

impl SectionParser for StaticSectionParser {
    fn kind(&self) -> &str {
        self.0.kind()
    }
    fn imports(&self) -> bool {
        self.0.imports()
    }
    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        self.0.parse_body(ctx, errors)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    struct KindA;
    struct KindB;
    struct KindADup;

    impl SectionParser for KindA {
        fn kind(&self) -> &'static str {
            "KindA"
        }
        fn parse_body(
            &self,
            _ctx: &mut ParserContext<'_>,
            _errors: &mut Vec<ParseError>,
        ) -> Vec<Box<dyn DSLElement>> {
            Vec::new()
        }
    }
    impl SectionParser for KindB {
        fn kind(&self) -> &'static str {
            "KindB"
        }
        fn parse_body(
            &self,
            _ctx: &mut ParserContext<'_>,
            _errors: &mut Vec<ParseError>,
        ) -> Vec<Box<dyn DSLElement>> {
            Vec::new()
        }
    }
    impl SectionParser for KindADup {
        fn kind(&self) -> &'static str {
            "KindA"
        }
        fn parse_body(
            &self,
            _ctx: &mut ParserContext<'_>,
            _errors: &mut Vec<ParseError>,
        ) -> Vec<Box<dyn DSLElement>> {
            Vec::new()
        }
    }

    #[test]
    fn discovered_empty_slice_returns_empty_vec() {
        assert!(discovered_section_parsers().is_empty());
    }

    #[test]
    fn validate_accepts_unique_kinds() {
        let parsers: [&'static (dyn SectionParser + Send + Sync); 2] = [&KindA, &KindB];
        let out = validate_section_parsers(parsers.iter().copied());
        assert_eq!(out.len(), 2);
    }

    #[test]
    #[should_panic(expected = "two section parsers registered for kind `KindA`")]
    fn validate_panics_on_kind_collision() {
        let parsers: [&'static (dyn SectionParser + Send + Sync); 2] = [&KindA, &KindADup];
        let _ = validate_section_parsers(parsers.iter().copied());
    }
}
