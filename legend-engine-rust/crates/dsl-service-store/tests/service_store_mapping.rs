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

//! Service store class mappings, as the engine's `ServiceStoreParseTreeWalker`
//! reads them. Linking this crate registers the grammar into `dsl-mapping`'s.

use legend_pure_dsl_mapping as _;
use legend_pure_dsl_service_store as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The mapping `###Mapping\nMapping my::M\n(\n{body}\n)\n` defines.
fn mapping(body: &str) -> Value {
    let source = format!("###Mapping\nMapping my::M\n(\n{body}\n)\n");
    let file = legend_pure_parser_parser::parse(&source, "m.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

fn error(body: &str) -> ParseError {
    let source = format!("###Mapping\nMapping my::M\n(\n{body}\n)\n");
    legend_pure_parser_parser::parse(&source, "m.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn span(node: &Value) -> (u64, u64, u64, u64) {
    let si: SourceInformation = serde_json::from_value(node["sourceInformation"].clone())
        .unwrap_or_else(|e| panic!("no position in {node}: {e}"));
    (
        si.start_line.into(),
        si.start_column.into(),
        si.end_line.into(),
        si.end_column.into(),
    )
}

#[test]
fn a_class_mapping_reads_local_properties_and_services() {
    let m = mapping(
        "  *my::Person[p]: ServiceStore\n  {\n    +id: String[1];\n    +firm: my::Firm <- my::FirmSchema[0..1];\n    ~service [my::Store] people.byName\n    (\n      ~path $service.response.data.items\n      ~request\n      (\n        parameters\n        (\n          name = $this.name,\n          \"x-key\" = 'k'\n        )\n        body = $this.id\n      )\n    )\n  }",
    );
    let class_mapping = &m["classMappings"][0];
    assert_eq!(class_mapping["_type"], "serviceStore");
    assert_eq!(class_mapping["class"], "my::Person");
    assert_eq!(class_mapping["id"], "p");
    assert_eq!(class_mapping["root"], true);
    assert_eq!(span(class_mapping), (4, 3, 21, 3));
    // The type as written, without spaces.
    let properties = &class_mapping["localMappingProperties"];
    assert_eq!(properties[0]["name"], "id");
    assert_eq!(properties[0]["type"], "String");
    assert_eq!(
        properties[0]["multiplicity"],
        json!({"lowerBound": 1, "upperBound": 1})
    );
    assert_eq!(span(&properties[0]), (6, 5, 6, 19));
    assert_eq!(properties[1]["type"], "my::Firm<-my::FirmSchema");
    assert_eq!(
        properties[1]["multiplicity"],
        json!({"lowerBound": 0, "upperBound": 1})
    );
    let service_mapping = &class_mapping["servicesMapping"][0];
    assert_eq!(span(service_mapping), (8, 5, 20, 5));
    // The service, in its group, each positioned on its name.
    let service = &service_mapping["service"];
    assert_eq!(service["serviceStore"], "my::Store");
    assert_eq!(service["service"], "byName");
    assert_eq!(span(service), (8, 33, 8, 38));
    assert_eq!(service["parent"]["serviceGroup"], "people");
    assert_eq!(service["parent"]["serviceStore"], "my::Store");
    assert_eq!(span(&service["parent"]), (8, 26, 8, 31));
    assert_eq!(
        service_mapping["pathOffset"],
        json!({
            "startType": "$service.response",
            "path": [
                {"_type": "propertyPath", "property": "data", "parameters": []},
                {"_type": "propertyPath", "property": "items", "parameters": []},
            ],
        })
    );
    let request = &service_mapping["requestBuildInfo"];
    assert_eq!(span(request), (11, 7, 19, 7));
    let parameters = &request["requestParametersBuildInfo"];
    assert_eq!(span(parameters), (13, 9, 17, 9));
    let name = &parameters["parameterBuildInfoList"][0];
    assert_eq!(name["serviceParameter"], "name");
    assert_eq!(span(name), (15, 11, 15, 27));
    // A lambda without parameters, positioned on the expression.
    let transform = &name["transform"];
    assert_eq!(transform["_type"], "lambda");
    assert!(transform["parameters"].as_array().is_none_or(Vec::is_empty));
    assert_eq!(span(transform), (15, 18, 15, 27));
    assert_eq!(transform["body"][0]["property"], "name");
    // A quoted parameter, unquoted.
    assert_eq!(
        parameters["parameterBuildInfoList"][1]["serviceParameter"],
        "x-key"
    );
    let body = &request["requestBodyBuildInfo"];
    assert_eq!(span(body), (18, 9, 18, 23));
    assert_eq!(span(&body["transform"]), (18, 16, 18, 23));
    assert_eq!(body["transform"]["body"][0]["property"], "id");
}

#[test]
fn a_class_mapping_may_have_no_id_and_a_service_no_block() {
    let m = mapping("  my::Person: ServiceStore\n  {\n    ~service [my::Store] byName\n  }");
    let class_mapping = &m["classMappings"][0];
    assert_eq!(class_mapping["root"], false);
    assert!(class_mapping.get("id").is_none(), "{class_mapping}");
    let service_mapping = &class_mapping["servicesMapping"][0];
    assert!(service_mapping["service"].get("parent").is_none());
    assert!(service_mapping.get("pathOffset").is_none());
    assert!(service_mapping.get("requestBuildInfo").is_none());
}

#[test]
fn the_old_parameter_mappings_replace_the_requests_parameters() {
    let m = mapping(
        "  *my::Person: ServiceStore\n  {\n    ~service [my::Store] byName\n    (\n      ~request\n      (\n        parameters\n        (\n          ignored = 1\n        )\n      )\n      ~paramMapping\n      (\n        name: $this.name\n      )\n      firstName: $service.parameters.\"first-name\"\n    )\n  }",
    );
    let request = &m["classMappings"][0]["servicesMapping"][0]["requestBuildInfo"];
    // The request keeps its span; its parameters are the old mappings',
    // without one.
    assert_eq!(span(request), (8, 7, 14, 7));
    let parameters = &request["requestParametersBuildInfo"];
    assert!(
        parameters.get("sourceInformation").is_none(),
        "{parameters}"
    );
    let list = &parameters["parameterBuildInfoList"];
    assert_eq!(list.as_array().map(Vec::len), Some(2));
    assert_eq!(list[0]["serviceParameter"], "name");
    assert_eq!(span(&list[0]), (17, 9, 17, 24));
    assert_eq!(span(&list[0]["transform"]), (17, 15, 17, 24));
    // A property's parameter: `$this.property`, parsed where the property
    // is written, in a lambda spanning the mapping.
    let property = &list[1];
    assert_eq!(property["serviceParameter"], "first-name");
    assert_eq!(span(property), (19, 7, 19, 49));
    assert_eq!(span(&property["transform"]), (19, 7, 19, 49));
    let access = &property["transform"]["body"][0];
    assert_eq!(access["property"], "firstName");
    assert_eq!(access["parameters"][0]["name"], "this");
    assert_eq!(span(&access["parameters"][0]).0, 19);
    assert_eq!(span(&access["parameters"][0]).1, 7);
}

#[test]
fn the_old_parameter_mappings_make_a_request_without_a_span() {
    let m = mapping(
        "  *my::Person: ServiceStore\n  {\n    ~service [my::Store] byName\n    (\n      ~paramMapping\n      (\n        name: 'n'\n      )\n    )\n  }",
    );
    let request = &m["classMappings"][0]["servicesMapping"][0]["requestBuildInfo"];
    assert!(request.get("sourceInformation").is_none(), "{request}");
    assert_eq!(
        request["requestParametersBuildInfo"]["parameterBuildInfoList"][0]["serviceParameter"],
        "name"
    );
}

#[test]
fn a_service_mapping_cant_extend_another() {
    // The walker's error, once the body is read, on the super mapping's id.
    let extends = error(
        "  my::Person[p] extends [q]: ServiceStore\n  {\n    ~service [my::Store] byName\n  }",
    );
    assert!(
        extends
            .message()
            .contains("Service Store Mapping does not support extends"),
        "{extends}"
    );
    let si = extends.source_info().expect("positioned");
    assert_eq!((si.start_line, si.start_column, si.end_column), (4, 26, 26));
    // A syntax error in the body comes first.
    let syntax =
        error("  my::Person[p] extends [q]: ServiceStore\n  {\n    ~service my::Store byName\n  }");
    let si = syntax.source_info().expect("positioned");
    assert_eq!((si.start_line, si.start_column), (6, 14));
}

#[test]
fn parameters_need_their_parentheses() {
    // As the grammar reads `parameters`, it expects `(` next.
    let error = error(
        "  my::Person: ServiceStore\n  {\n    ~service [my::Store] byName\n    (\n      ~request\n      (\n        parameters name = 1\n      )\n    )\n  }",
    );
    let si = error.source_info().expect("positioned");
    assert_eq!((si.start_line, si.start_column), (10, 20));
}
