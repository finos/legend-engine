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

//! Cross-store and model join association mappings, as the engine's
//! `XStoreAssociationMappingParseTreeWalker` and
//! `ModelJoinAssociationMappingParseTreeWalker` read them.

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

#[test]
fn a_cross_store_mapping_maps_each_property_with_an_expression() {
    let m = mapping(
        "  my::Person_Firm[pf]: XStore\n  {\n    employer[p, f]: $this.firmId == $that.id,\n    employees: $that.firmId == $this.id\n  }",
    );
    let association = &m["associationMappings"][0];
    assert_eq!(association["_type"], "xStore");
    assert_eq!(association["id"], "pf");
    assert_eq!(association["association"]["path"], "my::Person_Firm");
    assert_eq!(association["stores"], json!([]));
    let employer = &association["propertyMappings"][0];
    assert_eq!(employer["_type"], "xStorePropertyMapping");
    assert_eq!(employer["property"]["class"], "my::Person_Firm");
    assert_eq!(
        (employer["source"].clone(), employer["target"].clone()),
        (json!("p"), json!("f"))
    );
    // An unpositioned lambda without parameters.
    let cross = &employer["crossExpression"];
    assert_eq!(cross["_type"], "lambda");
    assert!(cross.get("sourceInformation").is_none());
    assert_eq!(cross["body"][0]["function"], "equal");
    // Without ids, empty ones.
    let employees = &association["propertyMappings"][1];
    assert_eq!(
        (employees["source"].clone(), employees["target"].clone()),
        (json!(""), json!(""))
    );
}

#[test]
fn a_model_join_mapping_is_its_join_condition() {
    let m = mapping(
        "  my::Person_Firm: ModelJoin\n  {\n    {p: my::Person[1], f: my::Firm[1] | $p.firmId == $f.id}\n  }",
    );
    let association = &m["associationMappings"][0];
    assert_eq!(association["_type"], "modelJoin");
    let condition = &association["joinCondition"];
    assert_eq!(condition["_type"], "lambda");
    assert_eq!(condition["parameters"][0]["name"], "p");
    assert!(condition.get("sourceInformation").is_some());
    // Anything but a lambda is the walker's error, at the expression.
    let source = "###Mapping\nMapping my::M\n(\n  my::Person_Firm: ModelJoin\n  {\n    $p.firmId == $f.id\n  }\n)\n";
    let error = legend_pure_parser_parser::parse(source, "m.pure")
        .expect_err("fails")
        .errors
        .remove(0);
    assert!(
        error.message().contains("requires a lambda join condition"),
        "{error}"
    );
    let si = error.source_info().expect("positioned");
    assert_eq!((si.start_line, si.start_column), (6, 5));
}
