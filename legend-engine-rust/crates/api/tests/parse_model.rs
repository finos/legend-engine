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

//! Whole models, as the engine's `PureGrammarParser.parseModel` parses them:
//! the expected values are the engine's own results for the same code.

use legend_pure_dsl_data as _;
use legend_pure_dsl_mapping as _;
use legend_pure_parser_api::{
    ErrorKind, ModelRequest, SectionError, parse_model, parse_model_json,
};
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

fn request(code: &str) -> ModelRequest<'_> {
    ModelRequest {
        code,
        source_id: "",
        line_offset: 0,
        column_offset: 0,
    }
}

fn model(request: &ModelRequest<'_>) -> Vec<Value> {
    parse_model(request).expect("parses").elements
}

fn error(request: &ModelRequest<'_>) -> SectionError {
    parse_model(request).expect_err("fails")
}

/// `start_line:start_column-end_line:end_column`.
fn span(value: &Value) -> String {
    let si = position(value);
    format!(
        "{}:{}-{}:{}",
        si.start_line, si.start_column, si.end_line, si.end_column
    )
}

/// A JSON node's source information.
fn position(value: &Value) -> SourceInformation {
    serde_json::from_value(value["sourceInformation"].clone())
        .unwrap_or_else(|e| panic!("no position in {value}: {e}"))
}

/// The elements but the index: `path span`.
fn elements(elements: &[Value]) -> Vec<String> {
    elements[..elements.len() - 1]
        .iter()
        .map(|e| {
            let package = e["package"].as_str().unwrap_or_default();
            let name = e["name"].as_str().expect("named");
            let path = if package.is_empty() {
                name.to_string()
            } else {
                format!("{package}::{name}")
            };
            format!("{path} {}", span(e))
        })
        .collect()
}

/// The index's sections: `type parser span [paths] imports`, imports only
/// for an import-aware section.
fn sections(elements: &[Value]) -> Vec<String> {
    let index = elements.last().expect("an index");
    assert_eq!(index["_type"], "sectionIndex");
    assert_eq!(index["package"], "__internal__");
    assert_eq!(index["name"], "SectionIndex");
    index["sections"]
        .as_array()
        .expect("sections")
        .iter()
        .map(|s| {
            let paths: Vec<&str> = s["elements"]
                .as_array()
                .expect("paths")
                .iter()
                .map(|p| p.as_str().expect("a path"))
                .collect();
            let imports = match s.get("imports") {
                Some(imports) => format!(" {imports}"),
                None => String::new(),
            };
            format!(
                "{} {} {} {paths:?}{imports}",
                s["_type"].as_str().expect("typed"),
                s["parserName"].as_str().expect("named"),
                span(s)
            )
        })
        .collect()
}

fn error_at(error: &SectionError) -> (ErrorKind, Option<(u32, u32)>) {
    (
        error.kind,
        error
            .source_information
            .as_ref()
            .map(|si| (si.start_line, si.start_column)),
    )
}

#[test]
fn code_without_a_header_is_a_pure_section() {
    let parsed = model(&request(""));
    assert_eq!(elements(&parsed), Vec::<String>::new());
    assert_eq!(sections(&parsed), ["importAware Pure 1:1-2:8 [] []"]);

    let parsed = model(&request("Class a::A{}\n###Mapping\nMapping a::M\n(\n)\n"));
    assert_eq!(elements(&parsed), ["a::A 1:1-1:12", "a::M 3:1-5:1"]);
    assert_eq!(
        sections(&parsed),
        [
            "importAware Pure 1:1-3:12 [\"a::A\"] []",
            "importAware Mapping 3:13-7:2 [\"a::M\"] []",
        ]
    );
}

#[test]
fn code_starting_with_a_header_leaves_the_default_section_empty() {
    let parsed = model(&request("###Mapping\nMapping a::M\n(\n)\n"));
    assert_eq!(elements(&parsed), ["a::M 2:1-4:1"]);
    assert_eq!(
        sections(&parsed),
        [
            "importAware Pure 1:1-1:8 [] []",
            "importAware Mapping 2:8-6:2 [\"a::M\"] []",
        ]
    );
    // A header and nothing else; then at the end of the code.
    assert_eq!(
        sections(&model(&request("###Pure\n"))),
        [
            "importAware Pure 1:1-1:8 [] []",
            "importAware Pure 2:8-3:8 [] []",
        ]
    );
    assert_eq!(
        sections(&model(&request("Class a::A{}\n###Mapping"))),
        [
            "importAware Pure 1:1-3:12 [\"a::A\"] []",
            "importAware Mapping 3:13-3:23 [] []",
        ]
    );
}

#[test]
fn sections_of_the_same_type_are_listed_apart() {
    let parsed = model(&request("###Pure\nClass a::A{}\n###Pure\nClass a::B{}"));
    assert_eq!(elements(&parsed), ["a::A 2:1-2:12", "a::B 4:1-4:12"]);
    assert_eq!(
        sections(&parsed),
        [
            "importAware Pure 1:1-1:8 [] []",
            "importAware Pure 2:8-4:12 [\"a::A\"] []",
            "importAware Pure 4:13-6:12 [\"a::B\"] []",
        ]
    );
}

#[test]
fn a_grammar_without_imports_makes_a_default_section() {
    assert_eq!(
        sections(&model(&request("###Data\n"))),
        ["importAware Pure 1:1-1:8 [] []", "default Data 2:8-3:8 []",]
    );
    assert_eq!(
        sections(&model(&request("Class a::A{}\n###Data"))),
        [
            "importAware Pure 1:1-3:12 [\"a::A\"] []",
            "default Data 3:13-3:20 []",
        ]
    );
}

#[test]
fn code_after_a_header_on_its_line_is_positioned_from_the_header() {
    let parsed = model(&request("###Pure Class a::A{}\n"));
    assert_eq!(elements(&parsed), ["a::A 1:2-1:13"]);
    assert_eq!(
        sections(&parsed),
        [
            "importAware Pure 1:1-1:8 [] []",
            "importAware Pure 2:8-3:21 [\"a::A\"] []",
        ]
    );
    // Spaces after the name are the section's.
    assert_eq!(
        elements(&model(&request("###Mapping   \nMapping a::M\n(\n)\n"))),
        ["a::M 2:1-4:1"]
    );
    // Whatever the section's grammar, errors too.
    assert_eq!(
        elements(&model(&request("###Mapping Mapping a::M\n(\n)\n"))),
        ["a::M 1:2-3:1"]
    );
    assert_eq!(
        elements(&model(&request(
            "###Data Data a::D\n{\n  ExternalFormat\n  #{\n    contentType: 'application/json';\n    data: '{}';\n  }#\n}\n"
        ))),
        ["a::D 1:2-8:1"]
    );
    assert_eq!(
        error_at(&error(&request("###Mapping bad\n"))),
        (ErrorKind::Parser, Some((1, 2)))
    );
}

#[test]
fn paths_and_imports_are_listed_once() {
    let parsed = model(&request(
        "import a::*;\nimport a::*;\nClass a::A{}\nClass a::A{}\n",
    ));
    // The elements themselves all are.
    assert_eq!(elements(&parsed), ["a::A 3:1-3:12", "a::A 4:1-4:12"]);
    assert_eq!(
        sections(&parsed),
        ["importAware Pure 1:1-6:13 [\"a::A\"] [\"a\"]"]
    );
}

#[test]
fn lines_end_at_line_feeds_and_columns_count_code_points() {
    let parsed = model(&request(
        "Class a::A{}\r\n###Mapping\r\nMapping a::M\r\n(\r\n)\r\n",
    ));
    assert_eq!(elements(&parsed), ["a::A 1:1-1:12", "a::M 3:1-5:1"]);
    assert_eq!(
        sections(&parsed),
        [
            "importAware Pure 1:1-3:13 [\"a::A\"] []",
            "importAware Mapping 3:14-7:3 [\"a::M\"] []",
        ]
    );
    // The last token's end column adds its length in UTF-16 code units.
    assert_eq!(
        sections(&model(&request("Class a::A{}\n// \u{1F600}"))),
        ["importAware Pure 1:1-4:5 [\"a::A\"] []"]
    );
}

#[test]
fn offsets_shift_lines_and_the_first_line_s_columns() {
    let with_offsets = |code| ModelRequest {
        code,
        source_id: "src",
        line_offset: 5,
        column_offset: 7,
    };
    let parsed = model(&with_offsets(
        "###Mapping\nMapping a::M\n(\n)\n###Pure\nClass a::A{}\n",
    ));
    assert_eq!(elements(&parsed), ["a::M 7:1-9:1", "a::A 11:1-11:12"]);
    assert_eq!(
        sections(&parsed),
        [
            "importAware Pure 6:8-6:15 [] []",
            "importAware Mapping 7:8-11:1 [\"a::M\"] []",
            "importAware Pure 11:2-13:13 [\"a::A\"] []",
        ]
    );
    assert_eq!(position(&parsed[2]["sections"][0]).source_id, "src");
    let parsed = model(&with_offsets(
        "Class a::A{}\n###Mapping\nMapping a::M\n(\n)\n",
    ));
    assert_eq!(elements(&parsed), ["a::A 6:1-6:12", "a::M 8:1-10:1"]);
    assert_eq!(
        sections(&parsed),
        [
            "importAware Pure 6:8-8:12 [\"a::A\"] []",
            "importAware Mapping 8:13-12:2 [\"a::M\"] []",
        ]
    );
    let columns_only = ModelRequest {
        line_offset: 0,
        ..with_offsets("###Mapping\nMapping a::M\n(\n)\n")
    };
    assert_eq!(
        sections(&model(&columns_only)),
        [
            "importAware Pure 1:8-1:15 [] []",
            "importAware Mapping 2:8-6:2 [\"a::M\"] []",
        ]
    );
}

#[test]
fn a_header_must_start_a_line() {
    // Anywhere else, `###` is its section's text, which no grammar reads.
    for (code, at) in [
        ("Class a::A{}\n  ###Mapping\nClass a::B{}\n", (2, 3)),
        ("Class a::A{}\n\t###Mapping\n", (2, 2)),
        ("Class a::A{}\n###\nClass a::B{}\n", (2, 1)),
        ("###Pure  ###Mapping\nMapping a::M\n(\n)\n", (1, 3)),
        ("###Mapping\nMapping a::M\n(\n) ###Pure\n", (4, 3)),
    ] {
        assert_eq!(
            error_at(&error(&request(code))),
            (ErrorKind::Parser, Some(at)),
            "{code:?}"
        );
    }
    // A header in a string literal still splits the model.
    assert_eq!(
        error_at(&error(&request(
            "function a::f():String[1]\n{\n  'x\n###Mapping\ny'\n}\n"
        ))),
        (ErrorKind::Parser, Some((3, 3)))
    );
}

#[test]
fn the_first_section_in_error_reports_it() {
    let code = "Class a::A{}\n###Mapping\nMapping a::M\n(\n  bad\n)\n";
    assert_eq!(
        error_at(&error(&request(code))),
        (ErrorKind::Parser, Some((6, 1)))
    );
}

#[test]
fn what_rust_does_not_parse_is_unsupported() {
    // A section type without a linked grammar, whatever its name; the engine
    // knows it or reports it.
    for code in [
        "###Pure$x\nClass a::A{}\n",
        "###1abc\nClass a::A{}\n",
        "Class a::A{}\n###Bogus\nwhatever\n",
    ] {
        assert_eq!(
            error_at(&error(&request(code))),
            (ErrorKind::Unsupported, None),
            "{code:?}"
        );
    }
    // Offsets below zero.
    let negative = ModelRequest {
        line_offset: -1,
        ..request("Class a::A{}\n")
    };
    assert_eq!(error(&negative).kind, ErrorKind::Unsupported);
}

#[test]
fn json_shapes() {
    let ok: Value =
        serde_json::from_str(&parse_model_json(&request("Class a::A{}\n"))).expect("JSON");
    assert_eq!(ok["elements"][0]["_type"], "class");
    assert_eq!(ok["elements"][1]["_type"], "sectionIndex");
    let failed: Value =
        serde_json::from_str(&parse_model_json(&request("Class a::A{\n"))).expect("JSON");
    assert_eq!(failed["error"]["kind"], json!("parser"));
}
