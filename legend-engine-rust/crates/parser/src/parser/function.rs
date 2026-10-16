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
use super::test_assertion::expected_relation;
use crate::embedded_data::RELATION;
use crate::error::ParseError;
use legend_pure_parser_ast::annotation::PackageableElementPtr;
use legend_pure_parser_ast::annotation::Parameter;
use legend_pure_parser_ast::data::EmbeddedData;
use legend_pure_parser_ast::element::{
    Element, EmbeddedDataValue, ExpectedRelation, ExternalFormatValue, FunctionDef, FunctionTest,
    FunctionTestAssertion, FunctionTestData, FunctionTestDataValue, FunctionTestExpected,
    NativeFunctionDef,
};
use legend_pure_parser_lexer::TokenKind;
use smol_str::SmolStr;

impl Parser {
    // ── Function ────────────────────────────────────────────────────────

    pub(crate) fn parse_function(&mut self) -> R<Element> {
        let start = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::Function)?;
        let header = self.parse_element_header()?;
        let type_and_multiplicity_parameters = self.parse_type_and_multiplicity_parameters()?;
        self.cursor.expect(TokenKind::LParen)?;
        let parameters = self.parse_separated(TokenKind::RParen, Self::parse_parameter)?;
        self.cursor.expect(TokenKind::RParen)?;
        self.cursor.expect(TokenKind::Colon)?;
        let return_type = self.parse_type_spec()?;
        self.cursor.expect(TokenKind::LBracket)?;
        let return_multiplicity = self.parse_multiplicity()?;
        self.cursor.expect(TokenKind::RBracket)?;
        // Optional constraint block: `[name : expr, ...]` between the
        // return-multiplicity and the body. Reuses `parse_constraints`
        // from `class.rs` — same shape as `ClassDef.constraints`.
        let constraints = if self.cursor.check(TokenKind::LBracket) {
            self.parse_constraints()?
        } else {
            vec![]
        };
        self.cursor.expect(TokenKind::LBrace)?;
        let body = self.parse_expression_list()?;
        self.cursor.expect(TokenKind::RBrace)?;
        // Optional function test block: { testName | func(args) => expected; }
        let tests = if self.cursor.check(TokenKind::LBrace) {
            self.parse_function_tests()?
        } else {
            vec![]
        };
        // The whole definition, test block included (the engine's
        // `functionDefinition`).
        let span = self.cursor.span_since(&start);
        Ok(Element::Function(FunctionDef {
            package: header.package,
            name: header.name,
            type_and_multiplicity_parameters,
            parameters,
            return_type,
            return_multiplicity,
            constraints,
            body,
            stereotypes: header.stereotypes,
            tagged_values: header.tagged_values,
            tests,
            source_info: span,
        }))
    }

    // ── Native Function ─────────────────────────────────────────────────

    /// Parse `native function pkg::name(params): ReturnType[mult];`
    pub(crate) fn parse_native_function(&mut self) -> R<Element> {
        let start = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::Native)?;
        self.cursor.expect(TokenKind::Function)?;
        let header = self.parse_element_header()?;
        let type_and_multiplicity_parameters = self.parse_type_and_multiplicity_parameters()?;
        self.cursor.expect(TokenKind::LParen)?;
        let parameters = self.parse_separated(TokenKind::RParen, Self::parse_parameter)?;
        self.cursor.expect(TokenKind::RParen)?;
        self.cursor.expect(TokenKind::Colon)?;
        let return_type = self.parse_type_spec()?;
        self.cursor.expect(TokenKind::LBracket)?;
        let return_multiplicity = self.parse_multiplicity()?;
        self.cursor.expect(TokenKind::RBracket)?;
        let semi = self.cursor.expect(TokenKind::Semicolon)?;
        Ok(Element::NativeFunction(NativeFunctionDef {
            package: header.package,
            name: header.name,
            type_and_multiplicity_parameters,
            parameters,
            return_type,
            return_multiplicity,
            stereotypes: header.stereotypes,
            tagged_values: header.tagged_values,
            source_info: start.merge(&semi.source_info),
        }))
    }

    /// Parse function test block: `{ (data | assertion | suite)* }`
    ///
    /// Grammar:
    /// ```text
    /// functionTestSuiteDef: '{' (simpleFunctionTest | simpleFunctionSuite | functionData)* '}'
    /// simpleFunctionSuite:  identifier '(' (functionData)* simpleFunctionTest+ ')'
    /// functionData:         qualifiedName ':' functionDataValue ';'
    /// simpleFunctionTest:   identifier (STRING)? '|' identifier '(' params ')' '=>' (externalFormatValue | primitiveValue) ';'
    /// ```
    pub(crate) fn parse_function_tests(&mut self) -> R<Vec<FunctionTest>> {
        let start = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::LBrace)?;

        let mut data = Vec::new();
        let mut assertions = Vec::new();
        let mut named_suites = Vec::new();

        while !self.cursor.check(TokenKind::RBrace) {
            // Data bindings have a ':' after the full qualified name, tests a
            // '|' (possibly after a doc string), named suites a '('.
            if self.is_test_data_binding() {
                data.push(self.parse_function_test_data()?);
            } else if self.is_named_suite() {
                named_suites.push(self.parse_named_suite()?);
            } else {
                assertions.push(self.parse_function_test_assertion()?);
            }
        }
        self.cursor.expect(TokenKind::RBrace)?;

        // The block's own tests and data form the engine's default suite,
        // ahead of the named ones, positioned on the whole block.
        if !data.is_empty() || !assertions.is_empty() {
            named_suites.insert(
                0,
                FunctionTest {
                    name: None,
                    data,
                    assertions,
                    source_info: self.cursor.span_since(&start),
                },
            );
        }
        Ok(named_suites)
    }

    /// Lookahead: is this a test data binding? (`qualifiedName ':' ...`)
    ///
    /// Data bindings look like: `store::MyStore: (JSON) '{}';`
    /// Assertions look like: `testName | func() => result;`
    ///
    /// We scan forward from the current token, skipping `identifier ::` pairs,
    /// until we see `:` (data) or `|`/STRING (assertion).
    pub(crate) fn is_test_data_binding(&self) -> bool {
        let mut offset = 0;
        // Skip the first identifier
        if self.cursor.peek_kind_at(offset) != TokenKind::Identifier {
            return false;
        }
        offset += 1;

        // Skip `:: identifier` path segments
        while self.cursor.peek_kind_at(offset) == TokenKind::PathSep {
            offset += 1; // skip ::
            offset += 1; // skip identifier
        }

        // If next is `:`, it's a data binding
        self.cursor.peek_kind_at(offset) == TokenKind::Colon
    }

    /// Lookahead: is this a named test suite? (`identifier '(' ...`)
    pub(crate) fn is_named_suite(&self) -> bool {
        self.cursor.peek_kind() == TokenKind::Identifier
            && self.cursor.peek_kind_at(1) == TokenKind::LParen
    }

    /// Parse a named test suite: `SuiteName ( data* assertion+ )`
    pub(crate) fn parse_named_suite(&mut self) -> R<FunctionTest> {
        let start = self.cursor.current_source_info();
        let (name, _) = self.cursor.expect_identifier_or_keyword()?;
        self.cursor.expect(TokenKind::LParen)?;

        let mut data = Vec::new();
        let mut assertions = Vec::new();

        while !self.cursor.check(TokenKind::RParen) {
            if self.is_test_data_binding() {
                data.push(self.parse_function_test_data()?);
            } else {
                assertions.push(self.parse_function_test_assertion()?);
            }
        }
        self.cursor.expect(TokenKind::RParen)?;

        Ok(FunctionTest {
            name: Some(name),
            data,
            assertions,
            source_info: self.cursor.span_since(&start),
        })
    }

    /// Parse a test data binding: `qualifiedName: dataValue;`
    ///
    /// Data values:
    /// - External format: `(JSON) '{...}'`
    /// - Data element: `testing::MyReference`
    /// - Embedded: `Relation #{ content }#`
    pub(crate) fn parse_function_test_data(&mut self) -> R<FunctionTestData> {
        let start = self.cursor.current_source_info();
        let store = self.parse_element_pointer()?;
        self.cursor.expect(TokenKind::Colon)?;
        let data = if self.cursor.check(TokenKind::LParen) {
            FunctionTestDataValue::ExternalFormat(self.parse_external_format_value()?)
        } else if self.at_embedded_data() {
            FunctionTestDataValue::Embedded(self.parse_embedded_data()?)
        } else {
            FunctionTestDataValue::Reference(self.parse_element_pointer()?)
        };
        self.cursor.expect(TokenKind::Semicolon)?;
        Ok(FunctionTestData {
            store,
            data,
            source_info: self.cursor.span_since(&start),
        })
    }

    /// Parse: `testName ('doc')? | funcCall(args) => expected;`, where the
    /// expected result is a value, `(format) 'data'` or embedded data.
    pub(crate) fn parse_function_test_assertion(&mut self) -> R<FunctionTestAssertion> {
        let start = self.cursor.current_source_info();
        let (test_name, _) = self.cursor.expect_identifier_or_keyword()?;
        let doc = if self.cursor.check(TokenKind::StringLiteral) {
            Some(SmolStr::new(self.parse_string_value()?))
        } else {
            None
        };
        self.cursor.expect(TokenKind::Pipe)?;
        let invocation = self.parse_expression()?;
        self.cursor.expect(TokenKind::FatArrow)?;
        let expected = if self.cursor.check(TokenKind::LParen)
            && self.cursor.peek_kind_at(1) == TokenKind::Identifier
            && self.cursor.peek_kind_at(2) == TokenKind::RParen
        {
            FunctionTestExpected::ExternalFormat(self.parse_external_format_value()?)
        } else if self.at_embedded_data() {
            self.parse_expected_embedded_data()?
        } else {
            FunctionTestExpected::Value(Box::new(self.parse_expression()?))
        };
        self.cursor.expect(TokenKind::Semicolon)?;
        Ok(FunctionTestAssertion {
            name: test_name,
            doc,
            invocation,
            expected,
            source_info: self.cursor.span_since(&start),
        })
    }

    /// A qualified name, positioned on the whole path.
    pub(super) fn parse_element_pointer(&mut self) -> R<PackageableElementPtr> {
        let start = self.cursor.current_source_info();
        let path = self.parse_package_path()?;
        let (package, name) = super::split_package_name(&path);
        Ok(PackageableElementPtr {
            package,
            name,
            source_info: self.cursor.span_since(&start),
        })
    }

    /// `(format) 'data'`.
    fn parse_external_format_value(&mut self) -> R<ExternalFormatValue> {
        let start = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::LParen)?;
        let (format, _) = self.cursor.expect_identifier_or_keyword()?;
        self.cursor.expect(TokenKind::RParen)?;
        let content = self.parse_string_value()?;
        Ok(ExternalFormatValue {
            format,
            content,
            source_info: self.cursor.span_since(&start),
        })
    }

    /// A string literal's value, unescaped as the engine does.
    fn parse_string_value(&mut self) -> R<String> {
        let token = self.cursor.expect(TokenKind::StringLiteral)?;
        super::strings::string_value(&token.text)
            .map(|(value, _)| value)
            .map_err(|message| ParseError::unexpected(message, token.source_info.clone()))
    }

    /// A test's expected embedded data: a relation, read as mapping and
    /// service tests read one. Other types are kept as written: the engine
    /// accepts only relations here.
    fn parse_expected_embedded_data(&mut self) -> R<FunctionTestExpected> {
        let island = self.read_typed_island()?;
        if island.type_name != RELATION {
            return Ok(FunctionTestExpected::Embedded(EmbeddedDataValue {
                type_name: island.type_name,
                data: EmbeddedData::Text(island.body.text),
                source_info: island.source_info,
            }));
        }
        Ok(FunctionTestExpected::Relation(ExpectedRelation {
            element: expected_relation(&island)?,
            source_info: island.source_info,
        }))
    }

    pub(crate) fn parse_parameter(&mut self) -> R<Parameter> {
        let start = self.cursor.current_source_info();
        let (name, _) = self.cursor.expect_identifier_or_keyword()?;
        self.cursor.expect(TokenKind::Colon)?;
        // Any type: a relation type `(col:Type, ...)` included.
        let type_ref = self.parse_type_spec()?;
        self.cursor.expect(TokenKind::LBracket)?;
        let multiplicity = self.parse_multiplicity()?;
        self.cursor.expect(TokenKind::RBracket)?;
        Ok(Parameter {
            name,
            type_ref: Some(type_ref),
            multiplicity: Some(multiplicity),
            // `name: Type[mult]`, as the engine positions parameters.
            source_info: self.cursor.span_since(&start),
        })
    }
}
