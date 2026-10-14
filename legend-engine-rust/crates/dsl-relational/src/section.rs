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

//! `SectionParser` for `###Relational`, the engine's `RelationalParserGrammar`
//! (its `database` rule and what it holds) and `RelationalParseTreeWalker`:
//!
//! ```text
//! database:  doc? Database <<s>>? {t}? name ( include* (schema | table | view
//!            | join | tabularFunction | filter | multiGrainFilter)* )
//! schema:    doc? Schema <<s>>? {t}? name ( (table | view | tabularFunction)* )
//! table:     doc? Table <<s>>? {t}? name ( milestoning(…)? column, … )
//! column:    doc? name <<s>>? {t}? TYPE (size (, scale)?)? (PRIMARY KEY | NOT NULL)?
//! view:      doc? View <<s>>? {t}? name ( ~filter …? ~groupBy(…)? ~distinct? name: operation PRIMARY KEY?, … )
//! ```
//!
//! The section has no imports. Names of schemas, tables, views, functions,
//! columns and view columns are kept as written, quotes included, as the
//! walker keeps them; joins' and filters' are unquoted.

use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{ParserContext, SectionParser};
use smol_str::SmolStr;

use crate::ast::{
    Column, ColumnMapping, DataType, Database, Filter, FilterMapping, Include, IncludedStore, Join,
    SECTION_KIND, Schema, Table, TabularFunction, View,
};
use crate::milestoning::milestone_spec;
use crate::operation::{Scope, join_sequence, operation};
use crate::tokens::{
    Name, at_command, at_word, database_pointer, documentation, eat_command, eat_words,
    expect_word, identifier, integer, is_word, is_word_kind, qualified_name, relational_identifier,
    schema_identifier, unexpected,
};

/// Parser for `###Relational` sections.
pub struct RelationalSectionParser;

impl SectionParser for RelationalSectionParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn imports(&self) -> bool {
        false
    }

    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        let mut elements: Vec<Box<dyn DSLElement>> = Vec::new();
        while !ctx.cursor().check(TokenKind::SectionHeader) && !ctx.cursor().check(TokenKind::Eof) {
            match database(ctx) {
                Ok(database) => elements.push(Box::new(database)),
                // The engine reports the first error; the rest of the section
                // is not read.
                Err(error) => {
                    errors.push(error);
                    break;
                }
            }
        }
        elements
    }
}

/// An element's documentation, stereotypes and tagged values, and its
/// start: the documentation's, else its keyword's.
struct Header {
    start: SourceInfo,
    documentation: Option<Token>,
}

impl Header {
    fn read(ctx: &mut ParserContext<'_>) -> Self {
        let start = ctx.cursor().current_source_info();
        let documentation = documentation(ctx);
        Self {
            start,
            documentation,
        }
    }
}

/// `<<stereotypes>>? {tagged values}?`, the documentation folded in.
fn annotations(
    ctx: &mut ParserContext<'_>,
    documentation: Option<Token>,
) -> Result<
    (
        Vec<legend_pure_parser_ast::annotation::StereotypePtr>,
        Vec<legend_pure_parser_ast::annotation::TaggedValue>,
    ),
    ParseError,
> {
    let stereotypes = if ctx.cursor().check(TokenKind::LessLess) {
        ctx.parse_stereotypes()?
    } else {
        Vec::new()
    };
    let tagged_values = if ctx.cursor().check(TokenKind::LBrace) {
        ctx.parse_tagged_values()?
    } else {
        Vec::new()
    };
    Ok((
        stereotypes,
        ParserContext::with_documentation(documentation, tagged_values)?,
    ))
}

fn database(ctx: &mut ParserContext<'_>) -> Result<Database, ParseError> {
    let header = Header::read(ctx);
    expect_word(ctx, "Database")?;
    let (stereotypes, tagged_values) = annotations(ctx, header.documentation)?;
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    let path = package
        .as_ref()
        .map_or_else(|| name.to_string(), |package| format!("{package}::{name}"));
    let scope = Scope::database(path);
    ctx.cursor().expect(TokenKind::LParen)?;

    let mut includes = Vec::new();
    let mut included_stores = Vec::new();
    while at_word(ctx, "include") {
        include(ctx, &mut includes, &mut included_stores)?;
    }
    let mut schemas = Vec::new();
    let mut tables = Vec::new();
    let mut views = Vec::new();
    let mut tabular_functions = Vec::new();
    let mut joins = Vec::new();
    let mut filters = Vec::new();
    let mut multi_grain_filters = Vec::new();
    while !ctx.cursor().check(TokenKind::RParen) {
        let header = Header::read(ctx);
        let keyword = ctx.cursor().peek().clone();
        let with_documentation = header.documentation.is_some();
        match keyword.text.as_str() {
            _ if !is_word_kind(keyword.kind) => return Err(unexpected(&keyword)),
            "Schema" => schemas.push(schema(ctx, header, &scope)?),
            "Table" => tables.push(table(ctx, header)?),
            "View" => views.push(view(ctx, header, &scope)?),
            _ if with_documentation => return Err(unexpected(&keyword)),
            "TabularFunction" => tabular_functions.push(tabular_function(ctx)?),
            "Join" => joins.push(join(ctx, &scope)?),
            "Filter" => filters.push(filter(ctx, &scope, false)?),
            "MultiGrainFilter" => multi_grain_filters.push(filter(ctx, &scope, true)?),
            _ => return Err(unexpected(&keyword)),
        }
    }
    ctx.cursor().expect(TokenKind::RParen)?;
    let source_info = ctx.cursor().span_since(&header.start);
    // Tables, views and functions outside a schema are in `default`.
    if !tables.is_empty() || !views.is_empty() || !tabular_functions.is_empty() {
        schemas.push(Schema {
            name: SmolStr::new_static("default"),
            stereotypes: Vec::new(),
            tagged_values: Vec::new(),
            tables,
            views,
            tabular_functions,
            source_info: source_info.clone(),
        });
    }
    filters.extend(multi_grain_filters);
    Ok(Database {
        package,
        name: SpannedString {
            value: name,
            source_info: name_source_info,
        },
        stereotypes,
        tagged_values,
        includes,
        included_stores,
        schemas,
        joins,
        filters,
        source_info,
    })
}

/// `include my::Db`, or `include StoreType my::Store`.
fn include(
    ctx: &mut ParserContext<'_>,
    includes: &mut Vec<Include>,
    included_stores: &mut Vec<IncludedStore>,
) -> Result<(), ParseError> {
    let start = ctx.cursor().current_source_info();
    ctx.cursor().advance();
    if at_included_store(ctx) {
        let store_type = identifier(ctx)?.value;
        let path = qualified_name(ctx)?;
        included_stores.push(IncludedStore {
            store_type,
            path,
            source_info: ctx.cursor().span_since(&start),
        });
    } else {
        let path = qualified_name(ctx)?;
        includes.push(Include {
            path,
            source_info: ctx.cursor().span_since(&start),
        });
    }
    Ok(())
}

/// After `include`, a store type then a store: `ServiceStore my::Store`,
/// rather than a database followed by the next element.
fn at_included_store(ctx: &mut ParserContext<'_>) -> bool {
    const ELEMENTS: [&str; 8] = [
        "include",
        "Schema",
        "Table",
        "View",
        "Join",
        "Filter",
        "MultiGrainFilter",
        "TabularFunction",
    ];
    let cursor = ctx.cursor();
    let next = cursor.peek_at(1);
    if cursor.peek_kind_at(1) == TokenKind::PathSep
        || !(is_word_kind(next.kind) || crate::tokens::is_string(next))
    {
        return false;
    }
    // An element keyword starts the next element, unless a path goes on.
    !ELEMENTS.iter().any(|element| is_word(next, element))
        || cursor.peek_kind_at(2) == TokenKind::PathSep
}

fn schema(
    ctx: &mut ParserContext<'_>,
    header: Header,
    scope: &Scope,
) -> Result<Schema, ParseError> {
    expect_word(ctx, "Schema")?;
    let (stereotypes, tagged_values) = annotations(ctx, header.documentation)?;
    let name = schema_identifier(ctx)?;
    // A schema's views find their columns' tables in it.
    let view_scope = Scope {
        schema: Some(name.clone()),
        ..scope.clone()
    };
    ctx.cursor().expect(TokenKind::LParen)?;
    let mut tables = Vec::new();
    let mut views = Vec::new();
    let mut tabular_functions = Vec::new();
    while !ctx.cursor().check(TokenKind::RParen) {
        let header = Header::read(ctx);
        let keyword = ctx.cursor().peek().clone();
        match keyword.text.as_str() {
            _ if !is_word_kind(keyword.kind) => return Err(unexpected(&keyword)),
            "Table" => tables.push(table(ctx, header)?),
            "View" => views.push(view(ctx, header, &view_scope)?),
            "TabularFunction" if header.documentation.is_none() => {
                tabular_functions.push(tabular_function(ctx)?);
            }
            _ => return Err(unexpected(&keyword)),
        }
    }
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(Schema {
        name: name.text,
        stereotypes,
        tagged_values,
        tables,
        views,
        tabular_functions,
        source_info: ctx.cursor().span_since(&header.start),
    })
}

fn table(ctx: &mut ParserContext<'_>, header: Header) -> Result<Table, ParseError> {
    expect_word(ctx, "Table")?;
    let (stereotypes, tagged_values) = annotations(ctx, header.documentation)?;
    let name = relational_identifier(ctx)?;
    ctx.cursor().expect(TokenKind::LParen)?;
    // `milestoning(…)`, unlike a column named `milestoning`.
    let milestoning =
        if at_word(ctx, "milestoning") && ctx.cursor().peek_kind_at(1) == TokenKind::LParen {
            milestone_spec(ctx)?
        } else {
            Vec::new()
        };
    let mut primary_key = Vec::new();
    let columns = columns(ctx, &mut primary_key)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(Table {
        name: name.text,
        stereotypes,
        tagged_values,
        columns,
        primary_key,
        milestoning,
        source_info: ctx.cursor().span_since(&header.start),
    })
}

/// `column, …` up to `)`.
fn columns(
    ctx: &mut ParserContext<'_>,
    primary_key: &mut Vec<SmolStr>,
) -> Result<Vec<Column>, ParseError> {
    let mut columns = Vec::new();
    if ctx.cursor().check(TokenKind::RParen) {
        return Ok(columns);
    }
    loop {
        columns.push(column(ctx, primary_key)?);
        if !ctx.cursor().eat(TokenKind::Comma) {
            return Ok(columns);
        }
    }
}

fn column(
    ctx: &mut ParserContext<'_>,
    primary_key: &mut Vec<SmolStr>,
) -> Result<Column, ParseError> {
    let header = Header::read(ctx);
    let name = relational_identifier(ctx)?;
    let (stereotypes, tagged_values) = annotations(ctx, header.documentation)?;
    let type_name = identifier(ctx)?;
    let mut parameters = Vec::new();
    let mut close = None;
    if ctx.cursor().eat(TokenKind::LParen) {
        parameters.push(integer(ctx)?);
        if ctx.cursor().eat(TokenKind::Comma) {
            parameters.push(integer(ctx)?);
        }
        close = Some(ctx.cursor().expect(TokenKind::RParen)?.source_info);
    }
    let nullable = if eat_words(ctx, &["PRIMARY", "KEY"]).is_some() {
        primary_key.push(name.text.clone());
        false
    } else {
        eat_words(ctx, &["NOT", "NULL"]).is_none()
    };
    // From the type to `)`, as the walker reports a type's errors.
    let type_source_info = close.map_or_else(
        || type_name.source_info.clone(),
        |close| type_name.source_info.merge(&close),
    );
    // The walker's checks.
    let data_type =
        data_type(&name, &type_name, &parameters, &type_source_info).unwrap_or_else(|error| {
            ctx.defer(error);
            DataType::Other
        });
    Ok(Column {
        name: name.text,
        stereotypes,
        tagged_values,
        data_type,
        nullable,
        source_info: ctx.cursor().span_since(&header.start),
    })
}

/// The walker's column types: `RelationalDataType.valueOf(name.toUpperCase())`
/// and its parameter checks.
fn data_type(
    column: &Name,
    type_name: &Name,
    parameters: &[(String, SourceInfo)],
    source_info: &SourceInfo,
) -> Result<DataType, ParseError> {
    let upper = type_name.value.to_ascii_uppercase();
    let declaration = || {
        let parameters: Vec<&str> = parameters.iter().map(|(p, _)| p.as_str()).collect();
        let parameters = if parameters.is_empty() {
            String::new()
        } else {
            format!("({})", parameters.join(","))
        };
        format!("{}{}{parameters}", column.text, type_name.text)
    };
    let error = |requirement: &str| ParseError::Unexpected {
        message: format!(
            "Column data type {upper} {requirement} in declaration '{}'",
            declaration()
        ),
        source_info: source_info.clone(),
    };
    let int = |i: usize| -> Result<i32, ParseError> {
        let (text, source_info) = &parameters[i];
        text.parse::<i32>().map_err(|_| ParseError::Unexpected {
            message: format!("For input string: \"{text}\""),
            source_info: source_info.clone(),
        })
    };
    let sized = |make: fn(i32) -> DataType| {
        if parameters.len() == 1 {
            Ok(make(int(0)?))
        } else {
            Err(error("requires 1 parameter (size)"))
        }
    };
    let scaled = |make: fn(i32, i32) -> DataType| {
        if parameters.len() == 2 {
            Ok(make(int(0)?, int(1)?))
        } else {
            Err(error("requires 2 parameters (precision, scale)"))
        }
    };
    let plain = |data_type: DataType| {
        if parameters.is_empty() {
            Ok(data_type)
        } else {
            Err(error("does not expect any parameters"))
        }
    };
    match upper.as_str() {
        "CHAR" => sized(DataType::Char),
        "VARCHAR" => sized(DataType::Varchar),
        "BINARY" => sized(DataType::Binary),
        "VARBINARY" => sized(DataType::Varbinary),
        // `BOOLEAN`, the engine's since #5293, is a `BIT`.
        "BIT" | "BOOLEAN" => plain(DataType::Bit),
        // `INT` reports as `INTEGER`.
        "INT" | "INTEGER" => plain(DataType::Integer).map_err(|_| ParseError::Unexpected {
            message: format!(
                "Column data type INTEGER does not expect any parameters in declaration '{}'",
                declaration()
            ),
            source_info: source_info.clone(),
        }),
        "BIGINT" => plain(DataType::BigInt),
        "SMALLINT" => plain(DataType::SmallInt),
        "TINYINT" => plain(DataType::TinyInt),
        "TIMESTAMP" => plain(DataType::Timestamp),
        "DATE" => plain(DataType::Date),
        "NUMERIC" => scaled(|precision, scale| DataType::Numeric { precision, scale }),
        "DECIMAL" => scaled(|precision, scale| DataType::Decimal { precision, scale }),
        "FLOAT" => plain(DataType::Float),
        "DOUBLE" => plain(DataType::Double),
        "REAL" => plain(DataType::Real),
        "ARRAY" | "OTHER" => plain(DataType::Other),
        "SEMISTRUCTURED" => plain(DataType::SemiStructured),
        "JSON" => plain(DataType::Json),
        _ => Err(ParseError::Unexpected {
            message: format!("Unsupported column data type '{}'", type_name.value),
            source_info: type_name.source_info.clone(),
        }),
    }
}

fn view(ctx: &mut ParserContext<'_>, header: Header, scope: &Scope) -> Result<View, ParseError> {
    expect_word(ctx, "View")?;
    let (stereotypes, tagged_values) = annotations(ctx, header.documentation)?;
    let name = relational_identifier(ctx)?;
    ctx.cursor().expect(TokenKind::LParen)?;
    let filter = if at_command(ctx, "filter") {
        Some(view_filter(ctx)?)
    } else {
        None
    };
    let mut group_by = Vec::new();
    if eat_command(ctx, "groupBy") {
        ctx.cursor().expect(TokenKind::LParen)?;
        if !ctx.cursor().check(TokenKind::RParen) {
            loop {
                group_by.push(operation(ctx, scope)?);
                if !ctx.cursor().eat(TokenKind::Comma) {
                    break;
                }
            }
        }
        ctx.cursor().expect(TokenKind::RParen)?;
    }
    let distinct = eat_command(ctx, "distinct");
    let mut column_mappings = Vec::new();
    let mut primary_key = Vec::new();
    if !ctx.cursor().check(TokenKind::RParen) {
        loop {
            let start = ctx.cursor().current_source_info();
            let column = identifier(ctx)?;
            // `name[id]`: a set id, which the walker ignores.
            if ctx.cursor().eat(TokenKind::LBracket) {
                identifier(ctx)?;
                ctx.cursor().expect(TokenKind::RBracket)?;
            }
            ctx.cursor().expect(TokenKind::Colon)?;
            let operation = operation(ctx, scope)?;
            if eat_words(ctx, &["PRIMARY", "KEY"]).is_some() {
                primary_key.push(column.text.clone());
            }
            column_mappings.push(ColumnMapping {
                name: column.text,
                operation,
                source_info: ctx.cursor().span_since(&start),
            });
            if !ctx.cursor().eat(TokenKind::Comma) {
                break;
            }
        }
    }
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(View {
        name: name.text,
        stereotypes,
        tagged_values,
        distinct,
        filter,
        group_by,
        column_mappings,
        primary_key,
        source_info: ctx.cursor().span_since(&header.start),
    })
}

/// `~filter [db] @J1 > @J2 | [db] Filter`, `~filter [db] Filter` or
/// `~filter Filter`.
fn view_filter(ctx: &mut ParserContext<'_>) -> Result<FilterMapping, ParseError> {
    let start = ctx.cursor().current_source_info();
    eat_command(ctx, "filter");
    let mut joins = Vec::new();
    let mut database = None;
    if ctx.cursor().check(TokenKind::LBracket) {
        let pointer = database_pointer(ctx)?;
        if ctx.cursor().check(TokenKind::At) || ctx.cursor().check(TokenKind::LParen) {
            joins = join_sequence(ctx, Some(pointer.clone()), &Scope::database(pointer))?;
            ctx.cursor().expect(TokenKind::Pipe)?;
            database = Some(database_pointer(ctx)?);
        } else {
            database = Some(pointer);
        }
    }
    let filter = identifier(ctx)?.value;
    Ok(FilterMapping {
        database,
        filter,
        joins,
        source_info: ctx.cursor().span_since(&start),
    })
}

fn tabular_function(ctx: &mut ParserContext<'_>) -> Result<TabularFunction, ParseError> {
    let start = ctx.cursor().current_source_info();
    expect_word(ctx, "TabularFunction")?;
    let name = relational_identifier(ctx)?;
    ctx.cursor().expect(TokenKind::LParen)?;
    let columns = columns(ctx, &mut Vec::new())?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(TabularFunction {
        name: name.text,
        columns,
        source_info: ctx.cursor().span_since(&start),
    })
}

fn join(ctx: &mut ParserContext<'_>, scope: &Scope) -> Result<Join, ParseError> {
    let start = ctx.cursor().current_source_info();
    expect_word(ctx, "Join")?;
    let name = identifier(ctx)?.value;
    ctx.cursor().expect(TokenKind::LParen)?;
    let operation = operation(ctx, scope)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(Join {
        name,
        operation,
        source_info: ctx.cursor().span_since(&start),
    })
}

fn filter(
    ctx: &mut ParserContext<'_>,
    scope: &Scope,
    multi_grain: bool,
) -> Result<Filter, ParseError> {
    let start = ctx.cursor().current_source_info();
    ctx.cursor().advance();
    let name = identifier(ctx)?.value;
    ctx.cursor().expect(TokenKind::LParen)?;
    let operation = operation(ctx, scope)?;
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(Filter {
        name,
        multi_grain,
        operation,
        source_info: ctx.cursor().span_since(&start),
    })
}

#[linkme::distributed_slice(legend_pure_parser_parser::section_parser::SECTION_PARSERS)]
static RELATIONAL_SECTION_PARSER: &(dyn SectionParser + Send + Sync) = &RelationalSectionParser;
