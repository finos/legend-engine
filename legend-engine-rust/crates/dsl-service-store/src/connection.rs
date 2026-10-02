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

//! `ServiceStoreConnection` (`ServiceStoreConnectionParserGrammar` and its
//! `ServiceStoreConnectionParseTreeWalker`), registered as `dsl-connection`'s
//! connection grammar for the type:
//!
//! ```text
//! value:  ( store: my::Store; | baseUrl: 'http://host'; )*
//! ```
//!
//! The walker checks the store, written at most once and, unless the
//! connection is a runtime's own, required; then the base URL, required and
//! not ending with `/`, which it reports on the field.

use std::any::Any;

use legend_pure_dsl_connection::ast::{ConnectionValue, ConnectionValueBody};
use legend_pure_dsl_connection::grammar::{
    CONNECTION_VALUE_GRAMMARS, ConnectionValueGrammar, ValueSpan,
};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::{missing, optional, required};
use legend_pure_parser_parser::words::{identifier, is_word, qualified_name};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;
use serde::Serialize;
use serde_json::Value;

/// The connection type this grammar reads.
pub const TYPE_NAME: &str = "ServiceStoreConnection";

/// A service store connection, as its walker keeps it.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ServiceStoreConnection {
    /// `store: my::Store;`: the store's path, unquoted, and the path's span.
    pub store: Option<(String, SourceInfo)>,
    /// `baseUrl: '…';`, unquoted.
    pub base_url: String,
}

impl ConnectionValueBody for ServiceStoreConnection {
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

/// The service store connection grammar.
pub struct ServiceStoreConnectionGrammar;

impl ConnectionValueGrammar for ServiceStoreConnectionGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(
        &self,
        span: &ValueSpan,
        ctx: &mut ParserContext<'_>,
    ) -> Result<Box<dyn ConnectionValueBody>, ParseError> {
        let (mut stores, mut urls) = (Vec::new(), Vec::new());
        loop {
            let start = ctx.cursor().current_source_info();
            let field = ctx.cursor().peek().clone();
            if is_word(&field, "store") {
                ctx.cursor().advance();
                ctx.cursor().expect(TokenKind::Colon)?;
                let store = qualified_name(ctx)?;
                stores.push((store.path, store.source_info));
            } else if is_word(&field, "baseUrl") {
                ctx.cursor().advance();
                ctx.cursor().expect(TokenKind::Colon)?;
                let url = identifier(ctx)?.value;
                ctx.cursor().expect(TokenKind::Semicolon)?;
                urls.push((url, ctx.cursor().span_since(&start)));
                continue;
            } else {
                break;
            }
            ctx.cursor().expect(TokenKind::Semicolon)?;
        }
        let at = &span.value;
        let store = optional(stores, "store", at)?;
        if store.is_none() && !span.embedded {
            return Err(missing("store", at));
        }
        let (base_url, url_source_info) = required(urls, "baseUrl", at)?;
        if base_url.ends_with('/') {
            return Err(ParseError::Unexpected {
                message: "baseUrl should not end with '/'".to_string(),
                source_info: url_source_info,
            });
        }
        Ok(Box::new(ServiceStoreConnection {
            store,
            base_url: base_url.to_string(),
        }))
    }

    fn convert(&self, value: &ConnectionValue) -> Result<Value, ConvertError> {
        let connection = value
            .body
            .as_any()
            .downcast_ref::<ServiceStoreConnection>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "the body of a service store connection is not its grammar's",
                    Some(&value.source_info),
                )
            })?;
        Ok(serde_json::to_value(ServiceStoreConnectionJson {
            base_url: &connection.base_url,
            source_information: SourceInformation::from(&value.source_info),
            element: connection.store.as_ref().map(|(path, _)| path.as_str()),
            element_source_information: connection
                .store
                .as_ref()
                .map(|(_, source_info)| SourceInformation::from(source_info)),
        })?)
    }
}

/// The engine's `ServiceStoreConnection`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "serviceStore", rename_all = "camelCase")]
struct ServiceStoreConnectionJson<'a> {
    base_url: &'a str,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    element: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    element_source_information: Option<SourceInformation>,
}

#[distributed_slice(CONNECTION_VALUE_GRAMMARS)]
static SERVICE_STORE_CONNECTION_GRAMMAR: &(dyn ConnectionValueGrammar + Send + Sync) =
    &ServiceStoreConnectionGrammar;
