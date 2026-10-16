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

//! Source location information for AST nodes.
//!
//! Every AST node carries a [`SourceInfo`] to pinpoint its location in the
//! original source text. This is critical for error messages, IDE integration,
//! and debugging parsing/compilation problems.

use serde::{Deserialize, Serialize};
use smol_str::SmolStr;

/// Tracks the source location of an AST node within the original source text.
///
/// Line and column numbers are 1-indexed to match conventional editor/IDE behavior
/// and the existing Java `SourceInformation` format.
///
/// # Examples
///
/// ```
/// use legend_pure_parser_ast::SourceInfo;
/// use smol_str::SmolStr;
///
/// let info = SourceInfo {
///     source: SmolStr::new("test.pure"),
///     start_line: 1,
///     start_column: 1,
///     end_line: 1,
///     end_column: 10,
/// };
/// assert_eq!(info.start_line, 1);
/// ```
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct SourceInfo {
    /// Source identifier (file path or URI).
    pub source: SmolStr,
    /// Starting line (1-indexed).
    pub start_line: u32,
    /// Starting column (1-indexed).
    pub start_column: u32,
    /// Ending line (1-indexed).
    pub end_line: u32,
    /// Ending column (1-indexed).
    pub end_column: u32,
}

impl SourceInfo {
    /// Creates a new `SourceInfo` from a source identifier and position.
    #[must_use]
    pub fn new(
        source: impl Into<SmolStr>,
        start_line: u32,
        start_column: u32,
        end_line: u32,
        end_column: u32,
    ) -> Self {
        Self {
            source: source.into(),
            start_line,
            start_column,
            end_line,
            end_column,
        }
    }

    /// Merges two source spans into one that covers both.
    ///
    /// Useful when building a parent node whose span should cover all children.
    #[must_use]
    pub fn merge(&self, other: &Self) -> Self {
        let (start_line, start_column) = if self.start_line < other.start_line
            || (self.start_line == other.start_line && self.start_column <= other.start_column)
        {
            (self.start_line, self.start_column)
        } else {
            (other.start_line, other.start_column)
        };

        let (end_line, end_column) = if self.end_line > other.end_line
            || (self.end_line == other.end_line && self.end_column >= other.end_column)
        {
            (self.end_line, self.end_column)
        } else {
            (other.end_line, other.end_column)
        };

        Self {
            source: self.source.clone(),
            start_line,
            start_column,
            end_line,
            end_column,
        }
    }
}

impl std::fmt::Display for SourceInfo {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "{}:{}:{}-{}:{}",
            self.source, self.start_line, self.start_column, self.end_line, self.end_column
        )
    }
}

/// Where a piece of source text sits inside a larger document.
///
/// Positions in the text are reported as if it started at this offset:
/// `line` is added to every line number, and `column` is added to column
/// numbers on the text's first line only (the rest of the first line
/// continues the enclosing document's line; later lines start fresh).
///
/// This mirrors Legend Engine's `ParseTreeWalkerSourceInformation`, whose
/// section parsers receive a section body plus the line offset of its
/// header. The engine routinely uses a line offset of `-1`, so offsets are
/// signed; [`SourceOffset::new`] only rejects offsets that could place a
/// position below zero.
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub struct SourceOffset {
    line: i32,
    column: i32,
}

impl SourceOffset {
    /// Creates an offset, or `None` if it could produce a negative line or
    /// column (the smallest in-text position is line 1, column 1, so both
    /// offsets must be at least `-1`).
    #[must_use]
    pub fn new(line: i32, column: i32) -> Option<Self> {
        (line >= -1 && column >= -1).then_some(Self { line, column })
    }

    /// The offset added to every line number.
    #[must_use]
    pub fn line(self) -> i32 {
        self.line
    }

    /// The offset added to column numbers on the first line.
    #[must_use]
    pub fn column(self) -> i32 {
        self.column
    }

    /// Translates a span measured within the text (1-based, first line = 1)
    /// into document coordinates.
    #[must_use]
    pub fn apply(self, si: SourceInfo) -> SourceInfo {
        let column_on = |line: u32, column: u32| {
            if line == 1 {
                column.saturating_add_signed(self.column)
            } else {
                column
            }
        };
        SourceInfo {
            start_column: column_on(si.start_line, si.start_column),
            end_column: column_on(si.end_line, si.end_column),
            start_line: si.start_line.saturating_add_signed(self.line),
            end_line: si.end_line.saturating_add_signed(self.line),
            source: si.source,
        }
    }
}

/// Trait for all AST nodes that carry source location information.
///
/// **Every AST node must implement `Spanned`.** This is a core design principle
/// — no node exists without a known source position. This eliminates the
/// troubleshooting gaps present in the Java parser where some nodes lack
/// `sourceInformation`.
pub trait Spanned {
    /// Returns the source location of this node.
    fn source_info(&self) -> &SourceInfo;
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_source_info_display() {
        let info = SourceInfo::new("test.pure", 1, 5, 3, 10);
        assert_eq!(info.to_string(), "test.pure:1:5-3:10");
    }

    #[test]
    fn test_source_info_merge() {
        let a = SourceInfo::new("test.pure", 1, 5, 2, 10);
        let b = SourceInfo::new("test.pure", 2, 3, 4, 15);
        let merged = a.merge(&b);

        assert_eq!(merged.start_line, 1);
        assert_eq!(merged.start_column, 5);
        assert_eq!(merged.end_line, 4);
        assert_eq!(merged.end_column, 15);
    }

    #[test]
    fn test_source_info_merge_same_line() {
        let a = SourceInfo::new("test.pure", 1, 10, 1, 20);
        let b = SourceInfo::new("test.pure", 1, 1, 1, 5);
        let merged = a.merge(&b);

        assert_eq!(merged.start_line, 1);
        assert_eq!(merged.start_column, 1);
        assert_eq!(merged.end_line, 1);
        assert_eq!(merged.end_column, 20);
    }

    #[test]
    fn source_offset_rejects_offsets_that_could_go_negative() {
        assert!(SourceOffset::new(-1, -1).is_some());
        assert!(SourceOffset::new(-2, 0).is_none());
        assert!(SourceOffset::new(0, -2).is_none());
    }

    #[test]
    fn source_offset_shifts_columns_on_the_first_line_only() {
        let offset = SourceOffset::new(10, 4).expect("valid");
        let first = offset.apply(SourceInfo::new("s", 1, 3, 1, 7));
        assert_eq!(first, SourceInfo::new("s", 11, 7, 11, 11));
        let later = offset.apply(SourceInfo::new("s", 2, 3, 2, 7));
        assert_eq!(later, SourceInfo::new("s", 12, 3, 12, 7));
        // A span starting on line 1 and ending later shifts only its start column.
        let spanning = offset.apply(SourceInfo::new("s", 1, 3, 2, 7));
        assert_eq!(spanning, SourceInfo::new("s", 11, 7, 12, 7));
    }

    #[test]
    fn source_offset_matches_the_engine_section_offset() {
        // Engine: headerless code gets "\n###Pure\n" prepended, so a section
        // body's line 2 is the caller's line 1 (lineOffset = -1).
        let offset = SourceOffset::new(-1, 0).expect("valid");
        assert_eq!(
            offset.apply(SourceInfo::new("s", 2, 1, 2, 5)),
            SourceInfo::new("s", 1, 1, 1, 5)
        );
    }

    #[derive(crate::Spanned)]
    struct TestNode {
        source_info: SourceInfo,
    }

    #[test]
    fn test_impl_spanned_macro() {
        let node = TestNode {
            source_info: SourceInfo::new("test.pure", 1, 1, 1, 10),
        };
        assert_eq!(node.source_info().start_line, 1);
        assert_eq!(node.source_info().end_column, 10);
    }
}
