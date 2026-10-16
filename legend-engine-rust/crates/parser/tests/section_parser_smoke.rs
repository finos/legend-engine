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

//! End-to-end test for [`SectionParser`] dispatch.
//!
//! Defines a `MockDSL` element + section parser entirely outside of
//! core (the test crate plays the role of a DSL crate) and asserts:
//!
//! 1. A `###MockDSL` section is routed to the registered parser.
//! 2. The parser's output flows through `Element::DSLElement`.
//! 3. Pure sections in the same file continue to parse as M3
//!    elements unaffected.
//!
//! Locks the contract that core has no DSL-specific knowledge.

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, Element, PackageableElement};
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::section_parser::SectionParser;

// ---------------------------------------------------------------------------
// Mock DSL element — would live in a DSL crate in production.
// ---------------------------------------------------------------------------

#[derive(Debug, Clone, PartialEq)]
struct MockDSL {
    name: SpannedString,
    source_info: SourceInfo,
}

impl Spanned for MockDSL {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for MockDSL {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }
    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl PackageableElement for MockDSL {
    fn package(&self) -> Option<&Package> {
        None
    }
    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for MockDSL {
    fn kind(&self) -> &'static str {
        "MockDSL"
    }
    fn as_any(&self) -> &dyn std::any::Any {
        self
    }
    fn clone_box(&self) -> Box<dyn DSLElement> {
        Box::new(self.clone())
    }
    fn eq_content(&self, other: &dyn DSLElement) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|o| self == o)
    }
}

// ---------------------------------------------------------------------------
// Mock section parser — consumes identifiers between section boundaries
// and emits a MockDSL per identifier.
// ---------------------------------------------------------------------------

struct MockSectionParser;

impl SectionParser for MockSectionParser {
    fn kind(&self) -> &'static str {
        "MockDSL"
    }

    fn parse_body(
        &self,
        ctx: &mut legend_pure_parser_parser::ParserContext<'_>,
        _errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        let cursor = ctx.cursor();
        let mut out: Vec<Box<dyn DSLElement>> = Vec::new();
        while !cursor.check(TokenKind::SectionHeader) && !cursor.check(TokenKind::Eof) {
            if cursor.check(TokenKind::Identifier) {
                let tok = cursor.advance().clone();
                let span = tok.source_info.clone();
                out.push(Box::new(MockDSL {
                    name: SpannedString {
                        value: smol_str::SmolStr::new(tok.text.clone()),
                        source_info: span.clone(),
                    },
                    source_info: span,
                }));
            } else {
                cursor.advance();
            }
        }
        out
    }
}

/// A section grammar without imports, like the engine's `###Data`: every
/// token of its body must be an identifier.
struct NoImportsSectionParser;

impl SectionParser for NoImportsSectionParser {
    fn kind(&self) -> &'static str {
        "NoImports"
    }

    fn imports(&self) -> bool {
        false
    }

    fn parse_body(
        &self,
        ctx: &mut legend_pure_parser_parser::ParserContext<'_>,
        errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        let cursor = ctx.cursor();
        while !cursor.check(TokenKind::SectionHeader) && !cursor.check(TokenKind::Eof) {
            let token = cursor.advance().clone();
            if token.kind != TokenKind::Identifier {
                errors.push(ParseError::Unexpected {
                    message: "expected an identifier".to_string(),
                    source_info: token.source_info,
                });
            }
        }
        Vec::new()
    }
}

#[test]
fn only_a_section_grammar_with_imports_has_them_read_first() {
    let parse = |source: &str| {
        legend_pure_parser_parser::parse_with_sections(
            source,
            "imports.pure",
            legend_pure_parser_parser::island::default_island_parsers(),
            vec![
                Box::new(MockSectionParser),
                Box::new(NoImportsSectionParser),
            ],
        )
    };
    let file = parse("###MockDSL\nimport a::*;\nfoo\n").expect("imports, then the body");
    assert_eq!(file.sections[0].imports.len(), 1);
    assert_eq!(file.sections[0].elements.len(), 1);
    // `import` reaches the grammar, which rejects it.
    let errors = parse("###NoImports\nimport a::*;\n")
        .expect_err("not an identifier")
        .errors;
    let si = errors[0].source_info().expect("positioned");
    assert_eq!((si.start_line, si.start_column), (2, 1));
}

#[test]
fn section_parser_dispatches_to_dsl_plugin() {
    let source = "\
###MockDSL
foo
bar
###Pure
Class smoke::Person { name: String[1]; }
";

    let result = legend_pure_parser_parser::parse_with_sections(
        source,
        "section_smoke.pure",
        legend_pure_parser_parser::island::default_island_parsers(),
        vec![Box::new(MockSectionParser)],
    );

    let file = match result {
        Ok(f) => f,
        Err(p) => panic!("parse failed with errors: {:?}", p.errors),
    };

    assert_eq!(file.sections.len(), 2, "expected exactly two sections");
    let mock_section = file
        .sections
        .iter()
        .find(|s| s.kind.as_str() == "MockDSL")
        .expect("MockDSL section not found");
    assert_eq!(
        mock_section.elements.len(),
        2,
        "MockDSL parser should have produced two MockDSL elements; got {}",
        mock_section.elements.len()
    );
    for elem in &mock_section.elements {
        match elem {
            Element::DSLElement(dsl) => {
                assert_eq!(dsl.kind(), "MockDSL");
                let mock = dsl
                    .as_any()
                    .downcast_ref::<MockDSL>()
                    .expect("downcast to MockDSL");
                assert!(mock.name.value == "foo" || mock.name.value == "bar");
            }
            other => panic!("expected Element::DSLElement, got {other:?}"),
        }
    }

    // The Pure section must still parse normally — proves the dispatch
    // doesn't leak across sections.
    let pure_section = file
        .sections
        .iter()
        .find(|s| s.kind.as_str() == "Pure")
        .expect("Pure section not found");
    assert_eq!(
        pure_section.elements.len(),
        1,
        "Pure section should have 1 Class"
    );
    assert!(matches!(&pure_section.elements[0], Element::Class(_)));
}

#[test]
fn unregistered_section_kind_falls_through_to_m3_grammar() {
    // No section parser registered for "Unknown" — the body uses
    // standard M3 syntax so it should parse via the default path.
    let source = "\
###Unknown
Class u::Foo { x: Integer[1]; }
";
    let result = legend_pure_parser_parser::parse_with_sections(
        source,
        "fallthrough.pure",
        legend_pure_parser_parser::island::default_island_parsers(),
        vec![],
    );
    let file = result.expect("parse should succeed");
    let unknown = &file.sections[0];
    assert_eq!(unknown.kind.as_str(), "Unknown");
    assert_eq!(unknown.elements.len(), 1);
    assert!(matches!(&unknown.elements[0], Element::Class(_)));
}
