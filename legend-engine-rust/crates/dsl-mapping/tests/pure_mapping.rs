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

//! Model-to-model class mappings, `my::Person: Pure { ~src … name: … }`, as
//! the engine's `PureInstanceClassMappingParseTreeWalker` reads them.

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
fn a_class_mapping_reads_its_source_filter_and_properties() {
    let m = mapping(
        "  *my::Person[p] extends [base]: Pure\n  {\n    ~src my::_Person\n    ~filter $src.active\n    +local: String[0..1]: $src.x,\n    name: $src.fullName,\n    firm[p, f]*: $src.firm,\n    type: EnumerationMapping types: $src.type\n  }",
    );
    let class_mapping = &m["classMappings"][0];
    assert_eq!(class_mapping["_type"], "pureInstance");
    assert_eq!(class_mapping["class"], "my::Person");
    assert_eq!(class_mapping["id"], "p");
    assert_eq!(class_mapping["root"], true);
    assert_eq!(class_mapping["extendsClassMappingId"], "base");
    assert_eq!(class_mapping["srcClass"], "my::_Person");
    // Line 4, `  *my::Person[p] extends [base]: Pure`, to the `}` of line 12.
    assert_eq!(span(class_mapping), (4, 3, 12, 3));
    assert_eq!(
        class_mapping["classSourceInformation"]["startColumn"],
        json!(4)
    );
    assert_eq!(
        class_mapping["sourceClassSourceInformation"]["startColumn"],
        json!(10)
    );
    // The filter: the body of an unpositioned lambda without parameters.
    // It ends where the local property starts.
    let filter = &class_mapping["filter"];
    assert_eq!(filter["_type"], "lambda");
    assert!(filter.get("sourceInformation").is_none());
    assert_eq!(filter["body"][0]["property"], "active");
    let properties = &class_mapping["propertyMappings"];
    // A local property: its type and multiplicity, no class.
    let local = &properties[0];
    assert_eq!(local["_type"], "purePropertyMapping");
    assert_eq!(
        local["property"],
        json!({"property": "local", "sourceInformation": local["property"]["sourceInformation"]})
    );
    assert_eq!(local["localMappingProperty"]["type"], "String");
    assert_eq!(
        local["localMappingProperty"]["multiplicity"],
        json!({"lowerBound": 0, "upperBound": 1})
    );
    assert_eq!(span(&local["property"]), (8, 6, 8, 10));
    assert_eq!(span(local), (8, 5, 8, 32));
    // A property's source is the class mapping's id.
    let name = &properties[1];
    assert_eq!(name["property"]["class"], "my::Person");
    assert_eq!(name["source"], "p");
    assert_eq!(name["explodeProperty"], false);
    assert!(name.get("target").is_none());
    assert_eq!(name["transform"]["body"][0]["property"], "fullName");
    // Its target is the first id between its brackets.
    let firm = &properties[2];
    assert_eq!(firm["target"], "p");
    assert_eq!(firm["explodeProperty"], true);
    assert_eq!(properties[3]["enumMappingId"], "types");
}

#[test]
fn a_class_mapping_without_an_id_is_its_properties_empty_source() {
    let m = mapping("  my::Person: Pure\n  {\n    name: 'x'\n  }");
    let class_mapping = &m["classMappings"][0];
    assert!(class_mapping.get("id").is_none());
    assert_eq!(class_mapping["root"], false);
    assert!(class_mapping.get("srcClass").is_none());
    assert_eq!(class_mapping["propertyMappings"][0]["source"], "");
}

#[test]
fn a_repeated_source_is_reported_at_the_class_mapping() {
    let source = "###Mapping\nMapping my::M\n(\n  my::Person: Pure\n  {\n    ~src my::A\n    ~src my::B\n  }\n)\n";
    let error = legend_pure_parser_parser::parse(source, "m.pure")
        .expect_err("fails")
        .errors
        .remove(0);
    assert_eq!(
        error.message(),
        "Field '~src' should be specified only once"
    );
    let si = error.source_info().expect("positioned");
    assert_eq!(
        (si.start_line, si.start_column, si.end_line, si.end_column),
        (4, 3, 8, 3)
    );
}
