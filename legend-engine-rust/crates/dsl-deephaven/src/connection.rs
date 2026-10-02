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

//! `DeephavenConnection` (`DeephavenConnectionParserGrammar` and the
//! extension's `DeephavenParseTreeWalker`), registered as `dsl-connection`'s
//! connection grammar for the type:
//!
//! ```text
//! value:  ( store: my::Store; | serverUrl: 'http://…' | authentication: # Type { … }#; )*
//! ```
//!
//! The server's URL ends without `;`. The walker positions the connection on
//! its body, from its first token to the end of input, whose text, `<EOF>`,
//! ANTLR counts; then checks the store, the URL and the authentication, each
//! required, named in its messages as its lexer names it (`'store'`), and
//! the URL a `java.net.URI` (its quotes, not its escapes, taken off). Rust
//! reports a URL `URI` rejects for an illegal character or a malformed
//! escape, takes a plain `scheme://authority/path?query` one, and leaves any
//! other to the engine.

use std::any::Any;

use legend_pure_dsl_authentication::grammar::parse_authentication;
use legend_pure_dsl_authentication::object::Object;
use legend_pure_dsl_connection::ast::{ConnectionValue, ConnectionValueBody};
use legend_pure_dsl_connection::grammar::{
    CONNECTION_VALUE_GRAMMARS, ConnectionValueGrammar, ValueSpan,
};
use legend_pure_dsl_connection::uri::{JavaUri, java_uri};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::required;
use legend_pure_parser_parser::words::{is_string, is_word, qualified_name, unexpected};
use legend_pure_parser_parser::{Island, ParserContext};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;
use serde::Serialize;
use serde_json::Value;

type R<T> = Result<T, ParseError>;

/// The connection type this grammar reads.
pub const TYPE_NAME: &str = "DeephavenConnection";

/// A Deephaven connection, as its walker keeps it.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DeephavenConnection {
    /// The store's path, unquoted, and the path's span.
    pub store: (String, SourceInfo),
    /// The server's URL.
    pub url: String,
    /// The authentication.
    pub authentication: Object,
    /// The body, from its first token to the end of input.
    pub source_info: SourceInfo,
}

impl ConnectionValueBody for DeephavenConnection {
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

/// The Deephaven connection grammar.
pub struct DeephavenConnectionGrammar;

impl ConnectionValueGrammar for DeephavenConnectionGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(
        &self,
        _span: &ValueSpan,
        ctx: &mut ParserContext<'_>,
    ) -> R<Box<dyn ConnectionValueBody>> {
        let start = ctx.cursor().current_source_info();
        let (mut stores, mut urls, mut authentications) =
            (Vec::new(), Vec::new(), Vec::<Island>::new());
        loop {
            let field_start = ctx.cursor().current_source_info();
            let field = ctx.cursor().peek().clone();
            if is_word(&field, "store") {
                ctx.cursor().advance();
                ctx.cursor().expect(TokenKind::Colon)?;
                let store = qualified_name(ctx)?;
                ctx.cursor().expect(TokenKind::Semicolon)?;
                stores.push((store.path, store.source_info));
            } else if is_word(&field, "serverUrl") {
                ctx.cursor().advance();
                ctx.cursor().expect(TokenKind::Colon)?;
                let url = ctx.cursor().peek().clone();
                if !is_string(&url) {
                    return Err(unexpected(&url));
                }
                ctx.cursor().advance();
                urls.push((url.text.to_string(), ctx.cursor().span_since(&field_start)));
            } else if is_word(&field, "authentication") {
                ctx.cursor().advance();
                ctx.cursor().expect(TokenKind::Colon)?;
                authentications.push(ctx.read_island()?);
                ctx.cursor().expect(TokenKind::Semicolon)?;
            } else {
                break;
            }
        }
        if !ctx.cursor().check(TokenKind::Eof) {
            return Err(unexpected(ctx.cursor().peek()));
        }
        // The walker, on the body: from its first token to the end of input.
        let end = ctx.cursor().current_source_info();
        let at = SourceInfo::new(
            start.source.clone(),
            start.start_line,
            start.start_column,
            end.start_line,
            end.start_column + 4,
        );
        let store = required(stores, "'store'", &at)?;
        let (url, url_source_info) = required(urls, "'serverUrlDefinition'", &at)?;
        let url = url.trim();
        let url = url.strip_prefix('\'').unwrap_or(url);
        let url = url.strip_suffix('\'').unwrap_or(url).to_string();
        match java_uri(&url) {
            JavaUri::Valid => {}
            JavaUri::Invalid => {
                return Err(ParseError::Unexpected {
                    message: "URL format is not valid".to_string(),
                    source_info: url_source_info,
                });
            }
            JavaUri::Unknown => {
                return Err(ParseError::Unsupported {
                    message: format!("whether '{url}' is a URI is the engine's to say"),
                    source_info: url_source_info,
                });
            }
        }
        let authentication = required(authentications, "'authentication'", &at)?;
        let authentication = parse_authentication(ctx, &authentication)?;
        Ok(Box::new(DeephavenConnection {
            store,
            url,
            authentication,
            source_info: at,
        }))
    }

    fn convert(&self, value: &ConnectionValue) -> Result<Value, ConvertError> {
        let connection = value
            .body
            .as_any()
            .downcast_ref::<DeephavenConnection>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "the body of a Deephaven connection is not its grammar's",
                    Some(&value.source_info),
                )
            })?;
        let (store, store_source_info) = &connection.store;
        Ok(serde_json::to_value(DeephavenConnectionJson {
            element: store,
            element_source_information: SourceInformation::from(store_source_info),
            source_spec: SourceSpecJson {
                url: &connection.url,
            },
            auth_spec: &connection.authentication,
            source_information: SourceInformation::from(&connection.source_info),
        })?)
    }
}

/// The engine's connection JSON.
#[derive(Serialize)]
#[serde(
    tag = "_type",
    rename = "deephavenConnection",
    rename_all = "camelCase"
)]
struct DeephavenConnectionJson<'a> {
    element: &'a str,
    element_source_information: SourceInformation,
    source_spec: SourceSpecJson<'a>,
    auth_spec: &'a Object,
    source_information: SourceInformation,
}

/// The connection's source specification: its URL.
#[derive(Serialize)]
struct SourceSpecJson<'a> {
    url: &'a str,
}

#[distributed_slice(CONNECTION_VALUE_GRAMMARS)]
static DEEPHAVEN_CONNECTION_GRAMMAR: &(dyn ConnectionValueGrammar + Send + Sync) =
    &DeephavenConnectionGrammar;
