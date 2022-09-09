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

import java.net.URL;
import java.util.List;
import java.util.Map;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.junit.Assert;
import org.junit.Test;


public class TestDataSpaceAnalyticsArtifactGenerationExtension
{

    private String getResourceAsString(String path)
    {
        try
        {
            URL infoURL = DeploymentStateAndVersions.class.getClassLoader().getResource(path);
            if (infoURL != null)
            {
                java.util.Scanner scanner = new java.util.Scanner(infoURL.openStream()).useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : null;
            }
            return null;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDataSpaceAnalyticsArtifactGenerationExtension()
    {
        String pureModelString = getResourceAsString("models/DataspaceModel.pure");
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString);
        PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.TEST, null);
        Map<DataSpace, List<Artifact>> results = Maps.mutable.empty();
        DataSpaceAnalyticsArtifactGenerationExtension extension = new DataSpaceAnalyticsArtifactGenerationExtension();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = pureModel.getPackageableElement("dataSpace::_FirmDataSpace");
        Assert.assertTrue(packageableElement instanceof Root_meta_pure_metamodel_dataSpace_DataSpace);
        Root_meta_pure_metamodel_dataSpace_DataSpace metamodelDatasapce = (Root_meta_pure_metamodel_dataSpace_DataSpace) packageableElement;
        Assert.assertTrue(extension.canGenerate(metamodelDatasapce));
        List<Artifact> outputs = extension.generate(packageableElement, pureModel, pureModelContextData, "vX_X_X");
        Assert.assertEquals(1, outputs.size());
        Artifact dataSpaceAnalyticsResult = outputs.get(0);
        Assert.assertEquals(dataSpaceAnalyticsResult.format, "json");
        Assert.assertEquals(dataSpaceAnalyticsResult.path, "AnalyticsResult.json");
        Assert.assertNotNull(dataSpaceAnalyticsResult.content);
        Assert.assertNotEquals("", dataSpaceAnalyticsResult.content);
    }

}
