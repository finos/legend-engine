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

//! Protocol test assertions: the engine's `TestAssertion`s, in function,
//! mapping and service tests.

use serde::{Deserialize, Serialize};

use super::data::{ExternalFormatData, RelationElement};
use super::source_info::SourceInformation;
use super::value_spec::ValueSpecification;

/// A test's assertion on its result: a function's, a mapping's or a
/// service's.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(tag = "_type")]
pub enum TestAssertion {
    /// Equal to a primitive value or a collection of them.
    #[serde(rename = "equalTo", rename_all = "camelCase")]
    EqualTo {
        /// Assertion id.
        id: String,
        /// The expected value.
        expected: ValueSpecification,
        /// Source location.
        #[serde(default, skip_serializing_if = "Option::is_none")]
        source_information: Option<SourceInformation>,
    },
    /// Equal to JSON (or another external format) data.
    #[serde(rename = "equalToJson", rename_all = "camelCase")]
    EqualToJson {
        /// Assertion id.
        id: String,
        /// The expected data.
        #[serde(serialize_with = "serialize_external_format")]
        expected: ExternalFormatData,
        /// Source location.
        #[serde(default, skip_serializing_if = "Option::is_none")]
        source_information: Option<SourceInformation>,
    },
    /// Equal to a relation.
    #[serde(rename = "equalToRelation", rename_all = "camelCase")]
    EqualToRelation {
        /// Assertion id.
        id: String,
        /// The expected relation.
        expected: RelationElement,
        /// Source location.
        #[serde(default, skip_serializing_if = "Option::is_none")]
        source_information: Option<SourceInformation>,
    },
}

/// Serializes external format data in a field declared as such (an
/// assertion's `expected`) the way the engine does: with the `_type` every
/// embedded data carries. Inside [`EmbeddedData`] it gets it from the enum.
fn serialize_external_format<S: serde::Serializer>(
    data: &ExternalFormatData,
    serializer: S,
) -> Result<S::Ok, S::Error> {
    #[derive(Serialize)]
    #[serde(tag = "_type", rename = "externalFormat")]
    struct Tagged<'a> {
        #[serde(flatten)]
        data: &'a ExternalFormatData,
    }
    Tagged { data }.serialize(serializer)
}
