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

//! Linking this crate registers the `###Mapping` section grammar with both
//! the parser (`SECTION_PARSERS`) and the protocol converter
//! (`ELEMENT_PROTOCOLS`): mappings convert to the engine's JSON, positioned
//! as its `MappingParseTreeWalker` positions them.

use legend_pure_dsl_mapping as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::{SourceInformation, edit_positions};
use serde_json::{Value, json};

/// The first mapping `body`, a `###Mapping` section's, defines.
fn mapping(body: &str) -> Value {
    let source = format!("###Mapping\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "m.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

/// The first error `body` has.
fn error(body: &str) -> ParseError {
    let source = format!("###Mapping\n{body}");
    legend_pure_parser_parser::parse(&source, "m.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn position(error: &ParseError) -> (u32, u32, u32, u32) {
    let si = error.source_info().expect("positioned");
    (si.start_line, si.start_column, si.end_line, si.end_column)
}

/// A JSON node's source information.
fn source_information(node: &Value) -> SourceInformation {
    serde_json::from_value(node["sourceInformation"].clone())
        .unwrap_or_else(|e| panic!("no position in {node}: {e}"))
}

fn span(node: &Value) -> (u64, u64, u64, u64) {
    let si = source_information(node);
    (
        si.start_line.into(),
        si.start_column.into(),
        si.end_line.into(),
        si.end_column.into(),
    )
}

/// The source id of every position in `node`.
fn source_ids(node: &Value, ids: &mut Vec<String>) {
    edit_positions(&mut node.clone(), &mut |position| {
        ids.push(position.source_id.clone());
    })
    .expect("positions");
}

#[test]
fn an_include_keeps_its_store_substitution_when_it_has_one() {
    let m = mapping(
        "Mapping my::M\n(\n  include my::A\n  include mapping my::B[my::S1 -> my::S2]\n  include my::C[s1 -> s2, s3 -> s4]\n)\n",
    );
    assert_eq!(m["_type"], "mapping");
    assert_eq!(m["package"], "my");
    assert_eq!(m["name"], "M");
    let includes = &m["includedMappings"];
    assert_eq!(
        includes[0],
        json!({"_type": "mappingIncludeMapping", "includedMapping": "my::A", "sourceInformation": includes[0]["sourceInformation"]})
    );
    // `include mapping`: the core type, named.
    assert_eq!(includes[1]["includedMapping"], "my::B");
    assert_eq!(includes[1]["sourceDatabasePath"], "my::S1");
    assert_eq!(includes[1]["targetDatabasePath"], "my::S2");
    // Line 5, `  include mapping my::B[my::S1 -> my::S2]`.
    assert_eq!(span(&includes[1]), (5, 3, 5, 41));
    // Two substitutions: the engine keeps neither.
    assert!(includes[2].get("sourceDatabasePath").is_none());
    // The lists are always there; the suites only when there are some.
    for list in [
        "classMappings",
        "enumerationMappings",
        "associationMappings",
        "tests",
    ] {
        assert_eq!(m[list], json!([]), "{list}");
    }
    assert!(m.get("testSuites").is_none());
    assert_eq!(span(&m), (2, 1, 7, 1));
}

#[test]
fn a_data_space_include_names_the_data_space() {
    // Its store substitutions read, and dropped, as the data space
    // extension drops them.
    let m = mapping(
        "Mapping my::M\n(\n  include mapping my::A   include dataspace my::'D S'[my::S1 -> my::S2]\n)\n",
    );
    let includes = &m["includedMappings"];
    assert_eq!(includes[0]["_type"], "mappingIncludeMapping");
    assert_eq!(
        includes[1],
        json!({
            "_type": "mappingIncludeDataSpace",
            "includedDataSpace": "my::D S",
            "sourceInformation": includes[1]["sourceInformation"],
        })
    );
    // From `include` to `]`.
    assert_eq!(span(&includes[1]), (4, 27, 4, 71));
}

#[test]
fn an_include_of_another_type_is_unsupported() {
    // An engine extension may read it.
    let error = error("Mapping my::M\n(\n  include other my::D\n)\n");
    assert!(matches!(error, ParseError::Unsupported { .. }), "{error}");
}

#[test]
fn an_element_of_a_type_without_grammar_is_unsupported() {
    let error = error("Mapping my::M\n(\n  *my::P[p]: Unported\n  {\n    x: y\n  }\n)\n");
    assert!(matches!(error, ParseError::Unsupported { .. }), "{error}");
    // The whole element.
    assert_eq!(position(&error), (4, 3, 7, 3));
}

#[test]
fn the_section_is_read_before_its_elements_bodies() {
    // The body of the first element is unsupported, but the grammar error
    // after it comes first, as the engine reads the whole section first.
    let error = error("Mapping my::M\n(\n  my::P: Unported { x }\n  my::Q Unported { }\n)\n");
    assert!(matches!(error, ParseError::Unexpected { .. }), "{error}");
    assert_eq!(position(&error).0, 5);
}

#[test]
fn a_legacy_test_reads_its_query_as_a_source_of_the_mappings() {
    let m = mapping(
        "Mapping my::M\n(\n  MappingTests\n  [\n    test1\n    (\n      query: |my::P.all();\n      data: [<Object, JSON, my::S, '{\"a\":\\'1\\'}' + '{}'>];\n      assert: '[{\"a\":\\'1\\'}]';\n    )\n  ]\n)\n",
    );
    let test = &m["tests"][0];
    assert_eq!(test["name"], "test1");
    assert_eq!(span(test), (6, 5, 11, 5));
    // The query, positioned in a source named after the mapping.
    let query = &test["query"];
    assert_eq!(query["_type"], "lambda");
    assert_eq!(span(query), (8, 14, 8, 25));
    let mut ids = Vec::new();
    source_ids(query, &mut ids);
    assert!(!ids.is_empty());
    assert!(ids.iter().all(|id| id == "my::M"), "{ids:?}");
    // The input's strings as written, joined; the mapping's own positions
    // are in the file.
    let input = &test["inputData"][0];
    assert_eq!(input["_type"], "object");
    assert_eq!(input["inputType"], "JSON");
    assert_eq!(input["sourceClass"], "my::S");
    assert_eq!(input["data"], "{\"a\":\\'1\\'}{}");
    assert_eq!(source_information(input).source_id, "m.pure");
    assert_eq!(span(input), (9, 14, 9, 56));
    // The expected output as written too.
    let assert = &test["assert"];
    assert_eq!(assert["_type"], "expectedOutputMappingTestAssert");
    assert_eq!(assert["expectedOutput"], "[{\"a\":\\'1\\'}]");
    assert_eq!(span(assert), (10, 7, 10, 30));
}

#[test]
fn a_legacy_test_is_checked_as_the_walker_checks_it() {
    let test = |fields: &str| {
        error(&format!(
            "Mapping my::M\n(\n  MappingTests\n  [\n    test1\n    (\n{fields}\n    )\n  ]\n)\n"
        ))
    };
    // A missing field is reported at the test.
    let error = test("      query: |1;\n      data: [];");
    assert_eq!(error.message(), "Field 'assert' is required");
    assert_eq!(position(&error), (6, 5, 10, 5));
    let error = test("      query: |1;\n      query: |2;\n      data: [];\n      assert: '';");
    assert_eq!(
        error.message(),
        "Field 'query' should be specified only once"
    );
    // An input's format is its type's to check.
    let error = test("      query: |1;\n      data: [<Object, my::S, '{}'>];\n      assert: '';");
    assert!(
        error.message().contains("'input type' is missing"),
        "{error}"
    );
    assert_eq!(position(&error), (9, 14, 9, 34));
    let error =
        test("      query: |1;\n      data: [<Object, CSV, my::S, '{}'>];\n      assert: '';");
    assert!(
        error.message().contains("does not support format 'CSV'"),
        "{error}"
    );
    assert_eq!(position(&error), (9, 23, 9, 25));
    // Only a string is expected.
    let error = test("      query: |1;\n      data: [];\n      assert: 'a' + 'b';");
    assert_eq!(
        error.message(),
        "Mapping test currently only support string assertion"
    );
    assert_eq!(position(&error), (10, 7, 10, 24));
}

#[test]
fn a_test_suite_reads_its_data_and_assertions() {
    let m = mapping(
        "Mapping my::M\n(\n  testSuites:\n  [\n    suite1:\n    {\n      doc: 'a\\tb';\n      function: |my::P.all();\n      tests:\n      [\n        test1:\n        {\n          doc: 'first';\n          data: [ Db: Reference #{ my::Data }#, my::Db: Reference #{ my::Data }# ];\n          asserts: [ 'check': EqualToJson #{ expected: ExternalFormat #{ contentType: 'application/json'; data: '[]'; }#; }# ];\n        }\n      ];\n    }\n  ]\n)\n",
    );
    let suite = &m["testSuites"][0];
    assert_eq!(suite["_type"], "mappingTestSuite");
    assert_eq!(suite["id"], "suite1");
    assert_eq!(suite["doc"], "a\tb");
    assert_eq!(span(suite), (6, 5, 19, 5));
    let mut ids = Vec::new();
    source_ids(&suite["func"], &mut ids);
    assert!(ids.iter().all(|id| id == "my::M"), "{ids:?}");
    let test = &suite["tests"][0];
    assert_eq!(test["_type"], "mappingTest");
    assert_eq!(test["id"], "test1");
    assert_eq!(test["doc"], "first");
    // A store without a package as written, one with one unquoted.
    let data = &test["storeTestData"];
    assert_eq!(data[0]["store"]["path"], "Db");
    assert_eq!(data[0]["store"]["type"], "STORE");
    assert_eq!(span(&data[0]["store"]), (15, 19, 15, 20));
    assert_eq!(span(&data[0]), (15, 19, 15, 46));
    assert_eq!(data[0]["data"]["_type"], "reference");
    assert_eq!(data[1]["store"]["path"], "my::Db");
    // The assertion, named without its quotes.
    let assertion = &test["assertions"][0];
    assert_eq!(assertion["_type"], "equalToJson");
    assert_eq!(assertion["id"], "check");
    assert_eq!(assertion["expected"]["_type"], "externalFormat");
    assert_eq!(assertion["expected"]["data"], "[]");
    assert_eq!(span(assertion), (16, 31, 16, 124));
}

#[test]
fn a_test_suite_is_checked_as_the_walker_checks_it() {
    let suite = |fields: &str| {
        error(&format!(
            "Mapping my::M\n(\n  testSuites:\n  [\n    suite1:\n    {{\n{fields}\n    }}\n  ]\n)\n"
        ))
    };
    let tests = "      tests: [ t: { data: []; asserts: [ a: EqualTo #{ expected: 1; }# ]; } ];";
    let error = suite(tests);
    assert_eq!(
        error.message(),
        "Mapping Test Suite requires a query function"
    );
    assert_eq!(position(&error), (6, 5, 9, 5));
    // A repeated `doc` is reported at the mapping, as the walker does.
    let error = suite(&format!(
        "      doc: 'a';\n      doc: 'b';\n      function: |1;\n{tests}"
    ));
    assert_eq!(error.message(), "Field 'doc' should be specified only once");
    assert_eq!(position(&error).0, 2);
    // A test needs assertions.
    let error = suite("      function: |1;\n      tests: [ t: { data: []; asserts: []; } ];");
    assert_eq!(error.message(), "Field 'asserts' is required");
    assert_eq!(position(&error), (9, 16, 9, 44));
}
