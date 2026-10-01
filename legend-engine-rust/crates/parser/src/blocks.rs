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

//! Blocks of fields, as many of the engine's section grammars read them
//! before their walkers check them.
//!
//! Their rules are blocks, `Keyword { field … }` (some without a keyword),
//! whose fields, `name: value`, come in any order and as often as written:
//! a [`ReadBlock`] keeps them as read, named as the walker names them, and
//! the walk takes each one out ([`ReadBlock::take`] and
//! [`ReadBlock::take_optional`], which check them as the walker's
//! `validateAndExtract…Field` do). A value is a string, a name, a path, a
//! navigation path, a block, a list, an island, …: what the grammar's rule
//! for the field reads. Each rule's reader checks the syntax, so a section's
//! syntax errors come before its walker's errors; parts the walker parses
//! with another grammar (an island, embedded data, a test assertion) are
//! read but not parsed.
//!
//! A grammar describes each block by a field reader ([`Fields`]): given the
//! name next, it reads the field ([`simple`], `name: value;`, or
//! [`unended`], `name: value`) or reads nothing when the block has no such
//! field, which ends the block.

use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::{Token, TokenKind};

use crate::error::ParseError;
use crate::fields::{optional, required};
use crate::forms::{field_start, list, separated, string, string_text};
use crate::words::{Name, QualifiedName, identifier, is_word_kind, qualified_name, unexpected};
use crate::{Block, Island, ParserContext, TypedIsland};

type R<T> = Result<T, ParseError>;

/// A block, `Keyword { field … }`, `Keyword;` or `Keyword {}`: its keyword
/// (`{` for one without), its fields, and its span, from the keyword to its
/// end.
#[derive(Debug, Clone)]
pub struct ReadBlock {
    /// The keyword, or `{`.
    pub keyword: Token,
    /// The fields, as read.
    pub fields: Vec<ReadField>,
    /// From the keyword to the block's end.
    pub source_info: SourceInfo,
}

/// `name: value`, positioned from the name to the field's end.
#[derive(Debug, Clone)]
pub struct ReadField {
    /// The field's name, as the walker names it.
    pub name: &'static str,
    /// Its value.
    pub value: ReadValue,
    /// From the name to the field's end.
    pub source_info: SourceInfo,
}

/// What a field holds.
#[derive(Debug, Clone)]
pub enum ReadValue {
    /// `'text'`.
    String(Token),
    /// A name, `'quoted'` or not.
    Identifier(Name),
    /// A qualified name, `my::Element`.
    Path(QualifiedName),
    /// `true` or `false`.
    Boolean(bool),
    /// A navigation path, `#/Type/property#`.
    NavigationPath(Box<Expression>),
    /// An expression, as the grammar parses it.
    Expression(Box<Expression>),
    /// A block.
    Block(ReadBlock),
    /// `[ value, … ]`.
    List(Vec<ReadValue>),
    /// `Type` or `Type #{ … }#`, which the grammar its type names reads.
    Specification(ReadSpecification),
    /// `{}`.
    Empty,
    /// `#{ … }#`.
    Island(Island),
    /// `Type #{ … }#`: embedded data, or a test assertion.
    TypedIsland(TypedIsland),
    /// `name: value` or `name = value`, from the name to the value's end.
    Named(Name, Box<ReadValue>, SourceInfo),
    /// `value -> value`, from the first to the second's end.
    Pair(Box<ReadValue>, Box<ReadValue>, SourceInfo),
    /// A value the walker parses again from its tokens' texts joined
    /// (`getText()`), and where it starts.
    Text(String, SourceInfo),
}

/// `Type` or `Type #{ … }#`.
#[derive(Debug, Clone)]
pub struct ReadSpecification {
    /// The type, as written.
    pub type_name: Name,
    /// The island, if any.
    pub island: Option<Island>,
    /// From the type to the island's end.
    pub source_info: SourceInfo,
}

impl ReadBlock {
    /// The values of the field `name`, in order, taken out of the block.
    pub fn all(&mut self, name: &str) -> Vec<ReadField> {
        let (taken, kept) = std::mem::take(&mut self.fields)
            .into_iter()
            .partition(|field| field.name == name);
        self.fields = kept;
        taken
    }

    /// The required field `name`'s one value, as the walker checks it on
    /// `at`.
    ///
    /// # Errors
    ///
    /// No value, or more than one.
    pub fn take(&mut self, name: &'static str, at: &SourceInfo) -> R<ReadField> {
        required(self.all(name), name, at)
    }

    /// The optional field `name`'s value, as the walker checks it on `at`.
    ///
    /// # Errors
    ///
    /// More than one value.
    pub fn take_optional(&mut self, name: &'static str, at: &SourceInfo) -> R<Option<ReadField>> {
        optional(self.all(name), name, at)
    }
}

impl ReadField {
    /// A value the grammar can't have read for the field: a bug.
    #[must_use]
    pub fn misread(&self) -> ParseError {
        ParseError::Unexpected {
            message: format!("the grammar misread the field '{}'", self.name),
            source_info: self.source_info.clone(),
        }
    }

    /// A string's value, unescaped.
    ///
    /// # Errors
    ///
    /// Not a string, or a malformed escape.
    pub fn string(self) -> R<String> {
        match &self.value {
            ReadValue::String(token) => string_text(token),
            _ => Err(self.misread()),
        }
    }

    /// A name, unquoted (`fromIdentifier`).
    ///
    /// # Errors
    ///
    /// Not a name.
    pub fn identifier(self) -> R<String> {
        match &self.value {
            ReadValue::Identifier(name) => Ok(name.value.to_string()),
            _ => Err(self.misread()),
        }
    }

    /// A qualified name's path, unquoted.
    ///
    /// # Errors
    ///
    /// Not a qualified name.
    pub fn path(self) -> R<String> {
        match self.value {
            ReadValue::Path(name) => Ok(name.path),
            _ => Err(self.misread()),
        }
    }

    /// A block.
    ///
    /// # Errors
    ///
    /// Not a block.
    pub fn block(self) -> R<ReadBlock> {
        match self.value {
            ReadValue::Block(block) => Ok(block),
            _ => Err(self.misread()),
        }
    }

    /// A list's values.
    ///
    /// # Errors
    ///
    /// Not a list.
    pub fn values(self) -> R<Vec<ReadValue>> {
        match self.value {
            ReadValue::List(values) => Ok(values),
            _ => Err(self.misread()),
        }
    }
}

/// Whether `token` is the keyword `word`.
#[must_use]
pub fn is_keyword(token: &Token, word: &str) -> bool {
    (is_word_kind(token.kind) || matches!(token.kind, TokenKind::True | TokenKind::False))
        && token.text == word
}

// ── Blocks and fields ────────────────────────────────────────────────────

/// A field reader: reads the field `name`, which is next, from its name to
/// its end, or returns `None`, reading nothing, when the block has no such
/// field.
pub trait Fields:
    FnMut(&mut ParserContext<'_>, &str) -> R<Option<(&'static str, ReadValue)>>
{
}

impl<F> Fields for F where
    F: FnMut(&mut ParserContext<'_>, &str) -> R<Option<(&'static str, ReadValue)>>
{
}

/// `{ field … }`: the fields `fields` reads, until one it doesn't; the
/// `{`, and the fields.
///
/// # Errors
///
/// No braces, or a field's error.
pub fn braces(ctx: &mut ParserContext<'_>, mut fields: impl Fields) -> R<(Token, Vec<ReadField>)> {
    let open = ctx.cursor().expect(TokenKind::LBrace)?;
    let read = fields_until(ctx, &mut fields)?;
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok((open, read))
}

/// The fields `fields` reads, until one it doesn't: a block's without its
/// braces, as a whole island's.
///
/// # Errors
///
/// A field's error.
pub fn fields_until(ctx: &mut ParserContext<'_>, fields: &mut impl Fields) -> R<Vec<ReadField>> {
    let mut read = Vec::new();
    loop {
        let token = ctx.cursor().peek().clone();
        if !is_word_kind(token.kind) {
            break;
        }
        let Some((name, value)) = fields(ctx, &token.text)? else {
            break;
        };
        read.push(ReadField {
            name,
            value,
            source_info: ctx.cursor().span_since(&token.source_info),
        });
    }
    Ok(read)
}

/// `Keyword { field … }`, the keyword next.
///
/// # Errors
///
/// No braces, or a field's error.
pub fn keyword_block(ctx: &mut ParserContext<'_>, fields: impl Fields) -> R<ReadBlock> {
    let keyword = ctx.cursor().advance().clone();
    let (_, fields) = braces(ctx, fields)?;
    Ok(ReadBlock {
        source_info: ctx.cursor().span_since(&keyword.source_info),
        keyword,
        fields,
    })
}

/// `{ field … }`: a block without a keyword, positioned on its braces.
///
/// # Errors
///
/// No braces, or a field's error.
pub fn bare_block(ctx: &mut ParserContext<'_>, fields: impl Fields) -> R<ReadBlock> {
    let (open, fields) = braces(ctx, fields)?;
    Ok(ReadBlock {
        source_info: ctx.cursor().span_since(&open.source_info),
        keyword: open,
        fields,
    })
}

/// `Keyword;`, or `Keyword {}` when `empty_braces` allows it, the keyword
/// next.
///
/// # Errors
///
/// Anything else after the keyword.
pub fn keyword_alone(ctx: &mut ParserContext<'_>, empty_braces: bool) -> R<ReadBlock> {
    let keyword = ctx.cursor().advance().clone();
    if empty_braces && ctx.cursor().eat(TokenKind::LBrace) {
        ctx.cursor().expect(TokenKind::RBrace)?;
    } else {
        ctx.cursor().expect(TokenKind::Semicolon)?;
    }
    Ok(ReadBlock {
        source_info: ctx.cursor().span_since(&keyword.source_info),
        keyword,
        fields: Vec::new(),
    })
}

/// `name: value;`, the name next, the value read by `value` and kept as
/// `name`.
///
/// # Errors
///
/// No `:` or `;`, or `value`'s error.
pub fn simple(
    ctx: &mut ParserContext<'_>,
    name: &'static str,
    value: impl FnOnce(&mut ParserContext<'_>) -> R<ReadValue>,
) -> R<Option<(&'static str, ReadValue)>> {
    field_start(ctx)?;
    let value = value(ctx)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok(Some((name, value)))
}

/// `name: value` with no `;`, the name next.
///
/// # Errors
///
/// No `:`, or `value`'s error.
pub fn unended(
    ctx: &mut ParserContext<'_>,
    name: &'static str,
    value: impl FnOnce(&mut ParserContext<'_>) -> R<ReadValue>,
) -> R<Option<(&'static str, ReadValue)>> {
    field_start(ctx)?;
    Ok(Some((name, value(ctx)?)))
}

/// A reader of a block, `Keyword …`, the keyword next.
pub type BlockReader<'a> = &'a mut dyn FnMut(&mut ParserContext<'_>) -> R<ReadBlock>;

/// One of `options`' keywords, read by its reader; anything else is an
/// error.
///
/// # Errors
///
/// No option's keyword next, or its reader's error.
pub fn choice(
    ctx: &mut ParserContext<'_>,
    options: &mut [(&str, BlockReader<'_>)],
) -> R<ReadValue> {
    let token = ctx.cursor().peek().clone();
    for (keyword, read) in options.iter_mut() {
        if is_keyword(&token, keyword) {
            return Ok(ReadValue::Block(read(ctx)?));
        }
    }
    Err(unexpected(&token))
}

// ── Values ────────────────────────────────────────────────────────────────

/// `'text'`.
///
/// # Errors
///
/// Anything else next.
pub fn string_value(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    Ok(ReadValue::String(string(ctx)?))
}

/// A name ([`identifier`]).
///
/// # Errors
///
/// Anything else next.
pub fn identifier_value(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    Ok(ReadValue::Identifier(identifier(ctx)?))
}

/// A qualified name ([`qualified_name`]).
///
/// # Errors
///
/// Anything else next.
pub fn path_value(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    Ok(ReadValue::Path(qualified_name(ctx)?))
}

/// A navigation path, `#/Type/property#`, read whole as one token of the
/// engine's lexer.
///
/// # Errors
///
/// Anything else next, or a malformed path.
pub fn navigation_path(ctx: &mut ParserContext<'_>) -> R<Expression> {
    let cursor = ctx.cursor();
    if !cursor.check(TokenKind::HashSlash) {
        return Err(unexpected(cursor.peek()));
    }
    let mut count = 1;
    while !matches!(cursor.peek_kind_at(count), TokenKind::Hash | TokenKind::Eof) {
        count += 1;
    }
    let block = ctx.read_tokens(count + 1);
    expression_block(ctx, &block)
}

/// An expression read as a block, which it must be whole.
// `ParserContext::parse_expression` itself is not general enough over the
// block's context lifetime, so the closure stays.
#[allow(clippy::redundant_closure_for_method_calls)]
fn expression_block(ctx: &mut ParserContext<'_>, block: &Block) -> R<Expression> {
    ctx.parse_block(block, |block| block.parse_expression())
}

/// An expression before the next `terminator` (see
/// [`ParserContext::read_until`]), as the grammars' `combinedExpression`
/// reads one before what follows it.
///
/// # Errors
///
/// A syntax error in the expression.
pub fn expression_until(ctx: &mut ParserContext<'_>, terminator: TokenKind) -> R<Expression> {
    let block = ctx.read_until(terminator)?;
    expression_block(ctx, &block)
}

/// `[ value, … ]`, possibly empty.
///
/// # Errors
///
/// No brackets, or `value`'s error.
pub fn values(
    ctx: &mut ParserContext<'_>,
    value: impl FnMut(&mut ParserContext<'_>) -> R<ReadValue>,
) -> R<ReadValue> {
    Ok(ReadValue::List(list(ctx, value)?))
}

/// `[ value, … ]`, at least one.
///
/// # Errors
///
/// No brackets, no value, or `value`'s error.
pub fn some_values(
    ctx: &mut ParserContext<'_>,
    value: impl FnMut(&mut ParserContext<'_>) -> R<ReadValue>,
) -> R<ReadValue> {
    ctx.cursor().expect(TokenKind::LBracket)?;
    let values = separated(ctx, value)?;
    ctx.cursor().expect(TokenKind::RBracket)?;
    Ok(ReadValue::List(values))
}

/// `Type` or `Type #{ … }#`, the type read by `type_name`.
///
/// # Errors
///
/// `type_name`'s error, or a malformed island.
pub fn specification(
    ctx: &mut ParserContext<'_>,
    type_name: impl FnOnce(&mut ParserContext<'_>) -> R<Name>,
) -> R<ReadValue> {
    let type_name = type_name(ctx)?;
    let island = if ctx.cursor().check(TokenKind::IslandOpen) {
        Some(ctx.read_island()?)
    } else {
        None
    };
    Ok(ReadValue::Specification(ReadSpecification {
        source_info: ctx.cursor().span_since(&type_name.source_info),
        type_name,
        island,
    }))
}

/// A primitive value's tokens: a collection's, to its `]`; a signed
/// number's; bytes', `toBytes('…')`; an enum value's, `my::Enum.VALUE`; or a
/// literal.
fn primitive_value_length(ctx: &mut ParserContext<'_>) -> usize {
    let cursor = ctx.cursor();
    let first = cursor.peek_kind();
    if first == TokenKind::LBracket {
        let mut count = 1;
        while !matches!(
            cursor.peek_kind_at(count),
            TokenKind::RBracket | TokenKind::Eof
        ) {
            count += 1;
        }
        return count + 1;
    }
    if matches!(first, TokenKind::Minus | TokenKind::Plus) {
        return 2;
    }
    if is_word_kind(first)
        && cursor.peek().text == "toBytes"
        && cursor.peek_kind_at(1) == TokenKind::LParen
    {
        return 4;
    }
    if is_word_kind(first) || first == TokenKind::StringLiteral {
        let mut next = 1;
        while cursor.peek_kind_at(next) == TokenKind::PathSep {
            next += 2;
        }
        if cursor.peek_kind_at(next) == TokenKind::Dot {
            return next + 2;
        }
    }
    1
}

fn parse_primitive_value(ctx: &mut ParserContext<'_>) -> R<Expression> {
    ctx.parse_primitive_value()
}

/// A primitive value, checked now, kept as its tokens' texts joined, which
/// some walkers parse again where the value starts (`getText()`).
///
/// # Errors
///
/// A malformed value.
pub fn primitive_value_text(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let length = primitive_value_length(ctx);
    let block = ctx.read_tokens(length);
    ctx.parse_block(&block, parse_primitive_value)?;
    Ok(ReadValue::Text(block.text(), block.source_info))
}

/// `Type #{ … }#`: embedded data, or a test assertion, not parsed yet.
///
/// # Errors
///
/// Anything else next.
pub fn typed_island(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    Ok(ReadValue::TypedIsland(ctx.read_typed_island()?))
}
