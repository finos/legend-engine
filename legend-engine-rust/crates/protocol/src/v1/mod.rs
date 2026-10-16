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

//! Protocol v1 model — Rust structs mirroring the Java `m3` protocol package.
//!
//! Each struct derives `Serialize`/`Deserialize` to produce JSON compatible with
//! the Java protocol. See [`crate`] for design rationale.

pub mod annotation;
pub mod context;
pub mod convert;
pub mod data;
pub mod element;
mod engine_operators;
pub mod from_protocol;
pub mod function_test;
pub mod generic_type;
pub mod multiplicity;
pub mod property;
pub mod record;
pub mod source_info;
pub mod test_assertion;
pub mod value_spec;
