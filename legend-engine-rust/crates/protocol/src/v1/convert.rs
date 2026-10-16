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

//! AST → Protocol v1 conversion.
//!
//! Converts the parser's AST types into the protocol v1 JSON model.
//! Each conversion is implemented as a `From` trait impl or a free function.
//!
//! ## Design Principles
//!
//! - **Fallible**: Conversion functions return [`Result`]. Constructs Protocol
//!   v1 cannot represent, or that no linked plug-in converts, are
//!   [`ConvertError::Unsupported`]; nothing is silently dropped or replaced
//!   with a placeholder.
//! - **No state**: Conversions are pure functions with no shared mutable state.
//! - **Recursive**: Complex types (expressions, elements) recurse into children.

use legend_pure_parser_ast as ast;
use legend_pure_parser_ast::source_info::Spanned;
use legend_pure_parser_ast::type_ref::HasMultiplicity;
use serde::Serialize;

pub use crate::error::{ConvertError, Result};
use crate::v1;

impl From<&ast::SourceInfo> for v1::source_info::SourceInformation {
    fn from(si: &ast::SourceInfo) -> Self {
        Self {
            source_id: si.source.to_string(),
            start_line: si.start_line,
            start_column: si.start_column,
            end_line: si.end_line,
            end_column: si.end_column,
        }
    }
}

impl From<ast::SourceInfo> for v1::source_info::SourceInformation {
    fn from(si: ast::SourceInfo) -> Self {
        Self::from(&si)
    }
}

/// Converts a borrowed AST [`SourceInfo`](ast::SourceInfo) into a protocol
/// `Option<SourceInformation>` without cloning.
///
/// This is the standard way to populate `source_information` fields during
/// AST → Protocol conversion.
#[allow(clippy::unnecessary_wraps)]
fn source_information(source_info: &ast::SourceInfo) -> Option<v1::source_info::SourceInformation> {
    Some(source_info.into())
}

impl From<&ast::Multiplicity> for v1::multiplicity::Multiplicity {
    fn from(m: &ast::Multiplicity) -> Self {
        Self {
            lower_bound: m.lower(),
            upper_bound: m.upper(),
        }
    }
}

/// Converts a recursive AST `Package` into a flat `"a::b::c"` path string.
fn package_to_path(pkg: &ast::type_ref::Package) -> String {
    pkg.to_string()
}

/// Converts an optional AST `Package` into the flat path string used by protocol.
fn optional_package_to_path(pkg: Option<&ast::type_ref::Package>) -> String {
    match pkg {
        Some(p) => package_to_path(p),
        None => String::new(),
    }
}

// The engine's `processGenericType` positions the raw type (the whole type
// text) and never the `GenericType` itself.
impl From<&ast::type_ref::TypeReference> for v1::generic_type::GenericType {
    fn from(tr: &ast::type_ref::TypeReference) -> Self {
        // The engine's backward-compatibility rule for types written before
        // `Result` took type arguments: bare `Result` is `Result<Any|1..*>`.
        if tr.package.is_none() && tr.name == "Result" && tr.type_arguments.is_empty() {
            let any = Self {
                raw_type: v1::generic_type::RawType::packageable(
                    "meta::pure::metamodel::type::Any",
                    None,
                ),
                type_arguments: vec![],
                multiplicity_arguments: vec![],
                type_variable_values: vec![],
                source_information: None,
            };
            return Self {
                raw_type: v1::generic_type::RawType::packageable(
                    tr.full_path(),
                    source_information(&tr.source_info),
                ),
                type_arguments: vec![any],
                multiplicity_arguments: vec![v1::multiplicity::Multiplicity::ONE_MANY],
                type_variable_values: tr
                    .type_variable_values
                    .iter()
                    .map(type_variable_value)
                    .collect(),
                source_information: None,
            };
        }
        Self {
            raw_type: v1::generic_type::RawType::packageable(
                tr.full_path(),
                source_information(&tr.source_info),
            ),
            type_arguments: tr.type_arguments.iter().map(Into::into).collect(),
            multiplicity_arguments: tr
                .multiplicity_arguments
                .iter()
                .map(|ma| match ma {
                    ast::type_ref::MultiplicityArgument::Identifier(_, _) => {
                        // Named multiplicity variables produce a placeholder multiplicity.
                        // The Java protocol uses the same representation — the name is lost
                        // and must be recovered by the compiler from parameter position.
                        v1::multiplicity::Multiplicity::PURE_ONE
                    }
                    ast::type_ref::MultiplicityArgument::Concrete(m, _) => m.into(),
                })
                .collect(),
            type_variable_values: tr
                .type_variable_values
                .iter()
                .map(type_variable_value)
                .collect(),
            source_information: None,
        }
    }
}

// The engine grammar's type arguments are plain types: Legend Pure's
// binding, relation algebra and bound (`Z=T - X ⊆ S`) have no protocol form.
impl From<&ast::type_ref::TypeArgument> for v1::generic_type::GenericType {
    fn from(argument: &ast::type_ref::TypeArgument) -> Self {
        (&argument.type_spec).into()
    }
}

impl From<&ast::type_ref::TypeSpec> for v1::generic_type::GenericType {
    fn from(ts: &ast::type_ref::TypeSpec) -> Self {
        match ts {
            ast::type_ref::TypeSpec::Type(tr) => tr.into(),
            ast::type_ref::TypeSpec::Unit(ur) => {
                let mut gt: Self = (&ur.measure).into();
                if let v1::generic_type::RawType::Packageable(p) = &mut gt.raw_type {
                    p.full_path = ts.full_path();
                    p.source_information = source_information(&ur.source_info);
                }
                gt
            }
            // The engine's `relationType`: columns with their types and
            // multiplicities (`[0..1]` when not written).
            ast::type_ref::TypeSpec::Relation(rt) => v1::generic_type::GenericType {
                raw_type: v1::generic_type::RawType::Relation(v1::generic_type::RelationType {
                    columns: rt
                        .columns
                        .iter()
                        .map(|col| v1::generic_type::Column {
                            name: col.name.to_string(),
                            generic_type: (&col.type_ref).into(),
                            multiplicity: col
                                .multiplicity
                                .as_ref()
                                .map_or(v1::multiplicity::Multiplicity::ZERO_ONE, Into::into),
                            source_information: source_information(&col.source_info),
                        })
                        .collect(),
                    source_information: None,
                }),
                type_arguments: vec![],
                multiplicity_arguments: vec![],
                type_variable_values: vec![],
                source_information: None,
            },
            ast::type_ref::TypeSpec::Function(ft) => v1::generic_type::GenericType {
                raw_type: v1::generic_type::RawType::packageable(
                    "meta::pure::metamodel::type::FunctionType",
                    source_information(&ft.source_info),
                ),
                type_arguments: vec![],
                multiplicity_arguments: vec![],
                type_variable_values: vec![],
                source_information: None,
            },
        }
    }
}

/// A type variable value, e.g. `200` of `VARCHAR(200)`: the engine's
/// `instanceLiteral`, positioned.
fn type_variable_value(
    tvv: &ast::type_ref::TypeVariableValue,
) -> v1::value_spec::ValueSpecification {
    use v1::value_spec::{CInteger, CString, ValueSpecification};
    match tvv {
        ast::type_ref::TypeVariableValue::Integer(value, si) => {
            ValueSpecification::Integer(CInteger {
                value: *value,
                source_information: source_information(si),
            })
        }
        ast::type_ref::TypeVariableValue::String(value, si) => {
            ValueSpecification::String(CString {
                multi_line: false,
                value: value.clone(),
                source_information: source_information(si),
            })
        }
    }
}

impl From<&ast::annotation::StereotypePtr> for v1::annotation::StereotypePtr {
    fn from(s: &ast::annotation::StereotypePtr) -> Self {
        Self {
            profile: s.profile.to_string(),
            value: s.value.to_string(),
            source_information: source_information(&s.source_info),
            profile_source_information: source_information(&s.profile.source_info),
        }
    }
}

/// A stereotype as the engine's section walkers other than the Pure one
/// (`DataParseTreeWalker`, `ServiceParseTreeWalker`, …) position it: on its
/// value alone. The `From` conversion is the Pure walker's, on
/// `profile.value`.
#[must_use]
pub fn section_stereotype(
    stereotype: &ast::annotation::StereotypePtr,
) -> v1::annotation::StereotypePtr {
    v1::annotation::StereotypePtr {
        source_information: source_information(&stereotype.value_source_info),
        ..stereotype.into()
    }
}

impl From<&ast::annotation::TagPtr> for v1::annotation::TagPtr {
    fn from(t: &ast::annotation::TagPtr) -> Self {
        Self {
            profile: t.profile.to_string(),
            value: t.value.to_string(),
            source_information: source_information(&t.source_info),
            profile_source_information: source_information(&t.profile.source_info),
        }
    }
}

impl From<&ast::annotation::TaggedValue> for v1::annotation::TaggedValue {
    fn from(tv: &ast::annotation::TaggedValue) -> Self {
        Self {
            tag: (&tv.tag).into(),
            value: v1::annotation::TaggedValueText {
                value: tv.value.clone(),
                multi_line: tv.multi_line,
            },
            source_information: source_information(&tv.source_info),
        }
    }
}

/// Converts an AST `Property` into a protocol `Property`.
///
/// # Errors
///
/// Returns an error if expression serialization within default values fails.
fn convert_property(p: &ast::element::Property) -> Result<v1::property::Property> {
    let default_value = match &p.default_value {
        Some(dv) => Some(v1::property::DefaultValue {
            value: convert_expression(&dv.value)?,
            source_information: source_information(&dv.source_info),
        }),
        None => None,
    };
    Ok(v1::property::Property {
        name: p.name.to_string(),
        generic_type: (&p.type_ref).into(),
        multiplicity: (&p.multiplicity).into(),
        default_value,
        stereotypes: p.stereotypes.iter().map(Into::into).collect(),
        tagged_values: p.tagged_values.iter().map(Into::into).collect(),
        aggregation: p.aggregation.map(std::convert::Into::into),
        source_information: source_information(&p.source_info),
    })
}

impl From<ast::element::AggregationKind> for v1::property::AggregationKind {
    fn from(ak: ast::element::AggregationKind) -> Self {
        match ak {
            ast::element::AggregationKind::None => Self::NONE,
            ast::element::AggregationKind::Shared => Self::SHARED,
            ast::element::AggregationKind::Composite => Self::COMPOSITE,
        }
    }
}

/// Converts an AST `QualifiedProperty` into a protocol `QualifiedProperty`.
///
/// # Errors
///
/// Returns an error if parameter or body expression serialization fails.
fn convert_qualified_property(
    qp: &ast::element::QualifiedProperty,
) -> Result<v1::property::QualifiedProperty> {
    let parameters: std::result::Result<Vec<_>, _> =
        qp.parameters.iter().map(convert_parameter).collect();
    let body: std::result::Result<Vec<_>, _> = qp.body.iter().map(convert_expression).collect();
    Ok(v1::property::QualifiedProperty {
        name: qp.name.to_string(),
        parameters: parameters?,
        return_generic_type: (&qp.return_type).into(),
        return_multiplicity: (&qp.return_multiplicity).into(),
        stereotypes: qp.stereotypes.iter().map(Into::into).collect(),
        tagged_values: qp.tagged_values.iter().map(Into::into).collect(),
        body: body?,
        source_information: source_information(&qp.source_info),
    })
}

/// Converts an AST `Constraint` into a protocol `Constraint`, as the engine's
/// `DomainParseTreeWalker.visitConstraint` builds it: an unnamed constraint
/// is named by its position in the class's constraint list, and the
/// function and message are lambdas over `$this`.
///
/// # Errors
///
/// Returns an error if constraint expression conversion fails.
fn convert_constraint(
    index: usize,
    c: &ast::element::Constraint,
) -> Result<v1::property::Constraint> {
    Ok(v1::property::Constraint {
        name: c
            .name
            .as_ref()
            .map_or_else(|| index.to_string(), ToString::to_string),
        owner: c.owner.as_ref().map(ToString::to_string),
        function_definition: constraint_lambda(&c.function_definition)?,
        source_information: source_information(&c.source_info),
        external_id: c.external_id.clone(),
        enforcement_level: c.enforcement_level.as_ref().map(ToString::to_string),
        message_function: c.message.as_ref().map(constraint_lambda).transpose()?,
    })
}

/// A constraint function or message: `{$this: [1] | body}` with no source
/// information, as the engine builds it.
fn constraint_lambda(body: &ast::expression::Expression) -> Result<serde_json::Value> {
    let this = v1::value_spec::Variable {
        name: "this".to_string(),
        generic_type: None,
        multiplicity: Some(v1::multiplicity::Multiplicity::PURE_ONE),
        supports_stream: None,
        source_information: None,
    };
    let lambda = v1::value_spec::ValueSpecification::Lambda(v1::value_spec::LambdaFunction {
        body: vec![convert_expression_typed(body)?],
        parameters: vec![this],
        source_information: None,
    });
    Ok(serde_json::to_value(&lambda)?)
}

/// A parameter's type as the engine's protocol mapper reads it back, which
/// is how the engine reads this protocol: its
/// `fixVariableOfResultTypeWithMissingTypeAndMultiplicityArgument` converter
/// gives a `Result` without type arguments `Any` and `[1..*]` (the engine's
/// `PURE_MANY`) as its arguments.
fn parameter_type(
    type_ref: Option<&ast::type_ref::TypeSpec>,
) -> Option<v1::generic_type::GenericType> {
    let mut generic_type: v1::generic_type::GenericType = type_ref?.into();
    if matches!(
        generic_type.raw_type.full_path(),
        Some("meta::pure::mapping::Result" | "Result")
    ) && generic_type.type_arguments.is_empty()
    {
        generic_type.type_arguments = vec![v1::generic_type::GenericType {
            raw_type: v1::generic_type::RawType::packageable(
                "meta::pure::metamodel::type::Any",
                None,
            ),
            type_arguments: Vec::new(),
            multiplicity_arguments: Vec::new(),
            type_variable_values: Vec::new(),
            source_information: None,
        }];
        generic_type.multiplicity_arguments = vec![v1::multiplicity::Multiplicity::ONE_MANY];
    }
    Some(generic_type)
}

/// Converts a `Parameter` to a serialized variable value specification.
///
/// # Errors
///
/// Returns an error if the variable serialization fails.
fn convert_parameter(p: &ast::annotation::Parameter) -> Result<serde_json::Value> {
    let var = v1::value_spec::Variable {
        name: p.name.to_string(),
        generic_type: parameter_type(p.type_ref.as_ref()),
        multiplicity: p.multiplicity.as_ref().map(Into::into),
        supports_stream: None,
        source_information: source_information(&p.source_info),
    };
    let vs = v1::value_spec::ValueSpecification::Var(var);
    Ok(serde_json::to_value(&vs)?)
}

/// A decimal literal's value as written, negated when `negative`: its digits
/// and scale, as the engine's `BigDecimal` keeps them (`1D` is `1`, `2.10D`
/// is `2.10`).
fn decimal_value(
    digits: &str,
    negative: bool,
    source_info: &ast::source_info::SourceInfo,
) -> Result<serde_json::Number> {
    let number = if digits.starts_with('.') {
        format!("0{digits}")
    } else {
        digits.to_string()
    };
    let signed = if negative {
        format!("-{number}")
    } else {
        number
    };
    signed.parse::<serde_json::Number>().map_err(|_| {
        ConvertError::unsupported(
            format!("decimal literal '{digits}' is not a number"),
            source_info,
        )
    })
}

/// Converts an AST `Expression` into a protocol `serde_json::Value`.
///
/// Returns a `serde_json::Value` rather than a typed `ValueSpecification`
/// because expressions can appear in contexts that expect `serde_json::Value`
/// (e.g., constraint function definitions, qualified property bodies).
///
/// # Errors
///
/// Returns an error if the expression cannot be converted or serialized.
pub fn convert_expression(expr: &ast::expression::Expression) -> Result<serde_json::Value> {
    let vs = convert_expression_typed(expr)?;
    Ok(serde_json::to_value(&vs)?)
}

/// Converts every expression in `exprs`, stopping at the first failure.
fn convert_all<'a, I>(exprs: I) -> Result<Vec<v1::value_spec::ValueSpecification>>
where
    I: IntoIterator<Item = &'a ast::expression::Expression>,
{
    exprs.into_iter().map(convert_expression_typed).collect()
}

/// Converts an AST `Expression` into a typed `ValueSpecification`.
///
/// # Errors
///
/// [`ConvertError::Unsupported`] for constructs with no Protocol v1 form
/// (slices, islands without a registered converter, …); otherwise any
/// nested conversion or plug-in failure.
#[allow(clippy::too_many_lines)]
pub fn convert_expression_typed(
    expr: &ast::expression::Expression,
) -> Result<v1::value_spec::ValueSpecification> {
    use ast::expression::Expression;
    use v1::value_spec::AppliedFunction;
    use v1::value_spec::CString;
    use v1::value_spec::LambdaFunction;
    use v1::value_spec::ProtocolCollection;
    use v1::value_spec::ProtocolPackageableElementPtr;
    use v1::value_spec::ValueSpecification;
    use v1::value_spec::Variable;

    Ok(match expr {
        // -- Literals --
        Expression::Literal(lit) => convert_literal(lit)?,

        // -- Variables --
        Expression::Variable(var) => ValueSpecification::Var(Variable {
            name: var.name.to_string(),
            generic_type: None,
            multiplicity: None,
            supports_stream: None,
            source_information: source_information(&var.source_info),
        }),

        // -- Operators: the engine's own operator-tree shapes --
        Expression::Arithmetic(_)
        | Expression::Comparison(_)
        | Expression::Logical(_)
        | Expression::Not(_)
        | Expression::UnaryMinus(_) => super::engine_operators::convert_operator_chain(expr)?,
        // The engine grammar has no bitwise operators.
        Expression::Bitwise(e) => {
            return Err(ConvertError::unsupported(
                "bitwise operators have no Engine grammar form",
                &e.source_info,
            ));
        }
        Expression::BitwiseNot(e) => {
            return Err(ConvertError::unsupported(
                "bitwise operators have no Engine grammar form",
                &e.source_info,
            ));
        }

        // -- Function application --
        // The engine positions a call on its (qualified) function name.
        Expression::FunctionApplication(e) => ValueSpecification::Func(AppliedFunction {
            function: e.function.to_string(),
            f_control: None,
            parameters: convert_all(&e.arguments)?,
            source_information: source_information(&e.function.source_info),
        }),

        // -- Arrow function: `expr->func(args)` desugars to func(expr, args) --
        Expression::ArrowFunction(e) => ValueSpecification::Func(AppliedFunction {
            function: e.function.to_string(),
            f_control: None,
            parameters: convert_all(std::iter::once(e.target.as_ref()).chain(&e.arguments))?,
            source_information: source_information(&e.function.source_info),
        }),

        // -- Member access --
        Expression::MemberAccess(ma) => convert_member_access(ma)?,

        // -- Type reference: `@MyType` → packageableElementPtr --
        // `@Type` is always a generic type instance in the engine.
        Expression::TypeReferenceExpr(e) => {
            let mut generic_type: v1::generic_type::GenericType = (&e.type_ref).into();
            let mut span = &e.source_info;
            // A unit here is the engine's `unitTypeReference`: positioned on
            // the unit, without `@`, and its raw type unpositioned (a unit
            // elsewhere is positioned).
            if let (
                ast::type_ref::TypeSpec::Unit(unit),
                v1::generic_type::RawType::Packageable(raw_type),
            ) = (&e.type_ref, &mut generic_type.raw_type)
            {
                raw_type.source_information = None;
                span = &unit.source_info;
            }
            ValueSpecification::GenericTypeInstance(v1::value_spec::ProtocolGenericTypeInstance {
                generic_type,
                source_information: source_information(span),
            })
        }

        //
        // Settled wire form (mirrors what Java's `MultiplicityInstance`
        // round-trips to at the metamodel level):
        //
        // - Well-known multiplicities (`@[1]`, `@[0..1]`, `@[*]`,
        //   `@[1..*]`) → `packageableElementPtr` pointing at the
        //   corresponding `meta::pure::metamodel::multiplicity::{PureOne,
        //   ZeroOne, ZeroMany, OneMany}` `PackageableMultiplicity`
        //   instance.
        // - Arbitrary ranges (`@[2..5]`) → `classInstance("multiplicity",
        //   { lowerBound, upperBound? })`.
        // - Parameter variables (`@[m]`) → `classInstance("multiplicity",
        //   { multiplicityParameter })`.
        Expression::MultiplicityReferenceExpr(e) => {
            multiplicity_arg_to_value_spec(&e.multiplicity, source_information(&e.source_info))
        }

        // -- Lambda --
        Expression::Lambda(e) => ValueSpecification::Lambda(LambdaFunction {
            body: convert_all(&e.body)?,
            parameters: e
                .parameters
                .iter()
                .map(|p| Variable {
                    name: p.name.to_string(),
                    generic_type: parameter_type(p.type_ref.as_ref()),
                    multiplicity: p.multiplicity.as_ref().map(Into::into),
                    supports_stream: None,
                    // The engine positions a lambda parameter only when it
                    // declares a type.
                    source_information: p
                        .type_ref
                        .as_ref()
                        .and_then(|_| source_information(&p.source_info)),
                })
                .collect(),
            source_information: source_information(&e.source_info),
        }),

        // -- Let: desugared to letFunction('name', expr) --
        Expression::Let(e) => {
            // The engine leaves the variable name unpositioned.
            let name_val = ValueSpecification::String(CString {
                multi_line: false,
                value: e.name.to_string(),
                source_information: None,
            });
            // The engine overwrites the value's position with the whole
            // `let` statement's (`DomainParseTreeWalker.letExpression`).
            let mut value_val = convert_expression_typed(&e.value)?;
            value_val.set_source_information(source_information(&e.source_info));
            ValueSpecification::Func(AppliedFunction {
                function: "letFunction".to_string(),
                f_control: None,
                parameters: vec![name_val, value_val],
                source_information: source_information(&e.source_info),
            })
        }

        // -- Collection literal --
        Expression::Collection(e) => ValueSpecification::Collection(ProtocolCollection {
            multiplicity: collection_multiplicity(e.elements.len()),
            values: convert_all(&e.elements)?,
            source_information: source_information(&e.source_info),
        }),

        // Slices (`$x[1:3]`) are legend-pure grammar with no Engine protocol form.
        Expression::Slice(e) => {
            return Err(ConvertError::unsupported(
                "slice expressions have no Engine protocol form",
                &e.source_info,
            ));
        }

        // -- New instance: `^MyClass(name='John')` (see `convert_new_instance`) --
        Expression::NewInstance(e) => convert_new_instance(e)?,

        // -- Copy from variable: `^$var(props)` → copy(var, key-expressions) --
        Expression::Copy(e) => {
            let source_var = ValueSpecification::Var(Variable {
                name: e.source.to_string(),
                generic_type: None,
                multiplicity: None,
                supports_stream: None,
                source_information: source_information(&e.source_info),
            });
            let keys_collection = key_expressions(&e.assignments, &e.source_info)?;
            ValueSpecification::Func(AppliedFunction {
                function: "copy".to_string(),
                f_control: None,
                parameters: vec![source_var, keys_collection],
                source_information: source_information(&e.source_info),
            })
        }

        // -- Column expressions → classInstance --
        Expression::Column(col) => convert_column(col)?,

        // -- Bare element reference: the engine's `instanceReference` --
        Expression::PackageableElementRef(e) => element_pointer(&e.element),

        // -- A unit as a value: the engine's `instanceReference: unitName` --
        Expression::UnitType(e) => v1::value_spec::ValueSpecification::UnitType(
            v1::value_spec::ProtocolPackageableElementPtr {
                full_path: e.unit.to_string(),
                source_information: source_information(&e.source_info),
            },
        ),

        // -- `Class.all()` and its variants: the engine's `allOrFunction`,
        // positioned on the `.all(…)` part --
        Expression::AllInstances(e) => {
            let function = match e.kind {
                ast::expression::AllInstancesKind::All => "getAll",
                ast::expression::AllInstancesKind::AllVersions => "getAllVersions",
                ast::expression::AllInstancesKind::AllVersionsInRange => {
                    return Err(ConvertError::unsupported(
                        "allVersionsInRange() has no Engine grammar form",
                        &e.call_source_info,
                    ));
                }
            };
            ValueSpecification::Func(AppliedFunction {
                function: function.to_string(),
                f_control: None,
                parameters: std::iter::once(Ok(element_pointer(&e.class)))
                    .chain(e.arguments.iter().map(convert_expression_typed))
                    .collect::<Result<_>>()?,
                source_information: source_information(&e.call_source_info),
            })
        }

        // -- Island grammar: converted by the island's registered IslandProtocol --
        Expression::Island(island) => crate::island_protocol::convert_island(island)?,

        // -- Navigation path: `#/Type/p1/p2!alias#` → classInstance("path", { startType, path, name }) --
        Expression::NavigationPath(nav) => convert_navigation_path(nav)?,

        // -- Unit instance: `5 RomanLength~Pes` → newUnit(unit, value) --
        Expression::UnitInstance(e) => {
            let unit_ref =
                ValueSpecification::PackageableElementPtr(ProtocolPackageableElementPtr {
                    full_path: e.unit.to_string(),
                    source_information: source_information(&e.source_info),
                });
            let value = convert_expression_typed(&e.value)?;
            ValueSpecification::Func(AppliedFunction {
                function: "newUnit".to_string(),
                f_control: None,
                parameters: vec![unit_ref, value],
                source_information: source_information(&e.source_info),
            })
        }

        // -- Grouping (transparent) --
        Expression::Group(inner) => convert_expression_typed(inner)?,
    })
}

/// `^Class<Args>(key = value, …)` as the engine's
/// `DomainParseTreeWalker.newFunction` builds it: `new(@Class<Class<Args>>,
/// '', [keyExpressions])`, with no source information anywhere. The engine
/// rewrites three classes to their constructor functions (`Pair`,
/// `TdsOlapRank`, `BasicColumnSpecification`); those are mirrored here.
fn convert_new_instance(
    e: &ast::expression::NewInstanceExpr,
) -> Result<v1::value_spec::ValueSpecification> {
    use v1::value_spec::{
        AppliedFunction, CString, ProtocolCollection, ProtocolGenericTypeInstance,
        ProtocolKeyExpression, ValueSpecification,
    };

    let class_path = e.class.to_string();
    let keys = e
        .assignments
        .iter()
        .map(|a| Ok((a.key.to_string(), convert_expression_typed(&a.value)?)))
        .collect::<Result<Vec<(String, ValueSpecification)>>>()?;
    let constructor = |function: &str, names: &[&str]| {
        // The engine drops absent keys only for `col`; an absent `first` /
        // `second` / `func` would be a null parameter there, which no
        // well-formed source produces.
        let parameters = names
            .iter()
            .filter_map(|name| keys.iter().find(|(k, _)| k == name).map(|(_, v)| v.clone()))
            .collect();
        ValueSpecification::Func(AppliedFunction {
            function: function.to_string(),
            f_control: None,
            parameters,
            source_information: None,
        })
    };
    Ok(match class_path.as_str() {
        "Pair" | "meta::pure::functions::collection::Pair" => constructor(
            "meta::pure::functions::collection::pair",
            &["first", "second"],
        ),
        "TdsOlapRank" | "meta::pure::tds::TdsOlapRank" => {
            constructor("meta::pure::tds::func", &["func"])
        }
        "BasicColumnSpecification" | "meta::pure::tds::BasicColumnSpecification" => {
            constructor("meta::pure::tds::col", &["func", "name", "documentation"])
        }
        _ => {
            let target = v1::generic_type::GenericType {
                raw_type: v1::generic_type::RawType::packageable(class_path, None),
                type_arguments: e.type_arguments.iter().map(Into::into).collect(),
                multiplicity_arguments: vec![],
                type_variable_values: e
                    .type_variable_values
                    .iter()
                    .map(type_variable_value)
                    .collect(),
                source_information: None,
            };
            let class = ValueSpecification::GenericTypeInstance(ProtocolGenericTypeInstance {
                generic_type: v1::generic_type::GenericType {
                    raw_type: v1::generic_type::RawType::packageable(
                        "meta::pure::metamodel::type::Class",
                        None,
                    ),
                    type_arguments: vec![target],
                    multiplicity_arguments: vec![],
                    type_variable_values: vec![],
                    source_information: None,
                },
                source_information: None,
            });
            let values: Vec<ValueSpecification> = keys
                .into_iter()
                .map(|(key, expression)| {
                    ValueSpecification::KeyExpression(ProtocolKeyExpression {
                        add: false,
                        key: Box::new(ValueSpecification::String(CString {
                            multi_line: false,
                            value: key,
                            source_information: None,
                        })),
                        expression: Box::new(expression),
                        source_information: None,
                    })
                })
                .collect();
            ValueSpecification::Func(AppliedFunction {
                function: "new".to_string(),
                f_control: None,
                parameters: vec![
                    class,
                    ValueSpecification::String(CString {
                        multi_line: false,
                        value: String::new(),
                        source_information: None,
                    }),
                    ValueSpecification::Collection(ProtocolCollection {
                        multiplicity: collection_multiplicity(values.len()),
                        values,
                        source_information: None,
                    }),
                ],
                source_information: None,
            })
        }
    })
}

/// Converts `^Class(key=value, …)` / `^$var(…)` assignments into the
/// collection of `keyExpression`s the protocol carries.
fn key_expressions(
    assignments: &[ast::expression::KeyValuePair],
    source_info: &ast::SourceInfo,
) -> Result<v1::value_spec::ValueSpecification> {
    use v1::value_spec::{CString, ProtocolCollection, ProtocolKeyExpression, ValueSpecification};

    let values = assignments
        .iter()
        .map(|a| {
            Ok(ValueSpecification::KeyExpression(ProtocolKeyExpression {
                add: false,
                key: Box::new(ValueSpecification::String(CString {
                    multi_line: false,
                    value: a.key.to_string(),
                    source_information: source_information(&a.source_info),
                })),
                expression: Box::new(convert_expression_typed(&a.value)?),
                source_information: source_information(&a.source_info),
            }))
        })
        .collect::<Result<Vec<_>>>()?;
    Ok(ValueSpecification::Collection(ProtocolCollection {
        multiplicity: collection_multiplicity(values.len()),
        values,
        source_information: source_information(source_info),
    }))
}

/// A reference to a packageable element, positioned on its name.
fn element_pointer(
    element: &ast::annotation::PackageableElementPtr,
) -> v1::value_spec::ValueSpecification {
    v1::value_spec::ValueSpecification::PackageableElementPtr(
        v1::value_spec::ProtocolPackageableElementPtr {
            full_path: element.to_string(),
            source_information: source_information(&element.source_info),
        },
    )
}

/// Converts a literal AST node to a `ValueSpecification`.
fn convert_literal(lit: &ast::expression::Literal) -> Result<v1::value_spec::ValueSpecification> {
    use ast::expression::Literal;
    use v1::value_spec::CBoolean;
    use v1::value_spec::CDateTime;
    use v1::value_spec::CDecimal;
    use v1::value_spec::CFloat;
    use v1::value_spec::CInteger;
    use v1::value_spec::CStrictDate;
    use v1::value_spec::CStrictTime;
    use v1::value_spec::CString;
    use v1::value_spec::ValueSpecification;

    Ok(match lit {
        Literal::Integer(e) => ValueSpecification::Integer(CInteger {
            value: e.value,
            source_information: source_information(&e.source_info),
        }),
        Literal::Float(e) => ValueSpecification::Float(CFloat {
            value: e.value,
            source_information: source_information(&e.source_info),
        }),
        Literal::Decimal(e) => ValueSpecification::Decimal(CDecimal {
            value: decimal_value(&e.value, false, &e.source_info)?,
            source_information: source_information(&e.source_info),
        }),
        Literal::String(e) => ValueSpecification::String(CString {
            multi_line: e.multi_line,
            value: e.value.to_string(),
            source_information: source_information(&e.source_info),
        }),
        Literal::Boolean(e) => ValueSpecification::Boolean(CBoolean {
            value: e.value,
            source_information: source_information(&e.source_info),
        }),
        Literal::StrictDate(e) => ValueSpecification::StrictDate(CStrictDate {
            value: e.value.clone(),
            source_information: source_information(&e.source_info),
        }),
        Literal::DateTime(e) => ValueSpecification::DateTime(CDateTime {
            // The engine's `DateParseTreeWalker` keeps the `%` of a
            // year-month date (`%2014-02`), and only of that one.
            value: if e.value.trim_start_matches('-').split('-').count() == 2 {
                format!("%{}", e.value)
            } else {
                e.value.clone()
            },
            source_information: source_information(&e.source_info),
        }),
        Literal::StrictTime(e) => ValueSpecification::StrictTime(CStrictTime {
            value: e.value.clone(),
            source_information: source_information(&e.source_info),
        }),
        Literal::LatestDate(e) => ValueSpecification::LatestDate(v1::value_spec::CLatestDate {
            source_information: source_information(&e.source_info),
        }),
        Literal::ByteArray(e) => ValueSpecification::ByteArray(v1::value_spec::CByteArray {
            value: base64(e.value.as_bytes()),
            source_information: source_information(&e.source_info),
        }),
    })
}

/// `bytes` in base64, padded, as Jackson writes a `byte[]`.
fn base64(bytes: &[u8]) -> String {
    const ALPHABET: &[u8; 64] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    let mut text = String::with_capacity(bytes.len().div_ceil(3) * 4);
    for chunk in bytes.chunks(3) {
        let group = chunk.iter().enumerate().fold(0_u32, |group, (i, &byte)| {
            group | (u32::from(byte) << (16 - 8 * i))
        });
        for i in 0..4 {
            if i <= chunk.len() {
                text.push(char::from(
                    ALPHABET[((group >> (18 - 6 * i)) & 63) as usize],
                ));
            } else {
                text.push('=');
            }
        }
    }
    text
}

/// Creates a multiplicity with both bounds set to `len` (exact collection size).
#[allow(clippy::cast_possible_truncation)]
pub(super) fn collection_multiplicity(len: usize) -> v1::multiplicity::Multiplicity {
    let bound = len as u32;
    v1::multiplicity::Multiplicity {
        lower_bound: bound,
        upper_bound: Some(bound),
    }
}

/// Converts a `MemberAccess` into a property value specification.
fn convert_member_access(
    ma: &ast::expression::MemberAccess,
) -> Result<v1::value_spec::ValueSpecification> {
    use ast::expression::MemberAccess;
    use v1::value_spec::AppliedProperty;
    use v1::value_spec::ValueSpecification;

    Ok(match ma {
        MemberAccess::Simple(e) => ValueSpecification::Property(AppliedProperty {
            class: None,
            property: e.member.to_string(),
            parameters: vec![convert_expression_typed(&e.target)?],
            // The engine positions a property on its identifier.
            source_information: source_information(&e.member_source_info),
        }),
        MemberAccess::Qualified(e) => {
            let mut parameters =
                convert_all(std::iter::once(e.target.as_ref()).chain(&e.arguments))?;
            // A milestoned property's `(%latest)` / `(%latest, %latest)`:
            // the engine's `functionExpressionLatestMilestoningDateParameter`
            // builds those dates without positions.
            let latest_only = matches!(e.arguments.len(), 1 | 2)
                && e.arguments.iter().all(|a| {
                    matches!(
                        a,
                        ast::expression::Expression::Literal(ast::expression::Literal::LatestDate(
                            _
                        ))
                    )
                });
            if latest_only {
                for date in &mut parameters[1..] {
                    date.set_source_information(None);
                }
            }
            ValueSpecification::Property(AppliedProperty {
                class: None,
                property: e.member.to_string(),
                parameters,
                source_information: source_information(&e.member_source_info),
            })
        }
    })
}

/// Converts `~spec` or `~[specs]` to the engine's `colSpec` or
/// `colSpecArray` class instance, positioned on what follows the `~`.
fn convert_column(
    e: &ast::expression::ColumnBuilderExpr,
) -> Result<v1::value_spec::ValueSpecification> {
    let mut specs = e.columns.iter().map(col_spec).collect::<Result<Vec<_>>>()?;
    let (type_name, value) = if e.is_array {
        (
            "colSpecArray",
            serde_json::to_value(ColSpecArrayJson { col_specs: specs })?,
        )
    } else {
        let spec = specs.pop().ok_or_else(|| {
            ConvertError::unsupported("a column builder without a column", &e.source_info)
        })?;
        ("colSpec", serde_json::to_value(spec)?)
    };
    Ok(v1::value_spec::ValueSpecification::ClassInstance(
        v1::value_spec::ClassInstance {
            type_name: type_name.to_string(),
            value,
            source_information: source_information(&e.source_info),
        },
    ))
}

/// The engine's `ColSpec` (`processOneColSpec`): a name, then a type and
/// multiplicity or one or two lambdas, with stereotypes and tagged values.
fn col_spec(col: &ast::expression::ColumnSpec) -> Result<ColSpecJson> {
    use ast::expression::{ColumnTypeSpec, Expression};

    let lambda = |expression: &Expression| -> Result<v1::value_spec::ValueSpecification> {
        if !matches!(expression, Expression::Lambda(_)) {
            return Err(ConvertError::unsupported(
                "a column function must be a lambda",
                expression.source_info(),
            ));
        }
        convert_expression_typed(expression)
    };
    let mut spec = ColSpecJson {
        name: col.name.to_string(),
        generic_type: None,
        multiplicity: None,
        function1: None,
        function2: None,
        stereotypes: col.stereotypes.iter().map(Into::into).collect(),
        tagged_values: col.tagged_values.iter().map(Into::into).collect(),
        source_information: source_information(&col.source_info),
    };
    match (&col.type_spec, &col.extra_function) {
        (Some(ColumnTypeSpec::Typed(type_ref, multiplicity)), _) => {
            // The engine reads a second function only after a first one.
            spec.generic_type = Some(v1::generic_type::GenericType::from(type_ref));
            spec.multiplicity = multiplicity
                .as_ref()
                .map(v1::multiplicity::Multiplicity::from);
        }
        (Some(ColumnTypeSpec::Lambda(function1)), function2) => {
            spec.function1 = Some(lambda(&Expression::Lambda(function1.clone()))?);
            if let Some(function2) = function2 {
                spec.function2 = Some(lambda(function2)?);
            }
        }
        // The parser keeps a non-lambda after `:` here.
        (None, Some(function)) => {
            return Err(ConvertError::unsupported(
                "a column function must be a lambda",
                function.source_info(),
            ));
        }
        (None, None) => {}
    }
    Ok(spec)
}

/// The engine's `ColSpec`: a name, then a type and a multiplicity, or one
/// or two lambdas, with stereotypes and tagged values.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ColSpecJson {
    name: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    generic_type: Option<v1::generic_type::GenericType>,
    #[serde(skip_serializing_if = "Option::is_none")]
    multiplicity: Option<v1::multiplicity::Multiplicity>,
    #[serde(skip_serializing_if = "Option::is_none")]
    function1: Option<v1::value_spec::ValueSpecification>,
    #[serde(skip_serializing_if = "Option::is_none")]
    function2: Option<v1::value_spec::ValueSpecification>,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    stereotypes: Vec<v1::annotation::StereotypePtr>,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    tagged_values: Vec<v1::annotation::TaggedValue>,
    #[serde(skip_serializing_if = "Option::is_none")]
    source_information: Option<v1::source_info::SourceInformation>,
}

/// The engine's `ColSpecArray`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ColSpecArrayJson {
    col_specs: Vec<ColSpecJson>,
}

/// Converts a `NavigationPath` into a `classInstance("path", ...)`.
///
/// Wire format (mirrors Legend Engine's `NavigationPathComposer`):
/// ```json
/// {
///   "_type": "classInstance",
///   "type": "path",
///   "value": {
///     "startType": "model::Person",
///     "path": [
///       { "_type": "propertyPathElement", "property": "name", "parameters": [...] },
///       ...
///     ],
///     "name": "alias"   // optional
///   }
/// }
/// ```
fn convert_navigation_path(
    nav: &ast::expression::NavigationPath,
) -> Result<v1::value_spec::ValueSpecification> {
    // The engine parses a path with a nested parser whose column offset adds
    // the whole `#/…#` token's length rather than its opening `#`
    // (`visitNavigationPath`): every position inside the path is shifted
    // right by the token's length minus one, and the path itself ends past
    // its text by the length of ANTLR's `<EOF>`.
    let token = &nav.source_info;
    let length = (token.end_column + 1).saturating_sub(token.start_column);
    let shift = |si: &ast::SourceInfo| {
        ast::SourceInfo::new(
            si.source.clone(),
            si.start_line,
            si.start_column + length - 1,
            si.end_line,
            si.end_column + length - 1,
        )
    };
    let path_si = ast::SourceInfo::new(
        token.source.clone(),
        token.start_line,
        token.start_column + length,
        token.start_line,
        token.start_column + 2 * length + 2,
    );

    let path = nav
        .path
        .iter()
        .map(|step| {
            Ok(PropertyPathJson {
                property: step.property.to_string(),
                parameters: step
                    .parameters
                    .iter()
                    .map(|p| navigation_parameter(p, &shift))
                    .collect::<Result<_>>()?,
                source_information: source_information(&shift(&step.source_info)),
            })
        })
        .collect::<Result<_>>()?;
    let value = NavigationPathJson {
        start_type: nav.start_type.full_path(),
        path,
        name: nav.name.as_ref().map(ToString::to_string),
        source_information: source_information(&path_si),
    };
    Ok(v1::value_spec::ValueSpecification::ClassInstance(
        v1::value_spec::ClassInstance {
            type_name: "path".to_string(),
            value: serde_json::to_value(value)?,
            source_information: source_information(&path_si),
        },
    ))
}

/// The engine's `Path`, a navigation path's class instance value.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct NavigationPathJson {
    start_type: String,
    path: Vec<PropertyPathJson>,
    #[serde(skip_serializing_if = "Option::is_none")]
    name: Option<String>,
    source_information: Option<v1::source_info::SourceInformation>,
}

/// The engine's `PropertyPathElement`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "propertyPath", rename_all = "camelCase")]
struct PropertyPathJson {
    property: String,
    parameters: Vec<v1::value_spec::ValueSpecification>,
    source_information: Option<v1::source_info::SourceInformation>,
}

/// A navigation path step's parameter (the engine's `NavigationParseTreeWalker`):
/// a scalar, or a collection of them, which keeps no position.
fn navigation_parameter(
    parameter: &ast::expression::Expression,
    shift: &impl Fn(&ast::SourceInfo) -> ast::SourceInfo,
) -> Result<v1::value_spec::ValueSpecification> {
    Ok(match parameter {
        ast::expression::Expression::Collection(collection) => {
            v1::value_spec::ValueSpecification::Collection(v1::value_spec::ProtocolCollection {
                multiplicity: collection_multiplicity(collection.elements.len()),
                values: collection
                    .elements
                    .iter()
                    .map(|e| navigation_scalar(e, shift))
                    .collect::<Result<_>>()?,
                source_information: None,
            })
        }
        scalar => navigation_scalar(scalar, shift)?,
    })
}

/// A literal (dates as date-times without their `%`), `%latest`, or an enum
/// value, which keeps no position.
fn navigation_scalar(
    scalar: &ast::expression::Expression,
    shift: &impl Fn(&ast::SourceInfo) -> ast::SourceInfo,
) -> Result<v1::value_spec::ValueSpecification> {
    use ast::expression::{Expression, Literal, MemberAccess};
    use v1::value_spec::{CDateTime, ValueSpecification};

    Ok(match scalar {
        Expression::Literal(
            Literal::StrictDate(ast::expression::StrictDateLiteral { value, source_info })
            | Literal::DateTime(ast::expression::DateTimeLiteral { value, source_info })
            | Literal::StrictTime(ast::expression::StrictTimeLiteral { value, source_info }),
        ) => ValueSpecification::DateTime(CDateTime {
            value: value.clone(),
            source_information: source_information(&shift(source_info)),
        }),
        Expression::Literal(literal) => {
            let mut value = convert_literal(literal)?;
            value.set_source_information(source_information(&shift(literal.source_info())));
            value
        }
        Expression::MemberAccess(MemberAccess::Simple(access)) => {
            let Expression::PackageableElementRef(enumeration) = access.target.as_ref() else {
                return Err(ConvertError::unsupported(
                    "a navigation path parameter must be a literal or an enum value",
                    scalar.source_info(),
                ));
            };
            ValueSpecification::EnumValue(v1::value_spec::ProtocolEnumValue {
                full_path: enumeration.element.to_string(),
                value: access.member.to_string(),
                source_information: None,
            })
        }
        other => {
            return Err(ConvertError::unsupported(
                "a navigation path parameter must be a literal or an enum value",
                other.source_info(),
            ));
        }
    })
}

/// Converts an AST `Element` into a protocol `PackageableElement`.
///
/// # Errors
///
/// [`ConvertError::Unsupported`] for elements with no Engine protocol form
/// (native functions, primitive type definitions) and for DSL elements whose
/// kind has no registered [`ElementProtocol`](crate::ElementProtocol);
/// otherwise any nested conversion failure.
pub fn convert_element(elem: &ast::element::Element) -> Result<v1::element::PackageableElement> {
    use ast::element::Element;
    use v1::element::PackageableElement;

    match elem {
        Element::Class(c) => Ok(PackageableElement::Class(convert_class(c)?)),
        Element::Enumeration(e) => Ok(PackageableElement::Enumeration(convert_enumeration(e))),
        Element::Function(f) => Ok(PackageableElement::Function(convert_function(f)?)),
        Element::NativeFunction(n) => Err(ConvertError::unsupported(
            "native function definitions have no Engine protocol form",
            n.source_info(),
        )),
        Element::Profile(p) => Ok(PackageableElement::Profile(convert_profile(p))),
        Element::Association(a) => Ok(PackageableElement::Association(convert_association(a)?)),
        Element::Measure(m) => Ok(PackageableElement::Measure(convert_measure(m)?)),
        Element::Primitive(p) => Err(ConvertError::unsupported(
            "primitive type definitions have no Engine protocol form",
            p.source_info(),
        )),
        // Section-grammar elements are converted by their owning crate's
        // registered `ElementProtocol`, which writes JSON text: read back here,
        // for this value of the element (`convert_element_json` keeps the text).
        Element::DSLElement(e) => Ok(PackageableElement::Extension(serde_json::from_str(
            crate::element_protocol::convert_dsl_element(e.as_ref())?.get(),
        )?)),
    }
}

/// An element's protocol JSON, written once as text, and its path as the
/// engine's `getPath()` makes it, for a model's section index.
#[derive(Debug)]
pub struct ElementJson {
    /// The element's path: its name, behind its package if it has one; a
    /// function's name includes its signature.
    pub path: String,
    /// The element's protocol JSON object.
    pub json: Box<serde_json::value::RawValue>,
}

/// [`convert_element`] as JSON text, without building a JSON value first: a
/// section grammar's element as its converter writes it, a core element
/// serialized.
///
/// # Errors
///
/// As for [`convert_element`].
pub fn convert_element_json(elem: &ast::element::Element) -> Result<ElementJson> {
    if let ast::element::Element::DSLElement(element) = elem {
        return Ok(ElementJson {
            path: dsl_path(element.as_ref()),
            json: crate::element_protocol::convert_dsl_element(element.as_ref())?,
        });
    }
    let protocol = convert_element(elem)?;
    Ok(ElementJson {
        path: protocol.path(),
        json: serde_json::value::to_raw_value(&protocol)?,
    })
}

/// [`convert_element_json`] written at the end of `out`, the model's JSON,
/// rather than into a buffer of its own; returns the element's path.
///
/// # Errors
///
/// As for [`convert_element`].
pub fn convert_element_into(elem: &ast::element::Element, out: &mut Vec<u8>) -> Result<String> {
    if let ast::element::Element::DSLElement(element) = elem {
        crate::element_protocol::convert_dsl_element_into(element.as_ref(), out)?;
        return Ok(dsl_path(element.as_ref()));
    }
    let protocol = convert_element(elem)?;
    serde_json::to_writer(&mut *out, &protocol)?;
    Ok(protocol.path())
}

/// A section grammar's element's path: its name, behind its package if it
/// has one.
fn dsl_path(element: &dyn ast::dsl::DSLElement) -> String {
    let name = element.name();
    match element.package() {
        Some(package) => format!("{package}::{name}"),
        None => name.to_string(),
    }
}

fn convert_class(c: &ast::element::ClassDef) -> Result<v1::element::ProtocolClass> {
    let properties: std::result::Result<Vec<_>, _> =
        c.properties.iter().map(convert_property).collect();
    let qualified_properties: std::result::Result<Vec<_>, _> = c
        .qualified_properties
        .iter()
        .map(convert_qualified_property)
        .collect();
    let constraints: std::result::Result<Vec<_>, _> = c
        .constraints
        .iter()
        .enumerate()
        .map(|(i, constraint)| convert_constraint(i, constraint))
        .collect();
    Ok(v1::element::ProtocolClass {
        package_path: optional_package_to_path(c.package.as_ref()),
        name: c.name.value.to_string(),
        super_types: c
            .super_types
            .iter()
            .map(|t| {
                v1::element::PackageableElementPointer::class(
                    t.full_path(),
                    source_information(&t.source_info),
                )
            })
            .collect(),
        properties: properties?,
        qualified_properties: qualified_properties?,
        constraints: constraints?,
        original_milestoned_properties: vec![],
        stereotypes: c.stereotypes.iter().map(Into::into).collect(),
        tagged_values: c.tagged_values.iter().map(Into::into).collect(),
        source_information: source_information(&c.source_info),
    })
}

fn convert_enumeration(e: &ast::element::EnumDef) -> v1::element::ProtocolEnumeration {
    v1::element::ProtocolEnumeration {
        package_path: optional_package_to_path(e.package.as_ref()),
        name: e.name.value.to_string(),
        values: e.values.iter().map(convert_enum_value).collect(),
        stereotypes: e.stereotypes.iter().map(Into::into).collect(),
        tagged_values: e.tagged_values.iter().map(Into::into).collect(),
        source_information: source_information(&e.source_info),
    }
}

fn convert_enum_value(v: &ast::element::EnumValue) -> v1::element::ProtocolEnumMember {
    v1::element::ProtocolEnumMember {
        value: v.name.to_string(),
        stereotypes: v.stereotypes.iter().map(Into::into).collect(),
        tagged_values: v.tagged_values.iter().map(Into::into).collect(),
        source_information: source_information(&v.source_info),
    }
}

/// A function test suite, as the engine's `visitFunction` builds it: the
/// test block's own tests and data as suite `default`, then each named
/// suite.
fn function_test_suite(
    f: &ast::element::FunctionDef,
    suite: &ast::element::FunctionTest,
) -> Result<v1::function_test::FunctionTestSuite> {
    Ok(v1::function_test::FunctionTestSuite {
        id: suite.name.as_ref().map_or_else(
            || v1::function_test::DEFAULT_TESTABLE_ID.to_string(),
            ToString::to_string,
        ),
        tests: suite
            .assertions
            .iter()
            .map(|test| function_test(f, test))
            .collect::<Result<_>>()?,
        test_data: suite
            .data
            .iter()
            .map(function_test_data)
            .collect::<Result<_>>()?,
        source_information: source_information(&suite.source_info),
    })
}

/// One test (the engine's `processFuncTest`): the call's arguments, named
/// after the function's parameters, and one assertion.
fn function_test(
    f: &ast::element::FunctionDef,
    test: &ast::element::FunctionTestAssertion,
) -> Result<v1::function_test::FunctionTest> {
    use ast::element::FunctionTestExpected;
    use v1::function_test::{DEFAULT_TESTABLE_ID, ParameterValue};
    use v1::test_assertion::TestAssertion;

    let ast::expression::Expression::FunctionApplication(call) = &test.invocation else {
        return Err(ConvertError::unsupported(
            "a function test must call its function",
            test.invocation.source_info(),
        ));
    };
    let parameters = call
        .arguments
        .iter()
        .enumerate()
        .map(|(index, argument)| {
            Ok(ParameterValue {
                name: f.parameters.get(index).map(|p| p.name.to_string()),
                value: primitive_value(argument)?,
                source_information: source_information(&primitive_value_span(argument)),
            })
        })
        .collect::<Result<_>>()?;
    let assertion = match &test.expected {
        FunctionTestExpected::Value(value) => TestAssertion::EqualTo {
            id: DEFAULT_TESTABLE_ID.to_string(),
            expected: primitive_value(value)?,
            source_information: source_information(&primitive_value_span(value)),
        },
        FunctionTestExpected::ExternalFormat(data) => TestAssertion::EqualToJson {
            id: DEFAULT_TESTABLE_ID.to_string(),
            expected: external_format_data(data),
            source_information: source_information(&data.source_info),
        },
        FunctionTestExpected::Relation(relation) => TestAssertion::EqualToRelation {
            id: DEFAULT_TESTABLE_ID.to_string(),
            expected: relation_element(&relation.element),
            source_information: source_information(&relation.source_info),
        },
        FunctionTestExpected::Embedded(data) => return Err(embedded_data_unsupported(data)),
    };
    Ok(v1::function_test::FunctionTest {
        id: test.name.to_string(),
        doc: test.doc.as_ref().map(ToString::to_string),
        parameters,
        assertions: vec![assertion],
        source_information: source_information(&test.source_info),
    })
}

/// A mapping or service test's assertion, `Type #{ … }#`, with the id its
/// test names it by (the engine's `HelperTestAssertionGrammarParser` and
/// the walkers' `EqualToRelation`).
///
/// # Errors
///
/// [`ConvertError::Unsupported`] for an expected value that is not a
/// primitive value, which the engine's `EqualTo` grammar rejects, or a
/// nested conversion failure.
pub fn convert_test_assertion(
    id: &str,
    assertion: &ast::test_assertion::TestAssertion,
) -> Result<v1::test_assertion::TestAssertion> {
    use ast::test_assertion::TestAssertion as Assertion;
    use v1::test_assertion::TestAssertion;

    let id = id.to_string();
    Ok(match assertion {
        // The engine parses the value with a walker of its own, positioned
        // on the value.
        Assertion::EqualTo {
            expected,
            source_info,
        } => TestAssertion::EqualTo {
            id,
            expected: primitive_value(expected)?,
            source_information: source_information(source_info),
        },
        Assertion::EqualToJson {
            expected,
            source_info,
        } => {
            let v1::data::EmbeddedData::ExternalFormat(expected) = convert_embedded_data(expected)?
            else {
                return Err(ConvertError::unsupported(
                    "an EqualToJson assertion expects ExternalFormat data",
                    &expected.source_info,
                ));
            };
            TestAssertion::EqualToJson {
                id,
                expected,
                source_information: source_information(source_info),
            }
        }
        Assertion::EqualToRelation {
            expected,
            source_info,
        } => TestAssertion::EqualToRelation {
            id,
            expected: relation_element(expected),
            source_information: source_information(source_info),
        },
    })
}

/// A value as the engine's `DomainParser.parsePrimitiveValue` reads it, for
/// a service test's parameters: a literal (a signed number included), an
/// enum value, or a collection of them, positioned.
///
/// # Errors
///
/// [`ConvertError::Unsupported`] for any other expression, which the
/// engine's `primitiveValue` rule rejects.
pub fn convert_primitive_value(
    expression: &ast::expression::Expression,
) -> Result<v1::value_spec::ValueSpecification> {
    primitive_value(expression)
}

/// The engine's `primitiveValue`: a literal (a signed number included), an
/// enum value, or a collection of them.
fn primitive_value(
    expression: &ast::expression::Expression,
) -> Result<v1::value_spec::ValueSpecification> {
    use ast::expression::{Expression, Literal, MemberAccess};
    use v1::value_spec::{CDecimal, CFloat, CInteger, ValueSpecification};

    let span = primitive_value_span(expression);
    Ok(match expression {
        Expression::Literal(literal) => convert_literal(literal)?,
        Expression::UnaryMinus(minus) => match minus.operand.as_ref() {
            Expression::Literal(Literal::Integer(i)) => ValueSpecification::Integer(CInteger {
                value: -i.value,
                source_information: source_information(&span),
            }),
            Expression::Literal(Literal::Float(f)) => ValueSpecification::Float(CFloat {
                value: -f.value,
                source_information: source_information(&span),
            }),
            Expression::Literal(Literal::Decimal(d)) => ValueSpecification::Decimal(CDecimal {
                value: decimal_value(&d.value, true, &d.source_info)?,
                source_information: source_information(&span),
            }),
            _ => return Err(not_a_primitive_value(expression)),
        },
        Expression::Collection(collection) => {
            ValueSpecification::Collection(v1::value_spec::ProtocolCollection {
                multiplicity: collection_multiplicity(collection.elements.len()),
                values: collection
                    .elements
                    .iter()
                    .map(primitive_value)
                    .collect::<Result<_>>()?,
                source_information: source_information(&span),
            })
        }
        Expression::MemberAccess(MemberAccess::Simple(access)) => {
            let Expression::PackageableElementRef(enumeration) = access.target.as_ref() else {
                return Err(not_a_primitive_value(expression));
            };
            ValueSpecification::EnumValue(v1::value_spec::ProtocolEnumValue {
                full_path: enumeration.element.to_string(),
                value: access.member.to_string(),
                source_information: source_information(&span),
            })
        }
        _ => return Err(not_a_primitive_value(expression)),
    })
}

/// Where a primitive value is: a signed number from its sign, an enum value
/// from its enumeration to its value.
fn primitive_value_span(expression: &ast::expression::Expression) -> ast::SourceInfo {
    use ast::expression::{Expression, MemberAccess};
    match expression {
        Expression::UnaryMinus(minus) => minus.source_info.merge(&minus.operand_span),
        Expression::MemberAccess(MemberAccess::Simple(access)) => access
            .target
            .source_info()
            .merge(&access.member_source_info),
        other => other.source_info().clone(),
    }
}

fn not_a_primitive_value(expression: &ast::expression::Expression) -> ConvertError {
    ConvertError::unsupported(
        "a test value must be a literal, an enum value or a collection of them",
        expression.source_info(),
    )
}

/// A store's test data (the engine's `processStoreTestData`).
fn function_test_data(
    data: &ast::element::FunctionTestData,
) -> Result<v1::function_test::FunctionTestData> {
    use ast::element::FunctionTestDataValue;
    use v1::data::EmbeddedData;
    use v1::element::PackageableElementPointer;

    let value = match &data.data {
        FunctionTestDataValue::ExternalFormat(external) => {
            EmbeddedData::ExternalFormat(external_format_data(external))
        }
        FunctionTestDataValue::Reference(element) => EmbeddedData::Reference {
            data_element: PackageableElementPointer {
                element_type: Some("DATA".to_string()),
                path: element.to_string(),
                source_information: source_information(&element.source_info),
            },
            source_information: source_information(&element.source_info),
        },
        FunctionTestDataValue::Embedded(embedded) => convert_embedded_data(embedded)?,
    };
    Ok(v1::function_test::FunctionTestData {
        packageable_element_pointer: PackageableElementPointer {
            element_type: None,
            path: data.store.to_string(),
            source_information: source_information(&data.store.source_info),
        },
        data: value,
        source_information: source_information(&data.source_info),
    })
}

/// Embedded data, `Type #{ … }#`: an engine core type, or a plug-in
/// grammar's data converted by its linked protocol half.
///
/// # Errors
///
/// [`ConvertError::Unsupported`] for data no linked grammar converts, or a
/// nested conversion failure.
pub fn convert_embedded_data(
    embedded: &ast::data::EmbeddedDataValue,
) -> Result<v1::data::EmbeddedData> {
    use ast::data::EmbeddedData as Data;
    use v1::data::EmbeddedData;

    Ok(match &embedded.data {
        Data::ExternalFormat(data) => EmbeddedData::ExternalFormat(v1::data::ExternalFormatData {
            content_type: data.content_type.clone(),
            data: data.data.clone(),
            source_information: source_information(&embedded.source_info),
        }),
        Data::ModelStore(data) => EmbeddedData::ModelStore(v1::data::ModelStoreData {
            model_data: data
                .model_data
                .iter()
                .map(model_test_data)
                .collect::<Result<_>>()?,
            source_information: source_information(&embedded.source_info),
        }),
        // The engine positions both on the whole embedded data.
        Data::Reference(reference) => EmbeddedData::Reference {
            data_element: v1::element::PackageableElementPointer {
                element_type: Some(reference.element_type.to_string()),
                path: reference.data_element.clone(),
                source_information: source_information(&embedded.source_info),
            },
            source_information: source_information(&embedded.source_info),
        },
        Data::Relation(relation) => EmbeddedData::Relation(v1::data::RelationElementsData {
            relation_elements: relation.elements.iter().map(relation_element).collect(),
            source_information: source_information(&relation.source_info),
        }),
        Data::Extension(content) => {
            EmbeddedData::Extension(embedded_data_extension(content.as_ref(), embedded)?)
        }
        Data::Text(_) => return Err(embedded_data_unsupported(embedded)),
    })
}

/// One class's model store data.
fn model_test_data(entry: &ast::data::ModelTestData) -> Result<v1::data::ModelTestData> {
    use ast::data::ModelTestDataValue;

    let model = entry.model.to_string();
    let source_information = source_information(&entry.source_info);
    Ok(match &entry.data {
        ModelTestDataValue::Instances(instances) => v1::data::ModelTestData::Instances {
            model,
            instances: unpositioned_collection(
                instances
                    .iter()
                    .map(model_instance)
                    .collect::<Result<_>>()?,
            ),
            source_information,
        },
        ModelTestDataValue::Embedded(data) => v1::data::ModelTestData::Embedded {
            model,
            data: convert_embedded_data(data)?,
            source_information,
        },
    })
}

/// `^Type(p = v, …)` as the engine's `ModelStoreDataParseTreeWalker` builds
/// it, `new(Type, 'dummy', [p = [v, …], …])`, positioned only on its values.
fn model_instance(
    instance: &ast::data::ModelInstance,
) -> Result<v1::value_spec::ValueSpecification> {
    use v1::value_spec::{
        AppliedFunction, CString, ProtocolKeyExpression, ProtocolPackageableElementPtr,
        ValueSpecification,
    };

    let string = |value: &str| {
        ValueSpecification::String(CString {
            value: value.to_string(),
            multi_line: false,
            source_information: None,
        })
    };
    let assignments = instance
        .properties
        .iter()
        .map(|property| {
            Ok(ValueSpecification::KeyExpression(ProtocolKeyExpression {
                add: false,
                key: Box::new(string(&property.property)),
                expression: Box::new(unpositioned_collection(
                    property
                        .values
                        .iter()
                        .map(model_value)
                        .collect::<Result<_>>()?,
                )),
                source_information: None,
            }))
        })
        .collect::<Result<_>>()?;
    Ok(ValueSpecification::Func(AppliedFunction {
        function: "new".to_string(),
        f_control: None,
        parameters: vec![
            ValueSpecification::PackageableElementPtr(ProtocolPackageableElementPtr {
                full_path: instance.class.to_string(),
                source_information: None,
            }),
            string("dummy"),
            unpositioned_collection(assignments),
        ],
        source_information: None,
    }))
}

/// A value in model store data.
fn model_value(value: &ast::data::ModelValue) -> Result<v1::value_spec::ValueSpecification> {
    use ast::data::ModelValue;
    use v1::value_spec::{ProtocolEnumValue, ValueSpecification};

    Ok(match value {
        ModelValue::Literal(literal) => convert_literal(literal)?,
        ModelValue::EnumValue {
            enumeration,
            value,
            source_info,
        } => ValueSpecification::EnumValue(ProtocolEnumValue {
            full_path: enumeration.to_string(),
            value: value.to_string(),
            source_information: source_information(source_info),
        }),
        ModelValue::Instance(instance) => model_instance(instance)?,
    })
}

/// The engine's `new Collection(values)`: sized by its values, unpositioned.
fn unpositioned_collection(
    values: Vec<v1::value_spec::ValueSpecification>,
) -> v1::value_spec::ValueSpecification {
    v1::value_spec::ValueSpecification::Collection(v1::value_spec::ProtocolCollection {
        multiplicity: collection_multiplicity(values.len()),
        values,
        source_information: None,
    })
}

/// The engine's `visitExternalFormat`: `JSON` and `XML` stand for their
/// media types.
fn external_format_data(value: &ast::element::ExternalFormatValue) -> v1::data::ExternalFormatData {
    let content_type = match value.format.as_str() {
        "JSON" => "application/json",
        "XML" => "application/xml",
        other => other,
    };
    v1::data::ExternalFormatData {
        content_type: content_type.to_string(),
        data: value.content.clone(),
        source_information: source_information(&value.source_info),
    }
}

/// A plug-in grammar's data, converted by its linked protocol half.
fn embedded_data_extension(
    content: &dyn ast::data::EmbeddedDataContent,
    embedded: &ast::element::EmbeddedDataValue,
) -> Result<serde_json::Value> {
    crate::embedded_data_protocol::discovered_embedded_data_protocols()
        .iter()
        .find(|p| p.type_name() == content.type_name())
        .ok_or_else(|| embedded_data_unsupported(embedded))?
        .convert(content)
}

/// A relation, `Relation #{ … }#`'s first table, as the engine keeps one
/// (a test's expected relation, a data space executable's sample values).
#[must_use]
pub fn convert_relation_element(element: &ast::data::RelationElement) -> v1::data::RelationElement {
    relation_element(element)
}

/// One table of relation data.
fn relation_element(element: &ast::data::RelationElement) -> v1::data::RelationElement {
    let strings = |cells: &[smol_str::SmolStr]| cells.iter().map(ToString::to_string).collect();
    v1::data::RelationElement {
        paths: strings(&element.paths),
        columns: strings(&element.columns),
        rows: element
            .rows
            .iter()
            .map(|row| v1::data::RelationRowTestData {
                values: strings(row),
            })
            .collect(),
        source_information: source_information(&element.source_info),
    }
}

/// Embedded data no grammar read (a test's expected value of a type other
/// than `Relation`): the engine rejects it, or parses the section itself.
fn embedded_data_unsupported(data: &ast::element::EmbeddedDataValue) -> ConvertError {
    ConvertError::unsupported(
        format!("embedded '{}' data has no Rust grammar", data.type_name),
        &data.source_info,
    )
}

fn convert_function(f: &ast::element::FunctionDef) -> Result<v1::element::ProtocolFunction> {
    let mut function = v1::element::ProtocolFunction {
        package_path: optional_package_to_path(f.package.as_ref()),
        name: String::new(),
        parameters: f
            .parameters
            .iter()
            .map(|p| v1::value_spec::Variable {
                name: p.name.to_string(),
                generic_type: parameter_type(p.type_ref.as_ref()),
                multiplicity: p.multiplicity.as_ref().map(Into::into),
                supports_stream: None,
                source_information: source_information(&p.source_info),
            })
            .collect(),
        return_generic_type: (&f.return_type).into(),
        return_multiplicity: (&f.return_multiplicity).into(),
        body: convert_all(&f.body)?,
        stereotypes: f.stereotypes.iter().map(Into::into).collect(),
        tagged_values: f.tagged_values.iter().map(Into::into).collect(),
        tests: f
            .tests
            .iter()
            .map(|suite| function_test_suite(f, suite))
            .collect::<Result<_>>()?,
        pre_constraints: vec![],
        post_constraints: vec![],
        source_information: source_information(&f.source_info),
    };
    function.name = format!(
        "{}{}",
        f.name.value,
        engine_function_signature(&function, &f.source_info)?
    );
    Ok(function)
}

/// The signature suffix the engine appends to a function's name
/// (`HelperValueSpecificationGrammarComposer.getFunctionSignature`): each
/// typed parameter as `<Type>_<multiplicity>`, joined by `__`, then
/// `__<ReturnType>_<multiplicity>_`, with a leading `_` when there are
/// parameters. Types are their last path segment. `new(): Any[1]` is
/// `new__Any_1_`; `f(a: String[1], b: x::T[*]): Boolean[0..1]` is
/// `f_String_1__T_MANY__Boolean_$0_1$_`.
fn engine_function_signature(
    function: &v1::element::ProtocolFunction,
    source_info: &ast::SourceInfo,
) -> Result<String> {
    // The engine casts every raw type here to a named type.
    let named = |gt: &v1::generic_type::GenericType| {
        gt.raw_type.full_path().map(str::to_owned).ok_or_else(|| {
            ConvertError::unsupported(
                "functions with relation-typed parameters or results",
                source_info,
            )
        })
    };
    let mut parameters = Vec::new();
    for p in &function.parameters {
        // Untyped parameters are left out, as in the engine.
        if let (Some(gt), Some(m)) = (&p.generic_type, &p.multiplicity) {
            parameters.push(format!(
                "{}_{}",
                class_signature(&named(gt)?),
                multiplicity_signature(m)
            ));
        }
    }
    let signature = format!(
        "{}__{}_{}_",
        parameters.join("__"),
        class_signature(&named(&function.return_generic_type)?),
        multiplicity_signature(&function.return_multiplicity)
    );
    Ok(if function.parameters.is_empty() {
        signature
    } else {
        format!("_{signature}")
    })
}

/// A protocol function's name without the engine's signature suffix
/// (`HelperValueSpecificationGrammarComposer.getFunctionNameWithNoPackage`).
pub(crate) fn function_name_without_signature(function: &v1::element::ProtocolFunction) -> &str {
    engine_function_signature(function, &ast::SourceInfo::new("", 0, 0, 0, 0))
        .ok()
        .and_then(|signature| {
            function
                .name
                .find(&signature)
                .filter(|&i| i > 0)
                .map(|i| &function.name[..i])
        })
        .unwrap_or(&function.name)
}

/// A type's last path segment (`meta::pure::Integer` → `Integer`).
fn class_signature(path: &str) -> &str {
    path.rsplit("::").next().unwrap_or(path)
}

/// `1`, `MANY` (`*`), or `$<lower>_<upper|MANY>$`.
fn multiplicity_signature(m: &v1::multiplicity::Multiplicity) -> String {
    match (m.lower_bound, m.upper_bound) {
        (lower, Some(upper)) if lower == upper => lower.to_string(),
        (0, None) => "MANY".to_string(),
        (lower, upper) => format!(
            "${lower}_{}$",
            upper.map_or_else(|| "MANY".to_string(), |u| u.to_string())
        ),
    }
}

fn convert_profile(p: &ast::element::ProfileDef) -> v1::element::ProtocolProfile {
    v1::element::ProtocolProfile {
        package_path: optional_package_to_path(p.package.as_ref()),
        name: p.name.value.to_string(),
        stereotypes: p.stereotype_names.iter().map(profile_entry).collect(),
        tags: p.tag_names.iter().map(profile_entry).collect(),
        source_information: source_information(&p.source_info),
    }
}

/// A stereotype or tag a profile declares, positioned on its identifier.
fn profile_entry(name: &ast::annotation::SpannedString) -> v1::element::ProfileEntry {
    v1::element::ProfileEntry {
        value: name.value.to_string(),
        source_information: source_information(&name.source_info),
    }
}

fn convert_association(
    a: &ast::element::AssociationDef,
) -> Result<v1::element::ProtocolAssociation> {
    let properties: std::result::Result<Vec<_>, _> =
        a.properties.iter().map(convert_property).collect();
    let qualified_properties: std::result::Result<Vec<_>, _> = a
        .qualified_properties
        .iter()
        .map(convert_qualified_property)
        .collect();
    Ok(v1::element::ProtocolAssociation {
        package_path: optional_package_to_path(a.package.as_ref()),
        name: a.name.value.to_string(),
        properties: properties?,
        qualified_properties: qualified_properties?,
        original_milestoned_properties: vec![],
        stereotypes: a.stereotypes.iter().map(Into::into).collect(),
        tagged_values: a.tagged_values.iter().map(Into::into).collect(),
        source_information: source_information(&a.source_info),
    })
}

fn convert_measure(m: &ast::element::MeasureDef) -> Result<v1::element::ProtocolMeasure> {
    let mut units = m.non_canonical_units.iter();
    // The engine's `visitMeasure`: a non-convertible measure (no `*` unit)
    // makes its first unit canonical, and has non-canonical units only when
    // it has more than one.
    let canonical_unit = m.canonical_unit.as_ref().or_else(|| units.next());
    let non_canonical_units: Vec<_> = units.map(|u| convert_unit(m, u)).collect::<Result<_>>()?;
    let non_convertible_single = m.canonical_unit.is_none() && non_canonical_units.is_empty();
    Ok(v1::element::ProtocolMeasure {
        package_path: optional_package_to_path(m.package.as_ref()),
        name: m.name.value.to_string(),
        canonical_unit: canonical_unit.map(|u| convert_unit(m, u)).transpose()?,
        non_canonical_units: (!non_convertible_single).then_some(non_canonical_units),
        source_information: source_information(&m.source_info),
    })
}

fn convert_unit(
    measure: &ast::element::MeasureDef,
    unit: &ast::element::UnitDef,
) -> Result<v1::element::ProtocolUnit> {
    let measure_path = match &measure.package {
        Some(pkg) => format!("{pkg}::{}", measure.name.value),
        None => measure.name.value.to_string(),
    };
    // The engine's `visitUnit`: a lambda over the (untyped) parameter.
    let conversion_function = unit
        .conversion_body
        .as_ref()
        .map(|body| -> Result<v1::value_spec::ValueSpecification> {
            Ok(v1::value_spec::ValueSpecification::Lambda(
                v1::value_spec::LambdaFunction {
                    body: vec![convert_expression_typed(body)?],
                    parameters: unit
                        .conversion_param
                        .as_ref()
                        .map(|p| {
                            vec![v1::value_spec::Variable {
                                name: p.to_string(),
                                generic_type: None,
                                multiplicity: None,
                                supports_stream: None,
                                source_information: None,
                            }]
                        })
                        .unwrap_or_default(),
                    source_information: None,
                },
            ))
        })
        .transpose()?;
    Ok(v1::element::ProtocolUnit {
        name: unit.name.to_string(),
        measure: measure_path,
        conversion_function,
        source_information: source_information(&unit.source_info),
    })
}

/// Convert an AST `MultiplicityArgument` (the `@[m]` / `@[1..*]` /
/// `@[*]` form in expression position) to a protocol
/// `ValueSpecification`. Settled wire form:
///
/// - Well-known multiplicities (`@[1]`, `@[0..1]`, `@[*]`, `@[1..*]`)
///   → `packageableElementPtr` pointing at
///   `meta::pure::metamodel::multiplicity::{PureOne, ZeroOne,
///   ZeroMany, OneMany}`.
/// - Arbitrary ranges (`@[2..5]`) → `classInstance("multiplicity",
///   { lowerBound, upperBound? })`.
/// - Parameter variables (`@[m]`) → `classInstance("multiplicity",
///   { multiplicityParameter })`.
///
/// Mirrors what Java's `MultiplicityInstance.createPersistent` builds
/// at the metamodel level — the engine then serialises that instance
/// to the same JSON shape.
#[must_use]
pub fn multiplicity_arg_to_value_spec(
    arg: &ast::type_ref::MultiplicityArgument,
    source_information: Option<v1::source_info::SourceInformation>,
) -> v1::value_spec::ValueSpecification {
    use ast::type_ref::MultiplicityArgument;
    match arg {
        MultiplicityArgument::Identifier(name, _) => {
            multiplicity_variable_value_spec(name.as_str(), source_information)
        }
        MultiplicityArgument::Concrete(m, _) => multiplicity_to_value_spec(m, source_information),
    }
}

/// Convert an AST `Multiplicity` (concrete bounds, no variable case)
/// to a protocol `ValueSpecification` using the same wire form as
/// [`multiplicity_arg_to_value_spec`].
#[must_use]
pub fn multiplicity_to_value_spec(
    m: &ast::type_ref::Multiplicity,
    source_information: Option<v1::source_info::SourceInformation>,
) -> v1::value_spec::ValueSpecification {
    use ast::type_ref::Multiplicity as AM;
    use v1::multiplicity::Multiplicity as PM;
    let pm = match m {
        AM::PureOne => PM::PURE_ONE,
        AM::ZeroOrOne => PM::ZERO_ONE,
        AM::ZeroOrMany => PM::ZERO_MANY,
        AM::OneOrMany => PM::ONE_MANY,
        AM::Range {
            lower,
            upper: Some(u),
        } => PM {
            lower_bound: *lower,
            upper_bound: Some(*u),
        },
        AM::Range { lower, upper: None } => PM {
            lower_bound: *lower,
            upper_bound: None,
        },
        AM::Variable(name) => return multiplicity_variable_value_spec(name, source_information),
    };
    protocol_multiplicity_to_value_spec(&pm, source_information)
}

/// Convert a protocol `Multiplicity` directly to its `ValueSpecification`
/// representation. Used by callers that already have the resolved
/// bounds in protocol form (e.g. the Pure→Protocol path in the CLI
/// crate, which renders lowered `Multiplicity` through
/// `crate::commands::parse_compiled::render_multiplicity`).
#[must_use]
pub fn protocol_multiplicity_to_value_spec(
    m: &v1::multiplicity::Multiplicity,
    source_information: Option<v1::source_info::SourceInformation>,
) -> v1::value_spec::ValueSpecification {
    use v1::value_spec::{ClassInstance, ProtocolPackageableElementPtr, ValueSpecification};
    if let Some(path) = m.well_known_path() {
        return ValueSpecification::PackageableElementPtr(ProtocolPackageableElementPtr {
            full_path: path.to_string(),
            source_information,
        });
    }
    let mut value_map = serde_json::Map::new();
    value_map.insert(
        "lowerBound".to_string(),
        serde_json::Value::Number(m.lower_bound.into()),
    );
    if let Some(u) = m.upper_bound {
        value_map.insert(
            "upperBound".to_string(),
            serde_json::Value::Number(u.into()),
        );
    }
    ValueSpecification::ClassInstance(ClassInstance {
        type_name: "multiplicity".to_string(),
        value: serde_json::Value::Object(value_map),
        source_information,
    })
}

fn multiplicity_variable_value_spec(
    name: &str,
    source_information: Option<v1::source_info::SourceInformation>,
) -> v1::value_spec::ValueSpecification {
    use v1::value_spec::{ClassInstance, ValueSpecification};
    let mut value_map = serde_json::Map::new();
    value_map.insert(
        "multiplicityParameter".to_string(),
        serde_json::Value::String(name.to_string()),
    );
    ValueSpecification::ClassInstance(ClassInstance {
        type_name: "multiplicity".to_string(),
        value: serde_json::Value::Object(value_map),
        source_information,
    })
}

/// Converts a parsed `SourceFile` into a `PureModelContextData`.
///
/// This is the top-level entry point for AST → Protocol conversion.
///
/// # Errors
///
/// Returns the first element conversion failure (see [`convert_element`]).
pub fn convert_source_file(
    source_file: &ast::section::SourceFile,
) -> Result<v1::context::PureModelContextData> {
    let mut elements: Vec<v1::element::PackageableElement> = Vec::new();
    let mut sections: Vec<v1::element::ProtocolSection> = Vec::new();
    for section in &source_file.sections {
        let converted = section
            .elements
            .iter()
            .map(convert_element)
            .collect::<Result<Vec<_>>>()?;
        // The section index lists protocol paths, which for functions
        // include the signature.
        let element_paths = converted
            .iter()
            .map(v1::element::PackageableElement::path)
            .collect();
        elements.extend(converted);

        // The engine's `###Pure` parser (`DomainParser`) always produces an
        // import-aware section. For plug-in sections the engine decides per
        // section parser; until section grammars carry that choice, sections
        // with imports are import-aware and the rest default.
        sections.push(if section.kind != "Pure" && section.imports.is_empty() {
            v1::element::ProtocolSection::Default(v1::element::DefaultCodeSection {
                parser_name: section.kind.to_string(),
                elements: element_paths,
                source_information: source_information(&section.source_info),
            })
        } else {
            v1::element::ProtocolSection::ImportAware(v1::element::ImportAwareCodeSection {
                parser_name: section.kind.to_string(),
                elements: element_paths,
                imports: section.imports.iter().map(|i| i.path.to_string()).collect(),
                source_information: source_information(&section.source_info),
            })
        });
    }

    // `PureGrammarParser` names the index `__internal__::SectionIndex`.
    elements.push(v1::element::PackageableElement::SectionIndex(
        v1::element::ProtocolSectionIndex {
            package_path: "__internal__".to_string(),
            name: "SectionIndex".to_string(),
            sections,
            source_information: source_information(&source_file.source_info),
        },
    ));

    Ok(v1::context::PureModelContextData::new(elements))
}

#[cfg(test)]
mod tests {
    use super::*;
    use legend_pure_parser_ast as ast;
    use legend_pure_parser_ast::type_ref::Identifier;

    fn src() -> ast::SourceInfo {
        ast::SourceInfo::new("test.pure", 1, 1, 1, 10)
    }

    #[test]
    fn test_source_info_conversion() {
        let ast_si = ast::SourceInfo::new("test.pure", 3, 5, 10, 20);
        let proto_si: v1::source_info::SourceInformation = (&ast_si).into();
        assert_eq!(proto_si.source_id, "test.pure");
        assert_eq!(proto_si.start_line, 3);
        assert_eq!(proto_si.start_column, 5);
        assert_eq!(proto_si.end_line, 10);
        assert_eq!(proto_si.end_column, 20);
    }

    #[test]
    fn test_multiplicity_conversion() {
        let pure_one: v1::multiplicity::Multiplicity = (&ast::Multiplicity::one()).into();
        assert_eq!(pure_one, v1::multiplicity::Multiplicity::PURE_ONE);

        let zero_many: v1::multiplicity::Multiplicity = (&ast::Multiplicity::zero_or_many()).into();
        assert_eq!(zero_many, v1::multiplicity::Multiplicity::ZERO_MANY);

        let zero_one: v1::multiplicity::Multiplicity = (&ast::Multiplicity::zero_or_one()).into();
        assert_eq!(zero_one, v1::multiplicity::Multiplicity::ZERO_ONE);
    }

    #[test]
    fn test_package_to_path() {
        let pkg = ast::type_ref::Package::root(Identifier::new("meta"), src())
            .child(Identifier::new("pure"), src())
            .child(Identifier::new("profiles"), src());
        assert_eq!(package_to_path(&pkg), "meta::pure::profiles");
    }

    #[test]
    fn test_type_reference_conversion() {
        let tr = ast::type_ref::TypeReference {
            package: None,
            name: Identifier::new("String"),
            type_arguments: vec![],
            multiplicity_arguments: vec![],
            type_variable_values: vec![],
            source_info: src(),
        };
        let gt: v1::generic_type::GenericType = (&tr).into();
        assert_eq!(gt.raw_type.full_path(), Some("String"));
        assert!(gt.type_arguments.is_empty());
    }

    #[test]
    fn test_stereotype_ptr_conversion() {
        let ast_sp = ast::annotation::StereotypePtr {
            profile: ast::annotation::PackageableElementPtr {
                package: Some(
                    ast::type_ref::Package::root(Identifier::new("meta"), src())
                        .child(Identifier::new("pure"), src())
                        .child(Identifier::new("profiles"), src()),
                ),
                name: Identifier::new("temporal"),
                source_info: src(),
            },
            value: Identifier::new("businesstemporal"),
            value_source_info: src(),
            source_info: src(),
        };
        let proto_sp: v1::annotation::StereotypePtr = (&ast_sp).into();
        assert_eq!(proto_sp.profile, "meta::pure::profiles::temporal");
        assert_eq!(proto_sp.value, "businesstemporal");
    }

    #[test]
    fn test_integer_literal_conversion() {
        let expr = ast::expression::Expression::Literal(ast::expression::Literal::Integer(
            ast::expression::IntegerLiteral {
                value: 42,
                source_info: src(),
            },
        ));
        let vs = convert_expression_typed(&expr).unwrap();
        match vs {
            v1::value_spec::ValueSpecification::Integer(ci) => {
                assert_eq!(ci.value, 42);
            }
            other => panic!("Expected Integer, got {other:?}"),
        }
    }

    #[test]
    fn test_variable_conversion() {
        let expr = ast::expression::Expression::Variable(ast::expression::Variable {
            name: Identifier::new("name"),
            source_info: src(),
        });
        let vs = convert_expression_typed(&expr).unwrap();
        match vs {
            v1::value_spec::ValueSpecification::Var(v) => {
                assert_eq!(v.name, "name");
            }
            other => panic!("Expected Var, got {other:?}"),
        }
    }

    #[test]
    fn test_arithmetic_uses_the_engine_collection_shape() {
        let expr = ast::expression::Expression::Arithmetic(ast::expression::ArithmeticExpr {
            left: Box::new(ast::expression::Expression::Literal(
                ast::expression::Literal::Integer(ast::expression::IntegerLiteral {
                    value: 1,
                    source_info: src(),
                }),
            )),
            op: ast::expression::ArithmeticOp::Plus,
            right: Box::new(ast::expression::Expression::Literal(
                ast::expression::Literal::Integer(ast::expression::IntegerLiteral {
                    value: 2,
                    source_info: src(),
                }),
            )),
            left_span: src(),
            right_span: src(),
            source_info: src(),
        });
        let vs = convert_expression_typed(&expr).unwrap();
        // Engine: plus([1, 2]) — one collection parameter, not two operands.
        match vs {
            v1::value_spec::ValueSpecification::Func(f) => {
                assert_eq!(f.function, "plus");
                assert_eq!(f.parameters.len(), 1);
                match &f.parameters[0] {
                    v1::value_spec::ValueSpecification::Collection(c) => {
                        assert_eq!(c.values.len(), 2);
                    }
                    other => panic!("Expected Collection, got {other:?}"),
                }
            }
            other => panic!("Expected Func, got {other:?}"),
        }
    }

    #[test]
    fn test_convert_profile_element() {
        let profile = ast::element::Element::Profile(ast::element::ProfileDef {
            package: Some(ast::type_ref::Package::root(Identifier::new("meta"), src())),
            name: ast::annotation::SpannedString {
                value: Identifier::new("doc"),
                source_info: src(),
            },
            stereotype_names: vec![ast::annotation::SpannedString {
                value: Identifier::new("deprecated"),
                source_info: src(),
            }],
            tag_names: vec![ast::annotation::SpannedString {
                value: Identifier::new("description"),
                source_info: src(),
            }],
            stereotypes: vec![],
            tagged_values: vec![],
            source_info: src(),
        });
        let pe = convert_element(&profile).unwrap();
        match pe {
            v1::element::PackageableElement::Profile(p) => {
                assert_eq!(p.package_path, "meta");
                assert_eq!(p.name, "doc");
                assert_eq!(p.stereotypes[0].value, "deprecated");
                assert_eq!(p.tags[0].value, "description");
            }
            other => panic!("Expected Profile, got {other:?}"),
        }
    }

    #[test]
    fn test_convert_class_element() {
        let class = ast::element::Element::Class(ast::element::ClassDef {
            package: Some(
                ast::type_ref::Package::root(Identifier::new("model"), src())
                    .child(Identifier::new("domain"), src()),
            ),
            name: ast::annotation::SpannedString {
                value: Identifier::new("Person"),
                source_info: src(),
            },
            type_variable_parameters: vec![],
            type_and_multiplicity_parameters: None,
            super_types: vec![],
            properties: vec![],
            qualified_properties: vec![],
            constraints: vec![],
            stereotypes: vec![],
            tagged_values: vec![],
            source_info: src(),
        });
        let pe = convert_element(&class).unwrap();
        match pe {
            v1::element::PackageableElement::Class(c) => {
                assert_eq!(c.package_path, "model::domain");
                assert_eq!(c.name, "Person");
            }
            other => panic!("Expected Class, got {other:?}"),
        }
    }
}
