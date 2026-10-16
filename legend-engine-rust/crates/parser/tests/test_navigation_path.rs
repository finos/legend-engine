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

//! Navigation path (`#/Type/prop1/prop2!alias#`) parser tests.
//!
//! These cover the syntactic surface only — Stage 1 of the DSL Path port
//! (parse + AST). Lowering and runtime evaluation land in later stages.

mod helpers;

use helpers::{parse_err, parse_ok};

#[test]
fn simple_single_property() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    #/Person/name#
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn multi_step_qualified_start_type() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    #/model::Firm/employees/address#
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn start_type_with_type_arguments() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    #/Firm<Any>/employees/address#
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn property_with_string_parameter() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    #/Person/nameWithTitle('Mr')#
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn property_with_multiple_parameters() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    #/Person/nameWithPrefixAndSuffix('a', 'b')#
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn property_with_collection_parameter() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    #/Person/nameWithPrefixAndSuffix('a', ['x', 'y'])#
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn property_with_enum_stub_parameter() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    #/Product/synonymsByType(SynonymType.CUSIP)/value#
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn alias_suffix_on_last_step() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    #/Product/synonymsByType(SynonymType.CUSIP)/value!cusip#
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn path_inside_arrow_call() {
    // The motivating real-world shape: paths flow into higher-order
    // functions like `sortBy` as first-class function values.
    let file = parse_ok(
        r"###Pure
function my::test(xs: Any[*]): Any[*]
{
    $xs->sortBy(#/model::Person/lastName#)
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn empty_path_is_a_parse_error() {
    // Java parity: NavigationGraphBuilder enforces "A path must contain
    // at least one navigation". `#/Type#` (no `/property` step) is
    // rejected at parse time.
    parse_err(
        r"###Pure
function my::test(): Any[*]
{
    #/Person#
}",
        "navigation step",
    );
}
