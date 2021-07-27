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

public class DateParser implements ValueParser
{
    private final DateTimeFormatter formatter;

    private DateParser(String format)
    {
        this.formatter = DateTimeFormatter.ofPattern(format);
    }

    public LocalDate parse(String s) throws ParseException
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
            return "Unparseable date: \"" + s + "\"";
        }
    }

    public String toString(LocalDate date)
    {
        return formatter.format(date);
    }

    public static DateParser of(String format)
    {
        return new DateParser(format);
    }
}
