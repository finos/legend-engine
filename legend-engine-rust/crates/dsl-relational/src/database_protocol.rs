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

//! `ElementProtocol` for databases: the engine's relational `Database` and
//! what it holds (`Schema`, `Table`, `Column` and its data types, `View`,
//! `Join`, `Filter`, milestoning and `RelationalOperationElement`s).
//!
//! Stereotypes are positioned on their value, as every engine section walker
//! but the Pure one positions them.

use legend_pure_parser_ast::annotation::{StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_protocol::element_protocol::{
    ConvertError, ELEMENT_PROTOCOLS, ElementProtocol, Result,
};
use legend_pure_parser_protocol::v1::annotation;
use legend_pure_parser_protocol::v1::convert::section_stereotype;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;
use serde::Serialize;
use serde_json::value::{RawValue, to_raw_value};

use crate::ast::{
    Column, ColumnMapping, DataType, Database, Filter, FilterMapping, IncludedStore, InfinityDate,
    Join, JoinPointer, LiteralValue, Milestoning, Operation, SECTION_KIND, Schema, Table,
    TableAliasColumn, TablePtr, TabularFunction, View,
};

/// Protocol converter for databases.
pub struct DatabaseProtocol;

impl ElementProtocol for DatabaseProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Box<RawValue>> {
        let database = element.as_any().downcast_ref::<Database>().ok_or_else(|| {
            ConvertError::plugin(
                format!(
                    "DatabaseProtocol: a '{}' element is not a Database",
                    element.kind()
                ),
                None,
            )
        })?;
        Ok(to_raw_value(&DatabaseJson::from(database))?)
    }
}

/// The engine's relational `Database`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "relational", rename_all = "camelCase")]
struct DatabaseJson<'a> {
    package: String,
    name: &'a str,
    stereotypes: Vec<annotation::StereotypePtr>,
    tagged_values: Vec<annotation::TaggedValue>,
    included_stores: Vec<IncludeJson<'a>>,
    schemas: Vec<SchemaJson<'a>>,
    joins: Vec<JoinJson<'a>>,
    filters: Vec<FilterJson<'a>>,
    source_information: SourceInformation,
    /// Null, so absent, when there are none.
    #[serde(skip_serializing_if = "Vec::is_empty")]
    included_store_specifications: Vec<IncludedStoreJson<'a>>,
}

impl<'a> From<&'a Database> for DatabaseJson<'a> {
    fn from(database: &'a Database) -> Self {
        Self {
            package: database
                .package
                .as_ref()
                .map(ToString::to_string)
                .unwrap_or_default(),
            name: database.name.value.as_str(),
            stereotypes: stereotypes(&database.stereotypes),
            tagged_values: tagged_values(&database.tagged_values),
            included_stores: database
                .includes
                .iter()
                .map(|include| IncludeJson {
                    element_type: "STORE",
                    path: &include.path,
                    source_information: position(&include.source_info),
                })
                .collect(),
            schemas: database.schemas.iter().map(SchemaJson::from).collect(),
            joins: database.joins.iter().map(JoinJson::from).collect(),
            filters: database.filters.iter().map(FilterJson::from).collect(),
            source_information: position(&database.source_info),
            included_store_specifications: database
                .included_stores
                .iter()
                .map(IncludedStoreJson::from)
                .collect(),
        }
    }
}

/// An included database's pointer.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct IncludeJson<'a> {
    #[serde(rename = "type")]
    element_type: &'static str,
    path: &'a str,
    source_information: SourceInformation,
}

/// The engine's `IncludedStoreSpecification`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct IncludedStoreJson<'a> {
    store_type: &'a str,
    packageable_element_pointer: PointerJson<'a>,
    source_information: SourceInformation,
}

impl<'a> From<&'a IncludedStore> for IncludedStoreJson<'a> {
    fn from(store: &'a IncludedStore) -> Self {
        Self {
            store_type: store.store_type.as_str(),
            packageable_element_pointer: PointerJson {
                path: &store.path,
                source_information: position(&store.source_info),
            },
            source_information: position(&store.source_info),
        }
    }
}

/// A packageable element pointer without a type.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct PointerJson<'a> {
    path: &'a str,
    source_information: SourceInformation,
}

fn position(source_info: &SourceInfo) -> SourceInformation {
    SourceInformation::from(source_info)
}

fn stereotypes(stereotypes: &[StereotypePtr]) -> Vec<annotation::StereotypePtr> {
    stereotypes.iter().map(section_stereotype).collect()
}

fn tagged_values(tagged_values: &[TaggedValue]) -> Vec<annotation::TaggedValue> {
    tagged_values
        .iter()
        .map(annotation::TaggedValue::from)
        .collect()
}

/// The engine's `Schema`. The walker leaves a schema's, table's, column's or
/// view's stereotypes null when there are none, where the protocol reads `[]`
/// as an empty list: they are written only when there are some.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct SchemaJson<'a> {
    name: &'a str,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    stereotypes: Vec<annotation::StereotypePtr>,
    tagged_values: Vec<annotation::TaggedValue>,
    tables: Vec<TableJson<'a>>,
    views: Vec<ViewJson<'a>>,
    tabular_functions: Vec<TabularFunctionJson<'a>>,
    source_information: SourceInformation,
}

impl<'a> From<&'a Schema> for SchemaJson<'a> {
    fn from(schema: &'a Schema) -> Self {
        Self {
            name: schema.name.as_str(),
            stereotypes: stereotypes(&schema.stereotypes),
            tagged_values: tagged_values(&schema.tagged_values),
            tables: schema.tables.iter().map(TableJson::from).collect(),
            views: schema.views.iter().map(ViewJson::from).collect(),
            tabular_functions: schema
                .tabular_functions
                .iter()
                .map(TabularFunctionJson::from)
                .collect(),
            source_information: position(&schema.source_info),
        }
    }
}

/// The engine's `Table`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct TableJson<'a> {
    name: &'a str,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    stereotypes: Vec<annotation::StereotypePtr>,
    tagged_values: Vec<annotation::TaggedValue>,
    columns: Vec<ColumnJson<'a>>,
    primary_key: Vec<&'a str>,
    milestoning: Vec<MilestoningJson<'a>>,
    source_information: SourceInformation,
}

impl<'a> From<&'a Table> for TableJson<'a> {
    fn from(table: &'a Table) -> Self {
        Self {
            name: table.name.as_str(),
            stereotypes: stereotypes(&table.stereotypes),
            tagged_values: tagged_values(&table.tagged_values),
            columns: table.columns.iter().map(ColumnJson::from).collect(),
            primary_key: table
                .primary_key
                .iter()
                .map(smol_str::SmolStr::as_str)
                .collect(),
            milestoning: table
                .milestoning
                .iter()
                .map(MilestoningJson::from)
                .collect(),
            source_information: position(&table.source_info),
        }
    }
}

/// The engine's `Column`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ColumnJson<'a> {
    name: &'a str,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    stereotypes: Vec<annotation::StereotypePtr>,
    tagged_values: Vec<annotation::TaggedValue>,
    #[serde(rename = "type")]
    data_type: DataTypeJson,
    nullable: bool,
    source_information: SourceInformation,
}

impl<'a> From<&'a Column> for ColumnJson<'a> {
    fn from(column: &'a Column) -> Self {
        Self {
            name: column.name.as_str(),
            stereotypes: stereotypes(&column.stereotypes),
            tagged_values: tagged_values(&column.tagged_values),
            data_type: DataTypeJson::from(column.data_type),
            nullable: column.nullable,
            source_information: position(&column.source_info),
        }
    }
}

/// The engine's column `DataType`s.
#[derive(Serialize)]
#[serde(tag = "_type")]
enum DataTypeJson {
    Char { size: i32 },
    Varchar { size: i32 },
    Binary { size: i32 },
    Varbinary { size: i32 },
    Numeric { precision: i32, scale: i32 },
    Decimal { precision: i32, scale: i32 },
    Bit,
    Integer,
    BigInt,
    SmallInt,
    TinyInt,
    Timestamp,
    Date,
    Float,
    Double,
    Real,
    Other,
    SemiStructured,
    Json,
}

impl From<DataType> for DataTypeJson {
    fn from(data_type: DataType) -> Self {
        match data_type {
            DataType::Char(size) => Self::Char { size },
            DataType::Varchar(size) => Self::Varchar { size },
            DataType::Binary(size) => Self::Binary { size },
            DataType::Varbinary(size) => Self::Varbinary { size },
            DataType::Numeric { precision, scale } => Self::Numeric { precision, scale },
            DataType::Decimal { precision, scale } => Self::Decimal { precision, scale },
            DataType::Bit => Self::Bit,
            DataType::Integer => Self::Integer,
            DataType::BigInt => Self::BigInt,
            DataType::SmallInt => Self::SmallInt,
            DataType::TinyInt => Self::TinyInt,
            DataType::Timestamp => Self::Timestamp,
            DataType::Date => Self::Date,
            DataType::Float => Self::Float,
            DataType::Double => Self::Double,
            DataType::Real => Self::Real,
            DataType::Other => Self::Other,
            DataType::SemiStructured => Self::SemiStructured,
            DataType::Json => Self::Json,
        }
    }
}

/// The engine's `Milestoning`s.
#[derive(Serialize)]
#[serde(tag = "_type")]
enum MilestoningJson<'a> {
    #[serde(rename = "businessMilestoning", rename_all = "camelCase")]
    Business {
        from: &'a str,
        thru: &'a str,
        thru_is_inclusive: bool,
        infinity_date: Option<InfinityDateJson<'a>>,
        source_information: SourceInformation,
    },
    #[serde(rename = "businessSnapshotMilestoning", rename_all = "camelCase")]
    BusinessSnapshot {
        snapshot_date: &'a str,
        source_information: SourceInformation,
    },
    #[serde(rename = "processingMilestoning", rename_all = "camelCase")]
    Processing {
        #[serde(rename = "in")]
        in_column: &'a str,
        #[serde(rename = "out")]
        out_column: &'a str,
        out_is_inclusive: bool,
        infinity_date: Option<InfinityDateJson<'a>>,
        source_information: SourceInformation,
    },
    #[serde(rename = "processingSnapshotMilestoning", rename_all = "camelCase")]
    ProcessingSnapshot {
        snapshot_date: &'a str,
        source_information: SourceInformation,
    },
}

impl<'a> From<&'a Milestoning> for MilestoningJson<'a> {
    fn from(milestoning: &'a Milestoning) -> Self {
        match milestoning {
            Milestoning::Business {
                from,
                thru,
                thru_is_inclusive,
                infinity_date,
                source_info,
            } => Self::Business {
                from: from.as_str(),
                thru: thru.as_str(),
                thru_is_inclusive: *thru_is_inclusive,
                infinity_date: infinity_date.as_ref().map(InfinityDateJson::from),
                source_information: position(source_info),
            },
            Milestoning::BusinessSnapshot {
                snapshot_date,
                source_info,
            } => Self::BusinessSnapshot {
                snapshot_date: snapshot_date.as_str(),
                source_information: position(source_info),
            },
            Milestoning::Processing {
                in_column,
                out_column,
                out_is_inclusive,
                infinity_date,
                source_info,
            } => Self::Processing {
                in_column: in_column.as_str(),
                out_column: out_column.as_str(),
                out_is_inclusive: *out_is_inclusive,
                infinity_date: infinity_date.as_ref().map(InfinityDateJson::from),
                source_information: position(source_info),
            },
            Milestoning::ProcessingSnapshot {
                snapshot_date,
                source_info,
            } => Self::ProcessingSnapshot {
                snapshot_date: snapshot_date.as_str(),
                source_information: position(source_info),
            },
        }
    }
}

/// A `CStrictDate` or `CDateTime`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct InfinityDateJson<'a> {
    #[serde(rename = "_type")]
    date_type: &'static str,
    value: &'a str,
    source_information: SourceInformation,
}

impl<'a> From<&'a InfinityDate> for InfinityDateJson<'a> {
    fn from(date: &'a InfinityDate) -> Self {
        Self {
            date_type: if date.strict {
                "strictDate"
            } else {
                "dateTime"
            },
            value: &date.value,
            source_information: position(&date.source_info),
        }
    }
}

/// The engine's `View`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ViewJson<'a> {
    name: &'a str,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    stereotypes: Vec<annotation::StereotypePtr>,
    tagged_values: Vec<annotation::TaggedValue>,
    distinct: bool,
    filter: Option<FilterMappingJson<'a>>,
    group_by: Vec<OperationJson<'a>>,
    column_mappings: Vec<ColumnMappingJson<'a>>,
    primary_key: Vec<&'a str>,
    source_information: SourceInformation,
}

impl<'a> From<&'a View> for ViewJson<'a> {
    fn from(view: &'a View) -> Self {
        Self {
            name: view.name.as_str(),
            stereotypes: stereotypes(&view.stereotypes),
            tagged_values: tagged_values(&view.tagged_values),
            distinct: view.distinct,
            filter: view.filter.as_ref().map(FilterMappingJson::from),
            group_by: view.group_by.iter().map(OperationJson::from).collect(),
            column_mappings: view
                .column_mappings
                .iter()
                .map(ColumnMappingJson::from)
                .collect(),
            primary_key: view
                .primary_key
                .iter()
                .map(smol_str::SmolStr::as_str)
                .collect(),
            source_information: position(&view.source_info),
        }
    }
}

/// The engine's `FilterMapping`: a view's or a class mapping's filter.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct FilterMappingJson<'a> {
    filter: FilterPointerJson<'a>,
    joins: Vec<JoinPointerJson<'a>>,
    source_information: SourceInformation,
}

impl<'a> FilterMappingJson<'a> {
    /// The filter `name` of database `db`, reached through `joins`.
    pub(crate) fn new(
        db: Option<&'a str>,
        name: &'a str,
        joins: &'a [JoinPointer],
        source_info: &SourceInfo,
    ) -> Self {
        Self {
            filter: FilterPointerJson { db, name },
            joins: joins.iter().map(JoinPointerJson::from).collect(),
            source_information: position(source_info),
        }
    }
}

impl<'a> From<&'a FilterMapping> for FilterMappingJson<'a> {
    fn from(filter: &'a FilterMapping) -> Self {
        Self::new(
            filter.database.as_deref(),
            filter.filter.as_str(),
            &filter.joins,
            &filter.source_info,
        )
    }
}

/// The engine's `FilterPointer`.
#[derive(Serialize)]
struct FilterPointerJson<'a> {
    db: Option<&'a str>,
    name: &'a str,
}

/// A view's column mapping.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ColumnMappingJson<'a> {
    name: &'a str,
    operation: OperationJson<'a>,
    source_information: SourceInformation,
}

impl<'a> From<&'a ColumnMapping> for ColumnMappingJson<'a> {
    fn from(mapping: &'a ColumnMapping) -> Self {
        Self {
            name: mapping.name.as_str(),
            operation: OperationJson::from(&mapping.operation),
            source_information: position(&mapping.source_info),
        }
    }
}

/// The engine's `TabularFunction`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct TabularFunctionJson<'a> {
    name: &'a str,
    columns: Vec<ColumnJson<'a>>,
    source_information: SourceInformation,
}

impl<'a> From<&'a TabularFunction> for TabularFunctionJson<'a> {
    fn from(function: &'a TabularFunction) -> Self {
        Self {
            name: function.name.as_str(),
            columns: function.columns.iter().map(ColumnJson::from).collect(),
            source_information: position(&function.source_info),
        }
    }
}

/// The engine's `Join`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct JoinJson<'a> {
    name: &'a str,
    operation: OperationJson<'a>,
    source_information: SourceInformation,
}

impl<'a> From<&'a Join> for JoinJson<'a> {
    fn from(join: &'a Join) -> Self {
        Self {
            name: join.name.as_str(),
            operation: OperationJson::from(&join.operation),
            source_information: position(&join.source_info),
        }
    }
}

/// The engine's `Filter` or `MultiGrainFilter`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct FilterJson<'a> {
    #[serde(rename = "_type")]
    filter_type: &'static str,
    name: &'a str,
    operation: OperationJson<'a>,
    source_information: SourceInformation,
}

impl<'a> From<&'a Filter> for FilterJson<'a> {
    fn from(filter: &'a Filter) -> Self {
        Self {
            filter_type: if filter.multi_grain {
                "multigrain"
            } else {
                "filter"
            },
            name: filter.name.as_str(),
            operation: OperationJson::from(&filter.operation),
            source_information: position(&filter.source_info),
        }
    }
}

/// A relational operation, the engine's `RelationalOperationElement`.
#[derive(Serialize)]
#[serde(tag = "_type")]
pub(crate) enum OperationJson<'a> {
    #[serde(rename = "dynaFunc", rename_all = "camelCase")]
    DynaFunc {
        func_name: &'a str,
        parameters: Vec<OperationJson<'a>>,
        source_information: SourceInformation,
    },
    #[serde(rename = "column")]
    Column(TableAliasColumnJson<'a>),
    #[serde(rename = "literal", rename_all = "camelCase")]
    Literal {
        value: LiteralJson<'a>,
        source_information: SourceInformation,
    },
    #[serde(rename = "literalList", rename_all = "camelCase")]
    LiteralList {
        values: Vec<LiteralItemJson<'a>>,
        source_information: SourceInformation,
    },
    #[serde(rename = "elemtWithJoins", rename_all = "camelCase")]
    ElementWithJoins {
        joins: Vec<JoinPointerJson<'a>>,
        relational_element: Option<Box<OperationJson<'a>>>,
        source_information: SourceInformation,
    },
    #[serde(rename = "relationalLambda", rename_all = "camelCase")]
    Lambda {
        parameter_names: Vec<&'a str>,
        body: Box<OperationJson<'a>>,
        source_information: SourceInformation,
    },
    #[serde(rename = "lambdaParameter", rename_all = "camelCase")]
    LambdaParameter {
        name: &'a str,
        source_information: SourceInformation,
    },
}

impl<'a> From<&'a Operation> for OperationJson<'a> {
    fn from(operation: &'a Operation) -> Self {
        match operation {
            Operation::DynaFunc {
                name,
                parameters,
                source_info,
            } => Self::DynaFunc {
                func_name: name.as_str(),
                parameters: parameters.iter().map(Self::from).collect(),
                source_information: position(source_info),
            },
            Operation::Column(column) => Self::Column(TableAliasColumnJson::from(column)),
            Operation::Literal { value, source_info } => Self::Literal {
                value: LiteralJson::from(value),
                source_information: position(source_info),
            },
            // Each item held by an unpositioned literal.
            Operation::LiteralList { items, source_info } => Self::LiteralList {
                values: items
                    .iter()
                    .map(|item| LiteralItemJson {
                        literal_type: "literal",
                        value: Self::from(item),
                    })
                    .collect(),
                source_information: position(source_info),
            },
            Operation::ElementWithJoins {
                joins,
                element,
                source_info,
            } => Self::ElementWithJoins {
                joins: joins.iter().map(JoinPointerJson::from).collect(),
                relational_element: element.as_deref().map(|e| Box::new(Self::from(e))),
                source_information: position(source_info),
            },
            Operation::Lambda {
                parameters,
                body,
                source_info,
            } => Self::Lambda {
                parameter_names: parameters.iter().map(smol_str::SmolStr::as_str).collect(),
                body: Box::new(Self::from(body.as_ref())),
                source_information: position(source_info),
            },
            Operation::LambdaParameter { name, source_info } => Self::LambdaParameter {
                name: name.as_str(),
                source_information: position(source_info),
            },
        }
    }
}

/// A literal's value.
#[derive(Serialize)]
#[serde(untagged)]
pub(crate) enum LiteralJson<'a> {
    String(&'a str),
    Integer(i32),
    Float(f64),
}

impl<'a> From<&'a LiteralValue> for LiteralJson<'a> {
    fn from(value: &'a LiteralValue) -> Self {
        match value {
            LiteralValue::String(value) => Self::String(value),
            LiteralValue::Integer(value) => Self::Integer(*value),
            LiteralValue::Float(value) => Self::Float(*value),
        }
    }
}

/// An unpositioned literal holding an item of a literal list.
#[derive(Serialize)]
pub(crate) struct LiteralItemJson<'a> {
    #[serde(rename = "_type")]
    literal_type: &'static str,
    value: OperationJson<'a>,
}

/// A column, the engine's `TableAliasColumn`, its alias its table's name.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct TableAliasColumnJson<'a> {
    column: &'a str,
    table: TablePtrJson<'a>,
    table_alias: &'a str,
    source_information: SourceInformation,
}

impl<'a> From<&'a TableAliasColumn> for TableAliasColumnJson<'a> {
    fn from(column: &'a TableAliasColumn) -> Self {
        Self {
            column: column.column.as_str(),
            table: TablePtrJson::from(&column.table),
            table_alias: column.table.table.as_str(),
            source_information: position(&column.source_info),
        }
    }
}

/// A table, the engine's `TablePtr`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "Table", rename_all = "camelCase")]
pub(crate) struct TablePtrJson<'a> {
    database: Option<&'a str>,
    main_table_db: Option<&'a str>,
    schema: &'a str,
    table: &'a str,
    source_information: SourceInformation,
}

impl<'a> From<&'a TablePtr> for TablePtrJson<'a> {
    fn from(table: &'a TablePtr) -> Self {
        Self {
            database: table.database.as_deref(),
            main_table_db: table.database.as_deref(),
            schema: table.schema.as_str(),
            table: table.table.as_str(),
            source_information: position(&table.source_info),
        }
    }
}

/// A join of a sequence, the engine's `JoinPointer`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct JoinPointerJson<'a> {
    db: Option<&'a str>,
    join_type: Option<&'a str>,
    name: &'a str,
    source_information: SourceInformation,
}

impl<'a> From<&'a JoinPointer> for JoinPointerJson<'a> {
    fn from(join: &'a JoinPointer) -> Self {
        Self {
            db: join.database.as_deref(),
            join_type: join.join_type.as_ref().map(smol_str::SmolStr::as_str),
            name: join.name.as_str(),
            source_information: position(&join.source_info),
        }
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static DATABASE_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &DatabaseProtocol;
