//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.authorization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TestResourceContextNormalizationForDatabase
{
    private final String databaseName;
    private String hostName;
    private int port;
    private final String expectedNormalizedValue;

    public TestResourceContextNormalizationForDatabase(String databaseName, String hostName, int port, String expectedNormalizedValue)
    {
        this.databaseName = databaseName;
        this.hostName = hostName;
        this.port = port;
        this.expectedNormalizedValue = expectedNormalizedValue;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][]{
                {"trade_db_dev", "host1.example.com", 1234, "DB@trade_db_dev@host1.example.com@1234"},
        });
    }

    @Test
    public void testNormalization()
    {
        String actualNormalizedValue = RelationalMiddleTierPlanExecutionAuthorizer.normalizeDatabaseResourceContext(this.databaseName, this.hostName, this.port);
        assertEquals(expectedNormalizedValue, actualNormalizedValue);
    }
}
