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

//! The `###Elasticsearch` section, as the engine's
//! `ElasticsearchStoreParseTreeWalker` reads it.

use legend_pure_dsl_elasticsearch as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The store of the section `###Elasticsearch\n{body}`.
fn store(body: &str) -> Value {
    let source = format!("###Elasticsearch\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "e.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

fn error(body: &str) -> ParseError {
    let source = format!("###Elasticsearch\n{body}");
    legend_pure_parser_parser::parse(&source, "e.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn position(error: &ParseError) -> (u32, u32, u32, u32) {
    let si = error.source_info().expect("positioned");
    (si.start_line, si.start_column, si.end_line, si.end_column)
}

/// A store of one index, `people`, of `properties`.
fn index(properties: &str) -> String {
    format!(
        "Elasticsearch7Cluster my::S\n{{\n  indices: [ people: {{ properties: [ {properties} ]; }} ];\n}}\n"
    )
}

/// A property of a type with no fields.
fn scalar(field: &str, protocol_type: &str, json_type: &str) -> Value {
    json!({field: {"_pure_protocol_type": protocol_type, "type": json_type}})
}

#[test]
fn a_store_maps_its_indices_properties() {
    let store = store(
        "Elasticsearch7Cluster my::S\n{\n  indices: [\n    people: {\n      properties: [\n        name: Text { fields: [ raw: Keyword ]; },\n        age: Integer {},\n        'home address': Object { properties: [ city: Keyword ]; }\n      ];\n    }\n  ];\n}\n",
    );
    assert_eq!(store["_type"], "elasticsearch7Store");
    assert_eq!(store["name"], "S");
    // An index's properties are a list; a property's own, by name.
    assert_eq!(
        store["indices"],
        json!([{
            "indexName": "people",
            "properties": [
                {
                    "propertyName": "name",
                    "property": {"text": {
                        "_pure_protocol_type": "textProperty",
                        "type": "text",
                        "fields": {"raw": scalar("keyword", "keywordProperty", "keyword")},
                    }},
                },
                // Braces without fields: none, but not absent.
                {
                    "propertyName": "age",
                    "property": {"integer": {
                        "_pure_protocol_type": "integerNumberProperty",
                        "type": "integer",
                        "fields": {},
                    }},
                },
                {
                    "propertyName": "home address",
                    "property": {"object": {
                        "_pure_protocol_type": "objectProperty",
                        "type": "object",
                        "properties": {"city": scalar("keyword", "keywordProperty", "keyword")},
                    }},
                },
            ],
        }])
    );
}

#[test]
fn every_type_has_its_names() {
    let types = [
        ("Keyword", "keyword", "keywordProperty", "keyword"),
        ("Date", "date", "dateProperty", "date"),
        ("Short", "_short", "shortNumberProperty", "short"),
        ("Byte", "_byte", "byteNumberProperty", "byte"),
        ("Long", "_long", "longNumberProperty", "long"),
        ("Float", "_float", "floatNumberProperty", "float"),
        (
            "HalfFloat",
            "half_float",
            "halfFloatNumberProperty",
            "half_float",
        ),
        ("Double", "_double", "doubleNumberProperty", "double"),
        ("Boolean", "_boolean", "booleanProperty", "boolean"),
    ];
    for (type_name, field, protocol_type, json_type) in types {
        let store = store(&index(&format!("p: {type_name}")));
        assert_eq!(
            store["indices"][0]["properties"][0]["property"],
            scalar(field, protocol_type, json_type),
            "{type_name}"
        );
    }
}

#[test]
fn the_walker_checks_fields_by_their_literal_names() {
    let indices = error("Elasticsearch7Cluster my::S\n{\n}\n");
    assert_eq!(indices.message(), "Field ''indices'' is required");
    assert_eq!(position(&indices), (2, 1, 4, 1));
    let properties = error("Elasticsearch7Cluster my::S\n{\n  indices: [ people: { } ];\n}\n");
    assert_eq!(properties.message(), "Field ''properties'' is required");
    assert_eq!(position(&properties), (4, 14, 4, 24));
    // A complex property's, on its braces.
    let nested = error(&index("a: Object { }"));
    assert_eq!(nested.message(), "Field ''properties'' is required");
    assert_eq!(position(&nested), (4, 48, 4, 50));
}

#[test]
fn names_are_not_keywords_nor_written_twice() {
    let keyword = error(&index("fields: Keyword"));
    assert_eq!(position(&keyword), (4, 38, 4, 43));
    // The engine fails without a message on a name written twice.
    let twice = error(&index("a: Object { properties: [ b: Keyword, b: Text ]; }"));
    assert!(matches!(twice, ParseError::Unsupported { .. }), "{twice:?}");
}
