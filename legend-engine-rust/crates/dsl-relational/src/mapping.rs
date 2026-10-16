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

//! Relational mapping elements: `my::Person: Relational { … }` and
//! `my::Assoc: Relational { AssociationMapping ( … ) }` (the relational
//! store's `RelationalParserGrammar` `mapping` rule and its
//! `RelationalParseTreeWalker`), the engine's `RootRelationalClassMapping`
//! and `RelationalAssociationMapping`.
//!
//! ```text
//! mapping:       AssociationMapping ( property, … ) | classMapping
//! classMapping:  ~filter …? ~distinct? ~groupBy(…)? ~primaryKey(…)? ~mainTable [db]scope?
//!                (property (, property)*)?
//! property:      scope([db]scope?) ( single, … ) | single
//! single:        + name: Type[m] relational
//!              | name ([source (, target)?])? (relational | ( embedded ) (Otherwise(…))? | ( ) Inline[id])
//! relational:    : (EnumerationMapping id: | Binding my::Binding:)? operation
//! ```
//!
//! As the walker does, the operations of property mappings outside a
//! `scope(…)` have no database, and a class mapping's primary key and group
//! by are in its filter's database; an embedded mapping's primary key is in
//! none. A property mapping's source is the first of its two ids, else the
//! class mapping's id; its target, its last id.

use std::any::Any;

use legend_pure_dsl_mapping::ast::{
    MappingElement, MappingElementBody, MappingElementHeader, TestInput,
};
use legend_pure_dsl_mapping::grammar::{
    ElementSpan, MAPPING_ELEMENT_GRAMMARS, MappingElementGrammar, MappingElementKind,
    TEST_INPUT_GRAMMARS, TestInputGrammar,
};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{Name, at_word, qualified_name, unexpected};
use legend_pure_parser_parser::{ParserContext, string_value};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;
use serde::Serialize;
use serde_json::Value;
use serde_json::value::{RawValue, to_raw_value};
use smol_str::SmolStr;

use crate::ast::{JoinPointer, Operation, TablePtr};
use crate::database_protocol::{FilterMappingJson, OperationJson, TablePtrJson};
use crate::operation::{Scope, join_sequence, operation, scoped_table_ptr};
use crate::tokens::{
    at_command, database_pointer, eat_command, identifier, is_word, relational_identifier,
};

/// The element type this grammar reads.
pub const TYPE_NAME: &str = "Relational";

/// A relational mapping element's body.
#[derive(Debug, Clone, PartialEq)]
pub enum RelationalMappingBody {
    /// A class mapping.
    Class(Box<RelationalClassMapping>),
    /// `AssociationMapping ( … )`.
    Association(Vec<PropertyMapping>),
}

/// A class mapping's body.
#[derive(Debug, Clone, PartialEq)]
pub struct RelationalClassMapping {
    /// `~filter [db] (joins | [db])? Filter`.
    pub filter: Option<FilterMapping>,
    /// `~distinct`.
    pub distinct: bool,
    /// `~groupBy( … )`.
    pub group_by: Vec<Operation>,
    /// `~primaryKey( … )`.
    pub primary_key: Vec<Operation>,
    /// `~mainTable [db] table`.
    pub main_table: Option<TablePtr>,
    /// The property mappings.
    pub property_mappings: Vec<PropertyMapping>,
}

/// `~filter …`: a filter of the store, maybe through joins.
#[derive(Debug, Clone, PartialEq)]
pub struct FilterMapping {
    /// The filter's database: the last pointer's.
    pub database: String,
    /// The filter, unquoted.
    pub name: SmolStr,
    /// The joins to the filter's table.
    pub joins: Vec<JoinPointer>,
    /// From `~filter` to the filter's name.
    pub source_info: SourceInfo,
}

/// The property a property mapping maps.
#[derive(Debug, Clone, PartialEq)]
pub struct PropertyPointer {
    /// The class: the class mapping's, for a root class mapping's own
    /// properties.
    pub class: Option<String>,
    /// The property, unquoted.
    pub property: SmolStr,
    /// The property's name.
    pub source_info: SourceInfo,
}

/// A property mapping.
#[derive(Debug, Clone, PartialEq)]
pub enum PropertyMapping {
    /// `name: operation`: the engine's `RelationalPropertyMapping`.
    Relational(RelationalPropertyMapping),
    /// `name ( … )`, maybe `Otherwise ( … )`: the engine's
    /// `EmbeddedRelationalPropertyMapping` or
    /// `OtherwiseEmbeddedRelationalPropertyMapping`.
    Embedded {
        /// The property.
        property: PropertyPointer,
        /// The target id, as written.
        target: Option<SmolStr>,
        /// The embedded class mapping.
        class_mapping: EmbeddedClassMapping,
        /// `Otherwise ([target]: joins)`: the mapping when there is no
        /// embedded value, and its span, from `Otherwise`.
        otherwise: Option<(Box<RelationalPropertyMapping>, SourceInfo)>,
        /// From `(` to `)`.
        source_info: SourceInfo,
    },
    /// `name ( ) Inline[id]`: the engine's `InlineEmbeddedPropertyMapping`.
    Inline {
        /// The property.
        property: PropertyPointer,
        /// The target id, as written.
        target: Option<SmolStr>,
        /// The class mapping inlined, unquoted.
        set_implementation_id: SmolStr,
        /// From `(` to `]`.
        source_info: SourceInfo,
    },
}

/// `name: operation`.
#[derive(Debug, Clone, PartialEq)]
pub struct RelationalPropertyMapping {
    /// The property.
    pub property: PropertyPointer,
    /// The source class mapping's id.
    pub source: Option<SmolStr>,
    /// The target class mapping's id.
    pub target: Option<SmolStr>,
    /// `+name: Type[m]`: the type, its multiplicity and their span, from
    /// `:` to `]`.
    pub local: Option<(String, u32, Option<u32>, SourceInfo)>,
    /// `EnumerationMapping id:`, unquoted.
    pub enumeration_mapping: Option<SmolStr>,
    /// `Binding my::Binding:`.
    pub binding: Option<String>,
    /// The value.
    pub operation: Operation,
    /// From `:` to the end of the value.
    pub source_info: SourceInfo,
}

/// An embedded class mapping: its primary key and property mappings.
#[derive(Debug, Clone, PartialEq)]
pub struct EmbeddedClassMapping {
    /// `~primaryKey( … )`.
    pub primary_key: Vec<Operation>,
    /// The property mappings.
    pub property_mappings: Vec<PropertyMapping>,
    /// From `(` to `)`, or to the `Otherwise` part's `)`.
    pub source_info: SourceInfo,
}

impl MappingElementBody for RelationalMappingBody {
    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn MappingElementBody> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn MappingElementBody) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}

/// The relational mapping element grammar.
pub struct RelationalMappingGrammar;

impl MappingElementGrammar for RelationalMappingGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(
        &self,
        header: &MappingElementHeader,
        _span: &ElementSpan,
        ctx: &mut ParserContext<'_>,
    ) -> Result<Box<dyn MappingElementBody>, ParseError> {
        if at_word(ctx, "AssociationMapping") && ctx.cursor().peek_kind_at(1) == TokenKind::LParen {
            ctx.cursor().advance();
            ctx.cursor().advance();
            let mappings = property_mappings(ctx, TokenKind::RParen, None, None)?;
            if mappings.is_empty() {
                return Err(unexpected(ctx.cursor().peek()));
            }
            ctx.cursor().expect(TokenKind::RParen)?;
            return Ok(Box::new(RelationalMappingBody::Association(mappings)));
        }
        let id = header.id.as_ref().map(|id| id.text.clone());
        Ok(Box::new(RelationalMappingBody::Class(Box::new(
            class_mapping(ctx, id.as_ref(), &header.target.path)?,
        ))))
    }

    fn convert(
        &self,
        element: &MappingElement,
    ) -> Result<(MappingElementKind, Box<RawValue>), ConvertError> {
        let (kind, json) = element_json(element)?;
        Ok((kind, to_raw_value(&json)?))
    }

    fn convert_into(
        &self,
        element: &MappingElement,
        out: &mut Vec<u8>,
    ) -> Result<MappingElementKind, ConvertError> {
        let (kind, json) = element_json(element)?;
        serde_json::to_writer(&mut *out, &json)?;
        Ok(kind)
    }
}

/// A relational mapping element's JSON: a class mapping's or an
/// association mapping's.
#[derive(Serialize)]
#[serde(untagged)]
enum ElementJson<'a> {
    Class(Box<ClassMappingJson<'a>>),
    Association(AssociationMappingJson<'a>),
}

fn element_json(
    element: &MappingElement,
) -> Result<(MappingElementKind, ElementJson<'_>), ConvertError> {
    let body = element
        .body
        .as_any()
        .downcast_ref::<RelationalMappingBody>()
        .ok_or_else(|| {
            ConvertError::plugin(
                "RelationalMappingGrammar: the body is not a relational mapping's",
                Some(&element.source_info),
            )
        })?;
    let header = &element.header;
    Ok(match body {
        RelationalMappingBody::Class(class_mapping) => (
            MappingElementKind::Class,
            ElementJson::Class(Box::new(ClassMappingJson::new(
                class_mapping,
                header,
                &element.source_info,
            ))),
        ),
        RelationalMappingBody::Association(mappings) => (
            MappingElementKind::Association,
            ElementJson::Association(AssociationMappingJson {
                association: AssociationPointerJson {
                    element_type: "ASSOCIATION",
                    path: &header.target.path,
                    source_information: position(&header.target.source_info),
                },
                property_mappings: mappings.iter().map(PropertyMappingJson::from).collect(),
                stores: stores(mappings),
                source_information: position(&element.source_info),
                id: header.id.as_ref().map(|id| id.text.as_str()),
            }),
        ),
    })
}

/// `~filter …? ~distinct? ~groupBy(…)? ~primaryKey(…)? ~mainTable …?`
/// then the property mappings, in that order.
fn class_mapping(
    ctx: &mut ParserContext<'_>,
    id: Option<&SmolStr>,
    class: &str,
) -> Result<RelationalClassMapping, ParseError> {
    let filter = if at_command(ctx, "filter") {
        Some(filter_mapping(ctx)?)
    } else {
        None
    };
    let distinct = eat_command(ctx, "distinct");
    // In the filter's database.
    let scope = Scope {
        database: filter.as_ref().map(|filter| filter.database.clone()),
        ..Scope::default()
    };
    let group_by = if eat_command(ctx, "groupBy") {
        operations(ctx, &scope)?
    } else {
        Vec::new()
    };
    let primary_key = if eat_command(ctx, "primaryKey") {
        operations(ctx, &scope)?
    } else {
        Vec::new()
    };
    let main_table = if eat_command(ctx, "mainTable") {
        let database = database_pointer(ctx)?;
        let (schema, table) = scope_names(ctx)?;
        Some(scoped_table_ptr(Some(database), schema, table))
    } else {
        None
    };
    let property_mappings = property_mappings(ctx, TokenKind::Eof, id, Some(class))?;
    Ok(RelationalClassMapping {
        filter,
        distinct,
        group_by,
        primary_key,
        main_table,
        property_mappings,
    })
}

/// `~filter [db] (joins | [db])? Filter`.
fn filter_mapping(ctx: &mut ParserContext<'_>) -> Result<FilterMapping, ParseError> {
    let start = ctx.cursor().current_source_info();
    eat_command(ctx, "filter");
    let mut database = database_pointer(ctx)?;
    let mut joins = Vec::new();
    if matches!(ctx.cursor().peek_kind(), TokenKind::At | TokenKind::LParen) {
        joins = join_sequence(
            ctx,
            Some(database.clone()),
            &Scope::database(database.clone()),
        )?;
        ctx.cursor().expect(TokenKind::Pipe)?;
        database = database_pointer(ctx)?;
    }
    let name = identifier(ctx)?;
    Ok(FilterMapping {
        database,
        name: name.value,
        joins,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `( operation, … )`.
fn operations(ctx: &mut ParserContext<'_>, scope: &Scope) -> Result<Vec<Operation>, ParseError> {
    ctx.cursor().expect(TokenKind::LParen)?;
    let operations = separated(ctx, TokenKind::RParen, |ctx| operation(ctx, scope))?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(operations)
}

/// `schema (. table (. column)?)?` after a database pointer: a scope whose
/// first name is the schema, or the table when it is alone (the walker's
/// `visitMappingScopeInfo`). A column is not kept.
fn mapping_scope(
    ctx: &mut ParserContext<'_>,
    database: Option<String>,
) -> Result<Scope, ParseError> {
    let (schema, table) = scope_names(ctx)?;
    Ok(Scope {
        database,
        schema: Some(schema),
        table,
    })
}

/// A scope's names: `schema(.table(.column)?)?`, the column read and left
/// out.
fn scope_names(ctx: &mut ParserContext<'_>) -> Result<(Name, Option<Name>), ParseError> {
    let schema = relational_identifier(ctx)?;
    let mut table = None;
    if ctx.cursor().eat(TokenKind::Dot) {
        table = Some(relational_identifier(ctx)?);
        if ctx.cursor().eat(TokenKind::Dot) {
            relational_identifier(ctx)?;
        }
    }
    Ok((schema, table))
}

/// Property mappings, `scope( … )` blocks expanded, up to `close`.
fn property_mappings(
    ctx: &mut ParserContext<'_>,
    close: TokenKind,
    class_mapping_id: Option<&SmolStr>,
    class: Option<&str>,
) -> Result<Vec<PropertyMapping>, ParseError> {
    let blocks = separated(ctx, close, |ctx| {
        if at_word(ctx, "scope") && ctx.cursor().peek_kind_at(1) == TokenKind::LParen {
            scoped_property_mappings(ctx, class_mapping_id, class)
        } else {
            Ok(vec![single_property_mapping(
                ctx,
                &Scope::default(),
                class_mapping_id,
                class,
            )?])
        }
    })?;
    Ok(blocks.into_iter().flatten().collect())
}

/// `scope([db] scope?) ( single, … )`.
fn scoped_property_mappings(
    ctx: &mut ParserContext<'_>,
    class_mapping_id: Option<&SmolStr>,
    class: Option<&str>,
) -> Result<Vec<PropertyMapping>, ParseError> {
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::LParen)?;
    let database = database_pointer(ctx)?;
    let scope = if ctx.cursor().check(TokenKind::RParen) {
        Scope::database(database)
    } else {
        mapping_scope(ctx, Some(database))?
    };
    ctx.cursor().expect(TokenKind::RParen)?;
    ctx.cursor().expect(TokenKind::LParen)?;
    let mappings = separated(ctx, TokenKind::RParen, |ctx| {
        single_property_mapping(ctx, &scope, class_mapping_id, class)
    })?;
    if mappings.is_empty() {
        return Err(unexpected(ctx.cursor().peek()));
    }
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(mappings)
}

/// `+ name: Type[m] relational`, or `name ([ids])?` then a relational,
/// embedded or inline mapping.
fn single_property_mapping(
    ctx: &mut ParserContext<'_>,
    scope: &Scope,
    class_mapping_id: Option<&SmolStr>,
    class: Option<&str>,
) -> Result<PropertyMapping, ParseError> {
    if ctx.cursor().eat(TokenKind::Plus) {
        let name = identifier(ctx)?;
        let local = local_property(ctx)?;
        let property = PropertyPointer {
            class: None,
            property: name.value,
            source_info: name.source_info,
        };
        return Ok(PropertyMapping::Relational(relational_property_mapping(
            ctx,
            property,
            Some(local),
            scope,
            class_mapping_id.cloned(),
            None,
        )?));
    }
    let name = identifier(ctx)?;
    let property = PropertyPointer {
        class: class.map(ToString::to_string),
        property: name.value,
        source_info: name.source_info,
    };
    let (source, target) = if ctx.cursor().eat(TokenKind::LBracket) {
        let first = identifier(ctx)?;
        let ids = if ctx.cursor().eat(TokenKind::Comma) {
            (Some(first.text), Some(identifier(ctx)?.text))
        } else {
            (None, Some(first.text))
        };
        ctx.cursor().expect(TokenKind::RBracket)?;
        ids
    } else {
        (None, None)
    };
    match ctx.cursor().peek_kind() {
        TokenKind::Colon => Ok(PropertyMapping::Relational(relational_property_mapping(
            ctx,
            property,
            None,
            scope,
            source.or_else(|| class_mapping_id.cloned()),
            target,
        )?)),
        TokenKind::LParen if ctx.cursor().peek_kind_at(1) == TokenKind::RParen => {
            inline(ctx, property, target)
        }
        TokenKind::LParen => embedded(ctx, property, scope, class_mapping_id, target),
        _ => Err(unexpected(ctx.cursor().peek())),
    }
}

/// `: Type[from..to]`, from `:` to `]`: the type unquoted, its bounds as
/// the walker's `buildMultiplicity` reads them.
fn local_property(
    ctx: &mut ParserContext<'_>,
) -> Result<(String, u32, Option<u32>, SourceInfo), ParseError> {
    let start = ctx.cursor().current_source_info();
    ctx.cursor().expect(TokenKind::Colon)?;
    let type_name = qualified_name(ctx)?.path;
    ctx.cursor().expect(TokenKind::LBracket)?;
    let first = bound(ctx)?;
    let (lower, upper) = if ctx.cursor().eat(TokenKind::Dot) {
        ctx.cursor().expect(TokenKind::Dot)?;
        let lower = first.ok_or_else(|| unexpected(ctx.cursor().peek()))?;
        (lower, bound(ctx)?)
    } else {
        (first.unwrap_or(0), first)
    };
    ctx.cursor().expect(TokenKind::RBracket)?;
    Ok((type_name, lower, upper, ctx.cursor().span_since(&start)))
}

/// A bound: an integer, or `*` (`None`).
fn bound(ctx: &mut ParserContext<'_>) -> Result<Option<u32>, ParseError> {
    let token = ctx.cursor().peek().clone();
    match token.kind {
        TokenKind::Star => {
            ctx.cursor().advance();
            Ok(None)
        }
        TokenKind::IntegerLiteral => {
            ctx.cursor().advance();
            token.text.parse().map(Some).map_err(|_| unexpected(&token))
        }
        _ => Err(unexpected(&token)),
    }
}

/// `: (EnumerationMapping id: | Binding my::Binding:)? operation`.
fn relational_property_mapping(
    ctx: &mut ParserContext<'_>,
    property: PropertyPointer,
    local: Option<(String, u32, Option<u32>, SourceInfo)>,
    scope: &Scope,
    source: Option<SmolStr>,
    target: Option<SmolStr>,
) -> Result<RelationalPropertyMapping, ParseError> {
    let start = ctx.cursor().current_source_info();
    ctx.cursor().expect(TokenKind::Colon)?;
    let (mut enumeration_mapping, mut binding) = (None, None);
    if at_word(ctx, "EnumerationMapping") && ctx.cursor().peek_kind_at(2) == TokenKind::Colon {
        ctx.cursor().advance();
        enumeration_mapping = Some(identifier(ctx)?.value);
        ctx.cursor().expect(TokenKind::Colon)?;
    } else if at_word(ctx, "Binding") && at_binding(ctx) {
        ctx.cursor().advance();
        binding = Some(qualified_name(ctx)?.path);
        ctx.cursor().expect(TokenKind::Colon)?;
    }
    let operation = operation(ctx, scope)?;
    Ok(RelationalPropertyMapping {
        property,
        source,
        target,
        local,
        enumeration_mapping,
        binding,
        operation,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `Binding a::b:`: a qualified name then `:` after `Binding`.
fn at_binding(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    let mut at = 1;
    loop {
        if !cursor.peek_kind_at(at).is_identifier_like() {
            return false;
        }
        at += 1;
        if cursor.peek_kind_at(at) != TokenKind::PathSep {
            return cursor.peek_kind_at(at) == TokenKind::Colon;
        }
        at += 1;
    }
}

/// `( ) Inline [id]`.
fn inline(
    ctx: &mut ParserContext<'_>,
    property: PropertyPointer,
    target: Option<SmolStr>,
) -> Result<PropertyMapping, ParseError> {
    let start = ctx.cursor().current_source_info();
    ctx.cursor().advance();
    ctx.cursor().advance();
    let inline = ctx.cursor().peek().clone();
    if !is_word(&inline, "Inline") {
        return Err(unexpected(&inline));
    }
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::LBracket)?;
    let set_implementation_id = identifier(ctx)?.value;
    ctx.cursor().expect(TokenKind::RBracket)?;
    Ok(PropertyMapping::Inline {
        property,
        target,
        set_implementation_id,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `( ~primaryKey(…)? single, … )`, then maybe
/// `Otherwise ( [target]: [db]? joins )`.
fn embedded(
    ctx: &mut ParserContext<'_>,
    property: PropertyPointer,
    scope: &Scope,
    class_mapping_id: Option<&SmolStr>,
    target: Option<SmolStr>,
) -> Result<PropertyMapping, ParseError> {
    let start = ctx.cursor().current_source_info();
    ctx.cursor().expect(TokenKind::LParen)?;
    // In no database.
    let primary_key = if eat_command(ctx, "primaryKey") {
        operations(ctx, &Scope::default())?
    } else {
        Vec::new()
    };
    let property_mappings = separated(ctx, TokenKind::RParen, |ctx| {
        single_property_mapping(ctx, scope, class_mapping_id, None)
    })?;
    if !primary_key.is_empty() && property_mappings.is_empty() {
        return Err(unexpected(ctx.cursor().peek()));
    }
    ctx.cursor().expect(TokenKind::RParen)?;
    let embedded_span = ctx.cursor().span_since(&start);
    let otherwise =
        if at_word(ctx, "Otherwise") && ctx.cursor().peek_kind_at(1) == TokenKind::LParen {
            let otherwise_start = ctx.cursor().current_source_info();
            ctx.cursor().advance();
            ctx.cursor().advance();
            let mapping_start = ctx.cursor().current_source_info();
            ctx.cursor().expect(TokenKind::LBracket)?;
            let otherwise_target = identifier(ctx)?.value;
            ctx.cursor().expect(TokenKind::RBracket)?;
            ctx.cursor().expect(TokenKind::Colon)?;
            let database = if ctx.cursor().check(TokenKind::LBracket) {
                Some(database_pointer(ctx)?)
            } else {
                None
            };
            let joins = join_sequence(ctx, database, scope)?;
            let mapping_span = ctx.cursor().span_since(&mapping_start);
            ctx.cursor().expect(TokenKind::RParen)?;
            let mapping = RelationalPropertyMapping {
                property: property.clone(),
                source: None,
                target: Some(otherwise_target),
                local: None,
                enumeration_mapping: None,
                binding: None,
                operation: Operation::ElementWithJoins {
                    joins,
                    element: None,
                    source_info: mapping_span.clone(),
                },
                source_info: mapping_span,
            };
            Some((Box::new(mapping), ctx.cursor().span_since(&otherwise_start)))
        } else {
            None
        };
    let class_mapping = EmbeddedClassMapping {
        primary_key,
        property_mappings,
        source_info: ctx.cursor().span_since(&start),
    };
    Ok(PropertyMapping::Embedded {
        property,
        target,
        class_mapping,
        otherwise,
        source_info: embedded_span,
    })
}

/// `item (, item)*`, or nothing when `close` is next; `close` is left.
fn separated<T>(
    ctx: &mut ParserContext<'_>,
    close: TokenKind,
    mut item: impl FnMut(&mut ParserContext<'_>) -> Result<T, ParseError>,
) -> Result<Vec<T>, ParseError> {
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

// ── The protocol ───────────────────────────────────────────────────────

fn position(source_info: &SourceInfo) -> SourceInformation {
    SourceInformation::from(source_info)
}

/// The engine's `RootRelationalClassMapping`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "relational", rename_all = "camelCase")]
struct ClassMappingJson<'a> {
    class: &'a str,
    root: bool,
    distinct: bool,
    primary_key: Vec<OperationJson<'a>>,
    group_by: Vec<OperationJson<'a>>,
    property_mappings: Vec<PropertyMappingJson<'a>>,
    source_information: SourceInformation,
    class_source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    id: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    extends_class_mapping_id: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    filter: Option<FilterMappingJson<'a>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    main_table: Option<TablePtrJson<'a>>,
}

impl<'a> ClassMappingJson<'a> {
    fn new(
        class_mapping: &'a RelationalClassMapping,
        header: &'a MappingElementHeader,
        source_info: &SourceInfo,
    ) -> Self {
        Self {
            class: &header.target.path,
            root: header.root,
            distinct: class_mapping.distinct,
            primary_key: class_mapping
                .primary_key
                .iter()
                .map(OperationJson::from)
                .collect(),
            group_by: class_mapping
                .group_by
                .iter()
                .map(OperationJson::from)
                .collect(),
            property_mappings: class_mapping
                .property_mappings
                .iter()
                .map(PropertyMappingJson::from)
                .collect(),
            source_information: position(source_info),
            class_source_information: position(&header.target.source_info),
            id: header.id.as_ref().map(|id| id.text.as_str()),
            extends_class_mapping_id: header.extends.as_ref().map(|extends| extends.text.as_str()),
            filter: class_mapping.filter.as_ref().map(|filter| {
                FilterMappingJson::new(
                    Some(&filter.database),
                    filter.name.as_str(),
                    &filter.joins,
                    &filter.source_info,
                )
            }),
            main_table: class_mapping.main_table.as_ref().map(TablePtrJson::from),
        }
    }
}

/// The engine's `RelationalAssociationMapping`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "relational", rename_all = "camelCase")]
struct AssociationMappingJson<'a> {
    association: AssociationPointerJson<'a>,
    property_mappings: Vec<PropertyMappingJson<'a>>,
    stores: Vec<String>,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    id: Option<&'a str>,
}

/// The association's pointer.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct AssociationPointerJson<'a> {
    #[serde(rename = "type")]
    element_type: &'static str,
    path: &'a str,
    source_information: SourceInformation,
}

/// The engine's `PropertyPointer`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct PropertyPointerJson<'a> {
    property: &'a str,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    class: Option<&'a str>,
}

impl<'a> From<&'a PropertyPointer> for PropertyPointerJson<'a> {
    fn from(property: &'a PropertyPointer) -> Self {
        Self {
            property: property.property.as_str(),
            source_information: position(&property.source_info),
            class: property.class.as_deref(),
        }
    }
}

/// The engine's relational `PropertyMapping`s.
#[derive(Serialize)]
#[serde(tag = "_type")]
enum PropertyMappingJson<'a> {
    #[serde(rename = "relationalPropertyMapping")]
    Relational(RelationalPropertyMappingJson<'a>),
    #[serde(rename = "embeddedPropertyMapping")]
    Embedded(EmbeddedPropertyMappingJson<'a>),
    #[serde(rename = "otherwiseEmbeddedPropertyMapping")]
    OtherwiseEmbedded(EmbeddedPropertyMappingJson<'a>),
    #[serde(rename = "inlineEmbeddedPropertyMapping")]
    Inline(InlinePropertyMappingJson<'a>),
}

impl<'a> From<&'a PropertyMapping> for PropertyMappingJson<'a> {
    fn from(mapping: &'a PropertyMapping) -> Self {
        match mapping {
            PropertyMapping::Relational(mapping) => {
                Self::Relational(RelationalPropertyMappingJson::from(mapping))
            }
            PropertyMapping::Embedded {
                property,
                target,
                class_mapping,
                otherwise,
                source_info,
            } => {
                let target = target.as_ref().map(SmolStr::as_str);
                let embedded = |source_info, otherwise| EmbeddedPropertyMappingJson {
                    property: PropertyPointerJson::from(property),
                    class_mapping: EmbeddedClassMappingJson {
                        class_mapping_type: "embedded",
                        root: false,
                        primary_key: class_mapping
                            .primary_key
                            .iter()
                            .map(OperationJson::from)
                            .collect(),
                        property_mappings: class_mapping
                            .property_mappings
                            .iter()
                            .map(Self::from)
                            .collect(),
                        source_information: position(&class_mapping.source_info),
                        id: target,
                    },
                    source_information: position(source_info),
                    otherwise_property_mapping: otherwise,
                    id: target,
                    target,
                };
                match otherwise {
                    Some((mapping, span)) => Self::OtherwiseEmbedded(embedded(
                        span,
                        Some(Box::new(Self::Relational(
                            RelationalPropertyMappingJson::from(mapping.as_ref()),
                        ))),
                    )),
                    None => Self::Embedded(embedded(source_info, None)),
                }
            }
            PropertyMapping::Inline {
                property,
                target,
                set_implementation_id,
                source_info,
            } => {
                let target = target.as_ref().map(SmolStr::as_str);
                Self::Inline(InlinePropertyMappingJson {
                    property: PropertyPointerJson::from(property),
                    set_implementation_id: set_implementation_id.as_str(),
                    source_information: position(source_info),
                    id: target,
                    target,
                })
            }
        }
    }
}

/// The engine's `EmbeddedRelationalPropertyMapping`, with or without an
/// `otherwise`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct EmbeddedPropertyMappingJson<'a> {
    property: PropertyPointerJson<'a>,
    class_mapping: EmbeddedClassMappingJson<'a>,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    otherwise_property_mapping: Option<Box<PropertyMappingJson<'a>>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    id: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    target: Option<&'a str>,
}

/// An embedded property mapping's class mapping.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct EmbeddedClassMappingJson<'a> {
    #[serde(rename = "_type")]
    class_mapping_type: &'static str,
    root: bool,
    primary_key: Vec<OperationJson<'a>>,
    property_mappings: Vec<PropertyMappingJson<'a>>,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    id: Option<&'a str>,
}

/// The engine's `InlineEmbeddedPropertyMapping`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct InlinePropertyMappingJson<'a> {
    property: PropertyPointerJson<'a>,
    set_implementation_id: &'a str,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    id: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    target: Option<&'a str>,
}

/// The engine's `RelationalPropertyMapping`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct RelationalPropertyMappingJson<'a> {
    property: PropertyPointerJson<'a>,
    relational_operation: OperationJson<'a>,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    source: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    target: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    local_mapping_property: Option<LocalMappingPropertyJson<'a>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    enum_mapping_id: Option<&'a str>,
    /// Its position is not written (`@JsonIgnore`).
    #[serde(skip_serializing_if = "Option::is_none")]
    binding_transformer: Option<BindingTransformerJson<'a>>,
}

impl<'a> From<&'a RelationalPropertyMapping> for RelationalPropertyMappingJson<'a> {
    fn from(mapping: &'a RelationalPropertyMapping) -> Self {
        Self {
            property: PropertyPointerJson::from(&mapping.property),
            relational_operation: OperationJson::from(&mapping.operation),
            source_information: position(&mapping.source_info),
            source: mapping.source.as_ref().map(SmolStr::as_str),
            target: mapping.target.as_ref().map(SmolStr::as_str),
            local_mapping_property: mapping.local.as_ref().map(
                |(type_name, lower, upper, source_info)| LocalMappingPropertyJson {
                    property_type: type_name,
                    multiplicity: MultiplicityJson {
                        lower_bound: *lower,
                        upper_bound: *upper,
                    },
                    source_information: position(source_info),
                },
            ),
            enum_mapping_id: mapping.enumeration_mapping.as_ref().map(SmolStr::as_str),
            binding_transformer: mapping
                .binding
                .as_deref()
                .map(|binding| BindingTransformerJson { binding }),
        }
    }
}

/// A local mapping property's type and multiplicity.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct LocalMappingPropertyJson<'a> {
    #[serde(rename = "type")]
    property_type: &'a str,
    multiplicity: MultiplicityJson,
    source_information: SourceInformation,
}

/// A multiplicity, its upper bound absent when unbounded.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct MultiplicityJson {
    lower_bound: u32,
    #[serde(skip_serializing_if = "Option::is_none")]
    upper_bound: Option<u32>,
}

/// The engine's `BindingTransformer`.
#[derive(Serialize)]
struct BindingTransformerJson<'a> {
    binding: &'a str,
}

/// The walker's `propagateStorePath`: the database of the first join
/// that names one, in the first relational property mapping, when its
/// value goes through joins.
fn stores(mappings: &[PropertyMapping]) -> Vec<String> {
    let first = mappings.iter().find_map(|mapping| match mapping {
        PropertyMapping::Relational(mapping) => Some(&mapping.operation),
        _ => None,
    });
    match first {
        Some(Operation::ElementWithJoins { joins, .. }) => joins
            .iter()
            .find_map(|join| join.database.clone())
            .into_iter()
            .collect(),
        _ => Vec::new(),
    }
}

// ── Legacy test input ──────────────────────────────────────────────────

/// The relational store's test input: `<Relational, SQL, my::Db, '…'>`
/// (`RelationalGrammarParserExtension`'s, the engine's
/// `RelationalInputData`).
pub struct RelationalInputGrammar;

/// The formats the engine's `RelationalInputType` has.
const INPUT_FORMATS: [&str; 2] = ["SQL", "CSV"];

impl TestInputGrammar for RelationalInputGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn check(&self, input: &TestInput) -> Result<(), ParseError> {
        let possible = INPUT_FORMATS.join(", ");
        let Some(format) = &input.format else {
            return Err(ParseError::Unexpected {
                message: format!(
                    "Mapping test relational 'input type' is missing. Possible values: {possible}"
                ),
                source_info: input.source_info.clone(),
            });
        };
        if !INPUT_FORMATS.contains(&format.text.as_str()) {
            return Err(ParseError::Unexpected {
                message: format!(
                    "Mapping test relational input data does not support format '{}'. Possible \
                     values: {possible}",
                    format.text
                ),
                source_info: format.source_info.clone(),
            });
        }
        data(input).map(|_| ())
    }

    fn convert(&self, input: &TestInput) -> Result<Value, ConvertError> {
        Ok(serde_json::to_value(InputDataJson {
            // As written.
            database: &input.source.text,
            data: data(input).unwrap_or_default(),
            input_type: input.format.as_ref().map(|format| format.text.as_str()),
            source_information: position(&input.source_info),
        })?)
    }
}

/// The engine's `RelationalInputData`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "relational", rename_all = "camelCase")]
struct InputDataJson<'a> {
    database: &'a str,
    data: String,
    input_type: Option<&'a str>,
    source_information: SourceInformation,
}

/// The strings, unescaped but for `\;`, joined.
fn data(input: &TestInput) -> Result<String, ParseError> {
    let mut data = String::new();
    for text in &input.data {
        let (value, _) = string_value(&text.replace("\\;", "\\\\;")).map_err(|message| {
            ParseError::Unexpected {
                message,
                source_info: input.source_info.clone(),
            }
        })?;
        data.push_str(&value);
    }
    Ok(data)
}

#[distributed_slice(MAPPING_ELEMENT_GRAMMARS)]
static RELATIONAL_MAPPING_GRAMMAR: &(dyn MappingElementGrammar + Send + Sync) =
    &RelationalMappingGrammar;

#[distributed_slice(TEST_INPUT_GRAMMARS)]
static RELATIONAL_INPUT_GRAMMAR: &(dyn TestInputGrammar + Send + Sync) = &RelationalInputGrammar;
