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

//! `SectionParser` for `###Mapping`: the engine's `MappingParserGrammar` and
//! `MappingParseTreeWalker`.
//!
//! ```text
//! mapping:  Mapping name ( include* element* (MappingTests [ test, … ])?
//!           (testSuites: [ suite, … ])? )
//! include:  include type? path ([source -> target, …])?
//! element:  *? target ([id])? (extends [id])? : Type name? { body }
//! test:     name ( (query: expression; | data: [ input, … ]; | assert: 'output';)* )
//! input:    < Type, Format?, source, 'data' + … >
//! suite:    id: { (doc: 'doc'; | function: expression; | tests: [ id: { (doc: 'doc';
//!           | data: [ store: Type #{ … }#, … ]; | asserts: [ id: Type #{ … }#, … ];)* }, … ];)* }
//! ```
//!
//! As in the engine, the section's grammar is read whole first, then each
//! mapping is walked: its includes, its elements' bodies (read by their
//! type's grammar), its legacy tests, then its test suites, whose data and
//! assertions are read then. The section's syntax errors are therefore
//! reported before any error the walk finds.

use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::{missing, optional, required};
use legend_pure_parser_parser::words::{
    Name, QualifiedName, expect_word, identifier, is_string, is_word, qualified_name,
    same_line_gap, unexpected, word,
};
use legend_pure_parser_parser::{
    Block, ParserContext, SectionParser, TypedIsland, raw_string_value, string_value,
};
use smol_str::SmolStr;

use crate::ast::{
    IncludeKind, LegacyTest, Mapping, MappingElement, MappingElementHeader, MappingInclude,
    MappingTest, SECTION_KIND, StoreSubstitution, StoreTestData, TestInput, TestSuite,
};
use crate::grammar::{
    ElementSpan, linked_mapping_element_types, linked_test_input_types, mapping_element_grammar,
    test_input_grammar,
};

type R<T> = Result<T, ParseError>;

/// The engine core's mapping include type: `include mapping my::M`, or
/// `include my::M`.
const MAPPING_INCLUDE: &str = "mapping";

/// The engine's data space extension's include type:
/// `include dataspace my::DataSpace`.
const DATA_SPACE_INCLUDE: &str = "dataspace";

/// Parser for `###Mapping` sections.
pub struct MappingSectionParser;

impl SectionParser for MappingSectionParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        let mut read = Vec::new();
        while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
            match read_mapping(ctx) {
                Ok(mapping) => read.push(mapping),
                Err(error) => {
                    errors.push(error);
                    return Vec::new();
                }
            }
        }
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        for mapping in read {
            match walk_mapping(ctx, mapping) {
                Ok(mapping) => elements.push(Box::new(mapping)),
                // The engine reports the first error.
                Err(error) => {
                    errors.push(error);
                    break;
                }
            }
        }
        elements
    }
}

// ── The grammar: the section read, its parts not parsed yet ─────────────

/// A mapping as its grammar reads it.
struct ReadMapping {
    package: Option<Package>,
    name: SpannedString,
    includes: Vec<ReadInclude>,
    elements: Vec<ReadElement>,
    legacy_tests: Vec<ReadLegacyTest>,
    test_suites: Option<Vec<ReadSuite>>,
    source_info: SourceInfo,
}

struct ReadInclude {
    /// The type the engine's lexer reads with `include`: `include mapping`.
    include_type: Option<Name>,
    path: QualifiedName,
    substitutions: Vec<StoreSubstitution>,
    source_info: SourceInfo,
}

struct ReadElement {
    header: MappingElementHeader,
    body: Block,
    source_info: SourceInfo,
}

struct ReadLegacyTest {
    name: SmolStr,
    queries: Vec<Expression>,
    data: Vec<Vec<TestInput>>,
    asserts: Vec<ReadAssert>,
    source_info: SourceInfo,
}

/// `assert: …;`: a string, or an expression the walker rejects.
struct ReadAssert {
    /// The string as written between its quotes; `None` for an expression.
    expected: Option<String>,
    source_info: SourceInfo,
}

struct ReadSuite {
    id: Name,
    docs: Vec<Token>,
    functions: Vec<Expression>,
    tests: Vec<Vec<ReadTest>>,
    source_info: SourceInfo,
}

struct ReadTest {
    id: Name,
    docs: Vec<Token>,
    asserts: Vec<Vec<(Name, TypedIsland)>>,
    data: Vec<Vec<ReadStoreData>>,
    source_info: SourceInfo,
}

struct ReadStoreData {
    store: QualifiedName,
    data: TypedIsland,
    source_info: SourceInfo,
}

fn read_mapping(ctx: &mut ParserContext<'_>) -> R<ReadMapping> {
    let start = ctx.cursor().current_source_info();
    expect_word(ctx, "Mapping")?;
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    ctx.cursor().expect(TokenKind::LParen)?;
    let mut includes = Vec::new();
    while at_include(ctx) {
        includes.push(read_include(ctx)?);
    }
    let mut elements = Vec::new();
    while !ctx.cursor().check(TokenKind::RParen) && !at_legacy_tests(ctx) && !at_test_suites(ctx) {
        elements.push(read_element(ctx)?);
    }
    let legacy_tests = if at_legacy_tests(ctx) {
        read_legacy_tests(ctx)?
    } else {
        Vec::new()
    };
    let test_suites = if at_test_suites(ctx) {
        Some(read_test_suites(ctx)?)
    } else {
        None
    };
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(ReadMapping {
        package,
        name: SpannedString {
            value: name,
            source_info: name_source_info,
        },
        includes,
        elements,
        legacy_tests,
        test_suites,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `include` then a name: an include rather than an element of a class
/// named `include`.
fn at_include(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    let next = cursor.peek_at(1);
    is_word(cursor.peek(), "include") && (next.kind.is_identifier_like() || is_string(next))
}

/// `MappingTests [ ]` or `MappingTests [ name (`: legacy tests rather than
/// an element of a class named `MappingTests`.
fn at_legacy_tests(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    is_word(cursor.peek(), "MappingTests")
        && cursor.peek_kind_at(1) == TokenKind::LBracket
        && (cursor.peek_kind_at(2) == TokenKind::RBracket
            || cursor.peek_kind_at(3) == TokenKind::LParen)
}

/// `testSuites: [`.
fn at_test_suites(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    is_word(cursor.peek(), "testSuites")
        && cursor.peek_kind_at(1) == TokenKind::Colon
        && cursor.peek_kind_at(2) == TokenKind::LBracket
}

/// `include my::M`, `include type my::M`, then `[source -> target, …]`.
///
/// The engine's lexer reads `include `, a lowercase word and a space as one
/// token (`INCLUDETYPE`): the word is the type of what is included. A space
/// ending a line is not seen here: the word is then taken as the path.
fn read_include(ctx: &mut ParserContext<'_>) -> R<ReadInclude> {
    let start = ctx.cursor().current_source_info();
    let include = ctx.cursor().advance().clone();
    let include_type = {
        let cursor = ctx.cursor();
        let (word, next) = (cursor.peek(), cursor.peek_at(1));
        (same_line_gap(&include, word) == Some(1)
            && !word.text.is_empty()
            && word.text.bytes().all(|b| b.is_ascii_lowercase())
            && same_line_gap(word, next).is_some_and(|gap| gap > 0))
        .then(|| Name {
            text: word.text.clone(),
            value: word.text.clone(),
            source_info: word.source_info.clone(),
        })
    };
    if include_type.is_some() {
        ctx.cursor().advance();
    }
    let path = qualified_name(ctx)?;
    let mut substitutions = Vec::new();
    if ctx.cursor().eat(TokenKind::LBracket) {
        substitutions = separated(ctx, TokenKind::RBracket, |ctx| {
            let source = qualified_name(ctx)?;
            ctx.cursor().expect(TokenKind::Arrow)?;
            let target = qualified_name(ctx)?;
            Ok(StoreSubstitution {
                source: source.path,
                target: target.path,
            })
        })?;
        ctx.cursor().expect(TokenKind::RBracket)?;
    }
    Ok(ReadInclude {
        include_type,
        path,
        substitutions,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `*? target ([id])? (extends [id])? : Type name? { body }`.
fn read_element(ctx: &mut ParserContext<'_>) -> R<ReadElement> {
    let start = ctx.cursor().current_source_info();
    let root = ctx.cursor().eat(TokenKind::Star);
    let target = qualified_name(ctx)?;
    let id = if ctx.cursor().eat(TokenKind::LBracket) {
        let id = word(ctx)?;
        ctx.cursor().expect(TokenKind::RBracket)?;
        Some(id)
    } else {
        None
    };
    let extends = if is_word(ctx.cursor().peek(), "extends") {
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::LBracket)?;
        let id = word(ctx)?;
        ctx.cursor().expect(TokenKind::RBracket)?;
        Some(id)
    } else {
        None
    };
    ctx.cursor().expect(TokenKind::Colon)?;
    let type_name = identifier(ctx)?;
    let name = if ctx.cursor().check(TokenKind::LBrace) {
        None
    } else {
        Some(word(ctx)?)
    };
    let body = ctx.read_block()?;
    Ok(ReadElement {
        header: MappingElementHeader {
            root,
            target,
            id,
            extends,
            type_name,
            name,
        },
        body,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `MappingTests [ test, … ]`.
fn read_legacy_tests(ctx: &mut ParserContext<'_>) -> R<Vec<ReadLegacyTest>> {
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::LBracket)?;
    let tests = separated(ctx, TokenKind::RBracket, read_legacy_test)?;
    ctx.cursor().expect(TokenKind::RBracket)?;
    Ok(tests)
}

/// `name ( (query: …; | data: […]; | assert: …;)* )`.
fn read_legacy_test(ctx: &mut ParserContext<'_>) -> R<ReadLegacyTest> {
    let start = ctx.cursor().current_source_info();
    let name = identifier(ctx)?.text;
    ctx.cursor().expect(TokenKind::LParen)?;
    let (mut queries, mut data, mut asserts) = (Vec::new(), Vec::new(), Vec::new());
    while !ctx.cursor().check(TokenKind::RParen) {
        let field = ctx.cursor().peek().clone();
        if is_word(&field, "query") {
            field_value(ctx, |ctx| {
                queries.push(expression(ctx)?);
                Ok(())
            })?;
        } else if is_word(&field, "data") {
            field_value(ctx, |ctx| {
                ctx.cursor().expect(TokenKind::LBracket)?;
                data.push(separated(ctx, TokenKind::RBracket, read_test_input)?);
                ctx.cursor().expect(TokenKind::RBracket)?;
                Ok(())
            })?;
        } else if is_word(&field, "assert") {
            // A string, if it is the whole value; else an expression.
            let mut expected = None;
            field_value(ctx, |ctx| {
                let cursor = ctx.cursor();
                if is_string(cursor.peek()) && cursor.peek_kind_at(1) == TokenKind::Semicolon {
                    expected = Some(raw_string_value(&cursor.advance().text));
                } else {
                    expression(ctx)?;
                }
                Ok(())
            })?;
            asserts.push(ReadAssert {
                expected,
                source_info: ctx.cursor().span_since(&field.source_info),
            });
        } else {
            return Err(unexpected(&field));
        }
    }
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(ReadLegacyTest {
        name,
        queries,
        data,
        asserts,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `< Type, Format?, source, 'data' + … >`: the format is there when the
/// strings don't follow the second item.
fn read_test_input(ctx: &mut ParserContext<'_>) -> R<TestInput> {
    let start = ctx.cursor().current_source_info();
    ctx.cursor().expect(TokenKind::Less)?;
    let type_name = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Comma)?;
    let first = qualified_name(ctx)?;
    ctx.cursor().expect(TokenKind::Comma)?;
    let (format, source) = if at_input_data(ctx) {
        (None, first)
    } else if first.has_package {
        return Err(unexpected(ctx.cursor().peek()));
    } else {
        let format = Name {
            value: SmolStr::new(&first.path),
            text: SmolStr::new(&first.text),
            source_info: first.source_info,
        };
        let source = qualified_name(ctx)?;
        ctx.cursor().expect(TokenKind::Comma)?;
        (Some(format), source)
    };
    let mut data = vec![string(ctx)?];
    while ctx.cursor().eat(TokenKind::Plus) {
        data.push(string(ctx)?);
    }
    ctx.cursor().expect(TokenKind::Greater)?;
    Ok(TestInput {
        type_name,
        format,
        source,
        data,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// Whether an input's data, `'…' + '…'` then `>`, is next.
fn at_input_data(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    if !is_string(cursor.peek()) {
        return false;
    }
    let mut next = 1;
    while cursor.peek_kind_at(next) == TokenKind::Plus && is_string(cursor.peek_at(next + 1)) {
        next += 2;
    }
    cursor.peek_kind_at(next) == TokenKind::Greater
}

/// A `STRING`, as written.
fn string(ctx: &mut ParserContext<'_>) -> R<SmolStr> {
    let token = ctx.cursor().peek().clone();
    if !is_string(&token) {
        return Err(unexpected(&token));
    }
    ctx.cursor().advance();
    Ok(token.text)
}

/// `testSuites: [ suite, … ]`.
fn read_test_suites(ctx: &mut ParserContext<'_>) -> R<Vec<ReadSuite>> {
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::Colon)?;
    ctx.cursor().expect(TokenKind::LBracket)?;
    let suites = separated(ctx, TokenKind::RBracket, read_suite)?;
    ctx.cursor().expect(TokenKind::RBracket)?;
    Ok(suites)
}

/// `id: { (doc: …; | function: …; | tests: […];)* }`.
fn read_suite(ctx: &mut ParserContext<'_>) -> R<ReadSuite> {
    let start = ctx.cursor().current_source_info();
    let id = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    ctx.cursor().expect(TokenKind::LBrace)?;
    let (mut docs, mut functions, mut tests) = (Vec::new(), Vec::new(), Vec::new());
    while !ctx.cursor().check(TokenKind::RBrace) {
        let field = ctx.cursor().peek().clone();
        if is_word(&field, "doc") {
            field_value(ctx, |ctx| {
                docs.push(doc(ctx)?);
                Ok(())
            })?;
        } else if is_word(&field, "function") {
            field_value(ctx, |ctx| {
                functions.push(expression(ctx)?);
                Ok(())
            })?;
        } else if is_word(&field, "tests") {
            field_value(ctx, |ctx| {
                ctx.cursor().expect(TokenKind::LBracket)?;
                tests.push(separated(ctx, TokenKind::RBracket, read_test)?);
                ctx.cursor().expect(TokenKind::RBracket)?;
                Ok(())
            })?;
        } else {
            return Err(unexpected(&field));
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ReadSuite {
        id,
        docs,
        functions,
        tests,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `id: { (doc: …; | asserts: […]; | data: […];)* }`.
fn read_test(ctx: &mut ParserContext<'_>) -> R<ReadTest> {
    let start = ctx.cursor().current_source_info();
    let id = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    ctx.cursor().expect(TokenKind::LBrace)?;
    let (mut docs, mut asserts, mut data) = (Vec::new(), Vec::new(), Vec::new());
    while !ctx.cursor().check(TokenKind::RBrace) {
        let field = ctx.cursor().peek().clone();
        if is_word(&field, "doc") {
            field_value(ctx, |ctx| {
                docs.push(doc(ctx)?);
                Ok(())
            })?;
        } else if is_word(&field, "asserts") {
            field_value(ctx, |ctx| {
                ctx.cursor().expect(TokenKind::LBracket)?;
                asserts.push(separated(ctx, TokenKind::RBracket, |ctx| {
                    let id = identifier(ctx)?;
                    ctx.cursor().expect(TokenKind::Colon)?;
                    Ok((id, ctx.read_typed_island()?))
                })?);
                ctx.cursor().expect(TokenKind::RBracket)?;
                Ok(())
            })?;
        } else if is_word(&field, "data") {
            field_value(ctx, |ctx| {
                ctx.cursor().expect(TokenKind::LBracket)?;
                data.push(separated(ctx, TokenKind::RBracket, |ctx| {
                    let start = ctx.cursor().current_source_info();
                    let store = qualified_name(ctx)?;
                    ctx.cursor().expect(TokenKind::Colon)?;
                    let data = ctx.read_typed_island()?;
                    Ok(ReadStoreData {
                        store,
                        data,
                        source_info: ctx.cursor().span_since(&start),
                    })
                })?);
                ctx.cursor().expect(TokenKind::RBracket)?;
                Ok(())
            })?;
        } else {
            return Err(unexpected(&field));
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ReadTest {
        id,
        docs,
        asserts,
        data,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `doc: 'text'`'s string.
fn doc(ctx: &mut ParserContext<'_>) -> R<Token> {
    let token = ctx.cursor().peek().clone();
    if !is_string(&token) {
        return Err(unexpected(&token));
    }
    ctx.cursor().advance();
    Ok(token)
}

/// A field's value, `combinedExpression`: an expression before the field's
/// `;`, which a lambda's body doesn't take.
fn expression(ctx: &mut ParserContext<'_>) -> R<Expression> {
    let value = ctx.read_until(TokenKind::Semicolon)?;
    expression_block(ctx, &value)
}

/// An expression read as a block, which it must be whole.
// `ParserContext::parse_expression` itself is not general enough over the
// block's context lifetime, so the closure stays.
#[allow(clippy::redundant_closure_for_method_calls)]
pub(crate) fn expression_block(ctx: &mut ParserContext<'_>, block: &Block) -> R<Expression> {
    ctx.parse_block(block, |block| block.parse_expression())
}

/// `field: value;`, the field's name next.
fn field_value(
    ctx: &mut ParserContext<'_>,
    value: impl FnOnce(&mut ParserContext<'_>) -> R<()>,
) -> R<()> {
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::Colon)?;
    value(ctx)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok(())
}

/// `item (, item)*`, or nothing when `close` is next; `close` is left.
pub(crate) fn separated<T>(
    ctx: &mut ParserContext<'_>,
    close: TokenKind,
    mut item: impl FnMut(&mut ParserContext<'_>) -> R<T>,
) -> R<Vec<T>> {
    let mut items = Vec::new();
    if ctx.cursor().check(close) {
        return Ok(items);
    }
    loop {
        items.push(item(ctx)?);
        if !ctx.cursor().eat(TokenKind::Comma) {
            return Ok(items);
        }
    }
}

// ── The walk: each mapping's parts, in the engine walker's order ────────

fn walk_mapping(ctx: &mut ParserContext<'_>, read: ReadMapping) -> R<Mapping> {
    let includes = read
        .includes
        .into_iter()
        .map(walk_include)
        .collect::<R<_>>()?;
    let elements = read
        .elements
        .into_iter()
        .map(|element| walk_element(ctx, element))
        .collect::<R<_>>()?;
    let legacy_tests = read
        .legacy_tests
        .into_iter()
        .map(walk_legacy_test)
        .collect::<R<_>>()?;
    let test_suites = read
        .test_suites
        .map(|suites| {
            suites
                .into_iter()
                .map(|suite| walk_suite(ctx, suite, &read.source_info))
                .collect::<R<Vec<_>>>()
        })
        .transpose()?;
    Ok(Mapping {
        package: read.package,
        name: read.name,
        includes,
        elements,
        legacy_tests,
        test_suites,
        source_info: read.source_info,
    })
}

/// An include of a mapping or of a data space; any other type is another
/// engine extension's.
fn walk_include(read: ReadInclude) -> R<MappingInclude> {
    let kind = match read.include_type.as_ref().map(|name| name.text.as_str()) {
        None | Some(MAPPING_INCLUDE) => IncludeKind::Mapping,
        Some(DATA_SPACE_INCLUDE) => IncludeKind::DataSpace,
        Some(other) => {
            return Err(ParseError::Unsupported {
                message: format!(
                    "no mapping include grammar is linked for '{other}'; the linked ones \
                     are: {DATA_SPACE_INCLUDE}, {MAPPING_INCLUDE}"
                ),
                source_info: read.source_info,
            });
        }
    };
    Ok(MappingInclude {
        kind,
        path: read.path.path,
        substitutions: read.substitutions,
        source_info: read.source_info,
    })
}

/// An element's body, read by its type's grammar.
fn walk_element(ctx: &mut ParserContext<'_>, read: ReadElement) -> R<MappingElement> {
    let type_name = &read.header.type_name.text;
    let Some(grammar) = mapping_element_grammar(type_name) else {
        return Err(ParseError::Unsupported {
            message: format!(
                "no mapping element grammar is linked for '{type_name}'; the linked ones are: {}",
                linked_mapping_element_types()
            ),
            source_info: read.source_info,
        });
    };
    let span = ElementSpan {
        element: read.source_info.clone(),
        body: read.body.source_info.clone(),
    };
    let body = ctx.parse_block(&read.body, |body| grammar.parse(&read.header, &span, body))?;
    Ok(MappingElement {
        header: read.header,
        body,
        source_info: read.source_info,
    })
}

/// A legacy test (the walker's `DEPRECATED_visitMappingTest`).
fn walk_legacy_test(read: ReadLegacyTest) -> R<LegacyTest> {
    let source_info = read.source_info;
    let query = required(read.queries, "query", &source_info)?;
    let inputs = required(read.data, "data", &source_info)?;
    for input in &inputs {
        let Some(grammar) = test_input_grammar(&input.type_name.text) else {
            return Err(ParseError::Unsupported {
                message: format!(
                    "no mapping test input grammar is linked for '{}'; the linked ones are: {}",
                    input.type_name.text,
                    linked_test_input_types()
                ),
                source_info: input.source_info.clone(),
            });
        };
        grammar.check(input)?;
    }
    let assert = required(read.asserts, "assert", &source_info)?;
    let Some(expected_output) = assert.expected else {
        return Err(ParseError::Unexpected {
            message: "Mapping test currently only support string assertion".to_string(),
            source_info: assert.source_info,
        });
    };
    Ok(LegacyTest {
        name: read.name,
        query,
        inputs,
        expected_output,
        assert_source_info: assert.source_info,
        source_info,
    })
}

/// A test suite (the walker's `visitMappingTestSuite`). A repeated `doc` is
/// reported at the mapping, as the walker reports it.
fn walk_suite(
    ctx: &mut ParserContext<'_>,
    read: ReadSuite,
    mapping_source_info: &SourceInfo,
) -> R<TestSuite> {
    let source_info = read.source_info;
    let Some(function) = optional(read.functions, "function", &source_info)? else {
        return Err(ParseError::Unexpected {
            message: "Mapping Test Suite requires a query function".to_string(),
            source_info,
        });
    };
    let tests = required(read.tests, "tests", &source_info)?
        .into_iter()
        .map(|test| walk_test(ctx, test))
        .collect::<R<_>>()?;
    let doc = optional(read.docs, "doc", mapping_source_info)?
        .map(|doc| doc_value(&doc))
        .transpose()?;
    Ok(TestSuite {
        id: read.id.value,
        doc,
        function,
        tests,
        source_info,
    })
}

/// A test (the walker's `visitMappingTests`): its assertions, then its data.
fn walk_test(ctx: &mut ParserContext<'_>, read: ReadTest) -> R<MappingTest> {
    let source_info = read.source_info;
    let doc = optional(read.docs, "doc", &source_info)?
        .map(|doc| doc_value(&doc))
        .transpose()?;
    let asserts = required(read.asserts, "asserts", &source_info)?;
    if asserts.is_empty() {
        return Err(missing("asserts", &source_info));
    }
    let assertions = asserts
        .into_iter()
        .map(|(id, island)| Ok((id.value, ctx.parse_test_assertion(&island)?)))
        .collect::<R<_>>()?;
    let store_data = required(read.data, "data", &source_info)?
        .into_iter()
        .map(|data| {
            Ok(StoreTestData {
                // The walker reads a store without a package as written.
                store: if data.store.has_package {
                    data.store.path
                } else {
                    data.store.text
                },
                store_source_info: data.store.source_info,
                data: ctx.parse_embedded_data_island(&data.data)?,
                source_info: data.source_info,
            })
        })
        .collect::<R<_>>()?;
    Ok(MappingTest {
        id: read.id.value,
        doc,
        assertions,
        store_data,
        source_info,
    })
}

/// A `doc` string's value, unescaped.
fn doc_value(token: &Token) -> R<String> {
    string_value(&token.text)
        .map(|(value, _)| value)
        .map_err(|message| ParseError::Unexpected {
            message,
            source_info: token.source_info.clone(),
        })
}

#[linkme::distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static MAPPING_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &MappingSectionParser;
