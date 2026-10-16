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

//! Authentication specifications, `# Type { … }#` (the engine's
//! `IAuthenticationGrammarParserExtension.parseAuthentication`), each read by
//! the grammar its type names. The engine's authentication module's are this
//! crate's, built in; another module's are its crate's, which registers them
//! into [`AUTHENTICATION_GRAMMARS`]. A type no linked grammar reads is
//! unsupported: an engine extension may read it.

use std::collections::HashSet;
use std::sync::OnceLock;

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{Island, ParserContext};
use linkme::distributed_slice;

use crate::object::Object;
use crate::specifications::CORE_AUTHENTICATIONS;

pub(crate) type R<T> = Result<T, ParseError>;

/// The grammar of one type of authentication specification, e.g.
/// `UserPassword`.
///
/// Stateless: the same instance may parse on several threads at once.
pub trait AuthenticationGrammar {
    /// The type it reads, as written between `#` and `{`.
    fn type_name(&self) -> &str;

    /// Reads a specification, `ctx` holding the island's body, then checks
    /// it as the engine's walker does.
    ///
    /// # Errors
    ///
    /// The grammar's or the walker's error.
    fn parse(&self, ctx: &mut ParserContext<'_>) -> R<Object>;
}

/// Distributed slice into which each crate owning an authentication grammar
/// registers it.
#[distributed_slice]
pub static AUTHENTICATION_GRAMMARS: [&'static (dyn AuthenticationGrammar + Send + Sync)] = [..];

/// The authentication module's grammars and the registered
/// [`AUTHENTICATION_GRAMMARS`], validated once per process.
///
/// # Panics
///
/// Panics when two registered grammars share a type: a link-time
/// misconfiguration.
#[must_use]
pub fn authentication_grammars() -> &'static [&'static (dyn AuthenticationGrammar + Send + Sync)] {
    static DISCOVERED: OnceLock<Vec<&'static (dyn AuthenticationGrammar + Send + Sync)>> =
        OnceLock::new();
    DISCOVERED.get_or_init(|| {
        let mut seen = HashSet::new();
        CORE_AUTHENTICATIONS
            .iter()
            .chain(AUTHENTICATION_GRAMMARS.iter())
            .copied()
            .inspect(|grammar| {
                let type_name = grammar.type_name();
                assert!(
                    seen.insert(type_name),
                    "two authentication grammars are registered for type `{type_name}`"
                );
            })
            .collect()
    })
}

/// An authentication specification, `# Type { … }#`, read by the grammar its
/// type names.
///
/// # Errors
///
/// No type, a type no linked grammar reads (unsupported), or the grammar's
/// error.
pub fn parse_authentication(ctx: &mut ParserContext<'_>, island: &Island) -> R<Object> {
    let type_name = island.tag().trim();
    if type_name.is_empty() {
        return Err(ParseError::Unexpected {
            message: "Missing authentication type.  Expect '# TYPE_HERE { ... }#'".to_string(),
            source_info: island.open_source_info().clone(),
        });
    }
    let Some(grammar) = authentication_grammars()
        .iter()
        .copied()
        .find(|grammar| grammar.type_name() == type_name)
    else {
        let mut linked: Vec<&str> = authentication_grammars()
            .iter()
            .map(|grammar| grammar.type_name())
            .collect();
        linked.sort_unstable();
        return Err(ParseError::Unsupported {
            message: format!(
                "no authentication grammar is linked for '{type_name}'; the linked ones are: {}",
                linked.join(", ")
            ),
            source_info: island.source_info.clone(),
        });
    };
    ctx.parse_island(island, |body| grammar.parse(body))
}

// ── What the grammars share ──────────────────────────────────────────────

/// From `start` to the end of input, as ANTLR spans a rule ending with
/// `EOF`, the end-of-input token's text being `<EOF>`.
pub(crate) fn to_end_of_input(ctx: &mut ParserContext<'_>, start: &SourceInfo) -> SourceInfo {
    let end = ctx.cursor().current_source_info();
    SourceInfo::new(
        start.source.clone(),
        start.start_line,
        start.start_column,
        end.start_line,
        end.start_column + 4,
    )
}
