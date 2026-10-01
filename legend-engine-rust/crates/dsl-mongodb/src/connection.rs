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

//! `MongoDBConnection` (`MongoDBConnectionParserGrammar` and its
//! `MongoDBConnectionParseTreeWalker`), registered as `dsl-connection`'s
//! connection grammar for the type:
//!
//! ```text
//! value:  ( store: my::Store; | database: name; | serverURLs: [ host:27017, … ];
//!         | authentication: # Type { … }#; )*
//! ```
//!
//! The grammar's lexer reads a host, `my-host.example.com`, as one word, of
//! letters, digits, `_`, `$`, `-` and `.`, from a letter or `_`; so is an
//! unquoted database name. The walker checks the store (optional), then the
//! database and the servers, then the authentication (each required, on the
//! connection), which `dsl-authentication` reads.

use std::any::Any;

use legend_pure_dsl_authentication::grammar::parse_authentication;
use legend_pure_dsl_authentication::object::Object;
use legend_pure_dsl_connection::ast::{ConnectionValue, ConnectionValueBody};
use legend_pure_dsl_connection::grammar::{
    CONNECTION_VALUE_GRAMMARS, ConnectionValueGrammar, ValueSpan,
};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::{optional, required};
use legend_pure_parser_parser::words::{
    adjacent, is_string, is_word, is_word_kind, qualified_name, unexpected,
};
use legend_pure_parser_parser::{Island, ParserContext, string_value};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;
use serde_json::{Value, json};

type R<T> = Result<T, ParseError>;

/// The connection type this grammar reads.
pub const TYPE_NAME: &str = "MongoDBConnection";

/// A MongoDB connection, as its walker keeps it.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MongoDbConnection {
    /// `store: my::Store;`: the store's path, unquoted, and the path's span.
    pub store: Option<(String, SourceInfo)>,
    /// `database: name;`, unquoted.
    pub database: String,
    /// `serverURLs: [ host:port, … ];`.
    pub servers: Vec<(String, i64)>,
    /// `authentication: # Type { … }#;`.
    pub authentication: Object,
}

impl ConnectionValueBody for MongoDbConnection {
    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn ConnectionValueBody> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn ConnectionValueBody) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}

/// The MongoDB connection grammar.
pub struct MongoDbConnectionGrammar;

impl ConnectionValueGrammar for MongoDbConnectionGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(
        &self,
        span: &ValueSpan,
        ctx: &mut ParserContext<'_>,
    ) -> R<Box<dyn ConnectionValueBody>> {
        let (mut stores, mut databases, mut servers, mut authentications) =
            (Vec::new(), Vec::new(), Vec::new(), Vec::<Island>::new());
        loop {
            let field = ctx.cursor().peek().clone();
            let is = |name: &str| is_word(&field, name);
            if !(is("store") || is("database") || is("serverURLs") || is("authentication")) {
                break;
            }
            ctx.cursor().advance();
            ctx.cursor().expect(TokenKind::Colon)?;
            match field.text.as_str() {
                "store" => {
                    let store = qualified_name(ctx)?;
                    stores.push((store.path, store.source_info));
                }
                "database" => databases.push(identifier(ctx)?),
                "serverURLs" => servers.push(server_urls(ctx)?),
                _ => authentications.push(ctx.read_island()?),
            }
            ctx.cursor().expect(TokenKind::Semicolon)?;
        }
        // The walker.
        let at = &span.value;
        let store = optional(stores, "store", at)?;
        let database = required(databases, "database", at)?;
        let servers = required(servers, "serverURLs", at)?;
        // The walker names the field as its lexer does: quoted.
        let authentication = required(authentications, "'authentication'", at)?;
        let authentication = parse_authentication(ctx, &authentication)?;
        Ok(Box::new(MongoDbConnection {
            store,
            database,
            servers,
            authentication,
        }))
    }

    fn convert(&self, value: &ConnectionValue) -> Result<Value, ConvertError> {
        let connection = value
            .body
            .as_any()
            .downcast_ref::<MongoDbConnection>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "the body of a MongoDB connection is not its grammar's",
                    Some(&value.source_info),
                )
            })?;
        let mut json = json!({
            "_type": TYPE_NAME,
            "type": "MongoDb",
            "dataSourceSpecification": {
                "databaseName": connection.database,
                "serverURLs": connection.servers.iter().map(|(host, port)| json!({"baseUrl": host, "port": port})).collect::<Vec<_>>(),
            },
            "authenticationSpecification": connection.authentication.to_json(),
            "sourceInformation": SourceInformation::from(&value.source_info),
        });
        if let Some((path, source_info)) = &connection.store {
            json["element"] = json!(path);
            json["elementSourceInformation"] = json!(SourceInformation::from(source_info));
        }
        Ok(json)
    }
}

/// `[ host:port, … ]`, at least one.
fn server_urls(ctx: &mut ParserContext<'_>) -> R<Vec<(String, i64)>> {
    ctx.cursor().expect(TokenKind::LBracket)?;
    let mut servers = vec![server_url(ctx)?];
    while ctx.cursor().eat(TokenKind::Comma) {
        servers.push(server_url(ctx)?);
    }
    ctx.cursor().expect(TokenKind::RBracket)?;
    Ok(servers)
}

/// `host:port`.
fn server_url(ctx: &mut ParserContext<'_>) -> R<(String, i64)> {
    let host = host(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let port = ctx.cursor().peek().clone();
    if port.kind != TokenKind::IntegerLiteral {
        return Err(unexpected(&port));
    }
    ctx.cursor().advance();
    let port_number = port.text.parse().map_err(|_| ParseError::Unsupported {
        message: format!(
            "{} is too large a port, which the engine reports",
            port.text
        ),
        source_info: port.source_info.clone(),
    })?;
    Ok((host, port_number))
}

/// A host, as the grammar's lexer reads it: the adjacent tokens of a word
/// from a letter or `_`, of letters, digits, `_`, `$`, `-` and `.`.
fn host(ctx: &mut ParserContext<'_>) -> R<String> {
    let first = ctx.cursor().peek().clone();
    let starts = first
        .text
        .starts_with(|c: char| c.is_ascii_alphabetic() || c == '_');
    if !is_word_kind(first.kind) || !starts {
        return Err(unexpected(&first));
    }
    ctx.cursor().advance();
    let mut host = first.text.to_string();
    let mut previous = first;
    loop {
        let next = ctx.cursor().peek().clone();
        let joins = matches!(
            next.kind,
            TokenKind::Minus | TokenKind::Dot | TokenKind::Dollar | TokenKind::IntegerLiteral
        ) || is_word_kind(next.kind);
        if !joins || !adjacent(&previous, &next) {
            break;
        }
        if !next
            .text
            .chars()
            .all(|c| c.is_ascii_alphanumeric() || matches!(c, '_' | '$' | '-' | '.'))
        {
            break;
        }
        ctx.cursor().advance();
        host.push_str(&next.text);
        previous = next;
    }
    Ok(host)
}

/// The grammar's `identifier`: a quoted name, unquoted, or a word as its
/// lexer reads it.
fn identifier(ctx: &mut ParserContext<'_>) -> R<String> {
    let token = ctx.cursor().peek().clone();
    if is_string(&token) {
        ctx.cursor().advance();
        return string_value(&token.text)
            .map(|(value, _)| value)
            .map_err(|message| ParseError::Unexpected {
                message,
                source_info: token.source_info,
            });
    }
    host(ctx)
}

#[distributed_slice(CONNECTION_VALUE_GRAMMARS)]
static MONGODB_CONNECTION_GRAMMAR: &(dyn ConnectionValueGrammar + Send + Sync) =
    &MongoDbConnectionGrammar;
