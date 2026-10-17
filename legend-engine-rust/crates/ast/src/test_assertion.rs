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

//! Test assertions, `Type #{ … }#`, in the tests of the engine's section
//! grammars (mapping and service tests): the engine's `TestAssertion`s.

use crate::data::{EmbeddedDataValue, RelationElement};
use crate::expression::Expression;
use crate::source_info::SourceInfo;

/// A test's assertion. Its id is the name the test gives it.
#[derive(Debug, Clone, PartialEq)]
pub enum TestAssertion {
    /// `EqualTo #{ expected: value; }#`: equal to a primitive value or a
    /// collection of them.
    EqualTo {
        /// The expected value, as written.
        expected: Expression,
        /// From `EqualTo` to `}#`.
        source_info: SourceInfo,
    },
    /// `EqualToJson #{ expected: ExternalFormat #{ … }#; }#`: equal to data
    /// in an external format.
    EqualToJson {
        /// The expected data, `ExternalFormat` data.
        expected: EmbeddedDataValue,
        /// From `EqualToJson` to `}#`.
        source_info: SourceInfo,
    },
    /// `Relation #{ … }#`: equal to a relation, the engine's
    /// `EqualToRelation`.
    EqualToRelation {
        /// The relation: the first table of the body read as one without a
        /// path, positioned as the engine positions it (see the parser).
        expected: RelationElement,
        /// From `Relation` to `}#`.
        source_info: SourceInfo,
    },
}
