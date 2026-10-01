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

//! Linking this crate registers the `###Data` section grammar with both the
//! parser (`SECTION_PARSERS`) and the protocol converter
//! (`ELEMENT_PROTOCOLS`), so plain `parse()` + `convert_source_file()`
//! produce the engine's JSON.

use legend_pure_dsl_data as _;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The elements `source` defines.
fn elements(source: &str) -> Vec<Value> {
    let file = legend_pure_parser_parser::parse(source, "data.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    pmcd.elements
        .iter()
        .map(|e| serde_json::to_value(e).expect("serializes"))
        .collect()
}

fn span(node: &Value) -> (u64, u64, u64, u64) {
    let si = &node["sourceInformation"];
    let at = |key: &str| {
        si[key]
            .as_u64()
            .unwrap_or_else(|| panic!("no {key} in {node}"))
    };
    (
        at("startLine"),
        at("startColumn"),
        at("endLine"),
        at("endColumn"),
    )
}

#[test]
fn a_data_element_holds_embedded_data() {
    let source = "###Data\nData <<my::P.s>> {my::P.t = 'v'} my::MyData\n{\n  ExternalFormat\n  #{\n    contentType: 'application/json';\n    data: '{}';\n  }#\n}\n";
    let element = &elements(source)[0];
    assert_eq!(element["_type"], "dataElement");
    assert_eq!(element["package"], "my");
    assert_eq!(element["name"], "MyData");
    assert_eq!(element["stereotypes"][0]["value"], "s");
    // On its value, as the engine's section walkers position a stereotype.
    assert_eq!(span(&element["stereotypes"][0]), (2, 14, 2, 14));
    assert_eq!(element["taggedValues"][0]["value"], "v");
    // From `Data` to `}`.
    assert_eq!(span(element), (2, 1, 9, 1));
    let data = &element["data"];
    assert_eq!(data["_type"], "externalFormat");
    assert_eq!(data["contentType"], "application/json");
    // From the type to `}#`.
    assert_eq!(span(data), (4, 3, 8, 4));
    assert!(element.get("dataResolvers").is_none(), "{element}");
}

#[test]
fn a_data_element_gives_other_elements_data() {
    let source =
        "###Data\nData my::MyData\n{\n  my::Db: Relation #{ t: a\n  1; }#;\n  my::Other;\n}\n";
    let element = &elements(source)[0];
    assert!(element.get("data").is_none(), "{element}");
    let base = &element["dataResolvers"][0];
    assert_eq!(base["_type"], "baseDataResolver");
    assert_eq!(
        base["elementPointer"],
        json!({"path": "my::Db", "sourceInformation": {"sourceId": "data.pure", "startLine": 4, "startColumn": 3, "endLine": 4, "endColumn": 8}})
    );
    assert_eq!(base["data"]["_type"], "relationAccessor");
    // From the element to `}#`, without the `;`.
    assert_eq!(span(base), (4, 3, 5, 7));
    let reference = &element["dataResolvers"][1];
    assert_eq!(reference["_type"], "referenceDataResolver");
    assert_eq!(reference["elementPointer"]["path"], "my::Other");
    assert_eq!(span(reference), (6, 3, 6, 11));
    assert_eq!(span(&reference["elementPointer"]), (6, 3, 6, 11));
}

#[test]
fn documentation_is_a_leading_doc_tagged_value() {
    let source =
        "###Data\n'''\n  About.\n  '''\nData my::MyData\n{\n  Reference #{ my::Other }#\n}\n";
    let element = &elements(source)[0];
    assert_eq!(element["taggedValues"][0]["tag"]["value"], "doc");
    // The element starts at its documentation.
    let (line, column, _, _) = span(element);
    assert_eq!((line, column), (2, 1));
    assert_eq!(element["data"]["dataElement"]["path"], "my::Other");
}

#[test]
fn a_data_section_has_no_imports() {
    let source = "###Data\nimport my::*;\nData my::MyData\n{\n}\n";
    let error = legend_pure_parser_parser::parse(source, "data.pure").expect_err("no imports");
    let si = error.errors[0].source_info().expect("positioned");
    // `import` is where the grammar fails, as the engine's does.
    assert_eq!((si.start_line, si.start_column), (2, 1));
}
