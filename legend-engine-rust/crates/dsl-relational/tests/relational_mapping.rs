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

//! Relational mapping elements and test input, as the relational store's
//! `RelationalParseTreeWalker` and `RelationalGrammarParserExtension` read
//! them. Linking this crate registers them into `dsl-mapping`'s grammars.

use legend_pure_dsl_mapping as _;
use legend_pure_dsl_relational as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The mapping `###Mapping\nMapping my::M\n(\n{body}\n)\n` defines.
fn mapping(body: &str) -> Value {
    let source = format!("###Mapping\nMapping my::M\n(\n{body}\n)\n");
    let file = legend_pure_parser_parser::parse(&source, "m.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

fn error(body: &str) -> ParseError {
    let source = format!("###Mapping\nMapping my::M\n(\n{body}\n)\n");
    legend_pure_parser_parser::parse(&source, "m.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn start(node: &Value) -> (u64, u64) {
    let si = &node["sourceInformation"];
    (
        si["startLine"].as_u64().expect("line"),
        si["startColumn"].as_u64().expect("column"),
    )
}

#[test]
fn a_class_mapping_reads_its_commands_in_order() {
    let m = mapping(
        "  *my::Person[p]: Relational\n  {\n    ~filter [db]@J1 | [db]ActiveFilter\n    ~distinct\n    ~groupBy([db]t.a)\n    ~primaryKey(t.id)\n    ~mainTable [db]s.t\n    name: [db]t.name\n  }",
    );
    let class_mapping = &m["classMappings"][0];
    assert_eq!(class_mapping["_type"], "relational");
    assert_eq!(class_mapping["id"], "p");
    assert_eq!(class_mapping["distinct"], true);
    assert_eq!(
        class_mapping["filter"]["filter"],
        json!({"db": "db", "name": "ActiveFilter"})
    );
    assert_eq!(class_mapping["filter"]["joins"][0]["name"], "J1");
    // The primary key is in the filter's database.
    assert_eq!(
        class_mapping["primaryKey"][0]["table"]["database"],
        json!("db")
    );
    assert_eq!(class_mapping["groupBy"][0]["column"], "a");
    let main = &class_mapping["mainTable"];
    assert_eq!(main["schema"], "s");
    assert_eq!(main["table"], "t");
    assert_eq!(main["mainTableDb"], "db");
    // A property's value is positioned from its `:`.
    let name = &class_mapping["propertyMappings"][0];
    assert_eq!(name["_type"], "relationalPropertyMapping");
    assert_eq!(name["property"]["class"], "my::Person");
    assert_eq!(name["source"], "p");
    assert_eq!(start(name), (11, 9));
}

#[test]
fn property_mappings_read_scopes_transformers_and_embedded_mappings() {
    let m = mapping(
        "  my::Person: Relational\n  {\n    scope([db]t)\n    (\n      name: name,\n      +local: String[0..1]: x.c\n    ),\n    firm[f1, f2]: Binding my::Json: [db]t.json,\n    kind: EnumerationMapping kinds: [db]t.kind,\n    address\n    (\n      street: [db]t.street\n    ) Otherwise ([addr]: [db]@J1),\n    other() Inline[o]\n  }",
    );
    let properties = &m["classMappings"][0]["propertyMappings"];
    // A scope's table is its columns'; a table named in the scope's place
    // is the schema's.
    let name = &properties[0]["relationalOperation"]["table"];
    assert_eq!(
        (name["schema"].clone(), name["table"].clone()),
        (json!("default"), json!("t"))
    );
    let local = &properties[1];
    assert_eq!(
        local["localMappingProperty"]["multiplicity"],
        json!({"lowerBound": 0, "upperBound": 1})
    );
    assert!(local["property"].get("class").is_none());
    let local_table = &local["relationalOperation"]["table"];
    assert_eq!(
        (local_table["schema"].clone(), local_table["table"].clone()),
        (json!("t"), json!("x"))
    );
    // Two ids: the source, then the target.
    let firm = &properties[2];
    assert_eq!(
        (firm["source"].clone(), firm["target"].clone()),
        (json!("f1"), json!("f2"))
    );
    assert_eq!(firm["bindingTransformer"], json!({"binding": "my::Json"}));
    assert_eq!(properties[3]["enumMappingId"], "kinds");
    // Embedded, with an otherwise mapping positioned from `Otherwise`.
    let address = &properties[4];
    assert_eq!(address["_type"], "otherwiseEmbeddedPropertyMapping");
    assert_eq!(address["classMapping"]["_type"], "embedded");
    assert_eq!(address["classMapping"]["root"], false);
    assert_eq!(start(&address["classMapping"]), (14, 5));
    assert_eq!(start(address), (16, 7));
    let otherwise = &address["otherwisePropertyMapping"];
    assert_eq!(otherwise["target"], "addr");
    assert_eq!(otherwise["relationalOperation"]["_type"], "elemtWithJoins");
    let inline = &properties[5];
    assert_eq!(inline["_type"], "inlineEmbeddedPropertyMapping");
    assert_eq!(inline["setImplementationId"], "o");
}

#[test]
fn an_association_mapping_names_its_store() {
    let m = mapping(
        "  my::PersonFirm: Relational\n  {\n    AssociationMapping\n    (\n      firm: [db]@Person_Firm,\n      employees: [db]@Person_Firm\n    )\n  }",
    );
    let association = &m["associationMappings"][0];
    assert_eq!(association["_type"], "relational");
    assert_eq!(association["association"]["type"], "ASSOCIATION");
    assert_eq!(association["association"]["path"], "my::PersonFirm");
    assert_eq!(association["stores"], json!(["db"]));
    assert!(
        association["propertyMappings"][0]["property"]
            .get("class")
            .is_none()
    );
}

#[test]
fn a_legacy_test_reads_relational_input() {
    let m = mapping(
        "  MappingTests\n  [\n    t\n    (\n      query: |my::P.all();\n      data: [<Relational, SQL, my::Db, 'insert into t values(\\'a\\;b\\');' + '\\t'>];\n      assert: '[]';\n    )\n  ]",
    );
    let input = &m["tests"][0]["inputData"][0];
    assert_eq!(input["_type"], "relational");
    assert_eq!(input["inputType"], "SQL");
    assert_eq!(input["database"], "my::Db");
    // Unescaped, but for `\;`.
    assert_eq!(input["data"], "insert into t values('a\\;b');\t");
    let error = error(
        "  MappingTests\n  [\n    t\n    (\n      query: |1;\n      data: [<Relational, XML, my::Db, ''>];\n      assert: '[]';\n    )\n  ]",
    );
    assert!(
        error.message().contains("does not support format 'XML'"),
        "{error}"
    );
}

#[test]
fn the_walkers_errors_come_after_the_grammars() {
    // `name` has no table, which the walker finds; the grammar first finds
    // `::` where the value should have ended.
    let syntax = error("  my::Person: Relational\n  {\n    name: name,\n    firm: a::b\n  }");
    assert!(matches!(syntax, ParseError::Unexpected { .. }));
    let si = syntax.source_info().expect("positioned");
    assert_eq!((si.start_line, si.start_column), (7, 12));
    // Without a syntax error, the walker's.
    let walker = error("  my::Person: Relational\n  {\n    name: name\n  }");
    assert!(
        walker
            .message()
            .contains("Missing table or alias for column 'name'"),
        "{walker}"
    );
}
