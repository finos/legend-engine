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

//! Expression parsing tests — snapshot-based AST validation.

mod helpers;

use helpers::parse_ok;

#[test]
fn arithmetic_precedence() {
    let file = parse_ok(
        r"###Pure
function my::test(): Integer[1]
{
    (1 - 4 * (2 + 3)) * 4
}",
    );
    // Snapshot captures the full expression tree — verifies precedence via nesting
    insta::assert_debug_snapshot!(file);
}

#[test]
fn boolean_precedence() {
    let file = parse_ok(
        r"###Pure
function my::test(): Boolean[1]
{
    true || false && true
}",
    );
    // && should bind tighter than || in the AST tree
    insta::assert_debug_snapshot!(file);
}

#[test]
fn comparison_with_arithmetic() {
    let file = parse_ok(
        r"###Pure
function my::test(): Boolean[1]
{
    1 + 2 <= 3 - 4
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn or_with_arithmetic() {
    let file = parse_ok(
        r"###Pure
function my::validate(this: my::Validated[1]): Boolean[1]
{
    $this.id->isEmpty() || $this.id >= 0
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn cast() {
    let file = parse_ok(
        r"###Pure
function my::test(x: Any[1]): Float[1]
{
    $x->cast(@Float)
}",
    );
    insta::assert_debug_snapshot!(file);
}

/// Bare multiplicity literal `@[m]` — sibling to `@T` per
/// `M3CoreParser.g4:311 AT (type | multiplicity)`. Exercises every
/// concrete shape (`@[1]`, `@[0..1]`, `@[1..*]`, `@[*]`) plus the
/// named-parameter shape (`@[o]`). Parses to
/// `Expression::MultiplicityReferenceExpr(MultiplicityReferenceExpr {
/// multiplicity, .. })`. Regression guard against future parser drift.
#[test]
fn multiplicity_literal_concrete_and_named() {
    let file = parse_ok(
        r"###Pure
function my::a(xs: String[*]): String[1]    { $xs->toMultiplicity(@[1]) }
function my::b(xs: String[*]): String[0..1] { $xs->toMultiplicity(@[0..1]) }
function my::c(xs: String[*]): String[1..*] { $xs->toMultiplicity(@[1..*]) }
function my::d(xs: String[*]): String[*]    { $xs->toMultiplicity(@[*]) }
function my::e<|o>(xs: String[*]): String[o] { $xs->toMultiplicity(@[o]) }",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn collection_with_function() {
    let file = parse_ok(
        r"###Pure
function my::test(): Boolean[1]
{
    [(true && false), false]->oneOf()
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn new_instance_nested() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[1]
{
    ^goes2(v2=^goes(v='value'))
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn let_binding() {
    let file = parse_ok(
        r"###Pure
function my::test(): String[1]
{
    let x = 'hello';
    $x;
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn lambda_variants() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    [1, 2, 3]->filter(x | $x > 1)
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn no_param_lambda_multi_statement_body_terminates_at_comma() {
    // Regression: the no-param `| body` lambda used in `if(cond, |then, |else)`
    // forms must terminate its multi-statement body at the comma separating
    // arms. Before the fix, the parser only stopped on `)`, `}`, `]`, or
    // EOF — never `,` — so a trailing `;,` (last statement ends with `;`,
    // arm separator follows) was consumed as the start of another body
    // statement. The pattern shows up across `platform_dsl_mapping` (e.g.
    // `addAssociationMappingsIfRequired` in functions_Mapping.pure).
    let _ = parse_ok(
        r"###Pure
function my::test(x: Boolean[1]): Any[1]
{
    if($x,
       | let a = 1;
         let b = 2;
         $a + $b;,
       | 0
    )
}",
    );
}

#[test]
fn enum_value_access() {
    let file = parse_ok(
        r"###Pure
function my::test(): my::Color[1]
{
    my::Color.RED
}",
    );
    insta::assert_debug_snapshot!(file);
}

/// `~'col name'` must lower as a column spec whose `name` field holds the
/// bare text `col name` — without the surrounding `'`. Java strips the
/// quotes once at AST→M3 conversion (`AntlrContextToM3CoreInstance.java:
/// 950`); the Rust parser must do the same so downstream consumers
/// (`alloc_col_spec_literal`, name-based column lookups) see the canonical
/// identifier instead of `'col name'` (which never matches a real column).
#[test]
fn column_spec_with_quoted_name_strips_quotes() {
    use legend_pure_parser_ast::element::Element;
    use legend_pure_parser_ast::expression::Expression;

    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    ~'col name'
}",
    );

    let func = file
        .all_elements()
        .find_map(|e| match e {
            Element::Function(f) => Some(f),
            _ => None,
        })
        .expect("parsed function");
    let col = func
        .body
        .iter()
        .find_map(|expr| match expr {
            Expression::Column(c) => Some(c),
            _ => None,
        })
        .expect("column builder in body");
    assert_eq!(col.columns.len(), 1);
    assert_eq!(
        col.columns[0].name.as_str(),
        "col name",
        "quotes must be stripped from ~'col name'",
    );
    assert!(!col.is_array, "~name is the single-column ColSpec shape");
}
