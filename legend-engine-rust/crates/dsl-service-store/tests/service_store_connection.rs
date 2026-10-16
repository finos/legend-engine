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

//! Service store connections, as the engine's
//! `ServiceStoreConnectionParseTreeWalker` reads them.

use legend_pure_dsl_connection as _;
use legend_pure_dsl_runtime as _;
use legend_pure_dsl_service_store as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The value of the connection of `fields`, from line 4 on.
fn connection(fields: &str) -> Value {
    let source = format!("###Connection\nServiceStoreConnection my::C\n{{\n{fields}\n}}\n");
    let file = legend_pure_parser_parser::parse(&source, "c.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")["connectionValue"].clone()
}

fn error(source: &str) -> ParseError {
    legend_pure_parser_parser::parse(source, "c.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    serde_json::to_value(SourceInformation {
        source_id: "c.pure".into(),
        start_line,
        start_column,
        end_line,
        end_column,
    })
    .expect("a position serializes")
}

#[test]
fn a_connection_names_its_store_and_base_url() {
    assert_eq!(
        connection("  store: my::Store;\n  baseUrl: 'http://host:8080';"),
        json!({
            "_type": "serviceStore",
            "baseUrl": "http://host:8080",
            "element": "my::Store",
            "elementSourceInformation": si(4, 10, 4, 18),
            "sourceInformation": si(2, 1, 6, 1),
        })
    );
}

#[test]
fn a_packageable_connection_needs_its_store_and_a_url_without_a_trailing_slash() {
    let no_store =
        error("###Connection\nServiceStoreConnection my::C\n{\n  baseUrl: 'http://host';\n}\n");
    assert_eq!(no_store.message(), "Field 'store' is required");
    let slash = error(
        "###Connection\nServiceStoreConnection my::C\n{\n  store: my::Store;\n  baseUrl: 'http://host/';\n}\n",
    );
    assert_eq!(slash.message(), "baseUrl should not end with '/'");
    // On the field.
    let at = slash.source_info().expect("positioned");
    assert_eq!(
        (at.start_line, at.start_column, at.end_line, at.end_column),
        (5, 3, 5, 26)
    );
}

#[test]
fn a_runtimes_own_connection_needs_no_store() {
    let source = "###Runtime\nRuntime my::R\n{\n  mappings: [];\n  connections: [my::Store: [c: #{ ServiceStoreConnection { baseUrl: 'http://host'; } }#]];\n}\n";
    let file = legend_pure_parser_parser::parse(source, "r.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    let runtime = serde_json::to_value(&pmcd.elements[0]).expect("serializes");
    let connection =
        &runtime["runtimeValue"]["connections"][0]["storeConnections"][0]["connection"];
    assert_eq!(connection["_type"], "serviceStore");
    assert!(connection.get("element").is_none(), "{connection}");
}
