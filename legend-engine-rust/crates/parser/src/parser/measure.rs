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
use legend_pure_parser_ast::element::{Element, MeasureDef, UnitDef};
use legend_pure_parser_lexer::TokenKind;

impl Parser {
    // ── Measure ─────────────────────────────────────────────────────────

    pub(crate) fn parse_measure(&mut self) -> R<Element> {
        let start = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::Measure)?;
        let header = self.parse_element_header()?;
        self.cursor.expect(TokenKind::LBrace)?;
        let mut canonical_unit = None;
        let mut non_canonical_units = Vec::new();
        while !self.cursor.check(TokenKind::RBrace) {
            let is_canonical = self.cursor.eat(TokenKind::Star);
            let unit = self.parse_unit_def()?;
            if is_canonical {
                canonical_unit = Some(unit);
            } else {
                non_canonical_units.push(unit);
            }
        }
        let close = self.cursor.expect(TokenKind::RBrace)?;
        Ok(Element::Measure(MeasureDef {
            package: header.package,
            name: header.name,
            canonical_unit,
            non_canonical_units,
            source_info: start.merge(&close.source_info),
        }))
    }

    pub(crate) fn parse_unit_def(&mut self) -> R<UnitDef> {
        let (name, si) = self.cursor.expect_identifier_or_keyword()?;
        if self.cursor.eat(TokenKind::Colon) {
            let (param, _) = self.cursor.expect_identifier()?;
            self.cursor.expect(TokenKind::Arrow)?;
            let body = self.parse_expression()?;
            self.cursor.expect(TokenKind::Semicolon)?;
            Ok(UnitDef {
                name,
                conversion_param: Some(param),
                conversion_body: Some(body),
                source_info: self.cursor.span_since(&si),
            })
        } else {
            self.cursor.expect(TokenKind::Semicolon)?;
            Ok(UnitDef {
                name,
                conversion_param: None,
                conversion_body: None,
                source_info: self.cursor.span_since(&si),
            })
        }
    }
}
