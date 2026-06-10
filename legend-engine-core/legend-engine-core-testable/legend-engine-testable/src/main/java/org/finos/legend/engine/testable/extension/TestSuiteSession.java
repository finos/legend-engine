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

package org.finos.legend.engine.testable.extension;

import java.util.Collection;

/**
 * <p>Session to manage resources that are created when {@link #initialize}
 * is called and persist throughout all atomic test runs until {@link #close}
 * is called.</p>
 *
 * <p>Obtained via {@link TestRunner#openTestSuiteSession}. The caller owns
 * the session lifetime and must close it (try-with-resources) so that
 * runtime and connection resources are released. After {@link #close} the
 * session must not be used again. If {@link #initialize} is not explicitly
 * called, initialization will be performed automatically when the first test
 * is run.</p>
 *
 * <p>The contract over multiple {@link #runAtomicTest} calls matches the
 * single-call contract of {@link TestRunner#executeTestSuite}: each
 * atomic-test result is independent, a failure in one does not abort the
 * session, and the runner returns a per-test error rather than throwing
 * for an individual test failure.</p>
 */
public interface TestSuiteSession<TR> extends AutoCloseable
{
    /**
     * Get the id of the test suite.
     *
     * @return test suite id
     */
    String getTestSuiteId();

    /**
     * Get the ids of the atomic tests in the suite
     *
     * @return ids of atomic tests in the suite
     */
    Collection<String> getAtomicTestIds();

    /**
     * Return whether the given string is an id for an atomic test in the suite.
     *
     * @param id id string
     * @return whether id is an atomic test id for the suite
     */
    boolean isAtomicTestId(String id);

    /**
     * Perform any initialization necessary before running tests from the suite. Should
     * be called before calling {@link #runAtomicTest} for the first time. Idempotent:
     * once initialized, subsequent calls have no effect.
     */
    void initialize();

    /**
     * Run one atomic test against the cached suite-level setup.
     *
     * @param atomicTestId id of an atomic test in the suite the session was opened for
     * @return result of executing that atomic test, or {@code null} if no
     * such test exists in the suite
     */
    TR runAtomicTest(String atomicTestId);

    /**
     * Release any resources held by the session (connections, plan
     * executor handles, runtime closeables, etc). Idempotent: safe to call
     * more than once. The session must not be used after close.
     */
    @Override
    void close();
}
