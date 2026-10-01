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

//! The Snowflake AST: the `###Snowflake` section's apps and UDFs, each with
//! a [`Record`] in the shape of the engine's protocol object.

use std::any::Any;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use legend_pure_parser_protocol::v1::record::Record;

/// The section kind, `###Snowflake`, which is also its elements' kind.
pub const SECTION_KIND: &str = "Snowflake";

/// How a function is activated.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SnowflakeKind {
    /// `SnowflakeApp`: as a Snowflake app.
    App,
    /// `SnowflakeM2MUdf`: as a model-to-model UDF.
    M2MUdf,
}

/// `SnowflakeApp my::A { … }` or `SnowflakeM2MUdf my::U { … }`.
#[derive(Debug, Clone, PartialEq)]
pub struct SnowflakeElement {
    /// How its function is activated.
    pub kind: SnowflakeKind,
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// `<<profile.stereotype>>`.
    pub stereotypes: Vec<StereotypePtr>,
    /// `{profile.tag = 'value'}`, a documentation literal's first.
    pub tagged_values: Vec<TaggedValue>,
    /// The element's object, without its package, name and annotations.
    pub record: Record,
    /// From the documentation, or the keyword, to `}`.
    pub source_info: SourceInfo,
}

impl Spanned for SnowflakeElement {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for SnowflakeElement {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &self.stereotypes
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &self.tagged_values
    }
}

impl PackageableElement for SnowflakeElement {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for SnowflakeElement {
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
