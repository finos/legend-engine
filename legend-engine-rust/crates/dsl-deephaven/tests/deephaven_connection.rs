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

//! Deephaven connections, as the engine's `DeephavenParseTreeWalker` reads
//! them.

use legend_pure_dsl_deephaven as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The section, `fields` from line 4 on.
fn source(fields: &str) -> String {
    format!("###Connection\nDeephavenConnection my::C\n{{\n{fields}\n}}\n")
}

fn connection(fields: &str) -> Value {
    let file = legend_pure_parser_parser::parse(&source(fields), "c.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")["connectionValue"].clone()
}

fn error(fields: &str) -> ParseError {
    legend_pure_parser_parser::parse(&source(fields), "c.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    json!({
        "sourceId": "c.pure",
        "startLine": start_line,
        "startColumn": start_column,
        "endLine": end_line,
        "endColumn": end_column,
    })
}

const AUTH: &str = "  authentication: # PSK { psk: 'key'; }#;";

#[test]
fn a_connection_reads_its_store_url_and_authentication() {
    assert_eq!(
        connection(&format!(
            "  store: my::Store;\n  serverUrl: 'http://localhost:10000'\n{AUTH}"
        )),
        json!({
            "_type": "deephavenConnection",
            "element": "my::Store",
            "elementSourceInformation": si(4, 10, 4, 18),
            "sourceSpec": {"url": "http://localhost:10000"},
            "authSpec": {"_type": "PSK", "psk": "key"},
            // The body, from its first token to the end of input, whose text,
            // `<EOF>`, ANTLR counts.
            "sourceInformation": si(4, 3, 7, 5),
        })
    );
}

#[test]
fn the_walker_names_its_fields_as_its_lexer_does() {
    let no_store = error(&format!("  serverUrl: 'http://h'\n{AUTH}"));
    assert_eq!(no_store.message(), "Field ''store'' is required");
    let no_url = error(&format!("  store: my::Store;\n{AUTH}"));
    assert_eq!(
        no_url.message(),
        "Field ''serverUrlDefinition'' is required"
    );
}

#[test]
fn a_url_is_a_uri() {
    // One `java.net.URI` rejects, on the field.
    let invalid = error(&format!(
        "  store: my::Store;\n  serverUrl: 'http://a b'\n{AUTH}"
    ));
    assert_eq!(invalid.message(), "URL format is not valid");
    let si = invalid.source_info().expect("positioned");
    assert_eq!(
        (si.start_line, si.start_column, si.end_line, si.end_column),
        (5, 3, 5, 25)
    );
    // One Rust doesn't judge is the engine's.
    let unknown = error(&format!(
        "  store: my::Store;\n  serverUrl: 'mailto:someone'\n{AUTH}"
    ));
    assert!(
        matches!(unknown, ParseError::Unsupported { .. }),
        "{unknown}"
    );
}
