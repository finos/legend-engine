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

//! # Legend Pure Parser — Engine API
//!
//! The engine-facing contract: parse one grammar **section body** into
//! Protocol v1 elements, exactly as a Legend Engine `SectionParser`
//! receives it (see `docs/engine-integration.md`).
//!
//! - The body has no `###Section` header; positions are always reported,
//!   in document coordinates using the engine's line/column offsets (the
//!   engine drops them on read when its caller asks for none).
//! - Parsing is all-or-nothing: the first error wins and no partial
//!   elements are returned.
//! - [`ErrorKind::Unsupported`] marks input this parser cannot yet turn into
//!   engine protocol (an island or section grammar that isn't linked, a
//!   `native function`, …), so the caller can fall back to its own parser.
//!
//! Grammar plug-ins are whatever crates are linked into the final binary
//! (see [`capabilities`]); this crate has no knowledge of any of them.
//! Every call builds its own parser, so calls are independent and may run
//! concurrently.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

use legend_pure_parser_ast::element::{Element, FunctionTestExpected};
use legend_pure_parser_ast::expression::{CodeBlock, Expression};
use legend_pure_parser_ast::source_info::Spanned;
use legend_pure_parser_ast::{SourceFile, SourceInfo, SourceOffset};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::island::discovered_island_parsers;
use legend_pure_parser_parser::section_parser::discovered_section_parsers;
use legend_pure_parser_protocol::ConvertError;
use legend_pure_parser_protocol::island_protocol::discovered_island_protocols;
use legend_pure_parser_protocol::v1::convert::{convert_element, convert_expression_typed};
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::value_spec::{LambdaFunction, ValueSpecification};
use serde::Serialize;

/// The section type whose body is the core Pure grammar.
pub const PURE_SECTION: &str = "Pure";

/// What the linked grammar plug-ins can parse.
#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
pub struct Capabilities {
    /// Section types [`parse_section`] accepts: `Pure`, then every linked
    /// section grammar's, sorted.
    pub sections: Vec<String>,
    /// Island tags with both a linked parser and a linked protocol
    /// converter (`""` is the graph-fetch island `#{…}#`).
    pub islands: Vec<String>,
}

/// Reports the section types and island tags this build supports.
#[must_use]
pub fn capabilities() -> Capabilities {
    let mut linked: Vec<String> = discovered_section_parsers()
        .iter()
        .map(|p| p.kind().to_string())
        .collect();
    linked.sort_unstable();
    let mut sections = vec![PURE_SECTION.to_string()];
    sections.extend(linked);
    let protocols = discovered_island_protocols();
    let islands = discovered_island_parsers()
        .iter()
        .map(|p| p.tag())
        .filter(|tag| protocols.iter().any(|p| p.tag() == *tag))
        .map(str::to_string)
        .collect();
    Capabilities { sections, islands }
}

/// One section body to parse, with the engine's position context.
#[derive(Debug, Clone, Copy)]
pub struct SectionRequest<'a> {
    /// The section type from the `###` header, e.g. `Pure`.
    pub section_type: &'a str,
    /// The section body: the text after the `###` header token.
    pub code: &'a str,
    /// The engine's `sourceId`, reported in every source information.
    pub source_id: &'a str,
    /// Added to every line number (signed; the engine often passes `-1`).
    pub line_offset: i32,
    /// Added to column numbers on the body's first line.
    pub column_offset: i32,
}

/// The parsed section: its protocol elements and its imports.
#[derive(Debug, Clone, PartialEq, Serialize)]
pub struct SectionResult {
    /// Protocol v1 elements, in source order.
    pub elements: Vec<serde_json::Value>,
    /// Import paths declared at the top of the section, or `None` when the
    /// section's grammar has no imports (`###Data`): the engine then makes
    /// the section a `DefaultCodeSection`, not an `ImportAwareCodeSection`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub imports: Option<Vec<String>>,
}

/// Why a section could not be parsed.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum ErrorKind {
    /// The input is not valid grammar.
    Parser,
    /// The input may be valid, but this parser can't turn it into engine
    /// protocol; the caller should fall back to its own parser.
    Unsupported,
    /// A bug or plug-in failure inside this parser.
    Internal,
}

/// A failed [`parse_section`].
#[derive(Debug, Clone, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SectionError {
    /// The error category.
    pub kind: ErrorKind,
    /// A human-readable message, without position.
    pub message: String,
    /// Where the error is, in document coordinates.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

impl SectionError {
    fn new(kind: ErrorKind, message: impl Into<String>, source_info: Option<&SourceInfo>) -> Self {
        Self {
            kind,
            message: message.into(),
            source_information: source_info.map(SourceInformation::from),
        }
    }
}

impl From<ConvertError> for SectionError {
    fn from(e: ConvertError) -> Self {
        let kind = match e {
            ConvertError::Unsupported { .. } => ErrorKind::Unsupported,
            ConvertError::Plugin { .. } | ConvertError::Json(_) => ErrorKind::Internal,
        };
        Self::new(kind, e.to_string(), e.source_info())
    }
}

/// Parses one section body into protocol elements.
///
/// # Errors
///
/// The first problem found, as a [`SectionError`]: a syntax error
/// ([`ErrorKind::Parser`]), input this build cannot convert
/// ([`ErrorKind::Unsupported`]), or an internal failure.
pub fn parse_section(request: &SectionRequest<'_>) -> Result<SectionResult, SectionError> {
    let offset = source_offset(request.line_offset, request.column_offset)?;
    let text = section_text(request)?;
    let has_imports = request.section_type == PURE_SECTION
        || discovered_section_parsers()
            .iter()
            .any(|p| p.kind() == request.section_type && p.imports());
    let file = legend_pure_parser_parser::parse_with_offset(&text, request.source_id, offset)
        .map_err(|partial| match partial.errors.first() {
            // Parse errors are collected in source order; the engine
            // reports the first one.
            Some(first) => parse_error(first),
            None => SectionError::new(ErrorKind::Internal, "parse failed without an error", None),
        })?;

    engine_profile(&file)?;

    let mut elements = Vec::new();
    let mut imports = Vec::new();
    for section in &file.sections {
        imports.extend(section.imports.iter().map(|i| i.path.to_string()));
        for element in &section.elements {
            let protocol = convert_element(element)?;
            let json = serde_json::to_value(&protocol)
                .map_err(|e| SectionError::from(ConvertError::from(e)))?;
            elements.push(json);
        }
    }
    Ok(SectionResult {
        elements,
        imports: has_imports.then_some(imports),
    })
}

/// Which standalone expression to parse, as the engine's `PureGrammarParser`
/// entry points name them.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ExpressionKind {
    /// `parseLambda`: a lambda, or statements the engine makes the body of
    /// one.
    Lambda,
    /// `parseValueSpecification`: one expression.
    ValueSpecification,
    /// `parseGraphFetch`: the tree of a graph fetch.
    GraphFetch,
}

impl ExpressionKind {
    /// The kind a caller names `lambda`, `valueSpecification` or
    /// `graphFetch`.
    #[must_use]
    pub fn from_name(name: &str) -> Option<Self> {
        match name {
            "lambda" => Some(Self::Lambda),
            "valueSpecification" => Some(Self::ValueSpecification),
            "graphFetch" => Some(Self::GraphFetch),
            _ => None,
        }
    }
}

/// One standalone expression to parse, with the engine's position context.
#[derive(Debug, Clone, Copy)]
pub struct ExpressionRequest<'a> {
    /// What to parse the text as.
    pub kind: ExpressionKind,
    /// The expression's text.
    pub code: &'a str,
    /// The engine's `sourceId`, reported in every source information.
    pub source_id: &'a str,
    /// Added to every line number.
    pub line_offset: i32,
    /// Added to column numbers on the text's first line.
    pub column_offset: i32,
}

/// Parses a standalone expression into its protocol JSON, as the engine's
/// `PureGrammarParser` does: a `LambdaFunction`, a value specification, or
/// a `RootGraphFetchTree`.
///
/// # Errors
///
/// As for [`parse_section`].
pub fn parse_expression(
    request: &ExpressionRequest<'_>,
) -> Result<serde_json::Value, SectionError> {
    use legend_pure_parser_parser::{parse_code_block, parse_standalone_expression};

    let offset = source_offset(request.line_offset, request.column_offset)?;
    let (code, source_id) = (request.code, request.source_id);
    let value = match request.kind {
        ExpressionKind::Lambda => {
            let block = parse_code_block(code, source_id, offset).map_err(|e| parse_error(&e))?;
            lambda(&block)?
        }
        ExpressionKind::ValueSpecification => {
            let expression = parse_standalone_expression(code, source_id, offset)
                .map_err(|e| parse_error(&e))?;
            convert_expression_typed(&expression)?
        }
        ExpressionKind::GraphFetch => {
            let expression = parse_standalone_expression(code, source_id, offset)
                .map_err(|e| parse_error(&e))?;
            return graph_fetch_tree(&expression);
        }
    };
    serde_json::to_value(&value).map_err(|e| SectionError::from(ConvertError::from(e)))
}

/// The engine's `concreteFunctionDefinition`: a code block that is one
/// lambda is that lambda; any other is the body of a lambda without
/// parameters, positioned on the code block.
fn lambda(block: &CodeBlock) -> Result<ValueSpecification, SectionError> {
    if let [expression @ Expression::Lambda(_)] = block.expressions.as_slice() {
        return Ok(convert_expression_typed(expression)?);
    }
    let body = block
        .expressions
        .iter()
        .map(convert_expression_typed)
        .collect::<Result<Vec<_>, _>>()?;
    Ok(ValueSpecification::Lambda(LambdaFunction {
        body,
        parameters: Vec::new(),
        source_information: Some(SourceInformation::from(&block.source_info)),
    }))
}

/// The engine's `parseGraphFetch`: the value of the class instance a graph
/// fetch island is.
fn graph_fetch_tree(expression: &Expression) -> Result<serde_json::Value, SectionError> {
    let ValueSpecification::ClassInstance(instance) = convert_expression_typed(expression)? else {
        return Err(SectionError::new(
            ErrorKind::Parser,
            "Expected a graph fetch tree",
            Some(expression.source_info()),
        ));
    };
    Ok(instance.value)
}

/// The engine's source offsets, which must not make positions negative.
fn source_offset(line_offset: i32, column_offset: i32) -> Result<SourceOffset, SectionError> {
    SourceOffset::new(line_offset, column_offset).ok_or_else(|| {
        SectionError::new(
            ErrorKind::Unsupported,
            format!(
                "source offsets (line {line_offset}, column {column_offset}) would produce negative positions"
            ),
            None,
        )
    })
}

/// A parser error as the engine reports it: unsupported input or a syntax
/// error.
fn parse_error(error: &ParseError) -> SectionError {
    let kind = match error {
        ParseError::Unsupported { .. } => ErrorKind::Unsupported,
        _ => ErrorKind::Parser,
    };
    SectionError::new(kind, error.message(), error.source_info())
}

/// Rejects what the Legend Pure grammar accepts but the engine's does not,
/// as the engine reports it: a parser error at the same position.
fn engine_profile(file: &SourceFile) -> Result<(), SectionError> {
    for element in file.sections.iter().flat_map(|s| &s.elements) {
        let parameters = match element {
            Element::Class(class) => class.type_and_multiplicity_parameters.as_ref(),
            Element::Function(function) => function.type_and_multiplicity_parameters.as_ref(),
            _ => None,
        };
        if let Some(parameters) = parameters {
            return Err(SectionError::new(
                ErrorKind::Parser,
                "Type and/or multiplicity parameters are not authorized in Legend Engine",
                Some(&parameters.source_info),
            ));
        }
        // A function's tests must call that function (the engine's
        // `processFuncTest`), and expect a relation if they expect embedded
        // data (its `visitTestAssertion`); in that order.
        if let Element::Function(function) = element {
            for test in function.tests.iter().flat_map(|suite| &suite.assertions) {
                if let Expression::FunctionApplication(call) = &test.invocation
                    && call.function.name != function.name.value
                {
                    return Err(SectionError::new(
                        ErrorKind::Parser,
                        format!(
                            "Function name in test '{}' does not match function name '{}'",
                            call.function.name, function.name.value
                        ),
                        Some(&call.function.source_info),
                    ));
                }
                if let FunctionTestExpected::Embedded(data) = &test.expected {
                    return Err(SectionError::new(
                        ErrorKind::Parser,
                        format!(
                            "Unsupported embedded data type for function test assertion: {}. Only 'Relation' is supported.",
                            data.type_name
                        ),
                        Some(&data.source_info),
                    ));
                }
            }
        }
    }
    Ok(())
}

/// The text to hand the parser: the body itself for `###Pure` (headerless
/// input is a Pure section), or the body behind its own header for a linked
/// section grammar. The header goes on the body's first line — where it
/// sits in the engine's document — so no line shifts.
fn section_text(request: &SectionRequest<'_>) -> Result<String, SectionError> {
    if request.section_type == PURE_SECTION {
        return Ok(request.code.to_string());
    }
    if discovered_section_parsers()
        .iter()
        .any(|p| p.kind() == request.section_type)
    {
        return Ok(format!("###{}{}", request.section_type, request.code));
    }
    Err(SectionError::new(
        ErrorKind::Unsupported,
        format!(
            "no section grammar is linked for '###{}'",
            request.section_type
        ),
        None,
    ))
}

/// [`capabilities`] as a JSON string.
#[must_use]
pub fn capabilities_json() -> String {
    to_json(&capabilities())
}

/// [`parse_section`] as a JSON string: `{"elements":[…],"imports":[…]}` on
/// success (`imports` absent for a grammar without them),
/// `{"error":{"kind","message","sourceInformation"?}}` on failure.
#[must_use]
pub fn parse_section_json(request: &SectionRequest<'_>) -> String {
    match parse_section(request) {
        Ok(result) => to_json(&result),
        Err(error) => error_json(&error),
    }
}

/// [`parse_expression`] as a JSON string: `{"value":…}` on success, the
/// [`parse_section_json`] error shape on failure.
#[must_use]
pub fn parse_expression_json(request: &ExpressionRequest<'_>) -> String {
    match parse_expression(request) {
        Ok(value) => to_json(&serde_json::json!({ "value": value })),
        Err(error) => error_json(&error),
    }
}

/// Renders `error` in the [`parse_section_json`] error shape.
#[must_use]
pub fn error_json(error: &SectionError) -> String {
    to_json(&serde_json::json!({ "error": error }))
}

fn to_json<T: Serialize>(value: &T) -> String {
    // The values rendered here are built from strings, numbers and maps
    // only, so serialization cannot fail; report it rather than panic if
    // that ever changes.
    serde_json::to_string(value).unwrap_or_else(|e| {
        format!(
            r#"{{"error":{{"kind":"internal","message":"serializing the result failed: {}"}}}}"#,
            e.to_string().replace('"', "'")
        )
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn error_json_shape() {
        let e = SectionError::new(ErrorKind::Unsupported, "nope", None);
        assert_eq!(
            error_json(&e),
            r#"{"error":{"kind":"unsupported","message":"nope"}}"#
        );
    }
}
