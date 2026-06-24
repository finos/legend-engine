// Copyright 2020 Goldman Sachs
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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.utility.ListIterate;

import java.util.Collections;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.StoreProviderCompilerHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.TestBuilderHelper;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingEnumSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingIntegerSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingStringSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumerationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.LocalMappingPropertyInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateFunction;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateSetImplementationContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.GroupByFunction;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest_Legacy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.StoreTestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.modelJoin.ModelJoinAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.modelJoin.ModelJoinPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.xStore.XStoreAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.ObjectInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PurePropertyMapping;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_store_model_ModelStore_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;
import org.finos.legend.pure.generated.Root_meta_pure_data_StoreTestData;
import org.finos.legend.pure.generated.Root_meta_pure_data_StoreTestData_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_EnumValueMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_EnumerationMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_MappingClass_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_aggregationAware_AggregateSetImplementationContainer_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_aggregationAware_AggregateSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_aggregationAware_AggregationFunctionSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_aggregationAware_GroupByFunctionSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTest;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTestData;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTestData_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTestSuite;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTestSuite_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_modelJoin_ModelJoinAssociationImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_xStore_XStoreAssociationImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_LambdaFunction_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_property_Property_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relationship_Generalization_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregateSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationFunctionSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.GroupByFunctionSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelJoin.ModelJoinAssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.xStore.XStoreAssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.testable.Test;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.finos.legend.pure.generated.platform_dsl_mapping_functions_Mapping.Root_meta_pure_mapping__allClassMappingsRecursive_Mapping_1__SetImplementation_MANY_;

public class HelperMappingBuilder
{
    public static SetIterable<Mapping> getAllIncludedMappings(Mapping mapping)
    {
        return collectAllIncludedMappings(mapping, Sets.mutable.empty());
    }

    private static MutableSet<Mapping> collectAllIncludedMappings(Mapping mapping, MutableSet<Mapping> results)
    {
        if (results.add(mapping))
        {
            mapping._includes().forEach(include -> collectAllIncludedMappings(include._included(), results));
        }
        return results;
    }

    public static ImmutableSet<SetImplementation> getAllClassMappings(Mapping mapping)
    {
        return HelperMappingBuilder.getAllIncludedMappings(mapping).flatCollect(m -> (Iterable<SetImplementation>) m._classMappings()).toSet().toImmutable();
    }

    /**
     * Finds all InstanceSetImplementations in allClassMappings whose class equals or is a subtype of the given class.
     */
    public static MutableList<InstanceSetImplementation> findAllSetsForClassOrSubtypes(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type clazz, RichIterable<SetImplementation> allClassMappings, CompileContext context)
    {
        org.finos.legend.pure.m3.navigation.ProcessorSupport ps = context.pureModel.getExecutionSupport().getProcessorSupport();
        MutableList<InstanceSetImplementation> result = Lists.mutable.empty();
        for (SetImplementation c : allClassMappings)
        {
            if (c instanceof InstanceSetImplementation)
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type mappedClass = c._class();
                if (mappedClass.equals(clazz) || org.finos.legend.pure.m3.navigation.type.Type.subTypeOf((org.finos.legend.pure.m4.coreinstance.CoreInstance) mappedClass, (org.finos.legend.pure.m4.coreinstance.CoreInstance) clazz, ps))
                {
                    result.add((InstanceSetImplementation) c);
                }
            }
        }
        return result;
    }

    /**
     * Compiles a property mapping for all source×target pairs and adds results to the output list.
     */
    public static void compileForAllPairs(
            MutableList<InstanceSetImplementation> sourceSets,
            MutableList<InstanceSetImplementation> targetSets,
            java.util.function.BiFunction<InstanceSetImplementation, InstanceSetImplementation, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping> compiler,
            MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping> output)
    {
        for (InstanceSetImplementation sourceSet : sourceSets)
        {
            for (InstanceSetImplementation targetSet : targetSets)
            {
                output.add(compiler.apply(sourceSet, targetSet));
            }
        }
    }

    /**
     * Resolves a property from an association by name, handling milestoned edge-point properties.
     */
    public static Property<?, ?> resolveAssociationPropertyByName(Association association, String propertyName)
    {
        String edgePointName = org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions.getEdgePointPropertyName(propertyName);
        Property<?, ?> prop = association._properties().detect(p ->
                p._name().equals(propertyName) || p._name().equals(edgePointName));
        if (prop == null)
        {
            prop = association._originalMilestonedProperties().detect(p -> p._name().equals(propertyName));
        }
        return prop;
    }

    public static ImmutableSet<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping<Object>> getAllEnumerationMappings(Mapping mapping)
    {
        return HelperMappingBuilder.getAllIncludedMappings(mapping).flatCollect(m -> (Iterable<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping<Object>>) m._enumerationMappings(), Sets.mutable.empty()).toImmutable();
    }

    public static String getPropertyMappingTargetId(PropertyMapping pm)
    {
        return pm.target == null ? "" : pm.target;
    }

    public static String getPropertyMappingTargetId(PropertyMapping propertyMapping, Property<?, ?> property, CompileContext context)
    {
        if (propertyMapping.target == null && (property._genericType()._rawType() instanceof Class))
        {
            return HelperModelBuilder.getTypeFullPath(property._genericType()._rawType(), "_", context.pureModel.getExecutionSupport());
        }
        return HelperMappingBuilder.getPropertyMappingTargetId(propertyMapping);
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping<Object> processEnumMapping(EnumerationMapping em, Mapping pureMapping, CompileContext context)
    {
        // validate there is no mixed protocol format for source value
        List<Object> sourceValues = em.enumValueMappings.stream().flatMap(enumValueMapping -> enumValueMapping.sourceValues.stream()).collect(Collectors.toList());
        if (sourceValues.stream().anyMatch(sourceValue -> !(sourceValue instanceof EnumValueMappingSourceValue)) && sourceValues.stream().anyMatch(sourceValue -> sourceValue instanceof EnumValueMappingSourceValue))
        {
            throw new EngineException("Mixed formats for enum value mapping source values", em.sourceInformation, EngineErrorType.COMPILATION);
        }
        // process enum value mappings
        String id = getEnumerationMappingId(em);
        return new Root_meta_pure_mapping_EnumerationMapping_Impl<>(id, SourceInformationHelper.toM3SourceInformation(em.sourceInformation), null)
                ._classifierGenericType(context.newGenericType(context.pureModel.getType(M2MappingPaths.EnumerationMapping), Lists.mutable.with(context.newGenericType(context.pureModel.getType(M3Paths.Any)))))
                ._name(id)
                ._parent(pureMapping)
                ._enumeration(context.resolveEnumeration(em.enumeration.path, em.enumeration.sourceInformation))
                ._enumValueMappings(ListIterate.collect(em.enumValueMappings, v -> new Root_meta_pure_mapping_EnumValueMapping_Impl(null, SourceInformationHelper.toM3SourceInformation(v.sourceInformation), context.pureModel.getClass("meta::pure::mapping::EnumValueMapping"))
                        ._enum(context.resolveEnumValue(em.enumeration.path, v.enumValue))
                        ._sourceValues(convertSourceValues(em, v.sourceValues, context))
                ));
    }

    public static RichIterable<?> convertSourceValues(EnumerationMapping em, List<Object> sourceValues, CompileContext context)
    {
        // for v1_11_0 and later, we structure source value
        if (sourceValues.stream().allMatch(sourceValue -> sourceValue instanceof EnumValueMappingSourceValue))
        {
            return ListIterate.collect(sourceValues, o -> processStructuredSourceValue(o, context));
        }
        // for v1_5_0 and below, old format that has a structure for the sourceValue with type
        if (sourceValues.size() == 1 && sourceValues.get(0) instanceof Map)
        {
            return processSourceValuesWithTypeFlagOnEnumValueMapping((Map) sourceValues.get(0), context);
        }
        // for v1_10_0, we experimented with having `sourceType` on the enumeration mapping but that not only does not help
        // but even causes more trouble as it does not solve the fundamental problem
        if (em.sourceType != null)
        {
            return processSourceValuesWithSourceType(sourceValues, em.sourceType, context);
        }
        // for v1_6_0 to v1_9_0, we used `transformAny` function so source value can be anything String or Integer
        return ListIterate.collect(sourceValues, HelperMappingBuilder::processSimpleSourceValue);
    }

    /**
     * Process structured source value with type information embedded in each value
     * (>= v1_11_0)
     */
    private static Object processStructuredSourceValue(Object o, CompileContext context)
    {
        if (o instanceof EnumValueMappingIntegerSourceValue)
        {
            return Long.valueOf(((EnumValueMappingIntegerSourceValue) o).value);
        }
        else if (o instanceof EnumValueMappingStringSourceValue)
        {
            return ((EnumValueMappingStringSourceValue) o).value;
        }
        return context.resolveEnumValue(((EnumValueMappingEnumSourceValue) o).enumeration, ((EnumValueMappingEnumSourceValue) o).value);
    }

    /**
     * Try to convert OLD format source value to their appropriate type when possible
     * (>= v1_6_0 and <= v1_9_0)
     */
    private static Object processSimpleSourceValue(Object o)
    {
        if (o instanceof Integer)
        {
            // We use Long.valueOf() so that we can have a wider range of number
            return Long.valueOf((Integer) o);
        }
        return o;
    }

    /**
     * Support converting OLD format that has a structure for the sourceValue with type (<= v1_5_0)
     */
    private static RichIterable<?> processSourceValuesWithTypeFlagOnEnumValueMapping(Map map, CompileContext context)
    {
        String type = (String) map.get("_type");
        if ("string".equals(type))
        {
            return Lists.mutable.withAll((List) map.get("values"));
        }
        else if ("integer".equals(type))
        {
            // We use Long.valueOf() so that we can have a wider range of number
            return ListIterate.collect((List<Object>) map.get("values"), o -> Long.valueOf((Integer) o));
        }
        else if ("enumValue".equals(type))
        {
            return Lists.mutable.with(context.resolveEnumValue((String) map.get("fullPath"), (String) map.get("value")));
        }
        else if ("collection".equals(type))
        {
            return ListIterate.collect((List<Object>) map.get("values"), o -> processSourceValuesWithTypeFlagOnEnumValueMapping((Map) o, context));
        }
        throw new UnsupportedOperationException("Type '" + type + "' is not supported in enumeration mapping");
    }

    /**
     * Support converting OLD format that has source type (v1_10_0)
     */
    private static RichIterable<?> processSourceValuesWithSourceType(List<Object> sourceValues, String sourceType, CompileContext context)
    {
        if (sourceType.toUpperCase().equals("STRING"))
        {
            // String needs no processing
            return Lists.mutable.withAll(sourceValues);
        }
        else if (sourceType.toUpperCase().equals("INTEGER"))
        {
            return ListIterate.collect(sourceValues, o ->
            {
                // Since in the protocol we had SourceValues: String[*] for easy serialization, we need to anticipate
                // that we can receive "10" for value 10 but with the source type, we know how to process this to 10
                if (o instanceof String)
                {
                    return Long.valueOf((String) o);
                }
                else if (o instanceof Integer)
                {
                    return Long.valueOf((Integer) o);
                }
                return o;
            });
        }
        else
        {
            return ListIterate.collect(sourceValues, o -> context.resolveEnumValue(sourceType, (String) o));
        }
    }

    public static String getEnumerationMappingId(EnumerationMapping em)
    {
        return em.id != null ? em.id : em.enumeration.path.replaceAll("::", "_");
    }

    public static String getClassMappingId(ClassMapping cm, CompileContext context)
    {
        return cm.id != null ? cm.id : HelperModelBuilder.getElementFullPath(context.resolveClass(cm._class, cm.classSourceInformation), context.pureModel.getExecutionSupport()).replaceAll("::", "_");
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> processPurePropertyMappingTransform(PurePropertyMapping ppm, LambdaFunction lambda, PropertyMappingsImplementation owner, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type inputVarType, CompileContext context, String mappingName)
    {
        List<ValueSpecification> expressions = lambda.body;
        VariableExpression lambdaParam = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::VariableExpression"))
                ._name("src")
                ._multiplicity(context.pureModel.getMultiplicity("one"))
                ._genericType(context.newGenericType(inputVarType));
        MutableList<VariableExpression> pureParameters = Lists.mutable.with(lambdaParam);
        ProcessingContext ctx = new ProcessingContext("Pure M2M Transform Lambda");
        ctx.addInferredVariables("src", lambdaParam);
        MutableList<String> openVariables = Lists.mutable.empty();
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> valueSpecifications = ListIterate.collect(expressions, p -> p.accept(new ValueSpecificationBuilder(context, openVariables, ctx)));
        MutableList<String> cleanedOpenVariables = openVariables.distinct();
        cleanedOpenVariables.removeAll(pureParameters.collect(e -> e._name()));
        GenericType functionType = PureModel.buildFunctionType(pureParameters, valueSpecifications.getLast()._genericType(), valueSpecifications.getLast()._multiplicity(), context.pureModel);
        String propertyName = owner._id() + "." + ppm.property.property;
        ctx.flushVariable("src");
        return new Root_meta_pure_metamodel_function_LambdaFunction_Impl<Object>(propertyName, SourceInformationHelper.toM3SourceInformation(lambda.sourceInformation), null)
                ._classifierGenericType(context.newGenericType(context.pureModel.getType(M3Paths.LambdaFunction), Lists.mutable.with(functionType)))
                ._openVariables(cleanedOpenVariables)
                ._expressionSequence(valueSpecifications);
    }

    public static void collectPrerequisiteElementsFromPurePropertyMappingTransform(Set<PackageableElementPointer> prerequisiteElements, PurePropertyMapping ppm, CompileContext context)
    {
        ValueSpecificationPrerequisiteElementsPassBuilder valueSpecificationPrerequisiteElementsPassBuilder = new ValueSpecificationPrerequisiteElementsPassBuilder(context, prerequisiteElements);
        ListIterate.forEach(ppm.transform.body, p -> p.accept(valueSpecificationPrerequisiteElementsPassBuilder));
    }

    public static void processMappingTest(MappingTest_Legacy mappingTestLegacy, CompileContext context)
    {
        // todo hack to support legacy test flow
        // this is to ensure generics are set and prevent NPE
        // this assume a cache for types, and this prime the value
        context.resolveGenericType("meta::pure::mapping::Result")
                ._typeArguments(Lists.fixedSize.of(context.resolveGenericType("meta::pure::metamodel::type::Any")))
                ._multiplicityArguments(Lists.fixedSize.of(context.pureModel.getMultiplicity("zeromany")));

        HelperValueSpecificationBuilder.buildLambda(mappingTestLegacy.query, context);
        mappingTestLegacy.inputData.forEach(t -> HelperMappingBuilder.processMappingTestInputData(t, context));
    }

    public static void collectPrerequisiteElementsFromMappingTest(Set<PackageableElementPointer> prerequisiteElements, MappingTest_Legacy mappingTestLegacy, CompileContext context)
    {
        mappingTestLegacy.query.accept(new ValueSpecificationPrerequisiteElementsPassBuilder(context, prerequisiteElements));
        ListIterate.forEach(mappingTestLegacy.inputData, t -> HelperMappingBuilder.collectPrerequisiteElementsFromMappingTestInputData(prerequisiteElements, t, context));
    }

    public static void processMappingTestInputData(InputData inputData, CompileContext context)
    {
        if (inputData instanceof ObjectInputData)
        {
            ObjectInputData objectInputData = (ObjectInputData) inputData;
            if (objectInputData.inputType == null)
            {
                throw new EngineException("Object input data does not have a format type", objectInputData.sourceInformation, EngineErrorType.COMPILATION);
            }
            context.resolveClass(objectInputData.sourceClass, objectInputData.sourceInformation);
        }
        else
        {
            context.getCompilerExtensions().getExtraMappingTestInputDataProcessors().forEach(processor -> processor.value(inputData, context));
        }
    }

    public static void collectPrerequisiteElementsFromMappingTestInputData(Set<PackageableElementPointer> prerequisiteElements, InputData inputData, CompileContext context)
    {
        if (inputData instanceof ObjectInputData)
        {
            ObjectInputData objectInputData = (ObjectInputData) inputData;
            prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.CLASS, objectInputData.sourceClass, objectInputData.sourceInformation));
        }
        else
        {
            context.getCompilerExtensions().getExtraMappingTestInputDataPrerequisiteElementsPassProcessors().forEach(processor -> processor.value(inputData, prerequisiteElements));
        }
    }

    public static AssociationImplementation processAssociationImplementation(AssociationMapping associationMapping, CompileContext context, Mapping parentMapping)
    {
        if (associationMapping instanceof XStoreAssociationMapping)
        {
            XStoreAssociationMapping xStoreAssociationMapping = (XStoreAssociationMapping) associationMapping;
            XStoreAssociationImplementation base = new Root_meta_pure_mapping_xStore_XStoreAssociationImplementation_Impl("", SourceInformationHelper.toM3SourceInformation(associationMapping.sourceInformation), context.pureModel.getClass("meta::pure::mapping::xStore::XStoreAssociationImplementation"));
            final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association pureAssociation = context.resolveAssociation(xStoreAssociationMapping.association.path, xStoreAssociationMapping.association.sourceInformation);
            MutableList<Store> stores = ListIterate.collect(xStoreAssociationMapping.stores, context::resolveStore);
            final String id = associationMapping.id != null ? associationMapping.id : PackageableElement.getUserPathForPackageableElement(pureAssociation, "_");
            base._id(id)
                ._association(pureAssociation)
                ._stores(stores)._parent(parentMapping)
                ._propertyMappings(ListIterate.collect(xStoreAssociationMapping.propertyMappings, propertyMapping -> propertyMapping.accept(new PropertyMappingBuilder(context, parentMapping, base, HelperMappingBuilder.getAllClassMappings(parentMapping)))));
            return base;
        }
        else if (associationMapping instanceof ModelJoinAssociationMapping)
        {
            return processModelJoinAssociationMapping((ModelJoinAssociationMapping) associationMapping, context, parentMapping);
        }
        return context.getCompilerExtensions().getExtraAssociationMappingProcessors().stream()
                .map(processor -> processor.value(associationMapping, parentMapping, context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported association mapping type '" + associationMapping.getClass() + "'"));
    }

    public static void collectPrerequisiteElementsFromAssociationImplementation(Set<PackageableElementPointer> prerequisiteElements, AssociationMapping associationMapping, CompileContext context)
    {
        if (associationMapping instanceof XStoreAssociationMapping)
        {
            XStoreAssociationMapping xStoreAssociationMapping = (XStoreAssociationMapping) associationMapping;
            prerequisiteElements.add(xStoreAssociationMapping.association);
            prerequisiteElements.addAll(ListIterate.collect(xStoreAssociationMapping.stores, s -> new PackageableElementPointer(PackageableElementType.STORE, s)));
            PropertyMappingPrerequisiteElementsBuilder propertyMappingPrerequisiteElementsBuilder = new PropertyMappingPrerequisiteElementsBuilder(context, prerequisiteElements);
            ListIterate.forEach(xStoreAssociationMapping.propertyMappings, propertyMapping -> propertyMapping.accept(propertyMappingPrerequisiteElementsBuilder));
        }
        else if (associationMapping instanceof ModelJoinAssociationMapping)
        {
            ModelJoinAssociationMapping modelJoinMapping = (ModelJoinAssociationMapping) associationMapping;
            prerequisiteElements.add(modelJoinMapping.association);
            prerequisiteElements.addAll(ListIterate.collect(modelJoinMapping.stores, s -> new PackageableElementPointer(PackageableElementType.STORE, s)));
            if (modelJoinMapping.joinCondition != null && modelJoinMapping.joinCondition.body != null)
            {
                ValueSpecificationPrerequisiteElementsPassBuilder valueSpecificationPrerequisiteElementsPassBuilder = new ValueSpecificationPrerequisiteElementsPassBuilder(context, prerequisiteElements);
                ListIterate.forEach(modelJoinMapping.joinCondition.body, vs -> vs.accept(valueSpecificationPrerequisiteElementsPassBuilder));
            }
        }
        else
        {
            context.getCompilerExtensions().getExtraAssociationMappingPrerequisiteElementsPassProcessors().forEach(processor -> processor.value(associationMapping, prerequisiteElements));
        }
    }

    public static Root_meta_pure_mapping_MappingClass_Impl processMappingClass(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingClass mappingclass, CompileContext context, Mapping parent)
    {
        Root_meta_pure_mapping_MappingClass_Impl mappingClass = new Root_meta_pure_mapping_MappingClass_Impl<>(" ", SourceInformationHelper.toM3SourceInformation(mappingclass.sourceInformation), null);
        mappingClass._name(mappingclass.name);
        MutableList<Generalization> generalizations = ListIterate.collect(mappingclass.superTypes, (superTypePtr) ->
        {
            String superType = superTypePtr.path;
            Generalization generalization = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))._general(context.resolveGenericType(superType))._specific(mappingClass);
            context.resolveType(superType)._specializationsAdd(generalization);
            return generalization;
        });
        mappingClass._generalizations(generalizations);
        if (mappingclass.setImplementation != null)
        {
            mappingClass._setImplementation(mappingclass.setImplementation.accept(new ClassMappingFirstPassBuilder(context, parent)).getOne());
        }
        org.finos.legend.engine.protocol.pure.m3.type.Class rootClass = mappingclass.rootClass;
        if (rootClass != null)
        {
            context.processThirdPass(rootClass);
            mappingClass._class(context.pureModel.getClass(context.pureModel.buildPackageString(rootClass._package, rootClass.name), rootClass.sourceInformation));
        }
        return mappingClass;
    }

    public static void collectPrerequisiteElementsFromMappingClass(Set<PackageableElementPointer> prerequisiteElements, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingClass mappingClass, CompileContext context)
    {
        prerequisiteElements.addAll(mappingClass.superTypes);
        if (Objects.nonNull(mappingClass.setImplementation))
        {
            mappingClass.setImplementation.accept(new ClassMappingPrerequisiteElementsPassBuilder(context, prerequisiteElements));
        }
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregateSetImplementationContainer processAggregateSetImplementationContainer(AggregateSetImplementationContainer aggregateSetImplementationContainer, CompileContext context, Mapping parent)
    {
        if (aggregateSetImplementationContainer.setImplementation.mappingClass == null)
        {
            Class<?> _class = context.resolveClass(aggregateSetImplementationContainer.setImplementation._class, aggregateSetImplementationContainer.setImplementation.classSourceInformation);
            aggregateSetImplementationContainer.setImplementation.mappingClass = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingClass();
            aggregateSetImplementationContainer.setImplementation.mappingClass.name = _class.getName() + "_" + parent.getName() + "_" + aggregateSetImplementationContainer.setImplementation.id;
            aggregateSetImplementationContainer.setImplementation.mappingClass.superTypes = Lists.mutable.with(new PackageableElementPointer(HelperModelBuilder.getElementFullPath(_class, context.pureModel.getExecutionSupport())));
        }
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregateSetImplementationContainer container = new Root_meta_pure_mapping_aggregationAware_AggregateSetImplementationContainer_Impl("", null, context.pureModel.getClass("meta::pure::mapping::aggregationAware::AggregateSetImplementationContainer"));
        container._setImplementation((InstanceSetImplementation) aggregateSetImplementationContainer.setImplementation.accept(new ClassMappingFirstPassBuilder(context, parent)).getOne());
        container._index(aggregateSetImplementationContainer.index);
        container._aggregateSpecification(processAggregateSpecification(aggregateSetImplementationContainer.aggregateSpecification, context, Lists.mutable.empty(), aggregateSetImplementationContainer.setImplementation._class));
        return container;
    }

    public static void collectPrerequisiteElementsFromAggregateSetImplementationContainer(Set<PackageableElementPointer> prerequisiteElements, AggregateSetImplementationContainer aggregateSetImplementationContainer, CompileContext context)
    {
        prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.CLASS, aggregateSetImplementationContainer.setImplementation._class, aggregateSetImplementationContainer.setImplementation.classSourceInformation));
        aggregateSetImplementationContainer.setImplementation.accept(new ClassMappingPrerequisiteElementsPassBuilder(context, prerequisiteElements));
        HelperMappingBuilder.collectPrerequisiteElementsFromAggregateSpecification(prerequisiteElements, aggregateSetImplementationContainer.aggregateSpecification, context);
    }

    private static AggregateSpecification processAggregateSpecification(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateSpecification aggregateSpecification, CompileContext context, MutableList<String> openVariables, String parentClassPath)
    {
        ProcessingContext ctx = new ProcessingContext("Lambda");
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification thisVariable = HelperModelBuilder.createThisVariableForClass(context, parentClassPath);
        ctx.addInferredVariables("this", thisVariable);
        AggregateSpecification as = new Root_meta_pure_mapping_aggregationAware_AggregateSpecification_Impl(" ");
        as._canAggregate(aggregateSpecification.canAggregate);
        for (GroupByFunction gb : aggregateSpecification.groupByFunctions)
        {
            as._groupByFunctionsAdd(processGroupByFunction(gb, context, openVariables, ctx));
        }
        for (AggregateFunction af : aggregateSpecification.aggregateValues)
        {
            as._aggregateValuesAdd(processAggregationFunction(af, context, openVariables, ctx));
        }
        ctx.flushVariable("this");
        return as;
    }

    private static void collectPrerequisiteElementsFromAggregateSpecification(Set<PackageableElementPointer> prerequisiteElements, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateSpecification aggregateSpecification, CompileContext context)
    {
        ValueSpecificationPrerequisiteElementsPassBuilder valueSpecificationPrerequisiteElementsPassBuilder = new ValueSpecificationPrerequisiteElementsPassBuilder(context, prerequisiteElements);
        ListIterate.forEach(aggregateSpecification.groupByFunctions, gb -> gb.groupByFn.accept(valueSpecificationPrerequisiteElementsPassBuilder));
        ListIterate.forEach(aggregateSpecification.aggregateValues, af ->
        {
            af.mapFn.accept(valueSpecificationPrerequisiteElementsPassBuilder);
            af.aggregateFn.accept(valueSpecificationPrerequisiteElementsPassBuilder);
        });
    }

    private static GroupByFunctionSpecification processGroupByFunction(GroupByFunction groupByFunction, CompileContext context, MutableList<String> openVariables, ProcessingContext processingContext)
    {
        GroupByFunctionSpecification gb = new Root_meta_pure_mapping_aggregationAware_GroupByFunctionSpecification_Impl("", null, context.pureModel.getClass("meta::pure::mapping::aggregationAware::GroupByFunctionSpecification"));
        gb._groupByFn((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction) ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue) groupByFunction.groupByFn.accept(new ValueSpecificationBuilder(context, openVariables, processingContext)))._values().getFirst());
        return gb;
    }

    private static AggregationFunctionSpecification processAggregationFunction(AggregateFunction aggregateFunction, CompileContext context, MutableList<String> openVariables, ProcessingContext processingContext)
    {
        AggregationFunctionSpecification afs = new Root_meta_pure_mapping_aggregationAware_AggregationFunctionSpecification_Impl(" ");
        InstanceValue processed = (InstanceValue) aggregateFunction.mapFn.accept(new ValueSpecificationBuilder(context, openVariables, processingContext));
        afs._mapFn((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction) processed._values().getFirst());

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification thisVariable = HelperModelBuilder.createVariableForMapped((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction) processed._values().getFirst(), context);
        processingContext.addInferredVariables("mapped", thisVariable);

        if (aggregateFunction.aggregateFn.parameters.size() > 0)
        {
            Variable variable = aggregateFunction.aggregateFn.parameters.get(0);
            variable.genericType = context.convertGenericType(Handlers.funcReturnType(processed, context.pureModel));
            variable.multiplicity = new Multiplicity(1, 1);
        }
        afs._aggregateFn((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction) ((InstanceValue) aggregateFunction.aggregateFn.accept(new ValueSpecificationBuilder(context, openVariables, processingContext)))._values().getFirst());
        return afs;
    }

    public static Property getMappedProperty(PropertyMapping propertyMapping, CompileContext context)
    {
        if (propertyMapping.localMappingProperty != null)
        {
            // Local property mapping
            LocalMappingPropertyInfo localMappingPropertyInfo = propertyMapping.localMappingProperty;

            GenericType sourceGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass(M3Paths.GenericType)); // Raw type will be populated when mapping class is built
            GenericType targetGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass(M3Paths.GenericType))
                    ._rawType(context.resolveType(localMappingPropertyInfo.type, localMappingPropertyInfo.sourceInformation));
            GenericType propertyClassifierGenericType = context.newGenericType(
                    context.pureModel.getType(M3Paths.Property),
                    Lists.fixedSize.of(sourceGenericType, targetGenericType),
                    Lists.fixedSize.of(context.pureModel.getMultiplicity(localMappingPropertyInfo.multiplicity))
            );

            return new Root_meta_pure_metamodel_function_property_Property_Impl<>(propertyMapping.property.property, SourceInformationHelper.toM3SourceInformation(propertyMapping.sourceInformation), null)
                    ._name(propertyMapping.property.property)
                    ._classifierGenericType(propertyClassifierGenericType)
                    ._genericType(targetGenericType)
                    ._multiplicity(context.pureModel.getMultiplicity(localMappingPropertyInfo.multiplicity));
        }

        PropertyOwner owner = context.resolvePropertyOwner(propertyMapping.property._class, propertyMapping.property.sourceInformation);
        Class<?> _class = owner instanceof Class<?> ? (Class<?>) owner : HelperModelBuilder.getAssociationPropertyClass((Association) owner, propertyMapping.property.property, propertyMapping.property.sourceInformation, context);
        return HelperModelBuilder.getPropertyOrResolvedEdgePointProperty(context, _class, Optional.empty(), propertyMapping.property.property, propertyMapping.property.sourceInformation);
    }

    public static void collectPrerequisiteElementsFromMappedProperty(Set<PackageableElementPointer> prerequisiteElements, PropertyMapping propertyMapping)
    {
        if (propertyMapping.localMappingProperty != null)
        {
            prerequisiteElements.add(new PackageableElementPointer(null, propertyMapping.localMappingProperty.type, propertyMapping.localMappingProperty.sourceInformation));
        }
        else
        {
            prerequisiteElements.add(new PackageableElementPointer(null, propertyMapping.property._class, propertyMapping.property.sourceInformation));
            prerequisiteElements.add(new PackageableElementPointer(null, propertyMapping.property.property, propertyMapping.property.sourceInformation));
        }
    }

    public static void buildMappingClassOutOfLocalProperties(SetImplementation setImplementation, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping> propertyMappings, CompileContext context)
    {
        if (setImplementation instanceof InstanceSetImplementation && propertyMappings != null)
        {
            List<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping> localPropertyMappings = ListIterate.select(
                    propertyMappings.toList(),
                    x -> x._localMappingProperty() != null && x._localMappingProperty()
            );

            if (!localPropertyMappings.isEmpty())
            {
                String mappingClassName = setImplementation._class().getName() + "_" + setImplementation._parent().getName() + "_" + setImplementation._id();

                final MappingClass mappingClass = new Root_meta_pure_mapping_MappingClass_Impl<>(mappingClassName);
                mappingClass._name(mappingClassName);
                GenericType classifierGenericType = context.newGenericType(
                        context.pureModel.getType("meta::pure::mapping::MappingClass"),
                        Lists.fixedSize.of(context.newGenericType(mappingClass))
                );
                mappingClass._classifierGenericType(classifierGenericType);

                GenericType superType = context.newGenericType(setImplementation._class());
                Generalization newGeneralization = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, context.pureModel.getClass(M3Paths.Generalization))._specific(mappingClass)._general(superType);
                mappingClass._generalizations(Lists.immutable.with(newGeneralization));
                setImplementation._class()._specializationsAdd(newGeneralization);

                mappingClass._properties(ListIterate.collect(localPropertyMappings, pm ->
                {
                    Property property = pm._property();
                    property._owner(mappingClass);
                    property._classifierGenericType()._typeArguments().toList().get(0)._rawType(mappingClass);
                    return property;
                }));

                ((InstanceSetImplementation) setImplementation)._mappingClass(mappingClass);
            }
        }
    }


    public static Test processMappingTestAndTestSuite(org.finos.legend.engine.protocol.pure.v1.model.test.Test test, final org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping pureMapping, CompileContext context)
    {
        if (test instanceof MappingTestSuite)
        {
            // validate tests and test suite ids
            MappingTestSuite queryTestSuite = (MappingTestSuite) test;
            TestBuilderHelper.validateNonEmptySuite(queryTestSuite);
            TestBuilderHelper.validateTestIds(queryTestSuite.tests, queryTestSuite.sourceInformation);
            Root_meta_pure_mapping_metamodel_MappingTestSuite compiledMappingSuite = new Root_meta_pure_mapping_metamodel_MappingTestSuite_Impl("", SourceInformationHelper.toM3SourceInformation(test.sourceInformation), context.pureModel.getClass("meta::pure::mapping::metamodel::MappingTestSuite"));
            return compiledMappingSuite._id(queryTestSuite.id)
                    ._query(HelperValueSpecificationBuilder.buildLambda(queryTestSuite.func, context))
                    ._tests(ListIterate.collect(queryTestSuite.tests, unitTest -> (Root_meta_pure_test_AtomicTest) HelperMappingBuilder.processMappingTestAndTestSuite(unitTest, pureMapping, context)))
                    ._testable(pureMapping);
        }
        else if (test instanceof MappingTest)
        {
            MappingTest mappingTest = (MappingTest) test;
            Root_meta_pure_test_AtomicTest pureMappingTest = (Root_meta_pure_test_AtomicTest) TestCompilerHelper.compilePureMappingTests(mappingTest, context, new ProcessingContext("Mapping Test '" + mappingTest.id + "' Second Pass"));
            TestBuilderHelper.validateNonEmptyTest(mappingTest);
            TestBuilderHelper.validateAssertionIds(mappingTest.assertions, mappingTest.sourceInformation);
            pureMappingTest._assertions(ListIterate.collect(mappingTest.assertions, assertion -> context.getCompilerExtensions().getExtraTestAssertionProcessors().stream()
                    .map(processor -> processor.value(assertion, context, new ProcessingContext("Test Assertion '" + assertion.id + "'")))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new UnsupportedOperationException("No Processors found for assertion: " + assertion.id))));
            pureMappingTest._testable(pureMapping);
            return pureMappingTest;
        }
        else
        {
            return null;
        }
    }

    public static void collectPrerequisiteElementsFromMappingTestAndTestSuite(Set<PackageableElementPointer> prerequisiteElements, org.finos.legend.engine.protocol.pure.v1.model.test.Test test, CompileContext context)
    {
        if (test instanceof MappingTestSuite)
        {
            MappingTestSuite queryTestSuite = (MappingTestSuite) test;
            queryTestSuite.func.accept(new ValueSpecificationPrerequisiteElementsPassBuilder(context, prerequisiteElements));
            ListIterate.forEach(queryTestSuite.tests, unitTest -> HelperMappingBuilder.collectPrerequisiteElementsFromMappingTestAndTestSuite(prerequisiteElements, unitTest, context));
        }
        else if (test instanceof MappingTest)
        {
            MappingTest mappingTest = (MappingTest) test;
            TestCompilerHelper.collectPrerequisiteElementsFromPureMappingTests(prerequisiteElements, mappingTest, context);
            ListIterate.forEach(mappingTest.assertions, assertion -> context.getCompilerExtensions().getExtraTestAssertionPrerequisiteElementsPassProcessors().forEach(processor -> processor.value(prerequisiteElements, assertion, context)));
        }
    }

    static Root_meta_pure_mapping_metamodel_MappingTestData processMappingElementTestData(StoreTestData testData, CompileContext context, ProcessingContext processingContext)
    {
        Root_meta_pure_mapping_metamodel_MappingTestData mappingStoreTestData = new Root_meta_pure_mapping_metamodel_MappingTestData_Impl("", SourceInformationHelper.toM3SourceInformation(testData.sourceInformation), context.pureModel.getClass("meta::pure::mapping::metamodel::MappingTestData"));
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element;
        try
        {
            element = StoreProviderCompilerHelper.getStoreFromPackageableElementPointer(testData.store, context);
        }
        catch (Exception e)
        {
            element = context.resolvePackageableElement(testData.store);
        }
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement resolvedElement = element;
        Root_meta_pure_data_EmbeddedData metamodelData = context.getCompilerExtensions().getExtraEmbeddedDataProcessors().stream().map(processor -> processor.value(testData.data, context, processingContext))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported data"));
        Root_meta_pure_data_EmbeddedData resolvedData = context.getCompilerExtensions().getPackageableElementToEmbeddedDataProcessors().stream().map(processor -> processor.value(resolvedElement, metamodelData, context, true, testData.sourceInformation))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new EngineException("Unsupported relation accessor"));
        mappingStoreTestData._element(resolvedElement)._data(resolvedData);
        return mappingStoreTestData;
    }

    static void collectPrerequisiteElementsFromMappingElementTestData(Set<PackageableElementPointer> prerequisiteElements, StoreTestData testData, CompileContext context)
    {
        context.getCompilerExtensions().getExtraEmbeddedDataPrerequisiteElementsPassProcessors().forEach(processor -> processor.value(prerequisiteElements, testData.data, context));
        if (!testData.store.path.equals("ModelStore"))
        {
            prerequisiteElements.add(testData.store);
        }
    }

    static ImmutableList<Store> getStoresFromMappingIgnoringIncludedMappings(Mapping mapping, CompileContext context)
    {
        ImmutableList<String> mappedStores = getUniqueMapStorePathsFromMapping(mapping, context);
        return mappedStores.collect(store -> store.equals("ModelStore") ?
                new Root_meta_external_store_model_ModelStore_Impl("", null, context.pureModel.getClass("meta::external::store::model::ModelStore"))
                : context.resolveStore(store));
    }

    static ImmutableList<String> getUniqueMapStorePathsFromMapping(Mapping mapping, CompileContext context)
    {
        MutableSet<String> mappedStores = Sets.mutable.empty();
        ListIterate.forEach(Root_meta_pure_mapping__allClassMappingsRecursive_Mapping_1__SetImplementation_MANY_(mapping, context.pureModel.getExecutionSupport()).toList(), setImplementation ->
        {
            context.getCompilerExtensions().getExtraSetImplementationSourceScanners().forEach(scanner -> scanner.value(setImplementation, mappedStores, context));
        });
        return mappedStores.toList().toImmutable();
    }

    /**
     * Compiles a {@link ModelJoinAssociationMapping} into an {@link AssociationImplementation}.
     * <p>
     * The user supplies a single 2-parameter lambda (typed or untyped) describing how the two
     * association ends relate, e.g. {@code {firm:Firm[1], person:Person[1] | $firm.id == $person.firmId}}.
     * Compilation:
     * <ol>
     *   <li>Resolves the association and asserts it has exactly 2 properties.</li>
     *   <li>Pairs the lambda parameters with the two association properties (see
     *       {@link #resolveLambdaParamNames}).</li>
     *   <li>Rewrites the lambda body twice — once per direction — substituting the user's
     *       parameter references with the {@code _mj_src} / {@code _mj_tgt} sentinels the
     *       runtime expects (see {@link #MODEL_JOIN_SOURCE_VAR} / {@link #MODEL_JOIN_TARGET_VAR}).</li>
     *   <li>For each direction, materialises one compiled PropertyMapping per (source-set, target-set)
     *       pair across all class mappings of the participating classes (and their subtypes).</li>
     * </ol>
     */
    private static AssociationImplementation processModelJoinAssociationMapping(ModelJoinAssociationMapping modelJoinMapping, CompileContext context, Mapping parentMapping)
    {
        ModelJoinAssociationImplementation assocImpl = new Root_meta_pure_mapping_modelJoin_ModelJoinAssociationImplementation_Impl("", SourceInformationHelper.toM3SourceInformation(modelJoinMapping.sourceInformation), context.pureModel.getClass("meta::pure::mapping::modelJoin::ModelJoinAssociationImplementation"));
        final Association pureAssociation = context.resolveAssociation(modelJoinMapping.association.path, modelJoinMapping.association.sourceInformation);
        MutableList<Store> stores = ListIterate.collect(modelJoinMapping.stores, context::resolveStore);
        final String id = modelJoinMapping.id != null ? modelJoinMapping.id : HelperModelBuilder.getElementFullPath(pureAssociation, context.pureModel.getExecutionSupport()).replaceAll("::", "_");

        // ModelJoin only models binary associations; reject anything else up-front.
        RichIterable<? extends Property<?, ?>> associationProperties = pureAssociation._properties();
        if (associationProperties.size() != 2)
        {
            throw new EngineException("ModelJoin requires an association with exactly 2 properties", modelJoinMapping.sourceInformation, EngineErrorType.COMPILATION);
        }
        Property<?, ?> prop1 = associationProperties.toList().get(0);
        Property<?, ?> prop2 = associationProperties.toList().get(1);

        // Use pre-milestoning property names so the join condition references the names the user wrote.
        String rewriteProp1Name = resolveOriginalPropertyName(prop1, pureAssociation);
        String rewriteProp2Name = resolveOriginalPropertyName(prop2, pureAssociation);

        LambdaFunction joinCondition = modelJoinMapping.joinCondition;

        // One ModelJoinPropertyMapping per direction; the lambda body is rewritten so the
        // owning side of each direction binds to _mj_src and the navigated side to _mj_tgt.
        ModelJoinPropertyMapping pm1 = createModelJoinPropertyMapping(modelJoinMapping, rewriteProp1Name,
                rewriteModelJoinExpression(joinCondition, rewriteProp2Name, rewriteProp1Name, prop2, prop1, context));
        ModelJoinPropertyMapping pm2 = createModelJoinPropertyMapping(modelJoinMapping, rewriteProp2Name,
                rewriteModelJoinExpression(joinCondition, rewriteProp1Name, rewriteProp2Name, prop1, prop2, context));

        ImmutableSet<SetImplementation> allClassMappings = HelperMappingBuilder.getAllClassMappings(parentMapping);
        PropertyMappingBuilder builder = new PropertyMappingBuilder(context, parentMapping, assocImpl, allClassMappings);

        // Wire the association onto the implementation before delegating to PropertyMappingBuilder —
        // its visitor reads back getAssociationPropertyNames() during compilation.
        assocImpl._id(id)._association(pureAssociation)._stores(stores)._parent(parentMapping);

        // Expand every direction across the full Cartesian product of (source-class set × target-class
        // set). prop1 returns class1, so its OWNING side is class2 (and vice-versa for prop2).
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type class1 = prop1._genericType()._rawType();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type class2 = prop2._genericType()._rawType();
        MutableList<InstanceSetImplementation> class1Sets = findAllSetsForClassOrSubtypes(class1, allClassMappings, context);
        MutableList<InstanceSetImplementation> class2Sets = findAllSetsForClassOrSubtypes(class2, allClassMappings, context);

        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping> compiledMappings = Lists.mutable.empty();
        // pm1 navigates class2 → class1; pm2 navigates class1 → class2.
        compileForAllPairs(class2Sets, class1Sets, (s, t) -> builder.visitModelJoinPropertyMapping(pm1, s, t), compiledMappings);
        compileForAllPairs(class1Sets, class2Sets, (s, t) -> builder.visitModelJoinPropertyMapping(pm2, s, t), compiledMappings);

        assocImpl._propertyMappings(compiledMappings);
        return assocImpl;
    }

    private static String resolveOriginalPropertyName(Property<?, ?> prop, Association association)
    {
        RichIterable<? extends Property<?, ?>> originalMilestonedProps = association._originalMilestonedProperties();
        if (originalMilestonedProps != null)
        {
            for (Property<?, ?> origProp : originalMilestonedProps)
            {
                if (prop._genericType()._rawType().equals(origProp._genericType()._rawType()))
                {
                    return origProp._name();
                }
            }
        }
        return prop._name();
    }

    private static ModelJoinPropertyMapping createModelJoinPropertyMapping(ModelJoinAssociationMapping modelJoinMapping, String propertyName, LambdaFunction rewrittenCondition)
    {
        ModelJoinPropertyMapping pm = new ModelJoinPropertyMapping();
        pm.property = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyPointer();
        pm.property._class = modelJoinMapping.association.path;
        pm.property.property = propertyName;
        pm.source = "";
        pm.target = "";
        pm.joinCondition = rewrittenCondition;
        pm.sourceInformation = modelJoinMapping.sourceInformation;
        return pm;
    }

    /**
     * Rewrites a join-condition lambda body for one direction by substituting the user's
     * parameter references with the {@code _mj_src} / {@code _mj_tgt} sentinel variables that
     * downstream consumers (router, plan generator) bind to the source / target sets.
     * <p>
     * Accepts both typed ({@code {a:TypeA[1], b:TypeB[1] | ...}}) and untyped
     * ({@code {a, b | ...}}) 2-parameter lambdas; pairing rules live in
     * {@link #resolveLambdaParamNames}.
     */
    private static LambdaFunction rewriteModelJoinExpression(LambdaFunction original, String srcName, String tgtName, Property<?, ?> thisProp, Property<?, ?> thatProp, CompileContext context)
    {
        if (!isValidLambda(original))
        {
            throw new EngineException("ModelJoin requires a lambda with exactly 2 parameters, e.g. {a:TypeA[1], b:TypeB[1] | $a.prop == $b.prop} (param types optional)", original.sourceInformation, EngineErrorType.COMPILATION);
        }
        LambdaFunction result = new LambdaFunction();
        String[] resolved = resolveLambdaParamNames(original, srcName, tgtName, thisProp, thatProp, context);
        result.parameters = Collections.emptyList();
        result.body = ListIterate.collect(original.body, vs -> rewriteValueSpecification(vs, resolved[0], resolved[1]));
        return result;
    }

    private static boolean isValidLambda(LambdaFunction lambda)
    {
        return lambda.parameters != null && lambda.parameters.size() == 2;
    }

    /**
     * Pairs the two lambda parameters with the two association properties so the body can be
     * rewritten with one param standing in for $this (the owning side) and the other for $that.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>Type-based — requires both params to be typed and to match the two association
     *       property types unambiguously. Naturally skipped for self-associations (both
     *       property types equal → both orderings match → ambiguous).</li>
     *   <li>Name-based — pair by parameter name == association property name. Required
     *       fallback for self-associations and for untyped lambdas.</li>
     * </ol>
     * Returns [thisParamName, thatParamName].
     */
    private static String[] resolveLambdaParamNames(LambdaFunction lambda, String srcName, String tgtName, Property<?, ?> thisProp, Property<?, ?> thatProp, CompileContext context)
    {

        org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable param0 = lambda.parameters.get(0);
        org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable param1 = lambda.parameters.get(1);

        CoreInstance thisRawType = thisProp._genericType()._rawType();
        CoreInstance thatRawType = thatProp._genericType()._rawType();
        boolean isSelfAssociation = thisRawType.equals(thatRawType);

        // 1. Type-based pairing — only when both params are typed AND the association is not
        // self-referential (otherwise both orderings match and pairing is ambiguous).
        String param0Type = getVariableTypePath(param0);
        String param1Type = getVariableTypePath(param1);
        if (!isSelfAssociation && param0Type != null && param1Type != null)
        {
            ProcessorSupport ps = context.pureModel.getExecutionSupport().getProcessorSupport();
            CoreInstance param0CoreType = context.pureModel.getType(param0Type);
            CoreInstance param1CoreType = context.pureModel.getType(param1Type);
            boolean p0IsThis = org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(thisRawType, param0CoreType, ps);
            boolean p1IsThat = org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(thatRawType, param1CoreType, ps);
            boolean p0IsThat = org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(thatRawType, param0CoreType, ps);
            boolean p1IsThis = org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(thisRawType, param1CoreType, ps);
            if (p0IsThis && p1IsThat)
            {
                return new String[]{param0.name, param1.name};
            }
            if (p0IsThat && p1IsThis)
            {
                return new String[]{param1.name, param0.name};
            }
            // Types present but don't pair up — genuine type mismatch.
            throw new EngineException(
                    "ModelJoin lambda parameter types do not match association property types: expected one parameter of type '" + getTypeNameOrPlaceholder(thisProp) + "' and one of type '" + getTypeNameOrPlaceholder(thatProp) + "', got '" + param0.name + ":" + param0Type + "' and '" + param1.name + ":" + param1Type + "'",
                    lambda.sourceInformation, EngineErrorType.COMPILATION);
        }

        // 2. Name-based fallback — applies to self-associations and untyped lambdas.
        if (param0.name.equals(srcName) && param1.name.equals(tgtName))
        {
            return new String[]{param0.name, param1.name};
        }
        if (param0.name.equals(tgtName) && param1.name.equals(srcName))
        {
            return new String[]{param1.name, param0.name};
        }

        if (isSelfAssociation)
        {
            throw new EngineException(
                    "Self-association ModelJoin requires lambda parameter names to match the association property names ('" + srcName + "' and '" + tgtName + "'), got '" + param0.name + "' and '" + param1.name + "'",
                    lambda.sourceInformation, EngineErrorType.COMPILATION);
        }
        throw new EngineException(
                "Cannot pair ModelJoin lambda parameters '" + param0.name + "' and '" + param1.name + "' with association properties '" + srcName + "' and '" + tgtName + "'."
                        + " Either type the parameters explicitly (e.g. {" + param0.name + ":" + getTypeNameOrPlaceholder(thisProp) + "[1], " + param1.name + ":" + getTypeNameOrPlaceholder(thatProp) + "[1] | ...})"
                        + " or rename the parameters to match the association property names ('" + srcName + "' and '" + tgtName + "').",
                lambda.sourceInformation, EngineErrorType.COMPILATION);
    }

    private static String getTypeNameOrPlaceholder(Property<?, ?> prop)
    {
        CoreInstance rawType = prop._genericType()._rawType();
        if (rawType instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)
        {
            String name = ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) rawType)._name();
            return name != null ? name : "Type";
        }
        return "Type";
    }

    private static String getVariableTypePath(org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable var)
    {
        if (var.genericType != null && var.genericType.rawType instanceof org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType)
        {
            return ((org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType) var.genericType.rawType).fullPath;
        }
        return null;
    }

    private static org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification rewriteValueSpecification(
            org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification vs,
            String srcName, String tgtName)
    {
        if (vs instanceof org.finos.legend.engine.protocol.pure.m3.valuespecification.Collection)
        {
            org.finos.legend.engine.protocol.pure.m3.valuespecification.Collection collection = (org.finos.legend.engine.protocol.pure.m3.valuespecification.Collection) vs;
            org.finos.legend.engine.protocol.pure.m3.valuespecification.Collection result = new org.finos.legend.engine.protocol.pure.m3.valuespecification.Collection();
            result.sourceInformation = collection.sourceInformation;
            result.multiplicity = collection.multiplicity;
            result.values = collection.values != null ? ListIterate.collect(collection.values, p -> rewriteValueSpecification(p, srcName, tgtName)) : null;
            return result;
        }
        if (vs instanceof org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty)
        {
            org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty ap = (org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty) vs;
            if (ap.parameters != null && !ap.parameters.isEmpty())
            {
                String matchedVar = matchBaseToVariable(ap.parameters.get(0), srcName, tgtName);
                if (matchedVar != null)
                {
                    return createPropertyAccessOnVariable(matchedVar, ap.property, ap.sourceInformation);
                }
            }
            // Rewrite parameters recursively
            org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty result = new org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty();
            result.property = ap.property;
            result.sourceInformation = ap.sourceInformation;
            result.parameters = ap.parameters != null ? ListIterate.collect(ap.parameters, p -> rewriteValueSpecification(p, srcName, tgtName)) : null;
            return result;
        }
        else if (vs instanceof org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction)
        {
            org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction af = (org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction) vs;
            org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction result = new org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction();
            result.function = af.function;
            result.fControl = af.fControl;
            result.sourceInformation = af.sourceInformation;
            result.parameters = af.parameters != null ? ListIterate.collect(af.parameters, p -> rewriteValueSpecification(p, srcName, tgtName)) : null;
            return result;
        }
        else if (vs instanceof org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable)
        {
            org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable var = (org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable) vs;
            String mapped = mapVariableName(var.name, srcName, tgtName);
            if (mapped != null)
            {
                org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable result = new org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable();
                result.name = mapped;
                result.sourceInformation = var.sourceInformation;
                return result;
            }
        }
        return vs;
    }

    /**
     * If {@code base} is a variable bound to one of the user's lambda params, returns the
     * corresponding ModelJoin sentinel ({@link #MODEL_JOIN_SOURCE_VAR} or
     * {@link #MODEL_JOIN_TARGET_VAR}); otherwise returns {@code null}.
     */
    private static String matchBaseToVariable(org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification base, String srcName, String tgtName)
    {
        if (base instanceof org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable)
        {
            String name = ((org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable) base).name;
            return mapVariableName(name, srcName, tgtName);
        }
        return null;
    }

    public static final String MODEL_JOIN_SOURCE_VAR = "_mj_src";
    public static final String MODEL_JOIN_TARGET_VAR = "_mj_tgt";

    private static String mapVariableName(String name, String srcName, String tgtName)
    {
        if (srcName.equals(name))
        {
            return MODEL_JOIN_SOURCE_VAR;
        }
        else if (tgtName.equals(name))
        {
            return MODEL_JOIN_TARGET_VAR;
        }
        return null;
    }

    private static org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty createPropertyAccessOnVariable(
            String varName, String propertyName,
            org.finos.legend.engine.protocol.pure.m3.SourceInformation sourceInformation)
    {
        org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable variable = new org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable();
        variable.name = varName;
        variable.sourceInformation = sourceInformation;

        org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty result = new org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty();
        result.property = propertyName;
        result.sourceInformation = sourceInformation;
        result.parameters = Lists.mutable.with(variable);
        return result;
    }
}
