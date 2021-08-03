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

public class DateTimeParser implements ValueParser
{
    private final DateTimeFormatter formatter;
    private final ZoneId timeZone;

    private DateTimeParser(String format, String timeZone)
    {
        this.timeZone = ZoneId.of(timeZone, ZoneId.SHORT_IDS);
        this.formatter = DateTimeFormatter.ofPattern(format).withZone(this.timeZone);
    }

    public Instant parse(String s) throws ParseException
    {
        try
        {
            try
            {
                return Instant.from(formatter.parse(s));
            }
            catch (DateTimeException e)
            {
                return LocalDateTime.from(formatter.parse(s)).atZone(timeZone).toInstant();
            }
        }
        catch (DateTimeParseException e)
        {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    @Override
    public String validate(String s)
    {
        try
        {
            formatter.parse(s);
            return null;
        }
        catch (DateTimeParseException e)
        {
            return "Unparseable datetime: \"" + s + "\"";
        }
    }

    public String toString(Instant dateTime)
    {
        return formatter.format(dateTime);
    }

    public static DateTimeParser of(String format, String timeZone)
    {
        return new DateTimeParser(format, timeZone);
    }
}
