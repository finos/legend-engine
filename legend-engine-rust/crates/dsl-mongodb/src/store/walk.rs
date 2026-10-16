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

//! The walk (the engine's `MongoDBSchemaParseTreeWalker`, and its
//! `MongoDBSchemaDeserializer` for each schema): each database's
//! collections built into records shaped as the engine's protocol. The
//! engine writes the flags its classes default to `false` always, as here.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::record::{Record, Value};
use serde_json::Value as JsonValue;

use super::json::Json;
use super::read::{ReadCollection, ReadDatabase};

type R<T> = Result<T, ParseError>;

/// What the engine's deserializer fails on, without a message of its
/// own: Rust reports it unsupported, for the engine to fail on.
fn fails(message: &str, at: &SourceInfo) -> ParseError {
    ParseError::Unsupported {
        message: format!("the engine fails on this schema: {message}"),
        source_info: at.clone(),
    }
}

/// The walker's `visitMongoDBStore`: the database's own record, without
/// its package and name.
pub(crate) fn database(read: &ReadDatabase) -> R<Record> {
    let collections = read
        .collections
        .iter()
        .map(collection)
        .collect::<R<Vec<_>>>()?;
    Ok(Record::new("MongoDatabase")
        .at(&read.source_info)
        .with("collections", collections))
}

/// The walker's `visitCollection`.
fn collection(read: &ReadCollection) -> R<Record> {
    let level = if read.moderate { "moderate" } else { "strict" };
    let action = if read.warn { "warn" } else { "error" };
    let Json::Object(_) = &read.schema else {
        return Err(fails("the schema isn't an object", &read.source_info));
    };
    let schema = object_type(Record::new("schema"), &read.schema, &read.source_info, true)?;
    Ok(Record::untyped().with("name", read.name.clone()).with(
        "validator",
        Record::untyped()
            .with("validationLevel", level.to_string())
            .with("validationAction", action.to_string())
            .with(
                "validatorExpression",
                Record::new("jsonSchemaExpression").with("schemaExpression", schema),
            ),
    ))
}

/// A type's `bsonType`, as the deserializer reads it: missing, the engine
/// fails (`get(…).textValue()` on null).
fn bson_type<'a>(node: &'a Json, at: &SourceInfo) -> R<Option<&'a str>> {
    match node.get("bsonType") {
        Some(value) => Ok(value.text()),
        None => Err(fails("a type without a bsonType", at)),
    }
}

/// `processBaseTypeFields`: a title and a description, if text.
fn base_type(record: Record, node: &Json) -> Record {
    let text = |name: &str| node.get(name).and_then(Json::text).map(str::to_string);
    record
        .with_some("title", text("title"))
        .with_some("description", text("description"))
}

/// `getSchema` (the root, which must be an object) or
/// `processObjectTypeFields`.
fn object_type(record: Record, node: &Json, at: &SourceInfo, root: bool) -> R<Record> {
    if root && bson_type(node, at)? != Some("object") {
        return Err(fails("a schema whose bsonType isn't object", at));
    }
    let mut record = base_type(record, node);
    if let Some(Json::Object(properties)) = node.get("properties") {
        let properties = properties
            .iter()
            .map(|(key, value)| {
                Ok(Record::untyped()
                    .with("key", key.clone())
                    .with("value", bson_node_type(value, at)?))
            })
            .collect::<R<Vec<_>>>()?;
        record = record.with("properties", properties);
    }
    if let Some(value) = node.get("maxProperties") {
        record = record.with("maxProperties", Value::Integer(value.long()));
    }
    if let Some(value) = node.get("minProperties") {
        record = record.with("minProperties", Value::Integer(value.long()));
    }
    let mut allowed = false;
    match node.get("additionalProperties") {
        Some(Json::Bool(value)) => allowed = *value,
        Some(Json::Object(_)) => {
            // An `ObjectTypeImpl`, a class no protocol reader knows.
            return Err(fails("a schema of additional properties", at));
        }
        _ => {}
    }
    record = record.with("additionalPropertiesAllowed", allowed);
    if let Some(Json::Array(items)) = node.get("required") {
        let required = items
            .iter()
            .map(|item| item.text().map_or(JsonValue::Null, JsonValue::from))
            .collect();
        record = record.with("required", Value::Json(JsonValue::Array(required)));
    }
    Ok(record)
}

/// `getBsonNodeType`: a property's or an item's type, by its `bsonType`.
fn bson_node_type(node: &Json, at: &SourceInfo) -> R<Record> {
    let Json::Object(_) = node else {
        return Err(fails("a type that isn't an object", at));
    };
    let Some(type_name) = bson_type(node, at)? else {
        return Err(fails("a type whose bsonType isn't text", at));
    };
    let bounds = |record: Record,
                  maximum: &'static str,
                  minimum: &'static str,
                  value: fn(&Json) -> Value| {
        let mut record = base_type(record, node);
        if let Some(bound) = node.get(maximum) {
            record = record.with(maximum, value(bound));
        }
        if let Some(bound) = node.get(minimum) {
            record = record.with(minimum, value(bound));
        }
        record
    };
    let long = |json: &Json| Value::Integer(json.long());
    Ok(match type_name {
        "double" => bounds(Record::new("doubleType"), "maximum", "minimum", |json| {
            Value::Double(json.double())
        }),
        "string" => bounds(Record::new("stringType"), "maxLength", "minLength", long),
        "object" => object_type(Record::new("objectType"), node, at, false)?,
        "array" => array_type(node, at)?,
        "binData" => base_type(Record::new("binaryType"), node),
        "objectId" => base_type(Record::new("objectIdType"), node),
        "bool" => base_type(Record::new("boolType"), node),
        "date" => base_type(Record::new("dateType"), node),
        "int" => bounds(Record::new("intType"), "maximum", "minimum", long),
        "timestamp" => base_type(Record::new("timeStampType"), node),
        "long" => bounds(Record::new("longType"), "maximum", "minimum", long),
        "decimal" => bounds(Record::new("decimalType"), "maximum", "minimum", decimal),
        "minKey" => base_type(Record::new("minKeyType"), node),
        "maxKey" => base_type(Record::new("maxKeyType"), node),
        other => return Err(fails(&format!("the bsonType '{other}'"), at)),
    })
}

/// Jackson's `decimalValue`: a number as written; 0 for anything else.
fn decimal(json: &Json) -> Value {
    match json {
        Json::Number(text) => serde_json::from_str(text).map_or(Value::Integer(0), Value::Json),
        _ => Value::Integer(0),
    }
}

/// `processArrayTypeFields`.
fn array_type(node: &Json, at: &SourceInfo) -> R<Record> {
    let mut record = base_type(Record::new("arrayType"), node);
    if let Some(value) = node.get("maxItems") {
        record = record.with("maxItems", Value::Integer(value.long()));
    }
    if let Some(value) = node.get("minItems") {
        record = record.with("minItems", Value::Integer(value.long()));
    }
    let unique = node.get("uniqueItems").is_some_and(Json::boolean);
    if let Some(items) = node.get("items") {
        let items = match items {
            Json::Array(items) => items
                .iter()
                .map(|item| bson_node_type(item, at))
                .collect::<R<Vec<_>>>()?,
            item => vec![bson_node_type(item, at)?],
        };
        record = record.with("items", items);
    }
    Ok(record
        .with("additionalItemsAllowed", false)
        .with("uniqueItems", unique))
}
