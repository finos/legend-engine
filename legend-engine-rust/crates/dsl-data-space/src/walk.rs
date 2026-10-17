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

//! The walk (the engine's `DataSpaceParseTreeWalker`): each data space's
//! fields checked and built in the walker's order, into records shaped as
//! the engine's protocol.

use legend_pure_parser_ast::data::EmbeddedData;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::test_assertion::TestAssertion;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{ReadBlock, ReadField, ReadValue};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::string_text;
use legend_pure_parser_protocol::v1::record::{Record, Value};

type R<T> = Result<T, ParseError>;

/// The engine's coverage regions.
const REGIONS: [&str; 4] = ["APAC", "EMEA", "LAMR", "NAMR"];

/// The engine's delivery frequencies.
const FREQUENCIES: [&str; 7] = [
    "INTRADAY",
    "DAILY",
    "WEEKLY",
    "MONTHLY",
    "QUARTERLY",
    "YEARLY",
    "ON_DEMAND",
];

/// The type of the embedded data sample values are.
const RELATION: &str = "Relation";

/// A string field's value, if any.
fn optional_string(read: &mut ReadBlock, name: &'static str, at: &SourceInfo) -> R<Option<String>> {
    read.take_optional(name, at)?
        .map(ReadField::string)
        .transpose()
}

/// A pointer to an element, of `element_type` if any, positioned on `at`.
fn pointer(element_type: Option<&str>, path: String, at: &SourceInfo) -> Record {
    let pointer = Record::untyped();
    let pointer = match element_type {
        Some(element_type) => pointer.with("type", element_type.to_string()),
        None => pointer,
    };
    pointer.with("path", path).at(at)
}

/// A field's value, a list.
fn list(field: ReadField) -> R<Vec<ReadValue>> {
    field.values()
}

/// A value of a list the grammar reads blocks into.
fn block_of(value: ReadValue, at: &SourceInfo) -> R<ReadBlock> {
    match value {
        ReadValue::Block(block) => Ok(block),
        _ => Err(ParseError::Unexpected {
            message: "the data space grammar misread a block".to_string(),
            source_info: at.clone(),
        }),
    }
}

/// The strings of a list.
fn strings(field: ReadField) -> R<Vec<String>> {
    let at = field.source_info.clone();
    list(field)?
        .into_iter()
        .map(|value| match value {
            ReadValue::String(token) => string_text(&token),
            _ => Err(ParseError::Unexpected {
                message: "the data space grammar misread a string".to_string(),
                source_info: at.clone(),
            }),
        })
        .collect()
}

/// The walker's `visitDataSpace`, after its annotations: each field in the
/// walker's order, the data space's own record without its package, name,
/// stereotypes and tagged values.
pub(crate) fn data_space(ctx: &mut ParserContext<'_>, mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let contexts = match read.take_optional("executionContexts", &at)? {
        Some(field) => {
            let field_at = field.source_info.clone();
            list(field)?
                .into_iter()
                .map(|value| execution_context(ctx, block_of(value, &field_at)?))
                .collect::<R<Vec<_>>>()?
        }
        None => Vec::new(),
    };
    let default_context = optional_string(&mut read, "defaultExecutionContext", &at)?;
    let title = optional_string(&mut read, "title", &at)?;
    let description = optional_string(&mut read, "description", &at)?;
    let elements = read
        .take_optional("elements", &at)?
        .map(|field| {
            let field_at = field.source_info.clone();
            list(field)?
                .into_iter()
                .map(|value| element_pointer(value, &field_at))
                .collect::<R<Vec<_>>>()
        })
        .transpose()?;
    let executables = read
        .take_optional("executables", &at)?
        .map(|field| {
            let field_at = field.source_info.clone();
            list(field)?
                .into_iter()
                .map(|value| executable(ctx, block_of(value, &field_at)?))
                .collect::<R<Vec<_>>>()
        })
        .transpose()?;
    let mut diagrams = read
        .take_optional("diagrams", &at)?
        .map(|field| {
            let field_at = field.source_info.clone();
            list(field)?
                .into_iter()
                .map(|value| diagram(block_of(value, &field_at)?))
                .collect::<R<Vec<_>>>()
        })
        .transpose()?;
    // Deprecated for `diagrams`, which they join, untitled.
    if let Some(featured) = read.take_optional("featuredDiagrams", &at)? {
        let featured = list(featured)?
            .into_iter()
            .map(|value| match value {
                ReadValue::Path(name) => Ok(Record::untyped()
                    .at(&name.source_info)
                    .with("title", String::new())
                    .with("diagram", pointer(None, name.path, &name.source_info))),
                _ => Err(ParseError::Unexpected {
                    message: "the data space grammar misread a diagram".to_string(),
                    source_info: at.clone(),
                }),
            })
            .collect::<R<Vec<_>>>()?;
        diagrams.get_or_insert_with(Vec::new).extend(featured);
    }
    let support = read
        .take_optional("supportInfo", &at)?
        .map(|field| support_info(field.block()?))
        .transpose()?;
    let metadata = read
        .take_optional("operationalMetadata", &at)?
        .map(operational_metadata)
        .transpose()?;
    Ok(Record::new("dataSpace")
        .at(&at)
        .with("executionContexts", contexts)
        .with_some("defaultExecutionContext", default_context)
        .with_some("title", title)
        .with_some("description", description)
        .with_some("elements", elements)
        .with_some("executables", executables)
        .with_some("diagrams", diagrams)
        .with_some("supportInfo", support)
        .with_some("operationalMetadata", metadata))
}

/// The walker's `visitDataSpaceExecutionContext`: a mapping or a mapping
/// provider, not both.
fn execution_context(ctx: &mut ParserContext<'_>, mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let name = read.take("name", &at)?.string()?;
    let title = optional_string(&mut read, "title", &at)?;
    let description = optional_string(&mut read, "description", &at)?;
    let mapping = read
        .take_optional("mapping", &at)?
        .map(|field| {
            let field_at = field.source_info.clone();
            Ok::<_, ParseError>(pointer(Some("MAPPING"), field.path()?, &field_at))
        })
        .transpose()?;
    let provider = read
        .take_optional("mappingProvider", &at)?
        .map(|field| {
            let field_at = field.source_info.clone();
            let ReadValue::Pair(element, keys, _) = field.value else {
                return Err(ParseError::Unexpected {
                    message: "the data space grammar misread a mapping provider".to_string(),
                    source_info: field_at,
                });
            };
            let (ReadValue::Path(element), ReadValue::List(keys)) = (*element, *keys) else {
                return Err(ParseError::Unexpected {
                    message: "the data space grammar misread a mapping provider".to_string(),
                    source_info: field_at,
                });
            };
            let keys = keys
                .into_iter()
                .filter_map(|key| match key {
                    ReadValue::Identifier(name) => Some(name.value.to_string()),
                    _ => None,
                })
                .collect::<Vec<_>>();
            Ok(Record::untyped()
                .at(&field_at)
                .with("element", pointer(None, element.path, &element.source_info))
                .with("keys", keys))
        })
        .transpose()?;
    if mapping.is_some() == provider.is_some() {
        let message = if mapping.is_none() {
            "Data space execution context must define either 'mapping' or 'mappingProvider'"
        } else {
            "Data space execution context cannot define both 'mapping' and 'mappingProvider'"
        };
        return Err(ParseError::Unexpected {
            message: message.to_string(),
            source_info: at,
        });
    }
    let runtime = read
        .take_optional("defaultRuntime", &at)?
        .map(|field| {
            let field_at = field.source_info.clone();
            Ok::<_, ParseError>(pointer(Some("RUNTIME"), field.path()?, &field_at))
        })
        .transpose()?;
    let test_data = read
        .take_optional("testData", &at)?
        .map(|field| {
            let field_at = field.source_info.clone();
            let ReadValue::TypedIsland(island) = field.value else {
                return Err(ParseError::Unexpected {
                    message: "the data space grammar misread test data".to_string(),
                    source_info: field_at,
                });
            };
            let data = ctx.parse_embedded_data_island(&island)?;
            if !matches!(data.data, EmbeddedData::Reference(_)) {
                return Err(ParseError::Unexpected {
                    message: "Test data within a DataSpace must be created as a \
                              DataElementReference type, not an EmbeddedData type."
                        .to_string(),
                    source_info: field_at,
                });
            }
            Ok(Value::EmbeddedData(Box::new(data)))
        })
        .transpose()?;
    let mut context = Record::untyped()
        .at(&at)
        .with("name", name)
        .with_some("title", title)
        .with_some("description", description)
        .with_some("mapping", mapping)
        .with_some("mappingProvider", provider)
        .with_some("defaultRuntime", runtime);
    if let Some(test_data) = test_data {
        context = context.with("testData", test_data);
    }
    Ok(context)
}

/// `(-)? my::Element`: excluded when written with `-`.
fn element_pointer(value: ReadValue, at: &SourceInfo) -> R<Record> {
    let mut block = block_of(value, at)?;
    let block_at = block.source_info.clone();
    let path = block.take("path", &block_at)?.path()?;
    let pointer = Record::untyped().with("path", path);
    let pointer = if block.keyword.text == "-" {
        pointer.with("exclude", true)
    } else {
        pointer
    };
    Ok(pointer.at(&block_at))
}

/// The walker's `visitDataSpaceExecutable`: a template, with a query, or a
/// packageable element's.
fn executable(ctx: &mut ParserContext<'_>, mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let template = read.fields.iter().any(|field| field.name == "query");
    if !template && !read.fields.iter().any(|field| field.name == "executable") {
        return Err(ParseError::Unsupported {
            message: "the engine fails, without a position, on an executable with neither a \
                      query nor an executable"
                .to_string(),
            source_info: at,
        });
    }
    let id = if template {
        Some(id(read.take("id", &at)?)?)
    } else {
        read.take_optional("id", &at)?.map(id).transpose()?
    };
    let title = read.take("title", &at)?.string()?;
    let description = optional_string(&mut read, "description", &at)?;
    let key = optional_string(&mut read, "executionContextKey", &at)?;
    let executable = if template {
        let query = read.take("query", &at)?;
        let query_at = query.source_info.clone();
        let ReadValue::Expression(query) = query.value else {
            return Err(ParseError::Unexpected {
                message: "the data space grammar misread a query".to_string(),
                source_info: query_at,
            });
        };
        ("query", Value::Lambda(query))
    } else {
        let executable = read.take("executable", &at)?;
        let executable_at = executable.source_info.clone();
        let pointer = match executable.value {
            ReadValue::Text(text, _) => pointer(Some("FUNCTION"), text, &executable_at),
            ReadValue::Path(name) => pointer(None, name.path, &executable_at),
            _ => {
                return Err(ParseError::Unexpected {
                    message: "the data space grammar misread an executable".to_string(),
                    source_info: executable_at,
                });
            }
        };
        ("executable", Value::from(pointer))
    };
    let samples = sample_values(ctx, &mut read, &at)?;
    let kind = if template {
        "dataSpaceTemplateExecutable"
    } else {
        "dataSpacePackageableElementExecutable"
    };
    Ok(Record::new(kind)
        .at(&at)
        .with_some("id", id)
        .with("title", title)
        .with_some("description", description)
        .with_some("executionContextKey", key)
        .with(executable.0, executable.1)
        .with_some("sampleValues", samples))
}

/// An executable's id, as written.
fn id(field: ReadField) -> R<String> {
    match &field.value {
        ReadValue::String(token) => Ok(token.text.to_string()),
        _ => Err(field.misread()),
    }
}

/// An executable's sample values: a relation, `Relation #{ … }#`.
fn sample_values(
    ctx: &mut ParserContext<'_>,
    read: &mut ReadBlock,
    at: &SourceInfo,
) -> R<Option<Value>> {
    let Some(field) = read.take_optional("sampleValues", at)? else {
        return Ok(None);
    };
    let field_at = field.source_info.clone();
    let ReadValue::TypedIsland(island) = field.value else {
        return Err(ParseError::Unexpected {
            message: "the data space grammar misread sample values".to_string(),
            source_info: field_at,
        });
    };
    if island.type_name != RELATION {
        return Err(ParseError::Unexpected {
            message: format!(
                "Data space executable sampleValues must be a standalone Relation element (e.g. \
                 sampleValues: Relation #{{ ... }}#), got type '{}'",
                island.type_name
            ),
            source_info: island.source_info.clone(),
        });
    }
    match ctx.parse_test_assertion(&island)? {
        TestAssertion::EqualToRelation { expected, .. } => {
            Ok(Some(Value::RelationElement(Box::new(expected))))
        }
        _ => Err(ParseError::Unexpected {
            message: "the data space grammar misread sample values".to_string(),
            source_info: island.source_info.clone(),
        }),
    }
}

/// The walker's `visitDataSpaceDiagram`.
fn diagram(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let title = read.take("title", &at)?.string()?;
    let description = optional_string(&mut read, "description", &at)?;
    let path = read.take("diagram", &at)?;
    let path_at = path.source_info.clone();
    Ok(Record::untyped()
        .at(&at)
        .with("title", title)
        .with_some("description", description)
        .with("diagram", pointer(None, path.path()?, &path_at)))
}

/// The walker's `visitDataSpaceSupportInfo`.
fn support_info(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    match read.keyword.text.as_str() {
        "Combined" => {
            let url = optional_string(&mut read, "documentationUrl", &at)?;
            let emails = read
                .take_optional("emails", &at)?
                .map(strings)
                .transpose()?;
            let website = optional_string(&mut read, "website", &at)?;
            let faq = optional_string(&mut read, "faqUrl", &at)?;
            let support = optional_string(&mut read, "supportUrl", &at)?;
            Ok(Record::new("combined")
                .at(&at)
                .with_some("documentationUrl", url)
                .with_some("emails", emails)
                .with_some("website", website)
                .with_some("faqUrl", faq)
                .with_some("supportUrl", support))
        }
        "Email" => {
            let url = optional_string(&mut read, "documentationUrl", &at)?;
            let address = read.take("address", &at)?.string()?;
            Ok(Record::new("email")
                .at(&at)
                .with_some("documentationUrl", url)
                .with("address", address))
        }
        _ => {
            let mut link = |name: &'static str| -> R<Option<Record>> {
                read.take_optional(name, &at)?
                    .map(|field| self::link(field.block()?))
                    .transpose()
            };
            let documentation = link("documentation")?;
            let website = link("website")?;
            let faq = link("faqUrl")?;
            let support = link("supportUrl")?;
            let emails = read
                .take_optional("emails", &at)?
                .map(|field| {
                    list(field)?
                        .into_iter()
                        .map(|value| {
                            let mut email = block_of(value, &at)?;
                            let email_at = email.source_info.clone();
                            let title = email.take("title", &email_at)?.string()?;
                            let address = email.take("address", &email_at)?.string()?;
                            Ok(Record::untyped()
                                .at(&email_at)
                                .with("title", title)
                                .with("address", address))
                        })
                        .collect::<R<Vec<_>>>()
                })
                .transpose()?;
            let expertise = read
                .take_optional("expertise", &at)?
                .map(|field| {
                    list(field)?
                        .into_iter()
                        .map(|value| {
                            let mut expertise = block_of(value, &at)?;
                            let expertise_at = expertise.source_info.clone();
                            let description =
                                optional_string(&mut expertise, "description", &expertise_at)?;
                            let ids = expertise
                                .take_optional("expertIds", &expertise_at)?
                                .map(strings)
                                .transpose()?;
                            Ok(Record::untyped()
                                .at(&expertise_at)
                                .with_some("description", description)
                                .with_some("expertIds", ids))
                        })
                        .collect::<R<Vec<_>>>()
                })
                .transpose()?;
            Ok(Record::new("full")
                .at(&at)
                .with_some("documentation", documentation)
                .with_some("website", website)
                .with_some("faqUrl", faq)
                .with_some("supportUrl", support)
                .with_some("emails", emails)
                .with_some("expertise", expertise))
        }
    }
}

/// The walker's `visitDataSpaceLink`.
fn link(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let label = optional_string(&mut read, "label", &at)?;
    let url = read.take("url", &at)?.string()?;
    Ok(Record::untyped()
        .at(&at)
        .with_some("label", label)
        .with("url", url))
}

/// The walker's `visitDataSpaceOperationalMetadata`, positioned on its
/// field: each region and the frequency one the engine knows.
fn operational_metadata(field: ReadField) -> R<Record> {
    let at = field.source_info.clone();
    let mut read = field.block()?;
    let regions = match read.take_optional("coverageRegions", &at)? {
        Some(field) => list(field)?
            .into_iter()
            .map(|value| match value {
                ReadValue::Identifier(name) if REGIONS.contains(&name.value.as_str()) => {
                    Ok(name.value.to_string())
                }
                ReadValue::Identifier(name) => Err(ParseError::Unexpected {
                    message: format!(
                        "Unknown coverage region '{}'. Valid values: [{}]",
                        name.value,
                        REGIONS.join(", ")
                    ),
                    source_info: name.source_info,
                }),
                _ => Err(ParseError::Unexpected {
                    message: "the data space grammar misread a region".to_string(),
                    source_info: at.clone(),
                }),
            })
            .collect::<R<Vec<_>>>()?,
        None => Vec::new(),
    };
    let frequency = read
        .take_optional("updateFrequency", &at)?
        .map(|field| {
            let misread = field.misread();
            match field.value {
                ReadValue::Identifier(name) if FREQUENCIES.contains(&name.value.as_str()) => {
                    Ok(name.value.to_string())
                }
                ReadValue::Identifier(name) => Err(ParseError::Unexpected {
                    message: format!(
                        "Unknown update frequency '{}'. Valid values: [{}]",
                        name.value,
                        FREQUENCIES.join(", ")
                    ),
                    source_info: name.source_info,
                }),
                _ => Err(misread),
            }
        })
        .transpose()?;
    Ok(Record::untyped()
        .at(&at)
        .with("coverageRegions", regions)
        .with_some("updateFrequency", frequency))
}
