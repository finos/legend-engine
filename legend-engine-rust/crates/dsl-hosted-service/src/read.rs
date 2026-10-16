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

//! The section's grammar, `HostedServiceParserGrammar`: what it reads,
//! before the walk checks it. The core parser reads the section's imports
//! first. A hosted service's fields are read as a block of fields (see
//! [`legend_pure_parser_parser::blocks`]); an execution environment's
//! executions as the grammar writes them, a mapping and a runtime each.

use legend_pure_dsl_function_activator::read::{
    ReadActivator, activator, deployment, documentation, function,
};
use legend_pure_dsl_runtime::ast::ElementPath;
use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Package;
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::blocks::{
    ReadValue, boolean_value, path_value, simple, string_value, values,
};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::forms::{list, separated, string};
use legend_pure_parser_parser::words::{
    Name, expect_word, identifier, is_word, is_word_kind, qualified_name, unexpected,
};
use legend_pure_parser_parser::{Island, ParserContext};

type R<T> = Result<T, ParseError>;

/// The grammar's keywords, which a post-deployment action's type, a
/// `VALID_STRING`, isn't.
const KEYWORDS: [&str; 43] = [
    "stereotypes",
    "tags",
    "HostedService",
    "import",
    "pattern",
    "ownership",
    "Deployment",
    "identifier",
    "UserList",
    "users",
    "activationConfiguration",
    "documentation",
    "autoActivateUpdates",
    "generateLineage",
    "storeModel",
    "function",
    "binding",
    "contentType",
    "testSuites",
    "data",
    "connections",
    "tests",
    "asserts",
    "serializationFormat",
    "parameters",
    "keys",
    "list",
    "postValidations",
    "description",
    "params",
    "assertions",
    "actions",
    "ExecutionEnvironment",
    "mapping",
    "runtime",
    "executions",
    "HostedServiceDeploymentConfiguration",
    "stage",
    "all",
    "let",
    "allVersions",
    "allVersionsInRange",
    "toBytes",
];

/// A hosted service, an execution environment or a deployment
/// configuration.
pub(crate) enum ReadElement {
    /// `HostedService my::S { … }`.
    Service(Box<ReadActivator>),
    /// `ExecutionEnvironment my::E { executions: [ … ]; }`.
    Environment(ReadEnvironment),
    /// `HostedServiceDeploymentConfiguration my::C { }`, from its keyword
    /// to `}`.
    Configuration(SourceInfo),
}

/// `ExecutionEnvironment my::E { executions: [ … ]; }`.
pub(crate) struct ReadEnvironment {
    pub package: Option<Package>,
    pub name: SpannedString,
    pub executions: Vec<ReadExecution>,
    /// From `ExecutionEnvironment` to `}`.
    pub source_info: SourceInfo,
}

/// `key: { … }`, or a group of them, `key: [ key: { … }, … ]`.
pub(crate) enum ReadExecution {
    Single(Box<ReadSingle>),
    Multi(Name, Vec<ReadSingle>),
}

/// `key: { mapping: my::M; runtime: … }`.
pub(crate) struct ReadSingle {
    pub key: Name,
    pub mapping: ElementPath,
    pub runtime: ReadRuntime,
    /// From the key to `}`.
    pub source_info: SourceInfo,
}

/// `my::R;`, or a runtime of its own, `#{ … }#;`.
pub(crate) enum ReadRuntime {
    Pointer(ElementPath),
    /// The island, and its span to the `;` after it.
    Embedded(Island, SourceInfo),
}

/// A hosted service, an execution environment or a deployment
/// configuration.
pub(crate) fn element(ctx: &mut ParserContext<'_>) -> R<ReadElement> {
    let start = ctx.cursor().current_source_info();
    let token = ctx.cursor().peek().clone();
    if is_word(&token, "ExecutionEnvironment") {
        return Ok(ReadElement::Environment(environment(ctx)?));
    }
    if is_word(&token, "HostedServiceDeploymentConfiguration") {
        ctx.cursor().advance();
        qualified_name(ctx)?;
        ctx.cursor().expect(TokenKind::LBrace)?;
        ctx.cursor().expect(TokenKind::RBrace)?;
        return Ok(ReadElement::Configuration(ctx.cursor().span_since(&start)));
    }
    let documentation = documentation(ctx);
    let keyword = ctx.cursor().peek().clone();
    if !is_word(&keyword, "HostedService") {
        return Err(unexpected(&keyword));
    }
    Ok(ReadElement::Service(Box::new(activator(
        ctx,
        &start,
        documentation,
        field,
    )?)))
}

fn field(ctx: &mut ParserContext<'_>, name: &str) -> R<Option<(&'static str, ReadValue)>> {
    match name {
        "pattern" => simple(ctx, "pattern", string_value),
        "function" => simple(ctx, "function", function),
        "ownership" => simple(ctx, "ownership", ownership),
        "documentation" => simple(ctx, "documentation", string_value),
        "autoActivateUpdates" => simple(ctx, "autoActivateUpdates", boolean_value),
        "actions" => simple(ctx, "actions", |ctx| values(ctx, action)),
        // Read and left out, as the walker leaves them.
        "activationConfiguration" => simple(ctx, "activationConfiguration", path_value),
        "generateLineage" => simple(ctx, "generateLineage", boolean_value),
        "storeModel" => simple(ctx, "storeModel", boolean_value),
        // `binding: my::B;;`: its rule's `;`, then the field's.
        "binding" => simple(ctx, "binding", |ctx| {
            let binding = path_value(ctx)?;
            ctx.cursor().expect(TokenKind::Semicolon)?;
            Ok(binding)
        }),
        "contentType" => simple(ctx, "contentType", string_value),
        "testSuites" | "postValidations" => Err(ParseError::Unsupported {
            message: format!(
                "the engine's walker leaves a hosted service's {name} out; Rust doesn't read \
                 their grammar"
            ),
            source_info: ctx.cursor().peek().source_info.clone(),
        }),
        _ => Ok(None),
    }
}

/// `UserList { users: [ 'user', … ] }`, its users; or `Deployment {
/// identifier: 'id' }`, its identifier.
fn ownership(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    if !is_word(ctx.cursor().peek(), "UserList") {
        return deployment(ctx);
    }
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::LBrace)?;
    expect_word(ctx, "users")?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let users = list(ctx, |ctx| Ok(ReadValue::String(string(ctx)?)))?;
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ReadValue::List(users))
}

/// `Type #{ … }#`: its type, a `VALID_STRING`, and its body, if any;
/// positioned from its type to its end.
fn action(ctx: &mut ParserContext<'_>) -> R<ReadValue> {
    let token = ctx.cursor().peek().clone();
    if !is_word_kind(token.kind) || KEYWORDS.contains(&token.text.as_str()) {
        return Err(unexpected(&token));
    }
    ctx.cursor().advance();
    if ctx.cursor().check(TokenKind::IslandOpen) {
        ctx.read_island()?;
    }
    Ok(ReadValue::Text(
        token.text.to_string(),
        ctx.cursor().span_since(&token.source_info),
    ))
}

/// `ExecutionEnvironment my::E { executions: [ … ]; }`.
fn environment(ctx: &mut ParserContext<'_>) -> R<ReadEnvironment> {
    let start = ctx.cursor().advance().source_info.clone();
    let (package, name, name_source_info) = ctx.parse_qualified_name()?;
    ctx.cursor().expect(TokenKind::LBrace)?;
    expect_word(ctx, "executions")?;
    ctx.cursor().expect(TokenKind::Colon)?;
    ctx.cursor().expect(TokenKind::LBracket)?;
    let executions = separated(ctx, |ctx| {
        let key = identifier(ctx)?;
        ctx.cursor().expect(TokenKind::Colon)?;
        if ctx.cursor().eat(TokenKind::LBracket) {
            let singles = separated(ctx, |ctx| {
                let key = identifier(ctx)?;
                ctx.cursor().expect(TokenKind::Colon)?;
                single(ctx, key)
            })?;
            ctx.cursor().expect(TokenKind::RBracket)?;
            return Ok(ReadExecution::Multi(key, singles));
        }
        Ok(ReadExecution::Single(Box::new(single(ctx, key)?)))
    })?;
    ctx.cursor().expect(TokenKind::RBracket)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ReadEnvironment {
        package,
        name: SpannedString {
            value: name,
            source_info: name_source_info,
        },
        executions,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `{ mapping: my::M; runtime: my::R; }`, after `key:`: a mapping, then a
/// runtime, a pointer or one of its own.
fn single(ctx: &mut ParserContext<'_>, key: Name) -> R<ReadSingle> {
    ctx.cursor().expect(TokenKind::LBrace)?;
    expect_word(ctx, "mapping")?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let mapping = element_path(ctx)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    expect_word(ctx, "runtime")?;
    ctx.cursor().expect(TokenKind::Colon)?;
    let runtime = if ctx.cursor().check(TokenKind::IslandOpen) {
        let island = ctx.read_island()?;
        ctx.cursor().expect(TokenKind::Semicolon)?;
        let source_info = ctx.cursor().span_since(&island.source_info);
        ReadRuntime::Embedded(island, source_info)
    } else {
        let path = element_path(ctx)?;
        ctx.cursor().expect(TokenKind::Semicolon)?;
        ReadRuntime::Pointer(path)
    };
    ctx.cursor().expect(TokenKind::RBrace)?;
    Ok(ReadSingle {
        source_info: ctx.cursor().span_since(&key.source_info),
        key,
        mapping,
        runtime,
    })
}

/// A path, unquoted, positioned.
fn element_path(ctx: &mut ParserContext<'_>) -> R<ElementPath> {
    let name = qualified_name(ctx)?;
    Ok(ElementPath {
        path: name.path,
        source_info: name.source_info,
    })
}
