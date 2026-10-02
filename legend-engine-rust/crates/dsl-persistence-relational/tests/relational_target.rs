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

//! Relational persistence targets, as the engine's
//! `PersistenceRelationalParseTreeWalker` reads them.

use legend_pure_dsl_persistence_relational as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// A persistence whose one output targets `target`, from line 9 on.
fn source(target: &str) -> String {
    format!(
        "###Persistence\nPersistence my::P\n{{\n  doc: 'Doc';\n  trigger: Manual;\n  service: my::S;\n  serviceOutputTargets:\n  [\n    TDS {{ keys: [id] datasetType: Snapshot {{}} }}\n    ->\n{target}\n  ];\n}}\n"
    )
}

fn target(target: &str) -> Value {
    let file = legend_pure_parser_parser::parse(&source(target), "p.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")["serviceOutputTargets"][0]
        ["persistenceTarget"]
        .clone()
}

fn error(target: &str) -> ParseError {
    legend_pure_parser_parser::parse(&source(target), "p.pure")
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

#[test]
fn a_target_names_its_table_database_and_temporality() {
    let target = target(
        "    Relational\n    #{\n      table: db.people;\n      database: my::Db;\n      temporality: Unitemporal\n      {\n        processingDimension: DateTime\n        {\n          dateTimeIn: IN_Z;\n          dateTimeOut: OUT_Z;\n        }\n      }\n    }#",
    );
    assert_eq!(
        target,
        json!({
            "_type": "relationalPersistenceTarget",
            "table": "db.people",
            "database": {"type": "STORE", "path": "my::Db", "sourceInformation": si(14, 7, 14, 23)},
            "temporality": {
                "_type": "unitemporalTemporality",
                "processingDimension": {
                    "_type": "processingTime",
                    "timeIn": "IN_Z",
                    "timeOut": "OUT_Z",
                    "sourceInformation": si(17, 30, 21, 9),
                },
                "sourceInformation": si(15, 20, 22, 7),
            },
            // From its first token to the end of the body the walker parses,
            // two characters short of `}#`, whose `<EOF>` ANTLR counts.
            "sourceInformation": si(13, 7, 23, 7),
        })
    );
}

#[test]
fn the_walker_parses_the_body_two_characters_short() {
    // Without a temporality, the target is non-temporal.
    let target = target("    Relational #{ table: t; database: my::Db;   }#");
    assert_eq!(target["temporality"], json!({"_type": "none"}));
    // With only one space before `}#`, the walker cuts the last `;` off.
    let short = error("    Relational #{ table: t; database: my::Db; }#");
    assert_eq!(short.message(), "Expected ';', found end of file");
    let empty = error("    Relational #{  }#");
    assert_eq!(
        empty.message(),
        "Persistence target 'Relational' must have a non-empty body"
    );
}
