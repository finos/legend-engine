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
use legend_pure_parser_ast::element::{AssociationDef, Element};
use legend_pure_parser_lexer::TokenKind;

impl Parser {
    // ── Association ─────────────────────────────────────────────────────

    pub(crate) fn parse_association(&mut self) -> R<Element> {
        let start = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::Association)?;
        let header = self.parse_element_header()?;
        self.cursor.expect(TokenKind::LBrace)?;
        let (properties, qualified_properties) = self.parse_class_body()?;
        let close = self.cursor.expect(TokenKind::RBrace)?;
        Ok(Element::Association(AssociationDef {
            package: header.package,
            name: header.name,
            properties,
            qualified_properties,
            stereotypes: header.stereotypes,
            tagged_values: header.tagged_values,
            source_info: start.merge(&close.source_info),
        }))
    }
}
