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

//! Enumeration parsing tests — snapshot-based AST validation.

mod helpers;

use helpers::parse_ok;

#[test]
fn basic_with_annotations() {
    let file = parse_ok(
        r"###Pure
Enum <<meta::pure::profiles::doc.deprecated>> {meta::pure::profiles::doc.doc = 'An enum'} my::Color
{
    <<meta::pure::profiles::doc.deprecated>> RED,
    GREEN,
    BLUE
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn quoted_names() {
    let file = parse_ok(
        r"###Pure
Enum '@'::'my Enum'
{
    'Anything e'
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn numeric_names() {
    let file = parse_ok(
        r"###Pure
Enum my::DayCountConvention
{
    '30_360',
    '30_ACT',
    ACT_365
}",
    );
    insta::assert_debug_snapshot!(file);
}
