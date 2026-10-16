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

//! Type system parsing tests — snapshot-based AST validation.

mod helpers;

use helpers::parse_ok;

#[test]
fn type_arguments() {
    let file = parse_ok(
        r"###Pure
function my::test(r: Result<String>[1]): Result<String>[1]
{
    $r
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn cast_with_relation() {
    let file = parse_ok(
        r"###Pure
function my::test(x: Any[1]): Any[1]
{
    $x->cast(@Relation<(a:Integer)>)
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn type_variable_values() {
    let file = parse_ok(
        r"###Pure
function my::test(r: Res(1)[1], v: VARCHAR(200)[1]): Res(1)[1]
{
    $r
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn generics_and_variables() {
    let file = parse_ok(
        r"###Pure
function my::test(r: Res<String>(1, 'a')[1]): Res<String>(1, 'a')[1]
{
    $r
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn relation_column_types() {
    let file = parse_ok(
        r"###Pure
function my::test(r: X<(a:Integer(200), z:V('ok'))>[1]): X<(a:Integer(200), z:V('ok'))>[1]
{
    $r
}",
    );
    insta::assert_debug_snapshot!(file);
}

// ---------------------------------------------------------------------------
// Multiplicity argument tests
// ---------------------------------------------------------------------------

#[test]
fn multiplicity_args_on_class() {
    let file = parse_ok(
        r"###Pure
Class my::Generic<T|m>
{
    value: T[1];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn multiplicity_args_on_function() {
    let file = parse_ok(
        r"###Pure
function my::test<Z|y>(col: Z[y]): Z[y]
{
    $col
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn multiplicity_args_concrete() {
    // Test concrete multiplicity arguments: <T|1>, <T|*>, <T|0..1>
    let file = parse_ok(
        r"###Pure
function my::test(a: Result<String|1>[1], b: Result<Integer|*>[*], c: Result<Boolean|0..1>[0..1]): Boolean[1]
{
    true
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn multiplicity_args_multiple() {
    // Multiple type args AND multiple multiplicity args
    let file = parse_ok(
        r"###Pure
function my::test(x: MyType<String, Integer|m, n>[1]): Boolean[1]
{
    true
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn multiplicity_args_only_mult_params() {
    // Class with only multiplicity parameters (no type params): <|m>
    let file = parse_ok(
        r"###Pure
Class my::OnlyMult<|m>
{
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn generic_arg_with_subtype_bound_and_wildcard_column() {
    // Real shape from
    // `core_functions_relation/relation/functions/eval.pure:18`:
    //
    //   meta::pure::functions::relation::eval<Z,T>(
    //       col:ColSpec<(?:Z)⊆T>[1], row:T[1]
    //   ) : Z[0..1]
    //
    // - `(?:Z)` is a relation column whose name is the wildcard `?`
    //   and whose type is `Z`.
    // - `⊆T` is a subtype constraint: the column-spec type is a
    //   subtype of `T`.
    //
    // Both syntaxes are accepted at parse time; the constraint and
    // wildcard semantics are resolved at compile time downstream.
    let file = parse_ok(
        "###Pure\n\
         native function my::eval<Z,T>(col: ColSpec<(?:Z)\u{2286}T>[1], row: T[1]): Z[0..1];",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn generic_arg_with_type_union() {
    // `T+V` is the structural type-union operator inside a generic
    // type argument. Real shape from
    // `core_functions_relation/.../transformation/asofjoin.pure:18`:
    //
    //   native function meta::pure::functions::relation::asOfJoin<T,V>(
    //       rel1:Relation<T>[1], rel2:Relation<V>[1], ...
    //   ): Relation<T+V>[1];
    //
    // The parser captures the `+`-chained types as `algebra_ops`
    // (`AlgebraOp { kind: Union, operand }`) on the base type-arg, so
    // the union survives to the resolver, which folds it into a
    // `GenericTypeOperation` tree and evaluates it against concrete
    // relation types (mirroring Java's `GenericTypeOperation`).
    let file = parse_ok(
        r"###Pure
native function my::asOfJoin<T,V>(rel:Relation<T+V>[1]): Relation<T+V>[1];",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn relation_rename_type_algebra() {
    // The full `rename` signature exercises every type-algebra slot the
    // parser must now capture (was discarded pre-feature). Real shape from
    // `core_functions_relation/.../rename.pure`:
    //
    //   native function meta::pure::functions::relation::rename<T,Z,K,V>(
    //       r:Relation<T>[1],
    //       old:ColSpec<Z=(?:K)\u{2286}T>[1],
    //       new:ColSpec<V=(?:K)>[1]
    //   ): Relation<T-Z+V>[1];
    //
    // The snapshot is the Phase-a gate: it must show
    //   - return type `Relation<T-Z+V>`: type-arg `T` carries
    //     `algebra_ops: [Difference(Z), Union(V)]` (left-associative);
    //   - param `old`: `equal_binding: Some("Z")`, `subset_bound: Some(T)`,
    //     and the `(?:K)` wildcard column preserved;
    //   - param `new`: `equal_binding: Some("V")`, `(?:K)` preserved.
    let file = parse_ok(
        "###Pure\n\
         native function my::rename<T,Z,K,V>(\
         r: Relation<T>[1], \
         old: ColSpec<Z=(?:K)\u{2286}T>[1], \
         new: ColSpec<V=(?:K)>[1]\
         ): Relation<T-Z+V>[1];",
    );
    insta::assert_debug_snapshot!(file);
}
