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

package org.finos.legend.engine.language.pure.grammar.from.domain;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictDate;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class DateParseTreeWalker
{
    static final char DATE_SEPARATOR = '-';
    static final char DATE_TIME_SEPARATOR = 'T';

    public static final char DATE_PREFIX = '%';
    public TerminalNode dateToken;
    public ParseTreeWalkerSourceInformation walkerSourceInformation;

    public DateParseTreeWalker(TerminalNode dateToken, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.dateToken = dateToken;
        this.walkerSourceInformation = walkerSourceInformation;
    }

    public CDate visitDefinition()
    {
        String value = this.dateToken.getText();
        return this.parsePureDate(value, 0, value.length());
    }

    /**
     * Parse a portion of a string into the correct CDate type
     *
     * @param value string
     * @param start start index of the date (inclusive)
     * @param end   end index of the date (exclusive)
     * @return Pure date
     */
    private CDate parsePureDate(String value, int start, int end)
    {
        // Skip whitespace at start and end
        while ((start < end) && (value.charAt(start) <= ' '))
        {
            start++;
        }
        end--;
        while ((end > start) && (value.charAt(end) <= ' '))
        {
            end--;
        }
        end++;
        if (start >= end)
        {
            throwInvalidDateString(value);
        }

        // Skip Pure date prefix character if present
        if (value.charAt(start) == DATE_PREFIX)
        {
            start++;
            if (start >= end)
            {
                throwInvalidDateString(value);
            }
        }

        // Year
        int year = -1;
        int previous = (value.charAt(start) == '-') ? start + 1 : start;
        int index = ParserTreeWalkerUtility.findNonDigit(value, previous, end);
        try
        {
            year = Integer.parseInt(value.substring(start, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing year", value, start, end);
        }

        if (index == end && year != -1)
        {
            return createDateTime(value.substring(value.lastIndexOf(DATE_PREFIX) + 1));
        }
        if (value.charAt(index++) != DATE_SEPARATOR)
        {
            throwInvalidDateString(value, start, end);
        }

        // Month
        int month = -1;
        previous = index;
        index = ParserTreeWalkerUtility.findNonDigit(value, previous, end);
        try
        {
            month = Integer.parseInt(value.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing month", value, start, end);
        }

        if (index == end && month != -1)
        {
            return createDateTime(value);
        }
        if (value.charAt(index++) != DATE_SEPARATOR)
        {
            throwInvalidDateString(value, start, end);
        }

        // Day
        int day = -1;
        previous = index;
        index = ParserTreeWalkerUtility.findNonDigit(value, previous, end);
        try
        {
            day = Integer.parseInt(value.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidDateString("Error parsing day", value, start, end);
        }
        if (index == end && day != -1)
        {
            CStrictDate cStrictDate = new CStrictDate(value.substring(value.lastIndexOf(DATE_PREFIX) + 1));
            cStrictDate.sourceInformation = walkerSourceInformation.getSourceInformation(this.dateToken.getSymbol());
            return cStrictDate;
        }
        if (value.charAt(index++) != DATE_TIME_SEPARATOR)
        {
            throwInvalidDateString(value, start, end);
        }
        return createDateTime(value.substring(value.lastIndexOf(DATE_PREFIX) + 1));
    }

    private CDateTime createDateTime(String value)
    {
        CDateTime cDateTime = new CDateTime(value);
        cDateTime.sourceInformation = walkerSourceInformation.getSourceInformation(this.dateToken.getSymbol());
        return cDateTime;
    }

    private void throwInvalidDateString(String string)
    {
        throwInvalidDateString(string, 0, string.length());
    }

    private void throwInvalidDateString(String string, int start, int end)
    {
        throwInvalidDateString("Invalid date value ", string, start, end);
    }

    private void throwInvalidDateString(String message, String dateString, int start, int end)
    {
        throw new EngineException(message + " '" + dateString.substring(start, end).replace("'", "\\'") + "'", this.walkerSourceInformation.getSourceInformation(this.dateToken.getSymbol()), EngineErrorType.PARSER);
    }
}
