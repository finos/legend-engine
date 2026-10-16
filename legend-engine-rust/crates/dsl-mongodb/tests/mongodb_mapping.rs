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

//! MongoDB class mappings, as the engine's `MongoDBMappingParseTreeWalker`
//! reads them. Linking this crate registers the grammar into `dsl-mapping`'s.

use legend_pure_dsl_mapping as _;
use legend_pure_dsl_mongodb as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The first class mapping of `###Mapping\nMapping my::M\n(\n{body}\n)\n`.
fn class_mapping(body: &str) -> Value {
    let source = format!("###Mapping\nMapping my::M\n(\n{body}\n)\n");
    let file = legend_pure_parser_parser::parse(&source, "m.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")["classMappings"][0].clone()
}

fn error_start(body: &str) -> (u32, u32) {
    let source = format!("###Mapping\nMapping my::M\n(\n{body}\n)\n");
    let error: ParseError = legend_pure_parser_parser::parse(&source, "m.pure")
        .expect_err("fails")
        .errors
        .remove(0);
    let si = error.source_info().expect("positioned");
    (si.start_line, si.start_column)
}

#[test]
fn a_class_mapping_names_its_collection_and_binding() {
    // The filter read and ignored; the collection's name unquoted, the
    // store and the binding as written; no source information.
    let mapping = class_mapping(
        "  *my::Person[p]: MongoDB\n  {\n    ~filter [my::Db] active\n    ~mainCollection [my::Db] 'person-records'.a.b\n    ~binding my::'Binding'\n  }",
    );
    assert_eq!(
        mapping,
        json!({
            "_type": "MongoDB",
            "class": "my::Person",
            "id": "p",
            "root": true,
            "mainCollectionName": "person-records",
            "storePath": "my::Db",
            "bindingPath": "my::'Binding'",
        })
    );
}

#[test]
fn a_collection_or_binding_is_kept_when_written_once() {
    let mapping = class_mapping(
        "  my::Person: MongoDB\n  {\n    ~mainCollection [my::Db] a\n    ~binding my::B\n    ~mainCollection [my::Db] b\n  }",
    );
    assert_eq!(
        mapping,
        json!({
            "_type": "MongoDB",
            "class": "my::Person",
            "root": false,
            "bindingPath": "my::B",
        })
    );
    let empty = class_mapping("  my::Person: MongoDB\n  {\n  }");
    assert_eq!(
        empty,
        json!({"_type": "MongoDB", "class": "my::Person", "root": false})
    );
}

#[test]
fn the_filter_comes_first_and_a_scope_has_two_names() {
    assert_eq!(
        error_start(
            "  my::Person: MongoDB\n  {\n    ~mainCollection [my::Db] a\n    ~filter [my::Db] f\n  }"
        ),
        (7, 5)
    );
    // The third `.`.
    assert_eq!(
        error_start("  my::Person: MongoDB\n  {\n    ~mainCollection [my::Db] a.b.c.d\n  }"),
        (6, 35)
    );
    // `~` apart from its command.
    assert_eq!(
        error_start("  my::Person: MongoDB\n  {\n    ~ binding my::B\n  }"),
        (6, 5)
    );
}
