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

//! The `###HostedService` section, as the engine's `HostedServiceTreeWalker`
//! reads it.

use legend_pure_dsl_hosted_service as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The elements of the section `###HostedService\n{body}`, its index last.
fn elements(body: &str) -> Vec<Value> {
    let source = format!("###HostedService\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "h.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|element| serde_json::to_value(element).expect("serializes"))
        .collect()
}

fn error(body: &str) -> ParseError {
    let source = format!("###HostedService\n{body}");
    legend_pure_parser_parser::parse(&source, "h.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    serde_json::to_value(SourceInformation {
        source_id: "h.pure".into(),
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

/// A hosted service `my::S` with the fields it requires, then `fields`.
fn service(fields: &str) -> String {
    format!(
        "HostedService my::S\n{{\n  pattern: '/p';\n  function: my::f():String[1];\n  ownership: Deployment {{ identifier: 'o' }};\n{fields}}}\n"
    )
}

#[test]
fn a_hosted_service_with_its_annotations_and_fields() {
    let elements = elements(
        "import my::*;\nHostedService <<doc.deprecated>> my::People\n{\n  pattern: '/people/{name}';\n  ownership: UserList { users: ['alice', 'bob'] };\n  function: my::people(String[1]):TabularDataSet[1];\n  documentation: 'The people of a name';\n  autoActivateUpdates: true;\n}\n",
    );
    let service = &elements[0];
    assert_eq!(service["_type"], "hostedService");
    assert_eq!(service["package"], "my");
    assert_eq!(service["name"], "People");
    assert_eq!(service["sourceInformation"], si(3, 1, 10, 1));
    assert_eq!(service["stereotypes"][0]["value"], "deprecated");
    assert_eq!(service["pattern"], "/people/{name}");
    assert_eq!(
        service["ownership"],
        json!({"_type": "userList", "users": ["alice", "bob"]})
    );
    assert_eq!(
        service["function"],
        json!({"type": "FUNCTION", "path": "my::people(String[1]):TabularDataSet[1]", "sourceInformation": si(7, 13, 7, 51)})
    );
    assert_eq!(service["documentation"], "The people of a name");
    assert_eq!(service["autoActivateUpdates"], true);
    // The section has imports.
    let section = &elements[1]["sections"][0];
    assert_eq!(section["_type"], "importAware");
    assert_eq!(section["imports"], json!(["my"]));
    assert_eq!(section["elements"], json!(["my::People"]));
}

#[test]
fn the_walker_writes_its_defaults_and_leaves_fields_out() {
    let elements = elements(&service(
        "  binding: my::B;;\n  contentType: 'x';\n  generateLineage: true;\n  storeModel: true;\n  activationConfiguration: my::C;\n  actions: [];\n",
    ));
    let hosted = &elements[0];
    assert_eq!(
        hosted["ownership"],
        json!({"_type": "DeploymentOwner", "id": "o"})
    );
    // `autoActivateUpdates` not given is false; the walker reads neither
    // `generateLineage` nor `storeModel`.
    assert_eq!(hosted["autoActivateUpdates"], false);
    assert_eq!(hosted["generateLineage"], false);
    assert_eq!(hosted["storeModel"], false);
    for field in [
        "binding",
        "contentType",
        "activationConfiguration",
        "documentation",
    ] {
        assert!(hosted.get(field).is_none(), "{field}: {hosted}");
    }
    // A binding's rule has its own `;`, before the field's.
    let binding = error(&service("  binding: my::B;\n"));
    assert_eq!(position(&binding), (8, 1, 8, 1));
}

#[test]
fn the_walker_checks_fields() {
    let ownership =
        error("HostedService my::S\n{\n  pattern: '/p';\n  function: my::f():String[1];\n}\n");
    assert_eq!(ownership.message(), "Field 'ownership' is required");
    assert_eq!(position(&ownership), (2, 1, 6, 1));
    let documentation = error(&service("  documentation: 'a';\n  documentation: 'b';\n"));
    assert_eq!(
        documentation.message(),
        "Field 'documentation' should be specified only once"
    );
}

#[test]
fn what_rust_does_not_read_is_unsupported() {
    // No grammar of the engine's reads a post-deployment action.
    let action = error(&service("  actions: [Foo #{ x }#];\n"));
    assert!(
        matches!(action, ParseError::Unsupported { .. }),
        "{action:?}"
    );
    assert_eq!(position(&action), (7, 13, 7, 23));
    // The walker leaves test suites and post validations out.
    let suites = error(&service("  testSuites: [ ]\n"));
    assert!(
        matches!(suites, ParseError::Unsupported { .. }),
        "{suites:?}"
    );
    let validations = error(&service("  postValidations: [ ]\n"));
    assert!(
        matches!(validations, ParseError::Unsupported { .. }),
        "{validations:?}"
    );
    // The walker makes a deployment configuration an element without a
    // name.
    let configuration = error("HostedServiceDeploymentConfiguration my::C\n{\n}\n");
    assert!(
        matches!(configuration, ParseError::Unsupported { .. }),
        "{configuration:?}"
    );
    assert_eq!(position(&configuration), (2, 1, 4, 1));
}

#[test]
fn execution_environments() {
    let singles = elements(
        "ExecutionEnvironment my::E\n{\n  executions: [\n    a: { mapping: my::M; runtime: my::R; },\n    'b c': { mapping: my::M; runtime: #{ mappings: [my::M]; connections: []; }#; }\n  ];\n}\n",
    );
    let environment = &singles[0];
    assert_eq!(environment["_type"], "executionEnvironmentInstance");
    assert_eq!(environment["sourceInformation"], si(2, 1, 8, 1));
    let a = &environment["executionParameters"][0];
    assert_eq!(a["_type"], "singleExecutionParameters");
    assert_eq!(a["key"], "a");
    assert_eq!(a["sourceInformation"], si(5, 5, 5, 42));
    assert_eq!(a["mapping"], "my::M");
    assert_eq!(a["mappingSourceInformation"], si(5, 19, 5, 23));
    assert_eq!(
        a["runtime"],
        json!({"_type": "runtimePointer", "runtime": "my::R", "sourceInformation": si(5, 35, 5, 39)})
    );
    // A key with spaces, which the service walker rejects; a runtime of its
    // own.
    let b = &environment["executionParameters"][1];
    assert_eq!(b["key"], "b c");
    assert_eq!(b["sourceInformation"], si(6, 5, 6, 82));
    assert_eq!(b["runtime"]["_type"], "engineRuntime");
    assert_eq!(b["runtime"]["sourceInformation"], si(6, 42, 6, 76));
    assert_eq!(
        b["runtime"]["mappings"][0]["sourceInformation"],
        si(6, 53, 6, 57)
    );
    let groups = elements(
        "ExecutionEnvironment my::E\n{\n  executions: [ g: [ a: { mapping: my::M; runtime: my::R; } ] ];\n}\n",
    );
    let group = &groups[0]["executionParameters"][0];
    assert_eq!(group["_type"], "multiExecutionParameters");
    assert_eq!(group["masterKey"], "g");
    assert_eq!(
        group["singleExecutionParameters"][0]["sourceInformation"],
        si(4, 22, 4, 59)
    );
    // The engine fails on a group among single executions.
    let mixed = error(
        "ExecutionEnvironment my::E\n{\n  executions: [ g: [ a: { mapping: my::M; runtime: my::R; } ], b: { mapping: my::M; runtime: my::R; } ];\n}\n",
    );
    assert!(matches!(mixed, ParseError::Unsupported { .. }), "{mixed:?}");
}
