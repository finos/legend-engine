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

//! Protocol shapes the engine parser builds while walking the grammar
//! (`DomainParseTreeWalker`), as the parity checks against it found them.

use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::from_protocol;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The converted elements of `code`, as JSON, without source information.
fn elements(code: &str) -> Vec<Value> {
    let file = legend_pure_parser_parser::parse(code, "shapes.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|e| strip(serde_json::to_value(e).expect("serializes")))
        .collect()
}

fn strip(value: Value) -> Value {
    match value {
        Value::Object(map) => Value::Object(
            map.into_iter()
                .filter(|(key, _)| key != "sourceInformation")
                .map(|(key, value)| (key, strip(value)))
                .collect(),
        ),
        Value::Array(items) => Value::Array(items.into_iter().map(strip).collect()),
        other => other,
    }
}

#[test]
fn a_non_convertible_measure_makes_its_first_unit_canonical() {
    // The engine's `visitMeasure`.
    let code = "Measure Currency\n{\n  USD;\n  GBP;\n  EUR;\n}";
    let measure = &elements(code)[0];
    assert_eq!(
        measure["canonicalUnit"],
        json!({"measure": "Currency", "name": "USD"})
    );
    assert_eq!(
        measure["nonCanonicalUnits"],
        json!([{"measure": "Currency", "name": "GBP"}, {"measure": "Currency", "name": "EUR"}])
    );
    // Back to the AST: still a measure without a canonical unit.
    let file = legend_pure_parser_parser::parse(code, "shapes.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    let back = from_protocol::convert_element(&pmcd.elements[0]).expect("converts back");
    let Some(legend_pure_parser_ast::element::Element::Measure(m)) = &back else {
        panic!("a measure: {back:?}");
    };
    assert!(m.canonical_unit.is_none());
    assert_eq!(m.non_canonical_units.len(), 3);
}

#[test]
fn a_bare_element_name_is_a_packageable_element_pointer() {
    // The engine's `instanceReference` without a call.
    let code = "function f(x: Any[1]): Any[*]\n{\n  $x->instanceOf(String)\n}";
    let call = &elements(code)[0]["body"][0];
    assert_eq!(
        call["parameters"][1],
        json!({"_type": "packageableElementPtr", "fullPath": "String"})
    );
}

#[test]
fn a_unit_as_a_value_is_the_engine_unit_type() {
    // The engine's `instanceReference: unitName` and `unitTypeReference`.
    let code =
        "function f(): Any[*]\n{\n  newUnit(my::Mass~Kilogram, 5)->cast(@my::Mass~Kilogram)\n}";
    let file = legend_pure_parser_parser::parse(code, "shapes.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    let body = &serde_json::to_value(&pmcd.elements[0]).expect("serializes")["body"][0];
    let si = |start_column: u32, end_column: u32| {
        serde_json::to_value(SourceInformation {
            source_id: "shapes.pure".into(),
            start_line: 3,
            start_column,
            end_line: 3,
            end_column,
        })
        .expect("a position serializes")
    };
    // From the measure to the unit.
    assert_eq!(
        body["parameters"][0]["parameters"][0],
        json!({"_type": "unitType", "fullPath": "my::Mass~Kilogram", "sourceInformation": si(11, 27)})
    );
    // `@` a unit: the unit, and its raw type unpositioned.
    let cast = &body["parameters"][1];
    assert_eq!(cast["sourceInformation"], si(40, 56));
    assert_eq!(
        cast["genericType"]["rawType"],
        json!({"_type": "packageableType", "fullPath": "my::Mass~Kilogram"})
    );
}

#[test]
fn the_root_package_calls_nothing() {
    // The engine's `instanceReference`: `::` with `allOrFunction`.
    for call in ["::.all()", "::(1)"] {
        let code = format!("function f(): Any[*]\n{{\n  {call}\n}}");
        let error = legend_pure_parser_parser::parse(&code, "shapes.pure")
            .expect_err("rejected")
            .errors
            .remove(0);
        assert_eq!(error.message(), "Expected a non-empty function caller");
        let si = error.source_info().expect("positioned");
        assert_eq!((si.start_line, si.start_column), (3, 3), "{call}");
    }
}

#[test]
fn all_after_an_element_name_is_the_engine_get_all_function() {
    // The engine's `allOrFunction`: `.all()`, `.allVersions()` and
    // `.all(<milestoning dates>)` after an element name.
    let code = "function f(p: Any[1]): Any[*]\n{\n  [my::Person.all(), Firm.allVersions(), my::Person.all(%latest, %2020-12-12), $p.all()]\n}";
    let values = &elements(code)[0]["body"][0]["values"];
    let person = json!({"_type": "packageableElementPtr", "fullPath": "my::Person"});
    assert_eq!(
        values[0],
        json!({"_type": "func", "function": "getAll", "parameters": [person]})
    );
    assert_eq!(
        values[1],
        json!({"_type": "func", "function": "getAllVersions", "parameters": [{"_type": "packageableElementPtr", "fullPath": "Firm"}]})
    );
    assert_eq!(
        values[2]["parameters"],
        json!([person, {"_type": "latestDate"}, {"_type": "strictDate", "value": "2020-12-12"}])
    );
    // On anything but an element name, `all()` is a qualified property.
    assert_eq!(values[3]["_type"], "property");
}

#[test]
fn latest_is_a_latest_date_wherever_it_appears() {
    let code = "Class A\n{\n  p: String[1];\n  latest() { $this.p(%latest) }: String[*];\n}";
    let qp = &elements(code)[0]["qualifiedProperties"][0];
    assert_eq!(
        qp["body"][0]["parameters"][1],
        json!({"_type": "latestDate"})
    );
    // And back: a latestDate is `%latest` again.
    let file = legend_pure_parser_parser::parse(code, "shapes.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    let back = from_protocol::convert_element(&pmcd.elements[0]).expect("converts back");
    assert!(format!("{back:?}").contains("LatestDate"), "{back:?}");
}

#[test]
fn a_bare_result_type_gets_the_engine_default_arguments() {
    // The engine's backward-compatibility rule in `processGenericType`:
    // `Result` without type arguments is `Result<Any|1..*>`.
    let code = "function f(r: Result[1], s: Result<String|1>[1], t: my::Result[1]): List<Result>[1]\n{\n  []\n}";
    let function = &elements(code)[0];
    let result_any = json!({
        "rawType": {"_type": "packageableType", "fullPath": "Result"},
        "typeArguments": [{"rawType": {"_type": "packageableType", "fullPath": "meta::pure::metamodel::type::Any"}}],
        "multiplicityArguments": [{"lowerBound": 1}],
    });
    assert_eq!(function["parameters"][0]["genericType"], result_any);
    assert_eq!(
        function["parameters"][1]["genericType"]["typeArguments"][0]["rawType"]["fullPath"],
        "String"
    );
    assert!(
        function["parameters"][2]["genericType"]
            .get("typeArguments")
            .is_none()
    );
    assert_eq!(
        function["returnGenericType"]["typeArguments"][0],
        result_any
    );
}

#[test]
fn column_specs_are_the_engine_col_spec_and_col_spec_array() {
    // The engine's `processOneColSpec`, wrapped as `colSpec`, or all of them
    // as `colSpecArray`.
    let code =
        "function f(): Any[*]\n{\n  [~a, ~b:String[1], ~c:x|$x.a:y|$y->sum(), ~[d, e:Integer]]\n}";
    let values = &elements(code)[0]["body"][0]["values"];
    assert_eq!(
        values[0],
        json!({"_type": "classInstance", "type": "colSpec", "value": {"name": "a"}})
    );
    assert_eq!(
        values[1]["value"],
        json!({
            "name": "b",
            "genericType": {"rawType": {"_type": "packageableType", "fullPath": "String"}},
            "multiplicity": {"lowerBound": 1, "upperBound": 1},
        })
    );
    let c = &values[2]["value"];
    assert_eq!(c["function1"]["_type"], "lambda");
    assert_eq!(c["function1"]["parameters"][0]["name"], "x");
    assert_eq!(c["function2"]["parameters"][0]["name"], "y");
    assert_eq!(values[3]["type"], "colSpecArray");
    assert_eq!(values[3]["value"]["colSpecs"][0], json!({"name": "d"}));
    assert_eq!(
        values[3]["value"]["colSpecs"][1]["genericType"]["rawType"]["fullPath"],
        "Integer"
    );
}

#[test]
fn function_tests_are_the_engine_function_test_suites() {
    // The engine's `visitFunction`: the block's own tests and data as suite
    // `default`, then the named suites.
    let code = "function my::hello(name: String[1], n: Integer[1]): String[1]\n{\n  $name\n}\n{\n  ModelStore: (JSON) '{\"a\":1}';\n  store::S: my::Data;\n  first 'The first' | hello('x', -1) => 'x';\n  S2\n  (\n    second | hello(E.A, [1, 2]) => (XML) '<a/>';\n  )\n}";
    let tests = &elements(code)[0]["tests"];
    assert_eq!(tests[0]["_type"], "functionTestSuite");
    assert_eq!(tests[0]["id"], "default");
    assert_eq!(
        tests[0]["testData"][0],
        json!({
            "packageableElementPointer": {"path": "ModelStore"},
            "data": {"_type": "externalFormat", "contentType": "application/json", "data": "{\"a\":1}"},
        })
    );
    assert_eq!(
        tests[0]["testData"][1]["data"],
        json!({"_type": "reference", "dataElement": {"type": "DATA", "path": "my::Data"}})
    );
    assert_eq!(
        tests[0]["tests"][0],
        json!({
            "_type": "functionTest",
            "id": "first",
            "doc": "The first",
            "parameters": [
                {"name": "name", "value": {"_type": "string", "value": "x"}},
                {"name": "n", "value": {"_type": "integer", "value": -1}},
            ],
            "assertions": [{"_type": "equalTo", "id": "default", "expected": {"_type": "string", "value": "x"}}],
        })
    );
    let second = &tests[1]["tests"][0];
    assert_eq!(tests[1]["id"], "S2");
    assert_eq!(
        second["parameters"][0]["value"],
        json!({"_type": "enumValue", "fullPath": "E", "value": "A"})
    );
    assert_eq!(second["parameters"][1]["value"]["_type"], "collection");
    assert_eq!(
        second["assertions"][0],
        json!({"_type": "equalToJson", "id": "default", "expected": {"_type": "externalFormat", "contentType": "application/xml", "data": "<a/>"}})
    );
}

#[test]
fn an_expected_value_of_other_embedded_data_is_unsupported() {
    // The engine accepts only relations there; the protocol has no form for
    // other data.
    let code = "function f(): String[1]\n{\n  'a'\n}\n{\n  t | f() => Other #{ a }#;\n}";
    let file = legend_pure_parser_parser::parse(code, "shapes.pure").expect("parses");
    let error = convert_source_file(&file).expect_err("embedded data");
    assert!(
        error.to_string().contains("embedded 'Other' data"),
        "{error}"
    );
}

#[test]
fn list_fields_the_engine_leaves_null_are_written_when_it_sets_them() {
    // `Enumeration.values` and `Measure.nonCanonicalUnits` default to null in
    // the engine's classes: they're written empty when the walker sets them.
    let code = "Enum E\n{\n}\n\nMeasure M\n{\n  *One: x -> $x;\n}\n\nMeasure N\n{\n  Only;\n}";
    let elements = elements(code);
    assert_eq!(elements[0]["values"], json!([]));
    assert_eq!(elements[1]["nonCanonicalUnits"], json!([]));
    assert!(elements[2].get("nonCanonicalUnits").is_none());
}

#[test]
fn dates_follow_the_engine_date_walker() {
    // `DateParseTreeWalker`: strict only with year, month and day; a
    // year-month date keeps its `%`.
    let code = "function f(): Any[*]\n{\n  [%2014, %2014-02, %2014-02-03, %2014-02-03T10:00:00, %10:12]\n}";
    let values = &elements(code)[0]["body"][0]["values"];
    assert_eq!(values[0], json!({"_type": "dateTime", "value": "2014"}));
    assert_eq!(values[1], json!({"_type": "dateTime", "value": "%2014-02"}));
    assert_eq!(
        values[2],
        json!({"_type": "strictDate", "value": "2014-02-03"})
    );
    assert_eq!(
        values[3],
        json!({"_type": "dateTime", "value": "2014-02-03T10:00:00"})
    );
    assert_eq!(values[4], json!({"_type": "strictTime", "value": "10:12"}));
}

#[test]
fn navigation_paths_are_the_engine_path() {
    // `NavigationParseTreeWalker`: `propertyPath` steps; enum values and
    // collections among the parameters keep no position; dates are
    // date-times without their `%`.
    let code =
        "function f(): Any[*]\n{\n  #/my::Person/name('x', E.V, [1], %2020-01-01)/firm!alias#\n}";
    let value = &elements(code)[0]["body"][0]["value"];
    assert_eq!(value["startType"], "my::Person");
    assert_eq!(value["name"], "alias");
    assert_eq!(value["path"][0]["_type"], "propertyPath");
    assert_eq!(value["path"][0]["property"], "name");
    let parameters = &value["path"][0]["parameters"];
    assert_eq!(parameters[0], json!({"_type": "string", "value": "x"}));
    assert_eq!(
        parameters[1],
        json!({"_type": "enumValue", "fullPath": "E", "value": "V"})
    );
    assert_eq!(
        parameters[2],
        json!({"_type": "collection", "multiplicity": {"lowerBound": 1, "upperBound": 1}, "values": [{"_type": "integer", "value": 1}]})
    );
    assert_eq!(
        parameters[3],
        json!({"_type": "dateTime", "value": "2020-01-01"})
    );
}
