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

//! # Legend Pure — function activators
//!
//! What the engine's function activator grammars share, as their protocol
//! does (`legend-engine-xts-functionActivator`'s `FunctionActivator` and
//! `DeploymentOwner`): an activator's header, `'''doc''' Keyword <<s>> {t}
//! my::A { field … }`, the fields most activators have, read and walked,
//! and the element, with its JSON.
//!
//! ```pure
//! ###MemSql
//! MemSqlFunction <<doc.deprecated>> my::PeopleFunction
//! {
//!   functionName: 'people';
//!   function: my::people():TabularDataSet[1];
//!   ownership: Deployment { identifier: 'owner' };
//! }
//! ```
//!
//! Each activator's crate reads its section, its activators' fields with
//! [`read::activator`], and walks them with [`walk::activator`]; it
//! registers its section parser and an [`element::ActivatorProtocol`] for
//! its section's kind.
//!
//! Crate layout:
//!
//! - [`element`] — [`element::ActivatorElement`], a
//!   [`DSLElement`](legend_pure_parser_ast::dsl::DSLElement), and
//!   [`element::ActivatorProtocol`], its JSON.
//! - [`read`] — an activator's header and the shared fields' readers.
//! - [`walk`] — an activator's element and the shared fields' records.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod element;
pub mod read;
pub mod walk;
