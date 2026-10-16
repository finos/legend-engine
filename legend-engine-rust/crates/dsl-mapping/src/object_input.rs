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

//! The engine core's `Object` test input data,
//! `<Object, JSON, my::Class, '{…}'>`: instances of a class, in JSON or XML
//! (`CorePureGrammarParser.parseObjectInputData`, the engine's
//! `ObjectInputData`).

use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::raw_string_value;
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde::Serialize;

use crate::ast::TestInput;
use crate::grammar::TestInputGrammar;

/// The input's type.
pub const TYPE_NAME: &str = "Object";

/// The formats the engine's `ObjectInputType` has.
const FORMATS: [&str; 2] = ["JSON", "XML"];

/// The `Object` test input grammar.
pub struct ObjectInputGrammar;

impl TestInputGrammar for ObjectInputGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn check(&self, input: &TestInput) -> Result<(), ParseError> {
        let possible = FORMATS.join(", ");
        let Some(format) = &input.format else {
            return Err(ParseError::Unexpected {
                message: format!(
                    "Mapping test object 'input type' is missing. Possible values: {possible}"
                ),
                source_info: input.source_info.clone(),
            });
        };
        if !FORMATS.contains(&format.text.as_str()) {
            return Err(ParseError::Unexpected {
                message: format!(
                    "Mapping test object input data does not support format '{}'. Possible \
                     values: {possible}",
                    format.text
                ),
                source_info: format.source_info.clone(),
            });
        }
        Ok(())
    }

    fn convert(&self, input: &TestInput) -> Result<serde_json::Value, ConvertError> {
        Ok(serde_json::to_value(ObjectInputData {
            source_class: input.source.path.clone(),
            // The strings as written, joined.
            data: input.data.iter().map(|s| raw_string_value(s)).collect(),
            input_type: input
                .format
                .as_ref()
                .map(|format| format.text.to_string())
                .unwrap_or_default(),
            source_information: SourceInformation::from(&input.source_info),
        })?)
    }
}

/// The engine's `ObjectInputData`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "object", rename_all = "camelCase")]
struct ObjectInputData {
    source_class: String,
    data: String,
    input_type: String,
    source_information: SourceInformation,
}
