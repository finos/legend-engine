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

//! Graph-fetch grammar parsing — snapshot-based AST validation.
//!
//! Moved from `crates/parser/tests/test_graph_fetch.rs` so the core parser
//! crate carries zero graph-fetch identifiers. The plug-in is registered
//! via `default_island_parsers()` for every fixture below.

use legend_pure_parser_ast::SourceFile;

fn parse_ok(source: &str) -> SourceFile {
    legend_pure_parser_parser::parse_with_islands(
        source,
        "test.pure",
        legend_pure_dsl_graph::parser::default_island_parsers(),
    )
    .unwrap_or_else(|e| {
        panic!(
            "Expected parse to succeed, but got error(s): {:?}",
            e.errors.iter().map(ToString::to_string).collect::<Vec<_>>()
        )
    })
}

#[test]
fn basic_with_qualifier() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    my::Person.all()->graphFetch(
        #{
            my::Person {
                firstName,
                lastName,
                employeesByFirstName(['Peter']) {
                    firstName,
                    lastName
                }
            }
        }#
    )
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn subtype_at_root() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    my::Firm.all()->graphFetch(
        #{
            my::Firm {
                name,
                ->subType(@my::FirmSubType) {
                    SubTypeName
                }
            }
        }#
    )
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn subtype_with_alias() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    my::Firm.all()->graphFetch(
        #{
            my::Firm {
                name,
                ->subType(@my::FirmSubType) {
                    'alias1' : SubTypeName
                }
            }
        }#
    )
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn subtype_not_at_root_error() {
    // The engine accepts `->subType(@T){…}` paths only among the root's
    // paths, and reports one anywhere else at the enclosing `{`.
    let source = r"###Pure
function my::test(): Any[*]
{
    my::Firm.all()->graphFetch(
        #{
            my::Firm {
                name {
                    ->subType(@my::SubType) {
                        SubTypeName
                    }
                }
            }
        }#
    )
}";
    let error = legend_pure_parser_parser::parse_with_islands(
        source,
        "test.pure",
        legend_pure_dsl_graph::parser::default_island_parsers(),
    )
    .expect_err("nested ->subType is rejected");
    let first = &error.errors[0];
    assert!(
        first.to_string().contains("supported only at root level"),
        "{first}"
    );
    let si = first.source_info().expect("positioned");
    assert_eq!((si.start_line, si.start_column), (7, 22));
}

#[test]
fn a_graph_definition_needs_a_path() {
    let error = legend_pure_parser_parser::parse_with_islands(
        "###Pure\nfunction f(): Any[*] { #{my::Firm{->subType(@my::Sub){}}}# }",
        "test.pure",
        legend_pure_dsl_graph::parser::default_island_parsers(),
    )
    .expect_err("empty graph definition");
    let si = error.errors[0].source_info().expect("positioned");
    assert_eq!((si.start_line, si.start_column), (2, 55));
}

#[test]
fn a_graph_fetch_tree_must_not_be_empty() {
    let error = legend_pure_parser_parser::parse_with_islands(
        "###Pure\nfunction f(): Any[*] { my::Firm.all()->graphFetch(#{}#) }",
        "test.pure",
        legend_pure_dsl_graph::parser::default_island_parsers(),
    )
    .expect_err("empty tree");
    let first = &error.errors[0];
    assert!(first.to_string().contains("must not be empty"), "{first}");
    // The whole island.
    let si = first.source_info().expect("positioned");
    assert_eq!(
        (si.start_line, si.start_column, si.end_line, si.end_column),
        (2, 51, 2, 54)
    );
}
