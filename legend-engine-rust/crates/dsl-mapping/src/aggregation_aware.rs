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

//! The engine core's aggregation aware class mappings: a class mapping and
//! aggregate views of it, each a class mapping of its own type and the
//! aggregations it can answer
//! (`AggregationAwareParserGrammar`, `AggregationAwareMappingParseTreeWalker`),
//! the engine's `AggregationAwareClassMapping`.
//!
//! ```text
//! aggregationAwareClassMapping:  Views: [ view, … ], ~mainMapping: Type { body }
//! view:                          ( ~modelOperation: { specification }, ~aggregateMapping: Type { body } )
//! specification:                 ~canAggregate true|false, ~groupByFunctions( expression, … ),
//!                                ~aggregateValues( ( ~mapFn: expression, ~aggregateFn: expression ), … )
//! ```
//!
//! As the walker does, the main mapping is parsed first, then each view's
//! specification and mapping; each mapping by its type's grammar, with the
//! element's own header. Its id is the element's (or its class's path, `::`
//! made `_`), then `_Main` or `_Aggregate_<index>`. A model-to-model main
//! mapping's property mappings are the element's too, reduced to their
//! property, source and target.
//!
//! The walker positions a nested mapping's own span and class from the
//! element's place in the section's text, through the element's body's
//! walker, and a specification's lines from its `~modelOperation`: those
//! positions are reproduced.

use std::any::Any;

use legend_pure_parser_ast::SourceOffset;
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{Name, eat_command, expect_word, identifier, unexpected};
use legend_pure_parser_parser::{Block, ParserContext};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::convert::convert_expression_typed;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::value_spec::{LambdaFunction, ValueSpecification};
use serde_json::{Value, json};
use smol_str::SmolStr;

use crate::ast::{MappingElement, MappingElementBody, MappingElementHeader};
use crate::grammar::{
    ElementSpan, MappingElementGrammar, MappingElementKind, linked_mapping_element_types,
    mapping_element_grammar,
};
use crate::section::separated;

/// The element type this grammar reads.
pub const TYPE_NAME: &str = "AggregationAware";

/// An aggregation aware class mapping's body.
#[derive(Debug, Clone, PartialEq)]
pub struct AggregationAwareBody {
    /// The aggregate views.
    pub views: Vec<AggregateView>,
    /// The main mapping.
    pub main: NestedMapping,
}

/// `( ~modelOperation: { … }, ~aggregateMapping: Type { … } )`.
#[derive(Debug, Clone, PartialEq)]
pub struct AggregateView {
    /// What the view can answer.
    pub specification: AggregateSpecification,
    /// The view's mapping.
    pub mapping: NestedMapping,
}

/// `~canAggregate …, ~groupByFunctions(…), ~aggregateValues(…)`.
#[derive(Debug, Clone, PartialEq)]
pub struct AggregateSpecification {
    /// `~canAggregate`.
    pub can_aggregate: bool,
    /// The group by functions.
    pub group_by: Vec<Expression>,
    /// The aggregate values: each map function and aggregate function.
    pub aggregate_values: Vec<(Expression, Expression)>,
    /// The lines the walker moves its expressions up by: from
    /// `~modelOperation` down to the `{` of the specification.
    pub line_shift: u32,
}

/// A class mapping inside the element, of its own type.
#[derive(Debug, Clone)]
pub struct NestedMapping {
    /// Its type.
    pub type_name: SmolStr,
    /// Its body, as its type's grammar reads it.
    pub body: Box<dyn MappingElementBody>,
    /// Its span, as the walker positions it.
    pub source_info: SourceInfo,
    /// Its class's span, as the walker positions it.
    pub class_source_info: SourceInfo,
}

impl PartialEq for NestedMapping {
    fn eq(&self, other: &Self) -> bool {
        self.type_name == other.type_name
            && *self.body == *other.body
            && self.source_info == other.source_info
            && self.class_source_info == other.class_source_info
    }
}

impl MappingElementBody for AggregationAwareBody {
    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn MappingElementBody> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn MappingElementBody) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}

/// The aggregation aware class mapping grammar.
pub struct AggregationAwareGrammar;

impl MappingElementGrammar for AggregationAwareGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(
        &self,
        header: &MappingElementHeader,
        span: &ElementSpan,
        ctx: &mut ParserContext<'_>,
    ) -> Result<Box<dyn MappingElementBody>, ParseError> {
        // The grammar: the views and the main mapping, their bodies read.
        expect_word(ctx, "Views")?;
        ctx.cursor().expect(TokenKind::Colon)?;
        ctx.cursor().expect(TokenKind::LBracket)?;
        let views = separated(ctx, TokenKind::RBracket, read_view)?;
        if views.is_empty() {
            return Err(unexpected(ctx.cursor().peek()));
        }
        ctx.cursor().expect(TokenKind::RBracket)?;
        ctx.cursor().expect(TokenKind::Comma)?;
        let main = read_mapping(ctx, "mainMapping")?;
        // The walk: the main mapping, then each view.
        let main = nested_mapping(ctx, header, span, &main)?;
        let views = views
            .into_iter()
            .map(|view| {
                let specification = ctx.parse_block(&view.specification, |spec| {
                    specification(spec, view.line_shift)
                })?;
                let mapping = nested_mapping(ctx, header, span, &view.mapping)?;
                Ok(AggregateView {
                    specification,
                    mapping,
                })
            })
            .collect::<Result<_, ParseError>>()?;
        Ok(Box::new(AggregationAwareBody { views, main }))
    }

    fn convert(
        &self,
        element: &MappingElement,
    ) -> Result<(MappingElementKind, Value), ConvertError> {
        let body = element
            .body
            .as_any()
            .downcast_ref::<AggregationAwareBody>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "AggregationAwareGrammar: the body is not an aggregation aware class mapping's",
                    Some(&element.source_info),
                )
            })?;
        let header = &element.header;
        let id = header.id.as_ref().map_or_else(
            || header.target.path.replace("::", "_"),
            |id| id.text.to_string(),
        );
        let main = nested_json(element, &body.main, &format!("{id}_Main"))?;
        let mut json = json!({
            "_type": "aggregationAware",
            "class": header.target.path,
            "id": id,
            "root": header.root,
            "aggregateSetImplementations": body
                .views
                .iter()
                .enumerate()
                .map(|(index, view)| {
                    Ok(json!({
                        "index": index,
                        "aggregateSpecification": specification_json(&view.specification)?,
                        "setImplementation": nested_json(element, &view.mapping, &format!("{id}_Aggregate_{index}"))?,
                    }))
                })
                .collect::<Result<Vec<_>, ConvertError>>()?,
            "sourceInformation": position(&element.source_info),
        });
        // A model-to-model main mapping's property mappings.
        if main["_type"] == "pureInstance" {
            let mappings: Vec<Value> = main["propertyMappings"]
                .as_array()
                .into_iter()
                .flatten()
                .map(|mapping| {
                    let mut json = json!({
                        "_type": "AggregationAwarePropertyMapping",
                        "property": mapping["property"],
                        "source": mapping["source"],
                        "sourceInformation": mapping["sourceInformation"],
                    });
                    if let Some(target) = mapping.get("target") {
                        json["target"] = target.clone();
                    }
                    json
                })
                .collect();
            json["propertyMappings"] = json!(mappings);
        }
        json["mainSetImplementation"] = main;
        if let Some(extends) = &header.extends {
            json["extendsClassMappingId"] = json!(extends.text.as_str());
        }
        Ok((MappingElementKind::Class, json))
    }
}

/// A view as the grammar reads it.
struct ReadView {
    specification: Block,
    line_shift: u32,
    mapping: ReadMapping,
}

/// `~name: Type { … }` as the grammar reads it.
struct ReadMapping {
    type_name: Name,
    body: Block,
}

/// `( ~modelOperation: { … }, ~aggregateMapping: Type { … } )`.
fn read_view(ctx: &mut ParserContext<'_>) -> Result<ReadView, ParseError> {
    ctx.cursor().expect(TokenKind::LParen)?;
    let command = ctx.cursor().current_source_info();
    command_colon(ctx, "modelOperation")?;
    let specification = ctx.read_block()?;
    let line_shift = specification.source_info.start_line - command.start_line;
    ctx.cursor().expect(TokenKind::Comma)?;
    let mapping = read_mapping(ctx, "aggregateMapping")?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(ReadView {
        specification,
        line_shift,
        mapping,
    })
}

/// `~name: Type { … }`.
fn read_mapping(ctx: &mut ParserContext<'_>, command: &str) -> Result<ReadMapping, ParseError> {
    command_colon(ctx, command)?;
    let type_name = identifier(ctx)?;
    let body = ctx.read_block()?;
    Ok(ReadMapping { type_name, body })
}

/// `~name:`.
fn command_colon(ctx: &mut ParserContext<'_>, command: &str) -> Result<(), ParseError> {
    if !eat_command(ctx, command) {
        return Err(unexpected(ctx.cursor().peek()));
    }
    ctx.cursor().expect(TokenKind::Colon)?;
    Ok(())
}

/// A nested mapping, read by its type's grammar with the element's header.
fn nested_mapping(
    ctx: &mut ParserContext<'_>,
    header: &MappingElementHeader,
    span: &ElementSpan,
    read: &ReadMapping,
) -> Result<NestedMapping, ParseError> {
    let type_name = &read.type_name.text;
    let Some(grammar) = mapping_element_grammar(type_name) else {
        return Err(ParseError::Unsupported {
            message: format!(
                "no mapping element grammar is linked for '{type_name}'; the linked ones are: {}",
                linked_mapping_element_types()
            ),
            source_info: read.type_name.source_info.clone(),
        });
    };
    let section = ctx.source_offset();
    let nested_span = ElementSpan {
        element: walker_position(&span.element, section, &span.body),
        body: read.body.source_info.clone(),
    };
    let body = ctx.parse_block(&read.body, |nested| {
        grammar.parse(header, &nested_span, nested)
    })?;
    Ok(NestedMapping {
        type_name: type_name.clone(),
        body,
        source_info: nested_span.element,
        class_source_info: walker_position(&header.target.source_info, section, &span.body),
    })
}

/// Where the walker positions a part of the element for a nested mapping:
/// the part's place in the section's own text (the document less
/// `section`), through the element's body's walker (`body`, from `{`). The
/// lines move down by the body's line, less one; on the text's first line,
/// the columns are the body's instead of the section's.
fn walker_position(span: &SourceInfo, section: SourceOffset, body: &SourceInfo) -> SourceInfo {
    let move_to = |line: u32, column: u32| {
        let text_line = i64::from(line) - i64::from(section.line());
        let line = text_line + i64::from(body.start_line) - 1;
        let column = if text_line == 1 {
            i64::from(column) - i64::from(section.column()) + i64::from(body.start_column)
        } else {
            i64::from(column)
        };
        (
            u32::try_from(line).unwrap_or_default(),
            u32::try_from(column).unwrap_or_default(),
        )
    };
    let (start_line, start_column) = move_to(span.start_line, span.start_column);
    let (end_line, end_column) = move_to(span.end_line, span.end_column);
    SourceInfo {
        start_line,
        start_column,
        end_line,
        end_column,
        ..span.clone()
    }
}

/// `~canAggregate …, ~groupByFunctions(…), ~aggregateValues(…)`.
// `ParserContext::parse_expression` itself is not general enough over the
// context's lifetime, so the closure stays.
#[allow(clippy::redundant_closure_for_method_calls)]
fn specification(
    ctx: &mut ParserContext<'_>,
    line_shift: u32,
) -> Result<AggregateSpecification, ParseError> {
    if !eat_command(ctx, "canAggregate") {
        return Err(unexpected(ctx.cursor().peek()));
    }
    let can_aggregate = match ctx.cursor().peek_kind() {
        TokenKind::True => true,
        TokenKind::False => false,
        _ => return Err(unexpected(ctx.cursor().peek())),
    };
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::Comma)?;
    if !eat_command(ctx, "groupByFunctions") {
        return Err(unexpected(ctx.cursor().peek()));
    }
    ctx.cursor().expect(TokenKind::LParen)?;
    let group_by = separated(ctx, TokenKind::RParen, |ctx| ctx.parse_expression())?;
    ctx.cursor().expect(TokenKind::RParen)?;
    ctx.cursor().expect(TokenKind::Comma)?;
    if !eat_command(ctx, "aggregateValues") {
        return Err(unexpected(ctx.cursor().peek()));
    }
    ctx.cursor().expect(TokenKind::LParen)?;
    let aggregate_values = separated(ctx, TokenKind::RParen, |ctx| {
        ctx.cursor().expect(TokenKind::LParen)?;
        command_colon(ctx, "mapFn")?;
        let map = ctx.parse_expression()?;
        ctx.cursor().expect(TokenKind::Comma)?;
        command_colon(ctx, "aggregateFn")?;
        let aggregate = ctx.parse_expression()?;
        ctx.cursor().expect(TokenKind::RParen)?;
        Ok((map, aggregate))
    })?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(AggregateSpecification {
        can_aggregate,
        group_by,
        aggregate_values,
        line_shift,
    })
}

// ── The protocol ───────────────────────────────────────────────────────

fn position(source_info: &SourceInfo) -> Value {
    json!(SourceInformation::from(source_info))
}

/// A nested mapping's JSON, converted by its type's grammar with the
/// element's header, positioned as the walker positions it, and named `id`.
fn nested_json(
    element: &MappingElement,
    nested: &NestedMapping,
    id: &str,
) -> Result<Value, ConvertError> {
    let grammar = mapping_element_grammar(&nested.type_name).ok_or_else(|| {
        ConvertError::unsupported(
            format!(
                "no mapping element grammar is linked for '{}'",
                nested.type_name
            ),
            &nested.source_info,
        )
    })?;
    let mut header = element.header.clone();
    header.target.source_info = nested.class_source_info.clone();
    let (_, mut json) = grammar.convert(&MappingElement {
        header,
        body: nested.body.clone(),
        source_info: nested.source_info.clone(),
    })?;
    json["id"] = json!(id);
    Ok(json)
}

/// The engine's `AggregateSpecification`.
fn specification_json(specification: &AggregateSpecification) -> Result<Value, ConvertError> {
    let lambda = |expression: &Expression| -> Result<Value, ConvertError> {
        let mut json = serde_json::to_value(ValueSpecification::Lambda(LambdaFunction {
            body: vec![convert_expression_typed(expression)?],
            parameters: Vec::new(),
            source_information: None,
        }))?;
        move_lines_up(&mut json, specification.line_shift);
        Ok(json)
    };
    Ok(json!({
        "canAggregate": specification.can_aggregate,
        "groupByFunctions": specification
            .group_by
            .iter()
            .map(|expression| Ok(json!({"groupByFn": lambda(expression)?})))
            .collect::<Result<Vec<_>, ConvertError>>()?,
        "aggregateValues": specification
            .aggregate_values
            .iter()
            .map(|(map, aggregate)| Ok(json!({"mapFn": lambda(map)?, "aggregateFn": lambda(aggregate)?})))
            .collect::<Result<Vec<_>, ConvertError>>()?,
    }))
}

/// Moves every position in `json` `lines` lines up.
fn move_lines_up(json: &mut Value, lines: u32) {
    if lines == 0 {
        return;
    }
    match json {
        Value::Object(map) => {
            for (key, value) in map.iter_mut() {
                match (key.as_str(), value.as_u64()) {
                    ("startLine" | "endLine", Some(line)) => {
                        *value = json!(line.saturating_sub(u64::from(lines)));
                    }
                    _ => move_lines_up(value, lines),
                }
            }
        }
        Value::Array(items) => items.iter_mut().for_each(|item| move_lines_up(item, lines)),
        _ => {}
    }
}
