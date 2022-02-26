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

package org.finos.legend.engine.language.pure.grammar.to;

import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.*;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static org.finos.legend.engine.language.pure.grammar.from.ServiceStoreParseTreeWalker.SERVICE_MAPPING_PATH_PREFIX;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperServiceStoreGrammarComposer
{
    public static String renderServiceStore(ServiceStore serviceStore)
    {
        int baseIndentation = 1;

        StringBuilder builder = new StringBuilder();
        builder.append("ServiceStore ").append(PureGrammarComposerUtility.convertPath(serviceStore.getPath())).append("\n(\n");

        if (serviceStore.description != null)
        {
            builder.append("description : ").append("'").append(serviceStore.description).append("'").append(";\n\n");
        }
        renderServiceStoreElements(serviceStore.elements, builder, baseIndentation);

        builder.append(")");
        return builder.toString();
    }

    private static void renderServiceStoreElements(List<ServiceStoreElement> elements, StringBuilder builder, int baseIndentation)
    {
        List<Service> serviceList = ListIterate.selectInstancesOf(elements, Service.class);
        List<ServiceGroup> serviceGroupList = ListIterate.selectInstancesOf(elements, ServiceGroup.class);

        ListIterate.forEach(serviceList, service -> renderService(service, builder, baseIndentation));
        ListIterate.forEach(serviceGroupList, serviceGroup -> renderServiceGroup(serviceGroup, builder, baseIndentation));
    }

    private static void renderServiceGroup(ServiceGroup serviceGroup, StringBuilder builder, int baseIndentation)
    {
        builder.append(getTabString(baseIndentation)).append("ServiceGroup ").append(serviceGroup.id).append("\n");
        builder.append(getTabString(baseIndentation)).append("(\n");
        builder.append(getTabString(baseIndentation + 1)).append("path : ").append("'").append(serviceGroup.path).append("'").append(";\n\n");
        renderServiceStoreElements(serviceGroup.elements, builder, baseIndentation + 1);
        builder.append(getTabString(baseIndentation)).append(")\n");
    }

    private static void renderService(Service service, StringBuilder builder, int baseIndentation)
    {
        builder.append(getTabString(baseIndentation)).append("Service ").append(service.id).append("\n");
        builder.append(getTabString(baseIndentation)).append("(\n");
        builder.append(getTabString(baseIndentation + 1)).append("path : ").append("'").append(service.path).append("'").append(";\n");
        if (service.requestBody != null)
        {
            builder.append(getTabString(baseIndentation + 1)).append("requestBody : ").append(renderTypeReference(service.requestBody)).append(";\n");
        }
        builder.append(getTabString(baseIndentation + 1)).append("method : ").append(service.method).append(";\n");
        if (service.parameters != null)
        {
            builder.append(getTabString(baseIndentation + 1)).append("parameters :\n")
                    .append(getTabString(baseIndentation + 1)).append("(\n")
                    .append(String.join(",\n", ListIterate.collect(service.parameters, param -> renderServiceParameter(param, baseIndentation + 2))))
                    .append("\n")
                    .append(getTabString(baseIndentation + 1)).append(");\n");
        }
        builder.append(getTabString(baseIndentation + 1)).append("response : ").append(renderTypeReference(service.response)).append(";\n");
        builder.append(getTabString(baseIndentation + 1)).append("security : [")
                .append(String.join(",", ListIterate.collect(service.security, HelperServiceStoreGrammarComposer::renderAuthenticationStrategy)))
                .append("];\n");

        builder.append(getTabString(baseIndentation)).append(")\n");
    }

    private static String renderServiceParameter(ServiceParameter param, int baseIndentation)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(getTabString(baseIndentation))
                .append(renderServiceParameterName(param.name))
                .append(" : ")
                .append(renderTypeReference(param.type))
                .append(" ( ")
                .append("location = ")
                .append(param.location.toString().toLowerCase());

        if (param.serializationFormat != null && param.serializationFormat.style != null)
        {
            builder.append(", ").append("style = ").append(param.serializationFormat.style);
        }

        if (param.serializationFormat != null && param.serializationFormat.explode != null)
        {
            builder.append(", ").append("explode = ").append(param.serializationFormat.explode);
        }

        if (param.enumeration != null)
        {
            builder.append(", ").append("enum = ").append(param.enumeration);
        }

        if (param.allowReserved != null)
        {
            builder.append(", ").append("allowReserved = ").append(param.allowReserved);
        }

        if (param.required != null)
        {
            builder.append(", ").append("required = ").append(param.required);
        }

        builder.append(" )");

        return builder.toString();
    }

    private static String renderTypeReference(TypeReference typeReference)
    {
        StringBuilder builder = new StringBuilder();

        if (typeReference.list)
        {
            builder.append("[");
        }

        if (typeReference instanceof BooleanTypeReference)
        {
            builder.append("Boolean");
        }
        else if (typeReference instanceof FloatTypeReference)
        {
            builder.append("Float");
        }
        else if (typeReference instanceof IntegerTypeReference)
        {
            builder.append("Integer");
        }
        else if (typeReference instanceof StringTypeReference)
        {
            builder.append("String");
        }
        else if (typeReference instanceof ComplexTypeReference)
        {
            builder.append(((ComplexTypeReference) typeReference).type).append(" <- ").append(((ComplexTypeReference) typeReference).binding);
        }
        else
        {
            throw new EngineException("ServiceStore Composer does not support " + typeReference.getClass().getSimpleName() + " Parameter Value Type", SourceInformation.getUnknownSourceInformation(), EngineErrorType.PARSER);
        }

        if (typeReference.list)
        {
            builder.append("]");
        }

        return builder.toString();
    }

    private static String renderAuthenticationStrategy(SecurityScheme securityScheme)
    {
        List<Function<SecurityScheme, String>> processors = ListIterate.flatCollect(IServiceStoreGrammarComposerExtension.getExtensions(), ext -> ext.getExtraSecuritySchemesComposers());

        return ListIterate
                .collect(processors, processor -> processor.apply(securityScheme))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported SecurityScheme - " + securityScheme.getClass().getSimpleName(), securityScheme.sourceInformation, EngineErrorType.PARSER));
    }

    // -------------------------------------- CLASS MAPPING --------------------------------------

    public static void visitRootServiceClassMappingContents(RootServiceStoreClassMapping rootServiceStoreClassMapping, StringBuilder builder)
    {
        int baseIndentation = 2;
        ListIterate.forEach(rootServiceStoreClassMapping.localMappingProperties, prop -> visitLocalMappingProperty(prop, builder, baseIndentation));
        if (rootServiceStoreClassMapping.localMappingProperties != null && !rootServiceStoreClassMapping.localMappingProperties.isEmpty())
        {
            builder.append("\n");
        }
        ListIterate.forEach(rootServiceStoreClassMapping.servicesMapping, sm -> visitServiceMapping(sm, builder, baseIndentation));
    }

    private static void visitLocalMappingProperty(LocalMappingProperty localMappingProperty, StringBuilder builder, int baseIndentation)
    {
        builder.append(getTabString(baseIndentation))
                .append("+")
                .append(PureGrammarComposerUtility.convertIdentifier(localMappingProperty.name))
                .append(" : ")
                .append(localMappingProperty.type).append("[").append(HelperDomainGrammarComposer.renderMultiplicity(localMappingProperty.multiplicity)).append("]")
                .append(";\n");
    }

    private static void visitServiceMapping(ServiceMapping serviceMapping, StringBuilder builder, int baseIndentation)
    {
        builder.append(getTabString(baseIndentation)).append("~service ").append(renderServicePtr(serviceMapping.service)).append("\n");

        if ((serviceMapping.parameterMappings != null && !serviceMapping.parameterMappings.isEmpty()) || (serviceMapping.pathOffset != null && !(serviceMapping.pathOffset.path.isEmpty())))
        {
            List<ParameterIndexedParameterMapping> parameterIndexedParameterMappings = ListIterate.selectInstancesOf(serviceMapping.parameterMappings, ParameterIndexedParameterMapping.class);
            List<PropertyIndexedParameterMapping> propertyIndexedParameterMappings = ListIterate.selectInstancesOf(serviceMapping.parameterMappings, PropertyIndexedParameterMapping.class);
            builder.append(getTabString(baseIndentation)).append("(\n");

            if (serviceMapping.pathOffset != null && !(serviceMapping.pathOffset.path.isEmpty()))
            {
                builder.append(getTabString(baseIndentation + 1)).append("~path " + SERVICE_MAPPING_PATH_PREFIX + ".")
                        .append(ListAdapter.adapt(serviceMapping.pathOffset.path).collect(p -> HelperValueSpecificationGrammarComposer.renderPathElement(p, DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build())).makeString("."))
                        .append("\n");
            }

            if (!parameterIndexedParameterMappings.isEmpty())
            {
                builder.append(getTabString(baseIndentation + 1)).append("~paramMapping\n")
                        .append(getTabString(baseIndentation + 1)).append("(\n")
                        .append(String.join(",\n", ListIterate.collect(parameterIndexedParameterMappings, pm -> visitParameterIndexedParameterMapping(pm, baseIndentation + 2)))).append("\n")
                        .append(getTabString(baseIndentation + 1)).append(")\n");
            }

            if (!propertyIndexedParameterMappings.isEmpty())
            {
                builder.append(String.join(",\n", ListIterate.collect(propertyIndexedParameterMappings, pm -> visitPropertyIndexedParameterMapping(pm, baseIndentation + 1)))).append("\n");
            }

            builder.append(getTabString(baseIndentation)).append(")\n");
        }
    }

    private static String visitParameterIndexedParameterMapping(ParameterIndexedParameterMapping serviceParameterMapping, int baseIndentation)
    {
        return getTabString(baseIndentation) + renderServiceParameterName(serviceParameterMapping.serviceParameter) + " : " + serviceParameterMapping.transform.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build()).replaceFirst("\\|", "");
    }

    private static String visitPropertyIndexedParameterMapping(PropertyIndexedParameterMapping serviceParameterMapping, int baseIndentation)
    {
        return getTabString(baseIndentation) + PureGrammarComposerUtility.convertIdentifier(serviceParameterMapping.property) + " : $service.parameters." + renderServiceParameterName(serviceParameterMapping.serviceParameter);
    }

    private static String renderServiceParameterName(String serviceParameter)
    {
        return PureGrammarComposerUtility.convertIdentifier(serviceParameter, true);
    }

    private static String renderServicePtr(ServicePtr servicePtr)
    {
        return "[" + servicePtr.serviceStore + "] " + renderServicePath(servicePtr);
    }

    public static String renderServicePath(ServicePtr servicePtr)
    {
        return (servicePtr.parent == null ? "" : renderServiceGroupPath(servicePtr.parent) + ".") + servicePtr.service;
    }

    public static String renderServiceGroupPath(ServiceGroupPtr serviceGroupPtr)
    {
        if (serviceGroupPtr.parent == null)
        {
            return serviceGroupPtr.serviceGroup;
        }
        else
        {
            return renderServiceGroupPath(serviceGroupPtr.parent) + "." + serviceGroupPtr.serviceGroup;
        }
    }
}
