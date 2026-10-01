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
use legend_pure_parser_protocol::v1::convert::section_stereotype;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;
use serde_json::{Value, json};

use crate::ast::{
    Column, DataType, Database, Filter, InfinityDate, JoinPointer, LiteralValue, Milestoning,
    Operation, SECTION_KIND, Schema, Table, TableAliasColumn, TabularFunction, View,
};

/// Protocol converter for databases.
pub struct DatabaseProtocol;

impl ElementProtocol for DatabaseProtocol {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn convert(&self, element: &dyn DSLElement) -> Result<Value> {
        let database = element.as_any().downcast_ref::<Database>().ok_or_else(|| {
            ConvertError::plugin(
                format!(
                    "DatabaseProtocol: a '{}' element is not a Database",
                    element.kind()
                ),
                None,
            )
        })?;
        Ok(database_json(database))
    }
}

fn database_json(database: &Database) -> Value {
    let mut json = json!({
        "_type": "relational",
        "package": database.package.as_ref().map(ToString::to_string).unwrap_or_default(),
        "name": database.name.value.as_str(),
        "stereotypes": stereotypes(&database.stereotypes),
        "taggedValues": tagged_values(&database.tagged_values),
        "includedStores": database.includes.iter().map(|include| json!({
            "type": "STORE",
            "path": include.path,
            "sourceInformation": position(&include.source_info),
        })).collect::<Vec<_>>(),
        "schemas": database.schemas.iter().map(schema).collect::<Vec<_>>(),
        "joins": database.joins.iter().map(|join| json!({
            "name": join.name.as_str(),
            "operation": operation(&join.operation),
            "sourceInformation": position(&join.source_info),
        })).collect::<Vec<_>>(),
        "filters": database.filters.iter().map(filter).collect::<Vec<_>>(),
        "sourceInformation": position(&database.source_info),
    });
    // Null, so absent, when there are none.
    if !database.included_stores.is_empty() {
        json["includedStoreSpecifications"] = database
            .included_stores
            .iter()
            .map(|store| {
                json!({
                    "storeType": store.store_type.as_str(),
                    "packageableElementPointer": {
                        "path": store.path,
                        "sourceInformation": position(&store.source_info),
                    },
                    "sourceInformation": position(&store.source_info),
                })
            })
            .collect();
    }
    json
}

fn position(source_info: &SourceInfo) -> SourceInformation {
    SourceInformation::from(source_info)
}

fn stereotypes(stereotypes: &[StereotypePtr]) -> Value {
    json!(
        stereotypes
            .iter()
            .map(section_stereotype)
            .collect::<Vec<_>>()
    )
}

/// `json` with its `stereotypes`, when there are some: the walker leaves a
/// schema's, table's, column's or view's null otherwise, where the protocol
/// reads `[]` as an empty list.
fn with_stereotypes(mut json: Value, list: &[StereotypePtr]) -> Value {
    if !list.is_empty() {
        json["stereotypes"] = stereotypes(list);
    }
    json
}

fn tagged_values(tagged_values: &[TaggedValue]) -> Value {
    json!(
        tagged_values
            .iter()
            .map(legend_pure_parser_protocol::v1::annotation::TaggedValue::from)
            .collect::<Vec<_>>()
    )
}

fn schema(schema: &Schema) -> Value {
    let json = json!({
        "name": schema.name.as_str(),
        "taggedValues": tagged_values(&schema.tagged_values),
        "tables": schema.tables.iter().map(table).collect::<Vec<_>>(),
        "views": schema.views.iter().map(view).collect::<Vec<_>>(),
        "tabularFunctions": schema.tabular_functions.iter().map(tabular_function).collect::<Vec<_>>(),
        "sourceInformation": position(&schema.source_info),
    });
    with_stereotypes(json, &schema.stereotypes)
}

fn table(table: &Table) -> Value {
    let json = json!({
        "name": table.name.as_str(),
        "taggedValues": tagged_values(&table.tagged_values),
        "columns": table.columns.iter().map(column).collect::<Vec<_>>(),
        "primaryKey": table.primary_key.iter().map(smol_str::SmolStr::as_str).collect::<Vec<_>>(),
        "milestoning": table.milestoning.iter().map(milestoning).collect::<Vec<_>>(),
        "sourceInformation": position(&table.source_info),
    });
    with_stereotypes(json, &table.stereotypes)
}

fn column(column: &Column) -> Value {
    let json = json!({
        "name": column.name.as_str(),
        "taggedValues": tagged_values(&column.tagged_values),
        "type": data_type(column.data_type),
        "nullable": column.nullable,
        "sourceInformation": position(&column.source_info),
    });
    with_stereotypes(json, &column.stereotypes)
}

fn data_type(data_type: DataType) -> Value {
    match data_type {
        DataType::Char(size) => json!({"_type": "Char", "size": size}),
        DataType::Varchar(size) => json!({"_type": "Varchar", "size": size}),
        DataType::Binary(size) => json!({"_type": "Binary", "size": size}),
        DataType::Varbinary(size) => json!({"_type": "Varbinary", "size": size}),
        DataType::Numeric { precision, scale } => {
            json!({"_type": "Numeric", "precision": precision, "scale": scale})
        }
        DataType::Decimal { precision, scale } => {
            json!({"_type": "Decimal", "precision": precision, "scale": scale})
        }
        DataType::Bit => json!({"_type": "Bit"}),
        DataType::Integer => json!({"_type": "Integer"}),
        DataType::BigInt => json!({"_type": "BigInt"}),
        DataType::SmallInt => json!({"_type": "SmallInt"}),
        DataType::TinyInt => json!({"_type": "TinyInt"}),
        DataType::Timestamp => json!({"_type": "Timestamp"}),
        DataType::Date => json!({"_type": "Date"}),
        DataType::Float => json!({"_type": "Float"}),
        DataType::Double => json!({"_type": "Double"}),
        DataType::Real => json!({"_type": "Real"}),
        DataType::Other => json!({"_type": "Other"}),
        DataType::SemiStructured => json!({"_type": "SemiStructured"}),
        DataType::Json => json!({"_type": "Json"}),
    }
}

fn milestoning(milestoning: &Milestoning) -> Value {
    match milestoning {
        Milestoning::Business {
            from,
            thru,
            thru_is_inclusive,
            infinity_date: date,
            source_info,
        } => json!({
            "_type": "businessMilestoning",
            "from": from.as_str(),
            "thru": thru.as_str(),
            "thruIsInclusive": thru_is_inclusive,
            "infinityDate": date.as_ref().map(infinity_date),
            "sourceInformation": position(source_info),
        }),
        Milestoning::BusinessSnapshot {
            snapshot_date,
            source_info,
        } => json!({
            "_type": "businessSnapshotMilestoning",
            "snapshotDate": snapshot_date.as_str(),
            "sourceInformation": position(source_info),
        }),
        Milestoning::Processing {
            in_column,
            out_column,
            out_is_inclusive,
            infinity_date: date,
            source_info,
        } => json!({
            "_type": "processingMilestoning",
            "in": in_column.as_str(),
            "out": out_column.as_str(),
            "outIsInclusive": out_is_inclusive,
            "infinityDate": date.as_ref().map(infinity_date),
            "sourceInformation": position(source_info),
        }),
        Milestoning::ProcessingSnapshot {
            snapshot_date,
            source_info,
        } => json!({
            "_type": "processingSnapshotMilestoning",
            "snapshotDate": snapshot_date.as_str(),
            "sourceInformation": position(source_info),
        }),
    }
}

/// A `CStrictDate` or `CDateTime`.
fn infinity_date(date: &InfinityDate) -> Value {
    json!({
        "_type": if date.strict { "strictDate" } else { "dateTime" },
        "value": date.value,
        "sourceInformation": position(&date.source_info),
    })
}

fn view(view: &View) -> Value {
    let json = json!({
        "name": view.name.as_str(),
        "taggedValues": tagged_values(&view.tagged_values),
        "distinct": view.distinct,
        "filter": view.filter.as_ref().map(|filter| json!({
            "filter": {
                "db": filter.database,
                "name": filter.filter.as_str(),
            },
            "joins": filter.joins.iter().map(join_pointer).collect::<Vec<_>>(),
            "sourceInformation": position(&filter.source_info),
        })),
        "groupBy": view.group_by.iter().map(operation).collect::<Vec<_>>(),
        "columnMappings": view.column_mappings.iter().map(|mapping| json!({
            "name": mapping.name.as_str(),
            "operation": operation(&mapping.operation),
            "sourceInformation": position(&mapping.source_info),
        })).collect::<Vec<_>>(),
        "primaryKey": view.primary_key.iter().map(smol_str::SmolStr::as_str).collect::<Vec<_>>(),
        "sourceInformation": position(&view.source_info),
    });
    with_stereotypes(json, &view.stereotypes)
}

fn tabular_function(function: &TabularFunction) -> Value {
    json!({
        "name": function.name.as_str(),
        "columns": function.columns.iter().map(column).collect::<Vec<_>>(),
        "sourceInformation": position(&function.source_info),
    })
}

fn filter(filter: &Filter) -> Value {
    json!({
        "_type": if filter.multi_grain { "multigrain" } else { "filter" },
        "name": filter.name.as_str(),
        "operation": operation(&filter.operation),
        "sourceInformation": position(&filter.source_info),
    })
}

/// A relational operation, the engine's `RelationalOperationElement`.
#[must_use]
pub fn operation(operation: &Operation) -> Value {
    match operation {
        Operation::DynaFunc {
            name,
            parameters,
            source_info,
        } => json!({
            "_type": "dynaFunc",
            "funcName": name.as_str(),
            "parameters": parameters.iter().map(self::operation).collect::<Vec<_>>(),
            "sourceInformation": position(source_info),
        }),
        Operation::Column(column) => table_alias_column(column),
        Operation::Literal { value, source_info } => json!({
            "_type": "literal",
            "value": literal_value(value),
            "sourceInformation": position(source_info),
        }),
        // Each item held by an unpositioned literal.
        Operation::LiteralList { items, source_info } => json!({
            "_type": "literalList",
            "values": items.iter().map(|item| json!({
                "_type": "literal",
                "value": self::operation(item),
            })).collect::<Vec<_>>(),
            "sourceInformation": position(source_info),
        }),
        Operation::ElementWithJoins {
            joins,
            element,
            source_info,
        } => json!({
            "_type": "elemtWithJoins",
            "joins": joins.iter().map(join_pointer).collect::<Vec<_>>(),
            "relationalElement": element.as_deref().map(self::operation),
            "sourceInformation": position(source_info),
        }),
        Operation::Lambda {
            parameters,
            body,
            source_info,
        } => json!({
            "_type": "relationalLambda",
            "parameterNames": parameters.iter().map(smol_str::SmolStr::as_str).collect::<Vec<_>>(),
            "body": self::operation(body),
            "sourceInformation": position(source_info),
        }),
        Operation::LambdaParameter { name, source_info } => json!({
            "_type": "lambdaParameter",
            "name": name.as_str(),
            "sourceInformation": position(source_info),
        }),
    }
}

fn literal_value(value: &LiteralValue) -> Value {
    match value {
        LiteralValue::String(value) => json!(value),
        LiteralValue::Integer(value) => json!(value),
        LiteralValue::Float(value) => json!(value),
    }
}

/// A column, the engine's `TableAliasColumn`, its alias its table's name.
#[must_use]
pub fn table_alias_column(column: &TableAliasColumn) -> Value {
    let table = &column.table;
    json!({
        "_type": "column",
        "column": column.column.as_str(),
        "table": {
            "_type": "Table",
            "database": table.database,
            "mainTableDb": table.database,
            "schema": table.schema.as_str(),
            "table": table.table.as_str(),
            "sourceInformation": position(&table.source_info),
        },
        "tableAlias": table.table.as_str(),
        "sourceInformation": position(&column.source_info),
    })
}

/// A join of a sequence, the engine's `JoinPointer`.
#[must_use]
pub fn join_pointer(join: &JoinPointer) -> Value {
    json!({
        "db": join.database,
        "joinType": join.join_type.as_ref().map(smol_str::SmolStr::as_str),
        "name": join.name.as_str(),
        "sourceInformation": position(&join.source_info),
    })
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static DATABASE_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &DatabaseProtocol;
