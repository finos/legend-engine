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

//! The `###Runtime` section, as the engine's `RuntimeParseTreeWalker` reads
//! it.

use legend_pure_dsl_runtime as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The elements of the section `###Runtime\n{body}`.
fn elements(body: &str) -> Vec<Value> {
    let source = format!("###Runtime\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "r.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|element| serde_json::to_value(element).expect("serializes"))
        .collect()
}

fn error(body: &str) -> ParseError {
    let source = format!("###Runtime\n{body}");
    legend_pure_parser_parser::parse(&source, "r.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    json!({
        "sourceId": "r.pure",
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

/// A runtime of `fields`, on line 4 on.
fn runtime(fields: &str) -> String {
    format!("Runtime my::R\n{{\n{fields}\n}}\n")
}

#[test]
fn a_runtime_names_its_mappings_connections_and_connection_stores() {
    let runtime = &elements(&runtime(
        "  mappings: [my::M, 'my'::'N 2'];\n  connections:\n  [\n    my::Db: [c1: my::Conn, c2: #{ JsonModelConnection { class: my::P; url: 'u'; } }#],\n    ModelStore: [],\n    my::Other: []\n  ];\n  connectionStores: [my::Conn: [(dataspace) my::DS, my::Db]];",
    ))[0];
    assert_eq!(runtime["_type"], "runtime");
    assert_eq!(runtime["package"], "my");
    assert_eq!(runtime["name"], "R");
    assert_eq!(runtime["sourceInformation"], si(2, 1, 12, 1));
    let value = &runtime["runtimeValue"];
    assert_eq!(value["_type"], "engineRuntime");
    assert_eq!(value["sourceInformation"], si(2, 1, 12, 1));
    assert_eq!(
        value["mappings"],
        json!([
            {"type": "MAPPING", "path": "my::M", "sourceInformation": si(4, 14, 4, 18)},
            {"type": "MAPPING", "path": "my::N 2", "sourceInformation": si(4, 21, 4, 31)},
        ])
    );
    // A store without connections is left out.
    assert_eq!(
        value["connections"],
        json!([{
            "store": {"type": "STORE", "path": "my::Db", "sourceInformation": si(7, 5, 7, 10)},
            "storeConnections": [
                {
                    "id": "c1",
                    "connection": {
                        "_type": "connectionPointer",
                        "connection": "my::Conn",
                        "sourceInformation": si(7, 18, 7, 25),
                    },
                    "sourceInformation": si(7, 14, 7, 25),
                },
                {
                    "id": "c2",
                    // A runtime's own connection is positioned where it is,
                    // `Type { … }`, and names no store.
                    "connection": {
                        "_type": "JsonModelConnection",
                        "class": "my::P",
                        "classSourceInformation": si(7, 64, 7, 68),
                        "url": "u",
                        "sourceInformation": si(7, 35, 7, 81),
                    },
                    "sourceInformation": si(7, 28, 7, 84),
                },
            ],
            "sourceInformation": si(7, 5, 7, 85),
        }])
    );
    assert_eq!(
        value["connectionStores"],
        json!([{
            "connectionPointer": {
                "_type": "connectionPointer",
                "connection": "my::Conn",
                "sourceInformation": si(11, 22, 11, 29),
            },
            "storePointers": [
                {"type": "DATASPACE", "path": "my::DS", "sourceInformation": si(11, 33, 11, 50)},
                {"path": "my::Db", "sourceInformation": si(11, 53, 11, 58)},
            ],
            "sourceInformation": si(11, 22, 11, 59),
        }])
    );
}

#[test]
fn single_connection_runtimes_come_after_the_others() {
    let runtimes = elements(
        "SingleConnectionRuntime my::S\n{\n  connection: my::C;\n  mappings: [my::M];\n}\nRuntime my::R\n{\n  mappings: [];\n}\n",
    );
    assert_eq!(runtimes[0]["name"], "R");
    let single = &runtimes[1];
    assert_eq!(single["name"], "S");
    let value = &single["runtimeValue"];
    assert_eq!(value["_type"], "localEngineRuntime");
    assert_eq!(value["connections"], json!([]));
    // The connection's store list, positioned on the connection.
    assert_eq!(
        value["connectionStores"],
        json!([{
            "connectionPointer": {
                "_type": "connectionPointer",
                "connection": "my::C",
                "sourceInformation": si(4, 15, 4, 19),
            },
            "storePointers": [],
            "sourceInformation": si(4, 15, 4, 19),
        }])
    );
}

#[test]
fn a_stores_connections_are_given_once() {
    let twice = error(&runtime(
        "  mappings: [];\n  connections: [my::Db: [a: my::C], my::Db: [b: my::C]];",
    ));
    assert_eq!(
        twice.message(),
        "Connections for store 'my::Db' is already specified"
    );
    assert_eq!(position(&twice), (5, 37, 5, 54));
    // A store without connections isn't kept, so it may be given again.
    let runtime = &elements(&runtime(
        "  mappings: [];\n  connections: [my::Db: [], my::Db: [a: my::C]];",
    ))[0];
    assert_eq!(
        runtime["runtimeValue"]["connections"][0]["storeConnections"][0]["id"],
        "a"
    );
}

#[test]
fn a_runtimes_fields_are_checked_on_the_whole_runtime() {
    let missing = error(&runtime("  connections: [];"));
    assert_eq!(missing.message(), "Field 'mappings' is required");
    assert_eq!(position(&missing), (2, 1, 5, 1));
    // The walker names connection stores so.
    let twice = error(&runtime(
        "  mappings: [];\n  connectionStores: [];\n  connectionStores: [];",
    ));
    assert_eq!(
        twice.message(),
        "Field 'connectionPointerStores' should be specified only once"
    );
    let no_connection = error("SingleConnectionRuntime my::S\n{\n  mappings: [];\n}\n");
    assert_eq!(no_connection.message(), "Field 'connection' is required");
    // A field of the other kind of runtime is a syntax error.
    let other = error(&runtime("  mappings: [];\n  connection: my::C;"));
    assert!(matches!(other, ParseError::Unexpected { .. }), "{other}");
    assert_eq!((position(&other).0, position(&other).1), (5, 3));
}

#[test]
fn a_runtimes_own_connection_is_read_by_its_types_grammar() {
    let empty = error(&runtime(
        "  mappings: [];\n  connections: [my::Db: [a: #{}#]];",
    ));
    assert_eq!(empty.message(), "Embedded connection must not be empty");
    // The island.
    assert_eq!(position(&empty), (5, 29, 5, 32));
    // What follows the value is not read.
    let runtime_with_trailer = &elements(&runtime(
        "  mappings: [];\n  connections: [my::Db: [a: #{ ModelChainConnection { mappings: []; } ignored; }#]];",
    ))[0];
    let connection = &runtime_with_trailer["runtimeValue"]["connections"][0]["storeConnections"][0]
        ["connection"];
    assert_eq!(connection["_type"], "ModelChainConnection");
    assert!(connection.get("element").is_none(), "{connection}");
    let unsupported = error(&runtime(
        "  mappings: [];\n  connections: [my::Db: [a: #{ RelationalDatabaseConnection { store: my::Db; } }#]];",
    ));
    assert!(
        matches!(unsupported, ParseError::Unsupported { .. }),
        "{unsupported}"
    );
}

#[test]
fn a_store_pointers_type_is_an_element_type() {
    // The engine fails on a type it doesn't know, outside its parser.
    let unknown = error(&runtime(
        "  mappings: [];\n  connectionStores: [my::C: [(Unknown) my::S]];",
    ));
    assert!(
        matches!(unknown, ParseError::Unsupported { .. }),
        "{unknown}"
    );
    // The grammar's keywords aren't words there.
    let keyword = error(&runtime(
        "  mappings: [];\n  connectionStores: [my::C: [(connection) my::S]];",
    ));
    assert!(
        matches!(keyword, ParseError::Unexpected { .. }),
        "{keyword}"
    );
    assert_eq!((position(&keyword).0, position(&keyword).1), (5, 31));
}
