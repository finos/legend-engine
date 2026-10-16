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

//! End-to-end island dispatch: the host lexes `#tag{ … }#` as an island,
//! finds the parser linked for its tag and hands it the body, lexed again at
//! its place in the document, which the parser must consume whole; or, for a
//! grammar that reads its body raw, the body as written.

use std::any::Any;

use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::island::IslandContent;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::IslandParser;
use legend_pure_parser_parser::error::ParseError;
use smol_str::SmolStr;

// ---------------------------------------------------------------------------
// Mock content + parser for `#>{ body }#` (curly-body, tag = ">").
// ---------------------------------------------------------------------------

#[derive(Debug, Clone, PartialEq)]
struct CurlyEcho {
    text: SmolStr,
    source_info: SourceInfo,
}

impl IslandContent for CurlyEcho {
    fn tag(&self) -> &'static str {
        ">"
    }
    fn as_any(&self) -> &dyn Any {
        self
    }
    fn clone_box(&self) -> Box<dyn IslandContent> {
        Box::new(self.clone())
    }
    fn eq_content(&self, other: &dyn IslandContent) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|o| self == o)
    }
}

struct CurlyEchoParser;

impl IslandParser for CurlyEchoParser {
    fn tag(&self) -> &'static str {
        ">"
    }
    fn parse(
        &self,
        ctx: &mut legend_pure_parser_parser::ParserContext<'_>,
    ) -> Result<Box<dyn IslandContent>, ParseError> {
        let source_info = ctx.island_span().cloned().expect("an island");
        let cursor = ctx.cursor();
        // Echo every token of the body.
        let mut buf = String::new();
        while !cursor.check(TokenKind::Eof) {
            let tok = cursor.advance().clone();
            if !buf.is_empty() {
                buf.push(' ');
            }
            buf.push_str(&tok.text);
        }
        Ok(Box::new(CurlyEcho {
            text: SmolStr::new(buf),
            source_info,
        }))
    }
}

// ---------------------------------------------------------------------------
// Mock parser that reads one token of its body (tag = "one").
// ---------------------------------------------------------------------------

struct OneTokenParser;

impl IslandParser for OneTokenParser {
    fn tag(&self) -> &'static str {
        "one"
    }
    fn parse(
        &self,
        ctx: &mut legend_pure_parser_parser::ParserContext<'_>,
    ) -> Result<Box<dyn IslandContent>, ParseError> {
        let source_info = ctx.island_span().cloned().expect("an island");
        let tok = ctx.cursor().advance().clone();
        Ok(Box::new(CurlyEcho {
            text: tok.text,
            source_info,
        }))
    }
}

// ---------------------------------------------------------------------------
// Mock parser that reads its body as written (tag = "raw").
// ---------------------------------------------------------------------------

struct RawTextParser;

impl IslandParser for RawTextParser {
    fn tag(&self) -> &'static str {
        "raw"
    }
    fn raw_body(&self) -> bool {
        true
    }
    fn parse(
        &self,
        ctx: &mut legend_pure_parser_parser::ParserContext<'_>,
    ) -> Result<Box<dyn IslandContent>, ParseError> {
        let source_info = ctx.island_span().cloned().expect("an island");
        let text = SmolStr::new(ctx.island_text().expect("an island"));
        Ok(Box::new(CurlyEcho { text, source_info }))
    }
}

// ---------------------------------------------------------------------------
// Helper: parse a single function body and return the lone expression.
// ---------------------------------------------------------------------------

fn parse_body_expression(src: &str, parsers: Vec<Box<dyn IslandParser>>) -> Expression {
    // Wrap the island in a function body so it parses as an expression.
    let wrapped = format!("function test::f(): String[1] {{ {src} }}");
    let file =
        legend_pure_parser_parser::parse_with_islands(&wrapped, "tag_dispatch.pure", parsers)
            .unwrap_or_else(|p| {
                panic!(
                    "parse failed: {:?}",
                    p.errors.iter().map(ToString::to_string).collect::<Vec<_>>()
                )
            });

    let elem = &file.sections[0].elements[0];
    let legend_pure_parser_ast::element::Element::Function(f) = elem else {
        panic!("expected Function, got {elem:?}");
    };
    f.body
        .first()
        .cloned()
        .expect("function body has at least one expression")
}

#[test]
fn an_island_dispatches_by_tag_with_its_body_relexed() {
    let parsers: Vec<Box<dyn IslandParser>> = vec![Box::new(CurlyEchoParser)];
    let expr = parse_body_expression("#>{ alpha beta }#", parsers);
    let isl = match &expr {
        Expression::Island(i) => i,
        other => panic!("expected Island, got {other:?}"),
    };
    let echo = isl
        .content
        .as_any()
        .downcast_ref::<CurlyEcho>()
        .expect("CurlyEcho");
    assert_eq!(echo.text.as_str(), "alpha beta");
    // The whole island: `function test::f(): String[1] { ` is 32 columns.
    assert_eq!(
        (echo.source_info.start_column, echo.source_info.end_column),
        (33, 49)
    );
}

#[test]
fn an_island_parser_must_consume_its_whole_body() {
    let parsers: Vec<Box<dyn IslandParser>> = vec![Box::new(OneTokenParser)];
    let wrapped = "function test::f(): String[1] { #one{ alpha beta }# }";
    let error = legend_pure_parser_parser::parse_with_islands(wrapped, "t.pure", parsers)
        .expect_err("`beta` is left over");
    let si = error.errors[0].source_info().expect("positioned");
    assert_eq!(
        (si.start_line, si.start_column),
        (1, 45),
        "{:?}",
        error.errors
    );
}

#[test]
fn a_raw_island_reads_its_body_as_written() {
    // Not Pure: lexing would reject the unterminated quote.
    let parsers: Vec<Box<dyn IslandParser>> = vec![Box::new(RawTextParser)];
    let expr = parse_body_expression("#raw{\n  I'm, raw #{ nested }#\n}#", parsers);
    let Expression::Island(isl) = &expr else {
        panic!("expected Island, got {expr:?}");
    };
    let raw = isl
        .content
        .as_any()
        .downcast_ref::<CurlyEcho>()
        .expect("CurlyEcho");
    assert_eq!(raw.text.as_str(), "\n  I'm, raw #{ nested }#\n");
    assert_eq!(
        (raw.source_info.start_line, raw.source_info.end_line),
        (1, 3)
    );
}

#[test]
fn unknown_tag_is_unsupported() {
    let parsers: Vec<Box<dyn IslandParser>> = vec![Box::new(CurlyEchoParser)];
    let wrapped = "function test::f(): String[1] { #UNKNOWN_TAG{ I'm not Pure }# }";
    let result =
        legend_pure_parser_parser::parse_with_islands(wrapped, "tag_dispatch.pure", parsers);
    let error = result.expect_err("unregistered tag should fail");
    let legend_pure_parser_parser::error::ParseError::Unsupported { message, .. } =
        &error.errors[0]
    else {
        panic!("{:?}", error.errors);
    };
    // The message names the islands that are linked.
    assert_eq!(
        message,
        "no island grammar is linked for '#UNKNOWN_TAG{…}#'; the linked ones are: #>{…}#"
    );
}
