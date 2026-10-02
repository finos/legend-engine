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

//! The `###ExternalFormat` section, as the engine's
//! `ExternalFormatParseTreeWalker` reads it.

use legend_pure_dsl_external_format as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The elements of the section `###ExternalFormat\n{body}`, its index
/// last.
fn elements(body: &str) -> Vec<Value> {
    let source = format!("###ExternalFormat\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "e.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|element| serde_json::to_value(element).expect("serializes"))
        .collect()
}

fn error(body: &str) -> ParseError {
    let source = format!("###ExternalFormat\n{body}");
    legend_pure_parser_parser::parse(&source, "e.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    serde_json::to_value(SourceInformation {
        source_id: "e.pure".into(),
        start_line,
        start_column,
        end_line,
        end_column,
    })
    .expect("a position serializes")
}

fn position(error: &ParseError) -> (u32, u32, u32, u32) {
    let si = error.source_info().expect("positioned");
    (si.start_line, si.start_column, si.end_line, si.end_column)
}

#[test]
fn schema_sets_come_before_bindings() {
    let elements = elements(
        "import my::*;\n\nBinding my::B\n{\n  schemaSet: my::S;\n  schemaId: people;\n  contentType: 'application/json';\n  modelIncludes: [ my::Person, my::Firm ];\n  modelExcludes: [ my::Secret ];\n}\n\nSchemaSet my::S\n{\n  format: JSON;\n  schemas: [ { id: people; location: 'people.json'; content: '{}'; }, { content: '[]'; } ];\n}\n",
    );
    // The walker walks the schema sets, then the bindings.
    assert_eq!(
        elements[0],
        json!({
            "_type": "externalFormatSchemaSet",
            "package": "my",
            "name": "S",
            "format": "JSON",
            "schemas": [
                {
                    "id": "people",
                    "location": "people.json",
                    "content": "{}",
                    "contentSourceInformation": si(16, 53, 16, 66),
                    "sourceInformation": si(16, 14, 16, 68),
                },
                {
                    "content": "[]",
                    "contentSourceInformation": si(16, 73, 16, 86),
                    "sourceInformation": si(16, 71, 16, 88),
                },
            ],
            "sourceInformation": si(13, 1, 17, 1),
        })
    );
    assert_eq!(
        elements[1],
        json!({
            "_type": "binding",
            "package": "my",
            "name": "B",
            "schemaSet": "my::S",
            "schemaId": "people",
            "contentType": "application/json",
            "modelUnit": {
                "packageableElementIncludes": ["my::Person", "my::Firm"],
                "packageableElementExcludes": ["my::Secret"],
            },
            "sourceInformation": si(4, 1, 11, 1),
        })
    );
    // The section has imports, and lists its elements in the same order.
    let section = &elements[2]["sections"][0];
    assert_eq!(section["_type"], "importAware");
    assert_eq!(section["imports"], json!(["my"]));
    assert_eq!(section["elements"], json!(["my::S", "my::B"]));
}

#[test]
fn the_walker_checks_fields() {
    let format = error("SchemaSet my::S\n{\n  schemas: [ { content: 'x'; } ];\n}\n");
    assert_eq!(format.message(), "Field 'format' is required");
    assert_eq!(position(&format), (2, 1, 5, 1));
    // A schema's on the schema.
    let content = error("SchemaSet my::S\n{\n  format: XSD;\n  schemas: [ { id: a; } ];\n}\n");
    assert_eq!(content.message(), "Field 'content' is required");
    assert_eq!(position(&content), (5, 14, 5, 23));
    let includes = error("Binding my::B\n{\n  contentType: 'x';\n}\n");
    assert_eq!(includes.message(), "Field 'modelIncludes' is required");
    assert_eq!(position(&includes), (2, 1, 5, 1));
}

#[test]
fn the_grammar_s_rules_hold() {
    // A format is a word but one of the grammar's keywords.
    let keyword =
        error("SchemaSet my::S\n{\n  format: content;\n  schemas: [ { content: 'x'; } ];\n}\n");
    assert_eq!(position(&keyword), (4, 11, 4, 17));
    // A schema set has at least one schema.
    let empty = error("SchemaSet my::S\n{\n  format: XSD;\n  schemas: [];\n}\n");
    assert_eq!(position(&empty), (5, 13, 5, 13));
}

#[test]
fn a_format_no_module_of_the_engine_defines_is_unsupported() {
    // The engine checks it against the formats it has: a test's own.
    let example =
        error("SchemaSet my::S\n{\n  format: Example;\n  schemas: [ { content: 'x'; } ];\n}\n");
    assert!(
        matches!(example, ParseError::Unsupported { .. }),
        "{example:?}"
    );
    assert_eq!(position(&example), (4, 3, 4, 18));
}
