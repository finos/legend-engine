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

//! The `###Snowflake` section, as the engine's `SnowflakeTreeWalker` reads
//! it.

use legend_pure_dsl_snowflake as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The elements of the section `###Snowflake\n{body}`, its index last.
fn elements(body: &str) -> Vec<Value> {
    let source = format!("###Snowflake\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "s.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|element| serde_json::to_value(element).expect("serializes"))
        .collect()
}

fn error(body: &str) -> ParseError {
    let source = format!("###Snowflake\n{body}");
    legend_pure_parser_parser::parse(&source, "s.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    serde_json::to_value(SourceInformation {
        source_id: "s.pure".into(),
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

#[test]
fn apps_come_before_udfs_and_configurations_are_left_out() {
    let elements = elements(
        "SnowflakeM2MUdf my::U\n{\n  udfName: 'u';\n  function: my::f(String[1]) : String[1];\n  ownership: Deployment { identifier: 'owner' };\n  deploymentSchema: 's';\n  deploymentStage: 'st';\n}\n\nSnowflakeAppDeploymentConfiguration my::Config\n{\n  activationConnection: my::Connection;\n}\n\n'''\nAn app\n'''\nSnowflakeApp <<doc.deprecated>> my::A\n{\n  applicationName: 'app';\n  description: 'Its people';\n  function: my::people():TabularDataSet[1];\n  ownership: Deployment { identifier: 'owner' };\n  usageRole: 'role';\n  permissionScheme: SEQUESTERED;\n  deploymentSchema: 'schema';\n  activationConfiguration: my::Connection;\n}\n",
    );
    let app = &elements[0];
    assert_eq!(app["_type"], "snowflakeApp");
    assert_eq!(app["name"], "A");
    // From the documentation, the first tagged value.
    assert_eq!(app["sourceInformation"], si(16, 1, 29, 1));
    assert_eq!(app["taggedValues"][0]["value"]["value"], "An app");
    assert_eq!(app["stereotypes"][0]["value"], "deprecated");
    assert_eq!(app["applicationName"], "app");
    assert_eq!(app["description"], "Its people");
    assert_eq!(app["usageRole"], "role");
    assert_eq!(app["permissionScheme"], "SEQUESTERED");
    assert_eq!(app["deploymentSchema"], "schema");
    // The function as written without spaces, positioned on itself.
    assert_eq!(
        app["function"],
        json!({"type": "FUNCTION", "path": "my::people():TabularDataSet[1]", "sourceInformation": si(23, 13, 23, 42)})
    );
    assert_eq!(
        app["ownership"],
        json!({"_type": "DeploymentOwner", "id": "owner"})
    );
    assert_eq!(
        app["activationConfiguration"],
        json!({
            "_type": "snowflakeDeploymentConfiguration",
            "activationConnection": {"_type": "connectionPointer", "connection": "my::Connection", "sourceInformation": si(28, 28, 28, 41)},
        })
    );
    let udf = &elements[1];
    assert_eq!(udf["_type"], "snowflakeM2MUdf");
    assert_eq!(udf["udfName"], "u");
    assert_eq!(udf["function"]["path"], "my::f(String[1]):String[1]");
    assert_eq!(udf["deploymentStage"], "st");
    // The deployment configuration is read and left out.
    assert_eq!(
        elements[2]["sections"][0]["elements"],
        json!(["my::A", "my::U"])
    );
}

#[test]
fn the_walker_checks_fields() {
    let name = error(
        "SnowflakeApp my::A\n{\n  function: my::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n}\n",
    );
    assert_eq!(name.message(), "Field 'applicationName' is required");
    assert_eq!(position(&name), (2, 1, 6, 1));
    let stage = error(
        "SnowflakeM2MUdf my::U\n{\n  udfName: 'u';\n  function: my::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n  deploymentSchema: 's';\n}\n",
    );
    assert_eq!(stage.message(), "Field 'deploymentStage' is required");
    let scheme = error(
        "SnowflakeApp my::A\n{\n  applicationName: 'app';\n  function: my::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n  permissionScheme: OPEN;\n}\n",
    );
    assert_eq!(scheme.message(), "Unknown permission scheme 'OPEN'");
    assert_eq!(position(&scheme), (7, 3, 7, 25));
}

#[test]
fn a_function_is_identified_by_its_signature() {
    let path = error("SnowflakeApp my::A\n{\n  applicationName: 'app';\n  function: my::f;\n}\n");
    assert_eq!(position(&path), (5, 18, 5, 18));
}
