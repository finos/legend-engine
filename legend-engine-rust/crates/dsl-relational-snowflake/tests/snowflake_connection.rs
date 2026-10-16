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

//! Snowflake's specifications in relational connections, as the
//! extension's `SnowflakeGrammarParserExtension` reads them.

use legend_pure_dsl_connection as _;
use legend_pure_dsl_relational_snowflake as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

fn source(fields: &str) -> String {
    format!("###Connection\nRelationalDatabaseConnection my::C\n{{\n{fields}\n}}\n")
}

/// The value of the connection of `fields`, from line 4 on.
fn connection(fields: &str) -> Value {
    let file = legend_pure_parser_parser::parse(&source(fields), "c.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")["connectionValue"].clone()
}

fn error(fields: &str) -> ParseError {
    legend_pure_parser_parser::parse(&source(fields), "c.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

const AUTH: &str = "  auth: SnowflakePublic { publicUserName: 'u'; privateKeyVaultReference: 'k'; passPhraseVaultReference: 'p'; };";

#[test]
fn a_snowflake_connection_reads_its_specification_and_strategy() {
    let value = connection(&format!(
        "  type: Snowflake;\n  specification: Snowflake {{ name: 'db'; account: 'acc'; warehouse: 'wh'; region: 'r'; quotedIdentifiersIgnoreCase: true; accountType: MultiTenant; role: 'admin'; }};\n{AUTH}"
    ));
    let datasource = &value["datasourceSpecification"];
    assert_eq!(datasource["_type"], "snowflake");
    assert_eq!(datasource["databaseName"], "db");
    assert_eq!(datasource["accountName"], "acc");
    assert_eq!(datasource["warehouseName"], "wh");
    assert_eq!(datasource["region"], "r");
    assert_eq!(datasource["quotedIdentifiersIgnoreCase"], true);
    // A word, as written.
    assert_eq!(datasource["accountType"], "MultiTenant");
    assert_eq!(datasource["role"], "admin");
    assert!(datasource.get("cloudType").is_none(), "{datasource}");
    assert_eq!(value["authenticationStrategy"]["_type"], "snowflakePublic");
    assert_eq!(value["authenticationStrategy"]["publicUserName"], "u");
    // In the walker's order, on the field.
    let missing = error(&format!(
        "  type: Snowflake;\n  specification: Snowflake {{ account: 'acc'; }};\n{AUTH}"
    ));
    assert_eq!(missing.message(), "Field 'name' is required");
    let si = missing.source_info().expect("positioned");
    assert_eq!((si.start_line, si.start_column), (5, 3));
}

#[test]
fn a_local_snowflake_connection_names_its_store_in_its_specifications() {
    let value = connection("  store: my::store::Db;\n  type: Snowflake;\n  mode: local;");
    assert_eq!(value["localMode"], true);
    assert_eq!(
        value["datasourceSpecification"],
        json!({
            "_type": "snowflake",
            "accountName": "legend-local-snowflake-accountName-my-store-Db",
            "databaseName": "legend-local-snowflake-databaseName-my-store-Db",
            "role": "legend-local-snowflake-role-my-store-Db",
            "warehouseName": "legend-local-snowflake-warehouseName-my-store-Db",
            "region": "legend-local-snowflake-region-my-store-Db",
            "cloudType": "legend-local-snowflake-cloudType-my-store-Db",
        })
    );
    assert_eq!(
        value["authenticationStrategy"]["passPhraseVaultReference"],
        "legend-local-snowflake-passphraseVaultReference-my-store-Db"
    );
    // Without a store, the engine fails outside its parser.
    let storeless = error("  type: Snowflake;\n  mode: local;");
    assert!(
        matches!(storeless, ParseError::Unsupported { .. }),
        "{storeless}"
    );
}
