// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.testable.extension;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestDebug;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;

import java.util.List;

public interface TestRunner
{
    default TestResult executeAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData data)
    {
        throw new UnsupportedOperationException("Atomic tests should be run in the context of a test suite");
    }

    default List<TestResult> executeTestSuite(Root_meta_pure_test_TestSuite testSuite, List<String> atomicTestIds, PureModel pureModel, PureModelContextData data)
    {
        try (TestSuiteSession<TestResult> session = openTestSuiteSession(testSuite, pureModel, data))
        {
            session.initialize();
            return ListIterate.collect(atomicTestIds, session::runAtomicTest);
        }
    }

    /**
     * What {@link #executeAtomicTest} would execute, without executing it: the execution plan, or
     * for a test that does not run one, the statements it would issue. A runner whose atomic tests
     * only run inside a suite throws here, as its {@link #executeAtomicTest} does.
     */
    TestDebug debugAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData data);

    default List<TestDebug> debugTestSuite(Root_meta_pure_test_TestSuite testSuite, List<String> atomicTestIds, PureModel pureModel, PureModelContextData data)
    {
        try (TestSuiteSession<TestDebug> session = openTestSuiteDebugSession(testSuite, pureModel, data))
        {
            session.initialize();
            return ListIterate.collect(atomicTestIds, session::runAtomicTest);
        }
    }

    /**
     * Open a session that manages resources for a test suite so the caller
     * can run many atomic tests without re-initializing against it without
     * re-paying that setup cost per test. The caller owns the returned
     * session and MUST close it (try-with-resources) so that any suite
     * resources are cleaned up at the end.
     */
    TestSuiteSession<TestResult> openTestSuiteSession(Root_meta_pure_test_TestSuite testSuite, PureModel pureModel, PureModelContextData data);

    /**
     * Open a session that does everything {@link #openTestSuiteSession} does up to execution
     * (runtime, test connections, plan generation) and returns, per atomic test, what that test
     * would execute instead of executing it. Every runner supports it: it is how Studio's debug and
     * EMIT's snapshots see what a test runs. (See {@link #openTestSuiteSession} for more
     * information about test suite sessions.)
     *
     * @return test suite debug session
     * @see #openTestSuiteSession
     */
    TestSuiteSession<TestDebug> openTestSuiteDebugSession(Root_meta_pure_test_TestSuite testSuite, PureModel pureModel, PureModelContextData data);
}
