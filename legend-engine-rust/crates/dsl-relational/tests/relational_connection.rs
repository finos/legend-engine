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

//! Relational connections, as the relational store's
//! `RelationalDatabaseConnectionParseTreeWalker` and its specifications'
//! walkers read them.

use legend_pure_dsl_connection as _;
use legend_pure_dsl_relational as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

/// The value of the connection `RelationalDatabaseConnection my::C { {fields} }`.
fn connection(fields: &str) -> Value {
    let source = source(fields);
    let file = legend_pure_parser_parser::parse(&source, "c.pure").expect("parses");
    let pmcd = convert_source_file(&file).expect("converts");
    serde_json::to_value(&pmcd.elements[0]).expect("serializes")["connectionValue"].clone()
}

/// The section, the fields from line 4 on.
fn source(fields: &str) -> String {
    format!("###Connection\nRelationalDatabaseConnection my::C\n{{\n{fields}\n}}\n")
}

fn error(fields: &str) -> ParseError {
    legend_pure_parser_parser::parse(&source(fields), "c.pure")
        .expect_err("fails")
        .errors
        .remove(0)
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    serde_json::to_value(SourceInformation {
        source_id: "c.pure".into(),
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

const H2: &str = "  type: H2;\n  specification: LocalH2 {};\n  auth: DefaultH2;";

#[test]
fn a_connection_reads_its_fields() {
    let value = connection(
        "  store: my::Db;\n  type: H2;\n  timezone: -0500;\n  quoteIdentifiers: true;\n  queryTimeOutInSeconds: 30;\n  specification: LocalH2\n  {\n    testDataSetupSqls: ['create table a', 'drop table a'];\n  };\n  auth: DefaultH2;",
    );
    assert_eq!(
        value,
        json!({
            "_type": "RelationalDatabaseConnection",
            "type": "H2",
            "databaseType": "H2",
            "element": "my::Db",
            "elementSourceInformation": si(4, 10, 4, 15),
            "timeZone": "-0500",
            "quoteIdentifiers": true,
            "queryTimeOutInSeconds": 30,
            // Each positioned on its field.
            "datasourceSpecification": {
                "_type": "h2Local",
                "testDataSetupSqls": ["create table a", "drop table a"],
                "sourceInformation": si(9, 3, 12, 4),
            },
            "authenticationStrategy": {"_type": "h2Default", "sourceInformation": si(13, 3, 13, 18)},
            "postProcessors": [],
            "postProcessorWithParameter": [],
            // The whole connection.
            "sourceInformation": si(2, 1, 14, 1),
        })
    );
    // A zone, unquoted.
    let zoned = connection(&format!("{H2}\n  timezone: 'US/Arizona';"));
    assert_eq!(zoned["timeZone"], "US/Arizona");
}

#[test]
fn what_a_specification_reads_is_positioned_as_the_walker_parses_it_again() {
    // The walker parses `relationalMapper{…}` again from the type: without
    // the line break before `{`, `my::Mapper` is a line up; with two spaces
    // before `{` instead of one, a column left.
    let value = connection(&format!(
        "{H2}\n  postProcessors:\n  [\n    relationalMapper\n    {{\n      my::Mapper\n    }},\n    relationalMapper  {{ my::Other }}\n  ];"
    ));
    let processors = &value["postProcessors"];
    assert_eq!(
        processors[0],
        json!({
            "_type": "relationalMapper",
            "relationalMappers": [{"type": "QUERYPOSTPROCESSOR", "path": "my::Mapper", "sourceInformation": si(10, 7, 10, 16)}],
        })
    );
    assert_eq!(
        processors[1]["relationalMappers"][0]["sourceInformation"],
        si(13, 24, 13, 32)
    );
    // A query generation config's text a line further down: `wrong`, on line
    // 10 after `{` on the type's line, is reported on line 11.
    let config = error(&format!(
        "{H2}\n  queryGenerationConfigs:\n  [\n    GenerationFeaturesConfig {{\n      enabled: ['A']; wrong: ['B'];\n    }}\n  ];"
    ));
    assert_eq!((position(&config).0, position(&config).1), (11, 23));
    // The walker's errors in a mapper are on the mapper, parsed again.
    let mapper = error(&format!(
        "{H2}\n  postProcessors:\n  [\n    mapper\n    {{\n      mappers: [table {{from: 'a'; to: 'A'; schemaTo: 'S';}}];\n    }}\n  ];"
    ));
    assert_eq!(mapper.message(), "Field 'schemaFrom' is required");
    assert_eq!(position(&mapper), (10, 17, 10, 58));
}

#[test]
fn post_processors_and_configs_convert() {
    let value = connection(&format!(
        "{H2}\n  postProcessors: [mapper {{ mappers: [table {{from: 'a'; to: 'A'; schemaFrom: 's'; schemaTo: 'S';}}, schema {{from: 'c'; to: 'C';}}]; }}, ExtractSubQueriesAsCTEsPostProcessor];\n  queryGenerationConfigs: [GenerationFeaturesConfig {{ disabled: ['F']; }}];"
    ));
    assert_eq!(
        value["postProcessors"],
        json!([
            {"_type": "mapper", "mappers": [
                {"_type": "table", "from": "a", "to": "A", "schema": {"_type": "schema", "from": "s", "to": "S"}},
                {"_type": "schema", "from": "c", "to": "C"},
            ]},
            {"_type": "ExtractSubQueriesAsCTEsPostProcessor", "sourceInformation": si(7, 134, 7, 169)},
        ])
    );
    assert_eq!(
        value["queryGenerationConfigs"],
        json!([{"_type": "generationFeaturesConfig", "enabled": [], "disabled": ["F"], "sourceInformation": si(8, 28, 8, 72)}])
    );
    // Absent, the query generation configs aren't there.
    assert!(connection(H2).get("queryGenerationConfigs").is_none());
}

#[test]
fn the_walker_checks_the_fields_in_its_order() {
    // The type first: the whole connection.
    let no_type = error("  specification: LocalH2 {};\n  auth: DefaultH2;");
    assert_eq!(no_type.message(), "Field 'type' is required");
    assert_eq!(position(&no_type), (2, 1, 6, 1));
    // One of the engine's, on the field.
    let unknown = error("  type: Unknown;\n  specification: LocalH2 {};\n  auth: DefaultH2;");
    assert_eq!(unknown.message(), "Unknown database type 'Unknown'");
    assert_eq!(position(&unknown), (4, 3, 4, 16));
    // The specification, then the authentication strategy.
    assert_eq!(
        error("  type: H2;").message(),
        "Field 'specification' is required"
    );
    assert_eq!(
        error("  type: H2;\n  specification: LocalH2 {};").message(),
        "Field 'auth' is required"
    );
    // A mode other than `local` is ignored.
    assert_eq!(connection(&format!("{H2}\n  mode: remote;"))["type"], "H2");
    // A `local` connection's specifications are its database type's local
    // mode's: H2 has none, which the engine reports outside its parser.
    let local = error("  type: H2;\n  mode: local;");
    assert!(matches!(local, ParseError::Unsupported { .. }), "{local}");
}

#[test]
fn a_specification_of_a_type_without_grammar_is_unsupported() {
    let unknown =
        error("  type: Snowflake;\n  specification: Snowflake { name: 'a'; };\n  auth: DefaultH2;");
    assert!(
        matches!(unknown, ParseError::Unsupported { .. }),
        "{unknown}"
    );
    // The field.
    assert_eq!(position(&unknown), (5, 3, 5, 42));
}

#[test]
fn the_stores_datasource_specifications_and_authentication_strategies_convert() {
    let value = connection(
        "  type: SqlServer;\n  specification: Static { name: 'db'; host: 'h'; port: 1433; };\n  auth: UserNamePassword { userNameVaultReference: 'u'; passwordVaultReference: 'p'; };",
    );
    assert_eq!(value["datasourceSpecification"]["_type"], "static");
    assert_eq!(value["datasourceSpecification"]["host"], "h");
    assert_eq!(value["datasourceSpecification"]["port"], 1433);
    assert_eq!(value["datasourceSpecification"]["databaseName"], "db");
    assert_eq!(
        value["authenticationStrategy"],
        json!({
            "_type": "userNamePassword",
            "userNameVaultReference": "u",
            "passwordVaultReference": "p",
            "sourceInformation": si(6, 3, 6, 87),
        })
    );
    let embedded = connection(
        "  type: H2;\n  specification: EmbeddedH2 { name: 'db'; directory: '/tmp/h2'; autoServerMode: true; };\n  auth: DefaultH2;",
    );
    assert_eq!(
        embedded["datasourceSpecification"],
        json!({
            "_type": "h2Embedded",
            "databaseName": "db",
            "directory": "/tmp/h2",
            "autoServerMode": true,
            "sourceInformation": si(5, 3, 5, 88),
        })
    );
    // The walker's order, on the field.
    let static_error =
        error("  type: H2;\n  specification: Static { name: 'db'; };\n  auth: DefaultH2;");
    assert_eq!(static_error.message(), "Field 'host' is required");
    assert_eq!(position(&static_error), (5, 3, 5, 40));
    let oauth =
        error("  type: H2;\n  specification: LocalH2 {};\n  auth: OAuth { scopeName: 's'; };");
    assert_eq!(oauth.message(), "Field 'oauthKey' is required");
    // Without a body, a Kerberos strategy names no principal; with one, it
    // needs one, which the engine fails on outside its parser.
    let kerberos =
        connection("  type: H2;\n  specification: LocalH2 {};\n  auth: DelegatedKerberos;");
    assert_eq!(
        kerberos["authenticationStrategy"],
        json!({"_type": "delegatedKerberos", "sourceInformation": si(6, 3, 6, 26)})
    );
    let empty = error("  type: H2;\n  specification: LocalH2 {};\n  auth: DelegatedKerberos {};");
    assert!(matches!(empty, ParseError::Unsupported { .. }), "{empty}");
    // A strategy without a body doesn't read one written.
    let ignored =
        connection("  type: H2;\n  specification: LocalH2 {};\n  auth: Test { anything };");
    assert_eq!(ignored["authenticationStrategy"]["_type"], "test");
}
