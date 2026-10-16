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

//! The walk (the engine's `ElasticsearchStoreParseTreeWalker`): each
//! store's indices and properties checked and built in the walker's order,
//! into records shaped as the engine's protocol.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::blocks::{ReadBlock, ReadField, ReadValue};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::record::{Record, Value};
use serde_json::{Map, Value as Json};

type R<T> = Result<T, ParseError>;

/// A value the grammar can't have read where it stands: a bug.
fn misread(what: &str, at: &SourceInfo) -> ParseError {
    ParseError::Unexpected {
        message: format!("the Elasticsearch grammar misread {what}"),
        source_info: at.clone(),
    }
}

/// The walker's `visitV7Store`: the store's own record, without its
/// package and name.
pub(crate) fn store(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let indices = read
        .take("'indices'", &at)?
        .values()?
        .into_iter()
        .map(|index| self::index(index, &at))
        .collect::<R<Vec<_>>>()?;
    Ok(Record::new("elasticsearch7Store")
        .at(&at)
        .with("indices", indices))
}

/// A named block's name, block and position.
fn named(value: ReadValue, at: &SourceInfo) -> R<(String, ReadValue, SourceInfo)> {
    match value {
        ReadValue::Named(name, value, source_info) => {
            Ok((name.value.to_string(), *value, source_info))
        }
        _ => Err(misread("a name", at)),
    }
}

/// The walker's `visitIndex`: its properties, checked on the index.
fn index(value: ReadValue, at: &SourceInfo) -> R<Record> {
    let (name, value, index_at) = named(value, at)?;
    let ReadValue::Block(mut read) = value else {
        return Err(misread("an index", &index_at));
    };
    let properties = read
        .take("'properties'", &index_at)?
        .values()?
        .into_iter()
        .map(|property| {
            let (name, property) = self::property(property, &index_at)?;
            Ok(Record::untyped()
                .with("propertyName", name)
                .with("property", Value::Json(property)))
        })
        .collect::<R<Vec<_>>>()?;
    Ok(Record::untyped()
        .with("indexName", name)
        .with("properties", properties))
}

/// A property's JSON names for its type: its field in `Property`, its
/// `_pure_protocol_type` and its `type`.
fn type_names(type_name: &str) -> Option<(&'static str, &'static str, &'static str)> {
    Some(match type_name {
        "Keyword" => ("keyword", "keywordProperty", "keyword"),
        "Text" => ("text", "textProperty", "text"),
        "Date" => ("date", "dateProperty", "date"),
        "Short" => ("_short", "shortNumberProperty", "short"),
        "Byte" => ("_byte", "byteNumberProperty", "byte"),
        "Integer" => ("integer", "integerNumberProperty", "integer"),
        "Long" => ("_long", "longNumberProperty", "long"),
        "Float" => ("_float", "floatNumberProperty", "float"),
        "HalfFloat" => ("half_float", "halfFloatNumberProperty", "half_float"),
        "Double" => ("_double", "doubleNumberProperty", "double"),
        "Boolean" => ("_boolean", "booleanProperty", "boolean"),
        "Object" => ("object", "objectProperty", "object"),
        "Nested" => ("nested", "nestedProperty", "nested"),
        _ => return None,
    })
}

/// The walker's `visitNamedPropertyDefinition`: the name, and the
/// `Property` holding its type's object, with the properties or fields its
/// content names (a scalar's, only when it has a content).
fn property(value: ReadValue, at: &SourceInfo) -> R<(String, Json)> {
    let (name, value, property_at) = named(value, at)?;
    let ReadValue::Block(mut read) = value else {
        return Err(misread("a property", &property_at));
    };
    let type_name = read.keyword.text.to_string();
    let (field, protocol_type, json_type) =
        type_names(&type_name).ok_or_else(|| misread("a property type", &property_at))?;
    let mut object = Map::new();
    object.insert("_pure_protocol_type".to_string(), Json::from(protocol_type));
    object.insert("type".to_string(), Json::from(json_type));
    if let Some(content) = read.take_optional("content", &property_at)? {
        let content_at = content.source_info.clone();
        let mut content = content.block()?;
        let complex = matches!(type_name.as_str(), "Object" | "Nested");
        let (name, inner) = if complex {
            (
                "properties",
                content.take("'properties'", &content_at).map(Some)?,
            )
        } else {
            ("fields", content.take_optional("'fields'", &content_at)?)
        };
        let map = match inner {
            Some(inner) => map(inner, &content_at)?,
            None => Map::new(),
        };
        object.insert(name.to_string(), Json::Object(map));
    }
    let mut property = Map::new();
    property.insert(field.to_string(), Json::Object(object));
    Ok((name, Json::Object(property)))
}

/// Properties by name, as the walker's `Collectors.toMap` makes them: a
/// name written twice makes the engine fail without a message.
fn map(field: ReadField, at: &SourceInfo) -> R<Map<String, Json>> {
    let mut map = Map::new();
    for value in field.values()? {
        let (name, property) = property(value, at)?;
        if map.contains_key(&name) {
            return Err(ParseError::Unsupported {
                message: format!(
                    "the engine fails on the property '{name}' written twice in one mapping"
                ),
                source_info: at.clone(),
            });
        }
        map.insert(name, property);
    }
    Ok(map)
}
