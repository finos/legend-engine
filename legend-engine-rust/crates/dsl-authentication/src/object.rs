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

//! What the authentication grammars read: the objects the engine's walkers
//! build, as they build them, and their JSON.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Map, Value, json};

/// An object of the engine's protocol: its type, its fields in order, and
/// its position when the walker gives it one.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Object {
    /// Its `_type`, when it has one.
    pub json_type: Option<&'static str>,
    /// Its fields, named as in the protocol.
    pub fields: Vec<(&'static str, Field)>,
    /// Its position, when the walker gives it one.
    pub source_info: Option<SourceInfo>,
}

/// A field's value.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Field {
    /// A string.
    Text(String),
    /// Strings.
    Texts(Vec<String>),
    /// An object.
    Object(Object),
}

impl Object {
    /// An object of `_type` `json_type`, without fields.
    #[must_use]
    pub fn new(json_type: &'static str, source_info: Option<SourceInfo>) -> Self {
        Self {
            json_type: Some(json_type),
            fields: Vec::new(),
            source_info,
        }
    }

    /// The object, with `field`.
    #[must_use]
    pub fn with(mut self, name: &'static str, field: Field) -> Self {
        self.fields.push((name, field));
        self
    }

    /// The engine's JSON of the object.
    #[must_use]
    pub fn to_json(&self) -> Value {
        let mut json = Map::new();
        if let Some(json_type) = self.json_type {
            json.insert("_type".to_string(), json!(json_type));
        }
        for (name, field) in &self.fields {
            let value = match field {
                Field::Text(text) => json!(text),
                Field::Texts(texts) => json!(texts),
                Field::Object(object) => object.to_json(),
            };
            json.insert((*name).to_string(), value);
        }
        if let Some(source_info) = &self.source_info {
            json.insert(
                "sourceInformation".to_string(),
                json!(SourceInformation::from(source_info)),
            );
        }
        Value::Object(json)
    }
}
