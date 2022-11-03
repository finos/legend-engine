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
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.HelperServiceStoreGrammarComposer;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.connection.ServiceStoreConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.*;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    public static void compileAndAddSecuritySchemesToServiceStore(Root_meta_external_store_service_metamodel_ServiceStore pureServiceStore, ServiceStore serviceStore, CompileContext context)
    {
        pureServiceStore._securitySchemes(new PureMap(compileServiceStoreSecuritySchemes(serviceStore.securitySchemes, pureServiceStore, null, context).stream().collect(Collectors.toMap(pair -> pair.getOne(), pair -> pair.getTwo()))));
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

    private static List<Pair<String,Root_meta_external_store_service_metamodel_SecurityScheme>> compileServiceStoreSecuritySchemes(List<SecurityScheme> securitySchemes, Root_meta_external_store_service_metamodel_ServiceStore pureServiceStore, Root_meta_external_store_service_metamodel_ServiceGroup parent, CompileContext context)
    {
        return ListIterate.collect(securitySchemes, scheme ->
        {
            if (scheme instanceof SimpleHttpSecurityScheme)
            {
                SimpleHttpSecurityScheme simpleHttpSecurityScheme = (SimpleHttpSecurityScheme) scheme;
                return Tuples.pair(simpleHttpSecurityScheme.id,
                        new Root_meta_external_store_service_metamodel_SimpleHttpSecurityScheme_Impl(simpleHttpSecurityScheme.id,null, context.pureModel.getClass("meta::external::store::service::metamodel::SimpleHttpSecurityScheme"))
                           ._scheme(simpleHttpSecurityScheme.scheme)
                           ._id(simpleHttpSecurityScheme.id));

            }
            else if (scheme instanceof ApiKeySecurityScheme)
            {
                ApiKeySecurityScheme apiKeySecurityScheme = (ApiKeySecurityScheme) scheme;
                return Tuples.pair(apiKeySecurityScheme.id,
                        new Root_meta_external_store_service_metamodel_ApiKeySecurityScheme_Impl(apiKeySecurityScheme.id, null, context.pureModel.getClass("meta::external::store::service::metamodel::ApiKeySecurityScheme"))
                           ._location(apiKeySecurityScheme.location)
                           ._keyName(apiKeySecurityScheme.keyName)
                           ._id(apiKeySecurityScheme.id));
            }
            else if (scheme instanceof OauthSecurityScheme)
            {
                OauthSecurityScheme oauthSecurityScheme = (OauthSecurityScheme) scheme;
                return Tuples.pair(oauthSecurityScheme.id,
                        new Root_meta_external_store_service_metamodel_OauthSecurityScheme_Impl(oauthSecurityScheme.id, null, context.pureModel.getClass("meta::external::store::service::metamodel::OauthSecurityScheme"))
                           ._scopesAddAll(Lists.mutable.withAll(oauthSecurityScheme.scopes))
                           ._id(oauthSecurityScheme.id));
            }
            else
            {
                throw new EngineException("Unsupported Security Scheme Type : " + scheme.getClass().getSimpleName(), scheme.sourceInformation, EngineErrorType.COMPILATION);
            }
        });
    }

    public static List<Pair<String, ? extends Root_meta_external_store_service_metamodel_runtime_AuthenticationTokenGenerationSpecification>> compileAuthentication(ServiceStoreConnection serviceStoreConnection, Root_meta_external_store_service_metamodel_runtime_ServiceStoreConnection pureServiceStoreConnection, CompileContext context)
    {
        return serviceStoreConnection.authSpecs.entrySet().stream().map(
                entry ->
        {
            Root_meta_external_store_service_metamodel_ServiceStore pureServiceStore = HelperServiceStoreBuilder.getServiceStore(serviceStoreConnection.element, serviceStoreConnection.elementSourceInformation, context);
            String securitySchemeId = entry.getKey();
            AuthenticationSpecification authSpec = entry.getValue();

            validateSecurityScheme(securitySchemeId,authSpec,pureServiceStore,serviceStoreConnection.sourceInformation);

            if (authSpec instanceof UsernamePasswordAuthentication)
            {
                UsernamePasswordAuthentication usernamePasswordAuthentication = (UsernamePasswordAuthentication) authSpec;
                return Tuples.pair(securitySchemeId,
                        new  Root_meta_external_store_service_metamodel_runtime_UsernamePasswordGenerationSpecification_Impl("")
                           ._username(usernamePasswordAuthentication.username)
                           ._password(usernamePasswordAuthentication.password));

            }
           else if (authSpec instanceof OAuthAuthentication)
           {
               OAuthAuthentication oAuthAuthentication = (OAuthAuthentication) authSpec;
               return Tuples.pair(securitySchemeId,
                       new Root_meta_external_store_service_metamodel_runtime_OauthTokenGenerationSpecification_Impl("")
                          //._grantType(context.pureModel.getEnumValue("meta::external::store::service::metamodel::runtime::OauthGrantType", oAuthAuthentication.grantType.toString()))
                                ._grantType(oAuthAuthentication.grantType)
                          ._clientId(oAuthAuthentication.clientId)
                          ._clientSecretVaultReference(oAuthAuthentication.clientSecretVaultReference)
                          ._authServerUrl(oAuthAuthentication.authServerUrl));
           }
           else if (authSpec instanceof ApiKeyAuthentication)
            {
                ApiKeyAuthentication apiKeyAuthentication = (ApiKeyAuthentication) authSpec;
                return Tuples.pair(securitySchemeId,
                        new Root_meta_external_store_service_metamodel_runtime_ApiKeySpecification_Impl("")
                                ._value(apiKeyAuthentication.value));
                               
            }
           else
           {
               throw new EngineException("Unsupported Auth Generation Specification Type : " + authSpec.getClass().getSimpleName(), null, EngineErrorType.COMPILATION);
           }
        }).collect(Collectors.toList());
    }

    private static void validateSecurityScheme(String id, AuthenticationSpecification authSpec, Root_meta_external_store_service_metamodel_ServiceStore pureServiceStore, SourceInformation sourceInformation)
    {
        Root_meta_external_store_service_metamodel_SecurityScheme_Impl securityScheme = (Root_meta_external_store_service_metamodel_SecurityScheme_Impl) pureServiceStore._securitySchemes().getMap().get(id);
        if(securityScheme == null)
        {
            throw new EngineException("Security Scheme not defined in ServiceStore: " + id, sourceInformation, EngineErrorType.COMPILATION);
        }

        if (securityScheme instanceof Root_meta_external_store_service_metamodel_SimpleHttpSecurityScheme_Impl)
        {
            if (!(authSpec instanceof UsernamePasswordAuthentication))
            {
                throw new EngineException("securityScheme-Authentication combination is not supported. Only supported combinations are \n [Http, UsernamePasswordAuthentication], [ApiKey, ApiKeySpecification], [Oauth, OauthAuthentication]",sourceInformation,EngineErrorType.COMPILATION);
            }
        }
        else if (securityScheme instanceof Root_meta_external_store_service_metamodel_ApiKeySecurityScheme_Impl)
        {
            if (!(authSpec instanceof ApiKeyAuthentication))
            {
                throw new EngineException("securityScheme-Authentication combination is not supported. Only supported combinations are \n [Http, UsernamePasswordAuthentication], [ApiKey, ApiKeySpecification], [Oauth, OauthAuthentication]",sourceInformation,EngineErrorType.COMPILATION);
            }
        }
        else if (securityScheme instanceof Root_meta_external_store_service_metamodel_OauthSecurityScheme_Impl)
        {
            if (!(authSpec instanceof OAuthAuthentication))
            {
                throw new EngineException("securityScheme-Authentication combination is not supported. Only supported combinations are \n [Http, UsernamePasswordAuthentication], [ApiKey, ApiKeySpecification], [Oauth, OauthAuthentication]",sourceInformation,EngineErrorType.COMPILATION);
            }
        }
        else
        {
            throw new EngineException("Unsupported Security Scheme type : " + id);
        }
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
        pureService._security(ListIterate.collect(service.security, scheme -> compileSecurityScheme(scheme,scheme.sourceInformation,context,owner)));

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
            Root_meta_external_store_service_metamodel_SerializationFormat serializationFormat = new Root_meta_external_store_service_metamodel_SerializationFormat_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::SerializationFormat"));
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
            pureTypeReference = new Root_meta_external_store_service_metamodel_BooleanTypeReference_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::BooleanTypeReference"));
        }
        else if (typeReference instanceof FloatTypeReference)
        {
            pureTypeReference = new Root_meta_external_store_service_metamodel_FloatTypeReference_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::FloatTypeReference"));
        }
        else if (typeReference instanceof IntegerTypeReference)
        {
            pureTypeReference = new Root_meta_external_store_service_metamodel_IntegerTypeReference_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::IntegerTypeReference"));
        }
        else if (typeReference instanceof StringTypeReference)
        {
            pureTypeReference = new Root_meta_external_store_service_metamodel_StringTypeReference_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::StringTypeReference"));
        }
        else if (typeReference instanceof ComplexTypeReference)
        {
            Root_meta_external_store_service_metamodel_ComplexTypeReference complexTypeReference = new Root_meta_external_store_service_metamodel_ComplexTypeReference_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::ComplexTypeReference"));
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

    private static Root_meta_external_store_service_metamodel_SecurityScheme compileSecurityScheme(SecurityScheme securityScheme, SourceInformation info, CompileContext context, Root_meta_external_store_service_metamodel_ServiceStore owner)
    {
       List<Function3<SecurityScheme,CompileContext,Root_meta_external_store_service_metamodel_ServiceStore,Root_meta_external_store_service_metamodel_SecurityScheme>> processors = ListIterate.flatCollect(IServiceStoreCompilerExtension.getExtensions(), ext -> ext.getExtraSecuritySchemeProcessors());

       return ListIterate
               .collect(processors,processor -> processor.value(securityScheme,context,owner))
               .select(Objects::nonNull)
               .getFirstOptional()
               .orElseThrow(() -> new EngineException("Can't find security scheme : " + ((IdentifiedSecurityScheme)securityScheme).id,info,EngineErrorType.COMPILATION));
    }
}
