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

//! Plug-in conversion contract.
//!
//! This test binary plays the role of a grammar crate: it registers one
//! island converter and one element converter through the linkme slices,
//! and deliberately leaves a second island tag and a second element kind
//! unregistered. It locks that:
//!
//! 1. Registered islands and DSL elements convert through their plug-in,
//!    with DSL elements carried verbatim as `PackageableElement::Extension`.
//! 2. Anything without a registered converter — and anything Protocol v1
//!    cannot represent — is `ConvertError::Unsupported` at the construct's
//!    position, never a silent placeholder.

use std::any::Any;

use legend_pure_parser_ast::annotation::{SpannedString, StereotypePtr, TaggedValue};
use legend_pure_parser_ast::dsl::DSLElement;
use legend_pure_parser_ast::element::{Annotated, PackageableElement};
use legend_pure_parser_ast::island::IslandContent;
use legend_pure_parser_ast::source_info::{SourceInfo, Spanned};
use legend_pure_parser_ast::type_ref::{Identifier, Package};
use legend_pure_parser_lexer::TokenKind;
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::section_parser::SectionParser;
use legend_pure_parser_parser::{IslandParser, ParserContext};
use legend_pure_parser_protocol::element_protocol::{ELEMENT_PROTOCOLS, ElementProtocol};
use legend_pure_parser_protocol::island_protocol::{ISLAND_PROTOCOLS, IslandProtocol};
use legend_pure_parser_protocol::v1;
use legend_pure_parser_protocol::v1::convert::{ConvertError, Result, convert_source_file};
use legend_pure_parser_protocol::v1::value_spec::{ClassInstance, ValueSpecification};
use linkme::distributed_slice;

// ---------------------------------------------------------------------------
// Island `#echo{ word }#`: parser + registered protocol converter.
// Island `#bare{ word }#`: parser only, no protocol converter.
// ---------------------------------------------------------------------------

#[derive(Debug, Clone, PartialEq)]
struct Echo {
    tag: &'static str,
    word: String,
    source_info: SourceInfo,
}

impl IslandContent for Echo {
    fn tag(&self) -> &str {
        self.tag
    }
    fn as_any(&self) -> &dyn Any {
        self
    }
    fn clone_box(&self) -> Box<dyn IslandContent> {
        Box::new(self.clone())
    }
    fn eq_content(&self, other: &dyn IslandContent) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|o| self == o)
    }
}

struct EchoParser(&'static str);

impl IslandParser for EchoParser {
    fn tag(&self) -> &str {
        self.0
    }
    fn parse(
        &self,
        ctx: &mut ParserContext<'_>,
    ) -> std::result::Result<Box<dyn IslandContent>, ParseError> {
        let source_info = ctx.island_span().cloned().expect("an island");
        let word = ctx.cursor().expect(TokenKind::Identifier)?.text.to_string();
        Ok(Box::new(Echo {
            tag: self.0,
            word,
            source_info,
        }))
    }
}

struct EchoProtocol;

impl IslandProtocol for EchoProtocol {
    fn tag(&self) -> &'static str {
        "echo"
    }
    fn convert(&self, content: &dyn IslandContent) -> Result<ValueSpecification> {
        let echo = content
            .as_any()
            .downcast_ref::<Echo>()
            .ok_or_else(|| ConvertError::plugin("not an Echo", None))?;
        Ok(ValueSpecification::ClassInstance(ClassInstance {
            type_name: "echo".to_string(),
            value: serde_json::json!({ "word": echo.word }),
            source_information: None,
        }))
    }
}

#[distributed_slice(ISLAND_PROTOCOLS)]
static ECHO_PROTOCOL: &(dyn IslandProtocol + Send + Sync) = &EchoProtocol;

// ---------------------------------------------------------------------------
// Section `###Widget`: one element per identifier, with a registered
// converter. Section `###Gadget`: same shape, no converter.
// ---------------------------------------------------------------------------

#[derive(Debug, Clone, PartialEq)]
struct Widget {
    kind: &'static str,
    name: SpannedString,
    source_info: SourceInfo,
}

impl Spanned for Widget {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

impl Annotated for Widget {
    fn stereotypes(&self) -> &[StereotypePtr] {
        &[]
    }
    fn tagged_values(&self) -> &[TaggedValue] {
        &[]
    }
}

impl PackageableElement for Widget {
    fn package(&self) -> Option<&Package> {
        None
    }
    fn name(&self) -> &Identifier {
        &self.name.value
    }
}

impl DSLElement for Widget {
    fn kind(&self) -> &str {
        self.kind
    }
    fn as_any(&self) -> &dyn Any {
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

struct WidgetSectionParser(&'static str);

impl SectionParser for WidgetSectionParser {
    fn kind(&self) -> &str {
        self.0
    }
    fn parse_body(
        &self,
        ctx: &mut ParserContext<'_>,
        _errors: &mut Vec<ParseError>,
    ) -> Vec<Box<dyn DSLElement>> {
        let cursor = ctx.cursor();
        let mut out: Vec<Box<dyn DSLElement>> = Vec::new();
        while !cursor.check(TokenKind::SectionHeader) && !cursor.check(TokenKind::Eof) {
            let tok = cursor.advance().clone();
            if tok.kind == TokenKind::Identifier {
                out.push(Box::new(Widget {
                    kind: self.0,
                    name: SpannedString {
                        value: smol_str::SmolStr::new(tok.text.clone()),
                        source_info: tok.source_info.clone(),
                    },
                    source_info: tok.source_info.clone(),
                }));
            }
        }
        out
    }
}

struct WidgetProtocol;

impl ElementProtocol for WidgetProtocol {
    fn kind(&self) -> &'static str {
        "Widget"
    }
    fn convert(&self, element: &dyn DSLElement) -> Result<serde_json::Value> {
        Ok(serde_json::json!({
            "_type": "widget",
            "name": element.name().to_string(),
        }))
    }
}

#[distributed_slice(ELEMENT_PROTOCOLS)]
static WIDGET_PROTOCOL: &(dyn ElementProtocol + Send + Sync) = &WidgetProtocol;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

fn parse(source: &str) -> legend_pure_parser_ast::section::SourceFile {
    legend_pure_parser_parser::parse_with_sections(
        source,
        "plugins.pure",
        vec![Box::new(EchoParser("echo")), Box::new(EchoParser("bare"))],
        vec![
            Box::new(WidgetSectionParser("Widget")),
            Box::new(WidgetSectionParser("Gadget")),
        ],
    )
    .expect("test source parses")
}

fn unsupported(source: &str) -> (String, SourceInfo) {
    match convert_source_file(&parse(source)) {
        Err(ConvertError::Unsupported {
            message,
            source_info,
        }) => (message, source_info),
        other => panic!("expected Unsupported, got {other:?}"),
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[test]
fn registered_island_converts_through_its_protocol() {
    let pmcd =
        convert_source_file(&parse("function f():Any[1] { #echo{ hello }# }")).expect("converts");
    let json = serde_json::to_value(&pmcd.elements[0]).expect("serializes");
    assert_eq!(json["body"][0]["_type"], "classInstance");
    assert_eq!(json["body"][0]["type"], "echo");
    assert_eq!(json["body"][0]["value"]["word"], "hello");
}

#[test]
fn island_without_protocol_is_unsupported_at_the_island() {
    let (message, si) = unsupported("function f():Any[1]\n{\n  #bare{ hello }#\n}");
    assert!(message.contains("island tag 'bare'"), "{message}");
    assert_eq!(si.start_line, 3);
}

#[test]
fn registered_dsl_element_is_carried_verbatim_as_extension() {
    let pmcd = convert_source_file(&parse("###Widget\nfirst second\n")).expect("converts");
    let widgets: Vec<_> = pmcd
        .elements
        .iter()
        .filter_map(|e| match e {
            v1::element::PackageableElement::Extension(v) => Some(v.clone()),
            _ => None,
        })
        .collect();
    assert_eq!(
        widgets,
        vec![
            serde_json::json!({"_type": "widget", "name": "first"}),
            serde_json::json!({"_type": "widget", "name": "second"}),
        ]
    );
    // Untagged: serializes as the plug-in's object, with no wrapper `_type`.
    let json = serde_json::to_value(&pmcd.elements[0]).expect("serializes");
    assert_eq!(
        json,
        serde_json::json!({"_type": "widget", "name": "first"})
    );
}

#[test]
fn dsl_element_without_protocol_is_unsupported_at_the_element() {
    let (message, si) = unsupported("###Gadget\n\n  thing\n");
    assert!(message.contains("element kind 'Gadget'"), "{message}");
    assert_eq!((si.start_line, si.start_column), (3, 3));
}

#[test]
fn native_function_is_unsupported() {
    let (message, si) =
        unsupported("Class a::A {}\nnative function a::nf(x: String[1]): String[1];\n");
    assert!(message.contains("native function"), "{message}");
    assert_eq!(si.start_line, 2);
}

#[test]
fn slice_is_unsupported() {
    let (message, _) = unsupported("function f():Any[*] { [1:3] }");
    assert!(message.contains("slice"), "{message}");
}

#[test]
fn unknown_element_type_deserializes_as_extension_and_round_trips() {
    let json = serde_json::json!({"_type": "mapping", "name": "M", "package": "a"});
    let element: v1::element::PackageableElement =
        serde_json::from_value(json.clone()).expect("deserializes");
    assert!(matches!(
        element,
        v1::element::PackageableElement::Extension(_)
    ));
    assert_eq!(serde_json::to_value(&element).expect("serializes"), json);
}

#[test]
fn pure_section_is_import_aware_even_without_imports() {
    let pmcd = convert_source_file(&parse("Class a::A {}\n")).expect("converts");
    let index =
        serde_json::to_value(pmcd.elements.last().expect("section index")).expect("serializes");
    assert_eq!(index["sections"][0]["_type"], "importAware");
    assert_eq!(index["sections"][0]["parserName"], "Pure");
}
