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

package org.finos.legend.engine.plan.execution.stores.relational.result;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Tests for how {@link ResultColumn} reads a date or timestamp column, against a database rather
 * than against a hand built {@link java.sql.Date} or {@link java.sql.Timestamp}, since what a
 * driver puts in one of those is the whole of the question.
 */
public class TestResultColumn
{
    /**
     * A date column carries a day and no zone, and has to be read as that day whatever zone the JVM
     * runs in.
     *
     * <p>Reading it as a {@link java.sql.Date} did not do that. The driver worked the day into an
     * instant in the JVM default zone, and the day was read back out of that instant in GMT, so a
     * JVM running east of GMT lost a day on every date. Asking for the day itself carries no
     * instant and so no zone to disagree over.
     */
    @Test
    public void testDateColumnIsIndependentOfTheDefaultTimeZone() throws SQLException
    {
        List<String> days = Arrays.asList("1753-12-31", "1900-01-01", "2014-03-10");
        TimeZone defaultTimeZone = TimeZone.getDefault();
        try
        {
            for (String zoneId : Arrays.asList("GMT", "America/New_York", "Asia/Tokyo", "Pacific/Kiritimati"))
            {
                TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
                Assert.assertEquals("default " + zoneId, days, readDays(days));
            }
        }
        finally
        {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    /**
     * A timestamp column carries a wall clock the database keeps in the zone the connection names,
     * and a Pure date is that moment in UTC, so reading one shifts it out of that zone. The shift
     * is done here rather than by handing the driver a calendar, which not every driver honours.
     */
    @Test
    public void testTimestampColumnIsShiftedOutOfTheConnectionZone() throws SQLException
    {
        List<String> stored = Arrays.asList("1753-12-31 00:00:00", "2014-03-10 13:07:44");
        TimeZone defaultTimeZone = TimeZone.getDefault();
        try
        {
            for (String defaultZoneId : Arrays.asList("GMT", "Asia/Tokyo"))
            {
                TimeZone.setDefault(TimeZone.getTimeZone(defaultZoneId));
                for (String connectionZoneId : Arrays.asList("GMT", "America/New_York", "Asia/Tokyo"))
                {
                    List<String> want = new ArrayList<>(stored.size());
                    for (String moment : stored)
                    {
                        want.add(inUtc(moment, connectionZoneId));
                    }
                    Assert.assertEquals("default " + defaultZoneId + ", connection " + connectionZoneId,
                            want, readMoments(stored, connectionZoneId));
                }
            }
        }
        finally
        {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    private static String inUtc(String moment, String connectionZoneId)
    {
        LocalDateTime utc = LocalDateTime.parse(moment.replace(' ', 'T'))
                .atZone(ZoneId.of(connectionZoneId)).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return String.format("%04d-%02d-%02dT%02d:%02d:%02d.%09d", utc.getYear(), utc.getMonthValue(),
                utc.getDayOfMonth(), utc.getHour(), utc.getMinute(), utc.getSecond(), utc.getNano());
    }

    private static List<String> readDays(List<String> days) throws SQLException
    {
        return read(days, "recorded_on DATE", "DATE", "DATE", Types.DATE, "GMT");
    }

    private static List<String> readMoments(List<String> stored, String connectionZoneId) throws SQLException
    {
        return read(stored, "recorded_at TIMESTAMP", "TIMESTAMP", "TIMESTAMP", Types.TIMESTAMP, connectionZoneId);
    }

    /**
     * Connect straight through the driver. Another test in this module can leave DriverManager
     * without DuckDB registered, and this does not depend on it.
     */
    private static Connection connect() throws SQLException
    {
        return new org.duckdb.DuckDBDriver().connect("jdbc:duckdb:", new Properties());
    }

    private static List<String> read(List<String> values, String columnDefinition, String literalType,
                                     String dataType, int columnType, String calendarZoneId) throws SQLException
    {
        String column = columnDefinition.split(" ")[0];
        try (Connection connection = connect();
             Statement statement = connection.createStatement())
        {
            statement.execute("CREATE TABLE t (" + columnDefinition + ")");
            for (String value : values)
            {
                statement.execute("INSERT INTO t VALUES (" + literalType + " '" + value + "')");
            }

            ResultColumn resultColumn = new ResultColumn(1, column, dataType, columnType);
            Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone(calendarZoneId));
            List<String> read = new ArrayList<>(values.size());
            try (ResultSet resultSet = statement.executeQuery("SELECT " + column + " FROM t ORDER BY " + column))
            {
                while (resultSet.next())
                {
                    read.add(String.valueOf(resultColumn.getTransformedValue(resultSet, calendar)));
                }
            }
            return read;
        }
    }
}
