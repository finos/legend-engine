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

//! The engine's authentication specifications (`AuthenticationParserGrammar`
//! and its `AuthenticationParseTreeWalker`), each the body of
//! `# Type { … }#`:
//!
//! ```text
//! UserPassword:          username: '…'; password: secret;
//! ApiKey:                location: 'header' | 'cookie'; keyName: '…'; value: secret;
//! EncryptedPrivateKey:   userName: '…'; privateKey: secret; passphrase: secret;
//! GCPWIFWithAWSIdP:      serviceAccountEmail: '…';
//!                        idP: AWSIdP { accountId: '…'; region: '…'; role: '…'; awsCredentials: credentials }
//!                        workload: GCPWorkload { projectNumber: '…'; poolId: '…'; providerId: '…'; }
//! Kerberos:              (nothing)
//! PSK:                   psk: '…';
//! ```
//!
//! The fields in any order; once the body is read, the walker checks them,
//! each required once, on the specification, which spans its body to the end
//! of input as ANTLR spans a rule ending with it. A PSK specification has no
//! position, and the walker doesn't read what follows its field.

use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{is_word, unexpected};

use crate::grammar::{AuthenticationGrammar, R, to_end_of_input};
use crate::object::{Field, Object};
use crate::secret::{
    Holds, RawValue, block_of, credentials, one, read_fields, secret, text, text_of,
};

/// The authentication module's grammars.
pub(crate) static CORE_AUTHENTICATIONS: [&(dyn AuthenticationGrammar + Send + Sync); 6] = [
    &UserPassword,
    &ApiKey,
    &EncryptedPrivateKey,
    &GcpWifWithAwsIdp,
    &Kerberos,
    &Psk,
];

/// `UserPassword`.
pub struct UserPassword;
/// `ApiKey`.
pub struct ApiKey;
/// `EncryptedPrivateKey`.
pub struct EncryptedPrivateKey;
/// `GCPWIFWithAWSIdP`.
pub struct GcpWifWithAwsIdp;
/// `Kerberos`.
pub struct Kerberos;
/// `PSK`.
pub struct Psk;

/// The body read whole, then its span: the walker's checks come after the
/// grammar's errors.
fn body(
    ctx: &mut ParserContext<'_>,
    fields: &[(&'static str, Holds)],
) -> R<(
    Vec<Vec<RawValue>>,
    legend_pure_parser_ast::source_info::SourceInfo,
)> {
    let start = ctx.cursor().current_source_info();
    let read = read_fields(ctx, fields)?;
    if !ctx.cursor().check(TokenKind::Eof) {
        return Err(unexpected(ctx.cursor().peek()));
    }
    Ok((read, to_end_of_input(ctx, &start)))
}

impl AuthenticationGrammar for UserPassword {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "UserPassword"
    }

    fn parse(&self, ctx: &mut ParserContext<'_>) -> R<Object> {
        let (read, at) = body(
            ctx,
            &[("username", Holds::Text), ("password", Holds::Secret)],
        )?;
        let mut read = read.into_iter();
        // The walker names the user name so.
        let username = text_of(one(read.next().unwrap_or_default(), "userName", &at)?);
        let password = secret(block_of(one(
            read.next().unwrap_or_default(),
            "password",
            &at,
        )?))?;
        Ok(Object::new("userPassword", Some(at))
            .with("username", Field::Text(username))
            .with("password", Field::Object(password)))
    }
}

impl AuthenticationGrammar for ApiKey {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "ApiKey"
    }

    fn parse(&self, ctx: &mut ParserContext<'_>) -> R<Object> {
        let (read, at) = body(
            ctx,
            &[
                ("location", Holds::Text),
                ("keyName", Holds::Text),
                ("value", Holds::Secret),
            ],
        )?;
        let mut read = read.into_iter();
        let location =
            text_of(one(read.next().unwrap_or_default(), "location", &at)?).to_uppercase();
        if location != "HEADER" && location != "COOKIE" {
            return Err(ParseError::Unsupported {
                message: format!(
                    "'{location}' is no API key location, which the engine reports outside its parser"
                ),
                source_info: at,
            });
        }
        let key_name = text_of(one(read.next().unwrap_or_default(), "keyName", &at)?);
        let value = secret(block_of(one(
            read.next().unwrap_or_default(),
            "value",
            &at,
        )?))?;
        Ok(Object::new("apiKey", Some(at))
            .with("location", Field::Text(location))
            .with("keyName", Field::Text(key_name))
            .with("value", Field::Object(value)))
    }
}

impl AuthenticationGrammar for EncryptedPrivateKey {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "EncryptedPrivateKey"
    }

    fn parse(&self, ctx: &mut ParserContext<'_>) -> R<Object> {
        let (read, at) = body(
            ctx,
            &[
                ("userName", Holds::Text),
                ("privateKey", Holds::Secret),
                ("passphrase", Holds::Secret),
            ],
        )?;
        let mut read = read.into_iter();
        let user_name = text_of(one(read.next().unwrap_or_default(), "userName", &at)?);
        let private_key = secret(block_of(one(
            read.next().unwrap_or_default(),
            "privateKey",
            &at,
        )?))?;
        let passphrase = secret(block_of(one(
            read.next().unwrap_or_default(),
            "passphrase",
            &at,
        )?))?;
        Ok(Object::new("encryptedPrivateKey", Some(at))
            .with("userName", Field::Text(user_name))
            .with("privateKey", Field::Object(private_key))
            .with("passphrase", Field::Object(passphrase)))
    }
}

const AWS_IDP: &[(&str, Holds)] = &[
    ("accountId", Holds::Text),
    ("region", Holds::Text),
    ("role", Holds::Text),
    ("awsCredentials", Holds::Credentials),
];

const GCP_WORKLOAD: &[(&str, Holds)] = &[
    ("projectNumber", Holds::Text),
    ("poolId", Holds::Text),
    ("providerId", Holds::Text),
];

impl AuthenticationGrammar for GcpWifWithAwsIdp {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "GCPWIFWithAWSIdP"
    }

    fn parse(&self, ctx: &mut ParserContext<'_>) -> R<Object> {
        let (read, at) = body(
            ctx,
            &[
                ("serviceAccountEmail", Holds::Text),
                ("idP", Holds::Block("AWSIdP", AWS_IDP)),
                ("workload", Holds::Block("GCPWorkload", GCP_WORKLOAD)),
            ],
        )?;
        let mut read = read.into_iter();
        let email = text_of(one(
            read.next().unwrap_or_default(),
            "serviceAccountEmail",
            &at,
        )?);
        // Each configuration's fields checked on it, `idP` to `}`.
        let idp = block_of(one(read.next().unwrap_or_default(), "idP", &at)?);
        let idp_at = idp.source_info.clone();
        let mut idp_fields = idp.fields.into_iter();
        let mut next = || idp_fields.next().unwrap_or_default();
        let account_id = text_of(one(next(), "accountId", &idp_at)?);
        let region = text_of(one(next(), "region", &idp_at)?);
        let role = text_of(one(next(), "role", &idp_at)?);
        let aws_credentials = credentials(block_of(one(next(), "awsCredentials", &idp_at)?))?;
        let workload = block_of(one(read.next().unwrap_or_default(), "workload", &at)?);
        let workload_at = workload.source_info.clone();
        let mut workload_fields = workload.fields.into_iter();
        let project_number = text_of(one(
            workload_fields.next().unwrap_or_default(),
            "projectNumber",
            &workload_at,
        )?);
        let pool_ids = workload_fields.next().unwrap_or_default();
        let provider_id = text_of(one(
            workload_fields.next().unwrap_or_default(),
            "providerId",
            &workload_at,
        )?);
        let pool_id = text_of(one(pool_ids, "poolId", &workload_at)?);
        let idp_configuration = Object {
            json_type: None,
            fields: Vec::new(),
            source_info: None,
        }
        .with("region", Field::Text(region))
        .with("accountId", Field::Text(account_id))
        .with("role", Field::Text(role))
        .with("awsCredentials", Field::Object(aws_credentials));
        let workload_configuration = Object {
            json_type: None,
            fields: Vec::new(),
            source_info: None,
        }
        .with("projectNumber", Field::Text(project_number))
        .with("poolId", Field::Text(pool_id))
        .with("providerId", Field::Text(provider_id));
        Ok(Object::new("gcpWithAWSIdP", Some(at))
            .with("serviceAccountEmail", Field::Text(email))
            .with("idPConfiguration", Field::Object(idp_configuration))
            .with(
                "workloadConfiguration",
                Field::Object(workload_configuration),
            )
            .with("additionalGcpScopes", Field::Texts(Vec::new())))
    }
}

impl AuthenticationGrammar for Kerberos {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "Kerberos"
    }

    fn parse(&self, ctx: &mut ParserContext<'_>) -> R<Object> {
        let (_, at) = body(ctx, &[])?;
        Ok(Object::new("kerberos", Some(at)))
    }
}

impl AuthenticationGrammar for Psk {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "PSK"
    }

    fn parse(&self, ctx: &mut ParserContext<'_>) -> R<Object> {
        let keyword = ctx.cursor().peek().clone();
        if !is_word(&keyword, "psk") {
            return Err(unexpected(&keyword));
        }
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::Colon)?;
        let psk = text(ctx)?;
        ctx.cursor().expect(TokenKind::Semicolon)?;
        // The rule ends with the field.
        while !ctx.cursor().check(TokenKind::Eof) {
            ctx.cursor().advance();
        }
        Ok(Object::new("PSK", None).with("psk", Field::Text(psk)))
    }
}
