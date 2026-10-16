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

//! # Legend Pure — Generations
//!
//! The engine's generation sections (its generation extension):
//! `###FileGeneration`, a generator's configuration, and
//! `###GenerationSpecification`, which elements and file generations to
//! generate.
//!
//! ```pure
//! ###FileGeneration
//! Avro my::AvroConfig
//! {
//!   scopeElements: [my::Person];
//!   generationOutputPath: 'avro';
//!   includeNamespace: true;
//! }
//!
//! ###GenerationSpecification
//! GenerationSpecification my::Generation
//! {
//!   generationNodes: [ { id: 'model'; generationElement: my::Model; } ];
//!   fileGenerations: [ my::AvroConfig ];
//! }
//! ```
//!
//! Mirrors the engine's `FileGenerationParserGrammar`,
//! `GenerationSpecificationParserGrammar` and their walkers: each section
//! read whole first, then each element walked into a protocol record
//! ([`legend_pure_parser_protocol::v1::record`]). Both sections have
//! imports.
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::GenerationElement`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement).
//! - [`section`] — the sections' parsers and their elements' protocols.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
mod file_generation;
mod generation_specification;
pub mod section;
