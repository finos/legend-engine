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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.imports.PureModelContextDataGenerator;
import org.finos.legend.engine.generation.analytics.model.DataSpaceAnalysisResult;
import org.finos.legend.engine.generation.analytics.model.DataSpaceAssociationDocumentationEntry;
import org.finos.legend.engine.generation.analytics.model.DataSpaceBasicDocumentationEntry;
import org.finos.legend.engine.generation.analytics.model.DataSpaceClassDocumentationEntry;
import org.finos.legend.engine.generation.analytics.model.DataSpaceEnumerationDocumentationEntry;
import org.finos.legend.engine.generation.analytics.model.DataSpaceExecutionContextAnalysisResult;
import org.finos.legend.engine.generation.analytics.model.DataSpaceModelDocumentationEntry;
import org.finos.legend.engine.generation.analytics.model.DataSpaceStereotypeInfo;
import org.finos.legend.engine.generation.analytics.model.DataSpaceTaggedValueInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.analytics.model.MappingModelCoverageAnalysisResult;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceAnalysisResult;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceAssociationDocumentationEntry;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceBasicDocumentationEntry;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceClassDocumentationEntry;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceEnumerationDocumentationEntry;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceExecutionContextAnalysisResult;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_analytics_modelCoverage_DiagramModelCoverageAnalysisResult;
import org.finos.legend.pure.generated.core_analytics_mapping_modelCoverage_serializer;
import org.finos.legend.pure.generated.core_data_space_analytics_analytics;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;

import java.util.ArrayList;
import java.util.List;

public class DataSpaceAnalyticsHelper
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static DataSpaceBasicDocumentationEntry buildBasicDocumentationEntry(Root_meta_pure_metamodel_dataSpace_analytics_DataSpaceBasicDocumentationEntry entry)
    {
        DataSpaceBasicDocumentationEntry docEntry = new DataSpaceBasicDocumentationEntry();
        docEntry.name = entry._name();
        docEntry.docs = new ArrayList<>(entry._docs().toList());
        return docEntry;
    }

    public static DataSpaceAnalysisResult analyzeDataSpace(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, String clientVersion)
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
            excResult.description = executionContext._description();
            excResult.mapping = HelperModelBuilder.getElementFullPath(executionContext._mapping(), pureModel.getExecutionSupport());
            excResult.defaultRuntime = HelperModelBuilder.getElementFullPath(executionContext._defaultRuntime(), pureModel.getExecutionSupport());
            excResult.compatibleRuntimes = ListIterate.collect(executionContextAnalysisResult._compatibleRuntimes().toList(), runtime -> HelperModelBuilder.getElementFullPath(runtime, pureModel.getExecutionSupport()));
            Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult mappingModelCoverageAnalysisResult = executionContextAnalysisResult._mappingCoverage();
            try
            {
                excResult.mappingModelCoverageAnalysisResult = DataSpaceAnalyticsHelper.objectMapper.readValue(core_analytics_mapping_modelCoverage_serializer.Root_meta_analytics_mapping_modelCoverage_serialization_json_getSerializedMappingModelCoverageAnalysisResult_MappingModelCoverageAnalysisResult_1__String_1_(mappingModelCoverageAnalysisResult, pureModel.getExecutionSupport()), MappingModelCoverageAnalysisResult.class);
            }
            catch (Exception ignored)
            {
            }
            return excResult;
        });
        result.defaultExecutionContext = dataSpace._defaultExecutionContext()._name();

        // diagrams
        result.featuredDiagrams = dataSpace._featuredDiagrams() != null ? dataSpace._featuredDiagrams().toList().collect(diagram -> HelperModelBuilder.getElementFullPath(diagram, pureModel.getExecutionSupport())) : Lists.mutable.empty();
        // NOTE: right now, we only build and do analysis for featured diagrams
        Root_meta_pure_metamodel_diagram_analytics_modelCoverage_DiagramModelCoverageAnalysisResult diagramAnalysisResult = analysisResult._diagramModels();
        PureModelContextData classes = PureModelContextDataGenerator.generatePureModelContextDataFromClasses(diagramAnalysisResult._classes(), clientVersion, pureModel.getExecutionSupport());
        PureModelContextData enums = PureModelContextDataGenerator.generatePureModelContextDataFromEnumerations(diagramAnalysisResult._enumerations(), clientVersion, pureModel.getExecutionSupport());
        PureModelContextData _profiles = PureModelContextDataGenerator.generatePureModelContextDataFromProfile((RichIterable<Profile>) diagramAnalysisResult._profiles(), clientVersion, pureModel.getExecutionSupport());
        PureModelContextData associations = PureModelContextDataGenerator.generatePureModelContextDataFromAssociations(diagramAnalysisResult._associations(), clientVersion, pureModel.getExecutionSupport());
        PureModelContextData.Builder builder = PureModelContextData.newBuilder();
        List<String> featuredDiagramPaths = dataSpace._featuredDiagrams() != null ? dataSpace._featuredDiagrams().collect(diagram -> HelperModelBuilder.getElementFullPath(diagram, pureModel.getExecutionSupport())).toList() : Lists.mutable.empty();
        pureModelContextData.getElements().stream().filter(el -> featuredDiagramPaths.contains(el.getPath())).forEach(builder::addElement);
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
                ed.properties = doc._properties().toList().collect(DataSpaceAnalyticsHelper::buildBasicDocumentationEntry);
                ed.inheritedProperties = doc._inheritedProperties().toList().collect(DataSpaceAnalyticsHelper::buildBasicDocumentationEntry);
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
                ed.properties = doc._properties().toList().collect(DataSpaceAnalyticsHelper::buildBasicDocumentationEntry);
                return ed;
            }
            DataSpaceModelDocumentationEntry ed = new DataSpaceModelDocumentationEntry();
            ed.path = elementDoc._path();
            ed.name = elementDoc._name();
            ed.docs = new ArrayList<>(elementDoc._docs().toList());
            return ed;
        });

        // support
        result.supportInfo = dataSpaceProtocol.supportInfo;

        return result;
    }
}
