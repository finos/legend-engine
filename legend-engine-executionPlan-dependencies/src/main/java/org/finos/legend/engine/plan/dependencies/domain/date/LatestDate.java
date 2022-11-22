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

package org.finos.legend.engine.plan.dependencies.domain.date;

import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

import java.util.GregorianCalendar;

public class LatestDate implements AbstractPureDate
{
    public static final LatestDate INSTANCE = new LatestDate();
    private static final String latestDateConstant = "%latest";

    private LatestDate()
    {
    }

    public int getYear()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public boolean hasMonth()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public int getMonth()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public boolean hasDay()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public int getDay()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public boolean hasHour()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public int getHour()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public boolean hasMinute()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public int getMinute()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public boolean hasSecond()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public int getSecond()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public boolean hasSubsecond()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public String getSubsecond()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public String format(String formatString)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public void format(Appendable appendable, String formatString)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public PureDate addYears(int years)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public PureDate addMonths(int months)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public PureDate addWeeks(int weeks)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public PureDate addDays(int days)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public PureDate addHours(int hours)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public PureDate addMinutes(int minutes)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public PureDate addSeconds(int seconds)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public PureDate addMilliseconds(int milliseconds)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public PureDate addMicroseconds(long microseconds)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public PureDate addNanoseconds(long nanoseconds)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public PureDate addSubseconds(String subseconds)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public PureDate subtractSubseconds(String subseconds)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public GregorianCalendar getCalendar()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public void writeString(Appendable appendable)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public long dateDifference(PureDate otherDate, String unit)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public int compareTo(PureDate pureDate)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    public String toString()
    {
        return latestDateConstant;
    }

    public LatestDate clone()
    {
        return this;
    }

    public static boolean isLatestDate(PureDate date)
    {
        return date == INSTANCE;
    }

    public static boolean isLatestDateString(String string)
    {
        return latestDateConstant.equals(string);
    }
}
