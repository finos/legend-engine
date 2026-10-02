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

//! Linking this crate registers the TDS island with both the parser
//! (`ISLAND_PARSERS`) and the protocol converter (`ISLAND_PROTOCOLS`), so
//! plain `parse()` + `convert_source_file()` produce the engine's JSON.

use legend_pure_dsl_tds as _;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The JSON of the first expression of the function `source` defines.
fn first_body_value(source: &str) -> Value {
    let file = legend_pure_parser_parser::parse(source, "tds.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    let function = serde_json::to_value(&pmcd.elements[0]).expect("serializes");
    function["body"][0].clone()
}

#[test]
fn a_tds_literal_keeps_its_body_as_written() {
    // CSV, not Pure: `I'm` would not lex.
    let source =
        "function f(): Any[*]\n{\n  #TDS{\n    id, name:String[1]\n    1, I'm here\n  }#\n}";
    let tds = first_body_value(source);
    assert_eq!(tds["_type"], "classInstance");
    assert_eq!(tds["type"], "TDS");
    assert_eq!(
        tds["value"],
        json!({"tdsString": "\n    id, name:String[1]\n    1, I'm here\n  "})
    );
    // Positioned on the whole island, `#TDS{` to `}#`.
    let si: SourceInformation =
        serde_json::from_value(tds["sourceInformation"].clone()).expect("positioned");
    assert_eq!(si.start_line, 3);
    assert_eq!(si.start_column, 3);
    assert_eq!(si.end_line, 6);
    assert_eq!(si.end_column, 4);
}

#[test]
fn a_tds_literal_starts_a_chain() {
    let source = "function f(): Any[*]\n{\n  #TDS{a\n1}#->extend(~b:x|$x.a)\n}";
    let extend = first_body_value(source);
    assert_eq!(extend["function"], "extend");
    assert_eq!(extend["parameters"][0]["type"], "TDS");
    assert_eq!(
        extend["parameters"][0]["value"],
        json!({"tdsString": "a\n1"})
    );
}

#[test]
fn an_empty_tds_literal_has_empty_text() {
    let tds = first_body_value("function f(): Any[*] { #TDS{}# }");
    assert_eq!(tds["value"], json!({"tdsString": ""}));
}
