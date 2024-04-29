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
public class TestResourceContextNormalizationForService
{
    private final String servicePath;
    private String serviceUniqueId;
    private final String expectedNormalizedValue;

    public TestResourceContextNormalizationForService(String servicePath, String serviceUniqueId, String expectedNormalizedValue)
    {
        this.servicePath = servicePath;
        this.serviceUniqueId = serviceUniqueId;
        this.expectedNormalizedValue = expectedNormalizedValue;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][]{
                {"/a/b", "901:1234", "id@901_1234@_a_b"},
                {"/a/b/", "901:1234", "id@901_1234@_a_b_"},
                {"/a/b{date}", "901:1234", "id@901_1234@_a_b_date_"},
        });
    }

    @Test
    public void testNormalization()
    {
        String actualNormalizedValue = RelationalMiddleTierPlanExecutionAuthorizer.normalizeServiceResourceContext(this.servicePath, this.serviceUniqueId);
        assertEquals(expectedNormalizedValue, actualNormalizedValue);
    }
}
