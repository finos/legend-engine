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

//! Operation class mappings, `my::Person: Operation { …(p1, p2) }`, as the
//! engine's `OperationClassMappingParseTreeWalker` reads them.

use legend_pure_dsl_mapping as _;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The mapping `###Mapping\nMapping my::M\n(\n{element}\n)\n` defines.
fn mapping(element: &str) -> Value {
    let source = format!("###Mapping\nMapping my::M\n(\n{element}\n)\n");
    let file = legend_pure_parser_parser::parse(&source, "m.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

#[test]
fn an_operation_is_named_by_its_function() {
    let m = mapping(
        "  *my::Person[u]: Operation\n  {\n    meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(p1, 'p 2');\n  }\n  my::Firm: Operation\n  {\n    my::op()\n  }",
    );
    let union = &m["classMappings"][0];
    assert_eq!(union["_type"], "operation");
    assert_eq!(union["operation"], "STORE_UNION");
    assert_eq!(union["parameters"], json!(["p1", "p 2"]));
    assert_eq!(union["id"], "u");
    // A path of no operation names none.
    let other = &m["classMappings"][1];
    assert!(other.get("operation").is_none());
    assert_eq!(other["parameters"], json!([]));
}

#[test]
fn a_merge_validation_is_parsed_again_where_the_function_starts() {
    let m = mapping(
        "  my::Person: Operation\n  {\n    meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_([p1, p2], {a: my::A[1], b: my::B[1] | $a.id == $b.id})\n  }",
    );
    let merge = &m["classMappings"][0];
    assert_eq!(merge["_type"], "mergeOperation");
    assert_eq!(merge["operation"], "MERGE");
    assert_eq!(merge["parameters"], json!(["p1", "p2"]));
    // The body of an unpositioned lambda.
    let validation = &merge["validationFunction"];
    assert!(validation.get("sourceInformation").is_none());
    let lambda = &validation["body"][0];
    assert_eq!(lambda["_type"], "lambda");
    // Its text without spaces, `{a:my::A[1],b:my::B[1]|$a.id==$b.id}`,
    // starting at the function's column, 5: `a` is at 6, `b` at 17.
    let si = |node: &Value| {
        let si: SourceInformation =
            serde_json::from_value(node["sourceInformation"].clone()).expect("positioned");
        (si.start_line, si.start_column)
    };
    assert_eq!(si(&lambda["parameters"][0]), (6, 6));
    assert_eq!(si(&lambda["parameters"][1]), (6, 17));
}
