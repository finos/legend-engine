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

//! Shared test utilities for AST tests.
//!
//! This module provides common helper functions used across multiple
//! test modules in the `ast` crate to avoid duplication.

use crate::SourceInfo;

/// Creates a dummy [`SourceInfo`] for use in unit tests.
///
/// Returns a source location pointing to `test.pure:1:1-1:20`.
#[must_use]
pub fn src() -> SourceInfo {
    SourceInfo::new("test.pure", 1, 1, 1, 20)
}
