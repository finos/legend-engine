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

//! # Legend Pure — Relational persistence targets
//!
//! The engine's relational persistence target (its
//! `PersistenceRelationalParserGrammar` and
//! `PersistenceRelationalParseTreeWalker`), a service output's target,
//! registered as `dsl-persistence`'s target grammar for `Relational`:
//!
//! ```text
//! Relational #{ ( table: name (. name)?; | database: my::Db;
//!               | temporality: None { … } | Unitemporal { … } | Bitemporal { … } )* }#
//! ```
//!
//! The engine's walker parses the island's body without its last two
//! characters (usually the spaces before `}#`), as its own source, its
//! rule ending at the end of input: the target is positioned from its first
//! token to the end of input, whose text, `<EOF>`, ANTLR counts. Without a
//! temporality, the target is non-temporal.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

use legend_pure_dsl_persistence::grammar::{
    PERSISTENCE_SPECIFICATION_GRAMMARS, Specification, SpecificationGrammar, SpecificationKind,
};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{
    ReadBlock, ReadField, ReadValue, choice, identifier_value, keyword_alone, keyword_block,
    simple, unended,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::is_word_kind;
use legend_pure_parser_parser::words::{identifier, qualified_name};
use legend_pure_parser_protocol::v1::record::Record;
use linkme::distributed_slice;

type R<T> = Result<T, ParseError>;

/// The target type this grammar reads.
pub const TYPE_NAME: &str = "Relational";

/// The relational persistence target's grammar.
pub struct RelationalTarget;

impl SpecificationGrammar for RelationalTarget {
    fn kind(&self) -> SpecificationKind {
        SpecificationKind::Target
    }

    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(&self, specification: &Specification<'_>, ctx: &mut ParserContext<'_>) -> R<Record> {
        let body = specification.body().unwrap_or_default();
        let Some(island) = specification.island.filter(|_| !body.is_empty()) else {
            return Err(ParseError::Unexpected {
                message: format!("Persistence target '{TYPE_NAME}' must have a non-empty body"),
                source_info: specification.source_info.clone(),
            });
        };
        let mut read = ctx.parse_island_text(island, &body, |ctx| {
            let start = ctx.cursor().current_source_info();
            let mut fields = Vec::new();
            loop {
                let token = ctx.cursor().peek().clone();
                if !is_word_kind(token.kind) {
                    break;
                }
                let Some((name, value)) = definition_field(ctx, &token.text)? else {
                    break;
                };
                fields.push(ReadField {
                    name,
                    value,
                    source_info: ctx.cursor().span_since(&token.source_info),
                });
            }
            // The rule ends at the end of input, which ANTLR counts as
            // `<EOF>`.
            let end = ctx.cursor().current_source_info();
            if !ctx.cursor().check(TokenKind::Eof) {
                return Err(legend_pure_parser_parser::words::unexpected(
                    ctx.cursor().peek(),
                ));
            }
            Ok(ReadBlock {
                keyword: ctx.cursor().peek().clone(),
                fields,
                source_info: SourceInfo::new(
                    start.source.clone(),
                    start.start_line,
                    start.start_column,
                    end.start_line,
                    end.start_column + 4,
                ),
            })
        })?;
        target(&mut read)
    }
}

fn definition_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "table" => simple(ctx, "table", |ctx| {
            let table = identifier(ctx)?;
            let mut value = table.value.to_string();
            if ctx.cursor().eat(TokenKind::Dot) {
                value.push('.');
                value.push_str(&identifier(ctx)?.value);
            }
            Ok(ReadValue::Identifier(
                legend_pure_parser_parser::words::Name {
                    text: table.text,
                    value: value.into(),
                    source_info: table.source_info,
                },
            ))
        }),
        "database" => simple(ctx, "database", |ctx| {
            Ok(ReadValue::Path(qualified_name(ctx)?))
        }),
        "temporality" => unended(ctx, "temporality", |ctx| {
            choice(
                ctx,
                &mut [
                    ("None", &mut |ctx| keyword_block(ctx, nontemporal_field)),
                    ("Unitemporal", &mut |ctx| {
                        keyword_block(ctx, processing_field)
                    }),
                    ("Bitemporal", &mut |ctx| {
                        keyword_block(ctx, |ctx, name| {
                            if name == "sourceDerivedDimension" {
                                return source_derived_dimension(ctx);
                            }
                            processing_field(ctx, name)
                        })
                    }),
                ],
            )
        }),
        _ => Ok(None),
    }
}

fn nontemporal_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "auditing" => unended(ctx, "auditing", |ctx| {
            choice(
                ctx,
                &mut [
                    ("DateTime", &mut |ctx| {
                        keyword_block(ctx, |ctx, name| match name {
                            "dateTimeName" => simple(ctx, "auditingDateTimeName", identifier_value),
                            _ => Ok(None),
                        })
                    }),
                    ("None", &mut |ctx| keyword_alone(ctx, true)),
                ],
            )
        }),
        "updatesHandling" => unended(ctx, "updatesHandling", |ctx| {
            choice(
                ctx,
                &mut [
                    ("AppendOnly", &mut |ctx| {
                        keyword_block(ctx, |ctx, name| match name {
                            "appendStrategy" => unended(ctx, "appendStrategy", |ctx| {
                                choice(
                                    ctx,
                                    &mut [
                                        ("AllowDuplicates", &mut |ctx| keyword_alone(ctx, true)),
                                        ("FailOnDuplicates", &mut |ctx| keyword_alone(ctx, true)),
                                        ("FilterDuplicates", &mut |ctx| keyword_alone(ctx, true)),
                                    ],
                                )
                            }),
                            _ => Ok(None),
                        })
                    }),
                    ("Overwrite", &mut |ctx| keyword_alone(ctx, true)),
                ],
            )
        }),
        _ => Ok(None),
    }
}

/// A field whose value is a name: `name: value;`.
fn name_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
    names: &[&'static str],
) -> R<Option<(&'static str, ReadValue)>> {
    match names.iter().find(|candidate| **candidate == name) {
        Some(name) => simple(ctx, name, identifier_value),
        None => Ok(None),
    }
}

fn processing_field(
    ctx: &mut ParserContext<'_>,
    name: &str,
) -> R<Option<(&'static str, ReadValue)>> {
    if name != "processingDimension" {
        return Ok(None);
    }
    unended(ctx, "processingDimension", |ctx| {
        choice(
            ctx,
            &mut [
                ("BatchIdAndDateTime", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| {
                        name_field(
                            ctx,
                            name,
                            &["batchIdIn", "batchIdOut", "dateTimeIn", "dateTimeOut"],
                        )
                    })
                }),
                ("BatchId", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| {
                        name_field(ctx, name, &["batchIdIn", "batchIdOut"])
                    })
                }),
                ("DateTime", &mut |ctx| {
                    keyword_block(ctx, |ctx, name| {
                        name_field(ctx, name, &["dateTimeIn", "dateTimeOut"])
                    })
                }),
            ],
        )
    })
}

fn source_derived_dimension(ctx: &mut ParserContext<'_>) -> R<Option<(&'static str, ReadValue)>> {
    unended(ctx, "sourceDerivedDimension", |ctx| {
        choice(
            ctx,
            &mut [("DateTime", &mut |ctx| {
                keyword_block(ctx, |ctx, name| {
                    if name == "sourceFields" {
                        return unended(ctx, "sourceFields", |ctx| {
                            choice(
                                ctx,
                                &mut [
                                    ("StartAndEnd", &mut |ctx| {
                                        keyword_block(ctx, |ctx, name| {
                                            name_field(ctx, name, &["startField", "endField"])
                                        })
                                    }),
                                    ("Start", &mut |ctx| {
                                        keyword_block(ctx, |ctx, name| {
                                            name_field(ctx, name, &["startField"])
                                        })
                                    }),
                                ],
                            )
                        });
                    }
                    name_field(ctx, name, &["dateTimeStart", "dateTimeEnd"])
                })
            })],
        )
    })
}

// ── The walk ─────────────────────────────────────────────────────────────

/// The walker's `visitPersistenceTarget`.
fn target(read: &mut ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let table = read.take("table", &at)?.identifier()?;
    let database = read.take("database", &at)?;
    let database_at = database.source_info.clone();
    let database = Record::untyped()
        .with("type", "STORE".to_string())
        .with("path", database.path()?)
        .at(&database_at);
    let temporality = match read.take_optional("temporality", &at)? {
        Some(field) => temporality(field.block()?)?,
        None => Record::new("none"),
    };
    Ok(Record::new("relationalPersistenceTarget")
        .at(&at)
        .with("table", table)
        .with("database", database)
        .with("temporality", temporality))
}

/// The walker's `visitTemporality`.
fn temporality(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    match read.keyword.text.as_str() {
        "None" => {
            let auditing = read
                .take_optional("auditing", &at)?
                .map(|field| auditing(field.block()?))
                .transpose()?;
            let updates = updates_handling(read.take("updatesHandling", &at)?.block()?)?;
            Ok(Record::new("none")
                .at(&at)
                .with_some("auditing", auditing)
                .with("updatesHandling", updates))
        }
        "Unitemporal" => {
            let processing = processing_dimension(read.take("processingDimension", &at)?.block()?)?;
            Ok(Record::new("unitemporalTemporality")
                .at(&at)
                .with("processingDimension", processing))
        }
        _ => {
            let processing = processing_dimension(read.take("processingDimension", &at)?.block()?)?;
            let derived = source_derived_dimension_record(
                read.take("sourceDerivedDimension", &at)?.block()?,
            )?;
            Ok(Record::new("bitemporalTemporality")
                .at(&at)
                .with("processingDimension", processing)
                .with("sourceDerivedDimension", derived))
        }
    }
}

/// The walker's `visitAuditing`.
fn auditing(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    if read.keyword.text == "None" {
        return Ok(Record::new("noAuditing").at(&at));
    }
    let name = read.take("auditingDateTimeName", &at)?.identifier()?;
    Ok(Record::new("auditingDateTime")
        .at(&at)
        .with("auditingDateTimeName", name))
}

/// The walker's `visitUpdatesHandling`.
fn updates_handling(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    if read.keyword.text == "Overwrite" {
        return Ok(Record::new("overwrite").at(&at));
    }
    let strategy = read.take("appendStrategy", &at)?.block()?;
    let kind = match strategy.keyword.text.as_str() {
        "AllowDuplicates" => "allowDuplicates",
        "FailOnDuplicates" => "failOnDuplicates",
        _ => "filterDuplicates",
    };
    Ok(Record::new("appendOnly").at(&at).with(
        "appendStrategy",
        Record::new(kind).at(&strategy.source_info),
    ))
}

/// The walker's `visitProcessingDimension`: each kind's names, in its order.
fn processing_dimension(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let (kind, fields): (&'static str, &[(&'static str, &'static str)]) =
        match read.keyword.text.as_str() {
            "BatchId" => (
                "batchId",
                &[("batchIdIn", "batchIdIn"), ("batchIdOut", "batchIdOut")],
            ),
            "DateTime" => (
                "processingTime",
                &[("dateTimeIn", "timeIn"), ("dateTimeOut", "timeOut")],
            ),
            _ => (
                "batchIdAndProcessingTime",
                &[
                    ("batchIdIn", "batchIdIn"),
                    ("batchIdOut", "batchIdOut"),
                    ("dateTimeIn", "timeIn"),
                    ("dateTimeOut", "timeOut"),
                ],
            ),
        };
    let mut dimension = Record::new(kind).at(&at);
    for (name, protocol_name) in fields {
        dimension = dimension.with(protocol_name, read.take(name, &at)?.identifier()?);
    }
    Ok(dimension)
}

/// The walker's `visitSourceDerivedDimension`.
fn source_derived_dimension_record(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let start = read.take("dateTimeStart", &at)?.identifier()?;
    let end = read.take("dateTimeEnd", &at)?.identifier()?;
    let mut fields = read.take("sourceFields", &at)?.block()?;
    let fields_at = fields.source_info.clone();
    let start_field = fields.take("startField", &fields_at)?.identifier()?;
    let source_fields = if fields.keyword.text == "Start" {
        Record::new("sourceTimeStart")
            .at(&fields_at)
            .with("startField", start_field)
    } else {
        let end_field = fields.take("endField", &fields_at)?.identifier()?;
        Record::new("sourceTimeStartAndEnd")
            .at(&fields_at)
            .with("startField", start_field)
            .with("endField", end_field)
    };
    Ok(Record::new("sourceDerivedTime")
        .at(&at)
        .with("timeStart", start)
        .with("timeEnd", end)
        .with("sourceTimeFields", source_fields))
}

#[distributed_slice(PERSISTENCE_SPECIFICATION_GRAMMARS)]
static RELATIONAL_TARGET: &(dyn SpecificationGrammar + Send + Sync) = &RelationalTarget;
