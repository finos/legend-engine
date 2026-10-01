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
use crate::error::ParseError;
use legend_pure_parser_ast::annotation::SpannedString;
use legend_pure_parser_ast::element::{Element, ProfileDef};
use legend_pure_parser_lexer::TokenKind;

impl Parser {
    // ── Profile ─────────────────────────────────────────────────────────

    pub(crate) fn parse_profile(&mut self) -> R<Element> {
        let start = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::Profile)?;
        let header = self.parse_element_header()?;
        self.cursor.expect(TokenKind::LBrace)?;

        let mut stereotypes = Vec::new();
        let mut tags = Vec::new();

        while !self.cursor.check(TokenKind::RBrace) {
            if self.cursor.check(TokenKind::Stereotypes) {
                self.cursor.advance();
                self.cursor.expect(TokenKind::Colon)?;
                self.cursor.expect(TokenKind::LBracket)?;
                loop {
                    let (val, si) = self.cursor.expect_identifier_or_keyword()?;
                    stereotypes.push(SpannedString {
                        value: val,
                        source_info: si,
                    });
                    if !self.cursor.eat(TokenKind::Comma) {
                        break;
                    }
                }
                self.cursor.expect(TokenKind::RBracket)?;
                self.cursor.expect(TokenKind::Semicolon)?;
            } else if self.cursor.check(TokenKind::Tags) {
                self.cursor.advance();
                self.cursor.expect(TokenKind::Colon)?;
                self.cursor.expect(TokenKind::LBracket)?;
                loop {
                    let (val, si) = self.cursor.expect_identifier_or_keyword()?;
                    tags.push(SpannedString {
                        value: val,
                        source_info: si,
                    });
                    if !self.cursor.eat(TokenKind::Comma) {
                        break;
                    }
                }
                self.cursor.expect(TokenKind::RBracket)?;
                self.cursor.expect(TokenKind::Semicolon)?;
            } else {
                return Err(ParseError::expected(
                    "'stereotypes' or 'tags'",
                    self.cursor.peek_kind(),
                    self.cursor.current_source_info(),
                ));
            }
        }
        let close = self.cursor.expect(TokenKind::RBrace)?;
        Ok(Element::Profile(ProfileDef {
            package: header.package,
            name: header.name,
            stereotype_names: stereotypes,
            tag_names: tags,
            stereotypes: header.stereotypes,
            tagged_values: header.tagged_values,
            source_info: start.merge(&close.source_info),
        }))
    }
}
