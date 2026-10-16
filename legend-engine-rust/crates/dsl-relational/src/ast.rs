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

//! Relational AST: the `###Relational` section's databases, and
//! `Relational #{ … }#` test data.
//!
//! A database's operations (joins, filters, views' columns) are built as the
//! engine's `RelationalParseTreeWalker` builds them: its scope resolution
//! (which database and table a column belongs to) and its positions are
//! applied while parsing, so that the protocol conversion is one to one.

use std::any::Any;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::data::EmbeddedDataContent;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use smol_str::SmolStr;

/// The embedded data type this grammar reads.
pub const TYPE_NAME: &str = "Relational";

/// `Relational #{ … }#`: one CSV text per table.
#[derive(Debug, Clone, PartialEq)]
pub struct RelationalData {
    /// The tables.
    pub tables: Vec<RelationalTable>,
    /// The whole embedded data, from `Relational` to `}#`, as the engine
    /// positions it.
    pub source_info: SourceInfo,
}

/// `schema.table: 'row\n' + 'row\n';`.
#[derive(Debug, Clone, PartialEq)]
pub struct RelationalTable {
    /// The schema.
    pub schema: SmolStr,
    /// The table.
    pub table: SmolStr,
    /// The rows' strings, unescaped (`\;` kept) and joined.
    pub values: String,
    /// From the schema to `;`.
    pub source_info: SourceInfo,
}

impl EmbeddedDataContent for RelationalData {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn EmbeddedDataContent> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn EmbeddedDataContent) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|o| self == o)
    }
}

/// The section kind, `###Relational`, which is also its databases' kind.
pub const SECTION_KIND: &str = "Relational";

/// A database: `Database my::Db ( include …  Schema …  Table …  Join … )`.
#[derive(Debug, Clone, PartialEq)]
pub struct Database {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values, a documentation literal's first.
    pub tagged_values: Vec<TaggedValue>,
    /// `include my::Other`: the databases included, each positioned on its
    /// whole `include`.
    pub includes: Vec<Include>,
    /// `include StoreType my::Store`: other stores included.
    pub included_stores: Vec<IncludedStore>,
    /// The schemas, then a `default` schema, positioned on the database, for
    /// the tables, views and tabular functions declared outside one.
    pub schemas: Vec<Schema>,
    /// The joins.
    pub joins: Vec<Join>,
    /// The filters, then the multi-grain filters.
    pub filters: Vec<Filter>,
    /// From the documentation, or `Database`, to `)`.
    pub source_info: SourceInfo,
}

/// `include my::Other`.
#[derive(Debug, Clone, PartialEq)]
pub struct Include {
    /// The database's path.
    pub path: String,
    /// The whole `include`.
    pub source_info: SourceInfo,
}

/// `include StoreType my::Store`.
#[derive(Debug, Clone, PartialEq)]
pub struct IncludedStore {
    /// The kind of store.
    pub store_type: SmolStr,
    /// The store's path.
    pub path: String,
    /// The whole `include`.
    pub source_info: SourceInfo,
}

/// `Schema name ( Table …  View … )`.
#[derive(Debug, Clone, PartialEq)]
pub struct Schema {
    /// The name as written, quotes included, as the engine keeps it.
    pub name: SmolStr,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values, a documentation literal's first.
    pub tagged_values: Vec<TaggedValue>,
    /// The tables.
    pub tables: Vec<Table>,
    /// The views.
    pub views: Vec<View>,
    /// The tabular functions.
    pub tabular_functions: Vec<TabularFunction>,
    /// The whole schema.
    pub source_info: SourceInfo,
}

/// `Table name ( milestoning(…) column TYPE PRIMARY KEY, … )`.
#[derive(Debug, Clone, PartialEq)]
pub struct Table {
    /// The name as written, quotes included.
    pub name: SmolStr,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values, a documentation literal's first.
    pub tagged_values: Vec<TaggedValue>,
    /// The columns.
    pub columns: Vec<Column>,
    /// The primary key's columns, as written.
    pub primary_key: Vec<SmolStr>,
    /// How the table's rows are milestoned.
    pub milestoning: Vec<Milestoning>,
    /// The whole table.
    pub source_info: SourceInfo,
}

/// A column: `name VARCHAR(200) NOT NULL`.
#[derive(Debug, Clone, PartialEq)]
pub struct Column {
    /// The name as written, quotes included.
    pub name: SmolStr,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values, a documentation literal's first.
    pub tagged_values: Vec<TaggedValue>,
    /// The column's type.
    pub data_type: DataType,
    /// Neither `PRIMARY KEY` nor `NOT NULL`.
    pub nullable: bool,
    /// The whole column.
    pub source_info: SourceInfo,
}

/// A column's type.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DataType {
    /// `CHAR(size)`.
    Char(i32),
    /// `VARCHAR(size)`.
    Varchar(i32),
    /// `BINARY(size)`.
    Binary(i32),
    /// `VARBINARY(size)`.
    Varbinary(i32),
    /// `BIT`.
    Bit,
    /// `INT` or `INTEGER`.
    Integer,
    /// `BIGINT`.
    BigInt,
    /// `SMALLINT`.
    SmallInt,
    /// `TINYINT`.
    TinyInt,
    /// `TIMESTAMP`.
    Timestamp,
    /// `DATE`.
    Date,
    /// `NUMERIC(precision, scale)`.
    Numeric {
        /// The precision.
        precision: i32,
        /// The scale.
        scale: i32,
    },
    /// `DECIMAL(precision, scale)`.
    Decimal {
        /// The precision.
        precision: i32,
        /// The scale.
        scale: i32,
    },
    /// `FLOAT`.
    Float,
    /// `DOUBLE`.
    Double,
    /// `REAL`.
    Real,
    /// `OTHER` or `ARRAY`.
    Other,
    /// `SEMISTRUCTURED`.
    SemiStructured,
    /// `JSON`.
    Json,
}

/// How a table's rows are milestoned: `business(…)` or `processing(…)`.
#[derive(Debug, Clone, PartialEq)]
pub enum Milestoning {
    /// `business(BUS_FROM = from, BUS_THRU = thru, …)`, positioned from
    /// `BUS_FROM` to the last value.
    Business {
        /// The from column, as written.
        from: SmolStr,
        /// The thru column, as written.
        thru: SmolStr,
        /// `THRU_IS_INCLUSIVE = true`.
        thru_is_inclusive: bool,
        /// `INFINITY_DATE = %…`.
        infinity_date: Option<InfinityDate>,
        /// Where it is.
        source_info: SourceInfo,
    },
    /// `business(BUS_SNAPSHOT_DATE = column)`.
    BusinessSnapshot {
        /// The snapshot date column.
        snapshot_date: SmolStr,
        /// From `BUS_SNAPSHOT_DATE` to the column.
        source_info: SourceInfo,
    },
    /// `processing(PROCESSING_IN = in, PROCESSING_OUT = out, …)`, positioned
    /// from `processing` to `)`.
    Processing {
        /// The in column, as written.
        in_column: SmolStr,
        /// The out column, as written.
        out_column: SmolStr,
        /// `OUT_IS_INCLUSIVE = true`.
        out_is_inclusive: bool,
        /// `INFINITY_DATE = %…`.
        infinity_date: Option<InfinityDate>,
        /// Where it is.
        source_info: SourceInfo,
    },
    /// `processing(PROCESSING_SNAPSHOT_DATE = column)`.
    ProcessingSnapshot {
        /// The snapshot date column.
        snapshot_date: SmolStr,
        /// From `PROCESSING_SNAPSHOT_DATE` to the column.
        source_info: SourceInfo,
    },
}

/// A milestoning's `INFINITY_DATE = %9999-12-31`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct InfinityDate {
    /// `%YYYY-MM-DD`, as opposed to a date-time.
    pub strict: bool,
    /// The date without its `%`.
    pub value: String,
    /// The date.
    pub source_info: SourceInfo,
}

/// `View name ( ~filter F ~groupBy(…) ~distinct column: operation, … )`.
#[derive(Debug, Clone, PartialEq)]
pub struct View {
    /// The name as written, quotes included.
    pub name: SmolStr,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values, a documentation literal's first.
    pub tagged_values: Vec<TaggedValue>,
    /// `~distinct`.
    pub distinct: bool,
    /// `~filter …`.
    pub filter: Option<FilterMapping>,
    /// `~groupBy(…)`.
    pub group_by: Vec<Operation>,
    /// The columns.
    pub column_mappings: Vec<ColumnMapping>,
    /// The columns marked `PRIMARY KEY`.
    pub primary_key: Vec<SmolStr>,
    /// The whole view.
    pub source_info: SourceInfo,
}

/// A view's `~filter [db] @J1 > @J2 | [db] FilterName`.
#[derive(Debug, Clone, PartialEq)]
pub struct FilterMapping {
    /// The filter's database, when given.
    pub database: Option<String>,
    /// The filter.
    pub filter: SmolStr,
    /// The joins leading to the filter's table.
    pub joins: Vec<JoinPointer>,
    /// The whole `~filter …`.
    pub source_info: SourceInfo,
}

/// A view's column: `name : operation PRIMARY KEY`.
#[derive(Debug, Clone, PartialEq)]
pub struct ColumnMapping {
    /// The name as written.
    pub name: SmolStr,
    /// Its value.
    pub operation: Operation,
    /// The whole column.
    pub source_info: SourceInfo,
}

/// `TabularFunction name ( column TYPE, … )`.
#[derive(Debug, Clone, PartialEq)]
pub struct TabularFunction {
    /// The name as written, quotes included.
    pub name: SmolStr,
    /// The columns.
    pub columns: Vec<Column>,
    /// The whole function.
    pub source_info: SourceInfo,
}

/// `Join name ( operation )`.
#[derive(Debug, Clone, PartialEq)]
pub struct Join {
    /// The name.
    pub name: SmolStr,
    /// The join condition.
    pub operation: Operation,
    /// The whole join.
    pub source_info: SourceInfo,
}

/// `Filter name ( operation )` or `MultiGrainFilter name ( operation )`.
#[derive(Debug, Clone, PartialEq)]
pub struct Filter {
    /// The name.
    pub name: SmolStr,
    /// A `MultiGrainFilter`.
    pub multi_grain: bool,
    /// The condition.
    pub operation: Operation,
    /// The whole filter.
    pub source_info: SourceInfo,
}

/// A relational operation, the engine's `RelationalOperationElement`.
#[derive(Debug, Clone, PartialEq)]
pub enum Operation {
    /// A function, an operator (`equal`, `and`, `isNull`, …), or `group`
    /// for parentheses.
    DynaFunc {
        /// The function.
        name: SmolStr,
        /// Its arguments.
        parameters: Vec<Operation>,
        /// Where the engine positions it.
        source_info: SourceInfo,
    },
    /// A table's column.
    Column(TableAliasColumn),
    /// `'text'`, `1` or `1.5`.
    Literal {
        /// The value.
        value: LiteralValue,
        /// Where the engine positions it.
        source_info: SourceInfo,
    },
    /// A function's `[a, b]` argument: each item held by an unpositioned
    /// literal.
    LiteralList {
        /// The items.
        items: Vec<Operation>,
        /// The whole `[…]`.
        source_info: SourceInfo,
    },
    /// `@J1 > @J2 | element`.
    ElementWithJoins {
        /// The joins.
        joins: Vec<JoinPointer>,
        /// What follows `|`.
        element: Option<Box<Operation>>,
        /// The whole operation.
        source_info: SourceInfo,
    },
    /// A function's `x | body` or `(x, y | body)` argument.
    Lambda {
        /// The parameters.
        parameters: Vec<SmolStr>,
        /// The body.
        body: Box<Operation>,
        /// The whole lambda.
        source_info: SourceInfo,
    },
    /// `$x` in a lambda's body.
    LambdaParameter {
        /// The parameter.
        name: SmolStr,
        /// Where the engine positions it.
        source_info: SourceInfo,
    },
}

impl Operation {
    /// Where the engine positions the operation.
    #[must_use]
    pub fn source_info(&self) -> &SourceInfo {
        match self {
            Self::DynaFunc { source_info, .. }
            | Self::Literal { source_info, .. }
            | Self::LiteralList { source_info, .. }
            | Self::ElementWithJoins { source_info, .. }
            | Self::Lambda { source_info, .. }
            | Self::LambdaParameter { source_info, .. } => source_info,
            Self::Column(column) => &column.source_info,
        }
    }

    /// Positions the operation elsewhere: the engine's walker repositions an
    /// operand on the whole comparison it is the left side of.
    pub fn set_source_info(&mut self, source_info: SourceInfo) {
        match self {
            Self::DynaFunc {
                source_info: si, ..
            }
            | Self::Literal {
                source_info: si, ..
            }
            | Self::LiteralList {
                source_info: si, ..
            }
            | Self::ElementWithJoins {
                source_info: si, ..
            }
            | Self::Lambda {
                source_info: si, ..
            }
            | Self::LambdaParameter {
                source_info: si, ..
            } => *si = source_info,
            Self::Column(column) => column.source_info = source_info,
        }
    }
}

/// A literal's value.
#[derive(Debug, Clone, PartialEq)]
pub enum LiteralValue {
    /// `'text'`, unescaped.
    String(String),
    /// `1`, `-1`.
    Integer(i32),
    /// `1.5`, `-1.5`.
    Float(f64),
}

/// A column: `table.column`, `schema.table.column`, `{target}.column`, or
/// `column` where the scope names the table.
#[derive(Debug, Clone, PartialEq)]
pub struct TableAliasColumn {
    /// The column as written.
    pub column: SmolStr,
    /// Its table.
    pub table: TablePtr,
    /// Where the engine positions it.
    pub source_info: SourceInfo,
}

/// A table, or an alias of one, the engine's `TablePtr`.
#[derive(Debug, Clone, PartialEq)]
pub struct TablePtr {
    /// The database, when known.
    pub database: Option<String>,
    /// The schema as written, or `default`.
    pub schema: SmolStr,
    /// The table or alias as written.
    pub table: SmolStr,
    /// The table, or the schema to the table.
    pub source_info: SourceInfo,
}

/// `@Name`, one join of a sequence, the engine's `JoinPointer`.
#[derive(Debug, Clone, PartialEq)]
pub struct JoinPointer {
    /// The database, when known.
    pub database: Option<String>,
    /// `(INNER)` or `(OUTER)`.
    pub join_type: Option<SmolStr>,
    /// The join.
    pub name: SmolStr,
    /// `@Name`.
    pub source_info: SourceInfo,
}

impl Spanned for Database {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for Database {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &self.stereotypes
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &self.tagged_values
    }
}

impl PackageableElement for Database {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for Database {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn DSLElement> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn DSLElement) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}
