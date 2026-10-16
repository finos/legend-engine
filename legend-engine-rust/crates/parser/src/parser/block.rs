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

//! Parts of a section read now and parsed later: a `{ … }` block (a mapping
//! element's body), a typed island `Type #{ … }#` (embedded data, a test
//! assertion), and the tokens before a terminator (a field's value, before
//! its `;`).
//!
//! The engine's section grammars read these as text, and their walkers parse
//! that text afterwards with the grammar the part's type names. A section's
//! syntax errors are therefore reported before any error inside such a
//! part, and a part is only parsed when the walker gets to it. A section
//! grammar that follows suit reads its parts with
//! [`ParserContext::read_block`] and [`ParserContext::read_typed_island`],
//! and parses them once the section is read.

use legend_pure_parser_ast::SourceInfo;
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_lexer::{Token, TokenKind};
use smol_str::SmolStr;

use super::{Parser, ParserContext, R};
use crate::cursor::Cursor;
use crate::error::ParseError;

/// Tokens read but not parsed yet: a `{ … }` block's
/// ([`ParserContext::read_block`]), those before a terminator
/// ([`ParserContext::read_until`]), or a given number
/// ([`ParserContext::read_tokens`]).
#[derive(Debug, Clone, PartialEq)]
pub struct Block {
    /// The tokens, then an end of input positioned on the token that ends
    /// them: the closing brace, or the terminator.
    tokens: Vec<Token>,
    /// From `{` to `}`; from the first token to the last.
    pub source_info: SourceInfo,
}

impl Block {
    /// The token that ends the block: its `}`, or the terminator.
    #[must_use]
    pub fn close(&self) -> &SourceInfo {
        // The end of input the reader appends.
        &self.tokens[self.tokens.len() - 1].source_info
    }

    /// The block with each token, and its end, positioned by `position`:
    /// some of the engine's walkers parse a block's text again from where
    /// they say it is, and so position what they read there differently.
    #[must_use]
    pub fn with_positions(&self, position: impl Fn(&SourceInfo) -> SourceInfo) -> Self {
        Self {
            tokens: self
                .tokens
                .iter()
                .map(|token| Token {
                    source_info: position(&token.source_info),
                    ..token.clone()
                })
                .collect(),
            source_info: position(&self.source_info),
        }
    }

    /// The block's tokens' texts, joined without what was between them, as
    /// ANTLR's `getText` reads a rule's text: some of the engine's walkers
    /// parse that text again.
    #[must_use]
    pub fn text(&self) -> String {
        self.tokens
            .iter()
            .map(|token| token.text.as_str())
            .collect()
    }
}

/// `#{ … }#`, not parsed yet: an island whose body a grammar reads whole,
/// like a runtime's embedded connection, `#{ JsonModelConnection { … } }#`.
#[derive(Debug, Clone, PartialEq)]
pub struct Island {
    /// `#{`, or `# Type {`: some grammars' islands name their type there.
    pub(crate) open: Token,
    /// The body as written, positioned on its first character.
    pub(crate) body: Token,
    /// From `#{` to `}#`.
    pub source_info: SourceInfo,
}

impl Island {
    /// The body as written, without `#{` and `}#`.
    #[must_use]
    pub fn text(&self) -> &str {
        &self.body.text
    }

    /// What is written between `#` and `{`, as written: an island's type,
    /// in the grammars whose islands name one (`# UserPassword { … }#`).
    #[must_use]
    pub fn tag(&self) -> &str {
        let text = self.open.text.as_str();
        text.get(1..text.len().saturating_sub(1))
            .unwrap_or_default()
    }

    /// `#{`, or `# Type {`.
    #[must_use]
    pub fn open_source_info(&self) -> &SourceInfo {
        &self.open.source_info
    }

    /// The body, from its first character.
    #[must_use]
    pub fn body_source_info(&self) -> &SourceInfo {
        &self.body.source_info
    }
}

/// `Type #{ … }#`, not parsed yet: embedded data or a test assertion.
#[derive(Debug, Clone, PartialEq)]
pub struct TypedIsland {
    /// The type, e.g. `ExternalFormat` or `EqualToJson`.
    pub type_name: SmolStr,
    /// The type.
    pub type_source_info: SourceInfo,
    /// The body as written, positioned on its first character.
    pub(crate) body: Token,
    /// From the type to `}#`.
    pub source_info: SourceInfo,
}

impl TypedIsland {
    /// The body as written, without `#{` and `}#`.
    #[must_use]
    pub fn text(&self) -> &str {
        &self.body.text
    }
}

impl Parser {
    /// `#{ … }#`.
    pub(crate) fn read_island(&mut self) -> R<Island> {
        let open = self.cursor.expect(TokenKind::IslandOpen)?;
        let body = self.cursor.expect(TokenKind::IslandContent)?;
        self.cursor.expect(TokenKind::RBraceHash)?;
        let source_info = self.cursor.span_since(&open.source_info);
        Ok(Island {
            open,
            body,
            source_info,
        })
    }

    /// `Type #{ … }#`.
    pub(crate) fn read_typed_island(&mut self) -> R<TypedIsland> {
        let (type_name, type_source_info) = self.cursor.expect_identifier_or_keyword()?;
        self.cursor.expect(TokenKind::IslandOpen)?;
        let body = self.cursor.expect(TokenKind::IslandContent)?;
        self.cursor.expect(TokenKind::RBraceHash)?;
        let source_info = self.cursor.span_since(&type_source_info);
        Ok(TypedIsland {
            type_name,
            type_source_info,
            body,
            source_info,
        })
    }
}

impl ParserContext<'_> {
    /// Reads a `{ … }` block, braces nested, for
    /// [`parse_block`](Self::parse_block). Islands are single tokens, so
    /// the braces of `#{ … }#` don't count.
    ///
    /// # Errors
    ///
    /// No `{`, or no `}` to close it.
    pub fn read_block(&mut self) -> R<Block> {
        let cursor = self.cursor();
        let open = cursor.expect(TokenKind::LBrace)?;
        let mut depth = 1_usize;
        let mut tokens = Vec::new();
        loop {
            let token = cursor.peek().clone();
            match token.kind {
                TokenKind::LBrace => depth += 1,
                TokenKind::RBrace if depth == 1 => {
                    cursor.advance();
                    let source_info = open.source_info.merge(&token.source_info);
                    tokens.push(Token {
                        kind: TokenKind::Eof,
                        text: SmolStr::default(),
                        source_info: token.source_info,
                    });
                    return Ok(Block {
                        tokens,
                        source_info,
                    });
                }
                TokenKind::RBrace => depth -= 1,
                TokenKind::Eof => {
                    return Err(ParseError::expected(
                        "'}'",
                        TokenKind::Eof,
                        token.source_info,
                    ));
                }
                _ => {}
            }
            tokens.push(token);
            cursor.advance();
        }
    }

    /// Reads the tokens before the next `terminator` outside parentheses,
    /// brackets and braces, for [`parse_block`](Self::parse_block), and
    /// leaves the terminator: e.g. a field's value, before its `;`. A
    /// closing parenthesis, bracket or brace that none of the tokens opened
    /// ends them too, as does the end of input.
    ///
    /// The engine's grammars read such a value as far as the rest of their
    /// rule allows, so a Pure lambda there doesn't take the field's `;` as
    /// the separator of another statement of its body.
    ///
    /// # Errors
    ///
    /// None yet: the block is checked when parsed.
    pub fn read_until(&mut self, terminator: TokenKind) -> R<Block> {
        let cursor = self.cursor();
        let start = cursor.current_source_info();
        let mut depth = 0_usize;
        let mut tokens: Vec<Token> = Vec::new();
        loop {
            let token = cursor.peek().clone();
            match token.kind {
                TokenKind::LParen | TokenKind::LBracket | TokenKind::LBrace => depth += 1,
                TokenKind::RParen | TokenKind::RBracket | TokenKind::RBrace if depth > 0 => {
                    depth -= 1;
                }
                kind if kind == terminator && depth == 0 => break,
                TokenKind::RParen | TokenKind::RBracket | TokenKind::RBrace | TokenKind::Eof => {
                    break;
                }
                _ => {}
            }
            tokens.push(token);
            cursor.advance();
        }
        let end = cursor.current_source_info();
        let source_info = tokens
            .last()
            .map_or_else(|| end.clone(), |last| start.merge(&last.source_info));
        tokens.push(Token {
            kind: TokenKind::Eof,
            text: SmolStr::default(),
            source_info: end,
        });
        Ok(Block {
            tokens,
            source_info,
        })
    }

    /// Reads the next `count` tokens, for [`parse_block`](Self::parse_block):
    /// e.g. an expression whose end its section grammar finds itself. The
    /// block's end of input is positioned on the token after them.
    pub fn read_tokens(&mut self, count: usize) -> Block {
        let cursor = self.cursor();
        let start = cursor.current_source_info();
        let mut tokens: Vec<Token> = Vec::with_capacity(count + 1);
        for _ in 0..count {
            if cursor.check(TokenKind::Eof) {
                break;
            }
            tokens.push(cursor.advance().clone());
        }
        let end = cursor.current_source_info();
        let source_info = tokens
            .last()
            .map_or_else(|| end.clone(), |last| start.merge(&last.source_info));
        tokens.push(Token {
            kind: TokenKind::Eof,
            text: SmolStr::default(),
            source_info: end,
        });
        Block {
            tokens,
            source_info,
        }
    }

    /// Parses `block` with `parse`, which must read it whole: its context's
    /// cursor holds the block's tokens, then an end of input positioned on
    /// the closing `}`. Islands in the block are parsed with this parser's
    /// island grammars.
    ///
    /// # Errors
    ///
    /// `parse`'s error, a token `parse` left, or else the first error `parse`
    /// deferred ([`defer`](Self::defer)).
    pub fn parse_block<T>(
        &mut self,
        block: &Block,
        parse: impl FnOnce(&mut ParserContext<'_>) -> R<T>,
    ) -> R<T> {
        let island_parsers = std::mem::take(&mut self.parser.island_parsers);
        let mut body =
            Parser::with_island_parsers(Cursor::new(block.tokens.clone()), island_parsers);
        body.offset = self.parser.offset;
        let result = parse(&mut ParserContext {
            parser: &mut body,
            island: None,
        })
        .and_then(|value| {
            if !body.cursor.check(TokenKind::Eof) {
                let token = body.cursor.peek();
                return Err(ParseError::unexpected(
                    format!("Unexpected token '{}'", token.text),
                    token.source_info.clone(),
                ));
            }
            // The walker's errors come after the grammar's.
            body.deferred.take().map_or(Ok(value), Err)
        });
        self.parser.island_parsers = std::mem::take(&mut body.island_parsers);
        result
    }

    /// Parses `text` as one expression, lexed as if it started at `start`'s
    /// start: some of the engine's walkers parse again a text they rebuilt
    /// (see [`Block::text`]), positioned where they say.
    ///
    /// # Errors
    ///
    /// A lexical or syntax error, or text left after the expression.
    pub fn parse_expression_text(&mut self, text: &str, start: &SourceInfo) -> R<Expression> {
        let line = i32::try_from(start.start_line).unwrap_or(i32::MAX);
        let column = i32::try_from(start.start_column).unwrap_or(i32::MAX);
        let offset =
            legend_pure_parser_ast::SourceOffset::new(line - 1, column - 1).unwrap_or_default();
        let tokens = legend_pure_parser_lexer::tokenize_with_offset(text, &start.source, offset)?;
        let island_parsers = std::mem::take(&mut self.parser.island_parsers);
        let mut parser = Parser::with_island_parsers(Cursor::new(tokens), island_parsers);
        parser.offset = self.parser.offset;
        let result = parser.parse_expression().and_then(|expression| {
            if parser.cursor.check(TokenKind::Eof) {
                Ok(expression)
            } else {
                let token = parser.cursor.peek();
                Err(ParseError::unexpected(
                    format!("Unexpected token '{}'", token.text),
                    token.source_info.clone(),
                ))
            }
        });
        self.parser.island_parsers = std::mem::take(&mut parser.island_parsers);
        result
    }

    /// Parses `text` with `parse`, which must read it whole, lexed as if it
    /// started at `start`'s start: some of the engine's walkers parse again
    /// a text they rebuilt (see [`Block::text`]), positioned where they say.
    ///
    /// # Errors
    ///
    /// A lexical error, `parse`'s error, or text `parse` left.
    pub fn parse_text<T>(
        &mut self,
        text: &str,
        start: &SourceInfo,
        parse: impl FnOnce(&mut ParserContext<'_>) -> R<T>,
    ) -> R<T> {
        let line = i32::try_from(start.start_line).unwrap_or(i32::MAX);
        let column = i32::try_from(start.start_column).unwrap_or(i32::MAX);
        let offset =
            legend_pure_parser_ast::SourceOffset::new(line - 1, column - 1).unwrap_or_default();
        let tokens = legend_pure_parser_lexer::tokenize_with_offset(text, &start.source, offset)?;
        let island_parsers = std::mem::take(&mut self.parser.island_parsers);
        let mut parser = Parser::with_island_parsers(Cursor::new(tokens), island_parsers);
        parser.offset = self.parser.offset;
        let result = parse(&mut ParserContext {
            parser: &mut parser,
            island: None,
        })
        .and_then(|value| {
            if parser.cursor.check(TokenKind::Eof) {
                Ok(value)
            } else {
                let token = parser.cursor.peek();
                Err(ParseError::unexpected(
                    format!("Unexpected token '{}'", token.text),
                    token.source_info.clone(),
                ))
            }
        });
        self.parser.island_parsers = std::mem::take(&mut parser.island_parsers);
        result
    }

    /// Parses the body of a typed island read with
    /// [`read_typed_island`](Self::read_typed_island) with `parse`, which must
    /// read it whole: its context's cursor holds the body's tokens, lexed at
    /// their place in the document, and its
    /// [`island_span`](Self::island_span) is the whole island. For a grammar
    /// of its own in a plug-in's island, like a service store's content
    /// patterns.
    ///
    /// # Errors
    ///
    /// A lexical error, `parse`'s error, or a token `parse` left.
    pub fn parse_typed_island<T>(
        &mut self,
        island: &TypedIsland,
        parse: impl FnOnce(&mut ParserContext<'_>) -> R<T>,
    ) -> R<T> {
        super::expression::parse_island_body(&island.body, island.source_info.clone(), false, parse)
    }

    /// Reads an island, `#{ … }#`, to parse later with
    /// [`parse_island`](Self::parse_island).
    ///
    /// # Errors
    ///
    /// Anything else next.
    pub fn read_island(&mut self) -> R<Island> {
        self.parser.read_island()
    }

    /// Parses the body of an island read with
    /// [`read_island`](Self::read_island) with `parse`, which must read it
    /// whole: its context's cursor holds the body's tokens, lexed at their
    /// place in the document, and its [`island_span`](Self::island_span) is
    /// the island, `#{` to `}#`.
    ///
    /// # Errors
    ///
    /// A lexical error, `parse`'s error, or a token `parse` left.
    pub fn parse_island<T>(
        &mut self,
        island: &Island,
        parse: impl FnOnce(&mut ParserContext<'_>) -> R<T>,
    ) -> R<T> {
        super::expression::parse_island_body(&island.body, island.source_info.clone(), false, parse)
    }

    /// Parses `text`, in place of an island's body, with `parse`, lexed as if
    /// it started where the body does: some of the engine's walkers parse
    /// their island's body with a few characters cut off its end (see
    /// [`parse_island`](Self::parse_island) for the rest).
    ///
    /// # Errors
    ///
    /// A lexical error, `parse`'s error, or a token `parse` left.
    pub fn parse_island_text<T>(
        &mut self,
        island: &Island,
        text: &str,
        parse: impl FnOnce(&mut ParserContext<'_>) -> R<T>,
    ) -> R<T> {
        let body = Token {
            text: SmolStr::new(text),
            ..island.body.clone()
        };
        super::expression::parse_island_body(&body, island.source_info.clone(), false, parse)
    }

    /// Reads a typed island, `Type #{ … }#`, to parse later as embedded
    /// data ([`parse_embedded_data_island`](Self::parse_embedded_data_island))
    /// or a test assertion
    /// ([`parse_test_assertion`](Self::parse_test_assertion)).
    ///
    /// # Errors
    ///
    /// Anything else next.
    pub fn read_typed_island(&mut self) -> R<TypedIsland> {
        self.parser.read_typed_island()
    }
}

#[cfg(test)]
mod tests {
    use super::super::test_support::{position, with_context};
    use super::*;

    #[test]
    fn a_block_ends_at_its_own_brace() {
        with_context("{ a { b } #{ c { d }# } e", |ctx| {
            let block = ctx.read_block().expect("reads");
            assert_eq!(position(&block.source_info), (1, 1, 1, 23));
            assert_eq!(position(block.close()), (1, 23, 1, 23));
            // What follows is left.
            assert_eq!(ctx.cursor().peek().text, "e");
            let words = ctx
                .parse_block(&block, |body| {
                    let mut words = Vec::new();
                    while !body.cursor().check(TokenKind::Eof) {
                        words.push(body.cursor().advance().text.to_string());
                    }
                    Ok(words)
                })
                .expect("parses");
            assert_eq!(words, ["a", "{", "b", "}", "#{", " c { d ", "}#"]);
        });
    }

    #[test]
    fn a_block_must_be_read_whole() {
        with_context("{ a b }", |ctx| {
            let block = ctx.read_block().expect("reads");
            let error = ctx
                .parse_block(&block, |body| {
                    body.cursor().advance();
                    Ok(())
                })
                .expect_err("b is left");
            assert_eq!(
                position(error.source_info().expect("positioned")),
                (1, 5, 1, 5)
            );
            // The end of the block is its `}`.
            let error = ctx
                .parse_block(&block, |body| {
                    body.cursor().advance();
                    body.cursor().advance();
                    body.cursor().expect(TokenKind::Comma)
                })
                .expect_err("no comma");
            assert_eq!(
                position(error.source_info().expect("positioned")),
                (1, 7, 1, 7)
            );
        });
    }

    #[test]
    fn a_value_ends_at_its_terminator_outside_brackets() {
        with_context("|f([a; b], {x|$x; 1}); c )", |ctx| {
            let value = ctx.read_until(TokenKind::Semicolon).expect("reads");
            assert_eq!(position(&value.source_info), (1, 1, 1, 21));
            // The terminator is left, and ends the value.
            assert!(ctx.cursor().check(TokenKind::Semicolon));
            assert_eq!(position(value.close()), (1, 22, 1, 22));
            ctx.cursor().advance();
            // So does a closing parenthesis nothing opened.
            let value = ctx.read_until(TokenKind::Semicolon).expect("reads");
            assert_eq!(position(&value.source_info), (1, 24, 1, 24));
            assert!(ctx.cursor().check(TokenKind::RParen));
        });
    }

    #[test]
    fn an_islands_body_is_parsed_where_it_is_written() {
        with_context("#{ Type\n  { a } }# b", |ctx| {
            let island = ctx.read_island().expect("reads");
            assert_eq!(position(&island.source_info), (1, 1, 2, 10));
            assert_eq!(island.tag(), "");
            assert_eq!(island.text(), " Type\n  { a } ");
            assert_eq!(ctx.cursor().peek().text, "b");
            let (span, a) = ctx
                .parse_island(&island, |body| {
                    body.cursor().advance();
                    let block = body.read_block()?;
                    let span = body.island_span().cloned();
                    let a = body.parse_block(&block, |inner| {
                        Ok(inner.cursor().advance().source_info.clone())
                    })?;
                    Ok((span, a))
                })
                .expect("parses");
            // The island is the span the grammar sees; its tokens are where
            // they are in the document.
            assert_eq!(span.as_ref().map(position), Some((1, 1, 2, 10)));
            assert_eq!(position(&a), (2, 5, 2, 5));
        });
    }

    #[test]
    fn a_blocks_tokens_can_be_positioned_elsewhere() {
        with_context("{ a\n b }", |ctx| {
            let block = ctx.read_block().expect("reads");
            let moved = block.with_positions(|si| {
                SourceInfo::new(
                    si.source.clone(),
                    si.start_line + 10,
                    si.start_column,
                    si.end_line + 10,
                    si.end_column,
                )
            });
            let positions = ctx
                .parse_block(&moved, |body| {
                    let a = body.cursor().advance().source_info.clone();
                    let b = body.cursor().advance().source_info.clone();
                    Ok((position(&a), position(&b)))
                })
                .expect("parses");
            assert_eq!(positions, ((11, 3, 11, 3), (12, 2, 12, 2)));
            // Its end too.
            assert_eq!(position(moved.close()), (12, 4, 12, 4));
        });
    }

    #[test]
    fn a_number_of_tokens_ends_at_the_next() {
        with_context("a + b c", |ctx| {
            let value = ctx.read_tokens(3);
            assert_eq!(position(&value.source_info), (1, 1, 1, 5));
            assert_eq!(position(value.close()), (1, 7, 1, 7));
            assert_eq!(ctx.cursor().peek().text, "c");
        });
    }

    #[test]
    fn a_deferred_error_comes_after_the_grammars() {
        with_context("{ a b }", |ctx| {
            let block = ctx.read_block().expect("reads");
            let walker = |body: &mut ParserContext<'_>| {
                let a = body.cursor().advance().clone();
                body.defer(ParseError::unexpected("walker", a.source_info));
            };
            // Read whole: the deferred error.
            let error = ctx
                .parse_block(&block, |body| {
                    walker(body);
                    body.cursor().advance();
                    Ok(())
                })
                .expect_err("deferred");
            assert_eq!(error.message(), "walker");
            // A syntax error further on comes first.
            let error = ctx
                .parse_block(&block, |body| {
                    walker(body);
                    body.cursor().expect(TokenKind::Comma)
                })
                .expect_err("syntax");
            assert_ne!(error.message(), "walker");
        });
    }

    #[test]
    fn a_block_without_its_brace_is_an_error() {
        with_context("{ a { b }", |ctx| {
            assert!(ctx.read_block().is_err());
        });
    }

    #[test]
    fn a_typed_island_is_read_whole() {
        with_context("EqualToJson #{ expected: x; }# ;", |ctx| {
            let island = ctx.read_typed_island().expect("reads");
            assert_eq!(island.type_name, "EqualToJson");
            assert_eq!(position(&island.type_source_info), (1, 1, 1, 11));
            assert_eq!(position(&island.source_info), (1, 1, 1, 30));
            assert_eq!(island.text(), " expected: x; ");
            assert!(ctx.cursor().check(TokenKind::Semicolon));
        });
    }
}
