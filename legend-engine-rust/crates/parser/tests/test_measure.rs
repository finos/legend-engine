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

//! Measure parsing tests — snapshot-based AST validation.

mod helpers;

use helpers::{corpus, parse_ok};

#[test]
fn convertible() {
    let file = parse_ok(&corpus("measure_convertible.pure"));
    insta::assert_debug_snapshot!(file);
}

#[test]
fn non_convertible() {
    let file = parse_ok(
        r"###Pure
Measure pkg::NonConvertible
{
    UnitOne;
    UnitTwo;
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn quoted() {
    let file = parse_ok(
        r"###Pure
Measure pkg::'some measure'
{
    *'Unit One': x -> $x;
    'Unit Two': x -> $x * 1000;
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn class_with_unit_properties() {
    let file = parse_ok(
        r"###Pure
Measure pkg::NewMeasure
{
    *UnitOne: x -> $x;
    UnitTwo: x -> $x * 1000;
}

Class pkg::WithUnitProp
{
    weight: NewMeasure~UnitOne[0..1];
}",
    );
    insta::assert_debug_snapshot!(file);
}
