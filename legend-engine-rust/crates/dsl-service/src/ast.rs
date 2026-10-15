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

//! The service AST: the `###Service` section's services and execution
//! environments.

use std::any::Any;

use legend_pure_dsl_runtime::ast::{ElementPath, EngineRuntime};
use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::data::EmbeddedDataValue;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::test_assertion::TestAssertion;
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use smol_str::SmolStr;

/// The section kind, `###Service`, which is also its services' kind.
pub const SECTION_KIND: &str = "Service";

/// The kind of the section's execution environments.
pub const EXECUTION_ENVIRONMENT_KIND: &str = "ExecutionEnvironment";

/// `Service my::S { … }`: a service, the engine's `Service`.
#[derive(Debug, Clone, PartialEq)]
pub struct Service {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// `<<profile.stereotype>>`.
    pub stereotypes: Vec<StereotypePtr>,
    /// `{profile.tag = 'value'}`, a documentation literal's first.
    pub tagged_values: Vec<TaggedValue>,
    /// `pattern: '/url/{param}';`.
    pub pattern: String,
    /// `title: '…';`.
    pub title: Option<String>,
    /// `owners: [ '…', … ];`, deprecated for `ownership`.
    pub owners: Vec<String>,
    /// `ownership: DID { identifier: '…' };` or `ownership: UserList { … };`.
    pub ownership: Option<Ownership>,
    /// `documentation: '…';`.
    pub documentation: String,
    /// `autoActivateUpdates: true;`, false without.
    pub auto_activate_updates: bool,
    /// `execution: Single { … }` or `execution: Multi { … }`.
    pub execution: Execution,
    /// `testSuites: [ … ]`; `None` without, as the engine leaves them null.
    pub test_suites: Option<Vec<ServiceTestSuite>>,
    /// `test: Single { … }` or `test: Multi { … }`, the legacy test.
    pub test: Option<LegacyTest>,
    /// `postValidations: [ { … }, … ]`.
    pub post_validations: Vec<PostValidation>,
    /// `mcpServer: name;`.
    pub mcp_server: Option<SmolStr>,
    /// From the documentation, or `Service`, to `}`.
    pub source_info: SourceInfo,
}

/// Who owns a service.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Ownership {
    /// `DID { identifier: '…' }`: a deployment.
    Deployment(String),
    /// `UserList { users: [ '…', … ] }`: users.
    Users(Vec<String>),
}

/// What a service runs.
#[derive(Debug, Clone, PartialEq)]
pub enum Execution {
    /// `Single { query: …; mapping: …; runtime: …; }`, the engine's
    /// `PureSingleExecution`.
    Single {
        /// The query, as written: a lambda, or its body.
        query: Expression,
        /// The mapping and its runtime, which come together.
        mapping_runtime: Option<(ElementPath, Runtime)>,
        /// From `Single` to `}`.
        source_info: SourceInfo,
    },
    /// `Multi { query: …; key: '…'; executions['key']: { … } … }`, the
    /// engine's `PureMultiExecution`.
    Multi {
        /// The query, as written: a lambda, or its body.
        query: Expression,
        /// `key: '…';`, the name of the parameter choosing the execution.
        key: Option<String>,
        /// `executions['key']: { mapping: …; runtime: …; }`, each.
        parameters: Vec<KeyedExecutionParameter>,
        /// From `Multi` to `}`.
        source_info: SourceInfo,
    },
}

/// `executions['key']: { mapping: …; runtime: …; }`: the mapping and the
/// runtime a multi execution runs with for a key.
#[derive(Debug, Clone, PartialEq)]
pub struct KeyedExecutionParameter {
    /// The key.
    pub key: String,
    /// The mapping.
    pub mapping: ElementPath,
    /// The runtime.
    pub runtime: Runtime,
    /// From `executions` to `}`.
    pub source_info: SourceInfo,
}

/// A service's runtime: a packageable runtime's path, or its own.
#[derive(Debug, Clone, PartialEq)]
pub enum Runtime {
    /// `my::Runtime`.
    Pointer(ElementPath),
    /// `#{ mappings: [ … ]; connections: [ … ]; }#`.
    Embedded(EngineRuntime),
}

/// A test suite: `id: { data: [ … ] tests: [ … ] }`, or the flat form
/// `id 'doc'? ( data… tests… )`.
#[derive(Debug, Clone, PartialEq)]
pub struct ServiceTestSuite {
    /// The id, unquoted.
    pub id: SmolStr,
    /// The flat form's documentation.
    pub doc: Option<String>,
    /// The suite's data.
    pub test_data: Option<TestData>,
    /// The tests.
    pub tests: Vec<ServiceTest>,
    /// From the id to `}` or `)`.
    pub source_info: SourceInfo,
}

/// A test suite's data: its connections' (`data: [ connections: [ … ] ]`),
/// or the flat form's data resolvers.
#[derive(Debug, Clone, PartialEq)]
pub enum TestData {
    /// `data: [ connections: [ my::Connection: Type #{ … }#, … ] ]`.
    Connections {
        /// The connections' data; `None` without `connections`.
        connections: Option<Vec<ConnectionTestData>>,
        /// From `data` to `]`.
        source_info: SourceInfo,
    },
    /// `my::Store: Type #{ … }#;` and `my::DataElement;` statements.
    Resolvers {
        /// The statements.
        resolvers: Vec<DataResolver>,
        /// The suite's body, from its documentation, or `(`, to `)`.
        source_info: SourceInfo,
    },
}

/// `my::Connection: Type #{ … }#`: a connection's test data.
#[derive(Debug, Clone, PartialEq)]
pub struct ConnectionTestData {
    /// The connection's path.
    pub connection: String,
    /// Its data.
    pub data: EmbeddedDataValue,
    /// From the path to `}#`.
    pub source_info: SourceInfo,
}

/// The flat form's data: an element's (`my::Store: Type #{ … }#;`), or a
/// data element's (`my::DataElement;`).
#[derive(Debug, Clone, PartialEq)]
pub enum DataResolver {
    /// `my::Store: Type #{ … }#`, the engine's `BaseDataResolver`.
    Base {
        /// The element.
        element: ElementPath,
        /// Its data.
        data: EmbeddedDataValue,
        /// From the element to `}#`.
        source_info: SourceInfo,
    },
    /// `my::DataElement`, the engine's `ReferenceDataResolver`.
    Reference(ElementPath),
}

/// A test: `id: { parameters: [ … ] serializationFormat: …; keys: [ … ];
/// asserts: [ … ] }`, or the flat form
/// `id 'doc'? (params)? [keys]? (: format)? => Type #{ … }#;`.
#[derive(Debug, Clone, PartialEq)]
pub struct ServiceTest {
    /// The id, unquoted.
    pub id: SmolStr,
    /// The flat form's documentation.
    pub doc: Option<String>,
    /// The service's parameters; `None` without, as the engine leaves them
    /// null.
    pub parameters: Option<Vec<ParameterValue>>,
    /// The keys of the result the test compares.
    pub keys: Vec<String>,
    /// The serialization format, unquoted.
    pub serialization_format: Option<SmolStr>,
    /// The assertions, each with its id.
    pub assertions: Vec<(SmolStr, TestAssertion)>,
    /// From the id to `}`, or to `;`.
    pub source_info: SourceInfo,
}

/// `name = value`: a value for a service parameter.
#[derive(Debug, Clone, PartialEq)]
pub struct ParameterValue {
    /// The parameter's name, unquoted.
    pub name: SmolStr,
    /// A primitive value, an enum value, or a collection of them.
    pub value: Expression,
}

/// A legacy test: `test: Single { … }` or `test: Multi { … }`.
#[derive(Debug, Clone, PartialEq)]
pub enum LegacyTest {
    /// `Single { data: '…'; asserts: [ … ]; }`, the engine's
    /// `SingleExecutionTest`.
    Single {
        /// The data.
        data: String,
        /// The assertions.
        asserts: Vec<TestContainer>,
        /// From `Single` to `}`.
        source_info: SourceInfo,
    },
    /// `Multi { tests['key']: { … } … }`, the engine's `MultiExecutionTest`.
    Multi {
        /// The tests, each for an execution key.
        tests: Vec<KeyedSingleExecutionTest>,
        /// From `Multi` to `}`.
        source_info: SourceInfo,
    },
}

/// `tests['key']: { data: '…'; asserts: [ … ]; }`.
#[derive(Debug, Clone, PartialEq)]
pub struct KeyedSingleExecutionTest {
    /// The execution key.
    pub key: String,
    /// The data.
    pub data: String,
    /// The assertions.
    pub asserts: Vec<TestContainer>,
    /// From `tests` to `}`.
    pub source_info: SourceInfo,
}

/// `{ [ parameters ], assertion }`: a legacy assertion, the engine's
/// `TestContainer`.
#[derive(Debug, Clone, PartialEq)]
pub struct TestContainer {
    /// The service's parameters' values.
    pub parameters: Vec<TestParameter>,
    /// The assertion, as written: a lambda, or its body.
    pub assertion: Expression,
    /// From `{` to `}`.
    pub source_info: SourceInfo,
}

/// A legacy assertion's parameter value.
#[derive(Debug, Clone, PartialEq)]
pub enum TestParameter {
    /// `list([ value, … ])`.
    List {
        /// The values.
        values: Vec<Expression>,
        /// From `list` to `)`.
        source_info: SourceInfo,
    },
    /// A value.
    Value(Box<Expression>),
}

/// `{ description: '…'; params: [ … ]; assertions: [ … ]; }`: a check of
/// the service's result, the engine's `PostValidation`.
#[derive(Debug, Clone, PartialEq)]
pub struct PostValidation {
    /// The description.
    pub description: String,
    /// The parameters, each as written: a lambda, or its body.
    pub parameters: Vec<Expression>,
    /// The assertions.
    pub assertions: Vec<PostValidationAssertion>,
    /// From `{` to `}`.
    pub source_info: SourceInfo,
}

/// `id: assertion`.
#[derive(Debug, Clone, PartialEq)]
pub struct PostValidationAssertion {
    /// The id, unquoted.
    pub id: SmolStr,
    /// The assertion, as written: a lambda, or its body.
    pub assertion: Expression,
    /// From the id to the assertion's end.
    pub source_info: SourceInfo,
}

/// `ExecutionEnvironment my::E { executions: [ … ]; }`: the mappings and
/// runtimes a service runs with by key, the engine's
/// `ExecutionEnvironmentInstance`.
#[derive(Debug, Clone, PartialEq)]
pub struct ExecutionEnvironment {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// The executions: single ones, or groups of them.
    pub executions: ExecutionParameters,
    /// From `ExecutionEnvironment` to `}`.
    pub source_info: SourceInfo,
}

/// An execution environment's executions, all of one form.
#[derive(Debug, Clone, PartialEq)]
pub enum ExecutionParameters {
    /// `key: { mapping: …; runtime: …; }`, each.
    Single(Vec<SingleExecutionParameters>),
    /// `key: [ key: { … }, … ]`, each.
    Multi(Vec<MultiExecutionParameters>),
}

/// `key: { mapping: …; runtime: …; runtimeComponents: { … } }`.
#[derive(Debug, Clone, PartialEq)]
pub struct SingleExecutionParameters {
    /// The key, unquoted.
    pub key: SmolStr,
    /// The mapping.
    pub mapping: ElementPath,
    /// The runtime.
    pub runtime: Option<Runtime>,
    /// The runtime's components.
    pub runtime_components: Option<RuntimeComponents>,
    /// From the key to `}`.
    pub source_info: SourceInfo,
}

/// `key: [ key: { … }, … ]`.
#[derive(Debug, Clone, PartialEq)]
pub struct MultiExecutionParameters {
    /// The key, unquoted.
    pub master_key: SmolStr,
    /// The executions.
    pub executions: Vec<SingleExecutionParameters>,
}

/// `runtimeComponents: { runtime: …; class: …; binding: …; }`: each a path,
/// positioned from its field to its `;`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RuntimeComponents {
    /// The runtime.
    pub runtime: ElementPath,
    /// The class.
    pub class: ElementPath,
    /// The binding.
    pub binding: ElementPath,
}

impl Spanned for Service {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for Service {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &self.stereotypes
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &self.tagged_values
    }
}

impl PackageableElement for Service {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for Service {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn DSLElement> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn DSLElement) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}

impl Spanned for ExecutionEnvironment {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for ExecutionEnvironment {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl PackageableElement for ExecutionEnvironment {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for ExecutionEnvironment {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        EXECUTION_ENVIRONMENT_KIND
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn DSLElement> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn DSLElement) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}
