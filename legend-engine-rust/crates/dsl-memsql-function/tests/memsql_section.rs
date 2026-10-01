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

//! The `###MemSql` section, as the engine's `MemSqlFunctionTreeWalker`
//! reads it.

use legend_pure_dsl_memsql_function as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The elements of the section `###MemSql\n{body}`, its index last.
fn elements(body: &str) -> Vec<Value> {
    let source = format!("###MemSql\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "m.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|element| serde_json::to_value(element).expect("serializes"))
        .collect()
}

fn error(body: &str) -> ParseError {
    let source = format!("###MemSql\n{body}");
    legend_pure_parser_parser::parse(&source, "m.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    json!({
        "sourceId": "m.pure",
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

#[test]
fn a_function_with_its_annotations_and_fields() {
    let elements = elements(
        "'''\nPeople\n'''\nMemSqlFunction <<doc.deprecated>> {doc.owner = 'me'} my::People\n{\n  functionName: 'people';\n  description: 'Its people';\n  function: my::people(String[0..1]):TabularDataSet[1];\n  ownership: Deployment { identifier: 'owner' };\n  activationConfiguration: 'my'::Connection;\n}\n",
    );
    let function = &elements[0];
    assert_eq!(function["_type"], "memSqlFunction");
    assert_eq!(function["package"], "my");
    assert_eq!(function["name"], "People");
    // From the documentation, the first tagged value.
    assert_eq!(function["sourceInformation"], si(2, 1, 12, 1));
    assert_eq!(function["taggedValues"][0]["value"]["value"], "People");
    // A one-line value is a plain string.
    assert_eq!(function["taggedValues"][1]["value"], "me");
    assert_eq!(function["stereotypes"][0]["value"], "deprecated");
    assert_eq!(function["functionName"], "people");
    assert_eq!(function["description"], "Its people");
    // The function as written without spaces, positioned on itself.
    assert_eq!(
        function["function"],
        json!({"type": "FUNCTION", "path": "my::people(String[0..1]):TabularDataSet[1]", "sourceInformation": si(9, 13, 9, 54)})
    );
    assert_eq!(
        function["ownership"],
        json!({"_type": "DeploymentOwner", "id": "owner"})
    );
    // The connection as written, unpositioned.
    assert_eq!(
        function["activationConfiguration"],
        json!({
            "_type": "memSqlFunctionConfig",
            "activationConnection": {"_type": "connectionPointer", "connection": "'my'::Connection"},
        })
    );
    // The section has no imports.
    let section = &elements[1]["sections"][0];
    assert_eq!(section["_type"], "default");
    assert_eq!(section["elements"], json!(["my::People"]));
}

#[test]
fn the_walker_checks_fields() {
    let name = error(
        "MemSqlFunction my::F\n{\n  function: my::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n}\n",
    );
    assert_eq!(name.message(), "Field 'functionName' is required");
    assert_eq!(position(&name), (2, 1, 6, 1));
    let ownership =
        error("MemSqlFunction my::F\n{\n  functionName: 'f';\n  function: my::f():String[1];\n}\n");
    assert_eq!(ownership.message(), "Field 'ownership' is required");
    let activation = error(
        "MemSqlFunction my::F\n{\n  functionName: 'f';\n  function: my::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n  activationConfiguration: my::C;\n  activationConfiguration: my::D;\n}\n",
    );
    assert_eq!(
        activation.message(),
        "Field 'activationConfiguration' should be specified only once"
    );
    assert_eq!(position(&activation), (2, 1, 9, 1));
}

#[test]
fn a_deployment_configuration_is_unsupported_after_the_functions() {
    let configuration = "MemSqlFunctionDeploymentConfiguration my::C\n{\n  activationConnection: my::Connection;\n}\n";
    // The engine makes it an element without a name.
    let unsupported = error(configuration);
    assert!(
        matches!(unsupported, ParseError::Unsupported { .. }),
        "{unsupported:?}"
    );
    assert_eq!(position(&unsupported), (2, 1, 5, 1));
    // The walker walks the functions first.
    let function = error(&format!(
        "{configuration}MemSqlFunction my::F\n{{\n  function: my::f():String[1];\n}}\n"
    ));
    assert_eq!(function.message(), "Field 'functionName' is required");
}

#[test]
fn the_grammars_keywords_are_identifiers() {
    let elements = elements(
        "MemSqlFunction function::let::all::toBytes::MemSqlFunction\n{\n  functionName: 'f';\n  function: ownership::identifier(Deployment[1]):description[*];\n  ownership: Deployment { identifier: 'o' };\n}\n",
    );
    assert_eq!(elements[0]["package"], "function::let::all::toBytes");
    assert_eq!(elements[0]["name"], "MemSqlFunction");
    assert_eq!(
        elements[0]["function"]["path"],
        "ownership::identifier(Deployment[1]):description[*]"
    );
}
