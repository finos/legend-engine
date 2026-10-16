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

//! The walk (the engine's `DiagramParseTreeWalker`): each diagram's views
//! checked and built in the walker's order (its class views, then its
//! property views, then its generalization views), into records shaped as
//! the engine's protocol.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::blocks::{ReadBlock, ReadField, ReadValue};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::record::{Record, Value};

type R<T> = Result<T, ParseError>;

/// A value the grammar can't have read where it stands: a bug.
fn misread(what: &str, at: &SourceInfo) -> ParseError {
    ParseError::Unexpected {
        message: format!("the diagram grammar misread {what}"),
        source_info: at.clone(),
    }
}

/// The walker's `visitDiagram`: the diagram's own record, without its
/// package and name. Its lists are always written: the engine's walker
/// sets them, and its class defaults them to null.
pub(crate) fn diagram(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let class_views = read
        .all("classView")
        .into_iter()
        .map(class_view)
        .collect::<R<Vec<_>>>()?;
    let property_views = read
        .all("propertyView")
        .into_iter()
        .map(|view| relationship_view(view, true))
        .collect::<R<Vec<_>>>()?;
    let generalization_views = read
        .all("generalizationView")
        .into_iter()
        .map(|view| relationship_view(view, false))
        .collect::<R<Vec<_>>>()?;
    Ok(Record::new("diagram")
        .at(&at)
        .with("classViews", class_views)
        .with("propertyViews", property_views)
        .with("generalizationViews", generalization_views))
}

/// A number pair's two numbers, as the walker's `Double.parseDouble` reads
/// them.
fn numbers(field: ReadField) -> R<(Value, Value)> {
    let at = field.source_info.clone();
    let numbers = field
        .values()?
        .into_iter()
        .map(|value| match value {
            ReadValue::Text(text, number_at) => text
                .parse::<f64>()
                .map(Value::Double)
                .map_err(|_| misread("a number", &number_at)),
            _ => Err(misread("a number", &at)),
        })
        .collect::<R<Vec<_>>>()?;
    let [first, second]: [Value; 2] = numbers
        .try_into()
        .map_err(|_| misread("a number pair", &at))?;
    Ok((first, second))
}

/// A point, `(x, y)`.
fn point(field: ReadField) -> R<Record> {
    let (x, y) = numbers(field)?;
    Ok(Record::untyped().with("x", x).with("y", y))
}

/// A flag the walker sets only when true.
fn flag(field: Option<ReadField>) -> Option<bool> {
    field.and_then(|field| match field.value {
        ReadValue::Boolean(true) => Some(true),
        _ => None,
    })
}

/// A view's id, as written, and where.
fn view_id(field: ReadField) -> R<(String, SourceInfo)> {
    match field.value {
        ReadValue::Text(id, at) => Ok((id, at)),
        _ => Err(misread("a view's id", &field.source_info)),
    }
}

/// The walker's `visitClassView`.
fn class_view(field: ReadField) -> R<Record> {
    let at = field.source_info.clone();
    let mut read = field.block()?;
    let (id, _) = view_id(read.take("id", &at)?)?;
    let class = read.take("class", &at)?;
    let ReadValue::Path(class) = class.value else {
        return Err(misread("a class", &class.source_info));
    };
    let position = point(read.take("position", &at)?)?;
    let (width, height) = numbers(read.take("rectangle", &at)?)?;
    let hide_properties = flag(read.take_optional("hideProperties", &at)?);
    let hide_tagged_values = flag(read.take_optional("hideTaggedValue", &at)?);
    let hide_stereotypes = flag(read.take_optional("hideStereotype", &at)?);
    Ok(Record::untyped()
        .at(&at)
        .with("id", id)
        .with("class", class.path)
        .with(
            "classSourceInformation",
            Value::SourceInfo(class.source_info),
        )
        .with("position", position)
        .with(
            "rectangle",
            Record::untyped()
                .with("width", width)
                .with("height", height),
        )
        .with_some("hideProperties", hide_properties)
        .with_some("hideTaggedValues", hide_tagged_values)
        .with_some("hideStereotypes", hide_stereotypes))
}

/// The walker's `visitPropertyView`, its property first, or
/// `visitGeneralizationView`.
fn relationship_view(field: ReadField, property: bool) -> R<Record> {
    let at = field.source_info.clone();
    let mut read = field.block()?;
    let mut record = Record::untyped().at(&at);
    if property {
        let pointer = read.take("property", &at)?;
        let pointer_at = pointer.source_info.clone();
        let ReadValue::Pair(class, name, _) = pointer.value else {
            return Err(misread("a property", &pointer_at));
        };
        let (ReadValue::Path(class), ReadValue::Identifier(name)) = (*class, *name) else {
            return Err(misread("a property", &pointer_at));
        };
        record = record.with(
            "property",
            Record::untyped()
                .with("class", class.path)
                .with("property", name.value.to_string())
                .at(&class.source_info),
        );
    }
    let points = read
        .take("points", &at)?
        .values()?
        .into_iter()
        .map(|pair| {
            point(ReadField {
                name: "points",
                value: pair,
                source_info: at.clone(),
            })
        })
        .collect::<R<Vec<_>>>()?;
    let (source, source_at) = view_id(read.take("source", &at)?)?;
    let (target, target_at) = view_id(read.take("target", &at)?)?;
    Ok(record
        .with("line", Record::untyped().with("points", points))
        .with("sourceView", source)
        .with("sourceViewSourceInformation", Value::SourceInfo(source_at))
        .with("targetView", target)
        .with("targetViewSourceInformation", Value::SourceInfo(target_at)))
}
