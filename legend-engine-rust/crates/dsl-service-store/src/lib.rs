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

//! # Legend Pure — Service store grammars
//!
//! The engine's service store extension (`legend-engine-xt-serviceStore`):
//!
//! - the `###ServiceStore` section: stores of services, in groups, with
//!   their parameters and responses (`ServiceStoreParserGrammar`'s
//!   `definition` and its `ServiceStoreParseTreeWalker`), the engine's
//!   `ServiceStore`;
//! - `ServiceStore` mapping elements: a class mapped from services' responses
//!   (`ServiceStoreParserGrammar`'s `mapping` rule and its
//!   `ServiceStoreParseTreeWalker`), the engine's
//!   `RootServiceStoreClassMapping`;
//! - `ServiceStoreConnection`s: the services' base URL
//!   (`ServiceStoreConnectionParserGrammar` and its walker), the engine's
//!   `ServiceStoreConnection`;
//! - `ServiceStore #{ … }#` test data: stubs of the services' requests and
//!   responses (`ServiceStoreEmbeddedDataParserGrammar`, its walker and its
//!   `EqualTo` and `EqualToJson` content patterns), the engine's
//!   `ServiceStoreEmbeddedData`.
//!
//! ```pure
//! ###Mapping
//! Mapping my::M
//! (
//!   *my::Person: ServiceStore
//!   {
//!     ~service [my::Store] people.byName
//!     (
//!       ~request
//!       (
//!         parameters
//!         (
//!           name = $this.name
//!         )
//!       )
//!     )
//!   }
//! )
//! ```
//!
//! Crate layout:
//!
//! - [`mapping`] — [`mapping::ServiceStoreMappingGrammar`] registers as
//!   `dsl-mapping`'s mapping element grammar for `ServiceStore`.
//! - [`connection`] — [`connection::ServiceStoreConnectionGrammar`] registers
//!   as `dsl-connection`'s connection grammar for `ServiceStoreConnection`.
//! - [`data`] — [`data::ServiceStoreDataParser`] and
//!   [`data::ServiceStoreDataProtocol`] register as the parser's and the
//!   protocol's embedded data grammar for `ServiceStore`.
//! - [`store`] — the `###ServiceStore` section, its stores, services and
//!   groups: [`store::section::ServiceStoreSectionParser`] and
//!   [`store::section::ServiceStoreProtocol`] register as its section
//!   grammar and element protocol.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod connection;
pub mod data;
pub mod mapping;
pub mod store;
