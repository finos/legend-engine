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

//! Protocol v1 snapshot tests — validates JSON output shape against golden files.
//!
//! These tests construct protocol types directly and verify that their
//! serialized JSON matches expected snapshots managed by `insta`.
//!
//! The snapshots serve two purposes:
//! 1. **Regression guard**: Any unintentional change to JSON shape is caught.
//! 2. **Java compatibility baseline**: Snapshots can be compared with Java
//!    `PureGrammarParser` output for byte-level compatibility validation.

use legend_pure_parser_protocol::v1;

/// `value` as pretty JSON text, which `serde_json` renders with numbers as
/// written (the workspace enables its `arbitrary_precision`, which insta's
/// own JSON serializer doesn't know).
fn json_text<T: serde::Serialize>(value: &T) -> String {
    serde_json::to_string_pretty(value).expect("serializes")
}

// ---------------------------------------------------------------------------
// Helper: build common protocol types
// ---------------------------------------------------------------------------

fn si(source: &str, sl: u32, sc: u32, el: u32, ec: u32) -> v1::source_info::SourceInformation {
    v1::source_info::SourceInformation {
        source_id: source.to_string(),
        start_line: sl,
        start_column: sc,
        end_line: el,
        end_column: ec,
    }
}

fn mult(lo: u32, hi: Option<u32>) -> v1::multiplicity::Multiplicity {
    v1::multiplicity::Multiplicity {
        lower_bound: lo,
        upper_bound: hi,
    }
}

fn raw_type(path: &str) -> v1::generic_type::RawType {
    v1::generic_type::RawType::packageable(path, None)
}

fn generic(path: &str) -> v1::generic_type::GenericType {
    v1::generic_type::GenericType {
        raw_type: raw_type(path),
        type_arguments: vec![],
        multiplicity_arguments: vec![],
        type_variable_values: vec![],
        source_information: None,
    }
}

fn simple_prop(name: &str, type_path: &str, lo: u32, hi: Option<u32>) -> v1::property::Property {
    v1::property::Property {
        name: name.to_string(),
        generic_type: generic(type_path),
        multiplicity: mult(lo, hi),
        stereotypes: vec![],
        tagged_values: vec![],
        default_value: None,
        aggregation: None,
        source_information: None,
    }
}

fn var(name: &str) -> v1::value_spec::Variable {
    v1::value_spec::Variable {
        name: name.to_string(),
        generic_type: None,
        multiplicity: None,
        supports_stream: None,
        source_information: None,
    }
}

fn typed_var(name: &str, type_path: &str, lo: u32, hi: Option<u32>) -> v1::value_spec::Variable {
    v1::value_spec::Variable {
        name: name.to_string(),
        generic_type: Some(generic(type_path)),
        multiplicity: Some(mult(lo, hi)),
        supports_stream: None,
        source_information: None,
    }
}

// ---------------------------------------------------------------------------
// Snapshot: Empty class
// ---------------------------------------------------------------------------

#[test]
fn snapshot_empty_class() {
    let class = v1::element::PackageableElement::Class(v1::element::ProtocolClass {
        package_path: "model".to_string(),
        name: "Empty".to_string(),
        super_types: vec![],
        properties: vec![],
        qualified_properties: vec![],
        constraints: vec![],
        original_milestoned_properties: vec![],
        stereotypes: vec![],
        tagged_values: vec![],
        source_information: Some(si("test.pure", 1, 1, 3, 1)),
    });
    let json = serde_json::to_value(&class).unwrap();
    insta::assert_snapshot!("empty_class", json_text(&json));
}

// ---------------------------------------------------------------------------
// Snapshot: Class with properties + annotations
// ---------------------------------------------------------------------------

#[test]
fn snapshot_class_with_properties() {
    let class = v1::element::PackageableElement::Class(v1::element::ProtocolClass {
        package_path: "model::domain".to_string(),
        name: "Person".to_string(),
        super_types: vec!["model::domain::LegalEntity".into()],
        properties: vec![
            simple_prop("name", "String", 1, Some(1)),
            simple_prop("age", "Integer", 0, Some(1)),
            v1::property::Property {
                name: "address".to_string(),
                generic_type: generic("model::domain::Address"),
                multiplicity: mult(0, None),
                stereotypes: vec![],
                tagged_values: vec![],
                default_value: None,
                aggregation: Some(v1::property::AggregationKind::COMPOSITE),
                source_information: None,
            },
        ],
        qualified_properties: vec![],
        constraints: vec![],
        original_milestoned_properties: vec![],
        stereotypes: vec![v1::annotation::StereotypePtr {
            profile: "temporal".to_string(),
            value: "businesstemporal".to_string(),
            source_information: None,
            profile_source_information: None,
        }],
        tagged_values: vec![v1::annotation::TaggedValue {
            tag: v1::annotation::TagPtr {
                profile: "doc".to_string(),
                value: "description".to_string(),
                source_information: None,
                profile_source_information: None,
            },
            value: "A person entity".into(),
            source_information: None,
        }],
        source_information: Some(si("test.pure", 2, 1, 8, 1)),
    });
    let json = serde_json::to_value(&class).unwrap();
    insta::assert_snapshot!("class_with_properties", json_text(&json));
}

// ---------------------------------------------------------------------------
// Snapshot: Enumeration
// ---------------------------------------------------------------------------

#[test]
fn snapshot_enumeration() {
    let enumeration =
        v1::element::PackageableElement::Enumeration(v1::element::ProtocolEnumeration {
            package_path: "model".to_string(),
            name: "Color".to_string(),
            values: vec![
                v1::element::ProtocolEnumMember {
                    value: "RED".to_string(),
                    stereotypes: vec![],
                    tagged_values: vec![],
                    source_information: None,
                },
                v1::element::ProtocolEnumMember {
                    value: "GREEN".to_string(),
                    stereotypes: vec![],
                    tagged_values: vec![],
                    source_information: None,
                },
                v1::element::ProtocolEnumMember {
                    value: "BLUE".to_string(),
                    stereotypes: vec![v1::annotation::StereotypePtr {
                        profile: "doc".to_string(),
                        value: "deprecated".to_string(),
                        source_information: None,
                        profile_source_information: None,
                    }],
                    tagged_values: vec![],
                    source_information: None,
                },
            ],
            stereotypes: vec![],
            tagged_values: vec![],
            source_information: Some(si("test.pure", 1, 1, 5, 1)),
        });
    let json = serde_json::to_value(&enumeration).unwrap();
    insta::assert_snapshot!("enumeration", json_text(&json));
}

// ---------------------------------------------------------------------------
// Snapshot: Profile
// ---------------------------------------------------------------------------

#[test]
fn snapshot_profile() {
    let profile = v1::element::PackageableElement::Profile(v1::element::ProtocolProfile {
        package_path: "meta::pure::profiles".to_string(),
        name: "doc".to_string(),
        stereotypes: vec!["deprecated".into(), "experimental".into()],
        tags: vec!["description".into(), "todo".into()],
        source_information: Some(si("test.pure", 1, 1, 8, 1)),
    });
    let json = serde_json::to_value(&profile).unwrap();
    insta::assert_snapshot!("profile", json_text(&json));
}

// ---------------------------------------------------------------------------
// Snapshot: Function
// ---------------------------------------------------------------------------

#[test]
fn snapshot_function() {
    let func = v1::element::PackageableElement::Function(v1::element::ProtocolFunction {
        package_path: "my".to_string(),
        name: "hello".to_string(),
        parameters: vec![typed_var("name", "String", 1, Some(1))],
        return_generic_type: generic("String"),
        return_multiplicity: mult(1, Some(1)),
        body: vec![v1::value_spec::ValueSpecification::Func(
            v1::value_spec::AppliedFunction {
                function: "plus".to_string(),
                f_control: None,
                parameters: vec![
                    v1::value_spec::ValueSpecification::String(v1::value_spec::CString {
                        multi_line: false,
                        value: "Hello ".to_string(),
                        source_information: None,
                    }),
                    v1::value_spec::ValueSpecification::Var(var("name")),
                ],
                source_information: None,
            },
        )],
        stereotypes: vec![],
        tagged_values: vec![],
        tests: vec![],
        pre_constraints: vec![],
        post_constraints: vec![],
        source_information: Some(si("test.pure", 2, 1, 6, 1)),
    });
    let json = serde_json::to_value(&func).unwrap();
    insta::assert_snapshot!("function", json_text(&json));
}

// ---------------------------------------------------------------------------
// Snapshot: Association
// ---------------------------------------------------------------------------

#[test]
fn snapshot_association() {
    let assoc = v1::element::PackageableElement::Association(v1::element::ProtocolAssociation {
        package_path: "model".to_string(),
        name: "Person_Address".to_string(),
        properties: vec![
            simple_prop("addresses", "model::Address", 0, None),
            simple_prop("person", "model::Person", 1, Some(1)),
        ],
        qualified_properties: vec![],
        original_milestoned_properties: vec![],
        stereotypes: vec![],
        tagged_values: vec![],
        source_information: Some(si("test.pure", 1, 1, 5, 1)),
    });
    let json = serde_json::to_value(&assoc).unwrap();
    insta::assert_snapshot!("association", json_text(&json));
}

// ---------------------------------------------------------------------------
// Snapshot: Measure with units
// ---------------------------------------------------------------------------

#[test]
fn snapshot_measure() {
    let measure = v1::element::PackageableElement::Measure(v1::element::ProtocolMeasure {
        package_path: "pkg".to_string(),
        name: "Distance".to_string(),
        canonical_unit: Some(v1::element::ProtocolUnit {
            name: "Meter".to_string(),
            measure: "pkg::Distance".to_string(),
            conversion_function: None,
            source_information: None,
        }),
        non_canonical_units: Some(vec![v1::element::ProtocolUnit {
            name: "Kilometer".to_string(),
            measure: "pkg::Distance".to_string(),
            conversion_function: Some(v1::value_spec::ValueSpecification::Lambda(
                v1::value_spec::LambdaFunction {
                    parameters: vec![var("x")],
                    body: vec![v1::value_spec::ValueSpecification::Func(
                        v1::value_spec::AppliedFunction {
                            function: "times".to_string(),
                            f_control: None,
                            parameters: vec![
                                v1::value_spec::ValueSpecification::Var(var("x")),
                                v1::value_spec::ValueSpecification::Integer(
                                    v1::value_spec::CInteger {
                                        value: 1000,
                                        source_information: None,
                                    },
                                ),
                            ],
                            source_information: None,
                        },
                    )],
                    source_information: None,
                },
            )),
            source_information: None,
        }]),
        source_information: Some(si("test.pure", 1, 1, 5, 1)),
    });
    let json = serde_json::to_value(&measure).unwrap();
    insta::assert_snapshot!("measure", json_text(&json));
}

// ---------------------------------------------------------------------------
// Snapshot: PureModelContextData (full model)
// ---------------------------------------------------------------------------

#[test]
fn snapshot_pure_model_context() {
    let ctx = v1::context::PureModelContextData {
        serializer: Some(v1::context::Protocol {
            name: "pure".to_string(),
            version: "vX_X_X".to_string(),
        }),
        elements: vec![
            v1::element::PackageableElement::Class(v1::element::ProtocolClass {
                package_path: "model".to_string(),
                name: "Person".to_string(),
                super_types: vec![],
                properties: vec![simple_prop("name", "String", 1, Some(1))],
                qualified_properties: vec![],
                constraints: vec![],
                original_milestoned_properties: vec![],
                stereotypes: vec![],
                tagged_values: vec![],
                source_information: None,
            }),
            v1::element::PackageableElement::Enumeration(v1::element::ProtocolEnumeration {
                package_path: "model".to_string(),
                name: "Status".to_string(),
                values: vec![
                    v1::element::ProtocolEnumMember {
                        value: "ACTIVE".to_string(),
                        stereotypes: vec![],
                        tagged_values: vec![],
                        source_information: None,
                    },
                    v1::element::ProtocolEnumMember {
                        value: "INACTIVE".to_string(),
                        stereotypes: vec![],
                        tagged_values: vec![],
                        source_information: None,
                    },
                ],
                stereotypes: vec![],
                tagged_values: vec![],
                source_information: None,
            }),
            v1::element::PackageableElement::SectionIndex(v1::element::ProtocolSectionIndex {
                package_path: "__internal__".to_string(),
                name: "test.pure".to_string(),
                sections: vec![v1::element::ProtocolSection::Default(
                    v1::element::DefaultCodeSection {
                        parser_name: "Pure".to_string(),
                        elements: vec!["model::Person".to_string(), "model::Status".to_string()],
                        source_information: None,
                    },
                )],
                source_information: None,
            }),
        ],
    };
    let json = serde_json::to_value(&ctx).unwrap();
    insta::assert_snapshot!("pure_model_context", json_text(&json));
}

// ---------------------------------------------------------------------------
// Snapshot: Expressions
// ---------------------------------------------------------------------------

#[test]
fn snapshot_arithmetic_expression() {
    // 1 + 2 → func("plus", [integer(1), integer(2)])
    let vs = v1::value_spec::ValueSpecification::Func(v1::value_spec::AppliedFunction {
        function: "plus".to_string(),
        f_control: None,
        parameters: vec![
            v1::value_spec::ValueSpecification::Integer(v1::value_spec::CInteger {
                value: 1,
                source_information: Some(si("test.pure", 1, 1, 1, 1)),
            }),
            v1::value_spec::ValueSpecification::Integer(v1::value_spec::CInteger {
                value: 2,
                source_information: Some(si("test.pure", 1, 5, 1, 5)),
            }),
        ],
        source_information: Some(si("test.pure", 1, 1, 1, 5)),
    });
    let json = serde_json::to_value(&vs).unwrap();
    insta::assert_snapshot!("arithmetic_expression", json_text(&json));
}

#[test]
fn snapshot_let_expression() {
    // let x = 42 → func("letFunction", [string("x"), integer(42)])
    let vs = v1::value_spec::ValueSpecification::Func(v1::value_spec::AppliedFunction {
        function: "letFunction".to_string(),
        f_control: None,
        parameters: vec![
            v1::value_spec::ValueSpecification::String(v1::value_spec::CString {
                multi_line: false,
                value: "x".to_string(),
                source_information: None,
            }),
            v1::value_spec::ValueSpecification::Integer(v1::value_spec::CInteger {
                value: 42,
                source_information: None,
            }),
        ],
        source_information: Some(si("test.pure", 3, 5, 3, 18)),
    });
    let json = serde_json::to_value(&vs).unwrap();
    insta::assert_snapshot!("let_expression", json_text(&json));
}

#[test]
fn snapshot_lambda_expression() {
    // {x: String[1] | $x->toUpperCase()}
    let vs = v1::value_spec::ValueSpecification::Lambda(v1::value_spec::LambdaFunction {
        parameters: vec![typed_var("x", "String", 1, Some(1))],
        body: vec![v1::value_spec::ValueSpecification::Func(
            v1::value_spec::AppliedFunction {
                function: "toUpperCase".to_string(),
                f_control: None,
                parameters: vec![v1::value_spec::ValueSpecification::Var(var("x"))],
                source_information: None,
            },
        )],
        source_information: None,
    });
    let json = serde_json::to_value(&vs).unwrap();
    insta::assert_snapshot!("lambda_expression", json_text(&json));
}

#[test]
fn snapshot_member_access() {
    // $this.name → property("name", [$this])
    let vs = v1::value_spec::ValueSpecification::Property(v1::value_spec::AppliedProperty {
        class: None,
        property: "name".to_string(),
        parameters: vec![v1::value_spec::ValueSpecification::Var(var("this"))],
        source_information: None,
    });
    let json = serde_json::to_value(&vs).unwrap();
    insta::assert_snapshot!("member_access", json_text(&json));
}

#[test]
fn snapshot_nested_expression() {
    // $this.firstName + $sep + $this.lastName
    // → plus(plus(property(firstName, $this), $sep), property(lastName, $this))
    let vs = v1::value_spec::ValueSpecification::Func(v1::value_spec::AppliedFunction {
        function: "plus".to_string(),
        f_control: None,
        parameters: vec![
            v1::value_spec::ValueSpecification::Func(v1::value_spec::AppliedFunction {
                function: "plus".to_string(),
                f_control: None,
                parameters: vec![
                    v1::value_spec::ValueSpecification::Property(v1::value_spec::AppliedProperty {
                        class: None,
                        property: "firstName".to_string(),
                        parameters: vec![v1::value_spec::ValueSpecification::Var(var("this"))],
                        source_information: None,
                    }),
                    v1::value_spec::ValueSpecification::Var(var("sep")),
                ],
                source_information: None,
            }),
            v1::value_spec::ValueSpecification::Property(v1::value_spec::AppliedProperty {
                class: None,
                property: "lastName".to_string(),
                parameters: vec![v1::value_spec::ValueSpecification::Var(var("this"))],
                source_information: None,
            }),
        ],
        source_information: None,
    });
    let json = serde_json::to_value(&vs).unwrap();
    insta::assert_snapshot!("nested_expression", json_text(&json));
}

// ---------------------------------------------------------------------------
// Snapshot: Class with constraint
// ---------------------------------------------------------------------------

#[test]
fn snapshot_class_with_constraint() {
    let class = v1::element::PackageableElement::Class(v1::element::ProtocolClass {
        package_path: "my".to_string(),
        name: "ConstrainedClass".to_string(),
        super_types: vec![],
        properties: vec![simple_prop("name", "String", 1, Some(1))],
        qualified_properties: vec![],
        constraints: vec![v1::property::Constraint {
            name: "nameNotEmpty".to_string(),
            owner: None,
            function_definition: serde_json::json!({
                "_type": "func",
                "function": "isNotEmpty",
                "parameters": [
                    {
                        "_type": "property",
                        "property": "name",
                        "parameters": [
                            { "_type": "var", "name": "this" }
                        ]
                    }
                ]
            }),
            enforcement_level: Some("Error".to_string()),
            external_id: Some("RULE-001".to_string()),
            message_function: Some(serde_json::json!({
                "_type": "string",
                "value": "Name must not be empty"
            })),
            source_information: None,
        }],
        original_milestoned_properties: vec![],
        stereotypes: vec![],
        tagged_values: vec![],
        source_information: None,
    });
    let json = serde_json::to_value(&class).unwrap();
    insta::assert_snapshot!("class_with_constraint", json_text(&json));
}

// ---------------------------------------------------------------------------
// Snapshot: Class with qualified property
// ---------------------------------------------------------------------------

#[test]
fn snapshot_class_with_qualified_property() {
    let class = v1::element::PackageableElement::Class(v1::element::ProtocolClass {
        package_path: "model::domain".to_string(),
        name: "Person".to_string(),
        super_types: vec![],
        properties: vec![
            simple_prop("firstName", "String", 1, Some(1)),
            simple_prop("lastName", "String", 1, Some(1)),
        ],
        qualified_properties: vec![v1::property::QualifiedProperty {
            name: "fullName".to_string(),
            parameters: vec![serde_json::json!({
                "_type": "var",
                "name": "sep",
                "genericType": {
                    "rawType": { "fullPath": "String" },
                },
                "multiplicity": { "lowerBound": 1, "upperBound": 1 }
            })],
            return_generic_type: generic("String"),
            return_multiplicity: mult(1, Some(1)),
            body: vec![serde_json::json!({
                "_type": "func",
                "function": "plus",
                "parameters": [
                    {
                        "_type": "func",
                        "function": "plus",
                        "parameters": [
                            { "_type": "property", "property": "firstName", "parameters": [{ "_type": "var", "name": "this" }] },
                            { "_type": "var", "name": "sep" }
                        ]
                    },
                    { "_type": "property", "property": "lastName", "parameters": [{ "_type": "var", "name": "this" }] }
                ]
            })],
            stereotypes: vec![],
            tagged_values: vec![],
            source_information: None,
        }],
        constraints: vec![],
        original_milestoned_properties: vec![],
        stereotypes: vec![],
        tagged_values: vec![],
        source_information: None,
    });
    let json = serde_json::to_value(&class).unwrap();
    insta::assert_snapshot!("class_with_qualified_property", json_text(&json));
}

// ---------------------------------------------------------------------------
// Snapshot: Section index with imports
// ---------------------------------------------------------------------------

#[test]
fn snapshot_section_index_with_imports() {
    let section_index =
        v1::element::PackageableElement::SectionIndex(v1::element::ProtocolSectionIndex {
            package_path: "__internal__".to_string(),
            name: "test.pure".to_string(),
            sections: vec![
                v1::element::ProtocolSection::ImportAware(v1::element::ImportAwareCodeSection {
                    parser_name: "Pure".to_string(),
                    elements: vec!["model::Person".to_string(), "model::Address".to_string()],
                    imports: vec!["model::domain".to_string(), "model::common".to_string()],
                    source_information: Some(si("test.pure", 1, 1, 20, 1)),
                }),
                v1::element::ProtocolSection::Default(v1::element::DefaultCodeSection {
                    parser_name: "Relational".to_string(),
                    elements: vec!["model::store::MyStore".to_string()],
                    source_information: Some(si("test.pure", 21, 1, 40, 1)),
                }),
            ],
            source_information: Some(si("test.pure", 1, 1, 40, 1)),
        });
    let json = serde_json::to_value(&section_index).unwrap();
    insta::assert_snapshot!("section_index_with_imports", json_text(&json));
}

// ---------------------------------------------------------------------------
// Snapshot: All literal types
// ---------------------------------------------------------------------------

#[test]
fn snapshot_all_literal_types() {
    let literals = vec![
        (
            "integer",
            v1::value_spec::ValueSpecification::Integer(v1::value_spec::CInteger {
                value: 42,
                source_information: None,
            }),
        ),
        (
            "float",
            v1::value_spec::ValueSpecification::Float(v1::value_spec::CFloat {
                value: 2.72,
                source_information: None,
            }),
        ),
        (
            "decimal",
            v1::value_spec::ValueSpecification::Decimal(v1::value_spec::CDecimal {
                value: "99.99".parse().unwrap(),
                source_information: None,
            }),
        ),
        (
            "string",
            v1::value_spec::ValueSpecification::String(v1::value_spec::CString {
                multi_line: false,
                value: "hello world".to_string(),
                source_information: None,
            }),
        ),
        (
            "boolean_true",
            v1::value_spec::ValueSpecification::Boolean(v1::value_spec::CBoolean {
                value: true,
                source_information: None,
            }),
        ),
        (
            "boolean_false",
            v1::value_spec::ValueSpecification::Boolean(v1::value_spec::CBoolean {
                value: false,
                source_information: None,
            }),
        ),
        (
            "datetime",
            v1::value_spec::ValueSpecification::DateTime(v1::value_spec::CDateTime {
                value: "2024-01-15T10:30:00.000Z".to_string(),
                source_information: None,
            }),
        ),
        (
            "strict_date",
            v1::value_spec::ValueSpecification::StrictDate(v1::value_spec::CStrictDate {
                value: "2024-01-15".to_string(),
                source_information: None,
            }),
        ),
        (
            "strict_time",
            v1::value_spec::ValueSpecification::StrictTime(v1::value_spec::CStrictTime {
                value: "10:30:00".to_string(),
                source_information: None,
            }),
        ),
        (
            "latest_date",
            v1::value_spec::ValueSpecification::LatestDate(v1::value_spec::CLatestDate {
                source_information: None,
            }),
        ),
    ];

    for (name, vs) in &literals {
        let json = serde_json::to_value(vs).unwrap();
        insta::assert_snapshot!(format!("literal_{name}"), json_text(&json));
    }
}

// ---------------------------------------------------------------------------
// Snapshot: Collection expression
// ---------------------------------------------------------------------------

#[test]
fn snapshot_collection() {
    let vs = v1::value_spec::ValueSpecification::Collection(v1::value_spec::ProtocolCollection {
        multiplicity: mult(3, Some(3)),
        values: vec![
            v1::value_spec::ValueSpecification::Integer(v1::value_spec::CInteger {
                value: 1,
                source_information: None,
            }),
            v1::value_spec::ValueSpecification::Integer(v1::value_spec::CInteger {
                value: 2,
                source_information: None,
            }),
            v1::value_spec::ValueSpecification::Integer(v1::value_spec::CInteger {
                value: 3,
                source_information: None,
            }),
        ],
        source_information: None,
    });
    let json = serde_json::to_value(&vs).unwrap();
    insta::assert_snapshot!("collection", json_text(&json));
}
