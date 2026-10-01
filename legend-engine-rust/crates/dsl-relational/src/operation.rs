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

//! Relational operations: joins' and filters' conditions, views' columns
//! (and, later, relational mappings'), the engine's `operation` rules and
//! `RelationalParseTreeWalker.visitOperation`:
//!
//! ```text
//! operation:        booleanOperation
//! booleanOperation: atomicOperation (('and' | 'or') operation)?
//! atomicOperation:  ( '(' operation ')' | [db]? function | '$' name | column
//!                   | joinOperation | constant )
//!                   (comparison atomicOperation | 'is null' | 'is not null')?
//! joinOperation:    [db]? joinSequence ('|' booleanOperation)?
//! joinSequence:     ('(' joinType ')')? '@' join ('>' ('(' joinType ')')? [db]? '@' join)*
//! ```
//!
//! As the walker, the parser resolves each column's table from the
//! [`Scope`] and positions operations its way: an operator on its right
//! side (`= 'A'`), its left operand on the whole comparison.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::string_value;
use smol_str::SmolStr;

use crate::ast::{JoinPointer, LiteralValue, Operation, TableAliasColumn, TablePtr};
use crate::tokens::{
    Name, adjacent, at_relational_identifier, at_signed_number, at_target, database_pointer,
    eat_words, identifier, integer, is_quoted_string, is_string, is_word, is_word_kind,
    relational_identifier, unexpected,
};

/// What a column without a table (or a table without a schema) belongs to:
/// the engine's walker's `ScopeInfo`.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct Scope {
    /// The database.
    pub database: Option<String>,
    /// The schema, as its name's token: inside a schema's view, the table of
    /// a `column` without one.
    pub schema: Option<Name>,
    /// The table, as its name's token.
    pub table: Option<Name>,
}

impl Scope {
    /// The database's scope.
    #[must_use]
    pub fn database(database: String) -> Self {
        Self {
            database: Some(database),
            ..Self::default()
        }
    }

    /// This scope in `database`, whether or not it has one.
    #[must_use]
    pub fn with_database(&self, database: Option<String>) -> Self {
        Self {
            database,
            ..self.clone()
        }
    }
}

/// `booleanOperation`: one operation.
///
/// # Errors
///
/// A syntax error, or one of the walker's (a column without a table, an
/// unsupported join type).
pub fn operation(ctx: &mut ParserContext<'_>, scope: &Scope) -> Result<Operation, ParseError> {
    let left = atomic_operation(ctx, scope)?;
    let operator = ctx.cursor().peek().clone();
    let (name, inverse) = if is_word(&operator, "and") {
        ("and", "or")
    } else if is_word(&operator, "or") {
        ("or", "and")
    } else {
        return Ok(left);
    };
    ctx.cursor().advance();
    let right = operation(ctx, scope)?;
    // From the operator to the right side's end.
    let source_info = ctx.cursor().span_since(&operator.source_info);
    let mut parameters = vec![left];
    match right {
        // `a and b and c` is one `and` of three.
        Operation::DynaFunc {
            name: right_name,
            parameters: right_parameters,
            ..
        } if right_name == name => parameters.extend(right_parameters),
        // `a and b or c` groups `b or c`, as it is read.
        right @ Operation::DynaFunc { .. } if dyna_func_name(&right) == Some(inverse) => {
            let group_source_info = right.source_info().clone();
            parameters.push(Operation::DynaFunc {
                name: SmolStr::new_static("group"),
                parameters: vec![right],
                source_info: group_source_info,
            });
        }
        right => parameters.push(right),
    }
    Ok(Operation::DynaFunc {
        name: SmolStr::new_static(name),
        parameters,
        source_info,
    })
}

fn dyna_func_name(operation: &Operation) -> Option<&str> {
    match operation {
        Operation::DynaFunc { name, .. } => Some(name),
        _ => None,
    }
}

/// `atomicOperation`: an operand, compared or tested for null. The walker
/// positions the operand on the whole atomic operation, and the comparison
/// on its operator and right side.
fn atomic_operation(ctx: &mut ParserContext<'_>, scope: &Scope) -> Result<Operation, ParseError> {
    let start = ctx.cursor().current_source_info();
    let (mut operand, is_join) = operand(ctx, scope, &start)?;
    let operator_start = ctx.cursor().current_source_info();
    let comparison = if let Some(name) = comparison_operator(ctx) {
        let right = atomic_operation(ctx, scope)?;
        Some((name, Some(right)))
    } else {
        null_test(ctx).map(|name| (name, None))
    };
    // The walker returns a join operation as it is, whatever follows it.
    if is_join {
        return Ok(operand);
    }
    operand.set_source_info(ctx.cursor().span_since(&start));
    let Some((name, right)) = comparison else {
        return Ok(operand);
    };
    let mut parameters = vec![operand];
    parameters.extend(right);
    Ok(Operation::DynaFunc {
        name: SmolStr::new_static(name),
        parameters,
        source_info: ctx.cursor().span_since(&operator_start),
    })
}

/// A comparison operator, consumed: `=`, `!=`, `<>`, `>`, `<`, `>=`, `<=`.
fn comparison_operator(ctx: &mut ParserContext<'_>) -> Option<&'static str> {
    let cursor = ctx.cursor();
    let name = match cursor.peek_kind() {
        TokenKind::Equals => "equal",
        TokenKind::BangEqual => "notEqual",
        TokenKind::GreaterEqual => "greaterThanEqual",
        TokenKind::LessEqual => "lessThanEqual",
        TokenKind::Greater => "greaterThan",
        TokenKind::Less
            if cursor.peek_kind_at(1) == TokenKind::Greater
                && adjacent(cursor.peek(), cursor.peek_at(1)) =>
        {
            cursor.advance();
            "notEqualAnsi"
        }
        TokenKind::Less => "lessThan",
        _ => return None,
    };
    cursor.advance();
    Some(name)
}

/// `is null` or `is not null`, consumed.
fn null_test(ctx: &mut ParserContext<'_>) -> Option<&'static str> {
    if eat_words(ctx, &["is", "null"]).is_some() {
        Some("isNull")
    } else if eat_words(ctx, &["is", "not", "null"]).is_some() {
        Some("isNotNull")
    } else {
        None
    }
}

/// An atomic operation's operand, and whether it is a join operation.
fn operand(
    ctx: &mut ParserContext<'_>,
    scope: &Scope,
    start: &SourceInfo,
) -> Result<(Operation, bool), ParseError> {
    let token = ctx.cursor().peek().clone();
    let operation = match token.kind {
        TokenKind::LParen if at_join_type(ctx) => {
            return Ok((join_operation(ctx, None, scope, start)?, true));
        }
        TokenKind::LParen => {
            ctx.cursor().advance();
            let inner = operation(ctx, scope)?;
            ctx.cursor().expect(TokenKind::RParen)?;
            Operation::DynaFunc {
                name: SmolStr::new_static("group"),
                parameters: vec![inner],
                source_info: ctx.cursor().span_since(start),
            }
        }
        TokenKind::At => return Ok((join_operation(ctx, None, scope, start)?, true)),
        TokenKind::LBracket => {
            let database = database_pointer(ctx)?;
            if ctx.cursor().check(TokenKind::At) || at_join_type(ctx) {
                return Ok((join_operation(ctx, Some(database), scope, start)?, true));
            }
            if at_function(ctx) {
                function(ctx, &scope.with_database(Some(database)))?
            } else {
                column(ctx, Some(database), scope)?
            }
        }
        TokenKind::Dollar => {
            ctx.cursor().advance();
            Operation::LambdaParameter {
                name: identifier(ctx)?.value,
                source_info: start.clone(),
            }
        }
        TokenKind::LBrace if at_target(ctx) => column(ctx, None, scope)?,
        _ if at_function(ctx) => function(ctx, scope)?,
        TokenKind::StringLiteral if is_string(&token) => {
            ctx.cursor().advance();
            let (value, _) =
                string_value(&token.text).map_err(|message| ParseError::Unexpected {
                    message,
                    source_info: token.source_info.clone(),
                })?;
            Operation::Literal {
                value: LiteralValue::String(value),
                source_info: token.source_info,
            }
        }
        TokenKind::IntegerLiteral | TokenKind::FloatLiteral => number(ctx)?,
        TokenKind::Minus | TokenKind::Plus if at_signed_number(ctx) => number(ctx)?,
        _ if at_relational_identifier(ctx) => column(ctx, None, scope)?,
        _ => return Err(unexpected(&token)),
    };
    Ok((operation, false))
}

/// `(INNER) @Join`: a join sequence's type, rather than a group.
fn at_join_type(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    cursor.check(TokenKind::LParen)
        && (is_word_kind(cursor.peek_kind_at(1)) || is_string(cursor.peek_at(1)))
        && cursor.peek_kind_at(2) == TokenKind::RParen
        && cursor.peek_kind_at(3) == TokenKind::At
}

/// `name(`: a function.
fn at_function(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    (is_word_kind(cursor.peek_kind()) || is_string(cursor.peek()))
        && cursor.peek_kind_at(1) == TokenKind::LParen
}

/// `INTEGER` or `FLOAT`, their sign included.
fn number(ctx: &mut ParserContext<'_>) -> Result<Operation, ParseError> {
    let start = ctx.cursor().current_source_info();
    let signed = at_signed_number(ctx);
    let kind = ctx.cursor().peek_kind_at(usize::from(signed));
    if kind == TokenKind::IntegerLiteral {
        let (text, source_info) = integer(ctx)?;
        // The walker's `Integer.parseInt`.
        let value = text.parse::<i32>().map_err(|_| ParseError::Unexpected {
            message: format!("For input string: \"{text}\""),
            source_info: source_info.clone(),
        })?;
        return Ok(Operation::Literal {
            value: LiteralValue::Integer(value),
            source_info,
        });
    }
    let mut text = String::new();
    if signed {
        text.push_str(&ctx.cursor().advance().text);
    }
    text.push_str(&ctx.cursor().expect(TokenKind::FloatLiteral)?.text);
    let source_info = ctx.cursor().span_since(&start);
    // The walker's `Double.parseDouble`.
    let value = text
        .trim_end_matches(['f', 'F'])
        .parse::<f64>()
        .map_err(|_| ParseError::Unexpected {
            message: format!("For input string: \"{text}\""),
            source_info: source_info.clone(),
        })?;
    Ok(Operation::Literal {
        value: LiteralValue::Float(value),
        source_info,
    })
}

/// `name(arguments)`.
fn function(ctx: &mut ParserContext<'_>, scope: &Scope) -> Result<Operation, ParseError> {
    let start = ctx.cursor().current_source_info();
    let name = identifier(ctx)?.value;
    ctx.cursor().expect(TokenKind::LParen)?;
    let parameters = arguments(ctx, scope, TokenKind::RParen)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(Operation::DynaFunc {
        name,
        parameters,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// Function arguments, comma-separated, up to `close`.
fn arguments(
    ctx: &mut ParserContext<'_>,
    scope: &Scope,
    close: TokenKind,
) -> Result<Vec<Operation>, ParseError> {
    let mut arguments = Vec::new();
    if ctx.cursor().check(close) {
        return Ok(arguments);
    }
    loop {
        arguments.push(argument(ctx, scope)?);
        if !ctx.cursor().eat(TokenKind::Comma) {
            return Ok(arguments);
        }
    }
}

/// A function's argument: an operation, `[an, array]` or a lambda.
fn argument(ctx: &mut ParserContext<'_>, scope: &Scope) -> Result<Operation, ParseError> {
    let start = ctx.cursor().current_source_info();
    if ctx.cursor().check(TokenKind::LBracket) && !at_database_pointer_operand(ctx) {
        ctx.cursor().advance();
        let items = arguments(ctx, scope, TokenKind::RBracket)?;
        ctx.cursor().expect(TokenKind::RBracket)?;
        return Ok(Operation::LiteralList {
            items,
            source_info: ctx.cursor().span_since(&start),
        });
    }
    if let Some(parenthesized) = at_lambda(ctx) {
        return lambda(ctx, scope, parenthesized, &start);
    }
    operation(ctx, scope)
}

/// Whether `[a::b]` starts an operand in its database rather than an
/// array: an operand follows `]`.
fn at_database_pointer_operand(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    let mut i = 1;
    loop {
        let token = cursor.peek_at(i);
        if !(is_word_kind(token.kind) || is_string(token)) {
            return false;
        }
        i += 1;
        match cursor.peek_kind_at(i) {
            TokenKind::PathSep => i += 1,
            TokenKind::RBracket => break,
            _ => return false,
        }
    }
    let next = cursor.peek_at(i + 1);
    matches!(
        next.kind,
        TokenKind::At | TokenKind::LParen | TokenKind::LBrace | TokenKind::Dollar
    ) || is_word_kind(next.kind)
        || is_quoted_string(next)
}

/// `x | body` (`Some(false)`) or `(x, y | body)` (`Some(true)`).
fn at_lambda(ctx: &mut ParserContext<'_>) -> Option<bool> {
    let cursor = ctx.cursor();
    let name = |i: usize| is_word_kind(cursor.peek_kind_at(i)) || is_string(cursor.peek_at(i));
    if name(0) && cursor.peek_kind_at(1) == TokenKind::Pipe {
        return Some(false);
    }
    if !cursor.check(TokenKind::LParen) {
        return None;
    }
    let mut i = 1;
    loop {
        if !name(i) {
            return None;
        }
        match cursor.peek_kind_at(i + 1) {
            TokenKind::Comma => i += 2,
            TokenKind::Pipe => return Some(true),
            _ => return None,
        }
    }
}

/// A lambda, the engine's `RelationalLambda`.
fn lambda(
    ctx: &mut ParserContext<'_>,
    scope: &Scope,
    parenthesized: bool,
    start: &SourceInfo,
) -> Result<Operation, ParseError> {
    if parenthesized {
        ctx.cursor().expect(TokenKind::LParen)?;
    }
    let mut parameters = vec![identifier(ctx)?.value];
    while parenthesized && ctx.cursor().eat(TokenKind::Comma) {
        parameters.push(identifier(ctx)?.value);
    }
    ctx.cursor().expect(TokenKind::Pipe)?;
    let body = Box::new(operation(ctx, scope)?);
    if parenthesized {
        ctx.cursor().expect(TokenKind::RParen)?;
    }
    Ok(Operation::Lambda {
        parameters,
        body,
        source_info: ctx.cursor().span_since(start),
    })
}

/// A column: `{target}.column`, or up to `schema.table.column`, in the
/// pointer's database, else the scope's.
fn column(
    ctx: &mut ParserContext<'_>,
    database: Option<String>,
    scope: &Scope,
) -> Result<Operation, ParseError> {
    let database = database.or_else(|| scope.database.clone());
    Ok(Operation::Column(table_alias_column(ctx, database, scope)?))
}

/// `tableAliasColumnOperation`: `{target}.column`, or up to
/// `schema.table.column` whose missing parts the scope gives.
///
/// # Errors
///
/// A syntax error, or a column with no table (`Missing table or alias`).
pub fn table_alias_column(
    ctx: &mut ParserContext<'_>,
    database: Option<String>,
    scope: &Scope,
) -> Result<TableAliasColumn, ParseError> {
    if at_target(ctx) {
        let start = ctx.cursor().current_source_info();
        for _ in 0..3 {
            ctx.cursor().advance();
        }
        let target = Name {
            text: SmolStr::new_static("{target}"),
            value: SmolStr::new_static("{target}"),
            source_info: ctx.cursor().span_since(&start),
        };
        ctx.cursor().expect(TokenKind::Dot)?;
        let column = relational_identifier(ctx)?;
        // A fresh scope: only the database.
        let table = resolved_table(ctx, database, None, Some(target), &column);
        return Ok(TableAliasColumn {
            column: column.text,
            table,
            source_info: column.source_info,
        });
    }
    let first = relational_identifier(ctx)?;
    let (schema, table, column) = if ctx.cursor().eat(TokenKind::Dot) {
        let second = relational_identifier(ctx)?;
        if ctx.cursor().eat(TokenKind::Dot) {
            (Some(first), Some(second), relational_identifier(ctx)?)
        } else {
            (None, Some(first), second)
        }
    } else {
        (None, None, first)
    };
    let database = non_empty(database).or_else(|| scope.database.clone());
    let table = resolved_table(
        ctx,
        database,
        schema.or_else(|| scope.schema.clone()),
        table.or_else(|| scope.table.clone()),
        &column,
    );
    Ok(TableAliasColumn {
        column: column.text,
        table,
        source_info: column.source_info,
    })
}

/// A column's table, as [`table_ptr`] finds it. Without one, the walker's
/// error is deferred (see `ParserContext::defer`), and an unnamed table
/// stands in for it.
fn resolved_table(
    ctx: &mut ParserContext<'_>,
    database: Option<String>,
    schema: Option<Name>,
    table: Option<Name>,
    column: &Name,
) -> TablePtr {
    table_ptr(database.clone(), schema, table, Some(column)).unwrap_or_else(|error| {
        ctx.defer(error);
        TablePtr {
            database,
            schema: SmolStr::new_static("default"),
            table: SmolStr::default(),
            source_info: column.source_info.clone(),
        }
    })
}

/// The walker's `generateTableAlias`: a table named by `table`, else by
/// the schema (then without one), in `schema` or `default`. Without either,
/// an error at `column`.
pub(crate) fn table_ptr(
    database: Option<String>,
    schema: Option<Name>,
    table: Option<Name>,
    column: Option<&Name>,
) -> Result<TablePtr, ParseError> {
    let (schema, table) = match (schema, table) {
        (schema, Some(table)) => (schema, table),
        (Some(schema), None) => (None, schema),
        (None, None) => {
            let column = column.expect("a column or a scope names the table");
            return Err(ParseError::Unexpected {
                message: format!("Missing table or alias for column '{}'", column.text),
                source_info: column.source_info.clone(),
            });
        }
    };
    let source_info = schema.as_ref().map_or_else(
        || table.source_info.clone(),
        |schema| schema.source_info.merge(&table.source_info),
    );
    Ok(TablePtr {
        database,
        schema: schema.map_or_else(|| SmolStr::new_static("default"), |schema| schema.text),
        table: table.text,
        source_info,
    })
}

fn non_empty(database: Option<String>) -> Option<String> {
    database.filter(|database| !database.is_empty())
}

/// `[db]? joinSequence ('|' booleanOperation)?`, from `start`.
fn join_operation(
    ctx: &mut ParserContext<'_>,
    database: Option<String>,
    scope: &Scope,
    start: &SourceInfo,
) -> Result<Operation, ParseError> {
    let database = database.or_else(|| scope.database.clone());
    let joins = join_sequence(ctx, database.clone(), scope)?;
    // The element is in the last join's database.
    let database = joins
        .last()
        .and_then(|join| join.database.clone())
        .or(database);
    let element = if ctx.cursor().eat(TokenKind::Pipe) {
        Some(Box::new(operation(ctx, &scope.with_database(database))?))
    } else {
        None
    };
    Ok(Operation::ElementWithJoins {
        joins,
        element,
        source_info: ctx.cursor().span_since(start),
    })
}

/// The join types a join sequence may name.
const JOIN_TYPES: [&str; 2] = ["INNER", "OUTER"];

/// `(INNER)? @J1 > (OUTER)? [db]? @J2 …`, in `database` unless a pointer
/// names another.
///
/// # Errors
///
/// A syntax error, or a join type other than `INNER` and `OUTER`.
pub fn join_sequence(
    ctx: &mut ParserContext<'_>,
    database: Option<String>,
    scope: &Scope,
) -> Result<Vec<JoinPointer>, ParseError> {
    // The first join's type is kept as written, the others' unquoted.
    let first_type = join_type(ctx, true)?.map(|name| name.text);
    let mut joins = vec![join_pointer(ctx, first_type, database.clone(), scope)?];
    while at_next_join(ctx) {
        ctx.cursor().advance();
        let join_type = join_type(ctx, false)?.map(|name| name.value);
        let database = if ctx.cursor().check(TokenKind::LBracket) {
            Some(database_pointer(ctx)?)
        } else {
            database.clone()
        };
        joins.push(join_pointer(ctx, join_type, database, scope)?);
    }
    Ok(joins)
}

/// `> (TYPE)? [db]? @Join`: the sequence goes on, rather than a comparison.
fn at_next_join(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    if !cursor.check(TokenKind::Greater) {
        return false;
    }
    let name = |i: usize| is_word_kind(cursor.peek_kind_at(i)) || is_string(cursor.peek_at(i));
    let mut i = 1;
    if cursor.peek_kind_at(i) == TokenKind::LParen {
        if !(name(i + 1) && cursor.peek_kind_at(i + 2) == TokenKind::RParen) {
            return false;
        }
        i += 3;
    }
    if cursor.peek_kind_at(i) == TokenKind::LBracket {
        i += 1;
        loop {
            if !name(i) {
                return false;
            }
            i += 1;
            match cursor.peek_kind_at(i) {
                TokenKind::PathSep => i += 1,
                TokenKind::RBracket => {
                    i += 1;
                    break;
                }
                _ => return false,
            }
        }
    }
    cursor.peek_kind_at(i) == TokenKind::At
}

/// `(INNER)`, validated.
fn join_type(ctx: &mut ParserContext<'_>, first: bool) -> Result<Option<Name>, ParseError> {
    if !ctx.cursor().eat(TokenKind::LParen) {
        return Ok(None);
    }
    let name = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    if !JOIN_TYPES.contains(&name.value.as_str()) {
        let supported = if first {
            format!(
                ". The supported join types are: [{}]",
                JOIN_TYPES.join(", ")
            )
        } else {
            String::new()
        };
        // The walker's check.
        ctx.defer(ParseError::Unexpected {
            message: format!("Unsupported join type '{}'{supported}", name.text),
            source_info: name.source_info.clone(),
        });
    }
    Ok(Some(name))
}

/// `@Name`, in `database`, else the scope's.
fn join_pointer(
    ctx: &mut ParserContext<'_>,
    join_type: Option<SmolStr>,
    database: Option<String>,
    scope: &Scope,
) -> Result<JoinPointer, ParseError> {
    let start = ctx.cursor().current_source_info();
    ctx.cursor().expect(TokenKind::At)?;
    let name = identifier(ctx)?.value;
    Ok(JoinPointer {
        database: non_empty(database).or_else(|| non_empty(scope.database.clone())),
        join_type,
        name,
        source_info: ctx.cursor().span_since(&start),
    })
}
