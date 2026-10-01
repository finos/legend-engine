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
use crate::v1::convert::{
    convert_embedded_data, convert_expression_typed, convert_primitive_value,
    convert_relation_element, convert_test_assertion,
};
use crate::v1::source_info::SourceInformation;
use crate::v1::value_spec::{LambdaFunction, ValueSpecification};
use serde_json::{Map, Value as Json, json};
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

    /// The engine's JSON of the object.
    ///
    /// # Errors
    ///
    /// A part another grammar read fails to convert.
    pub fn to_json(&self) -> Result<Json> {
        let mut map = Map::new();
        if let Some(kind) = self.kind {
            map.insert("_type".to_string(), json!(kind));
        }
        for (name, value) in &self.fields {
            map.insert((*name).to_string(), value.to_json()?);
        }
        if let Some(source_info) = &self.source_info {
            map.insert(
                "sourceInformation".to_string(),
                json!(SourceInformation::from(source_info)),
            );
        }
        Ok(Json::Object(map))
    }
}

impl Value {
    fn to_json(&self) -> Result<Json> {
        Ok(match self {
            Self::Text(text) => json!(text),
            Self::Texts(texts) => json!(texts),
            Self::Bool(value) => json!(value),
            Self::Integer(value) => json!(value),
            Self::Double(value) => java_double(*value)?,
            Self::Record(record) => record.to_json()?,
            Self::Records(records) => {
                Json::Array(records.iter().map(Record::to_json).collect::<Result<_>>()?)
            }
            Self::Path(path) => path_json(path)?,
            Self::Paths(paths) => Json::Array(paths.iter().map(path_json).collect::<Result<_>>()?),
            Self::Primitive(value) => serde_json::to_value(convert_primitive_value(value)?)?,
            Self::EmbeddedData(data) => serde_json::to_value(convert_embedded_data(data)?)?,
            Self::Assertions(assertions) => Json::Array(
                assertions
                    .iter()
                    .map(|(id, assertion)| {
                        Ok(serde_json::to_value(convert_test_assertion(
                            id, assertion,
                        )?)?)
                    })
                    .collect::<Result<_>>()?,
            ),
            Self::Lambda(expression) => {
                let lambda = match convert_expression_typed(expression)? {
                    lambda @ ValueSpecification::Lambda(_) => lambda,
                    body => ValueSpecification::Lambda(LambdaFunction {
                        body: vec![body],
                        parameters: Vec::new(),
                        source_information: None,
                    }),
                };
                serde_json::to_value(lambda)?
            }
            Self::RelationElement(element) => {
                serde_json::to_value(convert_relation_element(element))?
            }
            Self::SourceInfo(source_info) => json!(SourceInformation::from(source_info)),
            Self::Json(json) => json.clone(),
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
pub fn java_double(value: f64) -> Result<Json> {
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
