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

//! The `###FunctionJar` section, as the engine's `FunctionJarTreeWalker`
//! reads it.

use legend_pure_dsl_function_jar as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The elements of the section `###FunctionJar\n{body}`, its index last.
fn elements(body: &str) -> Vec<Value> {
    let source = format!("###FunctionJar\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "f.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|element| serde_json::to_value(element).expect("serializes"))
        .collect()
}

fn error(body: &str) -> ParseError {
    let source = format!("###FunctionJar\n{body}");
    legend_pure_parser_parser::parse(&source, "f.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    json!({
        "sourceId": "f.pure",
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
fn a_function_jar_with_its_annotations_and_fields() {
    let elements = elements(
        "import my::*;\n'''\nThe people\n'''\nFunctionJar {doc.owner = 'me'} my::People\n{\n  function: my::people():TabularDataSet[1];\n  ownership: Deployment { identifier: 'owner' };\n  documentation: 'Its people';\n  activationConfiguration: my::Connection;\n}\n",
    );
    let jar = &elements[0];
    assert_eq!(jar["_type"], "functionJar");
    assert_eq!(jar["package"], "my");
    assert_eq!(jar["name"], "People");
    // From the documentation, the first tagged value.
    assert_eq!(jar["sourceInformation"], si(3, 1, 12, 1));
    assert_eq!(jar["taggedValues"][0]["value"]["value"], "The people");
    assert_eq!(jar["taggedValues"][1]["value"], "me");
    assert_eq!(
        jar["function"],
        json!({"type": "FUNCTION", "path": "my::people():TabularDataSet[1]", "sourceInformation": si(8, 13, 8, 42)})
    );
    assert_eq!(
        jar["ownership"],
        json!({"_type": "DeploymentOwner", "id": "owner"})
    );
    assert_eq!(jar["documentation"], "Its people");
    // The walker leaves the activation configuration out.
    assert!(jar.get("activationConfiguration").is_none(), "{jar}");
    // The section has imports.
    let section = &elements[1]["sections"][0];
    assert_eq!(section["_type"], "importAware");
    assert_eq!(section["imports"], json!(["my"]));
    assert_eq!(section["elements"], json!(["my::People"]));
}

#[test]
fn the_walker_checks_fields() {
    let function = error("FunctionJar my::J\n{\n  ownership: Deployment { identifier: 'o' };\n}\n");
    assert_eq!(function.message(), "Field 'function' is required");
    assert_eq!(position(&function), (2, 1, 5, 1));
    let ownership = error("FunctionJar my::J\n{\n  function: my::f():String[1];\n}\n");
    assert_eq!(ownership.message(), "Field 'ownership' is required");
    let documentation = error(
        "FunctionJar my::J\n{\n  function: my::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n  documentation: 'a';\n  documentation: 'b';\n}\n",
    );
    assert_eq!(
        documentation.message(),
        "Field 'documentation' should be specified only once"
    );
}

#[test]
fn the_grammars_keywords_are_identifiers() {
    let elements = elements(
        "FunctionJar stereotypes::tags::FunctionJar::import::ownership::Deployment\n{\n  function: a::f():String[1];\n  ownership: Deployment { identifier: '17' };\n}\n",
    );
    assert_eq!(
        elements[0]["package"],
        "stereotypes::tags::FunctionJar::import::ownership"
    );
    assert_eq!(elements[0]["name"], "Deployment");
}
