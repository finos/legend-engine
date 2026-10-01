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

//! `Relation #{ … }#` data: the engine's relation data grammar
//! (`RelationElementsDataLexerGrammar` and `…ParserGrammar`), tables of text
//! cells.
//!
//! ```text
//! definition:      relationElement* EOF
//! relationElement: paths TABLE_START table | TABLE_START tableCSV
//! table:           columnNames rows TABLE_END
//! tableCSV:        NEWLINE* columnNames rows TABLE_END
//! paths:           identifier (DOT identifier)*
//! columnNames:     cell (ROW_COMMA cell)*
//! rows:            (NEWLINE rowValues)*
//! rowValues:       cell (ROW_COMMA cell)*
//! cell:            ROW_VALUE | QUOTED_ROW_VALUE | (nothing)
//! ```
//!
//! Outside tables the Pure lexer's rules apply: whitespace and comments are
//! skipped. `TABLE_START` is `:` with all the whitespace after it, line breaks
//! included, and enters the table mode, which `;` (`TABLE_END`) leaves:
//!
//! ```text
//! QUOTED_ROW_VALUE: [ \t]* '"' ('""' | ~["\r\n])* '"' [ \t]*
//! ROW_VALUE:        ~[",\r\n;]+
//! ROW_COMMA:        ','
//! NEWLINE:          '\r'? '\n' [ \t]*
//! ```
//!
//! As ANTLR's lexer does, the longest match wins, the first rule on a tie.

use legend_pure_parser_ast::SourceInfo;
use legend_pure_parser_ast::data::{RelationData, RelationElement};
use smol_str::SmolStr;

use super::strings::string_value;
use crate::error::ParseError;

/// Where a text starts in the document: its first character's line and
/// column. Lines after its first need no column shift.
#[derive(Debug, Clone, Copy)]
pub(crate) struct Origin {
    /// The document line of the text's first line.
    pub(crate) line: u32,
    /// The document column of the text's first character.
    pub(crate) column: u32,
}

/// Parses relation data, `text` starting at `origin` in `source`, and
/// positions it as the engine's walker does.
pub(crate) fn parse(text: &str, origin: Origin, source: &str) -> Result<RelationData, ParseError> {
    let positions = Positions { origin, source };
    let tokens = tokenize(text).map_err(|at| {
        ParseError::unexpected(
            "Unexpected character in relation data",
            positions.span_at(at.line, at.column),
        )
    })?;
    RelationDataParser {
        tokens,
        next: 0,
        positions,
    }
    .definition()
}

/// Java's `String.trim()`: code points up to U+0020 go.
pub(crate) fn java_trim(text: &str) -> &str {
    text.trim_matches(|c: char| c <= ' ')
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum Kind {
    Identifier,
    QuotedIdentifier,
    Dot,
    TableStart,
    RowValue,
    QuotedRowValue,
    RowComma,
    Newline,
    TableEnd,
    /// Any other token of the Pure lexer: never valid here.
    Other,
    Eof,
}

/// A token, positioned as ANTLR does: its line (1-based) and column
/// (0-based, in code points) in the text.
#[derive(Debug, Clone, Copy)]
struct Token<'a> {
    kind: Kind,
    text: &'a str,
    line: u32,
    column: u32,
}

/// A character no lexer rule matches.
#[derive(Debug, Clone, Copy)]
struct Unmatched {
    line: u32,
    column: u32,
}

fn tokenize(text: &str) -> Result<Vec<Token<'_>>, Unmatched> {
    let mut lexer = Lexer {
        text,
        pos: 0,
        line: 1,
        column: 0,
    };
    let mut tokens = Vec::new();
    let mut in_table = false;
    loop {
        let token = if in_table {
            lexer.table_token()?
        } else {
            lexer.default_token()?
        };
        match token.kind {
            Kind::TableStart => in_table = true,
            Kind::TableEnd => in_table = false,
            _ => {}
        }
        let eof = token.kind == Kind::Eof;
        tokens.push(token);
        if eof {
            return Ok(tokens);
        }
    }
}

struct Lexer<'a> {
    text: &'a str,
    /// Byte offset.
    pos: usize,
    line: u32,
    column: u32,
}

impl<'a> Lexer<'a> {
    fn rest(&self) -> &'a str {
        &self.text[self.pos..]
    }

    /// Moves past `bytes` bytes, tracking lines and columns.
    fn skip(&mut self, bytes: usize) {
        for c in self.text[self.pos..self.pos + bytes].chars() {
            if c == '\n' {
                self.line += 1;
                self.column = 0;
            } else {
                self.column += 1;
            }
        }
        self.pos += bytes;
    }

    /// A token of `bytes` bytes at the current position, moving past it.
    fn token(&mut self, kind: Kind, bytes: usize) -> Token<'a> {
        let token = Token {
            kind,
            text: &self.text[self.pos..self.pos + bytes],
            line: self.line,
            column: self.column,
        };
        self.skip(bytes);
        token
    }

    fn eof(&self) -> Token<'a> {
        Token {
            kind: Kind::Eof,
            text: "<EOF>",
            line: self.line,
            column: self.column,
        }
    }

    fn unmatched(&self) -> Unmatched {
        Unmatched {
            line: self.line,
            column: self.column,
        }
    }

    /// The Pure lexer's rules, as far as this grammar needs them.
    fn default_token(&mut self) -> Result<Token<'a>, Unmatched> {
        loop {
            let rest = self.rest();
            let Some(c) = rest.chars().next() else {
                return Ok(self.eof());
            };
            if matches!(c, ' ' | '\r' | '\t' | '\n') {
                let run = rest.len() - rest.trim_start_matches([' ', '\r', '\t', '\n']).len();
                self.skip(run);
            } else if rest.starts_with("//") {
                let line = rest.find(['\r', '\n']).unwrap_or(rest.len());
                self.skip(line);
            } else if let Some(end) = rest.strip_prefix("/*").and_then(|r| r.find("*/")) {
                self.skip(end + 4);
            } else if c == ':' {
                let spaces =
                    rest[1..].len() - rest[1..].trim_start_matches([' ', '\r', '\t', '\n']).len();
                return Ok(self.token(Kind::TableStart, 1 + spaces));
            } else if rest.starts_with("..") {
                return Ok(self.token(Kind::Other, 2));
            } else if c == '.' {
                return Ok(self.token(Kind::Dot, 1));
            } else if c == '\'' {
                let length = quoted_string_length(rest).ok_or_else(|| self.unmatched())?;
                return Ok(self.token(Kind::QuotedIdentifier, length));
            } else if c.is_ascii_alphanumeric() || c == '_' {
                let length = rest
                    .find(|c: char| !(c.is_ascii_alphanumeric() || c == '_' || c == '$'))
                    .unwrap_or(rest.len());
                let word = &rest[..length];
                // `INTEGER` and `BOOLEAN` come before `VALID_STRING`.
                let kind = if word.bytes().all(|b| b.is_ascii_digit())
                    || word == "true"
                    || word == "false"
                {
                    Kind::Other
                } else {
                    Kind::Identifier
                };
                return Ok(self.token(kind, length));
            } else {
                return Ok(self.token(Kind::Other, c.len_utf8()));
            }
        }
    }

    fn table_token(&mut self) -> Result<Token<'a>, Unmatched> {
        let rest = self.rest();
        let Some(c) = rest.chars().next() else {
            return Ok(self.eof());
        };
        let quoted = quoted_row_value_length(rest).unwrap_or(0);
        let row = rest.find(['"', ',', '\r', '\n', ';']).unwrap_or(rest.len());
        if quoted > 0 && quoted >= row {
            return Ok(self.token(Kind::QuotedRowValue, quoted));
        }
        if row > 0 {
            return Ok(self.token(Kind::RowValue, row));
        }
        match c {
            ',' => Ok(self.token(Kind::RowComma, 1)),
            ';' => Ok(self.token(Kind::TableEnd, 1)),
            '\n' | '\r' if rest.starts_with('\n') || rest.starts_with("\r\n") => {
                let line_break = if c == '\r' { 2 } else { 1 };
                let indent = rest[line_break..].len()
                    - rest[line_break..].trim_start_matches([' ', '\t']).len();
                Ok(self.token(Kind::Newline, line_break + indent))
            }
            _ => Err(self.unmatched()),
        }
    }
}

/// The length of a Pure string, `'…'` with backslash escapes, at the start
/// of `text`.
fn quoted_string_length(text: &str) -> Option<usize> {
    let mut chars = text.char_indices().skip(1);
    while let Some((i, c)) = chars.next() {
        match c {
            '\'' => return Some(i + 1),
            '\\' => {
                chars.next()?;
            }
            '\r' | '\n' => return None,
            _ => {}
        }
    }
    None
}

/// The length of `QUOTED_ROW_VALUE` at the start of `text`, if it matches:
/// the longest match, a doubled quote being content where that goes further.
fn quoted_row_value_length(text: &str) -> Option<usize> {
    let open = text.len() - text.trim_start_matches([' ', '\t']).len();
    if !text[open..].starts_with('"') {
        return None;
    }
    let mut end = None;
    let mut i = open + 1;
    while let Some(c) = text[i..].chars().next() {
        match c {
            '"' if text[i + 1..].starts_with('"') => {
                // Closing here matches; going on past `""` may match further.
                end = Some(i + 1);
                i += 2;
            }
            '"' => {
                end = Some(i + 1);
                break;
            }
            '\r' | '\n' => break,
            _ => i += c.len_utf8(),
        }
    }
    let end = end?;
    Some(end + (text[end..].len() - text[end..].trim_start_matches([' ', '\t']).len()))
}

/// Maps a text's own positions to the document's.
#[derive(Debug, Clone, Copy)]
struct Positions<'a> {
    origin: Origin,
    source: &'a str,
}

impl Positions<'_> {
    /// The document position of the text's `line` (1-based) and `column`
    /// (1-based).
    fn document(&self, line: u32, column: u32) -> (u32, u32) {
        if line == 1 {
            (self.origin.line, self.origin.column - 1 + column)
        } else {
            (self.origin.line + line - 1, column)
        }
    }

    /// From `start`'s first character to `end`'s last, where ANTLR puts
    /// them: a token's end is its start plus its UTF-16 length, on its first
    /// line.
    fn span(&self, start: &Token<'_>, end: &Token<'_>) -> SourceInfo {
        let (start_line, start_column) = self.document(start.line, start.column + 1);
        let length = u32::try_from(end.text.encode_utf16().count()).unwrap_or(u32::MAX);
        let (end_line, end_column) = self.document(end.line, end.column + length);
        SourceInfo::new(self.source, start_line, start_column, end_line, end_column)
    }

    fn span_at(&self, line: u32, column: u32) -> SourceInfo {
        let (line, column) = self.document(line, column + 1);
        SourceInfo::new(self.source, line, column, line, column)
    }
}

struct RelationDataParser<'a> {
    tokens: Vec<Token<'a>>,
    next: usize,
    positions: Positions<'a>,
}

impl<'a> RelationDataParser<'a> {
    fn peek(&self) -> Token<'a> {
        self.tokens[self.next.min(self.tokens.len() - 1)]
    }

    fn advance(&mut self) -> Token<'a> {
        let token = self.peek();
        if token.kind != Kind::Eof {
            self.next += 1;
        }
        token
    }

    fn error(&self, token: &Token<'_>) -> ParseError {
        let found = if token.kind == Kind::Eof {
            "the end of the data".to_string()
        } else {
            format!("'{}'", token.text)
        };
        ParseError::unexpected(
            format!("Unexpected {found} in relation data"),
            self.positions.span(token, token),
        )
    }

    fn expect(&mut self, kind: Kind) -> Result<Token<'a>, ParseError> {
        let token = self.peek();
        if token.kind == kind {
            Ok(self.advance())
        } else {
            Err(self.error(&token))
        }
    }

    /// `definition: relationElement* EOF`, positioned from its first token
    /// to `<EOF>`.
    fn definition(&mut self) -> Result<RelationData, ParseError> {
        let first = self.peek();
        let mut elements = Vec::new();
        while self.peek().kind != Kind::Eof {
            elements.push(self.relation_element()?);
        }
        let eof = self.peek();
        Ok(RelationData {
            elements,
            source_info: self.positions.span(&first, &eof),
        })
    }

    fn relation_element(&mut self) -> Result<RelationElement, ParseError> {
        let start = self.peek();
        let mut paths = Vec::new();
        match start.kind {
            Kind::TableStart => {
                self.advance();
                while self.peek().kind == Kind::Newline {
                    self.advance();
                }
            }
            Kind::Identifier | Kind::QuotedIdentifier => {
                paths.push(self.identifier()?);
                while self.peek().kind == Kind::Dot {
                    self.advance();
                    paths.push(self.identifier()?);
                }
                self.expect(Kind::TableStart)?;
            }
            _ => return Err(self.error(&start)),
        }
        let columns = self.cells();
        let mut rows = Vec::new();
        while self.peek().kind == Kind::Newline {
            self.advance();
            rows.push(self.cells());
        }
        let end = self.expect(Kind::TableEnd)?;
        Ok(RelationElement {
            paths,
            columns,
            rows,
            source_info: self.positions.span(&start, &end),
        })
    }

    fn identifier(&mut self) -> Result<SmolStr, ParseError> {
        let token = self.peek();
        match token.kind {
            Kind::Identifier => {
                self.advance();
                Ok(SmolStr::new(token.text))
            }
            Kind::QuotedIdentifier => {
                self.advance();
                let (value, _) = string_value(token.text).map_err(|message| {
                    ParseError::unexpected(message, self.positions.span(&token, &token))
                })?;
                Ok(SmolStr::new(value))
            }
            _ => Err(self.error(&token)),
        }
    }

    /// `cell (ROW_COMMA cell)*`, each cell trimmed; a quoted cell keeps its
    /// quotes.
    fn cells(&mut self) -> Vec<SmolStr> {
        let mut cells = vec![self.cell()];
        while self.peek().kind == Kind::RowComma {
            self.advance();
            cells.push(self.cell());
        }
        cells
    }

    fn cell(&mut self) -> SmolStr {
        match self.peek().kind {
            Kind::RowValue | Kind::QuotedRowValue => SmolStr::new(java_trim(self.advance().text)),
            _ => SmolStr::default(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn parse_at_origin(text: &str) -> RelationData {
        parse(text, Origin { line: 1, column: 1 }, "t").expect("parses")
    }

    #[test]
    fn tables_with_and_without_paths() {
        let data = parse_at_origin("\n  a.'b c'.d:\n  x, y\n  1, \"2, 3\" ;\n  :z\n  4;\n");
        assert_eq!(data.elements.len(), 2);
        let first = &data.elements[0];
        assert_eq!(first.paths, ["a", "b c", "d"]);
        assert_eq!(first.columns, ["x", "y"]);
        assert_eq!(first.rows, [vec!["1", "\"2, 3\""]]);
        let second = &data.elements[1];
        assert!(second.paths.is_empty());
        assert_eq!(second.columns, ["z"]);
        assert_eq!(second.rows, [vec!["4"]]);
    }

    #[test]
    fn empty_cells_and_rows_are_kept() {
        let data = parse_at_origin(":a,,b\n,\n;");
        let element = &data.elements[0];
        assert_eq!(element.columns, ["a", "", "b"]);
        // `,` is two empty cells; the line break before `;` starts a row.
        assert_eq!(element.rows, [vec!["", ""], vec![""]]);
    }

    #[test]
    fn a_doubled_quote_is_content() {
        let data = parse_at_origin(":\"a\"\"b\" , c;");
        assert_eq!(data.elements[0].columns, ["\"a\"\"b\"", "c"]);
    }

    #[test]
    fn positions_follow_the_engine() {
        // The text starts at line 3, column 7 of the document.
        let origin = Origin { line: 3, column: 7 };
        let data = parse("\n  t: a\n  1;\n  ", origin, "t").expect("parses");
        let element = &data.elements[0];
        // `t` to `;`.
        assert_eq!(
            (
                element.source_info.start_line,
                element.source_info.start_column
            ),
            (4, 3)
        );
        assert_eq!(
            (element.source_info.end_line, element.source_info.end_column),
            (5, 4)
        );
        // The data ends at `<EOF>`: its column plus five.
        assert_eq!(
            (data.source_info.end_line, data.source_info.end_column),
            (6, 7)
        );
    }

    #[test]
    fn a_table_needs_its_semicolon() {
        let error = parse(":a\n1", Origin { line: 1, column: 1 }, "t").expect_err("no `;`");
        assert_eq!(error.source_info().map(|si| si.start_line), Some(2));
    }

    #[test]
    fn integers_and_booleans_are_not_identifiers() {
        for text in ["1:a;", "true:a;", "a.2:b;"] {
            assert!(
                parse(text, Origin { line: 1, column: 1 }, "t").is_err(),
                "{text}"
            );
        }
    }
}
