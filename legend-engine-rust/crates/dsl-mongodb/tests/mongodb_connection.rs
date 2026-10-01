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

//! MongoDB connections, as the engine's `MongoDBConnectionParseTreeWalker`
//! reads them, and the authentication specifications they hold, as the
//! engine's authentication module reads them.

use legend_pure_dsl_mongodb as _;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::convert::convert_source_file;
use serde_json::{Value, json};

/// The section, `fields` from line 4 on.
fn source(fields: &str) -> String {
    format!("###Connection\nMongoDBConnection my::C\n{{\n{fields}\n}}\n")
}

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

/// The authentication specification of a connection with `authentication`,
/// on line 6 on.
fn authentication(authentication: &str) -> Value {
    connection(&format!(
        "  database: db;\n  serverURLs: [localhost:1];\n  authentication: {authentication};"
    ))["authenticationSpecification"]
        .clone()
}

fn authentication_error(authentication: &str) -> ParseError {
    error(&format!(
        "  database: db;\n  serverURLs: [localhost:1];\n  authentication: {authentication};"
    ))
}

fn si(start_line: u32, start_column: u32, end_line: u32, end_column: u32) -> Value {
    json!({
        "sourceId": "c.pure",
        "startLine": start_line,
        "startColumn": start_column,
        "endLine": end_line,
        "endColumn": end_column,
    })
}

#[test]
fn a_connection_reads_its_servers_and_authentication() {
    let value = connection(
        "  store: my::Store;\n  database: legend-db;\n  serverURLs: [my-host.example.com:27071, localhost:1];\n  authentication: # UserPassword {\n    username: 'u';\n    password: SystemPropertiesSecret\n    {\n      systemPropertyName: 'p';\n    };\n  }#;",
    );
    assert_eq!(
        value,
        json!({
            "_type": "MongoDBConnection",
            "type": "MongoDb",
            // A host and a database name read as the grammar's lexer reads
            // them: one word.
            "dataSourceSpecification": {
                "databaseName": "legend-db",
                "serverURLs": [
                    {"baseUrl": "my-host.example.com", "port": 27071},
                    {"baseUrl": "localhost", "port": 1},
                ],
            },
            "authenticationSpecification": {
                "_type": "userPassword",
                "username": "u",
                "password": {
                    "_type": "systemproperties",
                    "systemPropertyName": "p",
                    "sourceInformation": si(9, 15, 12, 5),
                },
                // From the body's first token to the end of input, whose
                // text, `<EOF>`, ANTLR counts.
                "sourceInformation": si(8, 5, 13, 7),
            },
            "element": "my::Store",
            "elementSourceInformation": si(4, 10, 4, 18),
            "sourceInformation": si(2, 1, 14, 1),
        })
    );
}

#[test]
fn the_walker_checks_the_fields_in_its_order() {
    let no_database = error("  serverURLs: [localhost:1];");
    assert_eq!(no_database.message(), "Field 'database' is required");
    // The walker names the field as its lexer does.
    let no_authentication = error("  database: db;\n  serverURLs: [localhost:1];");
    assert_eq!(
        no_authentication.message(),
        "Field ''authentication'' is required"
    );
}

#[test]
fn authentication_specifications_read_their_type_and_secrets() {
    assert_eq!(
        authentication("# PSK { psk: 'key'; }#"),
        json!({"_type": "PSK", "psk": "key"})
    );
    let kerberos = authentication("# Kerberos {}#");
    assert_eq!(kerberos["_type"], "kerberos");
    let api_key = authentication(
        "# ApiKey { location: 'header'; keyName: 'k'; value: EnvironmentSecret { envVariableName: 'E'; }; }#",
    );
    assert_eq!(api_key["location"], "HEADER");
    assert_eq!(api_key["value"]["_type"], "environment");
    let key_pair = authentication(
        "# EncryptedPrivateKey { userName: 'u'; privateKey: PropertiesFileSecret { propertyName: 'k'; }; passphrase: AWSSecretsManagerSecret { secretId: 's'; versionId: 'v'; versionStage: 'st'; awsCredentials: Default {} }; }#",
    );
    assert_eq!(key_pair["privateKey"]["_type"], "properties");
    // A secrets manager secret has no position, nor do default credentials.
    assert_eq!(
        key_pair["passphrase"],
        json!({"_type": "awssecretsmanager", "secretId": "s", "versionId": "v", "versionStage": "st", "awsCredentials": {"_type": "awsDefault"}})
    );
    let gcp = authentication(
        "# GCPWIFWithAWSIdP { serviceAccountEmail: 'e'; idP: AWSIdP { accountId: 'a'; region: 'r'; role: 'ro'; awsCredentials: STSAssumeRole { roleArn: 'arn'; awsCredentials: Default {} } } workload: GCPWorkload { projectNumber: 'n'; poolId: 'p'; providerId: 'pr'; } }#",
    );
    assert_eq!(gcp["_type"], "gcpWithAWSIdP");
    assert_eq!(
        gcp["idPConfiguration"]["awsCredentials"]["_type"],
        "awsSTSAssumeRole"
    );
    assert_eq!(gcp["workloadConfiguration"]["poolId"], "p");
    assert_eq!(gcp["additionalGcpScopes"], json!([]));
}

#[test]
fn an_authentication_needs_a_known_type_and_its_fields() {
    let missing_type = authentication_error("#  { psk: 'k'; }#");
    assert!(
        missing_type
            .message()
            .starts_with("Missing authentication type"),
        "{missing_type}"
    );
    let unknown = authentication_error("# Unknown { }#");
    assert!(
        matches!(unknown, ParseError::Unsupported { .. }),
        "{unknown}"
    );
    // The walker names the user name so; on the specification.
    let no_user = authentication_error(
        "# UserPassword { password: SystemPropertiesSecret { systemPropertyName: 'p'; }; }#",
    );
    assert_eq!(no_user.message(), "Field 'userName' is required");
    // Where the engine fails outside its parser, or without a position.
    for unsupported in [
        "# ApiKey { location: 'body'; keyName: 'k'; value: EnvironmentSecret { envVariableName: 'E'; }; }#",
        "# UserPassword { username: 'u'; password: AWSSecretsManagerSecret { versionId: 'v'; versionStage: 's'; awsCredentials: Default {} }; }#",
        "# UserPassword { username: 'u'; password: AWSSecretsManagerSecret { secretId: 's'; versionId: 'v'; versionStage: 's'; awsCredentials: STSAssumeRole { roleArn: 'a'; awsCredentials: STSAssumeRole { roleArn: 'b'; awsCredentials: Default {} } } }; }#",
    ] {
        let error = authentication_error(unsupported);
        assert!(matches!(error, ParseError::Unsupported { .. }), "{error}");
    }
}
