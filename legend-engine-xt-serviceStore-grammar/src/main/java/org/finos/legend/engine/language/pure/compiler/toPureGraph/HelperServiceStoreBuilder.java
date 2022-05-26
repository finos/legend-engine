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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.HelperServiceStoreGrammarComposer;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.BooleanTypeReference;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ComplexTypeReference;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.FloatTypeReference;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.IntegerTypeReference;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.Location;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecurityScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ServiceGroup;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ServiceGroupPtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ServiceParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ServicePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ServiceStore;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ServiceStoreElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.StringTypeReference;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.TypeReference;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_BooleanTypeReference_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_ComplexTypeReference;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_ComplexTypeReference_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_FloatTypeReference_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_IntegerTypeReference_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_SecurityScheme;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_SerializationFormat;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_SerializationFormat_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_Service_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_ServiceGroup;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_ServiceGroup_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_ServiceParameter;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_ServiceParameter_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_ServiceStore;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_ServiceStoreElement;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_StringTypeReference_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_TypeReference;
import org.finos.legend.pure.generated.core_pure_model_modelUnit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class HelperServiceStoreBuilder
{
    public static Root_meta_external_store_service_metamodel_ServiceStore getServiceStore(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        try
        {
            Store store = context.pureModel.getStore(fullPath, sourceInformation);
            if (store instanceof Root_meta_external_store_service_metamodel_ServiceStore)
            {
                return (Root_meta_external_store_service_metamodel_ServiceStore) store;
            }
            throw new RuntimeException("Store found but not a service");
        }
        catch (Exception e)
        {
            throw new EngineException("Can't find service '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    public static Root_meta_external_store_service_metamodel_Service getServiceFromServicePtr(ServicePtr servicePtr, CompileContext context)
    {
        SourceInformation sourceInformation = servicePtr.sourceInformation;
        try
        {
            Root_meta_external_store_service_metamodel_ServiceStore serviceStore = getServiceStore(servicePtr.serviceStore, sourceInformation, context);
            Root_meta_external_store_service_metamodel_ServiceStoreElement serviceStoreElement;
            if (servicePtr.parent == null)
            {
                serviceStoreElement = serviceStore._elements().detect(element -> element._id().equals(servicePtr.service));
            }
            else
            {
                Root_meta_external_store_service_metamodel_ServiceGroup parentServiceGroup = getServiceGroup(servicePtr.parent, serviceStore, sourceInformation);
                serviceStoreElement = parentServiceGroup._elements().detect(element -> element._id().equals(servicePtr.service));
            }

            if (serviceStoreElement == null)
            {
                throw new RuntimeException("Can't find service '" + HelperServiceStoreGrammarComposer.renderServicePath(servicePtr) + "' in Service Store : '" + servicePtr.serviceStore + "'");
            }
            if (serviceStoreElement instanceof Root_meta_external_store_service_metamodel_ServiceGroup)
            {
                throw new RuntimeException("Expected '[" + servicePtr.serviceStore + "]" + HelperServiceStoreGrammarComposer.renderServicePath(servicePtr) + "' to be service but found service group");
            }

            return (Root_meta_external_store_service_metamodel_Service) serviceStoreElement;
        }
        catch (Exception e)
        {
            throw new EngineException(e.getMessage(), sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private static Root_meta_external_store_service_metamodel_ServiceGroup getServiceGroup(ServiceGroupPtr serviceGroupPtr, Root_meta_external_store_service_metamodel_ServiceStore serviceStore, SourceInformation sourceInformation)
    {
        try
        {
            Root_meta_external_store_service_metamodel_ServiceStoreElement serviceStoreElement;
            if (serviceGroupPtr.parent == null)
            {
                serviceStoreElement = serviceStore._elements().detect(element -> element._id().equals(serviceGroupPtr.serviceGroup));
            }
            else
            {
                Root_meta_external_store_service_metamodel_ServiceGroup parentServiceGroup = getServiceGroup(serviceGroupPtr.parent, serviceStore, sourceInformation);
                serviceStoreElement = parentServiceGroup._elements().detect(element -> element._id().equals(serviceGroupPtr.serviceGroup));
            }

            if (serviceStoreElement == null)
            {
                throw new RuntimeException("Can't find service group '" + HelperServiceStoreGrammarComposer.renderServiceGroupPath(serviceGroupPtr) + "' in Service Store : '" + serviceGroupPtr.serviceStore + "'");
            }

            if (serviceStoreElement instanceof Root_meta_external_store_service_metamodel_Service)
            {
                throw new RuntimeException("Expected '[" + serviceGroupPtr.serviceStore + "]" + HelperServiceStoreGrammarComposer.renderServiceGroupPath(serviceGroupPtr) + "' to be service group but found service");
            }

            return (Root_meta_external_store_service_metamodel_ServiceGroup) serviceStoreElement;
        }
        catch (Exception e)
        {
            throw new EngineException(e.getMessage(), sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    public static void compileAndAddElementsToServiceStore(Root_meta_external_store_service_metamodel_ServiceStore pureServiceStore, ServiceStore serviceStore, CompileContext context)
    {
        pureServiceStore._elements(FastList.newList(compileServiceStoreElements(serviceStore.elements, pureServiceStore, null, context)).toImmutable());
    }

    private static List<Root_meta_external_store_service_metamodel_ServiceStoreElement> compileServiceStoreElements(List<ServiceStoreElement> elements, Root_meta_external_store_service_metamodel_ServiceStore owner, Root_meta_external_store_service_metamodel_ServiceGroup parent, CompileContext context)
    {
        return ListIterate.collect(elements, element ->
        {
            if (element instanceof ServiceGroup)
            {
                return compileServiceGroup((ServiceGroup) element, owner, parent, context);
            }
            else if (element instanceof Service)
            {
                return compileService((Service) element, owner, parent, context);
            }
            else
            {
                throw new EngineException("Unsupported Service Store Element type : " + element.getClass().getSimpleName(), element.sourceInformation, EngineErrorType.COMPILATION);
            }
        });
    }

    private static Root_meta_external_store_service_metamodel_ServiceGroup compileServiceGroup(ServiceGroup serviceGroup, Root_meta_external_store_service_metamodel_ServiceStore owner, Root_meta_external_store_service_metamodel_ServiceGroup parent, CompileContext context)
    {
        Root_meta_external_store_service_metamodel_ServiceGroup pureServiceGroup = new Root_meta_external_store_service_metamodel_ServiceGroup_Impl(serviceGroup.id);

        pureServiceGroup._owner(owner);
        if (parent != null)
        {
            pureServiceGroup._parent(parent);
        }

        pureServiceGroup._id(serviceGroup.id);
        pureServiceGroup._path(serviceGroup.path);

        pureServiceGroup._elements(FastList.newList(compileServiceStoreElements(serviceGroup.elements, owner, pureServiceGroup, context)).toImmutable());

        return pureServiceGroup;
    }

    private static Root_meta_external_store_service_metamodel_Service compileService(Service service, Root_meta_external_store_service_metamodel_ServiceStore owner, Root_meta_external_store_service_metamodel_ServiceGroup parent, CompileContext context)
    {
        Root_meta_external_store_service_metamodel_Service pureService = new Root_meta_external_store_service_metamodel_Service_Impl(service.id);

        pureService._owner(owner);
        if (parent != null)
        {
            pureService._parent(parent);
        }

        pureService._id(service.id);
        pureService._path(service.path);

        if (service.requestBody != null)
        {
            pureService._requestBody(compileTypeReference(service.requestBody, context));
        }
        pureService._method(context.pureModel.getEnumValue("meta::pure::functions::io::http::HTTPMethod", service.method.name()));
        if (service.parameters != null)
        {
            pureService._parameters(ListIterate.collect(service.parameters, param -> compileServiceParameter(param, context)));
        }
        pureService._response((Root_meta_external_store_service_metamodel_ComplexTypeReference) compileTypeReference(service.response, context));
        pureService._security(ListIterate.collect(service.security, HelperServiceStoreBuilder::compileSecurityScheme));

        RichIterable<String> parameters = pureService._parameters().collect(param -> param._name());
        List<String> parametersDefinedMoreThanOnce = parameters.select(e -> Collections.frequency(parameters.toList(), e) > 1).toSet().toList();

        if (!parametersDefinedMoreThanOnce.isEmpty())
        {
            throw new EngineException("Multiple definitions found for parameters : [" + String.join(",", parametersDefinedMoreThanOnce) + "].", service.sourceInformation, EngineErrorType.COMPILATION);
        }

        return pureService;
    }

    private static Root_meta_external_store_service_metamodel_ServiceParameter compileServiceParameter(ServiceParameter serviceParameter, CompileContext context)
    {
        validateServiceParameter(serviceParameter);

        Root_meta_external_store_service_metamodel_ServiceParameter pureServiceParameter = new Root_meta_external_store_service_metamodel_ServiceParameter_Impl(serviceParameter.name);

        pureServiceParameter._name(serviceParameter.name);
        pureServiceParameter._type(compileTypeReference(serviceParameter.type, context));
        pureServiceParameter._location(context.pureModel.getEnumValue("meta::external::store::service::metamodel::Location", serviceParameter.location.toString()));

        if (serviceParameter.enumeration != null)
        {
            pureServiceParameter._enum(context.resolveEnumeration(serviceParameter.enumeration));
        }

        if (serviceParameter.allowReserved != null)
        {
            pureServiceParameter._allowReserved(serviceParameter.allowReserved);
        }

        if (serviceParameter.required != null)
        {
            pureServiceParameter._required(serviceParameter.required);
        }
        else
        {
            pureServiceParameter._required(serviceParameter.location == Location.PATH);
        }

        if (serviceParameter.serializationFormat != null)
        {
            Root_meta_external_store_service_metamodel_SerializationFormat serializationFormat = new Root_meta_external_store_service_metamodel_SerializationFormat_Impl("");
            if (serviceParameter.serializationFormat.style != null)
            {
                serializationFormat._style(serviceParameter.serializationFormat.style);
            }
            if (serviceParameter.serializationFormat.explode != null)
            {
                serializationFormat._explode(serviceParameter.serializationFormat.explode);
            }
            pureServiceParameter._serializationFormat(serializationFormat);
        }

        return pureServiceParameter;
    }

    private static void validateServiceParameter(ServiceParameter serviceParameter)
    {
        List<String> bannedHeaderParamNames = FastList.newListWith("Accept", "Content-Type", "Authorization");
        if (serviceParameter.location == Location.HEADER && bannedHeaderParamNames.contains(serviceParameter.name))
        {
            throw new EngineException("Header parameters cannot have following names : [" + String.join(",", bannedHeaderParamNames) + "]", serviceParameter.sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private static Root_meta_external_store_service_metamodel_TypeReference compileTypeReference(TypeReference typeReference, CompileContext context)
    {
        Root_meta_external_store_service_metamodel_TypeReference pureTypeReference;

        if (typeReference instanceof BooleanTypeReference)
        {
            pureTypeReference = new Root_meta_external_store_service_metamodel_BooleanTypeReference_Impl("");
        }
        else if (typeReference instanceof FloatTypeReference)
        {
            pureTypeReference = new Root_meta_external_store_service_metamodel_FloatTypeReference_Impl("");
        }
        else if (typeReference instanceof IntegerTypeReference)
        {
            pureTypeReference = new Root_meta_external_store_service_metamodel_IntegerTypeReference_Impl("");
        }
        else if (typeReference instanceof StringTypeReference)
        {
            pureTypeReference = new Root_meta_external_store_service_metamodel_StringTypeReference_Impl("");
        }
        else if (typeReference instanceof ComplexTypeReference)
        {
            Root_meta_external_store_service_metamodel_ComplexTypeReference complexTypeReference = new Root_meta_external_store_service_metamodel_ComplexTypeReference_Impl("");
            Root_meta_external_shared_format_binding_Binding binding = HelperExternalFormat.getBinding(((ComplexTypeReference) typeReference).binding, context);
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass = context.resolveClass(((ComplexTypeReference) typeReference).type);

            if (!core_pure_model_modelUnit.Root_meta_pure_model_unit_resolve_ModelUnit_1__ResolvedModelUnit_1_(binding._modelUnit(), context.getExecutionSupport()).classes(context.getExecutionSupport()).contains(pureClass))
            {
                throw new EngineException("Class : " + ((ComplexTypeReference) typeReference).type + " should be included in modelUnit for binding : " + ((ComplexTypeReference) typeReference).binding, typeReference.sourceInformation, EngineErrorType.COMPILATION);
            }

            complexTypeReference._binding(binding);
            complexTypeReference._type(pureClass);
            pureTypeReference = complexTypeReference;
        }
        else
        {
            throw new EngineException("Unsupported Type Reference : " + typeReference.getClass().getSimpleName(), typeReference.sourceInformation, EngineErrorType.COMPILATION);
        }

        pureTypeReference._list(typeReference.list);
        return pureTypeReference;
    }

    private static Root_meta_external_store_service_metamodel_SecurityScheme compileSecurityScheme(SecurityScheme securityScheme)
    {
        List<Function<SecurityScheme, Root_meta_external_store_service_metamodel_SecurityScheme>> processors = ListIterate.flatCollect(IServiceStoreCompilerExtension.getExtensions(), ext -> ext.getExtraSecuritySchemeProcessors());

        return ListIterate
                .collect(processors, processor -> processor.apply(securityScheme))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported SecurityScheme - " + securityScheme.getClass().getSimpleName(), securityScheme.sourceInformation, EngineErrorType.COMPILATION));
    }
}
