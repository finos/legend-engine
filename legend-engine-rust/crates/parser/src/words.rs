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

//! Names as the engine's section grammars read them, from Pure tokens.
//!
//! The engine's section lexers (`CoreLexerGrammar` and the grammars built on
//! it) read an identifier as a word (`VALID_STRING`, or one of the grammar's
//! keywords, which its `identifier` rule lists) or a `'single-quoted'`
//! string (`STRING`): here, any Pure word or keyword but `true` and `false`,
//! and a single-quoted string literal. Their walkers read a name as written
//! (`getText()`, quotes included) or unquoted (`fromIdentifier`), so a
//! [`Name`] carries both.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::{Token, TokenKind};
use smol_str::SmolStr;

use crate::ParserContext;
use crate::error::ParseError;
use crate::string_value;

/// A name: as written, and unquoted.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Name {
    /// As written (`getText()`).
    pub text: SmolStr,
    /// A `'quoted'` name unquoted; otherwise as written (`fromIdentifier`).
    pub value: SmolStr,
    /// The name.
    pub source_info: SourceInfo,
}

/// A qualified name, `a::b::C`: the grammar's `qualifiedName`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct QualifiedName {
    /// Its parts, quoted ones unquoted, joined by `::` (`fromQualifiedName`).
    pub path: String,
    /// As written, without spaces (`getText()`).
    pub text: String,
    /// Whether it has a package: more than one part.
    pub has_package: bool,
    /// From the first part to the last.
    pub source_info: SourceInfo,
}

/// A syntax error at `token`.
#[must_use]
pub fn unexpected(token: &Token) -> ParseError {
    let found = if token.kind == TokenKind::Eof {
        "end of input".to_string()
    } else {
        format!("'{}'", token.text)
    };
    ParseError::Unexpected {
        message: format!("Unexpected token {found}"),
        source_info: token.source_info.clone(),
    }
}

/// A `VALID_STRING` or keyword: an identifier-like token other than a
/// string or a boolean.
#[must_use]
pub fn is_word_kind(kind: TokenKind) -> bool {
    kind.is_identifier_like()
        && !matches!(
            kind,
            TokenKind::StringLiteral | TokenKind::True | TokenKind::False
        )
}

/// `word`, as a keyword.
#[must_use]
pub fn is_word(token: &Token, word: &str) -> bool {
    is_word_kind(token.kind) && token.text == word
}

/// A `'single-quoted'` string: the grammars' `STRING`.
#[must_use]
pub fn is_string(token: &Token) -> bool {
    token.kind == TokenKind::StringLiteral && token.text.starts_with('\'')
}

/// Whether `next` starts right after `previous`, on its line.
#[must_use]
pub fn adjacent(previous: &Token, next: &Token) -> bool {
    same_line_gap(previous, next) == Some(0)
}

/// The columns between `previous`'s end and `next`'s start, when both are
/// on one line.
#[must_use]
pub fn same_line_gap(previous: &Token, next: &Token) -> Option<u32> {
    let (a, b) = (&previous.source_info, &next.source_info);
    (a.end_line == b.start_line && b.start_column > a.end_column)
        .then(|| b.start_column - a.end_column - 1)
}

/// Whether `word`, the keyword, is next.
pub fn at_word(ctx: &mut ParserContext<'_>, word: &str) -> bool {
    is_word(ctx.cursor().peek(), word)
}

/// Consumes `word`, the keyword.
///
/// # Errors
///
/// Anything else next.
pub fn expect_word(ctx: &mut ParserContext<'_>, word: &str) -> Result<Token, ParseError> {
    let token = ctx.cursor().peek().clone();
    if !is_word(&token, word) {
        return Err(unexpected(&token));
    }
    ctx.cursor().advance();
    Ok(token)
}

/// Whether the command `~name` (`~src`, `~filter`, …) is next: the engine's
/// lexers read it as one token, so the name is written right after `~`.
pub fn at_command(ctx: &mut ParserContext<'_>, name: &str) -> bool {
    let cursor = ctx.cursor();
    cursor.check(TokenKind::Tilde)
        && is_word_kind(cursor.peek_kind_at(1))
        && cursor.peek_at(1).text == name
        && adjacent(cursor.peek(), cursor.peek_at(1))
}

/// Consumes the command `~name`.
pub fn eat_command(ctx: &mut ParserContext<'_>, name: &str) -> bool {
    if !at_command(ctx, name) {
        return false;
    }
    ctx.cursor().advance();
    ctx.cursor().advance();
    true
}

/// Whether an identifier is next.
pub fn at_identifier(ctx: &mut ParserContext<'_>) -> bool {
    let token = ctx.cursor().peek();
    is_word_kind(token.kind) || is_string(token)
}

/// The grammars' `identifier`: a word or a `'quoted'` name.
///
/// # Errors
///
/// Anything else next, or a quoted name with a malformed escape.
pub fn identifier(ctx: &mut ParserContext<'_>) -> Result<Name, ParseError> {
    let token = ctx.cursor().peek().clone();
    if is_word_kind(token.kind) {
        ctx.cursor().advance();
        return Ok(Name {
            text: token.text.clone(),
            value: token.text,
            source_info: token.source_info,
        });
    }
    if is_string(&token) {
        ctx.cursor().advance();
        let (value, _) = string_value(&token.text).map_err(|message| ParseError::Unexpected {
            message,
            source_info: token.source_info.clone(),
        })?;
        return Ok(Name {
            text: token.text,
            value: SmolStr::new(value),
            source_info: token.source_info,
        });
    }
    Err(unexpected(&token))
}

/// The grammars' `word`: an identifier, a boolean or an integer, which the
/// walkers read as written.
///
/// # Errors
///
/// As for [`identifier`].
pub fn word(ctx: &mut ParserContext<'_>) -> Result<Name, ParseError> {
    let token = ctx.cursor().peek().clone();
    if matches!(
        token.kind,
        TokenKind::True | TokenKind::False | TokenKind::IntegerLiteral
    ) {
        ctx.cursor().advance();
        return Ok(Name {
            text: token.text.clone(),
            value: token.text,
            source_info: token.source_info,
        });
    }
    identifier(ctx)
}

/// The grammars' `qualifiedName`: identifiers separated by `::`.
///
/// # Errors
///
/// As for [`identifier`].
pub fn qualified_name(ctx: &mut ParserContext<'_>) -> Result<QualifiedName, ParseError> {
    let first = identifier(ctx)?;
    let mut path = first.value.to_string();
    let mut text = first.text.to_string();
    let mut has_package = false;
    while ctx.cursor().check(TokenKind::PathSep) {
        ctx.cursor().advance();
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
