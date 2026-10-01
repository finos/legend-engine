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

//! The relational store's query generation configs
//! (`QueryGenerationConfigsParserGrammar` and its
//! `QueryGenerationConfigParseTreeWalker`):
//!
//! ```text
//! GenerationFeaturesConfig { ( enabled: [ 'FEATURE', … ]; | disabled: [ 'FEATURE', … ]; )* }
//! ```
//!
//! Positioned as the specification, where its walker reports a field written
//! twice.

use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::fields::optional;
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde_json::{Value, json};

use super::specification::{
    BodyUse, Specification, SpecificationBody, SpecificationGrammar, SpecificationKind,
    SpecificationSpan, downcast,
};
use super::values::{at_field, field, strings};

type R<T> = Result<T, ParseError>;

/// The relational store's query generation config grammars.
pub(crate) static CORE_QUERY_GENERATION_CONFIGS: [&(dyn SpecificationGrammar + Send + Sync); 1] =
    [&GenerationFeaturesConfigGrammar];

/// `GenerationFeaturesConfig`: features of the SQL generated, enabled or
/// disabled.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct GenerationFeaturesConfig {
    /// The features enabled, unescaped.
    pub enabled: Vec<String>,
    /// The features disabled, unescaped.
    pub disabled: Vec<String>,
}

body!(GenerationFeaturesConfig);

/// The `GenerationFeaturesConfig` grammar.
pub struct GenerationFeaturesConfigGrammar;

impl SpecificationGrammar for GenerationFeaturesConfigGrammar {
    fn kind(&self) -> SpecificationKind {
        SpecificationKind::QueryGenerationConfig
    }

    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        "GenerationFeaturesConfig"
    }

    fn body_use(&self) -> BodyUse {
        BodyUse::Required
    }

    fn parse(
        &self,
        span: &SpecificationSpan,
        body: Option<&mut ParserContext<'_>>,
    ) -> R<Box<dyn SpecificationBody>> {
        let (mut enabled, mut disabled) = (Vec::new(), Vec::new());
        if let Some(ctx) = body {
            loop {
                if at_field(ctx, "enabled") {
                    enabled.push(field(ctx, strings)?.0);
                } else if at_field(ctx, "disabled") {
                    disabled.push(field(ctx, strings)?.0);
                } else {
                    break;
                }
            }
        }
        let at = &span.source_info;
        Ok(Box::new(GenerationFeaturesConfig {
            enabled: optional(enabled, "enabled", at)?.unwrap_or_default(),
            disabled: optional(disabled, "disabled", at)?.unwrap_or_default(),
        }))
    }

    fn convert(&self, specification: &Specification) -> Result<Value, ConvertError> {
        let config = downcast::<GenerationFeaturesConfig>(specification)?;
        Ok(json!({
            "_type": "generationFeaturesConfig",
            "enabled": config.enabled,
            "disabled": config.disabled,
            "sourceInformation": SourceInformation::from(&specification.source_info),
        }))
    }
}
