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
import org.finos.legend.engine.external.shared.format.imports.PureModelContextDataGenerator;
import org.finos.legend.engine.generation.analytics.model.DataSpaceAnalysisResult;
import org.finos.legend.engine.generation.analytics.model.DataSpaceAssociationDocumentationEntry;
import org.finos.legend.engine.generation.analytics.model.DataSpaceBasicDocumentationEntry;
import org.finos.legend.engine.generation.analytics.model.DataSpaceClassDocumentationEntry;
import org.finos.legend.engine.generation.analytics.model.DataSpaceDiagramAnalysisResult;
import org.finos.legend.engine.generation.analytics.model.DataSpaceEnumerationDocumentationEntry;
import org.finos.legend.engine.generation.analytics.model.DataSpaceExecutableAnalysisResult;
import org.finos.legend.engine.generation.analytics.model.DataSpaceExecutableResult;
import org.finos.legend.engine.generation.analytics.model.DataSpaceExecutableTDSResult;
import org.finos.legend.engine.generation.analytics.model.DataSpaceExecutableTDSResultColumn;
import org.finos.legend.engine.generation.analytics.model.DataSpaceExecutionContextAnalysisResult;
import org.finos.legend.engine.generation.analytics.model.DataSpaceModelDocumentationEntry;
import org.finos.legend.engine.generation.analytics.model.DataSpacePropertyDocumentationEntry;
import org.finos.legend.engine.generation.analytics.model.DataSpaceServiceExecutableInfo;
import org.finos.legend.engine.generation.analytics.model.DataSpaceStereotypeInfo;
import org.finos.legend.engine.generation.analytics.model.DataSpaceTaggedValueInfo;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.pure.generated.Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_PureSingleExecution;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceAnalysisResult;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceAssociationDocumentationEntry;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceBasicDocumentationEntry;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceClassDocumentationEntry;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceEnumerationDocumentationEntry;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceExecutionContextAnalysisResult;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpacePropertyDocumentationEntry;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_analytics_modelCoverage_DiagramModelCoverageAnalysisResult;
import org.finos.legend.pure.generated.core_analytics_mapping_modelCoverage_serializer;
import org.finos.legend.pure.generated.core_data_space_analytics_analytics;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

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

    public static DataSpaceAnalysisResult analyzeDataSpace(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, String clientVersion)
    {
        return analyzeDataSpace(dataSpace, pureModel, dataSpaceProtocol, pureModelContextData, clientVersion, Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class)), EntitlementServiceExtensionLoader.extensions());
    }

    public static DataSpaceAnalysisResult analyzeDataSpace(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, String clientVersion, MutableList<PlanGeneratorExtension> generatorExtensions, List<EntitlementServiceExtension> entitlementServiceExtensions)
    {
        Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceAnalysisResult analysisResult = core_data_space_analytics_analytics.Root_meta_pure_metamodel_dataSpace_analytics_analyzeDataSpace_DataSpace_1__PackageableRuntime_MANY__DataSpaceAnalysisResult_1_(
                dataSpace,
                ListIterate.selectInstancesOf(pureModelContextData.getElements(), PackageableRuntime.class).collect(runtime -> pureModel.getPackageableRuntime(runtime.getPath(), runtime.sourceInformation)),
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

        // execution contexts
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
            Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult mappingModelCoverageAnalysisResult = executionContextAnalysisResult._mappingCoverage();
            try
            {
                excResult.mappingModelCoverageAnalysisResult = DataSpaceAnalyticsHelper.objectMapper.readValue(core_analytics_mapping_modelCoverage_serializer.Root_meta_analytics_mapping_modelCoverage_serialization_json_getSerializedMappingModelCoverageAnalysisResult_MappingModelCoverageAnalysisResult_1__String_1_(mappingModelCoverageAnalysisResult, pureModel.getExecutionSupport()), MappingModelCoverageAnalysisResult.class);
                excResult.datasets = LazyIterate.flatCollect(entitlementServiceExtensions, extension -> extension.generateDatasetSpecifications(null, excResult.defaultRuntime, pureModel.getRuntime(excResult.defaultRuntime), excResult.mapping, pureModel.getMapping(excResult.mapping), pureModelContextData, pureModel)).toList();
            }
            catch (Exception ignored)
            {
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
        result.executables = Lists.mutable.empty();
        if (dataSpace._executables() != null)
        {
            dataSpace._executables().forEach((executable) ->
            {
                if (executable._executable() instanceof Root_meta_legend_service_metamodel_Service)
                {
                    Root_meta_legend_service_metamodel_Service service = (Root_meta_legend_service_metamodel_Service) executable._executable();

                    // NOTE: right now we only support service with single execution for simplicity
                    if (service._execution() instanceof Root_meta_legend_service_metamodel_PureSingleExecution)
                    {
                        Root_meta_legend_service_metamodel_PureSingleExecution execution = ((Root_meta_legend_service_metamodel_PureSingleExecution) service._execution());

                        DataSpaceExecutableAnalysisResult executableAnalysisResult = new DataSpaceExecutableAnalysisResult();
                        executableAnalysisResult.title = executable._title();
                        executableAnalysisResult.description = executable._description();
                        String servicePath = HelperModelBuilder.getElementFullPath(executable._executable(), pureModel.getExecutionSupport());
                        executableAnalysisResult.executable = servicePath;

                        DataSpaceServiceExecutableInfo serviceExecutableInfo = new DataSpaceServiceExecutableInfo();
                        serviceExecutableInfo.pattern = service._pattern();
                        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement _el = ListIterate.detect(pureModelContextData.getElements(), el -> el.getPath().equals(servicePath) && el instanceof Service);
                        if (!(_el instanceof Service))
                        {
                            throw new RuntimeException("Can't find protocol for service '" + servicePath + "'");
                        }
                        Service serviceProtocol = (Service) _el;
                        serviceExecutableInfo.query = ((PureSingleExecution) serviceProtocol.execution).func.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build());
                        serviceExecutableInfo.mapping = HelperModelBuilder.getElementFullPath(execution._mapping(), pureModel.getExecutionSupport());
                        if (serviceProtocol.execution instanceof PureSingleExecution && ((PureSingleExecution) serviceProtocol.execution).runtime instanceof RuntimePointer)
                        {
                            serviceExecutableInfo.runtime = pureModel.getRuntimePath(execution._runtime());
                        }

                        executableAnalysisResult.info = serviceExecutableInfo;
                        executableAnalysisResult.result = buildExecutableResult(PlanGenerator.generateExecutionPlanDebug(
                                (LambdaFunction<?>) execution._func(),
                                execution._mapping(),
                                execution._runtime(),
                                HelperValueSpecificationBuilder.processExecutionContext(new BaseExecutionContext(), pureModel.getContext()),
                                pureModel,
                                PureClientVersions.production,
                                PlanPlatform.JAVA,
                                null,
                                generatorExtensions.flatCollect(e -> e.getExtraExtensions(pureModel)),
                                generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers)
                        ).plan.rootExecutionNode.resultType);
                        executableAnalysisResult.datasets = LazyIterate.flatCollect(entitlementServiceExtensions, extension -> extension.generateDatasetSpecifications(null, pureModel.getRuntimePath(execution._runtime()), execution._runtime(), HelperModelBuilder.getElementFullPath(execution._mapping(), pureModel.getExecutionSupport()), execution._mapping(), pureModelContextData, pureModel)).toList();
                        result.executables.add(executableAnalysisResult);
                    }
                }
                // TODO: when Executable is ready, we will handle it here
            });
        }

        // support
        result.supportInfo = dataSpaceProtocol.supportInfo;
        if (result.supportInfo != null)
        {
            result.supportInfo.sourceInformation = null;
        }

        return result;
    }
}
