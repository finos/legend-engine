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

//! Token types for the Pure grammar lexer.

use legend_pure_parser_ast::SourceInfo;
use smol_str::SmolStr;

/// A single token produced by the lexer.
#[derive(Debug, Clone, PartialEq)]
pub struct Token {
    /// The kind of token.
    pub kind: TokenKind,
    /// The raw text of the token.
    pub text: SmolStr,
    /// Source location.
    pub source_info: SourceInfo,
}

/// All token kinds in the Pure grammar.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum TokenKind {
    // -- Element keywords --
    /// `Class`
    Class,
    /// `Enum`
    Enum,
    /// `Profile`
    Profile,
    /// `Association`
    Association,
    /// `Measure`
    Measure,
    /// `Primitive`
    Primitive,
    /// `function`
    Function,

    // -- Other keywords --
    /// `import`
    Import,
    /// `extends`
    Extends,
    /// `let`
    Let,
    /// `native`
    Native,

    // -- Boolean literals --
    /// `true`
    True,
    /// `false`
    False,

    // -- Profile body keywords --
    /// `stereotypes`
    Stereotypes,
    /// `tags`
    Tags,

    // -- Aggregation keywords --
    /// `shared`
    Shared,
    /// `composite`
    Composite,
    /// `none`
    None,

    // -- Identifiers & literals --
    /// Regular identifier: `name`, `Person`, `myFunc`
    Identifier,
    /// Quoted identifier or string literal: `'hello'`, `'with spaces'`
    StringLiteral,
    /// Integer literal: `42`, `0`
    IntegerLiteral,
    /// Float literal: `3.14`
    FloatLiteral,
    /// Decimal literal: `1.0D`, `3.14D`
    DecimalLiteral,
    /// Date/time literal: `%2024-01-15`, `%2024-01-15T10:30:00`, `%10:30:00`
    DateLiteral,

    // -- Delimiters --
    /// `(`
    LParen,
    /// `)`
    RParen,
    /// `{`
    LBrace,
    /// `}`
    RBrace,
    /// `[`
    LBracket,
    /// `]`
    RBracket,
    /// `;`
    Semicolon,
    /// `,`
    Comma,
    /// `:`
    Colon,
    /// `.`
    Dot,

    // -- Operators --
    /// `+`
    Plus,
    /// `-`
    Minus,
    /// `*`
    Star,
    /// `/`
    Slash,
    /// `=`
    Equals,
    /// `==`
    EqualEqual,
    /// `!=`
    BangEqual,
    /// `<`
    Less,
    /// `<=`
    LessEqual,
    /// `<<` (stereotype open — use `<<<` for bitwise shift left)
    LessLess,
    /// `>`
    Greater,
    /// `>=`
    GreaterEqual,
    /// `>>` (stereotype close — use `>>>` for bitwise shift right)
    GreaterGreater,
    /// `&&` (logical AND — use `&&&` for bitwise AND)
    AmpAmp,
    /// `||` (logical OR — use `|||` for bitwise OR)
    PipePipe,
    /// `&&&` (bitwise AND, F#-style)
    AmpAmpAmp,
    /// `|||` (bitwise OR, F#-style)
    PipePipePipe,
    /// `^^^` (bitwise XOR, F#-style)
    CaretCaretCaret,
    /// `<<<` (bitwise shift left, F#-style)
    LessLessLess,
    /// `>>>` (bitwise shift right, F#-style)
    GreaterGreaterGreater,
    /// `!`
    Bang,
    /// `&` (reserved — use `&&&` for bitwise AND)
    Amp,
    /// `|` (lambda pipe)
    Pipe,
    /// `^` (new instance prefix)
    Caret,
    /// `~` (column builder prefix, Measure~Unit)
    Tilde,
    /// `~~~` (bitwise NOT, F#-style — reserved for future use)
    TildeTildeTilde,
    /// `?` (wildcard column-name placeholder in `ColSpec<(?:Type)>`)
    Question,
    /// `⊆` (U+2286, subtype-constraint operator: `<X⊆T>` reads as
    /// "X is a subtype of T" — used in generic-type-arg bounds)
    Subset,

    // -- Special operators --
    /// `->`
    Arrow,
    /// `=>`
    FatArrow,
    /// `::`
    PathSep,
    /// `@`
    At,
    /// `$`
    Dollar,
    /// `%`
    Percent,

    // -- Section & island markers --
    /// `###` followed by section name (e.g., `###Pure`)
    SectionHeader,
    /// `#tag{` — an island's opener, the engine lexer's `ISLAND_OPEN`: `#`,
    /// any text but `#` and `{` (the tag: empty for graph fetch `#{`, `>`
    /// for a relation store accessor `#>{`), then `{`. The island's body
    /// follows as one [`TokenKind::IslandContent`], then its
    /// [`TokenKind::RBraceHash`].
    IslandOpen,
    /// An island's raw body, as the engine lexer's island mode reads it: up
    /// to the `}#` that closes the island, `#{ … }#` pairs nested. Its
    /// grammar, if one is linked, lexes it again.
    IslandContent,
    /// `#/` — atomic opener for a navigation-path expression
    /// (`#/Type/prop1/prop2!alias#`). Emitted as a single token
    /// so the path parser sees a clean opener and downstream
    /// `/` characters keep their `Slash` lexing inside the path
    /// body. Section headers (`###`) and `#{` are matched
    /// above and never reach this arm.
    HashSlash,
    /// `}#` — closing for `#tag{ … }#` islands.
    RBraceHash,
    /// `#` — a single hash: a navigation path's closer (`#/T/p#`), or any
    /// `#` that opens neither a section (`###`), an island (`#tag{`) nor a
    /// path (`#/`).
    Hash,
    /// Text no grammar reads, as the engine's lexers' `INVALID` token. The
    /// lexer never makes one; the parser makes one of a `###` header inside
    /// a section's body, where the engine, which splits a model into
    /// sections at headers that start a line, leaves it.
    Invalid,

    // -- End of file --
    /// End of input.
    Eof,
}

impl TokenKind {
    /// Returns the keyword kind for the given identifier text, if any.
    pub(crate) fn keyword(text: &str) -> Option<TokenKind> {
        match text {
            "Class" => Some(TokenKind::Class),
            "Enum" => Some(TokenKind::Enum),
            "Profile" => Some(TokenKind::Profile),
            "Association" => Some(TokenKind::Association),
            "Measure" => Some(TokenKind::Measure),
            "Primitive" => Some(TokenKind::Primitive),
            "function" => Some(TokenKind::Function),
            "import" => Some(TokenKind::Import),
            "extends" => Some(TokenKind::Extends),
            "let" => Some(TokenKind::Let),
            "native" => Some(TokenKind::Native),
            "true" => Some(TokenKind::True),
            "false" => Some(TokenKind::False),
            "stereotypes" => Some(TokenKind::Stereotypes),
            "tags" => Some(TokenKind::Tags),
            "shared" => Some(TokenKind::Shared),
            "composite" => Some(TokenKind::Composite),
            "none" => Some(TokenKind::None),
            _ => Option::None,
        }
    }

    /// Returns a human-readable description of this token kind.
    #[must_use]
    pub fn description(&self) -> &'static str {
        match self {
            TokenKind::Class => "'Class'",
            TokenKind::Enum => "'Enum'",
            TokenKind::Profile => "'Profile'",
            TokenKind::Association => "'Association'",
            TokenKind::Measure => "'Measure'",
            TokenKind::Primitive => "'Primitive'",
            TokenKind::Function => "'function'",
            TokenKind::Import => "'import'",
            TokenKind::Extends => "'extends'",
            TokenKind::Let => "'let'",
            TokenKind::Native => "'native'",
            TokenKind::True => "'true'",
            TokenKind::False => "'false'",
            TokenKind::Stereotypes => "'stereotypes'",
            TokenKind::Tags => "'tags'",
            TokenKind::Shared => "'shared'",
            TokenKind::Composite => "'composite'",
            TokenKind::None => "'none'",
            TokenKind::Identifier => "identifier",
            TokenKind::StringLiteral => "string literal",
            TokenKind::IntegerLiteral => "integer literal",
            TokenKind::FloatLiteral => "float literal",
            TokenKind::DecimalLiteral => "decimal literal",
            TokenKind::DateLiteral => "date literal",
            TokenKind::LParen => "'('",
            TokenKind::RParen => "')'",
            TokenKind::LBrace => "'{'",
            TokenKind::RBrace => "'}'",
            TokenKind::LBracket => "'['",
            TokenKind::RBracket => "']'",
            TokenKind::Semicolon => "';'",
            TokenKind::Comma => "','",
            TokenKind::Colon => "':'",
            TokenKind::Dot => "'.'",
            TokenKind::Plus => "'+'",
            TokenKind::Minus => "'-'",
            TokenKind::Star => "'*'",
            TokenKind::Slash => "'/'",
            TokenKind::Equals => "'='",
            TokenKind::EqualEqual => "'=='",
            TokenKind::BangEqual => "'!='",
            TokenKind::Less => "'<'",
            TokenKind::LessEqual => "'<='",
            TokenKind::LessLess => "'<<'",
            TokenKind::Greater => "'>'",
            TokenKind::GreaterEqual => "'>='",
            TokenKind::GreaterGreater => "'>>'",
            TokenKind::AmpAmp => "'&&'",
            TokenKind::PipePipe => "'||'",
            TokenKind::AmpAmpAmp => "'&&&'",
            TokenKind::PipePipePipe => "'|||'",
            TokenKind::CaretCaretCaret => "'^^^'",
            TokenKind::LessLessLess => "'<<<'",
            TokenKind::GreaterGreaterGreater => "'>>>'",
            TokenKind::Bang => "'!'",
            TokenKind::Amp => "'&'",
            TokenKind::Pipe => "'|'",
            TokenKind::Caret => "'^'",
            TokenKind::Tilde => "'~'",
            TokenKind::TildeTildeTilde => "'~~~'",
            TokenKind::Question => "'?'",
            TokenKind::Subset => "'\u{2286}'",
            TokenKind::Arrow => "'->'",
            TokenKind::FatArrow => "'=>'",
            TokenKind::PathSep => "'::'",
            TokenKind::At => "'@'",
            TokenKind::Dollar => "'$'",
            TokenKind::Percent => "'%'",
            TokenKind::SectionHeader => "section header",
            TokenKind::IslandOpen => "island opener",
            TokenKind::IslandContent => "island content",
            TokenKind::HashSlash => "'#/'",
            TokenKind::RBraceHash => "'}#'",
            TokenKind::Hash => "'#'",
            TokenKind::Invalid => "invalid token",
            TokenKind::Eof => "end of file",
        }
    }

    /// Returns `true` if this token kind can be used as an identifier.
    ///
    /// This includes `Identifier`, `StringLiteral` (quoted identifiers), and
    /// all keywords — Pure keywords are valid identifiers in most positions
    /// (e.g., `Class::Enum::Profile` is a valid package path).
    #[must_use]
    pub fn is_identifier_like(self) -> bool {
        matches!(
            self,
            TokenKind::Identifier
                | TokenKind::StringLiteral
                | TokenKind::Class
                | TokenKind::Enum
                | TokenKind::Profile
                | TokenKind::Association
                | TokenKind::Measure
                | TokenKind::Primitive
                | TokenKind::Function
                | TokenKind::Import
                | TokenKind::Extends
                | TokenKind::Let
                | TokenKind::Native
                | TokenKind::Stereotypes
                | TokenKind::Tags
                | TokenKind::Shared
                | TokenKind::Composite
                | TokenKind::None
                | TokenKind::True
                | TokenKind::False
        )
    }
}
