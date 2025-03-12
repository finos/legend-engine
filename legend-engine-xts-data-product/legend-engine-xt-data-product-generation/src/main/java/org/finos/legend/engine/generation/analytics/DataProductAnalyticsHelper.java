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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProduct;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProductTemplateExecutable;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
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


public class DataProductAnalyticsHelper
{
    private static final ObjectMapper objectMapper = getNewObjectMapper();

    public static ObjectMapper getNewObjectMapper()
    {
        return EntitlementModelObjectMapperFactory.withEntitlementModelExtensions(withStandardConfigurations(PureProtocolObjectMapperFactory.withPureProtocolExtensions(new ObjectMapper())));
    }

    private static DataProductBasicDocumentationEntry buildBasicDocumentationEntry(Root_meta_pure_metamodel_dataProduct_analytics_DataProductBasicDocumentationEntry entry)
    {
        DataProductBasicDocumentationEntry docEntry = new DataProductBasicDocumentationEntry();
        docEntry.name = entry._name();
        docEntry.docs = new ArrayList<>(entry._docs().toList());
        return docEntry;
    }

    private static DataProductPropertyDocumentationEntry buildPropertyDocumentationEntry(Root_meta_pure_metamodel_dataProduct_analytics_DataProductPropertyDocumentationEntry entry)
    {
        DataProductPropertyDocumentationEntry docEntry = new DataProductPropertyDocumentationEntry();
        docEntry.name = entry._name();
        docEntry.docs = new ArrayList<>(entry._docs().toList());
        docEntry.milestoning = entry._milestoning();
        docEntry.type = entry._type();
        docEntry.multiplicity = new Multiplicity(entry._multiplicity()._lowerBound()._value().intValue(), entry._multiplicity()._upperBound()._value() != null ? entry._multiplicity()._upperBound()._value().intValue() : null);
        return docEntry;
    }

    private static DataProductExecutableResult buildExecutableResult(ResultType resultType)
    {
        if (resultType instanceof TDSResultType)
        {
            DataProductExecutableTDSResult result = new DataProductExecutableTDSResult();
            result.columns = (((TDSResultType) resultType).tdsColumns).stream().map(tdsColumn ->
            {
                DataProductExecutableTDSResultColumn column = new DataProductExecutableTDSResultColumn();
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

    private static List<DataProductExecutableAnalysisResult> buildDataProductExecutableAnalysisResult(Root_meta_pure_metamodel_dataProduct_DataProduct dataProduct, PureModel pureModel, DataProduct dataProductProtocol, PureModelContextData pureModelContextData, List<EntitlementServiceExtension> entitlementServiceExtensions, MutableList<PlanGeneratorExtension> generatorExtensions, boolean buildResult)
    {
        List<DataProductExecutableAnalysisResult> dataProductExecutionContextAnalysisResults = new ArrayList<>();
        if (dataProduct._executables() != null)
        {
            dataProduct._executables().forEach((executable) ->
            {
                if (executable instanceof Root_meta_pure_metamodel_dataProduct_DataProductTemplateExecutable)
                {
                    DataProductExecutableAnalysisResult executableAnalysisResult = new DataProductExecutableAnalysisResult();
                    executableAnalysisResult.title = executable._title();
                    executableAnalysisResult.description = executable._description();
                    DataProductTemplateExecutableInfo templateExecutableInfo = new DataProductTemplateExecutableInfo();
                    templateExecutableInfo.id = executable._id();
                    // get V1 lambda
                    DataProductTemplateExecutable executableV1 = (DataProductTemplateExecutable) dataProductProtocol.executables.stream().filter(e -> e instanceof DataProductTemplateExecutable && e.id.equals(executable._id())).findFirst().get();
                    templateExecutableInfo.query = executableV1.query.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withIndentation(getTabSize(1)).build());
                    Root_meta_pure_metamodel_dataProduct_DataProductExecutionContext executionContext = executable._executionContextKey() == null ? dataProduct._defaultExecutionContext() :
                            dataProduct._executionContexts().toList().stream().filter(c -> c._name().equals(executable._executionContextKey())).findFirst().get();
                    templateExecutableInfo.executionContextKey = executable._executionContextKey() == null ? dataProduct._defaultExecutionContext()._name() : executable._executionContextKey();
                    executableAnalysisResult.info = templateExecutableInfo;
                    if (buildResult)
                    {
                        executableAnalysisResult.result = buildExecutableResult(PlanGenerator.generateExecutionPlanDebug(
                                ((Root_meta_pure_metamodel_dataProduct_DataProductTemplateExecutable) executable)._query(),
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
                    dataProductExecutionContextAnalysisResults.add(executableAnalysisResult);
                }
                else if (executable instanceof Root_meta_pure_metamodel_dataProduct_DataProductPackageableElementExecutable)
                {
                    if (((Root_meta_pure_metamodel_dataProduct_DataProductPackageableElementExecutable)executable)._executable() instanceof Root_meta_legend_service_metamodel_Service ||
                            ((Root_meta_pure_metamodel_dataProduct_DataProductPackageableElementExecutable)executable)._executable() instanceof ConcreteFunctionDefinition)
                    {
                        DataProductExecutableAnalysisResult executableAnalysisResult = new DataProductExecutableAnalysisResult();
                        executableAnalysisResult.title = executable._title();
                        executableAnalysisResult.description = executable._description();
                        String executablePath = HelperModelBuilder.getElementFullPath(((Root_meta_pure_metamodel_dataProduct_DataProductPackageableElementExecutable) executable)._executable(), pureModel.getExecutionSupport());
                        executableAnalysisResult.executable = executablePath;
                        org.finos.legend.engine.protocol.pure.m3.PackageableElement _el = ListIterate.detect(pureModelContextData.getElements(), el -> el.getPath().equals(executablePath) && (el instanceof Service || el instanceof Function));
                        Mapping mapping = null;
                        Root_meta_core_runtime_Runtime runtime = null;
                        FunctionDefinition<?> lambdaFunc = null;
                        if (_el instanceof Service)
                        {
                            Service serviceProtocol = (Service) _el;
                            Root_meta_legend_service_metamodel_Service service = (Root_meta_legend_service_metamodel_Service)  ((Root_meta_pure_metamodel_dataProduct_DataProductPackageableElementExecutable) executable)._executable();
                            if (service._execution() instanceof Root_meta_legend_service_metamodel_PureSingleExecution)
                            {
                                Root_meta_legend_service_metamodel_PureSingleExecution execution = ((Root_meta_legend_service_metamodel_PureSingleExecution) service._execution());
                                DataProductServiceExecutableInfo serviceExecutableInfo = new DataProductServiceExecutableInfo();
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
                                DataProductMultiExecutionServiceExecutableInfo multiExecutionServiceExecutableInfo = new DataProductMultiExecutionServiceExecutableInfo();
                                multiExecutionServiceExecutableInfo.pattern = service._pattern();
                                multiExecutionServiceExecutableInfo.id = executable._id();
                                multiExecutionServiceExecutableInfo.executionContextKey = executable._executionContextKey();
                                multiExecutionServiceExecutableInfo.query = ((PureMultiExecution) serviceProtocol.execution).func.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build());
                                multiExecutionServiceExecutableInfo.keyedExecutableInfos = new ArrayList<>();
                                for (Root_meta_legend_service_metamodel_KeyedExecutionParameter keyedExecutionParameter: execution._executionParameters())
                                {
                                    DataProductMultiExecutionServiceKeyedExecutableInfo keyedExecutableInfo = new DataProductMultiExecutionServiceKeyedExecutableInfo();
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
                            DataProductFunctionPointerExecutableInfo functionPointerExecutableInfo = new DataProductFunctionPointerExecutableInfo();
                            functionPointerExecutableInfo.id = executable._id();
                            functionPointerExecutableInfo.executionContextKey = executable._executionContextKey();
                            functionPointerExecutableInfo.function = executablePath;
                            lambdaFunc = pureModel.getConcreteFunctionDefinition_safe(executablePath);
                            Root_meta_pure_metamodel_dataProduct_DataProductExecutionContext executionContext = executable._executionContextKey() == null ? dataProduct._defaultExecutionContext() :
                                    dataProduct._executionContexts().toList().stream().filter(c -> c._name().equals(executable._executionContextKey())).findFirst().get();
                            mapping = executionContext._mapping();
                            runtime = executionContext._defaultRuntime()._runtimeValue();
                            LambdaFunction lambda = new LambdaFunction();
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
                        dataProductExecutionContextAnalysisResults.add(executableAnalysisResult);
                    }
                }
                else
                {
                    throw new UnsupportedOperationException();
                }
                // TODO: when Executable is ready, we will handle it here
            });
        }
        return dataProductExecutionContextAnalysisResults;
    }

    private static MappingModelCoverageAnalysisResult buildMappingModelCoverageAnalysisResult(Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult mappingModelCoverageAnalysisResult, DataProductExecutionContextAnalysisResult excResult, PureModel pureModel, DataProduct dataProductProtocol, PureModelContextData pureModelContextData, String clientVersion, MutableList<PlanGeneratorExtension> generatorExtensions, List<EntitlementServiceExtension> entitlementServiceExtensions, boolean returnDataSets, boolean returnLightPMCD)
    {
        try
        {
            MappingModelCoverageAnalysisResult mappingModelCoverageAnalysisResultProtocol = DataProductAnalyticsHelper.objectMapper.readValue(core_analytics_mapping_modelCoverage_serializer.Root_meta_analytics_mapping_modelCoverage_serialization_json_getSerializedMappingModelCoverageAnalysisResult_MappingModelCoverageAnalysisResult_1__String_1_(mappingModelCoverageAnalysisResult, pureModel.getExecutionSupport()), MappingModelCoverageAnalysisResult.class);
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


    public static DataProductAnalysisResult analyzeDataProductCoverage(Root_meta_pure_metamodel_dataProduct_DataProduct dataProduct, PureModel pureModel, DataProduct dataProductProtocol, PureModelContextData pureModelContextData, String clientVersion, MutableList<PlanGeneratorExtension> generatorExtensions, List<EntitlementServiceExtension> entitlementServiceExtensions, boolean returnLightGraph)
    {
        Root_meta_pure_metamodel_dataProduct_analytics_DataProductCoverageAnalysisResult analysisResult = core_data_space_analytics_analytics.Root_meta_pure_metamodel_dataProduct_analytics_analyzeDataProductCoverage_DataProduct_1__PackageableRuntime_MANY__Boolean_1__DataProductCoverageAnalysisResult_1_(
                dataProduct,
                ListIterate.selectInstancesOf(pureModelContextData.getElements(), PackageableRuntime.class).collect(runtime -> pureModel.getPackageableRuntime(runtime.getPath(), runtime.sourceInformation)),
                returnLightGraph,
                pureModel.getExecutionSupport()
        );

        DataProductAnalysisResult result = new DataProductAnalysisResult();
        result.name = dataProductProtocol.name;
        result._package = dataProductProtocol._package;
        result.path = dataProductProtocol.getPath();
        result.title = dataProductProtocol.title;
        result.description = dataProductProtocol.description;

        result.taggedValues = ListIterate.collect(dataProduct._taggedValues().toList(), taggedValue ->
        {
            DataProductTaggedValueInfo info = new DataProductTaggedValueInfo();
            info.profile = HelperModelBuilder.getElementFullPath(taggedValue._tag()._profile(), pureModel.getExecutionSupport());
            info.tag = taggedValue._tag()._value();
            info.value = taggedValue._value();
            return info;
        });

        result.stereotypes = ListIterate.collect(dataProduct._stereotypes().toList(), stereotype ->
        {
            DataProductStereotypeInfo info = new DataProductStereotypeInfo();
            info.profile = HelperModelBuilder.getElementFullPath(stereotype._profile(), pureModel.getExecutionSupport());
            info.value = stereotype._value();
            return info;
        });
        result.executionContexts = dataProduct._executionContexts().toList().collect(executionContext ->
        {
            Root_meta_pure_metamodel_dataProduct_analytics_DataProductExecutionContextAnalysisResult executionContextAnalysisResult = analysisResult._executionContexts().detect(context -> context._name().equals(executionContext._name()));
            DataProductExecutionContextAnalysisResult excResult = new DataProductExecutionContextAnalysisResult();
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
                        DataProductExecutionContextRuntimeMetadata metadata = new DataProductExecutionContextRuntimeMetadata(storePath, connectionPath);
                        if (packageableConnection.isPresent() && packageableConnection.get() instanceof PackageableConnection && ((PackageableConnection) packageableConnection.get()).connectionValue instanceof RelationalDatabaseConnection)
                        {
                            metadata.connectionType = ((RelationalDatabaseConnection) ((PackageableConnection) packageableConnection.get()).connectionValue).type.name();
                        }
                        excResult.runtimeMetadata = metadata;
                    }
                    else if (connection instanceof RelationalDatabaseConnection)
                    {
                        DataProductExecutionContextRuntimeMetadata metadata = new DataProductExecutionContextRuntimeMetadata(storePath, null);
                        metadata.connectionType = ((RelationalDatabaseConnection) connection).type.name();
                        excResult.runtimeMetadata = metadata;
                    }
                }
            }
            Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult mappingModelCoverageAnalysisResult = executionContextAnalysisResult._mappingCoverage();
            if (result.mappingToMappingCoverageResult == null)
            {
                result.mappingToMappingCoverageResult = new HashMap<>();
                result.mappingToMappingCoverageResult.put(excResult.mapping, buildMappingModelCoverageAnalysisResult(mappingModelCoverageAnalysisResult, excResult, pureModel, dataProductProtocol, pureModelContextData, clientVersion, generatorExtensions, entitlementServiceExtensions, false, returnLightGraph));
            }
            else if (!result.mappingToMappingCoverageResult.containsKey(excResult.mapping))
            {
                result.mappingToMappingCoverageResult.put(excResult.mapping, buildMappingModelCoverageAnalysisResult(mappingModelCoverageAnalysisResult, excResult, pureModel, dataProductProtocol, pureModelContextData, clientVersion, generatorExtensions, entitlementServiceExtensions, false, returnLightGraph));
            }
            return excResult;
        });
        result.defaultExecutionContext = dataProduct._defaultExecutionContext()._name();
        // executables
        result.executables = buildDataProductExecutableAnalysisResult(dataProduct, pureModel, dataProductProtocol, pureModelContextData, entitlementServiceExtensions, generatorExtensions, false);
        // elements
        result.elements = dataProduct._elements() != null ? dataProduct._elements().toList().collect(el -> HelperModelBuilder.getElementFullPath(el, pureModel.getExecutionSupport())) : Lists.mutable.empty();
        // support
        result.supportInfo = dataProductProtocol.supportInfo;
        if (result.supportInfo != null)
        {
            result.supportInfo.sourceInformation = null;
        }

        return result;
    }


    public static DataProductAnalysisResult analyzeDataProduct(Root_meta_pure_metamodel_dataProduct_DataProduct dataProduct, PureModel pureModel, DataProduct dataProductProtocol, PureModelContextData pureModelContextData, String clientVersion, boolean returnLightGraph)
    {
        boolean isDataProductInDev = dataProduct._stereotypes().anySatisfy(stereotype -> stereotype._profile()._name().equals("devStatus") && stereotype._profile()._p_stereotypes().anySatisfy(s -> s._value().equals("inProgress")));
        return analyzeDataProduct(dataProduct, pureModel, dataProductProtocol, pureModelContextData, clientVersion, Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class)), EntitlementServiceExtensionLoader.extensions(), returnLightGraph && !isDataProductInDev);
    }

    public static DataProductAnalysisResult analyzeDataProduct(Root_meta_pure_metamodel_dataProduct_DataProduct dataProduct, PureModel pureModel, DataProduct dataProductProtocol, PureModelContextData pureModelContextData, String clientVersion, MutableList<PlanGeneratorExtension> generatorExtensions, List<EntitlementServiceExtension> entitlementServiceExtensions, boolean returnLightGraph)
    {
        Root_meta_pure_metamodel_dataProduct_analytics_DataProductAnalysisResult analysisResult = core_data_space_analytics_analytics.Root_meta_pure_metamodel_dataProduct_analytics_analyzeDataProduct_DataProduct_1__PackageableRuntime_MANY__Boolean_1__DataProductAnalysisResult_1_(
                dataProduct,
                ListIterate.selectInstancesOf(pureModelContextData.getElements(), PackageableRuntime.class).collect(runtime -> pureModel.getPackageableRuntime(runtime.getPath(), runtime.sourceInformation)),
                returnLightGraph,
                pureModel.getExecutionSupport()
        );

        DataProductAnalysisResult result = new DataProductAnalysisResult();
        result.name = dataProductProtocol.name;
        result._package = dataProductProtocol._package;
        result.path = dataProductProtocol.getPath();
        result.title = dataProductProtocol.title;
        result.description = dataProductProtocol.description;

        result.taggedValues = ListIterate.collect(dataProduct._taggedValues().toList(), taggedValue ->
        {
            DataProductTaggedValueInfo info = new DataProductTaggedValueInfo();
            info.profile = HelperModelBuilder.getElementFullPath(taggedValue._tag()._profile(), pureModel.getExecutionSupport());
            info.tag = taggedValue._tag()._value();
            info.value = taggedValue._value();
            return info;
        });

        result.stereotypes = ListIterate.collect(dataProduct._stereotypes().toList(), stereotype ->
        {
            DataProductStereotypeInfo info = new DataProductStereotypeInfo();
            info.profile = HelperModelBuilder.getElementFullPath(stereotype._profile(), pureModel.getExecutionSupport());
            info.value = stereotype._value();
            return info;
        });

        result.executionContexts = dataProduct._executionContexts().toList().collect(executionContext ->
        {
            Root_meta_pure_metamodel_dataProduct_analytics_DataProductExecutionContextAnalysisResult executionContextAnalysisResult = analysisResult._executionContexts().detect(context -> context._name().equals(executionContext._name()));
            DataProductExecutionContextAnalysisResult excResult = new DataProductExecutionContextAnalysisResult();
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
                        DataProductExecutionContextRuntimeMetadata metadata = new DataProductExecutionContextRuntimeMetadata(storePath, connectionPath);
                        if (packageableConnection.isPresent() && packageableConnection.get() instanceof PackageableConnection && ((PackageableConnection) packageableConnection.get()).connectionValue instanceof RelationalDatabaseConnection)
                        {
                            metadata.connectionType = ((RelationalDatabaseConnection) ((PackageableConnection) packageableConnection.get()).connectionValue).type.name();
                        }
                        excResult.runtimeMetadata = metadata;
                    }
                    else if (connection instanceof RelationalDatabaseConnection)
                    {
                        DataProductExecutionContextRuntimeMetadata metadata = new DataProductExecutionContextRuntimeMetadata(storePath, null);
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
                result.mappingToMappingCoverageResult.put(excResult.mapping, buildMappingModelCoverageAnalysisResult(mappingModelCoverageAnalysisResult, excResult, pureModel, dataProductProtocol, pureModelContextData, clientVersion, generatorExtensions, entitlementServiceExtensions, true, returnLightGraph));
            }
            else if (!result.mappingToMappingCoverageResult.containsKey(excResult.mapping))
            {
                result.mappingToMappingCoverageResult.put(excResult.mapping, buildMappingModelCoverageAnalysisResult(mappingModelCoverageAnalysisResult, excResult, pureModel, dataProductProtocol, pureModelContextData, clientVersion, generatorExtensions, entitlementServiceExtensions, true, returnLightGraph));
            }
            return excResult;
        });
        result.defaultExecutionContext = dataProduct._defaultExecutionContext()._name();

        // diagrams
        result.diagrams = dataProduct._diagrams() != null ? ListIterate.collect(dataProduct._diagrams().toList(), diagram ->
        {
            DataProductDiagramAnalysisResult diagramAnalysisResult = new DataProductDiagramAnalysisResult();
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
        result.elements = dataProduct._elements() != null ? dataProduct._elements().toList().collect(el -> HelperModelBuilder.getElementFullPath(el, pureModel.getExecutionSupport())) : Lists.mutable.empty();
        result.elementDocs = analysisResult._elementDocs().toList().collect(elementDoc ->
        {
            PackageableElement element = elementDoc._element();
            if (elementDoc instanceof Root_meta_pure_metamodel_dataProduct_analytics_DataProductClassDocumentationEntry)
            {
                DataProductClassDocumentationEntry ed = new DataProductClassDocumentationEntry();
                ed.path = elementDoc._path();
                ed.name = elementDoc._name();
                ed.docs = new ArrayList<>(elementDoc._docs().toList());

                Root_meta_pure_metamodel_dataProduct_analytics_DataProductClassDocumentationEntry doc = (Root_meta_pure_metamodel_dataProduct_analytics_DataProductClassDocumentationEntry) elementDoc;
                ed.properties = doc._properties().toList().collect(DataProductAnalyticsHelper::buildPropertyDocumentationEntry);
                ed.milestoning = doc._milestoning();
                return ed;
            }
            else if (elementDoc instanceof Root_meta_pure_metamodel_dataProduct_analytics_DataProductEnumerationDocumentationEntry)
            {
                DataProductEnumerationDocumentationEntry ed = new DataProductEnumerationDocumentationEntry();
                ed.path = elementDoc._path();
                ed.name = elementDoc._name();
                ed.docs = new ArrayList<>(elementDoc._docs().toList());

                Root_meta_pure_metamodel_dataProduct_analytics_DataProductEnumerationDocumentationEntry doc = (Root_meta_pure_metamodel_dataProduct_analytics_DataProductEnumerationDocumentationEntry) elementDoc;
                ed.enumValues = doc._enumValues().toList().collect(DataProductAnalyticsHelper::buildBasicDocumentationEntry);
                return ed;
            }
            else if (elementDoc instanceof Root_meta_pure_metamodel_dataProduct_analytics_DataProductAssociationDocumentationEntry)
            {
                DataProductAssociationDocumentationEntry ed = new DataProductAssociationDocumentationEntry();
                ed.path = elementDoc._path();
                ed.name = elementDoc._name();
                ed.docs = new ArrayList<>(elementDoc._docs().toList());

                Root_meta_pure_metamodel_dataProduct_analytics_DataProductAssociationDocumentationEntry doc = (Root_meta_pure_metamodel_dataProduct_analytics_DataProductAssociationDocumentationEntry) elementDoc;
                ed.properties = doc._properties().toList().collect(DataProductAnalyticsHelper::buildPropertyDocumentationEntry);
                return ed;
            }
            DataProductModelDocumentationEntry ed = new DataProductModelDocumentationEntry();
            ed.path = elementDoc._path();
            ed.name = elementDoc._name();
            ed.docs = new ArrayList<>(elementDoc._docs().toList());
            return ed;
        });

        // executables
        result.executables = buildDataProductExecutableAnalysisResult(dataProduct, pureModel, dataProductProtocol, pureModelContextData, entitlementServiceExtensions, generatorExtensions, true);

        // support
        result.supportInfo = dataProductProtocol.supportInfo;
        if (result.supportInfo != null)
        {
            result.supportInfo.sourceInformation = null;
        }

        return result;
    }

}
