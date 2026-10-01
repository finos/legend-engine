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

//! Locks `GraphFetchIslandProtocol`: an `IslandExpression`
//! containing a `RootGraphFetchTree` round-trips through
//! `dispatch_island_convert` into the `_type: "rootGraphFetchTree"`
//! v1 JSON shape.

use legend_pure_dsl_graph::parser::default_island_parsers;
use legend_pure_dsl_graph::protocol::default_island_protocols;
use legend_pure_parser_ast::element::Element;
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_protocol::dispatch_island_convert;
use legend_pure_parser_protocol::v1::value_spec::ValueSpecification;

#[test]
fn graph_fetch_island_converts_to_root_graph_fetch_tree_json() {
    let source = r"
function test::f(): Any[*] {
    #{
        my::Person {
            firstName,
            lastName
        }
    }#
}
";
    let file = legend_pure_parser_parser::parse_with_islands(
        source,
        "protocol_smoke.pure",
        default_island_parsers(),
    )
    .unwrap_or_else(|p| {
        panic!(
            "parse failed: {:?}",
            p.errors.iter().map(ToString::to_string).collect::<Vec<_>>()
        )
    });

    let elem = &file.sections[0].elements[0];
    let Element::Function(f) = elem else {
        panic!("expected Function");
    };
    let Expression::Island(isl) = f.body.first().expect("body") else {
        panic!("expected Island");
    };

    let protocols = default_island_protocols();
    let refs: Vec<&dyn legend_pure_parser_protocol::IslandProtocol> =
        protocols.iter().map(std::convert::AsRef::as_ref).collect();
    let result = dispatch_island_convert(isl, &refs).expect("convert ok");
    let value = result.expect("converter matched");

    let ValueSpecification::ClassInstance(ci) = value else {
        panic!("expected ClassInstance");
    };
    assert_eq!(ci.type_name, "rootGraphFetchTree");
    assert_eq!(ci.value["_type"].as_str(), Some("rootGraphFetchTree"));
    assert_eq!(ci.value["class"].as_str(), Some("my::Person"));
    let sub_trees = ci.value["subTrees"].as_array().expect("subTrees array");
    assert_eq!(sub_trees.len(), 2);
}

#[test]
fn graph_fetch_dates_keep_their_percent() {
    // The engine's graph fetch walker makes any date a date-time of its raw
    // text (`new CDateTime(ctx.getText())`).
    let json =
        island_json("###Pure\nfunction f(): Any[*] { #{my::Person{firm(%2020-12-12){name}}}# }");
    let date = &json["value"]["subTrees"][0]["parameters"][0];
    assert_eq!(date["_type"], "dateTime");
    assert_eq!(date["value"], "%2020-12-12");
}

#[test]
fn graph_fetch_booleans_are_literals() {
    // The engine's graph grammar lexes `true` and `false` as BOOLEAN, never as
    // identifiers, so neither starts an enum reference.
    let json = island_json(
        "###Pure\nfunction f(): Any[*] { #{my::Person{fullName(true), name(false, my::E.A)}}# }",
    );
    let full_name = &json["value"]["subTrees"][0]["parameters"][0];
    assert_eq!(full_name["_type"], "boolean");
    assert_eq!(full_name["value"], true);
    // `true` is columns 46 to 49 of line 2.
    assert_eq!(full_name["sourceInformation"]["startColumn"], 46);
    assert_eq!(full_name["sourceInformation"]["endColumn"], 49);
    let name = &json["value"]["subTrees"][1]["parameters"];
    assert_eq!(name[0]["value"], false);
    assert_eq!(name[1]["_type"], "enumValue");
    assert_eq!(name[1]["fullPath"], "my::E");
}

/// The JSON of the graph fetch island that is `source`'s first function body.
fn island_json(source: &str) -> serde_json::Value {
    let sf = legend_pure_parser_parser::parse_with_islands(
        source,
        "test.pure",
        legend_pure_dsl_graph::parser::default_island_parsers(),
    )
    .expect("parses");
    let legend_pure_parser_ast::element::Element::Function(f) = sf.all_elements().next().unwrap()
    else {
        panic!("a function");
    };
    let legend_pure_parser_ast::expression::Expression::Island(island) = &f.body[0] else {
        panic!("an island");
    };
    let protocols = legend_pure_dsl_graph::protocol::default_island_protocols();
    let refs: Vec<&dyn legend_pure_parser_protocol::IslandProtocol> =
        protocols.iter().map(std::convert::AsRef::as_ref).collect();
    let value = legend_pure_parser_protocol::dispatch_island_convert(island, &refs)
        .expect("converts")
        .expect("matched");
    serde_json::to_value(&value).unwrap()
}
