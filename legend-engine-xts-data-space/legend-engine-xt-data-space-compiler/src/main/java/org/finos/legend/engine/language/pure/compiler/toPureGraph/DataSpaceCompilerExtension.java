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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.EmbeddedDataFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.core.EmbeddedDataCompilerHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionExpressionBuilderRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerDispatchBuilderInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.IncludedMappingHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.StoreProviderCompilerHelper;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceDiagram;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceSupportCombinedInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceSupportEmail;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.MappingIncludeDataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpacePackageableElementExecutable;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceTemplateExecutable;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.Diagram;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.StoreProviderPointer;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.function.InvalidFunctionDescriptorException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

public class DataSpaceCompilerExtension implements CompilerExtension, EmbeddedDataCompilerHelper, StoreProviderCompilerHelper
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "DataSpace");
    }

    static final ConcurrentHashMap<String, Root_meta_pure_metamodel_dataSpace_DataSpace> dataSpacesIndex = new ConcurrentHashMap<>();

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
                    HashSet<String> executionContextSet = new HashSet<>();
                    metamodel._executionContexts(ListIterate.collect(dataSpace.executionContexts, executionContext ->
                    {
                        if (executionContextSet.add(executionContext.name))
                        {
                            Root_meta_pure_runtime_PackageableRuntime runtime = context.resolvePackageableRuntime(executionContext.defaultRuntime.path, executionContext.defaultRuntime.sourceInformation);
                            Mapping mapping = context.resolveMapping(executionContext.mapping.path, executionContext.mapping.sourceInformation);
                            return new Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpaceExecutionContext"))
                                    ._name(executionContext.name)
                                    ._title(executionContext.title)
                                    ._description(executionContext.description)
                                    ._mapping(mapping)
                                    ._defaultRuntime(runtime);
                        }
                        else
                        {
                            throw new EngineException("Data space execution context, " + executionContext.name + ", is not unique", executionContext.sourceInformation, EngineErrorType.COMPILATION);
                        }
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
                    MutableMap<String, Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext> dataSpaceExecutionContextIndex = Maps.mutable.empty();
                    metamodel._executionContexts().forEach(dataSpaceExecutionContext -> dataSpaceExecutionContextIndex.put(dataSpaceExecutionContext._name(), dataSpaceExecutionContext));
                    dataSpace.executionContexts.forEach(executionContext ->
                    {
                        Root_meta_pure_data_EmbeddedData data = Objects.isNull(executionContext.testData) ? null : executionContext.testData.accept(new EmbeddedDataFirstPassBuilder(context, new ProcessingContext("Dataspace '" + metamodel._name() + "' Second Pass")));
                        dataSpaceExecutionContextIndex.get(executionContext.name)._testData(data);
                    });
                },
                (dataSpace, context) ->
                {

                },
                (dataSpace, context) ->
                {

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
                        metamodel._elements(elements.toSortedList(Comparator.comparing(el -> getElementFullPath(el, context.pureModel.getExecutionSupport()))));
                    }

                    // executables
                    HashSet<String> executableIds = new HashSet<>();
                    metamodel._executables(dataSpace.executables != null ? ListIterate.collect(dataSpace.executables, executable ->
                    {
                        if (executable.executionContextKey != null && !dataSpace.executionContexts.stream().map(c -> c.name).collect(Collectors.toList()).contains(executable.executionContextKey))
                        {
                            throw new EngineException("Data space template executable's executionContextKey, " + executable.executionContextKey + ", is not valid. Please specify one from " + dataSpace.executionContexts.stream().map(c -> c.name).collect(Collectors.toList()).toString(), dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                        }
                        if (executable instanceof DataSpacePackageableElementExecutable)
                        {
                            PackageableElement element;
                            String executablePath = ((DataSpacePackageableElementExecutable) executable).executable.path;
                            SourceInformation sourceInformation = ((DataSpacePackageableElementExecutable) executable).executable.sourceInformation;
                            String executableId;
                            try
                            {
                                element = context.pureModel.getPackageableElement(executablePath, sourceInformation);
                                executableId = executable.id == null ? ((DataSpacePackageableElementExecutable) executable).executable.path : executable.id;
                            }
                            catch (Exception exception)
                            {
                                try
                                {
                                    element = context.pureModel.getPackageableElement(FunctionDescriptor.functionDescriptorToId(executablePath), sourceInformation);
                                    executableId = executable.id;
                                }
                                catch (InvalidFunctionDescriptorException e)
                                {
                                    throw new EngineException(exception.getMessage(), EngineErrorType.COMPILATION);
                                }
                            }
                            if (executableIds.add(executableId))
                            {
                                return new Root_meta_pure_metamodel_dataSpace_DataSpacePackageableElementExecutable_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpacePackageableElementExecutable"))
                                        ._id(executable.id)
                                        ._title(executable.title)
                                        ._description(executable.description)
                                        ._executionContextKey(executable.executionContextKey)
                                        ._executable(element);
                            }
                            else
                            {
                                throw new EngineException("Data space executable id, " + executableId + ", is not unique", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                            }
                        }
                        else if (executable instanceof DataSpaceTemplateExecutable)
                        {
                            if (executableIds.add(executable.id))
                            {
                                FunctionDefinition<?> templateExecutableQuery = HelperValueSpecificationBuilder.buildLambda(((DataSpaceTemplateExecutable) executable).query, context);
                                return new Root_meta_pure_metamodel_dataSpace_DataSpaceTemplateExecutable_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpaceTemplateExecutable"))
                                        ._id(executable.id)
                                        ._title(executable.title)
                                        ._description(executable.description)
                                        ._query(templateExecutableQuery)
                                        ._executionContextKey(executable.executionContextKey);
                            }
                            else
                            {
                                throw new EngineException("Data space executable id, " + executable.id + ", is not unique", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                            }
                        }
                        else
                        {
                            throw new EngineException("Data space executables could only be template or executable", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                        }
                    }) : Lists.immutable.empty());

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
                                    ._diagram(HelperDiagramBuilder.resolveDiagram(diagram.diagram.path, diagram.diagram.sourceInformation, context))) : Lists.immutable.empty());

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
                },
                (dataSpace, context) ->
                {
                    // Move checking mapping/runtime utilized in the function within the curated template query does not align with the mapping/runtime applied in the execution context
                    // to the sixth pass because function body and parameter are processed in the fifthPassBuilder (see https://github.com/finos/legend-engine/pull/2918)
                    // if we do the checking in the fifth pass, function body and parameter will be an empty list
                    if (dataSpace.executables != null)
                    {
                        dataSpace.executables.forEach(executable ->
                        {
                            if (executable instanceof DataSpacePackageableElementExecutable)
                            {
                                FunctionDefinition<?> executableFunction = null;
                                try
                                {
                                    // function
                                    executableFunction = (FunctionDefinition<?>) context.resolvePackageableElement(FunctionDescriptor.functionDescriptorToId((((DataSpacePackageableElementExecutable) executable).executable).path), ((DataSpacePackageableElementExecutable) executable).executable.sourceInformation);
                                }
                                catch (Exception e)
                                {
                                    // service
                                }
                                if (executableFunction != null)
                                {
                                    if (executableFunction instanceof Root_meta_pure_metamodel_function_ConcreteFunctionDefinition_Impl)
                                    {
                                        Optional<? extends ValueSpecification> fromFunc = executableFunction._expressionSequence().toList().stream()
                                                .filter(func -> func instanceof Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl && ((Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl) func)._functionName.equals("from")).findAny();
                                        if (fromFunc.isPresent())
                                        {
                                            Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl fromFuncExpression = (Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl) fromFunc.get();

                                            // only check mapping and runtime if using ->from() in the most basic way e.g. ->from(model::Mapping, model::Runtime)
                                            ValueSpecification mappingInstance = fromFuncExpression._parametersValues().toList().get(1);
                                            if (mappingInstance instanceof Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl)
                                            {
                                                String executionContextKey = executable.executionContextKey != null ? executable.executionContextKey : dataSpace.defaultExecutionContext;
                                                DataSpaceExecutionContext executionContext = dataSpace.executionContexts.stream().filter(ec -> ec.name.equals(executionContextKey)).collect(Collectors.toList()).get(0);
                                                // check if mapping matches to what is used in execution key
                                                Object mappingImpl = ((Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl) mappingInstance)._values().toList().get(0);
                                                if (mappingImpl instanceof Root_meta_pure_mapping_Mapping_Impl)
                                                {
                                                    String mappingPath = platform_pure_essential_meta_graph_elementToPath.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1__String_1_((Root_meta_pure_mapping_Mapping_Impl) mappingImpl, "::", context.pureModel.getExecutionSupport());
                                                    if (!mappingPath.equals(executionContext.mapping.path))
                                                    {
                                                        throw new EngineException("The mapping utilized in the function within the curated template query does not align with the mapping applied in the execution context `" + executionContext.name + "`.");
                                                    }
                                                }
                                                // check if runtime matches to what is used in execution key
                                                RichIterable<? extends Root_meta_core_runtime_Runtime> runtimes = core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_extractRuntimesFromFunctionDefinition_FunctionDefinition_1__Runtime_MANY_(executableFunction, context.pureModel.getExecutionSupport());
                                                Root_meta_core_runtime_Runtime runtimeImpl;
                                                if (runtimes.isEmpty())
                                                {
                                                    runtimeImpl = null;
                                                }
                                                else if (runtimes.size() == 1)
                                                {
                                                    runtimeImpl = runtimes.getOnly();
                                                }
                                                else
                                                {
                                                    throw new UnsupportedOperationException("More than one runtime present in from() function");
                                                }
                                                if (runtimeImpl != null)
                                                {
                                                    String runtimePath = context.pureModel.getRuntimePath(runtimeImpl);
                                                    if (!runtimePath.equals(executionContext.defaultRuntime.path))
                                                    {
                                                        throw new EngineException("The runtime utilized in the function within the curated template query does not align with the runtime applied in the execution context `" + executionContext.name + "`.");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        });
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
    public List<Function3<PackageableElement, CompileContext, ProcessingContext, InstanceValue>> getExtraValueSpecificationBuilderForFuncExpr()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with((packageableElement, context, processingContext) ->
        {
            if (packageableElement instanceof Root_meta_pure_metamodel_dataSpace_DataSpace)
            {
                GenericType dSGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(context.pureModel.getType("meta::pure::metamodel::dataSpace::DataSpace"));
                return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                        ._genericType(dSGenericType)
                        ._multiplicity(context.pureModel.getMultiplicity("one"))
                        ._values(processingContext.peek().equals("Applying new") ? FastList.newList() : FastList.newListWith(packageableElement));
            }
            return null;
        });
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
        String dataSpacePath = getElementFullPath(dataspace, context.pureModel.getExecutionSupport());
        String mappingPath = getElementFullPath(dataspace._defaultExecutionContext()._mapping(), context.pureModel.getExecutionSupport());
        if (stores.isEmpty())
        {
            throw new EngineException("Default mapping (" + mappingPath + ") in dataspace (" + dataSpacePath + ") is not mapped to a store type supported by the ExtraSetImplementationSourceScanners.", storeProviderPointer.sourceInformation, EngineErrorType.COMPILATION);
        }
        else if (stores.size() > 1)
        {
            throw new EngineException("Default mapping (" + mappingPath + ") in dataspace (" + dataSpacePath
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
        return Lists.immutable.with(DataSpaceCompilerExtension::getDataFromDataReferencePMCD);
    }

    private static List<EmbeddedData> getDataFromDataReferencePMCD(DataElementReference dataElementReference, PureModelContextData pureModelContextData)
    {
        return ListIterate
                .select(pureModelContextData.getElementsOfType(DataSpace.class), e -> dataElementReference.dataElement.path.equals(e.getPath()))
                .collect(d -> Iterate.detect(d.executionContexts, e -> e.name.equals(d.defaultExecutionContext)).testData)
                .collect(d -> EmbeddedDataCompilerHelper.getEmbeddedDataFromDataElement(d, pureModelContextData));
    }

    @Override
    public List<Function<Handlers, List<FunctionHandlerDispatchBuilderInfo>>> getExtraFunctionHandlerDispatchBuilderInfoCollectors()
    {
        return Collections.singletonList((handlers) ->
                Lists.mutable.with(
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::mapping::from_T_m__DataSpaceExecutionContext_1__T_m_", (List<ValueSpecification> ps) -> ps.size() == 2 && handlers.isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "DataSpaceExecutionContext".equals(ps.get(1)._genericType()._rawType()._name()))),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::metamodel::dataSpace::get_DataSpace_1__String_1__DataSpaceExecutionContext_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && handlers.isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "DataSpace".equals(ps.get(0)._genericType()._rawType()._name())) && handlers.isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())))

                ));
    }

    @Override
    public List<Function<Handlers, List<FunctionExpressionBuilderRegistrationInfo>>> getExtraFunctionExpressionBuilderRegistrationInfoCollectors()
    {
        return Collections.singletonList((handlers) ->
                Lists.mutable.with(
                        new FunctionExpressionBuilderRegistrationInfo(org.eclipse.collections.impl.factory.Lists.mutable.with(0),
                                handlers.m(handlers.h("meta::pure::mapping::from_T_m__DataSpaceExecutionContext_1__T_m_", false, ps -> handlers.res(ps.get(0)._genericType(), ps.get(0)._multiplicity()), ps -> ps.size() == 2 && handlers.typeOne(ps.get(1), "DataSpaceExecutionContext")))
                        ),
                        // getter for execution parameters from execution environment
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(handlers.m(handlers.h("meta::pure::metamodel::dataSpace::get_DataSpace_1__String_1__DataSpaceExecutionContext_1_", false, ps -> handlers.res("meta::pure::metamodel::dataSpace::DataSpaceExecutionContext", "one"), ps -> ps.size() == 2))))

                ));
    }

}
