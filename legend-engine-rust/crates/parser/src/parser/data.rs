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

//! Embedded data, `Type #{ … }#`, as the engine's
//! `HelperEmbeddedDataGrammarParser` reads it: the engine core's types
//! (`ExternalFormat`, `ModelStore` and `Reference` here, `Relation` in
//! `relation_data`), or a linked plug-in's.

use std::collections::HashSet;

use super::block::TypedIsland;
use super::expression::parse_island_body;
use super::relation_data::{self, Origin};
use super::{Parser, ParserContext, R};
use crate::embedded_data::{self, EXTERNAL_FORMAT, MODEL_STORE, REFERENCE};
use crate::error::ParseError;
use legend_pure_parser_ast::SourceInfo;
use legend_pure_parser_ast::annotation::TaggedValue;
use legend_pure_parser_ast::data::{
    DataElementReference, EmbeddedData, EmbeddedDataValue, ExternalFormatData, ModelInstance,
    ModelPropertyValue, ModelStoreData, ModelTestData, ModelTestDataValue, ModelValue,
};
use legend_pure_parser_ast::expression::{DecimalLiteral, FloatLiteral, IntegerLiteral, Literal};
use legend_pure_parser_ast::source_info::Spanned;
use legend_pure_parser_lexer::{Token, TokenKind};

impl Parser {
    /// Whether `Type #{` (embedded data) is next.
    pub(crate) fn at_embedded_data(&self) -> bool {
        self.cursor.peek_kind() == TokenKind::Identifier
            && self.cursor.peek_kind_at(1) == TokenKind::IslandOpen
    }

    /// Embedded data, read by its type's grammar: an engine core type by the
    /// parser itself, another type by its linked grammar. A type without one
    /// is unsupported: the engine decides.
    pub(crate) fn parse_embedded_data(&mut self) -> R<EmbeddedDataValue> {
        let island = self.read_typed_island()?;
        self.parse_embedded_data_island(&island)
    }

    /// [`parse_embedded_data`](Self::parse_embedded_data), from a typed
    /// island already read.
    pub(crate) fn parse_embedded_data_island(
        &mut self,
        island: &TypedIsland,
    ) -> R<EmbeddedDataValue> {
        let TypedIsland {
            type_name,
            body,
            source_info,
            ..
        } = island.clone();
        let plug_in = embedded_data::discovered_embedded_data_parsers()
            .iter()
            .find(|p| p.type_name() == type_name);
        if !embedded_data::CORE_TYPES.contains(&type_name.as_str()) && plug_in.is_none() {
            return Err(ParseError::Unsupported {
                message: format!(
                    "no embedded data grammar is linked for '{type_name}'; the linked ones are: {}",
                    embedded_data::linked_types().join(", ")
                ),
                source_info,
            });
        }
        if body.text.is_empty() {
            return Err(ParseError::unexpected(
                "Embedded data must not be empty",
                source_info,
            ));
        }
        let span = source_info.clone();
        let data = if let Some(grammar) = plug_in {
            parse_island_body(&body, span, grammar.raw_body(), |ctx| grammar.parse(ctx))?
        } else {
            match type_name.as_str() {
                EXTERNAL_FORMAT => {
                    EmbeddedData::ExternalFormat(parse_island_body(&body, span, false, |ctx| {
                        external_format(ctx, &source_info)
                    })?)
                }
                MODEL_STORE => {
                    EmbeddedData::ModelStore(parse_island_body(&body, span, false, |ctx| {
                        ctx.parser.parse_model_store_data()
                    })?)
                }
                REFERENCE => EmbeddedData::Reference(DataElementReference {
                    data_element: embedded_data::element_path(
                        &body.text,
                        "DataElementReference",
                        &source_info,
                    )?,
                    element_type: "DATA".into(),
                }),
                // `RELATION`, the other core type.
                _ => EmbeddedData::Relation(relation_data::parse(
                    &body.text,
                    origin(&body),
                    &body.source_info.source,
                )?),
            }
        };
        Ok(EmbeddedDataValue {
            type_name,
            data,
            source_info,
        })
    }

    /// `ModelStore` data: `Type: [instances] | Type: Data #{ … }#`, entries
    /// separated by commas, one per type (the engine's
    /// `ModelStoreDataParseTreeWalker`).
    fn parse_model_store_data(&mut self) -> R<ModelStoreData> {
        let model_data = self.parse_separated(TokenKind::Eof, Self::parse_model_test_data)?;
        let mut types = HashSet::new();
        for entry in &model_data {
            let path = entry.model.to_string();
            if !types.insert(path.clone()) {
                return Err(ParseError::unexpected(
                    format!("Multiple entries found for type: '{path}'"),
                    entry.source_info.clone(),
                ));
            }
        }
        Ok(ModelStoreData { model_data })
    }

    /// One type's entry: its instances, or embedded data.
    fn parse_model_test_data(&mut self) -> R<ModelTestData> {
        let start = self.cursor.current_source_info();
        let model = self.parse_element_pointer()?;
        self.cursor.expect(TokenKind::Colon)?;
        let data = if self.at_embedded_data() {
            ModelTestDataValue::Embedded(self.parse_embedded_data()?)
        } else {
            self.cursor.expect(TokenKind::LBracket)?;
            let instances =
                self.parse_separated(TokenKind::RBracket, Self::parse_model_instance)?;
            self.cursor.expect(TokenKind::RBracket)?;
            ModelTestDataValue::Instances(instances)
        };
        Ok(ModelTestData {
            model,
            data,
            source_info: self.cursor.span_since(&start),
        })
    }

    /// `^Type(property = value, …)`.
    fn parse_model_instance(&mut self) -> R<ModelInstance> {
        let start = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::Caret)?;
        let class = self.parse_element_pointer()?;
        self.cursor.expect(TokenKind::LParen)?;
        let properties = self.parse_separated(TokenKind::RParen, |p| {
            let start = p.cursor.current_source_info();
            let (property, _) = p.cursor.expect_identifier_or_keyword()?;
            p.cursor.expect(TokenKind::Equals)?;
            let values = if p.cursor.eat(TokenKind::LBracket) {
                let values = p.parse_separated(TokenKind::RBracket, Self::parse_model_value)?;
                p.cursor.expect(TokenKind::RBracket)?;
                values
            } else {
                vec![p.parse_model_value()?]
            };
            Ok(ModelPropertyValue {
                property,
                values,
                source_info: p.cursor.span_since(&start),
            })
        })?;
        self.cursor.expect(TokenKind::RParen)?;
        Ok(ModelInstance {
            class,
            properties,
            source_info: self.cursor.span_since(&start),
        })
    }

    /// A literal (a number may be signed), an instance, or an enum value
    /// `my::Enum.VALUE`.
    fn parse_model_value(&mut self) -> R<ModelValue> {
        let start = self.cursor.current_source_info();
        if self.cursor.check(TokenKind::Caret) {
            return Ok(ModelValue::Instance(self.parse_model_instance()?));
        }
        let sign = match self.cursor.peek_kind() {
            TokenKind::Minus | TokenKind::Plus => Some(self.cursor.advance().kind),
            _ => None,
        };
        if let Some(literal) = self.parse_literal()? {
            let negative = sign == Some(TokenKind::Minus);
            let source_info = self.cursor.span_since(&start);
            return match (sign, literal) {
                (_, Literal::Integer(i)) => {
                    Ok(ModelValue::Literal(Literal::Integer(IntegerLiteral {
                        value: if negative { -i.value } else { i.value },
                        source_info,
                    })))
                }
                (_, Literal::Float(f)) => Ok(ModelValue::Literal(Literal::Float(FloatLiteral {
                    value: if negative { -f.value } else { f.value },
                    source_info,
                }))),
                (_, Literal::Decimal(d)) => {
                    Ok(ModelValue::Literal(Literal::Decimal(DecimalLiteral {
                        value: if negative {
                            format!("-{}", d.value)
                        } else {
                            d.value
                        },
                        source_info,
                    })))
                }
                // Only numbers take a sign; the engine's core lexer has no
                // `%latest`.
                (Some(_), other) | (None, other @ Literal::LatestDate(_)) => Err(
                    ParseError::unexpected("Unexpected token", other.source_info().clone()),
                ),
                (None, other) => Ok(ModelValue::Literal(other)),
            };
        }
        if sign.is_some() {
            return Err(ParseError::expected(
                "a number",
                self.cursor.peek_kind(),
                self.cursor.current_source_info(),
            ));
        }
        let enumeration = self.parse_element_pointer()?;
        self.cursor.expect(TokenKind::Dot)?;
        let (value, _) = self.cursor.expect_identifier_or_keyword()?;
        Ok(ModelValue::EnumValue {
            enumeration,
            value,
            source_info: self.cursor.span_since(&start),
        })
    }
}

impl ParserContext<'_> {
    /// Whether embedded data, `Type #{`, is next.
    #[must_use]
    pub fn at_embedded_data(&self) -> bool {
        self.parser.at_embedded_data()
    }

    /// Parses embedded data, `Type #{ … }#`, with its type's grammar: an
    /// engine core type (`ExternalFormat`, `ModelStore`, `Reference`,
    /// `Relation`) or a linked plug-in's, as the engine's
    /// `HelperEmbeddedDataGrammarParser` does.
    ///
    /// # Errors
    ///
    /// A syntax error, or [`ParseError::Unsupported`] for a type no linked
    /// grammar reads.
    pub fn parse_embedded_data(&mut self) -> R<EmbeddedDataValue> {
        self.parser.parse_embedded_data()
    }

    /// [`parse_embedded_data`](Self::parse_embedded_data), from a typed
    /// island read with [`read_typed_island`](Self::read_typed_island).
    ///
    /// # Errors
    ///
    /// As for [`parse_embedded_data`](Self::parse_embedded_data).
    pub fn parse_embedded_data_island(&mut self, island: &TypedIsland) -> R<EmbeddedDataValue> {
        self.parser.parse_embedded_data_island(island)
    }

    /// Folds an element's documentation, the `'''…'''` literal written
    /// before it, into its tagged values, as the engine's
    /// `PureGrammarParserUtility.taggedValuesWithDocumentation` does: a
    /// leading `meta::pure::profiles::doc.doc` tagged value.
    ///
    /// # Errors
    ///
    /// A documentation literal that isn't a text block, or an element that
    /// also has an explicit `doc.doc` tagged value.
    pub fn with_documentation(
        documentation: Option<Token>,
        tagged_values: Vec<TaggedValue>,
    ) -> R<Vec<TaggedValue>> {
        Parser::with_documentation(documentation, tagged_values)
    }
}

/// `ExternalFormat` data: `contentType: '…';` and `data: '…';`, each exactly
/// once, in any order (the engine's `ExternalFormatEmbeddedDataParser`).
/// A missing or repeated field is reported at `span`, the whole embedded
/// data.
fn external_format(ctx: &mut ParserContext<'_>, span: &SourceInfo) -> R<ExternalFormatData> {
    let mut content_types = Vec::new();
    let mut data = Vec::new();
    let cursor = ctx.cursor();
    while !cursor.check(TokenKind::Eof) {
        let field = cursor.peek().clone();
        let values = match field.text.as_str() {
            "contentType" if field.kind == TokenKind::Identifier => &mut content_types,
            "data" if field.kind == TokenKind::Identifier => &mut data,
            _ => {
                return Err(ParseError::unexpected(
                    format!("Unexpected token '{}'", field.text),
                    field.source_info,
                ));
            }
        };
        cursor.advance();
        cursor.expect(TokenKind::Colon)?;
        let value = cursor.expect(TokenKind::StringLiteral)?;
        cursor.expect(TokenKind::Semicolon)?;
        values.push(
            super::strings::string_value(&value.text)
                .map(|(value, _)| value)
                .map_err(|message| ParseError::unexpected(message, value.source_info.clone()))?,
        );
    }
    Ok(ExternalFormatData {
        content_type: exactly_one(content_types, "contentType", span)?,
        data: exactly_one(data, "data", span)?,
    })
}

/// The engine's `validateAndExtractRequiredField`.
fn exactly_one(mut values: Vec<String>, field: &str, span: &SourceInfo) -> R<String> {
    match values.len() {
        0 => Err(ParseError::unexpected(
            format!("Field '{field}' is required"),
            span.clone(),
        )),
        1 => Ok(values.remove(0)),
        _ => Err(ParseError::unexpected(
            format!("Field '{field}' should be specified only once"),
            span.clone(),
        )),
    }
}

/// Where an island's body starts in the document.
pub(super) fn origin(body: &Token) -> Origin {
    Origin {
        line: body.source_info.start_line,
        column: body.source_info.start_column,
    }
}
