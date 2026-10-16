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

//! The plug-in grammars of a persistence's trigger, a context's platform and
//! a service output's target (the engine's `IPersistenceParserExtension`):
//! each written `Type` or `Type #{ … }#`, read by the grammar its type
//! names. The engine core's (`Manual` triggers, the `Default` platform) are
//! built in; other engine modules' register into
//! [`PERSISTENCE_SPECIFICATION_GRAMMARS`] when linked (`dsl-persistence-
//! relational`'s `Relational` target, `dsl-persistence-cloud`'s `AwsGlue`
//! platform). A type no linked grammar reads is unsupported: another engine
//! extension may read it.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{Island, ParserContext};
use linkme::distributed_slice;

use legend_pure_parser_protocol::v1::record::Record;

type R<T> = Result<T, ParseError>;

/// What a specification specifies.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SpecificationKind {
    /// A persistence's trigger.
    Trigger,
    /// A persistence context's platform.
    Platform,
    /// A service output's target.
    Target,
}

impl SpecificationKind {
    /// The kind as the engine's messages name it.
    #[must_use]
    pub fn name(self) -> &'static str {
        match self {
            Self::Trigger => "trigger",
            Self::Platform => "persistence platform",
            Self::Target => "persistence target",
        }
    }
}

/// `Type` or `Type #{ … }#`, as the walker hands it to its type's grammar.
pub struct Specification<'a> {
    /// The island, if any.
    pub island: Option<&'a Island>,
    /// From the type to the island's end.
    pub source_info: &'a SourceInfo,
}

impl Specification<'_> {
    /// The island's body as the engine's walker parses it: without its last
    /// two characters (the walker takes `}#` off text that doesn't have it),
    /// usually the spaces before `}#`. `None` without an island.
    #[must_use]
    pub fn body(&self) -> Option<String> {
        let island = self.island?;
        let mut text = island.text().to_string();
        text.pop();
        text.pop();
        Some(text)
    }
}

/// A grammar for a specification of one type.
pub trait SpecificationGrammar: Send + Sync {
    /// What it specifies.
    fn kind(&self) -> SpecificationKind;

    /// The type it reads, as written.
    fn type_name(&self) -> &str;

    /// The specification's object, as the engine's protocol has it.
    ///
    /// # Errors
    ///
    /// The grammar's.
    fn parse(&self, specification: &Specification<'_>, ctx: &mut ParserContext<'_>) -> R<Record>;
}

/// The engine core's `Manual` trigger, its island, if any, ignored.
struct ManualTrigger;

impl SpecificationGrammar for ManualTrigger {
    fn kind(&self) -> SpecificationKind {
        SpecificationKind::Trigger
    }

    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "Manual"
    }

    fn parse(&self, _: &Specification<'_>, _: &mut ParserContext<'_>) -> R<Record> {
        Ok(Record::new("manualTrigger"))
    }
}

/// The engine core's `Default` platform, positioned on the specification,
/// its island, if any, ignored.
struct DefaultPlatform;

impl SpecificationGrammar for DefaultPlatform {
    fn kind(&self) -> SpecificationKind {
        SpecificationKind::Platform
    }

    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "Default"
    }

    fn parse(&self, specification: &Specification<'_>, _: &mut ParserContext<'_>) -> R<Record> {
        Ok(Record::new("default").at(specification.source_info))
    }
}

/// The engine core's grammars.
static CORE_GRAMMARS: [&(dyn SpecificationGrammar + Send + Sync); 2] =
    [&ManualTrigger, &DefaultPlatform];

/// The specification grammars other engine modules register.
#[distributed_slice]
pub static PERSISTENCE_SPECIFICATION_GRAMMARS: [&'static (
              dyn SpecificationGrammar + Send + Sync
          )] = [..];

/// The grammar for `kind` specifications of `type_name`, if one is linked.
#[must_use]
pub fn specification_grammar(
    kind: SpecificationKind,
    type_name: &str,
) -> Option<&'static (dyn SpecificationGrammar + Send + Sync)> {
    CORE_GRAMMARS
        .iter()
        .chain(PERSISTENCE_SPECIFICATION_GRAMMARS.iter())
        .copied()
        .find(|grammar| grammar.kind() == kind && grammar.type_name() == type_name)
}

/// The types linked grammars read for `kind`, sorted and comma-separated.
#[must_use]
pub fn linked_specification_types(kind: SpecificationKind) -> String {
    let mut types: Vec<&str> = CORE_GRAMMARS
        .iter()
        .chain(PERSISTENCE_SPECIFICATION_GRAMMARS.iter())
        .filter(|grammar| grammar.kind() == kind)
        .map(|grammar| grammar.type_name())
        .collect();
    types.sort_unstable();
    types.join(", ")
}
