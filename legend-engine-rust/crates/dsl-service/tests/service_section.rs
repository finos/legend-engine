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

//! The `###Service` section, as the engine's `ServiceParseTreeWalker` reads
//! it.

use legend_pure_dsl_connection as _;
use legend_pure_dsl_service as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The elements of the section `###Service\n{body}`.
fn elements(body: &str) -> Vec<Value> {
    let source = format!("###Service\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "s.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|element| serde_json::to_value(element).expect("serializes"))
        .collect()
}

fn error(body: &str) -> ParseError {
    let source = format!("###Service\n{body}");
    legend_pure_parser_parser::parse(&source, "s.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    json!({
        "sourceId": "s.pure",
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

/// A service of `fields`, from line 4 on, with a pattern, documentation and
/// execution unless `fields` has them.
fn service(fields: &str) -> String {
    format!("Service my::S\n{{\n{fields}\n}}\n")
}

const BASICS: &str = "  pattern: 'p';\n  documentation: 'd';";

const SINGLE: &str = "  execution: Single { query: |1; }";

#[test]
fn a_service_names_its_pattern_owners_and_execution() {
    let source = "Service <<my::P.s>> {my::P.t = 'v'} my::S\n{\n  pattern: '/p/{x}';\n  title: 'T';\n  owners: ['a', 'b'];\n  ownership: DID { identifier: 'd1' };\n  documentation: 'Doc';\n  autoActivateUpdates: true;\n  mcpServer: server_1;\n  execution: Single\n  {\n    query: x: String[1]|$x;\n    mapping: my::M;\n    runtime: my::R;\n  }\n}\n";
    let service = &elements(source)[0];
    assert_eq!(service["_type"], "service");
    assert_eq!(service["package"], "my");
    assert_eq!(service["name"], "S");
    assert_eq!(service["pattern"], "/p/{x}");
    assert_eq!(service["title"], "T");
    assert_eq!(service["owners"], json!(["a", "b"]));
    assert_eq!(
        service["ownership"],
        json!({"_type": "deploymentOwnership", "identifier": "d1"})
    );
    assert_eq!(service["documentation"], "Doc");
    assert_eq!(service["autoActivateUpdates"], true);
    assert_eq!(service["mcpServer"], "server_1");
    assert_eq!(service["postValidations"], json!([]));
    // The walker positions a stereotype on its value.
    assert_eq!(
        service["stereotypes"][0]["sourceInformation"],
        si(2, 17, 2, 17)
    );
    assert_eq!(service["sourceInformation"], si(2, 1, 17, 1));
    let execution = &service["execution"];
    assert_eq!(execution["_type"], "pureSingleExecution");
    assert_eq!(execution["func"]["_type"], "lambda");
    assert_eq!(execution["func"]["parameters"][0]["name"], "x");
    assert_eq!(execution["mapping"], "my::M");
    assert_eq!(execution["mappingSourceInformation"], si(14, 14, 14, 18));
    assert_eq!(
        execution["runtime"],
        json!({"_type": "runtimePointer", "runtime": "my::R", "sourceInformation": si(15, 14, 15, 18)})
    );
    assert_eq!(execution["sourceInformation"], si(11, 14, 16, 3));
}

#[test]
fn absent_fields_are_the_walker_s_defaults() {
    let walked = &elements(&service(&format!("{BASICS}\n{SINGLE}")))[0];
    // Without the field, updates are not activated, and there are no owners.
    assert_eq!(walked["autoActivateUpdates"], false);
    assert_eq!(walked["owners"], json!([]));
    for absent in ["title", "ownership", "testSuites", "test", "mcpServer"] {
        assert!(walked.get(absent).is_none(), "{absent}");
    }
    // Neither a mapping nor a runtime.
    let execution = &walked["execution"];
    assert!(execution.get("mapping").is_none());
    assert!(execution.get("runtime").is_none());
    // A query that isn't a lambda is the body of one, unpositioned.
    let query = &elements(&service(&format!(
        "{BASICS}\n  execution: Single {{ query: 1; }}"
    )))[0]["execution"]["func"];
    assert_eq!(query["_type"], "lambda");
    assert!(query.get("sourceInformation").is_none());
    assert_eq!(query["body"][0]["_type"], "integer");
}

#[test]
fn the_walker_checks_fields_in_its_order_after_the_grammar() {
    let no_pattern = error(&service(&format!("  documentation: 'd';\n{SINGLE}")));
    assert_eq!(no_pattern.message(), "Field 'pattern' is required");
    assert_eq!(position(&no_pattern), (2, 1, 6, 1));
    let twice = error(&service(&format!(
        "{BASICS}\n  title: 'a';\n  title: 'b';\n{SINGLE}"
    )));
    assert_eq!(
        twice.message(),
        "Field 'title' should be specified only once"
    );
    // A syntax error comes first, wherever it is.
    let syntax = error(&service(
        "  documentation: 'd';\n  execution: Single { query: |1 +; }",
    ));
    assert_eq!(position(&syntax), (5, 34, 5, 34));
    // A mapping and a runtime come together, reported on the execution.
    let no_runtime = error(&service(&format!(
        "{BASICS}\n  execution: Single {{ query: |1; mapping: my::M; }}"
    )));
    assert_eq!(no_runtime.message(), "Field 'runtime' is required");
    assert_eq!(position(&no_runtime), (6, 14, 6, 50));
    let invalid_server = error(&service(&format!(
        "{BASICS}\n{SINGLE}\n  mcpServer: 'a b';"
    )));
    assert_eq!(
        invalid_server.message(),
        "Service mcpServer should be a valid identifier ([a-zA-Z_][a-zA-Z0-9_]*)"
    );
    assert_eq!(position(&invalid_server), (7, 3, 7, 19));
}

#[test]
fn a_runtime_of_its_own_is_read_by_the_runtime_grammar() {
    let execution = &elements(&service(&format!(
        "{BASICS}\n  execution: Single\n  {{\n    query: |1;\n    mapping: my::M;\n    runtime:\n    #{{\n      mappings: [my::M];\n      connections: [my::Store: [c: my::C]];\n    }}#;\n  }}"
    )))[0]["execution"];
    let runtime = &execution["runtime"];
    assert_eq!(runtime["_type"], "engineRuntime");
    assert_eq!(runtime["mappings"][0]["path"], "my::M");
    assert_eq!(
        runtime["connections"][0]["storeConnections"][0]["connection"]["connection"],
        "my::C"
    );
    // From its first field to its last.
    assert_eq!(runtime["sourceInformation"], si(12, 7, 13, 43));
    // One without fields is empty, reported from `#{` to `;`; what follows
    // the fields is not read.
    for body in [" ", " foo "] {
        let empty = error(&service(&format!(
            "{BASICS}\n  execution: Single {{ query: |1; mapping: my::M; runtime: #{{{body}}}#; }}"
        )));
        assert_eq!(empty.message(), "Embedded runtime must not be empty");
        let end = 63 + u32::try_from(body.len()).expect("short");
        assert_eq!(position(&empty), (6, 59, 6, end));
    }
    let rest = &elements(&service(&format!(
        "{BASICS}\n  execution: Single {{ query: |1; mapping: my::M; runtime: #{{ mappings: [my::M]; foo }}#; }}"
    )))[0];
    assert_eq!(rest["execution"]["runtime"]["mappings"][0]["path"], "my::M");
}

#[test]
fn a_multi_execution_runs_with_a_mapping_and_runtime_per_key() {
    let execution = &elements(&service(&format!(
        "{BASICS}\n  execution: Multi\n  {{\n    query: |1;\n    key: 'env';\n    executions['QA']:\n    {{\n      mapping: my::M;\n      runtime: my::R;\n    }}\n  }}"
    )))[0]["execution"];
    assert_eq!(execution["_type"], "pureMultiExecution");
    assert_eq!(execution["executionKey"], "env");
    assert_eq!(
        execution["executionParameters"],
        json!([{
            "key": "QA",
            "mapping": "my::M",
            "mappingSourceInformation": si(12, 16, 12, 20),
            "runtime": {"_type": "runtimePointer", "runtime": "my::R", "sourceInformation": si(13, 16, 13, 20)},
            "sourceInformation": si(10, 5, 14, 5),
        }])
    );
    let no_key = error(&service(&format!(
        "{BASICS}\n  execution: Multi {{ query: |1; executions['QA']: {{ mapping: my::M; runtime: my::R; }} }}"
    )));
    assert_eq!(no_key.message(), "Field 'key' is required");
}

#[test]
fn test_suites_give_connections_data_and_assert_results() {
    let suites = &elements(&service(&format!(
        "{BASICS}\n{SINGLE}\n  testSuites:\n  [\n    suite:\n    {{\n      data:\n      [\n        connections:\n        [\n          my::C: Reference #{{ my::Data }}#\n        ]\n      ]\n      tests:\n      [\n        test:\n        {{\n          parameters:\n          [\n            p = 1, q = toBytes('ab')\n          ]\n          serializationFormat: PURE;\n          keys: ['k'];\n          asserts:\n          [\n            shape: EqualTo #{{ expected: 1; }}#\n          ]\n        }}\n      ]\n    }}\n  ]"
    )))[0]["testSuites"];
    let suite = &suites[0];
    assert_eq!(suite["_type"], "serviceTestSuite");
    assert_eq!(suite["id"], "suite");
    // The data from `data` to `]`.
    assert_eq!(suite["testData"]["sourceInformation"], si(11, 7, 17, 7));
    let connection = &suite["testData"]["connectionsTestData"][0];
    assert_eq!(connection["id"], "my::C");
    assert_eq!(connection["data"]["_type"], "reference");
    let test = &suite["tests"][0];
    assert_eq!(test["_type"], "serviceTest");
    assert_eq!(test["serializationFormat"], "PURE");
    assert_eq!(test["keys"], json!(["k"]));
    assert_eq!(test["parameters"][0]["name"], "p");
    assert_eq!(test["parameters"][0]["value"]["_type"], "integer");
    // Bytes, base64-encoded, from `toBytes` to `)`.
    assert_eq!(
        test["parameters"][1]["value"],
        json!({"_type": "byteArray", "value": "YWI=", "sourceInformation": si(24, 24, 24, 36)})
    );
    assert_eq!(test["assertions"][0]["_type"], "equalTo");
    assert_eq!(test["assertions"][0]["id"], "shape");
    // A test needs its assertions.
    let no_asserts = error(&service(&format!(
        "{BASICS}\n{SINGLE}\n  testSuites: [ suite: {{ tests: [ test: {{ keys: ['k']; }} ] }} ]"
    )));
    assert_eq!(no_asserts.message(), "Field 'asserts' is required");
}

#[test]
fn a_flat_test_suite_resolves_data_and_expects_results() {
    let suite = &elements(&service(&format!(
        "{BASICS}\n{SINGLE}\n  testSuites:\n  [\n    suite 'Suite'\n    (\n      my::Store: Relation #{{\n        t:\n          a\n          1;\n      }}#;\n      my::Data;\n      relation 'doc' (p = 'x') ['k'] => Relation #{{\n        a\n        1;\n      }}#;\n      json => ExternalFormat #{{ contentType: 'application/json'; data: '[]'; }}#;\n    )\n  ]"
    )))[0]["testSuites"][0];
    assert_eq!(suite["doc"], "Suite");
    // The data from the documentation to `)`.
    assert_eq!(suite["testData"]["sourceInformation"], si(9, 11, 22, 5));
    let resolvers = &suite["testData"]["serviceTestData"];
    assert_eq!(resolvers[0]["_type"], "baseDataResolver");
    assert_eq!(
        resolvers[0]["elementPointer"],
        json!({"path": "my::Store", "sourceInformation": si(11, 7, 11, 15)})
    );
    assert_eq!(resolvers[0]["sourceInformation"], si(11, 7, 15, 8));
    assert_eq!(
        resolvers[1],
        json!({
            "_type": "referenceDataResolver",
            "elementPointer": {"path": "my::Data", "sourceInformation": si(16, 7, 16, 14)},
            "sourceInformation": si(16, 7, 16, 14),
        })
    );
    let relation = &suite["tests"][0];
    assert_eq!(relation["doc"], "doc");
    assert_eq!(relation["keys"], json!(["k"]));
    assert_eq!(relation["parameters"][0]["value"]["value"], "x");
    assert_eq!(relation["assertions"][0]["_type"], "equalToRelation");
    assert_eq!(relation["assertions"][0]["id"], "default");
    let json = &suite["tests"][1];
    assert_eq!(json["assertions"][0]["_type"], "equalToJson");
    assert_eq!(json["keys"], json!([]));
    assert!(json.get("parameters").is_none());
    // Other data is no expected result.
    let other = error(&service(&format!(
        "{BASICS}\n{SINGLE}\n  testSuites: [ suite ( test => Reference #{{ my::Data }}#; ) ]"
    )));
    assert!(
        other
            .message()
            .starts_with("Service atomic test '=> ...' expects Relation or ExternalFormat"),
        "{other}"
    );
}

#[test]
fn a_legacy_test_asserts_with_parameters() {
    let test = &elements(&service(&format!(
        "{BASICS}\n{SINGLE}\n  test: Single\n  {{\n    data: 'd';\n    asserts:\n    [\n      {{ ['a', list([1, 2])], res: Result[1]|true }}\n    ];\n  }}"
    )))[0]["test"];
    assert_eq!(test["_type"], "singleExecutionTest");
    assert_eq!(test["data"], "d");
    let container = &test["asserts"][0];
    assert_eq!(container["sourceInformation"], si(12, 7, 12, 50));
    assert_eq!(container["parametersValues"][0]["value"], "a");
    let list = &container["parametersValues"][1];
    assert_eq!(list["_type"], "classInstance");
    assert_eq!(list["type"], "listInstance");
    assert_eq!(list["sourceInformation"], si(12, 15, 12, 26));
    assert_eq!(list["value"]["values"][1]["value"], 2);
    // The engine's protocol makes a bare `Result` a `Result<Any|1..*>`.
    let result = &container["assert"]["parameters"][0]["genericType"];
    assert_eq!(
        result["typeArguments"][0]["rawType"]["fullPath"],
        "meta::pure::metamodel::type::Any"
    );
    assert_eq!(result["multiplicityArguments"], json!([{"lowerBound": 1}]));
    let multi = &elements(&service(&format!(
        "{BASICS}\n{SINGLE}\n  test: Multi\n  {{\n    tests['QA']: {{ data: 'd'; }}\n  }}"
    )))[0]["test"];
    assert_eq!(multi["_type"], "multiExecutionTest");
    assert_eq!(
        multi["tests"],
        json!([{"key": "QA", "data": "d", "asserts": [], "sourceInformation": si(9, 5, 9, 31)}])
    );
    let no_data = error(&service(&format!(
        "{BASICS}\n{SINGLE}\n  test: Single {{ asserts: []; }}"
    )));
    assert_eq!(no_data.message(), "Field 'data' is required");
}

#[test]
fn post_validations_check_the_result() {
    let validation = &elements(&service(&format!(
        "{BASICS}\n{SINGLE}\n  postValidations:\n  [\n    {{\n      description: 'v';\n      params: [|1, |2];\n      assertions: [ a: tds: Any[1]|true ];\n    }}\n  ]"
    )))[0]["postValidations"][0];
    assert_eq!(validation["description"], "v");
    assert_eq!(validation["parameters"].as_array().map(Vec::len), Some(2));
    assert_eq!(validation["assertions"][0]["id"], "a");
    assert_eq!(validation["assertions"][0]["assertion"]["_type"], "lambda");
    assert_eq!(validation["sourceInformation"], si(9, 5, 13, 5));
    let no_params = error(&service(&format!(
        "{BASICS}\n{SINGLE}\n  postValidations: [ {{ description: 'v'; assertions: []; }} ]"
    )));
    assert_eq!(no_params.message(), "Field 'params' is required");
}

#[test]
fn execution_environments_follow_the_services() {
    let elements = elements(
        "ExecutionEnvironment my::E\n{\n  executions:\n  [\n    QA:\n    {\n      mapping: my::M;\n      runtimeComponents:\n      {\n        runtime: my::R;\n        class: my::C;\n        binding: my::B;\n      }\n    },\n    PROD: { mapping: my::M; runtime: my::R; }\n  ];\n}\nService my::S { pattern: 'p'; documentation: 'd'; execution: Single { query: |1; } }\n",
    );
    assert_eq!(elements[0]["_type"], "service");
    let environment = &elements[1];
    assert_eq!(environment["_type"], "executionEnvironmentInstance");
    assert_eq!(environment["sourceInformation"], si(2, 1, 18, 1));
    let qa = &environment["executionParameters"][0];
    assert_eq!(qa["_type"], "singleExecutionParameters");
    assert_eq!(qa["key"], "QA");
    assert_eq!(qa["sourceInformation"], si(6, 5, 15, 5));
    // Each component from its field to its `;`.
    assert_eq!(
        qa["runtimeComponents"],
        json!({
            "runtime": {"_type": "runtimePointer", "runtime": "my::R", "sourceInformation": si(11, 9, 11, 23)},
            "clazz": {"type": "CLASS", "path": "my::C", "sourceInformation": si(12, 9, 12, 21)},
            "binding": {"type": "BINDING", "path": "my::B", "sourceInformation": si(13, 9, 13, 23)},
        })
    );
    assert_eq!(
        environment["executionParameters"][1]["runtime"]["runtime"],
        "my::R"
    );
    let grouped = &self::elements(
        "ExecutionEnvironment my::E { executions: [ group: [ QA: { mapping: my::M; runtime: my::R; } ] ]; }\n",
    )[0]["executionParameters"][0];
    assert_eq!(grouped["_type"], "multiExecutionParameters");
    assert_eq!(grouped["masterKey"], "group");
    assert_eq!(grouped["singleExecutionParameters"][0]["key"], "QA");
    let spaced = error(
        "ExecutionEnvironment my::E { executions: [ 'Q A': { mapping: my::M; runtime: my::R; } ]; }\n",
    );
    assert_eq!(
        spaced.message(),
        "Execution param key cannot contain spaces. Invalid Key: Q A"
    );
    // The mapping comes first, then a runtime.
    let runtime_first = error(
        "ExecutionEnvironment my::E { executions: [ QA: { runtime: my::R; mapping: my::M; } ]; }\n",
    );
    assert_eq!(position(&runtime_first), (2, 50, 2, 56));
}

#[test]
fn what_the_engine_reads_elsewhere_is_unsupported() {
    // An execution of another type, which an engine extension may read.
    let extension = error(&service(&format!(
        "{BASICS}\n  execution: Custom {{ a {{ b }} }}"
    )));
    assert!(
        matches!(extension, ParseError::Unsupported { .. }),
        "{extension}"
    );
    // The engine fails without a position on single and grouped executions
    // together, and on runtime components without each part.
    for environment in [
        "ExecutionEnvironment my::E { executions: [ QA: { mapping: my::M; runtime: my::R; }, g: [ P: { mapping: my::M; runtime: my::R; } ] ]; }\n",
        "ExecutionEnvironment my::E { executions: [ QA: { mapping: my::M; runtimeComponents: { runtime: my::R; } } ]; }\n",
    ] {
        let unsupported = error(environment);
        assert!(
            matches!(unsupported, ParseError::Unsupported { .. }),
            "{unsupported}"
        );
    }
}
