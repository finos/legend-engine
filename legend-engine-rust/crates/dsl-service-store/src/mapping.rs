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

//! `ServiceStore` mapping elements (`ServiceStoreParserGrammar`'s `mapping`
//! rule and its `ServiceStoreParseTreeWalker`), the engine's
//! `RootServiceStoreClassMapping`.
//!
//! ```text
//! classMapping:  (+ name: Type[m];)* serviceMapping*
//! serviceMapping: ~service [my::Store] group.service
//!                 ( ( ~path $service.response(.name)* )? ( ~request( parameters( name = expression, … )? body = expression? ) )?
//!                   ( ~paramMapping( name: expression, … ) )? ( property: $service.parameters.name, … )? )?
//! ```
//!
//! Each expression is the body of a lambda without parameters, positioned
//! on the expression. `~paramMapping` and the property mappings, which the
//! walker keeps for old mappings, become the request's parameters, and
//! replace those of `~request`. A property mapping's value is
//! `$this.property`, which the walker parses as if it were written where
//! the property is. A service mapping can't extend another.

use std::any::Any;

use legend_pure_dsl_mapping::ast::{MappingElement, MappingElementBody, MappingElementHeader};
use legend_pure_dsl_mapping::grammar::{
    ElementSpan, MAPPING_ELEMENT_GRAMMARS, MappingElementGrammar, MappingElementKind,
};
use legend_pure_parser_ast::expression::Expression;
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_ast::type_ref::Multiplicity;
use legend_pure_parser_lexer::{Token, TokenKind};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_parser::words::{
    Name, adjacent, at_command, at_word, eat_command, identifier, is_word, is_word_kind,
    qualified_name, unexpected,
};
use legend_pure_parser_parser::{ParserContext, string_value};
use legend_pure_parser_protocol::element_protocol::ConvertError;
use legend_pure_parser_protocol::v1::convert::convert_expression_typed;
use legend_pure_parser_protocol::v1::multiplicity::Multiplicity as ProtocolMultiplicity;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use legend_pure_parser_protocol::v1::value_spec::{LambdaFunction, ValueSpecification};
use linkme::distributed_slice;
use serde::Serialize;
use serde_json::value::{RawValue, to_raw_value};
use smol_str::SmolStr;

/// The element type this grammar reads.
pub const TYPE_NAME: &str = "ServiceStore";

/// What the walker names the start of a path offset: the service's
/// response, replaced by its class when compiled.
const PATH_START: &str = "$service.response";

/// A service store class mapping's body.
#[derive(Debug, Clone, PartialEq)]
pub struct ServiceStoreBody {
    /// `+name: Type[m];`.
    pub local_properties: Vec<LocalProperty>,
    /// `~service …`.
    pub services: Vec<ServiceMapping>,
}

/// `+name: Type[m];`.
#[derive(Debug, Clone, PartialEq)]
pub struct LocalProperty {
    /// The name, unquoted.
    pub name: SmolStr,
    /// The type, as written without spaces.
    pub type_text: String,
    /// The multiplicity.
    pub multiplicity: Multiplicity,
    /// From `+` to `;`.
    pub source_info: SourceInfo,
}

/// `~service [my::Store] group.service ( … )`.
#[derive(Debug, Clone, PartialEq)]
pub struct ServiceMapping {
    /// The store, unquoted.
    pub store: String,
    /// The service's path: its groups, then the service, unquoted.
    pub path: Vec<Name>,
    /// `~path $service.response.a.b`: the properties, unquoted.
    pub path_offset: Option<Vec<SmolStr>>,
    /// `~request( … )`.
    pub request: Option<Request>,
    /// `~paramMapping( name: expression, … )`.
    pub parameter_mappings: Option<Vec<ParameterValue>>,
    /// `property: $service.parameters.name, …`.
    pub property_mappings: Option<Vec<PropertyParameter>>,
    /// From `~service` to the end.
    pub source_info: SourceInfo,
}

/// `~request( parameters( … )? body = expression? )`.
#[derive(Debug, Clone, PartialEq)]
pub struct Request {
    /// `parameters( name = expression, … )`, and its span.
    pub parameters: Option<(Vec<ParameterValue>, SourceInfo)>,
    /// `body = expression`: the expression, its span, and the whole.
    pub body: Option<(Expression, SourceInfo, SourceInfo)>,
    /// From `~request` to `)`.
    pub source_info: SourceInfo,
}

/// `name = expression` or `name: expression`: a service parameter's value.
#[derive(Debug, Clone, PartialEq)]
pub struct ParameterValue {
    /// The parameter, unquoted.
    pub parameter: String,
    /// The value.
    pub expression: Expression,
    /// The value's span.
    pub expression_source_info: SourceInfo,
    /// From the parameter to the end of the value.
    pub source_info: SourceInfo,
}

/// `property: $service.parameters.name`: a parameter given a property's
/// value, `$this.property` as the walker writes it.
#[derive(Debug, Clone, PartialEq)]
pub struct PropertyParameter {
    /// The parameter, unquoted.
    pub parameter: String,
    /// `$this.property`, positioned where the property is.
    pub expression: Expression,
    /// From the property to the parameter.
    pub source_info: SourceInfo,
}

impl MappingElementBody for ServiceStoreBody {
    fn as_any(&self) -> &dyn Any {
        self
    }

    fn clone_box(&self) -> Box<dyn MappingElementBody> {
        Box::new(self.clone())
    }

    fn eq_content(&self, other: &dyn MappingElementBody) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|other| self == other)
    }
}

/// The service store mapping element grammar.
pub struct ServiceStoreMappingGrammar;

impl MappingElementGrammar for ServiceStoreMappingGrammar {
    #[allow(clippy::unnecessary_literal_bound)]
    fn type_name(&self) -> &str {
        TYPE_NAME
    }

    fn parse(
        &self,
        header: &MappingElementHeader,
        _span: &ElementSpan,
        ctx: &mut ParserContext<'_>,
    ) -> Result<Box<dyn MappingElementBody>, ParseError> {
        let mut local_properties = Vec::new();
        while ctx.cursor().check(TokenKind::Plus) {
            local_properties.push(local_property(ctx)?);
        }
        let mut services = Vec::new();
        while !ctx.cursor().check(TokenKind::Eof) {
            services.push(service_mapping(ctx)?);
        }
        // The extension's check, once the body is read.
        if let Some(extends) = &header.extends {
            ctx.defer(ParseError::Unexpected {
                message: "Service Store Mapping does not support extends".to_string(),
                source_info: extends.source_info.clone(),
            });
        }
        Ok(Box::new(ServiceStoreBody {
            local_properties,
            services,
        }))
    }

    fn convert(
        &self,
        element: &MappingElement,
    ) -> Result<(MappingElementKind, Box<RawValue>), ConvertError> {
        let body = element
            .body
            .as_any()
            .downcast_ref::<ServiceStoreBody>()
            .ok_or_else(|| {
                ConvertError::plugin(
                    "ServiceStoreMappingGrammar: the body is not a service store mapping's",
                    Some(&element.source_info),
                )
            })?;
        let header = &element.header;
        let json = ClassMappingJson {
            class: &header.target.path,
            root: header.root,
            local_mapping_properties: body
                .local_properties
                .iter()
                .map(|property| LocalPropertyJson {
                    name: property.name.as_str(),
                    property_type: &property.type_text,
                    multiplicity: ProtocolMultiplicity::from(&property.multiplicity),
                    source_information: SourceInformation::from(&property.source_info),
                })
                .collect(),
            services_mapping: body
                .services
                .iter()
                .map(service_mapping_json)
                .collect::<Result<Vec<_>, ConvertError>>()?,
            source_information: SourceInformation::from(&element.source_info),
            class_source_information: SourceInformation::from(&header.target.source_info),
            id: header.id.as_ref().map(|id| id.text.as_str()),
        };
        Ok((MappingElementKind::Class, to_raw_value(&json)?))
    }
}

/// `+name: Type[m];`, the type an identifier or `a::B <- c::D`.
fn local_property(ctx: &mut ParserContext<'_>) -> Result<LocalProperty, ParseError> {
    let start = ctx.cursor().current_source_info();
    ctx.cursor().advance();
    let name = identifier(ctx)?.value;
    ctx.cursor().expect(TokenKind::Colon)?;
    let first = qualified_name(ctx)?;
    let cursor = ctx.cursor();
    let inverted_arrow = cursor.check(TokenKind::Less)
        && cursor.peek_kind_at(1) == TokenKind::Minus
        && adjacent(cursor.peek(), cursor.peek_at(1));
    let type_text = if inverted_arrow {
        ctx.cursor().advance();
        ctx.cursor().advance();
        format!("{}<-{}", first.text, qualified_name(ctx)?.text)
    } else if first.has_package {
        return Err(unexpected(ctx.cursor().peek()));
    } else {
        first.text
    };
    ctx.cursor().expect(TokenKind::LBracket)?;
    let multiplicity = ctx.parse_multiplicity()?;
    ctx.cursor().expect(TokenKind::RBracket)?;
    ctx.cursor().expect(TokenKind::Semicolon)?;
    Ok(LocalProperty {
        name,
        type_text,
        multiplicity,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `~service [my::Store] group.service ( … )?`.
fn service_mapping(ctx: &mut ParserContext<'_>) -> Result<ServiceMapping, ParseError> {
    let start = ctx.cursor().current_source_info();
    if !eat_command(ctx, "service") {
        return Err(unexpected(ctx.cursor().peek()));
    }
    ctx.cursor().expect(TokenKind::LBracket)?;
    let store = qualified_name(ctx)?.path;
    ctx.cursor().expect(TokenKind::RBracket)?;
    let mut path = vec![identifier(ctx)?];
    while ctx.cursor().eat(TokenKind::Dot) {
        path.push(identifier(ctx)?);
    }
    let (mut path_offset, mut request, mut parameter_mappings, mut property_mappings) =
        (None, None, None, None);
    if ctx.cursor().eat(TokenKind::LParen) {
        if eat_command(ctx, "path") {
            service_reference(ctx)?;
            ctx.cursor().expect(TokenKind::Dot)?;
            let response = ctx.cursor().peek().clone();
            if !is_word(&response, "response") {
                return Err(unexpected(&response));
            }
            ctx.cursor().advance();
            let mut properties = Vec::new();
            while ctx.cursor().eat(TokenKind::Dot) {
                properties.push(identifier(ctx)?.value);
            }
            path_offset = Some(properties);
        }
        if at_command(ctx, "request") {
            request = Some(request_block(ctx)?);
        }
        if eat_command(ctx, "paramMapping") {
            ctx.cursor().expect(TokenKind::LParen)?;
            let mut mappings = vec![parameter_value(ctx, TokenKind::Colon)?];
            while ctx.cursor().eat(TokenKind::Comma) {
                mappings.push(parameter_value(ctx, TokenKind::Colon)?);
            }
            ctx.cursor().expect(TokenKind::RParen)?;
            parameter_mappings = Some(mappings);
        }
        if !ctx.cursor().check(TokenKind::RParen) {
            let mut mappings = vec![property_parameter(ctx)?];
            while ctx.cursor().eat(TokenKind::Comma) {
                mappings.push(property_parameter(ctx)?);
            }
            property_mappings = Some(mappings);
        }
        ctx.cursor().expect(TokenKind::RParen)?;
    }
    Ok(ServiceMapping {
        store,
        path,
        path_offset,
        request,
        parameter_mappings,
        property_mappings,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `$service`, one token in the grammar's lexer.
fn service_reference(ctx: &mut ParserContext<'_>) -> Result<(), ParseError> {
    let cursor = ctx.cursor();
    let (dollar, name) = (cursor.peek().clone(), cursor.peek_at(1).clone());
    if dollar.kind != TokenKind::Dollar || !is_word(&name, "service") || !adjacent(&dollar, &name) {
        return Err(unexpected(&dollar));
    }
    cursor.advance();
    cursor.advance();
    Ok(())
}

/// `~request( parameters( name = expression, … )? body = expression? )`.
fn request_block(ctx: &mut ParserContext<'_>) -> Result<Request, ParseError> {
    let start = ctx.cursor().current_source_info();
    eat_command(ctx, "request");
    ctx.cursor().expect(TokenKind::LParen)?;
    let parameters = if at_word(ctx, "parameters") {
        let parameters_start = ctx.cursor().current_source_info();
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::LParen)?;
        let mut values = vec![parameter_value(ctx, TokenKind::Equals)?];
        while ctx.cursor().eat(TokenKind::Comma) {
            values.push(parameter_value(ctx, TokenKind::Equals)?);
        }
        ctx.cursor().expect(TokenKind::RParen)?;
        Some((values, ctx.cursor().span_since(&parameters_start)))
    } else {
        None
    };
    let body = if at_word(ctx, "body") {
        let body_start = ctx.cursor().current_source_info();
        ctx.cursor().advance();
        ctx.cursor().expect(TokenKind::Equals)?;
        let expression_start = ctx.cursor().current_source_info();
        let expression = ctx.parse_expression()?;
        let expression_span = ctx.cursor().span_since(&expression_start);
        Some((
            expression,
            expression_span,
            ctx.cursor().span_since(&body_start),
        ))
    } else {
        None
    };
    ctx.cursor().expect(TokenKind::RParen)?;
    Ok(Request {
        parameters,
        body,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// A parameter's name: a word, or a `"double-quoted"` name, unquoted.
pub(crate) fn parameter_name(ctx: &mut ParserContext<'_>) -> Result<String, ParseError> {
    let token: Token = ctx.cursor().peek().clone();
    if is_word_kind(token.kind) {
        ctx.cursor().advance();
        return Ok(token.text.to_string());
    }
    if token.kind == TokenKind::StringLiteral && token.text.starts_with('"') {
        ctx.cursor().advance();
        return string_value(&token.text)
            .map(|(value, _)| value)
            .map_err(|message| ParseError::Unexpected {
                message,
                source_info: token.source_info.clone(),
            });
    }
    Err(unexpected(&token))
}

/// `name <separator> expression`.
fn parameter_value(
    ctx: &mut ParserContext<'_>,
    separator: TokenKind,
) -> Result<ParameterValue, ParseError> {
    let start = ctx.cursor().current_source_info();
    let parameter = parameter_name(ctx)?;
    ctx.cursor().expect(separator)?;
    let expression_start = ctx.cursor().current_source_info();
    let expression = ctx.parse_expression()?;
    let expression_source_info = ctx.cursor().span_since(&expression_start);
    Ok(ParameterValue {
        parameter,
        expression,
        expression_source_info,
        source_info: ctx.cursor().span_since(&start),
    })
}

/// `property: $service.parameters.name`.
fn property_parameter(ctx: &mut ParserContext<'_>) -> Result<PropertyParameter, ParseError> {
    let start = ctx.cursor().current_source_info();
    let property = identifier(ctx)?;
    ctx.cursor().expect(TokenKind::Colon)?;
    service_reference(ctx)?;
    ctx.cursor().expect(TokenKind::Dot)?;
    let parameters = ctx.cursor().peek().clone();
    if !is_word(&parameters, "parameters") {
        return Err(unexpected(&parameters));
    }
    ctx.cursor().advance();
    ctx.cursor().expect(TokenKind::Dot)?;
    let parameter = parameter_name(ctx)?;
    let source_info = ctx.cursor().span_since(&start);
    let expression =
        ctx.parse_expression_text(&format!("$this.{}", property.value), &property.source_info)?;
    Ok(PropertyParameter {
        parameter,
        expression,
        source_info,
    })
}

// ── The protocol ───────────────────────────────────────────────────────

/// The engine's `RootServiceInstanceSetImplementation`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "serviceStore", rename_all = "camelCase")]
struct ClassMappingJson<'a> {
    class: &'a str,
    root: bool,
    local_mapping_properties: Vec<LocalPropertyJson<'a>>,
    services_mapping: Vec<ServiceMappingJson<'a>>,
    source_information: SourceInformation,
    class_source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    id: Option<&'a str>,
}

/// The engine's `LocalMappingProperty`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct LocalPropertyJson<'a> {
    name: &'a str,
    #[serde(rename = "type")]
    property_type: &'a str,
    multiplicity: ProtocolMultiplicity,
    source_information: SourceInformation,
}

/// An expression as the body of a lambda without parameters, positioned on
/// `span`.
fn lambda(expression: &Expression, span: &SourceInfo) -> Result<ValueSpecification, ConvertError> {
    Ok(ValueSpecification::Lambda(LambdaFunction {
        body: vec![convert_expression_typed(expression)?],
        parameters: Vec::new(),
        source_information: Some(SourceInformation::from(span)),
    }))
}

/// The engine's `ServiceRequestParameterBuildInfo`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ParameterJson<'a> {
    service_parameter: &'a str,
    transform: ValueSpecification,
    source_information: SourceInformation,
}

fn parameter_json(value: &ParameterValue) -> Result<ParameterJson<'_>, ConvertError> {
    Ok(ParameterJson {
        service_parameter: &value.parameter,
        transform: lambda(&value.expression, &value.expression_source_info)?,
        source_information: SourceInformation::from(&value.source_info),
    })
}

/// The engine's `ServiceMapping`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ServiceMappingJson<'a> {
    service: ServicePointerJson<'a>,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    path_offset: Option<PathJson<'a>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    request_build_info: Option<RequestJson<'a>>,
}

/// The response's path, the engine's `Path`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct PathJson<'a> {
    start_type: &'static str,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    path: Vec<PropertyPathJson<'a>>,
}

/// The engine's `PropertyPathElement`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "propertyPath")]
struct PropertyPathJson<'a> {
    property: &'a str,
    parameters: [(); 0],
}

/// The engine's `ServiceRequestBuildInfo`.
#[derive(Serialize, Default)]
#[serde(rename_all = "camelCase")]
struct RequestJson<'a> {
    #[serde(skip_serializing_if = "Option::is_none")]
    source_information: Option<SourceInformation>,
    #[serde(skip_serializing_if = "Option::is_none")]
    request_parameters_build_info: Option<ParametersJson<'a>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    request_body_build_info: Option<BodyJson>,
}

/// The engine's `ServiceRequestParametersBuildInfo`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ParametersJson<'a> {
    parameter_build_info_list: Vec<ParameterJson<'a>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    source_information: Option<SourceInformation>,
}

/// The engine's `ServiceRequestBodyBuildInfo`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct BodyJson {
    transform: ValueSpecification,
    source_information: SourceInformation,
}

fn service_mapping_json(mapping: &ServiceMapping) -> Result<ServiceMappingJson<'_>, ConvertError> {
    let path_offset = mapping.path_offset.as_ref().map(|properties| PathJson {
        start_type: PATH_START,
        path: properties
            .iter()
            .map(|property| PropertyPathJson {
                property: property.as_str(),
                parameters: [],
            })
            .collect(),
    });
    let mut request = match &mapping.request {
        Some(request) => Some(RequestJson {
            source_information: Some(SourceInformation::from(&request.source_info)),
            request_parameters_build_info: request
                .parameters
                .as_ref()
                .map(|(parameters, source_info)| {
                    Ok::<_, ConvertError>(ParametersJson {
                        parameter_build_info_list: parameters
                            .iter()
                            .map(parameter_json)
                            .collect::<Result<Vec<_>, ConvertError>>()?,
                        source_information: Some(SourceInformation::from(source_info)),
                    })
                })
                .transpose()?,
            request_body_build_info: request
                .body
                .as_ref()
                .map(|(expression, expression_span, source_info)| {
                    Ok::<_, ConvertError>(BodyJson {
                        transform: lambda(expression, expression_span)?,
                        source_information: SourceInformation::from(source_info),
                    })
                })
                .transpose()?,
        }),
        None => None,
    };
    // The old forms: the request's parameters, replacing `~request`'s.
    if mapping.parameter_mappings.is_some() || mapping.property_mappings.is_some() {
        let mut parameters = Vec::new();
        for value in mapping.parameter_mappings.iter().flatten() {
            parameters.push(parameter_json(value)?);
        }
        for property in mapping.property_mappings.iter().flatten() {
            parameters.push(ParameterJson {
                service_parameter: &property.parameter,
                transform: lambda(&property.expression, &property.source_info)?,
                source_information: SourceInformation::from(&property.source_info),
            });
        }
        request
            .get_or_insert_with(RequestJson::default)
            .request_parameters_build_info = Some(ParametersJson {
            parameter_build_info_list: parameters,
            source_information: None,
        });
    }
    Ok(ServiceMappingJson {
        service: service_pointer(&mapping.store, &mapping.path, &mapping.source_info)?,
        source_information: SourceInformation::from(&mapping.source_info),
        path_offset,
        request_build_info: request,
    })
}

/// The engine's `ServicePtr`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ServicePointerJson<'a> {
    service_store: &'a str,
    service: &'a str,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    parent: Option<GroupPointerJson<'a>>,
}

/// The engine's `ServiceGroupPtr`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct GroupPointerJson<'a> {
    service_store: &'a str,
    service_group: &'a str,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    parent: Option<Box<GroupPointerJson<'a>>>,
}

/// The engine's `ServicePtr`: the last name of `path`, in its groups. The
/// grammar reads at least one name; `span` is the mapping's, for a path
/// without one.
fn service_pointer<'a>(
    store: &'a str,
    path: &'a [Name],
    span: &SourceInfo,
) -> Result<ServicePointerJson<'a>, ConvertError> {
    let (service, groups) = path
        .split_last()
        .ok_or_else(|| ConvertError::plugin("a service mapping without a service", Some(span)))?;
    Ok(ServicePointerJson {
        service_store: store,
        service: service.value.as_str(),
        source_information: SourceInformation::from(&service.source_info),
        parent: group_pointer(store, groups),
    })
}

/// The engine's `ServiceGroupPtr`, the last of `groups` in the others.
fn group_pointer<'a>(store: &'a str, groups: &'a [Name]) -> Option<GroupPointerJson<'a>> {
    let (group, parents) = groups.split_last()?;
    Some(GroupPointerJson {
        service_store: store,
        service_group: group.value.as_str(),
        source_information: SourceInformation::from(&group.source_info),
        parent: group_pointer(store, parents).map(Box::new),
    })
}

#[distributed_slice(MAPPING_ELEMENT_GRAMMARS)]
static SERVICE_STORE_MAPPING_GRAMMAR: &(dyn MappingElementGrammar + Send + Sync) =
    &ServiceStoreMappingGrammar;
