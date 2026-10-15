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

//! `IslandParser` for graph-fetch (`#{ Type { fields } }#`).
//!
//! Tag: `""` (the empty-tag opener `#{ … }#` dispatches here).
//!
//! Follows the graph grammar (`GraphFetchTreeParserGrammar`): a root class
//! followed by a brace-delimited, comma-separated list of property paths
//! and, at the root only, `->subType(@T){…}` paths; each property path has
//! an optional alias, parameters, subtype cast and sub-tree.
//!
//! Plugged into the host parser via
//! [`legend_pure_parser_parser::parse_with_islands`].

use legend_pure_parser_ast::annotation::PackageableElementPtr;
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::island::IslandContent;
use legend_pure_parser_ast::source_info::Spanned;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{IslandParser, ParserContext, split_package_name, unquote_string};
use smol_str::SmolStr;

use crate::ast::{
    GraphFetchParameter, PropertyGraphFetchTree, RootGraphFetchTree, ScalarParameter,
    SubTypeGraphFetchTree, TAG,
};

/// Parser for graph-fetch tree syntax: `#{ Type { field1, field2 { sub } } }#`.
pub struct GraphFetchIslandParser;

impl IslandParser for GraphFetchIslandParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn tag(&self) -> &str {
        TAG
    }

    fn parse(&self, ctx: &mut ParserContext<'_>) -> Result<Box<dyn IslandContent>, ParseError> {
        // The engine's `visitGraphFetchTree` checks the text between `#{`
        // and `}#`, and reports it on the whole island.
        if ctx.island_text() == Some("") {
            let source_info = ctx
                .island_span()
                .cloned()
                .unwrap_or_else(|| ctx.cursor().current_source_info());
            return Err(ParseError::Unexpected {
                message: "Graph fetch tree must not be empty".to_string(),
                source_info,
            });
        }
        let tree = parse_graph_fetch_tree(ctx)?;
        Ok(Box::new(tree))
    }
}

#[linkme::distributed_slice(legend_pure_parser_parser::island::ISLAND_PARSERS)]
static GRAPH_FETCH_PARSER: &(dyn IslandParser + Send + Sync) = &GraphFetchIslandParser;

/// Convenience helper — `vec![Box::new(GraphFetchIslandParser)]` for
/// callers that want the empty-tag default island registered.
#[must_use]
pub fn default_island_parsers() -> Vec<Box<dyn IslandParser>> {
    vec![Box::new(GraphFetchIslandParser)]
}

type Paths = (Vec<PropertyGraphFetchTree>, Vec<SubTypeGraphFetchTree>);

/// `definition: qualifiedName graphDefinition`.
fn parse_graph_fetch_tree(ctx: &mut ParserContext<'_>) -> Result<RootGraphFetchTree, ParseError> {
    let start = ctx.cursor().current_source_info();
    let class = parse_element_pointer(ctx)?;
    let (sub_trees, sub_type_trees) = parse_graph_definition(ctx, true)?;
    Ok(RootGraphFetchTree {
        class,
        sub_trees,
        sub_type_trees,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// A qualified name, positioned on the whole path.
fn parse_element_pointer(ctx: &mut ParserContext<'_>) -> Result<PackageableElementPtr, ParseError> {
    let start = ctx.cursor().current_source_info();
    let path = ctx.parse_package_path()?;
    let (package, name) = split_package_name(&path);
    Ok(PackageableElementPtr {
        package,
        name,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `graphDefinition: { graphPaths }`, where `graphPaths` is one or more
/// comma-separated paths. `->subType(@T){…}` paths are only allowed at the
/// root: the engine rejects them anywhere else, at the enclosing `{`.
fn parse_graph_definition(ctx: &mut ParserContext<'_>, root: bool) -> Result<Paths, ParseError> {
    let open = ctx.cursor().current_source_info();
    ctx.cursor().expect(TokenKind::LBrace)?;
    let mut sub_trees = Vec::new();
    let mut sub_type_trees = Vec::new();
    loop {
        if at_sub_type(ctx) {
            if !root {
                return Err(ParseError::Unexpected {
                    message: "->subType() is supported only at root level".to_string(),
                    source_info: open,
                });
            }
            sub_type_trees.push(parse_sub_type_path(ctx)?);
        } else {
            sub_trees.push(parse_graph_path(ctx)?);
        }
        if !ctx.cursor().eat(TokenKind::Comma) {
            break;
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok((sub_trees, sub_type_trees))
}

/// Whether `->subType(` is next.
fn at_sub_type(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    cursor.check(TokenKind::Arrow)
        && cursor.peek_at(1).text == "subType"
        && cursor.peek_kind_at(2) == TokenKind::LParen
}

/// `subtype: ->subType(@ qualifiedName )`: the class.
fn parse_sub_type_cast(ctx: &mut ParserContext<'_>) -> Result<PackageableElementPtr, ParseError> {
    ctx.cursor().expect(TokenKind::Arrow)?;
    let (_sub_type, _) = ctx.cursor().expect_identifier_or_keyword()?;
    ctx.cursor().expect(TokenKind::LParen)?;
    ctx.cursor().expect(TokenKind::At)?;
    let class = parse_element_pointer(ctx)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(class)
}

/// `subTypeGraphPath: subtype graphDefinition`.
fn parse_sub_type_path(ctx: &mut ParserContext<'_>) -> Result<SubTypeGraphFetchTree, ParseError> {
    let start = ctx.cursor().current_source_info();
    let sub_type_class = parse_sub_type_cast(ctx)?;
    let (sub_trees, _) = parse_graph_definition(ctx, false)?;
    Ok(SubTypeGraphFetchTree {
        sub_type_class,
        sub_trees,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `graphPath: alias? identifier propertyParameters? subtype?
/// graphDefinition?`.
fn parse_graph_path(ctx: &mut ParserContext<'_>) -> Result<PropertyGraphFetchTree, ParseError> {
    let start = ctx.cursor().current_source_info();
    let alias = if ctx.cursor().check(TokenKind::StringLiteral)
        && ctx.cursor().peek_kind_at(1) == TokenKind::Colon
    {
        let tok = ctx.cursor().advance().clone();
        ctx.cursor().advance(); // :
        Some(SmolStr::from(unquote_string(&tok.text)))
    } else {
        None
    };
    let (property, property_source_info) = ctx.cursor().expect_identifier_or_keyword()?;
    let mut parameters = Vec::new();
    if ctx.cursor().eat(TokenKind::LParen) {
        while !ctx.cursor().check(TokenKind::RParen) {
            parameters.push(parse_parameter(ctx)?);
            if !ctx.cursor().eat(TokenKind::Comma) {
                break;
            }
        }
        ctx.cursor().expect(TokenKind::RParen)?;
    }
    let sub_type = if at_sub_type(ctx) {
        Some(parse_sub_type_cast(ctx)?)
    } else {
        None
    };
    let sub_trees = if ctx.cursor().check(TokenKind::LBrace) {
        parse_graph_definition(ctx, false)?.0
    } else {
        Vec::new()
    };
    Ok(PropertyGraphFetchTree {
        property,
        property_source_info,
        parameters,
        alias,
        sub_type,
        sub_trees,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `parameter: scalarParameter | collectionParameter`.
fn parse_parameter(ctx: &mut ParserContext<'_>) -> Result<GraphFetchParameter, ParseError> {
    if !ctx.cursor().check(TokenKind::LBracket) {
        return Ok(GraphFetchParameter::Scalar(parse_scalar_parameter(ctx)?));
    }
    let start = ctx.cursor().current_source_info();
    ctx.cursor().advance();
    let mut values = Vec::new();
    while !ctx.cursor().check(TokenKind::RBracket) {
        values.push(parse_scalar_parameter(ctx)?);
        if !ctx.cursor().eat(TokenKind::Comma) {
            break;
        }
    }
    ctx.cursor().expect(TokenKind::RBracket)?;
    Ok(GraphFetchParameter::Collection {
        values,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `scalarParameter: LATEST_DATE | instanceLiteral | variable |
/// enumReference`.
fn parse_scalar_parameter(ctx: &mut ParserContext<'_>) -> Result<ScalarParameter, ParseError> {
    let start = ctx.cursor().current_source_info();
    let kind = ctx.cursor().peek_kind();
    if kind == TokenKind::Dollar {
        ctx.cursor().advance();
        let (name, source_info) = ctx.cursor().expect_identifier_or_keyword()?;
        return Ok(ScalarParameter::Variable { name, source_info });
    }
    // A quoted name followed by `.` starts an enum reference; otherwise a
    // string is a literal. `true` and `false` are always literals: the
    // graph grammar lexes them as BOOLEAN, never as identifiers.
    let enum_reference = (kind.is_identifier_like() || kind == TokenKind::PathSep)
        && !matches!(kind, TokenKind::True | TokenKind::False)
        && (kind != TokenKind::StringLiteral || ctx.cursor().peek_kind_at(1) == TokenKind::Dot);
    if enum_reference {
        let enumeration = parse_element_pointer(ctx)?;
        ctx.cursor().expect(TokenKind::Dot)?;
        let (value, _) = ctx.cursor().expect_identifier_or_keyword()?;
        return Ok(ScalarParameter::EnumValue {
            enumeration,
            value,
            source_info: ctx.cursor().span_since(&start),
        });
    }
    match ctx.parse_expression()? {
        Expression::Literal(literal) => Ok(ScalarParameter::Literal(literal)),
        other => Err(ParseError::Unexpected {
            message: "Expected a literal, a variable or an enum value".to_string(),
            source_info: other.source_info().clone(),
        }),
    }
}
