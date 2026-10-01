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

//! `ServiceStore #{ … }#` test data (`ServiceStoreEmbeddedDataParserGrammar`
//! and its `ServiceStoreEmbeddedDataParseTreeWalker`), the engine's
//! `ServiceStoreEmbeddedData`: stubs of the services' requests and
//! responses.
//!
//! ```text
//! definition:  [ ( { stub } (, { stub })* )? ]
//! stub:        ( request: { field* }; | response: { (body: ExternalFormat #{ … }#;)* }; )*
//! field:       method: GET; | url: '…'; | urlPath: '…'; | bodyPatterns: [ pattern* ];
//!            | headerParameters: { name: pattern, … }; | queryParameters: { name: pattern, … };
//! pattern:     EqualTo #{ expected: '…'; }# | EqualToJson #{ expected: '…'; }#
//! ```
//!
//! A stub needs a request and a response; a request, its method (`GET` or
//! `POST`, in any case); a response, its body, external format data. Each
//! field may be written once, each parameter once.

use std::any::Any;
use std::collections::HashSet;

use legend_pure_parser_ast::data::{EmbeddedData, EmbeddedDataContent, EmbeddedDataValue};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::{optional, required};
use legend_pure_parser_parser::words::{identifier, is_string, is_word_kind, unexpected};
use legend_pure_parser_parser::{
    EmbeddedDataParser, ParserContext, TypedIsland, raw_string_value, string_value,
};
use legend_pure_parser_protocol::EmbeddedDataProtocol;
use legend_pure_parser_protocol::embedded_data_protocol::{
    ConvertError, EMBEDDED_DATA_PROTOCOLS, Result as ConvertResult,
};
use legend_pure_parser_protocol::v1::convert::convert_embedded_data;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;
use serde_json::{Map, Value, json};

/// The embedded data type this grammar reads.
pub const TYPE_NAME: &str = "ServiceStore";

/// The engine's `HttpMethod`s.
const METHODS: [&str; 2] = ["GET", "POST"];

/// `ServiceStore #{ … }#`: stubs.
#[derive(Debug, Clone, PartialEq)]
pub struct ServiceStoreData {
    /// The stubs.
    pub stubs: Vec<ServiceStub>,
    /// The whole embedded data, from `ServiceStore` to `}#`.
    pub source_info: SourceInfo,
}

/// `{ request: …; response: …; }`.
#[derive(Debug, Clone, PartialEq)]
pub struct ServiceStub {
    /// The request.
    pub request: RequestPattern,
    /// The response.
    pub response: ResponseDefinition,
    /// From `{` to `}`.
    pub source_info: SourceInfo,
}

/// `request: { … };`.
#[derive(Debug, Clone, PartialEq)]
pub struct RequestPattern {
    /// The method, upper case.
    pub method: String,
    /// `url: '…';`, unescaped.
    pub url: Option<String>,
    /// `urlPath: '…';`, unescaped.
    pub url_path: Option<String>,
    /// `headerParameters: { … };`.
    pub header_parameters: Option<Vec<(String, ContentPattern)>>,
    /// `queryParameters: { … };`.
    pub query_parameters: Option<Vec<(String, ContentPattern)>>,
    /// `bodyPatterns: [ … ];`.
    pub body_patterns: Option<Vec<ContentPattern>>,
    /// From `request` to `;`.
    pub source_info: SourceInfo,
}

/// `response: { body: …; };`.
#[derive(Debug, Clone, PartialEq)]
pub struct ResponseDefinition {
    /// The body, external format data.
    pub body: EmbeddedDataValue,
    /// From `response` to `;`.
    pub source_info: SourceInfo,
}

/// A content pattern: `EqualTo #{ expected: '…'; }#` (the value unescaped)
/// or `EqualToJson #{ expected: '…'; }#` (the value as written).
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ContentPattern {
    /// Equal to a value.
    EqualTo(String),
    /// Equal to JSON.
    EqualToJson(String),
}

impl EmbeddedDataContent for ServiceStoreData {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn EmbeddedDataContent> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn EmbeddedDataContent) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}

/// Parser for `ServiceStore #{ … }#` test data.
pub struct ServiceStoreDataParser;

impl EmbeddedDataParser for ServiceStoreDataParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(&self, ctx: &mut ParserContext<'_>) -> Result<EmbeddedData, ParseError> {
        let source_info = ctx
            .island_span()
            .cloned()
            .unwrap_or_else(|| ctx.cursor().current_source_info());
        ctx.cursor().expect(TokenKind::LBracket)?;
        let mut stubs = Vec::new();
        if !ctx.cursor().check(TokenKind::RBracket) {
            loop {
                stubs.push(stub(ctx)?);
                if !ctx.cursor().eat(TokenKind::Comma) {
                    break;
                }
            }
        }
        ctx.cursor().expect(TokenKind::RBracket)?;
        Ok(EmbeddedData::Extension(Box::new(ServiceStoreData {
            stubs,
            source_info,
        })))
    }
}

/// A field's parts as the grammar reads them, checked by the walker.
struct ReadRequest {
    methods: Vec<(legend_pure_parser_parser::words::Name, SourceInfo)>,
    urls: Vec<(String, SourceInfo)>,
    url_paths: Vec<(String, SourceInfo)>,
    header_parameters: Vec<Vec<ReadParameter>>,
    query_parameters: Vec<Vec<ReadParameter>>,
    body_patterns: Vec<Vec<TypedIsland>>,
    source_info: SourceInfo,
}

struct ReadParameter {
    name: String,
    pattern: TypedIsland,
    source_info: SourceInfo,
}

/// `{ (request: …; | response: …;)* }`, then the walker's checks.
fn stub(ctx: &mut ParserContext<'_>) -> Result<ServiceStub, ParseError> {
    let start = ctx.cursor().current_source_info();
    ctx.cursor().expect(TokenKind::LBrace)?;
    let (mut requests, mut responses) = (Vec::new(), Vec::new());
    while !ctx.cursor().check(TokenKind::RBrace) {
        let head = ctx.cursor().peek().clone();
        let head_start = head.source_info.clone();
        if is_word_kind(head.kind) && head.text == "request" {
            ctx.cursor().advance();
            ctx.cursor().expect(TokenKind::Colon)?;
            requests.push(read_request(ctx, head_start)?);
        } else if is_word_kind(head.kind) && head.text == "response" {
            ctx.cursor().advance();
            ctx.cursor().expect(TokenKind::Colon)?;
            ctx.cursor().expect(TokenKind::LBrace)?;
            let mut bodies = Vec::new();
            while !ctx.cursor().check(TokenKind::RBrace) {
                let body = ctx.cursor().peek().clone();
                if !(is_word_kind(body.kind) && body.text == "body") {
                    return Err(unexpected(&body));
                }
                ctx.cursor().advance();
                ctx.cursor().expect(TokenKind::Colon)?;
                bodies.push(ctx.read_typed_island()?);
                ctx.cursor().expect(TokenKind::Semicolon)?;
            }
            ctx.cursor().expect(TokenKind::RBrace)?;
            ctx.cursor().expect(TokenKind::Semicolon)?;
            responses.push((bodies, ctx.cursor().span_since(&head_start)));
        } else {
            return Err(unexpected(&head));
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    let source_info = ctx.cursor().span_since(&start);
    // The walker: a request and a response, each once, then the request's
    // fields, then the response's.
    let request = required(requests, "request", &source_info)?;
    let response = required(responses, "response", &source_info)?;
    let request = walk_request(ctx, request)?;
    let (bodies, response_source_info) = response;
    let body = required(bodies, "body", &response_source_info)?;
    let body_span = body.source_info.clone();
    let body = ctx.parse_embedded_data_island(&body)?;
    if !matches!(body.data, EmbeddedData::ExternalFormat(_)) {
        return Err(ParseError::Unexpected {
            message: "Service response body should be ExternalFormat Embedded Data".to_string(),
            source_info: body_span,
        });
    }
    Ok(ServiceStub {
        request,
        response: ResponseDefinition {
            body,
            source_info: response_source_info,
        },
        source_info,
    })
}

/// `{ field* };` after `request:`.
fn read_request(ctx: &mut ParserContext<'_>, start: SourceInfo) -> Result<ReadRequest, ParseError> {
    ctx.cursor().expect(TokenKind::LBrace)?;
    let mut read = ReadRequest {
        methods: Vec::new(),
        urls: Vec::new(),
        url_paths: Vec::new(),
        header_parameters: Vec::new(),
        query_parameters: Vec::new(),
        body_patterns: Vec::new(),
        source_info: start.clone(),
    };
    while !ctx.cursor().check(TokenKind::RBrace) {
        let field = ctx.cursor().peek().clone();
        let field_start = field.source_info.clone();
        if !is_word_kind(field.kind) {
            return Err(unexpected(&field));
        }
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::Colon)?;
        match field.text.as_str() {
            "method" => {
                let method = identifier(ctx)?;
                ctx.cursor().expect(TokenKind::Semicolon)?;
                read.methods
                    .push((method, ctx.cursor().span_since(&field_start)));
            }
            "url" | "urlPath" => {
                let value = ctx.cursor().peek().clone();
                if !is_string(&value) {
                    return Err(unexpected(&value));
                }
                ctx.cursor().advance();
                ctx.cursor().expect(TokenKind::Semicolon)?;
                let (text, _) =
                    string_value(&value.text).map_err(|message| ParseError::Unexpected {
                        message,
                        source_info: value.source_info.clone(),
                    })?;
                let entry = (text, ctx.cursor().span_since(&field_start));
                if field.text == "url" {
                    read.urls.push(entry);
                } else {
                    read.url_paths.push(entry);
                }
            }
            "headerParameters" | "queryParameters" => {
                ctx.cursor().expect(TokenKind::LBrace)?;
                let mut parameters = vec![read_parameter(ctx)?];
                while ctx.cursor().eat(TokenKind::Comma) {
                    parameters.push(read_parameter(ctx)?);
                }
                ctx.cursor().expect(TokenKind::RBrace)?;
                ctx.cursor().expect(TokenKind::Semicolon)?;
                if field.text == "headerParameters" {
                    read.header_parameters.push(parameters);
                } else {
                    read.query_parameters.push(parameters);
                }
            }
            "bodyPatterns" => {
                ctx.cursor().expect(TokenKind::LBracket)?;
                let mut patterns = Vec::new();
                while !ctx.cursor().check(TokenKind::RBracket) {
                    patterns.push(ctx.read_typed_island()?);
                }
                ctx.cursor().expect(TokenKind::RBracket)?;
                ctx.cursor().expect(TokenKind::Semicolon)?;
                read.body_patterns.push(patterns);
            }
            _ => return Err(unexpected(&field)),
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    read.source_info = ctx.cursor().span_since(&start);
    Ok(read)
}

/// `name: Pattern #{ … }#`, the name a word or a `"double-quoted"` name.
fn read_parameter(ctx: &mut ParserContext<'_>) -> Result<ReadParameter, ParseError> {
    let start = ctx.cursor().current_source_info();
    let token = ctx.cursor().peek().clone();
    let name = if is_word_kind(token.kind) {
        ctx.cursor().advance();
        token.text.to_string()
    } else if token.kind == TokenKind::StringLiteral && token.text.starts_with('"') {
        ctx.cursor().advance();
        string_value(&token.text)
            .map(|(value, _)| value)
            .map_err(|message| ParseError::Unexpected {
                message,
                source_info: token.source_info.clone(),
            })?
    } else {
        return Err(unexpected(&token));
    };
    ctx.cursor().expect(TokenKind::Colon)?;
    let pattern = ctx.read_typed_island()?;
    Ok(ReadParameter {
        name,
        pattern,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// The walker's `visitServiceRequestPattern`.
fn walk_request(
    ctx: &mut ParserContext<'_>,
    read: ReadRequest,
) -> Result<RequestPattern, ParseError> {
    let source_info = read.source_info;
    let (method, method_source_info) = required(read.methods, "method", &source_info)?;
    let method_value = method.value.to_uppercase();
    if !METHODS.contains(&method_value.as_str()) {
        return Err(ParseError::Unexpected {
            message: format!(
                "Unsupported HTTP Method type - {method_value}. Supported types are - {}",
                METHODS.join(",")
            ),
            source_info: method_source_info,
        });
    }
    let url = optional(read.urls, "url", &source_info)?.map(|(url, _)| url);
    let url_path = optional(read.url_paths, "urlPath", &source_info)?.map(|(path, _)| path);
    let query_parameters = optional(read.query_parameters, "queryParameters", &source_info)?
        .map(|parameters| walk_parameters(ctx, parameters, "Query Param"))
        .transpose()?;
    let header_parameters = optional(read.header_parameters, "headerParameters", &source_info)?
        .map(|parameters| walk_parameters(ctx, parameters, "Header Param"))
        .transpose()?;
    let body_patterns = optional(read.body_patterns, "bodyPatterns", &source_info)?
        .map(|patterns| {
            patterns
                .iter()
                .map(|pattern| content_pattern(ctx, pattern))
                .collect::<Result<Vec<_>, ParseError>>()
        })
        .transpose()?;
    Ok(RequestPattern {
        method: method_value,
        url,
        url_path,
        header_parameters,
        query_parameters,
        body_patterns,
        source_info,
    })
}

/// Parameters, each once.
fn walk_parameters(
    ctx: &mut ParserContext<'_>,
    parameters: Vec<ReadParameter>,
    what: &str,
) -> Result<Vec<(String, ContentPattern)>, ParseError> {
    let mut seen = HashSet::new();
    let mut walked = Vec::new();
    for parameter in parameters {
        let pattern = content_pattern(ctx, &parameter.pattern)?;
        if !seen.insert(parameter.name.clone()) {
            return Err(ParseError::Unexpected {
                message: format!(
                    "{what} : '{}' value should be defined only once",
                    parameter.name
                ),
                source_info: parameter.source_info,
            });
        }
        walked.push((parameter.name, pattern));
    }
    Ok(walked)
}

/// A content pattern (the extension's `HelperContentPatternGrammarParser`):
/// `EqualTo` or `EqualToJson`, `expected: '…';`.
fn content_pattern(
    ctx: &mut ParserContext<'_>,
    island: &TypedIsland,
) -> Result<ContentPattern, ParseError> {
    let equal_to = match island.type_name.as_str() {
        "EqualTo" => true,
        "EqualToJson" => false,
        other => {
            return Err(ParseError::Unexpected {
                message: format!("Unknown contentPattern pattern type: {other}"),
                source_info: island.type_source_info.clone(),
            });
        }
    };
    if island.text().is_empty() {
        return Err(ParseError::Unexpected {
            message: "Content Pattern must not be empty".to_string(),
            source_info: island.source_info.clone(),
        });
    }
    ctx.parse_typed_island(island, |body| {
        let expected = body.cursor().peek().clone();
        if !(is_word_kind(expected.kind) && expected.text == "expected") {
            return Err(unexpected(&expected));
        }
        body.cursor().advance();
        body.cursor().expect(TokenKind::Colon)?;
        let value = body.cursor().peek().clone();
        if !is_string(&value) {
            return Err(unexpected(&value));
        }
        body.cursor().advance();
        body.cursor().expect(TokenKind::Semicolon)?;
        Ok(if equal_to {
            let (text, _) =
                string_value(&value.text).map_err(|message| ParseError::Unexpected {
                    message,
                    source_info: value.source_info.clone(),
                })?;
            ContentPattern::EqualTo(text)
        } else {
            ContentPattern::EqualToJson(raw_string_value(&value.text))
        })
    })
}

#[linkme::distributed_slice(legend_pure_parser_parser::embedded_data::EMBEDDED_DATA_PARSERS)]
static SERVICE_STORE_DATA_PARSER: &(dyn EmbeddedDataParser + Send + Sync) = &ServiceStoreDataParser;

// ── The protocol ───────────────────────────────────────────────────────

/// Protocol converter for service store test data.
pub struct ServiceStoreDataProtocol;

impl EmbeddedDataProtocol for ServiceStoreDataProtocol {
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn convert(&self, content: &dyn EmbeddedDataContent) -> ConvertResult<Value> {
        let data = content
            .as_any()
            .downcast_ref::<ServiceStoreData>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "ServiceStoreDataProtocol: '{}' data is not ServiceStoreData",
                        content.type_name()
                    ),
                    None,
                )
            })?;
        Ok(json!({
            "_type": "serviceStore",
            "serviceStubMappings": data.stubs.iter().map(stub_json).collect::<ConvertResult<Vec<_>>>()?,
            "sourceInformation": position(&data.source_info),
        }))
    }
}

fn position(source_info: &SourceInfo) -> Value {
    json!(SourceInformation::from(source_info))
}

fn stub_json(stub: &ServiceStub) -> ConvertResult<Value> {
    let request = &stub.request;
    let mut request_json = json!({
        "method": request.method,
        "sourceInformation": position(&request.source_info),
    });
    if let Some(url) = &request.url {
        request_json["url"] = json!(url);
    }
    if let Some(path) = &request.url_path {
        request_json["urlPath"] = json!(path);
    }
    if let Some(parameters) = &request.header_parameters {
        request_json["headerParams"] = parameters_json(parameters);
    }
    if let Some(parameters) = &request.query_parameters {
        request_json["queryParams"] = parameters_json(parameters);
    }
    if let Some(patterns) = &request.body_patterns {
        request_json["bodyPatterns"] = json!(patterns.iter().map(pattern_json).collect::<Vec<_>>());
    }
    Ok(json!({
        "requestPattern": request_json,
        "responseDefinition": {
            "body": convert_embedded_data(&stub.response.body)?,
            "sourceInformation": position(&stub.response.source_info),
        },
        "sourceInformation": position(&stub.source_info),
    }))
}

fn parameters_json(parameters: &[(String, ContentPattern)]) -> Value {
    let mut map = Map::new();
    for (name, pattern) in parameters {
        map.insert(name.clone(), pattern_json(pattern));
    }
    Value::Object(map)
}

fn pattern_json(pattern: &ContentPattern) -> Value {
    match pattern {
        ContentPattern::EqualTo(value) => json!({"_type": "equalTo", "expectedValue": value}),
        ContentPattern::EqualToJson(value) => {
            json!({"_type": "equalToJson", "expectedValue": value})
        }
    }
}

#[distributed_slice(EMBEDDED_DATA_PROTOCOLS)]
static SERVICE_STORE_DATA_PROTOCOL: &(dyn EmbeddedDataProtocol + Send + Sync) =
    &ServiceStoreDataProtocol;
