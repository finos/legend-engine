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

//! # Legend Pure — Elasticsearch grammars
//!
//! The engine's Elasticsearch store extension
//! (`legend-engine-xt-elasticsearch-V7-grammar`):
//! `Elasticsearch7ClusterConnection`s, a cluster's URL and authentication
//! (`ElasticsearchConnectionParserGrammar` and the extension's walker), the
//! engine's `Elasticsearch7StoreConnection`.
//!
//! ```pure
//! ###Connection
//! Elasticsearch7ClusterConnection my::Connection
//! {
//!   store: my::Store;
//!   clusterDetails: # URL { http://localhost:9200 }#;
//!   authentication: # UserPassword { username: 'u'; password: EnvironmentSecret { envVariableName: 'P'; }; }#;
//! }
//! ```
//!
//! The extension's `###Elasticsearch` section is still to come (step 7 of
//! `docs/porting-status.md`).
//!
//! Crate layout:
//!
//! - [`connection`] — [`connection::ElasticsearchConnectionGrammar`]
//!   registers as `dsl-connection`'s connection grammar for
//!   `Elasticsearch7ClusterConnection`.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod connection;
