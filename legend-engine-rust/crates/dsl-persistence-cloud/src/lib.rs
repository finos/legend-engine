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

//! # Legend Pure — Cloud persistence platforms
//!
//! The engine's cloud persistence platform (its `PersistenceCloudParserGrammar`
//! and `PersistenceCloudParseTreeWalker`), a persistence context's platform,
//! registered as `dsl-persistence`'s platform grammar for `AwsGlue`:
//!
//! ```text
//! AwsGlue #{ dataProcessingUnits: 10; }#
//! ```
//!
//! As for the relational target, the engine's walker parses the island's
//! body without its last two characters, as its own source, its rule ending
//! at the end of input: the platform is positioned from its first token to
//! the end of input, whose text, `<EOF>`, ANTLR counts.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

use legend_pure_dsl_persistence::grammar::{
    PERSISTENCE_SPECIFICATION_GRAMMARS, Specification, SpecificationGrammar, SpecificationKind,
};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::required;
use legend_pure_parser_parser::forms::field_start;
use legend_pure_parser_parser::words::{is_word, unexpected};
use legend_pure_parser_protocol::v1::record::{Record, Value};
use linkme::distributed_slice;

type R<T> = Result<T, ParseError>;

/// The platform type this grammar reads.
pub const TYPE_NAME: &str = "AwsGlue";

/// The AWS Glue persistence platform's grammar.
pub struct AwsGluePlatform;

impl SpecificationGrammar for AwsGluePlatform {
    fn kind(&self) -> SpecificationKind {
        SpecificationKind::Platform
    }

    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(&self, specification: &Specification<'_>, ctx: &mut ParserContext<'_>) -> R<Record> {
        let body = specification.body().unwrap_or_default();
        let Some(island) = specification.island.filter(|_| !body.is_empty()) else {
            return Err(ParseError::Unexpected {
                message: format!("Persistence platform '{TYPE_NAME}' must have a non-empty body"),
                source_info: specification.source_info.clone(),
            });
        };
        let (counts, at) = ctx.parse_island_text(island, &body, |ctx| {
            let start = ctx.cursor().current_source_info();
            let mut counts: Vec<Token> = Vec::new();
            while is_word(ctx.cursor().peek(), "dataProcessingUnits") {
                field_start(ctx)?;
                counts.push(ctx.cursor().expect(TokenKind::IntegerLiteral)?);
                ctx.cursor().expect(TokenKind::Semicolon)?;
            }
            if !ctx.cursor().check(TokenKind::Eof) {
                return Err(unexpected(ctx.cursor().peek()));
            }
            // The rule ends at the end of input, which ANTLR counts as
            // `<EOF>`.
            let end = ctx.cursor().current_source_info();
            let at = SourceInfo::new(
                start.source.clone(),
                start.start_line,
                start.start_column,
                end.start_line,
                end.start_column + 4,
            );
            Ok((counts, at))
        })?;
        let count = required(counts, "dataProcessingUnits", &at)?;
        let Ok(count) = count.text.parse::<i32>() else {
            return Err(ParseError::Unsupported {
                message: "the engine fails, without a position, on a count that is no int"
                    .to_string(),
                source_info: count.source_info,
            });
        };
        Ok(Record::new("awsGlue")
            .at(&at)
            .with("dataProcessingUnits", Value::Integer(i64::from(count))))
    }
}

#[distributed_slice(PERSISTENCE_SPECIFICATION_GRAMMARS)]
static AWS_GLUE_PLATFORM: &(dyn SpecificationGrammar + Send + Sync) = &AwsGluePlatform;
