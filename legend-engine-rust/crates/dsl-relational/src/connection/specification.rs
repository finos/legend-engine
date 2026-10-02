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

//! A relational connection's specifications (the relational store's
//! `IRelationalGrammarParserExtension`s): its datasource specification,
//! authentication strategy, post-processors and query generation configs,
//! each `Type { … }` or `Type`, read by the grammar its type names. The
//! store's own are this crate's, built in; a database extension's
//! (Snowflake's, …) are its crate's, which registers them:
//!
//! ```ignore
//! use legend_pure_dsl_relational::connection::specification::{SpecificationGrammar, SPECIFICATION_GRAMMARS};
//! use linkme::distributed_slice;
//!
//! #[distributed_slice(SPECIFICATION_GRAMMARS)]
//! static SNOWFLAKE: &(dyn SpecificationGrammar + Send + Sync) = &SnowflakeDatasourceGrammar;
//! ```
//!
//! A specification of a type no linked grammar reads is unsupported: an
//! engine extension may read it. So is a connection in `local` mode for a
//! database type without a [`LocalModeGrammar`], which the engine fails on
//! outside its parser.
//!
//! The engine's walker parses a specification again from its text as ANTLR's
//! `getText` gives it, the type then the body with nothing between them,
//! from a token it chooses (the field's name for a datasource specification
//! or an authentication strategy, the type in a list, and a line further
//! down for a query generation config) whose column it adds to the text's
//! first line as an offset, one too many. So what it reads in a body is
//! positioned the same way: on the body's first line, on that token's line
//! and from its column plus the type's length; below it, as many lines up as
//! the text dropped, at the same column.

use std::any::Any;
use std::collections::HashSet;
use std::fmt;
use std::sync::OnceLock;

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::Name;
use legend_pure_parser_parser::{Block, ParserContext};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use linkme::distributed_slice;
use serde::ser::{Serialize, SerializeMap, Serializer};
use serde_json::Value;

use super::authentication::CORE_AUTHENTICATION_STRATEGIES;
use super::datasource::CORE_DATASOURCE_SPECIFICATIONS;
use super::generation_config::CORE_QUERY_GENERATION_CONFIGS;
use super::post_processor::CORE_POST_PROCESSORS;

type R<T> = Result<T, ParseError>;

/// What a specification specifies.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum SpecificationKind {
    /// `specification: Type { … };`, the engine's `DatasourceSpecification`.
    DatasourceSpecification,
    /// `auth: Type { … };`, the engine's `AuthenticationStrategy`.
    AuthenticationStrategy,
    /// One of `postProcessors: [ … ];`, the engine's `PostProcessor`.
    PostProcessor,
    /// One of `queryGenerationConfigs: [ … ];`, the engine's
    /// `RelationalQueryGenerationConfig`.
    QueryGenerationConfig,
}

impl SpecificationKind {
    /// Its name in messages.
    #[must_use]
    pub fn what(self) -> &'static str {
        match self {
            Self::DatasourceSpecification => "datasource specification",
            Self::AuthenticationStrategy => "authentication strategy",
            Self::PostProcessor => "post-processor",
            Self::QueryGenerationConfig => "query generation config",
        }
    }
}

/// Whether a specification's grammar reads a body.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BodyUse {
    /// `Type`: a body written is not read, as the engine's rule ends with the
    /// type.
    None,
    /// `Type` or `Type { … }`.
    Optional,
    /// `Type { … }`.
    Required,
}

/// Where a specification is.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SpecificationSpan {
    /// Where the engine positions the specification and reports what its
    /// walker finds wrong with it: the field, `auth: Type { … };`, or, in a
    /// list, the specification itself.
    pub source_info: SourceInfo,
}

/// A specification's body, as its type's grammar reads it.
pub trait SpecificationBody: fmt::Debug + Send + Sync {
    /// Downcast to the grammar's concrete type.
    fn as_any(&self) -> &dyn Any;

    /// Clone into a new boxed trait object.
    fn clone_box(&self) -> Box<dyn SpecificationBody>;

    /// Equality with another body: `false` for another concrete type.
    fn eq_content(&self, other: &dyn SpecificationBody) -> bool;
}

impl Clone for Box<dyn SpecificationBody> {
    fn clone(&self) -> Self {
        self.clone_box()
    }
}

impl PartialEq for dyn SpecificationBody {
    fn eq(&self, other: &Self) -> bool {
        self.eq_content(other)
    }
}

/// A specification, read by its type's grammar.
#[derive(Debug, Clone)]
pub struct Specification {
    /// What it specifies.
    pub kind: SpecificationKind,
    /// The type, as written.
    pub type_name: Name,
    /// The body.
    pub body: Box<dyn SpecificationBody>,
    /// See [`SpecificationSpan::source_info`].
    pub source_info: SourceInfo,
}

impl PartialEq for Specification {
    fn eq(&self, other: &Self) -> bool {
        self.kind == other.kind
            && self.type_name == other.type_name
            && *self.body == *other.body
            && self.source_info == other.source_info
    }
}

/// The grammar of one type of specification, e.g. the `LocalH2` datasource
/// specification.
///
/// Stateless: the same instance may parse on several threads at once.
pub trait SpecificationGrammar {
    /// What it reads.
    fn kind(&self) -> SpecificationKind;

    /// The type it reads.
    fn type_name(&self) -> &str;

    /// Whether it reads a body.
    fn body_use(&self) -> BodyUse;

    /// Reads a specification, `body` holding its body's tokens (between the
    /// braces) when written and read, positioned as the engine's walker
    /// positions them, then checks it as the walker does. It must read the
    /// body whole.
    ///
    /// # Errors
    ///
    /// The grammar's or the walker's error.
    fn parse(
        &self,
        span: &SpecificationSpan,
        body: Option<&mut ParserContext<'_>>,
    ) -> R<Box<dyn SpecificationBody>>;

    /// The engine's JSON of a specification this grammar read: an object
    /// with its `_type`.
    ///
    /// # Errors
    ///
    /// A conversion failure.
    fn convert(&self, specification: &Specification) -> Result<Value, ConvertError>;
}

/// A connection in `local` mode's datasource specification and
/// authentication strategy for a database type (the engine's
/// `getExtraLocalModeDataSourceSpecification` and
/// `getExtraLocalModeAuthenticationStrategy`), made from the connection.
pub trait LocalModeGrammar {
    /// The database type, as the engine's `DatabaseType` names it.
    fn database_type(&self) -> &str;

    /// The datasource specification and the authentication strategy, for a
    /// connection to `store`, if it names one; none where the engine fails,
    /// outside its parser.
    fn specifications(
        &self,
        store: Option<&str>,
    ) -> Option<(LocalSpecification, LocalSpecification)>;
}

/// A local mode's datasource specification or authentication strategy: its
/// `_type` and its fields, all strings. It serializes as the engine's JSON
/// of it.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LocalSpecification {
    /// The `_type`.
    pub json_type: &'static str,
    /// The fields, named as in the protocol, each name once.
    pub fields: Vec<(&'static str, String)>,
}

impl Serialize for LocalSpecification {
    fn serialize<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        let mut map = serializer.serialize_map(Some(self.fields.len() + 1))?;
        map.serialize_entry("_type", self.json_type)?;
        for (name, value) in &self.fields {
            map.serialize_entry(name, value)?;
        }
        map.end()
    }
}

/// The relational store's own specification grammars.
fn core_grammars() -> impl Iterator<Item = &'static (dyn SpecificationGrammar + Send + Sync)> {
    CORE_DATASOURCE_SPECIFICATIONS
        .iter()
        .chain(CORE_AUTHENTICATION_STRATEGIES.iter())
        .chain(CORE_POST_PROCESSORS.iter())
        .chain(CORE_QUERY_GENERATION_CONFIGS.iter())
        .copied()
}

/// Distributed slice into which each crate owning a specification grammar
/// registers it. See the module docs.
#[distributed_slice]
pub static SPECIFICATION_GRAMMARS: [&'static (dyn SpecificationGrammar + Send + Sync)] = [..];

/// Distributed slice into which each crate owning a local mode registers it.
#[distributed_slice]
pub static LOCAL_MODE_GRAMMARS: [&'static (dyn LocalModeGrammar + Send + Sync)] = [..];

/// The store's specification grammars and the registered
/// [`SPECIFICATION_GRAMMARS`], validated once per process.
///
/// # Panics
///
/// Panics when two registered grammars read the same kind and type: a
/// link-time misconfiguration.
#[must_use]
pub fn specification_grammars() -> &'static [&'static (dyn SpecificationGrammar + Send + Sync)] {
    static DISCOVERED: OnceLock<Vec<&'static (dyn SpecificationGrammar + Send + Sync)>> =
        OnceLock::new();
    DISCOVERED.get_or_init(|| {
        let mut seen = HashSet::new();
        core_grammars()
            .chain(SPECIFICATION_GRAMMARS.iter().copied())
            .inspect(|grammar| {
                let key = (grammar.kind(), grammar.type_name());
                assert!(
                    seen.insert(key),
                    "two {} grammars are registered for type `{}`",
                    key.0.what(),
                    key.1
                );
            })
            .collect()
    })
}

/// The grammar for `kind`s of type `type_name`.
#[must_use]
pub fn specification_grammar(
    kind: SpecificationKind,
    type_name: &str,
) -> Option<&'static (dyn SpecificationGrammar + Send + Sync)> {
    specification_grammars()
        .iter()
        .copied()
        .find(|grammar| grammar.kind() == kind && grammar.type_name() == type_name)
}

/// The local mode for `database_type`.
#[must_use]
pub fn local_mode_grammar(
    database_type: &str,
) -> Option<&'static (dyn LocalModeGrammar + Send + Sync)> {
    LOCAL_MODE_GRAMMARS
        .iter()
        .copied()
        .find(|grammar| grammar.database_type() == database_type)
}

/// The linked types of `kind`, sorted, for messages.
fn linked_types(kind: SpecificationKind) -> String {
    let mut types: Vec<&str> = specification_grammars()
        .iter()
        .filter(|grammar| grammar.kind() == kind)
        .map(|grammar| grammar.type_name())
        .collect();
    types.sort_unstable();
    types.join(", ")
}

/// A specification as the connection's grammar reads it: its type, its body
/// read but not parsed, and where the engine's walker parses it again from.
#[derive(Debug, Clone)]
pub struct ReadSpecification {
    /// The type, as written.
    pub type_name: Name,
    /// `{ … }`.
    pub body: Option<Block>,
    /// See [`SpecificationSpan::source_info`].
    pub source_info: SourceInfo,
    /// The token the walker positions the text from.
    pub anchor: SourceInfo,
}

/// Reads `read` with its type's grammar.
///
/// # Errors
///
/// No linked grammar reads the type (unsupported), a body is required and
/// not written, or the grammar's error.
pub fn parse_specification(
    ctx: &mut ParserContext<'_>,
    kind: SpecificationKind,
    read: &ReadSpecification,
) -> R<Specification> {
    let type_name = &read.type_name.text;
    let Some(grammar) = specification_grammar(kind, type_name) else {
        return Err(ParseError::Unsupported {
            message: format!(
                "no relational {} grammar is linked for '{type_name}'; the linked ones are: {}",
                kind.what(),
                linked_types(kind)
            ),
            source_info: read.source_info.clone(),
        });
    };
    let span = SpecificationSpan {
        source_info: read.source_info.clone(),
    };
    let reparse = Reparse::new(kind, read);
    let body = match (grammar.body_use(), &read.body) {
        (BodyUse::None, _) | (BodyUse::Optional, None) => grammar.parse(&span, None)?,
        (_, Some(block)) => {
            let block = block.with_positions(|si| reparse.position(si));
            ctx.parse_block(&block, |body| grammar.parse(&span, Some(body)))?
        }
        (BodyUse::Required, None) => {
            // The text ends with the type, where the grammar wants `{`.
            let end = &read.type_name.source_info;
            let at = reparse.position(&SourceInfo::new(
                end.source.clone(),
                end.start_line,
                end.end_column + 1,
                end.start_line,
                end.end_column + 1,
            ));
            return Err(ParseError::Unexpected {
                message: "Unexpected end of input: expected '{'".to_string(),
                source_info: at,
            });
        }
    };
    Ok(Specification {
        kind,
        type_name: read.type_name.clone(),
        body,
        source_info: read.source_info.clone(),
    })
}

/// Where the engine's walker positions what it reads of a specification:
/// see the module docs.
#[derive(Debug, Clone, Copy)]
struct Reparse {
    /// The line the text's first line is on.
    line: i64,
    /// What the text's first line's columns are moved by: the anchor's
    /// column, plus the type's length, less the brace's column.
    shift: i64,
    /// The brace's line: the text's first.
    brace_line: i64,
}

impl Reparse {
    fn new(kind: SpecificationKind, read: &ReadSpecification) -> Self {
        let extra_line = i64::from(kind == SpecificationKind::QueryGenerationConfig);
        let type_source_info = &read.type_name.source_info;
        let type_length = i64::try_from(read.type_name.text.chars().count()).unwrap_or(i64::MAX);
        // The text is the type, `{`, then the body as written: the type
        // starts on the text's first line.
        let (brace_line, brace_column) = read.body.as_ref().map_or(
            (
                i64::from(type_source_info.start_line),
                i64::from(type_source_info.end_column) + 1,
            ),
            |body| {
                (
                    i64::from(body.source_info.start_line),
                    i64::from(body.source_info.start_column),
                )
            },
        );
        Self {
            line: i64::from(read.anchor.start_line) + extra_line,
            shift: i64::from(read.anchor.start_column) + type_length + 1 - brace_column,
            brace_line,
        }
    }

    fn point(self, line: u32, column: u32) -> (u32, u32) {
        let (line, column) = (i64::from(line), i64::from(column));
        let (line, column) = if line == self.brace_line {
            (self.line, column + self.shift)
        } else {
            (self.line + line - self.brace_line, column)
        };
        (
            u32::try_from(line).unwrap_or(0),
            u32::try_from(column).unwrap_or(0),
        )
    }

    fn position(self, source_info: &SourceInfo) -> SourceInfo {
        let (start_line, start_column) =
            self.point(source_info.start_line, source_info.start_column);
        let (end_line, end_column) = self.point(source_info.end_line, source_info.end_column);
        SourceInfo::new(
            source_info.source.clone(),
            start_line,
            start_column,
            end_line,
            end_column,
        )
    }
}

/// A specification's body, as the grammar converting it read it.
pub(crate) fn downcast<T: 'static>(specification: &Specification) -> Result<&T, ConvertError> {
    specification
        .body
        .as_any()
        .downcast_ref::<T>()
        .ok_or_else(|| {
            ConvertError::plugin(
                format!(
                    "the body of a '{}' {} is not its grammar's",
                    specification.type_name.text,
                    specification.kind.what()
                ),
                Some(&specification.source_info),
            )
        })
}
