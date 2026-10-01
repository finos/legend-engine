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

//! The engine core's model join association mappings:
//! `my::Person_Firm: ModelJoin { {p: Person[1], f: Firm[1] | $p.firmId == $f.id} }`
//! (`ModelJoinAssociationMappingParserGrammar`,
//! `ModelJoinAssociationMappingParseTreeWalker`), the engine's
//! `ModelJoinAssociationMapping`: a join condition, which must be a
//! lambda.

use std::any::Any;

use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::convert::convert_expression_typed;
use serde_json::Value;

use crate::ast::{MappingElement, MappingElementBody, MappingElementHeader};
use crate::grammar::{ElementSpan, MappingElementGrammar, MappingElementKind};
use crate::xstore::association_json;

/// The element type this grammar reads.
pub const TYPE_NAME: &str = "ModelJoin";

/// A model join association mapping's body: its join condition.
#[derive(Debug, Clone, PartialEq)]
pub struct ModelJoinBody {
    /// The join condition, a lambda.
    pub join_condition: Expression,
}

impl MappingElementBody for ModelJoinBody {
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

/// The model join association mapping grammar.
pub struct ModelJoinGrammar;

impl MappingElementGrammar for ModelJoinGrammar {
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
        let start = ctx.cursor().current_source_info();
        let join_condition = ctx.parse_expression()?;
        // The walker's check.
        if !matches!(join_condition, Expression::Lambda(_)) {
            let source_info = ctx.cursor().span_since(&start);
            ctx.defer(ParseError::Unexpected {
                message: "ModelJoin association mapping requires a lambda join condition of the \
                          form '{src: SrcClass[1], tgt: TgtClass[1] | <boolean expression>}'"
                    .to_string(),
                source_info,
            });
        }
        Ok(Box::new(ModelJoinBody { join_condition }))
    }

    fn convert(
        &self,
        element: &MappingElement,
    ) -> Result<(MappingElementKind, Value), ConvertError> {
        let body = element
            .body
            .as_any()
            .downcast_ref::<ModelJoinBody>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "ModelJoinGrammar: the body is not a model join association mapping's",
                    Some(&element.source_info),
                )
            })?;
        let join_condition = serde_json::to_value(convert_expression_typed(&body.join_condition)?)?;
        Ok((
            MappingElementKind::Association,
            association_json("modelJoin", element, "joinCondition", join_condition),
        ))
    }
}
