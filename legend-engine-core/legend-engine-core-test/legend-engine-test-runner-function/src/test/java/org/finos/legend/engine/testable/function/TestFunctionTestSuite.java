//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.testable.function;

import net.javacrumbs.jsonunit.JsonAssert;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.function.extension.FunctionTestableRunnerExtension;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.test.TestAccessor;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.List;

public class TestFunctionTestSuite
{

    @Test
    public void testSimpleFunction()
    {
        List<TestResult> testResults = executeFunctionTest("legend-testable-function-test-model.pure", "model::Simple__String_1_");
        Assert.assertEquals(1, testResults.size());
        Assert.assertTrue(hasTestPassed(findTestById(testResults, "testPass")));
    }

    @Test
    public void testSimpleFunctionReference()
    {
        List<TestResult> testResults = executeFunctionTest("legend-testable-function-test-model.pure", "model::SimpleReference__String_1_");
        Assert.assertEquals(2, testResults.size());
        Assert.assertTrue(hasTestPassed(findTestById(testResults, "testPass")));
        TestResult failedResult = findTestById(testResults, "testFail");
        Assert.assertTrue(failedResult instanceof TestExecuted);
        TestExecuted executed = (TestExecuted) failedResult;
        Assert.assertEquals(TestExecutionStatus.FAIL, executed.testExecutionStatus);
    }

    @Test
    public void testSimpleFunctionWithParameters()
    {
        List<TestResult> testResults = executeFunctionTest("legend-testable-function-test-model.pure", "model::Hello_String_1__String_1_");
        Assert.assertEquals(2, testResults.size());
        Assert.assertTrue(hasTestPassed(findTestById(testResults, "testPass")));
        String message = "expected:Hello World! My name is Johnx., Found : Hello World! My name is John.";
        TestResult failedResult = findTestById(testResults, "testFail");
        Assert.assertTrue(failedResult instanceof TestExecuted);
        TestExecuted executed = (TestExecuted) failedResult;
        Assert.assertEquals(TestExecutionStatus.FAIL, executed.testExecutionStatus);
        Assert.assertEquals(1, executed.assertStatuses.size());
        AssertionStatus assertionStatus = executed.assertStatuses.get(0);
        Assert.assertTrue(assertionStatus instanceof AssertFail);
        AssertFail fail = (AssertFail) assertionStatus;
        Assert.assertEquals(message, fail.message);
    }

    @Test
    public void testRelationalFunctionTest()
    {
        List<TestResult> inlineServiceStoreTestResults = executeFunctionTest("legend-testable-function-test-model-relational.pure", "model::PersonQuery__TabularDataSet_1_");
        Assert.assertEquals(1, inlineServiceStoreTestResults.size());
        Assert.assertTrue(inlineServiceStoreTestResults.get(0) instanceof TestExecuted);
        TestExecuted testExecuted = (TestExecuted) inlineServiceStoreTestResults.get(0);
        Assert.assertEquals(TestExecutionStatus.PASS, testExecuted.testExecutionStatus);

    }

    @Test
    public void testRelationalWithSharedData()
    {
        List<TestResult> usingSharedData = executeFunctionTest("legend-testable-function-test-model-relational.pure", "model::PersonQuerySharedData__TabularDataSet_1_");
        Assert.assertEquals(1, usingSharedData.size());
        Assert.assertTrue(usingSharedData.get(0) instanceof TestExecuted);
        TestExecuted sharedTextExecuted = (TestExecuted) usingSharedData.get(0);
        Assert.assertEquals(TestExecutionStatus.FAIL, sharedTextExecuted.testExecutionStatus);
        String expected = "[ {\n" +
                "  \"First Name\" : \"I'm John\",\n" +
                "  \"Last Name\" : \"Doe, Jr\"\n" +
                "}, {\n" +
                "  \"First Name\" : \"Nicole\",\n" +
                "  \"Last Name\" : \"Smith\"\n" +
                "}, {\n" +
                "  \"First Name\" : \"Time\",\n" +
                "  \"Last Name\" : \"Smith\"\n" +
                "} ]";
        String actual = "[ {\n" +
                "  \"First Name\" : \"John\",\n" +
                "  \"Last Name\" : \"Doe\"\n" +
                "}, {\n" +
                "  \"First Name\" : \"Nicole\",\n" +
                "  \"Last Name\" : \"Smith\"\n" +
                "}, {\n" +
                "  \"First Name\" : \"Time\",\n" +
                "  \"Last Name\" : \"Smith\"\n" +
                "} ]";
        testFailingTest(findTestById(usingSharedData, "test_1"), expected, actual);
    }

    @Test
    public void testRelationalWithConnectionStores()
    {
        List<TestResult> usingSharedData = executeFunctionTest("legend-testable-function-test-model-relational.pure", "model::PersonWithConnectionStores__TabularDataSet_1_");
        Assert.assertEquals(1, usingSharedData.size());
        Assert.assertTrue(usingSharedData.get(0) instanceof TestExecuted);
        TestExecuted sharedTextExecuted = (TestExecuted) usingSharedData.get(0);
        Assert.assertEquals(TestExecutionStatus.FAIL, sharedTextExecuted.testExecutionStatus);
    }

    @Test
    public void testFunctionTestM2M()
    {
        List<TestResult> inlineServiceStoreTestResults = executeFunctionTest("legend-testable-function-test-model-m2m.pure", "model::PersonQuery__String_1_");
        Assert.assertEquals(1, inlineServiceStoreTestResults.size());
        Assert.assertTrue(inlineServiceStoreTestResults.get(0) instanceof TestExecuted);
        TestExecuted testExecuted = (TestExecuted) inlineServiceStoreTestResults.get(0);
        Assert.assertEquals(TestExecutionStatus.PASS, testExecuted.testExecutionStatus);
    }

    @Test
    public void testFunctionTestM2MReferenceData()
    {
        List<TestResult> inlineServiceStoreTestResults = executeFunctionTest("legend-testable-function-test-model-m2m.pure", "model::PersonQuerySharedData__String_1_");
        Assert.assertEquals(1, inlineServiceStoreTestResults.size());
        Assert.assertTrue(inlineServiceStoreTestResults.get(0) instanceof TestExecuted);
        TestExecuted testExecuted = (TestExecuted) inlineServiceStoreTestResults.get(0);
        Assert.assertEquals(TestExecutionStatus.PASS, testExecuted.testExecutionStatus);
    }

    @Test
    public void testFunctionTestWithParameters()
    {
        List<TestResult> testResults = executeFunctionTest("legend-testable-function-test-model-relational.pure", "model::PersonWithParams_String_1__TabularDataSet_1_");
        Assert.assertEquals(2, testResults.size());
        Assert.assertTrue(hasTestPassed(findTestById(testResults, "testPass")));
        String expected = "[]";
        String actual = "[ {\n" +
                "  \"First Name\" : \"Nicole\",\n" +
                "  \"Last Name\" : \"Smith\"\n" +
                "} ]";
        testFailingTest(findTestById(testResults, "testFail"), expected, actual);
    }

    @Test
    public void testRelationalWithModelJoin()
    {
        List<TestResult> inlineServiceStoreTestResults = executeFunctionTest("legend-testable-function-test-model-join-relational.pure", "com::trade::TestFunction__TabularDataSet_1_");
        Assert.assertEquals(1, inlineServiceStoreTestResults.size());
        Assert.assertTrue(inlineServiceStoreTestResults.get(0) instanceof TestExecuted);
        TestExecuted testExecuted = (TestExecuted) inlineServiceStoreTestResults.get(0);
        Assert.assertEquals(TestExecutionStatus.PASS, testExecuted.testExecutionStatus);
    }

    @Test
    public void testRelationalPass()
    {
        List<TestResult> inlineServiceStoreTestResults = executeFunctionTest("legend-testable-function-test-model-relational.pure", "model::PersonQuery__TabularDataSet_1_");
        Assert.assertEquals(1, inlineServiceStoreTestResults.size());
        Assert.assertTrue(inlineServiceStoreTestResults.get(0) instanceof TestExecuted);
        TestExecuted testExecuted = (TestExecuted) inlineServiceStoreTestResults.get(0);
        Assert.assertEquals(TestExecutionStatus.PASS, testExecuted.testExecutionStatus);
    }

    private List<TestResult> executeFunctionTest(String grammar, String fullPath)
    {
        FunctionTestableRunnerExtension functionTestableRunnerExtension = new FunctionTestableRunnerExtension();
        String pureModelString = getResourceAsString("testable/" + grammar);
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString);
        PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.TEST, Identity.getAnonymousIdentity());
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element = pureModel.getPackageableElement(fullPath);
        Assert.assertTrue(element instanceof  org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition);
        ConcreteFunctionDefinition<?> functionMetamodel = (ConcreteFunctionDefinition<?>) element;
        TestRunner testRunner = functionTestableRunnerExtension.getTestRunner(functionMetamodel);
        Assert.assertNotNull("Unable to get function test runner from testable extension", testRunner);
        return functionMetamodel._tests().flatCollect(testSuite ->
        {
            List<String> atomicTestIds = ((Root_meta_pure_test_TestSuite) testSuite)._tests().collect(TestAccessor::_id).toList();
            return testRunner.executeTestSuite((Root_meta_pure_test_TestSuite) testSuite, atomicTestIds, pureModel, pureModelContextData);
        }).toList();
    }

    private void testFailingTest(TestResult testResult, String expected, String actual)
    {
        TestExecuted testExecuted = guaranteedTestExecuted(testResult);
        Assert.assertEquals(TestExecutionStatus.FAIL, testExecuted.testExecutionStatus);
        AssertionStatus status = testExecuted.assertStatuses.get(0);
        if (status instanceof EqualToJsonAssertFail)
        {
            EqualToJsonAssertFail equalToJsonAssertFail = (EqualToJsonAssertFail)status;
            JsonAssert.assertJsonEquals(expected, equalToJsonAssertFail.expected);
            JsonAssert.assertJsonEquals(actual, equalToJsonAssertFail.actual);
        }
        else
        {
            throw new RuntimeException("Test Assertion" + status.id + " expected to fail");
        }
    }

    private TestExecuted guaranteedTestExecuted(TestResult result)
    {
        if (result instanceof  TestExecuted)
        {
            return (TestExecuted) result;
        }
        throw new RuntimeException("test expected to have been executed");
    }


    private boolean hasTestPassed(TestResult result)
    {
        if (result instanceof  TestExecuted)
        {
            return ((TestExecuted) result).testExecutionStatus.equals(TestExecutionStatus.PASS);
        }
        return false;
    }

    private TestResult findTestById(List<TestResult> results, String id)
    {
        return results.stream().filter(test -> test.atomicTestId.equals(id)).findFirst().orElseThrow(() -> new RuntimeException("Test Id " + id + " not found"));
    }

    private String getResourceAsString(String path)
    {
        try
        {
            URL infoURL = TestFunctionTestSuite.class.getClassLoader().getResource(path);
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
