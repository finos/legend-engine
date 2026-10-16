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

//! Profile parsing tests — snapshot-based AST validation.

mod helpers;

use helpers::parse_ok;

#[test]
fn basic() {
    let file = parse_ok(
        r"###Pure
Profile meta::pure::profiles::doc
{
    stereotypes: [deprecated, experimental];
    tags: [doc, todo];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn quoted() {
    let file = parse_ok(
        r"###Pure
Profile meta::pure::profiles::'with quotes'
{
    stereotypes: [deprecated];
    tags: [doc];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn empty() {
    let file = parse_ok(
        r"###Pure
Profile my::EmptyProfile
{
}",
    );
    insta::assert_debug_snapshot!(file);
}
