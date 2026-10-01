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
use legend_pure_parser_protocol::v1::value_spec::{
    CDateTime, ClassInstance, ProtocolCollection, ProtocolEnumValue, ValueSpecification, Variable,
};
use linkme::distributed_slice;

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
    Ok(ValueSpecification::ClassInstance(ClassInstance {
        type_name: "rootGraphFetchTree".to_string(),
        value: serde_json::json!({
            "_type": "rootGraphFetchTree",
            "class": tree.class.to_string(),
            "sourceInformation": position,
            "subTrees": tree
                .sub_trees
                .iter()
                .map(convert_property_graph_fetch_tree)
                .collect::<Result<Vec<_>>>()?,
            "subTypeTrees": tree
                .sub_type_trees
                .iter()
                .map(convert_sub_type_graph_fetch_tree)
                .collect::<Result<Vec<_>>>()?,
        }),
        source_information: Some(position),
    }))
}

fn convert_property_graph_fetch_tree(prop: &PropertyGraphFetchTree) -> Result<serde_json::Value> {
    let parameters = prop
        .parameters
        .iter()
        .map(convert_parameter)
        .collect::<Result<Vec<_>>>()?;
    let mut obj = serde_json::json!({
        "_type": "propertyGraphFetchTree",
        "property": prop.property.to_string(),
        "sourceInformation": source_information(&prop.property_source_info),
        "parameters": parameters,
        "subTrees": prop
            .sub_trees
            .iter()
            .map(convert_property_graph_fetch_tree)
            .collect::<Result<Vec<_>>>()?,
    });
    if let Some(alias) = &prop.alias {
        obj["alias"] = serde_json::json!(alias.to_string());
    }
    if let Some(sub_type) = &prop.sub_type {
        obj["subType"] = serde_json::json!(sub_type.to_string());
    }
    Ok(obj)
}

fn convert_sub_type_graph_fetch_tree(sub: &SubTypeGraphFetchTree) -> Result<serde_json::Value> {
    Ok(serde_json::json!({
        "_type": "subTypeGraphFetchTree",
        "subTypeClass": sub.sub_type_class.to_string(),
        "sourceInformation": source_information(&sub.sub_type_class.source_info),
        "subTrees": sub
            .sub_trees
            .iter()
            .map(convert_property_graph_fetch_tree)
            .collect::<Result<Vec<_>>>()?,
    }))
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

fn source_information(
    si: &legend_pure_parser_ast::source_info::SourceInfo,
) -> legend_pure_parser_protocol::v1::source_info::SourceInformation {
    legend_pure_parser_protocol::v1::source_info::SourceInformation::from(si)
}
