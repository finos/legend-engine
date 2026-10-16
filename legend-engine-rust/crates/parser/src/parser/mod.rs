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

//! Recursive descent parser for the Pure grammar.
//!
//! This parser is strictly responsible for **syntax analysis** — converting a
//! token stream into an AST. It does not perform semantic validation (e.g., type
//! checking, name resolution, or structural constraints on graph fetch trees).
//! See `docs/SEMANTIC_VALIDATIONS.md` for deferred validations.

use legend_pure_parser_ast::annotation::{Parameter, SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::element::{Constraint, Element};
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::section::{ImportStatement, Section, SourceFile};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::{Multiplicity, Package, TypeReference};
use legend_pure_parser_lexer::TokenKind;
use smol_str::SmolStr;

mod annotation;
mod association;
pub mod block;
mod class;
mod data;
mod enum_def;
mod expression;
mod function;
pub mod helpers;
mod measure;
mod primitive;
mod primitive_value;
mod profile;
mod relation_data;
pub mod strings;
pub mod test_assertion;
mod type_ref;

pub(crate) use helpers::{is_wildcard_ahead, split_package_name, unquote_string};

use crate::cursor::Cursor;
use crate::error::ParseError;
use crate::island::IslandParser;

type R<T> = Result<T, ParseError>;

/// The common prefix of every packageable element definition:
///
/// ```text
/// KEYWORD (<<stereotypes>> | {tagged_values})* pkg::Name
/// ```
///
/// Stereotypes and tagged values may appear in any order and repeat.
pub(crate) struct ElementHeader {
    /// Stereotypes applied to the element.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values applied to the element.
    pub tagged_values: Vec<TaggedValue>,
    /// The package (if qualified), e.g. `meta::pure::profiles` → `Some(...)`.
    pub package: Option<Package>,
    /// The element name with its exact token source location.
    ///
    /// Using [`SpannedString`] rather than separate `name: SmolStr` +
    /// `name_source_info: SourceInfo` fields ensures the position can never
    /// be computed and then silently dropped — both travel together.
    pub name: SpannedString,
}

/// Main parser struct wrapping a token cursor and grammar plug-ins.
pub(crate) struct Parser {
    cursor: Cursor,
    island_parsers: Vec<Box<dyn IslandParser>>,
    section_parsers: Vec<Box<dyn crate::SectionParser>>,
    /// A documentation literal read before an element keyword, folded into
    /// that element's tagged values by `parse_element_header`.
    pending_documentation: Option<legend_pure_parser_lexer::Token>,
    /// The first error a plug-in grammar's walker checks found, reported
    /// once the grammar has read its whole unit (see
    /// [`ParserContext::defer`]).
    deferred: Option<ParseError>,
    /// The offset the text was lexed with (see
    /// [`ParserContext::source_offset`]).
    pub(crate) offset: legend_pure_parser_ast::SourceOffset,
    /// The section type of the body the text is, if it is one, without its
    /// header (see [`crate::parse_section_body`]); otherwise the text is a
    /// file of sections.
    pub(crate) section_body: Option<SmolStr>,
}

impl Parser {
    /// Create a parser with the default set of island grammar plug-ins
    /// and no section parsers (only `Pure` sections recognised).
    pub fn new(cursor: Cursor) -> Self {
        Self {
            cursor,
            island_parsers: crate::island::default_island_parsers(),
            section_parsers: Vec::new(),
            pending_documentation: None,
            deferred: None,
            offset: legend_pure_parser_ast::SourceOffset::default(),
            section_body: None,
        }
    }

    /// Create a parser with a custom set of island grammar plug-ins.
    pub fn with_island_parsers(cursor: Cursor, island_parsers: Vec<Box<dyn IslandParser>>) -> Self {
        Self {
            cursor,
            island_parsers,
            section_parsers: Vec::new(),
            pending_documentation: None,
            deferred: None,
            offset: legend_pure_parser_ast::SourceOffset::default(),
            section_body: None,
        }
    }

    /// Create a parser with both island and section grammar plug-ins.
    pub fn with_plugins(
        cursor: Cursor,
        island_parsers: Vec<Box<dyn IslandParser>>,
        section_parsers: Vec<Box<dyn crate::SectionParser>>,
    ) -> Self {
        Self {
            cursor,
            island_parsers,
            section_parsers,
            pending_documentation: None,
            deferred: None,
            offset: legend_pure_parser_ast::SourceOffset::default(),
            section_body: None,
        }
    }

    // ── Top-level ───────────────────────────────────────────────────────

    /// Parse the entire source file with element-level error recovery.
    ///
    /// Returns `Ok(SourceFile)` if all elements parse successfully, or
    /// `Err(PartialSourceFile)` with the valid elements and collected
    /// errors if any elements fail.
    pub fn parse_source_file(&mut self) -> Result<SourceFile, crate::PartialSourceFile> {
        let start = self.cursor.current_source_info();
        let mut sections = Vec::new();
        let mut errors = Vec::new();

        if let Some(kind) = self.section_body.clone() {
            // One section, to the end of the text: its grammar reads it all,
            // or reports where it stops.
            sections.push(self.parse_section_of(start.clone(), kind, &mut errors));
            if errors.is_empty() && !self.cursor.check(TokenKind::Eof) {
                let token = self.cursor.peek().clone();
                errors.push(ParseError::unexpected(
                    format!("Unexpected token '{}'", token.text),
                    token.source_info,
                ));
            }
        }
        while self.section_body.is_none() && !self.cursor.check(TokenKind::Eof) {
            sections.push(self.parse_section(&mut errors));
        }

        if sections.is_empty() {
            sections.push(Section {
                kind: SmolStr::new("Pure"),
                imports: vec![],
                elements: vec![],
                source_info: start.clone(),
            });
        }

        let source_file = SourceFile {
            sections,
            source_info: start,
        };

        if errors.is_empty() {
            Ok(source_file)
        } else {
            Err(crate::PartialSourceFile {
                source_file,
                errors,
            })
        }
    }

    /// Parse a section with element-level error recovery.
    ///
    /// When an element fails to parse, the error is collected and the cursor
    /// skips to the next element boundary (a top-level keyword like `function`,
    /// `class`, `native`, etc.).
    ///
    /// If a registered [`SectionParser`](crate::SectionParser) matches the
    /// section header (e.g. `###Diagram`), the body is delegated to that
    /// plug-in and the resulting `DSLElement`s are wrapped as
    /// `Element::DSLElement` entries. The default `Pure` section continues
    /// to use the M3 element grammar.
    fn parse_section(&mut self, errors: &mut Vec<ParseError>) -> Section {
        let start = self.cursor.current_source_info();
        let kind = if self.cursor.check(TokenKind::SectionHeader) {
            let tok = self.cursor.advance().clone();
            SmolStr::new(tok.text.trim_start_matches('#'))
        } else {
            SmolStr::new("Pure")
        };
        self.parse_section_of(start, kind, errors)
    }

    /// A section of type `kind`, its header (if any) read, from `start`.
    fn parse_section_of(
        &mut self,
        start: SourceInfo,
        kind: SmolStr,
        errors: &mut Vec<ParseError>,
    ) -> Section {
        let plug_in = self
            .section_parsers
            .iter()
            .position(|p| p.kind() == kind.as_str());
        // Imports still fail-fast — a malformed import is unusual and
        // likely indicates a fundamentally broken file structure. A
        // section grammar without imports reads `import` itself.
        let has_imports = plug_in.is_none_or(|idx| self.section_parsers[idx].imports());
        let mut imports = Vec::new();
        while has_imports && self.cursor.check(TokenKind::Import) {
            match self.parse_import() {
                Ok(imp) => imports.push(imp),
                Err(e) => {
                    errors.push(e);
                    self.skip_to_next_element();
                }
            }
        }

        // Section-level plug-in dispatch: if a `SectionParser` is
        // registered for this section's kind, hand the cursor over.
        // The plug-in consumes tokens up to the next section header
        // / EOF and returns its own DSL elements.
        if let Some(idx) = plug_in {
            // Take the parser out of the Vec to avoid an aliasing
            // borrow on `self` while constructing the ParserContext.
            let plug_in = self.section_parsers.swap_remove(idx);
            let dsl_elements = {
                let mut ctx = ParserContext {
                    parser: self,
                    island: None,
                };
                plug_in.parse_body(&mut ctx, errors)
            };
            // The walker's errors come after the grammar's.
            if let Some(error) = self.deferred.take() {
                errors.push(error);
            }
            // Restore the parser; order doesn't matter since lookup
            // is by kind, not index.
            self.section_parsers.push(plug_in);
            let elements = dsl_elements
                .into_iter()
                .map(legend_pure_parser_ast::element::Element::DSLElement)
                .collect();
            return Section {
                kind,
                imports,
                elements,
                source_info: start,
            };
        }

        let mut elements = Vec::new();
        while !self.cursor.check(TokenKind::SectionHeader) && !self.cursor.check(TokenKind::Eof) {
            match self.parse_element() {
                Ok(elem) => elements.push(elem),
                Err(e) => {
                    errors.push(e);
                    self.skip_to_next_element();
                }
            }
        }

        Section {
            kind,
            imports,
            elements,
            source_info: start,
        }
    }

    /// Skip tokens until the cursor reaches a token that starts a new
    /// top-level element or the end of the section/file.
    ///
    /// Element boundary tokens: `function`, `native`, `class`, `enum`,
    /// `profile`, `association`, `measure`, section headers, and EOF.
    fn skip_to_next_element(&mut self) {
        loop {
            match self.cursor.peek_kind() {
                // These tokens can start a new top-level element
                TokenKind::Function
                | TokenKind::Native
                | TokenKind::Class
                | TokenKind::Enum
                | TokenKind::Profile
                | TokenKind::Association
                | TokenKind::Measure
                | TokenKind::SectionHeader
                | TokenKind::Eof => break,
                _ => {
                    self.cursor.advance();
                }
            }
        }
    }

    pub(crate) fn parse_import(&mut self) -> R<ImportStatement> {
        let start = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::Import)?;
        let path = self.parse_package_path()?;
        // Consume ::* if present
        if self.cursor.eat(TokenKind::PathSep) {
            self.cursor.expect(TokenKind::Star)?;
        }
        self.cursor.expect(TokenKind::Semicolon)?;
        Ok(ImportStatement {
            path,
            source_info: start,
        })
    }

    pub(crate) fn parse_element(&mut self) -> R<Element> {
        // Documentation precedes the element keyword (classes, associations,
        // enumerations and functions, as in the engine grammar).
        self.pending_documentation = None;
        if self.cursor.check(TokenKind::StringLiteral)
            && matches!(
                self.cursor.peek_kind_at(1),
                TokenKind::Class | TokenKind::Association | TokenKind::Enum | TokenKind::Function
            )
        {
            self.pending_documentation = Some(self.cursor.advance().clone());
        }
        let documentation_start = self
            .pending_documentation
            .as_ref()
            .map(|token| token.source_info.clone());
        let mut element = match self.cursor.peek_kind() {
            TokenKind::Profile => self.parse_profile(),
            TokenKind::Enum => self.parse_enum(),
            TokenKind::Class => self.parse_class(),
            TokenKind::Association => self.parse_association(),
            TokenKind::Measure => self.parse_measure(),
            TokenKind::Primitive => self.parse_primitive_def(),
            TokenKind::Function => self.parse_function(),
            TokenKind::Native => self.parse_native_function(),
            _ => Err(ParseError::unexpected(
                format!("Unexpected token {}", self.cursor.peek().text),
                self.cursor.current_source_info(),
            )),
        }?;
        // The element's rule starts at its documentation.
        if let Some(start) = documentation_start {
            element.widen_start_to(&start);
        }
        Ok(element)
    }

    // ── Package path: my::pkg::Name ─────────────────────────────────────

    pub(crate) fn parse_package_path(&mut self) -> R<Package> {
        // Handle leading :: for root-qualified paths (e.g., ::meta::pure)
        // or standalone :: as a reference to the root package itself
        if self.cursor.check(TokenKind::PathSep) {
            self.cursor.advance();
            // If no identifier follows (e.g., `elementToPath(::)`), return root package
            if !self.cursor.peek_kind().is_identifier_like() {
                return Ok(Package::root(
                    SmolStr::new_static(""),
                    self.cursor.current_source_info(),
                ));
            }
        }
        let (name, si) = self.cursor.expect_identifier_or_keyword()?;
        let mut pkg = Package::root(name, si);
        while self.cursor.check(TokenKind::PathSep) && !is_wildcard_ahead(&self.cursor) {
            self.cursor.advance();
            let (seg, si) = self.cursor.expect_identifier_or_keyword()?;
            pkg = pkg.child(seg, si);
        }
        Ok(pkg)
    }

    /// Parse a qualified name, returning `(package, name, name_source_info)`.
    ///
    /// `name_source_info` points at the **last** segment — the simple name
    /// itself, not the full FQN span. Callers that need the full span can
    /// reconstruct it from the package's first-segment span; the name span
    /// is for `SourceInformation.line`/`column` parity with Java Pure.
    pub(crate) fn parse_qualified_name(&mut self) -> R<(Option<Package>, SmolStr, SourceInfo)> {
        let (first, first_si) = self.cursor.expect_identifier_or_keyword()?;
        if !self.cursor.check(TokenKind::PathSep) {
            return Ok((None, first, first_si));
        }
        let mut pkg = Package::root(first, first_si);
        while self.cursor.eat(TokenKind::PathSep) {
            let (seg, si) = self.cursor.expect_identifier_or_keyword()?;
            if self.cursor.check(TokenKind::PathSep) {
                pkg = pkg.child(seg, si);
            } else {
                return Ok((Some(pkg), seg, si));
            }
        }
        // Last segment in package is actually the name — shouldn't happen
        // since the inner return covers the has-trailing-segment case, but
        // preserve graceful handling for malformed input.
        let name = SmolStr::new(pkg.name());
        let si = legend_pure_parser_ast::source_info::Spanned::source_info(&pkg).clone();
        Ok((pkg.parent().cloned(), name, si))
    }

    // ── Documentation: '''…''' before a documentable construct ───────────

    /// Whether the current token is a documentation literal for a member
    /// (property, qualified property): a string that is not itself the
    /// member's quoted name.
    pub(crate) fn at_member_documentation(&self) -> bool {
        self.cursor.check(TokenKind::StringLiteral)
            && !matches!(
                self.cursor.peek_kind_at(1),
                TokenKind::Colon | TokenKind::LParen
            )
    }

    /// `item (',' item)*`, or nothing when `close` comes first. `close` is
    /// left for the caller, so a missing separator is reported there, at the
    /// token that should have been `,` or `close`.
    pub(crate) fn parse_separated<T>(
        &mut self,
        close: TokenKind,
        mut item: impl FnMut(&mut Self) -> R<T>,
    ) -> R<Vec<T>> {
        let mut items = Vec::new();
        if self.cursor.check(close) {
            return Ok(items);
        }
        loop {
            items.push(item(self)?);
            if !self.cursor.eat(TokenKind::Comma) {
                return Ok(items);
            }
        }
    }

    /// Whether the current token is a documentation literal for an enum
    /// value: a string that is not itself the value's quoted name.
    pub(crate) fn at_enum_value_documentation(&self) -> bool {
        self.cursor.check(TokenKind::StringLiteral)
            && !matches!(
                self.cursor.peek_kind_at(1),
                TokenKind::Comma | TokenKind::RBrace
            )
    }

    /// Folds a documentation literal into `tagged_values`, as the engine's
    /// `PureGrammarParserUtility.taggedValuesWithDocumentation` does: it is
    /// sugar for a leading `meta::pure::profiles::doc.doc` tagged value
    /// (written as a text block), positioned on the literal.
    pub(crate) fn with_documentation(
        documentation: Option<legend_pure_parser_lexer::Token>,
        mut tagged_values: Vec<TaggedValue>,
    ) -> R<Vec<TaggedValue>> {
        let Some(token) = documentation else {
            return Ok(tagged_values);
        };
        if !strings::is_text_block(&token.text) {
            return Err(ParseError::unexpected(
                "Documentation must be written as a multi-line ('''...''') literal",
                token.source_info,
            ));
        }
        if let Some(conflict) = tagged_values.iter().find(|tv| is_doc_tag(&tv.tag)) {
            return Err(ParseError::unexpected(
                "Element has both documentation and an explicit doc.doc tagged value. Use one.",
                conflict.source_info.clone(),
            ));
        }
        // Token spans end at start + length (ANTLR); a documentation literal
        // always spans lines, so its end is computed from the text, as the
        // engine's `documentationSourceInformation` does.
        let newlines = u32::try_from(token.text.matches('\n').count()).unwrap_or(u32::MAX);
        let last_line = token.text.rsplit('\n').next().unwrap_or("");
        let si = SourceInfo {
            end_line: token.source_info.start_line.saturating_add(newlines),
            end_column: u32::try_from(last_line.encode_utf16().count()).unwrap_or(u32::MAX),
            ..token.source_info
        };
        let profile_package = Package::root(SmolStr::new_static("meta"), si.clone())
            .child(SmolStr::new_static("pure"), si.clone())
            .child(SmolStr::new_static("profiles"), si.clone());
        tagged_values.insert(
            0,
            TaggedValue {
                tag: legend_pure_parser_ast::annotation::TagPtr {
                    profile: legend_pure_parser_ast::annotation::PackageableElementPtr {
                        package: Some(profile_package),
                        name: SmolStr::new_static("doc"),
                        source_info: si.clone(),
                    },
                    value: SmolStr::new_static("doc"),
                    source_info: si.clone(),
                },
                value: strings::documentation_value(&token.text),
                multi_line: true,
                source_info: si,
            },
        );
        Ok(tagged_values)
    }

    /// Parses the common element header: `(<<stereotypes>> | {tagged_values})* pkg::Name`.
    ///
    /// This is the shared prefix for all packageable element definitions.
    /// Stereotypes and tagged values may appear in any order and can repeat.
    /// The element keyword must have already been consumed before calling this.
    pub(crate) fn parse_element_header(&mut self) -> R<ElementHeader> {
        let mut stereotypes = Vec::new();
        let mut tagged_values = Vec::new();

        // Parse annotations in any order: <<stereos>>, {tags}, <<stereos>>, ...
        loop {
            if self.cursor.check(TokenKind::LessLess) {
                stereotypes.extend(self.parse_stereotypes()?);
            } else if self.is_tagged_value_start() {
                tagged_values.extend(self.parse_tagged_values()?);
            } else {
                break;
            }
        }

        let tagged_values =
            Self::with_documentation(self.pending_documentation.take(), tagged_values)?;
        let (package, name_str, name_si) = self.parse_qualified_name()?;
        Ok(ElementHeader {
            stereotypes,
            tagged_values,
            package,
            name: SpannedString {
                value: name_str,
                source_info: name_si,
            },
        })
    }
}

/// Whether a tag is the documentation tag `doc.doc`, written as `doc` (via an
/// import) or `meta::pure::profiles::doc`. Tags aren't resolved at parse
/// time; a user profile that merely ends in `doc` is different.
fn is_doc_tag(tag: &legend_pure_parser_ast::annotation::TagPtr) -> bool {
    if tag.value != "doc" || tag.profile.name != "doc" {
        return false;
    }
    match &tag.profile.package {
        None => true,
        Some(package) => package.to_string() == "meta::pure::profiles",
    }
}

/// Shared parser context passed to island grammar plugins.
///
/// Provides access to the token [`Cursor`] (via `.cursor`) and the host
/// parser's expression/path parsing capabilities.
///
/// # Example
///
/// ```rust,ignore
/// fn parse(&self, ctx: &mut ParserContext<'_>) -> Result<...> {
///     let si = ctx.cursor().current_source_info();
///     ctx.cursor().expect(TokenKind::LBrace)?;
///     let expr = ctx.parse_expression()?;
///     // ...
/// }
/// ```
pub struct ParserContext<'a> {
    /// The underlying parser — provides cursor access and expression parsing.
    pub(crate) parser: &'a mut Parser,
    /// The island being parsed, for island parsers.
    pub(crate) island: Option<Island>,
}

/// The island an island parser is parsing.
pub(crate) struct Island {
    /// `#tag{` to `}#`.
    pub(crate) span: SourceInfo,
    /// The body as written, without `#tag{` and `}#`.
    pub(crate) text: SmolStr,
}

impl ParserContext<'_> {
    /// The island being parsed, `#tag{` to `}#`: an island parser's cursor
    /// only holds the island's body. `None` for a section parser.
    #[must_use]
    pub fn island_span(&self) -> Option<&SourceInfo> {
        self.island.as_ref().map(|island| &island.span)
    }

    /// The island's body as written, without `#tag{` and `}#`: what a
    /// [`raw_body`](crate::IslandParser::raw_body) grammar parses. `None`
    /// for a section parser.
    #[must_use]
    pub fn island_text(&self) -> Option<&str> {
        self.island.as_ref().map(|island| island.text.as_str())
    }

    /// Records an error that the engine's walker, rather than its grammar,
    /// finds (a column without a table, an unknown join type): the engine
    /// reports it only once the grammar has read the whole unit it parses at
    /// once (a section, a mapping element's body), so a syntax error further
    /// on comes first. The first such error is kept; the section, block or
    /// island being parsed fails with it once read without syntax errors.
    pub fn defer(&mut self, error: ParseError) {
        if self.parser.deferred.is_none() {
            self.parser.deferred = Some(error);
        }
    }

    /// The offset the text was lexed with: where the engine's section (or
    /// expression) starts in its document, the line added to every line and
    /// the column to those of the text's first line. Some of the engine's
    /// walkers position things relative to the section's own text.
    #[must_use]
    pub fn source_offset(&self) -> legend_pure_parser_ast::SourceOffset {
        self.parser.offset
    }

    /// Access the token cursor directly.
    ///
    /// Public so external island-parser plug-ins (`dsl-graph`,
    /// `dsl-store`, `dsl-tds`, …) can consume tokens directly when
    /// their grammar isn't expressible via the high-level helpers
    /// (`parse_expression`, `parse_package_path`, etc.).
    pub fn cursor(&mut self) -> &mut Cursor {
        &mut self.parser.cursor
    }

    /// Parse a full expression using the host parser's expression grammar.
    ///
    /// # Errors
    /// Propagates any [`ParseError`] raised by the underlying expression parser.
    pub fn parse_expression(&mut self) -> R<Expression> {
        self.parser.parse_expression()
    }

    /// Parse a package path: `my::pkg::Name`.
    ///
    /// # Errors
    /// Propagates any [`ParseError`] raised by the underlying package-path parser.
    pub fn parse_package_path(&mut self) -> R<Package> {
        self.parser.parse_package_path()
    }

    /// Parse a qualified name, returning (package, name, `source_info`).
    ///
    /// # Errors
    /// Propagates any [`ParseError`] raised by the underlying qualified-name parser.
    pub fn parse_qualified_name(&mut self) -> R<(Option<Package>, SmolStr, SourceInfo)> {
        self.parser.parse_qualified_name()
    }

    /// Parse a type reference: `my::Class[1]`.
    ///
    /// # Errors
    /// Propagates any [`ParseError`] raised by the underlying type-reference parser.
    pub fn parse_type_reference(&mut self) -> R<TypeReference> {
        self.parser.parse_type_reference()
    }

    /// Parse a multiplicity: `[1]`, `[1..*]`.
    ///
    /// # Errors
    /// Propagates any [`ParseError`] raised by the underlying multiplicity parser.
    pub fn parse_multiplicity(&mut self) -> R<Multiplicity> {
        self.parser.parse_multiplicity()
    }

    /// Parse stereotypes: `<<profile.stereotype>>`.
    ///
    /// # Errors
    /// Propagates any [`ParseError`] raised by the underlying stereotype parser.
    pub fn parse_stereotypes(&mut self) -> R<Vec<StereotypePtr>> {
        self.parser.parse_stereotypes()
    }

    /// Parse tagged values: `{profile.tag = 'value'}`.
    ///
    /// # Errors
    /// Propagates any [`ParseError`] raised by the underlying tagged-value parser.
    pub fn parse_tagged_values(&mut self) -> R<Vec<TaggedValue>> {
        self.parser.parse_tagged_values()
    }

    /// Parse constraints: `[name: expr]`.
    ///
    /// # Errors
    /// Propagates any [`ParseError`] raised by the underlying constraint parser.
    pub fn parse_constraints(&mut self) -> R<Vec<Constraint>> {
        self.parser.parse_constraints()
    }

    /// Parse a parameter: `name: Type[1]`.
    ///
    /// # Errors
    /// Propagates any [`ParseError`] raised by the underlying parameter parser.
    pub fn parse_parameter(&mut self) -> R<Parameter> {
        self.parser.parse_parameter()
    }
}

#[cfg(test)]
mod test_support {
    use legend_pure_parser_ast::SourceInfo;

    use super::{Parser, ParserContext};
    use crate::cursor::Cursor;

    /// Runs `f` on a context over `source`'s tokens.
    pub(super) fn with_context<T>(source: &str, f: impl FnOnce(&mut ParserContext<'_>) -> T) -> T {
        let tokens = legend_pure_parser_lexer::tokenize(source, "t.pure").expect("lexes");
        let mut parser = Parser::new(Cursor::new(tokens));
        f(&mut ParserContext {
            parser: &mut parser,
            island: None,
        })
    }

    /// Start line and column, end line and column.
    pub(super) fn position(si: &SourceInfo) -> (u32, u32, u32, u32) {
        (si.start_line, si.start_column, si.end_line, si.end_column)
    }
}
