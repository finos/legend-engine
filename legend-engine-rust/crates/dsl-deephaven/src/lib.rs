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

//! # Legend Pure — Deephaven grammars
//!
//! The engine's Deephaven store extension (`legend-engine-xts-deephaven`):
//! `DeephavenConnection`s, a Deephaven server's URL and authentication
//! (`DeephavenConnectionParserGrammar` and its walker), the engine's
//! `DeephavenConnection`.
//!
//! ```pure
//! ###Connection
//! DeephavenConnection my::Connection
//! {
//!   store: my::Store;
//!   serverUrl: 'http://localhost:10000'
//!   authentication: # PSK { psk: 'key'; }#;
//! }
//! ```
//!
//! The extension's `###Deephaven` section is still to come (step 7 of
//! `docs/porting-status.md`).
//!
//! Crate layout:
//!
//! - [`connection`] — [`connection::DeephavenConnectionGrammar`] registers
//!   as `dsl-connection`'s connection grammar for `DeephavenConnection`.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod connection;
