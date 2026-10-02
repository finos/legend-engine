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

//! Protocol `SourceInformation` — source location metadata.
//!
//! Maps to Java `org.finos.legend.engine.protocol.pure.m3.SourceInformation`,
//! written as one string rather than the engine's object:
//!
//! - `sourceId:line:column-column` when the span starts and ends on one line;
//! - `sourceId:line:column-line:column` otherwise.
//!
//! Lines and columns are 1-based and the end column is inclusive, as
//! everywhere in Legend. A source id may itself hold `:` and `-`
//! (`file:///c:/a-b.pure`, `model::Mapping`), so the string is read from its
//! end. Both this type and the engine's Java class read the string and the
//! engine's object form; the engine still writes objects.
//!
//! JSON example:
//! ```json
//! "test.pure:1:1-5:10"
//! ```

use std::fmt;
use std::str::FromStr;

use serde::de::{self, MapAccess, Visitor};
use serde::{Deserialize, Deserializer, Serialize, Serializer};
use serde_json::Value;

/// Source location of an element in the Pure source file.
///
/// Holds the fields of Java `org.finos.legend.engine.protocol.pure.m3.SourceInformation`;
/// its JSON is the compact string the module documentation describes.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SourceInformation {
    /// The source file identifier (e.g., `"test.pure"`).
    pub source_id: String,
    /// The starting line number (1-indexed).
    pub start_line: u32,
    /// The starting column number (1-indexed).
    pub start_column: u32,
    /// The ending line number (1-indexed).
    pub end_line: u32,
    /// The ending column number (1-indexed, inclusive).
    pub end_column: u32,
}

/// The compact form: `sourceId:line:column-column` on one line,
/// `sourceId:line:column-line:column` across lines.
impl fmt::Display for SourceInformation {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if self.start_line == self.end_line {
            write!(
                f,
                "{}:{}:{}-{}",
                self.source_id, self.start_line, self.start_column, self.end_column
            )
        } else {
            write!(
                f,
                "{}:{}:{}-{}:{}",
                self.source_id, self.start_line, self.start_column, self.end_line, self.end_column
            )
        }
    }
}

/// A string that is not a source information's compact form.
#[derive(Debug, Clone, PartialEq, Eq, thiserror::Error)]
#[error(
    "not a source information: {0:?} (expected sourceId:line:column-column or sourceId:line:column-line:column)"
)]
pub struct ParseSourceInformationError(String);

impl FromStr for SourceInformation {
    type Err = ParseSourceInformationError;

    /// Reads the compact form from its end, where the numbers are: what is
    /// left before them, but for the `:` that separates it, is the source id.
    /// Also takes a one-line span written with its end line.
    fn from_str(text: &str) -> Result<Self, Self::Err> {
        let invalid = || ParseSourceInformationError(text.to_string());
        let mut rest = text;
        let last = take_number(&mut rest).ok_or_else(invalid)?;
        let (start_line, start_column, end_line) = if take(&mut rest, '-') {
            // line:column-column
            let start_column = take_number(&mut rest).ok_or_else(invalid)?;
            if !take(&mut rest, ':') {
                return Err(invalid());
            }
            let start_line = take_number(&mut rest).ok_or_else(invalid)?;
            (start_line, start_column, start_line)
        } else if take(&mut rest, ':') {
            // line:column-line:column
            let end_line = take_number(&mut rest).ok_or_else(invalid)?;
            if !take(&mut rest, '-') {
                return Err(invalid());
            }
            let start_column = take_number(&mut rest).ok_or_else(invalid)?;
            if !take(&mut rest, ':') {
                return Err(invalid());
            }
            let start_line = take_number(&mut rest).ok_or_else(invalid)?;
            (start_line, start_column, end_line)
        } else {
            return Err(invalid());
        };
        if !take(&mut rest, ':') {
            return Err(invalid());
        }
        Ok(Self {
            source_id: rest.to_string(),
            start_line,
            start_column,
            end_line,
            end_column: last,
        })
    }
}

/// Takes the number `text` ends with off its end.
fn take_number(text: &mut &str) -> Option<u32> {
    let head = text.trim_end_matches(|c: char| c.is_ascii_digit());
    let number = text[head.len()..].parse().ok()?;
    *text = head;
    Some(number)
}

/// Takes `separator` off the end of `text`, if `text` ends with it.
fn take(text: &mut &str, separator: char) -> bool {
    match text.strip_suffix(separator) {
        Some(head) => {
            *text = head;
            true
        }
        None => false,
    }
}

impl Serialize for SourceInformation {
    fn serialize<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        serializer.collect_str(self)
    }
}

impl<'de> Deserialize<'de> for SourceInformation {
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        deserializer.deserialize_any(SourceInformationVisitor)
    }
}

struct SourceInformationVisitor;

impl<'de> Visitor<'de> for SourceInformationVisitor {
    type Value = SourceInformation;

    fn expecting(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(
            "a source information: \"sourceId:line:column-line:column\", or the engine's object",
        )
    }

    fn visit_str<E: de::Error>(self, text: &str) -> Result<SourceInformation, E> {
        text.parse().map_err(E::custom)
    }

    fn visit_map<A: MapAccess<'de>>(self, map: A) -> Result<SourceInformation, A::Error> {
        let object = EngineObject::deserialize(de::value::MapAccessDeserializer::new(map))?;
        Ok(SourceInformation {
            source_id: object.source_id,
            start_line: object.start_line,
            start_column: object.start_column,
            end_line: object.end_line,
            end_column: object.end_column,
        })
    }
}

/// The engine's own form: `{"sourceId": …, "startLine": …, "startColumn": …,
/// "endLine": …, "endColumn": …}`.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct EngineObject {
    source_id: String,
    start_line: u32,
    start_column: u32,
    end_line: u32,
    end_column: u32,
}

/// Edits, in place, every position in protocol JSON.
///
/// Every protocol field holding a position is named `sourceInformation` or
/// ends in `SourceInformation` (`classSourceInformation`,
/// `mappingSourceInformation`, …), here as in the engine's protocol; this
/// finds positions by that name, inside islands' opaque JSON too, which a walk
/// over the typed values could not reach. Nulls are left as they are.
///
/// # Errors
///
/// A value under such a field that is not a source information: the JSON was
/// not built from this crate's types.
pub fn edit_positions(
    json: &mut Value,
    edit: &mut impl FnMut(&mut SourceInformation),
) -> Result<(), serde_json::Error> {
    match json {
        Value::Object(map) => {
            for (key, value) in map.iter_mut() {
                if (key == "sourceInformation" || key.ends_with("SourceInformation"))
                    && !value.is_null()
                {
                    let mut position = SourceInformation::deserialize(&*value)?;
                    edit(&mut position);
                    *value = serde_json::to_value(&position)?;
                } else {
                    edit_positions(value, edit)?;
                }
            }
        }
        Value::Array(items) => {
            for item in items {
                edit_positions(item, edit)?;
            }
        }
        _ => {}
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    fn si(source_id: &str, start: (u32, u32), end: (u32, u32)) -> SourceInformation {
        SourceInformation {
            source_id: source_id.into(),
            start_line: start.0,
            start_column: start.1,
            end_line: end.0,
            end_column: end.1,
        }
    }

    #[test]
    fn test_writes_one_line_without_its_end_line() {
        let json = serde_json::to_string(&si("test.pure", (3, 5), (3, 12))).unwrap();
        assert_eq!(json, r#""test.pure:3:5-12""#);
    }

    #[test]
    fn test_writes_lines_apart_in_full() {
        let json = serde_json::to_string(&si("test.pure", (1, 1), (5, 10))).unwrap();
        assert_eq!(json, r#""test.pure:1:1-5:10""#);
    }

    #[test]
    fn test_reads_both_forms_back() {
        for position in [
            si("my/file.pure", (10, 3), (15, 42)),
            si("my/file.pure", (10, 3), (10, 42)),
            si("my/file.pure", (10, 3), (10, 3)),
        ] {
            let json = serde_json::to_string(&position).unwrap();
            assert_eq!(
                serde_json::from_str::<SourceInformation>(&json).unwrap(),
                position,
                "{json}"
            );
        }
    }

    #[test]
    fn test_reads_one_line_written_with_its_end_line() {
        let position: SourceInformation = "x:4:7-4:13".parse().unwrap();
        assert_eq!(position, si("x", (4, 7), (4, 13)));
    }

    #[test]
    fn test_source_ids_hold_anything() {
        // Read from the end: ':' and '-' in the id are the id's.
        for id in [
            "",
            "a::B",
            "file:///c:/x-1.pure",
            "model::A@derivedProperty@name@1-2",
            "dir\\with \"quotes\"",
            "4:7-13",
        ] {
            let position = si(id, (4, 7), (6, 2));
            let json = serde_json::to_string(&position).unwrap();
            assert_eq!(
                serde_json::from_str::<SourceInformation>(&json).unwrap(),
                position,
                "{json}"
            );
            let one_line = si(id, (4, 7), (4, 13));
            assert_eq!(
                one_line.to_string().parse::<SourceInformation>().unwrap(),
                one_line
            );
        }
    }

    #[test]
    fn test_refuses_what_is_not_a_position() {
        for text in [
            "",
            "4:7-4:13",
            "x:4:7",
            "x:4:7-4:",
            "x:4:7:4:13",
            "x:4-7:4-13",
            "x4:7-4:13",
            "x:4:7--13",
            "x:4:7-4:13 ",
            "x:4:7-99999999999",
            "x:-4:7-13",
        ] {
            let error = text.parse::<SourceInformation>().unwrap_err();
            assert_eq!(error, ParseSourceInformationError(text.to_string()));
            assert!(
                serde_json::from_value::<SourceInformation>(json!(text)).is_err(),
                "{text}"
            );
        }
    }

    #[test]
    fn test_deserialize_from_java_json() {
        // The engine's object form
        let java_json = r#"{
            "sourceId": "model.pure",
            "startLine": 3,
            "startColumn": 1,
            "endLine": 8,
            "endColumn": 1
        }"#;
        let position: SourceInformation = serde_json::from_str(java_json).unwrap();
        assert_eq!(position, si("model.pure", (3, 1), (8, 1)));
    }

    #[test]
    fn test_edits_every_position_islands_included() {
        let mut json = json!({
            "_type": "lambda",
            "body": [{
                "_type": "classInstance",
                "type": "rootGraphFetchTree",
                "value": {
                    "subTrees": [{"property": "name", "sourceInformation": "x:4:3-9"}],
                    "sourceInformation": "x:3:1-5:4"
                },
                "sourceInformation": "x:3:1-5:4"
            }, {
                "_type": "string",
                "value": "x:4:3-9",
                "sourceInformation": null
            }],
            "classSourceInformation": "x:2:2-3",
            "sourceInformation": "x:2:1-6:2"
        });
        edit_positions(&mut json, &mut |position| {
            position.source_id = "m::M".into();
            position.start_line -= 1;
            position.end_line -= 1;
        })
        .unwrap();
        assert_eq!(
            json,
            json!({
                "_type": "lambda",
                "body": [{
                    "_type": "classInstance",
                    "type": "rootGraphFetchTree",
                    "value": {
                        "subTrees": [{"property": "name", "sourceInformation": "m::M:3:3-9"}],
                        "sourceInformation": "m::M:2:1-4:4"
                    },
                    "sourceInformation": "m::M:2:1-4:4"
                }, {
                    "_type": "string",
                    "value": "x:4:3-9",
                    "sourceInformation": null
                }],
                "classSourceInformation": "m::M:1:2-3",
                "sourceInformation": "m::M:1:1-5:2"
            })
        );
    }

    #[test]
    fn test_edit_refuses_a_value_that_is_not_a_position() {
        let mut json = json!({"sourceInformation": "not a position"});
        assert!(edit_positions(&mut json, &mut |_| {}).is_err());
    }
}
