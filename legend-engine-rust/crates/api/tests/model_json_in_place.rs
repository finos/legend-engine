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
//! buffer; `parse_model` keeps each element's JSON on its own. Every sample
//! model, and a model of each linked grammar, must give the same JSON both
//! ways.

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

/// A model of each linked section grammar and island.
const LINKED: &[&str] = &[
    "Class a::Person\n{\n  name: String[1];\n}\nfunction a::graph(): Any[*]\n{\n  #{ a::Person { name } }#\n}\nfunction a::store(): Any[*]\n{\n  #>{ a::Db.people }#\n}\nfunction a::sql(): Any[*]\n{\n  #SQL{select * from t}#\n}\nfunction a::tds(): Any[*]\n{\n  #TDS{\n    id, name:String[1]\n    1, A\n  }#\n}\n",
    "###Data\nData a::D\n{\n  ExternalFormat\n  #{\n    contentType: 'application/json';\n    data: '{}';\n  }#\n}\n",
    "###Mapping\nMapping a::M\n(\n  a::Person: Pure\n  {\n    ~src a::Person\n    name: $src.name\n  }\n)\n",
    "###Connection\nJsonModelConnection a::C\n{\n  class: a::Person;\n  url: 'data:application/json,{}';\n}\n",
    "###Runtime\nRuntime a::R\n{\n  mappings: [a::M];\n  connections:\n  [\n    ModelStore: [c: a::C]\n  ];\n}\n",
    "###Relational\nDatabase a::Db\n(\n  Table people (id INTEGER PRIMARY KEY, name VARCHAR(200))\n)\n\n###Mapping\nMapping a::RM\n(\n  a::Person: Relational\n  {\n    name: [a::Db]people.name\n  }\n)\n\n###Connection\nRelationalDatabaseConnection a::RC\n{\n  store: a::Db;\n  type: H2;\n  specification: LocalH2 {};\n  auth: DefaultH2;\n}\n",
    "###Service\nService a::S\n{\n  pattern: '/people';\n  documentation: '';\n  autoActivateUpdates: true;\n  execution: Single\n  {\n    query: |a::Person.all()->graphFetch(#{ a::Person { name } }#)->serialize(#{ a::Person { name } }#);\n    mapping: a::M;\n    runtime: a::R;\n  }\n}\n",
    "###ServiceStore\nServiceStore a::SS\n(\n  Service s\n  (\n    path : '/s';\n    method : GET;\n    response : a::Person <- a::B;\n    security : [];\n  )\n)\n",
    "###MongoDB\nDatabase a::Mdb\n(\n  Collection c ( validationLevel: moderate; jsonSchema: { \"bsonType\": \"object\" }; )\n)\n\n###Connection\nMongoDBConnection a::MC\n{\n  store: a::Mdb;\n  database: legend-db;\n  serverURLs: [localhost:1];\n  authentication: # UserPassword {\n    username: 'u';\n    password: SystemPropertiesSecret\n    {\n      systemPropertyName: 'p';\n    };\n  }#;\n}\n",
    "###Elasticsearch\nElasticsearch7Cluster a::E\n{\n  indices: [\n    people: {\n      properties: [\n        name: Keyword {}\n      ];\n    }\n  ];\n}\n",
    "###Snowflake\nSnowflakeApp a::A\n{\n  applicationName: 'app';\n  function: a::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n}\n",
    "###MemSql\nMemSqlFunction a::MF\n{\n  functionName: 'f';\n  function: a::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n}\n",
    "###BigQuery\nBigQueryFunction a::BF\n{\n  functionName: 'f';\n  function: a::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n}\n",
    "###FunctionJar\nFunctionJar a::J\n{\n  function: a::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n}\n",
    "###HostedService\nHostedService a::HS\n{\n  pattern: '/p';\n  function: a::f():String[1];\n  ownership: Deployment { identifier: 'o' };\n}\n",
    "###Deephaven\nDeephaven a::DS\n(\n  Table trades ( id: INT, at: TIMESTAMP )\n)\n",
    "###Persistence\nPersistence a::P\n{\n  doc: 'Doc';\n  trigger: Manual;\n  service: a::S;\n  serviceOutputTargets:\n  [\n    TDS { keys: [id] datasetType: Snapshot {} }\n    ->\n    Relational\n    #{\n      table: db.people;\n      database: a::Db;\n      temporality: Unitemporal\n      {\n        processingDimension: DateTime\n        {\n          dateTimeIn: IN_Z;\n          dateTimeOut: OUT_Z;\n        }\n      }\n    }#\n  ];\n}\n\nPersistenceContext a::C\n{\n  persistence: a::P;\n  platform: AwsGlue\n  #{\n    dataProcessingUnits: 10;\n  }#;\n}\n",
    "###DataSpace\nDataSpace a::DS\n{\n  executionContexts:\n  [\n    {\n      name: 'Default';\n      mapping: a::M;\n      defaultRuntime: a::R;\n    }\n  ];\n  defaultExecutionContext: 'Default';\n}\n",
    "###DataQualityValidation\nDataQualityValidation a::V\n{\n  context: fromMappingAndRuntime(a::M, a::R);\n  validationTree: $[ a::Person{ name } ]$;\n}\n",
    "###Diagram\nDiagram a::D\n{\n  classView a\n  {\n    class: a::Person;\n    position: (0.0,0.0);\n    rectangle: (100.0,50.0);\n  }\n}\n",
    "###ExternalFormat\nBinding a::B\n{\n  contentType: 'application/json';\n  modelIncludes: [\n    a::Person\n  ];\n}\n",
    "###FileGeneration\nJSONSchema a::G\n{\n  scopeElements: [a::Person];\n}\n\n###GenerationSpecification\nGenerationSpecification a::GS\n{\n  fileGenerations: [a::G];\n}\n",
    "###Text\nText a::T\n{\n  type: HTML;\n  content: 'x';\n}\n",
];

fn request(code: &str) -> ModelRequest<'_> {
    ModelRequest {
        code,
        source_id: "model.pure",
        line_offset: 0,
        column_offset: 0,
    }
}

/// The JSON `parse_model_json_bytes` writes, and the error `parse_model`
/// reports, if any.
fn both_ways(code: &str) -> (Value, Result<Value, String>) {
    let written: Value =
        serde_json::from_slice(&parse_model_json_bytes(&request(code))).expect("JSON");
    let kept = parse_model(&request(code))
        .map(|model| json!({ "elements": model.elements }))
        .map_err(|error| error.message);
    (written, kept)
}

#[test]
fn elements_written_in_place_are_parse_models() {
    let mut samples = 0;
    for dir in ["../parser/tests/corpus", "../protocol/tests/corpus"] {
        let dir = Path::new(env!("CARGO_MANIFEST_DIR")).join(dir);
        for entry in std::fs::read_dir(&dir).expect("the sample models") {
            let path = entry.expect("an entry").path();
            if path.extension().is_none_or(|e| e != "pure") {
                continue;
            }
            let (written, kept) = both_ways(&std::fs::read_to_string(&path).expect("its code"));
            match kept {
                Ok(expected) => assert_eq!(written, expected, "{}", path.display()),
                Err(message) => {
                    assert_eq!(written["error"]["message"], message, "{}", path.display());
                }
            }
            samples += 1;
        }
    }
    assert!(samples > 0, "the sample models are missing");
    for code in LINKED {
        let (written, kept) = both_ways(code);
        assert_eq!(written, kept.expect("a linked grammar parses"), "{code}");
    }
}
