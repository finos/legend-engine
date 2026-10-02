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

//! Enumeration mappings, `my::E: EnumerationMapping id { A: 'a' }`, as the
//! engine's `EnumerationMappingParseTreeWalker` reads them.

use legend_pure_dsl_mapping as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The mapping `###Mapping\nMapping my::M\n(\n{element}\n)\n` defines.
fn mapping(element: &str) -> Value {
    let source = format!("###Mapping\nMapping my::M\n(\n{element}\n)\n");
    let file = legend_pure_parser_parser::parse(&source, "m.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

/// The first error of that mapping, and where it starts.
fn error(element: &str) -> (ParseError, (u32, u32)) {
    let source = format!("###Mapping\nMapping my::M\n(\n{element}\n)\n");
    let error = legend_pure_parser_parser::parse(&source, "m.pure")
        .expect_err("fails")
        .errors
        .remove(0);
    let si = error.source_info().expect("positioned").clone();
    (error, (si.start_line, si.start_column))
}

fn span(node: &Value) -> (u64, u64, u64, u64) {
    let si: SourceInformation = serde_json::from_value(node["sourceInformation"].clone())
        .unwrap_or_else(|e| panic!("no position in {node}: {e}"));
    (
        si.start_line.into(),
        si.start_column.into(),
        si.end_line.into(),
        si.end_column.into(),
    )
}

#[test]
fn values_map_strings_integers_and_enum_values() {
    let m = mapping(
        "  my::E: EnumerationMapping id\n  {\n    A: 'a\\tb',\n    B: ['b', -1, +2, my::Other.X],\n    'C D': 3\n  }",
    );
    assert_eq!(m["classMappings"], json!([]));
    let enumeration = &m["enumerationMappings"][0];
    // Not a class mapping: no `_type`.
    assert!(enumeration.get("_type").is_none());
    assert_eq!(enumeration["id"], "id");
    assert_eq!(enumeration["enumeration"]["type"], "ENUMERATION");
    assert_eq!(enumeration["enumeration"]["path"], "my::E");
    // Line 4, `  my::E: EnumerationMapping id`.
    assert_eq!(span(&enumeration["enumeration"]), (4, 3, 4, 7));
    // The whole element.
    assert_eq!(span(enumeration), (4, 3, 9, 3));
    let values = &enumeration["enumValueMappings"];
    assert_eq!(
        values[0],
        json!({"enumValue": "A", "sourceValues": [{"_type": "stringSourceValue", "value": "a\tb"}]})
    );
    assert_eq!(
        values[1]["sourceValues"],
        json!([
            {"_type": "stringSourceValue", "value": "b"},
            {"_type": "integerSourceValue", "value": -1},
            {"_type": "integerSourceValue", "value": 2},
            {"_type": "enumSourceValue", "enumeration": "my::Other", "value": "X"},
        ])
    );
    // A quoted value unquoted; values are not positioned (the engine ignores
    // their positions when it writes them).
    assert_eq!(
        values[2],
        json!({"enumValue": "C D", "sourceValues": [{"_type": "integerSourceValue", "value": 3}]})
    );
}

#[test]
fn an_enumeration_mapping_without_a_name_has_no_id() {
    let m = mapping("  *my::E[x]: EnumerationMapping\n  {\n  }");
    let enumeration = &m["enumerationMappings"][0];
    assert!(enumeration.get("id").is_none(), "{enumeration}");
    assert_eq!(enumeration["enumValueMappings"], json!([]));
}

#[test]
fn source_values_are_checked_as_the_grammar_checks_them() {
    // `[]` needs a value.
    let (_, at) = error("  my::E: EnumerationMapping\n  {\n    A: []\n  }");
    assert_eq!(at, (6, 9));
    // A sign belongs to the integer written right after it.
    let (_, at) = error("  my::E: EnumerationMapping\n  {\n    A: - 1\n  }");
    assert_eq!(at, (6, 8));
    // No floats.
    let (_, at) = error("  my::E: EnumerationMapping\n  {\n    A: 1.5\n  }");
    assert_eq!(at, (6, 8));
    // A trailing comma: the body ends where a value should be, at its `}`.
    let (_, at) = error("  my::E: EnumerationMapping\n  {\n    A: 1,\n  }");
    assert_eq!(at, (7, 3));
    // Java's `Integer.parseInt`.
    let (error, _) = error("  my::E: EnumerationMapping\n  {\n    A: 2147483648\n  }");
    assert!(error.message().contains("not a 32-bit integer"), "{error}");
}
