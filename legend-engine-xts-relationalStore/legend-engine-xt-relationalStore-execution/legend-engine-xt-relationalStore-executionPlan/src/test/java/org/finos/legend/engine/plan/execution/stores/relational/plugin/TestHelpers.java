// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.plugin;

import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class TestHelpers
{
    @Test
    public void testNormalizerWithSingleQuotes()
    {
        Assert.assertEquals("'There''s a quote'", RelationalExecutionNodeExecutor.getNormalizer("''", "GMT").apply("There's a quote"));
    }

    // A StrictDate cross-key inserted into a TIMESTAMP temp table column comes back as
    // DateTime at midnight with subseconds (e.g. "000000000"). These tests verify that
    // parent-child matching still succeeds despite the precision promotion.

    @Test
    public void testCrossKeyValuesEqual_StrictDateMatchesDateTimeAtMidnight() throws Exception
    {
        PureDate strictDate = PureDate.newPureDate(2026, 4, 9);
        PureDate dateTimeAtMidnight = PureDate.newPureDate(2026, 4, 9, 0, 0, 0, "000000000");

        Assert.assertTrue(invokeCrossKeyValuesEqual(strictDate, dateTimeAtMidnight));
        Assert.assertTrue(invokeCrossKeyValuesEqual(dateTimeAtMidnight, strictDate));
    }

    @Test
    public void testCrossKeyValuesEqual_StrictDateDoesNotMatchNonMidnight() throws Exception
    {
        PureDate strictDate = PureDate.newPureDate(2026, 4, 9);
        PureDate dateTimeNonMidnight = PureDate.newPureDate(2026, 4, 9, 10, 30, 0);

        Assert.assertFalse(invokeCrossKeyValuesEqual(strictDate, dateTimeNonMidnight));
    }

    @Test
    public void testCrossKeyValueHashCode_ConsistentForStrictDateAndMidnightDateTime() throws Exception
    {
        PureDate strictDate = PureDate.newPureDate(2026, 4, 9);
        PureDate dateTimeAtMidnight = PureDate.newPureDate(2026, 4, 9, 0, 0, 0, "000000000");

        // hashCode() must be the same for the HashMap bucket lookup to work
        Assert.assertEquals(
                RelationalGraphFetchUtils.crossKeyValueHashCode(strictDate),
                RelationalGraphFetchUtils.crossKeyValueHashCode(dateTimeAtMidnight)
        );
    }

    private static boolean invokeCrossKeyValuesEqual(Object a, Object b) throws Exception
    {
        Method m = RelationalGraphFetchUtils.class.getDeclaredMethod("crossKeyValuesEqual", Object.class, Object.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, a, b);
    }
}
