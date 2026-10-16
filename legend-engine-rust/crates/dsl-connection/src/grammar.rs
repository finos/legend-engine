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

//! Grammars plugged into a connection: its value, chosen by its type (the
//! engine's `ConnectionValueParser`s). Each grammar converts what it read
//! too: it is its own protocol half.
//!
//! The engine core's model connections are this crate's, built in. A
//! store's connections are its crate's, which registers them:
//!
//! ```ignore
//! use legend_pure_dsl_connection::grammar::{ConnectionValueGrammar, CONNECTION_VALUE_GRAMMARS};
//! use linkme::distributed_slice;
//!
//! #[distributed_slice(CONNECTION_VALUE_GRAMMARS)]
//! static RELATIONAL: &(dyn ConnectionValueGrammar + Send + Sync) = &RelationalConnectionGrammar;
//! ```
//!
//! A connection of a type no linked grammar reads is unsupported: an engine
//! extension may read it, and the engine then parses the section.

use std::collections::HashSet;
use std::sync::OnceLock;

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::element_protocol::ConvertError;
use linkme::distributed_slice;

use crate::ast::{ConnectionValue, ConnectionValueBody};
use crate::model::{
    JsonModelConnectionGrammar, ModelChainConnectionGrammar, XmlModelConnectionGrammar,
};

/// The engine core's connection value grammars.
static CORE_CONNECTION_VALUE_GRAMMARS: [&(dyn ConnectionValueGrammar + Send + Sync); 3] = [
    &JsonModelConnectionGrammar,
    &XmlModelConnectionGrammar,
    &ModelChainConnectionGrammar,
];

/// Where a connection value is.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ValueSpan {
    /// Where the engine positions the value and reports what its walkers
    /// find wrong with it: a packageable connection's whole span, an
    /// embedded one's `Type { … }`.
    pub value: SourceInfo,
    /// Its body, from `{` to `}`: the engine parses the body as a text of
    /// its own, starting right after `{`.
    pub body: SourceInfo,
    /// Whether the connection is a runtime's own, `#{ Type { … } }#`.
    pub embedded: bool,
}

/// The grammar of one type of connection value, e.g. `JsonModelConnection`
/// or a store's `RelationalDatabaseConnection`.
///
/// Stateless: the same instance may parse on several threads at once.
pub trait ConnectionValueGrammar {
    /// The type it reads, as written before the connection's path.
    fn type_name(&self) -> &str;

    /// Reads a value, `ctx` holding its body's tokens (between the braces),
    /// then checks it as the engine's walker does. It must read the body
    /// whole.
    ///
    /// # Errors
    ///
    /// The grammar's or the walker's error.
    fn parse(
        &self,
        span: &ValueSpan,
        ctx: &mut ParserContext<'_>,
    ) -> Result<Box<dyn ConnectionValueBody>, ParseError>;

    /// The engine's JSON of a value this grammar read: an object with its
    /// `_type`.
    ///
    /// # Errors
    ///
    /// A conversion failure.
    fn convert(&self, value: &ConnectionValue) -> Result<serde_json::Value, ConvertError>;
}

/// Distributed slice into which each crate owning a connection value
/// grammar registers it. See the module docs.
#[distributed_slice]
pub static CONNECTION_VALUE_GRAMMARS: [&'static (dyn ConnectionValueGrammar + Send + Sync)] = [..];

/// The engine core's connection value grammars and the registered
/// [`CONNECTION_VALUE_GRAMMARS`], validated once per process.
///
/// # Panics
///
/// Panics when two registered grammars share a type: a link-time
/// misconfiguration.
#[must_use]
pub fn connection_value_grammars() -> &'static [&'static (dyn ConnectionValueGrammar + Send + Sync)]
{
    static DISCOVERED: OnceLock<Vec<&'static (dyn ConnectionValueGrammar + Send + Sync)>> =
        OnceLock::new();
    DISCOVERED.get_or_init(|| {
        let mut seen = HashSet::new();
        CORE_CONNECTION_VALUE_GRAMMARS
            .iter()
            .chain(CONNECTION_VALUE_GRAMMARS.iter())
            .copied()
            .inspect(|grammar| {
                let type_name = grammar.type_name();
                assert!(
                    seen.insert(type_name),
                    "two connection grammars are registered for type `{type_name}`"
                );
            })
            .collect()
    })
}

/// The grammar for connection values of type `type_name`.
#[must_use]
pub fn connection_value_grammar(
    type_name: &str,
) -> Option<&'static (dyn ConnectionValueGrammar + Send + Sync)> {
    connection_value_grammars()
        .iter()
        .copied()
        .find(|grammar| grammar.type_name() == type_name)
}

/// The linked connection types, sorted, for messages.
#[must_use]
pub fn linked_connection_value_types() -> String {
    let mut types: Vec<&str> = connection_value_grammars()
        .iter()
        .map(|grammar| grammar.type_name())
        .collect();
    types.sort_unstable();
    types.join(", ")
}
