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

//! Linking this crate registers the SQL island with both the parser
//! (`ISLAND_PARSERS`) and the protocol converter (`ISLAND_PROTOCOLS`), so
//! plain `parse()` + `convert_source_file()` produce the engine's JSON.

use legend_pure_dsl_sql as _;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The JSON of the first expression of the function `source` defines.
fn first_body_value(source: &str) -> Value {
    let file = legend_pure_parser_parser::parse(source, "sql.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    let function = serde_json::to_value(&pmcd.elements[0]).expect("serializes");
    function["body"][0].clone()
}

#[test]
fn a_sql_expression_keeps_its_text_as_written() {
    // SQL, not Pure: the unterminated-looking quote spans lines.
    let source = "function f(): Any[*]\n{\n  #SQL{select a from csv('a,b\n1,2')}#\n}";
    let sql = first_body_value(source);
    assert_eq!(sql["_type"], "classInstance");
    assert_eq!(sql["type"], "SQL");
    assert_eq!(
        sql["value"],
        json!({"sql": "select a from csv('a,b\n1,2')"})
    );
    // Positioned on the whole island, `#SQL{` to `}#`.
    let si: SourceInformation =
        serde_json::from_value(sql["sourceInformation"].clone()).expect("positioned");
    assert_eq!(si.start_line, 3);
    assert_eq!(si.start_column, 3);
    assert_eq!(si.end_line, 4);
    assert_eq!(si.end_column, 7);
}

#[test]
fn a_sql_expression_starts_a_chain() {
    let source = "function f(): Any[*]\n{\n  #SQL{select * from t}#->filter(x|$x.a == 1)\n}";
    let filter = first_body_value(source);
    assert_eq!(filter["function"], "filter");
    assert_eq!(filter["parameters"][0]["type"], "SQL");
    assert_eq!(
        filter["parameters"][0]["value"],
        json!({"sql": "select * from t"})
    );
}
