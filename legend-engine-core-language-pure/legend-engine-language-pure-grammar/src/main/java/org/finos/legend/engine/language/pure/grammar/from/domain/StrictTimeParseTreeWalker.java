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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictTime;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class StrictTimeParseTreeWalker
{
    public static final char STRICT_TIME_PREFIX = '%';
    public TerminalNode timeToken;
    public ParseTreeWalkerSourceInformation walkerSourceInformation;

    public StrictTimeParseTreeWalker(TerminalNode timeToken, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.timeToken = timeToken;
        this.walkerSourceInformation = walkerSourceInformation;
    }

    public CStrictTime visitStrictTimeDefinition()
    {
        String value = this.timeToken.getText();
        return this.parseStrictTime(value);
    }

    /**
     * Parse a portion of a string into the correct CStrictTime type
     *
     * @param value string
     * @return Pure StrictTime
     */
    private CStrictTime parseStrictTime(String value)
    {
        // Skip whitespace at start and end
        value = value.trim();
        int start = 0;
        int end = value.length();
        if (start >= end)
        {
            throwInvalidTimeString("Invalid StrictTime string", value, start, end);
        }
        // Skip Pure date prefix character if present
        if (value.charAt(start) == STRICT_TIME_PREFIX)
        {
            start++;
            if (start >= end)
            {
                throwInvalidTimeString(value);
            }
        }
        int previous = start;
        int index = ParserTreeWalkerUtility.findNonDigit(value, previous, end);
        //hour
        try
        {
            Integer.parseInt(value.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidTimeString("Error parsing hour", value, start, end);
        }
        if (index == end || value.charAt(index++) != ':')
        {
            throwInvalidTimeString(value, start, end);
        }
        CStrictTime cStrictTime = new CStrictTime();
        // Minute
        previous = index;
        index = ParserTreeWalkerUtility.findNonDigit(value, previous, end);
        try
        {
            Integer.parseInt(value.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidTimeString("Error parsing minute", value, start, end);
        }
        if (index == end)
        {
            return createStrictTime(value);
        }
        if (value.charAt(index++) != ':')
        {
            throwInvalidTimeString("Error parsing minute", value, start, end);
        }
        // Second
        previous = index;
        index = ParserTreeWalkerUtility.findNonDigit(value, previous, end);
        try
        {
            Integer.parseInt(value.substring(previous, index));
        }
        catch (NumberFormatException e)
        {
            throwInvalidTimeString("Error parsing second", value, start, end);
        }
        if (index == end)
        {
            return createStrictTime(value);
        }
        if (value.charAt(index) == '.')
        {
            previous = index + 1;
            index = ParserTreeWalkerUtility.findNonDigit(value, previous, end);
            if (previous == end || index < end)
            {
                throwInvalidTimeString("Error parsing subsecond", value, start, end);
            }
            return createStrictTime(value);
        }
        if (index < end)
        {
            throwInvalidTimeString("Error parsing second", value, start, end);
        }
        return cStrictTime;
    }

    private CStrictTime createStrictTime(String value)
    {
        CStrictTime cStrictTime = new CStrictTime(value.substring(value.lastIndexOf(STRICT_TIME_PREFIX) + 1));
        cStrictTime.sourceInformation = walkerSourceInformation.getSourceInformation(this.timeToken.getSymbol());
        return cStrictTime;
    }

    private void throwInvalidTimeString(String string)
    {
        throwInvalidTimeString(string, 0, string.length());
    }

    private void throwInvalidTimeString(String string, int start, int end)
    {
        throwInvalidTimeString("Invalid StrictTime value ", string, start, end);
    }

    private void throwInvalidTimeString(String message, String timeString, int start, int end)
    {
        throw new EngineException(message + " '" + timeString.substring(start, end).replace("'", "\\'") + "'", this.walkerSourceInformation.getSourceInformation(this.timeToken.getSymbol()), EngineErrorType.PARSER);
    }
}
