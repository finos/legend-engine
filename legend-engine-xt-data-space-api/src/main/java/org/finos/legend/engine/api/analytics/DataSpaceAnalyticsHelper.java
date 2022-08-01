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

package org.finos.legend.engine.api.analytics;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.api.analytics.model.DataSpaceAnalysisInput;
import org.finos.legend.engine.api.analytics.model.DataSpaceAnalysisResult;
import org.finos.legend.engine.external.shared.format.imports.PureModelContextDataGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_analytics_modelCoverage_DiagramModelCoverageAnalysisResult;
import org.finos.legend.pure.generated.core_diagram_analytics_analytics;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;

import java.util.List;

public class DataSpaceAnalyticsHelper
{
    public static DataSpaceAnalysisResult analyzeDataSpace(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModel pureModel, DataSpace dataSpaceProtocol, PureModelContextData pureModelContextData, DataSpaceAnalysisInput input)
    {
        DataSpaceAnalysisResult result = new DataSpaceAnalysisResult();
        result.name = dataSpaceProtocol.name;
        result._package = dataSpaceProtocol._package;
        result.path = dataSpaceProtocol.getPath();
        result.title = dataSpaceProtocol.title;
        result.description = dataSpaceProtocol.description;

        result.taggedValues = ListIterate.collect(dataSpace._taggedValues().toList(), taggedValue ->
        {
            DataSpaceAnalysisResult.DataSpaceTaggedValueInfo info = new DataSpaceAnalysisResult.DataSpaceTaggedValueInfo();
            info.profile = HelperModelBuilder.getElementFullPath(taggedValue._tag()._profile(), pureModel.getExecutionSupport());
            info.tag = taggedValue._tag()._value();
            info.value = taggedValue._value();
            return info;
        });

        result.stereotypes = ListIterate.collect(dataSpace._stereotypes().toList(), stereotype ->
        {
            DataSpaceAnalysisResult.DataSpaceStereotypeInfo info = new DataSpaceAnalysisResult.DataSpaceStereotypeInfo();
            info.profile = HelperModelBuilder.getElementFullPath(stereotype._profile(), pureModel.getExecutionSupport());
            info.value = stereotype._value();
            return info;
        });

        // execution contexts
        result.executionContexts = dataSpace._executionContexts().toList().collect(executionContext ->
        {
            DataSpaceAnalysisResult.DataSpaceExecutionContextAnalysisResult executionContextAnalysisResult = new DataSpaceAnalysisResult.DataSpaceExecutionContextAnalysisResult();
            executionContextAnalysisResult.name = executionContext._name();
            executionContextAnalysisResult.description = executionContext._description();
            executionContextAnalysisResult.mapping = HelperModelBuilder.getElementFullPath(executionContext._mapping(), pureModel.getExecutionSupport());
            executionContextAnalysisResult.defaultRuntime = HelperModelBuilder.getElementFullPath(executionContext._defaultRuntime(), pureModel.getExecutionSupport());
            executionContextAnalysisResult.compatibleRuntimes = ListIterate.collect(HelperRuntimeBuilder.getMappingCompatibleRuntimes(
                    executionContext._mapping(),
                    ListIterate.selectInstancesOf(pureModelContextData.getElements(), PackageableRuntime.class),
                    pureModel), runtime -> HelperModelBuilder.getElementFullPath(runtime, pureModel.getExecutionSupport()));
            return executionContextAnalysisResult;
        });
        result.defaultExecutionContext = dataSpace._defaultExecutionContext()._name();

        // model
        // NOTE: right now, we only build and do analysis for featured diagrams
        Root_meta_pure_metamodel_diagram_analytics_modelCoverage_DiagramModelCoverageAnalysisResult diagramAnalysisResult = core_diagram_analytics_analytics.Root_meta_pure_metamodel_diagram_analytics_modelCoverage_getDiagramModelCoverage_Diagram_MANY__DiagramModelCoverageAnalysisResult_1_(dataSpace._featuredDiagrams(), pureModel.getExecutionSupport());
        PureModelContextData classes = PureModelContextDataGenerator.generatePureModelContextDataFromClasses(diagramAnalysisResult._classes(), input.clientVersion, pureModel.getExecutionSupport());
        PureModelContextData enums = PureModelContextDataGenerator.generatePureModelContextDataFromEnumerations(diagramAnalysisResult._enumerations(), input.clientVersion, pureModel.getExecutionSupport());
        PureModelContextData _profiles = PureModelContextDataGenerator.generatePureModelContextDataFromProfile((RichIterable<Profile>) diagramAnalysisResult._profiles(), input.clientVersion, pureModel.getExecutionSupport());
        PureModelContextData associations = PureModelContextDataGenerator.generatePureModelContextDataFromAssociations(diagramAnalysisResult._associations(), input.clientVersion, pureModel.getExecutionSupport());
        PureModelContextData.Builder builder = PureModelContextData.newBuilder();
        List<String> featuredDiagramPaths = dataSpace._featuredDiagrams() != null ? dataSpace._featuredDiagrams().collect(diagram -> HelperModelBuilder.getElementFullPath(diagram, pureModel.getExecutionSupport())).toList() : Lists.mutable.empty();
        pureModelContextData.getElements().stream().filter(el -> featuredDiagramPaths.contains(el.getPath())).forEach(builder::addElement);
        result.model = builder.build().combine(classes).combine(enums).combine(_profiles).combine(associations);

        // diagrams
        result.featuredDiagrams = dataSpace._featuredDiagrams() != null ? dataSpace._featuredDiagrams().toList().collect(diagram -> HelperModelBuilder.getElementFullPath(diagram, pureModel.getExecutionSupport())) : Lists.mutable.empty();

        // support
        result.supportInfo = dataSpaceProtocol.supportInfo;

        return result;
    }
}
