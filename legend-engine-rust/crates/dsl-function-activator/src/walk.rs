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

//! An activator's walk: its element, its annotations first, then its
//! fields, which its section's walk checks and builds; and the records of
//! the fields most activators have.

use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{ReadBlock, ReadField, ReadValue};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::record::Record;

use crate::element::ActivatorElement;
use crate::read::ReadActivator;

type R<T> = Result<T, ParseError>;

/// The walker's element, of the section `section`: its annotations, its
/// documentation the first tagged value, then its record, which `fields`
/// builds from its fields.
///
/// # Errors
///
/// Documentation that isn't a text block, or `fields`'s error.
pub fn activator(
    read: ReadActivator,
    section: &'static str,
    fields: impl FnOnce(ReadBlock) -> R<Record>,
) -> R<ActivatorElement> {
    let source_info = read.body.source_info.clone();
    let tagged_values = ParserContext::with_documentation(read.documentation, read.tagged_values)?;
    let record = fields(read.body)?;
    Ok(ActivatorElement {
        section,
        package: read.package,
        name: SpannedString {
            value: read.name,
            source_info: read.name_source_info,
        },
        stereotypes: read.stereotypes,
        tagged_values,
        record,
        source_info,
    })
}

/// The activated function, as [`read::function`](crate::read::function)
/// reads it: a `PackageableElementPointer` of type `FUNCTION`, positioned
/// on its identifier.
///
/// # Errors
///
/// Anything else read.
pub fn function(field: ReadField) -> R<Record> {
    let ReadValue::Text(path, at) = field.value else {
        return Err(field.misread());
    };
    Ok(Record::untyped()
        .with("type", "FUNCTION".to_string())
        .with("path", path)
        .at(&at))
}

/// The owner, a deployment's, as
/// [`read::deployment`](crate::read::deployment) reads it: a
/// `DeploymentOwner`.
///
/// # Errors
///
/// Anything else read.
pub fn deployment_owner(field: ReadField) -> R<Record> {
    Ok(Record::new("DeploymentOwner").with("id", field.string()?))
}
