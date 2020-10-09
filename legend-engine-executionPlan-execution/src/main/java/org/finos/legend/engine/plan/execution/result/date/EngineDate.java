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

package org.finos.legend.engine.plan.execution.result.date;

import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class EngineDate
{
    private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d+-\\d+-\\d+)([Tt ](\\d+:\\d+:\\d+(\\.\\d+)?))?$");
    private static final int DATE_GROUP = 1;
    private static final int TIME_GROUP = 3;
    private static final int SUBSECOND_GROUP = 4;

    public abstract String formatToSql();

    public abstract PureDate transformToPureDate();

    public static EngineDate fromDateString(String dateString)
    {
        Matcher matcher = DATE_PATTERN.matcher(dateString);
        if (!matcher.matches())
        {
            throw new IllegalArgumentException("Invalid Date string: " + dateString);
        }
        return (matcher.group(TIME_GROUP) == null) ? fromStrictDateString(dateString) : fromDateTimeMatcher(matcher);
    }

    public static EngineDate fromStrictDateString(String strictDateString)
    {
        LocalDate localDate = LocalDate.parse(strictDateString, DateTimeFormatter.ISO_LOCAL_DATE);
        return fromLocalDate(localDate);
    }

    public static EngineDate fromDateTimeString(String dateTimeString)
    {
        Matcher matcher = DATE_PATTERN.matcher(dateTimeString);
        if (!matcher.matches() || (matcher.group(TIME_GROUP) == null))
        {
            throw new IllegalArgumentException("Invalid DateTime string: " + dateTimeString);
        }
        return fromDateTimeMatcher(matcher);
    }

    private static EngineDate fromDateTimeMatcher(Matcher matcher)
    {
        LocalDate localDate = LocalDate.parse(matcher.group(DATE_GROUP), DateTimeFormatter.ISO_LOCAL_DATE);
        LocalTime localTime = LocalTime.parse(matcher.group(TIME_GROUP), DateTimeFormatter.ISO_LOCAL_TIME);
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        return fromLocalDateTime(localDateTime, matcher.group(SUBSECOND_GROUP) == null);
    }

    public static EngineDate fromLocalDate(LocalDate date)
    {
        return new EngineLocalDate(date);
    }

    public static EngineDate fromLocalDateTime(LocalDateTime dateTime)
    {
        return fromLocalDateTime(dateTime, false);
    }

    public static EngineDate fromLocalDateTime(LocalDateTime dateTime, boolean ignoreNanos)
    {
        return new EngineLocalDateTime(dateTime, !ignoreNanos);
    }

    public static EngineDate fromZonedDateTime(ZonedDateTime zonedDateTime)
    {
        return fromZonedDateTime(zonedDateTime, false);
    }

    public static EngineDate fromZonedDateTime(ZonedDateTime zonedDateTime, boolean ignoreNanos)
    {
        return fromLocalDateTime(zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(), ignoreNanos);
    }

    public static EngineDate fromInstant(Instant instant)
    {
        return fromInstant(instant, false);
    }

    public static EngineDate fromInstant(Instant instant, boolean ignoreNanos)
    {
        return fromLocalDateTime(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), ignoreNanos);
    }

    private static class EngineLocalDate extends EngineDate
    {
        private final LocalDate date;

        private EngineLocalDate(LocalDate date)
        {
            this.date = date;
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) || ((other instanceof EngineLocalDate) && this.date.equals(((EngineLocalDate) other).date));
        }

        @Override
        public int hashCode()
        {
            return this.date.hashCode();
        }

        @Override
        public String toString()
        {
            return this.date.toString();
        }

        @Override
        public String formatToSql()
        {
            return this.date.toString();
        }

        @Override
        public PureDate transformToPureDate()
        {
            return PureDate.newPureDate(this.date.getYear(), this.date.getMonthValue(), this.date.getDayOfMonth());
        }
    }

    private static class EngineLocalDateTime extends EngineDate
    {
        private static final DateTimeFormatter NO_SUBSECONDS_SQL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private static final DateTimeFormatter SUBSECONDS_SQL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"); // TODO should this be SSSSSSSSS?

        private final LocalDateTime localDateTime;
        private final boolean hasSubseconds;

        private EngineLocalDateTime(LocalDateTime localDateTime, boolean hasSubseconds)
        {
            this.localDateTime = localDateTime;
            this.hasSubseconds = hasSubseconds;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof EngineLocalDateTime))
            {
                return false;
            }

            EngineLocalDateTime that = (EngineLocalDateTime) other;
            return (this.hasSubseconds == that.hasSubseconds) && this.localDateTime.equals(that.localDateTime);
        }

        @Override
        public int hashCode()
        {
            return this.localDateTime.hashCode() ^ Boolean.hashCode(this.hasSubseconds);
        }

        @Override
        public String toString()
        {
            return this.localDateTime.toString();
        }

        @Override
        public String formatToSql()
        {
            return (this.hasSubseconds ? SUBSECONDS_SQL_FORMATTER : NO_SUBSECONDS_SQL_FORMATTER).format(this.localDateTime);
        }

        @Override
        public PureDate transformToPureDate()
        {
            return this.hasSubseconds ?
                    PureDate.newPureDate(this.localDateTime.getYear(), this.localDateTime.getMonthValue(), this.localDateTime.getDayOfMonth(), this.localDateTime.getHour(), this.localDateTime.getMinute(), this.localDateTime.getSecond(), nanosToSubseconds(this.localDateTime.getNano())) :
                    PureDate.newPureDate(this.localDateTime.getYear(), this.localDateTime.getMonthValue(), this.localDateTime.getDayOfMonth(), this.localDateTime.getHour(), this.localDateTime.getMinute(), this.localDateTime.getSecond());
        }

        private static String nanosToSubseconds(int nanos)
        {
            if (nanos == 0)
            {
                return "000000000";
            }
            StringBuilder builder = new StringBuilder(9);
            for (int i = 100_000_000; i > nanos; i /= 10)
            {
                builder.append('0');
            }
            return builder.append(nanos).toString();
        }
    }
}