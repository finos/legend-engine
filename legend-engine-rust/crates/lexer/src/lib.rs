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

//! # Legend Pure Parser — Lexer
//!
//! Hand-written tokenizer for the Pure grammar. Produces a flat `Vec<Token>` with spans
//! from source text. Handles keywords, operators, literals, island grammar markers,
//! and section headers.
//!
//! # Usage
//!
//! ```
//! use legend_pure_parser_lexer::{tokenize, TokenKind};
//!
//! let tokens = tokenize("Class my::Person { }", "test.pure").unwrap();
//! assert_eq!(tokens[0].kind, TokenKind::Class);
//! assert_eq!(tokens[1].kind, TokenKind::Identifier);
//! assert_eq!(tokens[1].text, "my");
//! ```

#![forbid(unsafe_code)]
#![deny(missing_docs)]

mod lexer;
pub mod token;

pub use lexer::{tokenize, tokenize_with_offset};
pub use token::{Token, TokenKind};

use legend_pure_parser_ast::SourceInfo;

/// Errors produced during lexing.
#[derive(Debug, Clone, thiserror::Error, serde::Serialize)]
#[serde(tag = "kind", rename_all = "camelCase")]
pub enum LexError {
    /// An unterminated string literal.
    #[error("Unterminated string literal at {source_info}")]
    UnterminatedString {
        /// Location of the opening quote.
        source_info: SourceInfo,
    },

    /// An unterminated block comment.
    #[error("Unterminated block comment at {source_info}")]
    UnterminatedBlockComment {
        /// Location of the opening `/*`.
        source_info: SourceInfo,
    },

    /// An island without the `}#` that closes it.
    #[error("Unterminated island at {source_info}")]
    UnterminatedIsland {
        /// Location of the island's opener.
        source_info: SourceInfo,
    },

    /// An unexpected character.
    #[error("Unexpected character '{ch}' at {source_info}")]
    UnexpectedCharacter {
        /// The unexpected character.
        ch: char,
        /// Location.
        source_info: SourceInfo,
    },
}

impl LexError {
    /// The error description without its position.
    #[must_use]
    pub fn message(&self) -> String {
        match self {
            Self::UnterminatedString { .. } => "Unterminated string literal".to_string(),
            Self::UnterminatedBlockComment { .. } => "Unterminated block comment".to_string(),
            Self::UnterminatedIsland { .. } => "Unterminated island".to_string(),
            Self::UnexpectedCharacter { ch, .. } => format!("Unexpected character '{ch}'"),
        }
    }

    /// Where the malformed input is.
    #[must_use]
    pub fn source_info(&self) -> &SourceInfo {
        match self {
            Self::UnterminatedString { source_info }
            | Self::UnterminatedBlockComment { source_info }
            | Self::UnterminatedIsland { source_info }
            | Self::UnexpectedCharacter { source_info, .. } => source_info,
        }
    }
}
