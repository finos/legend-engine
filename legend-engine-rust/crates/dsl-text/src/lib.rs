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

//! # Legend Pure — texts
//!
//! The engine's `###Text` section (its `text` extension): texts, a content
//! of some type.
//!
//! ```pure
//! ###Text
//! Text my::Readme
//! {
//!   type: markdown;
//!   content: '# My model';
//! }
//! ```
//!
//! Mirrors the engine's `TextParserGrammar` and `TextParseTreeWalker`: the
//! section is read whole first, its texts' fields as blocks of fields
//! ([`legend_pure_parser_parser::blocks`]), then each text walked into a
//! protocol record ([`legend_pure_parser_protocol::v1::record`]).
//!
//! Crate layout:
//!
//! - [`TextElement`], a [`DSLElement`].
//! - [`section`] — [`section::TextSectionParser`] registers as
//!   [`legend_pure_parser_parser::SectionParser`] for `Text`, and
//!   [`section::TextProtocol`] as
//!   [`legend_pure_parser_protocol::ElementProtocol`] for its texts.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

use std::any::Any;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use legend_pure_parser_protocol::v1::record::Record;

pub mod section;

/// The section kind, `###Text`, which is also its texts' kind.
pub const SECTION_KIND: &str = "Text";

/// `Text my::T { type: …; content: '…'; }`.
#[derive(Debug, Clone, PartialEq)]
pub struct TextElement {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// The text's object, without its package and name.
    pub record: Record,
    /// From `Text` to `}`.
    pub source_info: SourceInfo,
}

impl Spanned for TextElement {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

/// A text has no annotations.
impl Annotated for TextElement {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl PackageableElement for TextElement {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for TextElement {
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
