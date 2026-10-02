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

//! The `###Diagram` section, as the engine's `DiagramParseTreeWalker`
//! reads it.

use legend_pure_dsl_diagram as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The diagram of the section `###Diagram\n{body}`.
fn diagram(body: &str) -> Value {
    let source = format!("###Diagram\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "d.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

fn error(body: &str) -> ParseError {
    let source = format!("###Diagram\n{body}");
    legend_pure_parser_parser::parse(&source, "d.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    serde_json::to_value(SourceInformation {
        source_id: "d.pure".into(),
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

/// A number as the engine's JSON writes it.
fn number(text: &str) -> Value {
    serde_json::from_str(text).expect("a number")
}

/// A diagram with one class view of `fields`.
fn class_view(fields: &str) -> String {
    format!("Diagram my::D\n{{\n  classView a\n  {{\n{fields}  }}\n}}\n")
}

#[test]
fn a_diagram_lays_out_its_views() {
    let diagram = diagram(
        "Diagram my::D\n{\n  generalizationView\n  {\n    source: 4cec85f9-9b66-450a-bdcb-c855aa0314e1;\n    target: firm;\n    points: [(.5,-12),(+3.25,2.23E10)];\n  }\n  classView 4cec85f9-9b66-450a-bdcb-c855aa0314e1\n  {\n    class: my::Person;\n    position: (100,50.5);\n    rectangle: (120.0,40);\n    hideProperties: true;\n    hideTaggedValue: false;\n  }\n  propertyView\n  {\n    property: my::Person.firm;\n    source: 4cec85f9-9b66-450a-bdcb-c855aa0314e1;\n    target: firm;\n    points: [];\n  }\n}\n",
    );
    assert_eq!(diagram["_type"], "diagram");
    assert_eq!(diagram["sourceInformation"], si(2, 1, 25, 1));
    // A view's id is read whole, as the engine's lexer reads it; a flag is
    // written only when set.
    assert_eq!(
        diagram["classViews"],
        json!([{
            "id": "4cec85f9-9b66-450a-bdcb-c855aa0314e1",
            "class": "my::Person",
            "classSourceInformation": si(12, 12, 12, 21),
            "position": {"x": number("100.0"), "y": number("50.5")},
            "rectangle": {"width": number("120.0"), "height": number("40.0")},
            "hideProperties": true,
            "sourceInformation": si(10, 3, 17, 3),
        }])
    );
    assert_eq!(
        diagram["propertyViews"],
        json!([{
            "property": {"class": "my::Person", "property": "firm", "sourceInformation": si(20, 15, 20, 24)},
            "line": {"points": []},
            "sourceView": "4cec85f9-9b66-450a-bdcb-c855aa0314e1",
            "sourceViewSourceInformation": si(21, 13, 21, 48),
            "targetView": "firm",
            "targetViewSourceInformation": si(22, 13, 22, 16),
            "sourceInformation": si(18, 3, 24, 3),
        }])
    );
    // Numbers signed, or without their integer part, written as Java
    // writes doubles.
    assert_eq!(
        diagram["generalizationViews"][0]["line"]["points"],
        json!([
            {"x": number("0.5"), "y": number("-12.0")},
            {"x": number("3.25"), "y": number("2.23E10")},
        ])
    );
}

#[test]
fn a_view_s_fields_are_checked_on_the_view() {
    let class = error(&class_view("    position: (0,0);\n    rectangle: (1,1);\n"));
    assert_eq!(class.message(), "Field 'class' is required");
    assert_eq!(position(&class), (4, 3, 8, 3));
    let target = error(
        "Diagram my::D\n{\n  propertyView\n  {\n    property: my::P.p;\n    source: a;\n    points: [];\n  }\n}\n",
    );
    assert_eq!(target.message(), "Field 'target' is required");
    assert_eq!(position(&target), (4, 3, 9, 3));
}

#[test]
fn tokens_are_the_engine_lexer_s() {
    // A number isn't a view's id.
    let id = error(
        "Diagram my::D\n{\n  classView 123\n  {\n    class: my::P;\n    position: (0,0);\n    rectangle: (1,1);\n  }\n}\n",
    );
    assert_eq!(position(&id), (4, 13, 4, 15));
    // A sign is written with its number.
    let sign = error(&class_view(
        "    class: my::P;\n    position: (- 1,0);\n    rectangle: (1,1);\n",
    ));
    assert_eq!(position(&sign), (7, 16, 7, 16));
    // Digits followed by letters are a view id, not a number.
    let digits = error(&class_view(
        "    class: my::P;\n    position: (12abc,0);\n    rectangle: (1,1);\n",
    ));
    assert_eq!(position(&digits), (7, 16, 7, 20));
}
