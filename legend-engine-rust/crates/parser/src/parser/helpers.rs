// Copyright 2026 The Legend Authors
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

use crate::cursor::Cursor;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::TokenKind;
use smol_str::SmolStr;

// ── Helpers ─────────────────────────────────────────────────────────────

/// Split a [`Package`] into its parent package + leaf name. Used by
/// island/section parser plug-ins that resolve qualified paths.
#[must_use]
pub fn split_package_name(pkg: &Package) -> (Option<Package>, SmolStr) {
    let name = SmolStr::new(pkg.name());
    (pkg.parent().cloned(), name)
}

/// Strip the surrounding single quotes from a string literal token's
/// text and resolve common escape sequences (`\\'`, `\\\\`, `\n`,
/// `\t`, `\r`). Used by island/section parser plug-ins that consume
/// `'...'` literals from the cursor.
#[must_use]
pub fn unquote_string(s: &str) -> String {
    let inner = &s[1..s.len() - 1]; // strip surrounding quotes
    let mut result = String::with_capacity(inner.len());
    let mut chars = inner.chars();
    while let Some(c) = chars.next() {
        if c == '\\' {
            #[allow(clippy::match_same_arms)] // Semantically distinct escapes
            match chars.next() {
                Some('\'') => result.push('\''),
                Some('\\') => result.push('\\'),
                Some('n') => result.push('\n'),
                Some('t') => result.push('\t'),
                Some('r') => result.push('\r'),
                Some(other) => {
                    // Unrecognized escape — preserve verbatim
                    result.push('\\');
                    result.push(other);
                }
                None => result.push('\\'), // trailing backslash
            }
        } else {
            result.push(c);
        }
    }
    result
}

pub(crate) fn is_wildcard_ahead(cursor: &Cursor) -> bool {
    cursor.peek_kind_at(1) == TokenKind::Star
}
