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

//! Protocol objects as a walker builds them: records in the shape of the
//! engine's protocol objects (a `_type`, fields in order, a position),
//! holding the parts other grammars read (navigation paths, primitive
//! values, embedded data, test assertions) as those grammars' trees,
//! converted with them, or as JSON already converted.
//!
//! Some of the engine's section protocols have dozens of classes, each built
//! by one walker method from the fields its grammar rule reads (the
//! persistence extension's, the data space extension's, …): a record per
//! object keeps their walkers as plain as the engine's.

use legend_pure_parser_ast::data::{EmbeddedDataValue, RelationElement};
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::test_assertion::TestAssertion;

use crate::element_protocol::{ConvertError, Result};
use crate::v1::annotation;
use crate::v1::convert::{
    convert_embedded_data, convert_expression_typed, convert_primitive_value,
    convert_relation_element, convert_test_assertion,
};
use crate::v1::data::{EmbeddedData, RelationElement as RelationElementJson};
use crate::v1::source_info::SourceInformation;
use crate::v1::test_assertion::TestAssertion as TestAssertionJson;
use crate::v1::value_spec::{LambdaFunction, ValueSpecification};
use serde::ser::{Serialize, SerializeMap, Serializer};
use serde_json::{Number, Value as Json};
use smol_str::SmolStr;

/// A protocol object: its `_type`, its fields, its position.
#[derive(Debug, Clone, PartialEq)]
pub struct Record {
    /// The `_type`, which an object of a class without subclasses lacks.
    pub kind: Option<&'static str>,
    /// The fields, in order; a field without a value is left out.
    pub fields: Vec<(&'static str, Value)>,
    /// Where the walker positions the object, if it does.
    pub source_info: Option<SourceInfo>,
}

/// A field's value.
#[derive(Debug, Clone, PartialEq)]
pub enum Value {
    /// A string.
    Text(String),
    /// Strings.
    Texts(Vec<String>),
    /// A boolean.
    Bool(bool),
    /// A number.
    Integer(i64),
    /// A Java `double`, written as the engine writes one
    /// ([`java_double`]).
    Double(f64),
    /// An object.
    Record(Box<Record>),
    /// Objects.
    Records(Vec<Record>),
    /// A navigation path, `#/Type/property#`: the engine's `Path`.
    Path(Box<Expression>),
    /// Navigation paths.
    Paths(Vec<Expression>),
    /// A primitive value, positioned by a walker of its own.
    Primitive(Box<Expression>),
    /// Embedded data.
    EmbeddedData(Box<EmbeddedDataValue>),
    /// Test assertions, each with its id.
    Assertions(Vec<(SmolStr, TestAssertion)>),
    /// A lambda, or, as the walkers make it of an expression that isn't one,
    /// the body of an unpositioned lambda without parameters.
    Lambda(Box<Expression>),
    /// A relation's first table.
    RelationElement(Box<RelationElement>),
    /// A position, as a field other than the object's own
    /// `sourceInformation` holds one (`styleSourceInformation`, …).
    SourceInfo(SourceInfo),
    /// JSON already converted: a part a plug-in's protocol converts.
    Json(Json),
}

impl Record {
    /// An object of `kind`, without fields or position yet.
    #[must_use]
    pub fn new(kind: &'static str) -> Self {
        Self {
            kind: Some(kind),
            fields: Vec::new(),
            source_info: None,
        }
    }

    /// An object of a class without subclasses, which has no `_type`.
    #[must_use]
    pub fn untyped() -> Self {
        Self {
            kind: None,
            fields: Vec::new(),
            source_info: None,
        }
    }

    /// The object, positioned.
    #[must_use]
    pub fn at(mut self, source_info: &SourceInfo) -> Self {
        self.source_info = Some(source_info.clone());
        self
    }

    /// The object with `name` set to `value`.
    #[must_use]
    pub fn with(mut self, name: &'static str, value: impl Into<Value>) -> Self {
        self.fields.push((name, value.into()));
        self
    }

    /// The object with `name` set to `value`, if any.
    #[must_use]
    pub fn with_some(self, name: &'static str, value: Option<impl Into<Value>>) -> Self {
        match value {
            Some(value) => self.with(name, value),
            None => self,
        }
    }

    /// The object's JSON, what other grammars read in it converted with
    /// them.
    ///
    /// # Errors
    ///
    /// What other grammars read in it fails to convert.
    pub fn to_json(&self) -> Result<Json> {
        Ok(serde_json::to_value(self.converted()?)?)
    }

    /// The object, what other grammars read in it converted with them,
    /// ready to serialize as the engine's protocol object.
    ///
    /// # Errors
    ///
    /// What other grammars read in it fails to convert.
    pub fn converted(&self) -> Result<RecordJson<'_>> {
        let mut json = RecordJson {
            fields: Vec::with_capacity(self.fields.len() + 2),
        };
        if let Some(kind) = self.kind {
            json = json.with("_type", kind);
        }
        for (name, value) in &self.fields {
            json = json.with(name, value.converted()?);
        }
        if let Some(source_info) = &self.source_info {
            json = json.with("sourceInformation", SourceInformation::from(source_info));
        }
        Ok(json)
    }
}

/// A [`Record`] converted: the engine's protocol object, its fields in the
/// order they were first set, a field set again replaced in place.
#[derive(Debug)]
pub struct RecordJson<'a> {
    fields: Vec<(&'static str, FieldJson<'a>)>,
}

impl<'a> RecordJson<'a> {
    /// The object with `name` set to `value`.
    #[must_use]
    pub fn with(mut self, name: &'static str, value: impl Into<FieldJson<'a>>) -> Self {
        let value = value.into();
        match self.fields.iter_mut().find(|(field, _)| *field == name) {
            Some((_, field)) => *field = value,
            None => self.fields.push((name, value)),
        }
        self
    }
}

impl Serialize for RecordJson<'_> {
    fn serialize<S: Serializer>(&self, serializer: S) -> std::result::Result<S::Ok, S::Error> {
        let mut map = serializer.serialize_map(Some(self.fields.len()))?;
        for (name, value) in &self.fields {
            map.serialize_entry(name, value)?;
        }
        map.end()
    }
}

/// A [`RecordJson`]'s field: a [`Value`] converted, or a part of the object
/// its element's converter adds (its package, name and annotations).
#[derive(Debug, serde::Serialize)]
#[serde(untagged)]
pub enum FieldJson<'a> {
    /// A string.
    Text(&'a str),
    /// A string made for the object.
    OwnedText(String),
    /// Strings.
    Texts(&'a [String]),
    /// A boolean.
    Bool(bool),
    /// A number.
    Integer(i64),
    /// A number as written (a Java `double`, [`java_double`]).
    Number(Number),
    /// An object.
    Record(RecordJson<'a>),
    /// Objects.
    Records(Vec<RecordJson<'a>>),
    /// A value specification: a primitive value, or a lambda.
    Specification(ValueSpecification),
    /// Embedded data.
    EmbeddedData(EmbeddedData),
    /// Test assertions.
    Assertions(Vec<TestAssertionJson>),
    /// A relation's first table.
    RelationElement(RelationElementJson),
    /// A position.
    SourceInformation(SourceInformation),
    /// Stereotypes.
    Stereotypes(Vec<annotation::StereotypePtr>),
    /// Tagged values.
    TaggedValues(Vec<annotation::TaggedValue>),
    /// JSON a plug-in's protocol converted.
    Json(&'a Json),
    /// JSON converted for the object: a navigation path's.
    OwnedJson(Json),
    /// JSON converted for the object: navigation paths'.
    OwnedJsons(Vec<Json>),
}

impl<'a> From<&'a str> for FieldJson<'a> {
    fn from(text: &'a str) -> Self {
        Self::Text(text)
    }
}

impl From<String> for FieldJson<'_> {
    fn from(text: String) -> Self {
        Self::OwnedText(text)
    }
}

impl From<SourceInformation> for FieldJson<'_> {
    fn from(source_information: SourceInformation) -> Self {
        Self::SourceInformation(source_information)
    }
}

impl From<Vec<annotation::StereotypePtr>> for FieldJson<'_> {
    fn from(stereotypes: Vec<annotation::StereotypePtr>) -> Self {
        Self::Stereotypes(stereotypes)
    }
}

impl From<Vec<annotation::TaggedValue>> for FieldJson<'_> {
    fn from(tagged_values: Vec<annotation::TaggedValue>) -> Self {
        Self::TaggedValues(tagged_values)
    }
}

impl Value {
    fn converted(&self) -> Result<FieldJson<'_>> {
        Ok(match self {
            Self::Text(text) => FieldJson::Text(text),
            Self::Texts(texts) => FieldJson::Texts(texts),
            Self::Bool(value) => FieldJson::Bool(*value),
            Self::Integer(value) => FieldJson::Integer(*value),
            Self::Double(value) => FieldJson::Number(java_double(*value)?),
            Self::Record(record) => FieldJson::Record(record.converted()?),
            Self::Records(records) => FieldJson::Records(
                records
                    .iter()
                    .map(Record::converted)
                    .collect::<Result<_>>()?,
            ),
            Self::Path(path) => FieldJson::OwnedJson(path_json(path)?),
            Self::Paths(paths) => {
                FieldJson::OwnedJsons(paths.iter().map(path_json).collect::<Result<_>>()?)
            }
            Self::Primitive(value) => FieldJson::Specification(convert_primitive_value(value)?),
            Self::EmbeddedData(data) => FieldJson::EmbeddedData(convert_embedded_data(data)?),
            Self::Assertions(assertions) => FieldJson::Assertions(
                assertions
                    .iter()
                    .map(|(id, assertion)| convert_test_assertion(id, assertion))
                    .collect::<Result<_>>()?,
            ),
            Self::Lambda(expression) => {
                FieldJson::Specification(match convert_expression_typed(expression)? {
                    lambda @ ValueSpecification::Lambda(_) => lambda,
                    body => ValueSpecification::Lambda(LambdaFunction {
                        body: vec![body],
                        parameters: Vec::new(),
                        source_information: None,
                    }),
                })
            }
            Self::RelationElement(element) => {
                FieldJson::RelationElement(convert_relation_element(element))
            }
            Self::SourceInfo(source_info) => {
                FieldJson::SourceInformation(SourceInformation::from(source_info))
            }
            Self::Json(json) => FieldJson::Json(json),
        })
    }
}

/// A `double` as the engine's JSON writes it, Java's `Double.toString`:
/// its shortest digits (ties to even, as Java breaks them), in decimal
/// notation from 10⁻³ to 10⁷ (`23.0`, `0.001`) and in scientific notation
/// otherwise (`2.23E10`).
///
/// # Errors
///
/// Not a finite number, which has no JSON form.
pub fn java_double(value: f64) -> Result<Number> {
    let finite = |value: f64| {
        serde_json::Number::from_f64(value)
            .ok_or_else(|| ConvertError::plugin(format!("{value} has no JSON form"), None))
    };
    let sign = if value.is_sign_negative() { "-" } else { "" };
    if value == 0.0 {
        return Ok(serde_json::from_str(&format!("{sign}0.0"))?);
    }
    // The shortest digits, from the JSON writer's own (Ryu's), and the
    // exponent of the first.
    let shortest = finite(value.abs())?.to_string();
    let (mantissa, scale) = match shortest.split_once(['e', 'E']) {
        Some((mantissa, scale)) => (mantissa, scale.parse::<i32>().unwrap_or_default()),
        None => (shortest.as_str(), 0),
    };
    let (integer, fraction) = mantissa.split_once('.').unwrap_or((mantissa, ""));
    let all = format!("{integer}{fraction}");
    let leading = all.len() - all.trim_start_matches('0').len();
    let digits = all.trim_start_matches('0').trim_end_matches('0');
    let exponent = scale + i32::try_from(integer.len()).unwrap_or_default()
        - 1
        - i32::try_from(leading).unwrap_or_default();
    let text = if (-3..7).contains(&exponent) {
        if exponent < 0 {
            let zeros = "0".repeat(usize::try_from(-exponent - 1).unwrap_or_default());
            format!("{sign}0.{zeros}{digits}")
        } else {
            let integer_length = usize::try_from(exponent + 1).unwrap_or_default();
            if digits.len() > integer_length {
                let (integer, fraction) = digits.split_at(integer_length);
                format!("{sign}{integer}.{fraction}")
            } else {
                let zeros = "0".repeat(integer_length - digits.len());
                format!("{sign}{digits}{zeros}.0")
            }
        }
    } else {
        let (first, rest) = digits.split_at(1);
        let rest = if rest.is_empty() { "0" } else { rest };
        format!("{sign}{first}.{rest}E{exponent}")
    };
    Ok(serde_json::from_str(&text)?)
}

/// A navigation path's `Path`: the value of the class instance the engine
/// makes of `#/Type/property#`.
fn path_json(path: &Expression) -> Result<Json> {
    match convert_expression_typed(path)? {
        ValueSpecification::ClassInstance(instance) if instance.type_name == "path" => {
            Ok(instance.value)
        }
        _ => Err(ConvertError::unsupported(
            "a persistence path must be a navigation path",
            path_span(path),
        )),
    }
}

fn path_span(path: &Expression) -> &SourceInfo {
    use legend_pure_parser_ast::source_info::Spanned;
    path.source_info()
}

impl From<String> for Value {
    fn from(text: String) -> Self {
        Self::Text(text)
    }
}

impl From<Vec<String>> for Value {
    fn from(texts: Vec<String>) -> Self {
        Self::Texts(texts)
    }
}

impl From<bool> for Value {
    fn from(value: bool) -> Self {
        Self::Bool(value)
    }
}

impl From<Record> for Value {
    fn from(record: Record) -> Self {
        Self::Record(Box::new(record))
    }
}

impl From<Vec<Record>> for Value {
    fn from(records: Vec<Record>) -> Self {
        Self::Records(records)
    }
}

#[cfg(test)]
mod tests {
    use super::java_double;

    #[test]
    fn a_double_is_written_as_java_writes_it() {
        let text = |value: f64| {
            java_double(value)
                .map(|json| json.to_string())
                .unwrap_or_default()
        };
        // Decimal notation from 10⁻³ to 10⁷, with a fraction.
        assert_eq!(text(23.0), "23.0");
        assert_eq!(text(-90.0), "-90.0");
        assert_eq!(text(123.123), "123.123");
        assert_eq!(text(0.3), "0.3");
        assert_eq!(text(0.001), "0.001");
        assert_eq!(text(9_999_999.0), "9999999.0");
        assert_eq!(text(0.0), "0.0");
        // Halfway between two shortest texts, the even one, as Java writes.
        // 602.27081298828125 exactly.
        assert_eq!(text(602.270_812_988_281_2), "602.2708129882812");
        // Scientific notation otherwise, as the JSON reader reads it back.
        assert_eq!(text(2.23e10), serde_json_text("2.23E10"));
        assert_eq!(text(2.23e-8), serde_json_text("2.23E-8"));
        assert_eq!(text(1e7), serde_json_text("1.0E7"));
    }

    fn serde_json_text(text: &str) -> String {
        serde_json::from_str::<serde_json::Value>(text)
            .map(|json| json.to_string())
            .unwrap_or_default()
    }
}
