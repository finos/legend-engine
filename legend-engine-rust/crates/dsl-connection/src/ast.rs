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

//! The connection AST: packageable connections, and the connection values
//! runtimes embed too.

use std::any::Any;
use std::fmt;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use legend_pure_parser_parser::words::Name;

/// The section kind, `###Connection`, which is also the elements' kind.
pub const KIND: &str = "Connection";

/// `Type my::Connection { … }`: a packageable connection, the engine's
/// `PackageableConnection`.
#[derive(Debug, Clone, PartialEq)]
pub struct PackageableConnection {
    /// The package.
    pub package: Option<Package>,
    /// The name, positioned.
    pub name: SpannedString,
    /// The value.
    pub value: ConnectionValue,
    /// From the type to `}`.
    pub source_info: SourceInfo,
}

/// A connection's value, `Type { … }`: its type, and its body as the type's
/// grammar ([`ConnectionValueGrammar`](crate::grammar::ConnectionValueGrammar))
/// reads it.
#[derive(Debug, Clone)]
pub struct ConnectionValue {
    /// The type, as written: it names the grammar.
    pub type_name: Name,
    /// The body.
    pub body: Box<dyn ConnectionValueBody>,
    /// Whether the connection is a runtime's own, `#{ Type { … } }#`, which
    /// some grammars read differently.
    pub embedded: bool,
    /// Where the engine positions the value: a packageable connection's
    /// whole span, an embedded one's `Type { … }`.
    pub source_info: SourceInfo,
}

impl PartialEq for ConnectionValue {
    fn eq(&self, other: &Self) -> bool {
        self.type_name == other.type_name
            && *self.body == *other.body
            && self.embedded == other.embedded
            && self.source_info == other.source_info
    }
}

/// A connection value's body, as its type's grammar
/// ([`ConnectionValueGrammar`](crate::grammar::ConnectionValueGrammar))
/// reads it.
pub trait ConnectionValueBody: fmt::Debug + Send + Sync {
    /// Downcast to the grammar's concrete type.
    fn as_any(&self) -> &dyn Any;

    /// Clone into a new boxed trait object.
    fn clone_box(&self) -> Box<dyn ConnectionValueBody>;

    /// Equality with another body: `false` for another concrete type.
    fn eq_content(&self, other: &dyn ConnectionValueBody) -> bool;
}

impl Clone for Box<dyn ConnectionValueBody> {
    fn clone(&self) -> Self {
        self.clone_box()
    }
}

impl PartialEq for dyn ConnectionValueBody {
    fn eq(&self, other: &Self) -> bool {
        self.eq_content(other)
    }
}

impl Spanned for PackageableConnection {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for PackageableConnection {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl PackageableElement for PackageableConnection {
    fn package(&self) -> Option<&Package> {
        self.package.as_ref()
    }

    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for PackageableConnection {
    #[allow(clippy::unnecessary_literal_bound)]
    fn kind(&self) -> &str {
        KIND
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
