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

//! Aggregation aware class mappings, as the engine's
//! `AggregationAwareMappingParseTreeWalker` reads them.

use legend_pure_dsl_mapping as _;
use legend_pure_parser_ast::SourceOffset;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

const ELEMENT: &str = "  *my::Sales: AggregationAware\n  {\n    Views: [\n      (\n        ~modelOperation: {\n          ~canAggregate true,\n          ~groupByFunctions($this.date),\n          ~aggregateValues((~mapFn: $this.amount, ~aggregateFn: $mapped->sum()))\n        },\n        ~aggregateMapping: Pure\n        {\n          ~src my::DailySales\n          date: $src.day\n        }\n      )\n    ],\n    ~mainMapping: Pure\n    {\n      ~src my::RawSales\n      date: $src.date,\n      amount[a, b]: $src.amount\n    }\n  }";

/// The mapping of a `###Mapping` section, `Mapping my::M ( element )`,
/// its text at `offset` in the document.
fn mapping(element: &str, offset: SourceOffset) -> Value {
    let source = format!("###Mapping\nMapping my::M\n(\n{element}\n)\n");
    let file =
        legend_pure_parser_parser::parse_with_offset(&source, "m.pure", offset).expect("parses");
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
fn views_and_the_main_mapping_are_class_mappings_of_their_own_types() {
    let m = mapping(ELEMENT, SourceOffset::default());
    let class_mapping = &m["classMappings"][0];
    assert_eq!(class_mapping["_type"], "aggregationAware");
    // Without an id, the class's path.
    assert_eq!(class_mapping["id"], "my_Sales");
    assert_eq!(class_mapping["root"], true);
    let main = &class_mapping["mainSetImplementation"];
    assert_eq!(main["_type"], "pureInstance");
    assert_eq!(main["id"], "my_Sales_Main");
    assert_eq!(main["class"], "my::Sales");
    assert_eq!(main["srcClass"], "my::RawSales");
    let view = &class_mapping["aggregateSetImplementations"][0];
    assert_eq!(view["index"], 0);
    assert_eq!(view["setImplementation"]["id"], "my_Sales_Aggregate_0");
    let specification = &view["aggregateSpecification"];
    assert_eq!(specification["canAggregate"], true);
    assert_eq!(
        specification["groupByFunctions"][0]["groupByFn"]["body"][0]["property"],
        "date"
    );
    assert_eq!(
        specification["aggregateValues"][0]["aggregateFn"]["body"][0]["function"],
        "sum"
    );
    // The main mapping's property mappings, reduced.
    let amount = &class_mapping["propertyMappings"][1];
    assert_eq!(
        amount,
        &json!({
            "_type": "AggregationAwarePropertyMapping",
            "property": main["propertyMappings"][1]["property"],
            "source": "",
            "target": "a",
            "sourceInformation": main["propertyMappings"][1]["sourceInformation"],
        })
    );
}

#[test]
fn a_nested_mapping_is_positioned_through_the_elements_body() {
    // The element spans lines 4 to 26 of the section's text; its body's `{`
    // is on line 5: the main mapping's span is the element's, four lines
    // further down, as the walker positions it.
    let m = mapping(ELEMENT, SourceOffset::default());
    let class_mapping = &m["classMappings"][0];
    assert_eq!(span(class_mapping), (4, 3, 26, 3));
    let main = &class_mapping["mainSetImplementation"];
    assert_eq!(span(main), (8, 3, 30, 3));
    assert_eq!(
        (
            main["classSourceInformation"]["startLine"].clone(),
            main["classSourceInformation"]["startColumn"].clone()
        ),
        (json!(8), json!(4))
    );
    // From the section's own text: the section's lines don't count twice.
    let m = mapping(ELEMENT, SourceOffset::new(10, 0).expect("offset"));
    let class_mapping = &m["classMappings"][0];
    assert_eq!(span(class_mapping), (14, 3, 36, 3));
    assert_eq!(
        span(&class_mapping["mainSetImplementation"]),
        (18, 3, 40, 3)
    );
}

#[test]
fn a_specification_is_positioned_from_its_model_operation() {
    // `{` on the line after `~modelOperation`: the walker positions the
    // specification's lines one line up.
    let element = ELEMENT.replace("~modelOperation: {", "~modelOperation:\n        {");
    let m = mapping(&element, SourceOffset::default());
    let group_by = &m["classMappings"][0]["aggregateSetImplementations"][0]["aggregateSpecification"]
        ["groupByFunctions"][0]["groupByFn"]["body"][0];
    // `$this.date` is on line 11.
    assert_eq!(span(group_by).0, 10);
}
