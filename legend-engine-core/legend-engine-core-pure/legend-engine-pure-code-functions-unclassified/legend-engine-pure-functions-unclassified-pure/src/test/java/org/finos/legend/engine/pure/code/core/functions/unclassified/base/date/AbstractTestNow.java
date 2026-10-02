// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core.functions.unclassified.base.date;

import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

public abstract class AbstractTestNow extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testNow()
    {
        compileTestSource("function test::nowWrapper():DateTime[1] { meta::pure::functions::date::now() }");
        long tolerance = 100;
        long before = System.currentTimeMillis();
        CoreInstance result = execute("test::nowWrapper():DateTime[1]");
        long after = System.currentTimeMillis();
        for (int i = 0; ((after - before) > tolerance) && (i < 10); i++)
        {
            before = System.currentTimeMillis();
            result = execute("test::nowWrapper():DateTime[1]");
            after = System.currentTimeMillis();
        }
        if ((after - before) > tolerance)
        {
            throw new RuntimeException("Could not get valid test, total duration (" + (after - before) + "ms) was greater than tolerance (" + tolerance + "ms)");
        }

        CoreInstance date = Instance.getValueForMetaPropertyToOneResolved(result, M3Properties.values, processorSupport);
        PureDate pureDate = PrimitiveUtilities.getDateValue(date);

        // Check that the date has millisecond precision
        Assert.assertTrue(pureDate.hasSubsecond());
        Assert.assertEquals(3, pureDate.getSubsecond().length());

        // Compare with before and after epoch millis
        long actual = pureDate.getCalendar().getTimeInMillis();
        Assert.assertTrue("Expected actual (" + pureDate + ") to be between " + Instant.ofEpochMilli(before) + " and " + Instant.ofEpochMilli(after), (before <= actual) && (actual <= after));
    }
}
