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

//! Hand-written tokenizer for the Pure grammar.
//!
//! The lexer operates on a UTF-8 string and produces a `Vec<Token>` with source spans.
//! It handles:
//! - Keywords and identifiers (including quoted `'with spaces'`)
//! - Numeric literals (integer, float, decimal with `D` suffix)
//! - String literals (single-quoted with `\'` escape)
//! - Date/time literals (`%2024-01-15`, `%10:30:00`)
//! - Section headers (`###Pure`)
//! - Island grammar markers (`#{`, `}#`)
//! - Comments (`//` line, `/* */` block)

use legend_pure_parser_ast::{SourceInfo, SourceOffset};
use smol_str::SmolStr;

use crate::LexError;
use crate::token::{Token, TokenKind};

/// Tokenizes Pure source text into a vector of tokens.
///
/// Skips whitespace and comments. The returned token stream always ends
/// with [`TokenKind::Eof`].
///
/// # Errors
///
/// Returns `Err` for malformed tokens (unterminated strings, invalid characters).
pub fn tokenize(source: &str, source_name: &str) -> Result<Vec<Token>, LexError> {
    tokenize_with_offset(source, source_name, SourceOffset::default())
}

/// Tokenizes `source` as a fragment of a larger document.
///
/// Every token and error position is reported in document coordinates,
/// shifted by `offset` (see [`SourceOffset`]). This is how Legend Engine
/// section bodies are lexed: the engine hands over the text after a
/// `###Section` header together with the header's line offset.
///
/// # Errors
///
/// Returns `Err` for malformed tokens (unterminated strings, invalid characters).
pub fn tokenize_with_offset(
    source: &str,
    source_name: &str,
    offset: SourceOffset,
) -> Result<Vec<Token>, LexError> {
    let mut lexer = Lexer::new(source, source_name, offset);
    let mut tokens = Vec::new();

    loop {
        let token = lexer.next_token()?;
        let is_eof = token.kind == TokenKind::Eof;
        tokens.push(token);
        if is_eof {
            break;
        }
    }

    Ok(tokens)
}

/// Stateful cursor over source text.
struct Lexer<'a> {
    /// The full source text.
    source: &'a str,
    /// Source identifier (file name).
    source_name: SmolStr,
    /// Current byte offset in source.
    pos: usize,
    /// Current line (1-indexed).
    line: u32,
    /// Current column (1-indexed).
    column: u32,
    /// Where `source` sits in the enclosing document; applied to every span.
    offset: SourceOffset,
    /// Inside a navigation path (`#/…#`): its closing `#` is not an island.
    in_navigation_path: bool,
    /// An island's body and closer, lexed with its opener and returned next.
    pending: std::collections::VecDeque<Token>,
}

impl<'a> Lexer<'a> {
    fn new(source: &'a str, source_name: &str, offset: SourceOffset) -> Self {
        Self {
            source,
            source_name: SmolStr::new(source_name),
            pos: 0,
            line: 1,
            column: 1,
            offset,
            in_navigation_path: false,
            pending: std::collections::VecDeque::new(),
        }
    }

    /// Returns the current byte without advancing, or `None` at EOF.
    fn peek(&self) -> Option<char> {
        self.source[self.pos..].chars().next()
    }

    /// Returns the byte at `pos + offset` without advancing.
    fn peek_at(&self, offset: usize) -> Option<char> {
        self.source[self.pos..].chars().nth(offset)
    }

    /// Advances the cursor by one character.
    fn advance(&mut self) -> Option<char> {
        let ch = self.peek()?;
        self.pos += ch.len_utf8();
        if ch == '\n' {
            self.line += 1;
            self.column = 1;
        } else {
            self.column += 1;
        }
        Some(ch)
    }

    /// Returns true if the cursor has reached the end.
    fn is_at_end(&self) -> bool {
        self.pos >= self.source.len()
    }

    /// Creates a `SourceInfo` from a saved start position to the current
    /// position, in document coordinates (see [`SourceOffset`]).
    fn span_from(&self, start_line: u32, start_column: u32) -> SourceInfo {
        self.offset.apply(SourceInfo::new(
            self.source_name.clone(),
            start_line,
            start_column,
            self.line,
            // column is 1 past the last char, so subtract 1 for inclusive end
            if self.column > 1 {
                self.column - 1
            } else {
                self.column
            },
        ))
    }

    /// Extracts the text from `start_pos` to the current position.
    fn text_from(&self, start_pos: usize) -> SmolStr {
        SmolStr::new(&self.source[start_pos..self.pos])
    }

    fn skip_whitespace(&mut self) {
        while let Some(ch) = self.peek() {
            if ch.is_ascii_whitespace() {
                self.advance();
            } else {
                break;
            }
        }
    }

    fn skip_line_comment(&mut self) {
        while let Some(ch) = self.advance() {
            if ch == '\n' {
                break;
            }
        }
    }

    fn skip_block_comment(&mut self) -> Result<(), LexError> {
        let start_line = self.line;
        let start_col = self.column;
        // Skip past the opening /*
        self.advance(); // *
        loop {
            match self.advance() {
                Some('*') if self.peek() == Some('/') => {
                    self.advance(); // /
                    return Ok(());
                }
                Some(_) => {}
                Option::None => {
                    return Err(LexError::UnterminatedBlockComment {
                        source_info: self.span_from(start_line, start_col),
                    });
                }
            }
        }
    }

    fn skip_whitespace_and_comments(&mut self) -> Result<(), LexError> {
        loop {
            self.skip_whitespace();
            if self.peek() == Some('/') {
                match self.peek_at(1) {
                    Some('/') => {
                        self.advance(); // first /
                        self.advance(); // second /
                        self.skip_line_comment();
                    }
                    Some('*') => {
                        self.advance(); // /
                        self.skip_block_comment()?;
                    }
                    _ => break,
                }
            } else {
                break;
            }
        }
        Ok(())
    }

    /// Produces the next token, skipping whitespace and comments.
    #[allow(clippy::too_many_lines)]
    fn next_token(&mut self) -> Result<Token, LexError> {
        if let Some(token) = self.pending.pop_front() {
            return Ok(token);
        }
        self.skip_whitespace_and_comments()?;
        if self.at_island_open() {
            return self.lex_island();
        }

        if self.is_at_end() {
            return Ok(Token {
                kind: TokenKind::Eof,
                text: SmolStr::default(),
                source_info: self.span_from(self.line, self.column),
            });
        }

        let start_line = self.line;
        let start_col = self.column;
        let start_pos = self.pos;

        let Some(ch) = self.advance() else {
            unreachable!("advance() called after is_at_end() check returned false");
        };

        let kind = match ch {
            // -- Section header: ### --
            '#' if self.peek() == Some('#') && self.peek_at(1) == Some('#') => {
                self.advance(); // second #
                self.advance(); // third #
                // Read the section name (identifier chars)
                while let Some(c) = self.peek() {
                    if c.is_alphanumeric() || c == '_' {
                        self.advance();
                    } else {
                        break;
                    }
                }
                TokenKind::SectionHeader
            }

            // -- Navigation path opener: #/ --
            //    Emitted as a single token so the inline path
            //    parser doesn't have to peek across `Hash` +
            //    `Slash`, and so a trailing `/` inside the body
            //    keeps lexing as `Slash`. Section headers (`###`)
            //    and `#{` are matched above and never reach this
            //    arm.
            '#' if self.peek() == Some('/') => {
                self.advance();
                self.in_navigation_path = true;
                TokenKind::HashSlash
            }

            // -- Single # — opener for tagged islands (`#TDS`,
            //    `#>`, `#sql`) and closer for raw-content islands
            //    (and for navigation paths). Section headers
            //    (`###`), `#{`, and `#/` are matched above and
            //    never reach this arm.
            '#' => {
                self.in_navigation_path = false;
                TokenKind::Hash
            }

            // -- Delimiters --
            '(' => TokenKind::LParen,
            ')' => TokenKind::RParen,
            '{' => TokenKind::LBrace,
            '}' if self.peek() == Some('#') => {
                self.advance();
                TokenKind::RBraceHash
            }
            '}' => TokenKind::RBrace,
            '[' => TokenKind::LBracket,
            ']' => TokenKind::RBracket,
            ';' => TokenKind::Semicolon,
            ',' => TokenKind::Comma,
            '.' => TokenKind::Dot,
            '@' => TokenKind::At,
            '$' => TokenKind::Dollar,
            '~' if self.peek() == Some('~') && self.peek_at(1) == Some('~') => {
                self.advance();
                self.advance();
                TokenKind::TildeTildeTilde
            }
            '~' => TokenKind::Tilde,
            '+' => TokenKind::Plus,
            '*' => TokenKind::Star,

            // -- : or :: --
            ':' if self.peek() == Some(':') => {
                self.advance();
                TokenKind::PathSep
            }
            ':' => TokenKind::Colon,

            // -- - or -> --
            '-' if self.peek() == Some('>') => {
                self.advance();
                TokenKind::Arrow
            }
            '-' => TokenKind::Minus,

            // -- = or == or => --
            '=' if self.peek() == Some('=') => {
                self.advance();
                TokenKind::EqualEqual
            }
            '=' if self.peek() == Some('>') => {
                self.advance();
                TokenKind::FatArrow
            }
            '=' => TokenKind::Equals,

            // -- ! or != --
            '!' if self.peek() == Some('=') => {
                self.advance();
                TokenKind::BangEqual
            }
            '!' => TokenKind::Bang,

            // -- < or <= or << or <<< --
            '<' if self.peek() == Some('<') && self.peek_at(1) == Some('<') => {
                self.advance();
                self.advance();
                TokenKind::LessLessLess
            }
            '<' if self.peek() == Some('=') => {
                self.advance();
                TokenKind::LessEqual
            }
            '<' if self.peek() == Some('<') => {
                self.advance();
                TokenKind::LessLess
            }
            '<' => TokenKind::Less,

            // -- > or >= or >> or >>> --
            '>' if self.peek() == Some('>') && self.peek_at(1) == Some('>') => {
                self.advance();
                self.advance();
                TokenKind::GreaterGreaterGreater
            }
            '>' if self.peek() == Some('=') => {
                self.advance();
                TokenKind::GreaterEqual
            }
            '>' if self.peek() == Some('>') => {
                self.advance();
                TokenKind::GreaterGreater
            }
            '>' => TokenKind::Greater,

            // -- & or && or &&& --
            '&' if self.peek() == Some('&') && self.peek_at(1) == Some('&') => {
                self.advance();
                self.advance();
                TokenKind::AmpAmpAmp
            }
            '&' if self.peek() == Some('&') => {
                self.advance();
                TokenKind::AmpAmp
            }
            '&' => TokenKind::Amp,

            // -- | or || or ||| --
            '|' if self.peek() == Some('|') && self.peek_at(1) == Some('|') => {
                self.advance();
                self.advance();
                TokenKind::PipePipePipe
            }
            '|' if self.peek() == Some('|') => {
                self.advance();
                TokenKind::PipePipe
            }
            '|' => TokenKind::Pipe,

            // -- ^ or ^^^ --
            '^' if self.peek() == Some('^') && self.peek_at(1) == Some('^') => {
                self.advance();
                self.advance();
                TokenKind::CaretCaretCaret
            }
            '^' => TokenKind::Caret,

            // -- / (slash, comments already handled above) --
            '/' => TokenKind::Slash,

            // -- Text block: '''<newline> … ''' (a multi-line string) --
            '\'' if self.at_text_block() => self.lex_text_block(start_line, start_col)?,

            // -- String literal: 'hello' with \' escapes --
            '\'' => self.lex_string(start_line, start_col, '\'')?,

            // -- String literal: "hello" with \" escapes (used by TDS
            //    cell rows for JSON-like values: `1, "[1,2,3]"`). Both
            //    quote styles produce the same StringLiteral kind so
            //    the parser doesn't need to disambiguate; the cell raw
            //    text preserves whichever quotes were used. --
            '"' => self.lex_string(start_line, start_col, '"')?,

            // -- Wildcard / subtype-constraint operators in type
            //    expressions. `?` is the column-name wildcard in
            //    `ColSpec<(?:Type)>`; `⊆` (U+2286) is the subtype
            //    constraint in `<X⊆T>`. Both are emitted as their
            //    own token kinds so the parser dispatches them
            //    explicitly. --
            '?' => TokenKind::Question,
            '\u{2286}' => TokenKind::Subset,

            // -- Date literal: %2024-01-15, %10:30:00 --
            '%' => self.lex_date_or_percent(start_line, start_col),

            // -- Numeric literal: 42, 3.14, 1.0D --
            c if c.is_ascii_digit() => self.lex_number(start_pos),

            // -- Identifier or keyword --
            c if is_ident_start(c) => self.lex_identifier(start_pos),

            _ => {
                return Err(LexError::UnexpectedCharacter {
                    ch,
                    source_info: self.span_from(start_line, start_col),
                });
            }
        };

        let text = self.text_from(start_pos);
        let source_info = self.token_span(start_line, start_col, &text);
        Ok(Token {
            kind,
            text,
            source_info,
        })
    }

    /// Whether an island opens here: `#`, any text but `#` and `{`, then `{`
    /// (the engine lexer's `ISLAND_OPEN`), and neither a section header
    /// (`###`), a navigation path (`#/`) nor a path's closing `#`.
    fn at_island_open(&self) -> bool {
        let rest = &self.source[self.pos..];
        if !rest.starts_with('#') || rest.starts_with("###") || rest.starts_with("#/") {
            return false;
        }
        !self.in_navigation_path
            && rest[1..]
                .find(['#', '{'])
                .is_some_and(|i| rest[1 + i..].starts_with('{'))
    }

    /// An island, as the engine lexer's island mode reads it: the opener
    /// `#tag{` (returned), its body up to the `}#` that closes it, `#{ … }#`
    /// pairs nested, and that `}#` (both queued).
    fn lex_island(&mut self) -> Result<Token, LexError> {
        let (start_line, start_col, start_pos) = (self.line, self.column, self.pos);
        while self.advance().is_some_and(|c| c != '{') {}
        let opener_text = self.text_from(start_pos);
        let opener = Token {
            source_info: self.token_span(start_line, start_col, &opener_text),
            kind: TokenKind::IslandOpen,
            text: opener_text,
        };
        let (body_line, body_col, body_pos) = (self.line, self.column, self.pos);
        let mut depth = 1u32;
        loop {
            let rest = &self.source[self.pos..];
            if rest.starts_with("}#") {
                depth -= 1;
                if depth == 0 {
                    break;
                }
                self.advance();
                self.advance();
            } else if rest.starts_with("#{") {
                depth += 1;
                self.advance();
                self.advance();
            } else if self.advance().is_none() {
                return Err(LexError::UnterminatedIsland {
                    source_info: opener.source_info,
                });
            }
        }
        // The body is positioned on its first character; its grammar lexes
        // it again from there.
        let body = Token {
            kind: TokenKind::IslandContent,
            text: self.text_from(body_pos),
            source_info: self.offset.apply(SourceInfo::new(
                self.source_name.clone(),
                body_line,
                body_col,
                self.line,
                self.column,
            )),
        };
        let (close_line, close_col) = (self.line, self.column);
        self.advance();
        self.advance();
        self.pending.push_back(body);
        self.pending.push_back(Token {
            kind: TokenKind::RBraceHash,
            text: SmolStr::new("}#"),
            source_info: self.token_span(close_line, close_col, "}#"),
        });
        Ok(opener)
    }

    /// A token's span as ANTLR (and so the engine) reports it: the end is
    /// the start column plus the token's UTF-16 length, on the start line.
    /// For a token spanning lines (a text block, a string with line breaks)
    /// that is past the end of its first line, not where it ends; spans that
    /// end on such a token inherit that, as they do in the engine.
    fn token_span(&self, start_line: u32, start_col: u32, text: &str) -> SourceInfo {
        let length = u32::try_from(text.encode_utf16().count()).unwrap_or(u32::MAX);
        self.offset.apply(SourceInfo::new(
            self.source_name.clone(),
            start_line,
            start_col,
            start_line,
            (start_col - 1).saturating_add(length),
        ))
    }

    fn lex_string(
        &mut self,
        start_line: u32,
        start_col: u32,
        quote: char,
    ) -> Result<TokenKind, LexError> {
        loop {
            match self.advance() {
                Some('\\') => {
                    // Skip escaped character
                    self.advance();
                }
                Some(c) if c == quote => return Ok(TokenKind::StringLiteral),
                Some(_) => {}
                Option::None => {
                    return Err(LexError::UnterminatedString {
                        source_info: self.span_from(start_line, start_col),
                    });
                }
            }
        }
    }

    /// Just past an opening `'`: whether this is a text block, i.e. `'''`
    /// followed by optional spaces or tabs and a line terminator. `'''abc'''`
    /// and `''''''` are not text blocks (they lex as ordinary strings), as in
    /// the engine grammar's `TextBlock` fragment.
    fn at_text_block(&self) -> bool {
        let Some(rest) = self.source[self.pos..].strip_prefix("''") else {
            return false;
        };
        let rest = rest.trim_start_matches([' ', '\t']);
        rest.starts_with('\n') || rest.starts_with("\r\n")
    }

    /// Scans a text block to its closing `'''` (the first one: no escapes
    /// apply to the delimiter). The token text is the whole block, quotes
    /// included; the parser derives its value.
    fn lex_text_block(&mut self, start_line: u32, start_col: u32) -> Result<TokenKind, LexError> {
        // Unterminated, the engine's lexer takes `''` for an empty string
        // and fails on the third quote; so does this one.
        if !self.source[self.pos + 2..].contains("'''") {
            self.advance();
            return Ok(TokenKind::StringLiteral);
        }
        self.advance();
        self.advance();
        while !self.source[self.pos..].starts_with("'''") {
            if self.advance().is_none() {
                return Err(LexError::UnterminatedString {
                    source_info: self.span_from(start_line, start_col),
                });
            }
        }
        for _ in 0..3 {
            self.advance();
        }
        Ok(TokenKind::StringLiteral)
    }

    fn lex_date_or_percent(&mut self, _start_line: u32, _start_col: u32) -> TokenKind {
        // Date literal: %YYYY-MM-DD or %HH:MM:SS or %YYYY-MM-DDTHH:MM:SS
        // Also: %-YYYY-MM-DD for negative (BCE) years
        // Also: %latest — the milestoning sentinel for "the latest version"
        // If no digit/minus/`latest` follows %, it's just a percent operator
        let rest = &self.source[self.pos..];
        if rest.starts_with("latest")
            && rest
                .as_bytes()
                .get(6)
                .is_none_or(|c| !c.is_ascii_alphanumeric() && *c != b'_')
        {
            // Consume the 6 chars of "latest" so the lexed token text
            // round-trips as `%latest`. Use char advance for span/col tracking.
            for _ in 0..6 {
                self.advance();
            }
            return TokenKind::DateLiteral;
        }
        let starts_date = self.peek().is_some_and(|c| c.is_ascii_digit())
            || (self.peek() == Some('-')
                && self.source[self.pos..]
                    .chars()
                    .nth(1)
                    .is_some_and(|c| c.is_ascii_digit()));
        if starts_date {
            // Consume optional leading minus for negative year
            if self.peek() == Some('-') {
                self.advance();
            }
            while let Some(c) = self.peek() {
                if c.is_ascii_digit() || c == 'T' || c == ':' || c == '.' || c == '+' || c == 'Z' {
                    self.advance();
                } else if c == '-' {
                    // Only consume '-' if followed by a digit (date segment separator).
                    // Do NOT consume '-' when followed by '>' (arrow operator ->).
                    if self.source[self.pos..]
                        .chars()
                        .nth(1)
                        .is_some_and(|n| n.is_ascii_digit())
                    {
                        self.advance();
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            TokenKind::DateLiteral
        } else {
            TokenKind::Percent
        }
    }

    fn lex_number(&mut self, start_pos: usize) -> TokenKind {
        let kind = self.lex_numeric_literal();
        // ANTLR's longest match: a run of letters, digits, `_` and `$` that
        // starts with a digit is an identifier (`VALID_STRING`) when it is
        // longer than the number it starts with, e.g. `360_ACT` or `12Dx`.
        let number = &self.source[start_pos..self.pos];
        if number.chars().all(is_ident_continue) && self.peek().is_some_and(is_ident_continue) {
            while self.peek().is_some_and(is_ident_continue) {
                self.advance();
            }
            return TokenKind::Identifier;
        }
        kind
    }

    fn lex_numeric_literal(&mut self) -> TokenKind {
        // Consume leading digits
        while self.peek().is_some_and(|c| c.is_ascii_digit()) {
            self.advance();
        }

        let mut is_float = false;

        // Check for decimal point
        if self.peek() == Some('.') && self.peek_at(1).is_some_and(|c| c.is_ascii_digit()) {
            is_float = true;
            self.advance(); // consume '.'
            while self.peek().is_some_and(|c| c.is_ascii_digit()) {
                self.advance();
            }
        }

        // Check for exponent: e/E followed by optional +/- and digits
        // Examples: 1e6, 134.21e-10, 1.3421E+8, 0.13421e-7
        if matches!(self.peek(), Some('e' | 'E')) {
            let has_exponent_digits = match self.peek_at(1) {
                Some(c) if c.is_ascii_digit() => true,
                Some('+' | '-') => self.peek_at(2).is_some_and(|c| c.is_ascii_digit()),
                _ => false,
            };
            if has_exponent_digits {
                is_float = true;
                self.advance(); // consume 'e'/'E'
                // consume optional sign
                if matches!(self.peek(), Some('+' | '-')) {
                    self.advance();
                }
                // consume exponent digits
                while self.peek().is_some_and(|c| c.is_ascii_digit()) {
                    self.advance();
                }
            }
        }

        // Check for decimal suffix 'D'/'d' — applies to both integers and floats
        // Examples: 1.0D, 1.5d, 9999999999999992d
        if matches!(self.peek(), Some('D' | 'd')) {
            self.advance();
            return TokenKind::DecimalLiteral;
        }

        if is_float {
            return TokenKind::FloatLiteral;
        }

        TokenKind::IntegerLiteral
    }

    fn lex_identifier(&mut self, start_pos: usize) -> TokenKind {
        while self.peek().is_some_and(is_ident_continue) {
            self.advance();
        }

        let text = &self.source[start_pos..self.pos];
        TokenKind::keyword(text).unwrap_or(TokenKind::Identifier)
    }
}

/// Returns true if `ch` can start an identifier.
fn is_ident_start(ch: char) -> bool {
    ch.is_ascii_alphabetic() || ch == '_'
}

/// Returns true if `ch` can continue an identifier.
///
/// Includes `$` to support mangled function names (e.g., `func_$0_1$_`).
fn is_ident_continue(ch: char) -> bool {
    ch.is_ascii_alphanumeric() || ch == '_' || ch == '$'
}

#[cfg(test)]
mod tests {
    use super::*;

    fn kinds(source: &str) -> Vec<TokenKind> {
        tokenize(source, "test.pure")
            .unwrap()
            .into_iter()
            .map(|t| t.kind)
            .collect()
    }

    fn texts(source: &str) -> Vec<String> {
        tokenize(source, "test.pure")
            .unwrap()
            .into_iter()
            .map(|t| t.text.to_string())
            .collect()
    }

    // -- Section headers --

    #[test]
    fn section_header() {
        let tokens = tokenize("###Pure", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::SectionHeader);
        assert_eq!(tokens[0].text, "###Pure");
    }

    #[test]
    fn section_header_mapping() {
        let tokens = tokenize("###Mapping", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::SectionHeader);
        assert_eq!(tokens[0].text, "###Mapping");
    }

    // -- Keywords --

    #[test]
    fn keywords() {
        assert_eq!(
            kinds("Class Enum Profile Association Measure function"),
            vec![
                TokenKind::Class,
                TokenKind::Enum,
                TokenKind::Profile,
                TokenKind::Association,
                TokenKind::Measure,
                TokenKind::Function,
                TokenKind::Eof,
            ]
        );
    }

    #[test]
    fn other_keywords() {
        assert_eq!(
            kinds("import extends let native true false"),
            vec![
                TokenKind::Import,
                TokenKind::Extends,
                TokenKind::Let,
                TokenKind::Native,
                TokenKind::True,
                TokenKind::False,
                TokenKind::Eof,
            ]
        );
    }

    #[test]
    fn profile_body_keywords() {
        assert_eq!(
            kinds("stereotypes tags"),
            vec![TokenKind::Stereotypes, TokenKind::Tags, TokenKind::Eof]
        );
    }

    // -- Identifiers --

    #[test]
    fn identifier() {
        let tokens = tokenize("myVar _private FOO_BAR", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::Identifier);
        assert_eq!(tokens[0].text, "myVar");
        assert_eq!(tokens[1].kind, TokenKind::Identifier);
        assert_eq!(tokens[1].text, "_private");
        assert_eq!(tokens[2].kind, TokenKind::Identifier);
        assert_eq!(tokens[2].text, "FOO_BAR");
    }

    // -- String literals --

    #[test]
    fn string_literal() {
        let tokens = tokenize("'hello world'", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::StringLiteral);
        assert_eq!(tokens[0].text, "'hello world'");
    }

    #[test]
    fn string_with_escape() {
        let tokens = tokenize(r"'test1\'s'", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::StringLiteral);
        assert_eq!(tokens[0].text, r"'test1\'s'");
    }

    #[test]
    fn unterminated_string() {
        let result = tokenize("'unterminated", "test.pure");
        assert!(result.is_err());
    }

    // -- Numeric literals --

    #[test]
    fn an_unterminated_text_block_fails_at_its_third_quote() {
        let error = tokenize("x\n  '''\n  Hello\n}", "t.pure").unwrap_err();
        let si = error.source_info();
        assert_eq!((si.start_line, si.start_column), (2, 5));
    }

    #[test]
    fn digit_led_names_longer_than_the_number_are_identifiers() {
        // ANTLR's longest match between INTEGER / DECIMAL and VALID_STRING.
        let kinds = |src: &str| -> Vec<(TokenKind, String)> {
            tokenize(src, "t.pure")
                .unwrap()
                .into_iter()
                .filter(|t| t.kind != TokenKind::Eof)
                .map(|t| (t.kind, t.text.to_string()))
                .collect()
        };
        assert_eq!(
            kinds("360_ACT"),
            vec![(TokenKind::Identifier, "360_ACT".to_string())]
        );
        assert_eq!(
            kinds("12Dx")[0],
            (TokenKind::Identifier, "12Dx".to_string())
        );
        // A tie keeps the number; a `.` ends the name.
        assert_eq!(kinds("12D")[0].0, TokenKind::DecimalLiteral);
        assert_eq!(
            kinds("1.5x")[0],
            (TokenKind::FloatLiteral, "1.5".to_string())
        );
        assert_eq!(
            kinds("[0..1]")[1],
            (TokenKind::IntegerLiteral, "0".to_string())
        );
    }

    #[test]
    fn integer_literal() {
        let tokens = tokenize("42", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::IntegerLiteral);
        assert_eq!(tokens[0].text, "42");
    }

    #[test]
    fn float_literal() {
        let tokens = tokenize("3.14", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::FloatLiteral);
        assert_eq!(tokens[0].text, "3.14");
    }

    #[test]
    fn float_with_exponent() {
        // 134.21e-10 — negative exponent
        let tokens = tokenize("134.21e-10", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::FloatLiteral);
        assert_eq!(tokens[0].text, "134.21e-10");
    }

    #[test]
    fn float_with_positive_exponent() {
        let tokens = tokenize("1.5E+3", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::FloatLiteral);
        assert_eq!(tokens[0].text, "1.5E+3");
    }

    #[test]
    fn float_with_unsigned_exponent() {
        let tokens = tokenize("0.13421e7", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::FloatLiteral);
        assert_eq!(tokens[0].text, "0.13421e7");
    }

    #[test]
    fn integer_with_exponent() {
        // 1e6 — integer base with exponent becomes float
        let tokens = tokenize("1e6", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::FloatLiteral);
        assert_eq!(tokens[0].text, "1e6");
    }

    #[test]
    fn exponent_in_expression() {
        // 134.21e-10->toString() — must not eat the ->
        let toks = texts("134.21e-10->toString");
        assert_eq!(toks[0], "134.21e-10");
        assert_eq!(toks[1], "->");
        assert_eq!(toks[2], "toString");
    }

    #[test]
    fn exponent_not_confused_with_identifier() {
        // 'e' alone after a float without digits is NOT an exponent
        // 3.14end should be float 3.14 then identifier 'end'
        let toks = texts("3.14end");
        assert_eq!(toks[0], "3.14");
        assert_eq!(toks[1], "end");
    }

    #[test]
    fn decimal_literal() {
        let tokens = tokenize("1.0D", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::DecimalLiteral);
        assert_eq!(tokens[0].text, "1.0D");
    }

    #[test]
    fn integer_decimal_literal() {
        // Integer with 'd' suffix is a decimal literal
        let tokens = tokenize("9999999999999992d", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::DecimalLiteral);
        assert_eq!(tokens[0].text, "9999999999999992d");
    }

    // -- Date literals --

    #[test]
    fn date_literal_strict_date() {
        let tokens = tokenize("%2024-01-15", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::DateLiteral);
        assert_eq!(tokens[0].text, "%2024-01-15");
    }

    #[test]
    fn date_literal_datetime() {
        let tokens = tokenize("%2024-01-15T10:30:00", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::DateLiteral);
        assert_eq!(tokens[0].text, "%2024-01-15T10:30:00");
    }

    #[test]
    fn date_literal_strict_time() {
        let tokens = tokenize("%10:30:00", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::DateLiteral);
        assert_eq!(tokens[0].text, "%10:30:00");
    }

    // -- Operators --

    #[test]
    fn two_char_operators() {
        assert_eq!(
            kinds("-> :: == != <= >= << >> && || =>"),
            vec![
                TokenKind::Arrow,
                TokenKind::PathSep,
                TokenKind::EqualEqual,
                TokenKind::BangEqual,
                TokenKind::LessEqual,
                TokenKind::GreaterEqual,
                TokenKind::LessLess,
                TokenKind::GreaterGreater,
                TokenKind::AmpAmp,
                TokenKind::PipePipe,
                TokenKind::FatArrow,
                TokenKind::Eof,
            ]
        );
    }

    #[test]
    fn triple_char_operators() {
        assert_eq!(
            kinds("&&& ||| ^^^ <<< >>> ~~~"),
            vec![
                TokenKind::AmpAmpAmp,
                TokenKind::PipePipePipe,
                TokenKind::CaretCaretCaret,
                TokenKind::LessLessLess,
                TokenKind::GreaterGreaterGreater,
                TokenKind::TildeTildeTilde,
                TokenKind::Eof,
            ]
        );
    }

    #[test]
    fn triple_vs_double_operators() {
        // Ensure longest match wins
        assert_eq!(
            kinds("&&&&"),
            vec![TokenKind::AmpAmpAmp, TokenKind::Amp, TokenKind::Eof]
        );
        assert_eq!(
            kinds("||||"),
            vec![TokenKind::PipePipePipe, TokenKind::Pipe, TokenKind::Eof]
        );
        assert_eq!(
            kinds("^^^^"),
            vec![TokenKind::CaretCaretCaret, TokenKind::Caret, TokenKind::Eof]
        );
        assert_eq!(
            kinds("<<<<"),
            vec![TokenKind::LessLessLess, TokenKind::Less, TokenKind::Eof]
        );
        assert_eq!(
            kinds(">>>>"),
            vec![
                TokenKind::GreaterGreaterGreater,
                TokenKind::Greater,
                TokenKind::Eof
            ]
        );
        assert_eq!(
            kinds("~~~~"),
            vec![TokenKind::TildeTildeTilde, TokenKind::Tilde, TokenKind::Eof]
        );
    }

    #[test]
    fn single_char_operators() {
        assert_eq!(
            kinds("+ - * / = ! < > & | ^ ~ @ $"),
            vec![
                TokenKind::Plus,
                TokenKind::Minus,
                TokenKind::Star,
                TokenKind::Slash,
                TokenKind::Equals,
                TokenKind::Bang,
                TokenKind::Less,
                TokenKind::Greater,
                TokenKind::Amp,
                TokenKind::Pipe,
                TokenKind::Caret,
                TokenKind::Tilde,
                TokenKind::At,
                TokenKind::Dollar,
                TokenKind::Eof,
            ]
        );
    }

    #[test]
    fn percent_vs_date() {
        // % followed by non-digit is the percent operator
        assert_eq!(
            kinds("% x"),
            vec![TokenKind::Percent, TokenKind::Identifier, TokenKind::Eof]
        );
        // % followed by digit starts a date literal
        assert_eq!(
            kinds("%2024-01-15"),
            vec![TokenKind::DateLiteral, TokenKind::Eof]
        );
    }

    #[test]
    fn date_followed_by_arrow() {
        // %2024-01-15->adjust() must NOT eat the - from ->
        let toks = texts("%2024-01-15->adjust");
        assert_eq!(toks[0], "%2024-01-15");
        assert_eq!(toks[1], "->");
    }

    #[test]
    fn negative_year_date() {
        let toks = texts("%-799997984-02-29");
        assert_eq!(toks[0], "%-799997984-02-29");
        assert_eq!(
            kinds("%-799997984-02-29"),
            vec![TokenKind::DateLiteral, TokenKind::Eof]
        );
    }

    // -- Delimiters --

    #[test]
    fn delimiters() {
        assert_eq!(
            kinds("( ) { } [ ] ; , : ."),
            vec![
                TokenKind::LParen,
                TokenKind::RParen,
                TokenKind::LBrace,
                TokenKind::RBrace,
                TokenKind::LBracket,
                TokenKind::RBracket,
                TokenKind::Semicolon,
                TokenKind::Comma,
                TokenKind::Colon,
                TokenKind::Dot,
                TokenKind::Eof,
            ]
        );
    }

    // -- Island markers --

    #[test]
    fn island_markers() {
        assert_eq!(
            kinds("#{ }#"),
            vec![
                TokenKind::IslandOpen,
                TokenKind::IslandContent,
                TokenKind::RBraceHash,
                TokenKind::Eof
            ]
        );
    }

    #[test]
    fn tagged_island_relation_store_accessor() {
        let tokens = tokenize("#>{ db::Store.col }#", "t.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::IslandOpen);
        assert_eq!(tokens[0].text, "#>{");
        assert_eq!(tokens[1].text, " db::Store.col ");
        assert_eq!(tokens[2].kind, TokenKind::RBraceHash);
    }

    #[test]
    fn an_island_body_is_raw_text() {
        // Relation data isn't Pure: `I'm John` must not open a string.
        let source = "Relation #{\n  1,I'm John,\"Doe, Jr\";\n}#;\n'next'";
        let tokens = tokenize(source, "t.pure").unwrap();
        let kinds: Vec<TokenKind> = tokens.iter().map(|t| t.kind).collect();
        assert_eq!(
            kinds,
            vec![
                TokenKind::Identifier,
                TokenKind::IslandOpen,
                TokenKind::IslandContent,
                TokenKind::RBraceHash,
                TokenKind::Semicolon,
                TokenKind::StringLiteral,
                TokenKind::Eof
            ]
        );
        assert_eq!(tokens[2].text, "\n  1,I'm John,\"Doe, Jr\";\n");
        // Positioned on its first character, right after `#{`.
        let si = &tokens[2].source_info;
        assert_eq!((si.start_line, si.start_column), (1, 12));
    }

    #[test]
    fn island_bodies_nest_hash_brace_pairs_only() {
        // Only `#{` opens a nested level, as in the engine's island mode:
        // `#Test{` inside a body is text, and the first `}#` closes it.
        let tokens = tokenize("#A{ x #{ y }# z }# #B{ #Test{ ok } w }#", "t.pure").unwrap();
        assert_eq!(tokens[1].text, " x #{ y }# z ");
        assert_eq!(tokens[4].text, " #Test{ ok } w ");
    }

    #[test]
    fn a_navigation_path_closer_is_not_an_island() {
        let tokens = tokenize("#/A/b# ->map(x|{$x})", "t.pure").unwrap();
        assert_eq!(tokens[4].kind, TokenKind::Hash);
        assert!(tokens.iter().all(|t| t.kind != TokenKind::IslandOpen));
    }

    #[test]
    fn an_unterminated_island_is_an_error() {
        let error = tokenize("x #{ a", "t.pure").unwrap_err();
        assert!(
            matches!(error, LexError::UnterminatedIsland { .. }),
            "{error:?}"
        );
        assert_eq!(error.source_info().start_column, 3);
    }

    #[test]
    fn tagged_island_tds_raw_body_terminator() {
        // `#TDS\n a, b\n 1, 2\n#` — `Hash` + `Identifier(TDS)` ...
        // closing `Hash`. The TDS parser scans tokens between the
        // two `Hash`es as raw body.
        assert_eq!(
            kinds("#TDS\n a, b\n#"),
            vec![
                TokenKind::Hash,
                TokenKind::Identifier,
                TokenKind::Identifier,
                TokenKind::Comma,
                TokenKind::Identifier,
                TokenKind::Hash,
                TokenKind::Eof,
            ]
        );
    }

    #[test]
    fn section_header_still_takes_precedence_over_hash() {
        // `###Foo` must still produce a single `SectionHeader`,
        // not `Hash` + `Hash` + `Hash` + identifier.
        assert_eq!(
            kinds("###Foo"),
            vec![TokenKind::SectionHeader, TokenKind::Eof]
        );
    }

    #[test]
    fn navigation_path_opener_is_atomic() {
        // `#/` is the dedicated path opener — must lex as a single
        // `HashSlash`, NOT `Hash` + `Slash`. Without this the inline
        // path parser would have to peek across two tokens just to
        // detect a path expression.
        assert_eq!(
            kinds("#/Person/name#"),
            vec![
                TokenKind::HashSlash,
                TokenKind::Identifier,
                TokenKind::Slash,
                TokenKind::Identifier,
                TokenKind::Hash,
                TokenKind::Eof,
            ]
        );
    }

    #[test]
    fn navigation_path_does_not_shadow_section_or_island() {
        // Order discipline: `###` > `#{` > `#/` > bare `#`. A run of
        // `###{/#}#` should parse as section-then-island-then-bare-hash,
        // never as `#/` smearing across the `###` boundary.
        assert_eq!(
            kinds("###Pure\n#{}#\n#/T/p#"),
            vec![
                TokenKind::SectionHeader,
                TokenKind::IslandOpen,
                TokenKind::IslandContent,
                TokenKind::RBraceHash,
                TokenKind::HashSlash,
                TokenKind::Identifier,
                TokenKind::Slash,
                TokenKind::Identifier,
                TokenKind::Hash,
                TokenKind::Eof,
            ]
        );
    }

    // -- Comments --

    #[test]
    fn line_comment() {
        assert_eq!(
            kinds("Class // comment\nEnum"),
            vec![TokenKind::Class, TokenKind::Enum, TokenKind::Eof]
        );
    }

    #[test]
    fn block_comment() {
        assert_eq!(
            kinds("Class /* block\ncomment */ Enum"),
            vec![TokenKind::Class, TokenKind::Enum, TokenKind::Eof]
        );
    }

    #[test]
    fn unterminated_block_comment() {
        let result = tokenize("/* unterminated", "test.pure");
        assert!(result.is_err());
    }

    // -- Source info --

    #[test]
    fn source_info_tracking() {
        let tokens = tokenize("Class\n  Person", "test.pure").unwrap();
        assert_eq!(tokens[0].source_info.start_line, 1);
        assert_eq!(tokens[0].source_info.start_column, 1);
        assert_eq!(tokens[0].source_info.end_column, 5);
        assert_eq!(tokens[1].source_info.start_line, 2);
        assert_eq!(tokens[1].source_info.start_column, 3);
    }

    // -- Full token stream for a real-ish input --

    #[test]
    fn profile_definition() {
        let source = "###Pure\nProfile my::doc\n{\n    stereotypes: [deprecated];\n}";
        let tks = texts(source);
        assert_eq!(
            tks,
            vec![
                "###Pure",
                "Profile",
                "my",
                "::",
                "doc",
                "{",
                "stereotypes",
                ":",
                "[",
                "deprecated",
                "]",
                ";",
                "}",
                ""
            ]
        );
    }

    #[test]
    fn class_with_stereotype() {
        let source = "<<temporal.bt>> Class my::P { name: String[1]; }";
        assert_eq!(
            kinds(source),
            vec![
                TokenKind::LessLess,
                TokenKind::Identifier, // temporal
                TokenKind::Dot,
                TokenKind::Identifier, // bt
                TokenKind::GreaterGreater,
                TokenKind::Class,
                TokenKind::Identifier, // my
                TokenKind::PathSep,
                TokenKind::Identifier, // P
                TokenKind::LBrace,
                TokenKind::Identifier, // name
                TokenKind::Colon,
                TokenKind::Identifier, // String
                TokenKind::LBracket,
                TokenKind::IntegerLiteral, // 1
                TokenKind::RBracket,
                TokenKind::Semicolon,
                TokenKind::RBrace,
                TokenKind::Eof,
            ]
        );
    }

    #[test]
    fn arrow_and_new_instance() {
        let source = "$x->cast(@Float); ^Cls(a='b')";
        assert_eq!(
            kinds(source),
            vec![
                TokenKind::Dollar,
                TokenKind::Identifier, // x
                TokenKind::Arrow,
                TokenKind::Identifier, // cast
                TokenKind::LParen,
                TokenKind::At,
                TokenKind::Identifier, // Float
                TokenKind::RParen,
                TokenKind::Semicolon,
                TokenKind::Caret,
                TokenKind::Identifier, // Cls
                TokenKind::LParen,
                TokenKind::Identifier, // a
                TokenKind::Equals,
                TokenKind::StringLiteral, // 'b'
                TokenKind::RParen,
                TokenKind::Eof,
            ]
        );
    }

    #[test]
    fn unexpected_character() {
        let result = tokenize("Class ¿", "test.pure");
        assert!(result.is_err());
    }

    #[test]
    fn lexes_double_quoted_string() {
        // TDS bodies use `"..."` for cell strings — both quote styles
        // produce StringLiteral so the parser treats them uniformly.
        let tokens = tokenize(r#""[1,2,3]""#, "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::StringLiteral);
        assert_eq!(tokens[0].text.as_str(), r#""[1,2,3]""#);
        // Single-quote form continues to work.
        let tokens = tokenize("'hello'", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::StringLiteral);
        assert_eq!(tokens[0].text.as_str(), "'hello'");
    }

    #[test]
    fn lexes_question_and_subset_operators() {
        // `?` is the wildcard column-name placeholder; `⊆` (U+2286) is
        // the subtype-constraint operator. Real shape:
        // `ColSpec<(?:Z)⊆T>` in
        // `core_functions_relation/.../eval.pure:18`.
        let tokens = tokenize("?\u{2286}", "test.pure").unwrap();
        assert_eq!(tokens[0].kind, TokenKind::Question);
        assert_eq!(tokens[1].kind, TokenKind::Subset);
    }

    #[test]
    fn test_offset_positions_are_in_document_coordinates() {
        let offset = SourceOffset::new(-1, 0).expect("valid");
        // Engine section body: starts with the newline that ended the header.
        let tokens = tokenize_with_offset("\nClass a::A", "doc.pure", offset).unwrap();
        let class = &tokens[0];
        assert_eq!(class.kind, TokenKind::Class);
        assert_eq!(
            (class.source_info.start_line, class.source_info.start_column),
            (1, 1)
        );
        assert_eq!(class.source_info.end_column, 5);
        assert_eq!(class.source_info.source, "doc.pure");
    }

    #[test]
    fn test_column_offset_applies_to_first_line_only() {
        let offset = SourceOffset::new(4, 10).expect("valid");
        let tokens = tokenize_with_offset("abc\ndef", "doc.pure", offset).unwrap();
        let first = &tokens[0].source_info;
        let second = &tokens[1].source_info;
        assert_eq!(
            (first.start_line, first.start_column, first.end_column),
            (5, 11, 13)
        );
        assert_eq!(
            (second.start_line, second.start_column, second.end_column),
            (6, 1, 3)
        );
    }

    #[test]
    fn test_lex_error_positions_are_offset() {
        let offset = SourceOffset::new(7, 0).expect("valid");
        let err = tokenize_with_offset("'unterminated", "doc.pure", offset).unwrap_err();
        assert_eq!(err.source_info().start_line, 8);
    }

    #[test]
    fn test_text_block_is_one_string_token() {
        let source = "x '''\n  hello\n  it's\n  ''' y";
        let tokens = tokenize(source, "t.pure").unwrap();
        assert_eq!(tokens[1].kind, TokenKind::StringLiteral);
        assert_eq!(tokens[1].text, "'''\n  hello\n  it's\n  '''");
        // ANTLR positions: the end is the start plus the whole token length,
        // on the start line (24 characters from column 3).
        let si = &tokens[1].source_info;
        assert_eq!(
            (si.start_line, si.start_column, si.end_line, si.end_column),
            (1, 3, 1, 26)
        );
        assert_eq!(tokens[2].text, "y");
    }

    #[test]
    fn test_triple_quotes_without_newline_are_ordinary_strings() {
        // `'''abc'''` is `''` `'abc'` `''`, as in the engine grammar.
        let tokens = tokenize("'''abc'''", "t.pure").unwrap();
        assert_eq!(tokens[0].text, "''");
        assert_eq!(tokens[1].text, "'abc'");
    }

    #[test]
    fn test_unterminated_text_block() {
        assert!(tokenize("'''\nnever closed", "t.pure").is_err());
    }
}
