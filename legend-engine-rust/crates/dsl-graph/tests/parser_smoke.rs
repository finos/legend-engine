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

//! Locks the `GraphFetchIslandParser` end-to-end: a `#{ Type { … } }#`
//! expression embedded in a function body parses through
//! `parse_with_islands` + `default_island_parsers()` (provided by
//! `dsl-graph`), produces an `IslandExpression` with content
//! downcastable to `RootGraphFetchTree`, and preserves the class
//! reference + sub-tree shape.

use legend_pure_dsl_graph::ast::RootGraphFetchTree;
use legend_pure_dsl_graph::parser::default_island_parsers;
use legend_pure_parser_ast::element::Element;
use legend_pure_parser_ast::expression::Expression;

fn parse_first_expression(src: &str) -> Expression {
    let wrapped = format!("function test::f(): Any[*] {{ {src} }}");
    let file = legend_pure_parser_parser::parse_with_islands(
        &wrapped,
        "graph_smoke.pure",
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
        panic!("expected Function, got {elem:?}");
    };
    f.body.first().cloned().expect("function has body")
}

#[test]
fn empty_tag_island_dispatches_to_graph_fetch_parser() {
    let expr = parse_first_expression(
        "#{
            my::Person {
                firstName,
                lastName
            }
        }#",
    );
    let isl = match expr {
        Expression::Island(i) => i,
        other => panic!("expected Island, got {other:?}"),
    };
    let root = isl
        .content
        .as_any()
        .downcast_ref::<RootGraphFetchTree>()
        .expect("downcast to RootGraphFetchTree");
    assert_eq!(root.class.name.as_str(), "Person");
    assert_eq!(root.sub_trees.len(), 2);
    assert_eq!(root.sub_trees[0].property.as_str(), "firstName");
    assert_eq!(root.sub_trees[1].property.as_str(), "lastName");
}

#[test]
fn nested_subtree_parses_with_dsl_graph() {
    let expr = parse_first_expression(
        "#{
            my::Person {
                firstName,
                address {
                    city,
                    street
                }
            }
        }#",
    );
    let Expression::Island(isl) = expr else {
        panic!("expected Island");
    };
    let root = isl
        .content
        .as_any()
        .downcast_ref::<RootGraphFetchTree>()
        .expect("RootGraphFetchTree");
    let address = root
        .sub_trees
        .iter()
        .find(|p| p.property.as_str() == "address")
        .expect("address property");
    assert_eq!(address.sub_trees.len(), 2);
}
