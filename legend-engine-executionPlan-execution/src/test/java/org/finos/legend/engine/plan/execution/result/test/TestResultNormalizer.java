// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.result.test;

import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.execution.result.ResultNormalizer;
import org.junit.Assert;
import org.junit.Test;

public class TestResultNormalizer
{
    @Test
    public void testStrictDateNormalization()
    {
        PureDate date = PureDate.parsePureDate("2020-01-01");
        String normalized = (String) ResultNormalizer.normalizeToSql(date);
        Assert.assertEquals("2020-01-01", normalized);
    }

    @Test
    public void testDateTimeNormalization()
    {
        PureDate date = PureDate.parsePureDate("2020-01-01T12:00:00+0530");
        String normalized = (String) ResultNormalizer.normalizeToSql(date);
        Assert.assertEquals("2020-01-01 06:30:00", normalized);
    }

    @Test
    public void testDateTimeWithSubSecondNormalization()
    {
        PureDate date = PureDate.parsePureDate("2020-01-01T12:00:00.345678+0530");
        String normalized = (String) ResultNormalizer.normalizeToSql(date);
        Assert.assertEquals("2020-01-01 06:30:00.345678", normalized);
    }

    @Test
    public void testNormalizationWithDbTimeZone()
    {
        PureDate date = PureDate.parsePureDate("2020-01-01T12:00:00.345678+0530");
        String normalized = (String) ResultNormalizer.normalizeToSql(date, "IST");
        Assert.assertEquals("2020-01-01 12:00:00.345678", normalized);
    }

    @Test
    public void testStringNormalization()
    {
        String inputToQuote = "'String Value'";
        String inputToQuoteNormalized = (String) ResultNormalizer.normalizeToSql(inputToQuote);
        Assert.assertEquals("\'String Value\'", inputToQuoteNormalized);

        String inputWithBackslash = "Value\\,String";
        String inputWithBackslashNormalized = (String) ResultNormalizer.normalizeToSql(inputWithBackslash);
        Assert.assertEquals("Value\\\\,String", inputWithBackslashNormalized);
    }
}
