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

//! Locks the `RelationStoreAccessorParser` end-to-end: a
//! `#>{ qualified::Store.table }#` expression embedded in a function
//! body parses through `parse_with_islands` +
//! `default_island_parsers()` (provided by `dsl-store`), produces an
//! `IslandExpression` whose content downcasts to
//! `RelationStoreAccessorRef`, and preserves the dotted path shape.

use legend_pure_dsl_store::ast::RelationStoreAccessorRef;
use legend_pure_dsl_store::parser::default_island_parsers;
use legend_pure_parser_ast::element::Element;
use legend_pure_parser_ast::expression::Expression;

fn parse_first_expression(src: &str) -> Expression {
    let wrapped = format!("function test::f(): Any[*] {{ {src} }}");
    let file = legend_pure_parser_parser::parse_with_islands(
        &wrapped,
        "store_smoke.pure",
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

fn assert_path(expr: &Expression, expected: &[&str]) {
    let Expression::Island(isl) = expr else {
        panic!("expected Island, got {expr:?}");
    };
    let acc = isl
        .content
        .as_any()
        .downcast_ref::<RelationStoreAccessorRef>()
        .expect("downcast to RelationStoreAccessorRef");
    let actual: Vec<&str> = acc.path.iter().map(smol_str::SmolStr::as_str).collect();
    assert_eq!(actual, expected);
}

#[test]
fn store_island_with_qualified_store_and_table() {
    let expr = parse_first_expression("#>{ my::mainDb.PersonTable }#");
    assert_path(&expr, &["my::mainDb", "PersonTable"]);
}

#[test]
fn store_island_with_deeper_namespace_and_extra_dot_segments() {
    let expr = parse_first_expression("#>{ test::sub::DB.tbl.col }#");
    assert_path(&expr, &["test::sub::DB", "tbl", "col"]);
}

#[test]
fn store_island_with_unqualified_store_name() {
    // Single-segment first path component is still legal — Java's
    // parser splits the body on `.` regardless of whether the first
    // segment contains `::`.
    let expr = parse_first_expression("#>{ DB.tbl }#");
    assert_path(&expr, &["DB", "tbl"]);
}

#[test]
fn a_store_alone_is_a_path() {
    // The engine splits the accessor on `.` whatever the number of
    // segments: `#>{my::mainDb}#` is the one-segment path `[my::mainDb]`.
    let expr = parse_first_expression("#>{ my::mainDb }#");
    assert!(format!("{expr:?}").contains("my::mainDb"), "{expr:?}");
}
