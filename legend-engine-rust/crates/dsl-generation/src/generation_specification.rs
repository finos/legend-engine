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

//! `###GenerationSpecification`: the engine's
//! `GenerationSpecificationParserGrammar`, then its
//! `GenerationSpecificationParseTreeWalker`, as blocks of fields (see
//! [`legend_pure_parser_parser::blocks`]).

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{
    ReadBlock, ReadField, ReadValue, braces, path_value, simple, string_value, values,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::field_start;
use legend_pure_parser_parser::words::{at_word, expect_word, is_word, unexpected};
use legend_pure_parser_protocol::v1::record::Record;
use smol_str::SmolStr;

type R<T> = Result<T, ParseError>;

/// `GenerationSpecification my::G { field … }`.
pub(crate) struct ReadSpecification {
    pub package: Option<Package>,
    pub name: SmolStr,
    pub name_source_info: SourceInfo,
    /// The fields; positioned from `GenerationSpecification` to `}`.
    pub body: ReadBlock,
}

/// A generation specification.
pub(crate) fn read(ctx: &mut ParserContext<'_>) -> R<ReadSpecification> {
    let keyword = ctx.cursor().peek().clone();
    if !is_word(&keyword, "GenerationSpecification") {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    let (_, fields) = braces(ctx, |ctx: &mut ParserContext<'_>, name: &str| match name {
        "generationNodes" => simple(ctx, "generationNodes", |ctx| values(ctx, node)),
        "fileGenerations" => simple(ctx, "fileGenerations", |ctx| values(ctx, path_value)),
        _ => Ok(None),
    })?;
    Ok(ReadSpecification {
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

/// `{ (id: 'id';)? generationElement: my::E; }`.
fn node(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let open = ctx.cursor().expect(TokenKind::LBrace)?;
    let mut fields = Vec::new();
    if at_word(ctx, "id") {
        let start = ctx.cursor().current_source_info();
        field_start(ctx)?;
        let id = string_value(ctx)?;
        ctx.cursor().expect(TokenKind::Semicolon)?;
        fields.push(ReadField {
            name: "id",
            value: id,
            source_info: ctx.cursor().span_since(&start),
        });
    }
    let start = ctx.cursor().current_source_info();
    expect_word(ctx, "generationElement")?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let element = path_value(ctx)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    fields.push(ReadField {
        name: "generationElement",
        value: element,
        source_info: ctx.cursor().span_since(&start),
    });
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ReadValue::Block(ReadBlock {
        source_info: ctx.cursor().span_since(&open.source_info),
        keyword: open,
        fields,
    }))
}

/// The walker's `visitGenerationSpecification`: the specification's own
/// record, without its package and name.
pub(crate) fn walk(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let nodes = read
        .take_optional("generationNodes", &at)?
        .map(|field| {
            field
                .values()?
                .into_iter()
                .map(|node| self::walk_node(node, &at))
                .collect::<R<Vec<_>>>()
        })
        .transpose()?;
    let file_generations = read
        .take_optional("fileGenerations", &at)?
        .map(|field| {
            field
                .values()?
                .into_iter()
                .map(|value| match value {
                    ReadValue::Path(path) => Ok(Record::untyped()
                        .with("type", "FILE_GENERATION".to_string())
                        .with("path", path.path)
                        .at(&path.source_info)),
                    _ => Err(misread("a file generation", &at)),
                })
                .collect::<R<Vec<_>>>()
        })
        .transpose()?;
    Ok(Record::new("generationSpecification")
        .at(&at)
        .with_some("generationNodes", nodes)
        .with_some("fileGenerations", file_generations))
}

fn misread(what: &str, at: &SourceInfo) -> ParseError {
    ParseError::Unexpected {
        message: format!("the generation specification grammar misread {what}"),
        source_info: at.clone(),
    }
}

/// The walker's `visitGenerationTreeNode`: its id, or its element's path.
fn walk_node(value: ReadValue, at: &SourceInfo) -> R<Record> {
    let ReadValue::Block(mut read) = value else {
        return Err(misread("a node", at));
    };
    let node_at = read.source_info.clone();
    let id = read
        .take_optional("id", &node_at)?
        .map(ReadField::string)
        .transpose()?;
    let element = read.take("generationElement", &node_at)?.path()?;
    Ok(Record::untyped()
        .at(&node_at)
        .with("id", id.unwrap_or_else(|| element.clone()))
        .with("generationElement", element))
}
