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

//! The `###MongoDB` section: MongoDB databases, their collections and each
//! collection's JSON schema (`MongoDBSchemaParserGrammar` and
//! `MongoDBSchemaParseTreeWalker`, which reads the schema with the engine's
//! `MongoDBSchemaDeserializer`).
//!
//! ```pure
//! ###MongoDB
//! Database my::PeopleDatabase
//! (
//!   Collection people
//!   (
//!     validationLevel: strict;
//!     validationAction: warn;
//!     jsonSchema: {
//!       "bsonType": "object",
//!       "properties": { "name": { "bsonType": "string" } }
//!     };
//!   )
//! )
//! ```
//!
//! The section is read whole first, then each database walked into a
//! protocol record ([`legend_pure_parser_protocol::v1::record`]). The walker
//! keeps only collections with a schema: includes, joins and views are
//! read and left out. A schema the engine's deserializer fails on (its
//! `bsonType` missing, or a type it doesn't read) is unsupported.

use std::any::Any;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use legend_pure_parser_protocol::v1::record::Record;

mod json;
mod read;
pub mod section;
mod walk;

/// The section kind, `###MongoDB`, which is also its databases' kind.
pub const SECTION_KIND: &str = "MongoDB";

/// `Database my::D ( … )`.
#[derive(Debug, Clone, PartialEq)]
pub struct MongoDatabase {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// The database's object, without its package and name.
    pub record: Record,
    /// From `Database` to `)`.
    pub source_info: SourceInfo,
}

impl Spanned for MongoDatabase {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

/// A database has no annotations.
impl Annotated for MongoDatabase {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl PackageableElement for MongoDatabase {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for MongoDatabase {
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
