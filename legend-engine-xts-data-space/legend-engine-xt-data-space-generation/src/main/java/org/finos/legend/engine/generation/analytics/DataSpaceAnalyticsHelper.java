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
import org.finos.legend.engine.language.pure.grammar.to.HelperValueSpecificationGrammarComposer;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceTemplateExecutable;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static MappingModelCoverageAnalysisResult buildMappingModelCoverageAnalysisResult(Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult mappingModelCoverageAnalysisResult, DataSpaceExecutionContextAnalysisResult excResult, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, String clientVersion, MutableList<PlanGeneratorExtension> generatorExtensions, List<EntitlementServiceExtension> entitlementServiceExtensions, Boolean returnDataSets, Boolean returnLightPMCD)
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

                // Here we prune the bindings to have just packageableIncludes part of ModelUnit
                // because we only need that as a part of analytics.
                List<String> bindingPaths = pureModelContextData.getElements().stream().filter(el -> el instanceof  Binding).map(b ->
                {
                    Binding _binding = new Binding();
                    _binding.name = b.name;
                    _binding.contentType = ((Binding) b).contentType;
                    _binding._package = b._package;
                    _binding.modelUnit = ((Binding) b).modelUnit;
                    _binding.modelUnit.packageableElementExcludes = org.eclipse.collections.api.factory.Lists.mutable.empty();
                    builder.addElement(_binding);
                    return b.getPath();
                }).collect(Collectors.toList());
                RichIterable<? extends Root_meta_external_format_shared_binding_Binding> bindings = org.eclipse.collections.api.factory.Lists.mutable.ofAll(bindingPaths.stream().map(path ->
                {
                    Root_meta_external_format_shared_binding_Binding binding;
                    try
                    {
                        binding = (Root_meta_external_format_shared_binding_Binding) pureModel.getPackageableElement(path);
                        return binding;
                    }
                    catch (Exception ignored)
                    {

                    }
                    return null;
                }).filter(c -> c != null).collect(Collectors.toList()));
                Root_meta_analytics_binding_modelCoverage_BindingModelCoverageAnalysisResult bindingAnalysisResult = core_analytics_binding_modelCoverage_analytics.Root_meta_analytics_binding_modelCoverage_getBindingModelCoverage_Binding_MANY__BindingModelCoverageAnalysisResult_1_(bindings, pureModel.getExecutionSupport());
                List<String> functionPaths = pureModelContextData.getElements().stream().filter(el -> el instanceof Function).map(e -> e.getPath()).collect(Collectors.toList());
                List<String> allExtraElements = functionPaths;
                allExtraElements.add(dataSpaceProtocol.getPath());
                pureModelContextData.getElements().stream().filter(el -> allExtraElements.contains(el.getPath())).forEach(builder::addElement);
                List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> elements = builder.build().getElements();
                RichIterable<? extends ConcreteFunctionDefinition<? extends  Object>> functions = org.eclipse.collections.api.factory.Lists.mutable.ofAll(functionPaths.stream().map(path ->
                {
                    ConcreteFunctionDefinition<? extends Object> function = null;
                    try
                    {
                        function = pureModel.getConcreteFunctionDefinition_safe(path);
                        if (function == null)
                        {
                            Function _function = (Function) elements.stream().filter(e -> e.getPath().equals(path)).findFirst().get();
                            function = pureModel.getConcreteFunctionDefinition_safe(path + HelperValueSpecificationGrammarComposer.getFunctionSignature(_function));

                        }
                        return function;
                    }
                    catch (Exception ignored)
                    {

                    }
                    return null;
                }).filter(c -> c != null).collect(Collectors.toList()));
                Root_meta_analytics_function_modelCoverage_FunctionModelCoverageAnalysisResult functionCoverageAnalysisResult = core_analytics_function_modelCoverage_analytics.Root_meta_analytics_function_modelCoverage_getFunctionModelCoverage_ConcreteFunctionDefinition_MANY__FunctionModelCoverageAnalysisResult_1_(org.eclipse.collections.impl.factory.Lists.mutable.ofAll(functions), pureModel.getExecutionSupport());
                MutableList<? extends Class<? extends Object>> coveredClasses = mappingModelCoverageAnalysisResult._classes().toList();
                List<String> coveredClassesPaths = coveredClasses.stream().map(c -> HelperModelBuilder.getElementFullPath(c, pureModel.getExecutionSupport())).collect(Collectors.toList());
                coveredClasses = org.eclipse.collections.impl.factory.Lists.mutable.ofAll(Stream.concat(Stream.concat(functionCoverageAnalysisResult._classes().toList().stream().filter(c -> !coveredClassesPaths.contains(HelperModelBuilder.getElementFullPath(c, pureModel.getExecutionSupport()))),
                        bindingAnalysisResult._classes().toList().stream().filter(c -> !coveredClassesPaths.contains(HelperModelBuilder.getElementFullPath(c, pureModel.getExecutionSupport())))).distinct(),
                        mappingModelCoverageAnalysisResult._classes().toList().stream()).collect(Collectors.toList()));
                MutableList<Enumeration<? extends Enum>> coveredEnumerations = org.eclipse.collections.impl.factory.Lists.mutable.ofAll(Stream.concat(mappingModelCoverageAnalysisResult._enumerations().toList().stream(), functionCoverageAnalysisResult._enumerations().toList().stream()).distinct().collect(Collectors.toList()));
                PureModelContextData classes = PureModelContextDataGenerator.generatePureModelContextDataFromClasses(coveredClasses, clientVersion, pureModel.getExecutionSupport());
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

    public static DataSpaceAnalysisResult analyzeDataSpace(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, String clientVersion)
    {
        return analyzeDataSpace(dataSpace, pureModel, dataSpaceProtocol, pureModelContextData, clientVersion, Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class)), EntitlementServiceExtensionLoader.extensions(), false);
    }

    public static DataSpaceAnalysisResult analyzeDataSpaceCoverage(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, String clientVersion, MutableList<PlanGeneratorExtension> generatorExtensions, List<EntitlementServiceExtension> entitlementServiceExtensions, Boolean returnLightGraph)
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
            Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult mappingModelCoverageAnalysisResult = executionContextAnalysisResult._mappingCoverage();
            excResult.mappingModelCoverageAnalysisResult = buildMappingModelCoverageAnalysisResult(mappingModelCoverageAnalysisResult, excResult, pureModel, dataSpaceProtocol, pureModelContextData, clientVersion, generatorExtensions, entitlementServiceExtensions, false, returnLightGraph);
            return excResult;
        });
        result.defaultExecutionContext = dataSpace._defaultExecutionContext()._name();
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

    public static DataSpaceAnalysisResult analyzeDataSpace(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, String clientVersion, MutableList<PlanGeneratorExtension> generatorExtensions, List<EntitlementServiceExtension> entitlementServiceExtensions, Boolean returnLightGraph)
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
            excResult.compatibleRuntimes = ListIterate.collect(executionContextAnalysisResult._compatibleRuntimes().toList(), runtime -> HelperModelBuilder.getElementFullPath(runtime, pureModel.getExecutionSupport()));
            Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult mappingModelCoverageAnalysisResult = executionContextAnalysisResult._mappingCoverage();
           excResult.mappingModelCoverageAnalysisResult = buildMappingModelCoverageAnalysisResult(mappingModelCoverageAnalysisResult, excResult, pureModel, dataSpaceProtocol, pureModelContextData, clientVersion, generatorExtensions, entitlementServiceExtensions, true, returnLightGraph);
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
                if (executable instanceof Root_meta_pure_metamodel_dataSpace_DataSpaceTemplateExecutable)
                {
                    DataSpaceExecutableAnalysisResult executableAnalysisResult = new DataSpaceExecutableAnalysisResult();
                    executableAnalysisResult.title = executable._title();
                    executableAnalysisResult.description = executable._description();
                    DataSpaceTemplateExecutableInfo templateExecutableInfo = new DataSpaceTemplateExecutableInfo();
                    templateExecutableInfo.id = ((Root_meta_pure_metamodel_dataSpace_DataSpaceTemplateExecutable) executable)._id();

                    // get V1 lambda
                    DataSpaceTemplateExecutable executableV1 = (DataSpaceTemplateExecutable) dataSpaceProtocol.executables.stream().filter(e -> e instanceof DataSpaceTemplateExecutable && ((DataSpaceTemplateExecutable) e).id.equals(((Root_meta_pure_metamodel_dataSpace_DataSpaceTemplateExecutable) executable)._id())).findFirst().get();
                    templateExecutableInfo.query = executableV1.query.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withIndentation(getTabSize(1)).build());

                    org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext executionContext = ((Root_meta_pure_metamodel_dataSpace_DataSpaceTemplateExecutable) executable)._executionContextKey() == null ? dataSpace._defaultExecutionContext() :
                            dataSpace._executionContexts().toList().stream().filter(c -> c._name().equals(((Root_meta_pure_metamodel_dataSpace_DataSpaceTemplateExecutable) executable)._executionContextKey())).findFirst().get();
                    templateExecutableInfo.executionContextKey = ((Root_meta_pure_metamodel_dataSpace_DataSpaceTemplateExecutable) executable)._executionContextKey() == null ? dataSpace._defaultExecutionContext()._name() : ((Root_meta_pure_metamodel_dataSpace_DataSpaceTemplateExecutable) executable)._executionContextKey();
                    executableAnalysisResult.info = templateExecutableInfo;
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
                    result.executables.add(executableAnalysisResult);
                }
                else if (executable instanceof Root_meta_pure_metamodel_dataSpace_DataSpacePackageableElementExecutable)
                {
                    if (((Root_meta_pure_metamodel_dataSpace_DataSpacePackageableElementExecutable)executable)._executable() instanceof Root_meta_legend_service_metamodel_Service)
                    {
                        Root_meta_legend_service_metamodel_Service service = (Root_meta_legend_service_metamodel_Service)  ((Root_meta_pure_metamodel_dataSpace_DataSpacePackageableElementExecutable) executable)._executable();
                        DataSpaceExecutableAnalysisResult executableAnalysisResult = new DataSpaceExecutableAnalysisResult();
                        executableAnalysisResult.title = executable._title();
                        executableAnalysisResult.description = executable._description();
                        String servicePath = HelperModelBuilder.getElementFullPath(((Root_meta_pure_metamodel_dataSpace_DataSpacePackageableElementExecutable) executable)._executable(), pureModel.getExecutionSupport());
                        executableAnalysisResult.executable = servicePath;
                        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement _el = ListIterate.detect(pureModelContextData.getElements(), el -> el.getPath().equals(servicePath) && el instanceof Service);
                        if (!(_el instanceof Service))
                        {
                            throw new RuntimeException("Can't find protocol for service '" + servicePath + "'");
                        }
                        Service serviceProtocol = (Service) _el;
                        Mapping mapping = null;
                        Root_meta_core_runtime_Runtime runtime = null;
                        LambdaFunction<?> lambdaFunc = null;
                        if (service._execution() instanceof Root_meta_legend_service_metamodel_PureSingleExecution)
                        {
                            Root_meta_legend_service_metamodel_PureSingleExecution execution = ((Root_meta_legend_service_metamodel_PureSingleExecution) service._execution());
                            DataSpaceServiceExecutableInfo serviceExecutableInfo = new DataSpaceServiceExecutableInfo();
                            serviceExecutableInfo.pattern = service._pattern();
                            serviceExecutableInfo.query = ((PureSingleExecution) serviceProtocol.execution).func.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build());
                            serviceExecutableInfo.mapping = HelperModelBuilder.getElementFullPath(execution._mapping(), pureModel.getExecutionSupport());
                            if (serviceProtocol.execution instanceof PureSingleExecution && ((PureSingleExecution) serviceProtocol.execution).runtime instanceof RuntimePointer)
                            {
                                serviceExecutableInfo.runtime = pureModel.getRuntimePath(execution._runtime());
                            }
                            serviceExecutableInfo.datasets = LazyIterate.flatCollect(entitlementServiceExtensions, extension -> extension.generateDatasetSpecifications(null, pureModel.getRuntimePath(execution._runtime()), execution._runtime(), HelperModelBuilder.getElementFullPath(execution._mapping(), pureModel.getExecutionSupport()), execution._mapping(), pureModelContextData, pureModel)).toList();
                            executableAnalysisResult.info = serviceExecutableInfo;
                            lambdaFunc = (LambdaFunction<?>) execution._func();
                            mapping = execution._mapping();
                            runtime = execution._runtime();
                        }
                        else if (service._execution() instanceof Root_meta_legend_service_metamodel_PureMultiExecution)
                        {
                            Root_meta_legend_service_metamodel_PureMultiExecution execution = ((Root_meta_legend_service_metamodel_PureMultiExecution) service._execution());
                            DataSpaceMultiExecutionServiceExecutableInfo multiExecutionServiceExecutableInfo = new DataSpaceMultiExecutionServiceExecutableInfo();
                            multiExecutionServiceExecutableInfo.pattern = service._pattern();
                            multiExecutionServiceExecutableInfo.query = ((PureMultiExecution) serviceProtocol.execution).func.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build());
                            multiExecutionServiceExecutableInfo.keyedExecutableInfos = new ArrayList<>();
                            for (Root_meta_legend_service_metamodel_KeyedExecutionParameter keyedExecutionParameter: execution._executionParameters())
                            {
                                DataSpaceMultiExecutionServiceKeyedExecutableInfo keyedExecutableInfo = new DataSpaceMultiExecutionServiceKeyedExecutableInfo();
                                keyedExecutableInfo.key = keyedExecutionParameter._key();
                                keyedExecutableInfo.mapping = HelperModelBuilder.getElementFullPath(keyedExecutionParameter._mapping(), pureModel.getExecutionSupport());
                                keyedExecutableInfo.runtime = pureModel.getRuntimePath(keyedExecutionParameter._runtime());
                                keyedExecutableInfo.datasets = LazyIterate.flatCollect(entitlementServiceExtensions, extension -> extension.generateDatasetSpecifications(null, pureModel.getRuntimePath(keyedExecutionParameter._runtime()), keyedExecutionParameter._runtime(), HelperModelBuilder.getElementFullPath(keyedExecutionParameter._mapping(), pureModel.getExecutionSupport()), keyedExecutionParameter._mapping(), pureModelContextData, pureModel)).toList();
                                multiExecutionServiceExecutableInfo.keyedExecutableInfos.add(keyedExecutableInfo);
                            }
                            //for multi execution, we use the first execution to generate execution plan
                            lambdaFunc = (LambdaFunction<?>) execution._func();
                            mapping = execution._executionParameters().getFirst()._mapping();
                            runtime = execution._executionParameters().getFirst()._runtime();
                        }
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
                        result.executables.add(executableAnalysisResult);
                    }
                }
                else
                {
                    throw new UnsupportedOperationException();
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
