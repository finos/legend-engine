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

//! The walk (the engine's `HostedServiceTreeWalker`): each hosted service's
//! fields checked and built in the walker's order, into a record shaped as
//! the engine's protocol; and each execution environment's executions.

use legend_pure_dsl_function_activator::walk::{deployment_owner, function};
use legend_pure_dsl_runtime::section::parse_embedded_runtime;
use legend_pure_dsl_service::ast::{
    ExecutionEnvironment, ExecutionParameters, MultiExecutionParameters, Runtime,
    SingleExecutionParameters,
};
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{ReadBlock, ReadField, ReadValue};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::string_text;
use legend_pure_parser_protocol::v1::record::Record;

use crate::read::{ReadEnvironment, ReadExecution, ReadRuntime, ReadSingle};

type R<T> = Result<T, ParseError>;

/// The walker's `visitHostedService`: the hosted service's own record,
/// without its package, name and annotations. The walker writes
/// `autoActivateUpdates` `false` when not given, and never reads
/// `generateLineage` and `storeModel`, `false`.
pub(crate) fn hosted_service(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let pattern = read.take("pattern", &at)?.string()?;
    let function = function(read.take("function", &at)?)?;
    let ownership = ownership(read.take("ownership", &at)?)?;
    let documentation = read
        .take_optional("documentation", &at)?
        .map(ReadField::string)
        .transpose()?;
    let auto_activate_updates = read
        .take_optional("autoActivateUpdates", &at)?
        .map(ReadField::boolean)
        .transpose()?
        .unwrap_or(false);
    if let Some(actions) = read.take_optional("actions", &at)? {
        actions_of(actions)?;
    }
    Ok(Record::new("hostedService")
        .at(&at)
        .with("pattern", pattern)
        .with("function", function)
        .with("ownership", ownership)
        .with_some("documentation", documentation)
        .with("autoActivateUpdates", auto_activate_updates)
        .with("generateLineage", false)
        .with("storeModel", false))
}

/// A deployment's owner, or a list of users.
fn ownership(field: ReadField) -> R<Record> {
    let ReadValue::List(users) = field.value else {
        return deployment_owner(field);
    };
    let users = users
        .into_iter()
        .map(|user| match user {
            ReadValue::String(token) => string_text(&token),
            _ => Err(ParseError::Unexpected {
                message: "the grammar misread the field 'ownership'".to_string(),
                source_info: field.source_info.clone(),
            }),
        })
        .collect::<R<Vec<_>>>()?;
    Ok(Record::new("userList").with("users", users))
}

/// The post-deployment actions: none, as no grammar of the engine's reads
/// one (`IPostDeploymentActionGrammarParserExtension` has no
/// implementation), which the engine reports as unsupported syntax.
fn actions_of(field: ReadField) -> R<()> {
    let at = field.source_info.clone();
    match field.values()?.into_iter().next() {
        None => Ok(()),
        Some(ReadValue::Text(kind, source_info)) => Err(ParseError::Unsupported {
            message: format!("no post-deployment action grammar is linked for '{kind}'"),
            source_info,
        }),
        Some(_) => Err(ParseError::Unexpected {
            message: "the grammar misread the field 'actions'".to_string(),
            source_info: at,
        }),
    }
}

/// The walker's `visitExecutionEnvironment`: its executions all single
/// ones, or all groups of them.
pub(crate) fn environment(
    ctx: &mut ParserContext<'_>,
    read: ReadEnvironment,
) -> R<ExecutionEnvironment> {
    let source_info = read.source_info;
    let executions = if read
        .executions
        .iter()
        .any(|execution| matches!(execution, ReadExecution::Single(_)))
    {
        ExecutionParameters::Single(
            read.executions
                .into_iter()
                .map(|execution| match execution {
                    ReadExecution::Single(single) => walk_single(ctx, *single),
                    // The engine fails on a group among single executions.
                    ReadExecution::Multi(..) => Err(ParseError::Unsupported {
                        message: "the engine fails on an execution environment mixing single \
                                  and grouped executions"
                            .to_string(),
                        source_info: source_info.clone(),
                    }),
                })
                .collect::<R<_>>()?,
        )
    } else {
        ExecutionParameters::Multi(
            read.executions
                .into_iter()
                .filter_map(|execution| match execution {
                    ReadExecution::Multi(master_key, singles) => Some((master_key, singles)),
                    ReadExecution::Single(_) => None,
                })
                .map(|(master_key, singles)| {
                    Ok(MultiExecutionParameters {
                        master_key: master_key.value,
                        executions: singles
                            .into_iter()
                            .map(|single| walk_single(ctx, single))
                            .collect::<R<_>>()?,
                    })
                })
                .collect::<R<_>>()?,
        )
    };
    Ok(ExecutionEnvironment {
        package: read.package,
        name: read.name,
        executions,
        source_info,
    })
}

/// The walker's `visitSingleExecutionParameters`.
fn walk_single(ctx: &mut ParserContext<'_>, read: ReadSingle) -> R<SingleExecutionParameters> {
    let runtime = match read.runtime {
        ReadRuntime::Pointer(path) => Runtime::Pointer(path),
        ReadRuntime::Embedded(island, at) => {
            Runtime::Embedded(parse_embedded_runtime(ctx, &island, &at)?)
        }
    };
    Ok(SingleExecutionParameters {
        key: read.key.value,
        mapping: read.mapping,
        runtime: Some(runtime),
        runtime_components: None,
        source_info: read.source_info,
    })
}
