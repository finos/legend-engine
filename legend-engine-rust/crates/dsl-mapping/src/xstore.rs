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

//! The engine core's cross-store association mappings:
//! `my::Person_Firm: XStore { employer[p, f]: $this.firmId == $that.id }`
//! (`XStoreAssociationMappingParserGrammar`,
//! `XStoreAssociationMappingParseTreeWalker`), the engine's
//! `XStoreAssociationMapping`.
//!
//! ```text
//! xStoreAssociationMapping:  (property (, property)*)?
//! property:                  name ([source, target])? : expression
//! ```
//!
//! Each expression is the body of an unpositioned lambda without
//! parameters, as the walker makes it; without ids, the source and target
//! are empty.

use std::any::Any;

use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{Name, QualifiedName, qualified_name, word};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::convert::convert_expression_typed;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::value_spec::{LambdaFunction, ValueSpecification};
use serde::Serialize;
use serde_json::value::{RawValue, to_raw_value};

use crate::ast::{MappingElement, MappingElementBody, MappingElementHeader};
use crate::grammar::{ElementSpan, MappingElementGrammar, MappingElementKind};
use crate::section::separated;

/// The element type this grammar reads.
pub const TYPE_NAME: &str = "XStore";

/// A cross-store association mapping's body: its property mappings.
#[derive(Debug, Clone, PartialEq)]
pub struct XStoreBody {
    /// The property mappings.
    pub property_mappings: Vec<XStorePropertyMapping>,
}

/// `name[source, target]: expression`.
#[derive(Debug, Clone, PartialEq)]
pub struct XStorePropertyMapping {
    /// The association's property.
    pub property: QualifiedName,
    /// `[source, target]`: the class mappings' ids, as written.
    pub ids: Option<(Name, Name)>,
    /// The cross expression.
    pub cross_expression: Expression,
    /// From the property to the end of the expression.
    pub source_info: SourceInfo,
}

impl MappingElementBody for XStoreBody {
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

/// The cross-store association mapping grammar.
pub struct XStoreGrammar;

impl MappingElementGrammar for XStoreGrammar {
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
        let property_mappings = separated(ctx, TokenKind::Eof, |ctx| {
            let start = ctx.cursor().current_source_info();
            let property = qualified_name(ctx)?;
            let ids = if ctx.cursor().eat(TokenKind::LBracket) {
                let source = word(ctx)?;
                ctx.cursor().expect(TokenKind::Comma)?;
                let target = word(ctx)?;
                ctx.cursor().expect(TokenKind::RBracket)?;
                Some((source, target))
            } else {
                None
            };
            ctx.cursor().expect(TokenKind::Colon)?;
            let cross_expression = ctx.parse_expression()?;
            Ok(XStorePropertyMapping {
                property,
                ids,
                cross_expression,
                source_info: ctx.cursor().span_since(&start),
            })
        })?;
        Ok(Box::new(XStoreBody { property_mappings }))
    }

    fn convert(
        &self,
        element: &MappingElement,
    ) -> Result<(MappingElementKind, Box<RawValue>), ConvertError> {
        let body = element
            .body
            .as_any()
            .downcast_ref::<XStoreBody>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "XStoreGrammar: the body is not a cross-store association mapping's",
                    Some(&element.source_info),
                )
            })?;
        let association = &element.header.target;
        let property_mappings = body
            .property_mappings
            .iter()
            .map(|mapping| {
                let (source, target) = mapping.ids.as_ref().map_or(("", ""), |(source, target)| {
                    (source.text.as_str(), target.text.as_str())
                });
                Ok(XStorePropertyMappingJson {
                    property: PropertyPointerJson {
                        class: &association.path,
                        property: &mapping.property.path,
                        source_information: SourceInformation::from(&mapping.property.source_info),
                    },
                    source,
                    target,
                    cross_expression: ValueSpecification::Lambda(LambdaFunction {
                        body: vec![convert_expression_typed(&mapping.cross_expression)?],
                        parameters: Vec::new(),
                        source_information: None,
                    }),
                    source_information: SourceInformation::from(&mapping.source_info),
                })
            })
            .collect::<Result<Vec<_>, ConvertError>>()?;
        let mut json = AssociationMappingJson::new("xStore", element);
        json.property_mappings = Some(property_mappings);
        Ok((MappingElementKind::Association, to_raw_value(&json)?))
    }
}

/// The engine's `XStorePropertyMapping`.
#[derive(Serialize)]
#[serde(
    tag = "_type",
    rename = "xStorePropertyMapping",
    rename_all = "camelCase"
)]
pub(crate) struct XStorePropertyMappingJson<'a> {
    property: PropertyPointerJson<'a>,
    source: &'a str,
    target: &'a str,
    cross_expression: ValueSpecification,
    source_information: SourceInformation,
}

/// The engine's `PropertyPointer`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct PropertyPointerJson<'a> {
    class: &'a str,
    property: &'a str,
    source_information: SourceInformation,
}

pub(crate) fn position(source_info: &SourceInfo) -> SourceInformation {
    SourceInformation::from(source_info)
}

/// The engine's `AssociationMapping`s without stores: its association, its
/// id, and its content, cross-store property mappings or a model join's
/// condition.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AssociationMappingJson<'a> {
    #[serde(rename = "_type")]
    mapping_type: &'static str,
    association: AssociationPointerJson<'a>,
    stores: [(); 0],
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    id: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) property_mappings: Option<Vec<XStorePropertyMappingJson<'a>>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) join_condition: Option<ValueSpecification>,
}

impl<'a> AssociationMappingJson<'a> {
    /// The association mapping `element`, of type `mapping_type`, without
    /// content yet.
    pub(crate) fn new(mapping_type: &'static str, element: &'a MappingElement) -> Self {
        let header = &element.header;
        Self {
            mapping_type,
            association: AssociationPointerJson {
                element_type: "ASSOCIATION",
                path: &header.target.path,
                source_information: position(&header.target.source_info),
            },
            stores: [],
            source_information: position(&element.source_info),
            id: header.id.as_ref().map(|id| id.text.as_str()),
            property_mappings: None,
            join_condition: None,
        }
    }
}

/// The association's pointer.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct AssociationPointerJson<'a> {
    #[serde(rename = "type")]
    element_type: &'static str,
    path: &'a str,
    source_information: SourceInformation,
}
