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

//! Class parsing tests — snapshot-based AST validation.

mod helpers;

use helpers::{corpus, parse_ok};

#[test]
fn basic() {
    let file = parse_ok(&corpus("class_basic.pure"));
    insta::assert_debug_snapshot!(file);
}

#[test]
fn complex_constraints() {
    let file = parse_ok(&corpus("class_constraints.pure"));
    insta::assert_debug_snapshot!(file);
}

#[test]
fn aggregation_kinds() {
    let file = parse_ok(
        r"###Pure
Class my::AggTest
{
    (shared) sharedProp: String[*];
    (composite) compositeProp: Integer[1];
    (none) noneProp: Boolean[0..1];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn multiple_annotations() {
    let file = parse_ok(
        r"###Pure
Class <<temporal.businesstemporal, temporal.processingtemporal>> {doc.description = 'A person', doc.todo = 'Add more fields'} model::Person
{
    <<doc.deprecated>> {doc.description = 'The name'} name: String[1];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn quoted_annotations() {
    let file = parse_ok(
        r"###Pure
Class <<'my profile'.'my stereo'>> my::QuotedClass
{
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn escaped_tagged_values() {
    let file = parse_ok(
        r"###Pure
Class {doc.description = 'test1\'s'} my::EscapedClass
{
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn quoted_package() {
    let file = parse_ok(
        r"###Pure
Class test::'p a c k a g e'::A
{
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn with_import() {
    let file = parse_ok(
        r"###Pure
import anything::*;
Class my::ImportedClass
{
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn unit_properties() {
    let file = parse_ok(
        r"###Pure
Class my::WithUnit
{
    weight: NewMeasure~UnitOne[0..1];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn default_values() {
    let file = parse_ok(&corpus("class_default_values.pure"));
    insta::assert_debug_snapshot!(file);
}

#[test]
fn sourceinfo_validation() {
    let file = parse_ok("###Pure\nClass my::Foo\n{\n    name: String[1];\n}\n");
    insta::assert_debug_snapshot!(file);
}

#[test]
fn type_parameters() {
    let file = parse_ok(
        r"###Pure
Class my::Generic<T>
{
    value: T[1];
}",
    );
    insta::assert_debug_snapshot!(file);
}
