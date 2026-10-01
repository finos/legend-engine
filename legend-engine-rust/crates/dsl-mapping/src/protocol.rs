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

//! `ElementProtocol` for mappings: the engine's `Mapping`.
//!
//! ```json
//! {
//!     "_type": "mapping",
//!     "package": "my",
//!     "name": "M",
//!     "includedMappings": [ { "_type": "mappingIncludeMapping", … } ],
//!     "classMappings": [ … ],
//!     "enumerationMappings": [ … ],
//!     "associationMappings": [ … ],
//!     "tests": [ { "name": …, "query": …, "inputData": [ … ], "assert": … } ],
//!     "testSuites": [ { "_type": "mappingTestSuite", … } ],
//!     "sourceInformation": { … }
//! }
//! ```
//!
//! Each element is converted by its type's grammar, and goes in the list
//! the grammar says. `testSuites` is absent without suites, as the engine
//! leaves it null; the other lists are always there.

use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use legend_pure_parser_protocol::v1::convert::{
    convert_embedded_data, convert_expression_typed, convert_test_assertion,
};
use legend_pure_parser_protocol::v1::data::EmbeddedData;
use legend_pure_parser_protocol::v1::element::PackageableElementPointer;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::test_assertion::TestAssertion;
use legend_pure_parser_protocol::v1::value_spec::{LambdaFunction, ValueSpecification};
use linkme::distributed_slice;
use serde::Serialize;

use crate::ast::{
    IncludeKind, LegacyTest, Mapping, MappingInclude, MappingTest, SECTION_KIND, StoreTestData,
    TestSuite,
};
use crate::grammar::{MappingElementKind, mapping_element_grammar, test_input_grammar};

/// Protocol converter for mappings.
pub struct MappingProtocol;

impl ElementProtocol for MappingProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<serde_json::Value> {
        let mapping = element.as_any().downcast_ref::<Mapping>().ok_or_else(|| {
            ConvertError::plugin(
                format!(
                    "MappingProtocol: a '{}' element is not a Mapping",
                    element.kind()
                ),
                None,
            )
        })?;
        let package = mapping
            .package
            .as_ref()
            .map(ToString::to_string)
            .unwrap_or_default();
        let name = mapping.name.value.to_string();
        let path = if package.is_empty() {
            name.clone()
        } else {
            format!("{package}::{name}")
        };
        let (mut class_mappings, mut enumeration_mappings, mut association_mappings) =
            (Vec::new(), Vec::new(), Vec::new());
        for element in &mapping.elements {
            let type_name = &element.header.type_name.text;
            let grammar = mapping_element_grammar(type_name).ok_or_else(|| {
                ConvertError::unsupported(
                    format!("no mapping element grammar is linked for '{type_name}'"),
                    &element.source_info,
                )
            })?;
            let (kind, json) = grammar.convert(element)?;
            match kind {
                MappingElementKind::Class => class_mappings.push(json),
                MappingElementKind::Enumeration => enumeration_mappings.push(json),
                MappingElementKind::Association => association_mappings.push(json),
            }
        }
        let json = MappingJson {
            package,
            name,
            included_mappings: mapping.includes.iter().map(include).collect(),
            class_mappings,
            enumeration_mappings,
            association_mappings,
            tests: mapping
                .legacy_tests
                .iter()
                .map(|test| legacy_test(test, &path))
                .collect::<Result<_>>()?,
            test_suites: mapping
                .test_suites
                .as_ref()
                .map(|suites| {
                    suites
                        .iter()
                        .map(|suite| test_suite(suite, &path))
                        .collect::<Result<_>>()
                })
                .transpose()?,
            source_information: SourceInformation::from(&mapping.source_info),
        };
        Ok(serde_json::to_value(json)?)
    }
}

/// The engine's `Mapping`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "mapping", rename_all = "camelCase")]
struct MappingJson {
    package: String,
    name: String,
    included_mappings: Vec<IncludeJson>,
    class_mappings: Vec<serde_json::Value>,
    enumeration_mappings: Vec<serde_json::Value>,
    association_mappings: Vec<serde_json::Value>,
    tests: Vec<LegacyTestJson>,
    #[serde(skip_serializing_if = "Option::is_none")]
    test_suites: Option<Vec<TestSuiteJson>>,
    source_information: SourceInformation,
}

/// The engine's `MappingInclude`s.
#[derive(Serialize)]
#[serde(tag = "_type")]
enum IncludeJson {
    /// The engine core's `MappingIncludeMapping`.
    #[serde(rename = "mappingIncludeMapping", rename_all = "camelCase")]
    Mapping {
        included_mapping: String,
        #[serde(skip_serializing_if = "Option::is_none")]
        source_database_path: Option<String>,
        #[serde(skip_serializing_if = "Option::is_none")]
        target_database_path: Option<String>,
        source_information: SourceInformation,
    },
    /// The data space extension's `MappingIncludeDataSpace`.
    #[serde(rename = "mappingIncludeDataSpace", rename_all = "camelCase")]
    DataSpace {
        included_data_space: String,
        source_information: SourceInformation,
    },
}

/// The engine's `MappingTest_Legacy`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct LegacyTestJson {
    name: String,
    query: serde_json::Value,
    input_data: Vec<serde_json::Value>,
    assert: ExpectedOutputJson,
    source_information: SourceInformation,
}

/// The engine's `ExpectedOutputMappingTestAssert`.
#[derive(Serialize)]
#[serde(
    tag = "_type",
    rename = "expectedOutputMappingTestAssert",
    rename_all = "camelCase"
)]
struct ExpectedOutputJson {
    expected_output: String,
    source_information: SourceInformation,
}

/// The engine's `MappingTestSuite`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "mappingTestSuite", rename_all = "camelCase")]
struct TestSuiteJson {
    id: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    doc: Option<String>,
    func: serde_json::Value,
    tests: Vec<MappingTestJson>,
    source_information: SourceInformation,
}

/// The engine's `MappingTest`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "mappingTest", rename_all = "camelCase")]
struct MappingTestJson {
    id: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    doc: Option<String>,
    assertions: Vec<TestAssertion>,
    store_test_data: Vec<StoreTestDataJson>,
    source_information: SourceInformation,
}

/// The engine's `StoreTestData`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct StoreTestDataJson {
    store: PackageableElementPointer,
    data: EmbeddedData,
    source_information: SourceInformation,
}

/// An include. The engine keeps a mapping's store substitution only when
/// there is one (`CorePureGrammarParser.parseMappingInclude`), and a data
/// space's never (`DataSpaceParserExtension.parseMappingInclude`).
fn include(include: &MappingInclude) -> IncludeJson {
    let source_information = SourceInformation::from(&include.source_info);
    match include.kind {
        IncludeKind::Mapping => {
            let (source_database_path, target_database_path) =
                match include.substitutions.as_slice() {
                    [substitution] => (
                        Some(substitution.source.clone()),
                        Some(substitution.target.clone()),
                    ),
                    _ => (None, None),
                };
            IncludeJson::Mapping {
                included_mapping: include.path.clone(),
                source_database_path,
                target_database_path,
                source_information,
            }
        }
        IncludeKind::DataSpace => IncludeJson::DataSpace {
            included_data_space: include.path.clone(),
            source_information,
        },
    }
}

fn legacy_test(test: &LegacyTest, mapping_path: &str) -> Result<LegacyTestJson> {
    Ok(LegacyTestJson {
        name: test.name.to_string(),
        query: lambda(&test.query, mapping_path)?,
        input_data: test
            .inputs
            .iter()
            .map(|input| {
                let type_name = &input.type_name.text;
                test_input_grammar(type_name)
                    .ok_or_else(|| {
                        ConvertError::unsupported(
                            format!("no mapping test input grammar is linked for '{type_name}'"),
                            &input.source_info,
                        )
                    })?
                    .convert(input)
            })
            .collect::<Result<_>>()?,
        assert: ExpectedOutputJson {
            expected_output: test.expected_output.clone(),
            source_information: SourceInformation::from(&test.assert_source_info),
        },
        source_information: SourceInformation::from(&test.source_info),
    })
}

fn test_suite(suite: &TestSuite, mapping_path: &str) -> Result<TestSuiteJson> {
    Ok(TestSuiteJson {
        id: suite.id.to_string(),
        doc: suite.doc.clone(),
        func: lambda(&suite.function, mapping_path)?,
        tests: suite.tests.iter().map(test).collect::<Result<_>>()?,
        source_information: SourceInformation::from(&suite.source_info),
    })
}

fn test(test: &MappingTest) -> Result<MappingTestJson> {
    Ok(MappingTestJson {
        id: test.id.to_string(),
        doc: test.doc.clone(),
        assertions: test
            .assertions
            .iter()
            .map(|(id, assertion)| convert_test_assertion(id, assertion))
            .collect::<Result<_>>()?,
        store_test_data: test
            .store_data
            .iter()
            .map(store_data)
            .collect::<Result<_>>()?,
        source_information: SourceInformation::from(&test.source_info),
    })
}

fn store_data(data: &StoreTestData) -> Result<StoreTestDataJson> {
    Ok(StoreTestDataJson {
        store: PackageableElementPointer {
            element_type: Some("STORE".to_string()),
            path: data.store.clone(),
            source_information: Some(SourceInformation::from(&data.store_source_info)),
        },
        data: convert_embedded_data(&data.data)?,
        source_information: SourceInformation::from(&data.source_info),
    })
}

/// A test's query, as the walker's `visitMappingTreeLambda` builds it: a
/// lambda, or the body of an unpositioned lambda without parameters. The
/// walker parses it as a source of its own named after the mapping, so its
/// positions carry the mapping's path.
fn lambda(expression: &Expression, mapping_path: &str) -> Result<serde_json::Value> {
    let lambda = match convert_expression_typed(expression)? {
        lambda @ ValueSpecification::Lambda(_) => lambda,
        body => ValueSpecification::Lambda(LambdaFunction {
            body: vec![body],
            parameters: Vec::new(),
            source_information: None,
        }),
    };
    let mut json = serde_json::to_value(lambda)?;
    retarget(&mut json, mapping_path);
    Ok(json)
}

/// Names `source_id` the source of every position in `json`.
fn retarget(json: &mut serde_json::Value, source_id: &str) {
    match json {
        serde_json::Value::Object(map) => {
            for (key, value) in map.iter_mut() {
                if key == "sourceId" {
                    *value = serde_json::Value::String(source_id.to_string());
                } else {
                    retarget(value, source_id);
                }
            }
        }
        serde_json::Value::Array(items) => {
            for item in items {
                retarget(item, source_id);
            }
        }
        _ => {}
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static MAPPING_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &MappingProtocol;
