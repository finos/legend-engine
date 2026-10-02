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

//! The `###QueryPostProcessor` section, as the engine's
//! `RelationalMapperParseTreeWalker` reads it.

use legend_pure_dsl_relational as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The mapper of the section `###QueryPostProcessor\n{body}`.
fn mapper(body: &str) -> Value {
    let source = format!("###QueryPostProcessor\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "q.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

fn error(body: &str) -> ParseError {
    let source = format!("###QueryPostProcessor\n{body}");
    legend_pure_parser_parser::parse(&source, "q.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    serde_json::to_value(SourceInformation {
        source_id: "q.pure".into(),
        start_line,
        start_column,
        end_line,
        end_column,
    })
    .expect("a position serializes")
}

fn position(error: &ParseError) -> (u32, u32, u32, u32) {
    let si = error.source_info().expect("positioned");
    (si.start_line, si.start_column, si.end_line, si.end_column)
}

#[test]
fn a_mapper_renames_databases_schemas_and_tables() {
    let mapper = mapper(
        "RelationalMapper my::Renames\n(\n  DatabaseMappers: [ [my::Db.sales, my::Db.default] -> 'SalesDb' ];\n  SchemaMappers: [ my::Db.sales -> 'Sales' ];\n  TableMappers: [ my::Db.sales.ORDERS -> 'Orders' ];\n)\n",
    );
    assert_eq!(mapper["_type"], "relationalMapper");
    assert_eq!(mapper["name"], "Renames");
    assert_eq!(mapper["sourceInformation"], si(2, 1, 7, 1));
    // Pointers positioned on themselves, their database as written.
    assert_eq!(
        mapper["databaseMappers"],
        json!([{
            "databaseName": "SalesDb",
            "schemas": [
                {"_type": "Schema", "database": "my::Db", "schema": "sales", "sourceInformation": si(4, 23, 4, 34)},
                {"_type": "Schema", "database": "my::Db", "schema": "default", "sourceInformation": si(4, 37, 4, 50)},
            ],
        }])
    );
    assert_eq!(
        mapper["schemaMappers"],
        json!([{
            "from": {"_type": "Schema", "database": "my::Db", "schema": "sales", "sourceInformation": si(5, 20, 5, 31)},
            "to": "Sales",
        }])
    );
    assert_eq!(
        mapper["tableMappers"],
        json!([{
            "from": {"_type": "Table", "database": "my::Db", "schema": "sales", "table": "ORDERS", "sourceInformation": si(6, 19, 6, 37)},
            "to": "Orders",
        }])
    );
}

#[test]
fn the_mappers_come_in_their_order() {
    // Schema mappers before database mappers aren't the grammar's.
    let order =
        error("RelationalMapper my::M\n(\n  SchemaMappers: [];\n  DatabaseMappers: [];\n)\n");
    assert_eq!(position(&order), (5, 3, 5, 17));
    // A keyword isn't a name.
    let keyword =
        error("RelationalMapper my::M\n(\n  SchemaMappers: [ my::Db.TableMappers -> 'x' ];\n)\n");
    assert_eq!(position(&keyword), (4, 27, 4, 38));
}
