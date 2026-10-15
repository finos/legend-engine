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

//! The engine core's model-to-model class mappings:
//! `*my::Person[p]: Pure { ~src my::_Person ~filter … name: $src.fullName }`
//! (`PureInstanceClassMappingParserGrammar`,
//! `PureInstanceClassMappingParseTreeWalker`), the engine's
//! `PureInstanceClassMapping`.
//!
//! ```text
//! pureInstanceClassMapping:  (~src class | ~filter expression)* (property (, property)*)?
//! property:                  (+ name: Type[multiplicity] | name ([source (, target)?])?) *?
//!                            : (EnumerationMapping id:)? expression
//! ```
//!
//! The walker wraps the filter and each property's expression in a lambda
//! without parameters, unpositioned. A property mapping's `source` is the
//! class mapping's id, and its `target` the first id between its brackets.
//! A filter's expression ends where the property mappings start: the
//! grammar reads it only as far as the rest of the body still parses.

use std::any::Any;

use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Multiplicity;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::cursor::Cursor;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::type_text;
use legend_pure_parser_parser::words::{
    Name, QualifiedName, adjacent, eat_command, identifier, is_string, is_word, is_word_kind,
    qualified_name,
};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::convert::convert_expression_typed;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::value_spec::{LambdaFunction, ValueSpecification};
use serde::Serialize;

use crate::ast::{MappingElement, MappingElementBody, MappingElementHeader};
use crate::grammar::{ElementSpan, MappingElementGrammar, MappingElementKind};
use crate::section::{expression_block, separated};
use serde_json::value::{RawValue, to_raw_value};

/// The element type this grammar reads.
pub const TYPE_NAME: &str = "Pure";

/// A model-to-model class mapping's body.
#[derive(Debug, Clone, PartialEq)]
pub struct PureInstanceBody {
    /// `~src my::_Person`: the source class.
    pub source_class: Option<QualifiedName>,
    /// `~filter …`: the source instances mapped.
    pub filter: Option<Expression>,
    /// The property mappings.
    pub property_mappings: Vec<PurePropertyMapping>,
}

/// `name: expression`, a property's value from the source.
#[derive(Debug, Clone, PartialEq)]
pub struct PurePropertyMapping {
    /// `+name: Type[multiplicity]`: a property of the mapping only.
    pub local: Option<LocalProperty>,
    /// The property.
    pub property: QualifiedName,
    /// `[source, target]`: the first id, the target class mapping's.
    pub target: Option<QualifiedName>,
    /// `*`: one instance per value.
    pub explode: bool,
    /// `EnumerationMapping id:`: the enumeration mapping of the value.
    pub enumeration_mapping: Option<Name>,
    /// The value.
    pub transform: Expression,
    /// From `+` or the property to the end of the value.
    pub source_info: SourceInfo,
}

/// A local property's type and multiplicity.
#[derive(Debug, Clone, PartialEq)]
pub struct LocalProperty {
    /// The type as written, without spaces.
    pub type_text: String,
    /// The multiplicity.
    pub multiplicity: Multiplicity,
}

impl MappingElementBody for PureInstanceBody {
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

/// The model-to-model class mapping grammar.
pub struct PureInstanceGrammar;

impl MappingElementGrammar for PureInstanceGrammar {
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
        let (mut sources, mut filters) = (Vec::new(), Vec::new());
        loop {
            if eat_command(ctx, "src") {
                sources.push(qualified_name(ctx)?);
            } else if eat_command(ctx, "filter") {
                let length = filter_length(ctx.cursor());
                let expression = ctx.read_tokens(length);
                filters.push(expression_block(ctx, &expression)?);
            } else {
                break;
            }
        }
        let property_mappings = separated(ctx, TokenKind::Eof, property_mapping)?;
        // The walker's checks, reported at the class mapping.
        Ok(Box::new(PureInstanceBody {
            source_class: at_most_one(sources, "~src", &span.element)?,
            filter: at_most_one(filters, "~filter", &span.element)?,
            property_mappings,
        }))
    }

    fn convert(
        &self,
        element: &MappingElement,
    ) -> Result<(MappingElementKind, Box<RawValue>), ConvertError> {
        let body = element
            .body
            .as_any()
            .downcast_ref::<PureInstanceBody>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "PureInstanceGrammar: the body is not a model-to-model class mapping's",
                    Some(&element.source_info),
                )
            })?;
        let header = &element.header;
        let class = header.target.path.clone();
        let source = header
            .id
            .as_ref()
            .map(|id| id.text.to_string())
            .unwrap_or_default();
        let json = PureInstanceJson {
            id: header.id.as_ref().map(|id| id.text.to_string()),
            class: class.clone(),
            extends_class_mapping_id: header.extends.as_ref().map(|id| id.text.to_string()),
            root: header.root,
            src_class: body.source_class.as_ref().map(|name| name.path.clone()),
            source_class_source_information: body
                .source_class
                .as_ref()
                .map(|name| SourceInformation::from(&name.source_info)),
            filter: body.filter.as_ref().map(lambda).transpose()?,
            property_mappings: body
                .property_mappings
                .iter()
                .map(|mapping| property_mapping_json(mapping, &class, &source))
                .collect::<Result<_, ConvertError>>()?,
            source_information: SourceInformation::from(&element.source_info),
            class_source_information: SourceInformation::from(&header.target.source_info),
        };
        Ok((MappingElementKind::Class, to_raw_value(&json)?))
    }
}

/// The walker's `validateAndExtractOptionalField`.
fn at_most_one<T>(
    mut values: Vec<T>,
    field: &str,
    span: &SourceInfo,
) -> Result<Option<T>, ParseError> {
    match values.len() {
        0 => Ok(None),
        1 => Ok(Some(values.remove(0))),
        _ => Err(ParseError::Unexpected {
            message: format!("Field '{field}' should be specified only once"),
            source_info: span.clone(),
        }),
    }
}

/// How many tokens a filter's expression has: up to the next `~src` or
/// `~filter`, or the first property mapping, outside brackets.
fn filter_length(cursor: &Cursor) -> usize {
    let (mut at, mut depth) = (0_usize, 0_usize);
    loop {
        let kind = cursor.peek_kind_at(at);
        if kind == TokenKind::Eof
            || (depth == 0
                && at > 0
                && (at_property_mapping(cursor, at) || at_source_or_filter(cursor, at)))
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

/// Whether `~src` or `~filter` is `at` tokens ahead.
fn at_source_or_filter(cursor: &Cursor, at: usize) -> bool {
    let (tilde, name) = (cursor.peek_at(at), cursor.peek_at(at + 1));
    tilde.kind == TokenKind::Tilde
        && (is_word(name, "src") || is_word(name, "filter"))
        && adjacent(tilde, name)
}

/// Whether a property mapping starts `at` tokens ahead:
/// `+`? name (`::` name)* (`[` … `]`)? `*`? `:`.
fn at_property_mapping(cursor: &Cursor, mut at: usize) -> bool {
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
    if cursor.peek_kind_at(at) == TokenKind::LBracket {
        while !matches!(
            cursor.peek_kind_at(at),
            TokenKind::RBracket | TokenKind::Eof
        ) {
            at += 1;
        }
        at += 1;
    }
    if cursor.peek_kind_at(at) == TokenKind::Star {
        at += 1;
    }
    cursor.peek_kind_at(at) == TokenKind::Colon
}

/// `(+ name: Type[m] | name ([source (, target)?])?) *? : (EnumerationMapping id:)? expression`.
fn property_mapping(ctx: &mut ParserContext<'_>) -> Result<PurePropertyMapping, ParseError> {
    let start = ctx.cursor().current_source_info();
    let (local, property, target) = if ctx.cursor().eat(TokenKind::Plus) {
        let property = qualified_name(ctx)?;
        ctx.cursor().expect(TokenKind::Colon)?;
        let type_text = type_text(ctx)?;
        ctx.cursor().expect(TokenKind::LBracket)?;
        let multiplicity = ctx.parse_multiplicity()?;
        ctx.cursor().expect(TokenKind::RBracket)?;
        let local = LocalProperty {
            type_text,
            multiplicity,
        };
        (Some(local), property, None)
    } else {
        let property = qualified_name(ctx)?;
        let target = if ctx.cursor().eat(TokenKind::LBracket) {
            let source = qualified_name(ctx)?;
            if ctx.cursor().eat(TokenKind::Comma) {
                qualified_name(ctx)?;
            }
            ctx.cursor().expect(TokenKind::RBracket)?;
            Some(source)
        } else {
            None
        };
        (None, property, target)
    };
    let explode = ctx.cursor().eat(TokenKind::Star);
    ctx.cursor().expect(TokenKind::Colon)?;
    let enumeration_mapping = {
        let cursor = ctx.cursor();
        if is_word(cursor.peek(), "EnumerationMapping")
            && cursor.peek_kind_at(2) == TokenKind::Colon
        {
            cursor.advance();
            let id = identifier(ctx)?;
            ctx.cursor().expect(TokenKind::Colon)?;
            Some(id)
        } else {
            None
        }
    };
    let transform = ctx.parse_expression()?;
    Ok(PurePropertyMapping {
        local,
        property,
        target,
        explode,
        enumeration_mapping,
        transform,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// An expression as the walker makes it a lambda: the body of one without
/// parameters, unpositioned.
fn lambda(expression: &Expression) -> Result<ValueSpecification, ConvertError> {
    Ok(ValueSpecification::Lambda(LambdaFunction {
        body: vec![convert_expression_typed(expression)?],
        parameters: Vec::new(),
        source_information: None,
    }))
}

fn property_mapping_json(
    mapping: &PurePropertyMapping,
    class: &str,
    source: &str,
) -> Result<PurePropertyMappingJson, ConvertError> {
    let property_source_information = SourceInformation::from(&mapping.property.source_info);
    Ok(PurePropertyMappingJson {
        property: PropertyPointerJson {
            // A local property belongs to no class.
            class: mapping.local.is_none().then(|| class.to_string()),
            property: mapping.property.path.clone(),
            source_information: property_source_information.clone(),
        },
        source: source.to_string(),
        target: mapping.target.as_ref().map(|target| target.path.clone()),
        local_mapping_property: mapping
            .local
            .as_ref()
            .map(|local| LocalMappingPropertyJson {
                type_: local.type_text.clone(),
                multiplicity: (&local.multiplicity).into(),
                source_information: property_source_information,
            }),
        enum_mapping_id: mapping
            .enumeration_mapping
            .as_ref()
            .map(|id| id.value.to_string()),
        transform: lambda(&mapping.transform)?,
        explode_property: mapping.explode,
        source_information: SourceInformation::from(&mapping.source_info),
    })
}

/// The engine's `PureInstanceClassMapping`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "pureInstance", rename_all = "camelCase")]
struct PureInstanceJson {
    #[serde(skip_serializing_if = "Option::is_none")]
    id: Option<String>,
    class: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    extends_class_mapping_id: Option<String>,
    root: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    src_class: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    source_class_source_information: Option<SourceInformation>,
    #[serde(skip_serializing_if = "Option::is_none")]
    filter: Option<ValueSpecification>,
    property_mappings: Vec<PurePropertyMappingJson>,
    source_information: SourceInformation,
    class_source_information: SourceInformation,
}

/// The engine's `PurePropertyMapping`.
#[derive(Serialize)]
#[serde(
    tag = "_type",
    rename = "purePropertyMapping",
    rename_all = "camelCase"
)]
struct PurePropertyMappingJson {
    property: PropertyPointerJson,
    source: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    target: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    local_mapping_property: Option<LocalMappingPropertyJson>,
    #[serde(skip_serializing_if = "Option::is_none")]
    enum_mapping_id: Option<String>,
    transform: ValueSpecification,
    explode_property: bool,
    source_information: SourceInformation,
}

/// The engine's `PropertyPointer`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct PropertyPointerJson {
    #[serde(skip_serializing_if = "Option::is_none")]
    class: Option<String>,
    property: String,
    source_information: SourceInformation,
}

/// The engine's `LocalMappingPropertyInfo`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct LocalMappingPropertyJson {
    #[serde(rename = "type")]
    type_: String,
    multiplicity: legend_pure_parser_protocol::v1::multiplicity::Multiplicity,
    source_information: SourceInformation,
}
