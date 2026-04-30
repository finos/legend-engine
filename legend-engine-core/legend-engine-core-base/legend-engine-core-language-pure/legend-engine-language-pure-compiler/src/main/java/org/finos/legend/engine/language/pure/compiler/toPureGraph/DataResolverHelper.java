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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.EmbeddedDataFirstPassBuilder;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.BaseDataResolver;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataResolver;
import org.finos.legend.engine.protocol.pure.v1.model.data.ReferenceDataResolver;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElement;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataResolver;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;

import org.finos.legend.pure.generated.Root_meta_pure_data_RelationElement;
import org.finos.legend.pure.generated.Root_meta_pure_data_RelationElementsData;
import org.finos.legend.pure.generated.Root_meta_pure_data_RelationElementsData_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;

import java.util.ArrayList;
import java.util.List;

public class DataResolverHelper
{
    public MutableMap<PackageableElement, Root_meta_pure_data_EmbeddedData> resolveDataFromDataResolvers(List<DataResolver> dataResolvers, CompileContext context, ProcessingContext processingContext)
    {
        // priority order for a given PackageableElement: base data > Direct PackageableElement (non DataElement) reference > DataElement reference (in case of multiple, priority is from first to last)
        // embeddedDataPerElement is created in increasing priority order
        MutableMap<PackageableElement, List<Root_meta_pure_data_EmbeddedData>> embeddedDataPerElement = Maps.mutable.empty();
        MutableSet<String> visitedReferencePaths = Sets.mutable.empty();
        MutableSet<String> visitedBaseDataPaths = Sets.mutable.empty();

        EmbeddedDataFirstPassBuilder embeddedDataFirstPassBuilder = new EmbeddedDataFirstPassBuilder(context, processingContext);

        dataResolvers.stream().filter(resolver -> resolver instanceof ReferenceDataResolver).forEach(dataResolver ->
        {
            ReferenceDataResolver referenceDataResolver = (ReferenceDataResolver) dataResolver;
            if (visitedReferencePaths.contains(referenceDataResolver.elementPointer.path))
            {
                throw new EngineException("Duplicate data reference: " + referenceDataResolver.elementPointer.path, referenceDataResolver.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
            }
            PackageableElement element = context.resolvePackageableElement(referenceDataResolver.elementPointer.path, referenceDataResolver.elementPointer.sourceInformation);
            if (element instanceof Root_meta_pure_data_DataElement)
            {
                ((Root_meta_pure_data_DataElement) element)._resolvedData().getMap()
                        .forEach((k, v) -> embeddedDataPerElement.getIfAbsentPut((PackageableElement) k, ArrayList::new).add((Root_meta_pure_data_EmbeddedData) v));
            }
            else if (!(element instanceof Root_meta_pure_data_DataResolver))
            {
                throw new EngineException("Unsupported type for data resolution: " + referenceDataResolver.elementPointer.path, referenceDataResolver.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
            }
            visitedReferencePaths.add(referenceDataResolver.elementPointer.path);
        });

        dataResolvers.stream().filter(resolver -> resolver instanceof ReferenceDataResolver).forEach(dataResolver ->
        {
            ReferenceDataResolver referenceDataResolver = (ReferenceDataResolver) dataResolver;
            PackageableElement element = context.resolvePackageableElement(referenceDataResolver.elementPointer.path, referenceDataResolver.elementPointer.sourceInformation);
            if (element instanceof Root_meta_pure_data_DataResolver)
            {
                Root_meta_pure_data_DataResolver dataProvider = (Root_meta_pure_data_DataResolver) element;
                if (dataProvider._data() == null)
                {
                    throw new EngineException(String.format("'%s' does not provide and embedded data", referenceDataResolver.elementPointer.path), referenceDataResolver.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
                }
                embeddedDataPerElement.getIfAbsentPut(element, ArrayList::new).add(dataProvider._data());
            }
        });

        dataResolvers.stream().filter(resolver -> resolver instanceof BaseDataResolver).forEach(dataResolver ->
        {
            BaseDataResolver baseDataResolver = (BaseDataResolver) dataResolver;
            if (visitedBaseDataPaths.contains(baseDataResolver.elementPointer.path))
            {
                throw new EngineException("Duplicate base data reference: " + baseDataResolver.elementPointer.path, baseDataResolver.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
            }
            PackageableElement element = context.resolvePackageableElement(baseDataResolver.elementPointer.path, baseDataResolver.elementPointer.sourceInformation);
            if (element instanceof Root_meta_pure_data_DataResolver || element instanceof Database)
            {
                Root_meta_pure_data_EmbeddedData metamodelData = baseDataResolver.data.accept(embeddedDataFirstPassBuilder);
                Root_meta_pure_data_EmbeddedData resolvedData = context.getCompilerExtensions()
                        .getPackageableElementToEmbeddedDataProcessors().stream()
                        .map(processor -> processor.value(element, metamodelData, context, false, baseDataResolver.sourceInformation))
                        .filter(java.util.Objects::nonNull)
                        .findFirst()
                        .orElse(metamodelData);
                embeddedDataPerElement.getIfAbsentPut(element, ArrayList::new).add(resolvedData);
            }
            else
            {
                throw new EngineException("Unsupported type for data resolution: " + baseDataResolver.elementPointer.path, baseDataResolver.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
            }
            visitedBaseDataPaths.add(baseDataResolver.elementPointer.path);
        });

        // Merge/combine embedded data
        MutableMap<PackageableElement, Root_meta_pure_data_EmbeddedData> resolvedDataMap = Maps.mutable.empty();
        embeddedDataPerElement.forEach((element, allEmbeddedData) ->
        {
            if (allEmbeddedData.stream().anyMatch(data -> !(data instanceof Root_meta_pure_data_RelationElementsData)))
            {
                resolvedDataMap.put(element, allEmbeddedData.get(allEmbeddedData.size() - 1));
            }
            else
            {
                Root_meta_pure_data_RelationElementsData mergedData = new Root_meta_pure_data_RelationElementsData_Impl("", null, context.pureModel.getClass("meta::pure::data::RelationElementsData"));
                MutableMap<String, Root_meta_pure_data_RelationElement> relationElementByPath = Maps.mutable.empty();
                allEmbeddedData.forEach(data ->
                {
                    Root_meta_pure_data_RelationElementsData relationData = (Root_meta_pure_data_RelationElementsData) data;
                    for (Root_meta_pure_data_RelationElement relationElement : relationData._relationElements())
                    {
                        relationElementByPath.put(String.join(".", relationElement._paths()), relationElement);
                    }
                });
                mergedData._relationElementsAddAll(relationElementByPath.valuesView());
                resolvedDataMap.put(element, mergedData);
            }
        });

        return resolvedDataMap;
    }
}
