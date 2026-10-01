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

//! The `###DataQualityValidation` section, as the engine's
//! `DataQualityTreeWalker` reads it.

use legend_pure_dsl_dataquality as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The element of the section `###DataQualityValidation\n{body}`.
fn element(body: &str) -> Value {
    let source = format!("###DataQualityValidation\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "q.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

fn error(body: &str) -> ParseError {
    let source = format!("###DataQualityValidation\n{body}");
    legend_pure_parser_parser::parse(&source, "q.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    json!({
        "sourceId": "q.pure",
        "startLine": start_line,
        "startColumn": start_column,
        "endLine": end_line,
        "endColumn": end_column,
    })
}

fn position(error: &ParseError) -> (u32, u32, u32, u32) {
    let si = error.source_info().expect("positioned");
    (si.start_line, si.start_column, si.end_line, si.end_column)
}

/// A property of a validation tree, without sub-trees or constraints.
fn leaf(property: &str, at: Value) -> Value {
    json!({
        "_type": "dataQualityPropertyGraphFetchTree",
        "property": property,
        "constraints": [],
        "parameters": [],
        "subTrees": [],
        "subTypeTrees": [],
        "sourceInformation": at,
    })
}

#[test]
fn a_validation_checks_a_class_s_instances_through_a_tree() {
    let validation = element(
        "'''\nA doc\n'''\nDataQualityValidation <<meta::pure::profiles::doc.deprecated>> {doc.todo = 'x'} my::V\n{\n  context: fromMappingAndRuntime(my::M, my::R);\n  validationTree: $[\n    my::Person<mustBeAdult, 'quoted'>{\n      'nick': name<nonEmpty>,\n      firm(%latest, [1, -2])->subType(@my::Firm){\n        legalName\n      },\n      ->subType(@my::Other){ x },\n      address\n    }\n  ]$;\n  filter: $this.age > 0;\n  filter: |true;\n}\n",
    );
    assert_eq!(validation["_type"], "dataQualityValidation");
    assert_eq!(validation["package"], "my");
    assert_eq!(validation["name"], "V");
    // From the documentation, which becomes the first tagged value.
    assert_eq!(validation["sourceInformation"], si(2, 1, 20, 1));
    assert_eq!(validation["taggedValues"][0]["value"]["value"], "A doc");
    assert_eq!(validation["taggedValues"][1]["value"], "x");
    assert_eq!(validation["stereotypes"][0]["value"], "deprecated");
    // The context is unpositioned, its pointers positioned on their names.
    assert_eq!(
        validation["context"],
        json!({
            "_type": "mappingAndRuntimeDataQualityExecutionContext",
            "mapping": {"type": "MAPPING", "path": "my::M", "sourceInformation": si(7, 34, 7, 38)},
            "runtime": {"type": "RUNTIME", "path": "my::R", "sourceInformation": si(7, 41, 7, 45)},
        })
    );
    // The root is positioned on its class, its constraints unquoted; a
    // property on its name, its constraints as written, its alias without
    // quotes. Parameters and sub-type paths are left out.
    let mut firm = leaf("firm", si(11, 7, 11, 10));
    firm["subType"] = json!("my::Firm");
    firm["subTrees"] = json!([leaf("legalName", si(12, 9, 12, 17))]);
    let mut name = leaf("name", si(10, 15, 10, 18));
    name["alias"] = json!("nick");
    name["constraints"] = json!(["nonEmpty"]);
    assert_eq!(
        validation["dataQualityRootGraphFetchTree"],
        json!({
            "_type": "dataQualityRootGraphFetchTree",
            "class": "my::Person",
            "constraints": ["mustBeAdult", "quoted"],
            "subTrees": [name, firm, leaf("address", si(15, 7, 15, 13))],
            "subTypeTrees": [],
            "sourceInformation": si(9, 5, 9, 14),
        })
    );
    // The first filter, an expression that isn't a lambda made the body of
    // an unpositioned one.
    let filter = &validation["filter"];
    assert_eq!(filter["_type"], "lambda");
    assert_eq!(filter["body"][0]["function"], "greaterThan");
    assert!(filter.get("sourceInformation").is_none());
}

#[test]
fn a_data_space_context_names_its_execution_context() {
    let validation = element(
        "DataQualityValidation my::V\n{\n  context: fromDataSpace(my::Space, 'Main');\n  validationTree: $[ my::P{} ]$;\n}\n",
    );
    assert_eq!(
        validation["context"],
        json!({
            "_type": "dataSpaceDataQualityExecutionContext",
            "dataSpace": {"type": "DATASPACE", "path": "my::Space", "sourceInformation": si(4, 26, 4, 34)},
            "context": "Main",
        })
    );
    assert!(validation.get("filter").is_none());
}

#[test]
fn a_relation_validation_asserts_on_a_query_s_rows() {
    let validation = element(
        "DataQualityRelationValidation my::RV\n{\n  query: |my::Person.all()->size();\n  validations:\n  [\n    {\n      name: 'notEmpty';\n      description: 'Has rows';\n      assertion: rel|$rel->assertRelationNotEmpty();\n      type: AGGREGATE;\n    }\n  ];\n  testSuites:\n  [\n    suite1:\n    {\n      data:\n      [\n        my::Store: Reference #{ my::Data }#\n      ]\n      tests:\n      [\n        test1:\n        {\n          asserts:\n          [\n            assert1: EqualToJson #{ expected: ExternalFormat #{ contentType: 'application/json'; data: '[]'; }#; }#\n          ]\n        }\n      ]\n    }\n  ]\n}\n",
    );
    assert_eq!(validation["_type"], "dataqualityRelationValidation");
    assert_eq!(validation["sourceInformation"], si(2, 1, 34, 1));
    assert_eq!(validation["query"]["sourceInformation"], si(4, 10, 4, 34));
    // A validation is unpositioned.
    let rule = &validation["validations"][0];
    assert_eq!(rule["name"], "notEmpty");
    assert_eq!(rule["description"], "Has rows");
    assert_eq!(rule["type"], "AGGREGATE");
    assert_eq!(rule["assertion"]["_type"], "lambda");
    assert!(rule.get("sourceInformation").is_none());
    // Test suites: each store's data, and the tests' assertions.
    let suite = &validation["testSuites"][0];
    assert_eq!(suite["_type"], "dataQualityRelationValidationTestSuite");
    assert_eq!(suite["id"], "suite1");
    assert_eq!(suite["sourceInformation"], si(16, 5, 32, 5));
    assert_eq!(suite["testData"]["sourceInformation"], si(18, 7, 21, 7));
    let data = &suite["testData"]["testData"][0];
    assert_eq!(
        data["packageableElementPointer"],
        json!({"type": "STORE", "path": "my::Store", "sourceInformation": si(20, 9, 20, 17)})
    );
    assert_eq!(data["data"]["_type"], "reference");
    assert_eq!(data["sourceInformation"], si(20, 9, 20, 43));
    let test = &suite["tests"][0];
    assert_eq!(test["_type"], "dataQualityRelationValidationTest");
    assert_eq!(test["id"], "test1");
    assert_eq!(test["sourceInformation"], si(24, 9, 30, 9));
    assert_eq!(test["assertions"][0]["_type"], "equalToJson");
    assert_eq!(test["assertions"][0]["id"], "assert1");
}

#[test]
fn a_relation_comparison_compares_two_queries() {
    let comparison = element(
        "DataQualityRelationComparison my::C\n{\n  source: |my::A.all();\n  target: |my::B.all();\n  keys: [id, 'quoted key'];\n  columnsToCompare: [];\n  strategy: MD5Hash\n  {\n    sourceHashColumn: s;\n    aggregatedHash: false;\n  };\n  expectedMatch: 0.5;\n  testSuites: [ s1: { tests: [ t1: { asserts: [ a1: EqualToJson #{ expected: ExternalFormat #{ contentType: 'application/json'; data: '[]'; }#; }# ] } ] } ]\n}\n",
    );
    assert_eq!(comparison["_type"], "dataQualityRelationComparison");
    assert_eq!(comparison["source"]["sourceInformation"], si(4, 11, 4, 22));
    assert_eq!(comparison["target"]["sourceInformation"], si(5, 11, 5, 22));
    assert_eq!(comparison["keys"], json!(["id", "quoted key"]));
    assert_eq!(comparison["columnsToCompare"], json!([]));
    assert!(comparison.get("additionalColumnsToPersist").is_none());
    assert_eq!(
        comparison["strategy"],
        json!({"_type": "md5Hash", "sourceHashColumn": "s", "aggregatedHash": false})
    );
    assert_eq!(comparison["expectedMatch"], json!(0.5));
    assert_eq!(
        comparison["testSuites"][0]["_type"],
        "dataQualityRelationComparisonTestSuite"
    );
    assert_eq!(
        comparison["testSuites"][0]["tests"][0]["_type"],
        "dataQualityRelationComparisonTest"
    );
    // A comparison has no annotations, in its grammar or its protocol.
    assert!(comparison.get("stereotypes").is_none());
    assert!(comparison.get("taggedValues").is_none());
}

#[test]
fn the_walker_checks_fields_on_the_element() {
    let context = error("DataQualityValidation my::V\n{\n  validationTree: $[ my::P{} ]$;\n}\n");
    assert_eq!(context.message(), "Field 'context' is required");
    assert_eq!(position(&context), (2, 1, 5, 1));
    // A validation's own fields too.
    let name = error(
        "DataQualityRelationValidation my::RV\n{\n  query: |1;\n  validations: [ { assertion: |true; } ];\n}\n",
    );
    assert_eq!(name.message(), "Field 'name' is required");
    assert_eq!(position(&name), (2, 1, 6, 1));
    // A suite's on the suite.
    let tests = error(
        "DataQualityRelationValidation my::RV\n{\n  query: |1;\n  validations: [ { name: 'v'; assertion: |true; } ];\n  testSuites: [ s: { data: [] } ]\n}\n",
    );
    assert_eq!(tests.message(), "Field 'tests' is required");
    assert_eq!(position(&tests), (6, 17, 6, 31));
}

#[test]
fn the_grammar_s_tokens_are_written_whole() {
    // `$[` and `->subType(@` are each one token of the engine's lexer.
    let graph_start = error(
        "DataQualityValidation my::V\n{\n  context: fromDataSpace(my::S, 'ctx');\n  validationTree: $ [ my::P{} ]$;\n}\n",
    );
    assert_eq!(position(&graph_start), (5, 19, 5, 19));
    let sub_type = error(
        "DataQualityValidation my::V\n{\n  context: fromDataSpace(my::S, 'ctx');\n  validationTree: $[ my::P{ a-> subType(@my::Q) } ]$;\n}\n",
    );
    assert_eq!(position(&sub_type), (5, 30, 5, 31));
    // A strategy's braces hold a field; a validation's type is one of two.
    let strategy = error(
        "DataQualityRelationComparison my::C\n{\n  source: |1;\n  target: |2;\n  strategy: MD5Hash { };\n}\n",
    );
    assert_eq!(position(&strategy), (6, 23, 6, 23));
    let validation_type = error(
        "DataQualityRelationValidation my::RV\n{\n  query: |1;\n  validations: [ { name: 'v'; assertion: |true; type: ROW; } ];\n}\n",
    );
    assert_eq!(position(&validation_type), (5, 55, 5, 57));
    // A comparison has no documentation.
    let documented = error("'''\ndoc\n'''\nDataQualityRelationComparison my::C\n{\n}\n");
    assert_eq!(position(&documented), (5, 1, 5, 29));
}

#[test]
fn a_persistence_strategy_is_unsupported() {
    // The engine reads one with an extension, which no grammar here is.
    let strategy = error(
        "DataQualityRelationComparison my::C\n{\n  source: |1;\n  target: |2;\n  strategy: MD5Hash;\n  persistenceStrategy: # Delta { table: x; }#;\n}\n",
    );
    assert!(
        matches!(strategy, ParseError::Unsupported { .. }),
        "{strategy:?}"
    );
    assert_eq!(
        strategy.message(),
        "no data quality persistence strategy grammar is linked for 'Delta'"
    );
    // One without a type is an error, on its opening.
    let untyped = error(
        "DataQualityRelationComparison my::C\n{\n  source: |1;\n  target: |2;\n  strategy: MD5Hash;\n  persistenceStrategy: #{ x }#;\n}\n",
    );
    assert_eq!(
        untyped.message(),
        "Missing persistence strategy type.  Expect '# TYPE_HERE { ... }#'"
    );
    assert_eq!(position(&untyped), (7, 24, 7, 25));
}
