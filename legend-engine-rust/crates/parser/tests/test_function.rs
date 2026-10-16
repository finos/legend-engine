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

//! Function parsing tests — snapshot-based AST validation.

mod helpers;

use helpers::{corpus, parse_ok};

#[test]
fn basic_with_body() {
    let file = parse_ok(&corpus("function_basic.pure"));
    insta::assert_debug_snapshot!(file);
}

#[test]
fn date_return_types() {
    let file = parse_ok(
        r"###Pure
function my::getDate(): StrictDate[1]
{
    %2024-01-15
}

function my::getDateTime(): DateTime[1]
{
    %2024-01-15T10:30:00
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn overloading() {
    let file = parse_ok(
        r"###Pure
function my::add(a: Integer[1]): Integer[1]
{
    $a
}

function my::add(a: Integer[1], b: Integer[1]): Integer[1]
{
    $a + $b
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn with_new_instance() {
    let file = parse_ok(
        r"###Pure
function my::create(): Any[1]
{
    ^my::Person(name='John', address=^my::Address(city='NYC'))
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn quoted_params_and_vars() {
    let file = parse_ok(
        r"###Pure
function my::func('1,2,3': Integer[3]): Integer[1]
{
    let '1,2,3' = [1, 2, 3];
    $'1,2,3'->at(0);
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn full_path_meta_execution() {
    let file = parse_ok(
        r"###Pure
function my::test(): Number[1]
{
    [1, 2, 3]->meta::pure::functions::math::max()
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn function_tests() {
    let file = parse_ok(&corpus("function_tests.pure"));
    insta::assert_debug_snapshot!(file);
}

#[test]
fn derived_multiple_statements() {
    let file = parse_ok(
        r"###Pure
Class my::WithDerived
{
    derived(s: String[1])
    {
        let x = 0;
        $x;
    }: Integer[1];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn with_annotations_and_import() {
    let file = parse_ok(
        r"###Pure
import model::*;
function <<doc.deprecated>> {doc.description = 'Old func'} my::oldFunc(): String[1]
{
    'deprecated'
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn multi_if_expressions() {
    let file = parse_ok(
        r"###Pure
function my::classify(x: Integer[1]): String[1]
{
    if($x > 0,
       | 'positive',
       | if($x < 0,
            | 'negative',
            | 'zero'))
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn native_function_simple() {
    let file = parse_ok(
        r"###Pure
native function meta::pure::functions::string::plus(strings: String[*]): String[1];",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn native_function_no_params() {
    let file = parse_ok(
        r"###Pure
native function meta::pure::functions::date::now(): DateTime[1];",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn function_with_constraint_block() {
    // Real shape from
    // `core_functions_standard/date/extract/dayOfWeekNumber.pure` —
    // an optional `[name : expr, ...]` block sits between the return-
    // multiplicity `]` and the body `{`. Mirrors the constraint slot
    // already supported on `Class` definitions.
    let file = parse_ok(
        r"###Pure
function my::test(x: Integer[1]): Integer[1]
[
   nonNegative : $x >= 0
]
{
   $x + 1
}",
    );
    insta::assert_debug_snapshot!(file);
}
