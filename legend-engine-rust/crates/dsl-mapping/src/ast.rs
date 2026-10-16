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

//! The mapping AST: the `###Mapping` section's mappings, their includes,
//! elements and tests. A mapping element's body is its type's grammar's
//! ([`MappingElementBody`]).

use std::any::Any;
use std::fmt;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::data::EmbeddedDataValue;
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::test_assertion::TestAssertion;
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use legend_pure_parser_parser::words::{Name, QualifiedName};
use smol_str::SmolStr;

/// The section kind, `###Mapping`, which is also its mappings' kind.
pub const SECTION_KIND: &str = "Mapping";

/// A mapping: `Mapping my::M ( include … elements … MappingTests [ … ]
/// testSuites: [ … ] )`.
#[derive(Debug, Clone, PartialEq)]
pub struct Mapping {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// The mappings included.
    pub includes: Vec<MappingInclude>,
    /// The mapping elements: class, enumeration and association mappings.
    pub elements: Vec<MappingElement>,
    /// `MappingTests [ … ]`, the legacy tests.
    pub legacy_tests: Vec<LegacyTest>,
    /// `testSuites: [ … ]`; `None` without, as the engine leaves them null.
    pub test_suites: Option<Vec<TestSuite>>,
    /// From `Mapping` to `)`.
    pub source_info: SourceInfo,
}

/// `include my::Other` (or `include mapping my::Other`), with store
/// substitutions: `include my::Other[my::Db1 -> my::Db2]`; or
/// `include dataspace my::DataSpace`.
#[derive(Debug, Clone, PartialEq)]
pub struct MappingInclude {
    /// What is included.
    pub kind: IncludeKind,
    /// The included mapping's or data space's path, unquoted.
    pub path: String,
    /// The store substitutions.
    pub substitutions: Vec<StoreSubstitution>,
    /// From `include` to the path, or to `]`.
    pub source_info: SourceInfo,
}

/// What an include includes: the type the engine's lexer reads with
/// `include`, which names the extension that reads the include.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum IncludeKind {
    /// `include my::Other` or `include mapping my::Other`: a mapping.
    Mapping,
    /// `include dataspace my::DataSpace`: a data space's mapping, as the
    /// engine's data space extension reads it (`MappingIncludeDataSpace`),
    /// without its store substitutions.
    DataSpace,
}

/// `source -> target`: a store of the included mapping, and the store that
/// replaces it.
#[derive(Debug, Clone, PartialEq)]
pub struct StoreSubstitution {
    /// The included mapping's store, unquoted.
    pub source: String,
    /// The store that replaces it, unquoted.
    pub target: String,
}

/// What precedes a mapping element's body:
/// `*my::Person[id] extends [superId]: Type name`.
#[derive(Debug, Clone, PartialEq)]
pub struct MappingElementHeader {
    /// `*`: the class's root mapping.
    pub root: bool,
    /// The class, enumeration or association mapped.
    pub target: QualifiedName,
    /// `[id]`, which the walker reads as written.
    pub id: Option<Name>,
    /// `extends [id]`: the mapping the element extends, as written.
    pub extends: Option<Name>,
    /// The type after the colon, which names the body's grammar, as written.
    pub type_name: Name,
    /// The name after the type, as written: an enumeration mapping's id.
    pub name: Option<Name>,
}

/// A mapping element: `Person[p]: Pure { … }`.
#[derive(Debug, Clone)]
pub struct MappingElement {
    /// Its header.
    pub header: MappingElementHeader,
    /// Its body, as its type's grammar reads it.
    pub body: Box<dyn MappingElementBody>,
    /// From `*`, or the target, to `}`.
    pub source_info: SourceInfo,
}

impl PartialEq for MappingElement {
    fn eq(&self, other: &Self) -> bool {
        self.header == other.header
            && *self.body == *other.body
            && self.source_info == other.source_info
    }
}

/// A mapping element's body, as its type's grammar
/// ([`MappingElementGrammar`](crate::grammar::MappingElementGrammar))
/// reads it.
pub trait MappingElementBody: fmt::Debug + Send + Sync {
    /// Downcast to the grammar's concrete type.
    fn as_any(&self) -> &dyn Any;

    /// Clone into a new boxed trait object.
    fn clone_box(&self) -> Box<dyn MappingElementBody>;

    /// Equality with another body: `false` for another concrete type.
    fn eq_content(&self, other: &dyn MappingElementBody) -> bool;
}

impl Clone for Box<dyn MappingElementBody> {
    fn clone(&self) -> Self {
        self.clone_box()
    }
}

impl PartialEq for dyn MappingElementBody {
    fn eq(&self, other: &Self) -> bool {
        self.eq_content(other)
    }
}

/// A legacy test: `name ( query: …; data: [ … ]; assert: '…'; )`, the
/// engine's `MappingTest_Legacy`.
#[derive(Debug, Clone, PartialEq)]
pub struct LegacyTest {
    /// The name, as written.
    pub name: SmolStr,
    /// The query.
    pub query: Expression,
    /// The input data.
    pub inputs: Vec<TestInput>,
    /// The expected output, as written between the quotes.
    pub expected_output: String,
    /// From `assert` to `;`.
    pub assert_source_info: SourceInfo,
    /// From the name to `)`.
    pub source_info: SourceInfo,
}

/// A legacy test's input data: `<Type, Format?, source, 'data' + 'data'>`,
/// which the type's grammar
/// ([`TestInputGrammar`](crate::grammar::TestInputGrammar)) checks and
/// converts.
#[derive(Debug, Clone, PartialEq)]
pub struct TestInput {
    /// The type, as written: which grammar reads the input.
    pub type_name: Name,
    /// The format, as written.
    pub format: Option<Name>,
    /// Where the data goes: a class or a store.
    pub source: QualifiedName,
    /// The data's strings, as written, quotes included.
    pub data: Vec<SmolStr>,
    /// From `<` to `>`.
    pub source_info: SourceInfo,
}

/// A test suite: `id: { function: …; tests: [ … ]; }`, the engine's
/// `MappingTestSuite`.
#[derive(Debug, Clone, PartialEq)]
pub struct TestSuite {
    /// The id, unquoted.
    pub id: SmolStr,
    /// The documentation.
    pub doc: Option<String>,
    /// The query the tests run.
    pub function: Expression,
    /// The tests.
    pub tests: Vec<MappingTest>,
    /// From the id to `}`.
    pub source_info: SourceInfo,
}

/// A test: `id: { data: [ … ]; asserts: [ … ]; }`, the engine's
/// `MappingTest`.
#[derive(Debug, Clone, PartialEq)]
pub struct MappingTest {
    /// The id, unquoted.
    pub id: SmolStr,
    /// The documentation.
    pub doc: Option<String>,
    /// The assertions, each with its id, unquoted.
    pub assertions: Vec<(SmolStr, TestAssertion)>,
    /// The stores' data.
    pub store_data: Vec<StoreTestData>,
    /// From the id to `}`.
    pub source_info: SourceInfo,
}

/// A store's data for a test: `my::Store: Type #{ … }#`.
#[derive(Debug, Clone, PartialEq)]
pub struct StoreTestData {
    /// The store: as written when it is a single name, else unquoted, as
    /// the walker reads it.
    pub store: String,
    /// The store's name.
    pub store_source_info: SourceInfo,
    /// The data.
    pub data: EmbeddedDataValue,
    /// From the store to the end of the data.
    pub source_info: SourceInfo,
}

impl Spanned for Mapping {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for Mapping {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl PackageableElement for Mapping {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for Mapping {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        SECTION_KIND
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn DSLElement> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn DSLElement) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}
