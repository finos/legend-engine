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

import java.util.Collections;
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

    default TestDebug debugAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData data)
    {
        return null;
    }

    default List<TestDebug> debugTestSuite(Root_meta_pure_test_TestSuite testSuite, List<String> atomicTestIds, PureModel pureModel, PureModelContextData data)
    {
        if (!supportsDebug())
        {
            return Collections.emptyList();
        }
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
     * Return whether the runner supports debug mode. If so, then a debug
     * session may be created by calling {@link #openTestSuiteDebugSession}.
     * Otherwise, that method with throw an {@link UnsupportedOperationException}.
     *
     * @return whether the runner supports debug
     * @see #openTestSuiteDebugSession
     */
    default boolean supportsDebug()
    {
        return false;
    }

    /**
     * If the runner supports debug, then this will return a test suite debug session.
     * (See {@link #openTestSuiteSession} for more information about test suite sessions.)
     * If the runner does not support debug, this will throw an {@link UnsupportedOperationException}.
     *
     * @return test suite debug session
     * @see #supportsDebug
     * @see #openTestSuiteSession
     */
    default TestSuiteSession<TestDebug> openTestSuiteDebugSession(Root_meta_pure_test_TestSuite testSuite, PureModel pureModel, PureModelContextData data)
    {
        throw new UnsupportedOperationException("Debug is not supported");
    }
}
