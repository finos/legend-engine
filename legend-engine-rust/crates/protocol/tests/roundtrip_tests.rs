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

//! Tests for protocol conversion roundtrips (AST -> Protocol -> AST)
//! and direct protocol definition coverages.

use legend_pure_parser_ast as ast;
use legend_pure_parser_protocol::v1;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

fn var(name: &str) -> v1::value_spec::Variable {
    v1::value_spec::Variable {
        name: name.to_string(),
        generic_type: None,
        multiplicity: None,
        supports_stream: None,
        source_information: None,
    }
}

// ---------------------------------------------------------------------------
// Literal and simple ValueSpecification tests (from_protocol coverage)
// ---------------------------------------------------------------------------

#[test]
fn roundtrip_float_literal() {
    let vs = v1::value_spec::ValueSpecification::Float(v1::value_spec::CFloat {
        value: std::f64::consts::E,
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    match ast_expr {
        ast::expression::Expression::Literal(ast::expression::Literal::Float(f)) => {
            assert!((f.value - std::f64::consts::E).abs() < f64::EPSILON);
        }
        _ => panic!("Expected Float expression"),
    }
}

#[test]
fn roundtrip_decimal_literal() {
    let vs = v1::value_spec::ValueSpecification::Decimal(v1::value_spec::CDecimal {
        value: "42.42".parse().unwrap(),
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    match ast_expr {
        ast::expression::Expression::Literal(ast::expression::Literal::Decimal(d)) => {
            assert_eq!(d.value, "42.42");
        }
        _ => panic!("Expected Decimal expression"),
    }
}

#[test]
fn roundtrip_string_literal() {
    let vs = v1::value_spec::ValueSpecification::String(v1::value_spec::CString {
        multi_line: false,
        value: "hello".to_string(),
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    match ast_expr {
        ast::expression::Expression::Literal(ast::expression::Literal::String(s)) => {
            assert_eq!(s.value.as_str(), "hello");
        }
        _ => panic!("Expected String expression"),
    }
}

#[test]
fn roundtrip_boolean_literal() {
    let vs = v1::value_spec::ValueSpecification::Boolean(v1::value_spec::CBoolean {
        value: true,
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    match ast_expr {
        ast::expression::Expression::Literal(ast::expression::Literal::Boolean(b)) => {
            assert!(b.value);
        }
        _ => panic!("Expected Boolean expression"),
    }
}

#[test]
fn roundtrip_datetime_literal() {
    let vs = v1::value_spec::ValueSpecification::DateTime(v1::value_spec::CDateTime {
        value: "2024-01-01T00:00:00Z".to_string(),
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    match ast_expr {
        ast::expression::Expression::Literal(ast::expression::Literal::DateTime(d)) => {
            assert_eq!(d.value.as_str(), "2024-01-01T00:00:00Z");
        }
        _ => panic!("Expected DateTime expression"),
    }
}

#[test]
fn roundtrip_strict_date_literal() {
    let vs = v1::value_spec::ValueSpecification::StrictDate(v1::value_spec::CStrictDate {
        value: "2024-01-01".to_string(),
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    match ast_expr {
        ast::expression::Expression::Literal(ast::expression::Literal::StrictDate(d)) => {
            // Internally represented by the same Date node, but has length properties if strict
            assert_eq!(d.value.as_str(), "2024-01-01");
        }
        _ => panic!("Expected StrictDate expression"),
    }
}

#[test]
fn roundtrip_latest_date_literal() {
    let vs = v1::value_spec::ValueSpecification::LatestDate(v1::value_spec::CLatestDate {
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    assert!(
        matches!(
            ast_expr,
            ast::expression::Expression::Literal(ast::expression::Literal::LatestDate(_))
        ),
        "{ast_expr:?}"
    );
}

#[test]
fn roundtrip_collection() {
    let vs = v1::value_spec::ValueSpecification::Collection(v1::value_spec::ProtocolCollection {
        multiplicity: v1::multiplicity::Multiplicity {
            lower_bound: 1,
            upper_bound: Some(1),
        },
        values: vec![v1::value_spec::ValueSpecification::Integer(
            v1::value_spec::CInteger {
                value: 1,
                source_information: None,
            },
        )],
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    match ast_expr {
        ast::expression::Expression::Collection(c) => {
            assert_eq!(c.elements.len(), 1);
        }
        _ => panic!("Expected Collection expression"),
    }
}

// helpers to create typed variables
fn typed_var(
    name: &str,
    type_str: &str,
    lower: u32,
    upper: Option<u32>,
) -> v1::value_spec::Variable {
    v1::value_spec::Variable {
        name: name.to_string(),
        generic_type: Some(v1::generic_type::GenericType {
            raw_type: v1::generic_type::RawType::Packageable(v1::generic_type::PackageableType {
                full_path: type_str.to_string(),
                source_information: None,
            }),
            type_arguments: vec![],
            multiplicity_arguments: vec![],
            type_variable_values: vec![],
            source_information: None,
        }),
        multiplicity: Some(v1::multiplicity::Multiplicity {
            lower_bound: lower,
            upper_bound: upper,
        }),
        supports_stream: None,
        source_information: None,
    }
}

#[test]
fn roundtrip_lambda() {
    let vs = v1::value_spec::ValueSpecification::Lambda(v1::value_spec::LambdaFunction {
        parameters: vec![typed_var("x", "String", 1, Some(1))],
        body: vec![v1::value_spec::ValueSpecification::Var(var("x"))],
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    match ast_expr {
        ast::expression::Expression::Lambda(l) => {
            assert_eq!(l.parameters.len(), 1);
            assert_eq!(l.body.len(), 1);
        }
        _ => panic!("Expected Lambda expression"),
    }
}

#[test]
fn unsupported_value_spec() {
    // EnumValue is not meant to be read directly as an expression since it should be an EnumValue
    // protocol mapping. Let's see if we get the UnsupportedValueSpec error.
    let vs = v1::value_spec::ValueSpecification::EnumValue(v1::value_spec::ProtocolEnumValue {
        full_path: "MyEnum".to_string(),
        value: "SomeValue".to_string(),
        source_information: None,
    });
    let err = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap_err();
    assert!(matches!(
        err,
        v1::from_protocol::ConversionError::UnsupportedValueSpec
    ));
}

#[test]
fn roundtrip_applied_function_equal() {
    let vs = v1::value_spec::ValueSpecification::Func(v1::value_spec::AppliedFunction {
        function: "equal".to_string(),
        parameters: vec![
            v1::value_spec::ValueSpecification::Var(var("x")),
            v1::value_spec::ValueSpecification::Var(var("y")),
        ],
        f_control: None,
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    match ast_expr {
        ast::expression::Expression::Comparison(op) => {
            assert_eq!(op.op, ast::expression::ComparisonOp::Equal);
        }
        _ => panic!("Expected Comparison expression"),
    }
}

#[test]
fn roundtrip_applied_function_not() {
    let vs = v1::value_spec::ValueSpecification::Func(v1::value_spec::AppliedFunction {
        function: "not".to_string(),
        parameters: vec![v1::value_spec::ValueSpecification::Var(var("x"))],
        f_control: None,
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    match ast_expr {
        ast::expression::Expression::Not(op) => {
            assert!(matches!(
                *op.operand,
                ast::expression::Expression::Variable(_)
            ));
        }
        _ => panic!("Expected Not expression"),
    }
}

#[test]
fn roundtrip_applied_property_qualified() {
    let vs = v1::value_spec::ValueSpecification::Property(v1::value_spec::AppliedProperty {
        property: "fullName".to_string(),
        parameters: vec![
            v1::value_spec::ValueSpecification::Var(var("this")),
            v1::value_spec::ValueSpecification::String(v1::value_spec::CString {
                multi_line: false,
                value: " ".to_string(),
                source_information: None,
            }),
        ],
        class: None,
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    match ast_expr {
        ast::expression::Expression::MemberAccess(ast::expression::MemberAccess::Qualified(q)) => {
            assert_eq!(q.member.as_str(), "fullName");
            assert_eq!(q.arguments.len(), 1);
        }
        _ => panic!("Expected QualifiedMemberAccess"),
    }
}

#[test]
fn roundtrip_applied_property_simple() {
    let vs = v1::value_spec::ValueSpecification::Property(v1::value_spec::AppliedProperty {
        property: "name".to_string(),
        parameters: vec![v1::value_spec::ValueSpecification::Var(var("this"))],
        class: None,
        source_information: None,
    });
    let ast_expr = v1::from_protocol::convert_value_spec_to_expression(&vs).unwrap();
    match ast_expr {
        ast::expression::Expression::MemberAccess(ast::expression::MemberAccess::Simple(s)) => {
            assert_eq!(s.member.as_str(), "name");
        }
        _ => panic!("Expected SimpleMemberAccess"),
    }
}

#[test]
fn parse_empty_path() {
    let err = v1::from_protocol::convert_value_spec_to_expression(
        &v1::value_spec::ValueSpecification::PackageableElementPtr(
            v1::value_spec::ProtocolPackageableElementPtr {
                full_path: String::new(),
                source_information: None,
            },
        ),
    )
    .unwrap_err();
    assert!(matches!(err, v1::from_protocol::ConversionError::EmptyPath));
}

// ---------------------------------------------------------------------------
// Struct -> Element conversions
// ---------------------------------------------------------------------------

#[test]
fn enumeration_element_conversion() {
    let enumeration =
        v1::element::PackageableElement::Enumeration(v1::element::ProtocolEnumeration {
            package_path: "model".to_string(),
            name: "MyEnum".to_string(),
            values: vec![v1::element::ProtocolEnumMember {
                value: "VAL1".to_string(),
                stereotypes: vec![],
                tagged_values: vec![],
                source_information: None,
            }],
            stereotypes: vec![],
            tagged_values: vec![],
            source_information: None,
        });

    let element = v1::from_protocol::convert_element(&enumeration).unwrap();
    match element {
        Some(ast::element::Element::Enumeration(e)) => {
            assert_eq!(e.name.value.as_str(), "MyEnum");
            assert_eq!(e.package.unwrap().to_string().as_str(), "model");
            assert_eq!(e.values.len(), 1);
        }
        _ => panic!("Expected Enumeration"),
    }
}

#[test]
fn association_element_conversion() {
    let association =
        v1::element::PackageableElement::Association(v1::element::ProtocolAssociation {
            package_path: "model".to_string(),
            name: "MyAssoc".to_string(),
            properties: vec![v1::property::Property {
                name: "prop1".to_string(),
                generic_type: v1::generic_type::GenericType {
                    raw_type: v1::generic_type::RawType::Packageable(
                        v1::generic_type::PackageableType {
                            full_path: "model::Class1".to_string(),
                            source_information: None,
                        },
                    ),
                    type_arguments: vec![],
                    multiplicity_arguments: vec![],
                    type_variable_values: vec![],
                    source_information: None,
                },
                multiplicity: v1::multiplicity::Multiplicity {
                    lower_bound: 1,
                    upper_bound: Some(1),
                },
                stereotypes: vec![],
                tagged_values: vec![],
                default_value: None,
                aggregation: None,
                source_information: None,
            }],
            qualified_properties: vec![],
            original_milestoned_properties: vec![],
            stereotypes: vec![],
            tagged_values: vec![],
            source_information: None,
        });

    let element = v1::from_protocol::convert_element(&association).unwrap();
    match element {
        Some(ast::element::Element::Association(a)) => {
            assert_eq!(a.name.value.as_str(), "MyAssoc");
            assert_eq!(a.properties.len(), 1);
        }
        _ => panic!("Expected Association"),
    }
}

#[test]
fn measure_element_conversion() {
    let measure = v1::element::PackageableElement::Measure(v1::element::ProtocolMeasure {
        package_path: "model".to_string(),
        name: "MyMeasure".to_string(),
        canonical_unit: Some(v1::element::ProtocolUnit {
            name: "MyUnit".to_string(),
            measure: "model::MyMeasure".to_string(),
            conversion_function: None,
            source_information: None,
        }),
        non_canonical_units: Some(vec![]),
        source_information: None,
    });
    let element = v1::from_protocol::convert_element(&measure).unwrap();
    match element {
        Some(ast::element::Element::Measure(m)) => {
            assert_eq!(m.name.value.as_str(), "MyMeasure");
        }
        _ => panic!("Expected Measure"),
    }
}

#[test]
fn function_element_conversion() {
    let func = v1::element::PackageableElement::Function(v1::element::ProtocolFunction {
        package_path: "model".to_string(),
        name: "myFunc".to_string(),
        parameters: vec![],
        return_generic_type: v1::generic_type::GenericType {
            raw_type: v1::generic_type::RawType::Packageable(v1::generic_type::PackageableType {
                full_path: "String".to_string(),
                source_information: None,
            }),
            type_arguments: vec![],
            multiplicity_arguments: vec![],
            type_variable_values: vec![],
            source_information: None,
        },
        return_multiplicity: v1::multiplicity::Multiplicity {
            lower_bound: 1,
            upper_bound: Some(1),
        },
        body: vec![v1::value_spec::ValueSpecification::String(
            v1::value_spec::CString {
                multi_line: false,
                value: "hi".to_string(),
                source_information: None,
            },
        )],
        stereotypes: vec![],
        tagged_values: vec![],
        tests: vec![],
        pre_constraints: vec![],
        post_constraints: vec![],
        source_information: None,
    });
    let element = v1::from_protocol::convert_element(&func).unwrap();
    match element {
        Some(ast::element::Element::Function(f)) => {
            assert_eq!(f.name.value.as_str(), "myFunc");
            assert_eq!(f.body.len(), 1);
        }
        _ => panic!("Expected Function"),
    }
}

// ---------------------------------------------------------------------------
// Navigation path roundtrip (Stage 2)
// ---------------------------------------------------------------------------

fn synthetic_si() -> ast::SourceInfo {
    ast::SourceInfo::new("test.pure", 0, 0, 0, 0)
}

fn type_ref(pkg_segs: &[&str], name: &str) -> ast::type_ref::TypeReference {
    let package = pkg_segs.iter().fold(None, |parent, seg| {
        Some(Box::new(ast::type_ref::Package::new(
            smol_str::SmolStr::new(*seg),
            parent,
            synthetic_si(),
        )))
    });
    ast::type_ref::TypeReference {
        package: package.map(|b| *b),
        name: smol_str::SmolStr::new(name),
        type_arguments: vec![],
        multiplicity_arguments: vec![],
        type_variable_values: vec![],
        source_info: synthetic_si(),
    }
}

fn assert_path_roundtrips(original: ast::expression::NavigationPath) {
    use ast::expression::Expression;
    let ast_expr = Expression::NavigationPath(original.clone());
    let vs = v1::convert::convert_expression_typed(&ast_expr).expect("converts");
    let recovered = v1::from_protocol::convert_value_spec_to_expression(&vs)
        .expect("ClassInstance(\"path\", ...) should roundtrip back to NavigationPath");

    match recovered {
        Expression::NavigationPath(rec) => {
            assert_eq!(
                rec.start_type.full_path(),
                original.start_type.full_path(),
                "start_type FQN mismatch"
            );
            assert_eq!(rec.path.len(), original.path.len(), "step count mismatch");
            for (got, want) in rec.path.iter().zip(original.path.iter()) {
                assert_eq!(got.property, want.property, "property mismatch");
                assert_eq!(
                    got.parameters.len(),
                    want.parameters.len(),
                    "parameter count mismatch on step '{}'",
                    want.property
                );
            }
            assert_eq!(rec.name, original.name, "alias mismatch");
        }
        other => panic!("Expected NavigationPath after roundtrip, got: {other:?}"),
    }
}

#[test]
fn roundtrip_navigation_path_simple() {
    use ast::expression::{NavigationPath, PropertyPathElement};
    assert_path_roundtrips(NavigationPath {
        start_type: type_ref(&[], "Person"),
        path: vec![PropertyPathElement {
            property: smol_str::SmolStr::new("name"),
            parameters: vec![],
            source_info: synthetic_si(),
        }],
        name: None,
        source_info: synthetic_si(),
    });
}

#[test]
fn roundtrip_navigation_path_qualified_multi_step() {
    use ast::expression::{NavigationPath, PropertyPathElement};
    assert_path_roundtrips(NavigationPath {
        start_type: type_ref(&["model"], "Firm"),
        path: vec![
            PropertyPathElement {
                property: smol_str::SmolStr::new("employees"),
                parameters: vec![],
                source_info: synthetic_si(),
            },
            PropertyPathElement {
                property: smol_str::SmolStr::new("address"),
                parameters: vec![],
                source_info: synthetic_si(),
            },
        ],
        name: None,
        source_info: synthetic_si(),
    });
}

#[test]
fn roundtrip_navigation_path_with_alias_and_string_param() {
    use ast::expression::{
        Expression, Literal, NavigationPath, PropertyPathElement, StringLiteral,
    };
    assert_path_roundtrips(NavigationPath {
        start_type: type_ref(&[], "Person"),
        path: vec![PropertyPathElement {
            property: smol_str::SmolStr::new("nameWithTitle"),
            parameters: vec![Expression::Literal(Literal::String(StringLiteral {
                value: smol_str::SmolStr::new("Mr"),
                multi_line: false,
                source_info: synthetic_si(),
            }))],
            source_info: synthetic_si(),
        }],
        name: Some(smol_str::SmolStr::new("formalName")),
        source_info: synthetic_si(),
    });
}

#[test]
fn navigation_path_serializes_with_path_discriminator() {
    use ast::expression::{Expression, NavigationPath, PropertyPathElement};
    let ast_expr = Expression::NavigationPath(NavigationPath {
        start_type: type_ref(&[], "Person"),
        path: vec![PropertyPathElement {
            property: smol_str::SmolStr::new("name"),
            parameters: vec![],
            source_info: synthetic_si(),
        }],
        name: None,
        source_info: synthetic_si(),
    });
    let vs = v1::convert::convert_expression_typed(&ast_expr).expect("converts");
    let json = serde_json::to_value(&vs).expect("serializes");
    // Locks the wire shape: `{ _type: "classInstance", type: "path",
    // value: { startType, path: [...], name? } }`.
    assert_eq!(json["_type"], "classInstance");
    assert_eq!(json["type"], "path");
    assert_eq!(json["value"]["startType"], "Person");
    assert_eq!(json["value"]["path"][0]["_type"], "propertyPath");
    assert_eq!(json["value"]["path"][0]["property"], "name");
}
