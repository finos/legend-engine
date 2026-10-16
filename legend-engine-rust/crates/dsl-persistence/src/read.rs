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

//! The section's grammar, `PersistenceParserGrammar`: what it reads, before
//! the walk checks it, as blocks of fields (see
//! [`legend_pure_parser_parser::blocks`]). The grammar's `identifier`
//! includes `true` and `false`, which its lexer reads as keywords. Parts the
//! walker parses with another grammar (a trigger's, platform's or target's
//! island, an embedded connection, embedded data, a test assertion) are read
//! but not parsed.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{
    ReadBlock, ReadField, ReadValue, bare_block, braces, choice, is_keyword, keyword_alone,
    keyword_block, navigation_path, primitive_value_text, simple, some_values, specification,
    string_value, unended, values,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::field_start;
use legend_pure_parser_parser::words::{Name, QualifiedName, is_word_kind, unexpected};

type R<T> = Result<T, ParseError>;

/// An element, `Persistence my::P { … }` or `PersistenceContext my::C { … }`.
pub(crate) struct ReadElement {
    /// `Persistence` or `PersistenceContext`, and the element's fields.
    pub body: ReadBlock,
    /// The element's path.
    pub path: QualifiedName,
}

// ── Names, as the persistence grammar's `identifier` reads them ──────────

/// The grammar's `identifier`: a word, `true` or `false` included, or a
/// `'quoted'` name.
pub fn identifier(ctx: &mut ParserContext<'_>) -> R<Name> {
    let token = ctx.cursor().peek().clone();
    if matches!(token.kind, TokenKind::True | TokenKind::False) {
        ctx.cursor().advance();
        return Ok(Name {
            text: token.text.clone(),
            value: token.text,
            source_info: token.source_info,
        });
    }
    legend_pure_parser_parser::words::identifier(ctx)
}

/// The grammar's `qualifiedName`: identifiers separated by `::`.
pub fn qualified_name(ctx: &mut ParserContext<'_>) -> R<QualifiedName> {
    let first = identifier(ctx)?;
    let mut path = first.value.to_string();
    let mut text = first.text.to_string();
    let mut has_package = false;
    while ctx.cursor().eat(TokenKind::PathSep) {
        let part = identifier(ctx)?;
        path.push_str("::");
        path.push_str(&part.value);
        text.push_str("::");
        text.push_str(&part.text);
        has_package = true;
    }
    Ok(QualifiedName {
        path,
        text,
        has_package,
        source_info: ctx.cursor().span_since(&first.source_info),
    })
}

/// A name, `true` or `false` included.
fn identifier_value(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    Ok(ReadValue::Identifier(identifier(ctx)?))
}

/// A qualified name whose parts may be `true` or `false`.
fn path_value(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    Ok(ReadValue::Path(qualified_name(ctx)?))
}

/// `Type` or `Type #{ … }#`: a trigger, a platform or a target.
fn type_specification(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    specification(ctx, identifier)
}

// ── Values ────────────────────────────────────────────────────────────────

/// `serviceOutputValue`: a name, or a navigation path.
fn output_value(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    if ctx.cursor().check(TokenKind::HashSlash) {
        return Ok(ReadValue::NavigationPath(Box::new(navigation_path(ctx)?)));
    }
    identifier_value(ctx)
}

/// A connection: a path, or one of the element's own, `#{ Type { … } }#`.
fn connection(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    if ctx.cursor().check(TokenKind::IslandOpen) {
        return Ok(ReadValue::Island(ctx.read_island()?));
    }
    path_value(ctx)
}

/// Whether a qualified name, then `.`, is next: an enum value.
fn at_enum_value(ctx: &mut ParserContext<'_>) -> bool {
    let cursor = ctx.cursor();
    let mut next = 1;
    while cursor.peek_kind_at(next) == TokenKind::PathSep {
        next += 2;
    }
    cursor.peek_kind_at(next) == TokenKind::Dot
}

/// `name = value`: a primitive value, a connection's path, or a connection
/// of the context's own.
fn service_parameter(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let name = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Equals)?;
    let token = ctx.cursor().peek().clone();
    let value = if token.kind == TokenKind::IslandOpen {
        ReadValue::Island(ctx.read_island()?)
    } else if is_word_kind(token.kind) && !at_enum_value(ctx) && token.text != "toBytes" {
        path_value(ctx)?
    } else {
        primitive_value_text(ctx)?
    };
    Ok(ReadValue::Named(
        name.clone(),
        Box::new(value),
        ctx.cursor().span_since(&name.source_info),
    ))
}

// ── The elements ──────────────────────────────────────────────────────────

/// `PersistenceContext my::C { … }` or `Persistence my::P { … }`.
pub(crate) fn element(ctx: &mut ParserContext<'_>) -> R<ReadElement> {
    let keyword = ctx.cursor().peek().clone();
    let context = is_keyword(&keyword, "PersistenceContext");
    if !context && !is_keyword(&keyword, "Persistence") {
        return Err(unexpected(&keyword));
    }
    ctx.cursor().advance();
    let path = qualified_name(ctx)?;
    let (_, fields) = if context {
        braces(ctx, context_field)?
    } else {
        braces(ctx, persistence_field)?
    };
    Ok(ReadElement {
        body: ReadBlock {
            source_info: ctx.cursor().span_since(&keyword.source_info),
            keyword,
            fields,
        },
        path,
    })
}

fn context_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "persistence" => simple(ctx, "persistence", path_value),
        "platform" => simple(ctx, "platform", type_specification),
        "serviceParameters" => simple(ctx, "serviceParameters", |ctx| {
            values(ctx, service_parameter)
        }),
        "sinkConnection" => simple(ctx, "sinkConnection", connection),
        _ => Ok(None),
    }
}

fn persistence_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "doc" => simple(ctx, "doc", string_value),
        "trigger" => simple(ctx, "trigger", type_specification),
        "service" => simple(ctx, "service", path_value),
        "serviceOutputTargets" => simple(ctx, "serviceOutputTargets", |ctx| {
            some_values(ctx, output_target)
        }),
        "persister" => unended(ctx, "persister", |ctx| {
            choice(
                ctx,
                &mut [
                    ("Streaming", &mut |ctx| keyword_block(ctx, streaming_field)),
                    ("Batch", &mut |ctx| keyword_block(ctx, batch_field)),
                ],
            )
        }),
        "notifier" => unended(ctx, "notifier", |ctx| {
            Ok(ReadValue::Block(bare_block(ctx, |ctx, name| match name {
                "notifyees" => simple(ctx, "notifyees", |ctx| some_values(ctx, notifyee)),
                _ => Ok(None),
            })?))
        }),
        "tests" => unended(ctx, "tests", |ctx| values(ctx, test)),
        _ => Ok(None),
    }
}

// ── Notifier ──────────────────────────────────────────────────────────────

fn notifyee(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    choice(
        ctx,
        &mut [
            ("Email", &mut |ctx| {
                keyword_block(ctx, |ctx, name| match name {
                    "address" => simple(ctx, "address", string_value),
                    _ => Ok(None),
                })
            }),
            ("PagerDuty", &mut |ctx| {
                keyword_block(ctx, |ctx, name| match name {
                    "url" => simple(ctx, "url", string_value),
                    _ => Ok(None),
                })
            }),
        ],
    )
}

// ── Service output targets ───────────────────────────────────────────────

/// `output -> target`.
fn output_target(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let start = ctx.cursor().current_source_info();
    let output = service_output(ctx)?;
    ctx.cursor().expect(TokenKind::Arrow)?;
    let target = if ctx.cursor().check(TokenKind::LBrace) {
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::RBrace)?;
        ReadValue::Empty
    } else {
        type_specification(ctx)?
    };
    Ok(ReadValue::Pair(
        Box::new(ReadValue::Block(output)),
        Box::new(target),
        ctx.cursor().span_since(&start),
    ))
}

/// `#/Type/property# { … }` or `TDS { … }`: a graph fetch's output keeps its
/// path as a field, `path`.
fn service_output(ctx: &mut ParserContext<'_>) -> R<ReadBlock> {
    let keyword = ctx.cursor().peek().clone();
    if is_keyword(&keyword, "TDS") {
        return keyword_block(ctx, dataset_field);
    }
    if keyword.kind != TokenKind::HashSlash {
        return Err(unexpected(&keyword));
    }
    let path = navigation_path(ctx)?;
    let path_span = ctx.cursor().span_since(&keyword.source_info);
    let (_, mut fields) = braces(ctx, dataset_field)?;
    fields.insert(
        0,
        ReadField {
            name: "path",
            value: ReadValue::NavigationPath(Box::new(path)),
            source_info: path_span,
        },
    );
    Ok(ReadBlock {
        source_info: ctx.cursor().span_since(&keyword.source_info),
        keyword,
        fields,
    })
}

fn dataset_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "keys" => unended(ctx, "keys", |ctx| values(ctx, output_value)),
        "deduplication" => unended(ctx, "deduplication", |ctx| {
            choice(
                ctx,
                &mut [
                    ("None", &mut |ctx| keyword_alone(ctx, true)),
                    ("AnyVersion", &mut |ctx| keyword_alone(ctx, true)),
                    ("MaxVersion", &mut |ctx| keyword_block(ctx, version_field)),
                ],
            )
        }),
        "datasetType" => unended(ctx, "datasetType", |ctx| {
            choice(
                ctx,
                &mut [
                    ("Snapshot", &mut |ctx| keyword_block(ctx, snapshot_field)),
                    ("Delta", &mut |ctx| keyword_block(ctx, delta_field)),
                ],
            )
        }),
        _ => Ok(None),
    }
}

fn version_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "versionField" => simple(ctx, "versionField", output_value),
        _ => Ok(None),
    }
}

fn snapshot_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "partitioning" => unended(ctx, "partitioning", |ctx| {
            choice(
                ctx,
                &mut [
                    ("None", &mut |ctx| {
                        keyword_block(ctx, |ctx, name| match name {
                            "emptyDatasetHandling" => unended(ctx, "emptyDatasetHandling", |ctx| {
                                choice(
                                    ctx,
                                    &mut [
                                        ("NoOp", &mut |ctx| keyword_alone(ctx, true)),
                                        ("DeleteTargetData", &mut |ctx| keyword_alone(ctx, true)),
                                    ],
                                )
                            }),
                            _ => Ok(None),
                        })
                    }),
                    ("FieldBased", &mut |ctx| {
                        keyword_block(ctx, partition_fields)
                    }),
                ],
            )
        }),
        _ => Ok(None),
    }
}

fn partition_fields(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "partitionFields" => simple(ctx, "partitionFields", |ctx| values(ctx, output_value)),
        _ => Ok(None),
    }
}

fn delta_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "actionIndicator" => unended(ctx, "actionIndicator", |ctx| {
            choice(
                ctx,
                &mut [
                    ("None", &mut |ctx| keyword_alone(ctx, true)),
                    ("DeleteIndicator", &mut |ctx| {
                        keyword_block(ctx, |ctx, name| match name {
                            "deleteField" => simple(ctx, "deleteField", output_value),
                            "deleteValues" => {
                                simple(ctx, "deleteValues", |ctx| values(ctx, identifier_value))
                            }
                            _ => Ok(None),
                        })
                    }),
                ],
            )
        }),
        _ => Ok(None),
    }
}

// ── Tests ─────────────────────────────────────────────────────────────────

/// `id: { testBatches: [ … ] (isTestDataFromServiceOutput: …;)? (graphFetchPath: …;)? … }`:
/// the two optional fields only after test batches.
fn test(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let id = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    ctx.cursor().expect(TokenKind::LBrace)?;
    let mut fields = Vec::new();
    let mut push = |ctx: &mut ParserContext<'_>,
                    start: &SourceInfo,
                    read: Option<(&'static str, ReadValue)>| {
        if let Some((name, value)) = read {
            fields.push(ReadField {
                name,
                value,
                source_info: ctx.cursor().span_since(start),
            });
        }
    };
    while is_keyword(ctx.cursor().peek(), "testBatches") {
        let start = ctx.cursor().current_source_info();
        let read = unended(ctx, "testBatches", |ctx| values(ctx, test_batch))?;
        push(ctx, &start, read);
        if is_keyword(ctx.cursor().peek(), "isTestDataFromServiceOutput") {
            let start = ctx.cursor().current_source_info();
            let read = simple(ctx, "isTestDataFromServiceOutput", identifier_value)?;
            push(ctx, &start, read);
        }
        if is_keyword(ctx.cursor().peek(), "graphFetchPath") {
            let start = ctx.cursor().current_source_info();
            let read = simple(ctx, "graphFetchPath", |ctx| {
                Ok(ReadValue::NavigationPath(Box::new(navigation_path(ctx)?)))
            })?;
            push(ctx, &start, read);
        }
    }
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ReadValue::Block(ReadBlock {
        source_info: ctx.cursor().span_since(&id.source_info),
        keyword: Token {
            kind: TokenKind::Identifier,
            text: id.value,
            source_info: id.source_info,
        },
        fields,
    }))
}

/// `id: { data: { connection: { Type #{ … }# } } asserts: [ … ] }`.
fn test_batch(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let id = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let (_, fields) = braces(ctx, |ctx, name| match name {
        // `data: { connection: { Type #{ … }# } }`: one connection, always.
        "data" => unended(ctx, "data", |ctx| {
            let open = ctx.cursor().expect(TokenKind::LBrace)?;
            let connection = ctx.cursor().peek().clone();
            if !is_keyword(&connection, "connection") {
                return Err(unexpected(&connection));
            }
            field_start(ctx)?;
            ctx.cursor().expect(TokenKind::LBrace)?;
            let data = ctx.read_typed_island()?;
            ctx.cursor().expect(TokenKind::RBrace)?;
            let connection_at = ctx.cursor().span_since(&connection.source_info);
            ctx.cursor().expect(TokenKind::RBrace)?;
            Ok(ReadValue::Block(ReadBlock {
                source_info: ctx.cursor().span_since(&open.source_info),
                keyword: open,
                fields: vec![ReadField {
                    name: "connection",
                    value: ReadValue::TypedIsland(data),
                    source_info: connection_at,
                }],
            }))
        }),
        "asserts" => unended(ctx, "asserts", |ctx| {
            values(ctx, |ctx| {
                let id = identifier(ctx)?;
                ctx.cursor().expect(TokenKind::Colon)?;
                let island = ctx.read_typed_island()?;
                let at = ctx.cursor().span_since(&id.source_info);
                Ok(ReadValue::Named(
                    id,
                    Box::new(ReadValue::TypedIsland(island)),
                    at,
                ))
            })
        }),
        _ => Ok(None),
    })?;
    Ok(ReadValue::Block(ReadBlock {
        source_info: ctx.cursor().span_since(&id.source_info),
        keyword: Token {
            kind: TokenKind::Identifier,
            text: id.value,
            source_info: id.source_info,
        },
        fields,
    }))
}

// ── Persisters (the grammar's first version) ─────────────────────────────

fn streaming_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "sink" => unended(ctx, "sink", sink),
        _ => Ok(None),
    }
}

fn batch_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "sink" => unended(ctx, "sink", sink),
        "targetShape" => unended(ctx, "targetShape", |ctx| {
            choice(
                ctx,
                &mut [
                    ("MultiFlat", &mut |ctx| keyword_block(ctx, multi_flat_field)),
                    ("Flat", &mut |ctx| keyword_block(ctx, flat_field)),
                ],
            )
        }),
        "ingestMode" => unended(ctx, "ingestMode", ingest_mode),
        _ => Ok(None),
    }
}

fn sink(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    choice(
        ctx,
        &mut [
            ("Relational", &mut |ctx| {
                keyword_block(ctx, |ctx, name| match name {
                    "database" => simple(ctx, "database", path_value),
                    _ => Ok(None),
                })
            }),
            ("ObjectStorage", &mut |ctx| {
                keyword_block(ctx, |ctx, name| match name {
                    "binding" => simple(ctx, "binding", path_value),
                    _ => Ok(None),
                })
            }),
        ],
    )
}

fn multi_flat_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "modelClass" => simple(ctx, "modelClass", path_value),
        "transactionScope" => simple(ctx, "transactionScope", |ctx| {
            let token = ctx.cursor().peek().clone();
            if !is_keyword(&token, "SINGLE_TARGET") && !is_keyword(&token, "ALL_TARGETS") {
                return Err(unexpected(&token));
            }
            ctx.cursor().advance();
            Ok(ReadValue::Identifier(Name {
                text: token.text.clone(),
                value: token.text,
                source_info: token.source_info,
            }))
        }),
        "parts" => simple(ctx, "parts", |ctx| {
            some_values(ctx, |ctx| {
                Ok(ReadValue::Block(bare_block(ctx, part_field)?))
            })
        }),
        _ => Ok(None),
    }
}

fn part_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "modelProperty" => simple(ctx, "modelProperty", identifier_value),
        "targetName" => simple(ctx, "targetName", string_value),
        "partitionFields" => partition_fields(ctx, name),
        "deduplicationStrategy" => deduplication_strategy(ctx),
        _ => Ok(None),
    }
}

fn flat_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "targetName" => simple(ctx, "targetName", string_value),
        "modelClass" => simple(ctx, "modelClass", path_value),
        "partitionFields" => partition_fields(ctx, name),
        "deduplicationStrategy" => deduplication_strategy(ctx),
        _ => Ok(None),
    }
}

fn deduplication_strategy(ctx: &mut ParserContext<'_>) -> R<Option<(&'static str, ReadValue)>> {
    unended(ctx, "deduplicationStrategy", |ctx| {
        choice(
            ctx,
            &mut [
                ("None", &mut |ctx| keyword_alone(ctx, false)),
                ("AnyVersion", &mut |ctx| keyword_alone(ctx, false)),
                ("MaxVersion", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| match name {
                        "versionField" => simple(ctx, "versionField", output_value),
                        _ => Ok(None),
                    })
                }),
                ("DuplicateCount", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| match name {
                        "duplicateCountName" => simple(ctx, "duplicateCountName", string_value),
                        _ => Ok(None),
                    })
                }),
            ],
        )
    })
}

fn ingest_mode(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    choice(
        ctx,
        &mut [
            ("NontemporalSnapshot", &mut |ctx| {
                keyword_block(ctx, auditing_field)
            }),
            ("UnitemporalSnapshot", &mut |ctx| {
                keyword_block(ctx, |ctx, name| match name {
                    "transactionMilestoning" => transaction_milestoning(ctx),
                    _ => Ok(None),
                })
            }),
            ("BitemporalSnapshot", &mut |ctx| {
                keyword_block(ctx, |ctx, name| match name {
                    "transactionMilestoning" => transaction_milestoning(ctx),
                    "validityMilestoning" => validity_milestoning(ctx),
                    _ => Ok(None),
                })
            }),
            ("NontemporalDelta", &mut |ctx| {
                keyword_block(ctx, |ctx, name| match name {
                    "mergeStrategy" => merge_strategy(ctx),
                    "auditing" => auditing(ctx),
                    _ => Ok(None),
                })
            }),
            ("UnitemporalDelta", &mut |ctx| {
                keyword_block(ctx, |ctx, name| match name {
                    "mergeStrategy" => merge_strategy(ctx),
                    "transactionMilestoning" => transaction_milestoning(ctx),
                    _ => Ok(None),
                })
            }),
            ("BitemporalDelta", &mut |ctx| {
                keyword_block(ctx, |ctx, name| match name {
                    "mergeStrategy" => merge_strategy(ctx),
                    "transactionMilestoning" => transaction_milestoning(ctx),
                    "validityMilestoning" => validity_milestoning(ctx),
                    _ => Ok(None),
                })
            }),
            ("AppendOnly", &mut |ctx| {
                keyword_block(ctx, |ctx, name| match name {
                    "auditing" => auditing(ctx),
                    "filterDuplicates" => simple(ctx, "filterDuplicates", |ctx| {
                        let token = ctx.cursor().peek().clone();
                        let value = match token.kind {
                            TokenKind::True => true,
                            TokenKind::False => false,
                            _ => return Err(unexpected(&token)),
                        };
                        ctx.cursor().advance();
                        Ok(ReadValue::Boolean(value))
                    }),
                    _ => Ok(None),
                })
            }),
        ],
    )
}

fn auditing_field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "auditing" => auditing(ctx),
        _ => Ok(None),
    }
}

fn auditing(ctx: &mut ParserContext<'_>) -> R<Option<(&'static str, ReadValue)>> {
    unended(ctx, "auditing", |ctx| {
        choice(
            ctx,
            &mut [
                ("None", &mut |ctx| keyword_alone(ctx, false)),
                ("DateTime", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| match name {
                        "dateTimeName" => simple(ctx, "dateTimeName", string_value),
                        _ => Ok(None),
                    })
                }),
            ],
        )
    })
}

/// A milestoning's field whose value is a string, `name: '…';`.
fn name_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
    names: &[&'static str],
) -> R<Option<(&'static str, ReadValue)>> {
    match names.iter().find(|candidate| **candidate == name) {
        Some(name) => simple(ctx, name, string_value),
        None => Ok(None),
    }
}

fn transaction_milestoning(ctx: &mut ParserContext<'_>) -> R<Option<(&'static str, ReadValue)>> {
    unended(ctx, "transactionMilestoning", |ctx| {
        choice(
            ctx,
            &mut [
                ("BatchIdAndDateTime", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| {
                        if name == "derivation" {
                            return transaction_derivation(ctx);
                        }
                        name_field(
                            ctx,
                            name,
                            &[
                                "batchIdInName",
                                "batchIdOutName",
                                "dateTimeInName",
                                "dateTimeOutName",
                            ],
                        )
                    })
                }),
                ("BatchId", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| {
                        name_field(ctx, name, &["batchIdInName", "batchIdOutName"])
                    })
                }),
                ("DateTime", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| {
                        if name == "derivation" {
                            return transaction_derivation(ctx);
                        }
                        name_field(ctx, name, &["dateTimeInName", "dateTimeOutName"])
                    })
                }),
            ],
        )
    })
}

fn transaction_derivation(ctx: &mut ParserContext<'_>) -> R<Option<(&'static str, ReadValue)>> {
    unended(ctx, "derivation", |ctx| {
        choice(
            ctx,
            &mut [
                ("SourceSpecifiesInDateTime", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| match name {
                        "sourceDateTimeInField" => {
                            simple(ctx, "sourceDateTimeInField", identifier_value)
                        }
                        _ => Ok(None),
                    })
                }),
                ("SourceSpecifiesInAndOutDateTime", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| match name {
                        "sourceDateTimeInField" => {
                            simple(ctx, "sourceDateTimeInField", identifier_value)
                        }
                        "sourceDateTimeOutField" => {
                            simple(ctx, "sourceDateTimeOutField", identifier_value)
                        }
                        _ => Ok(None),
                    })
                }),
            ],
        )
    })
}

fn validity_milestoning(ctx: &mut ParserContext<'_>) -> R<Option<(&'static str, ReadValue)>> {
    unended(ctx, "validityMilestoning", |ctx| {
        choice(
            ctx,
            &mut [("DateTime", &mut |ctx| {
                keyword_block(ctx, |ctx, name| {
                    if name == "derivation" {
                        return validity_derivation(ctx);
                    }
                    name_field(ctx, name, &["dateTimeFromName", "dateTimeThruName"])
                })
            })],
        )
    })
}

fn validity_derivation(ctx: &mut ParserContext<'_>) -> R<Option<(&'static str, ReadValue)>> {
    unended(ctx, "derivation", |ctx| {
        choice(
            ctx,
            &mut [
                ("SourceSpecifiesFromDateTime", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| match name {
                        "sourceDateTimeFromField" => {
                            simple(ctx, "sourceDateTimeFromField", identifier_value)
                        }
                        _ => Ok(None),
                    })
                }),
                ("SourceSpecifiesFromAndThruDateTime", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| match name {
                        "sourceDateTimeFromField" => {
                            simple(ctx, "sourceDateTimeFromField", identifier_value)
                        }
                        "sourceDateTimeThruField" => {
                            simple(ctx, "sourceDateTimeThruField", identifier_value)
                        }
                        _ => Ok(None),
                    })
                }),
            ],
        )
    })
}

fn merge_strategy(ctx: &mut ParserContext<'_>) -> R<Option<(&'static str, ReadValue)>> {
    unended(ctx, "mergeStrategy", |ctx| {
        choice(
            ctx,
            &mut [
                ("NoDeletes", &mut |ctx| keyword_alone(ctx, false)),
                ("DeleteIndicator", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| match name {
                        "deleteField" => simple(ctx, "deleteField", identifier_value),
                        "deleteValues" => {
                            simple(ctx, "deleteValues", |ctx| some_values(ctx, string_value))
                        }
                        _ => Ok(None),
                    })
                }),
            ],
        )
    })
}
