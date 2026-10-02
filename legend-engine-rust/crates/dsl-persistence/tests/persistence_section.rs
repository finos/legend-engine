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

//! The `###Persistence` section, as the engine's
//! `PersistenceParseTreeWalker` and `PersistenceContextParseTreeWalker` read
//! it.

use legend_pure_dsl_persistence as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The elements of the section `###Persistence\n{body}`.
fn elements(body: &str) -> Vec<Value> {
    let source = format!("###Persistence\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "p.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|element| serde_json::to_value(element).expect("serializes"))
        .collect()
}

fn error(body: &str) -> ParseError {
    let source = format!("###Persistence\n{body}");
    legend_pure_parser_parser::parse(&source, "p.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    serde_json::to_value(SourceInformation {
        source_id: "p.pure".into(),
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

/// A persistence's first fields, lines 4 to 6.
const HEADER: &str = "Persistence my::P\n{\n  doc: 'Doc';\n  trigger: Manual;\n  service: my::S;";

#[test]
fn a_persistence_persists_a_service_s_result_with_a_persister() {
    let persistence = &elements(&format!(
        "{HEADER}\n  persister: Batch\n  {{\n    sink: Relational\n    {{\n      database: my::Db;\n    }}\n    ingestMode: AppendOnly\n    {{\n      auditing: None;\n      filterDuplicates: true;\n    }}\n    targetShape: Flat\n    {{\n      targetName: 'T';\n      modelClass: my::C;\n    }}\n  }}\n}}\n"
    ))[0];
    assert_eq!(persistence["_type"], "persistence");
    assert_eq!(persistence["package"], "my");
    assert_eq!(persistence["name"], "P");
    assert_eq!(persistence["documentation"], "Doc");
    // A manual trigger, unpositioned.
    assert_eq!(persistence["trigger"], json!({"_type": "manualTrigger"}));
    // The service, positioned on its field.
    assert_eq!(
        persistence["service"],
        json!({"type": "SERVICE", "path": "my::S", "sourceInformation": si(6, 3, 6, 17)})
    );
    // Without the fields, no outputs and no one to notify.
    assert_eq!(persistence["serviceOutputTargets"], json!([]));
    assert_eq!(persistence["notifier"], json!({"notifyees": []}));
    assert!(persistence.get("tests").is_none());
    assert_eq!(persistence["sourceInformation"], si(2, 1, 24, 1));
    let persister = &persistence["persister"];
    assert_eq!(persister["_type"], "batchPersister");
    assert_eq!(persister["sourceInformation"], si(7, 14, 23, 3));
    assert_eq!(
        persister["sink"],
        json!({
            "_type": "relationalSink",
            "database": {"type": "STORE", "path": "my::Db", "sourceInformation": si(11, 7, 11, 23)},
            "sourceInformation": si(9, 11, 12, 5),
        })
    );
    assert_eq!(
        persister["ingestMode"],
        json!({
            "_type": "appendOnly",
            "auditing": {"_type": "noAuditing"},
            "filterDuplicates": true,
            "sourceInformation": si(13, 17, 17, 5),
        })
    );
    assert_eq!(
        persister["targetShape"],
        json!({
            "_type": "flatTarget",
            "modelClass": "my::C",
            "targetName": "T",
            "partitionFields": [],
            "deduplicationStrategy": {"_type": "noDeduplicationStrategy"},
            "sourceInformation": si(18, 18, 22, 5),
        })
    );
}

#[test]
fn service_outputs_are_a_tds_s_or_a_graph_fetch_s() {
    let targets = &elements(&format!(
        "{HEADER}\n  serviceOutputTargets:\n  [\n    TDS\n    {{\n      keys: [id, 'na me']\n      deduplication: None;\n      datasetType: Delta {{ actionIndicator: None {{}} }}\n    }}\n    ->\n    {{\n    }},\n    #/my::T/p#\n    {{\n      keys: [#/my::T/p/id#]\n      datasetType: Snapshot {{ partitioning: FieldBased {{ partitionFields: [#/my::T/p/d#]; }} }}\n    }}\n    ->\n    {{\n    }}\n  ];\n}}\n"
    ))[0]["serviceOutputTargets"];
    // `{}` is no target.
    assert!(targets[0].get("persistenceTarget").is_none());
    assert_eq!(targets[0]["sourceInformation"], si(9, 5, 17, 5));
    let tds = &targets[0]["serviceOutput"];
    assert_eq!(tds["_type"], "tdsServiceOutput");
    assert_eq!(tds["keys"], json!(["id", "na me"]));
    assert_eq!(
        tds["deduplication"],
        json!({"_type": "noDeduplication", "sourceInformation": si(12, 22, 12, 26)})
    );
    assert_eq!(
        tds["datasetType"],
        json!({
            "_type": "delta",
            "actionIndicator": {"_type": "noActionIndicator", "sourceInformation": si(13, 45, 13, 51)},
            "sourceInformation": si(13, 20, 13, 53),
        })
    );
    let graph_fetch = &targets[1]["serviceOutput"];
    assert_eq!(graph_fetch["_type"], "graphFetchServiceOutput");
    assert_eq!(graph_fetch["path"]["startType"], "my::T");
    assert_eq!(graph_fetch["keys"][0]["path"][1]["property"], "id");
    let partitioning = &graph_fetch["datasetType"]["partitioning"];
    assert_eq!(partitioning["_type"], "fieldBasedForGraphFetch");
    assert_eq!(
        partitioning["partitionFieldPaths"][0]["path"][1]["property"],
        "d"
    );
}

#[test]
fn a_context_names_its_persistence_parameters_and_sink() {
    let context = &elements(
        "PersistenceContext my::C\n{\n  persistence: my::P;\n  serviceParameters: [a = 'x', b = [1,  2], c = my::Conn];\n  sinkConnection: my::Sink;\n}\n",
    )[0];
    assert_eq!(context["_type"], "persistenceContext");
    assert_eq!(
        context["persistence"],
        json!({"type": "PERSISTENCE", "path": "my::P", "sourceInformation": si(4, 3, 4, 21)})
    );
    // Without a platform, the default one, unpositioned.
    assert_eq!(context["platform"], json!({"_type": "default"}));
    let parameters = &context["serviceParameters"];
    assert_eq!(
        parameters[0],
        json!({
            "name": "a",
            "value": {
                "_type": "primitiveTypeValue",
                "primitiveType": {"_type": "string", "value": "x", "sourceInformation": si(5, 27, 5, 29)},
            },
            "sourceInformation": si(5, 23, 5, 29),
        })
    );
    // The walker parses a value's tokens' texts joined, without the spaces
    // between them: `[1,2]`.
    let collection = &parameters[1]["value"]["primitiveType"];
    assert_eq!(collection["sourceInformation"], si(5, 36, 5, 40));
    assert_eq!(
        collection["values"][1]["sourceInformation"],
        si(5, 39, 5, 39)
    );
    assert_eq!(parameters[1]["sourceInformation"], si(5, 32, 5, 42));
    assert_eq!(
        parameters[2]["value"],
        json!({
            "_type": "connectionValue",
            "connection": {"_type": "connectionPointer", "connection": "my::Conn", "sourceInformation": si(5, 49, 5, 56)},
        })
    );
    assert_eq!(
        context["sinkConnection"],
        json!({"_type": "connectionPointer", "connection": "my::Sink", "sourceInformation": si(6, 19, 6, 26)})
    );
}

#[test]
fn tests_run_batches_numbered_in_order() {
    let data = "data: { connection: { ExternalFormat #{ contentType: 'application/json'; data: '[]'; }# } }";
    let asserts = "asserts: [ a: EqualToJson #{ expected: ExternalFormat #{ contentType: 'application/json'; data: '[]'; }#; }# ]";
    let tests = &elements(&format!(
        "{HEADER}\n  tests:\n  [\n    t: {{ testBatches: [ b0: {{ {data} {asserts} }}, b1: {{ {data} {asserts} }} ] isTestDataFromServiceOutput: false; }},\n    u: {{ testBatches: [] }}\n  ]\n}}\n"
    ))[0]["tests"];
    assert_eq!(tests[0]["_type"], "test");
    assert_eq!(tests[0]["isTestDataFromServiceOutput"], false);
    let batches = &tests[0]["testBatches"];
    assert_eq!(batches[0]["id"], "b0");
    assert_eq!(batches[1]["batchId"], 1);
    assert_eq!(
        batches[1]["testData"]["connection"]["data"]["_type"],
        "externalFormat"
    );
    assert_eq!(batches[1]["assertions"][0]["_type"], "equalToJson");
    assert_eq!(batches[1]["assertions"][0]["id"], "a");
    // Without the field, the test's data is the service's output.
    assert_eq!(tests[1]["isTestDataFromServiceOutput"], true);
}

#[test]
fn the_walker_checks_fields_in_its_order_after_the_grammar() {
    let no_doc = error("Persistence my::P\n{\n  trigger: Manual;\n  service: my::S;\n}\n");
    assert_eq!(no_doc.message(), "Field 'doc' is required");
    assert_eq!(position(&no_doc), (2, 1, 6, 1));
    // A syntax error anywhere comes first.
    let syntax = error(
        "Persistence my::P\n{\n  trigger: Manual;\n}\nPersistence my::Q\n{\n  doc: 'd' 'e';\n}\n",
    );
    assert_eq!(position(&syntax), (8, 12, 8, 14));
    // A test batch's data holds a connection's, always.
    let empty_data = error(&format!(
        "{HEADER}\n  tests: [ t: {{ testBatches: [ b: {{ data: {{ }} }} ] }} ]\n}}\n"
    ));
    assert_eq!(position(&empty_data), (7, 45, 7, 45));
    let no_data = error(&format!(
        "{HEADER}\n  tests: [ t: {{ testBatches: [ b: {{ asserts: [] }} ] }} ]\n}}\n"
    ));
    assert_eq!(
        no_data.message(),
        "TestData cannot be empty or null within Persistence TestBatch"
    );
    assert_eq!(position(&no_data), (7, 32, 7, 49));
}

#[test]
fn what_another_engine_module_reads_is_unsupported() {
    for trigger in ["Cron", "Custom #{ every: day; }#"] {
        let unsupported = error(&format!(
            "Persistence my::P\n{{\n  doc: 'd';\n  trigger: {trigger};\n  service: my::S;\n}}\n"
        ));
        assert!(
            matches!(unsupported, ParseError::Unsupported { .. }),
            "{unsupported}"
        );
    }
    let platform = error(
        "PersistenceContext my::C\n{\n  persistence: my::P;\n  platform: AwsGlue #{ dataProcessingUnits: 1; }#;\n}\n",
    );
    assert!(
        matches!(platform, ParseError::Unsupported { .. }),
        "{platform}"
    );
    // The engine fails without a position on a graph fetch's field written
    // as a name.
    let name_as_path = error(&format!(
        "{HEADER}\n  serviceOutputTargets: [ #/my::T/p# {{ keys: [id] datasetType: Snapshot {{}} }} -> {{}} ];\n}}\n"
    ));
    assert!(
        matches!(name_as_path, ParseError::Unsupported { .. }),
        "{name_as_path}"
    );
}
