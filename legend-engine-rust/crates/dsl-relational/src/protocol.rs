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

//! `EmbeddedDataProtocol` for relational test data: the engine's
//! `RelationalCSVData` (`tables` is always written, even empty: the engine's
//! class defaults it to null).
//!
//! ```json
//! {
//!     "_type": "relationalCSVData",
//!     "tables": [
//!         { "schema": "default", "table": "PersonTable", "values": "id,name\n1,John\n", "sourceInformation": { … } }
//!     ],
//!     "sourceInformation": { … }
//! }
//! ```

use legend_pure_parser_ast::data::EmbeddedDataContent;
use legend_pure_parser_protocol::EmbeddedDataProtocol;
use legend_pure_parser_protocol::embedded_data_protocol::{
    ConvertError, EMBEDDED_DATA_PROTOCOLS, Result,
};
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use linkme::distributed_slice;

use crate::ast::{RelationalData, TYPE_NAME};

/// Protocol converter for relational test data.
pub struct RelationalDataProtocol;

impl EmbeddedDataProtocol for RelationalDataProtocol {
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn convert(&self, content: &dyn EmbeddedDataContent) -> Result<serde_json::Value> {
        let data = content
            .as_any()
            .downcast_ref::<RelationalData>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "RelationalDataProtocol: '{}' data is not RelationalData",
                        content.type_name()
                    ),
                    None,
                )
            })?;
        let tables: Vec<serde_json::Value> = data
            .tables
            .iter()
            .map(|table| {
                serde_json::json!({
                    "schema": table.schema.as_str(),
                    "table": table.table.as_str(),
                    "values": table.values,
                    "sourceInformation": SourceInformation::from(&table.source_info),
                })
            })
            .collect();
        Ok(serde_json::json!({
            "_type": "relationalCSVData",
            "tables": tables,
            "sourceInformation": SourceInformation::from(&data.source_info),
        }))
    }
}

#[distributed_slice(EMBEDDED_DATA_PROTOCOLS)]
static RELATIONAL_DATA_PROTOCOL: &(dyn EmbeddedDataProtocol + Send + Sync) =
    &RelationalDataProtocol;
