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

//! Elasticsearch connections, as the engine's
//! `ElasticsearchStoreParseTreeWalker` reads them.

use legend_pure_dsl_elasticsearch as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The section, `fields` from line 4 on.
fn source(fields: &str) -> String {
    format!("###Connection\nElasticsearch7ClusterConnection my::C\n{{\n{fields}\n}}\n")
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

fn position(error: &ParseError) -> (u32, u32, u32, u32) {
    let si = error.source_info().expect("positioned");
    (si.start_line, si.start_column, si.end_line, si.end_column)
}

const AUTH: &str = "  authentication: # Kerberos {}#;";

#[test]
fn a_connection_reads_its_store_cluster_and_authentication() {
    let value = connection(&format!(
        "  store: my::Store;\n  clusterDetails: # URL {{ http://localhost:9200 }}#;\n{AUTH}"
    ));
    assert_eq!(value["_type"], "elasticsearch7StoreConnection");
    assert_eq!(value["element"], "my::Store");
    assert_eq!(value["sourceSpec"], json!({"url": "http://localhost:9200"}));
    assert_eq!(value["authSpec"]["_type"], "kerberos");
    // The body, from its first token to the end of input, whose `<EOF>`
    // ANTLR counts.
    assert_eq!(
        value["sourceInformation"],
        json!({"sourceId": "c.pure", "startLine": 4, "startColumn": 3, "endLine": 7, "endColumn": 5})
    );
}

#[test]
fn the_cluster_details_are_a_url() {
    // Another type, on `# Type {`.
    let uri = error(&format!(
        "  store: my::Store;\n  clusterDetails: # URI {{ http://h }}#;\n{AUTH}"
    ));
    assert_eq!(
        uri.message(),
        "Unsupported cluster details type: URI.  Supported: URL"
    );
    assert_eq!(position(&uri), (5, 19, 5, 25));
    // One `java.net.URI` rejects, from the text to `}#`.
    let invalid = error(&format!(
        "  store: my::Store;\n  clusterDetails: # URL {{ wrong url }}#;\n{AUTH}"
    ));
    assert_eq!(invalid.message(), "URL is not valid");
    assert_eq!(position(&invalid), (5, 26, 5, 38));
    // The walker names its fields as its lexer does.
    let no_details = error(&format!("  store: my::Store;\n{AUTH}"));
    assert_eq!(no_details.message(), "Field ''clusterDetails'' is required");
}
