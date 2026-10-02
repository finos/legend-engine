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

//! Standalone expressions, parsed as the engine's `PureGrammarParser`
//! parses lambdas, value specifications and graph fetch trees.

use legend_pure_dsl_graph as _;
use legend_pure_parser_api::{
    ErrorKind, ExpressionKind, ExpressionRequest, parse_expression, parse_expression_json,
};
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::Value;

fn request(kind: ExpressionKind, code: &str) -> ExpressionRequest<'_> {
    ExpressionRequest {
        kind,
        code,
        source_id: "query",
        line_offset: 0,
        column_offset: 0,
    }
}

/// Where a JSON node's source information starts and ends.
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
fn a_lone_lambda_is_the_result() {
    let lambda =
        parse_expression(&request(ExpressionKind::Lambda, "x: Integer[1]|$x + 1")).expect("parses");
    assert_eq!(lambda["_type"], "lambda");
    assert_eq!(lambda["parameters"][0]["name"], "x");
    assert_eq!(lambda["body"][0]["function"], "plus");
}

#[test]
fn statements_are_the_body_of_a_lambda() {
    // Positioned on the code block, from the first statement to the last `;`.
    let lambda =
        parse_expression(&request(ExpressionKind::Lambda, "let a = 1;\n$a + 1;")).expect("parses");
    assert_eq!(lambda["_type"], "lambda");
    assert!(lambda.get("parameters").is_none(), "{lambda}");
    assert_eq!(lambda["body"].as_array().map(Vec::len), Some(2));
    assert_eq!(span(&lambda), (1, 1, 2, 7));
}

#[test]
fn a_lambda_needs_a_statement() {
    let error = parse_expression(&request(ExpressionKind::Lambda, "  ")).expect_err("empty");
    assert_eq!(error.kind, ErrorKind::Parser, "{error:?}");
}

#[test]
fn a_value_specification_is_one_expression() {
    let collection =
        parse_expression(&request(ExpressionKind::ValueSpecification, "[1, 2]")).expect("parses");
    assert_eq!(collection["_type"], "collection");
    let error = parse_expression(&request(ExpressionKind::ValueSpecification, "1; 2"))
        .expect_err("two statements");
    assert_eq!(error.kind, ErrorKind::Parser, "{error:?}");
    let si = error.source_information.expect("positioned");
    assert_eq!((si.start_line, si.start_column), (1, 2));
}

#[test]
fn a_graph_fetch_is_its_island_tree() {
    let tree = parse_expression(&request(ExpressionKind::GraphFetch, "#{my::Person{name}}#"))
        .expect("parses");
    assert_eq!(tree["_type"], "rootGraphFetchTree");
    assert_eq!(tree["class"], "my::Person");
    let error =
        parse_expression(&request(ExpressionKind::GraphFetch, "1")).expect_err("not a graph fetch");
    assert_eq!(error.kind, ErrorKind::Parser, "{error:?}");
}

#[test]
fn offsets_shift_positions() {
    let mut shifted = request(ExpressionKind::Lambda, "|1");
    shifted.line_offset = 4;
    shifted.column_offset = 10;
    let lambda = parse_expression(&shifted).expect("parses");
    assert_eq!(span(&lambda), (5, 11, 5, 12));
}

#[test]
fn json_shapes() {
    let ok: Value = serde_json::from_str(&parse_expression_json(&request(
        ExpressionKind::ValueSpecification,
        "1",
    )))
    .expect("JSON");
    assert_eq!(ok["value"]["_type"], "integer");
    let failed: Value = serde_json::from_str(&parse_expression_json(&request(
        ExpressionKind::ValueSpecification,
        "1 +",
    )))
    .expect("JSON");
    assert_eq!(failed["error"]["kind"], "parser");
}

#[test]
fn a_column_spec_may_have_typed_lambdas() {
    // `~name: param: Type[m]|map : param: Type[m]|reduce`, the type a class,
    // a relation or a function type.
    for code in [
        "|$r->project(~[id:x: demo::Person[1]|$x.id, name:x: demo::Person[1]|$x.name])",
        "|$r->groupBy(~[id], ~count:row: (id:Integer[1], b:String[0..1])[1]|$row.id:y: Integer[*]|$y->count())",
        "|$r->extend(~f:x: {Integer[1]->String[1]}[1]|$x)",
    ] {
        let lambda = parse_expression(&request(ExpressionKind::Lambda, code))
            .unwrap_or_else(|e| panic!("{code}: {e:?}"));
        assert_eq!(lambda["_type"], "lambda", "{code}");
    }
}
