// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.testable.function;

import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.function.extension.FunctionTestableRunnerExtension;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.testable.TestAccessor;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TestFunctionTestSuiteConcurrency
{
    private static final String GRAMMAR_FILE = "testable/legend-testable-function-test-relation-relationDatabaseAccessor.pure";
    private static final String FUNCTION_PATH = "model::RelationQuery__Relation_1_";

    private static final int CONCURRENT_USERS = 20;
    private static final int SUITES_PER_USER = 5;
    private static final int TIMEOUT_SECONDS = 180;

    @Test
    public void testConcurrentRelationTestExecutionsAllPass() throws Exception
    {

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_USERS);
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        AtomicInteger totalSuites = new AtomicInteger(0);

        for (int user = 0; user < CONCURRENT_USERS; user++)
        {
            executor.submit(() ->
            {
                try
                {
                    CompiledModel model = compileModel();
                    startGate.await();
                    for (int run = 0; run < SUITES_PER_USER; run++)
                    {
                        List<TestResult> results = runFunctionTestSuite(model);
                        assertAllPass(results);
                        totalSuites.incrementAndGet();
                    }
                }
                catch (Throwable t)
                {
                    errors.add(t);
                }
                finally
                {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = doneLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        executor.shutdownNow();

        Assert.assertTrue("Timed out waiting for concurrent test suites to complete", finished);

        Assert.assertTrue(
                "Concurrent test suite executions produced errors:\n" +
                        errors.stream().map(Throwable::getMessage).collect(Collectors.joining("\n")),
                errors.isEmpty()
        );

        Assert.assertEquals(
                "Expected " + (CONCURRENT_USERS * SUITES_PER_USER) + " suites to complete — some were silently dropped",
                CONCURRENT_USERS * SUITES_PER_USER,
                totalSuites.get()
        );
    }

    private CompiledModel compileModel()
    {
        String grammar = readResource(GRAMMAR_FILE);
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(grammar);
        PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        return new CompiledModel(pureModelContextData, pureModel);
    }

    private List<TestResult> runFunctionTestSuite(CompiledModel model)
    {
        FunctionTestableRunnerExtension extension = new FunctionTestableRunnerExtension();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element =
                model.pureModel.getPackageableElement(FUNCTION_PATH);
        Assert.assertTrue(element instanceof ConcreteFunctionDefinition);
        ConcreteFunctionDefinition<?> func = (ConcreteFunctionDefinition<?>) element;
        TestRunner runner = extension.getTestRunner(func);
        Assert.assertNotNull(runner);

        return func._tests().flatCollect(suite ->
        {
            List<String> ids = ((Root_meta_pure_test_TestSuite) suite)._tests()
                    .collect(TestAccessor::_id).toList();
            return runner.executeTestSuite((Root_meta_pure_test_TestSuite) suite, ids,
                    model.pureModel, model.pureModelContextData);
        }).toList();
    }

    private void assertAllPass(List<TestResult> results)
    {
        Assert.assertFalse("No test results were returned", results.isEmpty());
        for (TestResult result : results)
        {
            Assert.assertTrue(
                    "Test result was not TestExecuted: " + result.getClass().getSimpleName() + " id=" + result.atomicTestId,
                    result instanceof TestExecuted
            );
            TestExecuted executed = (TestExecuted) result;
            Assert.assertEquals(
                    "Test " + executed.atomicTestId + " did not PASS: " + executed.testExecutionStatus +
                            (executed.assertStatuses != null && !executed.assertStatuses.isEmpty()
                                    ? " / " + executed.assertStatuses.get(0)
                                    : ""),
                    TestExecutionStatus.PASS,
                    executed.testExecutionStatus
            );
        }
    }


    private String readResource(String path)
    {
        try
        {
            URL url = TestFunctionTestSuiteConcurrency.class.getClassLoader().getResource(path);
            Assert.assertNotNull("Resource not found: " + path, url);
            java.util.Scanner scanner = new java.util.Scanner(url.openStream()).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }

    private static class CompiledModel
    {
        final PureModelContextData pureModelContextData;
        final PureModel pureModel;

        CompiledModel(PureModelContextData pureModelContextData, PureModel pureModel)
        {
            this.pureModelContextData = pureModelContextData;
            this.pureModel = pureModel;
        }
    }
}
