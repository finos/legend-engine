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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

public class DateParser implements ValueParser
{
    private final List<String> possibleFormats;
    private final List<DateTimeFormatter> possibleFormatters;
    private String format;
    private DateTimeFormatter formatter;

    private DateParser(List<String> formats)
    {
        this.possibleFormats = formats;
        this.possibleFormatters = formats.stream().map(DateTimeFormatter::ofPattern).collect(Collectors.toList());
        if (possibleFormats.size() == 1)
        {
            formatter = possibleFormatters.get(0);
            format = possibleFormats.get(0);
        }
    }

    public LocalDate parse(String s) throws ParseException
    {
        if (formatter == null)
        {
            for (int i = 0; i < possibleFormatters.size(); i++)
            {
                try
                {
                    LocalDate date = LocalDate.parse(s, possibleFormatters.get(i));
                    formatter = possibleFormatters.get(i);
                    format = possibleFormats.get(i);
                    return date;
                }
                catch (DateTimeParseException e)
                {
                    // Ignore
                }
            }
            throw new ParseException("Unable to parse date: " + s, 0);
        }
        else
        {
            try
            {
                return LocalDate.parse(s, formatter);
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
        if (formatter == null)
        {
            for (int i = 0; i < possibleFormatters.size(); i++)
            {
                try
                {
                    possibleFormatters.get(i).parse(s);
                    formatter = possibleFormatters.get(i);
                    format = possibleFormats.get(i);
                    return null;
                }
                catch (DateTimeParseException e)
                {
                    // Ignore
                }
            }
            return "Unparseable date: \"" + s + "\" for formats " + possibleFormats.stream().map(f -> "'" + f + "'").collect(Collectors.joining(", "));
        }
        else
        {
            try
            {
                formatter.parse(s);
                return null;
            }
            catch (DateTimeParseException e)
            {
                return "Unparseable date: \"" + s + "\" for format '" + format + "'";
            }
        }
    }

    public String toString(LocalDate date)
    {
        return possibleFormatters.get(0).format(date);
    }

    public static DateParser of(List<String> format)
    {
        return new DateParser(format);
    }
}
