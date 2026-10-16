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

//! # Legend Pure — Services
//!
//! The engine's `###Service` section (its service extension): services and
//! execution environments.
//!
//! ```pure
//! ###Service
//! Service my::PeopleService
//! {
//!   pattern: '/people/{name}';
//!   documentation: 'The people of a name';
//!   execution: Single
//!   {
//!     query: name: String[1]|my::Person.all()->filter(p | $p.name == $name);
//!     mapping: my::Mapping;
//!     runtime: my::Runtime;
//!   }
//!   testSuites:
//!   [
//!     suite:
//!     {
//!       data: [ connections: [ my::Connection: ExternalFormat #{ … }# ] ]
//!       tests: [ test: { parameters: [ name = 'Ann' ] asserts: [ shape: EqualToJson #{ … }# ] } ]
//!     }
//!   ]
//! }
//! ```
//!
//! Mirrors the engine's `ServiceParserGrammar` / `ServiceParseTreeWalker`
//! and its `Service` and `ExecutionEnvironmentInstance` protocol. A
//! service's own runtime, `#{ … }#`, is read by `dsl-runtime`; its tests'
//! data and assertions by the parser's grammars for them, as anywhere
//! else.
//!
//! The engine records a section's imports only when the section has a
//! service; Rust records them for a section of execution environments too.
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::Service`] and [`ast::ExecutionEnvironment`],
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement)s.
//! - [`section`] — [`section::ServiceSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `Service`.
//! - [`protocol`] — [`protocol::ServiceProtocol`] and
//!   [`protocol::ExecutionEnvironmentProtocol`] register as
//!   [`legend_pure_parser_protocol::ElementProtocol`]s.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
pub mod protocol;
pub mod section;
