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

//! # Legend Pure — MongoDB grammars
//!
//! The engine's MongoDB store extension (`legend-engine-xts-mongodb`):
//! `MongoDB` mapping elements, a class mapped from a collection's documents
//! (`MongoDBMappingParserGrammar` and its `MongoDBMappingParseTreeWalker`),
//! the engine's `RootMongoDBClassMapping`; and `MongoDBConnection`s, a
//! database's servers and authentication, the engine's
//! `MongoDBConnection`.
//!
//! ```pure
//! ###Mapping
//! Mapping my::M
//! (
//!   *my::Person: MongoDB
//!   {
//!     ~mainCollection [my::Db] people
//!     ~binding my::PersonBinding
//!   }
//! )
//! ```
//!
//! The extension's `###MongoDB` section is still to come (step 7 of
//! `docs/porting-status.md`).
//!
//! Crate layout:
//!
//! - [`mapping`] — [`mapping::MongoDbMappingGrammar`] registers as
//!   `dsl-mapping`'s mapping element grammar for `MongoDB`.
//! - [`connection`] — [`connection::MongoDbConnectionGrammar`] registers as
//!   `dsl-connection`'s connection grammar for `MongoDBConnection`.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod connection;
pub mod mapping;
