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

package org.finos.legend.engine.external.format.avro.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.external.format.avro.schema.generations.AvroGenerationInput;
import org.finos.legend.engine.external.format.avro.schema.generations.AvroGenerationService;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Objects;

public class TestAvroGeneration
{
    private final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testAvroConfig() throws Exception
    {
        String expected = "[{\"content\":\"{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"A\\\",\\\"namespace\\\":\\\"meta.avro.service.tests.model.latest\\\",\\\"fields\\\":[{\\\"name\\\":\\\"b\\\",\\\"type\\\":[\\\"null\\\",{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"B\\\",\\\"namespace\\\":\\\"meta.avro.service.tests.model.latest\\\",\\\"fields\\\":[{\\\"name\\\":\\\"i\\\",\\\"type\\\":\\\"long\\\"}]}],\\\"default\\\":null},{\\\"name\\\":\\\"e\\\",\\\"type\\\":{\\\"type\\\":\\\"array\\\",\\\"items\\\":{\\\"type\\\":\\\"enum\\\",\\\"name\\\":\\\"E\\\",\\\"namespace\\\":\\\"meta.avro.service.tests.model.latest\\\",\\\"symbols\\\":[\\\"E1\\\",\\\"E2\\\"]}}}]}\",\"fileName\":\"meta/avro/service/tests/model/1_0_0/A.avro\",\"format\":\"json\"}]";
        test("avroInputWithNameSpaceConfig.json", expected);
    }

    @Test
    public void testAvro() throws Exception
    {
        String expected = "[{\"content\":\"{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"A\\\",\\\"namespace\\\":\\\"meta.avro.service.tests.model.1_0_0\\\",\\\"fields\\\":[{\\\"name\\\":\\\"b\\\",\\\"type\\\":[\\\"null\\\",{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"B\\\",\\\"namespace\\\":\\\"meta.avro.service.tests.model.1_0_0\\\",\\\"fields\\\":[{\\\"name\\\":\\\"i\\\",\\\"type\\\":\\\"long\\\"}]}],\\\"default\\\":null},{\\\"name\\\":\\\"e\\\",\\\"type\\\":{\\\"type\\\":\\\"array\\\",\\\"items\\\":{\\\"type\\\":\\\"enum\\\",\\\"name\\\":\\\"E\\\",\\\"namespace\\\":\\\"meta.avro.service.tests.model.1_0_0\\\",\\\"symbols\\\":[\\\"E1\\\",\\\"E2\\\"]}}}]}\",\"fileName\":\"meta/avro/service/tests/model/1_0_0/A.avro\",\"format\":\"json\"}]";
        test("avroInputNoConfig.json", expected);
    }

    public void test(String directoryInput, String expected) throws Exception
    {
        InputStream stream;
        stream = Objects.requireNonNull(TestAvroGeneration.class.getResourceAsStream(directoryInput));
        String jsonInput = new java.util.Scanner(stream).useDelimiter("\\A").next();
        AvroGenerationInput request = objectMapper.readValue(jsonInput, AvroGenerationInput.class);
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        AvroGenerationService executor = new AvroGenerationService(modelManager);
        Response response = executor.generateAvro(request, null);
        String returnedContext = response.getEntity().toString();
        Assert.assertEquals(expected, returnedContext);
        stream.close();
    }
}
