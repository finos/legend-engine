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

//! Relation class mappings, `my::Person: Relation { ~func … }`, as the
//! engine's `RelationFunctionMappingParseTreeWalker` reads them.

use legend_pure_dsl_mapping as _;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The mapping `###Mapping\nMapping my::M\n(\n{element}\n)\n` defines.
fn mapping(element: &str) -> Value {
    let source = format!("###Mapping\nMapping my::M\n(\n{element}\n)\n");
    let file = legend_pure_parser_parser::parse(&source, "m.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

fn span(node: &Value) -> (u64, u64, u64, u64) {
    let si = &node["sourceInformation"];
    let at = |key: &str| si[key].as_u64().unwrap_or_else(|| panic!("no {key}"));
    (
        at("startLine"),
        at("startColumn"),
        at("endLine"),
        at("endColumn"),
    )
}

#[test]
fn a_function_source_and_its_property_mappings() {
    let m = mapping(
        "  *my::Person[p]: Relation\n  {\n    ~func my::people(String[1]):Relation<Any>[1]\n    ~primaryKey: [ID, 'Name']\n    id: ID,\n    +local: String[0..1]: LOCAL,\n    name: Binding my::Json: NAME,\n    kind: EnumerationMapping kinds: $src.KIND->toUpper(),\n    firm\n    (\n      legalName: FIRM\n    ),\n    other() Inline[o]\n  }",
    );
    let class_mapping = &m["classMappings"][0];
    assert_eq!(class_mapping["_type"], "relation");
    // Not positioned on its class, as the walker leaves it.
    assert!(class_mapping.get("classSourceInformation").is_none());
    // The function as written, without spaces.
    let function = &class_mapping["relationFunction"];
    assert_eq!(function["type"], "FUNCTION");
    assert_eq!(function["path"], "my::people(String[1]):Relation<Any>[1]");
    assert_eq!(span(function), (6, 11, 6, 48));
    assert_eq!(class_mapping["primaryKey"], json!(["ID", "Name"]));
    let properties = &class_mapping["propertyMappings"];
    let id = &properties[0];
    assert_eq!(id["_type"], "relationFunctionPropertyMapping");
    assert_eq!(id["column"], "ID");
    assert_eq!(id["source"], "p");
    assert_eq!(id["property"]["class"], "my::Person");
    let local = &properties[1];
    assert_eq!(local["localMappingProperty"]["type"], "String");
    assert_eq!(
        local["localMappingProperty"]["multiplicity"],
        json!({"lowerBound": 0, "upperBound": 1})
    );
    assert_eq!(
        properties[2]["bindingTransformer"],
        json!({"binding": "my::Json"})
    );
    // An expression: the body of a lambda.
    let kind = &properties[3];
    assert_eq!(kind["enumMappingId"], "kinds");
    assert_eq!(kind["valueFn"]["_type"], "lambda");
    assert_eq!(kind["valueFn"]["body"][0]["function"], "toUpper");
    // Embedded: its properties have no class and no source.
    let firm = &properties[4];
    assert_eq!(firm["_type"], "relationFunctionEmbeddedPropertyMapping");
    assert_eq!(firm["source"], "p");
    let legal_name = &firm["propertyMappings"][0];
    assert!(legal_name["property"].get("class").is_none());
    assert!(legal_name.get("source").is_none());
    // Inline: an embedded mapping naming the class mapping it inlines.
    let other = &properties[5];
    assert_eq!(other["_type"], "relationFunctionEmbeddedPropertyMapping");
    assert_eq!(other["id"], "o");
    assert_eq!(other["propertyMappings"], json!([]));
}

#[test]
fn an_expression_lambda_counts_its_offset_in_the_body_twice() {
    // The body starts on line 5 (`{`). `~src`'s expression is on line 6,
    // the body's second line: its lambda is positioned one line further
    // down; `name`'s, on line 7, the third, two lines down.
    let m = mapping("  my::Person: Relation\n  {\n    ~src my::people()\n    name: $src.NAME\n  }");
    let class_mapping = &m["classMappings"][0];
    let source = &class_mapping["sourceLambda"];
    assert_eq!(span(source), (7, 10, 7, 21));
    // The expression itself is where it is.
    assert_eq!(span(&source["body"][0]).0, 6);
    let value = &class_mapping["propertyMappings"][0]["valueFn"];
    assert_eq!(span(value), (9, 11, 9, 19));
    assert_eq!(span(&value["body"][0]).0, 7);
    // On the body's first line, the columns move right too, by the
    // expression's column in the body.
    let m = mapping("  my::Person: Relation {~src my::people()\n  }");
    // `{` is column 24; the expression starts at 30, column 5 of the body.
    assert_eq!(span(&m["classMappings"][0]["sourceLambda"]), (4, 35, 4, 46));
}

#[test]
fn a_second_source_is_read_as_a_property() {
    // The grammar's names include `~src`: the error is where the property's
    // `:` should be.
    let source = "###Mapping\nMapping my::M\n(\n  my::Person: Relation\n  {\n    ~func my::f():Relation<Any>[1]\n    ~src my::g()\n  }\n)\n";
    let error = legend_pure_parser_parser::parse(source, "m.pure")
        .expect_err("fails")
        .errors
        .remove(0);
    let si = error.source_info().expect("positioned");
    assert_eq!((si.start_line, si.start_column), (7, 10));
}
