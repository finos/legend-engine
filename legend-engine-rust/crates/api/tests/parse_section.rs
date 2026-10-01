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

//! The engine section contract, with the island grammars linked the way
//! the JNI library links them.

use legend_pure_dsl_authentication as _;
use legend_pure_dsl_connection as _;
use legend_pure_dsl_data as _;
use legend_pure_dsl_data_space as _;
use legend_pure_dsl_dataquality as _;
use legend_pure_dsl_deephaven as _;
use legend_pure_dsl_diagram as _;
use legend_pure_dsl_elasticsearch as _;
use legend_pure_dsl_external_format as _;
use legend_pure_dsl_graph as _;
use legend_pure_dsl_mapping as _;
use legend_pure_dsl_mongodb as _;
use legend_pure_dsl_persistence as _;
use legend_pure_dsl_persistence_cloud as _;
use legend_pure_dsl_persistence_relational as _;
use legend_pure_dsl_relational as _;
use legend_pure_dsl_relational_snowflake as _;
use legend_pure_dsl_runtime as _;
use legend_pure_dsl_service as _;
use legend_pure_dsl_service_store as _;
use legend_pure_dsl_snowflake as _;
use legend_pure_dsl_sql as _;
use legend_pure_dsl_store as _;
use legend_pure_dsl_tds as _;
use legend_pure_parser_api::{
    ErrorKind, SectionRequest, capabilities, parse_section, parse_section_json,
};

/// A `###Pure` body as the engine hands it over for headerless code:
/// `"\n###Pure\n"` was prepended, so the body starts with a newline and
/// the section's line offset is -1.
fn pure(code: &str) -> SectionRequest<'_> {
    SectionRequest {
        section_type: "Pure",
        code,
        source_id: "model.pure",
        line_offset: -1,
        column_offset: 0,
        return_source_info: true,
    }
}

#[test]
fn capabilities_report_linked_grammars() {
    let caps = capabilities();
    assert_eq!(
        caps.sections,
        [
            "Pure",
            "Connection",
            "Data",
            "DataQualityValidation",
            "DataSpace",
            "Diagram",
            "Elasticsearch",
            "ExternalFormat",
            "Mapping",
            "Persistence",
            "Relational",
            "Runtime",
            "Service",
            "ServiceStore",
            "Snowflake"
        ]
    );
    assert!(
        caps.islands.contains(&String::new()),
        "graph fetch: {caps:?}"
    );
    assert!(
        caps.islands.contains(&">".to_string()),
        "store accessor: {caps:?}"
    );
}

#[test]
fn a_section_without_imports_has_none() {
    // `###Data` has no imports: the engine makes it a `DefaultCodeSection`.
    let result = parse_section(&SectionRequest {
        section_type: "Data",
        ..pure("\nData my::D\n{\n  Reference #{ my::Other }#\n}\n")
    })
    .expect("parses");
    assert_eq!(result.imports, None);
    assert_eq!(result.elements[0]["_type"], "dataElement");
    // Positioned in the document: the body's first line follows the header.
    assert_eq!(result.elements[0]["sourceInformation"]["startLine"], 1);
    let json = parse_section_json(&SectionRequest {
        section_type: "Data",
        ..pure("\nData my::D\n{\n}\n")
    });
    assert!(!json.contains("imports"), "{json}");
}

#[test]
fn elements_and_imports_with_document_positions() {
    let result = parse_section(&pure(
        "\nimport my::pkg::*;\nClass a::Person\n{\n  name: String[1];\n}\n",
    ))
    .expect("parses");
    assert_eq!(result.imports, Some(vec!["my::pkg".to_string()]));
    assert_eq!(result.elements.len(), 1);
    let class = &result.elements[0];
    assert_eq!(class["_type"], "class");
    assert_eq!(class["package"], "a");
    assert_eq!(class["name"], "Person");
    // Caller's line 2 (after the import on line 1).
    assert_eq!(class["sourceInformation"]["startLine"], 2);
    assert_eq!(class["sourceInformation"]["sourceId"], "model.pure");
    assert_eq!(class["properties"][0]["sourceInformation"]["startLine"], 4);
}

#[test]
fn no_source_information_when_not_requested() {
    let mut request = pure("\nClass <<doc.deprecated>> a::A { p: String[1]; }\n");
    request.return_source_info = false;
    let result = parse_section(&request).expect("parses");
    let json = serde_json::to_string(&result.elements).expect("serializes");
    assert!(!json.contains("ourceInformation"), "{json}");
}

#[test]
fn graph_fetch_island_converts() {
    let result = parse_section(&pure(
        "\nfunction f():Any[*] { #{ a::Person { name } }# }\n",
    ))
    .expect("parses");
    let island = &result.elements[0]["body"][0];
    assert_eq!(island["_type"], "classInstance");
    assert_eq!(island["type"], "rootGraphFetchTree");
}

#[test]
fn first_syntax_error_wins_with_its_position() {
    let error = parse_section(&pure("\nClass a::A { p: }\nClass b::B { q: }\n"))
        .expect_err("does not parse");
    assert_eq!(error.kind, ErrorKind::Parser);
    let si = error.source_information.expect("positioned");
    assert_eq!(si.start_line, 1);
    assert!(
        !error.message.contains("model.pure"),
        "no position in message: {}",
        error.message
    );
}

#[test]
fn native_function_is_unsupported() {
    let error =
        parse_section(&pure("\nnative function a::f(): String[1];\n")).expect_err("unsupported");
    assert_eq!(error.kind, ErrorKind::Unsupported);
    assert_eq!(error.source_information.expect("positioned").start_line, 1);
}

#[test]
fn unlinked_section_type_is_unsupported() {
    // A type no grammar here reads, as an engine extension's may be.
    let mut request = pure("\nWidget a::W {}\n");
    request.section_type = "Widget";
    assert_eq!(
        parse_section(&request).expect_err("unsupported").kind,
        ErrorKind::Unsupported
    );
}

#[test]
fn offsets_below_minus_one_are_unsupported() {
    let mut request = pure("\nClass a::A {}\n");
    request.line_offset = -2;
    assert_eq!(
        parse_section(&request).expect_err("unsupported").kind,
        ErrorKind::Unsupported
    );
}

#[test]
fn json_shapes() {
    let ok: serde_json::Value =
        serde_json::from_str(&parse_section_json(&pure("\nClass a::A {}\n"))).expect("json");
    assert_eq!(ok["elements"][0]["name"], "A");
    assert!(ok["imports"].as_array().is_some_and(Vec::is_empty));

    let err: serde_json::Value =
        serde_json::from_str(&parse_section_json(&pure("\nClass a::A { p: }\n"))).expect("json");
    assert_eq!(err["error"]["kind"], "parser");
    assert_eq!(err["error"]["sourceInformation"]["startLine"], 1);
}

#[test]
fn an_island_without_a_linked_grammar_is_unsupported() {
    // The engine may have a Java parser for it: it parses the section itself.
    let error = parse_section(&pure("\nfunction f(): Any[*]\n{\n  #GQL{query}#\n}\n"))
        .expect_err("no GQL island is linked");
    assert_eq!(error.kind, ErrorKind::Unsupported, "{error:?}");
    let si = error.source_information.expect("positioned");
    assert_eq!((si.start_line, si.start_column), (3, 3));
}

#[test]
fn type_and_multiplicity_parameters_are_rejected_like_the_engine() {
    // Legend Pure accepts them; the engine's walker rejects them at the `<`.
    for code in [
        "\nClass x::X<T|m>{}\n",
        "\nfunction x::f<|m>(x: String[1]): String[1] { 'ok' }\n",
    ] {
        let error = parse_section(&pure(code)).expect_err(code);
        assert_eq!(error.kind, ErrorKind::Parser, "{error:?}");
        assert!(
            error.message.contains("not authorized in Legend Engine"),
            "{error:?}"
        );
        let si = error.source_information.expect("positioned");
        let column = code
            .lines()
            .nth(1)
            .and_then(|l| l.find('<'))
            .expect("a `<`")
            + 1;
        assert_eq!(
            (si.start_line, si.start_column),
            (1, u32::try_from(column).unwrap())
        );
    }
}

#[test]
fn a_function_test_must_call_its_function() {
    let error = parse_section(&pure(
        "\nfunction my::hello(): String[1]\n{\n  'Hello'\n}\n{\n  myTest | goodbye() => 'Hello';\n}\n",
    ))
    .expect_err("the test calls another function");
    assert_eq!(error.kind, ErrorKind::Parser, "{error:?}");
    assert!(
        error.message.contains("'goodbye' does not match"),
        "{error:?}"
    );
    let si = error.source_information.expect("positioned");
    assert_eq!((si.start_line, si.start_column), (6, 12));
}

/// Where a JSON node's source information starts and ends.
fn span(node: &serde_json::Value) -> (u64, u64, u64, u64) {
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

#[test]
fn relation_test_data_lists_are_always_written() {
    // The engine's classes default them to null, so an empty list is written.
    let result = parse_section(&pure(
        "\nfunction my::f(): Any[*]\n{\n  1\n}\n{\n  store::DB: Relation #{:a;}#;\n  t | f() => 1;\n}\n",
    ))
    .expect("parses");
    let data = &result.elements[0]["tests"][0]["testData"][0]["data"];
    assert_eq!(data["_type"], "relationAccessor");
    let element = &data["relationElements"][0];
    assert_eq!(element["paths"], serde_json::json!([]));
    assert_eq!(element["columns"], serde_json::json!(["a"]));
    assert_eq!(element["rows"], serde_json::json!([]));
}

#[test]
fn an_expected_relation_is_positioned_as_the_engine_does() {
    // The engine reads the body trimmed, behind `: `, as if that text
    // started right after `#{`: `1, John;` is reported a line up.
    let code = "\nfunction my::f(): Any[*]\n{\n  1\n}\n{\n  t | f() => Relation\n  #{\n    id, name\n    1, John;\n  }#;\n}\n";
    let result = parse_section(&pure(code)).expect("parses");
    let assertion = &result.elements[0]["tests"][0]["tests"][0]["assertions"][0];
    assert_eq!(assertion["_type"], "equalToRelation");
    // `Relation` to `}#`.
    assert_eq!(span(assertion), (6, 14, 10, 4));
    let expected = &assertion["expected"];
    assert_eq!(expected["paths"], serde_json::json!([]));
    assert_eq!(expected["columns"], serde_json::json!(["id", "name"]));
    assert_eq!(
        expected["rows"],
        serde_json::json!([{"values": ["1", "John"]}])
    );
    // `:` right after `#{`, to the `;` ending the text's second line.
    assert_eq!(span(expected), (7, 5, 8, 12));

    // Unlike an `equalTo`'s, the relation has no positions when none are
    // asked for: the engine reads it with the section's own setting.
    let mut request = pure(code);
    request.return_source_info = false;
    let result = parse_section(&request).expect("parses");
    let expected = &result.elements[0]["tests"][0]["tests"][0]["assertions"][0]["expected"];
    assert!(expected.get("sourceInformation").is_none(), "{expected}");
}

#[test]
fn empty_embedded_data_is_rejected_like_the_engine() {
    let error = parse_section(&pure(
        "\nfunction my::f(): Any[*]\n{\n  1\n}\n{\n  store::DB: Relation #{}#;\n  t | f() => 1;\n}\n",
    ))
    .expect_err("empty data");
    assert_eq!(error.kind, ErrorKind::Parser, "{error:?}");
    assert!(error.message.contains("must not be empty"), "{error:?}");
    let si = error.source_information.expect("positioned");
    assert_eq!((si.start_line, si.start_column), (6, 14));
}

#[test]
fn embedded_data_without_a_linked_grammar_is_unsupported() {
    // The engine may have a Java parser for it: it parses the section itself.
    let error = parse_section(&pure(
        "\nfunction my::f(): Any[*]\n{\n  1\n}\n{\n  store::DB: Other #{ x }#;\n  t | f() => 1;\n}\n",
    ))
    .expect_err("no grammar for `Other`");
    assert_eq!(error.kind, ErrorKind::Unsupported, "{error:?}");
}

#[test]
fn a_test_expects_only_relations_as_embedded_data() {
    // The engine's walker rejects any other type there, at the whole data.
    let error = parse_section(&pure(
        "\nfunction my::f(): Any[*]\n{\n  1\n}\n{\n  t | f() => Other #{ a }#;\n}\n",
    ))
    .expect_err("only relations");
    assert_eq!(error.kind, ErrorKind::Parser, "{error:?}");
    assert!(
        error
            .message
            .contains("type for function test assertion: Other. Only 'Relation' is supported"),
        "{error:?}"
    );
    let si = error.source_information.expect("positioned");
    assert_eq!((si.start_line, si.start_column), (6, 14));
}
