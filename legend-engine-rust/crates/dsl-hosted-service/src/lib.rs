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

//! # Legend Pure — hosted services
//!
//! The engine's `###HostedService` section (its `hostedService`
//! extension): functions activated as hosted services, and execution
//! environments.
//!
//! ```pure
//! ###HostedService
//! HostedService my::PeopleService
//! {
//!   pattern: '/people/{name}';
//!   function: my::people(String[1]):TabularDataSet[1];
//!   ownership: UserList { users: ['alice', 'bob'] };
//!   documentation: 'The people of a name';
//!   autoActivateUpdates: true;
//! }
//! ```
//!
//! Mirrors the engine's `HostedServiceParserGrammar` and
//! `HostedServiceTreeWalker`. The section is read whole first, then walked
//! in the walker's order: its hosted services, read, walked and converted
//! as `dsl-function-activator` reads, walks and converts function
//! activators; then its execution environments, `dsl-service`'s
//! `ExecutionEnvironment`s, their runtimes of their own read by
//! `dsl-runtime`. Not read: a hosted service's test suites and post
//! validations, which the walker leaves out, and its post-deployment
//! actions, which no grammar of the engine's reads: Rust reports them
//! unsupported, as it reports a deployment configuration, which the walker
//! makes an element without a name.
//!
//! Crate layout:
//!
//! - [`section`] — [`section::HostedServiceSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `HostedService`, and
//!   an `ActivatorProtocol` as
//!   [`legend_pure_parser_protocol::ElementProtocol`] for its hosted
//!   services.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

mod read;
pub mod section;
mod walk;

/// The section kind, `###HostedService`, which is also its hosted services'
/// kind.
pub const SECTION_KIND: &str = "HostedService";
