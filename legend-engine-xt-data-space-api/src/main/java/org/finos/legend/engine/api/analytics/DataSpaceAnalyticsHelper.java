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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.api.analytics.model.DataSpaceAnalysisResult;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_analytics_modelCoverage_DiagramModelCoverageAnalysisResult;
import org.finos.legend.pure.generated.core_diagram_analytics_analytics;

import java.util.HashMap;
import java.util.Map;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperDataSpaceBuilder.getExecutionContextCompatibleRuntimes;

public class DataSpaceAnalyticsHelper
{
    public static DataSpaceAnalysisResult analyzeDataSpace(Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace, PureModelContextData pureModelContextData, PureModel pureModel)
    {
        // diagrams
        Map<String, DataSpaceAnalysisResult.DataSpaceDiagramAnalysisResult> diagrams = new HashMap<>();
        dataSpace._featuredDiagrams().each(diagram ->
        {
            Root_meta_pure_metamodel_diagram_analytics_modelCoverage_DiagramModelCoverageAnalysisResult result = core_diagram_analytics_analytics.Root_meta_pure_metamodel_diagram_analytics_modelCoverage_getDiagramModelCoverage_Diagram_1__DiagramModelCoverageAnalysisResult_1_(diagram, pureModel.getExecutionSupport());
            diagrams.put(HelperModelBuilder.getElementFullPath(diagram, pureModel.getExecutionSupport()), new DataSpaceAnalysisResult.DataSpaceDiagramAnalysisResult(
                    result._profiles().collect(profile -> HelperModelBuilder.getElementFullPath(profile, pureModel.getExecutionSupport())).toList(),
                    result._enumerations().collect(enumeration -> HelperModelBuilder.getElementFullPath(enumeration, pureModel.getExecutionSupport())).toList(),
                    result._classes().collect(_class -> HelperModelBuilder.getElementFullPath(_class, pureModel.getExecutionSupport())).toList()));
        });

        // execution contexts
        Map<String, DataSpaceAnalysisResult.DataSpaceExecutionContextAnalysisResult> executionContexts = new HashMap<>();
        dataSpace._executionContexts().each(executionContext ->
        {
            executionContexts.put(executionContext._name(), new DataSpaceAnalysisResult.DataSpaceExecutionContextAnalysisResult(
                    ListIterate.collect(getExecutionContextCompatibleRuntimes(executionContext, ListIterate.selectInstancesOf(pureModelContextData.getElements(), PackageableRuntime.class), pureModel), runtime -> HelperModelBuilder.getElementFullPath(runtime, pureModel.getExecutionSupport()))));
        });

        return new DataSpaceAnalysisResult(diagrams, executionContexts);
    }
}
