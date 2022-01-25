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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser;

import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class DateTimeParser implements ValueParser
{
    public abstract Instant parse(String s) throws ParseException;
    public abstract String toString(Instant dateTime);

    public static DateTimeParser of(List<String> format, String timeZone)
    {
        return new BasicDateTimeParser(format, timeZone);
    }

    public static DateTimeParser of(DateTimeParser base, String timeZone)
    {
        if (!(base instanceof  BasicDateTimeParser))
        {
            throw new IllegalArgumentException("Can only override timezone on a BasicDateTimeParser");
        }
        return new OverrideTimezoneDateTimeParser((BasicDateTimeParser) base, timeZone);
    }

    private static class BasicDateTimeParser extends DateTimeParser
    {
        private final List<String> possibleFormats;
        private final List<DateTimeFormatter> possibleFormatters;
        private final ZoneId timeZone;
        private String format;
        private DateTimeFormatter formatter;

        private BasicDateTimeParser(List<String> formats, String timeZone)
        {
            this.possibleFormats = formats;
            this.possibleFormatters = formats.stream().map(DateTimeFormatter::ofPattern).collect(Collectors.toList());
            this.timeZone = ZoneId.of(timeZone, ZoneId.SHORT_IDS);
            if (possibleFormats.size() == 1)
            {
                formatter = possibleFormatters.get(0);
                format = possibleFormats.get(0);
            }
        }

        @Override
        public Instant parse(String s) throws ParseException
        {
            return doParse(s, timeZone);
        }

        private Instant doParse(String s, ZoneId tz) throws ParseException
        {
            if (formatter == null)
            {
                for (int i=0; i<possibleFormatters.size(); i++)
                {
                    try
                    {
                        try
                        {
                            Instant dateTime = Instant.from(possibleFormatters.get(i).withZone(tz).parse(fixTimezone(s, possibleFormats.get(i))));
                            formatter = possibleFormatters.get(i);
                            format = possibleFormats.get(i);
                            return dateTime;
                        }
                        catch (DateTimeException e)
                        {
                            Instant dateTime = LocalDateTime.from(possibleFormatters.get(i).parse(s)).atZone(tz).toInstant();
                            formatter = possibleFormatters.get(i);
                            format = possibleFormats.get(i);
                            return dateTime;
                        }
                    }
                    catch (DateTimeParseException e)
                    {
                        // Ignore
                    }
                }
                throw new ParseException("Unable to parse datetime: " + s, 0);
            }
            else
            {
                try
                {
                    try
                    {
                        return Instant.from(formatter.withZone(tz).parse(fixTimezone(s, format)));
                    }
                    catch (DateTimeException e)
                    {
                        return LocalDateTime.from(formatter.parse(s)).atZone(tz).toInstant();
                    }
                }
                catch (DateTimeParseException e)
                {
                    throw new ParseException(e.getMessage(), 0);
                }
            }
        }
        @Override
        public String validate(String s)
        {
            return doValidate(s, timeZone);
        }

        String doValidate(String s, ZoneId tz)
        {
            if (formatter == null)
            {
                for (int i=0; i<possibleFormatters.size(); i++)
                {
                    try
                    {
                        possibleFormatters.get(i).withZone(tz).parse(fixTimezone(s, possibleFormats.get(i)));
                        formatter = possibleFormatters.get(i);
                        format = possibleFormats.get(i);
                        return null;
                    }
                    catch (DateTimeParseException e)
                    {
                        // Ignore
                    }
                }
                return "Unparseable datetime: \"" + s + "\" for formats " + possibleFormats.stream().map(f -> "'" + f + "'").collect(Collectors.joining(", "));
            }
            else
            {
                try
                {
                    formatter.withZone(tz).parse(fixTimezone(s, format));
                    return null;
                }
                catch (DateTimeParseException e)
                {
                    return "Unparseable datetime: \"" + s + "\" for format '" + format  + "'";
                }
            }
        }

        public String toString(Instant dateTime)
        {
            return doToString(dateTime, timeZone);
        }

        String doToString(Instant dateTime, ZoneId tz)
        {
            return possibleFormatters.get(0).withZone(tz).format(dateTime);
        }

        // TODO Allow configuration of this - in the interim zz implies do this while z or zzz will not invoke it
        private String fixTimezone(String s, String fmt)
        {
            return Arrays.asList(fmt.split(" ")).contains("zz")
                    ? s.replace("BST", "+01:00")
                    : s;
        }
    }

    private static class OverrideTimezoneDateTimeParser extends DateTimeParser
    {
        private final BasicDateTimeParser base;
        private final ZoneId timeZone;

        private OverrideTimezoneDateTimeParser(BasicDateTimeParser base, String timeZone)
        {
            this.base = base;
            this.timeZone = ZoneId.of(timeZone, ZoneId.SHORT_IDS);
        }

        @Override
        public Instant parse(String s) throws ParseException
        {
            return base.doParse(s, timeZone);
        }

        @Override
        public String validate(String s)
        {
            return base.doValidate(s, timeZone);
        }

        @Override
        public String toString(Instant dateTime)
        {
            return base.doToString(dateTime, timeZone);
        }
    }
}
