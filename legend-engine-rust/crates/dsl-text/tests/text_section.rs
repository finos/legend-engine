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

//! The `###Text` section, as the engine's `TextParseTreeWalker` reads it.

use legend_pure_dsl_text as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The elements of the section `###Text\n{body}`, its index last.
fn elements(body: &str) -> Vec<Value> {
    let source = format!("###Text\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "t.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|element| serde_json::to_value(element).expect("serializes"))
        .collect()
}

fn error(body: &str) -> ParseError {
    let source = format!("###Text\n{body}");
    legend_pure_parser_parser::parse(&source, "t.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    serde_json::to_value(SourceInformation {
        source_id: "t.pure".into(),
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
fn texts_with_their_type_and_content() {
    let elements = elements(
        "Text my::Readme\n{\n  type: 'mark down';\n  content: 'let\\'s \\n begin';\n}\n\nText my::Bare\n{\n  content: '';\n}\n",
    );
    assert_eq!(
        elements[0],
        json!({
            "_type": "text",
            "package": "my",
            "name": "Readme",
            "type": "mark down",
            "content": "let's \n begin",
            "sourceInformation": si(2, 1, 6, 1),
        })
    );
    // The type is optional.
    assert!(elements[1].get("type").is_none(), "{}", elements[1]);
    assert_eq!(elements[1]["content"], "");
    // The section has no imports.
    let section = &elements[2]["sections"][0];
    assert_eq!(section["_type"], "default");
    assert_eq!(section["elements"], json!(["my::Readme", "my::Bare"]));
}

#[test]
fn the_walker_checks_fields() {
    let content = error("Text my::T\n{\n  type: HTML;\n}\n");
    assert_eq!(content.message(), "Field 'content' is required");
    assert_eq!(position(&content), (2, 1, 5, 1));
    let types = error("Text my::T\n{\n  type: HTML;\n  content: 'c';\n  type: HTML;\n}\n");
    assert_eq!(
        types.message(),
        "Field 'type' should be specified only once"
    );
    // A field the grammar doesn't have.
    let field = error("Text my::T\n{\n  kind: HTML;\n}\n");
    assert_eq!(position(&field), (4, 3, 4, 6));
}

#[test]
fn the_grammars_keywords_are_identifiers() {
    let elements = elements("Text Text::type::content\n{\n  type: content;\n  content: 'c';\n}\n");
    assert_eq!(elements[0]["package"], "Text::type");
    assert_eq!(elements[0]["name"], "content");
    assert_eq!(elements[0]["type"], "content");
}
