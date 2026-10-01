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

//! `Elasticsearch7ClusterConnection` (`ElasticsearchConnectionParserGrammar`
//! and the extension's `ElasticsearchStoreParseTreeWalker`), registered as
//! `dsl-connection`'s connection grammar for the type:
//!
//! ```text
//! value:  ( store: my::Store; | clusterDetails: # URL { http://… }#;
//!         | authentication: # Type { … }#; )*
//! ```
//!
//! The walker positions the connection on its body, from its first token to
//! the end of input, whose text, `<EOF>`, ANTLR counts; then checks the
//! store, the cluster's details and the authentication, each required,
//! named in its messages as its lexer names it (`'store'`). The details are
//! a URL, `# URL { … }#` (another type is reported on `# Type {`), its text
//! trimmed a `java.net.URI` (one it rejects is reported from the text to
//! `}#`; see `dsl-connection`'s `uri`).

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
use legend_pure_parser_parser::words::{is_word, qualified_name, unexpected};
use legend_pure_parser_parser::{Island, ParserContext};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;
use serde_json::{Value, json};

type R<T> = Result<T, ParseError>;

/// The connection type this grammar reads.
pub const TYPE_NAME: &str = "Elasticsearch7ClusterConnection";

/// An Elasticsearch connection, as its walker keeps it.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ElasticsearchConnection {
    /// The store's path, unquoted, and the path's span.
    pub store: (String, SourceInfo),
    /// The cluster's URL.
    pub url: String,
    /// The authentication.
    pub authentication: Object,
    /// The body, from its first token to the end of input.
    pub source_info: SourceInfo,
}

impl ConnectionValueBody for ElasticsearchConnection {
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

/// The Elasticsearch connection grammar.
pub struct ElasticsearchConnectionGrammar;

impl ConnectionValueGrammar for ElasticsearchConnectionGrammar {
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
        let (mut stores, mut details, mut authentications) =
            (Vec::new(), Vec::<Island>::new(), Vec::<Island>::new());
        loop {
            let field = ctx.cursor().peek().clone();
            if is_word(&field, "store") {
                ctx.cursor().advance();
                ctx.cursor().expect(TokenKind::Colon)?;
                let store = qualified_name(ctx)?;
                stores.push((store.path, store.source_info));
            } else if is_word(&field, "clusterDetails") || is_word(&field, "authentication") {
                ctx.cursor().advance();
                ctx.cursor().expect(TokenKind::Colon)?;
                let island = ctx.read_island()?;
                if field.text == "clusterDetails" {
                    details.push(island);
                } else {
                    authentications.push(island);
                }
            } else {
                break;
            }
            ctx.cursor().expect(TokenKind::Semicolon)?;
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
        let details = required(details, "'clusterDetails'", &at)?;
        let url = cluster_url(&details)?;
        let authentication = required(authentications, "'authentication'", &at)?;
        let authentication = parse_authentication(ctx, &authentication)?;
        Ok(Box::new(ElasticsearchConnection {
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
            .downcast_ref::<ElasticsearchConnection>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "the body of an Elasticsearch connection is not its grammar's",
                    Some(&value.source_info),
                )
            })?;
        let (store, store_source_info) = &connection.store;
        Ok(json!({
            "_type": "elasticsearch7StoreConnection",
            "element": store,
            "elementSourceInformation": SourceInformation::from(store_source_info),
            "sourceSpec": {"url": connection.url},
            "authSpec": connection.authentication.to_json(),
            "sourceInformation": SourceInformation::from(&connection.source_info),
        }))
    }
}

/// `# URL { http://… }#`: the URL, trimmed.
fn cluster_url(details: &Island) -> R<String> {
    let type_name = details.tag().trim();
    if type_name != "URL" {
        return Err(ParseError::Unexpected {
            message: format!("Unsupported cluster details type: {type_name}.  Supported: URL"),
            source_info: details.open_source_info().clone(),
        });
    }
    let url = details.text().trim().to_string();
    // From the text to `}#`.
    let body = details.body_source_info();
    let text_span = SourceInfo::new(
        body.source.clone(),
        body.start_line,
        body.start_column,
        details.source_info.end_line,
        details.source_info.end_column,
    );
    match java_uri(&url) {
        JavaUri::Valid => Ok(url),
        JavaUri::Invalid => Err(ParseError::Unexpected {
            message: "URL is not valid".to_string(),
            source_info: text_span,
        }),
        JavaUri::Unknown => Err(ParseError::Unsupported {
            message: format!("whether '{url}' is a URI is the engine's to say"),
            source_info: text_span,
        }),
    }
}

#[distributed_slice(CONNECTION_VALUE_GRAMMARS)]
static ELASTICSEARCH_CONNECTION_GRAMMAR: &(dyn ConnectionValueGrammar + Send + Sync) =
    &ElasticsearchConnectionGrammar;
