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
//!     "sourceInformation": "…"
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
use legend_pure_parser_protocol::v1::source_info::{SourceInformation, edit_positions};
use legend_pure_parser_protocol::v1::test_assertion::TestAssertion;
use legend_pure_parser_protocol::v1::value_spec::{LambdaFunction, ValueSpecification};
use linkme::distributed_slice;
use serde::Serialize;
use serde_json::Value;
use serde_json::value::{RawValue, to_raw_value};

use crate::ast::{
    IncludeKind, LegacyTest, Mapping, MappingElement, MappingInclude, MappingTest, SECTION_KIND,
    StoreTestData, TestSuite,
};
use crate::grammar::{
    MappingElementGrammar, MappingElementKind, mapping_element_grammar, test_input_grammar,
};

/// Protocol converter for mappings.
pub struct MappingProtocol;

impl ElementProtocol for MappingProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Box<RawValue>> {
        let mapping = as_mapping(element)?;
        let (package, name, path) = names(mapping);
        let (mut class_mappings, mut enumeration_mappings, mut association_mappings) =
            (Vec::new(), Vec::new(), Vec::new());
        for element in &mapping.elements {
            let (kind, json) = grammar_of(element)?.convert(element)?;
            match kind {
                MappingElementKind::Class => class_mappings.push(json),
                MappingElementKind::Enumeration => enumeration_mappings.push(json),
                MappingElementKind::Association => association_mappings.push(json),
            }
        }
        let json = MappingJson {
            package,
            name,
            included_mappings: includes(mapping),
            class_mappings,
            enumeration_mappings,
            association_mappings,
            tests: tests(mapping, &path)?,
            test_suites: test_suites(mapping, &path)?,
            source_information: SourceInformation::from(&mapping.source_info),
        };
        Ok(to_raw_value(&json)?)
    }

    /// The mapping written in place with its elements, which are most of its
    /// JSON: `MappingJson`'s object, field by field, each element written
    /// where it goes by its grammar's `convert_into`. Class mappings, nearly
    /// all of them, go straight into their list; the others wait for theirs.
    fn convert_into(&self, element: &dyn DSLElement, out: &mut Vec<u8>) -> Result<()> {
        let mapping = as_mapping(element)?;
        let (package, name, path) = names(mapping);
        out.extend_from_slice(br#"{"_type":"mapping","package":"#);
        serde_json::to_writer(&mut *out, &package)?;
        out.extend_from_slice(br#","name":"#);
        serde_json::to_writer(&mut *out, &name)?;
        out.extend_from_slice(br#","includedMappings":"#);
        serde_json::to_writer(&mut *out, &includes(mapping))?;
        out.extend_from_slice(br#","classMappings":["#);
        let (mut enumerations, mut associations) = (Vec::new(), Vec::new());
        let mut classes = 0;
        for element in &mapping.elements {
            let start = out.len();
            if classes > 0 {
                out.push(b',');
            }
            let json = out.len();
            let list = match grammar_of(element)?.convert_into(element, out)? {
                MappingElementKind::Class => {
                    classes += 1;
                    continue;
                }
                MappingElementKind::Enumeration => &mut enumerations,
                MappingElementKind::Association => &mut associations,
            };
            if !list.is_empty() {
                list.push(b',');
            }
            list.extend_from_slice(&out[json..]);
            out.truncate(start);
        }
        out.extend_from_slice(br#"],"enumerationMappings":["#);
        out.extend_from_slice(&enumerations);
        out.extend_from_slice(br#"],"associationMappings":["#);
        out.extend_from_slice(&associations);
        out.extend_from_slice(br#"],"tests":"#);
        serde_json::to_writer(&mut *out, &tests(mapping, &path)?)?;
        if let Some(suites) = test_suites(mapping, &path)? {
            out.extend_from_slice(br#","testSuites":"#);
            serde_json::to_writer(&mut *out, &suites)?;
        }
        out.extend_from_slice(br#","sourceInformation":"#);
        serde_json::to_writer(&mut *out, &SourceInformation::from(&mapping.source_info))?;
        out.push(b'}');
        Ok(())
    }
}

fn as_mapping(element: &dyn DSLElement) -> Result<&Mapping> {
    element.as_any().downcast_ref::<Mapping>().ok_or_else(|| {
        ConvertError::plugin(
            format!(
                "MappingProtocol: a '{}' element is not a Mapping",
                element.kind()
            ),
            None,
        )
    })
}

/// The mapping's package, name and path.
fn names(mapping: &Mapping) -> (String, String, String) {
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
    (package, name, path)
}

/// The linked grammar of `element`'s type.
fn grammar_of(
    element: &MappingElement,
) -> Result<&'static (dyn MappingElementGrammar + Send + Sync)> {
    let type_name = &element.header.type_name.text;
    mapping_element_grammar(type_name).ok_or_else(|| {
        ConvertError::unsupported(
            format!("no mapping element grammar is linked for '{type_name}'"),
            &element.source_info,
        )
    })
}

fn includes(mapping: &Mapping) -> Vec<IncludeJson> {
    mapping.includes.iter().map(include).collect()
}

fn tests(mapping: &Mapping, path: &str) -> Result<Vec<LegacyTestJson>> {
    mapping
        .legacy_tests
        .iter()
        .map(|test| legacy_test(test, path))
        .collect()
}

/// Absent without suites, as the engine leaves them null.
fn test_suites(mapping: &Mapping, path: &str) -> Result<Option<Vec<TestSuiteJson>>> {
    mapping
        .test_suites
        .as_ref()
        .map(|suites| suites.iter().map(|suite| test_suite(suite, path)).collect())
        .transpose()
}

/// The engine's `Mapping`. Its elements are their grammars' JSON.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "mapping", rename_all = "camelCase")]
struct MappingJson {
    package: String,
    name: String,
    included_mappings: Vec<IncludeJson>,
    class_mappings: Vec<Box<RawValue>>,
    enumeration_mappings: Vec<Box<RawValue>>,
    association_mappings: Vec<Box<RawValue>>,
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

/// The engine's `MappingTest_Legacy`. Its inputs are their grammars' JSON.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct LegacyTestJson {
    name: String,
    query: Value,
    input_data: Vec<Value>,
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
    func: Value,
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
    let query = lambda(&test.query, mapping_path)?;
    let input_data = test
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
        .collect::<Result<_>>()?;
    Ok(LegacyTestJson {
        name: test.name.to_string(),
        query,
        input_data,
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
    edit_positions(&mut json, &mut |position| {
        position.source_id = mapping_path.to_string();
    })?;
    Ok(json)
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static MAPPING_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &MappingProtocol;
