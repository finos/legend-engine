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

package org.finos.legend.engine.test.runner.mapping.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Objects;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.test.runner.mapping.api.LegacyMappingTestRunnerResult.LegacyMappingTestResult;
import org.finos.legend.engine.test.runner.shared.TestResult;
import org.junit.Test;
import org.junit.Assert;

public class TestLegacyMappingRunner
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    private final LegacyMappingRunner api = new LegacyMappingRunner(new ModelManager(DeploymentMode.TEST));

    @Test
    public void testRunnerMapping() throws IOException
    {
        LegacyMappingTestRunnerInput context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("org/finos/legend/engine/test/runner/mapping/api/MappingTestRunnerInput.json")), LegacyMappingTestRunnerInput.class);
        LegacyMappingTestRunnerResult runnerResult =  api.runTests(context, null);
        Assert.assertEquals(runnerResult.results.size(), 1);
        LegacyMappingTestResult firstResult = runnerResult.results.get(0);
        Assert.assertEquals(firstResult.result, TestResult.SUCCESS);
    }

    @Test
    public void testRunnerMappingAllTests() throws IOException
    {
        LegacyMappingTestRunnerInput context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("org/finos/legend/engine/test/runner/mapping/api/MappingTestRunnerInput2.json")), LegacyMappingTestRunnerInput.class);
        LegacyMappingTestRunnerResult runnerResult =  api.runTests(context, null);
        Assert.assertEquals(runnerResult.results.size(), 3);
        LegacyMappingTestResult firstResult = runnerResult.results.get(0);
        Assert.assertEquals(firstResult.result, TestResult.SUCCESS);
        LegacyMappingTestResult secondResult = runnerResult.results.get(1);
        Assert.assertEquals(secondResult.result, TestResult.FAILURE);
        Assert.assertNotNull(secondResult.expected);
        Assert.assertNotNull(secondResult.actual);
        LegacyMappingTestResult thirdResult = runnerResult.results.get(2);
        Assert.assertEquals(thirdResult.result, TestResult.ERROR);
        Assert.assertNotNull(thirdResult.errorMessage);
    }


}
