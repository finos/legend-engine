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

//! The section's grammar, `DataQualityParserGrammar`: what it reads, before
//! the walk checks it, as blocks of fields (see
//! [`legend_pure_parser_parser::blocks`]).
//!
//! A validation tree, `$[ my::Class<constraint>{ property, … } ]$`, is read
//! into blocks too: the root's fields are its `class`, its `constraints`
//! and its `subTrees`, a property's its `alias`, `property`, `constraints`,
//! `subType` and `subTrees`. The grammar also reads property parameters and
//! sub-type paths, which the walker leaves out, as they are left out here.

use legend_pure_parser_ast::annotation::{StereotypePtr, TaggedValue};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{
    ReadBlock, ReadField, ReadValue, bare_block, braces, expression_until, fields_until,
    identifier_value, simple, some_values, string_value, typed_island, unended, values,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::separated;
use legend_pure_parser_parser::words::{
    QualifiedName, adjacent, identifier, is_string, is_word, qualified_name, unexpected,
};
use smol_str::SmolStr;

use crate::ast::DataQualityKind;

type R<T> = Result<T, ParseError>;

/// `'''doc''' Keyword <<s>> {t} my::E { field … }`.
pub(crate) struct ReadElement {
    pub kind: DataQualityKind,
    pub documentation: Option<Token>,
    pub stereotypes: Vec<StereotypePtr>,
    pub tagged_values: Vec<TaggedValue>,
    pub package: Option<Package>,
    pub name: SmolStr,
    pub name_source_info: SourceInfo,
    /// The fields; positioned from the documentation, or the keyword, to
    /// `}`.
    pub body: ReadBlock,
}

/// An element: a validation, a relation validation or a relation
/// comparison, which has neither documentation nor annotations.
pub(crate) fn element(ctx: &mut ParserContext<'_>) -> R<ReadElement> {
    let start = ctx.cursor().current_source_info();
    let documentation = is_string(ctx.cursor().peek()).then(|| ctx.cursor().advance().clone());
    let keyword = ctx.cursor().peek().clone();
    let kind = if is_word(&keyword, "DataQualityValidation") {
        DataQualityKind::Validation
    } else if is_word(&keyword, "DataQualityRelationValidation") {
        DataQualityKind::RelationValidation
    } else if is_word(&keyword, "DataQualityRelationComparison") && documentation.is_none() {
        DataQualityKind::RelationComparison
    } else {
        return Err(unexpected(&keyword));
    };
    ctx.cursor().advance();
    let mut stereotypes = Vec::new();
    let mut tagged_values = Vec::new();
    if kind.annotated() {
        if ctx.cursor().check(TokenKind::LessLess) {
            stereotypes = ctx.parse_stereotypes()?;
        }
        if ctx.cursor().check(TokenKind::LBrace) {
            tagged_values = ctx.parse_tagged_values()?;
        }
    }
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    let (_, fields) = match kind {
        DataQualityKind::Validation => braces(ctx, validation_field)?,
        DataQualityKind::RelationValidation => braces(ctx, relation_validation_field)?,
        DataQualityKind::RelationComparison => braces(ctx, comparison_field)?,
    };
    Ok(ReadElement {
        kind,
        documentation,
        stereotypes,
        tagged_values,
        package,
        name,
        name_source_info,
        body: ReadBlock {
            source_info: ctx.cursor().span_since(&start),
            keyword,
            fields,
        },
    })
}

/// A `combinedExpression`, up to the field's `;`.
fn expression(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let expression = expression_until(ctx, TokenKind::Semicolon)?;
    Ok(ReadValue::Expression(Box::new(expression)))
}

/// A persistence strategy, `# Type { … }#`, which an extension reads.
fn island(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    Ok(ReadValue::Island(ctx.read_island()?))
}

/// `id: value`, from the id to the value's end.
fn named(
    ctx: &mut ParserContext<'_>,
    value: impl FnOnce(&mut ParserContext<'_>) -> R<ReadValue>,
) -> R<ReadValue> {
    let id = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let value = value(ctx)?;
    let source_info = ctx.cursor().span_since(&id.source_info);
    Ok(ReadValue::Named(id, Box::new(value), source_info))
}

/// A field whose value is positioned on itself.
fn field(name: &'static str, value: ReadValue, source_info: SourceInfo) -> ReadField {
    ReadField {
        name,
        value,
        source_info,
    }
}

// ── Validations ──────────────────────────────────────────────────────────

fn validation_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "context" => simple(ctx, "context", context),
        "filter" => simple(ctx, "filter", expression),
        "validationTree" => simple(ctx, "validationTree", tree),
        _ => Ok(None),
    }
}

/// `fromDataSpace(my::Space, 'context')` or
/// `fromMappingAndRuntime(my::Mapping, my::Runtime)`: a block whose keyword
/// is the form's, its parts as fields.
fn context(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let keyword = ctx.cursor().peek().clone();
    let data_space = is_word(&keyword, "fromDataSpace");
    if !data_space && !is_word(&keyword, "fromMappingAndRuntime") {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::LParen)?;
    let first = qualified_name(ctx)?;
    ctx.cursor().expect(TokenKind::Comma)?;
    let fields = if data_space {
        let name = ctx.cursor().expect(TokenKind::StringLiteral)?;
        vec![
            field(
                "dataSpace",
                ReadValue::Path(first.clone()),
                first.source_info,
            ),
            field("context", ReadValue::String(name.clone()), name.source_info),
        ]
    } else {
        let runtime = qualified_name(ctx)?;
        vec![
            field("mapping", ReadValue::Path(first.clone()), first.source_info),
            field(
                "runtime",
                ReadValue::Path(runtime.clone()),
                runtime.source_info,
            ),
        ]
    };
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(ReadValue::Block(ReadBlock {
        source_info: ctx.cursor().span_since(&keyword.source_info),
        keyword,
        fields,
    }))
}

/// Two tokens the engine's lexer reads as one, `$[` or `]$`, written
/// together.
fn joined(ctx: &mut ParserContext<'_>, first: TokenKind, second: TokenKind) -> R<Token> {
    let cursor = ctx.cursor();
    let token = cursor.peek().clone();
    if token.kind != first
        || cursor.peek_kind_at(1) != second
        || !adjacent(&token, cursor.peek_at(1))
    {
        return Err(unexpected(&token));
    }
    cursor.advance();
    cursor.advance();
    Ok(token)
}

/// `$[ my::Class<constraint, …>{ … } ]$`.
fn tree(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let open = joined(ctx, TokenKind::Dollar, TokenKind::LBracket)?;
    let class = qualified_name(ctx)?;
    let mut fields = vec![field(
        "class",
        ReadValue::Path(class.clone()),
        class.source_info,
    )];
    if ctx.cursor().check(TokenKind::Less) {
        fields.push(constraints(ctx)?);
    }
    fields.push(graph_definition(ctx)?);
    joined(ctx, TokenKind::RBracket, TokenKind::Dollar)?;
    Ok(ReadValue::Block(ReadBlock {
        source_info: ctx.cursor().span_since(&open.source_info),
        keyword: open,
        fields,
    }))
}

/// `<constraint, …>`: at least one.
fn constraints(ctx: &mut ParserContext<'_>) -> R<ReadField> {
    let open = ctx.cursor().expect(TokenKind::Less)?;
    let names = separated(ctx, identifier_value)?;
    ctx.cursor().expect(TokenKind::Greater)?;
    Ok(field(
        "constraints",
        ReadValue::List(names),
        ctx.cursor().span_since(&open.source_info),
    ))
}

/// `{ path, … }`, possibly empty, as `subTrees`: its paths but those of
/// sub-types (`->subType(@my::Type){ … }`), which the walker leaves out.
fn graph_definition(ctx: &mut ParserContext<'_>) -> R<ReadField> {
    let open = ctx.cursor().expect(TokenKind::LBrace)?;
    let mut paths = Vec::new();
    if !ctx.cursor().check(TokenKind::RBrace) {
        loop {
            if ctx.cursor().check(TokenKind::Arrow) {
                sub_type(ctx)?;
                graph_definition(ctx)?;
            } else {
                paths.push(graph_path(ctx)?);
            }
            if !ctx.cursor().eat(TokenKind::Comma) {
                break;
            }
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(field(
        "subTrees",
        ReadValue::List(paths),
        ctx.cursor().span_since(&open.source_info),
    ))
}

/// `'alias': property<constraint, …>(parameter, …)->subType(@my::Type){ … }`,
/// all but the property optional.
fn graph_path(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let first = ctx.cursor().peek().clone();
    let mut fields = Vec::new();
    if is_string(&first) && ctx.cursor().peek_kind_at(1) == TokenKind::Colon {
        ctx.cursor().advance();
        ctx.cursor().advance();
        fields.push(field(
            "alias",
            ReadValue::String(first.clone()),
            first.source_info.clone(),
        ));
    }
    let property = identifier(ctx)?;
    fields.push(field(
        "property",
        ReadValue::Identifier(property.clone()),
        property.source_info,
    ));
    if ctx.cursor().check(TokenKind::Less) {
        fields.push(constraints(ctx)?);
    }
    if ctx.cursor().check(TokenKind::LParen) {
        parameters(ctx)?;
    }
    if ctx.cursor().check(TokenKind::Arrow) {
        let sub_type = sub_type(ctx)?;
        fields.push(field(
            "subType",
            ReadValue::Path(sub_type.clone()),
            sub_type.source_info,
        ));
    }
    if ctx.cursor().check(TokenKind::LBrace) {
        fields.push(graph_definition(ctx)?);
    }
    Ok(ReadValue::Block(ReadBlock {
        source_info: ctx.cursor().span_since(&first.source_info),
        keyword: first,
        fields,
    }))
}

/// `->subType(@my::Type)`, whose opening is one token of the engine's
/// lexer, written without spaces.
fn sub_type(ctx: &mut ParserContext<'_>) -> R<QualifiedName> {
    let cursor = ctx.cursor();
    let arrow = cursor.peek().clone();
    let opening = [
        TokenKind::Arrow,
        TokenKind::Identifier,
        TokenKind::LParen,
        TokenKind::At,
    ]
    .iter()
    .enumerate()
    .all(|(offset, kind)| cursor.peek_kind_at(offset) == *kind)
        && cursor.peek_at(1).text == "subType"
        && (1..4).all(|offset| adjacent(cursor.peek_at(offset - 1), cursor.peek_at(offset)));
    if !opening {
        return Err(unexpected(&arrow));
    }
    for _ in 0..4 {
        cursor.advance();
    }
    let path = qualified_name(ctx)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(path)
}

/// `(parameter, …)`, possibly empty, which the walker leaves out.
fn parameters(ctx: &mut ParserContext<'_>) -> R<()> {
    ctx.cursor().expect(TokenKind::LParen)?;
    if !ctx.cursor().check(TokenKind::RParen) {
        separated(ctx, parameter)?;
    }
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(())
}

/// A scalar, or `[scalar, …]`, possibly empty.
fn parameter(ctx: &mut ParserContext<'_>) -> R<()> {
    if !ctx.cursor().eat(TokenKind::LBracket) {
        return scalar(ctx);
    }
    if !ctx.cursor().check(TokenKind::RBracket) {
        separated(ctx, scalar)?;
    }
    ctx.cursor().expect(TokenKind::RBracket)?;
    Ok(())
}

/// `%latest`, a literal, a signed number, `$variable` or `my::Enum.VALUE`.
fn scalar(ctx: &mut ParserContext<'_>) -> R<()> {
    let token = ctx.cursor().peek().clone();
    match token.kind {
        // `%latest` or a date; not a strict time.
        TokenKind::DateLiteral if !is_strict_time(&token.text) => {}
        TokenKind::IntegerLiteral
        | TokenKind::FloatLiteral
        | TokenKind::DecimalLiteral
        | TokenKind::True
        | TokenKind::False => {}
        TokenKind::StringLiteral
            if !matches!(
                ctx.cursor().peek_kind_at(1),
                TokenKind::Dot | TokenKind::PathSep
            ) => {}
        TokenKind::Minus | TokenKind::Plus => {
            ctx.cursor().advance();
            let number = ctx.cursor().peek().clone();
            if !matches!(
                number.kind,
                TokenKind::IntegerLiteral | TokenKind::FloatLiteral | TokenKind::DecimalLiteral
            ) {
                return Err(unexpected(&number));
            }
        }
        TokenKind::Dollar => {
            ctx.cursor().advance();
            identifier(ctx)?;
            return Ok(());
        }
        _ => {
            qualified_name(ctx)?;
            ctx.cursor().expect(TokenKind::Dot)?;
            identifier(ctx)?;
            return Ok(());
        }
    }
    ctx.cursor().advance();
    Ok(())
}

/// `%HH:MM…`: the engine lexer's `STRICTTIME`, which a date token here
/// can't be.
fn is_strict_time(text: &str) -> bool {
    let digits = text
        .trim_start_matches('%')
        .trim_start_matches(|c: char| c.is_ascii_digit());
    digits.starts_with(':')
}

// ── Relation validations ─────────────────────────────────────────────────

fn relation_validation_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "query" => simple(ctx, "query", expression),
        "validations" => simple(ctx, "validations", |ctx| {
            some_values(ctx, |ctx| {
                Ok(ReadValue::Block(bare_block(ctx, validation)?))
            })
        }),
        "persistenceStrategy" => simple(ctx, "persistenceStrategy", island),
        "testSuites" => unended(ctx, "testSuites", |ctx| values(ctx, test_suite)),
        _ => Ok(None),
    }
}

/// One of a relation validation's validations.
fn validation(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "name" => simple(ctx, "name", string_value),
        "description" => simple(ctx, "description", string_value),
        "assertion" => simple(ctx, "assertion", expression),
        "type" => simple(ctx, "type", |ctx| {
            let token = ctx.cursor().peek().clone();
            if !is_word(&token, "ROW_LEVEL") && !is_word(&token, "AGGREGATE") {
                return Err(unexpected(&token));
            }
            identifier_value(ctx)
        }),
        _ => Ok(None),
    }
}

// ── Relation comparisons ─────────────────────────────────────────────────

fn comparison_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    let columns = |ctx: &mut ParserContext<'_>| values(ctx, identifier_value);
    match name {
        "source" => simple(ctx, "source", expression),
        "target" => simple(ctx, "target", expression),
        "keys" => simple(ctx, "keys", columns),
        "columnsToCompare" => simple(ctx, "columnsToCompare", columns),
        "additionalColumnsToPersist" => simple(ctx, "additionalColumnsToPersist", columns),
        "strategy" => simple(ctx, "strategy", strategy),
        // A float, which the walker reads as written.
        "expectedMatch" => simple(ctx, "expectedMatch", |ctx| {
            let token = ctx.cursor().expect(TokenKind::FloatLiteral)?;
            Ok(ReadValue::Text(token.text.to_string(), token.source_info))
        }),
        "persistenceStrategy" => simple(ctx, "persistenceStrategy", island),
        "testSuites" => unended(ctx, "testSuites", |ctx| values(ctx, test_suite)),
        _ => Ok(None),
    }
}

/// `MD5Hash`, or `MD5Hash { field … }` with at least one field.
fn strategy(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let keyword = ctx.cursor().peek().clone();
    if !is_word(&keyword, "MD5Hash") {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
    let mut fields = Vec::new();
    if ctx.cursor().eat(TokenKind::LBrace) {
        let mut reader = md5_hash_field;
        fields = fields_until(ctx, &mut reader)?;
        if fields.is_empty() {
            return Err(unexpected(ctx.cursor().peek()));
        }
        ctx.cursor().expect(TokenKind::RBrace)?;
    }
    Ok(ReadValue::Block(ReadBlock {
        source_info: ctx.cursor().span_since(&keyword.source_info),
        keyword,
        fields,
    }))
}

fn md5_hash_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "sourceHashColumn" => simple(ctx, "sourceHashColumn", identifier_value),
        "targetHashColumn" => simple(ctx, "targetHashColumn", identifier_value),
        "aggregatedHash" => simple(ctx, "aggregatedHash", |ctx| {
            let token = ctx.cursor().peek().clone();
            let value = match token.kind {
                TokenKind::True => true,
                TokenKind::False => false,
                _ => return Err(unexpected(&token)),
            };
            ctx.cursor().advance();
            Ok(ReadValue::Boolean(value))
        }),
        _ => Ok(None),
    }
}

// ── Test suites ──────────────────────────────────────────────────────────

/// `id: { data: [ … ] tests: [ … ] }`, its fields without `;`.
fn test_suite(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    named(ctx, |ctx| {
        Ok(ReadValue::Block(bare_block(ctx, test_suite_field)?))
    })
}

fn test_suite_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "data" => unended(ctx, "data", |ctx| values(ctx, store_data)),
        "tests" => unended(ctx, "tests", |ctx| {
            values(ctx, |ctx| {
                named(ctx, |ctx| {
                    Ok(ReadValue::Block(bare_block(ctx, test_field)?))
                })
            })
        }),
        _ => Ok(None),
    }
}

/// `my::Store: Type #{ … }#`, from the store to the data's end.
fn store_data(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let store = qualified_name(ctx)?;
    let start = store.source_info.clone();
    ctx.cursor().expect(TokenKind::Colon)?;
    let data = typed_island(ctx)?;
    Ok(ReadValue::Pair(
        Box::new(ReadValue::Path(store)),
        Box::new(data),
        ctx.cursor().span_since(&start),
    ))
}

/// A test: `asserts: [ id: Type #{ … }#, … ]`.
fn test_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "asserts" => unended(ctx, "asserts", |ctx| {
            values(ctx, |ctx| named(ctx, typed_island))
        }),
        _ => Ok(None),
    }
}
