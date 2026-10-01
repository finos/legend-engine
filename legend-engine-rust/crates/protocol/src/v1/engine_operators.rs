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

//! Legend Engine's operator-expression shapes.
//!
//! The engine grammar (`M3ParserGrammar.g4`) does not encode operator
//! precedence. A `combinedExpression` is an `expression` followed by a flat
//! list of parts — runs of one arithmetic operator (`+ b + c`), single
//! relational comparisons (`< d`) and boolean parts (`&& e`) — and `==` /
//! `!=` bind inside `expression` to everything arithmetic on their right.
//! `DomainParseTreeWalker` then assembles the protocol tree from that list
//! with its own algorithm (`combinedExpression`, `arithmeticPart`,
//! `processOp`, `booleanPart`, `processBooleanOp`), which fixes up only one
//! level of precedence. The resulting shapes are pinned by the engine's own
//! tests: `$x > 0 && $x < 30` becomes
//! `lessThan(and(greaterThan($x, 0), $x), 30)`.
//!
//! The Rust parser builds a conventional precedence tree. To emit exactly
//! what the engine emits, [`convert_operator_chain`] flattens a maximal
//! operator chain back into source order (a parenthesised group stays a
//! single operand, as `(combinedExpression)` does in the engine grammar),
//! re-parses it with the engine's grammar rules and replays the walker's
//! building algorithm, source information included.

use legend_pure_parser_ast::SourceInfo;
use legend_pure_parser_ast::expression::{ArithmeticOp, ComparisonOp, Expression, LogicalOp};
use legend_pure_parser_ast::source_info::Spanned;

use super::convert::{collection_multiplicity, convert_expression_typed};
use crate::error::{ConvertError, Result};
use crate::v1::source_info::SourceInformation;
use crate::v1::value_spec::{AppliedFunction, ProtocolCollection, ValueSpecification};

/// Converts the operator chain rooted at `expr` into the engine's shape.
///
/// # Errors
///
/// Any operand conversion failure.
pub(super) fn convert_operator_chain(expr: &Expression) -> Result<ValueSpecification> {
    let mut items = Vec::new();
    flatten(expr, expr.source_info(), &mut items);
    let mut replay = Replay { items, pos: 0 };
    let built = replay.combined_expression()?;
    if replay.pos != replay.items.len() {
        return Err(ConvertError::plugin(
            "operator chain has tokens the engine grammar would not consume",
            Some(expr.source_info()),
        ));
    }
    Ok(built.value)
}

// ---------------------------------------------------------------------------
// Flattening
// ---------------------------------------------------------------------------

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum Op {
    Plus,
    Minus,
    Times,
    Divide,
    LessThan,
    LessThanEqual,
    GreaterThan,
    GreaterThanEqual,
    Equal,
    NotEqual,
    And,
    Or,
}

impl Op {
    fn function(self) -> &'static str {
        match self {
            Self::Plus => "plus",
            Self::Minus => "minus",
            Self::Times => "times",
            Self::Divide => "divide",
            Self::LessThan => "lessThan",
            Self::LessThanEqual => "lessThanEqual",
            Self::GreaterThan => "greaterThan",
            Self::GreaterThanEqual => "greaterThanEqual",
            Self::Equal | Self::NotEqual => "equal",
            Self::And => "and",
            Self::Or => "or",
        }
    }

    /// `+ - * /`: an `arithmeticPart` repeats one of these (`+ b + c`).
    fn is_run(self) -> bool {
        matches!(self, Self::Plus | Self::Minus | Self::Times | Self::Divide)
    }

    fn is_relational(self) -> bool {
        matches!(
            self,
            Self::LessThan | Self::LessThanEqual | Self::GreaterThan | Self::GreaterThanEqual
        )
    }

    fn is_boolean(self) -> bool {
        matches!(self, Self::And | Self::Or)
    }

    fn is_equality(self) -> bool {
        matches!(self, Self::Equal | Self::NotEqual)
    }

    fn is_product(self) -> bool {
        matches!(self, Self::Times | Self::Divide)
    }

    fn is_additive(self) -> bool {
        matches!(self, Self::Plus | Self::Minus)
    }
}

impl From<ArithmeticOp> for Op {
    fn from(op: ArithmeticOp) -> Self {
        match op {
            ArithmeticOp::Plus => Self::Plus,
            ArithmeticOp::Minus => Self::Minus,
            ArithmeticOp::Times => Self::Times,
            ArithmeticOp::Divide => Self::Divide,
        }
    }
}

impl From<ComparisonOp> for Op {
    fn from(op: ComparisonOp) -> Self {
        match op {
            ComparisonOp::Equal => Self::Equal,
            ComparisonOp::NotEqual => Self::NotEqual,
            ComparisonOp::LessThan => Self::LessThan,
            ComparisonOp::LessThanOrEqual => Self::LessThanEqual,
            ComparisonOp::GreaterThan => Self::GreaterThan,
            ComparisonOp::GreaterThanOrEqual => Self::GreaterThanEqual,
        }
    }
}

impl From<LogicalOp> for Op {
    fn from(op: LogicalOp) -> Self {
        match op {
            LogicalOp::And => Self::And,
            LogicalOp::Or => Self::Or,
        }
    }
}

/// One element of a flattened chain, in source order.
enum Item<'a> {
    /// A non-operator expression and its full extent.
    Operand {
        expr: &'a Expression,
        extent: &'a SourceInfo,
    },
    /// A binary operator and its token.
    Binary { op: Op, token: &'a SourceInfo },
    /// Prefix `!` and its token.
    Not { token: &'a SourceInfo },
    /// Prefix `-` and its token.
    Negate { token: &'a SourceInfo },
}

/// Appends the chain rooted at `expr` (whose full extent is `extent`) to
/// `out` in source order. In-order traversal of a precedence tree
/// reproduces the token order; groups and every other expression kind are
/// operands.
fn flatten<'a>(expr: &'a Expression, extent: &'a SourceInfo, out: &mut Vec<Item<'a>>) {
    let binary = |left: &'a Expression,
                  left_span: &'a SourceInfo,
                  op: Op,
                  token: &'a SourceInfo,
                  right: &'a Expression,
                  right_span: &'a SourceInfo,
                  out: &mut Vec<Item<'a>>| {
        flatten(left, left_span, out);
        out.push(Item::Binary { op, token });
        flatten(right, right_span, out);
    };
    match expr {
        Expression::Arithmetic(e) => binary(
            &e.left,
            &e.left_span,
            e.op.into(),
            &e.source_info,
            &e.right,
            &e.right_span,
            out,
        ),
        Expression::Comparison(e) => binary(
            &e.left,
            &e.left_span,
            e.op.into(),
            &e.source_info,
            &e.right,
            &e.right_span,
            out,
        ),
        Expression::Logical(e) => binary(
            &e.left,
            &e.left_span,
            e.op.into(),
            &e.source_info,
            &e.right,
            &e.right_span,
            out,
        ),
        Expression::Not(e) => {
            out.push(Item::Not {
                token: &e.source_info,
            });
            flatten(&e.operand, &e.operand_span, out);
        }
        Expression::UnaryMinus(e) => {
            out.push(Item::Negate {
                token: &e.source_info,
            });
            flatten(&e.operand, &e.operand_span, out);
        }
        _ => out.push(Item::Operand { expr, extent }),
    }
}

// ---------------------------------------------------------------------------
// Replay of the engine grammar + DomainParseTreeWalker
// ---------------------------------------------------------------------------

/// A built value and the end of the last token it consumed (the engine's
/// `ctx.getStop()`), used to span the parts that end with it.
struct Built {
    value: ValueSpecification,
    end: SourceInfo,
}

/// `arithmeticPart`: a run of one of `+ - * /`, or one relational comparison.
struct ArithmeticPart {
    op: Op,
    /// From the first operator token to the end of the last operand.
    span: SourceInfo,
    operands: Vec<ValueSpecification>,
}

/// `booleanPart`: `&& expression` or `|| expression`.
struct BooleanPart {
    op: Op,
    token: SourceInfo,
    operand: Box<ValueSpecification>,
    /// End of the operand's last token.
    end: SourceInfo,
}

enum Part {
    Arithmetic(ArithmeticPart),
    Boolean(BooleanPart),
}

struct Replay<'a> {
    items: Vec<Item<'a>>,
    pos: usize,
}

impl<'a> Replay<'a> {
    fn peek(&self) -> Option<&Item<'a>> {
        self.items.get(self.pos)
    }

    fn peek_binary(&self) -> Option<(Op, &'a SourceInfo)> {
        match self.peek() {
            Some(Item::Binary { op, token }) => Some((*op, *token)),
            _ => None,
        }
    }

    /// `combinedExpression: expression expressionPart*`, built as the
    /// walker's `combinedExpression` does: consecutive arithmetic parts are
    /// applied as one group, consecutive boolean parts as another, each
    /// group taking the previous group's result as its input.
    fn combined_expression(&mut self) -> Result<Built> {
        let first = self.expression()?;
        let parts = self.parts(true)?;
        let end = parts.last().map_or_else(|| first.end.clone(), part_end);

        let mut bool_result = first.value.clone();
        let mut arith_result = first.value.clone();
        let mut arithmetic: Vec<ArithmeticPart> = Vec::new();
        let mut boolean: Vec<BooleanPart> = Vec::new();
        for part in parts {
            match part {
                Part::Arithmetic(p) => {
                    if !boolean.is_empty() {
                        bool_result =
                            boolean_part(std::mem::take(&mut boolean), arith_result.clone());
                    }
                    arithmetic.push(p);
                }
                Part::Boolean(p) => {
                    if !arithmetic.is_empty() {
                        arith_result =
                            arithmetic_part(std::mem::take(&mut arithmetic), &bool_result)?;
                    }
                    boolean.push(p);
                }
            }
        }
        let value = if !arithmetic.is_empty() {
            arithmetic_part(arithmetic, &bool_result)?
        } else if !boolean.is_empty() {
            boolean_part(boolean, arith_result)
        } else {
            first.value
        };
        Ok(Built { value, end })
    }

    /// `combinedArithmeticOnly: expression arithmeticPart*` (the right side
    /// of `==` / `!=`).
    fn combined_arithmetic_only(&mut self) -> Result<Built> {
        let first = self.expression()?;
        let parts = self.parts(false)?;
        let Some(end) = parts.last().map(part_end) else {
            return Ok(first);
        };
        let arithmetic = parts
            .into_iter()
            .filter_map(|p| match p {
                Part::Arithmetic(a) => Some(a),
                Part::Boolean(_) => None,
            })
            .collect();
        Ok(Built {
            value: arithmetic_part(arithmetic, &first.value)?,
            end,
        })
    }

    /// `expression: nonArrowOrEqualExpression (…)* (equalNotEqual)?` — an
    /// operand, or `!` / `-` applied to an `expression`, optionally followed
    /// by `== combinedArithmeticOnly` / `!= combinedArithmeticOnly`.
    fn expression(&mut self) -> Result<Built> {
        let mut built = match self.items.get(self.pos) {
            Some(Item::Operand { expr, extent }) => {
                let (expr, extent) = (*expr, *extent);
                self.pos += 1;
                Built {
                    value: convert_expression_typed(expr)?,
                    end: extent.clone(),
                }
            }
            Some(Item::Not { token }) => {
                let token = *token;
                self.pos += 1;
                let negated = self.expression()?;
                Built {
                    value: func("not", vec![negated.value], &join(token, &negated.end)),
                    end: negated.end,
                }
            }
            Some(Item::Negate { token }) => {
                let token = *token;
                self.pos += 1;
                let number = self.expression()?;
                Built {
                    value: func("minus", vec![number.value], token),
                    end: number.end,
                }
            }
            Some(Item::Binary { token, .. }) => {
                return Err(ConvertError::plugin(
                    "operator where the engine grammar expects an operand",
                    Some(token),
                ));
            }
            None => {
                return Err(ConvertError::plugin(
                    "operator chain ends where the engine grammar expects an operand",
                    None,
                ));
            }
        };
        if let Some((op, token)) = self.peek_binary().filter(|(op, _)| op.is_equality()) {
            self.pos += 1;
            let other = self.combined_arithmetic_only()?;
            let equal = func(op.function(), vec![built.value, other.value], token);
            let value = if op == Op::NotEqual {
                func("not", vec![equal], token)
            } else {
                equal
            };
            built = Built {
                value,
                end: other.end,
            };
        }
        Ok(built)
    }

    /// `expressionPart*` (or `arithmeticPart*` when `allow_boolean` is off).
    fn parts(&mut self, allow_boolean: bool) -> Result<Vec<Part>> {
        let mut parts = Vec::new();
        while let Some((op, token)) = self.peek_binary() {
            if op.is_run() || op.is_relational() {
                self.pos += 1;
                let mut operands = vec![self.expression()?];
                if op.is_run() {
                    while self.peek_binary().is_some_and(|(next, _)| next == op) {
                        self.pos += 1;
                        operands.push(self.expression()?);
                    }
                }
                let end = operands
                    .last()
                    .map_or_else(|| token.clone(), |b| b.end.clone());
                parts.push(Part::Arithmetic(ArithmeticPart {
                    op,
                    span: join(token, &end),
                    operands: operands.into_iter().map(|b| b.value).collect(),
                }));
            } else if op.is_boolean() && allow_boolean {
                self.pos += 1;
                let operand = self.expression()?;
                parts.push(Part::Boolean(BooleanPart {
                    op,
                    token: token.clone(),
                    operand: Box::new(operand.value),
                    end: operand.end,
                }));
            } else {
                break;
            }
        }
        Ok(parts)
    }
}

fn part_end(part: &Part) -> SourceInfo {
    match part {
        Part::Arithmetic(p) => p.span.clone(),
        Part::Boolean(p) => p.end.clone(),
    }
}

/// The walker's `arithmeticPart`: applies a group of arithmetic parts to
/// `initial`, fixing up one level of precedence via `processOp`.
fn arithmetic_part(
    parts: Vec<ArithmeticPart>,
    initial: &ValueSpecification,
) -> Result<ValueSpecification> {
    let mut applied: Option<AppliedFunction> = None;
    for part in parts {
        applied = Some(if part.op.is_relational() {
            let input = applied.map_or_else(|| initial.clone(), ValueSpecification::Func);
            AppliedFunction {
                function: part.op.function().to_string(),
                f_control: None,
                parameters: vec![input, first_operand(&part)?],
                source_information: Some(SourceInformation::from(&part.span)),
            }
        } else {
            process_op(applied, &part, initial)?
        });
    }
    applied
        .map(ValueSpecification::Func)
        .ok_or_else(|| ConvertError::plugin("arithmetic group without parts", None))
}

/// The walker's `processOp`.
fn process_op(
    applied: Option<AppliedFunction>,
    part: &ArithmeticPart,
    initial: &ValueSpecification,
) -> Result<AppliedFunction> {
    let Some(mut applied) = applied else {
        return build(part, initial.clone());
    };
    if !is_strictly_lower_precedence(&applied.function, part.op) {
        return build(part, ValueSpecification::Func(applied));
    }
    // The current part binds tighter than the previous operation: rebuild
    // the previous operation's last operand with the current part applied.
    if applied.function == "divide" || is_relational_function(&applied.function) {
        let last = applied.parameters.pop();
        let first = applied.parameters.first().cloned();
        let (Some(first), Some(last)) = (first, last) else {
            return Err(ConvertError::plugin(
                "binary operation without operands",
                None,
            ));
        };
        let rebuilt = build(part, last)?;
        applied.parameters = vec![first, ValueSpecification::Func(rebuilt)];
    } else {
        let Some(ValueSpecification::Collection(collection)) = applied.parameters.first() else {
            return Err(ConvertError::plugin(
                "arithmetic operation without its operand collection",
                None,
            ));
        };
        let mut values = collection.values.clone();
        let Some(last) = values.pop() else {
            return Err(ConvertError::plugin(
                "empty arithmetic operand collection",
                None,
            ));
        };
        values.push(ValueSpecification::Func(build(part, last)?));
        applied.parameters = vec![collect(values, &part.span)];
    }
    Ok(applied)
}

/// `buildArithmeticOp` (`plus`, `minus`, `times`: one collection of all
/// operands) and `buildDivide` (left-nested binary `divide`s).
fn build(part: &ArithmeticPart, initial: ValueSpecification) -> Result<AppliedFunction> {
    if part.op == Op::Divide {
        let mut acc = initial;
        for operand in &part.operands {
            acc = func("divide", vec![acc, operand.clone()], &part.span);
        }
        return match acc {
            ValueSpecification::Func(f) => Ok(f),
            _ => Err(ConvertError::plugin("divide part without operands", None)),
        };
    }
    let mut values = Vec::with_capacity(part.operands.len() + 1);
    values.push(initial);
    values.extend(part.operands.iter().cloned());
    Ok(AppliedFunction {
        function: part.op.function().to_string(),
        f_control: None,
        parameters: vec![collect(values, &part.span)],
        source_information: Some(SourceInformation::from(&part.span)),
    })
}

/// The walker's `booleanPart` / `processBooleanOp`: `and` binds tighter
/// than a preceding `or`; everything else associates to the left.
fn boolean_part(parts: Vec<BooleanPart>, input: ValueSpecification) -> ValueSpecification {
    let mut applied: Option<AppliedFunction> = None;
    for part in parts {
        let build_boolean = |input: ValueSpecification| AppliedFunction {
            function: part.op.function().to_string(),
            f_control: None,
            parameters: vec![input, (*part.operand).clone()],
            source_information: Some(SourceInformation::from(&part.token)),
        };
        applied = Some(match applied {
            None => build_boolean(input.clone()),
            Some(mut previous) if previous.function == "or" && part.op == Op::And => {
                match (
                    previous.parameters.pop(),
                    previous.parameters.first().cloned(),
                ) {
                    (Some(last), Some(first)) => {
                        previous.parameters =
                            vec![first, ValueSpecification::Func(build_boolean(last))];
                        previous
                    }
                    _ => build_boolean(ValueSpecification::Func(previous)),
                }
            }
            Some(previous) => build_boolean(ValueSpecification::Func(previous)),
        });
    }
    applied.map_or(input, ValueSpecification::Func)
}

fn first_operand(part: &ArithmeticPart) -> Result<ValueSpecification> {
    part.operands
        .first()
        .cloned()
        .ok_or_else(|| ConvertError::plugin("comparison without operand", None))
}

fn is_relational_function(function: &str) -> bool {
    matches!(
        function,
        "lessThan" | "lessThanEqual" | "greaterThan" | "greaterThanEqual"
    )
}

/// The walker's `isStrictlyLowerPrecedence(previous, current)`.
fn is_strictly_lower_precedence(previous: &str, current: Op) -> bool {
    let additive = matches!(previous, "plus" | "minus");
    (is_relational_function(previous) && (current.is_additive() || current.is_product()))
        || (additive && current.is_product())
}

fn func(name: &str, parameters: Vec<ValueSpecification>, si: &SourceInfo) -> ValueSpecification {
    ValueSpecification::Func(AppliedFunction {
        function: name.to_string(),
        f_control: None,
        parameters,
        source_information: Some(SourceInformation::from(si)),
    })
}

fn collect(values: Vec<ValueSpecification>, si: &SourceInfo) -> ValueSpecification {
    ValueSpecification::Collection(ProtocolCollection {
        multiplicity: collection_multiplicity(values.len()),
        values,
        source_information: Some(SourceInformation::from(si)),
    })
}

/// The span from the start of `start` to the end of `end`.
fn join(start: &SourceInfo, end: &SourceInfo) -> SourceInfo {
    SourceInfo::new(
        start.source.clone(),
        start.start_line,
        start.start_column,
        end.end_line,
        end.end_column,
    )
}
