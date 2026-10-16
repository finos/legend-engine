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

//! A collection's JSON schema, as the grammar's `json` rule reads it from
//! the section's tokens (`"…"` strings, numbers, `true`, `false`, `null`,
//! objects and arrays), into the tree the engine's deserializer walks: a
//! Jackson `JsonNode`, whose objects keep their keys in the order written,
//! a key written again keeping its place and taking the later value.

use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{adjacent, is_word, unexpected};

type R<T> = Result<T, ParseError>;

/// A JSON value.
#[derive(Debug, Clone, PartialEq)]
pub(crate) enum Json {
    String(String),
    /// A number, as written.
    Number(String),
    Bool(bool),
    Null,
    Object(Vec<(String, Json)>),
    Array(Vec<Json>),
}

impl Json {
    /// Jackson's `get`: an object's value of `key`.
    pub(crate) fn get(&self, key: &str) -> Option<&Json> {
        match self {
            Self::Object(fields) => fields
                .iter()
                .find(|(name, _)| name == key)
                .map(|(_, value)| value),
            _ => None,
        }
    }

    /// Jackson's `textValue`: a string's text.
    pub(crate) fn text(&self) -> Option<&str> {
        match self {
            Self::String(text) => Some(text),
            _ => None,
        }
    }

    /// Jackson's `longValue`: a number, its fraction cut off; 0 for
    /// anything else.
    pub(crate) fn long(&self) -> i64 {
        match self {
            Self::Number(text) => text.parse::<i64>().unwrap_or_else(|_| {
                // Saturating, as Java's cast of a double is.
                #[allow(clippy::cast_possible_truncation)]
                let value = text.parse::<f64>().map(|value| value as i64);
                value.unwrap_or_default()
            }),
            _ => 0,
        }
    }

    /// Jackson's `doubleValue`: a number; 0 for anything else.
    pub(crate) fn double(&self) -> f64 {
        match self {
            Self::Number(text) => text.parse().unwrap_or_default(),
            _ => 0.0,
        }
    }

    /// Jackson's `booleanValue`: a boolean; false for anything else.
    pub(crate) fn boolean(&self) -> bool {
        matches!(self, Self::Bool(true))
    }
}

/// A `"…"` string's text, unescaped as JSON.
fn quoted(token: &Token) -> R<String> {
    if token.kind != TokenKind::StringLiteral || !token.text.starts_with('"') {
        return Err(unexpected(token));
    }
    serde_json::from_str(&token.text).map_err(|error| ParseError::Unexpected {
        message: format!("Invalid JSON string: {error}"),
        source_info: token.source_info.clone(),
    })
}

/// A JSON value.
pub(crate) fn value(ctx: &mut ParserContext<'_>) -> R<Json> {
    let token = ctx.cursor().peek().clone();
    match token.kind {
        TokenKind::StringLiteral => {
            let text = quoted(&token)?;
            ctx.cursor().advance();
            Ok(Json::String(text))
        }
        TokenKind::LBrace => object(ctx),
        TokenKind::LBracket => array(ctx),
        TokenKind::True | TokenKind::False => {
            ctx.cursor().advance();
            Ok(Json::Bool(token.kind == TokenKind::True))
        }
        _ if is_word(&token, "null") => {
            ctx.cursor().advance();
            Ok(Json::Null)
        }
        _ => number(ctx),
    }
}

/// `-? digits (. digits)? exponent?`, its sign written with it.
fn number(ctx: &mut ParserContext<'_>) -> R<Json> {
    let cursor = ctx.cursor();
    let first = cursor.peek().clone();
    let signed = first.kind == TokenKind::Minus;
    let digits = cursor.peek_at(usize::from(signed)).clone();
    if !matches!(
        digits.kind,
        TokenKind::IntegerLiteral | TokenKind::FloatLiteral
    ) || (signed && !adjacent(&first, &digits))
    {
        return Err(unexpected(&first));
    }
    if signed {
        cursor.advance();
    }
    cursor.advance();
    let sign = if signed { "-" } else { "" };
    Ok(Json::Number(format!("{sign}{}", digits.text)))
}

/// `{ "key": value, … }`, possibly empty.
fn object(ctx: &mut ParserContext<'_>) -> R<Json> {
    ctx.cursor().expect(TokenKind::LBrace)?;
    let mut fields: Vec<(String, Json)> = Vec::new();
    if !ctx.cursor().eat(TokenKind::RBrace) {
        loop {
            let key = ctx.cursor().peek().clone();
            let key = quoted(&key)?;
            ctx.cursor().advance();
            ctx.cursor().expect(TokenKind::Colon)?;
            let value = value(ctx)?;
            match fields.iter_mut().find(|(name, _)| *name == key) {
                Some(field) => field.1 = value,
                None => fields.push((key, value)),
            }
            if !ctx.cursor().eat(TokenKind::Comma) {
                break;
            }
        }
        ctx.cursor().expect(TokenKind::RBrace)?;
    }
    Ok(Json::Object(fields))
}

/// `[ value, … ]`, possibly empty.
fn array(ctx: &mut ParserContext<'_>) -> R<Json> {
    ctx.cursor().expect(TokenKind::LBracket)?;
    let mut values = Vec::new();
    if !ctx.cursor().eat(TokenKind::RBracket) {
        loop {
            values.push(value(ctx)?);
            if !ctx.cursor().eat(TokenKind::Comma) {
                break;
            }
        }
        ctx.cursor().expect(TokenKind::RBracket)?;
    }
    Ok(Json::Array(values))
}
