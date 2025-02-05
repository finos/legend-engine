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

package org.finos.legend.engine.generation;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.generation.analytics.DataSpaceAnalyticsHelper;
import org.finos.legend.engine.generation.analytics.model.DataSpaceAnalysisResult;
import org.finos.legend.engine.generation.analytics.partition.MappingAnalysisCoveragePartition;
import org.finos.legend.engine.generation.analytics.partition.MappingModelCoveragePartition;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.analytics.model.MappingModelCoverageAnalysisResult;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

public class DataSpaceAnalyticsArtifactGenerationExtension implements ArtifactGenerationExtension
{
    public final String ROOT_PATH = "dataSpace-analytics";
    private boolean returnLightGraph = true;
    private final String MAIN_ANALYTICS_FILE = "AnalyticsResult.json";
    private final String MAPPING_MODEL_PREFIX = "MappingModel_";
    private final String MAPPING_ANALYSIS_FILE_PREFIX = "MappingAnalysis_";
    private final String ANALYTICS_FORMAT = "json";

    public static ObjectMapper objectMapper = DataSpaceAnalyticsHelper.getNewObjectMapper();

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "DataSpace");
    }

    @Override
    public String getKey()
    {
        return ROOT_PATH;
    }

    public void setReturnLightGraph(boolean returnLightGraph)
    {
        this.returnLightGraph = returnLightGraph;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
        return element instanceof Root_meta_pure_metamodel_dataSpace_DataSpace;
    }

    @Override
    public List<Artifact> generate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {

        String dataSpacePath = getElementFullPath(element, pureModel.getExecutionSupport());
        Assert.assertTrue(this.canGenerate(element), () -> "DataSpace analytics only supports dataSpace elements");
        Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace = (Root_meta_pure_metamodel_dataSpace_DataSpace) element;
        org.finos.legend.engine.protocol.pure.m3.PackageableElement dataSpaceProtocol = data.getElements().stream().filter(el -> dataSpacePath.equals(el.getPath())).findFirst().orElse(null);
        Assert.assertTrue(dataSpaceProtocol instanceof DataSpace, () -> "Can't find data space '" + dataSpacePath + "'");
        DataSpaceAnalysisResult result = DataSpaceAnalyticsHelper.analyzeDataSpace(dataSpace, pureModel, (DataSpace) dataSpaceProtocol, data, clientVersion, this.returnLightGraph);

        // Partition
        List<MappingModelCoveragePartition> mappingModelPartitions = Lists.mutable.empty();
        List<MappingAnalysisCoveragePartition> mappingAnalysisPartitions = Lists.mutable.empty();
        partitionDataSpaceAnalyticsResult(result,mappingModelPartitions, mappingAnalysisPartitions);
        try
        {
            List<Artifact> artifacts = Lists.mutable.empty();
            String stringResult = objectMapper.writeValueAsString(result);
            artifacts.add(new Artifact(stringResult, MAIN_ANALYTICS_FILE, ANALYTICS_FORMAT));
            if (mappingModelPartitions != null && !mappingModelPartitions.isEmpty())
            {
                for (int i = 0; i < mappingModelPartitions.size(); i++)
                {
                    MappingModelCoveragePartition modelPartition = mappingModelPartitions.get(i);
                    String fileName = MAPPING_MODEL_PREFIX + i + "." + ANALYTICS_FORMAT;
                    artifacts.add(new Artifact(objectMapper.writeValueAsString(modelPartition), fileName, "json"));
                }
            }
            if (mappingAnalysisPartitions != null && !mappingAnalysisPartitions.isEmpty())
            {
                for (int i = 0; i < mappingAnalysisPartitions.size(); i++)
                {
                    MappingAnalysisCoveragePartition analysisPartition = mappingAnalysisPartitions.get(i);
                    String fileName = MAPPING_ANALYSIS_FILE_PREFIX + i + "." + ANALYTICS_FORMAT;
                    artifacts.add(new Artifact(objectMapper.writeValueAsString(analysisPartition), fileName, ANALYTICS_FORMAT));
                }
            }
            return artifacts;
        }
        catch (Exception exception)
        {
            throw new EngineException("Can't serialize data space analysis result", exception);
        }
    }


    private void partitionDataSpaceAnalyticsResult(DataSpaceAnalysisResult result, List<MappingModelCoveragePartition> mappingModelPartitions, List<MappingAnalysisCoveragePartition> mappingAnalysisPartitions)
    {
        Map<String, MappingModelCoverageAnalysisResult> mappingToMappingCoverageResult = result.mappingToMappingCoverageResult;
        if (mappingToMappingCoverageResult != null && !mappingToMappingCoverageResult.isEmpty())
        {
            Set<String> mappingsToRemove = new HashSet<>();
            mappingToMappingCoverageResult.forEach((mapping, mappingCoverageResult) ->
            {
                if (mappingCoverageResult.model != null)
                {
                    MappingModelCoveragePartition modelPartition = new MappingModelCoveragePartition();
                    modelPartition.mapping = mapping;
                    modelPartition.model = mappingCoverageResult.model;
                    mappingModelPartitions.add(modelPartition);
                    // remove model from coverage result
                    mappingCoverageResult.model = null;
                }
                MappingAnalysisCoveragePartition mappingCoverageModelPartition = new MappingAnalysisCoveragePartition();
                mappingCoverageModelPartition.mapping = mapping;
                mappingCoverageModelPartition.analysisResult = mappingCoverageResult;
                mappingAnalysisPartitions.add(mappingCoverageModelPartition);
                mappingsToRemove.add(mapping);
            });
            mappingsToRemove.forEach(mappingToMappingCoverageResult::remove);
        }
    }




}
