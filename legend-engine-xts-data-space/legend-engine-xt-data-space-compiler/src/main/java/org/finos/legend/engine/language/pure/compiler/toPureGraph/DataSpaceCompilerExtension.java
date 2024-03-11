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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.EmbeddedDataFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.core.EmbeddedDataCompilerHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.IncludedMappingHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.StoreProviderCompilerHelper;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.Diagram;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.StoreProviderPointer;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

import java.util.*;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

public class DataSpaceCompilerExtension implements CompilerExtension, EmbeddedDataCompilerHelper, StoreProviderCompilerHelper
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "DataSpace");
    }

    static final MutableMap<String, Root_meta_pure_metamodel_dataSpace_DataSpace> dataSpacesIndex = Maps.mutable.empty();

    @Override
    public CompilerExtension build()
    {
        return new DataSpaceCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.singletonList(Processor.newProcessor(
                DataSpace.class,
                Lists.fixedSize.with(PackageableRuntime.class, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.class, Diagram.class),
                (dataSpace, context) ->
                {
                    Root_meta_pure_metamodel_dataSpace_DataSpace metamodel = new Root_meta_pure_metamodel_dataSpace_DataSpace_Impl(dataSpace.name, null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpace"))._name(dataSpace.name);
                    dataSpacesIndex.put(context.pureModel.buildPackageString(dataSpace._package, dataSpace.name), metamodel);
                    metamodel._stereotypes(ListIterate.collect(dataSpace.stereotypes, s -> context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)));
                    metamodel._taggedValues(ListIterate.collect(dataSpace.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::extension::TaggedValue"))._tag(context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.tag.sourceInformation))._value(t.value)));

                    // execution context
                    if (dataSpace.executionContexts.isEmpty())
                    {
                        throw new EngineException("Data space must have at least one execution context", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                    }
                    metamodel._executionContexts(ListIterate.collect(dataSpace.executionContexts, executionContext ->
                    {
                        Root_meta_pure_runtime_PackageableRuntime runtime = context.resolvePackageableRuntime(executionContext.defaultRuntime.path, executionContext.defaultRuntime.sourceInformation);
                        Mapping mapping = context.resolveMapping(executionContext.mapping.path, executionContext.mapping.sourceInformation);
                        Root_meta_pure_data_EmbeddedData data = Objects.isNull(executionContext.testData) ? null : executionContext.testData.accept(new EmbeddedDataFirstPassBuilder(context, new ProcessingContext("Dataspace '" + metamodel._name() + "' First Pass")));
                        return new Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpaceExecutionContext"))
                                ._name(executionContext.name)
                                ._title(executionContext.title)
                                ._description(executionContext.description)
                                ._mapping(mapping)
                                ._testData(data)
                                ._defaultRuntime(runtime);
                    }));
                    Assert.assertTrue(dataSpace.defaultExecutionContext != null, () -> "Default execution context is missing", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                    Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext defaultExecutionContext = metamodel._executionContexts().toList().select(c -> dataSpace.defaultExecutionContext.equals(c._name())).getFirst();
                    if (defaultExecutionContext == null)
                    {
                        throw new EngineException("Default execution context '" + dataSpace.defaultExecutionContext + "' does not match any existing execution contexts", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                    }
                    metamodel._defaultExecutionContext(defaultExecutionContext);

                    metamodel._title(dataSpace.title);
                    metamodel._description(dataSpace.description);

                    return metamodel;
                },
                (dataSpace, context) ->
                {
                    Root_meta_pure_metamodel_dataSpace_DataSpace metamodel = dataSpacesIndex.get(context.pureModel.buildPackageString(dataSpace._package, dataSpace.name));

                    ListIterate.forEach(dataSpace.executionContexts, executionContext ->
                    {
                        Mapping mapping = context.resolveMapping(executionContext.mapping.path, executionContext.mapping.sourceInformation);
                        Root_meta_pure_runtime_PackageableRuntime runtime = context.resolvePackageableRuntime(executionContext.defaultRuntime.path, executionContext.defaultRuntime.sourceInformation);
                        if (!HelperRuntimeBuilder.isRuntimeCompatibleWithMapping(runtime, mapping))
                        {
                            throw new EngineException("Execution context '" + executionContext.name + "' default runtime is not compatible with mapping", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                        }
                    });

                    // elements
                    if (dataSpace.elements != null)
                    {
                        MutableSet<PackageableElement> elements = Sets.mutable.empty();
                        MutableList<DataSpaceElementPointer> includes = ListIterate.select(dataSpace.elements, el -> el.exclude == null || !el.exclude);
                        MutableSet<String> excludePaths = ListIterate.select(dataSpace.elements, el -> el.exclude != null && el.exclude).collect(el -> el.path).toSet();

                        includes.forEach(include -> HelperDataSpaceBuilder.collectElements(include, elements, excludePaths, context));
                        metamodel._elements(elements.toSortedList(Comparator.comparing(el -> HelperModelBuilder.getElementFullPath(el, context.pureModel.getExecutionSupport()))));
                    }

                    // executables
                    HashSet<String> executableTitles = new HashSet<>();
                    metamodel._executables(dataSpace.executables != null ? ListIterate.collect(dataSpace.executables, executable ->
                    {
                        if (executable instanceof DataSpacePackageableElementExecutable)
                        {
                            return new Root_meta_pure_metamodel_dataSpace_DataSpacePackageableElementExecutable_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpacePackageableElementExecutable"))
                                    ._title(executable.title)
                                    ._description(executable.description)
                                    ._executable(context.pureModel.getPackageableElement(((DataSpacePackageableElementExecutable) executable).executable.path, ((DataSpacePackageableElementExecutable) executable).executable.sourceInformation));
                        }
                        else if (executable instanceof DataSpaceTemplateExecutable)
                        {
                            if (executableTitles.add(executable.title))
                            {
                                if (((DataSpaceTemplateExecutable) executable).executionContextKey != null && !dataSpace.executionContexts.stream().map(c -> c.name).collect(Collectors.toList()).contains(((DataSpaceTemplateExecutable) executable).executionContextKey))
                                {
                                    throw new EngineException("Data space template executable's executionContextKey is not valid", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                                }
                                return new Root_meta_pure_metamodel_dataSpace_DataSpaceTemplateExecutable_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpaceTemplateExecutable"))
                                        ._title(executable.title)
                                        ._description(executable.description)
                                        ._query(HelperValueSpecificationBuilder.buildLambda(((DataSpaceTemplateExecutable) executable).query, context))
                                        ._executionContextKey(((DataSpaceTemplateExecutable) executable).executionContextKey);
                            }
                            else
                            {
                                throw new EngineException("Data space executable title is not unique", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                            }
                        }
                        else
                        {
                            throw new EngineException("Data space executables could only be template or executable", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                        }
                    }) : null);

                    // diagrams
                    if (dataSpace.featuredDiagrams != null)
                    {
                        List<DataSpaceDiagram> featuredDiagrams = ListIterate.collect(dataSpace.featuredDiagrams, featuredDiagram ->
                        {
                            DataSpaceDiagram diagram = new DataSpaceDiagram();
                            diagram.title = "";
                            diagram.diagram = featuredDiagram;
                            diagram.sourceInformation = featuredDiagram.sourceInformation;
                            return diagram;
                        });
                        if (dataSpace.diagrams != null)
                        {
                            dataSpace.diagrams.addAll(featuredDiagrams);
                        }
                        else
                        {
                            dataSpace.diagrams = featuredDiagrams;
                        }
                    }
                    metamodel._diagrams(dataSpace.diagrams != null ? ListIterate.collect(dataSpace.diagrams, diagram ->
                            new Root_meta_pure_metamodel_dataSpace_DataSpaceDiagram_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpaceDiagram"))
                                    ._title(diagram.title)
                                    ._description(diagram.description)
                                    ._diagram(HelperDiagramBuilder.resolveDiagram(diagram.diagram.path, diagram.diagram.sourceInformation, context))) : null);

                    // support info
                    if (dataSpace.supportInfo != null)
                    {
                        Root_meta_pure_metamodel_dataSpace_DataSpaceSupportInfo supportInfo = null;
                        if (dataSpace.supportInfo instanceof DataSpaceSupportEmail)
                        {
                            supportInfo = new Root_meta_pure_metamodel_dataSpace_DataSpaceSupportEmail_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpaceSupportEmail"))
                                    ._documentationUrl(dataSpace.supportInfo.documentationUrl)
                                    ._address(((DataSpaceSupportEmail) dataSpace.supportInfo).address);
                        }
                        else if (dataSpace.supportInfo instanceof DataSpaceSupportCombinedInfo)
                        {
                            supportInfo = new Root_meta_pure_metamodel_dataSpace_DataSpaceSupportCombinedInfo_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpaceSupportCombinedInfo"))
                                    ._documentationUrl(dataSpace.supportInfo.documentationUrl)
                                    ._website(((DataSpaceSupportCombinedInfo) dataSpace.supportInfo).website)
                                    ._faqUrl(((DataSpaceSupportCombinedInfo) dataSpace.supportInfo).faqUrl)
                                    ._supportUrl(((DataSpaceSupportCombinedInfo) dataSpace.supportInfo).supportUrl)
                                    ._emails(Lists.mutable.ofAll(((DataSpaceSupportCombinedInfo) dataSpace.supportInfo).emails));
                        }
                        metamodel._supportInfo(supportInfo);
                    }
                }
        ));
    }

    @Override
    public Map<String, IncludedMappingHandler> getExtraIncludedMappingHandlers()
    {
        return org.eclipse.collections.impl.factory.Maps.mutable.of(
                MappingIncludeDataSpace.class.getName(), new DataSpaceIncludedMappingHandler()
        );
    }

    @Override
    public Map<PackageableElementType, Function2<StoreProviderPointer, CompileContext, Store>> getExtraStoreProviderHandlers()
    {
        return Maps.mutable.of(PackageableElementType.DATASPACE, DataSpaceCompilerExtension::resolveStore);
    }

    private static Store resolveStore(StoreProviderPointer storeProviderPointer, CompileContext context)
    {
        String packageAddress = storeProviderPointer.path;
        if (!DataSpaceCompilerExtension.dataSpacesIndex.containsKey(packageAddress))
        {
            throw new EngineException("Dataspace " + packageAddress + " cannot be found.", storeProviderPointer.sourceInformation, EngineErrorType.COMPILATION);
        }
        Root_meta_pure_metamodel_dataSpace_DataSpace dataspace =
                DataSpaceCompilerExtension.dataSpacesIndex.get(packageAddress);
        ImmutableList<Store> stores = HelperMappingBuilder.getStoresFromMappingIgnoringIncludedMappings(dataspace._defaultExecutionContext()._mapping(),context);
        String dataspacePath = HelperModelBuilder.getElementFullPath(dataspace, context.pureModel.getExecutionSupport());
        String mappingPath = HelperModelBuilder.getElementFullPath(dataspace._defaultExecutionContext()._mapping(), context.pureModel.getExecutionSupport());
        if (stores.isEmpty())
        {
            throw new EngineException("Default mapping (" + mappingPath + ") in dataspace (" + dataspacePath + ") is not mapped to a store type supported by the ExtraSetImplementationSourceScanners.", storeProviderPointer.sourceInformation, EngineErrorType.COMPILATION);
        }
        else if (stores.size() > 1)
        {
            throw new EngineException("Default mapping (" + mappingPath + ") in dataspace (" + dataspacePath
                    + ") cannot be resolved to a single store. Stores found : ["
                    + stores.collect(s -> getElementFullPath(s, context.pureModel.getExecutionSupport())).makeString(",")
                    + "]. Please notify dataspace owners that their dataspace cannot be used as a store interface.",
                    storeProviderPointer.sourceInformation,
                    EngineErrorType.COMPILATION
            );
        }
        return stores.get(0);
    }

    @Override
    public List<Function3<EmbeddedData, CompileContext, ProcessingContext, Root_meta_pure_data_EmbeddedData>> getExtraEmbeddedDataProcessors()
    {
        return Collections.singletonList(this::compileDataspaceDataElementReference);
    }

    private Root_meta_pure_data_EmbeddedData compileDataspaceDataElementReference(EmbeddedData embeddedData, CompileContext compileContext, ProcessingContext processingContext)
    {
        if (embeddedData instanceof DataElementReference
                && ((DataElementReference) embeddedData).dataElement.type.equals(PackageableElementType.DATASPACE))
        {
            DataElementReference data = (DataElementReference) embeddedData;
            if (DataSpaceCompilerExtension.dataSpacesIndex.containsKey(data.dataElement.path))
            {
                return ((Root_meta_pure_data_DataElementReference) Optional
                        .ofNullable(DataSpaceCompilerExtension.dataSpacesIndex.get(data.dataElement.path)._defaultExecutionContext()._testData())
                        .orElseThrow(() -> new EngineException("Dataspace " + data.dataElement.path + " does not have test data in its default execution context.", data.sourceInformation, EngineErrorType.COMPILATION))
                        )._dataElement()._data();
            }
            throw new EngineException("Dataspace " + data.dataElement.path + " cannot be found.", data.sourceInformation, EngineErrorType.COMPILATION);
        }
        return null;
    }


    @Override
    public Iterable<? extends Function2<DataElementReference, PureModelContextData, List<EmbeddedData>>> getExtraDataElementReferencePMCDTraversers()
    {
        return org.eclipse.collections.api.factory.Lists.immutable.with(DataSpaceCompilerExtension::getDataFromDataReferencePMCD);
    }

    private static List<EmbeddedData> getDataFromDataReferencePMCD(DataElementReference dataElementReference, PureModelContextData pureModelContextData)
    {
        return ListIterate
                .select(pureModelContextData.getElementsOfType(DataSpace.class), e -> dataElementReference.dataElement.path.equals(e.getPath()))
                .collect(d -> Iterate.detect(d.executionContexts, e -> e.name.equals(d.defaultExecutionContext)).testData)
                .collect(d -> EmbeddedDataCompilerHelper.getEmbeddedDataFromDataElement(d, pureModelContextData));
    }
}
