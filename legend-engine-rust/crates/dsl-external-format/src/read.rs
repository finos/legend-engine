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

//! The section's grammar, `ExternalFormatParserGrammar`: what it reads,
//! before the walk checks it, as blocks of fields (see
//! [`legend_pure_parser_parser::blocks`]). The core parser reads the
//! section's imports first.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{
    ReadBlock, ReadValue, bare_block, braces, identifier_value, path_value, simple, some_values,
    string_value, values,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{is_word, is_word_kind, unexpected};
use smol_str::SmolStr;

use crate::ast::ExternalFormatKind;

type R<T> = Result<T, ParseError>;

/// The grammar's keywords, which a `VALID_STRING` isn't.
const KEYWORDS: [&str; 13] = [
    "SchemaSet",
    "import",
    "format",
    "schemas",
    "id",
    "location",
    "content",
    "Binding",
    "schemaSet",
    "schemaId",
    "contentType",
    "modelIncludes",
    "modelExcludes",
];

/// `Keyword my::E { field … }`.
pub(crate) struct ReadElement {
    pub kind: ExternalFormatKind,
    pub package: Option<Package>,
    pub name: SmolStr,
    pub name_source_info: SourceInfo,
    /// The fields; positioned from the keyword to `}`.
    pub body: ReadBlock,
}

/// A schema set or a binding.
pub(crate) fn element(ctx: &mut ParserContext<'_>) -> R<ReadElement> {
    let keyword = ctx.cursor().peek().clone();
    let kind = if is_word(&keyword, "SchemaSet") {
        ExternalFormatKind::SchemaSet
    } else if is_word(&keyword, "Binding") {
        ExternalFormatKind::Binding
    } else {
        return Err(unexpected(&keyword));
    };
    ctx.cursor().advance();
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    let (_, fields) = match kind {
        ExternalFormatKind::SchemaSet => braces(ctx, schema_set_field)?,
        ExternalFormatKind::Binding => braces(ctx, binding_field)?,
    };
    Ok(ReadElement {
        kind,
        package,
        name,
        name_source_info,
        body: ReadBlock {
            source_info: ctx.cursor().span_since(&keyword.source_info),
            keyword,
            fields,
        },
    })
}

fn schema_set_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        // A `VALID_STRING`: a word but one of the grammar's keywords.
        "format" => simple(ctx, "format", |ctx| {
            let token = ctx.cursor().peek().clone();
            if !is_word_kind(token.kind) || KEYWORDS.contains(&token.text.as_str()) {
                return Err(unexpected(&token));
            }
            identifier_value(ctx)
        }),
        "schemas" => simple(ctx, "schemas", |ctx| {
            some_values(ctx, |ctx| {
                Ok(ReadValue::Block(bare_block(ctx, schema_field)?))
            })
        }),
        _ => Ok(None),
    }
}

fn schema_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "id" => simple(ctx, "id", identifier_value),
        "location" => simple(ctx, "location", string_value),
        "content" => simple(ctx, "content", string_value),
        _ => Ok(None),
    }
}

fn binding_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "schemaSet" => simple(ctx, "schemaSet", path_value),
        "schemaId" => simple(ctx, "schemaId", identifier_value),
        "contentType" => simple(ctx, "contentType", string_value),
        "modelIncludes" => simple(ctx, "modelIncludes", |ctx| values(ctx, path_value)),
        "modelExcludes" => simple(ctx, "modelExcludes", |ctx| values(ctx, path_value)),
        _ => Ok(None),
    }
}
