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

//! The engine core's enumeration mappings:
//! `my::E: EnumerationMapping id { A: 'a', B: ['b', 1, my::Other.X] }`
//! (`EnumerationMappingParserGrammar`, `EnumerationMappingParseTreeWalker`),
//! the engine's `EnumerationMapping`.
//!
//! ```text
//! enumerationMapping:  (entry (, entry)*)?
//! entry:               value : (source | [ source (, source)* ])
//! source:              'string' | integer | my::Enum.VALUE
//! ```
//!
//! An integer may be signed, its sign written right before it: the
//! grammar's lexer reads `-1` as one integer. The element's id is the name
//! written after its type; its `[id]`, `extends` and `*` are not read.

use std::any::Any;

use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{
    Name, adjacent, identifier, is_string, qualified_name, unexpected,
};
use legend_pure_parser_parser::{ParserContext, string_value};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::element::PackageableElementPointer;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde::Serialize;
use smol_str::SmolStr;

use crate::ast::{MappingElement, MappingElementBody, MappingElementHeader};
use crate::grammar::{ElementSpan, MappingElementGrammar, MappingElementKind};
use crate::section::separated;
use serde_json::value::{RawValue, to_raw_value};

/// The element type this grammar reads.
pub const TYPE_NAME: &str = "EnumerationMapping";

/// An enumeration mapping's body: each enum value's source values.
#[derive(Debug, Clone, PartialEq)]
pub struct EnumerationMappingBody {
    /// The entries, one per enum value.
    pub values: Vec<EnumValueMapping>,
}

/// `A: 'a'` or `A: ['a', 1]`: an enum value and the values mapped to it.
#[derive(Debug, Clone, PartialEq)]
pub struct EnumValueMapping {
    /// The enum value.
    pub enum_value: Name,
    /// The source values.
    pub source_values: Vec<SourceValue>,
}

/// A value mapped to an enum value.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum SourceValue {
    /// `'text'`, unescaped.
    String(String),
    /// An integer, sign included.
    Integer(i32),
    /// `my::Enum.VALUE`.
    Enum {
        /// The enumeration's path, unquoted.
        enumeration: String,
        /// The value, unquoted.
        value: SmolStr,
    },
}

impl MappingElementBody for EnumerationMappingBody {
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

/// The enumeration mapping grammar.
pub struct EnumerationMappingGrammar;

impl MappingElementGrammar for EnumerationMappingGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(
        &self,
        _header: &MappingElementHeader,
        _span: &ElementSpan,
        ctx: &mut ParserContext<'_>,
    ) -> Result<Box<dyn MappingElementBody>, ParseError> {
        let values = separated(ctx, TokenKind::Eof, enum_value_mapping)?;
        Ok(Box::new(EnumerationMappingBody { values }))
    }

    fn convert(
        &self,
        element: &MappingElement,
    ) -> Result<(MappingElementKind, Box<RawValue>), ConvertError> {
        let body = element
            .body
            .as_any()
            .downcast_ref::<EnumerationMappingBody>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "EnumerationMappingGrammar: the body is not an enumeration mapping's",
                    Some(&element.source_info),
                )
            })?;
        let header = &element.header;
        let json = EnumerationMappingJson {
            id: header.name.as_ref().map(|name| name.text.to_string()),
            enumeration: PackageableElementPointer {
                element_type: Some("ENUMERATION".to_string()),
                path: header.target.path.clone(),
                source_information: Some(SourceInformation::from(&header.target.source_info)),
            },
            enum_value_mappings: body
                .values
                .iter()
                .map(|mapping| EnumValueMappingJson {
                    enum_value: mapping.enum_value.value.to_string(),
                    source_values: mapping
                        .source_values
                        .iter()
                        .map(|value| match value {
                            SourceValue::String(value) => SourceValueJson::String {
                                value: value.clone(),
                            },
                            SourceValue::Integer(value) => {
                                SourceValueJson::Integer { value: *value }
                            }
                            SourceValue::Enum { enumeration, value } => SourceValueJson::Enum {
                                enumeration: enumeration.clone(),
                                value: value.to_string(),
                            },
                        })
                        .collect(),
                })
                .collect(),
            source_information: SourceInformation::from(&element.source_info),
        };
        Ok((MappingElementKind::Enumeration, to_raw_value(&json)?))
    }
}

/// `value : (source | [source, …])`.
fn enum_value_mapping(ctx: &mut ParserContext<'_>) -> Result<EnumValueMapping, ParseError> {
    let enum_value = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let source_values = if ctx.cursor().eat(TokenKind::LBracket) {
        let values = separated(ctx, TokenKind::RBracket, source_value)?;
        // At least one.
        if values.is_empty() {
            return Err(unexpected(ctx.cursor().peek()));
        }
        ctx.cursor().expect(TokenKind::RBracket)?;
        values
    } else {
        vec![source_value(ctx)?]
    };
    Ok(EnumValueMapping {
        enum_value,
        source_values,
    })
}

/// `'string'`, an integer, or `my::Enum.VALUE`.
fn source_value(ctx: &mut ParserContext<'_>) -> Result<SourceValue, ParseError> {
    let token = ctx.cursor().peek().clone();
    if is_string(&token) {
        ctx.cursor().advance();
        let (value, _) = string_value(&token.text).map_err(|message| ParseError::Unexpected {
            message,
            source_info: token.source_info.clone(),
        })?;
        return Ok(SourceValue::String(value));
    }
    let signed = matches!(token.kind, TokenKind::Minus | TokenKind::Plus)
        && ctx.cursor().peek_kind_at(1) == TokenKind::IntegerLiteral
        && adjacent(&token, ctx.cursor().peek_at(1));
    if signed || token.kind == TokenKind::IntegerLiteral {
        let start = ctx.cursor().current_source_info();
        let mut text = String::new();
        if signed {
            text.push_str(&ctx.cursor().advance().text);
        }
        text.push_str(&ctx.cursor().advance().text);
        // Java's `Integer.parseInt`.
        let value = text.parse().map_err(|_| ParseError::Unexpected {
            message: format!("'{text}' is not a 32-bit integer"),
            source_info: ctx.cursor().span_since(&start),
        })?;
        return Ok(SourceValue::Integer(value));
    }
    let enumeration = qualified_name(ctx)?;
    ctx.cursor().expect(TokenKind::Dot)?;
    let value = identifier(ctx)?;
    Ok(SourceValue::Enum {
        enumeration: enumeration.path,
        value: value.value,
    })
}

/// The engine's `EnumerationMapping`. Its values' positions are not
/// serialized (`@JsonIgnore`).
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct EnumerationMappingJson {
    #[serde(skip_serializing_if = "Option::is_none")]
    id: Option<String>,
    enumeration: PackageableElementPointer,
    enum_value_mappings: Vec<EnumValueMappingJson>,
    source_information: SourceInformation,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct EnumValueMappingJson {
    enum_value: String,
    source_values: Vec<SourceValueJson>,
}

#[derive(Serialize)]
#[serde(tag = "_type")]
enum SourceValueJson {
    #[serde(rename = "stringSourceValue")]
    String { value: String },
    #[serde(rename = "integerSourceValue")]
    Integer { value: i32 },
    #[serde(rename = "enumSourceValue")]
    Enum { enumeration: String, value: String },
}
