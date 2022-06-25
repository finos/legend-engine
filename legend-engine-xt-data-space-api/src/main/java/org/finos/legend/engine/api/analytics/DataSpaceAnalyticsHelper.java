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
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.api.analytics.model.DataSpaceAnalysisInput;
import org.finos.legend.engine.api.analytics.model.DataSpaceAnalysisResult;
import org.finos.legend.engine.external.shared.format.imports.PureModelContextDataGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_analytics_modelCoverage_DiagramModelCoverageAnalysisResult;
import org.finos.legend.pure.generated.core_diagram_analytics_analytics;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperDataSpaceBuilder.getExecutionContextCompatibleRuntimes;

public class DataSpaceAnalyticsHelper
{
    public static DataSpaceAnalysisResult analyzeDataSpace(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModelContextData pureModelContextData, PureModel pureModel, DataSpaceAnalysisInput input)
    {
        // diagrams
        // NOTE: right now, we only build and do analysis for featured diagrams
        Root_meta_pure_metamodel_diagram_analytics_modelCoverage_DiagramModelCoverageAnalysisResult result = core_diagram_analytics_analytics.Root_meta_pure_metamodel_diagram_analytics_modelCoverage_getDiagramModelCoverage_Diagram_MANY__DiagramModelCoverageAnalysisResult_1_(dataSpace._featuredDiagrams(), pureModel.getExecutionSupport());
        PureModelContextData classes = PureModelContextDataGenerator.generatePureModelContextDataFromClasses(result._classes(), input.clientVersion, pureModel.getExecutionSupport());
        PureModelContextData enums = PureModelContextDataGenerator.generatePureModelContextDataFromEnumerations(result._enumerations(), input.clientVersion, pureModel.getExecutionSupport());
        PureModelContextData _profiles = PureModelContextDataGenerator.generatePureModelContextDataFromProfile((RichIterable<Profile>) result._profiles(), input.clientVersion, pureModel.getExecutionSupport());
        PureModelContextData.Builder builder = PureModelContextData.newBuilder();
        List<String> featuredDiagramPaths = dataSpace._featuredDiagrams().collect(diagram -> HelperModelBuilder.getElementFullPath(diagram, pureModel.getExecutionSupport())).toList();
        pureModelContextData.getElements().stream().filter(el -> featuredDiagramPaths.contains(el.getPath())).forEach(builder::addElement);
        PureModelContextData diagramsModel = builder.build().combine(classes).combine(enums).combine(_profiles);

        // execution contexts
        Map<String, DataSpaceAnalysisResult.DataSpaceExecutionContextAnalysisResult> executionContexts = new HashMap<>();
        dataSpace._executionContexts().each(executionContext ->
        {
            executionContexts.put(executionContext._name(), new DataSpaceAnalysisResult.DataSpaceExecutionContextAnalysisResult(
                    ListIterate.collect(getExecutionContextCompatibleRuntimes(executionContext, ListIterate.selectInstancesOf(pureModelContextData.getElements(), PackageableRuntime.class), pureModel), runtime -> HelperModelBuilder.getElementFullPath(runtime, pureModel.getExecutionSupport()))));
        });

        return new DataSpaceAnalysisResult(diagramsModel, executionContexts);
    }
}
