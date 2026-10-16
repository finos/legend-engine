// Copyright 2026 The Legend Authors
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

use super::Parser;
use super::R;
use legend_pure_parser_ast::element::{Element, EnumDef, EnumValue};
use legend_pure_parser_lexer::TokenKind;

impl Parser {
    // ── Enum ────────────────────────────────────────────────────────────

    pub(crate) fn parse_enum(&mut self) -> R<Element> {
        let start = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::Enum)?;
        let header = self.parse_element_header()?;
        self.cursor.expect(TokenKind::LBrace)?;
        let values = self.parse_separated(TokenKind::RBrace, |p| {
            // The engine's `enumValue`: documentation, stereotypes and
            // tagged values included.
            let value_start = p.cursor.current_source_info();
            let documentation = p
                .at_enum_value_documentation()
                .then(|| p.cursor.advance().clone());
            let val_stereos = p.parse_stereotypes()?;
            let val_tvs = Self::with_documentation(documentation, p.parse_tagged_values()?)?;
            let (val_name, _) = p.cursor.expect_identifier_or_keyword()?;
            Ok(EnumValue {
                name: val_name,
                stereotypes: val_stereos,
                tagged_values: val_tvs,
                source_info: p.cursor.span_since(&value_start),
            })
        })?;
        let close = self.cursor.expect(TokenKind::RBrace)?;
        Ok(Element::Enumeration(EnumDef {
            package: header.package,
            name: header.name,
            values,
            stereotypes: header.stereotypes,
            tagged_values: header.tagged_values,
            source_info: start.merge(&close.source_info),
        }))
    }
}
