//  Copyright 2026 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.testable.service;

import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertPass;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.testable.service.extension.ServiceTestableRunnerExtension;
import org.finos.legend.engine.testable.service.result.MultiExecutionServiceTestResult;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.List;
import java.util.Map;

public class TestServiceTestSuiteNewGrammar
{
    private static final String BASE_PATH = "testable/service-new-grammar/";

    @Test
    public void testBaseDataResolver()
    {
        List<TestResult> results = runServiceTests("service-baseResolver.pure", "service::svctd::BaseResolverService");
        assertSinglePass(results, "service::svctd::BaseResolverService");
    }

    @Test
    public void testReferenceDataResolver()
    {
        List<TestResult> results = runServiceTests("service-referenceResolver.pure", "service::svctd::ReferenceResolverService");
        assertSinglePass(results, "service::svctd::ReferenceResolverService");
    }

    @Test
    public void testBaseResolverOverridesReferenceResolver()
    {
        List<TestResult> results = runServiceTests("service-overrideResolver.pure", "service::svctd::OverrideResolverService");
        assertSinglePass(results, "service::svctd::OverrideResolverService");
    }

    @Test
    public void testFailingAssertion()
    {
        List<TestResult> results = runServiceTests("service-failing.pure", "service::svctd::FailingService");
        Assert.assertEquals(1, results.size());
        TestResult testResult = results.get(0);
        Assert.assertFalse(testResult instanceof TestError);
        TestExecuted executed = (TestExecuted) testResult;
        Assert.assertEquals("service::svctd::FailingService", executed.testable);
        Assert.assertEquals("testSuite_1", executed.testSuiteId);
        Assert.assertEquals("test_1", executed.atomicTestId);
        Assert.assertEquals(TestExecutionStatus.FAIL, executed.testExecutionStatus);
        Assert.assertFalse(executed.assertStatuses.isEmpty());
        boolean hasFail = false;
        for (AssertionStatus status : executed.assertStatuses)
        {
            if (status instanceof AssertFail)
            {
                hasFail = true;
            }
        }
        Assert.assertTrue(hasFail);
    }

    @Test
    public void testMultiExecutionWithKeysRunsOnlyProvidedKey()
    {
        List<TestResult> results = runServiceTests("service-multiExec.pure", "service::svctd::MultiExecService");
        TestResult onlyKeyAResult = results.stream()
                .filter(r -> "test_onlyKeyA".equals(r.atomicTestId))
                .findFirst()
                .orElseThrow(AssertionError::new);
        Assert.assertFalse(onlyKeyAResult instanceof TestError);
        Assert.assertTrue(onlyKeyAResult instanceof MultiExecutionServiceTestResult);
        Map<String, TestResult> perKey = ((MultiExecutionServiceTestResult) onlyKeyAResult).getKeyIndexedTestResults();
        Assert.assertEquals(1, perKey.size());
        Assert.assertTrue(perKey.containsKey("KEY_A"));
        TestResult keyAResult = perKey.get("KEY_A");
        Assert.assertTrue(keyAResult instanceof TestExecuted);
        Assert.assertEquals(TestExecutionStatus.PASS, ((TestExecuted) keyAResult).testExecutionStatus);
        for (AssertionStatus status : ((TestExecuted) keyAResult).assertStatuses)
        {
            Assert.assertTrue(status instanceof AssertPass);
        }
    }

    @Test
    public void testMultiExecutionWithoutKeysRunsAllContexts()
    {
        List<TestResult> results = runServiceTests("service-multiExec.pure", "service::svctd::MultiExecService");
        TestResult allKeysResult = results.stream()
                .filter(r -> "test_allKeys".equals(r.atomicTestId))
                .findFirst()
                .orElseThrow(AssertionError::new);
        Assert.assertFalse(allKeysResult instanceof TestError);
        Assert.assertTrue(allKeysResult instanceof MultiExecutionServiceTestResult);
        Map<String, TestResult> perKey = ((MultiExecutionServiceTestResult) allKeysResult).getKeyIndexedTestResults();
        Assert.assertEquals(2, perKey.size());
        Assert.assertTrue(perKey.containsKey("KEY_A"));
        Assert.assertTrue(perKey.containsKey("KEY_B"));

        TestResult keyAResult = perKey.get("KEY_A");
        Assert.assertTrue(keyAResult instanceof TestExecuted);
        Assert.assertEquals(TestExecutionStatus.PASS, ((TestExecuted) keyAResult).testExecutionStatus);

        TestResult keyBResult = perKey.get("KEY_B");
        boolean keyBFailedOrErrored = keyBResult instanceof TestError
                || (keyBResult instanceof TestExecuted && ((TestExecuted) keyBResult).testExecutionStatus == TestExecutionStatus.FAIL);
        Assert.assertTrue(keyBFailedOrErrored);
    }

    private List<TestResult> runServiceTests(String serviceFile, String fullPath)
    {
        String pureModelString = getResourceAsString(BASE_PATH + serviceFile);
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString);
        PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        Root_meta_legend_service_metamodel_Service serviceWithTest = (Root_meta_legend_service_metamodel_Service) pureModel.getPackageableElement(fullPath);
        return new ServiceTestableRunnerExtension().executeAllTest(serviceWithTest, pureModel, pureModelContextData);
    }

    private void assertSinglePass(List<TestResult> results, String servicePath)
    {
        Assert.assertEquals(1, results.size());
        TestResult testResult = results.get(0);
        Assert.assertFalse(testResult instanceof TestError);
        TestExecuted executed = (TestExecuted) testResult;
        Assert.assertEquals(servicePath, executed.testable);
        Assert.assertEquals(TestExecutionStatus.PASS, executed.testExecutionStatus);
        for (AssertionStatus status : executed.assertStatuses)
        {
            Assert.assertTrue(status instanceof AssertPass);
        }
    }

    private String getResourceAsString(String path)
    {
        try
        {
            URL infoURL = TestServiceTestSuiteNewGrammar.class.getClassLoader().getResource(path);
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
}
