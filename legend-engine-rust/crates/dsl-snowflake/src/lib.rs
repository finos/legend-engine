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

//! # Legend Pure — Snowflake function activators
//!
//! The engine's `###Snowflake` section (its Snowflake extension): functions
//! activated as Snowflake apps and as model-to-model UDFs.
//!
//! ```pure
//! ###Snowflake
//! SnowflakeApp my::PeopleApp
//! {
//!   applicationName: 'people';
//!   function: my::people():TabularDataSet[1];
//!   ownership: Deployment { identifier: 'owner' };
//!   activationConfiguration: my::SnowflakeConnection;
//! }
//! ```
//!
//! Mirrors the engine's `SnowflakeParserGrammar` and `SnowflakeTreeWalker`.
//! The section is read whole first, as blocks of fields
//! ([`legend_pure_parser_parser::blocks`]), then walked into protocol
//! records ([`legend_pure_parser_protocol::v1::record`]): its apps first,
//! then its UDFs, as the walker walks them. Its deployment configurations
//! are read and left out, as the walker leaves them.
//!
//! Crate layout:
//!
//! - [`ast`] — [`ast::SnowflakeElement`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement).
//! - [`section`] — [`section::SnowflakeSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `Snowflake`, and
//!   [`section::SnowflakeProtocol`] as
//!   [`legend_pure_parser_protocol::ElementProtocol`] for its elements.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod ast;
mod read;
pub mod section;
mod walk;
