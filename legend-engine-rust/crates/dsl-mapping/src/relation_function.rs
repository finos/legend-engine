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

//! The engine core's relation class mappings, a class mapped from a relation
//! function's rows:
//! `*my::Person[p]: Relation { ~func my::people():Relation<Any>[1] name: NAME }`
//! (`RelationFunctionMappingParserGrammar`,
//! `RelationFunctionMappingParseTreeWalker`), the engine's
//! `RelationFunctionClassMapping`.
//!
//! ```text
//! relationFunctionMapping:  (~func function():Type[m] | ~src expression)
//!                           (~primaryKey: (name | [name, …]))? (property (, property)*)?
//! property:                 + name: Type[m] value
//!                         | name (value | ( property, … ) | ( ) Inline[id])
//! value:                    : (Binding my::Binding: | EnumerationMapping id:)? (column | expression)
//! ```
//!
//! A value that is a single name is a column; any other is an expression,
//! which the walker makes the body of a lambda without parameters, as it
//! does the `~src` expression. It positions that lambda with the offset of
//! the expression in the element's body counted twice, which is reproduced. That
//! expression ends where a `~primaryKey` or a property mapping starts: the
//! grammar reads it only as far as the rest still parses. A property
//! mapping's source is the class mapping's id; inside an embedded mapping,
//! its properties have no class and no source.

use std::any::Any;

use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Multiplicity;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::cursor::Cursor;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{
    QualifiedName, adjacent, at_word, eat_command, identifier, is_string, is_word, is_word_kind,
    qualified_name, unexpected,
};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::convert::convert_expression_typed;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::value_spec::{LambdaFunction, ValueSpecification};
use serde_json::{Value, json};
use smol_str::SmolStr;

use crate::ast::{MappingElement, MappingElementBody, MappingElementHeader};
use crate::grammar::{ElementSpan, MappingElementGrammar, MappingElementKind};
use crate::pure_instance::{balanced, type_text};
use crate::section::{expression_block, separated};

/// The element type this grammar reads.
pub const TYPE_NAME: &str = "Relation";

/// A relation class mapping's body.
#[derive(Debug, Clone, PartialEq)]
pub struct RelationFunctionBody {
    /// Where the rows come from.
    pub source: RelationSource,
    /// `~primaryKey`: the key columns, unquoted.
    pub primary_key: Vec<SmolStr>,
    /// The property mappings.
    pub property_mappings: Vec<RelationPropertyMapping>,
}

/// Where a relation class mapping's rows come from.
#[derive(Debug, Clone, PartialEq)]
pub enum RelationSource {
    /// `~func my::f():Relation<Any>[1]`: a function, as written without
    /// spaces, positioned from its name to its multiplicity.
    Function(String, SourceInfo),
    /// `~src expression`, and its lambda's span (see
    /// `walker_lambda_span`).
    Expression(Box<Expression>, SourceInfo),
}

/// A property's mapping.
#[derive(Debug, Clone, PartialEq)]
pub enum RelationPropertyMapping {
    /// `name: value`, or `+name: Type[m] value`.
    Value {
        /// The property.
        property: QualifiedName,
        /// `+`: its type as written, and its multiplicity.
        local: Option<(String, Multiplicity)>,
        /// `Binding my::Binding:` or `EnumerationMapping id:`.
        transformer: Option<Transformer>,
        /// The value.
        value: RelationValue,
        /// From `+` or the property to the end of the value.
        source_info: SourceInfo,
    },
    /// `name ( property, … )`.
    Embedded {
        /// The property.
        property: QualifiedName,
        /// The embedded class's property mappings.
        property_mappings: Vec<RelationPropertyMapping>,
        /// From the property to `)`.
        source_info: SourceInfo,
    },
    /// `name ( ) Inline[id]`.
    Inline {
        /// The property.
        property: QualifiedName,
        /// The class mapping inlined, unquoted.
        id: SmolStr,
        /// From the property to `]`.
        source_info: SourceInfo,
    },
}

/// How a value is transformed.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Transformer {
    /// `Binding my::Binding:`, the binding's path.
    Binding(String),
    /// `EnumerationMapping id:`, the id unquoted.
    Enumeration(SmolStr),
}

/// A property's value.
#[derive(Debug, Clone, PartialEq)]
pub enum RelationValue {
    /// A single name: the column, unquoted.
    Column(SmolStr),
    /// Any other expression, and its lambda's span (see
    /// `walker_lambda_span`).
    Expression(Box<Expression>, SourceInfo),
}

impl MappingElementBody for RelationFunctionBody {
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

/// The relation class mapping grammar.
pub struct RelationFunctionGrammar;

impl MappingElementGrammar for RelationFunctionGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(
        &self,
        _header: &MappingElementHeader,
        span: &ElementSpan,
        ctx: &mut ParserContext<'_>,
    ) -> Result<Box<dyn MappingElementBody>, ParseError> {
        let body = &span.body;
        let source = if eat_command(ctx, "func") {
            let start = ctx.cursor().current_source_info();
            let text = function_identifier(ctx)?;
            RelationSource::Function(text, ctx.cursor().span_since(&start))
        } else if eat_command(ctx, "src") {
            let start = ctx.cursor().current_source_info();
            let length = source_length(ctx.cursor());
            let block = ctx.read_tokens(length);
            let expression = expression_block(ctx, &block)?;
            RelationSource::Expression(
                Box::new(expression),
                walker_lambda_span(&ctx.cursor().span_since(&start), body),
            )
        } else {
            return Err(unexpected(ctx.cursor().peek()));
        };
        let primary_key = if eat_command(ctx, "primaryKey") {
            ctx.cursor().expect(TokenKind::Colon)?;
            if ctx.cursor().eat(TokenKind::LBracket) {
                let keys = separated(ctx, TokenKind::RBracket, |ctx| Ok(identifier(ctx)?.value))?;
                if keys.is_empty() {
                    return Err(unexpected(ctx.cursor().peek()));
                }
                ctx.cursor().expect(TokenKind::RBracket)?;
                keys
            } else {
                vec![identifier(ctx)?.value]
            }
        } else {
            Vec::new()
        };
        let property_mappings = separated(ctx, TokenKind::Eof, |ctx| property_mapping(ctx, body))?;
        Ok(Box::new(RelationFunctionBody {
            source,
            primary_key,
            property_mappings,
        }))
    }

    fn convert(
        &self,
        element: &MappingElement,
    ) -> Result<(MappingElementKind, Value), ConvertError> {
        let body = element
            .body
            .as_any()
            .downcast_ref::<RelationFunctionBody>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "RelationFunctionGrammar: the body is not a relation class mapping's",
                    Some(&element.source_info),
                )
            })?;
        let header = &element.header;
        let source = header.id.as_ref().map(|id| id.text.to_string());
        let mut json = json!({
            "_type": "relation",
            "class": header.target.path,
            "root": header.root,
            "primaryKey": body.primary_key.iter().map(SmolStr::as_str).collect::<Vec<_>>(),
            "propertyMappings": body
                .property_mappings
                .iter()
                .map(|mapping| property_mapping_json(mapping, Some(&header.target.path), source.as_deref()))
                .collect::<Result<Vec<_>, ConvertError>>()?,
            "sourceInformation": position(&element.source_info),
        });
        match &body.source {
            RelationSource::Function(path, source_info) => {
                json["relationFunction"] = json!({
                    "type": "FUNCTION",
                    "path": path,
                    "sourceInformation": position(source_info),
                });
            }
            RelationSource::Expression(expression, span) => {
                json["sourceLambda"] = lambda(expression, span)?;
            }
        }
        if let Some(id) = &header.id {
            json["id"] = json!(id.text.as_str());
        }
        if let Some(extends) = &header.extends {
            json["extendsClassMappingId"] = json!(extends.text.as_str());
        }
        Ok((MappingElementKind::Class, json))
    }
}

/// `my::f(Type[m], …):Type[m]`, as written without spaces (the grammar's
/// `functionIdentifier`).
fn function_identifier(ctx: &mut ParserContext<'_>) -> Result<String, ParseError> {
    let mut text = qualified_name(ctx)?.text;
    if !ctx.cursor().check(TokenKind::LParen) {
        return Err(unexpected(ctx.cursor().peek()));
    }
    balanced(ctx, &mut text)?;
    let colon = ctx.cursor().expect(TokenKind::Colon)?;
    text.push_str(&colon.text);
    text.push_str(&type_text(ctx)?);
    if !ctx.cursor().check(TokenKind::LBracket) {
        return Err(unexpected(ctx.cursor().peek()));
    }
    balanced(ctx, &mut text)?;
    Ok(text)
}

/// How many tokens a `~src` expression has: up to `~primaryKey`, or the
/// first property mapping, outside brackets.
fn source_length(cursor: &Cursor) -> usize {
    let (mut at, mut depth) = (0_usize, 0_usize);
    loop {
        let kind = cursor.peek_kind_at(at);
        if kind == TokenKind::Eof
            || (depth == 0 && at > 0 && (at_primary_key(cursor, at) || at_property(cursor, at)))
        {
            return at;
        }
        match kind {
            TokenKind::LParen | TokenKind::LBracket | TokenKind::LBrace => depth += 1,
            TokenKind::RParen | TokenKind::RBracket | TokenKind::RBrace => {
                depth = depth.saturating_sub(1);
            }
            _ => {}
        }
        at += 1;
    }
}

/// Whether `~primaryKey` is `at` tokens ahead.
fn at_primary_key(cursor: &Cursor, at: usize) -> bool {
    let (tilde, name) = (cursor.peek_at(at), cursor.peek_at(at + 1));
    tilde.kind == TokenKind::Tilde && is_word(name, "primaryKey") && adjacent(tilde, name)
}

/// Whether a value's property mapping, `+`? name (`::` name)* `:`, starts
/// `at` tokens ahead.
fn at_property(cursor: &Cursor, mut at: usize) -> bool {
    let name = |token: &Token| is_word_kind(token.kind) || is_string(token);
    if cursor.peek_kind_at(at) == TokenKind::Plus {
        at += 1;
    }
    if !name(cursor.peek_at(at)) {
        return false;
    }
    at += 1;
    while cursor.peek_kind_at(at) == TokenKind::PathSep && name(cursor.peek_at(at + 1)) {
        at += 2;
    }
    cursor.peek_kind_at(at) == TokenKind::Colon
}

/// A property mapping, in the element body `body`.
fn property_mapping(
    ctx: &mut ParserContext<'_>,
    body: &SourceInfo,
) -> Result<RelationPropertyMapping, ParseError> {
    let start = ctx.cursor().current_source_info();
    if ctx.cursor().eat(TokenKind::Plus) {
        let property = property_name(ctx)?;
        ctx.cursor().expect(TokenKind::Colon)?;
        let type_text = type_text(ctx)?;
        ctx.cursor().expect(TokenKind::LBracket)?;
        let multiplicity = ctx.parse_multiplicity()?;
        ctx.cursor().expect(TokenKind::RBracket)?;
        let (transformer, value) = value(ctx, body)?;
        return Ok(RelationPropertyMapping::Value {
            property,
            local: Some((type_text, multiplicity)),
            transformer,
            value,
            source_info: ctx.cursor().span_since(&start),
        });
    }
    let property = property_name(ctx)?;
    if ctx.cursor().check(TokenKind::LParen) {
        ctx.cursor().advance();
        if ctx.cursor().eat(TokenKind::RParen) {
            let inline = ctx.cursor().peek().clone();
            if !is_word(&inline, "Inline") {
                return Err(unexpected(&inline));
            }
            ctx.cursor().advance();
            ctx.cursor().expect(TokenKind::LBracket)?;
            let id = identifier(ctx)?.value;
            ctx.cursor().expect(TokenKind::RBracket)?;
            return Ok(RelationPropertyMapping::Inline {
                property,
                id,
                source_info: ctx.cursor().span_since(&start),
            });
        }
        let property_mappings =
            separated(ctx, TokenKind::RParen, |ctx| property_mapping(ctx, body))?;
        ctx.cursor().expect(TokenKind::RParen)?;
        return Ok(RelationPropertyMapping::Embedded {
            property,
            property_mappings,
            source_info: ctx.cursor().span_since(&start),
        });
    }
    let (transformer, value) = value(ctx, body)?;
    Ok(RelationPropertyMapping::Value {
        property,
        local: None,
        transformer,
        value,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// A property's name: a qualified name, or one of the grammar's commands,
/// which its `identifier` rule lists (a `~src` after a `~func` is read as a
/// property).
fn property_name(ctx: &mut ParserContext<'_>) -> Result<QualifiedName, ParseError> {
    let cursor = ctx.cursor();
    let (tilde, name) = (cursor.peek().clone(), cursor.peek_at(1).clone());
    if tilde.kind == TokenKind::Tilde
        && ["func", "src", "primaryKey"]
            .iter()
            .any(|command| is_word(&name, command))
        && adjacent(&tilde, &name)
    {
        cursor.advance();
        cursor.advance();
        let text = format!("~{}", name.text);
        return Ok(QualifiedName {
            path: text.clone(),
            text,
            has_package: false,
            source_info: tilde.source_info.merge(&name.source_info),
        });
    }
    qualified_name(ctx)
}

/// `: (Binding my::Binding: | EnumerationMapping id:)? (column | expression)`.
fn value(
    ctx: &mut ParserContext<'_>,
    body: &SourceInfo,
) -> Result<(Option<Transformer>, RelationValue), ParseError> {
    ctx.cursor().expect(TokenKind::Colon)?;
    let transformer =
        if at_word(ctx, "EnumerationMapping") && ctx.cursor().peek_kind_at(2) == TokenKind::Colon {
            ctx.cursor().advance();
            let id = identifier(ctx)?.value;
            ctx.cursor().expect(TokenKind::Colon)?;
            Some(Transformer::Enumeration(id))
        } else if at_word(ctx, "Binding") && at_property(ctx.cursor(), 1) {
            ctx.cursor().advance();
            let binding = qualified_name(ctx)?.path;
            ctx.cursor().expect(TokenKind::Colon)?;
            Some(Transformer::Binding(binding))
        } else {
            None
        };
    // A single name, then the end of the mapping, is a column.
    let cursor = ctx.cursor();
    let (first, next) = (cursor.peek(), cursor.peek_kind_at(1));
    if (is_word_kind(first.kind) || is_string(first))
        && matches!(next, TokenKind::Comma | TokenKind::RParen | TokenKind::Eof)
    {
        return Ok((transformer, RelationValue::Column(identifier(ctx)?.value)));
    }
    let start = ctx.cursor().current_source_info();
    let expression = ctx.parse_expression()?;
    let span = walker_lambda_span(&ctx.cursor().span_since(&start), body);
    Ok((
        transformer,
        RelationValue::Expression(Box::new(expression), span),
    ))
}

/// An expression's lambda's position, as the walker's
/// `visitInlineExpressionAsLambda` computes it: it positions the expression
/// with a walker already offset to where the expression starts in the
/// element's body, so that offset counts twice. The lines move down by the
/// expression's line in the body, less one; on the body's first line, the
/// columns move right by the expression's column there.
fn walker_lambda_span(span: &SourceInfo, body: &SourceInfo) -> SourceInfo {
    let line_shift = span.start_line - body.start_line;
    // The body starts right after `{`.
    let column_shift = if span.start_line == body.start_line {
        span.start_column - body.start_column - 1
    } else {
        0
    };
    SourceInfo {
        start_line: span.start_line + line_shift,
        start_column: span.start_column + column_shift,
        end_line: span.end_line + line_shift,
        end_column: if span.end_line == body.start_line {
            span.end_column + column_shift
        } else {
            span.end_column
        },
        ..span.clone()
    }
}

// ── The protocol ───────────────────────────────────────────────────────

fn position(source_info: &SourceInfo) -> Value {
    json!(SourceInformation::from(source_info))
}

/// An expression as the walker makes it a lambda: the body of one without
/// parameters, positioned on the expression.
fn lambda(expression: &Expression, span: &SourceInfo) -> Result<Value, ConvertError> {
    Ok(serde_json::to_value(ValueSpecification::Lambda(
        LambdaFunction {
            body: vec![convert_expression_typed(expression)?],
            parameters: Vec::new(),
            source_information: Some(SourceInformation::from(span)),
        },
    ))?)
}

fn property_pointer(property: &QualifiedName, class: Option<&str>) -> Value {
    let mut json = json!({
        "property": property.path,
        "sourceInformation": position(&property.source_info),
    });
    if let Some(class) = class {
        json["class"] = json!(class);
    }
    json
}

/// A property mapping, of the class `class` (none inside an embedded
/// mapping) and from the class mapping `source`.
fn property_mapping_json(
    mapping: &RelationPropertyMapping,
    class: Option<&str>,
    source: Option<&str>,
) -> Result<Value, ConvertError> {
    let mut json = match mapping {
        RelationPropertyMapping::Value {
            property,
            local,
            transformer,
            value,
            source_info,
        } => {
            let mut json = json!({
                "_type": "relationFunctionPropertyMapping",
                "property": property_pointer(property, class),
                "sourceInformation": position(source_info),
            });
            match value {
                RelationValue::Column(column) => json["column"] = json!(column.as_str()),
                RelationValue::Expression(expression, span) => {
                    json["valueFn"] = lambda(expression, span)?;
                }
            }
            if let Some((type_text, multiplicity)) = local {
                json["localMappingProperty"] = json!({
                    "type": type_text,
                    "multiplicity": legend_pure_parser_protocol::v1::multiplicity::Multiplicity::from(multiplicity),
                    "sourceInformation": position(&property.source_info),
                });
            }
            match transformer {
                // Its position is not written (`@JsonIgnore`).
                Some(Transformer::Binding(binding)) => {
                    json["bindingTransformer"] = json!({"binding": binding});
                }
                Some(Transformer::Enumeration(id)) => json["enumMappingId"] = json!(id.as_str()),
                None => {}
            }
            json
        }
        RelationPropertyMapping::Embedded {
            property,
            property_mappings,
            source_info,
        } => json!({
            "_type": "relationFunctionEmbeddedPropertyMapping",
            "property": property_pointer(property, class),
            "propertyMappings": property_mappings
                .iter()
                .map(|mapping| property_mapping_json(mapping, None, None))
                .collect::<Result<Vec<_>, ConvertError>>()?,
            "sourceInformation": position(source_info),
        }),
        RelationPropertyMapping::Inline {
            property,
            id,
            source_info,
        } => json!({
            "_type": "relationFunctionEmbeddedPropertyMapping",
            "property": property_pointer(property, class),
            "id": id.as_str(),
            "propertyMappings": [],
            "sourceInformation": position(source_info),
        }),
    };
    if let Some(source) = source {
        json["source"] = json!(source);
    }
    Ok(json)
}
