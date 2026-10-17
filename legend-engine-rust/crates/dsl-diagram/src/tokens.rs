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

//! The diagram lexer's tokens, rebuilt from Pure tokens.
//!
//! `DiagramLexerGrammar` reads a run of `[A-Za-z0-9_-]` as one token,
//! `VALID_DIAGRAM_VIEW_ID`, unless one of its earlier rules reads as much
//! there (a number, a boolean, a keyword, a `VALID_STRING`), and a number's
//! sign with the number. The Pure lexer splits such a run
//! (`4cec85f9-9b66` is `4`, `cec85f9`, `-`, `9`, `b66`): here it is joined
//! again from the tokens written together.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{
    self, Name, QualifiedName, adjacent, is_string, unexpected,
};

type R<T> = Result<T, ParseError>;

fn is_view_id_char(c: char) -> bool {
    c.is_ascii_alphanumeric() || c == '_' || c == '-'
}

/// The run of view id characters next, written together: its tokens'
/// count, and its text.
fn run(ctx: &mut ParserContext<'_>) -> (usize, String) {
    let cursor = ctx.cursor();
    let mut count = 0;
    let mut text = String::new();
    loop {
        let token = cursor.peek_at(count);
        if token.text.is_empty()
            || !token.text.chars().all(is_view_id_char)
            || (count > 0 && !adjacent(cursor.peek_at(count - 1), token))
        {
            return (count, text);
        }
        text.push_str(&token.text);
        count += 1;
    }
}

/// `[+-]?[0-9]+`: an `INTEGER`.
fn is_integer(text: &str) -> bool {
    let digits = text.strip_prefix('-').unwrap_or(text);
    !digits.is_empty() && digits.chars().all(|c| c.is_ascii_digit())
}

/// A view's id, as written: a quoted name, or a run of view id characters
/// that the engine's lexer doesn't read as a number or a boolean.
pub(crate) fn view_id(ctx: &mut ParserContext<'_>) -> R<(String, SourceInfo)> {
    let first = ctx.cursor().peek().clone();
    if is_string(&first) {
        ctx.cursor().advance();
        return Ok((first.text.to_string(), first.source_info));
    }
    let (count, text) = run(ctx);
    if count == 0 {
        // A `VALID_STRING` with a `$`, which no view id has.
        let name = words::identifier(ctx)?;
        return Ok((name.text.to_string(), name.source_info));
    }
    if is_integer(&text) || text == "true" || text == "false" {
        return Err(unexpected(&first));
    }
    for _ in 0..count {
        ctx.cursor().advance();
    }
    Ok((text, ctx.cursor().span_since(&first.source_info)))
}

/// The grammar's `identifier`; one followed by view id characters, written
/// together, is a view id to the engine's lexer.
pub(crate) fn identifier(ctx: &mut ParserContext<'_>) -> R<Name> {
    let first = ctx.cursor().peek().clone();
    if run(ctx).0 > 1 {
        return Err(unexpected(&first));
    }
    words::identifier(ctx)
}

/// The grammar's `qualifiedName`, of [`identifier`]s.
pub(crate) fn qualified_name(ctx: &mut ParserContext<'_>) -> R<QualifiedName> {
    let first = identifier(ctx)?;
    let mut path = first.value.to_string();
    let mut text = first.text.to_string();
    let mut has_package = false;
    while ctx.cursor().eat(TokenKind::PathSep) {
        let part = identifier(ctx)?;
        path.push_str("::");
        path.push_str(&part.value);
        text.push_str("::");
        text.push_str(&part.text);
        has_package = true;
    }
    Ok(QualifiedName {
        path,
        text,
        has_package,
        source_info: ctx.cursor().span_since(&first.source_info),
    })
}

/// A number, `FLOAT` or `INTEGER`, its sign written with it, as written: a
/// float has a point, before which its digits are optional (`.5`); an
/// integer followed by view id characters is a view id to the engine's
/// lexer.
pub(crate) fn number(ctx: &mut ParserContext<'_>) -> R<(String, SourceInfo)> {
    let cursor = ctx.cursor();
    let first = cursor.peek().clone();
    // The sign, and a float's leading point, which the Pure lexer reads
    // apart from the digits.
    let mut count = usize::from(matches!(first.kind, TokenKind::Plus | TokenKind::Minus));
    let point = cursor.peek_kind_at(count) == TokenKind::Dot;
    count += usize::from(point);
    let number = cursor.peek_at(count).clone();
    let well_formed = match number.kind {
        TokenKind::IntegerLiteral => {
            let next = cursor.peek_at(count + 1);
            !(adjacent(&number, next) && next.text.starts_with(is_view_id_char))
        }
        TokenKind::FloatLiteral => number.text.contains('.') != point,
        _ => false,
    } && (1..=count)
        .all(|offset| adjacent(cursor.peek_at(offset - 1), cursor.peek_at(offset)));
    if !well_formed {
        return Err(unexpected(&first));
    }
    let mut text = String::new();
    for _ in 0..=count {
        text.push_str(&cursor.advance().text);
    }
    Ok((text, cursor.span_since(&first.source_info)))
}
