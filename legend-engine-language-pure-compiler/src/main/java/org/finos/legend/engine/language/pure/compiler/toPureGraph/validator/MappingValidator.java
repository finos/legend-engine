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
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.Warning;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementationAccessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        this.validateClassMappingRoots(pureModel, mappings, pureMappings);

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
        mapping.includedMappings.forEach(mappingInclude ->
        {
            Mapping includedMapping = mappings.get(mappingInclude.getIncludedMapping());
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
        pureModel.addWarnings(mapping._classMappings().flatCollect(cm ->
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
            return Lists.mutable.with(new Warning(org.finos.legend.engine.language.pure.compiler.toPureGraph.SourceInformationHelper.fromM3SourceInformation(pm.getSourceInformation()), "Error '" + id + "' can't be found in the mapping " + pureModel.buildPackageString(mapping._package()._name(), mapping._name())));
        }
        return Lists.mutable.empty();
    }


    public void validateClassMappingRoots(PureModel pureModel, Map<String, Mapping> mappings, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> pureMappings)
    {
        pureMappings.forEach((mappingPath, mapping) ->
        {
            MutableListMultimap<String, SetImplementation> directMappings = Multimaps.mutable.list.empty();
            for (SetImplementation classMapping : mapping._classMappings())
            {
                if (!(classMapping instanceof EmbeddedSetImplementation))
                {
                    directMappings.put(HelperModelBuilder.getElementFullPath(classMapping._class(), pureModel.getExecutionSupport()), classMapping);
                }
            }
            for (Pair<String, RichIterable<SetImplementation>> val : directMappings.keyMultiValuePairsView())
            {
                RichIterable<SetImplementation> classMappings = val.getTwo();
                if (classMappings.size() == 1)
                {
                    classMappings.toList().get(0)._root(true);
                }
                else
                {
                    int rootCount = classMappings.count(SetImplementationAccessor::_root);
                    if (rootCount != 1)
                    {
                        throw new EngineException("Class '" + val.getOne() + "' is mapped by " + classMappings.size() + " set implementations and has " + rootCount + " roots. There should be exactly one root set implementation for the class, and it should be marked with a '*'", mappings.get(mappingPath).sourceInformation, EngineErrorType.COMPILATION);
                    }
                }
            }
        });
    }
}
