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

//! End-to-end protocol snapshot tests.
//!
//! Each test follows the full pipeline:
//!
//!   `.pure` source → Parser → AST → Protocol → JSON
//!
//! The resulting JSON is snapshotted with `insta`. These snapshots serve as the
//! authoritative golden files for Java compatibility: any drift between the Rust
//! and Java protocol output should appear as a snapshot diff.

use legend_pure_parser_protocol::v1::convert::convert_source_file;

/// `value` as pretty JSON text, which `serde_json` renders with numbers as
/// written (the workspace enables its `arbitrary_precision`, which insta's
/// own JSON serializer doesn't know).
fn json_text<T: serde::Serialize>(value: &T) -> String {
    serde_json::to_string_pretty(value).expect("serializes")
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/// Load a `.pure` corpus file from the `tests/corpus/` directory.
fn corpus(name: &str) -> String {
    let path = format!("{}/tests/corpus/{name}", env!("CARGO_MANIFEST_DIR"));
    std::fs::read_to_string(&path)
        .unwrap_or_else(|e| panic!("Failed to read corpus file {path}: {e}"))
}

/// Parse → AST → Protocol → JSON pipeline. Returns a `serde_json::Value`.
///
/// # Panics
///
/// Panics if parsing, conversion, or serialization fails.
fn parse_to_protocol_json(source: &str, source_name: &str) -> serde_json::Value {
    let ast = legend_pure_parser_parser::parse(source, source_name)
        .unwrap_or_else(|e| panic!("Parse failed for {source_name}: {e}"));
    let protocol = convert_source_file(&ast).unwrap();
    serde_json::to_value(&protocol).expect("Protocol serialization cannot fail")
}

// ---------------------------------------------------------------------------
// End-to-End Snapshot Tests
// ---------------------------------------------------------------------------

#[test]
fn e2e_profile() {
    let source = corpus("profile.pure");
    let json = parse_to_protocol_json(&source, "profile.pure");
    insta::assert_snapshot!("e2e_profile", json_text(&json));
}

#[test]
fn e2e_enumeration() {
    let source = corpus("enumeration.pure");
    let json = parse_to_protocol_json(&source, "enumeration.pure");
    insta::assert_snapshot!("e2e_enumeration", json_text(&json));
}

#[test]
fn e2e_class_simple() {
    let source = corpus("class_simple.pure");
    let json = parse_to_protocol_json(&source, "class_simple.pure");
    insta::assert_snapshot!("e2e_class_simple", json_text(&json));
}

#[test]
fn e2e_class_with_annotations() {
    let source = corpus("class_with_annotations.pure");
    let json = parse_to_protocol_json(&source, "class_with_annotations.pure");
    insta::assert_snapshot!("e2e_class_with_annotations", json_text(&json));
}

#[test]
fn e2e_class_with_qualified_property() {
    let source = corpus("class_with_qualified_property.pure");
    let json = parse_to_protocol_json(&source, "class_with_qualified_property.pure");
    insta::assert_snapshot!("e2e_class_with_qualified_property", json_text(&json));
}

#[test]
fn e2e_function() {
    let source = corpus("function.pure");
    let json = parse_to_protocol_json(&source, "function.pure");
    insta::assert_snapshot!("e2e_function", json_text(&json));
}

#[test]
fn e2e_association() {
    let source = corpus("association.pure");
    let json = parse_to_protocol_json(&source, "association.pure");
    insta::assert_snapshot!("e2e_association", json_text(&json));
}

#[test]
fn e2e_measure() {
    let source = corpus("measure.pure");
    let json = parse_to_protocol_json(&source, "measure.pure");
    insta::assert_snapshot!("e2e_measure", json_text(&json));
}

#[test]
fn e2e_multi_element() {
    let source = corpus("multi_element.pure");
    let json = parse_to_protocol_json(&source, "multi_element.pure");
    insta::assert_snapshot!("e2e_multi_element", json_text(&json));
}
