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

//! The walk (the engine's `MemSqlFunctionTreeWalker`): each function's
//! fields checked and built in the walker's order, into a record shaped as
//! the engine's protocol.

use legend_pure_dsl_function_activator::walk::{deployment_owner, function};
use legend_pure_parser_parser::blocks::{ReadBlock, ReadField, ReadValue};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::record::Record;

type R<T> = Result<T, ParseError>;

/// The walker's `visitMemSqlFunction`: the function's own record, without
/// its package, name and annotations.
pub(crate) fn function_activator(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let function_name = read.take("functionName", &at)?.string()?;
    let function = function(read.take("function", &at)?)?;
    let ownership = deployment_owner(read.take("ownership", &at)?)?;
    let description = read
        .take_optional("description", &at)?
        .map(ReadField::string)
        .transpose()?;
    let activation = read
        .take_optional("activationConfiguration", &at)?
        .map(activation)
        .transpose()?;
    Ok(Record::new("memSqlFunction")
        .at(&at)
        .with("functionName", function_name)
        .with("function", function)
        .with("ownership", ownership)
        .with_some("description", description)
        .with_some("activationConfiguration", activation))
}

/// The deployment configuration: its connection as written (`getText()`),
/// unpositioned, as the walker leaves it.
fn activation(field: ReadField) -> R<Record> {
    let ReadValue::Path(connection) = field.value else {
        return Err(field.misread());
    };
    Ok(Record::new("memSqlFunctionConfig").with(
        "activationConnection",
        Record::new("connectionPointer").with("connection", connection.text),
    ))
}
