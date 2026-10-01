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

//! The forms the engine's section grammars repeat: a field, `name: value;`;
//! a list, `[ item, … ]`; a string, `'text'`; and what their walkers read as
//! written, without spaces (`getText()`): a type, a function's identifier.

use legend_pure_parser_lexer::{Token, TokenKind};

use crate::ParserContext;
use crate::error::ParseError;
use crate::string_value;
use crate::words::{adjacent, is_string, is_word_kind, qualified_name, unexpected};

type R<T> = Result<T, ParseError>;

/// A field's name, whatever it is, and its `:`; the name's token.
///
/// # Errors
///
/// No `:` after the name.
pub fn field_start(ctx: &mut ParserContext<'_>) -> R<Token> {
    let name = ctx.cursor().advance().clone();
    ctx.cursor().expect(TokenKind::Colon)?;
    Ok(name)
}

/// `name: 'text';`'s string.
///
/// # Errors
///
/// Anything but `: 'text';` after the name.
pub fn string_field(ctx: &mut ParserContext<'_>) -> R<Token> {
    field_start(ctx)?;
    let value = string(ctx)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok(value)
}

/// A `'single-quoted'` string: the grammars' `STRING`.
///
/// # Errors
///
/// Anything else next.
pub fn string(ctx: &mut ParserContext<'_>) -> R<Token> {
    let token = ctx.cursor().peek().clone();
    if !is_string(&token) {
        return Err(unexpected(&token));
    }
    ctx.cursor().advance();
    Ok(token)
}

/// A string's value, unescaped, as the walkers read one
/// (`fromGrammarString`).
///
/// # Errors
///
/// A malformed escape, on the string.
pub fn string_text(token: &Token) -> R<String> {
    string_value(&token.text)
        .map(|(value, _)| value)
        .map_err(|message| ParseError::Unexpected {
            message,
            source_info: token.source_info.clone(),
        })
}

/// `[ item, … ]`, possibly empty.
///
/// # Errors
///
/// No brackets, or `item`'s error.
pub fn list<T>(
    ctx: &mut ParserContext<'_>,
    item: impl FnMut(&mut ParserContext<'_>) -> R<T>,
) -> R<Vec<T>> {
    ctx.cursor().expect(TokenKind::LBracket)?;
    let items = if ctx.cursor().check(TokenKind::RBracket) {
        Vec::new()
    } else {
        separated(ctx, item)?
    };
    ctx.cursor().expect(TokenKind::RBracket)?;
    Ok(items)
}

/// `item, …`: at least one.
///
/// # Errors
///
/// `item`'s error.
pub fn separated<T>(
    ctx: &mut ParserContext<'_>,
    mut item: impl FnMut(&mut ParserContext<'_>) -> R<T>,
) -> R<Vec<T>> {
    let mut items = vec![item(ctx)?];
    while ctx.cursor().eat(TokenKind::Comma) {
        items.push(item(ctx)?);
    }
    Ok(items)
}

/// A type as the walker reads it, `getText()`: its tokens without spaces.
/// A qualified name, with its unit (`~Unit`), type arguments (`<…>`) and
/// type variable values (`(…)`), or a function or relation type (`{…}`,
/// `(…)`).
///
/// # Errors
///
/// A malformed type.
pub fn type_text(ctx: &mut ParserContext<'_>) -> Result<String, ParseError> {
    let mut text = String::new();
    if matches!(
        ctx.cursor().peek_kind(),
        TokenKind::LBrace | TokenKind::LParen
    ) {
        balanced(ctx, &mut text)?;
        return Ok(text);
    }
    text.push_str(&qualified_name(ctx)?.text);
    // A unit, `Mass~Kilogram`.
    let cursor = ctx.cursor();
    if cursor.check(TokenKind::Tilde)
        && is_word_kind(cursor.peek_kind_at(1))
        && adjacent(cursor.peek(), cursor.peek_at(1))
    {
        text.push_str(&cursor.advance().text);
        text.push_str(&cursor.advance().text);
    }
    if ctx.cursor().check(TokenKind::Less) {
        balanced(ctx, &mut text)?;
    }
    if ctx.cursor().check(TokenKind::LParen) {
        balanced(ctx, &mut text)?;
    }
    Ok(text)
}

/// An opening bracket, parenthesis, brace or `<` and what it holds, up to
/// its closing one, appended to `text` as written.
///
/// # Errors
///
/// The input ends before the closing one.
pub fn balanced(ctx: &mut ParserContext<'_>, text: &mut String) -> Result<(), ParseError> {
    let mut depth = 0_usize;
    loop {
        let token = ctx.cursor().peek().clone();
        let (opens, closes) = match token.kind {
            TokenKind::LBrace | TokenKind::LParen | TokenKind::LBracket | TokenKind::Less => (1, 0),
            TokenKind::RBrace | TokenKind::RParen | TokenKind::RBracket | TokenKind::Greater => {
                (0, 1)
            }
            TokenKind::GreaterGreater => (0, 2),
            TokenKind::GreaterGreaterGreater => (0, 3),
            TokenKind::Eof => return Err(unexpected(&token)),
            _ => (0, 0),
        };
        text.push_str(&token.text);
        ctx.cursor().advance();
        depth = (depth + opens).saturating_sub(closes);
        if depth == 0 {
            return Ok(());
        }
    }
}

/// `my::f(Type[m], …):Type[m]`, as written without spaces (the grammar's
/// `functionIdentifier`).
///
/// # Errors
///
/// A malformed identifier.
pub fn function_identifier(ctx: &mut ParserContext<'_>) -> Result<String, ParseError> {
    let mut text = qualified_name(ctx)?.text;
    if !ctx.cursor().check(TokenKind::LParen) {
        return Err(unexpected(ctx.cursor().peek()));
    }
    balanced(ctx, &mut text)?;
    let colon = ctx.cursor().expect(TokenKind::Colon)?;
    text.push_str(&colon.text);
    text.push_str(&type_text(ctx)?);
    if !ctx.cursor().check(TokenKind::LBracket) {
        return Err(unexpected(ctx.cursor().peek()));
    }
    balanced(ctx, &mut text)?;
    Ok(text)
}
