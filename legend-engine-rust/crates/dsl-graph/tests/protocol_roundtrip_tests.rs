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

//! Graph-fetch AST → Protocol JSON conversion.
//!
//! Moved from `crates/protocol/tests/full_roundtrip_tests.rs` so the core
//! protocol crate carries zero graph-fetch identifiers. The converter is
//! resolved via `default_island_protocols()` and dispatched through the
//! generic `IslandProtocol` trait.

#[test]
fn graph_fetch_ast_to_protocol() {
    let source = r"
###Pure
function my::func(): Any[*] {
    #{
        meta::pure::domain::Person {
            name,
            details {
                address
            }
        }
    }#
}
";
    let sf = legend_pure_parser_parser::parse_with_islands(
        source,
        "test.pure",
        legend_pure_dsl_graph::parser::default_island_parsers(),
    )
    .expect("parse failed");

    // Find the island expression inside the function body and dispatch
    // via the IslandProtocol trait + dsl-graph's protocol converter.
    // After the protocol extraction, core's `convert_island_expression`
    // emits an `unknownIsland_*` placeholder unless the caller routes
    // through `dispatch_island_convert`.
    let ast_elem = sf.all_elements().next().unwrap();
    let legend_pure_parser_ast::element::Element::Function(f) = ast_elem else {
        panic!("expected function");
    };
    let legend_pure_parser_ast::expression::Expression::Island(island) =
        f.body.first().expect("body")
    else {
        panic!("expected island expression");
    };

    let protocols = legend_pure_dsl_graph::protocol::default_island_protocols();
    let refs: Vec<&dyn legend_pure_parser_protocol::IslandProtocol> =
        protocols.iter().map(std::convert::AsRef::as_ref).collect();
    let value = legend_pure_parser_protocol::dispatch_island_convert(island, &refs)
        .expect("convert ok")
        .expect("converter matched");

    let json = serde_json::to_string(&value).unwrap();
    assert!(
        json.contains("rootGraphFetchTree"),
        "expected rootGraphFetchTree in output; got {json}"
    );
}
