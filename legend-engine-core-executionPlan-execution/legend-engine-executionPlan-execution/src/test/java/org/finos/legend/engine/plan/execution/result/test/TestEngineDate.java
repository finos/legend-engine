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
import org.finos.legend.engine.plan.execution.result.date.EngineDate;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

public class TestEngineDate
{
    @Test
    public void testFromDateString()
    {
        assertLegendDate("2020-07-14", PureDate.newPureDate(2020, 7, 14), EngineDate.fromDateString("2020-07-14"));
        assertLegendDate("2015-02-28", PureDate.newPureDate(2015, 2, 28), EngineDate.fromDateString("2015-02-28"));
        assertLegendDate("2016-02-29", PureDate.newPureDate(2016, 2, 29), EngineDate.fromDateString("2016-02-29"));

        assertDoesNotParse("abcd", "Date", EngineDate::fromDateString);
        assertDoesNotParse("2020-1234123-1231234", "Date", EngineDate::fromDateString);
        assertDoesNotParse("2020-12-12T14:15:16_otherstuff", "Date", EngineDate::fromDateString);
    }

    @Test
    public void testFromStrictDateString()
    {
        assertLegendDate("2020-07-14", PureDate.newPureDate(2020, 7, 14), EngineDate.fromStrictDateString("2020-07-14"));
        assertLegendDate("2015-02-28", PureDate.newPureDate(2015, 2, 28), EngineDate.fromStrictDateString("2015-02-28"));
        assertLegendDate("2016-02-29", PureDate.newPureDate(2016, 2, 29), EngineDate.fromStrictDateString("2016-02-29"));

        assertDoesNotParse("abcd", "StrictDate", EngineDate::fromStrictDateString);
        assertDoesNotParse("2010-01-31T13:14:15.123", "StrictDate", EngineDate::fromStrictDateString);
    }

    @Test
    public void testFromDateTimeString()
    {
        assertLegendDate("2020-07-14 12:04:01", PureDate.newPureDate(2020, 7, 14, 12, 4, 1), EngineDate.fromDateTimeString("2020-07-14 12:04:01"));
        assertLegendDate("2020-07-14 12:04:01", PureDate.newPureDate(2020, 7, 14, 12, 4, 1), EngineDate.fromDateTimeString("2020-07-14T12:04:01"));
        assertLegendDate("2020-07-14 12:04:01.123", PureDate.newPureDate(2020, 7, 14, 12, 4, 1, "123000000"), EngineDate
                .fromDateTimeString("2020-07-14 12:04:01.123"));
        assertLegendDate("2020-07-14 12:04:01.123", PureDate.newPureDate(2020, 7, 14, 12, 4, 1, "123000000"), EngineDate
                .fromDateTimeString("2020-07-14T12:04:01.123"));

        assertDoesNotParse("abcd", "DateTime", EngineDate::fromDateTimeString);
        assertDoesNotParse("2020-07-14", "DateTime", EngineDate::fromDateTimeString);
        assertDoesNotParse("2020-12-12T14:15:16_otherstuff", "DateTime", EngineDate::fromDateTimeString);
    }

    @Test
    public void testFromZonedDateTimeString()
    {
        assertLegendDate("2020-07-14 15:04:01", PureDate.newPureDate(2020, 7, 14, 15, 4, 1), EngineDate.fromDateTimeString("2020-07-14T12:04:01-0300"));
        assertLegendDate("2020-07-14 09:04:01.123", PureDate.newPureDate(2020, 7, 14, 9, 4, 1, "123000000"), EngineDate
                .fromDateTimeString("2020-07-14T12:04:01.123+0300"));
        assertDoesNotParse("2020-12-12T14:15:16_otherstuff", "DateTime", EngineDate::fromDateTimeString);
        assertDoesNotParse("2020-12-12T14:15:16*0300", "DateTime", EngineDate::fromDateTimeString);
        assertDoesNotParse("2020-12-12T14:15:16+-0300", "DateTime", EngineDate::fromDateTimeString);
    }

    @Test
    public void testFromLocalDate()
    {
        assertLegendDate("2020-07-14", PureDate.newPureDate(2020, 7, 14), EngineDate.fromLocalDate(LocalDate.of(2020, 7, 14)));
        assertLegendDate("2015-02-28", PureDate.newPureDate(2015, 2, 28), EngineDate.fromLocalDate(LocalDate.of(2015, 2, 28)));
        assertLegendDate("2016-02-29", PureDate.newPureDate(2016, 2, 29), EngineDate.fromLocalDate(LocalDate.of(2016, 2, 29)));
    }

    @Test
    public void testFromLocalDateTime()
    {
        assertLegendDate("2020-07-14 12:04:01.000", PureDate.newPureDate(2020, 7, 14, 12, 4, 1, "000000000"), EngineDate
                .fromLocalDateTime(LocalDateTime.of(2020, 7, 14, 12, 4, 1)));
        assertLegendDate("2020-07-14 12:04:01", PureDate.newPureDate(2020, 7, 14, 12, 4, 1), EngineDate.fromLocalDateTime(LocalDateTime.of(2020, 7, 14, 12, 4, 1), true));
        assertLegendDate("2020-07-14 12:04:01.123", PureDate.newPureDate(2020, 7, 14, 12, 4, 1, "123000000"), EngineDate
                .fromLocalDateTime(LocalDateTime.of(2020, 7, 14, 12, 4, 1, 123_000_000)));
        assertLegendDate("2020-07-14 12:04:01", PureDate.newPureDate(2020, 7, 14, 12, 4, 1), EngineDate.fromLocalDateTime(LocalDateTime.of(2020, 7, 14, 12, 4, 1, 123_000_000), true));
    }

    @Test
    public void testFromZonedDateTime()
    {
        assertLegendDate("2020-07-14 12:04:01.000", PureDate.newPureDate(2020, 7, 14, 12, 4, 1, "000000000"), EngineDate
                .fromZonedDateTime(ZonedDateTime.of(2020, 7, 14, 12, 4, 1, 0, ZoneOffset.UTC)));
        assertLegendDate("2020-07-14 12:04:01", PureDate.newPureDate(2020, 7, 14, 12, 4, 1), EngineDate.fromZonedDateTime(ZonedDateTime.of(2020, 7, 14, 12, 4, 1, 0, ZoneOffset.UTC), true));
        assertLegendDate("2020-07-14 12:04:01.123", PureDate.newPureDate(2020, 7, 14, 12, 4, 1, "123000000"), EngineDate
                .fromZonedDateTime(ZonedDateTime.of(2020, 7, 14, 12, 4, 1, 123_000_000, ZoneOffset.UTC)));
        assertLegendDate("2020-07-14 12:04:01", PureDate.newPureDate(2020, 7, 14, 12, 4, 1), EngineDate.fromZonedDateTime(ZonedDateTime.of(2020, 7, 14, 12, 4, 1, 123_000_000, ZoneOffset.UTC), true));
        assertLegendDate("2020-07-14 07:04:01.123", PureDate.newPureDate(2020, 7, 14, 7, 4, 1, "123000000"), EngineDate.fromZonedDateTime(ZonedDateTime.of(2020, 7, 14, 12, 4, 1, 123_000_000, ZoneOffset.ofHours(5))));
        assertLegendDate("2020-07-14 07:04:01", PureDate.newPureDate(2020, 7, 14, 7, 4, 1), EngineDate.fromZonedDateTime(ZonedDateTime.of(2020, 7, 14, 12, 4, 1, 123_000_000, ZoneOffset.ofHours(5)), true));
    }

    @Test
    public void testFromInstant()
    {
        assertLegendDate("2020-07-14 12:04:01.000", PureDate.newPureDate(2020, 7, 14, 12, 4, 1, "000000000"), EngineDate
                .fromInstant(ZonedDateTime.of(2020, 7, 14, 12, 4, 1, 0, ZoneOffset.UTC).toInstant()));
        assertLegendDate("2020-07-14 12:04:01", PureDate.newPureDate(2020, 7, 14, 12, 4, 1), EngineDate.fromInstant(ZonedDateTime.of(2020, 7, 14, 12, 4, 1, 0, ZoneOffset.UTC).toInstant(), true));
        assertLegendDate("2020-07-14 12:04:01.123", PureDate.newPureDate(2020, 7, 14, 12, 4, 1, "123000000"), EngineDate
                .fromInstant(ZonedDateTime.of(2020, 7, 14, 12, 4, 1, 123_000_000, ZoneOffset.UTC).toInstant()));
        assertLegendDate("2020-07-14 12:04:01", PureDate.newPureDate(2020, 7, 14, 12, 4, 1), EngineDate.fromInstant(ZonedDateTime.of(2020, 7, 14, 12, 4, 1, 123_000_000, ZoneOffset.UTC).toInstant(), true));
    }

    private void assertDoesNotParse(String string, String type, Function<String, EngineDate> parser)
    {
        try
        {
            parser.apply(string);
            Assert.fail("Expected exception parsing: " + string);
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals(string, "Invalid " + type + " string: " + string, e.getMessage());
        }
        catch (DateTimeParseException ignored)
        {
            // this is ok
        }
    }

    private void assertLegendDate(String expectedSQL, PureDate expectedPureDate, EngineDate EngineDate)
    {
        Assert.assertEquals(expectedSQL, EngineDate.formatToSql());
        Assert.assertEquals(expectedPureDate, EngineDate.transformToPureDate());
    }
}
