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

package org.finos.legend.engine.testable.mapping.extension;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.extension.TestSuiteSession;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.testable.TestAccessor;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs many atomic tests of a single {@link MappingTestRunner} session concurrently. All shared
 * session state must be safe to read from concurrent test threads, and the per-test path must
 * not race through shared mutable plan or executor-argument state. Both grammars end up in
 * shared-plan mode (ModelStore plans do not depend on the data content), so the second variant
 * — three tests with three different inline datasets — is the critical one: each concurrent
 * execution must see its own test's data (passed per execution via thread-local stream bound
 * during that test's own connection build), with no bleed between threads.
 */
public class TestMappingTestSuiteConcurrency
{
    private static final int THREADS = 8;
    private static final int EXECUTIONS = 48;
    private static final int TIMEOUT_SECONDS = 180;

    private static final String GRAMMAR_BASE = "###Pure\n" +
            "Class test::model\n" +
            "{\n" +
            "    name: String[1];\n" +
            "    id: String[1];\n" +
            "}\n" +
            "\n" +
            "Class test::changedModel{    name: String[1];    id: Integer[1];}\n" +
            "###Data\n" +
            "Data test::data::MyData\n" +
            "{\n" +
            "  ExternalFormat\n" +
            "  #{\n" +
            "    contentType: 'application/json';\n" +
            "    data: '{\"name\":\"john doe\",\"id\":\"77\"}';\n" +
            "  }#\n" +
            "}\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping test::modelToModelMapping\n" +
            "(\n" +
            "    *test::changedModel: Pure\n" +
            "{\n" +
            "    ~src test::model\n" +
            "    name: $src.name,\n" +
            "    id: $src.id->parseInteger()\n" +
            "}\n" +
            "\n";

    private static final String SHARED_DATA_TEST_1 = sharedDataTest("test1");
    private static final String SHARED_DATA_TEST_2 = sharedDataTest("test2");

    private static final String SHARED_DATA_GRAMMAR = GRAMMAR_BASE +
            "  testSuites:\n" +
            "  [\n" +
            "    testSuite1:\n" +
            "    {\n" +
            "      function: |test::changedModel.all()->graphFetch(#{test::changedModel{id,name}}#)->serialize(#{test::changedModel{id,name}}#);\n" +
            "      tests:\n" +
            "      [\n" +
            SHARED_DATA_TEST_1 + ",\n" +
            SHARED_DATA_TEST_2 + "\n" +
            "      ];\n" +
            "    }\n" +
            "  ]\n" +
            ")\n";

    private static final String PER_TEST_DATA_GRAMMAR = GRAMMAR_BASE +
            "  testSuites:\n" +
            "  [\n" +
            "    testSuite1:\n" +
            "    {\n" +
            "      function: |test::changedModel.all()->graphFetch(#{test::changedModel{id,name}}#)->serialize(#{test::changedModel{id,name}}#);\n" +
            "      tests:\n" +
            "      [\n" +
            inlineDataTest("test1", "jane", "1") + ",\n" +
            inlineDataTest("test2", "john", "2") + ",\n" +
            inlineDataTest("test3", "judy", "3") + "\n" +
            "      ];\n" +
            "    }\n" +
            "  ]\n" +
            ")\n";

    @Test
    public void testConcurrentAtomicTestsWithSharedPlan() throws Exception
    {
        runSuiteTestsConcurrently(SHARED_DATA_GRAMMAR);
    }

    @Test
    public void testConcurrentAtomicTestsWithSharedPlanAndPerTestData() throws Exception
    {
        runSuiteTestsConcurrently(PER_TEST_DATA_GRAMMAR);
    }

    private void runSuiteTestsConcurrently(String grammar) throws Exception
    {
        PureModelContextData pmcd = PureGrammarParser.newInstance().parseModel(grammar);
        PureModel pureModel = Compiler.compile(pmcd, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        Mapping mapping = (Mapping) pureModel.getPackageableElement("test::modelToModelMapping");

        MappingTestableRunnerExtension extension = new MappingTestableRunnerExtension();
        extension.setPureVersion(PureClientVersions.production);
        TestRunner runner = extension.getTestRunner(mapping);
        Root_meta_pure_test_TestSuite suite = (Root_meta_pure_test_TestSuite) mapping._tests().getOnly();
        MutableList<String> atomicTestIds = suite._tests().collect(TestAccessor::_id, Lists.mutable.empty());

        try (TestSuiteSession<TestResult> session = runner.openTestSuiteSession(suite, pureModel, pmcd))
        {
            session.initialize();
            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            try
            {
                MutableList<Future<TestResult>> futures = Lists.mutable.empty();
                for (int i = 0; i < EXECUTIONS; i++)
                {
                    String atomicTestId = atomicTestIds.get(i % atomicTestIds.size());
                    futures.add(pool.submit(() -> session.runAtomicTest(atomicTestId)));
                }
                for (Future<TestResult> future : futures)
                {
                    TestResult result = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    Assert.assertTrue(
                            "Expected TestExecuted, got " + result.getClass().getSimpleName() + " for " + result.atomicTestId
                                    + ((result instanceof TestError) ? ": " + ((TestError) result).error : ""),
                            result instanceof TestExecuted);
                    TestExecuted executed = (TestExecuted) result;
                    Assert.assertEquals(
                            "Test " + executed.atomicTestId + " did not PASS" +
                                    ((executed.assertStatuses != null && !executed.assertStatuses.isEmpty()) ? " / " + executed.assertStatuses.get(0) : ""),
                            TestExecutionStatus.PASS,
                            executed.testExecutionStatus);
                }
            }
            finally
            {
                pool.shutdownNow();
            }
        }
    }

    private static String sharedDataTest(String testId)
    {
        return "        " + testId + ":\n" +
                "        {\n" +
                "          data:\n" +
                "          [\n" +
                "           ModelStore: ModelStore\n" +
                "            #{\n" +
                "               test::model:\n" +
                "                Reference\n" +
                "                #{\n" +
                "                  test::data::MyData\n" +
                "                }#\n" +
                "            }#\n" +
                "          ];\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected :\n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"id\" : 77, \"name\" : \"john doe\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        }";
    }

    private static String inlineDataTest(String testId, String name, String id)
    {
        return "        " + testId + ":\n" +
                "        {\n" +
                "          data:\n" +
                "          [\n" +
                "           ModelStore: ModelStore\n" +
                "            #{\n" +
                "               test::model:\n" +
                "               ExternalFormat\n" +
                "               #{\n" +
                "                 contentType: 'application/json';\n" +
                "                 data: '{\"name\":\"" + name + "\",\"id\":\"" + id + "\"}';\n" +
                "               }#\n" +
                "            }#\n" +
                "          ];\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected :\n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"id\" : " + id + ", \"name\" : \"" + name + "\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        }";
    }
}
