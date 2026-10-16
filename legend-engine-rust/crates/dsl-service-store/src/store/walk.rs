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

//! The walk (the engine's `ServiceStoreParseTreeWalker`): each store's
//! services and groups checked and built in the walker's order, into
//! records shaped as the engine's protocol.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::blocks::{ReadBlock, ReadField, ReadValue};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::record::{Record, Value};

type R<T> = Result<T, ParseError>;

/// The engine's HTTP methods.
const METHODS: [&str; 2] = ["GET", "POST"];

/// The engine's parameter locations.
const LOCATIONS: [&str; 3] = ["HEADER", "PATH", "QUERY"];

fn error(message: impl Into<String>, at: &SourceInfo) -> ParseError {
    ParseError::Unexpected {
        message: message.into(),
        source_info: at.clone(),
    }
}

/// A value the grammar can't have read where it stands: a bug.
fn misread(what: &str, at: &SourceInfo) -> ParseError {
    error(format!("the service store grammar misread {what}"), at)
}

/// The walker's `visitServiceStore`: the store's own record, without its
/// package and name.
pub(crate) fn store(read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let elements = elements(read.fields, &at)?;
    Ok(Record::new("serviceStore")
        .at(&at)
        .with("elements", elements))
}

/// The services and groups among `fields`, walked in order, their ids
/// unique (checked on `at`, the store's or group's position).
fn elements(fields: Vec<ReadField>, at: &SourceInfo) -> R<Vec<Record>> {
    let mut ids = Vec::new();
    let mut records = Vec::new();
    for field in fields {
        let (id, record) = match field.name {
            "Service" => service(field)?,
            "ServiceGroup" => group(field)?,
            _ => continue,
        };
        ids.push(id);
        records.push(record);
    }
    let mut repeated: Vec<&str> = Vec::new();
    for id in &ids {
        if !repeated.contains(&id.as_str()) && ids.iter().filter(|other| *other == id).count() > 1 {
            repeated.push(id);
        }
    }
    if !repeated.is_empty() {
        return Err(error(
            format!(
                "Service Store Elements should have unique ids. Multiple elements found with ids - [{}]",
                repeated.join(",")
            ),
            at,
        ));
    }
    Ok(records)
}

/// A block's id, and the block.
fn identified(field: ReadField) -> R<(String, ReadBlock, SourceInfo)> {
    let at = field.source_info.clone();
    let mut read = field.block()?;
    let id = read.take("id", &at)?.identifier()?;
    Ok((id, read, at))
}

/// The required `path`, which starts with `/` and doesn't end with one.
fn path(read: &mut ReadBlock, at: &SourceInfo) -> R<String> {
    let field = read.take("path", at)?;
    let ReadValue::Identifier(name) = field.value else {
        return Err(misread("a path", &field.source_info));
    };
    let path = name.value.to_string();
    if !path.starts_with('/') || path.ends_with('/') {
        return Err(error(
            "Path should start with '/' & should not end with '/'",
            &name.source_info,
        ));
    }
    Ok(path)
}

/// The walker's `visitServiceGroup`: its path, then its elements.
fn group(field: ReadField) -> R<(String, Record)> {
    let (id, mut read, at) = identified(field)?;
    let path = path(&mut read, &at)?;
    let elements = elements(read.fields, &at)?;
    let record = Record::new("serviceGroup")
        .at(&at)
        .with("id", id.clone())
        .with("path", path)
        .with("elements", elements);
    Ok((id, record))
}

/// The walker's `visitService`, then its `validateService`.
fn service(field: ReadField) -> R<(String, Record)> {
    let (id, mut read, at) = identified(field)?;
    let path = path(&mut read, &at)?;
    let request_body = read
        .take_optional("requestBody", &at)?
        .map(type_reference)
        .transpose()?;
    let method = read.take("method", &at)?;
    let method_at = method.source_info.clone();
    let method = method.identifier()?;
    if !METHODS.contains(&method.as_str()) {
        return Err(error(
            format!(
                "Unsupported HTTP Method type - {method}. Supported types are - {}",
                METHODS.join(",")
            ),
            &method_at,
        ));
    }
    let parameters = match read.take_optional("parameters", &at)? {
        Some(parameters) => Some(
            parameters
                .values()?
                .into_iter()
                .map(|parameter| self::parameter(parameter, &at))
                .collect::<R<Vec<_>>>()?,
        ),
        None => None,
    };
    let response = read.take("response", &at)?;
    let response_at = response.source_info.clone();
    let response = type_reference(response)?;
    if response.kind != TypeKind::Complex {
        return Err(error(
            format!(
                "Expected Complex Type Reference for response, found : {}",
                response.kind.class_name()
            ),
            &response_at,
        ));
    }
    // The engine reads each security scheme with an extension of its
    // grammar.
    if let Some(scheme) = read.take("security", &at)?.values()?.into_iter().next() {
        let ReadValue::Identifier(scheme) = scheme else {
            return Err(misread("a security scheme", &at));
        };
        return Err(ParseError::Unsupported {
            message: format!(
                "no service store security scheme grammar is linked for '{}'",
                scheme.value
            ),
            source_info: scheme.source_info,
        });
    }
    if method == "GET" && request_body.is_some() {
        return Err(error(
            "Request Body should not be specified for GET end point",
            &at,
        ));
    }
    if let Some(parameters) = &parameters {
        let missing: Vec<&str> = parameters
            .iter()
            .filter(|parameter| parameter.location == "PATH")
            .map(|parameter| parameter.name.as_str())
            .filter(|name| !path.contains(&format!("{{{name}}}")))
            .collect();
        if !missing.is_empty() {
            return Err(error(
                format!(
                    "Path parameters should be specified in path as '{{param_name}}'. [{}] parameters were not found in path {path}",
                    missing.join(",")
                ),
                &at,
            ));
        }
    }
    let record = Record::new("service")
        .at(&at)
        .with("id", id.clone())
        .with("path", path)
        .with_some("requestBody", request_body.map(|body| body.record))
        .with("method", method)
        .with_some(
            "parameters",
            parameters.map(|parameters| {
                parameters
                    .into_iter()
                    .map(|parameter| parameter.record)
                    .collect::<Vec<_>>()
            }),
        )
        .with("response", response.record)
        .with("security", Vec::<Record>::new());
    Ok((id, record))
}

/// A type reference's kind.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum TypeKind {
    Boolean,
    Float,
    Integer,
    String,
    Complex,
}

impl TypeKind {
    /// The `_type` of its protocol class.
    fn type_name(self) -> &'static str {
        match self {
            Self::Boolean => "boolean",
            Self::Float => "float",
            Self::Integer => "integer",
            Self::String => "string",
            Self::Complex => "complex",
        }
    }

    /// Its protocol class's simple name.
    fn class_name(self) -> &'static str {
        match self {
            Self::Boolean => "BooleanTypeReference",
            Self::Float => "FloatTypeReference",
            Self::Integer => "IntegerTypeReference",
            Self::String => "StringTypeReference",
            Self::Complex => "ComplexTypeReference",
        }
    }
}

/// A type reference: its record, its kind, and whether it is a list's.
struct TypeReference {
    record: Record,
    kind: TypeKind,
    list: bool,
}

/// The walker's `visitTypeReference`, positioned on the whole reference:
/// a class and its binding, or one of four primitive types.
fn type_reference(field: ReadField) -> R<TypeReference> {
    let mut read = field.block()?;
    let at = read.source_info.clone();
    let list = read.keyword.kind == legend_pure_parser_lexer::TokenKind::LBracket;
    let class = read.take("type", &at)?;
    let class_at = class.source_info.clone();
    let class = class.path()?;
    let (kind, record) = if let Some(binding) = read.take_optional("binding", &at)? {
        (
            TypeKind::Complex,
            Record::new(TypeKind::Complex.type_name())
                .with("type", class)
                .with("binding", binding.path()?),
        )
    } else {
        let kind = match class.as_str() {
            "Boolean" => TypeKind::Boolean,
            "Float" => TypeKind::Float,
            "Integer" => TypeKind::Integer,
            "String" => TypeKind::String,
            _ => {
                return Err(error(
                    format!(
                        "Unsupported Parameter Value Type - {class}. Supported types are - Boolean, Float, Integer, String"
                    ),
                    &class_at,
                ));
            }
        };
        (kind, Record::new(kind.type_name()))
    };
    Ok(TypeReference {
        record: record.with("list", list).at(&at),
        kind,
        list,
    })
}

/// A parameter, with what the service's checks read of it.
struct Parameter {
    name: String,
    location: String,
    record: Record,
}

/// A boolean option's value.
fn boolean(field: ReadField) -> R<bool> {
    match field.value {
        ReadValue::Boolean(value) => Ok(value),
        _ => Err(misread("a boolean", &field.source_info)),
    }
}

/// The walker's `visitServiceParameter`: its options checked on the
/// parameter, `at`, in the walker's order.
fn parameter(value: ReadValue, service_at: &SourceInfo) -> R<Parameter> {
    let ReadValue::Block(mut read) = value else {
        return Err(misread("a parameter", service_at));
    };
    let at = read.source_info.clone();
    let ReadValue::Text(name, _) = read.take("name", &at)?.value else {
        return Err(misread("a parameter's name", &at));
    };
    let type_reference = type_reference(read.take("type", &at)?)?;
    if read.fields.is_empty() {
        return Err(error(
            "Field location is required for service parameters",
            &at,
        ));
    }
    let location = read.take("location", &at)?.identifier()?;
    let upper = location.to_uppercase();
    if !LOCATIONS.contains(&upper.as_str()) {
        return Err(error(
            format!(
                "Unsupported Parameter Location - {location}. Supported Locations are - {}",
                LOCATIONS.map(str::to_lowercase).join(",")
            ),
            &at,
        ));
    }
    let enumeration = read
        .take_optional("enum", &at)?
        .map(ReadField::path)
        .transpose()?;
    let allow_reserved = read
        .take_optional("allowReserved", &at)?
        .map(boolean)
        .transpose()?;
    let required = match read.take_optional("required", &at)? {
        Some(required) => {
            let required_at = required.source_info.clone();
            let required = boolean(required)?;
            if upper == "PATH" && !required {
                return Err(error("Path parameters cannot be optional", &required_at));
            }
            Some(required)
        }
        None => None,
    };
    let mut format = Record::untyped();
    if type_reference.list {
        let style = read.take("style", &at)?;
        let style_at = style.source_info.clone();
        let explode = read.take("explode", &at)?;
        let explode_at = explode.source_info.clone();
        format = format
            .with("style", style.identifier()?)
            .with("explode", boolean(explode)?)
            .with("styleSourceInformation", Value::SourceInfo(style_at))
            .with("explodeSourceInformation", Value::SourceInfo(explode_at));
    } else {
        if let Some(style) = read.take_optional("style", &at)? {
            return Err(error(
                "style should not be provided with non-list service parameter",
                &style.source_info,
            ));
        }
        if let Some(explode) = read.take_optional("explode", &at)? {
            // The engine positions this error on the style, which there is
            // none of: it fails without a message.
            return Err(ParseError::Unsupported {
                message:
                    "the engine fails on explode without style for a non-list service parameter"
                        .to_string(),
                source_info: explode.source_info,
            });
        }
    }
    let record = Record::untyped()
        .at(&at)
        .with("name", name.clone())
        .with("type", type_reference.record)
        .with("location", upper.clone())
        .with_some("allowReserved", allow_reserved)
        .with_some("required", required)
        .with_some("enumeration", enumeration)
        .with("serializationFormat", format);
    Ok(Parameter {
        name,
        location: upper,
        record,
    })
}
