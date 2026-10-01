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

//! The `###Deephaven` section, as the engine's `DeephavenParseTreeWalker`
//! reads it.

use legend_pure_dsl_deephaven as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The elements of the section `###Deephaven\n{body}`, its index last.
fn elements(body: &str) -> Vec<Value> {
    let source = format!("###Deephaven\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "d.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|element| serde_json::to_value(element).expect("serializes"))
        .collect()
}

fn error(body: &str) -> ParseError {
    let source = format!("###Deephaven\n{body}");
    legend_pure_parser_parser::parse(&source, "d.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    json!({
        "sourceId": "d.pure",
        "startLine": start_line,
        "startColumn": start_column,
        "endLine": end_line,
        "endColumn": end_column,
    })
}

fn position(error: &ParseError) -> (u32, u32, u32, u32) {
    let si = error.source_info().expect("positioned");
    (si.start_line, si.start_column, si.end_line, si.end_column)
}

const APP: &str = "DeephavenApp my::App\n{\n  applicationName: 'trades';\n  description: 'Its trades';\n  function: my::trades(String[1], Integer[*]):Any[0 . . 1];\n  ownership: Deployment { identifier: 'owner' };\n}\n";

const STORE: &str = "Deephaven my::Trades\n(\n  Table trades\n  (\n    id: INT, 'unit price': DECIMAL(10, 2), at: TIMESTAMP, ok: BOOLEAN,\n    f: FLOAT, d: DOUBLE, s: STRING, t: DATETIME\n  )\n  Table 'other table' ( x: INT )\n)\n";

#[test]
fn stores_come_before_apps() {
    let elements = elements(&format!("import my::*;\n{APP}\n{STORE}"));
    let store = &elements[0];
    assert_eq!(store["_type"], "deephavenStore");
    assert_eq!(store["package"], "my");
    assert_eq!(store["name"], "Trades");
    assert_eq!(store["sourceInformation"], si(11, 1, 19, 1));
    let types = |table: &Value| -> Vec<Value> {
        table["columns"]
            .as_array()
            .expect("columns")
            .iter()
            .map(|column| {
                assert_eq!(column["_type"], "column");
                json!([column["name"], column["type"]])
            })
            .collect()
    };
    assert_eq!(store["tables"][0]["name"], "trades");
    assert_eq!(
        types(&store["tables"][0]),
        vec![
            json!(["id", {"_type": "intType"}]),
            json!(["unit price", {"_type": "decimalType", "precision": 10, "scale": 2}]),
            json!(["at", {"_type": "timestampType"}]),
            json!(["ok", {"_type": "booleanType"}]),
            json!(["f", {"_type": "floatType"}]),
            json!(["d", {"_type": "doubleType"}]),
            json!(["s", {"_type": "stringType"}]),
            json!(["t", {"_type": "dateTimeType"}]),
        ]
    );
    // A quoted name, unquoted.
    assert_eq!(store["tables"][1]["name"], "other table");
    assert_eq!(
        types(&store["tables"][1]),
        vec![json!(["x", {"_type": "intType"}])]
    );
    let app = &elements[1];
    assert_eq!(app["_type"], "DeephavenApp");
    assert_eq!(app["name"], "App");
    assert_eq!(app["sourceInformation"], si(3, 1, 9, 1));
    assert_eq!(app["applicationName"], "trades");
    assert_eq!(app["description"], "Its trades");
    // The function as written without spaces, positioned on itself.
    assert_eq!(
        app["function"],
        json!({"type": "FUNCTION", "path": "my::trades(String[1],Integer[*]):Any[0..1]", "sourceInformation": si(7, 13, 7, 58)})
    );
    assert_eq!(
        app["ownership"],
        json!({"_type": "DeploymentOwner", "id": "owner"})
    );
    // The section has imports, and lists its elements in the same order.
    let section = &elements[2]["sections"][0];
    assert_eq!(section["_type"], "importAware");
    assert_eq!(section["imports"], json!(["my"]));
    assert_eq!(section["elements"], json!(["my::Trades", "my::App"]));
}

#[test]
fn the_walker_checks_tables_and_columns() {
    let tables = error("Deephaven my::S\n(\n)\n");
    assert_eq!(tables.message(), "Field ''tables'' is required");
    assert_eq!(position(&tables), (2, 1, 4, 1));
    let columns = error("Deephaven my::S\n(\n  Table t ( )\n)\n");
    assert_eq!(columns.message(), "Field ''columns'' is required");
    assert_eq!(position(&columns), (4, 3, 4, 13));
    // Columns not separated by commas are another list of them.
    let lists = error("Deephaven my::S\n(\n  Table t ( a: INT b: INT )\n)\n");
    assert_eq!(
        lists.message(),
        "Field ''columns'' should be specified only once"
    );
    assert_eq!(position(&lists), (4, 3, 4, 27));
}

#[test]
fn the_walker_checks_an_apps_fields() {
    let name = error(
        "DeephavenApp my::A\n{\n  function: my::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n}\n",
    );
    assert_eq!(name.message(), "Field 'applicationName' is required");
    assert_eq!(position(&name), (2, 1, 6, 1));
    let owner = error(
        "DeephavenApp my::A\n{\n  applicationName: 'a';\n  function: my::f():String[1];\n}\n",
    );
    assert_eq!(owner.message(), "Field 'ownership' is required");
    let description = error(
        "DeephavenApp my::A\n{\n  description: 'd';\n  applicationName: 'a';\n  function: my::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n  description: 'e';\n}\n",
    );
    assert_eq!(
        description.message(),
        "Field 'description' should be specified only once"
    );
    assert_eq!(position(&description), (2, 1, 9, 1));
}

#[test]
fn the_section_is_read_before_it_is_walked() {
    // The walker walks the stores first.
    let store = error("DeephavenApp my::A\n{\n}\nDeephaven my::S\n(\n)\n");
    assert_eq!(store.message(), "Field ''tables'' is required");
    // A syntax error comes before any of the walker's.
    let syntax = error("Deephaven my::S\n(\n)\nDeephavenApp my::A\n{\n  name: 'a';\n}\n");
    assert_eq!(position(&syntax), (7, 3, 7, 6));
}

#[test]
fn names_are_not_the_grammars_keywords() {
    let keyword = error("Deephaven my::S\n(\n  Table t ( description: STRING )\n)\n");
    assert_eq!(position(&keyword), (4, 13, 4, 23));
    let table = error("Deephaven my::S\n(\n  Table Table ( a: INT )\n)\n");
    assert_eq!(position(&table), (4, 9, 4, 13));
    let column_type = error("Deephaven my::S\n(\n  Table t ( a: int )\n)\n");
    assert_eq!(position(&column_type), (4, 16, 4, 18));
}

#[test]
fn multiplicities_are_the_grammars_own() {
    let app = |function: &str| {
        format!(
            "DeephavenApp my::A\n{{\n  applicationName: 'a';\n  function: {function};\n  ownership: Deployment {{ identifier: 'o' }};\n}}\n"
        )
    };
    // The engine's lexer reads `..` as one token, which the grammar doesn't
    // take, and `.1` as a float.
    let dot_dot = error(&app("my::f():String[0..1]"));
    assert_eq!(dot_dot.message(), "Unexpected token '..'");
    assert_eq!(position(&dot_dot), (5, 29, 5, 30));
    let float = error(&app("my::f():String[1 . .2]"));
    assert_eq!(float.message(), "Unexpected token '.2'");
    assert_eq!(position(&float), (5, 32, 5, 33));
    let star = elements(&app("my::f(Integer[1 . . *]):String[*]"));
    assert_eq!(
        star[0]["function"]["path"],
        "my::f(Integer[1..*]):String[*]"
    );
    // A type is a qualified name: no type arguments.
    let arguments = error(&app("my::f(List<String>[1]):String[1]"));
    assert_eq!(position(&arguments), (5, 23, 5, 23));
}

#[test]
fn a_decimal_too_large_for_a_long_is_unsupported() {
    let large = error("Deephaven my::S\n(\n  Table t ( a: DECIMAL(99999999999999999999, 2) )\n)\n");
    assert!(matches!(large, ParseError::Unsupported { .. }), "{large:?}");
    assert_eq!(position(&large), (4, 24, 4, 43));
}
