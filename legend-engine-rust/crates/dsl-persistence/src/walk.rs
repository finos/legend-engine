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

//! The walk (the engine's `PersistenceParseTreeWalker` and
//! `PersistenceContextParseTreeWalker`): each element's fields checked and
//! built in the walker's order, into records shaped as the engine's protocol.
//!
//! The walker positions most objects on their rule; some it builds without
//! a position (`None` strategies, a `DeleteIndicator` merge strategy), as the
//! records here do. A service output's dataset is read differently for a
//! graph fetch (paths) and a TDS (names): the walker fails, without a
//! position, on one written the other way, which is unsupported here.

use legend_pure_dsl_connection::section::parse_embedded_connection;
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::string_text;

use crate::grammar::{
    Specification, SpecificationKind, linked_specification_types, specification_grammar,
};
use legend_pure_dsl_connection::protocol::convert_connection_value;
use legend_pure_parser_parser::blocks::{ReadBlock, ReadField, ReadSpecification, ReadValue};
use legend_pure_parser_protocol::v1::record::{Record, Value};

type R<T> = Result<T, ParseError>;

/// Whether a service output's dataset is a graph fetch's or a TDS's.
#[derive(Clone, Copy, PartialEq, Eq)]
enum Dataset {
    GraphFetch,
    Tds,
}

// ── Taking fields ────────────────────────────────────────────────────────

fn take(block: &mut ReadBlock, name: &'static str, at: &SourceInfo) -> R<ReadField> {
    block.take(name, at)
}

fn take_optional(
    block: &mut ReadBlock,
    name: &'static str,
    at: &SourceInfo,
) -> R<Option<ReadField>> {
    block.take_optional(name, at)
}

/// A value the grammar can't have read for the field: a bug.
fn misread(field: &ReadField) -> ParseError {
    ParseError::Unexpected {
        message: format!("the persistence grammar misread the field '{}'", field.name),
        source_info: field.source_info.clone(),
    }
}

fn string(field: ReadField) -> R<String> {
    field.string()
}

fn identifier(field: ReadField) -> R<String> {
    field.identifier()
}

fn path(field: ReadField) -> R<String> {
    field.path()
}

fn block(field: ReadField) -> R<ReadBlock> {
    field.block()
}

fn values(field: ReadField) -> R<Vec<ReadValue>> {
    field.values()
}

/// A pointer to an element: `type`, `path`, positioned on `at`.
fn pointer(element_type: Option<&str>, path: String, at: &SourceInfo) -> Record {
    let pointer = Record::untyped();
    let pointer = match element_type {
        Some(element_type) => pointer.with("type", element_type.to_string()),
        None => pointer,
    };
    pointer.with("path", path).at(at)
}

/// A dataset's field, a path for a graph fetch, a name for a TDS: the
/// walker fails without a position on the other.
fn output_value(dataset: Dataset, value: ReadValue, at: &SourceInfo) -> R<Value> {
    match (dataset, value) {
        (Dataset::GraphFetch, ReadValue::NavigationPath(path)) => Ok(Value::Path(path)),
        (Dataset::Tds, ReadValue::Identifier(name)) => Ok(Value::Text(name.value.to_string())),
        _ => Err(ParseError::Unsupported {
            message: "the engine fails on a graph fetch's dataset field written as a name, or a \
                      TDS's as a path"
                .to_string(),
            source_info: at.clone(),
        }),
    }
}

fn output_values(dataset: Dataset, field: ReadField) -> R<Value> {
    let at = field.source_info.clone();
    let values = values(field)?;
    if dataset == Dataset::GraphFetch {
        let paths = values
            .into_iter()
            .map(|value| match output_value(dataset, value, &at)? {
                Value::Path(path) => Ok(*path),
                _ => Err(ParseError::Unsupported {
                    message: "a graph fetch's dataset field is a path".to_string(),
                    source_info: at.clone(),
                }),
            })
            .collect::<R<Vec<Expression>>>()?;
        return Ok(Value::Paths(paths));
    }
    let names = values
        .into_iter()
        .map(|value| match output_value(dataset, value, &at)? {
            Value::Text(name) => Ok(name),
            _ => Err(ParseError::Unsupported {
                message: "a TDS's dataset field is a name".to_string(),
                source_info: at.clone(),
            }),
        })
        .collect::<R<Vec<String>>>()?;
    Ok(Value::Texts(names))
}

/// A list of names (`fromIdentifier`).
fn names(field: ReadField) -> R<Vec<String>> {
    let at = field.source_info.clone();
    values(field)?
        .into_iter()
        .map(|value| match value {
            ReadValue::Identifier(name) => Ok(name.value.to_string()),
            _ => Err(ParseError::Unsupported {
                message: "the engine fails on a path where a name is expected".to_string(),
                source_info: at.clone(),
            }),
        })
        .collect()
}

/// A list of strings, unescaped.
fn strings(field: ReadField) -> R<Vec<String>> {
    let at = field.source_info.clone();
    values(field)?
        .into_iter()
        .map(|value| match value {
            ReadValue::String(token) => string_text(&token),
            _ => Err(ParseError::Unexpected {
                message: "the persistence grammar misread a string".to_string(),
                source_info: at.clone(),
            }),
        })
        .collect()
}

/// A field whose value is a name or a path, read as the walker reads a
/// TDS's (`identifier()`), failing on a path.
fn name_of(field: ReadField) -> R<String> {
    let at = field.source_info.clone();
    match field.value {
        ReadValue::Identifier(name) => Ok(name.value.to_string()),
        ReadValue::NavigationPath(_) => Err(ParseError::Unsupported {
            message: "the engine fails on a path where a name is expected".to_string(),
            source_info: at,
        }),
        _ => Err(ParseError::Unexpected {
            message: "the persistence grammar misread a name".to_string(),
            source_info: at,
        }),
    }
}

/// A field whose value is a name or a path, read as the walker reads a
/// graph fetch's (`dslNavigationPath()`), failing on a name.
fn path_of(field: ReadField) -> R<Value> {
    let at = field.source_info.clone();
    match field.value {
        ReadValue::NavigationPath(path) => Ok(Value::Path(path)),
        _ => Err(ParseError::Unsupported {
            message: "the engine fails on a name where a path is expected".to_string(),
            source_info: at,
        }),
    }
}

// ── Specifications ───────────────────────────────────────────────────────

/// A trigger, platform or target, read by the grammar its type names.
fn specification(
    ctx: &mut ParserContext<'_>,
    kind: SpecificationKind,
    read: &ReadSpecification,
) -> R<Record> {
    let type_name = read.type_name.text.as_str();
    let Some(grammar) = specification_grammar(kind, type_name) else {
        return Err(ParseError::Unsupported {
            message: format!(
                "no {} grammar is linked for '{type_name}'; the linked ones are: {}",
                kind.name(),
                linked_specification_types(kind)
            ),
            source_info: read.source_info.clone(),
        });
    };
    grammar.parse(
        &Specification {
            island: read.island.as_ref(),
            source_info: &read.source_info,
        },
        ctx,
    )
}

fn specification_field(
    ctx: &mut ParserContext<'_>,
    kind: SpecificationKind,
    field: ReadField,
) -> R<Record> {
    match &field.value {
        ReadValue::Specification(read) => specification(ctx, kind, read),
        _ => Err(misread(&field)),
    }
}

// ── Elements ─────────────────────────────────────────────────────────────

/// The walker's `visitPersistence`.
pub(crate) fn persistence(ctx: &mut ParserContext<'_>, mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let documentation = string(take(&mut read, "doc", &at)?)?;
    let trigger = specification_field(
        ctx,
        SpecificationKind::Trigger,
        take(&mut read, "trigger", &at)?,
    )?;
    let service = take(&mut read, "service", &at)?;
    let service_at = service.source_info.clone();
    let service = pointer(Some("SERVICE"), path(service)?, &service_at);
    let targets = match take_optional(&mut read, "serviceOutputTargets", &at)? {
        Some(field) => values(field)?
            .into_iter()
            .map(|value| output_target(ctx, value))
            .collect::<R<Vec<_>>>()?,
        None => Vec::new(),
    };
    let persister = take_optional(&mut read, "persister", &at)?
        .map(persister)
        .transpose()?;
    let notifier = match take_optional(&mut read, "notifier", &at)? {
        Some(field) => notifier(field)?,
        None => Record::untyped().with("notifyees", Vec::<Record>::new()),
    };
    let tests = take_optional(&mut read, "tests", &at)?
        .map(|field| {
            values(field)?
                .into_iter()
                .map(|value| test(ctx, value))
                .collect::<R<Vec<_>>>()
        })
        .transpose()?;
    Ok(Record::new("persistence")
        .at(&at)
        .with("documentation", documentation)
        .with("trigger", trigger)
        .with("service", service)
        .with("serviceOutputTargets", targets)
        .with_some("persister", persister)
        .with("notifier", notifier)
        .with_some("tests", tests))
}

/// The walker's `visitPersistenceContext`.
pub(crate) fn context(ctx: &mut ParserContext<'_>, mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let persistence = take(&mut read, "persistence", &at)?;
    let persistence_at = persistence.source_info.clone();
    let persistence = pointer(Some("PERSISTENCE"), path(persistence)?, &persistence_at);
    let platform = match take_optional(&mut read, "platform", &at)? {
        Some(field) => specification_field(ctx, SpecificationKind::Platform, field)?,
        None => Record::new("default"),
    };
    let parameters = match take_optional(&mut read, "serviceParameters", &at)? {
        Some(field) => values(field)?
            .into_iter()
            .map(|value| service_parameter(ctx, value))
            .collect::<R<Vec<_>>>()?,
        None => Vec::new(),
    };
    let sink = take_optional(&mut read, "sinkConnection", &at)?
        .map(|field| connection(ctx, field.value))
        .transpose()?;
    let mut context = Record::new("persistenceContext")
        .at(&at)
        .with("persistence", persistence)
        .with("platform", platform)
        .with("serviceParameters", parameters);
    if let Some(sink) = sink {
        context = context.with("sinkConnection", sink);
    }
    Ok(context)
}

/// `name = value`: a primitive value, or a connection.
fn service_parameter(ctx: &mut ParserContext<'_>, read: ReadValue) -> R<Record> {
    let ReadValue::Named(name, value, at) = read else {
        return Err(ParseError::Unexpected {
            message: "the persistence grammar misread a service parameter".to_string(),
            source_info: SourceInfo::new("", 0, 0, 0, 0),
        });
    };
    let value = match *value {
        // The walker parses the value's text again, its tokens' texts
        // joined (`getText()`), where the value starts.
        ReadValue::Text(text, start) => {
            let value = ctx.parse_text(&text, &start, primitive_value)?;
            Record::new("primitiveTypeValue")
                .with("primitiveType", Value::Primitive(Box::new(value)))
        }
        other => Record::new("connectionValue").with("connection", connection(ctx, other)?),
    };
    Ok(Record::untyped()
        .at(&at)
        .with("name", name.value.to_string())
        .with("value", value))
}

fn primitive_value(ctx: &mut ParserContext<'_>) -> R<Expression> {
    ctx.parse_primitive_value()
}

/// A connection: its path, or one of the element's own.
fn connection(ctx: &mut ParserContext<'_>, read: ReadValue) -> R<Value> {
    match read {
        ReadValue::Path(name) => Ok(Value::Record(Box::new(
            Record::new("connectionPointer")
                .with("connection", name.path)
                .at(&name.source_info),
        ))),
        ReadValue::Island(island) => {
            let connection = parse_embedded_connection(ctx, &island)?;
            let json =
                convert_connection_value(&connection).map_err(|error| ParseError::Unsupported {
                    message: error.to_string(),
                    source_info: connection.source_info.clone(),
                })?;
            Ok(Value::Json(json))
        }
        _ => Err(ParseError::Unexpected {
            message: "the persistence grammar misread a connection".to_string(),
            source_info: SourceInfo::new("", 0, 0, 0, 0),
        }),
    }
}

// ── Service output targets ───────────────────────────────────────────────

/// The walker's `visitServiceOutputTarget`: `{}` is no target.
fn output_target(ctx: &mut ParserContext<'_>, read: ReadValue) -> R<Record> {
    let ReadValue::Pair(output, target, at) = read else {
        return Err(ParseError::Unexpected {
            message: "the persistence grammar misread a service output target".to_string(),
            source_info: SourceInfo::new("", 0, 0, 0, 0),
        });
    };
    let ReadValue::Block(output) = *output else {
        return Err(ParseError::Unexpected {
            message: "the persistence grammar misread a service output".to_string(),
            source_info: at,
        });
    };
    let output = service_output(output)?;
    let target = match *target {
        ReadValue::Empty => None,
        ReadValue::Specification(specification) => Some(self::specification(
            ctx,
            SpecificationKind::Target,
            &specification,
        )?),
        _ => {
            return Err(ParseError::Unexpected {
                message: "the persistence grammar misread a target".to_string(),
                source_info: at,
            });
        }
    };
    Ok(Record::untyped()
        .at(&at)
        .with("serviceOutput", output)
        .with_some("persistenceTarget", target))
}

/// The walker's `visitServiceOutput`.
fn service_output(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    if read.keyword.text == "TDS" {
        let keys = output_values(Dataset::Tds, take(&mut read, "keys", &at)?)?;
        let deduplication = take_optional(&mut read, "deduplication", &at)?
            .map(|field| deduplication(Dataset::Tds, field))
            .transpose()?;
        let dataset_type = dataset_type(Dataset::Tds, take(&mut read, "datasetType", &at)?)?;
        return Ok(Record::new("tdsServiceOutput")
            .at(&at)
            .with("keys", keys)
            .with_some("deduplication", deduplication)
            .with("datasetType", dataset_type));
    }
    let path = path_of(take(&mut read, "path", &at)?)?;
    let keys = output_values(Dataset::GraphFetch, take(&mut read, "keys", &at)?)?;
    let deduplication = take_optional(&mut read, "deduplication", &at)?
        .map(|field| deduplication(Dataset::GraphFetch, field))
        .transpose()?;
    let dataset_type = dataset_type(Dataset::GraphFetch, take(&mut read, "datasetType", &at)?)?;
    Ok(Record::new("graphFetchServiceOutput")
        .at(&at)
        .with("path", path)
        .with("keys", keys)
        .with_some("deduplication", deduplication)
        .with("datasetType", dataset_type))
}

/// The walker's `visitDeduplication`.
fn deduplication(dataset: Dataset, field: ReadField) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    Ok(match read.keyword.text.as_str() {
        "None" => Record::new("noDeduplication").at(&at),
        "AnyVersion" => Record::new("anyVersion").at(&at),
        _ => {
            let version = take(&mut read, "versionField", &at)?;
            match dataset {
                Dataset::GraphFetch => Record::new("maxVersionForGraphFetch")
                    .at(&at)
                    .with("versionFieldPath", path_of(version)?),
                Dataset::Tds => Record::new("maxVersionForTds")
                    .at(&at)
                    .with("versionField", name_of(version)?),
            }
        }
    })
}

/// The walker's `visitDatasetType`.
fn dataset_type(dataset: Dataset, field: ReadField) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    if read.keyword.text == "Snapshot" {
        let partitioning = take_optional(&mut read, "partitioning", &at)?
            .map(|field| partitioning(dataset, field))
            .transpose()?;
        return Ok(Record::new("snapshot")
            .at(&at)
            .with_some("partitioning", partitioning));
    }
    let indicator = take_optional(&mut read, "actionIndicator", &at)?
        .map(|field| action_indicator(dataset, field))
        .transpose()?;
    Ok(Record::new("delta")
        .at(&at)
        .with_some("actionIndicator", indicator))
}

/// The walker's `visitPartitioning`.
fn partitioning(dataset: Dataset, field: ReadField) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    if read.keyword.text == "None" {
        let handling = take_optional(&mut read, "emptyDatasetHandling", &at)?
            .map(|field| {
                let read = block(field)?;
                let kind = if read.keyword.text == "NoOp" {
                    "noOp"
                } else {
                    "deleteTargetDataset"
                };
                Ok::<_, ParseError>(Record::new(kind).at(&read.source_info))
            })
            .transpose()?;
        return Ok(Record::new("noPartitioning")
            .at(&at)
            .with_some("emptyDatasetHandling", handling));
    }
    let fields = take(&mut read, "partitionFields", &at)?;
    Ok(match dataset {
        Dataset::GraphFetch => Record::new("fieldBasedForGraphFetch")
            .at(&at)
            .with("partitionFieldPaths", output_values(dataset, fields)?),
        Dataset::Tds => Record::new("fieldBasedForTds")
            .at(&at)
            .with("partitionFields", output_values(dataset, fields)?),
    })
}

/// The walker's `visitActionIndicator`.
fn action_indicator(dataset: Dataset, field: ReadField) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    if read.keyword.text == "None" {
        return Ok(Record::new("noActionIndicator").at(&at));
    }
    let delete_field = take(&mut read, "deleteField", &at)?;
    let (kind, field_name, delete_field) = match dataset {
        Dataset::GraphFetch => (
            "deleteIndicatorForGraphFetch",
            "deleteFieldPath",
            path_of(delete_field)?,
        ),
        Dataset::Tds => (
            "deleteIndicatorForTds",
            "deleteField",
            Value::Text(name_of(delete_field)?),
        ),
    };
    let delete_values = names(take(&mut read, "deleteValues", &at)?)?;
    Ok(Record::new(kind)
        .at(&at)
        .with(field_name, delete_field)
        .with("deleteValues", delete_values))
}

// ── Notifier ─────────────────────────────────────────────────────────────

/// The walker's `visitNotifier`, positioned on the field.
fn notifier(field: ReadField) -> R<Record> {
    let at = field.source_info.clone();
    let mut read = block(field)?;
    let notifyees = values(take(&mut read, "notifyees", &at)?)?
        .into_iter()
        .map(|value| {
            let ReadValue::Block(mut notifyee) = value else {
                return Err(ParseError::Unexpected {
                    message: "the persistence grammar misread a notifyee".to_string(),
                    source_info: at.clone(),
                });
            };
            let notifyee_at = notifyee.source_info.clone();
            if notifyee.keyword.text == "Email" {
                let address = string(take(&mut notifyee, "address", &notifyee_at)?)?;
                Ok(Record::new("emailNotifyee")
                    .at(&notifyee_at)
                    .with("address", address))
            } else {
                let url = string(take(&mut notifyee, "url", &notifyee_at)?)?;
                Ok(Record::new("pagerDutyNotifyee")
                    .at(&notifyee_at)
                    .with("url", url))
            }
        })
        .collect::<R<Vec<_>>>()?;
    Ok(Record::untyped().at(&at).with("notifyees", notifyees))
}

// ── Tests ────────────────────────────────────────────────────────────────

/// The walker's `visitPersistenceTest`.
fn test(ctx: &mut ParserContext<'_>, read: ReadValue) -> R<Record> {
    let ReadValue::Block(mut read) = read else {
        return Err(ParseError::Unexpected {
            message: "the persistence grammar misread a test".to_string(),
            source_info: SourceInfo::new("", 0, 0, 0, 0),
        });
    };
    let at = read.source_info.clone();
    let id = read.keyword.text.to_string();
    let batches = values(take(&mut read, "testBatches", &at)?)?
        .into_iter()
        .enumerate()
        .map(|(index, value)| test_batch(ctx, index, value))
        .collect::<R<Vec<_>>>()?;
    let from_service_output = take_optional(&mut read, "isTestDataFromServiceOutput", &at)?
        .map(identifier)
        .transpose()?
        .is_none_or(|value| value.eq_ignore_ascii_case("true"));
    let graph_fetch_path = take_optional(&mut read, "graphFetchPath", &at)?
        .map(path_of)
        .transpose()?;
    Ok(Record::new("test")
        .at(&at)
        .with("id", id)
        .with("testBatches", batches)
        .with("isTestDataFromServiceOutput", from_service_output)
        .with_some("graphFetchPath", graph_fetch_path))
}

/// The walker's `visitTestBatch`: its index in its test is its id.
fn test_batch(ctx: &mut ParserContext<'_>, index: usize, read: ReadValue) -> R<Record> {
    let ReadValue::Block(mut read) = read else {
        return Err(ParseError::Unexpected {
            message: "the persistence grammar misread a test batch".to_string(),
            source_info: SourceInfo::new("", 0, 0, 0, 0),
        });
    };
    let at = read.source_info.clone();
    let Some(data) = take_optional(&mut read, "data", &at)? else {
        return Err(ParseError::Unexpected {
            message: "TestData cannot be empty or null within Persistence TestBatch".to_string(),
            source_info: at,
        });
    };
    let data_at = data.source_info.clone();
    let mut data = block(data)?;
    let connection = take(&mut data, "connection", &data_at)?;
    let connection_at = connection.source_info.clone();
    let ReadValue::TypedIsland(island) = connection.value else {
        return Err(ParseError::Unexpected {
            message: "the persistence grammar misread test data".to_string(),
            source_info: connection_at,
        });
    };
    let embedded = ctx.parse_embedded_data_island(&island)?;
    let test_data = Record::untyped().at(&data_at).with(
        "connection",
        Record::untyped()
            .at(&connection_at)
            .with("data", Value::EmbeddedData(Box::new(embedded))),
    );
    let assertions = values(take(&mut read, "asserts", &at)?)?
        .into_iter()
        .map(|value| match value {
            ReadValue::Named(id, island, _) => match *island {
                ReadValue::TypedIsland(island) => {
                    Ok((id.value, ctx.parse_test_assertion(&island)?))
                }
                _ => Err(ParseError::Unexpected {
                    message: "the persistence grammar misread an assertion".to_string(),
                    source_info: at.clone(),
                }),
            },
            _ => Err(ParseError::Unexpected {
                message: "the persistence grammar misread an assertion".to_string(),
                source_info: at.clone(),
            }),
        })
        .collect::<R<Vec<_>>>()?;
    Ok(Record::untyped()
        .at(&at)
        .with("id", read.keyword.text.to_string())
        .with(
            "batchId",
            Value::Integer(i64::try_from(index).unwrap_or(i64::MAX)),
        )
        .with("testData", test_data)
        .with("assertions", Value::Assertions(assertions)))
}

// ── Persisters (the grammar's first version) ─────────────────────────────

/// The walker's `visitPersister`.
fn persister(field: ReadField) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    let sink = sink(take(&mut read, "sink", &at)?)?;
    if read.keyword.text == "Streaming" {
        return Ok(Record::new("streamingPersister").at(&at).with("sink", sink));
    }
    let shape = target_shape(take(&mut read, "targetShape", &at)?)?;
    let mode = ingest_mode(take(&mut read, "ingestMode", &at)?)?;
    Ok(Record::new("batchPersister")
        .at(&at)
        .with("sink", sink)
        .with("targetShape", shape)
        .with("ingestMode", mode))
}

/// The walker's `visitSink`.
fn sink(field: ReadField) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    if read.keyword.text == "Relational" {
        let database = take(&mut read, "database", &at)?;
        let database_at = database.source_info.clone();
        return Ok(Record::new("relationalSink").at(&at).with(
            "database",
            pointer(Some("STORE"), path(database)?, &database_at),
        ));
    }
    let binding = take(&mut read, "binding", &at)?;
    let binding_at = binding.source_info.clone();
    Ok(Record::new("objectStorageSink")
        .at(&at)
        .with("binding", pointer(None, path(binding)?, &binding_at)))
}

/// The walker's `visitTargetShape`.
fn target_shape(field: ReadField) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    let model_class = path(take(&mut read, "modelClass", &at)?)?;
    if read.keyword.text == "Flat" {
        let target_name = string(take(&mut read, "targetName", &at)?)?;
        let partition_fields = partition_fields(&mut read, &at)?;
        let strategy =
            deduplication_strategy(take_optional(&mut read, "deduplicationStrategy", &at)?)?;
        return Ok(Record::new("flatTarget")
            .at(&at)
            .with("modelClass", model_class)
            .with("targetName", target_name)
            .with("partitionFields", partition_fields)
            .with("deduplicationStrategy", strategy));
    }
    let scope = identifier(take(&mut read, "transactionScope", &at)?)?;
    let parts = values(take(&mut read, "parts", &at)?)?
        .into_iter()
        .map(|value| {
            let ReadValue::Block(mut part) = value else {
                return Err(ParseError::Unexpected {
                    message: "the persistence grammar misread a part".to_string(),
                    source_info: at.clone(),
                });
            };
            let part_at = part.source_info.clone();
            let property = identifier(take(&mut part, "modelProperty", &part_at)?)?;
            let target_name = string(take(&mut part, "targetName", &part_at)?)?;
            let partition_fields = partition_fields(&mut part, &part_at)?;
            let strategy = deduplication_strategy(take_optional(
                &mut part,
                "deduplicationStrategy",
                &part_at,
            )?)?;
            Ok(Record::untyped()
                .at(&part_at)
                .with("modelProperty", property)
                .with("targetName", target_name)
                .with("partitionFields", partition_fields)
                .with("deduplicationStrategy", strategy))
        })
        .collect::<R<Vec<_>>>()?;
    Ok(Record::new("multiFlatTarget")
        .at(&at)
        .with("modelClass", model_class)
        .with("transactionScope", scope)
        .with("parts", parts))
}

/// A target's partition fields, names, empty without.
fn partition_fields(read: &mut ReadBlock, at: &SourceInfo) -> R<Vec<String>> {
    take_optional(read, "partitionFields", at)?
        .map(|field| {
            values(field)?
                .into_iter()
                .map(|value| match value {
                    ReadValue::Identifier(name) => Ok(name.value.to_string()),
                    _ => Err(ParseError::Unsupported {
                        message: "the engine fails on a path where a name is expected".to_string(),
                        source_info: at.clone(),
                    }),
                })
                .collect()
        })
        .transpose()
        .map(Option::unwrap_or_default)
}

/// The walker's `visitDeduplicationStrategy`: none without.
fn deduplication_strategy(field: Option<ReadField>) -> R<Record> {
    let Some(field) = field else {
        return Ok(Record::new("noDeduplicationStrategy"));
    };
    let mut read = block(field)?;
    let at = read.source_info.clone();
    Ok(match read.keyword.text.as_str() {
        "None" => Record::new("noDeduplicationStrategy"),
        "AnyVersion" => Record::new("anyVersionDeduplicationStrategy"),
        "MaxVersion" => {
            let version = name_of(take(&mut read, "versionField", &at)?)?;
            Record::new("maxVersionDeduplicationStrategy")
                .at(&at)
                .with("versionField", version)
        }
        _ => {
            let name = string(take(&mut read, "duplicateCountName", &at)?)?;
            Record::new("duplicateCountDeduplicationStrategy")
                .at(&at)
                .with("duplicateCountName", name)
        }
    })
}

/// The walker's `visitIngestMode`: each mode's fields, in the walker's order.
fn ingest_mode(field: ReadField) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    let (kind, fields): (&'static str, &[&'static str]) = match read.keyword.text.as_str() {
        "NontemporalSnapshot" => ("nontemporalSnapshot", &["auditing"]),
        "UnitemporalSnapshot" => ("unitemporalSnapshot", &["transactionMilestoning"]),
        "BitemporalSnapshot" => (
            "bitemporalSnapshot",
            &["transactionMilestoning", "validityMilestoning"],
        ),
        "NontemporalDelta" => ("nontemporalDelta", &["mergeStrategy", "auditing"]),
        "UnitemporalDelta" => (
            "unitemporalDelta",
            &["mergeStrategy", "transactionMilestoning"],
        ),
        "BitemporalDelta" => (
            "bitemporalDelta",
            &[
                "mergeStrategy",
                "transactionMilestoning",
                "validityMilestoning",
            ],
        ),
        _ => ("appendOnly", &["auditing", "filterDuplicates"]),
    };
    let mut mode = Record::new(kind).at(&at);
    for name in fields {
        let field = take(&mut read, name, &at)?;
        let value: Value = match *name {
            "auditing" => auditing(field)?.into(),
            "transactionMilestoning" => transaction_milestoning(field)?.into(),
            "validityMilestoning" => validity_milestoning(field)?.into(),
            "mergeStrategy" => merge_strategy(field)?.into(),
            _ => match &field.value {
                ReadValue::Boolean(value) => Value::Bool(*value),
                _ => return Err(misread(&field)),
            },
        };
        mode = mode.with(name, value);
    }
    Ok(mode)
}

/// The walker's `visitAuditing`.
fn auditing(field: ReadField) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    if read.keyword.text == "None" {
        return Ok(Record::new("noAuditing"));
    }
    let name = string(take(&mut read, "dateTimeName", &at)?)?;
    Ok(Record::new("dateTimeAuditing")
        .at(&at)
        .with("dateTimeName", name))
}

/// The walker's `visitTransactionMilestoning`.
fn transaction_milestoning(field: ReadField) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    let (kind, names): (&'static str, &[&'static str]) = match read.keyword.text.as_str() {
        "BatchId" => (
            "batchIdTransactionMilestoning",
            &["batchIdInName", "batchIdOutName"],
        ),
        "DateTime" => (
            "dateTimeTransactionMilestoning",
            &["dateTimeInName", "dateTimeOutName"],
        ),
        _ => (
            "batchIdAndDateTimeTransactionMilestoning",
            &[
                "batchIdInName",
                "batchIdOutName",
                "dateTimeInName",
                "dateTimeOutName",
            ],
        ),
    };
    let mut milestoning = Record::new(kind).at(&at);
    for name in names {
        milestoning = milestoning.with(name, string(take(&mut read, name, &at)?)?);
    }
    if read.keyword.text != "BatchId" {
        let derivation = take_optional(&mut read, "derivation", &at)?
            .map(|field| {
                derivation(
                    field,
                    &[
                        (
                            "SourceSpecifiesInDateTime",
                            "sourceSpecifiesInDateTime",
                            &["sourceDateTimeInField"],
                        ),
                        (
                            "SourceSpecifiesInAndOutDateTime",
                            "sourceSpecifiesInAndOutDateTime",
                            &["sourceDateTimeInField", "sourceDateTimeOutField"],
                        ),
                    ],
                )
            })
            .transpose()?;
        milestoning = milestoning.with_some("derivation", derivation);
    }
    Ok(milestoning)
}

/// A derivation: its kind by its keyword, its fields, names, all required.
fn derivation(field: ReadField, kinds: &[(&str, &'static str, &[&'static str])]) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    let Some((_, kind, names)) = kinds
        .iter()
        .find(|(keyword, _, _)| read.keyword.text == *keyword)
    else {
        return Err(ParseError::Unexpected {
            message: "the persistence grammar misread a derivation".to_string(),
            source_info: at,
        });
    };
    let mut derivation = Record::new(kind).at(&at);
    for name in *names {
        derivation = derivation.with(name, identifier(take(&mut read, name, &at)?)?);
    }
    Ok(derivation)
}

/// The walker's `visitValidityMilestoning`: a derivation required.
fn validity_milestoning(field: ReadField) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    let from = string(take(&mut read, "dateTimeFromName", &at)?)?;
    let thru = string(take(&mut read, "dateTimeThruName", &at)?)?;
    let derivation = derivation(
        take(&mut read, "derivation", &at)?,
        &[
            (
                "SourceSpecifiesFromDateTime",
                "sourceSpecifiesFromDateTime",
                &["sourceDateTimeFromField"],
            ),
            (
                "SourceSpecifiesFromAndThruDateTime",
                "sourceSpecifiesFromAndThruDateTime",
                &["sourceDateTimeFromField", "sourceDateTimeThruField"],
            ),
        ],
    )?;
    Ok(Record::new("dateTimeValidityMilestoning")
        .at(&at)
        .with("dateTimeFromName", from)
        .with("dateTimeThruName", thru)
        .with("derivation", derivation))
}

/// The walker's `visitMergeStrategy`: a delete indicator unpositioned.
fn merge_strategy(field: ReadField) -> R<Record> {
    let mut read = block(field)?;
    let at = read.source_info.clone();
    if read.keyword.text == "NoDeletes" {
        return Ok(Record::new("noDeletesMergeStrategy"));
    }
    let delete_field = identifier(take(&mut read, "deleteField", &at)?)?;
    let delete_values = strings(take(&mut read, "deleteValues", &at)?)?;
    Ok(Record::new("deleteIndicatorMergeStrategy")
        .with("deleteField", delete_field)
        .with("deleteValues", delete_values))
}
