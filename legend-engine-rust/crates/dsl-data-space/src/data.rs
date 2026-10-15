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

//! `DataspaceTestData #{ my::DataSpace }#`: a data space's data as embedded
//! data, the engine's `DataspaceDataElementReferenceParser`. Like the
//! core's `Reference`, it reads a reference
//! ([`EmbeddedData::Reference`]), here to a data space, which may stand as
//! an execution context's test data.

use legend_pure_parser_ast::data::{DataElementReference, EmbeddedData};
use legend_pure_parser_parser::embedded_data::{EMBEDDED_DATA_PARSERS, element_path};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::{EmbeddedDataParser, ParserContext};
use linkme::distributed_slice;

/// The data type.
const TYPE_NAME: &str = "DataspaceTestData";

/// Parser for `DataspaceTestData #{ … }#`.
pub struct DataSpaceTestDataParser;

impl EmbeddedDataParser for DataSpaceTestDataParser {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    /// The engine reads the body's text, not its tokens.
    fn raw_body(&self) -> bool {
        true
    }

    fn parse(&self, ctx: &mut ParserContext<'_>) -> Result<EmbeddedData, ParseError> {
        let span = ctx
            .island_span()
            .cloned()
            .unwrap_or_else(|| ctx.cursor().current_source_info());
        let text = ctx.island_text().unwrap_or_default();
        Ok(EmbeddedData::Reference(DataElementReference {
            data_element: element_path(text, "DataspaceDataElementReference", &span)?,
            element_type: "DATASPACE".into(),
        }))
    }
}

#[distributed_slice(EMBEDDED_DATA_PARSERS)]
static DATA_SPACE_TEST_DATA_PARSER: &(dyn EmbeddedDataParser + Send + Sync) =
    &DataSpaceTestDataParser;
