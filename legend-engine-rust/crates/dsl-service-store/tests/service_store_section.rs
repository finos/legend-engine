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

//! The `###ServiceStore` section, as the engine's
//! `ServiceStoreParseTreeWalker` reads it.

use legend_pure_dsl_service_store as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The store of the section `###ServiceStore\n{body}`.
fn store(body: &str) -> Value {
    let source = format!("###ServiceStore\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "s.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

fn error(body: &str) -> ParseError {
    let source = format!("###ServiceStore\n{body}");
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

/// A store with one service, `parameters` and `response` its fields.
fn service(fields: &str) -> String {
    format!(
        "ServiceStore my::Store\n(\n  Service s\n  (\n    path : '/s';\n    method : GET;\n{fields}    security : [];\n  )\n)\n"
    )
}

#[test]
fn a_store_holds_services_in_groups() {
    let store = store(
        "ServiceStore my::Store\n(\n  description : 'Ignored';\n  ServiceGroup people\n  (\n    path : '/people';\n    Service byName\n    (\n      path : '/{name}';\n      method : GET;\n      parameters :\n      (\n        name : String ( location = path, required = true ),\n        \"tag list\" : [String] ( location = query, style = form, explode = false, allowReserved = true ),\n        kind : Integer ( location = header, enum = my::Kind )\n      );\n      response : [my::Person <- my::PersonBinding];\n      security : [];\n    )\n  )\n  Service add\n  (\n    path : '/add';\n    method : POST;\n    requestBody : my::Person <- my::PersonBinding;\n    response : my::Person <- my::PersonBinding;\n    security : [];\n  )\n)\n",
    );
    assert_eq!(store["_type"], "serviceStore");
    assert_eq!(store["package"], "my");
    assert_eq!(store["name"], "Store");
    assert_eq!(store["sourceInformation"], si(2, 1, 30, 1));
    // The description is read and left out, as the engine's walker leaves
    // it.
    assert!(store.get("description").is_none());
    let group = &store["elements"][0];
    assert_eq!(group["_type"], "serviceGroup");
    assert_eq!(group["id"], "people");
    assert_eq!(group["path"], "/people");
    assert_eq!(group["sourceInformation"], si(5, 3, 21, 3));
    let by_name = &group["elements"][0];
    assert_eq!(by_name["_type"], "service");
    assert_eq!(by_name["id"], "byName");
    assert_eq!(by_name["path"], "/{name}");
    assert_eq!(by_name["method"], "GET");
    assert_eq!(by_name["security"], json!([]));
    assert_eq!(by_name["sourceInformation"], si(8, 5, 20, 5));
    // A parameter's location in capitals; a list's style and explode,
    // positioned; a name in double quotes, unquoted.
    assert_eq!(
        by_name["parameters"],
        json!([
            {
                "name": "name",
                "type": {"_type": "string", "list": false, "sourceInformation": si(14, 16, 14, 21)},
                "location": "PATH",
                "required": true,
                "serializationFormat": {},
                "sourceInformation": si(14, 9, 14, 58),
            },
            {
                "name": "tag list",
                "type": {"_type": "string", "list": true, "sourceInformation": si(15, 22, 15, 29)},
                "location": "QUERY",
                "allowReserved": true,
                "serializationFormat": {
                    "style": "form",
                    "explode": false,
                    "styleSourceInformation": si(15, 51, 15, 62),
                    "explodeSourceInformation": si(15, 65, 15, 79),
                },
                "sourceInformation": si(15, 9, 15, 103),
            },
            {
                "name": "kind",
                "type": {"_type": "integer", "list": false, "sourceInformation": si(16, 16, 16, 22)},
                "location": "HEADER",
                "enumeration": "my::Kind",
                "serializationFormat": {},
                "sourceInformation": si(16, 9, 16, 61),
            },
        ])
    );
    // A class and its binding.
    assert_eq!(
        by_name["response"],
        json!({
            "_type": "complex",
            "type": "my::Person",
            "binding": "my::PersonBinding",
            "list": true,
            "sourceInformation": si(18, 18, 18, 50),
        })
    );
    let add = &store["elements"][1];
    assert_eq!(add["method"], "POST");
    assert_eq!(add["requestBody"]["type"], "my::Person");
    assert!(add.get("parameters").is_none());
}

#[test]
fn a_service_s_fields_are_checked() {
    let path = error(&service("").replace("'/s'", "'nope'").replace(
        "    security",
        "    response : my::P <- my::B;\n    security",
    ));
    assert_eq!(
        path.message(),
        "Path should start with '/' & should not end with '/'"
    );
    assert_eq!(position(&path), (6, 12, 6, 17));
    let method = error(&service("    response : my::P <- my::B;\n").replace("GET", "PUT"));
    assert_eq!(
        method.message(),
        "Unsupported HTTP Method type - PUT. Supported types are - GET,POST"
    );
    assert_eq!(position(&method), (7, 5, 7, 17));
    let response = error(&service("    response : String;\n"));
    assert_eq!(
        response.message(),
        "Expected Complex Type Reference for response, found : StringTypeReference"
    );
    assert_eq!(position(&response), (8, 5, 8, 22));
    let body = error(&service(
        "    requestBody : my::P <- my::B;\n    response : my::P <- my::B;\n",
    ));
    assert_eq!(
        body.message(),
        "Request Body should not be specified for GET end point"
    );
    assert_eq!(position(&body), (4, 3, 11, 3));
}

#[test]
fn a_parameter_s_options_are_checked() {
    let parameter = |parameter: &str| {
        error(&service(&format!(
            "    parameters : ( {parameter} );\n    response : my::P <- my::B;\n"
        )))
    };
    let unknown_type = parameter("p : Date ( location = query )");
    assert_eq!(
        unknown_type.message(),
        "Unsupported Parameter Value Type - Date. Supported types are - Boolean, Float, Integer, String"
    );
    assert_eq!(position(&unknown_type), (8, 24, 8, 27));
    let no_options = parameter("p : String");
    assert_eq!(
        no_options.message(),
        "Field location is required for service parameters"
    );
    assert_eq!(position(&no_options), (8, 20, 8, 29));
    let location = parameter("p : String ( location = body )");
    assert_eq!(
        location.message(),
        "Unsupported Parameter Location - body. Supported Locations are - header,path,query"
    );
    let optional_path = parameter("p : String ( location = path, required = false )");
    assert_eq!(
        optional_path.message(),
        "Path parameters cannot be optional"
    );
    assert_eq!(position(&optional_path), (8, 50, 8, 65));
    let explode = parameter("p : [String] ( location = query, style = form )");
    assert_eq!(explode.message(), "Field 'explode' is required");
    assert_eq!(position(&explode), (8, 20, 8, 66));
    let not_in_path = parameter("p : String ( location = path )");
    assert_eq!(
        not_in_path.message(),
        "Path parameters should be specified in path as '{param_name}'. [p] parameters were not found in path /s"
    );
    assert_eq!(position(&not_in_path), (4, 3, 11, 3));
}

#[test]
fn ids_are_unique_and_tokens_whole() {
    let ids = error(
        "ServiceStore my::Store\n(\n  Service a ( path : '/a'; method : GET; response : my::P <- my::B; security : []; )\n  Service a ( path : '/b'; method : GET; response : my::P <- my::B; security : []; )\n)\n",
    );
    assert_eq!(
        ids.message(),
        "Service Store Elements should have unique ids. Multiple elements found with ids - [a]"
    );
    assert_eq!(position(&ids), (2, 1, 6, 1));
    // `<-` is one token of the engine's lexer.
    let arrow = error(&service("    response : my::P < - my::B;\n"));
    assert_eq!(position(&arrow), (8, 22, 8, 22));
}

#[test]
fn a_security_scheme_is_unsupported() {
    // The engine reads one with an extension, which no grammar here is.
    let scheme = error(&service("    response : my::P <- my::B;\n").replace("[]", "[oauth]"));
    assert!(
        matches!(scheme, ParseError::Unsupported { .. }),
        "{scheme:?}"
    );
    assert_eq!(
        scheme.message(),
        "no service store security scheme grammar is linked for 'oauth'"
    );
}
