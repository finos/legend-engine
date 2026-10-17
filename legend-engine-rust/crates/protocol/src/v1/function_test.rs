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

//! Protocol function tests: the engine's `FunctionTestSuite`,
//! `FunctionTest` and `FunctionTestData`. Their embedded data is in
//! [`data`](super::data), their assertions in
//! [`test_assertion`](super::test_assertion).

use serde::{Deserialize, Serialize};

use super::data::EmbeddedData;
use super::element::PackageableElementPointer;
use super::source_info::SourceInformation;
use super::test_assertion::TestAssertion;
use super::value_spec::ValueSpecification;

/// The id of a function's unnamed suite and of every test assertion.
pub const DEFAULT_TESTABLE_ID: &str = "default";

/// A suite: the tests and data of a function's test block (id `default`)
/// or of one named suite.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(tag = "_type", rename = "functionTestSuite", rename_all = "camelCase")]
pub struct FunctionTestSuite {
    /// Suite id.
    pub id: String,
    /// Tests. Always written, even empty: the engine's walker sets it, and
    /// its class defaults it to null.
    #[serde(default)]
    pub tests: Vec<FunctionTest>,
    /// Store data. Always written, even empty, as `tests`.
    #[serde(default)]
    pub test_data: Vec<FunctionTestData>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// One test: a call of the function and its expected result.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(tag = "_type", rename = "functionTest", rename_all = "camelCase")]
pub struct FunctionTest {
    /// Test id.
    pub id: String,
    /// Documentation.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub doc: Option<String>,
    /// The call's arguments.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub parameters: Vec<ParameterValue>,
    /// The expected result.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub assertions: Vec<TestAssertion>,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// An argument of a test's call, named after the function's parameter.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ParameterValue {
    /// The parameter's name.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// The value.
    pub value: ValueSpecification,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}

/// A store's data for a suite.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FunctionTestData {
    /// The store.
    pub packageable_element_pointer: PackageableElementPointer,
    /// The data.
    pub data: EmbeddedData,
    /// Source location.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source_information: Option<SourceInformation>,
}
