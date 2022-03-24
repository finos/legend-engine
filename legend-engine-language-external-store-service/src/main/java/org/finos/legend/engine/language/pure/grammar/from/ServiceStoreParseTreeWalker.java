// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ServiceStoreParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.*;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.path.Path;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.path.PropertyPathElement;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServiceStoreParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public static final String SERVICE_MAPPING_PATH_PREFIX = "$service.response";

    public ServiceStoreParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = null;
        this.section = null;
    }

    public ServiceStoreParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(ServiceStoreParserGrammar.DefinitionContext ctx)
    {
        ctx.serviceStore().stream().map(this::visitServiceStore).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private ServiceStore visitServiceStore(ServiceStoreParserGrammar.ServiceStoreContext ctx)
    {
        ServiceStore serviceStore = new ServiceStore();
        serviceStore.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        serviceStore._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        serviceStore.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        serviceStore.elements = ListIterate.collect(ctx.serviceStoreElement(), this::visitServiceStoreElement);
        validateServiceStoreElements(serviceStore.elements, serviceStore.sourceInformation);

        return serviceStore;
    }

    private void validateServiceStoreElements(List<ServiceStoreElement> elements, SourceInformation sourceInformation)
    {
        RichIterable<String> ids = ListIterate.collect(elements, element -> element.id);
        List<String> nonUniqueIds = ids.select(e -> Collections.frequency(ids.toList(), e) > 1).toSet().toList();

        if (nonUniqueIds != null && !nonUniqueIds.isEmpty())
        {
            throw new EngineException("Service Store Elements should have unique ids. Multiple elements found with ids - [" + String.join(",", nonUniqueIds) + "]", sourceInformation, EngineErrorType.PARSER);
        }
    }

    private ServiceStoreElement visitServiceStoreElement(ServiceStoreParserGrammar.ServiceStoreElementContext ctx)
    {
        if (ctx.serviceGroup() != null)
        {
            return this.visitServiceGroup(ctx.serviceGroup());
        }
        else
        {
            return this.visitService(ctx.service());
        }
    }

    private void validatePath(String path, SourceInformation sourceInformation)
    {
        if (!path.startsWith("/") || path.endsWith("/"))
        {
            throw new EngineException("Path should start with '/' & should not end with '/'", sourceInformation, EngineErrorType.PARSER);
        }
    }

    private ServiceGroup visitServiceGroup(ServiceStoreParserGrammar.ServiceGroupContext ctx)
    {
        ServiceGroup serviceGroup = new ServiceGroup();
        serviceGroup.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        // id
        serviceGroup.id = PureGrammarParserUtility.fromIdentifier(ctx.identifier());

        // path
        ServiceStoreParserGrammar.PathDefinitionContext pathCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.pathDefinition(), "path", this.walkerSourceInformation.getSourceInformation(ctx));
        serviceGroup.path = PureGrammarParserUtility.fromIdentifier(pathCtx.identifier());
        validatePath(serviceGroup.path, this.walkerSourceInformation.getSourceInformation(pathCtx.identifier()));

        // elements
        serviceGroup.elements = ListIterate.collect(ctx.serviceStoreElement(), this::visitServiceStoreElement);
        validateServiceStoreElements(serviceGroup.elements, serviceGroup.sourceInformation);

        return serviceGroup;
    }

    private Service visitService(ServiceStoreParserGrammar.ServiceContext ctx)
    {
        Service service = new Service();
        service.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        // id
        service.id = PureGrammarParserUtility.fromIdentifier(ctx.identifier());

        // path
        ServiceStoreParserGrammar.PathDefinitionContext pathCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.pathDefinition(), "path", this.walkerSourceInformation.getSourceInformation(ctx));
        service.path = PureGrammarParserUtility.fromIdentifier(pathCtx.identifier());
        validatePath(service.path, this.walkerSourceInformation.getSourceInformation(pathCtx.identifier()));

        // requestBody
        ServiceStoreParserGrammar.BodyDefinitionContext bodyCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.bodyDefinition(), "requestBody", this.walkerSourceInformation.getSourceInformation(ctx));
        if (bodyCtx != null)
        {
            service.requestBody = visitTypeReference(bodyCtx.typeReferenceDefinition());
        }

        // method
        ServiceStoreParserGrammar.MethodDefinitionContext methodCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.methodDefinition(), "method", this.walkerSourceInformation.getSourceInformation(ctx));
        String providedMethod = PureGrammarParserUtility.fromIdentifier(methodCtx.identifier());
        List<String> supportedMethods = ListIterate.collect(Lists.mutable.with(HttpMethod.values()), HttpMethod::toString);
        if (!supportedMethods.contains(providedMethod))
        {
            throw new EngineException("Unsupported HTTP Method type - " + providedMethod + ". Supported types are - " + String.join(",", supportedMethods), this.walkerSourceInformation.getSourceInformation(methodCtx), EngineErrorType.PARSER);
        }
        service.method = HttpMethod.valueOf(providedMethod);

        // parameters
        ServiceStoreParserGrammar.ParametersDefinitionContext parameterCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.parametersDefinition(), "parameters", this.walkerSourceInformation.getSourceInformation(ctx));
        if (parameterCtx != null)
        {
            service.parameters = ListIterate.collect(parameterCtx.parameterDefinition(), this::visitServiceParameter);
        }

        // response
        ServiceStoreParserGrammar.ResponseDefinitionContext responseCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.responseDefinition(), "response", this.walkerSourceInformation.getSourceInformation(ctx));
        TypeReference responseTypeReference = visitTypeReference(responseCtx.typeReferenceDefinition());
        if (!(responseTypeReference instanceof ComplexTypeReference))
        {
            throw new EngineException("Expected Complex Type Reference for response, found : " + responseTypeReference.getClass().getSimpleName(), this.walkerSourceInformation.getSourceInformation(responseCtx), EngineErrorType.PARSER);
        }
        service.response = (ComplexTypeReference) responseTypeReference;

        // security
        ServiceStoreParserGrammar.SecuritySchemeDefinitionContext supportedAuthCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.securitySchemeDefinition(), "security", this.walkerSourceInformation.getSourceInformation(ctx));
        service.security = ListIterate.collect(supportedAuthCtx.identifier(), this::visitSecurityScheme);

        validateService(service);
        return service;
    }

    private void validateService(Service service)
    {
        if (service.method == HttpMethod.GET && service.requestBody != null)
        {
            throw new EngineException("Request Body should not be specified for GET end point", service.sourceInformation, EngineErrorType.PARSER);
        }

        if (service.parameters != null)
        {
            List<String> pathParams = ListIterate.collectIf(service.parameters, param -> param.location == Location.PATH, param -> param.name);
            List<String> pathParamsNotPresentOnPath = ListIterate.select(pathParams, pathParam -> !service.path.contains("{" + pathParam + "}"));
            if (pathParamsNotPresentOnPath != null && !pathParamsNotPresentOnPath.isEmpty())
            {
                throw new EngineException("Path parameters should be specified in path as '{param_name}'. [" + String.join(",", pathParamsNotPresentOnPath) + "] parameters were not found in path " + service.path, service.sourceInformation, EngineErrorType.PARSER);
            }
        }
    }

    private ServiceParameter visitServiceParameter(ServiceStoreParserGrammar.ParameterDefinitionContext paramCtx)
    {
        ServiceParameter param = new ServiceParameter();
        param.sourceInformation = this.walkerSourceInformation.getSourceInformation(paramCtx);

        // name
        param.name = visitServiceParameterName(paramCtx.parameterName());

        // type
        ServiceStoreParserGrammar.TypeReferenceDefinitionContext typeCtx = paramCtx.typeReferenceDefinition();
        param.type = visitTypeReference(typeCtx);

        // parameter options
        if (paramCtx.parameterOptions() == null || paramCtx.parameterOptions().isEmpty())
        {
            throw new EngineException("Field location is required for service parameters", this.walkerSourceInformation.getSourceInformation(paramCtx), EngineErrorType.PARSER);
        }
        ServiceStoreParserGrammar.LocationDefinitionContext locationCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ListIterate.collectIf(paramCtx.parameterOptions(), p -> p.locationDefinition() != null, ServiceStoreParserGrammar.ParameterOptionsContext::locationDefinition), "location", this.walkerSourceInformation.getSourceInformation(paramCtx));
        String providedLocation = PureGrammarParserUtility.fromIdentifier(locationCtx.identifier()).toUpperCase();
        List<String> supportedLocations = ListIterate.collect(Lists.mutable.with(Location.values()), Location::toString);
        if (!supportedLocations.contains(providedLocation))
        {
            throw new EngineException("Unsupported Parameter Location - " + PureGrammarParserUtility.fromIdentifier(locationCtx.identifier()) + ". Supported Locations are - " + String.join(",", ListIterate.collect(supportedLocations, String::toLowerCase)), this.walkerSourceInformation.getSourceInformation(paramCtx), EngineErrorType.PARSER);
        }
        param.location = Location.valueOf(providedLocation);

        ServiceStoreParserGrammar.EnumDefinitionContext enumCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ListIterate.collectIf(paramCtx.parameterOptions(), p -> p.enumDefinition() != null, ServiceStoreParserGrammar.ParameterOptionsContext::enumDefinition), "enum", this.walkerSourceInformation.getSourceInformation(paramCtx));
        if (enumCtx != null)
        {
            param.enumeration = PureGrammarParserUtility.fromQualifiedName(enumCtx.qualifiedName().packagePath() == null ? Collections.emptyList() : enumCtx.qualifiedName().packagePath().identifier(), enumCtx.qualifiedName().identifier());
        }

        ServiceStoreParserGrammar.AllowReservedDefinitionContext allowReservedCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ListIterate.collectIf(paramCtx.parameterOptions(), p -> p.allowReservedDefinition() != null, ServiceStoreParserGrammar.ParameterOptionsContext::allowReservedDefinition), "allowReserved", this.walkerSourceInformation.getSourceInformation(paramCtx));
        if (allowReservedCtx != null)
        {
            param.allowReserved = Boolean.parseBoolean(allowReservedCtx.BOOLEAN().getText());
        }

        ServiceStoreParserGrammar.RequiredDefinitionContext requiredCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ListIterate.collectIf(paramCtx.parameterOptions(), p -> p.requiredDefinition() != null, ServiceStoreParserGrammar.ParameterOptionsContext::requiredDefinition), "required", this.walkerSourceInformation.getSourceInformation(paramCtx));
        if (requiredCtx != null)
        {
            boolean required = Boolean.parseBoolean(requiredCtx.BOOLEAN().getText());
            if ((param.location == Location.PATH) && (!required))
            {
                throw new EngineException("Path parameters cannot be optional", this.walkerSourceInformation.getSourceInformation(requiredCtx), EngineErrorType.PARSER);
            }
            param.required = required;
        }

        SerializationFormat serializationFormat = new SerializationFormat();
        if (param.type.list)
        {
            ServiceStoreParserGrammar.StyleDefinitionContext styleCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ListIterate.collectIf(paramCtx.parameterOptions(), p -> p.styleDefinition() != null, ServiceStoreParserGrammar.ParameterOptionsContext::styleDefinition), "style", this.walkerSourceInformation.getSourceInformation(paramCtx));
            serializationFormat.style = PureGrammarParserUtility.fromIdentifier(styleCtx.identifier());
            serializationFormat.styleSourceInformation = this.walkerSourceInformation.getSourceInformation(styleCtx);

            ServiceStoreParserGrammar.ExplodeDefinitionContext explodeCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ListIterate.collectIf(paramCtx.parameterOptions(), p -> p.explodeDefinition() != null, ServiceStoreParserGrammar.ParameterOptionsContext::explodeDefinition), "explode", this.walkerSourceInformation.getSourceInformation(paramCtx));
            serializationFormat.explode = Boolean.parseBoolean(explodeCtx.BOOLEAN().getText());
            serializationFormat.explodeSourceInformation = this.walkerSourceInformation.getSourceInformation(explodeCtx);
        }
        else
        {
            ServiceStoreParserGrammar.StyleDefinitionContext styleCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ListIterate.collectIf(paramCtx.parameterOptions(), p -> p.styleDefinition() != null, ServiceStoreParserGrammar.ParameterOptionsContext::styleDefinition), "style", this.walkerSourceInformation.getSourceInformation(paramCtx));
            if (styleCtx != null)
            {
                throw new EngineException("style should not be provided with non-list service parameter", this.walkerSourceInformation.getSourceInformation(styleCtx), EngineErrorType.PARSER);
            }

            ServiceStoreParserGrammar.ExplodeDefinitionContext explodeCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ListIterate.collectIf(paramCtx.parameterOptions(), p -> p.explodeDefinition() != null, ServiceStoreParserGrammar.ParameterOptionsContext::explodeDefinition), "explode", this.walkerSourceInformation.getSourceInformation(paramCtx));
            if (explodeCtx != null)
            {
                throw new EngineException("explode should not be provided with non-list service parameter", this.walkerSourceInformation.getSourceInformation(styleCtx), EngineErrorType.PARSER);
            }
        }
        param.serializationFormat = serializationFormat;

        return param;
    }

    private String visitServiceParameterName(ServiceStoreParserGrammar.ParameterNameContext paramCtx)
    {
        if (paramCtx.unquotedIdentifier() != null)
        {
            return PureGrammarParserUtility.fromIdentifier(paramCtx.unquotedIdentifier());
        }
        else
        {
            return PureGrammarParserUtility.fromGrammarString(paramCtx.QUOTED_STRING().getText(), true);
        }
    }

    private TypeReference visitTypeReference(ServiceStoreParserGrammar.TypeReferenceDefinitionContext ctx)
    {
        TypeReference typeReference;
        ServiceStoreParserGrammar.TypeContext typeCtx = ctx.type() != null ? ctx.type() : ctx.listType().type();

        if (typeCtx.complexType() != null)
        {
            ComplexTypeReference complexTypeReference = new ComplexTypeReference();
            complexTypeReference.type = PureGrammarParserUtility.fromQualifiedName(typeCtx.complexType().qualifiedName(0).packagePath() == null ? Collections.emptyList() : typeCtx.complexType().qualifiedName(0).packagePath().identifier(), typeCtx.complexType().qualifiedName(0).identifier());
            complexTypeReference.binding = PureGrammarParserUtility.fromQualifiedName(typeCtx.complexType().qualifiedName(1).packagePath() == null ? Collections.emptyList() : typeCtx.complexType().qualifiedName(1).packagePath().identifier(), typeCtx.complexType().qualifiedName(1).identifier());

            typeReference = complexTypeReference;
        }
        else
        {
            String type = PureGrammarParserUtility.fromIdentifier(typeCtx.primitiveType().identifier());
            switch (type)
            {
                case "Boolean":
                {
                    typeReference = new BooleanTypeReference();
                    break;
                }
                case "Float":
                {
                    typeReference = new FloatTypeReference();
                    break;
                }
                case "Integer":
                {
                    typeReference = new IntegerTypeReference();
                    break;
                }
                case "String":
                {
                    typeReference = new StringTypeReference();
                    break;
                }
                default:
                {
                    throw new EngineException("Unsupported Parameter Value Type - " + type + ". Supported types are - Boolean, Float, Integer, String", this.walkerSourceInformation.getSourceInformation(typeCtx), EngineErrorType.PARSER);
                }
            }
        }

        typeReference.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        typeReference.list = ctx.listType() != null;

        return typeReference;
    }

    private SecurityScheme visitSecurityScheme(ServiceStoreParserGrammar.IdentifierContext securitySchemeCtx)
    {
        String securityScheme = PureGrammarParserUtility.fromIdentifier(securitySchemeCtx);
        List<Function<String, SecurityScheme>> processors = ListIterate.flatCollect(IServiceStoreGrammarParserExtension.getExtensions(), ext -> ext.getExtraSecuritySchemesParsers());

        return ListIterate
                .collect(processors, processor -> processor.apply(securityScheme))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported SecurityScheme - " + securityScheme, this.walkerSourceInformation.getSourceInformation(securitySchemeCtx), EngineErrorType.PARSER));
    }


    // -------------------------------------- CLASS MAPPING --------------------------------------

    public void visitRootServiceClassMapping(ServiceStoreParserGrammar.ClassMappingContext ctx, RootServiceStoreClassMapping rootServiceStoreClassMapping, String _class)
    {
        rootServiceStoreClassMapping.localMappingProperties = ListIterate.collect(ctx.localPropertyDefinition(), this::visitLocalMappingProperty);
        rootServiceStoreClassMapping.servicesMapping = ListIterate.collect(ctx.serviceMapping(), this::visitServiceMapping);
    }

    private LocalMappingProperty visitLocalMappingProperty(ServiceStoreParserGrammar.LocalPropertyDefinitionContext ctx)
    {
        LocalMappingProperty localMappingProperty = new LocalMappingProperty();

        localMappingProperty.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        localMappingProperty.type = ctx.type().getText();
        localMappingProperty.multiplicity = buildMultiplicity(ctx.multiplicity().multiplicityArgument());
        localMappingProperty.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        return localMappingProperty;
    }

    private ServiceMapping visitServiceMapping(ServiceStoreParserGrammar.ServiceMappingContext ctx)
    {
        ServiceMapping serviceMapping = new ServiceMapping();
        serviceMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        serviceMapping.service = buildServicePtr(ctx.mappingService());

        if (ctx.pathOffsetBlock() != null)
        {
            serviceMapping.pathOffset = visitPathOffset(ctx.pathOffsetBlock());
        }

        if (ctx.requestBuildingBlock() != null)
        {
            serviceMapping.requestBuildInfo = visitRequestBuildingBlock(ctx.requestBuildingBlock());
        }

        // TODO: TO BE REMOVED
        if (ctx.parametersMappingBlock() != null || ctx.mappingBlock() != null)
        {
            ServiceRequestBuildInfo serviceRequestBuildInfo = serviceMapping.requestBuildInfo == null ? new ServiceRequestBuildInfo() : serviceMapping.requestBuildInfo;
            ServiceRequestParametersBuildInfo requestParametersBuildInfo = new ServiceRequestParametersBuildInfo();
            requestParametersBuildInfo.parameterBuildInfoList = Lists.mutable.empty();

            if (ctx.parametersMappingBlock() != null)
            {
                requestParametersBuildInfo.parameterBuildInfoList.addAll(buildParameterIndexedParameterMappings(ctx.parametersMappingBlock()));
            }
            if (ctx.mappingBlock() != null)
            {
                requestParametersBuildInfo.parameterBuildInfoList.addAll(buildPropertyIndexedParameterMappings(ctx.mappingBlock()));
            }

            serviceRequestBuildInfo.requestParametersBuildInfo = requestParametersBuildInfo;
            serviceMapping.requestBuildInfo = serviceRequestBuildInfo;
        }

        return serviceMapping;
    }

    private Path visitPathOffset(ServiceStoreParserGrammar.PathOffsetBlockContext ctx)
    {
        Path p = new Path();

        //This is replaced with response class in compilation phase
        p.startType = SERVICE_MAPPING_PATH_PREFIX;

        if (ctx.identifier() != null && !ctx.identifier().isEmpty())
        {
            p.path = Lists.mutable.empty();
            for (ServiceStoreParserGrammar.IdentifierContext idCtx : ctx.identifier())
            {
                PropertyPathElement ppe = new PropertyPathElement();
                ppe.property = PureGrammarParserUtility.fromIdentifier(idCtx);
                ppe.parameters = Lists.mutable.empty();

                p.path.add(ppe);
            }
        }

        return p;
    }

    private ServiceRequestBuildInfo visitRequestBuildingBlock(ServiceStoreParserGrammar.RequestBuildingBlockContext ctx)
    {
        ServiceRequestBuildInfo requestBuildInfo = new ServiceRequestBuildInfo();

        if (ctx.requestParametersBuildingBlock() != null)
        {
            requestBuildInfo.requestParametersBuildInfo = visitRequestParametersBuildInfo(ctx.requestParametersBuildingBlock());
        }

        if (ctx.requestBodyBuildingBlock() != null)
        {
            requestBuildInfo.requestBodyBuildInfo = visitRequestBodyBuildInfo(ctx.requestBodyBuildingBlock());
        }

        requestBuildInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        return requestBuildInfo;
    }

    private ServiceRequestParametersBuildInfo visitRequestParametersBuildInfo(ServiceStoreParserGrammar.RequestParametersBuildingBlockContext ctx)
    {
        ServiceRequestParametersBuildInfo requestParametersBuildInfo = new ServiceRequestParametersBuildInfo();

        requestParametersBuildInfo.parameterBuildInfoList = ListIterate.collect(ctx.parameterBuildingBlock(), this::visitRequestParameterBuildInfo);
        requestParametersBuildInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        return requestParametersBuildInfo;
    }

    private ServiceRequestParameterBuildInfo visitRequestParameterBuildInfo(ServiceStoreParserGrammar.ParameterBuildingBlockContext ctx)
    {
        ServiceRequestParameterBuildInfo requestParameterBuildInfo = new ServiceRequestParameterBuildInfo();

        requestParameterBuildInfo.serviceParameter = visitServiceParameterName(ctx.parameterName());
        requestParameterBuildInfo.transform = visitLambda(ctx.combinedExpression());
        requestParameterBuildInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        return requestParameterBuildInfo;
    }

    private ServiceRequestBodyBuildInfo visitRequestBodyBuildInfo(ServiceStoreParserGrammar.RequestBodyBuildingBlockContext ctx)
    {
        ServiceRequestBodyBuildInfo requestBodyBuildInfo = new ServiceRequestBodyBuildInfo();

        requestBodyBuildInfo.transform = visitLambda(ctx.combinedExpression());
        requestBodyBuildInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        return requestBodyBuildInfo;
    }

    private Lambda visitLambda(ServiceStoreParserGrammar.CombinedExpressionContext ctx)
    {
        String lambdaString = ctx.start.getInputStream().getText(Interval.of(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));

        DomainParser parser = new DomainParser();

        // prepare island grammar walker source information
        int startLine = ctx.getStart().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation combineExpressionSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        ValueSpecification valueSpecification = parser.parseCombinedExpression(lambdaString, combineExpressionSourceInformation, null);

        Lambda lambda = new Lambda();
        lambda.body = new ArrayList<>();
        lambda.body.add(valueSpecification);
        lambda.parameters = Lists.mutable.empty();
        lambda.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        return lambda;
    }

    private ServicePtr buildServicePtr(ServiceStoreParserGrammar.MappingServiceContext ctx)
    {
        ServicePtr servicePtr = new ServicePtr();

        int pathDepth = ctx.servicePath().identifier().size();
        ServiceStoreParserGrammar.IdentifierContext serviceIdentifierCtx = ctx.servicePath().identifier(pathDepth - 1);
        servicePtr.serviceStore = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        servicePtr.service = PureGrammarParserUtility.fromIdentifier(serviceIdentifierCtx);
        if (pathDepth > 1)
        {
            servicePtr.parent = buildServiceGroupPtr(ctx.servicePath().identifier().subList(0, pathDepth - 1), servicePtr.serviceStore);
        }
        servicePtr.sourceInformation = this.walkerSourceInformation.getSourceInformation(serviceIdentifierCtx);

        return servicePtr;
    }

    private ServiceGroupPtr buildServiceGroupPtr(List<ServiceStoreParserGrammar.IdentifierContext> ctx, String serviceStore)
    {
        ServiceGroupPtr serviceGroupPtr = new ServiceGroupPtr();

        int pathDepth = ctx.size();
        ServiceStoreParserGrammar.IdentifierContext serviceGroupIdentifierCtx = ctx.get(pathDepth - 1);
        serviceGroupPtr.serviceStore = serviceStore;
        serviceGroupPtr.serviceGroup = PureGrammarParserUtility.fromIdentifier(serviceGroupIdentifierCtx);
        if (pathDepth > 1)
        {
            serviceGroupPtr.parent = buildServiceGroupPtr(ctx.subList(0, pathDepth - 1), serviceStore);
        }
        serviceGroupPtr.sourceInformation = this.walkerSourceInformation.getSourceInformation(serviceGroupIdentifierCtx);

        return serviceGroupPtr;
    }

    private Multiplicity buildMultiplicity(ServiceStoreParserGrammar.MultiplicityArgumentContext ctx)
    {
        String star = "*";
        Multiplicity m = new Multiplicity();
        m.lowerBound = Integer.parseInt(ctx.fromMultiplicity() != null ? ctx.fromMultiplicity().getText() : star.equals(ctx.toMultiplicity().getText()) ? "0" : ctx.toMultiplicity().getText());
        m.setUpperBound(star.equals(ctx.toMultiplicity().getText()) ? null : Integer.parseInt(ctx.toMultiplicity().getText()));
        return m;
    }


    // TODO: TO BE REMOVED
    private List<ServiceRequestParameterBuildInfo> buildParameterIndexedParameterMappings(ServiceStoreParserGrammar.ParametersMappingBlockContext ctx)
    {
        return ListIterate.collect(ctx.parameterMapping(), this::buildParameterIndexedParameterMapping);
    }

    private ServiceRequestParameterBuildInfo buildParameterIndexedParameterMapping(ServiceStoreParserGrammar.ParameterMappingContext ctx)
    {
        ServiceRequestParameterBuildInfo requestParameterBuildInfo = new ServiceRequestParameterBuildInfo();
        requestParameterBuildInfo.serviceParameter = visitServiceParameterName(ctx.parameterName());
        requestParameterBuildInfo.transform = visitLambda(ctx.combinedExpression());
        requestParameterBuildInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        return requestParameterBuildInfo;
    }

    private List<ServiceRequestParameterBuildInfo> buildPropertyIndexedParameterMappings(ServiceStoreParserGrammar.MappingBlockContext ctx)
    {
        return ListIterate.collect(ctx.elementMapping(), this::buildPropertyIndexedParameterMapping);
    }

    private ServiceRequestParameterBuildInfo buildPropertyIndexedParameterMapping(ServiceStoreParserGrammar.ElementMappingContext ctx)
    {
        DomainParser parser = new DomainParser();
        // prepare island grammar walker source information
        int startLine = ctx.getStart().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation combineExpressionSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        ValueSpecification valueSpecification = parser.parseCombinedExpression("$this." + PureGrammarParserUtility.fromIdentifier(ctx.identifier()), combineExpressionSourceInformation, null);

        Lambda lambda = new Lambda();
        lambda.body = new ArrayList<>();
        lambda.body.add(valueSpecification);
        lambda.parameters = Lists.mutable.empty();
        lambda.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        ServiceRequestParameterBuildInfo requestParameterBuildInfo = new ServiceRequestParameterBuildInfo();

        requestParameterBuildInfo.serviceParameter = visitServiceParameterName(ctx.parameterName());
        requestParameterBuildInfo.transform = lambda;
        requestParameterBuildInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        return requestParameterBuildInfo;
    }
}
