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

//! `ServiceStore #{ … }#` test data, as the engine's
//! `ServiceStoreEmbeddedDataParseTreeWalker` reads it. Linking this crate
//! registers the grammar with both the parser (`EMBEDDED_DATA_PARSERS`) and
//! the protocol converter (`EMBEDDED_DATA_PROTOCOLS`).

use legend_pure_dsl_service_store as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// A function's test suite, its data `data`: the data on line 6.
fn source(data: &str) -> String {
    format!("function f(): Any[*]\n{{\n  1\n}}\n{{\n  my::S: {data};\n  t | f() => 1;\n}}")
}

/// The data of the first store of the first suite of the function `source`
/// defines.
fn test_data(data: &str) -> Value {
    let file = legend_pure_parser_parser::parse(&source(data), "s.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    let function = serde_json::to_value(&pmcd.elements[0]).expect("serializes");
    function["tests"][0]["testData"][0]["data"].clone()
}

fn error(data: &str) -> ParseError {
    legend_pure_parser_parser::parse(&source(data), "s.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

/// Service store data of one stub, its request's fields `request`: the stub
/// starts on column 28, its request on 30, the fields on 41.
fn stubs(request: &str) -> String {
    format!(
        "ServiceStore #{{ [ {{ request: {{ {request} }}; response: {{ body: {BODY}; }}; }} ] }}#"
    )
}

const BODY: &str = "ExternalFormat #{ contentType: 'application/json'; data: '[]'; }#";

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

fn assert_error(error: &ParseError, message: &str, at: (u32, u32, u32, u32)) {
    assert!(error.message().contains(message), "{error}");
    let si = error.source_info().expect("positioned");
    assert_eq!(
        (si.start_line, si.start_column, si.end_line, si.end_column),
        at,
        "{error}"
    );
}

#[test]
fn a_stub_reads_its_request_and_response() {
    let data = test_data(
        "ServiceStore\n  #{\n    [\n      {\n        request:\n        {\n          method: post;\n          urlPath: '/people\\'s';\n          queryParameters:\n          {\n            name: EqualTo #{ expected: 'a\\'b'; }#,\n            \"x-id\": EqualToJson #{ expected: '{\"a\": \\'1\\'}'; }#\n          };\n          bodyPatterns: [ EqualTo #{ expected: 'x'; }# ];\n        };\n        response:\n        {\n          body: ExternalFormat #{ contentType: 'application/json'; data: '[]'; }#;\n        };\n      }\n    ]\n  }#",
    );
    assert_eq!(data["_type"], "serviceStore");
    // The data spans `ServiceStore` to `}#`; a stub, `{` to `}`.
    assert_eq!(span(&data), (6, 10, 27, 4));
    let stub = &data["serviceStubMappings"][0];
    assert_eq!(span(stub), (9, 7, 25, 7));
    let request = &stub["requestPattern"];
    assert_eq!(span(request), (10, 9, 20, 10));
    // The method in upper case; the strings unescaped.
    assert_eq!(request["method"], "POST");
    assert_eq!(request["urlPath"], "/people's");
    assert!(request.get("url").is_none(), "{request}");
    assert!(request.get("headerParams").is_none(), "{request}");
    // `EqualTo`'s value unescaped, `EqualToJson`'s as written; a quoted
    // parameter, unquoted.
    assert_eq!(
        request["queryParams"],
        json!({
            "name": {"_type": "equalTo", "expectedValue": "a'b"},
            "x-id": {"_type": "equalToJson", "expectedValue": "{\"a\": \\'1\\'}"},
        })
    );
    assert_eq!(
        request["bodyPatterns"],
        json!([{"_type": "equalTo", "expectedValue": "x"}])
    );
    let response = &stub["responseDefinition"];
    assert_eq!(span(response), (21, 9, 24, 10));
    assert_eq!(response["body"]["_type"], "externalFormat");
    assert_eq!(response["body"]["contentType"], "application/json");
}

#[test]
fn a_stub_needs_a_request_a_response_and_a_method() {
    // Each on the stub, `{` to `}`.
    let no_request = error(&format!(
        "ServiceStore #{{ [ {{ response: {{ body: {BODY}; }}; }} ] }}#"
    ));
    assert_error(&no_request, "Field 'request' is required", (6, 28, 6, 118));
    let no_response = error("ServiceStore #{ [ { request: { method: GET; }; } ] }#");
    assert_error(&no_response, "Field 'response' is required", (6, 28, 6, 57));
    // On the request, `request` to `;`.
    let no_method = error(&stubs("url: '/a';"));
    assert_error(&no_method, "Field 'method' is required", (6, 30, 6, 53));
}

#[test]
fn a_request_reads_each_field_once_and_the_engines_methods() {
    let twice = error(&stubs("method: GET; url: '/a'; url: '/b';"));
    assert_error(
        &twice,
        "Field 'url' should be specified only once",
        (6, 30, 6, 77),
    );
    // On the method's field.
    let method = error(&stubs("method: put;"));
    assert_error(
        &method,
        "Unsupported HTTP Method type - PUT. Supported types are - GET,POST",
        (6, 41, 6, 52),
    );
}

#[test]
fn a_parameter_is_given_once() {
    // On the second, its name to its pattern's end.
    let error = error(&stubs(
        "method: GET; headerParameters: { a: EqualTo #{ expected: '1'; }#, a: EqualTo #{ expected: '2'; }# };",
    ));
    assert_error(
        &error,
        "Header Param : 'a' value should be defined only once",
        (6, 107, 6, 137),
    );
}

#[test]
fn a_content_pattern_is_equal_to_or_equal_to_json_and_not_empty() {
    let unknown = error(&stubs(
        "method: GET; bodyPatterns: [ Like #{ expected: 'x'; }# ];",
    ));
    // On the type.
    assert_error(
        &unknown,
        "Unknown contentPattern pattern type: Like",
        (6, 70, 6, 73),
    );
    let empty = error(&stubs("method: GET; bodyPatterns: [ EqualTo #{}# ];"));
    // On the whole pattern.
    assert_error(&empty, "Content Pattern must not be empty", (6, 70, 6, 81));
}

#[test]
fn a_response_body_is_external_format_data() {
    let error = error(
        "ServiceStore #{ [ { request: { method: GET; }; response: { body: Reference #{ my::D }#; }; } ] }#",
    );
    // On the body's data.
    assert_error(
        &error,
        "Service response body should be ExternalFormat Embedded Data",
        (6, 75, 6, 95),
    );
}
