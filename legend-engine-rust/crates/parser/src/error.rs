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

//! Parse error types.

use legend_pure_parser_ast::SourceInfo;
use legend_pure_parser_lexer::TokenKind;

/// A parser error.
#[derive(Debug, Clone, thiserror::Error, serde::Serialize)]
#[serde(tag = "kind", rename_all = "camelCase")]
pub enum ParseError {
    /// Unexpected token encountered.
    #[error("{message} at {source_info}")]
    Unexpected {
        /// Human-readable error message.
        message: String,
        /// Location of the unexpected token.
        source_info: SourceInfo,
    },

    /// Lexer error propagated up.
    #[error(transparent)]
    Lex(#[from] legend_pure_parser_lexer::LexError),

    /// Input no linked grammar can parse, though another parser may: an
    /// island whose tag has no linked grammar.
    #[error("{message} at {source_info}")]
    Unsupported {
        /// Human-readable error message.
        message: String,
        /// Where the unsupported input starts.
        source_info: SourceInfo,
    },
}

impl ParseError {
    /// Returns the source location of the error, if available.
    #[must_use]
    pub fn source_info(&self) -> Option<&SourceInfo> {
        match self {
            Self::Unexpected { source_info, .. } | Self::Unsupported { source_info, .. } => {
                Some(source_info)
            }
            Self::Lex(lex_err) => Some(lex_err.source_info()),
        }
    }

    /// Returns the human-readable error message without location info.
    #[must_use]
    pub fn message(&self) -> String {
        match self {
            Self::Unexpected { message, .. } | Self::Unsupported { message, .. } => message.clone(),
            Self::Lex(e) => e.message(),
        }
    }

    /// Creates an "expected X, found Y" error.
    pub(crate) fn expected(expected: &str, found: TokenKind, source_info: SourceInfo) -> Self {
        Self::Unexpected {
            message: format!("Expected {expected}, found {}", found.description()),
            source_info,
        }
    }

    /// Creates a generic unexpected token error.
    pub(crate) fn unexpected(message: impl Into<String>, source_info: SourceInfo) -> Self {
        Self::Unexpected {
            message: message.into(),
            source_info,
        }
    }
}
