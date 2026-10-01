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

//! Source positions follow the engine parser's per-construct conventions
//! (`DomainParseTreeWalker`), as the engine golden corpus
//! (`tests/engine-corpus`) found them.

use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::Value;

/// The converted elements of `code`, as JSON.
fn elements(code: &str) -> Vec<Value> {
    let file = legend_pure_parser_parser::parse(code, "positions.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|e| serde_json::to_value(e).expect("serializes"))
        .collect()
}

/// The source text `node`'s source information covers (1-based lines and
/// columns, inclusive end).
fn covered(code: &str, node: &Value) -> String {
    let si = &node["sourceInformation"];
    let at = |key: &str| {
        let n = si[key]
            .as_u64()
            .unwrap_or_else(|| panic!("no {key} in {node}"));
        usize::try_from(n).expect("position fits")
    };
    let (start_line, start_column) = (at("startLine"), at("startColumn"));
    let (end_line, end_column) = (at("endLine"), at("endColumn"));
    let lines: Vec<&str> = code.split('\n').collect();
    let mut text = String::new();
    for (i, line) in lines.iter().enumerate().take(end_line).skip(start_line - 1) {
        let from = if i + 1 == start_line {
            start_column - 1
        } else {
            0
        };
        let to = if i + 1 == end_line {
            end_column
        } else {
            line.chars().count()
        };
        if i + 1 > start_line {
            text.push('\n');
        }
        text.extend(line.chars().skip(from).take(to - from));
    }
    text
}

#[test]
fn a_lambda_spans_its_pipe_and_code_block() {
    // The engine's `lambdaPipe`: from `|` to the end of the code block, a
    // trailing `;` included; parameters and braces are outside it.
    let code = "function f(): Any[*]\n{\n  [1]->map(x|$x + 1)->map({y: Integer[1]|$y;})->filter(z: Integer[1]|true)->filter({|false})\n}";
    let body = &elements(code)[0]["body"][0];
    assert_eq!(covered(code, &body["parameters"][1]), "|false");
    let typed_bare = &body["parameters"][0];
    assert_eq!(covered(code, &typed_bare["parameters"][1]), "|true");
    let braced = &typed_bare["parameters"][0];
    assert_eq!(covered(code, &braced["parameters"][1]), "|$y;");
    let bare = &braced["parameters"][0];
    assert_eq!(covered(code, &bare["parameters"][1]), "|$x + 1");
}

#[test]
fn a_multi_line_lambda_ends_at_its_last_statement() {
    let code = "function f(): Any[*]\n{\n  {|\n    let a = 1;\n    $a;\n  }\n}";
    let lambda = &elements(code)[0]["body"][0];
    assert_eq!(covered(code, lambda), "|\n    let a = 1;\n    $a;");
}

#[test]
fn a_raw_type_spans_the_whole_type() {
    // The engine positions a raw type with its whole `type` rule: type
    // arguments, type variable values and unit included.
    let code = "Class A\n{\n  unit: NewMeasure~UnitOne[0..1];\n  list: List<String>[1];\n  name: VARCHAR(200)[1];\n}\n\nfunction f(): Relation<(a:Integer)>[0..1]\n{\n  []\n}";
    let elements = elements(code);
    let raw = |i: usize| &elements[0]["properties"][i]["genericType"]["rawType"];
    assert_eq!(covered(code, raw(0)), "NewMeasure~UnitOne");
    assert_eq!(covered(code, raw(1)), "List<String>");
    assert_eq!(covered(code, raw(2)), "VARCHAR(200)");
    assert_eq!(
        covered(code, &elements[1]["returnGenericType"]["rawType"]),
        "Relation<(a:Integer)>"
    );
}

#[test]
fn a_unit_spans_its_definition() {
    // The engine's `measureExpr` / `nonConvertibleMeasureExpr`: name to `;`,
    // without the canonical `*`.
    let code = "Measure NewMeasure\n{\n  *UnitOne: x -> $x;\n  UnitTwo: x -> $x * 1000;\n}\n\nMeasure Currency\n{\n  USD;\n  GBP;\n}";
    let elements = elements(code);
    assert_eq!(
        covered(code, &elements[0]["canonicalUnit"]),
        "UnitOne: x -> $x;"
    );
    assert_eq!(
        covered(code, &elements[0]["nonCanonicalUnits"][0]),
        "UnitTwo: x -> $x * 1000;"
    );
    assert_eq!(covered(code, &elements[1]["canonicalUnit"]), "USD;");
}

#[test]
fn get_all_spans_its_all_call() {
    // The engine positions `getAll` on `.all(…)` and the element on its name.
    let code = "function f(): Any[*]\n{\n  my::Person.all(%latest)->filter(p|$p.name == Firm)\n}";
    let filter = &elements(code)[0]["body"][0];
    let get_all = &filter["parameters"][0];
    assert_eq!(covered(code, get_all), ".all(%latest)");
    assert_eq!(covered(code, &get_all["parameters"][0]), "my::Person");
    assert_eq!(covered(code, &get_all["parameters"][1]), "%latest");
    let firm = &filter["parameters"][1]["body"][0]["parameters"][1];
    assert_eq!(covered(code, firm), "Firm");
}

#[test]
fn a_milestoned_property_latest_date_has_no_position() {
    // The engine's `functionExpressionLatestMilestoningDateParameter` builds
    // its `%latest` dates without positions; `.all(%latest)` positions them.
    let code = "function f(p: Any[1]): Any[*]\n{\n  [$p.name(%latest), my::Person.all(%latest)]\n}";
    let values = &elements(code)[0]["body"][0]["values"];
    assert!(
        values[0]["parameters"][1]
            .get("sourceInformation")
            .is_none()
    );
    assert_eq!(covered(code, &values[1]["parameters"][1]), "%latest");
}

#[test]
fn an_enum_value_spans_its_annotations_and_documentation() {
    let code = "Enum E\n{\n  '''\n  A value.\n  '''\n  A,\n  <<meta::pure::profiles::doc.deprecated>> {meta::pure::profiles::doc.doc = 'b'} B,\n  C\n}";
    let values = &elements(code)[0]["values"];
    assert_eq!(covered(code, &values[0]), "'''\n  A value.\n  '''\n  A");
    assert_eq!(
        covered(code, &values[1]),
        "<<meta::pure::profiles::doc.deprecated>> {meta::pure::profiles::doc.doc = 'b'} B"
    );
    assert_eq!(covered(code, &values[2]), "C");
}

#[test]
fn type_variable_values_are_positioned_literals() {
    let code = "Class A\n{\n  name: VARCHAR(200)[1];\n}";
    let value = &elements(code)[0]["properties"][0]["genericType"]["typeVariableValues"][0];
    assert_eq!(value["_type"], "integer");
    assert_eq!(covered(code, value), "200");
}

#[test]
fn a_column_spec_spans_its_specification_without_the_tilde() {
    let code = "function f(): Any[*]\n{\n  [~b:String, ~[d, e:Integer]]\n}";
    let values = &elements(code)[0]["body"][0]["values"];
    assert_eq!(covered(code, &values[0]), "b:String");
    assert_eq!(covered(code, &values[0]["value"]), "b:String");
    assert_eq!(covered(code, &values[1]), "[d, e:Integer]");
    assert_eq!(
        covered(code, &values[1]["value"]["colSpecs"][1]),
        "e:Integer"
    );
}

#[test]
fn a_default_value_spans_its_whole_expression() {
    // The engine's `defaultValueExpression`.
    let code = "Class A\n{\n  b: B[1] = ^my::B(name = 'x');\n  n: Integer[1] = 1 + 2;\n}";
    let properties = &elements(code)[0]["properties"];
    assert_eq!(
        covered(code, &properties[0]["defaultValue"]),
        "^my::B(name = 'x')"
    );
    assert_eq!(covered(code, &properties[1]["defaultValue"]), "1 + 2");
}

#[test]
fn function_tests_are_positioned_as_the_engine_does() {
    let code = "function my::hello(name: String[1]): String[1]\n{\n  $name\n}\n{\n  ModelStore: (JSON) '{}';\n  first | hello('x') => 'x';\n  S2\n  (\n    second | hello('y') => (XML) '<a/>';\n  )\n}";
    let function = &elements(code)[0];
    let block = "{\n  ModelStore: (JSON) '{}';\n  first | hello('x') => 'x';\n  S2\n  (\n    second | hello('y') => (XML) '<a/>';\n  )\n}";
    assert!(covered(code, function).ends_with(block));
    let suites = &function["tests"];
    assert_eq!(covered(code, &suites[0]), block);
    let data = &suites[0]["testData"][0];
    assert_eq!(covered(code, data), "ModelStore: (JSON) '{}';");
    assert_eq!(
        covered(code, &data["packageableElementPointer"]),
        "ModelStore"
    );
    assert_eq!(covered(code, &data["data"]), "(JSON) '{}'");
    let first = &suites[0]["tests"][0];
    assert_eq!(covered(code, first), "first | hello('x') => 'x';");
    assert_eq!(covered(code, &first["parameters"][0]), "'x'");
    assert_eq!(covered(code, &first["assertions"][0]), "'x'");
    assert_eq!(
        covered(code, &suites[1]),
        "S2\n  (\n    second | hello('y') => (XML) '<a/>';\n  )"
    );
    assert_eq!(
        covered(code, &suites[1]["tests"][0]["assertions"][0]),
        "(XML) '<a/>'"
    );
}

#[test]
fn navigation_paths_are_positioned_as_the_engine_does() {
    // The engine's nested path parser adds the whole `#/…#` token's length
    // to its column offset: every position is shifted right by that length
    // minus one, and the path ends past its text by `<EOF>`'s length.
    let code = "function f(): Any[*]\n{\n  #/model::Person/lastName#\n}";
    let path = &elements(code)[0]["body"][0];
    let si = |node: &Value| {
        let s = &node["sourceInformation"];
        (
            s["startColumn"].as_u64().unwrap(),
            s["endColumn"].as_u64().unwrap(),
        )
    };
    // The token spans columns 3 to 27 (length 25).
    assert_eq!(si(path), (28, 55));
    assert_eq!(si(&path["value"]), (28, 55));
    // `/lastName` spans columns 18 to 26.
    assert_eq!(si(&path["value"]["path"][0]), (42, 50));
}
