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
//! a list, `[ item, … ]`; a string, `'text'`.

use legend_pure_parser_lexer::{Token, TokenKind};

use crate::ParserContext;
use crate::error::ParseError;
use crate::string_value;
use crate::words::{is_string, unexpected};

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
