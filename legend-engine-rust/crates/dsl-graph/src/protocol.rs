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

//! `IslandProtocol` for graph-fetch.
//!
//! Tag: `""`. Converts `RootGraphFetchTree` AST into the v1 protocol's
//! `ClassInstance { _type: "rootGraphFetchTree", … }` value
//! specification, matching the Java engine's JSON output.
//!
//! Originally lived in
//! `legend-pure-parser-protocol::v1::convert`; moved here so core
//! protocol carries no graph-fetch knowledge. Plug-in callers
//! register via [`default_island_protocols`] when calling
//! [`legend_pure_parser_protocol::dispatch_island_convert`].

use legend_pure_parser_ast::expression::{
    DateTimeLiteral, Expression, Literal, StrictDateLiteral, StrictTimeLiteral,
};
use legend_pure_parser_ast::island::IslandContent;
use legend_pure_parser_protocol::IslandProtocol;
use legend_pure_parser_protocol::island_protocol::{ConvertError, ISLAND_PROTOCOLS, Result};
use legend_pure_parser_protocol::v1::convert::convert_expression_typed;
use legend_pure_parser_protocol::v1::multiplicity::Multiplicity;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::value_spec::{
    CDateTime, ClassInstance, ProtocolCollection, ProtocolEnumValue, ValueSpecification, Variable,
};
use linkme::distributed_slice;
use serde::Serialize;

use crate::ast::{
    GraphFetchParameter, PropertyGraphFetchTree, RootGraphFetchTree, ScalarParameter,
    SubTypeGraphFetchTree, TAG,
};

/// Protocol converter for graph-fetch trees. Emits the canonical
/// `_type: "rootGraphFetchTree"` JSON shape.
pub struct GraphFetchIslandProtocol;

impl IslandProtocol for GraphFetchIslandProtocol {
    fn tag(&self) -> &str {
        TAG
    }

    fn convert(&self, content: &dyn IslandContent) -> Result<ValueSpecification> {
        let tree = content
            .as_any()
            .downcast_ref::<RootGraphFetchTree>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    format!(
                        "GraphFetchIslandProtocol: content with tag '{}' is not a RootGraphFetchTree",
                        content.tag()
                    ),
                    None,
                )
            })?;
        convert_root_graph_fetch_tree(tree)
    }
}

#[distributed_slice(ISLAND_PROTOCOLS)]
static GRAPH_FETCH_PROTOCOL: &(dyn IslandProtocol + Send + Sync) = &GraphFetchIslandProtocol;

/// Convenience helper — `vec![Box::new(GraphFetchIslandProtocol)]`
/// for callers that want the empty-tag default protocol converter.
#[must_use]
pub fn default_island_protocols() -> Vec<Box<dyn IslandProtocol>> {
    vec![Box::new(GraphFetchIslandProtocol)]
}

// The engine's `GraphFetchTreeParseTreeWalker` shapes: every tree is
// positioned on its class or property name; parameters are the graph
// grammar's literals, variables, enum values and collections.

fn convert_root_graph_fetch_tree(tree: &RootGraphFetchTree) -> Result<ValueSpecification> {
    let position = source_information(&tree.class.source_info);
    let value = RootGraphFetchTreeJson {
        class: tree.class.to_string(),
        source_information: position.clone(),
        sub_trees: tree
            .sub_trees
            .iter()
            .map(convert_property_graph_fetch_tree)
            .collect::<Result<_>>()?,
        sub_type_trees: tree
            .sub_type_trees
            .iter()
            .map(convert_sub_type_graph_fetch_tree)
            .collect::<Result<_>>()?,
    };
    Ok(ValueSpecification::ClassInstance(ClassInstance {
        type_name: "rootGraphFetchTree".to_string(),
        value: serde_json::to_value(value)?,
        source_information: Some(position),
    }))
}

/// The engine's `RootGraphFetchTree`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "rootGraphFetchTree", rename_all = "camelCase")]
struct RootGraphFetchTreeJson<'a> {
    class: String,
    source_information: SourceInformation,
    sub_trees: Vec<PropertyGraphFetchTreeJson<'a>>,
    sub_type_trees: Vec<SubTypeGraphFetchTreeJson<'a>>,
}

/// The engine's `PropertyGraphFetchTree`.
#[derive(Serialize)]
#[serde(
    tag = "_type",
    rename = "propertyGraphFetchTree",
    rename_all = "camelCase"
)]
struct PropertyGraphFetchTreeJson<'a> {
    property: &'a str,
    source_information: SourceInformation,
    parameters: Vec<ValueSpecification>,
    sub_trees: Vec<PropertyGraphFetchTreeJson<'a>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    alias: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    sub_type: Option<String>,
}

/// The engine's `SubTypeGraphFetchTree`.
#[derive(Serialize)]
#[serde(
    tag = "_type",
    rename = "subTypeGraphFetchTree",
    rename_all = "camelCase"
)]
struct SubTypeGraphFetchTreeJson<'a> {
    sub_type_class: String,
    source_information: SourceInformation,
    sub_trees: Vec<PropertyGraphFetchTreeJson<'a>>,
}

fn convert_property_graph_fetch_tree(
    prop: &PropertyGraphFetchTree,
) -> Result<PropertyGraphFetchTreeJson<'_>> {
    let parameters = prop
        .parameters
        .iter()
        .map(convert_parameter)
        .collect::<Result<Vec<_>>>()?;
    Ok(PropertyGraphFetchTreeJson {
        property: prop.property.as_str(),
        source_information: source_information(&prop.property_source_info),
        parameters,
        sub_trees: prop
            .sub_trees
            .iter()
            .map(convert_property_graph_fetch_tree)
            .collect::<Result<_>>()?,
        alias: prop.alias.as_deref(),
        sub_type: prop.sub_type.as_ref().map(ToString::to_string),
    })
}

fn convert_sub_type_graph_fetch_tree(
    sub: &SubTypeGraphFetchTree,
) -> Result<SubTypeGraphFetchTreeJson<'_>> {
    Ok(SubTypeGraphFetchTreeJson {
        sub_type_class: sub.sub_type_class.to_string(),
        source_information: source_information(&sub.sub_type_class.source_info),
        sub_trees: sub
            .sub_trees
            .iter()
            .map(convert_property_graph_fetch_tree)
            .collect::<Result<_>>()?,
    })
}

/// A collection keeps no position; its values do.
fn convert_parameter(parameter: &GraphFetchParameter) -> Result<ValueSpecification> {
    Ok(match parameter {
        GraphFetchParameter::Scalar(scalar) => convert_scalar_parameter(scalar)?,
        GraphFetchParameter::Collection { values, .. } => {
            let bound = u32::try_from(values.len()).unwrap_or(u32::MAX);
            ValueSpecification::Collection(ProtocolCollection {
                multiplicity: Multiplicity {
                    lower_bound: bound,
                    upper_bound: Some(bound),
                },
                values: values
                    .iter()
                    .map(convert_scalar_parameter)
                    .collect::<Result<_>>()?,
                source_information: None,
            })
        }
    })
}

fn convert_scalar_parameter(scalar: &ScalarParameter) -> Result<ValueSpecification> {
    Ok(match scalar {
        // The engine's graph fetch walker makes any date a date-time of its
        // raw text, `%` included.
        ScalarParameter::Literal(
            Literal::StrictDate(StrictDateLiteral { value, source_info })
            | Literal::DateTime(DateTimeLiteral { value, source_info })
            | Literal::StrictTime(StrictTimeLiteral { value, source_info }),
        ) => ValueSpecification::DateTime(CDateTime {
            value: format!("%{value}"),
            source_information: Some(source_information(source_info)),
        }),
        ScalarParameter::Literal(literal) => {
            convert_expression_typed(&Expression::Literal(literal.clone()))?
        }
        ScalarParameter::Variable { name, source_info } => ValueSpecification::Var(Variable {
            name: name.to_string(),
            generic_type: None,
            multiplicity: None,
            supports_stream: None,
            source_information: Some(source_information(source_info)),
        }),
        ScalarParameter::EnumValue {
            enumeration,
            value,
            source_info,
        } => ValueSpecification::EnumValue(ProtocolEnumValue {
            full_path: enumeration.to_string(),
            value: value.to_string(),
            source_information: Some(source_information(source_info)),
        }),
    })
}

fn source_information(si: &legend_pure_parser_ast::source_info::SourceInfo) -> SourceInformation {
    SourceInformation::from(si)
}
