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

package org.finos.legend.engine.test.emit;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.test.emit.EMITTasks.ParseResult;
import org.finos.legend.engine.test.emit.error.EMITException;
import org.finos.legend.engine.testable.extension.TestSuiteSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Direct coverage of {@link EMITTasks#openTestSession} and the
 * {@link TestSuiteSession} contract it returns. The fixtures are loaded
 * via {@link EMITModelLoader} + {@link EMITTasks#parse} + {@link EMITTasks#compile}
 * so the tests can drive the session API independently of the JUnit
 * builder, isolating session semantics from the per-test wiring in
 * {@code EMITTestSuiteBuilder}.
 *
 * <p>The {@code m2m-passing} model carries a Mapping with one test suite
 * ({@code fullNameSuite}) of two atomic tests ({@code johnSmith} and
 * {@code janeDoe}) — the canonical shape for verifying that a session can
 * drive more than one atomic test against the same per-suite setup.</p>
 */
public class TestEMITTaskSessions
{
    private static final String TESTABLE_PATH = "demo::PersonM2MMapping";
    private static final String SUITE_ID = "fullNameSuite";

    private static PureModelContextData pmcd;
    private static PureModel pureModel;

    @BeforeAll
    static void parseAndCompileOnce() throws IOException
    {
        EMITSourceSet sourceSet = new EMITModelLoader().load(resource("emit-models/basic/m2m-passing.emit.yaml"));
        ParseResult parseResult = EMITTasks.parse(sourceSet);
        pmcd = parseResult.getPmcd();
        pureModel = EMITTasks.compile(pmcd);
    }

    @Test
    void sessionDrivesEveryAtomicTestInTheSuite()
    {
        try (TestSuiteSession<TestResult> session = EMITTasks.openTestSession(TESTABLE_PATH, SUITE_ID, pmcd, pureModel))
        {
            assertPasses(session.runAtomicTest("johnSmith"), "johnSmith");
            assertPasses(session.runAtomicTest("janeDoe"), "janeDoe");
        }
    }

    @Test
    void sessionAtomicTestsAreIndependentAcrossCalls()
    {
        // Running the same atomic test twice through one session must yield
        // the same outcome each time — the cached suite setup is not consumed
        // by an invocation and is safe to reuse.
        try (TestSuiteSession<TestResult> session = EMITTasks.openTestSession(TESTABLE_PATH, SUITE_ID, pmcd, pureModel))
        {
            assertPasses(session.runAtomicTest("johnSmith"), "johnSmith (first call)");
            assertPasses(session.runAtomicTest("johnSmith"), "johnSmith (second call)");
            assertPasses(session.runAtomicTest("janeDoe"), "janeDoe (after re-run of johnSmith)");
        }
    }

    @Test
    void sessionRunAtomicTestForUnknownIdReturnsNull()
    {
        try (TestSuiteSession<TestResult> session = EMITTasks.openTestSession(TESTABLE_PATH, SUITE_ID, pmcd, pureModel))
        {
            Assertions.assertNull(session.runAtomicTest("does-not-exist"),
                    "runAtomicTest with an unknown id should return null, not throw");
        }
    }

    @Test
    void sessionRunAtomicTestAfterCloseThrows()
    {
        TestSuiteSession<TestResult> session = EMITTasks.openTestSession(TESTABLE_PATH, SUITE_ID, pmcd, pureModel);
        session.close();
        Assertions.assertThrows(IllegalStateException.class,
                () -> session.runAtomicTest("johnSmith"),
                "runAtomicTest after close must throw IllegalStateException");
    }

    @Test
    void sessionCloseIsIdempotent()
    {
        TestSuiteSession<TestResult> session = EMITTasks.openTestSession(TESTABLE_PATH, SUITE_ID, pmcd, pureModel);
        session.close();
        Assertions.assertDoesNotThrow(session::close, "Second close must be a no-op");
    }

    @Test
    void reopeningASessionForTheSameSuiteWorksIndependently()
    {
        // A fresh session after the previous one was closed must build its own
        // setup and run cleanly — i.e. the close on a prior session leaves no
        // residual state that breaks a later open.
        try (TestSuiteSession<TestResult> first = EMITTasks.openTestSession(TESTABLE_PATH, SUITE_ID, pmcd, pureModel))
        {
            assertPasses(first.runAtomicTest("johnSmith"), "first session / johnSmith");
        }
        try (TestSuiteSession<TestResult> second = EMITTasks.openTestSession(TESTABLE_PATH, SUITE_ID, pmcd, pureModel))
        {
            assertPasses(second.runAtomicTest("janeDoe"), "second session / janeDoe");
        }
    }

    @Test
    void openTestSessionForUnknownSuiteThrowsEmitException()
    {
        EMITException ex = Assertions.assertThrows(EMITException.class,
                () -> EMITTasks.openTestSession(TESTABLE_PATH, "no-such-suite", pmcd, pureModel),
                "Opening a session for a non-existent suite id should fail fast");
        Assertions.assertEquals(EMITPhase.TEST_EXECUTION, ex.getPhase());
        Assertions.assertTrue(ex.getMessage().contains("no-such-suite"), () -> "Message should mention the missing suite id; got: " + ex.getMessage());
    }

    @Test
    void openTestSessionForNonTestableElementThrowsEmitException()
    {
        EMITException ex = Assertions.assertThrows(EMITException.class,
                () -> EMITTasks.openTestSession("demo::source::Person", SUITE_ID, pmcd, pureModel),
                "Opening a session against a non-testable element should fail fast");
        Assertions.assertEquals(EMITPhase.TEST_EXECUTION, ex.getPhase());
        Assertions.assertTrue(ex.getMessage().contains("not a testable element") || ex.getMessage().contains("demo::source::Person"),
                () -> "Message should explain the testable-element rejection; got: " + ex.getMessage());
    }

    @Test
    void openTestSessionRejectsNullSuiteId()
    {
        // A session is a per-suite amortization handle, so it requires a real suite id. Suite-less
        // top-level atomic tests (e.g. Persistence) have no suite to amortize and are run standalone
        // via EMITTasks.runTest, not through a fabricated "no-suite" session.
        Assertions.assertThrows(NullPointerException.class,
                () -> EMITTasks.openTestSession(TESTABLE_PATH, null, pmcd, pureModel),
                "openTestSession must reject a null suiteId rather than fabricating a suite-less session");
    }

    private static void assertPasses(TestResult result, String label)
    {
        Assertions.assertNotNull(result, () -> "Expected a TestResult for " + label + " but got null");
        Assertions.assertInstanceOf(TestExecuted.class, result, () -> "Expected TestExecuted for " + label + " but got " + result.getClass().getSimpleName());
        TestExecuted executed = (TestExecuted) result;
        Assertions.assertEquals(TestExecutionStatus.PASS, executed.testExecutionStatus,
                () -> "Expected " + label + " to PASS but got " + executed.testExecutionStatus + "; assertions=" + executed.assertStatuses);
    }

    private static Path resource(String name)
    {
        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        Assertions.assertNotNull(url, "test resource not found: " + name);
        try
        {
            return Paths.get(url.toURI());
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }
}
