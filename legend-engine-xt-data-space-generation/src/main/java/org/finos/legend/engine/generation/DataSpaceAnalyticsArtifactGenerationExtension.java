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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.analytics.DataSpaceAnalyticsHelper;
import org.finos.legend.engine.analytics.model.DataSpaceAnalysisResult;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;
import static org.finos.legend.pure.generated.platform_pure_corefunctions_meta.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1__String_1_;

public class DataSpaceAnalyticsArtifactGenerationExtension implements ArtifactGenerationExtension
{

    public final String ROOT_PATH = "data-space-analytics";
    public static ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Override
    public boolean canGenerate(PackageableElement element)
    {
        return element instanceof Root_meta_pure_metamodel_dataSpace_DataSpace;
    }

    @Override
    public String getArtifactsRootPath()
    {
        return ROOT_PATH;
    }

    @Override
    public List<Artifact> generate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {
        String dataSpacePath = getElementFullPath(element, pureModel.getExecutionSupport());
        Assert.assertTrue(this.canGenerate(element), () -> "DataSpace analytics only supports dataSpace elements");
        Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace = (Root_meta_pure_metamodel_dataSpace_DataSpace) element;
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement dataSpaceProtocol = data.getElements().stream().filter(el -> dataSpacePath.equals(el.getPath())).findFirst().orElse(null);
        Assert.assertTrue(dataSpaceProtocol instanceof DataSpace, () -> "Can't find data space '" + dataSpacePath + "'");
        DataSpaceAnalysisResult result = DataSpaceAnalyticsHelper.analyzeDataSpace(dataSpace, pureModel, (DataSpace) dataSpaceProtocol, data, clientVersion);
        try
        {
            String stringResult = objectMapper.writeValueAsString(result);
            String filePath = Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1__String_1_(element, "/", pureModel.getExecutionSupport()) + ".json";
            Artifact output = new Artifact(stringResult, filePath, "json");
            return Collections.singletonList(output);

        }
        catch (Exception ignored)
        {
            // ignore
        }
        return null;
    }
}
