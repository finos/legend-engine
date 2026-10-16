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
use super::{ParserContext, split_package_name};
use crate::error::ParseError;
use legend_pure_parser_ast::SourceInfo;
use legend_pure_parser_ast::annotation::{PackageableElementPtr, Parameter};
use legend_pure_parser_ast::expression::{
    AllInstancesExpr, AllInstancesKind, ArithmeticExpr, ArithmeticOp, ArrowFunction,
    BooleanLiteral, CodeBlock, CollectionExpr, ComparisonExpr, ComparisonOp, CopyExpr,
    DateTimeLiteral, DecimalLiteral, Expression, FloatLiteral, FunctionApplication, IntegerLiteral,
    KeyValuePair, Lambda, LatestDateLiteral, LetExpr, Literal, LogicalExpr, LogicalOp,
    MemberAccess, MultiplicityReferenceExpr, NavigationPath, NewInstanceExpr, NotExpr,
    PackageableElementRef, PropertyPathElement, QualifiedMemberAccess, SimpleMemberAccess,
    SliceExpr, StrictDateLiteral, StrictTimeLiteral, StringLiteral, TypeReferenceExpr,
    UnaryMinusExpr, UnitInstanceExpr, UnitTypeRef, Variable,
};
use legend_pure_parser_ast::island::IslandExpression;
use legend_pure_parser_ast::source_info::Spanned;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::TokenKind;
use smol_str::SmolStr;

impl Parser {
    // ── Expressions (precedence-climbing recursive descent) ─────────────

    /// The grammar's `codeBlock`: `line (';' (line ';')*)?`. A single line
    /// may stand alone; with more than one, each line ends with `;`.
    pub(crate) fn parse_expression_list(&mut self) -> R<Vec<Expression>> {
        let mut exprs = Vec::new();
        while !self.cursor.check(TokenKind::RBrace) && !self.cursor.check(TokenKind::Eof) {
            exprs.push(self.parse_expression()?);
            if exprs.len() == 1
                && (self.cursor.check(TokenKind::RBrace) || self.cursor.check(TokenKind::Eof))
            {
                break;
            }
            self.cursor.expect(TokenKind::Semicolon)?;
        }
        Ok(exprs)
    }

    /// A code block standing alone, to the end of the input: a lambda's
    /// text for the engine's `parseLambda`.
    pub(crate) fn parse_standalone_code_block(&mut self) -> R<CodeBlock> {
        let start = self.cursor.current_source_info();
        let expressions = self.parse_expression_list()?;
        if expressions.is_empty() {
            return Err(ParseError::expected(
                "an expression",
                self.cursor.peek_kind(),
                self.cursor.current_source_info(),
            ));
        }
        self.expect_end_of_input()?;
        Ok(CodeBlock {
            expressions,
            source_info: self.cursor.span_since(&start),
        })
    }

    /// An expression standing alone, to the end of the input: the engine's
    /// `parseValueSpecification`.
    pub(crate) fn parse_standalone_expression(&mut self) -> R<Expression> {
        let expression = self.parse_expression()?;
        self.expect_end_of_input()?;
        Ok(expression)
    }

    fn expect_end_of_input(&self) -> R<()> {
        if self.cursor.check(TokenKind::Eof) {
            Ok(())
        } else {
            Err(ParseError::expected(
                "the end of the input",
                self.cursor.peek_kind(),
                self.cursor.current_source_info(),
            ))
        }
    }

    /// Parse an expression (entry point — handles precedence via recursive descent).
    pub fn parse_expression(&mut self) -> R<Expression> {
        self.parse_or_expression()
    }

    pub(crate) fn parse_or_expression(&mut self) -> R<Expression> {
        let start = self.cursor.current_source_info();
        let mut left = self.parse_and_expression()?;
        while self.cursor.check(TokenKind::PipePipe) {
            let left_span = self.cursor.span_since(&start);
            let si = self.cursor.current_source_info();
            self.cursor.advance();
            let right_start = self.cursor.current_source_info();
            let right = self.parse_and_expression()?;
            left = Expression::Logical(LogicalExpr {
                left: Box::new(left),
                op: LogicalOp::Or,
                right: Box::new(right),
                left_span,
                right_span: self.cursor.span_since(&right_start),
                source_info: si,
            });
        }
        Ok(left)
    }

    pub(crate) fn parse_and_expression(&mut self) -> R<Expression> {
        let start = self.cursor.current_source_info();
        let mut left = self.parse_comparison()?;
        while self.cursor.check(TokenKind::AmpAmp) {
            let left_span = self.cursor.span_since(&start);
            let si = self.cursor.current_source_info();
            self.cursor.advance();
            let right_start = self.cursor.current_source_info();
            let right = self.parse_comparison()?;
            left = Expression::Logical(LogicalExpr {
                left: Box::new(left),
                op: LogicalOp::And,
                right: Box::new(right),
                left_span,
                right_span: self.cursor.span_since(&right_start),
                source_info: si,
            });
        }
        Ok(left)
    }

    pub(crate) fn parse_comparison(&mut self) -> R<Expression> {
        let start = self.cursor.current_source_info();
        let mut left = self.parse_additive()?;
        if let Some(op) = self.comparison_op() {
            let left_span = self.cursor.span_since(&start);
            let si = self.cursor.current_source_info();
            self.cursor.advance();
            let right_start = self.cursor.current_source_info();
            let right = self.parse_additive()?;
            left = Expression::Comparison(ComparisonExpr {
                left: Box::new(left),
                op,
                right: Box::new(right),
                left_span,
                right_span: self.cursor.span_since(&right_start),
                source_info: si,
            });
        }
        Ok(left)
    }

    pub(crate) fn comparison_op(&self) -> Option<ComparisonOp> {
        match self.cursor.peek_kind() {
            TokenKind::EqualEqual => Some(ComparisonOp::Equal),
            TokenKind::BangEqual => Some(ComparisonOp::NotEqual),
            TokenKind::Less => Some(ComparisonOp::LessThan),
            TokenKind::LessEqual => Some(ComparisonOp::LessThanOrEqual),
            TokenKind::Greater => Some(ComparisonOp::GreaterThan),
            TokenKind::GreaterEqual => Some(ComparisonOp::GreaterThanOrEqual),
            _ => None,
        }
    }

    pub(crate) fn parse_additive(&mut self) -> R<Expression> {
        let start = self.cursor.current_source_info();
        let mut left = self.parse_multiplicative()?;
        while matches!(self.cursor.peek_kind(), TokenKind::Plus | TokenKind::Minus) {
            let left_span = self.cursor.span_since(&start);
            let si = self.cursor.current_source_info();
            let op = if self.cursor.peek_kind() == TokenKind::Plus {
                ArithmeticOp::Plus
            } else {
                ArithmeticOp::Minus
            };
            self.cursor.advance();
            let right_start = self.cursor.current_source_info();
            let right = self.parse_multiplicative()?;
            left = Expression::Arithmetic(ArithmeticExpr {
                left: Box::new(left),
                op,
                right: Box::new(right),
                left_span,
                right_span: self.cursor.span_since(&right_start),
                source_info: si,
            });
        }
        Ok(left)
    }

    pub(crate) fn parse_multiplicative(&mut self) -> R<Expression> {
        let start = self.cursor.current_source_info();
        let mut left = self.parse_unary()?;
        while matches!(self.cursor.peek_kind(), TokenKind::Star | TokenKind::Slash) {
            let left_span = self.cursor.span_since(&start);
            let si = self.cursor.current_source_info();
            let op = if self.cursor.peek_kind() == TokenKind::Star {
                ArithmeticOp::Times
            } else {
                ArithmeticOp::Divide
            };
            self.cursor.advance();
            let right_start = self.cursor.current_source_info();
            let right = self.parse_unary()?;
            left = Expression::Arithmetic(ArithmeticExpr {
                left: Box::new(left),
                op,
                right: Box::new(right),
                left_span,
                right_span: self.cursor.span_since(&right_start),
                source_info: si,
            });
        }
        Ok(left)
    }

    pub(crate) fn parse_unary(&mut self) -> R<Expression> {
        if self.cursor.check(TokenKind::Bang) {
            let si = self.cursor.current_source_info();
            self.cursor.advance();
            let operand_start = self.cursor.current_source_info();
            let expr = self.parse_unary()?;
            return Ok(Expression::Not(NotExpr {
                operand: Box::new(expr),
                operand_span: self.cursor.span_since(&operand_start),
                source_info: si,
            }));
        }
        if self.cursor.check(TokenKind::Minus) && !self.cursor.check(TokenKind::Arrow) {
            let si = self.cursor.current_source_info();
            self.cursor.advance();
            let operand_start = self.cursor.current_source_info();
            let expr = self.parse_unary()?;
            return Ok(Expression::UnaryMinus(UnaryMinusExpr {
                operand: Box::new(expr),
                operand_span: self.cursor.span_since(&operand_start),
                source_info: si,
            }));
        }
        // Unary plus: `+expr` is semantically a no-op (identity)
        if self.cursor.check(TokenKind::Plus) {
            self.cursor.advance();
            return self.parse_unary();
        }
        self.parse_postfix()
    }

    pub(crate) fn parse_postfix(&mut self) -> R<Expression> {
        let mut expr = self.parse_primary()?;
        loop {
            if self.cursor.check(TokenKind::Arrow) {
                // Capture the target's span BEFORE we wrap it — used as
                // the start of the combined `target->fn(args)` span so
                // IDE goto / hover can locate the call from clicks
                // anywhere inside the chain. The `->` and function-name
                // spans alone (which the original code used) are far
                // too narrow.
                let target_si = expr.source_info().clone();
                let arrow_si = self.cursor.current_source_info();
                self.cursor.advance();
                let (func_name, func_si) = self.cursor.expect_identifier_or_keyword()?;
                // Build fully qualified path for the function
                let mut path = Package::root(func_name, func_si.clone());
                while self.cursor.eat(TokenKind::PathSep) {
                    let (seg, seg_si) = self.cursor.expect_identifier_or_keyword()?;
                    path = path.child(seg, seg_si);
                }
                let (pkg, name) = split_package_name(&path);
                let func = PackageableElementPtr {
                    package: pkg,
                    name,
                    // The whole (qualified) function name, where the
                    // engine positions the call.
                    source_info: self.cursor.span_since(&func_si),
                };
                self.cursor.expect(TokenKind::LParen)?;
                let args = self.parse_separated(TokenKind::RParen, Self::parse_expression)?;
                // Capture the closing `)`'s span before consuming so the
                // combined source_info covers the whole call.
                let close_si = self.cursor.current_source_info();
                self.cursor.expect(TokenKind::RParen)?;
                let combined_si = SourceInfo::new(
                    target_si.source.clone(),
                    target_si.start_line,
                    target_si.start_column,
                    close_si.end_line,
                    close_si.end_column,
                );
                let _ = arrow_si; // kept for trace-debug if ever needed
                expr = Expression::ArrowFunction(ArrowFunction {
                    target: Box::new(expr),
                    function: func,
                    arguments: args,
                    source_info: combined_si,
                });
            } else if self.cursor.check(TokenKind::Dot) {
                let si = self.cursor.current_source_info();
                self.cursor.advance();
                let (member, member_si) = self.cursor.expect_identifier_or_keyword()?;
                if self.cursor.check(TokenKind::LParen) {
                    // Qualified member access: expr.member(args)
                    self.cursor.advance();
                    let args = self.parse_separated(TokenKind::RParen, Self::parse_expression)?;
                    self.cursor.expect(TokenKind::RParen)?;
                    // `Class.all()` and its variants: only right after an
                    // element name (the grammar's `instanceReference`).
                    if let (Some(kind), Expression::PackageableElementRef(class)) =
                        (AllInstancesKind::from_member(&member), &expr)
                    {
                        if class.element.name.is_empty() {
                            return Err(empty_function_caller(
                                self.cursor.span_since(&class.source_info),
                            ));
                        }
                        expr = Expression::AllInstances(AllInstancesExpr {
                            class: class.element.clone(),
                            kind,
                            arguments: args,
                            call_source_info: self.cursor.span_since(&si),
                            source_info: self.cursor.span_since(&class.source_info),
                        });
                        continue;
                    }
                    expr =
                        Expression::MemberAccess(MemberAccess::Qualified(QualifiedMemberAccess {
                            target: Box::new(expr),
                            member,
                            member_source_info: member_si,
                            arguments: args,
                            source_info: si,
                        }));
                } else {
                    expr = Expression::MemberAccess(MemberAccess::Simple(SimpleMemberAccess {
                        target: Box::new(expr),
                        member,
                        member_source_info: member_si,
                        source_info: si,
                    }));
                }
            } else {
                break;
            }
        }
        Ok(expr)
    }

    /// A literal token: a number, string, boolean or date, as the engine's
    /// `instanceLiteralToken` reads it; `None`, consuming nothing, when the
    /// next token isn't one.
    pub(super) fn parse_literal(&mut self) -> R<Option<Literal>> {
        let si = self.cursor.current_source_info();
        let literal = match self.cursor.peek_kind() {
            TokenKind::IntegerLiteral => {
                let tok = self.cursor.advance().clone();
                Literal::Integer(IntegerLiteral {
                    value: tok.text.parse().unwrap_or(0),
                    source_info: si,
                })
            }
            TokenKind::FloatLiteral => {
                let tok = self.cursor.advance().clone();
                Literal::Float(FloatLiteral {
                    value: tok.text.parse().unwrap_or(0.0),
                    source_info: si,
                })
            }
            TokenKind::DecimalLiteral => {
                let tok = self.cursor.advance().clone();
                Literal::Decimal(DecimalLiteral {
                    value: tok.text.trim_end_matches(['D', 'd']).to_string(),
                    source_info: si,
                })
            }
            TokenKind::StringLiteral => {
                let tok = self.cursor.advance().clone();
                let (value, multi_line) = super::strings::string_value(&tok.text)
                    .map_err(|message| ParseError::unexpected(message, si.clone()))?;
                Literal::String(StringLiteral {
                    value: value.into(),
                    multi_line,
                    source_info: si,
                })
            }
            TokenKind::True | TokenKind::False => {
                let value = self.cursor.advance().kind == TokenKind::True;
                Literal::Boolean(BooleanLiteral {
                    value,
                    source_info: si,
                })
            }
            TokenKind::DateLiteral => {
                let tok = self.cursor.advance().clone();
                let raw = tok.text.trim_start_matches('%');
                if raw == "latest" {
                    return Ok(Some(Literal::LatestDate(LatestDateLiteral {
                        source_info: si,
                    })));
                }
                // The grammar's STRICTTIME token (`%HH:MM[:SS[.f]]`) or DATE
                // token; a date is strict with year, month and day and no
                // time (the engine's `DateParseTreeWalker`).
                let unsigned = raw.strip_prefix('-').unwrap_or(raw);
                let head_len = unsigned
                    .find(|c: char| !c.is_ascii_digit())
                    .unwrap_or(unsigned.len());
                let strict_time = unsigned[head_len..].starts_with(':');
                let value = raw.to_string();
                if strict_time {
                    Literal::StrictTime(StrictTimeLiteral {
                        value,
                        source_info: si,
                    })
                } else if !raw.contains('T') && unsigned.split('-').count() == 3 {
                    Literal::StrictDate(StrictDateLiteral {
                        value,
                        source_info: si,
                    })
                } else {
                    Literal::DateTime(DateTimeLiteral {
                        value,
                        source_info: si,
                    })
                }
            }
            _ => return Ok(None),
        };
        Ok(Some(literal))
    }

    #[allow(clippy::too_many_lines, clippy::similar_names)]
    pub(crate) fn parse_primary(&mut self) -> R<Expression> {
        let si = self.cursor.current_source_info();
        if let Some(literal) = self.parse_literal()? {
            return match literal {
                // A number may be a unit's value: `5 Mass~Kilogram`.
                Literal::Integer(_) | Literal::Float(_) | Literal::Decimal(_) => {
                    self.maybe_unit_instance(Expression::Literal(literal), si)
                }
                other => Ok(Expression::Literal(other)),
            };
        }
        match self.cursor.peek_kind() {
            // Leading-dot float literal: .01 → 0.01
            TokenKind::Dot if self.cursor.peek_kind_at(1) == TokenKind::IntegerLiteral => {
                self.cursor.advance(); // consume the dot
                let tok = self.cursor.advance().clone();
                let text = format!("0.{}", tok.text);
                let value: f64 = text.parse().unwrap_or(0.0);
                Ok(Expression::Literal(Literal::Float(FloatLiteral {
                    value,
                    source_info: si,
                })))
            }
            // Variable: $name
            TokenKind::Dollar => {
                self.cursor.advance();
                let (name, _) = self.cursor.expect_identifier_or_keyword()?;
                Ok(Expression::Variable(Variable {
                    name,
                    source_info: self.cursor.span_since(&si),
                }))
            }
            // let binding
            TokenKind::Let => {
                self.cursor.advance();
                let (name, name_source_info) = self.cursor.expect_identifier_or_keyword()?;
                self.cursor.expect(TokenKind::Equals)?;
                let value = self.parse_expression()?;
                Ok(Expression::Let(LetExpr {
                    name,
                    name_source_info,
                    value: Box::new(value),
                    source_info: self.cursor.span_since(&si),
                }))
            }
            // No-param bare lambda: | body
            TokenKind::Pipe => {
                self.cursor.advance();
                let mut body = vec![self.parse_expression()?];
                while self.cursor.eat(TokenKind::Semicolon) {
                    // Stop if we hit a closing delimiter — `Comma` covers
                    // the case `if(cond, | then; , | else)` where a
                    // multi-statement no-param lambda body ends with `;,`
                    // before the next `if`/`match` arm.
                    if self.cursor.check(TokenKind::RParen)
                        || self.cursor.check(TokenKind::RBrace)
                        || self.cursor.check(TokenKind::RBracket)
                        || self.cursor.check(TokenKind::Comma)
                        || self.cursor.check(TokenKind::Eof)
                    {
                        break;
                    }
                    body.push(self.parse_expression()?);
                }
                Ok(Expression::Lambda(Lambda {
                    parameters: vec![],
                    body,
                    source_info: self.cursor.span_since(&si),
                }))
            }
            // New instance: ^Type(props), ^Type<Args>(props), or copy: ^$var(overrides)
            TokenKind::Caret => {
                self.cursor.advance();

                // Copy from variable: ^$var(overrides)
                if self.cursor.check(TokenKind::Dollar) {
                    self.cursor.advance();
                    let (var_name, _) = self.cursor.expect_identifier_or_keyword()?;
                    self.cursor.expect(TokenKind::LParen)?;
                    let assignments = self.parse_separated(TokenKind::RParen, |p| {
                        let kv_si = p.cursor.current_source_info();
                        let (mut prop, _) = p.cursor.expect_identifier_or_keyword()?;
                        // Accept dotted property paths: address.name
                        while p.cursor.eat(TokenKind::Dot) {
                            let (next, _) = p.cursor.expect_identifier_or_keyword()?;
                            prop = SmolStr::new(format!("{prop}.{next}"));
                        }
                        // `+=` (append) vs `=` (replace) — record on the
                        // KeyValuePair so the runtime can pick `mutate_add`
                        // (append to existing values) or `mutate_set`
                        // (replace). Java threads this through as the
                        // `KeyValue.add` slot; mirror that semantics.
                        let augmented = p.cursor.eat(TokenKind::Plus);
                        p.cursor.expect(TokenKind::Equals)?;
                        let val = p.parse_expression()?;
                        Ok(KeyValuePair {
                            key: prop,
                            value: val,
                            augmented,
                            source_info: kv_si,
                        })
                    })?;
                    self.cursor.expect(TokenKind::RParen)?;
                    return Ok(Expression::Copy(CopyExpr {
                        source: var_name,
                        assignments,
                        source_info: si,
                    }));
                }

                // New instance: ^Type(props) or ^Type<Args>(props)
                let path = self.parse_package_path()?;
                let (pkg, name) = split_package_name(&path);

                // Optional type arguments: ^List<U>(...) or ^Class<|1>(...)
                let type_arguments = if self.cursor.eat(TokenKind::Less) {
                    let mut args = Vec::new();
                    loop {
                        if self.cursor.check(TokenKind::Pipe) {
                            // Multiplicity argument: |1, |* — skip over it
                            self.cursor.advance(); // consume |
                            if self.cursor.check(TokenKind::IntegerLiteral)
                                || self.cursor.check(TokenKind::Star)
                            {
                                self.cursor.advance(); // consume the value
                            }
                        } else {
                            args.push(self.parse_type_argument()?);
                        }
                        if !self.cursor.eat(TokenKind::Comma) {
                            break;
                        }
                    }
                    self.cursor.expect_closing_angle_bracket()?;
                    args
                } else {
                    vec![]
                };
                // type_arguments: thread through to NewInstanceExpr so the
                // compiler can preserve them in the lowered `new(…)` call —
                // `^List<String>(values=…)` instances need their element-side
                // bindings reflected at runtime via `genericType().typeArguments`.

                // Optional type variable values: (10, 'ok')
                let type_variable_values = if self.cursor.check(TokenKind::LParen) {
                    // Disambiguate from property assignments `(prop = val)`.
                    // A property assignment starts with an identifier/keyword (or integer property)
                    // followed by `=` or `+=` or `.` (for nested properties).
                    let mut lookahead = 1;
                    if self.cursor.peek_kind_at(lookahead) == TokenKind::IntegerLiteral {
                        lookahead += 1; // Integer keys
                    } else {
                        // Skip dotted property paths: address.name
                        while self.cursor.peek_kind_at(lookahead).is_identifier_like() {
                            lookahead += 1;
                            if self.cursor.peek_kind_at(lookahead) == TokenKind::Dot {
                                lookahead += 1;
                            } else {
                                break;
                            }
                        }
                    }
                    let next_kind = self.cursor.peek_kind_at(lookahead);
                    // `(prop = val)` or `(prop += val)` → property assignments
                    // `()` → empty property assignments (only if nothing preceded the `)`)
                    // `(10)` or `('str')` → type variable values
                    let is_prop_assignments = next_kind == TokenKind::Equals
                        || next_kind == TokenKind::Plus
                        || (next_kind == TokenKind::RParen && lookahead == 1); // truly empty `()`

                    if is_prop_assignments {
                        vec![]
                    } else {
                        self.cursor.expect(TokenKind::LParen)?;
                        let mut vals = Vec::new();
                        if !self.cursor.check(TokenKind::RParen) {
                            loop {
                                vals.push(self.parse_type_variable_value()?);
                                if !self.cursor.eat(TokenKind::Comma) {
                                    break;
                                }
                            }
                        }
                        self.cursor.expect(TokenKind::RParen)?;
                        vals
                    }
                } else {
                    vec![]
                };

                // Merge the start span (`^` token, captured before this
                // match arm) with the path's last-segment span so
                // `class_ref.source_info` covers the whole `^abc::Class1`
                // prefix (sans `()`). Without this, the lowered
                // `PackageableElementRef` carries a near-zero-width span
                // and Cmd-click on `Class1` inside `^abc::Class1()` falls
                // outside the index's recorded region.
                let class_span = si.merge(
                    legend_pure_parser_ast::source_info::Spanned::source_info(&path),
                );
                let class_ref = PackageableElementPtr {
                    package: pkg,
                    name,
                    source_info: class_span,
                };
                // Optional instance name: `^Type name(props)`. The Java
                // grammar accepts a single identifier between the type
                // (and any `<args>`/`(typeVars)` blocks) and the
                // property-assignments `(`. The runtime treats the name
                // as a distinguishing key.
                let instance_name = if !self.cursor.check(TokenKind::LParen)
                    && self.cursor.peek_kind().is_identifier_like()
                    && self.cursor.peek_kind_at(1) == TokenKind::LParen
                {
                    let (n, _) = self.cursor.expect_identifier_or_keyword()?;
                    Some(n)
                } else {
                    None
                };
                self.cursor.expect(TokenKind::LParen)?;
                let assignments = self.parse_separated(TokenKind::RParen, |p| {
                    let kv_si = p.cursor.current_source_info();
                    // Accept identifier, dotted path, or integer key
                    let mut prop = if p.cursor.check(TokenKind::IntegerLiteral) {
                        let tok = p.cursor.advance().clone();
                        tok.text.clone()
                    } else {
                        let (name, _) = p.cursor.expect_identifier_or_keyword()?;
                        name
                    };
                    // Accept dotted property paths: address.name
                    while p.cursor.eat(TokenKind::Dot) {
                        let (next, _) = p.cursor.expect_identifier_or_keyword()?;
                        prop = SmolStr::new(format!("{prop}.{next}"));
                    }
                    // `+=` is a no-op on `^Class(...)` (no carried values
                    // to append to), but the parser tolerates it for symmetry
                    // with the copy-expression form. Recording the flag keeps
                    // the AST honest; the runtime collapses to `mutate_set`
                    // either way on a fresh allocation.
                    let augmented = p.cursor.eat(TokenKind::Plus);
                    p.cursor.expect(TokenKind::Equals)?;
                    let val = p.parse_expression()?;
                    Ok(KeyValuePair {
                        key: prop,
                        value: val,
                        augmented,
                        source_info: kv_si,
                    })
                })?;
                self.cursor.expect(TokenKind::RParen)?;
                Ok(Expression::NewInstance(NewInstanceExpr {
                    class: class_ref,
                    type_arguments,
                    type_variable_values,
                    instance_name,
                    assignments,
                    source_info: si,
                }))
            }
            // Cast: `@Type` OR bare multiplicity literal: `@[m]`.
            // Java grammar `M3CoreParser.g4:311 AT (type | multiplicity)`
            // — peek the token after `@` to disambiguate.
            TokenKind::At => {
                self.cursor.advance();
                if self.cursor.check(TokenKind::LBracket) {
                    self.cursor.advance();
                    let multiplicity = self.parse_multiplicity_argument()?;
                    self.cursor.expect(TokenKind::RBracket)?;
                    return Ok(Expression::MultiplicityReferenceExpr(
                        MultiplicityReferenceExpr {
                            multiplicity,
                            source_info: si,
                        },
                    ));
                }
                let type_ref = self.parse_type_spec()?;
                Ok(Expression::TypeReferenceExpr(TypeReferenceExpr {
                    type_ref,
                    source_info: self.cursor.span_since(&si),
                }))
            }
            // Column builder: ~name or ~[name, name2]
            TokenKind::Tilde => {
                self.cursor.advance();
                self.parse_column_builder()
            }
            // Collection: [expr, ...] or Range: [start:stop:step]
            TokenKind::LBracket => {
                self.cursor.advance();
                // Check for [:stop] (no start)
                if self.cursor.eat(TokenKind::Colon) {
                    let stop = self.parse_expression()?;
                    let step = if self.cursor.eat(TokenKind::Colon) {
                        Some(Box::new(self.parse_expression()?))
                    } else {
                        None
                    };
                    self.cursor.expect(TokenKind::RBracket)?;
                    return Ok(Expression::Slice(SliceExpr {
                        start: None,
                        stop: Box::new(stop),
                        step,
                        source_info: si,
                    }));
                }
                if self.cursor.check(TokenKind::RBracket) {
                    self.cursor.advance();
                    return Ok(Expression::Collection(CollectionExpr {
                        elements: vec![],
                        multiplicity: None,
                        source_info: self.cursor.span_since(&si),
                    }));
                }
                let first = self.parse_expression()?;
                // Range: [start:stop] or [start:stop:step]
                if self.cursor.eat(TokenKind::Colon) {
                    let stop = self.parse_expression()?;
                    let step = if self.cursor.eat(TokenKind::Colon) {
                        Some(Box::new(self.parse_expression()?))
                    } else {
                        None
                    };
                    self.cursor.expect(TokenKind::RBracket)?;
                    Ok(Expression::Slice(SliceExpr {
                        start: Some(Box::new(first)),
                        stop: Box::new(stop),
                        step,
                        source_info: si,
                    }))
                } else {
                    // Standard collection
                    let mut elements = vec![first];
                    while self.cursor.eat(TokenKind::Comma) {
                        if self.cursor.check(TokenKind::RBracket) {
                            break;
                        }
                        elements.push(self.parse_expression()?);
                    }
                    self.cursor.expect(TokenKind::RBracket)?;
                    Ok(Expression::Collection(CollectionExpr {
                        elements,
                        multiplicity: None,
                        source_info: self.cursor.span_since(&si),
                    }))
                }
            }
            // Parenthesized expression: (expr)
            // — OR bare relation-type literal at expression position:
            //   `(a:Integer, b:String)` →
            //   `Expression::TypeReferenceExpr(TypeSpec::Relation(...))`,
            //   mirroring Java `M3CoreParser.g4` `atomicExpression: ... | type`
            //   reaching `type → GROUP_OPEN columnType (COMMA columnType)* GROUP_CLOSE`.
            //   The discriminator is 2-token lookahead: `( ident|? :` is
            //   only a relation-type literal — no Pure expression starts
            //   with `<ident>:` or `?:`. Wildcard column names (`?`)
            //   are valid per `parse_relation_columns`.
            TokenKind::LParen => {
                let next = self.cursor.peek_kind_at(1);
                let after = self.cursor.peek_kind_at(2);
                let is_relation_literal = (next.is_identifier_like()
                    || next == TokenKind::Question)
                    && after == TokenKind::Colon;
                if is_relation_literal {
                    let type_ref = self.parse_relation_type()?;
                    // Bare `(cols)` and `@(cols)` produce the same AST
                    // shape: Java's processor routes both through the
                    // same `ctx.type()` handler (line 1096) and builds
                    // an `InstanceValue<Type[1]>` either way.
                    return Ok(Expression::TypeReferenceExpr(TypeReferenceExpr {
                        type_ref,
                        source_info: self.cursor.span_since(&si),
                    }));
                }
                self.cursor.advance();
                let expr = self.parse_expression()?;
                self.cursor.expect(TokenKind::RParen)?;
                Ok(Expression::Group(Box::new(expr)))
            }
            // Lambda or block: {x | body}, {| body}, {x: String[1], y | body}, or {expr; expr}
            TokenKind::LBrace => {
                self.cursor.advance();
                if self.is_lambda_start() {
                    let mut params = Vec::new();
                    // Only parse params if the next token isn't pipe ({|body})
                    if !self.cursor.check(TokenKind::Pipe) {
                        loop {
                            params.push(self.parse_lambda_param()?);
                            if !self.cursor.eat(TokenKind::Comma) {
                                break;
                            }
                        }
                    }
                    let pipe = self.cursor.current_source_info();
                    self.cursor.expect(TokenKind::Pipe)?;
                    let body = self.parse_expression_list()?;
                    // The engine's `lambdaPipe`: the pipe and code block,
                    // without the braces.
                    let source_info = self.cursor.span_since(&pipe);
                    self.cursor.expect(TokenKind::RBrace)?;
                    Ok(Expression::Lambda(Lambda {
                        parameters: params,
                        body,
                        source_info,
                    }))
                } else {
                    let exprs = self.parse_expression_list()?;
                    self.cursor.expect(TokenKind::RBrace)?;
                    if exprs.len() == 1 {
                        if let Some(single) = exprs.into_iter().next() {
                            Ok(single)
                        } else {
                            unreachable!("len() == 1 guarantees at least one element");
                        }
                    } else {
                        Ok(Expression::Collection(CollectionExpr {
                            elements: exprs,
                            multiplicity: None,
                            source_info: self.cursor.span_since(&si),
                        }))
                    }
                }
            }
            // Bare lambda: x|body or x: Type[1]|body
            // Must be checked before identifier since both start with an identifier.
            TokenKind::Identifier if self.is_bare_lambda() => self.parse_bare_lambda(),
            // Identifier or element keyword used as a name.
            // Every identifier-like token (per `TokenKind::is_identifier_like`)
            // is valid in expression position — e.g., `Class('arg')`,
            // `my::Enum::VAL`, or `assertInstanceOf($x, Association)`.
            // The `PathSep` arm covers root-qualified paths (`::pkg::Name`).
            kind if kind.is_identifier_like() || kind == TokenKind::PathSep => {
                let path = self.parse_package_path()?;

                // Handle Measure~Unit references: RomanLength~Pes
                let tilde_suffix = if self.cursor.eat(TokenKind::Tilde) {
                    let (unit_name, unit_source_info) =
                        self.cursor.expect_identifier_or_keyword()?;
                    Some((unit_name, unit_source_info))
                } else {
                    None
                };

                // Span the whole FQN (`abc::Sub::Name`) for the
                // resulting `PackageableElementPtr.source_info` so the
                // goto-def index records a clickable region covering
                // every segment. Without this, the `si` we'd inherit
                // is just the position before the path — a single-
                // token span. Mirrors the merge done for type refs.
                let fqn_span = si.merge(legend_pure_parser_ast::source_info::Spanned::source_info(
                    &path,
                ));
                if self.cursor.check(TokenKind::LParen) {
                    // Function call: name(args)
                    self.cursor.advance();
                    let args = self.parse_separated(TokenKind::RParen, Self::parse_expression)?;
                    self.cursor.expect(TokenKind::RParen)?;
                    let (pkg, mut name) = split_package_name(&path);
                    if name.is_empty() {
                        return Err(empty_function_caller(self.cursor.span_since(&si)));
                    }
                    if let Some((unit, _)) = &tilde_suffix {
                        name = SmolStr::new(format!("{name}~{unit}"));
                    }
                    let func = PackageableElementPtr {
                        package: pkg,
                        name,
                        source_info: fqn_span.clone(),
                    };
                    Ok(Expression::FunctionApplication(FunctionApplication {
                        function: func,
                        arguments: args,
                        source_info: si,
                    }))
                } else if let Some((unit, unit_source_info)) = tilde_suffix {
                    // A unit as a value: RomanLength~Pes, positioned from
                    // the measure to the unit.
                    let (pkg, name) = split_package_name(&path);
                    let source_info = fqn_span.merge(&unit_source_info);
                    Ok(Expression::UnitType(UnitTypeRef {
                        unit: PackageableElementPtr {
                            package: pkg,
                            name: SmolStr::new(format!("{name}~{unit}")),
                            source_info: source_info.clone(),
                        },
                        source_info,
                    }))
                } else {
                    // Bare element reference (no parens): String, my::Enum, MyClass
                    let (pkg, name) = split_package_name(&path);
                    let element = PackageableElementPtr {
                        package: pkg,
                        name,
                        source_info: fqn_span.clone(),
                    };
                    Ok(Expression::PackageableElementRef(PackageableElementRef {
                        element,
                        source_info: fqn_span,
                    }))
                }
            }
            // Island: `#tag{ … }#`, parsed by its linked grammar.
            TokenKind::IslandOpen => self.parse_island(),

            // Navigation path expression: `#/Type/prop1/prop2(args)/prop3!alias#`.
            //
            // Distinct from the tagged-island path below — paths use `/`
            // as their step separator, not as a tag, and own a dedicated
            // opener token (`HashSlash`) emitted by the lexer. The body
            // is parsed inline (not via `IslandParser`) because Path is
            // a built-in language form, not an extension DSL.
            TokenKind::HashSlash => {
                self.cursor.advance();
                self.parse_navigation_path(si)
            }

            _ => Err(ParseError::expected(
                "expression",
                self.cursor.peek_kind(),
                si,
            )),
        }
    }

    /// Parses the body of a navigation-path expression after the leading
    /// `#/` has been consumed.
    ///
    /// Grammar:
    /// ```text
    /// nav-path  ::= start-type ('/' prop ('(' params ')')?)+ ('!' alias)? '#'
    /// start-type ::= type-reference   // supports type-args (`Firm<Any>`)
    /// prop       ::= identifier
    /// alias      ::= identifier
    /// ```
    ///
    /// At least one `/property` step is required — `#/Type#` is rejected
    /// at parse time, matching the Java grammar's "A path must contain at
    /// least one navigation" rule.
    ///
    /// Parameters are parsed as full expressions and may include scalar
    /// literals, collection literals (`['a', 'b']`), and enum stubs
    /// (`SynType.CUSIP` parses as `MemberAccess::Simple`).
    fn parse_navigation_path(&mut self, opener_si: SourceInfo) -> R<Expression> {
        let start_type = self.parse_type_reference()?;

        let mut path: Vec<PropertyPathElement> = Vec::new();
        while self.cursor.check(TokenKind::Slash) {
            let step_si = self.cursor.current_source_info();
            self.cursor.advance(); // consume '/'
            let (property, _prop_si) = self.cursor.expect_identifier_or_keyword()?;
            let parameters = if self.cursor.eat(TokenKind::LParen) {
                let mut args = Vec::new();
                if !self.cursor.check(TokenKind::RParen) {
                    loop {
                        args.push(self.parse_expression()?);
                        if !self.cursor.eat(TokenKind::Comma) {
                            break;
                        }
                    }
                }
                self.cursor.expect(TokenKind::RParen)?;
                args
            } else {
                Vec::new()
            };
            path.push(PropertyPathElement {
                property,
                parameters,
                source_info: self.cursor.span_since(&step_si),
            });
        }

        if path.is_empty() {
            return Err(ParseError::expected(
                "navigation step (`/property`) — a path must contain at least one navigation",
                self.cursor.peek_kind(),
                self.cursor.current_source_info(),
            ));
        }

        // Optional alias: `!identifier`. Java grammar uses `!` (lexed as
        // `Bang`) here, distinct from `!=` (`BangEqual`).
        let name = if self.cursor.eat(TokenKind::Bang) {
            let (alias, _) = self.cursor.expect_identifier_or_keyword()?;
            Some(alias)
        } else {
            None
        };

        self.cursor.expect(TokenKind::Hash)?;

        Ok(Expression::NavigationPath(NavigationPath {
            start_type,
            path,
            name,
            source_info: self.cursor.span_since(&opener_si),
        }))
    }

    /// An island, `#tag{ … }#`: its linked grammar parses the body, lexed
    /// again at its place in the document, and must consume it whole, as the
    /// engine re-parses an island's content with the island's own grammar. A
    /// grammar that reads its body raw gets it as written, never lexed; so
    /// does no grammar at all: an island without a linked grammar is
    /// unsupported.
    fn parse_island(&mut self) -> R<Expression> {
        let open = self.cursor.advance().clone();
        let tag = open
            .text
            .strip_prefix('#')
            .and_then(|t| t.strip_suffix('{'))
            .unwrap_or_default()
            .trim()
            .to_string();
        let body = self.cursor.expect(TokenKind::IslandContent)?;
        self.cursor.expect(TokenKind::RBraceHash)?;
        let span = self.cursor.span_since(&open.source_info);
        let Some(island_parser) = self.island_parsers.iter().find(|p| p.tag() == tag) else {
            let linked = self.island_parsers.iter().map(|p| p.tag());
            return Err(unlinked_island(&tag, linked, &open.source_info));
        };
        let content = parse_island_body(&body, span.clone(), island_parser.raw_body(), |ctx| {
            island_parser.parse(ctx)
        })?;
        Ok(Expression::Island(IslandExpression {
            content,
            source_info: span,
        }))
    }

    /// Detects whether the current token starts a lambda inside braces.
    ///
    /// Matches patterns after `{` has been consumed:
    /// - `{x |`            — untyped single param
    /// - `{x, y |`         — untyped multi param
    /// - `{x: Type[1] |`   — typed param
    /// - `{|`              — no-param braced lambda
    pub(crate) fn is_lambda_start(&self) -> bool {
        // {| body}
        if self.cursor.check(TokenKind::Pipe) {
            return true;
        }
        // {ident ... |}
        matches!(
            self.cursor.peek_kind(),
            TokenKind::Identifier | TokenKind::StringLiteral
        ) && matches!(
            self.cursor.peek_kind_at(1),
            TokenKind::Pipe | TokenKind::Comma | TokenKind::Colon
        )
    }

    /// Detects bare lambda in expression position (outside braces).
    ///
    /// Only single-param bare lambdas are valid (matching Java grammar):
    /// - `x|body`              — untyped
    /// - `x: Type[mult]|body`  — typed
    pub(crate) fn is_bare_lambda(&self) -> bool {
        // ident followed by pipe: x|body
        if matches!(self.cursor.peek_kind_at(1), TokenKind::Pipe) {
            return true;
        }
        // ident followed by colon: x: Type[mult]|body
        // We need to scan past the type + multiplicity to check for pipe
        if matches!(self.cursor.peek_kind_at(1), TokenKind::Colon) {
            return self.scan_past_type_for_pipe(2);
        }
        false
    }

    /// Scans forward from `offset` past a type reference and multiplicity
    /// bracket to check if the next token is `|` (pipe).
    ///
    /// Used to detect typed bare lambdas: `x: path::Type<Args>[mult]|body`,
    /// the type being a relation type `(col: Type[m], …)` or a function type
    /// `{Type[m]->Type[m]}` too.
    pub(crate) fn scan_past_type_for_pipe(&self, start_offset: usize) -> bool {
        let mut offset = start_offset;
        let bracketed = match self.cursor.peek_kind_at(offset) {
            TokenKind::LParen => Some((TokenKind::LParen, TokenKind::RParen)),
            TokenKind::LBrace => Some((TokenKind::LBrace, TokenKind::RBrace)),
            _ => None,
        };
        if let Some((open, close)) = bracketed {
            let Some(next) = self.offset_past_group(offset, open, close) else {
                return false;
            };
            return self.multiplicity_then_pipe(next);
        }
        // Skip the type path: identifier (:: identifier)*
        // Element keywords (`Class`, `Association`, `Profile`, `Measure`,
        // `Primitive`, etc.) are valid type names, so accept anything
        // that's identifier-like — not just plain `Identifier`.
        if !self.cursor.peek_kind_at(offset).is_identifier_like() {
            return false;
        }
        offset += 1;
        // Skip :: segments
        while matches!(self.cursor.peek_kind_at(offset), TokenKind::PathSep) {
            offset += 1; // ::
            offset += 1; // ident
        }
        // Skip <TypeArgs>
        if matches!(self.cursor.peek_kind_at(offset), TokenKind::Less) {
            let mut depth = 1;
            offset += 1;
            while depth > 0 {
                match self.cursor.peek_kind_at(offset) {
                    TokenKind::Less => depth += 1,
                    TokenKind::Greater => depth -= 1,
                    TokenKind::Eof => return false,
                    _ => {}
                }
                offset += 1;
            }
        }
        self.multiplicity_then_pipe(offset)
    }

    /// Whether a multiplicity bracket then `|` start at `offset`.
    fn multiplicity_then_pipe(&self, mut offset: usize) -> bool {
        // Expect [multiplicity]
        if !matches!(self.cursor.peek_kind_at(offset), TokenKind::LBracket) {
            return false;
        }
        offset += 1;
        // Skip multiplicity content until ]
        while !matches!(
            self.cursor.peek_kind_at(offset),
            TokenKind::RBracket | TokenKind::Eof
        ) {
            offset += 1;
        }
        if matches!(self.cursor.peek_kind_at(offset), TokenKind::RBracket) {
            offset += 1;
        }
        // Check for pipe
        matches!(self.cursor.peek_kind_at(offset), TokenKind::Pipe)
    }

    /// The offset past the group `open` starts at `offset`, its nested groups
    /// included, or `None` when the input ends first.
    fn offset_past_group(
        &self,
        mut offset: usize,
        open: TokenKind,
        close: TokenKind,
    ) -> Option<usize> {
        let mut depth = 0usize;
        loop {
            match self.cursor.peek_kind_at(offset) {
                kind if kind == open => depth += 1,
                kind if kind == close => {
                    depth -= 1;
                    if depth == 0 {
                        return Some(offset + 1);
                    }
                }
                TokenKind::Eof => return None,
                _ => {}
            }
            offset += 1;
        }
    }

    /// Parses a bare lambda: `x|body` or `x: Type[mult]|body`.
    ///
    /// Only single-param bare lambdas are valid per the Java grammar.
    pub(crate) fn parse_bare_lambda(&mut self) -> R<Expression> {
        let param = self.parse_lambda_param()?;
        // The engine's `lambdaPipe`: the pipe and code block, without the
        // parameter.
        let si = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::Pipe)?;

        let mut body = vec![self.parse_expression()?];
        while self.cursor.eat(TokenKind::Semicolon) {
            if self.cursor.check(TokenKind::RParen)
                || self.cursor.check(TokenKind::RBrace)
                || self.cursor.check(TokenKind::RBracket)
                || self.cursor.check(TokenKind::Comma)
                || self.cursor.check(TokenKind::Eof)
            {
                break;
            }
            body.push(self.parse_expression()?);
        }

        Ok(Expression::Lambda(Lambda {
            parameters: vec![param],
            body,
            source_info: self.cursor.span_since(&si),
        }))
    }

    /// Parses a lambda parameter: `name` or `name: Type[multiplicity]`.
    pub(crate) fn parse_lambda_param(&mut self) -> R<Parameter> {
        let si = self.cursor.current_source_info();
        let (name, _) = self.cursor.expect_identifier()?;
        if self.cursor.eat(TokenKind::Colon) {
            let type_ref = self.parse_type_spec()?;
            self.cursor.expect(TokenKind::LBracket)?;
            let multiplicity = self.parse_multiplicity()?;
            self.cursor.expect(TokenKind::RBracket)?;
            Ok(Parameter {
                name,
                type_ref: Some(type_ref),
                multiplicity: Some(multiplicity),
                source_info: self.cursor.span_since(&si),
            })
        } else {
            Ok(Parameter {
                name,
                type_ref: None,
                multiplicity: None,
                source_info: si,
            })
        }
    }

    /// The columns after a `~`: one `oneColSpec`, or `[…]` of them.
    pub(crate) fn parse_column_builder(&mut self) -> R<Expression> {
        let start = self.cursor.current_source_info();
        let mut columns = vec![];
        let is_array = if self.cursor.eat(TokenKind::LBracket) {
            if !self.cursor.check(TokenKind::RBracket) {
                loop {
                    columns.push(self.parse_one_col_spec()?);
                    if !self.cursor.eat(TokenKind::Comma) {
                        break;
                    }
                }
            }
            self.cursor.expect(TokenKind::RBracket)?;
            true
        } else {
            columns.push(self.parse_one_col_spec()?);
            false
        };
        Ok(Expression::Column(
            legend_pure_parser_ast::expression::ColumnBuilderExpr {
                columns,
                is_array,
                source_info: self.cursor.span_since(&start),
            },
        ))
    }

    fn parse_one_col_spec(&mut self) -> R<legend_pure_parser_ast::expression::ColumnSpec> {
        use legend_pure_parser_ast::expression::{ColumnSpec, ColumnTypeSpec};

        let si = self.cursor.current_source_info();
        let stereotypes = self.parse_stereotypes()?;
        let tagged_values = self.parse_tagged_values()?;
        // `columnName: identifier | STRING`: a quoted name (`~'col name'`)
        // arrives unquoted and unescaped from the cursor, like every quoted
        // identifier.
        let (name, _) = self.cursor.expect_identifier_or_keyword()?;

        let mut type_spec = None;
        let mut extra_function = None;

        if self.cursor.eat(TokenKind::Colon) {
            // Java grammar:
            //   oneColSpec: ... columnName (COLON (type multiplicity? | anyLambda) extraFunction?)?
            //   extraFunction: COLON anyLambda
            //
            // The first piece after `:` is either a type-with-mult or a
            // primary lambda (init function); the optional second piece
            // after another `:` is the aggregator lambda.
            if self.cursor.check(TokenKind::LBrace) || self.is_bare_lambda() {
                let expr = self.parse_expression()?;
                if let Expression::Lambda(l) = expr {
                    type_spec = Some(ColumnTypeSpec::Lambda(l));
                } else {
                    // Non-lambda expression in lambda position is unusual
                    // but harmless — keep it as the extra-function carrier
                    // so the resolver can still see it.
                    extra_function = Some(Box::new(expr));
                }
            } else {
                let type_ref = self.parse_type_reference()?;
                let mult = if self.cursor.check(TokenKind::LBracket) {
                    self.cursor.advance();
                    let m = self.parse_multiplicity()?;
                    self.cursor.expect(TokenKind::RBracket)?;
                    Some(m)
                } else {
                    None
                };
                type_spec = Some(ColumnTypeSpec::Typed(type_ref, mult));
            }

            // Optional `extraFunction`: `: anyLambda` after the primary.
            // Mirrors Java's `extraFunction: COLON anyLambda`.
            if self.cursor.eat(TokenKind::Colon) {
                let expr = self.parse_expression()?;
                extra_function = Some(Box::new(expr));
            }
        }

        Ok(ColumnSpec {
            stereotypes,
            tagged_values,
            name,
            type_spec,
            extra_function,
            source_info: self.cursor.span_since(&si),
        })
    }

    /// After parsing a numeric literal, check if it's followed by a
    /// `Measure~Unit` reference (e.g., `5 RomanLength~Pes`).
    /// If so, wrap them into a `UnitInstanceExpr`.
    fn maybe_unit_instance(
        &mut self,
        num_expr: Expression,
        si: legend_pure_parser_ast::SourceInfo,
    ) -> R<Expression> {
        // Look ahead: Identifier followed by Tilde means unit value
        if self.cursor.peek_kind() == TokenKind::Identifier
            && self.cursor.peek_kind_at(1) == TokenKind::Tilde
        {
            let (measure_name, _) = self.cursor.expect_identifier_or_keyword()?;
            self.cursor.expect(TokenKind::Tilde)?;
            let (unit_name, _) = self.cursor.expect_identifier_or_keyword()?;
            let combined = SmolStr::new(format!("{measure_name}~{unit_name}"));
            let end_si = self.cursor.current_source_info();
            Ok(Expression::UnitInstance(UnitInstanceExpr {
                value: Box::new(num_expr),
                unit: PackageableElementPtr {
                    package: None,
                    name: combined,
                    source_info: end_si,
                },
                source_info: si,
            }))
        } else {
            Ok(num_expr)
        }
    }
}

/// Parses an island's body with a plug-in grammar: the body lexed again at
/// its place in the document, which the grammar must consume whole, or, for
/// a grammar that reads it `raw`, only its text. `span` is what the grammar
/// sees as the island's span.
pub(super) fn parse_island_body<T>(
    body: &legend_pure_parser_lexer::Token,
    span: legend_pure_parser_ast::SourceInfo,
    raw: bool,
    parse: impl FnOnce(&mut ParserContext<'_>) -> R<T>,
) -> R<T> {
    let tokens = if raw {
        vec![legend_pure_parser_lexer::Token {
            kind: TokenKind::Eof,
            text: SmolStr::default(),
            source_info: body.source_info.clone(),
        }]
    } else {
        let line = i32::try_from(body.source_info.start_line).unwrap_or(i32::MAX);
        let column = i32::try_from(body.source_info.start_column).unwrap_or(i32::MAX);
        let offset =
            legend_pure_parser_ast::SourceOffset::new(line - 1, column - 1).unwrap_or_default();
        legend_pure_parser_lexer::tokenize_with_offset(
            &body.text,
            &body.source_info.source,
            offset,
        )?
    };
    let mut body_parser =
        Parser::with_island_parsers(crate::cursor::Cursor::new(tokens), Vec::new());
    let result = parse(&mut ParserContext {
        parser: &mut body_parser,
        island: Some(super::Island {
            span,
            text: body.text.clone(),
        }),
    })?;
    if !body_parser.cursor.check(TokenKind::Eof) {
        return Err(ParseError::expected(
            "the end of the island",
            body_parser.cursor.peek_kind(),
            body_parser.cursor.current_source_info(),
        ));
    }
    if let Some(error) = body_parser.deferred.take() {
        return Err(error);
    }
    Ok(result)
}

/// `::.all()` or `::(…)`: the root package calls nothing (the engine's
/// `instanceReference`).
fn empty_function_caller(source_info: legend_pure_parser_ast::SourceInfo) -> ParseError {
    ParseError::unexpected("Expected a non-empty function caller", source_info)
}

/// An island whose tag has no linked grammar: unsupported, so a caller
/// with its own island parsers (the engine) can parse the section instead.
/// The message names the islands that are linked.
fn unlinked_island<'a>(
    tag: &str,
    linked: impl Iterator<Item = &'a str>,
    source_info: &legend_pure_parser_ast::SourceInfo,
) -> ParseError {
    let mut linked: Vec<String> = linked.map(|t| format!("#{t}{{…}}#")).collect();
    linked.sort();
    let linked = if linked.is_empty() {
        "none".to_string()
    } else {
        linked.join(", ")
    };
    ParseError::Unsupported {
        message: format!(
            "no island grammar is linked for '#{tag}{{…}}#'; the linked ones are: {linked}"
        ),
        source_info: source_info.clone(),
    }
}
