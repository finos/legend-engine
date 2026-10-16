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

//! Locks the `IslandContent` impl on `RootGraphFetchTree` —
//! wrapping in `IslandExpression` and round-tripping through
//! `Clone` / `PartialEq` must preserve identity. The graph-fetch
//! AST types now live in `dsl-graph`; core ast carries only the
//! trait surface.

use legend_pure_dsl_graph::ast::{RootGraphFetchTree, TAG};
use legend_pure_parser_ast::annotation::PackageableElementPtr;
use legend_pure_parser_ast::island::{IslandContent, IslandExpression};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use smol_str::SmolStr;

fn dummy_si() -> SourceInfo {
    SourceInfo::new("smoke.pure", 1, 1, 1, 8)
}

fn dummy_root() -> RootGraphFetchTree {
    RootGraphFetchTree {
        class: PackageableElementPtr {
            package: Some(Package::root(SmolStr::new("my"), dummy_si())),
            name: SmolStr::new("Person"),
            source_info: dummy_si(),
        },
        sub_trees: Vec::new(),
        sub_type_trees: Vec::new(),
        source_info: dummy_si(),
    }
}

#[test]
fn root_graph_fetch_tree_implements_island_content_with_empty_tag() {
    let r = dummy_root();
    let kind = <RootGraphFetchTree as IslandContent>::tag(&r);
    assert_eq!(kind, "");
    assert_eq!(kind, TAG);
}

#[test]
fn root_round_trips_through_island_expression() {
    let original = dummy_root();
    let wrapped = IslandExpression {
        content: Box::new(original.clone()),
        source_info: dummy_si(),
    };
    let cloned = wrapped.clone();
    assert_eq!(wrapped, cloned, "IslandExpression::clone broke");

    let recovered = cloned
        .content
        .as_any()
        .downcast_ref::<RootGraphFetchTree>()
        .expect("downcast to RootGraphFetchTree");
    assert_eq!(recovered, &original);
}
