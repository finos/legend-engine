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
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.generation.analytics.model.DataSpaceAnalysisResult;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.List;


public class TestDataSpaceAnalyticsArtifactGenerationExtension
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final String minimumPureClientVersion = "v1_20_0";
    private static final ImmutableList<String> testVersions = PureClientVersions.versionsSince(minimumPureClientVersion);

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
    public void testDataSpaceAnalyticsArtifactGenerationExtension() throws Exception
    {
        String pureModelString = getResourceAsString("models/DataspaceModel.pure");
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString, false);
        PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.TEST, null);
        DataSpaceAnalyticsArtifactGenerationExtension extension = new DataSpaceAnalyticsArtifactGenerationExtension();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = pureModel.getPackageableElement("dataSpace::_FirmDataSpace");
        Assert.assertTrue(packageableElement instanceof Root_meta_pure_metamodel_dataSpace_DataSpace);
        Root_meta_pure_metamodel_dataSpace_DataSpace metamodelDataSpace = (Root_meta_pure_metamodel_dataSpace_DataSpace) packageableElement;
        Assert.assertTrue(extension.canGenerate(metamodelDataSpace));
        for (String pureClient : testVersions)
        {
            List<Artifact> outputs = extension.generate(packageableElement, pureModel, pureModelContextData, pureClient);
            Assert.assertEquals(1, outputs.size());
            Artifact dataSpaceAnalyticsResult = outputs.get(0);
            Assert.assertEquals(dataSpaceAnalyticsResult.format, "json");
            Assert.assertEquals(dataSpaceAnalyticsResult.path, "AnalyticsResult.json");
            Assert.assertEquals("{\"defaultExecutionContext\":\"dummyContext\",\"elementDocs\":[{\"_type\":\"association\",\"docs\":[],\"name\":\"Firm_Person\",\"path\":\"model::Firm_Person\",\"properties\":[{\"docs\":[],\"name\":\"firm\"},{\"docs\":[\"some doc for employees\"],\"name\":\"employees\"}]},{\"_type\":\"enumeration\",\"docs\":[\"Types of company\"],\"enumValues\":[{\"docs\":[\"Limited\"],\"name\":\"LLC\"},{\"docs\":[],\"name\":\"CORP\"}],\"name\":\"IncType\",\"path\":\"model::target::IncType\"},{\"_type\":\"class\",\"docs\":[],\"inheritedProperties\":[],\"name\":\"_Firm\",\"path\":\"model::target::_Firm\",\"properties\":[{\"docs\":[],\"name\":\"employees\"},{\"docs\":[],\"name\":\"type\"},{\"docs\":[],\"name\":\"name\"}]},{\"_type\":\"class\",\"docs\":[],\"inheritedProperties\":[],\"name\":\"LegalEntity\",\"path\":\"model::LegalEntity\",\"properties\":[{\"docs\":[\"name of the entity\"],\"name\":\"legalName\"}]},{\"_type\":\"class\",\"docs\":[\"The Firm concept\"],\"inheritedProperties\":[{\"docs\":[\"name of the entity\"],\"name\":\"legalName\"}],\"name\":\"Firm\",\"path\":\"model::Firm\",\"properties\":[{\"docs\":[\"type of firm: e.g. CORP, LTD\"],\"name\":\"type\"},{\"docs\":[\"some doc for employees\"],\"name\":\"employees\"}]},{\"_type\":\"class\",\"docs\":[\"Animal class\"],\"inheritedProperties\":[],\"name\":\"Animal\",\"path\":\"model::Animal\",\"properties\":[{\"docs\":[\"age\"],\"name\":\"age\"}]},{\"_type\":\"class\",\"docs\":[\"Homo Sapien\"],\"inheritedProperties\":[{\"docs\":[\"age\"],\"name\":\"age\"}],\"name\":\"Person\",\"path\":\"model::Person\",\"properties\":[{\"docs\":[],\"name\":\"firstName\"},{\"docs\":[],\"name\":\"lastName\"},{\"docs\":[],\"name\":\"firm\"}]}],\"elements\":[\"model::Firm_Person\",\"model::target::IncType\",\"model::target::_Firm\",\"model::LegalEntity\",\"model::Firm\",\"model::Animal\",\"model::Person\"],\"executionContexts\":[{\"compatibleRuntimes\":[],\"defaultRuntime\":\"mapping::ModelToModelRuntime\",\"mapping\":\"mapping::ModelToModelMapping\",\"mappingModelCoverageAnalysisResult\":{\"mappedEntities\":[{\"path\":\"model::target::_Firm\",\"properties\":[{\"_type\":\"entity\",\"entityPath\":\"model::target::_Person\",\"name\":\"employees\"},{\"_type\":\"MappedProperty\",\"name\":\"name\"},{\"_type\":\"enum\",\"enumPath\":\"model::target::IncType\",\"name\":\"type\"}]},{\"path\":\"model::target::_Person\",\"properties\":[{\"_type\":\"MappedProperty\",\"name\":\"fullName\"}]}]},\"name\":\"dummyContext\"}],\"featuredDiagrams\":[\"diagram::MyDiagram\"],\"model\":{\"_type\":\"data\",\"elements\":[{\"_type\":\"diagram\",\"classViews\":[{\"class\":\"model::target::_Firm\",\"id\":\"a6b8266b-395e-4308-a08a-8f9b44af6d53\",\"position\":{\"x\":547.0,\"y\":270.0},\"rectangle\":{\"height\":72.0,\"width\":159.0}},{\"class\":\"model::target::_Person\",\"id\":\"c41deda8-19be-4374-9260-8be50c32d3cd\",\"position\":{\"x\":1128.0,\"y\":308.0},\"rectangle\":{\"height\":44.0,\"width\":122.0}}],\"generalizationViews\":[],\"name\":\"MyDiagram\",\"package\":\"diagram\",\"propertyViews\":[{\"line\":{\"points\":[{\"x\":626.5,\"y\":306.0},{\"x\":1189.0,\"y\":330.0}]},\"property\":{\"class\":\"model::target::_Firm\",\"property\":\"employees\"},\"sourceView\":\"a6b8266b-395e-4308-a08a-8f9b44af6d53\",\"targetView\":\"c41deda8-19be-4374-9260-8be50c32d3cd\"}]},{\"_type\":\"Enumeration\",\"name\":\"IncType\",\"package\":\"model::target\",\"stereotypes\":[],\"taggedValues\":[],\"values\":[{\"stereotypes\":[],\"taggedValues\":[],\"value\":\"LLC\"},{\"stereotypes\":[],\"taggedValues\":[],\"value\":\"CORP\"}]},{\"_type\":\"class\",\"constraints\":[],\"name\":\"_Firm\",\"originalMilestonedProperties\":[],\"package\":\"model::target\",\"properties\":[{\"multiplicity\":{\"lowerBound\":1},\"name\":\"employees\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"model::target::_Person\"},{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"type\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"model::target::IncType\"},{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"name\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"String\"}],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[],\"taggedValues\":[]},{\"_type\":\"class\",\"constraints\":[],\"name\":\"_Person\",\"originalMilestonedProperties\":[],\"package\":\"model::target\",\"properties\":[{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"fullName\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"String\"}],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[],\"taggedValues\":[]}]},\"name\":\"_FirmDataSpace\",\"package\":\"dataSpace\",\"path\":\"dataSpace::_FirmDataSpace\",\"stereotypes\":[],\"taggedValues\":[],\"title\":\"Firm Dataspace\"}", dataSpaceAnalyticsResult.content);
            objectMapper.readValue(dataSpaceAnalyticsResult.content, DataSpaceAnalysisResult.class);
        }
    }
}
