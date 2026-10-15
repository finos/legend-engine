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

//! Primitive values, the engine's `primitiveValue`: what a test passes a
//! service's parameters and an `EqualTo` assertion expects.
//!
//! ```text
//! primitiveValue:  atomic | [ (atomic (, atomic)*)? ]
//! atomic:          literal | - number | toBytes('text') | my::Enum.VALUE
//! ```

use legend_pure_parser_ast::expression::{
    ByteArrayLiteral, CollectionExpr, Expression, Literal, MemberAccess, PackageableElementRef,
    SimpleMemberAccess, UnaryMinusExpr,
};
use legend_pure_parser_lexer::TokenKind;
use smol_str::SmolStr;

use super::{Parser, ParserContext, R};
use crate::error::ParseError;

impl Parser {
    /// A primitive value: a literal, a negative number, bytes
    /// (`toBytes('text')`), an enum value, or a collection of them.
    pub(crate) fn parse_primitive_value(&mut self) -> R<Expression> {
        if !self.cursor.check(TokenKind::LBracket) {
            return self.parse_atomic_value();
        }
        let start = self.cursor.advance().source_info.clone();
        let mut elements = Vec::new();
        if !self.cursor.check(TokenKind::RBracket) {
            elements.push(self.parse_atomic_value()?);
            while self.cursor.eat(TokenKind::Comma) {
                elements.push(self.parse_atomic_value()?);
            }
        }
        self.cursor.expect(TokenKind::RBracket)?;
        Ok(Expression::Collection(CollectionExpr {
            elements,
            multiplicity: None,
            source_info: self.cursor.span_since(&start),
        }))
    }

    /// A literal, a negative number, bytes or an enum value.
    fn parse_atomic_value(&mut self) -> R<Expression> {
        let start = self.cursor.current_source_info();
        if self.cursor.check(TokenKind::Minus)
            && matches!(
                self.cursor.peek_kind_at(1),
                TokenKind::IntegerLiteral | TokenKind::FloatLiteral | TokenKind::DecimalLiteral
            )
        {
            self.cursor.advance();
            let operand_start = self.cursor.current_source_info();
            let operand = self.parse_literal()?.map(Expression::Literal);
            return Ok(Expression::UnaryMinus(UnaryMinusExpr {
                operand: Box::new(operand.ok_or_else(|| self.unexpected_value())?),
                operand_span: self.cursor.span_since(&operand_start),
                source_info: start,
            }));
        }
        if let Some(literal) = self.parse_literal()? {
            return Ok(Expression::Literal(literal));
        }
        let token = self.cursor.peek();
        if token.kind.is_identifier_like()
            && token.text == "toBytes"
            && self.cursor.peek_kind_at(1) == TokenKind::LParen
        {
            self.cursor.advance();
            self.cursor.advance();
            let text = self.cursor.expect(TokenKind::StringLiteral)?;
            self.cursor.expect(TokenKind::RParen)?;
            let (value, _) = super::strings::string_value(&text.text)
                .map_err(|message| ParseError::unexpected(message, text.source_info.clone()))?;
            return Ok(Expression::Literal(Literal::ByteArray(ByteArrayLiteral {
                value: SmolStr::new(value),
                source_info: self.cursor.span_since(&start),
            })));
        }
        if !token.kind.is_identifier_like() {
            return Err(self.unexpected_value());
        }
        let enumeration = self.parse_element_pointer()?;
        let dot = self.cursor.expect(TokenKind::Dot)?;
        let (member, member_source_info) = self.cursor.expect_identifier_or_keyword()?;
        Ok(Expression::MemberAccess(MemberAccess::Simple(
            SimpleMemberAccess {
                target: Box::new(Expression::PackageableElementRef(PackageableElementRef {
                    source_info: enumeration.source_info.clone(),
                    element: enumeration,
                })),
                member,
                member_source_info,
                source_info: dot.source_info,
            },
        )))
    }

    fn unexpected_value(&mut self) -> ParseError {
        let token = self.cursor.peek();
        ParseError::unexpected(
            format!("Unexpected token '{}'", token.text),
            token.source_info.clone(),
        )
    }
}

impl ParserContext<'_> {
    /// Parses a primitive value, as the engine's `primitiveValue` reads one:
    /// a literal, a negative number, bytes (`toBytes('text')`), an enum
    /// value (`my::Enum.VALUE`), or a collection of them (`[1, 2]`).
    ///
    /// # Errors
    ///
    /// Anything else next.
    pub fn parse_primitive_value(&mut self) -> R<Expression> {
        self.parser.parse_primitive_value()
    }
}

#[cfg(test)]
mod tests {
    use super::super::test_support::{position, with_context};
    use super::*;

    // `ParserContext::parse_primitive_value` itself is not general enough
    // over the context's lifetime, so the closure stays.
    #[allow(clippy::redundant_closure_for_method_calls)]
    fn value(source: &str) -> R<Expression> {
        with_context(source, |ctx| ctx.parse_primitive_value())
    }

    #[test]
    fn bytes_are_a_text_s() {
        let Expression::Literal(Literal::ByteArray(bytes)) =
            value("toBytes('a\\nb')").expect("parses")
        else {
            panic!("not bytes");
        };
        assert_eq!(bytes.value, "a\nb");
        assert_eq!(position(&bytes.source_info), (1, 1, 1, 15));
    }

    #[test]
    fn a_collection_holds_values_of_any_kind() {
        let Expression::Collection(collection) =
            value("[1, -2.5, 'a', my::E.A, toBytes('b')]").expect("parses")
        else {
            panic!("not a collection");
        };
        assert_eq!(collection.elements.len(), 5);
        assert!(matches!(collection.elements[1], Expression::UnaryMinus(_)));
        assert!(matches!(
            collection.elements[3],
            Expression::MemberAccess(MemberAccess::Simple(_))
        ));
        assert_eq!(position(&collection.source_info), (1, 1, 1, 37));
    }

    #[test]
    fn an_expression_is_not_a_primitive_value() {
        let error = value("$x").expect_err("fails");
        assert_eq!(error.message(), "Unexpected token '$'");
        // An operator after a value is the caller's to reject.
        with_context("1 + 2", |ctx| {
            ctx.parse_primitive_value().expect("parses");
            assert!(ctx.cursor().check(TokenKind::Plus));
        });
    }
}
