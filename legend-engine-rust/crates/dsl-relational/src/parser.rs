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

//! `EmbeddedDataParser` for `Relational #{ … }#`, the engine's
//! `RelationalEmbeddedDataParserGrammar`:
//!
//! ```text
//! definition: csvTable* EOF
//! csvTable:   schema DOT table COLON rows SEMI_COLON
//! rows:       STRING ('+' STRING)*
//! ```

use legend_pure_parser_ast::data::EmbeddedData;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{EmbeddedDataParser, ParserContext, string_value};

use crate::ast::{RelationalData, RelationalTable, TYPE_NAME};

/// Parser for `Relational #{ … }#` test data.
pub struct RelationalDataParser;

impl EmbeddedDataParser for RelationalDataParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(&self, ctx: &mut ParserContext<'_>) -> Result<EmbeddedData, ParseError> {
        // The whole embedded data, as the engine positions it.
        let source_info = ctx
            .island_span()
            .cloned()
            .unwrap_or_else(|| ctx.cursor().current_source_info());
        let mut tables = Vec::new();
        while !ctx.cursor().check(TokenKind::Eof) {
            tables.push(table(ctx)?);
        }
        Ok(EmbeddedData::Extension(Box::new(RelationalData {
            tables,
            source_info,
        })))
    }
}

/// `schema.table: 'row\n' + 'row\n';`.
fn table(ctx: &mut ParserContext<'_>) -> Result<RelationalTable, ParseError> {
    let start = ctx.cursor().current_source_info();
    let (schema, _) = ctx.cursor().expect_identifier_or_keyword()?;
    ctx.cursor().expect(TokenKind::Dot)?;
    let (table, _) = ctx.cursor().expect_identifier_or_keyword()?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let mut values = String::new();
    loop {
        let row = ctx.cursor().expect(TokenKind::StringLiteral)?;
        // The engine doubles `\;`'s backslash first, so the escape survives.
        let (value, _) = string_value(&row.text.replace("\\;", "\\\\;")).map_err(|message| {
            ParseError::Unexpected {
                message,
                source_info: row.source_info.clone(),
            }
        })?;
        values.push_str(&value);
        if !ctx.cursor().eat(TokenKind::Plus) {
            break;
        }
    }
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok(RelationalTable {
        schema,
        table,
        values,
        source_info: ctx.cursor().span_since(&start),
    })
}

#[linkme::distributed_slice(legend_pure_parser_parser::embedded_data::EMBEDDED_DATA_PARSERS)]
static RELATIONAL_DATA_PARSER: &(dyn EmbeddedDataParser + Send + Sync) = &RelationalDataParser;
