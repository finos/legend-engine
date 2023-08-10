// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestLoadFileGeneration
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void testVX_X_X() throws Exception
    {
        simpleSlangTest("vX_X_X");
    }

    private void simpleSlangTest(String version) throws Exception
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("scopedFieldFileGeneration_" + version + ".json")), PureModelContextData.class);
        Assert.assertEquals(version, context.serializer.version);
    }

    @Test
    public void testFileGenerationWithUnsupportedValueForPropertyConfiguration()
    {
        // array with non-string values
        try
        {
            objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("fileGenerationWithUnsupportedArrayValueForPropertyConfiguration.json")), PureModelContextData.class);
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("Configuration property value array only supports string values"));
        }
        // map with non-string values
        try
        {
            objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("fileGenerationWithUnsupportedMapValueForPropertyConfiguration.json")), PureModelContextData.class);
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("Configuration property value map only supports string values"));
        }
    }
}
