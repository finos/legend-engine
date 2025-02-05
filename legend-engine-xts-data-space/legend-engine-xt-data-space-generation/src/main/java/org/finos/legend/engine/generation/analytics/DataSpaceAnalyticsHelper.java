// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.generation.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.entitlement.services.EntitlementModelObjectMapperFactory;
import org.finos.legend.engine.entitlement.services.EntitlementServiceExtension;
import org.finos.legend.engine.entitlement.services.EntitlementServiceExtensionLoader;
import org.finos.legend.engine.language.pure.compiler.fromPureGraph.PureModelContextDataGenerator;
import org.finos.legend.engine.generation.analytics.model.*;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.analytics.model.MappingModelCoverageAnalysisResult;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.ResultType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSResultType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceTemplateExecutable;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.m3.function.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;

import java.util.*;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabSize;
import static org.finos.legend.engine.shared.core.ObjectMapperFactory.withStandardConfigurations;


public class DataSpaceAnalyticsHelper
{
    private static final ObjectMapper objectMapper = getNewObjectMapper();

    public static ObjectMapper getNewObjectMapper()
    {
        return EntitlementModelObjectMapperFactory.withEntitlementModelExtensions(withStandardConfigurations(PureProtocolObjectMapperFactory.withPureProtocolExtensions(new ObjectMapper())));
    }

    private static DataSpaceBasicDocumentationEntry buildBasicDocumentationEntry(Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceBasicDocumentationEntry entry)
    {
        DataSpaceBasicDocumentationEntry docEntry = new DataSpaceBasicDocumentationEntry();
        docEntry.name = entry._name();
        docEntry.docs = new ArrayList<>(entry._docs().toList());
        return docEntry;
    }

    private static DataSpacePropertyDocumentationEntry buildPropertyDocumentationEntry(Root_meta_pure_metamodel_dataSpace_analytics_DataSpacePropertyDocumentationEntry entry)
    {
        DataSpacePropertyDocumentationEntry docEntry = new DataSpacePropertyDocumentationEntry();
        docEntry.name = entry._name();
        docEntry.docs = new ArrayList<>(entry._docs().toList());
        docEntry.milestoning = entry._milestoning();
        docEntry.type = entry._type();
        docEntry.multiplicity = new Multiplicity(entry._multiplicity()._lowerBound()._value().intValue(), entry._multiplicity()._upperBound()._value() != null ? entry._multiplicity()._upperBound()._value().intValue() : null);
        return docEntry;
    }

    private static DataSpaceExecutableResult buildExecutableResult(ResultType resultType)
    {
        if (resultType instanceof TDSResultType)
        {
            DataSpaceExecutableTDSResult result = new DataSpaceExecutableTDSResult();
            result.columns = (((TDSResultType) resultType).tdsColumns).stream().map(tdsColumn ->
            {
                DataSpaceExecutableTDSResultColumn column = new DataSpaceExecutableTDSResultColumn();
                column.name = tdsColumn.name;
                column.type = tdsColumn.type;
                column.relationalType = tdsColumn.relationalType;
                column.documentation = tdsColumn.doc;
                return column;
            }).collect(Collectors.toList());
            return result;
        }
        return null;
    }

    private static List<DataSpaceExecutableAnalysisResult> buildDataSpaceExecutableAnalysisResult(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, List<EntitlementServiceExtension> entitlementServiceExtensions, MutableList<PlanGeneratorExtension> generatorExtensions, boolean buildResult)
    {
        List<DataSpaceExecutableAnalysisResult> dataSpaceExecutionContextAnalysisResults = new ArrayList<>();
        if (dataSpace._executables() != null)
        {
            dataSpace._executables().forEach((executable) ->
            {
                if (executable instanceof Root_meta_pure_metamodel_dataSpace_DataSpaceTemplateExecutable)
                {
                    DataSpaceExecutableAnalysisResult executableAnalysisResult = new DataSpaceExecutableAnalysisResult();
                    executableAnalysisResult.title = executable._title();
                    executableAnalysisResult.description = executable._description();
                    DataSpaceTemplateExecutableInfo templateExecutableInfo = new DataSpaceTemplateExecutableInfo();
                    templateExecutableInfo.id = executable._id();
                    // get V1 lambda
                    DataSpaceTemplateExecutable executableV1 = (DataSpaceTemplateExecutable) dataSpaceProtocol.executables.stream().filter(e -> e instanceof DataSpaceTemplateExecutable && e.id.equals(executable._id())).findFirst().get();
                    templateExecutableInfo.query = executableV1.query.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withIndentation(getTabSize(1)).build());
                    Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext executionContext = executable._executionContextKey() == null ? dataSpace._defaultExecutionContext() :
                            dataSpace._executionContexts().toList().stream().filter(c -> c._name().equals(executable._executionContextKey())).findFirst().get();
                    templateExecutableInfo.executionContextKey = executable._executionContextKey() == null ? dataSpace._defaultExecutionContext()._name() : executable._executionContextKey();
                    executableAnalysisResult.info = templateExecutableInfo;
                    if (buildResult)
                    {
                        executableAnalysisResult.result = buildExecutableResult(PlanGenerator.generateExecutionPlanDebug(
                                ((Root_meta_pure_metamodel_dataSpace_DataSpaceTemplateExecutable) executable)._query(),
                                executionContext._mapping(),
                                executionContext._defaultRuntime()._runtimeValue(),
                                HelperValueSpecificationBuilder.processExecutionContext(new BaseExecutionContext(), pureModel.getContext()),
                                pureModel,
                                PureClientVersions.production,
                                PlanPlatform.JAVA,
                                null,
                                PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport())),
                                generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers)
                        ).plan.rootExecutionNode.resultType);
                    }
                    dataSpaceExecutionContextAnalysisResults.add(executableAnalysisResult);
                }
                else if (executable instanceof Root_meta_pure_metamodel_dataSpace_DataSpacePackageableElementExecutable)
                {
                    if (((Root_meta_pure_metamodel_dataSpace_DataSpacePackageableElementExecutable)executable)._executable() instanceof Root_meta_legend_service_metamodel_Service ||
                            ((Root_meta_pure_metamodel_dataSpace_DataSpacePackageableElementExecutable)executable)._executable() instanceof ConcreteFunctionDefinition)
                    {
                        DataSpaceExecutableAnalysisResult executableAnalysisResult = new DataSpaceExecutableAnalysisResult();
                        executableAnalysisResult.title = executable._title();
                        executableAnalysisResult.description = executable._description();
                        String executablePath = HelperModelBuilder.getElementFullPath(((Root_meta_pure_metamodel_dataSpace_DataSpacePackageableElementExecutable) executable)._executable(), pureModel.getExecutionSupport());
                        executableAnalysisResult.executable = executablePath;
                        org.finos.legend.engine.protocol.pure.m3.PackageableElement _el = ListIterate.detect(pureModelContextData.getElements(), el -> el.getPath().equals(executablePath) && (el instanceof Service || el instanceof Function));
                        Mapping mapping = null;
                        Root_meta_core_runtime_Runtime runtime = null;
                        FunctionDefinition<?> lambdaFunc = null;
                        if (_el instanceof Service)
                        {
                            Service serviceProtocol = (Service) _el;
                            Root_meta_legend_service_metamodel_Service service = (Root_meta_legend_service_metamodel_Service)  ((Root_meta_pure_metamodel_dataSpace_DataSpacePackageableElementExecutable) executable)._executable();
                            if (service._execution() instanceof Root_meta_legend_service_metamodel_PureSingleExecution)
                            {
                                Root_meta_legend_service_metamodel_PureSingleExecution execution = ((Root_meta_legend_service_metamodel_PureSingleExecution) service._execution());
                                DataSpaceServiceExecutableInfo serviceExecutableInfo = new DataSpaceServiceExecutableInfo();
                                serviceExecutableInfo.pattern = service._pattern();
                                serviceExecutableInfo.id = executable._id();
                                serviceExecutableInfo.executionContextKey = executable._executionContextKey();
                                serviceExecutableInfo.query = ((PureSingleExecution) serviceProtocol.execution).func.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build());
                                serviceExecutableInfo.mapping = HelperModelBuilder.getElementFullPath(execution._mapping(), pureModel.getExecutionSupport());
                                if (serviceProtocol.execution instanceof PureSingleExecution && ((PureSingleExecution) serviceProtocol.execution).runtime instanceof RuntimePointer)
                                {
                                    serviceExecutableInfo.runtime = pureModel.getRuntimePath(execution._runtime());
                                }
                                if (buildResult)
                                {
                                    serviceExecutableInfo.datasets = LazyIterate.flatCollect(entitlementServiceExtensions, extension -> extension.generateDatasetSpecifications(null, pureModel.getRuntimePath(execution._runtime()), execution._runtime(), HelperModelBuilder.getElementFullPath(execution._mapping(), pureModel.getExecutionSupport()), execution._mapping(), pureModelContextData, pureModel)).toList();
                                }
                                executableAnalysisResult.info = serviceExecutableInfo;
                                lambdaFunc = execution._func();
                                mapping = execution._mapping();
                                runtime = execution._runtime();
                            }
                            else if (service._execution() instanceof Root_meta_legend_service_metamodel_PureMultiExecution)
                            {
                                Root_meta_legend_service_metamodel_PureMultiExecution execution = ((Root_meta_legend_service_metamodel_PureMultiExecution) service._execution());
                                DataSpaceMultiExecutionServiceExecutableInfo multiExecutionServiceExecutableInfo = new DataSpaceMultiExecutionServiceExecutableInfo();
                                multiExecutionServiceExecutableInfo.pattern = service._pattern();
                                multiExecutionServiceExecutableInfo.id = executable._id();
                                multiExecutionServiceExecutableInfo.executionContextKey = executable._executionContextKey();
                                multiExecutionServiceExecutableInfo.query = ((PureMultiExecution) serviceProtocol.execution).func.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build());
                                multiExecutionServiceExecutableInfo.keyedExecutableInfos = new ArrayList<>();
                                for (Root_meta_legend_service_metamodel_KeyedExecutionParameter keyedExecutionParameter: execution._executionParameters())
                                {
                                    DataSpaceMultiExecutionServiceKeyedExecutableInfo keyedExecutableInfo = new DataSpaceMultiExecutionServiceKeyedExecutableInfo();
                                    keyedExecutableInfo.key = keyedExecutionParameter._key();
                                    keyedExecutableInfo.mapping = HelperModelBuilder.getElementFullPath(keyedExecutionParameter._mapping(), pureModel.getExecutionSupport());
                                    keyedExecutableInfo.runtime = pureModel.getRuntimePath(keyedExecutionParameter._runtime());
                                    if (buildResult)
                                    {
                                        keyedExecutableInfo.datasets = LazyIterate.flatCollect(entitlementServiceExtensions, extension -> extension.generateDatasetSpecifications(null, pureModel.getRuntimePath(keyedExecutionParameter._runtime()), keyedExecutionParameter._runtime(), HelperModelBuilder.getElementFullPath(keyedExecutionParameter._mapping(), pureModel.getExecutionSupport()), keyedExecutionParameter._mapping(), pureModelContextData, pureModel)).toList();
                                    }
                                    multiExecutionServiceExecutableInfo.keyedExecutableInfos.add(keyedExecutableInfo);
                                }
                                //for multi execution, we use the first execution to generate execution plan
                                lambdaFunc = execution._func();
                                mapping = execution._executionParameters().getFirst()._mapping();
                                runtime = execution._executionParameters().getFirst()._runtime();
                            }
                        }
                        else if (_el instanceof Function)
                        {
                            DataSpaceFunctionPointerExecutableInfo functionPointerExecutableInfo = new DataSpaceFunctionPointerExecutableInfo();
                            functionPointerExecutableInfo.id = executable._id();
                            functionPointerExecutableInfo.executionContextKey = executable._executionContextKey();
                            functionPointerExecutableInfo.function = executablePath;
                            lambdaFunc = pureModel.getConcreteFunctionDefinition_safe(executablePath);
                            Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext executionContext = executable._executionContextKey() == null ? dataSpace._defaultExecutionContext() :
                                    dataSpace._executionContexts().toList().stream().filter(c -> c._name().equals(executable._executionContextKey())).findFirst().get();
                            mapping = executionContext._mapping();
                            runtime = executionContext._defaultRuntime()._runtimeValue();
                            Lambda lambda = new Lambda();
                            lambda.body = new ArrayList<>();
                            lambda.body.addAll(((Function) _el).body);
                            lambda.parameters = new ArrayList<>();
                            lambda.parameters.addAll(((Function) _el).parameters);
                            functionPointerExecutableInfo.query = lambda.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withIndentation(getTabSize(1)).build());
                            executableAnalysisResult.info = functionPointerExecutableInfo;
                        }
                        else
                        {
                            throw new RuntimeException("Can't find protocol for service or function '" + executablePath + "'");
                        }
                        if (buildResult)
                        {
                            executableAnalysisResult.result = buildExecutableResult(PlanGenerator.generateExecutionPlanDebug(
                                    lambdaFunc,
                                    mapping,
                                    runtime,
                                    HelperValueSpecificationBuilder.processExecutionContext(new BaseExecutionContext(), pureModel.getContext()),
                                    pureModel,
                                    PureClientVersions.production,
                                    PlanPlatform.JAVA,
                                    null,
                                    PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport())),
                                    generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers)
                            ).plan.rootExecutionNode.resultType);
                        }
                        dataSpaceExecutionContextAnalysisResults.add(executableAnalysisResult);
                    }
                }
                else
                {
                    throw new UnsupportedOperationException();
                }
                // TODO: when Executable is ready, we will handle it here
            });
        }
        return dataSpaceExecutionContextAnalysisResults;
    }

    private static MappingModelCoverageAnalysisResult buildMappingModelCoverageAnalysisResult(Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult mappingModelCoverageAnalysisResult, DataSpaceExecutionContextAnalysisResult excResult, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, String clientVersion, MutableList<PlanGeneratorExtension> generatorExtensions, List<EntitlementServiceExtension> entitlementServiceExtensions, boolean returnDataSets, boolean returnLightPMCD)
    {
        try
        {
            MappingModelCoverageAnalysisResult mappingModelCoverageAnalysisResultProtocol = DataSpaceAnalyticsHelper.objectMapper.readValue(core_analytics_mapping_modelCoverage_serializer.Root_meta_analytics_mapping_modelCoverage_serialization_json_getSerializedMappingModelCoverageAnalysisResult_MappingModelCoverageAnalysisResult_1__String_1_(mappingModelCoverageAnalysisResult, pureModel.getExecutionSupport()), MappingModelCoverageAnalysisResult.class);
            if (returnDataSets)
            {
                excResult.datasets = LazyIterate.flatCollect(entitlementServiceExtensions, extension -> extension.generateDatasetSpecifications(null, excResult.defaultRuntime, pureModel.getRuntime(excResult.defaultRuntime), excResult.mapping, pureModel.getMapping(excResult.mapping), pureModelContextData, pureModel)).toList();
            }
            if (returnLightPMCD)
            {
                PureModelContextData.Builder builder = PureModelContextData.newBuilder();
                MutableList<Enumeration<? extends Enum>> coveredEnumerations = org.eclipse.collections.impl.factory.Lists.mutable.ofAll(mappingModelCoverageAnalysisResult._enumerations().toList().stream().collect(Collectors.toList()));
                PureModelContextData classes = PureModelContextDataGenerator.generatePureModelContextDataFromClasses(mappingModelCoverageAnalysisResult._classes().toList(), clientVersion, pureModel.getExecutionSupport());
                PureModelContextData enums = PureModelContextDataGenerator.generatePureModelContextDataFromEnumerations(coveredEnumerations, clientVersion, pureModel.getExecutionSupport());
                PureModelContextData _profiles = PureModelContextDataGenerator.generatePureModelContextDataFromProfile((RichIterable<Profile>) mappingModelCoverageAnalysisResult._profiles(), clientVersion, pureModel.getExecutionSupport());
                PureModelContextData associations = PureModelContextDataGenerator.generatePureModelContextDataFromAssociations(mappingModelCoverageAnalysisResult._associations(), clientVersion, pureModel.getExecutionSupport());
                mappingModelCoverageAnalysisResultProtocol.model = builder.build().combine(classes).combine(enums).combine(_profiles).combine(associations);
            }
            return mappingModelCoverageAnalysisResultProtocol;
        }
        catch (Exception ignored)
        {
        }
        return null;
    }


    public static DataSpaceAnalysisResult analyzeDataSpaceCoverage(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, String clientVersion, MutableList<PlanGeneratorExtension> generatorExtensions, List<EntitlementServiceExtension> entitlementServiceExtensions, boolean returnLightGraph)
    {
        Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceCoverageAnalysisResult analysisResult = core_data_space_analytics_analytics.Root_meta_pure_metamodel_dataSpace_analytics_analyzeDataSpaceCoverage_DataSpace_1__PackageableRuntime_MANY__Boolean_1__DataSpaceCoverageAnalysisResult_1_(
                dataSpace,
                ListIterate.selectInstancesOf(pureModelContextData.getElements(), PackageableRuntime.class).collect(runtime -> pureModel.getPackageableRuntime(runtime.getPath(), runtime.sourceInformation)),
                returnLightGraph,
                pureModel.getExecutionSupport()
        );

        DataSpaceAnalysisResult result = new DataSpaceAnalysisResult();
        result.name = dataSpaceProtocol.name;
        result._package = dataSpaceProtocol._package;
        result.path = dataSpaceProtocol.getPath();
        result.title = dataSpaceProtocol.title;
        result.description = dataSpaceProtocol.description;

        result.taggedValues = ListIterate.collect(dataSpace._taggedValues().toList(), taggedValue ->
        {
            DataSpaceTaggedValueInfo info = new DataSpaceTaggedValueInfo();
            info.profile = HelperModelBuilder.getElementFullPath(taggedValue._tag()._profile(), pureModel.getExecutionSupport());
            info.tag = taggedValue._tag()._value();
            info.value = taggedValue._value();
            return info;
        });

        result.stereotypes = ListIterate.collect(dataSpace._stereotypes().toList(), stereotype ->
        {
            DataSpaceStereotypeInfo info = new DataSpaceStereotypeInfo();
            info.profile = HelperModelBuilder.getElementFullPath(stereotype._profile(), pureModel.getExecutionSupport());
            info.value = stereotype._value();
            return info;
        });
        result.executionContexts = dataSpace._executionContexts().toList().collect(executionContext ->
        {
            Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceExecutionContextAnalysisResult executionContextAnalysisResult = analysisResult._executionContexts().detect(context -> context._name().equals(executionContext._name()));
            DataSpaceExecutionContextAnalysisResult excResult = new DataSpaceExecutionContextAnalysisResult();
            excResult.name = executionContext._name();
            excResult.title = executionContext._title();
            excResult.description = executionContext._description();
            excResult.mapping = HelperModelBuilder.getElementFullPath(executionContext._mapping(), pureModel.getExecutionSupport());
            excResult.defaultRuntime = HelperModelBuilder.getElementFullPath(executionContext._defaultRuntime(), pureModel.getExecutionSupport());
            excResult.compatibleRuntimes = ListIterate.collect(executionContextAnalysisResult._compatibleRuntimes().toList(), runtime -> HelperModelBuilder.getElementFullPath(runtime, pureModel.getExecutionSupport()));
            Optional<org.finos.legend.engine.protocol.pure.m3.PackageableElement> packageableRuntime = pureModelContextData.getElements().stream().filter(e -> e.getPath().equals(excResult.defaultRuntime) && e instanceof PackageableRuntime).findFirst();
            if (packageableRuntime.isPresent() && packageableRuntime.get() instanceof PackageableRuntime)
            {
                PackageableRuntime runtime = (PackageableRuntime) packageableRuntime.get();
                if (runtime.runtimeValue.connections != null && !runtime.runtimeValue.connections.isEmpty() && !runtime.runtimeValue.connections.get(0).storeConnections.isEmpty())
                {
                    String storePath = runtime.runtimeValue.connections.get(0).store.path;
                    Connection connection = runtime.runtimeValue.connections.get(0).storeConnections.get(0).connection;
                    if (connection instanceof ConnectionPointer)
                    {
                        String connectionPath = ((ConnectionPointer) connection).connection;
                        Optional<org.finos.legend.engine.protocol.pure.m3.PackageableElement> packageableConnection = pureModelContextData.getElements().stream().filter(e -> e.getPath().equals(connectionPath)).findAny();
                        DataSpaceExecutionContextRuntimeMetadata metadata = new DataSpaceExecutionContextRuntimeMetadata(storePath, connectionPath);
                        if (packageableConnection.isPresent() && packageableConnection.get() instanceof PackageableConnection && ((PackageableConnection) packageableConnection.get()).connectionValue instanceof RelationalDatabaseConnection)
                        {
                            metadata.connectionType = ((RelationalDatabaseConnection) ((PackageableConnection) packageableConnection.get()).connectionValue).type.name();
                        }
                        excResult.runtimeMetadata = metadata;
                    }
                    else if (connection instanceof RelationalDatabaseConnection)
                    {
                        DataSpaceExecutionContextRuntimeMetadata metadata = new DataSpaceExecutionContextRuntimeMetadata(storePath, null);
                        metadata.connectionType = ((RelationalDatabaseConnection) connection).type.name();
                        excResult.runtimeMetadata = metadata;
                    }
                }
            }
            Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult mappingModelCoverageAnalysisResult = executionContextAnalysisResult._mappingCoverage();
            if (result.mappingToMappingCoverageResult == null)
            {
                result.mappingToMappingCoverageResult = new HashMap<>();
                result.mappingToMappingCoverageResult.put(excResult.mapping, buildMappingModelCoverageAnalysisResult(mappingModelCoverageAnalysisResult, excResult, pureModel, dataSpaceProtocol, pureModelContextData, clientVersion, generatorExtensions, entitlementServiceExtensions, false, returnLightGraph));
            }
            else if (!result.mappingToMappingCoverageResult.containsKey(excResult.mapping))
            {
                result.mappingToMappingCoverageResult.put(excResult.mapping, buildMappingModelCoverageAnalysisResult(mappingModelCoverageAnalysisResult, excResult, pureModel, dataSpaceProtocol, pureModelContextData, clientVersion, generatorExtensions, entitlementServiceExtensions, false, returnLightGraph));
            }
            return excResult;
        });
        result.defaultExecutionContext = dataSpace._defaultExecutionContext()._name();
        // executables
        result.executables = buildDataSpaceExecutableAnalysisResult(dataSpace, pureModel, dataSpaceProtocol, pureModelContextData, entitlementServiceExtensions, generatorExtensions, false);
        // elements
        result.elements = dataSpace._elements() != null ? dataSpace._elements().toList().collect(el -> HelperModelBuilder.getElementFullPath(el, pureModel.getExecutionSupport())) : Lists.mutable.empty();
        // support
        result.supportInfo = dataSpaceProtocol.supportInfo;
        if (result.supportInfo != null)
        {
            result.supportInfo.sourceInformation = null;
        }

        return result;
    }


    public static DataSpaceAnalysisResult analyzeDataSpace(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, String clientVersion, boolean returnLightGraph)
    {
        boolean isDataSpaceInDev = dataSpace._stereotypes().anySatisfy(stereotype -> stereotype._profile()._name().equals("devStatus") && stereotype._profile()._p_stereotypes().anySatisfy(s -> s._value().equals("inProgress")));
        return analyzeDataSpace(dataSpace, pureModel, dataSpaceProtocol, pureModelContextData, clientVersion, Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class)), EntitlementServiceExtensionLoader.extensions(), returnLightGraph && !isDataSpaceInDev);
    }

    public static DataSpaceAnalysisResult analyzeDataSpace(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, String clientVersion, MutableList<PlanGeneratorExtension> generatorExtensions, List<EntitlementServiceExtension> entitlementServiceExtensions, boolean returnLightGraph)
    {
        Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceAnalysisResult analysisResult = core_data_space_analytics_analytics.Root_meta_pure_metamodel_dataSpace_analytics_analyzeDataSpace_DataSpace_1__PackageableRuntime_MANY__Boolean_1__DataSpaceAnalysisResult_1_(
                dataSpace,
                ListIterate.selectInstancesOf(pureModelContextData.getElements(), PackageableRuntime.class).collect(runtime -> pureModel.getPackageableRuntime(runtime.getPath(), runtime.sourceInformation)),
                returnLightGraph,
                pureModel.getExecutionSupport()
        );

        DataSpaceAnalysisResult result = new DataSpaceAnalysisResult();
        result.name = dataSpaceProtocol.name;
        result._package = dataSpaceProtocol._package;
        result.path = dataSpaceProtocol.getPath();
        result.title = dataSpaceProtocol.title;
        result.description = dataSpaceProtocol.description;

        result.taggedValues = ListIterate.collect(dataSpace._taggedValues().toList(), taggedValue ->
        {
            DataSpaceTaggedValueInfo info = new DataSpaceTaggedValueInfo();
            info.profile = HelperModelBuilder.getElementFullPath(taggedValue._tag()._profile(), pureModel.getExecutionSupport());
            info.tag = taggedValue._tag()._value();
            info.value = taggedValue._value();
            return info;
        });

        result.stereotypes = ListIterate.collect(dataSpace._stereotypes().toList(), stereotype ->
        {
            DataSpaceStereotypeInfo info = new DataSpaceStereotypeInfo();
            info.profile = HelperModelBuilder.getElementFullPath(stereotype._profile(), pureModel.getExecutionSupport());
            info.value = stereotype._value();
            return info;
        });

        result.executionContexts = dataSpace._executionContexts().toList().collect(executionContext ->
        {
            Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceExecutionContextAnalysisResult executionContextAnalysisResult = analysisResult._executionContexts().detect(context -> context._name().equals(executionContext._name()));
            DataSpaceExecutionContextAnalysisResult excResult = new DataSpaceExecutionContextAnalysisResult();
            excResult.name = executionContext._name();
            excResult.title = executionContext._title();
            excResult.description = executionContext._description();
            excResult.mapping = HelperModelBuilder.getElementFullPath(executionContext._mapping(), pureModel.getExecutionSupport());
            excResult.defaultRuntime = HelperModelBuilder.getElementFullPath(executionContext._defaultRuntime(), pureModel.getExecutionSupport());
            Optional<org.finos.legend.engine.protocol.pure.m3.PackageableElement> packageableRuntime = pureModelContextData.getElements().stream().filter(e -> e.getPath().equals(excResult.defaultRuntime) && e instanceof PackageableRuntime).findFirst();
            if (packageableRuntime.isPresent() && packageableRuntime.get() instanceof PackageableRuntime)
            {
                PackageableRuntime runtime = (PackageableRuntime) packageableRuntime.get();
                if (runtime.runtimeValue.connections != null && !runtime.runtimeValue.connections.isEmpty() && !runtime.runtimeValue.connections.get(0).storeConnections.isEmpty())
                {
                    String storePath = runtime.runtimeValue.connections.get(0).store.path;
                    Connection connection = runtime.runtimeValue.connections.get(0).storeConnections.get(0).connection;
                    if (connection instanceof ConnectionPointer)
                    {
                        String connectionPath = ((ConnectionPointer) connection).connection;
                        Optional<org.finos.legend.engine.protocol.pure.m3.PackageableElement> packageableConnection = pureModelContextData.getElements().stream().filter(e -> e.getPath().equals(connectionPath)).findAny();
                        DataSpaceExecutionContextRuntimeMetadata metadata = new DataSpaceExecutionContextRuntimeMetadata(storePath, connectionPath);
                        if (packageableConnection.isPresent() && packageableConnection.get() instanceof PackageableConnection && ((PackageableConnection) packageableConnection.get()).connectionValue instanceof RelationalDatabaseConnection)
                        {
                            metadata.connectionType = ((RelationalDatabaseConnection) ((PackageableConnection) packageableConnection.get()).connectionValue).type.name();
                        }
                        excResult.runtimeMetadata = metadata;
                    }
                    else if (connection instanceof RelationalDatabaseConnection)
                    {
                        DataSpaceExecutionContextRuntimeMetadata metadata = new DataSpaceExecutionContextRuntimeMetadata(storePath, null);
                        metadata.connectionType = ((RelationalDatabaseConnection) connection).type.name();
                        excResult.runtimeMetadata = metadata;
                    }
                }
            }
            excResult.compatibleRuntimes = ListIterate.collect(executionContextAnalysisResult._compatibleRuntimes().toList(), runtime -> HelperModelBuilder.getElementFullPath(runtime, pureModel.getExecutionSupport()));
            Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult mappingModelCoverageAnalysisResult = executionContextAnalysisResult._mappingCoverage();
            if (result.mappingToMappingCoverageResult == null)
            {
                result.mappingToMappingCoverageResult = new HashMap<>();
                result.mappingToMappingCoverageResult.put(excResult.mapping, buildMappingModelCoverageAnalysisResult(mappingModelCoverageAnalysisResult, excResult, pureModel, dataSpaceProtocol, pureModelContextData, clientVersion, generatorExtensions, entitlementServiceExtensions, true, returnLightGraph));
            }
            else if (!result.mappingToMappingCoverageResult.containsKey(excResult.mapping))
            {
                result.mappingToMappingCoverageResult.put(excResult.mapping, buildMappingModelCoverageAnalysisResult(mappingModelCoverageAnalysisResult, excResult, pureModel, dataSpaceProtocol, pureModelContextData, clientVersion, generatorExtensions, entitlementServiceExtensions, true, returnLightGraph));
            }
            return excResult;
        });
        result.defaultExecutionContext = dataSpace._defaultExecutionContext()._name();

        // diagrams
        result.diagrams = dataSpace._diagrams() != null ? ListIterate.collect(dataSpace._diagrams().toList(), diagram ->
        {
            DataSpaceDiagramAnalysisResult diagramAnalysisResult = new DataSpaceDiagramAnalysisResult();
            diagramAnalysisResult.title = diagram._title();
            diagramAnalysisResult.description = diagram._description();
            diagramAnalysisResult.diagram = HelperModelBuilder.getElementFullPath(diagram._diagram(), pureModel.getExecutionSupport());
            return diagramAnalysisResult;
        }) : Lists.mutable.empty();
        // NOTE: right now, we only build and do analysis for featured diagrams
        Root_meta_pure_metamodel_diagram_analytics_modelCoverage_DiagramModelCoverageAnalysisResult diagramAnalysisResult = analysisResult._diagramModels();
        PureModelContextData classes = PureModelContextDataGenerator.generatePureModelContextDataFromClasses(diagramAnalysisResult._classes(), clientVersion, pureModel.getExecutionSupport());
        PureModelContextData enums = PureModelContextDataGenerator.generatePureModelContextDataFromEnumerations(diagramAnalysisResult._enumerations(), clientVersion, pureModel.getExecutionSupport());
        PureModelContextData _profiles = PureModelContextDataGenerator.generatePureModelContextDataFromProfile((RichIterable<Profile>) diagramAnalysisResult._profiles(), clientVersion, pureModel.getExecutionSupport());
        PureModelContextData associations = PureModelContextDataGenerator.generatePureModelContextDataFromAssociations(diagramAnalysisResult._associations(), clientVersion, pureModel.getExecutionSupport());
        PureModelContextData.Builder builder = PureModelContextData.newBuilder();
        // add diagrams to model
        pureModelContextData.getElements().stream().filter(el -> ListIterate.collect(result.diagrams, diagram -> diagram.diagram).contains(el.getPath())).forEach(builder::addElement);
        result.model = builder.build().combine(classes).combine(enums).combine(_profiles).combine(associations);

        // elements
        result.elements = dataSpace._elements() != null ? dataSpace._elements().toList().collect(el -> HelperModelBuilder.getElementFullPath(el, pureModel.getExecutionSupport())) : Lists.mutable.empty();
        result.elementDocs = analysisResult._elementDocs().toList().collect(elementDoc ->
        {
            PackageableElement element = elementDoc._element();
            if (elementDoc instanceof Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceClassDocumentationEntry)
            {
                DataSpaceClassDocumentationEntry ed = new DataSpaceClassDocumentationEntry();
                ed.path = elementDoc._path();
                ed.name = elementDoc._name();
                ed.docs = new ArrayList<>(elementDoc._docs().toList());

                Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceClassDocumentationEntry doc = (Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceClassDocumentationEntry) elementDoc;
                ed.properties = doc._properties().toList().collect(DataSpaceAnalyticsHelper::buildPropertyDocumentationEntry);
                ed.milestoning = doc._milestoning();
                return ed;
            }
            else if (elementDoc instanceof Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceEnumerationDocumentationEntry)
            {
                DataSpaceEnumerationDocumentationEntry ed = new DataSpaceEnumerationDocumentationEntry();
                ed.path = elementDoc._path();
                ed.name = elementDoc._name();
                ed.docs = new ArrayList<>(elementDoc._docs().toList());

                Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceEnumerationDocumentationEntry doc = (Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceEnumerationDocumentationEntry) elementDoc;
                ed.enumValues = doc._enumValues().toList().collect(DataSpaceAnalyticsHelper::buildBasicDocumentationEntry);
                return ed;
            }
            else if (elementDoc instanceof Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceAssociationDocumentationEntry)
            {
                DataSpaceAssociationDocumentationEntry ed = new DataSpaceAssociationDocumentationEntry();
                ed.path = elementDoc._path();
                ed.name = elementDoc._name();
                ed.docs = new ArrayList<>(elementDoc._docs().toList());

                Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceAssociationDocumentationEntry doc = (Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceAssociationDocumentationEntry) elementDoc;
                ed.properties = doc._properties().toList().collect(DataSpaceAnalyticsHelper::buildPropertyDocumentationEntry);
                return ed;
            }
            DataSpaceModelDocumentationEntry ed = new DataSpaceModelDocumentationEntry();
            ed.path = elementDoc._path();
            ed.name = elementDoc._name();
            ed.docs = new ArrayList<>(elementDoc._docs().toList());
            return ed;
        });

        // executables
        result.executables = buildDataSpaceExecutableAnalysisResult(dataSpace, pureModel, dataSpaceProtocol, pureModelContextData, entitlementServiceExtensions, generatorExtensions, true);

        // support
        result.supportInfo = dataSpaceProtocol.supportInfo;
        if (result.supportInfo != null)
        {
            result.supportInfo.sourceInformation = null;
        }

        return result;
    }

}
