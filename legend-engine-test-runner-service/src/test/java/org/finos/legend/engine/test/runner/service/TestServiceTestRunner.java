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

package org.finos.legend.engine.test.runner.service;

import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.test.runner.shared.TestResult;
import org.finos.legend.pure.generated.core_relational_relational_router_router_extension;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TestServiceTestRunner
{
    private void test(String serviceModelPath, String servicePath, TestResult expectedResult, boolean multiExecution) throws Exception
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(serviceModelPath));
        PureModelContextData pureModelContextData = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(url, PureModelContextData.class);
        PureModel pureModel = new PureModel(pureModelContextData, null, Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);

        Service service = pureModelContextData.getElementsOfType(Service.class).stream().filter(s -> s.getPath().equals(servicePath)).findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find service with path '" + servicePath + "'"));

        List<RichServiceTestResult> testResults = this.runTest(service, pureModel, pureModelContextData);
        Assert.assertNotNull(testResults);
        Assert.assertEquals(1, testResults.size());
        RichServiceTestResult testResult = testResults.get(0);
        Assert.assertEquals(servicePath, testResult.getServicePath());
        if(multiExecution){
            Assert.assertNotNull(testResult.getOptionalMultiExecutionKey());
        } else {
            Assert.assertNull(testResult.getOptionalMultiExecutionKey());
        }
        Assert.assertEquals(Collections.emptyMap(), testResult.getAssertExceptions());
        Assert.assertEquals(Collections.singletonMap("test0", expectedResult), testResult.getResults());
    }

    private List<RichServiceTestResult> runTest(Service service, PureModel pureModel, PureModelContextData pureModelContextData) {
        ServiceTestRunner serviceTestRunner = new ServiceTestRunner(service, Tuples.pair(pureModelContextData, pureModel), PlanExecutor.newPlanExecutorWithAvailableStoreExecutors(), core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers, "vX_X_X");
        try {
            return serviceTestRunner.executeTests();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void testSucceedingService() throws Exception
    {
        test("legend-sdlc-test-services-with-tests.json", "test::legend::service::execution::test::m2m::simpleJsonService", TestResult.SUCCESS, false);
    }

    @Test
    public void testFailingService() throws Exception
    {
        test("legend-sdlc-test-services-with-tests.json", "test::legend::service::execution::test::m2m::simpleFailingJsonService", TestResult.FAILURE, false);
    }

    @Test
    public void testMultiExecutionService() throws Exception
    {
        test("legend-sdlc-test-services-multi-execution.json", "my::Service", TestResult.SUCCESS, true);
    }

}
