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

//! AWS Glue persistence platforms, as the engine's
//! `PersistenceCloudParseTreeWalker` reads them.

use legend_pure_dsl_persistence_cloud as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::json;

fn source(platform: &str) -> String {
    format!(
        "###Persistence\nPersistenceContext my::C\n{{\n  persistence: my::P;\n  platform: {platform};\n}}\n"
    )
}

fn error(platform: &str) -> ParseError {
    legend_pure_parser_parser::parse(&source(platform), "p.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

#[test]
fn a_platform_counts_its_data_processing_units() {
    let file = legend_pure_parser_parser::parse(
        &source("AwsGlue\n  #{\n    dataProcessingUnits: 10;\n  }#"),
        "p.pure",
    )
    .expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    let context = serde_json::to_value(&pmcd.elements[0]).expect("serializes");
    assert_eq!(
        context["platform"],
        json!({
            "_type": "awsGlue",
            "dataProcessingUnits": 10,
            // To the end of the body the walker parses, two characters short
            // of `}#`, whose `<EOF>` ANTLR counts.
            "sourceInformation": {"sourceId": "p.pure", "startLine": 7, "startColumn": 5, "endLine": 8, "endColumn": 5},
        })
    );
}

#[test]
fn a_platform_needs_its_count() {
    assert_eq!(
        error("AwsGlue #{   }#").message(),
        "Field 'dataProcessingUnits' is required"
    );
    assert_eq!(
        error("AwsGlue").message(),
        "Persistence platform 'AwsGlue' must have a non-empty body"
    );
}
