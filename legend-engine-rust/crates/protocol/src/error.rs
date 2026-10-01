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

//! Errors raised while converting the AST into Protocol v1.

use legend_pure_parser_ast::SourceInfo;

/// Result alias for AST → Protocol conversions.
pub type Result<T> = std::result::Result<T, ConvertError>;

/// Failure to convert an AST node into Protocol v1.
#[derive(Debug, thiserror::Error)]
pub enum ConvertError {
    /// The construct parses, but Protocol v1 has no representation for it,
    /// or no linked plug-in converts it (an island tag or DSL element kind
    /// without a registered converter).
    ///
    /// Engine callers treat this as "fall back to the Java parser" rather
    /// than as a user-facing syntax error.
    #[error("{message}")]
    Unsupported {
        /// What is unsupported, e.g. `native function definitions`.
        message: String,
        /// Where the construct appears.
        source_info: SourceInfo,
    },
    /// A registered plug-in failed on content it claimed (for example, an
    /// island converter handed content of an unexpected type).
    #[error("{message}")]
    Plugin {
        /// The plug-in's diagnostic.
        message: String,
        /// Where the offending content appears, when known.
        source_info: Option<SourceInfo>,
    },
    /// JSON serialization failed.
    #[error(transparent)]
    Json(#[from] serde_json::Error),
}

impl ConvertError {
    /// Builds an [`ConvertError::Unsupported`] for the construct at `source_info`.
    #[must_use]
    pub fn unsupported(message: impl Into<String>, source_info: &SourceInfo) -> Self {
        Self::Unsupported {
            message: message.into(),
            source_info: source_info.clone(),
        }
    }

    /// Builds a [`ConvertError::Plugin`] error.
    #[must_use]
    pub fn plugin(message: impl Into<String>, source_info: Option<&SourceInfo>) -> Self {
        Self::Plugin {
            message: message.into(),
            source_info: source_info.cloned(),
        }
    }

    /// The source location the error refers to, when known.
    #[must_use]
    pub fn source_info(&self) -> Option<&SourceInfo> {
        match self {
            Self::Unsupported { source_info, .. } => Some(source_info),
            Self::Plugin { source_info, .. } => source_info.as_ref(),
            Self::Json(_) => None,
        }
    }
}
