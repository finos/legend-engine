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

//! # Legend Pure — Authentication
//!
//! The engine's authentication module (`legend-engine-xt-authentication-grammar`):
//! the authentication specifications some connections hold,
//! `# Type { … }#`, and their secrets.
//!
//! ```pure
//! authentication: # UserPassword {
//!   username: 'user';
//!   password: SystemPropertiesSecret { systemPropertyName: 'my.password'; };
//! }#;
//! ```
//!
//! A connection's grammar reads one with
//! [`grammar::parse_authentication`] and converts it with
//! [`object::Object::to_json`].
//!
//! Crate layout:
//!
//! - [`grammar`] — the plug-in grammars, by type, and
//!   [`grammar::parse_authentication`].
//! - [`specifications`] — the engine's authentication specifications.
//! - [`object`] — what the grammars read: the engine's protocol objects.
//! - `secret` — secrets and AWS credentials.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod grammar;
pub mod object;
mod secret;
pub mod specifications;
