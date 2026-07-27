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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.EmbeddedDataFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.core.EmbeddedDataCompilerHelper;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.BaseDataResolver;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataProvider;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataResolver;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ReferenceDataResolver;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElement;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElementsData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
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
import java.util.function.Function;
import java.util.function.Predicate;

public class DataResolverHelper
{
    public MutableMap<PackageableElement, Root_meta_pure_data_EmbeddedData> resolveDataFromDataResolvers(List<DataResolver> dataResolvers, CompileContext context, ProcessingContext processingContext)
    {
        // priority order for a given PackageableElement (later overrides earlier): DataElement reference < Direct PackageableElement (non DataElement) reference < base data
        // embeddedDataPerElement is populated in increasing priority order, and mergeByPriority picks the last entry (or per-path last-wins for RelationElementsData)
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
                Root_meta_pure_data_DataElement dataElement = (Root_meta_pure_data_DataElement) element;
                if (dataElement._resolvedData() == null)
                {
                    throw new EngineException("Data element '" + referenceDataResolver.elementPointer.path + "' referenced by a ReferenceDataResolver must define dataResolvers", referenceDataResolver.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
                }
                dataElement._resolvedData().getMap()
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

        return mergeByPriority(
                embeddedDataPerElement,
                Root_meta_pure_data_RelationElementsData.class,
                relationDataList ->
                {
                    Root_meta_pure_data_RelationElementsData mergedData = new Root_meta_pure_data_RelationElementsData_Impl("", null, context.pureModel.getClass("meta::pure::data::RelationElementsData"));
                    MutableMap<String, Root_meta_pure_data_RelationElement> relationElementByPath = Maps.mutable.empty();
                    relationDataList.forEach(relationData ->
                    {
                        for (Root_meta_pure_data_RelationElement relationElement : relationData._relationElements())
                        {
                            relationElementByPath.put(String.join(".", relationElement._paths()), relationElement);
                        }
                    });
                    mergedData._relationElementsAddAll(relationElementByPath.valuesView());
                    return mergedData;
                });
    }

    // data resolver at the protocol level that can be used directly by test runners; this is a recursive function unlike the above which deals with compiled embedded data
    // resolveDataFromDataResolvers can be used for validation alongside this
    // shares merging logic with resolveDataFromDataResolvers
    public MutableMap<PackageableElement, EmbeddedData> resolveDataFromDataResolversToProtocol(List<DataResolver> dataResolvers, CompileContext context, PureModelContextData pureModelContextData)
    {
        MutableMap<PackageableElement, List<EmbeddedData>> embeddedDataPerElement = Maps.mutable.empty();
        MutableSet<String> visitedReferencePaths = Sets.mutable.empty();
        MutableSet<String> visitedBaseDataPaths = Sets.mutable.empty();

        // Pass A: ReferenceDataResolver -> DataElement (transitive expansion of that DataElement's own dataResolvers)
        dataResolvers.stream().filter(r -> r instanceof ReferenceDataResolver).forEach(r ->
        {
            ReferenceDataResolver ref = (ReferenceDataResolver) r;
            if (visitedReferencePaths.contains(ref.elementPointer.path))
            {
                throw new EngineException("Duplicate data reference: " + ref.elementPointer.path, ref.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
            }
            PackageableElement element = context.resolvePackageableElement(ref.elementPointer.path, ref.elementPointer.sourceInformation);
            if (element instanceof Root_meta_pure_data_DataElement)
            {
                DataElement protocolDataElement = pureModelContextData.getElementsOfType(DataElement.class).stream().filter(e -> ref.elementPointer.path.equals(e.getPath())).findFirst().orElse(null);
                if (protocolDataElement == null || protocolDataElement.dataResolvers == null || protocolDataElement.dataResolvers.isEmpty())
                {
                    throw new EngineException("Data element '" + ref.elementPointer.path + "' referenced by a ReferenceDataResolver must define dataResolvers", ref.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
                }
                MutableMap<PackageableElement, EmbeddedData> nested = resolveDataFromDataResolversToProtocol(protocolDataElement.dataResolvers, context, pureModelContextData);
                nested.forEach((k, v) -> embeddedDataPerElement.getIfAbsentPut(k, ArrayList::new).add(v));
            }
            else if (!(element instanceof Root_meta_pure_data_DataResolver))
            {
                throw new EngineException("Unsupported type for data resolution: " + ref.elementPointer.path, ref.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
            }
            visitedReferencePaths.add(ref.elementPointer.path);
        });

        // Pass B: ReferenceDataResolver -> DataProvider
        dataResolvers.stream().filter(r -> r instanceof ReferenceDataResolver).forEach(r ->
        {
            ReferenceDataResolver ref = (ReferenceDataResolver) r;
            PackageableElement element = context.resolvePackageableElement(ref.elementPointer.path, ref.elementPointer.sourceInformation);
            if (element instanceof Root_meta_pure_data_DataResolver && !(element instanceof Root_meta_pure_data_DataElement))
            {
                DataProvider protocolProvider = pureModelContextData.getElementsOfType(DataProvider.class).stream().filter(e -> ref.elementPointer.path.equals(e.getPath())).findFirst().orElse(null);
                if (protocolProvider != null)
                {
                    if (protocolProvider.data == null)
                    {
                        throw new EngineException(String.format("'%s' does not provide and embedded data", ref.elementPointer.path), ref.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
                    }
                    if (protocolProvider.data instanceof DataElementReference)
                    {
                        throw new EngineException(String.format("'%s' provides a DataElementReference, which is not supported for DataProviders", ref.elementPointer.path), ref.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
                    }
                    embeddedDataPerElement.getIfAbsentPut(element, ArrayList::new).add(protocolProvider.data);
                }
            }
        });

        // Pass C: BaseDataResolver -> Store/Database, data already in protocol shape
        dataResolvers.stream().filter(r -> r instanceof BaseDataResolver).forEach(r ->
        {
            BaseDataResolver base = (BaseDataResolver) r;
            if (visitedBaseDataPaths.contains(base.elementPointer.path))
            {
                throw new EngineException("Duplicate base data reference: " + base.elementPointer.path, base.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
            }
            PackageableElement element = context.resolvePackageableElement(base.elementPointer.path, base.elementPointer.sourceInformation);
            if (element instanceof Root_meta_pure_data_DataResolver || element instanceof Database)
            {
                EmbeddedData resolvedData = base.data instanceof DataElementReference
                        ? EmbeddedDataCompilerHelper.getEmbeddedDataFromDataElement((DataElementReference) base.data, pureModelContextData)
                        : base.data;
                embeddedDataPerElement.getIfAbsentPut(element, ArrayList::new).add(resolvedData);
            }
            else
            {
                throw new EngineException("Unsupported type for data resolution: " + base.elementPointer.path, base.elementPointer.sourceInformation, EngineErrorType.COMPILATION);
            }
            visitedBaseDataPaths.add(base.elementPointer.path);
        });

        return mergeByPriority(
                embeddedDataPerElement,
                RelationElementsData.class,
                relationDataList ->
                {
                    RelationElementsData merged = new RelationElementsData();
                    MutableMap<String, RelationElement> relationElementByPath = Maps.mutable.empty();
                    relationDataList.forEach(relationData ->
                    {
                        if (relationData.relationElements != null)
                        {
                            for (RelationElement relationElement : relationData.relationElements)
                            {
                                relationElementByPath.put(String.join(".", relationElement.paths == null ? Lists.mutable.empty() : relationElement.paths), relationElement);
                            }
                        }
                    });
                    merged.relationElements = Lists.mutable.withAll(relationElementByPath.valuesView());
                    return merged;
                });
    }

    private static <E, R extends E> MutableMap<PackageableElement, E> mergeByPriority(
            MutableMap<PackageableElement, List<E>> perElement,
            Class<R> relationType,
            Function<List<R>, R> relationMerger)
    {
        Predicate<E> isRelation = relationType::isInstance;
        MutableMap<PackageableElement, E> resolved = Maps.mutable.empty();
        perElement.forEach((element, all) ->
        {
            if (all.stream().anyMatch(isRelation.negate()))
            {
                resolved.put(element, all.get(all.size() - 1));
            }
            else
            {
                MutableList<R> relations = Lists.mutable.empty();
                for (E e : all)
                {
                    relations.add(relationType.cast(e));
                }
                resolved.put(element, relationMerger.apply(relations));
            }
        });
        return resolved;
    }
}
