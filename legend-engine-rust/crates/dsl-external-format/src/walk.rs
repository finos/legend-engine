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

//! The walk (the engine's `ExternalFormatParseTreeWalker`): each element's
//! fields checked and built in the walker's order, into records shaped as
//! the engine's protocol.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::blocks::{ReadBlock, ReadField, ReadValue};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::record::{Record, Value};

use crate::ast::ExternalFormatKind;

type R<T> = Result<T, ParseError>;

/// The schema formats the engine's modules define (their external format
/// contracts' ids); a test's own (`Example`) is none of them.
const FORMATS: [&str; 9] = [
    "Arrow",
    "DAML",
    "FlatData",
    "GraphQL_Introspection",
    "GraphQL_SDL",
    "Haskell",
    "JSON",
    "Protobuf",
    "XSD",
];

/// A value the grammar can't have read where it stands: a bug.
fn misread(what: &str, at: &SourceInfo) -> ParseError {
    ParseError::Unexpected {
        message: format!("the external format grammar misread {what}"),
        source_info: at.clone(),
    }
}

/// The walker's `visitSchemaSet` or `visitBinding`: the element's own
/// record, without its package and name.
pub(crate) fn element(kind: ExternalFormatKind, read: ReadBlock) -> R<Record> {
    match kind {
        ExternalFormatKind::SchemaSet => schema_set(read),
        ExternalFormatKind::Binding => binding(read),
    }
}

/// The walker's `visitSchemaSet`: its format, one an extension of the
/// engine defines, then its schemas.
fn schema_set(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let format = read.take("format", &at)?;
    let format_at = format.source_info.clone();
    let format = format.identifier()?;
    if !FORMATS.contains(&format.as_str()) {
        return Err(ParseError::Unsupported {
            message: format!(
                "the schema format '{format}' is none the engine's modules define: the engine checks it against the formats it has"
            ),
            source_info: format_at,
        });
    }
    let schemas = read
        .take("schemas", &at)?
        .values()?
        .into_iter()
        .map(|schema| self::schema(schema, &at))
        .collect::<R<Vec<_>>>()?;
    Ok(Record::new("externalFormatSchemaSet")
        .at(&at)
        .with("format", format)
        .with("schemas", schemas))
}

/// The walker's `visitSchema`: its content positioned on its field.
fn schema(value: ReadValue, at: &SourceInfo) -> R<Record> {
    let ReadValue::Block(mut read) = value else {
        return Err(misread("a schema", at));
    };
    let at = read.source_info.clone();
    let id = read
        .take_optional("id", &at)?
        .map(ReadField::identifier)
        .transpose()?;
    let location = read
        .take_optional("location", &at)?
        .map(ReadField::string)
        .transpose()?;
    let content = read.take("content", &at)?;
    let content_at = content.source_info.clone();
    Ok(Record::untyped()
        .at(&at)
        .with_some("id", id)
        .with_some("location", location)
        .with("content", content.string()?)
        .with("contentSourceInformation", Value::SourceInfo(content_at)))
}

/// The walker's `visitBinding`.
fn binding(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let schema_set = read
        .take_optional("schemaSet", &at)?
        .map(ReadField::path)
        .transpose()?;
    let schema_id = read
        .take_optional("schemaId", &at)?
        .map(ReadField::identifier)
        .transpose()?;
    let content_type = read.take("contentType", &at)?.string()?;
    let includes = paths(read.take("modelIncludes", &at)?)?;
    let excludes = read
        .take_optional("modelExcludes", &at)?
        .map(paths)
        .transpose()?;
    let model_unit = Record::untyped()
        .with("packageableElementIncludes", includes)
        .with_some("packageableElementExcludes", excludes);
    Ok(Record::new("binding")
        .at(&at)
        .with_some("schemaSet", schema_set)
        .with_some("schemaId", schema_id)
        .with("contentType", content_type)
        .with("modelUnit", model_unit))
}

/// A list's paths.
fn paths(field: ReadField) -> R<Vec<String>> {
    let at = field.source_info.clone();
    field
        .values()?
        .into_iter()
        .map(|value| match value {
            ReadValue::Path(name) => Ok(name.path),
            _ => Err(misread("a path", &at)),
        })
        .collect()
}
