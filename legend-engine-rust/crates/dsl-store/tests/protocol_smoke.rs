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

//! Locks the `RelationStoreAccessorProtocol` end-to-end: a
//! `#>{ store::path.tbl }#` island parses through `dsl-store`'s parser
//! plug-in, then converts to a v1 `ClassInstance { _type: ">", path:
//! [...] }` JSON value via `dispatch_island_convert` +
//! `default_island_protocols()`.

use legend_pure_dsl_store::parser::default_island_parsers;
use legend_pure_dsl_store::protocol::default_island_protocols;
use legend_pure_parser_ast::element::Element;
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_protocol::IslandProtocol;
use legend_pure_parser_protocol::dispatch_island_convert;

#[test]
fn relation_store_accessor_converts_to_protocol_class_instance() {
    let source = r"
###Pure
function my::func(): Any[*] {
    #>{ my::mainDb.PersonTable }#
}
";
    let sf = legend_pure_parser_parser::parse_with_islands(
        source,
        "protocol_smoke.pure",
        default_island_parsers(),
    )
    .expect("parse failed");

    let ast_elem = sf.all_elements().next().expect("first element");
    let Element::Function(f) = ast_elem else {
        panic!("expected function");
    };
    let Expression::Island(island) = f.body.first().expect("body") else {
        panic!("expected island expression");
    };

    let protocols = default_island_protocols();
    let refs: Vec<&dyn IslandProtocol> =
        protocols.iter().map(std::convert::AsRef::as_ref).collect();
    let value = dispatch_island_convert(island, &refs)
        .expect("convert ok")
        .expect("converter matched");

    let json = serde_json::to_value(&value).expect("serialise to json");
    // The engine's shape: ClassInstance { type: ">", value: { path, sourceInformation } },
    // both positioned on the whole island.
    assert_eq!(
        json["type"], ">",
        "ClassInstance.type should be `>`. Got: {json}"
    );
    assert!(json["value"].get("_type").is_none(), "{json}");
    assert_eq!(
        json["value"]["sourceInformation"],
        json["sourceInformation"]
    );
    let path = json["value"]["path"].as_array().expect("path array");
    let path_strs: Vec<&str> = path.iter().map(|v| v.as_str().expect("string")).collect();
    assert_eq!(path_strs, vec!["my::mainDb", "PersonTable"]);
}
