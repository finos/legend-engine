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
//!     "sourceInformation": { … }
//! }
//! ```
//!
//! A query, an assertion or a post-validation's parameter that is not a
//! lambda is the body of an unpositioned lambda without parameters, as the
//! walker makes it. A test's parameters' values are positioned whether or
//! not the engine asks for positions: the walker parses them with a source
//! information of its own.

use legend_pure_dsl_runtime::ast::ElementPath;
use legend_pure_dsl_runtime::protocol::convert_engine_runtime;
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
use legend_pure_parser_protocol::v1::element::PackageableElementPointer;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::value_spec::{
    ClassInstance, LambdaFunction, ValueSpecification,
};
use linkme::distributed_slice;
use serde::Serialize;
use serde_json::{Value, json};

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

    fn convert(&self, element: &dyn DSLElement) -> Result<Value> {
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
            name: service.name.value.to_string(),
            stereotypes: service.stereotypes.iter().map(section_stereotype).collect(),
            tagged_values: service.tagged_values.iter().map(Into::into).collect(),
            pattern: service.pattern.clone(),
            title: service.title.clone(),
            owners: service.owners.clone(),
            ownership: service.ownership.as_ref().map(ownership),
            documentation: service.documentation.clone(),
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
            mcp_server: service.mcp_server.as_ref().map(ToString::to_string),
            source_information: SourceInformation::from(&service.source_info),
        };
        Ok(serde_json::to_value(json)?)
    }
}

/// Protocol converter for execution environments.
pub struct ExecutionEnvironmentProtocol;

impl ElementProtocol for ExecutionEnvironmentProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        EXECUTION_ENVIRONMENT_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Value> {
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
        let parameters = match &environment.executions {
            ExecutionParameters::Single(singles) => singles
                .iter()
                .map(single_parameters)
                .collect::<Result<Vec<_>>>()?,
            ExecutionParameters::Multi(multis) => multis
                .iter()
                .map(|multi| {
                    Ok(json!({
                        "_type": "multiExecutionParameters",
                        "masterKey": multi.master_key.as_str(),
                        "singleExecutionParameters": multi
                            .executions
                            .iter()
                            .map(single_parameters)
                            .collect::<Result<Vec<_>>>()?,
                    }))
                })
                .collect::<Result<Vec<_>>>()?,
        };
        Ok(json!({
            "_type": "executionEnvironmentInstance",
            "package": package(environment.package.as_ref()),
            "name": environment.name.value.as_str(),
            "executionParameters": parameters,
            "sourceInformation": SourceInformation::from(&environment.source_info),
        }))
    }
}

/// The engine's `Service`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "service", rename_all = "camelCase")]
struct ServiceJson {
    package: String,
    name: String,
    stereotypes: Vec<StereotypePtr>,
    tagged_values: Vec<TaggedValue>,
    pattern: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    title: Option<String>,
    owners: Vec<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    ownership: Option<Value>,
    documentation: String,
    auto_activate_updates: bool,
    execution: Value,
    #[serde(skip_serializing_if = "Option::is_none")]
    test_suites: Option<Vec<Value>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    test: Option<Value>,
    post_validations: Vec<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    mcp_server: Option<String>,
    source_information: SourceInformation,
}

fn package(package: Option<&legend_pure_parser_ast::type_ref::Package>) -> String {
    package.map(ToString::to_string).unwrap_or_default()
}

fn ownership(ownership: &Ownership) -> Value {
    match ownership {
        Ownership::Deployment(identifier) => {
            json!({"_type": "deploymentOwnership", "identifier": identifier})
        }
        Ownership::Users(users) => json!({"_type": "userListOwnership", "users": users}),
    }
}

fn execution(execution: &Execution) -> Result<Value> {
    Ok(match execution {
        Execution::Single {
            query,
            mapping_runtime,
            source_info,
        } => {
            let mut json = json!({
                "_type": "pureSingleExecution",
                "func": lambda(query)?,
                "sourceInformation": SourceInformation::from(source_info),
            });
            if let Some((mapping, runtime_value)) = mapping_runtime {
                json["mapping"] = json!(mapping.path);
                json["mappingSourceInformation"] =
                    json!(SourceInformation::from(&mapping.source_info));
                json["runtime"] = runtime(runtime_value)?;
            }
            json
        }
        Execution::Multi {
            query,
            key,
            parameters,
            source_info,
        } => {
            let mut json = json!({
                "_type": "pureMultiExecution",
                "func": lambda(query)?,
                "sourceInformation": SourceInformation::from(source_info),
            });
            if let Some(key) = key {
                json["executionKey"] = json!(key);
            }
            if !parameters.is_empty() {
                json["executionParameters"] = parameters
                    .iter()
                    .map(|parameter| {
                        Ok(json!({
                            "key": parameter.key,
                            "mapping": parameter.mapping.path,
                            "mappingSourceInformation":
                                SourceInformation::from(&parameter.mapping.source_info),
                            "runtime": runtime(&parameter.runtime)?,
                            "sourceInformation": SourceInformation::from(&parameter.source_info),
                        }))
                    })
                    .collect::<Result<Vec<_>>>()?
                    .into();
            }
            json
        }
    })
}

fn runtime(runtime: &Runtime) -> Result<Value> {
    match runtime {
        Runtime::Pointer(path) => Ok(runtime_pointer(path)),
        Runtime::Embedded(runtime) => convert_engine_runtime(runtime),
    }
}

/// The engine's `RuntimePointer`.
fn runtime_pointer(path: &ElementPath) -> Value {
    json!({
        "_type": "runtimePointer",
        "runtime": path.path,
        "sourceInformation": SourceInformation::from(&path.source_info),
    })
}

/// A query, an assertion or a parameter as the walker's `visitLambda` makes
/// it: a lambda, or the body of an unpositioned lambda without parameters.
fn lambda(expression: &Expression) -> Result<Value> {
    let lambda = match convert_expression_typed(expression)? {
        lambda @ ValueSpecification::Lambda(_) => lambda,
        body => ValueSpecification::Lambda(LambdaFunction {
            body: vec![body],
            parameters: Vec::new(),
            source_information: None,
        }),
    };
    Ok(serde_json::to_value(lambda)?)
}

fn test_suite(suite: &ServiceTestSuite) -> Result<Value> {
    let mut json = json!({
        "_type": "serviceTestSuite",
        "id": suite.id.as_str(),
        "tests": suite.tests.iter().map(test).collect::<Result<Vec<_>>>()?,
        "sourceInformation": SourceInformation::from(&suite.source_info),
    });
    if let Some(doc) = &suite.doc {
        json["doc"] = json!(doc);
    }
    if let Some(data) = &suite.test_data {
        json["testData"] = test_data(data)?;
    }
    Ok(json)
}

fn test_data(data: &TestData) -> Result<Value> {
    Ok(match data {
        TestData::Connections {
            connections,
            source_info,
        } => {
            let mut json = json!({"sourceInformation": SourceInformation::from(source_info)});
            if let Some(connections) = connections {
                json["connectionsTestData"] = connections
                    .iter()
                    .map(|connection| {
                        Ok(json!({
                            "id": connection.connection,
                            "data": convert_embedded_data(&connection.data)?,
                            "sourceInformation": SourceInformation::from(&connection.source_info),
                        }))
                    })
                    .collect::<Result<Vec<_>>>()?
                    .into();
            }
            json
        }
        TestData::Resolvers {
            resolvers,
            source_info,
        } => json!({
            "serviceTestData": resolvers.iter().map(resolver).collect::<Result<Vec<_>>>()?,
            "sourceInformation": SourceInformation::from(source_info),
        }),
    })
}

/// The engine's `DataResolver`s, their element's pointer without a type.
fn resolver(resolver: &DataResolver) -> Result<Value> {
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
        } => json!({
            "_type": "baseDataResolver",
            "elementPointer": pointer(element),
            "data": convert_embedded_data(data)?,
            "sourceInformation": SourceInformation::from(source_info),
        }),
        DataResolver::Reference(element) => json!({
            "_type": "referenceDataResolver",
            "elementPointer": pointer(element),
            "sourceInformation": SourceInformation::from(&element.source_info),
        }),
    })
}

fn test(test: &ServiceTest) -> Result<Value> {
    let mut json = json!({
        "_type": "serviceTest",
        "id": test.id.as_str(),
        "keys": test.keys,
        "assertions": test
            .assertions
            .iter()
            .map(|(id, assertion)| convert_test_assertion(id, assertion))
            .collect::<Result<Vec<_>>>()?,
        "sourceInformation": SourceInformation::from(&test.source_info),
    });
    if let Some(doc) = &test.doc {
        json["doc"] = json!(doc);
    }
    if let Some(format) = &test.serialization_format {
        json["serializationFormat"] = json!(format.as_str());
    }
    if let Some(parameters) = &test.parameters {
        json["parameters"] = parameters
            .iter()
            .map(|parameter| {
                Ok(json!({
                    "name": parameter.name.as_str(),
                    "value": convert_primitive_value(&parameter.value)?,
                }))
            })
            .collect::<Result<Vec<_>>>()?
            .into();
    }
    Ok(json)
}

fn legacy_test(test: &LegacyTest) -> Result<Value> {
    Ok(match test {
        LegacyTest::Single {
            data,
            asserts,
            source_info,
        } => json!({
            "_type": "singleExecutionTest",
            "data": data,
            "asserts": asserts.iter().map(test_container).collect::<Result<Vec<_>>>()?,
            "sourceInformation": SourceInformation::from(source_info),
        }),
        LegacyTest::Multi { tests, source_info } => json!({
            "_type": "multiExecutionTest",
            "tests": tests
                .iter()
                .map(|test| {
                    Ok(json!({
                        "key": test.key,
                        "data": test.data,
                        "asserts": test.asserts.iter().map(test_container).collect::<Result<Vec<_>>>()?,
                        "sourceInformation": SourceInformation::from(&test.source_info),
                    }))
                })
                .collect::<Result<Vec<_>>>()?,
            "sourceInformation": SourceInformation::from(source_info),
        }),
    })
}

/// The engine's `TestContainer`: a list parameter is a `listInstance` of
/// the values.
fn test_container(container: &TestContainer) -> Result<Value> {
    let parameters = container
        .parameters
        .iter()
        .map(|parameter| match parameter {
            TestParameter::Value(value) => {
                Ok(serde_json::to_value(convert_primitive_value(value)?)?)
            }
            TestParameter::List {
                values,
                source_info,
            } => {
                let source_information = Some(SourceInformation::from(source_info));
                let list = ValueSpecification::ClassInstance(ClassInstance {
                    type_name: "listInstance".to_string(),
                    value: json!({
                        "values": values
                            .iter()
                            .map(convert_primitive_value)
                            .collect::<Result<Vec<_>>>()?,
                        "sourceInformation": source_information,
                    }),
                    source_information,
                });
                Ok(serde_json::to_value(list)?)
            }
        })
        .collect::<Result<Vec<_>>>()?;
    Ok(json!({
        "parametersValues": parameters,
        "assert": lambda(&container.assertion)?,
        "sourceInformation": SourceInformation::from(&container.source_info),
    }))
}

fn post_validation(validation: &PostValidation) -> Result<Value> {
    Ok(json!({
        "description": validation.description,
        "parameters": validation.parameters.iter().map(lambda).collect::<Result<Vec<_>>>()?,
        "assertions": validation
            .assertions
            .iter()
            .map(|assertion| {
                Ok(json!({
                    "id": assertion.id.as_str(),
                    "assertion": lambda(&assertion.assertion)?,
                    "sourceInformation": SourceInformation::from(&assertion.source_info),
                }))
            })
            .collect::<Result<Vec<_>>>()?,
        "sourceInformation": SourceInformation::from(&validation.source_info),
    }))
}

fn single_parameters(single: &SingleExecutionParameters) -> Result<Value> {
    let mut json = json!({
        "_type": "singleExecutionParameters",
        "key": single.key.as_str(),
        "mapping": single.mapping.path,
        "mappingSourceInformation": SourceInformation::from(&single.mapping.source_info),
        "sourceInformation": SourceInformation::from(&single.source_info),
    });
    if let Some(runtime_value) = &single.runtime {
        json["runtime"] = runtime(runtime_value)?;
    }
    if let Some(components) = &single.runtime_components {
        let pointer = |element_type: &str, path: &ElementPath| PackageableElementPointer {
            element_type: Some(element_type.to_string()),
            path: path.path.clone(),
            source_information: Some(SourceInformation::from(&path.source_info)),
        };
        json["runtimeComponents"] = json!({
            "runtime": runtime_pointer(&components.runtime),
            "clazz": pointer("CLASS", &components.class),
            "binding": pointer("BINDING", &components.binding),
        });
    }
    Ok(json)
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static SERVICE_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &ServiceProtocol;

#[distributed_slice(ELEMENT_PROTOCOLS)]
static EXECUTION_ENVIRONMENT_PROTOCOL: &(dyn ElementProtocol + Send + Sync) =
    &ExecutionEnvironmentProtocol;
