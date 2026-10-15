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

//! Linking this crate registers its relation-store-accessor island with both the parser
//! (`ISLAND_PARSERS`) and the protocol converter (`ISLAND_PROTOCOLS`), so
//! plain `parse()` + `convert_source_file()` handle it with no wiring.

use legend_pure_dsl_store as _;
use legend_pure_parser_protocol::v1::convert::convert_source_file;

#[test]
fn linked_crate_registers_parser_and_protocol() {
    let source = "function f():Any[*] { #>{ my::Db.people }# }";
    let file = legend_pure_parser_parser::parse(source, "registration.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    let json = serde_json::to_value(&pmcd.elements[0]).expect("serializes");
    let island = &json["body"][0];
    assert_eq!(island["_type"], "classInstance");
    assert_eq!(island["type"], ">");
    let rendered = island["value"]["path"].to_string();
    assert!(rendered.contains("my::Db"), "{rendered}");
}
