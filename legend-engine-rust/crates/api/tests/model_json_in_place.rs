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

//! `parse_model_json_bytes` writes a model's elements in place, in one
//! buffer; `parse_model` keeps each element's JSON on its own. Every section
//! of the engine corpus, as a model, must give the same JSON both ways.

use legend_pure_dsl_authentication as _;
use legend_pure_dsl_bigquery_function as _;
use legend_pure_dsl_connection as _;
use legend_pure_dsl_data as _;
use legend_pure_dsl_data_space as _;
use legend_pure_dsl_dataquality as _;
use legend_pure_dsl_deephaven as _;
use legend_pure_dsl_diagram as _;
use legend_pure_dsl_elasticsearch as _;
use legend_pure_dsl_external_format as _;
use legend_pure_dsl_function_jar as _;
use legend_pure_dsl_generation as _;
use legend_pure_dsl_graph as _;
use legend_pure_dsl_hosted_service as _;
use legend_pure_dsl_mapping as _;
use legend_pure_dsl_memsql_function as _;
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
use legend_pure_dsl_text as _;
use legend_pure_parser_api::{ModelRequest, parse_model, parse_model_json_bytes};
use serde_json::{Value, json};
use std::path::Path;

#[test]
fn elements_written_in_place_are_parse_models() {
    let dir = Path::new(env!("CARGO_MANIFEST_DIR")).join("../../tests/engine-corpus/sections");
    let mut models = 0;
    for entry in std::fs::read_dir(&dir).expect("the engine corpus") {
        let path = entry.expect("an entry").path();
        if path.extension().is_none_or(|e| e != "pure") {
            continue;
        }
        let meta: Value = serde_json::from_str(
            &std::fs::read_to_string(path.with_extension("meta.json")).expect("its meta.json"),
        )
        .expect("JSON");
        let section_type = meta["sectionType"].as_str().expect("a section type");
        let code = format!(
            "###{section_type}\n{}",
            std::fs::read_to_string(&path).expect("its code")
        );
        let request = ModelRequest {
            code: &code,
            source_id: "model.pure",
            line_offset: 0,
            column_offset: 0,
        };
        let written: Value =
            serde_json::from_slice(&parse_model_json_bytes(&request)).expect("JSON");
        let expected = match parse_model(&request) {
            Ok(model) => json!({ "elements": model.elements }),
            Err(error) => {
                assert_eq!(
                    written["error"]["message"],
                    error.message,
                    "{}",
                    path.display()
                );
                continue;
            }
        };
        assert_eq!(written, expected, "{}", path.display());
        models += 1;
    }
    assert!(models > 3000, "only {models} models parsed");
}
