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

//! The `###Connection` section and the engine core's model connections, as
//! the engine's `ConnectionParseTreeWalker` and `ModelConnectionParseTreeWalker`
//! read them.

use legend_pure_dsl_connection as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The elements of the section `###Connection\n{body}`.
fn elements(body: &str) -> Vec<Value> {
    let source = format!("###Connection\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "c.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|element| serde_json::to_value(element).expect("serializes"))
        .collect()
}

fn error(body: &str) -> ParseError {
    let source = format!("###Connection\n{body}");
    legend_pure_parser_parser::parse(&source, "c.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

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

fn position(error: &ParseError) -> (u32, u32, u32, u32) {
    let si = error.source_info().expect("positioned");
    (si.start_line, si.start_column, si.end_line, si.end_column)
}

#[test]
fn a_model_connection_names_its_class_and_url() {
    let connection = &elements(
        "JsonModelConnection my::People\n{\n  class: my::Person;\n  url: 'data:application/json,{\"a\":\\'b\\'}';\n}\n",
    )[0];
    let si = |start_line, start_column, end_line, end_column| {
        serde_json::to_value(SourceInformation {
            source_id: "c.pure".into(),
            start_line,
            start_column,
            end_line,
            end_column,
        })
        .expect("a position serializes")
    };
    // The value is positioned as the whole connection; a packageable model
    // connection's store is the model store.
    assert_eq!(
        connection,
        &json!({
            "_type": "connection",
            "package": "my",
            "name": "People",
            "connectionValue": {
                "_type": "JsonModelConnection",
                "class": "my::Person",
                "classSourceInformation": si(4, 10, 4, 19),
                "url": "data:application/json,{\"a\":'b'}",
                "element": "ModelStore",
                "sourceInformation": si(2, 1, 6, 1),
            },
            "sourceInformation": si(2, 1, 6, 1),
        })
    );
}

#[test]
fn each_model_connection_reads_its_own_fields() {
    let connections = elements(
        "XmlModelConnection a::X\n{\n  url: 'u';\n  mappings: [a::Ignored];\n  class: a::C;\n}\nModelChainConnection a::Chain\n{\n  class: a::Ignored;\n  mappings: [a::M1, 'a'::'M 2'];\n}\n",
    );
    let xml = &connections[0]["connectionValue"];
    assert_eq!(xml["_type"], "XmlModelConnection");
    assert_eq!(xml["class"], "a::C");
    assert!(xml.get("mappings").is_none(), "{xml}");
    let chain = &connections[1]["connectionValue"];
    assert_eq!(chain["_type"], "ModelChainConnection");
    assert_eq!(chain["mappings"], json!(["a::M1", "a::M 2"]));
    assert_eq!(chain["element"], "ModelStore");
    // `mappings` to `;`, on line 11.
    assert_eq!(
        span(&json!({"sourceInformation": chain["mappingsSourceInformation"]})),
        (11, 3, 11, 32)
    );
    assert!(chain.get("class").is_none(), "{chain}");
}

#[test]
fn a_models_fields_are_checked_on_the_whole_connection() {
    let missing = error("JsonModelConnection a::C\n{\n  url: 'u';\n}\n");
    assert_eq!(missing.message(), "Field 'class' is required");
    assert_eq!(position(&missing), (2, 1, 5, 1));
    let twice = error("ModelChainConnection a::C\n{\n  mappings: [];\n  mappings: [];\n}\n");
    assert_eq!(
        twice.message(),
        "Field 'mappings' should be specified only once"
    );
    // The class first, then the URL.
    let neither = error("XmlModelConnection a::C\n{\n}\n");
    assert_eq!(neither.message(), "Field 'class' is required");
}

#[test]
fn the_section_is_read_before_the_values() {
    // The first value lacks its class, but the grammar error after it comes
    // first, as the engine reads the whole section first.
    let syntax = error("JsonModelConnection a::C\n{\n}\nJsonModelConnection a::D (\n");
    assert!(matches!(syntax, ParseError::Unexpected { .. }), "{syntax}");
    assert_eq!(position(&syntax), (5, 26, 5, 26));
    // A syntax error in a value is the value's grammar's.
    let in_value = error("JsonModelConnection a::C\n{\n  class a::A;\n}\n");
    assert_eq!((position(&in_value).0, position(&in_value).1), (4, 9));
}

#[test]
fn a_connection_of_a_type_without_grammar_is_unsupported() {
    let error = error("RelationalDatabaseConnection a::C\n{\n  store: a::Db;\n}\n");
    assert!(matches!(error, ParseError::Unsupported { .. }), "{error}");
    // The whole connection.
    assert_eq!(position(&error), (2, 1, 5, 1));
}
