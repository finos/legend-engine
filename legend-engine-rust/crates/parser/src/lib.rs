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

//! # Legend Pure Parser — Parser
//!
//! Recursive descent parser for the Pure grammar. Consumes tokens from the lexer
//! and produces an AST.
//!
//! # Usage
//!
//! ```
//! use legend_pure_parser_parser::parse;
//!
//! let source = "###Pure\nProfile my::doc { stereotypes: [deprecated]; tags: [todo]; }";
//! let file = parse(source, "test.pure").unwrap();
//! assert_eq!(file.element_count(), 1);
//! ```
//!
//! # Parallel Parsing
//!
//! For multi-file workloads, use [`parse_many`] with [`SourceProvider`]
//! implementations to parse files concurrently across all available CPU cores:
//!
//! ```
//! use legend_pure_parser_parser::{parse_many, source::SourceInput};
//!
//! let sources = vec![
//!     SourceInput::in_memory("a.pure", "Class A {}"),
//!     SourceInput::in_memory("b.pure", "Class B {}"),
//! ];
//! let outputs = parse_many(&sources);
//! assert!(outputs.iter().all(|o| o.is_ok()));
//! ```

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod blocks;
pub mod cursor;
pub mod embedded_data;
pub mod error;
pub mod fields;
pub mod forms;
pub mod island;
mod parser;
pub mod section_parser;
pub mod source;
pub mod words;

use legend_pure_parser_ast::{SourceFile, SourceOffset};

pub use embedded_data::EmbeddedDataParser;
pub use error::ParseError;
pub use island::IslandParser;
pub use parser::ParserContext;
pub use parser::block::{Block, Island, TypedIsland};
pub use parser::helpers::{split_package_name, unquote_string};
pub use parser::strings::{raw_string_value, string_value};
pub use section_parser::SectionParser;
pub use source::SourceProvider;

/// A partial parse result: the best-effort AST plus accumulated errors.
///
/// Returned in the `Err` variant of [`parse()`] when element-level parse
/// errors occur. Unlike discarding the entire file, this preserves all
/// successfully parsed elements:
///
/// - **Compiler**: can declare/resolve elements from valid portions
/// - **LSP**: can show diagnostics alongside partial navigation
/// - **CLI**: can report all errors at once
#[derive(Debug)]
pub struct PartialSourceFile {
    /// The partially parsed AST (may have fewer elements than the source).
    pub source_file: SourceFile,
    /// Parse errors (guaranteed non-empty).
    pub errors: Vec<ParseError>,
}

/// Convenience: extract the AST from either Ok or Err.
impl PartialSourceFile {
    /// Returns the partial AST.
    #[must_use]
    pub fn into_source_file(self) -> SourceFile {
        self.source_file
    }
}

impl std::fmt::Display for PartialSourceFile {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{} parse error(s)", self.errors.len())?;
        if let Some(first) = self.errors.first() {
            write!(f, ": {first}")?;
        }
        Ok(())
    }
}

/// Parse Pure source text into an AST [`SourceFile`].
///
/// Threads every [`IslandParser`] and [`SectionParser`] discovered via
/// the [`crate::island::ISLAND_PARSERS`] / `crate::SECTION_PARSERS`
/// distributed slices into the parser. This is the production default —
/// each linked DSL crate self-registers its parsers, and the binary
/// inherits them with no per-call wiring.
///
/// Tests that need to compose plugins by hand should call
/// [`parse_with_islands`] or [`parse_with_sections`] instead.
///
/// # Errors
///
/// - `Ok(SourceFile)` — all elements parsed successfully
/// - `Err(PartialSourceFile)` — some elements failed to parse, but valid
///   elements are preserved in `partial.source_file`
///
/// # Panics
///
/// Panics at discovery time when two registered island parsers share a
/// `tag()` or two section parsers share a `kind()`. See
/// [`crate::island::discovered_island_parsers`] and
/// [`crate::section_parser::discovered_section_parsers`].
#[allow(clippy::result_large_err)]
pub fn parse(source: &str, source_name: &str) -> Result<SourceFile, PartialSourceFile> {
    parse_with_offset(source, source_name, SourceOffset::default())
}

/// Parse `source` as a fragment of a larger document, with every
/// discovered plug-in (see [`parse`]).
///
/// All AST and error positions are reported in document coordinates,
/// shifted by `offset`. Legend Engine uses this shape for section bodies:
/// the text after a `###Section` header, plus the header's line offset.
///
/// # Errors
///
/// As for [`parse`].
///
/// # Panics
///
/// As for [`parse`].
#[allow(clippy::result_large_err)]
pub fn parse_with_offset(
    source: &str,
    source_name: &str,
    offset: SourceOffset,
) -> Result<SourceFile, PartialSourceFile> {
    parse_text(source, source_name, offset, None)
}

/// Parse `source` as the body of one `###kind` section (the text after its
/// header), positioned as [`parse_with_offset`] positions it: what Legend
/// Engine hands its section parsers. The result has that one section.
///
/// The engine splits a model into sections before parsing them, at headers
/// that start a line, and leaves any other `###` in its section's text,
/// where no grammar reads it: here too, such a header is a token no grammar
/// reads, a syntax error at its first `#`.
///
/// # Errors
///
/// As for [`parse`].
///
/// # Panics
///
/// As for [`parse`].
#[allow(clippy::result_large_err)]
pub fn parse_section_body(
    source: &str,
    source_name: &str,
    offset: SourceOffset,
    kind: &str,
) -> Result<SourceFile, PartialSourceFile> {
    parse_text(source, source_name, offset, Some(kind))
}

/// Parse `source`: a file of sections, or the body of a `###body` section.
#[allow(clippy::result_large_err)]
fn parse_text(
    source: &str,
    source_name: &str,
    offset: SourceOffset,
    body: Option<&str>,
) -> Result<SourceFile, PartialSourceFile> {
    let mut tokens = legend_pure_parser_lexer::tokenize_with_offset(source, source_name, offset)
        .map_err(|e| PartialSourceFile {
            source_file: SourceFile {
                sections: vec![],
                source_info: legend_pure_parser_ast::SourceInfo::new(source_name, 0, 0, 0, 0),
            },
            errors: vec![e.into()],
        })?;
    if body.is_some() {
        for token in &mut tokens {
            if token.kind == legend_pure_parser_lexer::TokenKind::SectionHeader {
                token.kind = legend_pure_parser_lexer::TokenKind::Invalid;
            }
        }
    }
    let cursor = cursor::Cursor::new(tokens);
    let discovered_islands = crate::island::discovered_island_parsers();
    let discovered_sections = crate::section_parser::discovered_section_parsers();
    if discovered_islands.is_empty() && discovered_sections.is_empty() {
        // Hot path: no DSL crates registered any parsers. Skip the
        // wrapper / boxing allocations entirely.
        let mut p = parser::Parser::new(cursor);
        p.offset = offset;
        p.section_body = body.map(smol_str::SmolStr::new);
        return p.parse_source_file();
    }
    let islands: Vec<Box<dyn IslandParser>> = discovered_islands
        .iter()
        .map(|&p| Box::new(crate::island::StaticIslandParser(p)) as Box<dyn IslandParser>)
        .collect();
    let sections: Vec<Box<dyn SectionParser>> = discovered_sections
        .iter()
        .map(|&p| Box::new(crate::section_parser::StaticSectionParser(p)) as Box<dyn SectionParser>)
        .collect();
    let mut p = parser::Parser::with_plugins(cursor, islands, sections);
    p.offset = offset;
    p.section_body = body.map(smol_str::SmolStr::new);
    p.parse_source_file()
}

/// Parse a lambda's text as the engine's `PureGrammarParser.parseLambda`
/// reads it: a code block, statements separated by `;`, to the end of the
/// input, positioned in document coordinates shifted by `offset`, with every
/// discovered island grammar.
///
/// # Errors
///
/// The first lexing or parsing error.
///
/// # Panics
///
/// As for [`parse`].
pub fn parse_code_block(
    source: &str,
    source_name: &str,
    offset: SourceOffset,
) -> Result<legend_pure_parser_ast::expression::CodeBlock, ParseError> {
    standalone_parser(source, source_name, offset)?.parse_standalone_code_block()
}

/// Parse an expression standing alone, as the engine's
/// `PureGrammarParser.parseValueSpecification` reads it, to the end of the
/// input, positioned as [`parse_code_block`] positions its statements.
///
/// # Errors
///
/// The first lexing or parsing error.
///
/// # Panics
///
/// As for [`parse`].
pub fn parse_standalone_expression(
    source: &str,
    source_name: &str,
    offset: SourceOffset,
) -> Result<legend_pure_parser_ast::expression::Expression, ParseError> {
    standalone_parser(source, source_name, offset)?.parse_standalone_expression()
}

/// A parser over `source` with every discovered island grammar.
fn standalone_parser(
    source: &str,
    source_name: &str,
    offset: SourceOffset,
) -> Result<parser::Parser, ParseError> {
    let tokens = legend_pure_parser_lexer::tokenize_with_offset(source, source_name, offset)?;
    let islands: Vec<Box<dyn IslandParser>> = crate::island::discovered_island_parsers()
        .iter()
        .map(|&p| Box::new(crate::island::StaticIslandParser(p)) as Box<dyn IslandParser>)
        .collect();
    Ok(parser::Parser::with_island_parsers(
        cursor::Cursor::new(tokens),
        islands,
    ))
}

/// Parse Pure source text with a custom set of island grammar plugins.
///
/// Use this when you need to register additional island grammars beyond
/// the built-in set (e.g., path expressions, embedded SQL).
///
/// # Errors
///
/// - `Ok(SourceFile)` — all elements parsed successfully
/// - `Err(PartialSourceFile)` — some elements failed, valid elements preserved
#[allow(clippy::result_large_err)]
pub fn parse_with_islands(
    source: &str,
    source_name: &str,
    island_parsers: Vec<Box<dyn IslandParser>>,
) -> Result<SourceFile, PartialSourceFile> {
    let tokens =
        legend_pure_parser_lexer::tokenize(source, source_name).map_err(|e| PartialSourceFile {
            source_file: SourceFile {
                sections: vec![],
                source_info: legend_pure_parser_ast::SourceInfo::new(source_name, 0, 0, 0, 0),
            },
            errors: vec![e.into()],
        })?;
    let cursor = cursor::Cursor::new(tokens);
    let mut p = parser::Parser::with_island_parsers(cursor, island_parsers);
    p.parse_source_file()
}

/// Parse Pure source text with both island and section grammar plug-ins.
///
/// Section plug-ins (e.g. `###Diagram`, `###Mapping`) consume the body
/// of a section whose header matches their `kind()` and contribute
/// type-erased [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement)
/// instances. Sections without a registered parser fall through to
/// the default M3 element grammar.
///
/// Use the empty-vec default islands plus your DSL section parser:
///
/// ```rust,ignore
/// parse_with_sections(
///     source,
///     name,
///     legend_pure_parser_parser::island::default_island_parsers(),
///     vec![Box::new(my_dsl::DiagramSectionParser)],
/// )
/// ```
///
/// # Errors
///
/// - `Ok(SourceFile)` — all elements parsed successfully
/// - `Err(PartialSourceFile)` — some elements failed, valid elements preserved
#[allow(clippy::result_large_err)]
pub fn parse_with_sections(
    source: &str,
    source_name: &str,
    island_parsers: Vec<Box<dyn IslandParser>>,
    section_parsers: Vec<Box<dyn SectionParser>>,
) -> Result<SourceFile, PartialSourceFile> {
    let tokens =
        legend_pure_parser_lexer::tokenize(source, source_name).map_err(|e| PartialSourceFile {
            source_file: SourceFile {
                sections: vec![],
                source_info: legend_pure_parser_ast::SourceInfo::new(source_name, 0, 0, 0, 0),
            },
            errors: vec![e.into()],
        })?;
    let cursor = cursor::Cursor::new(tokens);
    let mut p = parser::Parser::with_plugins(cursor, island_parsers, section_parsers);
    p.parse_source_file()
}

/// Result of loading and parsing a single source via [`parse_many`].
///
/// Carries the source name and loaded text alongside the parse result
/// so that callers can render error diagnostics without re-loading the file.
#[derive(Debug)]
pub struct ParseOutput {
    /// The logical source name (from [`SourceProvider::name`]).
    pub name: String,
    /// The loaded source text, or `None` if loading failed.
    pub source_text: Option<String>,
    /// The parse outcome.
    pub outcome: ParseOutcome,
}

/// Outcome of loading + parsing a single source.
#[derive(Debug)]
pub enum ParseOutcome {
    /// Source loaded and parsed successfully (zero errors).
    Success(SourceFile),
    /// Source loaded, partially parsed (some elements valid, some errors).
    Partial(PartialSourceFile),
    /// Source could not be loaded (I/O error).
    IoError(std::io::Error),
}

impl ParseOutput {
    /// Returns `true` if the source was loaded and parsed successfully
    /// (with zero errors).
    #[must_use]
    pub fn is_ok(&self) -> bool {
        matches!(self.outcome, ParseOutcome::Success(_))
    }

    /// Returns the AST if parsing succeeded fully, `None` otherwise.
    ///
    /// For partial results, use [`partial()`](Self::partial) instead.
    #[must_use]
    pub fn ast(&self) -> Option<&SourceFile> {
        match &self.outcome {
            ParseOutcome::Success(ast) => Some(ast),
            _ => None,
        }
    }

    /// Returns the AST regardless of whether parsing was full or partial.
    #[must_use]
    pub fn ast_any(&self) -> Option<&SourceFile> {
        match &self.outcome {
            ParseOutcome::Success(ast) => Some(ast),
            ParseOutcome::Partial(p) => Some(&p.source_file),
            ParseOutcome::IoError(_) => None,
        }
    }

    /// Returns the partial result if the parse had errors.
    #[must_use]
    pub fn partial(&self) -> Option<&PartialSourceFile> {
        match &self.outcome {
            ParseOutcome::Partial(p) => Some(p),
            _ => None,
        }
    }

    /// Consumes self and returns the AST if parsing succeeded fully.
    #[must_use]
    pub fn into_ast(self) -> Option<SourceFile> {
        match self.outcome {
            ParseOutcome::Success(ast) => Some(ast),
            _ => None,
        }
    }

    /// Consumes self and returns the AST from either full or partial results.
    #[must_use]
    pub fn into_ast_any(self) -> Option<SourceFile> {
        match self.outcome {
            ParseOutcome::Success(ast) => Some(ast),
            ParseOutcome::Partial(p) => Some(p.source_file),
            ParseOutcome::IoError(_) => None,
        }
    }
}

/// Parse multiple sources in parallel using all available CPU cores.
///
/// Accepts any slice of [`SourceProvider`] implementations. Each source is
/// loaded ([`source_text()`](SourceProvider::source_text)) and parsed in
/// parallel via Rayon. Results are returned in the **same order** as the
/// input, making output deterministic regardless of scheduling.
///
/// This is the canonical entry point for parallel parsing — CLI commands,
/// benchmarks, and IDE integrations should use this rather than manually
/// calling `rayon::par_iter`.
///
/// # Example
///
/// ```
/// use legend_pure_parser_parser::{parse_many, source::SourceInput};
///
/// let sources = vec![
///     SourceInput::in_memory("a.pure", "Class pkg::A {}"),
///     SourceInput::in_memory("b.pure", "Class pkg::B {}"),
/// ];
/// let outputs = parse_many(&sources);
/// assert_eq!(outputs.len(), 2);
/// assert!(outputs[0].is_ok());
/// assert!(outputs[1].is_ok());
/// ```
pub fn parse_many<S: SourceProvider>(sources: &[S]) -> Vec<ParseOutput> {
    parse_many_with_islands(sources, Vec::<Box<dyn IslandParser>>::new)
}

/// Parses sources in parallel with island grammar plug-ins.
///
/// `island_factory` is invoked once per parallel task to build a
/// fresh `Vec<Box<dyn IslandParser>>` for that task's parse — the
/// Vec is consumed by the underlying [`parse_with_islands`] call,
/// so it can't be shared. DSL crates expose
/// `default_island_parsers()` factory helpers that fit this
/// signature directly:
///
/// ```rust,ignore
/// parse_many_with_islands(
///     &sources,
///     legend_pure_dsl_graph::parser::default_island_parsers,
/// )
/// ```
///
/// Compose factories by calling several `default_island_parsers()`
/// helpers and concatenating their results inside the closure.
pub fn parse_many_with_islands<S, F>(sources: &[S], island_factory: F) -> Vec<ParseOutput>
where
    S: SourceProvider,
    F: Fn() -> Vec<Box<dyn IslandParser>> + Sync,
{
    use rayon::prelude::*;

    sources
        .par_iter()
        .map(|source| {
            let name = source.name().to_string();
            match source.source_text() {
                Ok(text) => {
                    let text_str = text.into_owned();
                    let outcome = match parse_with_islands(&text_str, &name, island_factory()) {
                        Ok(ast) => ParseOutcome::Success(ast),
                        Err(partial) => ParseOutcome::Partial(partial),
                    };
                    ParseOutput {
                        name,
                        source_text: Some(text_str),
                        outcome,
                    }
                }
                Err(io_err) => ParseOutput {
                    name,
                    source_text: None,
                    outcome: ParseOutcome::IoError(io_err),
                },
            }
        })
        .collect()
}
