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

//! Parser error case tests.
//!
//! These use `parse_err` to validate that malformed Pure source produces
//! appropriate error messages. No snapshots — just error message assertions.

mod helpers;

use helpers::{parse_err, parse_ok};

#[test]
fn unexpected_token() {
    parse_err(
        r"###Pure
asd Class my::Foo
{
}",
        "Unexpected token",
    );
}

#[test]
fn reserved_keywords() {
    // In the Java Pure grammar, keywords like 'false' can appear in path segments.
    // This test verifies the parser accepts them (matching Java engine behavior).
    let file = parse_ok(
        r"###Pure
Class false::me
{
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn invalid_aggregation_kind() {
    parse_err(
        r"###Pure
Class my::Foo
{
    (tunnel) prop: String[1];
}",
        "aggregation",
    );
}

#[test]
fn function_test_name_mismatch() {
    // The parser accepts a test that calls another function; the engine
    // rejects it, which `parse_section`'s engine profile checks.
    let file = parse_ok(
        r"###Pure
function my::hello(): String[1]
{
    'Hello'
}
{
    myTest | goodbye() => 'Hello';
}",
    );
    insta::assert_debug_snapshot!(file);
}

#[test]
fn code_block_lines_are_separated_and_terminated() {
    // `codeBlock: line (';' (line ';')*)?`: one line may stand alone; with
    // more, each ends with `;`.
    for (source, line, column) in [
        ("###Pure\nfunction f(): Any[1]\n{\n  1 2\n}", 4, 5),
        ("###Pure\nfunction f(): Any[1]\n{\n  1;\n  2\n}", 6, 1),
    ] {
        let errors = legend_pure_parser_parser::parse(source, "test.pure")
            .expect_err(source)
            .errors;
        let si = errors[0].source_info().expect("positioned");
        assert_eq!((si.start_line, si.start_column), (line, column), "{source}");
    }
    for source in [
        "###Pure\nfunction f(): Any[1]\n{\n  1\n}",
        "###Pure\nfunction f(): Any[1]\n{\n  1;\n}",
        "###Pure\nfunction f(): Any[1]\n{\n  1;\n  2;\n}",
    ] {
        legend_pure_parser_parser::parse(source, "test.pure").expect(source);
    }
}

#[test]
fn list_items_are_separated_by_commas() {
    // `item (',' item)*`: arguments, property assignments, parameters and
    // enum values need their commas, and a trailing one is an error. The
    // error is at the token that should have been `,` or the closing token.
    let function = |body: &str| format!("###Pure\nfunction f(): Any[1]\n{{\n  {body}\n}}");
    for (source, line, column) in [
        (function("f(1 2)"), 4, 7),
        (function("1->f(1 2)"), 4, 10),
        (function("$x.f(1 2)"), 4, 10),
        (function("^A(a=1 b=2)"), 4, 10),
        (function("^$x(a=1 b=2)"), 4, 11),
        (function("f(1,)"), 4, 7),
        (
            "###Pure\nfunction f(a: Integer[1] b: Integer[1]): Any[1]\n{\n  1\n}".to_string(),
            2,
            26,
        ),
        (
            "###Pure\nClass A\n{\n  q(a: Integer[1] b: Integer[1]) {1}: Integer[1];\n}".to_string(),
            4,
            19,
        ),
        ("###Pure\nEnum E\n{\n  A\n  B\n}".to_string(), 5, 3),
        ("###Pure\nEnum E\n{\n  A,\n}".to_string(), 5, 1),
    ] {
        let errors = legend_pure_parser_parser::parse(&source, "test.pure")
            .expect_err(&source)
            .errors;
        let si = errors[0].source_info().expect("positioned");
        assert_eq!((si.start_line, si.start_column), (line, column), "{source}");
    }
    for source in [
        function("f()"),
        function("1->f(1, 2)->g()"),
        function("^A(a=1, b=2)"),
        "###Pure\nfunction f(a: Integer[1], b: Integer[1]): Any[1]\n{\n  1\n}".to_string(),
        "###Pure\nEnum E\n{\n  A,\n  B\n}".to_string(),
        "###Pure\nEnum E\n{\n}".to_string(),
    ] {
        legend_pure_parser_parser::parse(&source, "test.pure").expect(&source);
    }
}
