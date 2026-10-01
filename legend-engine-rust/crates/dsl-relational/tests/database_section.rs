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

//! Linking this crate registers the `###Relational` section grammar with both
//! the parser (`SECTION_PARSERS`) and the protocol converter
//! (`ELEMENT_PROTOCOLS`): databases convert to the engine's JSON, positioned
//! as its `RelationalParseTreeWalker` positions them.

use legend_pure_dsl_relational as _;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The first database `body`, a `###Relational` section's, defines.
fn database(body: &str) -> Value {
    let source = format!("###Relational\n{body}");
    let file = legend_pure_parser_parser::parse(&source, "db.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")
}

/// The start of the first error `body` has.
fn error(body: &str) -> (String, (u32, u32, u32, u32)) {
    let source = format!("###Relational\n{body}");
    let errors = legend_pure_parser_parser::parse(&source, "db.pure")
        .expect_err("fails")
        .errors;
    let si = errors[0].source_info().expect("positioned");
    (
        errors[0].message(),
        (si.start_line, si.start_column, si.end_line, si.end_column),
    )
}

fn span(node: &Value) -> (u64, u64, u64, u64) {
    let si = &node["sourceInformation"];
    let at = |key: &str| {
        si[key]
            .as_u64()
            .unwrap_or_else(|| panic!("no {key} in {node}"))
    };
    (
        at("startLine"),
        at("startColumn"),
        at("endLine"),
        at("endColumn"),
    )
}

#[test]
fn tables_outside_a_schema_are_in_default() {
    let db = database(
        "Database my::Db\n(\n  Schema s\n  (\n    Table \"My Table\" (id INT PRIMARY KEY, name VARCHAR(20) NOT NULL, amount DECIMAL(10, 2))\n  )\n  Table t (x BIGINT)\n)\n",
    );
    assert_eq!(db["_type"], "relational");
    assert_eq!(db["package"], "my");
    let schemas = db["schemas"].as_array().expect("schemas");
    assert_eq!(schemas.len(), 2);
    // Quoted names keep their quotes, as the walker keeps them.
    let table = &schemas[0]["tables"][0];
    assert_eq!(table["name"], "\"My Table\"");
    // Absent, not empty, as the walker leaves them: the protocol reads `[]`
    // as an empty list, where Java's is null.
    assert!(table.get("stereotypes").is_none(), "{table}");
    assert!(table["columns"][0].get("stereotypes").is_none());
    assert!(schemas[0].get("stereotypes").is_none());
    // A database's are always there.
    assert_eq!(db["stereotypes"], json!([]));
    assert_eq!(table["primaryKey"], json!(["id"]));
    let columns = &table["columns"];
    assert_eq!(columns[0]["nullable"], false);
    assert_eq!(columns[1]["type"], json!({"_type": "Varchar", "size": 20}));
    assert_eq!(columns[1]["nullable"], false);
    assert_eq!(
        columns[2]["type"],
        json!({"_type": "Decimal", "precision": 10, "scale": 2})
    );
    // `default` comes last, positioned on the whole database.
    assert_eq!(schemas[1]["name"], "default");
    assert_eq!(span(&schemas[1]), span(&db));
    assert_eq!(schemas[1]["tables"][0]["columns"][0]["nullable"], true);
}

#[test]
fn a_comparison_is_positioned_on_its_operator_and_its_left_side_on_the_whole() {
    let db = database("Database my::Db\n(\n  Table t (a INT)\n  Filter F(t.a = 'A')\n)\n");
    // Line 5, `  Filter F(t.a = 'A')`: `t` at 12, `=` at 16, `'A'` from 18 to
    // 20.
    let equal = &db["filters"][0]["operation"];
    assert_eq!(equal["funcName"], "equal");
    assert_eq!(span(equal), (5, 16, 5, 20));
    let column = &equal["parameters"][0];
    assert_eq!(span(column), (5, 12, 5, 20));
    assert_eq!(column["column"], "a");
    assert_eq!(column["tableAlias"], "t");
    assert_eq!(
        column["table"]["database"], "my::Db",
        "a column's database is the scope's"
    );
    assert_eq!(column["table"]["schema"], "default");
    assert_eq!(span(&column["table"]), (5, 12, 5, 12));
    assert_eq!(span(&equal["parameters"][1]), (5, 18, 5, 20));
}

#[test]
fn and_and_or_read_right_to_left() {
    let db = database(
        "Database my::Db\n(\n  Table t (a INT)\n  Filter F(t.a = 1 and t.a = 2 and t.a = 3)\n  Filter G(t.a = 1 and t.a = 2 or t.a = 3)\n)\n",
    );
    // One `and` of three.
    let and = &db["filters"][0]["operation"];
    assert_eq!(and["funcName"], "and");
    assert_eq!(and["parameters"].as_array().expect("parameters").len(), 3);
    // `and` then a group of `or`, positioned on it, from `or`.
    let and = &db["filters"][1]["operation"];
    let group = &and["parameters"][1];
    assert_eq!(group["funcName"], "group");
    assert_eq!(group["parameters"][0]["funcName"], "or");
    assert_eq!(span(group), span(&group["parameters"][0]));
    assert_eq!(span(group).1, 32);
}

#[test]
fn joins_navigate_to_an_element_in_the_last_joins_database() {
    let db = database(
        "Database my::Db\n(\n  Table t (a INT)\n  View v\n  (\n    x: [my::Db] @J1 > (INNER) [my::Other] @J2 | t.a,\n    y: {target}.a\n  )\n)\n",
    );
    let view = &db["schemas"][0]["views"][0];
    let with_joins = &view["columnMappings"][0]["operation"];
    assert_eq!(with_joins["_type"], "elemtWithJoins");
    let joins = &with_joins["joins"];
    assert_eq!(joins[0]["db"], "my::Db");
    assert_eq!(joins[0]["name"], "J1");
    assert!(joins[0]["joinType"].is_null());
    assert_eq!(joins[1]["db"], "my::Other");
    assert_eq!(joins[1]["joinType"], "INNER");
    assert_eq!(
        with_joins["relationalElement"]["table"]["database"],
        "my::Other"
    );
    let target = &view["columnMappings"][1]["operation"];
    assert_eq!(target["tableAlias"], "{target}");
    assert_eq!(target["table"]["schema"], "default");
}

#[test]
fn functions_take_arrays_and_lambdas() {
    let db = database(
        "Database my::Db\n(\n  Table t (a INT, b VARCHAR(9))\n  Filter F(in(t.a, [1, -2]) and exists(t.b, x | $x = 'y'))\n)\n",
    );
    let and = &db["filters"][0]["operation"];
    let array = &and["parameters"][0]["parameters"][1];
    assert_eq!(array["_type"], "literalList");
    // Each item held by an unpositioned literal; `-2` is one number.
    assert_eq!(array["values"][1]["_type"], "literal");
    assert!(array["values"][1].get("sourceInformation").is_none());
    assert_eq!(array["values"][1]["value"]["value"], -2);
    let lambda = &and["parameters"][1]["parameters"][1];
    assert_eq!(lambda["_type"], "relationalLambda");
    assert_eq!(lambda["parameterNames"], json!(["x"]));
    assert_eq!(lambda["body"]["parameters"][0]["_type"], "lambdaParameter");
}

#[test]
fn milestoning_is_positioned_as_the_engine_parses_it_again() {
    let db = database(
        "Database my::Db\n(\n  Table t\n  (\n    milestoning(business(BUS_FROM = f, BUS_THRU = t, INFINITY_DATE = %9999-12-31), processing(PROCESSING_SNAPSHOT_DATE = s))\n    f DATE, t DATE, s DATE\n  )\n)\n",
    );
    let milestoning = &db["schemas"][0]["tables"][0]["milestoning"];
    let business = &milestoning[0];
    assert_eq!(business["_type"], "businessMilestoning");
    assert_eq!(business["thruIsInclusive"], false);
    assert_eq!(business["infinityDate"]["_type"], "strictDate");
    assert_eq!(business["infinityDate"]["value"], "9999-12-31");
    // `BUS_FROM` starts at 26 and the date ends at 80: both one column right.
    assert_eq!(span(business), (6, 27, 6, 81));
    let snapshot = &milestoning[1];
    assert_eq!(snapshot["_type"], "processingSnapshotMilestoning");
    assert_eq!(snapshot["snapshotDate"], "s");
}

#[test]
fn column_types_are_checked_as_the_engine_checks_them() {
    let (message, position) = error("Database my::Db\n(\n  Table t (a VARCHAR, b INT)\n)\n");
    assert!(
        message.contains("VARCHAR requires 1 parameter (size)"),
        "{message}"
    );
    // At the type.
    assert_eq!(position, (4, 14, 4, 20));
    let (message, position) = error("Database my::Db\n(\n  Table t (a INT(4))\n)\n");
    assert!(
        message.contains("INTEGER does not expect any parameters"),
        "{message}"
    );
    // From the type to `)`.
    assert_eq!(position, (4, 14, 4, 19));
    let (message, _) = error("Database my::Db\n(\n  Table t (a STRING)\n)\n");
    assert!(
        message.contains("Unsupported column data type 'STRING'"),
        "{message}"
    );
}

#[test]
fn a_column_needs_a_table() {
    let (message, position) = error("Database my::Db\n(\n  Filter F(a = 1)\n)\n");
    assert!(
        message.contains("Missing table or alias for column 'a'"),
        "{message}"
    );
    assert_eq!(position, (4, 12, 4, 12));
}

#[test]
fn a_relational_section_has_no_imports() {
    let (_, position) = error("import my::*;\nDatabase my::Db()\n");
    assert_eq!((position.0, position.1), (2, 1));
}
