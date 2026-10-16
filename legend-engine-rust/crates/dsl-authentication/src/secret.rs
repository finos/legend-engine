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

//! Secrets (`CredentialVaultSecretParseTreeWalker`) and AWS credentials
//! (`AWSCredentialsParser`), as the grammar reads them, then as the walkers
//! check them:
//!
//! ```text
//! secret:       PropertiesFileSecret { propertyName: '…'; } | EnvironmentSecret { envVariableName: '…'; }
//!             | SystemPropertiesSecret { systemPropertyName: '…'; }
//!             | AWSSecretsManagerSecret { secretId: '…'; versionId: '…'; versionStage: '…'; awsCredentials: credentials }
//! credentials:  Default { } | Static { accessKeyId: secret; secretAccessKey: secret; }
//!             | STSAssumeRole { roleArn: '…'; roleSessionName: '…'; awsCredentials: credentials }
//! ```
//!
//! Each positioned on itself, where its walker reports a field missing or
//! written twice; but an `AWSSecretsManagerSecret`, which its walker gives no
//! position, and `Default` credentials. An `STSAssumeRole`'s credentials
//! can't be another's: the engine fails outside its parser.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::{optional, required};
use legend_pure_parser_parser::words::{is_string, is_word, unexpected};

use crate::grammar::R;
use crate::object::{Field, Object};

/// A block, `Keyword { field: value; … }`, or the island's body, as the
/// grammar reads it: each field's values, as written.
#[derive(Debug, Clone)]
pub(crate) struct RawBlock {
    pub(crate) keyword: String,
    pub(crate) fields: Vec<Vec<RawValue>>,
    pub(crate) source_info: SourceInfo,
}

/// A field's value, as read.
#[derive(Debug, Clone)]
pub(crate) enum RawValue {
    Text(String),
    Block(RawBlock),
}

/// What a field holds.
#[derive(Debug, Clone, Copy)]
pub(crate) enum Holds {
    /// `'text';`.
    Text,
    /// `secret;`.
    Secret,
    /// `credentials`, without `;`.
    Credentials,
    /// `Keyword { … }`, a block of these fields, without `;`.
    Block(&'static str, &'static [(&'static str, Holds)]),
}

/// The fields of a block, `name: value`, in any order, until none is next.
pub(crate) fn read_fields(
    ctx: &mut ParserContext<'_>,
    fields: &[(&'static str, Holds)],
) -> R<Vec<Vec<RawValue>>> {
    let mut read: Vec<Vec<RawValue>> = vec![Vec::new(); fields.len()];
    'fields: loop {
        for (index, (name, holds)) in fields.iter().enumerate() {
            if is_word(ctx.cursor().peek(), name) {
                let field_start = ctx.cursor().current_source_info();
                ctx.cursor().advance();
                ctx.cursor().expect(TokenKind::Colon)?;
                let value = match holds {
                    Holds::Text => {
                        let value = RawValue::Text(text(ctx)?);
                        ctx.cursor().expect(TokenKind::Semicolon)?;
                        value
                    }
                    Holds::Secret => {
                        let value = RawValue::Block(read_secret(ctx)?);
                        ctx.cursor().expect(TokenKind::Semicolon)?;
                        value
                    }
                    Holds::Credentials => RawValue::Block(read_credentials(ctx)?),
                    // The field's block: `name: Keyword { … }`.
                    Holds::Block(keyword, fields) => {
                        let mut block = read_block(ctx, &[(keyword, fields)])?;
                        block.source_info = ctx.cursor().span_since(&field_start);
                        RawValue::Block(block)
                    }
                };
                read[index].push(value);
                continue 'fields;
            }
        }
        return Ok(read);
    }
}

/// `'text'`, unescaped.
pub(crate) fn text(ctx: &mut ParserContext<'_>) -> R<String> {
    let token = ctx.cursor().peek().clone();
    if !is_string(&token) {
        return Err(unexpected(&token));
    }
    ctx.cursor().advance();
    legend_pure_parser_parser::string_value(&token.text)
        .map(|(value, _)| value)
        .map_err(|message| ParseError::Unexpected {
            message,
            source_info: token.source_info,
        })
}

/// One of `blocks`, `Keyword { fields }`, its keyword next.
fn read_block(
    ctx: &mut ParserContext<'_>,
    blocks: &[(&'static str, &'static [(&'static str, Holds)])],
) -> R<RawBlock> {
    let start = ctx.cursor().current_source_info();
    let keyword = ctx.cursor().peek().clone();
    let Some((name, fields)) = blocks.iter().find(|(name, _)| is_word(&keyword, name)) else {
        return Err(unexpected(&keyword));
    };
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::LBrace)?;
    let fields = read_fields(ctx, fields)?;
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(RawBlock {
        keyword: (*name).to_string(),
        fields,
        source_info: ctx.cursor().span_since(&start),
    })
}

const SECRETS: [(&str, &[(&str, Holds)]); 4] = [
    ("PropertiesFileSecret", &[("propertyName", Holds::Text)]),
    ("EnvironmentSecret", &[("envVariableName", Holds::Text)]),
    (
        "SystemPropertiesSecret",
        &[("systemPropertyName", Holds::Text)],
    ),
    (
        "AWSSecretsManagerSecret",
        &[
            ("secretId", Holds::Text),
            ("versionId", Holds::Text),
            ("versionStage", Holds::Text),
            ("awsCredentials", Holds::Credentials),
        ],
    ),
];

const CREDENTIALS: [(&str, &[(&str, Holds)]); 3] = [
    ("Default", &[]),
    (
        "Static",
        &[
            ("accessKeyId", Holds::Secret),
            ("secretAccessKey", Holds::Secret),
        ],
    ),
    (
        "STSAssumeRole",
        &[
            ("roleArn", Holds::Text),
            ("roleSessionName", Holds::Text),
            ("awsCredentials", Holds::Credentials),
        ],
    ),
];

pub(crate) fn read_secret(ctx: &mut ParserContext<'_>) -> R<RawBlock> {
    read_block(ctx, &SECRETS)
}

fn read_credentials(ctx: &mut ParserContext<'_>) -> R<RawBlock> {
    read_block(ctx, &CREDENTIALS)
}

// ── The walkers ──────────────────────────────────────────────────────────

/// The one value of a required field, checked on `at`.
pub(crate) fn one(values: Vec<RawValue>, field: &str, at: &SourceInfo) -> R<RawValue> {
    required(values, field, at)
}

/// A text field's value.
pub(crate) fn text_of(value: RawValue) -> String {
    match value {
        RawValue::Text(text) => text,
        RawValue::Block(block) => block.keyword,
    }
}

/// A block field's value.
pub(crate) fn block_of(value: RawValue) -> RawBlock {
    match value {
        RawValue::Block(block) => block,
        RawValue::Text(text) => RawBlock {
            keyword: text,
            fields: Vec::new(),
            source_info: SourceInfo::new("", 0, 0, 0, 0),
        },
    }
}

/// A field checked before the walker knows where its object is: its error
/// has no position, which Rust leaves to the engine.
fn unpositioned(values: Vec<RawValue>, field: &str, at: &SourceInfo) -> R<RawValue> {
    let mut values = values;
    if values.len() == 1 {
        return Ok(values.remove(0));
    }
    Err(ParseError::Unsupported {
        message: format!(
            "'{field}' is missing or written twice, which the engine reports without a position"
        ),
        source_info: at.clone(),
    })
}

/// The walker's `visitCredentialVaultSecret`.
pub(crate) fn secret(block: RawBlock) -> R<Object> {
    let at = block.source_info.clone();
    let mut fields = block.fields.into_iter();
    let mut next = || fields.next().unwrap_or_default();
    let (json_type, field) = match block.keyword.as_str() {
        "PropertiesFileSecret" => ("properties", "propertyName"),
        "EnvironmentSecret" => ("environment", "envVariableName"),
        "SystemPropertiesSecret" => ("systemproperties", "systemPropertyName"),
        _ => {
            let secret_id = text_of(unpositioned(next(), "secretId", &at)?);
            let version_id = text_of(unpositioned(next(), "versionId", &at)?);
            let version_stage = text_of(unpositioned(next(), "versionStage", &at)?);
            let credentials = block_of(unpositioned(next(), "awsCredentials", &at)?);
            return Ok(Object {
                json_type: Some("awssecretsmanager"),
                fields: Vec::new(),
                source_info: None,
            }
            .with("secretId", Field::Text(secret_id))
            .with("versionId", Field::Text(version_id))
            .with("versionStage", Field::Text(version_stage))
            .with(
                "awsCredentials",
                Field::Object(credentials_of(credentials)?),
            ));
        }
    };
    let value = text_of(one(next(), field, &at)?);
    Ok(Object::new(json_type, Some(at)).with(field, Field::Text(value)))
}

/// The walker's `visitAWSCredentials`.
fn credentials_of(block: RawBlock) -> R<Object> {
    let at = block.source_info.clone();
    let mut fields = block.fields.into_iter();
    let mut next = || fields.next().unwrap_or_default();
    match block.keyword.as_str() {
        "Default" => Ok(Object::new("awsDefault", None)),
        "Static" => {
            let access_key_id = secret(block_of(one(next(), "accessKeyId", &at)?))?;
            let secret_access_key = secret(block_of(one(next(), "secretAccessKey", &at)?))?;
            Ok(Object::new("awsStatic", Some(at))
                .with("accessKeyId", Field::Object(access_key_id))
                .with("secretAccessKey", Field::Object(secret_access_key)))
        }
        _ => {
            let role_arn = text_of(one(next(), "roleArn", &at)?);
            let role_session_name = optional(next(), "roleSessionName", &at)?.map(text_of);
            let credentials = block_of(one(next(), "awsCredentials", &at)?);
            if credentials.keyword == "STSAssumeRole" {
                return Err(ParseError::Unsupported {
                    message: "an STSAssumeRole's credentials are another's, which the engine \
                              reports outside its parser"
                        .to_string(),
                    source_info: credentials.source_info,
                });
            }
            let mut object =
                Object::new("awsSTSAssumeRole", Some(at)).with("roleArn", Field::Text(role_arn));
            if let Some(name) = role_session_name {
                object = object.with("roleSessionName", Field::Text(name));
            }
            Ok(object.with(
                "awsCredentials",
                Field::Object(credentials_of(credentials)?),
            ))
        }
    }
}

/// The walker's `visitAWSCredentials`, for a field that holds credentials.
pub(crate) fn credentials(block: RawBlock) -> R<Object> {
    credentials_of(block)
}
