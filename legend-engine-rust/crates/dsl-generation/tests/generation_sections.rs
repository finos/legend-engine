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

//! The `###FileGeneration` and `###GenerationSpecification` sections, as
//! the engine's walkers read them.

use legend_pure_dsl_generation as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The first element of `source`.
fn element(source: &str) -> Value {
    let file = legend_pure_parser_parser::parse(source, "g.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

fn error(source: &str) -> ParseError {
    legend_pure_parser_parser::parse(source, "g.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    serde_json::to_value(SourceInformation {
        source_id: "g.pure".into(),
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
fn a_file_generation_configures_its_generator() {
    let generation = element(
        "###FileGeneration\nimport my::*;\nAvro my::Config\n{\n  scopeElements: ['a', my::B];\n  generationOutputPath: 'out';\n  options.level: 3;\n  flags: [];\n  sizes: [1, 2];\n  map: { a.b: 'x'; c: true; c: false; };\n}\n",
    );
    assert_eq!(generation["_type"], "fileGeneration");
    // Its type, the generator's, with a lowercase first letter.
    assert_eq!(generation["type"], "avro");
    assert_eq!(generation["typeSourceInformation"], si(3, 1, 3, 4));
    // A list of names is the scope's, as the grammar takes it first.
    assert_eq!(generation["scopeElements"], json!(["a", "my::B"]));
    assert_eq!(generation["generationOutputPath"], "out");
    // Properties by name as written, their values typed; in a map, a name
    // written again takes the later value.
    assert_eq!(
        generation["configurationProperties"],
        json!([
            {"name": "options.level", "value": 3, "sourceInformation": si(7, 3, 7, 19)},
            {"name": "flags", "value": [], "sourceInformation": si(8, 3, 8, 12)},
            {"name": "sizes", "value": [1, 2], "sourceInformation": si(9, 3, 9, 16)},
            {"name": "map", "value": {"a.b": "x", "c": false}, "sourceInformation": si(10, 3, 10, 40)},
        ])
    );
}

#[test]
fn a_file_generation_s_own_fields_are_its_own() {
    // A property of a reserved name: the grammar took it as one.
    let reserved = error("###FileGeneration\nAvro my::Config\n{\n  scopeElements: [1];\n}\n");
    assert_eq!(
        reserved.message(),
        "Can't have config property with reserved name 'scopeElements'"
    );
    assert_eq!(position(&reserved), (2, 1, 5, 1));
    let twice = error(
        "###FileGeneration\nAvro my::Config\n{\n  generationOutputPath: 'a';\n  generationOutputPath: 'b';\n}\n",
    );
    assert_eq!(
        twice.message(),
        "Field 'generationOutputPath' should be specified only once"
    );
    // A number too large for a Java long makes the engine fail.
    let big = error("###FileGeneration\nAvro my::Config\n{\n  big: 99999999999999999999;\n}\n");
    assert!(matches!(big, ParseError::Unsupported { .. }), "{big:?}");
}

#[test]
fn a_generation_specification_names_what_to_generate() {
    let specification = element(
        "###GenerationSpecification\nGenerationSpecification my::G\n{\n  generationNodes: [ { generationElement: my::E; }, { id: 'n'; generationElement: my::F; } ];\n  fileGenerations: [ my::Config ];\n}\n",
    );
    assert_eq!(specification["_type"], "generationSpecification");
    // A node's id defaults to its element's path.
    assert_eq!(
        specification["generationNodes"],
        json!([
            {"generationElement": "my::E", "id": "my::E", "sourceInformation": si(4, 22, 4, 50)},
            {"generationElement": "my::F", "id": "n", "sourceInformation": si(4, 53, 4, 90)},
        ])
    );
    assert_eq!(
        specification["fileGenerations"],
        json!([{"type": "FILE_GENERATION", "path": "my::Config", "sourceInformation": si(5, 22, 5, 31)}])
    );
    let twice = error(
        "###GenerationSpecification\nGenerationSpecification my::G\n{\n  fileGenerations: [];\n  fileGenerations: [];\n}\n",
    );
    assert_eq!(
        twice.message(),
        "Field 'fileGenerations' should be specified only once"
    );
    assert_eq!(position(&twice), (2, 1, 6, 1));
}
