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

package org.finos.legend.engine.external.format.flatdata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.external.format.flatdata.fromModel.ModelToFlatDataConfiguration;
import org.finos.legend.engine.external.format.flatdata.toModel.FlatDataToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.api.GenerateModelInput;
import org.finos.legend.engine.external.shared.format.model.api.GenerateSchemaInput;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class TestGenerationConfigDeserialize
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void canDeserializeToModelConfig() throws JsonProcessingException
    {
       String json = "{\n" +
               "  \"clientVersion\": \"vX_X_X\",\n" +
               "  \"model\": {},\n" +
               "  \"config\": {\n" +
               "    \"format\": \"FlatData\",\n" +
               "    \"sourceSchemaSet\": \"test::SchemaSet\",\n" +
               "    \"sourceSchemaId\": \"anId\",\n" +
               "    \"targetBinding\": \"test::target::GeneratedBinding\",\n" +
               "    \"targetPackage\": \"test::target\",\n" +
               "    \"purifyNames\": true,\n" +
               "    \"schemaClassName\":  \"test::target::SchemaClass\"\n" +
               "  }\n" +
               "}";

        GenerateModelInput input = objectMapper.readValue(json, GenerateModelInput.class);
        Assert.assertTrue(input.config instanceof FlatDataToModelConfiguration);
        FlatDataToModelConfiguration config = (FlatDataToModelConfiguration) input.config;
        Assert.assertEquals("FlatData", config.format);
        Assert.assertEquals("test::SchemaSet", config.sourceSchemaSet);
        Assert.assertEquals("anId", config.sourceSchemaId);
        Assert.assertEquals("test::target::GeneratedBinding", config.targetBinding);
        Assert.assertEquals("test::target", config.targetPackage);
        Assert.assertEquals(true, config.purifyNames);
        Assert.assertEquals("test::target::SchemaClass", config.schemaClassName);
    }

    @Test
    public void canDeserializeToSchemaConfig() throws JsonProcessingException
    {
        String json = "{\n" +
                "  \"clientVersion\": \"vX_X_X\",\n" +
                "  \"model\": {},\n" +
                "  \"config\": {\n" +
                "    \"format\": \"FlatData\",\n" +
                "    \"targetBinding\": \"test::target::GeneratedBinding\",\n" +
                "    \"targetSchemaSet\": \"test::target::GeneratedSchemaSet\",\n" +
                "    \"sourceModel\": [\"test::A\", \"test::B\"]\n" +
                "  }\n" +
                "}";

        GenerateSchemaInput input = objectMapper.readValue(json, GenerateSchemaInput.class);
        Assert.assertTrue(input.config instanceof ModelToFlatDataConfiguration);
        ModelToFlatDataConfiguration config = (ModelToFlatDataConfiguration) input.config;
        Assert.assertEquals("FlatData", config.format);
        Assert.assertEquals(Arrays.asList("test::A", "test::B"), config.sourceModel);
        Assert.assertEquals("test::target::GeneratedBinding", config.targetBinding);
        Assert.assertEquals("test::target::GeneratedSchemaSet", config.targetSchemaSet);
    }
}
