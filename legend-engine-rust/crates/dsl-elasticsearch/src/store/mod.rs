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

//! The `###Elasticsearch` section: Elasticsearch 7 clusters, their indices
//! and their properties' mappings (`ElasticsearchParserGrammar` and
//! `ElasticsearchStoreParseTreeWalker`).
//!
//! ```pure
//! ###Elasticsearch
//! Elasticsearch7Cluster my::Store
//! {
//!   indices: [
//!     people: {
//!       properties: [
//!         name: Text { fields: [ raw: Keyword ]; },
//!         address: Object { properties: [ city: Keyword ]; }
//!       ];
//!     }
//!   ];
//! }
//! ```
//!
//! The section is read whole first, as blocks of fields
//! ([`legend_pure_parser_parser::blocks`]), then each store walked into a
//! protocol record ([`legend_pure_parser_protocol::v1::record`]). Its
//! fields are named as the walker names them, by the lexer's literal names
//! (`'indices'`), which its messages quote again.

use std::any::Any;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use legend_pure_parser_protocol::v1::record::Record;

mod read;
pub mod section;
mod walk;

/// The section kind, `###Elasticsearch`, which is also its stores' kind.
pub const SECTION_KIND: &str = "Elasticsearch";

/// `Elasticsearch7Cluster my::S { … }`.
#[derive(Debug, Clone, PartialEq)]
pub struct ElasticsearchStore {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// The store's object, without its package and name.
    pub record: Record,
    /// From `Elasticsearch7Cluster` to `}`.
    pub source_info: SourceInfo,
}

impl Spanned for ElasticsearchStore {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

/// A store has no annotations.
impl Annotated for ElasticsearchStore {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl PackageableElement for ElasticsearchStore {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for ElasticsearchStore {
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
