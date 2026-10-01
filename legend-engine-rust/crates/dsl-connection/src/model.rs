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

//! The engine core's model connections (`ModelConnectionParserGrammar` and
//! its `ModelConnectionParseTreeWalker`): `JsonModelConnection` and
//! `XmlModelConnection`, a class's instances read from a URL, and
//! `ModelChainConnection`, mappings chained.
//!
//! ```text
//! value:  ( class: my::Class; | url: 'url'; | mappings: [ my::Mapping, … ]; )*
//! ```
//!
//! The grammar is the same for the three types: each type's walker checks
//! its own fields, each required once, and ignores the others. A packageable
//! model connection's store is the model store, `ModelStore`; a runtime's
//! own names none.

use std::any::Any;

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::required;
use legend_pure_parser_parser::words::{
    QualifiedName, is_string, is_word, qualified_name, unexpected,
};
use legend_pure_parser_parser::{ParserContext, string_value};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

use crate::ast::{ConnectionValue, ConnectionValueBody};
use crate::grammar::{ConnectionValueGrammar, ValueSpan};

/// The store a packageable model connection connects to.
const MODEL_STORE: &str = "ModelStore";

/// `JsonModelConnection`'s and `XmlModelConnection`'s value: a class, and
/// the URL of its instances.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ClassConnection {
    /// The class's path, unquoted.
    pub class: String,
    /// The class's path.
    pub class_source_info: SourceInfo,
    /// The URL, unescaped.
    pub url: String,
}

/// `ModelChainConnection`'s value: the mappings chained.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ModelChain {
    /// The mappings' paths, unquoted.
    pub mappings: Vec<String>,
    /// From `mappings` to `;`.
    pub mappings_source_info: SourceInfo,
}

impl ConnectionValueBody for ClassConnection {
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

impl ConnectionValueBody for ModelChain {
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

/// `JsonModelConnection`: instances of a class, read as JSON.
pub struct JsonModelConnectionGrammar;

/// `XmlModelConnection`: instances of a class, read as XML.
pub struct XmlModelConnectionGrammar;

/// `ModelChainConnection`: mappings chained.
pub struct ModelChainConnectionGrammar;

impl ConnectionValueGrammar for JsonModelConnectionGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "JsonModelConnection"
    }

    fn parse(
        &self,
        span: &ValueSpan,
        ctx: &mut ParserContext<'_>,
    ) -> Result<Box<dyn ConnectionValueBody>, ParseError> {
        Ok(Box::new(class_connection(read_fields(ctx)?, span)?))
    }

    fn convert(&self, value: &ConnectionValue) -> Result<Value, ConvertError> {
        convert_class_connection(self.type_name(), value)
    }
}

impl ConnectionValueGrammar for XmlModelConnectionGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "XmlModelConnection"
    }

    fn parse(
        &self,
        span: &ValueSpan,
        ctx: &mut ParserContext<'_>,
    ) -> Result<Box<dyn ConnectionValueBody>, ParseError> {
        Ok(Box::new(class_connection(read_fields(ctx)?, span)?))
    }

    fn convert(&self, value: &ConnectionValue) -> Result<Value, ConvertError> {
        convert_class_connection(self.type_name(), value)
    }
}

impl ConnectionValueGrammar for ModelChainConnectionGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "ModelChainConnection"
    }

    fn parse(
        &self,
        span: &ValueSpan,
        ctx: &mut ParserContext<'_>,
    ) -> Result<Box<dyn ConnectionValueBody>, ParseError> {
        let fields = read_fields(ctx)?;
        let (mappings, mappings_source_info) = required(fields.mappings, "mappings", &span.value)?;
        Ok(Box::new(ModelChain {
            mappings: mappings.into_iter().map(|mapping| mapping.path).collect(),
            mappings_source_info,
        }))
    }

    fn convert(&self, value: &ConnectionValue) -> Result<Value, ConvertError> {
        let chain = body::<ModelChain>(value)?;
        let mut json = json!({
            "_type": self.type_name(),
            "mappings": chain.mappings,
            "mappingsSourceInformation": SourceInformation::from(&chain.mappings_source_info),
            "sourceInformation": SourceInformation::from(&value.source_info),
        });
        if !value.embedded {
            json["element"] = json!(MODEL_STORE);
        }
        Ok(json)
    }
}

/// The fields as the grammar reads them, for the walker to check.
#[derive(Default)]
struct Fields {
    classes: Vec<QualifiedName>,
    urls: Vec<String>,
    mappings: Vec<(Vec<QualifiedName>, SourceInfo)>,
}

/// `class: my::Class;`, `url: 'url';` and `mappings: [ … ];`, in any order.
fn read_fields(ctx: &mut ParserContext<'_>) -> Result<Fields, ParseError> {
    let mut fields = Fields::default();
    loop {
        let start = ctx.cursor().current_source_info();
        let field = ctx.cursor().peek().clone();
        let known = ["class", "url", "mappings"]
            .iter()
            .any(|name| is_word(&field, name));
        if !known {
            return Ok(fields);
        }
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::Colon)?;
        match field.text.as_str() {
            "class" => fields.classes.push(qualified_name(ctx)?),
            "url" => {
                let url = ctx.cursor().peek().clone();
                if !is_string(&url) {
                    return Err(unexpected(&url));
                }
                ctx.cursor().advance();
                let (url, _) =
                    string_value(&url.text).map_err(|message| ParseError::Unexpected {
                        message,
                        source_info: url.source_info.clone(),
                    })?;
                fields.urls.push(url);
            }
            _ => {
                ctx.cursor().expect(TokenKind::LBracket)?;
                let mut mappings = Vec::new();
                if !ctx.cursor().check(TokenKind::RBracket) {
                    mappings.push(qualified_name(ctx)?);
                    while ctx.cursor().eat(TokenKind::Comma) {
                        mappings.push(qualified_name(ctx)?);
                    }
                }
                ctx.cursor().expect(TokenKind::RBracket)?;
                ctx.cursor().expect(TokenKind::Semicolon)?;
                fields
                    .mappings
                    .push((mappings, ctx.cursor().span_since(&start)));
                continue;
            }
        }
        ctx.cursor().expect(TokenKind::Semicolon)?;
    }
}

/// The walker's checks for a class and a URL, in that order.
fn class_connection(fields: Fields, span: &ValueSpan) -> Result<ClassConnection, ParseError> {
    let class = required(fields.classes, "class", &span.value)?;
    let url = required(fields.urls, "url", &span.value)?;
    Ok(ClassConnection {
        class: class.path,
        class_source_info: class.source_info,
        url,
    })
}

fn convert_class_connection(
    type_name: &str,
    value: &ConnectionValue,
) -> Result<Value, ConvertError> {
    let connection = body::<ClassConnection>(value)?;
    let mut json = json!({
        "_type": type_name,
        "class": connection.class,
        "classSourceInformation": SourceInformation::from(&connection.class_source_info),
        "url": connection.url,
        "sourceInformation": SourceInformation::from(&value.source_info),
    });
    if !value.embedded {
        json["element"] = json!(MODEL_STORE);
    }
    Ok(json)
}

/// The value's body, as the grammar that converts it read it.
fn body<T: 'static>(value: &ConnectionValue) -> Result<&T, ConvertError> {
    value.body.as_any().downcast_ref::<T>().ok_or_else(|| {
        ConvertError::plugin(
            format!(
                "the body of a '{}' connection is not the model connection grammar's",
                value.type_name.text
            ),
            Some(&value.source_info),
        )
    })
}
