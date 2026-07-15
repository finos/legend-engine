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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.validator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.SourceInformationHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.Warning;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensions;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.IncludedMappingHandler;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionEmbeddedPropertyMapping;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;

import java.util.*;
import java.util.stream.Collectors;

public class MappingValidator
{
    public void validate(PureModel pureModel, PureModelContextData pureModelContextData, CompilerExtensions extensions)
    {
        // Create map of elements in V1 since when we try to resolve the generalization in M3, we will get stuffs like Annotated Element, PackageableElement
        // Also using V1 element is more convenient for extracting the source information purpose
        Map<String, Mapping> mappings = new LinkedHashMap<>(); // ensure we validate element in order they are inserted in the pure model context data
        Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> pureMappings = new LinkedHashMap<>();

        Map<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping>> mappingByClassMappingId = Maps.mutable.empty();
        LazyIterate.selectInstancesOf(pureModelContextData.getElements(), Mapping.class).forEach(mapping ->
        {
            String mappingPath = pureModel.buildPackageString(mapping._package, mapping.name);
            mappings.put(mappingPath, mapping);
            pureMappings.put(mappingPath, pureModel.getMapping(mappingPath, mapping.sourceInformation));
        });
        this.validateGeneralization(pureModel, mappings);
        this.validateEnumerationMappings(pureModel, mappings);
        this.validateMappingElementIds(pureModel, mappings, pureMappings, mappingByClassMappingId);
        this.validateRelationFunctionClassMapping(pureModel, pureMappings, mappings);

        MappingValidatorContext mappingValidatorContext = new MappingValidatorContext(mappingByClassMappingId, pureMappings, mappings);
        extensions.getExtraMappingPostValidators().forEach(v -> v.accept(pureModel, mappingValidatorContext));
    }


    public void validate(PureModel pureModel, PureModelContextData pureModelContextData)
    {
        validate(pureModel, pureModelContextData, null);
    }

    private void visitMappingInclude(Mapping mapping, PureModel pureModel, Map<String, Mapping> mappings, Set<Mapping> visited, Set<Mapping> discovered)
    {
        discovered.add(mapping);
        RichIterable<? extends MappingInclude> mappingIncludes = pureModel.getContext().resolveMapping(pureModel.buildPackageString(mapping._package,
                mapping.name))._includes();
        mappingIncludes.forEach(mappingInclude ->
        {
            String underlyingMappingName = IncludedMappingHandler.parseIncludedMappingNameRecursively(mappingInclude);
            Mapping includedMapping = mappings.get(underlyingMappingName);
            if (includedMapping != null)
            {
                if (discovered.contains(includedMapping))
                {
                    throw new EngineException(pureModel.buildPackageString(mapping._package, mapping.name) + " -> " + pureModel.buildPackageString(includedMapping._package, includedMapping.name));
                }
                else if (!visited.contains(includedMapping))
                {
                    try
                    {
                        this.visitMappingInclude(includedMapping, pureModel, mappings, visited, discovered);
                    }
                    catch (Exception e)
                    {
                        throw new EngineException(pureModel.buildPackageString(mapping._package, mapping.name) + " -> " + e.getMessage());
                    }
                }
            }
            // NOTE: if the included mapping is not the map, it is either in system or it
            // belongs to another project, and by right, we should ensure there is not circular dependency in project
            // dependency chain, so we don't have to check that case.
            // For the case the included mapping does not exist, other compilation flows would catch that error anyway
        });
        discovered.remove(mapping);
        visited.add(mapping);
    }

    /**
     * Validate if there is a cycle in mapping inclusion
     */
    public void validateGeneralization(PureModel pureModel, Map<String, Mapping> mappings)
    {
        Set<Mapping> visitedMappings = new HashSet<>();
        Set<Mapping> discoveredMappings = new HashSet<>();
        mappings.values().forEach(mapping ->
        {
            if (!visitedMappings.contains(mapping) || !discoveredMappings.contains(mapping))
            {
                try
                {
                    this.visitMappingInclude(mapping, pureModel, mappings, visitedMappings, discoveredMappings);
                }
                catch (Exception e)
                {
                    throw new EngineException("Cycle detected in mapping include hierarchy: " + e.getMessage(), mapping.sourceInformation, EngineErrorType.COMPILATION);
                }
            }
        });
    }


    /**
     * Validate that for each enumeration mapping, the mapped value all of their enum values are consistent with the source type
     * NOTE: we tried to take the validator function in Pure since with it, we can also validate older format of enumeration mapping
     * but that involves more work on the Pure side (some methods are not supported in compiled mode), so we put a hold on that
     * and have this simple method which only validate for newer format of enumeration mapping where source type is available
     */
    public void validateEnumerationMappings(PureModel pureModel, Map<String, Mapping> mappings)
    {
        mappings.forEach((mappingPath, mapping) ->
        {
            mapping.enumerationMappings.forEach(enumerationMapping ->
            {
                Set<Class<?>> foundSourceTypes = new HashSet<>();
                List<Object> sourceValues = enumerationMapping.enumValueMappings.stream().flatMap(enumValueMapping -> enumValueMapping.sourceValues.stream()).collect(Collectors.toList());
                if (sourceValues.stream().allMatch(sourceValue -> sourceValue instanceof EnumValueMappingSourceValue))
                {
                    sourceValues.forEach(sourceValue -> foundSourceTypes.add(sourceValue.getClass()));
                }
                // TODO: we might need to do validation for older format
                if (foundSourceTypes.size() > 1)
                {
                    throw new EngineException("Only one type of source value (integer, string or an enum) is allowed for enumeration mapping", enumerationMapping.sourceInformation, EngineErrorType.COMPILATION);
                }
            });
        });
    }

    private void collectAndValidateClassMappingIds(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> mappingByClassMappingId, Set<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> visitedMappings)
    {
        if (!visitedMappings.contains(mapping))
        {
            mapping._includes().each(mappingInclude ->
            {
                this.collectAndValidateClassMappingIds(mappingInclude._included(), mappingByClassMappingId, visitedMappings);
            });
            Set<String> ownedClassMappingIds = new HashSet<>();
            mapping._classMappings().select(classMapping -> !(classMapping instanceof EmbeddedSetImplementation)).each(classMapping ->
            {
                // check ID duplication across mappings
                if (mappingByClassMappingId.get(classMapping._id()) != null)
                {
                    if (mappingByClassMappingId.get(classMapping._id()) != mapping)
                    {
                        // only throw if the ID is already associated but with another mapping
                        throw new EngineException(classMapping._id());
                    }
                }
                else
                {
                    mappingByClassMappingId.put(classMapping._id(), mapping);
                }
                // check ID duplication within mapping
                if (!ownedClassMappingIds.add(classMapping._id()))
                {
                    throw new EngineException(classMapping._id());
                }
            });
            visitedMappings.add(mapping);
        }
    }

    private void collectAndValidateEnumerationMappingIds(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> mappingByEnumerationMappingId, Set<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> visitedMappings)
    {
        if (!visitedMappings.contains(mapping))
        {
            mapping._includes().each(mappingInclude ->
            {
                this.collectAndValidateEnumerationMappingIds(mappingInclude._included(), mappingByEnumerationMappingId, visitedMappings);
            });
            Set<String> ownedEnumerationMappingIds = new HashSet<>();
            mapping._enumerationMappings().each(enumerationMapping ->
            {
                // check ID duplication across mappings
                if (mappingByEnumerationMappingId.get(enumerationMapping._name()) != null)
                {
                    if (mappingByEnumerationMappingId.get(enumerationMapping._name()) != mapping)
                    {
                        // only throw if the ID is already associated but with another mapping
                        throw new EngineException(enumerationMapping._name());
                    }
                }
                else
                {
                    mappingByEnumerationMappingId.put(enumerationMapping._name(), mapping);
                }
                // check ID duplication within mapping
                if (!ownedEnumerationMappingIds.add(enumerationMapping._name()))
                {
                    throw new EngineException(enumerationMapping._name());
                }
            });
            visitedMappings.add(mapping);
        }
    }


    public void validateMappingElementIds(PureModel pureModel, Map<String, Mapping> mappings, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> pureMappings, Map<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping>> mappingByClassMappingId)
    {
        pureMappings.forEach((mappingPath, mapping) ->
        {
            try
            {
                Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> localMappingByID = Maps.mutable.empty();

                collectAndValidateClassMappingIds(mapping, localMappingByID, new HashSet<>());
                validatePropertyMappings(pureModel, mapping, localMappingByID);

                mappingByClassMappingId.put(mapping, localMappingByID);
            }
            catch (EngineException e)
            {
                throw new EngineException("Duplicated class mappings found with ID '" + e.getMessage() + "' in mapping '" + mappingPath + "'", mappings.get(mappingPath).sourceInformation, EngineErrorType.COMPILATION);
            }
            try
            {
                collectAndValidateEnumerationMappingIds(mapping, new HashMap<>(), new HashSet<>());
            }
            catch (EngineException e)
            {
                throw new EngineException("Duplicated enumeration mappings found with ID '" + e.getMessage() + "' in mapping '" + mappingPath + "'", mappings.get(mappingPath).sourceInformation, EngineErrorType.COMPILATION);
            }
        });
    }


    private void validatePropertyMappings(PureModel pureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> mappingByClassMappingId)
    {
        Set<String> ids = mappingByClassMappingId.keySet();
        pureModel.addDefects(mapping._classMappings().flatCollect(cm ->
        {
            if (cm instanceof PropertyMappingsImplementation && !(cm instanceof EmbeddedSetImplementation))
            {
                return ((PropertyMappingsImplementation) cm)._propertyMappings().select(p -> !(p instanceof EmbeddedSetImplementation)).flatCollect(pm ->
                        Lists.mutable.withAll(checkId(pureModel, mapping, ids, pm, pm._sourceSetImplementationId()))
                                .withAll(checkId(pureModel, mapping, ids, pm, pm._targetSetImplementationId()))
                );
            }
            else
            {
                return Lists.mutable.empty();
            }
        }));
    }

    private Iterable<Warning> checkId(PureModel pureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, Set<String> ids, PropertyMapping pm, String id)
    {
        if (!"".equals(id) && !ids.contains(id))
        {
            return Lists.mutable.with(new Warning(SourceInformationHelper.fromM3SourceInformation(pm.getSourceInformation()), "Error '" + id + "' can't be found in the mapping " + pureModel.buildPackageString(mapping._package()._name(), mapping._name())));
        }
        return Lists.mutable.empty();
    }

    public void validateRelationFunctionClassMapping(PureModel pureModel, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> pureMappings, Map<String, Mapping> protocolMappings)
    {
        protocolMappings.forEach((mappingPath, protocolMapping) ->
        {
            if (protocolMapping.classMappings != null)
            {
                Set<String> classMappingIds = protocolMapping.classMappings.stream()
                        .filter(cm -> cm instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping)
                        .map(cm -> cm.id != null ? cm.id : cm._class.replace("::", "_"))
                        .collect(Collectors.toSet());
                protocolMapping.classMappings.stream()
                        .filter(cm -> cm instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping)
                        .forEach(cm ->
                        {
                            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping rfcm = (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping) cm;
                            if (rfcm.propertyMappings != null)
                            {
                                rfcm.propertyMappings.stream()
                                        .filter(pm -> pm instanceof RelationFunctionEmbeddedPropertyMapping)
                                        .map(pm -> (RelationFunctionEmbeddedPropertyMapping) pm)
                                        .forEach(pm ->
                                        {
                                            if (pm.id != null && !pm.id.isEmpty() && (pm.propertyMappings == null || pm.propertyMappings.isEmpty()) && !classMappingIds.contains(pm.id))
                                            {
                                                throw new EngineException("The set implementation '" + pm.id + "' referenced in the inline embedded mapping for property '" + pm.property.property + "' does not exist in the mapping " + mappingPath, pm.sourceInformation, EngineErrorType.COMPILATION);
                                            }
                                        });
                            }
                        });
            }
        });

        pureMappings.forEach((mappingPath, mapping) ->
                mapping._classMappings().select(classMapping -> (classMapping instanceof RelationFunctionInstanceSetImplementation)).each(classMapping ->
                {
                    RelationFunctionInstanceSetImplementation relationClassMapping = (RelationFunctionInstanceSetImplementation) classMapping;
                    FunctionDefinition<?> relationFunction = relationClassMapping._relationFunction();
                    ProcessorSupport processorSupport = pureModel.getExecutionSupport().getProcessorSupport();
                    FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(relationFunction);
                    if (!functionType._parameters().isEmpty())
                    {
                        throw new EngineException("Relation mapping function expecting arguments is not supported!", SourceInformationHelper.fromM3SourceInformation(relationFunction.getSourceInformation()), EngineErrorType.COMPILATION);
                    }
                    if (!processorSupport.type_subTypeOf(functionType._returnType()._rawType(), processorSupport.package_getByUserPath(M3Paths.Relation)))
                    {
                        throw new EngineException("Relation mapping function should return a Relation! Found a " + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(functionType._returnType(), processorSupport) + " instead.", SourceInformationHelper.fromM3SourceInformation(relationFunction.getSourceInformation()), EngineErrorType.COMPILATION);
                    }
                    GenericType lastExpressionType = relationFunction._expressionSequence().toList().getLast()._genericType();
                    RelationType<?> relationType = (RelationType<?>) lastExpressionType._typeArguments().toList().getFirst()._rawType();
                    Set<String> relationFunctionClassMappingIds = mapping._classMappings()
                            .select(cm -> cm instanceof RelationFunctionInstanceSetImplementation)
                            .collect(SetImplementation::_id).toSet();
                    validateRelationPropertyMappings(relationClassMapping._propertyMappings(), relationType, processorSupport, mapping, pureModel, relationFunctionClassMappingIds);
                }));
    }

    private void validateRelationPropertyMappings(RichIterable<? extends PropertyMapping> propertyMappings, RelationType<?> relationType, ProcessorSupport processorSupport, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, PureModel pureModel, Set<String> relationFunctionClassMappingIds)
    {
        propertyMappings.each(pm ->
        {
            if (pm instanceof RelationFunctionPropertyMapping)
            {
                validateRelationFunctionPropertyMapping((RelationFunctionPropertyMapping) pm, processorSupport);
            }
            else if (pm instanceof EmbeddedSetImplementation)
            {
                validateRelationPropertyMappings(((EmbeddedSetImplementation) pm)._propertyMappings(), relationType, processorSupport, mapping, pureModel, relationFunctionClassMappingIds);
            }
        });
    }

    /**
     * Validate the {@code _valueFn} of a relation property mapping against the property's declared
     * type and multiplicity.  Mirrors {@code RelationFunctionInstanceSetImplementationValidator} in
     * legend-pure:
     * <ul>
     *   <li>The lambda body's inferred multiplicity must be subsumed by the property multiplicity.</li>
     *   <li>The lambda body's inferred generic type must be a subtype of the property type — unless
     *       an {@link org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping} or
     *       {@link org.finos.legend.pure.generated.Root_meta_external_format_shared_binding_BindingTransformer}
     *       is in play, in which case the transformer is responsible for the conversion.</li>
     * </ul>
     */
    private void validateRelationFunctionPropertyMapping(RelationFunctionPropertyMapping pm, ProcessorSupport processorSupport)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> valueFn = pm._valueFn();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification lastExpr = valueFn._expressionSequence().getLast();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> property = pm._property();

        // Multiplicity subsumption: the property's declared multiplicity must subsume the body's.
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity propertyMultiplicity = property._multiplicity();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity bodyMultiplicity = lastExpr._multiplicity();
        if (!org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.subsumes(propertyMultiplicity, bodyMultiplicity))
        {
            throw new EngineException(
                    "Multiplicity Error: The property '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property) +
                            "' has a multiplicity range of " + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(propertyMultiplicity) +
                            " when the given expression has a multiplicity range of " + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(bodyMultiplicity),
                    SourceInformationHelper.fromM3SourceInformation(pm.getSourceInformation()),
                    EngineErrorType.COMPILATION);
        }

        // Type subtyping check is skipped when a transformer (Binding or EnumerationMapping) is present —
        // the transformer is responsible for converting the body's type to the property type.
        if (pm._transformer() != null)
        {
            return;
        }
        // GenericType-level comparison so the check works for parameterised types, and so that
        // GenericType.print produces meaningful output (a raw Type has no `rawType` slot and would
        // print/compare as if empty).
        GenericType propertyGenericType = property._genericType();
        GenericType bodyGenericType = lastExpr._genericType();

        if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(bodyGenericType, propertyGenericType, processorSupport))
        {
            String exprTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(bodyGenericType, true, processorSupport);
            String propertyTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyGenericType, true, processorSupport);
            throw new EngineException(
                    "Mismatching property and relation expression types. Property '" + pm._property()._name()
                            + "' is of type '" + propertyTypeString
                            + "', but the expression mapped to it is of type '" + exprTypeString + "'.",
                    SourceInformationHelper.fromM3SourceInformation(pm.getSourceInformation()),
                    EngineErrorType.COMPILATION);
        }
    }

}
