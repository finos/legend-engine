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

//! Text blocks, documentation sugar and string escapes, as the engine's
//! `PureGrammarParserUtility` derives them.

use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;

fn elements(source: &str) -> Vec<serde_json::Value> {
    let file = legend_pure_parser_parser::parse(source, "doc.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd).expect("serializes")["elements"]
        .as_array()
        .expect("elements")
        .clone()
}

fn parse_error(source: &str) -> (String, u32) {
    let partial = legend_pure_parser_parser::parse(source, "doc.pure").expect_err("rejected");
    let error = &partial.errors[0];
    (
        error.message(),
        error.source_info().expect("positioned").start_line,
    )
}

#[test]
fn documentation_is_a_leading_doc_tagged_value() {
    let source = "'''\n  A person.\n\n  Identity is \\d+.\n  '''\nClass <<a::p.s>> {a::p.t = 'x'} model::Person\n{\n  '''\n  The name.\n  '''\n  name: String[1];\n}\n";
    let class = &elements(source)[0];
    let tagged = class["taggedValues"].as_array().expect("tagged values");
    assert_eq!(tagged.len(), 2);
    assert_eq!(tagged[0]["tag"]["profile"], "meta::pure::profiles::doc");
    assert_eq!(tagged[0]["tag"]["value"], "doc");
    // Literal content (no unescape), blank edges dropped, multi-line wire form.
    assert_eq!(
        tagged[0]["value"],
        serde_json::json!({"_type": "string", "multiLine": true, "value": "A person.\n\nIdentity is \\d+."})
    );
    // Spans the literal: from its opening quotes to its closing quotes.
    let si: SourceInformation =
        serde_json::from_value(tagged[0]["sourceInformation"].clone()).expect("positioned");
    assert_eq!((si.start_line, si.end_line, si.end_column), (1, 5, 5));
    assert_eq!(tagged[1]["value"], "x");
    let property = &class["properties"][0];
    assert_eq!(property["taggedValues"][0]["value"]["value"], "The name.");
}

#[test]
fn enum_values_and_quoted_names() {
    let source = "Enum model::E\n{\n  '''\n  First.\n  '''\n  A,\n  'quoted value'\n}\nClass model::C\n{\n  'quoted name': String[1];\n}\n";
    let els = elements(source);
    assert_eq!(
        els[0]["values"][0]["taggedValues"][0]["value"]["value"],
        "First."
    );
    assert_eq!(els[0]["values"][1]["value"], "quoted value");
    assert!(els[0]["values"][1].get("taggedValues").is_none());
    assert_eq!(els[1]["properties"][0]["name"], "quoted name");
}

#[test]
fn documentation_errors() {
    let (message, line) = parse_error("'not a block'\nClass model::C {}\n");
    assert_eq!(
        message,
        "Documentation must be written as a multi-line ('''...''') literal"
    );
    assert_eq!(line, 1);
    let (message, line) = parse_error("'''\n  Doc.\n  '''\nClass {doc.doc = 'also'} model::C {}\n");
    assert_eq!(
        message,
        "Element has both documentation and an explicit doc.doc tagged value. Use one."
    );
    assert_eq!(line, 4);
}

#[test]
fn text_block_strings_and_java_escapes() {
    let source =
        "function model::f(): Any[*]\n{\n  ['''\n    a\\tb\n      c\n    ''', 'it\\'s \\d']\n}\n";
    let body = &elements(source)[0]["body"][0]["values"];
    assert_eq!(body[0]["value"], "a\tb\n  c\n");
    assert_eq!(body[0]["multiLine"], true);
    // Ordinary strings: unescapeJava drops unknown escapes' backslashes.
    assert_eq!(body[1]["value"], "it's d");
    assert!(body[1].get("multiLine").is_none());
}
