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

//! Primitive literal parsing tests — snapshot-based AST validation.

mod helpers;

use helpers::parse_ok;

#[test]
fn decimal() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    1.0D;
    [1.0D, 3.0D];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn string() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    'ok';
    ['ok', 'bla'];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn integer() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    1;
    [1, 2];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn boolean() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    true;
    [true, false, true];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn mixed() {
    let file = parse_ok(
        r"###Pure
function my::test(): Any[*]
{
    [1, 'a', true]
}",
    );
    insta::assert_debug_snapshot!(file);
}
