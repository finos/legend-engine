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

//! The engine walkers' checks of fields their grammars read in any order,
//! as often as written (`PureGrammarParserUtility`'s
//! `validateAndExtractRequiredField` and `validateAndExtractOptionalField`):
//! each field written at most once, some required. The error is positioned
//! on what holds the fields.

use legend_pure_parser_ast::source_info::SourceInfo;

use crate::error::ParseError;

/// The one value of a required field.
///
/// # Errors
///
/// No value, or more than one.
pub fn required<T>(
    mut values: Vec<T>,
    field: &str,
    source_info: &SourceInfo,
) -> Result<T, ParseError> {
    match values.len() {
        0 => Err(missing(field, source_info)),
        1 => Ok(values.remove(0)),
        _ => Err(once(field, source_info)),
    }
}

/// The error for a required field without a value, which some walkers
/// report for an empty one too.
#[must_use]
pub fn missing(field: &str, source_info: &SourceInfo) -> ParseError {
    error(format!("Field '{field}' is required"), source_info)
}

/// The value of an optional field, if written.
///
/// # Errors
///
/// More than one value.
pub fn optional<T>(
    mut values: Vec<T>,
    field: &str,
    source_info: &SourceInfo,
) -> Result<Option<T>, ParseError> {
    match values.len() {
        0 => Ok(None),
        1 => Ok(Some(values.remove(0))),
        _ => Err(once(field, source_info)),
    }
}

fn once(field: &str, source_info: &SourceInfo) -> ParseError {
    error(
        format!("Field '{field}' should be specified only once"),
        source_info,
    )
}

fn error(message: String, source_info: &SourceInfo) -> ParseError {
    ParseError::Unexpected {
        message,
        source_info: source_info.clone(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_field_is_written_at_most_once() {
        let at = SourceInfo::new("f.pure", 1, 1, 1, 5);
        assert_eq!(required(vec![1], "a", &at).expect("one"), 1);
        assert_eq!(optional::<i32>(Vec::new(), "a", &at).expect("none"), None);
        let missing = required::<i32>(Vec::new(), "a", &at).expect_err("none");
        assert_eq!(missing.message(), "Field 'a' is required");
        let twice = optional(vec![1, 2], "a", &at).expect_err("two");
        assert_eq!(twice.message(), "Field 'a' should be specified only once");
        assert_eq!(twice.source_info(), Some(&at));
    }
}
