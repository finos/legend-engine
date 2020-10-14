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

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.StringIterate;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class PureDate implements org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate
{
    private static final char DATE_PREFIX = '%';
    private static final char DATE_SEPARATOR = '-';
    private static final char TIME_SEPARATOR = ':';
    private static final char DATE_TIME_SEPARATOR = 'T';

    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    private int year = 0;
    private int month = -1;
    private int day = -1;
    private int hour = -1;
    private int minute = -1;
    private int second = -1;
    private String subsecond = null;

    protected PureDate()
    {
    }

    public int getYear()
    {
        return this.year;
    }

    public boolean hasMonth()
    {
        return this.month != -1;
    }

    public int getMonth()
    {
        return this.month;
    }

    public boolean hasDay()
    {
        return this.day != -1;
    }

    public int getDay()
    {
        return this.day;
    }

    public boolean hasHour()
    {
        return this.hour != -1;
    }

    public int getHour()
    {
        return this.hour;
    }

    public boolean hasMinute()
    {
        return this.minute != -1;
    }

    public int getMinute()
    {
        return this.minute;
    }

    public boolean hasSecond()
    {
        return this.second != -1;
    }

    public int getSecond()
    {
        return this.second;
    }

    public boolean hasSubsecond()
    {
        return this.subsecond != null;
    }

    public String getSubsecond()
    {
        return this.subsecond;
    }

    public String format(String formatString)
    {
        StringBuilder builder = new StringBuilder(32);
        format(builder, formatString);
        return builder.toString();
    }

    public void format(Appendable appendable, String formatString)
    {
        try
        {
            int length = formatString.length();
            GregorianCalendar calendar = null;
            int i = 0;
            while (i < length)
            {
                char character = formatString.charAt(i++);
                switch (character)
                {
                    // Timezone conversion
                    case '[':
                    {
                        StringBuilder timeZoneId = new StringBuilder();
                        boolean done = false;
                        boolean escaped = false;
                        boolean inQuotes = false;
                        while (!done && (i < length))
                        {
                            char next = formatString.charAt(i++);
                            if (escaped)
                            {
                                timeZoneId.append(next);
                                escaped = false;
                            }
                            else if (inQuotes)
                            {
                                timeZoneId.append(next);
                            }
                            else if (next == ']')
                            {
                                done = true;
                            }
                            else if (next == '"')
                            {
                                inQuotes = !inQuotes;
                            }
                            else if (next == '\\')
                            {
                                escaped = true;
                            }
                            else
                            {
                                timeZoneId.append(next);
                            }
                        }
                        if (!done)
                        {
                            throw new IllegalArgumentException("Missing closing bracket in format string: " + formatString);
                        }
                        TimeZone timeZone;
                        try
                        {
                            timeZone = TimeZone.getTimeZone(timeZoneId.toString());
                        }
                        catch (RuntimeException e)
                        {
                            throw new IllegalArgumentException("Unknown time zone: " + timeZoneId.toString());
                        }

                        if (hasHour())
                        {
                            if (calendar == null)
                            {
                                calendar = getCalendar();
                                calendar.setTimeZone(timeZone);
                                calendar.add(Calendar.MILLISECOND, timeZone.getOffset(calendar.getTimeInMillis()));
                            }
                            else if (!timeZone.equals(calendar.getTimeZone()))
                            {
                                throw new IllegalArgumentException("Cannot set multiple timezones: " + calendar.getTimeZone().getID() + ", " + timeZone.getID());
                            }
                        }
                        break;
                    }
                    // Year
                    case 'y':
                    {
                        int displayYear = (calendar == null) ? this.year : calendar.get(Calendar.YEAR);
                        int count = getCharCountFrom(character, formatString, i);
                        if (count < 3)
                        {
                            appendTwoDigitInt(appendable, displayYear % 100);
                        }
                        else
                        {
                            appendable.append(Integer.toString(displayYear));
                        }
                        i += count;
                        break;
                    }
                    // Month
                    case 'M':
                    {
                        if (!hasMonth())
                        {
                            throw new IllegalArgumentException("Date has no month: " + this);
                        }
                        int displayMonth = (calendar == null) ? this.month : (calendar.get(Calendar.MONTH) + 1);
                        int count = getCharCountFrom(character, formatString, i);
                        appendZeroPaddedInt(appendable, displayMonth, count + 1);
                        i += count;
                        break;
                    }
                    // Day
                    case 'd':
                    {
                        if (!hasDay())
                        {
                            throw new IllegalArgumentException("Date has no day: " + this);
                        }
                        int displayDay = (calendar == null) ? this.day : calendar.get(Calendar.DAY_OF_MONTH);
                        int count = getCharCountFrom(character, formatString, i);
                        appendZeroPaddedInt(appendable, displayDay, count + 1);
                        i += count;
                        break;
                    }
                    // Hour (1-12)
                    case 'h':
                    {
                        if (!hasHour())
                        {
                            throw new IllegalArgumentException("Date has no hour: " + this);
                        }
                        int preDisplayHour = (calendar == null) ? this.hour : calendar.get(Calendar.HOUR_OF_DAY);
                        int displayHour = (preDisplayHour == 0) ? 12 : ((preDisplayHour > 12) ? (preDisplayHour - 12) : preDisplayHour);
                        int count = getCharCountFrom(character, formatString, i);
                        appendZeroPaddedInt(appendable, displayHour, count + 1);
                        i += count;
                        break;
                    }
                    // Hour (0-23)
                    case 'H':
                    {
                        if (!hasHour())
                        {
                            throw new IllegalArgumentException("Date has no hour: " + this);
                        }
                        int displayHour = (calendar == null) ? this.hour : calendar.get(Calendar.HOUR_OF_DAY);
                        int count = getCharCountFrom(character, formatString, i);
                        appendZeroPaddedInt(appendable, displayHour, count + 1);
                        i += count;
                        break;
                    }
                    // AM/PM
                    case 'a':
                    {
                        if (!hasHour())
                        {
                            throw new IllegalArgumentException("Date has no hour: " + this);
                        }
                        int displayHour = (calendar == null) ? this.hour : calendar.get(Calendar.HOUR_OF_DAY);
                        appendable.append((displayHour < 12) ? "AM" : "PM");
                        break;
                    }
                    // Minute
                    case 'm':
                    {
                        if (!hasMinute())
                        {
                            throw new IllegalArgumentException("Date has no minute: " + this);
                        }
                        int displayMinute = (calendar == null) ? this.minute : calendar.get(Calendar.MINUTE);
                        int count = getCharCountFrom(character, formatString, i);
                        appendZeroPaddedInt(appendable, displayMinute, count + 1);
                        i += count;
                        break;
                    }
                    // Second
                    case 's':
                    {
                        if (!hasSecond())
                        {
                            throw new IllegalArgumentException("Date has no second: " + this);
                        }
                        int count = getCharCountFrom(character, formatString, i);
                        appendZeroPaddedInt(appendable, this.second, count + 1);
                        i += count;
                        break;
                    }
                    // Subsecond
                    case 'S':
                    {
                        if (!hasSubsecond())
                        {
                            throw new IllegalArgumentException("Date has no sub-second: " + this);
                        }
                        int count = getCharCountFrom(character, formatString, i);
                        if (count < 3)
                        {
                            int maxLen = count + 1;
                            int len = this.subsecond.length();
                            if (len <= maxLen)
                            {
                                appendable.append(this.subsecond);
                            }
                            else
                            {
                                int j = 0;
                                while (j < maxLen)
                                {
                                    appendable.append(this.subsecond.charAt(j++));
                                }
                            }
                        }
                        else
                        {
                            appendable.append(this.subsecond);
                        }
                        i += count;
                        break;
                    }
                    // General time zone
                    case 'z':
                    {
                        int count = getCharCountFrom(character, formatString, i);
                        // TODO
                        if (calendar == null)
                        {
                            appendable.append("GMT");
                        }
                        else
                        {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("z");
                            dateFormat.setTimeZone(calendar.getTimeZone());
                            appendable.append(dateFormat.format(calendar.getTime()));
                        }
                        i += count;
                        break;
                    }
                    // RFC 822 time zone
                    case 'Z':
                    {
                        int count = getCharCountFrom(character, formatString, i);
                        if (calendar == null)
                        {
                            appendable.append("+0000");
                        }
                        else
                        {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("Z");
                            dateFormat.setTimeZone(calendar.getTimeZone());
                            appendable.append(dateFormat.format(calendar.getTime()));
                        }
                        i += count;
                        break;
                    }
                    // ISO 8601 time zone
                    case 'X':
                    {
                        int count = getCharCountFrom(character, formatString, i);
                        if (calendar == null)
                        {
                            appendable.append("Z");
                        }
                        else
                        {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("X");
                            dateFormat.setTimeZone(calendar.getTimeZone());
                            appendable.append(dateFormat.format(calendar.getTime()));
                        }
                        i += count;
                        break;
                    }
                    // Separator
                    case '-':
                    case '/':
                    case ':':
                    case '.':
                    case ' ':
                    case '\t':
                    {
                        appendable.append(character);
                        break;
                    }
                    // Quote
                    case '"':
                    {
                        boolean done = false;
                        boolean escaped = false;
                        while (!done && (i < length))
                        {
                            char next = formatString.charAt(i++);
                            if (escaped)
                            {
                                appendable.append(next);
                                escaped = false;
                            }
                            else if (next == '"')
                            {
                                done = true;
                            }
                            else if (next == '\\')
                            {
                                escaped = true;
                            }
                            else
                            {
                                appendable.append(next);
                            }
                        }
                        if (!done)
                        {
                            throw new IllegalArgumentException("Missing closing quote in format string: " + formatString);
                        }
                        break;
                    }
                    default:
                    {
                        throw new IllegalArgumentException("Invalid format control character '" + character + "' in format string: " + formatString);
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof PureDate))
        {
            return false;
        }

        PureDate otherDate = (PureDate) other;
        return (this.year == otherDate.year) &&
                (this.month == otherDate.month) &&
                (this.day == otherDate.day) &&
                (this.hour == otherDate.hour) &&
                (this.minute == otherDate.minute) &&
                (this.second == otherDate.second) &&
                Comparators.nullSafeEquals(this.subsecond, otherDate.subsecond);
    }

    @Override
    public int hashCode()
    {
        int hash = this.year;
        if (this.month != -1)
        {
            hash ^= this.month;
            if (this.day != -1)
            {
                hash ^= this.day;
                if (this.hour != -1)
                {
                    hash ^= this.hour;
                    if (this.minute != -1)
                    {
                        hash ^= this.minute;
                        if (this.second != -1)
                        {
                            hash ^= this.second;
                            if (this.subsecond != null)
                            {
                                hash ^= this.subsecond.hashCode();
                            }
                        }
                    }
                }
            }
        }
        return hash;
    }

    public int compareTo(org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate other)
    {
        if (this == other)
        {
            return 0;
        }

        // Compare year
        if (this.year < other.getYear())
        {
            return -1;
        }
        if (this.year > other.getYear())
        {
            return 1;
        }

        // Compare month
        if (this.month == -1)
        {
            return (other.getMonth() == -1) ? 0 : -1;
        }
        if (other.getMonth() == -1)
        {
            return 1;
        }
        if (this.month < other.getMonth())
        {
            return -1;
        }
        if (this.month > other.getMonth())
        {
            return 1;
        }

        // Compare day
        if (this.day == -1)
        {
            return (other.getDay() == -1) ? 0 : -1;
        }
        if (other.getDay() == -1)
        {
            return 1;
        }
        if (this.day < other.getDay())
        {
            return -1;
        }
        if (this.day > other.getDay())
        {
            return 1;
        }

        // Compare hour
        if (this.hour == -1)
        {
            return (other.getHour() == -1) ? 0 : -1;
        }
        if (other.getHour() == -1)
        {
            return 1;
        }
        if (this.hour < other.getHour())
        {
            return -1;
        }
        if (this.hour > other.getHour())
        {
            return 1;
        }

        // Compare minute
        if (this.minute == -1)
        {
            return (other.getMinute() == -1) ? 0 : -1;
        }
        if (other.getMinute() == -1)
        {
            return 1;
        }
        if (this.minute < other.getMinute())
        {
            return -1;
        }
        if (this.minute > other.getMinute())
        {
            return 1;
        }

        // Compare second
        if (this.second == -1)
        {
            return (other.getSecond() == -1) ? 0 : -1;
        }
        if (other.getSecond() == -1)
        {
            return 1;
        }
        if (this.second < other.getSecond())
        {
            return -1;
        }
        if (this.second > other.getSecond())
        {
            return 1;
        }

        // Compare subsecond
        if (this.subsecond == null)
        {
            return (other.getSubsecond() == null) ? 0 : -1;
        }
        if (other.getSubsecond() == null)
        {
            return 1;
        }
        int thisLength = this.subsecond.length();
        int otherLength = other.getSubsecond().length();
        int minLength = Math.min(thisLength, otherLength);
        for (int i = 0; i < minLength; i++)
        {
            char thisChar = this.subsecond.charAt(i);
            char otherChar = other.getSubsecond().charAt(i);
            if (thisChar < otherChar)
            {
                return -1;
            }
            if (thisChar > otherChar)
            {
                return 1;
            }
        }
        return (thisLength == otherLength) ? 0 : ((thisLength < otherLength) ? -1 : 1);
    }

    @Override
    public PureDate clone()
    {
        PureDate copy = new PureDate();
        return copyValues(copy);
    }

    PureDate copyValues(PureDate copy)
    {
        copy.year = this.year;
        copy.month = this.month;
        copy.day = this.day;
        copy.hour = this.hour;
        copy.minute = this.minute;
        copy.second = this.second;
        copy.subsecond = this.subsecond;
        return copy;
    }

    public PureDate addYears(int years)
    {
        if (years == 0)
        {
            return this;
        }
        PureDate copy = clone();
        copy.incrementYear(years);
        if (!copy.isLeapYear() && (copy.month == 2) && (copy.day == 29))
        {
            copy.day = 28;
        }
        return copy;
    }

    public PureDate addMonths(int months)
    {
        if (!hasMonth())
        {
            throw new UnsupportedOperationException("Cannot add months to a date that does not have a month: " + this);
        }
        if (months == 0)
        {
            return this;
        }
        PureDate copy = clone();
        copy.incrementMonth(months);
        if (copy.hasDay())
        {
            int maxDay = getMaxDayOfMonth(copy.year, copy.month);
            if (copy.day > maxDay)
            {
                copy.day = maxDay;
            }
        }
        return copy;
    }

    public PureDate addWeeks(int weeks)
    {
        if (!hasDay())
        {
            throw new UnsupportedOperationException("Cannot add weeks to a date that does not have a day: " + this);
        }
        if (weeks == 0)
        {
            return this;
        }
        PureDate copy = clone();
        copy.incrementDay(7 * weeks);
        return copy;
    }

    public PureDate addDays(int days)
    {
        if (!hasDay())
        {
            throw new UnsupportedOperationException("Cannot add days to a date that does not have a day: " + this);
        }
        if (days == 0)
        {
            return this;
        }
        PureDate copy = clone();
        copy.incrementDay(days);
        return copy;
    }

    public PureDate addHours(int hours)
    {
        if (!hasHour())
        {
            throw new UnsupportedOperationException("Cannot add hours to a date that does not have an hour: " + this);
        }
        if (hours == 0)
        {
            return this;
        }
        PureDate copy = clone();
        copy.incrementHour(hours);
        return copy;
    }

    public PureDate addMinutes(int minutes)
    {
        if (!hasMinute())
        {
            throw new UnsupportedOperationException("Cannot add minutes to a date that does not have a minute: " + this);
        }
        if (minutes == 0)
        {
            return this;
        }
        PureDate copy = clone();
        copy.incrementMinute(minutes);
        return copy;
    }

    public PureDate addSeconds(int seconds)
    {
        if (!hasSecond())
        {
            throw new UnsupportedOperationException("Cannot add seconds to a date that does not have a second: " + this);
        }
        if (seconds == 0)
        {
            return this;
        }
        PureDate copy = clone();
        copy.incrementSecond(seconds);
        return copy;
    }

    public PureDate addMilliseconds(int milliseconds)
    {
        if (!hasSubsecond() || (this.subsecond.length() < 3))
        {
            throw new UnsupportedOperationException("Cannot add milliseconds to a date that does not have milliseconds: " + this);
        }
        if (milliseconds == 0)
        {
            return this;
        }
        PureDate copy = clone();

        int seconds = milliseconds / 1000;
        if (seconds != 0)
        {
            copy.incrementSecond(seconds);
            milliseconds %= 1000;
        }
        if (milliseconds < 0)
        {
            copy.decrementSubsecond(String.format("%03d", -milliseconds), 0, 3);
        }
        else if (milliseconds != 0)
        {
            copy.incrementSubsecond(String.format("%03d", milliseconds), 0, 3);
        }
        return copy;
    }

    public PureDate addMicroseconds(int microseconds)
    {
        if (microseconds == 0)
        {
            return this;
        }

        String subsecond = getSubsecond();
        if (subsecond.length() < 6)
        {
            throw new UnsupportedOperationException("Cannot add microseconds to a date that does not have microseconds: " + this);
        }

        PureDate copy = clone();
        int seconds = microseconds / 1_000_000;
        if (seconds != 0)
        {
            copy.incrementSecond(seconds);
            microseconds %= 1_000_000;
        }
        if (microseconds < 0)
        {
            copy.decrementSubsecond(String.format("%06d", -microseconds), 0, 6);
        }
        else if (microseconds != 0)
        {
            copy.incrementSubsecond(String.format("%06d", microseconds), 0, 6);
        }
        return copy;
    }

    public PureDate addNanoseconds(long nanoseconds)
    {
        if (nanoseconds == 0)
        {
            return this;
        }

        String subsecond = getSubsecond();
        if (subsecond.length() < 9)
        {
            throw new UnsupportedOperationException("Cannot add nanoseconds to a date that does not have nanoseconds: " + this);
        }

        PureDate copy = clone();
        long seconds = nanoseconds / 1_000_000_000;
        if (seconds != 0)
        {
            if ((seconds > Integer.MAX_VALUE) || (seconds < Integer.MIN_VALUE))
            {
                long days = seconds / 86_400;
                if ((days > Integer.MAX_VALUE) || (days < Integer.MIN_VALUE))
                {
                    throw new IllegalArgumentException(String.format("Cannot add %,d nanoseconds: too large", nanoseconds));
                }
                copy.incrementDay((int) days);
                seconds %= 86_400;
            }
            copy.incrementSecond((int) seconds);
            nanoseconds %= 1_000_000_000;
        }
        if (nanoseconds < 0)
        {
            copy.decrementSubsecond(String.format("%09d", -nanoseconds), 0, 9);
        }
        else if (nanoseconds != 0)
        {
            copy.incrementSubsecond(String.format("%09d", nanoseconds), 0, 9);
        }
        return copy;
    }


    public PureDate addSubseconds(String subseconds)
    {
        return adjustSubseconds(subseconds, true);
    }

    public PureDate subtractSubseconds(String subseconds)
    {
        return adjustSubseconds(subseconds, false);
    }

    private PureDate adjustSubseconds(String subseconds, boolean add)
    {
        if ((subseconds == null) || subseconds.isEmpty() || !StringIterate.isNumber(subseconds))
        {
            throw new IllegalArgumentException("subseconds must be a non-empty, numeric string; got: \"" + subseconds + "\"");
        }

        if (!hasSubsecond())
        {
            throw new UnsupportedOperationException("Cannot " + (add ? "add" : "subtract") + " subseconds " + (add ? "to" : "from") + " a date that does not have subseconds: " + this);
        }

        int start = 0;
        int end = subseconds.length();
        // ignore trailing zeros
        while ((end > start) && (subseconds.charAt(end - 1) == '0'))
        {
            end--;
        }

        if (start == end)
        {
            // adjusting by zero seconds, nothing to change
            return this;
        }

        if ((end - start) > this.subsecond.length())
        {
            throw new UnsupportedOperationException("Cannot " + (add ? "add" : "subtract") + " subseconds with " + (end - start) + " digits of precision " + (add ? "to" : "from") + " a date that has subseconds to only " + this.subsecond.length() + " digits of precision");
        }

        PureDate copy = clone();
        if (add)
        {
            copy.incrementSubsecond(subseconds, start, end);
        }
        else
        {
            copy.decrementSubsecond(subseconds, start, end);
        }
        return copy;
    }

    void setYear(int year)
    {
        this.year = year;
    }

    private void incrementYear(int delta)
    {
        this.year += delta;
    }

    void setMonth(int month)
    {
        if ((month < 1) || (month > 12))
        {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
        this.month = month;
    }

    private void incrementMonth(int delta)
    {
        incrementYear(delta / 12);
        this.month += (delta % 12);
        if (this.month < 1)
        {
            incrementYear(-1);
            this.month += 12;
        }
        else if (this.month > 12)
        {
            incrementYear(1);
            this.month -= 12;
        }
    }

    void setDay(int day)
    {
        if (this.month == -1)
        {
            throw new RuntimeException("Cannot set day without month");
        }
        if (day < 1)
        {
            throw new IllegalArgumentException("Invalid day: " + day);
        }
        if (day > getMaxDayOfMonth(this.year, this.month))
        {
            throw new IllegalArgumentException("Invalid day: " + this.year + "-" + this.month + "-" + this.day);
        }
        this.day = day;
    }

    private void incrementDay(int delta)
    {
        if (delta < 0)
        {
            this.day += delta;
            while (this.day < 1)
            {
                incrementMonth(-1);
                this.day += getMaxDayOfMonth(this.year, this.month);
            }
        }
        else if (delta > 0)
        {
            this.day += delta;
            for (int maxDay = getMaxDayOfMonth(this.year, this.month); this.day > maxDay; maxDay = getMaxDayOfMonth(this.year, this.month))
            {
                this.day -= maxDay;
                incrementMonth(1);
            }
        }
    }

    void setHour(int hour)
    {
        if (this.day == -1)
        {
            throw new RuntimeException("Cannot set hour without day");
        }
        if ((hour < 0) || (hour > 23))
        {
            throw new IllegalArgumentException("Invalid hour: " + hour);
        }
        this.hour = hour;
    }

    private void incrementHour(int delta)
    {
        incrementDay(delta / 24);
        this.hour += (delta % 24);
        if (this.hour < 0)
        {
            incrementDay(-1);
            this.hour += 24;
        }
        else if (this.hour > 23)
        {
            incrementDay(1);
            this.hour -= 24;
        }
    }

    void setMinute(int minute)
    {
        if (this.hour == -1)
        {
            throw new RuntimeException("Cannot set minute without hour");
        }
        if ((minute < 0) || (minute > 59))
        {
            throw new IllegalArgumentException("Invalid minute: " + minute);
        }
        this.minute = minute;
    }

    private void incrementMinute(int delta)
    {
        incrementHour(delta / 60);
        this.minute += (delta % 60);
        if (this.minute < 0)
        {
            incrementHour(-1);
            this.minute += 60;
        }
        else if (this.minute > 59)
        {
            incrementHour(1);
            this.minute -= 60;
        }
    }

    void setSecond(int second)
    {
        if (this.minute == -1)
        {
            throw new RuntimeException("Cannot set second without minute");
        }
        if ((second < 0) || (second > 59))
        {
            throw new IllegalArgumentException("Invalid second: " + second);
        }
        this.second = second;
    }

    private void incrementSecond(int delta)
    {
        incrementMinute(delta / 60);
        this.second += (delta % 60);
        if (this.second < 0)
        {
            incrementMinute(-1);
            this.second += 60;
        }
        else if (this.second > 59)
        {
            incrementMinute(1);
            this.second -= 60;
        }
    }

    void setSubsecond(String string, int start, int end)
    {
        if (this.second == -1)
        {
            throw new RuntimeException("Cannot set sub-second without second");
        }
        String newSubsecond = string.substring(start, end);
        if (newSubsecond.isEmpty() || !StringIterate.isNumber(newSubsecond))
        {
            throw new IllegalArgumentException("Invalid subsecond value: \"" + string.substring(start, end) + "\"");
        }
        this.subsecond = newSubsecond;
    }

    private void incrementSubsecond(String delta, int start, int end)
    {
        char[] digits = this.subsecond.toCharArray();
        boolean carry = false;
        for (int i = (end - start) - 1; i >= 0; i--)
        {
            int sum = (int) digits[i] + (int) delta.charAt(i + start) - 96;
            if (carry)
            {
                sum += 1;
            }
            if (sum >= 10)
            {
                carry = true;
                sum -= 10;
            }
            else
            {
                carry = false;
            }
            digits[i] = (char) (sum + 48);
        }
        if (carry)
        {
            incrementSecond(1);
        }
        this.subsecond = new String(digits);
    }

    private void decrementSubsecond(String delta, int start, int end)
    {
        char[] digits = this.subsecond.toCharArray();
        boolean carry = false;
        for (int i = (end - start) - 1; i >= 0; i--)
        {
            int difference = (int) digits[i] - (int) delta.charAt(i + start);
            if (carry)
            {
                difference -= 1;
            }
            if (difference < 0)
            {
                carry = true;
                difference = 10 + difference;
            }
            else
            {
                carry = false;
            }
            digits[i] = (char) (difference + 48);
        }
        if (carry)
        {
            incrementSecond(-1);
        }
        this.subsecond = new String(digits);
    }

    void setTimeZone(String string, int start, int end)
    {
        if (this.minute == -1)
        {
            throw new RuntimeException("Cannot set time zone without time");
        }
        char first = string.charAt(start++);
        boolean negative;
        if (first == '+')
        {
            negative = false;
        }
        else if (first == '-')
        {
            negative = true;
        }
        else
        {
            throw new IllegalArgumentException("Invalid time zone: " + string.substring(start - 1, end));
        }
        if (end - start != 4)
        {
            throw new IllegalArgumentException("Invalid time zone: " + string.substring(start - 1, end));
        }

        int hourOffset = Integer.parseInt(string.substring(start, start + 2));
        int minuteOffset = Integer.parseInt(string.substring(start + 2, end));

        if ((hourOffset != 0) || (minuteOffset != 0))
        {
            // Adjust to UTC
            if (!negative)
            {
                // Offset is from UTC, so we need to reverse the direction
                hourOffset = -hourOffset;
                minuteOffset = -minuteOffset;
            }
            GregorianCalendar calendar = new GregorianCalendar(this.year, this.month - 1, this.day, this.hour, this.minute);
            calendar.add(Calendar.HOUR, hourOffset);
            calendar.add(Calendar.MINUTE, minuteOffset);
            this.year = calendar.get(Calendar.YEAR);
            this.month = calendar.get(Calendar.MONTH) + 1;
            this.day = calendar.get(Calendar.DAY_OF_MONTH);
            this.hour = calendar.get(Calendar.HOUR_OF_DAY);
            this.minute = calendar.get(Calendar.MINUTE);
        }
    }

    public int getQuarter()
    {
        int monthNumber = this.getMonth();

        if (monthNumber <= 3)
        {
            return 1;
        }
        else if (monthNumber <= 6)
        {
            return 2;
        }
        else if (monthNumber <= 9)
        {
            return 3;
        }
        else
        {
            return 4;
        }
    }

    /**
     * Get a Gregorian calendar representation of this Pure date.
     * Note that precision may be lost if the Pure date has
     * precision greater than millisecond.
     *
     * @return Gregorian calendar for Pure date
     */
    public GregorianCalendar getCalendar()
    {
        GregorianCalendar calendar = new GregorianCalendar(this.year, (this.month == -1) ? 0 : (this.month - 1), (this.day == -1) ? 1 : this.day);
        calendar.setTimeZone(GMT_TIME_ZONE);
        if (this.hour != -1)
        {
            calendar.set(Calendar.HOUR, this.hour);
            if (this.minute != -1)
            {
                calendar.set(Calendar.MINUTE, this.minute);
                if (this.second != -1)
                {
                    calendar.set(Calendar.SECOND, this.second);
                    if (this.subsecond != null)
                    {
                        String millisecond;
                        int length = this.subsecond.length();
                        switch (length)
                        {
                            case 1:
                            {
                                millisecond = this.subsecond + "00";
                                break;
                            }
                            case 2:
                            {
                                millisecond = this.subsecond + "0";
                                break;
                            }
                            case 3:
                            {
                                millisecond = this.subsecond;
                                break;
                            }
                            default:
                            {
                                millisecond = this.subsecond.substring(0, 3);
                            }
                        }
                        calendar.set(Calendar.MILLISECOND, Integer.valueOf(millisecond));
                    }
                }
            }
        }
        return calendar;
    }

    public void writeString(Appendable appendable)
    {
        try
        {
            appendable.append(Integer.toString(this.year));
            if (hasMonth())
            {
                appendable.append(DATE_SEPARATOR);
                appendTwoDigitInt(appendable, this.month);
                if (hasDay())
                {
                    appendable.append(DATE_SEPARATOR);
                    appendTwoDigitInt(appendable, this.day);
                    if (hasHour())
                    {
                        appendable.append(DATE_TIME_SEPARATOR);
                        appendTwoDigitInt(appendable, this.hour);
                        if (hasMinute())
                        {
                            appendable.append(TIME_SEPARATOR);
                            appendTwoDigitInt(appendable, this.minute);
                            if (hasSecond())
                            {
                                appendable.append(TIME_SEPARATOR);
                                appendTwoDigitInt(appendable, this.second);
                                if (hasSubsecond())
                                {
                                    appendable.append('.');
                                    appendable.append(this.subsecond);
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString()
    {
        if (!hasMonth())
        {
            return Integer.toString(this.year);
        }
        StringBuilder builder = new StringBuilder(32);
        writeString(builder);
        return builder.toString();
    }

    private boolean isLeapYear()
    {
        return isLeapYear(this.year);
    }

    private static void appendTwoDigitInt(Appendable appendable, int integer)
    {
        appendZeroPaddedInt(appendable, integer, 2);
    }

    private static void appendZeroPaddedInt(Appendable appendable, int integer, int minLength)
    {
        String string = Integer.toString(integer);
        try
        {
            for (int fill = minLength - string.length(); fill > 0; fill--)
            {
                appendable.append('0');
            }
            appendable.append(string);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static int getCharCountFrom(char character, String string, int start)
    {
        int count = 0;
        for (int i = start, length = string.length(); (i < length) && (string.charAt(i) == character); i++)
        {
            count++;
        }
        return count;
    }

    public static PureDate fromCalendar(GregorianCalendar calendar, int precision)
    {
        return fromCalendar(calendar, precision, new PureDate());
    }

    public static PureDate fromCalendar(GregorianCalendar calendar)
    {
        return fromCalendar(calendar, Calendar.MILLISECOND, new PureDate());
    }

    public static <T extends PureDate> T fromCalendar(GregorianCalendar calendar, int precision, T newDate)
    {
        TimeZone timeZone = calendar.getTimeZone();
        if (!GMT_TIME_ZONE.equals(timeZone))
        {
            // Possibly adjust to UTC
            long time = calendar.getTimeInMillis();
            int offset = timeZone.getOffset(time);
            if (offset != 0)
            {
                // Adjust to UTC
                calendar = new GregorianCalendar(GMT_TIME_ZONE);
                calendar.setTimeInMillis(time - offset);
            }
        }

        PureDate date = (PureDate) newDate;
        date.year = calendar.get(Calendar.YEAR);
        if (precision >= Calendar.MONTH)
        {
            date.month = calendar.get(Calendar.MONTH) + 1;
            if (precision >= Calendar.DAY_OF_MONTH)
            {
                date.day = calendar.get(Calendar.DAY_OF_MONTH);
                if (precision >= Calendar.HOUR_OF_DAY)
                {
                    date.hour = calendar.get(Calendar.HOUR_OF_DAY);
                    if (precision >= Calendar.MINUTE)
                    {
                        date.minute = calendar.get(Calendar.MINUTE);
                        if (precision >= Calendar.SECOND)
                        {
                            date.second = calendar.get(Calendar.SECOND);
                            if (precision >= Calendar.MILLISECOND)
                            {
                                date.subsecond = String.format("%03d", calendar.get(Calendar.MILLISECOND));
                            }
                        }
                    }
                }
            }
        }
        return newDate;
    }

    public static PureDate fromDate(Date date)
    {
        if (date instanceof java.sql.Date)
        {
            return fromSQLDate((java.sql.Date) date);
        }
        if (date instanceof java.sql.Timestamp)
        {
            return fromSQLTimestamp((java.sql.Timestamp) date);
        }
        GregorianCalendar calendar = new GregorianCalendar(GMT_TIME_ZONE);
        calendar.setTime(date);
        return fromCalendar(calendar, Calendar.MILLISECOND, new PureDate());
    }

    public static PureDate fromSQLDate(java.sql.Date date)
    {
        GregorianCalendar calendar = new GregorianCalendar(GMT_TIME_ZONE);
        calendar.setTime(date);
        return fromCalendar(calendar, Calendar.DAY_OF_MONTH, new PureDate());
    }

    public static PureDate fromSQLTimestamp(java.sql.Timestamp timestamp)
    {
        GregorianCalendar calendar = new GregorianCalendar(GMT_TIME_ZONE);
        calendar.setTime(timestamp);
        PureDate pureDate = fromCalendar(calendar, Calendar.SECOND, new PureDate());
        ((PureDate) pureDate).subsecond = String.format("%09d", timestamp.getNanos());
        return pureDate;
    }

    public static PureDate newPureDate(int year)
    {
        PureDate date = new PureDate();
        date.setYear(year);
        return date;
    }

    public static PureDate newPureDate(int year, int month)
    {
        PureDate date = newPureDate(year);
        date.setMonth(month);
        return date;
    }

    public static PureDate newPureDate(int year, int month, int day)
    {
        PureDate date = new PureDate();
        date.setYear(year);
        date.setMonth(month);
        date.setDay(day);
        return date;
    }

    public static PureDate newPureDate(int year, int month, int day, int hour)
    {
        PureDate date = new PureDate();
        date.setYear(year);
        date.setMonth(month);
        date.setDay(day);
        date.setHour(hour);
        return date;
    }

    public static PureDate newPureDate(int year, int month, int day, int hour, int minute)
    {
        PureDate date = newPureDate(year, month, day, hour);
        date.setMinute(minute);
        return date;
    }

    public static PureDate newPureDate(int year, int month, int day, int hour, int minute, int second)
    {
        PureDate date = newPureDate(year, month, day, hour, minute);
        date.setSecond(second);
        return date;
    }

    public static PureDate newPureDate(int year, int month, int day, int hour, int minute, int second, String subsecond)
    {
        if (subsecond == null)
        {
            throw new IllegalArgumentException("Invalid subsecond value: null");
        }
        PureDate date = newPureDate(year, month, day, hour, minute, second);
        date.setSubsecond(subsecond, 0, subsecond.length());
        return date;
    }

    public static PureDate newPureDate(int year, int month, int day, int hour, int minute, Number second)
    {
        int secondInt;
        String subsecond = null;
        if ((second instanceof Integer) || (second instanceof Long) || (second instanceof BigInteger))
        {
            secondInt = second.intValue();
        }
        else if ((second instanceof Float) || (second instanceof Double) || (second instanceof BigDecimal))
        {
            secondInt = second.intValue();
            String string = new BigDecimal(second.toString()).toPlainString();
            int index = string.indexOf('.');
            if (index != -1)
            {
                subsecond = string.substring(index + 1);
            }
        }
        else
        {
            throw new RuntimeException("Unhandled number: " + second);
        }

        PureDate date = newPureDate(year, month, day, hour, minute, secondInt);
        if (subsecond != null)
        {
            date.setSubsecond(subsecond, 0, subsecond.length());
        }

        return date;
    }

    /**
     * Parse a string into a Pure date.
     *
     * @param string string
     * @return Pure date
     */
    public static PureDate parsePureDate(String string)
    {
        return parsePureDate(string, 0, string.length());
    }

    /**
     * Parse a portion of a string into a Pure date.
     *
     * @param string string
     * @param start  start index of the date (inclusive)
     * @param end    end index of the date (exclusive)
     * @return Pure date
     */
    public static PureDate parsePureDate(String string, int start, int end)
    {
        // Skip whitespace at start and end
        while ((start < end) && (string.charAt(start) <= ' '))
        {
            start++;
        }
        end--;
        while ((end > start) && (string.charAt(end) <= ' '))
        {
            end--;
        }
        end++;
        if (start >= end)
        {
            throwInvalidDateString(string);
        }

        // Skip Pure date prefix character if present
        if (string.charAt(start) == DATE_PREFIX)
        {
            start++;
            if (start >= end)
            {
                throwInvalidDateString(string);
            }
        }


        // Year
        int year = -1;
        int previous = (string.charAt(start) == '-') ? start + 1 : start;
        int index = findNonDigit(string, previous, end);
        try
        {
            year = Integer.parseInt(string.substring(start, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing year", string, start, end);
        }

        if (index == end)
        {
            return newPureDate(year);
        }
        if (string.charAt(index++) != DATE_SEPARATOR)
        {
            throwInvalidDateString(string, start, end);
        }

        // Month
        int month = -1;
        previous = index;
        index = findNonDigit(string, previous, end);
        try
        {
            month = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing month", string, start, end);
        }

        if (index == end)
        {
            return newPureDate(year, month);
        }
        if (string.charAt(index++) != DATE_SEPARATOR)
        {
            throwInvalidDateString(string, start, end);
        }

        // Day
        int day = -1;
        previous = index;
        index = findNonDigit(string, previous, end);
        try
        {
            day = Integer.parseInt(string.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing day", string, start, end);
        }

        if (index == end)
        {
            return newPureDate(year, month, day);
        }
        if (string.charAt(index++) != DATE_TIME_SEPARATOR)
        {
            throwInvalidDateString(string, start, end);
        }

        // Hour
        PureDate date = new PureDate();
        date.setYear(year);
        date.setMonth(month);
        date.setDay(day);
        previous = index;
        index = findNonDigit(string, previous, end);
        try
        {
            date.setHour(Integer.parseInt(string.substring(previous, index)));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing hour", string, start, end);
        }

        if (index == end)
        {
            return date;
        }

        char character = string.charAt(index++);
        if (character != TIME_SEPARATOR)
        {
            throwInvalidDateString(string, start, end);
        }

        // Minute
        previous = index;
        index = findNonDigit(string, previous, end);
        try
        {
            date.setMinute(Integer.parseInt(string.substring(previous, index)));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing minute", string, start, end);
        }

        if (index == end)
        {
            return date;
        }

        character = string.charAt(index++);
        if (character != TIME_SEPARATOR)
        {
            // Time zone
            date.setTimeZone(string, index - 1, end);
            return date;
        }

        // Second
        previous = index;
        index = findNonDigit(string, previous, end);
        try
        {
            date.setSecond(Integer.parseInt(string.substring(previous, index)));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing second", string, start, end);
        }

        if (index == end)
        {
            return date;
        }

        if (string.charAt(index) == '.')
        {
            // Subsecond
            previous = index + 1;
            index = findNonDigit(string, previous, end);
            if (index == previous)
            {
                throwInvalidDateString(string, start, end);
            }
            date.setSubsecond(string, previous, index);

            if (index == end)
            {
                return date;
            }
        }

        if (index < end)
        {
            // Time zone
            date.setTimeZone(string, index, end);
        }
        return date;
    }

    static int getYearDays(int year)
    {
        return isLeapYear(year) ? 366 : 365;
    }

    /**
     * Return the number of date/time units between the two dates.
     *
     * @param otherDate date to be compared with
     * @param unit      corresponds to the Pure DurationUnit
     * @return the difference in the chosen units.
     */
    public long dateDifference(org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate otherDate, String unit)
    {
        long result = 0;
        if (this.equals(otherDate))
        {
            result = 0;
        }
        else
        {
            switch (unit)
            {
                case "YEARS":
                {
                    result = DateDiff.getDiffYears(this, otherDate);
                    break;
                }
                case "MONTHS":
                {
                    result = DateDiff.getDiffMonths(this, otherDate);
                    break;
                }
                case "WEEKS":
                {
                    result = DateDiff.getDateDiffWeeks(this, otherDate);
                    break;
                }
                case "DAYS":
                {
                    result = DateDiff.getDiffDays(this, otherDate);
                    break;
                }
                case "HOURS":
                {
                    result = DateDiff.getDiffHours(this, otherDate);
                    break;
                }
                case "MINUTES":
                {
                    result = DateDiff.getDiffMinutes(this, otherDate);
                    break;
                }
                case "SECONDS":
                {
                    result = DateDiff.getDiffSeconds(this, otherDate);
                    break;
                }
                case "MILLISECONDS":
                {
                    result = DateDiff.getDiffInMilliseconds(this, otherDate);
                    break;
                }
                default:
                {
                    throw new IllegalArgumentException("Unsupported duration unit: " + unit);
                }
            }
        }
        int sign = otherDate.compareTo(this);
        return sign * result;
    }

    /**
     * Format a Java date to a canonical Pure date string.  The Java date
     * is assumed to represent a date in UTC.
     *
     * @param date Java date
     * @return canonical Pure date string
     */
    public static String formatDate(Date date)
    {
        if (date instanceof java.sql.Date)
        {
            return date.toString();
        }
        return fromDate(date).toString();
    }

    /**
     * Return the index of the first character in string
     * between start and end that is not a digit.  Returns
     * end if no non-digit character is found.
     *
     * @param string date string
     * @param start  start index for search (inclusive)
     * @param end    end index for search (exclusive)
     * @return index of the first non-digit character
     */
    private static int findNonDigit(String string, int start, int end)
    {
        while ((start < end) && isDigit(string.charAt(start)))
        {
            start++;
        }
        return start;
    }

    private static boolean isDigit(char character)
    {
        return ('0' <= character) && (character <= '9');
    }

    private static int getMaxDayOfMonth(int year, int month)
    {
        switch (month)
        {
            case 2:
            {
                return isLeapYear(year) ? 29 : 28;
            }
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
            {
                return 31;
            }
            default:
            {
                return 30;
            }
        }
    }


    private static boolean isLeapYear(int year)
    {
        return (year % 400 == 0) || ((year % 4 == 0) && (year % 100 != 0));
    }

    private static void throwInvalidDateString(String string)
    {
        throwInvalidDateString(string, 0, string.length());
    }

    private static void throwInvalidDateString(String string, int start, int end)
    {
        throwInvalidDateString("Invalid date string", string, start, end);
    }

    private static void throwInvalidDateString(String message, String dateString, int start, int end)
    {
        throw new IllegalArgumentException(message + ": '" + dateString.substring(start, end).replace("'", "\\'") + "'");
    }

    static class DateDiff
    {
        private DateDiff()
        {
        }

        static long getDiffYears(org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate from, org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate to)
        {
            return Math.abs(from.getYear() - to.getYear());
        }

        static long getDiffMonths(org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate from, org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate to)
        {
            int thisMonthTotal = (from.getYear() * 12) + from.getMonth();
            int otherMonthTotal = (to.getYear() * 12) + to.getMonth();
            return Math.abs(thisMonthTotal - otherMonthTotal);
        }

        static long getDateDiffWeeks(org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate from, org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate to)
        {
            long absDateDiffDays = Math.abs(getDiffDays(from, to));
            int noDaysTillSunday = daysUntilSunday(from.getCalendar(), to.getCalendar());

            if (noDaysTillSunday > absDateDiffDays)
            {
                return 0;
            }
            else
            {
                long fullWeeks = (absDateDiffDays - noDaysTillSunday) / 7;
                boolean partialWeek = noDaysTillSunday > 0;
                return fullWeeks + (partialWeek ? 1 : 0);
            }
        }

        static long getDiffDays(org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate first, org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate second)
        {
            Pair<GregorianCalendar, org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate> thisCalPair = Tuples.pair(first.getCalendar(), first);
            Pair<GregorianCalendar, org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate> otherCalPair = Tuples.pair(second.getCalendar(), second);
            Pair<Pair<GregorianCalendar, org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate>, Pair<GregorianCalendar, org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate>> earlierLaterPair = thisCalPair.getOne().before(otherCalPair.getOne()) ? Tuples.pair(thisCalPair, otherCalPair) : Tuples.pair(otherCalPair, thisCalPair);
            long result = 0;
            if (first.getYear() != second.getYear())
            {
                int fromYear = earlierLaterPair.getOne().getTwo().getYear();
                int toYear = earlierLaterPair.getTwo().getTwo().getYear();
                result += DateFunctions.getYearDays(fromYear) - earlierLaterPair.getOne().getOne().get(Calendar.DAY_OF_YEAR);
                int nextYear = fromYear + 1;
                for (; nextYear != toYear; nextYear++)
                {
                    result += DateFunctions.getYearDays(nextYear);
                }
                result += earlierLaterPair.getTwo().getOne().get(Calendar.DAY_OF_YEAR);
            }
            else
            {
                result = (long)earlierLaterPair.getTwo().getOne().get(Calendar.DAY_OF_YEAR) - earlierLaterPair.getOne().getOne().get(Calendar.DAY_OF_YEAR);
            }
            return result;
        }

        static long getDiffHours(org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate first, org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate second)
        {
            long msDiff = getDiffInMilliseconds(first, second);
            return TimeUnit.MILLISECONDS.toHours(msDiff);
        }

        static long getDiffMinutes(org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate first, org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate second)
        {
            long msDiff = getDiffInMilliseconds(first, second);
            return TimeUnit.MILLISECONDS.toMinutes(msDiff);
        }

        static long getDiffSeconds(org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate first, org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate second)
        {
            long msDiff = getDiffInMilliseconds(first, second);
            return TimeUnit.MILLISECONDS.toSeconds(msDiff);
        }

        static long getDiffInMilliseconds(org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate date1, org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate date2)
        {
            long time1 = date1.getCalendar().getTimeInMillis();
            long time2 = date2.getCalendar().getTimeInMillis();
            return Math.abs(time1 - time2);
        }

        private static int daysUntilSunday(Calendar start, Calendar end)
        {
            if (start.before(end))
            {
                int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
                return 7 - (dayOfWeek - 1);
            }
            else
            {
                int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
                return dayOfWeek - 1;
            }
        }
    }

    public static class DateFunctions
    {
        static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

        static int getYearDays(int year)
        {
            return isLeapYear(year) ? 366 : 365;
        }

    }
}
