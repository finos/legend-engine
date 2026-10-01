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

//! Linking this crate registers the `Relational` embedded data grammar with
//! both the parser (`EMBEDDED_DATA_PARSERS`) and the protocol converter
//! (`EMBEDDED_DATA_PROTOCOLS`), so plain `parse()` + `convert_source_file()`
//! produce the engine's JSON.

use legend_pure_dsl_relational as _;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::Value;

/// The data of the first store of the first suite of the function `source`
/// defines.
fn test_data(source: &str) -> Value {
    let file = legend_pure_parser_parser::parse(source, "relational.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    let function = serde_json::to_value(&pmcd.elements[0]).expect("serializes");
    function["tests"][0]["testData"][0]["data"].clone()
}

fn span(node: &Value) -> (u64, u64, u64, u64) {
    let si = &node["sourceInformation"];
    let at = |key: &str| {
        si[key]
            .as_u64()
            .unwrap_or_else(|| panic!("no {key} in {node}"))
    };
    (
        at("startLine"),
        at("startColumn"),
        at("endLine"),
        at("endColumn"),
    )
}

#[test]
fn relational_data_keeps_one_csv_text_per_table() {
    let source = "function f(): Any[*]\n{\n  1\n}\n{\n  store::DB:\n    Relational\n    #{\n      default.PersonTable:\n        'id,name\\n'+\n        '1,I\\'m John\\n';\n      other.'My Table':\n        'a\\;b\\n';\n    }#;\n  t | f() => 1;\n}";
    let data = test_data(source);
    assert_eq!(data["_type"], "relationalCSVData");
    let tables = &data["tables"];
    assert_eq!(tables[0]["schema"], "default");
    assert_eq!(tables[0]["table"], "PersonTable");
    assert_eq!(tables[0]["values"], "id,name\n1,I'm John\n");
    assert_eq!(tables[1]["schema"], "other");
    assert_eq!(tables[1]["table"], "My Table");
    // `\;` is kept as written.
    assert_eq!(tables[1]["values"], "a\\;b\n");
    // The data spans `Relational` to `}#`; a table, its schema to `;`.
    assert_eq!(span(&data), (7, 5, 14, 6));
    assert_eq!(span(&tables[0]), (9, 7, 11, 24));
}

#[test]
fn a_table_needs_its_semicolon() {
    let source = "function f(): Any[*]\n{\n  1\n}\n{\n  store::DB: Relational #{ s.t: 'a' }#;\n  t | f() => 1;\n}";
    let error = legend_pure_parser_parser::parse(source, "relational.pure").expect_err("no `;`");
    let si = error.errors[0].source_info().expect("positioned");
    // At the end of the body.
    assert_eq!(si.start_line, 6);
}
