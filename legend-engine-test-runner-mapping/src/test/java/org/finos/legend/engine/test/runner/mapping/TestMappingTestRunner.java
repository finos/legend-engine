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

package org.finos.legend.engine.test.runner.mapping;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest_Legacy;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestPassed;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.testable.mapping.extension.MappingTestableRunnerExtension;
import org.finos.legend.engine.test.runner.shared.TestResult;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import static org.finos.legend.pure.generated.core_relational_relational_extensions_extension.Root_meta_relational_extension_relationalExtensions__Extension_MANY_;
import static org.junit.Assert.assertEquals;

public class TestMappingTestRunner
{
    private static final ObjectMapper objectMapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    private PlanExecutor planExecutor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();

    //legacy tests
    @Test
    public void testSuccessfulTestExecution() throws IOException
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("modelToModelMappingTests1.json"));
        PureModelContextData pureModelContextData = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(url, PureModelContextData.class);
        PureModel pureModel = new PureModel(pureModelContextData, null, Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);

        RichMappingTestResult testResult = runTest(pureModelContextData, pureModel);

        assertEquals("model::domain::inmemm2m::mapping::m2mmapping", testResult.getMappingPath());
        assertEquals("simpleTest", testResult.getTestName());
        assertEquals(TestResult.SUCCESS, testResult.getResult());
        assertEquals(
                objectMapper.readValue("[{\"name\":\"CompanyA\",\"employees\":[{\"fullName\":\"John Smith\"},{\"fullName\":\"Mark Johnson\"}]}]", JsonNode.class),
                objectMapper.readValue(testResult.getExpected().get(), JsonNode.class));

        assertEquals(
                objectMapper.readValue("[{\"name\":\"CompanyA\",\"employees\":[{\"fullName\":\"John Smith\"},{\"fullName\":\"Mark Johnson\"}]}]", JsonNode.class),
                objectMapper.readValue(testResult.getActual().get(), JsonNode.class));
    }

    @Test
    public void testAssertionFailure() throws IOException
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("modelToModelMappingTests2.json"));
        PureModelContextData pureModelContextData = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(url, PureModelContextData.class);
        PureModel pureModel = new PureModel(pureModelContextData, null, Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);

        RichMappingTestResult testResult = runTest(pureModelContextData, pureModel);

        assertEquals("model::domain::inmemm2m::mapping::m2mmapping", testResult.getMappingPath());
        assertEquals("simpleTest", testResult.getTestName());
        assertEquals(TestResult.FAILURE, testResult.getResult());

        assertEquals(
                objectMapper.readValue("[{\"name\":\"CompanyA\",\"employees\":[{\"fullName\":\"John Smith\"},{\"fullName\":\"Mark Johnson_DoesNotExist\"}]}]", JsonNode.class),
                objectMapper.readValue(testResult.getExpected().get(), JsonNode.class));

        assertEquals(
                objectMapper.readValue("[{\"name\":\"CompanyA\",\"employees\":[{\"fullName\":\"John Smith\"},{\"fullName\":\"Mark Johnson\"}]}]", JsonNode.class),
                objectMapper.readValue(testResult.getActual().get(), JsonNode.class));
    }

    private RichMappingTestResult runTest(PureModelContextData pureModelContextData, PureModel pureModel)
    {
        Mapping mapping = pureModelContextData.getElementsOfType(Mapping.class).get(0);
        MappingTest_Legacy mappingTestLegacy = mapping.tests.get(0);
        MappingTestRunner mappingTestRunner = new MappingTestRunner(pureModel, mapping.getPath(), mappingTestLegacy, planExecutor, Root_meta_relational_extension_relationalExtensions__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers, "vX_X_X");
        return mappingTestRunner.setupAndRunTest();
    }

    @Test
    public void jsonDeserializationTest() throws IOException
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("modelToModelMappingTests1.json"));
        PureModelContextData pureModelContextData = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(url, PureModelContextData.class);
        new PureModel(pureModelContextData, null, Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);
    }
}
