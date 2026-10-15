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

//! The `###DataSpace` section, as the engine's `DataSpaceParseTreeWalker`
//! reads it.

use legend_pure_dsl_data_space as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The data space of the section `###DataSpace\n{body}`.
fn data_space(body: &str) -> Value {
    let source = format!("###DataSpace\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "d.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

fn error(body: &str) -> ParseError {
    let source = format!("###DataSpace\n{body}");
    legend_pure_parser_parser::parse(&source, "d.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    serde_json::to_value(SourceInformation {
        source_id: "d.pure".into(),
        start_line,
        start_column,
        end_line,
        end_column,
    })
    .expect("a position serializes")
}

fn position(error: &ParseError) -> (u32, u32, u32, u32) {
    let si = error.source_info().expect("positioned");
    (si.start_line, si.start_column, si.end_line, si.end_column)
}

#[test]
fn a_data_space_names_its_execution_contexts_elements_and_diagrams() {
    let data_space = data_space(
        "DataSpace <<meta::pure::profiles::doc.deprecated>> {doc.doc = 'x'} my::DS\n{\n  executionContexts:\n  [\n    {\n      name: 'Main';\n      title: 'T';\n      mapping: my::M;\n      defaultRuntime: my::R;\n      testData: Reference #{ my::Data }#;\n    },\n    {\n      name: 'Other';\n      mappingProvider: my::P.key1, key2;\n      testData: DataspaceTestData #{ my::'Other DS' }#;\n    }\n  ];\n  defaultExecutionContext: 'Main';\n  title: 'People';\n  description: 'All of them';\n  elements: [my::Person, -my::Firm];\n  diagrams:\n  [\n    {\n      title: 'D';\n      diagram: my::Diagram;\n    }\n  ];\n  featuredDiagrams: [my::Old];\n}\n",
    );
    assert_eq!(data_space["_type"], "dataSpace");
    assert_eq!(data_space["package"], "my");
    assert_eq!(data_space["name"], "DS");
    assert_eq!(data_space["sourceInformation"], si(2, 1, 31, 1));
    assert_eq!(data_space["stereotypes"][0]["value"], "deprecated");
    assert_eq!(data_space["taggedValues"][0]["value"], "x");
    assert_eq!(data_space["defaultExecutionContext"], "Main");
    assert_eq!(data_space["title"], "People");
    assert_eq!(data_space["description"], "All of them");
    // Pointers positioned on their fields, a reference on its embedded data.
    let main = &data_space["executionContexts"][0];
    assert_eq!(main["name"], "Main");
    assert_eq!(main["title"], "T");
    assert_eq!(main["sourceInformation"], si(6, 5, 12, 5));
    assert_eq!(
        main["mapping"],
        json!({"type": "MAPPING", "path": "my::M", "sourceInformation": si(9, 7, 9, 21)})
    );
    assert_eq!(
        main["defaultRuntime"],
        json!({"type": "RUNTIME", "path": "my::R", "sourceInformation": si(10, 7, 10, 28)})
    );
    assert_eq!(
        main["testData"],
        json!({
            "_type": "reference",
            "dataElement": {"type": "DATA", "path": "my::Data", "sourceInformation": si(11, 17, 11, 40)},
            "sourceInformation": si(11, 17, 11, 40),
        })
    );
    // A mapping provider and its keys; a data space's data as test data,
    // its quoted name unquoted.
    let other = &data_space["executionContexts"][1];
    assert!(other.get("mapping").is_none());
    assert_eq!(
        other["mappingProvider"],
        json!({
            "element": {"path": "my::P", "sourceInformation": si(15, 24, 15, 28)},
            "keys": ["key1", "key2"],
            "sourceInformation": si(15, 7, 15, 40),
        })
    );
    assert_eq!(
        other["testData"],
        json!({
            "_type": "reference",
            "dataElement": {"type": "DATASPACE", "path": "my::Other DS", "sourceInformation": si(16, 17, 16, 54)},
            "sourceInformation": si(16, 17, 16, 54),
        })
    );
    // An element written with `-` is excluded.
    assert_eq!(
        data_space["elements"],
        json!([
            {"path": "my::Person", "sourceInformation": si(22, 14, 22, 23)},
            {"path": "my::Firm", "exclude": true, "sourceInformation": si(22, 26, 22, 34)},
        ])
    );
    // Featured diagrams join the diagrams, untitled, positioned on their path.
    assert_eq!(
        data_space["diagrams"],
        json!([
            {
                "title": "D",
                "diagram": {"path": "my::Diagram", "sourceInformation": si(27, 7, 27, 27)},
                "sourceInformation": si(25, 5, 28, 5),
            },
            {
                "title": "",
                "diagram": {"path": "my::Old", "sourceInformation": si(30, 22, 30, 28)},
                "sourceInformation": si(30, 22, 30, 28),
            },
        ])
    );
}

#[test]
fn without_fields_a_data_space_has_no_execution_contexts() {
    let data_space = data_space("DataSpace my::DS\n{\n}\n");
    assert_eq!(data_space["executionContexts"], json!([]));
    for field in [
        "title",
        "elements",
        "executables",
        "diagrams",
        "supportInfo",
    ] {
        assert!(data_space.get(field).is_none(), "{field}");
    }
}

#[test]
fn executables_are_templates_or_packageable_elements() {
    let data_space = data_space(
        "DataSpace my::DS\n{\n  executionContexts: [{ name: 'Main'; mapping: my::M; }];\n  executables:\n  [\n    {\n      id: q1;\n      title: 'Q';\n      query: |my::Person.all()->project([p|$p.name], ['name']);\n      executionContextKey: 'Main';\n    },\n    {\n      title: 'F';\n      executable: my::f(String[1]) : String[1];\n      sampleValues: Relation #{\n        name\n        Ann;\n      }#;\n    },\n    {\n      id: 2;\n      title: 'S';\n      executable: my::Service;\n    }\n  ];\n}\n",
    );
    let executables = &data_space["executables"];
    // A template: its query, a lambda.
    let template = &executables[0];
    assert_eq!(template["_type"], "dataSpaceTemplateExecutable");
    assert_eq!(template["id"], "q1");
    assert_eq!(template["title"], "Q");
    assert_eq!(template["executionContextKey"], "Main");
    assert_eq!(template["query"]["_type"], "lambda");
    assert_eq!(template["query"]["body"][0]["function"], "project");
    assert_eq!(template["sourceInformation"], si(7, 5, 12, 5));
    // A function, written as its identifier, without spaces, with sample
    // values: a relation.
    let function = &executables[1];
    assert_eq!(function["_type"], "dataSpacePackageableElementExecutable");
    assert!(function.get("id").is_none());
    assert_eq!(
        function["executable"],
        json!({"type": "FUNCTION", "path": "my::f(String[1]):String[1]", "sourceInformation": si(15, 7, 15, 47)})
    );
    assert_eq!(function["sampleValues"]["columns"], json!(["name"]));
    assert_eq!(
        function["sampleValues"]["rows"],
        json!([{"values": ["Ann"]}])
    );
    // Another element, without a type; an id is kept as written.
    let service = &executables[2];
    assert_eq!(service["id"], "2");
    assert_eq!(
        service["executable"],
        json!({"path": "my::Service", "sourceInformation": si(24, 7, 24, 30)})
    );
}

#[test]
fn support_is_an_email_a_combined_one_or_links() {
    let email = data_space(
        "DataSpace my::DS\n{\n  supportInfo: Email\n  {\n    address: 'a@b.c';\n  };\n}\n",
    );
    assert_eq!(
        email["supportInfo"],
        json!({"_type": "email", "address": "a@b.c", "sourceInformation": si(4, 16, 7, 3)})
    );
    let combined = data_space(
        "DataSpace my::DS\n{\n  supportInfo: Combined\n  {\n    emails: ['a@b.c', 'd@e.f'];\n    website: 'w';\n  };\n}\n",
    );
    assert_eq!(combined["supportInfo"]["_type"], "combined");
    assert_eq!(combined["supportInfo"]["emails"], json!(["a@b.c", "d@e.f"]));
    assert_eq!(combined["supportInfo"]["website"], "w");
    let full = data_space(
        "DataSpace my::DS\n{\n  supportInfo:\n  {\n    documentation: { label: 'Docs'; url: 'u'; };\n    emails: [{ title: 'Team'; address: 'a@b.c'; }];\n    expertise: [{ description: 'All'; expertIds: ['x']; }];\n  };\n  operationalMetadata:\n  {\n    coverageRegions: [EMEA, NAMR];\n    updateFrequency: DAILY;\n  };\n}\n",
    );
    let support = &full["supportInfo"];
    assert_eq!(support["_type"], "full");
    assert_eq!(support["documentation"]["label"], "Docs");
    assert_eq!(support["documentation"]["url"], "u");
    assert_eq!(support["emails"][0]["title"], "Team");
    assert_eq!(support["expertise"][0]["expertIds"], json!(["x"]));
    // Operational metadata, positioned on its field.
    assert_eq!(
        full["operationalMetadata"],
        json!({
            "coverageRegions": ["EMEA", "NAMR"],
            "updateFrequency": "DAILY",
            "sourceInformation": si(10, 3, 14, 4),
        })
    );
}

#[test]
fn regions_and_frequencies_are_the_engine_s() {
    let region =
        error("DataSpace my::DS\n{\n  operationalMetadata: { coverageRegions: [EU]; };\n}\n");
    assert_eq!(
        region.message(),
        "Unknown coverage region 'EU'. Valid values: [APAC, EMEA, LAMR, NAMR]"
    );
    assert_eq!(position(&region), (4, 44, 4, 45));
    let frequency =
        error("DataSpace my::DS\n{\n  operationalMetadata: { updateFrequency: HOURLY; };\n}\n");
    assert_eq!(
        frequency.message(),
        "Unknown update frequency 'HOURLY'. Valid values: [INTRADAY, DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, ON_DEMAND]"
    );
    assert_eq!(position(&frequency), (4, 43, 4, 48));
}

#[test]
fn an_execution_context_has_a_mapping_or_a_mapping_provider() {
    let neither = error("DataSpace my::DS\n{\n  executionContexts: [{ name: 'Main'; }];\n}\n");
    assert_eq!(
        neither.message(),
        "Data space execution context must define either 'mapping' or 'mappingProvider'"
    );
    assert_eq!(position(&neither), (4, 23, 4, 39));
    let both = error(
        "DataSpace my::DS\n{\n  executionContexts: [{ name: 'Main'; mapping: my::M; mappingProvider: my::P; }];\n}\n",
    );
    assert_eq!(
        both.message(),
        "Data space execution context cannot define both 'mapping' and 'mappingProvider'"
    );
    assert_eq!(position(&both), (4, 23, 4, 79));
}

#[test]
fn test_data_is_a_reference() {
    let model_store = error(
        "DataSpace my::DS\n{\n  executionContexts: [{ name: 'Main'; mapping: my::M; testData: ModelStore #{ my::A: [] }#; }];\n}\n",
    );
    assert_eq!(
        model_store.message(),
        "Test data within a DataSpace must be created as a DataElementReference type, not an EmbeddedData type."
    );
    assert_eq!(position(&model_store), (4, 55, 4, 91));
    // A data space's data needs its path; the error is on the embedded data.
    let blank = error(
        "DataSpace my::DS\n{\n  executionContexts: [{ name: 'Main'; mapping: my::M; testData: DataspaceTestData #{  }#; }];\n}\n",
    );
    assert_eq!(
        blank.message(),
        "Path should be provided for DataspaceDataElementReference"
    );
    assert_eq!(position(&blank), (4, 65, 4, 88));
}

#[test]
fn sample_values_are_a_relation() {
    let error = error(
        "DataSpace my::DS\n{\n  executables: [{ title: 'X'; executable: my::S; sampleValues: ExternalFormat #{ contentType: 'a'; data: 'b'; }#; }];\n}\n",
    );
    assert_eq!(
        error.message(),
        "Data space executable sampleValues must be a standalone Relation element (e.g. sampleValues: Relation #{ ... }#), got type 'ExternalFormat'"
    );
    assert_eq!(position(&error), (4, 64, 4, 112));
}

#[test]
fn the_walker_checks_fields_in_its_order_after_the_grammar() {
    let twice = error("DataSpace my::DS\n{\n  title: 'A';\n  title: 'B';\n}\n");
    assert_eq!(
        twice.message(),
        "Field 'title' should be specified only once"
    );
    assert_eq!(position(&twice), (2, 1, 6, 1));
    // A template's id comes first.
    let id = error("DataSpace my::DS\n{\n  executables: [{ query: |1; }];\n}\n");
    assert_eq!(id.message(), "Field 'id' is required");
    assert_eq!(position(&id), (4, 17, 4, 30));
}

#[test]
fn an_executable_with_neither_a_query_nor_an_executable_is_unsupported() {
    // The engine fails on it without a position, which a Rust error has: it
    // parses the section itself.
    let error = error("DataSpace my::DS\n{\n  executables: [{ title: 'X'; }];\n}\n");
    assert!(matches!(error, ParseError::Unsupported { .. }), "{error:?}");
}
