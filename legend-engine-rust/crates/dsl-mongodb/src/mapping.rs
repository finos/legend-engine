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

//! `MongoDB` mapping elements (`MongoDBMappingParserGrammar` and its
//! `MongoDBMappingParseTreeWalker`), the engine's `RootMongoDBClassMapping`.
//!
//! ```text
//! classMapping: ( ~filter [my::Db] name )?
//!               ( ~mainCollection [my::Db] name(.scope(.scope)?)? | ~binding my::Binding )*
//! ```
//!
//! The walker ignores the filter, and keeps the main collection (its name
//! and its store) and the binding each only when written once. The engine's
//! protocol has no source information for these class mappings.
//!
//! The engine positions a syntax error in the body as if the body started
//! where the section does (the grammar's error listener gets the section's
//! positions, not the body's); the Rust parser reports it where it is.

use std::any::Any;

use legend_pure_dsl_mapping::ast::{MappingElement, MappingElementBody, MappingElementHeader};
use legend_pure_dsl_mapping::grammar::{
    ElementSpan, MAPPING_ELEMENT_GRAMMARS, MappingElementGrammar, MappingElementKind,
};
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{eat_command, identifier, qualified_name};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use linkme::distributed_slice;
use serde_json::{Value, json};
use smol_str::SmolStr;

/// The element type this grammar reads.
pub const TYPE_NAME: &str = "MongoDB";

/// A MongoDB class mapping's body.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MongoDbBody {
    /// `~filter [my::Db] name`.
    pub filter: Option<Collection>,
    /// `~mainCollection [my::Db] name`, in order.
    pub main_collections: Vec<Collection>,
    /// `~binding my::Binding`, as written (`getText()`), in order.
    pub bindings: Vec<String>,
}

/// `[my::Db] name.scope`: a store's collection.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Collection {
    /// The store, as written (`getText()`).
    pub store: String,
    /// The collection, unquoted.
    pub name: SmolStr,
    /// The names after the collection's, unquoted: at most two.
    pub scope: Vec<SmolStr>,
}

impl MappingElementBody for MongoDbBody {
    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn MappingElementBody> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn MappingElementBody) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}

/// The MongoDB mapping element grammar.
pub struct MongoDbMappingGrammar;

impl MappingElementGrammar for MongoDbMappingGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(
        &self,
        _header: &MappingElementHeader,
        _span: &ElementSpan,
        ctx: &mut ParserContext<'_>,
    ) -> Result<Box<dyn MappingElementBody>, ParseError> {
        let filter = if eat_command(ctx, "filter") {
            Some(collection(ctx, 0)?)
        } else {
            None
        };
        let (mut main_collections, mut bindings) = (Vec::new(), Vec::new());
        loop {
            if eat_command(ctx, "mainCollection") {
                main_collections.push(collection(ctx, 2)?);
            } else if eat_command(ctx, "binding") {
                bindings.push(qualified_name(ctx)?.text);
            } else {
                break;
            }
        }
        Ok(Box::new(MongoDbBody {
            filter,
            main_collections,
            bindings,
        }))
    }

    fn convert(
        &self,
        element: &MappingElement,
    ) -> Result<(MappingElementKind, Value), ConvertError> {
        let body = element
            .body
            .as_any()
            .downcast_ref::<MongoDbBody>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "MongoDbMappingGrammar: the body is not a MongoDB mapping's",
                    Some(&element.source_info),
                )
            })?;
        let header = &element.header;
        let mut json = json!({
            "_type": TYPE_NAME,
            "class": header.target.path,
            "root": header.root,
        });
        if let Some(id) = &header.id {
            json["id"] = json!(id.text.as_str());
        }
        if let [binding] = body.bindings.as_slice() {
            json["bindingPath"] = json!(binding);
        }
        if let [collection] = body.main_collections.as_slice() {
            json["mainCollectionName"] = json!(collection.name.as_str());
            json["storePath"] = json!(collection.store);
        }
        Ok((MappingElementKind::Class, json))
    }
}

/// `[my::Db] name`, then up to `scope` names, each after a `.`.
fn collection(ctx: &mut ParserContext<'_>, scope: usize) -> Result<Collection, ParseError> {
    ctx.cursor().expect(TokenKind::LBracket)?;
    let store = qualified_name(ctx)?.text;
    ctx.cursor().expect(TokenKind::RBracket)?;
    let name = identifier(ctx)?.value;
    let mut names = Vec::new();
    while names.len() < scope && ctx.cursor().eat(TokenKind::Dot) {
        names.push(identifier(ctx)?.value);
    }
    Ok(Collection {
        store,
        name,
        scope: names,
    })
}

#[distributed_slice(MAPPING_ELEMENT_GRAMMARS)]
static MONGODB_MAPPING_GRAMMAR: &(dyn MappingElementGrammar + Send + Sync) = &MongoDbMappingGrammar;
