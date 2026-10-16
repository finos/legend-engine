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

//! Reproduction tests for parser gaps surfaced when compiling engine-side
//! Pure repos through the Rust pipeline. Each test is a minimal
//! reproduction of one cataloged failure in
//! `legend-engine-rust/crates/integration-smoke/tests/relational_metamodel_compiles.rs`.

mod helpers;

use helpers::parse_ok;

/// `core_functions_unclassified/meta/create/newAssociation.pure:28`
/// `assertInstanceOf($x, Association)` — `Association` used as a value
/// in expression position. The parser needs to treat `Association` (and
/// other element keywords like `Measure`, `Primitive`) as identifier-
/// like in expression-primary context, mirroring `Class`/`Enum`/etc.
#[test]
fn association_as_value_in_expression() {
    let _ = parse_ok(
        r"###Pure
function my::test(x: Any[1]): Boolean[1]
{
    assertInstanceOf($x, Association)
}",
    );
}

#[test]
fn measure_as_value_in_expression() {
    let _ = parse_ok(
        r"###Pure
function my::test(x: Any[1]): Boolean[1]
{
    assertInstanceOf($x, Measure)
}",
    );
}

#[test]
fn primitive_as_value_in_expression() {
    let _ = parse_ok(
        r"###Pure
function my::test(x: Any[1]): Boolean[1]
{
    assertInstanceOf($x, Primitive)
}",
    );
}

/// `core_functions_unclassified/meta/get.pure:23`
/// `^CO_Location a(place='Jersey City, NJ')` — "new with name" pattern:
/// the optional identifier between the type and the property block names
/// the new instance.
#[test]
fn new_instance_with_optional_instance_name() {
    let _ = parse_ok(
        r"###Class
Class my::CO_Location
{
    place: String[1];
}

###Pure
function my::test(): my::CO_Location[1]
{
    ^my::CO_Location a(place='Jersey City, NJ')
}",
    );
}

/// `core_functions_unclassified/milestoning/reverseMilestoningTransforms.pure:26`
/// Match with a typed-parameter lambda whose type carries a generic and
/// a multiplicity: `[c:Class<Any>[1]| body, a:Association[1]| body]`.
#[test]
fn match_with_generic_typed_lambda_param() {
    let _ = parse_ok(
        r"###Pure
function my::test(x: Any[1]): String[1]
{
    $x->match([c: Class<Any>[1]| 'class', a: Association[1]| 'association'])
}",
    );
}

/// Probe: bare-lambda with a non-generic typed param works fine.
#[test]
fn bare_lambda_with_simple_type() {
    let _ = parse_ok(
        r"###Pure
function my::test(x: Any[1]): String[1]
{
    [c: String[1]| 'class']
}",
    );
}

/// Probe: bare-lambda with a generic typed param — does the generic on
/// its own work, or is it the multiplicity pairing that fails?
#[test]
fn bare_lambda_with_generic_typed_param() {
    let _ = parse_ok(
        r"###Pure
function my::test(x: Any[1]): String[1]
{
    [c: Class<Any>[1]| 'class']
}",
    );
}

/// Operator-precedence guard for the engine relation `extend` ColSpec lambda.
///
/// `core_functions_relation/relation/functions/transformation/extend.pure:61`
/// (`testSimpleExtendStrShared` / `testSimpleExtendStr_MultipleExpressions`)
/// and `.../relation/tests/composition.pure:61` (`testExtendFilter`) all
/// contain the column lambda:
///
/// ```text
/// ~name:c | $c.str->toOne() + $c.val->toOne()->toString()
/// ```
///
/// A legend-engine port report hypothesized the Rust parser groups this as
/// `($c.str->toOne() + $c.val->toOne())->toString()` — routing `String +
/// Integer` through `plus` and applying `toString` to the sum (the cause of
/// the `plus_String` "expected Object, got Integer" trace). That would
/// require `+` to bind tighter than postfix `->`. It does not: `->` is parsed
/// in `parse_postfix`, below `parse_additive` in the precedence-climbing
/// chain, so the right operand of `+` fully consumes its arrow chain before
/// the `Arithmetic` node is built. The six top-level guards in
/// `runtime/tests/eval_tests.rs` lock the literal forms; this locks the
/// `~name:c|<body>` ColSpec-lambda path the engine actually exercises, whose
/// body routes through `parse_expression` at `expression.rs:1221`.
///
/// Expected grouping (matches the Protocol JSON emitted on this HEAD):
/// `plus(toOne($c.str), toString(toOne($c.val)))` — `plus` at the top,
/// `toString` nested under its *right* operand.
#[test]
fn colspec_lambda_binds_arrow_tighter_than_plus() {
    use legend_pure_parser_ast::element::Element;
    use legend_pure_parser_ast::expression::{ArithmeticOp, ColumnTypeSpec, Expression};

    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    ~name:c|$c.str->toOne() + $c.val->toOne()->toString()
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
    let lambda = match col.columns[0].type_spec.as_ref() {
        Some(ColumnTypeSpec::Lambda(l)) => l,
        other => panic!("expected ColSpec lambda, got {other:?}"),
    };
    let body = lambda.body.first().expect("lambda body expression");

    // Top of the lambda body must be `+` (plus), NOT the arrow `->toString()`.
    // If `->` bound looser than `+`, this would be an
    // `ArrowFunction { function: toString, .. }` wrapping the sum.
    let arith = match body {
        Expression::Arithmetic(a) => a,
        other => panic!(
            "expected top-level `plus`, got {other:?} \
             (would be ArrowFunction(toString) if `+` bound tighter than `->`)"
        ),
    };
    assert_eq!(arith.op, ArithmeticOp::Plus);

    // Right operand is `$c.val->toOne()->toString()`: an arrow whose function
    // is `toString` and whose target is itself the `->toOne()` arrow — i.e.
    // `toString`'s receiver is the Integer column, not the `String + Integer`
    // sum.
    let right = match arith.right.as_ref() {
        Expression::ArrowFunction(a) => a,
        other => panic!("expected right operand `...->toString()`, got {other:?}"),
    };
    assert_eq!(right.function.name.as_str(), "toString");
    match right.target.as_ref() {
        Expression::ArrowFunction(inner) => assert_eq!(
            inner.function.name.as_str(),
            "toOne",
            "toString's receiver is `$c.val->toOne()`, not the `+` sum"
        ),
        other => panic!("expected `toString` target `$c.val->toOne()`, got {other:?}"),
    }

    // Left operand is `$c.str->toOne()`, unaffected by the right-hand arrow.
    match arith.left.as_ref() {
        Expression::ArrowFunction(a) => assert_eq!(a.function.name.as_str(), "toOne"),
        other => panic!("expected left operand `$c.str->toOne()`, got {other:?}"),
    }
}
