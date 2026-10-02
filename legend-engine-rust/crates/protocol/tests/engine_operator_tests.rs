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

//! Operator chains convert to exactly the engine parser's shapes.
//!
//! Expectations follow `DomainParseTreeWalker` (`combinedExpression`,
//! `arithmeticPart`, `processOp`, `booleanPart`, `processBooleanOp`,
//! `equalNotEqual`), quirks included. The anchor is the engine's pinned
//! fixture `SimpleMixedBooleanArithmeticConstraint.json`.

use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;

/// Renders a protocol value compactly: `plus([1, 2])`, `$x.name`, …
/// With `positions`, operator nodes and collections carry `@line:col-line:col`.
fn show(v: &serde_json::Value, positions: bool) -> String {
    let at = |v: &serde_json::Value| -> String {
        if !positions {
            return String::new();
        }
        let s = &v["sourceInformation"];
        if s.is_null() {
            return "@-".to_string();
        }
        let s: SourceInformation = serde_json::from_value(s.clone()).expect("a position");
        format!(
            "@{}:{}-{}:{}",
            s.start_line, s.start_column, s.end_line, s.end_column
        )
    };
    let all = |xs: &serde_json::Value| -> Vec<String> {
        xs.as_array()
            .map(|a| a.iter().map(|x| show(x, positions)).collect())
            .unwrap_or_default()
    };
    match v["_type"].as_str() {
        Some("func") => format!(
            "{}{}({})",
            v["function"].as_str().unwrap_or("?"),
            at(v),
            all(&v["parameters"]).join(", ")
        ),
        Some("collection") => format!("[{}]{}", all(&v["values"]).join(", "), at(v)),
        Some("var") => format!("${}", v["name"].as_str().unwrap_or("?")),
        Some("property") => format!(
            "{}.{}",
            all(&v["parameters"]).first().cloned().unwrap_or_default(),
            v["property"].as_str().unwrap_or("?")
        ),
        Some("integer" | "float" | "boolean") => v["value"].to_string(),
        Some("string") => format!("'{}'", v["value"].as_str().unwrap_or("")),
        Some(other) => other.to_string(),
        None => "?".to_string(),
    }
}

fn body(code: &str) -> serde_json::Value {
    let source = format!("function f():Any[*]\n{{\n{code}\n}}");
    let file = legend_pure_parser_parser::parse(&source, "ops.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")["body"][0].clone()
}

fn shape(code: &str) -> String {
    show(&body(code), false)
}

#[test]
fn same_operator_runs_share_one_collection() {
    assert_eq!(shape("1 + 2 + 3"), "plus([1, 2, 3])");
    assert_eq!(shape("1 - 2 - 3"), "minus([1, 2, 3])");
    assert_eq!(shape("1 * 2 * 3"), "times([1, 2, 3])");
}

#[test]
fn divide_nests_binary_to_the_left() {
    assert_eq!(shape("1 / 2 / 3"), "divide(divide(1, 2), 3)");
}

#[test]
fn different_additive_operators_nest() {
    assert_eq!(shape("1 + 2 - 3"), "minus([plus([1, 2]), 3])");
}

#[test]
fn product_after_additive_rebinds_the_last_operand() {
    assert_eq!(shape("1 + 2 * 3"), "plus([1, times([2, 3])])");
    assert_eq!(shape("1 * 2 + 3"), "plus([times([1, 2]), 3])");
    assert_eq!(shape("1 + 2 / 3"), "plus([1, divide(2, 3)])");
    assert_eq!(shape("1 / 2 + 3"), "plus([divide(1, 2), 3])");
}

#[test]
fn comparisons() {
    assert_eq!(shape("1 + 2 < 3"), "lessThan(plus([1, 2]), 3)");
    assert_eq!(shape("1 < 2 + 3"), "lessThan(1, plus([2, 3]))");
}

#[test]
fn only_one_level_of_precedence_is_fixed_up() {
    // Engine quirk: the product rebinds the relational's last operand,
    // which is already plus(2, 3).
    assert_eq!(
        shape("1 < 2 + 3 * 4"),
        "lessThan(1, times([plus([2, 3]), 4]))"
    );
}

#[test]
fn equality_binds_inside_its_left_expression() {
    // Engine quirk: `==` belongs to the operand on its left.
    assert_eq!(shape("$a + 1 == 2"), "plus([$a, equal(1, 2)])");
    assert_eq!(shape("$a == 1 + 2"), "equal($a, plus([1, 2]))");
    assert_eq!(shape("$a != $b"), "not(equal($a, $b))");
    assert_eq!(shape("!$a == $b"), "not(equal($a, $b))");
}

#[test]
fn boolean_parts() {
    assert_eq!(shape("$a || $b && $c"), "or($a, and($b, $c))");
    assert_eq!(shape("$a && $b || $c"), "or(and($a, $b), $c)");
    assert_eq!(
        shape("$a == 1 && $b == 2"),
        "and(equal($a, 1), equal($b, 2))"
    );
}

#[test]
fn unary_and_grouped_operands() {
    assert_eq!(shape("-$a + 1"), "plus([minus($a), 1])");
    assert_eq!(shape("($a + 1) * 2"), "times([plus([$a, 1]), 2])");
    assert_eq!(shape("$a->f() + 1"), "plus([f($a), 1])");
    assert_eq!(shape("!$a"), "not($a)");
}

#[test]
fn numbers_are_unsigned_and_minus_is_an_operator() {
    // Engine numbers have no sign; `-1` is signedExpression → minus(1).
    assert_eq!(shape("-1"), "minus(1)");
    assert_eq!(shape("2 -1"), "minus([2, 1])");
    assert_eq!(shape("2 - -1"), "minus([2, minus(1)])");
}

/// The engine's pinned fixture: `SimpleMixedBooleanArithmeticConstraint.json`
/// (legend-engine-language-pure-grammar test resources), constraint on line 3.
#[test]
fn engine_fixture_mixed_boolean_arithmetic_constraint() {
    let source = "Class test::C\n[\n   $this.id > 0 && $this.id < 30,\n  constraint1\n  (\n    ~function: $this.id > 0 && $this.id < 30\n    ~message: $this.id->toString() + ' is invalid'\n  )\n]\n{\n  id : Integer[1];\n}\n";
    let file = legend_pure_parser_parser::parse(source, "").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    let class = serde_json::to_value(&pmcd.elements[0]).expect("serializes");
    let lambda = &class["constraints"][0]["functionDefinition"];
    assert_eq!(
        show(&lambda["body"][0], false),
        "lessThan(and(greaterThan($this.id, 0), $this.id), 30)"
    );
    assert_eq!(
        show(&lambda["body"][0], true),
        "lessThan@3:29-3:32(and@3:17-3:18(greaterThan@3:13-3:15($this.id, 0), $this.id), 30)"
    );
}

/// Engine shapes survive protocol → AST → protocol unchanged, including the
/// quirky ones (the AST rebuilt from protocol replays to the same tree).
#[test]
fn engine_shapes_round_trip_through_the_ast() {
    use legend_pure_parser_protocol::v1::convert::convert_expression_typed;
    use legend_pure_parser_protocol::v1::from_protocol::convert_value_spec_to_expression;
    use legend_pure_parser_protocol::v1::value_spec::ValueSpecification;

    for code in [
        "1 + 2 + 3",
        "1 + 2 * 3",
        "1 / 2 / 3",
        "$a != $b",
        "$a || $b && $c",
        "$a + 1 == 2",
        "1 < 2 + 3 * 4",
        "$this.id > 0 && $this.id < 30",
    ] {
        let json = body(code);
        let protocol: ValueSpecification = serde_json::from_value(json.clone()).expect("typed");
        let ast = convert_value_spec_to_expression(&protocol).expect("reverses");
        let again = convert_expression_typed(&ast).expect("converts");
        let again = serde_json::to_value(&again).expect("serializes");
        assert_eq!(show(&again, false), show(&json, false), "{code}");
    }
}

/// Function names carry the engine's signature suffix
/// (`HelperValueSpecificationGrammarComposer.getFunctionSignature`), and the
/// section index lists those names.
#[test]
fn function_names_carry_the_engine_signature() {
    let source = "function a::f(x: String[1], y: b::T[*], z: Integer[1..*]): Boolean[0..1] { true }\nfunction a::g(): Any[1] { 1 }\n";
    let file = legend_pure_parser_parser::parse(source, "sig.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    let json = serde_json::to_value(&pmcd).expect("serializes");
    let elements = json["elements"].as_array().expect("elements");
    assert_eq!(
        elements[0]["name"],
        "f_String_1__T_MANY__Integer_$1_MANY$__Boolean_$0_1$_"
    );
    assert_eq!(elements[1]["name"], "g__Any_1_");
    let index = &elements[2];
    assert_eq!(index["name"], "SectionIndex");
    assert_eq!(
        index["sections"][0]["elements"],
        serde_json::json!([
            "a::f_String_1__T_MANY__Integer_$1_MANY$__Boolean_$0_1$_",
            "a::g__Any_1_"
        ])
    );
}
