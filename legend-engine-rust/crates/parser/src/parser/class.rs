// Copyright 2026 The Legend Authors
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

use super::Parser;
use super::R;
use super::unquote_string;
use crate::error::ParseError;
use legend_pure_parser_ast::SourceInfo;
use legend_pure_parser_ast::annotation::{StereotypePtr, TaggedValue};
use legend_pure_parser_ast::element::{
    AggregationKind, ClassDef, Constraint, DefaultValue, Element, Property, QualifiedProperty,
};
use legend_pure_parser_ast::expression::{BooleanLiteral, Expression, Literal};
use legend_pure_parser_lexer::TokenKind;
use smol_str::SmolStr;

impl Parser {
    // ── Class ───────────────────────────────────────────────────────────

    pub(crate) fn parse_class(&mut self) -> R<Element> {
        let start = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::Class)?;
        let header = self.parse_element_header()?;

        let type_variable_parameters = self.parse_type_variable_parameters()?;

        let type_and_multiplicity_parameters = self.parse_type_and_multiplicity_parameters()?;

        let super_types = if self.cursor.eat(TokenKind::Extends) {
            let mut supers = Vec::new();
            loop {
                supers.push(self.parse_type_reference()?);
                if !self.cursor.eat(TokenKind::Comma) {
                    break;
                }
            }
            supers
        } else {
            vec![]
        };

        let constraints = if self.cursor.check(TokenKind::LBracket) {
            self.parse_constraints()?
        } else {
            vec![]
        };

        self.cursor.expect(TokenKind::LBrace)?;
        let (properties, qualified_properties) = self.parse_class_body()?;
        let close = self.cursor.expect(TokenKind::RBrace)?;

        Ok(Element::Class(ClassDef {
            package: header.package,
            name: header.name,
            type_variable_parameters,
            type_and_multiplicity_parameters,
            super_types,
            properties,
            qualified_properties,
            constraints,
            stereotypes: header.stereotypes,
            tagged_values: header.tagged_values,
            source_info: start.merge(&close.source_info),
        }))
    }

    pub(crate) fn parse_constraints(&mut self) -> R<Vec<Constraint>> {
        self.cursor.expect(TokenKind::LBracket)?;
        let mut result = Vec::new();
        loop {
            result.push(self.parse_constraint()?);
            if !self.cursor.eat(TokenKind::Comma) {
                break;
            }
        }
        self.cursor.expect(TokenKind::RBracket)?;
        Ok(result)
    }

    pub(crate) fn parse_constraint(&mut self) -> R<Constraint> {
        let start = self.cursor.current_source_info();
        let mut name = None;
        let mut owner = None;
        let mut function_definition;
        let mut enforcement_level = None;
        let mut external_id = None;
        let mut message = None;

        if self.is_named_constraint() {
            let (n, _) = self.cursor.expect_identifier()?;
            if self.cursor.check(TokenKind::LParen)
                && self.cursor.peek_kind_at(1) == TokenKind::Tilde
            {
                // Extended constraint: name ( ~function: ... ~enforcementLevel: ... )
                self.cursor.advance(); // (
                function_definition = Expression::Literal(Literal::Boolean(BooleanLiteral {
                    value: true,
                    source_info: start.clone(),
                }));
                while self.cursor.check(TokenKind::Tilde) {
                    self.cursor.advance(); // ~
                    let (key, _) = self.cursor.expect_identifier_or_keyword()?;
                    self.cursor.expect(TokenKind::Colon)?;
                    match key.as_str() {
                        "owner" => {
                            let (identifier, _) = self.cursor.expect_identifier_or_keyword()?;
                            owner = Some(identifier);
                        }
                        "function" => function_definition = self.parse_expression()?,
                        "enforcementLevel" => {
                            let (level, _) = self.cursor.expect_identifier_or_keyword()?;
                            enforcement_level = Some(level);
                        }
                        "externalId" => {
                            let tok = self.cursor.expect(TokenKind::StringLiteral)?;
                            external_id = Some(unquote_string(&tok.text));
                        }
                        "message" => {
                            message = Some(self.parse_expression()?);
                        }
                        other => {
                            return Err(ParseError::unexpected(
                                format!("Unknown constraint field '~{other}'"),
                                self.cursor.current_source_info(),
                            ));
                        }
                    }
                }
                self.cursor.expect(TokenKind::RParen)?;
                name = Some(n);
            } else {
                self.cursor.expect(TokenKind::Colon)?;
                function_definition = self.parse_expression()?;
                name = Some(n);
            }
        } else {
            function_definition = self.parse_expression()?;
        }
        Ok(Constraint {
            name,
            owner,
            function_definition,
            enforcement_level,
            external_id,
            message,
            // The whole constraint, as the engine positions it.
            source_info: self.cursor.span_since(&start),
        })
    }

    pub(crate) fn is_named_constraint(&self) -> bool {
        let k0 = self.cursor.peek_kind();
        let k1 = self.cursor.peek_kind_at(1);
        (k0 == TokenKind::Identifier || k0 == TokenKind::StringLiteral)
            && (k1 == TokenKind::Colon
                || (k1 == TokenKind::LParen && self.cursor.peek_kind_at(2) == TokenKind::Tilde))
    }

    pub(crate) fn parse_class_body(&mut self) -> R<(Vec<Property>, Vec<QualifiedProperty>)> {
        let mut props = Vec::new();
        let mut qprops = Vec::new();
        while !self.cursor.check(TokenKind::RBrace) {
            // A member's span runs from its first token (documentation
            // included) through its `;`, as the engine's property rules do.
            let member_start = self.cursor.current_source_info();
            let documentation = self
                .at_member_documentation()
                .then(|| self.cursor.advance().clone());
            let stereos = self.parse_stereotypes()?;
            let tvs = Self::with_documentation(documentation, self.parse_tagged_values()?)?;
            let aggregation = self.parse_aggregation()?;
            let (prop_name, _) = self.cursor.expect_identifier_or_keyword()?;

            if self.cursor.check(TokenKind::LParen) {
                // Qualified property: name(params) { body }: RetType[mult];
                qprops.push(self.parse_qualified_property(
                    prop_name,
                    member_start,
                    stereos,
                    tvs,
                )?);
            } else {
                // Regular property: name: Type[mult];
                self.cursor.expect(TokenKind::Colon)?;
                let type_ref = self.parse_type_spec()?;
                self.cursor.expect(TokenKind::LBracket)?;
                let multiplicity = self.parse_multiplicity()?;
                self.cursor.expect(TokenKind::RBracket)?;
                let default_value = if self.cursor.eat(TokenKind::Equals) {
                    let start = self.cursor.current_source_info();
                    let value = self.parse_expression()?;
                    Some(DefaultValue {
                        value,
                        source_info: self.cursor.span_since(&start),
                    })
                } else {
                    None
                };
                self.cursor.expect(TokenKind::Semicolon)?;
                props.push(Property {
                    name: prop_name,
                    type_ref,
                    multiplicity,
                    aggregation,
                    default_value,
                    stereotypes: stereos,
                    tagged_values: tvs,
                    source_info: self.cursor.span_since(&member_start),
                });
            }
        }
        Ok((props, qprops))
    }

    pub(crate) fn parse_aggregation(&mut self) -> R<Option<AggregationKind>> {
        if !self.cursor.check(TokenKind::LParen) {
            return Ok(None);
        }
        // Lookahead: (shared), (composite), (none) — vs regular parens
        match self.cursor.peek_kind_at(1) {
            TokenKind::Shared => {
                self.cursor.advance();
                self.cursor.advance();
                self.cursor.expect(TokenKind::RParen)?;
                Ok(Some(AggregationKind::Shared))
            }
            TokenKind::Composite => {
                self.cursor.advance();
                self.cursor.advance();
                self.cursor.expect(TokenKind::RParen)?;
                Ok(Some(AggregationKind::Composite))
            }
            TokenKind::None => {
                self.cursor.advance();
                self.cursor.advance();
                self.cursor.expect(TokenKind::RParen)?;
                Ok(Some(AggregationKind::None))
            }
            TokenKind::Identifier if self.cursor.peek_kind_at(2) == TokenKind::RParen => {
                let si = self.cursor.current_source_info();
                self.cursor.advance(); // (
                let tok = self.cursor.advance().clone(); // bad keyword
                self.cursor.advance(); // )
                Err(ParseError::unexpected(
                    format!(
                        "Invalid aggregation kind '{}'. Expected 'shared', 'composite', or 'none'",
                        tok.text
                    ),
                    si,
                ))
            }
            _ => Ok(None),
        }
    }

    pub(crate) fn parse_qualified_property(
        &mut self,
        name: SmolStr,
        start: SourceInfo,
        stereotypes: Vec<StereotypePtr>,
        tagged_values: Vec<TaggedValue>,
    ) -> R<QualifiedProperty> {
        self.cursor.expect(TokenKind::LParen)?;
        let parameters = self.parse_separated(TokenKind::RParen, Self::parse_parameter)?;
        self.cursor.expect(TokenKind::RParen)?;
        self.cursor.expect(TokenKind::LBrace)?;
        let body = self.parse_expression_list()?;
        self.cursor.expect(TokenKind::RBrace)?;
        self.cursor.expect(TokenKind::Colon)?;
        let return_type = self.parse_type_spec()?;
        self.cursor.expect(TokenKind::LBracket)?;
        let return_multiplicity = self.parse_multiplicity()?;
        self.cursor.expect(TokenKind::RBracket)?;
        self.cursor.expect(TokenKind::Semicolon)?;
        Ok(QualifiedProperty {
            name,
            parameters,
            return_type,
            return_multiplicity,
            body,
            stereotypes,
            tagged_values,
            source_info: self.cursor.span_since(&start),
        })
    }
}
