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
//! Maps to Java `org.finos.legend.engine.protocol.pure.m3.SourceInformation`.
//!
//! JSON example:
//! ```json
//! {
//!   "sourceId": "test.pure",
//!   "startLine": 1,
//!   "startColumn": 1,
//!   "endLine": 5,
//!   "endColumn": 10
//! }
//! ```

use serde::{Deserialize, Serialize};

/// Source location of an element in the Pure source file.
///
/// Maps 1:1 to Java `org.finos.legend.engine.protocol.pure.m3.SourceInformation`.
/// All fields are always present (no optional fields).
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SourceInformation {
    /// The source file identifier (e.g., `"test.pure"`).
    pub source_id: String,
    /// The starting line number (1-indexed).
    pub start_line: u32,
    /// The starting column number (1-indexed).
    pub start_column: u32,
    /// The ending line number (1-indexed).
    pub end_line: u32,
    /// The ending column number (1-indexed).
    pub end_column: u32,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_serialization_field_names() {
        let si = SourceInformation {
            source_id: "test.pure".into(),
            start_line: 1,
            start_column: 1,
            end_line: 5,
            end_column: 10,
        };
        let json = serde_json::to_value(&si).unwrap();

        // Verify exact camelCase field names match Java
        assert_eq!(json["sourceId"], "test.pure");
        assert_eq!(json["startLine"], 1);
        assert_eq!(json["startColumn"], 1);
        assert_eq!(json["endLine"], 5);
        assert_eq!(json["endColumn"], 10);

        // Verify no extra fields
        let obj = json.as_object().unwrap();
        assert_eq!(obj.len(), 5);
    }

    #[test]
    fn test_deserialization_roundtrip() {
        let si = SourceInformation {
            source_id: "my/file.pure".into(),
            start_line: 10,
            start_column: 3,
            end_line: 15,
            end_column: 42,
        };
        let json_str = serde_json::to_string(&si).unwrap();
        let back: SourceInformation = serde_json::from_str(&json_str).unwrap();
        assert_eq!(back, si);
    }

    #[test]
    fn test_deserialize_from_java_json() {
        // Simulates JSON produced by the Java protocol
        let java_json = r#"{
            "sourceId": "model.pure",
            "startLine": 3,
            "startColumn": 1,
            "endLine": 8,
            "endColumn": 1
        }"#;
        let si: SourceInformation = serde_json::from_str(java_json).unwrap();
        assert_eq!(si.source_id, "model.pure");
        assert_eq!(si.start_line, 3);
        assert_eq!(si.end_column, 1);
    }
}
