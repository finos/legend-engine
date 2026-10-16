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

//! `IslandParser` for `#TDS{ … }#`.
//!
//! Tag: `"TDS"` — dispatched by the host parser for an island opening with
//! `#TDS{`. The body is read as written: any text is a TDS literal, as it is
//! for the engine's parser.

use legend_pure_parser_ast::island::IslandContent;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{IslandParser, ParserContext};
use smol_str::SmolStr;

use crate::ast::{TAG, TdsLiteral};

/// Parser for `#TDS{ … }#` islands.
pub struct TdsIslandParser;

impl IslandParser for TdsIslandParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn tag(&self) -> &str {
        TAG
    }

    fn raw_body(&self) -> bool {
        true
    }

    fn parse(&self, ctx: &mut ParserContext<'_>) -> Result<Box<dyn IslandContent>, ParseError> {
        // The whole island, `#TDS{` to `}#`, as the engine positions it.
        let source_info = ctx
            .island_span()
            .cloned()
            .unwrap_or_else(|| ctx.cursor().current_source_info());
        let text = SmolStr::new(ctx.island_text().unwrap_or_default());
        Ok(Box::new(TdsLiteral { text, source_info }))
    }
}

#[linkme::distributed_slice(legend_pure_parser_parser::island::ISLAND_PARSERS)]
static TDS_ISLAND_PARSER: &(dyn IslandParser + Send + Sync) = &TdsIslandParser;

/// Convenience helper — `vec![Box::new(TdsIslandParser)]` for callers that
/// want the TDS plug-in registered.
#[must_use]
pub fn default_island_parsers() -> Vec<Box<dyn IslandParser>> {
    vec![Box::new(TdsIslandParser)]
}
