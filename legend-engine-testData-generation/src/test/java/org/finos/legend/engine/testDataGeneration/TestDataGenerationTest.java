// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.testDataGeneration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.engine.testData.generation.service.RelationalCSVTableGenerationService;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.List;

public class TestDataGenerationTest
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

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

    private void testRelationalCSVTableGeneration(String modelFilePath, String mappingPath, String expectedResult) throws Exception
    {
        String pureModelString = getResourceAsString(modelFilePath);
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString, false);
        PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.TEST, null);
        Mapping mapping = pureModel.getMapping(mappingPath);
        RelationalCSVTableGenerationService service = new RelationalCSVTableGenerationService();
        List<RelationalCSVTable> tables = service.generateRelationalCSVTable(mapping, pureModel);
        Assert.assertEquals(objectMapper.writeValueAsString(tables), expectedResult);
    }

    @Test
    public void testStoreEntitlementAnalyticsForH2() throws Exception
    {
        testRelationalCSVTableGeneration("models/relationalModel.pure",
                "mapping::CovidDataMapping",
                "[{\"schema\":\"default\",\"table\":\"DEMOGRAPHICS\",\"values\":\"\"},{\"schema\":\"default\",\"table\":\"COVID_DATA\",\"values\":\"\"}]");
    }
}
