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

package org.finos.legend.engine.plan.execution.dependencies.domain.date.test;

import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.junit.Assert;
import org.junit.Test;

public class TestPureDate
{
    @Test
    public void testParsePureDate()
    {
        Assert.assertEquals(PureDate.newPureDate(2014, 2, 27, 10, 1, 35, "231"), PureDate.parsePureDate("2014-02-27T10:01:35.231"));
        Assert.assertEquals(PureDate.newPureDate(2014, 2, 27, 10, 1, 35, "231"), PureDate.parsePureDate("2014-2-27T10:01:35.231"));

        Assert.assertEquals(PureDate.newPureDate(2014, 2, 27, 10, 1, 35, "231"), PureDate.parsePureDate("2014-02-27T10:01:35.231Z"));
        Assert.assertEquals(PureDate.newPureDate(2014, 2, 27, 10, 1, 35, "231"), PureDate.parsePureDate("2014-2-27T10:01:35.231Z"));
        Assert.assertEquals(PureDate.newPureDate(2014, 2, 27, 10, 1, 35, "231878"), PureDate.parsePureDate("2014-2-27T10:01:35.231878Z"));
        Assert.assertEquals(PureDate.newPureDate(2014, 2, 27, 10, 1, 35, "000000"), PureDate.parsePureDate("2014-2-27T10:01:35.000000Z"));
        Assert.assertEquals(PureDate.newPureDate(2014, 2, 27, 10, 1, 35), PureDate.parsePureDate("2014-2-27T10:01:35Z"));

        Assert.assertEquals(PureDate.newPureDate(2014, 2, 27, 15, 1, 35, "231"), PureDate.parsePureDate("2014-02-27T10:01:35.231-0500"));
        Assert.assertEquals(PureDate.newPureDate(2014, 2, 27, 5, 1, 35, "231"), PureDate.parsePureDate("2014-02-27T10:01:35.231+0500"));
    }

    @Test
    public void testFormat()
    {
        PureDate date = PureDate.newPureDate(2014, 3, 10, 16, 12, 35, "070004235");
        Assert.assertEquals("2014", date.format("yyyy"));

        Assert.assertEquals("2014-3", date.format("yyyy-M"));
        Assert.assertEquals("2014-03", date.format("yyyy-MM"));
        Assert.assertEquals("2014-003", date.format("yyyy-MMM"));
        Assert.assertEquals("2014-03-10", date.format("yyyy-MM-d"));
        Assert.assertEquals("2014-03-10", date.format("yyyy-MM-dd"));
        Assert.assertEquals("2014-03-10 4:12:35PM", date.format("yyyy-MM-dd h:mm:ssa"));
        Assert.assertEquals("2014-03-10 16:12:35.0700 GMT", date.format("yyyy-MM-dd HH:mm:ss.SSSS z"));
        Assert.assertEquals("2014-03-10T16:12:35.0700+0000", date.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSSZ"));
        Assert.assertEquals("2014-03-10 16:12:35.070Z", date.format("yyyy-MM-dd HH:mm:ss.SSSX"));
        Assert.assertEquals("2014-03-10 16:12:35.070004", date.format("yyyy-MM-dd HH:mm:ss.SSSSSS"));
        Assert.assertEquals("2014-03-10 16:12:35.070004235", date.format("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"));
    }

    @Test
    public void testFormatWithTimeZoneShift()
    {
        PureDate date = PureDate.newPureDate(2014, 1, 1, 1, 1, 1, "070004235");
        Assert.assertEquals("2014-01-01 01:01:01.070+0000", date.format("yyyy-MM-dd HH:mm:ss.SSSZ"));
        Assert.assertEquals("2014-01-01 01:01:01.070 GMT", date.format("yyyy-MM-dd HH:mm:ss.SSS z"));
        Assert.assertEquals("2014-01-01 01:01:01.070Z", date.format("yyyy-MM-dd HH:mm:ss.SSSX"));

        Assert.assertEquals("2013-12-31 20:01:01.070-0500", date.format("[EST]yyyy-MM-dd HH:mm:ss.SSSZ"));
        Assert.assertEquals("2013-12-31 20:01:01.070 EST", date.format("[EST]yyyy-MM-dd HH:mm:ss.SSS z"));
        Assert.assertEquals("2013-12-31 20:01:01.070-05", date.format("[EST]yyyy-MM-dd HH:mm:ss.SSSX"));

        Assert.assertEquals("2013-12-31 19:01:01.070-0600", date.format("[CST]yyyy-MM-dd HH:mm:ss.SSSZ"));
        Assert.assertEquals("2013-12-31 19:01:01.070 CST", date.format("[CST]yyyy-MM-dd HH:mm:ss.SSS z"));
        Assert.assertEquals("2013-12-31 19:01:01.070-06", date.format("[CST]yyyy-MM-dd HH:mm:ss.SSSX"));

        Assert.assertEquals("2014-01-01 02:01:01.070+0100", date.format("[CET]yyyy-MM-dd HH:mm:ss.SSSZ"));
        Assert.assertEquals("2014-01-01 02:01:01.070 CET", date.format("[CET]yyyy-MM-dd HH:mm:ss.SSS z"));
        Assert.assertEquals("2014-01-01 02:01:01.070+01", date.format("[CET]yyyy-MM-dd HH:mm:ss.SSSX"));
    }

    @Test
    public void testFormatWithTimeZoneShiftButNoHour()
    {
        PureDate date = PureDate.newPureDate(2015, 8, 15);
        Assert.assertEquals("2015-08-15", date.format("yyyy-MM-dd"));
        Assert.assertEquals("2015-08-15", date.format("[EST]yyyy-MM-dd"));
        Assert.assertEquals("2015-08-15", date.format("[CST]yyyy-MM-dd"));
        Assert.assertEquals("2015-08-15", date.format("[CET]yyyy-MM-dd"));
    }

    @Test
    public void testFormatWithMultipleTimeZones()
    {
        PureDate date = PureDate.newPureDate(2014, 1, 1, 1, 1, 1, "070004235");
        Assert.assertEquals("2013-12-31 20:01:01.070-0500", date.format("[EST]yyyy-MM-dd HH:mm:ss.SSSZ"));
        Assert.assertEquals("2013-12-31 20:01:01.070-0500", date.format("[EST]yyyy-MM-dd [EST]HH:mm:ss.SSSZ"));
        try
        {
            date.format("[EST]yyyy-MM-dd [CST] HH:mm:ss.SSSZ");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Cannot set multiple timezones: EST, CST", e.getMessage());
        }
    }

    @Test
    public void testFormatRefersToNonexistentComponent()
    {
        // When a date has no hour, timezone shifts are no-ops, so multiple timezones don't conflict
        PureDate date = PureDate.newPureDate(2014, 1, 1);
        Assert.assertEquals("2014-01-01 00:00:00.000+0000", date.format("[EST]yyyy-MM-dd[CST] HH:mm:ss.SSSZ"));
    }

    @Test
    public void testFormatISO8601SubSecondPrecisionVariants()
    {
        String fmt9 = "yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSSSSZ";

        // 1. No sub-seconds with various S format lengths
        PureDate noSubSec = PureDate.parsePureDate("2023-01-01T00:00:00Z");
        Assert.assertEquals("2023-01-01T00:00:00.0+0000", noSubSec.format("yyyy-MM-dd\"T\"HH:mm:ss.SZ"));
        Assert.assertEquals("2023-01-01T00:00:00.000+0000", noSubSec.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSZ"));
        Assert.assertEquals("2023-01-01T00:00:00.000000+0000", noSubSec.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSZ"));
        Assert.assertEquals("2023-01-01T00:00:00.000000000+0000", noSubSec.format(fmt9));

        // 2. 1 digit sub-second
        PureDate oneSub = PureDate.parsePureDate("2023-01-01T12:30:45.1Z");
        Assert.assertEquals("2023-01-01T12:30:45.1+0000", oneSub.format("yyyy-MM-dd\"T\"HH:mm:ss.SZ"));
        Assert.assertEquals("2023-01-01T12:30:45.100+0000", oneSub.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSZ"));
        Assert.assertEquals("2023-01-01T12:30:45.100000000+0000", oneSub.format(fmt9));

        // 3. 2 digits sub-second
        PureDate twoSub = PureDate.parsePureDate("2023-01-01T12:30:45.12Z");
        Assert.assertEquals("2023-01-01T12:30:45.1+0000", twoSub.format("yyyy-MM-dd\"T\"HH:mm:ss.SZ"));
        Assert.assertEquals("2023-01-01T12:30:45.12+0000", twoSub.format("yyyy-MM-dd\"T\"HH:mm:ss.SSZ"));
        Assert.assertEquals("2023-01-01T12:30:45.120+0000", twoSub.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSZ"));
        Assert.assertEquals("2023-01-01T12:30:45.120000000+0000", twoSub.format(fmt9));

        // 4. 3 digits (milliseconds)
        PureDate millis = PureDate.parsePureDate("2023-01-01T12:30:45.123Z");
        Assert.assertEquals("2023-01-01T12:30:45.12+0000", millis.format("yyyy-MM-dd\"T\"HH:mm:ss.SSZ"));
        Assert.assertEquals("2023-01-01T12:30:45.123+0000", millis.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSZ"));
        Assert.assertEquals("2023-01-01T12:30:45.123000+0000", millis.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSZ"));
        Assert.assertEquals("2023-01-01T12:30:45.123000000+0000", millis.format(fmt9));

        // 5. 6 digits (microseconds)
        PureDate micros = PureDate.parsePureDate("2023-01-01T12:30:45.123456Z");
        Assert.assertEquals("2023-01-01T12:30:45.123+0000", micros.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSZ"));
        Assert.assertEquals("2023-01-01T12:30:45.123456+0000", micros.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSZ"));
        Assert.assertEquals("2023-01-01T12:30:45.123456000+0000", micros.format(fmt9));

        // 6. 9 digits (nanoseconds) — exact match, no padding needed
        PureDate nanos = PureDate.parsePureDate("2023-01-01T12:30:45.123456789Z");
        Assert.assertEquals("2023-01-01T12:30:45.123+0000", nanos.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSZ"));
        Assert.assertEquals("2023-01-01T12:30:45.123456789+0000", nanos.format(fmt9));
    }

    @Test
    public void testFormatISO8601TimezoneOffsetVariants()
    {
        String fmt9 = "yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSSSSZ";

        // UTC designator "Z"
        PureDate utc = PureDate.parsePureDate("2023-01-01T00:00:00Z");
        Assert.assertEquals("2023-01-01T00:00:00.000000000+0000", utc.format(fmt9));

        // Explicit zero offset +0000
        PureDate zeroPos = PureDate.parsePureDate("2023-01-01T00:00:00+0000");
        Assert.assertEquals("2023-01-01T00:00:00.000000000+0000", zeroPos.format(fmt9));

        // Negative zero offset -0000
        PureDate zeroNeg = PureDate.parsePureDate("2023-01-01T00:00:00-0000");
        Assert.assertEquals("2023-01-01T00:00:00.000000000+0000", zeroNeg.format(fmt9));

        // Positive offset (India +0530)
        PureDate india = PureDate.parsePureDate("2023-01-01T05:30:00+0530");
        Assert.assertEquals("2023-01-01T00:00:00.000000000+0000", india.format(fmt9));

        // Negative offset (US Eastern -0500)
        PureDate eastern = PureDate.parsePureDate("2023-01-01T00:00:00-0500");
        Assert.assertEquals("2023-01-01T05:00:00.000000000+0000", eastern.format(fmt9));

        // Max positive offset (+1400, Line Islands)
        PureDate maxPos = PureDate.parsePureDate("2023-01-01T14:00:00+1400");
        Assert.assertEquals("2023-01-01T00:00:00.000000000+0000", maxPos.format(fmt9));

        // Max negative offset (-1200, Baker Island)
        PureDate maxNeg = PureDate.parsePureDate("2023-01-01T00:00:00-1200");
        Assert.assertEquals("2023-01-01T12:00:00.000000000+0000", maxNeg.format(fmt9));
    }

    @Test
    public void testParseISO8601ColonOffsetsNotSupported()
    {
        // PureDate parser requires RFC 822 style offsets (+0530), not colon-separated (+05:30)
        try
        {
            PureDate.parsePureDate("2023-01-01T00:00:00+05:30");
            Assert.fail("Expected exception for colon-separated offset");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

        try
        {
            PureDate.parsePureDate("2023-01-01T00:00:00-05:00");
            Assert.fail("Expected exception for colon-separated offset");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

        try
        {
            PureDate.parsePureDate("2023-01-01T00:00:00+00:00");
            Assert.fail("Expected exception for colon-separated offset");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    @Test
    public void testFormatISO8601SubSecondsWithTimezoneOffsets()
    {
        String fmt9 = "yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSSSSZ";

        // Millis + positive offset
        PureDate millisPos = PureDate.parsePureDate("2023-01-01T12:30:45.123+0530");
        Assert.assertEquals("2023-01-01T07:00:45.123000000+0000", millisPos.format(fmt9));

        // Nanos + negative offset
        PureDate nanosNeg = PureDate.parsePureDate("2023-01-01T12:30:45.123456789-0500");
        Assert.assertEquals("2023-01-01T17:30:45.123456789+0000", nanosNeg.format(fmt9));

        // Nanos + RFC 822 zero offset
        PureDate nanosZero = PureDate.parsePureDate("2023-01-01T12:30:45.123456789+0000");
        Assert.assertEquals("2023-01-01T12:30:45.123456789+0000", nanosZero.format(fmt9));
    }

    @Test
    public void testFormatISO8601BoundaryTimeValues()
    {
        String fmt9 = "yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSSSSZ";

        // Midnight with explicit subsecond zeros
        PureDate midnightSub = PureDate.parsePureDate("2023-07-04T00:00:00.000000000Z");
        Assert.assertEquals("2023-07-04T00:00:00.000000000+0000", midnightSub.format(fmt9));

        // Last second of day (no sub-seconds)
        PureDate lastSecond = PureDate.parsePureDate("2023-01-01T23:59:59Z");
        Assert.assertEquals("2023-01-01T23:59:59.000000000+0000", lastSecond.format(fmt9));

        // Last nanosecond of day
        PureDate lastNano = PureDate.parsePureDate("2023-01-01T23:59:59.999999999Z");
        Assert.assertEquals("2023-01-01T23:59:59.999999999+0000", lastNano.format(fmt9));

        // Last nanosecond of year
        PureDate lastNanoYear = PureDate.parsePureDate("2023-12-31T23:59:59.999999999Z");
        Assert.assertEquals("2023-12-31T23:59:59.999999999+0000", lastNanoYear.format(fmt9));
    }

    @Test
    public void testFormatISO8601BoundaryDateValues()
    {
        String fmt9 = "yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSSSSZ";

        // Leap year Feb 29
        PureDate leapDay = PureDate.parsePureDate("2000-02-29T12:00:00Z");
        Assert.assertEquals("2000-02-29T12:00:00.000000000+0000", leapDay.format(fmt9));

        // Unix epoch
        PureDate epoch = PureDate.parsePureDate("1970-01-01T00:00:00Z");
        Assert.assertEquals("1970-01-01T00:00:00.000000000+0000", epoch.format(fmt9));

        // Max common year
        PureDate maxYear = PureDate.parsePureDate("9999-12-31T23:59:59.999999999Z");
        Assert.assertEquals("9999-12-31T23:59:59.999999999+0000", maxYear.format(fmt9));

        // Negative year (1 BCE)
        PureDate negYear = PureDate.newPureDate(-1, 1, 1, 0, 0, 0);
        Assert.assertEquals("-1-01-01T00:00:00.000000000+0000", negYear.format(fmt9));

        // Year zero
        PureDate yearZero = PureDate.newPureDate(0, 1, 1, 0, 0, 0);
        Assert.assertEquals("0-01-01T00:00:00.000000000+0000", yearZero.format(fmt9));

        // Subsecond all zeros
        PureDate zeroSub = PureDate.parsePureDate("2023-01-01T12:00:00.000000Z");
        Assert.assertEquals("2023-01-01T12:00:00.000000000+0000", zeroSub.format(fmt9));
    }

    @Test
    public void testFormatISO8601NoTimezone()
    {
        String fmt9 = "yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSSSSZ";

        // No timezone, no sub-seconds
        PureDate noTzNoSub = PureDate.parsePureDate("2023-01-01T00:00:00");
        Assert.assertEquals("2023-01-01T00:00:00.000000000+0000", noTzNoSub.format(fmt9));

        // No timezone, with millis
        PureDate noTzMillis = PureDate.parsePureDate("2023-01-01T12:30:45.123");
        Assert.assertEquals("2023-01-01T12:30:45.123000000+0000", noTzMillis.format(fmt9));

        // No timezone, with nanos
        PureDate noTzNanos = PureDate.parsePureDate("2023-01-01T12:30:45.123456789");
        Assert.assertEquals("2023-01-01T12:30:45.123456789+0000", noTzNanos.format(fmt9));
    }

    @Test
    public void testFormatISO8601TimezoneOutputFormats()
    {
        // Z and z/X output formats with subsecond padding (Z+RFC822 offset already tested in testFormatISO8601TimezoneOffsetVariants,
        // EST shift already tested in testFormatWithTimeZoneShift)
        PureDate date = PureDate.parsePureDate("2023-01-01T00:00:00Z");

        // X format (ISO 8601) — outputs "Z" for UTC
        Assert.assertEquals("2023-01-01T00:00:00.000000000Z", date.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSSSSX"));

        // z format (general timezone name)
        Assert.assertEquals("2023-01-01T00:00:00.000000000 GMT", date.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSSSS z"));

        // EST shift with subsecond padding (no sub-seconds)
        Assert.assertEquals("2022-12-31T19:00:00.000000000-0500", date.format("[EST]yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSSSSZ"));
    }

    @Test
    public void testFormatISO8601PartialDateTimesThrowOnFullFormat()
    {
        String fmt9 = "yyyy-MM-dd\"T\"HH:mm:ss.SSSSSSSSSZ";

        // Year only
        PureDate yearOnly = PureDate.parsePureDate("2023");
        try
        {
            yearOnly.format(fmt9);
            Assert.fail("Expected exception for year-only date formatted with full datetime format");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Date has no month: 2023", e.getMessage());
        }

        // Year-month
        PureDate yearMonth = PureDate.parsePureDate("2023-01");
        try
        {
            yearMonth.format(fmt9);
            Assert.fail("Expected exception for year-month date formatted with full datetime format");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Date has no day: 2023-01", e.getMessage());
        }

        // Date only — hours, minutes, seconds padded with 0
        PureDate dateOnly = PureDate.parsePureDate("2023-01-01");
        Assert.assertEquals("2023-01-01T00:00:00.000000000+0000", dateOnly.format(fmt9));

        // Date + hour only — minutes and seconds padded with 0
        PureDate dateHour = PureDate.parsePureDate("2023-01-01T12");
        Assert.assertEquals("2023-01-01T12:00:00.000000000+0000", dateHour.format(fmt9));

        // Date + hour:minute (no seconds) — seconds padded with 0
        PureDate dateHourMin = PureDate.parsePureDate("2023-01-01T12:30");
        Assert.assertEquals("2023-01-01T12:30:00.000000000+0000", dateHourMin.format(fmt9));
    }

    @Test
    public void testFormatISO8601PartialDateTimesWithMatchingFormats()
    {
        // Only test partial formats not already covered by testFormat (which tests yyyy, yyyy-MM, yyyy-MM-dd, etc.)
        Assert.assertEquals("2023-01-01T12", PureDate.parsePureDate("2023-01-01T12").format("yyyy-MM-dd\"T\"HH"));
        Assert.assertEquals("2023-01-01T12:30", PureDate.parsePureDate("2023-01-01T12:30").format("yyyy-MM-dd\"T\"HH:mm"));
    }

    @Test
    public void testFormatPadsHoursMinutesSecondsWithZeroNoSubseconds()
    {
        String fmtSeconds = "yyyy-MM-dd\"T\"HH:mm:ss";

        // Date only — hours, minutes, seconds all padded with 0
        PureDate dateOnly = PureDate.parsePureDate("2023-06-15");
        Assert.assertEquals("2023-06-15T00:00:00", dateOnly.format(fmtSeconds));

        // Date + hour only — minutes and seconds padded with 0
        PureDate dateHour = PureDate.parsePureDate("2023-06-15T14");
        Assert.assertEquals("2023-06-15T14:00:00", dateHour.format(fmtSeconds));

        // Date + hour:minute — seconds padded with 0
        PureDate dateHourMin = PureDate.parsePureDate("2023-06-15T14:45");
        Assert.assertEquals("2023-06-15T14:45:00", dateHourMin.format(fmtSeconds));

        // Full datetime — no padding needed
        PureDate full = PureDate.parsePureDate("2023-06-15T14:45:30");
        Assert.assertEquals("2023-06-15T14:45:30", full.format(fmtSeconds));

        // Format with only HH:mm (no seconds in format) — hours and minutes padded
        String fmtMinutes = "yyyy-MM-dd\"T\"HH:mm";
        Assert.assertEquals("2023-06-15T00:00", dateOnly.format(fmtMinutes));
        Assert.assertEquals("2023-06-15T14:00", dateHour.format(fmtMinutes));

        // Format with only HH (no minutes/seconds in format) — hours padded
        String fmtHours = "yyyy-MM-dd\"T\"HH";
        Assert.assertEquals("2023-06-15T00", dateOnly.format(fmtHours));
    }



    @Test
    public void testInvalidSubseconds()
    {
        try
        {
            PureDate.newPureDate(2016, 5, 17, 10, 26, 33, null);
            Assert.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid subsecond value: null", e.getMessage());
        }

        try
        {
            PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "");
            Assert.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid subsecond value: \"\"", e.getMessage());
        }

        try
        {
            PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "789as9898");
            Assert.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid subsecond value: \"789as9898\"", e.getMessage());
        }

        try
        {
            PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "-789");
            Assert.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid subsecond value: \"-789\"", e.getMessage());
        }
    }

    @Test
    public void testAddYears()
    {
        Assert.assertEquals(PureDate.newPureDate(2017, 2, 28), PureDate.newPureDate(2016, 2, 29).addYears(1));
        Assert.assertEquals(PureDate.newPureDate(2020, 2, 29), PureDate.newPureDate(2016, 2, 29).addYears(4));
    }

    @Test
    public void testAddMonths()
    {
        Assert.assertEquals(PureDate.newPureDate(2017, 2, 28), PureDate.newPureDate(2017, 1, 31).addMonths(1));
        Assert.assertEquals(PureDate.newPureDate(2020, 2, 29), PureDate.newPureDate(2020, 1, 31).addMonths(1));
        Assert.assertEquals(PureDate.newPureDate(2017, 3, 31), PureDate.newPureDate(2017, 1, 31).addMonths(2));
        Assert.assertEquals(PureDate.newPureDate(2017, 4, 30), PureDate.newPureDate(2017, 1, 31).addMonths(3));

        Assert.assertEquals(PureDate.newPureDate(2010, 1, 29), PureDate.newPureDate(2012, 2, 29).addMonths(-25));
    }

    @Test
    public void testAddDays()
    {
        Assert.assertEquals(PureDate.newPureDate(2017, 3, 1), PureDate.newPureDate(2017, 2, 28).addDays(1));
        Assert.assertEquals(PureDate.newPureDate(2020, 2, 29), PureDate.newPureDate(2020, 2, 28).addDays(1));
        Assert.assertEquals(PureDate.newPureDate(2015, 3, 30), PureDate.newPureDate(2015, 4, 16).addDays(-17));
        Assert.assertEquals(PureDate.newPureDate(2015, 3, 30), PureDate.newPureDate(2014, 3, 30).addDays(365));
        Assert.assertEquals(PureDate.newPureDate(2013, 3, 30), PureDate.newPureDate(2014, 3, 30).addDays(-365));
    }

    @Test
    public void testAddMilliseconds()
    {
        PureDate date = PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780013429");

        Assert.assertSame(date, date.addMilliseconds(0));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 34, "779013429"), date.addMilliseconds(999));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 32, "781013429"), date.addMilliseconds(-999));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "783013429"), date.addMilliseconds(3));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "777013429"), date.addMilliseconds(-3));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 34, "780013429"), date.addMilliseconds(1000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 32, "780013429"), date.addMilliseconds(-1000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 43, "780013429"), date.addMilliseconds(10000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 23, "780013429"), date.addMilliseconds(-10000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 44, "603013429"), date.addMilliseconds(10823));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 22, "957013429"), date.addMilliseconds(-10823));
    }

    @Test
    public void testAddMicroseconds()
    {
        PureDate date = PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780013429");

        Assert.assertSame(date, date.addMicroseconds(0));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "781012429"), date.addMicroseconds(999));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "779014429"), date.addMicroseconds(-999));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780016429"), date.addMicroseconds(3));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780010429"), date.addMicroseconds(-3));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "781013429"), date.addMicroseconds(1000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "779013429"), date.addMicroseconds(-1000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "790013429"), date.addMicroseconds(10000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "770013429"), date.addMicroseconds(-10000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "790836429"), date.addMicroseconds(10823));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "769190429"), date.addMicroseconds(-10823));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 34, "780013429"), date.addMicroseconds(1_000_000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 32, "780013429"), date.addMicroseconds(-1_000_000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 43, "780013429"), date.addMicroseconds(10_000_000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 23, "780013429"), date.addMicroseconds(-10_000_000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 44, "603014429"), date.addMicroseconds(10_823_001));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 22, "957012429"), date.addMicroseconds(-10_823_001));
    }

    @Test
    public void testAddNanoseconds()
    {
        PureDate date = PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780013429");

        Assert.assertSame(date, date.addNanoseconds(0));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780014428"), date.addNanoseconds(999));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780012430"), date.addNanoseconds(-999));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780013432"), date.addNanoseconds(3));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780013426"), date.addNanoseconds(-3));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780014429"), date.addNanoseconds(1000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780012429"), date.addNanoseconds(-1000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780023429"), date.addNanoseconds(10000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780003429"), date.addNanoseconds(-10000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780024252"), date.addNanoseconds(10823));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780002606"), date.addNanoseconds(-10823));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 34, "780013429"), date.addNanoseconds(1_000_000_000));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 32, "780013429"), date.addNanoseconds(-1_000_000_000));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 43, "780013429"), date.addNanoseconds(10_000_000_000L));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 23, "780013429"), date.addNanoseconds(-10_000_000_000L));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 44, "603013430"), date.addNanoseconds(10_823_000_001L));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 22, "957013428"), date.addNanoseconds(-10_823_000_001L));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 18, 10, 26, 33, "780013430"), date.addNanoseconds(86_400_000_000_001L));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 16, 10, 26, 33, "780013428"), date.addNanoseconds(-86_400_000_000_001L));

        Assert.assertEquals(PureDate.newPureDate(2016, 6, 6, 10, 26, 36, "780013430"), date.addNanoseconds(1_728_003_000_000_001L));
        Assert.assertEquals(PureDate.newPureDate(2016, 4, 27, 10, 26, 30, "780013428"), date.addNanoseconds(-1_728_003_000_000_001L));

        Assert.assertEquals(PureDate.newPureDate(2021, 11, 7, 10, 26, 36, "780013430"), date.addNanoseconds(172_800_003_000_000_001L));
        Assert.assertEquals(PureDate.newPureDate(2010, 11, 25, 10, 26, 30, "780013428"), date.addNanoseconds(-172_800_003_000_000_001L));
    }

    @Test
    public void testAddSubseconds()
    {
        PureDate date = PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780013429");

        Assert.assertSame(date, date.addSubseconds("0"));
        Assert.assertSame(date, date.addSubseconds("00000000000000000000000000000000000"));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "880013429"), date.addSubseconds("1"));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "880013429"), date.addSubseconds("1000000"));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 34, "580013429"), date.addSubseconds("8"));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "790013429"), date.addSubseconds("01"));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "800013429"), date.addSubseconds("02"));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 34, "779013429"), date.addSubseconds("999"));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "783013429"), date.addSubseconds("003"));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 34, "582213500"), date.addSubseconds("802200071"));
    }

    @Test
    public void testSubtractSubseconds()
    {
        PureDate date = PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "780013429");

        Assert.assertSame(date, date.subtractSubseconds("0"));
        Assert.assertSame(date, date.subtractSubseconds("00000000000000000000000000000000000"));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "680013429"), date.subtractSubseconds("1"));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "680013429"), date.subtractSubseconds("1000000"));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 32, "980013429"), date.subtractSubseconds("8"));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "770013429"), date.subtractSubseconds("01"));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "760013429"), date.subtractSubseconds("02"));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 32, "781013429"), date.subtractSubseconds("999"));
        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 33, "777013429"), date.subtractSubseconds("003"));

        Assert.assertEquals(PureDate.newPureDate(2016, 5, 17, 10, 26, 32, "977813358"), date.subtractSubseconds("802200071"));
    }
}
