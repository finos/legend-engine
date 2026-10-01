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

//! Grammars plugged into a mapping: a mapping element's body, chosen by the
//! type after the element's colon (the engine's `MappingElementParser`s),
//! and a legacy test's input data, chosen by its type (its
//! `MappingTestInputDataParser`s). Each grammar converts what it read too:
//! it is its own protocol half.
//!
//! The engine core's grammars are this crate's, built in. A store's are its
//! crate's (`dsl-relational`'s `Relational`), which registers them:
//!
//! ```ignore
//! use legend_pure_dsl_mapping::grammar::{MappingElementGrammar, MAPPING_ELEMENT_GRAMMARS};
//! use linkme::distributed_slice;
//!
//! #[distributed_slice(MAPPING_ELEMENT_GRAMMARS)]
//! static RELATIONAL: &(dyn MappingElementGrammar + Send + Sync) = &RelationalMappingGrammar;
//! ```
//!
//! An element or input of a type no linked grammar reads is unsupported: an
//! engine extension may read it, and the engine then parses the section.

use std::collections::HashSet;
use std::sync::OnceLock;

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::element_protocol::ConvertError;
use linkme::distributed_slice;

use crate::aggregation_aware::AggregationAwareGrammar;
use crate::ast::{MappingElement, MappingElementBody, MappingElementHeader, TestInput};
use crate::enumeration::EnumerationMappingGrammar;
use crate::model_join::ModelJoinGrammar;
use crate::object_input::ObjectInputGrammar;
use crate::operation::OperationGrammar;
use crate::pure_instance::PureInstanceGrammar;
use crate::relation_function::RelationFunctionGrammar;
use crate::xstore::XStoreGrammar;

/// The engine core's mapping element grammars.
static CORE_MAPPING_ELEMENT_GRAMMARS: [&(dyn MappingElementGrammar + Send + Sync); 7] = [
    &AggregationAwareGrammar,
    &EnumerationMappingGrammar,
    &PureInstanceGrammar,
    &RelationFunctionGrammar,
    &XStoreGrammar,
    &ModelJoinGrammar,
    &OperationGrammar,
];

/// The engine core's legacy test input grammars.
static CORE_TEST_INPUT_GRAMMARS: [&(dyn TestInputGrammar + Send + Sync); 1] = [&ObjectInputGrammar];

/// Where a mapping element is.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ElementSpan {
    /// The whole element, from its header to its `}`: where the engine's
    /// walkers report what they find wrong with the element as a whole.
    pub element: SourceInfo,
    /// Its body, from `{` to `}`: the engine parses the body as a text of
    /// its own, starting right after `{`.
    pub body: SourceInfo,
}

/// Which of a mapping's lists an element goes in.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MappingElementKind {
    /// `classMappings`.
    Class,
    /// `enumerationMappings`.
    Enumeration,
    /// `associationMappings`.
    Association,
}

/// The grammar of one type of mapping element, e.g. `EnumerationMapping` or
/// `Pure`.
///
/// Stateless: the same instance may parse on several threads at once.
pub trait MappingElementGrammar: Send + Sync {
    /// The type written after the element's colon.
    fn type_name(&self) -> &str;

    /// Parses an element's body. The context's cursor holds the tokens
    /// between the braces, then an end of input positioned on the closing
    /// `}`: the grammar must read them all. `span` says where the element
    /// and its body are.
    ///
    /// # Errors
    ///
    /// A malformed body, or one the element's header doesn't allow.
    fn parse(
        &self,
        header: &MappingElementHeader,
        span: &ElementSpan,
        ctx: &mut ParserContext<'_>,
    ) -> Result<Box<dyn MappingElementBody>, ParseError>;

    /// Converts an element whose body this grammar read into its protocol
    /// JSON, and says which of the mapping's lists it goes in.
    ///
    /// # Errors
    ///
    /// A body of another grammar's type, or a nested conversion failure.
    fn convert(
        &self,
        element: &MappingElement,
    ) -> Result<(MappingElementKind, serde_json::Value), ConvertError>;
}

/// The grammar of one type of legacy test input data, e.g. `Object`.
///
/// Stateless: the same instance may parse on several threads at once.
pub trait TestInputGrammar: Send + Sync {
    /// The type written first in the input.
    fn type_name(&self) -> &str;

    /// Checks an input of this type, as the engine's parser for it does.
    ///
    /// # Errors
    ///
    /// An input this type doesn't allow, e.g. a format it doesn't know.
    fn check(&self, input: &TestInput) -> Result<(), ParseError>;

    /// Converts an input of this type into its protocol JSON.
    ///
    /// # Errors
    ///
    /// A conversion failure.
    fn convert(&self, input: &TestInput) -> Result<serde_json::Value, ConvertError>;
}

/// Distributed slice into which each crate owning a mapping element grammar
/// registers it. See the module docs.
#[distributed_slice]
pub static MAPPING_ELEMENT_GRAMMARS: [&'static (dyn MappingElementGrammar + Send + Sync)] = [..];

/// Distributed slice into which each crate owning a legacy test input
/// grammar registers it. See the module docs.
#[distributed_slice]
pub static TEST_INPUT_GRAMMARS: [&'static (dyn TestInputGrammar + Send + Sync)] = [..];

/// The engine core's mapping element grammars and the registered
/// [`MAPPING_ELEMENT_GRAMMARS`], validated once per process.
///
/// # Panics
///
/// Panics when two registered grammars share a type: a link-time
/// misconfiguration.
#[must_use]
pub fn mapping_element_grammars() -> &'static [&'static (dyn MappingElementGrammar + Send + Sync)] {
    static DISCOVERED: OnceLock<Vec<&'static (dyn MappingElementGrammar + Send + Sync)>> =
        OnceLock::new();
    DISCOVERED.get_or_init(|| {
        unique(
            CORE_MAPPING_ELEMENT_GRAMMARS
                .iter()
                .chain(MAPPING_ELEMENT_GRAMMARS.iter())
                .copied(),
            |g| g.type_name(),
            "mapping element",
        )
    })
}

/// The engine core's legacy test input grammars and the registered
/// [`TEST_INPUT_GRAMMARS`], validated once per process.
///
/// # Panics
///
/// Panics when two registered grammars share a type: a link-time
/// misconfiguration.
#[must_use]
pub fn test_input_grammars() -> &'static [&'static (dyn TestInputGrammar + Send + Sync)] {
    static DISCOVERED: OnceLock<Vec<&'static (dyn TestInputGrammar + Send + Sync)>> =
        OnceLock::new();
    DISCOVERED.get_or_init(|| {
        unique(
            CORE_TEST_INPUT_GRAMMARS
                .iter()
                .chain(TEST_INPUT_GRAMMARS.iter())
                .copied(),
            |g| g.type_name(),
            "test input",
        )
    })
}

/// The grammar for mapping elements of type `type_name`.
#[must_use]
pub fn mapping_element_grammar(
    type_name: &str,
) -> Option<&'static (dyn MappingElementGrammar + Send + Sync)> {
    mapping_element_grammars()
        .iter()
        .copied()
        .find(|g| g.type_name() == type_name)
}

/// The grammar for legacy test inputs of type `type_name`.
#[must_use]
pub fn test_input_grammar(
    type_name: &str,
) -> Option<&'static (dyn TestInputGrammar + Send + Sync)> {
    test_input_grammars()
        .iter()
        .copied()
        .find(|g| g.type_name() == type_name)
}

/// The linked mapping element types, sorted, for messages.
#[must_use]
pub fn linked_mapping_element_types() -> String {
    linked(mapping_element_grammars().iter().map(|g| g.type_name()))
}

/// The linked legacy test input types, sorted, for messages.
#[must_use]
pub fn linked_test_input_types() -> String {
    linked(test_input_grammars().iter().map(|g| g.type_name()))
}

fn linked<'a>(types: impl Iterator<Item = &'a str>) -> String {
    let mut types: Vec<&str> = types.collect();
    if types.is_empty() {
        return "none".to_string();
    }
    types.sort_unstable();
    types.join(", ")
}

fn unique<T: ?Sized>(
    grammars: impl Iterator<Item = &'static T>,
    type_name: impl Fn(&'static T) -> &'static str,
    what: &str,
) -> Vec<&'static T> {
    let mut seen = HashSet::new();
    grammars
        .inspect(|&g| {
            let type_name = type_name(g);
            assert!(
                seen.insert(type_name),
                "two {what} grammars are registered for type `{type_name}`"
            );
        })
        .collect()
}
