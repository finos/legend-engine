// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.jsonSchema;

import org.finos.legend.engine.external.format.jsonSchema.extension.JSONSchemaGenerationConfigBuilder;
import org.finos.legend.engine.external.format.jsonSchema.schema.generations.JSONSchemaConfig;
import org.finos.legend.engine.external.format.jsonSchema.schema.generations.JSONSchemaGenerationService;
import org.finos.legend.engine.external.shared.format.generations.GenerationOutput;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

public class TestJSONSchemaFileGeneration
{
    @Test
    public void testSimpleJSONSchema()
    {
        try
        {
            PureModelContextData pureModelContextData = getProtocol("simpleFileGeneration.json");
            PureModel pureModel = new PureModel(pureModelContextData, null, DeploymentMode.TEST);
            FileGenerationSpecification fileGeneration = pureModelContextData.getElementsOfType(FileGenerationSpecification.class).get(0);
            JSONSchemaConfig jsonSchemaConfig = JSONSchemaGenerationConfigBuilder.build(fileGeneration);
            Assert.assertTrue(jsonSchemaConfig.useConstraints);
            List<GenerationOutput> outputs = JSONSchemaGenerationService.generate(jsonSchemaConfig, pureModel);
            Assert.assertEquals(outputs.size(), 4);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private PureModelContextData getProtocol(String fileName) throws JsonProcessingException
    {
        String jsonString = this.getResourceAsString(fileName);
        return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(jsonString, PureModelContextData.class);
    }

    private String getResourceAsString(String fileName)
    {
        InputStream inputStream = TestJSONSchemaFileGeneration.class.getResourceAsStream(fileName);
        Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}