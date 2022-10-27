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

package org.finos.legend.engine.testable.persistence.ingestmode.delta.unitemporal;

import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestFailed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestPassed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.testable.persistence.ingestmode.TestPersistenceBase;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TestPersistenceUnitemporalDelta extends TestPersistenceBase
{
    @Test
    public void testBatchIdBasedNoDeleteIndicator() throws Exception
    {
        String path = "src/test/resources/unitemporal-delta/batch_id_based/persistence_no_delete_indicator.txt";
        String persistenceSpec = readPureCode(path);

        TestResult result = testPersistence(persistenceSpec).results.get(0);

        assertTrue(result instanceof TestPassed);
        Assert.assertEquals("test::TestPersistence", ((TestPassed) result).testable);
    }

    @Test
    public void testBatchIdAndTimeBasedNoDeleteIndicator() throws Exception
    {
        String path = "src/test/resources/unitemporal-delta/batch_id_and_date_time_based/persistence_no_delete_indicator.txt";
        String persistenceSpec = readPureCode(path);

        TestResult result = testPersistence(persistenceSpec).results.get(0);

        assertTrue(result instanceof TestPassed);
        Assert.assertEquals("test::TestPersistence", ((TestPassed) result).testable);
    }

    @Test
    public void testTimeBasedWithDeleteIndicator() throws Exception
    {
        String path = "src/test/resources/unitemporal-delta/date_time_based/persistence_with_delete_indicator.txt";
        String persistenceSpec = readPureCode(path);

        TestResult result = testPersistence(persistenceSpec).results.get(0);

        assertTrue(result instanceof TestPassed);
        Assert.assertEquals("test::TestPersistence", ((TestPassed) result).testable);
    }
}
