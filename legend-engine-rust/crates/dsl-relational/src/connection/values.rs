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

//! The values of a relational connection's fields, `name: value;`, as its
//! grammars read them.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{is_string, is_word, unexpected};
use legend_pure_parser_parser::{ParserContext, string_value};

type R<T> = Result<T, ParseError>;

/// Whether the field `name` is next.
pub(crate) fn at_field(ctx: &mut ParserContext<'_>, name: &str) -> bool {
    is_word(ctx.cursor().peek(), name)
}

/// `name: value;`, the name next: the value, and the field's span.
pub(crate) fn field<T>(
    ctx: &mut ParserContext<'_>,
    value: impl FnOnce(&mut ParserContext<'_>) -> R<T>,
) -> R<(T, SourceInfo)> {
    let start = ctx.cursor().current_source_info();
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::Colon)?;
    let value = value(ctx)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok((value, ctx.cursor().span_since(&start)))
}

/// `'text'`, unescaped.
pub(crate) fn string(ctx: &mut ParserContext<'_>) -> R<String> {
    let token = ctx.cursor().peek().clone();
    if !is_string(&token) {
        return Err(unexpected(&token));
    }
    ctx.cursor().advance();
    string_value(&token.text)
        .map(|(value, _)| value)
        .map_err(|message| ParseError::Unexpected {
            message,
            source_info: token.source_info,
        })
}

/// `[ 'a', 'b' ]`, unescaped.
pub(crate) fn strings(ctx: &mut ParserContext<'_>) -> R<Vec<String>> {
    ctx.cursor().expect(TokenKind::LBracket)?;
    let mut values = Vec::new();
    if !ctx.cursor().check(TokenKind::RBracket) {
        values.push(string(ctx)?);
        while ctx.cursor().eat(TokenKind::Comma) {
            values.push(string(ctx)?);
        }
    }
    ctx.cursor().expect(TokenKind::RBracket)?;
    Ok(values)
}

/// An integer, as the walker's `Integer.parseInt` reads it: one too large
/// for it is the engine's to fail on, outside its parser.
pub(crate) fn integer(ctx: &mut ParserContext<'_>) -> R<i32> {
    let token = ctx.cursor().peek().clone();
    if token.kind != TokenKind::IntegerLiteral {
        return Err(unexpected(&token));
    }
    ctx.cursor().advance();
    token.text.parse().map_err(|_| ParseError::Unsupported {
        message: format!(
            "{} is too large an integer, which the engine reports",
            token.text
        ),
        source_info: token.source_info,
    })
}

/// `true` or `false`.
pub(crate) fn boolean(ctx: &mut ParserContext<'_>) -> R<bool> {
    let token = ctx.cursor().peek().clone();
    let value = match token.kind {
        TokenKind::True => true,
        TokenKind::False => false,
        _ => return Err(unexpected(&token)),
    };
    ctx.cursor().advance();
    Ok(value)
}
