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

//! Specifications whose body is fields, `name: value;` in any order, each
//! checked by the walker once the body is read, in the order the grammar
//! lists them, where the specification is: most authentication strategies,
//! and database extensions' specifications (Snowflake's). The JSON has each
//! field written, under its name in the engine's protocol, and the
//! specification's position.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::{optional, required};
use legend_pure_parser_parser::words::{is_string, is_word_kind, unexpected};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde::ser::{Serialize, SerializeMap, Serializer};
use serde_json::Value;

use super::specification::{
    BodyUse, Specification, SpecificationBody, SpecificationGrammar, SpecificationKind,
    SpecificationSpan, downcast,
};
use super::values::{at_field, boolean, field, string, strings};

type R<T> = Result<T, ParseError>;

/// A specification's grammar of fields.
#[derive(Debug)]
pub struct FieldGrammar {
    /// What it specifies.
    pub kind: SpecificationKind,
    /// The type it reads.
    pub type_name: &'static str,
    /// Its `_type` in the engine's protocol.
    pub json_type: &'static str,
    /// Whether it reads a body.
    pub body_use: BodyUse,
    /// Its fields, in the order the walker checks them.
    pub fields: &'static [Field],
}

/// A field: `name: value;`.
#[derive(Debug, Clone, Copy)]
pub struct Field {
    /// Its name in the grammar.
    pub name: &'static str,
    /// Its name in the engine's protocol.
    pub json_name: &'static str,
    /// How the walker checks it.
    pub check: Check,
    /// Its value.
    pub value: ValueKind,
}

impl Field {
    /// A string field, named the same in the grammar and the protocol.
    #[must_use]
    pub const fn string(name: &'static str, check: Check) -> Self {
        Self {
            name,
            json_name: name,
            check,
            value: ValueKind::String,
        }
    }

    /// The field, named `json_name` in the protocol.
    #[must_use]
    pub const fn in_protocol(self, json_name: &'static str) -> Self {
        Self { json_name, ..self }
    }

    /// The field, its value of `value`.
    #[must_use]
    pub const fn of(self, value: ValueKind) -> Self {
        Self { value, ..self }
    }
}

/// How the walker checks a field.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Check {
    /// Written at most once.
    Optional,
    /// Written once.
    Required,
    /// Written once, checked before the walker knows where the
    /// specification is: the engine reports it without a position, or fails
    /// outside its parser when it's missing (unsupported).
    Unpositioned,
}

/// A field's value.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ValueKind {
    /// `'text'`, unescaped.
    String,
    /// `[ 'a', … ]`, unescaped.
    Strings,
    /// `true` or `false`.
    Boolean,
    /// A word, as written.
    Word,
}

/// A specification's fields, as the walker keeps them.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct FieldValues(Vec<(&'static str, FieldValue)>);

#[derive(Debug, Clone, PartialEq, Eq, serde::Serialize)]
#[serde(untagged)]
enum FieldValue {
    Text(String),
    Texts(Vec<String>),
    Boolean(bool),
}

body!(FieldValues);

impl SpecificationGrammar for FieldGrammar {
    fn kind(&self) -> SpecificationKind {
        self.kind
    }

    fn type_name(&self) -> &str {
        self.type_name
    }

    fn body_use(&self) -> BodyUse {
        self.body_use
    }

    fn parse(
        &self,
        span: &SpecificationSpan,
        body: Option<&mut ParserContext<'_>>,
    ) -> R<Box<dyn SpecificationBody>> {
        let Some(ctx) = body else {
            return Ok(Box::new(FieldValues(Vec::new())));
        };
        let mut written: Vec<Vec<FieldValue>> = vec![Vec::new(); self.fields.len()];
        'fields: loop {
            for (index, spec) in self.fields.iter().enumerate() {
                if at_field(ctx, spec.name) {
                    let value = match spec.value {
                        ValueKind::String => FieldValue::Text(field(ctx, string)?.0),
                        ValueKind::Strings => FieldValue::Texts(field(ctx, strings)?.0),
                        ValueKind::Boolean => FieldValue::Boolean(field(ctx, boolean)?.0),
                        ValueKind::Word => FieldValue::Text(field(ctx, word)?.0),
                    };
                    written[index].push(value);
                    continue 'fields;
                }
            }
            break;
        }
        let mut fields = Vec::new();
        for (spec, values) in self.fields.iter().zip(written) {
            let value = match spec.check {
                Check::Optional => optional(values, spec.name, &span.source_info)?,
                Check::Required => Some(required(values, spec.name, &span.source_info)?),
                Check::Unpositioned => Some(unpositioned(values, spec.name, &span.source_info)?),
            };
            if let Some(value) = value {
                fields.push((spec.json_name, value));
            }
        }
        Ok(Box::new(FieldValues(fields)))
    }

    fn convert(&self, specification: &Specification) -> Result<Value, ConvertError> {
        let FieldValues(fields) = downcast::<FieldValues>(specification)?;
        Ok(serde_json::to_value(FieldSpecificationJson {
            json_type: self.json_type,
            fields,
            source_information: SourceInformation::from(&specification.source_info),
        })?)
    }
}

/// A specification this grammar read, as the engine's JSON of it: its
/// `_type`, its fields (each name once), its `sourceInformation`.
struct FieldSpecificationJson<'a> {
    json_type: &'static str,
    fields: &'a [(&'static str, FieldValue)],
    source_information: SourceInformation,
}

impl Serialize for FieldSpecificationJson<'_> {
    fn serialize<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        let mut map = serializer.serialize_map(Some(self.fields.len() + 2))?;
        map.serialize_entry("_type", self.json_type)?;
        for (name, value) in self.fields {
            map.serialize_entry(name, value)?;
        }
        map.serialize_entry("sourceInformation", &self.source_information)?;
        map.end()
    }
}

/// A word, as written: not a quoted string.
fn word(ctx: &mut ParserContext<'_>) -> R<String> {
    let token = ctx.cursor().peek().clone();
    if !is_word_kind(token.kind) || is_string(&token) {
        return Err(unexpected(&token));
    }
    ctx.cursor().advance();
    Ok(token.text.to_string())
}

/// A field the walker checks before it knows where the specification is:
/// its error has no position, and a missing field fails it outside its
/// parser.
fn unpositioned<T>(mut values: Vec<T>, field: &str, source_info: &SourceInfo) -> R<T> {
    if values.len() == 1 {
        return Ok(values.remove(0));
    }
    Err(ParseError::Unsupported {
        message: format!(
            "'{field}' is missing or written twice, which the engine reports without a position"
        ),
        source_info: source_info.clone(),
    })
}
