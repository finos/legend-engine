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

use legend_pure_parser_ast::element::{Element, PrimitiveDef};
use legend_pure_parser_lexer::TokenKind;

use super::{Parser, R};

impl Parser {
    /// Parses: `Primitive [(<<stereos>> | {tags})*] fqn::Name[(params)] extends BaseType [\n constraints\n]`
    ///
    /// The optional `[…]` block holds constraints — same grammar as class
    /// constraints. Evaluated at cast/new time with `$this` bound to the
    /// value and any `type_variable_parameters` bound to their call-site
    /// type-variable-values.
    pub(crate) fn parse_primitive_def(&mut self) -> R<Element> {
        let si = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::Primitive)?;
        let header = self.parse_element_header()?;

        // Optional type variable parameters: (x:Integer[1])
        let type_variable_parameters = self.parse_type_variable_parameters()?;

        self.cursor.expect(TokenKind::Extends)?;
        let super_type = self.parse_type_reference()?;

        // Optional constraint block — reuse the class-constraint grammar.
        let constraints = if self.cursor.check(TokenKind::LBracket) {
            self.parse_constraints()?
        } else {
            vec![]
        };

        Ok(Element::Primitive(PrimitiveDef {
            package: header.package,
            name: header.name,
            type_variable_parameters,
            super_type,
            constraints,
            source_info: si,
        }))
    }
}
