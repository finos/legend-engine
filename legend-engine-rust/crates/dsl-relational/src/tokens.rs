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

//! The engine's relational lexer, read from the Pure lexer's tokens.
//!
//! The relational lexer has tokens of its own that the Pure lexer splits:
//! `PRIMARY KEY`, `NOT NULL`, `is null` and `is not null` (words one space
//! apart), `~filter` and the other commands, `{target}`, `<>`, and signed
//! numbers (`-1` is one `INTEGER`). Each is read here as its Pure tokens,
//! written next to each other as the relational token requires. Its names
//! are `VALID_STRING`s and keywords (Pure's keywords are names there, but
//! not `true` and `false`), `'single-quoted'` names (`STRING`) and
//! `"double-quoted"` ones (`QUOTED_STRING`), both of which the Pure lexer
//! reads as string literals.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
pub use legend_pure_parser_parser::words::Name;
pub(crate) use legend_pure_parser_parser::words::{
    adjacent, at_command, at_word, eat_command, expect_word, identifier, is_string, is_word,
    is_word_kind, same_line_gap, unexpected,
};

/// A `"double-quoted"` string: the grammar's `QUOTED_STRING`.
pub(crate) fn is_quoted_string(token: &Token) -> bool {
    token.kind == TokenKind::StringLiteral && token.text.starts_with('"')
}

/// Whether the next tokens are `words`, one space apart on one line: the
/// relational lexer's `PRIMARY KEY`, `NOT NULL`, `is null`, `is not null`.
pub(crate) fn at_words(ctx: &mut ParserContext<'_>, words: &[&str]) -> bool {
    let cursor = ctx.cursor();
    words.iter().enumerate().all(|(i, word)| {
        is_word(cursor.peek_at(i), word)
            && (i == 0 || same_line_gap(cursor.peek_at(i - 1), cursor.peek_at(i)) == Some(1))
    })
}

/// Consumes [`at_words`]' `words`, returning their span.
pub(crate) fn eat_words(ctx: &mut ParserContext<'_>, words: &[&str]) -> Option<SourceInfo> {
    if !at_words(ctx, words) {
        return None;
    }
    let start = ctx.cursor().current_source_info();
    for _ in words {
        ctx.cursor().advance();
    }
    Some(ctx.cursor().span_since(&start))
}

/// Whether `{target}` is next.
pub(crate) fn at_target(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    cursor.check(TokenKind::LBrace)
        && is_word(cursor.peek_at(1), "target")
        && cursor.peek_kind_at(2) == TokenKind::RBrace
        && adjacent(cursor.peek(), cursor.peek_at(1))
        && adjacent(cursor.peek_at(1), cursor.peek_at(2))
}

/// The grammar's `relationalIdentifier`: a word or a `"quoted"` name, as
/// written.
pub(crate) fn relational_identifier(ctx: &mut ParserContext<'_>) -> Result<Name, ParseError> {
    let token = ctx.cursor().peek().clone();
    if is_word_kind(token.kind) || is_quoted_string(&token) {
        ctx.cursor().advance();
        return Ok(Name {
            text: token.text.clone(),
            value: token.text,
            source_info: token.source_info,
        });
    }
    Err(unexpected(&token))
}

/// Whether a `relationalIdentifier` is next.
pub(crate) fn at_relational_identifier(ctx: &mut ParserContext<'_>) -> bool {
    let token = ctx.cursor().peek();
    is_word_kind(token.kind) || is_quoted_string(token)
}

/// The grammar's `schemaIdentifier`: any name, as written.
pub(crate) fn schema_identifier(ctx: &mut ParserContext<'_>) -> Result<Name, ParseError> {
    if is_quoted_string(ctx.cursor().peek()) {
        return relational_identifier(ctx);
    }
    let token = ctx.cursor().peek().clone();
    let name = identifier(ctx)?;
    Ok(Name {
        value: token.text.clone(),
        ..name
    })
}

/// A qualified name's path, its quoted parts unquoted: `a::b::'C D'`.
pub(crate) fn qualified_name(ctx: &mut ParserContext<'_>) -> Result<String, ParseError> {
    Ok(legend_pure_parser_parser::words::qualified_name(ctx)?.path)
}

/// `[my::Db]`: a database pointer's path.
pub(crate) fn database_pointer(ctx: &mut ParserContext<'_>) -> Result<String, ParseError> {
    ctx.cursor().expect(TokenKind::LBracket)?;
    let path = qualified_name(ctx)?;
    ctx.cursor().expect(TokenKind::RBracket)?;
    Ok(path)
}

/// A documentation literal: a `'…'` string (`STRING`) before a database,
/// schema, table, view or column.
pub(crate) fn documentation(ctx: &mut ParserContext<'_>) -> Option<Token> {
    is_string(ctx.cursor().peek()).then(|| ctx.cursor().advance().clone())
}

/// A signed number's sign, when it is written right before the number it
/// belongs to: the relational lexer's `INTEGER` and `FLOAT` include it.
pub(crate) fn at_signed_number(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    matches!(cursor.peek_kind(), TokenKind::Minus | TokenKind::Plus)
        && matches!(
            cursor.peek_kind_at(1),
            TokenKind::IntegerLiteral | TokenKind::FloatLiteral
        )
        && adjacent(cursor.peek(), cursor.peek_at(1))
}

/// An `INTEGER`, sign included, as written.
pub(crate) fn integer(ctx: &mut ParserContext<'_>) -> Result<(String, SourceInfo), ParseError> {
    let start = ctx.cursor().current_source_info();
    let mut text = String::new();
    if at_signed_number(ctx) && ctx.cursor().peek_kind_at(1) == TokenKind::IntegerLiteral {
        text.push_str(&ctx.cursor().advance().text);
    }
    let token = ctx.cursor().expect(TokenKind::IntegerLiteral)?;
    text.push_str(&token.text);
    Ok((text, ctx.cursor().span_since(&start)))
}
