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

//! The engine core's operation class mappings, a class mapped by an
//! operation over other class mappings:
//! `*my::Person: Operation { meta::pure::router::operations::union_…(p1, p2) }`
//! (`OperationClassMappingParserGrammar`,
//! `OperationClassMappingParseTreeWalker`), the engine's
//! `OperationClassMapping`, or its `MergeOperationClassMapping` for
//! `merge_…([p1, p2], {a: A[1], b: B[1] | …})`.
//!
//! ```text
//! operationClassMapping:  function (( id, … ) | ( [ id, … ], expression )) ;?
//! ```
//!
//! The operation is named by the function's path, as written; a path of no
//! operation names none. The walker parses a merge's validation lambda
//! again from its text without spaces, positioned as if it started where
//! the function does, and makes it the body of a lambda: those positions
//! are reproduced.

use std::any::Any;

use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{identifier, qualified_name, unexpected};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::convert::convert_expression_typed;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::value_spec::{LambdaFunction, ValueSpecification};
use serde_json::{Value, json};
use smol_str::SmolStr;

use crate::ast::{MappingElement, MappingElementBody, MappingElementHeader};
use crate::grammar::{ElementSpan, MappingElementGrammar, MappingElementKind};
use crate::section::{expression_block, separated};

/// The element type this grammar reads.
pub const TYPE_NAME: &str = "Operation";

/// The operations the walker knows, by their function's path
/// (`OperationClassMappingParseTreeWalker.funcToOps`).
const OPERATIONS: [(&str, &str); 4] = [
    (
        "meta::pure::router::operations::special_union_OperationSetImplementation_1__SetImplementation_MANY_",
        "ROUTER_UNION",
    ),
    (
        "meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_",
        "STORE_UNION",
    ),
    (
        "meta::pure::router::operations::inheritance_OperationSetImplementation_1__SetImplementation_MANY_",
        "INHERITANCE",
    ),
    (
        "meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_",
        "MERGE",
    ),
];

/// An operation class mapping's body.
#[derive(Debug, Clone, PartialEq)]
pub struct OperationBody {
    /// The function's path, as written.
    pub function: String,
    /// The class mappings the operation takes, unquoted.
    pub parameters: Vec<SmolStr>,
    /// A merge's validation lambda, as the walker parses it again.
    pub validation: Option<Expression>,
}

impl MappingElementBody for OperationBody {
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

/// The operation class mapping grammar.
pub struct OperationGrammar;

impl MappingElementGrammar for OperationGrammar {
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
        let function_start = ctx.cursor().current_source_info();
        let function = qualified_name(ctx)?.text;
        ctx.cursor().expect(TokenKind::LParen)?;
        let (parameters, validation) = if ctx.cursor().eat(TokenKind::LBracket) {
            let parameters = ids(ctx, TokenKind::RBracket)?;
            ctx.cursor().expect(TokenKind::RBracket)?;
            ctx.cursor().expect(TokenKind::Comma)?;
            let lambda = ctx.read_until(TokenKind::RParen)?;
            if lambda.text().is_empty() {
                return Err(unexpected(ctx.cursor().peek()));
            }
            // The grammar reads it as written.
            expression_block(ctx, &lambda)?;
            let validation = ctx.parse_expression_text(&lambda.text(), &function_start)?;
            (parameters, Some(validation))
        } else {
            (ids(ctx, TokenKind::RParen)?, None)
        };
        ctx.cursor().expect(TokenKind::RParen)?;
        ctx.cursor().eat(TokenKind::Semicolon);
        Ok(Box::new(OperationBody {
            function,
            parameters,
            validation,
        }))
    }

    fn convert(
        &self,
        element: &MappingElement,
    ) -> Result<(MappingElementKind, Value), ConvertError> {
        let body = element
            .body
            .as_any()
            .downcast_ref::<OperationBody>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "OperationGrammar: the body is not an operation class mapping's",
                    Some(&element.source_info),
                )
            })?;
        let header = &element.header;
        let mut json = json!({
            "_type": if body.validation.is_some() { "mergeOperation" } else { "operation" },
            "class": header.target.path,
            "root": header.root,
            "parameters": body.parameters.iter().map(SmolStr::as_str).collect::<Vec<_>>(),
            "sourceInformation": position(&element.source_info),
            "classSourceInformation": position(&header.target.source_info),
        });
        if let Some((_, operation)) = OPERATIONS.iter().find(|(path, _)| *path == body.function) {
            json["operation"] = json!(operation);
        }
        if let Some(id) = &header.id {
            json["id"] = json!(id.text.as_str());
        }
        if let Some(validation) = &body.validation {
            json["validationFunction"] =
                serde_json::to_value(ValueSpecification::Lambda(LambdaFunction {
                    body: vec![convert_expression_typed(validation)?],
                    parameters: Vec::new(),
                    source_information: None,
                }))?;
        }
        Ok((MappingElementKind::Class, json))
    }
}

/// Class mapping ids, unquoted, separated by commas, up to `close`.
fn ids(ctx: &mut ParserContext<'_>, close: TokenKind) -> Result<Vec<SmolStr>, ParseError> {
    separated(ctx, close, |ctx| Ok(identifier(ctx)?.value))
}

fn position(source_info: &SourceInfo) -> Value {
    json!(SourceInformation::from(source_info))
}
