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

//! The `###MongoDB` section, as the engine's `MongoDBSchemaParseTreeWalker`
//! and its `MongoDBSchemaDeserializer` read it.

use legend_pure_dsl_mongodb as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The database of the section `###MongoDB\n{body}`.
fn database(body: &str) -> Value {
    let source = format!("###MongoDB\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "m.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

fn error(body: &str) -> ParseError {
    let source = format!("###MongoDB\n{body}");
    legend_pure_parser_parser::parse(&source, "m.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn position(error: &ParseError) -> (u32, u32, u32, u32) {
    let si = error.source_info().expect("positioned");
    (si.start_line, si.start_column, si.end_line, si.end_column)
}

/// A database of one collection, `c`, of the schema `schema`.
fn collection(schema: &str) -> String {
    format!("Database my::DB\n(\n  Collection c ( jsonSchema: {schema}; )\n)\n")
}

#[test]
fn a_collection_s_schema_is_typed() {
    let database = database(
        "Database my::DB\n(\n  include my::Other\n  Collection \"people list\"\n  (\n    validationLevel: moderate;\n    jsonSchema: {\n      \"bsonType\": \"object\",\n      \"title\": \"Person\",\n      \"properties\": {\n        \"name\": { \"bsonType\": \"string\", \"maxLength\": 20 },\n        \"age\": { \"bsonType\": \"int\", \"minimum\": -1 },\n        \"tags\": { \"bsonType\": \"array\", \"uniqueItems\": true, \"items\": [ { \"bsonType\": \"string\" } ] },\n        \"name\": { \"bsonType\": \"long\" }\n      },\n      \"required\": [ \"name\" ],\n      \"additionalProperties\": true\n    };\n  )\n  Collection view\n  (\n    validationAction: warn;\n  )\n  Join j ( [my::DB] a.b = c.d )\n)\n",
    );
    assert_eq!(database["_type"], "MongoDatabase");
    assert_eq!(database["name"], "DB");
    // Includes, views and joins are read and left out; a quoted name loses
    // its quotes; the defaults are strict and error.
    assert_eq!(
        database["collections"],
        json!([{
            "name": "people list",
            "validator": {
                "validationLevel": "moderate",
                "validationAction": "error",
                "validatorExpression": {
                    "_type": "jsonSchemaExpression",
                    "schemaExpression": {
                        "_type": "schema",
                        "title": "Person",
                        // A key written again keeps its place, with its
                        // later value.
                        "properties": [
                            {"key": "name", "value": {"_type": "longType"}},
                            {"key": "age", "value": {"_type": "intType", "minimum": -1}},
                            {"key": "tags", "value": {
                                "_type": "arrayType",
                                "items": [{"_type": "stringType"}],
                                "additionalItemsAllowed": false,
                                "uniqueItems": true,
                            }},
                        ],
                        "required": ["name"],
                        "additionalPropertiesAllowed": true,
                    },
                },
            },
        }])
    );
}

#[test]
fn the_grammar_reads_json_and_its_values() {
    let level = error(
        "Database my::DB\n(\n  Collection c ( validationLevel: loose; jsonSchema: {}; )\n)\n",
    );
    assert_eq!(position(&level), (4, 35, 4, 39));
    // A key is a "double-quoted" string.
    let key = error(&collection("{ 'bsonType': \"object\" }"));
    assert_eq!(position(&key), (4, 32, 4, 41));
}

#[test]
fn a_schema_the_engine_fails_on_is_unsupported() {
    // Its deserializer fails without a message of its own.
    let untyped = error(&collection("{ \"title\": \"x\" }"));
    assert!(
        matches!(untyped, ParseError::Unsupported { .. }),
        "{untyped:?}"
    );
    let regex = error(&collection(
        "{ \"bsonType\": \"object\", \"properties\": { \"r\": { \"bsonType\": \"regex\" } } }",
    ));
    assert!(matches!(regex, ParseError::Unsupported { .. }), "{regex:?}");
}
