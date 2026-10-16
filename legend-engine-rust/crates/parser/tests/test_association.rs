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

//! Association parsing tests — snapshot-based AST validation.

mod helpers;

use helpers::parse_ok;

#[test]
fn basic() {
    let file = parse_ok(
        r"###Pure
Association model::Person_Firm
{
    employee: model::Person[*];
    employer: model::Firm[*];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn aggregation_kinds() {
    let file = parse_ok(
        r"###Pure
Association model::SharedAssoc
{
    (shared) left: model::A[*];
    (none) right: model::B[*];
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn with_annotations_and_import() {
    let file = parse_ok(
        r"###Pure
import model::*;
Association <<doc.deprecated>> {doc.description = 'Legacy'} model::OldAssoc
{
    left: A[1];
    right: B[1];
}",
    );
    insta::assert_debug_snapshot!(file);
}
