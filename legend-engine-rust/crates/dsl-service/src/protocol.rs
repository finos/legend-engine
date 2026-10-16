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

//! `ElementProtocol`s for services and execution environments: the engine's
//! `Service` and `ExecutionEnvironmentInstance`.
//!
//! ```json
//! {
//!     "_type": "service",
//!     "package": "my",
//!     "name": "Service",
//!     "stereotypes": [], "taggedValues": [],
//!     "pattern": "/url", "documentation": "…", "autoActivateUpdates": false,
//!     "owners": [], "postValidations": [],
//!     "execution": {
//!         "_type": "pureSingleExecution",
//!         "func": { "_type": "lambda", … },
//!         "mapping": "my::Mapping",
//!         "runtime": { "_type": "runtimePointer", "runtime": "my::Runtime", … },
//!         …
//!     },
//!     "testSuites": [ { "_type": "serviceTestSuite", … } ],
//!     "sourceInformation": "…"
//! }
//! ```
//!
//! A query, an assertion or a post-validation's parameter that is not a
//! lambda is the body of an unpositioned lambda without parameters, as the
//! walker makes it. A test's parameters' values are positioned whether or
//! not the engine asks for positions: the walker parses them with a source
//! information of its own.

use legend_pure_dsl_runtime::ast::ElementPath;
use legend_pure_dsl_runtime::protocol::EngineRuntimeJson;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use legend_pure_parser_protocol::v1::annotation::{StereotypePtr, TaggedValue};
use legend_pure_parser_protocol::v1::convert::{
    convert_embedded_data, convert_expression_typed, convert_primitive_value,
    convert_test_assertion, section_stereotype,
};
use legend_pure_parser_protocol::v1::data::EmbeddedData;
use legend_pure_parser_protocol::v1::element::PackageableElementPointer;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::test_assertion::TestAssertion;
use legend_pure_parser_protocol::v1::value_spec::{
    ClassInstance, LambdaFunction, ValueSpecification,
};
use linkme::distributed_slice;
use serde::Serialize;
use serde_json::value::{RawValue, to_raw_value};

use crate::ast::{
    DataResolver, EXECUTION_ENVIRONMENT_KIND, Execution, ExecutionEnvironment, ExecutionParameters,
    LegacyTest, Ownership, PostValidation, Runtime, SECTION_KIND, Service, ServiceTest,
    ServiceTestSuite, SingleExecutionParameters, TestContainer, TestData, TestParameter,
};

/// Protocol converter for services.
pub struct ServiceProtocol;

impl ElementProtocol for ServiceProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Box<RawValue>> {
        let service = element.as_any().downcast_ref::<Service>().ok_or_else(|| {
            ConvertError::plugin(
                format!(
                    "ServiceProtocol: a '{}' element is not a Service",
                    element.kind()
                ),
                None,
            )
        })?;
        let json = ServiceJson {
            package: package(service.package.as_ref()),
            name: service.name.value.as_str(),
            stereotypes: service.stereotypes.iter().map(section_stereotype).collect(),
            tagged_values: service.tagged_values.iter().map(Into::into).collect(),
            pattern: &service.pattern,
            title: service.title.as_deref(),
            owners: &service.owners,
            ownership: service.ownership.as_ref().map(OwnershipJson::from),
            documentation: &service.documentation,
            auto_activate_updates: service.auto_activate_updates,
            execution: execution(&service.execution)?,
            test_suites: service
                .test_suites
                .as_ref()
                .map(|suites| suites.iter().map(test_suite).collect::<Result<_>>())
                .transpose()?,
            test: service.test.as_ref().map(legacy_test).transpose()?,
            post_validations: service
                .post_validations
                .iter()
                .map(post_validation)
                .collect::<Result<_>>()?,
            mcp_server: service.mcp_server.as_deref(),
            source_information: SourceInformation::from(&service.source_info),
        };
        Ok(to_raw_value(&json)?)
    }
}

/// Protocol converter for execution environments.
pub struct ExecutionEnvironmentProtocol;

impl ElementProtocol for ExecutionEnvironmentProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        EXECUTION_ENVIRONMENT_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Box<RawValue>> {
        let environment = element
            .as_any()
            .downcast_ref::<ExecutionEnvironment>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "ExecutionEnvironmentProtocol: a '{}' element is not an \
                         ExecutionEnvironment",
                        element.kind()
                    ),
                    None,
                )
            })?;
        let execution_parameters = match &environment.executions {
            ExecutionParameters::Single(singles) => singles
                .iter()
                .map(|single| {
                    single_parameters(single)
                        .map(|single| ExecutionParametersJson::Single(Box::new(single)))
                })
                .collect::<Result<Vec<_>>>()?,
            ExecutionParameters::Multi(multis) => multis
                .iter()
                .map(|multi| {
                    Ok(ExecutionParametersJson::Multi(
                        MultiExecutionParametersJson {
                            master_key: multi.master_key.as_str(),
                            single_execution_parameters: multi
                                .executions
                                .iter()
                                .map(single_parameters)
                                .collect::<Result<Vec<_>>>()?,
                        },
                    ))
                })
                .collect::<Result<Vec<_>>>()?,
        };
        Ok(to_raw_value(&ExecutionEnvironmentJson {
            package: package(environment.package.as_ref()),
            name: environment.name.value.as_str(),
            execution_parameters,
            source_information: SourceInformation::from(&environment.source_info),
        })?)
    }
}

/// The engine's `Service`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "service", rename_all = "camelCase")]
struct ServiceJson<'a> {
    package: String,
    name: &'a str,
    stereotypes: Vec<StereotypePtr>,
    tagged_values: Vec<TaggedValue>,
    pattern: &'a str,
    #[serde(skip_serializing_if = "Option::is_none")]
    title: Option<&'a str>,
    owners: &'a [String],
    #[serde(skip_serializing_if = "Option::is_none")]
    ownership: Option<OwnershipJson<'a>>,
    documentation: &'a str,
    auto_activate_updates: bool,
    execution: ExecutionJson<'a>,
    #[serde(skip_serializing_if = "Option::is_none")]
    test_suites: Option<Vec<TestSuiteJson<'a>>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    test: Option<LegacyTestJson<'a>>,
    post_validations: Vec<PostValidationJson<'a>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    mcp_server: Option<&'a str>,
    source_information: SourceInformation,
}

/// The engine's `ExecutionEnvironmentInstance`.
#[derive(Serialize)]
#[serde(
    tag = "_type",
    rename = "executionEnvironmentInstance",
    rename_all = "camelCase"
)]
struct ExecutionEnvironmentJson<'a> {
    package: String,
    name: &'a str,
    execution_parameters: Vec<ExecutionParametersJson<'a>>,
    source_information: SourceInformation,
}

/// The engine's `ExecutionParameters`.
#[derive(Serialize)]
#[serde(untagged)]
enum ExecutionParametersJson<'a> {
    Single(Box<SingleExecutionParametersJson<'a>>),
    Multi(MultiExecutionParametersJson<'a>),
}

/// The engine's `MultiExecutionParameters`.
#[derive(Serialize)]
#[serde(
    tag = "_type",
    rename = "multiExecutionParameters",
    rename_all = "camelCase"
)]
struct MultiExecutionParametersJson<'a> {
    master_key: &'a str,
    single_execution_parameters: Vec<SingleExecutionParametersJson<'a>>,
}

/// The engine's `SingleExecutionParameters`.
#[derive(Serialize)]
#[serde(
    tag = "_type",
    rename = "singleExecutionParameters",
    rename_all = "camelCase"
)]
struct SingleExecutionParametersJson<'a> {
    key: &'a str,
    mapping: &'a str,
    mapping_source_information: SourceInformation,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    runtime: Option<RuntimeJson<'a>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    runtime_components: Option<RuntimeComponentsJson<'a>>,
}

/// The engine's `RuntimeComponents`.
#[derive(Serialize)]
struct RuntimeComponentsJson<'a> {
    runtime: RuntimePointerJson<'a>,
    clazz: PackageableElementPointer,
    binding: PackageableElementPointer,
}

fn package(package: Option<&legend_pure_parser_ast::type_ref::Package>) -> String {
    package.map(ToString::to_string).unwrap_or_default()
}

/// The engine's `Ownership`s.
#[derive(Serialize)]
#[serde(tag = "_type")]
enum OwnershipJson<'a> {
    #[serde(rename = "deploymentOwnership")]
    Deployment { identifier: &'a str },
    #[serde(rename = "userListOwnership")]
    Users { users: &'a [String] },
}

impl<'a> From<&'a Ownership> for OwnershipJson<'a> {
    fn from(ownership: &'a Ownership) -> Self {
        match ownership {
            Ownership::Deployment(identifier) => Self::Deployment { identifier },
            Ownership::Users(users) => Self::Users { users },
        }
    }
}

/// The engine's `PureExecution`s.
#[derive(Serialize)]
#[serde(tag = "_type")]
enum ExecutionJson<'a> {
    #[serde(rename = "pureSingleExecution", rename_all = "camelCase")]
    Single {
        func: ValueSpecification,
        source_information: SourceInformation,
        #[serde(skip_serializing_if = "Option::is_none")]
        mapping: Option<&'a str>,
        #[serde(skip_serializing_if = "Option::is_none")]
        mapping_source_information: Option<SourceInformation>,
        #[serde(skip_serializing_if = "Option::is_none")]
        runtime: Option<RuntimeJson<'a>>,
    },
    #[serde(rename = "pureMultiExecution", rename_all = "camelCase")]
    Multi {
        func: ValueSpecification,
        source_information: SourceInformation,
        #[serde(skip_serializing_if = "Option::is_none")]
        execution_key: Option<&'a str>,
        #[serde(skip_serializing_if = "Vec::is_empty")]
        execution_parameters: Vec<KeyedExecutionParameterJson<'a>>,
    },
}

/// The engine's `KeyedExecutionParameter`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct KeyedExecutionParameterJson<'a> {
    key: &'a str,
    mapping: &'a str,
    mapping_source_information: SourceInformation,
    runtime: RuntimeJson<'a>,
    source_information: SourceInformation,
}

fn execution(execution: &Execution) -> Result<ExecutionJson<'_>> {
    Ok(match execution {
        Execution::Single {
            query,
            mapping_runtime,
            source_info,
        } => {
            let func = lambda(query)?;
            let (mapping, mapping_source_information, runtime) = match mapping_runtime {
                Some((mapping, runtime_value)) => (
                    Some(mapping.path.as_str()),
                    Some(SourceInformation::from(&mapping.source_info)),
                    Some(runtime(runtime_value)?),
                ),
                None => (None, None, None),
            };
            ExecutionJson::Single {
                func,
                source_information: SourceInformation::from(source_info),
                mapping,
                mapping_source_information,
                runtime,
            }
        }
        Execution::Multi {
            query,
            key,
            parameters,
            source_info,
        } => ExecutionJson::Multi {
            func: lambda(query)?,
            source_information: SourceInformation::from(source_info),
            execution_key: key.as_deref(),
            execution_parameters: parameters
                .iter()
                .map(|parameter| {
                    Ok(KeyedExecutionParameterJson {
                        key: &parameter.key,
                        mapping: &parameter.mapping.path,
                        mapping_source_information: SourceInformation::from(
                            &parameter.mapping.source_info,
                        ),
                        runtime: runtime(&parameter.runtime)?,
                        source_information: SourceInformation::from(&parameter.source_info),
                    })
                })
                .collect::<Result<Vec<_>>>()?,
        },
    })
}

/// A runtime: a pointer, or one of the service's own.
#[derive(Serialize)]
#[serde(untagged)]
enum RuntimeJson<'a> {
    Pointer(RuntimePointerJson<'a>),
    Embedded(EngineRuntimeJson<'a>),
}

fn runtime(runtime: &Runtime) -> Result<RuntimeJson<'_>> {
    match runtime {
        Runtime::Pointer(path) => Ok(RuntimeJson::Pointer(RuntimePointerJson::from(path))),
        Runtime::Embedded(runtime) => Ok(RuntimeJson::Embedded(EngineRuntimeJson::new(runtime)?)),
    }
}

/// The engine's `RuntimePointer`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "runtimePointer", rename_all = "camelCase")]
struct RuntimePointerJson<'a> {
    runtime: &'a str,
    source_information: SourceInformation,
}

impl<'a> From<&'a ElementPath> for RuntimePointerJson<'a> {
    fn from(path: &'a ElementPath) -> Self {
        Self {
            runtime: &path.path,
            source_information: SourceInformation::from(&path.source_info),
        }
    }
}

/// A query, an assertion or a parameter as the walker's `visitLambda` makes
/// it: a lambda, or the body of an unpositioned lambda without parameters.
fn lambda(expression: &Expression) -> Result<ValueSpecification> {
    Ok(match convert_expression_typed(expression)? {
        lambda @ ValueSpecification::Lambda(_) => lambda,
        body => ValueSpecification::Lambda(LambdaFunction {
            body: vec![body],
            parameters: Vec::new(),
            source_information: None,
        }),
    })
}

/// The engine's `ServiceTestSuite`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "serviceTestSuite", rename_all = "camelCase")]
struct TestSuiteJson<'a> {
    id: &'a str,
    tests: Vec<ServiceTestJson<'a>>,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    doc: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    test_data: Option<TestDataJson<'a>>,
}

fn test_suite(suite: &ServiceTestSuite) -> Result<TestSuiteJson<'_>> {
    Ok(TestSuiteJson {
        id: suite.id.as_str(),
        tests: suite.tests.iter().map(test).collect::<Result<Vec<_>>>()?,
        source_information: SourceInformation::from(&suite.source_info),
        doc: suite.doc.as_deref(),
        test_data: suite.test_data.as_ref().map(test_data).transpose()?,
    })
}

/// The engine's `TestData`: connections' data, or data resolvers.
#[derive(Serialize)]
#[serde(untagged, rename_all = "camelCase")]
enum TestDataJson<'a> {
    #[serde(rename_all = "camelCase")]
    Connections {
        source_information: SourceInformation,
        #[serde(skip_serializing_if = "Option::is_none")]
        connections_test_data: Option<Vec<ConnectionTestDataJson<'a>>>,
    },
    #[serde(rename_all = "camelCase")]
    Resolvers {
        service_test_data: Vec<DataResolverJson>,
        source_information: SourceInformation,
    },
}

/// The engine's `ConnectionTestData`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ConnectionTestDataJson<'a> {
    id: &'a str,
    data: EmbeddedData,
    source_information: SourceInformation,
}

fn test_data(data: &TestData) -> Result<TestDataJson<'_>> {
    Ok(match data {
        TestData::Connections {
            connections,
            source_info,
        } => TestDataJson::Connections {
            source_information: SourceInformation::from(source_info),
            connections_test_data: connections
                .as_ref()
                .map(|connections| {
                    connections
                        .iter()
                        .map(|connection| {
                            Ok(ConnectionTestDataJson {
                                id: &connection.connection,
                                data: convert_embedded_data(&connection.data)?,
                                source_information: SourceInformation::from(
                                    &connection.source_info,
                                ),
                            })
                        })
                        .collect::<Result<Vec<_>>>()
                })
                .transpose()?,
        },
        TestData::Resolvers {
            resolvers,
            source_info,
        } => TestDataJson::Resolvers {
            service_test_data: resolvers.iter().map(resolver).collect::<Result<Vec<_>>>()?,
            source_information: SourceInformation::from(source_info),
        },
    })
}

/// The engine's `DataResolver`s, their element's pointer without a type.
#[derive(Serialize)]
#[serde(tag = "_type")]
enum DataResolverJson {
    #[serde(rename = "baseDataResolver", rename_all = "camelCase")]
    Base {
        element_pointer: PackageableElementPointer,
        data: EmbeddedData,
        source_information: SourceInformation,
    },
    #[serde(rename = "referenceDataResolver", rename_all = "camelCase")]
    Reference {
        element_pointer: PackageableElementPointer,
        source_information: SourceInformation,
    },
}

fn resolver(resolver: &DataResolver) -> Result<DataResolverJson> {
    let pointer = |element: &ElementPath| PackageableElementPointer {
        element_type: None,
        path: element.path.clone(),
        source_information: Some(SourceInformation::from(&element.source_info)),
    };
    Ok(match resolver {
        DataResolver::Base {
            element,
            data,
            source_info,
        } => DataResolverJson::Base {
            element_pointer: pointer(element),
            data: convert_embedded_data(data)?,
            source_information: SourceInformation::from(source_info),
        },
        DataResolver::Reference(element) => DataResolverJson::Reference {
            element_pointer: pointer(element),
            source_information: SourceInformation::from(&element.source_info),
        },
    })
}

/// The engine's `ServiceTest`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "serviceTest", rename_all = "camelCase")]
struct ServiceTestJson<'a> {
    id: &'a str,
    keys: &'a [String],
    assertions: Vec<TestAssertion>,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    doc: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    serialization_format: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    parameters: Option<Vec<ParameterValueJson<'a>>>,
}

/// The engine's `ParameterValue`.
#[derive(Serialize)]
struct ParameterValueJson<'a> {
    name: &'a str,
    value: ValueSpecification,
}

fn test(test: &ServiceTest) -> Result<ServiceTestJson<'_>> {
    Ok(ServiceTestJson {
        id: test.id.as_str(),
        keys: &test.keys,
        assertions: test
            .assertions
            .iter()
            .map(|(id, assertion)| convert_test_assertion(id, assertion))
            .collect::<Result<Vec<_>>>()?,
        source_information: SourceInformation::from(&test.source_info),
        doc: test.doc.as_deref(),
        serialization_format: test.serialization_format.as_deref(),
        parameters: test
            .parameters
            .as_ref()
            .map(|parameters| {
                parameters
                    .iter()
                    .map(|parameter| {
                        Ok(ParameterValueJson {
                            name: parameter.name.as_str(),
                            value: convert_primitive_value(&parameter.value)?,
                        })
                    })
                    .collect::<Result<Vec<_>>>()
            })
            .transpose()?,
    })
}

/// The engine's legacy `ServiceTest_Legacy`s.
#[derive(Serialize)]
#[serde(tag = "_type")]
enum LegacyTestJson<'a> {
    #[serde(rename = "singleExecutionTest", rename_all = "camelCase")]
    Single {
        data: &'a str,
        asserts: Vec<TestContainerJson>,
        source_information: SourceInformation,
    },
    #[serde(rename = "multiExecutionTest", rename_all = "camelCase")]
    Multi {
        tests: Vec<KeyedSingleExecutionTestJson<'a>>,
        source_information: SourceInformation,
    },
}

/// The engine's `KeyedSingleExecutionTest`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct KeyedSingleExecutionTestJson<'a> {
    key: &'a str,
    data: &'a str,
    asserts: Vec<TestContainerJson>,
    source_information: SourceInformation,
}

fn legacy_test(test: &LegacyTest) -> Result<LegacyTestJson<'_>> {
    Ok(match test {
        LegacyTest::Single {
            data,
            asserts,
            source_info,
        } => LegacyTestJson::Single {
            data,
            asserts: asserts
                .iter()
                .map(test_container)
                .collect::<Result<Vec<_>>>()?,
            source_information: SourceInformation::from(source_info),
        },
        LegacyTest::Multi { tests, source_info } => LegacyTestJson::Multi {
            tests: tests
                .iter()
                .map(|test| {
                    Ok(KeyedSingleExecutionTestJson {
                        key: &test.key,
                        data: &test.data,
                        asserts: test
                            .asserts
                            .iter()
                            .map(test_container)
                            .collect::<Result<Vec<_>>>()?,
                        source_information: SourceInformation::from(&test.source_info),
                    })
                })
                .collect::<Result<Vec<_>>>()?,
            source_information: SourceInformation::from(source_info),
        },
    })
}

/// The engine's `TestContainer`: a list parameter is a `listInstance` of
/// the values.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct TestContainerJson {
    parameters_values: Vec<ValueSpecification>,
    assert: ValueSpecification,
    source_information: SourceInformation,
}

fn test_container(container: &TestContainer) -> Result<TestContainerJson> {
    let parameters_values = container
        .parameters
        .iter()
        .map(|parameter| match parameter {
            TestParameter::Value(value) => convert_primitive_value(value),
            TestParameter::List {
                values,
                source_info,
            } => {
                let source_information = Some(SourceInformation::from(source_info));
                Ok(ValueSpecification::ClassInstance(ClassInstance {
                    type_name: "listInstance".to_string(),
                    value: serde_json::to_value(ListInstanceJson {
                        values: values
                            .iter()
                            .map(convert_primitive_value)
                            .collect::<Result<Vec<_>>>()?,
                        source_information: source_information.clone(),
                    })?,
                    source_information,
                }))
            }
        })
        .collect::<Result<Vec<_>>>()?;
    Ok(TestContainerJson {
        parameters_values,
        assert: lambda(&container.assertion)?,
        source_information: SourceInformation::from(&container.source_info),
    })
}

/// A `listInstance` class instance's value: the values, positioned on the
/// list.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ListInstanceJson {
    values: Vec<ValueSpecification>,
    source_information: Option<SourceInformation>,
}

/// The engine's `PostValidation`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct PostValidationJson<'a> {
    description: &'a str,
    parameters: Vec<ValueSpecification>,
    assertions: Vec<PostValidationAssertionJson<'a>>,
    source_information: SourceInformation,
}

/// The engine's `PostValidationAssertion`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct PostValidationAssertionJson<'a> {
    id: &'a str,
    assertion: ValueSpecification,
    source_information: SourceInformation,
}

fn post_validation(validation: &PostValidation) -> Result<PostValidationJson<'_>> {
    Ok(PostValidationJson {
        description: &validation.description,
        parameters: validation
            .parameters
            .iter()
            .map(lambda)
            .collect::<Result<Vec<_>>>()?,
        assertions: validation
            .assertions
            .iter()
            .map(|assertion| {
                Ok(PostValidationAssertionJson {
                    id: assertion.id.as_str(),
                    assertion: lambda(&assertion.assertion)?,
                    source_information: SourceInformation::from(&assertion.source_info),
                })
            })
            .collect::<Result<Vec<_>>>()?,
        source_information: SourceInformation::from(&validation.source_info),
    })
}

fn single_parameters(
    single: &SingleExecutionParameters,
) -> Result<SingleExecutionParametersJson<'_>> {
    let pointer = |element_type: &str, path: &ElementPath| PackageableElementPointer {
        element_type: Some(element_type.to_string()),
        path: path.path.clone(),
        source_information: Some(SourceInformation::from(&path.source_info)),
    };
    Ok(SingleExecutionParametersJson {
        key: single.key.as_str(),
        mapping: &single.mapping.path,
        mapping_source_information: SourceInformation::from(&single.mapping.source_info),
        source_information: SourceInformation::from(&single.source_info),
        runtime: single.runtime.as_ref().map(runtime).transpose()?,
        runtime_components: single.runtime_components.as_ref().map(|components| {
            RuntimeComponentsJson {
                runtime: RuntimePointerJson::from(&components.runtime),
                clazz: pointer("CLASS", &components.class),
                binding: pointer("BINDING", &components.binding),
            }
        }),
    })
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static SERVICE_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &ServiceProtocol;

#[distributed_slice(ELEMENT_PROTOCOLS)]
static EXECUTION_ENVIRONMENT_PROTOCOL: &(dyn ElementProtocol + Send + Sync) =
    &ExecutionEnvironmentProtocol;
