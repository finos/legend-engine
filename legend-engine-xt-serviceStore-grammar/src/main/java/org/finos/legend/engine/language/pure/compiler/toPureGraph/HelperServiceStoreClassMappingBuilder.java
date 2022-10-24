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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping.RootServiceStoreClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping.ServiceMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping.ServiceRequestBodyBuildInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping.ServiceRequestBuildInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping.ServiceRequestParameterBuildInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping.ServiceRequestParametersBuildInfo;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.path.Path;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_BindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_SuccessfulBindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_BooleanTypeReference;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_ComplexTypeReference;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_FloatTypeReference;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_IntegerTypeReference;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_ServiceParameter;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_StringTypeReference;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_TypeReference;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_EmbeddedServiceStoreSetImplementation;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_EmbeddedServiceStoreSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_RootServiceInstanceSetImplementation;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_RootServiceInstanceSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_ServiceMapping;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_ServiceMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_ServiceRequestBodyBuildInfo;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_ServiceRequestBodyBuildInfo_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_ServiceRequestBuildInfo;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_ServiceRequestBuildInfo_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_ServiceRequestParameterBuildInfo;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_ServiceRequestParameterBuildInfo_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_ServiceRequestParametersBuildInfo;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_ServiceRequestParametersBuildInfo_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_ServiceStorePropertyMapping;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_mapping_ServiceStorePropertyMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_MappingClass_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_LambdaFunction_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_property_Property_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_path_Path_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relationship_Generalization_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl;
import org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;
import static org.finos.legend.pure.generated.platform_pure_corefunctions_meta.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_;

public class HelperServiceStoreClassMappingBuilder
{
    public static Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>> compileRootServiceStoreClassMapping(RootServiceStoreClassMapping serviceStoreClassMapping, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping parent, CompileContext context)
    {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass = context.resolveClass(serviceStoreClassMapping._class, serviceStoreClassMapping.classSourceInformation);
        String id = HelperMappingBuilder.getClassMappingId(serviceStoreClassMapping, context);

        Root_meta_external_store_service_metamodel_mapping_RootServiceInstanceSetImplementation res = new Root_meta_external_store_service_metamodel_mapping_RootServiceInstanceSetImplementation_Impl(id);
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass mappingClass = generateMappingClass(pureClass, id, serviceStoreClassMapping, parent, context);

        res._id(id);
        res._root(serviceStoreClassMapping.root);
        res._class(pureClass);
        res._parent(parent);
        res._mappingClass(mappingClass);

        res._localProperties(mappingClass._properties());
        res._servicesMapping(ListIterate.collect(serviceStoreClassMapping.servicesMapping, sm -> compileServiceMapping(sm, res, context)));

        validateRootServiceStoreClassMapping(res, serviceStoreClassMapping);

        MutableList<EmbeddedSetImplementation> embeddedSetImplementations = Lists.mutable.empty();
        Set<Class<?>> processedClasses = new HashSet<>();
        List<PropertyMapping> generatePropertyMappings = generatePropertyMappingsForClassMapping(res, embeddedSetImplementations, serviceStoreClassMapping, processedClasses, context);
        res._propertyMappings(FastList.newList(generatePropertyMappings).toImmutable());

        return Tuples.pair(res, embeddedSetImplementations);
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass generateMappingClass(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass, String id, RootServiceStoreClassMapping serviceStoreClassMapping, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping parent, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass mappingClass = new Root_meta_pure_mapping_MappingClass_Impl<>("");

        GenericType gType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                ._rawType(context.pureModel.getType("meta::pure::mapping::MappingClass"))
                ._typeArguments(Lists.mutable.with(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(mappingClass)));
        Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))
                ._specific(mappingClass)
                ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(pureClass));

        mappingClass._name(pureClass._name() + "_" + parent._name() + "_" + id);
        mappingClass._classifierGenericType(gType);
        mappingClass._generalizations(Lists.mutable.with(g));
        mappingClass._properties(ListIterate.collect(serviceStoreClassMapping.localMappingProperties, property ->
        {
            GenericType returnGenericType = context.resolveGenericType(property.type, property.sourceInformation);
            return new Root_meta_pure_metamodel_function_property_Property_Impl<>(property.name)
                    ._name(property.name)
                    ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(context.pureModel.getType("meta::pure::metamodel::function::property::Property"))._typeArguments(Lists.fixedSize.of(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(mappingClass), returnGenericType)))
                    ._genericType(returnGenericType)
                    ._multiplicity(context.pureModel.getMultiplicity(property.multiplicity))
                    ._owner(mappingClass);
        }));

        return mappingClass;
    }

    private static Root_meta_external_store_service_metamodel_mapping_ServiceMapping compileServiceMapping(ServiceMapping serviceMapping, Root_meta_external_store_service_metamodel_mapping_RootServiceInstanceSetImplementation owner, CompileContext context)
    {
        Root_meta_external_store_service_metamodel_mapping_ServiceMapping pureServiceMapping = new Root_meta_external_store_service_metamodel_mapping_ServiceMapping_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::mapping::ServiceMapping"));
        Root_meta_external_store_service_metamodel_Service service = HelperServiceStoreBuilder.getServiceFromServicePtr(serviceMapping.service, context);

        pureServiceMapping._owner(owner);
        pureServiceMapping._service(service);

        if (serviceMapping.pathOffset != null)
        {
            pureServiceMapping._pathOffset(compilePath(serviceMapping.pathOffset, service, context));
        }

        if (serviceMapping.requestBuildInfo != null)
        {
            pureServiceMapping._requestBuildInfo(compileRequestBuildInfo(serviceMapping.requestBuildInfo, owner, service, context));
        }

        validateServiceMapping(pureServiceMapping, owner._class(), serviceMapping.sourceInformation, context);
        return pureServiceMapping;
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.Path compilePath(Path pathOffset, Root_meta_external_store_service_metamodel_Service service, CompileContext ctx)
    {
        pathOffset.startType = getElementFullPath(service._response()._type(), ctx.pureModel.getExecutionSupport());

        Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl instanceValue = (Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl)new ValueSpecificationBuilder(ctx, Lists.mutable.empty(), new ProcessingContext("")).processClassInstance(pathOffset);

        return (Root_meta_pure_metamodel_path_Path_Impl) instanceValue._values.getOnly();
    }

    private static Root_meta_external_store_service_metamodel_mapping_ServiceRequestBuildInfo compileRequestBuildInfo(ServiceRequestBuildInfo requestBuildInfo, Root_meta_external_store_service_metamodel_mapping_RootServiceInstanceSetImplementation owner, Root_meta_external_store_service_metamodel_Service service, CompileContext context)
    {
        Root_meta_external_store_service_metamodel_mapping_ServiceRequestBuildInfo pureRequestBuildInfo = new Root_meta_external_store_service_metamodel_mapping_ServiceRequestBuildInfo_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::mapping::ServiceRequestBuildInfo"));

        if (requestBuildInfo.requestParametersBuildInfo != null)
        {
            pureRequestBuildInfo._requestParametersBuildInfo(compileServiceRequestParametersBuildInfo(requestBuildInfo.requestParametersBuildInfo, owner, service, context));
        }
        if (requestBuildInfo.requestBodyBuildInfo != null)
        {
            pureRequestBuildInfo._requestBodyBuildInfo(compileServiceRequestBodyBuildInfo(requestBuildInfo.requestBodyBuildInfo, owner, context));
        }

        return pureRequestBuildInfo;
    }

    private static Root_meta_external_store_service_metamodel_mapping_ServiceRequestParametersBuildInfo compileServiceRequestParametersBuildInfo(ServiceRequestParametersBuildInfo requestParametersBuildInfo, Root_meta_external_store_service_metamodel_mapping_RootServiceInstanceSetImplementation owner, Root_meta_external_store_service_metamodel_Service service, CompileContext context)
    {
        Root_meta_external_store_service_metamodel_mapping_ServiceRequestParametersBuildInfo pureRequestParametersBuildInfo = new Root_meta_external_store_service_metamodel_mapping_ServiceRequestParametersBuildInfo_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::mapping::ServiceRequestParametersBuildInfo"));

        pureRequestParametersBuildInfo._parameterBuildInfoList(ListIterate.collect(requestParametersBuildInfo.parameterBuildInfoList, parameterBuildInfo -> compileServiceRequestParameterBuildInfo(parameterBuildInfo, owner, service, context)));

        return pureRequestParametersBuildInfo;
    }

    private static Root_meta_external_store_service_metamodel_mapping_ServiceRequestParameterBuildInfo compileServiceRequestParameterBuildInfo(ServiceRequestParameterBuildInfo serviceRequestParameterBuildInfo, Root_meta_external_store_service_metamodel_mapping_RootServiceInstanceSetImplementation owner, Root_meta_external_store_service_metamodel_Service service, CompileContext context)
    {
        Root_meta_external_store_service_metamodel_mapping_ServiceRequestParameterBuildInfo pureRequestParameterBuildInfo = new Root_meta_external_store_service_metamodel_mapping_ServiceRequestParameterBuildInfo_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::mapping::ServiceRequestParameterBuildInfo"));

        Root_meta_external_store_service_metamodel_ServiceParameter parameter = service._parameters().detect(param -> param._name().equals(serviceRequestParameterBuildInfo.serviceParameter));
        if (parameter == null)
        {
            throw new EngineException("Service Parameter : '" + serviceRequestParameterBuildInfo.serviceParameter + "' is not valid", serviceRequestParameterBuildInfo.sourceInformation, EngineErrorType.COMPILATION);
        }
        pureRequestParameterBuildInfo._serviceParameter(parameter);
        pureRequestParameterBuildInfo._transform(processTransform(serviceRequestParameterBuildInfo.transform, owner._id() + "." + serviceRequestParameterBuildInfo.serviceParameter, owner, context));

        validateServiceParameterMapping(pureRequestParameterBuildInfo, context, serviceRequestParameterBuildInfo.sourceInformation);

        return pureRequestParameterBuildInfo;
    }

    private static Root_meta_external_store_service_metamodel_mapping_ServiceRequestBodyBuildInfo compileServiceRequestBodyBuildInfo(ServiceRequestBodyBuildInfo requestBodyBuildInfo, Root_meta_external_store_service_metamodel_mapping_RootServiceInstanceSetImplementation owner, CompileContext context)
    {
        Root_meta_external_store_service_metamodel_mapping_ServiceRequestBodyBuildInfo pureRequestBodyBuildInfo = new Root_meta_external_store_service_metamodel_mapping_ServiceRequestBodyBuildInfo_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::mapping::ServiceRequestBodyBuildInfo"));

        pureRequestBodyBuildInfo._transform(processTransform(requestBodyBuildInfo.transform, null, owner, context));

        return pureRequestBodyBuildInfo;
    }

    private static LambdaFunction processTransform(Lambda transform, String id, Root_meta_external_store_service_metamodel_mapping_RootServiceInstanceSetImplementation owner, CompileContext context)
    {
        List<ValueSpecification> expressions = transform.body;
        VariableExpression lambdaParam = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::VariableExpression"))
                ._name("this")
                ._multiplicity(context.pureModel.getMultiplicity("one"))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(owner._mappingClass()));
        MutableList<VariableExpression> pureParameters = Lists.mutable.with(lambdaParam);

        ProcessingContext ctx = new ProcessingContext("Service Store Transform");
        ctx.addInferredVariables("this", lambdaParam);
        MutableList<String> openVariables = Lists.mutable.empty();
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> valueSpecifications = ListIterate.collect(expressions, p -> p.accept(new ValueSpecificationBuilder(context, openVariables, ctx)));
        MutableList<String> cleanedOpenVariables = openVariables.distinct();
        cleanedOpenVariables.removeAll(pureParameters.collect(e -> e._name()));
        GenericType functionType = context.pureModel.buildFunctionType(pureParameters, valueSpecifications.getLast()._genericType(), valueSpecifications.getLast()._multiplicity(), context.pureModel);
        String mappingPath = Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_(owner._parent(), context.pureModel.getExecutionSupport()).replace("::", "_");
        ctx.flushVariable("src");
        return new Root_meta_pure_metamodel_function_LambdaFunction_Impl(id, new org.finos.legend.pure.m4.coreinstance.SourceInformation(mappingPath, 0, 0, 0, 0), null)
                ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(context.pureModel.getType("meta::pure::metamodel::function::LambdaFunction"))._typeArguments(FastList.newListWith(functionType)))
                ._openVariables(cleanedOpenVariables)
                ._expressionSequence(valueSpecifications);
    }

    private static List<PropertyMapping> generatePropertyMappingsForClassMapping(Root_meta_external_store_service_metamodel_mapping_RootServiceInstanceSetImplementation rootClassMapping, List<EmbeddedSetImplementation> embeddedSetImplementations, RootServiceStoreClassMapping serviceStoreClassMapping, Set<Class<?>> processedClasses, CompileContext context)
    {
        Root_meta_external_shared_format_binding_Binding binding = rootClassMapping._servicesMapping().getAny()._service()._response()._binding();

        ExternalFormatExtension schemaExtension = HelperExternalFormat.getExternalFormatExtension(binding);
        Root_meta_external_shared_format_binding_validation_BindingDetail bindingDetail = schemaExtension.bindDetails(binding, context);

        if (bindingDetail instanceof Root_meta_external_shared_format_binding_validation_SuccessfulBindingDetail)
        {
            return generatePropertyMappings((Root_meta_external_shared_format_binding_validation_SuccessfulBindingDetail) bindingDetail, rootClassMapping._class(), rootClassMapping._id(), embeddedSetImplementations, rootClassMapping, serviceStoreClassMapping.sourceInformation, processedClasses, context);
        }
        else
        {
            throw new EngineException("External format : '" + binding._contentType() + "' not yet supported with service store mapping", serviceStoreClassMapping.sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private static List<PropertyMapping> generatePropertyMappings(Root_meta_external_shared_format_binding_validation_SuccessfulBindingDetail bindingDetail, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass, String sourceSetId, List<EmbeddedSetImplementation> embeddedSetImplementations, PropertyMappingsImplementation owner, SourceInformation sourceInformation, Set<Class<?>> processedClasses, CompileContext context)
    {
        if (processedClasses.contains(pureClass))
        {
            throw new EngineException("Non serializable model mapped with Service Store Mapping", sourceInformation, EngineErrorType.COMPILATION);
        }
        processedClasses.add(pureClass);

        RichIterable<Property> properties = bindingDetail.mappedPropertiesForClass(pureClass, context.getExecutionSupport());

        RichIterable<Property> primitiveProperties = properties.select(prop -> core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_isPrimitiveValueProperty_AbstractProperty_1__Boolean_1_(prop, context.getExecutionSupport()));
        RichIterable<Property> nonPrimitiveProperties = properties.select(prop -> !core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_isPrimitiveValueProperty_AbstractProperty_1__Boolean_1_(prop, context.getExecutionSupport()));

        List<PropertyMapping> primitivePropertyMappings = primitiveProperties.collect(prop -> buildPrimitivePropertyMapping(prop, sourceSetId, context)).toList();
        List<PropertyMapping> nonPrimitivePropertyMappings = nonPrimitiveProperties.collect(prop -> buildNonPrimitivePropertyMapping(prop, sourceSetId, bindingDetail, owner._parent(), embeddedSetImplementations, owner, sourceInformation, new HashSet<>(processedClasses), context)).toList();

        List<PropertyMapping> allPropertyMapping = Lists.mutable.empty();
        allPropertyMapping.addAll(primitivePropertyMappings);
        allPropertyMapping.addAll(nonPrimitivePropertyMappings);

        return allPropertyMapping;
    }

    private static PropertyMapping buildPrimitivePropertyMapping(Property property, String sourceSetId, CompileContext context)
    {
        Root_meta_external_store_service_metamodel_mapping_ServiceStorePropertyMapping propertyMapping = new Root_meta_external_store_service_metamodel_mapping_ServiceStorePropertyMapping_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::mapping::ServiceStorePropertyMapping"));

        propertyMapping._property(property);
        propertyMapping._sourceSetImplementationId(sourceSetId);
        propertyMapping._targetSetImplementationId("");

        return propertyMapping;
    }

    private static PropertyMapping buildNonPrimitivePropertyMapping(Property property, String sourceSetId, Root_meta_external_shared_format_binding_validation_SuccessfulBindingDetail bindingDetail, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping parent, List<EmbeddedSetImplementation> embeddedSetImplementations, PropertyMappingsImplementation owner, SourceInformation sourceInformation, Set<Class<?>> processedClasses, CompileContext context)
    {
        Root_meta_external_store_service_metamodel_mapping_EmbeddedServiceStoreSetImplementation propertyMapping = new Root_meta_external_store_service_metamodel_mapping_EmbeddedServiceStoreSetImplementation_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::mapping::EmbeddedServiceStoreSetImplementation"));
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class) property._genericType()._rawType();
        String id = owner._id() + "_" + property._name();

        propertyMapping._class(pureClass);
        propertyMapping._id(id);
        propertyMapping._owner(owner);
        propertyMapping._parent(parent);
        propertyMapping._property(property);
        propertyMapping._root(false);
        propertyMapping._sourceSetImplementationId(sourceSetId);
        propertyMapping._targetSetImplementationId(id);

        propertyMapping._propertyMappings(FastList.newList(generatePropertyMappings(bindingDetail, pureClass, id, embeddedSetImplementations, propertyMapping, sourceInformation, new HashSet<>(processedClasses), context)).toImmutable());

        embeddedSetImplementations.add(propertyMapping);
        return propertyMapping;
    }

    private static void validateRootServiceStoreClassMapping(Root_meta_external_store_service_metamodel_mapping_RootServiceInstanceSetImplementation pureServiceStoreClassMapping, RootServiceStoreClassMapping serviceStoreClassMapping)
    {
        if (pureServiceStoreClassMapping._servicesMapping() == null || pureServiceStoreClassMapping._servicesMapping().size() == 0)
        {
            throw new EngineException("Atleast one service mapping required !!", serviceStoreClassMapping.sourceInformation, EngineErrorType.COMPILATION);
        }

        if (pureServiceStoreClassMapping._servicesMapping().collect(sm -> sm._service()._owner()).toSet().size() != 1)
        {
            throw new EngineException("All service mappings should use same store. Multiple stores found - " + pureServiceStoreClassMapping._servicesMapping().collect(sm -> sm._service()._owner()._name()).toSet().makeString("[", ",", "]"), serviceStoreClassMapping.sourceInformation, EngineErrorType.COMPILATION);
        }

        if (pureServiceStoreClassMapping._servicesMapping().collect(sm -> sm._service()._response()._binding()).toSet().size() != 1)
        {
            throw new EngineException("All service mappings should use same binding. Multiple bindings found - " + pureServiceStoreClassMapping._servicesMapping().collect(sm -> sm._service()._response()._binding()._name()).toSet().makeString("[", ",", "]"), serviceStoreClassMapping.sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private static void validateServiceMapping(Root_meta_external_store_service_metamodel_mapping_ServiceMapping serviceMapping, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass, SourceInformation sourceInformation, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type sourceDataType;
        if (serviceMapping._pathOffset() == null)
        {
            sourceDataType = serviceMapping._service()._response()._type();
        }
        else
        {
            sourceDataType = ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement) serviceMapping._pathOffset()._path().getLast())._property()._genericType()._rawType();
        }

        if (sourceDataType != pureClass)
        {
            throw new EngineException("Response type of source service should match mapping class. Found response type : " + getElementFullPath((PackageableElement) sourceDataType, context.pureModel.getExecutionSupport()) + " does not match mapping class : " + getElementFullPath(pureClass, context.pureModel.getExecutionSupport()), sourceInformation, EngineErrorType.COMPILATION);
        }

        RichIterable<String> requiredServiceParameters = serviceMapping._service()._parameters().collectIf(Root_meta_external_store_service_metamodel_ServiceParameter::_required, Root_meta_external_store_service_metamodel_ServiceParameter::_name);
        RichIterable<String> mappedParameters = (serviceMapping._requestBuildInfo() == null) || (serviceMapping._requestBuildInfo()._requestParametersBuildInfo() == null) ? FastList.newList() : serviceMapping._requestBuildInfo()._requestParametersBuildInfo()._parameterBuildInfoList().collect(pm -> pm._serviceParameter()._name());

        List<String> parametersMappedMoreThanOnce = mappedParameters.select(e -> Collections.frequency(mappedParameters.toList(), e) > 1).toSet().toList();

        if (!parametersMappedMoreThanOnce.isEmpty())
        {
            throw new EngineException("Multiple Mappings for same parameter not allowed. Multiple mappings found for parameters : [" + String.join(",", parametersMappedMoreThanOnce) + "].", sourceInformation, EngineErrorType.COMPILATION);
        }
        if (!mappedParameters.containsAll(requiredServiceParameters.toList()))
        {
            throw new EngineException("All required service parameters should be mapped. Required Service Parameters : [" + String.join(",", requiredServiceParameters) + "]. Mapped Parameters : [" + String.join(",", mappedParameters) + "].", sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private static void validateServiceParameterMapping(Root_meta_external_store_service_metamodel_mapping_ServiceRequestParameterBuildInfo requestParameterBuildInfo, CompileContext context, SourceInformation sourceInformation)
    {
        if (requestParameterBuildInfo._serviceParameter()._enum() != null)
        {
            throw new EngineException("Mapping enum service parameter is not yet supported !!", sourceInformation, EngineErrorType.COMPILATION);
        }

        if (!validateTransformWithServiceParameterType(requestParameterBuildInfo, context, sourceInformation))
        {
            throw new EngineException("Parameter Type is not compatible with transform type", sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private static boolean validateTransformWithServiceParameterType(Root_meta_external_store_service_metamodel_mapping_ServiceRequestParameterBuildInfo requestParameterBuildInfo, CompileContext context, SourceInformation sourceInformation)
    {
        Root_meta_external_store_service_metamodel_TypeReference typeReference = requestParameterBuildInfo._serviceParameter()._type();
        Type rawtype = requestParameterBuildInfo._transform()._expressionSequence().getLast()._genericType()._rawType();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity multiplicity = requestParameterBuildInfo._transform()._expressionSequence().getLast()._multiplicity();

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity parameterMultiplicty;
        if (typeReference._list())
        {
            parameterMultiplicty = context.pureModel.getMultiplicity("zeromany");
        }
        else
        {
            parameterMultiplicty = context.pureModel.getMultiplicity("one");
        }

        if (!org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.subsumes(parameterMultiplicty, multiplicity))
        {
            throw new EngineException("Parameter multiplicity is not compatible with transform multiplicity - Multiplicity error: " + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(parameterMultiplicty) + " doesn't subsumes " + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(multiplicity), sourceInformation, EngineErrorType.COMPILATION);
        }

        if (typeReference instanceof Root_meta_external_store_service_metamodel_StringTypeReference)
        {
            return (rawtype instanceof PrimitiveType) && "String".equals(rawtype._name());
        }
        else if (typeReference instanceof Root_meta_external_store_service_metamodel_IntegerTypeReference)
        {
            return (rawtype instanceof PrimitiveType) && "Integer".equals(rawtype._name());
        }
        else if (typeReference instanceof Root_meta_external_store_service_metamodel_FloatTypeReference)
        {
            return (rawtype instanceof PrimitiveType) && "Float".equals(rawtype._name());
        }
        else if (typeReference instanceof Root_meta_external_store_service_metamodel_BooleanTypeReference)
        {
            return (rawtype instanceof PrimitiveType) && "Boolean".equals(rawtype._name());
        }
        else if (typeReference instanceof Root_meta_external_store_service_metamodel_ComplexTypeReference)
        {
            HelperModelBuilder.checkTypeCompatibility(context, ((Root_meta_external_store_service_metamodel_ComplexTypeReference) typeReference)._type(), rawtype, "Parameter Type is not compatible with transform type", sourceInformation);
            return true;
        }
        else
        {
            throw new EngineException("Unable to infer type for service parameter : " + requestParameterBuildInfo._serviceParameter()._name(), sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private static ExternalFormatExtension getExtension(Root_meta_external_shared_format_binding_Binding binding)
    {
        return ExternalFormatExtensionLoader.extensions().values().stream()
                .filter(ext -> ext.getContentTypes().contains(binding._contentType()))
                .findFirst()
                .orElseThrow(() -> new EngineException("Unknown contentType '" + binding._contentType() + "'", SourceInformation.getUnknownSourceInformation(), EngineErrorType.COMPILATION));  // Should never reach here as binding should be compiled before
    }
}
