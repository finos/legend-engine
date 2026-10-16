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

//! A table's `milestoning(business(…), processing(…))`, the engine's
//! `milestoneSpec` and its core milestoning parsers
//! (`MilestoningParseTreeWalker`):
//!
//! ```text
//! business(BUS_FROM = from, BUS_THRU = thru (, THRU_IS_INCLUSIVE = bool)? (, INFINITY_DATE = %date)?)
//! business(BUS_SNAPSHOT_DATE = column)
//! processing(PROCESSING_IN = in, PROCESSING_OUT = out (, OUT_IS_INCLUSIVE = bool)? (, INFINITY_DATE = %date)?)
//! processing(PROCESSING_SNAPSHOT_DATE = column)
//! ```
//!
//! Engine extensions may add milestoning types: another type is unsupported,
//! so that the engine parses the section.
//!
//! The engine parses a milestoning's text again, from its type, with
//! `ParseTreeWalkerSourceInformation.offset`, which takes the type's 1-based
//! column for a 0-based offset: whatever is on the type's line is positioned
//! one column right of where it is. The positions here are shifted alike.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;

use crate::ast::{InfinityDate, Milestoning};
use crate::tokens::{expect_word, identifier, is_word, unexpected};

/// `milestoning( milestoning, … )`.
pub(crate) fn milestone_spec(ctx: &mut ParserContext<'_>) -> Result<Vec<Milestoning>, ParseError> {
    expect_word(ctx, "milestoning")?;
    ctx.cursor().expect(TokenKind::LParen)?;
    let mut milestonings = Vec::new();
    if !ctx.cursor().check(TokenKind::RParen) {
        loop {
            milestonings.push(milestoning(ctx)?);
            if !ctx.cursor().eat(TokenKind::Comma) {
                break;
            }
        }
    }
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(milestonings)
}

/// `type( specification )`.
fn milestoning(ctx: &mut ParserContext<'_>) -> Result<Milestoning, ParseError> {
    let start = ctx.cursor().current_source_info();
    let kind = identifier(ctx)?;
    let line = kind.source_info.start_line;
    match kind.text.as_str() {
        "business" => business(ctx, line),
        "processing" => processing(ctx, &start, line),
        _ => {
            skip_specification(ctx)?;
            Err(ParseError::Unsupported {
                message: format!(
                    "'{}' milestoning has no Rust grammar; the linked ones are: business, \
                     processing",
                    kind.text
                ),
                source_info: ctx.cursor().span_since(&start),
            })
        }
    }
}

/// `( … )`, whatever it holds.
fn skip_specification(ctx: &mut ParserContext<'_>) -> Result<(), ParseError> {
    ctx.cursor().expect(TokenKind::LParen)?;
    while !ctx.cursor().check(TokenKind::RParen) {
        if ctx.cursor().check(TokenKind::Eof) {
            return Err(unexpected(ctx.cursor().peek()));
        }
        ctx.cursor().advance();
    }
    ctx.cursor().advance();
    Ok(())
}

/// `business(…)`: from and thru, positioned from `BUS_FROM` to the last
/// value, or a snapshot date.
fn business(ctx: &mut ParserContext<'_>, line: u32) -> Result<Milestoning, ParseError> {
    ctx.cursor().expect(TokenKind::LParen)?;
    let inner = ctx.cursor().current_source_info();
    let milestoning = if is_word(ctx.cursor().peek(), "BUS_SNAPSHOT_DATE") {
        let snapshot_date = assignment(ctx, "BUS_SNAPSHOT_DATE")?.value;
        Milestoning::BusinessSnapshot {
            snapshot_date,
            source_info: reparsed(ctx.cursor().span_since(&inner), line),
        }
    } else {
        let from = assignment(ctx, "BUS_FROM")?.text;
        ctx.cursor().expect(TokenKind::Comma)?;
        let thru = assignment(ctx, "BUS_THRU")?.text;
        let (thru_is_inclusive, infinity_date) = options(ctx, "THRU_IS_INCLUSIVE", line)?;
        Milestoning::Business {
            from,
            thru,
            thru_is_inclusive,
            infinity_date,
            source_info: reparsed(ctx.cursor().span_since(&inner), line),
        }
    };
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(milestoning)
}

/// `processing(…)`: in and out, positioned from `processing` to `)`, or a
/// snapshot date, positioned from `PROCESSING_SNAPSHOT_DATE`.
fn processing(
    ctx: &mut ParserContext<'_>,
    start: &SourceInfo,
    line: u32,
) -> Result<Milestoning, ParseError> {
    ctx.cursor().expect(TokenKind::LParen)?;
    let inner = ctx.cursor().current_source_info();
    if is_word(ctx.cursor().peek(), "PROCESSING_SNAPSHOT_DATE") {
        let snapshot_date = assignment(ctx, "PROCESSING_SNAPSHOT_DATE")?.value;
        let source_info = reparsed(ctx.cursor().span_since(&inner), line);
        ctx.cursor().expect(TokenKind::RParen)?;
        return Ok(Milestoning::ProcessingSnapshot {
            snapshot_date,
            source_info,
        });
    }
    let in_column = assignment(ctx, "PROCESSING_IN")?.text;
    ctx.cursor().expect(TokenKind::Comma)?;
    let out_column = assignment(ctx, "PROCESSING_OUT")?.text;
    let (out_is_inclusive, infinity_date) = options(ctx, "OUT_IS_INCLUSIVE", line)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(Milestoning::Processing {
        in_column,
        out_column,
        out_is_inclusive,
        infinity_date,
        source_info: reparsed(ctx.cursor().span_since(start), line),
    })
}

/// A position as the engine's second parse of the milestoning reports it:
/// on the type's `line`, one column right.
fn reparsed(mut source_info: SourceInfo, line: u32) -> SourceInfo {
    if source_info.start_line == line {
        source_info.start_column += 1;
    }
    if source_info.end_line == line {
        source_info.end_column += 1;
    }
    source_info
}

/// `KEY = name`.
fn assignment(ctx: &mut ParserContext<'_>, key: &str) -> Result<crate::tokens::Name, ParseError> {
    expect_word(ctx, key)?;
    ctx.cursor().expect(TokenKind::Equals)?;
    identifier(ctx)
}

/// `(, INCLUSIVE_KEY = bool)? (, INFINITY_DATE = %date)?`.
fn options(
    ctx: &mut ParserContext<'_>,
    inclusive_key: &str,
    line: u32,
) -> Result<(bool, Option<InfinityDate>), ParseError> {
    let mut inclusive = false;
    if ctx.cursor().check(TokenKind::Comma) && is_word(ctx.cursor().peek_at(1), inclusive_key) {
        ctx.cursor().advance();
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::Equals)?;
        inclusive = match ctx.cursor().peek_kind() {
            TokenKind::True => true,
            TokenKind::False => false,
            _ => return Err(unexpected(ctx.cursor().peek())),
        };
        ctx.cursor().advance();
    }
    let mut infinity_date = None;
    if ctx.cursor().eat(TokenKind::Comma) {
        expect_word(ctx, "INFINITY_DATE")?;
        ctx.cursor().expect(TokenKind::Equals)?;
        let date = ctx.cursor().expect(TokenKind::DateLiteral)?;
        let value = date.text.rsplit('%').next().unwrap_or_default().to_string();
        infinity_date = Some(InfinityDate {
            strict: is_strict_date(&date.text),
            value,
            source_info: reparsed(date.source_info, line),
        });
    }
    Ok((inclusive, infinity_date))
}

/// `%YYYY-MM-DD`, the walker's strict date pattern.
fn is_strict_date(text: &str) -> bool {
    let Some(date) = text.strip_prefix('%') else {
        return false;
    };
    let parts: Vec<&str> = date.split('-').collect();
    parts.len() == 3
        && [4, 2, 2]
            .iter()
            .zip(&parts)
            .all(|(len, part)| part.len() == *len && part.bytes().all(|b| b.is_ascii_digit()))
}
