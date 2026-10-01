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

//! The walk (the engine's `DeephavenParseTreeWalker`): each store's tables
//! and each app's fields checked and built in the walker's order, into
//! records shaped as the engine's protocol. The walker names a store's
//! fields by the lexer's literal names (`'tables'`), which its messages
//! quote again.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::Token;
use legend_pure_parser_parser::blocks::{ReadBlock, ReadField, ReadValue};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::{missing, required};
use legend_pure_parser_protocol::v1::record::{Record, Value};

use super::read::{ReadColumn, ReadTable};

type R<T> = Result<T, ParseError>;

/// The walker's `visitDeephavenStore`: the store's own record, without its
/// package and name.
pub(crate) fn store(tables: Vec<ReadTable>, at: &SourceInfo) -> R<Record> {
    // The grammar reads a store's tables as one list, if any.
    if tables.is_empty() {
        return Err(missing("'tables'", at));
    }
    let tables = tables.into_iter().map(table).collect::<R<Vec<_>>>()?;
    Ok(Record::new("deephavenStore").at(at).with("tables", tables))
}

/// The walker's `visitTableDefinition`: one list of columns.
fn table(read: ReadTable) -> R<Record> {
    let columns = required(read.columns, "'columns'", &read.source_info)?
        .into_iter()
        .map(column)
        .collect::<R<Vec<_>>>()?;
    Ok(Record::untyped()
        .with("name", read.name)
        .with("columns", columns))
}

/// The walker's `visitColumn`.
fn column(read: ReadColumn) -> R<Record> {
    let mut column_type = Record::new(read.type_name);
    if let Some((precision, scale)) = read.decimal {
        column_type = column_type
            .with("precision", Value::Integer(long(precision)?))
            .with("scale", Value::Integer(long(scale)?));
    }
    Ok(Record::new("column")
        .with("name", read.name)
        .with("type", column_type))
}

/// An integer as the walker reads it, `Long.parseLong`: one too large
/// makes the engine fail.
fn long(token: Token) -> R<i64> {
    token.text.parse().map_err(|_| ParseError::Unsupported {
        message: format!("the engine fails on {}, too large for a long", token.text),
        source_info: token.source_info,
    })
}

/// The walker's `visitDeephavenApp`: the app's own record, without its
/// package and name.
pub(crate) fn app(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let application_name = read.take("applicationName", &at)?.string()?;
    let function = function(read.take("function", &at)?)?;
    let owner = read.take("ownership", &at)?.string()?;
    let description = read
        .take_optional("description", &at)?
        .map(ReadField::string)
        .transpose()?;
    Ok(Record::new("DeephavenApp")
        .at(&at)
        .with("applicationName", application_name)
        .with("function", function)
        .with(
            "ownership",
            Record::new("DeploymentOwner").with("id", owner),
        )
        .with_some("description", description))
}

/// The activated function, positioned on its identifier.
fn function(field: ReadField) -> R<Record> {
    let ReadValue::Text(path, at) = field.value else {
        return Err(field.misread());
    };
    Ok(Record::untyped()
        .with("type", "FUNCTION".to_string())
        .with("path", path)
        .at(&at))
}
