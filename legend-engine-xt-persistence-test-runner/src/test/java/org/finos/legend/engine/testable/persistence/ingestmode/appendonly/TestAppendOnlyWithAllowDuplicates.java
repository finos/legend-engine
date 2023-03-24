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

package org.finos.legend.engine.testable.persistence.ingestmode.appendonly;

import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertPass;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.testable.persistence.ingestmode.TestPersistenceBase;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TestAppendOnlyWithAllowDuplicates extends TestPersistenceBase
{
    @Test
    public void testAppendOnlyWithNoAuditing() throws Exception
    {
        String path = "src/test/resources/append-only/allow_duplicates/persistence_no_audit_success.txt";
        String persistenceSpec = readPureCode(path);
        TestResult result = testPersistence(persistenceSpec).results.get(0);

        assertTrue(result instanceof TestExecuted);
        Assert.assertEquals(TestExecutionStatus.PASS, ((TestExecuted) result).testExecutionStatus);
        Assert.assertEquals("test::TestPersistence", result.testable);
    }

    @Test
    public void testAppendOnlyWithDateTimeAuditing() throws Exception
    {
        String path = "src/test/resources/append-only/allow_duplicates/persistence_date_time_auditing.txt";
        String persistenceSpec = readPureCode(path);
        TestResult result = testPersistence(persistenceSpec).results.get(0);

        assertTrue(result instanceof TestExecuted);
        Assert.assertEquals(TestExecutionStatus.PASS, ((TestExecuted) result).testExecutionStatus);
        Assert.assertEquals("test::TestPersistence", result.testable);
    }

    @Test
    public void testAppendOnlyWithFailure() throws Exception
    {
        String path = "src/test/resources/append-only/allow_duplicates/persistence_no_audit_fail.txt";
        String persistenceSpec = readPureCode(path);
        TestResult result = testPersistence(persistenceSpec).results.get(0);

        assertTrue(result instanceof TestExecuted);
        Assert.assertEquals(TestExecutionStatus.FAIL, ((TestExecuted) result).testExecutionStatus);
        Assert.assertEquals("test::TestPersistence", result.testable);
        TestExecuted testFailed = (TestExecuted) result;
        EqualToJsonAssertFail batch1Status = (EqualToJsonAssertFail) testFailed.assertStatuses.get(0);
        AssertPass batch2Status = (AssertPass) testFailed.assertStatuses.get(1);

        // no space
        Assert.assertEquals("[{\"ID\":1,\"NAME\":\"ANDY\"},{\"ID\":2,\"NAME\":\"BRAD\"}]", batch1Status.actual);
        // with space
        Assert.assertEquals("[{\"ID\":1, \"NAME\":\"CHLOE\"},{\"ID\":2, \"NAME\":\"BRAD\"}]", batch1Status.expected);
        Assert.assertTrue(batch1Status.message.contains("AssertionError: Results do not match the expected data"));
    }

    @Test
    public void testAppendOnlyWithWithError() throws Exception
    {
        String path = "src/test/resources/append-only/allow_duplicates/persistence_no_audit_error.txt";
        String persistenceSpec = readPureCode(path);
        TestResult result = testPersistence(persistenceSpec).results.get(0);

        assertTrue(result instanceof TestExecuted);
        Assert.assertEquals(TestExecutionStatus.FAIL, ((TestExecuted) result).testExecutionStatus);
        Assert.assertEquals("test::TestPersistence", result.testable);
        TestExecuted testFailed = (TestExecuted) result;
        AssertFail batch1Status = (AssertFail) testFailed.assertStatuses.get(0);
        Assert.assertTrue(batch1Status.message.contains("Unexpected close marker '}'"));
    }
}
