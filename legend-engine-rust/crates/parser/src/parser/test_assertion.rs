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

//! Test assertions, `Type #{ … }#`, as the engine reads them in mapping and
//! service tests: `HelperTestAssertionGrammarParser.parseTestAssertion` with
//! the engine core's `EqualTo` and `EqualToJson` grammars, and `Relation`,
//! which those tests' walkers read themselves as an `EqualToRelation`.

use legend_pure_parser_ast::data::RelationElement;
use legend_pure_parser_ast::test_assertion::TestAssertion;
use legend_pure_parser_lexer::TokenKind;

use super::block::TypedIsland;
use super::data::origin;
use super::expression::parse_island_body;
use super::{Parser, ParserContext, R, relation_data};
use crate::embedded_data::{EXTERNAL_FORMAT, RELATION};
use crate::error::ParseError;

/// The engine core's assertion that the result equals a value.
pub const EQUAL_TO: &str = "EqualTo";

/// The engine core's assertion that the result equals external format data.
pub const EQUAL_TO_JSON: &str = "EqualToJson";

impl Parser {
    /// A test assertion, from its typed island.
    pub(crate) fn parse_test_assertion(&mut self, island: &TypedIsland) -> R<TestAssertion> {
        let source_info = island.source_info.clone();
        match island.type_name.as_str() {
            RELATION => {
                return Ok(TestAssertion::EqualToRelation {
                    expected: expected_relation(island)?,
                    source_info,
                });
            }
            EQUAL_TO | EQUAL_TO_JSON => {}
            other => {
                return Err(ParseError::Unsupported {
                    message: format!(
                        "no test assertion grammar is linked for '{other}'; the linked ones are: \
                         {EQUAL_TO}, {EQUAL_TO_JSON}, {RELATION}"
                    ),
                    source_info: island.type_source_info.clone(),
                });
            }
        }
        if island.body.text.is_empty() {
            return Err(ParseError::unexpected(
                "Test Assertion must not be empty",
                source_info,
            ));
        }
        let equal_to = island.type_name == EQUAL_TO;
        parse_island_body(&island.body, source_info.clone(), false, |ctx| {
            let keyword = ctx.cursor().peek().clone();
            if !(keyword.kind.is_identifier_like() && keyword.text == "expected") {
                return Err(ParseError::unexpected(
                    format!("Unexpected token '{}'", keyword.text),
                    keyword.source_info,
                ));
            }
            ctx.cursor().advance();
            ctx.cursor().expect(TokenKind::Colon)?;
            let assertion = if equal_to {
                TestAssertion::EqualTo {
                    expected: ctx.parse_primitive_value()?,
                    source_info,
                }
            } else {
                let expected = ctx.parse_embedded_data()?;
                // The engine's `EqualToJsonGrammarParser` casts the data to
                // external format data.
                if expected.type_name != EXTERNAL_FORMAT {
                    return Err(ParseError::unexpected(
                        format!(
                            "{EQUAL_TO_JSON} expects {EXTERNAL_FORMAT} data, not '{}'",
                            expected.type_name
                        ),
                        expected.source_info,
                    ));
                }
                TestAssertion::EqualToJson {
                    expected,
                    source_info,
                }
            };
            ctx.cursor().expect(TokenKind::Semicolon)?;
            Ok(assertion)
        })
    }
}

/// An expected relation, `Relation #{ … }#`, as the engine's
/// `HelperTestAssertionGrammarParser.parseRelationElement` reads it: the
/// body trimmed, behind `: `, positioned as if that text started right after
/// `#{`; its first table is the relation.
pub(super) fn expected_relation(island: &TypedIsland) -> R<RelationElement> {
    let text = format!(": {}", relation_data::java_trim(&island.body.text));
    let data = relation_data::parse(&text, origin(&island.body), &island.body.source_info.source)?;
    data.elements.into_iter().next().ok_or_else(|| {
        ParseError::unexpected(
            "Expected at least one relation element in Relation assertion",
            island.source_info.clone(),
        )
    })
}

impl ParserContext<'_> {
    /// Parses a test assertion read with
    /// [`read_typed_island`](Self::read_typed_island): the engine core's
    /// `EqualTo #{ expected: value; }#` and
    /// `EqualToJson #{ expected: ExternalFormat #{ … }#; }#`, and
    /// `Relation #{ … }#`, which mapping and service tests read as an
    /// `EqualToRelation`.
    ///
    /// # Errors
    ///
    /// A syntax error, or [`ParseError::Unsupported`] for another type of
    /// assertion, which an engine extension may define.
    pub fn parse_test_assertion(&mut self, island: &TypedIsland) -> R<TestAssertion> {
        self.parser.parse_test_assertion(island)
    }
}

#[cfg(test)]
mod tests {
    use legend_pure_parser_ast::data::EmbeddedData;
    use legend_pure_parser_ast::expression::{Expression, Literal};

    use super::super::test_support::{position, with_context};
    use super::*;

    fn assertion(source: &str) -> R<TestAssertion> {
        with_context(source, |ctx| {
            let island = ctx.read_typed_island()?;
            ctx.parse_test_assertion(&island)
        })
    }

    #[test]
    fn equal_to_json_expects_external_format_data() {
        let parsed = assertion(
            "EqualToJson #{\n  expected: ExternalFormat #{ contentType: 'application/json'; data: '[]'; }#;\n}#",
        )
        .expect("parses");
        let TestAssertion::EqualToJson {
            expected,
            source_info,
        } = parsed
        else {
            panic!("{parsed:?}");
        };
        assert_eq!(position(&source_info), (1, 1, 3, 2));
        // From its type to its `}#`.
        assert_eq!(position(&expected.source_info), (2, 13, 2, 77));
        let EmbeddedData::ExternalFormat(data) = expected.data else {
            panic!("external format data");
        };
        assert_eq!(data.content_type, "application/json");
        let error = assertion("EqualToJson #{ expected: Reference #{ my::Data }#; }#")
            .expect_err("not external format data");
        assert!(
            error.message().contains("expects ExternalFormat data"),
            "{error}"
        );
    }

    #[test]
    fn equal_to_expects_a_value() {
        let parsed = assertion("EqualTo #{ expected: 'a'; }#").expect("parses");
        let TestAssertion::EqualTo { expected, .. } = parsed else {
            panic!("{parsed:?}");
        };
        assert!(matches!(expected, Expression::Literal(Literal::String(_))));
        let error = assertion("EqualTo #{ value: 'a'; }#").expect_err("no expected");
        assert_eq!(
            position(error.source_info().expect("positioned")),
            (1, 12, 1, 16)
        );
        let error = assertion("EqualTo #{}#").expect_err("empty");
        assert_eq!(error.message(), "Test Assertion must not be empty");
        assert_eq!(
            position(error.source_info().expect("positioned")),
            (1, 1, 1, 12)
        );
    }

    #[test]
    fn a_relation_is_its_first_table() {
        let parsed = assertion("Relation #{\n  id, name\n  1, a;\n}#").expect("parses");
        let TestAssertion::EqualToRelation { expected, .. } = parsed else {
            panic!("{parsed:?}");
        };
        assert!(expected.paths.is_empty());
        assert_eq!(expected.columns, ["id", "name"]);
        assert_eq!(expected.rows, [["1", "a"]]);
    }

    #[test]
    fn another_type_is_unsupported() {
        let error = assertion("EqualToTDS #{ expected: x; }#").expect_err("unsupported");
        assert!(matches!(error, ParseError::Unsupported { .. }), "{error}");
        assert_eq!(
            position(error.source_info().expect("positioned")),
            (1, 1, 1, 10)
        );
    }
}
