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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionTest;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.test.runner.shared.TestResult;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.finos.legend.pure.generated.core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension.Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestServiceTestRunner
{
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private void test(String serviceModelPath, String servicePath, TestResult expectedResult, boolean multiExecution) throws Exception
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(serviceModelPath));
        PureModelContextData pureModelContextData = OBJECT_MAPPER.readValue(url, PureModelContextData.class);
        PureModel pureModel = new PureModel(pureModelContextData, null, Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);

        Service service = pureModelContextData.getElementsOfType(Service.class).stream().filter(s -> s.getPath().equals(servicePath)).findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find service with path '" + servicePath + "'"));

        List<RichServiceTestResult> testResults = this.runTest(service, pureModel, pureModelContextData);
        assertNotNull(testResults);
        assertEquals(1, testResults.size());
        RichServiceTestResult testResult = testResults.get(0);
        assertEquals(servicePath, testResult.getServicePath());
        if (multiExecution)
        {
            assertNotNull(testResult.getOptionalMultiExecutionKey());
        }
        else
        {
            assertNull(testResult.getOptionalMultiExecutionKey());
        }
        assertEquals(Collections.emptyMap(), testResult.getAssertExceptions());
        assertEquals(Collections.singletonMap("test0", expectedResult), testResult.getResults());
    }

    private List<RichServiceTestResult> runTest(Service service, PureModel pureModel, PureModelContextData pureModelContextData)
    {
        ServiceTestRunner serviceTestRunner = new ServiceTestRunner(service, (Root_meta_legend_service_metamodel_Service) pureModel.getPackageableElement(service.getPath()), pureModelContextData, pureModel, OBJECT_MAPPER, PlanExecutor.newPlanExecutorWithAvailableStoreExecutors(), Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers, "vX_X_X");
        try
        {
            return serviceTestRunner.executeTests();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSucceedingService() throws Exception
    {
        test("legend-sdlc-test-services-with-tests.json", "test::legend::service::execution::test::m2m::simpleJsonService", TestResult.SUCCESS, false);
    }

    @Test
    public void testSucceedingServiceWithMultiParam() throws Exception
    {
        test("legend-sdlc-test-services-with-multi-param.json", "test::legend::service::execution::test::m2m::simpleJsonServiceMultiParam", TestResult.SUCCESS, false);
    }

    @Test
    public void testFailingService() throws Exception
    {
        test("legend-sdlc-test-services-with-tests.json", "test::legend::service::execution::test::m2m::simpleFailingJsonService", TestResult.FAILURE, false);
    }

    @Test
    public void testMultiExecutionService() throws Exception
    {
        Pair<Service, List<RichServiceTestResult>> serviceAndTestResults = runServiceTestsFromPureCode(
                "legend-sdlc-test-services-multi-execution.pure", "my::Service"
        );

        List<RichServiceTestResult> testResults = serviceAndTestResults.getTwo();

        assertNotNull(testResults);
        assertEquals(1, testResults.size());
        RichServiceTestResult testResult = testResults.get(0);
        assertEquals("my::Service", testResult.getServicePath());
        assertNotNull(testResult.getOptionalMultiExecutionKey());
        assertEquals(Collections.emptyMap(), testResult.getAssertExceptions());
        assertEquals(Collections.singletonMap("test0", TestResult.SUCCESS), testResult.getResults());
    }

    @Test
    public void testNoTestServiceFlow() throws Exception
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("legend-sdlc-test-services-without-tests.json"));
        PureModelContextData pureModelContextData = OBJECT_MAPPER.readValue(url, PureModelContextData.class);
        PureModel pureModel = new PureModel(pureModelContextData, null, Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);

        Service service = pureModelContextData.getElementsOfType(Service.class).get(0);

        List<RichServiceTestResult> testResults = this.runTest(service, pureModel, pureModelContextData);
        assertNotNull(testResults);
        assertEquals(1, testResults.size());
        RichServiceTestResult testResult = testResults.get(0);
        assertEquals("test::legend::service::execution::test::m2m::simpleServiceNoTest", testResult.getServicePath());
        assertNull(testResult.getOptionalMultiExecutionKey());
        assertNull(testResult.getExecutionPlan());
        assertNull(testResult.getJavaCodeString());
        assertEquals(Collections.emptyMap(), testResult.getAssertExceptions());
        assertEquals(Collections.emptyMap(), testResult.getResults());
    }

    @Test
    public void testMultiExecutionNoTestServiceFlow() throws Exception
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("legend-sdlc-test-services-multi-execution-without-tests.json"));
        PureModelContextData pureModelContextData = OBJECT_MAPPER.readValue(url, PureModelContextData.class);
        PureModel pureModel = new PureModel(pureModelContextData, null, Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);

        Service service = pureModelContextData.getElementsOfType(Service.class).get(0);

        List<RichServiceTestResult> testResults = this.runTest(service, pureModel, pureModelContextData);
        assertNotNull(testResults);
        assertEquals(1, testResults.size());
        RichServiceTestResult testResult = testResults.get(0);
        assertEquals("my::Service", testResult.getServicePath());
        assertEquals("Env1", testResult.getOptionalMultiExecutionKey());
        assertNull(testResult.getExecutionPlan());
        assertNull(testResult.getJavaCodeString());
        assertEquals(Collections.emptyMap(), testResult.getAssertExceptions());
        assertEquals(Collections.emptyMap(), testResult.getResults());
    }

    // Ensure that we do not execute a test if the assertions are always true (eg: | true)
    @Test
    public void tautologyAssertsServiceFlow() throws IOException
    {
        Pair<Service, List<RichServiceTestResult>> serviceAndTestResults = runServiceTestsFromPureCode(
                "tautologyServiceAsserts.pure", "org::finos::legend::TestService"
        );

        Service service = serviceAndTestResults.getOne();
        List<RichServiceTestResult> serviceTestResults = serviceAndTestResults.getTwo();

        assertEquals(1, ((SingleExecutionTest) service.test).asserts.size());
        assertEquals(1, serviceTestResults.size());
        assertEquals(0, serviceTestResults.get(0).getResults().size());  // indicator that test was not run
    }

    private static Pair<Service, List<RichServiceTestResult>> runServiceTestsFromPureCode(String pureFilepath, String servicePath) throws IOException
    {
        PureModelContextData pmcd = PureModelContextData.newBuilder()
                .withPureModelContextData(PureGrammarParser.newInstance().parseModel(
                        IOUtils.toString(
                                Objects.requireNonNull(
                                        TestServiceTestRunner.class.getClassLoader().getResource(pureFilepath)),
                                StandardCharsets.UTF_8
                        )
                ))
                .withSerializer(new Protocol("pure", "vX_X_X"))
                .withOrigin(new PureModelContextPointer())
                .build();

        PureModel pureModel = new PureModel(pmcd, null, Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);
        Service service = pmcd.getElementsOfType(Service.class).stream()
                .filter(s -> s.getPath().equals(servicePath))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find service with path '" + servicePath + "'"));

        ServiceTestRunner serviceTestRunner = new ServiceTestRunner(
                service,
                (Root_meta_legend_service_metamodel_Service) pureModel.getPackageableElement(service.getPath()),
                pmcd,
                pureModel,
                OBJECT_MAPPER,
                PlanExecutor.newPlanExecutor(true),
                Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()),
                LegendPlanTransformers.transformers,
                "vX_X_X"
        );
        try
        {
            return Tuples.pair(service, serviceTestRunner.executeTests());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
