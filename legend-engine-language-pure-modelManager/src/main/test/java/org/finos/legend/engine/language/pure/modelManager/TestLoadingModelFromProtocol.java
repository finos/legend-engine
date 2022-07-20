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
package org.finos.legend.engine.language.pure.modelManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;
import java.util.Scanner;

public class TestLoadingModelFromProtocol
{
    private static final ModelManager modelManager = new ModelManager(DeploymentMode.TEST_IGNORE_FUNCTION_MATCH);
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testFunctionLoadingWithPackageOffset()
    {
        String jsonString = new Scanner(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("functionExample.json")), "UTF-8").useDelimiter("\\A").next();
        try
        {
            PureModelContextData pureModelContextData = objectMapper.readValue(jsonString, PureModelContextData.class);
            PureModel pureModel = this.modelManager.loadModel(pureModelContextData, "vX_X_X", null, "update::");
            Assert.assertNotNull(pureModel);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
