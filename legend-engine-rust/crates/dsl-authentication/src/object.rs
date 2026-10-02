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
use serde::ser::{Serialize, SerializeMap, Serializer};

/// An object of the engine's protocol: its type, its fields in order, and
/// its position when the walker gives it one. It serializes as the engine's
/// JSON of it: its `_type`, its fields, its `sourceInformation`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Object {
    /// Its `_type`, when it has one.
    pub json_type: Option<&'static str>,
    /// Its fields, named as in the protocol, each name once.
    pub fields: Vec<(&'static str, Field)>,
    /// Its position, when the walker gives it one.
    pub source_info: Option<SourceInfo>,
}

/// A field's value.
#[derive(Debug, Clone, PartialEq, Eq, serde::Serialize)]
#[serde(untagged)]
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
}

impl Serialize for Object {
    fn serialize<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        let mut map = serializer.serialize_map(None)?;
        if let Some(json_type) = self.json_type {
            map.serialize_entry("_type", json_type)?;
        }
        for (name, field) in &self.fields {
            map.serialize_entry(name, field)?;
        }
        if let Some(source_info) = &self.source_info {
            map.serialize_entry("sourceInformation", &SourceInformation::from(source_info))?;
        }
        map.end()
    }
}
