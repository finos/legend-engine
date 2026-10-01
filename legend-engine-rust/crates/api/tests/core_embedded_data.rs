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

//! The engine core's embedded data types, `ExternalFormat`, `ModelStore`
//! and `Reference`, as a function test's store data: the engine's JSON and
//! its errors, at its positions.

use legend_pure_parser_api::{ErrorKind, SectionError, SectionRequest, parse_section};
use serde_json::{Value, json};

/// The data line's number in the document.
const LINE: u64 = 6;

/// A function whose test gives store `store::S` the data `data`, written on
/// line [`LINE`].
fn source(data: &str) -> String {
    format!(
        "\nfunction my::f(): Any[*]\n{{\n  1\n}}\n{{\n  store::S: {data};\n  t | f() => 1;\n}}\n"
    )
}

fn parse(data: &str) -> Result<Value, SectionError> {
    let code = source(data);
    let request = SectionRequest {
        section_type: "Pure",
        code: &code,
        source_id: "",
        line_offset: -1,
        column_offset: 0,
        return_source_info: true,
    };
    parse_section(&request)
        .map(|result| result.elements[0]["tests"][0]["testData"][0]["data"].clone())
}

/// The column of `needle` (its first occurrence) on the data line.
fn column(data: &str, needle: &str) -> u64 {
    let line = format!("  store::S: {data};");
    let index = line
        .find(needle)
        .unwrap_or_else(|| panic!("no {needle} in {line}"));
    u64::try_from(index).expect("fits") + 1
}

fn span(node: &Value) -> (u64, u64, u64, u64) {
    let si = &node["sourceInformation"];
    let at = |key: &str| {
        si[key]
            .as_u64()
            .unwrap_or_else(|| panic!("no {key} in {node}"))
    };
    (
        at("startLine"),
        at("startColumn"),
        at("endLine"),
        at("endColumn"),
    )
}

/// The whole embedded data's span: from its type to `}#`.
fn data_span(data: &str) -> (u64, u64, u64, u64) {
    let start = column(data, data);
    (
        LINE,
        start,
        LINE,
        start + u64::try_from(data.len()).expect("fits") - 1,
    )
}

/// A syntax error's message and start.
fn error(data: &str) -> (String, (u64, u64)) {
    let error = parse(data).expect_err(data);
    assert_eq!(error.kind, ErrorKind::Parser, "{data}: {error:?}");
    let si = error.source_information.expect("positioned");
    (
        error.message,
        (u64::from(si.start_line), u64::from(si.start_column)),
    )
}

#[test]
fn external_format_data_has_both_fields_in_any_order() {
    let data = "ExternalFormat #{ data: '{\"a\":1}'; contentType: 'application/json'; }#";
    let value = parse(data).expect("parses");
    assert_eq!(value["_type"], "externalFormat");
    assert_eq!(value["contentType"], "application/json");
    assert_eq!(value["data"], "{\"a\":1}");
    assert_eq!(span(&value), data_span(data));
}

#[test]
fn external_format_data_needs_each_field_once() {
    for (data, message) in [
        (
            "ExternalFormat #{ data: 'x'; }#",
            "Field 'contentType' is required",
        ),
        (
            "ExternalFormat #{ contentType: 'a'; }#",
            "Field 'data' is required",
        ),
        (
            "ExternalFormat #{ contentType: 'a'; data: 'x'; data: 'y'; }#",
            "Field 'data' should be specified only once",
        ),
    ] {
        let (actual, start) = error(data);
        assert_eq!(actual, message);
        // At the whole embedded data, as the engine reports them.
        assert_eq!(start, (LINE, column(data, data)), "{data}");
    }
    let data = "ExternalFormat #{ other: 'x'; }#";
    assert_eq!(error(data).1, (LINE, column(data, "other")));
}

#[test]
fn reference_data_names_a_data_element() {
    let data = "Reference #{ my::'Data 1' }#";
    let value = parse(data).expect("parses");
    assert_eq!(value["_type"], "reference");
    assert_eq!(value["dataElement"]["type"], "DATA");
    assert_eq!(value["dataElement"]["path"], "my::Data 1");
    // Both are positioned on the whole embedded data.
    assert_eq!(span(&value), data_span(data));
    assert_eq!(span(&value["dataElement"]), data_span(data));

    for (data, message) in [
        (
            "Reference #{   }#",
            "Path should be provided for DataElementReference",
        ),
        ("Reference #{ my::a-b }#", "Invalid identifier: a-b"),
    ] {
        assert_eq!(
            error(data),
            (message.to_string(), (LINE, column(data, data)))
        );
    }
}

#[test]
fn model_store_instances_are_new_calls_positioned_on_their_values() {
    let data = "ModelStore #{ my::Person: [^my::Person(name = 'x', age = -1, gender = my::G.M, \
                tags = [1, +2], firm = ^my::Firm(n = 2.5D))], my::Firm: ExternalFormat \
                #{ contentType: 'a'; data: 'b'; }#, my::None: [] }#";
    let value = parse(data).expect("parses");
    assert_eq!(value["_type"], "modelStore");
    assert_eq!(span(&value), data_span(data));

    let people = &value["modelData"][0];
    assert_eq!(people["_type"], "modelInstanceData");
    assert_eq!(people["model"], "my::Person");
    let end = column(data, "], my::Firm");
    assert_eq!(span(people), (LINE, column(data, "my::Person:"), LINE, end));

    // A collection of `new(Type, 'dummy', [key = [values]])`, positioned only
    // on the values.
    let instances = &people["instances"];
    assert_eq!(instances["_type"], "collection");
    assert_eq!(
        instances["multiplicity"],
        json!({"lowerBound": 1, "upperBound": 1})
    );
    assert!(instances.get("sourceInformation").is_none());
    let person = &instances["values"][0];
    assert_eq!(person["function"], "new");
    let parameters = &person["parameters"];
    assert_eq!(
        parameters[0],
        json!({"_type": "packageableElementPtr", "fullPath": "my::Person"})
    );
    assert_eq!(parameters[1], json!({"_type": "string", "value": "dummy"}));
    let assignments = &parameters[2]["values"];
    assert_eq!(parameters[2]["multiplicity"]["lowerBound"], 5);
    let values = |i: usize| {
        let assignment = &assignments[i];
        assert_eq!(assignment["_type"], "keyExpression");
        assert_eq!(assignment["add"], false);
        assignment["expression"]["values"].clone()
    };
    assert_eq!(
        assignments[1]["key"],
        json!({"_type": "string", "value": "age"})
    );

    // A signed number is one literal, from its sign.
    let age = &values(1)[0];
    assert_eq!(
        (age["_type"].clone(), age["value"].clone()),
        (json!("integer"), json!(-1))
    );
    let minus = column(data, "-1");
    assert_eq!(span(age), (LINE, minus, LINE, minus + 1));

    let gender = &values(2)[0];
    assert_eq!(gender["_type"], "enumValue");
    assert_eq!(gender["fullPath"], "my::G");
    assert_eq!(gender["value"], "M");
    let enumeration = column(data, "my::G.M");
    assert_eq!(span(gender), (LINE, enumeration, LINE, enumeration + 6));

    let tags = values(3);
    assert_eq!(tags[0]["value"], 1);
    assert_eq!(tags[1]["value"], 2);
    assert_eq!(
        assignments[3]["expression"]["multiplicity"]["lowerBound"],
        2
    );

    let firm = &values(4)[0];
    assert_eq!(firm["function"], "new");
    let n = &firm["parameters"][2]["values"][0]["expression"]["values"][0];
    assert_eq!(
        (n["_type"].clone(), n["value"].to_string()),
        (json!("decimal"), "2.5".to_string())
    );

    let embedded = &value["modelData"][1];
    assert_eq!(embedded["_type"], "modelEmbeddedData");
    assert_eq!(embedded["model"], "my::Firm");
    assert_eq!(embedded["data"]["_type"], "externalFormat");
    assert_eq!(embedded["data"]["contentType"], "a");

    let none = &value["modelData"][2]["instances"];
    assert_eq!(
        none["multiplicity"],
        json!({"lowerBound": 0, "upperBound": 0})
    );
}

#[test]
fn model_store_data_has_one_entry_per_type() {
    let data = "ModelStore #{ my::A: [], my::A: [] }#";
    let (message, start) = error(data);
    assert_eq!(message, "Multiple entries found for type: 'my::A'");
    // At the second entry.
    assert_eq!(start, (LINE, column(data, "my::A: [] }#")));
}
